---
story_id: "9.5"
story_key: 9-5-refusal-with-a-user-visible-reason
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["9.3"]
decision_gate: "G13 resolve threshold substitution language consistently with nearest-true selection."
external_gate: "No external party is needed for refusal UI tests after policy is decided."
source_contract_sha256: 778c8080ae6f13bd982a8dc309d70b34a7fdb1017260b35a9835fb1b783b4eb1
---

# Story 9.5: Refusal with a user-visible reason

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 9.5](../../planning-artifacts/epics.md#story-95-refusal-with-a-user-visible-reason).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 9.5: Refusal with a user-visible reason

As a holder who does not hold the requested threshold,
I want a refusal with a reason, never a neighbouring threshold,
So that a threshold I am not entitled to is not silently answered with a different one.

**Acceptance Criteria:**

- **Given** a request for a threshold the wallet does not hold,
- **When** the presentation is attempted,
- **Then** it is refused with a user-visible reason,
- **And** no neighbouring threshold is transmitted in its place,
- **And** an assertion over all fixtures shows no `false` predicate is ever transmitted.

**Covers:** FR53. **Source:** O6 acceptance criteria (ii)–(iii); U1 acceptance criteria.

## Analysis and implementation boundaries

Wire negotiated refusals to a visible localized cause and assert emitted bytes, not only a decision object. The phrase no neighboring threshold cannot silently contradict a separately implemented nearest-true rule; record G13 first.

**Prerequisites for full acceptance:** [Story 9.3](9-3-nearest-true-above-negotiation-library.md)

**Decision/evidence gate:** G13 resolve threshold substitution language consistently with nearest-true selection.

**External acceptance boundary:** No external party is needed for refusal UI tests after policy is decided.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt)
- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/PresentationTier.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/PresentationTier.kt)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/UNSUPPORTED-THRESHOLD-POLICY.md`

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

Inspect all refusal fixtures for zero proof/plain fallback emission and no false predicate; distinguish unsupported threshold, unmet age and unavailable circuit without revealing extra holder attributes.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
