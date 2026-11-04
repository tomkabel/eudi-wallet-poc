---
story_id: "9.3"
story_key: 9-3-nearest-true-above-negotiation-library
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["4.3", "9.1", "9.2"]
decision_gate: "G12 scope and G13 nearest-true-above versus 9.5's no-neighbor rule need an explicit shared decision."
external_gate: "No new legal or circuit authority can be inferred from a negotiation algorithm."
source_contract_sha256: ed6c6bb412211f2e4ff210fe2f07c0ecfefcb08d5b77469d78def027ace08be5
---

# Story 9.3: Nearest-true-above negotiation library

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 9.3](../../planning-artifacts/epics.md#story-93-nearest-true-above-negotiation-library).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 9.3: Nearest-true-above negotiation library

As a wallet implementer,
I want a negotiation library turning "the relying party asked for 21" into the right single predicate without ever disclosing a `false` one,
So that the rule is implemented once rather than improvised per request.

**Acceptance Criteria:**

- **Given** ISO/IEC 18013-5 §7.2.5 and `EE-POA-030` as the specification states it (at most one
predicate per presentation, `false` predicates withheld, and the one exception — a relying party
whose *registered intended use names each* requested predicate),
- **When** the library is written,
- **Then** it implements nearest-true-above with the stricter constraints applied,
- **And** the divergence from ISO §7.2.5 is documented as deliberate,
- **And** the library is covered by tests rather than by the policy class's existing assertions
alone,
- **And** a certificate-backed two-predicate request that Story 4.3 admits is not refused
downstream, asserted end to end with Story 4.3's fixture,
- **And** the divergence from ISO §7.2.5 is recorded against spec §22 item 21,

**Covers:** FR51. **Source:** O6 proposed capability; spec §22 divergence; `EE-POA-027`–`EE-POA-031`.

## Analysis and implementation boundaries

Define a decision table for requested, held-true, false, unheld and accepted-circuit predicates before implementation. G13 must specify when nearest-true selection is authorized versus forbidden substitution. Preserve the registered two-predicate exception at request level.

**Prerequisites for full acceptance:** [Story 4.3](4-3-certificate-driven-two-predicate-decision.md), [Story 9.1](9-1-statutory-threshold-to-circuit-map.md), [Story 9.2](9-2-accepted-circuit-set-per-threshold.md)

**Decision/evidence gate:** G12 scope and G13 nearest-true-above versus 9.5's no-neighbor rule need an explicit shared decision.

**External acceptance boundary:** No new legal or circuit authority can be inferred from a negotiation algorithm.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt)
- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgeThresholdNegotiator.kt`

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

Table-test no true answer, exact answer, higher held-true answer, unavailable circuit and authorized two-predicate request. Assert no false transmission, no request-level cardinality bypass and the approved G13 semantics end to end.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
