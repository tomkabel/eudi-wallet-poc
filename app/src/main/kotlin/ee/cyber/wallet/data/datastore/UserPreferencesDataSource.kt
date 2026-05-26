package ee.cyber.wallet.data.datastore

import androidx.datastore.core.DataStore
import ee.cyber.wallet.BlePeripheralModeProto
import ee.cyber.wallet.DarkThemeConfigProto
import ee.cyber.wallet.IssuerKeyTypeProto
import ee.cyber.wallet.UserPreferencesProto
import ee.cyber.wallet.copy
import ee.cyber.wallet.ui.model.DarkThemeConfig
import ee.cyber.wallet.ui.model.IssuerKeyType
import ee.cyber.wallet.ui.model.UserPreferences
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class UserPreferencesDataSource(
    private val dataStore: DataStore<UserPreferencesProto>
) {

    val userPreferences = dataStore.data.map { it.toModel() }.distinctUntilChanged()

    suspend fun setDarkThemeConfig(darkThemeConfig: DarkThemeConfig) {
        runCatching {
            dataStore.updateData {
                it.copy {
                    this.darkThemeConfig = when (darkThemeConfig) {
                        DarkThemeConfig.DARK -> DarkThemeConfigProto.DARK_THEME_CONFIG_DARK
                        DarkThemeConfig.FOLLOW_SYSTEM -> DarkThemeConfigProto.DARK_THEME_CONFIG_FOLLOW_SYSTEM
                        DarkThemeConfig.LIGHT -> DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT
                    }
                }
            }
        }
    }

    suspend fun setBlePeripheralMode(isPeripheralMode: Boolean) {
        runCatching {
            dataStore.updateData {
                it.copy {
                    this.bleMode = if (isPeripheralMode) {
                        BlePeripheralModeProto.BLE_PERIPHERAL_MODE_PERIPHERAL
                    } else {
                        BlePeripheralModeProto.BLE_PERIPHERAL_MODE_CENTRAL
                    }
                }
            }
        }
    }

    suspend fun setIssuerKeyType(issuerKeyType: IssuerKeyType) {
        runCatching {
            dataStore.updateData {
                it.copy {
                    this.issuerKeyType = when (issuerKeyType) {
                        IssuerKeyType.IACA_TRUSTED -> IssuerKeyTypeProto.ISSUER_KEY_TYPE_IACA_TRUSTED
                        IssuerKeyType.UNTRUSTED -> IssuerKeyTypeProto.ISSUER_KEY_TYPE_UNTRUSTED
                    }
                }
            }
        }
    }

    suspend fun setTrustAllValidator(trustAll: Boolean) {
        runCatching {
            dataStore.updateData {
                it.copy {
                    this.trustAllValidator = trustAll
                }
            }
        }
    }

    private fun UserPreferencesProto.toModel(): UserPreferences {
        return UserPreferences(
            darkThemeConfig = when (darkThemeConfig) {
                DarkThemeConfigProto.DARK_THEME_CONFIG_DARK -> DarkThemeConfig.DARK
                DarkThemeConfigProto.DARK_THEME_CONFIG_LIGHT -> DarkThemeConfig.LIGHT
                else -> DarkThemeConfig.FOLLOW_SYSTEM
            },
            blePeripheralMode = when (bleMode) {
                BlePeripheralModeProto.BLE_PERIPHERAL_MODE_CENTRAL -> false
                else -> true // UNSPECIFIED and PERIPHERAL both default to peripheral mode (true)
            },
            issuerKeyType = when (issuerKeyType) {
                IssuerKeyTypeProto.ISSUER_KEY_TYPE_UNTRUSTED -> IssuerKeyType.UNTRUSTED
                else -> IssuerKeyType.IACA_TRUSTED
            },
            trustAllValidator = trustAllValidator
        )
    }

    suspend fun clearAll() = runCatching {
        dataStore.updateData { UserPreferencesProto.getDefaultInstance() }
    }
}
