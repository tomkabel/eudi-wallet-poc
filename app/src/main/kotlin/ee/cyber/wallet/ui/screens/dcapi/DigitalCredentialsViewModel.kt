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
import ee.cyber.wallet.security.SecureAreaKeyManager
import ee.cyber.wallet.crypto.deviceCryptoProvider
import ee.cyber.wallet.data.repository.DocumentRepository
import ee.cyber.wallet.data.repository.TransactionLogRepository
import ee.cyber.wallet.di.Dispatcher
import ee.cyber.wallet.di.WalletDispatchers
import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.presentation.DcApiRequestDispatch
import ee.cyber.wallet.domain.presentation.EePoaConsumption
import ee.cyber.wallet.domain.presentation.DcApiProtocol
import ee.cyber.wallet.domain.presentation.ProtocolRefusal
import ee.cyber.wallet.domain.documents.mdoc.MDocUtils.generateDCApiHandover
import ee.cyber.wallet.domain.presentation.CredentialClaim
import ee.cyber.wallet.domain.presentation.HolderObligations
import ee.cyber.wallet.domain.presentation.LongfellowZkPresenter
import ee.cyber.wallet.domain.presentation.ZkPresentation
import ee.cyber.wallet.domain.presentation.ZkPresentationReason
import ee.cyber.wallet.domain.presentation.ZkPresenter
import ee.cyber.wallet.domain.presentation.resolveSchemeId
import ee.cyber.wallet.domain.presentation.zkSpecsByDocType
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
import ee.cyber.wallet.util.readerAuthSubject
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
    private val secureAreaKeyManager: SecureAreaKeyManager,
    private val openId4VPManager: OpenId4VPManager,
    private val transactionLogRepository: TransactionLogRepository,
    private val eePoaConsumption: EePoaConsumption,
    @Dispatcher(WalletDispatchers.Default) private val defaultDispatcher: CoroutineDispatcher
) : MviViewModel<DcEvent, DcUiState, DcEffect>() {

    private val logger = LoggerFactory.getLogger(DigitalCredentialsViewModel::class.java)

    // EE-ZKP-004 (plan §4 item 4c): the ZK path sits behind an interface — the view model asks
    // for a presentation over a scheme id and gets back a document or a reason. The Longfellow
    // implementation wraps the same lazy prover as before; the scheme id travels in the proof.
    private val zkPresenter: ZkPresenter by lazy { LongfellowZkPresenter(zkSystem) }

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
                val requestsArray = json.getJSONArray("requests")

                // EE-PRO-013 / plan §4 item 4a: read the `protocol` field of every entry BEFORE
                // touching the payload. Take the first entry this wallet can answer and refuse
                // the rest with a specific error instead of letting the payload parser throw a
                // bare JSONException on bytes it was never meant to read.
                val protocolNames = (0 until requestsArray.length()).map { index ->
                    val entry = requestsArray.getJSONObject(index)
                    if (entry.has("protocol")) entry.getString("protocol") else null
                }
                val firstSupportedIndex = when (val decision = DcApiRequestDispatch.dispatch(protocolNames)) {
                    is DcApiRequestDispatch.Decision.Take -> decision.index
                    is DcApiRequestDispatch.Decision.Empty -> {
                        logger.error("DC API request carries no protocol entries")
                        sendEffect { DcEffect.Refused(ProtocolRefusal.UNSUPPORTED_PROTOCOL) }
                        return@launch
                    }
                    is DcApiRequestDispatch.Decision.Unsupported -> {
                        logger.error("Unsupported DC API protocol: ${decision.protocolName}")
                        sendEffect { DcEffect.Refused(ProtocolRefusal.UNSUPPORTED_PROTOCOL) }
                        return@launch
                    }
                }
                val firstRequest = requestsArray.getJSONObject(firstSupportedIndex)
                val data = firstRequest.getJSONObject("data")
                val deviceRequestBase64 = data.getString("deviceRequest")
                val encryptionInfoBase64 = data.getString("encryptionInfo")

                val sessionTranscript = getSessionTranscript(encryptionInfoBase64, origin)
                val recipientPublicKey = getRecipientPublicKey(encryptionInfoBase64)

                val docRequests = DeviceRequestParser(
                    deviceRequestBase64.fromBase64Url(),
                    sessionTranscript.toCBOR()
                ).parse().docRequests

                // EE-RP-003 / plan §4 item 4d (finding F7, review finding 1): the consent screen
                // names an origin, not a relying party. The readerAuth certificate subject is
                // shown only when the readerAuth signature check PASSED — the parser populates
                // the chain even on a failed check, so an ungated CN is attacker-chosen. Trust
                // validation of a passing chain stays §8.3 scope.
                val readerSubject = readerAuthSubject(docRequests)

                val presentationDefinition = toPresentationDefinition(docRequests)

                val documents = documentRepository.documents.first()
                val documentMatches = presentationDefinition.getDocumentMatches(documents)

                handleMatchResult(
                    documentMatches,
                    origin,
                    sessionTranscript,
                    recipientPublicKey,
                    // Keyed by docType: a ZK request for one document must not change how another
                    // is presented. A repeated docType keeps its first doc request's specs only.
                    zkSpecsByDocType(docRequests),
                    readerSubject
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
        zkSystemSpecs: Map<String, List<ZkSystemSpec>>,
        readerSubject: String?
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
                            // EE-RP-003 (4d): show the readerAuth certificate subject when the
                            // reader authenticated the request; the origin alone stays otherwise.
                            readerSubject = readerSubject,
                            credentials = credentials,
                            sessionTranscript = sessionTranscript,
                            recipientPublicKey = recipientPublicKey,
                            zkSystemSpecs = zkSystemSpecs,
                            expectedPlainTier = expectedPlainTier(zkSystemSpecs, credentials)
                        )
                    }
                }
            }
        }
    }

    /**
     * The linkable tier this presentation would fall back to, or null when a zero-knowledge proof
     * is expected for every matched document. Drives the EE-ZKP-042 pre-share notice: the user is
     * told before sharing whenever the response would carry issuer-signed, linkable documents.
     *
     * Each credential is judged by the same per-docType predicate the EE-ZKP-051 share-time
     * refusal runs, and this is recomputed on every optional-field toggle, because a toggle
     * changes the would-be proof's attribute count: the user must not reach the share-time
     * refusal without the pre-share notice having reflected the same state.
     */
    private fun expectedPlainTier(
        specs: Map<String, List<ZkSystemSpec>>,
        credentials: List<DcCredential>
    ): PresentationTier? {
        // Per credential, against its own docType's specs; the response is linkable if any one is.
        val tiers = credentials.mapNotNull { credential ->
            val docSpecs = specs[credential.credentialType.docType().uri].orEmpty()
            HolderObligations.expectedPlainTier(
                zkCapable = zkSystem != null,
                proofRequested = docSpecs.isNotEmpty(),
                // Only the age doctypes are ever proved (step 4c); any other document goes out
                // plain, so the notice must not expect a proof for it.
                satisfiable = credential.credentialType.requiresZkProof() &&
                    resolveSchemeId(zkSystem, docSpecs, credential.allCheckedFields.size) != null
            )
        }
        // A proof the party asked for and will not get outranks "never asked" in the wording.
        return tiers.firstOrNull { it != PresentationTier.PLAIN_NOT_REQUESTED } ?: tiers.firstOrNull()
    }

    /**
     * The doctypes the ZK path exists for. EE-ZKP-051 is applied strictly to these; other
     * documents keep the documented mixed-response behaviour.
     */
    private fun CredentialType.requiresZkProof(): Boolean = when (this) {
        CredentialType.AGE_VERIFICATION, CredentialType.EE_POA -> true
        else -> false
    }

    /**
     * EE-ZKP-051, strict reading, scoped to the age doctypes. The device here is capable of the
     * ZKP path in general, but the relying party advertised only circuits this wallet does not
     * hold. The lenient reading would let that advertisement work as a downgrade lever —
     * advertise an unknown circuit hash, receive a linkable presentation — so the wallet refuses,
     * tells the user why, and logs the refusal.
     *
     * Returns true when the presentation was refused and nothing may be shared.
     */
    private suspend fun refusePlainWhenSpecsUnsatisfiable(currentState: DcUiState): Boolean {
        // Each age document against the specs its own doc request advertised, never another's.
        val refused = currentState.credentials.firstOrNull { credential ->
            val specs = currentState.zkSystemSpecs[credential.credentialType.docType().uri].orEmpty()
            credential.credentialType.requiresZkProof() &&
                HolderObligations.refusePlainFallback(
                    zkCapable = zkSystem != null,
                    proofRequested = specs.isNotEmpty(),
                    satisfiable = resolveSchemeId(zkSystem, specs, credential.allCheckedFields.size) != null
                )
        } ?: return false

        logger.warn("EE-ZKP-051: refusing plain fallback — device is ZK-capable but the advertised specs cannot be satisfied")
        refusePlain(
            currentState.verifier,
            refused.credentialType.docType(),
            PresentationTier.PLAIN_NO_MATCHING_CIRCUIT,
            AppError.PRESENTATION_NO_MATCHING_CIRCUIT
        )
        return true
    }

    /** Logs the EE-ZKP-051 refusal and shows it; nothing is shared. */
    private suspend fun refusePlain(verifier: String, docType: DocType, tier: PresentationTier, error: AppError) {
        transactionLogRepository.addTransactionLog(party = verifier, docType = docType, tier = tier, error = error)
        setState { copy(isLoading = false, plainRefusal = error) }
        sendEffect { DcEffect.RefusedPlainFallback }
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
            val toggled = credentials.map { credential ->
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
            copy(
                credentials = toggled,
                // A toggle changes the would-be proof's attribute count, so the EE-ZKP-042
                // expected tier must track the current state the share-time refusal will see.
                expectedPlainTier = expectedPlainTier(zkSystemSpecs, toggled)
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

        // EE-ZKP-051, strict reading: before anything is signed, refuse a linkable plain fallback
        // for the age doctypes when the device could prove but the advertised specs cannot be met.
        if (refusePlainWhenSpecsUnsatisfiable(currentState)) return

        setState { copy(isLoading = true) }

        try {
            val responseDocuments = mutableListOf<MDoc>()
            val zkDocuments = mutableListOf<ZkDocument>()
            val presented = mutableListOf<Triple<DocType, JsonObject, PresentationTier>>()

            currentState.credentials.forEach { credential ->
                val mDoc = credential.mDoc
                val checkedFields = credential.allCheckedFields.map { it.field }
                val docType = credential.credentialType.docType().uri
                val zkSystemSpecs = currentState.zkSystemSpecs[docType].orEmpty()

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
                    // Step 5: the DeviceAuthentication signature is made inside the SecureArea key.
                    cryptoProvider = cryptoProvider.deviceCryptoProvider(secureAreaKeyManager, keyId),
                    keyID = keyId
                )

                // Proving is seconds of blocking native work, and the first match also forces the
                // lazy circuit load. Both stay off the main thread or the share screen freezes
                // instead of showing its spinner.
                //
                // EE-ZKP-004 (plan §4 item 4c, F11/F15): the view model asks the ZkPresenter for a
                // presentation over a scheme id and gets back a document or a reason. The scheme
                // is resolved from THIS document's own request — its doctype's zkRequest and its
                // own checked-field count — never from a spec pool merged across doc requests.
                // Proving itself stays scoped to the age doctypes.
                val zkResult = withContext(defaultDispatcher) {
                    val schemeId = if (credential.credentialType.requiresZkProof()) {
                        resolveSchemeId(zkSystem, zkSystemSpecs, checkedFields.size)
                    } else {
                        // Other doctypes are never asked to prove; the reason is recorded as
                        // NO_SCHEME_REQUESTED so the log and the response agree.
                        null
                    }
                    zkPresenter.presentation(
                        schemeId = schemeId,
                        document = MdocDocument.fromDataItem(Cbor.decode(documentResponse.toMapElement().toCBOR())),
                        sessionTranscript = Cbor.decode(sessionTranscript.toCBOR())
                    )
                }
                val reason = (zkResult as? ZkPresentation.Unavailable)?.reason
                // EE-ZKP-051, strict reading: the device is capable and the party asked for a proof
                // of an age document, so a failed prover must not become a silent linkable
                // presentation (no EE-ZKP-042 notice was shown — a proof was expected). A party able
                // to make proving fail would otherwise hold the same downgrade lever as one that
                // advertises an unknown circuit. Nothing has been sent yet; refuse the whole response.
                if (reason == ZkPresentationReason.PROVER_FAILED && credential.credentialType.requiresZkProof()) {
                    refusePlain(
                        currentState.verifier,
                        credential.credentialType.docType(),
                        PresentationTier.PLAIN_PROOF_FAILED,
                        AppError.PRESENTATION_PROOF_FAILED
                    )
                    return
                }
                val zkDocument = (zkResult as? ZkPresentation.Proved)?.zkDocument
                if (zkDocument == null) {
                    responseDocuments.add(documentResponse)
                } else {
                    zkDocuments.add(zkDocument)
                }

                // Named rather than inferred from the null above: "the verifier never asked" and
                // "this device cannot prove" are the same response but very different facts, and
                // EE-ZKP-053 wants the distinction on the record. The pre-share notice the user
                // saw (EE-ZKP-042) is worded from the same computation. The reason from the
                // presenter decides, so the tier can no longer disagree with the attempt.
                val tier = when (reason) {
                    null -> PresentationTier.ZERO_KNOWLEDGE
                    ZkPresentationReason.NO_SCHEME_REQUESTED -> HolderObligations.tierFor(
                        zkUsed = false,
                        proofRequested = zkSystemSpecs.isNotEmpty(),
                        zkCapable = zkSystem != null
                    )
                    ZkPresentationReason.PROVER_UNAVAILABLE -> PresentationTier.PLAIN_DEVICE_INCAPABLE
                    ZkPresentationReason.UNKNOWN_SCHEME -> PresentationTier.PLAIN_NO_MATCHING_CIRCUIT
                    ZkPresentationReason.PROVER_FAILED -> PresentationTier.PLAIN_PROOF_FAILED
                }
                // EE-ZKP-053 / F9: record what was shared, as the redirect path already does.
                val attributes = JsonObject(
                    checkedFields.associate { field ->
                        field.name to JsonPrimitive(field.value)
                    }
                )
                presented.add(Triple(credential.credentialType.docType(), attributes, tier))
            }

            val deviceResponseBytes = if (zkDocuments.isEmpty()) {
                DeviceResponse(responseDocuments).toCBOR()
            } else {
                zkDeviceResponse(zkDocuments, responseDocuments, sessionTranscript)
            }
            val response = getEncryptedResponse(recipientPublicKey, deviceResponseBytes, sessionTranscript)
            // Logged only once there is a response to send, so a failed share leaves no record of a presentation.
            // EE-ZKP-053 per response (plan §4 item 4c): once any document went out plain, every
            // document in the response is linkable through that session and the log must say so.
            val responseTiers = HolderObligations.escalateToResponseTier(presented.map { it.third })
            presented.zip(responseTiers).forEach { (row, tier) ->
                val (docType, attributes, _) = row
                transactionLogRepository.addTransactionLog(
                    party = currentState.verifier,
                    docType = docType,
                    attributes = attributes,
                    tier = tier
                )
            }

            logger.info("Response generated successfully")

            // EE-POA-013 / WIAM_21 (plan §4 item 6): plain presentations consume the EE-PoA
            // attestation and its SecureArea key, never the last of the batch; a ZK presentation
            // consumes nothing (EE-ZKP-025). The tiers are the escalated per-response ones just
            // logged; `presented` holds one row per credential, in order. The response is already
            // signed and encrypted, so deleting the key cannot change it. Consumption runs before
            // the effect because the activity finishes on SendResponse and would cancel it half
            // way; a failed consumption is logged and never withholds the response.
            currentState.credentials.zip(responseTiers).forEach { (credential, tier) ->
                runCatching {
                    eePoaConsumption.consumeAfterPresentation(attestation = credential.attestation, tier = tier)
                }.onFailure { logger.error("EE-PoA consumption failed", it) }
            }

            sendEffect { DcEffect.SendResponse(response) }
        } catch (e: Exception) {
            logger.error("Error generating response", e)
            sendEffect { DcEffect.Error(e.message ?: "Error generating response") }
        } finally {
            setState { copy(isLoading = false) }
        }
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
    // EE-RP-003 (4d, F7): the readerAuth certificate subject, when the reader authenticated the
    // request. Rendered on the consent screen next to the origin; null when absent.
    val readerSubject: String? = null,
    val credentials: List<DcCredential> = listOf(),
    val shareDisabled: Boolean = false,
    val sessionTranscript: @RawValue ListElement? = null,
    val recipientPublicKey: @RawValue EcPublicKey? = null,
    val zkSystemSpecs: @RawValue Map<String, List<ZkSystemSpec>> = mapOf(),
    // EE-ZKP-042: set once a match exists, before the user shares. Non-null when the response
    // would be linkable, carrying which linkable tier it would fall back to.
    val expectedPlainTier: PresentationTier? = null,
    // EE-ZKP-051 refusal already happened for this request; the screen shows this reason.
    val plainRefusal: AppError? = null
) : ViewState, Parcelable

sealed class DcEffect : ViewSideEffect {
    data class SendResponse(val response: String) : DcEffect()
    data object Cancel : DcEffect()
    data object NoMatch : DcEffect()
    data class Error(val message: String) : DcEffect()

    /**
     * A refusal with a user-facing, localized reason (F8): distinct from a generic error so the
     * activity can surface the localized text instead of folding the case into cancellation.
     * The wire-level exception mapping stays the record's open item.
     */
    data class Refused(val refusal: ProtocolRefusal) : DcEffect()

    // EE-ZKP-051: the plain fallback was refused because the advertised ZK specs cannot be met.
    data object RefusedPlainFallback : DcEffect()
}

sealed class DcEvent : ViewEvent {
    data class OnOptionalFieldChange(val field: MatchedField, val checked: Boolean) : DcEvent()
    data object OnShareClicked : DcEvent()
    data object OnCancelClicked : DcEvent()
}
