---
story_id: "8.5"
story_key: 8-5-loa-high-authentication-with-a-zkp-presentation
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["3.2"]
decision_gate: null
external_gate: "Provisioned authentication service and evidence of actual assurance level, alongside a ZK-capable environment."
source_contract_sha256: b7280a713ed9a8a31a6bebd39d859f5bd4f1e751e54e7bd648240aa20b56d917
---

# Story 8.5: LoA-High authentication with a ZKP presentation

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.5](../../planning-artifacts/epics.md#story-85-loa-high-authentication-with-a-zkp-presentation).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.5: LoA-High authentication with a ZKP presentation

As a relying party that must preserve LoA High,
I want a test where a LoA-High authentication happens alongside a ZKP presentation,
So that `ZKP_09` moves from "compatible in principle" to tested.

**Acceptance Criteria:**

- **Given** TARA and Smart-ID/Mobile-ID as a second authentication route,
- **And** a route that is unavailable or an authentication that fails at demonstration time is
recorded as the demonstration's outcome, not left undefined,
- **When** the pilot test is run,
- **Then** a LoA-High authentication alongside a ZKP presentation is demonstrated,
- **And** the result is recorded with its environment and method,
- **And** the finding is reflected in the requirements matrix.

**Covers:** FR43. **Source:** A12 required change and verification method; §6 Smart-ID/Mobile-ID row.

## Analysis and implementation boundaries

Define session correlation between the authentication and presentation legs without placing wallet presentation inside TARA. Require evidence of the authentication's returned assurance level; the age predicate itself cannot confer LoA High.

**Prerequisites for full acceptance:** [Story 3.2](3-2-tara-facing-adapter-contract.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** Provisioned authentication service and evidence of actual assurance level, alongside a ZK-capable environment.

## Existing entry points

- [docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md](../../../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)
- [docs/research/2026-10-23-estonia-alignment-assessment.md](../../../docs/research/2026-10-23-estonia-alignment-assessment.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/analysis/LOA-HIGH-ZKP-DEMONSTRATION.md`

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

Exercise successful, failed, unavailable, mismatched-session and replayed authentication/presentation combinations. Record environment and actual assurance evidence; mock claims do not establish an external LoA-High demonstration.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
