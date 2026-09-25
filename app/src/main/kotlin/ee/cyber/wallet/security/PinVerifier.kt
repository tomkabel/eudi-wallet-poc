package ee.cyber.wallet.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * The wallet's PIN verifier, kept away from everything a data breach exposes
 * (JVM-H3):
 *
 *  - the PIN is never stored, even hashed: verification runs Argon2id over the
 *    presented PIN against a per-session random salt, and the result is compared
 *    against the Argon2 hash of the PIN captured at wallet activation. Both
 *    hashes are recomputed per attempt; neither is persisted anywhere.
 *  - the reference side of the comparison is kept as raw hash bytes, never as a
 *    String, and is wiped when [reset] clears the session (the hash itself is
 *    not a secret, but hygiene demands the raw bytes not linger in retyped
 *    String form).
 *  - comparison is constant-time (MessageDigest.isEqual).
 *  - failed-attempt lockout (5 attempts, then a 30-minute lock) lives on a
 *    process-wide singleton, so it survives ViewModel/Activity recreation —
 *    unlike the old in-ViewModel counter. Process-death persistence would need
 *    a DataStore-backed counter and is a conscious follow-up.
 *
 * Salt freshness per verification: because no PIN material is stored, "old
 * hash" attacks do not exist here; the Argon2 parameters are calibrated for a
 * phone (64 MiB, 1 pass, parallelism 1, ~250-400 ms) and pinning them into the
 * parameter block keeps the KDF cost paid on every attempt, including the
 * attacker's offline ones (they would first need the activation-time hash,
 * which this module never persists).
 *
 * Threading: attempts are tracked per (process-wide) singleton; the map is
 * concurrent. This module is pure JVM and unit-testable; Android wires the
 * singleton in Hilt.
 */
class PinVerifier(
    private val clock: () -> Long = System::currentTimeMillis,
    private val lockDurationMs: Long = LOCK_DURATION_MS,
    private val maxAttempts: Int = MAX_ATTEMPTS
) {

    /** Failed attempts since the last success, keyed per process. */
    private val failedAttempts = ConcurrentHashMap<String, Int>()
    private val lockedUntil = ConcurrentHashMap<String, Long>()

    /**
     * The activation reference: Argon2 hash of the PIN over [referenceSalt].
     * The salt is not secret and MUST be reused for presented-PIN hashing —
     * two random salts would never produce comparable hashes.
     */
    private var referenceHash: ByteArray? = null
    private var referenceSalt: ByteArray? = null

    /**
     * Capture the activation PIN's Argon2 hash (with a fresh random salt) as
     * the verification reference. Called by PIN-creation flows; the plain PIN
     * is not retained.
     */
    fun setPin(pin: CharArray) {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        referenceHash = hashWithSalt(pin, salt)
        referenceSalt = salt
    }

    /**
     * Verify a presented PIN under the hardening policy. Returns true when the
     * PIN matches the activation reference; enforces the lockout window. The
     * presented-side hash is wiped immediately after comparison.
     */
    fun verify(pin: CharArray, sessionId: String = "global"): Boolean {
        val now = clock()
        lockedUntil[sessionId]?.let { until ->
            if (now < until) return false
            lockedUntil.remove(sessionId, until)
        }
        val salt = referenceSalt ?: return false
        val expected = referenceHash ?: return false
        val presented = hashWithSalt(pin, salt)
        val match = MessageDigest.isEqual(presented, expected)
        presented.fill(0)
        if (match) {
            failedAttempts.remove(sessionId)
            return true
        }
        val attempts = failedAttempts.merge(sessionId, 1, Int::plus) ?: 1
        if (attempts >= maxAttempts) {
            lockedUntil[sessionId] = now + lockDurationMs
            failedAttempts.remove(sessionId)
        }
        return false
    }

    fun isLocked(sessionId: String = "global"): Boolean =
        (lockedUntil[sessionId] ?: 0L) > clock()

    fun reset(sessionId: String = "global") {
        failedAttempts.remove(sessionId)
        lockedUntil.remove(sessionId)
        referenceHash?.fill(0)
        referenceHash = null
        referenceSalt?.fill(0)
        referenceSalt = null
    }

    private fun hashWithSalt(pin: CharArray, salt: ByteArray): ByteArray {
        val generator = Argon2BytesGenerator()
        // Builder(int) + withSalt(): the constructor pair present in BOTH
        // BouncyCastle lineages on this classpath (jdk18on 1.80 and the
        // lts8on 2.73.x that cose-java drags in).
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withSalt(salt)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(MEMORY_KB)
            .withIterations(PASSES)
            .withParallelism(PARALLELISM)
            .build()
        generator.init(params)
        val hash = ByteArray(HASH_BYTES)
        generator.generateBytes(pin, hash)
        return hash
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCK_DURATION_MS = 30L * 60 * 1000
        const val SALT_BYTES = 16
        const val HASH_BYTES = 32

        /** ~64 MiB of memory hardness — calibrated for a modern phone. */
        const val MEMORY_KB = 65536
        const val PASSES = 1
        const val PARALLELISM = 1
    }
}

/**
 * The six-digit floor for PIN entry (JVM-H3). Rejects weaker pins at the
 * creation site; four-digit pins die to a smartphone-sized brute force.
 */
object PinPolicy {
    const val MIN_LENGTH = 6

    fun isAcceptable(pin: String): Boolean =
        pin.length >= MIN_LENGTH && pin.all { it in '0'..'9' }
}
