package ee.cyber.wallet.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.security.cert.CertPathBuilder
import java.security.cert.CertStore
import java.security.cert.CollectionCertStoreParameters
import java.security.cert.PKIXBuilderParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate

/**
 * Represents the current state of LOTL certificates.
 */
data class LotlStatus(
    val isSynced: Boolean = false,
    val certificateCount: Int = 0
)

/**
 * Validates an X.509 certificate chain.
 *
 * @param certificateChain The certificate chain with the leaf certificate as the first element.
 * @param lotlCertificates A list of trusted root certificates (must not be empty).
 * @param enableRevocationCheck Flag to enable or disable certificate revocation checking.
 * @return `true` if the certificate chain is valid, `false` otherwise.
 * @throws IllegalArgumentException if certificateChain or trustedRootCertificates is empty.
 */
object CertificateChainValidator {

    private val logger = LoggerFactory.getLogger(CertificateChainValidator::class.java)

    @Volatile
    private var lotlCertificates: List<X509Certificate> = emptyList()

    @Volatile
    private var trustAll: Boolean = false

    private val _lotlStatus = MutableStateFlow(LotlStatus())

    /**
     * Observable state flow of LOTL status updates.
     */
    val lotlStatus: StateFlow<LotlStatus> = _lotlStatus.asStateFlow()

    fun updateLOTLCertificates(certificates: List<X509Certificate>) {
        lotlCertificates = certificates
        _lotlStatus.value = LotlStatus(
            isSynced = certificates.isNotEmpty(),
            certificateCount = certificates.size
        )
        logger.info("Updated trusted root certificates with ${certificates.size} certificates")
    }

    fun setTrustAll(enabled: Boolean) {
        trustAll = enabled
        logger.info("Trust all validator mode: $enabled")
    }

    /**
     * Returns the number of LOTL certificates currently loaded.
     */
    fun getLOTLCertificateCount(): Int = lotlCertificates.size

    /**
     * Returns whether LOTL certificates have been synced (at least one certificate loaded).
     */
    fun isLOTLSynced(): Boolean = lotlCertificates.isNotEmpty()

    fun validateCertificateChain(
        certificateChain: List<X509Certificate>,
        trustedRootCertificates: List<X509Certificate>,
        enableRevocationCheck: Boolean = true
    ): Boolean {
        return runCatching {
            if (trustAll) {
                logger.warn("### Trust all validator mode is enabled - skipping certificate chain validation ###")
                if (certificateChain.isNotEmpty()) {
                    val cert = certificateChain.first()
                    logger.warn("Trusting certificate - Subject: ${cert.subjectX500Principal}, Issuer: ${cert.issuerX500Principal}, Serial: ${cert.serialNumber}, NotBefore: ${cert.notBefore}, NotAfter: ${cert.notAfter}")
                }
                return true
            }
            require(certificateChain.isNotEmpty()) { "Certificate chain cannot be empty" }
            val allTrustAnchors = buildList {
                addAll(trustedRootCertificates)
                if (lotlCertificates.isNotEmpty()) {
                    addAll(lotlCertificates)
                }
            }

            require(allTrustAnchors.isNotEmpty()) {
                "At least one trusted root certificate is required (static or LOTL)"
            }

            logger.debug(
                "Validating certificate chain with ${trustedRootCertificates.size} static + " +
                    "${lotlCertificates.size} LOTL trust anchors"
            )

            val pkixParams = PKIXBuilderParameters(
                allTrustAnchors.map { TrustAnchor(it, null) }.toSet(),
                X509CertSelector().apply { certificate = certificateChain.first() }
            ).apply {
                isRevocationEnabled = enableRevocationCheck
                addCertStore(
                    CertStore.getInstance(
                        "Collection",
                        CollectionCertStoreParameters(certificateChain)
                    )
                )
            }
            CertPathBuilder.getInstance("PKIX").build(pkixParams)
            true
        }.onFailure {
            logger.error("Failed to validate certificate chain", it)
        }.getOrDefault(false)
    }
}
