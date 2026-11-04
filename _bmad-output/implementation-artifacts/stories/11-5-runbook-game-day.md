---
story_id: "11.5"
story_key: 11-5-runbook-game-day
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["11.1", "11.2"]
decision_gate: null
external_gate: "Operator participants and representative environment; exercise authorization belongs to the later execution task."
source_contract_sha256: fc5fe551848b82e9d398fa65b7ef8a962eeb9da48db02e36c8973b5058728ba3
---

# Story 11.5: Runbook game day

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.5](../../planning-artifacts/epics.md#story-115-runbook-game-day).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.5: Runbook game day

As an operator,
I want the runbook exercised in a game day,
So that it is proven rather than written.

**Acceptance Criteria:**

- **Given** the runbook and the deployed service,
- **When** the game day is run,
- **Then** a drain, a rollback and one named failure mode are exercised,
- **And** defects found in the runbook are fixed and the runbook re-issued,
- **And** the exercise's date, participants and outcome are recorded.
- **And** a game day that aborts partway because the environment is unavailable is recorded as
aborted, not as an exercised runbook,
**Blocked by (external):** an operator to exercise the runbook. Where the external party or
hardware is unavailable, the story still closes its in-repository half and records the state as
blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR60. **Source:** A3 R1 exit criteria ("an operational runbook exercised in a game day").

## Analysis and implementation boundaries

Prepare bounded drain/rollback/failure scenarios with entry conditions, observations and recovery steps. State drain timeout consequences, key/session effects and how incident evidence will be retained.

**Prerequisites for full acceptance:** [Story 11.1](11-1-operations-runbook.md), [Story 11.2](11-2-monitoring-availability-and-incident-process.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** Operator participants and representative environment; exercise authorization belongs to the later execution task.

## Existing entry points

- [tests/drain_test.sh](../../../tests/drain_test.sh)
- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/operations/RP-GAME-DAY-RECORD.md`

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

Execute and record dates, roles, exact revisions, expected/observed effects and runbook defects. Abort is ABORTED with unfinished scenarios; reissue and retest the runbook before declaring the exercise complete.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
