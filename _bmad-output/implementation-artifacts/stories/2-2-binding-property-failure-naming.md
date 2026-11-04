---
story_id: "2.2"
story_key: 2-2-binding-property-failure-naming
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["2.1"]
decision_gate: null
external_gate: null
source_contract_sha256: d50ba52d5355c65f0224e00543391c1bc40c3e4d3a24959fd644c9663f77d89f
---

# Story 2.2: Binding-property failure naming

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 2.2](../../planning-artifacts/epics.md#story-22-binding-property-failure-naming).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 2.2: Binding-property failure naming

As an integrator debugging a rejected presentation,
I want a failing case to name the binding property that failed,
So that I can fix the right thing instead of guessing at a generic "invalid".

**Acceptance Criteria:**

- **Given** a response that violates `client_id`, `nonce`, `response_uri`, key thumbprint, single
use, or the predicate value,
- **When** the runner evaluates it,
- **Then** the failure names that property,
- **And** no failure path emits a generic "invalid" in place of the property name.
- **And** a well-formed response carrying no provable presentation is reported as its own outcome
(`no-presentation`, refused), not as a binding-property failure,
**Covers:** FR7. **Source:** O5 acceptance criteria (iii).

## Analysis and implementation boundaries

Define a stable runner-facing vocabulary for client_id, nonce, response_uri, key thumbprint, single-use and predicate failures. Reuse structured runtime causes where available; do not infer a property from a generic crash or disclose raw credentials in diagnostics.

**Prerequisites for full acceptance:** [Story 2.1](2-1-carrier-runner-over-existing-cases.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [verifier/go/oid4vp/carrier.go](../../../verifier/go/oid4vp/carrier.go)
- [verifier/go/oid4vp/transcript.go](../../../verifier/go/oid4vp/transcript.go)
- [tests/e2e.sh](../../../tests/e2e.sh)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/CARRIER-FAILURE-REASONS.md`

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

Isolate each binding mutation with all other bindings valid. Assert no-presentation, decryption failure and unrecognized carrier remain separate from binding-property failures.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
