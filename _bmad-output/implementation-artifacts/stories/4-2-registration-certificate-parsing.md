---
story_id: "4.2"
story_key: 4-2-registration-certificate-parsing
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["4.1", "4.6", "4.5"]
decision_gate: "G2 pin a certificate profile and authenticated registration binding; G3 define comparison evidence for register/certificate mismatch."
external_gate: "A production national access-certificate profile is not established by synthetic fixtures."
source_contract_sha256: 2877d787423510d0712ad9907a196f09975d43c3b79e3b68dadc0c2c1c0a87be
---

# Story 4.2: Registration-certificate parsing

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 4.2](../../planning-artifacts/epics.md#story-42-registration-certificate-parsing).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 4.2: Registration-certificate parsing

As a wallet that must act on the relying party's registered intended use,
I want the registration/access certificate chain parsed into a typed `RegisteredIntendedUse`,
So that the wallet can distinguish a lawful request from an over-reaching one.

**Acceptance Criteria:**

- **Given** a registration/access certificate chain,
- **When** it is parsed,
- **Then** a typed `RegisteredIntendedUse` is produced, carrying the relying party's trade name,
unique identifier, service identifier, Member State, intended use and privacy-policy URL,
- **And** an unparseable certificate yields refusal rather than a partial value,
- **And** the certificate profile version targeted is recorded, because the ARF annexes are still
moving.
- **And** a certificate whose profile version is newer than the version the parser targets is
refused with `version-unsupported` rather than partially parsed as the older profile,
**Covers:** FR17. **Source:** O3 proposed capability (a)–(b); A3; S10; O3 risks.

## Analysis and implementation boundaries

Extract the six-field RegisteredIntendedUse only after signature, chain, time and profile validation. Pin the in-repo ARF gitlink and a comparable profile version label. G3 is unresolved because the wallet must not add a timing-leaking register lookup merely to compare two sources.

**Prerequisites for full acceptance:** [Story 4.1](4-1-intendedusesource-interface.md), [Story 4.6](4-6-certificate-fixture-generator.md), [Story 4.5](4-5-fail-closed-defaults-and-certificate-property-tests.md)

**Decision/evidence gate:** G2 pin a certificate profile and authenticated registration binding; G3 define comparison evidence for register/certificate mismatch.

**External acceptance boundary:** A production national access-certificate profile is not established by synthetic fixtures.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt](../../../app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt)
- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/RegistrationCertificateParser.kt`
- `docs/profiles/ACCESS-CERTIFICATE-PROFILE.md`

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

Refuse malformed, partial, untrusted, tampered, expired, not-yet-valid, revoked and unsupported-version inputs. Test the explicit compatibility policy for older labels; a future label never falls through into partial parsing.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
