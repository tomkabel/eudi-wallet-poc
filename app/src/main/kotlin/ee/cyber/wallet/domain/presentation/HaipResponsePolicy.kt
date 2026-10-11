package ee.cyber.wallet.domain.presentation

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import eu.europa.ec.eudi.openid4vp.ResponseEncryptionSpecification
import eu.europa.ec.eudi.openid4vp.ResponseMode

/**
 * EE-PRO-001 (HAIP 1.0): the only response a redirect-flow verifier gets from this wallet is
 * `direct_post.jwt`, encrypted with ECDH-ES on P-256 and A128GCM or A256GCM.
 *
 * The library only offers encryption as *supported*, so a verifier that asks for plain
 * `direct_post`, `fragment` or `query` would otherwise receive the issuer-signed documents in
 * cleartext to whatever sits on its response path. Signed `*.jwt` modes other than
 * `direct_post.jwt` are refused too: HAIP profiles redirect presentation on `direct_post.jwt`
 * alone, and `fragment.jwt` / `query.jwt` put the response in a URL the browser history keeps.
 */
object HaipResponsePolicy {

    val encryptionAlgorithms: List<JWEAlgorithm> = listOf(JWEAlgorithm.ECDH_ES)
    val encryptionMethods: List<EncryptionMethod> = listOf(EncryptionMethod.A128GCM, EncryptionMethod.A256GCM)

    /** Why a request is refused, or null when the wallet may answer it. */
    fun refusal(mode: ResponseMode, encryption: ResponseEncryptionSpecification?): String? = when {
        mode !is ResponseMode.DirectPostJwt -> "response mode ${mode::class.simpleName} is not direct_post.jwt"
        encryption == null -> "direct_post.jwt request carries no encryption key"
        encryption.encryptionAlgorithm !in encryptionAlgorithms -> "alg ${encryption.encryptionAlgorithm} is not ECDH-ES"
        encryption.encryptionMethod !in encryptionMethods -> "enc ${encryption.encryptionMethod} is not A128GCM/A256GCM"
        (encryption.recipientKey as? ECKey)?.curve != Curve.P_256 -> "recipient key is not a P-256 EC key"
        else -> null
    }
}
