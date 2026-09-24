package ee.cyber.wallet.security

import android.content.Context
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.domain.provider.wallet.KeyType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
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
 * The multipaz key-metadata store lives in a private SQLite database in the app's no-backup files
 * dir: it holds public metadata (public key, attestation chain) for keys whose private halves live
 * in Android Keystore itself, so a restored copy would name keys the new device does not have.
 */
class SecureAreaKeyManager(
    private val secureArea: AndroidKeystoreSecureArea,
    private val selection: SecureAreaSelection,
    private val attestationChallengeSource: AttestationChallengeSource,
    private val dispatcher: CoroutineDispatcher
) : SecureAreaKeyDeleter {

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

    /**
     * Batch key creation for the EE-PoA batches (conformance plan §4 item 6): one Android
     * Keystore round trip for [count] keys instead of [count] single generations, called at
     * issuance time — pre-generation off the critical path (EE-POA-011a) is plan §8.4. Every
     * key carries the same settings — the provider challenge and the StrongBox/TEE selection of
     * this device — and is immediately usable as a presentation key.
     *
     * multipaz 0.99.0's AndroidKeystoreSecureArea inherits the default [batchCreateKey], which
     * loops [generateKey]-equivalent creation internally; the win is the shared settings and the
     * single call site the issuer needs.
     */
    suspend fun batchCreateKey(count: Int): SecureAreaKeyBatch = withContext(dispatcher) {
        require(count >= 1) { "a batch has at least one key" }
        val challenge = attestationChallengeSource.challenge()
        val settings = AndroidKeystoreCreateKeySettings
            .Builder(kotlinx.io.bytestring.ByteString(challenge))
            .setAlgorithm(Algorithm.ESP256)
            .setUseStrongBox(selection.useStrongBox())
            .build()
        val result = secureArea.batchCreateKey(count, settings)
        val keys = result.keyInfos.map { keyInfo ->
            val attestationChain = requireNotNull(keyInfo.attestation.certChain) {
                "the batch SecureArea key ${keyInfo.alias} has no attestation chain"
            }
            SecureAreaDeviceKey(
                keyId = keyInfo.alias,
                publicKey = keyInfo.publicKey,
                attestationChain = attestationChain,
                hardwareBacking = selection.backing()
            )
        }
        logger.info("batch-created {} SecureArea keys", keys.size)
        SecureAreaKeyBatch(keys)
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

    override suspend fun deleteKey(keyId: String) {
        runCatching { secureArea.deleteKey(keyId) }
            .onFailure { logger.error("failed to delete SecureArea key $keyId", it) }
    }

    /**
     * Best-effort sweep of SecureArea aliases the wallet's records no longer name (crash between
     * key generation and record insert, or a lost secure_area.db). AndroidKeystoreSecureArea
     * exposes no alias listing in 0.99.0, so this is deliberately empty; the per-alias deletion
     * driven by the keyAttestation rows is [SecureAreaKeyCleanup]'s job.
     */
    override suspend fun deleteAllKeys() {
        logger.info("deleteAllKeys: no alias listing in AndroidKeystoreSecureArea; keys are deleted per-alias from wallet records")
    }

    companion object {
        /** Partition the SecureArea metadata lives under in the multipaz storage. */
        const val METADATA_PARTITION = "device_keys"

        /**
         * Opens the multipaz AndroidKeystoreSecureArea on a private SQLite database. Suspend:
         * the metadata table is created lazily on first access. AndroidStorage hands the path
         * straight to SQLiteDatabase.openOrCreateDatabase, so it must be absolute - a bare file
         * name resolves against the process working directory, not the app's data dir.
         */
        suspend fun create(
            context: Context,
            selection: SecureAreaSelection,
            attestationChallengeSource: AttestationChallengeSource
        ): SecureAreaKeyManager {
            val storage = AndroidStorage(
                databasePath = java.io.File(context.noBackupFilesDir, "secure_area.db").absolutePath,
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

/** The keys of one [SecureAreaKeyManager.batchCreateKey] call, in creation order. */
data class SecureAreaKeyBatch(val keys: List<SecureAreaDeviceKey>)

/**
 * The public half as a Nimbus ECKey for the wallet provider's attestation flow. Only public
 * material crosses this boundary; there is no private counterpart to export (EE-SEC-003).
 */
fun SecureAreaDeviceKey.jwk(): com.nimbusds.jose.jwk.ECKey {
    val coordinate = publicKey as EcPublicKeyDoubleCoordinate
    val x5c = attestationChain.certificates.map {
        com.nimbusds.jose.util.Base64.encode(it.encoded.toByteArray())
    }
    return com.nimbusds.jose.jwk.ECKey.Builder(
        com.nimbusds.jose.jwk.Curve.P_256,
        com.nimbusds.jose.util.Base64URL.encode(coordinate.x),
        com.nimbusds.jose.util.Base64URL.encode(coordinate.y)
    ).x509CertChain(x5c).build()
}

/**
 * The seam [ee.cyber.wallet.data.repository.AccountRepository] depends on for SecureArea key
 * cleanup. An interface rather than [SecureAreaKeyManager] directly because Android Keystore
 * cannot be exercised from the JVM unit tests: the repository test proves deleteAllData runs the
 * per-alias SecureArea deletion (review finding 5) by injecting a recording fake here, while the
 * AndroidKeystoreSecureArea interaction itself stays PENDING-DEVICE
 * (docs/planning/STEP5-RECORD.md).
 */
interface SecureAreaKeyDeleter {
    /** Deletes one SecureArea key alias. Must not throw on an already-deleted/unknown alias. */
    suspend fun deleteKey(keyId: String)

    /** Best-effort bulk sweep for aliases no longer reachable from wallet records. */
    suspend fun deleteAllKeys()
}

/**
 * Maps the wallet's keyAttestation records onto per-alias SecureArea deletions, then wipes the
 * records. The EC rows are the alias registry — [ee.cyber.wallet.crypto.LocalCryptoProvider]
 * inserts one row per generated SecureArea key, under the key's own id — so deleting every EC
 * row's key deletes every SecureArea device key the wallet holds. RSA rows name BKS aliases
 * handled by the BKS keystore wipe and are not SecureArea keys.
 *
 * Ordering is the load-bearing property and is JVM-tested: per-alias deletions and the best-effort
 * sweep run BEFORE [KeyAttestationDao.deleteAll] wipes the registry — past the wipe the aliases
 * would be unreachable from wallet records while the Android Keystore keys lived on (review
 * finding 5). [ee.cyber.wallet.data.repository.AccountRepository.deleteAllData] must call this
 * before `walletDatabase.clearAllTables()`, which drops the same table.
 */
class SecureAreaKeyCleanup(
    private val keyAttestationDao: KeyAttestationDao,
    private val secureAreaKeyDeleter: SecureAreaKeyDeleter
) {

    suspend fun deleteAll() {
        val ecRows = keyAttestationDao.getByType(KeyType.EC.name)
        ecRows.forEach { row ->
            secureAreaKeyDeleter.deleteKey(row.id)
        }
        secureAreaKeyDeleter.deleteAllKeys()
        keyAttestationDao.deleteAll()
    }
}
