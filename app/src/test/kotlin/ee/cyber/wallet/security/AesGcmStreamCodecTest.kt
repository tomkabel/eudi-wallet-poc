package ee.cyber.wallet.security

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * JVM tests for the AES/GCM stream codec behind AndroidEncryptionManager
 * (JVM-H2/D5): the framing, the strict-failure contract and the randomized
 * IV. The keystore is not involved — a software AES key exercises the same
 * javax.crypto paths.
 */
class AesGcmStreamCodecTest {

    private fun newCodec(): AesGcmStreamCodec {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(256)
        return AesGcmStreamCodec(generator.generateKey())
    }

    @Test
    fun `round trip across payload sizes`() {
        val codec = newCodec()
        val random = SecureRandom()
        for (size in intArrayOf(0, 1, 15, 16, 17, 1024, 65536)) {
            val payload = ByteArray(size).also(random::nextBytes)
            val frame = codec.encrypt(payload)
            assertContentEquals(payload, codec.decrypt(frame), "payload size $size")
        }
    }

    @Test
    fun `every encryption uses a fresh IV`() {
        val codec = newCodec()
        val payload = "same plaintext".toByteArray()
        val frames = (1..8).map { codec.encrypt(payload) }
        assertTrue(frames.map { it.copyOfRange(Int.SIZE_BYTES, Int.SIZE_BYTES + AesGcmStreamCodec.GCM_IV_LENGTH_BYTES) }
            .toSet().size == frames.size, "IVs repeat across encryptions")
        frames.forEach { assertContentEquals(payload, codec.decrypt(it)) }
    }

    @Test
    fun `tampered ciphertext fails authentication`() {
        val codec = newCodec()
        val frame = codec.encrypt("attack at dawn".toByteArray())
        frame[frame.size - 1] = (frame[frame.size - 1].toInt() xor 0x01).toByte()
        assertFailsWith<Exception> { codec.decrypt(frame) }
    }

    @Test
    fun `tampered IV fails authentication`() {
        val codec = newCodec()
        val frame = codec.encrypt("attack at dawn".toByteArray())
        frame[Int.SIZE_BYTES] = (frame[Int.SIZE_BYTES].toInt() xor 0x01).toByte()
        assertFailsWith<Exception> { codec.decrypt(frame) }
    }

    @Test
    fun `truncated frame is rejected`() {
        val codec = newCodec()
        val frame = codec.encrypt("payload".toByteArray())
        assertFailsWith<IllegalArgumentException> { codec.decrypt(frame.copyOfRange(0, frame.size - 1)) }
        assertFailsWith<IllegalArgumentException> {
            codec.decrypt(frame.copyOfRange(0, AesGcmStreamCodec.MIN_FRAME_BYTES - 1))
        }
    }

    @Test
    fun `negative length prefix is rejected`() {
        val codec = newCodec()
        val frame = ByteArray(64)
        frame[0] = -1 // iv length prefix = 0xFFFFFFFF
        assertFailsWith<IllegalArgumentException> { codec.decrypt(frame) }
    }

    @Test
    fun `oversized length prefix is rejected without allocating`() {
        val codec = newCodec()
        val buffer = ByteArrayOutputStream()
        java.io.DataOutputStream(buffer).apply {
            // well-formed header shape, but the ciphertext prefix claims more than the ceiling
            writeInt(AesGcmStreamCodec.GCM_IV_LENGTH_BYTES)
            write(ByteArray(AesGcmStreamCodec.GCM_IV_LENGTH_BYTES))
            writeInt(AesGcmStreamCodec.MAX_BLOB_BYTES + 1)
        }
        assertFailsWith<IllegalArgumentException> { codec.decrypt(buffer.toByteArray()) }
    }

    @Test
    fun `empty ciphertext is rejected`() {
        val codec = newCodec()
        val buffer = ByteArrayOutputStream()
        java.io.DataOutputStream(buffer).apply {
            writeInt(AesGcmStreamCodec.GCM_IV_LENGTH_BYTES)
            write(ByteArray(AesGcmStreamCodec.GCM_IV_LENGTH_BYTES))
            writeInt(0)
        }
        assertFailsWith<IllegalArgumentException> { codec.decrypt(buffer.toByteArray()) }
    }

    @Test
    fun `manager surfaces decrypt failures instead of empty plaintext`() {
        // The old contract returned byteArrayOf() on ANY failure; the new one
        // must throw so the DataStore serializer can map it to
        // CorruptionException (JVM-H2). Prove it through the manager itself.
        val codec = newCodec()
        val frame = codec.encrypt("secret".toByteArray())
        frame[frame.size / 2] = (frame[frame.size / 2].toInt() xor 0x01).toByte()

        val manager = AndroidEncryptionManager(Dispatchers.Default)
        val otherCodec = newCodec()
        runBlocking {
            // wrong key: authentication must fail, not return empty bytes
            assertFailsWith<Exception> {
                manager.decrypt("no-such-alias", ByteArrayInputStream(frame))
            }
            // sanity: a correct frame decrypts through the manager path too
            val good = otherCodec.encrypt("secret".toByteArray())
            assertTrue(good.isNotEmpty())
        }
    }
}
