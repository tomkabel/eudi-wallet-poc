#!/usr/bin/env python3
"""Run the existing E2E assertions and committed carrier vector suites.

The E2E cases exercise the existing HTTP/proof flow. The report records the
observed carriers and the declared response mode and outcome of each case. The vector layer separately checks both committed
carrier encodings against the same proof. Local results are repository evidence
only; they do not establish independent-wallet behavior or external conformance.
"""

from __future__ import annotations

import argparse
from datetime import date
import hashlib
import json
import os
import re
import selectors
import signal
import subprocess
import sys
import tempfile
import time
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Callable, Sequence

from check_carrier_versions import validate_carrier_versions


RESPONSE_MODES = {"direct_post", "direct_post.jwt", "not-applicable", "NOT_OBSERVED"}
RESULTS = {"PASS", "FAIL"}
OUTCOMES = {
    "proof-verified",
    "transcript-binding-refused",
    "replay-refused",
    "false-predicate-refused",
    "prover-guard-refused",
    "prover-guard-failed",
    "response-key-published",
    "encrypted-wire-assertion",
    "decryption-failed",
    "plain-response-refused",
    "encrypted-default-confirmed",
    "multi-credential-proof-verified",
    "missing-entry",
    "unknown-carrier",
    "malformed-envelope",
    "no-presentation",
}
EXPECTED_CARRIERS = ("interim-json", "mso-mdoc-zk-cbor")
EXPECTED_PATH = "openid4vp-interim-poc"
EXPECTED_VECTOR_FILES = {
    "device_response.json.b64",
    "device_response.cbor.b64",
    "device_response.cbor",
    "device_response_transcript.bin",
    "request.json",
}
EXPECTED_MANIFEST_PROVENANCE = {
    "generated_by": "tools/build_cbor_fixture.py",
    "source_fixture": "verifier/go/zk/testdata/step8-7-openid4vp-zk",
    "source": "multipaz 0.99.0 wallet-generated zkDocument (step 8.7)",
}
EXPECTED_CASE_METADATA = {
    "1": (("interim-json",), "direct_post", ("proof-verified",)),
    "2": (("interim-json",), "direct_post", ("transcript-binding-refused",)),
    "3": (("interim-json",), "direct_post", ("replay-refused",)),
    "4": (("interim-json",), "direct_post", ("false-predicate-refused",)),
    "5": ((), "direct_post", ("prover-guard-refused", "prover-guard-failed")),
    "6": ((), "direct_post.jwt", ("response-key-published",)),
    "7": (("interim-json",), "direct_post.jwt", ("proof-verified",)),
    "8": ((), "direct_post.jwt", ("encrypted-wire-assertion",)),
    "9": ((), "direct_post.jwt", ("decryption-failed",)),
    "10": ((), "direct_post", ("plain-response-refused",)),
    "11": ((), "direct_post.jwt", ("encrypted-default-confirmed",)),
    "12": (("interim-json", "interim-json"), "direct_post", ("multi-credential-proof-verified",)),
    "13": ((), "direct_post", ("missing-entry",)),
    "14": ((), "direct_post", ("unknown-carrier",)),
    "15": ((), "direct_post", ("malformed-envelope",)),
    "16": (("interim-json",), "direct_post.jwt", ("transcript-binding-refused",)),
    "17": (("interim-json",), "direct_post.jwt", ("transcript-binding-refused",)),
    "18": (("interim-json",), "direct_post.jwt", ("transcript-binding-refused",)),
    "19": (("mso-mdoc-zk-cbor",), "direct_post", ("no-presentation",)),
}
EXPECTED_PROPERTIES = {
    "2": "nonce", "3": "single-use", "4": "predicate-value",
    "16": "client_id", "17": "response_uri", "18": "jwk_thumbprint",
}
CASE_IDS = tuple(EXPECTED_CASE_METADATA)
CASE_ECHO = re.compile(r"^\s*echo\s+[\"']?(\d+)\.", re.MULTILINE)
# Go prints "--- FAIL: Test"; tests/e2e.sh prints "  FAIL  msg"; go package failures print "FAIL\tpkg".
FAIL_MARKER = re.compile(r"(?m)(?:^|\s)FAIL(?::|\s|$)")
COMMAND_TIMEOUT_SECONDS = 900
COMMAND_OUTPUT_LIMIT_BYTES = 4 * 1024 * 1024


@dataclass
class SuiteResult:
    name: str
    status: str
    command: list[str]
    returncode: int | None
    output: str = ""
    detail: str = ""


