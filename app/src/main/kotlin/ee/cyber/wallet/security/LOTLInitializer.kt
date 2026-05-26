package ee.cyber.wallet.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ee.cyber.wallet.AppConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.URL
import java.security.KeyStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializes LOTL (List of Trusted Lists) certificates on application startup.
 *
 * This class fetches certificates from the configured LOTL location and
 * updates CertificateChainValidator with them for use in certificate validation.
 *
 * @param context Application context for accessing assets
 * @param fetchLOTLCertificates The LOTL certificate fetcher
 */
@Singleton
class LOTLInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fetchLOTLCertificates: FetchLOTLCertificatesDSS,
) {
    private val logger = LoggerFactory.getLogger(LOTLInitializer::class.java)

    private val lotlKeystoreConfig: KeyStoreConfig? by lazy {
        loadLotlKeystore()
    }

    /**
     * Initialize LOTL certificates asynchronously.
     * This should be called early in the application lifecycle.
     *
     * @param scope CoroutineScope to use for the async operation
     */
    fun initialize(scope: CoroutineScope) {
        if (!AppConfig.lotlEnabled) {
            logger.info("LOTL is disabled. Using static trust anchors only.")
            return
        }

        scope.launch {
            fetchCertificates()
        }
    }

    /**
     * Manually refresh LOTL certificates.
     * Can be called when the app comes to foreground or periodically.
     *
     * @param scope CoroutineScope to use for the async operation
     */
    fun refresh(scope: CoroutineScope) {
        if (!AppConfig.lotlEnabled) {
            return
        }

        scope.launch {
            fetchCertificates()
        }
    }

    /**
     * Clean up resources.
     */
    fun destroy() {
        fetchLOTLCertificates.destroy()
    }

    private suspend fun fetchCertificates() {
        try {
            logger.info("Initializing LOTL certificates from: ${AppConfig.lotlLocation}")

            val trustedListConfig = TrustedListConfig(
                location = URL(AppConfig.lotlLocation),
                serviceTypeIdentifier = AppConfig.lotlServiceTypeFilter,
                keystoreConfig = lotlKeystoreConfig!!
            )

            fetchLOTLCertificates(trustedListConfig).fold(
                ifLeft = { error ->
                    logger.error("Failed to fetch LOTL certificates. Will use static certificates only.", error)
                },
                ifRight = { certificates ->
                    logger.info("Successfully fetched ${certificates.size} certificates from LOTL")
                    CertificateChainValidator.updateLOTLCertificates(certificates)
                    logger.info("LOTL certificates cached and ready for use")
                }
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during LOTL initialization", e)
        }
    }

    private fun loadLotlKeystore(): KeyStoreConfig? {
        return try {

            val keyStore = KeyStore.getInstance("PKCS12")
            context.assets.open("certs/lotl-keystore.p12").use { inputStream ->
                keyStore.load(inputStream, "changeit".toCharArray())
            }

            logger.info("Loaded LOTL keystore from assets with ${keyStore.size()} entries")
            KeyStoreConfig(
                keystoreType = "PKCS12",
                keystorePassword = "changeit",
                keystore = keyStore
            )
        } catch (e: Exception) {
            logger.warn("Failed to load LOTL keystore from assets: ${e.message}")
            null
        }
    }
}
