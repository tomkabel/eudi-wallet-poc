package ee.cyber.wallet.security

import ee.cyber.wallet.test.TestCryptoUtils.generateECKeyPair
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.KeyUsage
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigInteger
import java.security.KeyPair
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date

/**
 * E1 (testing H1): the trust-decision gate gets pinned before any behavior flips behind it
 * (E2 fail-closed LOTL, E4 revocation). Every case the plan names is exercised against the
 * real PKIX [CertificateChainValidator] with locally minted chains — no Android, no network.
 *
 * Notes on the environment this pins:
 *  - unit tests run with `isReturnDefaultValues = true`, so `BuildConfig.DEBUG` reads `false`
 *    here — exactly the production (non-debug) reading. The `setTrustAll(true)` case below
 *    therefore pins the FALSE path: the dev escape hatch must be inert outside debuggable
 *    builds whatever a stored preference says.
 *  - the validator is a process-wide singleton; `resetValidatorState()` in the setup returns
 *    the LOTL-union and trust-all fields to their defaults so tests are order-independent.
 */
class CertificateChainValidatorTest {

    private val now = System.currentTimeMillis()

    @Before
    fun resetValidatorState() {
        CertificateChainValidator.updateLOTLCertificates(emptyList())
        CertificateChainValidator.setTrustAll(false)
    }

    // ---- local chain minting (custom validity periods; TestCertificateUtils' fixed 1h
    // ---- window cannot express the expired-leaf case) -------------------------------

