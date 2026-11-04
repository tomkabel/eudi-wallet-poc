---
story_id: "2.3"
story_key: 2-3-version-labelled-carrier-fixtures
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["2.1"]
decision_gate: null
external_gate: null
source_contract_sha256: 5c48a053555552fd415513830914af4927976f6fcd84663d7012c937d15cc0b6
---

# Story 2.3: Version-labelled carrier fixtures

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 2.3](../../planning-artifacts/epics.md#story-23-version-labelled-carrier-fixtures).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 2.3: Version-labelled carrier fixtures

As a maintainer watching ISO/IEC 18013-5 second edition,
I want every carrier fixture and profile rule labelled with the draft or document version it was built from,
So that a published edition that diverges is a relabelling job rather than an untraceable breakage.

**Acceptance Criteria:**

- **Given** the carrier fixtures and the carrier profile v1.0,
- **When** the labelling is applied,
- **Then** each fixture and each profile rule states the external document version it derives
from,
- **And** the labels are asserted by a test, and the assertion checks each label equals the
*current* carrier-profile version, so a superseded label fails rather than passing as labelled,
- **And** the assertion covers the rules in `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md` as
well as the fixtures, so a new profile rule cannot be added unlabelled.

**Covers:** FR40. **Source:** M2 exit criteria; risk R1 mitigation; O5 risks.

## Analysis and implementation boundaries

Separate the local carrier-profile version from the external source revision; record both on fixtures and rules. Preserve committed binary bytes; determine current local profile version from the profile itself rather than hardcoding the story's historical v1.0 label.

**Prerequisites for full acceptance:** [Story 2.1](2-1-carrier-runner-over-existing-cases.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [tests/vectors/carrier-v1/manifest.json](../../../tests/vectors/carrier-v1/manifest.json)
- [verifier/go/carrier_vectors_test.go](../../../verifier/go/carrier_vectors_test.go)
- [docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md](../../../docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `tests/check_carrier_versions.py`

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

Change a fixture label, remove a rule label and bump the profile without refreshing fixtures; each must fail read-only validation. A filename containing a version is insufficient evidence.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
