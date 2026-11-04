---
story_id: "12.4"
story_key: 12-4-proximity-share-carries-consent
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["12.1", "12.2", "12.3"]
decision_gate: null
external_gate: null
source_contract_sha256: 596a2eb8a35355d667815700ce817c32ff6ad178c05be4c0f3b5a27cb992cd8c
---

# Story 12.4: Proximity share carries consent

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 12.4](../../planning-artifacts/epics.md#story-124-proximity-share-carries-consent).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 12.4: Proximity share carries consent

As a holder presenting in proximity,
I want to consent explicitly before anything is emitted,
So that the proximity path is not a way around the gate the remote path applies.

**Acceptance Criteria:**

- **Given** the proximity presentation path,
- **When** a share is requested,
- **Then** it passes the consent gate before a document is emitted, where today it emits with no gate at all,
- **And** the screen states what is shared and with whom on the same terms as the remote path,
- **And** a test asserts the gate is present on the proximity path,
- **And** the relation to Story 4.7 is stated: this story gates the share; Story 4.7 decides whether a ZKP-capable device answers at all.

**Covers:** FR70. **Source:** PRD §4.7 FR-48.

## Analysis and implementation boundaries

Proximity sharing already requests a PIN before sendResponse. Verify gate coverage/reuse boundaries, align requester/field/tier/linkability notices with remote consent and preserve the separate 4.7 eligibility decision. Do not add another PIN prompt just to match stale backlog wording.

**Prerequisites for full acceptance:** [Story 12.1](12-1-requester-name-authenticated-at-consent.md), [Story 12.2](12-2-credential-keys-require-user-presence.md), [Story 12.3](12-3-consent-gate-resists-guessing.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt)
- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityScreen.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityScreen.kt)
- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/presentation/PresentationRequestScreen.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/presentation/PresentationRequestScreen.kt)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/planning/CONSENT-INTEGRITY-ACCEPTANCE.md`

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

Observe zero emitted document before consent, cancel/failure handling, renewed consent for a new session or changed requested fields, and platform authentication before key use. Check remote/proximity content parity and accessible localized states without merging privacy policy into authentication.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
