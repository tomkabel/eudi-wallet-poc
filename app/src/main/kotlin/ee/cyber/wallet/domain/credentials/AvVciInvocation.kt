package ee.cyber.wallet.domain.credentials

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.CodingErrorAction

/** An external offer is entry data only. It is never a request to mint a mock credential. */
data class AvVciOffer(val credentialIssuer: String) {
    val docType: String get() = "eu.europa.ec.av.1"
}

enum class InvocationRefusalReason { MISSING, MALFORMED, UNSUPPORTED, NAVIGATION, CAPACITY }

data class InvocationRefusal(val parameter: String, val reason: InvocationRefusalReason, val refusedCount: Int = 1)

sealed interface WalletInvocation {
    data class AvIssuance(val offer: AvVciOffer) : WalletInvocation
    data class ExistingLink(val uri: String) : WalletInvocation
    data class Refused(val refusal: InvocationRefusal) : WalletInvocation
}

/** Pure ingress parser: split raw components first, then decode each query component exactly once. */
object AvVciInvocation {
    private val json = ObjectMapper(JsonFactory.builder()
        .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(64).build())
        .build())
        .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    private val existingSchemes = setOf(
        "haip", "haip-vp", "openid4vp", "eudi-openid4vp", "mdoc-openid4vp",
        "mdl-openid4vp", "eudi-wallet", "av", "https"
    )

    fun parse(raw: String): WalletInvocation {
        val scheme = raw.substringBefore(':', "").lowercase()
        if (scheme != "av-vci") {
            return if (scheme in existingSchemes) WalletInvocation.ExistingLink(raw) else refused("scheme", InvocationRefusalReason.UNSUPPORTED)
        }
        val body = raw.substringAfter(':')
        if (!body.startsWith("//") || '#' in body) return refused("invocation", InvocationRefusalReason.UNSUPPORTED)
        val location = body.substringBefore('?').removePrefix("//")
        if (location != "" && location != "/") return refused("invocation", InvocationRefusalReason.UNSUPPORTED)
        if ('?' !in body || body.substringAfter('?').isEmpty()) return refused("credential_offer", InvocationRefusalReason.MISSING)
        if (raw.length > 65536) return refused("credential_offer", InvocationRefusalReason.MALFORMED)
        val inputs = mutableMapOf<String, String>()
        for (pair in body.substringAfter('?').split('&')) {
            val name = try {
                decode(pair.substringBefore('='))
            } catch (_: IllegalArgumentException) {
                return refused("query", InvocationRefusalReason.MALFORMED)
            }
            if (name != "credential_offer") {
                // Do not echo arbitrary query values, grant codes or unbounded attacker-controlled names.
                val safeName = name.takeIf { it.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]{0,63}")) } ?: "query"
                return refused(safeName, InvocationRefusalReason.UNSUPPORTED)
            }
            if (name in inputs) return refused(name, InvocationRefusalReason.MALFORMED)
            inputs[name] = try {
                decode(pair.substringAfter('=', ""))
            } catch (_: IllegalArgumentException) {
                return refused(name, InvocationRefusalReason.MALFORMED)
            }
        }
        val inline = inputs["credential_offer"].orEmpty()
        if (inline.isEmpty()) return refused("credential_offer", InvocationRefusalReason.MISSING)
        val offer = try {
            json.readTree(inline)
        } catch (_: Exception) {
            return refused("credential_offer", InvocationRefusalReason.MALFORMED)
        }
        if (offer == null || !offer.isObject) return refused("credential_offer", InvocationRefusalReason.MALFORMED)
        val issuer = offer.get("credential_issuer")
        if (issuer == null || !issuer.isTextual || !validIssuer(issuer.textValue())) {
            return refused("credential_issuer", InvocationRefusalReason.MALFORMED)
        }
        val configurations = offer.get("credential_configuration_ids")
        if (configurations == null || !configurations.isArray || configurations.size() != 1 ||
            !configurations[0].isTextual || configurations[0].textValue() != "proof_of_age"
        ) return refused("credential_configuration_ids", InvocationRefusalReason.MALFORMED)
        val grants = offer.get("grants")
        if (grants != null && !grants.isObject) return refused("grants", InvocationRefusalReason.MALFORMED)
        // Public grants and extension fields are accepted as inert JSON; never log, display or execute them.
        if (!validStrings(offer)) return refused("credential_offer", InvocationRefusalReason.MALFORMED)
        return WalletInvocation.AvIssuance(AvVciOffer(issuer.textValue()))
    }

    private fun validIssuer(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrEmpty() &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            uri.rawAuthority?.endsWith(':') != true && (uri.port == -1 || uri.port in 1..65535) &&
            value.none { it.code in 0x202A..0x202E || it.code in 0x2066..0x2069 || it.code == 0x061C || it.code == 0x200E || it.code == 0x200F }
    } catch (_: Exception) {
        false
    }

    private fun validStrings(node: JsonNode): Boolean {
        if (node.isTextual) return runCatching { utf8Bytes(node.textValue()) }.isSuccess
        if (node.isObject) return node.fields().asSequence().all { (name, child) ->
            runCatching { utf8Bytes(name) }.isSuccess && validStrings(child)
        }
        return !node.isContainerNode || node.all { validStrings(it) }
    }

    private fun utf8Bytes(value: String): ByteArray {
        val buffer = Charsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value))
        return ByteArray(buffer.remaining()).also { buffer.get(it) }
    }

    private fun asciiHex(char: Char): Int? = when (char) {
        in '0'..'9' -> char - '0'
        in 'A'..'F' -> char - 'A' + 10
        in 'a'..'f' -> char - 'a' + 10
        else -> null
    }

    private fun decode(component: String): String = try {
        val bytes = ByteArrayOutputStream()
        var index = 0
        while (index < component.length) {
            when (component[index]) {
                '%' -> {
                    require(index + 2 < component.length)
                    val high = asciiHex(component[index + 1])
                    val low = asciiHex(component[index + 2])
                    require(high != null && low != null)
                    bytes.write(high * 16 + low)
                    index += 3
                }
                '+' -> {
                    bytes.write(32)
                    index++
                }
                else -> {
                    val end = component.indexOfAny(charArrayOf('%', '+'), index).takeIf { it >= 0 } ?: component.length
                    bytes.write(utf8Bytes(component.substring(index, end)))
                    index = end
                }
            }
        }
        Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString()
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid query encoding", e)
    }

    private fun refused(parameter: String, reason: InvocationRefusalReason) = WalletInvocation.Refused(InvocationRefusal(parameter, reason))
}

/** The same callbacks are exercised by JVM tests and used by the Android loaded-app ingress. */
fun dispatchWalletInvocation(
    raw: String,
    enterIssuance: (AvVciOffer) -> Unit,
    navigate: (String) -> Unit,
    refuse: (InvocationRefusal) -> Unit
) {
    when (val invocation = AvVciInvocation.parse(raw)) {
        is WalletInvocation.AvIssuance -> enterIssuance(invocation.offer)
        is WalletInvocation.Refused -> refuse(invocation.refusal)
        is WalletInvocation.ExistingLink -> try {
            navigate(invocation.uri)
        } catch (_: Exception) {
            refuse(InvocationRefusal("invocation", InvocationRefusalReason.NAVIGATION))
        }
    }
}
