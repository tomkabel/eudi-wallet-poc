package ee.cyber.wallet.domain.documents

import ee.cyber.wallet.domain.credentials.CredentialAttribute
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.credentials.Namespace
import ee.cyber.wallet.domain.documents.mdoc.value
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.security.CertificateChainValidator
import ee.cyber.wallet.ui.screens.documents.asCredentialAttribute
import ee.cyber.wallet.ui.screens.documents.docType

import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.Disclosure
import eu.europa.ec.eudi.sdjwt.JwtAndClaims
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.name
import eu.europa.ec.eudi.sdjwt.recreateClaimsAndDisclosuresPerClaim
import eu.europa.ec.eudi.sdjwt.value
import eu.europa.ec.eudi.sdjwt.vc.ClaimPath
import eu.europa.ec.eudi.sdjwt.vc.X509CertificateTrust
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.doc.MDoc
import id.walt.mdoc.issuersigned.IssuerSignedItem
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.slf4j.LoggerFactory
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class CredentialToDocumentMapper(
    private val trustAnchors: List<X509Certificate>
) {

    private val logger = LoggerFactory.getLogger("CredentialToDocumentMapper")

    suspend fun convert(attestation: Attestation): CredentialDocument? {
        return when (attestation.type) {
            CredentialType.PID_SD_JWT -> loadSdJwtCredential(attestation)?.asDocument(attestation)
            CredentialType.PID_MDOC -> MDoc.fromCBORHex(attestation.credential).asDocument(attestation)
            CredentialType.MDL -> MDoc.fromCBORHex(attestation.credential).asDocument(attestation)
            CredentialType.AGE_VERIFICATION -> MDoc.fromCBORHex(attestation.credential).asDocument(attestation)
        }
    }

    private fun simpleCertificateChainValidator(trustAnchors: List<X509Certificate>) = X509CertificateTrust {
        CertificateChainValidator.validateCertificateChain(it, trustAnchors, false)
    }

    private suspend fun loadSdJwtCredential(attestation: Attestation): SdJwt<JwtAndClaims>? =
        DefaultSdJwtOps.SdJwtVcVerifier.usingX5c(simpleCertificateChainValidator(trustAnchors))
            .verify(attestation.credential)
            .onFailure { logger.error("failure: ", it) }
            .getOrNull()

    private fun MDoc.asDocument(attestation: Attestation): CredentialDocument.MDocDocument {
        val elements = nameSpaces.associateWith { ns ->
            getIssuerSignedItems(ns).associate {
                if (it.elementIdentifier.value.equals("driving_privileges")) {
                    it.elementIdentifier.value to getDrivingPrivileges(it)
                } else {
                    it.elementIdentifier.value to it.elementValue.value()
                }
            }
        }
        val fields = elements.flatMap { nsGroup ->
            val namespace = nsGroup.key.let { ns -> Namespace.byUri(ns) }
            if (namespace == null) {
                emptyList()
            } else {
                nsGroup.value.map {
                    DocumentField(
                        namespace = namespace,
                        name = it.key,
                        value = it.value!!.toString()
                    )
                }
            }
        }
        val expiresAt = fields.find { it.name == CredentialAttribute.MDOC_PID_1_EXPIRY_DATE.fieldName }?.value?.let { LocalDate.parse(it) }
        return CredentialDocument.MDocDocument(
            id = attestation.id,
            type = attestation.type.docType(),
            fields = fields.sortedBy { it.asCredentialAttribute(attestation.type.docType()) },
            expired = expiresAt?.let { LocalDate.now(ZoneOffset.UTC).isAfter(it) } ?: false,
            attestation = attestation,
            mDoc = this
        )
    }

    private fun getDrivingPrivileges(it: IssuerSignedItem): String {
        return (it.elementValue as ListElement).value.joinToString("\n") { element ->
            val privilegesKeyOrder = listOf("vehicle_category_code", "issue_date", "expiry_date", "codes")
            val codesKeyOrder = listOf("code", "sign", "value")
            (element as MapElement).value.entries
                .sortedBy { entry -> privilegesKeyOrder.indexOf(entry.key.toString()) }
                .joinToString(", ") { entry ->
                    if (entry.key.toString() == "codes") {
                        "\n" + (entry.value as ListElement).value.joinToString(", ") { codeElement ->
                            (codeElement as MapElement).value.entries
                                .sortedBy { entry -> codesKeyOrder.indexOf(entry.key.toString()) }
                                .joinToString(" ") { codeEntry ->
                                    "${codeEntry.value.internalValue}"
                                }
                        }
                    } else {
                        entry.value.internalValue.toString()
                    }
                }
        }
    }

    private fun extractSelectivelyDisclosableClaims(
        reconstructedClaims: JsonObject,
        disclosuresPerPath: Map<ClaimPath, List<Disclosure>>
    ): JsonObject {
        // Get all claim names that have disclosures (selectively disclosable)
        val sdClaimNames = mutableSetOf<String>()

        disclosuresPerPath.values.flatten().forEach { disclosure ->
            // disclosure.claim() returns Claim (Pair<String, JsonElement>)
            // .name() extension gets the first element (claim name)
            sdClaimNames.add(disclosure.claim().name())
        }

        // Recursively filter the JsonObject to only include SD claims
        return filterSelectivelyDisclosableClaims(reconstructedClaims, sdClaimNames)
    }

    private fun filterSelectivelyDisclosableClaims(
        obj: JsonObject,
        sdClaimNames: Set<String>
    ): JsonObject {
        return buildJsonObject {
            obj.forEach { (key, value) ->
                when {
                    // If this key is a selectively disclosable claim, include it
                    sdClaimNames.contains(key) -> {
                        when (value) {
                            is JsonObject -> {
                                // Recursively filter nested objects
                                put(key, filterSelectivelyDisclosableClaims(value, sdClaimNames))
                            }
                            else -> put(key, value)
                        }
                    }
                    // If value is an object, check if it contains any SD claims
                    value is JsonObject -> {
                        val filtered = filterSelectivelyDisclosableClaims(value, sdClaimNames)
                        if (filtered.isNotEmpty()) {
                            put(key, filtered)
                        }
                    }
                }
            }
        }
    }

    private fun flattenJsonObject(
        obj: JsonObject,
        prefix: String = ""
    ): List<DocumentField> {
        val fields = mutableListOf<DocumentField>()

        obj.forEach { (key, value) ->
            val fullName = if (prefix.isEmpty()) key else "$prefix.$key"

            when (value) {
                is JsonPrimitive -> {
                    fields.add(
                        DocumentField(
                            namespace = Namespace.NONE,
                            name = fullName,
                            value = value.content,
                            element = value
                        )
                    )
                }
                is JsonObject -> {
                    // Recursively flatten nested objects
                    fields.addAll(flattenJsonObject(value, fullName))
                }
                is JsonArray -> {
                    // Keep arrays as JSON string
                    fields.add(
                        DocumentField(
                            namespace = Namespace.NONE,
                            name = fullName,
                            value = value.toString(),
                            element = value
                        )
                    )
                }
            }
        }

        return fields
    }


    private fun SdJwt<JwtAndClaims>.asDocument(attestation: Attestation): CredentialDocument.JwtDocument {
        val jwtClaims = jwt.second
            .filter { it.value is JsonPrimitive }
            .map { Pair(it.key, (it.value as JsonPrimitive).content) }
            .toMap()

        // Use recreateClaimsAndDisclosuresPerClaim to properly handle nested structures
        val (reconstructedClaims, disclosuresPerPath) = with(DefaultSdJwtOps) {
            recreateClaimsAndDisclosuresPerClaim()
        }

        // Extract only selectively disclosable claims (exclude standard JWT claims like iss, exp, vct)
        val sdOnlyClaims = extractSelectivelyDisclosableClaims(reconstructedClaims, disclosuresPerPath)
        val fields = flattenJsonObject(sdOnlyClaims)

        val expiresAt = LocalDateTime.ofInstant(Instant.ofEpochSecond(jwtClaims["exp"]!!.toLong()), ZoneOffset.UTC)

        return when (val vct = jwtClaims["vct"]) {
            DocType.PID_SD_JWT.uri -> {
                CredentialDocument.JwtDocument(
                    id = attestation.id,
                    fields = fields.sortedBy { it.asCredentialAttribute(DocType.PID_SD_JWT) },
                    type = DocType.PID_SD_JWT,
                    expired = LocalDateTime.now(ZoneOffset.UTC).isAfter(expiresAt),
                    attestation = attestation,
                    sdJwt = this
                )
            }

            else -> throw IllegalArgumentException("Unsupported credential type: $vct")
        }
    }
}
