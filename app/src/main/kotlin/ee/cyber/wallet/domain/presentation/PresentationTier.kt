package ee.cyber.wallet.domain.presentation

/**
 * Which tier a presentation was made over, recorded per presentation in the transaction log.
 *
 * A zero-knowledge proof reveals a predicate and nothing the verifier can correlate. A plain mdoc
 * carries the issuer's signature, so two presentations of the same credential are linkable to each
 * other. The distinction is the point of the ZK path, and the user cannot reason about it unless
 * the wallet records which one actually happened.
 *
 * The three plain variants are kept apart because they mean different things operationally: the
 * verifier chose not to ask, we had nothing that fit what it asked for, or this device cannot
 * prove at all.
 */
enum class PresentationTier {
    ZERO_KNOWLEDGE,
    PLAIN_NOT_REQUESTED,
    PLAIN_NO_MATCHING_CIRCUIT,
    PLAIN_DEVICE_INCAPABLE;

    val isLinkable: Boolean get() = this != ZERO_KNOWLEDGE
}
