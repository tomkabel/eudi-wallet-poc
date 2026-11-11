package org.eudi.externalcarrier

import java.io.File
import java.util.Base64
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.multipaz.cbor.Cbor
import org.multipaz.mdoc.response.DeviceResponse
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import kotlin.test.Test

/** Repository vector feed through unmodified target public parser/verifier. */
class RepositoryCarrierVectorTest {
    @Test
    fun feedBothVectors() = runTest {
        val root = File(requireNotNull(System.getenv("EE_VECTOR_ROOT")))
        val output = File(requireNotNull(System.getenv("EE_VECTOR_OUTPUT")))
        val transcript = Cbor.decode(File(root, "device_response_transcript.bin").readBytes())
        val system = LongfellowZkSystem().apply { addDefaultCircuits() }
        val rows = listOf(
            "interim-json" to "device_response.json.b64",
            "mso-mdoc-zk-cbor" to "device_response.cbor.b64",
        ).map { (carrier, filename) ->
            var parsed = false
            var verified = false
            var detail = ""
            try {
                // No JSON-to-CBOR wrapping: feed the exact published carrier bytes.
                val bytes = Base64.getUrlDecoder().decode(File(root, filename).readText().trim())
                val response = DeviceResponse.fromDataItem(Cbor.decode(bytes))
                require(response.status == DeviceResponse.STATUS_OK) { "DeviceResponse status not OK" }
                require(response.zkDocuments.size == 1) { "Expected exactly one ZK document" }
                parsed = true
                val doc = response.zkDocuments.single()
                val spec = system.systemSpecs.single { it.id == doc.documentData.zkSystemSpecId }
                system.verifyProof(doc, spec, transcript)
                verified = true
                detail = "Exact repository vector parsed and native proof verified by Multipaz 0.99.0 public APIs"
            } catch (error: Exception) {
                detail = error.javaClass.name + ": " + (error.message ?: "no message")
            }
            buildJsonObject {
                put("carrier", carrier)
                put("result", if (verified) "PASS" else "FAIL")
                put("parsed", parsed)
                put("proof_verified", verified)
                put("evidence", detail)
            }
        }
        output.writeText(buildJsonObject {
            put("schema_version", 1)
            put("scope", "Repository vector compatibility only; zero of 19 flow cases executed")
            put("target_revision", "7486ddbca490038c3580513d8273b9bbf18c6b70")
            put("carrier_vectors", JsonArray(rows))
        }.toString() + "\n")
        // Compatibility failures are evidence rows; test process success only means both feeds ran.
    }
}
