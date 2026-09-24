package ee.cyber.wallet.security

/**
 * The attestation challenge the (mock) wallet provider issues for a freshly generated device key.
 * Conformance plan §4 item 5: `setAttestationChallenge` is fed from the wallet provider, not made
 * up by the wallet, so the attestation chain the provider later inspects carries a challenge it
 * can bind to the enrolment.
 *
 * The mock hands out a fresh random UUID per key, as its 36-byte ASCII string form (122 random
 * bits); it is not bound to the registered wallet instance, and nothing yet checks it in the
 * returned chain. A real provider would issue an unpredictable, single-use value per key and
 * verify it in the attestation it receives.
 */
interface AttestationChallengeSource {
    suspend fun challenge(): ByteArray
}

class MockAttestationChallengeSource : AttestationChallengeSource {
    override suspend fun challenge(): ByteArray = java.util.UUID.randomUUID().toString().toByteArray()
}
