---
title: '2.2 Binding-property failure naming'
type: 'feature'
created: '2026-11-11'
status: 'done'
baseline_commit: '8064c846b7cee675c057d582849cf14a8903e8d7'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The carrier runner does not name every binding property required by Story 2.2 or report an empty provable presentation as its own refusal. Integrators need actionable diagnoses instead of generic rejection text.

**Approach:** Extend the live negative controls and reporting inventory to name client_id, nonce, response_uri, jwk_thumbprint, single-use, and predicate-value. Give deterministic verifier refusals stable names while retaining separate carrier, decryption, and no-presentation outcomes.

## Boundaries & Constraints

**Always:** Preserve the fifteen existing E2E assertions, both committed carrier vectors, verifier-owned session inputs, encryption defaults, HTTP status semantics and fail-closed reporting. Retain errors.Is identities and actual assertion results.

**Never:** Add holder-asserted binding metadata, regenerate golden vectors, guess an individual transcript input from an opaque proof failure, expose proofs/keys/runtime internals, or claim independent conformance. No deployment, publication or push.

**Retained decision:** The archived approved Story 2.2 spec uses neutral transcript-binding at runtime for indistinguishable proof failures. Only controlled tests deliberately changing one input may name that component. The runtime category does not prove which input caused an arbitrary rejection.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Transcript control | Exactly one of client ID, nonce, response URI or thumbprint changed | Runner names that property; verifier refuses | Runtime says transcript-binding without guessing the component |
| Replay | Response posted after session already answered | single-use refusal | Preserve HTTP 410; do not re-verify |
| False predicate | Disclosed boolean violates query | predicate-value refusal | Preserve the check before proof work |
| Empty presentation | Well-formed CBOR contains no provable presentation | no-presentation, refused | Distinct from malformed carrier and binding failure |
| Broken encryption | JWE authentication/decryption fails | decryption failure | Stop before presentation parsing |
| Missing evidence | Child omits or mislabels a case | ERROR or NOT_EXECUTED | Never infer passing evidence from the expected label |

</frozen-after-approval>

## Code Map

- `wallet/present.py`: tamper flag, transcript construction, original response destination and JWE key.
- `verifier/go/oid4vp/carrier.go`, `vptoken.go`, `session.go`: ErrNoZkDocument, predicate checks and ErrAlreadyUsed provide existing refusal seams.
- `verifier/go/present.go`: response/check/verification handlers and persisted polling detail.
- `tests/e2e.sh`: numbered assertions and case_record; append without renumbering.
- `tests/run_carrier_conformance.py`: EXPECTED_CASE_METADATA, validate_case_records, print_summary; preserve subprocess bounds and observed carriers.
- `verifier/go/carrier_flag_test.go`, `step8_7_openid4vp_zk_test.go`, `oid4vp/*test.go`: reusable HTTP/parser harnesses and fixture-backed proof controls.
- `docs/profiles/CARRIER-CONFORMANCE-RUNNER.md`, `tests/README.md`, `docs/README.md`: reporting contract, reproducible commands and document index.

## Tasks & Acceptance

**Execution:**
- [x] `verifier/go/oid4vp/carrier.go`, `vptoken.go`, `session.go` — name deterministic predicate, empty-presentation and single-use refusals without changing acceptance rules.
- [x] `verifier/go/present.go` — expose neutral transcript-binding and deterministic refusal names consistently at response/result surfaces; preserve separate decryption and multi-credential semantics.
- [x] `wallet/present.py` — add controlled client ID, response URI and thumbprint tampering options alongside nonce tampering; keep the real post destination and JWE key unchanged.
- [x] `tests/e2e.sh` — strengthen nonce/replay/predicate/decryption assertions; append three transcript controls and a well-formed empty-CBOR refusal.
- [x] `tests/run_carrier_conformance.py` — expose and validate property labels per deliberate case and retain them when assertions fail; add no-presentation as its own outcome.
- [x] `tests/test_carrier_conformance.py`, focused Go tests and wallet controls — test matrix edges, missing/wrong/generic property labels, independent mutations, response/polling consistency and no proof work on deterministic refusals.
- [x] `docs/profiles/CARRIER-FAILURE-REASONS.md`, runner documentation, test README and document index — document names, controlled attribution, runtime limits and local evidence scope.

