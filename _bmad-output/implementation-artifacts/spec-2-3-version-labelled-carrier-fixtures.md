---
title: '2.3 Version-labelled carrier fixtures'
type: 'feature'
created: '2026-11-11'
status: 'done'
baseline_commit: '9066b7d65ef6d9d3942fd8f3291727f25d7a3ee7'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The published carrier vectors record hashes and Multipaz provenance but lack profile-version labels. The profile's fifteen rules lack individual versioned derivations, so standards drift and new unlabelled material can escape detection.

**Approach:** Label every published carrier fixture and numbered profile rule with the current carrier-profile version and explicit source references. Add one offline validator shared by fast checks and the carrier runner, with mutation controls demonstrating rejection of missing, superseded and uncovered labels.

## Boundaries & Constraints

**Always:** Read the current version from the profile's authoritative Version header; compare every label to that value. Distinguish carrier-profile version, circuit version, implementation release and external-document revision. Record rule-specific derivations: observed Multipaz wire shape, OpenID4VP transcript, or repository policy as appropriate. Check external identifiers against primary sources; preserve existing uncertainty and mark unverifiable provenance [UNVERIFIED] with a corresponding spec §23 reference. Keep source metadata reviewable and validation offline.

**Never:** Invent an ISO second-edition draft revision, label local policy as an ISO requirement, refresh standards automatically, regenerate proof/transcript/payload bytes, change acceptance behavior or encryption defaults, copy private material, touch demo-data, publish, push or claim independent conformance. This story labels the published carrier-v1 set; unrelated AV and source-fixture suites retain their own provenance.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Current inventory | All discovered fixtures and numbered rules have current labels and resolvable versioned sources | Offline assertion passes | No writes during validation |
| Missing or stale label | Absent, empty, malformed or superseded fixture/rule version | Assertion fails naming the item | Runner cannot report passing vectors |
| Profile revision | Version header changes while item labels retain old version | Every stale item is rejected | No hardcoded expected 1.0 shortcut |
| Inventory drift | New unlabelled rule/file, duplicate rule, missing indexed file or orphan metadata | Assertion fails | Discovery and metadata must agree |
| Source gap | Missing/unknown source reference or source without a version/evidence disposition | Assertion fails | No guessed document revision |

</frozen-after-approval>

## Code Map

- `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`: authoritative Version header; rules 1–9/11 use bold Profile rule headings, rule 10 plain prose, 12–15 bold list items. Normalize rule markers without changing their requirements.
- `tests/vectors/carrier-v1/manifest.json`: five hashed files and two carrier names; add per-file provenance without changing existing hashes.
- `tools/build_cbor_fixture.py:main`: emits the manifest; preserve byte-building helpers and ensure future generation retains labels.
- `tests/run_carrier_conformance.py:run_conformance`: existing manifest/hash failures mark vector suite and rows ERROR; reuse that fail-closed seam.
- `tests/test_carrier_conformance.py:setup_root`: synthetic checkout/manifest must include minimal valid profile and label evidence.
- `verifier/go/oid4vp/carrier_test.go`, `verifier/go/carrier_vectors_test.go`: existing independent byte/crypto checks; preserve unchanged.
- `Makefile:test-carrier-conformance`: already runs through make check; extend with offline assertion and mutation suite.

## Tasks & Acceptance

**Execution:**
- [x] `docs/profiles/carrier-versions.json` — add versioned source inventory with primary or local evidence and explicit uncertainty; index it in `docs/README.md`.
- [x] `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md` — attach visible current-version/source labels to every numbered rule; document label maintenance and honest provenance scope.
- [x] `tests/vectors/carrier-v1/manifest.json`, `tools/build_cbor_fixture.py` — label each of the five payload/support files and preserve generator consistency and all golden bytes.
- [x] `tests/check_carrier_versions.py` — implement reusable read-only validation, derive current version from the profile, discover the complete rule/file inventories, and resolve versioned sources.
- [x] `tests/test_carrier_versions.py`, `tests/test_carrier_conformance.py` — cover every matrix edge, including newly added rules/files and a bumped profile header.
- [x] `tests/run_carrier_conformance.py` — apply the validator and retain actionable errors through the existing vector/report failure path.
- [x] `Makefile`, `tests/README.md`, `docs/profiles/CARRIER-CONFORMANCE-RUNNER.md` — wire fast checks and document invocation, refresh steps and evidence limits.

