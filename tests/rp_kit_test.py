#!/usr/bin/env python3
"""Bounded real-process acceptance of the ADR-004 deployment profile."""

import configparser
import json
import os
import re
import shlex
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from contextlib import ExitStack
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
BINARY = Path(os.environ.get("EE_VERIFIER", REPO / "verifier/zkverify"))


def profile_command(work, base, port):
    """Stage the shipped installation and expand systemd's ${VAR} arguments."""
    unit = configparser.ConfigParser(interpolation=None)
    unit.read(REPO / "rp-kit/zkverify.service")
    service = unit["Service"]

    def staged(path):
        if not path.startswith("/"):
            raise ValueError(f"profile installation path must be absolute: {path}")
        return work / path.lstrip("/")

    for path in ("/usr/local/libexec", "/etc/zkverify", "/var/lib/zkverify"):
        staged(path).mkdir(parents=True)
    for source, target in (
        (BINARY, "/usr/local/libexec/zkverify"),
        (REPO / "verifier/circuits.json", "/etc/zkverify/circuits.json"),
        (work / "issuer/issuers.json", "/etc/zkverify/issuers.json"),
        (
            REPO / "verifier/zkverify-ffi/longfellow-rev.txt",
            "/etc/zkverify/longfellow-rev.txt",
        ),
        (REPO / "rp-kit/zkverify.env.example", "/etc/zkverify/zkverify.env"),
        (work / "response-key.pem", "/var/lib/zkverify/response-key.pem"),
    ):
        shutil.copy2(source, staged(target))
    env = {}
    # The shipped template uses KEY=value, with optional shell-style quoting.
    # Read the EnvironmentFile selected by the unit, not an independent flag set.
    for line in staged(service["EnvironmentFile"]).read_text().splitlines():
        line = line.strip()
        if not line or line.startswith(("#", ";")):
            continue
        name, value = line.split("=", 1)
        words = shlex.split(value)
        env[name] = " ".join(words)
    # Local acceptance changes only listener/public URL and bounded timing.
    env.update(
        ADDR=f"127.0.0.1:{port}", BASE_URL=base, SESSION_TTL="4s", DRAIN_TIMEOUT="1s"
    )
    arguments = []
    for word in shlex.split(service["ExecStart"]):
        # Expand after splitting: ${EMPTY} must remain one empty argv element.
        value = re.sub(
            r"\$\{([A-Za-z_][A-Za-z_0-9]*)\}", lambda match: env.get(match[1], ""), word
        )
        if "$" in value or "%" in value:
            raise ValueError(f"unsupported systemd expansion: {word}")
        arguments.append(str(staged(value)) if value.startswith("/") else value)
    expected_key = str(staged("/var/lib/zkverify/response-key.pem"))
    if Path(expected_key).stat().st_mode & 0o777 != 0o600:
        raise AssertionError("installed response key must have mode 0600")
    if arguments[arguments.index("-response-key-file") + 1] != expected_key:
        raise AssertionError("unit must use the installed persistent response key")
    if "-response-mode" in arguments or "-allow-unencrypted-response" in arguments:
        raise AssertionError("unit must preserve the encrypted response default")
    # Validate native unit syntax without privileged host installation.
    validator = shutil.which("systemd-analyze")
    if validator:
        validation_unit = work / "zkverify.service"
        validation_unit.write_text(
            (REPO / "rp-kit/zkverify.service")
            .read_text()
            .replace(
                "/usr/local/libexec/zkverify",
                str(staged("/usr/local/libexec/zkverify")),
            )
        )
        subprocess.run(  # noqa: S603 - installed validator and staged unit
            [validator, "verify", str(validation_unit)], check=True, timeout=10
        )
        print("PASS systemd unit syntax (staged executable)", flush=True)
    else:
        print("SKIP systemd unit syntax: systemd-analyze unavailable", flush=True)
    return arguments, staged(service["WorkingDirectory"])


def request(url, payload=None):
    data = None if payload is None else json.dumps(payload).encode()
    if not url.startswith("http://127.0.0.1:"):
        raise ValueError("acceptance only permits the local test server")
    req = urllib.request.Request(  # noqa: S310 - validated loopback HTTP URL
        url, data=data, headers={"Content-Type": "application/json"}
    )
    try:
        response = urllib.request.urlopen(req, timeout=2)  # noqa: S310 - loopback only
    except urllib.error.HTTPError as error:
        response = error
    with response:
        body = response.read()
        return response.status, (json.loads(body) if body.strip() != b"ok" else {})


