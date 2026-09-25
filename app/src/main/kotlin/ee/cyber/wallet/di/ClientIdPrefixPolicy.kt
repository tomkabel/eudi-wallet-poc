package ee.cyber.wallet.di

import eu.europa.ec.eudi.openid4vp.SupportedClientIdPrefix
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust

/**
 * The client-id prefixes this wallet accepts, extracted for unit testing (B4).
 *
 * The [SupportedClientIdPrefix.RedirectUri] member is deliberately absent: a
 * bare redirect URI as client_id has no cryptographic binding to a registered
 * client — any web origin could mint a request and impersonate a verifier
 * (identity A-M3 / codesec CS-M5). Only JWS-verified request objects under the
 * x509 SAN-DNS / x509 hash schemes are accepted; both schemes resolve through
 * the same chain validator, so a request whose certificate chain does not
 * anchor at the trusted set is refused before the consent screen.
 */
fun verifiableClientIdPrefixes(
    chainValidator: X509CertificateTrust
): List<SupportedClientIdPrefix> = listOf(
    SupportedClientIdPrefix.X509SanDns(chainValidator),
    SupportedClientIdPrefix.X509Hash(chainValidator)
)
