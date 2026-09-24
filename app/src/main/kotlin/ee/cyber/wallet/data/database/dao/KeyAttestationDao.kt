package ee.cyber.wallet.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ee.cyber.wallet.data.database.KeyAttestationEntity
import ee.cyber.wallet.data.database.KeyAttestationFields
import ee.cyber.wallet.data.database.TABLE_KEY_ATTESTATIONS

@Dao
interface KeyAttestationDao {

    @Query("SELECT * FROM $TABLE_KEY_ATTESTATIONS WHERE ${KeyAttestationFields.ID} = :id")
    suspend fun getById(id: String): KeyAttestationEntity

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: KeyAttestationEntity)

    @Query("DELETE FROM $TABLE_KEY_ATTESTATIONS WHERE ${KeyAttestationFields.ID} = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM $TABLE_KEY_ATTESTATIONS")
    suspend fun deleteAll()

    /**
     * Every attestation recorded for a key type. The EC rows double as the SecureArea alias
     * registry (LocalCryptoProvider.generateSecureAreaKey inserts one per generated key), so
     * SecureArea key cleanup iterates them before [deleteAll] wipes the table.
     */
    @Query("SELECT * FROM $TABLE_KEY_ATTESTATIONS WHERE ${KeyAttestationFields.KEY_TYPE} = :keyType")
    suspend fun getByType(keyType: String): List<KeyAttestationEntity>
}
