package ee.cyber.wallet.domain.presentation

import org.multipaz.mdoc.zkp.ZkSystem
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

    /** Exact production construction seam: registry failure never becomes native incapability. */
    fun create(
        native: () -> ZkSystem,
        registry: () -> Result<SignedCircuitList>,
        clock: () -> java.time.Instant = java.time.Instant::now
    ): ZkSystem? = runCatching { RegistryZkSystem.create(native(), registry, clock) }
        .onFailure { LoggerFactory.getLogger("ZkCapability").warn("Longfellow prover unavailable on this device", it) }
        .getOrNull()

    val zkSystem: ZkSystem? by lazy {
        create(
            native = { LongfellowZkSystem().apply { addDefaultCircuits() } },
            registry = { PinnedCircuitList.loaded }
        )
    }

    val capable: Boolean get() = zkSystem != null
    val registryFailure: String? get() = zkSystem?.registryFailure
}
