package ee.cyber.wallet.ui.screens.dcapi

import android.content.Intent
import android.os.Parcelable
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetDigitalCredentialOption
import androidx.credentials.provider.PendingIntentHandler
import androidx.lifecycle.viewModelScope
import com.upokecenter.cbor.CBORObject
import com.upokecenter.cbor.CBORType
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.crypto.deviceCryptoProvider
import ee.cyber.wallet.data.repository.DocumentRepository
import ee.cyber.wallet.data.repository.TransactionLogRepository
import ee.cyber.wallet.di.Dispatcher
import ee.cyber.wallet.di.WalletDispatchers
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.documents.mdoc.MDocUtils.generateDCApiHandover
import ee.cyber.wallet.domain.presentation.CredentialClaim
import ee.cyber.wallet.domain.presentation.OpenId4VPManager
import ee.cyber.wallet.domain.presentation.PresentationTier
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.ui.mvi.MviViewModel
import ee.cyber.wallet.ui.mvi.ViewEvent
import ee.cyber.wallet.ui.mvi.ViewSideEffect
import ee.cyber.wallet.ui.mvi.ViewState
import ee.cyber.wallet.ui.screens.documents.credentialType
import ee.cyber.wallet.ui.screens.documents.docType
import ee.cyber.wallet.ui.screens.presentation.MatchedField
import ee.cyber.wallet.ui.screens.presentation.MatchedFields
import ee.cyber.wallet.ui.screens.presentation.fields
import ee.cyber.wallet.util.DeviceRequestParser
import ee.cyber.wallet.util.toPresentationDefinition
import eu.europa.ec.eudi.prex.FieldQueryResult
import eu.europa.ec.eudi.prex.Match
import eu.europa.ec.eudi.prex.PresentationDefinition
import id.walt.mdoc.dataelement.EncodedCBORElement
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.dataelement.NullElement
import id.walt.mdoc.dataretrieval.DeviceResponse
import id.walt.mdoc.doc.MDoc
import id.walt.mdoc.docrequest.MDocRequestBuilder
import id.walt.mdoc.mdocauth.DeviceAuthentication
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.json.JSONObject
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.Hpke
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.response.buildDeviceResponse
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import org.multipaz.util.fromBase64Url
import org.slf4j.LoggerFactory
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption
import kotlin.io.encoding.ExperimentalEncodingApi
import org.multipaz.mdoc.response.DeviceResponse as MdocDeviceResponse