def main():
    if not (BINARY.is_file()):
        raise AssertionError("build verifier first: make deps")
    with (
        tempfile.TemporaryDirectory(prefix="rp-kit-") as directory,
        ExitStack() as files,
    ):
        work = Path(directory)
        subprocess.run(  # noqa: S603 - fixed commands and locally generated paths
            [
                sys.executable,
                str(REPO / "issuer/mint_ee_poa.py"),
                "--out",
                str(work / "issuer"),
                "--over",
                "18",
            ],
            check=True,
            stdout=subprocess.DEVNULL,
            timeout=20,
        )
        key = work / "response-key.pem"
        # Create with 0600 from the outset, including while openssl writes it.
        descriptor = os.open(key, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        with os.fdopen(descriptor, "wb") as output:
            subprocess.run(  # noqa: S603 - fixed commands and locally generated paths
                [
                    shutil.which("openssl") or "/usr/bin/openssl",
                    "ecparam",
                    "-name",
                    "prime256v1",
                    "-genkey",
                    "-noout",
                ],
                stdout=output,
                check=True,
                timeout=5,
            )
        if not (key.stat().st_mode & 0o777 == 0o600):
            raise AssertionError(
                "profile assertion failed: key.stat().st_mode & 0o777 == 0o600"
            )
        with socket.socket() as listener:
            listener.bind(("127.0.0.1", 0))
            port = listener.getsockname()[1]
        base = f"http://127.0.0.1:{port}"
        args, working_directory = profile_command(work, base, port)
        processes = []

        def start(extra=()):
            log = files.enter_context(
                open(work / f"process-{len(processes)}.log", "w+")
            )
            process = subprocess.Popen(  # noqa: S603 - staged profile binary/flags
                args + list(extra), cwd=working_directory, stdout=log, stderr=log
            )
            processes.append((process, log))
            deadline = time.monotonic() + 10
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    log.seek(0)
                    raise AssertionError(f"startup failed: {log.read()}")
                try:
                    code, health = request(base + "/healthz")
                    if code == 200:
                        if not (health.get("draining") is not True):
                            raise AssertionError(
                                "profile response did not match expectations"
                            )
                        return process
                except (OSError, ValueError):
                    pass
                time.sleep(0.05)
            log.flush()
            log.seek(0)
            raise AssertionError(f"startup exceeded 10 seconds: {log.read()}")

        def stop(process):
            process.send_signal(signal.SIGTERM)
            if not (process.wait(timeout=6) == 0):
                raise AssertionError(
                    "profile assertion failed: process.wait(timeout=6) == 0"
                )

        try:
            for extra, message in [
                (["-session-ttl", "0s"], "-session-ttl must be positive"),
                (["-session-ttl", "-1s"], "-session-ttl must be positive"),
                (
                    ["-response-mode", "direct_post"],
                    "needs -allow-unencrypted-response",
                ),
            ]:
                result = subprocess.run(args + extra, capture_output=True, timeout=5)  # noqa: S603 - fixed local binary/flags
                if not (result.returncode != 0):
                    raise AssertionError(extra)
                if message not in result.stderr.decode():
                    raise AssertionError(result.stderr)
            print("PASS nonpositive TTL and plain-alone startup refusal", flush=True)
            process = start(["-dcapi-origin", "https://rp.example.ee"])
            for route in ("/present/new", "/present/dcapi/new"):
                code, refusal = request(base + route, {})
                if (
                    code != 503
                    or refusal.get("reason") != "missing-register-prerequisite"
                ):
                    raise AssertionError(refusal)
            stop(process)
            print(
                "PASS shipped unconfigured profile refuses both presentations",
                flush=True,
            )
            args += [
                "-rp-register-file",
                str(REPO / "rp-kit/register/synthetic-v1.json"),
            ]
            process = start()
            _, jwks = request(base + "/present/jwks.json")
            if not (len(jwks["keys"]) == 1 and "d" not in jwks["keys"][0]):
                raise AssertionError("profile response did not match expectations")
            code, session = request(base + "/present/new", {"element": "age_over_18"})
            if not (code == 201):
                raise AssertionError(session)
            identifier = session["id"]
            code, obj = request(session["request_uri"])
            if not (code == 200 and obj["response_mode"] == "direct_post.jwt"):
                raise AssertionError(obj)
            if not (obj["client_metadata"]["jwks"] == jwks):
                raise AssertionError("profile response did not match expectations")
            code, pending = request(base + "/present/result/" + identifier)
            if not (
                code == 200 and pending["id"] == identifier and not pending["answered"]
            ):
                raise AssertionError("profile response did not match expectations")
            print(
                "PASS health, encrypted default, public JWKS and pending identity",
                flush=True,
            )
            time.sleep(4.1)
            for _ in range(2):
                code, outcome = request(base + "/present/result/" + identifier)
                if not (code == 410 and outcome["id"] == identifier):
                    raise AssertionError(outcome)
                if not (outcome["error"] == "oid4vp: session expired"):
                    raise AssertionError("profile response did not match expectations")
                time.sleep(0.55)  # repeated observation after a janitor tick
            for route in ("/present/result/",):
                code, outcome = request(base + route + "unknown-id")
                if not (code == 404 and outcome["id"] == "unknown-id"):
                    raise AssertionError(outcome)
            print(
                "PASS no callback: repeated identifiable expiry and unknown ids",
                flush=True,
            )
            stop(process)
            process = start(["-dcapi-origin", "https://rp.example.ee"])
            code, outcome = request(base + "/present/dcapi/result/unknown-id")
            if code != 404 or outcome.get("id") != "unknown-id":
                raise AssertionError(outcome)
            if not (request(base + "/present/jwks.json")[1] == jwks):
                raise AssertionError("profile response did not match expectations")
            code, session = request(
                base + "/present/dcapi/new", {"element": "age_over_18"}
            )
            if code != 201:
                raise AssertionError(session)
            identifier = session["id"]
            code, pending = request(session["result_uri"])
            if code != 200 or pending.get("id") != identifier or pending["answered"]:
                raise AssertionError(pending)
            time.sleep(4.1)
            for _ in range(2):
                code, outcome = request(session["result_uri"])
                if code != 410 or outcome.get("id") != identifier:
                    raise AssertionError(outcome)
                if outcome.get("error") != "oid4vp: session expired":
                    raise AssertionError(outcome)
                time.sleep(0.55)
            stop(process)
            print(
                "PASS stable JWKS across restart and optional DCAPI pending/expiry",
                flush=True,
            )
            installed_key = Path(args[args.index("-response-key-file") + 1])
            replacement_key = installed_key.with_suffix(".next.pem")
            descriptor = os.open(
                replacement_key, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600
            )
            with os.fdopen(descriptor, "wb") as output:
                subprocess.run(  # noqa: S603 - fixed local OpenSSL command
                    [
                        shutil.which("openssl") or "/usr/bin/openssl",
                        "ecparam",
                        "-name",
                        "prime256v1",
                        "-genkey",
                        "-noout",
                    ],
                    stdout=output,
                    check=True,
                    timeout=5,
                )
            process = start()
            replacement_key.replace(installed_key)
            if request(base + "/present/jwks.json")[1] != jwks:
                raise AssertionError("file swap changed the running process key")
            stop(process)
            process = start()
            rotated_jwks = request(base + "/present/jwks.json")[1]
            if rotated_jwks == jwks:
                raise AssertionError("rotation did not change JWKS after restart")
            if installed_key.stat().st_mode & 0o777 != 0o600:
                raise AssertionError("rotated response key must have mode 0600")
            stop(process)
            process = start()
            if request(base + "/present/jwks.json")[1] != rotated_jwks:
                raise AssertionError("replacement key did not persist across restart")
            stop(process)
            print(
                "PASS private key swap, drain, rotation and stable replacement JWKS",
                flush=True,
            )
            process = start(
                ["-response-mode", "direct_post", "-allow-unencrypted-response"]
            )
            _, session = request(base + "/present/new", {"element": "age_over_18"})
            code, obj = request(session["request_uri"])
            if not (code == 200 and obj["response_mode"] == "direct_post"):
                raise AssertionError(obj)
            stop(process)
            print(
                "PASS explicitly paired plain negative control and SIGTERM exit",
                flush=True,
            )
        finally:
            for process, log in processes:
                if process.poll() is None:
                    process.kill()
                    process.wait(timeout=5)
                log.close()


if __name__ == "__main__":
    main()
