package ee.cyber.wallet.domain.presentation

import android.util.Base64
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.util.Base64URL
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.security.SecureAreaKeyManager
import ee.cyber.wallet.crypto.deviceCryptoProvider
import ee.cyber.wallet.crypto.keyBindingSigner
import ee.cyber.wallet.domain.credentials.CredentialAttribute
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.documents.DocumentField
import ee.cyber.wallet.domain.documents.mdoc.DeviceResponse
import ee.cyber.wallet.domain.documents.mdoc.MDocUtils.getDeviceAuthentication
import eu.europa.ec.eudi.openid4vp.Consensus
import eu.europa.ec.eudi.openid4vp.EncryptionParameters
import eu.europa.ec.eudi.openid4vp.Resolution
import eu.europa.ec.eudi.openid4vp.ResolvedRequestObject
import eu.europa.ec.eudi.openid4vp.ResponseMode
import eu.europa.ec.eudi.openid4vp.SiopOpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.SiopOpenId4Vp
import eu.europa.ec.eudi.openid4vp.VerifiablePresentation
import eu.europa.ec.eudi.openid4vp.VerifiablePresentations
import eu.europa.ec.eudi.openid4vp.VerifierId
import eu.europa.ec.eudi.openid4vp.dcql.QueryId
import eu.europa.ec.eudi.prex.Format
import eu.europa.ec.eudi.prex.Match
import eu.europa.ec.eudi.prex.PresentationDefinition
import eu.europa.ec.eudi.prex.PresentationExchange
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps
import eu.europa.ec.eudi.sdjwt.DefaultSdJwtOps.serializeWithKeyBinding
import eu.europa.ec.eudi.sdjwt.HashAlgorithm
import eu.europa.ec.eudi.sdjwt.NimbusSdJwtOps.kbJwtIssuer
import eu.europa.ec.eudi.sdjwt.SdJwt
import eu.europa.ec.eudi.sdjwt.vc.ClaimPathElement
import id.walt.mdoc.docrequest.MDocRequestBuilder
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

private val formats = mapOf(
    SupportedFormat.SD_JWT to Format.format(
        buildJsonObject {
            putJsonObject(SupportedFormat.SD_JWT.value) { }
        }
    ),
    SupportedFormat.MSO_MDOC to Format.format(
        buildJsonObject {
            putJsonObject(SupportedFormat.MSO_MDOC.value) { }
        }
    )
)

enum class SupportedFormat(val value: String) {
    SD_JWT("dc+sd-jwt"),
    MSO_MDOC("mso_mdoc")
}

