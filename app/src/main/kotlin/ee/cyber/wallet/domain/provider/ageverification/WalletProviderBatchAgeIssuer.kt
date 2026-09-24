package ee.cyber.wallet.domain.provider.ageverification

import ee.cyber.wallet.data.database.KeyAttestationEntity
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import ee.cyber.wallet.domain.provider.wallet.WalletInstanceCredentials
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.security.SecureAreaKeyManager
import ee.cyber.wallet.security.jwk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * The production [BatchAgeIssuer]: one transaction's keys and mints (conformance plan §4 item 6,
 * EE-POA-003/011).
 *
 * The EE-PoA batch keys are created through [SecureAreaKeyManager.batchCreateKey] — one
 * Android Keystore call for the whole batch, at issuance time; pre-generation off the critical
 * path (EE-POA-011a) stays in plan §8.4 — and each is attested by the (mock) wallet provider and
 * registered in the keyAttestation table exactly like a single generated key, so SecureArea
 * cleanup keeps seeing every alias. The AV attestation gets a single key on the same manager. The
 * Android Keystore interaction itself is PENDING-DEVICE (docs/planning/STEP6-RECORD.md); the JVM
 * acceptance test drives the mint through the [BatchAgeIssuer] seam with fake keys instead.
 */
class WalletProviderBatchAgeIssuer(
    private val secureAreaKeyManager: SecureAreaKeyManager,
    private val walletProviderService: WalletProviderService,
    private val keyAttestationDao: ee.cyber.wallet.data.database.dao.KeyAttestationDao,
    private val credentials: suspend () -> WalletInstanceCredentials,
    private val issueCredential: suspend (Credential, KeyAttestation) -> Attestation,
    private val dispatcher: CoroutineDispatcher
) : BatchAgeIssuer {

    private val logger = LoggerFactory.getLogger("WalletProviderBatchAgeIssuer")

    override suspend fun generateAvKey(): KeyAttestation {
        val key = secureAreaKeyManager.generateKey()
        return registerAttestedKey(key.keyId, key.jwk())
    }

    override suspend fun issuePoaBatch(credential: Credential.EePoaCredential, count: Int): List<Attestation> =
        withContext(dispatcher) {
            require(count >= 1) { "an EE-PoA batch has at least one attestation" }

            // One SecureArea call for the whole batch (EE-POA-011a): the keys exist before the
            // first mint, so a mid-batch mint failure leaves no half-issued TRANSACTION. The key
            // creation itself is transactional in neither Keystore nor the mock, so a failure in
            // attest/mint/insert leaves already-created aliases no record names — delete them
            // best-effort rather than orphan live signing keys (second review, finding 5).
            val keys = secureAreaKeyManager.batchCreateKey(count).keys
            try {
                keys.map { key ->
                    val keyAttestation = registerAttestedKey(key.keyId, key.jwk())
                    issueCredential(credential, keyAttestation)
                }.also {
                    logger.info("issued an EE-PoA batch of {}", it.size)
                }
            } catch (e: Exception) {
                keys.forEach { key ->
                    runCatching { secureAreaKeyManager.deleteKey(key.keyId) }
                        .onFailure { cleanupError ->
                            logger.error("batch cleanup could not delete alias {}", key.keyId, cleanupError)
                        }
                }
                throw e
            }
        }

    /** Wallet-provider attestation + keyAttestation row, mirroring LocalCryptoProvider's path. */
    private suspend fun registerAttestedKey(keyId: String, jwk: com.nimbusds.jose.jwk.ECKey): KeyAttestation {
        val attestation = walletProviderService.attestKey(
            keyId = keyId,
            keyType = KeyType.EC,
            jwk = jwk,
            credentials = credentials()
        )
        keyAttestationDao.insert(
            KeyAttestationEntity(
                id = attestation.keyId,
                attestation = attestation.attestation,
                keyType = KeyType.EC.name
            )
        )
        return attestation
    }
}
