---
title: "3.4 RP-register interface proposal"
type: feature
created: "2026-11-13"
status: awaiting-operator
operator_actions:
  - "Publish the reviewed proposal from implementation commit 7515a9631c9efa8f1d686cb1edf4aa394ae831ae on the public fork remote or agreed public channel, and record its actual URL, full revision and publication date."
  - "Arrange an Estonian service-owner usability review of the published proposal, record the reviewer name and role, date, full reviewed revision, usable or not-usable verdict and concrete feedback with permission to publish, and address the feedback."
baseline_revision: 634b1d14c0844d53e82a8923161e6207d4f65412
review_loop_iteration: 0
followup_review_recommended: false
context:
  - "{project-root}/AGENTS.md"
  - "{project-root}/docs/DEVELOPMENT.md"
  - "{project-root}/docs/decisions/ADR-005-INTENDED-USE-SOURCE.md"
  - "{project-root}/docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md"
warnings: []
deferred: []
---

<intent-contract>

## Intent

**Problem:** Story 3.4 (FR12) requires a published RP-register interface proposal, mocks and a compliance note against ARF RP topics. The existing implementation contract lacks a sourced authority-facing proposal and supersession path.

**Approach:** Write an indexed proposal grounded in the shipped Story 3.3 seam and pinned public ARF primary sources. Supply reproducible mock controls, precise compliance gaps and publication/review handoff. D3 also requires an Estonian service owner to review usability; never fabricate human review or public publication.

## Boundaries & Constraints

**Always:** Preserve ADR-005/007: one six-field model, exact entity/service identity, doctype-qualified predicates, fail-closed errors, and strictly-younger-than-15m default positive cache. Explain fresh-cache outage exception and stale discard. Distinguish local policy permission from legal entitlement, request authentication and user approval. Pin ARF to the full canonical submodule revision and cite in-repo sources/requirement IDs. Verify version labels from primary text. Explain TS5/TS6 mapping and limitations, topics 6/27/44 and relevant 19/55 boundaries. Mark unverifiable current RIA API/certificate claims [UNVERIFIED], collected in spec section 23. Use public in-repo material only. You are not alone; preserve others’ edits. Commit all agent work before terminal handoff. Outstanding publication/human review requires awaiting-operator, nonempty imperative operator_actions and matching Auto Run Result.

**Never:** Write, stage, commit or revert sprint-status.yaml; orchestration states are not verification evidence. Commit demo-data/secrets, push, send external messages, import private ee-eudiw material, claim RIA API absence from unsuccessful search, claim a production certificate parser exists, expand runtime/proof scope or modify this intent-contract. Do not fabricate publication or a human verdict. Do not use blocked for operator-only obligations.

</intent-contract>

## Code Map

- `rp-kit/REGISTER-CONTRACT.md:1` — shipped interface, transport, errors and cache; proposal must agree and link both ways.
- `verifier/go/rpregister/register.go:24` — errors, DefaultTTL/Capacity, strict six-field model, mock, HTTP seam, bounded cache; `register_test.go` supplies negative controls.
- `verifier/go/present.go`, `iso.go`, register gate and related tests — both flows already gated; resolve exact symbols before citing.
- `rp-kit/register/synthetic-v1.json`, `register/README.md` — three synthetic positives and omitted negative; no authority registrations.
- `eudi-arf/hltr/high-level-requirements.csv` and `docs/annexes/annex-2/annex-2.02-high-level-requirements-by-topic.md` — primary identifiers at Git-pinned `6373eee10b6e80225c7ce706a5ff1775fb799b22`; verify Git revision.
- `eudi-arf/docs/technical-specifications/ts5-common-formats-and-api-for-rp-registration-information.md`, `ts6-common-set-of-rp-information-to-be-registered.md` — primary API/model/version evidence; six fields are a projection, not full TS5/TS6.
- `_bmad-output/planning-artifacts/epics.md:672`, `docs/research/2026-10-23-shared-appendices.md:140` — story ACs and D3 human usability exit criterion. Epic 3 context and Story 3.3 spec provide continuity independently of bookkeeping.
- `docs/README.md:9`, `rp-kit/README.md`, `tests/README.md`, `Makefile` — indexes and existing executable checks.
- `spec/EE-EUDIW-TS-1.0.md` section 23 — collect newly introduced unverified external claims without rewriting historical assertions.

## Tasks & Acceptance

**Execution:**
- [x] `rp-kit/REGISTER-PROPOSAL.md` — write release-ready purpose/scope, exact lookup/model/error/cache contract, synthetic reproduction, pinned ARF/TS5/TS6 compliance/gap table, privacy/trust/certificate migration, authority decisions and dated source record; include publication and human-review handoff.
- [x] `rp-kit/REGISTER-CONTRACT.md`, `rp-kit/README.md`, `docs/README.md` — add navigation; preserve house style and runnable paths.
- [x] `spec/EE-EUDIW-TS-1.0.md` section 23 — collect new external uncertainty and link proposal source record, only when needed.
- [x] Story spec — record actual verification/publication/human-review evidence; terminal operator_actions when applicable.

