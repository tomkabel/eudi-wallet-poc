package ee.cyber.wallet.domain.provider.wallet

import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import kotlinx.coroutines.CancellationException
import java.time.Clock
import java.time.Duration

/** Local demonstration format only; not a TS3 wire-format or integrity certification claim. */
data class AvIssuanceContext(val audience: String, val transactionId: String, val challenge: String)

data class AvsuaEvidence(val instanceAttestation: String?, val keyAttestation: String?)

data class AvIssuanceRequest(val context: AvIssuanceContext, val evidence: AvsuaEvidence?)

enum class AvsuaRefusalCause {
    UNSUPPORTED_PROVIDER, MISSING_BUNDLE, MISSING_INSTANCE, MISSING_KEY,
    SIGNATURE_OR_TRUST, BINDING, VALIDITY, INTEGRITY, KEY_SECURITY, MALFORMED
}

class AvsuaRefusal(val causeCode: AvsuaRefusalCause) : IllegalArgumentException("AVSUA refusal: $causeCode")

/**
 * Trust comes only from the AP's configured provider key supplier. Token headers, including
 * x5c, never establish trust. The supplier allows mock account reset to rotate the demo key.
 */
class AvsuaVerifier(private val providerTrustKey: () -> JWK?, private val clock: Clock = Clock.systemUTC()) {
    fun validate(evidence: AvsuaEvidence?, context: AvIssuanceContext, issuanceKey: JWK) {
        val bundle = evidence ?: throw AvsuaRefusal(AvsuaRefusalCause.MISSING_BUNDLE)
        val instance = bundle.instanceAttestation ?: throw AvsuaRefusal(AvsuaRefusalCause.MISSING_INSTANCE)
        val key = bundle.keyAttestation ?: throw AvsuaRefusal(AvsuaRefusalCause.MISSING_KEY)
        val trust = try {
            providerTrustKey() as? RSAKey ?: throw AvsuaRefusal(AvsuaRefusalCause.SIGNATURE_OR_TRUST)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AvsuaRefusal(AvsuaRefusalCause.SIGNATURE_OR_TRUST)
        }
        listOf(instance to INSTANCE_TYPE, key to KEY_TYPE).forEach { (serialized, type) ->
            try {
                val jwt = SignedJWT.parse(serialized)
                if (jwt.header.algorithm != JWSAlgorithm.RS256 || !jwt.verify(RSASSAVerifier(trust.toRSAPublicKey()))) {
                    throw AvsuaRefusal(AvsuaRefusalCause.SIGNATURE_OR_TRUST)
                }
                val claims = jwt.jwtClaimsSet
                if (jwt.header.type != JOSEObjectType(type) || claims.issuer != PROVIDER_ISSUER ||
                    claims.audience != listOf(context.audience) || claims.getStringClaim("transaction_id") != context.transactionId ||
                    claims.getStringClaim("nonce") != context.challenge || claims.jwtid.isNullOrBlank() ||
                    claims.getStringClaim("issuance_key_jkt") != issuanceKey.computeThumbprint().toString()
                ) throw AvsuaRefusal(AvsuaRefusalCause.BINDING)
                val now = clock.instant()
                val issued = claims.issueTime?.toInstant() ?: throw AvsuaRefusal(AvsuaRefusalCause.VALIDITY)
                val notBefore = claims.notBeforeTime?.toInstant() ?: throw AvsuaRefusal(AvsuaRefusalCause.VALIDITY)
                val expires = claims.expirationTime?.toInstant() ?: throw AvsuaRefusal(AvsuaRefusalCause.VALIDITY)
                if (issued > now || notBefore > now || expires <= now || expires <= issued ||
                    notBefore < issued || notBefore >= expires || Duration.between(issued, expires).seconds > MAX_LIFETIME_SECONDS
                ) throw AvsuaRefusal(AvsuaRefusalCause.VALIDITY)
                if (claims.getBooleanClaim("demo_only") != true) throw AvsuaRefusal(AvsuaRefusalCause.INTEGRITY)
                if (type == INSTANCE_TYPE && claims.getBooleanClaim("integrity_ok") != true) {
                    throw AvsuaRefusal(AvsuaRefusalCause.INTEGRITY)
                }
                if (type == KEY_TYPE && claims.getStringClaim("key_security") != DEMO_KEY_SECURITY) {
                    throw AvsuaRefusal(AvsuaRefusalCause.KEY_SECURITY)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: AvsuaRefusal) {
                throw e
            } catch (e: Exception) {
                throw AvsuaRefusal(AvsuaRefusalCause.MALFORMED)
            }
        }
    }

    companion object {
        const val INSTANCE_TYPE = "local-demo-avia+jwt"
        const val KEY_TYPE = "local-demo-ka+jwt"
        const val PROVIDER_ISSUER = "urn:ee:wallet:mock-provider"
        const val DEMO_KEY_SECURITY = "demo-assertion-only"
        const val MAX_LIFETIME_SECONDS = 300L
    }
}
