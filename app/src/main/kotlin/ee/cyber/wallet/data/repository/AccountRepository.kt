package ee.cyber.wallet.data.repository

import ee.cyber.wallet.crypto.LocalCryptoProvider
import ee.cyber.wallet.crypto.RemoteCryptoProvider
import ee.cyber.wallet.data.database.WalletDatabase
import ee.cyber.wallet.data.database.dao.AttestationDao
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.data.datastore.AuthorizationStateDataSource
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.data.datastore.UserSessionDataSource
import ee.cyber.wallet.security.EncryptedKeyStoreManager
import ee.cyber.wallet.security.SecureAreaKeyCleanup
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AccountRepository(
    private val dispatcher: CoroutineDispatcher,
    private val walletDatabase: WalletDatabase,
    private val remoteKeyManager: RemoteCryptoProvider,
    private val localKeyManager: LocalCryptoProvider,
    private val encryptedKeyStoreManager: EncryptedKeyStoreManager,
    private val attestationDao: AttestationDao,
    private val keyAttestationDao: KeyAttestationDao,
    private val secureAreaKeyCleanup: SecureAreaKeyCleanup,
    private val authorizationStateDataSource: AuthorizationStateDataSource,
    private val userSessionDataSource: UserSessionDataSource,
    private val userPreferencesDataSource: UserPreferencesDataSource
) {
    suspend fun deleteAllData() {
        withContext(dispatcher) {
            // The SecureArea keys must die BEFORE the records that name their aliases: the EC
            // keyAttestation rows are the alias registry, and past this point (and past
            // clearAllTables(), which drops the same table) the aliases would be unreachable
            // from wallet records while the Android Keystore keys lived on (review finding 5).
            // SecureAreaKeyManager.deleteKey is best-effort (it swallows failures), so one broken
            // alias cannot abort the whole wipe.
            val survivingKeys = runCatching { secureAreaKeyCleanup.deleteAll() }.getOrDefault(emptyList())

            runCatching { walletDatabase.clearAllTables() }
            runCatching { encryptedKeyStoreManager.clearAll() }

            remoteKeyManager.clearAll()
            localKeyManager.clearAll()
            attestationDao.deleteAll()
            keyAttestationDao.deleteAll()
            authorizationStateDataSource.clearAll()
            userSessionDataSource.clearAll()
            userPreferencesDataSource.clearAll()
            // Keys the platform refused to delete stay named, so the next wipe retries them.
            survivingKeys.forEach { runCatching { keyAttestationDao.insert(it) } }
        }
    }
}
