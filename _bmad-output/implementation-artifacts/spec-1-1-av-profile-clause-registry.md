---
title: '1.1 AV Profile clause registry'
type: 'feature'
created: '2026-10-23'
status: 'done'
baseline_revision: fc8793ed6f6d20b5660de709f5c04116b2d997e7
review_loop_iteration: 0
followup_review_recommended: false
context: []
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Story 1.1 lacks a machine-readable inventory connecting every normative AV Profile Annex A clause to a test identifier or an explicit v1 exclusion. Existing AV-related tests do not establish complete profile conformance.

**Approach:** Pin the public primary source at its full Git revision, retain its normative text with provenance, and produce an ordered JSON clause registry with source anchors, precise test mappings and exclusions. Add an offline structural/coverage validator and document its limits.

## Boundaries & Constraints

**Always:** Account for A.1–A.9, including inherited requirements, encodings, recommendations, permissions and conditional requirements. Preserve section order and source wording. Record a full canonical source commit, date and SHA-256. Distinguish mapped tests from executed conformance evidence. Give every clause exactly one test mapping or a non-empty explicit `out of scope for v1` reason. Check test references against real files/symbols and scope mappings to what tests assert. Public upstream source only; no private repository imports. Leave `sprint-status.yaml` entirely owned by the orchestrator.

**Never:** Claim certification, expand the inherited ISO/OpenID standards into unverified requirements, count the unofficial OpenID4VP ZK transport as AV-profile conformance, implement the later fixture/runner/issuance stories, change runtime policy to satisfy the profile, or commit `demo-data/`.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Complete registry | Pinned source and ordered clauses | Offline validation succeeds with mapped/excluded counts | Exit 0 |
| Coverage drift | Missing clause/source block, duplicate ID, unaccounted normative text | Identify missing or inconsistent coverage | Nonzero exit |
| Provenance drift | Source bytes/version digest changed | Reject pin mismatch | Nonzero exit |
| Broken disposition | Missing mapping/reason, both present, dangling test reference | Name offending clause/test | Nonzero exit |

</intent-contract>

## Code Map

- `_bmad-output/planning-artifacts/epics.md:365` — authoritative Story 1.1 criteria; read-only. Epic context is `_bmad-output/implementation-artifacts/epic-1-context.md`.
- Public `eu-digital-identity-wallet/av-doc-technical-specification`, commit `014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`, `docs/annexes/annex-A/annex-A-av-profile.md` — verified via GitHub primary API, commit date 2026-09-01. Downloaded full original at `/tmp/av-annex-a.md`; use the A.1–A.9 prefix, excluding A.10 comparison/A.11 examples. Preserve upstream attribution/license.
- `verifier/go/oid4vp/isodcapi_test.go` — `TestDeviceRequestBuild`, `TestEncryptionInfoShape`, `TestHPKERoundTrip` cover DC API structures and primitives, not complete plain mdoc fallback.
- `verifier/go/iso_test.go` — `TestStep2aMultipazDeviceResponse`, `TestISOUnsolicitedSpecIsRefusedFast`; integration tests link Rust. Local circuit policy is not the scheme owner's accepted set.
- `verifier/go/internal/jose/jwk_test.go`, `jws_test.go` — P-256/ES256 primitive tests. `verifier/go/zk/zk_test.go` — local allowlist checks only.
- `issuer/mint_ee_poa.py` adds `issuing_country`; do not map it as strict AV attribute-allowlist proof. `verifier/go/iso.go` parses ZK documents only; plain fallback is a gap. Android HAIP policy requires `direct_post.jwt`, differing from A.6 plain `direct_post`.
- `.github/workflows/ci.yml` — fast Python job and path filters; `Makefile` — `check` fast checks. Add offline stdlib-only validation consistently to both.
- `tests/README.md`, `docs/README.md` — catalogue checks and index new documentation.

## Tasks & Acceptance

