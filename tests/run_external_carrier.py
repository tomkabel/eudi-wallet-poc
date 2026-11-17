#!/usr/bin/env python3
"""Bounded pinned external carrier attempts; builds never imply assertions."""

from __future__ import annotations

import argparse
import base64
import hashlib
import json
import math
import os
import platform
import re
import shlex
import stat
import sys
import tempfile
from dataclasses import asdict
from datetime import UTC, datetime
from pathlib import Path
from unittest.mock import patch

import run_carrier_conformance as local

ROOT = Path(__file__).resolve().parents[1]
URL = "https://github.com/openwallet-foundation/multipaz.git"
REVISION = "7486ddbca490038c3580513d8273b9bbf18c6b70"
NAME = "Multipaz sample wallet 0.99.0"
TARGET_PATH = ROOT / "docs/profiles/carrier/external-target.json"


def strict_json(raw):
    def constant(value):
        raise ValueError(f"nonfinite JSON constant: {value}")

    value = json.loads(
        raw, object_pairs_hook=local._json_object, parse_constant=constant
    )
    json.dumps(value, ensure_ascii=False, allow_nan=False).encode("utf-8")
    return value


def target_metadata():
    target = strict_json(TARGET_PATH.read_bytes().decode("utf-8"))
    if (
        not isinstance(target, dict)
        or any(
            target.get(k) != v
            for k, v in (
                ("name", NAME),
                ("source_url", URL),
                ("tag", "0.99.0"),
                ("revision", REVISION),
            )
        )
        or not isinstance(target.get("rationale"), str)
        or not target["rationale"].strip()
    ):
        raise ValueError("target metadata does not match reviewed public target")
    return target


def execute(name, argv, cwd, timeout, output_limit, env=None):
    with (
        patch.object(local, "COMMAND_TIMEOUT_SECONDS", timeout),
        patch.object(local, "COMMAND_OUTPUT_LIMIT_BYTES", output_limit),
    ):
        result = asdict(local.run_command(name, argv, cwd, env))
    result["cwd"] = str(cwd)
    return result


def validate_evidence(payload):
    if (
        not isinstance(payload, dict)
        or type(payload.get("schema_version")) is not int
        or payload["schema_version"] != 1
    ):
        raise ValueError("adapter requires integer schema_version=1")
    if payload.get("target") != {"name": NAME, "revision": REVISION}:
        raise ValueError("adapter target name/revision mismatch")
    records = payload.get("cases")
    if not isinstance(records, list) or not all(
        isinstance(row, dict) for row in records
    ):
        raise ValueError("adapter cases must be objects")
    cases, problems = local.validate_case_records(records)
    if len(records) != len(local.CASE_IDS) or any(
        row["result"] not in {"PASS", "FAIL"} for row in cases
    ):
        problems.append("adapter must report every numbered case exactly once")
    for row in records:
        if not isinstance(row.get("evidence"), str) or not row["evidence"].strip():
            problems.append("every case requires nonempty assertion evidence")
    vectors = payload.get("carrier_vectors")
    if (
        not isinstance(vectors, list)
        or len(vectors) != 2
        or any(not isinstance(row, dict) for row in vectors)
    ):
        raise ValueError("adapter must report both carrier vectors exactly once")
    if sorted(str(row.get("carrier")) for row in vectors) != sorted(
        local.EXPECTED_CARRIERS
    ):
        problems.append("missing, duplicate or unknown carrier vector")
    for row in vectors:
        if (
            not isinstance(row.get("result"), str)
            or row["result"] not in {"PASS", "FAIL"}
            or not isinstance(row.get("evidence"), str)
            or not row["evidence"].strip()
        ):
            problems.append("vector requires PASS/FAIL and nonempty assertion evidence")
    if problems:
        raise ValueError("; ".join(problems))
    return cases, sorted(
        vectors, key=lambda row: local.EXPECTED_CARRIERS.index(row["carrier"])
    )


def read_evidence(path, output_limit):
    # Open once, without following links or waiting for a FIFO writer. All checks
    # and reads use this descriptor even if the pathname is replaced afterwards.
    fd = os.open(path, os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK)
    with os.fdopen(fd, "rb") as stream:
        info = os.fstat(stream.fileno())
        if not stat.S_ISREG(info.st_mode):
            raise ValueError("adapter evidence must be a regular file")
        if info.st_size > output_limit:
            raise ValueError("adapter evidence exceeds output limit")
        raw = stream.read(output_limit + 1)
    if len(raw) > output_limit:
        raise ValueError("adapter evidence exceeds output limit")
    return raw


