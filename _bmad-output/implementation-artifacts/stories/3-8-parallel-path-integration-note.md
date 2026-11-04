---
story_id: "3.8"
story_key: 3-8-parallel-path-integration-note
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 569574a3494d2a167cfb5f8cc69dfe4bd3705f9f6ffcd328fcb624f30330c52d
---

# Story 3.8: Parallel-path integration note

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.8](../../planning-artifacts/epics.md#story-38-parallel-path-integration-note).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.8: Parallel-path integration note

As an architect mapping the wallet onto Estonian infrastructure,
I want a written note stating that the wallet-facing integration is a parallel path and that X-Road has no documented relationship to the wallet protocols,
So that a false architectural premise does not enter a planning document.

**Acceptance Criteria:**

- **Given** the verified 0-hit result for "OpenID" across RIA's X-tee page, `x-road.global` and
the X-Road repository landing page,
- **When** the integration note is written,
- **Then** it records that X-Road has no documented relationship to the EUDI Wallet, OpenID4VP or
OpenID4VCI,
- **And** it names transporting `vp_token`s over X-Road and looking registrations up over X-Road
as proposed interfaces lacking verified support,
- **And** it names e-Residency/Digi-ID as a constraint and Smart-ID/Mobile-ID as a second
authentication route, not as EUDI presentation routes.

**Covers:** FR10. **Source:** §6 integration pathways (X-Road, e-Residency, Smart-ID rows); §5.1 P4.

## Analysis and implementation boundaries

Document TARA and wallet as parallel service interfaces. Treat the corpus's zero-hit X-Road search as dated evidence of no located integration, not proof that no relationship can ever exist. Label transport/register-over-X-Road ideas as proposals.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [docs/research/2026-10-23-estonia-alignment-assessment.md](../../../docs/research/2026-10-23-estonia-alignment-assessment.md)
- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/ESTONIAN-INTEGRATION-PATHS.md`

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

Recheck primary integration documentation or mark current relationship claims UNVERIFIED. Distinguish e-Residency identity-source constraints and Smart-ID/Mobile-ID authentication from wallet presentation protocols.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