**Acceptance Criteria:**
- Given the mock-backed seam, when an integrator reads the indexed proposal and follows its commands, then exact identity/model/refusal/default/cache behavior is documented and demonstrated by passing existing controls.
- Given pinned public ARF RP topics, when the compliance note is inspected, then identifiers, revision and TS5/TS6 mapping distinguish implemented behavior, proposal and gaps without certification claims.
- Given unresolved authority API/access-certificate decisions, when the proposal is prepared for publication, then it remains useful even if superseded, without waiting for a public API or asserting unverified absence.
- Given completed local work, when publication or D3 human usability review lacks evidence, then the committed deliverable provides a precise handoff and terminal awaiting-operator with nonempty imperative operator_actions; public publication/human verdict remain unclaimed.

## Spec Change Log

## Review Triage Log

### 2026-11-13 — Review pass

- verdicts: 8 findings — high 0, medium 7, low 0, false 1, maybe-false 0.
- Scheduling: all four required layers completed. The fourth launch hit the three-child concurrency limit, so intent alignment launched immediately after verification-gap returned; no layer was skipped. No triage or repair began before all four results arrived. Verification-gap reported no gaps; edge-case reported no findings.
- findings (report arrival order):
  - `[false]` `[reject]` Intent: machine-readable operator handoff and commit absent from intermediate review diff — the reviewed spec was deliberately in-review before this workflow’s Finalize phase; publication and human usability evidence were explicitly unclaimed. Finalization below supplies the terminal state, imperative operator_actions, canonical implementation revision and committed result. This is an expected intermediate snapshot, not an omitted implementation.
  - `[medium]` `[patch]` Blind 1: RPA_06a omitted from Topic 6 — pinned CSV confirms AS-WP-06-008; added its notification and non-disclosure-or-choice obligation plus explicit wallet gap. The same finding’s claim that empty RPA_02a does not exist is rejected: pinned CSV contains RPA_02a / AS-RP-06-001 with Empty text; retained and made the identifier explicit.
  - `[medium]` `[patch]` Blind 2: optional TS5 tradeName cannot always populate mandatory local field — primary TS5 model confirms multiplicity [0..1]; proposal now requires refusal when absent, without inventing a fallback.
  - `[medium]` `[patch]` Blind 3: TS5 wire comparison omits closer service-isolated GET and 404 — primary section 3.1.2 confirms that endpoint; added it, signed/richer-body distinction and exact missing-selected-use handling.
  - `[medium]` `[patch]` Blind 4: local TTL incorrectly bounds total permission staleness — implementation inserts after source lookup/validation and has no source-age field; text now bounds only additional local retention and requests separate source/replication freshness guarantees.
  - `[medium]` `[patch]` Blind 5: future revokedAt expiry can outlive unchanged positive cache — TS5 IntendedUse permits future end dates; current six-field model omits them. Proposal now makes a validity-aware interface/cache revision or refusal prerequisite to live integration, without claiming runtime support.
  - `[medium]` `[patch]` Blind 6: wallet comparison lacks authenticated projection delivery — no such mechanism is supplied by current seam or ADR-005. Converted the migration promise into an explicit unresolved authenticated-delivery design dependency, without adding wallet online lookup.
  - `[medium]` `[patch]` Blind 7: same-RP/service comparison conflates several intended uses — pinned RPRC_09 issues certificates per intended-use/service pair. Comparison now binds the selected intendedUseIdentifier and explicitly requires profile/interface revision because six fields omit it; no union of purposes.

All seven independent documentation corrections were applied by the original implementation agent and checked against source by the parent. No deferrals. The numeric first-pass threshold suggests another review, but no specific unverified risk remains in these source-checked wording corrections; followup_review_recommended remains false under the workflow’s risk requirement. Live authority integration gaps are disclosed design inputs, not claims implemented by this story.

## Design Notes

Documentation is the outer deliverable. Reuse implementation controls instead of brittle prose tests. Six fields are a local age-predicate projection; TS5 and registration/access certificates are distinct surfaces. Future authority adapters must map validated data without widening permission.

## Verification

- `make test-go-fast` — standalone register race and fast Go suites pass.
- `make test-go-zk` — linked register HTTP/redirect/DCAPI controls pass.
- `make test-rp-kit` — default refusal and explicit synthetic mock positives pass.
- `git diff --check` — clean.
- Inspect proposal against source/tests; resolve local Markdown links and full submodule revision; check six fields, named errors/statuses, cache bounds and indexes. Confirm public publication/human review remain unclaimed and staged files exclude sprint-status.yaml and demo-data.

### Implementation evidence — 13 November 2026

