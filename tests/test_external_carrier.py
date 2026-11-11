import copy
import hashlib
import json
import os
import signal
import sys
import tempfile
import time
import unittest
from pathlib import Path
from unittest.mock import patch

import run_carrier_conformance as local
import run_external_carrier as external
from test_carrier_conformance import record


def payload():
    return {
        "schema_version": 1,
        "target": {"name": external.NAME, "revision": external.REVISION},
        "cases": [
            {**record(case_id), "evidence": "observed external assertion"}
            for case_id in local.CASE_IDS
        ],
        "carrier_vectors": [
            {
                "carrier": carrier,
                "result": "PASS",
                "evidence": "observed external vector",
            }
            for carrier in local.EXPECTED_CARRIERS
        ],
    }


def fake_process(body=None, code=0, error="", revision=external.REVISION):
    def process(name, argv, cwd, timeout, output_limit, env=None):
        output = {
            "checkout-revision": revision + "\n",
            "post-preparation-revision": revision + "\n",
            "post-adapter-revision": revision + "\n",
            "post-adapter-cleanliness": "",
            "checkout-origin": external.URL + "\n",
            "checkout-cleanliness": "",
        }.get(name, "")
        returncode = (
            code if name.startswith("preparation-") or name == "external-adapter" else 0
        )
        if name == "external-adapter" and body is not None:
            if env is None:
                raise ValueError("adapter environment missing")
            data = body if isinstance(body, bytes) else json.dumps(body).encode()
            Path(env["EE_EXTERNAL_CARRIER_EVIDENCE"]).write_bytes(data)
        return {
            "name": name,
            "command": list(argv),
            "cwd": str(cwd),
            "returncode": returncode,
            "status": "ERROR" if error or returncode else "PASS",
            "output": output,
            "detail": error,
        }

    return process


