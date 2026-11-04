package ee.cyber.wallet.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** PKI plan NEW-S2-2: only currently positive services of the configured type may vouch. */
class LotlStatusFilterTest {
    private val rp = "http://uri.etsi.org/Svc/Svctype/CA/RPaccess"
    private val base = "http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/"

    @Test
    fun `granted and recognised RP-access services vouch`() {
        assertTrue(isTrustedNow(rp, base + "granted", rp))
        assertTrue(isTrustedNow(rp, base + "recognisedatnationallevel", rp))
    }

    @Test
    fun `withdrawn, deprecated, unknown or missing status does not vouch`() {
        for (s in listOf("withdrawn", "deprecatedatnationallevel", "suspended", "")) {
            assertFalse(s, isTrustedNow(rp, base + s, rp))
        }
        assertFalse(isTrustedNow(rp, null, rp))
    }

    @Test
    fun `a granted service of another type does not vouch`() {
        assertFalse(isTrustedNow("http://uri.etsi.org/Svc/Svctype/Provider/PID", base + "granted", rp))
    }
}
