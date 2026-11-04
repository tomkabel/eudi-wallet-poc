---
story_id: "3.6"
story_key: 3-6-refusal-distinguishable-from-cancellation
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 23909e0fc76c79ffc5d62c45591f11dc1d00a643810f9231919bf5778426ba06
---

# Story 3.6: Refusal distinguishable from cancellation

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.6](../../planning-artifacts/epics.md#story-36-refusal-distinguishable-from-cancellation).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.6: Refusal distinguishable from cancellation

As a holder whose request was refused,
I want the refusal to be distinguishable from my own cancellation,
So that I can tell a policy decision apart from my own action.

**Acceptance Criteria:**

- **Given** a refusing policy path and a user cancellation,
- **When** both are presented in the UI contract,
- **Then** the two outcomes are distinguishable in the interface contract,
- **And** a test asserts the distinction on the fixed dispatch path.
- **And** a third interface state exists for a technical failure, a timeout or an aborted session,
so an infrastructure failure is never shown to the holder as a policy refusal or as their own
cancellation,
**Covers:** FR14. **Source:** O2 acceptance criteria (ii); U5 acceptance criteria; S4 current capability note.

## Analysis and implementation boundaries

Define policy refusal, user cancellation and technical failure as distinct typed outcomes at the existing dispatch/UI boundaries. Include timeout and session-abort causes; preserve real refusal and cancellation intent through the sandbox's rendered state.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/presentation/PresentationRequestViewModel.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/presentation/PresentationRequestViewModel.kt)
- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt)
- [sandbox/static/app.js](../../../sandbox/static/app.js)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/PRESENTATION-OUTCOMES.md`

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

Drive one actual path of each outcome and verify UI text, log reason and machine state agree. Check visible/announced sandbox cancellation after its step is locked, and keep retry only on retryable failures.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
