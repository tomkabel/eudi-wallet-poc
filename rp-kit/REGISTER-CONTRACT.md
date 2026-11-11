# RP-register contract — local proposal, version 1

This implements ADR-007 and ADR-005. It proposes a local contract seam; it does
not claim that RIA or any other authority operates this API. Synthetic mock
success proves only local behavior. It adds no wallet/certificate parser.

## Go interface and identity

`verifier/go/rpregister` exposes:

```go
type RPRegister interface {
    IntendedUse(RegisterIdentity) (RegisteredIntendedUse, error)
}
```

`RegisterIdentity` pairs `uniqueIdentifier` and `serviceIdentifier` exactly.
The entity comes from server `-client-id`, service from `-rp-register-service`
(default `age-verification`); a stored session's ClientID must still match the
server identity. No identity is taken from holder input.

`RegisteredIntendedUse` has exactly six JSON fields: `tradeName` (nonempty),
`uniqueIdentifier`, `serviceIdentifier`, `memberState` (uppercase two-letter
code), `intendedUses` (nonempty array of unique `{doctype,predicate}` pairs),
and `privacyPolicyUrl` (HTTPS URL or `null`; blank refuses). Predicates are the
closed `age_over_NN` vocabulary. Entity and service must match the lookup.
Duplicate keys at every depth, case variants, unknown fields, trailing JSON,
malformed/empty records and duplicate pairs refuse. The HTTP/file document cap is 1 MiB. Each requested credential and
claim must be authorized: supported mdoc format, explicit doctype, nonempty
claims, exactly `[doctype, age_over_NN]` path, and a matching registered pair.
The namespace equals the doctype in this verifier's credential model.

## Failure and session policy

- `ErrNoImplementation`: missing/nil implementation, HTTP 503, log reason
  `missing-register-prerequisite`; health remains available.
- `ErrRegisterUnreachable`: configured connection/DNS/timeout/non-200 service
  error (except 404), HTTP 503, log reason `register-error`.
- `ErrNotFound`: no exact identity, HTTP 403, `register-not-found`.
- `ErrInvalid`: malformed/identity-mismatched registration, HTTP 403,
  `register-invalid`; `ErrUnauthorized`: disallowed query, HTTP 403,
  `register-unauthorized`.

Redirect and DCAPI creation, authorization/device request release, DCAPI page,
and callbacks enforce the policy. Release and callbacks reevaluate the stored
session ClientID/query; a later expiry/change can refuse. Denial releases no
request material, creates no session, claims no callback session and never
makes a new `valid=true` decision. Result polling remains available. Existing
encryption, drain, admission-before-Claim and single-use semantics remain.
Logs contain stable reasons and cache decisions, never holder attributes,
response tokens, keys, query bodies or provider error text.

## Cache and resource bounds

The published defaults (`zkverify.env.example` and startup log) are:
`-rp-register-cache-ttl 15m`, `-rp-register-cache-capacity 256`,
`-rp-register-timeout 2s`. TTL and capacity must be positive. A validated positive
entry with age **strictly less than** TTL may allow without contacting the
provider, including during an outage. Log `cache-hit-within-ttl`, age, TTL,
`provenance=bounded-positive-cache` and `network=not-contacted`; this is bounded
staleness, never a live-lookup claim. Cache reads do not refresh age. At the
exact TTL boundary or later, discard, log `stale-discard-relookup` and relookup;
provider failure/absence refuses with no stale fallback. Errors are never
cached. Capacity eviction removes the oldest inserted positive record and
logs `capacity-eviction`. Entries and returned records are defensive copies.
A short mutex protects cache entries. Nonblocking admission permits one provider
lookup per cache instance; excess cold lookups immediately refuse with
`ErrRegisterUnreachable` instead of queuing. Fresh cache hits remain accessible
while that lookup is running. HTTP has at most eight connections per host;
the client absolute timeout covers connect, headers and body. The cache stores
at most capacity entries, with no per-key synchronization objects retained.

## Local adapters and reproduction

`-rp-register-file "$PWD/rp-kit/register/synthetic-v1.json"` explicitly loads the
[shared synthetic corpus](register/README.md); `-rp-register-url` instead chooses
the proposed HTTP adapter. They are mutually exclusive and default empty.
Remote HTTP endpoints require HTTPS with normal Go certificate verification;
plain HTTP is allowed only for literal loopback IP hosts, for local controls.
URL credentials and fragments refuse. Redirects are never followed. The
adapter sends GET to the configured URL with `uniqueIdentifier` and
`serviceIdentifier` query parameters. HTTP 200 returns one six-field object,
404 means absent, other status codes refuse as configured service failure.

From the repository root after `make py-deps` and `make deps`:

```sh
make test-go-fast
make test-go-zk
make test-rp-kit
make e2e
```

The standalone race tests execute fresh/boundary/stale/capacity/copy/concurrency
controls. Linked HTTP-handler tests execute both flows, registration refusal,
connection/timeout/5xx failures and stored-session rechecks. Process acceptance
stages the actual systemd unit/environment, proves its default refusal, then
explicitly opts into synthetic registration. The RP README contains a complete
mock startup command with persisted response-key and encrypted defaults.
