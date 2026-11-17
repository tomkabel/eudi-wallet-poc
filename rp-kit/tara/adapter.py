"""Bounded TARA test-client reference contract; no application session service."""

import base64
import contextlib
import hashlib
import hmac
import http.client
import json
import math
import secrets
import socket
import ssl
import threading
import time
import urllib.parse
from dataclasses import dataclass, field

from jwcrypto import jwk, jws

ISSUER = "https://tara-test.ria.ee"
LEVELS = {"low": 0, "substantial": 1, "high": 2}
MAX_BODY = 128 * 1024


class Refused(Exception):
    """A sanitized protocol refusal, never containing an input value."""


class Blocked(Refused):
    """An external prerequisite or provider connection is unavailable."""


def strict_json(raw):
    def pairs(items):
        result = {}
        for key, value in items:
            if key in result:
                raise Refused("duplicate_json_member")
            result[key] = value
        return result

    try:
        value = json.loads(
            raw,
            object_pairs_hook=pairs,
            parse_constant=lambda _: (_ for _ in ()).throw(ValueError()),
        )
        if not isinstance(value, dict):
            raise ValueError()
        return value
    except (ValueError, TypeError, UnicodeError, RecursionError) as exc:
        raise Refused("malformed_json") from exc


@dataclass(frozen=True)
class Config:
    agreement_confirmed: bool = False
    registered_client_confirmed: bool = False
    client_id: str = field(default="", repr=False)
    client_secret: str = field(default="", repr=False)
    redirect_uri: str = ""
    minimum_acr: str = "substantial"
    ca_bundle: str = ""

    def require(self):
        missing = []
        if self.agreement_confirmed is not True:
            missing.append("ria_agreement_confirmation")
        if self.registered_client_confirmed is not True:
            missing.append("registered_test_client_confirmation")
        for name in ("client_id", "client_secret", "redirect_uri"):
            if not getattr(self, name):
                missing.append(name)
        if missing:
            raise Blocked("missing_prerequisites:" + ",".join(missing))
        uri = urllib.parse.urlsplit(self.redirect_uri)
        if (
            uri.scheme != "https"
            or not uri.hostname
            or uri.username
            or uri.password
            or uri.fragment
            or uri.query
            or not uri.path
            or uri.path == "/login"
            or len(self.redirect_uri) > 2048
            or any(ord(c) <= 32 or ord(c) == 127 for c in self.redirect_uri)
        ):
            raise Blocked("invalid_registered_https_callback")
        if self.minimum_acr not in LEVELS:
            raise Blocked("invalid_minimum_assurance")
        if any(len(v) > 4096 for v in (self.client_id, self.client_secret)):
            raise Blocked("oversize_client_configuration")


