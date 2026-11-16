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
import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.documents.mdoc.MDocUtils.generateDCApiHandover
import ee.cyber.wallet.domain.presentation.AgePredicatePolicy
import ee.cyber.wallet.domain.presentation.AgePredicatePolicy.error
import ee.cyber.wallet.domain.presentation.CredentialClaim
import ee.cyber.wallet.domain.presentation.EePoaConsumption
import ee.cyber.wallet.domain.presentation.HolderObligations
import ee.cyber.wallet.domain.presentation.LongfellowZkPresenter
import ee.cyber.wallet.domain.presentation.OpenId4VPManager
import ee.cyber.wallet.domain.presentation.PresentationTier
import ee.cyber.wallet.domain.presentation.ProtocolRefusal
import ee.cyber.wallet.domain.presentation.WalletRequestDispatch
import ee.cyber.wallet.domain.presentation.ZkCapability
import ee.cyber.wallet.domain.presentation.ZkPresentation
import ee.cyber.wallet.domain.presentation.ZkPresentationReason
import ee.cyber.wallet.domain.presentation.ZkPresenter
import ee.cyber.wallet.domain.presentation.mergeZkSpecs
import ee.cyber.wallet.domain.presentation.registryFailure
import ee.cyber.wallet.domain.presentation.resolveSchemeId
import ee.cyber.wallet.domain.presentation.zkSpecsByDocType
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
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
import ee.cyber.wallet.util.readerAuthSubject
import ee.cyber.wallet.util.toPresentationDefinition
import ee.cyber.wallet.util.zkSpecsByDocTypeFromDcql
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.json.JSONObject
import org.multipaz.cbor.Cbor
import org.multipaz.crypto.EcPublicKey
import org.multipaz.crypto.Hpke
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.response.buildDeviceResponse
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.util.fromBase64Url
import org.slf4j.LoggerFactory
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.Base64.PaddingOption
import kotlin.io.encoding.ExperimentalEncodingApi
import org.multipaz.mdoc.response.DeviceResponse as MdocDeviceResponse

