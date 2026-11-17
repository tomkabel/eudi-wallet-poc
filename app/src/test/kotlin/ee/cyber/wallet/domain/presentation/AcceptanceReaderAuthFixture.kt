package ee.cyber.wallet.domain.presentation

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

/** Fresh public reader chain/signature fixture. Private keys remain in JVM memory. */
internal class AcceptanceReaderAuthFixture {
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
        issuer: Issuer? = null
    ): Issuer {
        val subjectKeyPair = generateP256KeyPair()
        val signerKey = issuer?.keyPair ?: subjectKeyPair
        val issuerName = issuer?.cert?.let { JcaX509CertificateHolder(it).subject }
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

    private fun foreignRoot(): Issuer =
        mint("Foreign Reader Root", isCa = true, notBefore = Date(now - HOURS), notAfter = Date(now + HOURS))

    private fun leafUnder(issuer: Issuer): Issuer = mint(
        "Reader Leaf",
        isCa = false,
        notBefore = Date(now - MINUTES),
        notAfter = Date(now + HOURS),
        issuer = issuer
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
        chain: List<X509Cert>,
        encodedSessionTranscript: ByteArray
    ): ByteArray {
        val itemsRequest = buildCborMap {
            put("docType", "eu.europa.ec.av.1")
            put(
                "nameSpaces",
                buildCborMap {
                    put("eu.europa.ec.av.1", buildCborMap { put("age_over_18", false) })
                }
            )
        }
        val itemsRequestDataItem = Tagged(24, Bstr(Cbor.encode(itemsRequest)))
        val sessionTranscript = Cbor.decode(encodedSessionTranscript)
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

    suspend fun signedUntrusted(encodedSessionTranscript: ByteArray = Cbor.encode(buildCborMap { })): ByteArray {
        val root = foreignRoot()
        val leaf = leafUnder(root)
        return deviceRequestWithReaderAuth(leaf, listOf(leaf.multipazCert, root.multipazCert), encodedSessionTranscript)
    }

    companion object {
        private const val HOURS = 1 * 60 * 60 * 1000L
        private const val MINUTES = 5 * 60 * 1000L
    }
}
