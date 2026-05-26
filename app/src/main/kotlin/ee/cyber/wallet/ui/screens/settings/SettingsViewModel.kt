package ee.cyber.wallet.ui.screens.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.cyber.wallet.AppConfig
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.data.repository.AccountRepository
import ee.cyber.wallet.domain.AndroidLocaleManager
import ee.cyber.wallet.security.CertificateChainValidator
import ee.cyber.wallet.ui.util.LanguageResource
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UiState(
    val language: LanguageResource,
    val blePeripheralMode: Boolean = true,
    val trustAllValidator: Boolean = false,
    val lotlEnabled: Boolean = false,
    val lotlSynced: Boolean = false,
    val lotlCertificateCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val androidLocaleManager: AndroidLocaleManager,
    private val accountRepository: AccountRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {

    private val _state = mutableStateOf(
        UiState(
            language = LanguageResource.fromLocale(androidLocaleManager.getApplicationLocale()),
            lotlEnabled = AppConfig.lotlEnabled,
            lotlSynced = CertificateChainValidator.isLOTLSynced(),
            lotlCertificateCount = CertificateChainValidator.getLOTLCertificateCount()
        )
    )
    val state: State<UiState> by lazy { _state }

    init {
        userPreferencesDataSource.userPreferences
            .onEach { prefs ->
                _state.value = _state.value.copy(
                    blePeripheralMode = prefs.blePeripheralMode,
                    trustAllValidator = prefs.trustAllValidator
                )
                CertificateChainValidator.setTrustAll(prefs.trustAllValidator)
            }
            .launchIn(viewModelScope)

        CertificateChainValidator.lotlStatus
            .onEach { lotlStatus ->
                _state.value = _state.value.copy(
                    lotlSynced = lotlStatus.isSynced,
                    lotlCertificateCount = lotlStatus.certificateCount
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateLocales() {
        val locale = androidLocaleManager.getApplicationLocale()
        _state.value = _state.value.copy(language = LanguageResource.fromLocale(locale))
    }

    fun toggleBleMode() {
        viewModelScope.launch {
            userPreferencesDataSource.setBlePeripheralMode(!_state.value.blePeripheralMode)
        }
    }

    fun toggleTrustAllValidator() {
        viewModelScope.launch {
            userPreferencesDataSource.setTrustAllValidator(!_state.value.trustAllValidator)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun deleteAllAndRestart() {
        GlobalScope.launch {
            accountRepository.deleteAllData()
        }
    }
}
