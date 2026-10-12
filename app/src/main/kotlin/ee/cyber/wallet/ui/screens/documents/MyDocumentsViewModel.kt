package ee.cyber.wallet.ui.screens.documents

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.cyber.wallet.data.repository.DocumentRepository
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.provider.ageverification.AgeAttestationRenewal
import ee.cyber.wallet.domain.provider.ageverification.AgeReckoning
import ee.cyber.wallet.ui.mvi.MviViewModel
import ee.cyber.wallet.ui.mvi.ViewEvent
import ee.cyber.wallet.ui.mvi.ViewSideEffect
import ee.cyber.wallet.ui.mvi.ViewState
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Clock

data class UiState(
    val documents: List<CredentialDocument> = listOf(),
    val supportedTypes: List<CredentialType> = listOf()
) : ViewState

sealed class Event : ViewEvent

sealed class Effect : ViewSideEffect

@HiltViewModel
class MyDocumentsViewModel @Inject constructor(
    documentRepository: DocumentRepository
) : MviViewModel<Event, UiState, Effect>() {

    private val documents = documentRepository.documents

    init {
        viewModelScope.launch {
            documents.collect { documents ->
                setState {
                    copy(
                        documents = documents,
                        supportedTypes = getSupportedTypes(documents)
                    )
                }
            }
        }
    }

    private fun getSupportedTypes(documents: List<CredentialDocument>): List<CredentialType> =
        CredentialType.entries.filter { type -> documents.none { it.credentialType() == type } || type.renewalDue(documents) }
            .filter { // filter out MDL if MDOC PID is not issued yet
                when (it) {
                    CredentialType.MDL -> documents.any { doc -> doc.credentialType() == CredentialType.PID_MDOC }
                    CredentialType.PID_SD_JWT, CredentialType.PID_MDOC, CredentialType.AGE_VERIFICATION, CredentialType.EE_POA -> true
                    else -> false
                }
            }

    /**
     * EE-POA-025/029, EE-POA-013: a held age attestation is offered again once its renewal is
     * due; accepting the new batch replaces the old one (IssuanceViewModel).
     */
    private fun CredentialType.renewalDue(documents: List<CredentialDocument>): Boolean =
        this == CredentialType.AGE_VERIFICATION && AgeAttestationRenewal.isDue(
            AgeAttestationRenewal.held(documents),
            AgeAttestationRenewal.birthDate(documents),
            AgeReckoning.today(Clock.System.now())
        )

    override fun initialState(): UiState = UiState()

    override suspend fun handleEvents(event: Event) = TODO()
}