def _json_object(pairs: list[tuple[str, object]]) -> dict[str, object]:
    result: dict[str, object] = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON key {key!r}")
        result[key] = value
    return result


def decode_case_records(raw: str) -> tuple[list[dict[str, object]], list[str]]:
    """Decode JSONL records, retaining malformed-line diagnostics."""
    records: list[dict[str, object]] = []
    problems: list[str] = []
    for line_number, line in enumerate(raw.splitlines(), start=1):
        if not line.strip():
            continue
        try:
            item = json.loads(line, object_pairs_hook=_json_object)
        except (json.JSONDecodeError, ValueError) as exc:
            problems.append(f"line {line_number}: malformed case record: {exc}")
            continue
        if not isinstance(item, dict):
            problems.append(f"line {line_number}: case record is not a JSON object")
            continue
        records.append(item)
    return records, problems


def read_case_records(path: Path) -> tuple[list[dict[str, object]], list[str]]:
    try:
        raw = path.read_bytes().decode("utf-8", errors="strict")
    except UnicodeError as exc:
        return [], [f"case JSONL is not valid UTF-8: {exc}"]
    except OSError as exc:
        return [], [f"case JSONL could not be read: {exc}"]
    return decode_case_records(raw)


def discover_case_ids(script: str) -> tuple[str, ...]:
    """Read the numbered assertion inventory from tests/e2e.sh."""
    return tuple(CASE_ECHO.findall(script))


def validate_case_records(
    records: Sequence[dict[str, object]], initial_problems: Sequence[str] = (),
    case_ids: Sequence[str] = CASE_IDS,
    expected_metadata: dict[str, tuple[tuple[str, ...], str, tuple[str, ...]]] = EXPECTED_CASE_METADATA,
) -> tuple[list[dict[str, object]], list[str]]:
    """Return one fail-closed row per known case and all inventory problems."""
    problems = list(initial_problems)
    by_id: dict[str, dict[str, str]] = {
        case_id: {
            "case_id": case_id,
            "path_label": "NOT_OBSERVED",
            "carrier": "NOT_OBSERVED",
            "carriers": [],
            "declared_response_mode": "NOT_OBSERVED",
            "declared_outcome": "child-did-not-report",
            "result": "NOT_EXECUTED",
            "binding_property": "NOT_OBSERVED",
            "assertion_result": "NOT_EXECUTED",
        }
        for case_id in case_ids
    }
    seen: set[str] = set()
    for index, record in enumerate(records, start=1):
        case_id = record.get("case_id")
        if not isinstance(case_id, str) or case_id not in by_id:
            problems.append(f"record {index}: unknown or invalid case_id {case_id!r}")
            continue
        row = by_id[case_id]
        if case_id in seen:
            row.update(declared_outcome="duplicate-case-record", result="ERROR")
            problems.append(f"case {case_id}: duplicate record")
            continue
        seen.add(case_id)

        carriers = record.get("carriers")
        path_label = record.get("path_label")
        response_mode = record.get("declared_response_mode")
        outcome = record.get("declared_outcome")
        result = record.get("result")
        property_name = record.get("binding_property")
        row.update(binding_property=property_name, assertion_result=result)
        valid = True
        expected_property = EXPECTED_PROPERTIES.get(case_id, "not-applicable")
        if property_name != expected_property:
            problems.append(f"case {case_id}: binding_property {property_name!r} does not match expected {expected_property!r}")
            valid = False
        if not isinstance(carriers, list) or any(not isinstance(item, str) for item in carriers):
            problems.append(f"case {case_id}: carriers must be an array of strings")
            valid = False
        expected_carriers, expected_mode, expected_outcomes = expected_metadata.get(case_id, ((), "", ()))
        if case_id not in expected_metadata:
            problems.append(f"case {case_id}: numbered E2E assertion has no runner metadata")
            valid = False
        if isinstance(carriers, list) and (
            any(not isinstance(item, str) or item not in {"interim-json", "mso-mdoc-zk-cbor"} for item in carriers)
            or tuple(carriers) != expected_carriers
        ):
            problems.append(f"case {case_id}: carriers {carriers!r} do not match observed inventory {expected_carriers!r}")
            valid = False
        if path_label != EXPECTED_PATH:
            problems.append(f"case {case_id}: invalid path_label {path_label!r}")
            valid = False
        if not isinstance(response_mode, str) or response_mode not in RESPONSE_MODES:
            problems.append(f"case {case_id}: invalid response_mode {response_mode!r}")
            valid = False
        if response_mode != expected_mode:
            problems.append(
                f"case {case_id}: response_mode {response_mode!r} does not match observed inventory {expected_mode!r}"
            )
            valid = False
        if not isinstance(outcome, str) or outcome not in OUTCOMES:
            problems.append(f"case {case_id}: invalid outcome {outcome!r}")
            valid = False
        elif case_id == "5" and isinstance(result, str):
            expected_outcome = "prover-guard-refused" if result == "PASS" else "prover-guard-failed"
            if outcome != expected_outcome:
                problems.append(f"case {case_id}: outcome {outcome!r} does not match {result} result {expected_outcome!r}")
                valid = False
        elif outcome not in expected_outcomes:
            problems.append(f"case {case_id}: outcome {outcome!r} does not match expected {expected_outcomes!r}")
            valid = False
        if not isinstance(result, str) or result not in RESULTS:
            problems.append(f"case {case_id}: invalid result {result!r}")
            valid = False
        if not valid:
            row.update(declared_outcome="malformed-case-record", result="ERROR")
            continue
        row.update(
            carriers=list(carriers),
            carrier=", ".join(carriers) if carriers else "NOT_OBSERVED",
            path_label=str(path_label),
            declared_response_mode=str(response_mode),
            declared_outcome=str(outcome),
            result=str(result),
        )

    return [by_id[case_id] for case_id in case_ids], problems


