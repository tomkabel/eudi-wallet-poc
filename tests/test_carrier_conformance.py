import json
import shutil
import sys
import tempfile
import unittest
from pathlib import Path
from typing import cast
from unittest.mock import patch

import run_carrier_conformance as runner


def record(case_id):
    carriers, mode, outcomes = runner.EXPECTED_CASE_METADATA[str(case_id)]
    return {
        "case_id": str(case_id),
        "path_label": runner.EXPECTED_PATH,
        "carriers": list(carriers),
        "declared_response_mode": mode,
        "declared_outcome": outcomes[0],
        "result": "PASS",
        "binding_property": runner.EXPECTED_PROPERTIES.get(str(case_id), "not-applicable"),
    }


def write_records(env, last):
    lines = "".join(json.dumps(record(n)) + "\n" for n in range(1, last + 1))
    Path(env["EE_E2E_CASES"]).write_text(lines)


def passing_output(name):
    ok = name == "committed-carrier-vectors"
    return "--- PASS: TestCarrierVectorsVerifyIdentically" if ok else "ok"


def problems_of(report):
    return cast(list[str], report["inventory_problems"])


def suite_of(report, name):
    suites = cast(list[dict[str, str]], report["suites"])
    return next(s for s in suites if s["name"] == name)


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
    source = Path(__file__).resolve().parents[1]
    for relative in (
        "docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md",
        "docs/profiles/carrier-versions.json",
    ):
        target = root / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source / relative, target)
    inventory = json.loads((source / "docs/profiles/carrier-versions.json").read_text())
    for evidence in inventory["sources"].values():
        for relative in evidence["evidence"]:
            target = root / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source / relative, target)
    data = json.loads(manifest.read_text())
    source_manifest = source / "tests/vectors/carrier-v1/manifest.json"
    data["provenance"] = json.loads(source_manifest.read_text())["provenance"]
    manifest.write_text(json.dumps(data))
    return manifest


