package ee.cyber.wallet.zk

import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.devicesigned.buildDeviceNamespaces
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.mdoc.mso.MobileSecurityObject
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ProofVerificationFailureException
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/**
 * Proves and verifies an `age_over_18` Longfellow proof over a freshly minted EE age-verification
 * mdoc, then repeats the verification against a corrupted proof.
 *
 * The mdoc is minted here rather than checked in as a fixture so the test exercises this wallet's
 * own docType and namespace: a stored blob would only ever re-prove multipaz's test vector.
 */
class AgeProofRoundTripTest {

    @Test
    fun ageOver18ProofVerifiesAndTamperingIsRejected() = runTest {
        val zkSystem = LongfellowZkSystem().apply { addDefaultCircuits() }
        val sessionTranscript = sessionTranscript()
        val document = mintAgeVerificationMdoc()

        val spec = zkSystem.oneAttributeSpec()
        val zkDocument = zkSystem.generateProof(spec, document, sessionTranscript, SIGNED_AT)

        assertEquals(AV_DOCTYPE, zkDocument.documentData.docType)
        assertTrue(zkDocument.proof.size > 0, "prover returned an empty proof")

        // The positive control: an untouched proof verifies against the same spec and transcript.
        zkSystem.verifyProof(zkDocument, spec, sessionTranscript)

        // The negative control. Without it a prover that emitted a constant would still pass.
        val corrupted = zkDocument.proof.toByteArray().copyOf()
        corrupted[corrupted.size / 2] = (corrupted[corrupted.size / 2].toInt() xor 0x01).toByte()
        assertFailsWith<ProofVerificationFailureException> {
            zkSystem.verifyProof(zkDocument.copy(proof = ByteString(corrupted)), spec, sessionTranscript)
        }
    }

    /** The circuit for a single requested attribute, which is what `age_over_18` alone needs. */
    private fun LongfellowZkSystem.oneAttributeSpec(): ZkSystemSpec =
        systemSpecs
            .filter { it.getParam<Long>("num_attributes") == 1L }
            .maxByOrNull { it.getParam<Long>("version") ?: Long.MIN_VALUE }
            ?: error("no single-attribute circuit bundled with multipaz-longfellow")

    private suspend fun mintAgeVerificationMdoc(): MdocDocument {
        val issuerKey = AsymmetricKey.AnonymousExplicit(Crypto.createEcPrivateKey(EcCurve.P256))
        val deviceKey = AsymmetricKey.AnonymousExplicit(Crypto.createEcPrivateKey(EcCurve.P256))
        val validUntil = SIGNED_AT + 30.days

        val issuerCert = X509Cert.Builder(
            publicKey = issuerKey.publicKey,
            signingKey = issuerKey,
            serialNumber = ASN1Integer(1L),
            subject = X500Name.fromName("CN=EE Wallet Test Issuer"),
            issuer = X500Name.fromName("CN=EE Wallet Test Issuer"),
            validFrom = SIGNED_AT,
            validUntil = validUntil
        ).includeSubjectKeyIdentifier().build()

        val issuerNamespaces = buildIssuerNamespaces {
            addNamespace(AV_NAMESPACE) {
                addDataElement(AGE_OVER_18, true.toDataItem())
            }
        }

        val mso = MobileSecurityObject(
            version = "1.0",
            docType = AV_DOCTYPE,
            signedAt = SIGNED_AT,
            validFrom = SIGNED_AT,
            validUntil = validUntil,
            expectedUpdate = null,
            digestAlgorithm = Algorithm.SHA256,
            valueDigests = issuerNamespaces.getValueDigests(Algorithm.SHA256),
            deviceKey = deviceKey.publicKey
        )
        val issuerAuth = Cose.coseSign1Sign(
            signingKey = issuerKey,
            message = Cbor.encode(Tagged(Tagged.ENCODED_CBOR, Bstr(Cbor.encode(mso.toDataItem())))),
            includeMessageInPayload = true,
            protectedHeaders = mapOf<CoseLabel, DataItem>(
                CoseNumberLabel(Cose.COSE_LABEL_ALG) to Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
            ),
            unprotectedHeaders = mapOf<CoseLabel, DataItem>(
                CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN) to X509CertChain(listOf(issuerCert)).toDataItem()
            )
        )

        return MdocDocument.fromNamespaces(
            sessionTranscript = sessionTranscript(),
            docType = AV_DOCTYPE,
            issuerAuth = issuerAuth,
            issuerNamespaces = issuerNamespaces,
            deviceNamespaces = buildDeviceNamespaces {},
            deviceKey = deviceKey
        )
    }

    private fun sessionTranscript(): DataItem = buildCborArray {
        add(Bstr(byteArrayOf(1, 2, 3)))
        add(Bstr(byteArrayOf(4, 5, 6)))
        add("zk-conformance-handover")
    }

    private companion object {
        const val AV_DOCTYPE = "eu.europa.ec.av.1"
        const val AV_NAMESPACE = "eu.europa.ec.av.1"
        const val AGE_OVER_18 = "age_over_18"

        // 18013-5 clauses 7.1 and 9.1.2.4 forbid fractional seconds in these timestamps.
        val SIGNED_AT: Instant = Instant.fromEpochSeconds(Clock.System.now().epochSeconds, 0)
    }
}