class ExternalCarrierTests(unittest.TestCase):
    def setUp(self):
        self._provenance_function = external.suite_provenance
        provenance = patch.object(
            external, "suite_provenance", return_value={"test": True}
        )
        provenance.start()
        self.addCleanup(provenance.stop)

    def attempt(self, body=None, **kwargs):
        return external.run_attempt(
            Path("/external"), adapter=["adapter"], process=fake_process(body, **kwargs)
        )

    def test_complete_accounted_external_run_passes(self):
        report = self.attempt(payload())
        self.assertEqual(report["status"], "PASS")
        self.assertEqual(len(report["cases"]), 19)
        self.assertEqual(len(report["carrier_vectors"]), 2)
        self.assertIn("independent operator", report["evidence_scope"])
        self.assertIn(external.REVISION, external.markdown(report))

    def test_failed_assertions_are_never_blocked_even_on_nonzero_exit(self):
        for location in ["cases", "carrier_vectors"]:
            body = payload()
            body[location][0]["result"] = "FAIL"
            report = self.attempt(body, code=1)
            self.assertEqual(report["status"], "FAIL")
            self.assertEqual(report[location][0]["result"], "FAIL")
            self.assertIn("FAIL", report["raw_adapter_evidence"])

    def test_local_pass_and_build_success_cannot_satisfy_external_gate(self):
        report = external.run_attempt(
            Path("/external"), preparation=[["build"]], process=fake_process()
        )
        self.assertEqual(report["status"], "BLOCKED")
        self.assertTrue(
            all(
                row["result"] == "NOT_EXECUTED"
                for row in report["cases"] + report["carrier_vectors"]
            )
        )
        report = self.attempt()
        self.assertEqual(report["status"], "ERROR")
        self.assertIn("without required evidence", report["detail"])
        self.assertEqual(
            self.attempt({"suites": [{"status": "PASS"}]})["status"], "ERROR"
        )

    def test_invalid_evidence_matrix_is_error_with_original_retained(self):
        bad = []
        for key in ["cases", "carrier_vectors", "target", "schema_version"]:
            item = payload()
            item.pop(key)
            bad.append(item)
        for value in [True, "1", 2, None]:
            item = payload()
            item["schema_version"] = value
            bad.append(item)
        for key, value in [
            ("case_id", "20"),
            ("case_id", []),
            ("carriers", ["unknown"]),
            ("declared_response_mode", "wrong"),
            ("declared_outcome", "wrong"),
            ("binding_property", "wrong"),
            ("result", []),
            ("evidence", ""),
        ]:
            item = payload()
            item["cases"][0][key] = value
            bad.append(item)
        for mutation in [
            "missing",
            "duplicate",
            "vector-missing",
            "vector-duplicate",
            "wrong-revision",
            "no-evidence",
            "vector-result",
        ]:
            item = payload()
            if mutation == "missing":
                item["cases"].pop()
            elif mutation == "duplicate":
                item["cases"][-1] = copy.deepcopy(item["cases"][0])
            elif mutation == "vector-missing":
                item["carrier_vectors"].pop()
            elif mutation == "vector-duplicate":
                item["carrier_vectors"][1] = item["carrier_vectors"][0]
            elif mutation == "wrong-revision":
                item["target"]["revision"] = "0" * 40
            elif mutation == "no-evidence":
                item["carrier_vectors"][0].pop("evidence")
            else:
                item["carrier_vectors"][0]["result"] = []
            bad.append(item)
        for body in bad:
            with self.subTest(body=body):
                report = self.attempt(body)
                self.assertEqual(report["status"], "ERROR")
                self.assertIsNotNone(report["raw_adapter_evidence"])

    def test_malformed_json_encoding_duplicate_keys_and_nonfinite_are_errors(self):
        for body in [
            b"[]",
            b"{",
            b"\xff",
            b'{"schema_version":1,"schema_version":1}',
            b'{"x":NaN}',
            b'{"x":"\\ud800"}',
        ]:
            self.assertEqual(self.attempt(body)["status"], "ERROR", body)

    def test_revision_and_origin_mismatch_refuse_execution(self):
        report = self.attempt(payload(), revision="0" * 40)
        self.assertEqual(report["status"], "ERROR")
        self.assertEqual(len(report["executions"]), 1)

        def process(name, *args):
            row = fake_process()(name, *args)
            if name == "checkout-origin":
                row["output"] = "private-target"
            return row

        self.assertEqual(
            external.run_attempt(Path("/external"), process=process)["status"], "ERROR"
        )

    def test_preparation_and_adapter_unavailable_are_blocked(self):
        for detail in ["startup error", "crash", "timeout", "output limit"]:
            report = self.attempt(code=1, error=detail)
            # Checkout failure is a setup failure too.
            self.assertEqual(report["status"], "BLOCKED")
            self.assertIn(detail, report["detail"])
        report = external.run_attempt(
            Path("/external"), preparation=[["build"]], process=fake_process(code=1)
        )
        self.assertEqual(report["status"], "BLOCKED")
        self.assertEqual(report["executions"][-1]["returncode"], 1)

    def test_adapter_stage_timeout_and_crash_are_blocked_without_assertions(self):
        for diagnostic in ["could not start child", "timeout", "output limit", "crash"]:

            def process(name, *args, diagnostic=diagnostic):
                row = fake_process()(name, *args)
                if name == "external-adapter":
                    row.update(status="ERROR", returncode=-9, detail=diagnostic)
                return row

            report = external.run_attempt(
                Path("/external"), adapter=["adapter"], process=process
            )
            self.assertEqual(report["status"], "BLOCKED")
            self.assertIn(diagnostic, report["detail"])
            self.assertTrue(
                all(row["result"] == "NOT_EXECUTED" for row in report["cases"])
            )

    def test_evidence_symlink_is_error_without_reading_target(self):
        def process(name, argv, cwd, timeout, output_limit, env=None):
            row = fake_process()(name, argv, cwd, timeout, output_limit, env)
            if name == "external-adapter" and env is not None:
                Path(env["EE_EXTERNAL_CARRIER_EVIDENCE"]).symlink_to("/missing-target")
            return row

        report = external.run_attempt(
            Path("/external"), adapter=["adapter"], process=process
        )
        self.assertEqual(report["status"], "ERROR")
        self.assertIn("Invalid adapter evidence", report["detail"])

    def test_passing_claims_do_not_hide_adapter_process_failure(self):
        self.assertEqual(self.attempt(payload(), code=1)["status"], "ERROR")

    def test_evidence_size_is_bounded(self):
        report = external.run_attempt(
            Path("/external"),
            adapter=["adapter"],
            output_limit=10,
            process=fake_process(payload()),
        )
        self.assertEqual(report["status"], "ERROR")
        self.assertIn("limit", report["detail"])

    def test_real_start_failure_and_crash_have_diagnostics(self):
        for argv in [
            ["/missing-external-carrier-command"],
            [
                sys.executable,
                "-c",
                "import os,signal; os.kill(os.getpid(), signal.SIGTERM)",
            ],
        ]:
            result = external.execute("adapter", argv, Path.cwd(), 1, 256)
            self.assertEqual(result["status"], "ERROR")
            self.assertNotEqual(result["returncode"], 0)

    def test_real_timeout_reaps_process_group_descendant(self):
        with tempfile.TemporaryDirectory() as tmp:
            pidfile = Path(tmp) / "pid"
            body = (
                "import os,time; p=os.fork(); open("
                + repr(str(pidfile))
                + ',"w").write(str(p)) if p else None; time.sleep(30)'
            )
            result = external.execute(
                "adapter", [sys.executable, "-c", body], Path.cwd(), 3, 1024
            )
            self.assertEqual(result["status"], "ERROR")
            self.assertIn("timeout", result["detail"])
            pid = int(pidfile.read_text())
            for _ in range(20):
                stat = Path(f"/proc/{pid}/stat")
                if not stat.exists() or stat.read_text().split()[2] == "Z":
                    break
                time.sleep(0.01)
            else:
                os.kill(pid, signal.SIGKILL)
                self.fail("timed out descendant is still running")

    def test_real_output_limit(self):
        result = external.execute(
            "adapter", [sys.executable, "-c", 'print("x" * 10000)'], Path.cwd(), 1, 16
        )
        self.assertEqual(result["status"], "ERROR")
        self.assertIn("byte limit", result["detail"])

    def test_real_adapter_writes_validated_evidence_separately_from_logs(self):
        with tempfile.TemporaryDirectory() as tmp:
            checkout = Path(tmp)
            body = payload()
            script = checkout / "adapter.py"
            script.write_text(
                "import os,pathlib; print('adapter diagnostics'); "
                "pathlib.Path(os.environ['EE_EXTERNAL_CARRIER_EVIDENCE'])"
                ".write_text(" + repr(json.dumps(body)) + ")"
            )

            def process(name, argv, cwd, timeout, output_limit, env=None):
                if name == "external-adapter":
                    return external.execute(name, argv, cwd, timeout, output_limit, env)
                return fake_process()(name, argv, cwd, timeout, output_limit, env)

            report = external.run_attempt(
                checkout, adapter=[sys.executable, str(script)], process=process
            )
            self.assertEqual(report["status"], "PASS")
            self.assertIn("adapter diagnostics", report["executions"][3]["output"])
            self.assertEqual(
                report["cases"][0]["evidence"], "observed external assertion"
            )

    def test_post_execution_revision_change_cannot_pass(self):
        def process(name, *args):
            row = fake_process(payload())(name, *args)
            if name == "post-adapter-revision":
                row["output"] = "changed revision"
            return row

        report = external.run_attempt(
            Path("/external"), adapter=["adapter"], process=process
        )
        self.assertEqual(report["status"], "ERROR")

    def test_dirty_checkout_boundaries_refuse_attribution(self):
        for boundary in [
            "checkout-cleanliness",
            "post-preparation-cleanliness",
            "post-adapter-cleanliness",
        ]:
            seen = []

            def process(name, *args, boundary=boundary, seen=seen):
                seen.append(name)
                row = fake_process(payload())(name, *args)
                if name == boundary:
                    row["output"] = " M source.kt"
                return row

            report = external.run_attempt(
                Path("/external"),
                preparation=[["build"]],
                adapter=["adapter"],
                process=process,
            )
            self.assertEqual(report["status"], "ERROR", boundary)
            if boundary != "post-adapter-cleanliness":
                self.assertNotIn("external-adapter", seen)

    def test_attribution_error_outweighs_failed_assertion_and_keeps_rows(self):
        body = payload()
        body["cases"][0]["result"] = "FAIL"

        def process(name, *args):
            row = fake_process(body)(name, *args)
            if name == "post-adapter-revision":
                row["output"] = "wrong"
            return row

        report = external.run_attempt(
            Path("/external"), adapter=["adapter"], process=process
        )
        self.assertEqual(report["status"], "ERROR")
        self.assertEqual(report["cases"][0]["result"], "FAIL")
        self.assertIn("FAIL", report["raw_adapter_evidence"])

    def test_fifo_and_symlink_replacement_cannot_block_read(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "evidence"
            os.mkfifo(path)
            with self.assertRaises(ValueError):
                external.read_evidence(path, 100)
            path.unlink()
            path.write_bytes(b"original")
            real_open = os.open

            def swapped_open(name, flags):
                path.unlink()
                path.symlink_to(Path(tmp) / "missing")
                return real_open(name, flags)

            with (
                patch.object(external.os, "open", swapped_open),
                self.assertRaises(OSError),
            ):
                external.read_evidence(path, 100)
            path.unlink()
            path.write_bytes(b"original")
            real_fstat = os.fstat

            def replace_after_open(fd):
                path.unlink()
                os.mkfifo(path)
                return real_fstat(fd)

            with patch.object(external.os, "fstat", replace_after_open):
                self.assertEqual(external.read_evidence(path, 100), b"original")

    def test_main_exit_mapping_writes_both_reports(self):
        for state, code in [("PASS", 0), ("FAIL", 1), ("BLOCKED", 2), ("ERROR", 3)]:
            with tempfile.TemporaryDirectory() as tmp:
                root = Path(tmp)
                report = external.base_report(root, 1, 1000)
                report["status"] = state
                with patch.object(external, "run_attempt", return_value=report):
                    actual = external.main(
                        [
                            "--checkout",
                            tmp,
                            "--json",
                            str(root / "report.json"),
                            "--markdown",
                            str(root / "report.md"),
                        ]
                    )
                self.assertEqual(actual, code)
                self.assertEqual(
                    json.loads((root / "report.json").read_text())["status"], state
                )
                self.assertIn("Status: " + state, (root / "report.md").read_text())

    def test_main_metadata_errors_are_named_reported_errors(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            metadata = root / "metadata.json"
            for content in [None, "{", "{}"]:
                if content is not None:
                    metadata.write_text(content)
                with patch.object(external, "TARGET_PATH", metadata):
                    actual = external.main(
                        [
                            "--checkout",
                            tmp,
                            "--json",
                            str(root / "report.json"),
                            "--markdown",
                            str(root / "report.md"),
                        ]
                    )
                self.assertEqual(actual, 3)
                report = json.loads((root / "report.json").read_text())
                self.assertEqual(report["target"]["name"], external.NAME)
                self.assertEqual(report["status"], "ERROR")
                self.assertIn("metadata/input error", report["detail"])
                self.assertIn("Status: ERROR", (root / "report.md").read_text())

    def test_hostile_markdown_stays_in_escaped_prose_and_safe_blocks(self):
        report = self.attempt(payload())
        hostile = "~~~\n# escaped\n<script>alert(1)</script>\n~~~~~"
        report["detail"] = hostile
        report["executions"][0]["command"] = [hostile]
        report["executions"][0]["output"] = hostile
        report["raw_adapter_evidence"] = hostile
        rendered = external.markdown(report)
        self.assertIn("~~~~~~text\n" + hostile + "\n~~~~~~", rendered)
        self.assertIn("~~~~~~json\n" + hostile + "\n~~~~~~", rendered)
        self.assertIn("Detail: &#126;&#126;&#126;", rendered)
        self.assertNotIn("Command: ~~~", rendered)
        self.assertNotIn("Detail: ~~~", rendered)

    def test_successful_parent_cleanup_kills_silent_descendant(self):
        with tempfile.TemporaryDirectory() as tmp:
            pidfile = Path(tmp) / "pid"
            body = (
                "import os,time; p=os.fork(); "
                "open(" + repr(str(pidfile)) + ", 'w').write(str(p)) if p else None; "
                "os._exit(0) if p else None; os.close(1); os.close(2); time.sleep(30)"
            )
            result = external.execute(
                "silent-child", [sys.executable, "-c", body], Path.cwd(), 3, 1024
            )
            self.assertEqual(result["status"], "PASS")
            pid = int(pidfile.read_text())
            for _ in range(50):
                status = Path(f"/proc/{pid}/stat")
                if not status.exists() or status.read_text().split()[2] == "Z":
                    break
                time.sleep(0.01)
            else:
                os.kill(pid, signal.SIGKILL)
                self.fail("successful parent left silent descendant running")

    def test_suite_provenance_observes_revision_dirty_state_and_input_hashes(self):
        # Exercise the real collector independently of the attempt test fixture.
        collector = self._provenance_function

        def observed(name, *args):
            return {
                "name": name,
                "returncode": 0,
                "status": "PASS",
                "output": "a" * 40 if name == "suite-revision" else " M tests/e2e.sh",
            }

        with patch.object(external, "execute", observed):
            evidence = collector()
        self.assertEqual(evidence["revision"], "a" * 40)
        self.assertTrue(evidence["dirty"])
        self.assertEqual(
            evidence["sha256"]["tests/e2e.sh"],
            hashlib.sha256((external.ROOT / "tests/e2e.sh").read_bytes()).hexdigest(),
        )
        self.assertIn("tests/vectors/carrier-v1/manifest.json", evidence["sha256"])
        self.assertIn("tests/run_external_carrier.py", evidence["sha256"])

    def test_invalid_argv_still_uses_parser_error(self):
        with tempfile.TemporaryDirectory() as tmp:
            with self.assertRaises(SystemExit) as caught:
                external.main(
                    [
                        "--checkout",
                        tmp,
                        "--prepare-command",
                        "'",
                        "--json",
                        tmp + "/report.json",
                        "--markdown",
                        tmp + "/report.md",
                    ]
                )
            self.assertEqual(caught.exception.code, 2)
            self.assertFalse((Path(tmp) / "report.json").exists())

    def test_target_metadata_is_reviewed_and_offline(self):
        self.assertEqual(external.target_metadata()["revision"], external.REVISION)
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "target.json"
            path.write_text("{}")
            with (
                patch.object(external, "TARGET_PATH", path),
                self.assertRaises(ValueError),
            ):
                external.target_metadata()


if __name__ == "__main__":
    unittest.main()
