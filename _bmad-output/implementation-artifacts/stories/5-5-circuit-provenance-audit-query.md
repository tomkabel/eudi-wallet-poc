---
story_id: "5.5"
story_key: 5-5-circuit-provenance-audit-query
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["5.2"]
decision_gate: null
external_gate: null
source_contract_sha256: c535ea7186cd0b5062315255e27f988b307b94c1cf7c00e787de1fb4d218983d
---

# Story 5.5: Circuit provenance audit query

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 5.5](../../planning-artifacts/epics.md#story-55-circuit-provenance-audit-query).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 5.5: Circuit provenance audit query

As an auditor answering "which circuit proved this?" months later,
I want an audit query returning the circuit provenance for a given presentation,
So that provenance survives the transaction.

**Acceptance Criteria:**

- **Given** a completed presentation,
- **When** the audit query is issued,
- **Then** it returns the circuit provenance for that presentation,
- **And** the response includes the registry document version the circuit was accepted under.
- **And** an unknown, refused or aged-out presentation id returns a distinguished not-found result
— never an empty or inferred provenance — and the retention boundary itself is tested,
**Covers:** FR25. **Source:** O4 acceptance criteria (iv).

## Analysis and implementation boundaries

Implement the bounded in-memory per-session provenance ring from ADR-006, aggregating verified credentials with carrier tag and list version. The original months-later aspiration is limited by the accepted process-lifetime retention decision; do not introduce persistent logs to satisfy that phrase.

**Prerequisites for full acceptance:** [Story 5.2](5-2-verifier-fetch-with-pinned-key-validation.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [verifier/go/oid4vp/session.go](../../../verifier/go/oid4vp/session.go)
- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `verifier/go/circuits/provenance.go`
- `docs/profiles/CIRCUIT-PROVENANCE.md`

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

Test success, multi-credential aggregation, unknown/refused id, ring eviction and process restart. Return distinguished not-found rather than reconstructing provenance; assert no credential values or identifying holder attributes are persisted.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
