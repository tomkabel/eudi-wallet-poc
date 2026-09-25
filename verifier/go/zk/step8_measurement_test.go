package zk

// Step 8 of docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md §6: the relying-party half of the
// measurement programme, host-runnable. The device rows (approval-to-proof-ready time, peak
// prover memory, the transport figures) stay PENDING-DEVICE; this file measures the one row
// with a host in reach — verification time for multipaz proofs at the relying party,
// EE-ZKP-040's ≤ 1.0 s p95 verify budget — by timing zk.Verify over the step 0 fixture.

import (
	"os"
	"sort"
	"testing"
	"time"
)

// BenchmarkMultipazVerify measures zk.Verify over the step 0 multipaz fixture
// (testdata/step0-multipaz/proof.bin, produced by multipaz 0.99.0). Run it with:
//
//	cd verifier/go && go test ./zk/ -bench BenchmarkMultipazVerify -benchtime 10x -run '^$'
//
// and read ns/op as the per-verification wall time. EE-ZKP-040 states the budget as a p95 and
// the benchmark reports a mean, so TestMultipazVerifyLatencyPercentiles computes the p95 over
// individually timed runs; the benchmark exists to confirm the figure with standard tooling.
func BenchmarkMultipazVerify(b *testing.B) {
	req, _ := loadFixture(b, "testdata/step0-multipaz")
	if err := Verify(req); err != nil {
		b.Fatalf("fixture proof did not verify before timing: %v", err)
	}

	b.ReportAllocs()
	b.ResetTimer()
	for i := 0; i < b.N; i++ {
		if err := Verify(req); err != nil {
			b.Fatalf("verify failed during timing: %v", err)
		}
	}
}

// TestMultipazVerifyLatencyPercentiles records the per-verification wall times as explicit
// percentiles and compares them against the EE-ZKP-040 budget. It reports the figures rather
// than failing when they exceed the budget, because the budget is a device-provisioned claim
// (plan §6): a developer laptop or a shared CI runner is not provisioned for it, and the
// honest failure there is the recorded host figure, not a red build. EE_BENCH_FAIL=1 re-arms
// the check where the host is known to be quiet.
func TestMultipazVerifyLatencyPercentiles(t *testing.T) {
	if os.Getenv("EE_BENCH_SKIP") == "1" {
		t.Skip("skipped by EE_BENCH_SKIP=1")
	}

	req, _ := loadFixture(t, "testdata/step0-multipaz")
	if err := Verify(req); err != nil {
		t.Fatalf("fixture proof did not verify before timing: %v", err)
	}

	const runs = 20
	times := make([]time.Duration, 0, runs)
	for i := 0; i < runs; i++ {
		start := time.Now()
		if err := Verify(req); err != nil {
			t.Fatalf("verify failed on run %d: %v", i+1, err)
		}
		times = append(times, time.Since(start))
	}
	sort.Slice(times, func(i, j int) bool { return times[i] < times[j] })

	nearest := func(q float64) time.Duration {
		rank := int(q*float64(runs) + 0.5)
		if rank < 1 {
			rank = 1
		}
		return times[rank-1]
	}
	p50, p95 := nearest(0.50), nearest(0.95)

	t.Logf("multipaz proof verification over %d runs: p50=%s p95=%s max=%s (EE-ZKP-040 budget: ≤ 1.0 s p95)",
		runs, p50, p95, times[runs-1])

	if os.Getenv("EE_BENCH_FAIL") == "1" && p95 > time.Second {
		t.Errorf("p95 verification %s exceeds the EE-ZKP-040 budget of 1.0 s on this host", p95)
	}
}
