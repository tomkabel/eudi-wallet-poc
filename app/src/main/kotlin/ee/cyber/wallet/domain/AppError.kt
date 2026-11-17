package ee.cyber.wallet.domain

import androidx.annotation.StringRes
import ee.cyber.wallet.R

enum class AppError(
    @StringRes val resId: Int
) {
    UNKNOWN_ERROR(R.string.error_presentation_unknown_error),
    CONNECTION_ERROR(R.string.error_presentation_rp_connection_failed),
    PRESENTATION_FETCH_REQUEST_ERROR(R.string.error_presentation_verification_error),
    PRESENTATION_RESPONSE_ERROR(R.string.error_presentation_response_error),
    PRESENTATION_VERIFIER_REJECTED_ERROR(R.string.error_presentation_verifier_error),
    PRESENTATION_UNSUPPORTED_REQUEST_ERROR(R.string.error_presentation_unsupported_request_type),
    PRESENTATION_UNENCRYPTED_REQUEST(R.string.error_presentation_unencrypted_request),
    PRESENTATION_MATCH_ERROR(R.string.error_presentation_match_error),
    PRESENTATION_NO_MATCHING_CIRCUIT(R.string.error_presentation_no_matching_circuit),
    PRESENTATION_PROOF_FAILED(R.string.error_presentation_proof_failed),
    PRESENTATION_UNAUTHENTICATED_REQUEST(R.string.error_presentation_unauthenticated_request),
    PRESENTATION_LAST_AGE_ATTESTATION(R.string.error_presentation_last_age_attestation),
    PRESENTATION_MULTIPLE_AGE_PREDICATES(R.string.error_presentation_multiple_age_predicates),
    PRESENTATION_AGE_PREDICATE_FALSE(R.string.error_presentation_age_predicate_false),
    PRESENTATION_INCORRECT_PIN_ERROR(R.string.error_presentation_incorrect_pin),
    ISSUANCE_AUTHORIZATION_ERROR(R.string.error_issuance_authorization_error),
    ISSUANCE_ERROR(R.string.error_issuance_common_error),
    ISSUANCE_UNBOUND_CREDENTIAL(R.string.error_issuance_unbound_credential),
    ISSUANCE_DEFERRED_UNSUPPORTED(R.string.error_issuance_deferred_unsupported),
    USER_CANCELLED(R.string.error_presentation_user_cancelled)
}
