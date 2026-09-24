package ee.cyber.wallet.util

import org.multipaz.crypto.X509Cert
import org.multipaz.crypto.X509CertChain
import org.multipaz.mdoc.zkp.ZkSystemSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * EE-RP-003 / plan §4 item 4d, review finding 1: the consent-screen subject comes from the
 * readerAuth certificate ONLY when the readerAuth signature check passed. The parser populates
 * `readerCertificateChain` even when the check failed, so gating on the chain alone would let a
 * crafted request display an attacker-chosen CN.
 */
class ReaderAuthSubjectTest {

    /** A real self-signed P-256 certificate with CN=Test Reader Co, so subject parsing runs. */
    private val leafCert = X509Cert.fromPem(
        """
        -----BEGIN CERTIFICATE-----
        MIIBrTCCAVOgAwIBAgIULicFxRFOoxsnir1OQy+HcqHhY/kwCgYIKoZIzj0EAwIw
        LDEXMBUGA1UEAwwOVGVzdCBSZWFkZXIgQ28xETAPBgNVBAoMCFRlc3QgT3JnMB4X
        DTI2MDkyNDEyNTgwN1oXDTM2MDkyMTEyNTgwN1owLDEXMBUGA1UEAwwOVGVzdCBS
        ZWFkZXIgQ28xETAPBgNVBAoMCFRlc3QgT3JnMFkwEwYHKoZIzj0CAQYIKoZIzj0D
        AQcDQgAEo9diqfhTgKsoVdPoSgGQQ6VseHvqVk5MRVqtiv5NZLgsmN6iMJY1gaNM
        05NIiHEIl5cAHS8ROHZF+Ri0rDK7naNTMFEwHQYDVR0OBBYEFNHI99OqjUoShjJs
        PHXzvQtRsQEwMB8GA1UdIwQYMBaAFNHI99OqjUoShjJsPHXzvQtRsQEwMA8GA1Ud
        EwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSAAwRQIgT4GAtOEKnK5o8IHsm1TUXVkS
        /leWqvnJ3EqxsOkGlHACIQD7ujj6kEMpHu09zYcogvbS5iZbGnQFi9ImG8jjRXnP
        +g==
        -----END CERTIFICATE-----
        """.trimIndent()
    )

    private fun docRequest(
        readerAuthenticated: Boolean,
        chain: X509CertChain?
    ): DeviceRequestParser.DocRequest = DeviceRequestParser.DocRequest.Builder(
        "eu.europa.ec.av.1",
        ByteArray(0),
        emptyMap(),
        encodedReaderAuth = if (chain == null) null else ByteArray(0),
        readerCertChain = chain,
        readerAuthenticated = readerAuthenticated,
        zkSystemSpecs = emptyList()
    ).build()

    @Test
    fun `a chain with a FAILED signature check shows no subject`() {
        // The exact attack from review finding 1: the parser carries the chain regardless of
        // the signature outcome, so readerAuthenticated=false must suppress the CN entirely.
        val request = docRequest(readerAuthenticated = false, chain = X509CertChain(listOf(leafCert)))

        assertNull(readerAuthSubject(listOf(request)))
    }

    @Test
    fun `an authenticated request shows the leaf certificate CN`() {
        val request = docRequest(readerAuthenticated = true, chain = X509CertChain(listOf(leafCert)))

        assertEquals("Test Reader Co", readerAuthSubject(listOf(request)))
    }

    @Test
    fun `a request without reader auth shows no subject`() {
        val request = docRequest(readerAuthenticated = false, chain = null)

        assertNull(readerAuthSubject(listOf(request)))
    }

    @Test
    fun `the first AUTHENTICATED doc request wins over an earlier failed one`() {
        val failed = docRequest(readerAuthenticated = false, chain = X509CertChain(listOf(leafCert)))
        val passed = docRequest(readerAuthenticated = true, chain = X509CertChain(listOf(leafCert)))

        assertEquals("Test Reader Co", readerAuthSubject(listOf(failed, passed)))
    }

    @Test
    fun `an authenticated doc request without a chain shows no subject`() {
        // Defensive: readerAuthenticated=true with no chain is not a shape the parser produces,
        // but the helper must not crash on it.
        val request = docRequest(readerAuthenticated = true, chain = null)

        assertNull(readerAuthSubject(listOf(request)))
    }
}
