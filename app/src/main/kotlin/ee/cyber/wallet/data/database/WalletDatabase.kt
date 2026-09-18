package ee.cyber.wallet.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ee.cyber.wallet.data.database.dao.AttestationDao
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.data.database.dao.LogRecordDao

/**
 * Adds the presentation tier to the activity log. Nullable on purpose: rows written before this
 * column existed have no tier to report, and inventing one would misreport history.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE $TABLE_ACTIVITY_LOGS ADD COLUMN ${ActivityLogFields.TIER} TEXT")
    }
}

@Database(
    entities = [
        LogEntryEntity::class,
        KeyAttestationEntity::class,
        AttestationEntity::class
    ],
    version = 2
)
@TypeConverters(
    InstantConverter::class,
    SafeAppErrorConverter::class,
    JsonConverter::class
)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun logRecordDao(): LogRecordDao
    abstract fun keyAttestationDao(): KeyAttestationDao
    abstract fun attestationDao(): AttestationDao
}