def run_command(
    name: str,
    command: Sequence[str],
    cwd: Path,
    env: dict[str, str] | None = None,
) -> SuiteResult:
    """Run a bounded child, killing and reaping its process group on limits."""
    command_list = list(command)
    print(f"\n== {name}: {' '.join(command_list)}", flush=True)
    try:
        proc = subprocess.Popen(
            command_list,
            cwd=cwd,
            env=env,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            start_new_session=True,
        )
    except OSError as exc:
        return SuiteResult(name, "ERROR", command_list, None, detail=f"could not start child: {exc}")

    assert proc.stdout is not None
    selector = selectors.DefaultSelector()
    selector.register(proc.stdout, selectors.EVENT_READ)
    output = bytearray()
    deadline = time.monotonic() + COMMAND_TIMEOUT_SECONDS
    limit_error = ""
    eof = False
    while not eof:
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            limit_error = f"child exceeded {COMMAND_TIMEOUT_SECONDS}s timeout"
            break
        ready = selector.select(min(0.25, remaining))
        if not ready:
            if proc.poll() is not None:
                # A descendant may still hold the pipe open. It shares the
                # process group and will be terminated if the bound expires.
                continue
            continue
        try:
            chunk = os.read(proc.stdout.fileno(), min(65536, COMMAND_OUTPUT_LIMIT_BYTES + 1 - len(output)))
        except OSError as exc:
            limit_error = f"could not read child output: {exc}"
            break
        if not chunk:
            eof = True
            break
        output.extend(chunk)
        if len(output) > COMMAND_OUTPUT_LIMIT_BYTES:
            output = output[:COMMAND_OUTPUT_LIMIT_BYTES]
            limit_error = f"child output exceeded {COMMAND_OUTPUT_LIMIT_BYTES} byte limit"
            break
        sys.stdout.buffer.write(chunk)
        sys.stdout.buffer.flush()

    selector.close()
    if not limit_error and proc.poll() is None:
        remaining = deadline - time.monotonic()
        try:
            proc.wait(timeout=max(0, remaining))
        except subprocess.TimeoutExpired:
            limit_error = f"child exceeded {COMMAND_TIMEOUT_SECONDS}s timeout"
    if limit_error:
        try:
            os.killpg(proc.pid, signal.SIGTERM)
        except ProcessLookupError:
            pass
        try:
            proc.wait(timeout=1)
        except subprocess.TimeoutExpired:
            pass
        try:
            os.killpg(proc.pid, signal.SIGKILL)
        except ProcessLookupError:
            pass
        proc.wait()
    else:
        proc.wait()
    proc.stdout.close()
    text_output = output.decode("utf-8", errors="replace")
    if limit_error:
        text_output += "\n[runner terminated child: " + limit_error + "]\n"
        return SuiteResult(name, "ERROR", command_list, proc.returncode, text_output, limit_error)
    returncode = proc.returncode
    if returncode == 0:
        status = "PASS"
    elif FAIL_MARKER.search(text_output):
        status = "FAIL"
    else:
        status = "ERROR"
    return SuiteResult(name, status, command_list, returncode, text_output)


ProcessRunner = Callable[[str, Sequence[str], Path, dict[str, str] | None], SuiteResult]


