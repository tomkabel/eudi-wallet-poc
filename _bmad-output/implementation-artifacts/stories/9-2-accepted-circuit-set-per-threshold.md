---
story_id: "9.2"
story_key: 9-2-accepted-circuit-set-per-threshold
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["5.1", "5.2", "9.1"]
decision_gate: "G12 scope/legal mapping plus G5 circuit compatibility."
external_gate: "Scheme-owner acceptance per circuit entry."
source_contract_sha256: 770e59c9af8de0fce9bbf9df3c0b72a76e6f12790e982b9a30819121af10f461
---

# Story 9.2: Accepted circuit set per threshold

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 9.2](../../planning-artifacts/epics.md#story-92-accepted-circuit-set-per-threshold).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 9.2: Accepted circuit set per threshold

As a relying party asking for a specific threshold,
I want an accepted circuit set covering the mapped thresholds,
So that a proof for the requested threshold can actually be produced.

**Acceptance Criteria:**

- **Given** the Epic 5 registry and the mapped thresholds,
- **When** the accepted set is assembled,
- **Then** each mapped threshold has a named circuit,
- **And** an unmapped threshold yields refusal,
- **And** the external wait for scheme-owner acceptance is recorded as the state of each entry
rather than as a blocked story.

**Covers:** FR50. **Source:** O6 proposed capability; O6 dependencies (`circuit acceptance is external`); risk R4.

## Analysis and implementation boundaries

Map legally justified thresholds to exact circuit tuples and documented accepted-set status. Distinguish community signing from scheme-owner acceptance; a signed list cannot create the latter. Keep pending external acceptance at entry level as the backlog requires.

**Prerequisites for full acceptance:** [Story 5.1](5-1-signed-circuit-list-document-and-generator.md), [Story 5.2](5-2-verifier-fetch-with-pinned-key-validation.md), [Story 9.1](9-1-statutory-threshold-to-circuit-map.md)

**Decision/evidence gate:** G12 scope/legal mapping plus G5 circuit compatibility.

**External acceptance boundary:** Scheme-owner acceptance per circuit entry.

## Existing entry points

- [verifier/go/circuits/registry.go](../../../verifier/go/circuits/registry.go)
- [verifier/circuits.json](../../../verifier/circuits.json)
- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/THRESHOLD-CIRCUIT-SET.md`

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

Refuse unsupported/unaccepted thresholds and detect tuple/threshold mismatches. Preserve published state for generated-but-not-accepted circuits; do not hide the external wait behind a story-wide generic blocker.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
