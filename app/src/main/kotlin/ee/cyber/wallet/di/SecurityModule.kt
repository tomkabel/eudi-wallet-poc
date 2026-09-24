package ee.cyber.wallet.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.crypto.LocalCryptoProvider
import ee.cyber.wallet.crypto.RemoteCryptoProvider
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.data.datastore.WalletInstanceCredentialsDataSource
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.security.AndroidEncryptionManager
import ee.cyber.wallet.security.AttestationChallengeSource
import ee.cyber.wallet.security.DeviceSecureAreaSelection
import ee.cyber.wallet.security.EncryptedKeyStoreManager
import ee.cyber.wallet.security.MockAttestationChallengeSource
import ee.cyber.wallet.security.SecureAreaKeyManager
import ee.cyber.wallet.security.SecureAreaSelection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Singleton
    @Provides
    fun providesEncryptedKeyStore(
        @ApplicationContext context: Context
    ) = EncryptedKeyStoreManager(context)

    @Singleton
    @Provides
    fun providesAndroidEncryptionManager(
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher
    ) = AndroidEncryptionManager(dispatcher)

    /**
     * The StrongBox/TEE selection for device keys. Step 5 of the conformance plan: StrongBox
     * where the device has FEATURE_STRONGBOX_KEYSTORE, TEE otherwise.
     */
    @Singleton
    @Provides
    fun providesSecureAreaSelection(
        @ApplicationContext context: Context
    ): SecureAreaSelection = DeviceSecureAreaSelection(
        hasStrongBoxFeature = context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)
    )

    /** The attestation challenge the (mock) wallet provider hands out per generated key. */
    @Singleton
    @Provides
    fun providesAttestationChallengeSource(): AttestationChallengeSource = MockAttestationChallengeSource()

    /**
     * Initialises the multipaz AndroidKeystoreSecureArea and the key manager over it. The
     * SecureArea's SQLite metadata store is opened lazily on first use (suspend provider).
     */
    @Singleton
    @Provides
    fun providesSecureAreaKeyManager(
        @ApplicationContext context: Context,
        selection: SecureAreaSelection,
        attestationChallengeSource: AttestationChallengeSource
    ): SecureAreaKeyManager = runBlocking {
        SecureAreaKeyManager.create(
            context = context,
            selection = selection,
            attestationChallengeSource = attestationChallengeSource
        )
    }

    @Singleton
    @Provides
    fun providesRemoteCryptoProvider(
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher,
        keyAttestationDao: KeyAttestationDao,
        walletProviderService: WalletProviderService,
        walletInstanceCredentialsDataSource: WalletInstanceCredentialsDataSource
    ): RemoteCryptoProvider = RemoteCryptoProvider(
        dispatcher = dispatcher,
        keyAttestationDao = keyAttestationDao,
        walletProviderService = walletProviderService,
        walletInstanceCredentialsDataSource = walletInstanceCredentialsDataSource
    )

    @Singleton
    @Provides
    fun providesLocalCryptoProvider(
        encryptedKeyStoreManager: EncryptedKeyStoreManager,
        keyAttestationDao: KeyAttestationDao,
        walletProviderService: WalletProviderService,
        secureAreaKeyManager: SecureAreaKeyManager,
        walletInstanceCredentialsDataSource: WalletInstanceCredentialsDataSource
    ): LocalCryptoProvider = LocalCryptoProvider(
        keyAttestationDao = keyAttestationDao,
        keyStoreManager = encryptedKeyStoreManager,
        walletProviderService = walletProviderService,
        secureAreaKeyManager = secureAreaKeyManager,
        walletInstanceCredentialsDataSource = walletInstanceCredentialsDataSource
    )

    @Singleton
    @Provides
    fun providesCryptoProviderFactory(
        remoteCryptoProvider: RemoteCryptoProvider,
        localCryptoProvider: LocalCryptoProvider
    ): CryptoProvider.Factory = CryptoProvider.Factory(listOf(remoteCryptoProvider, localCryptoProvider))
}
