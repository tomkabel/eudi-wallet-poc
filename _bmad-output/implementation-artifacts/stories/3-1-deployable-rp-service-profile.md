---
story_id: "3.1"
story_key: 3-1-deployable-rp-service-profile
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 99d5b82978151271f34e5cfb87e145a735c2c7e461a65180c607b14a0932f724
---

# Story 3.1: Deployable RP service profile

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.1](../../planning-artifacts/epics.md#story-31-deployable-rp-service-profile).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.1: Deployable RP service profile

As an Estonian e-service operator,
I want a deployable relying-party service profile built from the Go verifier,
So that I have something to run acceptance tests against instead of a library to assemble.

**Acceptance Criteria:**

- **Given** the existing `verifier/go` service,
- **When** the `rp-kit/` deployment profile is produced,
- **Then** it starts from the documented commands with no undocumented step,
- **And** the deployment profile's decisions are recorded as an ADR before implementation,
- **And** `direct_post.jwt` remains the default with plain `direct_post` behind the explicit flag.
- **And** a session whose `response_uri` callback never arrives expires after the stated session
TTL and answers with the session id named, so the relying party is never left without a status,
**Covers:** FR9. **Source:** O2 proposed capability; O2 affected components; A3 P1 deliverable.

## Analysis and implementation boundaries

Follow the existing ADR-004 rather than reopening service packaging. Supply persisted response-key configuration, documented TTL/drain/admission flags and a service profile over zkverify. Extend the expired session response to name its id; do not add a wrapper service.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [verifier/go/main.go](../../../verifier/go/main.go)
- [verifier/go/present.go](../../../verifier/go/present.go)
- [tests/drain_test.sh](../../../tests/drain_test.sh)
- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `rp-kit/README.md`
- `rp-kit/zkverify.service`
- `rp-kit/environment.example`

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

Start from documented commands, expire a no-callback session and assert 410 with the session id. Verify restart/key permissions, drain behavior and encrypted default; exclude unsafe development flags from deployment configuration.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
