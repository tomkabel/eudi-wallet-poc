#!/usr/bin/env python3
"""Seven ordered Estonian acceptance cases with fresh, bounded evidence intake."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import uuid
import xml.etree.ElementTree as ET
from dataclasses import asdict
from datetime import UTC, datetime
from pathlib import Path

import run_carrier_conformance as bounded
from av_report import file_record, git_record

ROOT = Path(__file__).resolve().parents[1]
CASE_IDS = (
    "foreign-pid",
    "foreign-av",
    "unattested-openid-dcapi",
    "unregistered-rp",
    "shared-offline-fallback",
    "unauthenticated-iso-mdoc",
    "unauthenticated-proximity",
)
FOREIGN = CASE_IDS[:2]
DISPATCH = (CASE_IDS[2], CASE_IDS[5], CASE_IDS[6])
STATES = {"PASS", "FAIL", "ERROR", "NOT_EXECUTED"}
HEX_REV = re.compile(r"[0-9a-f]{40}")
HEX_DIGEST = re.compile(r"[0-9a-f]{64}")
LIMIT = 4 * 1024 * 1024


def decode(raw):
    depth, quoted, escaped = 0, False, False
    for char in raw:
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
        elif char == '"':
            quoted = True
        elif char in "[{":
            depth += 1
            if depth > 256:
                raise ValueError("evidence JSON nesting limit exceeded")
        elif char in "]}":
            depth -= 1

    def invalid_constant(value):
        raise ValueError("nonstandard JSON constant: " + value)

    def pairs(items):
        result = {}
        for key, value in items:
            if key in result:
                raise ValueError("duplicate JSON key: " + key)
            result[key] = value
        return result

    try:
        return json.loads(raw, object_pairs_hook=pairs, parse_constant=invalid_constant)
    except RecursionError as exc:
        raise ValueError("evidence JSON nesting limit exceeded") from exc


def bounded_bytes(path):
    with path.open("rb") as stream:
        raw = stream.read(LIMIT + 1)
    if len(raw) > LIMIT:
        raise ValueError("evidence exceeds byte limit")
    return raw


def read_json(path):
    return decode(bounded_bytes(path).decode("utf-8"))


def byte_record(raw):
    return {
        "exists": True,
        "sha256": hashlib.sha256(raw).hexdigest(),
        "size_bytes": len(raw),
    }


def streamed_record(path):
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(65536), b""):
            digest.update(chunk)
            size += len(chunk)
    return {"exists": True, "sha256": digest.hexdigest(), "size_bytes": size}


def git_executable():
    executable = shutil.which("git")
    if executable is None:
        raise FileNotFoundError("git executable is required for source provenance")
    return executable


def source_snapshot(root):
    result = subprocess.run(  # noqa: S603 - resolved Git executable and fixed read-only arguments
        [
            git_executable(),
            "ls-files",
            "-z",
            "--cached",
            "--others",
            "--exclude-standard",
        ],
        cwd=root,
        check=True,
        capture_output=True,
        timeout=30,
    )
    excluded = {
        "build",
        "target",
        ".gradle",
        ".venv",
        "__pycache__",
        "demo-data",
        "evidence",
    }
    files = {}
    for name in sorted(set(result.stdout.decode().split("\0")) - {""}):
        path = Path(name)
        if excluded.intersection(path.parts) or path.parts[0] in {
            "docs",
            "spec",
            "_bmad",
            "_bmad-output",
            ".claude",
        }:
            continue
        full = root / path
        if full.is_file():
            files[name] = streamed_record(full)
    return files


def require_independent_source(root, foreign_source):
    if foreign_source.resolve() == root.resolve():
        raise ValueError("supply an independent foreign source checkout")

    def roots(path):
        return set(
            subprocess.run(  # noqa: S603 - resolved Git executable and fixed read-only arguments
                [git_executable(), "rev-list", "--max-parents=0", "HEAD"],
                cwd=path,
                check=True,
                capture_output=True,
                text=True,
                timeout=30,
            ).stdout.split()
        )

    if roots(root).intersection(roots(foreign_source)):
        raise ValueError("foreign source shares this repository's history")


def wire_digest(wire):
    raw = (
        wire.encode("utf-8")
        if isinstance(wire, str)
        else json.dumps(
            wire,
            sort_keys=True,
            separators=(",", ":"),
            ensure_ascii=False,
        ).encode("utf-8")
    )
    return hashlib.sha256(raw).hexdigest()


def ordered_records(records, expected):
    if not isinstance(records, list):
        raise ValueError("records must be an array")
    found = {}
    for record in records:
        if not isinstance(record, dict) or record.get("case_id") not in expected:
            raise ValueError("unknown or malformed case record")
        key = record["case_id"]
        if key in found:
            raise ValueError("duplicate case record: " + key)
        found[key] = record
    if set(found) != set(expected):
        raise ValueError("missing required case record")
    return [found[key] for key in expected]


def run_child(name, command, root, env=None, timeout: float = 900, output_limit=LIMIT):
    # Reuse the carrier runner's process-group cleanup and bounded pipe reader.
    old = bounded.COMMAND_TIMEOUT_SECONDS, bounded.COMMAND_OUTPUT_LIMIT_BYTES
    bounded.COMMAND_TIMEOUT_SECONDS = timeout
    bounded.COMMAND_OUTPUT_LIMIT_BYTES = output_limit
    try:
        return bounded.run_command(name, command, root, env)
    finally:
        bounded.COMMAND_TIMEOUT_SECONDS, bounded.COMMAND_OUTPUT_LIMIT_BYTES = old


def marker_records(output, prefix):
    records = []
    for line in output.splitlines():
        if prefix not in line:
            continue
        if line.startswith("{"):
            event = decode(line)
            if not isinstance(event, dict):
                raise ValueError("malformed process event")
            line = event.get("Output", "")
            if not isinstance(line, str):
                raise ValueError("malformed process event output")
        if prefix in line:
            record = decode(line.split(prefix, 1)[1].strip())
            if not isinstance(record, dict):
                raise ValueError("malformed observation marker")
            records.append(record)
    return records


def validate_dispatch(records):
    rows = ordered_records(records, DISPATCH)
    for row in rows:
        if (
            row.get("evidence_kind") != "production-viewmodel"
            or row.get("refused") is not True
            or row.get("consent_released") is not False
            or row.get("matched_requests")
            != (0 if row.get("case_id") == DISPATCH[0] else 1)
            or type(row.get("responses")) is not int
            or row["responses"] != 0
            or type(row.get("requests")) is not int
            or row["requests"] < 1
            or not isinstance(row.get("outcome"), str)
            or not row["outcome"]
        ):
            raise ValueError("request dispatch refusal/release observation missing")
        if row["case_id"] == "unattested-openid-dcapi":
            if row["outcome"] != "UNSUPPORTED_PROTOCOL":
                raise ValueError(
                    "unsigned OpenID must name actual unsupported protocol"
                )
        elif row["outcome"] != "PRESENTATION_UNAUTHENTICATED_REQUEST":
            raise ValueError("ISO/proximity must name unauthenticated policy refusal")
    return rows


def validate_fallback(records):
    if (
        not isinstance(records, list)
        or len(records) != 2
        or any(not isinstance(row, dict) for row in records)
    ):
        raise ValueError("both actual fallback consumers must be observed exactly once")
    if {r.get("consumer") for r in records} != {
        "wallet",
        "verifier",
    }:
        raise ValueError("both actual fallback consumers must be observed exactly once")
    for row in records:
        if (
            row.get("fetched") is not False
            or row.get("signature_valid") is not True
            or not isinstance(row.get("list_version"), str)
            or not row["list_version"].isdigit()
            or not isinstance(row.get("document_sha256"), str)
            or HEX_DIGEST.fullmatch(row["document_sha256"]) is None
            or not isinstance(row.get("spec_id"), str)
            or not row["spec_id"].startswith("longfellow-libzk-v1_")
        ):
            raise ValueError("fallback validation/resolution observation missing")
    for key in ("list_version", "spec_id", "document_sha256"):
        if records[0][key] != records[1][key]:
            raise ValueError("fallback consumer drift: " + key)
    return {"outcome": "shared-signed-fallback-resolved", "evidence": records}


def jvm_evidence(paths, started_ns):
    dispatch, fallback, artifacts = [], [], []
    for path in paths:
        if not path.is_file() or path.stat().st_mtime_ns < started_ns:
            raise ValueError("missing or stale JVM result: " + str(path))
        raw = bounded_bytes(path)
        suite = ET.fromstring(raw)  # noqa: S314 - local bounded Gradle output
        if (
            int(suite.get("tests", "0")) < 1
            or any(
                int(suite.get(key, "0")) for key in ("failures", "errors", "skipped")
            )
            or suite.findall(".//skipped")
        ):
            raise ValueError("JVM suite incomplete, failed or skipped")
        output = "\n".join(item.text or "" for item in suite.findall(".//system-out"))
        dispatch.extend(marker_records(output, "ESTONIAN_ACCEPTANCE="))
        fallback.extend(marker_records(output, "ESTONIAN_FALLBACK="))
        artifacts.append({"path": str(path), **byte_record(raw)})
    return validate_dispatch(dispatch), fallback, artifacts


def validate_foreign(
    document,
    run_id,
    challenge,
    started_at,
    finished_at,
    work,
    source,
    adapter_digest=None,
):
    if (
        not isinstance(document, dict)
        or document.get("schema") != "ee-estonian-external/1"
        or document.get("run_id") != run_id
        or document.get("challenge") != challenge
    ):
        raise ValueError("foreign adapter must bind this fresh run and challenge")
    if (
        source.get("state") != "OK"
        or source.get("dirty") is not False
        or HEX_REV.fullmatch(source.get("revision", "")) is None
        or document.get("source_revision") != source["revision"]
    ):
        raise ValueError(
            "foreign source must be a clean independently pinned Git checkout"
        )
    rows = ordered_records(document.get("records"), FOREIGN)
    if any(row.get("state") != "NOT_EXECUTED" for row in rows):
        build = document.get("wallet_build")
        if (
            not isinstance(build, dict)
            or any(
                build.get(key) != value
                for key, value in {
                    "run_id": run_id,
                    "challenge": challenge,
                    "source_revision": source["revision"],
                    "source_path": source.get("path"),
                    "adapter_sha256": adapter_digest,
                }.items()
            )
            or not isinstance(build.get("package_id"), str)
            or not build["package_id"].strip()
        ):
            raise ValueError(
                "foreign wallet build identity/source/execution binding missing"
            )
        built_at = datetime.fromisoformat(build.get("built_at", ""))
        if built_at.tzinfo is None or not started_at <= built_at <= finished_at:
            raise ValueError("foreign wallet build is stale")
        artifact = build.get("artifact", {})
        if not isinstance(artifact, dict) or not isinstance(artifact.get("path"), str):
            raise ValueError("foreign wallet build artifact identity missing")
        path = (work / artifact.get("path", "")).resolve()
        if not path.is_relative_to(work.resolve()) or not path.is_file():
            raise ValueError("foreign wallet build artifact missing or outside output")
        record = streamed_record(path)
        if record["sha256"] != artifact.get("sha256"):
            raise ValueError("foreign wallet build artifact digest differs")
        document["observed_wallet_build"] = {**build, **record}
    sessions = set()
    for row in rows:
        state = row.get("state")
        if state not in STATES:
            raise ValueError("invalid foreign evidence state")
        if state == "NOT_EXECUTED":
            if (
                not isinstance(row.get("operator_action"), str)
                or not row["operator_action"].strip()
            ):
                raise ValueError("unexecuted foreign case requires an operator action")
            continue
        row["observed_wallet_build"] = document["observed_wallet_build"]
        session = row.get("session")
        if not isinstance(session, dict):
            raise ValueError("fresh foreign session observation is missing")
        if (
            session.get("challenge") != challenge
            or not isinstance(session.get("id"), str)
            or not session["id"]
            or session["id"] in sessions
            or session.get("credential_kind")
            != ("PID" if row["case_id"] == "foreign-pid" else "AV")
            or not isinstance(session.get("issuing_country"), str)
            or not re.fullmatch(r"[A-Z]{2}", session["issuing_country"])
            or session["issuing_country"] == "EE"
            or not isinstance(session.get("wallet"), str)
            or not session["wallet"].strip()
            or session.get("source_revision") != source["revision"]
            or session.get("wallet_build_sha256")
            != document["observed_wallet_build"]["sha256"]
        ):
            raise ValueError(
                "foreign source, country, credential or session provenance invalid"
            )
        observed = datetime.fromisoformat(session.get("observed_at", ""))
        if observed.tzinfo is None or not started_at <= observed <= finished_at:
            raise ValueError("foreign session is stale or timestamp is invalid")
        sessions.add(session["id"])
        artifacts = row.get("artifacts")
        if not isinstance(artifacts, dict) or set(artifacts) != {
            "request",
            "response",
            "verification",
        }:
            raise ValueError(
                "foreign wire request/response/verification artifacts required"
            )
        loaded, observed = {}, {}
        for kind, artifact in artifacts.items():
            if not isinstance(artifact, dict) or not isinstance(
                artifact.get("path"), str
            ):
                raise ValueError("invalid foreign artifact")
            path = (work / artifact["path"]).resolve()
            if not path.is_relative_to(work.resolve()) or not path.is_file():
                raise ValueError(
                    "foreign artifacts must be inside this fresh output directory"
                )
            raw = bounded_bytes(path)
            record = byte_record(raw)
            if record["sha256"] != artifact.get("sha256"):
                raise ValueError("foreign artifact digest differs")
            loaded[kind] = decode(raw.decode("utf-8"))
            observed[kind] = {"content": loaded[kind], **record}
            if (
                not isinstance(loaded[kind], dict)
                or loaded[kind].get("session_id") != session["id"]
            ):
                raise ValueError("foreign artifact session does not match")
        if (
            loaded["request"].get("challenge") != challenge
            or loaded["verification"].get("challenge") != challenge
        ):
            raise ValueError("fresh challenge is absent from request or verification")
        if not isinstance(row.get("outcome"), str) or not row["outcome"]:
            raise ValueError("observed foreign outcome is required")
        if state == "PASS" and (
            row["outcome"] != "presentation-accepted"
            or loaded["verification"].get("valid") is not True
            or loaded["response"].get("received") is not True
        ):
            raise ValueError(
                "foreign acceptance lacks observed received and verified response"
            )
        for kind in ("request", "response"):
            wire = loaded[kind].get("wire")
            if not isinstance(wire, (str, dict, list)) or not wire:
                raise ValueError("foreign wire request/response payload is missing")
        request_wire, response_wire = (
            loaded["request"]["wire"],
            loaded["response"]["wire"],
        )
        verification = loaded["verification"]
        if (
            not isinstance(request_wire, dict)
            or request_wire.get("nonce") != challenge
            or request_wire.get("state") != session["id"]
            or not isinstance(response_wire, dict)
            or response_wire.get("state") != session["id"]
            or not response_wire.get("vp_token")
            or verification.get("verified_nonce") != challenge
            or verification.get("verified_session_id") != session["id"]
            or verification.get("request_wire_sha256") != wire_digest(request_wire)
            or verification.get("response_wire_sha256") != wire_digest(response_wire)
        ):
            raise ValueError(
                "foreign wire nonce/session or verified wire digest binding invalid"
            )
        if (
            loaded["verification"].get("executed") is not True
            or not isinstance(loaded["verification"].get("verifier"), str)
            or not loaded["verification"]["verifier"].strip()
            or not isinstance(loaded["verification"].get("trust_input_sha256"), str)
            or HEX_DIGEST.fullmatch(loaded["verification"]["trust_input_sha256"])
            is None
        ):
            raise ValueError(
                "foreign verification execution/trust input provenance missing"
            )
        # Preserve inspectable artifacts after the private execution directory is
        # removed; a later reviewer must not rely on vanished temporary paths.
        row["observed_artifacts"] = observed
    return rows


def execute(
    root=ROOT, *, adapter=None, foreign_source=None, local=True, jvm=True, timeout=900
):
    root = root.resolve()
    run_id, challenge = str(uuid.uuid4()), uuid.uuid4().hex
    report = {
        "schema": "ee-estonian-acceptance/1",
        "run_id": run_id,
        "generated_at": datetime.now(UTC).isoformat(),
        "repository": git_record(root),
        "limits": {"child_timeout_seconds": timeout, "child_output_bytes": LIMIT},
        "scope": (
            "Local controls are separate from independent foreign wallet sessions; "
            "no certification or legal conclusion."
        ),
        "paths": [
            "dc-api (AV Profile mandated)",
            "openid4vp (profiled, unofficial)",
            "proximity",
        ],
        "cases": [
            {
                "case_id": key,
                "state": "NOT_EXECUTED",
                "outcome": "not-executed",
                "evidence": [],
            }
            for key in CASE_IDS
        ],
        "executions": [],
        "supplementary": [],
        "diagnostics": [],
        "operator_actions": [],
    }
    rows = {row["case_id"]: row for row in report["cases"]}

    def child(name, command, cwd=root, env=None):
        result = run_child(name, command, cwd, env, timeout)
        report["executions"].append(asdict(result))
        return result

    def error(keys, detail):
        report["diagnostics"].append(detail)
        for key in keys:
            rows[key].update(state="ERROR", outcome="evidence-error", detail=detail)

    try:
        before = source_snapshot(root)
        report["source_before"] = before
    except (OSError, ValueError, subprocess.SubprocessError) as exc:
        before = None
        error(CASE_IDS[2:], "source-provenance-error: " + str(exc))
    pin_error = None

    with tempfile.TemporaryDirectory(prefix="estonian-acceptance-") as directory:
        work = Path(directory)
        if local:
            binary = work / "zkverify"
            try:
                pin = (
                    bounded_bytes(root / "verifier/zkverify-ffi/longfellow-rev.txt")
                    .decode()
                    .strip()
                )
                if HEX_REV.fullmatch(pin) is None:
                    raise ValueError("expected a full 40-character revision")
            except (OSError, ValueError) as exc:
                pin = None
                pin_error = "missing-or-unreadable-longfellow-pin: " + str(exc)
                error([CASE_IDS[3], CASE_IDS[4]], pin_error)
            build = (
                child(
                    "fresh-verifier-build",
                    [
                        "go",
                        "build",
                        "-ldflags",
                        "-X main.longfellowRev=" + pin,
                        "-o",
                        str(binary),
                        ".",
                    ],
                    root / "verifier/go",
                )
                if pin is not None
                else None
            )
            if build is None:
                pass
            elif build.status != "PASS":
                error([CASE_IDS[3]], "fresh verifier build failed")
            else:
                output = work / "local.json"
                env = dict(os.environ, EE_VERIFIER=str(binary))
                result = child(
                    "local-rp-process-and-av-proof",
                    [
                        sys.executable,
                        str(root / "tests/estonian_local_acceptance.py"),
                        "--run-id",
                        run_id,
                        "--output",
                        str(output),
                    ],
                    env=env,
                )
                try:
                    if result.status != "PASS":
                        raise ValueError("local process/proof child did not pass")
                    doc = read_json(output)
                    if (
                        not isinstance(doc, dict)
                        or doc.get("schema") != "ee-estonian-local/1"
                        or doc.get("run_id") != run_id
                    ):
                        raise ValueError("local evidence schema/run mismatch")
                    record = ordered_records(doc.get("records"), [CASE_IDS[3]])[0]
                    routes = record.get("routes", [])
                    if (
                        not isinstance(routes, list)
                        or len(routes) != 2
                        or any(not isinstance(item, dict) for item in routes)
                        or {r.get("route") for r in routes}
                        != {"/present/new", "/present/dcapi/new"}
                        or any(
                            r.get("http_status") != 403
                            or r.get("reason") != "register-not-found"
                            or r.get("session_released") is not False
                            for r in routes
                        )
                    ):
                        raise ValueError(
                            "both real absent registration HTTP refusals required"
                        )
                    if (
                        not isinstance(doc.get("supplementary"), list)
                        or not doc["supplementary"]
                        or not isinstance(doc["supplementary"][0], dict)
                        or doc["supplementary"][0].get("outcome") != "proof-verified"
                    ):
                        raise ValueError("actual local AV proof control missing")
                    session_check = child(
                        "registration-before-session-allocation",
                        [
                            "go",
                            "test",
                            "-count=1",
                            "-json",
                            "-run",
                            "^TestEstonianAbsentRegistrationNoSession$",
                            ".",
                        ],
                        root / "verifier/go",
                    )
                    allocation = marker_records(
                        session_check.output, "ESTONIAN_REGISTRATION="
                    )
                    if (
                        session_check.status != "PASS"
                        or len(allocation) != 1
                        or allocation[0]
                        != {
                            "routes": 2,
                            "live_sessions_before": 0,
                            "live_sessions_after": 0,
                            "outcome": "register-not-found",
                        }
                    ):
                        raise ValueError(
                            "real creation paths did not prove zero session allocation"
                        )
                    rows[CASE_IDS[3]].update(
                        state="PASS",
                        outcome="register-not-found",
                        evidence=[record, allocation[0]],
                    )
                    report["supplementary"] = doc["supplementary"]
                except (OSError, ValueError, KeyError, TypeError) as exc:
                    error([CASE_IDS[3]], str(exc))
        go_observations = []
        if local:
            result = child(
                "verifier-offline-fallback",
                ["go", "test", "-count=1", "-json", "./circuits/..."],
                root / "verifier/go",
            )
            try:
                if result.status != "PASS":
                    raise ValueError("verifier fallback tests failed")
                go_observations = marker_records(result.output, "ESTONIAN_FALLBACK=")
            except (ValueError, TypeError) as exc:
                error([CASE_IDS[4]], str(exc))
        wallet_observations = []
        if jvm:
            results_dir = root / "app/build/test-results/testLocal_mocksUnitTest"
            patterns = (
                "*EstonianViewModelAcceptanceTest.xml",
                "*SignedCircuitFallbackTest.xml",
            )
            for pattern in patterns:
                for path in results_dir.glob(pattern):
                    path.unlink()
            started_ns = time.time_ns()
            result = child(
                "wallet-dispatch-and-offline-fallback",
                [
                    str(root / "gradlew"),
                    ":app:testLocal_mocksUnitTest",
                    "--rerun-tasks",
                    "--console=plain",
                    "--tests",
                    "*EstonianViewModelAcceptanceTest",
                    "--tests",
                    "*SignedCircuitFallbackTest",
                ],
            )
            try:
                if result.status != "PASS":
                    raise ValueError("fresh JVM acceptance execution did not pass")
                paths = [
                    path for pattern in patterns for path in results_dir.glob(pattern)
                ]
                if len(paths) != 2:
                    raise ValueError("both fresh JVM acceptance suites required")
                observed, wallet_observations, artifacts = jvm_evidence(
                    paths, started_ns
                )
                for record in observed:
                    rows[record["case_id"]].update(
                        state="PASS",
                        outcome=record["outcome"],
                        evidence=[record],
                        artifacts=artifacts,
                    )
            except (OSError, ValueError, TypeError, ET.ParseError) as exc:
                error([*DISPATCH, CASE_IDS[4]], str(exc))
        if local and jvm:
            try:
                observation = validate_fallback(
                    [*go_observations, *wallet_observations]
                )
                if (
                    observation["evidence"][0]["document_sha256"]
                    != file_record(root / "verifier/circuits.json")["sha256"]
                ):
                    raise ValueError(
                        "observed fallback differs from pinned signed document"
                    )
                rows[CASE_IDS[4]].update(state="PASS", **observation)
            except (OSError, ValueError, KeyError, TypeError) as exc:
                error([CASE_IDS[4]], str(exc))
        if adapter is not None:
            try:
                if foreign_source is None:
                    raise ValueError("supply an independent foreign source checkout")
                require_independent_source(root, foreign_source)
                source = git_record(foreign_source.resolve())
                source["path"] = str(foreign_source.resolve())
                adapter_path = Path(adapter[0]).resolve()
                report["external_adapter"] = {
                    "path": str(adapter_path),
                    **streamed_record(adapter_path),
                }
                output_dir = work / "foreign"
                output_dir.mkdir()
                output = output_dir / "evidence.json"
                started = datetime.now(UTC)
                env = dict(
                    os.environ,
                    EE_ACCEPTANCE_RUN_ID=run_id,
                    EE_ACCEPTANCE_CHALLENGE=challenge,
                    EE_ACCEPTANCE_OUTPUT=str(output),
                    EE_ACCEPTANCE_OUTPUT_DIR=str(output_dir),
                )
                result = child(
                    "external-foreign-wallet-adapter",
                    adapter,
                    foreign_source.resolve(),
                    env,
                )
                finished = datetime.now(UTC)
                if result.status != "PASS":
                    raise ValueError(
                        "foreign adapter failed, timed out or exceeded output limit"
                    )
                if {
                    **git_record(foreign_source.resolve()),
                    "path": str(foreign_source.resolve()),
                } != source:
                    raise ValueError("foreign source changed during execution")
                observed = validate_foreign(
                    read_json(output),
                    run_id,
                    challenge,
                    started,
                    finished,
                    output_dir,
                    source,
                    report["external_adapter"]["sha256"],
                )
                for record in observed:
                    rows[record["case_id"]].update(
                        state=record["state"],
                        outcome=record.get("outcome", "not-executed"),
                        evidence=[record],
                        source=source,
                    )
                    if record["state"] == "NOT_EXECUTED":
                        report["operator_actions"].append(record["operator_action"])
            except (
                OSError,
                ValueError,
                KeyError,
                TypeError,
                subprocess.SubprocessError,
            ) as exc:
                error(FOREIGN, str(exc))
    for key in FOREIGN:
        if rows[key]["state"] == "NOT_EXECUTED":
            report["operator_actions"].append(
                "Execute a fresh genuine foreign "
                + ("PID" if key == "foreign-pid" else "AV")
                + " wallet session with authorized issuer trust/access, "
                "and supply its external adapter evidence."
            )
    if pin_error:
        error([CASE_IDS[3], CASE_IDS[4]], pin_error)
    try:
        after = source_snapshot(root)
        report["source_after"] = after
        report["inputs"] = after
        if before is None or before != after:
            error(CASE_IDS[2:], "source-mutated-during-execution")
            report["supplementary"] = []
    except (OSError, ValueError, subprocess.SubprocessError) as exc:
        error(CASE_IDS[2:], "source-provenance-error: " + str(exc))
    states = [row["state"] for row in report["cases"]]
    report["result"] = (
        "ERROR"
        if "ERROR" in states
        else "FAIL"
        if "FAIL" in states
        else "INCOMPLETE"
        if "NOT_EXECUTED" in states
        else "PASS"
    )
    report["agent_controls_passed"] = all(
        rows[key]["state"] == "PASS" for key in CASE_IDS[2:]
    )
    report["status"] = (
        "awaiting-operator"
        if report["agent_controls_passed"] and report["operator_actions"]
        else "complete"
        if report["result"] == "PASS"
        else "incomplete"
    )
    report["exit_code"] = 0 if report["result"] == "PASS" else 1
    return report


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--json",
        required=True,
        type=Path,
        help="write the complete machine-readable report",
    )
    parser.add_argument(
        "--adapter", type=Path, help="executable external adapter (no shell parsing)"
    )
    parser.add_argument(
        "--foreign-source", type=Path, help="clean independent Git source checkout"
    )
    parser.add_argument(
        "--no-local",
        action="store_true",
        help="leave process/fallback cases unexecuted",
    )
    parser.add_argument(
        "--no-jvm", action="store_true", help="leave wallet/fallback cases unexecuted"
    )
    parser.add_argument(
        "--timeout", type=int, default=900, choices=range(1, 3601), metavar="1..3600"
    )
    args = parser.parse_args(argv)
    report = execute(
        adapter=[str(args.adapter.resolve())] if args.adapter else None,
        foreign_source=args.foreign_source,
        local=not args.no_local,
        jvm=not args.no_jvm,
        timeout=args.timeout,
    )
    args.json.parent.mkdir(parents=True, exist_ok=True)
    args.json.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n")
    print("Estonian acceptance: " + report["result"] + "; report: " + str(args.json))
    return report["exit_code"]


if __name__ == "__main__":
    sys.exit(main())
