---
story_id: "8.1"
story_key: 8-1-zkp_08-and-eccg-tension-report
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["7.7"]
decision_gate: "G10 confirm the pinned primary texts before repeating an unsatisfiability conclusion."
external_gate: "Official ARF and ECCG source revisions."
source_contract_sha256: dc1fad049d8eeb83cdf6cdc6c0f8d7cc9730fcac4af195972eb81db3a57484a2
---

# Story 8.1: ZKP_08 and ECCG tension report

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.1](../../planning-artifacts/epics.md#story-81-zkp_08-and-eccg-tension-report).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.1: ZKP_08 and ECCG tension report

As a programme owner being told about compliance,
I want a written report of the `ZKP_08`/ECCG tension with its sources and a re-check trigger,
So that nobody is told `ZKP_08` is met.

**Acceptance Criteria:**

- **Given** `ZKP_08` requires a scheme relying solely on the ECCG Agreed Cryptographic Mechanisms
v2.0 and that catalogue lists no ZKP scheme, while the AV Profile mandates `longfellow-libzk-v1`,
- **When** the report is written,
- **Then** it states the tension, names both primary sources, and says the requirement is
unsatisfiable as written,
- **And** it states that this is a finding about the specification rather than about this fork,
- **And** a re-check trigger is recorded on the catalogue's next revision.

**Covers:** FR39. **Source:** R5; A11; risk R3; §2.1 conflict paragraph; §8 conflicts table.

## Analysis and implementation boundaries

Build on the existing analysis, quote the requirement/catalogue scope and separate the finding from this fork's implementation gaps. Record catalogue/ARF/AV-profile revisions, review date and next-revision recheck trigger.

**Prerequisites for full acceptance:** [Story 7.7](7-7-eccg-clause-map-and-algorithm-inventory.md)

**Decision/evidence gate:** G10 confirm the pinned primary texts before repeating an unsatisfiability conclusion.

**External acceptance boundary:** Official ARF and ECCG source revisions.

## Existing entry points

- [docs/analysis/ZKP08-ANALYSIS.md](../../../docs/analysis/ZKP08-ANALYSIS.md)
- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/ZKP08-ECCG-TENSION-REPORT.md`

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

If current primary evidence supports the contradiction, state it within that pinned scope; otherwise retain the historical finding and mark the current conclusion UNVERIFIED. Never mark ZKP_08 met because AV mandates Longfellow.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
