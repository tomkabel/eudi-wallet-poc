---
story_id: "6.5"
story_key: 6-5-register-latency-and-availability-profile
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.3"]
decision_gate: "G1 register-cache precedence."
external_gate: "Real service data requires a reachable API; mock injection only establishes modeled costs."
source_contract_sha256: 4ab794c4b44e199356a846554386512682f21d3bc03c1ea36ddb2cac8340cf88
---

# Story 6.5: Register latency and availability profile

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 6.5](../../planning-artifacts/epics.md#story-65-register-latency-and-availability-profile).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 6.5: Register latency and availability profile

As a relying party whose every presentation depends on a register lookup,
I want the register's latency and availability impact profiled once its interface exists,
So that a fail-closed lookup on the critical path is a known cost rather than a surprise.

**Acceptance Criteria:**

- **Given** a register implementation (mock, or the real one when it exists),
- **When** the presentation path is profiled,
- **Then** the added latency and the behaviour under register unavailability are recorded,
- **And** the bounded-cache and stale-data decisions from Story 3.3 are exercised by the profile.

**Covers:** FR30. **Source:** §7.1 benchmark 5; risk R9.

## Analysis and implementation boundaries

Profile cache hit, miss, expiry and unavailable-register cases separately. Publish the configured TTL, lookup timeout and injected/mock versus real source; do not present a simulated latency distribution as production behavior.

**Prerequisites for full acceptance:** [Story 3.3](3-3-fail-closed-rp-register-interface-and-mock.md)

**Decision/evidence gate:** G1 register-cache precedence.

**External acceptance boundary:** Real service data requires a reachable API; mock injection only establishes modeled costs.

## Existing entry points

- [docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md](../../../docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/measurements/RP-REGISTER-IMPACT.md`

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

Compare baseline path and lookup-enabled path using the same host/load. Exercise exact TTL, cold outage and within-TTL outage according to G1; report both latency and refusal/allow counts.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
