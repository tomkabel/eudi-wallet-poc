---
story_id: "6.1"
story_key: 6-1-on-device-proving-measurements
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G8 sample sufficiency for cold/warm percentiles and device capability consequences."
external_gate: "Pixel 8 and a named mid-range physical device with a matching source APK."
source_contract_sha256: 7f2d73c3b6fac6a9d8b7aec44ead9c4440f56cc9e2d34f9d04f5957b0d0710f8
---

# Story 6.1: On-device proving measurements

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 6.1](../../planning-artifacts/epics.md#story-61-on-device-proving-measurements).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 6.1: On-device proving measurements

As a programme evaluating the wallet on real hardware,
I want p50/p95/p99 approval-to-proof-ready, cold and warm, plus peak memory, reported per device,
So that the pending figures stop being pending and the specification's device-side prover budgets
(`EE-ZKP-040`: ≤ 2.0 s p95 and ≤ 3.5 s p99 approval-to-proof-ready; `EE-ZKP-041`: ≤ 250 MB peak)
are checked on real hardware.

**Acceptance Criteria:**

- **Given** the committed `ZkProofBenchmark.kt` harness and the named device set (a Pixel 8 and
one mid-range device),
- **When** the programme is run,
- **Then** figures are reported per device, cold and warm, with `VmHWM` peak memory,
- **And** the results are assessed against `EE-ZKP-040`'s device-side prover budgets
(approval-to-proof-ready ≤ 2.0 s p95 and ≤ 3.5 s p99) and `EE-ZKP-041`'s ≤ 250 MB peak-memory
bound, and a miss is recorded as a miss in `docs/MEASUREMENTS.md` with the device and date named,
- **And** any row that remains unmeasured is left explicitly `PENDING-DEVICE` with a dated reason,
- **And** `docs/MEASUREMENTS.md` is updated with the method, the baseline and the confidence.
- **And** a device whose figures exceed the budgets has a pinned consequence rather than a note:
proximity Tier 2 is withheld for that device and `docs/MEASUREMENTS.md` records the miss, asserted
by the harness threshold check,
**Blocked by (external):** a Pixel 8 and one mid-range device. Where the external party or
hardware is unavailable, the story still closes its in-repository half and records the state as
blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR26. **Source:** A1 required change; U3 dependencies; §7.1 benchmark 1; Q4; D2 deliverable.

## Analysis and implementation boundaries

Wire the existing statistics harness to the actual device prover; retain approval-to-ready timestamps, raw samples and VmHWM. Separate cold/warm populations and report insufficient-tail sample sizes rather than interpreting five cold runs as meaningful p99 evidence.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G8 sample sufficiency for cold/warm percentiles and device capability consequences.

**External acceptance boundary:** Pixel 8 and a named mid-range physical device with a matching source APK.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/measurement/ZkProofBenchmark.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/measurement/ZkProofBenchmark.kt)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `app/src/androidTest/kotlin/ee/cyber/wallet/measurement/ZkProofBenchmarkRunner.kt`
- `docs/measurements/DEVICE-PROVING.md`

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

Check the spec's 2.0 s p95, 3.5 s p99 and 250 MB bounds per device; keep misses as misses and unrun rows PENDING-DEVICE. Verify any device eligibility consequence in a controlled configuration without claiming a proximity ZK transport now exists.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
