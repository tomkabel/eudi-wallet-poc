package ee.cyber.wallet.security

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.EOFException
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * The [ivLen][iv][ctLen][ct+tag] stream format AndroidEncryptionManager writes,
 * and the AES-256/GCM codec that parses it. Pure javax.crypto — no Android
 * Keystore — so the framing and failure paths are unit-testable on the JVM
 * with a software key; the manager only supplies the keystore-backed key.
 *
 * Decoding is strict on purpose: truncated input, negative or oversized length
 * prefixes, short payloads and failed authentication all throw. Callers (the
 * DataStore serializers) translate that into CorruptionException instead of
 * parsing silently-decrypted garbage.
 */
internal class AesGcmStreamCodec(private val key: SecretKey) {

    /**
     * Encrypt to the framed stream form. The IV is whatever the cipher
     * generated — for an AndroidKeyStore key with randomized encryption
     * required, a fresh Keystore-chosen 12-byte IV per call.
     */
    fun encrypt(rawBytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        require(iv.size in 1..MAX_IV_BYTES) { "cipher produced an implausible IV length: ${iv.size}" }
        val encrypted = cipher.doFinal(rawBytes)
        return ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).apply {
                writeInt(iv.size)
                write(iv)
                writeInt(encrypted.size)
                write(encrypted)
            }
            buffer.toByteArray()
        }
    }

    /**
     * Parse and decrypt the framed stream form. Every malformed frame throws
     * (EOFException / IllegalArgumentException / AEADBadTagException) — never a
     * partial or empty plaintext.
     */
    fun decrypt(frame: ByteArray): ByteArray {
        require(frame.size >= MIN_FRAME_BYTES) { "encrypted stream truncated: ${frame.size} bytes" }
        var offset = 0
        fun readInt(): Int {
            val value = ((frame[offset].toInt() and 0xFF) shl 24) or
                ((frame[offset + 1].toInt() and 0xFF) shl 16) or
                ((frame[offset + 2].toInt() and 0xFF) shl 8) or
                (frame[offset + 3].toInt() and 0xFF)
            offset += Int.SIZE_BYTES
            return value
        }

        fun readBytes(count: Int): ByteArray {
            require(count >= 0) { "negative length prefix: $count" }
            require(count <= MAX_BLOB_BYTES) { "length prefix $count exceeds the $MAX_BLOB_BYTES blob ceiling" }
            require(offset + count <= frame.size) { "stream truncated: claimed $count bytes, ${frame.size - offset} left" }
            return frame.copyOfRange(offset, offset + count).also { offset += count }
        }

        val iv = readBytes(readInt())
        require(iv.size in 1..MAX_IV_BYTES) { "implausible IV length: ${iv.size}" }
        val ciphertext = readBytes(readInt())
        require(ciphertext.isNotEmpty()) { "empty ciphertext" }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding" // NON-NLS
        const val GCM_IV_LENGTH_BYTES = 12
        const val GCM_TAG_LENGTH_BITS = 128
        const val MIN_FRAME_BYTES = Int.SIZE_BYTES + GCM_IV_LENGTH_BYTES + Int.SIZE_BYTES + 16

        /** The IV length GCM keys produce; anything else in the stream is corruption. */
        const val MAX_IV_BYTES = 16

        /**
         * Ceiling on any length prefix we are willing to honour. The decrypt
         * callers are the DataStore proto blobs (user session, wallet
         * credentials) — kilobytes, not gigabytes. A corrupted or hostile
         * length prefix must fail loudly, not trigger an OOM.
         */
        const val MAX_BLOB_BYTES = 64 * 1024 * 1024
    }
}
