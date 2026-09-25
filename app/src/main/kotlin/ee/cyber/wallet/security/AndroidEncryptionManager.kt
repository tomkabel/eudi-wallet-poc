package ee.cyber.wallet.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties.BLOCK_MODE_GCM
import android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import android.security.keystore.KeyProperties.PURPOSE_DECRYPT
import android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class AndroidEncryptionManager(
    private val dispatcher: CoroutineDispatcher
) {

    private val log = LoggerFactory.getLogger("AndroidEncryptionManager")

    suspend fun encrypt(keyAlias: String, rawBytes: ByteArray, output: OutputStream) = withContext(dispatcher) {
        output.use {
            it.write(AesGcmStreamCodec(getOrCreateSecretKey(keyAlias)).encrypt(rawBytes))
        }
    }

    /**
     * Decrypt the framed stream, or throw. There is deliberately no
     * swallow-and-return-empty path here: the DataStore serializers translate
     * a failed decrypt into CorruptionException, which is the DataStore
     * recovery contract — a corrupt blob must surface, not masquerade as a
     * freshly-defaulted proto.
     */
    suspend fun decrypt(keyAlias: String, inputStream: InputStream): ByteArray = withContext(dispatcher) {
        // readBytes() loops to a complete read, which is the guarantee the old
        // single InputStream.read() calls did not have (short reads truncated
        // the IV/ciphertext silently).
        val frame = inputStream.use { it.readBytes() }
        AesGcmStreamCodec(getOrCreateSecretKey(keyAlias)).decrypt(frame)
    }

    private fun keyStore() = KeyStore.getInstance(ANDROID_KEY_STORE).apply {
        load(null)
    }

    private fun getOrCreateSecretKey(keyAlias: String): SecretKey =
        getSecretKey(keyAlias) ?: KeyGenerator.getInstance(ALGORITHM, ANDROID_KEY_STORE).apply {
            init(
                KeyGenParameterSpec
                    .Builder(keyAlias, PURPOSE_ENCRYPT or PURPOSE_DECRYPT)
                    .setBlockModes(BLOCK_MODE)
                    .setKeySize(KEY_SIZE)
                    .setEncryptionPaddings(PADDING)
                    .setUserAuthenticationRequired(false)
                    .setRandomizedEncryptionRequired(true)
                    .apply {
                        // commented out due to unknown issue on Pixel 8, where decrypted value is wrong
//                        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
//                            setIsStrongBoxBacked(true)
//                        }
                    }
                    .build()
            )
        }.generateKey()

    private fun getSecretKey(keyAlias: String) = keyStore().getKey(keyAlias, charArrayOf()) as? SecretKey

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"

        private const val ALGORITHM = KEY_ALGORITHM_AES
        private const val BLOCK_MODE = BLOCK_MODE_GCM
        private const val PADDING = ENCRYPTION_PADDING_NONE
        private const val KEY_SIZE = 256
    }
}
