package ee.cyber.wallet.zk

import java.io.File
import org.junit.Assume.assumeTrue
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
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
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem

/**
 * Helpers shared by the :zk-conformance fixture tests (Step0CrossVerifyTest and
 * Step2aDeviceResponseFixtureTest): the session transcript shape, the minted
 * age-verification mdoc, the single-attribute circuit pick and Longfellow's own
 * timestamp formatting. Kept out of the test classes so both fixtures are
 * provably minted the same way.
 */
object ZkConformanceConsts {
    const val AV_DOCTYPE = "eu.europa.ec.av.1"
    const val AV_NAMESPACE = "eu.europa.ec.av.1"
    const val AGE_OVER_18 = "age_over_18"

    /** 18013-5 clauses 7.1 and 9.1.2.4 forbid fractional seconds in these timestamps. */
    fun signedAtNow(): Instant = Instant.fromEpochSeconds(Clock.System.now().epochSeconds, 0)
}

/** The ee-eudiw checkout both fixture tests write into. */
object EeEudiw {
    // Defaults live in build.gradle.kts, the one place that knows the repository root.
    val dir: File
        get() = File(checkNotNull(System.getProperty("step0.eeEudiw")) { "step0.eeEudiw is unset; run through Gradle" })

    /**
     * Skips the calling test when there is no ee-eudiw checkout; without this,
     * a fixture test's mkdirs would invent an ee-eudiw tree nobody reads.
     */
    fun assumePresent(): File = dir.also {
        assumeTrue("no ee-eudiw checkout at ${it.absolutePath}", File(it, "verifier/go/zk").isDirectory)
    }
}

/** The transcript shape both fixture proofs bind. */
object SessionTranscripts {
    fun forZkConformance(): DataItem = buildCborArray {
        add(Bstr(byteArrayOf(1, 2, 3)))
        add(Bstr(byteArrayOf(4, 5, 6)))
        add("zk-conformance-handover")
    }
}

/** Picks the single-attribute circuit, which is what `age_over_18` alone needs. */
object ZkConformanceSpecs {
    fun oneAttributeSpec(zkSystem: LongfellowZkSystem): ZkSystemSpec =
        zkSystem.systemSpecs
            .filter { it.getParam<Long>("num_attributes") == 1L }
            .maxByOrNull { it.getParam<Long>("version") ?: Long.MIN_VALUE }
            ?: error("no single-attribute circuit bundled with multipaz-longfellow")
}

/**
 * The timestamp format Longfellow proofs bind: whole seconds, 'Z' suffix. Uses
 * LongfellowZkSystem's own private formatDate via reflection, so a fixture
 * cannot drift from what generateProof actually bound.
 */
object ZkConformanceFormat {
    fun formatDate(i: Instant): String =
        LongfellowZkSystem::class.java.getDeclaredMethod("formatDate", Instant::class.java)
            .apply { isAccessible = true }
            .invoke(LongfellowZkSystem(), i) as String
}

/** The minted mdoc a fixture proof is made over, plus the issuer cert that signed it. */
data class MintedMdoc(val document: MdocDocument, val issuerCert: X509Cert)

object MdocMinter {
    /**
     * The same minting Step0CrossVerifyTest has always done: a self-signed
     * P-256 issuer over a freshly generated device key, one true data element,
     * MSO signed at [signedAt].
     */
    suspend fun mintAgeVerificationMdoc(
        sessionTranscript: DataItem,
        signedAt: Instant = ZkConformanceConsts.signedAtNow()
    ): MintedMdoc {
        val issuerKey = AsymmetricKey.AnonymousExplicit(Crypto.createEcPrivateKey(EcCurve.P256))
        val deviceKey = AsymmetricKey.AnonymousExplicit(Crypto.createEcPrivateKey(EcCurve.P256))
        val validUntil = signedAt + 30.days

        val issuerCert = X509Cert.Builder(
            publicKey = issuerKey.publicKey,
            signingKey = issuerKey,
            serialNumber = ASN1Integer(1L),
            subject = X500Name.fromName("CN=EE Wallet Test Issuer"),
            issuer = X500Name.fromName("CN=EE Wallet Test Issuer"),
            validFrom = signedAt,
            validUntil = validUntil
        ).includeSubjectKeyIdentifier().build()

        val issuerNamespaces = buildIssuerNamespaces {
            addNamespace(ZkConformanceConsts.AV_NAMESPACE) {
                addDataElement(ZkConformanceConsts.AGE_OVER_18, true.toDataItem())
            }
        }

        val mso = MobileSecurityObject(
            version = "1.0",
            docType = ZkConformanceConsts.AV_DOCTYPE,
            signedAt = signedAt,
            validFrom = signedAt,
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

        return MintedMdoc(
            document = MdocDocument.fromNamespaces(
                sessionTranscript = sessionTranscript,
                docType = ZkConformanceConsts.AV_DOCTYPE,
                issuerAuth = issuerAuth,
                issuerNamespaces = issuerNamespaces,
                deviceNamespaces = buildDeviceNamespaces {},
                deviceKey = deviceKey
            ),
            issuerCert = issuerCert
        )
    }
}
