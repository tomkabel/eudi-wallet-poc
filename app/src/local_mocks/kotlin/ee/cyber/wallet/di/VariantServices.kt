package ee.cyber.wallet.di

import android.content.Context
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import ee.cyber.wallet.domain.credentials.CredentialIssuanceServiceMock
import ee.cyber.wallet.domain.provider.MockIssuance
import ee.cyber.wallet.domain.provider.ageverification.AgeVerificationProviderServiceMock
import ee.cyber.wallet.domain.provider.ageverification.BatchAgeIssuer
import ee.cyber.wallet.domain.provider.mdl.MdlProviderServiceMock
import ee.cyber.wallet.domain.provider.pid.PidProviderService
import ee.cyber.wallet.domain.provider.pid.PidProviderServiceMock
import ee.cyber.wallet.domain.provider.wallet.AvsuaVerifier
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.domain.provider.wallet.WalletProviderServiceMock
import ee.cyber.wallet.security.EncryptedKeyStoreManager
import kotlinx.coroutines.CoroutineDispatcher

/**
 * local_mocks bindings: the in-app mock services. The other build types compile
 * `src/real/kotlin/.../VariantServices.kt` instead, so no mock class reaches them (critique C11).
 */
internal object VariantServices {

    fun walletProviderService(
        keyStoreManager: EncryptedKeyStoreManager,
        dispatcher: CoroutineDispatcher
    ): WalletProviderService = WalletProviderServiceMock(keyStoreManager, dispatcher)

    fun pidProviderService(
        context: Context,
        credentialIssuanceService: CredentialIssuanceService,
        @Suppress("UNUSED_PARAMETER") dispatcher: CoroutineDispatcher
    ): PidProviderService = PidProviderServiceMock(context, credentialIssuanceService)

    fun credentialIssuanceService(
        context: Context,
        dispatcher: CoroutineDispatcher,
        userPreferencesDataSource: UserPreferencesDataSource,
        walletProviderService: WalletProviderService
    ): CredentialIssuanceService = CredentialIssuanceServiceMock(
        context, dispatcher, userPreferencesDataSource,
        // Explicit local demo trust: never infer trust from AVSUA token headers.
        AvsuaVerifier({ (walletProviderService as? WalletProviderServiceMock)?.avsuaTrustKey() })
    )

    fun mockIssuance(
        context: Context,
        cryptoProviderFactory: CryptoProvider.Factory,
        credentialIssuanceService: CredentialIssuanceService
    ): MockIssuance {
        val mdl = MdlProviderServiceMock(context, cryptoProviderFactory, credentialIssuanceService)
        val ageVerification = AgeVerificationProviderServiceMock(context)
        return object : MockIssuance {
            override suspend fun issueMdl() = mdl.issueMdl()
            override suspend fun issueAgeVerification(batchIssuer: BatchAgeIssuer) =
                ageVerification.issueEePoaBatch(batchIssuer)
        }
    }
}
