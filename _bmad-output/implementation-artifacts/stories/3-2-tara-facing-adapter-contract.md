---
story_id: "3.2"
story_key: 3-2-tara-facing-adapter-contract
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.1"]
decision_gate: null
external_gate: "RIA agreement, reachable TARA test environment and provisioned test client."
source_contract_sha256: b6cf9e51e0e58b6f2603f41064fdd7bca60e418f516b8ac569d3c36bfb7aa951
---

# Story 3.2: TARA-facing adapter contract

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.2](../../planning-artifacts/epics.md#story-32-tara-facing-adapter-contract).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.2: TARA-facing adapter contract

As an integrator wiring a state e-service,
I want a TARA-facing adapter contract with an integration test against a TARA test client,
So that the authentication leg is defined rather than assumed.

**Acceptance Criteria:**

- **Given** TARA's OIDC/OAuth2 surface and the requirement that joining needs an agreement with
RIA,
- **When** the adapter contract is implemented,
- **Then** an integration test runs against a TARA test client,
- **And** the contract records where RIA agreement is a prerequisite rather than hiding it,
- **And** the wallet-facing path is documented as a parallel path, not as something inside TARA.
- **And** TARA unreachable, or the RIA agreement absent at run time, fails the adapter closed and
records the integration test as blocked rather than passed,
**Covers:** FR10. **Source:** E1; §6 integration pathways (TARA row) `[REC]`.

## Analysis and implementation boundaries

Specify the e-service's OIDC authentication leg alongside the wallet presentation leg. Record client registration, callback correlation and fail-closed error handling without inventing a RIA API or putting OpenID4VP inside TARA.

**Prerequisites for full acceptance:** [Story 3.1](3-1-deployable-rp-service-profile.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** RIA agreement, reachable TARA test environment and provisioned test client.

## Existing entry points

- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)
- [verifier/go/main.go](../../../verifier/go/main.go)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `rp-kit/TARA-ADAPTER.md`
- `rp-kit/tests/tara-integration`

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

Run a real provisioned-client integration when available; failed authentication must not be upgraded by a valid age proof. Missing agreement/client/service is BLOCKED, with mocks clearly labeled as contract-only tests.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
