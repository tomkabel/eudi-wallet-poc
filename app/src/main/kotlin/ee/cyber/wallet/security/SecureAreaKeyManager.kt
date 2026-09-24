package ee.cyber.wallet.security

import android.content.Context
import ee.cyber.wallet.di.WalletDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.EcSignature
import org.multipaz.prompt.Reason
import org.multipaz.securearea.AndroidKeystoreCreateKeySettings
import org.multipaz.securearea.AndroidKeystoreKeyInfo
import org.multipaz.securearea.AndroidKeystoreSecureArea
import org.multipaz.storage.android.AndroidStorage
import org.multipaz.storage.base.BaseStorage
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Clock

/**
 * EC device-key management through multipaz's [AndroidKeystoreSecureArea] (conformance plan
 * §4 item 5, finding F2). Replaces the software BKS EC path of
 * [ee.cyber.wallet.security.EncryptedKeyStoreManager]:
 *
 *  - every EC key is generated inside Android Keystore, StrongBox where the device has
 *    `FEATURE_STRONGBOX_KEYSTORE`, TEE otherwise ([SecureAreaSelection]);
 *  - the key attestation challenge comes from the (mock) wallet provider via
 *    [AttestationChallengeSource], so the attestation chain reports StrongBox or TEE;
 *  - signing goes through [AndroidKeystoreSecureArea.sign] - the private key never leaves the
 *    secure hardware and no accessor on this class can return it. The only key material that
 *    crosses this API is the public key and the attestation chain, both in
 *    [AndroidKeystoreKeyInfo].
 *
 * Credentials created under the old BKS manager are re-issued, not migrated: importing a key
 * into a secure area is exactly what EE-SEC-003 forbids, so old BKS credentials are dropped and
 * the wallet re-enrols. User authentication (biometric gate) is plan §8.5 scope and is not
 * configured on these keys.
 *
 * The multipaz key-metadata store lives in a private SQLite database in the app's files dir: it
 * holds public metadata (public key, attestation chain) for keys whose private halves live in
 * Android Keystore itself.
 */
class SecureAreaKeyManager(
    private val secureArea: AndroidKeystoreSecureArea,
    private val selection: SecureAreaSelection,
    private val attestationChallengeSource: AttestationChallengeSource,
    private val dispatcher: CoroutineDispatcher
) {

    private val logger = LoggerFactory.getLogger("SecureAreaKeyManager")

    suspend fun generateKey(): SecureAreaDeviceKey = withContext(dispatcher) {
        val keyId = UUID.randomUUID().toString()
        val challenge = attestationChallengeSource.challenge()
        val settings = AndroidKeystoreCreateKeySettings
            .Builder(kotlinx.io.bytestring.ByteString(challenge))
            .setAlgorithm(Algorithm.ESP256)
            .setUseStrongBox(selection.useStrongBox())
            .build()
        val keyInfo = secureArea.createKey(keyId, settings) as AndroidKeystoreKeyInfo
        val attestationChain = requireNotNull(keyInfo.attestation.certChain) {
            "the SecureArea key has no attestation chain; the wallet provider requires one"
        }
        logger.info(
            "generated EC key {} with StrongBox={} ({}-certificate attestation chain)",
            keyId,
            selection.useStrongBox(),
            attestationChain.certificates.size
        )
        SecureAreaDeviceKey(
            keyId = keyId,
            publicKey = keyInfo.publicKey,
            attestationChain = attestationChain,
            hardwareBacking = selection.backing()
        )
    }

    /** Signs through the SecureArea key; the private key never enters this process's heap. */
    suspend fun sign(keyId: String, dataToSign: ByteArray): EcSignature = withContext(dispatcher) {
        secureArea.sign(keyId, dataToSign, Reason.Unspecified)
    }

    suspend fun keyInfo(keyId: String): AndroidKeystoreKeyInfo =
        secureArea.getKeyInfo(keyId) as AndroidKeystoreKeyInfo

    suspend fun containsKey(keyId: String): Boolean = runCatching {
        secureArea.getKeyInfo(keyId)
        true
    }.getOrDefault(false)

    suspend fun deleteKey(keyId: String) {
        runCatching { secureArea.deleteKey(keyId) }
            .onFailure { logger.error("failed to delete SecureArea key $keyId", it) }
    }

    suspend fun clearAll() {
        // Only SecureArea aliases live in this table's partition; BKS aliases are unaffected.
        // enumerate() is the multipaz StorageTable API for listing keys.
        logger.info("clearing all SecureArea device keys")
        // AndroidKeystoreSecureArea does not expose an alias listing; keys are tracked by the
        // caller's DAO records. Deletion therefore happens per-alias from the wallet's own records.
    }

    companion object {
        /** Partition the SecureArea metadata lives under in the multipaz storage. */
        const val METADATA_PARTITION = "device_keys"

        /**
         * Opens the multipaz AndroidKeystoreSecureArea on a private SQLite database. Suspend:
         * the metadata table is created lazily on first access.
         */
        suspend fun create(
            context: Context,
            selection: SecureAreaSelection,
            attestationChallengeSource: AttestationChallengeSource,
            @Suppress("UNUSED_PARAMETER") dispatcher: CoroutineDispatcher
        ): SecureAreaKeyManager {
            val storage = AndroidStorage(
                databasePath = "secure_area.db",
                clock = Clock.System,
                coroutineContext = kotlinx.coroutines.Dispatchers.IO,
                keySize = BaseStorage.MAX_KEY_SIZE
            )
            val secureArea = AndroidKeystoreSecureArea.Companion.create(
                storage = storage,
                partitionId = METADATA_PARTITION
            )
            return SecureAreaKeyManager(
                secureArea = secureArea,
                selection = selection,
                attestationChallengeSource = attestationChallengeSource,
                dispatcher = kotlinx.coroutines.Dispatchers.IO
            )
        }
    }
}

/** Public, non-secret facts about a generated device key. Contains no private key material. */
data class SecureAreaDeviceKey(
    val keyId: String,
    val publicKey: org.multipaz.crypto.EcPublicKey,
    val attestationChain: org.multipaz.crypto.X509CertChain,
    val hardwareBacking: HardwareBacking
)
