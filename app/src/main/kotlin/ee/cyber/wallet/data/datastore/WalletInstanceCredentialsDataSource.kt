package ee.cyber.wallet.data.datastore

import androidx.datastore.core.DataStore
import ee.cyber.wallet.WalletInstanceCredentialsProto
import ee.cyber.wallet.copy
import ee.cyber.wallet.domain.provider.wallet.WalletInstanceCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class WalletInstanceCredentialsDataSource(private val dataStore: DataStore<WalletInstanceCredentialsProto>) {

    val credentials = dataStore.data
        .map { WalletInstanceCredentials(it.instanceId, it.instancePassword) }
        .distinctUntilChanged()

    // CancellationException rethrown: runCatching around updateData must not
    // swallow coroutine cancellation (JVM-M1).
    suspend fun updateCredentials(instanceId: String, instancePassword: String) =
        runCatching {
            dataStore.updateData {
                it.copy {
                    this.instanceId = instanceId
                    this.instancePassword = instancePassword
                }
            }
        }.onFailure { if (it is CancellationException) throw it }

    suspend fun clearAll() = runCatching {
        dataStore.updateData { WalletInstanceCredentialsProto.getDefaultInstance() }
    }.onFailure { if (it is CancellationException) throw it }
}
