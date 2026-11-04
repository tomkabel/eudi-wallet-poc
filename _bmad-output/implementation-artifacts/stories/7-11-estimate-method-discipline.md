---
story_id: "7.11"
story_key: 7-11-estimate-method-discipline
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 24c8a66f6cc0a2b3238ab9641d544b0e45347bf50cf1d0abf1705a38d1e75233
---

# Story 7.11: Estimate method discipline

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.11](../../planning-artifacts/epics.md#story-711-estimate-method-discipline).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.11: Estimate method discipline

As a recipient of this repository's effort figures,
I want the method and assumptions attached to every effort or cost figure this repository
publishes, including the 6–12 engineer-month re-creation range (Report 2 Q1) that is human cost
rather than agent-assisted effort,
So that a range is never read as a commitment.

**Acceptance Criteria:**

- **Given** every published figure of effort or cost, whether derived from agent-assisted work or
from human re-creation estimates (Report 2 Q1's 6–12 engineer-months carries its own baseline,
method and confidence),
- **When** any estimate is published,
- **Then** the method, the comparable slice it was derived from and the assumption set accompany
it,
- **And** no estimate appears in a schedule-shaped form,
- **And** the agent-assisted baseline is stated so a conventional team can size its own effort.

**Covers:** FR33. **Source:** risk R15 mitigation; Report 1 §6 ("effort estimates are one-person, agent-assisted baselines").

## Analysis and implementation boundaries

Inventory effort/cost figures and separate human recreation cost, agent-assisted history and future estimates. Preserve source ranges as attributed estimates with method/confidence; do not convert them to deadlines or assign a conventional team's productivity from this fork's pace.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/research/2026-10-23-innovation-assessment.md](../../../docs/research/2026-10-23-innovation-assessment.md)
- [docs/research/2026-10-23-estonia-alignment-assessment.md](../../../docs/research/2026-10-23-estonia-alignment-assessment.md)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/planning/ESTIMATE-METHODS.md`

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

Every range needs unit, baseline slice, assumptions and external-wait treatment. Unsupported methods remain UNVERIFIED or explicitly unavailable; no invented precision is used to complete a table.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
