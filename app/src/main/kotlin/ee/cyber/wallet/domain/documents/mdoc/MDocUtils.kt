package ee.cyber.wallet.domain.documents.mdoc

import id.walt.mdoc.dataelement.BooleanElement
import id.walt.mdoc.dataelement.ByteStringElement
import id.walt.mdoc.dataelement.DataElement
import id.walt.mdoc.dataelement.DateTimeElement
import id.walt.mdoc.dataelement.EncodedCBORElement
import id.walt.mdoc.dataelement.FullDateElement
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.dataelement.NullElement
import id.walt.mdoc.dataelement.NumberElement
import id.walt.mdoc.dataelement.StringElement
import id.walt.mdoc.mdocauth.DeviceAuthentication
import org.kotlincrypto.hash.sha2.SHA256
import org.slf4j.LoggerFactory
import java.util.Base64

fun DataElement.value(): Any? = when (this) {
    is ListElement -> this.value.map { it.value() }.joinToString(", ")
    is ByteStringElement -> Base64.getEncoder().encodeToString(this.value)
    else -> when (this) {
        is NumberElement -> this.value
        is BooleanElement -> this.value
        is StringElement -> this.value
        is NullElement -> this.value
        is DateTimeElement -> this.value
        is FullDateElement -> this.value
        is MapElement -> this.value
        is EncodedCBORElement -> this.value
        else -> throw IllegalArgumentException("Unknown DataElement type")
    }
}

object MDocUtils {
    private val log = LoggerFactory.getLogger("MDocUtils")

    @JvmStatic
    fun getDeviceAuthentication(clientId: String, nonce: String, jwkThumbprint: ByteArray?, responseUri: String, docType: String): DeviceAuthentication {
        val sessionTranscript = ListElement(
            listOf(
                NullElement(),
                NullElement(),
                generateOpenID4VPHandover(clientId, nonce, jwkThumbprint, responseUri)
            )
        )
        val deviceNameSpaces = EncodedCBORElement(MapElement(mapOf()))
        return DeviceAuthentication(sessionTranscript, docType, deviceNameSpaces)
    }

    /**
     * Generate OID4VPHandover for session transcript of MDoc device authentication, as defined in OpenID4VP spec B.2.6.1
     *
     * OpenID4VPHandover = [
     *   "OpenID4VPHandover",
     *   OpenID4VPHandoverInfoHash
     * ]
     *
     * OpenID4VPHandoverInfo = [
     *   clientId,
     *   nonce,
     *   jwkThumbprint,
     *   responseUri
     * ]
     *
     * @param clientId The client_id request parameter
     * @param nonce The nonce request parameter
     * @param jwkThumbprint JWK SHA-256 Thumbprint of Verifier's public key (null if response is not encrypted)
     * @param responseUri The redirect_uri or response_uri request parameter
     */
    @JvmStatic
    fun generateOpenID4VPHandover(clientId: String, nonce: String, jwkThumbprint: ByteArray?, responseUri: String): ListElement {
        val handoverInfo = ListElement(
            listOf(
                StringElement(clientId),
                StringElement(nonce),
                jwkThumbprint?.let { ByteStringElement(it) } ?: NullElement(),
                StringElement(responseUri)
            )
        )

        log.info("OpenID4VPHandover in CBOR: ${handoverInfo.toCBORHex()}")

        val handoverInfoHash = SHA256().digest(handoverInfo.toCBOR())

        return ListElement(
            listOf(
                StringElement("OpenID4VPHandover"),
                ByteStringElement(handoverInfoHash)
            )
        )
    }

    @JvmStatic
    fun generateDCApiHandover(encryptionInfoBase64: String, origin: String): ListElement {
        val handoverInfo = ListElement(
            listOf(
                StringElement(encryptionInfoBase64),
                StringElement(origin)
            )
        )
        val handoverInfoHash = SHA256().digest(handoverInfo.toCBOR())
        return ListElement(
            listOf(
                StringElement("dcapi"),
                ByteStringElement(handoverInfoHash)
            )
        )
    }
}
