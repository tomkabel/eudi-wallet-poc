package ee.cyber.wallet.domain.measurement

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JVM tests for the step 8 measurement harness: nearest-rank percentiles, the `VmHWM` parser,
 * and the cold/warm session shape — all over synthetic samples, per plan §6's split that the
 * statistics logic is testable here while the proving waits for a device.
 */
class ZkProofBenchmarkTest {

    // ---------------------------------------------------------------- percentiles

    @Test
    fun percentilesAreNearestRankOverASmallFixedSet() {
        val p = ZkProofBenchmark.percentilesOf(listOf(7L, 1L, 5L, 3L, 9L))
        // Sorted: 1 3 5 7 9 — every rank lands on a real sample, no interpolation.
        assertEquals(5L, p.p50)
        assertEquals(9L, p.p95)
        assertEquals(9L, p.p99)
    }

    @Test
    fun p95OfTwentyRunsIsTheNineteenthSample() {
        // ceil(0.95 · 20) = 19 → the 19th smallest.
        assertEquals(19L, ZkProofBenchmark.percentilesOf((1L..20L).toList()).p95)
    }

    @Test
    fun p50OfFourRunsIsTheSecondSample() {
        // ceil(0.5 · 4) = 2 → the 2nd smallest, never an average of the middle two.
        assertEquals(20L, ZkProofBenchmark.percentilesOf(listOf(40L, 10L, 30L, 20L)).p50)
    }

    @Test
    fun singleSampleGivesItselfAsEveryPercentile() {
        val p = ZkProofBenchmark.percentilesOf(listOf(42L))
        assertEquals(42L, p.p50)
        assertEquals(42L, p.p95)
        assertEquals(42L, p.p99)
    }

    @Test
    fun duplicateValuesAreKeptAndNotDeduplicated() {
        val p = ZkProofBenchmark.percentilesOf(List(10) { 5L } + listOf(100L))
        // 11 samples: [5 × 10, 100]. ceil(0.95 · 11) = 11 → the outlier; ceil(0.5 · 11) = 6 →
        // still one of the duplicated 5s, which only works when duplicates keep their ranks.
        assertEquals(100L, p.p95)
        assertEquals(5L, p.p50)
    }

    @Test
    fun emptySampleListIsRejected() {
        assertFailsWith<IllegalArgumentException> {
            ZkProofBenchmark.percentilesOf(emptyList())
        }
    }

    @Test
    fun everyPercentileIsARealSample() {
        val samples = listOf(3L, 1L, 4L, 1L, 5L, 9L, 2L, 6L)
        val sorted = samples.sorted()
        val p = ZkProofBenchmark.percentilesOf(samples)
        assertTrue(sorted.contains(p.p50) && sorted.contains(p.p95) && sorted.contains(p.p99))
    }

    // ---------------------------------------------------------------- VmHWM parsing

    @Test
    fun parsesTheKernelVmHwmLine() {
        val status = "Name:\tprover\n" +
            "State:\tR (running)\n" +
            "VmPeak:\t  234567 kB\n" +
            "VmHWM:\t  123456 kB\n" +
            "VmRSS:\t   98765 kB\n"
        assertEquals(123456L, readVmHwmKbFromText(status))
    }

    @Test
    fun parsesASpaceSeparatedLine() {
        assertEquals(999L, readVmHwmKbFromText("VmHWM: 999 kB\n"))
    }

    @Test
    fun parsesLabelAndValueGluedByMissingWhitespace() {
        // No whitespace after the colon: the value hides inside the first token, so the parser
        // strips the `VmHWM:` prefix and retries.
        assertEquals(4321L, readVmHwmKbFromText("VmHWM:4321 kB\n"))
    }

    @Test
    fun absentLineYieldsNull() {
        assertNull(readVmHwmKbFromText("Name:\tprover\nVmRSS:\t 100 kB\n"))
    }

    @Test
    fun nonNumericValueYieldsNull() {
        assertNull(readVmHwmKbFromText("VmHWM:\tunknown kB\n"))
    }

