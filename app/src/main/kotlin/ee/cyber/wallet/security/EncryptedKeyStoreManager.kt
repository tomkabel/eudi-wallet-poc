package ee.cyber.wallet.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import ee.cyber.wallet.security.BouncyCastleHelper.createBCCertificateFactory
import ee.cyber.wallet.security.BouncyCastleHelper.createBCContentSignerBuilder
import ee.cyber.wallet.security.BouncyCastleHelper.createBCKeyStore
import ee.cyber.wallet.security.BouncyCastleHelper.createKeyPairGenerator
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.X509v3CertificateBuilder
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.security.KeyPair
import java.security.KeyStore
import java.security.PublicKey
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val CN = "CN=EE Wallet"

/**
 * KeyStoreManager whose BKS keystore file is encrypted at rest with an
 * AES-256/GCM key held in the hardware-backed AndroidKeyStore.
 *
 * Replaces the deprecated androidx.security.crypto EncryptedFile/MasterKey
 * (security-crypto is in maintenance); the wrap key never leaves Keystore and
 * the GCM IV is generated fresh per write and stored prepended to the blob.
 */
class EncryptedKeyStoreManager(private val context: Context) : KeyStoreManager {

    private val logger = LoggerFactory.getLogger("EncryptedKeyStoreManager")

    companion object {
        private const val KEY_STORE = "BKS"
        private const val KEY_STORE_FILENAME = "keystore.jks"
        private const val MASTER_KEY_ALIAS = "ee_cyber_wallet_keystore_master"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
    }

    private val file = File(context.filesDir, KEY_STORE_FILENAME)

    private val masterKey: SecretKey by lazy { getOrCreateMasterKey() }

    /**
     * AES-256/GCM wrap key generated inside AndroidKeyStore; non-exportable,
     * kept across runs, recreated only if the keystore entry is missing.
     */
    private fun getOrCreateMasterKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private fun Cipher.initEncrypt() = init(Cipher.ENCRYPT_MODE, masterKey)

    private fun Cipher.initDecrypt(iv: ByteArray) = init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))

    /**
     * Decrypt the keystore blob (IV-prefixed AES/GCM) or return null when the
     * file does not exist yet or is unreadable/corrupt.
     */
    private fun readDecrypted(): ByteArray? =
        runCatching {
            if (!file.exists()) return null
            val bytes = file.readBytes()
            require(bytes.size > GCM_IV_LENGTH_BYTES) { "keystore blob too short" }
            val iv = bytes.copyOfRange(0, GCM_IV_LENGTH_BYTES)
            val ciphertext = bytes.copyOfRange(GCM_IV_LENGTH_BYTES, bytes.size)
            Cipher.getInstance("AES/GCM/NoPadding").run {
                initDecrypt(iv)
                doFinal(ciphertext)
            }
        }.onFailure { logger.error("failed to decrypt keystore blob, starting fresh", it) }.getOrNull()

    /**
     * Atomically encrypt and write the keystore bytes: temp file + rename, so
     * a failed write never truncates the previous keystore blob.
     */
    private fun writeEncrypted(plain: ByteArray) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.initEncrypt()
        val iv = cipher.iv
        val blob = iv + cipher.doFinal(plain)
        val temp = File(file.parentFile, "${file.name}.tmp")
        temp.writeBytes(blob)
        if (!temp.renameTo(file)) {
            // rename can fail across fs quirks; fall back to replace
            file.delete()
            if (!temp.renameTo(file)) {
                temp.delete()
                error("failed to persist keystore blob")
            }
        }
    }

    override fun keyStore() = loadKeyStore()

    override fun containsKey(alias: String): Boolean = keyStore().containsAlias(alias)

    override fun deleteKey(alias: String) {
        runCatching { keyStore().deleteEntry(alias) }
            .onFailure { logger.error("failed to delete key $alias", it) }
    }

    override fun createKeyPair(alias: String, keyAlgorithm: String, certSignAlgorithm: String, paramSpec: AlgorithmParameterSpec) {
        val keyPair = createKeyPairGenerator(keyAlgorithm).apply { initialize(paramSpec) }.generateKeyPair()
        keyStore().apply {
            setKeyEntry(alias, keyPair.private, "".toCharArray(), arrayOf(keyPair.buildSelfSignedCertificate(certSignAlgorithm)))
            save()
        }
    }

    override fun clearAll() {
        keyStore().aliases().toList().forEach {
            deleteKey(it)
        }
        file.delete()
    }

    private fun loadKeyStore(): KeyStore {
        val keyStore = createBCKeyStore(KEY_STORE)
        val input: InputStream? = readDecrypted()?.let { ByteArrayInputStream(it) }
        input?.use { keyStore.load(it, null) } ?: keyStore.load(null, null)
        return keyStore
    }

    private fun KeyStore.save() {
        val output = ByteArrayOutputStream()
        store(output, null)
        writeEncrypted(output.toByteArray())
    }

    private fun KeyPair.buildSelfSignedCertificate(signAlgorithm: String): Certificate {
        val signer = createBCContentSignerBuilder(signAlgorithm).build(private)
        val certificateBytes = createCertificateBuilder(public).build(signer).encoded
        val certificateFactory = createBCCertificateFactory("X.509")
        return certificateBytes.inputStream().use { certificateFactory.generateCertificate(it) }
    }

    private fun createCertificateBuilder(publicKey: PublicKey): X509v3CertificateBuilder {
        val now = Instant.now()
        return X509v3CertificateBuilder(
            /* issuer = */
            X500Name(CN),
            /* serial = */
            System.currentTimeMillis().toBigInteger(),
            /* notBefore = */
            Date.from(now),
            /* notAfter = */
            Date.from(now.plus(60 * 30, ChronoUnit.DAYS)),
            /* subject = */
            X500Name(CN),
            /* publicKeyInfo = */
            SubjectPublicKeyInfo.getInstance(publicKey.encoded)
        )
    }
}
