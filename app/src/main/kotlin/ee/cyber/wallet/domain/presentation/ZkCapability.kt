package ee.cyber.wallet.domain.presentation

import org.multipaz.mdoc.zkp.longfellow.LongfellowZkSystem
import org.slf4j.LoggerFactory

/**
 * Whether this device can prove at all (Tier 2). Loading the prover pulls in libzkp.so, which is
 * only packaged for arm64-v8a and x86_64, so the answer is asked once per process rather than
 * discovered as an UnsatisfiedLinkError in the middle of building a response.
 *
 * Shared by every presentation path: EE-POA-013 and EE-ZKP-055a decide on the device's
 * capability, also on the OpenID4VP redirect and proximity paths, which carry no ZK request and
 * can therefore never satisfy a Tier 2 requirement.
 */
object ZkCapability {

    // ponytail: holds all bundled circuits (~2 MB) once loaded; load only the requested one via
    // addCircuit() if that footprint ever matters.
    val zkSystem: LongfellowZkSystem? by lazy {
        runCatching { LongfellowZkSystem().apply { addDefaultCircuits() } }
            .onFailure { LoggerFactory.getLogger("ZkCapability").warn("Longfellow prover unavailable on this device", it) }
            .getOrNull()
    }

    val capable: Boolean get() = zkSystem != null
}
