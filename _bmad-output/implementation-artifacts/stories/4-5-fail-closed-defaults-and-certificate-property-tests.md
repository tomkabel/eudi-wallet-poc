---
story_id: "4.5"
story_key: 4-5-fail-closed-defaults-and-certificate-property-tests
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["4.1", "4.6"]
decision_gate: "G2 pins the eventual parser inputs; tests must land before 4.2's parser."
external_gate: "Revocation fixtures are synthetic, not live revocation evidence."
source_contract_sha256: ba41291184b69e1209da240d776f1b6925ab91aa98925fb8c35678cb371d1cf0
---

# Story 4.5: Fail-closed defaults and certificate property tests

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 4.5](../../planning-artifacts/epics.md#story-45-fail-closed-defaults-and-certificate-property-tests).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 4.5: Fail-closed defaults and certificate property tests

As a privacy engineer,
I want refusal to be the default whenever the certificate is absent, unparseable or names no use, proven by property tests,
So that a parsing error cannot quietly loosen a privacy control.

**Acceptance Criteria:**

- **Given** generated certificates including malformed, empty-intended-use and absent cases,
- **When** the property tests run,
- **Then** every such case refuses,
- **And** the default-to-refusal behaviour is asserted independently of the happy path,
- **And** the existing 169 Kotlin unit tests show no regression.
- **And** the property-test set includes the expired, revoked and untrusted-anchor certificates
the generator mints, and each is asserted to refuse,
**Covers:** FR20. **Source:** O3 acceptance criteria (iv); risk R8 mitigation; O3 validation.

## Analysis and implementation boundaries

Establish generated fail-closed source tests before parser implementation, using a rejecting fake at that boundary. Extend the same properties to the real parser when 4.2 lands; this staged order avoids a dependency cycle. Historical 169-test wording is not the current test inventory.

**Prerequisites for full acceptance:** [Story 4.1](4-1-intendedusesource-interface.md), [Story 4.6](4-6-certificate-fixture-generator.md)

**Decision/evidence gate:** G2 pins the eventual parser inputs; tests must land before 4.2's parser.

**External acceptance boundary:** Revocation fixtures are synthetic, not live revocation evidence.

## Existing entry points

- [app/src/test/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicyTest.kt](../../../app/src/test/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicyTest.kt)
- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `app/src/test/kotlin/ee/cyber/wallet/domain/presentation/IntendedUsePropertyTest.kt`

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

Generate absent, empty-use, malformed, expired, future, revoked, tampered, untrusted and future-version cases with reproducible seeds. Assert refusal independently of happy-path success and retain minimized failing inputs.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
