---
story_id: "11.7"
story_key: 11-7-cross-border-onboarding-without-a-national-identifier
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["10.1", "3.5"]
decision_gate: "G18 reconcile multi-credential onboarding with ADR-004 deployment-profile exclusions."
external_gate: "Real PID/AV credentials and any required cross-border candidate; real interop cannot be replaced by fixtures."
source_contract_sha256: 0c40977b331f14354233dd91b1099ae6ad034b0a0aad0c713db950e617724620
---

# Story 11.7: Cross-border onboarding without a national identifier

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.7](../../planning-artifacts/epics.md#story-117-cross-border-onboarding-without-a-national-identifier).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.7: Cross-border onboarding without a national identifier

As an e-resident or cross-border holder,
I want a service to request a PID attribute subset plus an AV attestation in one session,
So that onboarding completes without the isikukood or full date of birth being disclosed.

**Acceptance Criteria:**

- **Given** multi-credential sessions are supported and verified by tests, and real PID issuance
is Story 10.1,
- **When** the onboarding flow is exercised,
- **Then** the session's log names both credential ids,
- **And** a missing credential fails the session with the id named,
- **And** no isikukood appears anywhere in the wire transcript.
- **And** a credential held in a form the wallet cannot present is reported as unpresentable and
named, distinct from a credential that is absent,
**Covers:** FR65. **Source:** U4 workflow, dependencies and acceptance criteria.

## Analysis and implementation boundaries

Define requested PID subset and AV proof, forbidding isikukood and full birth date on wire. Resolve deployment placement first: ADR-004 excludes multi-credentials from the supported RP profile while this story requires them, so G18 must decide isolated harness versus approved profile extension.

**Prerequisites for full acceptance:** [Story 10.1](10-1-real-issuer-integration.md), [Story 3.5](3-5-four-estonian-acceptance-cases.md)

**Decision/evidence gate:** G18 reconcile multi-credential onboarding with ADR-004 deployment-profile exclusions.

**External acceptance boundary:** Real PID/AV credentials and any required cross-border candidate; real interop cannot be replaced by fixtures.

## Existing entry points

- [verifier/go/present.go](../../../verifier/go/present.go)
- [verifier/go/oid4vp/](../../../verifier/go/oid4vp/)
- [tests/e2e.sh](../../../tests/e2e.sh)
- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/interop/CROSS-BORDER-ONBOARDING.md`
- `rp-kit/tests/cross-border/`

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

Assert both credential ids in local result metadata, missing versus unpresentable distinctions and no prohibited attributes in decoded plaintext and transport artifacts. Correlation ids are not a reason to retain holder identifiers centrally.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
