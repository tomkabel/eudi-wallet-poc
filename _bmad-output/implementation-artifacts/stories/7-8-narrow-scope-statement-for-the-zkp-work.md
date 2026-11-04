---
story_id: "7.8"
story_key: 7-8-narrow-scope-statement-for-the-zkp-work
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: b02fca5946ae1d1fcd29c68d54477346be21c2c6a1808b252c6332de574f71e5
---

# Story 7.8: Narrow-scope statement for the ZKP work

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.8](../../planning-artifacts/epics.md#story-78-narrow-scope-statement-for-the-zkp-work).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.8: Narrow-scope statement for the ZKP work

As a programme owner deciding whether this work fits,
I want a written statement that the implementation covers a boolean age predicate on one attestation and that the general ARF ZKP vision is out of scope,
So that the deliberate narrowness is stated rather than discovered.

**Acceptance Criteria:**

- **Given** the ARF's general ZKP requirements (`ZKP_01` generic predicates, `ZKP_03` binding,
`ZKP_04` pseudonyms, `ACP_03`),
- **When** the scope statement is written,
- **Then** it names each of those requirements as out of scope with a reason,
- **And** it states that generality depends on schemes and specifications that do not exist yet,
- **And** it is phrased so that a narrow measured implementation is not presented as a general
one.

**Covers:** FR37. **Source:** A4 required change; A6, A7, A16; A6 tradeoff 1 ("stay narrow and say so").

## Analysis and implementation boundaries

Write the currently supported single-predicate/single-attestation claim and distinguish the broader multi-credential carrier capability. Name ZKP_01, ZKP_03, ZKP_04 and ACP_03 gaps; deferred Epic 4 exceptions are future scope until delivered.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)
- [PRODUCT.md](../../../PRODUCT.md)
- [docs/analysis/ZKP08-ANALYSIS.md](../../../docs/analysis/ZKP08-ANALYSIS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/ZKP-SCOPE.md`

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

Review every advertised surface against the scope statement, including carrier placement, 109-bit circuit limitation and missing device measurements. Describe unavailable schemes as the pinned evidence's finding, not a timeless claim about all schemes.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
