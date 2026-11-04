---
story_id: "3.5"
story_key: 3-5-four-estonian-acceptance-cases
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.1", "3.3", "3.6", "4.7", "5.2", "5.3"]
decision_gate: "G4 unattested-request expectations must distinguish missing registration from unauthenticated transport."
external_gate: "Foreign PID and AV candidates for actual interoperability evidence; local fixtures establish only harness behavior."
source_contract_sha256: 2870e6043fad3211143c143ffbd07201436b8b91296de7895f0b42b8fc22c650
---

# Story 3.5: Four Estonian acceptance cases

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 3.5](../../planning-artifacts/epics.md#story-35-four-estonian-acceptance-cases).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 3.5: Four Estonian acceptance cases

As an Estonian service owner,
I want the four requests a real service will meet executed with machine-readable results,
So that I can demonstrate acceptance before the national wallet exists.

**Acceptance Criteria:**

- **Given** the RP kit, a foreign wallet with a PID, a foreign wallet with an AV attestation, an
unattested ZK request, and a request from an unregistered relying party,
- **When** the four cases run,
- **Then** each produces a documented, machine-readable result,
- **And** the missing-registration and unattested-request cases refuse,
- **And** a fifth case is recorded for the state two faithful implementations of FR22 and FR23
would otherwise deadlock on: the verifier resolves circuits from its pinned local fallback while
the wallet has no fetched registry, so the wallet's resolution falls back to the same pinned
document version rather than refusing the ZK path while the verifier waits,
- **And** the least-trusted unattested case follows the policy that `EE-ZKP-055a` already governs.
- **And** the two remaining unauthenticated shapes `EE-ZKP-055a` governs are covered as well as
the least-trusted one: an `org-iso-mdoc` request without `readerAuth` chaining to a trusted access
CA, and a proximity request without `readerAuth`, each refusing a plain-mdoc-only request on a
ZK-capable device,
**Covers:** FR13. **Source:** O2 acceptance criteria (i); U5; L2 verification method.

## Analysis and implementation boundaries

Retain the four named cases and separately identify the offline-registry fifth case and three unauthenticated transport shapes. The fifth case can be staged until Epic 5 exists; do not claim it passed early. G4 prevents treating every unauthenticated ZK-capable request as intrinsically forbidden when EE-ZKP-055a instead forbids plain-only responses.

**Prerequisites for full acceptance:** [Story 3.1](3-1-deployable-rp-service-profile.md), [Story 3.3](3-3-fail-closed-rp-register-interface-and-mock.md), [Story 3.6](3-6-refusal-distinguishable-from-cancellation.md), [Story 4.7](4-7-plain-fallback-visibility-guard.md), [Story 5.2](5-2-verifier-fetch-with-pinned-key-validation.md), [Story 5.3](5-3-wallet-scheme-resolution-from-the-registry.md)

**Decision/evidence gate:** G4 unattested-request expectations must distinguish missing registration from unauthenticated transport.

**External acceptance boundary:** Foreign PID and AV candidates for actual interoperability evidence; local fixtures establish only harness behavior.

## Existing entry points

- [tests/e2e.sh](../../../tests/e2e.sh)
- [verifier/go/oid4vp/carrier.go](../../../verifier/go/oid4vp/carrier.go)
- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)
- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `rp-kit/tests/acceptance`
- `rp-kit/acceptance-cases.md`

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

Assert case id, identity, observed outcome and reason in machine results. Unregistered identity must refuse; unauthenticated plain-only requests on ZK-capable devices refuse across DC API, org-iso-mdoc and proximity. Test same pinned registry version on both sides with network absent.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
