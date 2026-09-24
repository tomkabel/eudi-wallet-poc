package ee.cyber.wallet.domain.presentation

import ee.cyber.wallet.data.database.AttestationAttestationKey
import ee.cyber.wallet.data.database.AttestationEntity
import ee.cyber.wallet.data.database.KeyAttestationEntity
import ee.cyber.wallet.data.database.dao.AttestationDao
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import ee.cyber.wallet.domain.provider.wallet.KeyType
import ee.cyber.wallet.security.SecureAreaKeyDeleter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The EE-POA-013 / WIAM_21 / EE-ZKP-025 acceptance tests (conformance plan §4 item 6): a plain
 * presentation of ee.riik.poa.1 consumes the attestation and its SecureArea key, never the last
 * of the batch; a ZK presentation consumes nothing. The SecureArea deletion is a seam here —
 * on hardware [ee.cyber.wallet.security.SecureAreaKeyManager] implements it (PENDING-DEVICE).
 */
class EePoaConsumptionTest {

    private class FakeAttestationDao : AttestationDao {
        val rows = MutableStateFlow<List<AttestationAttestationKey>>(emptyList())

        override fun getAll(): Flow<List<AttestationAttestationKey>> = rows

        override fun getById(id: String): Flow<AttestationAttestationKey?> =
            rows.map { list -> list.firstOrNull { it.attestation.id == id } }

        override suspend fun insert(entity: AttestationEntity) {
            rows.value = rows.value + AttestationAttestationKey(entity, keyEntity(entity.keyAttestationId))
        }

        override suspend fun deleteById(id: String) {
            rows.value = rows.value.filterNot { it.attestation.id == id }
        }

        override suspend fun deleteAll() {
            rows.value = emptyList()
        }

        fun add(id: String, keyId: String, type: CredentialType) {
            rows.value = rows.value + AttestationAttestationKey(
                AttestationEntity(id = id, credential = "{}", type = type, keyAttestationId = keyId),
                keyEntity(keyId)
            )
        }

        private val keyEntities = mutableMapOf<String, KeyAttestationEntity>()
        private fun keyEntity(id: String) = keyEntities.getOrPut(id) {
            KeyAttestationEntity(id = id, attestation = "{}", keyType = KeyType.EC.name)
        }
    }

    private class FakeKeyAttestationDao : KeyAttestationDao {
        val deleted = mutableListOf<String>()
        private val rows = MutableStateFlow<Map<String, KeyAttestationEntity>>(emptyMap())

        override suspend fun getById(id: String): KeyAttestationEntity = requireNotNull(rows.value[id]) { "no key attestation $id" }

        override suspend fun insert(entity: KeyAttestationEntity) {
            rows.value = rows.value + (entity.id to entity)
        }

        override suspend fun deleteById(id: String) {
            deleted.add(id)
            rows.value = rows.value - id
        }

        override suspend fun deleteAll() {
            rows.value = emptyMap()
        }

        override suspend fun getByType(keyType: String): List<KeyAttestationEntity> =
            rows.value.values.filter { it.keyType == keyType }
    }

    private class RecordingDeleter : SecureAreaKeyDeleter {
        val deletedKeys = mutableListOf<String>()
        var deleteAllCalled = false

        override suspend fun deleteKey(keyId: String) {
            deletedKeys.add(keyId)
        }
        override suspend fun keyExists(keyId: String): Boolean = keyId !in deletedKeys

        override suspend fun deleteAllKeys() {
            deleteAllCalled = true
        }
    }

    /** The same JWK shape EePoaIssuanceTest's fake uses — parseable, holder-irrelevant. */
    private fun keyAttestation(keyId: String) = KeyAttestation(
        keyId = keyId,
        attestation = """{"jwk":{"kty":"EC","crv":"P-256","x":"$keyId","y":"$keyId"}}""",
        keyType = KeyType.EC
    )

    private fun attestation(id: String, type: CredentialType = CredentialType.EE_POA) = Attestation(
        id = id,
        credential = "{}",
        type = type,
        keyAttestation = keyAttestation("key-$id")
    )

    private class Harness(val attestationDao: FakeAttestationDao, val deleter: RecordingDeleter) {
        val consumption = EePoaConsumption(attestationDao, FakeKeyAttestationDao(), deleter)

        fun seedBatch(size: Int) {
            repeat(size) { attestationDao.add("poa-$it", "key-poa-$it", CredentialType.EE_POA) }
        }
    }

    private fun harness(size: Int) = Harness(FakeAttestationDao(), RecordingDeleter()).also { it.seedBatch(size) }

    @Test
    fun `a plain presentation consumes the attestation and its key`() = runTest {
        val h = harness(size = 3)
        val presented = attestation("poa-0")

        val consumed = h.consumption.consumeAfterPresentation(presented, PresentationTier.PLAIN_NOT_REQUESTED)

        assertTrue(consumed)
        assertEquals(listOf(presented.keyAttestation.keyId), h.deleter.deletedKeys)
        assertFalse(h.attestationDao.rows.value.any { it.attestation.id == "poa-0" })
        assertEquals(2, h.attestationDao.rows.value.size)
    }

    @Test
    fun `never the last - the final EE-PoA in the batch is not consumed`() = runTest {
        val h = harness(size = 1)
        val last = attestation("poa-0")

        val consumed = h.consumption.consumeAfterPresentation(last, PresentationTier.PLAIN_NOT_REQUESTED)

        assertFalse(consumed, "EE-POA-013: the last remaining attestation must not be consumed")
        assertTrue(h.deleter.deletedKeys.isEmpty())
        assertEquals(1, h.attestationDao.rows.value.size)
    }

    @Test
    fun `a zero-knowledge presentation consumes nothing`() = runTest {
        val h = harness(size = 3)
        val presented = attestation("poa-1")

        val consumed = h.consumption.consumeAfterPresentation(presented, PresentationTier.ZERO_KNOWLEDGE)

        assertFalse(consumed, "EE-ZKP-025: a ZK presentation leaves the batch intact")
        assertTrue(h.deleter.deletedKeys.isEmpty())
        assertEquals(3, h.attestationDao.rows.value.size)
    }

    @Test
    fun `a non EE-PoA attestation is out of scope`() = runTest {
        val h = harness(size = 3)
        h.attestationDao.add("av-0", "key-av-0", CredentialType.AGE_VERIFICATION)
        val presented = attestation("av-0", CredentialType.AGE_VERIFICATION)

        val consumed = h.consumption.consumeAfterPresentation(presented, PresentationTier.PLAIN_NOT_REQUESTED)

        assertFalse(consumed)
        assertTrue(h.deleter.deletedKeys.isEmpty())
    }
}