def suite_provenance():
    observations = [
        execute("suite-revision", ["git", "rev-parse", "HEAD"], ROOT, 10, 65536),
        execute(
            "suite-cleanliness",
            ["git", "status", "--porcelain", "--untracked-files=all"],
            ROOT,
            10,
            65536,
        ),
    ]
    if any(row["returncode"] != 0 or row["status"] != "PASS" for row in observations):
        raise ValueError("suite source provenance unavailable")
    revision, cleanliness = (row["output"].strip() for row in observations)
    if not re.fullmatch(r"[0-9a-f]{40}", revision):
        raise ValueError("suite source revision is not canonical")
    paths = [
        ROOT / "tests/run_external_carrier.py",
        ROOT / "tests/run_carrier_conformance.py",
        ROOT / "tests/e2e.sh",
        TARGET_PATH,
        *sorted((ROOT / "tests/vectors/carrier-v1").rglob("*")),
    ]
    return {
        "observed_at": datetime.now(UTC).isoformat(),
        "revision": revision,
        "dirty": bool(cleanliness),
        "executions": observations,
        "sha256": {
            str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in paths
            if path.is_file()
        },
    }


def base_report(checkout, timeout, output_limit):
    cases = local.validate_case_records([])[0]
    vectors = [
        {
            "carrier": carrier,
            "result": "NOT_EXECUTED",
            "evidence": "external adapter has not executed",
        }
        for carrier in local.EXPECTED_CARRIERS
    ]
    return {
        "schema_version": 1,
        "generated_at": datetime.now(UTC).isoformat(),
        "target": {
            "name": NAME,
            "source_url": URL,
            "tag": "0.99.0",
            "revision": REVISION,
            "rationale": (
                "Target metadata unavailable; reviewed selection requires validation."
            ),
        },
        "host": {
            "hostname": platform.node(),
            "platform": platform.platform(),
            "python": platform.python_version(),
        },
        "status": "BLOCKED",
        "detail": "No external adapter configured; build success "
        "alone cannot establish assertions.",
        "checkout": str(checkout),
        "timeout_seconds": timeout,
        "output_limit_bytes": output_limit,
        "verified_revision": None,
        "executions": [],
        "cases": cases,
        "carrier_vectors": vectors,
        "case_inventory": list(local.CASE_IDS),
        "raw_adapter_evidence": None,
        "raw_adapter_evidence_base64": None,
        "evidence_scope": "Independent implementation attempt by "
        "repository agent; not independent operator evidence or certification.",
        "publication": "PREPARED_IN_REPOSITORY; public publication "
        "and independent reproduction remain outstanding",
        "engineering_actions": [
            "Build the pinned sample wallet and connect a supported device/emulator.",
            "Implement an adapter executing all 19 assertions and both "
            "carrier vectors against that target.",
            "Rerun with --adapter-command and inspect the retained evidence.",
        ],
        "operator_actions": [
            "Arrange maintainer publication of the reviewed evidence.",
            "Reproduce the run independently and retain operator evidence.",
        ],
        "limitations": [
            "Local Go results, cached fixtures, upstream self-tests "
            "and builds are not external assertions.",
            "No device, independent operator, certification, ZKP policy "
            "or circuit security claim is established.",
        ],
    }


