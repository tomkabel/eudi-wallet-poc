#!/usr/bin/env python3
"""Opt-in browser integration. Stdout is one sanitized terminal JSON report."""

import argparse
import contextlib
import datetime
import hashlib
import json
import os
import shutil
import socket
import subprocess
import sys
import threading
import time
from http.cookies import CookieError, SimpleCookie
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlsplit

from adapter import Adapter, Blocked, Config, Refused

ROOT = Path(__file__).resolve().parents[2]


def provenance():
    try:
        revision = subprocess.run(  # noqa: S603 - fixed git arguments, no caller input
            [shutil.which("git") or "/usr/bin/git", "rev-parse", "HEAD"],
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=3,
            check=True,
        ).stdout.strip()
        if len(revision) != 40 or any(c not in "0123456789abcdef" for c in revision):
            return "UNAVAILABLE"
        return revision
    except (OSError, subprocess.SubprocessError):
        return "UNAVAILABLE"


def serve(adapter, port, timeout):
    deadline = time.monotonic() + timeout
    result = {}
    callback_path = urlsplit(adapter.config.redirect_uri).path

    class Handler(BaseHTTPRequestHandler):
        def setup(self):
            self.request.settimeout(2)
            super().setup()
            self.request_deadline = threading.Timer(
                max(0.01, min(2, deadline - time.monotonic())), self.expire_request
            )
            self.request_deadline.daemon = True
            self.request_deadline.start()

        def expire_request(self):
            with contextlib.suppress(OSError):
                self.request.shutdown(socket.SHUT_RDWR)

        def finish(self):
            self.request_deadline.cancel()
            with contextlib.suppress(OSError):
                super().finish()

        def log_message(self, *_):
            pass  # BaseHTTPRequestHandler would log callback query strings.

        def do_GET(self):
            self.request_deadline.cancel()
            if len(self.path) > 8192:
                self.send_error(414)
                return
            path = urlsplit(self.path)
            cookie = "__Host-TARA"
            try:
                if path.path == "/login" and not path.query:
                    target, binding = adapter.begin()
                    self.send_response(302)
                    self.send_header("Location", target)
                    self.send_header(
                        "Set-Cookie",
                        f"{cookie}={binding}; Path=/; Secure; HttpOnly; "
                        f"SameSite=Lax; Max-Age={adapter.ttl}",
                    )
                    self.send_header("Cache-Control", "no-store")
                    self.send_header("Referrer-Policy", "no-referrer")
                    self.end_headers()
                    return
                if path.path != callback_path:
                    self.send_error(404)
                    return
                try:
                    fields = parse_qs(
                        path.query, keep_blank_values=True, max_num_fields=8
                    )
                    known = any(
                        state in adapter.transactions
                        for state in fields.get("state", [])
                    )
                except ValueError:
                    known = False
                if not known:
                    self.send_response(403)
                    self.send_header("Cache-Control", "no-store")
                    self.send_header("Referrer-Policy", "no-referrer")
                    self.end_headers()
                    self.wfile.write(b"Unrelated authentication callback refused.\n")
                    return
                cookies = SimpleCookie()
                cookies.load(self.headers.get("Cookie", ""))
                binding = cookies[cookie].value if cookie in cookies else ""
                validated = adapter.callback(path.query, binding)
                result.update(
                    outcome=validated["outcome"],
                    authenticated=validated["authenticated"],
                    reason="verified_code_exchange"
                    if validated["authenticated"]
                    else "cancelled",
                )
            except Blocked as exc:
                result.update(outcome="BLOCKED", authenticated=False, reason=str(exc))
            except (Refused, CookieError, ValueError):
                result.update(
                    outcome="FAIL", authenticated=False, reason="authentication_refused"
                )
            self.send_response(200 if result.get("authenticated") else 403)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Referrer-Policy", "no-referrer")
            self.send_header(
                "Set-Cookie",
                f"{cookie}=; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=0",
            )
            self.end_headers()
            self.wfile.write(
                b"Authentication attempt complete. See the operator terminal.\n"
            )

    with HTTPServer(("127.0.0.1", port), Handler) as server:
        server.timeout = 0.25
        public = urlsplit(adapter.config.redirect_uri)
        print(
            f"Open https://{public.netloc}/login through the configured HTTPS proxy.",
            file=sys.stderr,
        )
        while time.monotonic() < deadline and not result:
            try:
                server.handle_request()
            except (OSError, TimeoutError):
                continue
    adapter.transactions.clear()
    return result or {
        "outcome": "BLOCKED",
        "authenticated": False,
        "reason": "browser_run_timeout",
    }


def main(argv=None, *, environ=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--timeout", type=int, default=300)
    args = parser.parse_args(argv)
    env = os.environ if environ is None else environ
    report = {
        "schema": "tara-integration-v1",
        "issuer": "https://tara-test.ria.ee",
        "provenance": "external-tara-test",
        "revision": provenance(),
        "date": datetime.datetime.now(datetime.UTC).isoformat(),
        "command": "python3 rp-kit/tara/run_integration.py "
        f"--port {args.port} --timeout {args.timeout}",
        "effective_options": {"port": args.port, "timeout": args.timeout},
        "operator_actions": [],
        "source_sha256": {
            name: hashlib.sha256(
                Path(__file__).with_name(name).read_bytes()
            ).hexdigest()
            for name in ("adapter.py", "run_integration.py")
        },
    }
    try:
        config = Config(
            env.get("TARA_RIA_AGREEMENT_CONFIRMED") == "yes",
            env.get("TARA_REGISTERED_TEST_CLIENT_CONFIRMED") == "yes",
            env.get("TARA_CLIENT_ID", ""),
            env.get("TARA_CLIENT_SECRET", ""),
            env.get("TARA_REDIRECT_URI", ""),
            env.get("TARA_MINIMUM_ACR", "substantial"),
            env.get("TARA_CA_BUNDLE", ""),
        )
        config.require()
        if not 1 <= args.port <= 65535 or not 1 <= args.timeout <= 600:
            raise Blocked("invalid_run_bounds")
        if report["revision"] == "UNAVAILABLE":
            raise Blocked("canonical_revision_unavailable")
        report.update(serve(Adapter(config), args.port, args.timeout))
    except Blocked as exc:
        report.update(outcome="BLOCKED", authenticated=False, reason=str(exc))
    except Refused:
        report.update(
            outcome="FAIL", authenticated=False, reason="authentication_refused"
        )
    except (OSError, ValueError):
        report.update(
            outcome="BLOCKED", authenticated=False, reason="local_runner_unavailable"
        )
    except KeyboardInterrupt:
        report.update(
            outcome="CANCELLED", authenticated=False, reason="operator_cancelled"
        )
    except Exception:
        report.update(
            outcome="ERROR", authenticated=False, reason="unexpected_runner_error"
        )
    if report["outcome"] != "PASS":
        report["operator_actions"] = [
            "Obtain institutional RIA agreement and registered TARA "
            "test-client credentials.",
            "Configure the exact registered HTTPS callback, trusted TLS proxy "
            "and server-held environment credentials.",
            "Confirm runtime prerequisites and complete a real browser login "
            "with make test-tara-integration.",
        ]
    print(json.dumps(report, sort_keys=True))
    return 0 if report["outcome"] == "PASS" and report["authenticated"] else 1


if __name__ == "__main__":
    sys.exit(main())
