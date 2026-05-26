package ee.cyber.wallet.ui.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import ee.cyber.wallet.AppConfig
import ee.cyber.wallet.R
import ee.cyber.wallet.ui.components.AppContent
import ee.cyber.wallet.ui.components.AppHeader
import ee.cyber.wallet.ui.components.FundingLogo
import ee.cyber.wallet.ui.components.LanguageItemCard
import ee.cyber.wallet.ui.components.PrimaryButton
import ee.cyber.wallet.ui.components.VSpace
import ee.cyber.wallet.ui.components.WSpace
import ee.cyber.wallet.ui.screens.settings.language.LanguageItem
import ee.cyber.wallet.ui.screens.settings.language.LanguageViewModel
import ee.cyber.wallet.ui.screens.settings.language.UiState
import ee.cyber.wallet.ui.theme.PreviewThemes
import ee.cyber.wallet.ui.theme.PreviewThemesSmallScreen
import ee.cyber.wallet.ui.theme.WalletThemePreviewSurface
import ee.cyber.wallet.ui.util.ContentAlpha
import ee.cyber.wallet.ui.util.LanguageResource

@Composable
@PreviewThemes
@PreviewThemesSmallScreen
private fun WelcomeScreenPreview() {
    WalletThemePreviewSurface {
        WelcomeScreenContent(
            UiState(
                listOf(
                    LanguageItem(LanguageResource.EN, true),
                    LanguageItem(LanguageResource.ET, false)
                )
            )
        )
    }
}

@Composable
fun WelcomeScreen(viewModel: LanguageViewModel, onContinueClicked: () -> Unit = {}) {
    val state by viewModel.state
    val locale = Locale.current
    LaunchedEffect(locale) {
        viewModel.updateLocales()
        if (AppConfig.useMocks) {
            viewModel.updateIssuerKeyType()
        }
    }

    WelcomeScreenContent(
        state = state,
        showIssuerKeySelection = AppConfig.useMocks,
        onContinueClicked = onContinueClicked,
        onLanguageSelected = { viewModel.changeAppLanguage(it) },
        onIssuerKeyToggle = { viewModel.toggleIssuerKeyType() }
    )
}

@Composable
private fun WelcomeScreenContent(
    state: UiState,
    showIssuerKeySelection: Boolean = false,
    onLanguageSelected: (LanguageItem) -> Unit = {},
    onIssuerKeyToggle: () -> Unit = {},
    onContinueClicked: () -> Unit = {}
) {
    println("WelcomeScreenContent: ${ConfigurationCompat.getLocales(LocalConfiguration.current)[0]}")
    val placeholderLogo = painterResource(R.drawable.placeholder_logo)
    AppContent(
        // workaround for Android 10, where FundingLogo is not re-composed on locale change
        header = { AppHeader(placeholderLogo) }
    ) {
        // workaround for Android 10, where FundingLogo is not re-composed on locale change
        val painter = painterResource(R.drawable.funding_logo)
        FundingLogo(painter)
        VSpace(16)
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        VSpace(24.dp)
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        VSpace(24.dp)
        // workaround for Android 10, where content of ContentAlpha is not re-composed on locale change
        val text = stringResource(R.string.welcome_language_title).uppercase()
        ContentAlpha(0.6f) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp),
                text = text,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodySmall
            )
        }
        VSpace(12.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.languages.forEach {
                LanguageItemCard(
                    title = stringResource(it.languageResource.resId),
                    subTitle = stringResource(it.languageResource.localizedResId),
                    selected = it.selected
                ) {
                    if (!it.selected) onLanguageSelected(it)
                }
            }
        }

        if (showIssuerKeySelection) {
            VSpace(24.dp)
            IssuerKeySettingItem(isIacaTrusted = state.isIacaTrusted, onToggle = onIssuerKeyToggle)
        }

        WSpace()
        PrimaryButton(text = stringResource(R.string.welcome_create_pin), onClick = onContinueClicked)
    }
}

@Composable
private fun IssuerKeySettingItem(isIacaTrusted: Boolean, onToggle: () -> Unit = {}) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(48.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Issuer Signing Key",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isIacaTrusted) "IACA trusted" else "Untrusted",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = isIacaTrusted,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
