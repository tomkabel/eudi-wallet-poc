package ee.cyber.wallet.zk

import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assume.assumeTrue
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.toDataItem
import org.multipaz.crypto.EcPublicKeyDoubleCoordinate
import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.zkp.ProofVerificationFailureException
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkDocumentData
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Step 0 of ee-eudiw's docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md §4:
 * cross-verify the two ZK halves.
 *
 * This wallet proves with multipaz 0.99.0's `libzkp.so`; ee-eudiw verifies with a
 * Go service over the Rust Longfellow runtime. Circuit identity alone does not
 * tell us the two agree on the proof encoding, so this test does two things:
 *
 * 1. Writes one complete multipaz proof — ZkDocument CBOR, raw proof bytes,
 *    session transcript, issuer public key as (x, y), the timestamp and the
 *    disclosed attribute's CBOR — into ee-eudiw's
 *    `verifier/go/zk/testdata/step0-multipaz/`, for `zk.Verify` on the Go side,
 *    together with a provenance file and a file pairing all eight bundled
 *    circuit labels with their hashes.
 * 2. Runs the reverse direction: verifies a proof produced by ee-eudiw's Rust
 *    `ee_poa_demo` prover with multipaz's own `verifyProof`, and checks that
 *    the same proof with one bit flipped is rejected.
 *
 * Step 2a (docs/planning/STEP2A-RECORD.md, PENDING-DEVICE item 4) adds
 * `Step2aDeviceResponseFixtureTest` in this package: a complete `DeviceResponse`
 * fixture built through the wallet's real serialization path. The minting and
 * transcript helpers both tests share live in `ZkConformanceSupport.kt`.
 *
 * Fixture locations default to a sibling ee-eudiw checkout and can be
 * overridden with `-Dstep0.eeEudiw=...` and `-Dstep0.rustProverDir=...`.
 */
@OptIn(ExperimentalEncodingApi::class)
class Step0CrossVerifyTest {

    // Defaults live in build.gradle.kts, the one place that knows the repository root.
    private val rustProverDir: File =
        File(checkNotNull(System.getProperty("step0.rustProverDir")) { "step0.rustProverDir is unset; run through Gradle" })

    @Test
    fun writeMultipazFixtureForGoVerifierAndPairCircuitHashes() = runTest {
        val eeEudiw = EeEudiw.assumePresent()
        val zkSystem = LongfellowZkSystem().apply { addDefaultCircuits() }

        // Every bundled circuit, so the seven hashes the 18 September log did
        // not pair get their labels on record (plan §4, step 0, last sentence).
        val specs = zkSystem.systemSpecs
        val circuitLines = specs.joinToString("\n") { spec ->
            "  ${spec.id}" +
                "  version=${spec.getParam<Long>("version")}" +
                "  num_attributes=${spec.getParam<Long>("num_attributes")}" +
                "  circuit_hash=${spec.getParam<String>("circuit_hash")}"
        }
        val dir = File(eeEudiw, "verifier/go/zk/testdata/step0-multipaz")
        dir.mkdirs()
        File(dir, "multipaz-circuits.txt").writeText(
            "# Bundled systemSpecs of multipaz-longfellow-jvm-0.99.0, printed by\n" +
                "# Step0CrossVerifyTest (plan F1: only 7_1 was previously paired with its hash).\n" +
                "# Label format: longfellow-libzk-v1_<version>_<num_attributes>_<block_enc_hash>_<block_enc_sig>_<hash>\n" +
                circuitLines + "\n"
        )
        assertEquals(8, specs.size, "expected eight bundled circuit specs")

        // The proof the Go side must verify.
        val sessionTranscript = SessionTranscripts.forZkConformance()
        val minted = MdocMinter.mintAgeVerificationMdoc(sessionTranscript, SIGNED_AT)
        val spec = zkSystem.oneAttributeSpec()
        val zkDocument = zkSystem.generateProof(spec, minted.document, sessionTranscript, SIGNED_AT)

        // Self-check first: if this fails, the fixture is bad before the Go side
        // ever sees it.
        zkSystem.verifyProof(zkDocument, spec, sessionTranscript)

        val pub = minted.issuerCert.ecPublicKey as EcPublicKeyDoubleCoordinate
        val timestamp = formatDate(zkDocument.documentData.timestamp)

        val fixture = buildJsonObject {
            put("version", spec.getParam<Long>("version")!!)
            put("num_attributes", spec.getParam<Long>("num_attributes")!!)
            put("pkx", "0x" + pub.x.hex())
            put("pky", "0x" + pub.y.hex())
            put("doc_type", AV_DOCTYPE)
            put("namespace", AV_NAMESPACE)
            put("attr_id", AGE_OVER_18)
            put("attr_cbor_hex", Cbor.encode(true.toDataItem()).hex())
            put("timestamp", timestamp)
            put("zk_system_spec_id", spec.id)
        }
        File(dir, "request.json").writeText(Json.encodeToString(JsonObject.serializer(), fixture))
        File(dir, "mdoc.bin").writeBytes(Cbor.encode(zkDocument.toDataItem()))
        File(dir, "proof.bin").writeBytes(zkDocument.proof.toByteArray())
        File(dir, "transcript.bin").writeBytes(Cbor.encode(sessionTranscript))
        File(dir, "timestamp.txt").writeText(timestamp + "\n")
        File(dir, "issuer_dsc.pem").writeText(pemOf(minted.issuerCert))
        File(dir, "GENERATED-BY").writeText(
            "Generated by eudi-wallet-poc :zk-conformance Step0CrossVerifyTest\n" +
                "multipaz: 0.99.0 (org.multipaz:multipaz-longfellow-jvm)\n" +
                "command: (cd eudi-wallet-poc && ./gradlew :zk-conformance:test --tests " +
                "'ee.cyber.wallet.zk.Step0CrossVerifyTest')\n" +
                "consumed by: ee-eudiw verifier/go/zk TestStep0MultipazFixture\n"
        )

        assertTrue(zkDocument.proof.size > 0, "prover returned an empty proof")
        println("step0: wrote multipaz fixture to ${dir.absolutePath} " +
            "(spec ${spec.id}, proof ${zkDocument.proof.size} bytes)")
    }

