---
story_id: "10.2"
story_key: 10-2-batch-issuance
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["8.4"]
decision_gate: null
external_gate: "Real provider batch behavior is separate from mock-only coverage."
source_contract_sha256: e036ebada2fdfe0d7160a14babc11cd46a509c7e160093151343a327421e0832
---

# Story 10.2: Batch issuance

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 10.2](../../planning-artifacts/epics.md#story-102-batch-issuance).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 10.2: Batch issuance

As a holder who does not want presentations linkable,
I want batch issuance for `ee.riik.poa.1` and the AV attestation,
So that the recorded data-minimisation gap is closed.

**Acceptance Criteria:**

- **Given** batch issuance for `ee.riik.poa.1` is a recorded gap,
- **When** it is implemented,
- **Then** multiple credentials are issued for one issuance event,
- **And** a fixture-based disclosure test documents what each path reveals per presentation,
- **And** the unlinkability statement of Story 8.4 is updated to cover the batched path.
- **And** a batch issued partially — fewer credentials than requested, or a mid-batch failure — is
detected and reported, because a partial batch silently reduces the unlinkability the story
claims,
**Covers:** FR56. **Source:** A2 required change and verification method; S2; L4.

## Analysis and implementation boundaries

Inventory the existing WalletProviderBatchAgeIssuer before changing it: Epic 1 already added key/row rollback coverage. Complete any missing per-doctype issuance, consumption and presentation-disclosure behavior rather than duplicating the batch loop.

**Prerequisites for full acceptance:** [Story 8.4](8-4-per-path-unlinkability-statement.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** Real provider batch behavior is separate from mock-only coverage.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/provider/](../../../app/src/main/kotlin/ee/cyber/wallet/domain/provider/)
- [app/src/test/kotlin/ee/cyber/wallet/domain/provider/wallet/](../../../app/src/test/kotlin/ee/cyber/wallet/domain/provider/wallet/)
- [docs/profiles/AVSUA-ISSUANCE.md](../../../docs/profiles/AVSUA-ISSUANCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/BATCH-AGE-ISSUANCE.md`

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

Assert requested/issued counts, independent credential keys, partial mint refusal, evidence failure, cancellation cleanup and disclosure residue across reused versus fresh credentials. Update 8.4 with limits; multiple credentials alone do not prove unlinkability.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
