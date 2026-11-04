---
story_id: "7.4"
story_key: 7-4-evidence-scoped-compliance-and-estimate-vocabulary
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 7417b983049702a1790cbce9ab45a19fe4f47705f2779b32b904a669b5772344
---

# Story 7.4: Evidence-scoped compliance and estimate vocabulary

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.4](../../planning-artifacts/epics.md#story-74-evidence-scoped-compliance-and-estimate-vocabulary).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.4: Evidence-scoped compliance and estimate vocabulary

As someone showing this repository to a programme,
I want "conformant" and "certified" removed and every estimate to carry its method,
So that the only sentence the repository makes is "these requirements are supported by these tests, in this environment".

**Acceptance Criteria:**

- **Given** the repository's documents and artefacts,
- **When** the vocabulary pass is complete,
- **Then** no document claims conformance or certification,
- **And** the supported-by-evidence formulation is used instead,
- **And** every effort or cost range carries the method and assumptions that produced it.

**Covers:** FR33. **Source:** R3; L3; A11; §4 certification reserved; NFR1; NFR7; risk R15.

## Analysis and implementation boundaries

Inventory claims in project-authored prose, generated report templates and reader surfaces. Scope self-claims to tests/environment and carry the carrier labels. Preserve quoted normative terms, third-party certification descriptions and imported source text with attribution rather than indiscriminately deleting every matching word.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/README.md](../../../docs/README.md)
- [README.md](../../../README.md)
- [PRODUCT.md](../../../PRODUCT.md)
- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)
- [docs/decisions/ADR-008-CARRIER-PATH-PLACEMENT.md](../../../docs/decisions/ADR-008-CARRIER-PATH-PLACEMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/planning/EVIDENCE-VOCABULARY-REVIEW.md`

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

Review each hit for speaker and claim scope; retain an explicit quotation/source exception policy if automating the check. Estimates keep method/assumptions and no new compliance claim is introduced by replacement wording.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
