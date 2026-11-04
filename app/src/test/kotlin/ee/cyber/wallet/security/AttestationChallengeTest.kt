package ee.cyber.wallet.security

import kotlinx.coroutines.test.runTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** The mock provider's challenge must be unpredictable per key and carry no fixed value. */
class AttestationChallengeTest {

    @Test
    fun `challenge is a random UUID in its 36 byte ASCII form`() = runTest {
        val challenge = MockAttestationChallengeSource().challenge()
        assertEquals(36, challenge.size)
        assertTrue(challenge.all { it in 0x20..0x7e }, "ASCII only")
        UUID.fromString(challenge.decodeToString())
    }

    @Test
    fun `every call yields a fresh challenge`() = runTest {
        val source = MockAttestationChallengeSource()
        val challenges = List(50) { source.challenge().decodeToString() }
        assertEquals(50, challenges.toSet().size, "challenges must not repeat")
        assertNotEquals(source.challenge().decodeToString(), source.challenge().decodeToString())
    }
}
