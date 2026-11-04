---
story_id: "11.3"
story_key: 11-3-accessibility-conformance-audit
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 pin applicable EN 301 549 and WCAG scope; G15 reconcile current language/surface evidence."
external_gate: "Independent accessibility auditor and agreed in-scope builds/surfaces."
source_contract_sha256: 0156a96a1f235221142497c4d99862b47283f93c339cba8a9e0a61cd5674443b
---

# Story 11.3: Accessibility conformance audit

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.3](../../planning-artifacts/epics.md#story-113-accessibility-conformance-audit).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.3: Accessibility conformance audit

As a public-sector buyer under the accessibility duty,
I want an EN 301 549 conformance audit alongside the WCAG 2.1 AA commitment,
So that the accessibility claim has evidence.

**Acceptance Criteria:**

- **Given** PRODUCT.md commits to WCAG 2.1 AA, reduced-motion and screen-reader announcements are
implemented, and no EN 301 549 evidence exists,
- **When** the audit completes,
- **Then** an audit report exists with its findings and their resolution,
- **And** the audit is recorded as a milestone exit criterion for the pilot,
- **And** the report states which parts of the interface were and were not in scope.
- **And** an audit finding whose resolution is deferred or refused blocks the exit milestone, so
the milestone cannot be declared met with findings still open,
**Blocked by (external):** an accessibility auditor. Where the external party or hardware is
unavailable, the story still closes its in-repository half and records the state as blocked with
the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR62. **Source:** A13 required change; E5; risk R13; A3 R1 exit criteria.

## Analysis and implementation boundaries

Prepare a surface-by-criterion matrix covering wallet and sandbox as scoped, assistive technology, keyboard, focus, contrast, announcements, motion and locale behavior. Separate automated checks, maintainer testing and independent audit.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 pin applicable EN 301 549 and WCAG scope; G15 reconcile current language/surface evidence.

**External acceptance boundary:** Independent accessibility auditor and agreed in-scope builds/surfaces.

## Existing entry points

- [PRODUCT.md](../../../PRODUCT.md)
- [DESIGN.md](../../../DESIGN.md)
- [app/src/main/res/](../../../app/src/main/res/)
- [sandbox/static/](../../../sandbox/static/)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/reviews/ACCESSIBILITY-AUDIT.md`

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

Record actual findings, fixes and retests with excluded surfaces named. Deferred or unresolved findings keep the declared milestone open; no audit report template is conformance evidence.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
