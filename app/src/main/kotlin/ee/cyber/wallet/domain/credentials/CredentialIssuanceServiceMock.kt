package ee.cyber.wallet.domain.credentials

import android.content.Context
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.util.Base64
import ee.cyber.wallet.crypto.jwsSigner
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.domain.presentation.SupportedFormat
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.ageverification.AgeIssuanceConstants
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.ui.model.IssuerKeyType
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps.serialize
import eu.europa.ec.eudi.sdjwt.RFC7519
import eu.europa.ec.eudi.sdjwt.SdJwtVcSpec
import eu.europa.ec.eudi.sdjwt.cnf
import eu.europa.ec.eudi.sdjwt.sdJwt
import id.walt.mdoc.COSECryptoProviderKeyInfo
import id.walt.mdoc.SimpleCOSECryptoProvider
import id.walt.mdoc.dataelement.BooleanElement
import id.walt.mdoc.dataelement.ByteStringElement
import id.walt.mdoc.dataelement.DataElement
import id.walt.mdoc.dataelement.FullDateElement
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.dataelement.MapKey
import id.walt.mdoc.dataelement.NumberElement
import id.walt.mdoc.dataelement.StringElement
import id.walt.mdoc.doc.MDocBuilder
import id.walt.mdoc.mso.DeviceKeyInfo
import id.walt.mdoc.mso.Status
import id.walt.mdoc.mso.StatusListInfo
import id.walt.mdoc.mso.ValidityInfo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import org.cose.java.AlgorithmID
import org.cose.java.OneKey
import org.slf4j.LoggerFactory
import java.security.KeyStore
import java.time.ZoneOffset
import java.util.UUID
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus

