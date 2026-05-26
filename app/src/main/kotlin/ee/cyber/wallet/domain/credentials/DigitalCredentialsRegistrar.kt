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
        log.info("Registering credentials to Credentials Manager")
        val documents = documentRepository.documents.first()
        if (!documents.isEmpty()) {
            val matchingDocuments = documents.filterIsInstance<CredentialDocument.MDocDocument>()
            val client = IdentityCredentialManager.getClient(context)
            val matcher = context.getMatcher()
            val credentials = matchingDocuments.toCBORBytes()


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

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    /**
     * Converts a list of [CredentialDocument.MDocDocument] to a CBOR byte array in structure, that
     * identitycredentialmatcher.wasm requires.
     */
    private fun List<CredentialDocument.MDocDocument>.toCBORBytes(): ByteArray {
        val docsBuilder = CBORObject.NewArray()
        forEach { document ->
            docsBuilder.Add(CBORObject.NewMap().apply {
                Add("title", "Title")
                Add("subtitle", "Subtitle")
                Add("bitmap", byteArrayOf(0))
                Add("mdoc", CBORObject.NewMap().apply {
                    Add("id", document.id)
                    Add("docType", document.type.uri)
                    Add("namespaces", CBORObject.NewMap().apply {
                        document.fields.groupBy { it.namespace }
                            .forEach { (nameSpace, elements) ->
                                val namespaceBuilder = CBORObject.NewMap()
                                elements.forEach { element ->
                                    val elementBuilder = CBORObject.NewArray().apply {
                                        Add(element.name)
                                        Add(element.value)
                                    }
                                    namespaceBuilder.Add(element.name, elementBuilder)
                                }
                                Add(nameSpace.uri, namespaceBuilder)
                            }
                    })
                })
            })
        }
        val credentialBytes = docsBuilder.EncodeToBytes()
        return credentialBytes
    }

    private fun Context.getMatcher(): ByteArray {
        return this.assets.open("dcapi/identitycredentialmatcher.wasm").use { stream ->
            ByteArray(stream.available()).apply {
                stream.read(this)
            }
        }
    }
}