class CarrierConformanceTests(unittest.TestCase):
    def test_complete_inventory_is_reported_in_case_order(self):
        rows, problems = runner.validate_case_records(
            [record(n) for n in reversed(range(1, 20))]
        )
        self.assertEqual(problems, [])
        self.assertEqual([row["case_id"] for row in rows], list(runner.CASE_IDS))
        self.assertTrue(all(row["result"] == "PASS" for row in rows))

    def test_binding_property_evidence_is_required_and_case_specific(self):
        properties = {**runner.EXPECTED_PROPERTIES, "1": "not-applicable"}
        for case_id, property_name in properties.items():
            for claimed_result in ("PASS", "FAIL"):
                for wrong in (None, "binding", "transcript-binding", "wrong-property", []):
                    bad = record(case_id)
                    if wrong is None:
                        bad.pop("binding_property")
                    else:
                        bad["binding_property"] = wrong
                    bad["result"] = claimed_result
                    records = [
                        bad if n == case_id else record(n) for n in runner.CASE_IDS
                    ]
                    rows, problems = runner.validate_case_records(records)
                    row = next(row for row in rows if row["case_id"] == case_id)
                    self.assertEqual(
                        row["result"], "ERROR", (property_name, wrong, claimed_result)
                    )
                    self.assertEqual(row["assertion_result"], claimed_result)
                    self.assertEqual(row["binding_property"], wrong)
                    self.assertTrue(
                        any("binding_property" in problem for problem in problems)
                    )
                    self.assertFalse(runner.report_passed({
                        "inventory_problems": problems,
                        "suites": [{"status": "PASS"}],
                        "case_inventory": list(runner.CASE_IDS),
                        "cases": rows,
                        "carrier_vectors": [
                            {"carrier": carrier, "result": "PASS"}
                            for carrier in runner.EXPECTED_CARRIERS
                        ],
                    }))

    def test_failed_assertion_retains_controlled_property_in_summary(self):
        import contextlib
        import io
        for case_id, property_name in runner.EXPECTED_PROPERTIES.items():
            failed = record(case_id)
            failed["result"] = "FAIL"
            rows, problems = runner.validate_case_records([failed])
            row = next(row for row in rows if row["case_id"] == case_id)
            self.assertEqual(problems, [])
            self.assertEqual(row["binding_property"], property_name)
            self.assertEqual(row["result"], "FAIL")
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                runner.print_summary({"suites": [], "cases": [row], "carrier_vectors": [], "inventory_problems": []})
            self.assertIn("property=" + property_name, output.getvalue())
            self.assertIn("assertion=FAIL", output.getvalue())

    def test_missing_child_records_remain_not_executed(self):
        rows, problems = runner.validate_case_records([])
        self.assertEqual(problems, [])
        self.assertEqual(len(rows), 19)
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
        bad["declared_outcome"] = "binding-failure"
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[3]["result"], "ERROR")
        self.assertEqual(rows[3]["declared_outcome"], "malformed-case-record")
        self.assertTrue(any("invalid outcome" in problem for problem in problems))

    def test_case_carrier_must_match_observed_inventory(self):
        bad = record(1)
        bad["carriers"] = ["mso-mdoc-zk-cbor"]
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[0]["result"], "ERROR")
        self.assertTrue(any("do not match observed inventory" in problem for problem in problems))

    def test_case_response_mode_must_match_observed_inventory(self):
        bad = record(7)
        bad["declared_response_mode"] = "direct_post"
        rows, problems = runner.validate_case_records([bad])
        self.assertEqual(rows[6]["result"], "ERROR")
        self.assertTrue(any("response_mode" in problem and "observed inventory" in problem for problem in problems))

    def test_expected_outcome_is_case_specific(self):
        bad = record(4)
        bad["declared_outcome"] = "proof-verified"
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
            e2e.write_text(e2e.read_text() + '\necho "20. added assertion"\n')

            def fake_runner(name, command, cwd, env):
                if name == "e2e-assertions":
                    write_records(env, 19)
                output = passing_output(name)
                return runner.SuiteResult(name, "PASS", list(command), 0, output)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
        self.assertTrue(
            any(
                "case 20" in p and "no runner metadata" in p
                for p in problems_of(report)
            )
        )
        self.assertFalse(runner.report_passed(report))

    def test_failing_child_is_reported_as_fail_not_a_crash(self):
        # Go prints "--- FAIL:"; tests/e2e.sh prints "  FAIL  msg". Both exit non-zero.
        for body in ["print('--- FAIL: TestX')", "print('  FAIL  case 3 msg')"]:
            command = [sys.executable, "-c", body + "; raise SystemExit(1)"]
            result = runner.run_command("fail-test", command, Path.cwd())
            self.assertEqual(result.status, "FAIL", body)

    def test_deleted_numbered_assertion_is_reported(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            setup_root(root)
            e2e = root / "tests/e2e.sh"
            e2e.write_text(e2e.read_text().replace('echo "15.', 'echo "x.'))

            def fake_runner(name, command, cwd, env):
                if name == "e2e-assertions":
                    write_records(env, 14)
                output = passing_output(name)
                return runner.SuiteResult(name, "PASS", list(command), 0, output)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
        missing = "case 15: expected numbered assertion not found"
        self.assertTrue(any(missing in p for p in problems_of(report)))
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
            command = [sys.executable, "-c", "import os; os.write(1, b'x' * 100)"]
            result = runner.run_command("output-limit-test", command, Path.cwd())
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
                    write_records(env, 19)
                output = passing_output(name)
                return runner.SuiteResult(name, "PASS", list(command), 0, output)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            self.assertEqual(events, [
                "carrier-parser-tests", "committed-carrier-vectors", "e2e-assertions"
            ])
            self.assertEqual([item["carrier"] for item in report["carrier_vectors"]], list(runner.EXPECTED_CARRIERS))
            evidence = cast(dict[str, str], report["external_evidence"])
            self.assertEqual(evidence["status"], "BLOCKED")
            self.assertEqual(evidence["date"], runner.date.today().isoformat())
            self.assertEqual(evidence["attempt_date"], "2026-11-11")
            self.assertEqual(evidence["target"], "Multipaz sample wallet 0.99.0")
            self.assertEqual(
                evidence["revision"], "7486ddbca490038c3580513d8273b9bbf18c6b70"
            )
            self.assertEqual(evidence["runner"], "tests/run_external_carrier.py")
            self.assertEqual(
                evidence["evidence"],
                "docs/profiles/carrier/external-run-2026-11-11/report.json",
            )
            self.assertTrue(runner.report_passed(report))
            future = runner.date(2027, 1, 1)
            with patch.object(runner, "date") as clock:
                clock.today.return_value = future
                observed = runner.run_conformance(
                    root, fake_runner, staticlib_built=True
                )
            evidence = cast(dict[str, str], observed["external_evidence"])
            self.assertEqual(evidence["date"], "2027-01-01")
            self.assertEqual(evidence["attempt_date"], "2026-11-11")

    def test_vector_suite_requires_visible_pass_and_valid_manifest_hashes(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            manifest = setup_root(root)

            def fake_runner(name, command, cwd, env):
                if name == "e2e-assertions":
                    write_records(env, 19)
                output = "ok\n" if name == "committed-carrier-vectors" else "ok"
                return runner.SuiteResult(name, "PASS", list(command), 0, output)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = suite_of(report, "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            self.assertIn("without a visible PASS", vector_suite["detail"])
            vector_file = manifest.parent / "device_response.cbor"
            vector_file.write_bytes(b"changed")
            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = suite_of(report, "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            self.assertIn("SHA-256 mismatch", vector_suite["detail"])
            manifest_data = json.loads(manifest.read_text())
            manifest_data["sha256"].pop("request.json")
            manifest.write_text(json.dumps(manifest_data))
            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = suite_of(report, "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            self.assertIn("hash inventory", vector_suite["detail"])
            manifest_data["sha256"]["request.json"] = "x"
            manifest_data["source"] = "unverified-source"
            manifest.write_text(json.dumps(manifest_data))
            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            vector_suite = suite_of(report, "committed-carrier-vectors")
            self.assertEqual(vector_suite["status"], "ERROR")
            detail = vector_suite["detail"]
            self.assertTrue("provenance" in detail or "SHA-256 mismatch" in detail)

    def test_failed_child_without_records_marks_all_cases_not_executed(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            setup_root(root)

            def fake_runner(name, command, cwd, env):
                status = "ERROR" if name == "e2e-assertions" else "PASS"
                return runner.SuiteResult(name, status, list(command), None if status == "ERROR" else 0)

            report = runner.run_conformance(root, fake_runner, staticlib_built=True)
            cases = cast(list[dict[str, str]], report["cases"])
            self.assertTrue(all(c["result"] == "NOT_EXECUTED" for c in cases))
            self.assertFalse(runner.report_passed(report))


if __name__ == "__main__":
    unittest.main()
