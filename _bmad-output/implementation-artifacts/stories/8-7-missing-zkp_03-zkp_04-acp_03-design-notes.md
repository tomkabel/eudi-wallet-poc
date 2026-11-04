---
story_id: "8.7"
story_key: 8-7-missing-zkp_03-zkp_04-acp_03-design-notes
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["7.8"]
decision_gate: null
external_gate: null
source_contract_sha256: 504adb7ccb698190de0d7e6a27995c65099ea528672818fc146a29762df31fbb
---

# Story 8.7: Missing ZKP_03 ZKP_04 ACP_03 design notes

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.7](../../planning-artifacts/epics.md#story-87-missing-zkp_03-zkp_04-acp_03-design-notes).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.7: Missing ZKP_03 ZKP_04 ACP_03 design notes

As a reader of the requirements matrix,
I want short design notes recording `ZKP_03`, `ZKP_04` and `ACP_03` as out of scope with what each would require,
So that the missing rows are explained rather than merely marked missing.

**Acceptance Criteria:**

- **Given** A6 (`ZKP_03` binding of an attestation to a PID), A7 (`ZKP_04` pseudonym derivation)
and A16 (`ACP_03`) are marked missing,
- **When** the notes are written,
- **Then** each note states what a scheme for it would require and why this repository does not
implement one,
- **And** `ACP_03`'s SHOULD-use-ZKP wording is quoted,
- **And** no note presents the gap as planned work.

**Covers:** FR45. **Source:** A6, A7, A16; A6 tradeoff 1.

## Analysis and implementation boundaries

Write three short notes linking to the common scope statement. Quote ACP_03 from the pinned ARF source, describe necessary mechanisms and explain the current exclusion without silently adding an implementation roadmap.

**Prerequisites for full acceptance:** [Story 7.8](7-8-narrow-scope-statement-for-the-zkp-work.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)
- [eudi-arf/docs/](../../../eudi-arf/docs/)
- [docs/analysis/ZKP08-ANALYSIS.md](../../../docs/analysis/ZKP08-ANALYSIS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/ZKP03-BINDING.md`
- `docs/analysis/ZKP04-PSEUDONYMS.md`
- `docs/analysis/ACP03-SCOPE.md`

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

Check each requirement id and normative strength at its primary source; retain unknowns rather than fabricating a scheme design. Verify the notes consistently describe absent capabilities as absent.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
