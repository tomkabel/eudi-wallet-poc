"""Self-test for tools/publish_guard.py: a planted key in a throwaway git repo must fail the
guard, and the real tree must pass. The planted PEM carries a dummy body, built from parts so
this file never contains a private key header for a scanner to flag."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
GUARD = ROOT / "tools" / "publish_guard.py"
DUMMY_KEY = ("-----BEGIN " + "PRIVATE KEY-----\nAAAA\n-----END " + "PRIVATE KEY-----\n").encode()


def run(root):
    return subprocess.run(["python3", str(GUARD), "--root", str(root)], capture_output=True, text=True)


class PublishGuard(unittest.TestCase):
    def tree(self, files, allow=None):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        root = pathlib.Path(tmp.name)
        subprocess.run(["git", "init", "-q"], cwd=root, check=True)
        if allow:
            files = {**files, "tools/publish-guard.allow": allow.encode()}
        for rel, data in files.items():
            (root / rel).parent.mkdir(parents=True, exist_ok=True)
            (root / rel).write_bytes(data)
            subprocess.run(["git", "add", rel], cwd=root, check=True)
        return root

    def test_real_tree_passes(self):
        r = run(ROOT)
        self.assertEqual(r.returncode, 0, r.stderr)

    def test_planted_private_key_fails(self):
        r = run(self.tree({"fixtures/leak.pem": DUMMY_KEY}))
        self.assertEqual(r.returncode, 1)
        self.assertIn("fixtures/leak.pem", r.stderr)

    def test_planted_keystore_and_demo_data_fail(self):
        r = run(self.tree({"a/b.p12": b"x", "c/d.jks": b"x", "demo-data/x": b"x"}))
        self.assertEqual(r.returncode, 1)
        for p in ("a/b.p12", "c/d.jks", "demo-data/x"):
            self.assertIn(p, r.stderr)

    def test_public_pem_passes(self):
        self.assertEqual(run(self.tree({"ca.pem": b"-----BEGIN CERTIFICATE-----\nAAAA\n"})).returncode, 0)

    def test_allowlist_excuses_a_named_path_only(self):
        files = {"k/leak.pem": DUMMY_KEY, "k/other.pem": DUMMY_KEY}
        r = run(self.tree(files, allow="k/leak.pem  # throwaway\n"))
        self.assertEqual(r.returncode, 1)
        self.assertIn("k/other.pem", r.stderr)
        self.assertNotIn("k/leak.pem:", r.stderr)


if __name__ == "__main__":
    unittest.main()
