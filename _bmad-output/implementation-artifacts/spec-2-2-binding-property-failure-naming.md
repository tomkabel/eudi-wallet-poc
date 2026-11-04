---
title: '2.2 Binding-property failure naming'
type: 'feature'
created: '2026-10-23'
status: 'ready-for-dev'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '9d9f12af4056bf97f0129460d57398b51d89e555'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/stories/2-2-binding-property-failure-naming.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-2-1-carrier-runner-over-existing-cases.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** A rejected presentation can leave an integrator guessing which verifier binding failed. Named failures must stay separate from missing presentation, replay, and decryption outcomes.

**Approach:** Give the verifier and Story 2.1 runner stable, non-leaky reasons for checks they can identify. Keep session values authoritative; never infer a transcript input from a generic proof failure.

**Boundaries & Constraints**

**Always:** Keep transcript inputs verifier-owned; do not return credentials, keys, proofs, or runtime details. Distinguish `no-presentation`, `single-use`, predicate refusal, JWE decryption, and transcript binding. Preserve Story 2.1's layered report and inventory.

**Never:** Treat wallet-asserted binding values as authoritative; convert an unclassified failure into a guessed property; report a no-presentation or decryption refusal as a binding failure.

**Decision:** For indistinguishable transcript mismatches, the runtime reports the neutral `transcript-binding` reason. Controlled tests that deliberately change exactly one input may name that input in the test result. Do not change the response protocol to add untrusted binding claims.

**I/O & Edge-Case Matrix**

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Single-use replay | A response is posted after the session was answered | Stable `single-use` refusal | Preserve HTTP 410; do not reprocess the token |
| Predicate mismatch | Presentation discloses a value outside the query | Stable `predicate-value` refusal | Do not call it transcript binding |
| Empty proof response | A well-formed CBOR response contains no provable presentation | `no-presentation`, refused | Keep separate from parse and binding errors |
| Encrypted response failure | JWE cannot be authenticated/decrypted | `decryption` failure | Stop before presentation parsing |
| Transcript mismatch | A valid proof was made for a different bound transcript | `transcript-binding` | Controlled tests may name the one input they deliberately changed |

</frozen-after-approval>

## Code Map

- `verifier/go/oid4vp/transcript.go` -- `SessionTranscript` hashes client ID, nonce, thumbprint and response URI together; response carries no individual values.
- `verifier/go/oid4vp/session.go` -- `Store.Claim` and `ErrAlreadyUsed` implement single-use sessions; `Session.Transcript` uses stored verifier values.
- `verifier/go/oid4vp/carrier.go` -- `ErrNoZkDocument`, `matchesQueryValue`, `checkDisclosedValue` and carrier parser refusals already separate no-presentation and predicate failures.
- `verifier/go/present.go` -- decryption precedes `Claim`; `check`/`checkZk` parse; `verifyPlain` and `verifyZkPresentation` map proof failures; `handleResult` exposes `detail`.
- `verifier/go/main.go` -- `verifyErrorCategory` keeps runtime failures stable and non-leaky; reuse this approach for public diagnostics.
- `verifier/go/carrier_flag_test.go`, `verifier/go/step8_7_openid4vp_zk_test.go`, `verifier/go/oid4vp/transcript_test.go`, `verifier/go/oid4vp/vptoken_test.go` -- existing HTTP, replay, transcript and predicate controls.
- `tests/run_carrier_conformance.py` and `tests/e2e.sh` -- Story 2.1 report vocabulary and machine-readable case outcomes; update additively.

## Tasks & Acceptance

**Execution:**
- [ ] `verifier/go/oid4vp` -- define stable typed reasons for deterministic binding checks without exposing response data.
- [ ] `verifier/go/present.go` -- map replay, predicate, no-presentation, decryption and transcript outcomes at response and result surfaces.
- [ ] `tests/e2e.sh`, `tests/run_carrier_conformance.py`, focused Go/Python tests -- assert named outcomes; prevent malformed, no-presentation, decryption and replay controls from being mislabeled.
- [ ] `docs/profiles/CARRIER-FAILURE-REASONS.md`, `tests/README.md`, and `docs/README.md` -- document and index the stable reason vocabulary and its diagnostic limits.

**Acceptance Criteria:**
- Given a binding failure the verifier can directly identify, when evaluated, then its stable reason names the property and avoids generic `invalid` wording.
- Given a transcript proof failure whose changed component cannot be known, when reported, then the runtime reason is `transcript-binding`; controlled tests may name the input they deliberately changed.
- Given no provable presentation, replay, or undecryptable JWE, when evaluated, then outcomes remain `no-presentation`, `single-use`, or `decryption`, distinct from binding failure.
- Given local synthetic controls only, when the documentation describes the reasons, then it does not claim independent implementation behavior.

## Implementation Notes

Decision recorded: use `transcript-binding` at runtime for indistinguishable transcript mismatches. Tests that deliberately mutate one transcript input may report that property. No response protocol metadata is added.

## Spec Change Log

## Review Triage Log

## Design Notes

Name only directly observed failures. Transcript inputs are hashed together; proof errors cannot identify the changed input.

## Verification

**Commands:**
- `go test ./oid4vp/...` -- expected: named-error identity, no-presentation, predicate, and transcript mismatch controls pass.
- `go test . -run 'Test.*(Response|Replay|Binding|Predicate)'` -- expected: HTTP handler keeps replay, decryption, and binding response details distinct.
- `python3 -m unittest discover -s tests -p 'test_carrier_conformance.py'` -- expected: runner accepts only the approved per-case reason vocabulary and preserves no-presentation/decryption outcomes.
- `git diff --check` -- expected: no whitespace errors.
