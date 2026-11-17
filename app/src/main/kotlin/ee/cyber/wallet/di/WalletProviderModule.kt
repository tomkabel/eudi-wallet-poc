package ee.cyber.wallet.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import ee.cyber.wallet.domain.provider.pid.PidProviderService
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.security.EncryptedKeyStoreManager
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class WalletProviderModule {

    @Singleton
    @Provides
    fun providesWalletProviderService(
        keyStoreManager: EncryptedKeyStoreManager,
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher
    ): WalletProviderService = VariantServices.walletProviderService(keyStoreManager, dispatcher)

    @Singleton
    @Provides
    fun providesPidProviderService(
        @ApplicationContext
        context: Context,
        credentialIssuanceService: CredentialIssuanceService,
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher
    ): PidProviderService = VariantServices.pidProviderService(context, credentialIssuanceService, dispatcher)
}
