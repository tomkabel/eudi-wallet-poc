package ee.cyber.wallet.domain.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * JVM unit tests for the EE-PRO-013 protocol dispatch (conformance plan §4 item 4a): the walk
 * over `requests[]` takes the first supported protocol and refuses an unsupported one with a
 * specific decision instead of letting the payload parser crash.
 */
class DcApiRequestDispatchTest {

    @Test
    fun `takes the first supported entry`() {
        val decision = DcApiRequestDispatch.dispatch(listOf("org-iso-mdoc"))
        val take = assertIs<DcApiRequestDispatch.Decision.Take>(decision)
        assertEquals(0, take.index)
        assertEquals(DcApiProtocol.ISO_MDOC, take.protocol)
    }

    @Test
    fun `takes the first supported entry after an unsupported one`() {
        val decision = DcApiRequestDispatch.dispatch(listOf("openid4vp", "org-iso-mdoc"))
        val take = assertIs<DcApiRequestDispatch.Decision.Take>(decision)
        assertEquals(1, take.index)
        assertEquals(DcApiProtocol.ISO_MDOC, take.protocol)
    }

    @Test
    fun `refuses openid4vp as unsupported until section 82 work lands`() {
        val decision = DcApiRequestDispatch.dispatch(listOf("openid4vp"))
        val unsupported = assertIs<DcApiRequestDispatch.Decision.Unsupported>(decision)
        assertEquals("openid4vp", unsupported.protocolName)
    }
    @Test
    fun `refuses an unknown protocol with the name preserved for the error`() {
        val decision = DcApiRequestDispatch.dispatch(listOf("com.example.third-protocol"))
        val unsupported = assertIs<DcApiRequestDispatch.Decision.Unsupported>(decision)
        assertEquals("com.example.third-protocol", unsupported.protocolName)
    }

    @Test
    fun `refuses a missing protocol field instead of guessing from the payload`() {
        val decision = DcApiRequestDispatch.dispatch(listOf(null))
        val unsupported = assertIs<DcApiRequestDispatch.Decision.Unsupported>(decision)
        assertNull(unsupported.protocolName)
    }

    @Test
    fun `refuses an empty request list`() {
        assertIs<DcApiRequestDispatch.Decision.Empty>(DcApiRequestDispatch.dispatch(emptyList()))
    }

    @Test
    fun `wire names round trip`() {
        assertEquals(DcApiProtocol.ISO_MDOC, DcApiProtocol.fromWireName("org-iso-mdoc"))
        assertNull(DcApiProtocol.fromWireName("openid4vp"))
        assertNull(DcApiProtocol.fromWireName("org-iso-mdoc "))
        assertNull(DcApiProtocol.fromWireName(""))
    }
}
