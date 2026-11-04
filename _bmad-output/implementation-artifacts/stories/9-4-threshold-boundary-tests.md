---
story_id: "9.4"
story_key: 9-4-threshold-boundary-tests
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["9.1", "9.2", "9.3"]
decision_gate: "G12 legal/scope and G13 negotiation semantics; no speculative enabling of thresholds."
external_gate: "Accepted circuits and real prover availability for proof-level boundary evidence."
source_contract_sha256: 1ecaafdfa1286586c9de22ef8703d9486fce06259346571f9d6634e83c3904ae
---

# Story 9.4: Threshold boundary tests

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 9.4](../../planning-artifacts/epics.md#story-94-threshold-boundary-tests).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 9.4: Threshold boundary tests

As a programme owner verifying a threshold claim,
I want table-driven boundary tests per threshold including the day before, the day of, a leap-day birth and an incomplete date,
So that the boundary cases are proven rather than assumed.

**Acceptance Criteria:**

- **Given** the mapped thresholds and the existing leap-day/incomplete-date analysis,
- **When** the boundary suite runs,
- **Then** for each threshold a proof verifies only for a holder whose age meets it,
- **And** the day before, the day of, a leap-day birth and an incomplete date are each covered,
- **And** the 28 February rule derived from ISO §7.2.5 is asserted explicitly,
- **And** the boundary set includes the day *after* the threshold turning point as well as the day
before, and an impossible or future date of birth is distinguished from an incomplete one,
- **And** the suite is added to `tests/README.md` and wired into `make check` or the matching CI job (or the story records why it is excluded and how it is run instead),

incomplete date, and neither is answered as a plain false predicate,
before, so an off-by-one in the comparison cannot pass the suite,
**Covers:** FR52. **Source:** O6 acceptance criteria (i); E3 verification method; spec Annex A.

## Analysis and implementation boundaries

Build table-driven calendar and proof-level cases separately. Pin authoritative leap-day/incomplete-date interpretation; malformed/future dates are invalid inputs, not false predicates. Preserve committed transcript bytes and run genuine proof verification for claims about proofs.

**Prerequisites for full acceptance:** [Story 9.1](9-1-statutory-threshold-to-circuit-map.md), [Story 9.2](9-2-accepted-circuit-set-per-threshold.md), [Story 9.3](9-3-nearest-true-above-negotiation-library.md)

**Decision/evidence gate:** G12 legal/scope and G13 negotiation semantics; no speculative enabling of thresholds.

**External acceptance boundary:** Accepted circuits and real prover availability for proof-level boundary evidence.

## Existing entry points

- [docs/analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md](../../../docs/analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md)
- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)
- [tests/e2e.sh](../../../tests/e2e.sh)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `tests/threshold_boundaries/`

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

Cover day before/on/after each approved threshold, leap-day birth, incomplete/impossible/future dates and century-leap boundaries. A policy-only test cannot satisfy the proof-verification acceptance criterion; record absent accepted circuits explicitly.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