    @Test
    fun emptyTextYieldsNull() {
        assertNull(readVmHwmKbFromText(""))
    }

    @Test
    fun readsTheVmHwmLineFromAStatusFile() {
        val status = kotlin.io.path.createTempFile().toFile().apply {
            writeText("Name:\tprover\nVmHWM:\t  2048 kB\n")
            deleteOnExit()
        }
        assertEquals(2048L, readVmHwmKb(status.path))
    }

    @Test
    fun anUnreadableStatusFileYieldsNullNotAnException() {
        assertNull(readVmHwmKb("/nonexistent/proc/self/status"))
    }

    // ---------------------------------------------------------------- measure()

    @Test
    fun measureRunsColdThenWarmAndReportsBothPercentileSets() {
        val calls = mutableListOf<Int>()
        val prover = ZkProofBenchmark.Prover { calls.add(calls.size + 1) }
        val vmHwm = ZkProofBenchmark.VmHwmSource { 50_000L + calls.size * 100L }

        val report = ZkProofBenchmark.measure(prover, coldRuns = 2, warmRuns = 3, vmHwmSource = vmHwm)

        assertEquals(5, calls.size)
        assertEquals(2, report.cold.size)
        assertEquals(3, report.warm.size)
        // The source rises by 100 kB per prover call and is read after each one, so the last
        // warm run holds the peak; the baseline was read before any run.
        assertEquals(50_500L, report.peakProverKb)
        assertEquals(50_000L, report.vmHwmBeforeKb)
    }

    @Test
    fun measureCapturesPerRunLatencyAsNonNegativeMillis() {
        val report = ZkProofBenchmark.measure(
            prover = ZkProofBenchmark.Prover { },
            coldRuns = 1,
            warmRuns = 1,
            vmHwmSource = ZkProofBenchmark.VmHwmSource { 1_000L }
        )
        assertTrue(report.cold.first().proofReadyMillis >= 0)
        assertTrue(report.warm.first().proofReadyMillis >= 0)
    }

    @Test
    fun aMissingVmHwmReadIsCarriedAsNullNotZero() {
        val report = ZkProofBenchmark.measure(
            prover = ZkProofBenchmark.Prover { },
            coldRuns = 1,
            warmRuns = 1,
            vmHwmSource = ZkProofBenchmark.VmHwmSource { null }
        )
        assertNull(report.vmHwmBeforeKb)
        assertNull(report.peakProverKb)
        assertNull(report.cold.first().peakRssKb)
    }

    @Test
    fun proverFailurePropagatesOutOfMeasure() {
        assertFailsWith<IllegalStateException> {
            ZkProofBenchmark.measure(
                prover = ZkProofBenchmark.Prover { error("prover blew up") },
                coldRuns = 1,
                warmRuns = 1,
                vmHwmSource = ZkProofBenchmark.VmHwmSource { 1L }
            )
        }
    }

    @Test
    fun aFailingVmHwmReadDoesNotMaskTheProverFailure() {
        // The baseline read succeeds; any read after the failed proof would throw and, if it ran,
        // replace the prover's exception.
        var reads = 0
        assertFailsWith<IllegalStateException> {
            ZkProofBenchmark.measure(
                prover = ZkProofBenchmark.Prover { error("prover blew up") },
                coldRuns = 1,
                warmRuns = 1,
                vmHwmSource = ZkProofBenchmark.VmHwmSource {
                    if (reads++ > 0) throw java.io.IOException("status unreadable") else 1L
                }
            )
        }
    }

    @Test
    fun zeroColdOrWarmRunsIsRejected() {
        val prover = ZkProofBenchmark.Prover { }
        val source = ZkProofBenchmark.VmHwmSource { 1L }
        assertFailsWith<IllegalArgumentException> {
            ZkProofBenchmark.measure(prover, coldRuns = 0, warmRuns = 1, vmHwmSource = source)
        }
        assertFailsWith<IllegalArgumentException> {
            ZkProofBenchmark.measure(prover, coldRuns = 1, warmRuns = 0, vmHwmSource = source)
        }
    }
}
