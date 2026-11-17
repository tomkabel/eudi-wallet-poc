package ee.cyber.wallet.di

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import com.google.protobuf.InvalidProtocolBufferException
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.cyber.wallet.AuthorizationRequestStateProto
import ee.cyber.wallet.data.datastore.AuthorizationStateDataSource
import ee.cyber.wallet.security.AndroidEncryptionManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthorizationStateDataSourceModule {

    @Provides
    @Singleton
    fun providesAuthorizationStateDataStore(
        @ApplicationContext context: Context,
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher,
        @ApplicationScope scope: CoroutineScope,
        androidEncryptionManager: AndroidEncryptionManager
    ): DataStore<AuthorizationRequestStateProto> =
        DataStoreFactory.create(
            serializer = AuthorizationStateSerializer(androidEncryptionManager),
            // The state is a short-lived in-flight issuance; an unreadable blob (including a
            // plaintext one left by an older build) is dropped and the user restarts issuance.
            corruptionHandler = ReplaceFileCorruptionHandler { AuthorizationRequestStateProto.getDefaultInstance() },
            scope = CoroutineScope(scope.coroutineContext + dispatcher)
        ) {
            context.dataStoreFile("issuer_authorization.pb")
        }

    @Singleton
    @Provides
    fun providesAuthorizationStateDataSource(dataStore: DataStore<AuthorizationRequestStateProto>) = AuthorizationStateDataSource(dataStore)

    // Encrypted like the PIN and instance-password stores: the blob holds the PKCE verifier
    // and state of an in-flight authorization request.
    private class AuthorizationStateSerializer(
        private val encryptionManager: AndroidEncryptionManager
    ) : Serializer<AuthorizationRequestStateProto> {
        override val defaultValue: AuthorizationRequestStateProto = AuthorizationRequestStateProto.getDefaultInstance()

        override suspend fun readFrom(input: InputStream): AuthorizationRequestStateProto =
            try {
                AuthorizationRequestStateProto.parseFrom(encryptionManager.decrypt(KEY_ALIAS, input))
            } catch (exception: InvalidProtocolBufferException) {
                throw CorruptionException("Cannot read proto.", exception)
            } catch (exception: GeneralSecurityException) {
                throw CorruptionException("Cannot decrypt authorization state.", exception)
            } catch (exception: IllegalArgumentException) {
                throw CorruptionException("Malformed encrypted authorization state.", exception)
            }

        override suspend fun writeTo(t: AuthorizationRequestStateProto, output: OutputStream) {
            encryptionManager.encrypt(KEY_ALIAS, t.toByteArray(), output)
        }

        companion object {
            private const val KEY_ALIAS = "authorization-state-key"
        }
    }
}
