"""Offline integrity controls; synthetic records never establish wallet acceptance."""

import copy
import hashlib
import json
import os
import sys
import tempfile
import time
import unittest
from datetime import UTC, datetime, timedelta
from pathlib import Path
from unittest.mock import patch

import run_estonian_acceptance as runner


class EstonianAcceptanceIntegrityTest(unittest.TestCase):
    def test_nested_json_reports_named_error(self):
        with self.assertRaisesRegex(ValueError, "nesting limit"):
            runner.decode("[" * 12000 + "0" + "]" * 12000)

    def test_bounded_read_precedes_hash_and_decode(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "large.json"
            path.write_bytes(b" " * (runner.LIMIT + 1))
            with self.assertRaisesRegex(ValueError, "byte limit"):
                runner.read_json(path)

    def test_missing_or_unreadable_pin_keeps_seven_rows(self):
        original = runner.bounded_bytes

        def read(path):
            if path.name == "longfellow-rev.txt":
                raise PermissionError("synthetic denied pin")
            return original(path)

        child = runner.bounded.SuiteResult("fake", "ERROR", ["fake"], 1, "")
        with (
            patch.object(runner, "bounded_bytes", side_effect=read),
            patch.object(runner, "run_child", return_value=child),
        ):
            report = runner.execute(local=True, jvm=False)
        self.assertEqual(len(report["cases"]), 7)
        self.assertEqual(report["cases"][3]["state"], "ERROR")
        self.assertIn(
            "missing-or-unreadable-longfellow-pin", " ".join(report["diagnostics"])
        )

    def test_source_mutation_invalidates_all_local_claims(self):
        with patch.object(
            runner, "source_snapshot", side_effect=[{"a": "before"}, {"a": "after"}]
        ):
            report = runner.execute(local=False, jvm=False)
        self.assertTrue(all(row["state"] == "ERROR" for row in report["cases"][2:]))
        self.assertIn("source-mutated-during-execution", report["diagnostics"])

    def test_snapshot_includes_untracked_implementation_excludes_private_and_build_output(
        self,
    ):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            runner.subprocess.run(["git", "init", "-q", str(root)], check=True)
            for name in (
                "new.py",
                "verifier/go/iso.go",
                "app/src/main/Thing.kt",
                "demo-data/key",
                "app/build/output",
                "rp-kit/evidence/x",
            ):
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("control")
            self.assertEqual(
                set(runner.source_snapshot(root)),
                {"new.py", "verifier/go/iso.go", "app/src/main/Thing.kt"},
            )

    def test_clean_clone_of_same_repository_is_not_independent(self):
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "clone"
            runner.subprocess.run(
                [
                    "git",
                    "clone",
                    "--quiet",
                    "--no-checkout",
                    str(runner.ROOT),
                    str(source),
                ],
                check=True,
            )
            with self.assertRaisesRegex(ValueError, "shares this repository"):
                runner.require_independent_source(runner.ROOT, source)

    def test_seven_fixed_ordered_rows_and_nonzero_incomplete(self):
        report = runner.execute(local=False, jvm=False)
        self.assertEqual(
            tuple(row["case_id"] for row in report["cases"]), runner.CASE_IDS
        )
        self.assertTrue(all(row["state"] == "NOT_EXECUTED" for row in report["cases"]))
        self.assertEqual(report["result"], "INCOMPLETE")
        self.assertEqual(report["exit_code"], 1)
        self.assertEqual(len(report["operator_actions"]), 2)

    def test_strict_missing_duplicate_unknown_malformed_inventory(self):
        for records in (
            [],
            [{"case_id": "unknown"}],
            ["bad"],
            [{"case_id": key} for key in runner.FOREIGN] * 2,
        ):
            with self.subTest(records=records), self.assertRaises(ValueError):
                runner.ordered_records(records, runner.FOREIGN)
        with self.assertRaises(ValueError):
            runner.decode('{"state":"PASS","state":"FAIL"}')
        with self.assertRaises(ValueError):
            runner.decode("{")
        with self.assertRaises(ValueError):
            runner.decode('{"observed":NaN}')

    def test_exit_zero_missing_observations_never_passes(self):
        result = runner.bounded.SuiteResult("fake", "PASS", ["fake"], 0, "")
        with patch.object(runner, "run_child", return_value=result):
            report = runner.execute(local=True, jvm=False)
        self.assertEqual(report["cases"][3]["state"], "ERROR")
        self.assertFalse(report["agent_controls_passed"])
        self.assertEqual(report["exit_code"], 1)

    def test_dispatch_requires_real_refusal_and_zero_response(self):
        records = [
            {
                "case_id": key,
                "outcome": "UNSUPPORTED_PROTOCOL"
                if key == runner.DISPATCH[0]
                else "PRESENTATION_UNAUTHENTICATED_REQUEST",
                "evidence_kind": "production-viewmodel",
                "requests": 1,
                "responses": 0,
                "refused": True,
                "consent_released": False,
                "matched_requests": 0 if key == runner.DISPATCH[0] else 1,
            }
            for key in runner.DISPATCH
        ]
        self.assertEqual(len(runner.validate_dispatch(records)), 3)
        for key, value in (
            ("responses", 1),
            ("refused", False),
            ("requests", 0),
            ("evidence_kind", "boolean-policy"),
        ):
            changed = copy.deepcopy(records)
            changed[1][key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                runner.validate_dispatch(changed)
        records[0]["outcome"] = "tier2-refusal"
        with self.assertRaises(ValueError):
            runner.validate_dispatch(records)

    def test_fallback_requires_both_usable_matching_validated_consumers(self):
        records = [
            {
                "consumer": key,
                "list_version": "1",
                "spec_id": "longfellow-libzk-v1_6_1_2_3_hash",
                "document_sha256": "a" * 64,
                "fetched": False,
                "signature_valid": True,
            }
            for key in ("wallet", "verifier")
        ]
        self.assertEqual(
            runner.validate_fallback(records)["outcome"],
            "shared-signed-fallback-resolved",
        )
        for field, value in (
            ("list_version", "2"),
            ("spec_id", "longfellow-libzk-v1_other"),
            ("document_sha256", "b" * 64),
            ("fetched", True),
            ("signature_valid", False),
            ("spec_id", ""),
        ):
            changed = copy.deepcopy(records)
            changed[0][field] = value
            with self.subTest(field=field, value=value), self.assertRaises(ValueError):
                runner.validate_fallback(changed)
        for changed in (records[:1], records + records[:1]):
            with self.assertRaises(ValueError):
                runner.validate_fallback(changed)

    def test_jvm_stale_skipped_missing_or_failed_never_passes(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "suite.xml"
            for attribute in (
                'tests="0"',
                'tests="1" skipped="1"',
                'tests="1" failures="1"',
                'tests="1" errors="1"',
            ):
                path.write_text("<testsuite " + attribute + "></testsuite>")
                with self.subTest(attribute=attribute), self.assertRaises(ValueError):
                    runner.jvm_evidence([path], 0)
            path.write_text('<testsuite tests="1"></testsuite>')
            with self.assertRaises(ValueError):
                runner.jvm_evidence([path], time.time_ns() + 10**9)
            with self.assertRaises(ValueError):
                runner.jvm_evidence([Path(directory) / "absent"], 0)

    def test_cli_always_writes_seven_rows_and_nonzero_incomplete(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "report.json"
            self.assertEqual(
                runner.main(["--json", str(path), "--no-local", "--no-jvm"]), 1
            )
            self.assertEqual(len(json.loads(path.read_text())["cases"]), 7)


class ForeignEvidenceContractTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.work = Path(self.directory.name)
        self.now = datetime.now(UTC)
        self.source = {"state": "OK", "dirty": False, "revision": "a" * 40}
        self.source["path"] = "/independent/source"
        build = self.work / "wallet.apk"
        build.write_bytes(b"synthetic build contract control")
        build_digest = hashlib.sha256(build.read_bytes()).hexdigest()
        self.document = {
            "schema": "ee-estonian-external/1",
            "run_id": "run",
            "challenge": "fresh",
            "source_revision": "a" * 40,
            "wallet_build": {
                "run_id": "run",
                "challenge": "fresh",
                "source_revision": "a" * 40,
                "source_path": self.source["path"],
                "adapter_sha256": "d" * 64,
                "package_id": "independent.synthetic.control",
                "built_at": self.now.isoformat(),
                "artifact": {"path": "wallet.apk", "sha256": build_digest},
            },
            "records": [],
        }
        for key in runner.FOREIGN:
            session_id = "session-" + key
            request_wire = {"nonce": "fresh", "state": session_id}
            response_wire = {"state": session_id, "vp_token": "synthetic-contract-only"}
            artifacts = {}
            for kind in ("request", "response", "verification"):
                value = {
                    "session_id": session_id,
                    "challenge": "fresh",
                    "received": True,
                    "valid": True,
                    "wire": request_wire if kind == "request" else response_wire,
                    "verified_nonce": "fresh",
                    "verified_session_id": session_id,
                    "request_wire_sha256": runner.wire_digest(request_wire),
                    "response_wire_sha256": runner.wire_digest(response_wire),
                    "executed": True,
                    "verifier": "synthetic contract control",
                    "trust_input_sha256": "c" * 64,
                }
                path = self.work / (key + "-" + kind + ".json")
                path.write_text(json.dumps(value))
                artifacts[kind] = {
                    "path": path.name,
                    "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
                }
            self.document["records"].append(
                {
                    "case_id": key,
                    "state": "PASS",
                    "outcome": "presentation-accepted",
                    "session": {
                        "id": session_id,
                        "challenge": "fresh",
                        "credential_kind": "PID" if key == "foreign-pid" else "AV",
                        "wallet": "independent test fixture",
                        "source_revision": "a" * 40,
                        "wallet_build_sha256": build_digest,
                        "issuing_country": "FI",
                        "observed_at": self.now.isoformat(),
                    },
                    "artifacts": artifacts,
                }
            )

    def validate(self, document=None, source=None):
        return runner.validate_foreign(
            document or self.document,
            "run",
            "fresh",
            self.now - timedelta(seconds=1),
            self.now + timedelta(seconds=1),
            self.work,
            source or self.source,
            "d" * 64,
        )

    def test_synthetic_complete_contract_is_parseable_not_external_evidence(self):
        parsed = self.validate()
        self.assertEqual(len(parsed), 2)
        self.assertEqual(
            parsed[0]["observed_artifacts"]["request"]["content"]["wire"],
            {"nonce": "fresh", "state": "session-foreign-pid"},
        )

    def test_build_identity_digest_and_execution_are_required(self):
        for field, value in (
            ("source_revision", "b" * 40),
            ("adapter_sha256", "e" * 64),
            ("source_path", "/wrong"),
            ("package_id", ""),
            ("run_id", "stale"),
            ("built_at", (self.now - timedelta(days=1)).isoformat()),
        ):
            document = copy.deepcopy(self.document)
            document["wallet_build"][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.validate(document)
        (self.work / "wallet.apk").write_bytes(b"different build")
        with self.assertRaisesRegex(ValueError, "build artifact digest"):
            self.validate()

    def test_fresh_wrappers_cannot_repackage_stale_wire_or_false_verification_binding(
        self,
    ):
        for kind, field, value in (
            ("request", "nonce", "old"),
            ("request", "state", "old"),
            ("response", "state", "old"),
            ("verification", "request_wire_sha256", "0" * 64),
            ("verification", "response_wire_sha256", "0" * 64),
            ("verification", "verified_nonce", "old"),
        ):
            document = copy.deepcopy(self.document)
            artifact = document["records"][0]["artifacts"][kind]
            path = self.work / artifact["path"]
            original = path.read_bytes()
            content = json.loads(original)
            target = content if kind == "verification" else content["wire"]
            target[field] = value
            path.write_text(json.dumps(content))
            artifact["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
            with (
                self.subTest(kind=kind, field=field),
                self.assertRaisesRegex(ValueError, "wire nonce/session"),
            ):
                self.validate(document)
            path.write_bytes(original)

    def test_oversized_foreign_artifact_rejected_before_hashing(self):
        artifact = self.document["records"][0]["artifacts"]["request"]
        (self.work / artifact["path"]).write_bytes(b" " * (runner.LIMIT + 1))
        with self.assertRaisesRegex(ValueError, "byte limit"):
            self.validate()

    def test_stale_wrong_run_challenge_source_dirty_local_country_and_wrong_kind(self):
        for field, value in (
            ("observed_at", (self.now - timedelta(days=1)).isoformat()),
            ("challenge", "stale"),
            ("source_revision", "b" * 40),
            ("issuing_country", "EE"),
            ("credential_kind", "AV"),
            ("wallet", ""),
        ):
            document = copy.deepcopy(self.document)
            document["records"][0]["session"][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.validate(document)
        for field, value in (
            ("run_id", "old"),
            ("challenge", "old"),
            ("source_revision", "abc"),
        ):
            document = copy.deepcopy(self.document)
            document[field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.validate(document)
        with self.assertRaises(ValueError):
            self.validate(source={**self.source, "dirty": True})

    def test_unknown_duplicate_missing_skipped_foreign_case(self):
        for records in (
            self.document["records"][:1],
            self.document["records"] * 2,
            [{"case_id": "unknown"}],
            [{**row, "state": "SKIP"} for row in self.document["records"]],
        ):
            with self.subTest(records=records), self.assertRaises(ValueError):
                self.validate({**self.document, "records": records})

    def test_no_wire_artifacts_bad_digest_path_escape_or_false_verification(self):
        for kind in ("request", "response", "verification"):
            changed = copy.deepcopy(self.document)
            del changed["records"][0]["artifacts"][kind]
            with self.subTest(kind=kind), self.assertRaises(ValueError):
                self.validate(changed)
        for field, value in (("sha256", "a" * 64), ("path", "../outside.json")):
            changed = copy.deepcopy(self.document)
            changed["records"][0]["artifacts"]["request"][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                self.validate(changed)
        artifact = self.document["records"][0]["artifacts"]["verification"]
        path = self.work / artifact["path"]
        value = json.loads(path.read_text())
        value["valid"] = False
        path.write_text(json.dumps(value))
        artifact["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
        with self.assertRaises(ValueError):
            self.validate()

    def test_status_fields_alone_cannot_substitute_actual_wire_and_trust_inputs(self):
        for kind, field in (
            ("request", "wire"),
            ("response", "wire"),
            ("verification", "executed"),
            ("verification", "verifier"),
            ("verification", "trust_input_sha256"),
        ):
            with self.subTest(kind=kind, field=field):
                document = copy.deepcopy(self.document)
                artifact = document["records"][0]["artifacts"][kind]
                path = self.work / artifact["path"]
                original = path.read_text()
                value = json.loads(original)
                del value[field]
                path.write_text(json.dumps(value))
                artifact["sha256"] = hashlib.sha256(path.read_bytes()).hexdigest()
                with self.assertRaises(ValueError):
                    self.validate(document)
                path.write_text(original)

    def test_not_executed_requires_action_and_is_retained(self):
        document = copy.deepcopy(self.document)
        document["records"][0] = {
            "case_id": "foreign-pid",
            "state": "NOT_EXECUTED",
            "operator_action": "Obtain a genuine foreign PID and execute the session.",
        }
        self.assertEqual(self.validate(document)[0]["state"], "NOT_EXECUTED")
        document["records"][0]["operator_action"] = ""
        with self.assertRaises(ValueError):
            self.validate(document)

    def test_named_observed_failure_is_retained_instead_of_becoming_pass(self):
        document = copy.deepcopy(self.document)
        document["records"][0].update(state="FAIL", outcome="issuer-trust-refused")
        self.assertEqual(self.validate(document)[0]["state"], "FAIL")

    def test_duplicate_session_cannot_claim_two_distinct_cases(self):
        document = copy.deepcopy(self.document)
        document["records"][1]["session"]["id"] = document["records"][0]["session"][
            "id"
        ]
        with self.assertRaises(ValueError):
            self.validate(document)


class BoundedChildTest(unittest.TestCase):
    def test_success_failure_timeout_and_output_limit(self):
        commands = [
            ("pass", "print('ok')", "PASS", 2, 1000),
            ("fail", "raise SystemExit(2)", "ERROR", 2, 1000),
            ("timeout", "import time; time.sleep(5)", "ERROR", 0.1, 1000),
            ("output", "print('x'*10000)", "ERROR", 2, 100),
        ]
        for name, code, expected, timeout, limit in commands:
            with self.subTest(name=name):
                result = runner.run_child(
                    name,
                    [sys.executable, "-c", code],
                    runner.ROOT,
                    timeout=timeout,
                    output_limit=limit,
                )
                self.assertEqual(result.status, expected)
                if name in ("timeout", "output"):
                    self.assertIn("exceeded", result.detail)

    @unittest.skipUnless(os.name == "posix", "process-group assertion requires POSIX")
    def test_descendant_with_open_output_pipe_is_bounded(self):
        start = time.monotonic()
        result = runner.run_child(
            "descendant",
            [
                sys.executable,
                "-c",
                "import subprocess,sys; "
                "subprocess.Popen([sys.executable,'-c',"
                "'import time; time.sleep(10)'])",
            ],
            runner.ROOT,
            timeout=0.2,
        )
        self.assertEqual(result.status, "ERROR")
        self.assertLess(time.monotonic() - start, 3)


if __name__ == "__main__":
    unittest.main()
