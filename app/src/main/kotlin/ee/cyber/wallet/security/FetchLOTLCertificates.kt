package ee.cyber.wallet.security

import android.content.Context
import arrow.core.Either
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader
import eu.europa.esig.dss.spi.client.http.DSSFileLoader
import eu.europa.esig.dss.spi.tsl.TrustedListsCertificateSource
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource
import eu.europa.esig.dss.tsl.cache.CacheCleaner
import eu.europa.esig.dss.tsl.job.TLValidationJob
import eu.europa.esig.dss.tsl.source.LOTLSource
import eu.europa.esig.dss.tsl.sync.AcceptAllStrategy
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
        trustedListConfig: TrustedListConfig,
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
    private val executorService: ExecutorService = Executors.newFixedThreadPool(4),
) : FetchLOTLCertificates {
    private val dispatcher = executorService.asCoroutineDispatcher()

    /**
     * Cleans up resources when this instance is no longer needed.
     */
    fun destroy() {
        dispatcher.close()
    }

    override suspend fun invoke(
        trustedListConfig: TrustedListConfig,
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
            setSynchronizationStrategy(AcceptAllStrategy()) // TODO: Use ExpirationAndSignatureCheckStrategy
            setCacheCleaner(CacheCleaner())
            setExecutorService(executorService)
            setDebug(true)
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
        trustedListConfig: TrustedListConfig,
    ): LOTLSource {
        val lotlSource = LOTLSource()
        lotlSource.url = trustedListConfig.location.toExternalForm()

        trustedListConfig.keystoreConfig?.let { keystoreConfig ->
            logger.info("Loading LOTL certificate source")
            lotlCertificateSource(keystoreConfig).fold(
                ifLeft = { error ->
                    logger.error("Failed to load LOTL certificate source", error)
                },
                ifRight = { certSource ->
                    logger.info("Loaded LOTL certificate source with ${certSource.certificates.size} certificates")
                    lotlSource.certificateSource = certSource
                }
            )
        } ?: logger.warn("No keystore config provided for LOTL signature verification")

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
