---
story_id: "3.4"
story_key: 3-4-rp-register-interface-proposal
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.3"]
decision_gate: "G1 register cache precedence."
external_gate: "No external endorsement is required to write a proposal; external publication is a separate action."
source_contract_sha256: 36ec2486999217a6c2f0a4ba3ee27304ea451f7be87449ecd30995baf243ae33
---

# Story 3.4: RP-register interface proposal

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.4](../../planning-artifacts/epics.md#story-34-rp-register-interface-proposal).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.4: RP-register interface proposal

As the Estonian programme's competent authority,
I want a written register interface proposal with mocks and a compliance note against the ARF RP topics,
So that the interface is on the table rather than reconstructed after the procurement is awarded.

**Acceptance Criteria:**

- **Given** the mock-backed interface and the ARF relying-party registration topics,
- **When** the proposal is written and published,
- **Then** it states the proposed lookup contract, the refusal semantics and a fail-closed
default,
- **And** it is published even though RIA has no published API and the access-certificate approach
is still in design,
- **And** the proposal records that it is useful even if superseded.

**Covers:** FR12. **Source:** D3 deliverable and exit criteria; O2 risks and mitigation; A6 tradeoff 5.

## Analysis and implementation boundaries

Write the proposal from the shared model and mock, mapping ARF topics to sourced clauses. State lookup identity, versioning, TTL, refusal semantics and what is proposed versus verified. Draft the stable model now; finalize executable examples after 3.3.

**Prerequisites for full acceptance:** [Story 3.3](3-3-fail-closed-rp-register-interface-and-mock.md)

**Decision/evidence gate:** G1 register cache precedence.

**External acceptance boundary:** No external endorsement is required to write a proposal; external publication is a separate action.

## Existing entry points

- [docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md](../../../docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md)
- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/proposals/RP-REGISTER-INTERFACE.md`

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

Check each contract example against the mock and each normative citation against the pinned source. Include absent, unreachable, not-found and stale outcomes; record supersession and lack of national API authority.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
