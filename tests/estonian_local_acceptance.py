#!/usr/bin/env python3
"""Actual staged RP HTTP refusals and supplementary local EE AV proof evidence."""

from __future__ import annotations

import argparse
import json
import os
import signal
import socket
import subprocess
import sys
import tempfile
import time
from pathlib import Path

from rp_kit_test import BINARY, REPO, profile_command, request


def checked(command, timeout=30):
    return subprocess.run(  # noqa: S603 - fixed local binaries and private generated paths
        command, check=True, capture_output=True, text=True, timeout=timeout
    ).stdout


def execute(run_id, output):
    prover = Path(
        os.environ.get(
            "EE_PROVER",
            REPO.parent / "longfellow-zk/rust/target/release/examples/ee_poa_demo",
        )
    ).resolve()
    if not BINARY.is_file() or not os.access(prover, os.X_OK):
        raise ValueError("build verifier/prover first: make deps")
    with tempfile.TemporaryDirectory(prefix="estonian-local-") as directory:
        work = Path(directory)
        # Holder and signing keys live in the system temporary directory only.
        checked(
            [
                sys.executable,
                str(REPO / "issuer/mint_ee_poa.py"),
                "--out",
                str(work / "issuer"),
                "--doctype",
                "eu.europa.ec.av.1",
                "--over",
                "18",
            ]
        )
        descriptor = os.open(
            work / "response-key.pem", os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600
        )
        with os.fdopen(descriptor, "w") as stream:
            stream.write(
                checked(
                    ["openssl", "ecparam", "-name", "prime256v1", "-genkey", "-noout"]
                )
            )
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            port = listener.getsockname()[1]
        base = f"http://127.0.0.1:{port}"
        args, cwd = profile_command(work, base, port)
        common = [
            "-rp-register-file",
            str(REPO / "rp-kit/register/synthetic-v1.json"),
            "-dcapi-origin",
            "https://rp.example.ee",
            "-doctype",
            "eu.europa.ec.av.1",
            "-session-ttl",
            "3m",
        ]
        observations = []
        proof = None
        for registered in (False, True):
            log_path = work / f"server-{registered}.log"
            with log_path.open("w+") as log:
                process = subprocess.Popen(  # noqa: S603 - staged shipped profile
                    args
                    + common
                    + (
                        []
                        if registered
                        else ["-client-id", "x509_san_dns:omitted.example.ee"]
                    ),
                    cwd=cwd,
                    stdout=log,
                    stderr=log,
                )
                try:
                    deadline = time.monotonic() + 10
                    while True:
                        if process.poll() is not None or time.monotonic() > deadline:
                            log.seek(0)
                            raise ValueError("verifier startup failed: " + log.read())
                        try:
                            if request(base + "/healthz")[0] == 200:
                                break
                        except OSError:
                            pass
                        time.sleep(0.05)
                    if not registered:
                        for route in ("/present/new", "/present/dcapi/new"):
                            status, body = request(base + route, {})
                            if (
                                status != 403
                                or body.get("reason") != "register-not-found"
                                or any(
                                    key in body
                                    for key in ("id", "request_uri", "result_uri")
                                )
                            ):
                                raise ValueError(
                                    "expected absent registration refusal: "
                                    f"{status} {body}"
                                )
                            observations.append(
                                {
                                    "route": route,
                                    "http_status": status,
                                    "reason": body["reason"],
                                    "session_released": False,
                                }
                            )
                    else:
                        status, session = request(
                            base + "/present/new", {"element": "age_over_18"}
                        )
                        if status != 201:
                            raise ValueError(
                                "positive registration control failed: "
                                f"{status} {session}"
                            )
                        status, presentation_request = request(session["request_uri"])
                        if (
                            status != 200
                            or presentation_request.get("response_mode")
                            != "direct_post.jwt"
                        ):
                            raise ValueError("encrypted default changed")
                        request_file = work / "request.json"
                        request_file.write_text(json.dumps(presentation_request))
                        checked(
                            [
                                sys.executable,
                                str(REPO / "wallet/present.py"),
                                "--credential",
                                str(work / "issuer"),
                                "--verifier",
                                base,
                                "--prover",
                                str(prover),
                                "--request-file",
                                str(request_file),
                            ],
                            timeout=180,
                        )
                        status, result = request(
                            base + "/present/result/" + session["id"]
                        )
                        if (
                            status != 200
                            or result.get("valid") is not True
                            or result.get("answered") is not True
                        ):
                            raise ValueError(
                                f"actual local AV proof failed: {status} {result}"
                            )
                        proof = {
                            "scope": "supplementary-local-control",
                            "issuing_country": "EE",
                            "credential_kind": "AV",
                            "outcome": "proof-verified",
                            "response_mode": "direct_post.jwt",
                            "session_id": session["id"],
                            "verifier_result": result,
                            "path": "openid4vp (profiled, unofficial)",
                        }
                finally:
                    if process.poll() is None:
                        process.send_signal(signal.SIGTERM)
                        try:
                            process.wait(timeout=5)
                        except subprocess.TimeoutExpired:
                            process.kill()
                            process.wait(timeout=5)
        output.write_text(
            json.dumps(
                {
                    "schema": "ee-estonian-local/1",
                    "run_id": run_id,
                    "records": [
                        {
                            "case_id": "unregistered-rp",
                            "outcome": "register-not-found",
                            "routes": observations,
                        }
                    ],
                    "supplementary": [proof],
                },
                indent=2,
            )
            + "\n"
        )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    execute(args.run_id, args.output)


if __name__ == "__main__":
    main()
