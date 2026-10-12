package ee.cyber.wallet.domain.provider.ageverification

import ee.cyber.wallet.data.database.KeyAttestationEntity
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import ee.cyber.wallet.domain.provider.wallet.WalletInstanceCredentials
import ee.cyber.wallet.domain.provider.wallet.WalletProviderService
import ee.cyber.wallet.security.SecureAreaKeyCleanup
import ee.cyber.wallet.security.SecureAreaKeyManager
import ee.cyber.wallet.security.jwk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * The production [BatchAgeIssuer]: one transaction's keys and mints (conformance plan §4 item 6,
 * EE-POA-003/011).
 *
 * The EE-PoA batch keys are created through [SecureAreaKeyManager.batchCreateKey] — all of
 * them before the first mint, rolled back by the manager if creation fails partway; pre-generation off the critical
 * path (EE-POA-011a) stays in plan §8.4 — and each is attested by the (mock) wallet provider and
 * registered in the keyAttestation table exactly like a single generated key, so SecureArea
 * cleanup keeps seeing every alias. The AV attestation is batched the same way (EE-POA-003). The
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

    override suspend fun issueBatch(credential: Credential, count: Int): List<Attestation> =
        withContext(dispatcher) {
            require(count >= 1) { "an age attestation batch has at least one attestation" }

            // All keys exist before the first mint (EE-POA-011a), so a mid-batch mint failure
            // leaves no half-issued TRANSACTION; batchCreateKey cleans up its own partial
            // creation. Past it, a failure in attest/mint/insert leaves already-created aliases — and the keyAttestation rows
            // written for the earlier keys — behind. Roll both back, best-effort, rather than
            // orphan live signing keys or leave rows naming deleted ones (second review, finding
            // 5). A key that survives its deletion keeps its row, so the data wipe still reaches it.
            val keys = secureAreaKeyManager.batchCreateKey(count).keys
            try {
                keys.map { key ->
                    val keyAttestation = registerAttestedKey(key.keyId, key.jwk())
                    issueCredential(credential, keyAttestation)
                }.also {
                    logger.info("issued an age attestation batch of {}", it.size)
                }
            } catch (e: Exception) {
                // NonCancellable: a cancelled issuance must still roll back.
                withContext(NonCancellable) {
                    SecureAreaKeyCleanup(keyAttestationDao, secureAreaKeyManager).deleteKeys(keys.map { it.keyId })
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
