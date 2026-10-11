package ee.cyber.wallet.domain.presentation

import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.gen.ECKeyGenerator
import eu.europa.ec.eudi.openid4vp.ResponseEncryptionSpecification
import eu.europa.ec.eudi.openid4vp.ResponseMode
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * EE-PRO-001 (HAIP 1.0): only an encrypted `direct_post.jwt` response under ECDH-ES/P-256 with
 * A128GCM or A256GCM may leave the wallet; every other request is refused before consent.
 */
class HaipResponsePolicyTest {

    private val uri = URL("https://verifier.example.ee/response")

    // The library requires the recipient key to name the JWE algorithm and carry a kid.
    private fun key(curve: Curve, alg: JWEAlgorithm) =
        ECKeyGenerator(curve).algorithm(alg).keyID("verifier-enc").generate().toPublicJWK()

    private fun spec(
        alg: JWEAlgorithm = JWEAlgorithm.ECDH_ES,
        enc: EncryptionMethod = EncryptionMethod.A128GCM,
        curve: Curve = Curve.P_256
    ) = ResponseEncryptionSpecification(alg, enc, key(curve, alg))

    @Test
    fun `encrypted direct_post jwt is accepted with both HAIP content encryptions`() {
        assertNull(HaipResponsePolicy.refusal(ResponseMode.DirectPostJwt(uri), spec()))
        assertNull(HaipResponsePolicy.refusal(ResponseMode.DirectPostJwt(uri), spec(enc = EncryptionMethod.A256GCM)))
    }

    @Test
    fun `cleartext response modes are refused`() {
        listOf(
            ResponseMode.DirectPost(uri),
            ResponseMode.Fragment(uri.toURI()),
            ResponseMode.Query(uri.toURI())
        ).forEach { mode -> assertNotNull(HaipResponsePolicy.refusal(mode, null), "$mode was accepted") }
    }

    @Test
    fun `jwt modes that put the response in a url are refused`() {
        listOf(ResponseMode.FragmentJwt(uri.toURI()), ResponseMode.QueryJwt(uri.toURI()))
            .forEach { mode -> assertNotNull(HaipResponsePolicy.refusal(mode, spec()), "$mode was accepted") }
    }

    @Test
    fun `direct_post jwt without an encryption key is refused`() {
        assertNotNull(HaipResponsePolicy.refusal(ResponseMode.DirectPostJwt(uri), null))
    }

    @Test
    fun `non-HAIP algorithms and curves are refused`() {
        val mode = ResponseMode.DirectPostJwt(uri)
        assertNotNull(HaipResponsePolicy.refusal(mode, spec(alg = JWEAlgorithm.ECDH_ES_A256KW)))
        assertNotNull(HaipResponsePolicy.refusal(mode, spec(enc = EncryptionMethod.A128CBC_HS256)))
        assertNotNull(HaipResponsePolicy.refusal(mode, spec(curve = Curve.P_384)))
    }
}
