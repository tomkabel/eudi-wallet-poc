package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.ui.screens.documents.credentialType
import ee.cyber.wallet.ui.screens.presentation.Credential
import ee.cyber.wallet.ui.screens.presentation.MatchedField
import eu.europa.ec.eudi.openid4vp.dcql.DCQL
import eu.europa.ec.eudi.openid4vp.dcql.ClaimPathElement
import eu.europa.ec.eudi.openid4vp.dcql.ClaimsQuery
import eu.europa.ec.eudi.openid4vp.dcql.CredentialQuery
import eu.europa.ec.eudi.openid4vp.dcql.metaMsoMdoc
import eu.europa.ec.eudi.openid4vp.dcql.metaSdJwtVc
import org.slf4j.LoggerFactory

/**
 * Processes DCQL (Digital Credentials Query Language) queries to match credentials
 * from the wallet's document repository against verifier requirements.
 *
 * This processor handles both mso_mdoc and dc+sd-jwt credential formats.
 */
class DcqlRequestProcessor {

    private val log = LoggerFactory.getLogger("DcqlRequestProcessor")

    /**
     * Processes a DCQL query against available documents and returns matched credentials
     * with their required and optional fields.
     *
     * @param dcqlQuery The DCQL query from the presentation request
     * @param documents List of available credential documents in the wallet
     * @return List of matched Credentials with their fields, or null if no match found
     */
    fun process(
        dcqlQuery: DCQL,
        documents: List<CredentialDocument>
    ): List<Credential>? {
        val credentials = dcqlQuery.credentials.value
        log.info("Processing DCQL query with ${credentials.size} credential queries")

        val matchedCredentials = mutableListOf<Credential>()

        for (credentialQuery in credentials) {
            val formatValue = credentialQuery.format.value
            log.info("Processing credential query: id=${credentialQuery.id}, format=$formatValue")

            val credential = when (formatValue) {
                "mso_mdoc" -> processMsoMdocQuery(credentialQuery, documents)
                "dc+sd-jwt" -> processSdJwtVcQuery(credentialQuery, documents)
                else -> {
                    log.warn("Unsupported credential format: $formatValue")
                    null
                }
            }

            if (credential != null) {
                matchedCredentials.add(credential)
            } else {
                log.warn("No matching document found for credential query: ${credentialQuery.id}")
                // If any required credential is not found, return null
                return null
            }
        }

        if (matchedCredentials.isEmpty()) {
            log.info("No credentials matched the DCQL query")
            return null
        }

        log.info("Successfully matched ${matchedCredentials.size} credentials")
        return matchedCredentials
    }

    /**
     * Processes an mso_mdoc credential query.
     *
     * For mso_mdoc format:
     * - Matches documents by doctype_value from meta
     * - Claims use path with 2 elements: [namespace, claim_name]
     * - If no claims specified, includes all available claims from matched documents
     */
    private fun processMsoMdocQuery(
        credentialQuery: CredentialQuery,
        documents: List<CredentialDocument>
    ): Credential? {
        val meta = credentialQuery.metaMsoMdoc
        val doctypeValue = meta?.doctypeValue

        if (doctypeValue == null) {
            log.warn("mso_mdoc query missing doctype_value in meta")
            return null
        }

        log.info("Matching mso_mdoc with doctype: $doctypeValue")

        // Find matching mDoc documents by doctype
        val matchingDocuments = documents.filterIsInstance<CredentialDocument.MDocDocument>()
            .filter { it.type.uri == doctypeValue.value }

        if (matchingDocuments.isEmpty()) {
            log.warn("No mDoc documents found with doctype: $doctypeValue")
            return null
        }

        // Use the first matching document
        val document = matchingDocuments.first()
        log.info("Found matching mDoc document: ${document.id}")

        val claims = credentialQuery.claims

        return if (claims.isNullOrEmpty()) {
            // OpenID4VP §6.4.1: an absent claims list requests no selectively disclosable
            // claims. Offer the fields unchecked, so nothing is disclosed unless the holder opts in.
            log.info("No specific claims requested, offering ${document.fields.size} fields unchecked")
            Credential(
                credentialType = document.credentialType(),
                fields = emptyList(),
                optionalFields = document.fields.map { MatchedField(it, checked = false) },
                attestation = document.attestation
            )
        } else {
            // Match specific requested claims
            val (requiredFields, optionalFields) = matchMsoMdocClaims(claims, document)
            log.info("Matched ${requiredFields.size} required and ${optionalFields.size} optional fields")

            Credential(
                credentialType = document.credentialType(),
                fields = requiredFields,
                optionalFields = optionalFields,
                attestation = document.attestation
            )
        }
    }

