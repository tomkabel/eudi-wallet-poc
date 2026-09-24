package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.data.database.dao.AttestationDao
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.security.SecureAreaKeyDeleter
import kotlinx.coroutines.flow.first
import org.slf4j.LoggerFactory

/**
 * Consumes one-time EE-PoA attestations after they are presented (conformance plan §4 item 6).
 *
 * ARF `WIAM_21` / `EE-POA-013`: a plain presentation of `ee.riik.poa.1` consumes the attestation —
 * the attestation row and its SecureArea device key are deleted, and the attestation is never
 * presented again. A zero-knowledge presentation consumes nothing (`EE-ZKP-025`): the proof
 * discloses no identifying material, so re-presenting it stays unlinkable.
 *
 * Never-the-last (`EE-POA-013`): the wallet SHALL NOT consume the last remaining EE-PoA in the
 * batch, because Method A degrades to Method B when the batch is exhausted. This helper refuses
 * when the presented attestation is the only one of its type left.
 *
 * The deletion order mirrors [ee.cyber.wallet.security.SecureAreaKeyCleanup]: the SecureArea key
 * first, then the rows — past the row deletion the alias would be unreachable while the Android
 * Keystore key lived on. SecureArea deletion is a seam ([SecureAreaKeyDeleter]) so the whole
 * decision is JVM-testable; the real deleteKey on hardware is PENDING-DEVICE
 * (docs/planning/STEP6-RECORD.md).
 */
class EePoaConsumption(
    private val attestationDao: AttestationDao,
    private val keyAttestationDao: KeyAttestationDao,
    private val secureAreaKeyDeleter: SecureAreaKeyDeleter
) {

    private val logger = LoggerFactory.getLogger("EePoaConsumption")

    /**
     * Consumes one presented attestation when the rules ask for it. Returns true when the
     * attestation was consumed.
     *
     * @param attestation the attestation that was just presented
     * @param tier the recorded response tier (per response, escalated: one plain document in the
     *   response makes every row linkable, so the escalation is the caller's duty — pass the
     *   escalated tier, and this helper consumes on any non-ZK tier)
     */
    suspend fun consumeAfterPresentation(attestation: Attestation, tier: PresentationTier): Boolean {
        if (attestation.type != CredentialType.EE_POA) return false
        // EE-ZKP-025: a ZK presentation consumes nothing.
        if (tier == PresentationTier.ZERO_KNOWLEDGE) return false

        val siblings = attestationDao.getAll().first().filter { it.attestation.type == CredentialType.EE_POA }
        // EE-POA-013: never the last — Method A degrades to Method B on an exhausted batch.
        if (siblings.size <= 1) {
            logger.info("EE-PoA {} not consumed: last remaining attestation in the batch", attestation.id)
            return false
        }

        // WIAM_21: the key material goes first — SecureAreaKeyCleanup's ordering, for the same
        // reason: past the row deletion the alias is unreachable from wallet records.
        val keyId = attestation.keyAttestation.keyId
        secureAreaKeyDeleter.deleteKey(keyId)
        // Second review, finding 4: deleteKey is best-effort and swallows real failures. Deleting
        // the rows over a key the platform still holds would orphan a live, usable signing key no
        // record names and no sweep can reach — so the row deletion waits for a verified removal.
        // A key that was never there (already consumed by another path) counts as removed.
        if (secureAreaKeyDeleter.keyExists(keyId)) {
            logger.error("EE-PoA {} not consumed: SecureArea key {} still exists after delete", attestation.id, keyId)
            return false
        }
        attestationDao.deleteById(attestation.id)
        keyAttestationDao.deleteById(keyId)
        logger.info("EE-PoA {} consumed after a plain presentation ({} left)", attestation.id, siblings.size - 1)
        return true
    }
}
