---
story_id: "11.1"
story_key: 11-1-operations-runbook
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.1"]
decision_gate: null
external_gate: null
source_contract_sha256: 86f76d1c6ca038db712dbf3da08700411ece3d6f25644009ac9b625fde3a38f8
---

# Story 11.1: Operations runbook

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.1](../../planning-artifacts/epics.md#story-111-operations-runbook).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.1: Operations runbook

As an operator running the verification service for a pilot,
I want an operations runbook,
So that routine operation is documented rather than improvised.

**Acceptance Criteria:**

- **Given** the deployed service profile from Epic 3,
- **When** the runbook is written,
- **Then** it covers start, stop, drain (SIGTERM with `-drain-timeout`, `503`/`Retry-After`,
`draining:true` on `/healthz`), upgrade and rollback,
- **And** it names the failure modes with a first diagnostic step each,
- **And** it is exercised in Story 11.5.
- **And** a drain whose `-drain-timeout` expires with requests still in flight has a stated
operator behaviour, so an unfinished drain is not improvised,
**Covers:** FR60. **Source:** A3 R1 deliverable; `tests/drain_test.sh` behaviour.

## Analysis and implementation boundaries

Document start/stop, drain, upgrade, response-key rotation and rollback over the actual service profile. Include persisted-key permissions, session/provenance loss on restart and unfinished in-flight work at drain-timeout.

**Prerequisites for full acceptance:** [Story 3.1](3-1-deployable-rp-service-profile.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [tests/drain_test.sh](../../../tests/drain_test.sh)
- [verifier/go/main.go](../../../verifier/go/main.go)
- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/operations/RP-RUNBOOK.md`

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

Check every command against the deployment profile and list a first diagnostic action per failure mode. Prepare game-day scripts and expected observations; a written runbook remains unexercised until 11.5 evidence exists.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
