package ee.cyber.wallet.domain.provider.ageverification

import android.content.Context
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.pid.MOCK_APP_PREFS
import ee.cyber.wallet.domain.provider.pid.MOCK_USER_PID
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import java.time.ZoneOffset
import kotlin.time.Clock
import kotlin.time.toJavaInstant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDateTime

/** Mock-issuance constants shared between the service and its tests. */
object AgeIssuanceConstants {
    const val EE_POA_ISSUING_COUNTRY = "EE"
    const val EE_POA_ISSUING_AUTHORITY = "EE-EUDIW demo issuer"

    /** Spec §9.2: the EE-PoA's validity is at most 90 days. */
    const val EE_POA_MAX_VALIDITY_DAYS = 90

    /** Plan §4 item 6: three per doctype, enough to test consumption and never-the-last. */
    const val BATCH_SIZE = 3
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
 * Each doctype is minted over its own device key. On-device the keys are generated inside
 * Android Keystore through [ee.cyber.wallet.crypto.LocalCryptoProvider] and
 * [ee.cyber.wallet.security.SecureAreaKeyManager.batchCreateKey]; the JVM acceptance test
 * substitutes fakes, and the Android Keystore interaction itself stays PENDING-DEVICE
 * (docs/planning/STEP6-RECORD.md).
 */
class AgeVerificationProviderServiceMock(
    private val context: Context,
    private val cryptoProviderFactory: CryptoProvider.Factory,
    private val credentialIssuanceService: CredentialIssuanceService
) {

    suspend fun issueAgeVerification(): Attestation {
        val cryptoProvider = cryptoProviderFactory.forKeyType(KeyType.EC)
        val avKey = cryptoProvider.generateKey(KeyType.EC)

        return credentialIssuanceService.issueCredential(avCredential(holderIsAdult()), avKey)
    }

    /**
     * EE-POA-003: both doctypes in one transaction, the EE-PoA as a batch of
     * [AgeIssuanceConstants.BATCH_SIZE] over batch-created SecureArea keys. Three is enough to
     * exercise consumption and never-the-last; the measured >=30 batch and OpenID4VCI batch
     * issuance stay in plan §8.4 and are deliberately not implemented here.
     *
     * @return the freshly minted attestations, the AV attestation first and then the EE-PoA
     * batch, in storage order. The issuance screen previews the first document only, so it shows
     * the AV attestation rather than one arbitrary copy of the batch.
     */
    suspend fun issueEePoaBatch(issuer: BatchAgeIssuer): List<Attestation> {
        val ageOver18 = holderIsAdult()
        val avAttestation = credentialIssuanceService.issueCredential(
            avCredential(ageOver18),
            issuer.generateAvKey()
        )
        return listOf(avAttestation) + issuer.issuePoaBatch(poaCredential(ageOver18), AgeIssuanceConstants.BATCH_SIZE)
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

    private fun poaCredential(ageOver18: Boolean) = Credential.EePoaCredential(
        ageOver18 = ageOver18,
        issuingCountry = AgeIssuanceConstants.EE_POA_ISSUING_COUNTRY,
        issuingAuthority = AgeIssuanceConstants.EE_POA_ISSUING_AUTHORITY,
        expiryDate = issuanceDay().plus(AgeIssuanceConstants.EE_POA_MAX_VALIDITY_DAYS, DateTimeUnit.DAY)
    )

    private fun holderIsAdult(): Boolean {
        val prefs = context.getSharedPreferences(MOCK_APP_PREFS, Context.MODE_PRIVATE)
        return prefs.getString(MOCK_USER_PID, null) != "3" // "3" is the 17-year-old mock user
    }

    private fun issuanceDay(): LocalDate =
        Clock.System.now().toJavaInstant().atZone(ZoneOffset.UTC).toLocalDate()
            .let { javaDay -> LocalDate(javaDay.year, javaDay.monthValue, javaDay.dayOfMonth) }
}

/**
 * The per-transaction key and minting seam behind
 * [AgeVerificationProviderServiceMock.issueEePoaBatch]: the production implementation
 * ([WalletProviderBatchAgeIssuer]) batch-creates the EE-PoA keys through the SecureArea and
 * attests them with the (mock) wallet provider; the AV key is a single generation on the same
 * path. A top-level interface so Hilt/KSP resolves it and JVM tests can fake it.
 */
interface BatchAgeIssuer {
    /** One device key for the AV attestation of this transaction. */
    suspend fun generateAvKey(): KeyAttestation

    /**
     * Mints [count] EE-PoA attestations over [count] batch-created device keys, in storage
     * order.
     */
    suspend fun issuePoaBatch(credential: Credential.EePoaCredential, count: Int): List<Attestation>
}
