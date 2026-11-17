package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import org.multipaz.cbor.DataItem
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkDocument
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.multipaz.request.RequestedClaim
import java.security.MessageDigest
import java.time.Instant
import java.util.Base64

/** ADR-006 offline document. Network distribution remains a separate prerequisite. */
class SignedCircuitList private constructor(
    val listVersion: String,
    val documentSha256: String,
    val expiresAt: Instant,
    private val entries: List<Entry>
) {
    data class Entry(val hash: String, val version: Long, val attributes: Long, val blockHash: Long, val blockSig: Long) {
        val specId: String get() = "longfellow-libzk-v1_" + version + "_" + attributes + "_" + blockHash + "_" + blockSig + "_" + hash
        fun matches(spec: ZkSystemSpec): Boolean = runCatching {
            spec.id == specId &&
                spec.getParam<String>("circuit_hash") == hash && spec.getParam<Long>("version") == version &&
                spec.getParam<Long>("num_attributes") == attributes && spec.getParam<Long>("block_enc_hash") == blockHash &&
                spec.getParam<Long>("block_enc_sig") == blockSig
        }.getOrDefault(false)
    }

    fun accepted(spec: ZkSystemSpec, now: Instant = Instant.now()): Boolean =
        now.isBefore(expiresAt) && entries.any { it.matches(spec) }

    companion object {
        // SHAs have no ordering. Native SpecID/hash/parameters cross-check this exact pair.
        // Every new runtime artifact must establish another compatibility pair.
        const val LIBRARY_REVISION = "61a8a735964d1b22bccf79bf14ef6767249cdf92"
        const val WALLET_ARTIFACT = "multipaz-longfellow:0.99.0"
        private const val MAX_UNSIGNED_PARAMETER = 4294967295L
        private const val MAX_SAFE_JSON_INTEGER = 9007199254740991L
        private val documentFields = setOf(
            "schema", "list_version", "issued_at", "expires_at", "min_library_version",
            "maintainer", "systems", "circuits", "signatures"
        )
        private val circuitFields = setOf(
            "circuit_hash", "version", "num_attributes", "block_enc_hash", "block_enc_sig",
            "upstream_tag", "accepted_on", "deprecated_on", "audits"
        )

        fun validate(
            bytes: ByteArray,
            keyBytes: ByteArray,
            runtimeArtifact: String,
            now: Instant = Instant.now(),
            expectedVersion: String? = null
        ): SignedCircuitList {
            val source = bytes.decodeToString(throwOnInvalidSequence = true)
            JsonDocumentGuard(source).check()
            val root = Json.parseToJsonElement(source).jsonObject
            require(root.keys == documentFields) { "invalid-list-schema" }
            require(root.string("schema") == "ee-circuits/1") { "invalid-list-schema" }
            val version = root.string("list_version")
            require(version.matches(Regex("[1-9][0-9]*")) && version.toULongOrNull() != null) { "invalid-list-version" }
            val key = Json.parseToJsonElement(keyBytes.decodeToString()).jsonObject
            val payload = canonical(JsonObject(root.filterKeys { it != "signatures" })).encodeToByteArray()
            val publicKey = Ed25519PublicKeyParameters(Base64.getUrlDecoder().decode(key.string("public_key")), 0)
            val verified = (root.getValue("signatures") as JsonArray).any {
                val signature = it.jsonObject
                signature.string("key_id") == key.string("key_id") && runCatching {
                    Ed25519Signer().run {
                        init(false, publicKey)
                        update(payload, 0, payload.size)
                        verifySignature(Base64.getUrlDecoder().decode(signature.string("sig")))
                    }
                }.getOrDefault(false)
            }
            require(verified) { "invalid-list-signature-or-unknown-key" }
            val issued = Instant.parse(root.string("issued_at"))
            val expiry = Instant.parse(root.string("expires_at"))
            require(!issued.isAfter(now) && expiry.isAfter(issued)) { "invalid-list-time" }
            require(now.isBefore(expiry)) { "stale-list" }
            require(runtimeArtifact == WALLET_ARTIFACT && root.string("min_library_version") == LIBRARY_REVISION) { "incompatible-library-version" }
            require(expectedVersion == null || expectedVersion == version) { "list-version-skew" }
            require(root.string("maintainer").isNotBlank()) { "missing-list-prerequisite" }
            val systems = (root.getValue("systems") as JsonArray).map { it.jsonPrimitive.content }.sorted()
            require(systems == listOf("longfellow-libzk-v1", "org.iso.mdoc.zk")) { "invalid-list-systems" }
            val all = (root.getValue("circuits") as JsonArray).map { circuitEntry(it.jsonObject) }
            val entries = all.filterNot { it.second }.map { it.first }
            require(entries.map { it.version to it.attributes }.distinct().size == entries.size) { "duplicate live circuit" }
            require(entries.map { it.hash }.distinct().size == entries.size) { "duplicate circuit hash" }
            require(all.isNotEmpty()) { "missing-list-prerequisite" }
            return SignedCircuitList(version, MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }, expiry, entries)
        }

        private fun circuitEntry(circuit: JsonObject): Pair<Entry, Boolean> {
            require(circuit.keys.all { it in circuitFields }) { "invalid-circuit-field" }
            val entry = Entry(
                circuit.string("circuit_hash"),
                circuit.number("version"),
                circuit.number("num_attributes"),
                circuit.number("block_enc_hash"),
                circuit.number("block_enc_sig")
            )
            val parameters = listOf(entry.version, entry.attributes, entry.blockHash, entry.blockSig)
            require(entry.hash.matches(Regex("[0-9a-f]{64}")) && parameters.all { it in 1..MAX_UNSIGNED_PARAMETER }) {
                "invalid-circuit-parameters"
            }
            for (field in listOf("upstream_tag", "accepted_on", "deprecated_on")) circuit.optionalString(field)
            circuit["audits"]?.takeUnless { it == JsonNull }?.let { audits ->
                require(audits is JsonArray && audits.all { it is JsonPrimitive && it.isString }) { "invalid-circuit-metadata" }
            }
            return entry to circuit.optionalString("deprecated_on").isNotEmpty()
        }

        // RFC 8785's UTF-16 member order is Kotlin's natural String order. Reject other
        // JSON numbers because this schema permits integer circuit parameters only.
        internal fun canonical(value: JsonElement): String = when (value) {
            is JsonObject -> value.keys.sorted().joinToString(",", "{", "}") { JsonPrimitive(it).toString() + ":" + canonical(value.getValue(it)) }
            is JsonArray -> value.joinToString(",", "[", "]") { canonical(it) }
            is JsonPrimitive -> if (value.isString || value == JsonNull || value.content in setOf("true", "false")) {
                value.toString()
            } else {
                val number = value.content.toLongOrNull()
                require(number != null && number in -MAX_SAFE_JSON_INTEGER..MAX_SAFE_JSON_INTEGER) { "only safe integer JSON numbers are supported" }
                number.toString()
            }
        }
        private fun JsonObject.optionalString(key: String): String = get(key)?.takeUnless { it == JsonNull }?.let {
            require(it is JsonPrimitive && it.isString) { "invalid-circuit-metadata" }
            it.content
        }.orEmpty()
        private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.let {
            require(it.isString) { "invalid-list-field" }
            it.content
        }
        private fun JsonObject.number(key: String): Long = getValue(key).jsonPrimitive.let {
            require(!it.isString) { "invalid-circuit-parameters" }
            it.long
        }
    }
}

