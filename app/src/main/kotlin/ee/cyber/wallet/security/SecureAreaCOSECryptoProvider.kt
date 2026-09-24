
package ee.cyber.wallet.security

import ee.cyber.wallet.crypto.CryptoProvider
import id.walt.mdoc.cose.COSECryptoProvider
import id.walt.mdoc.cose.COSESign1
import id.walt.mdoc.dataelement.ByteStringElement
import id.walt.mdoc.dataelement.DataElement
import id.walt.mdoc.dataelement.MapElement
import kotlinx.coroutines.runBlocking
import org.multipaz.crypto.EcSignature
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.security.cert.CertificateFactory

/**
 * COSE signing for mdoc DeviceAuthentication through the SecureArea key (conformance plan §4
 * item 5, finding F2). Produces a COSE_Sign1 byte-identical in structure to what walt.id's
 * SimpleCOSECryptoProvider produced for the same inputs, except that the signature is made
 * inside Android Keystore: the private key never enters this process's heap.
 *
 * Verified against the org.cose 1.1.1-WALT library walt.id builds on (bytecode-level check in
 * this workspace, recorded in docs/planning/STEP5-RECORD.md):
 *  - its Sig_structure is `["Signature1", protected(bstr), bstr(empty), payload(bstr)]`;
 *  - ECDSA signatures are converted from DER to raw `r||s` before being stored, which is the
 *    same raw form multipaz's EcSignature.toCoseEncoded() emits;
 *  - with no custom headers the protected header is exactly the two-byte CBOR map {1: -7}
 *    (alg: ES256), and the x5chain header walt.id would add sits in the *unprotected* bucket,
 *    so for header-less device signatures both providers emit identical structures.
 */
class SecureAreaCOSECryptoProvider(
    private val keyManager: SecureAreaKeyManager
) : COSECryptoProvider {

    private val logger = LoggerFactory.getLogger("SecureAreaCOSECryptoProvider")

    override fun sign1(
        payload: ByteArray,
        protectedHeaders: MapElement?,
        unprotectedHeaders: MapElement?,
        keyId: String?
    ): COSESign1 {
        requireNotNull(keyId) { "SecureArea signing needs the key alias as keyID" }
        require(payload.isNotEmpty()) { "DeviceAuthentication payload must not be empty" }
        require(protectedHeaders == null || protectedHeaders.value.isEmpty()) {
            "custom protected headers are not supported by SecureArea signing"
        }
        require(unprotectedHeaders == null || unprotectedHeaders.value.isEmpty()) {
            "custom unprotected headers are not supported by SecureArea signing"
        }

        val protectedHeader = ES256_PROTECTED_HEADER
        val sigStructure = coseSigStructure(protectedHeader, payload)
        val rawSignature = runBlocking {
            keyManager.sign(keyId, sigStructure)
        }.toCoseEncoded()

        // COSE_Sign1 array: [protected, unprotected(empty map), payload, signature]. The payload
        // stays attached here; walt.id's MDoc detaches it when assembling the DeviceSigned.
        return COSESign1(
            listOf<DataElement>(
                ByteStringElement(protectedHeader),
                MapElement(emptyMap()),
                ByteStringElement(payload),
                ByteStringElement(rawSignature)
            )
        )
    }

    override fun verify1(coseSign1: COSESign1, keyId: String?): Boolean {
        requireNotNull(keyId) { "verification needs the key alias as keyID" }
        return runCatching {
            val keyInfo = runBlocking { keyManager.keyInfo(keyId) }
            val payload = requireNotNull(coseSign1.payload) { "detached COSE_Sign1 is not a DeviceAuthentication signature" }
            // The signature carried in a COSE_Sign1 is raw r||s - multipaz's EcSignature
            // cose encoding - so it can be fed back for local verification directly.
            val signature = EcSignature.Companion.fromCoseEncoded(coseSign1.signatureOrTag)
            runBlocking {
                org.multipaz.crypto.Crypto.checkSignature(
                    publicKey = keyInfo.publicKey,
                    message = coseSigStructure(coseSign1.protectedHeader, payload),
                    signature = signature,
                    algorithm = keyInfo.algorithm
                )
            }
            true
        }.onFailure { logger.warn("SecureArea COSE verification failed", it) }
            .getOrDefault(false)
    }

    override fun verifyX5Chain(coseSign1: COSESign1, keyId: String?): Boolean {
        requireNotNull(keyId) { "verification needs the key alias as keyID" }
        if (!verify1(coseSign1, keyId)) return false
        // The attestation chain in the COSE_Sign1 unprotected header, when present, must be the
        // chain the SecureArea recorded for the key.
        val chainBytes = coseSign1.x5Chain ?: return true
        val recorded = runCatching { runBlocking { keyManager.keyInfo(keyId) }.attestation.certChain }.getOrNull()
            ?: return false
        if (chainBytes.size != recorded.certificates.size) return false
        return chainBytes.zip(recorded.certificates).all { (raw, cert) ->
            raw.contentEquals(cert.encoded.toByteArray())
        }
    }

    companion object {
        /** CBOR map {1: -7} - protected header with alg: ES256, exactly what org.cose emits. */
        private val ES256_PROTECTED_HEADER = byteArrayOf(0xa1.toByte(), 0x01, 0x26)
    }
}

/**
 * The COSE Sig_structure: `["Signature1", protected(bstr), external(bstr, empty), payload(bstr)]`.
 * org.cose's Sign1Message.sign() signs exactly these bytes for ECDSA_256; see the class comment.
 */
private fun coseSigStructure(protectedHeader: ByteArray, payload: ByteArray): ByteArray {
    val out = ByteArrayOutputStream()
    out.write(0x84) // CBOR array of 4
    out.write(0x6b) // CBOR text string of 11 bytes
    out.write("Signature1".toByteArray(Charsets.US_ASCII))
    writeBstr(out, protectedHeader)
    writeBstr(out, ByteArray(0))
    writeBstr(out, payload)
    return out.toByteArray()
}

private fun writeBstr(out: ByteArrayOutputStream, value: ByteArray) {
    when {
        value.size < 24 -> out.write(0x40 or value.size)
        value.size <= 0xff -> {
            out.write(0x58)
            out.write(value.size)
        }
        value.size <= 0xffff -> {
            out.write(0x59)
            out.write((value.size shr 8) and 0xff)
            out.write(value.size and 0xff)
        }
        else -> {
            out.write(0x5a)
            out.write((value.size ushr 24) and 0xff)
            out.write((value.size ushr 16) and 0xff)
            out.write((value.size ushr 8) and 0xff)
            out.write(value.size and 0xff)
        }
    }
    out.write(value)
}
