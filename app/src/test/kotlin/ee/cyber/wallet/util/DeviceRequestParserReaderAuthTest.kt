package ee.cyber.wallet.util

import ee.cyber.wallet.security.CertificateChainValidator
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.crypto.toEcPrivateKey
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Base64
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * E5 (plan §8.3): the readerAuth chain-vs-anchor decision. [readerAuthenticated] pins the
 * SIGNATURE half (was the ItemsRequest signed by the presented leaf key?); the new
 * `readerChainTrusted` pins the TRUST half (does that chain path-build to the anchors the
 * parser was constructed with, under the centralized CertificateChainValidator policy?).
 *
 * The fixtures mint real EC P-256 chains with BouncyCastle; the leaf key signs the
 * `ReaderAuthentication` COSE_Sign1 over the same Sig_structure multipaz checks, so the
 * signature-valid cases exercise the parser's real verify path end to end, not a stub.
 */
class DeviceRequestParserReaderAuthTest {

    private val now = System.currentTimeMillis()

    // ---- chain minting (pattern from CertificateChainValidatorTest) --------------------

    private data class Issuer(
        val cert: X509Certificate,
        val keyPair: KeyPair,
        val multipazCert: X509Cert
    )

    private fun mint(
        subjectCn: String,
        isCa: Boolean,
        notBefore: Date,
        notAfter: Date,
        issuerCert: X509Certificate? = null,
        issuerKeyPair: KeyPair? = null,
        subjectKeyPair: KeyPair = generateP256KeyPair()
    ): Issuer {
        val signerKey = issuerKeyPair ?: subjectKeyPair
        val issuerName = issuerCert?.let { JcaX509CertificateHolder(it).subject }
            ?: X500Name("C=EE, O=E5-TEST, CN=$subjectCn")
        val subjectName = X500Name("C=EE, O=E5-TEST, CN=$subjectCn")
        val holder = X509v3CertificateBuilder(
            issuerName,
            BigInteger(64, SecureRandom()),
            notBefore,
            notAfter,
            subjectName,
            SubjectPublicKeyInfo.getInstance(subjectKeyPair.public.encoded)
        ).apply {
            addExtension(Extension.basicConstraints, true, BasicConstraints(isCa))
            addExtension(
                Extension.keyUsage,
                true,
                if (isCa) KeyUsage(KeyUsage.keyCertSign or KeyUsage.cRLSign) else KeyUsage(KeyUsage.digitalSignature)
            )
        }.build(org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withECDSA").build(signerKey.private))
        val cert = JcaX509CertificateConverter().getCertificate(holder)
        return Issuer(cert, subjectKeyPair, X509Cert.fromPem(toPem(cert)))
    }

    private fun toPem(cert: X509Certificate): String {
        val body = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(cert.encoded)
        return "-----BEGIN CERTIFICATE-----\n$body\n-----END CERTIFICATE-----\n"
    }

    private fun generateP256KeyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private fun trustedRoot(): Issuer =
        mint("Trusted Reader Root", isCa = true, notBefore = Date(now - HOURS), notAfter = Date(now + HOURS))

    private fun foreignRoot(): Issuer =
        mint("Foreign Reader Root", isCa = true, notBefore = Date(now - HOURS), notAfter = Date(now + HOURS))

    private fun leafUnder(issuer: Issuer): Issuer = mint(
        "Reader Leaf", isCa = false,
        notBefore = Date(now - MINUTES), notAfter = Date(now + HOURS),
        issuerCert = issuer.cert, issuerKeyPair = issuer.keyPair
    )

    // ---- the DeviceRequest wire fixture ------------------------------------------------

    /**
     * Encodes a minimal 18013-5 DeviceRequest whose single doc request carries a readerAuth
     * COSE_Sign1 made by the leaf key with the given chain in the (unprotected) x5chain
     * header — the same header slot the parser reads. The signature covers the
     * ReaderAuthentication CBOR built over the SAME minimal session transcript the parse call
     * supplies, exactly as a real reader would.
     */
    private suspend fun deviceRequestWithReaderAuth(
        leaf: Issuer,
        chain: List<X509Cert>
    ): ByteArray {
        val itemsRequest = buildCborMap {
            put("docType", "eu.europa.ec.av.1")
            put("nameSpaces", buildCborMap { })
        }
        val itemsRequestDataItem = Tagged(24, Bstr(Cbor.encode(itemsRequest)))
        val sessionTranscript = buildCborMap { }
        val encodedReaderAuthentication = Cbor.encode(
            buildCborArray {
                add("ReaderAuthentication")
                add(sessionTranscript)
                add(itemsRequestDataItem)
            }
        )
        val readerAuthenticationBytes = Cbor.encode(Tagged(24, Bstr(encodedReaderAuthentication)))
        @Suppress("DEPRECATION")
        val readerAuth = Cose.coseSign1Sign(
            leaf.keyPair.private.toEcPrivateKey(leaf.keyPair.public, org.multipaz.crypto.EcCurve.P256),
            readerAuthenticationBytes,
            includeDataInPayload = false,
            signatureAlgorithm = Algorithm.ES256,
            protectedHeaders = mapOf(CoseNumberLabel(Cose.COSE_LABEL_ALG) to Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()),
            unprotectedHeaders = mapOf(
                CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN) to X509CertChain(chain).toDataItem()
            )
        )
        val docRequest = buildCborMap {
            put("itemsRequest", itemsRequestDataItem)
            put("readerAuth", readerAuth.toDataItem())
        }
        return Cbor.encode(
            buildCborMap {
                put("version", "1.0")
                put("docRequests", buildCborArray { add(docRequest) })
            }
        )
    }

