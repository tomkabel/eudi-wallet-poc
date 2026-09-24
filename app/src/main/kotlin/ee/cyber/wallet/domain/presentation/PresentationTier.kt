package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.R

/**
 * Which tier a presentation was made over, recorded per presentation in the transaction log.
 *
 * A zero-knowledge proof reveals a predicate and nothing the verifier can correlate. A plain mdoc
 * carries the issuer's signature, so two presentations of the same credential are linkable to each
 * other. The distinction is the point of the ZK path, and the user cannot reason about it unless
 * the wallet records which one actually happened.
 *
 * The plain variants are kept apart because they mean different things operationally: the
 * verifier chose not to ask, we had nothing that fit what it asked for, this device cannot
 * prove at all, or the prover ran and failed.
 */
enum class PresentationTier {
    ZERO_KNOWLEDGE,
    PLAIN_NOT_REQUESTED,
    PLAIN_NO_MATCHING_CIRCUIT,
    PLAIN_DEVICE_INCAPABLE,
    PLAIN_PROOF_FAILED;

    val isLinkable: Boolean get() = this != ZERO_KNOWLEDGE

    companion object {
        /**
         * EE-ZKP-042 notice wording. Both variants state the same fact — this presentation can be
         * linked by the issuer — but the reader deserves to know whether the linkability comes
         * from a request the wallet could not satisfy or from the relying party never asking.
         */
        fun zkNoticeRes(isProofRequested: Boolean): Int = if (isProofRequested) {
            R.string.presentation_zk_notice_proof_requested
        } else {
            R.string.presentation_zk_notice_not_requested
        }
    }
}
