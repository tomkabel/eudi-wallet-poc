---
story_id: "8.3"
story_key: 8-3-zk-jwt-path-feasibility
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["7.6"]
decision_gate: "G11 zk-jwt is a feasibility study, not authority to add a new runtime proof family."
external_gate: "A specified scheme/implementation if a cryptographic round-trip is to be demonstrated."
source_contract_sha256: 4707f34e35fb8e989f0aea7d7214b1ac5e6dd91e873e022fc5d9889242d68db9
---

# Story 8.3: zk-jwt path feasibility

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.3](../../planning-artifacts/epics.md#story-83-zk-jwt-path-feasibility).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.3: zk-jwt path feasibility

As a relying party that needs proofs over SD-JWT VC,
I want a feasibility study for the `zk-jwt` format with a fixture round-trip,
So that the mdoc-only limitation is a known, costed gap rather than an assumption.

**Acceptance Criteria:**

- **Given** TS13 defines a `zk-jwt` format and the fork's ZK path is mdoc-only,
- **When** the study is completed,
- **Then** it states whether a `zk-jwt` path is feasible, at what cost, and what it would change,
- **And** a fixture round-trip demonstrates the finding or records why it cannot be demonstrated
yet,
- **And** the conclusion is reflected in the TS13 clause map.

**Covers:** FR41. **Source:** A9 required change; S7; U2 (`ZKP_06`).

## Analysis and implementation boundaries

Study representations, bindings, prover/verifier availability and cost with explicit assumptions. A syntax-only fixture round-trip must be labeled as such; introducing a deployed zk-jwt path requires a separate PRD scope decision.

**Prerequisites for full acceptance:** [Story 7.6](7-6-ts13-clause-map.md)

**Decision/evidence gate:** G11 zk-jwt is a feasibility study, not authority to add a new runtime proof family.

**External acceptance boundary:** A specified scheme/implementation if a cryptographic round-trip is to be demonstrated.

## Existing entry points

- [verifier/go/oid4vp/carrier.go](../../../verifier/go/oid4vp/carrier.go)
- [verifier/go/oid4vp/dcql.go](../../../verifier/go/oid4vp/dcql.go)
- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/ZK-JWT-FEASIBILITY.md`
- `tests/vectors/zk-jwt-study/`

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

Show what the fixture actually demonstrates, list unavailable prerequisites and update the TS13 gap without turning an illustrative parser round-trip into proof verification. An infeasible/undemonstrated result is an allowed study outcome.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
