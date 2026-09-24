package ee.cyber.wallet.domain.credentials

import android.content.Context
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import com.google.android.gms.identitycredentials.ClearRegistryRequest
import com.google.android.gms.identitycredentials.IdentityCredentialManager
import com.google.android.gms.identitycredentials.RegistrationRequest
import com.upokecenter.cbor.CBORObject
import dagger.hilt.android.qualifiers.ApplicationContext
import ee.cyber.wallet.data.repository.DocumentRepository
import ee.cyber.wallet.domain.documents.CredentialDocument
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DigitalCredentialsRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val documentRepository: DocumentRepository
) {
    private val log = LoggerFactory.getLogger(DigitalCredentialsRegistrar::class.java)

    /**
     * Initialize DCA credentials asynchronously.
     * This should be called early in the application lifecycle.
     *
     * @param scope CoroutineScope to use for the async operation
     */
    fun initialize(scope: CoroutineScope) {
        scope.launch {
            registerCredentials()
        }
    }

    @OptIn(ExperimentalDigitalCredentialApi::class)
    public suspend fun registerCredentials() {
        // ARF OIA_08f: the global user setting gates disclosure to the DC API framework. With the
        // switch off nothing is registered (and any earlier registration is cleared), so the
        // platform never learns which attestations this wallet holds. Per-attestation selection
        // after a disable is OIA_08f's SHOULD and is not built in this PoC.
        if (!documentRepository.isDcApiDisclosureEnabled.first()) {
            log.info("DC API disclosure disabled by user setting (OIA_08f); clearing the registry")
            clearRegistry()
            return
        }

        log.info("Registering credentials to Credentials Manager")
        val documents = documentRepository.documents.first()
        if (!documents.isEmpty()) {
            val matchingDocuments = documents.filterIsInstance<CredentialDocument.MDocDocument>()
            val client = IdentityCredentialManager.getClient(context)
            val matcher = context.getMatcher()

            // ARF OIA_08e: the platform learns the presence of stored attestations — their
            // docType — but never attribute names or values. Claim-level matching happens in the
            // wallet after launch, so the wallet may surface in the picker for requests it cannot
            // fully answer; the ARF note accepts that.
            val credentials = matchingDocuments.toRegistryDocTypes().toCBORBytes()

            client.clearRegistry(ClearRegistryRequest(deleteAll = true, clearTypedRegistryOption = null))
            client.registerCredentials(
                RegistrationRequest(
                    credentials = credentials,
                    matcher = matcher,
                    type = "com.credman.IdentityCredential",
                    requestType = "",
                    protocolTypes = emptyList(),
                )
            ).addOnSuccessListener {
                log.info("IdentityCredential registration succeeded")
            }.addOnFailureListener {
                log.info("IdentityCredential registration failed$it")
            }

            client.registerCredentials(
                RegistrationRequest(
                    credentials = credentials,
                    matcher = matcher,
                    type = DigitalCredential.TYPE_DIGITAL_CREDENTIAL,
                    requestType = "",
                    protocolTypes = emptyList(),
                )
            ).addOnSuccessListener {
                log.info("Digital Credential registration succeeded")
            }.addOnFailureListener {
                log.info("Digital Credential  registration failed $it")
            }
        }
    }

    private suspend fun clearRegistry() {
        IdentityCredentialManager.getClient(context).clearRegistry(
            ClearRegistryRequest(deleteAll = true, clearTypedRegistryOption = null)
        )
    }

    /**
     * ARF OIA_08e: the registry payload carries each document's id and docType — nothing else.
     * Namespace maps, element names and values stay inside the wallet.
     */
    fun List<CredentialDocument.MDocDocument>.toRegistryDocTypes(): List<RegistryDocType> =
        map { RegistryDocType(id = it.id, docType = it.type.uri) }

    /**
     * Converts the [RegistryDocType] list to a CBOR byte array in the structure
     * identitycredentialmatcher.wasm requires: an array of `{title, subtitle, bitmap, mdoc:
     * {id, docType}}` maps, with the mdoc entry holding NO `namespaces` member.
     */
    private fun List<RegistryDocType>.toCBORBytes(): ByteArray {
        val docsBuilder = CBORObject.NewArray()
        forEach { registryEntry ->
            docsBuilder.Add(CBORObject.NewMap().apply {
                Add("title", "Title")
                Add("subtitle", "Subtitle")
                Add("bitmap", byteArrayOf(0))
                Add("mdoc", CBORObject.NewMap().apply {
                    Add("id", registryEntry.id)
                    Add("docType", registryEntry.docType)
                })
            })
        }
        return docsBuilder.EncodeToBytes()
    }

    private fun Context.getMatcher(): ByteArray {
        return this.assets.open("dcapi/identitycredentialmatcher.wasm").use { stream ->
            ByteArray(stream.available()).apply {
                stream.read(this)
            }
        }
    }
}
