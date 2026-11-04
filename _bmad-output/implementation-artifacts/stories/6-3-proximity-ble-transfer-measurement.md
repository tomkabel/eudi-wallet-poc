---
story_id: "6.3"
story_key: 6-3-proximity-ble-transfer-measurement
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["6.1"]
decision_gate: "G9 proximity Tier-2 transport is absent and excluded from v1; implementation requires a separate scope decision."
external_gate: "Physical wallet/terminal pair and actual proximity ZK transfer path."
source_contract_sha256: 314580d099287bcc76e0190faafbe2cd5688e7bd4aa1512616fb2dfd9bf7ce30
---

# Story 6.3: Proximity BLE transfer measurement

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 6.3](../../planning-artifacts/epics.md#story-63-proximity-ble-transfer-measurement).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 6.3: Proximity BLE transfer measurement

As a holder at a venue,
I want a Tier-2 proximity ZK presentation measured end to end over BLE,
So that a ~360 KB proof either fits a stated budget or is documented as not fitting one.

**Acceptance Criteria:**

- **Given** the proximity path and a ~360 KB proof,
- **When** the transfer is measured with the wallet's own timestamps,
- **Then** the measured transfer time is published against `EE-ZKP-044`'s end-to-end ≤ 4.0 s p95
budget (user approval to the relying party's accept/reject decision, inclusive of proving,
transfer and verification), which `EE-CNF-006` makes a precondition of proximity Tier 2,
- **And** a refusal is raised when a ZK response is impossible, with no silent downgrade to a
plain mdoc,
- **And** an over-budget or previously-measured-slow transport pair results in a Tier-2 retry or a
refusal — never a fall to Tier 1 — wherever the relying party's registration certificate asserts
ZKP capability (`EE-ZKP-045a`), and a pair is not recorded as exceeding the budget on a
measurement taken against such a relying party,
- **And** proximity Tier-2 stays marked optional if it remains unmeasured.
- **And** a transfer interrupted mid-way — a BLE disconnect or a truncated payload — discards the
partial payload and is recorded as refused or retried, never as a completed measurement,
**Covers:** FR28. **Source:** A8 required change; U3 workflow and acceptance criteria; §7.1 benchmark 3; risk R6.

## Analysis and implementation boundaries

Prepare a transfer protocol only after G9. Separate payload-only transport time from the inclusive approval-to-RP-decision budget. Honor EE-ZKP-045a's authenticated-capable-reader rule and do not silently downgrade during a failed ZK run.

**Prerequisites for full acceptance:** [Story 6.1](6-1-on-device-proving-measurements.md)

**Decision/evidence gate:** G9 proximity Tier-2 transport is absent and excluded from v1; implementation requires a separate scope decision.

**External acceptance boundary:** Physical wallet/terminal pair and actual proximity ZK transfer path.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt](../../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/measurements/PROXIMITY-ZK-TRANSFER.md`

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

Retain device pair, payload bytes, proving/transfer/verification spans, truncated-transfer failures and p95 end-to-end comparison to 4.0 s. Unimplemented or unmeasured Tier 2 stays optional and pending, not a completed measurement story.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