object PinnedCircuitList {
    val loaded: Result<SignedCircuitList> by lazy {
        runCatching {
            val loader = PinnedCircuitList::class.java.classLoader!!
            val document = loader.getResourceAsStream("circuit-registry/circuits.json")?.use { it.readBytes() } ?: error("missing-local-list")
            val key = loader.getResourceAsStream("circuit-registry/pinned-key.json")?.use { it.readBytes() } ?: error("missing-pinned-key")
            SignedCircuitList.validate(document, key, BuildConfig.LONGFELLOW_RUNTIME_ARTIFACT, expectedVersion = BuildConfig.CIRCUIT_LIST_VERSION)
        }
    }
}

/** Preserve native capability even if the signed-list trust prerequisite is unavailable:
 * invalid fallback must never turn a capable device into a plain-response downgrade. */
class RegistryZkSystem private constructor(
    private val native: ZkSystem,
    private val registry: () -> Result<SignedCircuitList>,
    private val clock: () -> Instant
) : ZkSystem {
    constructor(native: ZkSystem, registry: () -> SignedCircuitList?) : this(
        native,
        { registry()?.let { Result.success(it) } ?: Result.failure(IllegalStateException("missing-local-list")) },
        Instant::now
    )

    val registryFailure: String?
        get() {
            val result = registry()
            val list = result.getOrNull() ?: return circuitRegistryFailure(result.exceptionOrNull())
            return if (clock().isBefore(list.expiresAt)) null else "stale-list"
        }

    private fun acceptedList(): SignedCircuitList? = registry().getOrNull()?.takeIf { clock().isBefore(it.expiresAt) }

    companion object {
        fun create(native: ZkSystem, registry: () -> Result<SignedCircuitList>, clock: () -> Instant = Instant::now): RegistryZkSystem =
            RegistryZkSystem(native, registry, clock)
    }
    override val name: String get() = native.name
    override val systemSpecs: List<ZkSystemSpec> get() = acceptedList()?.let { list -> native.systemSpecs.filter { list.accepted(it, clock()) } }.orEmpty()
    override fun generateProof(zkSystemSpec: ZkSystemSpec, document: MdocDocument, sessionTranscript: DataItem, timestamp: kotlin.time.Instant): ZkDocument {
        require(acceptedList()?.accepted(zkSystemSpec, clock()) == true) { registryFailure ?: "missing-acceptable-circuit" }
        val held = systemSpecs.firstOrNull { it.id == zkSystemSpec.id } ?: error("missing-native-circuit")
        return native.generateProof(held, document, sessionTranscript, timestamp)
    }
    override fun verifyProof(zkDocument: ZkDocument, zkSystemSpec: ZkSystemSpec, sessionTranscript: DataItem) {
        require(acceptedList()?.accepted(zkSystemSpec, clock()) == true) { registryFailure ?: "missing-acceptable-circuit" }
        native.verifyProof(zkDocument, systemSpecs.first { it.id == zkSystemSpec.id }, sessionTranscript)
    }
    override fun getMatchingSystemSpec(zkSystemSpecs: List<ZkSystemSpec>, requestedClaims: List<RequestedClaim>): ZkSystemSpec? =
        zkSystemSpecs.filter { requested -> acceptedList()?.accepted(requested, clock()) == true }.let { accepted ->
            if (accepted.isEmpty()) null else native.getMatchingSystemSpec(accepted, requestedClaims)
        }
}

