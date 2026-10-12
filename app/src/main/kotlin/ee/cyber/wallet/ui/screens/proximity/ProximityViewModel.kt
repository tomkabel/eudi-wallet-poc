package ee.cyber.wallet.ui.screens.proximity

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import ee.cyber.wallet.R
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.crypto.deviceCryptoProvider
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.data.repository.DocumentRepository
import ee.cyber.wallet.data.repository.TransactionLogRepository
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.presentation.AgePredicatePolicy
import ee.cyber.wallet.domain.presentation.AgePredicatePolicy.error
import ee.cyber.wallet.domain.presentation.CredentialClaim
import ee.cyber.wallet.domain.presentation.EePoaConsumption
import ee.cyber.wallet.domain.presentation.HolderObligations
import ee.cyber.wallet.domain.presentation.ZkCapability
import ee.cyber.wallet.domain.presentation.OpenId4VPManager
import ee.cyber.wallet.domain.presentation.PresentationTier
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.security.SecureAreaKeyManager
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
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.TransferManager
import eu.europa.ec.eudi.iso18013.transfer.engagement.BleRetrievalMethod
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.prex.FieldQueryResult
import eu.europa.ec.eudi.prex.Match
import eu.europa.ec.eudi.prex.PresentationDefinition
import id.walt.mdoc.dataelement.DataElement
import id.walt.mdoc.dataelement.EncodedCBORElement
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.doc.MDoc
import id.walt.mdoc.docrequest.MDocRequestBuilder
import id.walt.mdoc.mdocauth.DeviceAuthentication
import ee.cyber.wallet.di.Dispatcher
import ee.cyber.wallet.di.WalletDispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.slf4j.LoggerFactory
import javax.inject.Inject

