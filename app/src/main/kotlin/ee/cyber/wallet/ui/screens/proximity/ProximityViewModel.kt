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
import ee.cyber.wallet.di.Dispatcher
import ee.cyber.wallet.di.WalletDispatchers
import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.presentation.AgePredicatePolicy
import ee.cyber.wallet.domain.presentation.AgePredicatePolicy.error
import ee.cyber.wallet.domain.presentation.CredentialClaim
import ee.cyber.wallet.domain.presentation.EePoaConsumption
import ee.cyber.wallet.domain.presentation.OpenId4VPManager
import ee.cyber.wallet.domain.presentation.PresentationTier
import ee.cyber.wallet.domain.presentation.WalletRequestDispatch
import ee.cyber.wallet.domain.presentation.ZkCapability
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
import ee.cyber.wallet.ui.screens.presentation.RequestRefusalDispatch
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
class ProximityViewModel private constructor(
    private val production: Production?,
    private val requestDependencies: RequestDependencies?
) : MviViewModel<Event, UiState, Effect>() {

    @Inject
    constructor(production: Production) : this(production, null)

    internal constructor(dependencies: RequestDependencies) : this(null, dependencies)

    data class Production @Inject constructor(
        @ApplicationContext val context: Context,
        val documentRepository: DocumentRepository,
        val openId4VPManager: OpenId4VPManager,
        val transferManager: TransferManager,
        val cryptoProviderFactory: CryptoProvider.Factory,
        val secureAreaKeyManager: SecureAreaKeyManager,
        val transactionLogRepository: TransactionLogRepository,
        val userPreferencesDataSource: UserPreferencesDataSource,
        val eePoaConsumption: EePoaConsumption,
        @Dispatcher(WalletDispatchers.Default) val defaultDispatcher: CoroutineDispatcher
    )

    private val context: Context get() = requireNotNull(production).context
    private val documentRepository: DocumentRepository get() = requireNotNull(production).documentRepository
    private val openId4VPManager: OpenId4VPManager get() = requireNotNull(production).openId4VPManager
    private val transferManager: TransferManager get() = requireNotNull(production).transferManager
    private val cryptoProviderFactory: CryptoProvider.Factory get() = requireNotNull(production).cryptoProviderFactory
    private val secureAreaKeyManager: SecureAreaKeyManager get() = requireNotNull(production).secureAreaKeyManager
    private val transactionLogRepository: TransactionLogRepository get() = requireNotNull(production).transactionLogRepository
    private val userPreferencesDataSource: UserPreferencesDataSource get() = requireNotNull(production).userPreferencesDataSource
    private val eePoaConsumption: EePoaConsumption get() = requireNotNull(production).eePoaConsumption
    private val defaultDispatcher: CoroutineDispatcher get() = requireNotNull(production).defaultDispatcher

    private val logger = LoggerFactory.getLogger(ProximityViewModel::class.java)

    // D12 (mobile F8): the share is PIN-gated; the flag flips on the PIN
    // flow's UserAuthenticated event (same pattern as PresentationRequestViewModel).
    private val authenticated = kotlinx.coroutines.flow.MutableStateFlow(false)

    internal data class RequestDependencies(
        val documents: suspend () -> List<CredentialDocument>,
        val matches: (PresentationDefinition, List<CredentialDocument>) -> Pair<List<CredentialClaim>, Match>,
        val capable: () -> Boolean,
        val remaining: suspend (CredentialType) -> Int,
        val logRefusal: suspend (DocType, AppError) -> Unit,
        val listen: ((TransferEvent) -> Unit) -> Unit,
        val stop: (Boolean) -> Unit,
        val send: (eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceResponse) -> Unit
    )

    init {
        val dependencies = requestDependencies
        if (dependencies != null) {
            dependencies.listen(::onTransferEvent)
        } else {
            viewModelScope.launch {
                userPreferencesDataSource.userPreferences.collect { prefs ->
                    setState { copy(isPeripheralMode = prefs.blePeripheralMode) }
                    setupTransferManager()
                }
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
            transferManager.addTransferEventListener(::onTransferEvent)
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

    private fun onTransferEvent(event: TransferEvent) {
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
                viewModelScope.launch {
                    try {
                        WalletRequestDispatch.proximity(event) { request, parsed ->
                            handleRequestObject(request, parsed)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("proximity request could not be processed", e)
                        stopPresentation(true)
                        sendEffect { Effect.ProximityRequestNoMatch }
                    }
                }
            }

            TransferEvent.ResponseSent -> {
                logger.info("Response sent to the device")
                stopPresentation(false)

                sendEffect { Effect.ProximityResponseSent }
            }

            TransferEvent.Disconnected -> {
                logger.info("Disconnected from the device")
                stopPresentation(false)
            }

            is TransferEvent.Error -> {
                logger.error("Error occurred: ${event.error}")
                stopPresentation(false)
            }

            is TransferEvent.Redirect -> TODO()
            is TransferEvent.IntentToSend -> TODO()
        }
    }

    private fun matchedCredentials(documentMatches: Pair<List<CredentialClaim>, Match>, match: Match.Matched): List<Credential> {
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

        return credentials
    }

    private suspend fun handleRequestObject(deviceRequest: DeviceRequest, docRequests: List<DeviceRequestParser.DocRequest>) {
        val presentationDefinition = toPresentationDefinition(docRequests)
        val documents = requestDependencies?.documents?.invoke() ?: documentRepository.documents.first()
        val documentMatches = requestDependencies?.matches?.invoke(presentationDefinition, documents)
            ?: openId4VPManager.matches(presentationDefinition, documents)
        when (val match = documentMatches.second) {
            is Match.NotMatched -> {
                stopPresentation(true)
                sendEffect { Effect.ProximityRequestNoMatch }
            }

            is Match.Matched -> {
                val credentials = matchedCredentials(documentMatches, match)

                AgePredicatePolicy.decideAll(credentials.map { it.credentialType to it.fields + it.optionalFields }).error()?.let { error ->
                    refuseAgeRequest(credentials, error)
                    return
                }
                if (WalletRequestDispatch.agePlain(
                        credentials = credentials,
                        policy = WalletRequestDispatch.AgeRequestPolicy(
                            (requestDependencies?.capable?.invoke() ?: ZkCapability.capable),
                            WalletRequestDispatch.authenticationByDocType(docRequests)
                        ),
                        describe = { credential ->
                            WalletRequestDispatch.AgeCredential(
                                credential.credentialType,
                                credential.credentialType.docType().uri,
                                proofRequested = false,
                                satisfiable = false,
                                lastAttestation = remaining(credential.credentialType) <= 1
                            )
                        },
                        refuse = { _, error -> refuseAgeRequest(credentials, error) },
                        proceed = {}
                    )
                ) {
                    return
                }
                val lastAgeAttestation = credentials.any {
                    AgePredicatePolicy.isAgeAttestation(it.credentialType) && remaining(it.credentialType) <= 1
                }
                setState {
                    copy(
                        credentials = credentials,
                        sessionTranscript = deviceRequest.sessionTranscriptBytes,
                        refusal = null,
                        lastAgeAttestation = lastAgeAttestation
                    )
                }
                sendEffect { Effect.ProximityRequest }
            }
        }
    }

    private suspend fun refuseAgeRequest(credentials: List<Credential>, error: AppError) {
        logger.warn("Proximity age request refused: {}", error)
        // Close the transfer and retain the refusal before any fallible persistence operation.
        authenticated.value = false
        RequestRefusalDispatch.proximityAge(
            state.value,
            error,
            { updated -> setState { updated } },
            { stop -> stopPresentation(stop) },
            { emitted -> sendEffect { emitted } }
        )
        credentials.filter { AgePredicatePolicy.isAgeAttestation(it.credentialType) }.forEach {
            val dependencies = requestDependencies
            if (dependencies != null) {
                dependencies.logRefusal(it.credentialType.docType(), error)
            } else {
                transactionLogRepository.addTransactionLog(
                    party = context.getString(R.string.log_entry_proximity_party),
                    docType = it.credentialType.docType(),
                    error = error
                )
            }
        }
    }

    private val remaining: suspend (CredentialType) -> Int = { type ->
        requestDependencies?.remaining?.invoke(type) ?: eePoaConsumption.remaining(type)
    }

    private val stopPresentation: (Boolean) -> Unit = { sendSessionTerminationMessage ->
        val dependencies = requestDependencies
        if (dependencies != null) {
            dependencies.stop(sendSessionTerminationMessage)
        } else {
            transferManager.stopPresentation(sendSessionTerminationMessage)
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
                stopPresentation(false)
                sendEffect { Effect.ProximityCancel }
            }
            is Event.OnOptionalFieldChange -> onOptionalFieldChange(event.field, event.checked)
            Event.OnBleModeToggle -> {
                viewModelScope.launch {
                    userPreferencesDataSource.setBlePeripheralMode(!state.value.isPeripheralMode)
                }
                stopPresentation(false)
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
        if (state.value.refusal != null) return
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
            val dependencies = requestDependencies
            if (dependencies != null) {
                dependencies.send(response)
            } else {
                transferManager.sendResponse(response)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("proximity response failed — cancelling the transfer", e)
            stopPresentation(false)
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
