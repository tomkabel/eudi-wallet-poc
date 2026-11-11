import hashlib
import importlib.util
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from typing import cast
from unittest.mock import patch

import check_carrier_versions as versions
import run_carrier_conformance as runner
from test_carrier_conformance import passing_output, setup_root, suite_of, write_records


class CarrierVersionTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.manifest = setup_root(self.root)
        self.inventory = self.root / versions.INVENTORY
        self.profile = self.root / versions.PROFILE

    def edit_json(self, path, edit):
        data = json.loads(path.read_text())
        edit(data)
        path.write_text(json.dumps(data))

    def rejects(self, text):
        problems = versions.validate_carrier_versions(self.root)
        self.assertTrue(any(text in problem for problem in problems), problems)

    def test_current_inventory_passes_without_writes(self):
        before = {
            p: (p.stat().st_mtime_ns, hashlib.sha256(p.read_bytes()).digest())
            for p in self.root.rglob("*")
            if p.is_file()
        }
        self.assertEqual(versions.validate_carrier_versions(self.root), [])
        after = {
            p: (p.stat().st_mtime_ns, hashlib.sha256(p.read_bytes()).digest())
            for p in self.root.rglob("*")
            if p.is_file()
        }
        self.assertEqual(before, after)

    def test_fixture_and_rule_missing_empty_malformed_or_stale_versions(self):
        for kind, path, key in [
            ("rule", self.inventory, "rules"),
            ("fixture", self.manifest, "provenance"),
        ]:
            original = path.read_text()
            for value in [None, "", [], "not-a-version", "0.9"]:
                with self.subTest(kind=kind, value=value):
                    path.write_text(original)

                    def mutate(data, key=key, value=value):
                        entry = next(iter(data[key].values()))
                        if value is None:
                            entry.pop("carrier_profile_version")
                        else:
                            entry["carrier_profile_version"] = value

                    self.edit_json(path, mutate)
                    self.rejects("carrier_profile_version")
            path.write_text(original)

    def test_bumped_header_rejects_every_rule_and_fixture(self):
        self.profile.write_text(
            self.profile.read_text().replace("**Version:** 1.0", "**Version:** 1.1")
        )
        problems = versions.validate_carrier_versions(self.root)
        for number in range(1, 16):
            self.assertTrue(
                any(
                    p.startswith(f"rule {number}: carrier_profile_version")
                    for p in problems
                )
            )
        for name in runner.EXPECTED_VECTOR_FILES:
            self.assertTrue(
                any(
                    p.startswith(f"fixture {name}: carrier_profile_version")
                    for p in problems
                )
            )

    def test_header_is_the_authority_after_a_coordinated_revision(self):
        self.profile.write_text(
            self.profile.read_text()
            .replace("**Version:** 1.0", "**Version:** 2.7")
            .replace("carrier-profile: 1.0;", "carrier-profile: 2.7;")
        )
        for path, key in [(self.inventory, "rules"), (self.manifest, "provenance")]:
            self.edit_json(
                path,
                lambda data, key=key: [
                    entry.update(carrier_profile_version="2.7")
                    for entry in data[key].values()
                ],
            )
        self.assertEqual(versions.validate_carrier_versions(self.root), [])

    def test_missing_duplicate_or_malformed_header(self):
        original = self.profile.read_text()
        for text in [
            original.replace("**Version:**", "**Old version:**"),
            original + "\n**Version:** 1.1 — revision\n",
            original.replace("**Version:** 1.0", "**Version:** banana"),
            original + "\n**Version:** banana — malformed\n",
        ]:
            self.profile.write_text(text)
            self.rejects("Version header")

    def test_new_rules_discovered_even_with_historical_markers(self):
        original = self.profile.read_text()
        for marker in [
            "**Profile rule 16.** New.",
            "Profile rule 16 follows: New.",
            "- **Rule 16 — new.**",
            "### Profile rule 16 — new",
        ]:
            self.profile.write_text(original + "\n" + marker + "\n")
            self.rejects("rule 16: missing")

    def test_duplicate_deleted_and_orphan_rules(self):
        original = self.profile.read_text()
        self.profile.write_text(original + "\n**Profile rule 1.** duplicate\n")
        self.rejects("rule 1: duplicate")
        self.profile.write_text(
            original.replace("**Profile rule 1.**", "**Removed requirement.**")
        )
        self.rejects("rule 1: orphan")
        self.profile.write_text(original)
        self.edit_json(
            self.inventory,
            lambda data: data["rules"].update({"99": data["rules"]["1"]}),
        )
        self.rejects("rule 99: orphan")

    def test_visible_labels_are_required_current_and_consistent(self):
        original = self.profile.read_text()
        for replacement in [
            "",
            "[carrier-profile: ; sources: repository-policy]",
            "[carrier-profile: 0.9; sources: repository-policy]",
            "[carrier-profile: 1.0; sources: unknown]",
        ]:
            text = versions.LABEL.sub(replacement, original, count=1)
            self.profile.write_text(text)
            self.rejects("rule 1:")

    def test_new_files_require_hashes_and_provenance_including_nested_files(self):
        for name in ["new.bin", "nested/new.bin"]:
            target = self.manifest.parent / name
            target.parent.mkdir(exist_ok=True)
            target.write_bytes(b"new")
            self.rejects("fixture " + name + ": missing sha256")
            self.edit_json(
                self.manifest,
                lambda data, name=name: data["sha256"].update(
                    {name: hashlib.sha256(b"new").hexdigest()}
                ),
            )
            self.rejects("fixture " + name + ": missing label")
            target.unlink()

    def test_missing_indexed_file_and_orphan_fixture_metadata(self):
        (self.manifest.parent / "request.json").unlink()
        self.rejects("fixture request.json: indexed file missing")
        self.edit_json(
            self.manifest,
            lambda data: data["provenance"].update(
                {"orphan.bin": data["provenance"]["request.json"]}
            ),
        )
        self.rejects("fixture orphan.bin: indexed file missing")

    def test_missing_unknown_empty_and_duplicate_source_references(self):
        for path, key in [(self.inventory, "rules"), (self.manifest, "provenance")]:
            original = path.read_text()
            for refs in [
                None,
                [],
                ["unknown"],
                [{}],
                ["repository-policy", "repository-policy"],
            ]:
                path.write_text(original)
                self.edit_json(
                    path,
                    lambda data, key=key, refs=refs: next(
                        iter(data[key].values())
                    ).update(sources=refs),
                )
                self.rejects("source reference")
            path.write_text(original)

    def test_missing_source_version_disposition_evidence_and_derivation(self):
        original = self.inventory.read_text()
        for field in ["version", "disposition", "evidence", "scope", "kind"]:
            self.inventory.write_text(original)
            self.edit_json(
                self.inventory,
                lambda data, field=field: data["sources"]["repository-policy"].pop(
                    field
                ),
            )
            self.rejects("source repository-policy:")
        self.inventory.write_text(original)
        self.edit_json(
            self.inventory, lambda data: data["rules"]["1"].pop("derivation")
        )
        self.rejects("rule 1: missing rule-specific derivation")

    def test_unresolved_evidence_and_uncertainty_issue_are_rejected(self):
        original = self.inventory.read_text()
        for path in ["missing.txt", "../outside.txt", "/etc/passwd"]:
            self.inventory.write_text(original)
            self.edit_json(
                self.inventory,
                lambda data, path=path: data["sources"]["repository-policy"].update(
                    evidence=[path]
                ),
            )
            self.rejects("unresolved local evidence")
        for issue in [None, "spec/EE-EUDIW-TS-1.0.md#23:999", "spec/missing.md#23:38"]:
            self.inventory.write_text(original)
            self.edit_json(
                self.inventory,
                lambda data, issue=issue: data["sources"]["iso-second-edition"].update(
                    spec_issue=issue
                ),
            )
            self.rejects("source iso-second-edition:")

    def test_malformed_missing_duplicate_and_nonobject_json(self):
        original = self.inventory.read_text()
        for contents in ["{", "[]", '{"rules":{},"rules":{}}', "\xff"]:
            self.inventory.write_text(contents)
            self.rejects("carrier version evidence:")
        self.inventory.write_text(original)
        self.inventory.unlink()
        self.rejects("carrier version evidence:")

    def test_builder_preserves_reviewed_labels_and_golden_bytes_without_writes(self):
        source_root = Path(__file__).resolve().parents[1]
        spec = importlib.util.spec_from_file_location(
            "carrier_fixture_builder", source_root / "tools/build_cbor_fixture.py"
        )
        if spec is None or spec.loader is None:
            raise RuntimeError("fixture builder module is unavailable")
        builder = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(builder)
        manifest = versions.read_object(
            source_root / versions.VECTORS / "manifest.json"
        )
        self.assertEqual(
            builder.fixture_provenance(source_root, set(manifest["sha256"])),
            manifest["provenance"],
        )
        fixture = source_root / "verifier/go/zk/testdata/step8-7-openid4vp-zk"
        entry = builder.zk_document_entry_bytes(
            (fixture / "device_response.cbor").read_bytes()
        )
        request = versions.read_object(fixture / "request.json")
        self.assertEqual(
            builder.build_cbor_device_response(entry),
            (source_root / versions.VECTORS / "device_response.cbor").read_bytes(),
        )
        self.assertEqual(
            builder.session_transcript(request),
            (
                source_root / versions.VECTORS / "device_response_transcript.bin"
            ).read_bytes(),
        )
        original = self.manifest.read_text()
        for action in [
            lambda data: data["provenance"].pop("request.json"),
            lambda data: data["provenance"]["request.json"].update(
                carrier_profile_version="0.9"
            ),
        ]:
            self.manifest.write_text(original)
            self.edit_json(self.manifest, action)
            with self.assertRaises(SystemExit):
                builder.fixture_provenance(self.root, set(manifest["sha256"]))

    def builder(self):
        source_root = Path(__file__).resolve().parents[1]
        spec = importlib.util.spec_from_file_location(
            "carrier_builder_under_test", source_root / "tools/build_cbor_fixture.py"
        )
        if spec is None or spec.loader is None:
            raise RuntimeError("fixture builder unavailable")
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def test_builder_import_ignores_shadowing_tests_package_in_isolated_process(self):
        shadow = self.root / "shadow/tests"
        shadow.mkdir(parents=True)
        (shadow / "__init__.py").write_text("raise RuntimeError('shadow imported')")
        builder_path = (
            Path(__file__).resolve().parents[1] / "tools/build_cbor_fixture.py"
        )
        code = (
            "import runpy, sys; sys.path.insert(0, sys.argv[1]); "
            "runpy.run_path(sys.argv[2], run_name='import_check')"
        )
        # The isolated child runs only this repository file with local test paths.
        result = subprocess.run(  # noqa: S603
            [sys.executable, "-I", "-c", code, str(shadow.parent), str(builder_path)],
            capture_output=True,
            text=True,
            check=False,
        )
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_builder_main_generates_reviewed_provenance_and_exact_payload_hashes(self):
        builder = self.builder()
        source_root = Path(__file__).resolve().parents[1]
        source_fixture = source_root / "verifier/go/zk/testdata/step8-7-openid4vp-zk"
        golden = versions.read_object(source_root / versions.VECTORS / "manifest.json")
        with patch.multiple(
            builder,
            ROOT=str(self.root),
            FIXTURE=str(source_fixture),
            OUT=str(self.manifest.parent),
        ):
            builder.main()
        generated = versions.read_object(self.manifest)
        self.assertEqual(generated["provenance"], golden["provenance"])
        self.assertEqual(generated["sha256"], golden["sha256"])
        for name, digest in generated["sha256"].items():
            self.assertEqual(
                hashlib.sha256((self.manifest.parent / name).read_bytes()).hexdigest(),
                digest,
            )

    def test_builder_main_refuses_bad_source_refs_before_writing(self):
        builder = self.builder()
        source_fixture = (
            Path(__file__).resolve().parents[1]
            / "verifier/go/zk/testdata/step8-7-openid4vp-zk"
        )
        original = self.manifest.read_text()
        for refs in [
            ["unknown"],
            ["repository-policy", "repository-policy"],
            [{}],
            "bad",
        ]:
            self.manifest.write_text(original)
            self.edit_json(
                self.manifest,
                lambda data, refs=refs: data["provenance"]["request.json"].update(
                    sources=refs
                ),
            )
            before = {
                p: (p.read_bytes(), p.stat().st_mtime_ns)
                for p in self.manifest.parent.iterdir()
            }
            with (
                patch.multiple(
                    builder,
                    ROOT=str(self.root),
                    FIXTURE=str(source_fixture),
                    OUT=str(self.manifest.parent),
                ),
                self.assertRaises(SystemExit),
            ):
                builder.main()
            self.assertEqual(
                before,
                {
                    p: (p.read_bytes(), p.stat().st_mtime_ns)
                    for p in self.manifest.parent.iterdir()
                },
            )

    def test_fixture_digest_format_and_corruption_are_rejected(self):
        original = self.manifest.read_text()
        for digest in ["", "x", None, [], "0" * 64]:
            self.manifest.write_text(original)
            self.edit_json(
                self.manifest,
                lambda data, digest=digest: data["sha256"].update(
                    {"request.json": digest}
                ),
            )
            self.rejects("fixture request.json:")
        self.manifest.write_text(original)
        (self.manifest.parent / "request.json").write_bytes(b"corrupt")
        self.rejects("fixture request.json: SHA-256 mismatch")

    def test_uncertainty_spec_read_errors_and_missing_section_are_retained(self):
        spec_path = self.root / "spec/EE-EUDIW-TS-1.0.md"
        original = spec_path.read_bytes()

        def fake_runner(name, command, cwd, env):
            if name == "e2e-assertions":
                write_records(env, 19)
            return runner.SuiteResult(
                name, "PASS", list(command), 0, passing_output(name)
            )

        for contents, expected in [
            (b"\xff", "could not read uncertainty spec"),
            (b"38. Uncertainty [UNVERIFIED]\n", "missing spec §23 section"),
        ]:
            spec_path.write_bytes(contents)
            self.rejects(expected)
            report = runner.run_conformance(
                self.root, fake_runner, staticlib_built=True
            )
            self.assertFalse(runner.report_passed(report))
            self.assertIn(
                expected, suite_of(report, "committed-carrier-vectors")["detail"]
            )
        spec_path.write_bytes(original)
        read_text = Path.read_text

        def unreadable(path, *args, **kwargs):
            if path == spec_path:
                raise PermissionError("uncertainty spec unreadable")
            return read_text(path, *args, **kwargs)

        with patch.object(Path, "read_text", unreadable):
            self.rejects("could not read uncertainty spec")
            report = runner.run_conformance(
                self.root, fake_runner, staticlib_built=True
            )
        self.assertFalse(runner.report_passed(report))
        self.assertIn(
            "uncertainty spec unreadable",
            suite_of(report, "committed-carrier-vectors")["detail"],
        )

    def test_ci_fast_gate_covers_every_version_evidence_input(self):
        source_root = Path(__file__).resolve().parents[1]
        workflow = (source_root / ".github/workflows/ci.yml").read_text()
        fast_filter = workflow.split("python: &python", 1)[1].split(
            "# Every language", 1
        )[0]
        inventory = versions.read_object(source_root / versions.INVENTORY)
        paths = {versions.PROFILE.as_posix(), versions.INVENTORY.as_posix()}
        for source in inventory["sources"].values():
            paths.update(source["evidence"])
            if "spec_issue" in source:
                paths.add(source["spec_issue"].split("#", 1)[0])
        for path in paths:
            self.assertTrue(
                path.startswith("verifier/") or "'" + path + "'" in fast_filter,
                path,
            )
        self.assertIn("run: make test-carrier-conformance", workflow)

    def test_runner_rejects_labels_even_when_every_child_passes(self):
        def fake_runner(name, command, cwd, env):
            if name == "e2e-assertions":
                write_records(env, 19)
            return runner.SuiteResult(
                name, "PASS", list(command), 0, passing_output(name)
            )

        self.edit_json(
            self.manifest,
            lambda data: data["provenance"]["request.json"].update(
                carrier_profile_version="0.9"
            ),
        )
        report = runner.run_conformance(self.root, fake_runner, staticlib_built=True)
        self.assertFalse(runner.report_passed(report))
        suite = suite_of(report, "committed-carrier-vectors")
        self.assertEqual(suite["status"], "ERROR")
        self.assertIn("fixture request.json", suite["detail"])
        self.assertTrue(
            all(
                row["result"] == "ERROR"
                for row in cast(list[dict], report["carrier_vectors"])
            )
        )
        self.assertTrue(
            any(
                "fixture request.json" in p
                for p in cast(list[str], report["inventory_problems"])
            )
        )


if __name__ == "__main__":
    unittest.main()
