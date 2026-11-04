---
story_id: "9.1"
story_key: 9-1-statutory-threshold-to-circuit-map
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G12 Epic 9 is deferred; beyond-18 legal need is unverified and conflicts with the epic introduction."
external_gate: "Primary legislation and later qualified legal review in 9.6."
source_contract_sha256: 6ba0082a2bb6fc615794384b36cb54eef004ae76716d96b1e047a06d69ecdbf7
---

# Story 9.1: Statutory threshold-to-circuit map

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 9.1](../../planning-artifacts/epics.md#story-91-statutory-threshold-to-circuit-map).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 9.1: Statutory threshold-to-circuit map

As an attestation provider in a Member State with several age thresholds,
I want a documented threshold-to-circuit map with legal citations,
So that the thresholds are traceable to law rather than to memory.

**Acceptance Criteria:**

- **Given** the Estonian statutory ages and the fork's existing rules,
- **When** the map is published,
- **Then** each threshold carries its legal citation,
- **And** any threshold that cannot be mapped is treated as unsupported rather than guessed,
- **And** the map states its version so a statutory change is a visible revision.

**Covers:** FR49. **Source:** O6 proposed capability; E3; O6 risks (legal mapping errors are consequential).

## Analysis and implementation boundaries

Draft a versioned legal-to-predicate map that distinguishes an age gate from an identification duty. Do not copy the epic's asserted 16/21 thresholds as law: PRD NG-10 rejects that premise and the spec excludes gambling from anonymous age proof.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G12 Epic 9 is deferred; beyond-18 legal need is unverified and conflicts with the epic introduction.

**External acceptance boundary:** Primary legislation and later qualified legal review in 9.6.

## Existing entry points

- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)
- [docs/analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md](../../../docs/analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/STATUTORY-AGE-THRESHOLDS.md`

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

Each candidate threshold has exact provision, jurisdiction, effective date, interpretation and supported/unsupported state. Missing primary support remains UNVERIFIED and unsupported; no circuit is enabled by a research suggestion alone.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
