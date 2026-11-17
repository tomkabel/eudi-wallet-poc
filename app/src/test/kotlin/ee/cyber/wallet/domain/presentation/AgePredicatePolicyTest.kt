package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.credentials.Namespace
import ee.cyber.wallet.domain.documents.DocumentField
import ee.cyber.wallet.ui.screens.presentation.MatchedField
import kotlin.test.Test
import kotlin.test.assertEquals

/** EE-POA-030 and EE-POA-031: what one presentation of an age attestation may disclose. */
class AgePredicatePolicyTest {

    private fun field(name: String, value: String) =
        MatchedField(DocumentField(Namespace.EE_RIIK_POA_1, name, value), checked = true)

    @Test
    fun `one true predicate is allowed`() {
        assertEquals(
            AgePredicatePolicy.Decision.ALLOW,
            AgePredicatePolicy.decide(CredentialType.EE_POA, listOf(field("age_over_18", "true"), field("issuing_country", "EE")))
        )
    }

    @Test
    fun `two predicates are refused even when both are true`() {
        assertEquals(
            AgePredicatePolicy.Decision.REFUSE_MORE_THAN_ONE_PREDICATE,
            AgePredicatePolicy.decide(CredentialType.AGE_VERIFICATION, listOf(field("age_over_18", "true"), field("age_over_21", "true")))
        )
    }

    @Test
    fun `a predicate held false is never returned`() {
        assertEquals(
            AgePredicatePolicy.Decision.PREDICATE_HELD_FALSE,
            AgePredicatePolicy.decide(CredentialType.EE_POA, listOf(field("age_over_18", "false")))
        )
    }

    @Test
    fun `other doctypes are out of scope`() {
        assertEquals(
            AgePredicatePolicy.Decision.ALLOW,
            AgePredicatePolicy.decide(CredentialType.MDL, listOf(field("age_over_18", "false"), field("age_over_21", "false")))
        )
    }

    @Test
    fun `a request is refused on its first refused credential`() {
        assertEquals(
            AgePredicatePolicy.Decision.PREDICATE_HELD_FALSE,
            AgePredicatePolicy.decideAll(
                listOf(
                    CredentialType.PID_MDOC to listOf(field("family_name", "Tamm")),
                    CredentialType.EE_POA to listOf(field("age_over_18", "false"))
                )
            )
        )
    }
}
