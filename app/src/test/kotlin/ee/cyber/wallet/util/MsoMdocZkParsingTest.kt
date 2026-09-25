package ee.cyber.wallet.util

import ee.cyber.wallet.domain.presentation.HolderObligations
import ee.cyber.wallet.domain.presentation.mergeZkSpecs
import ee.cyber.wallet.domain.presentation.resolveSchemeId
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Plan §8.7 acceptance tests: the `mso_mdoc_zk` DCQL format parses into the same [ZkSystemSpec]
 * model the ISO `zkRequest` path uses, and an unregistered `zk_system_type` — a circuit_hash the
 * wallet does not hold — triggers the EE-ZKP-051 refusal on this path exactly as it does on the
 * ISO path.
 *
 * The refused-circuit case is proven against the REAL bundled Longfellow circuits
 * (`addDefaultCircuits`), not a fixture that mirrors them: "not in the wallet's held circuits"
 * must mean the circuits this wallet actually ships.
 */
class MsoMdocZkParsingTest {

    /** The sibling verifier's wire shape (`oid4vp.ZkAgeQuery` in ee-eudiw verifier/go/oid4vp/dcql.go). */
    private fun dcqlQuery(vararg credentials: String): JsonObject =
        Json.parseToJsonElement(
            """{"credentials": [${credentials.joinToString(",")}]}"""
        ) as JsonObject

    /**
     * A Google-Wallet-shaped mso_mdoc_zk query: doctype, the advertised circuit (Longfellow v7,
     * one attribute — the circuit the bundled set holds) and the age claim.
     */
    private fun zkQuery(
        docType: String = "eu.europa.ec.av.1",
        id: String = "longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
        system: String = "longfellow-libzk-v1",
        circuitHash: String,
        numAttributes: Int = 1,
        version: Int = 7,
        blockEncHash: Int = 4151,
        blockEncSig: Int = 4096
    ) = dcqlQuery(
        """
        {
          "id": "age_credential",
          "format": "mso_mdoc_zk",
          "meta": {
            "doctype_value": "$docType",
            "zk_system_type": [{
              "system": "$system",
              "id": "$id",
              "circuit_hash": "$circuitHash",
              "num_attributes": $numAttributes,
              "version": $version,
              "block_enc_hash": $blockEncHash,
              "block_enc_sig": $blockEncSig
            }]
          },
          "claims": [{"path": ["eu.europa.ec.av.1", "age_over_18"]}]
        }
        """.trimIndent()
    )

    @Test
    fun `zk_system_type parses into ZkSystemSpec with every param the ISO path names`() {
        val query = zkQuery(circuitHash = "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121")

        val specsByDocType = zkSpecsByDocTypeFromDcql(query)

        val specs = specsByDocType["eu.europa.ec.av.1"]
        assertEquals(1, specs?.size, "one zk_system_type entry must parse into one spec")
        val spec = specs!!.single()
        assertEquals(
            "longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
            spec.id,
            "the reader-advertised id stays verbatim — it is echoed in the proof"
        )
        assertEquals("longfellow-libzk-v1", spec.system)
        assertEquals(
            "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
            spec.getParam("circuit_hash")
        )
        assertEquals(1L, spec.getParam<Long>("num_attributes"))
        assertEquals(7L, spec.getParam<Long>("version"))
        assertEquals(4151L, spec.getParam<Long>("block_enc_hash"))
        assertEquals(4096L, spec.getParam<Long>("block_enc_sig"))
    }

