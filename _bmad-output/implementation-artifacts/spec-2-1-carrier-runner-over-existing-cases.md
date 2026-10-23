---
title: '2.1 Carrier runner over existing cases'
type: 'feature'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
baseline_commit: '6cf84df5806ecd22792eade3b8ced8d14f148202'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/stories/2-1-carrier-runner-over-existing-cases.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The 13 E2E assertions lack per-case carrier and response-mode results. They exercise interim JSON; separate Go tests cover both interim JSON and mso-mdoc-zk-cbor, so the suite must not imply a 13-case cross-carrier matrix.

**Approach:** Preserve the 13 cryptographic E2E assertions, report actual carrier and response mode per case, and run the existing carrier tests and vectors as a second layer. Distinguish missing entries, unknown bytes, malformed envelopes, and failed JWE decryption. Integrate with E2E CI and document evidence limits.

**Boundaries & Constraints**

**Always:** Preserve proof checks and negative controls; use checked-in vectors; separate carrier from transport mode; report missing cases; distinguish the mandatory DC API path from the unofficial OpenID4VP mso_mdoc_zk profile; preserve existing shared-file edits.

**Never:** Claim all 13 E2E cases exercise both encodings; replace cryptographic checks with parsing; claim local tests prove independent-wallet behavior or external conformance.

**I/O & Edge-Case Matrix**

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| E2E suite | 13 numbered assertions | One row per case with carrier, response mode and result | Missing rows are `NOT_EXECUTED`; failures remain `FAIL` |
| Carrier vectors | Both committed encodings | Report carrier and test outcome | Setup failure is `ERROR` |
| Missing entry | Query ID absent | `missing-entry` naming the ID | Not a parse failure |
| Unsupported bytes / malformed envelope | Matches neither family / malformed recognized JSON | `unknown-carrier` / `malformed-envelope` | Separate from binding failure |
| Invalid encrypted response | Compact JWE fails authentication | `decryption-failed` before presentation parsing | Separate from binding failure |

</frozen-after-approval>

## Code Map

- `tests/e2e.sh` -- numbered assertions and `pass`/`fail` helpers; cases 1–5 and 12–13 use `direct_post`, cases 6–11 use `direct_post.jwt`.
- `tests/README.md` -- documents the 13 behaviors. Append without replacing the in-progress AV runner documentation.
- `verifier/go/oid4vp/carrier.go` -- `Carrier`, both implementations and dispatch. Unknown non-JSON/non-CBOR bytes currently fall through to `InterimJSON`; classify them explicitly.
- `verifier/go/oid4vp/vptoken.go` -- `VPToken.ParseCarrier` names a missing entry and returns the parsed carrier.
- `verifier/go/present.go` -- decryption failure happens before carrier parsing; preserve that distinction.
- `verifier/go/oid4vp/carrier_test.go`, `verifier/go/carrier_vectors_test.go`, `tests/vectors/carrier-v1/manifest.json` -- existing dispatch, same-proof and committed vector coverage for both encodings; do not regenerate bytes.
- `Makefile` `e2e` and `.github/workflows/ci.yml` E2E job -- integration point. Keep `make check` Rust-free.

## Tasks & Acceptance

**Execution:**
- [x] `verifier/go/oid4vp/carrier.go` and `vptoken.go` -- distinguish unsupported bytes and missing entries as stable carrier outcomes without weakening strict parsing.
- [x] `tests/e2e.sh` -- emit stable machine-readable records for every existing numbered assertion, including actual carrier and response mode, while keeping current human-readable output and checks.
- [x] `tests/run_carrier_conformance.py` and focused tests -- orchestrate the E2E and Go carrier-vector suites, validate complete/unique case records, and report setup failures and unexecuted cases without converting them to passes.
- [x] `Makefile`, `.github/workflows/ci.yml`, `tests/README.md`, `docs/README.md` -- wire the runner into the E2E job and document/index the runner and its limits.
- [x] `docs/profiles/CARRIER-CONFORMANCE-RUNNER.md` -- describe the two-layer coverage model, outcome vocabulary, vector provenance, local command and explicit external-evidence boundary.

**Acceptance Criteria:**
- Given the existing E2E script and Go carrier tests, when the runner completes, then each of the 13 E2E cases appears once with carrier, response mode and result, and both encodings are covered by committed carrier vectors.
- Given missing entries, unsupported bytes, malformed envelopes or undecryptable JWEs, when reported, then each has a distinct outcome rather than a generic parse or binding failure.
- Given a failed or incomplete child suite, when a report is written, then failures stay `FAIL` and unobserved cases stay `NOT_EXECUTED` or `ERROR`.
- Given `make e2e` or CI E2E, when it runs, then it invokes the documented runner; `make check` remains Rust-free.
- Given only local tests, when evidence is reported, then no independent execution is claimed and Story 2.4 remains the external evidence gate.

## Implementation Notes

