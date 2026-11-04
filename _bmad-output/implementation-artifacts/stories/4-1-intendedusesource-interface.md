---
story_id: "4.1"
story_key: 4-1-intendedusesource-interface
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 2843013343496f2eb4e9358ca75c45f972e24d3480e6ff2108e5d074684cd117
---

# Story 4.1: IntendedUseSource interface

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 4.1](../../planning-artifacts/epics.md#story-41-intendedusesource-interface).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 4.1: IntendedUseSource interface

As the maintainer of the age-predicate policy,
I want the intended-use decision behind an interface before any certificate parsing exists,
So that the Estonian register or an EU registry can be plugged in without redesign.

**Acceptance Criteria:**

- **Given** the existing `AgePredicatePolicy` behaviour,
- **When** `IntendedUseSource` is introduced,
- **Then** the policy consumes the interface and its absence yields refusal,
- **And** the interface's data model is recorded as an ADR before the certificate implementation
starts,
- **And** no existing unit test regresses.
- **And** a source that is present but throws or times out refuses under the same fail-closed
default with `source-error` logged, distinct from the interface being absent,
**Covers:** FR16. **Source:** O3 implementation approach ("interface first"); A3 P2 deliverable.

## Analysis and implementation boundaries

Use ADR-005's sealed result and typed doctype-qualified vocabulary. Scope the intended-use check across the whole request; preserve the existing single-predicate behavior and false-withholding rule. No-source refusal applies where registered intended use is needed to widen disclosure, not an accidental ban on every single-predicate request.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt)
- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/IntendedUseSource.kt`

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

Before parsing exists, prove absent source, exception, timeout and empty intended use cannot authorize wider disclosure. Keep absent versus source-error distinguishable and test a cross-credential two-predicate request.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