    /** Verify ee_poa_demo's proof with multipaz's verifyProof — the reverse direction. */
    @Test
    fun verifyEePoaDemoProofWithMultipaz() = runTest {
        assumeTrue("step0: ${rustProverDir.absolutePath} does not exist", rustProverDir.isDirectory)
        val zkSystem = LongfellowZkSystem().apply { addDefaultCircuits() }

        // The proof binds the issuer's `now` as a public input, so the timestamp
        // must be the one ee_poa_demo was run with, not the test's own clock.
        val request = Json.parseToJsonElement(File(rustProverDir, "request.json").readText()).jsonObject
        val docType = request["doc_type"]!!.jsonPrimitive.content
        val namespace = request["namespace"]!!.jsonPrimitive.content
        val attrId = request["attr_id"]!!.jsonPrimitive.content
        val now = request["now"]!!.jsonPrimitive.content

        val transcriptItem = Cbor.decode(File(rustProverDir, "transcript.bin").readBytes())
        val cert = X509Cert.fromPem(File(rustProverDir, "issuer_dsc.pem").readText())
        val spec = zkSystem.oneAttributeSpec()

        val zkDoc = ZkDocument(
            documentData = ZkDocumentData(
                zkSystemSpecId = spec.id,
                docType = docType,
                timestamp = Instant.parse(now),
                issuerSigned = mapOf(
                    namespace to mapOf<String, DataItem>(attrId to true.toDataItem())
                ),
                deviceSigned = emptyMap(),
                msoX5chain = X509CertChain(listOf(cert))
            ),
            proof = ByteString(File(rustProverDir, "proof.bin").readBytes())
        )
        zkSystem.verifyProof(zkDoc, spec, transcriptItem)
        println("step0: ee_poa_demo proof verified with multipaz under ${spec.id}")

        // Plan §4 step 0 acceptance: a one-bit flip fails in both directions.
        val flipped = zkDoc.proof.toByteArray().copyOf()
        flipped[flipped.size / 2] = (flipped[flipped.size / 2].toInt() xor 0x01).toByte()
        assertFailsWith<ProofVerificationFailureException> {
            zkSystem.verifyProof(zkDoc.copy(proof = ByteString(flipped)), spec, transcriptItem)
        }
    }

    /** PEM with 64-character base64 lines, as OpenSSL writes them. */
    private fun pemOf(cert: X509Cert): String =
        "-----BEGIN CERTIFICATE-----\n" +
            Base64.encode(cert.encoded.toByteArray()).chunked(64).joinToString("\n") +
            "\n-----END CERTIFICATE-----\n"

    private fun ByteArray.hex(): String = joinToString("") { "%02x".format(it) }

    /** The circuit for a single requested attribute, which is what `age_over_18` alone needs. */
    private fun LongfellowZkSystem.oneAttributeSpec(): ZkSystemSpec =
        ZkConformanceSpecs.oneAttributeSpec(this)

    private fun formatDate(i: Instant): String = ZkConformanceFormat.formatDate(i)

    private companion object {
        const val AV_DOCTYPE = ZkConformanceConsts.AV_DOCTYPE
        const val AV_NAMESPACE = ZkConformanceConsts.AV_NAMESPACE
        const val AGE_OVER_18 = ZkConformanceConsts.AGE_OVER_18

        // 18013-5 clauses 7.1 and 9.1.2.4 forbid fractional seconds in these timestamps.
        val SIGNED_AT: Instant = ZkConformanceConsts.signedAtNow()
    }
}
