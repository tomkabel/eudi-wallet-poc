package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.util.DeviceRequestParser
import kotlinx.coroutines.test.runTest
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborArray
import org.multipaz.cbor.buildCborMap
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.request.RequestedClaim
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Review finding 2 acceptance tests: the ZK scheme resolution runs per doc request — a spec
 * advertised by doc request 2 must not satisfy doc request 1's proof (the crafted-mixed-request
 * EE-ZKP-051 defeat). The spec-keying helper is tested against the parser's own `DocRequest`
 * shape; the resolution against a fake [ZkSystem] whose specs mirror the bundled circuits.
 */
class ZkPerDocRequestResolutionTest {

    /** Minimal fake prover holding specs keyed like the real Longfellow circuits. */
    private class FakeZkSystem(override val systemSpecs: List<ZkSystemSpec>) : ZkSystem {
        override val name: String = "fake"

        override fun generateProof(
            zkSystemSpec: ZkSystemSpec,
            document: MdocDocument,
            sessionTranscript: DataItem,
            timestamp: Instant
        ): ZkDocument = throw UnsupportedOperationException("not needed — the resolution is what's under test")

        override fun verifyProof(
            zkDocument: ZkDocument,
            zkSystemSpec: ZkSystemSpec,
            sessionTranscript: DataItem
        ) = throw UnsupportedOperationException()

        override fun getMatchingSystemSpec(
            zkSystemSpecs: List<ZkSystemSpec>,
            requestedClaims: List<RequestedClaim>
        ): ZkSystemSpec? = null
    }

    private fun heldSpec(id: String, circuitHash: String, numAttributes: Long, version: Long = 1) =
        ZkSystemSpec(id, "longfellow").apply {
            addParam("circuit_hash", circuitHash)
            addParam("num_attributes", numAttributes)
            addParam("version", version)
        }

    private fun advertisedSpec(id: String, circuitHash: String, numAttributes: Long) =
        ZkSystemSpec(id, "longfellow").apply {
            addParam("circuit_hash", circuitHash)
            addParam("num_attributes", numAttributes)
        }

    /** A parser-shaped doc request carrying exactly the specs given. */
    private suspend fun docRequest(docType: String, specs: List<ZkSystemSpec>): DeviceRequestParser.DocRequest {
        val itemsRequest = buildCborMap {
            put("docType", docType)
            put("nameSpaces", buildCborMap { })
        }
        val docRequest = buildCborMap {
            put("itemsRequest", Tagged(24, Bstr(Cbor.encode(itemsRequest))))
        }
        val deviceRequest = Cbor.encode(
            buildCborMap {
                put("version", "1.0")
                put("docRequests", buildCborArray { add(docRequest) })
            }
        )
        val sessionTranscript = Cbor.encode(buildCborMap { })
        return run {
            val parsed = DeviceRequestParser(deviceRequest, sessionTranscript).parse().docRequests.single()
            // Rebuild with the specs the test chose: the parser derives them from the encoded
            // requestInfo, which this minimal fixture does not carry — substitute them here,
            // preserving the parser's real DocRequest fields for everything else.
            DeviceRequestParser.DocRequest(
                parsed.docType,
                parsed.itemsRequest,
                parsed.requestInfo,
                parsed.readerAuth,
                parsed.readerCertificateChain,
                parsed.readerAuthenticated,
                specs
            )
        }
    }

    private val zkSystem = FakeZkSystem(
        listOf(
            heldSpec("scheme-a-1", "aaa", numAttributes = 1, version = 1),
            heldSpec("scheme-a-7", "aaa", numAttributes = 1, version = 7),
            heldSpec("scheme-b", "bbb", numAttributes = 2, version = 1)
        )
    )
    @Test
    fun `the pooled flatMap would have satisfied doc request 1 - the per-request map does not`() = runTest {
        val requests = listOf(
            docRequest("eu.europa.ec.av.1", listOf(advertisedSpec("attacker", "fff", numAttributes = 1))),
            docRequest("org.iso.18013.5.1.mDL", listOf(advertisedSpec("held", "aaa", numAttributes = 1)))
        )
        val byDocType = zkSpecsByDocType(requests)

        // The old pooled behaviour: request 1's proof resolved over request 2's specs too.
        assertTrue(resolveSchemeId(zkSystem, requests.flatMap { it.zkSystemSpecs }, 1) != null)
        // The per-doc-request map removes exactly that.
        assertNull(resolveSchemeId(zkSystem, byDocType["eu.europa.ec.av.1"].orEmpty(), 1))
    }

    @Test
    fun `a doc request that advertised nothing resolves to nothing`() = runTest {
        val requests = listOf(
            docRequest("eu.europa.ec.av.1", emptyList()),
            docRequest("org.iso.18013.5.1.mDL", listOf(advertisedSpec("held", "aaa", numAttributes = 1)))
        )
        val byDocType = zkSpecsByDocType(requests)
        assertNull(resolveSchemeId(zkSystem, byDocType["eu.europa.ec.av.1"].orEmpty(), 1))
    }

    @Test
    fun `an unknown docType resolves to nothing`() = runTest {
        val requests = listOf(
            docRequest("eu.europa.ec.av.1", listOf(advertisedSpec("held", "aaa", numAttributes = 1)))
        )
        val byDocType = zkSpecsByDocType(requests)
        assertNull(resolveSchemeId(zkSystem, byDocType["ee.riik.poa.1"].orEmpty(), 1))
    }

    @Test
    fun `null zkSystem never resolves`() = runTest {
        val requests = listOf(
            docRequest("eu.europa.ec.av.1", listOf(advertisedSpec("held", "aaa", numAttributes = 1)))
        )
        val byDocType = zkSpecsByDocType(requests)
        assertNull(resolveSchemeId(null, byDocType["eu.europa.ec.av.1"].orEmpty(), 1))
    }

    @Test
    fun `first doc request wins on a duplicate docType`() = runTest {
        val requests = listOf(
            docRequest("eu.europa.ec.av.1", listOf(advertisedSpec("attacker", "fff", numAttributes = 1))),
            docRequest("eu.europa.ec.av.1", listOf(advertisedSpec("held", "aaa", numAttributes = 1)))
        )
        val byDocType = zkSpecsByDocType(requests)
        assertNull(
            resolveSchemeId(zkSystem, byDocType["eu.europa.ec.av.1"].orEmpty(), 1),
            "the second entry's specs must not leak through a duplicate docType"
        )
    }
}