def run_conformance(
    root: Path,
    process_runner: ProcessRunner = run_command,
    staticlib_built: bool | None = None,
) -> dict[str, object]:
    """Run Go carrier tests, committed vectors, then the numbered E2E script."""
    root = root.resolve()
    go_dir = root / "verifier" / "go"
    suites: list[SuiteResult] = []
    e2e_script = root / "tests" / "e2e.sh"
    try:
        e2e_source = e2e_script.read_text(encoding="utf-8")
        case_ids = discover_case_ids(e2e_source)
    except (OSError, UnicodeError) as exc:
        case_ids = ()
        discovery_problem = f"could not read E2E assertion inventory: {exc}"
    else:
        discovery_problem = ""
    metadata_gaps = [case_id for case_id in case_ids if case_id not in EXPECTED_CASE_METADATA]
    inventory_prefix = [discovery_problem] if discovery_problem else []
    if not case_ids:
        inventory_prefix.append("no numbered E2E assertions were discovered")
    if len(case_ids) != len(set(case_ids)):
        inventory_prefix.append("tests/e2e.sh contains duplicate numbered assertion ids")
    inventory_prefix.extend(f"case {case_id}: numbered assertion has no runner metadata" for case_id in metadata_gaps)
    inventory_prefix.extend(
        f"case {case_id}: expected numbered assertion not found in tests/e2e.sh"
        for case_id in CASE_IDS if case_id not in case_ids
    )

    # Preserve this order: parser/dispatch controls, crypto-backed committed
    # vectors, then the live E2E suite whose rows include carrier observations.
    parser_command = ["go", "test", "-race", "./oid4vp/..."]
    suites.append(process_runner("carrier-parser-tests", parser_command, go_dir, None))

    if staticlib_built is None:
        staticlib_built = (root / "verifier" / "zkverify-ffi" / "target" / "release" / "libzkverify.a").is_file()
    vector_command = ["go", "test", "-race", "-v", ".", "-run", "^TestCarrierVectorsVerifyIdentically$", "-count=1"]
    if staticlib_built:
        suites.append(process_runner("committed-carrier-vectors", vector_command, go_dir, None))
    else:
        suites.append(SuiteResult(
            "committed-carrier-vectors", "ERROR", vector_command, None,
            detail="Rust static library is missing; build it with make deps before vector verification",
        ))
        print("\n== committed-carrier-vectors: ERROR (Rust static library is missing)", flush=True)

    vector_suite = next(s for s in suites if s.name == "committed-carrier-vectors")
    vector_test_passed = "--- PASS: TestCarrierVectorsVerifyIdentically" in vector_suite.output
    vector_test_skipped = "--- SKIP: TestCarrierVectorsVerifyIdentically" in vector_suite.output
    if vector_suite.status == "PASS" and (not vector_test_passed or vector_test_skipped):
        vector_suite.status = "ERROR"
        vector_suite.detail = "Go command exited successfully without a visible PASS for TestCarrierVectorsVerifyIdentically"

    with tempfile.TemporaryDirectory(prefix="carrier-conformance-") as temp_dir:
        records_path = Path(temp_dir) / "e2e-cases.jsonl"
        child_env = os.environ.copy()
        child_env["EE_E2E_CASES"] = str(records_path)
        e2e_suite = process_runner("e2e-assertions", [str(e2e_script)], root, child_env)
        suites.append(e2e_suite)
        if records_path.exists():
            records, decode_problems = read_case_records(records_path)
        else:
            records, decode_problems = [], []
    cases, inventory_problems = validate_case_records(records, decode_problems, case_ids)
    inventory_problems[:0] = inventory_prefix
    if e2e_suite.status == "ERROR" and e2e_suite.returncode is None:
        inventory_problems.append("E2E child could not be started")
    if e2e_suite.returncode not in (0, None) and not records:
        # A startup/prerequisite failure before numbered assertions is setup
        # evidence. No case may be inferred from its diagnostic text.
        suites[-1].status = "ERROR"
        suites[-1].detail = "E2E child exited before emitting numbered case records"

    manifest_path = root / "tests" / "vectors" / "carrier-v1" / "manifest.json"
    vector_carriers = list(EXPECTED_CARRIERS)
    manifest_problem = ""
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"), object_pairs_hook=_json_object)
        listed = manifest.get("carriers") if isinstance(manifest, dict) else None
        if listed != list(EXPECTED_CARRIERS):
            manifest_problem = f"carrier manifest inventory is {listed!r}, expected {list(EXPECTED_CARRIERS)!r}"
        for field, expected in EXPECTED_MANIFEST_PROVENANCE.items():
            if not isinstance(manifest, dict) or manifest.get(field) != expected:
                manifest_problem = manifest_problem or f"carrier manifest {field} does not match expected provenance"
        hashes = manifest.get("sha256") if isinstance(manifest, dict) else None
        if not isinstance(hashes, dict):
            manifest_problem = manifest_problem or "carrier manifest sha256 field is not an object"
        else:
            if set(hashes) != EXPECTED_VECTOR_FILES:
                manifest_problem = manifest_problem or (
                    f"carrier manifest hash inventory is {sorted(hashes)!r}, "
                    f"expected {sorted(EXPECTED_VECTOR_FILES)!r}"
                )
            vector_dir = manifest_path.parent
            for name, expected_hash in hashes.items():
                if not isinstance(name, str) or not isinstance(expected_hash, str):
                    manifest_problem = manifest_problem or f"invalid manifest hash entry {name!r}: {expected_hash!r}"
                    continue
                try:
                    actual_hash = hashlib.sha256((vector_dir / name).read_bytes()).hexdigest()
                except OSError as exc:
                    manifest_problem = manifest_problem or f"manifest vector {name!r} is unavailable: {exc}"
                    continue
                if actual_hash != expected_hash:
                    manifest_problem = manifest_problem or f"manifest SHA-256 mismatch for {name!r}"
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        manifest_problem = f"could not read carrier vector manifest: {exc}"

    version_problems = validate_carrier_versions(root)
    if version_problems:
        manifest_problem = "\n".join(([manifest_problem] if manifest_problem else []) + version_problems)
        inventory_problems.extend(version_problems)

    vector_rows = [
        {
            "carrier": carrier,
            "result": vector_suite.status,
            "suite": vector_suite.name,
            "evidence_scope": "paired same-proof test; any failing leg is named in suite output",
        }
        for carrier in vector_carriers
    ]
    if manifest_problem:
        vector_rows = [dict(row, result="ERROR") for row in vector_rows]
        vector_suite.status = "ERROR"
        vector_suite.detail = manifest_problem

    report: dict[str, object] = {
        "schema_version": 1,
        "evidence_scope": "local repository execution; no independent-wallet or external-conformance claim",
        "suites": [asdict(suite) for suite in suites],
        "case_inventory": list(case_ids),
        "carrier_vectors": vector_rows,
        "cases": cases,
        "inventory_problems": inventory_problems,
        "external_evidence": {
            "status": "BLOCKED",
            "date": date.today().isoformat(),
            "target": "Not selected; target selection is assigned to Story 2.4",
            "reason": "This run is local; no independent implementation or revision has been run against the suite.",
        },
    }
    return report


