---
story_id: "4.4"
story_key: 4-4-topic-19-log-fields-from-the-certificate
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["4.2"]
decision_gate: null
external_gate: null
source_contract_sha256: d8802c22034fc10cfae8904cb90a21eef6303f1c30edc5e9c263e2e7db1bf7c7
---

# Story 4.4: Topic 19 log fields from the certificate

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 4.4](../../planning-artifacts/epics.md#story-44-topic-19-log-fields-from-the-certificate).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 4.4: Topic 19 log fields from the certificate

As an auditor reading a presentation log,
I want the ARF Topic 19 relying-party fields populated from the certificate and no attribute values recorded,
So that the log satisfies the documented logging duty without becoming a disclosure surface.

**Acceptance Criteria:**

- **Given** a certificate-backed presentation,
- **When** the log entry is written,
- **Then** the DASH_03 fields are populated from the certificate,
- **And** a test asserts each field individually,
- **And** no attribute value appears anywhere in the log (`DASH_03a`).
- **And** a named field that the certificate does not carry is logged with an explicit absent
marker, never left blank, and each field is asserted individually for the absent case,
**Covers:** FR19. **Source:** A3 required change and verification method; O3 acceptance criteria (iii).

## Analysis and implementation boundaries

Snapshot permitted RP metadata from validated certificate data at parse time; retain no certificate cache. Enumerate DASH_03 fields from the pinned primary ARF text rather than guessing field names. Use an explicit absent marker for optional missing data.

**Prerequisites for full acceptance:** [Story 4.2](4-2-registration-certificate-parsing.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt)
- [docs/decisions/ADR-005-INTENDED-USE-SOURCE.md](../../../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/PRESENTATION-LOG-FIELDS.md`

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

Assert every mapped field individually, including absence. Search serialized records for attribute values and raw certificate/credential contents; metadata identity is not permission to record the presented predicate value.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
