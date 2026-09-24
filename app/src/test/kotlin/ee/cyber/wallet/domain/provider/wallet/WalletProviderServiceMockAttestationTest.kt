package ee.cyber.wallet.domain.provider.wallet

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import ee.cyber.wallet.security.KeyStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security
import java.security.interfaces.RSAPublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECGenParameterSpec
import java.security.spec.RSAKeyGenParameterSpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JVM tests for the mock wallet provider's key-attestation path (review finding 4, EE-SEC-003):
 * the provider signs every key attestation with its OWN provider key — never with the attested
 * (holder) key — and refuses a private or symmetric JWK outright. The holder private key never
 * enters this process, so nothing here touches Android Keystore and the whole path runs on the JVM.
 */
class WalletProviderServiceMockAttestationTest {

    /** In-memory BKS stand-in for [ee.cyber.wallet.security.EncryptedKeyStoreManager]. */
    private class InMemoryKeyStoreManager : KeyStoreManager {

        private val keyStore: KeyStore = KeyStore.getInstance("BKS").apply { load(null, null) }

        override fun keyStore(): KeyStore = keyStore

        override fun containsKey(alias: String): Boolean = keyStore.containsAlias(alias)

        override fun deleteKey(alias: String) {
            keyStore.deleteEntry(alias)
        }

        override fun createKeyPair(alias: String, keyAlgorithm: String, certSignAlgorithm: String, paramSpec: AlgorithmParameterSpec) {
            val keyPair = KeyPairGenerator.getInstance(keyAlgorithm).apply { initialize(paramSpec) }.generateKeyPair()
            keyStore.setKeyEntry(alias, keyPair.private, "".toCharArray(), arrayOf(keyPair.selfSignedCertificate(certSignAlgorithm, alias)))
        }

        override fun clearAll() {
            keyStore.aliases().toList().forEach { keyStore.deleteEntry(it) }
        }

        internal fun KeyPair.selfSignedCertificate(signAlgorithm: String, cn: String): java.security.cert.Certificate {
            val now = Instant.now()
            val holder = X509v3CertificateBuilder(
                X500Name("CN=$cn"),
                BigInteger(64, SecureRandom()),
                Date.from(now),
                Date.from(now.plus(60 * 30, ChronoUnit.DAYS)),
                X500Name("CN=$cn"),
                SubjectPublicKeyInfo.getInstance(public.encoded)
            ).build(JcaContentSignerBuilder(signAlgorithm).build(private))
            return JcaX509CertificateConverter().getCertificate(holder)
        }
    }

    private lateinit var keyStore: InMemoryKeyStoreManager
    private lateinit var service: WalletProviderServiceMock

    @BeforeTest
    fun setUp() {
        Security.addProvider(BouncyCastleProvider())
        keyStore = InMemoryKeyStoreManager()
        service = WalletProviderServiceMock(keyStore, Dispatchers.Unconfined)
    }

    private fun credentials() = WalletInstanceCredentials(instanceId = "instance", instancePassword = "password")

