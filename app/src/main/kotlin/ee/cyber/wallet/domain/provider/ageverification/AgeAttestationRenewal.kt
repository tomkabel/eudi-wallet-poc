package ee.cyber.wallet.domain.provider.ageverification

import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.presentation.AgePredicatePolicy
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * When the wallet asks the user to renew the age attestations. Computed on the device only:
 * nothing here, in particular the PID date of birth, is sent to the Provider (EE-POA-029).
 *
 * Renewal is due on or after the earliest of:
 *  - EE-POA-025: [LEAD_DAYS] days before the earliest `validUntil` held;
 *  - EE-POA-029: the day a predicate held as `false` becomes true, from the PID `birth_date`;
 *  - EE-POA-013 / EE-POA-011c: the day a batch is down to its last attestation (low-water mark).
 *
 * The mock Provider re-identifies the user on every issuance, so renewal is offered in the UI
 * rather than run unattended.
 */
object AgeAttestationRenewal {

    const val LEAD_DAYS = 14

    data class Held(
        val type: CredentialType,
        val validUntil: LocalDate?,
        /** The `age_over_NN` thresholds this attestation holds as false. */
        val falseThresholds: List<Int>
    )

    fun dueDate(held: List<Held>, birthDate: LocalDate?, today: LocalDate): LocalDate? {
        if (held.isEmpty()) return null
        val candidates = buildList {
            held.mapNotNull { it.validUntil }.minOrNull()?.let { add(it.minus(LEAD_DAYS, DateTimeUnit.DAY)) }
            if (birthDate != null) {
                held.flatMap { it.falseThresholds }.distinct()
                    .forEach { add(AgeReckoning.anniversary(birthDate, it)) }
            }
            if (held.groupingBy { it.type }.eachCount().values.any { it <= 1 }) add(today)
        }
        return candidates.minOrNull()
    }

    fun isDue(held: List<Held>, birthDate: LocalDate?, today: LocalDate): Boolean =
        dueDate(held, birthDate, today)?.let { it <= today } ?: false

    /** The held age attestations among [documents], read from their signed MSO. */
    fun held(documents: List<CredentialDocument>): List<Held> = documents
        .filterIsInstance<CredentialDocument.MDocDocument>()
        .filter { AgePredicatePolicy.isAgeAttestation(it.attestation.type) }
        .map { document ->
            Held(
                type = document.attestation.type,
                validUntil = runCatching {
                    document.mDoc.MSO?.validityInfo?.validUntil?.value?.toLocalDateTime(AgeReckoning.TALLINN)?.date
                }.getOrNull(),
                falseThresholds = document.fields
                    .filter { AgePredicatePolicy.AGE_PREDICATE.matches(it.name) && it.value == "false" }
                    .map { it.name.removePrefix("age_over_").toInt() }
            )
        }

    /** The PID date of birth, from whichever PID the wallet holds. */
    fun birthDate(documents: List<CredentialDocument>): LocalDate? = documents
        .filter { it.attestation.type == CredentialType.PID_MDOC || it.attestation.type == CredentialType.PID_SD_JWT }
        .flatMap { it.fields }
        .firstOrNull { it.name == "birth_date" || it.name == "birthdate" }
        ?.let { runCatching { LocalDate.parse(it.value) }.getOrNull() }
}
