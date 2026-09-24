package ee.cyber.wallet.security

import id.walt.mdoc.dataelement.ByteStringElement
import id.walt.mdoc.dataelement.ListElement
import id.walt.mdoc.dataelement.MapElement
import id.walt.mdoc.dataelement.MapKey
import id.walt.mdoc.dataelement.NumberElement
import id.walt.mdoc.dataelement.StringElement
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Pins the hand-written COSE Sig_structure encoder the SecureArea DeviceAuthentication signature
 * is made over. A wrong byte here breaks every device signature at the verifier while nothing on
 * the JVM would notice, so it is checked against a literal vector and against walt.id's own CBOR
 * encoder at every bstr length-prefix boundary.
 */
@OptIn(ExperimentalStdlibApi::class)
class CoseSigStructureTest {

    @Test
    fun `matches the literal Signature1 vector`() {
        val expected = (
            "84" + // array(4)
                "6a" + "5369676e617475726531" + // tstr(10) "Signature1"
                "43" + "a10126" + // bstr(3) protected {1: -7}
                "40" + // bstr(0) external_aad
                "43" + "010203" // bstr(3) payload
            ).hexToByteArray()

        assertContentEquals(
            expected,
            coseSigStructure(SecureAreaCOSECryptoProvider.ES256_PROTECTED_HEADER, byteArrayOf(1, 2, 3))
        )
    }

    @Test
    fun `protected header is the CBOR map alg ES256`() {
        assertContentEquals(
            MapElement(mapOf(MapKey(1) to NumberElement(-7))).toCBOR(),
            SecureAreaCOSECryptoProvider.ES256_PROTECTED_HEADER
        )
    }

    @Test
    fun `matches walt id CBOR at every bstr length boundary`() {
        val header = SecureAreaCOSECryptoProvider.ES256_PROTECTED_HEADER
        listOf(0, 1, 23, 24, 255, 256, 65535, 65536).forEach { size ->
            val payload = ByteArray(size) { it.toByte() }
            val reference = ListElement(
                listOf(
                    StringElement("Signature1"),
                    ByteStringElement(header),
                    ByteStringElement(ByteArray(0)),
                    ByteStringElement(payload)
                )
            ).toCBOR()
            assertContentEquals(reference, coseSigStructure(header, payload), "payload of $size bytes")
        }
    }
}
