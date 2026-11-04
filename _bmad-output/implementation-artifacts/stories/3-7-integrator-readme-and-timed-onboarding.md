---
story_id: "3.7"
story_key: 3-7-integrator-readme-and-timed-onboarding
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.1", "3.5"]
decision_gate: null
external_gate: "At least three independent integrators without prior ZK knowledge and their attributed timing records."
source_contract_sha256: 1a5d243472b92040682f1a20ba16a4235f42c46c8b4881c9cf43390859ba9540
---

# Story 3.7: Integrator README and timed onboarding

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.7](../../planning-artifacts/epics.md#story-37-integrator-readme-and-timed-onboarding).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.7: Integrator README and timed onboarding

As an integrator with no prior ZK knowledge,
I want a README that takes me to four reproduced results in under a day,
So that the kit's value is measurable rather than claimed.

**Acceptance Criteria:**

- **Given** a clean checkout and the README,
- **When** an integrator with no prior ZK knowledge follows it,
- **Then** the four acceptance results are reproduced within a working day,
- **And** time-to-first-successful-verification is measured with a stopwatch and reported as a
distribution over at least three integrators,
- **And** the measurement's method and assumptions accompany the number.
- **And** every participant is recorded, including those who never reach a first successful
verification, and the reported figure carries both the count and the failure count,
**Blocked by (external):** three human integrators with no prior ZK knowledge. Where the external
party or hardware is unavailable, the story still closes its in-repository half and records the
state as blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR15. **Source:** O2 acceptance criteria (iii); §7.1 benchmark 4; NFR7.

## Analysis and implementation boundaries

Write one clean-checkout path using the documented build and CPU prerequisites. Define stopwatch start/stop, working-day duration, assistance policy and failed/aborted participant accounting before recruiting. Reuse 3.1's README rather than creating conflicting install instructions.

**Prerequisites for full acceptance:** [Story 3.1](3-1-deployable-rp-service-profile.md), [Story 3.5](3-5-four-estonian-acceptance-cases.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** At least three independent integrators without prior ZK knowledge and their attributed timing records.

## Existing entry points

- [docs/DEVELOPMENT.md](../../../docs/DEVELOPMENT.md)
- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `rp-kit/README.md`
- `docs/measurements/RP-ONBOARDING.md`

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

For every participant retain host, checkout, commands, help received and time or failure reason. Report sample count, failure count and distribution; automated rehearsals do not count as human onboarding evidence.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
