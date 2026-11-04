package ee.cyber.wallet.di

import android.content.Context
import ee.cyber.wallet.AppConfig
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import ee.cyber.wallet.domain.credentials.RpcCredentialIssuanceService
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.MockIssuance
import ee.cyber.wallet.domain.provider.ageverification.BatchAgeIssuer
import ee.cyber.wallet.domain.provider.pid.PidProviderService
import ee.cyber.wallet.domain.provider.pid.PidProviderServiceRpc
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.domain.provider.wallet.WalletProviderServiceRpc
import ee.cyber.wallet.security.EncryptedKeyStoreManager
import kotlinx.coroutines.CoroutineDispatcher

/**
 * release / debug / local bindings: the real RPC services. The local_mocks build type compiles
 * `src/local_mocks/kotlin/.../VariantServices.kt` instead; mock classes live only there (critique C11).
 */
@Suppress("UNUSED_PARAMETER")
internal object VariantServices {

    fun walletProviderService(
        keyStoreManager: EncryptedKeyStoreManager,
        dispatcher: CoroutineDispatcher
    ): WalletProviderService = WalletProviderServiceRpc(AppConfig.walletProviderRpcUrl, dispatcher)

    fun pidProviderService(
        context: Context,
        credentialIssuanceService: CredentialIssuanceService,
        dispatcher: CoroutineDispatcher
    ): PidProviderService = PidProviderServiceRpc(AppConfig.pidProviderUrl, dispatcher)

    fun credentialIssuanceService(
        context: Context,
        dispatcher: CoroutineDispatcher,
        userPreferencesDataSource: UserPreferencesDataSource,
        walletProviderService: WalletProviderService
    ): CredentialIssuanceService = RpcCredentialIssuanceService(AppConfig.walletProviderRpcUrl, dispatcher)

    fun mockIssuance(
        context: Context,
        cryptoProviderFactory: CryptoProvider.Factory,
        credentialIssuanceService: CredentialIssuanceService
    ): MockIssuance = object : MockIssuance {
        override suspend fun issueMdl(): Attestation = error("mock issuance exists only in the local_mocks build")
        override suspend fun issueAgeVerification(batchIssuer: BatchAgeIssuer): List<Attestation> =
            error("mock issuance exists only in the local_mocks build")
    }
}