**Acceptance Criteria:**
- Given a clean checkout, when the documented offline check runs, then all current numbered profile rules and published carrier-v1 fixture files have traceable version labels without requiring Rust or network access.
- Given invalid label evidence, when the carrier runner evaluates it, then its report fails even if child proof tests pass.
- Given unchanged golden payloads, when existing vector checks execute, then both carriers retain identical normalization, transcript bytes and proof verification.

## Implementation Notes

Implementation and matrix audit: 16 new version/mutation/generator controls and 20 existing runner tests passed. Header-bump coverage checks all 15 rules and five files; remaining controls cover absent/stale labels, uncovered/duplicate/orphan items, unresolved sources and runner ERROR propagation despite passing children. make check exited 0 (/tmp/story23-final-make-check.log); the required real Go carrier proof test passed in 30.048s. Direct baseline comparison confirms all five payload/support files and pre-existing hashes unchanged. Full live E2E was not run; the approved metadata-only verification uses fast and carrier-crypto gates.

## Spec Change Log

Completion evidence (2026-11-11): all three independent review layers returned findings; verified patch findings fixed and covered by 22 version tests plus 20 runner regressions, all passing. Final make check exited 0 (/tmp/story23-reviewed-make-check.log). Final Go real carrier proof verification passed in 33.210s (/tmp/story23-reviewed-carrier-proof.log). All five golden payload/support files remain byte-identical to baseline, with unchanged hashes. git diff --check passed. No deferred work; historical source version consistency remains a reviewed provenance assertion rather than automatic external-source refresh.

## Review Triage Log

- Edge 1 — medium, patch: builder checks list shape but not source membership, so unknown refs survive generation; share source validation and test output.
- Edge 2 — medium, patch: invalid UTF-8 uncertainty evidence reproduced UnicodeDecodeError outside the protected read; retain item-specific diagnostics and runner ERROR reports.
- Edge 3 — medium, patch: replacing evidence with item38 outside section23 reproduced a clean validation; require actual section23.
- Edge 4 — medium, patch: malformed second Version header is ignored because only valid matches are counted; require exactly one declaration.
- Blind 1 — medium, patch: system python builder import reproduced ModuleNotFoundError due to namespace package shadowing; import concrete local module and test isolated process.
- Blind 2 — medium, patch: same builder reference-validation defect as Edge1; unknown, duplicate and malformed refs need rejection before writes.
- Blind 3 — medium, patch: standalone validator checks hash presence but not digest/content, contradicting documented inventory agreement; check actual sha256 and corruption.
- Blind 4 — medium, patch: appended Markdown heading rule16 reproduced clean validation; extend numbered-rule discovery to Markdown headings.
- Blind 5 — medium, patch: same duplicate-declaration defect as Edge4; add malformed second-header control.
- Blind 6 — medium, patch: same uncaught evidence read as Edge2; cover invalid bytes and report retention.
- Blind 7 — medium, patch: same missing section23 guard as Edge3; resolve references only inside section23.
- Blind 8 — low, rejected: source versions are reviewed historical provenance, not automatically refreshed release assertions. Arbitrary replacement requires editing the reviewed inventory; source-specific parsers would add complexity beyond checking traceable nonempty revisions and explicit evidence disposition.
- Blind 9 — low, patch: the source snapshot is historical, while local paths are navigation/existence checks; clarify that distinction in the source scope rather than imply current working-tree content is hashed snapshot evidence. Existing Git history preserves the referenced canonical revision.
- Verification 1 — medium, patch: filed workflow evidence shows CI invokes only old regression discovery and omits profile/inventory inputs; run the full carrier make target and include evidence paths in fast filters.
- Verification 2 — medium, patch: filed mutation demonstrates deleting emitted provenance evades helper-only tests; invoke builder main against temporary paths and inspect generated metadata and hashes.

## Verification

- `make test-carrier-conformance` — offline assertion, mutation controls and existing runner regressions pass.
- `make check` — established fast CI gates pass.
- `cd verifier/go && go test -race . -run '^TestCarrierVectorsVerifyIdentically$' -count=1` — both committed carriers still verify, given the existing staticlib prerequisite.
- `git diff --check` and payload/hash comparison — no whitespace defects or golden byte changes; record unavailable checks with actual output.
