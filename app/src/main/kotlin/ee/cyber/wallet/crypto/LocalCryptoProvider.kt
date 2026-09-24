package ee.cyber.wallet.crypto

import android.security.keystore.KeyProperties
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.crypto.factories.DefaultJWSSignerFactory
import com.nimbusds.jose.jca.JCAContext
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.util.Base64URL
import ee.cyber.wallet.data.database.KeyAttestationEntity
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.data.database.toModel
import ee.cyber.wallet.data.datastore.WalletInstanceCredentialsDataSource
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.domain.provider.wallet.asJavaAlgorithm
import ee.cyber.wallet.domain.provider.wallet.jwsAlgorithm
import ee.cyber.wallet.security.EncryptedKeyStoreManager
import ee.cyber.wallet.security.SecureAreaKeyManager
import ee.cyber.wallet.security.jwk
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.security.spec.RSAKeyGenParameterSpec
import java.util.UUID

/**
 * The wallet-side [CryptoProvider].
 *
 * Since step 5 (conformance plan §4 item 5, finding F2) EC keys no longer live in the software
 * BKS keystore: they are generated inside Android Keystore through [SecureAreaKeyManager]
 * (multipaz AndroidKeystoreSecureArea, StrongBox where the device has the feature, TEE
 * otherwise), and DeviceAuthentication signatures go through the same key via
 * [SecureAreaCOSECryptoProvider]. The RSA path stays on BKS: RSA keys are issued-attestation
 * material handled with the (mock) wallet provider, not device-signing keys.
 *
 * There is deliberately no accessor that returns private key bytes for EC keys: the SecureArea
 * API only exposes sign(), the public key and the attestation chain (EE-SEC-003). Credentials
 * minted under the old software EC path are re-issued, not migrated - importing a key into a
 * secure area is exactly what EE-SEC-003 forbids.
 */
class LocalCryptoProvider(
    private val keyAttestationDao: KeyAttestationDao,
    private val keyStoreManager: EncryptedKeyStoreManager,
    private val walletProviderService: WalletProviderService,
    private val secureAreaKeyManager: SecureAreaKeyManager,
    private val walletInstanceCredentialsDataSource: WalletInstanceCredentialsDataSource
) : CryptoProvider {

    private val logger = LoggerFactory.getLogger("LocalCryptoProvider")

    private val credentials = walletInstanceCredentialsDataSource.credentials

    override suspend fun generateKey(keyType: KeyType): KeyAttestation {
        logger.debug("generateKey: {}", keyType)
        if (!supports(keyType)) throw IllegalArgumentException("Key type not supported: $keyType")

        return when (keyType) {
            KeyType.EC -> generateSecureAreaKey()
            KeyType.RSA -> with(UUID.randomUUID().toString()) {
                createKeyPair(keyType, this)
                createKeyAttestation(keyType, this)
                    .also {
                        keyAttestationDao.insert(
                            KeyAttestationEntity(
                                id = it.keyId,
                                attestation = it.attestation,
                                keyType = keyType.name
                            )
                        )
                    }
            }
        }
    }

    /**
     * EC keys: generated in the SecureArea, attested by the (mock) wallet provider over the
     * SecureArea's own attestation chain. The key attestation JWS records the StrongBox/TEE
     * backing and the provider challenge, and its `jwk` claim carries the SecureArea public key.
     */
    private suspend fun generateSecureAreaKey(): KeyAttestation {
        val key = secureAreaKeyManager.generateKey()
        // The keyAttestation row is the only record naming the alias (SecureAreaKeyCleanup), so a
        // failure before it is written deletes the key instead of orphaning a hardware key slot.
        return try {
            val attestation = walletProviderService.attestKey(
                keyId = key.keyId,
                keyType = KeyType.EC,
                jwk = key.jwk(),
                credentials = credentials.first()
            )
            keyAttestationDao.insert(
                KeyAttestationEntity(
                    id = attestation.keyId,
                    attestation = attestation.attestation,
                    keyType = KeyType.EC.name
                )
            )
            attestation
        } catch (e: Exception) {
            withContext(NonCancellable) { secureAreaKeyManager.deleteKey(key.keyId) }
            throw e
        }
    }

    override suspend fun getKeyAttestation(keyId: String): KeyAttestation =
        keyAttestationDao.getById(keyId).toModel()

    override suspend fun sign(keyId: String, dataToSign: ByteArray): ByteArray {
        logger.debug("sign: $keyId")
        val keyAttestation = getKeyAttestation(keyId)
        return when (keyAttestation.keyType) {
            // JWS ECDSA signatures are DER-encoded; SecureArea.sign returns raw r||s via EcSignature.
            KeyType.EC -> secureAreaKeyManager.sign(keyId, dataToSign).toDerEncoded()
            KeyType.RSA -> with(jwsSigner(keyAttestation)) {
                sign(JWSHeader(keyAttestation.jwsAlgorithm), dataToSign).decode()
            }
        }
    }

    override suspend fun jwsSigner(keyId: String): JWSSigner = jwsSigner(getKeyAttestation(keyId))

    private fun jwsSigner(key: KeyAttestation): JWSSigner = when (key.keyType) {
        KeyType.EC -> SecureAreaJwsSigner(key.keyId, secureAreaKeyManager)
        KeyType.RSA -> keyStoreManager.keyStore().let { ks ->
            DefaultJWSSignerFactory().createJWSSigner(RSAKey.load(ks, key.keyId, password))
        }
    }

    /**
     * Nimbus [JWSSigner] over the SecureArea key: produces ES256 compact JWS signatures without
     * ever holding the private key. The public JWK comes from the key attestation record.
     */
    private class SecureAreaJwsSigner(
        private val keyId: String,
        private val keyManager: SecureAreaKeyManager
    ) : JWSSigner {
        override fun getJCAContext(): JCAContext = JCAContext()
        override fun supportedJWSAlgorithms() = mutableSetOf(JWSAlgorithm.ES256)
        override fun sign(header: JWSHeader, signingInput: ByteArray): Base64URL {
            val der = kotlinx.coroutines.runBlocking { keyManager.sign(keyId, signingInput).toDerEncoded() }
            return Base64URL.encode(der)
        }
    }

    override fun supports(keyType: KeyType): Boolean = keyType == KeyType.EC || keyType == KeyType.RSA

    override suspend fun clearAll() {
        runCatching {
            keyStoreManager.clearAll()
            keyAttestationDao.deleteAll()
        }
    }

    private suspend fun createKeyAttestation(keyType: KeyType, keyId: String): KeyAttestation =
        walletProviderService.attestKey(keyId, keyType, loadJwkFromKeyStore(keyId), credentials.first())

    private fun createKeyPair(keyType: KeyType, keyId: String) {
        require(keyType == KeyType.RSA) { "only RSA keys are generated in the software keystore since step 5" }
        keyStoreManager.createKeyPair(
            alias = keyId,
            keyAlgorithm = KeyProperties.KEY_ALGORITHM_RSA,
            certSignAlgorithm = keyType.jwsAlgorithm().asJavaAlgorithm(),
            paramSpec = RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4)
        )
    }

    private fun loadJwkFromKeyStore(keyId: String) =
        RSAKey.load(keyStoreManager.keyStore(), keyId, password)

    companion object {
        private val password = "".toCharArray()
    }
}
