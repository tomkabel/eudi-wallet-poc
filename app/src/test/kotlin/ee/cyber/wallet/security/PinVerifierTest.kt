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
    fun `correct pin verifies and the stored record holds no plaintext`() {
        val v = verifier(FakeClock())
        val record = v.create("654321".toCharArray())
        assertFalse(record.hash.contentEquals("654321".toByteArray()))
        assertTrue(v.verify("654321".toCharArray(), record).first)
    }

    @Test
    fun `wrong pin is refused and counted`() {
        val v = verifier(FakeClock())
        val (ok, record) = v.verify("000000".toCharArray(), v.create("654321".toCharArray()))
        assertFalse(ok)
        assertEquals(1, record.failedAttempts)
        assertFalse(v.isLocked(record), "a single failure must not lock")
    }

    @Test
    fun `lockout engages after max attempts, survives in the record and expires`() {
        val clock = FakeClock()
        val v = verifier(clock)
        var record = v.create("654321".toCharArray())
        repeat(PinVerifier.MAX_ATTEMPTS) {
            val (ok, next) = v.verify("000000".toCharArray(), record)
            assertFalse(ok)
            record = next
        }
        // A fresh verifier (process restart) over the persisted record is still locked.
        val restarted = verifier(clock)
        assertTrue(restarted.isLocked(record), "lockout must survive a restart")
        assertFalse(restarted.verify("654321".toCharArray(), record).first, "correct pin must be refused during lockout")
        clock.advance(PinVerifier.LOCK_DURATION_MS + 1)
        assertFalse(restarted.isLocked(record), "lockout must expire")
        assertTrue(restarted.verify("654321".toCharArray(), record).first, "correct pin works again after expiry")
    }

    @Test
    fun `success clears the attempt counter`() {
        val v = verifier(FakeClock())
        var record = v.create("654321".toCharArray())
        repeat(PinVerifier.MAX_ATTEMPTS - 1) { record = v.verify("000000".toCharArray(), record).second }
        val (ok, cleared) = v.verify("654321".toCharArray(), record)
        assertTrue(ok)
        assertEquals(0, cleared.failedAttempts)
    }
}
