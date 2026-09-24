package ee.cyber.wallet.domain.measurement

import java.io.File

/**
 * Step 8 measurement harness (conformance plan §6): the statistics and the memory probe around
 * a ZK proof generation, with the proving itself behind [Prover] so the JVM unit tests exercise
 * every line here on synthetic samples and only the device run touches Longfellow. Kept free of
 * Android and multipaz types, like [ee.cyber.wallet.domain.presentation.HolderObligations].
 *
 * What plan §6 asks the device rows to produce is approval-to-proof-ready time — p50/p95/p99
 * over cold and warm circuit load, 50 runs per device — and peak prover memory from `VmHWM`
 * in `/proc/self/status`, which covers the native allocations `libzkp.so` makes outside the
 * JVM heap. This object computes both; the device-side caller only supplies the prover call.
 */
object ZkProofBenchmark {

    /**
     * One approval-to-proof-ready cycle. `proofReadyMillis` is wall-clock from the moment the
     * holder approves to a proof ready to send; `peakRssKb` is the process high-water mark read
     * from `VmHWM` immediately after the prover returned.
     */
    data class Sample(val proofReadyMillis: Long, val peakRssKb: Long?)

    /**
     * The proving step, isolated so tests substitute synthetic latencies and memory readings.
     * On the device this is one `ZkSystem.generateProof(...)` call over the resolved spec —
     * see `docs/MEASUREMENTS.md` for the wiring.
     */
    fun interface Prover {
        fun generateProof()
    }

    /** Reads the `VmHWM` line of a `/proc/self/status`-shaped text, in kB, or null when absent. */
    fun interface VmHwmSource {
        fun readVmHwmKb(): Long?
    }

    /** p50/p95/p99 of one phase, nearest-rank over the phase's samples. */
    data class Percentiles(val p50: Long, val p95: Long, val p99: Long)

    /**
     * The result of one measurement session: cold percentiles (first proofs after circuit load),
     * warm percentiles (the steady state the §6 budget is written against), and the peak prover
     * memory. `vmHwmBeforeKb` is the baseline the peak is read against — `VmHWM` never decreases,
     * so in a fresh instrumented process the absolute high-water mark is the prover's peak, while
     * in the running wallet the delta is what the proof phase added.
     */
    data class Report(
        val cold: List<Sample>,
        val warm: List<Sample>,
        val coldPercentiles: Percentiles,
        val warmPercentiles: Percentiles,
        val vmHwmBeforeKb: Long?,
        val peakProverKb: Long?
    )

    /**
     * Runs [coldRuns] measured cold proofs (first touch of the circuits), then [warmRuns] warm
     * ones — 5 + 45 by default, the 50 runs per device plan §6 asks for. Each run reads `VmHWM`
     * immediately after the prover call. A prover failure propagates: a failed proof is a broken
     * run, not a latency sample, and losing the session loudly beats a silently filtered set.
     */
    fun measure(
        prover: Prover,
        coldRuns: Int = 5,
        warmRuns: Int = 45,
        vmHwmSource: VmHwmSource = VmHwmSource { readVmHwmKb() }
    ): Report {
        require(coldRuns > 0) { "coldRuns must be positive" }
        require(warmRuns > 0) { "warmRuns must be positive" }

        val vmHwmBefore = vmHwmSource.readVmHwmKb()
        val cold = ArrayList<Sample>(coldRuns)
        val warm = ArrayList<Sample>(warmRuns)

        repeat(coldRuns) {
            cold.add(runOnce(prover, vmHwmSource))
        }
        repeat(warmRuns) {
            warm.add(runOnce(prover, vmHwmSource))
        }

        val peaks = (cold + warm).mapNotNull { it.peakRssKb }
        return Report(
            cold = cold,
            warm = warm,
            coldPercentiles = percentilesOf(cold.map { it.proofReadyMillis }),
            warmPercentiles = percentilesOf(warm.map { it.proofReadyMillis }),
            vmHwmBeforeKb = vmHwmBefore,
            peakProverKb = peaks.maxOrNull()
        )
    }

    private fun runOnce(prover: Prover, vmHwmSource: VmHwmSource): Sample {
        // No try/finally around the prover: a failure discards the whole session, so a VmHWM read
        // on that path would go nowhere, and a failing read there would mask the prover's error.
        val start = System.nanoTime()
        prover.generateProof()
        val proofReadyMillis = (System.nanoTime() - start) / 1_000_000
        return Sample(proofReadyMillis = proofReadyMillis, peakRssKb = vmHwmSource.readVmHwmKb())
    }

    /**
     * Nearest-rank percentiles: the ceil(q/100 · n)-th smallest sample, so p95 of 20 runs is the
     * 19th and p50 of 4 runs is the 2nd — never interpolated, every reported figure is a run
     * that actually happened.
     */
    fun percentilesOf(samples: List<Long>): Percentiles {
        require(samples.isNotEmpty()) { "no samples" }
        val sorted = samples.sorted()
        fun at(q: Int): Long {
            val rank = kotlin.math.ceil(q / 100.0 * sorted.size).toInt().coerceIn(1, sorted.size)
            return sorted[rank - 1]
        }
        return Percentiles(p50 = at(50), p95 = at(95), p99 = at(99))
    }
}

/**
 * The `VmHWM` line of the file `/proc/self/status`: `VmHWM:\t  1234 kB`. Returns null when the
 * line is absent; the caller then records memory as "not read" rather than guessing.
 */
fun readVmHwmKb(statusFile: String = "/proc/self/status"): Long? =
    readVmHwmKbFromText(File(statusFile).readText())

internal fun readVmHwmKbFromText(statusText: String): Long? {
    val line = statusText.lineSequence().firstOrNull { it.startsWith("VmHWM:") } ?: return null
    // The kernel separates the label from the value with a tab, which splits cleanly; a space
    // would glue `VmHWM:` and the value into one token, so strip the prefix and retry.
    val parts = line.trim().split(Regex("\\s+"))
    val valueToken = parts.getOrNull(1)?.takeIf { it.matches(Regex("\\d+")) }
        ?: parts[0].removePrefix("VmHWM:").takeIf { it.matches(Regex("\\d+")) }
    return valueToken?.toLongOrNull()
}
