package ee.cyber.wallet.ui.model

enum class DarkThemeConfig {
    FOLLOW_SYSTEM, LIGHT, DARK
}

enum class IssuerKeyType {
    IACA_TRUSTED, UNTRUSTED
}

data class UserPreferences(
    val darkThemeConfig: DarkThemeConfig = DarkThemeConfig.FOLLOW_SYSTEM,
    val blePeripheralMode: Boolean = true,
    val issuerKeyType: IssuerKeyType = IssuerKeyType.IACA_TRUSTED,
    val trustAllValidator: Boolean = false
)