    @Test
    fun `a held circuit resolves and a matching request proves over the real bundled circuits`() = runTest {
        val zkSystem = LongfellowZkSystem().apply { addDefaultCircuits() }
        val heldHash = zkSystem.systemSpecs
            .first { it.getParam<Long>("num_attributes") == 1L }
            .getParam<String>("circuit_hash")!!
        val query = zkQuery(
            id = "reader-named-id",
            circuitHash = heldHash,
            numAttributes = 1
        )

        val specsByDocType = zkSpecsByDocTypeFromDcql(query)

        // The DCQL-parsed spec satisfies the same resolution the ISO zkRequest path runs: the
        // HELD spec's id is resolved (the id travels in the proof; the verifier allowlists by
        // the query's own circuit_hash, which is how ee-eudiw's ZkSystemTypeAllowlist keys it).
        val schemeId = resolveSchemeId(zkSystem, specsByDocType["eu.europa.ec.av.1"].orEmpty(), 1)
        val heldSpec = zkSystem.systemSpecs
            .first { it.getParam<Long>("num_attributes") == 1L && it.getParam<String>("circuit_hash") == heldHash }
        assertEquals(
            heldSpec.id, schemeId,
            "the held circuit's own id resolves over the reader-advertised one"
        )
    }

    @Test
    fun `an unregistered circuit_hash refuses per EE-ZKP-051 against the real bundled circuits`() = runTest {
        val zkSystem = LongfellowZkSystem().apply { addDefaultCircuits() }
        // A well-formed advertisement naming a circuit this wallet does not hold — the downgrade
        // lever EE-ZKP-051's strict reading closes.
        val query = zkQuery(
            circuitHash = "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef",
            numAttributes = 1
        )

        val specsByDocType = zkSpecsByDocTypeFromDcql(query)
        assertTrue(specsByDocType.isNotEmpty(), "the query parses — the refusal is about the circuit, not the shape")

        val schemeId = resolveSchemeId(zkSystem, specsByDocType["eu.europa.ec.av.1"].orEmpty(), 1)
        assertNull(schemeId, "an unheld circuit must not resolve to a proof")

        // The full refusal chain on this path: proof was requested, the device is ZK-capable,
        // but no advertised circuit is satisfiable — the plain fallback is refused.
        val proofRequested = specsByDocType.values.any { it.isNotEmpty() }
        val satisfiable = schemeId != null
        assertTrue(
            HolderObligations.refusePlainFallback(zkCapable = true, proofRequested = proofRequested, satisfiable = satisfiable),
            "EE-ZKP-051 must refuse the plain fallback for an unregistered zk_system_type"
        )
    }

    @Test
    fun `an mso_mdoc query parses to nothing`() {
        val query = dcqlQuery(
            """
            {
              "id": "age_credential",
              "format": "mso_mdoc",
              "meta": {"doctype_value": "eu.europa.ec.av.1"}
            }
            """.trimIndent()
        )

        assertTrue(zkSpecsByDocTypeFromDcql(query).isEmpty())
    }

    @Test
    fun `an mso_mdoc_zk query without zk_system_type parses to nothing`() {
        val query = dcqlQuery(
            """
            {
              "id": "age_credential",
              "format": "mso_mdoc_zk",
              "meta": {"doctype_value": "eu.europa.ec.av.1"}
            }
            """.trimIndent()
        )

        // EE-ZKP-023's mirror: an mso_mdoc_zk query with no advertised circuit cannot be
        // allowlisted, so nothing resolves — and EE-ZKP-042 reports "not requested".
        assertTrue(zkSpecsByDocTypeFromDcql(query).isEmpty())
    }

    @Test
    fun `an entry missing circuit_hash or num_attributes fails closed`() {
        val query = dcqlQuery(
            """
            {
              "id": "age_credential",
              "format": "mso_mdoc_zk",
              "meta": {
                "doctype_value": "eu.europa.ec.av.1",
                "zk_system_type": [{"system": "longfellow-libzk-v1", "id": "partial"}]
              }
            }
            """.trimIndent()
        )

        val specs = zkSpecsByDocTypeFromDcql(query)["eu.europa.ec.av.1"].orEmpty()
        assertEquals(1, specs.size, "the entry itself is well-formed enough to parse")
        assertNull(specs.single().getParam<String>("circuit_hash"), "no circuit_hash — no match can pass")
        assertNull(specs.single().getParam<Long>("num_attributes"), "no num_attributes — no match can pass")
    }

