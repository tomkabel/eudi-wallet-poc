package ee.cyber.wallet.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Instant
import java.util.Base64
import java.util.Date

/**
 * PKI plan NEW-S2-4: the differential suite. tests/vectors/trust/cases.json is run here and by
 * the Go verifier (oid4vp.TestSharedTrustVectors); both must reach the case's `expect`, so any
 * disagreement between the engines fails one of the two CI jobs.
 */
class SharedTrustVectorsTest {

    @Test
    fun `android validator agrees with every shared trust vector`() {
        val file = File("../tests/vectors/trust/cases.json")
        val doc = Json.parseToJsonElement(file.readText()).jsonObject
        val at = Date.from(Instant.parse(doc.getValue("at").jsonPrimitive.content))
        val factory = CertificateFactory.getInstance("X.509")
        fun certs(key: String, case: Map<String, kotlinx.serialization.json.JsonElement>) =
            case.getValue(key).jsonArray.map {
                factory.generateCertificate(Base64.getDecoder().decode(it.jsonPrimitive.content).inputStream()) as X509Certificate
            }
        val cases = doc.getValue("cases").jsonArray.map { it.jsonObject }
        assertTrue("the suite has shrunk to ${cases.size} cases", cases.size >= 15)
        CertificateChainValidator.updateLOTLCertificates(emptyList())
        val mismatches = cases.mapNotNull { case ->
            val name = case.getValue("name").jsonPrimitive.content
            val profile = when (val p = case.getValue("profile").jsonPrimitive.content) {
                "mdoc_ds" -> ChainProfile.MDOC_DS
                "generic" -> ChainProfile.GENERIC
                else -> error("unknown profile $p in $name")
            }
            val accepted = CertificateChainValidator.validateCertificateChain(
                certs("chain", case),
                certs("anchors", case),
                enableRevocationCheck = false,
                profile = profile,
                at = at
            )
            val got = if (accepted) "accept" else "refuse"
            val want = case.getValue("expect").jsonPrimitive.content
            if (got == want) null else "$name: got $got, want $want (${case.getValue("why").jsonPrimitive.content})"
        }
        assertEquals("engine disagrees with the shared vectors", emptyList<String>(), mismatches)
    }
}
