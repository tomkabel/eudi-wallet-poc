---
story_id: "10.3"
story_key: 10-3-real-wallet-unit-attestation-issuance
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 pin TS3 and actual WUA format/security evidence."
external_gate: "Wallet-provider service capable of real issuance and trust validation."
source_contract_sha256: 6853d70922f0d25c900281f1d9443fcc9a51ffb36ca82746e6d7bb695bcf9146
---

# Story 10.3: Real Wallet Unit Attestation issuance

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 10.3](../../planning-artifacts/epics.md#story-103-real-wallet-unit-attestation-issuance).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 10.3: Real Wallet Unit Attestation issuance

As a wallet provider,
I want real WUA issuance per TS3 rather than a mock,
So that the wallet-unit obligation is met by an implementation instead of a written requirement.

**Acceptance Criteria:**

- **Given** `EE-WUA-007`/`EE-WUA-008` are written and the WUA paths are mock-only,
- **When** real issuance is implemented,
- **Then** an interop test runs against a wallet-provider service,
- **And** the mock remains for offline work,
- **And** the requirements matrix's WUA row is updated from its evidence.
- **And** a WUA that is issued but cannot be validated, and a wallet-provider service that is
unavailable, are both outcomes of the story rather than a test that checks issuance alone,
**Blocked by (external):** a wallet-provider service that issues a real WUA. Where the external
party or hardware is unavailable, the story still closes its in-repository half and records the
state as blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR57. **Source:** S9 required change and verification method; `EE-WUA-007`/`008`.

## Analysis and implementation boundaries

Separate real WUA issuance from the existing local AVSUA-equivalent bundle. Preserve offline providers, define trusted provider configuration and enforce audience/key/challenge/freshness and privacy requirements at the consumer boundary.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 pin TS3 and actual WUA format/security evidence.

**External acceptance boundary:** Wallet-provider service capable of real issuance and trust validation.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/](../../../app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/)
- [docs/profiles/AVSUA-ISSUANCE.md](../../../docs/profiles/AVSUA-ISSUANCE.md)
- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/interop/REAL-WUA.md`
- `tests/interop/wua/`

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

Test real issued-and-validated WUA, invalid signature/trust/binding, expired evidence and provider unavailable. A successfully received but unvalidated WUA is a failure, not a completed obligation.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
