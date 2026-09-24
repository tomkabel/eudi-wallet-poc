package ee.cyber.wallet.ui.screens.dcapi

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.credentials.DigitalCredential
import androidx.credentials.ExperimentalDigitalCredentialApi
import androidx.credentials.GetCredentialResponse
import androidx.credentials.provider.PendingIntentHandler
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import ee.cyber.wallet.ui.theme.WalletTheme
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory

@AndroidEntryPoint
class DigitalCredentialsActivity : AppCompatActivity() {
    private val log = LoggerFactory.getLogger(DigitalCredentialsActivity::class.java)

    @OptIn(ExperimentalDigitalCredentialApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WalletTheme(useDarkTheme = isSystemInDarkTheme()) {
                Surface(Modifier.safeDrawingPadding()) {
                    val viewModel: DigitalCredentialsViewModel = hiltViewModel()

                    LaunchedEffect(Unit) {
                        viewModel.processRequest(intent)
                    }

                    LaunchedEffect(Unit) {
                        viewModel.effect.onEach { effect ->
                            when (effect) {
                                is DcEffect.SendResponse -> {
                                    log.info("Sending response")
                                    val resultData = Intent()
                                    PendingIntentHandler.setGetCredentialResponse(
                                        resultData,
                                        GetCredentialResponse(
                                            DigitalCredential(effect.response)
                                        )
                                    )
                                    setResult(RESULT_OK, resultData)
                                    finish()
                                }

                                DcEffect.Cancel -> {
                                    log.info("User cancelled")
                                    setResult(RESULT_CANCELED)
                                    finish()
                                }

                                DcEffect.NoMatch -> {
                                    log.info("No matching credentials")
                                    // UI will show no match state
                                }

                                DcEffect.RefusedPlainFallback -> {
                                    log.info("EE-ZKP-051: plain fallback refused, UI shows the refusal notice")
                                    // UI will show the refusal state
                                }

                                is DcEffect.Error -> {
                                    log.error("Error: ${effect.message}")
                                    setResult(RESULT_CANCELED)
                                    finish()
                                }
                            }
                        }.collect()
                    }

                    DigitalCredentialsScreen(
                        state = viewModel.state.value,
                        onEvent = { viewModel.sendEvent(it) }
                    )
                }
            }
        }
    }
}
