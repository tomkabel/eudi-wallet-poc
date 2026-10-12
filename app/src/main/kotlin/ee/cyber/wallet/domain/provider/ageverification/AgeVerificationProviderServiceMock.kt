package ee.cyber.wallet.domain.provider.ageverification

import android.content.Context
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.pid.MOCK_APP_PREFS
import ee.cyber.wallet.domain.provider.pid.MOCK_USER_PID
import ee.cyber.wallet.domain.provider.pid.mockRegisterBirthDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.time.Clock

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
 * Issues the pair of age attestations from ONE authoritative source in ONE issuance transaction
 * (EE-POA-003, conformance plan §4 item 6):
 *
 *  - the EU AV Profile attestation `eu.europa.ec.av.1`, carrying its single mandatory attribute
 *    `age_over_18` (spec §9.2);
 *  - the Estonian EE-PoA `ee.riik.poa.1` per spec §9.2 (EE-POA-001), validity at most 90 days
 *    and no status reference.
 *
 * Both are batches of [AgeIssuanceConstants.BATCH_SIZE], because EE-POA-003 applies the EE-PoA's
 * batch, validity and consumption rules to the AV attestation too. The predicate is computed here,
 * from the mock register's date of birth and this side's clock in Europe/Tallinn (EE-POA-006,
 * EE-POA-027), never from a value the wallet supplies.
 *
 * Each attestation is minted over its own device key. On-device the keys are generated inside
 * Android Keystore through [ee.cyber.wallet.security.SecureAreaKeyManager.batchCreateKey]; the JVM
 * acceptance test substitutes fakes, and the Android Keystore interaction itself stays
 * PENDING-DEVICE (docs/planning/STEP6-RECORD.md).
 */
class AgeVerificationProviderServiceMock(
    private val context: Context,
    private val clock: Clock = Clock.System
) {

    /**
     * EE-POA-003: both doctypes in one transaction, each as a batch of
     * [AgeIssuanceConstants.BATCH_SIZE] over batch-created SecureArea keys. Three is enough to
     * exercise consumption and never-the-last; the measured >=30 batch and OpenID4VCI batch
     * issuance stay in plan §8.4 and are deliberately not implemented here.
     *
     * @return the freshly minted attestations, the AV batch first and then the EE-PoA batch, in
     * storage order. The issuance screen previews the first document only, so it shows an AV
     * attestation rather than one arbitrary EE-PoA copy.
     */
    suspend fun issueEePoaBatch(issuer: BatchAgeIssuer): List<Attestation> {
        val today = AgeReckoning.today(clock.now())
        val ageOver18 = AgeReckoning.isOver(registerBirthDate(), 18, today)
        val (_, validUntil) = AgeIssuanceConstants.validity(today)
        return issuer.issueBatch(avCredential(ageOver18), AgeIssuanceConstants.BATCH_SIZE) +
            issuer.issueBatch(poaCredential(ageOver18, validUntil), AgeIssuanceConstants.BATCH_SIZE)
    }

    /**
     * One mock source for both attestations (EE-POA-003). The credential model still carries the
     * mock's threshold zoo; the mint
     * ([ee.cyber.wallet.domain.credentials.CredentialIssuanceServiceMock.issueMDocAgeVerification])
     * carries only `age_over_18` into the signed document, per spec §9.2.
     */
    private fun avCredential(ageOver18: Boolean) = Credential.AgeVerificationCredential(
        ageOver13 = ageOver18,
        ageOver15 = ageOver18,
        ageOver16 = ageOver18,
        ageOver18 = ageOver18,
        ageOver21 = ageOver18,
        ageOver23 = ageOver18,
        ageOver25 = ageOver18,
        ageOver27 = ageOver18,
        ageOver28 = ageOver18,
        ageOver40 = ageOver18,
        ageOver60 = ageOver18,
        ageOver65 = ageOver18,
        ageOver67 = ageOver18,
        locationStatus = Credential.LocationStatus(
            idx = "765432",
            uri = "https://aarmam.github.io/statuslists/2"
        )
    )

    private fun poaCredential(ageOver18: Boolean, validUntil: LocalDate) = Credential.EePoaCredential(
        ageOver18 = ageOver18,
        issuingCountry = AgeIssuanceConstants.EE_POA_ISSUING_COUNTRY,
        issuingAuthority = AgeIssuanceConstants.EE_POA_ISSUING_AUTHORITY,
        expiryDate = validUntil
    )

    private fun registerBirthDate(): LocalDate {
        val prefs = context.getSharedPreferences(MOCK_APP_PREFS, Context.MODE_PRIVATE)
        return mockRegisterBirthDate(prefs.getString(MOCK_USER_PID, null))
    }
}

/**
 * The per-transaction key and minting seam behind
 * [AgeVerificationProviderServiceMock.issueEePoaBatch]: the production implementation
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