    @Test
    fun `first entry per doctype wins on a duplicate`() {
        val query = dcqlQuery(
            zkEntryJson(circuitHash = "aaa"),
            zkEntryJson(circuitHash = "bbb")
        )

        val specs = zkSpecsByDocTypeFromDcql(query)["eu.europa.ec.av.1"].orEmpty()
        assertEquals("aaa", specs.single().getParam<String>("circuit_hash"))
    }

    @Test
    fun `an entry with no parseable zk_system_type does not shadow a later valid one`() {
        val query = dcqlQuery(
            """
            {
              "id": "malformed",
              "format": "mso_mdoc_zk",
              "meta": {
                "doctype_value": "eu.europa.ec.av.1",
                "zk_system_type": [{"circuit_hash": "no-system-no-id"}]
              }
            }
            """.trimIndent(),
            zkEntryJson(circuitHash = "bbb")
        )

        val specs = zkSpecsByDocTypeFromDcql(query)["eu.europa.ec.av.1"].orEmpty()
        assertEquals("bbb", specs.single().getParam<String>("circuit_hash"))
    }

    @Test
    fun `two doctypes key independently`() {
        val query = dcqlQuery(
            zkEntryJson(circuitHash = "aaa", docType = "eu.europa.ec.av.1"),
            zkEntryJson(circuitHash = "bbb", docType = "ee.riik.poa.1")
        )

        val byDocType = zkSpecsByDocTypeFromDcql(query)
        assertEquals("aaa", byDocType["eu.europa.ec.av.1"]!!.single().getParam<String>("circuit_hash"))
        assertEquals("bbb", byDocType["ee.riik.poa.1"]!!.single().getParam<String>("circuit_hash"))
    }

    @Test
    fun `DCQL specs apply where the ISO doc request carried no zkRequest`() {
        // zkSpecsByDocType keys every requested docType, with an empty list when no zkRequest
        // came with it — the shape of an org-iso-mdoc entry whose companion DCQL query carries
        // the circuits.
        val dcql = zkSpecsByDocTypeFromDcql(dcqlQuery(zkEntryJson(circuitHash = "aaa")))

        val merged = mergeZkSpecs(mapOf("eu.europa.ec.av.1" to emptyList()), dcql)

        assertEquals("aaa", merged["eu.europa.ec.av.1"]!!.single().getParam<String>("circuit_hash"))
    }

    @Test
    fun `ISO specs win over DCQL on a shared doctype`() {
        val iso = ZkSystemSpec("iso-id", "longfellow-libzk-v1").apply { addParam("circuit_hash", "iso") }
        val dcql = zkSpecsByDocTypeFromDcql(dcqlQuery(zkEntryJson(circuitHash = "aaa")))

        val merged = mergeZkSpecs(mapOf("eu.europa.ec.av.1" to listOf(iso)), dcql)

        assertEquals(listOf(iso), merged["eu.europa.ec.av.1"])
    }

    @Test
    fun `a doctype only DCQL names is added`() {
        val dcql = zkSpecsByDocTypeFromDcql(dcqlQuery(zkEntryJson(circuitHash = "aaa")))

        val merged = mergeZkSpecs(mapOf("ee.riik.poa.1" to emptyList()), dcql)

        assertEquals("aaa", merged["eu.europa.ec.av.1"]!!.single().getParam<String>("circuit_hash"))
        assertTrue(merged["ee.riik.poa.1"]!!.isEmpty())
    }

    private fun zkEntryJson(
        circuitHash: String,
        docType: String = "eu.europa.ec.av.1"
    ) = """
        {
          "id": "age_credential",
          "format": "mso_mdoc_zk",
          "meta": {
            "doctype_value": "$docType",
            "zk_system_type": [{
              "system": "longfellow-libzk-v1",
              "id": "reader-named-id",
              "circuit_hash": "$circuitHash",
              "num_attributes": 1,
              "version": 7
            }]
          }
        }
    """.trimIndent()
}
