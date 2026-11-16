package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.credentials.Namespace
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.documents.DocumentField
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import ee.cyber.wallet.ui.screens.dcapi.DcEffect
import ee.cyber.wallet.ui.screens.dcapi.DcEvent
import ee.cyber.wallet.ui.screens.dcapi.DigitalCredentialsViewModel
import ee.cyber.wallet.ui.screens.proximity.Effect
import ee.cyber.wallet.ui.screens.proximity.Event
import ee.cyber.wallet.ui.screens.proximity.ProximityViewModel
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import eu.europa.ec.eudi.prex.Format
import eu.europa.ec.eudi.prex.Match
import eu.europa.ec.eudi.prex.PresentationDefinition
import eu.europa.ec.eudi.prex.PresentationExchange
import id.walt.mdoc.cose.COSESign1
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.dataelement.StringElement
import id.walt.mdoc.doc.MDoc
import id.walt.mdoc.issuersigned.IssuerSigned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.request.RequestedClaim
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/** Actual VM orchestration: wire entry, matcher, capability, retained refusal and release boundary. */
class EstonianViewModelAcceptanceTest {
    private val transcript = ListElement(listOf(MapElement(emptyMap())))
    private val recipientKey = runBlocking { Crypto.createEcPrivateKey(EcCurve.P256).publicKey }

    private class Capable : ZkSystem {
        override val name = "acceptance-native-capable"
        override val systemSpecs = emptyList<ZkSystemSpec>()
        override fun getMatchingSystemSpec(zkSystemSpecs: List<ZkSystemSpec>, requestedClaims: List<RequestedClaim>): ZkSystemSpec? = null
        override fun generateProof(
            zkSystemSpec: ZkSystemSpec,
            document: MdocDocument,
            sessionTranscript: DataItem,
            timestamp: Instant
        ): ZkDocument = error("unexpected proof release")
        override fun verifyProof(zkDocument: ZkDocument, zkSystemSpec: ZkSystemSpec, sessionTranscript: DataItem) = error("not a verifier")
    }

    private fun document() = CredentialDocument.MDocDocument(
        "av-1",
        DocType.AGE_VERIFICATION,
        listOf(DocumentField(Namespace.EU_EUROPA_EC_EUDI_AGE_VERIFICATION_1, "age_over_18", "true")),
        false,
        Attestation("av-1", "{}", CredentialType.AGE_VERIFICATION, KeyAttestation("key", "{}", KeyType.EC)),
        mDoc = MDoc(StringElement(DocType.AGE_VERIFICATION.uri), IssuerSigned(emptyMap(), COSESign1()), deviceSigned = null)
    )

    private fun matching(definition: PresentationDefinition, documents: List<CredentialDocument>): Pair<List<CredentialClaim>, Match> {
        val claims = documents.map { document ->
            object : CredentialClaim {
                override val uniqueId = document.id
                override val credentialDocument = document
                override val format = Format.format(buildJsonObject {})
                override fun asJsonString() = """{"eu.europa.ec.av.1":{"age_over_18":"true"}}"""
            }
        }
        return claims to PresentationExchange.matcher.match(definition, claims)
    }

    private fun wire(): ByteArray = Cbor.encode(
        buildCborMap {
            put("version", "1.0")
            put(
                "docRequests",
                buildCborArray {
                    add(
                        buildCborMap {
                            put(
                                "itemsRequest",
                                Tagged(
                                    24,
                                    Bstr(
                                        Cbor.encode(
                                            buildCborMap {
                                                put("docType", DocType.AGE_VERIFICATION.uri)
                                                put(
                                                    "nameSpaces",
                                                    buildCborMap {
                                                        put(DocType.AGE_VERIFICATION.uri, buildCborMap { put("age_over_18", false) })
                                                    }
                                                )
                                            }
                                        )
                                    )
                                )
                            )
                        }
                    )
                }
            )
        }
    )

    private fun json(bytes: ByteArray) = """{"requests":[{"protocol":"org-iso-mdoc","data":{"deviceRequest":"${Base64.getUrlEncoder().withoutPadding().encodeToString(
        bytes
    )}","encryptionInfo":"transport-fixture"}}]}"""

    private fun capableWithMissingRegistry() = ZkCapability.create(
        native = { Capable() },
        registry = { Result.failure(IllegalArgumentException("missing-local-list")) }
    )

