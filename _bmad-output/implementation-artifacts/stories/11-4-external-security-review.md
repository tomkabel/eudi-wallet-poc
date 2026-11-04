---
story_id: "11.4"
story_key: 11-4-external-security-review
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.1"]
decision_gate: "G16 pilot review scope and release/finding policy require a sponsor."
external_gate: "Independent security reviewer and reachable representative deployment."
source_contract_sha256: 2bf0638731077847c764200da835b191d159221d7c67ea504cf12f02120d5815
---

# Story 11.4: External security review

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.4](../../planning-artifacts/epics.md#story-114-external-security-review).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.4: External security review

As a pilot sponsor,
I want an external security review of the deployed service with no unresolved high findings,
So that the pilot is not run on unreviewed code.

**Acceptance Criteria:**

- **Given** the deployed service and its trust boundaries,
- **When** the external review completes,
- **Then** no high finding remains unresolved,
- **And** the review's scope and the reviewer's independence are recorded,
- **And** any accepted residual risk is stated with its rationale.
- **And** a review that cannot be completed before the pilot is recorded as not performed,
distinct from a clean review, so the two are never confused,
**Blocked by (external):** an independent security reviewer. Where the external party or hardware
is unavailable, the story still closes its in-repository half and records the state as blocked
with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR63. **Source:** A3 R1 exit criteria.

## Analysis and implementation boundaries

Prepare scope, trust-boundary inventory and evidence package using the current architecture record rather than the imported description alone. Keep internal audit findings visible and separate from an independent assessment.

**Prerequisites for full acceptance:** [Story 3.1](3-1-deployable-rp-service-profile.md)

**Decision/evidence gate:** G16 pilot review scope and release/finding policy require a sponsor.

**External acceptance boundary:** Independent security reviewer and reachable representative deployment.

## Existing entry points

- [docs/ARCHITECTURE.md](../../../docs/ARCHITECTURE.md)
- [_bmad-output/planning-artifacts/architecture.md](../../../_bmad-output/planning-artifacts/architecture.md)
- [docs/sota-audit-ROLLUP.md](../../../docs/sota-audit-ROLLUP.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/reviews/PILOT-SECURITY-REVIEW.md`

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

Retain reviewer independence, reviewed revision/deployment, methodology, findings and retests. Any unresolved high finding blocks the stated pilot gate; unavailable review is NOT_PERFORMED, never a clean review.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
