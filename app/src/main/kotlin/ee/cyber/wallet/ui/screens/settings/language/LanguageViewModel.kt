package ee.cyber.wallet.ui.screens.settings.language

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.domain.AndroidLocaleManager
import ee.cyber.wallet.ui.model.IssuerKeyType
import ee.cyber.wallet.ui.util.LanguageResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import javax.inject.Inject

data class LanguageItem(
    val languageResource: LanguageResource,
    val selected: Boolean
)

data class UiState(
    val languages: List<LanguageItem>,
    val isIacaTrusted: Boolean = true
)

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val androidLocaleManager: AndroidLocaleManager,
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {
    private val logger = LoggerFactory.getLogger("LanguageViewModel")

    private val defaultState by lazy {
        UiState(languageItems())
    }

    private fun languageItems(): List<LanguageItem> {
        val currentLocale = androidLocaleManager.getApplicationLocale()
        return androidLocaleManager.supportedLocales.map {
            LanguageItem(LanguageResource.fromLocale(it), it.language == currentLocale.language)
        }.distinct()
    }

    private val _state: MutableState<UiState> by lazy { mutableStateOf(defaultState) }
    val state: State<UiState> by lazy { _state }

    fun updateLocales() {
        _state.value = _state.value.copy(languages = languageItems())
    }

    fun updateIssuerKeyType() {
        viewModelScope.launch {
            val currentKeyType = userPreferencesDataSource.userPreferences.first().issuerKeyType
            _state.value = _state.value.copy(isIacaTrusted = currentKeyType == IssuerKeyType.IACA_TRUSTED)
        }
    }

    fun changeAppLanguage(language: LanguageItem) {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                androidLocaleManager.setApplicationLocale(language.languageResource.tag)
            }
            updateLocales()
        }
    }

    fun toggleIssuerKeyType() {
        viewModelScope.launch {
            val currentKeyType = userPreferencesDataSource.userPreferences.first().issuerKeyType
            val newKeyType = if (currentKeyType == IssuerKeyType.IACA_TRUSTED) {
                IssuerKeyType.UNTRUSTED
            } else {
                IssuerKeyType.IACA_TRUSTED
            }
            userPreferencesDataSource.setIssuerKeyType(newKeyType)
            updateIssuerKeyType()
        }
    }
}
