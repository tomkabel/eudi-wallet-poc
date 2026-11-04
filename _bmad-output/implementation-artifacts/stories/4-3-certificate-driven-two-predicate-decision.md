---
story_id: "4.3"
story_key: 4-3-certificate-driven-two-predicate-decision
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["4.2", "4.5"]
decision_gate: "G2/G3 certificate trust and registration comparison contract."
external_gate: "No external run is needed for fixture-backed policy tests."
source_contract_sha256: 4ab6533f217bc5db7b3673b1bb971c6995382e3d9e5af4bda3526e80713ca9c7
---

# Story 4.3: Certificate-driven two-predicate decision

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 4.3](../../planning-artifacts/epics.md#story-43-certificate-driven-two-predicate-decision).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 4.3: Certificate-driven two-predicate decision

As a relying party with a legitimate two-predicate use case,
I want a request naming both predicates in my registered intended use to be allowed, and everything else refused,
So that a precise legal rule replaces a blanket refusal.

**Acceptance Criteria:**

- **Given** a two-predicate request bearing a certificate whose intended use names both
predicates,
- **When** the request is presented,
- **Then** it is allowed and the user sees an explanation of the revealed range,
- **And** the same request without a certificate, or with one not naming both, is refused,
- **And** the loosening path is covered by explicit negative tests.
- **And** a certificate naming both predicates does not override the held-false rule: where the
wallet holds one predicate false it is withheld and the decision is recorded, so the loosened path
cannot transmit a false predicate,
- **And** a request naming three or more predicates is refused explicitly — the rule keys on the
registered intended use naming *each* requested predicate, and the upper bound is stated rather
than implied,
**Covers:** FR18. **Source:** O3 acceptance criteria (i)–(ii); E2; risk R8.

## Analysis and implementation boundaries

Authorize exactly two requested predicates only when a validated intended use names both doctype-qualified fields across the request. Keep the upper bound explicit, do not widen the circuit's proof capacity, and explain any revealed range before consent.

**Prerequisites for full acceptance:** [Story 4.2](4-2-registration-certificate-parsing.md), [Story 4.5](4-5-fail-closed-defaults-and-certificate-property-tests.md)

**Decision/evidence gate:** G2/G3 certificate trust and registration comparison contract.

**External acceptance boundary:** No external run is needed for fixture-backed policy tests.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt)
- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/AGE-PREDICATE-POLICY.md`

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

Cover matching certificate, one missing predicate, wrong doctype, cross-credential pairs, three predicates, and one held-false predicate. Assert false is withheld on the wire even when both predicates are registered.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