class CredentialIssuanceServiceMock(
    val context: Context,
    val dispatcher: CoroutineDispatcher,
    val userPreferencesDataSource: UserPreferencesDataSource
) : CredentialIssuanceService {
    private val logger = LoggerFactory.getLogger("CredentialIssuanceServiceMock")

    override fun supports(credentialType: CredentialType): Boolean = when (credentialType) {
        CredentialType.PID_SD_JWT, CredentialType.PID_MDOC, CredentialType.AGE_VERIFICATION, CredentialType.EE_POA -> true
        else -> false
    }

    override suspend fun issueCredential(credential: Credential, keyAttestation: KeyAttestation): Attestation {
        logger.debug("issueCredential: {}", credential)
        return when (credential) {
            is Credential.JwtArfPidCredential -> issueSdJwtPid(credential, keyAttestation)
            is Credential.MdocPidCredential -> issueMDocPid(credential, keyAttestation)
            is Credential.MdocMdlCredential -> issueMDocMdl(credential, keyAttestation)
            is Credential.AgeVerificationCredential -> issueMDocAgeVerification(credential, keyAttestation)
            is Credential.EePoaCredential -> issueMDocEePoa(credential, keyAttestation)
            else -> TODO("${credential.type} not supported yet")
        }.let {
            Attestation(
                id = UUID.randomUUID().toString(),
                credential = it,
                type = credential.type,
                keyAttestation = keyAttestation
            )
        }
    }

    suspend fun issueSdJwtPid(pid: Credential.JwtArfPidCredential, keyAttestation: KeyAttestation): String {
        val sdJwtSpec = sdJwt {
            claim(RFC7519.ISSUER, "https://eudi-issuer.dev.riaint.ee")
            claim(RFC7519.ISSUED_AT, 1740045600)
            claim(RFC7519.EXPIRATION_TIME, 1771581600)
            claim(SdJwtVcSpec.VCT, DocType.PID_SD_JWT.uri)
            objClaim("status") {
                objClaim("status_list") {
                    claim("idx", pid.locationStatus.idx)
                    claim("uri", pid.locationStatus.uri)
                }
            }
            cnf(keyAttestation.jwk.toPublicJWK())

            // Mandatory attributes specified in CIR 2024/2977
            sdClaim("family_name", pid.familyName)
            sdClaim("given_name", pid.givenName)
            sdClaim("birthdate", pid.birthdate.toString())
            sdObjClaim("place_of_birth") {
                pid.placeOfBirth.country?.let { country -> sdClaim("country", country) }
                pid.placeOfBirth.region?.let { region -> sdClaim("region", region) }
                pid.placeOfBirth.locality?.let { locality -> sdClaim("locality", locality) }
            }
            sdArrClaim("nationalities") {
                pid.nationalities.forEach {
                    sdClaim(it)
                }
            }

            // Optional attributes specified in CIR 2024/2977
            pid.address?.let {
                sdObjClaim("address") {
                    it.formatted?.let { formatted -> sdClaim("formatted", formatted) }
                    it.country?.let { country -> sdClaim("country", country) }
                    it.region?.let { region -> sdClaim("region", region) }
                    it.locality?.let { locality -> sdClaim("locality", locality) }
                    it.postalCode?.let { postalCode -> sdClaim("postal_code", postalCode) }
                    it.streetAddress?.let { streetAddress -> sdClaim("street_address", streetAddress) }
                    it.houseNumber?.let { houseNumber -> sdClaim("house_number", houseNumber) }
                }
            }
            sdClaim("personal_administrative_number", pid.personalAdministrativeNumber)
            pid.picture?.let { sdClaim("picture", Base64.encode(it).toString()) }
            sdClaim("birth_family_name", pid.birthFamilyName)
            sdClaim("birth_given_name", pid.birthGivenName)
            pid.sex?.let { sdClaim("sex", it) }
            pid.email?.let { sdClaim("email", it) }
            pid.phoneNumber?.let { sdClaim("phone_number", it) }

            // Mandatory metadata specified in CIR 2024/2977
            sdClaim("date_of_expiry", pid.dateOfExpiry.toString())
            sdClaim("issuing_authority", pid.issuingAuthority)
            sdClaim("issuing_country", pid.issuingCountry)

            // Optional metadata specified in CIR 2024/2977
            pid.documentNumber?.let { sdClaim("document_number", it) }
            pid.issuingJurisdiction?.let { sdClaim("issuing_jurisdiction", it) }

            // Additional optional attributes specified in ARF Rulebook
            sdClaim("date_of_issuance", pid.dateOfIssuance.toString())
            pid.trustAnchor?.let { sdClaim("trust_anchor", it) }
        }
        val keyPair = pidIssuerKeyPair()
        val issuer = NimbusSdJwtOps.issuer(signer = keyPair.jwsSigner(), signAlgorithm = JWSAlgorithm.ES256) {
            type(JOSEObjectType(SupportedFormat.SD_JWT.value))
            x509CertChain(keyPair.parsedX509CertChain.map { Base64.encode(it.encoded) })
        }
        return issuer.issue(sdJwtSpec).getOrThrow().serialize()
    }

    suspend fun issueMDocPid(pid: Credential.MdocPidCredential, keyAttestation: KeyAttestation): String {
        val deviceKeyInfo = DeviceKeyInfo(
            DataElement.fromCBOR(
                OneKey(keyAttestation.jwk.toECKey().toPublicKey(), null).AsCBOR().EncodeToBytes()
            )
        )
        val mdoc = MDocBuilder(DocType.PID.uri)

        // Mandatory attributes specified in CIR 2024/2977
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "family_name", StringElement(pid.familyName))
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "given_name", StringElement(pid.givenName))
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "birth_date", FullDateElement(pid.birthDate))
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "birth_place", StringElement(pid.birthPlace))
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "nationality", ListElement(pid.nationality.map { StringElement(it) }))

        // Optional attributes specified in CIR 2024/2977
        pid.residentAddress?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "resident_address", StringElement(it))
        }
        pid.residentCountry?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "resident_country", StringElement(it))
        }
        pid.residentState?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "resident_state", StringElement(it))
        }
        pid.residentCity?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "resident_city", StringElement(it))
        }
        pid.residentPostalCode?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "resident_postal_code", StringElement(it))
        }
        pid.residentStreet?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "resident_street", StringElement(it))
        }
        pid.residentHouseNumber?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "resident_house_number", StringElement(it))
        }
        mdoc.addItemToSign(
            Namespace.EU_EUROPA_EC_EUDI_PID_1.uri,
            "personal_administrative_number",
            StringElement(pid.personalAdministrativeNumber)
        )
        pid.portrait?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "portrait", ByteStringElement(it))
        }
        mdoc.addItemToSign(
            Namespace.EU_EUROPA_EC_EUDI_PID_1.uri,
            "family_name_birth",
            StringElement(pid.familyNameBirth)
        )
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "given_name_birth", StringElement(pid.givenNameBirth))
        pid.sex?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "sex", NumberElement(it))
        }
        pid.emailAddress?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "email_address", StringElement(it))
        }
        pid.mobilePhoneNumber?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "mobile_phone_number", StringElement(it))
        }
        // Mandatory metadata specified in CIR 2024/2977
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "expiry_date", FullDateElement(pid.expiryDate))
        mdoc.addItemToSign(
            Namespace.EU_EUROPA_EC_EUDI_PID_1.uri,
            "issuing_authority",
            StringElement(pid.issuingAuthority)
        )
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "issuing_country", StringElement(pid.issuingCountry))
        // Optional metadata specified in CIR 2024/2977
        pid.documentNumber?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "document_number", StringElement(it))
        }
        pid.issuingJurisdiction?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "issuing_jurisdiction", StringElement(it))
        }
        // Additional optional attributes specified in ARF Rulebook
        mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "issuance_date", FullDateElement(pid.issuanceDate))
        pid.trustAnchor?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "trust_anchor", StringElement(it))
        }
        pid.attestationLegalCategory?.let {
            mdoc.addItemToSign(Namespace.EU_EUROPA_EC_EUDI_PID_1.uri, "attestation_legal_category", StringElement(pid.attestationLegalCategory))
        }

        val status = Status(
            statusList = StatusListInfo(
                index = pid.locationStatus.idx.toUInt(),
                uri = pid.locationStatus.uri
            )
        )

        val signed = Instant.fromEpochSeconds(1779711303L)
        return mdoc.sign(
            ValidityInfo(signed, signed, Instant.fromEpochSeconds(2095329603L)),
            deviceKeyInfo,
            pidIssuerCryptoProvider(),
            getPidKeyAlias(),
            status
        ).toCBORHex()
    }

    suspend fun issueMDocMdl(mdl: Credential.MdocMdlCredential, keyAttestation: KeyAttestation): String {
        val deviceKeyInfo = DeviceKeyInfo(
            DataElement.fromCBOR(
                OneKey(keyAttestation.jwk.toECKey().toPublicKey(), null).AsCBOR().EncodeToBytes()
            )
        )

        fun getDrivingPrivileges(mdl: Credential.MdocMdlCredential): ListElement {
            return ListElement(
                mdl.drivingPrivileges.map { dp ->
                    MapElement(
                        mapOf(
                            MapKey("vehicle_category_code") to StringElement(dp.vehicleCategoryCode),
                            *listOfNotNull(
                                dp.issueDate?.let { MapKey("issue_date") to FullDateElement(it) },
                                dp.expiryDate?.let { MapKey("expiry_date") to FullDateElement(it) },
                                dp.codes?.takeIf { it.isNotEmpty() }?.let {
                                    MapKey("codes") to ListElement(
                                        it.map { code ->
                                            MapElement(
                                                buildMap {
                                                    put(MapKey("code"), StringElement(code.code))
                                                    code.sign?.let { put(MapKey("sign"), StringElement(it)) }
                                                    code.value?.let { put(MapKey("value"), StringElement(it)) }
                                                }
                                            )
                                        }
                                    )
                                }
                            ).toTypedArray()
                        )
                    )
                }
            )
        }

        val mdoc = MDocBuilder(DocType.MDL.uri)
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "family_name", StringElement(mdl.familyName))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "given_name", StringElement(mdl.givenName))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "birth_date", FullDateElement(mdl.birthDate))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "issue_date", FullDateElement(mdl.issueDate))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "expiry_date", FullDateElement(mdl.expiryDate))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "issuing_country", StringElement(mdl.issuingCountry))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "issuing_authority", StringElement(mdl.issuingAuthority))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "document_number", StringElement(mdl.documentNumber))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "portrait", ByteStringElement(mdl.portrait))
            .addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "driving_privileges", getDrivingPrivileges(mdl))
            .addItemToSign(
                Namespace.ORG_ISO_18013_5_1.uri,
                "un_distinguishing_sign",
                StringElement(mdl.unDistinguishingSign)
            )

        mdl.administrativeNumber?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "administrative_number", StringElement(it))
        }
        mdl.sex?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "sex", NumberElement(it))
        }
        mdl.height?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "height", NumberElement(it))
        }
        mdl.weight?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "weight", NumberElement(it))
        }
        mdl.eyeColor?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "eye_colour", StringElement(it))
        }
        mdl.hairColor?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "hair_colour", StringElement(it))
        }
        mdl.birthPlace?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "birth_place", StringElement(it))
        }
        mdl.residentAddress?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "resident_address", StringElement(it))
        }
        mdl.portraitCaptureDate?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "portrait_capture_date", FullDateElement(it))
        }
        mdl.ageInYears?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "age_in_years", NumberElement(it))
        }
        mdl.ageBirthYear?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "age_birth_year", NumberElement(it))
        }
        mdl.ageOver16?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "age_over_16", BooleanElement(it))
        }
        mdl.ageOver18?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "age_over_18", BooleanElement(it))
        }
        mdl.ageOver21?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "age_over_21", BooleanElement(it))
        }
        mdl.issuingJurisdiction?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "issuing_jurisdiction", StringElement(it))
        }
        mdl.nationality?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "nationality", StringElement(it))
        }
        mdl.residentCity?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "resident_city", StringElement(it))
        }
        mdl.residentState?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "resident_state", StringElement(it))
        }
        mdl.residentPostalCode?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "resident_postal_code", StringElement(it))
        }
        mdl.residentCountry?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "resident_country", StringElement(it))
        }
        mdl.biometricTemplateFace?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "biometric_template_face", ByteStringElement(it))
        }
        mdl.biometricTemplateFinger?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "biometric_template_finger", ByteStringElement(it))
        }
        mdl.biometricTemplateSignatureSign?.let {
            mdoc.addItemToSign(
                Namespace.ORG_ISO_18013_5_1.uri,
                "biometric_template_signature_sign",
                ByteStringElement(it)
            )
        }
        mdl.biometricTemplateIris?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "biometric_template_iris", ByteStringElement(it))
        }
        mdl.familyNameNationalCharacter?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "family_name_national_character", StringElement(it))
        }
        mdl.givenNameNationalCharacter?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "given_name_national_character", StringElement(it))
        }
        mdl.signatureUsualMark?.let {
            mdoc.addItemToSign(Namespace.ORG_ISO_18013_5_1.uri, "signature_usual_mark", ByteStringElement(it))
        }

        val status = Status(
            statusList = StatusListInfo(
                index = mdl.locationStatus.idx.toUInt(),
                uri = mdl.locationStatus.uri
            )
        )

        val signed = Instant.fromEpochSeconds(1779711303L)
        var validUntil = Instant.fromEpochSeconds(2095329603L)
        mdl.validUntil?.let {
            validUntil = it
        }
        return mdoc.sign(
            ValidityInfo(signed, signed, validUntil),
            deviceKeyInfo,
            mdlIssuerCryptoProvider(),
            getMdlKeyAlias(),
            status
        ).toCBORHex()
    }

    suspend fun pidIssuerCryptoProvider(): SimpleCOSECryptoProvider {
        val keyPair = pidIssuerKeyPair()
        return SimpleCOSECryptoProvider(
            listOf(
                COSECryptoProviderKeyInfo(
                    keyID = keyPair.keyID,
                    algorithmID = AlgorithmID.ECDSA_256,
                    publicKey = keyPair.toPublicKey(),
                    privateKey = keyPair.toPrivateKey(),
                    x5Chain = keyPair.parsedX509CertChain,
                    trustedRootCAs = emptyList()
                )
            )
        )
    }

    suspend fun mdlIssuerCryptoProvider(): SimpleCOSECryptoProvider {
        val keyPair = mdlIssuerKeyPair()
        return SimpleCOSECryptoProvider(
            listOf(
                COSECryptoProviderKeyInfo(
                    keyID = keyPair.keyID,
                    algorithmID = AlgorithmID.ECDSA_256,
                    publicKey = keyPair.toPublicKey(),
                    privateKey = keyPair.toPrivateKey(),
                    x5Chain = keyPair.parsedX509CertChain,
                    trustedRootCAs = emptyList()
                )
            )
        )
    }

    suspend fun ageVerificationIssuerCryptoProvider(): SimpleCOSECryptoProvider {
        val keyPair = ageVerificationIssuerKeyPair()
        return SimpleCOSECryptoProvider(
            listOf(
                COSECryptoProviderKeyInfo(
                    keyID = keyPair.keyID,
                    algorithmID = AlgorithmID.ECDSA_256,
                    publicKey = keyPair.toPublicKey(),
                    privateKey = keyPair.toPrivateKey(),
                    x5Chain = keyPair.parsedX509CertChain,
                    trustedRootCAs = emptyList()
                )
            )
        )
    }

    suspend fun ageVerificationIssuerKeyPair(): ECKey {
        val alias = getMdlKeyAlias()
        val keyStore = KeyStore.getInstance("PKCS12")
        context.assets.open("keys/doc_signer_mdl_issuer.p12").use { inputStream ->
            keyStore.load(inputStream, "changeit".toCharArray())
        }
        return ECKey.load(keyStore, alias, "changeit".toCharArray())
    }

    suspend fun pidIssuerKeyPair(): ECKey {
        val alias = getPidKeyAlias()
        val keyStore = KeyStore.getInstance("PKCS12")
        context.assets.open("keys/doc_signer_pid_issuer.p12").use { inputStream ->
            keyStore.load(inputStream, "changeit".toCharArray())
        }
        return ECKey.load(keyStore, alias, "changeit".toCharArray())
    }

    suspend fun mdlIssuerKeyPair(): ECKey {
        val alias = getMdlKeyAlias()
        val keyStore = KeyStore.getInstance("PKCS12")
        context.assets.open("keys/doc_signer_mdl_issuer.p12").use { inputStream ->
            keyStore.load(inputStream, "changeit".toCharArray())
        }
        return ECKey.load(keyStore, alias, "changeit".toCharArray())
    }

    suspend fun getMdlKeyAlias(): String {
        val issuerKeyType = userPreferencesDataSource.userPreferences.first().issuerKeyType
        return when (issuerKeyType) {
            IssuerKeyType.IACA_TRUSTED -> "mdl_issuer"
            IssuerKeyType.UNTRUSTED -> "mdl_issuer_untrusted"
        }
    }

    suspend fun getPidKeyAlias(): String {
        val issuerKeyType = userPreferencesDataSource.userPreferences.first().issuerKeyType
        return when (issuerKeyType) {
            IssuerKeyType.IACA_TRUSTED -> "pid_issuer"
            IssuerKeyType.UNTRUSTED -> "pid_issuer_untrusted"
        }
    }

    suspend fun issueMDocAgeVerification(ageVerification: Credential.AgeVerificationCredential, keyAttestation: KeyAttestation): String {
        val deviceKeyInfo = DeviceKeyInfo(
            DataElement.fromCBOR(
                OneKey(keyAttestation.jwk.toECKey().toPublicKey(), null).AsCBOR().EncodeToBytes()
            )
        )

        // Spec §9.2 / conformance plan §4 item 6: the AV Profile attestation carries its single
        // mandatory attribute age_over_18 and nothing else — the Commission's blueprint defines
        // no other attribute, so the threshold zoo the older mock minted is gone.
        val mdoc = MDocBuilder(DocType.AGE_VERIFICATION.uri)
            .addItemToSign(Namespace.EU_EUROPA_EC_EUDI_AGE_VERIFICATION_1.uri, "age_over_18", BooleanElement(ageVerification.ageOver18))

        val status = Status(
            statusList = StatusListInfo(
                index = ageVerification.locationStatus.idx.toUInt(),
                uri = ageVerification.locationStatus.uri
            )
        )

        val signed = Instant.fromEpochSeconds(1779711303L)
        return mdoc.sign(
            ValidityInfo(signed, signed, Instant.fromEpochSeconds(2095329603L)),
            deviceKeyInfo,
            ageVerificationIssuerCryptoProvider(),
            getMdlKeyAlias(),
            status
        ).toCBORHex()
    }

    /**
     * The EE Proof of Age attestation, spec §9.2 (conformance plan §4 item 6): exactly the
     * mandatory predicate `age_over_18` plus the mandatory metadata attributes
     * `issuing_country`, `issuing_authority` and `expiry_date` (EE-POA-001). No status
     * reference: a single-use attestation is consumed on presentation (EE-POA-013), not revoked.
     */
    suspend fun issueMDocEePoa(poa: Credential.EePoaCredential, keyAttestation: KeyAttestation): String {
        val issuanceDay = issuanceDay()
        require(poa.expiryDate in issuanceDay..issuanceDay.plus(AgeIssuanceConstants.EE_POA_MAX_VALIDITY_DAYS, DateTimeUnit.DAY)) {
            "EE-PoA validity must start at issuance and span at most ${AgeIssuanceConstants.EE_POA_MAX_VALIDITY_DAYS} days"
        }

        val deviceKeyInfo = DeviceKeyInfo(
            DataElement.fromCBOR(
                OneKey(keyAttestation.jwk.toECKey().toPublicKey(), null).AsCBOR().EncodeToBytes()
            )
        )

        val mdoc = MDocBuilder(DocType.EE_POA.uri)
            .addItemToSign(Namespace.EE_RIIK_POA_1.uri, "age_over_18", BooleanElement(poa.ageOver18))
            .addItemToSign(Namespace.EE_RIIK_POA_1.uri, "issuing_country", StringElement(poa.issuingCountry))
            .addItemToSign(Namespace.EE_RIIK_POA_1.uri, "issuing_authority", StringElement(poa.issuingAuthority))
            .addItemToSign(Namespace.EE_RIIK_POA_1.uri, "expiry_date", FullDateElement(poa.expiryDate))

        // EE-POA-012: signed and validFrom coarsened to 00:00:00Z of the issuance day.
        // EE-POA-016: validUntil is the expiry_date, at most 90 days out.
        // EE-POA-017: no status reference — null Status writes none into the MSO.
        val signed = issuanceDay.atStartOfDayIn(TimeZone.UTC)
        val issuerKeyPair = mdlIssuerKeyPair()
        return mdoc.sign(
            ValidityInfo(signed, signed, poa.expiryDate.atStartOfDayIn(TimeZone.UTC)),
            deviceKeyInfo,
            mdlIssuerCryptoProvider(),
            issuerKeyPair.keyID,
            null
        ).toCBORHex()
    }

    companion object {
        /**
         * The issuance day, midnight UTC, that every batch's ValidityInfo anchors to
         * (EE-POA-012): the current UTC date, so validity is measured from the day of issuance.
         */
        internal fun issuanceDay(): LocalDate {
            val javaDay = java.time.Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
            return LocalDate(javaDay.year, javaDay.monthValue, javaDay.dayOfMonth)
        }
    }
}
