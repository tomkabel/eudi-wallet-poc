---
story_id: "5.6"
story_key: 5-6-registry-hosting-and-succession-path
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["5.1", "5.2"]
decision_gate: "G7 fallback expiry/rollback policy must be operationally documented."
external_gate: "Host, signing-key custodian and separately authorized deployment/publication."
source_contract_sha256: f8ca73a9ea226e3f435b9018ab70f95f2436020caa72f8338cdcec2724be1304
---

# Story 5.6: Registry hosting and succession path

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 5.6](../../planning-artifacts/epics.md#story-56-registry-hosting-and-succession-path).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 5.6: Registry hosting and succession path

As a prospective adopter,
I want the registry shipped as a generator plus a hosted instance with a documented succession path,
So that adoption does not depend on who happens to host it today.

**Acceptance Criteria:**

- **Given** that the AV Profile names no maintainer for the accepted set,
- **When** the registry is published,
- **Then** it ships as a generator plus a hosted instance,
- **And** a documented succession path names what happens if no scheme owner adopts it,
- **And** the local pinned list is documented as the fallback for an ownerless period.

**Covers:** FR21. **Source:** O4 dependencies; risk R4 mitigation.

## Analysis and implementation boundaries

Prepare generator release, static hosting configuration and key succession procedure. Name the interim maintainer role honestly; a community list does not gain scheme-owner authority by being hosted. Specify ownerless-period expiry and key replacement behavior.

**Prerequisites for full acceptance:** [Story 5.1](5-1-signed-circuit-list-document-and-generator.md), [Story 5.2](5-2-verifier-fetch-with-pinned-key-validation.md)

**Decision/evidence gate:** G7 fallback expiry/rollback policy must be operationally documented.

**External acceptance boundary:** Host, signing-key custodian and separately authorized deployment/publication.

## Existing entry points

- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/operations/CIRCUIT-REGISTRY.md`
- `deployment/circuit-registry/`

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

Rehearse regeneration, expiry and key replacement locally. Record actual hosted URL, revision and retrieval evidence only after an authorized deployment; document an unhosted instance as pending rather than inventing a service.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
