package ee.cyber.wallet.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JVM tests for the PIN hardening module (JVM-H3/D6): policy floor, lockout
 * persistence semantics and the wipe-what-you-compared discipline. The
 * Argon2 cost parameters are the production ones, so each verify() call costs
 * ~300 ms — the suites keep attempt counts small on purpose.
 */
class PinVerifierTest {

    private class FakeClock(var now: Long = 1_000_000L) : () -> Long {
        override fun invoke(): Long = now
        fun advance(ms: Long) { now += ms }
    }

    private fun verifier(clock: FakeClock, maxAttempts: Int = PinVerifier.MAX_ATTEMPTS) =
        PinVerifier(clock = clock, lockDurationMs = 1_800_000, maxAttempts = maxAttempts)

    @Test
    fun `pin policy requires six digits or more`() {
        assertTrue(PinPolicy.isAcceptable("123456"))
        assertTrue(PinPolicy.isAcceptable("1234567890"))
        assertFalse(PinPolicy.isAcceptable("12345"), "five digits must be refused")
        assertFalse(PinPolicy.isAcceptable("12345a"), "non-digits must be refused")
        assertFalse(PinPolicy.isAcceptable(""), "empty must be refused")
    }

    @Test
    fun `correct pin verifies after activation`() {
        val clock = FakeClock()
        val v = verifier(clock)
        v.setPin("654321".toCharArray())
        assertTrue(v.verify("654321".toCharArray()))
    }

    @Test
    fun `wrong pin is refused and counted`() {
        val clock = FakeClock()
        val v = verifier(clock)
        v.setPin("654321".toCharArray())
        assertFalse(v.verify("000000".toCharArray()))
        assertFalse(v.isLocked(), "a single failure must not lock")
    }

    @Test
    fun `lockout engages after max attempts and expires`() {
        val clock = FakeClock()
        val v = verifier(clock)
        v.setPin("654321".toCharArray())
        repeat(PinVerifier.MAX_ATTEMPTS) { assertFalse(v.verify("000000".toCharArray())) }
        assertTrue(v.isLocked(), "lockout must be engaged after $PinVerifier.MAX_ATTEMPTS failures")
        // Even the CORRECT pin is refused while locked.
        assertFalse(v.verify("654321".toCharArray()), "correct pin must be refused during lockout")
        clock.advance(PinVerifier.LOCK_DURATION_MS + 1)
        assertFalse(v.isLocked(), "lockout must expire")
        assertTrue(v.verify("654321".toCharArray()), "correct pin works again after expiry")
    }

    @Test
    fun `success clears the attempt counter`() {
        val clock = FakeClock()
        val v = verifier(clock)
        v.setPin("654321".toCharArray())
        repeat(PinVerifier.MAX_ATTEMPTS - 1) { assertFalse(v.verify("000000".toCharArray())) }
        assertTrue(v.verify("654321".toCharArray()))
        repeat(PinVerifier.MAX_ATTEMPTS - 1) { assertFalse(v.verify("000000".toCharArray())) }
        // Counter was cleared by the success: one more failure must not lock.
        assertFalse(v.isLocked())
    }

    @Test
    fun `verify without an activation reference is refused`() {
        val clock = FakeClock()
        val v = verifier(clock)
        assertFalse(v.verify("654321".toCharArray()), "no reference set — nothing can match")
    }

    @Test
    fun `reset wipes the reference hash`() {
        val clock = FakeClock()
        val v = verifier(clock)
        v.setPin("654321".toCharArray())
        assertTrue(v.verify("654321".toCharArray()))
        v.reset()
        // The activation reference must not linger after a reset: reflectively
        // check the field is nulled/zeroed (the module's wipe contract).
        val field = PinVerifier::class.java.getDeclaredField("referenceHash")
        field.isAccessible = true
        val remaining = field.get(v) as ByteArray?
        assertTrue(remaining == null || remaining.all { it.toInt() == 0 }, "reference hash must be wiped on reset")
        assertFalse(v.verify("654321".toCharArray()), "after a reset there is no reference to verify against")
    }
}
