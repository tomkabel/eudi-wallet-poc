# Step 8 record — measurement programme, host side

**Status:** complete on this host, 24 September 2026. Branch
`step8-measurement-prep` off `step7-close-the-loop` (PR #25). Opened as PR #26.

This is plan §6, the measurement programme (this repository's branches call it step 8; plan §4
stops at step 7), host side: the harness and the one measurement a host
can take are built; every device row of the §6 programme stays open. The
parallel fork task (`tomkabel/eudi-wallet-poc`, branch `step8-measurement-harness`)
carries the JVM-testable harness and the `MEASUREMENTS.md` skeleton this
programme records into.

## What step 8 is

Plan §6's measurement programme has seven rows. Exactly one is host-runnable —
verification time for multipaz proofs at the relying party (`EE-ZKP-040`, ≤ 1.0 s
p95) — because the fixture the two halves already share (step 0) is a real
multipaz 0.99.0 proof. Everything else needs the device set (§10, D5: one Pixel
8, one mid-range device, neither in hand), so §8's instruction "do not fake the
device rows" holds: they are recorded as PENDING-DEVICE in the fork's
`docs/MEASUREMENTS.md`, with the exact instrumented-run commands that row needs.

## Implemented in this repository

### 1. The host-runnable measurement (`verifier/go/zk/step8_measurement_test.go`)

- `BenchmarkMultipazVerify` times `zk.Verify` over
  `testdata/step0-multipaz/proof.bin` (the step 0 fixture, produced by multipaz
  0.99.0 in the fork's `:zk-conformance` module). Command:
  `cd verifier/go && go test ./zk/ -bench BenchmarkMultipazVerify -benchtime 10x -run '^$'`.
- `TestMultipazVerifyLatencyPercentiles` runs 20 individually timed verifications
  and reports p50/p95/max. EE-ZKP-040 states the budget as a p95; a Go
  benchmark's ns/op is a mean, so the percentile test is the row's actual
  instrument and the benchmark confirms it with standard tooling.
- The budget check **records rather than fails** on overrun, because the budget
  is a device-provisioned claim: a developer laptop or a shared CI runner is not
  provisioned for it, and the honest output there is the host figure. Set
  `EE_BENCH_FAIL=1` to re-arm the check on a known-quiet host; `EE_BENCH_SKIP=1`
  skips the percentile run where its ~85 s matters. CI's `e2e` job sets
  `EE_BENCH_SKIP=1`, so the percentile run is local-only.
- `loadFixture` widens from `*testing.T` to `testing.TB` so the benchmark shares
  it; no behavioural change to the step 0 tests.

### 2. The measured figure (`verifier/README.md`, "Measured" section)

Intel i5-8365U @ 1.60 GHz, 8 threads, Rust built with `-C target-cpu=x86-64-v3`,
24 September 2026:

| statistic | value |
|---|---|
| benchmark mean (10 iterations) | 4.02 s/op — 512 B/op, 1 alloc/op |
| p50 over 20 runs | 3.83 s |
| p95 over the same runs | 4.91 s |
| max | 5.15 s |

The 1.0 s p95 budget is **not met on this host, and these are host figures, not
device figures**. The proof is a phone prover's output and the verifying runtime
is the production Rust Longfellow, but the machine is a laptop CPU; plan §6
routes the budget check through the device set. What the run establishes is the
host-side baseline the device figure will be read against, and that a multipaz
proof costs the relying party the same order of verification time as the
Rust-prover proofs in the 2.4 s table above it. This closes the gap plan S8
named: no document previously recorded how the measured verification time
relates to the p95 budget.

## Built in the fork (recorded here because §6 is one programme)

Branch `step8-measurement-harness` (2396245) off `step7-conformity-note` (43cb89e), two
commits, `:app:compileDebugKotlin` + `:app:testDebugUnitTest` green before each:

- `ZkProofBenchmark` (`app/src/main/kotlin/ee/cyber/wallet/domain/measurement/`):
  nearest-rank p50/p95/p99 over cold (5) and warm (45) circuit load — the 50
  runs per device §6 asks for — and the `VmHWM` reader for
  `/proc/self/status`, which is what covers the native allocations
  `libzkp.so` makes outside the JVM heap. The proving sits behind a one-method
  `Prover` interface, so `ZkProofBenchmarkTest` exercises percentiles, VmHWM
  parsing and the cold/warm session shape on the JVM with synthetic samples —
  the same JVM-testable-domain pattern as `HolderObligations`.
- `docs/MEASUREMENTS.md`: the §6 table (What / Requirement / Method / Result),
  one row per measured quantity, device rows PENDING-DEVICE, the relying-party
  row carrying the host measurement above, plus the device recipe: debug
  variant, 5 cold + 45 warm runs in one process, `LongfellowZkSystem().apply {
  addDefaultCircuits() }` wired into `Prover` per `DigitalCredentialsViewModel`
  and `AgeProofRoundTripTest`. The fork has no `androidTest` source set yet;
  writing the instrumented runner is part of the device work.

## Not done here, by design

- Every device row of §6: approval-to-proof-ready p50/p95/p99 (≤ 2.0 s p95,
  ≤ 3.5 s p99), peak prover memory (≤ 250 MB), approval-to-decision over DC API
  cross-device, BLE throughput for a ~360 KB proof, TEE/StrongBox key generation
  and slot capacity, and the named-device list. They are PENDING-DEVICE until
  the §10 D5 devices exist; the fork's MEASUREMENTS.md holds their commands.
- No claim about the Estonian installed base: spec §23 item 12 stays open until
  someone researches it (plan §6).

## Verification

- `gofmt -l .` clean; `go vet ./...` clean; `go test ./...` green (verifier/go:
  zk 83.8 s, oid4vp, internal/cborsub, main all ok).
- `TestStep0MultipazFixture` still passes — the measurement rides the fixture
  the step 0 test already verifies.
- The percentile test's own log line, from the recorded run:
  `multipaz proof verification over 20 runs: p50=3.828099958s p95=4.909696494s
  max=5.151200878s (EE-ZKP-040 budget: ≤ 1.0 s p95)`.
