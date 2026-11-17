package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.ui.screens.presentation.MatchedField

/**
 * What a presentation of an age attestation (`ee.riik.poa.1` or `eu.europa.ec.av.1`) may
 * disclose. One rule for the OpenID4VP, DC API and proximity paths.
 *
 * EE-POA-030: at most one `age_over_NN` per presentation. Two predicates bracket the holder's age
 * (`age_over_18` true with `age_over_21` false places them between 18 and 20). The exception for a
 * relying party whose registered intended use names each predicate needs registration
 * certificates, which this wallet does not read, so such a request is always refused here.
 * ISO/IEC 18013-5 §7.2.5 allows two; this is the spec's stricter rule (§22).
 *
 * EE-POA-031: a predicate held as `false` is never returned. The user is told, and the relying
 * party gets the answer it would get if no matching credential existed.
 */
object AgePredicatePolicy {

    enum class Decision { ALLOW, REFUSE_MORE_THAN_ONE_PREDICATE, PREDICATE_HELD_FALSE }

    val AGE_PREDICATE = Regex("age_over_\\d+")

    fun isAgeAttestation(type: CredentialType): Boolean =
        type == CredentialType.EE_POA || type == CredentialType.AGE_VERIFICATION

    /**
     * Judges one matched credential against everything the request named for it, required and
     * optional alike: an optional predicate the user could tick still counts as asked for.
     */
    fun decide(type: CredentialType, requested: List<MatchedField>): Decision {
        if (!isAgeAttestation(type)) return Decision.ALLOW
        val predicates = requested.filter { AGE_PREDICATE.matches(it.field.name) }
        return when {
            predicates.size > 1 -> Decision.REFUSE_MORE_THAN_ONE_PREDICATE
            predicates.any { it.field.value == "false" } -> Decision.PREDICATE_HELD_FALSE
            else -> Decision.ALLOW
        }
    }

    /** The user-facing reason for a refusal, null for [Decision.ALLOW]. */
    fun Decision.error(): AppError? = when (this) {
        Decision.ALLOW -> null
        Decision.REFUSE_MORE_THAN_ONE_PREDICATE -> AppError.PRESENTATION_MULTIPLE_AGE_PREDICATES
        Decision.PREDICATE_HELD_FALSE -> AppError.PRESENTATION_AGE_PREDICATE_FALSE
    }

    /** The first refusal over every credential of one request, or [Decision.ALLOW]. */
    fun decideAll(credentials: List<Pair<CredentialType, List<MatchedField>>>): Decision =
        credentials.map { (type, fields) -> decide(type, fields) }
            .firstOrNull { it != Decision.ALLOW } ?: Decision.ALLOW
}
