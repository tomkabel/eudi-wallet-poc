---
story_id: "11.6"
story_key: 11-6-certificate-transparency-for-access-certificates
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["4.2"]
decision_gate: "G17 pin CT protocol, trusted log policy, inclusion/SCT semantics and outage decision."
external_gate: "Authoritative access-certificate CT profile/log material if available; national adoption remains unresolved."
source_contract_sha256: d158e0128f8f0afd6febd542077034d3ee80d3b4f3836797079ed473be2b8632
---

# Story 11.6: Certificate Transparency for access certificates

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.6](../../planning-artifacts/epics.md#story-116-certificate-transparency-for-access-certificates).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.6: Certificate Transparency for access certificates

As a relying party that trusts an access certificate,
I want SCT verification when access certificates are added, with CT test vectors,
So that the CT requirements are implemented rather than deferred.

**Acceptance Criteria:**

- **Given** `CT_01`–`CT_06` are missing and the current implementation validates chains and CRLs
only,
- **When** SCT verification is implemented,
- **Then** CT test vectors cover the accepted and rejected cases,
- **And** the open question of whether Estonian access certificates will be CT-logged is recorded
as unresolved,
- **And** the requirements matrix's CT rows are updated from their evidence.
- **And** an unreachable CT log leaves a stated acceptance behaviour — refusal or a logged
deferral — rather than an undefined one,
**Covers:** FR64. **Source:** A14 required change and verification method; §8 unresolved questions.

## Analysis and implementation boundaries

Map CT_01-CT_06 from the pinned primary source before implementing SCT checks. An SCT signature check and a live log inclusion lookup are distinct; specify what is required and what an unavailable log means without assuming Estonian certificates are logged.

**Prerequisites for full acceptance:** [Story 4.2](4-2-registration-certificate-parsing.md)

**Decision/evidence gate:** G17 pin CT protocol, trusted log policy, inclusion/SCT semantics and outage decision.

**External acceptance boundary:** Authoritative access-certificate CT profile/log material if available; national adoption remains unresolved.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt](../../../app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt)
- [eudi-arf/docs/](../../../eudi-arf/docs/)
- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/security/ACCESS-CERTIFICATE-CT.md`
- `tests/vectors/access-cert-ct/`

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

Use accepted/rejected/tampered/unknown-log/time-boundary vectors and a defined outage case. No live CT evidence or national adoption is inferred from synthetic vectors; keep unresolved profile decisions explicit.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