- `make test-go-fast` — PASS, including the standalone register race suite (Go success cache used).
- `make test-go-zk` — PASS: root package 54.632s, zk package cached; linked HTTP/redirect/DCAPI and race controls execute in the root suite.
- `make test-rp-kit` — PASS, all eight reported process/profile groups, including shipped unconfigured refusal and explicitly enabled synthetic positives.
- ARF checkout and canonical HEAD gitlink both resolve `6373eee10b6e80225c7ce706a5ff1775fb799b22`. Pinned TS5/TS6 files are pointer pages, not model/version evidence. Their public standards repository was resolved and primary Markdown read at `cfc383e44294d65cfa6cccbbff6c927f0df91f9a`: TS5 1.6.1 dated 10 November 2026; TS6 1.2.2 dated 20 August 2026. Proposal records raw-byte SHA-256 values and reproduction.
- Proposal documents the five named refusal errors/statuses, exact entity/service lookup, strict six fields, doctype-qualified claims, strict age < TTL and cache outage/stale precedence; code/source inspection resolved all eight `presenter.registerGate` callers (graph name matches confirmed against source).
- Public publication and D3 Estonian service-owner usability review remain unperformed; no URL, named reviewer or verdict is claimed. The proposal supplies imperative publication/review handoff steps. Parent review and terminal finalization remain pending in this in-progress snapshot.
- Local Markdown destination check — PASS (108 destinations across proposal/navigation/spec; checked fragments resolve). All 17 named Go controls in the proposal resolve to actual test functions. `git diff --check` — PASS; working-tree changes are limited to the proposal, its navigation, spec §23 and this story record. No sprint-status.yaml or demo-data edits.

### Parent verification and review repairs — 13 November 2026

- Parent independently ran all three make targets successfully before review; source/Markdown inspection covered the complete staged baseline diff, not just the implementation report.
- Explicit uncached `go test -race -count=1 -v ./rpregister/...` — PASS (1.120s); all strict corpus/model/provider/cache/body-deadline/concurrency controls ran.
- Explicit uncached `EE_BENCH_SKIP=1 go test -race -count=1 -v -run '^TestRegister' .` — PASS (13.882s), including fresh/stale changes, both HTTP paths, stored policy and expiry-during-lookup controls.
- Review repairs change proposal prose only. The original agent checked all 32 proposal-local Markdown destinations and scoped whitespace; parent reread the repair diff and primary CSV/TS5 sections. Full post-repair make-target results are recorded in Auto Run Result.

## Auto Run Result

Status: awaiting-operator

Implemented the authority-facing RP-register proposal with one strict six-field
model, named fail-closed semantics, published local cache bounds, reproducible
synthetic controls, a pinned ARF/TS5/TS6 compliance-and-gap note and a concrete
publication/service-owner review handoff. All agent-executable work is complete.
Public publication and a human usability verdict have not been performed or
claimed. The nonempty frontmatter operator_actions list records those obligations.

Implementation commit: `7515a9631c9efa8f1d686cb1edf4aa394ae831ae`.
This commit was created before setting the terminal operator handoff; the result
record itself is committed separately. No push or external message was sent.

Files changed:
- `rp-kit/REGISTER-PROPOSAL.md` — proposal, standards mapping/gaps, controls, source pins and handoff.
- `rp-kit/REGISTER-CONTRACT.md` — link to authority-facing proposal.
- `rp-kit/README.md` — link from runnable service profile.
- `docs/README.md` — index the proposal.
- `spec/EE-EUDIW-TS-1.0.md` — section 23 item 40 collects current authority uncertainties.
- This story spec — immutable intent, executed checks, per-finding triage and terminal operator actions.

Review: all four layers completed. Seven medium documentation patches applied;
zero high/low patches and zero deferrals. Intent alignment’s intermediate-state
observation was rejected as a defect because the review precedes finalization;
this result now supplies the requested machine-readable handoff. Blind finding
1’s empty-RPA_02a absence subclaim was rejected against the actual pinned CSV,
while its missing-RPA_06a component was fixed. Every finding and evidence appears
in Review Triage Log. Verification-gap and edge-case layers reported no findings.

Follow-up review recommended: false. Patched entry counts: high 0, medium 7,
low 0. The first-pass numeric threshold was met, but no specific unverified
residual risk remains in the source-checked documentation corrections, so the
workflow’s specific-risk condition leaves the recommendation false.

Verification:
- `make test-go-fast` — PASS before review and after patches.
- `make test-go-zk` — PASS before review and after patches.
- `make test-rp-kit` — PASS before review and after patches, all eight process groups.
- Uncached register unit/race suite — PASS (1.120s).
- Uncached linked `TestRegister` HTTP/race suite — PASS (13.882s).
- Reviewed full baseline diff and repair diff against Go, pinned CSV and TS5/TS6 primary text; both external primary text hashes match the proposal’s source record.
- Local Markdown destinations, proposal fragments and all 17 referenced Go test names resolve; parent final destination scan passed for 107 file destinations.
- Complete YAML frontmatter parses; intent-contract unchanged; `git diff --check` passes.
- No runtime behavior changes; sprint-status.yaml and demo-data are absent from the changes and staging.

Residual limitations: current RIA API/trust/certificate/CT arrangements remain
explicitly [UNVERIFIED] in spec section 23. Full authority integration requires
the disclosed validity-aware cache, selected-intended-use and authenticated
projection designs. Those gaps are proposals for later integration, not
implemented claims. They do not prevent publishing this useful-even-if-superseded
proposal. Only actual publication and the D3 human service-owner usability
assessment remain owed under this story.
