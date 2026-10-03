package ee.cyber.wallet.data.datastore

import androidx.datastore.core.DataStore
import com.google.protobuf.ByteString
import ee.cyber.wallet.UserSessionProto
import ee.cyber.wallet.copy
import ee.cyber.wallet.security.PinRecord
import ee.cyber.wallet.security.PinVerifier
import ee.cyber.wallet.ui.model.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserSessionDataSource(
    private val dataStore: DataStore<UserSessionProto>,
    private val pinVerifier: PinVerifier
) {

    val userSession = dataStore.data
        .map { UserSession(isPinCreated = !it.pinHash.isEmpty || it.pin.isNotEmpty()) }
        .distinctUntilChanged()

    // CancellationException rethrown: runCatching around updateData must not
    // swallow coroutine cancellation (JVM-M1).
    suspend fun setPin(pin: CharArray) =
        runCatching {
            val record = pinVerifier.create(pin)
            dataStore.updateData { it.withRecord(record) }
        }.onFailure { if (it is CancellationException) throw it }

    /**
     * Verifies [pin] and persists the updated lockout counters in the same write, so the attempt
     * is on disk before the caller sees the answer. Without a stored PIN nothing matches.
     */
    suspend fun verifyPin(pin: CharArray): Boolean {
        var match = false
        dataStore.updateData { proto ->
            val record = proto.toRecord() ?: return@updateData proto
            val (ok, updated) = pinVerifier.verify(pin, record)
            match = ok
            proto.withRecord(updated)
        }
        return match
    }

    suspend fun clearAll() = runCatching {
        dataStore.updateData { UserSessionProto.getDefaultInstance() }
    }.onFailure { if (it is CancellationException) throw it }

    private fun UserSessionProto.toRecord(): PinRecord? = when {
        !pinHash.isEmpty -> PinRecord(pinHash.toByteArray(), pinSalt.toByteArray(), failedAttempts, lockedUntil)
        // An older build stored the PIN in plaintext: hash it now; withRecord() clears it.
        pin.isNotEmpty() -> pinVerifier.create(pin.toCharArray())
        else -> null
    }

    private fun UserSessionProto.withRecord(record: PinRecord) = copy {
        pin = ""
        pinHash = ByteString.copyFrom(record.hash)
        pinSalt = ByteString.copyFrom(record.salt)
        failedAttempts = record.failedAttempts
        lockedUntil = record.lockedUntil
    }
}
