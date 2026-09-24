package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.R
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * JVM unit tests for the step 3 holder obligations: the EE-ZKP-051 strict refusal, the
 * EE-ZKP-042 notice wording, and the EE-ZKP-053 linkable/unlinkable count.
 */
class HolderObligationsTest {

    // EE-ZKP-051: refuse the plain fallback exactly when the device is ZK-capable, the relying
    // party advertised ZKP support, and no advertised spec can be satisfied.
    @Test
    fun `refuses plain fallback on unsatisfiable advertised specs`() {
        val refused = HolderObligations.refusePlainFallback(
            zkCapable = true,
            proofRequested = true,
            satisfiable = false
        )
        assertTrue(refused)
    }

    @Test
    fun `allows plain fallback when the advertised specs are satisfiable`() {
        assertFalse(HolderObligations.refusePlainFallback(zkCapable = true, proofRequested = true, satisfiable = true))
    }

    @Test
    fun `allows plain fallback when the device cannot prove at all`() {
        // EE-ZKP-050 obliges the wallet to fall back here; the notice of EE-ZKP-042 covers it.
        assertFalse(HolderObligations.refusePlainFallback(zkCapable = false, proofRequested = true, satisfiable = false))
    }

    @Test
    fun `allows plain fallback when the party never asked for a proof`() {
        assertFalse(HolderObligations.refusePlainFallback(zkCapable = true, proofRequested = false, satisfiable = false))
    }

    // EE-ZKP-042: the notice wording follows whether the party asked for a proof or not.
    @Test
    fun `linkable tier with a requested proof uses the requested wording`() {
        assertEquals(
            R.string.presentation_zk_notice_proof_requested,
            PresentationTier.PLAIN_NO_MATCHING_CIRCUIT.zkNoticeRes()
        )
        assertEquals(
            R.string.presentation_zk_notice_proof_requested,
            PresentationTier.PLAIN_DEVICE_INCAPABLE.zkNoticeRes()
        )
    }

    @Test
    fun `not-requested tier uses the not-requested wording`() {
        assertEquals(
            R.string.presentation_zk_notice_not_requested,
            PresentationTier.PLAIN_NOT_REQUESTED.zkNoticeRes()
        )
    }

    @Test
    fun `expected tier is null when a proof will be produced`() {
        assertNull(HolderObligations.expectedPlainTier(zkCapable = true, proofRequested = true, satisfiable = true))
    }

    @Test
    fun `expected tier mirrors the refusal for unsatisfiable specs`() {
        assertEquals(
            PresentationTier.PLAIN_NO_MATCHING_CIRCUIT,
            HolderObligations.expectedPlainTier(zkCapable = true, proofRequested = true, satisfiable = false)
        )
    }

