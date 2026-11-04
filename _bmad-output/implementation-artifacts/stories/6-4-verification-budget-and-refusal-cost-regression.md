---
story_id: "6.4"
story_key: 6-4-verification-budget-and-refusal-cost-regression
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["6.2"]
decision_gate: "G8 calibrate a numeric refusal bound from measured target hardware rather than inventing one."
external_gate: "Target load environment for the measured regression limit."
source_contract_sha256: efbeb1f9bc91375214de9b2080b78b047d1216b424c0f3a2cae03b6c2be12415
---

# Story 6.4: Verification budget and refusal-cost regression

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 6.4](../../planning-artifacts/epics.md#story-64-verification-budget-and-refusal-cost-regression).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 6.4: Verification budget and refusal-cost regression

As a verifier operator,
I want the refusal cost pinned by a regression test and the admission control tuned on measured figures,
So that the 54–58 s refusal defect cannot return unnoticed.

**Acceptance Criteria:**

- **Given** the 24 September 2026 fix for the 54–58 s refusal defect,
- **When** a refusal-cost regression test runs,
- **Then** the refusal cost is pinned to a stated numeric bound (a p95 measured on the same
percentile harness the capacity figures come from) and asserted on every run, failing the run when
the bound is exceeded,
- **And** admission control is tuned to the measured capacity rather than to a guess,
- **And** the previously rejected host-side optimisations (Rust PGO, FFI scratch arena, Go PGO)
are not re-attempted without a new profile showing a changed cost distribution.
- **And** the bound's numeric value and the harness that produced it are published in
`docs/MEASUREMENTS.md`, so the regression test fails on a number a reader can find,
**Covers:** FR29. **Source:** M3 deliverable and exit criteria; Report 1 §1 finding 4; `docs/MEASUREMENTS.md` optimization log.

## Analysis and implementation boundaries

Use the actual refusal path and same percentile method as capacity work; distinguish pre-verification policy refusals from invalid-proof verification cost. Publish the numeric bound, calibration host and tolerances before pinning a regression gate.

**Prerequisites for full acceptance:** [Story 6.2](6-2-verification-capacity-on-target-hardware.md)

**Decision/evidence gate:** G8 calibrate a numeric refusal bound from measured target hardware rather than inventing one.

**External acceptance boundary:** Target load environment for the measured regression limit.

## Existing entry points

- [verifier/go/zk/](../../../verifier/go/zk/)
- [tests/e2e.sh](../../../tests/e2e.sh)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `tests/refusal_cost_regression.sh`

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

Assert the bound and verify a deliberately slowed refusal fails it. Exercise admission shedding before session claim; exclude queued/shed requests from successful-verification latency while reporting their counts separately.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