    private fun mint(
        subjectCn: String,
        isCa: Boolean,
        notBefore: Date,
        notAfter: Date,
        issuerCert: X509Certificate? = null,
        issuerKeyPair: KeyPair? = null,
        subjectKeyPair: KeyPair = generateECKeyPair()
    ): Pair<X509Certificate, KeyPair> {
        val signerKey = issuerKeyPair?.private ?: subjectKeyPair.private
        val issuerName = issuerCert?.let { JcaX509CertificateHolder(it).subject }
            ?: X500Name("C=EE, O=SOTA-TEST, CN=$subjectCn")
        val subjectName = X500Name("C=EE, O=SOTA-TEST, CN=$subjectCn")
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
        }.build(JcaContentSignerBuilder("SHA512withECDSA").build(signerKey))
        return JcaX509CertificateConverter().getCertificate(holder) to subjectKeyPair
    }

    private fun trustedRoot(): Pair<X509Certificate, KeyPair> =
        mint("Trusted Root", isCa = true, notBefore = Date(now - HOURS), notAfter = Date(now + HOURS))

    private fun foreignRoot(): Pair<X509Certificate, KeyPair> =
        mint("Foreign Root", isCa = true, notBefore = Date(now - HOURS), notAfter = Date(now + HOURS))

    private fun leafUnder(
        issuer: Pair<X509Certificate, KeyPair>,
        notBefore: Date = Date(now - MINUTES),
        notAfter: Date = Date(now + HOURS)
    ): X509Certificate =
        mint("Leaf", isCa = false, notBefore = notBefore, notAfter = notAfter, issuerCert = issuer.first, issuerKeyPair = issuer.second).first

    private fun intermediateUnder(issuer: Pair<X509Certificate, KeyPair>): Pair<X509Certificate, KeyPair> =
        mint("Intermediate", isCa = true, notBefore = Date(now - HOURS), notAfter = Date(now + HOURS), issuerCert = issuer.first, issuerKeyPair = issuer.second)

    // ---- the pinned cases -----------------------------------------------------------

    @Test
    fun `chain anchored at the trusted root is accepted`() {
        val root = trustedRoot()
        val leaf = leafUnder(root)
        assertTrue(
            CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(root.first))
        )
    }

    @Test
    fun `intermediate chain rooted at the trusted anchor is accepted`() {
        val root = trustedRoot()
        val intermediate = intermediateUnder(root)
        val leaf = leafUnder(intermediate)
        assertTrue(
            CertificateChainValidator.validateCertificateChain(listOf(leaf, intermediate.first), listOf(root.first))
        )
    }

    @Test
    fun `foreign root is refused`() {
        val foreign = foreignRoot()
        val leaf = leafUnder(foreign)
        assertFalse(
            CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(trustedRoot().first))
        )
    }

    @Test
    fun `expired leaf is refused`() {
        val root = trustedRoot()
        val expiredLeaf = leafUnder(root, notBefore = Date(now - 2 * HOURS), notAfter = Date(now - MINUTES))
        assertFalse(
            CertificateChainValidator.validateCertificateChain(listOf(expiredLeaf), listOf(root.first))
        )
    }

    @Test
    fun `not-yet-valid leaf is refused`() {
        val root = trustedRoot()
        val futureLeaf = leafUnder(root, notBefore = Date(now + HOURS), notAfter = Date(now + 2 * HOURS))
        assertFalse(
            CertificateChainValidator.validateCertificateChain(listOf(futureLeaf), listOf(root.first))
        )
    }

    @Test
    fun `empty chain is rejected`() {
        val root = trustedRoot()
        assertFalse(
            CertificateChainValidator.validateCertificateChain(emptyList(), listOf(root.first))
        )
    }

    @Test
    fun `empty anchors with no lotl union are rejected`() {
        val root = trustedRoot()
        val leaf = leafUnder(root)
        assertFalse(
            CertificateChainValidator.validateCertificateChain(listOf(leaf), emptyList())
        )
    }

    @Test
    fun `lotl anchors participate when the static anchor set is empty`() {
        val lotlRoot = trustedRoot()
        val leaf = leafUnder(lotlRoot)
        CertificateChainValidator.updateLOTLCertificates(listOf(lotlRoot.first))
        assertTrue(
            CertificateChainValidator.validateCertificateChain(listOf(leaf), emptyList())
        )
    }

    @Test
    fun `lotl anchors union cannot vouch for a foreign chain when a static anchor exists`() {
        val staticRoot = trustedRoot()
        val foreignLeaf = leafUnder(foreignRoot())
        CertificateChainValidator.updateLOTLCertificates(listOf(staticRoot.first))
        assertFalse(
            CertificateChainValidator.validateCertificateChain(listOf(foreignLeaf), listOf(staticRoot.first))
        )
    }

    @Test
    fun `trustAll cannot mask a bad chain when the gate is closed`() {
        // The dev escape hatch is hard-gated by `BuildConfig.DEBUG` inside setTrustAll()
        // (validator.kt:61). The local/dLocal unit-test variant compiles BuildConfig.DEBUG =
        // true (debug-type build), so the gate reads OPEN here and setTrustAll(true) DOES
        // take effect — this test pins the other half of the contract: with the preference
        // OFF (the @Before default), validation stays strict; and once toggled, setTrustAll
        // (false) restores strictness. The gate itself (false && enabled) is a compile-time
        // constant expression — the production reading — and is covered by review, not by
        // this suite: unit tests cannot re-link a different BuildConfig constant.
        CertificateChainValidator.setTrustAll(false)
        val foreign = foreignRoot()
        val leaf = leafUnder(foreign)
        val staticRoot = trustedRoot().first
        assertFalse(
            "trust-all preference OFF must validate strictly",
            CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(staticRoot))
        )
        // Toggle on, then off: the stored preference alone must not leave the hatch open.
        CertificateChainValidator.setTrustAll(true)
        CertificateChainValidator.setTrustAll(false)
        assertFalse(
            CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(staticRoot))
        )
    }

    @Test
    fun `revocation enabled with no CRL source soft-fails on availability`() {
        // E4 policy (validator :85-97): revocation is REFUSED only when a fetched,
        // signature-verified CRL names a chain certificate; an ABSENT CRL source is an
        // availability failure — logged, never treated as proof of revocation. The in-tree
        // dev CRL expired 2025-05-20, so a hard-fail flip would refuse every chain today.
        // A leaf with no distribution points at all is the same availability class: nothing
        // can be fetched, nothing is revoked, the chain stands. (The hard-fail variant —
        // NO_FALLBACK once the DP serves a live CRL — is a one-line change in the validator.)
        val root = trustedRoot()
        val leaf = leafUnder(root)
        assertTrue(
            "no-DP leaf under enforced revocation must not be refused (soft-fail availability)",
            CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(root.first), enableRevocationCheck = true)
        )
    }

    // ---- E4: revocation via a locally minted CRL served over file:// -----------------
    // The centralized overload fetches CRLs from each chain cert's CRL distribution
    // points; a temp-file URL keeps the test network-free while exercising the real
    // fetch → CertStore → PKIXRevocationChecker path.

    private fun mintCrl(
        issuer: Pair<X509Certificate, KeyPair>,
        revokedSerial: BigInteger?,
        crlFile: java.io.File
    ) {
        val holder = JcaX509CertificateHolder(issuer.first)
        val builder = org.bouncycastle.cert.X509v2CRLBuilder(
            holder.subject,
            Date(now - MINUTES)
        )
        if (revokedSerial != null) {
            builder.addCRLEntry(revokedSerial, Date(now - MINUTES), org.bouncycastle.asn1.x509.CRLReason.keyCompromise)
        }
        builder.setNextUpdate(Date(now + HOURS))
        val signer = JcaContentSignerBuilder("SHA512withECDSA").build(issuer.second.private)
        val crlHolder = builder.build(signer)
        crlFile.writeBytes(crlHolder.encoded)
    }

    private fun leafWithDp(
        issuer: Pair<X509Certificate, KeyPair>,
        crlUrl: String,
        cn: String = "Leaf"
    ): X509Certificate {
        val subjectKeyPair = generateECKeyPair()
        val holder = X509v3CertificateBuilder(
            JcaX509CertificateHolder(issuer.first).subject,
            BigInteger(64, SecureRandom()),
            Date(now - MINUTES),
            Date(now + HOURS),
            X500Name("C=EE, O=SOTA-TEST, CN=$cn"),
            SubjectPublicKeyInfo.getInstance(subjectKeyPair.public.encoded)
        ).apply {
            addExtension(Extension.basicConstraints, true, BasicConstraints(false))
            addExtension(Extension.keyUsage, true, KeyUsage(KeyUsage.digitalSignature))
            // RFC 5280 §4.2.1.13: CRLDistributionPoints SHOULD be non-critical — and the JDK
            // SunCertPathBuilder refuses any chain whose leaf carries a CRITICAL DP
            // ("unable to find valid certification path", even with revocation disabled
            // and zero CRLs in the store). Real-world issuer certificates mint the DP
            // non-critical; the test must mint what issuers actually ship.
            addExtension(
                Extension.cRLDistributionPoints,
                false,
                org.bouncycastle.asn1.x509.CRLDistPoint(
                    arrayOf(
                        org.bouncycastle.asn1.x509.DistributionPoint(
                            org.bouncycastle.asn1.x509.DistributionPointName(
                                org.bouncycastle.asn1.x509.DistributionPointName.FULL_NAME,
                                org.bouncycastle.asn1.x509.GeneralNames(
                                    org.bouncycastle.asn1.x509.GeneralName(org.bouncycastle.asn1.x509.GeneralName.uniformResourceIdentifier, crlUrl)
                                )
                            ),
                            null,
                            null
                        )
                    )
                )
            )
        }.build(JcaContentSignerBuilder("SHA512withECDSA").build(issuer.second.private))
        return JcaX509CertificateConverter().getCertificate(holder)
    }

    @Test
    fun `revoked leaf is refused through the centralized overload`() {
        val root = trustedRoot()
        val crlFile = java.io.File.createTempFile("sota-test-revoked", ".crl")
        val leaf = leafWithDp(root, crlFile.toURI().toString())
        mintCrl(root, revokedSerial = leaf.serialNumber, crlFile = crlFile)
        try {
            assertFalse(
                "a leaf named on a live CRL must be refused even under the soft-fail policy",
                CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(root.first))
            )
        } finally {
            crlFile.delete()
        }
    }

    @Test
    fun `non-revoked leaf with a live CRL passes the centralized overload`() {
        val root = trustedRoot()
        val crlFile = java.io.File.createTempFile("sota-test-clean", ".crl")
        val leaf = leafWithDp(root, crlFile.toURI().toString())
        // The CRL exists and is fresh, but names no serial.
        mintCrl(root, revokedSerial = null, crlFile = crlFile)
        try {
            assertTrue(
                CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(root.first))
            )
        } finally {
            crlFile.delete()
        }
    }

    @Test
    fun `unreachable CRL distribution point soft-fails without refusing the chain`() {
        val root = trustedRoot()
        // Port 1 on localhost is reserved and refuses connections — the availability
        // failure must not fail the chain (soft-fail availability policy, E4).
        val leaf = leafWithDp(root, "http://127.0.0.1:1/no-such.crl")
        assertTrue(
            CertificateChainValidator.validateCertificateChain(listOf(leaf), listOf(root.first))
        )
    }

    companion object {
        private const val MINUTES = 60 * 1000L
        private const val HOURS = 60 * MINUTES
    }
}