**Execution:**
- [x] `docs/profiles/av/annex-a-normative.md`, `source.json`, upstream license — retain reproducible public normative excerpt, source/full-excerpt hashes, extraction rule and full revision/date; no unverified version claims.
- [x] `docs/profiles/av/clause-registry.json` — inventory source blocks and stable ordered clause IDs, normative strength, source locations/excerpts, test catalogue and exactly one mapping or explicit v1 exclusion. Account for non-obligation sections and conditional notes. Reuse actual tests with scoped limits; no fabricated evidence.
- [x] `tests/check_av_registry.py`, `tests/test_av_registry.py` — offline schema, provenance, source-block completeness/order and test-reference validation; mutation checks for matrix failures. Make source coverage independently checkable against retained text, rather than merely checking hand-authored clause rows against each other.
- [x] `Makefile`, `.github/workflows/ci.yml` — wire validation/mutation checks into fast checks and include registry documentation in CI path filters.
- [x] `docs/profiles/AV-PROFILE-CLAUSE-REGISTRY.md`, `docs/README.md`, `tests/README.md` — explain schema, pinned version, mapping limits, exclusions, refresh procedure and commands.

**Acceptance Criteria:**
- Given the pinned Annex A version, when the registry is inspected and validated, then every normative clause has a real scoped test identifier or an explicit non-empty out-of-scope-for-v1 note.
- Given a clean checkout, when the offline check runs, then it verifies the recorded pin, complete ordered source accounting and referenced test symbols without requiring network access or cryptographic builds.
- Given edits that remove source coverage, corrupt a pin or break a disposition/test mapping, when checks run, then they fail with an actionable error.
- Given the registry documentation, when a programme evaluates coverage, then it can identify the precise revision, maintenance procedure, implementation gaps and distinction between mapped tests and unexecuted conformance claims.

## Spec Change Log

## Review Triage Log

### 2026-10-23 — Review pass
- verdicts: 12 findings — high 0, medium 4, low 6, false 2, maybe-false 0
- findings (in reviewer report order; verification-gap reported no gaps and intent auditor reported descriptive boundaries, not defects):
  - `[medium]` `[patch]` Edge: duplicate JSON keys silently choose the last provenance/disposition — reproduced with ordinary json.loads; both files now use a duplicate-rejecting object_pairs_hook, with malformed-file controls.
  - `[medium]` `[patch]` Edge: build-disabled Go files satisfy symbol checks — mapped files had no tags, but adding go:build ignore reproduced acceptance; explicit modern/legacy build constraints now fail with an actionable supported-boundary error.
  - `[false]` `[reject]` Blind: current validator must enforce historical ID stability — the guide explicitly makes cross-revision semantic identity preservation a refresh review obligation; this snapshot validator claims current syntax/uniqueness, not a historical-ID guarantee. No historical consumer or ID change exists in this diff.
  - `[false]` `[reject]` Blind: compound A.8-002 leaves obligations unaccounted — its exact excerpt includes every obligation and its explicit exclusion names mandatory system, full tuple, scheme acceptance and external editions. Story 1.1 allows exclusions; it does not require independently executed results for each sub-sentence. Full source partition checking prevents omissions.
  - `[medium]` `[patch]` Blind: duplicate JSON keys hide conflicting values — same verified root cause as Edge duplicate-key finding; fixed by duplicate-rejecting loading and tests for top-level and nested keys.
  - `[low]` `[reject]` Blind: editorial normative-strength labels can add a force absent from an excerpt — mutation confirmed, but exact source text remains immutable and governs; documentation explicitly limits labels to editorial classification. No runner uses these labels. Exhaustive contextual strength inference adds complexity for uncommon erroneous editorial edits; semantic review remains required.
  - `[low]` `[reject]` Blind: inherited proof-property strength can be relabelled informative — mutation confirmed; the original governing SHALL and every child remain intact and carry explicit exclusions. No claim of semantic-label validation or executed result depends on the labels. Contextual inheritance metadata/validation is disproportionate to this uncommon editorial mistake.
  - `[low]` `[patch]` Blind: comment stripping joins Go tokens — valid func/*comment*/TestName was rejected; replacing comments/literals with whitespace now preserves token separation, with a passing regression case.
  - `[low]` `[reject]` Blind: Go testing import aliases are not recognized — confirmed the detector expects *testing.T; every current mapped test uses that spelling and none is rejected. Adding alias/import parsing for an unused future form exceeds a direct correction; the checker fails visibly if a maintainer changes that form.
  - `[medium]` `[patch]` Blind: build constraints allow unavailable test references — same verified root cause as Edge build-selection finding; modern and legacy directives are now conservatively rejected and the boundary is documented.
  - `[low]` `[patch]` Blind: retained source relative terminology link breaks locally — confirmed target is absent; added a companion map to the pinned public upstream target without modifying source bytes.
  - `[low]` `[patch]` Blind: planning documents are missing from docs index — confirmed absent; added the epic-context and story-spec entries.
