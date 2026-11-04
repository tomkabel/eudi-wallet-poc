---
story_id: "7.6"
story_key: 7-6-ts13-clause-map
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 pin the actual TS13 source/revision before asserting constructs or handover status."
external_gate: "Primary TS13 text and any cited ETSI successor evidence."
source_contract_sha256: 570860a651cf3f30260c870433ccffad7ed259d46070e6d9ca619b1d2dd7aa53
---

# Story 7.6: TS13 clause map

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.6](../../planning-artifacts/epics.md#story-76-ts13-clause-map).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.6: TS13 clause map

As an implementer,
I want a clause map against EC TS13,
So that `mso_mdoc_zk` support and the `zk-jwt` gap are both visible instead of implied.

**Acceptance Criteria:**

- **Given** TS13 v1.0 (`mso_mdoc_zk`, `zk-jwt`, `ZkSystemSpec`, `ZkDocument`,
`longfellow-libzk-v1`, `circuit_hash`),
- **When** the clause map is produced,
- **Then** `mso_mdoc_zk` is marked supported with its evidence in `verifier/go/oid4vp/dcql.go` and
`DeviceRequestParser.kt`,
- **And** `zk-jwt` is marked missing,
- **And** the map records that TS13 states it is not intended to be a final specification and
hands over to ETSI TS 119 476-2.
- **And** every TS13 construct the map covers — `ZkSystemSpec`, `ZkDocument` and `circuit_hash` as
well as the two formats — carries a stated marking, not only the formats,
**Covers:** FR35. **Source:** S7; O1 deliverables note.

## Analysis and implementation boundaries

Map formats and ZkSystemSpec, ZkDocument, system identifiers and circuit_hash separately. Distinguish an accepted parameter from complete carrier support; record zk-jwt as missing unless direct current evidence changes that result.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 pin the actual TS13 source/revision before asserting constructs or handover status.

**External acceptance boundary:** Primary TS13 text and any cited ETSI successor evidence.

## Existing entry points

- [verifier/go/oid4vp/dcql.go](../../../verifier/go/oid4vp/dcql.go)
- [app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt](../../../app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt)
- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/conformity/TS13-CLAUSE-MAP.md`

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

Check every construct has a status, source clause and evidence scope; confirm the source's maturity/hand-over wording directly or mark it UNVERIFIED. A local unofficial carrier does not become a mandated TS13 transport.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
