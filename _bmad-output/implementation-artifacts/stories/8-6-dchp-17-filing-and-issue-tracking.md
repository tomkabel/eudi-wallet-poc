---
story_id: "8.6"
story_key: 8-6-dchp-17-filing-and-issue-tracking
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: "Explicit approval is required for filing; this task authorizes drafting only."
source_contract_sha256: 06e93cb8aa06cfb6af72902d992e953775ff3e2c297bb238802d321d8752616e
---

# Story 8.6: DCHP #17 filing and issue tracking

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.6](../../planning-artifacts/epics.md#story-86-dchp-17-filing-and-issue-tracking).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.6: DCHP #17 filing and issue tracking

As an implementer who wants a published carrier,
I want the prepared DCHP #17 submission text filed and the two OpenID issues tracked,
So that the standards gap this carrier fills is visible where it can be closed.

**Acceptance Criteria:**

- **Given** `docs/profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md` is prepared and unfiled,
- **When** the filing is performed,
- **Then** the submission is made with the user's explicit approval (this step is user-gated),
- **And** the mutual verification of spec fields follows the carrier-profile gate,
- **And** OpenID4VP issue #627 and openid/dchp issue #17 are tracked with their status.
- **And** a filing the user withholds approval for is recorded as withheld for that reason, so a
withheld submission is distinguishable from unfinished work,
**Covers:** FR44. **Source:** Report 1 §2 (prepared but unfiled); A5.3.

## Analysis and implementation boundaries

Review the existing submission text against current field definitions, prepare exact reviewable filing content and track issue status with dated primary evidence. Preserve the user-gated filing boundary; do not send messages or file issues during story preparation.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** Explicit approval is required for filing; this task authorizes drafting only.

## Existing entry points

- [docs/profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md](../../../docs/profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md)
- [docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md](../../../docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/STANDARDS-ISSUE-TRACKER.md`

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

Distinguish prepared, approved, filed, withheld and unverified states with evidence for each. Lack of approval is not evidence of explicit withholding; keep filing pending unless a user actually withholds it.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
