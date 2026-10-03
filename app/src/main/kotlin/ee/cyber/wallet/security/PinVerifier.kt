package ee.cyber.wallet.security

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * The persisted PIN state: the Argon2id hash of the PIN captured at activation, its salt, and
 * the failed-attempt lockout counters. Stored in the encrypted user-session DataStore, so the
 * lockout survives process death — a force-stop no longer resets the attempt budget.
 */
class PinRecord(
    val hash: ByteArray,
    val salt: ByteArray,
    val failedAttempts: Int = 0,
    val lockedUntil: Long = 0L
)

/**
 * The wallet's PIN verifier (JVM-H3), pure and stateless so the caller can persist the
 * [PinRecord] it returns atomically with the attempt:
 *
 *  - the PIN itself is never stored: [create] keeps only its Argon2id hash over a random salt.
 *  - comparison is constant-time (MessageDigest.isEqual) and the presented-side hash is wiped
 *    right after it.
 *  - failed-attempt lockout (5 attempts, then a 30-minute lock) is carried in the record.
 *
 * The Argon2 parameters are calibrated for a phone (64 MiB, 1 pass, parallelism 1,
 * ~250-400 ms), so every attempt — including an attacker's offline ones against a stolen
 * record — pays the KDF cost.
 */
class PinVerifier(
    private val clock: () -> Long = System::currentTimeMillis,
    private val lockDurationMs: Long = LOCK_DURATION_MS,
    private val maxAttempts: Int = MAX_ATTEMPTS
) {

    /** The activation record for [pin], with a fresh random salt and no failed attempts. */
    fun create(pin: CharArray): PinRecord {
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        return PinRecord(hash = hashWithSalt(pin, salt), salt = salt)
    }

    fun isLocked(record: PinRecord): Boolean = record.lockedUntil > clock()

    /**
     * Verify [pin] against [record] under the lockout policy. Returns whether it matched and the
     * record to persist; while locked even the correct PIN is refused and the record is unchanged.
     */
    fun verify(pin: CharArray, record: PinRecord): Pair<Boolean, PinRecord> {
        val now = clock()
        if (record.lockedUntil > now) return false to record
        val presented = hashWithSalt(pin, record.salt)
        val match = MessageDigest.isEqual(presented, record.hash)
        presented.fill(0)
        if (match) return true to PinRecord(record.hash, record.salt)
        val attempts = record.failedAttempts + 1
        return false to if (attempts >= maxAttempts) {
            PinRecord(record.hash, record.salt, failedAttempts = 0, lockedUntil = now + lockDurationMs)
        } else {
            PinRecord(record.hash, record.salt, failedAttempts = attempts)
        }
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
