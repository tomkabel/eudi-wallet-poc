---
story_id: "7.10"
story_key: 7-10-national-wallet-production-comparison
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 current external wallet-production claims need primary-source revalidation."
external_gate: "Current official repository/build and certification/provisioning material."
source_contract_sha256: 09d9ac38bec276ebfee0f942726a595e444342e768da4a70df0f2f0a62db69b9
---

# Story 7.10: National-wallet production comparison

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.10](../../planning-artifacts/epics.md#story-710-national-wallet-production-comparison).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.10: National-wallet production comparison

As a programme owner,
I want a comparison against the reference wallet app and the German national wallet build covering certification, provisioning and support,
So that the production pathway gap is described in the terms a procurement uses.

**Acceptance Criteria:**

- **Given** `eudi-app-android-wallet-ui` (dev/demo builds, not production-ready) and
`de-eudi-wallet-android` (read-only mirror, placeholder endpoints, unsigned release builds),
- **When** the comparison is written,
- **Then** it states what a national-wallet production pathway requires that this repository does
not have,
- **And** it records the difference between a one-way source mirror and a reproducible build,
- **And** it does not claim a certification path exists.

**Covers:** FR33. **Source:** A1.1 rows 3 and 10; A1.3 row 6; risk R12; L3.

## Analysis and implementation boundaries

Compare the two named wallet projects and this fork using dated revisions and observable build/provisioning/support evidence. The corpus's mirror/endpoints/release-signing claims are hypotheses to recheck, not prefilled conclusions.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 current external wallet-production claims need primary-source revalidation.

**External acceptance boundary:** Current official repository/build and certification/provisioning material.

## Existing entry points

- [docs/research/2026-10-23-shared-appendices.md](../../../docs/research/2026-10-23-shared-appendices.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/NATIONAL-WALLET-PRODUCTION-COMPARISON.md`

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

For each claim retain a primary source or UNVERIFIED marker; distinguish source availability, reproducible builds, release signing, provisioning, certification and operator support. Do not infer certification from project branding.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
