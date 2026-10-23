"""Executable and artifact regressions for the AV conformance runner."""
import contextlib
import hashlib
import io
import json
import os
import re
import shlex
import subprocess
import sys
import tempfile
import unittest
from html.parser import HTMLParser
from pathlib import Path
from unittest import mock

import av_report
import run_av_conformance as runner

ROOT = Path(__file__).resolve().parents[1]
RUNNER = ROOT / "tests/run_av_conformance.py"
STATICLIB = ROOT / "verifier/zkverify-ffi/target/release/libzkverify.a"
REGISTRY = json.loads(runner.REGISTRY.read_text())


class HTMLInventory(HTMLParser):
    def __init__(self, content):
        super().__init__()
        self.tags = []
        self.attributes = []
        self.text = []
        self.feed(content)

    def handle_starttag(self, tag, attrs):
        self.tags.append(tag)
        self.attributes.extend(attrs)

    def handle_data(self, data):
        self.text.append(data)


class AVConformanceRunnerTests(unittest.TestCase):
    def run_audit(self, *args):
        with tempfile.TemporaryDirectory() as tmp:
            report = Path(tmp) / "report.json"
            artifact = Path(tmp) / "report.html"
            proc = subprocess.run([sys.executable, str(RUNNER), "--json", str(report), *args], cwd=ROOT,
                                  text=True, capture_output=True, timeout=90)
            self.html = artifact.read_text() if artifact.exists() else ""
            return proc, json.loads(report.read_text()) if report.exists() else None

    def call_main(self, *args):
        with tempfile.TemporaryDirectory() as tmp:
            report = Path(tmp) / "report.json"
            out, err = io.StringIO(), io.StringIO()
            with mock.patch.object(sys, "argv", [str(RUNNER), "--json", str(report), *args]), contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
                code = runner.main()
            self.html = report.with_suffix(".html").read_text() if report.with_suffix(".html").exists() else ""
            return code, json.loads(report.read_text()) if report.exists() else None, err.getvalue()

    def adapter(self, payload):
        return shlex.quote(sys.executable) + " -c " + shlex.quote("print(" + repr(json.dumps(payload)) + ")")

    def payload(self):
        ids = list(dict.fromkeys(c["test"]["id"] for c in REGISTRY["clauses"] if c.get("test")))
        return {"schema_version": 1, "candidate": {"name": "external example", "revision": "a" * 40},
                "results": [{"test_id": tid, "property": "named property", "state": "PASS", "evidence": "executed external probe"} for tid in ids]}

    def assert_complete(self, report):
        expected = [c["id"] for c in REGISTRY["clauses"]]
        for candidate in report["candidates"]:
            self.assertEqual([r["clause_id"] for r in candidate["clauses"]], expected)
            self.assertEqual(sum(candidate["counts"].values()), 94)
            for row, clause in zip(candidate["clauses"], REGISTRY["clauses"]):
                if clause.get("test"):
                    self.assertEqual(row["test_id"], clause["test"]["id"])
                else:
                    self.assertEqual(row["state"], "EXCLUDED")
                    self.assertEqual(row["detail"], clause["out_of_scope_for_v1"])

    @unittest.skipUnless(STATICLIB.is_file(), "full local audit needs the Rust staticlib")
    def test_local_candidate_records_distinct_clause_and_negative_counts(self):
        proc, report = self.run_audit()
        self.assertEqual(proc.returncode, 1, proc.stdout + proc.stderr)
        local, external = report["candidates"]
        self.assertEqual(local["coverage"]["unique_clauses"], 20)
        self.assertGreaterEqual(local["coverage"]["negative_controls"], 8)
        self.assertEqual(len({r["clause_id"] for r in local["results"]}), 20)
        failure = next(r for r in local["results"] if r["clause_id"] == "AV-A.6-016")
        self.assertEqual(failure["state"], "FAIL")
        self.assertIn("candidate emits direct_post.jwt", failure["detail"])
        self.assertIn("candidate emits direct_post.jwt", self.html)
        self.assertTrue(all(r["state"] == "NOT_EXECUTED" for r in external["results"]))
        self.assert_complete(report)
        self.assertEqual(report["audit_exit_code"], proc.returncode)

    def test_help_is_available(self):
        proc = subprocess.run([sys.executable, str(RUNNER), "--help"], cwd=ROOT, text=True, capture_output=True)
        self.assertEqual(proc.returncode, 0)
        self.assertIn("--candidate-command", proc.stdout)
        self.assertIn("--html", proc.stdout)

    def test_skip_preserves_unexecuted_and_ordered_exclusions(self):
        proc, report = self.run_audit("--skip-local")
        self.assertEqual(proc.returncode, 1)
        self.assert_complete(report)
        self.assertTrue(all(r["state"] == "NOT_EXECUTED" for c in report["candidates"] for r in c["results"]))
        self.assertEqual(report["candidates"][0]["coverage"]["unique_clauses"], 0)
        inventory = HTMLInventory(self.html)
        self.assertEqual(inventory.tags.count("article"), 188)
        self.assertEqual(inventory.tags.count("details"), inventory.tags.count("summary"))
        text = " ".join(inventory.text)
        for candidate in report["candidates"]:
            positions = [text.index(row["clause_id"]) for row in candidate["clauses"]]
            self.assertEqual(positions, sorted(positions))
        self.assertIn("External reproduction is UNPERFORMED", text)
        self.assertEqual(report["external_reproduction"]["state"], "UNPERFORMED")
        self.assertTrue(report["external_reproduction"]["operator_actions"])

    def test_host_profile_and_revision_are_visible_in_both_artifacts(self):
        _, report = self.run_audit("--skip-local")
        self.assertRegex(report["repository"]["revision"], r"^[0-9a-f]{40}$")
        self.assertIsInstance(report["repository"]["dirty"], bool)
        self.assertEqual(report["profile"], json.loads((runner.REGISTRY.parent / "source.json").read_text()))
        host = report["host"]
        self.assertEqual(host["cpu"]["required_baseline"], "x86-64-v3")
        self.assertIn("observed_environment", host["rustflags"])
        self.assertIn("ffi_config", host["rustflags"])
        self.assertIn("not proven", host["rustflags"]["build_attestation"])
        self.assertEqual(host["longfellow"]["pinned_revision"], (ROOT / "verifier/zkverify-ffi/longfellow-rev.txt").read_text().strip())
        for version in ("rustc", "cargo", "jdk", "go"):
            self.assertIn(host["tools"][version]["state"], ("OK", "UNAVAILABLE"))
        text = " ".join(HTMLInventory(self.html).text)
        for value in (report["repository"]["revision"], report["profile"]["revision"], host["hostname"], host["longfellow"]["pinned_revision"]):
            self.assertIn(value, text)
        self.assertIn("mandated AV ZK path", text)
        self.assertIn("profiled, unofficial carrier", text)
        self.assertEqual(report["registry"], {"path": "docs/profiles/av/clause-registry.json", "sha256": hashlib.sha256(runner.REGISTRY.read_bytes()).hexdigest()})
        self.assertIn(report["registry"]["sha256"], text)
        self.assertIn(report["registry"]["path"], text)

    def test_all_external_states_and_untrusted_text_render_literally(self):
        payload = self.payload()
        attack = '<script>alert("x")</script><img src="https://example.invalid/x"> & text'
        payload["candidate"]["name"] = attack
        for index, row in enumerate(payload["results"]):
            row.update(state=("PASS", "FAIL", "ERROR", "NOT_EXECUTED")[index % 4], evidence=attack, property=attack)
        proc, report = self.run_audit("--skip-local", "--candidate-command", self.adapter(payload))
        self.assertEqual(proc.returncode, 1)
        self.assert_complete(report)
        external = report["candidates"][1]
        self.assertEqual({r["state"] for r in external["results"]}, {"PASS", "FAIL", "ERROR", "NOT_EXECUTED"})
        inventory = HTMLInventory(self.html)
        self.assertNotIn("script", inventory.tags)
        self.assertNotIn("img", inventory.tags)
        self.assertFalse(any(k in ("src", "href") for k, _ in inventory.attributes))
        self.assertIn(attack, " ".join(inventory.text))
        self.assertIn("&lt;script&gt;", self.html)
        for state, count in external["counts"].items():
            self.assertIn(f"{state}: {count}", self.html)

    def test_malformed_external_adapter_is_error_not_pass(self):
        proc, report = self.run_audit("--skip-local", "--candidate-command", "printf '{bad'")
        self.assertEqual(proc.returncode, 1)
        self.assertTrue(all(r["state"] == "ERROR" for r in report["candidates"][1]["results"]))
        self.assertIn("malformed adapter JSON", self.html)

    def test_missing_external_evidence_is_not_borrowed(self):
        payload = self.payload()
        payload["results"] = payload["results"][:1]
        _, report = self.run_audit("--skip-local", "--candidate-command", self.adapter(payload))
        external = report["candidates"][1]
        self.assertTrue(any(r["state"] == "PASS" for r in external["results"]))
        missing = [r for r in external["results"] if r["test_id"] != payload["results"][0]["test_id"]]
        self.assertTrue(all(r["state"] == "NOT_EXECUTED" for r in missing))
        self.assertIn("adapter omitted mapped test evidence", self.html)

    def test_duplicate_unknown_and_empty_adapter_evidence_fail_closed(self):
        for mutation in ("duplicate", "unknown", "empty", "bad_candidate"):
            with self.subTest(mutation=mutation):
                payload = self.payload()
                if mutation == "duplicate": payload["results"].append(payload["results"][0])
                if mutation == "unknown": payload["results"][0]["test_id"] = "unmapped::TestFoo"
                if mutation == "empty": payload["results"][0]["evidence"] = " "
                if mutation == "bad_candidate": payload["candidate"] = "invalid"
                proc, report = self.run_audit("--skip-local", "--candidate-command", self.adapter(payload))
                self.assertEqual(proc.returncode, 1)
                self.assertTrue(all(r["state"] == "ERROR" for r in report["candidates"][1]["results"]))
                self.assertIn("ERROR: 20", self.html)

    def test_adapter_timeout_is_error(self):
        command = shlex.quote(sys.executable) + " -c " + shlex.quote("import time; time.sleep(2)")
        _, report = self.run_audit("--skip-local", "--timeout", "1", "--candidate-command", command)
        self.assertTrue(all(r["state"] == "ERROR" for r in report["candidates"][1]["results"]))
        self.assertIn("adapter timed out", self.html)

    def test_adapter_process_failure_preserves_diagnostic(self):
        command = shlex.quote(sys.executable) + " -c " + shlex.quote('import sys; print("adapter startup cause", file=sys.stderr); print("{}"); sys.exit(3)')
        _, report = self.run_audit("--skip-local", "--candidate-command", command)
        self.assertIn("adapter startup cause", report["candidates"][1]["results"][0]["detail"])
        self.assertIn("adapter startup cause", self.html)

    def test_explicit_html_path_and_output_collision(self):
        with tempfile.TemporaryDirectory() as tmp:
            report, artifact = Path(tmp) / "evidence.json", Path(tmp) / "view.htm"
            proc = subprocess.run([sys.executable, str(RUNNER), "--skip-local", "--json", str(report), "--html", str(artifact)], capture_output=True, text=True)
            self.assertEqual(proc.returncode, 1)
            self.assertTrue(artifact.is_file())
            collision = subprocess.run([sys.executable, str(RUNNER), "--skip-local", "--json", str(report), "--html", str(report)], capture_output=True, text=True)
            self.assertEqual(collision.returncode, 2)
            self.assertEqual(json.loads(report.read_text())["audit_exit_code"], 1)

    def test_package_build_failure_does_not_borrow_other_package_error(self):
        def execute(command, **kwargs):
            if command[4] == ".":
                return subprocess.CompletedProcess(command, 1, "", "linker: missing libzkverify.a")
            symbol = "TestAVConformanceRequestParts"
            events = [{"Action": "output", "Test": symbol, "Output": "own package assertion"}, {"Action": "fail", "Test": symbol}]
            return subprocess.CompletedProcess(command, 1, "\n".join(map(json.dumps, events)), "")
        with mock.patch.object(runner.subprocess, "run", side_effect=execute):
            local = runner.local_results(REGISTRY, False, 5)
        root = next(r for r in local["results"] if r["clause_id"] == "AV-A.6-016")
        other = next(r for r in local["results"] if r["clause_id"] == "AV-A.6-005")
        self.assertEqual(root["state"], "ERROR")
        self.assertIn("missing libzkverify.a", root["detail"])
        self.assertEqual(other["state"], "FAIL")
        self.assertIn("own package assertion", other["detail"])
        self.assertNotIn("missing libzkverify.a", other["detail"])

    def test_sigill_and_missing_go_are_environment_errors(self):
        for failure in ("SIGILL: illegal instruction", OSError("go unavailable")):
            with self.subTest(failure=str(failure)):
                result = subprocess.CompletedProcess([], 1, json.dumps({"Action": "fail", "Test": "TestAVConformanceResponseMode"}), failure if isinstance(failure, str) else "")
                with mock.patch.object(runner.subprocess, "run", side_effect=failure if isinstance(failure, OSError) else None, return_value=result):
                    local = runner.local_results(REGISTRY, False, 5)
                self.assertTrue(all(r["state"] == "ERROR" for r in local["results"]))
                self.assertTrue(local["diagnostics"])
                self.assertTrue(all(d["state"] == "ERROR" for d in local["diagnostics"]))

    def test_missing_or_ambiguous_go_evidence_is_not_pass(self):
        for events, expected in (([], "NOT_EXECUTED"), ([{"Action": "pass", "Test": "TestAVConformanceResponseMode"}] * 2, "ERROR")):
            with mock.patch.object(runner.subprocess, "run", return_value=subprocess.CompletedProcess([], 0, "\n".join(map(json.dumps, events)), "")):
                local = runner.local_results(REGISTRY, False, 5)
            row = next(r for r in local["results"] if r["clause_id"] == "AV-A.6-016")
            self.assertEqual(row["state"], expected)

    def test_host_mismatch_and_unavailable_jdk_are_named(self):
        missing = {"state": "UNAVAILABLE", "revision": "unavailable", "dirty": None, "changes": []}
        with mock.patch.object(av_report, "git_record", return_value=missing), mock.patch.object(av_report, "command_record", return_value={"state": "UNAVAILABLE", "output": "tool absent"}), mock.patch.dict(os.environ, {"RUSTFLAGS": "-C target-cpu=native"}):
            host = av_report.host_record(ROOT)
        self.assertFalse(host["longfellow"]["revision_matches_pin"])
        self.assertTrue(any(d["name"] == "longfellow_revision" and d["state"] == "ERROR" for d in host["diagnostics"]))
        self.assertTrue(any(d["name"] == "jdk" and "not required" in d["detail"] for d in host["diagnostics"]))
        self.assertEqual(host["rustflags"]["observed_environment"]["RUSTFLAGS"], "-C target-cpu=native")
        self.assertEqual(host["rustflags"]["required_baseline"], "-C target-cpu=x86-64-v3")

    def test_cli_startup_error_is_rendered_as_environment_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            fake_go = Path(tmp) / "go"
            fake_go.write_text('#!/bin/sh\nif [ "$1" = version ]; then echo "go fake test tool"; exit 0; fi\necho "SIGILL: illegal instruction in startup" >&2\nexit 132\n')
            fake_go.chmod(0o755)
            with mock.patch.dict(os.environ, {"PATH": tmp + os.pathsep + os.environ["PATH"]}):
                proc, report = self.run_audit()
        self.assertEqual(proc.returncode, 1)
        local = report["candidates"][0]
        self.assertTrue(all(row["state"] == "ERROR" for row in local["results"]))
        self.assertGreater(report["summary"]["environment_errors"], 0)
        self.assertIn("SIGILL: illegal instruction in startup", self.html)
        self.assertIn("Environment diagnostics", self.html)
        self.assertEqual(local["counts"]["FAIL"], 0)

    def test_incompatible_architecture_has_cpu_environment_diagnostic(self):
        with mock.patch.object(av_report.platform, "machine", return_value="aarch64"):
            host = av_report.host_record(ROOT)
        self.assertEqual(host["cpu"]["baseline_state"], "MISMATCH")
        self.assertTrue(any(d["name"] == "cpu_baseline" and d["state"] == "ERROR" for d in host["diagnostics"]))

    def test_supplementary_probe_outcomes_survive_and_fail_audit(self):
        supplementary = "TestAVConformanceSupplementary"
        discover = runner.symbols
        known = {t["symbol"] for t in REGISTRY["tests"]}
        known.update(s for path in runner.PACKAGES for s in discover(ROOT / path))
        known.add(supplementary)
        def discover_extra(path):
            return discover(path) + ([supplementary] if path.parent == ROOT / "verifier/go" else [])
        for terminal, expected in (("fail", "FAIL"), ("skip", "NOT_EXECUTED"), ("ambiguous", "ERROR")):
            with self.subTest(terminal=terminal):
                def execute(command, **kwargs):
                    selected = [s for s in known if re.fullmatch(command[-1], s)]
                    events = []
                    for symbol in selected:
                        if symbol == supplementary:
                            events.append({"Action": "output", "Test": symbol, "Output": "supplementary named assertion"})
                            actions = ["pass", "fail"] if terminal == "ambiguous" else [terminal]
                        else:
                            actions = ["pass"]
                        events.extend({"Action": action, "Test": symbol} for action in actions)
                    if command[4] == "./oid4vp":
                        for index in range(8):
                            events.append({"Action": "pass", "Test": f"TestAVProfileRequiredParameters/negative_synthetic_{index}"})
                    return subprocess.CompletedProcess(command, 1 if supplementary in selected and terminal != "skip" else 0, "\n".join(map(json.dumps, events)), "")
                with mock.patch.object(runner, "symbols", side_effect=discover_extra), mock.patch.object(runner.subprocess, "run", side_effect=execute):
                    local = runner.local_results(REGISTRY, False, 5)
                external = {"name": "synthetic independent adapter", "revision": "a" * 40, "results": local["results"]}
                self.assertTrue(all(r["state"] == "PASS" for r in local["results"]))
                self.assertEqual(local["coverage"], {"unique_clauses": 20, "negative_controls": 8})
                probe = next(r for r in local["probe_results"] if r["property"] == supplementary)
                self.assertTrue(probe["supplementary"])
                self.assertEqual(probe["state"], expected)
                self.assertIn("supplementary named assertion", probe["detail"])
                with mock.patch.object(runner, "local_results", return_value=local), mock.patch.object(runner, "external_results", return_value=external):
                    code, report, _ = self.call_main()
                self.assertEqual(code, 1)
                self.assertTrue(report["summary"]["thresholds_met"])
                self.assertEqual(report["summary"]["supplementary_failed_results"], 1)
                self.assertEqual(report["summary"]["failed_results"], 1)
                text = " ".join(HTMLInventory(self.html).text)
                self.assertIn(supplementary, text)
                self.assertIn("supplementary named assertion", text)
                self.assertIn("Go commands, package exit codes and output", text)
                self.assertIn('"returncode": ' + ("0" if terminal == "skip" else "1"), text)
                self.assertIn('"-count=1"', text)
                self.assertIn("./oid4vp::TestAVProfileRequiredParameters/negative_synthetic_7", text)

    def test_negative_controls_count_only_unambiguous_passes_from_completed_packages(self):
        symbol = "TestAVConformanceShared"
        paths = ["verifier/go/a_test.go", "verifier/go/oid4vp/a_test.go", "verifier/go/zk/a_test.go", "verifier/go/internal/jose/a_test.go"]
        registry = {"tests": [{"id": p + "::" + symbol, "path": p, "symbol": symbol} for p in paths], "clauses": []}
        def execute(command, **kwargs):
            events = [{"Action": action, "Test": symbol + "/negative_" + label}
                      for action, label in (("pass", "same"), ("fail", "failed"), ("skip", "skipped"))]
            events.append({"Action": "fail", "Test": symbol})
            output = "\n".join(map(json.dumps, events))
            if command[4] == "./internal/jose":
                raise subprocess.TimeoutExpired(command, 5, output=output)
            return subprocess.CompletedProcess(command, 1, output, "SIGILL startup" if command[4] == "./zk" else "")
        with mock.patch.object(runner, "PACKAGES", {}), mock.patch.object(runner.subprocess, "run", side_effect=execute):
            local = runner.local_results(registry, False, 5)
        self.assertEqual(local["coverage"]["negative_controls"], 2)
        self.assertEqual(local["negative_results"][".::" + symbol + "/negative_same"], "PASS")
        self.assertEqual(local["negative_results"]["./oid4vp::" + symbol + "/negative_same"], "PASS")
        self.assertNotIn("./zk::" + symbol + "/negative_same", local["negative_count_eligible"])
        self.assertNotIn("./internal/jose::" + symbol + "/negative_same", local["negative_count_eligible"])
        self.assertEqual(local["negative_results"][".::" + symbol + "/negative_failed"], "FAIL")
        self.assertEqual(local["negative_results"][".::" + symbol + "/negative_skipped"], "NOT_EXECUTED")
        # Reuse the report fixture to verify every named negative outcome is visible in HTML.
        _, report = self.run_audit("--skip-local")
        report["candidates"][0].update(local)
        text = " ".join(HTMLInventory(av_report.render_html(report, "report.json")).text)
        for name, state in local["negative_results"].items():
            self.assertIn(json.dumps(name) + ": " + json.dumps(state), text)

    def test_unavailable_git_provenance_is_inspectable_error(self):
        unavailable = {"revision": "unavailable", "dirty": None, "changes": [], "state": "UNAVAILABLE"}
        with mock.patch.object(runner, "git_record", return_value=unavailable):
            code, report, _ = self.call_main("--skip-local")
        self.assertEqual(code, 1)
        self.assertEqual(report["repository"], unavailable)
        self.assertTrue(any(d["name"] == "repository_provenance" and d["state"] == "ERROR" for d in report["host"]["diagnostics"]))
        self.assertIn("repository_provenance", self.html)
        self.assertGreater(report["summary"]["environment_errors"], 0)

    def test_changed_registry_is_refused_before_probe_execution(self):
        changed = json.loads(json.dumps(REGISTRY))
        del changed["clauses"][0]["out_of_scope_for_v1"]
        validate = runner.check_av_registry.validate
        with mock.patch.object(runner.check_av_registry, "validate", side_effect=lambda root: validate(root, registry=changed)) as check, mock.patch.object(runner, "local_results") as probes:
            code, report, error = self.call_main("--skip-local")
        check.assert_called_once_with(ROOT)
        probes.assert_not_called()
        self.assertEqual(code, 2)
        self.assertIsNone(report)
        self.assertIn("AV artifact/source validation ERROR", error)

    def test_go_effective_environment_captures_overlay_workspace_and_compiler(self):
        expected = {key: "effective " + key for key in av_report.GO_ENV_KEYS}
        expected["GOFLAGS"] = "-overlay=/tmp/effective-overlay.json -tags=custom"
        command_record = av_report.command_record
        def command(command, cwd):
            if command[:3] == ["go", "env", "-json"]:
                self.assertEqual(cwd, ROOT / "verifier/go")
                self.assertEqual(set(command[3:]), set(av_report.GO_ENV_KEYS))
                return {"command": command, "returncode": 0, "state": "OK", "output": json.dumps(expected)}
            return command_record(command, cwd)
        with mock.patch.object(av_report, "command_record", side_effect=command), mock.patch.dict(os.environ, {"GOFLAGS": "-overlay=/tmp/observed-overlay.json"}):
            host = av_report.host_record(ROOT)
        self.assertEqual(host["go_environment"]["values"], expected)
        self.assertEqual(host["go_environment"]["observed_environment"]["GOFLAGS"], "-overlay=/tmp/observed-overlay.json")
        self.assertIn("not an attestation", host["go_environment"]["assessment_limit"])

    def test_required_baseline_does_not_claim_observed_config_matches(self):
        read_text = Path.read_text
        config = ROOT / "verifier/zkverify-ffi/.cargo/config.toml"
        changed = '[build]\nrustflags = ["-C", "target-cpu=native"]\n'
        def read(path, *args, **kwargs):
            return changed if path == config else read_text(path, *args, **kwargs)
        with mock.patch.object(Path, "read_text", new=read):
            host = av_report.host_record(ROOT)
        self.assertEqual(host["rustflags"]["required_baseline"], "-C target-cpu=x86-64-v3")
        self.assertEqual(host["rustflags"]["ffi_config"], changed)
        self.assertNotIn("configured_baseline", host["rustflags"])

    def test_host_input_read_errors_return_validation_exit_without_artifacts(self):
        read_text = Path.read_text
        config = ROOT / "verifier/zkverify-ffi/.cargo/config.toml"
        for failure in (FileNotFoundError("missing FFI configuration"), PermissionError("unreadable FFI configuration"), UnicodeError("invalid FFI configuration text")):
            with self.subTest(failure=type(failure).__name__):
                def read(path, *args, **kwargs):
                    if path == config:
                        raise failure
                    return read_text(path, *args, **kwargs)
                with mock.patch.object(Path, "read_text", new=read), mock.patch.object(runner, "local_results") as probes:
                    code, report, error = self.call_main("--skip-local")
                self.assertEqual(code, 2)
                self.assertIsNone(report)
                probes.assert_not_called()
                self.assertIn("AV artifact/source validation ERROR", error)
                self.assertNotIn("Traceback", error)

    def test_actual_git_clean_modified_and_untracked_evidence(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            def git(*args):
                return subprocess.run(["git", *args], cwd=root, capture_output=True, text=True, check=True).stdout.strip()
            git("init", "-q")
            source = root / "source.py"
            source.write_text("original\n")
            git("add", "source.py")
            git("-c", "user.name=AV test", "-c", "user.email=av-test@example.invalid", "-c", "core.hooksPath=/dev/null", "commit", "--no-gpg-sign", "-qm", "fixture")
            revision = git("rev-parse", "HEAD")
            clean = av_report.git_record(root)
            self.assertRegex(revision, r"^[0-9a-f]{40}$")
            self.assertEqual(clean, {"revision": revision, "dirty": False, "changes": [], "state": "OK"})
            source.write_text("modified\n")
            modified = av_report.git_record(root)
            self.assertEqual(modified["revision"], revision)
            self.assertTrue(modified["dirty"])
            self.assertIn(" M source.py", modified["changes"])
            source.write_text("original\n")
            (root / "untracked.py").write_text("new source\n")
            untracked = av_report.git_record(root)
            self.assertEqual(untracked["revision"], revision)
            self.assertTrue(untracked["dirty"])
            self.assertIn("?? untracked.py", untracked["changes"])

    def test_hardlinked_artifact_paths_are_refused_before_writes(self):
        with tempfile.TemporaryDirectory() as tmp:
            report, artifact = Path(tmp) / "report.json", Path(tmp) / "report.html"
            report.write_text("original evidence")
            artifact.hardlink_to(report)
            proc = subprocess.run([sys.executable, str(RUNNER), "--skip-local", "--json", str(report), "--html", str(artifact)], capture_output=True, text=True)
            self.assertEqual(proc.returncode, 2)
            self.assertIn("must be different paths", proc.stderr)
            self.assertEqual(report.read_text(), "original evidence")
            self.assertEqual(artifact.read_text(), "original evidence")

    def test_complete_candidate_distinguishes_ambiguous_rows(self):
        mapping = next(c for c in REGISTRY["clauses"] if c.get("test"))
        entry = {"clause_id": mapping["id"], "state": "PASS"}
        candidate = av_report.complete_candidate({"results": [entry, entry]}, REGISTRY)
        row = next(r for r in candidate["clauses"] if r["clause_id"] == mapping["id"])
        self.assertEqual(row["state"], "ERROR")
        self.assertEqual(row["detail"], "ambiguous mapped result")


if __name__ == "__main__":
    unittest.main()
