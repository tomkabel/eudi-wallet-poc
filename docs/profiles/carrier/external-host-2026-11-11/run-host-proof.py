#!/usr/bin/env python3
"""Bounded public target host tests; never complete carrier acceptance."""

import argparse
import hashlib
import importlib
import json
import os
import platform
import sys
from datetime import UTC, datetime
from pathlib import Path
from xml.etree import ElementTree

ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(ROOT / "tests"))
external = importlib.import_module("run_external_carrier")
execute = external.execute
REVISION = external.REVISION
OUT = Path(__file__).resolve().parent
TARGET = Path("/tmp/eudi-carrier-multipaz-0.99.0")  # noqa: S108 - reviewed checkout


def provenance_valid(rows):
    """Require successful exact revision, URL and cleanliness observations."""
    expected = [REVISION, external.URL, ""]
    return len(rows) == 3 and all(
        row["status"] == "PASS"
        and row["returncode"] == 0
        and row["output"].strip() == value
        for row, value in zip(rows, expected, strict=True)
    )


def validate_vectors(payload):
    """Return compatibility result only for two complete real observations."""
    if (
        not isinstance(payload, dict)
        or payload.get("schema_version") != 1
        or payload.get("target_revision") != REVISION
    ):
        raise ValueError("vector payload provenance/schema mismatch")
    rows = payload.get("carrier_vectors")
    if not isinstance(rows, list) or len(rows) != 2:
        raise ValueError("two vector rows required")
    seen = set()
    for row in rows:
        if (
            not isinstance(row, dict)
            or row.get("carrier") not in external.local.EXPECTED_CARRIERS
            or row["carrier"] in seen
            or row.get("result") not in {"PASS", "FAIL"}
            or type(row.get("parsed")) is not bool
            or type(row.get("proof_verified")) is not bool
            or not isinstance(row.get("evidence"), str)
            or not row["evidence"].strip()
            or (row["proof_verified"] and not row["parsed"])
            or (row["result"] == "PASS") != row["proof_verified"]
        ):
            raise ValueError("invalid or duplicate vector observation")
        seen.add(row["carrier"])
    return "FAIL" if any(row["result"] == "FAIL" for row in rows) else "PASS"


