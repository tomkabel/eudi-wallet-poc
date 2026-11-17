package ee.cyber.wallet.ui.screens.presentation

import androidx.lifecycle.SavedStateHandle
import ee.cyber.wallet.ui.navigation.main.MainRoute
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * EE-PRO-010: the consent screen's cross-device warning keys on how the request arrived. The
 * scanner passes the request as [MainRoute.ARG_URI]; a deep link fills `clientId`/`requestUri`.
 * Navigation attaches a deep-link intent to both, so the intent cannot tell them apart.
 */
class ScannerChannelTest {

    @Test
    fun `scanner navigation is cross-device`() {
        val handle = SavedStateHandle(mapOf(MainRoute.ARG_URI to "openid4vp://?client_id=x509_hash:abc&request_uri=https://rp.example.ee/r"))
        assertTrue(cameFromScanner(handle))
    }

    @Test
    fun `deep link is not labelled as scanned`() {
        val handle = SavedStateHandle(mapOf("clientId" to "x509_hash:abc", "requestUri" to "https://rp.example.ee/r"))
        assertFalse(cameFromScanner(handle))
    }
}
