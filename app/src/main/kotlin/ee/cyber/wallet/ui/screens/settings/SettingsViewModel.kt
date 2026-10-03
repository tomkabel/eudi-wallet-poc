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
import ee.cyber.wallet.domain.credentials.DigitalCredentialsRegistrar
import ee.cyber.wallet.di.ApplicationScope
import ee.cyber.wallet.security.CertificateChainValidator
import ee.cyber.wallet.ui.util.LanguageResource
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import javax.inject.Inject

data class UiState(
    val language: LanguageResource,
    val blePeripheralMode: Boolean = true,
    val trustAllValidator: Boolean = false,
    // ARF OIA_08f: the global DC API disclosure switch, shown in Settings.
    val dcApiDisclosureEnabled: Boolean = true,
    val lotlEnabled: Boolean = false,
    val lotlSynced: Boolean = false,
    val lotlCertificateCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val androidLocaleManager: AndroidLocaleManager,
    private val accountRepository: AccountRepository,
    private val userPreferencesDataSource: UserPreferencesDataSource,
    private val digitalCredentialsRegistrar: DigitalCredentialsRegistrar,
    @ApplicationScope private val applicationScope: kotlinx.coroutines.CoroutineScope
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
                    trustAllValidator = prefs.trustAllValidator,
                    dcApiDisclosureEnabled = prefs.dcApiDisclosureEnabled
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

    /**
     * ARF OIA_08f: toggling the global DC API disclosure switch re-runs registration — enabling
     * re-registers the current documents as docTypes, disabling clears the registry so the
     * platform holds nothing.
     */
    fun toggleDcApiDisclosure() {
        viewModelScope.launch {
            val enabled = !_state.value.dcApiDisclosureEnabled
            userPreferencesDataSource.setDcApiDisclosureEnabled(enabled)
            digitalCredentialsRegistrar.registerCredentials()
        }
    }

    /**
     * Wipes all wallet data on the application scope, not GlobalScope: the wipe
     * must outlive this ViewModel (the Activity is destroyed right after) but
     * stay inside the app's supervised, dispatcher-bound scope (JVM-M2).
     * [AccountRepository.deleteAllData] is NonCancellable internally, so the
     * destructive part completes even if the app dies mid-wipe.
     */
    fun deleteAllAndRestart() {
        applicationScope.launch {
            // Every wipe step has already run; this only records which ones failed.
            runCatching { accountRepository.deleteAllData() }
                .onFailure { LoggerFactory.getLogger(SettingsViewModel::class.java).error("Wallet wipe incomplete", it) }
        }
    }
}