@HiltViewModel
class ProximityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    // D15 (jvm L-batch): the injected collaborators are implementation detail — only the
    // MVI surface (state/effect/event) is public to the screen. (transferManager was
    // already made private in a parallel session.)
    private val documentRepository: DocumentRepository,
    private val openId4VPManager: OpenId4VPManager,
    private val transferManager: TransferManager,
    private val cryptoProviderFactory: CryptoProvider.Factory,
    private val secureAreaKeyManager: SecureAreaKeyManager,
    private val transactionLogRepository: TransactionLogRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val eePoaConsumption: EePoaConsumption,
    @Dispatcher(WalletDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) : MviViewModel<Event, UiState, Effect>() {

    private val logger = LoggerFactory.getLogger(ProximityViewModel::class.java)

    // D12 (mobile F8): the share is PIN-gated; the flag flips on the PIN
    // flow's UserAuthenticated event (same pattern as PresentationRequestViewModel).
    private val authenticated = kotlinx.coroutines.flow.MutableStateFlow(false)

    init {
        viewModelScope.launch {
            userPreferencesDataSource.userPreferences.collect { prefs ->
                setState { copy(isPeripheralMode = prefs.blePeripheralMode) }
                setupTransferManager()
            }
        }
    }

    // JVM-M5/D9: the transfer-event listener and the QR engagement are session
    // state, not per-preference state — registering them on every preference
    // emission stacked listeners (one event handled N times) and restarted the
    // engagement mid-session. Registered once, lazily; only setRetrievalMethods
    // follows the preference, and only when the mode actually changed.
    private var transferListenersRegistered = false
    private var lastAppliedPeripheralMode: Boolean? = null

    private fun setupTransferManager() {
        val isPeripheralMode = state.value.isPeripheralMode
        if (!transferListenersRegistered) {
            transferListenersRegistered = true
            transferManager.addTransferEventListener { event ->
                when (event) {
                    is TransferEvent.QrEngagementReady -> {
                        val qrCodeBitmap = event.qrCode.asBitmap(size = 800)
                        setState { copy(qrCodeBitmap = qrCodeBitmap) }
                    }

                    TransferEvent.Connecting -> {
                        logger.info("Connecting to device...")
                    }

                    TransferEvent.Connected -> {
                        logger.info("Connected to device")
                    }

                    is TransferEvent.RequestReceived -> {
                        logger.info("Request received")
                        val deviceRequest = event.request as DeviceRequest

                        viewModelScope.launch {
                            try {
                                handleRequestObject(deviceRequest)
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                logger.error("proximity request could not be processed", e)
                                transferManager.stopPresentation(true)
                                sendEffect { Effect.ProximityRequestNoMatch }
                            }
                        }
                    }

                    TransferEvent.ResponseSent -> {
                        logger.info("Response sent to the device")
                        transferManager.stopPresentation(false)

                        sendEffect { Effect.ProximityResponseSent }
                    }

                    TransferEvent.Disconnected -> {
                        logger.info("Disconnected from the device")
                        transferManager.stopPresentation(false)
                    }

                    is TransferEvent.Error -> {
                        logger.error("Error occurred: ${event.error}")
                        transferManager.stopPresentation(false)
                    }

                    is TransferEvent.Redirect -> TODO()
                    is TransferEvent.IntentToSend -> TODO()
                }
            }
            transferManager.startQrEngagement()
        }
        // The retrieval method IS per-preference state, but re-setting it on an
        // unchanged mode tears BLE down for nothing (JVM-M5 rider: distinct).
        if (lastAppliedPeripheralMode != isPeripheralMode) {
            lastAppliedPeripheralMode = isPeripheralMode
            transferManager.setRetrievalMethods(
                listOf(
                    BleRetrievalMethod(
                        peripheralServerMode = isPeripheralMode,
                        centralClientMode = !isPeripheralMode,
                        clearBleCache = true
                    )
                )
            )
        }
    }

    private suspend fun handleRequestObject(deviceRequest: DeviceRequest) {
        val presentationDefinition = deviceRequest.toPresentationDefinition()
        val documentMatches = presentationDefinition.getDocumentMatches(documentRepository.documents.first())
        when (val match = documentMatches.second) {
            is Match.NotMatched -> {
                transferManager.stopPresentation(true)
                sendEffect { Effect.ProximityRequestNoMatch }
            }

            is Match.Matched -> {
                val credentials = mutableListOf<Credential>()
                val claims = documentMatches.first
                match.matches.forEach { inputDescriptor ->
                    val candidateClaimId = inputDescriptor.value
                        .filter { (_, candidateClaim) ->
                            candidateClaim.matches.any { it.value is FieldQueryResult.CandidateField.Found }
                        }
                        .keys
                        .first()
                    val document = claims.first { it.uniqueId == candidateClaimId }.credentialDocument as CredentialDocument.MDocDocument
                    val fields = match.fields(
                        candidateClaimId,
                        false,
                        document.type
                    )
                    val optionalFields = match.fields(candidateClaimId, true, document.type)
                    if (!fields.isEmpty() || !optionalFields.isEmpty()) {
                        val credential = Credential(
                            id = document.id,
                            credentialType = document.credentialType(),
                            fields = fields,
                            optionalFields = optionalFields,
                            attestation = document.attestation,
                            mDoc = document.mDoc
                        )
                        credentials.add(credential)
                    }
                }

                ageRefusal(deviceRequest, credentials)?.let { error ->
                    logger.warn("Proximity age request refused: {}", error)
                    credentials.filter { AgePredicatePolicy.isAgeAttestation(it.credentialType) }.forEach {
                        transactionLogRepository.addTransactionLog(
                            party = context.getString(R.string.log_entry_proximity_party),
                            docType = it.credentialType.docType(),
                            error = error
                        )
                    }
                    // A predicate held false (EE-POA-031) ends exactly like no match; the other
                    // refusals end the same way on the wire and name their reason on screen.
                    transferManager.stopPresentation(true)
                    setState { copy(refusal = error) }
                    sendEffect { Effect.ProximityRequestNoMatch }
                    return
                }
                val lastAgeAttestation = credentials.any {
                    AgePredicatePolicy.isAgeAttestation(it.credentialType) && eePoaConsumption.remaining(it.credentialType) <= 1
                }
                setState {
                    copy(
                        credentials = credentials,
                        sessionTranscript = deviceRequest.sessionTranscriptBytes,
                        lastAgeAttestation = lastAgeAttestation
                    )
                }
                sendEffect { Effect.ProximityRequest }
            }
        }
    }

    /**
     * The age rules a proximity request must pass before consent. EE-POA-030/031 as on every
     * path. This path has no Tier 2 (no proximity ZK code exists), so on a ZK-capable device an
     * age attestation is refused whenever Tier 2 would be required: the reader did not
     * authenticate to a trusted access CA (EE-PRO-025, EE-ZKP-055a), or the attestation is the
     * last of its batch (EE-POA-013). No reader trust anchors are configured yet, so every
     * reader is unauthenticated here.
     */
    private suspend fun ageRefusal(deviceRequest: DeviceRequest, credentials: List<Credential>): AppError? {
        AgePredicatePolicy.decideAll(credentials.map { it.credentialType to it.fields + it.optionalFields }).error()
            ?.let { return it }
        val ageCredentials = credentials.filter { AgePredicatePolicy.isAgeAttestation(it.credentialType) }
        if (ageCredentials.isEmpty() || !ZkCapability.capable) return null
        val authenticated = DeviceRequestParser(deviceRequest.deviceRequestBytes, deviceRequest.sessionTranscriptBytes)
            .parse().docRequests.any { it.readerAuthenticated && it.readerChainTrusted }
        val last = ageCredentials.any { eePoaConsumption.remaining(it.credentialType) <= 1 }
        val refused = HolderObligations.refusePlainFallback(
            zkCapable = true,
            proofRequested = false,
            satisfiable = false,
            authenticated = authenticated,
            lastAttestation = last
        )
        return if (refused) HolderObligations.plainRefusalReason(proofRequested = false, authenticated = authenticated) else null
    }

    private fun PresentationDefinition.getDocumentMatches(documents: List<CredentialDocument>): Pair<List<CredentialClaim>, Match> =
        openId4VPManager.matches(this, documents)

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

    private fun updateState(reducer: UiState.() -> UiState) {
        setState {
            reducer()
                .let { state ->
                    val disableShare = state.credentials.all { credential ->
                        credential.fields.isEmpty() && credential.optionalFields.none { it.checked }
                    }
                    if (state.shareDisabled != disableShare) {
                        state.copy(shareDisabled = disableShare)
                    } else {
                        state
                    }
                }
        }
    }

    override fun initialState(): UiState {
        return UiState()
    }

    override suspend fun handleEvents(event: Event) {
        when (event) {
            is Event.OnShareClicked -> onShareClicked()
            Event.OnCancelClicked -> {
                transferManager.stopPresentation(false)
                sendEffect { Effect.ProximityCancel }
            }
            is Event.OnOptionalFieldChange -> onOptionalFieldChange(event.field, event.checked)
            Event.OnBleModeToggle -> {
                viewModelScope.launch {
                    userPreferencesDataSource.setBlePeripheralMode(!state.value.isPeripheralMode)
                }
                transferManager.stopPresentation(false)
            }
            // D12 (mobile F8): the PIN flow resolves through the screen's
            // launcher; only a confirmed PIN releases the response.
            Event.UserAuthenticated -> {
                authenticated.value = true
                onShareClicked()
            }
            Event.IncorrectPin -> {
                // The BLE session stays alive: the reader is still connected
                // and may retry or the user may re-enter the PIN (deliberately
                // NOT the remote path's onCancel() navigation).
                logger.warn("proximity PIN incorrect — session kept alive")
            }
        }
    }

    /**
     * D12 (mobile F8): the share is PIN-gated. Without authentication the
     * first tap only raises [Effect.AuthenticateWithPin] and NOTHING is sent;
     * after [Event.UserAuthenticated] the response is released. Unlike the
     * remote path, a cancelled PIN keeps the BLE session (the reader waits).
     */
    private suspend fun onShareClicked() {
        if (!authenticated.value) {
            sendEffect { Effect.AuthenticateWithPin(party = PROXIMITY_READER_NAME) }
            return
        }

        // D15 (jvm L6): the transcript is session state set by the engagement — reaching the
        // send path without it means the BLE session never completed the engagement step.
        // Fail the send with a logged error instead of a KotlinNullPointerException from
        // `!!`; the reader sees a cancelled transfer either way.
        val sessionTranscriptBytes = state.value.sessionTranscript
        if (sessionTranscriptBytes == null) {
            logger.error("proximity send without an engagement session transcript — refusing to respond")
            sendEffect { Effect.ProximityCancel }
            return
        }

        // A cancelled or failed biometric prompt surfaces as KeyLockedException from the sign
        // below; any failure ends the transfer rather than leaving the reader waiting.
        try {
            val responseDocuments = mutableListOf<MDoc>()
            val documentIds = mutableListOf<String>()
            state.value.credentials.forEach { credential ->
                val mDoc = credential.mDoc
                val fields = credential.allCheckedFields.map { it.field }
                val docType = credential.credentialType.docType().uri
                val mDocRequest = MDocRequestBuilder(docType).apply {
                    fields.forEach {
                        addDataElementRequest(it.namespace.uri, it.name, true)
                    }
                }.build(null)
                val cryptoProvider = cryptoProviderFactory.forKeyType(credential.attestation.keyAttestation.keyType)
                val keyId = credential.attestation.keyAttestation.keyId
                val deviceNameSpaces = EncodedCBORElement(MapElement(mapOf()))
                val sessionTranscript = DataElement.fromCBOR<ListElement>(sessionTranscriptBytes)
                val deviceAuthentication = DeviceAuthentication(sessionTranscript, docType, deviceNameSpaces)
                val deviceCryptoProvider = cryptoProvider.deviceCryptoProvider(secureAreaKeyManager, keyId)
                // Off the main thread: the SecureArea sign blocks until the biometric prompt
                // answers, and that prompt needs the main thread — signing on it deadlocks.
                val documentResponse = withContext(defaultDispatcher) {
                    mDoc.presentWithDeviceSignature(
                        mDocRequest = mDocRequest,
                        deviceAuthentication = deviceAuthentication,
                        // Step 5: the DeviceAuthentication signature is made inside the SecureArea key.
                        cryptoProvider = deviceCryptoProvider,
                        keyID = keyId
                    )
                }
                responseDocuments.add(documentResponse)
                documentIds.add(credential.id)
            }

            val response = eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse(
                deviceResponseBytes = ee.cyber.wallet.domain.documents.mdoc.DeviceResponse(responseDocuments).toCBOR(),
                sessionTranscriptBytes = sessionTranscriptBytes,
                documentIds = documentIds
            )
            transferManager.sendResponse(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("proximity response failed — cancelling the transfer", e)
            transferManager.stopPresentation(false)
            sendEffect { Effect.ProximityCancel }
            return
        }

        // EE-ZKP-053: proximity presentations land in the same log as the online paths.
        // A proximity reader cannot carry a ZK request either, so the tier is literally
        // "the reader did not ask for a proof", and the row records what was disclosed.
        state.value.credentials.forEach { credential ->
            val attributes = JsonObject(
                credential.allCheckedFields.associate { matchedField ->
                    matchedField.field.name to JsonPrimitive(matchedField.field.value)
                }
            )
            transactionLogRepository.addTransactionLog(
                party = context.getString(R.string.log_entry_proximity_party),
                docType = credential.credentialType.docType(),
                attributes = attributes,
                tier = PresentationTier.PLAIN_NOT_REQUESTED
            )
            // EE-POA-013 / WIAM_21: a plain proximity presentation consumes the age attestation
            // as the online paths do — never the last of the batch. A failure is logged and
            // never turns a delivered response into an error.
            runCatching {
                eePoaConsumption.consumeAfterPresentation(credential.attestation, PresentationTier.PLAIN_NOT_REQUESTED)
            }.onFailure { logger.error("EE-PoA consumption failed", it) }
        }
    }
}

/** Generic reader name until reader-auth trust work (E5) supplies the reader CN. */
private const val PROXIMITY_READER_NAME = "Proximity reader"

@Parcelize
data class Credential(
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
data class UiState(
    val verifier: String = "",
    val sessionTranscript: ByteArray? = null,
    val qrCodeBitmap: Bitmap? = null,
    val credentials: List<Credential> = listOf(),
    val shareDisabled: Boolean = false,
    val isPeripheralMode: Boolean = true,
    // Why an age request was refused before consent; null for a plain no-match.
    val refusal: AppError? = null,
    // EE-POA-013: an age attestation in this request is the last of its batch.
    val lastAgeAttestation: Boolean = false
) : ViewState, Parcelable

sealed class Effect : ViewSideEffect {
    data object ProximityRequest : Effect()
    data object ProximityRequestNoMatch : Effect()

    data object ProximityResponseSent : Effect()
    data object ProximityCancel : Effect()

    /**
     * D12 (mobile F8): the proximity share is gated by the same PIN flow as
     * the remote path. `party` is a generic reader name until the reader-auth
     * trust work (E5) can supply the actual reader CN.
     */
    data class AuthenticateWithPin(val party: String) : Effect()
}

sealed class Event : ViewEvent {
    data class OnOptionalFieldChange(val field: MatchedField, val checked: Boolean) : Event()
    data object OnShareClicked : Event()
    data object OnCancelClicked : Event()
    data object OnBleModeToggle : Event()
    data object UserAuthenticated : Event()
    data object IncorrectPin : Event()
}
