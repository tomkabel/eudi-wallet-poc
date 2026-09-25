# Measurements

The measurement programme of
[`EUDI-WALLET-POC-CONFORMANCE-PLAN.md` §6](https://github.com/tomkabel/ee-eudiw/blob/main/docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md)
(Estonian wallet repository, plan §6 "Measurement programme"). These are the figures the
PoC can produce, each replacing an assumption the specification marks. A figure is a
statement about the named device or host, never about Estonia's handsets (plan §6:
"Measurements are device-specific").

The columns follow the plan's table. Every `Result` cell is **PENDING-DEVICE** until a real
device run replaces it — nothing here is to be filled from an emulator, a laptop, or an
extrapolation. The one exception is the relying-party row, which this repository's host can
measure and ee-eudiw has.

## The table

| What | Requirement or open item | Method | Result |
|---|---|---|---|
| Approval-to-proof-ready time, p50/p95/p99, cold and warm circuit load | `EE-ZKP-040` (≤ 2.0 s p95, ≤ 3.5 s p99) | Instrumented run in this fork, 50 runs per device (5 cold + 45 warm), harness `ZkProofBenchmark` | PENDING-DEVICE (hardware unavailable, 2026-09-25 — see the log's device block) |
| Peak prover memory | `EE-ZKP-041` (≤ 250 MB) | `VmHWM` from `/proc/self/status` around `generateProof`, which covers native allocations | PENDING-DEVICE (hardware unavailable, 2026-09-25 — see the log's device block) |
| Verification time for multipaz proofs, at the relying party | `EE-ZKP-040` (≤ 1.0 s p95) | Go benchmark over the step 0 fixture, in ee-eudiw (`verifier/go/zk`, `BenchmarkMultipazVerify` + `TestMultipazVerifyLatencyPercentiles`) | MEASURED ON HOST, 24 Sep 2026: mean 4.02 s/op, p50 3.83 s, p95 4.91 s, max 5.15 s over 20 runs (i5-8365U @ 1.60 GHz, 8 threads, Rust `-C target-cpu=x86-64-v3`). The 1.0 s p95 budget is not met on this host. Host figures, not device figures — see `verifier/README.md` in ee-eudiw |
| Approval to relying-party decision over DC API cross-device | Remote analogue of `EE-ZKP-044`; **not** evidence for it, which is a proximity budget | Page and wallet timestamps | PENDING-DEVICE (hardware unavailable, 2026-09-25 — see the log's device block) |
| BLE device-retrieval throughput for a ~360 KB proof | Spec §23 item 24; `EE-ZKP-044`/`045`; `EE-CNF-006` | Plan §8.1 | PENDING-DEVICE (hardware unavailable, 2026-09-25 — see the log's device block) |
| Key generation in TEE and StrongBox; slot capacity | Spec §23 item 26; `EE-POA-011b` | Plan step 5's loop test | PENDING-DEVICE (hardware unavailable, 2026-09-25 — see the log's device block) |
| Which devices were used | Spec §23 item 12, `EE-ZKP-043` | Named models and Android versions, recorded per run | PENDING-DEVICE (hardware unavailable, 2026-09-25 — see the log's device block) |

The device set is decided in the plan (§10, D5): one Pixel 8, which carries the recorded
StrongBox anomaly, and one mid-range device. Run the programme on each; a row's `Result`
cell then names the device, the Android version, and the date.

## The harness

`app/src/main/kotlin/ee/cyber/wallet/domain/measurement/ZkProofBenchmark.kt` holds everything
that is not the proving itself: nearest-rank p50/p95/p99 over the cold and warm samples and
the `VmHWM` reader for `/proc/self/status`. The proving is behind a one-method `Prover`
interface, so the JVM unit tests (`ZkProofBenchmarkTest`) exercise the statistics and parsing
logic on synthetic samples and no Android or multipaz type enters the measured path. The
harness is committed and JVM-green; what waits for hardware is only the `Prover` wiring
below and the instrumented runner around it.

What one measured run produces:

- `Report.coldPercentiles` / `Report.warmPercentiles` — the approval-to-proof-ready p50/p95/p99
  for the `EE-ZKP-040` row, cold (first proofs after circuit load) and warm (the steady state).
  `EE-ZKP-040` does not qualify its budget by circuit state, so both phases are read against
  it. Nearest-rank never interpolates, so a percentile finer than the sample count allows is
  the slowest run: over 5 cold runs p95 and p99 are both the slowest cold run, and over 45 warm
  runs p99 is the slowest warm run (a p99 distinct from the maximum needs 100 samples, more than
  the 50 plan §6 budgets). The `Result` cell names such a figure as the maximum of n runs
  rather than as a percentile;
- `Report.vmHwmBeforeKb` and `Report.peakProverKb` — the `VmHWM` baseline and the peak, for
  the `EE-ZKP-041` row. The figure read against the 250 MB budget is the delta
  `peakProverKb − vmHwmBeforeKb`: `EE-ZKP-041` bounds prover memory and plan §6 reads `VmHWM`
  around `generateProof`, while the absolute peak also carries ART, the app and the
  instrumentation runner. `VmHWM` never decreases, so the delta is only the prover's share when
  nothing before `measure()` pushed the high-water mark up — hence the fresh process in step 4.

## Running it on a device

Build variant: **debug** — the release build type is disabled in `app/build.gradle.kts`, and
debug is what every other device-side step of this plan runs. The ABI filter already
restricts to `arm64-v8a` and `x86_64`, the ABIs `libzkp.so` ships for; a device must be
arm64-v8a to prove at all.

Run count: **50 per device** — `coldRuns = 5`, `warmRuns = 45` (the harness defaults), cold
first, same process, no relaunch between phases. Record the device model, Android version,
build fingerprint and date with the output (the last table row asks for exactly that).

The instrumented run is an `androidTest` that wires the real prover into the harness:

The clock `measure()` starts around each `Prover` call is the approval point, so the split
between steps 2 and 3 follows `DigitalCredentialsViewModel.kt`: what the wallet does before the
consent screen stays outside the `Prover`, what the share tap releases (`onShareClicked`) goes
inside it.

1. Construct the prover exactly as the wallet does:
   `LongfellowZkSystem().apply { addDefaultCircuits() }` — the version that loads all bundled
   circuits. The wallet builds it when the request arrives, before the consent screen, so it
   stays outside the `Prover`.
2. Before `measure()`: mint or load the age-verification mdoc's issuer-signed part (issuer
   namespaces, `issuerAuth`, device key) and build the session transcript the way
   `zk-conformance/src/test/.../AgeProofRoundTripTest.kt` does. The transcript also exists
   before approval — the wallet derives it from the request. The resolved spec is the
   highest-version single-attribute circuit, as in that test.
3. Implement `Prover { generateProof() }` as everything the tap releases, in order: bind the
   document to the session transcript — the `DeviceSigned` signature, which the test makes in
   `MdocDocument.fromNamespaces(sessionTranscript, …, deviceKey)` and the wallet in
   `presentWithDeviceSignature` — then one
   `zkSystem.generateProof(spec, document, sessionTranscript, SIGNED_AT)` call. The wallet signs
   with a SecureArea key; if the test signs with a software key, the figure leaves out the
   hardware signing time, and the `Result` cell says which key was used.
4. Call `ZkProofBenchmark.measure(prover)` with the default `VmHwmSource` (it reads
   `/proc/self/status` of the app process, which is what covers the native allocations).
   Run this test method on its own — `adb shell am instrument -w -e class <TestClass>#<method>
   <test package>/androidx.test.runner.AndroidJUnitRunner`, which starts a fresh app process —
   not after other tests in the same process.
5. Log the `Report` and paste the percentiles and the memory figures — the delta against the
   budget, the absolute peak beside it — into this table's
   `Result` cells, then carry the measured rows into spec §10.7 of the ee-eudiw repository,
   as the plan's table says.

The fork has no `androidTest` source set yet — the instrumented runner
(`androidx.test.runner.AndroidJUnitRunner`) is already configured in `app/build.gradle.kts`,
and writing the runner plus its dependencies is part of the device work, together with the
devices themselves. Until then the harness's correctness rests on the JVM tests, and every
device row stays PENDING-DEVICE.

## Host verify optimization log (workstream W2, 2026-09-25)

Everything in this section is host-side evidence about `zk.Verify` over the step 0 fixture
(`verifier/go/zk/testdata/step0-multipaz`, multipaz 0.99.0, circuit `longfellow-libzk-v1_7_1`).
Same discipline as the rest of this file: the command next to the figure, the host named, and
no claim wider than what was run. Headline: **the verify path is ~95 % Longfellow sumcheck
field arithmetic; every binding-layer optimization candidate (Rust PGO, an FFI
context/scratch-arena handle, Go PGO) was measured and rejected — none moves the number
outside noise — and the 1.0 s p95 verifier budget is re-derived as a capacity statement
instead.**

**Host.** i5-8365U @ 1.60 GHz, 8 threads, CachyOS, go1.27.1, rustc/cargo 1.95.0, perf 6.4,
system `llvm-profdata`. The Rust baseline pin (`verifier/zkverify-ffi/.cargo/config.toml`,
`-C target-cpu=x86-64-v3`) was active throughout. Caveat carried by every wall-clock figure
below: a concurrent build workload shared this machine for most of the day — single readings
of the *identical binary* swung p50 2.7 s → 5.1 s and loadavg peaked at 10.9 on 8 CPUs. The
A/B decisions therefore rest on interleaved same-window rounds, not on absolute values; no
quiet window opened in the session (a 45-minute wait for loadavg < 2.0 expired).

### 1. Profile first (no optimization before data)

Day-of baseline re-measurement of the 24 Sep row:

```
cd verifier/go && go test ./zk/ -bench BenchmarkMultipazVerify -benchtime 10x -run '^$'
# BenchmarkMultipazVerify-8  10  3.056 s/op, 512 B/op, 1 allocs/op   (24 Sep row: 4.02 s/op)
cd verifier/go && go test ./zk/ -run TestMultipazVerifyLatencyPercentiles -v
# p50=3.243 s  p95=4.097 s  max=4.626 s  over 20 runs
```

Go CPU profile over the benchmark (`go test -cpuprofile`, 10 iterations, 35.0 s sampled):

```
go tool pprof -top zkbench.test cpu.out
#   35.01s  100%  runtime.cgocall    <- every sample; the Go profiler cannot see past cgo
```

100 % of Go samples sit in `runtime.cgocall`, and the Go side allocates 512 B / 1 alloc per
verify: the Go layer is allocation- and CPU-irrelevant. Native attribution from `perf record`
over the same statically linked test binary (15 iterations, 48 256 samples):

```
perf record -F 997 -o perf.data -- ./zkbench.test -test.bench BenchmarkMultipazVerify -test.benchtime 15x
perf report --no-children -g none --percent-limit 0.5
#   66.53%  core_algebra::field::AlgebraicField::mulf
#   10.82%  runtime_algebra::lch14::Lch14<_,F>::fft
#    9.23%  runtime_sumcheck::hquad::HQuad<_,F>::bind_h
#    2.52%  runtime_sumcheck::hquad::HQuad<_,F>::bind_g
#    1.64%  sha256_compress
#    1.57%  core_algebra::field::AlgebraicField::mulf   (a second, inlined instance)
#    1.57%  runtime_sumcheck::eq::fill_recursive
#    1.36%  runtime_algebra::lch14::Lch14<_,F>::bidir_recur
```

With DWARF callchains (6 iterations, 23 695 samples) the `mulf` time resolves under
`zkv_verify → run_mdoc_verifier_inner → ZkVerifier::verify → symbolic_sumcheck_verifier_core`:
`bind_h` 45.2 %, `bind_g` 28.0 %, `eq::fill_recursive` 24.9 %, with `Lch14::fft` a separate
~11 % entry point. **Roughly 95 % of the path is the sumcheck/FFT field arithmetic; nothing
else clears 2 %.** A re-record with callchains cost one `perf.data` at DWARF size — nothing
in the repo.

Phase split (scratch binary over the runtime's public APIs, release profile identical to the
FFI crate's, 3–5 reps per phase; inputs copied from the fixture):

| phase | mean |
|---|---|
| `provider::materialize(7, 1)` — v7 is `CURRENT_VERSION`, so this recompiles the circuit and zstd-encodes it at `K_ZSTD_LEVEL = 16` per call | 0.005 s |
| `proto::decompress_circuits` | 0.013 s |
| `run_mdoc_verifier` on a pre-materialized circuit (includes its own decompress) | 2.712 s |
| verify core (row 3 − row 2) | ≈ 2.699 s |

The phases sum to 2.72 s against a 2.7–3.1 s end-to-end bench: there is no unaccounted copy,
allocation or conversion across the cgo boundary. The per-call recompile flagged in row 1 is
real but measures 0.5 % of the call — flagged for the future, not worth code today (see 2b).

**`overflow-checks` cost, measured** (release profile identical, checks toggled off in the
probe build only, 5-rep verify loop): core 2.699 s → 2.654 s, **≈ 1.7 %**. The checks stay:
they are what turns an overflow into a caught panic instead of a wrap-into-huge-allocation
abort (the invariant documented in `zkverify-ffi/src/lib.rs`), and 1.7 % buys nothing at the
scale that matters.

### 2. Candidates in expected-value order — all three measured, all three rejected

#### a. Rust PGO — REJECTED (gain inside noise, sign-inconsistent across rounds)

```
# in verifier/zkverify-ffi; the config pin must ride along because an env RUSTFLAGS
# REPLACES .cargo/config.toml's flags rather than appending to them
RUSTFLAGS='-C target-cpu=x86-64-v3 -Cprofile-generate=<dir>' cargo build --release
# training load: the benchmark itself through the real cgo path (go test -c, 6 iterations)
llvm-profdata merge -output=<dir>/merged.profdata <dir>/*.profraw
RUSTFLAGS='-C target-cpu=x86-64-v3 -Cprofile-use=<dir>/merged.profdata' cargo build --release
```

`make deps`' default path is untouched — without `RUSTFLAGS` the crate builds exactly as
before (config-file pin only), and `make deps` was re-run after the experiment to restore the
canonical staticlib (byte-identical size to the pre-experiment artifact). PGO stays a
documented two-command opt-in; wiring it into the default build needs a committed
`merged.profdata` and a Makefile change, and the measurement below says that change would be
unjustified.

Interleaved A/B (both statically linked test binaries re-run back-to-back, 20-run
percentiles per round, three rounds; loadavg 4.7–6.4 throughout, shared between arms):

| round | baseline p50 / p95 | PGO p50 / p95 | Δ p50 |
|---|---|---|---|
| 1 | 3.071 s / 3.543 s | 3.092 s / 3.468 s | +0.7 % |
| 2 | 2.801 s / 3.464 s | 2.963 s / 3.596 s | +5.8 % |
| 3 | 3.721 s / 4.741 s | 3.131 s / 3.914 s | −15.9 % |

Round 3 is the cleanest comparison (the paired arms share its load spike); rounds 1–2 carry
the same caveat in the other direction, so no round is taken alone. Final 10x bench pair:
baseline 2.864 s/op vs PGO 3.155 s/op. The deltas flip sign across
rounds while round-to-round variation of the *identical* baseline binary (p95 3.54 → 4.74)
is three times the largest apparent PGO effect. A CPU-time cross-check (bash `time`, user+sys
over 10×10 iterations, interleaved: baseline 5.40/4.62 s·CPU per verify, PGO 4.35/4.80)
overlaps the same way. The profile explains the flat result: the hot loop is field
multiplication (66 % flat in `mulf`), arithmetic rather than branch-mispredict-bound, which
is exactly the workload PGO does not rescue. Not adopted; the procedure above is the record
for whoever re-tests on different silicon.

#### b. FFI context/scratch arena (`zkv_verify_v2`) — REJECTED on profile evidence

The candidate would cache the only reusable state (`materialize` output / decompressed
circuits) behind an opaque `zkv_ctx`. The profile gives it no ground: materialize +
decompress are 18 ms of a ~2.7 s call (0.7 %), the Go side allocates 512 B/op, and phase sums
reproduce end-to-end time to within noise, so there is no hidden copy across the boundary. A
versioned ABI migration (keep v1, migrate the Go caller, delete v1) to chase a fraction of a
percent fails the plan's own honesty test in the other direction. Revisit trigger recorded:
if a future fixture pins a non-`CURRENT_VERSION` circuit (the zstd + archive-parse branch,
13 ms here) or circuits grow, a `(version, num_attributes)`-keyed cache is the right shape —
it does not need a new ABI symbol for that.

#### c. Go PGO on the test/service binary — REJECTED for this workload

Built with the verify workload's own CPU profile:

```
go test -c ./zk/ -pgo=cpu.out -o zkbench-gopgo.test
```

Interleaved rounds against a fresh baseline control (same window, same load):

| round | Go-PGO p50 / p95 | control p50 / p95 |
|---|---|---|
| 1 | 3.114 s / 3.894 s | 3.065 s / 3.375 s |
| 2 | 3.098 s / 3.451 s | 2.987 s / 3.208 s |

No improvement in either round (p95 +15 % / +7.6 % against control, and the Go profile is
100 % `runtime.cgocall` — there is no Go-level hot code for PGO to reshape; routing and JSON
are not in the measured path). A default `go build -pgo=auto` for the service binary remains
harmless and is a follow-up for the service owner; `main.go` and the Makefile are outside
this workstream's editable surface.

### 3. Budget re-derivation

Post-programme verdict: **the p95 stays at ~3.2–4.9 s on this host class (day-of
percentile runs: 3.21–4.75 s; the 24 Sep committed row: 4.91 s) against EE-ZKP-040's ≤ 1.0 s
p95 verifier budget — not met, at 3–5× the budget, and no available optimization lever moves
it.** The time is Longfellow sumcheck field arithmetic (~2.7 core-seconds per single-
attribute proof at 1.6 GHz-class silicon); a 1.0 s p95 for this circuit on this hardware was
never reachable by engineering the binding layer. The honest response is to re-derive, not to
imply a tuning gap.

**Sizing statement** (canonical build; quiet-host figure from the phase probe, loaded figure
from the CPU-time cross-checks — frequency scaling under concurrent load inflates per-verify
CPU, so both ends are stated):

- 1 verify ≈ **2.7 core-seconds** on a quiet 1.6 GHz-class core (≈ 4.3–5.4 s·CPU observed
  under heavy concurrent load). The cgo path is single-threaded; `max-concurrent-verify =
  NumCPU` parallelizes across verifications, not inside one.
- Steady state: one core sustains ≈ 1 300 verifies/hour (≈ 650 under load). A relying party
  at 100 presentations/peak-hour needs **≈ 0.08–0.15 cores** of this class for verification —
  trivially cheap at that rate.
- Burst is the real constraint: 100 presentations inside a 60 s window need **≈ 4.5–9 cores**
  to hold a 1.0 s per-verification promise, or ≈ 4.5 minutes of sequential time on one core
  (last presentation waits ≈ 4.5 min at 1 core). Sizing guidance: provision for the burst,
  not the average.

**Budget-split recommendation — spec-change proposal to ee-eudiw (note here; no spec edit
from this fork):** keep EE-ZKP-040's ≤ 1.0 s p95 as the **device-side prover** budget, where
plan §6 applies it (device hardware), and split the verifier row into (i) a
per-verification latency figure that names the hardware class (re-measured and re-stated per
release, per this log's method) and (ii) a throughput SLA the relying party provisions for
("X verifies/s/core at ≈ 2.7 core-seconds/verify on 1.6 GHz-class cores"). Rationale: the
verifier runs on provisioned server hardware where cores are the scaling unit; the prover
runs on unprovisioned phones where wall-clock latency is the user-experience unit. One
1.0 s number spanning both is what produced the misleading "5× over budget" reading this log
replaces with evidence.

### 4. Device rows (hardware unavailability, dated 2026-09-25)

No Pixel 8 or mid-range Android device is available in this session (2026-09-25), so the
device programme cannot run; each PENDING-DEVICE row in the table above carries the dated
statement and keeps its PENDING-DEVICE marker. Explicit follow-ups, owned outside this
workstream (the `app/` and `.github/` trees belong to another session's editable surface):

1. `app/src/androidTest/.../ZkProofBenchmarkRunner.kt` — the instrumented runner wiring the
   real prover into `ZkProofBenchmark` per "Running it on a device" above. The harness and
   its JVM tests are committed and green; only the `androidTest` source set is missing.
2. CI — the plan §9 `workflow_dispatch` perf job: runs `BenchmarkMultipazVerify`, fails only
   on > 20 % regression against a committed baseline file (noise-tolerant, not a flaky gate).
3. The device runs themselves (plan §10 D5: Pixel 8 + one mid-range device), which replace
   the dated-unavailability statements in the table.
