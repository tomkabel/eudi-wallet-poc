"""Self-test for tools/pin_gate.py: unpinned `uses:` and `FROM` lines in a throwaway git repo must
fail the gate, pinned ones and allowlisted ones must pass, and the real tree must pass."""
import pathlib
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parent.parent
GATE = ROOT / "tools" / "pin_gate.py"
SHA = "a" * 40
DIGEST = "b" * 64


def run(root):
    return subprocess.run(["python3", str(GATE), "--root", str(root)], capture_output=True, text=True)


class PinGate(unittest.TestCase):
    def tree(self, files):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        root = pathlib.Path(tmp.name)
        subprocess.run(["git", "init", "-q"], cwd=root, check=True)
        for rel, text in files.items():
            (root / rel).parent.mkdir(parents=True, exist_ok=True)
            (root / rel).write_text(text)
            subprocess.run(["git", "add", rel], cwd=root, check=True)
        return root

    def test_real_tree_passes(self):
        r = run(ROOT)
        self.assertEqual(r.returncode, 0, r.stderr)

    def test_tag_and_branch_refs_fail(self):
        for ref in ("actions/checkout@v4", "actions/checkout@main", "actions/checkout"):
            wf = f"jobs:\n  a:\n    steps:\n      - uses: {ref}\n"
            r = run(self.tree({".github/workflows/x.yml": wf}))
            self.assertEqual(r.returncode, 1, ref)
            self.assertIn("40-hex", r.stderr)

    def test_pinned_local_and_docker_digest_pass(self):
        wf = (f"      - uses: actions/checkout@{SHA} # v4\n      - uses: ./local\n"
              f"      - uses: docker://alpine@sha256:{DIGEST}\n")
        self.assertEqual(run(self.tree({".github/workflows/x.yml": wf})).returncode, 0)

    def test_from_needs_digest(self):
        r = run(self.tree({"d/Dockerfile": "FROM debian:trixie-slim\n"}))
        self.assertEqual(r.returncode, 1)
        self.assertIn("sha256", r.stderr)
        ok = f"FROM debian@sha256:{DIGEST} AS build\nFROM build\nFROM scratch\n"
        self.assertEqual(run(self.tree({"d/Dockerfile": ok})).returncode, 0)

    def test_allowlist_excuses_and_stale_entry_fails(self):
        allow = "d/Dockerfile:FROM debian  # reason\n"
        r = run(self.tree({"d/Dockerfile": "FROM debian:trixie-slim\n", "tools/pin-gate.allow": allow}))
        self.assertEqual(r.returncode, 0, r.stderr)
        r = run(self.tree({"d/Dockerfile": f"FROM debian@sha256:{DIGEST}\n", "tools/pin-gate.allow": allow}))
        self.assertEqual(r.returncode, 1)
        self.assertIn("matched nothing", r.stderr)


if __name__ == "__main__":
    unittest.main()
