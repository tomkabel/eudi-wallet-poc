import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import run_carrier_conformance as runner


def record(case_id):
    carriers, mode, outcomes = runner.EXPECTED_CASE_METADATA[str(case_id)]
    return {
        "case_id": str(case_id),
        "path_label": runner.EXPECTED_PATH,
        "carriers": list(carriers),
        "response_mode": mode,
        "outcome": outcomes[0],
        "result": "PASS",
    }


def setup_root(root):
    e2e = root / "tests/e2e.sh"
    e2e.parent.mkdir(parents=True)
    e2e.write_text(Path(__file__).with_name("e2e.sh").read_text())
    manifest = root / "tests/vectors/carrier-v1/manifest.json"
    manifest.parent.mkdir(parents=True)
    import hashlib
    hashes = {}
    for name in runner.EXPECTED_VECTOR_FILES:
        vector_file = manifest.parent / name
        vector_file.write_bytes(name.encode())
        hashes[name] = hashlib.sha256(name.encode()).hexdigest()
    manifest.write_text(json.dumps({
        **runner.EXPECTED_MANIFEST_PROVENANCE,
        "carriers": list(runner.EXPECTED_CARRIERS),
        "sha256": hashes,
    }))
    return manifest


class CarrierConformanceTests(unittest.TestCase):
    def test_complete_inventory_is_reported_in_case_order(self):
        rows, problems = runner.validate_case_records(
            [record(n) for n in reversed(range(1, 14))]
        )
        self.assertEqual(problems, [])
        self.assertEqual([row["case_id"] for row in rows], list(runner.CASE_IDS))
        self.assertTrue(all(row["result"] == "PASS" for row in rows))

    def test_missing_child_records_remain_not_executed(self):
        rows, problems = runner.validate_case_records([])
        self.assertEqual(problems, [])
        self.assertEqual(len(rows), 13)
        self.assertTrue(all(row["result"] == "NOT_EXECUTED" for row in rows))
        self.assertFalse(runner.report_passed({
            "inventory_problems": [],
            "suites": [{"status": "ERROR"}],
            "cases": rows,
            "carrier_vectors": [],
        }))

    def test_duplicate_case_is_an_error_not_a_pass(self):
        rows, problems = runner.validate_case_records([record(1), record(1)])
        self.assertEqual(rows[0]["result"], "ERROR")
        self.assertIn("case 1: duplicate record", problems)

    def test_malformed_outcome_is_an_error_not_a_pass(self):
        bad = record(4)
        bad["outcome"] = "binding-failure"
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[3]["result"], "ERROR")
        self.assertEqual(rows[3]["outcome"], "malformed-case-record")
        self.assertTrue(any("invalid outcome" in problem for problem in problems))

    def test_case_carrier_must_match_observed_inventory(self):
        bad = record(1)
        bad["carriers"] = ["mso-mdoc-zk-cbor"]
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[0]["result"], "ERROR")
        self.assertTrue(any("do not match observed inventory" in problem for problem in problems))

    def test_case_response_mode_must_match_observed_inventory(self):
        bad = record(7)
        bad["response_mode"] = "direct_post"
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[6]["result"], "ERROR")
        self.assertTrue(any("response_mode" in problem and "observed inventory" in problem for problem in problems))

    def test_expected_outcome_is_case_specific(self):
        bad = record(4)
        bad["outcome"] = "proof-verified"
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[3]["result"], "ERROR")
        self.assertTrue(any("outcome" in problem and "expected" in problem for problem in problems))

    def test_malformed_jsonl_is_preserved_as_a_problem(self):
        records, problems = runner.decode_case_records('{"case_id": "1",\n')
        rows, validation = runner.validate_case_records(records, problems)
        self.assertEqual(records, [])
        self.assertTrue(any("malformed case record" in problem for problem in validation))
        self.assertEqual(rows[0]["result"], "NOT_EXECUTED")

    def test_invalid_utf8_jsonl_is_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "cases.jsonl"
            path.write_bytes(b"\xff\xfe")
            records, problems = runner.read_case_records(path)
        self.assertEqual(records, [])
        self.assertTrue(any("not valid UTF-8" in problem for problem in problems))

    def test_unhashable_record_fields_are_errors_without_crashing(self):
        bad = record(1)
        bad.update(carriers=[{}], response_mode=[], outcome={}, result=[])
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[0]["result"], "ERROR")
        self.assertTrue(problems)

    def test_numbered_assertion_without_metadata_is_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            setup_root(root)
            e2e = root / "tests/e2e.sh"
            e2e.write_text(e2e.read_text() + '\necho "14. added assertion"\n')

            def fake_runner(name, command, cwd, env):
                if name == "e2e-assertions":
                    Path(env["EE_E2E_CASES"]).write_text("".join(json.dumps(record(n)) + "\n" for n in range(1, 14)))
                output = "--- PASS: TestCarrierVectorsVerifyIdentically" if name == "committed-carrier-vectors" else "ok"
                return runner.SuiteResult(name, "PASS", list(command), 0, output)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
        self.assertTrue(any("case 14" in problem and "no runner metadata" in problem for problem in report["inventory_problems"]))
        self.assertFalse(runner.report_passed(report))

    def test_child_timeout_is_bounded_and_reported_as_error(self):
        with patch.object(runner, "COMMAND_TIMEOUT_SECONDS", 0.05):
            result = runner.run_command(
                "timeout-test",
                [sys.executable, "-c", "import os,time; os.close(1); time.sleep(5)"],
                Path.cwd(),
            )
        self.assertEqual(result.status, "ERROR")
        self.assertIn("timeout", result.detail)

    def test_child_output_limit_is_bounded_and_reported_as_error(self):
        with patch.object(runner, "COMMAND_OUTPUT_LIMIT_BYTES", 16):
            result = runner.run_command("output-limit-test", [sys.executable, "-c", "import os; os.write(1, b'x' * 100)"], Path.cwd())
        self.assertEqual(result.status, "ERROR")
        self.assertIn("byte limit", result.detail)

    def test_runner_orders_go_parser_vectors_then_e2e(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            setup_root(root)
            events = []

            def fake_runner(name, command, cwd, env):
                events.append(name)
                if name == "e2e-assertions":
                    lines = "".join(json.dumps(record(n)) + "\n" for n in range(1, 14))
                    Path(env["EE_E2E_CASES"]).write_text(lines)
                output = "--- PASS: TestCarrierVectorsVerifyIdentically" if name == "committed-carrier-vectors" else "ok"
                return runner.SuiteResult(name, "PASS", list(command), 0, output)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            self.assertEqual(events, [
                "carrier-parser-tests", "committed-carrier-vectors", "e2e-assertions"
            ])
            self.assertEqual([item["carrier"] for item in report["carrier_vectors"]], list(runner.EXPECTED_CARRIERS))
            self.assertEqual(report["external_evidence"]["status"], "BLOCKED")
            self.assertEqual(report["external_evidence"]["date"], runner.date.today().isoformat())
            self.assertTrue(runner.report_passed(report))

    def test_vector_suite_requires_visible_pass_and_valid_manifest_hashes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            manifest = setup_root(root)

            def fake_runner(name, command, cwd, env):
                if name == "e2e-assertions":
                    Path(env["EE_E2E_CASES"]).write_text("".join(json.dumps(record(n)) + "\n" for n in range(1, 14)))
                output = "ok\n" if name == "committed-carrier-vectors" else "ok"
                return runner.SuiteResult(name, "PASS", list(command), 0, output)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = next(s for s in report["suites"] if s["name"] == "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            self.assertIn("without a visible PASS", vector_suite["detail"])
            vector_file = manifest.parent / "device_response.cbor"
            vector_file.write_bytes(b"changed")
            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = next(s for s in report["suites"] if s["name"] == "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            self.assertIn("SHA-256 mismatch", vector_suite["detail"])
            manifest_data = json.loads(manifest.read_text())
            manifest_data["sha256"].pop("request.json")
            manifest.write_text(json.dumps(manifest_data))
            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = next(s for s in report["suites"] if s["name"] == "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            self.assertIn("hash inventory", vector_suite["detail"])
            manifest_data["sha256"]["request.json"] = "x"
            manifest_data["source"] = "unverified-source"
            manifest.write_text(json.dumps(manifest_data))
            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = next(s for s in report["suites"] if s["name"] == "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            self.assertTrue("provenance" in vector_suite["detail"] or "SHA-256 mismatch" in vector_suite["detail"])

    def test_failed_child_without_records_marks_all_cases_not_executed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            setup_root(root)

            def fake_runner(name, command, cwd, env):
                status = "ERROR" if name == "e2e-assertions" else "PASS"
                return runner.SuiteResult(name, status, list(command), None if status == "ERROR" else 0)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            self.assertTrue(all(item["result"] == "NOT_EXECUTED" for item in report["cases"]))
            self.assertFalse(runner.report_passed(report))


if __name__ == "__main__":
    unittest.main()