- Grouped patch entries: 2 medium, 3 low. Deferred entries: 0. Rejected entries: 5. All four layers completed; the fourth launch was retried once a concurrency slot became available.

### 2026-10-23 — Follow-up review pass
- verdicts: 12 findings — high 0, medium 3, low 5, false 4, maybe-false 0
- findings (layer report order: verification gap, edge case, blind hunter):
  - `[low]` `[patch]` Verification: successful count output has no exact regression oracle — the reviewer demonstrated that reporting 94 exclusions instead of 88 passes the original suite. The positive control now asserts all five pinned counts and the clauses = mapped + excluded identity.
  - `[medium]` `[patch]` Edge: ignored or platform-specific Go filenames satisfy reference validation — reproduced acceptance of `_ignored_test.go`, `_windows_test.go` and `_amd64_test.go`; an isolated Go module confirms ignored/foreign-platform files yield exit 0 with no test files. The checker now rejects ignored basenames and known implicit platform suffixes, with rejection and portable-name controls.
  - `[false]` `[reject]` Blind: snapshot checking must enforce historical identifier identity — carried: the previous review established that historical semantic ID preservation is a documented source-refresh review obligation. No ID was changed or reused; the snapshot checker claims syntax, uniqueness and section consistency.
  - `[medium]` `[patch]` Blind: implicit Go file/directory selection is unchecked — same demonstrated filename defect as Edge; fixed by the same filename checks. The broader directory claim is bounded by exact package commands: Go's recursive package-pattern exclusions do not establish exclusion from an explicitly named package. Canonical command checks additionally prevent a stale command from selecting the old package after a directory move.
  - `[medium]` `[patch]` Blind: catalogue commands can select another or nonexistent test — reproduced acceptance of a command naming `TestDoesNotExist`. Commands must now equal the canonical exact package/symbol selector derived from each reference; wrong-package and wrong-symbol controls reject drift. This validates command text without executing it.
  - `[low]` `[reject]` Blind: directive-shaped raw-string data triggers conservative build-constraint rejection — reproduced a valid literal containing `//go:build ignore` being rejected. Current mapped files contain no such data; supporting this uncommon form requires additional lexical/directive-region parsing. The visible conservative failure is retained rather than adding that complexity.
  - `[low]` `[patch]` Blind: valid Go signature forms are rejected — reproduced rejection of the exact mapped declaration rewritten with a multiline trailing comma; the signature expression now accepts it and a positive control passes. The alias subclaim retains the prior low/reject decision: carried, every mapped declaration uses unaliased `testing.T`, the supported form is documented, and import-alias parsing would add complexity for an unused form.
  - `[low]` `[reject]` Blind: inherited SHALL labels can be changed to informative — carried: source wording and the governing SHALL remain intact, labels are explicitly editorial, no runner consumes them, and semantic inheritance review is documented. Contextual inference machinery remains disproportionate to this uncommon editorial mistake.
  - `[false]` `[reject]` Blind: compound A.4/A.8 rows leave requirements without dispositions — inspection confirms A.4-006 retains its complete exact text and explicit exclusion; A.8-011 retains its full text and an expressly partial mapping whose limits identify accepted-set authority and processing order. The intent permits exclusions and scoped mappings; it does not require one executable result per sub-obligation in this inventory.
  - `[false]` `[reject]` Blind: one test per disposition is a schema defect — the intent expressly requires exactly one mapping or exclusion. Source-preserving clause partitioning supports the current complementary primitive mappings; no current requirement loses its text or disposition. Multiple-test aggregation would introduce a later runner design into this story.
  - `[false]` `[reject]` Blind: omitted existing negative tests make the inventory incomplete — the cited tests exist, but the intent requires each clause to have a mapping or exclusion, not an exhaustive catalogue of every related test. Existing partial mappings make no exclusive or exhaustive evidence claim; extra catalogue entries are not necessary to account for any source text.
  - `[low]` `[patch]` Blind: nonempty but incorrect attribution passes provenance validation — reproduced acceptance of an unrelated author while the checked upstream license remained intact. The reviewed attribution text is now included in the independently fixed pin; the metadata mutation control rejects changes and the refresh guide identifies this pin.