def classify(process, before, after, payload=None, test_count=0, require_vectors=False):
    """Process success cannot substitute for valid fresh assertion evidence."""
    if not provenance_valid(before) or not provenance_valid(after):
        return "ERROR"
    if process["status"] != "PASS" or process["returncode"] != 0 or test_count != 1:
        return "ERROR"
    if require_vectors:
        return validate_vectors(payload)
    return "PASS"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vectors", action="store_true")
    options = parser.parse_args()
    env = os.environ.copy()
    env["JAVA_HOME"] = "/usr/lib/jvm/java-17-openjdk"
    env["ANDROID_HOME"] = "/opt/android-sdk"
    env["EE_HOST_HARNESS_DIR"] = str(OUT / "vector-harness")
    env["EE_VECTOR_ROOT"] = str(ROOT / "tests/vectors/carrier-v1")
    env["EE_VECTOR_OUTPUT"] = str(OUT / "vector-results.json")
    prefix = "host-vectors" if options.vectors else "host-proof"
    test_class = (
        "org.eudi.externalcarrier.RepositoryCarrierVectorTest"
        if options.vectors
        else "org.multipaz.mdoc.zkp.longfellow.SystemTest"
    )
    test_method = "feedBothVectors" if options.vectors else "testProofFullFlow_success"
    argv = [
        "./gradlew",
        "--offline",
        "--configure-on-demand",
        "--init-script",
        str(OUT / "vector-harness/init.gradle"),
        "--no-daemon",
        "--console=plain",
        "--max-workers=2",
        ":multipaz-longfellow:jvmTest",
        "--tests",
        test_class + "." + test_method,
    ]
    xml_path = TARGET / (
        "multipaz-longfellow/build/test-results/jvmTest/TEST-" + test_class + ".xml"
    )
    report = {
        "schema_version": 2,
        "generated_at": datetime.now(UTC).isoformat(),
        "scope": (
            "Repository vector compatibility only; zero of 19 flow cases executed"
            if options.vectors
            else "Upstream fresh proof self-test only; no carrier acceptance"
        ),
        "target": external.target_metadata(),
        "checkout": str(TARGET),
        "host": {
            "platform": platform.platform(),
            "machine": platform.machine(),
            "python": platform.python_version(),
            "JAVA_HOME": env["JAVA_HOME"],
            "ANDROID_HOME": env["ANDROID_HOME"],
        },
        "timeout_seconds": 600,
        "output_limit_bytes": 4194304,
        "executions": [],
        "cases": external.local.validate_case_records([])[0],
        "assertions": {"carrier_cases_executed": 0, "repository_vectors_executed": 0},
        "process_status": "NOT_EXECUTED",
        "result": "ERROR",
        "compatibility_result": "NOT_EXECUTED",
        "suite_provenance": external.suite_provenance(),
    }
    inputs = [
        Path(__file__),
        OUT / "vector-harness/init.gradle",
        OUT / "vector-harness/RepositoryCarrierVectorTest.kt",
        ROOT / "tests/vectors/carrier-v1/device_response.json.b64",
        ROOT / "tests/vectors/carrier-v1/device_response.cbor.b64",
        ROOT / "tests/vectors/carrier-v1/device_response_transcript.bin",
    ]
    report["input_sha256"] = {
        str(path.relative_to(ROOT)): hashlib.sha256(path.read_bytes()).hexdigest()
        for path in inputs
    }

    def provenance(suffix):
        rows = []
        for name, args in [
            ("revision", ["git", "rev-parse", "HEAD"]),
            ("origin", ["git", "remote", "get-url", "origin"]),
            ("cleanliness", ["git", "status", "--porcelain", "--untracked-files=all"]),
        ]:
            row = execute(name + suffix, args, TARGET, 10, 65536, env)
            rows.append(row)
            report["executions"].append(row)
        return rows

    before = provenance("-before")
    if provenance_valid(before):
        report["executions"].append(
            execute(
                "java-version",
                [env["JAVA_HOME"] + "/bin/java", "-version"],
                TARGET,
                10,
                65536,
                env,
            )
        )
        xml_path.unlink(missing_ok=True)
        if options.vectors:
            (OUT / "vector-results.json").unlink(missing_ok=True)
        process = execute("host-target-test", argv, TARGET, 600, 4194304, env)
        report["executions"].append(process)
        report["process_status"] = process["status"]
        after = provenance("-after")
        try:
            if not provenance_valid(after):
                raise ValueError("Target provenance mismatched after execution")
            xml = ElementTree.fromstring(xml_path.read_text())  # noqa: S314 - local test report
            test_count = int(xml.attrib["tests"])
            if (
                int(xml.attrib["failures"])
                or int(xml.attrib["errors"])
                or int(xml.attrib["skipped"])
            ):
                raise ValueError("target test did not execute successfully")
            (OUT / (prefix + "-test.xml")).write_text(xml_path.read_text())
            report["executed_test_count"] = test_count
            payload = None
            if options.vectors:
                payload = external.strict_json(
                    (OUT / "vector-results.json").read_text()
                )
                report["compatibility_result"] = validate_vectors(payload)
                report["vector_observations"] = payload
                report["assertions"]["repository_vectors_executed"] = 2
            report["result"] = classify(
                process, before, after, payload, test_count, options.vectors
            )
        except (
            OSError,
            ValueError,
            KeyError,
            TypeError,
            ElementTree.ParseError,
        ) as error:
            report["detail"] = str(error)
    else:
        report["detail"] = (
            "Target provenance unavailable or mismatched; test not executed"
        )
    report["finished_at"] = datetime.now(UTC).isoformat()
    (OUT / (prefix + ".json")).write_text(json.dumps(report, indent=2) + "\n")
    parts = [
        "# Multipaz host test attempt",
        "",
        report["scope"],
        "",
        f"Result: {report['result']}",
        f"Process status: {report['process_status']}",
        f"Compatibility result: {report['compatibility_result']}",
        "",
        "Full structured provenance, hashes and unexecuted inventory: `"
        + prefix
        + ".json`",
        "",
    ]
    if "vector_observations" in report:
        parts.extend(
            external.fenced(json.dumps(report["vector_observations"], indent=2), "json")
        )
        parts.append("")
    for row in report["executions"]:
        parts += [
            f"## {external.inline(row['name'])}",
            "",
            f"argv: {external.inline(json.dumps(row['command']))}",
            "status: "
            + external.inline(row["status"])
            + "; returncode: "
            + external.inline(row["returncode"]),
            "",
        ]
        parts.extend(external.fenced(row["output"]))
        parts.append("")
    parts += [
        "Presentation trims trailing whitespace; raw JSON retains original output."
    ]
    rendered = "\n".join(line.rstrip() for line in "\n".join(parts).splitlines())
    (OUT / (prefix + ".md")).write_text(rendered + "\n")
    print(
        json.dumps(
            {
                key: report[key]
                for key in (
                    "result",
                    "process_status",
                    "compatibility_result",
                    "assertions",
                )
            }
        )
    )


if __name__ == "__main__":
    main()
