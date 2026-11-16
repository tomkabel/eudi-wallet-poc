package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.util.DeviceRequestParser
import eu.europa.ec.eudi.iso18013.transfer.TransferEvent
import eu.europa.ec.eudi.iso18013.transfer.response.device.DeviceRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.util.fromBase64Url

/** The production wire dispatch boundary, shared by the provider/transfer entry and JVM controls. */
internal object WalletRequestDispatch {
    data class DcRequest(
        val data: JsonObject,
        val encryptionInfo: String,
        val docRequests: List<DeviceRequestParser.DocRequest>
    )

    suspend fun dcApi(
        requestJson: String,
        sessionTranscript: (String) -> ByteArray,
        refuse: (ProtocolRefusal) -> Unit,
        proceed: suspend (DcRequest) -> Unit
    ) {
        val entries = Json.parseToJsonElement(requestJson).jsonObject.getValue("requests").jsonArray
        val names = entries.map { it.jsonObject["protocol"]?.jsonPrimitive?.content }
        val selected = when (val decision = DcApiRequestDispatch.dispatch(names)) {
            is DcApiRequestDispatch.Decision.Take -> decision.index
            DcApiRequestDispatch.Decision.Empty -> {
                refuse(ProtocolRefusal.NO_REQUEST)
                return
            }
            is DcApiRequestDispatch.Decision.Unsupported -> {
                refuse(ProtocolRefusal.UNSUPPORTED_PROTOCOL)
                return
            }
        }
        val data = entries[selected].jsonObject.getValue("data").jsonObject
        val encryptionInfo = data.getValue("encryptionInfo").jsonPrimitive.content
        val parsed = DeviceRequestParser(
            data.getValue("deviceRequest").jsonPrimitive.content.fromBase64Url(),
            sessionTranscript(encryptionInfo)
        ).parse().docRequests
        proceed(DcRequest(data, encryptionInfo, parsed))
    }

    /** A signature on a different doc request cannot authenticate this document. First docType wins. */
    fun authenticationByDocType(requests: List<DeviceRequestParser.DocRequest>): Map<String, Boolean> =
        requests.fold(emptyMap()) { found, request ->
            if (request.docType in found) {
                found
            } else {
                found +
                    (request.docType to (request.readerAuthenticated && request.readerChainTrusted))
            }
        }

    suspend fun proximity(
        event: TransferEvent.RequestReceived,
        proceed: suspend (DeviceRequest, List<DeviceRequestParser.DocRequest>) -> Unit
    ) {
        val request = event.request as DeviceRequest
        val parsed = DeviceRequestParser(request.deviceRequestBytes, request.sessionTranscriptBytes).parse().docRequests
        proceed(request, parsed)
    }

    data class AgeCredential(
        val type: CredentialType,
        val docType: String,
        val proofRequested: Boolean,
        val satisfiable: Boolean,
        val lastAttestation: Boolean
    )

    data class AgeRequestPolicy(val zkCapable: Boolean, val authenticationByDocType: Map<String, Boolean>)

    /** Refuse before the caller can reach signing/response dispatch; callbacks are the VM effects. */
    suspend fun <T> agePlain(
        credentials: List<T>,
        policy: AgeRequestPolicy,
        describe: suspend (T) -> AgeCredential,
        refuse: suspend (T, AppError) -> Unit,
        proceed: suspend () -> Unit
    ): Boolean {
        for (credential in credentials) {
            val age = describe(credential)
            if (!AgePredicatePolicy.isAgeAttestation(age.type)) continue
            val authenticated = policy.authenticationByDocType[age.docType] == true
            if (HolderObligations.refusePlainFallback(
                    policy.zkCapable,
                    age.proofRequested,
                    age.satisfiable,
                    authenticated,
                    age.lastAttestation
                )
            ) {
                refuse(credential, HolderObligations.plainRefusalReason(age.proofRequested, authenticated))
                return true
            }
        }
        proceed()
        return false
    }
}
