package ee.cyber.wallet.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.engagement.BleRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.readerauth.ReaderTrustStore
import eu.europa.ec.eudi.wallet.document.DocumentManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ProximityModule {

    @Singleton
    @Provides
    fun providesReaderTrustStore(): ReaderTrustStore {
        return ReaderTrustStore.getDefault(
            trustedCertificates = listOf()
        )
    }

    // D11 (JVM-H5): kept as a provider — TransferManager (below) consumes it.
    // SoftwareSecureArea.create over EphemeralStorage is the plan's own
    // "cheap" case (in-memory, milliseconds); the SecureAreaKeyManager
    // deferred-facade redesign is the part deferred to interface extraction.
    @Singleton
    @Provides
    fun providesDocumentManager(): DocumentManager {
        val storage = org.multipaz.storage.ephemeral.EphemeralStorage()
        val secureArea = kotlinx.coroutines.runBlocking {
            org.multipaz.securearea.software.SoftwareSecureArea.create(storage)
        }
        val secureAreaRepository = org.multipaz.securearea.SecureAreaRepository.Builder().apply {
            add(secureArea)
        }.build()
        return DocumentManager.Builder()
            .setIdentifier("eudi_wallet_document_manager")
            .setStorage(storage)
            .setSecureAreaRepository(secureAreaRepository)
            .build()
    }

    @Singleton
    @Provides
    fun providesTransferManager(
        @ApplicationContext context: Context,
        documentManager: DocumentManager,
        readerTrustStore: ReaderTrustStore
    ): TransferManager {
        val transferManager = TransferManager.getDefault(
            context = context,
            documentManager = documentManager,
            retrievalMethods = listOf(
                BleRetrievalMethod(
                    peripheralServerMode = true,
                    centralClientMode = true,
                    clearBleCache = true
                )
            ),
            readerTrustStore = readerTrustStore
        )
        return transferManager
    }
}
