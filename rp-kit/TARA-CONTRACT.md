# TARA-facing adapter contract

This reference demonstrates the server-side TARA **test** authentication leg.
It is not a deployable authentication-service profile or production approval.
ADR-004 keeps it alongside the existing [wallet verifier](README.md): an e-service
may offer TARA login and wallet presentation as parallel choices. An age predicate
is not identity, and a wallet presentation is never transported inside TARA.

## Interface and trust boundary

After installing the repository's locked dependencies with make py-deps,
Adapter(Config(...)).begin() returns an authorization URL and a fresh browser
binding. Keep the binding in an HTTPS, host-only, Secure, HttpOnly,
SameSite=Lax cookie. The runner uses __Host-TARA with Path=/ and a five-minute
Max-Age; it clears that cookie after completion. The state, nonce, binding digest
and discovery endpoints stay server-side in a single-use transaction. A callback
must arrive from that initiating browser. A state claim burns the transaction
before token exchange, including failed binding and provider cancellation.
Expired transactions cannot authenticate; restarts discard pending transactions.

Adapter.callback(query, browser_binding) returns authenticated=true only after
verified code exchange, with outcome PASS against the pinned real test issuer or
LOCAL_TEST against an explicit loopback-only test override. The validated claims
remain in memory for an embedding e-service; the runner discards them. The
application owns session creation, subject mapping, authorization and logout.
No session or personal identifiers enter runner reports. A valid bound
access_denied yields CANCELLED and authenticated=false. All refusals deny login.
Token success responses require nonempty access_token, Bearer token_type and
id_token, and cannot contain an error. Security configuration is bound to the
initiating transaction; changing it refuses the callback before exchange.
Malformed authentication yields FAIL; unavailable prerequisites, HTTP errors and
network failures yield BLOCKED; unexpected runner faults yield ERROR. Every
non-PASS terminal CLI outcome exits 1. LOCAL_TEST never exits as live success.

The only live issuer is https://tara-test.ria.ee. There is no CLI or environment
issuer override. Discovery must repeat that exact issuer. Authorization, token
and JWKS endpoints must use the same origin, with no userinfo, query or fragment.
HTTP redirects are refused, including credential POST redirects. The client
uses client_secret_basic, form-encoding each credential before Basic encoding;
the token request repeats the exact configured registered callback. Only openid
scope is requested; configurable minimum assurance defaults to substantial.
No selective-method scope is requested, so no amr method restriction is asserted.

RS256 signatures are verified with the locked jwcrypto/cryptography stack using
one unambiguous kid-selected RSA key of at least 2048 bits from fresh JWKS.
Unexpected JOSE header extensions are refused. Checks cover discovery/token
issuer, audience, azp (mandatory for multiple audiences), nonce, nonempty subject,
finite numeric iat/exp, iat at most five seconds ahead, strictly unexpired exp,
optional nbf and minimum acr. Low/substantial/high are compared in order. No
userinfo call, ES256 verifier modification or identity inference from age occurs.

Resources: 64 pending transactions, five-minute TTL (library bounds 1–600 s),
128 KiB provider bodies, 32 JWKS keys, 32 KiB ID token, 8 KiB callback, eight
callback parameters, three-second socket inactivity timeout and a ten-second
absolute connect/header/body deadline. A timed-out transport is shut down and
closed; malformed HTTP also yields sanitized BLOCKED. One network worker per
adapter bounds pending connection/DNS work; DNS completion may outlive caller
expiry, but cannot send an expired request.
The synchronous runner binds loopback only, caps a run at 600 seconds (default
300), and accepts no POST callback. Read headers are bounded by Python's HTTP
parser and a two-second absolute request-header deadline. A provider operation
may finish within its bounded timeout after the overall browser deadline.
The reverse proxy must enforce its own connection/header/body limits. This
small single-process runner is a demonstration, not an internet service server.

## Operator setup and actual integration

1. Obtain institutional authorization and apply to RIA for TARA demo access,
   consent to the applicable SLA, and register your test application. Receive
   client_id and client_secret through the approved institutional channel.
2. Register the exact HTTPS callback, for example
   https://your-controlled-host.example/tara/callback. This narrow runner requires
   a nonempty path without query/fragment, userinfo or /login. Copy the registered
   value exactly; the adapter does not derive it from Host/Forwarded headers.
