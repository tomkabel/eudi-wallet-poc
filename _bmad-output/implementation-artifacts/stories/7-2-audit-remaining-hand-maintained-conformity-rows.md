---
story_id: "7.2"
story_key: 7-2-audit-remaining-hand-maintained-conformity-rows
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["7.1"]
decision_gate: null
external_gate: null
source_contract_sha256: 36596d1035ed1efc55768d5a30eae1cc41af0aa8552c21b3bb52a2878764cdcd
---

# Story 7.2: Audit remaining hand-maintained conformity rows

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.2](../../planning-artifacts/epics.md#story-72-audit-remaining-hand-maintained-conformity-rows).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.2: Audit remaining hand-maintained conformity rows

As a maintainer,
I want the remaining hand-maintained rows in `docs/CONFORMITY.md`, `docs/MEASUREMENTS.md` and the `STEP*-RECORD.md` files checked against the code,
So that the one found row is not the only one.

**Acceptance Criteria:**

- **Given** the hand-maintained prose files,
- **When** each status-bearing row is checked,
- **Then** the symbol named in the row is located in `app/src/main`, `verifier/go` or `tests/`
before the row is accepted or corrected,
- **And** every corrected or confirmed row cites `path:line`,
- **And** the audit's scope and its remaining un-audited rows are stated rather than implied.
- **And** a third audit outcome exists for a row whose named symbol cannot be located anywhere in
the tree: the row is struck as describing removed code rather than left standing as current,
**Covers:** FR31. **Source:** risk R12 residual uncertainty ("the repository has more rows of this kind that were not audited").

## Analysis and implementation boundaries

Inventory status-bearing rows across current documents and historical step records. Preserve historical execution records as dated evidence; annotate superseded claims instead of rewriting history into present behavior. Classify located, contradicted, removed-symbol and not-audited rows.

**Prerequisites for full acceptance:** [Story 7.1](7-1-fix-the-stale-issuerauth-conformity-row.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)
- [docs/planning/](../../../docs/planning/)
- [docs/sota-audit-ROLLUP.md](../../../docs/sota-audit-ROLLUP.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/planning/CONFORMITY-ROW-AUDIT.md`

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

For each current claim retain symbol/test path and revision. Removed code is visibly obsolete, not current; unaudited rows remain enumerated. Refresh audit severity summaries from supported findings without implying they were fixed.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
