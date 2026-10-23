# ADR-004 — `rp-kit/` is a deployment profile over `zkverify`, not a second service

**Status:** accepted, 23 October 2026. Records the decision Story 3.1
(`1-3-…` backlog, FR9) deferred to this record.

## Context

Estonian public-sector services must accept other Member States' certified
wallets from the end of 2026 while the Estonian wallet launches in 2028. They
need something to run acceptance tests against — a deployable
relying-party service, not a library to assemble.

`verifier/go` already *is* that service. `zkverify` is an HTTP server with the
deployment behaviour an operator needs, all verified in code:

- SIGTERM drain: new presentations get `503` + `Retry-After: 3` on
  `/present/new`, `/healthz` answers `{"draining": true}`, and the drain
  budget defaults to 2× the session TTL (`verifier/go/main.go:583-671`,
  `tests/drain_test.sh`);
- load shedding over `-max-concurrent-verify` (default `runtime.NumCPU`,
  `main.go:397`) — shed before `Claim`, so a shed request never burns a
  session;
- `direct_post.jwt` as the response-mode default (ADR-003,
  `main.go:382-383`), plain `direct_post` only behind
  `-response-mode direct_post -allow-unencrypted-response`;
- a stable response-key lifecycle when `-response-key-file` names a persisted
  P-256 key (ADR-003, "Key lifecycle").

## Decision

`rp-kit/` is a **deployment profile**: a directory holding the documented
flag set, a systemd unit and environment template, the four-case acceptance
harness (Story 3.5) and the integrator README (Story 3.7). It builds and runs
the existing `zkverify` binary. No wrapper service, no second Go binary, no
library packaging.

**The profile flag set.** An `rp-kit/` deployment documents and uses:
`-response-mode` (left at the `direct_post.jwt` default — the profile does
not turn ADR-003's default into an option), `-response-key-file`,
`-issuers`, `-session-ttl` (default 3 m, stated), `-max-concurrent-verify`,
`-drain-timeout`, and `-dcapi-origin` (off by default). The set is stable
per release, not frozen: it grows with ADR-006's circuit-registry
configuration (`-circuit-list-url`, `-circuit-list-key`, the local registry
path) and ADR-007's register-implementation configuration, so an operator
deploying "the profile" can point at their own registry and register.
**Excluded from the profile and never presented as supported deployment
modes:** the verifier's `-allow-unencrypted-response`, `-unsafe-dev-api`
and `-multi-credentials` — negative-control and dev-harness flags — and the
*prover-side* `--allow-false-predicate` of `wallet/present.py`, which is
not a `zkverify` flag at all. The acceptance harness may drive
negative-control cases with these switches; the deployed profile never
presents them as deployment modes.

**Key management.** A profile deployment **must** supply
`-response-key-file` with an operator-managed persisted P-256 key (mode
`0600`): the JWKS thumbprint is inside the session transcript (ADR-003), so
an ephemeral key makes every restart a new key and breaks cross-restart
session semantics. The ephemeral startup-generated key stays available for
tests. Rotation is manual: file swap → drain (`SIGTERM`, budget
`-drain-timeout`) → restart; the drain path *is* the rotation procedure.

**Session closure.** A session whose callback never arrives expires after
`-session-ttl` and `GET /present/result/{id}` answers `410` — today the body
carries only `{"error": …}` (`verifier/go/present.go` `statusFor`). The
profile requires the expired/answered `410` body to **name the session id**,
so a relying party is never left without a status. This is the one code
change Story 3.1 implements beyond configuration and documentation.

**TARA sits alongside, never inside.** TARA authenticates the human user to
the e-service over OIDC; the wallet-facing OpenID4VP presentation path is a
**parallel path**, not a leg inside a TARA flow. The TARA adapter (Story 3.2)
is a separate contract; its integration test fails closed and records
*blocked* — never *passed* — when the RIA agreement or TARA test client is
absent. X-Road has no documented relationship to these protocols (Story 3.8
records the verified 0-hit result); transporting `vp_token`s or looking up
registrations over X-Road are proposed interfaces, not supported paths.

## Consequences

- One binary, one profile: carrier adaptation (ADR-constrained
  `carrier.go`), drain and shedding semantics, and ADR-003's encryption
  default are all inherited by the kit without a second implementation to
  keep green.
- The negative-control switches remain in the binary (and the prover's in
  the wallet) for the e2e suite; the profile documents them as what they
  are, so a reader cannot mistake a downgrade path for a deployment mode
  (backlog NFR1 vocabulary discipline).
- Integrators get a runnable target for Story 3.7's measured
  time-to-first-verification, and the acceptance harness (Story 3.5) runs
  against exactly what an operator would deploy.

## Alternatives rejected

- **A wrapper service around `verifier/go`:** duplicates drain, shedding,
  `/healthz` and the session store — every one a second implementation of a
  concurrency behaviour the service already pins with `-race` tests.
- **Library packaging of the verifier packages:** `verifier/go` links the
  Rust staticlib at build time (`make deps` places `../longfellow-zk` as a
  sibling — a linker trap for any integrator who just wants a service to
  call). The binary *is* the integration surface.
- **A separate RP service written from scratch:** abandons the single
  adaptation point (`carrier.go`) and splits the circuit allowlist, trust
  store and transcript pinning across two codebases.
