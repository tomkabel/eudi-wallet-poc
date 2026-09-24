package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.R

/**
 * EE-PRO-013 / conformance plan §4 item 4a: the DC API request protocols this wallet answers.
 *
 * A `Digital Credentials` request arrives as JSON with a `requests` array; every entry names the
 * protocol its `data` payload speaks. Chrome currently offers `org-iso-mdoc` (ISO/IEC 18013-7
 * Annex C, HPKE-encrypted `DeviceRequest` / `DeviceResponse`), which is the only protocol this
 * fork can answer — OpenID4VP over the DC API is §8.2 future work and must also be refused, not
 * parsed. Anything else is refused with [UNSUPPORTED_PROTOCOL] instead of crashing in the payload
 * parser with a `JSONException`.
 */
enum class DcApiProtocol(val wireName: String) {
    ISO_MDOC("org-iso-mdoc");

    companion object {
        fun fromWireName(name: String?): DcApiProtocol? = entries.firstOrNull { it.wireName == name }
    }
}

/**
 * Pure decision core for the protocol walk over `requests[]`: the first entry whose protocol the
 * wallet supports is taken; entries the wallet does not support are refused with a specific,
 * distinguishable error (plan finding F8) instead of an unhandled parser exception.
 *
 * Kept free of Android and JSON types so the walk is unit-testable on the JVM; the view model
 * parses `requests[]` and delegates here.
 */
object DcApiRequestDispatch {

    /** What the walk decided to do with a request list. */
    sealed class Decision {
        /** The index of the first supported entry and the protocol it speaks. */
        data class Take(val index: Int, val protocol: DcApiProtocol) : Decision()

        /** Every entry named a protocol this wallet does not answer; the last name is reported. */
        data class Unsupported(val protocolName: String?) : Decision()

        /** No entries at all — also not a parser crash. */
        data object Empty : Decision()
    }

    /**
     * Walk `requests[]` and take the first supported entry, per plan §4 item 4a. `protocolNames`
     * is the `protocol` field of each entry, in order; null means the field is absent, which is
     * also unsupported (a wallet must not guess a protocol from the payload bytes).
     */
    fun dispatch(protocolNames: List<String?>): Decision = when {
        protocolNames.isEmpty() -> Decision.Empty
        else -> {
            val firstSupported = protocolNames.withIndex().firstOrNull { (index, _) ->
                DcApiProtocol.fromWireName(protocolNames[index]) != null
            }
            if (firstSupported != null) {
                Decision.Take(firstSupported.index, DcApiProtocol.fromWireName(protocolNames[firstSupported.index])!!)
            } else {
                Decision.Unsupported(protocolNames.firstOrNull())
            }
        }
    }
}

/**
 * The app error the unsupported-protocol refusal maps to. Distinct from the ZK circuit refusal
 * (EE-ZKP-051) and from the generic unsupported-request error so the relying party can tell them
 * apart (F8).
 */
enum class ProtocolRefusal(val messageRes: Int) {
    UNSUPPORTED_PROTOCOL(R.string.error_presentation_unsupported_protocol)
}
