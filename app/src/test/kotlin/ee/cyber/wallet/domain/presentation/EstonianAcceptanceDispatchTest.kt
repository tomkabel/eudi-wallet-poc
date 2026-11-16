package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.ui.screens.dcapi.DcEffect
import ee.cyber.wallet.ui.screens.dcapi.DcUiState
import ee.cyber.wallet.ui.screens.presentation.RequestRefusalDispatch
import ee.cyber.wallet.ui.screens.proximity.Effect
import ee.cyber.wallet.ui.screens.proximity.UiState
import ee.cyber.wallet.util.DeviceRequestParser
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.RequestProcessor
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Wire/transfer controls for the same dispatch used by both production ViewModel entries. */
class EstonianAcceptanceDispatchTest {
    private val transcript = Cbor.encode(buildCborMap { })

    private fun unsignedRequest(): ByteArray = Cbor.encode(
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

    private fun dcRequest(bytes: ByteArray): String = buildJsonObject {
        putJsonArray("requests") {
            addJsonObject {
                put("protocol", "org-iso-mdoc")
                putJsonObject("data") {
                    put("deviceRequest", Base64.getUrlEncoder().withoutPadding().encodeToString(bytes))
                    put("encryptionInfo", "transcript-supplied-at-transport-seam")
                }
            }
        }
    }.toString()

    private fun age() = WalletRequestDispatch.AgeCredential(
        CredentialType.AGE_VERIFICATION,
        DocType.AGE_VERIFICATION.uri,
        proofRequested = false,
        satisfiable = false,
        lastAttestation = false
    )

    private fun record(caseId: String, reason: String) {
        println(
            "ESTONIAN_ACCEPTANCE=" + buildJsonObject {
                put("case_id", caseId)
                put("outcome", reason)
                put("requests", 1)
                put("responses", 0)
                put("refused", true)
                put("evidence_kind", "production-dispatch")
            }
        )
    }

    @Test
    fun unsignedOpenIdDcApiRefusesUnsupportedProtocolBeforePayloadOrResponse() = runTest {
        var state = DcUiState()
        val effects = mutableListOf<DcEffect>()
        var responses = 0
        var transcripts = 0
        // Unsupported OpenID never enters the ISO parser or Tier-2 policy.
        val wire = buildJsonObject {
            putJsonArray("requests") {
                addJsonObject {
                    put("protocol", "openid4vp")
                    putJsonObject("data") {
                        put("client_id", "https://rp.example")
                        put("response_mode", "dc_api")
                        putJsonObject("dcql_query") {
                            putJsonArray("credentials") {
                                addJsonObject {
                                    put("id", "av")
                                    put("format", "mso_mdoc")
                                    putJsonObject("meta") { put("doctype_value", DocType.AGE_VERIFICATION.uri) }
                                }
                            }
                        }
                    }
                }
            }
        }.toString()
        WalletRequestDispatch.dcApi(wire, {
            transcripts++
            error("unsupported payload parsed")
        }, {
            RequestRefusalDispatch.dcProtocol(state, it, { state = it }, { effects += it })
        }) { responses++ }
        assertEquals(ProtocolRefusal.UNSUPPORTED_PROTOCOL, state.protocolRefusal)
        assertEquals(listOf<DcEffect>(DcEffect.Refused(ProtocolRefusal.UNSUPPORTED_PROTOCOL)), effects)
        assertNull(state.plainRefusal)
        assertEquals(0, transcripts)
        assertEquals(0, responses)
        record("unattested-openid-dcapi", "UNSUPPORTED_PROTOCOL")
    }

    private suspend fun iso(bytes: ByteArray, capable: Boolean): Pair<DcUiState, List<DcEffect>> {
        var state = DcUiState()
        val effects = mutableListOf<DcEffect>()
        var responses = 0
        WalletRequestDispatch.dcApi(dcRequest(bytes), { transcript }, {
            RequestRefusalDispatch.dcProtocol(state, it, { state = it }, { effects += it })
        }) { request ->
            val auth = WalletRequestDispatch.authenticationByDocType(request.docRequests)
            assertEquals(false, auth[DocType.AGE_VERIFICATION.uri])
            WalletRequestDispatch.agePlain(listOf(age()), WalletRequestDispatch.AgeRequestPolicy(capable, auth), { it }, { _, reason ->
                RequestRefusalDispatch.dcAge(state, reason, { state = it }, { effects += it })
            }) { responses++ }
        }
        assertEquals(if (capable) 0 else 1, responses)
        return state to effects
    }

    @Test
    fun unauthenticatedIsoMdocPlainOnlyRefusesBeforeResponse() = runTest {
        val (state, effects) = iso(unsignedRequest(), capable = true)
        assertEquals(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST, state.plainRefusal)
        assertEquals(listOf<DcEffect>(DcEffect.RefusedPlainFallback), effects)
        record("unauthenticated-iso-mdoc", state.plainRefusal!!.name)
    }

    @Test
    fun incapableIsoMdocControlReachesResponseContinuation() = runTest {
        val (state, effects) = iso(unsignedRequest(), capable = false)
        assertNull(state.plainRefusal)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun signatureValidUntrustedIsoMdocStillRefusesBeforeResponse() = runTest {
        val bytes = AcceptanceReaderAuthFixture().signedUntrusted()
        val parsed = DeviceRequestParser(bytes, transcript).parse().docRequests.single()
        assertTrue(parsed.readerAuthenticated)
        assertFalse(parsed.readerChainTrusted)
        val (state, effects) = iso(bytes, capable = true)
        assertEquals(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST, state.plainRefusal)
        assertEquals(listOf<DcEffect>(DcEffect.RefusedPlainFallback), effects)
    }

    private suspend fun proximity(bytes: ByteArray, capable: Boolean): Pair<UiState, List<Effect>> {
        var state = UiState(sessionTranscript = byteArrayOf(1, 2, 3))
        val effects = mutableListOf<Effect>()
        val stops = mutableListOf<Boolean>()
        var responses = 0
        val request = DeviceRequest(bytes, transcript)
        val processed = RequestProcessor.ProcessedRequest.Failure(IllegalStateException("match supplied at seam"))
        WalletRequestDispatch.proximity(TransferEvent.RequestReceived(processed, request)) { _, parsed ->
            WalletRequestDispatch.agePlain(
                listOf(age()),
                WalletRequestDispatch.AgeRequestPolicy(capable, WalletRequestDispatch.authenticationByDocType(parsed)),
                { it },
                { _, reason ->
                    RequestRefusalDispatch.proximityAge(state, reason, { state = it }, { stops += it }, { effects += it })
                }
            ) { responses++ }
        }
        assertEquals(if (capable) 0 else 1, responses)
        assertEquals(if (capable) listOf(true) else emptyList(), stops)
        if (capable) assertNull(state.sessionTranscript, "refusal clears any previous engagement response state")
        return state to effects
    }

    @Test
    fun unauthenticatedProximityTransferRefusesAndStopsBeforeResponse() = runTest {
        val (state, effects) = proximity(unsignedRequest(), capable = true)
        assertEquals(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST, state.refusal)
        assertEquals(listOf<Effect>(Effect.ProximityRequestNoMatch), effects)
        record("unauthenticated-proximity", state.refusal!!.name)
    }

    @Test
    fun incapableProximityControlReachesResponseContinuation() = runTest {
        val (state, effects) = proximity(unsignedRequest(), capable = false)
        assertNull(state.refusal)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun signatureValidUntrustedProximityStillRefusesAndStops() = runTest {
        val bytes = AcceptanceReaderAuthFixture().signedUntrusted()
        val parsed = DeviceRequestParser(bytes, transcript).parse().docRequests.single()
        assertTrue(parsed.readerAuthenticated)
        assertFalse(parsed.readerChainTrusted)
        val (state, effects) = proximity(bytes, capable = true)
        assertEquals(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST, state.refusal)
        assertEquals(listOf<Effect>(Effect.ProximityRequestNoMatch), effects)
    }

    @Test
    fun anotherAuthenticatedDocumentCannotAuthenticateUnsignedAgeRequest() = runTest {
        val unsigned = DeviceRequestParser(unsignedRequest(), transcript).parse().docRequests.single()
        val signedOther = DeviceRequestParser.DocRequest.Builder(
            DocType.PID.uri,
            byteArrayOf(),
            emptyMap(),
            byteArrayOf(),
            null,
            readerAuthenticated = true,
            readerChainTrusted = true
        ).build()
        val auth = WalletRequestDispatch.authenticationByDocType(listOf(signedOther, unsigned))
        assertEquals(mapOf(DocType.PID.uri to true, DocType.AGE_VERIFICATION.uri to false), auth)
        var refused: AppError? = null
        var responses = 0
        WalletRequestDispatch.agePlain(
            listOf(age()),
            WalletRequestDispatch.AgeRequestPolicy(true, auth),
            { it },
            { _, reason -> refused = reason }
        ) { responses++ }
        assertEquals(AppError.PRESENTATION_UNAUTHENTICATED_REQUEST, refused)
        assertEquals(0, responses)
    }
}
