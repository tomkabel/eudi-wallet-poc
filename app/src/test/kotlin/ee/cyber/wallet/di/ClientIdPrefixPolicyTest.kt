package ee.cyber.wallet.di

import eu.europa.ec.eudi.openid4vp.SupportedClientIdPrefix
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * B4 (identity A-M3 / codesec CS-M5): the RedirectUri client-id prefix must be
 * gone — a bare redirect URI binds to no registered client, so accepting it
 * would let any web origin mint a request and impersonate a verifier. Only the
 * JWS-verified x509 SAN-DNS / x509 hash schemes remain.
 */
class ClientIdPrefixPolicyTest {

    private val chainValidator = X509CertificateTrust { true }

    @Test
    fun `prefix list carries only the x509 schemes - no RedirectUri`() {
        val prefixes = verifiableClientIdPrefixes(chainValidator)

        assertEquals(2, prefixes.size)
        assertTrue(
            prefixes.all { it is SupportedClientIdPrefix.X509SanDns || it is SupportedClientIdPrefix.X509Hash },
            "an unexpected client-id prefix slipped into the policy: $prefixes"
        )
    }

    @Test
    fun `both x509 schemes share the injected chain validator`() {
        val prefixes = verifiableClientIdPrefixes(chainValidator)

        prefixes.forEach { prefix ->
            when (prefix) {
                is SupportedClientIdPrefix.X509SanDns -> assertEquals(chainValidator, prefix.trust)
                is SupportedClientIdPrefix.X509Hash -> assertEquals(chainValidator, prefix.trust)
                else -> throw AssertionError("unexpected prefix $prefix")
            }
        }
    }
}
