---
story_id: "7.1"
story_key: 7-1-fix-the-stale-issuerauth-conformity-row
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 26e6dae3536bd7e7c944f2e2dfda04d24ee4bd24eeabd79b7cf77358cada43d5
---

# Story 7.1: Fix the stale issuerAuth conformity row

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.1](../../planning-artifacts/epics.md#story-71-fix-the-stale-issuerauth-conformity-row).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.1: Fix the stale issuerAuth conformity row

As a technical evaluator reading `docs/CONFORMITY.md`,
I want the row that records on-device `issuerAuth` validation as absent corrected,
So that the note does not contradict the code it describes.

**Acceptance Criteria:**

- **Given** `docs/CONFORMITY.md` records on-device `issuerAuth` validation as "absent", while
`CredentialToDocumentMapper.kt` performs the leaf-signature and PKIX-anchoring check with tests
for the anchored and unanchored cases,
- **When** the row is corrected,
- **Then** the row describes the verified behaviour and cites both sides by `path:line`,
- **And** the correction keeps "documented" and "verified in code" as separate statements,
- **And** the change is recorded in `docs/README.md`'s index if the document set changes.

**Covers:** FR31. **Source:** Report 1 §2 divergence; R3; risk R12.

## Analysis and implementation boundaries

Re-read the row and mapper before editing: the backlog's claim that the row is stale may itself be stale. Record the observed leaf-signature/PKIX behavior, anchored and unanchored controls and precise source revision.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)
- [app/src/main/kotlin/ee/cyber/wallet/domain/documents/CredentialToDocumentMapper.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/documents/CredentialToDocumentMapper.kt)
- [app/src/test/kotlin/ee/cyber/wallet/domain/documents/CredentialToDocumentMapperMdocVerificationTest.kt](../../../app/src/test/kotlin/ee/cyber/wallet/domain/documents/CredentialToDocumentMapperMdocVerificationTest.kt)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/planning/CONFORMITY-ROW-AUDIT.md`

These are planned destinations, not files created by this story-preparation pass.
Reuse a destination already introduced by a prerequisite instead of duplicating it.
Index delivered documents in `docs/README.md` and new checks in `tests/README.md`.

## Tasks for later execution

- [ ] Recheck the current entry points and source contract; record any changed premise,
  resolve the listed decisions, and confirm prerequisite and external-evidence scope.
- [ ] Deliver the behavior or analysis described above in the proposed destinations,
  preserving the shared trust, disclosure, versioning and scope constraints.
- [ ] Verify the targeted cases below, map retained evidence to every backlog acceptance
  criterion, and run the applicable shared checks from [README.md](README.md).
- [ ] Record observed outcomes, failures and limitations with revision/environment;
  leave unavailable operator evidence pending and update lifecycle tracking only then.

## Targeted verification plan

Cite current path:line references for both implementation and tests; if the row is already accurate, record confirmation rather than manufacturing a correction. Do not promote code inspection into fresh executed-test evidence.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
