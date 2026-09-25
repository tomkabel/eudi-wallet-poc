package ee.cyber.wallet.domain.documents

import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.Curve
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import id.walt.mdoc.COSECryptoProviderKeyInfo
import id.walt.mdoc.SimpleCOSECryptoProvider
import id.walt.mdoc.dataelement.BooleanElement
import id.walt.mdoc.dataelement.DataElement
import id.walt.mdoc.doc.MDocBuilder
import id.walt.mdoc.mso.DeviceKeyInfo
import id.walt.mdoc.mso.ValidityInfo
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.cose.java.AlgorithmID
import org.cose.java.OneKey
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * E6 acceptance tests: the mapper's mdoc conversion performs on-device issuerAuth
 * verification (identity A-M6 part 2) before a credential can carry verified=true.
 *
 * Fixtures mint real BC EC P-256 chains and sign real waltid MSOs over them — the
 * same MDocBuilder/COSECryptoProviderKeyInfo shape CredentialIssuanceServiceMock
 * uses — so the signature path exercises org.cose Sign1Message.validate exactly as
 * production does.
 */
class CredentialToDocumentMapperMdocVerificationTest {

    @kotlin.test.BeforeTest
    fun registerBouncyCastle() {
        // The Android runtime registers BC automatically; the JVM test runner does not,
        // and org.cose OneKey/cose-java look it up by provider name.
        java.security.Security.getProvider("BC") ?: java.security.Security.addProvider(
            org.bouncycastle.jce.provider.BouncyCastleProvider()
        )
    }

