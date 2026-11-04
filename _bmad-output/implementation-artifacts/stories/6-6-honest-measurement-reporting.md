---
story_id: "6.6"
story_key: 6-6-honest-measurement-reporting
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 030274f9edfc52fdda384b2bb811b7bb80618c523816bca63204c4a47b82324a
---

# Story 6.6: Honest measurement reporting

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 6.6](../../planning-artifacts/epics.md#story-66-honest-measurement-reporting).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 6.6: Honest measurement reporting

As a reader of this repository's measurements,
I want every figure to carry its baseline, method, assumptions and confidence, with unmeasured rows saying so,
So that no estimate is read as a measurement.

**Acceptance Criteria:**

- **Given** the existing measurement prose and the new figures,
- **When** the reporting is reviewed,
- **Then** every figure states its baseline, method, assumptions and confidence,
- **And** no effort or cost range appears without its method,
- **And** unmeasured items are labelled explicitly rather than omitted.

**Covers:** FR26, FR27. **Source:** NFR2; NFR7; A5.1 measurements row; risk R15.

## Analysis and implementation boundaries

Define captions carrying baseline, method, raw-sample location, assumptions and confidence. Review historical and new figures without converting host results into device results; the template can be drafted before any hardware arrives.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)
- [docs/research/2026-10-23-shared-appendices.md](../../../docs/research/2026-10-23-shared-appendices.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/measurements/REPORTING-CONTRACT.md`

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

For every published numeric claim identify its source run or estimate method; enumerate pending rows and reasons. Record p99/sample limitations, failures and cohort exclusions explicitly.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
