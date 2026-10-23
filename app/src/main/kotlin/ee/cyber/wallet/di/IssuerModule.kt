package ee.cyber.wallet.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.cyber.wallet.AppConfig
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.data.datastore.WalletInstanceCredentialsDataSource
import ee.cyber.wallet.data.repository.WalletCredentialsRepository
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import ee.cyber.wallet.domain.credentials.CredentialIssuanceServiceMock
import ee.cyber.wallet.domain.credentials.RpcCredentialIssuanceService
import ee.cyber.wallet.domain.documents.CredentialToDocumentMapper
import ee.cyber.wallet.domain.provider.IssuePidUseCase
import ee.cyber.wallet.domain.provider.ageverification.AgeVerificationProviderServiceMock
import ee.cyber.wallet.domain.provider.ageverification.BatchAgeIssuer
import ee.cyber.wallet.domain.provider.ageverification.WalletProviderBatchAgeIssuer
import ee.cyber.wallet.domain.provider.mdl.MdlProviderServiceMock
import ee.cyber.wallet.domain.provider.pid.PidProviderService
import ee.cyber.wallet.domain.provider.wallet.AvsuaVerifier
import ee.cyber.wallet.domain.provider.wallet.WalletProviderServiceMock
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.security.SecureAreaKeyManager
import ee.cyber.wallet.security.SecureAreaBatchKeyManager
import ee.cyber.wallet.domain.provider.wallet.WalletInstanceCredentials
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class IssuerModule {

    @Singleton
    @Provides
    fun providesCredentialIssuanceService(
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher,
        @ApplicationContext context: Context,
        userPreferencesDataSource: UserPreferencesDataSource,
        walletProviderService: WalletProviderService
    ): CredentialIssuanceService {
        return if (AppConfig.useMocks) {
            CredentialIssuanceServiceMock(
                context, dispatcher, userPreferencesDataSource,
                // Explicit local demo trust: never infer trust from AVSUA token headers.
                AvsuaVerifier({ (walletProviderService as? WalletProviderServiceMock)?.avsuaTrustKey() })
            )
        } else {
            RpcCredentialIssuanceService(AppConfig.walletProviderRpcUrl, dispatcher)
        }
    }

    @Singleton
    @Provides
    fun providesMdlProviderServiceMock(
        @ApplicationContext
        context: Context,
        cryptoProviderFactory: CryptoProvider.Factory,
        credentialIssuanceService: CredentialIssuanceService
    ): MdlProviderServiceMock {
        return MdlProviderServiceMock(context, cryptoProviderFactory, credentialIssuanceService)
    }

    @Singleton
    @Provides
    fun providesAgeVerificationProviderServiceMock(
        @ApplicationContext
        context: Context
    ): AgeVerificationProviderServiceMock {
        return AgeVerificationProviderServiceMock(context)
    }

    /**
     * The EE-POA-003 batch transaction's key/mint plumbing: SecureArea batch keys attested by
     * the (mock) wallet provider, minted through the mock issuance service. PENDING-DEVICE: the
     * Android Keystore interaction is exercised on hardware, not on the JVM.
     */
    @Singleton
    @Provides
    fun providesWalletProviderBatchAgeIssuer(
        secureAreaKeyManager: SecureAreaKeyManager,
        walletProviderService: WalletProviderService,
        keyAttestationDao: ee.cyber.wallet.data.database.dao.KeyAttestationDao,
        walletInstanceCredentialsDataSource: WalletInstanceCredentialsDataSource,
        credentialIssuanceService: CredentialIssuanceService,
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher
    ): BatchAgeIssuer {
        return createWalletProviderBatchAgeIssuer(
            secureAreaKeyManager, walletProviderService, keyAttestationDao,
            { walletInstanceCredentialsDataSource.credentials.first() }, credentialIssuanceService, dispatcher
        )
    }

    /** Shared factory wiring; accepts the public-key storage interface used by the batch. */
    internal fun createWalletProviderBatchAgeIssuer(
        secureAreaKeyManager: SecureAreaBatchKeyManager,
        walletProviderService: WalletProviderService,
        keyAttestationDao: ee.cyber.wallet.data.database.dao.KeyAttestationDao,
        credentials: suspend () -> WalletInstanceCredentials,
        credentialIssuanceService: CredentialIssuanceService,
        dispatcher: CoroutineDispatcher
    ): BatchAgeIssuer {
        return WalletProviderBatchAgeIssuer(
            secureAreaKeyManager = secureAreaKeyManager,
            walletProviderService = walletProviderService,
            keyAttestationDao = keyAttestationDao,
            credentials = credentials,
            issueCredential = { credential, keyAttestation, avIssuance ->
                credentialIssuanceService.issueCredential(credential, keyAttestation, avIssuance)
            },
            createAvIssuanceContext = { credentialIssuanceService.createAvIssuanceContext(it) },
            dispatcher = dispatcher
        )
    }

    @Singleton
    @Provides
    fun providesIssuePidUseCase(
        cryptoProviderFactory: CryptoProvider.Factory,
        walletProviderService: WalletProviderService,
        pidProviderService: PidProviderService,
        walletCredentialsRepository: WalletCredentialsRepository,
        documentMapper: CredentialToDocumentMapper,
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher
    ) = IssuePidUseCase(
        cryptoProviderFactory = cryptoProviderFactory,
        walletProviderService = walletProviderService,
        pidProviderService = pidProviderService,
        walletCredentialsRepository = walletCredentialsRepository,
        documentMapper = documentMapper,
        dispatcher = dispatcher
    )
}
