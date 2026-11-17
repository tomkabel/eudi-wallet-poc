package ee.cyber.wallet.data.repository

import ee.cyber.wallet.data.database.LogEntryEntity
import ee.cyber.wallet.data.database.dao.LogRecordDao
import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.presentation.PresentationTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.serialization.json.JsonObject

class TransactionLogRepository(
    private val logRecordDao: LogRecordDao
) {

    val transactionLogs = logRecordDao.getAll()

    /** The id arrives from navigation; a malformed or unknown one yields nothing, not a crash. */
    fun getTransactionLog(id: String): Flow<LogEntryEntity> =
        id.toLongOrNull()?.let { logRecordDao.getById(it).filterNotNull() } ?: emptyFlow()

    suspend fun addTransactionLog(
        party: String,
        docType: DocType,
        attributes: JsonObject? = null,
        error: AppError? = null,
        tier: PresentationTier? = null
    ) =
        logRecordDao.insert(
            LogEntryEntity(
                date = kotlin.time.Clock.System.now(),
                party = party,
                docType = docType,
                attributes = attributes,
                error = error,
                tier = tier
            )
        )
}
