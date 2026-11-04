---
story_id: "10.1"
story_key: 10-1-real-issuer-integration
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 re-read pinned issuer library version and current service protocol."
external_gate: "Reachable Commission issuer, documented test identity/access and service revision."
source_contract_sha256: 7fe3c68da191191f33bf5ec85d10dd01d42cb503135858922115f8f7863ff972
---

# Story 10.1: Real issuer integration

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 10.1](../../planning-artifacts/epics.md#story-101-real-issuer-integration).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 10.1: Real issuer integration

As an integrator,
I want issuance exercised against the Commission's issuer service rather than a mock,
So that the issuance framework claim is backed by interop evidence.

**Acceptance Criteria:**

- **Given** the pinned `eudi-lib-jvm-openid4vci-kt` 0.6.1 and the mock issuance paths,
- **When** the integration test runs against the Commission's issuer service,
- **Then** the result is recorded with the service's revision and environment,
- **And** the requirements matrix's issuance row is updated from its evidence,
- **And** the mock paths remain available for offline tests.
- **And** the Commission's issuer service being unavailable or absent at test time is recorded as
blocked with the reason, so an unreachable service never leaves the interop claim unstated,
**Blocked by (external):** the Commission's issuer service, available and reachable. Where the
external party or hardware is unavailable, the story still closes its in-repository half and
records the state as blocked with the reason and date — never as passed, and never with a
fabricated result.
**Covers:** FR55. **Source:** L4 required change and verification method; S2 required change.

## Analysis and implementation boundaries

Preserve offline mock paths while defining explicit opt-in real-service configuration. Derive the current library pin from the tree rather than trusting the backlog's 0.6.1 premise; record credentials, protocol and environment prerequisites without storing secrets.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 re-read pinned issuer library version and current service protocol.

**External acceptance boundary:** Reachable Commission issuer, documented test identity/access and service revision.

## Existing entry points

- [gradle/libs.versions.toml](../../../gradle/libs.versions.toml)
- [app/src/main/kotlin/ee/cyber/wallet/domain/provider/](../../../app/src/main/kotlin/ee/cyber/wallet/domain/provider/)
- [docs/profiles/AVSUA-ISSUANCE.md](../../../docs/profiles/AVSUA-ISSUANCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/interop/REAL-ISSUER.md`
- `tests/interop/issuer/`

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

Run actual issuance and validate the returned attestation/bindings; report unavailable service as BLOCKED and protocol rejection as a real outcome. Keep real evidence separate from AVSUA-equivalent local issuance.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
