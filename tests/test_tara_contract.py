"""Real loopback HTTP controls; none of these tests constitute live TARA evidence."""

import base64
import datetime
import http.client
import io
import json
import socket
import socketserver
import ssl
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from contextlib import redirect_stdout
from dataclasses import replace
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from unittest.mock import patch
from urllib.parse import parse_qs, urlencode, urlsplit

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
from jwcrypto import jwk, jws

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "rp-kit/tara"))
import adapter  # noqa: E402 # pyright: ignore[reportMissingImports]
import run_integration  # noqa: E402 # pyright: ignore[reportMissingImports]


class Provider:
    def __init__(self):
        self.key = jwk.JWK.generate(kty="RSA", size=2048, kid="test-key")
        self.other = jwk.JWK.generate(kty="RSA", size=2048, kid="test-key")
        self.requests = []
        self.claim_changes = {}
        self.header_changes = {}
        self.nonce = ""
        self.token_override: dict | None = None
        self.token_changes = {}
        self.discovery_changes = {}
        self.key_override: list | None = None
        self.status = 200
        self.redirect = False
        self.raw: bytes | None = None
        self.signing_key = self.key
        owner = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *_):
                pass

            def do_GET(self):
                owner.requests.append((self.path, None, None))
                if self.path == "/.well-known/openid-configuration":
                    data = {
                        "issuer": owner.issuer,
                        "authorization_endpoint": owner.issuer + "/authorize",
                        "token_endpoint": owner.issuer + "/token",
                        "jwks_uri": owner.issuer + "/jwks",
                        "id_token_signing_alg_values_supported": ["RS256"],
                    }
                    data.update(owner.discovery_changes)
                else:
                    data = {
                        "keys": owner.key_override
                        if owner.key_override is not None
                        else [owner.key.export_public(as_dict=True)]
                    }
                self.reply(data)

            def do_POST(self):
                body = self.rfile.read(int(self.headers["Content-Length"]))
                owner.requests.append(
                    (self.path, body.decode(), self.headers.get("Authorization"))
                )
                if owner.token_override is not None:
                    data = owner.token_override
                else:
                    now = time.time()
                    claims = {
                        "iss": owner.issuer,
                        "aud": "client :+",
                        "sub": "synthetic-subject",
                        "nonce": owner.nonce,
                        "iat": now - 1,
                        "exp": now + 40,
                        "acr": "high",
                    }
                    claims.update(owner.claim_changes)
                    header = {"alg": "RS256", "kid": "test-key"}
                    header.update(owner.header_changes)
                    token = jws.JWS(json.dumps(claims).encode())
                    token.add_signature(owner.signing_key, protected=json.dumps(header))
                    data = {
                        "id_token": token.serialize(compact=True),
                        "access_token": "synthetic-access-token",
                        "token_type": "Bearer",
                    }
                    data.update(owner.token_changes)
                self.reply(data)

            def reply(self, data):
                self.send_response(
                    302 if owner.redirect and self.command == "POST" else owner.status
                )
                self.send_header("Content-Type", "application/json")
                self.send_header("Location", owner.issuer + "/stolen")
                self.end_headers()
                self.wfile.write(
                    owner.raw if owner.raw is not None else json.dumps(data).encode()
                )

        self.server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        self.issuer = "http://127.0.0.1:" + str(self.server.server_port)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()

    def close(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()


class ContractTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.provider = Provider()

    @classmethod
    def tearDownClass(cls):
        cls.provider.close()

    def setUp(self):
        p = self.provider
        p.requests = []
        p.claim_changes, p.header_changes, p.discovery_changes = {}, {}, {}
        p.token_override, p.key_override, p.raw = None, None, None
        p.token_changes = {}
        p.status, p.redirect, p.signing_key = 200, False, p.key
        self.config = adapter.Config(
            True, True, "client :+", "secret :+", "https://client.example/callback"
        )
        self.client = adapter.Adapter(self.config, _test_provider=p.issuer)

    def flow(self):
        url, browser = self.client.begin()
        query = parse_qs(urlsplit(url).query)
        self.provider.nonce = query["nonce"][0]
        return urlencode(
            {"state": query["state"][0], "code": "sensitive-code"}
        ), browser

    def test_local_success_http_and_basic_encoding(self):
        query, browser = self.flow()
        result = self.client.callback(query, browser)
        self.assertEqual(result["outcome"], "LOCAL_TEST")
        self.assertTrue(result["authenticated"])
        post = next(r for r in self.provider.requests if r[1])
        self.assertEqual(
            base64.b64decode(post[2][6:]).decode(), "client+%3A%2B:secret+%3A%2B"
        )
        self.assertEqual(parse_qs(post[1])["redirect_uri"], [self.config.redirect_uri])
        self.assertEqual(self.client.transactions, {})

    def test_authorization_request_exact_fields(self):
        self.client.config = replace(self.config, minimum_acr="high")
        target, browser = self.client.begin()
        uri = urlsplit(target)
        self.assertEqual(
            uri.scheme + "://" + uri.netloc + uri.path,
            self.provider.issuer + "/authorize",
        )
        fields = parse_qs(uri.query)
        self.assertEqual(
            set(fields),
            {
                "response_type",
                "scope",
                "client_id",
                "redirect_uri",
                "state",
                "nonce",
                "acr_values",
            },
        )
        self.assertEqual(fields["response_type"], ["code"])
        self.assertEqual(fields["scope"], ["openid"])
        self.assertEqual(fields["client_id"], [self.config.client_id])
        self.assertEqual(fields["redirect_uri"], [self.config.redirect_uri])
        self.assertEqual(fields["acr_values"], ["high"])
        self.assertGreaterEqual(len(fields["nonce"][0]), 32)
        self.assertGreaterEqual(len(browser), 32)
        self.assertIn(fields["state"][0], self.client.transactions)

    def test_discovery_algorithm_metadata_types(self):
        for value in (None, 5, "RS256", {"RS256": True}, ["RS256", 5], []):
            with self.subTest(value=value):
                self.provider.discovery_changes = {
                    "id_token_signing_alg_values_supported": value
                }
                with self.assertRaises(adapter.Refused):
                    self.client.begin()

    def test_config_change_cannot_reduce_assurance(self):
        self.client.config = replace(self.config, minimum_acr="high")
        query, browser = self.flow()
        before = len(self.provider.requests)
        self.client.config = self.config
        with self.assertRaisesRegex(
            adapter.Refused, "transaction_configuration_changed"
        ):
            self.client.callback(query, browser)
        self.assertEqual(len(self.provider.requests), before)

    def test_success_response_shape_and_error_mixture(self):
        for change in (
            {"error": "invalid_grant"},
            {"access_token": None},
            {"access_token": ""},
            {"token_type": None},
            {"token_type": "other"},
            {"id_token": None},
        ):
            with self.subTest(change=change):
                query, browser = self.flow()
                self.provider.token_changes = change
                with self.assertRaises(adapter.Refused):
                    self.client.callback(query, browser)
        for missing in ("access_token", "token_type", "id_token"):
            query, browser = self.flow()
            self.provider.token_override = {
                key: "value"
                for key in ("access_token", "token_type", "id_token")
                if key != missing
            }
            with self.assertRaises(adapter.Refused):
                self.client.callback(query, browser)
        self.provider.token_override = None
        query, browser = self.flow()
        query = query.split("&code=")[0] + "&error=access_denied&id_token=secret"
        with self.assertRaises(adapter.Refused):
            self.client.callback(query, browser)

    def test_missing_prerequisites_gate_begin_and_callback_without_network(self):
        for field, value in (
            ("agreement_confirmed", False),
            ("registered_client_confirmed", False),
            ("client_id", ""),
            ("client_secret", ""),
            ("redirect_uri", ""),
        ):
            with self.subTest(field=field):
                self.client.config = replace(self.config, **{field: value})
                with self.assertRaises(adapter.Blocked):
                    self.client.begin()
                with self.assertRaises(adapter.Blocked):
                    self.client.callback("state=secret&code=secret", "secret")
        self.assertEqual(self.provider.requests, [])

    def test_callback_requires_current_attestation(self):
        query, browser = self.flow()
        count = len(self.provider.requests)
        self.client.config = replace(self.config, agreement_confirmed=False)
        with self.assertRaises(adapter.Blocked):
            self.client.callback(query, browser)
        self.assertEqual(len(self.provider.requests), count)

    def test_callback_state_binding_replay_expiry_and_duplicates(self):
        for mode in (
            "state",
            "browser",
            "expiry",
            "duplicate",
            "missing_code",
            "oversize",
        ):
            with self.subTest(mode=mode):
                query, browser = self.flow()
                if mode == "state":
                    query = "state=wrong&code=secret"
                elif mode == "browser":
                    browser = "other-browser"
                elif mode == "expiry":
                    self.client.monotonic = lambda: time.monotonic() + 601
                elif mode == "duplicate":
                    query += "&code=another"
                elif mode == "missing_code":
                    query = query.split("&code=")[0]
                else:
                    query += "&huge=" + "a" * 8200
                before = len(self.provider.requests)
                with self.assertRaises(adapter.Refused):
                    self.client.callback(query, browser)
                self.assertEqual(len(self.provider.requests), before)
                self.client.monotonic = time.monotonic
        query, browser = self.flow()
        self.client.callback(query, browser)
        with self.assertRaises(adapter.Refused):
            self.client.callback(query, browser)

    def test_cancelled_and_provider_error(self):
        for error in ("access_denied", "invalid_request"):
            query, browser = self.flow()
            query = query.split("&code=")[0] + "&error=" + error
            count = len(self.provider.requests)
            if error == "access_denied":
                self.assertEqual(
                    self.client.callback(query, browser)["outcome"], "CANCELLED"
                )
            else:
                with self.assertRaises(adapter.Refused):
                    self.client.callback(query, browser)
            self.assertEqual(len(self.provider.requests), count)

    def test_claim_failure_matrix(self):
        for changes in (
            {"iss": "https://evil.example"},
            {"aud": "other"},
            {"aud": ["other", "client :+"]},
            {"azp": "other"},
            {"aud": ["client :+", "other"], "azp": "other"},
            {"nonce": "wrong"},
            {"exp": time.time() - 1},
            {"iat": time.time() + 20},
            {"iat": True},
            {"exp": "tomorrow"},
            {"nbf": time.time() + 20},
            {"acr": "low"},
            {"acr": "unknown"},
            {"sub": ""},
        ):
            with self.subTest(changes=changes):
                query, browser = self.flow()
                self.provider.claim_changes = changes
                with self.assertRaises(adapter.Refused):
                    self.client.callback(query, browser)
                self.assertEqual(
                    self.client.transactions.get(parse_qs(query)["state"][0]), None
                )

    def test_multiple_audiences_with_authorized_party(self):
        query, browser = self.flow()
        self.provider.claim_changes = {
            "aud": ["client :+", "other"],
            "azp": "client :+",
        }
        self.assertTrue(self.client.callback(query, browser)["authenticated"])

    def test_signature_kid_key_and_algorithm_failures(self):
        for mode in (
            "signature",
            "kid",
            "duplicate_kid",
            "no_keys",
            "bad_use",
            "algorithm",
            "malformed",
        ):
            with self.subTest(mode=mode):
                self.setUp()
                query, browser = self.flow()
                if mode == "signature":
                    self.provider.signing_key = self.provider.other
                elif mode == "kid":
                    self.provider.header_changes = {"kid": "absent"}
                elif mode == "duplicate_kid":
                    key = self.provider.key.export_public(as_dict=True)
                    self.provider.key_override = [key, key]
                elif mode == "no_keys":
                    self.provider.key_override = []
                elif mode == "bad_use":
                    self.provider.key_override = [
                        dict(self.provider.key.export_public(as_dict=True), use="enc")
                    ]
                elif mode == "algorithm":
                    token = jws.JWS(b"{}")
                    token.add_signature(
                        self.provider.key,
                        protected=json.dumps({"alg": "RS512", "kid": "test-key"}),
                    )
                    self.provider.token_override = {
                        "id_token": token.serialize(compact=True)
                    }
                else:
                    self.provider.token_override = {"id_token": "not.jwt"}
                with self.assertRaises(adapter.Refused):
                    self.client.callback(query, browser)

    def test_provider_redirect_never_posts_credentials_to_target(self):
        query, browser = self.flow()
        self.provider.redirect = True
        with self.assertRaises(adapter.Blocked):
            self.client.callback(query, browser)
        self.assertNotIn("/stolen", [r[0] for r in self.provider.requests])

    def test_discovery_body_http_and_connection_controls(self):
        for mode in (
            "issuer",
            "endpoint",
            "malformed",
            "duplicate",
            "oversize",
            "http",
        ):
            with self.subTest(mode=mode):
                self.setUp()
                if mode == "issuer":
                    self.provider.discovery_changes = {"issuer": "https://evil.example"}
                elif mode == "endpoint":
                    self.provider.discovery_changes = {
                        "token_endpoint": "https://evil.example/token"
                    }
                elif mode == "malformed":
                    self.provider.raw = b"not json"
                elif mode == "duplicate":
                    self.provider.raw = b'{"issuer":"one","issuer":"two"}'
                elif mode == "oversize":
                    self.provider.raw = b"x" * (adapter.MAX_BODY + 1)
                else:
                    self.provider.status = 503
                with self.assertRaises(adapter.Refused):
                    self.client.begin()
        with socket.socket() as sock:
            sock.bind(("127.0.0.1", 0))
            dead = sock.getsockname()[1]
        self.client = adapter.Adapter(
            self.config, _test_provider=f"http://127.0.0.1:{dead}"
        )
        with self.assertRaises(adapter.Blocked):
            self.client.begin()

    def test_network_timeout_is_blocked(self):
        with (
            patch.object(
                http.client.HTTPConnection, "connect", side_effect=TimeoutError()
            ),
            self.assertRaises(adapter.Blocked),
        ):
            self.client.begin()

    def test_absolute_deadline_and_malformed_http(self):
        for mode in ("slow_headers", "slow_chunk", "bad_status", "bad_chunk"):
            with self.subTest(mode=mode):
                closed = threading.Event()

                class Handler(socketserver.BaseRequestHandler):
                    def handle(self, mode=mode, closed=closed):
                        self.request.recv(8192)
                        try:
                            if mode == "bad_status":
                                self.request.sendall(b"NOT HTTP\r\n\r\n")
                            elif mode == "bad_chunk":
                                self.request.sendall(
                                    b"HTTP/1.1 200 OK\r\n"
                                    b"Content-Type: application/json\r\n"
                                    b"Transfer-Encoding: chunked\r\n\r\nNOPE\r\n"
                                )
                            else:
                                prefix = (
                                    b"HTTP/1.1 200 OK\r\n"
                                    b"Content-Type: application/json\r\n"
                                )
                                if mode == "slow_chunk":
                                    prefix += b"Transfer-Encoding: chunked\r\n\r\n"
                                self.request.sendall(prefix)
                                # Every byte is quicker than the inactivity timeout.
                                for _ in range(40):
                                    self.request.sendall(b"a")
                                    time.sleep(0.02)
                        except OSError:
                            pass
                        finally:
                            closed.set()

                with socketserver.TCPServer(("127.0.0.1", 0), Handler) as server:
                    worker = threading.Thread(target=server.handle_request, daemon=True)
                    worker.start()
                    client = adapter.Adapter(
                        self.config,
                        _test_provider=f"http://127.0.0.1:{server.server_address[1]}",
                    )
                    client._request_timeout = 0.15
                    started = time.monotonic()
                    with self.assertRaises(adapter.Blocked):
                        client.begin()
                    self.assertLess(time.monotonic() - started, 0.6)
                    self.assertTrue(closed.wait(0.6))
                    worker.join(timeout=1)

    def test_live_tls_context_certificates(self):
        now = datetime.datetime.now(datetime.UTC)
        ca_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
        ca_name = x509.Name(
            [x509.NameAttribute(NameOID.COMMON_NAME, "Synthetic test CA")]
        )
        ca = (
            x509.CertificateBuilder()
            .subject_name(ca_name)
            .issuer_name(ca_name)
            .public_key(ca_key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(now - datetime.timedelta(days=1))
            .not_valid_after(now + datetime.timedelta(days=1))
            .add_extension(
                x509.BasicConstraints(ca=True, path_length=None), critical=True
            )
            .add_extension(
                x509.SubjectKeyIdentifier.from_public_key(ca_key.public_key()), False
            )
            .add_extension(
                x509.KeyUsage(
                    False, False, False, False, False, True, True, False, False
                ),
                True,
            )
            .sign(ca_key, hashes.SHA256())
        )
        with tempfile.TemporaryDirectory() as tmp:
            folder = Path(tmp)
            trust = folder / "ca.pem"
            trust.write_bytes(ca.public_bytes(serialization.Encoding.PEM))
            invalid = folder / "invalid.pem"
            invalid.write_text("not a certificate")
            with self.assertRaises(adapter.Blocked):
                adapter.Adapter(replace(self.config, ca_bundle=str(invalid)))
            for mode in ("valid", "wrong_ca", "hostname", "expired"):
                with self.subTest(mode=mode):
                    server_key = rsa.generate_private_key(
                        public_exponent=65537, key_size=2048
                    )
                    signer = (
                        rsa.generate_private_key(public_exponent=65537, key_size=2048)
                        if mode == "wrong_ca"
                        else ca_key
                    )
                    cert = (
                        x509.CertificateBuilder()
                        .subject_name(
                            x509.Name(
                                [
                                    x509.NameAttribute(
                                        NameOID.COMMON_NAME, "Synthetic server"
                                    )
                                ]
                            )
                        )
                        .issuer_name(ca_name)
                        .public_key(server_key.public_key())
                        .serial_number(x509.random_serial_number())
                        .not_valid_before(now - datetime.timedelta(days=2))
                        .not_valid_after(
                            now
                            + datetime.timedelta(days=-1 if mode == "expired" else 1)
                        )
                        .add_extension(
                            x509.BasicConstraints(ca=False, path_length=None),
                            critical=True,
                        )
                        .add_extension(
                            x509.AuthorityKeyIdentifier.from_issuer_public_key(
                                signer.public_key()
                            ),
                            False,
                        )
                        .add_extension(
                            x509.SubjectAlternativeName(
                                [
                                    x509.DNSName(
                                        "wrong.example"
                                        if mode == "hostname"
                                        else "localhost"
                                    )
                                ]
                            ),
                            False,
                        )
                        .sign(signer, hashes.SHA256())
                    )
                    pem, key = folder / "server.pem", folder / "server.key"
                    pem.write_bytes(cert.public_bytes(serialization.Encoding.PEM))
                    key.write_bytes(
                        server_key.private_bytes(
                            serialization.Encoding.PEM,
                            serialization.PrivateFormat.PKCS8,
                            serialization.NoEncryption(),
                        )
                    )

                    class Handler(BaseHTTPRequestHandler):
                        def log_message(self, *_):
                            pass

                        def do_GET(self):
                            self.send_response(200)
                            self.send_header("Content-Type", "application/json")
                            self.end_headers()
                            self.wfile.write(b"{}")

                    with ThreadingHTTPServer(("127.0.0.1", 0), Handler) as server:
                        context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
                        context.load_cert_chain(pem, key)
                        server.socket = context.wrap_socket(
                            server.socket, server_side=True
                        )
                        worker = threading.Thread(
                            target=server.serve_forever, daemon=True
                        )
                        worker.start()
                        client = adapter.Adapter(
                            replace(self.config, ca_bundle=str(trust))
                        )
                        target = f"https://localhost:{server.server_port}/probe"
                        # Exercise the live context while isolating URL policy.
                        with patch.object(client, "_endpoint", return_value=target):
                            if mode == "valid":
                                self.assertEqual(client._json(target), {})
                            else:
                                with self.assertRaises(adapter.Blocked):
                                    client._json(target)
                        self.assertEqual(client.issuer, adapter.ISSUER)
                        server.shutdown()
                        worker.join(timeout=1)

    def test_capacity_and_expired_cleanup(self):
        self.client.capacity = 1
        self.client.begin()
        with self.assertRaises(adapter.Refused):
            self.client.begin()
        self.client.monotonic = lambda: time.monotonic() + 601
        self.client.begin()
        self.assertEqual(len(self.client.transactions), 1)

    def test_invalid_callback_configuration_and_synthetic_override(self):
        for value in (
            "http://client.example/callback",
            "https://user:pass@client.example/callback",
            "https://client.example/callback#fragment",
            "https://client.example/callback?query",
            "https://client.example/login",
        ):
            with self.assertRaises(adapter.Blocked):
                replace(self.config, redirect_uri=value).require()
        with self.assertRaises(ValueError):
            adapter.Adapter(self.config, _test_provider="https://evil.example")
        with self.assertRaises(adapter.Blocked):
            adapter.Adapter(self.config)

    def test_cli_no_prerequisites_no_network_and_sanitized_report(self):
        output = io.StringIO()
        with (
            patch.object(
                adapter.Adapter, "begin", side_effect=AssertionError("network")
            ),
            redirect_stdout(output),
        ):
            code = run_integration.main(
                [], environ={"TARA_CLIENT_SECRET": "DO-NOT-LOG"}
            )
        report = json.loads(output.getvalue())
        self.assertEqual(
            (code, report["outcome"], report["authenticated"]), (1, "BLOCKED", False)
        )
        self.assertNotIn("DO-NOT-LOG", output.getvalue())
        self.assertEqual(len(report["revision"]), 40)
        self.assertTrue(report["operator_actions"])

    def test_runner_real_http_cookie_and_callback(self):
        self.run_browser_callback(wrong_browser=False)

    def test_runner_known_state_wrong_browser(self):
        self.run_browser_callback(wrong_browser=True)

    def run_browser_callback(self, *, wrong_browser):
        with socket.socket() as reserve:
            reserve.bind(("127.0.0.1", 0))
            port = reserve.getsockname()[1]
        results = []
        thread = threading.Thread(
            target=lambda: results.append(run_integration.serve(self.client, port, 3)),
            daemon=True,
        )
        thread.start()
        connection = None
        for _ in range(100):
            try:
                connection = http.client.HTTPConnection("127.0.0.1", port, timeout=2)
                connection.request("GET", "/login")
                response = connection.getresponse()
                break
            except ConnectionRefusedError:
                time.sleep(0.01)
        else:
            self.fail("runner did not start")
        self.assertEqual(response.status, 302)
        cookie = response.getheader("Set-Cookie") or ""
        self.assertIn("Secure; HttpOnly; SameSite=Lax", cookie)
        self.assertTrue(cookie.startswith("__Host-TARA="))
        query = parse_qs(urlsplit(response.getheader("Location") or "").query)
        self.provider.nonce = query["nonce"][0]
        response.read()
        connection.close()
        connection = http.client.HTTPConnection("127.0.0.1", port, timeout=2)
        connection.request("GET", "/callback?state=stray&code=sensitive-stray")
        stray = connection.getresponse()
        self.assertEqual(stray.status, 403)
        self.assertIsNone(stray.getheader("Set-Cookie"))
        self.assertNotIn(b"sensitive-stray", stray.read())
        connection.close()
        self.assertTrue(thread.is_alive())
        before = len(self.provider.requests)
        connection = http.client.HTTPConnection("127.0.0.1", port, timeout=2)
        connection.request(
            "GET",
            "/callback?"
            + urlencode({"state": query["state"][0], "code": "secret-code"}),
            headers={
                "Cookie": "__Host-TARA=wrong" if wrong_browser else cookie.split(";")[0]
            },
        )
        response = connection.getresponse()
        self.assertEqual(response.status, 403 if wrong_browser else 200)
        self.assertIn("Max-Age=0", response.getheader("Set-Cookie") or "")
        body = response.read()
        for sensitive in (b"synthetic-subject", b"secret-code", cookie.encode()):
            self.assertNotIn(sensitive, body)
        connection.close()
        thread.join(timeout=4)
        self.assertFalse(thread.is_alive())
        self.assertEqual(
            results[0]["outcome"], "FAIL" if wrong_browser else "LOCAL_TEST"
        )
        self.assertEqual(results[0]["authenticated"], not wrong_browser)
        if wrong_browser:
            self.assertEqual(len(self.provider.requests), before)

    def test_runner_real_timeout(self):
        result = run_integration.serve(self.client, 0, 0.05)
        self.assertEqual(result["outcome"], "BLOCKED")
        self.assertFalse(result["authenticated"])

    def test_cli_terminal_semantics(self):
        env = {
            "TARA_RIA_AGREEMENT_CONFIRMED": "yes",
            "TARA_REGISTERED_TEST_CLIENT_CONFIRMED": "yes",
            "TARA_CLIENT_ID": "private-client",
            "TARA_CLIENT_SECRET": "secret-value",
            "TARA_REDIRECT_URI": "https://client.example/callback",
        }
        for outcome in ("PASS", "FAIL", "BLOCKED", "CANCELLED", "LOCAL_TEST"):
            with (
                self.subTest(outcome=outcome),
                patch.object(run_integration, "Adapter", return_value=self.client),
                patch.object(
                    run_integration,
                    "serve",
                    return_value={
                        "outcome": outcome,
                        "authenticated": outcome in ("PASS", "LOCAL_TEST"),
                        "reason": "control",
                    },
                ),
            ):
                output = io.StringIO()
                with redirect_stdout(output):
                    code = run_integration.main(
                        ["--port", "9876", "--timeout", "42"], environ=env
                    )
                report = json.loads(output.getvalue())
                self.assertEqual(
                    report["effective_options"], {"port": 9876, "timeout": 42}
                )
                self.assertIn("--port 9876 --timeout 42", report["command"])
                self.assertEqual(code, 0 if outcome == "PASS" else 1)
                self.assertNotIn("secret-value", output.getvalue())
        process = subprocess.run(  # noqa: S603 - fixed repository script with this interpreter
            [sys.executable, str(ROOT / "rp-kit/tara/run_integration.py")],
            env={"PATH": "/usr/bin"},
            capture_output=True,
            text=True,
            timeout=5,
        )
        self.assertEqual(process.returncode, 1)
        self.assertEqual(json.loads(process.stdout)["outcome"], "BLOCKED")


if __name__ == "__main__":
    unittest.main()
