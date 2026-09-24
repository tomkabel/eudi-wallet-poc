package ee.cyber.wallet.domain.provider.wallet

import android.security.keystore.KeyProperties
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSObject
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.Payload
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import ee.cyber.wallet.crypto.jwsSigner
import ee.cyber.wallet.security.KeyStoreManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.bouncycastle.jce.ECNamedCurveTable
import org.slf4j.LoggerFactory
import java.security.spec.RSAKeyGenParameterSpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

/**
 * The (mock) wallet provider. EE-SEC-003 and step 5: the holder's private key never enters this
 * process — the provider signs every attestation it issues with its OWN provider key
 * ([providerKeyAlias], generated once into the same BKS keystore the RSA wallet keys live in),
 * exactly how [ee.cyber.wallet.domain.credentials.CredentialIssuanceServiceMock] signs issued
 * credentials. [attestKey] receives the holder key's PUBLIC JWK only and refuses a private one,
 * so the attestation path cannot require holder private-key material by construction.
 */
class WalletProviderServiceMock(
    private val keyStore: KeyStoreManager,
    private val dispatcher: CoroutineDispatcher
) : WalletProviderService {

    private val logger = LoggerFactory.getLogger("WalletProviderServiceMock")

    /** Ensures the provider signing key exists; safe to call repeatedly. */
    private fun ensureProviderKey() {
        if (!keyStore.containsKey(PROVIDER_KEY_ALIAS)) {
            keyStore.createKeyPair(
                alias = PROVIDER_KEY_ALIAS,
                keyAlgorithm = KeyProperties.KEY_ALGORITHM_RSA,
                certSignAlgorithm = KeyType.RSA.jwsAlgorithm().asJavaAlgorithm(),
                paramSpec = RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4)
            )
        }
    }

    override suspend fun registerWalletInstance(deviceData: DeviceData): WalletInstanceCredentials {
        delay(3000)
        return WalletInstanceCredentials(
            instanceId = UUID.randomUUID().toString(),
            instancePassword = UUID.randomUUID().toString()
        )
    }

    override suspend fun activateInstance(personalDataAccessToken: ByteArray, credentials: WalletInstanceCredentials) = delay(1000)

    override fun supportsKey(keyType: KeyType): Boolean = keyType == KeyType.RSA

    override suspend fun generateKey(keyType: KeyType, credentials: WalletInstanceCredentials): KeyAttestation = withContext(dispatcher) {
        if (!supportsKey(keyType)) throw IllegalArgumentException("Key type not supported: $keyType")

        with(UUID.randomUUID().toString()) {
            createKeyPair(keyType, this)
            createKeyAttestation(keyType, this)
        }
    }

    private fun createKeyAttestation(keyType: KeyType, keyId: String): KeyAttestation {
        val jwk = loadJwkFromKeyStore(keyId)
        val now = Instant.now()
        val keyAttestation = JWSObject(
            JWSHeader.Builder(keyType.jwsAlgorithm())
                .x509CertChain(jwk.x509CertChain)
                .build(),
            Payload(
                JWTClaimsSet.Builder()
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(60 * 30, ChronoUnit.DAYS)))
                    .claim("jwk", jwk.toPublicJWK().toJSONObject())
                    .build().toJSONObject()
            )
        ).also {
            it.sign(jwsSigner(jwk))
        }.serialize()

        return KeyAttestation(
            keyId = keyId,
            attestation = keyAttestation,
            keyType = keyType
        )
    }

    private fun createKeyPair(keyType: KeyType, keyId: String) = when (keyType) {
        KeyType.RSA -> keyStore.createKeyPair(
            alias = keyId,
            keyAlgorithm = KeyProperties.KEY_ALGORITHM_RSA,
            certSignAlgorithm = keyType.jwsAlgorithm().asJavaAlgorithm(),
            paramSpec = RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4)
        )

        KeyType.EC -> keyStore.createKeyPair(
            alias = keyId,
            keyAlgorithm = KeyProperties.KEY_ALGORITHM_EC,
            certSignAlgorithm = keyType.jwsAlgorithm().asJavaAlgorithm(),
            paramSpec = ECNamedCurveTable.getParameterSpec("secp521r1")
        )
    }

    override suspend fun sign(keyAttestation: KeyAttestation, dataToBeSigned: ByteArray, credentials: WalletInstanceCredentials): ByteArray = withContext(dispatcher) {
        val jwk = loadJwkFromKeyStore(keyAttestation.keyId)
        jwsSigner(jwk).sign(JWSHeader(keyAttestation.jwsAlgorithm), dataToBeSigned).decode()
    }

    override suspend fun attestKey(keyId: String, keyType: KeyType, jwk: JWK, credentials: WalletInstanceCredentials): KeyAttestation {
        // The attested key is the HOLDER key: its JWK arrives here from the wallet process, so it
        // must be public-only — the holder private key never leaves secure hardware (EE-SEC-003).
        // A private JWK here would mean holder key material crossed the process boundary; a
        // symmetric key cannot be a holder device key at all. Refuse both instead of using them.
        require(!jwk.isPrivate && jwk !is OctetSequenceKey) {
            "attestKey must receive the holder key's PUBLIC JWK; got a ${jwkDescription(jwk)}"
        }
        ensureProviderKey()
        // Loaded per call, not cached: deleteAllData clears the keystore, and ensureProviderKey
        // then mints a new provider key the next attestation must be signed with.
        val providerJwk = loadJwkFromKeyStore(PROVIDER_KEY_ALIAS)
        val issuedAt = Instant.now()
        val expiresAt = issuedAt.plus(60 * 30, ChronoUnit.DAYS)
        val keyAttestation = JWSObject(
            // alg follows the SIGNER (the provider key), per JWS; the attested key's algorithm
            // is a property of the attested "jwk" claim, not of the signature over it. x5c is the
            // provider's own chain: this certificate is what the signature should be trusted to.
            JWSHeader.Builder(PROVIDER_KEY_JWS_ALGORITHM)
                .x509CertChain(providerJwk.x509CertChain)
                .build(),
            Payload(
                JWTClaimsSet.Builder()
                    .issueTime(Date.from(issuedAt))
                    .expirationTime(Date.from(expiresAt))
                    .claim("jwk", jwk.toPublicJWK().toJSONObject())
                    .build().toJSONObject()
            )
        ).also {
            // Signed by the PROVIDER key, not the attested (holder) key: the provider vouches for
            // the holder public key, which is exactly what a key attestation is.
            it.sign(jwsSigner(providerJwk))
        }.serialize()
        return KeyAttestation(
            keyId = keyId,
            attestation = keyAttestation,
            keyType = keyType
        )
    }

    private fun jwsSigner(jwk: JWK): JWSSigner = DefaultJWSSignerFactory().createJWSSigner(jwk)

    private fun jwkDescription(jwk: JWK): String =
        if (jwk is OctetSequenceKey) {
            "symmetric JWK"
        } else {
            "${if (jwk.isPrivate) "private" else "public"} ${jwk::class.simpleName}"
        }

    private fun loadJwkFromKeyStore(keyId: String): JWK = JWK.load(keyStore.keyStore(), keyId, password)

    companion object {
        private val password = "".toCharArray()

        /** Alias of the mock provider's own signing key inside the BKS keystore. */
        const val PROVIDER_KEY_ALIAS = "wallet-provider-mock-signing"

        /** JWS algorithm of the provider signing key (RSA, fixed by [ensureProviderKey]). */
        val PROVIDER_KEY_JWS_ALGORITHM = KeyType.RSA.jwsAlgorithm()
    }
}
