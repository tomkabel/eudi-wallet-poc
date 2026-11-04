---
story_id: "3.3"
story_key: 3-3-fail-closed-rp-register-interface-and-mock
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G1 register cache precedence must reconcile the literal story with ADR-007 before implementing allows."
external_gate: "Real RIA API is unnecessary for the mock-backed interface; national integration remains unverified."
source_contract_sha256: 42b65a44a5f1e6da08f97eb7e194c174fc3823d496fdb5a3a4187692758ceb4f
---

# Story 3.3: Fail-closed RP-register interface and mock

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.3](../../planning-artifacts/epics.md#story-33-fail-closed-rp-register-interface-and-mock).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.3: Fail-closed RP-register interface and mock

As a relying party that must request only what is registered,
I want the register lookup to be a pluggable interface that refuses when it has no entry,
So that a service that does not exist yet cannot silently become an allow.

**Acceptance Criteria:**

- **Given** the register interface with no implementation configured,
- **When** a presentation is attempted,
- **Then** the request is refused with the missing prerequisite named in the log,
- **And** a shipped mock satisfies the interface for tests,
- **And** the contract includes a bounded cache age and an explicit, logged decision about stale
data — never a silent allow.
- **And** a register that is configured but unreachable, timing out or answering 5xx refuses the
presentation and logs the register error — that state is distinct from no implementation
configured, and a cache hit is never required for the refusal to hold,
**Covers:** FR11. **Source:** E2; O2 proposed capability (b); risk R9 mitigation; A6 tradeoff 3.

## Analysis and implementation boundaries

Implement the ADR-007 interface and shared six-field model. Distinguish unconfigured, unreachable and not-found. The ADR explicitly permits logged within-TTL cached allows during an outage, whereas the story says outage refuses; preserve this as G1 until reconciled. Use one shared registration corpus with 4.6; the mock can start before its certificate generator.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G1 register cache precedence must reconcile the literal story with ADR-007 before implementing allows.

**External acceptance boundary:** Real RIA API is unnecessary for the mock-backed interface; national integration remains unverified.

## Existing entry points

- [docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md](../../../docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md)
- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)
- [verifier/go/main.go](../../../verifier/go/main.go)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `verifier/go/rpregister/`
- `rp-kit/fixtures/registrations.json`

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

After G1 is recorded, assert cold-cache timeout/5xx/DNS refusal, exact TTL boundary, stale-entry discard, the chosen fresh-cache outage behavior and an omitted unregistered identity. Never silently turn a lookup failure into allow.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