- Baseline: `6cf84df5806ecd22792eade3b8ced8d14f148202`.
- Added stable `ErrMissingVPTokenEntry`, `ErrUnknownCarrier`, and `ErrMalformedEnvelope` classes. Carrier selection labels JSON/CBOR families from their leading bytes and rejects bytes outside both families. Response decryption remains upstream and distinct.
- `tests/e2e.sh` retains its 13 human-readable assertions and writes private JSONL case events when called by the runner. The report confirms one unique row per case; carrier vectors remain a separate layer because the 13 E2E checks do not each execute both encodings.
- The runner discovers assertion ids from `tests/e2e.sh`, validates each case's observed carrier, mode and expected outcome, rejects malformed/duplicate/missing events, bounds child time/output, and verifies manifest hashes plus visible Go vector-test execution. Parser controls, the Rust-linked vector pair and E2E run in that order.
- The initial integrated run `/tmp/carrier-report.json` recorded parser, committed vectors and all 13 E2E cases as PASS with no inventory problems. The later run after adding observed carrier response fields and stricter validation passed parser/vector suites and E2E cases 1–11; it was stopped during the existing `stop_verifier` wait before cases 12–13. No full E2E rerun was performed after those final response/report changes.
- Final focused verification passed: `python3 -m unittest discover -s tests -p 'test_carrier_conformance.py'` (16 tests); `(cd verifier/go && go test -race ./oid4vp/...)`; `(cd verifier/go && go test -race . -run '^TestPlainPathCarrierOverride$' -count=1)`; targeted carrier/vector Go tests and compile-only root Go tests; `shellcheck --severity=warning tests/e2e.sh`; `python3 -m py_compile tests/run_carrier_conformance.py tests/test_carrier_conformance.py`; `git diff --check`; and direct validation of all five committed manifest SHA-256 hashes. The HTTP test asserts that selected carrier names appear in the response JSON.
- The runner emits a separate dated external-evidence record with status `BLOCKED`, a not-yet-selected target, and a reason. Independent execution remains pending Story 2.4; local results do not establish external conformance.

## Spec Change Log

## Review Triage Log

- `low` — The authored Story 2.1 criterion ended with a duplicated, incomplete “carrier family” fragment. Removed the fragment and rewrote the criterion as a single unknown-carrier/decryption requirement, preserving its intended cases.
- `medium` — The Story 2.1 planning record, sprint tracker and implementation spec had conflicting lifecycle states. The planning record now reflects the current implementation lifecycle; the tracker remains awaiting the external operator gate after this local review.
- `medium` — E2E case events assigned carriers in shell literals, which could report expectations as observations. The response now carries selected parser carrier names and E2E events read them; cases rejected before parsing carry an empty list.
- `medium` — A new assertion could be omitted while the hardcoded inventory still passed. The runner discovers numbered assertion ids from the E2E script and fails on any id without metadata.
- `medium` — An allowed but incorrect outcome label could pass. The runner validates expected carrier, response mode and outcome for each current case, with mutation controls.
- `medium` — Array/object metadata fields could raise `TypeError` during set membership. The runner checks field types before membership and emits an `ERROR` row.
- `medium` — Child processes had no deadline and could hold output indefinitely. The runner now bounds runtime/output, terminates and reaps the child process group, and returns `ERROR` on either limit.
- `medium` — A malformed UTF-8 JSONL file could abort report generation. Strict decode failures are retained as inventory problems, leaving unobserved cases `NOT_EXECUTED`.
- `medium` — A Go `-run` typo or skipped vector test could exit zero without verifying vectors. The runner uses verbose Go output and requires an observed `PASS` line; no match or skip is `ERROR`.
- `medium` — The manifest's committed byte hashes were not checked. The runner now compares all five listed hashes with the committed files.
- `false` — The review said a failed carrier-vector test could not identify its failing leg. `TestCarrierVectorsVerifyIdentically` has carrier-specific fatal messages for parse and verification stages, and the report retains the shared test output; vector rows now state that they reflect the paired same-proof test.
- `medium` — Legal JSON whitespace before `{` was rejected by carrier sniffing. Sniffing now skips JSON's four permitted leading whitespace bytes while parsing the original payload.
- `medium` — Runner outcomes were not tied to case IDs, so a valid but incorrect label could pass. A case-specific outcome map and mutation test now reject it.

## Design Notes

Keep reporting layered: E2E rows have response modes; the vector layer covers both encodings. Do not infer carrier from encryption. A leading `{` identifies the interim envelope even if malformed; CBOR map/array heads select the CBOR parser. Other bytes are `unknown-carrier`.

## Verification

**Commands:**
- `python3 -m unittest discover -s tests -p 'test_carrier_conformance.py'` -- runner event ordering, unique/exact inventory, missing-child and malformed outcome controls pass.
- `(cd verifier/go && go test -race ./oid4vp/...)` -- carrier parsing/dispatch tests pass.
- `make test-go-zk` -- fixture-backed proof cross-carrier tests pass when the documented Rust static library is built.
- `make e2e` -- all 13 live end-to-end assertions pass with carrier/mode rows emitted; if build prerequisites or dependencies are unavailable, retain the exact failure and mark runtime coverage unavailable.
- Inspect CI wiring and run the repository fast checks relevant to touched files; record every command and return code in the completion evidence.
