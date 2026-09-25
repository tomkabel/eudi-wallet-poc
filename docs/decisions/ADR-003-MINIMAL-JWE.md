# ADR-003 — Response encryption is a hand-rolled minimal JWE, in-house

**Status:** accepted, 25 September 2026.

## Context

The OpenID4VP response carries the ~360 KB ZK proof over plain HTTPS POST
(`direct_post`). HAIP 1.0 makes response encryption mandatory, and the proof's
size makes it the right default here too: a response readable in transit is a
relying-party relationship leak, whatever TLS does, and OpenID4VP 1.0 §6.2
answers it with `response_mode=direct_post.jwt` — the vp_token inside a JWE
whose key the verifier publishes with the request itself.

The verifier is the decrypting side. Whatever JWE surface it accepts is
attacker-reachable parse surface, and this module's first third-party runtime
dependency is a policy line ADR-002 already drew for the same reason on the
ISO dcapi path.

## Decision

`internal/jose` implements exactly the profile OpenID4VP response encryption
needs, and fails closed on everything else:

- `alg=ECDH-ES+A256KW` (ephemeral-static ECDH on P-256, AES-256 Key Wrap per
  RFC 3394) and `enc=A256GCM`; compact serialization only; single recipient;
  no `zip`, no `jku`/`x5u` key sources, no unprotected header. Every other
  `alg`, `enc`, curve or key type is `ErrUnsupported`, never a fallback.
- Keys are EC P-256 only. A JWK with any other `kty`/`crv` does not parse.
- The epk in the protected header is the ephemeral *public* key; a header
  carrying `d` is rejected — an ephemeral private key on the wire is either an
  attack or an implementation bug, and both are the same refusal.
- Key wrap and content encryption come from Go's standard library
  (`crypto/ecdh`, `crypto/aes`, `crypto/cipher`); the RFC 3394 wrap loop is
  ~40 lines and pinned to its published vectors. This mirrors the `cborsub`
  precedent: a small owned surface with stated limits beats a dependency
  boundary we do not control on a hostile-input path.

Correctness is pinned to published vectors, not our own round-trips:
RFC 7638-style thumbprint over a known RFC 7520 §5.5 key, RFC 3394 §4.6 for
the wrap, and RFC 7520 §4.7 for the ECDH-ES key agreement that feeds it.

## Key lifecycle

- The verifier generates an EC P-256 response key at startup unless
  `-response-key-file` names a PEM (SEC 1 / PKIX) file, which it loads
  instead — restart-stable thumbprints, so a session's transcript stays
  verifiable across the verifier's own restarts within the session TTL.
- The public half is published twice: `GET /present/jwks.json` for clients
  that fetch keys, and inline as the authorization request's
  `client_metadata.jwks` member, so a wallet never needs a second fetch. Both
  carry the same key; `kid` is the RFC 7638 thumbprint (base64url), which
  makes the key self-identifying.
- The thumbprint goes into the B.2.6.1 handover (`jwkThumbprint`), so a proof
  is bound to *this* response key: a response encrypted to a different key
  cannot be swapped onto a captured session, and the verifier refuses a plain
  response on any session it created encrypted (`RequireEncryptedResponse`).

## Consequences

- The JWE decode path is ~400 lines we own, including its bugs. Accepted: the
  profile is one `alg`/`enc` pair, the vectors are committed, and the
  alternative (a full JOSE dependency for one profile) is the same trade
  ADR-002 rejected.
- Python-side, the wallet encrypts with `jwcrypto`. That is a deviation from
  zero-deps — accepted, Python already runs `cbor2`+`cryptography`, and
  `cryptography`'s JWE API does not exist in the installed version. The Go
  zero-dep policy is untouched.
- A session that requires encryption rejects an unencrypted response with 400
  and a named error; the plain `direct_post` mode survives only behind the
  explicit `-allow-unencrypted-response` flag, so the downgrade is a
  configuration fact, not a silent fallback.
- If a second consumer needs more JOSE surface, promote `internal/jose` to a
  versioned module — do not fork it per package.

## Alternatives rejected

- **`golang-jwt/jwt` + `lestrrat-go/jwx`**: full JOSE grammars; each accepted
  algorithm is attack surface, and both break the module's zero-dependency
  rule for one `alg`/`enc` pair.
- **`smallstep/go-jose`**: same surface problem, plus a fork lineage we would
  have to track.
- **No encryption, keep `direct_post`**: HAIP-mandatory and the ~360 KB proof
  makes responses worth protecting; keeping it as the default would leave the
  strongest link on the interop path as an option nobody enables.
- **Direct JWE (`alg=ECDH-ES`) without key wrap**: fewer moving parts, but
  A256GCM direct key agreement fixes the CEK to the curve output and the
  OpenID4VP/HAIP profile mandates `ECDH-ES+A256KW`; supporting both doubles
  the accepted surface for no interoperability gain.
