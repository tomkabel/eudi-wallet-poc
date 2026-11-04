---
story_id: "8.4"
story_key: 8-4-per-path-unlinkability-statement
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 4222e93168c75647c27ae8d7069ba0b6cb9e008c73aac4debc556e6007e70aa7
---

# Story 8.4: Per-path unlinkability statement

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.4](../../planning-artifacts/epics.md#story-84-per-path-unlinkability-statement).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.4: Per-path unlinkability statement

As a privacy engineer,
I want an explicit unlinkability statement per path backed by a test asserting that no tracking
identifier is added,
So that `ZKP_07` is addressed with evidence rather than assertion.

**Acceptance Criteria:**

- **Given** one proof per presentation over the encrypted response and the documented `msoX5chain`
residue,
- **When** the statement and test are produced,
- **Then** an unlinkability statement exists per path (ZK and plain fallback),
- **And** a log/carrier inspection test asserts no tracking identifier is added,
- **And** the residue that remains is stated rather than omitted.

**Covers:** FR42. **Source:** A10 required change and verification method.

## Analysis and implementation boundaries

Inspect ZK and plain wire/log representations separately, including msoX5chain, credential reuse and request/session metadata. Define the tracking-identifier assertion narrowly enough to permit necessary ephemeral session correlation while exposing stable holder correlators.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md](../../../docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md)
- [wallet/present.py](../../../wallet/present.py)
- [verifier/go/oid4vp/](../../../verifier/go/oid4vp/)
- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkPresenter.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkPresenter.kt)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/PER-PATH-UNLINKABILITY.md`
- `tests/test_tracking_residue.py`

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

Compare multiple presentations and credentials, asserting no added stable holder identifier and reporting existing residue. A scan for one banned field is insufficient to claim unlinkability; state observer and path limits explicitly.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
