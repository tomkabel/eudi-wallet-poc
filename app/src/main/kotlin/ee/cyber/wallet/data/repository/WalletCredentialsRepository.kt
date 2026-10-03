package ee.cyber.wallet.data.repository

import android.os.Build
import ee.cyber.wallet.data.datastore.WalletInstanceCredentialsDataSource
import ee.cyber.wallet.domain.provider.wallet.DeviceData
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService

class WalletCredentialsRepository(
    private val walletInstanceCredentialsDataSource: WalletInstanceCredentialsDataSource,
    private val walletProviderService: WalletProviderService
) {

    val credentials = walletInstanceCredentialsDataSource.credentials

    suspend fun registerInstance() = walletProviderService.registerWalletInstance(DeviceData(Build.MODEL)).also {
        // A registered instance whose credentials were not persisted is unusable: fail loudly.
        walletInstanceCredentialsDataSource.updateCredentials(it.instanceId, it.instancePassword).getOrThrow()
    }
}
