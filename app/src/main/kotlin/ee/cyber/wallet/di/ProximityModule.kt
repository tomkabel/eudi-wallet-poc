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
import kotlinx.coroutines.runBlocking
import org.multipaz.securearea.SecureAreaRepository
import org.multipaz.securearea.software.SoftwareSecureArea
import org.multipaz.storage.ephemeral.EphemeralStorage
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

    @Singleton
    @Provides
    fun providesDocumentManager(): DocumentManager {

        val storage = EphemeralStorage()
        val secureArea = runBlocking { SoftwareSecureArea.create(storage) }
        val secureAreaRepository = SecureAreaRepository.Builder().apply {
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