class OpenId4VPManager(
    private val dispatcher: CoroutineDispatcher,
    private val openId4VPConfig: SiopOpenId4VPConfig,
    private val cryptoProviderFactory: CryptoProvider.Factory,
    private val secureAreaKeyManager: SecureAreaKeyManager,
    private val httpClient: HttpClient
) {

    private val logger = LoggerFactory.getLogger("OpenId4VPManager")
    private val openId4Vp: SiopOpenId4Vp by lazy { SiopOpenId4Vp(openId4VPConfig, httpClient) }

    suspend fun handleRequestUri(uri: String): Resolution = withContext(dispatcher) {
        openId4Vp.resolveRequestUri(urlEncodeQueryParam(uri, "dcql_query"))
    }

    private fun urlEncodeQueryParam(uri: String, paramName: String): String { // TODO: Remove. Only used for https://verifier.ageverification.dev/ where dcql_query needs to be url encoded
        val paramPrefix = "$paramName="
        val startIndex = uri.indexOf(paramPrefix)
        if (startIndex == -1) return uri

        val valueStart = startIndex + paramPrefix.length
        val valueEnd = uri.indexOf('&', valueStart).let { if (it == -1) uri.length else it }
        val value = uri.substring(valueStart, valueEnd)
        val encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.toString())

        return uri.substring(0, valueStart) + encodedValue + uri.substring(valueEnd)
    }

    suspend fun sendResponse(requestObject: ResolvedRequestObject, consensus: Consensus) = withContext(dispatcher) {
        EncryptionParameters.DiffieHellman(Base64URL.encode("mdoc_generated_nonce"))
            .let { encryptionParameters -> // TODO: EUDI ref. impl verifier fails if this is not set. Remove encryptionParameters as mdoc generated nonce is no longer required by ISO 18013:7 to be set in apu header.
                openId4Vp.dispatch(requestObject, consensus, encryptionParameters)
            }
    }

    /**
     * Matches credentials against a presentation definition.
     *
     * TODO: The whole combined presentation needs refactoring, due to the unsupported Presentation Exchange submission_requirements feature. The submission_requirements property
     *       defines which Input Descriptors are required for submission, overriding the default input evaluation behavior, in which all Input Descriptors are required.
     *
     * @param presentationDefinition The presentation definition specifying what the verifier wants
     * @param documents List of credential documents that the user has
     * @param disclosedFields Optional list of fields to disclose, defaults to all fields in the documents
     * @return A Pair containing:
     *         - List<CredentialClaim>: The formatted claims derived from the documents
     *         - Match: The match result indicating whether credentials satisfy requirements and which credentials match which requirements. Retrieving the matching document
     *         through the credentialDocument property of the claim whose uniqueId matches the candidateClaimId from the match results:
     *         val candidateClaimId = inputDescriptor.value.keys.first() // This works only for all Input Descriptors evaluator
     *         val credentialDocument = claims.first { it.uniqueId == candidateClaimId }.credentialDocument
     */
    fun matches(
        presentationDefinition: PresentationDefinition,
        documents: List<CredentialDocument>,
        disclosedFields: List<DocumentField> = documents.flatMap { document -> document.fields }.toList()
    ): Pair<List<CredentialClaim>, Match> {
        val claims = documents.map { document ->
            when (document) {
                is CredentialDocument.JwtDocument -> document.asClaim(document, formats[SupportedFormat.SD_JWT]!!, disclosedFields)
                is CredentialDocument.MDocDocument -> document.asClaim(document, formats[SupportedFormat.MSO_MDOC]!!, disclosedFields)
            }
        }
        return claims to PresentationExchange.matcher.match(presentationDefinition, claims)
    }

    private fun CredentialDocument.asClaim(credentialDocument: CredentialDocument, format: Format, disclosedFields: List<DocumentField> = fields): CredentialClaim =
        buildJsonObject {
            put("vct", type.uri)
            put("type", type.uri)
            val associated = disclosedFields
                .filter { field -> CredentialAttribute.find(field.namespace, field.name, type)?.disclosable == true }
                .groupBy { it.namespace.uri }

            associated.forEach {
                if (it.key.isNotEmpty()) {
                    put(
                        it.key,
                        buildJsonObject {
                            it.value.forEach { field ->
                                put(field.name, field.value)
                            }
                        }
                    )
                } else {
                    it.value.forEach { field ->
                        field.element?.let { element ->
                            put(field.name, element)
                        }
                    }
                }
            }
        }.let { json ->
            object : CredentialClaim {
                override val uniqueId = UUID.randomUUID().toString()
                override val format = format
                override fun asJsonString(): String = json.toString()
                override val credentialDocument = credentialDocument
            }
        }

    suspend fun buildConsensus(
        request: ResolvedRequestObject,
        documents: List<CredentialDocument>,
        disclosedFields: List<DocumentField>
    ): Consensus = withContext(dispatcher) {
        suspend fun buildOpenId4VPAuthorizationConsensus(
            request: ResolvedRequestObject.OpenId4VPAuthorization,
            documents: List<CredentialDocument>,
            disclosures: List<DocumentField>
        ): Consensus {
            if (documents.isEmpty()) {
                return Consensus.NegativeConsensus
            }

            // Build presentations from documents
            val jwkThumbprint = request.responseEncryptionSpecification?.recipientKey?.computeThumbprint()?.decode()
            //val jwkThumbprintBase64 = jwkThumbprint?.let { Base64.encodeToString(it, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP) }

            val presentations = documents.map { document ->
                presentDocument(document, disclosures, request.client.id, request.nonce, request.responseMode, jwkThumbprint)
            }.map { VerifiablePresentation.Generic(it) }

            // Map presentations to query IDs from DCQL
            val queryIds = request.query.credentials.ids
            val presentationsMap = if (queryIds.isNotEmpty()) {
                // Map each presentation to the corresponding query ID
                // Wrap each presentation in a list as the API expects Map<QueryId, List<VerifiablePresentation>>
                queryIds.zip(presentations.map { listOf(it) }).toMap()
            } else {
                // Fallback: use a default query ID
                mapOf(QueryId("default") to presentations)
            }

            return Consensus.PositiveConsensus.VPTokenConsensus(
                verifiablePresentations = VerifiablePresentations(presentationsMap)
            )
        }

        when (request) {
            is ResolvedRequestObject.OpenId4VPAuthorization -> buildOpenId4VPAuthorizationConsensus(request, documents, disclosedFields)
            else -> Consensus.NegativeConsensus
        }
    }

    private suspend fun presentDocument(
        document: CredentialDocument,
        disclosedFields: List<DocumentField> = document.fields,
        verifierId: VerifierId,
        nonce: String,
        responseMode: ResponseMode,
        jwkThumbprint: ByteArray?
    ): String = when (document) {
        is CredentialDocument.JwtDocument -> presentSdJwt(document, disclosedFields, verifierId, nonce)
        is CredentialDocument.MDocDocument -> presentMDoc(document, disclosedFields, verifierId, nonce, responseMode, jwkThumbprint)
    }

    // TODO: handle not disclosed optional fields
    private suspend fun presentMDoc(
        document: CredentialDocument.MDocDocument,
        disclosedFields: List<DocumentField>,
        verifierId: VerifierId,
        nonce: String,
        responseMode: ResponseMode,
        jwkThumbprint: ByteArray?
    ): String {
        val docType = document.type.uri
        val mDocRequest = MDocRequestBuilder(docType).apply {
            disclosedFields.forEach {
                addDataElementRequest(it.namespace.uri, it.name, true)
            }
        }.build(null)
        val cryptoProvider = cryptoProviderFactory.forKeyType(document.attestation.keyAttestation.keyType)
        val keyId = document.attestation.keyAttestation.keyId
        val responseURI: String = when (responseMode) {
            is ResponseMode.DirectPostJwt -> responseMode.responseURI.toString()
            is ResponseMode.DirectPost -> responseMode.responseURI.toString()
            is ResponseMode.FragmentJwt -> responseMode.redirectUri.toString()
            is ResponseMode.Fragment -> responseMode.redirectUri.toString()
            is ResponseMode.QueryJwt -> responseMode.redirectUri.toString()
            is ResponseMode.Query -> responseMode.redirectUri.toString()
        }

        val deviceAuthentication = getDeviceAuthentication(verifierId.clientId, nonce, jwkThumbprint, responseURI, docType)
        logger.info("DeviceAuthentication in CBOR: ${deviceAuthentication.toDE().toCBORHex()}")
        val mdoc = document.mDoc.presentWithDeviceSignature(
            mDocRequest = mDocRequest,
            deviceAuthentication = deviceAuthentication,
            // Step 5: the DeviceAuthentication signature is made inside the SecureArea key;
            // no private key material reaches this code path.
            cryptoProvider = cryptoProvider.deviceCryptoProvider(secureAreaKeyManager, keyId),
            keyID = keyId
        )

        return DeviceResponse(listOf(mdoc)).toCBORBase64URL()
    }

    private suspend fun presentSdJwt(
        document: CredentialDocument.JwtDocument,
        disclosedFields: List<DocumentField>,
        verifierId: VerifierId,
        nonce: String
    ): String {
        val issueTime = Date.from(Instant.now().truncatedTo(ChronoUnit.SECONDS))
        val jwt = document.sdJwt.jwt
        val hashAlg = jwt.second["_sd_alg"]?.jsonPrimitive?.let { HashAlgorithm.fromString(it.content) } ?: HashAlgorithm.SHA_256

        // Use recreateClaimsAndDisclosuresPerClaim to get exact path-to-disclosure mappings
        // This allows us to correctly match nested claims like "place_of_birth.locality" vs "address.locality"
        val (_, disclosuresPerPath) = with(DefaultSdJwtOps) {
            document.sdJwt.recreateClaimsAndDisclosuresPerClaim()
        }

        // Helper to convert ClaimPath to dotted notation string (e.g., "place_of_birth.locality")
        fun claimPathToString(claimPath: eu.europa.ec.eudi.sdjwt.vc.ClaimPath): String {
            // Use the public 'value' property which returns List<ClaimPathElement>
            return claimPath.value.joinToString(".") { element ->
                when (element) {
                    is ClaimPathElement.Claim -> element.name
                    is ClaimPathElement.ArrayElement -> "[${element.index}]"
                    is ClaimPathElement.AllArrayElements -> "[*]"
                }
            }
        }

        // Collect disclosures for all paths that match disclosed fields
        val disclosedFieldNames = disclosedFields.map { it.name }.toSet()
        val toBeDisclosed = mutableSetOf<eu.europa.ec.eudi.sdjwt.Disclosure>()

        disclosuresPerPath.forEach { (claimPath, disclosures) ->
            val pathString = claimPathToString(claimPath)

            // Include disclosure if:
            // 1. Exact match (e.g., "nationalities" matches "nationalities")
            // 2. This path is a child of a disclosed field (e.g., "nationalities[0]" is child of "nationalities")
            val shouldInclude = disclosedFieldNames.contains(pathString) ||
                    disclosedFieldNames.any { fieldName ->
                        // Check if pathString is a child: starts with "fieldName." or "fieldName["
                        pathString.startsWith("$fieldName.") || pathString.startsWith("$fieldName[")
                    }

            if (shouldInclude) {
                toBeDisclosed.addAll(disclosures)
            }
        }

        val presentationSdJwt = SdJwt(jwt, toBeDisclosed.toList())
        val keyManager = cryptoProviderFactory.forKeyType(document.attestation.keyAttestation.keyType)
        val keyBindingSigner = keyManager.keyBindingSigner(document.attestation.keyAttestation.keyId)

        // TODO: Derive JWSAlgorithm from key
        val buildKbJwt = kbJwtIssuer(keyBindingSigner, JWSAlgorithm.ES256, keyBindingSigner.publicKey) { // TODO: use hashAlg
            audience(verifierId.clientId)
            claim("nonce", nonce)
            issueTime(issueTime)
        }

        return presentationSdJwt.serializeWithKeyBinding(buildKbJwt).getOrThrow()
    }
}
