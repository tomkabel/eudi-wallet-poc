package ee.cyber.wallet.domain.presentation

import org.multipaz.cbor.DataItem
import org.multipaz.mdoc.response.MdocDocument
import org.multipaz.mdoc.zkp.ZkDocument
import ee.cyber.wallet.util.DeviceRequestParser
import org.multipaz.mdoc.zkp.ZkSystem
import org.multipaz.mdoc.zkp.ZkSystemSpec
import org.slf4j.LoggerFactory

/**
 * EE-ZKP-004 (conformance plan §4 item 4c): the zero-knowledge path sits behind a stable
 * interface. The view model asks for a presentation over a scheme id — the `ZkSystemSpec.id` the
 * reader's request named a circuit for — and gets back a document or a reason. The scheme id
 * travels in the presentation itself (`ZkDocumentData.zkSystemSpecId`, multipaz fills it from the
 * resolved spec), so the verifier sees which scheme the proof was made over.
 *
 * Proving is scoped by the caller to the age doctypes and to each doc request's own `zkRequest`
 * (findings F11, F15): this interface never decides which documents may prove, it only proves.
 */
interface ZkPresenter {

    /**
     * Ask for a zero-knowledge presentation of [document] over the scheme [schemeId].
     *
     * @param schemeId the `ZkSystemSpec.id` to prove over, or null when the document's own doc
     * request carried no `zkRequest` this document could use — never a guessed id.
     */
    fun presentation(schemeId: String?, document: MdocDocument, sessionTranscript: DataItem): ZkPresentation
}

/** The answer to a [ZkPresenter.presentation] call: a document or a reason. */
sealed interface ZkPresentation {
    data class Proved(val zkDocument: ZkDocument) : ZkPresentation
    data class Unavailable(val reason: ZkPresentationReason) : ZkPresentation
}

/** Why a proof could not be produced. Distinct reasons, because they mean different facts. */
enum class ZkPresentationReason {
    /** No scheme was requested for this document — the plain path is not a downgrade here. */
    NO_SCHEME_REQUESTED,

    /** This device cannot prove at all (no prover, or the native library is not packaged). */
    PROVER_UNAVAILABLE,

    /** The scheme id is not among the circuits this wallet holds. */
    UNKNOWN_SCHEME,

    /** The prover ran and failed (malformed input, size ceilings — finding F15). */
    PROVER_FAILED
}

/**
 * The Longfellow-backed implementation. The scheme id is resolved against the specs the prover
 * holds; the id then travels inside the produced [ZkDocument].
 */
class LongfellowZkPresenter(private val zkSystem: ZkSystem?) : ZkPresenter {

    private val logger = LoggerFactory.getLogger(LongfellowZkPresenter::class.java)

    override fun presentation(schemeId: String?, document: MdocDocument, sessionTranscript: DataItem): ZkPresentation {
        if (schemeId == null) return ZkPresentation.Unavailable(ZkPresentationReason.NO_SCHEME_REQUESTED)
        val system = zkSystem ?: return ZkPresentation.Unavailable(ZkPresentationReason.PROVER_UNAVAILABLE)
        val spec = system.systemSpecs.firstOrNull { it.id == schemeId }
            ?: return ZkPresentation.Unavailable(ZkPresentationReason.UNKNOWN_SCHEME)
        return try {
            ZkPresentation.Proved(system.generateProof(zkSystemSpec = spec, document = document, sessionTranscript = sessionTranscript))
        } catch (e: Throwable) {
            // Finding F15: the prover can reject input it cannot shape (size ceilings). Any JVM
            // throwable (an Error from JNI or memory included) is caught here, as the runCatching
            // this replaced did; a native abort remains a recorded device-level risk.
            logger.warn("Longfellow proof failed for scheme $schemeId", e)
            ZkPresentation.Unavailable(ZkPresentationReason.PROVER_FAILED)
        }
    }
}

/**
 * EE-ZKP-004 seam helper: which held spec a request resolves to, as a scheme id. Kept off
 * [ZkPresenter] so the decision (reader-advertised hashes + attribute count) stays unit-testable
 * through [HolderObligations.strongestMatchingSpec] while the presenter stays a pure executor.
 */
fun resolveSchemeId(
    zkSystem: ZkSystem?,
    requested: List<ZkSystemSpec>,
    numAttributes: Int
): String? {
    val system = zkSystem ?: return null
    val advertisedCircuitHashes = requested.mapNotNull { it.getParam<String>("circuit_hash") }.toSet()
    val held = system.systemSpecs.map {
        HolderObligations.SpecFingerprint(
            circuitHash = it.getParam<String>("circuit_hash"),
            numAttributes = it.getParam<Long>("num_attributes"),
            version = it.getParam<Long>("version")
        )
    }
    val best = HolderObligations.strongestMatchingSpec(held, advertisedCircuitHashes, numAttributes)
        ?: return null
    return system.systemSpecs[held.indexOf(best)].id
}

/**
 * The ZK specs each doc request advertised, keyed by its `docType`, so a credential resolves only
 * against its own doc request's circuits (EE-ZKP-051). A repeated docType keeps the FIRST doc
 * request's specs: the response carries one document per docType, and pooling a second spec set
 * would let a crafted request pair unknown hashes with a held one and defeat the refusal.
 */
fun zkSpecsByDocType(docRequests: List<DeviceRequestParser.DocRequest>): Map<String, List<ZkSystemSpec>> =
    docRequests.groupBy { it.docType }.mapValues { it.value.first().zkSystemSpecs }
