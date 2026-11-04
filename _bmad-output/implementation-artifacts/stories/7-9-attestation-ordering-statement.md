---
story_id: "7.9"
story_key: 7-9-attestation-ordering-statement
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: bca242a07e57c3351c7643cbbc86140159885b42eca61f2a76ffa91018ea40a1
---

# Story 7.9: Attestation ordering statement

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.9](../../planning-artifacts/epics.md#story-79-attestation-ordering-statement).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.9: Attestation ordering statement

As an Estonian programme deciding which attestation to prefer,
I want the `eu.europa.ec.av.1`-first ordering documented together with the `ee.riik.poa.1` fallback,
So that the interoperability choice and its political tradeoff are both visible.

**Acceptance Criteria:**

- **Given** the spec's existing choice to prefer the EU AV attestation for every relying party,
- **When** the ordering statement is written,
- **Then** it records both paths, the ordering, and the reason the ordering favours
interoperability,
- **And** it names the risk that an Estonian programme may want its own credential first,
- **And** both paths remain implemented.

**Covers:** FR38. **Source:** A6 tradeoff 2.

## Analysis and implementation boundaries

Record EU-AV-first ordering subject to request acceptance and a matching threshold, as EE-POA-024 actually conditions it. Retain EE-PoA fallback and describe the national-preference tradeoff without asserting a programme's adoption choice.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)
- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/AGE-ATTESTATION-ORDERING.md`

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

Trace selection behavior to source/tests for both doctypes, unsupported threshold and RP rejection of AV. The statement must not promise unconditional AV preference when the request does not accept it.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
