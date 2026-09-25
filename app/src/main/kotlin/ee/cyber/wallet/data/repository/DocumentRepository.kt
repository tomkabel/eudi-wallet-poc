package ee.cyber.wallet.data.repository

import ee.cyber.wallet.data.database.dao.AttestationDao
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.data.database.toEntity
import ee.cyber.wallet.data.database.toModel
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.documents.CredentialToDocumentMapper
import ee.cyber.wallet.domain.provider.wallet.KeyType
import ee.cyber.wallet.security.SecureAreaKeyCleanup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.slf4j.LoggerFactory

class DocumentRepository(
    private val credentialToDocumentMapper: CredentialToDocumentMapper,
    private val attestationDao: AttestationDao,
    private val keyAttestationDao: KeyAttestationDao,
    private val secureAreaKeyCleanup: SecureAreaKeyCleanup,
    private val userPreferencesDataSource: UserPreferencesDataSource
) {

    private val logger = LoggerFactory.getLogger("DocumentRepository")

    val documents: Flow<List<CredentialDocument>> = attestationDao.getAll().map { attestation ->
        attestation.mapNotNull { credentialToDocumentMapper.convert(it.toModel()) }
    }

    /**
     * ARF OIA_08f: the global user setting that gates all disclosure of stored attestations to
     * the Digital Credentials API framework. True by default (OIA_08e's "by default").
     */
    val isDcApiDisclosureEnabled: Flow<Boolean> =
        userPreferencesDataSource.userPreferences.map { it.dcApiDisclosureEnabled }

    fun getDocumentById(id: String): Flow<CredentialDocument?> = attestationDao.getById(id)
        .map { attestation ->
            attestation?.let { credentialToDocumentMapper.convert(it.toModel()) }
        }

    suspend fun addDocument(document: CredentialDocument) {
        attestationDao.insert(document.attestation.toEntity())
    }

    suspend fun deleteDocument(id: String) {
        val keyAttestationId = attestationDao.getById(id).first()?.keyAttestation?.id
        attestationDao.deleteById(id)
        keyAttestationId ?: return
        // An EC row names a SecureArea device key: delete the key too, and the row only once the
        // key is verifiably gone, or the StrongBox/TEE key outlives every record naming it.
        val isSecureAreaKey = runCatching { keyAttestationDao.getById(keyAttestationId).keyType == KeyType.EC.name }
            .getOrDefault(false)
        if (isSecureAreaKey) {
            secureAreaKeyCleanup.deleteKey(keyAttestationId)
        } else {
            keyAttestationDao.deleteById(keyAttestationId)
        }
    }

    suspend fun deleteAll() {
        attestationDao.deleteAll()
    }
}
