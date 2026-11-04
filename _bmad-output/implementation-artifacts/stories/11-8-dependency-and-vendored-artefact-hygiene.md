---
story_id: "11.8"
story_key: 11-8-dependency-and-vendored-artefact-hygiene
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: "Private-repository imports require explicit written maintainer permission; this planning task imports none."
source_contract_sha256: 26b0721ad3d67a3c59c330f595a59a26fef14f84b6d5ed180e2ca83d4ad1c7e1
---

# Story 11.8: Dependency and vendored-artefact hygiene

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 11.8](../../planning-artifacts/epics.md#story-118-dependency-and-vendored-artefact-hygiene).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 11.8: Dependency and vendored-artefact hygiene

As a licensing and security reviewer,
I want every vendored or mirrored artefact inventoried with its upstream licence and revision, and pre-1.0 dependencies watched,
So that provenance and churn risk are both known.

**Acceptance Criteria:**

- **Given** the published Maven mirror under `.github/maven-mirror`, the vendored `coseJava`
mirror, and the pinned pre-1.0 dependencies (multipaz, the `eu.europa.ec.eudi` libraries) — the
corpus records the newer-mirror licence position as `[UNVERIFIED]`, so it is established during
this story rather than assumed,
- **When** the inventory is produced,
- **Then** every vendored artefact is listed with its upstream licence and revision,
- **And** the MIT notice is retained and no material from a private repository is imported without
written permission,
- **And** the pre-1.0 pins are recorded with the breakage-detection role of the conformance module
named.

**Covers:** FR48. **Source:** risk R11; risk R14.

## Analysis and implementation boundaries

Inventory committed/mirrored binary and source artifacts with origin revision, digest, license and local patches. Mark missing primary license provenance UNVERIFIED; retain notices and account for pinned pre-1.0 compatibility checks.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** Private-repository imports require explicit written maintainer permission; this planning task imports none.

## Existing entry points

- [.github/maven-mirror/](../../../.github/maven-mirror/)
- [gradle/libs.versions.toml](../../../gradle/libs.versions.toml)
- [verifier/zkverify-ffi/longfellow-rev.txt](../../../verifier/zkverify-ffi/longfellow-rev.txt)
- [eudi-arf/](../../../eudi-arf/)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/security/DEPENDENCY-AND-LICENSE-INVENTORY.md`

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

Reconcile inventory against actual files and lock/pin sources, detecting missing provenance and duplicate artifact versions. State conformance-module drift checks and upgrade ownership without repinning dependencies during documentation preparation.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
