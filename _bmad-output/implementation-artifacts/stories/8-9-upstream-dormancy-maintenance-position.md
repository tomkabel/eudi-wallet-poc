---
story_id: "8.9"
story_key: 8-9-upstream-dormancy-maintenance-position
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 recheck upstream activity rather than repeating historical commit counts as current."
external_gate: "A succession owner is a maintainer choice; the draft names roles, not accepted commitments."
source_contract_sha256: dd210c8a37f775a6f0a4138b5bfbb7027a92d0ddbfb2e2507288e424171e0364
---

# Story 8.9: Upstream-dormancy maintenance position

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.9](../../planning-artifacts/epics.md#story-89-upstream-dormancy-maintenance-position).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.9: Upstream-dormancy maintenance position

As a maintainer,
I want the maintenance position recorded and a succession note written,
So that the fork carrying the whole load is a stated condition rather than a discovered one.

**Acceptance Criteria:**

- **Given** upstream has one commit in 12 months, 0 open issues/PRs and a single contributor, and
the fork is 0 commits behind (the corpus records this as judgement from the repository's activity,
not as a published policy),
- **When** the position is recorded,
- **Then** it states that the fork is a strict superset and vendors what it needs,
- **And** library upgrades stay routine and pinned in `gradle/libs.versions.toml`,
- **And** the absence of a second maintainer is named rather than glossed.

**Covers:** FR47. **Source:** risk R10.

## Analysis and implementation boundaries

Record pinned vendoring, upgrade cadence, upstream-diff checks and a succession process. Use dated observed activity and license provenance, avoiding claims about upstream intentions from sparse commits.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 recheck upstream activity rather than repeating historical commit counts as current.

**External acceptance boundary:** A succession owner is a maintainer choice; the draft names roles, not accepted commitments.

## Existing entry points

- [gradle/libs.versions.toml](../../../gradle/libs.versions.toml)
- [docs/research/2026-10-23-shared-appendices.md](../../../docs/research/2026-10-23-shared-appendices.md)
- [docs/planning/EE-EUDIW-ZK-IMPORT-PLAN.md](../../../docs/planning/EE-EUDIW-ZK-IMPORT-PLAN.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/operations/MAINTENANCE-AND-SUCCESSION.md`

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

Check fork/superset claims against a named upstream revision and passing relevant suites at execution time. Publish no promise of a second maintainer; document the consequence if nobody adopts responsibility.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