    private suspend fun parseRequest(deviceRequest: ByteArray, anchors: List<X509Certificate>) =
        DeviceRequestParser(deviceRequest, Cbor.encode(buildCborMap { }), anchors).parse()

    // ---- the pinned cases --------------------------------------------------------------

    @Test
    fun `a signature-valid chain under the anchor is authenticated and trusted`() = runTest {
        val root = trustedRoot()
        val leaf = leafUnder(root)
        val parsed = parseRequest(
            deviceRequestWithReaderAuth(leaf, listOf(leaf.multipazCert, root.multipazCert)),
            anchors = listOf(root.cert)
        ).docRequests.single()

        assertTrue(parsed.readerAuthenticated)
        assertTrue(parsed.readerChainTrusted)
    }

    @Test
    fun `a signature-valid chain under a foreign root is authenticated but NOT trusted`() = runTest {
        val root = trustedRoot()
        val foreign = foreignRoot()
        val leaf = leafUnder(foreign)
        val parsed = parseRequest(
            deviceRequestWithReaderAuth(leaf, listOf(leaf.multipazCert, foreign.multipazCert)),
            anchors = listOf(root.cert)
        ).docRequests.single()

        assertTrue(parsed.readerAuthenticated, "the signature itself is valid — the signature gate passes")
        assertFalse(parsed.readerChainTrusted, "the chain does not path-build to the anchor — the trust gate fails")
    }

    @Test
    fun `a request without readerAuth is neither authenticated nor trusted`() = runTest {
        val root = trustedRoot()
        val itemsRequest = buildCborMap {
            put("docType", "eu.europa.ec.av.1")
            put("nameSpaces", buildCborMap { })
        }
        val deviceRequest = Cbor.encode(
            buildCborMap {
                put("version", "1.0")
                put(
                    "docRequests",
                    buildCborArray {
                        add(buildCborMap { put("itemsRequest", Tagged(24, Bstr(Cbor.encode(itemsRequest)))) })
                    }
                )
            }
        )
        val parsed = parseRequest(deviceRequest, anchors = listOf(root.cert)).docRequests.single()

        assertFalse(parsed.readerAuthenticated)
        assertFalse(parsed.readerChainTrusted)
    }

    @Test
    fun `a signature-valid chain is NOT trusted when the parser gets no anchors`() = runTest {
        val root = trustedRoot()
        val leaf = leafUnder(root)
        val parsed = DeviceRequestParser(
            deviceRequestWithReaderAuth(leaf, listOf(leaf.multipazCert, root.multipazCert)),
            Cbor.encode(buildCborMap { })
        ).parse().docRequests.single()

        assertTrue(parsed.readerAuthenticated, "the default anchors keep the signature gate untouched")
        assertFalse(parsed.readerChainTrusted, "no anchors — the pre-E5 behaviour: trust stays the caller's problem")
    }

    @Test
    fun `the chain gate still runs when the signature check FAILED`() = runTest {
        // A chain rooted at the anchor but signed by the WRONG key: readerAuthenticated must
        // fall but the chain decision must still be computed and reported true — the two
        // gates are independent, and a caller combining them must see the trust signal even
        // when the signature is bad.
        val root = trustedRoot()
        val leaf = leafUnder(root)
        val wrongKeyLeaf = leaf.copyIssuerKeys(generateP256KeyPair())
        val parsed = parseRequest(
            deviceRequestWithReaderAuth(wrongKeyLeaf, listOf(leaf.multipazCert, root.multipazCert)),
            anchors = listOf(root.cert)
        ).docRequests.single()

        assertFalse(parsed.readerAuthenticated, "signed by the wrong key")
        assertTrue(parsed.readerChainTrusted, "the chain itself is still under the anchor")
    }

    /** The same leaf certificate, but a DIFFERENT private key signs the readerAuth. */
    private fun Issuer.copyIssuerKeys(newKeyPair: KeyPair): Issuer = Issuer(cert, newKeyPair, multipazCert)

    companion object {
        private const val HOURS = 1 * 60 * 60 * 1000L
        private const val MINUTES = 5 * 60 * 1000L
    }
}
