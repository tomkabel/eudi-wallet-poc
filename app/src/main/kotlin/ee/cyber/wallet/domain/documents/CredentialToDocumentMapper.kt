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
    private val trustAnchors: List<X509Certificate>,
    // E6: the IACA root(s) issuer mdoc chains must anchor at (the asset
    // keys/iaca_root.cer.pem in the dev setup). Kept separate from the SD-JWT
    // x5c anchors so one list can tighten without moving the other.
    private val issuerAnchors: List<X509Certificate> = trustAnchors
) {

    private val logger = LoggerFactory.getLogger("CredentialToDocumentMapper")

    suspend fun convert(attestation: Attestation): CredentialDocument? {
        return when (attestation.type) {
            CredentialType.PID_SD_JWT -> loadSdJwtCredential(attestation)?.asDocument(attestation)
            CredentialType.PID_MDOC -> MDoc.fromCBORHex(attestation.credential).asDocument(attestation)
            CredentialType.MDL -> MDoc.fromCBORHex(attestation.credential).asDocument(attestation)
            CredentialType.AGE_VERIFICATION -> MDoc.fromCBORHex(attestation.credential).asDocument(attestation)
            CredentialType.EE_POA -> MDoc.fromCBORHex(attestation.credential).asDocument(attestation)
        }
    }

    private fun simpleCertificateChainValidator(trustAnchors: List<X509Certificate>) = X509CertificateTrust {
        // B4: centralized overload — revocation policy logged at one place; both
        // production call sites flip to enforced together in E4, never one alone.
        CertificateChainValidator.validateCertificateChain(it, trustAnchors)
    }

    /**
     * E6 (identity A-M6 part 2): on-device issuerAuth verification for one mdoc.
     *
     * Two independent checks, both required for `verified = true`:
     *  1. Signature: the issuerAuth COSE_Sign1 (the MSO's signature envelope) validates
     *     with the leaf certificate of the x5chain it carries — the issuer really signed
     *     this MSO with the key the chain certifies. Verified through org.cose
     *     Sign1Message.validate over the waltid COSESign1's own CBOR bytes (the
     *     context string, external-data default and raw-r||s-to-DER conversion are
     *     handled by the same cose-java build the issuer signed with).
     *  2. Chain: the x5chain anchors at [issuerAnchors] through the centralized
     *     [CertificateChainValidator] — same PKIX + E4 CRL soft-fail policy as every
     *     other trust decision in the wallet.
     *
     * Failure semantics: a failed check logs loudly and yields `verified = false` —
     * the credential still maps (the holder keeps their record and the UI shows the
     * unverified state); it is never silently passed as verified, and never refused
     * outright (the convert call sites' `!!` would turn refusal into a crash at
     * issuance/presentation — the D15 fix class).
     */
    private fun MDoc.verifyIssuerAuth(): Boolean {
        val issuerAuth = issuerSigned.issuerAuth ?: return false.also {
            logger.error("mdoc carries no issuerAuth — credential cannot be verified (docType=${docType.value})")
        }
        val chainBytes = issuerAuth.x5Chain
        if (chainBytes.isNullOrEmpty()) {
            logger.error("issuerAuth carries no x5chain — signature has no certified key (docType=${docType.value})")
            return false
        }
        val chain = try {
            val factory = java.security.cert.CertificateFactory.getInstance("X509")
            chainBytes.map { factory.generateCertificate(java.io.ByteArrayInputStream(it)) as X509Certificate }
        } catch (e: Exception) {
            logger.error("issuerAuth x5chain does not parse as X.509: {}", e.toString())
            return false
        }
        val signatureOk = try {
            // waltid serializes COSE_Sign1 as the untagged 4-element array; cose-java's
            // decoder needs the tag supplied explicitly (MessageTag.Sign1 = CBOR tag 18).
            val message = org.cose.java.Message.DecodeFromBytes(
                issuerAuth.toCBOR(),
                org.cose.java.MessageTag.Sign1
            ) as org.cose.java.Sign1Message
            message.validate(org.cose.java.OneKey(chain.first().publicKey, null))
        } catch (e: Exception) {
            logger.error("issuerAuth signature did not verify with the presented leaf: {}", e.toString())
            false
        }
        if (!signatureOk) return false
        val chainOk = CertificateChainValidator.validateCertificateChain(chain, issuerAnchors)
        if (!chainOk) logger.error("issuerAuth x5chain does not anchor at the configured IACA roots (docType=${docType.value})")
        return chainOk
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
                        // D15 (jvm L5): `it` is a Map.Entry whose value came from
                        // DataElement.value() — null for element shapes this mapper does not
                        // model (it.value!! crashed the whole conversion). Render the mapped
                        // form when present, else the raw CBOR element's toString, so the
                        // field stays visible instead of killing the document load.
                        value = (it.value ?: it.toString()).toString()
                    )
                }
            }
        }
        val expiresAt = fields.find { it.name == CredentialAttribute.MDOC_PID_1_EXPIRY_DATE.fieldName }?.value?.let { LocalDate.parse(it) }
        // E6: the MSO's own validity window is authoritative when present — the
        // expiry_date document field above is issuer-chosen display data, while
        // validFrom/validUntil is what the issuer actually signed in the MSO. A window
        // miss folds into `expired` (same semantics as D15's exp handling: an unknown
        // window stays unexpired, a signed-and-missed window marks the credential dead).
        val msoWindow = runCatching {
            val validity = MSO?.validityInfo
            validity?.let { Triple(it.validFrom.value, it.validUntil.value, it.signed.value) }
        }.getOrNull()
        val msoExpired = msoWindow?.let { (from, until, _) ->
            val now = kotlin.time.Clock.System.now()
            now < from || now > until
        } ?: false
        return CredentialDocument.MDocDocument(
            id = attestation.id,
            type = attestation.type.docType(),
            fields = fields.sortedBy { it.asCredentialAttribute(attestation.type.docType()) },
            expired = (expiresAt?.let { LocalDate.now(ZoneOffset.UTC).isAfter(it) } ?: false) || msoExpired,
            attestation = attestation,
            verified = verifyIssuerAuth(),
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

        // D15 (jvm L5): exp!! killed the whole document load when an SD-JWT lacked the
        // standard `exp` claim (the SD-JWT VC spec makes it OPTIONAL — registered claims
        // live in the -disclosed- set only if the issuer chose to include them). A missing
        // or unparseable exp means "expiry unknown", not "credential dead".
        val expiresAt = jwtClaims["exp"]?.toLongOrNull()?.let {
            LocalDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneOffset.UTC)
        }

        return when (val vct = jwtClaims["vct"]) {
            DocType.PID_SD_JWT.uri -> {
                CredentialDocument.JwtDocument(
                    id = attestation.id,
                    fields = fields.sortedBy { it.asCredentialAttribute(DocType.PID_SD_JWT) },
                    type = DocType.PID_SD_JWT,
                    expired = expiresAt?.let { LocalDateTime.now(ZoneOffset.UTC).isAfter(it) } ?: false,
                    attestation = attestation,
                    // E6: reaching here means the SD-JWT x5c chain already verified through
                    // the same centralized validator (loadSdJwtCredential is the verify gate).
                    verified = true,
                    sdJwt = this
                )
            }

            else -> throw IllegalArgumentException("Unsupported credential type: $vct")
        }
    }
}