@HiltViewModel
class DigitalCredentialsViewModel @Inject constructor(
    private val allowedAppsJson: String,
    private val documentRepository: DocumentRepository,
    private val cryptoProviderFactory: CryptoProvider.Factory,
    private val openId4VPManager: OpenId4VPManager,
    private val transactionLogRepository: TransactionLogRepository,
    @Dispatcher(WalletDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) : MviViewModel<DcEvent, DcUiState, DcEffect>() {

    private val logger = LoggerFactory.getLogger(DigitalCredentialsViewModel::class.java)

    // Null when this device cannot prove at all. Loading the prover pulls in libzkp.so, which is
    // only packaged for arm64-v8a and x86_64, so ask once here rather than discovering it as an
    // UnsatisfiedLinkError in the middle of building a response.
    // ponytail: holds all bundled circuits (~2 MB) once a ZK request arrives; load only the
    // requested one via addCircuit() if that footprint ever matters.
    private val zkSystem: LongfellowZkSystem? by lazy {
        runCatching { LongfellowZkSystem().apply { addDefaultCircuits() } }
            .onFailure { logger.warn("Longfellow prover unavailable on this device", it) }
            .getOrNull()
    }

    override fun initialState(): DcUiState = DcUiState()

    @OptIn(ExperimentalDigitalCredentialApi::class)
    fun processRequest(intent: Intent) {
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val providerRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
                if (providerRequest == null) {
                    logger.error("No credential request found")
                    sendEffect { DcEffect.Error("No credential request found") }
                    return@launch
                }

                val origin = providerRequest.callingAppInfo.getOrigin(allowedAppsJson)
                if (origin == null) {
                    logger.error("Could not determine origin")
                    sendEffect { DcEffect.Error("Could not determine origin") }
                    return@launch
                }

                val option = providerRequest.credentialOptions
                    .filterIsInstance<GetDigitalCredentialOption>()
                    .firstOrNull()
                if (option == null) {
                    logger.error("No digital credential option found")
                    sendEffect { DcEffect.Error("No digital credential option found") }
                    return@launch
                }

                val json = JSONObject(option.requestJson)
                val firstRequest = json.getJSONArray("requests").getJSONObject(0)
                val data = firstRequest["data"] as JSONObject
                val requestData = JSONObject(data.toString())
                val deviceRequestBase64 = requestData.getString("deviceRequest")
                val encryptionInfoBase64 = requestData.getString("encryptionInfo")

                val sessionTranscript = getSessionTranscript(encryptionInfoBase64, origin)
                val recipientPublicKey = getRecipientPublicKey(encryptionInfoBase64)

                val docRequests = DeviceRequestParser(
                    deviceRequestBase64.fromBase64Url(),
                    sessionTranscript.toCBOR()
                ).parse().docRequests

                val presentationDefinition = toPresentationDefinition(docRequests)

                val documents = documentRepository.documents.first()
                val documentMatches = presentationDefinition.getDocumentMatches(documents)

                handleMatchResult(
                    documentMatches,
                    origin,
                    sessionTranscript,
                    recipientPublicKey,
                    docRequests.flatMap { it.zkSystemSpecs }
                )
            } catch (e: Exception) {
                logger.error("Error processing request", e)
                sendEffect { DcEffect.Error(e.message ?: "Unknown error") }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun handleMatchResult(
        documentMatches: Pair<List<CredentialClaim>, Match>,
        origin: String,
        sessionTranscript: ListElement,
        recipientPublicKey: EcPublicKey,
        zkSystemSpecs: List<ZkSystemSpec>
    ) {
        when (val match = documentMatches.second) {
            is Match.NotMatched -> {
                logger.info("No matching credentials found")
                sendEffect { DcEffect.NoMatch }
            }

            is Match.Matched -> {
                val credentials = mutableListOf<DcCredential>()
                val claims = documentMatches.first

                match.matches.forEach { inputDescriptor ->
                    val candidateClaimId = inputDescriptor.value
                        .filter { (_, candidateClaim) ->
                            candidateClaim.matches.any { it.value is FieldQueryResult.CandidateField.Found }
                        }
                        .keys
                        .firstOrNull() ?: return@forEach

                    val document = claims.firstOrNull { it.uniqueId == candidateClaimId }
                        ?.credentialDocument as? CredentialDocument.MDocDocument ?: return@forEach

                    val fields = match.fields(candidateClaimId, false, document.type)
                    val optionalFields = match.fields(candidateClaimId, true, document.type)

                    if (fields.isNotEmpty() || optionalFields.isNotEmpty()) {
                        credentials.add(
                            DcCredential(
                                id = document.id,
                                credentialType = document.credentialType(),
                                fields = fields,
                                optionalFields = optionalFields,
                                attestation = document.attestation,
                                mDoc = document.mDoc
                            )
                        )
                    }
                }

                if (credentials.isEmpty()) {
                    sendEffect { DcEffect.NoMatch }
                } else {
                    setState {
                        copy(
                            verifier = origin,
                            credentials = credentials,
                            sessionTranscript = sessionTranscript,
                            recipientPublicKey = recipientPublicKey,
                            zkSystemSpecs = zkSystemSpecs
                        )
                    }
                }
            }
        }
    }

    private fun PresentationDefinition.getDocumentMatches(
        documents: List<CredentialDocument>
    ): Pair<List<CredentialClaim>, Match> = openId4VPManager.matches(this, documents)

    private fun getSessionTranscript(encryptionInfoBase64: String, origin: String): ListElement {
        return ListElement(
            listOf(
                NullElement(),
                NullElement(),
                generateDCApiHandover(encryptionInfoBase64, origin)
            )
        )
    }

    private fun getRecipientPublicKey(encryptionInfoBase64: String): EcPublicKey {
        val encryptionInfo = CBORObject.DecodeFromBytes(encryptionInfoBase64.fromBase64Url())
        if (encryptionInfo.type != CBORType.Array) {
            throw IllegalArgumentException("EncryptionInfo should be an array but was: ${encryptionInfo.type}")
        }
        return Cbor.decode(encryptionInfo[1]["recipientPublicKey"].EncodeToBytes()).asCoseKey.ecPublicKey
    }

    override suspend fun handleEvents(event: DcEvent) {
        when (event) {
            is DcEvent.OnOptionalFieldChange -> onOptionalFieldChange(event.field, event.checked)
            DcEvent.OnShareClicked -> onShareClicked()
            DcEvent.OnCancelClicked -> sendEffect { DcEffect.Cancel }
        }
    }

    private fun onOptionalFieldChange(field: MatchedField, checked: Boolean) {
        updateState {
            copy(
                credentials = credentials.map { credential ->
                    credential.copy(
                        fields = credential.fields.map {
                            if (field == it && it.checked != checked) {
                                it.copy(checked = checked)
                            } else {
                                it
                            }
                        },
                        optionalFields = credential.optionalFields.map {
                            if (field == it && it.checked != checked) {
                                it.copy(checked = checked)
                            } else {
                                it
                            }
                        }
                    )
                }
            )
        }
    }

    private fun updateState(reducer: DcUiState.() -> DcUiState) {
        setState {
            reducer().let { newState ->
                val disableShare = newState.credentials.all { credential ->
                    credential.fields.isEmpty() && credential.optionalFields.none { it.checked }
                }
                if (newState.shareDisabled != disableShare) {
                    newState.copy(shareDisabled = disableShare)
                } else {
                    newState
                }
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun onShareClicked() {
        val currentState = state.value
        val sessionTranscript = currentState.sessionTranscript ?: return
        val recipientPublicKey = currentState.recipientPublicKey ?: return

        setState { copy(isLoading = true) }

        try {
            val responseDocuments = mutableListOf<MDoc>()
            val zkDocuments = mutableListOf<ZkDocument>()

            currentState.credentials.forEach { credential ->
                val mDoc = credential.mDoc
                val checkedFields = credential.allCheckedFields.map { it.field }
                val docType = credential.credentialType.docType().uri

                val mDocRequest = MDocRequestBuilder(docType).apply {
                    checkedFields.forEach {
                        addDataElementRequest(it.namespace.uri, it.name, true)
                    }
                }.build(null)

                val cryptoProvider = cryptoProviderFactory.forKeyType(credential.attestation.keyAttestation.keyType)
                val keyId = credential.attestation.keyAttestation.keyId
                val deviceNameSpaces = EncodedCBORElement(MapElement(mapOf()))
                val deviceAuthentication = DeviceAuthentication(sessionTranscript, docType, deviceNameSpaces)

                val documentResponse = mDoc.presentWithDeviceSignature(
                    mDocRequest = mDocRequest,
                    deviceAuthentication = deviceAuthentication,
                    cryptoProvider = cryptoProvider.deviceCryptoProvider(keyId),
                    keyID = keyId
                )

                // Proving is seconds of blocking native work, and the first match also forces the
                // lazy circuit load. Both stay off the main thread or the share screen freezes
                // instead of showing its spinner.
                val zkDocument = withContext(defaultDispatcher) {
                    val system = zkSystem
                    val spec = matchZkSystemSpec(currentState.zkSystemSpecs, checkedFields.size)
                    if (system == null || spec == null) {
                        null
                    } else {
                        system.generateProof(
                            zkSystemSpec = spec,
                            document = MdocDocument.fromDataItem(Cbor.decode(documentResponse.toMapElement().toCBOR())),
                            sessionTranscript = Cbor.decode(sessionTranscript.toCBOR())
                        )
                    }
                }
                if (zkDocument == null) {
                    responseDocuments.add(documentResponse)
                } else {
                    zkDocuments.add(zkDocument)
                }

                // Named rather than inferred from the null above: "the verifier never asked" and
                // "this device cannot prove" are the same response but very different facts, and
                // EE-ZKP-053 wants the distinction on the record.
                val tier = when {
                    zkDocument != null -> PresentationTier.ZERO_KNOWLEDGE
                    currentState.zkSystemSpecs.isEmpty() -> PresentationTier.PLAIN_NOT_REQUESTED
                    zkSystem == null -> PresentationTier.PLAIN_DEVICE_INCAPABLE
                    else -> PresentationTier.PLAIN_NO_MATCHING_CIRCUIT
                }
                transactionLogRepository.addTransactionLog(
                    party = currentState.verifier,
                    docType = credential.credentialType.docType(),
                    tier = tier
                )
            }

            val deviceResponseBytes = if (zkDocuments.isEmpty()) {
                DeviceResponse(responseDocuments).toCBOR()
            } else {
                zkDeviceResponse(zkDocuments, responseDocuments, sessionTranscript)
            }
            val response = getEncryptedResponse(recipientPublicKey, deviceResponseBytes, sessionTranscript)

            logger.info("Response generated successfully")
            sendEffect { DcEffect.SendResponse(response) }
        } catch (e: Exception) {
            logger.error("Error generating response", e)
            sendEffect { DcEffect.Error(e.message ?: "Error generating response") }
        } finally {
            setState { copy(isLoading = false) }
        }
    }

    /**
     * Picks the strongest circuit we hold that the reader also allows, mirroring
     * [org.multipaz.mdoc.zkp.ZkSystem.getMatchingSystemSpec] without having to build multipaz
     * `RequestedClaim`s the rest of this screen has no use for.
     */
    private fun matchZkSystemSpec(requested: List<ZkSystemSpec>, numAttributes: Int): ZkSystemSpec? {
        val allowedCircuitHashes = requested.mapNotNull { it.getParam<String>("circuit_hash") }.toSet()
        return (zkSystem ?: return null).systemSpecs
            .filter {
                it.getParam<String>("circuit_hash") in allowedCircuitHashes &&
                    it.getParam<Long>("num_attributes") == numAttributes.toLong()
            }
            .maxByOrNull { it.getParam<Long>("version") ?: Long.MIN_VALUE }
    }

    /**
     * ISO/IEC 18013-5 2nd edition `DeviceResponse` carrying proofs in `zkDocuments`. Any document we
     * could not prove is carried as a plain `documents` entry, so selecting a ZK-capable credential
     * never silently drops the others.
     */
    private suspend fun zkDeviceResponse(
        zkDocuments: List<ZkDocument>,
        plainDocuments: List<MDoc>,
        sessionTranscript: ListElement
    ): ByteArray {
        val transcript = Cbor.decode(sessionTranscript.toCBOR())
        val mdocDocuments = plainDocuments.map {
            MdocDocument.fromDataItem(Cbor.decode(it.toMapElement().toCBOR()))
        }
        return Cbor.encode(
            buildDeviceResponse(transcript, MdocDeviceResponse.STATUS_OK) {
                mdocDocuments.forEach { addDocument(it) }
                zkDocuments.forEach { addZkDocument(it) }
            }.toDataItem()
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getEncryptedResponse(
        recipientPublicKey: EcPublicKey,
        deviceResponseBytes: ByteArray,
        sessionTranscript: ListElement
    ): String {
        // The session transcript belongs in HPKE `info`, not the AEAD `aad`: the Tink-backed
        // Crypto.hpkeEncrypt this replaces fed its `aad` argument to Tink as contextInfo, so
        // sending it as `aad` here would change the bytes on the wire.
        val encrypter = Hpke.getEncrypter(
            cipherSuite = Hpke.CipherSuite.DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM,
            receiverPublicKey = recipientPublicKey,
            info = sessionTranscript.toCBOR()
        )
        val cipherText = encrypter.encrypt(plaintext = deviceResponseBytes, aad = byteArrayOf())
        val enc = encrypter.encapsulatedKey.toByteArray()
        val encryptedResponse = CBORObject.NewArray().apply {
            Add("dcapi")
            Add(CBORObject.NewMap().apply {
                Add("enc", enc)
                Add("cipherText", cipherText)
            })
        }.EncodeToBytes()

        val responseJson = JSONObject()
        responseJson.put("response", Base64.UrlSafe.withPadding(PaddingOption.ABSENT).encode(encryptedResponse))
        return responseJson.toString()
    }
}

@Parcelize
data class DcCredential(
    val id: String,
    val credentialType: CredentialType = CredentialType.PID_SD_JWT,
    val fields: MatchedFields = listOf(),
    val optionalFields: MatchedFields = listOf(),
    val attestation: @RawValue Attestation,
    val mDoc: @RawValue MDoc
) : Parcelable {
    private val allFields: MatchedFields
        get() = fields + optionalFields

    val allCheckedFields: MatchedFields
        get() = allFields.filter { it.checked }
}

@Parcelize
data class DcUiState(
    val isLoading: Boolean = false,
    val verifier: String = "",
    val credentials: List<DcCredential> = listOf(),
    val shareDisabled: Boolean = false,
    val sessionTranscript: @RawValue ListElement? = null,
    val recipientPublicKey: @RawValue EcPublicKey? = null,
    val zkSystemSpecs: @RawValue List<ZkSystemSpec> = listOf()
) : ViewState, Parcelable

sealed class DcEffect : ViewSideEffect {
    data class SendResponse(val response: String) : DcEffect()
    data object Cancel : DcEffect()
    data object NoMatch : DcEffect()
    data class Error(val message: String) : DcEffect()
}

sealed class DcEvent : ViewEvent {
    data class OnOptionalFieldChange(val field: MatchedField, val checked: Boolean) : DcEvent()
    data object OnShareClicked : DcEvent()
    data object OnCancelClicked : DcEvent()
}
