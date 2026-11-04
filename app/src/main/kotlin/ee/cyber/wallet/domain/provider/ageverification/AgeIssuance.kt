package ee.cyber.wallet.domain.provider.ageverification

import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.provider.Attestation
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** Mock-issuance constants shared between the service and its tests. */
object AgeIssuanceConstants {
    const val EE_POA_ISSUING_COUNTRY = "EE"
    const val EE_POA_ISSUING_AUTHORITY = "EE-EUDIW demo issuer"

    /** Spec §9.2: the EE-PoA's validity is at most 90 days. */
    const val EE_POA_MAX_VALIDITY_DAYS = 90

    /** EE-POA-026: the common `ValidityInfo` epoch, in days. */
    const val VALIDITY_EPOCH_DAYS = 7

    /** Plan §4 item 6: three per doctype, enough to test consumption and never-the-last. */
    const val BATCH_SIZE = 3

    /**
     * EE-POA-026: [day] rounded down to a [VALIDITY_EPOCH_DAYS] boundary counted from
     * 1970-01-01, so every attestation issued in one epoch carries the same dates.
     */
    fun epochFloor(day: LocalDate): LocalDate {
        val days = day.toEpochDays()
        return LocalDate.fromEpochDays(days - Math.floorMod(days, VALIDITY_EPOCH_DAYS.toLong()))
    }

    /**
     * The epoch-coarsened validity window of a batch issued on [issuanceDay] (EE-POA-016, -026):
     * both ends on epoch boundaries, at most [EE_POA_MAX_VALIDITY_DAYS] apart.
     */
    fun validity(issuanceDay: LocalDate): Pair<LocalDate, LocalDate> {
        val from = epochFloor(issuanceDay)
        return from to epochFloor(from.plus(EE_POA_MAX_VALIDITY_DAYS, DateTimeUnit.DAY))
    }
}

/**
 * The per-transaction key and minting seam behind the local_mocks build's
 * `AgeVerificationProviderServiceMock.issueEePoaBatch`: the production implementation
 * ([WalletProviderBatchAgeIssuer]) batch-creates the keys through the SecureArea and attests them
 * with the (mock) wallet provider. A top-level interface so Hilt/KSP resolves it and JVM tests can fake it.
 */
interface BatchAgeIssuer {
    /**
     * Mints [count] attestations of [credential] over [count] batch-created device keys, in
     * storage order.
     */
    suspend fun issueBatch(credential: Credential, count: Int): List<Attestation>
}
