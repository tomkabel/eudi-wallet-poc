package ee.cyber.wallet.security

import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.cert.X509Certificate
import java.security.spec.ECGenParameterSpec
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * PKI plan NEW-S1-2: the shared certificate algorithm policy (algorithms.json, the same file the
 * Go verifier embeds). Each case mints a self-signed certificate and asks the policy directly, so a
 * loosened rule fails here and not only in an end-to-end chain test.
 */
class AlgorithmPolicyTest {

    private fun ecKeys(curve: String): KeyPair =
        KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec(curve)) }.generateKeyPair()

    private fun rsaKeys(bits: Int): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(bits) }.generateKeyPair()

    private fun cert(keys: KeyPair, signatureAlgorithm: String): X509Certificate {
        val name = X500Name("CN=policy-test")
        val now = System.currentTimeMillis()
        val holder = X509v3CertificateBuilder(
            name,
            BigInteger.ONE,
            Date(now - 3_600_000),
            Date(now + 3_600_000),
            name,
            SubjectPublicKeyInfo.getInstance(keys.public.encoded)
        ).build(JcaContentSignerBuilder(signatureAlgorithm).build(keys.private))
        return JcaX509CertificateConverter().getCertificate(holder)
    }

    @Test
    fun `P-256 with ECDSA SHA-256 meets the policy`() {
        assertNull(AlgorithmPolicy.violation(cert(ecKeys("secp256r1"), "SHA256withECDSA")))
    }

    @Test
    fun `P-384 with ECDSA SHA-384 meets the policy`() {
        assertNull(AlgorithmPolicy.violation(cert(ecKeys("secp384r1"), "SHA384withECDSA")))
    }

    @Test
    fun `RSA at the 2048 bit floor meets the policy`() {
        assertNull(AlgorithmPolicy.violation(cert(rsaKeys(2048), "SHA256withRSA")))
    }

    @Test
    fun `P-521 is refused even with an allowed signature algorithm`() {
        val reason = AlgorithmPolicy.violation(cert(ecKeys("secp521r1"), "SHA512withECDSA"))
        assertNotNull(reason, "P-521 is not in the policy")
        assertContains(reason, "curve")
    }

    @Test
    fun `RSA below the floor is refused`() {
        val reason = AlgorithmPolicy.violation(cert(rsaKeys(1024), "SHA256withRSA"))
        assertNotNull(reason, "RSA-1024 is below the minimum")
        assertContains(reason, "RSA-1024")
    }

    @Test
    fun `SHA-1 signatures are refused`() {
        val reason = AlgorithmPolicy.violation(cert(rsaKeys(2048), "SHA1withRSA"))
        assertNotNull(reason, "SHA1withRSA is not in the policy")
        assertContains(reason, "signature algorithm")
    }
}
