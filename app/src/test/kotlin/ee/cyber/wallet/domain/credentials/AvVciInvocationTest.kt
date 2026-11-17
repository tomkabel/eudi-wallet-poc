package ee.cyber.wallet.domain.credentials

import java.net.URLEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AvVciInvocationTest {
    private val issuer = "https://issuer.example.com/tenant%26x"
    private fun offer(
        issuerJson: String = "\"$issuer\"",
        configurations: String = "[\"proof_of_age\"]",
        extra: String = ""
    ) = """{"credential_issuer":$issuerJson,"credential_configuration_ids":$configurations$extra}"""
    private fun link(json: String = offer()) = "av-vci://?credential_offer=" + URLEncoder.encode(json, "UTF-8")

    private fun dispatch(raw: String, expectedParameter: String? = null, expectedReason: InvocationRefusalReason = InvocationRefusalReason.MALFORMED): AvVciOffer? {
        val entries = mutableListOf<AvVciOffer>()
        val refusals = mutableListOf<InvocationRefusal>()
        var navigations = 0
        dispatchWalletInvocation(raw, entries::add, { navigations++ }, refusals::add)
        assertEquals(0, navigations, raw)
        if (expectedParameter != null) {
            assertTrue(entries.isEmpty(), "A refused link must never enter issuance")
            assertEquals(InvocationRefusal(expectedParameter, expectedReason), refusals.single(), raw)
        } else {
            assertTrue(refusals.isEmpty())
            assertEquals(1, entries.size)
        }
        return entries.singleOrNull()
    }

    @Test
    fun `valid public offers enter the shared callback without executing grants`() {
        for (extra in listOf(
            "",
            """, "grants":{"authorization_code":{"issuer_state":"secret-state","authorization_server":"https://do-not-contact.invalid"}}""",
            """, "grants":{"urn:ietf:params:oauth:grant-type:pre-authorized_code":{"pre-authorized_code":"secret-code","tx_code":{"length":4,"input_mode":"numeric"}}}"""
        )) {
            val parsed = dispatch(link(offer(extra = extra)))!!
            assertEquals(issuer, parsed.credentialIssuer)
            assertEquals("eu.europa.ec.av.1", parsed.docType)
            assertTrue(!parsed.toString().contains("secret"))
        }
        dispatch(link().replace("av-vci://?", "AV-VCI:///?"))
    }

    @Test
    fun `missing and malformed inline offers are refused before entry`() {
        listOf("av-vci://", "av-vci://?", "av-vci://?credential_offer", "av-vci://?credential_offer=").forEach {
            dispatch(it, "credential_offer", InvocationRefusalReason.MISSING)
        }
        listOf("%", "%GG", "%C0%AF", "%ED%A0%80", "%FF", "%E2%82", "%257B%257D").forEach {
            dispatch("av-vci://?credential_offer=$it", "credential_offer")
        }
        listOf("null", "[]", "42", "true", "\"offer\"", "{", "{}{}", "{} trailing",
            "{unquoted:1}", "{'a':1}", "{\"a\":1,}", "{\"a\":NaN}", "/* comment */{}",
            """{"credential_issuer":"https://a.example","credential_issuer":"https://b.example"}""",
            offer(extra = """, "grants":{"x":{"code":1,"code":2}}"""),
            offer(extra = """, "extension":"\uD800""""))
            .forEach { dispatch(link(it), "credential_offer") }
    }

    @Test
    fun `strict component decoding prevents duplicate and conflicting query inputs`() {
        dispatch(link() + "&credential_offer=%7B%7D", "credential_offer")
        dispatch(link() + "&%63redential_offer=%7B%7D", "credential_offer")
        dispatch(link() + "&credential_offer_uri=https%3A%2F%2Fdo-not-contact.invalid", "credential_offer_uri", InvocationRefusalReason.UNSUPPORTED)
        dispatch("av-vci://?credential_offer_uri=https://do-not-contact.invalid", "credential_offer_uri", InvocationRefusalReason.UNSUPPORTED)
        dispatch(link() + "&unknown=secret", "unknown", InvocationRefusalReason.UNSUPPORTED)
        dispatch(link() + "&%FF=secret", "query")
        dispatch(link() + "&=secret", "query", InvocationRefusalReason.UNSUPPORTED)
        dispatch(link() + "&", "query", InvocationRefusalReason.UNSUPPORTED)
        for (escaped in listOf("%７B", "%7Ｂ", "%٧B", "%۷B")) {
            dispatch(link().replaceFirst("%7B", escaped), "credential_offer")
            dispatch("av-vci://?" + escaped + "credential_offer=value", "query")
        }
        val encodedData = dispatch(link(offer(extra = """, "extension":"x&credential_offer_uri=https://inert.invalid?x=1+2"""")))!!
        assertEquals(issuer, encodedData.credentialIssuer)
        // Once decoded, the issuer's literal percent escapes must stay literal.
        assertTrue(encodedData.credentialIssuer.contains("%26"))
    }

    @Test
    fun `issuer errors name credential_issuer`() {
        dispatch(link("""{"credential_configuration_ids":["proof_of_age"]}"""), "credential_issuer")
        listOf("null", "1", "{}", "[]", "true", "\"\"", "\"http://issuer.example\"", "\"https:issuer.example\"",
            "\"https:///path\"", "\"https://\"", "\"https://bad host\"", "\"https://issuer.example:99999\"",
            "\"https://user:password@issuer.example\"", "\"https://issuer.example/#fragment\"")
            .forEach { dispatch(link(offer(issuerJson = it)), "credential_issuer") }
    }

    @Test
    fun `issuer URLs reject query empty port and bidi controls`() {
        for (value in listOf("https://issuer.example/path?token=secret", "https://issuer.example:")) {
            val encoded = "\"$value\""
            dispatch(link(offer(issuerJson = encoded)), "credential_issuer")
        }
        for (value in listOf("https://issuer.example", "https://issuer.example:443")) {
            val encoded = "\"$value\""
            assertEquals(value, dispatch(link(offer(issuerJson = encoded)))?.credentialIssuer)
        }
        val bidiIssuer = "https://issuer.example/" + 0x202E.toChar() + "evil"
        dispatch(link(offer(issuerJson = "\"$bidiIssuer\"")), "credential_issuer")
    }

    @Test
    fun `bounded JSON nesting and offer length are refused safely`() {
        val nested = "[".repeat(100) + "0" + "]".repeat(100)
        dispatch(link(offer(extra = ",\"extension\":$nested")), "credential_offer")

        fun padded(size: Int) = link(offer(extra = ",\"extension\":\"" + "x".repeat(size) + "\""))
        var low = 0
        var high = 65536
        while (low < high) {
            val middle = (low + high) / 2
            if (padded(middle).length < 65536) low = middle + 1 else high = middle
        }
        val exact = padded(low)
        assertEquals(65536, exact.length)
        assertTrue(dispatch(exact) != null)
        val oversized = padded(low + 1)
        assertEquals(65537, oversized.length)
        dispatch(oversized, "credential_offer")
    }

    @Test
    fun `configuration errors name credential_configuration_ids`() {
        dispatch(link("""{"credential_issuer":"https://issuer.example"}"""), "credential_configuration_ids")
        listOf("null", "{}", "\"proof_of_age\"", "[]", "[1]", "[null]", "[\"other\"]",
            "[\"proof_of_age\",\"other\"]", "[\"proof_of_age\",\"proof_of_age\"]")
            .forEach { dispatch(link(offer(configurations = it)), "credential_configuration_ids") }
        listOf("null", "[]", "1", "\"code\"").forEach {
            dispatch(link(offer(extra = ",\"grants\":$it")), "grants")
        }
    }

    @Test
    fun `unsupported AV location and unknown schemes never navigate`() {
        listOf("av-vci:offer", "av-vci:/?credential_offer=x", "av-vci://issuer?credential_offer=x",
            "av-vci://user@issuer?credential_offer=x", "av-vci://:42?credential_offer=x",
            "av-vci:///path?credential_offer=x", "av-vci:///%2F?credential_offer=x", "av-vci://#fragment",
            link() + "#", link() + "#fragment").forEach { dispatch(it, "invocation", InvocationRefusalReason.UNSUPPORTED) }
        listOf("unknown://offer", "javascript:alert(1)", "", "/relative").forEach { dispatch(it, "scheme", InvocationRefusalReason.UNSUPPORTED) }
    }

    @Test
    fun `existing presentation and onboarding links use the actual generic callback unchanged`() {
        listOf(
            "haip://pid?binding_token=secret%2520",
            "haip://issue?state=state&code=secret",
            "haip://present?client_id=c&request_uri=https%3A%2F%2Fr.example",
            "haip-vp://?client_id=c&request_uri=r", "openid4vp://?client_id=c&request_uri=r",
            "eudi-openid4vp://?client_id=c&request_uri=r", "mdoc-openid4vp://?client_id=c&request_uri=r",
            "mdl-openid4vp://?client_id=c&request_uri=r", "av://?client_id=c", "eudi-wallet://route",
            "https://pid-provider-test.example.com/bindInstance"
        ).forEach { raw ->
            var navigated: String? = null
            dispatchWalletInvocation(raw, { error("Unexpected issuance") }, { navigated = it }, { error("Unexpected refusal") })
            assertEquals(raw, navigated)
        }
    }

    @Test
    fun `generic navigation exceptions produce a visible refusal callback`() {
        var refusal: InvocationRefusal? = null
        dispatchWalletInvocation("https://unknown.example", { error("Unexpected issuance") },
            { throw IllegalArgumentException("secret in nav error") }, { refusal = it })
        assertEquals(InvocationRefusal("invocation", InvocationRefusalReason.NAVIGATION), refusal)
        assertEquals(
            WalletInvocation.Refused(InvocationRefusal("scheme", InvocationRefusalReason.UNSUPPORTED)),
            AvVciInvocation.parse("unknown://")
        )
    }
}
