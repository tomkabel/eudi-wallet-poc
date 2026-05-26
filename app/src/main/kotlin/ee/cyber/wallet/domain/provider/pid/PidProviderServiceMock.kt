package ee.cyber.wallet.domain.provider.pid

import android.content.Context
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.credentials.CredentialIssuanceService
import ee.cyber.wallet.domain.provider.Attestation
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.datetime.LocalDate
import kotlin.random.Random

const val MOCK_APP_PREFS = "mock_app_prefs"
const val MOCK_USER_PID = "mock_user_pid"

class PidProviderServiceMock(
    private val context: Context,
    private val credentialIssuanceService: CredentialIssuanceService
) : PidProviderService {

    private val PORTRAIT_38001085718 = context.resources.openRawResource(ee.cyber.wallet.R.raw.portrait_38001085718).readBytes()
    private val PORTRAIT_47101010033 = context.resources.openRawResource(ee.cyber.wallet.R.raw.portrait_47101010033).readBytes()
    private val PORTRAIT_50801139731 = context.resources.openRawResource(ee.cyber.wallet.R.raw.portrait_50801139731).readBytes()

    private val JWT_38001085718 = Credential.JwtArfPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "Jõeorg",
        givenName = "Jaak-Kristjan",
        birthdate = LocalDate.parse("2005-01-08"),
        placeOfBirth = Credential.PlaceOfBirth(locality = "EE"),
        nationalities = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        address = Credential.Address(
            formatted = "Tallinna mnt 605, 10145 Tallinn",
            country = "EE",
            region = "Harju",
            locality = "Tallinn",
            postalCode = "10145",
            streetAddress = "Tallinna mnt 605",
            houseNumber = "605"
        ),
        personalAdministrativeNumber = "38001085718",
        picture = PORTRAIT_38001085718,
        birthFamilyName = "Org",
        birthGivenName = "Jaak",
        sex = 1,
        email = "jaak.kristjan.joeorg@example.com",
        phoneNumber = "+37200000766",
        // Mandatory metadata specified in CIR 2024/2977
        dateOfExpiry = LocalDate.parse("2028-12-13"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE1234567",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123456",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        dateOfIssuance = LocalDate.parse("2020-12-13"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )
    private val JWT_38001085718_INVALID_STATUS = Credential.JwtArfPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "Jõeorg",
        givenName = "Jaak-Kristjan",
        birthdate = LocalDate.parse("2005-01-08"),
        placeOfBirth = Credential.PlaceOfBirth(locality = "EE"),
        nationalities = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        address = Credential.Address(
            formatted = "Tallinna mnt 605, 10145 Tallinn",
            country = "EE",
            region = "Harju",
            locality = "Tallinn",
            postalCode = "10145",
            streetAddress = "Tallinna mnt 605",
            houseNumber = "605"
        ),
        personalAdministrativeNumber = "38001085718",
        picture = PORTRAIT_38001085718,
        birthFamilyName = "Org",
        birthGivenName = "Jaak",
        sex = 1,
        email = "jaak.kristjan.joeorg@example.com",
        phoneNumber = "+37200000766",
        // Mandatory metadata specified in CIR 2024/2977
        dateOfExpiry = LocalDate.parse("2028-12-13"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE1234567",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123457",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        dateOfIssuance = LocalDate.parse("2020-12-13"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )

    private val MDOC_38001085718 = Credential.MdocPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "Joeorg",
        givenName = "Jaak-Kristjan",
        birthDate = LocalDate.parse("2005-01-08"),
        birthPlace = "EE",
        nationality = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        residentAddress = "Tallinna mnt 605, 10145 Tallinn",
        residentCountry = "EE",
        residentState = "Harju",
        residentCity = "Tallinn",
        residentPostalCode = "10145",
        residentStreet = "Tallinna mnt",
        residentHouseNumber = "605",
        personalAdministrativeNumber = "38001085718",
        portrait = PORTRAIT_38001085718,
        familyNameBirth = "Org",
        givenNameBirth = "Jaak",
        sex = 1,
        emailAddress = "jaak.kristjan.joeorg@example.com",
        mobilePhoneNumber = "+37200000766",
        // Mandatory metadata specified in CIR 2024/2977
        expiryDate = LocalDate.parse("2028-12-13"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE1234567",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123456",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        issuanceDate = LocalDate.parse("2020-12-13"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )

    private val MDOC_38001085718_INVALID_STATUS = Credential.MdocPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "Joeorg",
        givenName = "Jaak-Kristjan",
        birthDate = LocalDate.parse("2005-01-08"),
        birthPlace = "EE",
        nationality = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        residentAddress = "Tallinna mnt 605, 10145 Tallinn",
        residentCountry = "EE",
        residentState = "Harju",
        residentCity = "Tallinn",
        residentPostalCode = "10145",
        residentStreet = "Tallinna mnt",
        residentHouseNumber = "605",
        personalAdministrativeNumber = "38001085718",
        portrait = PORTRAIT_38001085718,
        familyNameBirth = "Org",
        givenNameBirth = "Jaak",
        sex = 1,
        emailAddress = "jaak.kristjan.joeorg@example.com",
        mobilePhoneNumber = "+37200000766",
        // Mandatory metadata specified in CIR 2024/2977
        expiryDate = LocalDate.parse("2028-12-13"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE1234567",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123457",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        issuanceDate = LocalDate.parse("2020-12-13"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )

    private val JWT_47101010033 = Credential.JwtArfPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "O’Connež-Šuslik",
        givenName = "Mari-Liis Õnne",
        birthdate = LocalDate.parse("1971-01-01"),
        placeOfBirth = Credential.PlaceOfBirth(locality = "EE"),
        nationalities = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        address = Credential.Address(
            formatted = "Pärnu mnt 705, 12145 Tallinn",
            country = "EE",
            region = "Harju",
            locality = "Tallinn",
            postalCode = "10145",
            streetAddress = "Pärnu mnt",
            houseNumber = "705"
        ),
        personalAdministrativeNumber = "47101010033",
        picture = PORTRAIT_47101010033,
        birthFamilyName = "O’Connež-Šuslik Sünnijärgne",
        birthGivenName = "Mari-Liis Õnne Sünnijärgne",
        sex = 2,
        email = "mari.liis.onne@example.com",
        phoneNumber = "+37200000877",
        // Mandatory metadata specified in CIR 2024/2977
        dateOfExpiry = LocalDate.parse("2028-12-13"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE2345678",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123456",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        dateOfIssuance = LocalDate.parse("2020-12-13"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )

    private val MDOC_47101010033 = Credential.MdocPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "O’Connež-Šuslik",
        givenName = "Mari-Liis Õnne",
        birthDate = LocalDate.parse("1971-01-01"),
        birthPlace = "EE",
        nationality = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        residentAddress = "Pärnu mnt 705, 12145 Tallinn",
        residentCountry = "EE",
        residentState = "Harju",
        residentCity = "Tallinn",
        residentPostalCode = "12145",
        residentStreet = "Pärnu mnt",
        residentHouseNumber = "705",
        personalAdministrativeNumber = "47101010033",
        portrait = PORTRAIT_47101010033,
        familyNameBirth = "O’Connež-Šuslik",
        givenNameBirth = "Mari-Liis Õnne",
        sex = 2,
        emailAddress = "mari.liis.onne@example.com",
        mobilePhoneNumber = "+37200000877",
        // Mandatory metadata specified in CIR 2024/2977
        expiryDate = LocalDate.parse("2028-11-13"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE2345678",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123456",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        issuanceDate = LocalDate.parse("2020-11-13"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )

    private val JWT_50801139731 = Credential.JwtArfPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "Alaealine",
        givenName = "Alar",
        birthdate = LocalDate.parse("2008-01-13"),
        placeOfBirth = Credential.PlaceOfBirth(locality = "EE"),
        nationalities = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        address = Credential.Address(
            formatted = "Tartu mnt 605, 10145 Tallinn",
            country = "EE",
            region = "Harju",
            locality = "Tallinn",
            postalCode = "10145",
            streetAddress = "Tartu mnt 605",
            houseNumber = "605"
        ),
        personalAdministrativeNumber = "50801139731",
        picture = PORTRAIT_50801139731,
        birthFamilyName = "Alarike",
        birthGivenName = "Vastsündinu",
        sex = 1,
        email = "alar.alaealine@example.com",
        phoneNumber = "+37200000988",
        // Mandatory metadata specified in CIR 2024/2977
        dateOfExpiry = LocalDate.parse("2028-12-13"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE3456789",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123456",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        dateOfIssuance = LocalDate.parse("2020-12-13"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )

    private val MDOC_50801139731 = Credential.MdocPidCredential(
        // Mandatory attributes specified in CIR 2024/2977
        familyName = "Alaealine",
        givenName = "Alar",
        birthDate = LocalDate.parse("2008-01-13"),
        birthPlace = "EE",
        nationality = listOf("EE"),
        // Optional attributes specified in CIR 2024/2977
        residentAddress = "Tartu mnt 605, 13145 Tallinn",
        residentCountry = "EE",
        residentState = "Harju",
        residentCity = "Tallinn",
        residentPostalCode = "13145",
        residentStreet = "Tartu mnt",
        residentHouseNumber = "605",
        personalAdministrativeNumber = "50801139731",
        portrait = PORTRAIT_50801139731,
        familyNameBirth = "Vastsündinu",
        givenNameBirth = "Alarike",
        sex = 1,
        emailAddress = "alar.alaealine@example.com",
        mobilePhoneNumber = "+37200000988",
        // Mandatory metadata specified in CIR 2024/2977
        expiryDate = LocalDate.parse("2028-10-14"),
        issuingAuthority = "PPA",
        issuingCountry = "EE",
        // Optional metadata specified in CIR 2024/2977
        documentNumber = "EE3456789",
        issuingJurisdiction = "EE-I",
        locationStatus = Credential.LocationStatus(
            idx = "123456",
            uri = "https://aarmam.github.io/statuslists/1"
        ),
        // Additional optional attributes specified in ARF Rulebook
        issuanceDate = LocalDate.parse("2020-10-14"),
        trustAnchor = "https://aarmam.github.io/trustanchors",
        attestationLegalCategory = "PID"
    )

    val pidToCredential = mapOf(
        "38001085718" to (JWT_38001085718 to MDOC_38001085718),
        "1" to (JWT_38001085718_INVALID_STATUS to MDOC_38001085718_INVALID_STATUS),
        "2" to (JWT_47101010033 to MDOC_47101010033),
        "3" to (JWT_50801139731 to MDOC_50801139731)
    )

    override suspend fun bindInstance(bindingToken: String): InstanceBinding {
        return InstanceBinding(
            sessionToken = Random.nextBytes(256),
            cNonce = Random.nextBytes(256),
            personalDataAccessToken = Random.nextBytes(256)
        )
    }

    override suspend fun issuePid(sessionToken: ByteArray, requests: Map<AttestationType, AttestationRequest>): List<Attestation> = coroutineScope {
        val pid = context.getSharedPreferences(MOCK_APP_PREFS, Context.MODE_PRIVATE).getString(MOCK_USER_PID, "")
        requests.map { entry ->
            async {
                val pid = pidToCredential.getOrDefault(pid, JWT_38001085718 to MDOC_38001085718)
                credentialIssuanceService.issueCredential(
                    credential = when (entry.key) {
                        AttestationType.SD_JWT_VC -> pid.first
                        AttestationType.MDOC -> pid.second
                    },
                    keyAttestation = entry.value.keyAttestation
                )
            }
        }.awaitAll()
    }
}