@HiltViewModel
class DigitalCredentialsViewModel private constructor(
    private val production: Production?,
    private val requestDependencies: RequestDependencies?
) : MviViewModel<DcEvent, DcUiState, DcEffect>() {

    @Inject
    constructor(production: Production) : this(production, null)

    internal constructor(dependencies: RequestDependencies) : this(null, dependencies)

    data class Production @Inject constructor(
        val allowedAppsJson: String,
        val documentRepository: DocumentRepository,
        val cryptoProviderFactory: CryptoProvider.Factory,
        val secureAreaKeyManager: SecureAreaKeyManager,
        val openId4VPManager: OpenId4VPManager,
        val transactionLogRepository: TransactionLogRepository,
        val eePoaConsumption: EePoaConsumption,
        @Dispatcher(WalletDispatchers.Default) val defaultDispatcher: CoroutineDispatcher
    )

    private val allowedAppsJson: String get() = requireNotNull(production).allowedAppsJson
    private val documentRepository: DocumentRepository get() = requireNotNull(production).documentRepository
    private val cryptoProviderFactory: CryptoProvider.Factory get() = requireNotNull(production).cryptoProviderFactory
    private val secureAreaKeyManager: SecureAreaKeyManager get() = requireNotNull(production).secureAreaKeyManager
    private val openId4VPManager: OpenId4VPManager get() = requireNotNull(production).openId4VPManager
    private val transactionLogRepository: TransactionLogRepository get() = requireNotNull(production).transactionLogRepository
    private val eePoaConsumption: EePoaConsumption get() = requireNotNull(production).eePoaConsumption
    private val defaultDispatcher: CoroutineDispatcher get() = requireNotNull(production).defaultDispatcher

    private val logger = LoggerFactory.getLogger(DigitalCredentialsViewModel::class.java)

    // EE-ZKP-004 (plan §4 item 4c): the ZK path sits behind an interface — the view model asks
    // for a presentation over a scheme id and gets back a document or a reason. The Longfellow
    // implementation wraps the same lazy prover as before; the scheme id travels in the proof.
    private val zkPresenter: ZkPresenter by lazy { LongfellowZkPresenter(zkSystem) }

    // Null when this device cannot prove at all (ZkCapability).
    private val zkSystem: ZkSystem? get() = requestDependencies?.zkSystem?.invoke() ?: if (requestDependencies == null) ZkCapability.zkSystem else null

    override fun initialState(): DcUiState = DcUiState()

    /** The ViewModel outlives activity recreation; the request is processed once per ViewModel. */
    private var requestProcessed = false

    internal data class ProviderRequest(val requestJson: String, val origin: String)

    internal data class RequestDependencies(
        val documents: suspend () -> List<CredentialDocument>,
        val matches: (PresentationDefinition, List<CredentialDocument>) -> Pair<List<CredentialClaim>, Match>,
        val zkSystem: () -> ZkSystem?,
        val remaining: suspend (CredentialType) -> Int,
        val logRefusal: suspend (String, DocType, AppError, PresentationTier?) -> Unit,
        val transcript: (String, String) -> ListElement,
        val recipientKey: (String) -> EcPublicKey,
        val releaseResponse: (String) -> Unit
    )

    @OptIn(ExperimentalDigitalCredentialApi::class)
    fun processRequest(intent: Intent) = processRequest {
        val providerRequest = PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        if (providerRequest == null) {
            logger.error("No credential request found")
            sendEffect { DcEffect.Error("No credential request found") }
            return@processRequest null
        }

        val origin = providerRequest.callingAppInfo.getOrigin(allowedAppsJson)
        if (origin == null) {
            logger.error("Could not determine origin")
            sendEffect { DcEffect.Error("Could not determine origin") }
            return@processRequest null
        }

        val option = providerRequest.credentialOptions
            .filterIsInstance<GetDigitalCredentialOption>()
            .firstOrNull()
        if (option == null) {
            logger.error("No digital credential option found")
            sendEffect { DcEffect.Error("No digital credential option found") }
            return@processRequest null
        }

        ProviderRequest(option.requestJson, origin)
    }

    internal fun processRequest(request: ProviderRequest) = processRequest { request }

    private fun processRequest(load: suspend () -> ProviderRequest?) {
        if (requestProcessed) return
        requestProcessed = true
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val request = load() ?: return@launch
                val origin = request.origin
                WalletRequestDispatch.dcApi(
                    requestJson = request.requestJson,
                    sessionTranscript = { getSessionTranscript(it, origin).toCBOR() },
                    refuse = ::refuseProtocol
                ) { wireRequest ->
                    val data = wireRequest.data
                    val sessionTranscript = getSessionTranscript(wireRequest.encryptionInfo, origin)
                    val recipientPublicKey = getRecipientPublicKey(wireRequest.encryptionInfo)
                    val docRequests = wireRequest.docRequests

                    // Plan §8.7: the accepted `org-iso-mdoc` entry's `data` may carry a DCQL query in
                    // `dcqlQuery` (provisional wire location; the protocol allow-list above is
                    // unchanged, `openid4vp` stays refused). Its mso_mdoc_zk zk_system_type entries
                    // parse into the same ZkSystemSpec model the ISO path uses and join the
                    // per-docType map — the ISO specs win where the doc request carried a zkRequest,
                    // DCQL's apply otherwise (mergeZkSpecs) — and the EE-ZKP-051 refusal, the
                    // per-credential resolveSchemeId and the EE-ZKP-042 notice run unchanged over it.
                    val dcqlSpecsByDocType = if (data.containsKey("dcqlQuery")) {
                        zkSpecsByDocTypeFromDcql(Json.parseToJsonElement(data.getValue("dcqlQuery").jsonPrimitive.content) as JsonObject)
                    } else {
                        emptyMap()
                    }
                    val effectiveSpecsByDocType = mergeZkSpecs(zkSpecsByDocType(docRequests), dcqlSpecsByDocType)

                    // EE-RP-003 / plan §4 item 4d (finding F7, review finding 1): the consent screen
                    // names an origin, not a relying party. The readerAuth certificate subject is
                    // shown only when the readerAuth signature check PASSED — the parser populates
                    // the chain even on a failed check, so an ungated CN is attacker-chosen. Trust
                    // validation of a passing chain stays §8.3 scope.
                    val readerSubject = readerAuthSubject(docRequests)
                    // EE-PRO-025 / EE-ZKP-055a: authenticated only when readerAuth both verified and
                    // chained to a trusted access CA. No anchors are configured on this path yet, so
                    // every DC API request counts as unauthenticated: the least-trusted case.
                    val authenticatedByDocType = WalletRequestDispatch.authenticationByDocType(docRequests)

                    val presentationDefinition = toPresentationDefinition(docRequests)

                    val documents = requestDependencies?.documents?.invoke() ?: documentRepository.documents.first()
                    val documentMatches = presentationDefinition.getDocumentMatches(documents)

                    handleMatchResult(
                        documentMatches,
                        PresentationTransport(origin, sessionTranscript, recipientPublicKey),
                        // Keyed by docType: a ZK request for one document must not change how another
                        // is presented. A repeated docType keeps its first doc request's specs only;
                        // the DCQL carrier's specs fill the docTypes whose ISO zkRequest is absent.
                        effectiveSpecsByDocType,
                        ReaderIdentity(readerSubject, authenticatedByDocType)
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Error processing request", e)
                sendEffect { DcEffect.Error(e.message ?: "Unknown error") }
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private data class PresentationTransport(val origin: String, val sessionTranscript: ListElement, val recipientPublicKey: EcPublicKey)

    private data class ReaderIdentity(val subject: String?, val authenticatedByDocType: Map<String, Boolean>)

    private fun matchedCredentials(documentMatches: Pair<List<CredentialClaim>, Match>, match: Match.Matched): List<DcCredential> {
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

        return credentials
    }

    private suspend fun handleMatchResult(
        documentMatches: Pair<List<CredentialClaim>, Match>,
        transport: PresentationTransport,
        zkSystemSpecs: Map<String, List<ZkSystemSpec>>,
        readerIdentity: ReaderIdentity
    ) {
        val (origin, sessionTranscript, recipientPublicKey) = transport
        val authenticatedByDocType = readerIdentity.authenticatedByDocType
        when (val match = documentMatches.second) {
            is Match.NotMatched -> {
                logger.info("No matching credentials found")
                sendEffect { DcEffect.NoMatch }
            }

            is Match.Matched -> {
                val credentials = matchedCredentials(documentMatches, match)

                if (credentials.isEmpty()) {
                    sendEffect { DcEffect.NoMatch }
                    return
                }
                // EE-POA-030/031: refuse before consent. A predicate held false ends like a
                // request with no matching credential: the activity closes as RESULT_CANCELED.
                val predicateDecision = AgePredicatePolicy.decideAll(
                    credentials.map { it.credentialType to it.fields + it.optionalFields }
                )
                predicateDecision.error()?.let { error ->
                    logger.warn("Age predicate rule refused the request: {}", predicateDecision)
                    refuseBeforeConsent(origin, credentials.first { AgePredicatePolicy.isAgeAttestation(it.credentialType) }.credentialType.docType(), error)
                    return
                }
                val lastAgeAttestation = credentials.any {
                    AgePredicatePolicy.isAgeAttestation(it.credentialType) && remaining(it.credentialType) <= 1
                }
                val matchedState = state.value.copy(
                    verifier = origin,
                    requestAuthenticated = authenticatedByDocType.isNotEmpty() && authenticatedByDocType.values.all { it },
                    authenticatedByDocType = authenticatedByDocType,
                    lastAgeAttestation = lastAgeAttestation,
                    // EE-RP-003 (4d): show the readerAuth certificate subject when the
                    // reader authenticated the request; the origin alone stays otherwise.
                    readerSubject = readerIdentity.subject,
                    credentials = credentials,
                    sessionTranscript = sessionTranscript,
                    recipientPublicKey = recipientPublicKey,
                    zkSystemSpecs = zkSystemSpecs,
                    expectedPlainTier = expectedPlainTier(zkSystemSpecs, credentials),
                    // EE-ZKP-042 (plan §8.7): the mso_mdoc_zk carrier puts the issuer's
                    // certificate chain (msoX5chain) into every ZkDocument, so the
                    // verifier sees which issuer stands behind the attestation. The
                    // consent screen says so before the user shares — for a matched
                    // credential whose own docType resolved specs, as expectedPlainTier
                    // is scoped, not for a request that merely named mso_mdoc_zk.
                    issuerDisclosesToVerifier = credentials.any {
                        zkSystemSpecs[it.credentialType.docType().uri].orEmpty().isNotEmpty()
                    }
                )
                // EE-ZKP-055a / EE-POA-013 without a ZK request: no optional-field toggle can make
                // the proof possible, so refuse now rather than after the consent screen.
                val askedForProof = credentials.any {
                    it.credentialType.requiresZkProof() && zkSystemSpecs[it.credentialType.docType().uri].orEmpty().isNotEmpty()
                }
                if (askedForProof || !refusePlainWhenSpecsUnsatisfiable(matchedState)) {
                    setState { matchedState }
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
        return WalletRequestDispatch.agePlain(
            credentials = currentState.credentials,
            policy = WalletRequestDispatch.AgeRequestPolicy(zkSystem != null, currentState.authenticatedByDocType),
            describe = { credential ->
                val specs = currentState.zkSystemSpecs[credential.credentialType.docType().uri].orEmpty()
                WalletRequestDispatch.AgeCredential(
                    credential.credentialType,
                    credential.credentialType.docType().uri,
                    specs.isNotEmpty(),
                    resolveSchemeId(zkSystem, specs, credential.allCheckedFields.size) != null,
                    remaining(credential.credentialType) <= 1
                )
            },
            refuse = { credential, error ->
                logger.warn("Refusing plain fallback for an age attestation on a ZK-capable device: {}", error)
                if (error == AppError.PRESENTATION_NO_MATCHING_CIRCUIT) {
                    refusePlain(currentState.verifier, credential.credentialType.docType(), PresentationTier.PLAIN_NO_MATCHING_CIRCUIT, error)
                } else {
                    refuseBeforeConsent(currentState.verifier, credential.credentialType.docType(), error)
                }
            },
            proceed = {}
        )
    }

    /** A refusal of a presentation that never happened: logged without a tier, nothing shared. */
    private suspend fun refuseBeforeConsent(verifier: String, docType: DocType, error: AppError) {
        RequestRefusalDispatch.dcAge(
            state.value.copy(verifier = verifier, registryRefusal = zkSystem?.registryFailure),
            error,
            { updated -> setState { updated } },
            { emitted -> sendEffect { emitted } }
        )
        logRefusal(verifier, docType, error)
    }

    /** Shows the EE-PRO-013 refusal (F8); the user closes it, which ends as RESULT_CANCELED. */
    private fun refuseProtocol(refusal: ProtocolRefusal) {
        RequestRefusalDispatch.dcProtocol(
            state.value,
            refusal,
            { updated -> setState { updated } },
            { emitted -> sendEffect { emitted } }
        )
    }

    /** Logs the EE-ZKP-051 refusal and shows it; nothing is shared. */
    private suspend fun refusePlain(verifier: String, docType: DocType, tier: PresentationTier, error: AppError) {
        RequestRefusalDispatch.dcAge(
            state.value.copy(registryRefusal = zkSystem?.registryFailure),
            error,
            { updated -> setState { updated } },
            { emitted -> sendEffect { emitted } }
        )
        logRefusal(verifier, docType, error, tier)
    }

    private fun PresentationDefinition.getDocumentMatches(
        documents: List<CredentialDocument>
    ): Pair<List<CredentialClaim>, Match> =
        requestDependencies?.matches?.invoke(this, documents) ?: openId4VPManager.matches(this, documents)

    private suspend fun remaining(type: CredentialType): Int =
        requestDependencies?.remaining?.invoke(type) ?: eePoaConsumption.remaining(type)

    private suspend fun logRefusal(party: String, docType: DocType, error: AppError, tier: PresentationTier? = null) {
        val dependencies = requestDependencies
        if (dependencies != null) {
            dependencies.logRefusal(party, docType, error, tier)
        } else {
            transactionLogRepository.addTransactionLog(party = party, docType = docType, error = error, tier = tier)
        }
    }

    private fun getSessionTranscript(encryptionInfoBase64: String, origin: String): ListElement {
        requestDependencies?.let { return it.transcript(encryptionInfoBase64, origin) }
        return ListElement(
            listOf(
                NullElement(),
                NullElement(),
                generateDCApiHandover(encryptionInfoBase64, origin)
            )
        )
    }

    private fun getRecipientPublicKey(encryptionInfoBase64: String): EcPublicKey {
        requestDependencies?.let { return it.recipientKey(encryptionInfoBase64) }
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
        if (state.value.protocolRefusal != null || state.value.plainRefusal != null) return
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

            // Plan §8.7: the mso_mdoc_zk path signs DeviceAuthentication with ECDSA only —
            // Longfellow binds the proof to the session transcript and does not work with
            // MAC-based device auth. The SecureArea device signing is EC by construction
            // (deviceCryptoProvider requires KeyType.EC and SecureAreaCOSECryptoProvider emits
            // ES256 COSE_Sign1); a non-EC attestation is refused for every credential before the
            // first one is signed, rather than answered with a MAC the verifier's ZK check cannot
            // consume.
            currentState.credentials.firstOrNull { it.attestation.keyAttestation.keyType != KeyType.EC }?.let {
                logger.error(
                    "Device auth is ECDSA-only (mso_mdoc_zk requires it); key {} is {}",
                    it.attestation.keyAttestation.keyId,
                    it.attestation.keyAttestation.keyType
                )
                throw IllegalStateException("Device authentication requires an EC device key")
            }

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

            val dependencies = requestDependencies
            if (dependencies != null) {
                dependencies.releaseResponse(response)
            } else {
                sendEffect { DcEffect.SendResponse(response) }
            }
        } catch (e: CancellationException) {
            throw e
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
            Add(
                CBORObject.NewMap().apply {
                    Add("enc", enc)
                    Add("cipherText", cipherText)
                }
            )
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
    val plainRefusal: AppError? = null,
    val registryRefusal: String? = null,
    // EE-PRO-025 / EE-ZKP-055a: readerAuth verified and chained to a trusted access CA.
    val requestAuthenticated: Boolean = false,
    val authenticatedByDocType: @RawValue Map<String, Boolean> = emptyMap(),
    // EE-POA-013: an age attestation in this request is the last of its batch; shown with the
    // EE-ZKP-042 notice when it would go out plain.
    val lastAgeAttestation: Boolean = false,
    // EE-PRO-013 (4a, F8): the request's protocol list was refused; the screen shows this reason.
    val protocolRefusal: ProtocolRefusal? = null,
    // EE-ZKP-042 (plan §8.7): true when the response would carry the issuer's certificate chain
    // (msoX5chain) — the mso_mdoc_zk carrier puts it into every ZkDocument — so the consent
    // screen surfaces the issuer-disclosure consequence in plain terms before the user shares.
    val issuerDisclosesToVerifier: Boolean = false
) : ViewState, Parcelable

sealed class DcEffect : ViewSideEffect {
    data class SendResponse(val response: String) : DcEffect()
    data object Cancel : DcEffect()
    data object NoMatch : DcEffect()
    data class Error(val message: String) : DcEffect()

    /**
     * A refusal with a user-facing, localized reason (F8), set in [DcUiState.protocolRefusal]:
     * the activity stays open so the screen shows it instead of folding the case into a silent
     * cancellation. The wire-level exception mapping stays the record's open item.
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
