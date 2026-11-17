"""Corruption controls for retained provenance and complete fixture-byte drift."""

import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("build_av_fixtures", ROOT / "tools/build_av_fixtures.py")
fixtures = importlib.util.module_from_spec(spec)
spec.loader.exec_module(fixtures)


class AVFixtureMutations(unittest.TestCase):
    def checkout(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        manifest = json.loads((ROOT / fixtures.OUT / "manifest.json").read_bytes())
        paths = [entry["path"] for entry in manifest["inputs"]]
        paths.append("tools/build_av_fixtures.py")
        for relative in paths:
            target = root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / relative, target)
        shutil.copytree(ROOT / fixtures.OUT, root / fixtures.OUT)
        return root

    def snapshot(self, root):
        return {p.relative_to(root).as_posix(): (p.read_bytes(), p.stat().st_mtime_ns)
                for p in root.rglob("*") if p.is_file()}

    def reject_read_only(self, root, message):
        before = self.snapshot(root)
        with self.assertRaisesRegex(fixtures.FixtureError, message):
            fixtures.check(root)
        self.assertEqual(before, self.snapshot(root), "failed drift checks must not write")

    def test_committed_complete_output_matches_builder(self):
        self.assertEqual(fixtures.check(), 49)
        expected = fixtures.build()
        manifest = json.loads(expected["manifest.json"])
        self.assertEqual({entry["path"] for entry in manifest["artifacts"]}, expected.keys() - {"manifest.json"})
        for entry in manifest["artifacts"]:
            self.assertEqual(entry["profile_revision"], fixtures.REVISION)
            self.assertEqual(len(entry["source_revision"]), 40)
            self.assertTrue(entry["origin"])
            self.assertTrue(entry["source"]["path"])
            self.assertEqual(entry["sha256"], fixtures.digest(expected[entry["path"]]))
        self.assertEqual(manifest["required_parameters"], list(fixtures.PARAMETERS))
        self.assertEqual((manifest["upstream_example_circuit_version"], manifest["local_circuit_version"]), (6, 7))

    def test_check_cli_is_read_only_and_nonzero_on_drift(self):
        root = self.checkout()
        command = [sys.executable, str(root / "tools/build_av_fixtures.py"), "--check"]
        before = self.snapshot(root)
        result = subprocess.run(command, capture_output=True, text=True, cwd=root)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(before, self.snapshot(root))
        (root / fixtures.OUT / "local/device-response.cbor").write_bytes(b"changed golden bytes")
        before = self.snapshot(root)
        result = subprocess.run(command, capture_output=True, text=True, cwd=root)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("byte drift: local/device-response.cbor", result.stderr)
        self.assertEqual(before, self.snapshot(root))

    def test_writing_cli_builds_complete_output_from_absent_tree(self):
        root = self.checkout()
        output = root / fixtures.OUT
        shutil.rmtree(output)
        self.assertFalse(output.exists())
        expected = fixtures.build(root)
        command = [sys.executable, str(root / "tools/build_av_fixtures.py")]
        result = subprocess.run(command, capture_output=True, text=True, cwd=root)
        self.assertEqual(result.returncode, 0, result.stderr)
        actual = {p.relative_to(output).as_posix(): p.read_bytes()
                  for p in output.rglob("*") if p.is_file()}
        self.assertEqual(actual.keys(), expected.keys())
        for name, raw in expected.items():
            with self.subTest(path=name):
                self.assertEqual(actual[name], raw)
        result = subprocess.run([*command, "--check"], capture_output=True, text=True, cwd=root)
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_output_corruption_including_manifest(self):
        for name in ("local/device-response.cbor", "local/device-request.cbor",
                     "external/22-dcapi-zkp-encrypted-response.cbor", "external/14-dcapi-request.json",
                     "external/23-dcapi-zkp-response.diag.txt", "manifest.json"):
            with self.subTest(path=name):
                root = self.checkout()
                path = root / fixtures.OUT / name
                raw = bytearray(path.read_bytes())
                raw[len(raw) // 2] ^= 1
                path.write_bytes(raw)
                self.reject_read_only(root, "byte drift: " + name)

    def test_manifest_cannot_relabel_origin_or_digest(self):
        for field, value in (("origin", "newly-generated-cryptographic-proof"),
                             ("profile_revision", "f" * 40), ("sha256", "0" * 64)):
            with self.subTest(field=field):
                root = self.checkout()
                path = root / fixtures.OUT / "manifest.json"
                manifest = json.loads(path.read_bytes())
                manifest["artifacts"][0][field] = value
                path.write_bytes(fixtures.json_bytes(manifest))
                self.reject_read_only(root, "byte drift: manifest.json")

    def test_missing_and_extra_outputs_fail_complete_inventory(self):
        root = self.checkout()
        (root / fixtures.OUT / "external/26-oid4vp-dcql.json").unlink()
        extra = root / fixtures.OUT / "unmanaged/nested.bin"
        extra.parent.mkdir()
        extra.write_bytes(b"extra")
        with self.assertRaises(fixtures.FixtureError) as error:
            fixtures.check(root)
        self.assertIn("missing output: external/26-oid4vp-dcql.json", str(error.exception))
        self.assertIn("extra output: unmanaged/nested.bin", str(error.exception))

    def test_output_root_symlink_is_refused(self):
        root = self.checkout()
        output = root / fixtures.OUT
        external = root / "external-output"
        shutil.move(output, external)
        output.symlink_to(external, target_is_directory=True)
        before = self.snapshot(root)
        with self.assertRaisesRegex(fixtures.FixtureError, "refusing output directory symlink"):
            fixtures.check(root)
        command = [sys.executable, str(root / "tools/build_av_fixtures.py")]
        result = subprocess.run(command, capture_output=True, text=True, cwd=root)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("refusing output directory symlink", result.stderr)
        self.assertEqual(before, self.snapshot(root), "builder must not write through an output symlink")

    def test_output_ancestor_symlink_is_refused(self):
        for relative in ("tests", "tests/vectors"):
            with self.subTest(ancestor=relative):
                root = self.checkout()
                ancestor = root / relative
                external = root / "external-output"
                shutil.move(ancestor, external)
                ancestor.symlink_to(external, target_is_directory=True)
                marker = external / ("vectors/av-profile" if relative == "tests" else "av-profile") / "local/device-response.cbor"
                marker.write_bytes(b"external marker must survive")
                before = self.snapshot(root)
                with self.assertRaisesRegex(fixtures.FixtureError, "refusing output directory symlink"):
                    fixtures.check(root)
                result = subprocess.run([sys.executable, str(root / "tools/build_av_fixtures.py")],
                                        capture_output=True, text=True, cwd=root)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("refusing output directory symlink", result.stderr)
                self.assertEqual(before, self.snapshot(root))

    def test_writing_cli_refuses_output_file_symlink(self):
        root = self.checkout()
        external = root / "external-marker.bin"
        external.write_bytes(b"external marker must survive")
        destination = root / fixtures.OUT / "local/device-response.cbor"
        destination.unlink()
        destination.symlink_to(external)
        before = (external.read_bytes(), external.stat().st_mtime_ns)
        result = subprocess.run([sys.executable, str(root / "tools/build_av_fixtures.py")],
                                capture_output=True, text=True, cwd=root)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("refusing output symlink: local/device-response.cbor", result.stderr)
        self.assertTrue(destination.is_symlink())
        self.assertEqual(before, (external.read_bytes(), external.stat().st_mtime_ns))

    def test_retained_source_and_local_input_corruption_fail_pins(self):
        for relative in ("docs/profiles/av/annex-a-examples.md", "docs/profiles/av/LICENSE.upstream",
                         *fixtures.LOCAL_PINS):
            with self.subTest(path=relative):
                root = self.checkout()
                path = root / relative
                path.write_bytes(path.read_bytes() + b"\n")
                self.reject_read_only(root, "SHA-256 pin mismatch")

    def test_provenance_mutation_cannot_refresh_pin(self):
        for field, value in (("revision", "0" * 40), ("source_sha256", "f" * 64),
                             ("extraction_rule", "reserialize examples"), ("attribution", "wrong author"),
                             ("normative", True), ("normative", 0), ("schema_version", True),
                             ("source_byte_start", 0)):
            with self.subTest(field=field):
                root = self.checkout()
                path = root / fixtures.PROFILE / "examples-source.json"
                metadata = json.loads(path.read_bytes())
                metadata[field] = value
                path.write_bytes(fixtures.json_bytes(metadata))
                self.reject_read_only(root, "pinned provenance mismatch")

    def test_normative_source_revision_cannot_diverge(self):
        root = self.checkout()
        path = root / fixtures.PROFILE / "source.json"
        metadata = json.loads(path.read_bytes())
        metadata["revision"] = "f" * 40
        path.write_bytes(fixtures.json_bytes(metadata))
        self.reject_read_only(root, "source.json: pinned revision mismatch")

    def test_duplicate_provenance_keys_fail(self):
        root = self.checkout()
        path = root / fixtures.PROFILE / "examples-source.json"
        path.write_text(path.read_text().replace('"normative": false', '"normative": true, "normative": false'))
        self.reject_read_only(root, "duplicate JSON key 'normative'")


if __name__ == "__main__":
    unittest.main()
