package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import java.io.File
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SignedCircuitFallbackTest {
    private val loader = javaClass.classLoader!!
    private fun resource(name: String) = loader.getResourceAsStream("circuit-registry/" + name)!!.use { it.readBytes() }
    private val now = Instant.parse("2026-11-13T12:00:00Z")

    @Test
    fun offlineSharedDocumentResolvesOfferedNativeCircuit() {
        val bytes = resource("circuits.json")
        val key = resource("pinned-key.json")
        val list = PinnedCircuitList.loaded.getOrThrow()
        assertEquals(BuildConfig.CIRCUIT_LIST_VERSION, list.listVersion)
        // Bind the generated build resource to the source; two look-alike bundles are insufficient.
        assertTrue(File("../verifier/circuits.json").readBytes().contentEquals(bytes))
        assertTrue(File("../verifier/go/circuits/pinned-key.json").readBytes().contentEquals(key))
        val system = RegistryZkSystem(LongfellowZkSystem().apply { addDefaultCircuits() }) { list }
        val document = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
        val entry = (document.getValue("circuits") as JsonArray).first().jsonObject
        val parameters = listOf("version", "num_attributes", "block_enc_hash", "block_enc_sig", "circuit_hash")
        val offeredId = "longfellow-libzk-v1_" + parameters.joinToString("_") { entry.getValue(it).jsonPrimitive.content }
        val native = assertNotNull(system.systemSpecs.singleOrNull { it.id == offeredId })
        assertEquals(offeredId, resolveSchemeId(system, listOf(native), 1))
        println(
            "ESTONIAN_FALLBACK=" + buildJsonObject {
                put("consumer", "wallet")
                put("list_version", list.listVersion)
                put("spec_id", offeredId)
                put("fetched", false)
                put("document_sha256", list.documentSha256)
                put("signature_valid", true)
            }
        )
    }

    private fun signedFixture(change: (MutableMap<String, JsonElement>) -> Unit = {}): Pair<ByteArray, ByteArray> {
        val document = Json.parseToJsonElement(resource("circuits.json").decodeToString()).jsonObject.toMutableMap()
        document["issued_at"] = JsonPrimitive("2026-11-01T00:00:00Z")
        document["expires_at"] = JsonPrimitive("2027-01-01T00:00:00Z")
        document.remove("signatures")
        change(document)
        val private = Ed25519PrivateKeyParameters(SecureRandom())
        val payload = SignedCircuitList.canonical(JsonObject(document)).encodeToByteArray()
        val signature = Ed25519Signer().run {
            init(true, private)
            update(payload, 0, payload.size)
            generateSignature()
        }
        document["signatures"] = buildJsonArray {
            add(
                buildJsonObject {
                    put("key_id", "ephemeral-test")
                    put("sig", Base64.getUrlEncoder().withoutPadding().encodeToString(signature))
                }
            )
        }
        val key = buildJsonObject {
            put("key_id", "ephemeral-test")
            put("public_key", Base64.getUrlEncoder().withoutPadding().encodeToString(private.generatePublicKey().encoded))
        }
        return JsonObject(document).toString().encodeToByteArray() to key.toString().encodeToByteArray()
    }

    @Test
    fun invalidFallbacksRefuseAndPreserveNativeCapability() {
        val native = LongfellowZkSystem().apply { addDefaultCircuits() }
        val (bytes, key) = signedFixture()
        val duplicateEnvelope = bytes.decodeToString().replaceFirst("{", "{\"schema\":\"ee-circuits/1\",")
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(duplicateEnvelope.encodeToByteArray(), key, SignedCircuitList.WALLET_ARTIFACT, now) }
        val list = SignedCircuitList.validate(bytes, key, SignedCircuitList.WALLET_ARTIFACT, now)
        val held = native.systemSpecs.first { list.accepted(it, now) }
        for (tamperedParameter in listOf("circuit_hash", "block_enc_hash")) {
            val forged = ZkSystemSpec(held.id, "longfellow-libzk-v1").apply {
                addParam("circuit_hash", if (tamperedParameter == "circuit_hash") "0".repeat(64) else held.getParam<String>("circuit_hash")!!)
                for (parameter in listOf("version", "num_attributes", "block_enc_hash", "block_enc_sig")) {
                    addParam(parameter, if (parameter == tamperedParameter) 1L else held.getParam<Long>(parameter)!!)
                }
            }
            assertFalse(list.accepted(forged, now), "same id cannot hide changed hash or block parameters")
            assertEquals(null, RegistryZkSystem(native) { list }.getMatchingSystemSpec(listOf(forged), emptyList()))
        }
        assertFalse(list.accepted(native.systemSpecs.first(), Instant.parse("2027-01-01T00:00:00Z")))
        val empty = RegistryZkSystem(native) { null }
        assertTrue(empty.systemSpecs.isEmpty())
        assertEquals(null, resolveSchemeId(empty, native.systemSpecs, 1))
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(bytes, key, SignedCircuitList.WALLET_ARTIFACT, now, "999") }
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(bytes, key, "multipaz-longfellow:unknown", now) }
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(bytes, key, SignedCircuitList.WALLET_ARTIFACT, Instant.parse("2027-01-01T00:00:00Z")) }
        val tampered = bytes.decodeToString().replace("community-maintained", "tampered").encodeToByteArray()
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(tampered, key, SignedCircuitList.WALLET_ARTIFACT, now) }
        val unknownKey = key.decodeToString().replace("ephemeral-test", "unknown").encodeToByteArray()
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(bytes, unknownKey, SignedCircuitList.WALLET_ARTIFACT, now) }
        val incompatible = signedFixture { it["min_library_version"] = JsonPrimitive("f".repeat(40)) }
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(incompatible.first, incompatible.second, SignedCircuitList.WALLET_ARTIFACT, now) }
        val duplicates = signedFixture { it["circuits"] = JsonArray((it.getValue("circuits") as JsonArray).let { c -> c + c.first() }) }
        assertFailsWith<IllegalArgumentException> { SignedCircuitList.validate(duplicates.first, duplicates.second, SignedCircuitList.WALLET_ARTIFACT, now) }
    }

    @Test
    fun deprecatedEntryDoesNotDisplaceLiveEntry() {
        val fixture = signedFixture { root ->
            val circuits = root.getValue("circuits") as JsonArray
            root["circuits"] = JsonArray(circuits + JsonObject(circuits.first().jsonObject + ("deprecated_on" to JsonPrimitive("2026-01-01"))))
        }
        val list = SignedCircuitList.validate(fixture.first, fixture.second, SignedCircuitList.WALLET_ARTIFACT, now)
        val system = RegistryZkSystem(LongfellowZkSystem().apply { addDefaultCircuits() }) { list }
        assertEquals(1, system.systemSpecs.size)
    }

    @Test
    fun productionFactoryRetainsCapabilityAndNamesEveryRegistryFailure() {
        val native = LongfellowZkSystem().apply { addDefaultCircuits() }
        val (bytes, key) = signedFixture()
        var clock = now
        fun factory(result: Result<SignedCircuitList>): ZkSystem = assertNotNull(
            ZkCapability.create({ native }, { result }, { clock })
        )
        val valid = factory(runCatching { SignedCircuitList.validate(bytes, key, SignedCircuitList.WALLET_ARTIFACT, now) })
        assertEquals(null, valid.registryFailure)
        assertEquals(1, valid.systemSpecs.size)
        val scenarios = listOf(
            "missing-local-list" to runCatching<SignedCircuitList> {
                loader.getResourceAsStream("missing/circuit-list.json") ?: error("missing-local-list")
                error("unexpected resource")
            },
            "invalid-list" to runCatching { SignedCircuitList.validate("{}".encodeToByteArray(), key, SignedCircuitList.WALLET_ARTIFACT, now) },
            "stale-list" to runCatching { SignedCircuitList.validate(bytes, key, SignedCircuitList.WALLET_ARTIFACT, Instant.parse("2027-01-01T00:00:00Z")) },
            "incompatible-library-version" to runCatching { SignedCircuitList.validate(bytes, key, "unknown", now) },
            "list-version-skew" to runCatching { SignedCircuitList.validate(bytes, key, SignedCircuitList.WALLET_ARTIFACT, now, "999") }
        )
        for ((expected, result) in scenarios) {
            val capable = factory(result)
            assertEquals(expected, capable.registryFailure)
            assertTrue(capable.systemSpecs.isEmpty(), expected)
            assertEquals(null, resolveSchemeId(capable, native.systemSpecs, 1))
        }
        clock = Instant.parse("2027-01-01T00:00:00Z")
        assertEquals("stale-list", valid.registryFailure)
        assertTrue(valid.systemSpecs.isEmpty(), "a previously loaded list must refuse after clock expiry")
        assertEquals(null, ZkCapability.create({ error("native unavailable") }, { error("registry must not load") }))
    }

    private fun copySpec(source: ZkSystemSpec, changed: String? = null, omit: Boolean = false, version: Long? = null): ZkSystemSpec {
        val hash = source.getParam<String>("circuit_hash")!!
        val newVersion = version ?: source.getParam<Long>("version")!!
        val id = if (changed == "id") "unknown-id" else if (version != null) source.id.replace("_7_", "_" + newVersion + "_") else source.id
        return ZkSystemSpec(id, "longfellow-libzk-v1").apply {
            if (!(changed == "circuit_hash" && omit)) addParam("circuit_hash", if (changed == "circuit_hash") "0".repeat(64) else hash)
            for (parameter in listOf("version", "num_attributes", "block_enc_hash", "block_enc_sig")) {
                if (parameter == changed && omit) continue
                val value = if (parameter == "version") newVersion else source.getParam<Long>(parameter)!!
                addParam(parameter, if (changed == parameter) value + 1 else value)
            }
        }
    }

    @Test
    fun requestedCircuitRequiresEveryIdentityComponentAndChoosesStrongestCompleteOffer() {
        val native = LongfellowZkSystem().apply { addDefaultCircuits() }
        val held = native.systemSpecs.first { it.getParam<Long>("num_attributes") == 1L }
        assertEquals(held.id, resolveSchemeId(native, listOf(copySpec(held)), 1))
        for (field in listOf("id", "circuit_hash", "version", "num_attributes", "block_enc_hash", "block_enc_sig")) {
            assertEquals(null, resolveSchemeId(native, listOf(copySpec(held, field)), 1), field)
            if (field != "id") assertEquals(null, resolveSchemeId(native, listOf(copySpec(held, field, omit = true)), 1), "missing " + field)
        }
        val wrongType = copySpec(held).apply { addParam("version", "7") }
        assertEquals(null, resolveSchemeId(native, listOf(wrongType), 1), "typed numeric params cannot be strings")
        val newer = copySpec(held, version = 8)
        val system = object : ZkSystem by native { override val systemSpecs = listOf(held, newer) }
        assertEquals(newer.id, resolveSchemeId(system, listOf(held, newer), 1))
        assertEquals(held.id, resolveSchemeId(system, listOf(held), 1))
        assertEquals(null, resolveSchemeId(system, emptyList(), 1), "another document offer cannot fill an empty per-document request")
        assertEquals(null, resolveSchemeId(system, listOf(held), 2))
    }

    @Test
    fun sharedOptionalMetadataHasIdenticalNullEmptyTypeAndDeprecationSemantics() {
        val cases = Json.parseToJsonElement(File("../tests/vectors/circuits/optional-metadata.json").readText()) as JsonArray
        val native = LongfellowZkSystem().apply { addDefaultCircuits() }
        for (element in cases) {
            val case = element.jsonObject
            val fixture = signedFixture { root ->
                val original = (root.getValue("circuits") as JsonArray).first().jsonObject
                root["circuits"] = JsonArray(listOf(JsonObject(original + (case.getValue("field").jsonPrimitive.content to case.getValue("value")))))
            }
            val result = runCatching { SignedCircuitList.validate(fixture.first, fixture.second, SignedCircuitList.WALLET_ARTIFACT, now) }
            val name = case.getValue("name").jsonPrimitive.content
            assertEquals(case.getValue("valid").jsonPrimitive.content.toBoolean(), result.isSuccess, name)
            if (result.isSuccess) {
                val expected = if (case.getValue("deprecated").jsonPrimitive.content.toBoolean()) 0 else 1
                assertEquals(expected, RegistryZkSystem.create(native, { result }, { now }).systemSpecs.size, name)
            }
        }
    }
}
