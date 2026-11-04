---
story_id: "7.5"
story_key: 7-5-haip-1-0-clause-statement
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 verify final HAIP edition/date against primary text; inherited date is UNVERIFIED in this draft."
external_gate: "Access to the official HAIP text or a provenance-pinned permitted copy."
source_contract_sha256: aa31c8142125fdb6d06049e74531212777ca1cbbe2c422cc5df3f4c077b9db84
---

# Story 7.5: HAIP 1.0 clause statement

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.5](../../planning-artifacts/epics.md#story-75-haip-10-clause-statement).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.5: HAIP 1.0 clause statement

As an evaluator comparing the fork against a final standard,
I want a clause-by-clause statement against HAIP 1.0,
So that the cheapest remaining credibility gap against the settled OpenID profiles is closed.

**Acceptance Criteria:**

- **Given** HAIP 1.0 Final (24 December 2025) and the fork's existing behaviour (encrypted
response by default, client-id prefix policy, per-presentation unlock),
- **When** the clause statement is produced,
- **Then** every clause is marked supported, partially supported or not applicable, with the
evidence for each,
- **And** the statement does not claim profile conformance.
- **And** a clause that cannot be assessed has its own member in the status set, so an
unassessable clause is never recorded as satisfied or as not applicable,
**Covers:** FR34. **Source:** S3 required change and verification method; §4 technical standards paragraph.

## Analysis and implementation boundaries

Enumerate every applicable clause from the pinned source and map behavior/tests per path. Use supported, partial, not applicable and not assessable; retain explicit unsupported subrequirements within partial or not-assessable explanations so absence cannot read as satisfaction.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 verify final HAIP edition/date against primary text; inherited date is UNVERIFIED in this draft.

**External acceptance boundary:** Access to the official HAIP text or a provenance-pinned permitted copy.

## Existing entry points

- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)
- [docs/decisions/ADR-003-MINIMAL-JWE.md](../../../docs/decisions/ADR-003-MINIMAL-JWE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/conformity/HAIP-CLAUSE-STATEMENT.md`

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

Check encrypted-default, client-id-prefix and per-presentation-unlock claims against code and actual evidence. Require rationale for every not-applicable row and a source for every clause; avoid an overall conformance assertion.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
