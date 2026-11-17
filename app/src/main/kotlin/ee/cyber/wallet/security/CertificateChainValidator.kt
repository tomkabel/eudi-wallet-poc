package ee.cyber.wallet.security

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
import java.security.cert.PKIXCertPathBuilderResult
import java.security.cert.TrustAnchor
import java.security.cert.X509CRL
import java.security.cert.X509CertSelector
import java.security.cert.X509Certificate
import java.security.cert.CertificateFactory
import java.util.Date

/**
 * Represents the current state of LOTL certificates.
 */
data class LotlStatus(
    val isSynced: Boolean = false,
    val certificateCount: Int = 0
)

/** Certificate profile a chain is validated against (PKI plan NEW-S2-1). */
enum class ChainProfile { GENERIC, MDOC_DS }

/**
 * Which anchors a call site may use (PKI plan NEW-S2-2). LOTL anchors are synced for the
 * relying-party access service type only, so only RP authentication may use them.
 */
enum class TrustDomain { STATIC, STATIC_AND_LOTL }

/** Validates X.509 certificate chains; see [validateCertificateChain]. */
object CertificateChainValidator {

    private val logger = LoggerFactory.getLogger(CertificateChainValidator::class.java)

    @Volatile
    private var lotlCertificates: List<X509Certificate> = emptyList()

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

    /**
     * Returns the number of LOTL certificates currently loaded.
     */
    fun getLOTLCertificateCount(): Int = lotlCertificates.size

    /**
     * Returns whether LOTL certificates have been synced (at least one certificate loaded).
     */
    fun isLOTLSynced(): Boolean = lotlCertificates.isNotEmpty()

    /**
     * The one trust decision for every X.509 chain the wallet accepts.
     *
     * - [profile]: [ChainProfile.MDOC_DS] adds the ISO/IEC 18013-5 document signer rules
     *   (PKI plan NEW-S2-1); [ChainProfile.GENERIC] is RFC 5280 path validation.
     * - [domain]: LOTL-derived anchors are relying-party access CAs, so only
     *   [TrustDomain.STATIC_AND_LOTL] call sites (OpenID4VP client and reader authentication)
     *   may use them. Issuer chains stay on their static anchors (NEW-S2-2).
     * - Every supplied certificate must meet the shared algorithm policy (NEW-S1-2).
     * - [at] fixes the validation instant for the shared trust vectors; null means now.
     *
     * Revocation (E4) is soft-fail on availability: a verified CRL that names a chain
     * certificate refuses the chain; an expired, unreachable or unverifiable CRL is logged and
     * does not. The in-tree dev CRL's nextUpdate passed on 2025-05-20, so hard-fail would refuse
     * every chain today. Tightening this is decision D1 of docs/planning/PKI-REMEDIATION-PLAN.md.
     */
    fun validateCertificateChain(
        certificateChain: List<X509Certificate>,
        trustedRootCertificates: List<X509Certificate>,
        enableRevocationCheck: Boolean = true,
        profile: ChainProfile = ChainProfile.GENERIC,
        domain: TrustDomain = TrustDomain.STATIC,
        at: Date? = null
    ): Boolean {
        return runCatching {
            require(certificateChain.isNotEmpty()) { "Certificate chain cannot be empty" }
            val lotl = if (domain == TrustDomain.STATIC_AND_LOTL) lotlCertificates else emptyList()
            val allTrustAnchors = trustedRootCertificates + lotl

            require(allTrustAnchors.isNotEmpty()) {
                "At least one trusted root certificate is required (static, or LOTL where the domain allows it)"
            }
            certificateChain.firstNotNullOfOrNull { cert ->
                AlgorithmPolicy.violation(cert)?.let { "${cert.subjectX500Principal}: $it" }
            }?.let { reason ->
                logger.warn("Algorithm policy refuses the chain: {}", reason)
                return false
            }
            if (profile == ChainProfile.MDOC_DS) {
                mdocDsViolation(certificateChain.first())?.let { reason ->
                    logger.warn("Document signer profile refuses the chain: {}", reason)
                    return false
                }
            }

            logger.debug(
                "Validating certificate chain ({}, {}) with {} static + {} LOTL trust anchors",
                profile,
                domain,
                trustedRootCertificates.size,
                lotl.size
            )

            val pkixParams = PKIXBuilderParameters(
                allTrustAnchors.map { TrustAnchor(it, null) }.toSet(),
                X509CertSelector().apply { certificate = certificateChain.first() }
            ).apply {
                isRevocationEnabled = false
                at?.let { date = it }
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
                    // an external revocation checker. The policy below is the SOFT_FAIL
                    // semantic made explicit and testable:
                    //  - a fetched, signature-verified CRL that NAMES a chain certificate
                    //    refuses the chain outright (revoked = refuse, always);
                    //  - a CRL that cannot be fetched, is expired, or fails signature
                    //    verification is an AVAILABILITY failure: logged loudly, never
                    //    treated as proof of revocation.
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
            val built = CertPathBuilder.getInstance("PKIX").build(pkixParams) as PKIXCertPathBuilderResult
            // RFC 5280 treats an anchor as a bare name and key, so the JDK ignores the anchor
            // certificate's own pathLenConstraint. An IACA with pathLen 0 must still forbid a
            // sub-CA (ISO/IEC 18013-5); Go's x509 enforces it, and the shared vectors caught
            // the difference (pathlen_violated).
            val anchorPathLen = built.trustAnchor.trustedCert?.basicConstraints ?: -1
            val intermediates = built.certPath.certificates.size - 1
            if (anchorPathLen in 0 until intermediates) {
                logger.warn("Anchor pathLenConstraint {} forbids {} intermediate CA(s) — refusing", anchorPathLen, intermediates)
                return false
            }
            true
        }.onFailure {
            logger.error("Failed to validate certificate chain", it)
        }.getOrDefault(false)
    }

    /** ISO/IEC 18013-5 document signer rules: not a CA, digitalSignature, the mdlDS EKU. */
    private fun mdocDsViolation(leaf: X509Certificate): String? = when {
        leaf.basicConstraints != -1 -> "${leaf.subjectX500Principal} is marked CA"
        leaf.keyUsage?.getOrNull(0) != true -> "${leaf.subjectX500Principal} lacks keyUsage digitalSignature"
        leaf.extendedKeyUsage?.contains(OID_MDL_DS) != true -> "${leaf.subjectX500Principal} lacks the mdlDS EKU $OID_MDL_DS"
        else -> null
    }

    /** id-mdl-kp-mdlDS, ISO/IEC 18013-5. */
    private const val OID_MDL_DS = "1.0.18013.5.1.2"

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
