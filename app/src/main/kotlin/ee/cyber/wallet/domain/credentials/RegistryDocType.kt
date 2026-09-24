package ee.cyber.wallet.domain.credentials

/**
 * ARF OIA_08e registration payload: docType only, never attribute names or values.
 *
 * The Digital Credentials API matcher (`identitycredentialmatcher.wasm`) matches a request
 * against the registered credentials by docType alone; claim-level selection happens in the
 * wallet after launch, where the user consents to specific fields. The ARF note (OIA_08e) is
 * explicit that this holds "even if such disclosure would enhance the services provided by the
 * operating system", so the attribute map that used to be registered alongside the docType is
 * gone: the wallet may therefore appear in the picker for requests it cannot fully answer,
 * which the ARF note accepts.
 *
 * Pure data, JVM-testable: the view model maps documents onto these and the registrar encodes
 * them to CBOR.
 */
data class RegistryDocType(
    val id: String,
    val docType: String
)
