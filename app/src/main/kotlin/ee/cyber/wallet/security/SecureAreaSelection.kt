package ee.cyber.wallet.security

/**
 * Where an Android Keystore key is backed. Recorded for each device key at generation time
 * (conformance plan §4 item 5): the attestation chain the wallet provider receives must report
 * StrongBox or TEE, and the wallet keeps the hardware class it asked for.
 */
enum class HardwareBacking {
    STRONGBOX,
    TEE
}

/**
 * The StrongBox/TEE policy for device keys, step 5 of the conformance plan: StrongBox where the
 * device advertises `FEATURE_STRONGBOX_KEYSTORE`, TEE otherwise. The Pixel 8 anomaly recorded at
 * AndroidEncryptionManager (wrong decrypt results under StrongBox) concerns AES CBC secret keys,
 * not EC signing keys, so the policy still prefers StrongBox for EC - and the step 5 regression
 * test pins the sign/verify round trip so a device anomaly surfaces as a test failure, not as a
 * silently wrong signature.
 *
 * Pure JVM logic: [hasStrongBoxFeature] is injected so tests can drive both branches without a
 * device (the Android feature lookup itself is PENDING-DEVICE, see docs/planning/STEP5-RECORD.md).
 */
interface SecureAreaSelection {
    val hasStrongBoxFeature: Boolean

    fun backing(): HardwareBacking = if (hasStrongBoxFeature) HardwareBacking.STRONGBOX else HardwareBacking.TEE

    fun useStrongBox(): Boolean = hasStrongBoxFeature
}

class DeviceSecureAreaSelection(
    override val hasStrongBoxFeature: Boolean
) : SecureAreaSelection
