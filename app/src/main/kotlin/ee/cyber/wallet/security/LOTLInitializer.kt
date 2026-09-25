package ee.cyber.wallet.security

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import ee.cyber.wallet.AppConfig
import ee.cyber.wallet.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
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
    private val fetchLOTLCertificates: FetchLOTLCertificatesDSS
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

            // E2 (codesec CS-M4): the keystore carries the LOTL signature-verification keys.
            // With it absent the sync cannot verify the LOTL signature, so proceeding would
            // mean either silently-zero anchors (the decompiled DSS nil-certSource path) or
            // unverified anchor import (AcceptAllStrategy) — both worse than staying on the
            // static anchors. `!!` here is deliberate and safe: the config is lazy-cached and
            // fetchCertificates is its only consumer; a null here is the loadLotlKeystore
            // failure logged at first touch, and the fold below records the fail-closed state.
            val keystoreConfig = lotlKeystoreConfig
            if (keystoreConfig == null) {
                logger.error(
                    "LOTL signing keystore unavailable — refusing to sync trust anchors; " +
                        "the validator stays on static anchors only"
                )
                return
            }

            val trustedListConfig = TrustedListConfig(
                location = URL(AppConfig.lotlLocation),
                serviceTypeIdentifier = AppConfig.lotlServiceTypeFilter,
                keystoreConfig = keystoreConfig
            )

            fetchLOTLCertificates(trustedListConfig).fold(
                ifLeft = { error ->
                    // Fail closed: an unreachable/unsigned/failed LOTL leaves the previously
                    // synced anchors (empty on first launch) untouched — zero-anchor LOTL
                    // state is never installed, static anchors remain the trust base.
                    logger.error("LOTL sync failed; keeping static trust anchors only.", error)
                },
                ifRight = { certificates ->
                    if (certificates.isEmpty()) {
                        // ExpirationAndSignatureCheckStrategy can lawfully yield an empty set
                        // (e.g. every TL expired). Installing zero LOTL anchors is not a sync
                        // success — it silently shrinks the trust base, so refuse it.
                        logger.error("LOTL sync yielded zero anchors — refusing to update the validator; static anchors stay")
                        return
                    }
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
            // E3 (codesec CS-L4 / jvm JVM-L7): tamper check BEFORE the load. The packaged
            // keystore bytes are pinned to a build-time digest (LOTL_KEYSTORE_SHA256, set in
            // app/build.gradle.kts); a repackaged APK whose asset differs is refused and the
            // wallet stays on static anchors. Note the password is 'changeit' — documented,
            // not hidden: this PKCS12 carries PUBLIC LOTL-signing certificates only (no
            // private keys) and ships inside the APK's assets, so a secret would be security
            // theatre; the digest pin above is the tamper control, not a password.
            val pinnedDigest = BuildConfig.LOTL_KEYSTORE_SHA256
            val actualDigest = context.assets.open("certs/lotl-keystore.p12").use { stream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                generateSequence { stream.read(buffer) }.takeWhile { it != -1 }.forEach { n ->
                    digest.update(buffer, 0, n)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
            if (actualDigest != pinnedDigest) {
                logger.error(
                    "LOTL keystore digest mismatch (pinned {}, packaged {}) — refusing to load; static anchors stay",
                    pinnedDigest, actualDigest
                )
                return null
            }

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
