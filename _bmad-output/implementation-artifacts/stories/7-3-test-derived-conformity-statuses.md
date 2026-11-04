---
story_id: "7.3"
story_key: 7-3-test-derived-conformity-statuses
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["7.2"]
decision_gate: null
external_gate: null
source_contract_sha256: 5124190a7e347ae618d1b9cd49a81e01ade71dea7057b908bb12f79db2447faa
---

# Story 7.3: Test-derived conformity statuses

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.3](../../planning-artifacts/epics.md#story-73-test-derived-conformity-statuses).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.3: Test-derived conformity statuses

As a maintainer,
I want conformity-note statuses re-derived from tests instead of hand-maintained,
So that the note cannot drift from the code between sessions.

**Acceptance Criteria:**

- **Given** the conformity note's status rows,
- **When** the derivation is built,
- **Then** statuses come from test evidence rather than prose editing,
- **And** the derivation runs as part of the repository's checks,
- **And** a row whose test disappears becomes visible rather than silently stale.

**Covers:** FR32. **Source:** risk R12 mitigation; R3.

## Analysis and implementation boundaries

Define a row-to-test-result manifest and generate statuses from actual result artifacts plus scope metadata. A test name's existence or an old green run is not current execution evidence. Keep policy interpretations separate from machine-derived outcomes.

**Prerequisites for full acceptance:** [Story 7.2](7-2-audit-remaining-hand-maintained-conformity-rows.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)
- [tests/README.md](../../../tests/README.md)
- [Makefile](../../../Makefile)
- [.github/workflows/ci.yml](../../../.github/workflows/ci.yml)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `tools/derive_conformity.py`
- `tests/conformity-evidence.json`

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

Remove a mapped test, omit a result, mark it skipped, fail it and give it the wrong revision; all must remain visible and none may derive supported. Hook deterministic checking into the appropriate CI job without requiring all platforms in the fast job.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