class Adapter:
    """Server-side code flow. Synthetic endpoint override is test-only, never CLI."""

    def __init__(
        self,
        config,
        *,
        _test_provider=None,
        clock=time.time,
        monotonic=time.monotonic,
        ttl=300,
        capacity=64,
    ):
        if not 1 <= ttl <= 600 or not 1 <= capacity <= 64:
            raise ValueError("invalid_transaction_bounds")
        self.config = config
        self.issuer = ISSUER
        self.local_test = _test_provider is not None
        if self.local_test:
            uri = urllib.parse.urlsplit(_test_provider)
            if uri.scheme != "http" or uri.hostname != "127.0.0.1" or uri.path:
                raise ValueError("test_provider_must_be_loopback_http")
            self.issuer = _test_provider
        self.clock, self.monotonic = clock, monotonic
        self.ttl, self.capacity = ttl, capacity
        self.transactions = {}
        # Live operators supply the deliberately restricted roots from TARA §5.1.2.
        # Never quietly expand that trust boundary to the full host CA store.
        if not self.local_test and not config.ca_bundle:
            raise Blocked("missing_prerequisites:restricted_tara_ca_bundle")
        try:
            context = ssl.create_default_context(cafile=config.ca_bundle or None)
        except (OSError, ssl.SSLError) as exc:
            raise Blocked("invalid_tara_ca_bundle") from exc
        self._context = context
        self._request_timeout = 10
        self._network_slot = threading.BoundedSemaphore(1)

    def _endpoint(self, url):
        if not isinstance(url, str) or len(url) > 2048:
            raise Refused("invalid_provider_endpoint")
        uri, issuer = urllib.parse.urlsplit(url), urllib.parse.urlsplit(self.issuer)
        if (
            uri.scheme != issuer.scheme
            or uri.netloc != issuer.netloc
            or uri.username
            or uri.password
            or uri.fragment
            or uri.query
            or not uri.path
            or any(ord(c) <= 32 for c in url)
        ):
            raise Refused("untrusted_provider_endpoint")
        return url

    def _json(self, url, *, data=None, headers=None):
        self._endpoint(url)
        if not self._network_slot.acquire(blocking=False):
            raise Blocked("provider_request_busy")
        uri = urllib.parse.urlsplit(url)
        if uri.scheme == "https":
            connection = http.client.HTTPSConnection(
                uri.hostname,
                uri.port,
                timeout=min(3, self._request_timeout),
                context=self._context,
            )
        else:
            connection = http.client.HTTPConnection(
                uri.hostname, uri.port, timeout=min(3, self._request_timeout)
            )
        finished, expired = threading.Event(), threading.Event()
        result = []
        transport = []

        def request():
            try:
                connection.connect()
                transport.append(connection.sock)
                if expired.is_set():
                    return
                connection.request(
                    "POST" if data is not None else "GET",
                    uri.path,
                    body=data,
                    headers=headers or {},
                )
                with connection.getresponse() as response:
                    if response.status != 200:
                        raise Blocked("provider_http_failure")
                    if response.headers.get_content_type() != "application/json":
                        raise Refused("provider_content_type")
                    raw = response.read(MAX_BODY + 1)
                    if len(raw) > MAX_BODY:
                        raise Refused("provider_body_limit")
                    result.append(strict_json(raw))
            except (http.client.HTTPException, OSError, ValueError):
                result.append(Blocked("provider_unavailable"))
            except Refused as exc:
                result.append(exc)
            finally:
                connection.close()
                self._network_slot.release()
                finished.set()

        threading.Thread(target=request, daemon=True).start()
        if not finished.wait(self._request_timeout):
            expired.set()
            # shutdown interrupts buffered reads of headers and chunk framing.
            if transport and transport[0] is not None:
                with contextlib.suppress(OSError):
                    transport[0].shutdown(socket.SHUT_RDWR)
            raise Blocked("provider_timeout")
        if not result:
            raise Blocked("provider_unavailable")
        if isinstance(result[0], Exception):
            raise result[0]
        return result[0]

    def _discovery(self):
        document = self._json(self.issuer + "/.well-known/openid-configuration")
        if document.get("issuer") != self.issuer:
            raise Refused("discovery_issuer")
        for name in ("authorization_endpoint", "token_endpoint", "jwks_uri"):
            self._endpoint(document.get(name))
        algorithms = document.get("id_token_signing_alg_values_supported")
        if (
            not isinstance(algorithms, list)
            or any(not isinstance(value, str) for value in algorithms)
            or "RS256" not in algorithms
        ):
            raise Refused("discovery_algorithm")
        return document

    def begin(self):
        self.config.require()  # Gate each new flow before any network request.
        now = self.monotonic()
        self.transactions = {s: t for s, t in self.transactions.items() if t[0] > now}
        if len(self.transactions) >= self.capacity:
            raise Refused("transaction_capacity")
        document = self._discovery()
        state, nonce, browser = (secrets.token_urlsafe(32) for _ in range(3))
        self.transactions[state] = (
            now + self.ttl,
            hashlib.sha256(browser.encode()).digest(),
            nonce,
            document,
            self.config,
        )
        query = urllib.parse.urlencode(
            {
                "response_type": "code",
                "scope": "openid",
                "client_id": self.config.client_id,
                "redirect_uri": self.config.redirect_uri,
                "state": state,
                "nonce": nonce,
                "acr_values": self.config.minimum_acr,
            }
        )
        return document["authorization_endpoint"] + "?" + query, browser

    def callback(self, query, browser):
        self.config.require()  # Gate callbacks too, including cancellation.
        if len(query) > 8192 or not isinstance(browser, str) or len(browser) > 128:
            raise Refused("callback_limit")
        try:
            items = urllib.parse.parse_qs(
                query, keep_blank_values=True, strict_parsing=True, max_num_fields=8
            )
        except ValueError as exc:
            raise Refused("malformed_callback") from exc
        if any(len(v) != 1 for v in items.values()):
            raise Refused("duplicate_callback_parameter")
        state = items.get("state", [""])[0]
        transaction = self.transactions.pop(
            state, None
        )  # Burn on any claimed callback.
        if transaction is None:
            raise Refused("unknown_or_replayed_state")
        expiry, binding, nonce, document, initiating_config = transaction
        if self.config != initiating_config:
            raise Refused("transaction_configuration_changed")
        if expiry <= self.monotonic():
            raise Refused("expired_transaction")
        if not hmac.compare_digest(binding, hashlib.sha256(browser.encode()).digest()):
            raise Refused("browser_binding")
        if "error" in items:
            if (
                set(items) & {"code", "id_token", "access_token", "token_type"}
                or items["error"][0] != "access_denied"
            ):
                raise Refused("provider_authentication_error")
            return {"outcome": "CANCELLED", "authenticated": False}
        code = items.get("code", [""])[0]
        if not code or len(code) > 4096:
            raise Refused("missing_or_invalid_code")
        body = urllib.parse.urlencode(
            {
                "grant_type": "authorization_code",
                "code": code,
                "redirect_uri": self.config.redirect_uri,
            }
        ).encode()
        # RFC 6749: form-encode each Basic component before joining with colon.
        credentials = ":".join(
            urllib.parse.quote_plus(v)
            for v in (self.config.client_id, self.config.client_secret)
        )
        token = self._json(
            document["token_endpoint"],
            data=body,
            headers={
                "Content-Type": "application/x-www-form-urlencoded",
                "Authorization": "Basic "
                + base64.b64encode(credentials.encode()).decode(),
            },
        )
        if (
            "error" in token
            or not isinstance(token.get("access_token"), str)
            or not token["access_token"]
            or not isinstance(token.get("token_type"), str)
            or token["token_type"].lower() != "bearer"
        ):
            raise Refused("invalid_token_response")
        compact = token.get("id_token")
        if not isinstance(compact, str) or len(compact) > 32768:
            raise Refused("missing_or_invalid_id_token")
        claims = self._verify(compact, nonce, document["jwks_uri"])
        # Only the embedding e-service may use this identity to create its own session.
        return {
            "outcome": "LOCAL_TEST" if self.local_test else "PASS",
            "authenticated": True,
            "claims": claims,
        }

    def _verify(self, compact, nonce, jwks_uri):
        try:
            parts = compact.split(".")
            if len(parts) != 3:
                raise Refused("token_encoding")
            strict_json(base64.urlsafe_b64decode(parts[0] + "=" * (-len(parts[0]) % 4)))
            signed = jws.JWS()
            signed.deserialize(compact)
            header = signed.jose_header
            if (
                header.get("alg") != "RS256"
                or not isinstance(header.get("kid"), str)
                or not header["kid"]
                or set(header) - {"alg", "kid", "typ"}
            ):
                raise Refused("token_header")
            keys = self._json(jwks_uri).get("keys")
            if not isinstance(keys, list) or len(keys) > 32:
                raise Refused("invalid_jwks")
            matches = [
                k for k in keys if isinstance(k, dict) and k.get("kid") == header["kid"]
            ]
            if len(matches) != 1:
                raise Refused("unknown_or_ambiguous_kid")
            key = matches[0]
            if (
                key.get("kty") != "RSA"
                or key.get("alg", "RS256") != "RS256"
                or key.get("use", "sig") != "sig"
                or "d" in key
                or key.get("key_ops", ["verify"]) != ["verify"]
            ):
                raise Refused("invalid_signing_key")
            public = jwk.JWK(**key)
            if public.get_op_key("verify").key_size < 2048:
                raise Refused("weak_signing_key")
            signed.verify(public, alg="RS256")
            claims = strict_json(signed.payload)
        except Refused:
            raise
        except Exception as exc:
            raise Refused("invalid_token_signature_or_encoding") from exc
        if claims.get("iss") != self.issuer:
            raise Refused("token_issuer")
        audience = claims.get("aud")
        if isinstance(audience, str):
            audience = [audience]
        if (
            not isinstance(audience, list)
            or not audience
            or any(not isinstance(a, str) for a in audience)
            or self.config.client_id not in audience
        ):
            raise Refused("token_audience")
        azp = claims.get("azp")
        if (len(audience) > 1 and azp != self.config.client_id) or (
            azp is not None and azp != self.config.client_id
        ):
            raise Refused("token_authorized_party")
        now = self.clock()
        for name in ("iat", "exp"):
            value = claims.get(name)
            if type(value) not in (int, float) or (
                isinstance(value, float) and not math.isfinite(value)
            ):
                raise Refused("token_time_type")
        if (
            claims["iat"] > now + 5
            or claims["exp"] <= now
            or claims["exp"] <= claims["iat"]
        ):
            raise Refused("token_time")
        if "nbf" in claims and (
            type(claims["nbf"]) not in (int, float)
            or (isinstance(claims["nbf"], float) and not math.isfinite(claims["nbf"]))
            or claims["nbf"] > now + 5
        ):
            raise Refused("token_not_before")
        if claims.get("nonce") != nonce:
            raise Refused("token_nonce")
        if not isinstance(claims.get("sub"), str) or not claims["sub"]:
            raise Refused("token_subject")
        if (
            not isinstance(claims.get("acr"), str)
            or claims.get("acr") not in LEVELS
            or LEVELS[claims["acr"]] < LEVELS[self.config.minimum_acr]
        ):
            raise Refused("token_assurance")
        return claims