- Grouped patch entries: 2 medium, 3 low. Deferred entries: 0. Rejected findings: 6 (with the rejected alias subclaim recorded in its patched row). No existing deferred-work entries or sprint-board data were edited.
- All four layers completed before triage. The concurrency limit allowed three initial launches; the intent auditor launched when the first slot became free. The intent audit reported descriptive interpretations and surface limits, not additional defects: textual accounting and reviewed partial assertions satisfy the inventory reading; per-obligation execution, automatic semantic mapping validation and upstream acquisition are distinct surfaces.
- Follow-up recommendation: false. This follow-up pass patched no high entry; the patched entry counts are high 0, medium 2, low 3. No additional unverified risk was introduced that requires another review pass.

## Design Notes

The pin identifies Annex A itself; unversioned standards it references are inherited dependencies, not independently verified edition claims. Keep `[UNVERIFIED]` for unresolved external publication/accepted-set details. Source blocks provide an independent coverage denominator, including declarative requirements without uppercase keywords. Splitting a source paragraph into multiple clause rows is permitted; omitting a normative block is not. A.1 overview and A.2 terminology receive explicit section dispositions; A.10/A.11 are non-normative and excluded. No human-only external action is required for this story.

## Verification

**Commands:**
- `python3 tests/check_av_registry.py` — complete ordered registry, provenance and references validate offline.
- `python3 -m unittest discover -s tests -p 'test_av_registry.py'` — meaningful malformed-registry mutations are rejected.
- `make check` — existing fast baseline and registry checks pass.

## Auto Run Result

Status: done

Completed a follow-up review of Story 1.1 against baseline `fc8793ed6f6d20b5660de709f5c04116b2d997e7`. The implementation present at dispatch was commit `7de60b19f10ae1cf2ebbae3db053f5eed646086c`; neither its completed status nor the orchestrator's board row was treated as verification evidence.

The registry retains public Annex A sections A.1–A.9 at revision `014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`, timestamp `2026-09-01T21:59:02Z`. Offline validation accounts for 86 source blocks and 94 clause rows: six partial mappings and 88 explicit exclusions, referencing six real test symbols. These totals include non-obligation text and do not represent an executed conformance result or certification.

