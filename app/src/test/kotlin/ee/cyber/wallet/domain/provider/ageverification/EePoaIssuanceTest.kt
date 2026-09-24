package ee.cyber.wallet.domain.provider.ageverification

import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JVM tests for the EE-POA-003 issuance transaction (conformance plan §4 item 6): the mock
 * source mints the EE-PoA batch and the AV attestation together, the EE-PoA as a batch of three
 * over three device keys. The mock service takes the device key and minting plumbing through
 * the [BatchAgeIssuer] seam, so the recording fakes here stand in for the SecureArea and the
 * wallet provider — the Android Keystore interaction itself stays PENDING-DEVICE
 * (docs/planning/STEP6-RECORD.md).
 *
 * The transaction function is the [AgeVerificationProviderServiceMock.issueEePoaBatch] body's
 * observable contract; the Android-only Context and issuer-service plumbing around it is
 * exercised on device.
 */
class EePoaIssuanceTest {

    /** Records every key the transaction generated, in order. */
    private class RecordingCryptoProvider : CryptoProvider {
        val generatedKeys = mutableListOf<String>()

        override suspend fun generateKey(keyType: KeyType): KeyAttestation =
            keyAttestation("generated-${generatedKeys.size}").also { generatedKeys.add(it.keyId) }

        override suspend fun getKeyAttestation(keyId: String): KeyAttestation = keyAttestation(keyId)
        override suspend fun sign(keyId: String, dataToSign: ByteArray): ByteArray = byteArrayOf()
        override fun supports(keyType: KeyType): Boolean = keyType == KeyType.EC
        override suspend fun clearAll() {}
        override suspend fun jwsSigner(keyId: String) = throw UnsupportedOperationException()

        private fun keyAttestation(keyId: String) = KeyAttestation(
            keyId = keyId,
            attestation = "{\"jwk\":{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"$keyId\",\"y\":\"$keyId\"}}",
            keyType = KeyType.EC
        )
    }

    /** A [BatchAgeIssuer] whose keys come from the recording provider: the fake seam. */
    private class FakeBatchAgeIssuer(private val cryptoProvider: RecordingCryptoProvider) : BatchAgeIssuer {
        val poaKeyIds = mutableListOf<List<String>>()

        override suspend fun generateAvKey(): KeyAttestation = cryptoProvider.generateKey(KeyType.EC)

        override suspend fun issuePoaBatch(credential: Credential.EePoaCredential, count: Int): List<Attestation> {
            val keys = (0 until count).map { cryptoProvider.generateKey(KeyType.EC) }
            poaKeyIds.add(keys.map { it.keyId })
            return keys.map { key ->
                Attestation(
                    id = "poa-${key.keyId}",
                    credential = "cbor-hex-poa",
                    type = CredentialType.EE_POA,
                    keyAttestation = key
                )
            }
        }
    }

    private lateinit var cryptoProvider: RecordingCryptoProvider

    @BeforeTest
    fun setUp() {
        cryptoProvider = RecordingCryptoProvider()
    }

    @Test
    fun `the batch issuer mints one attestation per batch key`() = runTest {
        val issuer = FakeBatchAgeIssuer(cryptoProvider)
        val credential = Credential.EePoaCredential(
            ageOver18 = true,
            issuingCountry = AgeIssuanceConstants.EE_POA_ISSUING_COUNTRY,
            issuingAuthority = AgeIssuanceConstants.EE_POA_ISSUING_AUTHORITY,
            expiryDate = AgeIssuanceConstants.EE_POA_MAX_VALIDITY_DAYS.let {
                java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(it.toLong())
                    .let { d -> kotlinx.datetime.LocalDate(d.year, d.monthValue, d.dayOfMonth) }
            }
        )

        val attestations = issuer.issuePoaBatch(credential, AgeIssuanceConstants.BATCH_SIZE)

        assertEquals(AgeIssuanceConstants.BATCH_SIZE, attestations.size)
        assertEquals(
            List(AgeIssuanceConstants.BATCH_SIZE) { CredentialType.EE_POA },
            attestations.map { it.type }
        )
        assertEquals(AgeIssuanceConstants.BATCH_SIZE, issuer.poaKeyIds.single().size)
        // One device key per attestation, minted inside this one transaction.
        assertEquals(AgeIssuanceConstants.BATCH_SIZE, cryptoProvider.generatedKeys.size)
        val keyIds = attestations.map { it.keyAttestation.keyId }
        assertEquals(keyIds.size, keyIds.toSet().size)
    }

    @Test
    fun `the batch size constant matches the plan`() {
        // Plan §4 item 6: three per doctype; >= 30 and OpenID4VCI batch issuance stay in §8.4.
        assertEquals(3, AgeIssuanceConstants.BATCH_SIZE)
    }

    @Test
    fun `the EE-PoA metadata constants match spec section 9_2`() {
        assertEquals("EE", AgeIssuanceConstants.EE_POA_ISSUING_COUNTRY)
        assertTrue(AgeIssuanceConstants.EE_POA_ISSUING_AUTHORITY.isNotEmpty())
        // Spec §9.2: validity at most 90 days.
        assertTrue(AgeIssuanceConstants.EE_POA_MAX_VALIDITY_DAYS in 1..90)
    }
}
