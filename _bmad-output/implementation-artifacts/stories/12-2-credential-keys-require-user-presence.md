---
story_id: "12.2"
story_key: 12-2-credential-keys-require-user-presence
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G15 audit current key creation/use paths; native proof export must not bypass operation-bound presence."
external_gate: "Physical device supporting platform user authentication and appropriate hardware/key capabilities."
source_contract_sha256: cfad8f440f957b6e81d967522b246af434b67a56dbaffbb29853eca670f90b43
---

# Story 12.2: Credential keys require user presence

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 12.2](../../planning-artifacts/epics.md#story-122-credential-keys-require-user-presence).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 12.2: Credential keys require user presence

As a holder whose device is unlocked and in someone else's hands,
I want my credential keys usable only after I authenticate,
So that a proof cannot be produced without me.

**Acceptance Criteria:**

- **Given** the platform's user-authentication capability and the wallet's key operations,
- **When** a key is used,
- **Then** the operation depends on a platform user-authentication result rather than on an application-level flag alone,
- **And** proof generation, plain presentation and signing each carry the gate, or the ungated path is removed rather than left reachable,
- **And** a test asserts that a key operation attempted without user authentication fails,
- **And** a device without the capability is refused rather than silently downgraded, recorded as blocked if hardware is absent.

**Blocked by (external):** a device that supports platform user authentication. Where it is
unavailable, the story closes its in-repository half and records the state as blocked with the
reason — never as passed.

**Covers:** FR68. **Source:** PRD §4.7 FR-46.

## Analysis and implementation boundaries

Inventory actual key creation, migration, export, signing and proof paths before changing them: SecureAreaKeyManager already configures authentication and PromptDialogHost carries a CryptoObject. Prove per-operation enforcement and remove/deny any ungated path instead of treating an app PIN flag as sufficient.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G15 audit current key creation/use paths; native proof export must not bypass operation-bound presence.

**External acceptance boundary:** Physical device supporting platform user authentication and appropriate hardware/key capabilities.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaKeyManager.kt](../../../app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaKeyManager.kt)
- [app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaCOSECryptoProvider.kt](../../../app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaCOSECryptoProvider.kt)
- [app/src/main/kotlin/ee/cyber/wallet/ui/prompt/PromptDialogHost.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/prompt/PromptDialogHost.kt)
- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkPresenter.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkPresenter.kt)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/security/CREDENTIAL-KEY-USER-PRESENCE.md`

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

Attempt real signing/plain/proof operations without authentication, after prompt cancellation, with stale authentication and after process restart. Hardware absent means pending external evidence, not a JVM fake passing as keystore enforcement.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
