"""Executable regressions for the AV conformance runner."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RUNNER = ROOT / "tests/run_av_conformance.py"
STATICLIB = ROOT / "verifier/zkverify-ffi/target/release/libzkverify.a"


class AVConformanceRunnerTests(unittest.TestCase):
    def run_audit(self, *args):
        with tempfile.TemporaryDirectory() as tmp:
            report = Path(tmp) / "report.json"
            proc = subprocess.run([sys.executable, str(RUNNER), "--json", str(report), *args], cwd=ROOT,
                                  text=True, capture_output=True, timeout=90)
            return proc, json.loads(report.read_text()) if report.exists() else None

    @unittest.skipUnless(STATICLIB.is_file(), "full local audit needs the Rust staticlib")
    def test_local_candidate_records_distinct_clause_and_negative_counts(self):
        proc, report = self.run_audit()
        self.assertEqual(proc.returncode, 1, proc.stdout + proc.stderr)
        local, external = report["candidates"]
        self.assertEqual(local["coverage"]["unique_clauses"], 20)
        self.assertGreaterEqual(local["coverage"]["negative_controls"], 8)
        self.assertEqual(len({r["clause_id"] for r in local["results"]}), 20)
        self.assertTrue(any(r["state"] == "FAIL" and r["clause_id"] == "AV-A.6-016" for r in local["results"]))
        self.assertTrue(all(r["state"] == "NOT_EXECUTED" for r in external["results"]))

    def test_help_is_available(self):
        proc = subprocess.run([sys.executable, str(RUNNER), "--help"], cwd=ROOT, text=True, capture_output=True)
        self.assertEqual(proc.returncode, 0)
        self.assertIn("--candidate-command", proc.stdout)

    def test_malformed_external_adapter_is_error_not_pass(self):
        proc, report = self.run_audit("--skip-local", "--candidate-command", "printf '{bad'")
        self.assertEqual(proc.returncode, 1)
        external = report["candidates"][1]
        self.assertTrue(all(r["state"] == "ERROR" for r in external["results"]))


if __name__ == "__main__":
    unittest.main()
