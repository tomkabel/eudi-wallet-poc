package ee.cyber.wallet.domain.credentials

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * JVM unit tests for the ARF OIA_08e/08f registry decision core: the CBOR the wallet hands the
 * platform carries docTypes only — no namespace URIs, no element names, no values.
 */
class RegistryDocTypeTest {

    @Test
    fun `cbor payload carries id and docType only`() {
        val bytes = listOf(
            RegistryDocType(id = "doc-1", docType = "eu.europa.ec.av.1"),
            RegistryDocType(id = "doc-2", docType = "eu.europa.ec.eudi.pid.1")
        ).toCBORBytes()

        val decoded = com.upokecenter.cbor.CBORObject.DecodeFromBytes(bytes)
        assertEquals(2, decoded.size())
        val first = decoded[0]
        assertEquals("Title", first["title"].AsString())
        assertEquals("Subtitle", first["subtitle"].AsString())
        val mdoc = first["mdoc"]
        assertEquals("doc-1", mdoc["id"].AsString())
        assertEquals("eu.europa.ec.av.1", mdoc["docType"].AsString())
        assertEquals(3, mdoc.size())
        // Present because the matcher dereferences it, but always empty.
        assertEquals(0, mdoc["namespaces"].size())
    }

    @Test
    fun `no attribute data escapes into the payload`() {
        val bytes = listOf(
            RegistryDocType(id = "doc-1", docType = "eu.europa.ec.eudi.pid.1")
        ).toCBORBytes()
        val decoded = com.upokecenter.cbor.CBORObject.DecodeFromBytes(bytes)
        // The isikukood, the name, the birthdate — none of it may appear anywhere in the bytes.
        val text = decoded.ToJSONString()
        kotlin.test.assertFalse("family_name" in text)
        kotlin.test.assertFalse("isikukood" in text)
        kotlin.test.assertFalse("birthdate" in text)
    }
}
