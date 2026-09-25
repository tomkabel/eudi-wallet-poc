package ee.cyber.wallet.security

import ee.cyber.wallet.data.database.KeyAttestationEntity
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.domain.provider.wallet.KeyType
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JVM tests for the deleteAllData SecureArea cleanup (review finding 5): the EC keyAttestation
 * rows are the SecureArea alias registry, so per-alias deletion must consume them BEFORE the
 * registry is wiped. Real Android Keystore cannot be exercised on the JVM — the
 * [SecureAreaKeyManager] interaction is PENDING-DEVICE (docs/planning/STEP5-RECORD.md) — so the
 * deletion is a recording fake behind the [SecureAreaKeyDeleter] seam and the DAO is a fake.
 *
 * [ee.cyber.wallet.data.repository.AccountRepository.deleteAllData] wires this class in as its
 * first step; the repository itself needs a database context and stays instrumentation scope.
 */
class SecureAreaKeyCleanupTest {

    private class FakeKeyAttestationDao : KeyAttestationDao {

        val rows = mutableListOf<KeyAttestationEntity>()
        var deleteAllCalled = false

        override suspend fun getById(id: String): KeyAttestationEntity = rows.first { it.id == id }

        override suspend fun insert(entity: KeyAttestationEntity) {
            rows.add(entity)
        }

        override suspend fun deleteById(id: String) {
            rows.removeAll { it.id == id }
        }

        override suspend fun deleteAll() {
            deleteAllCalled = true
            rows.clear()
        }

        override suspend fun getByType(keyType: String): List<KeyAttestationEntity> =
            rows.filter { it.keyType == keyType }
    }

    private class RecordingSecureAreaKeyDeleter : SecureAreaKeyDeleter {

        /** Every deleteKey call, in order. */
        val deletedAliases = mutableListOf<String>()
        var bulkDeleteCalled = false

        override suspend fun deleteKey(keyId: String) {
            deletedAliases.add(keyId)
        }
        override suspend fun keyExists(keyId: String): Boolean = false

        override suspend fun deleteAllKeys() {
            bulkDeleteCalled = true
        }
    }

    private fun entity(id: String, keyType: KeyType) = KeyAttestationEntity(
        id = id,
        attestation = "attestation-$id",
        keyType = keyType.name
    )

    private lateinit var dao: FakeKeyAttestationDao
    private lateinit var deleter: RecordingSecureAreaKeyDeleter
    private lateinit var cleanup: SecureAreaKeyCleanup

    @BeforeTest
    fun setUp() {
        dao = FakeKeyAttestationDao()
        deleter = RecordingSecureAreaKeyDeleter()
        cleanup = SecureAreaKeyCleanup(dao, deleter)
    }

    @Test
    fun `deletes every EC row's key per alias`() = runTest {
        dao.rows.add(entity("ec-1", KeyType.EC))
        dao.rows.add(entity("ec-2", KeyType.EC))
        dao.rows.add(entity("rsa-1", KeyType.RSA))

        cleanup.deleteAll()

        // RSA aliases live in the BKS keystore, not the SecureArea: only the EC rows map here.
        assertEquals(listOf("ec-1", "ec-2"), deleter.deletedAliases)
    }

    @Test
    fun `wipes the registry only after the aliases are consumed`() = runTest {
        dao.rows.add(entity("ec-1", KeyType.EC))
        val events = mutableListOf<String>()

        // Re-run with an order-recording wrapper around the deleter to pin the sequence.
        val ordered = object : SecureAreaKeyDeleter {
            override suspend fun deleteKey(keyId: String) {
                events.add("delete:$keyId")
            }

            override suspend fun keyExists(keyId: String): Boolean = false

            override suspend fun deleteAllKeys() {
                events.add("sweep")
                deleter.bulkDeleteCalled = true
            }
        }
        SecureAreaKeyCleanup(dao, ordered).deleteAll()

        // The wipe of the alias registry must come last; before it, every alias was deleted.
        assertTrue(events.contains("delete:ec-1"))
        assertTrue(events.indexOf("delete:ec-1") < events.indexOf("sweep"))
        assertTrue(dao.deleteAllCalled)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `runs the best-effort sweep even with no recorded keys`() = runTest {
        cleanup.deleteAll()
        assertTrue(deleter.bulkDeleteCalled)
        assertTrue(deleter.deletedAliases.isEmpty())
        assertTrue(dao.deleteAllCalled)
    }

    @Test
    fun `a failed batch rolls back every key and the rows already written`() = runTest {
        // Mid-batch failure: key-0 and key-1 got their rows, key-2 did not.
        dao.rows.add(entity("key-0", KeyType.EC))
        dao.rows.add(entity("key-1", KeyType.EC))
        dao.rows.add(entity("unrelated", KeyType.EC))

        cleanup.deleteKeys(listOf("key-0", "key-1", "key-2"))

        assertEquals(listOf("key-0", "key-1", "key-2"), deleter.deletedAliases)
        assertEquals(listOf("unrelated"), dao.rows.map { it.id })
    }

    @Test
    fun `a batch key that survives deletion keeps its row`() = runTest {
        dao.rows.add(entity("key-0", KeyType.EC))
        dao.rows.add(entity("key-1", KeyType.EC))
        val stuck = object : SecureAreaKeyDeleter {
            override suspend fun deleteKey(keyId: String) = Unit
            override suspend fun keyExists(keyId: String): Boolean = keyId == "key-0"
            override suspend fun deleteAllKeys() = Unit
        }

        SecureAreaKeyCleanup(dao, stuck).deleteKeys(listOf("key-0", "key-1"))

        // key-0 is still live: its row stays so deleteAll can still reach the alias.
        assertEquals(listOf("key-0"), dao.rows.map { it.id })
    }

    @Test
    fun `mixed key types only consume the EC aliases`() = runTest {
        dao.rows.add(entity("rsa-1", KeyType.RSA))
        dao.rows.add(entity("ec-9", KeyType.EC))
        dao.rows.add(entity("rsa-2", KeyType.RSA))

        cleanup.deleteAll()

        assertEquals(listOf("ec-9"), deleter.deletedAliases)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun `deleteAll keeps the record of a key that survived deletion`() = runTest {
        dao.rows.add(entity("ec-gone", KeyType.EC))
        dao.rows.add(entity("ec-stuck", KeyType.EC))
        val stuck = object : SecureAreaKeyDeleter {
            override suspend fun deleteKey(keyId: String) {}
            override suspend fun keyExists(keyId: String): Boolean = keyId == "ec-stuck"
            override suspend fun deleteAllKeys() {}
        }

        val survivors = SecureAreaKeyCleanup(dao, stuck).deleteAll()

        assertEquals(listOf("ec-stuck"), survivors.map { it.id })
        assertEquals(listOf("ec-stuck"), dao.rows.map { it.id })
    }
}
