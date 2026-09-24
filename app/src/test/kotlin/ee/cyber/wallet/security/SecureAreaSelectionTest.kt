package ee.cyber.wallet.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JVM tests for the step 5 StrongBox/TEE selection policy (conformance plan §4 item 5): StrongBox
 * where the device has FEATURE_STRONGBOX_KEYSTORE, TEE otherwise. The Android feature lookup is
 * injected, so both branches are testable without a device; the on-device attestation check is
 * PENDING-DEVICE and tracked in docs/planning/STEP5-RECORD.md.
 */
class SecureAreaSelectionTest {

    @Test
    fun `prefers StrongBox when the device advertises the feature`() {
        val selection = DeviceSecureAreaSelection(hasStrongBoxFeature = true)
        assertEquals(HardwareBacking.STRONGBOX, selection.backing())
        assertTrue(selection.useStrongBox())
    }

    @Test
    fun `falls back to TEE without the feature`() {
        val selection = DeviceSecureAreaSelection(hasStrongBoxFeature = false)
        assertEquals(HardwareBacking.TEE, selection.backing())
        assertFalse(selection.useStrongBox())
    }
}
