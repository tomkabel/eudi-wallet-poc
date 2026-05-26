package ee.cyber.wallet.security

import java.net.URL
import java.security.KeyStore

/**
 * Configuration for a List of Trusted Lists (LOTL).
 *
 * @param location URL of the LOTL XML document
 * @param serviceTypeIdentifier filter for service provider type
 * @param keystoreConfig Optional keystore containing the public key used to sign the LOTL
 */
data class TrustedListConfig(
    val location: URL,
    val serviceTypeIdentifier: String,
    val keystoreConfig: KeyStoreConfig,
)

/**
 * Configuration for a KeyStore containing trusted certificates.
 *
 * @param keystoreType Type of keystore (JKS, PKCS12, etc.)
 * @param keystorePassword Password for the keystore
 * @param keystore The loaded KeyStore instance
 */
data class KeyStoreConfig(
    val keystoreType: String = "PKCS12",
    val keystorePassword: String? = "",
    val keystore: KeyStore,
)