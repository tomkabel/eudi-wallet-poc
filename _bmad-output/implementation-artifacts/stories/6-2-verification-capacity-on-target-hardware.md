---
story_id: "6.2"
story_key: 6-2-verification-capacity-on-target-hardware
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G8 preserve EE-ZKP-040's verifier bound alongside sizing; target instance and required rate must be chosen."
external_gate: "RP's named target hardware class and agreed peak-rate assumption."
source_contract_sha256: e4a40cd4405e266fef3bb85087b56e7b8c36daec2cd53b612779115bc5e451dd
---

# Story 6.2: Verification capacity on target hardware

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 6.2](../../planning-artifacts/epics.md#story-62-verification-capacity-on-target-hardware).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 6.2: Verification capacity on target hardware

As a relying party sizing an instance,
I want verification capacity measured on the instance type the relying party plans to run,
So that budgets are planned from measurement rather than invention.

**Acceptance Criteria:**

- **Given** `BenchmarkMultipazVerify` and the percentiles harness,
- **When** the benchmark is re-run on the relying party's planned instance type,
- **Then** a stated verifications-per-second figure with its percentiles is published for that
instance type,
- **And** the existing host figures (p50 ≈ 3.24–3.83 s, p95 ≈ 4.10–4.91 s on an i5-8365U; ≈ 2.7
core-seconds per verify, ≈ 1,300
verifies/hour/core on a quiet host) are published as a capacity statement for a named hardware class rather than as a missed device budget,
- **And** `EE-ZKP-040`'s verifier-side ≤ 1.0 s p95 is re-expressed as a hardware-class-named
per-verification latency plus a throughput figure the relying party provisions for, rather than as
one 1.0 s p95 spanning both the phone and the server, since the same requirement sets the device
prover's ≤ 2.0 s p95 separately,
- **And** the peak-rate assumption used to size anything is labelled as an assumption agreed with
the relying party.
- **And** the relying party's required rate is stated, and a published figure that falls below it
is marked as below requirement rather than presented as a neutral capacity figure,
**Covers:** FR27. **Source:** §7.1 benchmark 2; Q2; risk R5 mitigation; M3.

## Analysis and implementation boundaries

Run the existing verification benchmark/percentile harness on the intended instance. Publish latency, throughput, concurrency, CPU baseline and load. The story's re-expression as capacity must not erase the spec's separate 1.0 s p95 verification requirement.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G8 preserve EE-ZKP-040's verifier bound alongside sizing; target instance and required rate must be chosen.

**External acceptance boundary:** RP's named target hardware class and agreed peak-rate assumption.

## Existing entry points

- [verifier/go/zk/](../../../verifier/go/zk/)
- [docs/MEASUREMENTS.md](../../../docs/MEASUREMENTS.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/measurements/VERIFIER-CAPACITY.md`

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

Retain raw samples and environment; assess both the normative latency target and the agreed throughput requirement. Any unmet target is explicitly unmet; host history is a baseline, not a new measurement.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