3. Configure a trusted HTTPS reverse proxy on that same host. Forward GET /login
   and the registered callback path unchanged to 127.0.0.1:8765. The proxy must
   preserve Cookie and Set-Cookie, preserve the callback query for processing,
   enforce HTTPS and avoid logging callback query strings. Disable proxy/browser
   debugging capture of codes, tokens, personal attributes and credentials.
4. Store credentials in the server's protected environment/secret manager (never
   in arguments, committed files, shell history or the browser). Configure
   TARA_CLIENT_ID, TARA_CLIENT_SECRET and TARA_REDIRECT_URI. Set TARA_CA_BUNDLE
   to a PEM file containing only current TARA-approved root certificates from
   authenticated CA distribution channels. Set both
   TARA_RIA_AGREEMENT_CONFIRMED=yes and
   TARA_REGISTERED_TEST_CLIENT_CONFIRMED=yes only after confirming them. These are
   operator attestations; the adapter cannot verify the agreement online. Every
   begin and callback rechecks the configuration and attestations.
5. Run make test-tara-integration and open the HTTPS /login URL printed on stderr
   in a real browser. Authenticate using the approved TARA test process. The
   runner waits for the callback and prints one sanitized JSON report on stdout.
   Direct invocation permits --port 8765 --timeout 300; keep the proxy aligned.
   Reports include the sanitized effective command, port and timeout.
6. Retain the report and exit status with the exact checked-out revision. PASS
   requires complete real browser authentication and validated code exchange.
   Discovery connectivity alone and offline test success cannot establish PASS.

No registered client or institutional agreement was available for the retained
[fresh post-review attempt](tara/evidence/attempt-2026-11-12-review.json), alongside
the unchanged historical attempt. It actually invoked
the live runner, stopped before network or login because prerequisites were
absent and exited nonzero with BLOCKED. External authentication is outstanding.
Do not replace this evidence with a synthetic provider result.

## Certificates and operational responsibilities

Python's TLS context verifies chains, hostname and validity using the required
operator-supplied TARA_CA_BUNDLE; verification cannot be disabled. Missing or
invalid bundles block live startup. Do not use the full operating-system CA
store: §5.1.2 advises against it. The checked root list is DigiCert Global Root G2,
GlobalSign R4, GTS Root R1/R2/R3/R4 and ISRG Root X1/X2. Obtain authentic PEMs
and track changes: the adapter cannot infer whether an arbitrary PEM contains
only the prescribed roots. Use maintained browser-trusted HTTPS certificates for
the callback proxy. TARA specification §5.1.2 describes chain,
hostname, validity, certificate constraints and revocation responsibilities;
§5.1.2.1 describes TLS 1.2/1.3, SNI and cipher interoperability. Operators must
check that the deployment's trust store meets that current root policy, maintain
clocks, protect/rotate credentials, subscribe to RIA key/certificate notices and
respond to revocation notices or implement appropriate OCSP/CRL monitoring.
Python's default context does not automatically implement online revocation
monitoring. This reference does not claim operational certificate compliance.
Fresh JWKS per exchange supports signing-key rotation without indefinite cache.

## Primary-source evidence and limits

Checked on 2026-11-12: the public TARA TechnicalSpecification page self-labels
v1.21, 03.09.2025. Sections 4/5 describe code flow, Basic encoding, state/session
binding, nonce, RS256/JWKS, issuer, audience, time and assurance checks; §7 requires
an institutional application, SLA consent and registered HTTPS callback and
explains credential issuance; §11 names the demo issuer. The public Liitumine
page requires an application through RIA forms. Source URLs for reproduction:

- https://e-gov.github.io/TARA-Doku/TechnicalSpecification
- https://e-gov.github.io/TARA-Doku/Liitumine
- https://github.com/e-gov/TARA-Doku

The public source repository HEAD resolved to
10b0aa8c1cc74fbe5d0a408a56f3052ef405d2c0 during this check; this is not proof of the
GitHub Pages deployment revision. The RIA joining destination linked by that
page returned HTTP error in this session; its current agreement wording remains
[UNVERIFIED]. Obtain current terms directly from RIA before runtime attestation.
The older TARA specification §5.3 suggests extensive token/URL logging; this
bounded reference intentionally emits only sanitized outcomes as required by
this story and does not implement that sensitive logging recommendation.
