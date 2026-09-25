package ee.cyber.wallet.security

import android.content.Context
import ee.cyber.wallet.BuildConfig
import arrow.core.Either
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader
import eu.europa.esig.dss.spi.client.http.DSSFileLoader
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource
import eu.europa.esig.dss.tsl.cache.CacheCleaner
import eu.europa.esig.dss.tsl.job.TLValidationJob
import eu.europa.esig.dss.tsl.source.LOTLSource
import eu.europa.esig.dss.tsl.sync.ExpirationAndSignatureCheckStrategy
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.cert.X509Certificate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Predicate
import kotlin.time.measureTimedValue

private val logger = LoggerFactory.getLogger("FetchLOTLCertificates")

/**
 * Interface for fetching certificates from a List of Trusted Lists (LOTL).
 */
fun interface FetchLOTLCertificates {
    /**
     * Fetches X509 certificates from the configured LOTL.
     *
     * @param trustedListConfig Configuration for the LOTL
     * @return Either a Throwable on failure, or a list of X509 certificates on success
     */
    suspend operator fun invoke(
        trustedListConfig: TrustedListConfig
    ): Either<Throwable, List<X509Certificate>>
}

/**
 * Implementation of FetchLOTLCertificates using the EU DSS library.
 *
 * This class fetches and validates certificates from a List of Trusted Lists (LOTL)
 * using the Digital Signature Service (DSS) library. It supports caching, offline/online
 * data loading, and signature verification.
 *
 * @param context Android context for accessing cache directory
 * @param executorService ExecutorService for concurrent operations (default: 4 threads)
 */
class FetchLOTLCertificatesDSS(
    private val context: Context,
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4)
) : FetchLOTLCertificates {
    private val dispatcher = executorService.asCoroutineDispatcher()

    /**
     * Cleans up resources when this instance is no longer needed.
     *
     * D15 (jvm L8): the executor that backs both this dispatcher and the
     * TLValidationJob is shut down here — previously only the coroutine
     * dispatcher view was closed, which detaches it but leaves the 4 pool
     * threads alive (non-daemon), leaking them for the process lifetime.
     * Idempotent: shutdown() on an already-shutdown pool is a no-op.
     */
    fun destroy() {
        dispatcher.close()
        executorService.shutdown()
    }

    override suspend fun invoke(
        trustedListConfig: TrustedListConfig
    ): Either<Throwable, List<X509Certificate>> = Either.catch {
        val trustedListsCertificateSource = TrustedListsCertificateSource()
        val tlCacheDirectory = File(context.cacheDir, "lotl-cache").apply {
            if (exists()) {
                deleteRecursively()
            }
            mkdirs()
        }
        val onlineLoader: DSSFileLoader = FileCacheDataLoader().apply {
            setCacheExpirationTime(24 * 60 * 60 * 1000)
            setFileCacheDirectory(tlCacheDirectory)
            dataLoader = CommonsDataLoader()
        }
        val validationJob = TLValidationJob().apply {
            setListOfTrustedListSources(lotlSource(trustedListConfig))
            setOnlineDataLoader(onlineLoader)
            setTrustedListCertificateSource(trustedListsCertificateSource)
            // E2 (codesec CS-M4): the sync strategy is the fail-closed change —
            // AcceptAllStrategy imported whatever the LOTL endpoint served (any content, any
            // signature state) into the trust source; ExpirationAndSignatureCheckStrategy
            // refuses material that is expired or fails the LOTL signature check. The
            // strategy name is verified present in dss-tsl-validation (1.02.x line).
            setSynchronizationStrategy(ExpirationAndSignatureCheckStrategy())
            setCacheCleaner(CacheCleaner())
            setExecutorService(executorService)
            // D15 (jvm L8): DSS debug logging dumps every fetched certificate (subject/issuer/
            // serial/validity) to logcat; in release that is noise plus metadata leakage, so the
            // verbose mode rides the debuggable flag like the rest of the trust-all surface.
            setDebug(BuildConfig.DEBUG)
        }

        logger.info("Starting LOTL validation job for: ${trustedListConfig.location}")
        val (certs, duration) = measureTimedValue {
            withContext(dispatcher) {
                validationJob.onlineRefresh()
            }

            trustedListsCertificateSource.certificates.map {
                it.certificate
            }
        }
        logger.info("Finished LOTL validation job in $duration. Found ${certs.size} certificates")
        certs.forEachIndexed { index, cert ->
            logger.info(
                """
                Certificate ${index + 1}:
                  Subject: ${cert.subjectDN}
                  Issuer: ${cert.issuerDN}
                  Serial Number: ${cert.serialNumber}
                  Valid From: ${cert.notBefore}
                  Valid Until: ${cert.notAfter}
                  ---
                """.trimIndent()
            )
        }
        certs
    }

    private suspend fun lotlSource(
        trustedListConfig: TrustedListConfig
    ): LOTLSource {
        val lotlSource = LOTLSource()
        lotlSource.url = trustedListConfig.location.toExternalForm()

        // E2 (codesec CS-M4): the keystore is the LOTL signature-verification material. A nil
        // certSource makes DSS treat the LOTL as unsigned and silently sync ZERO anchors (the
        // decompiled 1.02.x path maps a missing source to validationError, never to an
        // exception the caller sees) — "unknown trust state" that looked like success. The
        // load failure therefore aborts the sync: the IllegalStateException propagates out of
        // invoke()'s Either.catch as a typed Left instead of yielding a silently-empty
        // anchor set. (invoke() logs it; the initializer keeps static anchors only.)
        val certSource = lotlCertificateSource(trustedListConfig.keystoreConfig).fold(
            ifLeft = { error ->
                logger.error("LOTL signing certificate source unavailable — refusing to sync trust anchors", error)
                throw IllegalStateException(
                    "LOTL signing keystore unavailable — refusing to sync trust anchors without signature verification",
                    error
                )
            },
            ifRight = { certSource ->
                logger.info("Loaded LOTL certificate source with ${certSource.certificates.size} certificates")
                certSource
            }
        )
        lotlSource.certificateSource = certSource

        lotlSource.isPivotSupport = true
        lotlSource.trustServicePredicate = Predicate { tspServiceType ->
            tspServiceType.serviceInformation.serviceTypeIdentifier == trustedListConfig.serviceTypeIdentifier
        }
        return lotlSource
    }

    private suspend fun lotlCertificateSource(
        keystoreConfig: KeyStoreConfig
    ): Either<Throwable, KeyStoreCertificateSource> =
        withContext(dispatcher + CoroutineName("LotlCertificateSource")) {
            Either.catch {
                val keystoreStream = ByteArrayOutputStream().apply {
                    keystoreConfig.keystore.store(this, keystoreConfig.keystorePassword?.toCharArray())
                }.toByteArray().inputStream()

                KeyStoreCertificateSource(
                    keystoreStream,
                    keystoreConfig.keystoreType,
                    keystoreConfig.keystorePassword!!.toCharArray()
                )
            }
        }
}
