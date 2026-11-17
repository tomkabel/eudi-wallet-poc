package ee.cyber.wallet.domain.credentials

import com.upokecenter.cbor.CBORObject

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

/**
 * Encodes the registry payload in the structure identitycredentialmatcher.wasm requires: an array
 * of `{title, subtitle, bitmap, mdoc: {id, docType, namespaces}}` maps. The matcher dereferences
 * `namespaces`, so it must be present — but it is always empty: no namespace URI or element name
 * reaches the platform.
 */
internal fun List<RegistryDocType>.toCBORBytes(): ByteArray =
    CBORObject.NewArray().apply {
        this@toCBORBytes.forEach { entry ->
            Add(
                CBORObject.NewMap().apply {
                    Add("title", "Title")
                    Add("subtitle", "Subtitle")
                    Add("bitmap", byteArrayOf(0))
                    Add(
                        "mdoc",
                        CBORObject.NewMap().apply {
                            Add("id", entry.id)
                            Add("docType", entry.docType)
                            Add("namespaces", CBORObject.NewMap())
                        }
                    )
                }
            )
        }
    }.EncodeToBytes()