    // The view model recomputes the expected tier on every optional-field toggle with the same
    // per-credential inputs the share-time refusal uses, so a refusal is never reached on a state
    // that was not already shown as linkable.
    @Test
    fun `every refusal was already announced as a linkable expected tier`() {
        listOf(true, false).forEach { capable ->
            listOf(true, false).forEach { requested ->
                listOf(true, false).forEach { satisfiable ->
                    if (HolderObligations.refusePlainFallback(capable, requested, satisfiable)) {
                        assertEquals(
                            PresentationTier.PLAIN_NO_MATCHING_CIRCUIT,
                            HolderObligations.expectedPlainTier(capable, requested, satisfiable)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `expected tier names the party not asking before the device being incapable`() {
        assertEquals(
            PresentationTier.PLAIN_NOT_REQUESTED,
            HolderObligations.expectedPlainTier(zkCapable = false, proofRequested = false, satisfiable = false)
        )
        assertEquals(
            PresentationTier.PLAIN_DEVICE_INCAPABLE,
            HolderObligations.expectedPlainTier(zkCapable = false, proofRequested = true, satisfiable = false)
        )
    }

    // EE-ZKP-053: linkable versus unlinkable counts over logged tiers.
    @Test
    fun `counts linkable against unlinkable rows`() {
        val counts = HolderObligations.countLinkable(
            listOf(
                PresentationTier.ZERO_KNOWLEDGE,
                PresentationTier.PLAIN_NOT_REQUESTED,
                PresentationTier.PLAIN_NO_MATCHING_CIRCUIT,
                PresentationTier.ZERO_KNOWLEDGE,
                PresentationTier.PLAIN_DEVICE_INCAPABLE
            )
        )
        assertEquals(3 to 2, counts)
    }

    @Test
    fun `rows without a tier are not counted`() {
        val counts = HolderObligations.countLinkable(listOf(PresentationTier.ZERO_KNOWLEDGE, null, null))
        assertEquals(0 to 1, counts)
    }

    @Test
    fun `empty log counts as zero and zero`() {
        assertEquals(0 to 0, HolderObligations.countLinkable(listOf()))
        assertEquals(0 to 0, HolderObligations.countLinkable(listOf(null, null)))
    }

    // The strongest-circuit rule the DC API view model delegates to.
    @Test
    fun `picks the highest version among allowed circuits for the attribute count`() {
        val held = listOf(
            HolderObligations.SpecFingerprint(circuitHash = "aaa", numAttributes = 1, version = 1),
            HolderObligations.SpecFingerprint(circuitHash = "bbb", numAttributes = 1, version = 7),
            HolderObligations.SpecFingerprint(circuitHash = "ccc", numAttributes = 2, version = 99)
        )
        val best = HolderObligations.strongestMatchingSpec(held, setOf("aaa", "bbb"), numAttributes = 1)
        assertEquals("bbb", best?.circuitHash)
    }

    @Test
    fun `no held circuit matches an advertisement the wallet does not hold`() {
        val held = listOf(
            HolderObligations.SpecFingerprint(circuitHash = "aaa", numAttributes = 1, version = 1)
        )
        assertNull(HolderObligations.strongestMatchingSpec(held, setOf("fff"), numAttributes = 1))
    }

    @Test
    fun `attribute count must match exactly`() {
        val held = listOf(
            HolderObligations.SpecFingerprint(circuitHash = "aaa", numAttributes = 2, version = 1)
        )
        assertNull(HolderObligations.strongestMatchingSpec(held, setOf("aaa"), numAttributes = 1))
    }

    // ------------------------------------------------------------------
    // Review finding 7: EE-ZKP-053 linkability is per RESPONSE, not per row.
    // ------------------------------------------------------------------

    /**
     * The mixed-response case from review finding 7: an age credential proven in zero knowledge
     * alongside a plain identifying document makes the whole exchange linkable — the plain mdoc
     * carries the issuer's signature, so the verifier correlates every presentation of the
     * response session. Logging the ZK row as ZERO_KNOWLEDGE would understate the disclosure.
     */
    @Test
    fun `one plain document in a response makes every row linkable`() {
        val tiers = HolderObligations.escalateToResponseTier(
            listOf(PresentationTier.ZERO_KNOWLEDGE, PresentationTier.PLAIN_NOT_REQUESTED)
        )
        // The proven row takes the plain sibling's tier: that document is why the response is
        // linkable, and its reason is the true one (no invented "no matching circuit").
        assertEquals(
            listOf(PresentationTier.PLAIN_NOT_REQUESTED, PresentationTier.PLAIN_NOT_REQUESTED),
            tiers
        )
        // Both rows are now counted linkable by EE-ZKP-053's summary.
        assertEquals(2 to 0, HolderObligations.countLinkable(tiers))
    }

    @Test
    fun `all zero-knowledge response keeps its tiers`() {
        val tiers = listOf(PresentationTier.ZERO_KNOWLEDGE, PresentationTier.ZERO_KNOWLEDGE)
        assertEquals(tiers, HolderObligations.escalateToResponseTier(tiers))
        assertEquals(0 to 2, HolderObligations.countLinkable(HolderObligations.escalateToResponseTier(tiers)))
    }

    @Test
    fun `all-plain response is unchanged by escalation`() {
        val tiers = listOf(PresentationTier.PLAIN_DEVICE_INCAPABLE, PresentationTier.PLAIN_NO_MATCHING_CIRCUIT)
        assertEquals(tiers, HolderObligations.escalateToResponseTier(tiers))
    }

    @Test
    fun `single plain document response is linkable`() {
        assertEquals(
            listOf(PresentationTier.PLAIN_NOT_REQUESTED),
            HolderObligations.escalateToResponseTier(listOf(PresentationTier.PLAIN_NOT_REQUESTED))
        )
    }

    // ------------------------------------------------------------------
    // EE-ZKP-004 interface, conformance plan §4 item 4c: per-document tier
    // and the combined-request acceptance.
    // ------------------------------------------------------------------

    /**
     * The plan's acceptance case: `age_over_18` proved over its own `zkRequest` plus a PID
     * attribute from another document in the same response. The proof hides the undisclosed
     * attributes and the issuer signature, but the response names the holder — so the rows
     * log linkable and the combined-request notice is shown. A ZK row next to a PID row must
     * not read "unlinkable" (F15).
     */
    @Test
    fun `age_over_18 plus a PID attribute logs linkable and shows the combined-request notice`() {
        val documents = listOf(
            // The age document: proved in zero knowledge over its own scheme, a predicate only.
            DisclosedDocument(proofUsed = true, disclosesValue = false),
            // The PID document: plain, discloses an identifying text value (e.g. family_name).
            DisclosedDocument(proofUsed = false, disclosesValue = true)
        )

        // The response is identifying and every row escalates to a linkable tier...
        assertTrue(HolderObligations.responseIsIdentifying(documents))
        val perRow = listOf(PresentationTier.ZERO_KNOWLEDGE, PresentationTier.PLAIN_NOT_REQUESTED)
        val escalated = HolderObligations.escalateToResponseTier(perRow)
        assertTrue(escalated.all { it.isLinkable })

        // ...and the user is told, before sharing, that this presentation names them.
        assertTrue(HolderObligations.combinedRequestNotice(documents))
    }

    @Test
    fun `a pure predicate response stays unlinkable and shows no combined notice`() {
        val documents = listOf(
            DisclosedDocument(proofUsed = true, disclosesValue = false),
            DisclosedDocument(proofUsed = true, disclosesValue = false)
        )
        assertFalse(HolderObligations.responseIsIdentifying(documents))
        assertFalse(HolderObligations.combinedRequestNotice(documents))
    }

    @Test
    fun `identifying value inside a proof still makes the response identifying`() {
        // F15: the circuit does not care about doctype and will disclose a text value. A proof
        // over family_name hides the signature, not the name.
        assertTrue(
            HolderObligations.responseIsIdentifying(
                listOf(DisclosedDocument(proofUsed = true, disclosesValue = true))
            )
        )
    }

    @Test
    fun `plain identifying document without any proof shows no combined notice`() {
        // No ZK row to escalate: the ordinary EE-ZKP-042 notice covers it, not the combined one.
        assertFalse(
            HolderObligations.combinedRequestNotice(
                listOf(DisclosedDocument(proofUsed = false, disclosesValue = true))
            )
        )
    }

    @Test
    fun `tier per document follows the reason its ZK path took`() {
        assertEquals(PresentationTier.ZERO_KNOWLEDGE, HolderObligations.tierFor(zkUsed = true, proofRequested = true, zkCapable = true))
        assertEquals(PresentationTier.PLAIN_NOT_REQUESTED, HolderObligations.tierFor(zkUsed = false, proofRequested = false, zkCapable = true))
        assertEquals(PresentationTier.PLAIN_DEVICE_INCAPABLE, HolderObligations.tierFor(zkUsed = false, proofRequested = true, zkCapable = false))
        assertEquals(PresentationTier.PLAIN_NO_MATCHING_CIRCUIT, HolderObligations.tierFor(zkUsed = false, proofRequested = true, zkCapable = true))
    }
}
