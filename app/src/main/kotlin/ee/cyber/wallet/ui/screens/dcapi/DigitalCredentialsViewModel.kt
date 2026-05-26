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
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.documents.mdoc.MDocUtils.generateDCApiHandover
import ee.cyber.wallet.domain.presentation.CredentialClaim
import ee.cyber.wallet.domain.presentation.OpenId4VPManager
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.json.JSONObject
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.util.fromBase64Url
import org.slf4j.LoggerFactory
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption
import kotlin.io.encoding.ExperimentalEncodingApi

@HiltViewModel
class DigitalCredentialsViewModel @Inject constructor(
    private val allowedAppsJson: String,
    private val documentRepository: DocumentRepository,
    private val cryptoProviderFactory: CryptoProvider.Factory,
    private val openId4VPManager: OpenId4VPManager
) : MviViewModel<DcEvent, DcUiState, DcEffect>() {

    private val logger = LoggerFactory.getLogger(DigitalCredentialsViewModel::class.java)

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

                val presentationDefinition = DeviceRequestParser(
                    deviceRequestBase64.fromBase64Url(),
                    sessionTranscript.toCBOR()
                ).parseToPresentationDefinition()

                val documents = documentRepository.documents.first()
                val documentMatches = presentationDefinition.getDocumentMatches(documents)

                handleMatchResult(documentMatches, origin, sessionTranscript, recipientPublicKey)
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
        recipientPublicKey: EcPublicKey
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
                            recipientPublicKey = recipientPublicKey
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
                responseDocuments.add(documentResponse)
            }

            val deviceResponseBytes = DeviceResponse(responseDocuments).toCBOR()
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

    @OptIn(ExperimentalEncodingApi::class)
    private fun getEncryptedResponse(
        recipientPublicKey: EcPublicKey,
        deviceResponseBytes: ByteArray,
        sessionTranscript: ListElement
    ): String {
        val (cipherText, encapsulatedPublicKey) = Crypto.hpkeEncrypt(
            cipherSuite = Algorithm.HPKE_BASE_P256_SHA256_AES128GCM,
            receiverPublicKey = recipientPublicKey,
            plainText = deviceResponseBytes,
            aad = sessionTranscript.toCBOR()
        )
        val enc = (encapsulatedPublicKey as EcPublicKeyDoubleCoordinate).asUncompressedPointEncoding
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
    val recipientPublicKey: @RawValue EcPublicKey? = null
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
