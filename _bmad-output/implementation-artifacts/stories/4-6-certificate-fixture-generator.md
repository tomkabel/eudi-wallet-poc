---
story_id: "4.6"
story_key: 4-6-certificate-fixture-generator
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["4.1"]
decision_gate: "G2 choose and pin the synthetic certificate profile before claiming external compatibility."
external_gate: "No production CA or private repository input is needed for synthetic generation."
source_contract_sha256: 3c6085f1d5c9e581a1c7173a402468415bd25cecaefb44841ae04ef854660c0a
---

# Story 4.6: Certificate fixture generator

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 4.6](../../planning-artifacts/epics.md#story-46-certificate-fixture-generator).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 4.6: Certificate fixture generator

As the maintainer of the certificate policy,
I want a generator that mints the registration-certificate fixtures every Epic 4 test depends on,
So that Story 4.5's property tests have inputs and the fail-closed suite exists before the parsing
it guards.
**Acceptance Criteria:**
- **Given** the corpus names a fixture generator second in O3's implementation approach and no
story delivered one,
- **When** the generator is built,
- **Then** it mints at least: a valid certificate naming one predicate; one naming two; one naming
no intended use; a malformed one; one with an untrusted anchor; a tampered signature; an expired
one; a not-yet-valid one; one revoked; and one whose profile version exceeds the version the
parser targets,
- **And** the generator is pinned by a drift test against the committed fixture bytes,
- **And** Story 4.5's property tests consume it, and the fail-closed default (Story 4.1) is
asserted before Story 4.2's parsing exists, so the guards land before the code they guard,
- **And** the fixtures are added to `tests/README.md` with what each one proves,
**Covers:** FR17, FR20. **Source:** O3 implementation approach ("the certificate implementation
and a fixture generator second"); risk R8 mitigation; review finding 8.

## Analysis and implementation boundaries

Generate every case in the backlog from a single versioned registration dataset shared with the RP mock. Include doctype-qualified pairs and reproducible test-only signing keys, fixed times and serials; never use demo-data keys.

**Prerequisites for full acceptance:** [Story 4.1](4-1-intendedusesource-interface.md)

**Decision/evidence gate:** G2 choose and pin the synthetic certificate profile before claiming external compatibility.

**External acceptance boundary:** No production CA or private repository input is needed for synthetic generation.

## Existing entry points

- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)
- [docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md](../../../docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `tools/build_registration_fixtures.py`
- `tests/vectors/registration-certificates/`

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

Read-only regeneration must match committed bytes and detect changed profile/source pins. Show the unconfigured-source guard rejects before the parser exists; fixtures labeled revoked need a verifiable synthetic revocation artifact.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