def report_passed(report: dict[str, object]) -> bool:
    suites = report.get("suites", [])
    cases = report.get("cases", [])
    vector_rows = report.get("carrier_vectors", [])
    return (
        not report.get("inventory_problems")
        and all(isinstance(s, dict) and s.get("status") == "PASS" for s in suites)
        and len(cases) == len(report.get("case_inventory", []))
        and [c.get("case_id") for c in cases] == report.get("case_inventory")
        and all(c.get("result") == "PASS" for c in cases)
        and [v.get("carrier") for v in vector_rows] == list(EXPECTED_CARRIERS)
        and all(v.get("result") == "PASS" for v in vector_rows)
    )


def print_summary(report: dict[str, object]) -> None:
    print("\nCarrier conformance summary")
    for suite in report["suites"]:
        print(f"  {suite['status']:12} {suite['name']}")
    print("  E2E case rows:")
    for case in report["cases"]:
        print(
            f"    {case['case_id']:>2}  {case['carrier']:<30} {case['declared_response_mode']:<16} "
            f"{case['result']:<12} {case['declared_outcome']} property={case['binding_property']} "
            f"assertion={case['assertion_result']} [{case['path_label']}]"
        )
    print("  Carrier vectors:")
    for vector in report["carrier_vectors"]:
        print(f"    {vector['carrier']:<20} {vector['result']}")
    for problem in report["inventory_problems"]:
        print(f"  ERROR        {problem}")
    print("Evidence scope: local repository execution only; no independent-wallet or external-conformance claim.")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", type=Path, help="write the full machine-readable report to this path")
    args = parser.parse_args(argv)
    root = Path(__file__).resolve().parents[1]
    report = run_conformance(root)
    print_summary(report)
    if args.json:
        args.json.parent.mkdir(parents=True, exist_ok=True)
        args.json.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
        print(f"JSON report: {args.json}")
    return 0 if report_passed(report) else 1


if __name__ == "__main__":
    raise SystemExit(main())