def run_attempt(
    checkout,
    preparation=(),
    adapter=None,
    timeout=120,
    output_limit=1048576,
    process=execute,
):
    report = base_report(checkout, timeout, output_limit)
    try:
        report["target"] = target_metadata()
        report["suite_provenance"] = suite_provenance()
        inventory = local.discover_case_ids((ROOT / "tests/e2e.sh").read_text())
    except (OSError, UnicodeError, ValueError, RecursionError) as exc:
        report.update(status="ERROR", detail=f"Suite metadata/input error: {exc}")
        return report

    def command(name, argv, cwd, env=None):
        result = process(name, argv, cwd, timeout, output_limit, env)
        report["executions"].append(result)
        return result

    if inventory != local.CASE_IDS:
        report.update(
            status="ERROR",
            detail="numbered assertion inventory differs from runner metadata",
        )
        return report
    for name, argv in [
        ("checkout-revision", ["git", "rev-parse", "HEAD"]),
        ("checkout-origin", ["git", "remote", "get-url", "origin"]),
        (
            "checkout-cleanliness",
            ["git", "status", "--porcelain", "--untracked-files=all"],
        ),
    ]:
        result = command(name, argv, checkout)
        if result["returncode"] != 0 or result["status"] == "ERROR":
            report["detail"] = (
                f"{name} unavailable: {result['detail'] or result['output']}"
            )
            return report
        observed = result["output"].strip()
        if name == "checkout-revision":
            report["verified_revision"] = observed
            valid = observed == REVISION
        elif name == "checkout-origin":
            valid = observed == URL
        else:
            valid = not observed
        if not valid:
            report.update(
                status="ERROR",
                detail=f"{name} mismatch; refusing attribution to reviewed target",
            )
            return report
    for index, argv in enumerate(preparation):
        result = command(f"preparation-{index + 1}", argv, checkout)
        if result["returncode"] != 0 or result["status"] == "ERROR":
            report["detail"] = (
                f"External preparation unavailable: "
                f"{result['detail'] or 'command exited nonzero'}"
            )
            return report
    # Preparation must preserve both source revision and tracked/untracked source.
    if preparation:
        for name, argv, expected in (
            ("post-preparation-revision", ["git", "rev-parse", "HEAD"], REVISION),
            (
                "post-preparation-cleanliness",
                ["git", "status", "--porcelain", "--untracked-files=all"],
                "",
            ),
        ):
            checked = command(name, argv, checkout)
            if (
                checked["returncode"] != 0
                or checked["status"] == "ERROR"
                or checked["output"].strip() != expected
            ):
                report.update(
                    status="ERROR",
                    detail="target changed or unavailable after preparation",
                )
                return report
    if not adapter:
        return report
    with tempfile.TemporaryDirectory(prefix="external-carrier-") as tmp:
        evidence = Path(tmp) / "evidence.json"
        env = os.environ.copy()
        env.update(
            EE_EXTERNAL_CARRIER_EVIDENCE=str(evidence),
            EE_EXTERNAL_CARRIER_REVISION=REVISION,
            EE_EXTERNAL_CARRIER_CHECKOUT=str(checkout),
            EE_EXTERNAL_CARRIER_ROOT=str(ROOT),
        )
        result = command("external-adapter", adapter, checkout, env)
        if evidence.exists() or evidence.is_symlink():
            try:
                raw = read_evidence(evidence, output_limit)
                report["raw_adapter_evidence_base64"] = base64.b64encode(raw).decode(
                    "ascii"
                )
                report["raw_adapter_evidence"] = raw.decode(
                    "utf-8", errors="backslashreplace"
                )
                payload = strict_json(raw.decode("utf-8", errors="strict"))
                cases, vectors = validate_evidence(payload)
            except (OSError, ValueError, UnicodeError, RecursionError) as exc:
                report.update(status="ERROR", detail=f"Invalid adapter evidence: {exc}")
                return report
            for supplied, validated in zip(
                sorted(
                    payload["cases"],
                    key=lambda row: int(row["case_id"]),
                ),
                cases,
                strict=True,
            ):
                validated["evidence"] = supplied["evidence"]
            report.update(cases=cases, carrier_vectors=vectors)
            attribution_ok = True
            for name, argv, expected in (
                ("post-adapter-revision", ["git", "rev-parse", "HEAD"], REVISION),
                (
                    "post-adapter-cleanliness",
                    ["git", "status", "--porcelain", "--untracked-files=all"],
                    "",
                ),
            ):
                checked = command(name, argv, checkout)
                if (
                    checked["returncode"] != 0
                    or checked["status"] == "ERROR"
                    or checked["output"].strip() != expected
                ):
                    attribution_ok = False
            if not attribution_ok:
                report.update(
                    status="ERROR",
                    detail=(
                        "Post-execution target attribution failed; "
                        "assertion rows retained"
                    ),
                )
            elif any(row["result"] == "FAIL" for row in cases + vectors):
                report.update(
                    status="FAIL",
                    detail="External assertion failures retained in evidence",
                )
            elif result["returncode"] != 0 or result["status"] == "ERROR":
                report.update(
                    status="ERROR",
                    detail=(
                        "Adapter process or post-execution target attribution failed"
                    ),
                )
            else:
                report.update(
                    status="PASS",
                    detail=(
                        "Complete validated adapter-reported external assertions; "
                        "no independent operator claim"
                    ),
                )
        elif result["returncode"] != 0 or result["status"] == "ERROR":
            report["detail"] = (
                f"Adapter unavailable or interrupted before evidence: "
                f"{result['detail'] or result['output']}"
            )
        else:
            report.update(
                status="ERROR", detail="Adapter exited without required evidence file"
            )
    return report


def inline(value):
    # Entity-encode Markdown punctuation and HTML delimiters; collapse newlines.
    return "".join(
        char if char.isalnum() or char in " /:.,;-" else f"&#{ord(char)};"
        for char in str(value).replace("\n", " ").replace("\r", " ")
    )


def fenced(content, language="text"):
    content = str(content)
    fence = "~" * max(
        3, 1 + max((len(m) for m in re.findall(r"~+", content)), default=0)
    )
    return [fence + language, content, fence]


