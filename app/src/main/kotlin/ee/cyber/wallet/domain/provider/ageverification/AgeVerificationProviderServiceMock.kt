package ee.cyber.wallet.domain.provider.ageverification

import android.content.Context
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.pid.MOCK_APP_PREFS
import ee.cyber.wallet.domain.provider.pid.MOCK_USER_PID
import ee.cyber.wallet.domain.provider.wallet.KeyType
import kotlinx.datetime.LocalDate

class AgeVerificationProviderServiceMock(
    private val context: Context,
    private val cryptoProviderFactory: CryptoProvider.Factory,
    private val credentialIssuanceService: CredentialIssuanceService
) {

    suspend fun issueAgeVerification(): Attestation {
        val credential = getAgeVerificationCredentialByPid()
        val cryptoProvider = cryptoProviderFactory.forKeyType(KeyType.EC)

        return credentialIssuanceService.issueCredential(credential, cryptoProvider.generateKey(KeyType.EC))
    }

    private fun getAgeVerificationCredentialByPid(): Credential.AgeVerificationCredential {
        val prefs = context.getSharedPreferences(MOCK_APP_PREFS, Context.MODE_PRIVATE)
        val pid = prefs.getString(MOCK_USER_PID, null)
        
        return when (pid) {
            "38001085718" -> getAgeVerificationCredential38001085718() // 20 years old
            "1" -> getAgeVerificationCredential38001085718() // 20 years old
            "11" -> getAgeVerificationCredential38001085718() // 20 years old
            "12" -> getAgeVerificationCredential38001085718() // 20 years old
            "13" -> getAgeVerificationCredential38001085718() // 20 years old
            "2" -> getAgeVerificationCredential47101010033() // 54 years old
            "3" -> getAgeVerificationCredential50801139731() // 17 years old
            else -> getAgeVerificationCredential38001085718() // Default to adult
        }
    }

    private fun getAgeVerificationCredential38001085718() = Credential.AgeVerificationCredential(
        ageOver13 = true,
        ageOver15 = true,
        ageOver16 = true,
        ageOver18 = true,
        ageOver21 = false,
        ageOver23 = false,
        ageOver25 = false,
        ageOver27 = false,
        ageOver28 = false,
        ageOver40 = false,
        ageOver60 = false,
        ageOver65 = false,
        ageOver67 = false,
        locationStatus = Credential.LocationStatus(
            idx = "765432",
            uri = "https://aarmam.github.io/statuslists/2"
        )
    )

    private fun getAgeVerificationCredential47101010033() = Credential.AgeVerificationCredential(
        ageOver13 = true,
        ageOver15 = true,
        ageOver16 = true,
        ageOver18 = true,
        ageOver21 = true,
        ageOver23 = true,
        ageOver25 = true,
        ageOver27 = true,
        ageOver28 = true,
        ageOver40 = true,
        ageOver60 = false,
        ageOver65 = false,
        ageOver67 = false,
        locationStatus = Credential.LocationStatus(
            idx = "765432",
            uri = "https://aarmam.github.io/statuslists/2"
        )
    )

    private fun getAgeVerificationCredential50801139731() = Credential.AgeVerificationCredential(
        ageOver13 = true,
        ageOver15 = true,
        ageOver16 = true,
        ageOver18 = false,
        ageOver21 = false,
        ageOver23 = false,
        ageOver25 = false,
        ageOver27 = false,
        ageOver28 = false,
        ageOver40 = false,
        ageOver60 = false,
        ageOver65 = false,
        ageOver67 = false,
        locationStatus = Credential.LocationStatus(
            idx = "765432",
            uri = "https://aarmam.github.io/statuslists/2"
        )
    )
}