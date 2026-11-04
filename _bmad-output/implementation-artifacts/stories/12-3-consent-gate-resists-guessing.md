---
story_id: "12.3"
story_key: 12-3-consent-gate-resists-guessing
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G15 escalation/recovery copy remains open; preserve current five-attempt/30-minute policy unless deliberately revised."
external_gate: "No external hardware is needed for persistence tests; real restart/UI evidence needs an Android runtime."
source_contract_sha256: 4e8412f93d5e98bd3da542f54eb972c7d6cfd86b96a9d79a27081aa00c71ab7a
---

# Story 12.3: Consent gate resists guessing

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 12.3](../../planning-artifacts/epics.md#story-123-consent-gate-resists-guessing).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 12.3: Consent gate resists guessing

As a holder,
I want the consent gate to be unguessable rather than a speed bump,
So that physical access to an unlocked device is not enough to approve a presentation.

**Acceptance Criteria:**

- **Given** the consent gate and its attempt counter,
- **When** attempts fail,
- **Then** failed attempts are counted in persistent state that survives process restart, with a lockout threshold and an escalation path stated in the artefact that publishes it,
- **And** relaunching the application does not reset the count,
- **And** the existing behaviour, where the counter resets on relaunch and the check is a simulated delay, is removed rather than documented,
- **And** a test asserts the count survives a restart.

**Covers:** FR69. **Source:** PRD §4.7 FR-47.

## Analysis and implementation boundaries

Persistent Argon2id verification and atomic attempt updates already exist. Fix the UI's separate attemptsLeft counter and simulated delay; expose actual persisted remaining attempts/lock deadline rather than implementing a second lockout store. Name recovery/escalation policy before adding a destructive action.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G15 escalation/recovery copy remains open; preserve current five-attempt/30-minute policy unless deliberately revised.

**External acceptance boundary:** No external hardware is needed for persistence tests; real restart/UI evidence needs an Android runtime.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/security/PinVerifier.kt](../../../app/src/main/kotlin/ee/cyber/wallet/security/PinVerifier.kt)
- [app/src/main/kotlin/ee/cyber/wallet/data/datastore/UserSessionDataSource.kt](../../../app/src/main/kotlin/ee/cyber/wallet/data/datastore/UserSessionDataSource.kt)
- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/pin/PinEntryViewModel.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/pin/PinEntryViewModel.kt)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/security/PIN-LOCKOUT-POLICY.md`

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

Test failed attempts across DataStore recreation/process relaunch, correct PIN while locked, expiry, concurrent submissions and failed storage update. The lock state must not display merely incorrect PIN; record actual policy and localized remaining-count/lockout copy.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
