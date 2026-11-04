---
story_id: "11.2"
story_key: 11-2-monitoring-availability-and-incident-process
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.1", "3.3"]
decision_gate: "G16 service owner must select numeric SLO, window, severities and notification roles."
external_gate: "Deployment/monitoring environment and operator-approved incident process."
source_contract_sha256: 5ab6a7893a392545ad624a548d900241307f3e99b09b1030b78af73969fa49fa
---

# Story 11.2: Monitoring availability and incident process

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.2](../../planning-artifacts/epics.md#story-112-monitoring-availability-and-incident-process).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.2: Monitoring availability and incident process

As a service owner,
I want monitoring, a stated availability target and an incident process,
So that a pilot has an operational surface.

**Acceptance Criteria:**

- **Given** the service exposes `/healthz`,
- **When** monitoring is configured,
- **Then** an availability target is stated numerically,
- **And** the incident process names who is notified, the severity levels and the post-incident
record,
- **And** the register's unavailability behaviour is covered because it is on the critical path.
- **And** a breach of the numeric availability target raises an incident at a stated severity, so
a target breach cannot pass without an incident,
**Covers:** FR61. **Source:** A3 R1 deliverable; risk R9.

## Analysis and implementation boundaries

Define useful-health versus process-liveness signals, register outages, admission shedding and draining. Instrument without central holder/presentation tracking. Prepare incident routing configuration, but do not send notifications during this drafting task.

**Prerequisites for full acceptance:** [Story 3.1](3-1-deployable-rp-service-profile.md), [Story 3.3](3-3-fail-closed-rp-register-interface-and-mock.md)

**Decision/evidence gate:** G16 service owner must select numeric SLO, window, severities and notification roles.

**External acceptance boundary:** Deployment/monitoring environment and operator-approved incident process.

## Existing entry points

- [verifier/go/main.go](../../../verifier/go/main.go)
- [docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md](../../../docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/operations/RP-MONITORING-AND-INCIDENTS.md`
- `rp-kit/monitoring/`

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

Simulate target breach and assert incident classification/routing in a local sink. Record availability window/denominator and chosen severity; no invented 99.x target or named human commitment substitutes for owner agreement.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