    /** A holder EC key's PUBLIC JWK exactly as the SecureArea path produces it: no private part. */
    private fun publicHolderEcJwk(keyId: String): ECKey {
        val keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp521r1"))
        }.generateKeyPair()
        // A real (parseable) self-signed certificate, like SecureAreaDeviceKey.jwk() builds from
        // the key's attestation chain; Nimbus validates x5c entries at ECKey build time.
        val cert = with(keyStore) { keyPair.selfSignedCertificate("SHA256withECDSA", keyId) }
        val x5c = listOf(com.nimbusds.jose.util.Base64.encode(cert.encoded))
        return ECKey.Builder(com.nimbusds.jose.jwk.Curve.P_521, keyPair.public as java.security.interfaces.ECPublicKey)
            .keyID(keyId)
            .keyUse(KeyUse.SIGNATURE)
            .x509CertChain(x5c)
            .build()
    }

    private fun providerPublicKey(): PublicKey =
        keyStore.keyStore().getCertificate(WalletProviderServiceMock.PROVIDER_KEY_ALIAS).publicKey

    @Test
    fun `attests a public-only holder EC key signed by the provider key`() = runTest {
        val holderJwk = publicHolderEcJwk("holder-1")
        assertFalse(keyStore.containsKey(WalletProviderServiceMock.PROVIDER_KEY_ALIAS))

        val attestation = service.attestKey("holder-1", KeyType.EC, holderJwk, credentials())

        assertEquals("holder-1", attestation.keyId)
        assertEquals(KeyType.EC, attestation.keyType)
        // The signature is the PROVIDER's (RSA), so the JWS header alg follows the signer, not
        // the attested key; the attested key's own algorithm lives in its "jwk" claim.
        assertEquals(WalletProviderServiceMock.PROVIDER_KEY_JWS_ALGORITHM, attestation.jwt.header.algorithm)

        // The attested key (jwk claim) is the holder PUBLIC key, verbatim.
        val attested = JWK.parse(attestation.jwt.jwtClaimsSet.getJSONObjectClaim("jwk")) as ECKey
        assertEquals(holderJwk.curve, attested.curve)
        assertEquals(holderJwk.x, attested.x)
        assertEquals(holderJwk.y, attested.y)
        assertEquals(holderJwk.x509CertChain, attested.x509CertChain)
        assertFalse(attested.isPrivate)

        // The signature verifies against the PROVIDER's key — not the holder key — proving the
        // attestation never required holder private-key material.
        assertTrue(attestation.jwt.verify(RSASSAVerifier(providerPublicKey() as RSAPublicKey)))
        assertTrue(keyStore.containsKey(WalletProviderServiceMock.PROVIDER_KEY_ALIAS))
    }

    @Test
    fun `refuses a private holder JWK`() = runTest {
        // A private JWK in the wallet process would mean holder key material crossed the
        // process boundary — exactly what EE-SEC-003 forbids.
        val privateJwk = JWK.parse(
            """{"kty":"EC","crv":"P-256","x":"MKBCTNIcKUSDii11ySs3526iDZ8AiTo7Tu6KPAqv7D4",
                "y":"4Etl6SRW2YiLUrN5vfvVHuhp7x8PxltmWWlbbM4IFyM","d":"870MB6gfuTJ4HtUnUvYMyJpr5eUZNP4Bk43bVdj3eAE"}"""
        )
        assertTrue(privateJwk.isPrivate)

        val failure = assertFailsWith<IllegalArgumentException> {
            service.attestKey("holder-2", KeyType.EC, privateJwk, credentials())
        }
        assertTrue(failure.message!!.contains("PUBLIC JWK"))
    }

    @Test
    fun `refuses a symmetric JWK`() = runTest {
        val symmetricJwk = com.nimbusds.jose.jwk.OctetSequenceKey.Builder(Base64URLBytes())
            .keyID("sym-1")
            .build()

        val failure = assertFailsWith<IllegalArgumentException> {
            service.attestKey("holder-3", KeyType.EC, symmetricJwk, credentials())
        }
        assertTrue(failure.message!!.contains("PUBLIC JWK"))
    }

    @Test
    fun `reuses one provider signing key across attestations`() = runTest {
        val first = service.attestKey("holder-4", KeyType.EC, publicHolderEcJwk("holder-4"), credentials())
        val second = service.attestKey("holder-5", KeyType.EC, publicHolderEcJwk("holder-5"), credentials())

        val providerKey = providerPublicKey() as RSAPublicKey
        assertTrue(first.jwt.verify(RSASSAVerifier(providerKey)))
        assertTrue(second.jwt.verify(RSASSAVerifier(providerKey)))
    }

    private fun Base64URLBytes() = com.nimbusds.jose.util.Base64URL.encode("0123456789abcdef".toByteArray())
}
