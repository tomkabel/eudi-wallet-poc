"""Focused controls for retained target provenance and evidence classification."""

import importlib.util
import json
import unittest
from pathlib import Path

spec = importlib.util.spec_from_file_location(
    "host_evidence", Path(__file__).with_name("run-host-proof.py")
)
if spec is None or spec.loader is None:
    raise RuntimeError("Cannot load host evidence classifier")
host = importlib.util.module_from_spec(spec)
spec.loader.exec_module(host)


class HostEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.provenance = [
            {"status": "PASS", "returncode": 0, "output": value}
            for value in [host.REVISION, host.external.URL, ""]
        ]
        self.process = {"status": "PASS", "returncode": 0}
        self.payload = {
            "schema_version": 1,
            "target_revision": host.REVISION,
            "carrier_vectors": [
                {
                    "carrier": carrier,
                    "result": "PASS",
                    "parsed": True,
                    "proof_verified": True,
                    "evidence": "executed parser and native verifier",
                }
                for carrier in host.external.local.EXPECTED_CARRIERS
            ],
        }

    def classify(self, payload, test_count=1):
        return host.classify(
            self.process, self.provenance, self.provenance, payload, test_count, True
        )

    def test_exact_provenance_required(self):
        self.assertTrue(host.provenance_valid(self.provenance))
        for index, value in [
            (0, "wrong revision"),
            (1, "wrong origin"),
            (2, "?? source"),
        ]:
            rows = json.loads(json.dumps(self.provenance))
            rows[index]["output"] = value
            self.assertFalse(host.provenance_valid(rows))
            self.assertEqual(
                host.classify(
                    self.process, rows, self.provenance, self.payload, 1, True
                ),
                "ERROR",
            )

    def test_failure_overrides_process_success(self):
        self.payload["carrier_vectors"][0].update(
            result="FAIL", parsed=False, proof_verified=False
        )
        self.assertEqual(self.classify(self.payload), "FAIL")

    def test_no_vector_file_is_never_success(self):
        with self.assertRaises(ValueError):
            self.classify(None)

    def test_missing_or_duplicate_rows_rejected(self):
        self.payload["carrier_vectors"] = self.payload["carrier_vectors"][:1]
        with self.assertRaises(ValueError):
            self.classify(self.payload)
        self.payload["carrier_vectors"] *= 2
        with self.assertRaises(ValueError):
            self.classify(self.payload)

    def test_up_to_date_without_fresh_test_is_error(self):
        self.assertEqual(self.classify(self.payload, test_count=0), "ERROR")

    def test_nonzero_process_and_failed_post_provenance_are_errors(self):
        self.process["returncode"] = 1
        self.assertEqual(self.classify(self.payload), "ERROR")
        self.process["returncode"] = 0
        after = json.loads(json.dumps(self.provenance))
        after[2]["status"] = "ERROR"
        self.assertEqual(
            host.classify(self.process, self.provenance, after, self.payload, 1, True),
            "ERROR",
        )


if __name__ == "__main__":
    unittest.main()