    private fun record(caseId: String, outcome: String, matches: Int) {
        println(
            "ESTONIAN_ACCEPTANCE=" + buildJsonObject {
                put("case_id", caseId)
                put("outcome", outcome)
                put("requests", 1)
                put("responses", 0)
                put("refused", true)
                put("evidence_kind", "production-viewmodel")
                put("matched_requests", matches)
                put("consent_released", false)
            }
        )
    }

    @Test
    fun dcApiVmMatchesThenRefusesBeforeConsentAndIgnoresShare() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            for (signed in listOf(false, true)) {
                var matches = 0
                var releases = 0
                val logs = mutableListOf<AppError>()
                val vm = DigitalCredentialsViewModel(
                    DigitalCredentialsViewModel.RequestDependencies(
                        documents = { listOf(document()) },
                        matches = { definition, docs ->
                            matches++
                            matching(definition, docs)
                        },
                        zkSystem = { capableWithMissingRegistry() },
                        remaining = { 2 },
                        logRefusal = { _, _, reason, _ -> logs += reason },
                        transcript = { _, _ -> transcript },
                        recipientKey = { recipientKey },
                        releaseResponse = { releases++ }
                    )
                )
                val effects = mutableListOf<DcEffect>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects += it } }
                val bytes = if (signed) AcceptanceReaderAuthFixture().signedUntrusted(transcript.toCBOR()) else wire()
                vm.processRequest(DigitalCredentialsViewModel.ProviderRequest(json(bytes), "https://rp.example"))
                advanceUntilIdle()
                assertEquals(1, matches)
                assertEquals(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST, vm.state.value.plainRefusal)
                assertEquals("missing-local-list", vm.state.value.registryRefusal)
                assertTrue(vm.state.value.credentials.isEmpty(), "refusal never publishes consent credentials")
                assertNull(vm.state.value.sessionTranscript)
                assertFalse(vm.state.value.isLoading)
                assertEquals(listOf(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST), logs)
                assertEquals(listOf<DcEffect>(DcEffect.RefusedPlainFallback), effects)
                vm.handleEvents(DcEvent.OnShareClicked)
                advanceUntilIdle()
                assertEquals(0, releases)
                assertFalse(effects.any { it is DcEffect.SendResponse })
                if (!signed) record("unauthenticated-iso-mdoc", vm.state.value.plainRefusal!!.name, matches)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun dcApiVmUnsupportedOpenIdNeverMatchesOrReleases() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            var matches = 0
            var releases = 0
            val vm = DigitalCredentialsViewModel(
                DigitalCredentialsViewModel.RequestDependencies(
                    documents = { error("unsupported protocol must not load documents") },
                    matches = { definition, docs ->
                        matches++
                        matching(definition, docs)
                    },
                    zkSystem = { Capable() },
                    remaining = { 2 },
                    logRefusal = { _, _, _, _ -> },
                    transcript = { _, _ -> error("unsupported protocol must not parse ISO") },
                    recipientKey = { recipientKey },
                    releaseResponse = { releases++ }
                )
            )
            val effects = mutableListOf<DcEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects += it } }
            vm.processRequest(
                DigitalCredentialsViewModel.ProviderRequest(
                    """{"requests":[{"protocol":"openid4vp","data":{"response_mode":"dc_api","dcql_query":{"credentials":[{"format":"mso_mdoc"}]}}}]}""",
                    "https://rp.example"
                )
            )
            advanceUntilIdle()
            vm.handleEvents(DcEvent.OnShareClicked)
            assertEquals(ProtocolRefusal.UNSUPPORTED_PROTOCOL, vm.state.value.protocolRefusal)
            assertEquals(listOf<DcEffect>(DcEffect.Refused(ProtocolRefusal.UNSUPPORTED_PROTOCOL)), effects)
            assertEquals(0, matches)
            assertEquals(0, releases)
            assertTrue(vm.state.value.credentials.isEmpty())
            record("unattested-openid-dcapi", vm.state.value.protocolRefusal!!.name, matches)
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun proximityVmTransferMatchesThenStopsBeforeConsentOrPinRelease() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            for (signed in listOf(false, true)) {
                var listener: ((TransferEvent) -> Unit)? = null
                var matches = 0
                var sends = 0
                val stops = mutableListOf<Boolean>()
                val logs = mutableListOf<AppError>()
                val vm = ProximityViewModel(
                    ProximityViewModel.RequestDependencies(
                        documents = { listOf(document()) },
                        matches = { definition, docs ->
                            matches++
                            matching(definition, docs)
                        },
                        capable = { capableWithMissingRegistry() != null },
                        remaining = { 2 },
                        logRefusal = { _, error -> logs += error },
                        listen = { listener = it },
                        stop = { stops += it },
                        send = { sends++ }
                    )
                )
                val effects = mutableListOf<Effect>()
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.effect.collect { effects += it } }
                val bytes = if (signed) AcceptanceReaderAuthFixture().signedUntrusted(transcript.toCBOR()) else wire()
                listener!!(
                    TransferEvent.RequestReceived(
                        RequestProcessor.ProcessedRequest.Failure(IllegalStateException("unused upstream match")),
                        DeviceRequest(bytes, transcript.toCBOR())
                    )
                )
                advanceUntilIdle()
                assertEquals(1, matches)
                assertEquals(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST, vm.state.value.refusal)
                assertEquals(listOf(true), stops)
                assertEquals(listOf(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST), logs)
                assertEquals(listOf<Effect>(Effect.ProximityRequestNoMatch), effects)
                if (!signed) record("unauthenticated-proximity", vm.state.value.refusal!!.name, matches)
                assertTrue(vm.state.value.credentials.isEmpty())
                assertNull(vm.state.value.sessionTranscript)
                vm.handleEvents(Event.UserAuthenticated)
                vm.handleEvents(Event.OnShareClicked)
                advanceUntilIdle()
                assertEquals(0, sends)
                assertEquals(listOf<Effect>(Effect.ProximityRequestNoMatch), effects)
            }
        } finally { Dispatchers.resetMain() }
    }

    @Test
    fun incapableVmControlsMatchAndReachConsentWithoutRefusal() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val incapable = ZkCapability.create(native = { error("native unavailable") }, registry = { error("must not load registry") })
            assertNull(incapable)
            var matches = 0
            val dc = DigitalCredentialsViewModel(
                DigitalCredentialsViewModel.RequestDependencies(
                    documents = { listOf(document()) },
                    matches = { definition, docs ->
                        matches++
                        matching(definition, docs)
                    },
                    zkSystem = { incapable },
                    remaining = { 2 },
                    logRefusal = { _, _, _, _ -> error("unexpected refusal") },
                    transcript = { _, _ -> transcript },
                    recipientKey = { recipientKey },
                    releaseResponse = { error("unexpected release") }
                )
            )
            dc.processRequest(DigitalCredentialsViewModel.ProviderRequest(json(wire()), "https://rp.example"))
            advanceUntilIdle()
            assertEquals(1, matches)
            assertNull(dc.state.value.plainRefusal)
            assertEquals(1, dc.state.value.credentials.size)
            assertEquals(PresentationTier.PLAIN_NOT_REQUESTED, dc.state.value.expectedPlainTier)
            var listener: ((TransferEvent) -> Unit)? = null
            val proximity = ProximityViewModel(
                ProximityViewModel.RequestDependencies(
                    documents = { listOf(document()) },
                    matches = { definition, docs ->
                        matches++
                        matching(definition, docs)
                    },
                    capable = { incapable != null },
                    remaining = { 2 },
                    logRefusal = { _, _ -> error("unexpected refusal") },
                    listen = { listener = it },
                    stop = { error("unexpected stop") },
                    send = { error("unexpected release") }
                )
            )
            val effects = mutableListOf<Effect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { proximity.effect.collect { effects += it } }
            listener!!(
                TransferEvent.RequestReceived(RequestProcessor.ProcessedRequest.Failure(IllegalStateException("unused upstream match")), DeviceRequest(wire(), transcript.toCBOR()))
            )
            advanceUntilIdle()
            assertEquals(2, matches)
            assertNull(proximity.state.value.refusal)
            assertEquals(1, proximity.state.value.credentials.size)
            assertEquals(listOf<Effect>(Effect.ProximityRequest), effects)
        } finally { Dispatchers.resetMain() }
    }
}
