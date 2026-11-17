"""tools/doctor.py fails, one line per check, when the prover and the longfellow checkout are absent."""
import os
import pathlib
import subprocess
import sys
import tempfile
import unittest

DOCTOR = pathlib.Path(__file__).resolve().parent.parent / "tools" / "doctor.py"


def doctor(sibling, **env):
    e = {**os.environ, "DOCTOR_SIBLING": str(sibling), **env}
    return subprocess.run([sys.executable, str(DOCTOR)], capture_output=True, text=True, env=e)


class DoctorTest(unittest.TestCase):
    def test_missing_prover_fails(self):
        with tempfile.TemporaryDirectory() as d:
            r = doctor(d)
        self.assertEqual(r.returncode, 1, r.stdout)
        for name in ("prover binary (test -x)", "prover checksum and revision",
                     "longfellow-zk at the pinned revision"):
            self.assertIn(f"FAIL {name}", r.stdout)

    def test_skipped_groups_do_not_run(self):
        with tempfile.TemporaryDirectory() as d:
            r = doctor(d, DOCTOR_SKIP="prover longfellow")
        self.assertNotIn("prover binary", r.stdout)
        self.assertNotIn("longfellow-zk at", r.stdout)

    def test_rustflags_native_fails(self):
        with tempfile.TemporaryDirectory() as d:
            r = doctor(d, RUSTFLAGS="-C target-cpu=native")
        self.assertIn("FAIL RUSTFLAGS / target-cpu", r.stdout)


if __name__ == "__main__":
    unittest.main()
