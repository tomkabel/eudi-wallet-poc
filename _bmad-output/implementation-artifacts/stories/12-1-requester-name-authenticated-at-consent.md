---
story_id: "12.1"
story_key: 12-1-requester-name-authenticated-at-consent
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G15 requester identity must come from both signature and chain trust; issuer-offer surfaces lack the same proof input."
external_gate: "Configured trusted reader roots are needed for a verified-reader runtime case."
source_contract_sha256: 7f17849c856f1b0e5226ce8054f23df304ebea6d14e275869c0811ea4e7c4d05
---

# Story 12.1: Requester name authenticated at consent

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 12.1](../../planning-artifacts/epics.md#story-121-requester-name-authenticated-at-consent).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 12.1: Requester name authenticated at consent

As a holder reading the consent screen,
I want the relying party named there to be the relying party that signed the request,
So that the screen's claim is true rather than a rendered string.

**Acceptance Criteria:**

- **Given** a request carrying `readerAuth` and a consent screen that renders a requester name,
- **When** the name is shown,
- **Then** the `readerAuth` chain is validated to a trust anchor before any name renders as verified,
- **And** an unvalidated common name is never rendered as verified, and when the chain does not validate the screen says the requester is unverified with refusal as the default,
- **And** a negative test presents a certificate carrying an attacker-chosen common name and asserts it is not rendered as verified,
- **And** the outcome is distinguishable from a retryable error and from user cancellation, per NFR13.

**Covers:** FR67. **Source:** PRD §4.7 FR-45; `docs/sota-audit-ROLLUP.md` consent-integrity
finding; `EE-PRO-025`.

## Analysis and implementation boundaries

Use the existing independent readerAuthenticated and readerChainTrusted results together before asserting verified identity. readerAuthSubject currently selects signature-authenticated requests alone; target that trust gap and all badge consumers rather than duplicating PKIX validation. Model loading/unverified/verified states so an attacker name cannot flash verified.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G15 requester identity must come from both signature and chain trust; issuer-offer surfaces lack the same proof input.

**External acceptance boundary:** Configured trusted reader roots are needed for a verified-reader runtime case.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt](../../../app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt)
- [app/src/main/kotlin/ee/cyber/wallet/ui/components/VerifiedParty.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/components/VerifiedParty.kt)
- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt)

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

Test attacker CN with untrusted valid signature, bad signature, absent roots, malformed chain and valid trusted chain on remote/proximity/DC API surfaces. Assert first render and no-match state, no unverified-as-verified badge and distinct refusal/cancel/error outcomes.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