**Acceptance Criteria:**
- Given any of the six deliberate binding violations, when the runner executes, then its report and summary name the property and preserve the actual assertion result.
- Given an opaque proof rejection, when the runtime reports it, then transcript-binding replaces generic invalid wording without claiming to identify a component.
- Given no presentation, undecryptable JWE, unknown bytes or malformed envelope, when evaluated, then each retains its own outcome separate from binding failures.
- Given omitted or incorrect property evidence, when validated, then the report cannot pass.

## Implementation Notes

Planning baseline: 8064c846b7cee675c057d582849cf14a8903e8d7. Continuity: archived Story 2.1 code map/tasks/review and approved Story 2.2 diagnostic decision. No intent gaps or irreversible operations. Dispatch route reflects the cross-layer footprint.

Verification: make check and make test-go-zk passed. E2E 18/19 with a transport reset on case 17; separate real encrypted response_uri retry passed (HTTP 200, valid:false, transcript-binding). Full runner green remains pending. Evidence: /tmp/story22-check.log, /tmp/story22-zk.log, /tmp/story22-e2e-resource-failure.log, /tmp/story22-response-uri.log.

## Spec Change Log

Completion evidence (2026-11-11): final make check exited 0 (/tmp/story22-check-final.log), including 20 runner regressions and wallet transcript/mutation controls; final make test-go-zk exited 0 (/tmp/story22-zk-final.log, root package 133.090s); final make e2e exited 0 (/tmp/story22-e2e-final.log), all 19 ordered assertions and both committed carrier vectors PASS, with no inventory problems. This supersedes the earlier integrated transport-reset failure without erasing it. git diff --check passed; no demo-data or golden vector files changed.

Matrix audit: transcript inputs covered by live cases2/16/17/18 and wallet independent-mutation controls; replay by live case3 and TestSuccessfulResponseReplayPreservesCompletedResult; predicate by live case4 and typed-error/native multi controls; empty response by live case19, TestBindingFailureNamesAndPolling and TestNativeZKFailureNamesAndPolling; decryption by live case9 and TestDecryptionFailureDoesNotParseOrVerify; missing evidence by the 20 executed runner regression tests. TestMultiLaterDeterministicRefusalBeforeProof covers empty/predicate errors on a later credential without any proof calls. All covering suites ran and passed.

Final review fixes are tests/assertions only: native ZK empty/proof diagnostics, later-credential refusal, replay polling invariance, claimed-PASS metadata mutations, and exact E2E HTTP statuses. Final make check passed (/tmp/story22-check-final.log); final make test-go-zk passed (133.090s, /tmp/story22-zk-final.log). Full post-review E2E is executing with unchanged acceptance rules.

## Review Triage Log

- Edge 1 — medium, patch: case19 accepted 201 for both endpoints; now checks explicit create201/refusal400.
- Blind 1 — medium, defer: metadata failures discard other observed fields. This predates the story; new property/assertion fields retain their evidence.
- Blind 2 — medium, patch: nonce/predicate lacked HTTP status assertions; now require 200/400.
- Blind 3 — medium, patch: replay lacked initial success and polling invariance; added successful verification, call counter and before/after polling.
- Blind 4 — medium, patch: native ZK empty200 coverage missing; added polling and zero-proof checks.
- Blind 5 — medium, patch: multi later-credential empty/predicate coverage missing; added named reason, credential ID, polling and zero-proof checks.
- Blind 6 — medium, patch: CBOR opaque diagnostic coverage missing; native query now asserts transcript-binding and persisted detail.
- Blind 7 — medium, patch: malformed-property controls only claimed FAIL; added claimed PASS and nonbinding cases with report failure assertion.
- Blind 8 — false: Verification lists planned commands; Implementation Notes explicitly records 18/19 full-run failure and separate passing retry.
- Verification 1 — medium, patch: verified CBOR opaque diagnosis gap; same test correction as Blind6.
- Verification 2 — medium, patch: verified native and multi empty gaps; same test corrections as Blind4/5.


## Verification

- `make test-carrier-conformance` — runner vocabulary/inventory controls pass.
- `make test-go-fast` — race-enabled parser, session and transcript controls pass.
- `make test-go-zk` — linked verifier and HTTP regression tests pass.
- `make e2e` — fifteen existing and four new live assertions pass; committed carrier vectors still verify.
- `make check` and `git diff --check` — fast gates and whitespace checks pass; record unavailable prerequisites with their exact failures.