def markdown(report):
    target = report["target"]
    lines = ["# External carrier attempt", ""]
    for label, value in (
        ("Status", report["status"]),
        ("Detail", report["detail"]),
        ("Date", report["generated_at"]),
        ("Target", target["name"]),
        ("Source", target["source_url"]),
        ("Canonical commit", target["revision"]),
        ("Observed commit", report["verified_revision"]),
        ("Selection", target["rationale"]),
        ("Host", json.dumps(report["host"], sort_keys=True)),
        ("Evidence scope", report["evidence_scope"]),
        ("Publication", report["publication"]),
    ):
        lines.append(f"{label}: {inline(value)}")
    if report.get("suite_provenance"):
        lines.extend(["", "## Observed suite provenance", ""])
        lines.extend(fenced(json.dumps(report["suite_provenance"], indent=2), "json"))
    lines.extend(["", "## Executed commands", ""])
    for execution in report["executions"]:
        lines.extend(
            [
                "### " + inline(execution["name"]),
                "CWD: " + inline(execution["cwd"]),
                "Command: " + inline(shlex.join(execution["command"])),
                "Exit: "
                + inline(execution["returncode"])
                + "; disposition: "
                + inline(execution["status"]),
                "Detail: " + inline(execution["detail"]),
                "",
            ]
        )
        lines.extend(fenced(execution["output"]))
    lines.extend(
        [
            "",
            "## Case inventory",
            "",
            "| Case | Carriers | Mode | Property | Result |",
            "|---|---|---|---|---|",
        ]
    )
    for row in report["cases"]:
        carriers, mode, _ = local.EXPECTED_CASE_METADATA[row["case_id"]]
        values = (
            row["case_id"],
            ", ".join(carriers) or "none expected",
            mode,
            local.EXPECTED_PROPERTIES.get(row["case_id"], "not-applicable"),
            row["result"],
        )
        lines.append("| " + " | ".join(inline(value) for value in values) + " |")
    lines.extend(["", "## Carrier vectors", ""])
    lines.extend(
        "- " + inline(f"{row['carrier']}: {row['result']}; {row['evidence']}")
        for row in report["carrier_vectors"]
    )
    lines.extend(["", "## Limitations and remaining steps", ""])
    lines.extend(
        "- " + inline(item)
        for item in (
            report["limitations"]
            + report["engineering_actions"]
            + report["operator_actions"]
        )
    )
    if report["raw_adapter_evidence"] is not None:
        lines.extend(["", "## Original adapter evidence", ""])
        lines.extend(fenced(report["raw_adapter_evidence"], "json"))
    return "\n".join(lines) + "\n"


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--checkout", type=Path, required=True)
    parser.add_argument(
        "--prepare-command",
        action="append",
        default=[],
        help="explicit argv, shlex split; repeatable; no shell",
    )
    parser.add_argument(
        "--adapter-command", help="adapter argv; writes EE_EXTERNAL_CARRIER_EVIDENCE"
    )
    parser.add_argument("--timeout", type=float, default=120)
    parser.add_argument("--output-limit", type=int, default=1048576)
    parser.add_argument("--json", type=Path, required=True)
    parser.add_argument("--markdown", type=Path, required=True)
    args = parser.parse_args(argv)
    checkout = args.checkout.resolve()
    if checkout == ROOT or ROOT in checkout.parents:
        parser.error("external checkout must be outside the repository")
    if not math.isfinite(args.timeout) or args.timeout <= 0 or args.output_limit <= 0:
        parser.error("timeout and output-limit must be positive")
    if args.json.resolve() == args.markdown.resolve():
        parser.error("JSON and Markdown paths must differ")
    try:
        preparation = [shlex.split(command) for command in args.prepare_command]
        adapter = (
            shlex.split(args.adapter_command)
            if args.adapter_command is not None
            else None
        )
        if any(not command for command in preparation) or adapter == []:
            raise ValueError("empty command argv")
    except ValueError as exc:
        parser.error(str(exc))
    try:
        report = run_attempt(
            checkout, preparation, adapter, args.timeout, args.output_limit
        )
    except (OSError, UnicodeError, ValueError, RecursionError) as exc:
        report = base_report(checkout, args.timeout, args.output_limit)
        report.update(
            status="ERROR", detail=f"External run operational/protocol error: {exc}"
        )
    for path, content in (
        (args.json, json.dumps(report, indent=2, sort_keys=True) + "\n"),
        (args.markdown, markdown(report)),
    ):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8")
    print(f"External carrier {report['status']}: {report['detail']}")
    return {"PASS": 0, "FAIL": 1, "BLOCKED": 2, "ERROR": 3}[report["status"]]


if __name__ == "__main__":
    sys.exit(main())
