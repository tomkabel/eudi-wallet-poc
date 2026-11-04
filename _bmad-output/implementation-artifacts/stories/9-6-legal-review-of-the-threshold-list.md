---
story_id: "9.6"
story_key: 9-6-legal-review-of-the-threshold-list
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["9.1"]
decision_gate: "G12 threshold scope/legal premise."
external_gate: "Qualified legal reviewer, attributed review outcome and current primary citations."
source_contract_sha256: 3784f4210dc5ae8875939ad852408bdef62ef7445f7c0bf39088bfd0f36b2aca
---

# Story 9.6: Legal review of the threshold list

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 9.6](../../planning-artifacts/epics.md#story-96-legal-review-of-the-threshold-list).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 9.6: Legal review of the threshold list

As the programme owner,
I want the threshold list legally reviewed and an unsupported-threshold policy published,
So that any unmapped threshold is explicit rather than incidental.

**Acceptance Criteria:**

- **Given** the statutory threshold map,
- **When** the legal review completes,
- **Then** the review's outcome and reviewer role are recorded,
- **And** the unsupported-threshold policy states what happens for an unmapped threshold,
- **And** the extra circuits' cost effect on prover time and proof size (~353–360 KB, ~6.1 s
proving) is recorded alongside the benefit.
- **And** a threshold whose legal citation the reviewer rejects is unmapped and stops proving,
rather than staying mapped on a citation that no longer supports it,
**Blocked by (external):** a legal reviewer for the threshold citations. Where the external party
or hardware is unavailable, the story still closes its in-repository half and records the state as
blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR54. **Source:** O6 validation; O6 risks; §7 Q3.

## Analysis and implementation boundaries

Prepare review questions and unsupported-threshold rules without presenting an agent's analysis as legal approval. Remove unsupported mappings when review rejects their premise. Preserve quoted prover-time/size figures only with their historical host baseline, not as measured marginal cost of extra circuits.

**Prerequisites for full acceptance:** [Story 9.1](9-1-statutory-threshold-to-circuit-map.md)

**Decision/evidence gate:** G12 threshold scope/legal premise.

**External acceptance boundary:** Qualified legal reviewer, attributed review outcome and current primary citations.

## Existing entry points

- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)
- [docs/analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md](../../../docs/analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/reviews/THRESHOLD-LEGAL-REVIEW.md`
- `docs/profiles/UNSUPPORTED-THRESHOLD-POLICY.md`

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

Retain reviewer role, reviewed revision, per-citation decisions and resulting disabled mappings. Quantify extra-circuit cost only through stated method/measurements; missing review remains pending and cannot authorize enabling a threshold.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
