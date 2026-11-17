package ee.cyber.wallet.domain.provider.ageverification

import android.content.Context
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.pid.MOCK_APP_PREFS
import ee.cyber.wallet.domain.provider.pid.MOCK_USER_PID
import ee.cyber.wallet.domain.provider.pid.mockRegisterBirthDate
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

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
