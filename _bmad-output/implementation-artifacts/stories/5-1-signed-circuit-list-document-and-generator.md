---
story_id: "5.1"
story_key: 5-1-signed-circuit-list-document-and-generator
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G5 define library-version compatibility and G6 define list-version transport/binding before consumer implementation."
external_gate: "Scheme-owner authority is external; interim document is community-maintained."
source_contract_sha256: 8d01fabf4ce450486dee9c7d53f49c48e40a5910d820ceb95ddacfa650c11216
---

# Story 5.1: Signed circuit-list document and generator

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 5.1](../../planning-artifacts/epics.md#story-51-signed-circuit-list-document-and-generator).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 5.1: Signed circuit-list document and generator

As a scheme owner or community maintainer,
I want a signed JSON circuit-list document format plus a CLI that issues it,
So that "which circuits do we accept" is one checkable document instead of a per-operator convention.

**Acceptance Criteria:**

- **Given** the fields the standards require (system, version, attributes, block-encryption
parameters, hashes, minimum library version),
- **When** the generator emits a document,
- **Then** the document is signed and the signature is verifiable against a pinned key,
- **And** the document format is recorded as an ADR before the verifier work starts,
- **And** the generator stays in-repo so maintaining a list is cheap.

**Covers:** FR21. **Source:** O4 proposed capability; O4 implementation approach.

## Analysis and implementation boundaries

Implement ADR-006's ee-circuits/1 envelope, document-level systems, derived SpecID and existing tuple keying. Sign RFC 8785 canonical bytes excluding signatures, using test-only fixture keys. A revision hash cannot be ordered with less-than; G5 needs an explicit compatibility mapping.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G5 define library-version compatibility and G6 define list-version transport/binding before consumer implementation.

**External acceptance boundary:** Scheme-owner authority is external; interim document is community-maintained.

## Existing entry points

- [verifier/go/circuits/registry.go](../../../verifier/go/circuits/registry.go)
- [verifier/circuits.json](../../../verifier/circuits.json)
- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `verifier/go/cmd/circuit-list/`
- `docs/profiles/CIRCUIT-LIST.md`

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

Use shared golden signature bytes and independent signature verification; mutate payload, key id, duplicate keys and schema fields. Define numeric list_version behavior and source/runtime stamp semantics without inventing ordering over Git hashes.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
