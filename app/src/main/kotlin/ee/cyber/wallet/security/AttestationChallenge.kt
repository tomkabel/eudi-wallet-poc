package ee.cyber.wallet.security

/**
 * The attestation challenge the (mock) wallet provider issues for a freshly generated device key.
 * Conformance plan §4 item 5: `setAttestationChallenge` is fed from the wallet provider, not made
 * up by the wallet, so the attestation chain the provider later inspects carries a challenge it
 * can bind to the enrolment.
 *
 * The mock provider derives it from the wallet instance it registered; a real provider would hand
 * out an unpredictable, single-use value per key. 16 random bytes (a UUID) - the same entropy an
 * attestation challenge needs, without importing the key material itself.
 */
interface AttestationChallengeSource {
    suspend fun challenge(): ByteArray
}

class MockAttestationChallengeSource : AttestationChallengeSource {
    override suspend fun challenge(): ByteArray = java.util.UUID.randomUUID().toString().toByteArray()
}
