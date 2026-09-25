package ee.cyber.wallet.zk

import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import org.multipaz.mdoc.zkp.ProofVerificationFailureException
import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Proves and verifies an `age_over_18` Longfellow proof over a freshly minted EE age-verification
 * mdoc, then repeats the verification against a corrupted proof.
 *
 * The mdoc is minted here rather than checked in as a fixture so the test exercises this wallet's
 * own docType and namespace: a stored blob would only ever re-prove multipaz's test vector.
 * Minting, circuit picking, transcript and timestamp helpers are the shared ones from
 * ZkConformanceSupport, so this round trip binds exactly what the fixture tests bind.
 */
class AgeProofRoundTripTest {

    @Test
    fun ageOver18ProofVerifiesAndTamperingIsRejected() = runTest {
        val zkSystem = LongfellowZkSystem().apply { addDefaultCircuits() }
        val signedAt = ZkConformanceConsts.signedAtNow()
        val sessionTranscript = SessionTranscripts.forZkConformance()
        val document = MdocMinter.mintAgeVerificationMdoc(sessionTranscript, signedAt).document

        val spec = ZkConformanceSpecs.oneAttributeSpec(zkSystem)
        val zkDocument = zkSystem.generateProof(spec, document, sessionTranscript, signedAt)

        assertEquals(ZkConformanceConsts.AV_DOCTYPE, zkDocument.documentData.docType)
        assertTrue(zkDocument.proof.size > 0, "prover returned an empty proof")

        // The positive control: an untouched proof verifies against the same spec and transcript.
        zkSystem.verifyProof(zkDocument, spec, sessionTranscript)

        // The negative control. Without it a prover that emitted a constant would still pass.
        val corrupted = zkDocument.proof.toByteArray().copyOf()
        corrupted[corrupted.size / 2] = (corrupted[corrupted.size / 2].toInt() xor 0x01).toByte()
        assertFailsWith<ProofVerificationFailureException> {
            zkSystem.verifyProof(zkDocument.copy(proof = ByteString(corrupted)), spec, sessionTranscript)
        }
    }
}