/** Named refusal survives a failed cached load and clock expiry after a successful load. */
val ZkSystem.registryFailure: String? get() = (this as? RegistryZkSystem)?.registryFailure

internal fun circuitRegistryFailure(error: Throwable?): String = when (error?.message) {
    "missing-local-list", "missing-pinned-key" -> error.message!!
    "stale-list" -> "stale-list"
    "incompatible-library-version" -> "incompatible-library-version"
    "list-version-skew" -> "list-version-skew"
    else -> "invalid-list"
}

/** Reject duplicate member names and invalid UTF-16 before the JSON tree parser can
 * discard/replace them. The actual JSON parser then enforces value grammar. */
private class JsonDocumentGuard(private val text: String) {
    private var offset = 0
    fun check() {
        value()
        whitespace()
        require(offset == text.length) { "trailing JSON" }
    }
    private fun whitespace() { while (offset < text.length && text[offset] in " \t\r\n") offset++ }
    private fun value() {
        whitespace()
        require(offset < text.length) { "invalid-list-json" }
        when (text[offset]) {
            '{' -> objectValue()
            '[' -> arrayValue()
            '"' -> string()
            else -> while (offset < text.length && text[offset] !in ",]} \t\r\n") offset++
        }
    }

    private fun objectValue() {
        offset++
        val seen = mutableSetOf<String>()
        whitespace()
        if (text.getOrNull(offset) == '}') {
            offset++
            return
        }
        while (true) {
            whitespace()
            require(seen.add(string())) { "duplicate JSON key" }
            whitespace()
            require(text.getOrNull(offset++) == ':') { "invalid-list-json" }
            value()
            whitespace()
            val end = text.getOrNull(offset++)
            if (end == '}') return
            require(end == ',') { "invalid-list-json" }
        }
    }

    private fun arrayValue() {
        offset++
        whitespace()
        if (text.getOrNull(offset) == ']') {
            offset++
            return
        }
        while (true) {
            value()
            whitespace()
            val end = text.getOrNull(offset++)
            if (end == ']') return
            require(end == ',') { "invalid-list-json" }
        }
    }

    private fun string(): String {
        require(text.getOrNull(offset) == '"') { "invalid-list-json" }
        val start = offset++
        while (offset < text.length) {
            val current = text[offset++]
            if (current == '"') return validatedString(start)
            if (current == '\\') offset++
        }
        error("invalid-list-json")
    }

    private fun validatedString(start: Int): String {
        val decoded = Json.parseToJsonElement(text.substring(start, offset)).jsonPrimitive.content
        var index = 0
        while (index < decoded.length) {
            val current = decoded[index++]
            require(!current.isLowSurrogate()) { "invalid Unicode" }
            if (current.isHighSurrogate()) {
                require(index < decoded.length && decoded[index++].isLowSurrogate()) { "invalid Unicode" }
            }
        }
        return decoded
    }
}