Original implementation files covered by review:
- `docs/profiles/av/annex-a-normative.md` — exact public upstream A.1–A.9 prefix.
- `docs/profiles/av/source.json` — source revision, timestamp, digests, attribution and extraction rule.
- `docs/profiles/av/LICENSE.upstream` — retained upstream license notice.
- `docs/profiles/av/clause-registry.json` — complete source inventory, ordered dispositions and scoped test catalogue.
- `tests/check_av_registry.py` — offline source, provenance, coverage and reference validator.
- `tests/test_av_registry.py` — malformed-input controls and positive validation checks.
- `Makefile` and `.github/workflows/ci.yml` — fast local/CI validation and documentation triggers.
- `docs/profiles/AV-PROFILE-CLAUSE-REGISTRY.md`, `docs/README.md`, `tests/README.md` — schema, limitations, refresh instructions and indexes.
- `_bmad-output/implementation-artifacts/epic-1-context.md` — epic planning constraints.
- This spec — implementation contract, completed tasks, review decisions and result.

Files changed in this follow-up:
- `tests/check_av_registry.py` — reject ignored/platform-specific Go filenames and commands that select a different package/test, pin attribution, and accept multiline test signatures with a trailing comma.
- `tests/test_av_registry.py` — assert exact counts/accounting and add filename, command and signature controls; extend pin mutations to attribution. The suite now contains 36 tests.
- `docs/profiles/AV-PROFILE-CLAUSE-REGISTRY.md` — describe supported reference syntax, canonical commands and pin/toolchain refresh responsibilities.
- This spec — append the follow-up triage log and current hand-back result.

Review outcome: all four layers completed. Twelve findings received individual verdicts: high 0, medium 3, low 5, false 4, maybe-false 0. Six finding rows produced five grouped patches (high 0, medium 2, low 3). No items were deferred. Six findings were rejected: historical ID semantics are a refresh-review duty; conservative rejection of directive-shaped string data is a visible uncommon limitation whose repair requires additional parsing; inherited labels remain editorial; complete compound source passages already have honest dispositions; multiple-test aggregation conflicts with this intent's single mapping; and an exhaustive catalogue of every related test is not required for source accounting. The rejected import-alias subclaim remains documented alongside the signature correction. Detailed evidence and carried verdicts appear in the follow-up triage log above.

Follow-up review recommended: false. This was a follow-up pass and patched no high finding. No human decision or external operator action is required to finish this story.

Verification performed:
- `python3 tests/check_av_registry.py` — passed; 86 blocks / 94 clauses / 6 mappings / 88 exclusions / 6 symbols.
- `python3 -m unittest discover -s tests -p 'test_av_registry.py'` — all 36 tests passed, including malformed-input mutations, exact counts, portable filename controls and valid multiline declarations.
- `make check` — passed: formatting, Go vet, Python compilation/lint, shellcheck, fast Go race tests, transcript vectors, issuer key tests, sandbox age checks, registry validation and mutation suite.
- Public primary-source acquisition was repeated: GitHub's full canonical revision and committer timestamp match the pin; full upstream source and license SHA-256 values match; the extracted byte prefix exactly equals the retained excerpt.
- Isolated Go-module experiment confirmed ignored and foreign-platform files produce exit 0 with no test files, validating the reference-selection failure behind the filename patch.
- `git diff --check` — passed for the final owned changes.

Residual limits: registry validation does not execute catalogue commands, infer semantic obligation partitions, verify assertion bodies or prove historical ID identity. All mappings remain partial. Plain-mdoc fallback, AV-specific issuance/invocation, profile parameter fixtures and scheme-owner accepted-set authority remain documented gaps for later work; external edition/publication claims remain `[UNVERIFIED]`. Explicit build constraints are conservatively rejected, including directive-shaped raw-string data, and imported aliases for `testing` are unsupported. Known Go platform suffixes need review when the toolchain changes.

Ownership/finalization: the supplied spec had an existing removal of its prior result section; this run appends a current result without restoring that removed section. The sprint board was already modified at dispatch and was excluded from review, staging and commits. Its existing dirty state is preserved as orchestrator-owned work, not treated as a story defect or a blocking human decision. Existing deferred-work ledger entries were not opened, changed or resolved; the spec's `deferred: []` remains unchanged. Only this story's four follow-up files are committed; no push is performed.
