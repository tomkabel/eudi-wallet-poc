---
story_id: "8.2"
story_key: 8-2-iso-18013-5-second-edition-watch
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["2.3"]
decision_gate: "G10 ISO publication date and unread normative text remain UNVERIFIED until sourced."
external_gate: "Access to the published/draft ISO text needed to settle byte-level differences."
source_contract_sha256: fa919e75c9f3cb6f6c8590eb2b1928e4e899cc594e192480538c760b091450b0
---

# Story 8.2: ISO 18013-5 second-edition watch

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.2](../../planning-artifacts/epics.md#story-82-iso-18013-5-second-edition-watch).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.2: ISO 18013-5 second-edition watch

As a maintainer of the carrier layer,
I want a documented watch with version-labelled fixtures and a rename/adapt plan,
So that the published edition is a bounded change rather than a rewrite.

**Acceptance Criteria:**

- **Given** the ISO/IEC 18013-5 second edition's publication slipping from Q3 2026 to Q1 2027
`[UNVERIFIED]` (from the ISO project page, not the standard) and the ISO text being unreachable
during research,
- **When** the watch is established,
- **And** the fixtures and profile rules stay version-labelled under Story 2.3's assertion (this
story keeps the watch and the rename/adapt plan, not a second labelling duty),
- **And** `docs/analysis/ISO-18013-5-2E-BYTE-DIFF-CHECKLIST.md` is extended with the rename/adapt
steps,
- **And** the carrier interface stays the single adaptation point and the vectors are re-run when
the edition lands,
- **And** `ZKPresentation` remains labelled an interim versioned encoding rather than an
interoperability claim.

**Covers:** FR40. **Source:** S6 required change; U2 dependencies; risk R1; §6 "the ISO second-edition text could not be read".

## Analysis and implementation boundaries

Extend the checklist if it exists, otherwise create it at the backlog's named path. Pin the available source and separate a project schedule page from normative syntax. Keep carrier.go as the adaptation point and interim encoding explicitly versioned.

**Prerequisites for full acceptance:** [Story 2.3](2-3-version-labelled-carrier-fixtures.md)

**Decision/evidence gate:** G10 ISO publication date and unread normative text remain UNVERIFIED until sourced.

**External acceptance boundary:** Access to the published/draft ISO text needed to settle byte-level differences.

## Existing entry points

- [docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md](../../../docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md)
- [verifier/go/oid4vp/carrier.go](../../../verifier/go/oid4vp/carrier.go)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/ISO-18013-5-2E-BYTE-DIFF-CHECKLIST.md`

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

Specify rename/field-order/transcript/parameter checks, fixture refresh and reruns when source access arrives. A watch entry or forecast does not establish ISO interoperability; retain unavailable-source status.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
