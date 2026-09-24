package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.R

/**
 * Pure decision core for the step 3 holder obligations. Kept free of Android types so the
 * rules can be unit tested on the JVM; the view models map their framework objects onto
 * [SpecFingerprint] and delegate here.
 */
object HolderObligations {

    /**
     * The parts of an advertised or held ZK system spec that decide a match: which circuit the
     * proof would run and how many attributes it covers. `circuitHash` null means the spec does
     * not name a circuit.
     */
    data class SpecFingerprint(
        val circuitHash: String?,
        val numAttributes: Long?,
        val version: Long?
    )

    /**
     * The strongest circuit we hold that the reader also allows for this attribute count,
     * mirroring `ZkSystem.getMatchingSystemSpec`.
     */
    fun strongestMatchingSpec(
        held: List<SpecFingerprint>,
        advertisedCircuitHashes: Set<String>,
        numAttributes: Int
    ): SpecFingerprint? = held
        .filter {
            it.circuitHash in advertisedCircuitHashes &&
                it.numAttributes == numAttributes.toLong()
        }
        .maxByOrNull { it.version ?: Long.MIN_VALUE }

    /**
     * EE-ZKP-042: the tier the presentation would fall back to when a zero-knowledge proof cannot
     * be used, or null when no fallback is expected. `proofRequested` is whether the relying party
     * advertised any ZK system specs at all.
     */
    fun expectedPlainTier(zkCapable: Boolean, proofRequested: Boolean, satisfiable: Boolean): PresentationTier? = when {
        !proofRequested -> PresentationTier.PLAIN_NOT_REQUESTED
        !zkCapable -> PresentationTier.PLAIN_DEVICE_INCAPABLE
        !satisfiable -> PresentationTier.PLAIN_NO_MATCHING_CIRCUIT
        else -> null
    }

    /**
     * EE-ZKP-051, strict reading, scoped by the caller to the doctypes the ZK path exists for.
     * A device capable of proving in general still refuses the plain fallback when the relying
     * party advertised ZKP support but only circuits this wallet does not hold — otherwise the
     * advertisement itself becomes the downgrade lever. A device that cannot prove at all, or a
     * party that did not ask, keeps the plain path with the EE-ZKP-042 notice.
     */
    fun refusePlainFallback(zkCapable: Boolean, proofRequested: Boolean, satisfiable: Boolean): Boolean =
        zkCapable && proofRequested && !satisfiable

    /**
     * EE-ZKP-053: how many of the logged presentations the issuer can link. Rows without a tier
     * predate tier recording and are not counted at all.
     */
    fun countLinkable(tiers: List<PresentationTier?>): Pair<Int, Int> {
        val counted = tiers.filterNotNull()
        val linkable = counted.count { it.isLinkable }
        return linkable to counted.size - linkable
    }
}

/**
 * EE-ZKP-042 notice wording. Both variants state the same fact — this presentation can be linked
 * by the issuer — but the reader deserves to know whether the linkability comes from a request
 * the wallet could not satisfy or from the relying party never asking.
 */
fun PresentationTier.zkNoticeRes(): Int = when (this) {
    PresentationTier.PLAIN_NOT_REQUESTED -> R.string.presentation_zk_notice_not_requested
    else -> R.string.presentation_zk_notice_proof_requested
}