    private fun ecP256(): KeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(java.security.spec.ECGenParameterSpec("secp256r1"), SecureRandom())
    }.generateKeyPair()

    /** Mints a [leafKey] under [rootKey]; both certs use the TEST IACA Root CA DN shape. */
    private fun mintChain(rootKey: KeyPair, leafKey: KeyPair, leafCn: String): List<X509Certificate> {
        val now = Instant.now()
        val rootName = X500Name("C=EE, O=SOTA-TEST, CN=TEST IACA Root CA")
        val root = JcaX509v3CertificateBuilder(
            rootName, BigInteger(64, SecureRandom()),
            Date.from(now.minusSeconds(3600)), Date.from(now.plusSeconds(3600L * 24 * 3650)),
            rootName, org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(rootKey.public.encoded)
        ).apply {
            addExtension(
                org.bouncycastle.asn1.x509.Extension.basicConstraints, true,
                org.bouncycastle.asn1.x509.BasicConstraints(true)
            )
            addExtension(
                org.bouncycastle.asn1.x509.Extension.keyUsage, true,
                org.bouncycastle.asn1.x509.KeyUsage(
                    org.bouncycastle.asn1.x509.KeyUsage.keyCertSign or org.bouncycastle.asn1.x509.KeyUsage.cRLSign
                )
            )
        }.build(JcaContentSignerBuilder("SHA512withECDSA").build(rootKey.private))
            .let { JcaX509CertificateConverter().getCertificate(it) }
        val leaf = JcaX509v3CertificateBuilder(
            rootName, BigInteger(64, SecureRandom()),
            Date.from(now.minusSeconds(3600)), Date.from(now.plusSeconds(3600L * 24 * 3650)),
            X500Name("C=EE, O=SOTA-TEST, CN=$leafCn"),
            org.bouncycastle.asn1.x509.SubjectPublicKeyInfo.getInstance(leafKey.public.encoded)
        ).apply {
            addExtension(
                org.bouncycastle.asn1.x509.Extension.basicConstraints, true,
                org.bouncycastle.asn1.x509.BasicConstraints(false)
            )
            addExtension(
                org.bouncycastle.asn1.x509.Extension.keyUsage, true,
                org.bouncycastle.asn1.x509.KeyUsage(org.bouncycastle.asn1.x509.KeyUsage.digitalSignature)
            )
        }.build(JcaContentSignerBuilder("SHA512withECDSA").build(rootKey.private))
            .let { JcaX509CertificateConverter().getCertificate(it) }
        return listOf(leaf, root)
    }

    /** Signs a one-item age_over_18 mdoc over [chain] — the mock's exact mint shape. */
    private fun mintMdoc(
        chain: List<X509Certificate>,
        issuerKey: KeyPair,
        validFrom: Instant,
        validUntil: Instant
    ): String {
        val deviceKey = ecP256()
        // DeviceKeyInfo shape from CredentialIssuanceServiceMock: OneKey(pub, null).AsCBOR().
        val deviceKeyInfo = DeviceKeyInfo(
            DataElement.fromCBOR(OneKey(deviceKey.public, null).AsCBOR().EncodeToBytes())
        )
        val provider = SimpleCOSECryptoProvider(
            listOf(
                COSECryptoProviderKeyInfo(
                    keyID = "test-issuer",
                    algorithmID = AlgorithmID.ECDSA_256,
                    publicKey = issuerKey.public,
                    privateKey = issuerKey.private,
                    x5Chain = chain,
                    trustedRootCAs = emptyList()
                )
            )
        )
        return MDocBuilder("eu.europa.ec.av.1")
            .addItemToSign("eu.europa.ec.av.1", "age_over_18", BooleanElement(true))
            .sign(
                ValidityInfo(
                    kotlin.time.Instant.fromEpochSeconds(validFrom.epochSecond),
                    kotlin.time.Instant.fromEpochSeconds(validFrom.epochSecond),
                    kotlin.time.Instant.fromEpochSeconds(validUntil.epochSecond)
                ),
                deviceKeyInfo,
                provider,
                "test-issuer"
            ).toCBORHex()
    }

    private fun keyAttestation(): KeyAttestation {
        val kp = ecP256()
        val jwk = ECKey.Builder(
            Curve.P_256, kp.public as java.security.interfaces.ECPublicKey
        ).build()
        return KeyAttestation(
            keyId = "test-key",
            attestation = "{\"jwk\":${jwk.toPublicJWK().toJSONString()}}",
            keyType = KeyType.EC
        )
    }

    private fun attestationFor(credential: String) = Attestation(
        id = "test-${BigInteger(32, SecureRandom())}",
        credential = credential,
        type = CredentialType.AGE_VERIFICATION,
        keyAttestation = keyAttestation()
    )

    @Test
    fun `mdoc chaining to the IACA anchor maps verified`() = runTest {
        val rootKey = ecP256()
        val issuerKey = ecP256()
        val chain = mintChain(rootKey, issuerKey, "TEST AV Issuer")
        val mapper = CredentialToDocumentMapper(trustAnchors = emptyList(), issuerAnchors = listOf(chain.last()))
        val doc = mapper.convert(
            attestationFor(
                mintMdoc(chain, issuerKey, Instant.now(), Instant.now().plusSeconds(86400L * 300))
            )
        )!!
        assertTrue(doc.verified, "mdoc with IACA-anchored issuerAuth chain must map as verified")
        assertFalse(doc.expired)
    }

    @Test
    fun `mdoc under a foreign root maps unverified`() = runTest {
        val rootKey = ecP256()
        val issuerKey = ecP256()
        val chain = mintChain(rootKey, issuerKey, "TEST AV Issuer")
        // A root from an unrelated key: the chain cannot anchor there.
        val foreignRootKey = ecP256()
        val foreignChain = mintChain(foreignRootKey, ecP256(), "Unrelated")
        val mapper = CredentialToDocumentMapper(
            trustAnchors = emptyList(),
            issuerAnchors = listOf(foreignChain.last())
        )
        val doc = mapper.convert(
            attestationFor(
                mintMdoc(chain, issuerKey, Instant.now(), Instant.now().plusSeconds(86400L * 300))
            )
        )!!
        assertFalse(doc.verified, "mdoc whose issuerAuth chain roots elsewhere must map unverified, not refused")
    }

    @Test
    fun `expired MSO validity window maps expired`() = runTest {
        val rootKey = ecP256()
        val issuerKey = ecP256()
        val chain = mintChain(rootKey, issuerKey, "TEST AV Issuer")
        val mapper = CredentialToDocumentMapper(trustAnchors = emptyList(), issuerAnchors = listOf(chain.last()))
        val doc = mapper.convert(
            attestationFor(
                mintMdoc(
                    chain, issuerKey,
                    validFrom = Instant.now().minusSeconds(86400L * 60),
                    validUntil = Instant.now().minusSeconds(86400L * 30)
                )
            )
        )!!
        assertTrue(doc.expired, "an MSO whose validUntil is in the past must map as expired")
    }

    @Test
    fun `tampered issuerSigned maps unverified or refuses to parse`() = runTest {
        val rootKey = ecP256()
        val issuerKey = ecP256()
        val chain = mintChain(rootKey, issuerKey, "TEST AV Issuer")
        val mapper = CredentialToDocumentMapper(trustAnchors = emptyList(), issuerAnchors = listOf(chain.last()))
        val minted = mintMdoc(chain, issuerKey, Instant.now(), Instant.now().plusSeconds(86400L * 300))
        // Tamper with the CBOR hex: the taut string replaces the element value, breaking
        // both the digest and (where it intersects the signature) the signature itself.
        val tampered = minted.replaceRange(
            minted.length - 10,
            minted.length,
            "f4f4f4f4f4"
        )
        val doc = mapper.convert(attestationFor(tampered))
        // Both outcomes are acceptable; the invariant is: never verified.
        if (doc != null) {
            assertFalse(doc.verified, "a credential whose bytes do not match the signature must not be verified")
        }
    }
}