    /**
     * Matches mso_mdoc claims from the query against document fields.
     *
     * For mso_mdoc, the claim path has exactly 2 elements:
     * - path[0]: namespace
     * - path[1]: claim name
     */
    private fun matchMsoMdocClaims(
        claims: List<ClaimsQuery>,
        document: CredentialDocument.MDocDocument
    ): Pair<List<MatchedField>, List<MatchedField>> {
        val requiredFields = mutableListOf<MatchedField>()
        val optionalFields = mutableListOf<MatchedField>()

        for (claimQuery in claims) {
            val pathElements = claimQuery.path.value

            if (pathElements.size != 2) {
                log.warn("Invalid mso_mdoc claim path length: ${pathElements.size}, expected 2")
                continue
            }

            val namespace = (pathElements[0] as? ClaimPathElement.Claim)?.name
            val claimName = (pathElements[1] as? ClaimPathElement.Claim)?.name

            if (namespace == null || claimName == null) {
                log.warn("Invalid mso_mdoc claim path elements")
                continue
            }

            // Find matching field in document
            val matchingField = document.fields.find { field ->
                field.namespace.uri == namespace && field.name == claimName
            }

            if (matchingField != null) {
                val intentToRetain = claimQuery.intentToRetain ?: false
                val isRequired = intentToRetain || claimQuery.values != null

                // Optional claims start unchecked, as on the presentation-exchange path.
                val matchedField = MatchedField(matchingField, checked = isRequired)

                if (isRequired) {
                    requiredFields.add(matchedField)
                } else {
                    optionalFields.add(matchedField)
                }

                log.debug("Matched claim: namespace=$namespace, name=$claimName, required=$isRequired")
            } else {
                log.warn("Claim not found in document: namespace=$namespace, name=$claimName")
            }
        }

        return requiredFields to optionalFields
    }

    /**
     * Processes an SD-JWT VC credential query.
     *
     * For dc+sd-jwt format:
     * - Matches documents by vct_values from meta
     * - Claims use variable-length paths to navigate claim hierarchy
     * - If no claims specified, includes all available claims from matched documents
     */
    private fun processSdJwtVcQuery(
        credentialQuery: CredentialQuery,
        documents: List<CredentialDocument>
    ): Credential? {
        val meta = credentialQuery.metaSdJwtVc
        val vctValues = meta?.vctValues

        if (vctValues == null || vctValues.isEmpty()) {
            log.warn("dc+sd-jwt query missing vct_values in meta")
            return null
        }

        log.info("Matching SD-JWT VC with vct values: $vctValues")

        // Find matching JWT documents by vct (credential type)
        val matchingDocuments = documents.filterIsInstance<CredentialDocument.JwtDocument>()
            .filter { document -> vctValues.contains(document.type.uri) }

        if (matchingDocuments.isEmpty()) {
            log.warn("No JWT documents found with vct values: $vctValues")
            return null
        }

        // Use the first matching document
        val document = matchingDocuments.first()
        log.info("Found matching JWT document: ${document.id}")

        val claims = credentialQuery.claims

        return if (claims.isNullOrEmpty()) {
            // OpenID4VP §6.4.1: an absent claims list requests no selectively disclosable
            // claims. Offer the fields unchecked, so nothing is disclosed unless the holder opts in.
            log.info("No specific claims requested, offering ${document.fields.size} fields unchecked")
            Credential(
                credentialType = document.credentialType(),
                fields = emptyList(),
                optionalFields = document.fields.map { MatchedField(it, checked = false) },
                attestation = document.attestation
            )
        } else {
            // Match specific requested claims
            val (requiredFields, optionalFields) = matchSdJwtVcClaims(claims, document)
            log.info("Matched ${requiredFields.size} required and ${optionalFields.size} optional fields")

            Credential(
                credentialType = document.credentialType(),
                fields = requiredFields,
                optionalFields = optionalFields,
                attestation = document.attestation
            )
        }
    }

    /**
     * Matches SD-JWT VC claims from the query against document fields.
     *
     * For SD-JWT VC, the claim path can have variable length and represents
     * a hierarchical navigation through the claims.
     */
    private fun matchSdJwtVcClaims(
        claims: List<ClaimsQuery>,
        document: CredentialDocument.JwtDocument
    ): Pair<List<MatchedField>, List<MatchedField>> {
        val requiredFields = mutableListOf<MatchedField>()
        val optionalFields = mutableListOf<MatchedField>()

        for (claimQuery in claims) {
            val pathElements = claimQuery.path.value

            if (pathElements.isEmpty()) {
                log.warn("Empty claim path in SD-JWT VC query")
                continue
            }

            // For now, we support simple single-element paths
            // TODO: Add support for nested claim paths
            val claimName = when (val firstElement = pathElements[0]) {
                is ClaimPathElement.Claim -> firstElement.name
                else -> {
                    log.warn("Unsupported claim path element type: $firstElement")
                    continue
                }
            }

            // For nested paths, construct the full path
            val fullPath = if (pathElements.size > 1) {
                pathElements.joinToString(".") { element ->
                    when (element) {
                        is ClaimPathElement.Claim -> element.name
                        is ClaimPathElement.ArrayElement -> "[${element.index}]"
                        is ClaimPathElement.AllArrayElements -> "[*]"
                    }
                }
            } else {
                claimName
            }

            // Find matching field in document
            // For SD-JWT, fields may not have a namespace (empty namespace)
            val matchingField = document.fields.find { field ->
                field.name == claimName || field.name == fullPath
            }

            if (matchingField != null) {
                // For SD-JWT VC, intent_to_retain is not applicable (mso_mdoc only)
                // Determine if required based on presence of values constraint
                val isRequired = claimQuery.values != null

                // Optional claims start unchecked, as on the presentation-exchange path.
                val matchedField = MatchedField(matchingField, checked = isRequired)

                if (isRequired) {
                    requiredFields.add(matchedField)
                } else {
                    optionalFields.add(matchedField)
                }

                log.debug("Matched SD-JWT claim: name=$claimName, path=$fullPath, required=$isRequired")
            } else {
                log.warn("SD-JWT claim not found in document: name=$claimName, path=$fullPath")
            }
        }

        return requiredFields to optionalFields
    }
}
