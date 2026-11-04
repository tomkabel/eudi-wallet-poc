---
story_id: "10.4"
story_key: 10-4-revocation-end-to-end-with-a-revoked-fixture
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G14 distinguish credential status-list policy from certificate CRL soft-fail and keep age attestations outside unsupported revocation scope."
external_gate: "Real status-list endpoint and suitable revocable test credential; synthetic controls remain local evidence."
source_contract_sha256: c0c1429713c92be6903bc9704c21aa947c606b6d1fa94c31b900af846e0c83d8
---

# Story 10.4: Revocation end-to-end with a revoked fixture

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 10.4](../../planning-artifacts/epics.md#story-104-revocation-end-to-end-with-a-revoked-fixture).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 10.4: Revocation end-to-end with a revoked fixture

As a relying party,
I want an end-to-end status-check demonstration against a real list with a revoked fixture,
So that revocation is exercised rather than split into partial handling.

**Acceptance Criteria:**

- **Given** status-list handling is split out and CRL soft-fail is the documented policy,
- **When** the demonstration runs,
- **Then** a revoked fixture is refused end to end,
- **And** a certificate named in a verified CRL is refused, while a CRL that cannot be fetched is
logged only,
- **And** a CRL past its `nextUpdate` is neither treated as current nor silently ignored,
- **And** the policy's flip condition is stated in the demonstration's record.

**Covers:** FR58. **Source:** S11 required change and verification method.

## Analysis and implementation boundaries

Choose a credential that actually carries status metadata; PRD NG-11 says the age attestations do not. Demonstrate status revocation and certificate CRL behavior as separate mechanisms, with source pins, nextUpdate and the documented policy flip condition.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G14 distinguish credential status-list policy from certificate CRL soft-fail and keep age attestations outside unsupported revocation scope.

**External acceptance boundary:** Real status-list endpoint and suitable revocable test credential; synthetic controls remain local evidence.

## Existing entry points

- [statuslists/](../../../statuslists/)
- [app/src/main/kotlin/ee/cyber/wallet/security/](../../../app/src/main/kotlin/ee/cyber/wallet/security/)
- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)
- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/interop/REVOCATION-END-TO-END.md`
- `tests/interop/revocation/`

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

Exercise good/revoked credential, revoked certificate, unavailable CRL and stale CRL, recording each policy. Never apply CRL soft-fail as an unstated rule for credential status lists or add an age-status mechanism through this story.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
