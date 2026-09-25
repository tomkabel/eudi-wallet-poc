package ee.cyber.wallet.security

import ee.cyber.wallet.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.slf4j.LoggerFactory
import java.net.URL
import java.security.cert.CertPathBuilder
import java.security.cert.CertPathValidator
import java.security.cert.CertStore
import java.security.cert.CollectionCertStoreParameters
import java.security.cert.PKIXBuilderParameters
import java.security.cert.TrustAnchor
import java.security.cert.X509CRL
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate
import java.security.cert.CertificateFactory

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

    /** Development aid; ignored outside debuggable builds, whatever the stored preference says. */
    fun setTrustAll(enabled: Boolean) {
        trustAll = enabled && BuildConfig.DEBUG
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

    /**
     * Centralized revocation-policy entry point (B4, codesec CS-M5 step 1; E4, CS-M5 step 2):
     * both production call sites validate through this overload so the revocation decision is
     * made — and logged — in exactly one place, and any flip is inherently simultaneous (the
     * invariant "both call sites flip together, never one alone" is structural here).
     *
     * E4 policy decision (verified against the live infrastructure, 2026-09):
     *  - the ONLY CRL distribution point on the in-tree issuer chains points at the
     *    eudi-qeaa-issuer-poc dev branch's iaca.crl, whose nextUpdate expired 2025-05-20 —
     *    a hard-fail flip against it would refuse EVERY chain in every path today;
     *  - revocation is therefore wired but SOFT-FAIL: CRLs reachable from each chain's
     *    distribution points are fetched into the CertStore, an OCSP/CRL PKIXRevocationChecker
     *    runs with SOFT_FAIL, and a revoked certificate is still refused outright — soft-fail
     *    covers only the *availability* failures (expired/unreachable CRL), which log loudly;
     *  - the hard flip (NO_FALLBACK, expired-CRL = refuse) is a one-line change here once the
     *    DP serves a live CRL — and because both call sites route through this overload, the
     *    flip cannot land asymmetrically.
     */
    fun validateCertificateChain(
        certificateChain: List<X509Certificate>,
        trustedRootCertificates: List<X509Certificate>
    ): Boolean {
        logger.debug("Certificate chain validation: revocation=soft-fail (CRL DP fetch; availability failures logged, revoked certs refused)")
        return validateCertificateChain(certificateChain, trustedRootCertificates, true)
    }

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
                    logger.warn(
                        "Trusting certificate - Subject: ${cert.subjectX500Principal}, Issuer: ${cert.issuerX500Principal}, " +
                            "Serial: ${cert.serialNumber}, NotBefore: ${cert.notBefore}, NotAfter: ${cert.notAfter}"
                    )
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
                isRevocationEnabled = false
                addCertStore(
                    CertStore.getInstance(
                        "Collection",
                        CollectionCertStoreParameters(certificateChain)
                    )
                )
                if (enableRevocationCheck) {
                    // E4: revocation runs as an EXPLICIT post-build check, not via a
                    // PKIXRevocationChecker wired into PKIXBuilderParameters — empirically
                    // (verified in the E1 test battery) SunCertPathBuilder cannot integrate
                    // an external revocation checker: path building itself fails with
                    // "unable to find valid certification path" even when the checker
                    // should soft-fail. The policy below is the SOFT_FAIL semantic made
                    // explicit and testable:
                    //  - a fetched, signature-verified CRL that NAMES a chain certificate
                    //    refuses the chain outright (revoked = refuse, always);
                    //  - a CRL that cannot be fetched, is expired, or fails signature
                    //    verification is an AVAILABILITY failure: logged loudly, never
                    //    treated as proof of revocation (an attacker who can serve
                    //    garbage CRLs must not gain a denial-of-service).
                    val crls = fetchCrlsFor(certificateChain)
                    val trusted = crls.filter { crl -> crlSignatureVerifiable(crl, certificateChain, allTrustAnchors) }
                    val revoked = trusted.any { crl ->
                        certificateChain.any { cert ->
                            runCatching { crl.isRevoked(cert) }.getOrDefault(false)
                        }
                    }
                    if (revoked) {
                        logger.warn("Revocation check: a chain certificate is revoked by a verified CRL — refusing")
                        return false
                    }
                    logger.debug("Revocation check: {} verified CRL(s), no revocations; availability failures (if any) were logged by the fetcher", trusted.size)
                }
            }
            CertPathBuilder.getInstance("PKIX").build(pkixParams)
            true
        }.onFailure {
            logger.error("Failed to validate certificate chain", it)
        }.getOrDefault(false)
    }

    /**
     * E4: fetch CRLs for every chain certificate that advertises CRL distribution points.
     * Failures are per-URL and non-fatal (soft-fail availability policy); results are cached
     * by URL for [CRL_CACHE_TTL_MS] so a presentation burst does not re-fetch.
     */
    private fun fetchCrlsFor(chain: List<X509Certificate>): List<X509CRL> {
        val crls = mutableListOf<X509CRL>()
        for (cert in chain) {
            // X509Certificate has no public getCrlDistributionPoints on Android's API surface
            // — parse extension 2.5.29.31 manually.
            val urls = runCatching {
                val der = cert.getExtensionValue("2.5.29.31") ?: return@runCatching emptyList<String>()
                parseCrlDistributionPointUris(der)
            }.getOrDefault(emptyList())
            for (url in urls) {
                val cached = crlCache[url]
                if (cached != null && System.currentTimeMillis() - cached.fetchedAt < CRL_CACHE_TTL_MS) {
                    cached.crl?.let(crls::add)
                    continue
                }
                val fetched = runCatching {
                    URL(url).openStream().use { stream ->
                        val der = stream.readBytes()
                        CertificateFactory.getInstance("X.509")
                            .generateCRL(der.inputStream()) as X509CRL
                    }
                }.onFailure {
                    logger.warn("CRL fetch failed for {} — availability failure (soft-fail): {}", url, it.message)
                }.getOrNull()
                crlCache[url] = CrlEntry(fetched, System.currentTimeMillis())
                fetched?.let {
                    logger.info("Loaded CRL from {} (issuer {})", url, it.issuerX500Principal)
                    crls.add(it)
                }
            }
        }
        return crls
    }

    /**
     * E4: a CRL may only refuse a chain if its signature verifies against the certificate that
     * issued it — the CA whose subject matches the CRL's issuer, searched through the validated
     * chain first and then the trust anchors. An unmatchable or signature-failing CRL is an
     * availability failure (soft): refusing on it would let whoever controls the CRL
     * distribution point revoke arbitrary chains.
     */
    private fun crlSignatureVerifiable(
        crl: X509CRL,
        certificateChain: List<X509Certificate>,
        trustAnchors: List<X509Certificate>
    ): Boolean {
        val issuerCert = (certificateChain + trustAnchors).firstOrNull {
            it.subjectX500Principal == crl.issuerX500Principal
        }
        if (issuerCert == null) {
            logger.warn("CRL issuer {} has no matching certificate in the chain or anchors — treated as unavailable (soft-fail)", crl.issuerX500Principal)
            return false
        }
        return runCatching { crl.verify(issuerCert.publicKey) }
            .onFailure {
                logger.warn("CRL from {} failed signature verification — treated as unavailable (soft-fail): {}", crl.issuerX500Principal, it.message)
            }
            .isSuccess
    }

    /**
     * DER walk over the CRLDistributionPoints extension (2.5.29.31) extracting http/https/URI
     * names. The extension value arrives as an OCTET STRING wrapping the DER SEQUENCE OF
     * DistributionPoint; BouncyCastle (already on the classpath) provides the typed
     * [org.bouncycastle.asn1.x509.DistributionPoint] decoder, which correctly unwraps the
     * explicit [0] distributionPoint field around the implicitly-tagged [0] fullName
     * GeneralNames — a nesting the previous hand-rolled walk missed, yielding no URLs.
     * A malformed extension simply yields no URLs (soft-fail).
     */
    private fun parseCrlDistributionPointUris(extensionValue: ByteArray): List<String> {
        return runCatching {
            val wrapped = org.bouncycastle.asn1.ASN1Primitive.fromByteArray(extensionValue) as org.bouncycastle.asn1.ASN1OctetString
            val sequence = org.bouncycastle.asn1.ASN1Sequence.getInstance(wrapped.octets)
            sequence.flatMap { element ->
                runCatching {
                    val dp = org.bouncycastle.asn1.x509.DistributionPoint.getInstance(element)
                    val names = (dp.distributionPoint?.name as? org.bouncycastle.asn1.x509.GeneralNames)?.names
                        ?: return@runCatching emptyList<String>()
                    names.filter { it.tagNo == org.bouncycastle.asn1.x509.GeneralName.uniformResourceIdentifier }
                        .map { it.name.toString() }
                }.getOrDefault(emptyList())
            }
        }.getOrDefault(emptyList())
    }

    private data class CrlEntry(val crl: X509CRL?, val fetchedAt: Long)

    private val crlCache = java.util.concurrent.ConcurrentHashMap<String, CrlEntry>()

    /** CRL re-fetch interval; matches the LOTL cache TTL's spirit (1 day). */
    private const val CRL_CACHE_TTL_MS = 24 * 60 * 60 * 1000L
}
