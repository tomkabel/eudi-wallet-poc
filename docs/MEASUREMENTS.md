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
| Approval-to-proof-ready time, p50/p95/p99, cold and warm circuit load | `EE-ZKP-040` (≤ 2.0 s p95, ≤ 3.5 s p99) | Instrumented run in this fork, 50 runs per device (5 cold + 45 warm), harness `ZkProofBenchmark` | PENDING-DEVICE |
| Peak prover memory | `EE-ZKP-041` (≤ 250 MB) | `VmHWM` from `/proc/self/status` around `generateProof`, which covers native allocations | PENDING-DEVICE |
| Verification time for multipaz proofs, at the relying party | `EE-ZKP-040` (≤ 1.0 s p95) | Go benchmark over the step 0 fixture, in ee-eudiw (`verifier/go/zk`, `BenchmarkMultipazVerify` + `TestMultipazVerifyLatencyPercentiles`) | MEASURED ON HOST, 24 Sep 2026: mean 4.02 s/op, p50 3.83 s, p95 4.91 s, max 5.15 s over 20 runs (i5-8365U @ 1.60 GHz, 8 threads, Rust `-C target-cpu=x86-64-v3`). The 1.0 s p95 budget is not met on this host. Host figures, not device figures — see `verifier/README.md` in ee-eudiw |
| Approval to relying-party decision over DC API cross-device | Remote analogue of `EE-ZKP-044`; **not** evidence for it, which is a proximity budget | Page and wallet timestamps | PENDING-DEVICE |
| BLE device-retrieval throughput for a ~360 KB proof | Spec §23 item 24; `EE-ZKP-044`/`045`; `EE-CNF-006` | Plan §8.1 | PENDING-DEVICE |
| Key generation in TEE and StrongBox; slot capacity | Spec §23 item 26; `EE-POA-011b` | Plan step 5's loop test | PENDING-DEVICE |
| Which devices were used | Spec §23 item 12, `EE-ZKP-043` | Named models and Android versions, recorded per run | PENDING-DEVICE |

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
  for the `EE-ZKP-040` row, cold (first proofs after circuit load) and warm (the steady state
  the budget is written against);
- `Report.vmHwmBeforeKb` and `Report.peakProverKb` — the `VmHWM` baseline and the peak, for
  the `EE-ZKP-041` row. `VmHWM` never decreases; in a fresh instrumented process the peak is
  the prover's absolute high-water mark, and in a long-lived process the delta is what the
  proof phase added.

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
5. Log the `Report` and paste the percentiles and the memory figures into this table's
   `Result` cells, then carry the measured rows into spec §10.7 of the ee-eudiw repository,
   as the plan's table says.

The fork has no `androidTest` source set yet — the instrumented runner
(`androidx.test.runner.AndroidJUnitRunner`) is already configured in `app/build.gradle.kts`,
and writing the runner plus its dependencies is part of the device work, together with the
devices themselves. Until then the harness's correctness rests on the JVM tests, and every
device row stays PENDING-DEVICE.
