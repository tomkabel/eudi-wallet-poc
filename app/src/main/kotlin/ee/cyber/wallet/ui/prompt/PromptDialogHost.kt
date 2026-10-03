package ee.cyber.wallet.ui.prompt

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.multipaz.prompt.BiometricPromptDialogModel
import org.multipaz.prompt.PromptDialogModel
import org.multipaz.prompt.PromptUiNotAvailableException
import org.multipaz.prompt.Reason
import org.multipaz.securearea.UserAuthenticationType
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume

/**
 * D14 (mobile F2, stage 2) UI glue for the multipaz [org.multipaz.prompt.PromptModel].
 *
 * The wallet registers a global `AndroidPromptModel` in [ee.cyber.wallet.WalletApplication]
 * whose `uiLauncher` delegates here. When presentation-time SecureArea signing hits a locked
 * Stage-1 key, `AndroidKeystoreDefaultKeyUnlockDataProvider` builds an
 * `AndroidKeystoreKeyUnlockData`, asks the model to launch prompt UI, and retries the sign with
 * the CryptoObject-bound unlock data. This file is that launcher: it finds the resumed
 * [FragmentActivity], shows the platform [BiometricPrompt] bound to the keystore Signature
 * CryptoObject, and reports the outcome.
 *
 * Fail-closed directions (deliberate, never a retry loop):
 *  - no resumed activity -> [PromptUiNotAvailableException] -> `KeyLockedException` at the sign
 *    site — presentation cannot proceed without user presence;
 *  - user cancels / error -> `false` -> `KeyLockedException("User canceled authentication")`.
 *
 * Only the biometric dialog kind is hosted; every other multipaz prompt (NFC scan, passphrase,
 * consent) has no host in this PoC and reports unavailable the same way.
 */
object PromptDialogHost {

    private val logger = LoggerFactory.getLogger("PromptDialogHost")

    /** The resumed activity that can host the platform prompt; set from MainActivity lifecycle. */
    @Volatile
    var activity: FragmentActivity? = null

    /**
     * The [org.multipaz.prompt.AndroidPromptModel.Builder] `uiLauncher`.
     */
    suspend fun launch(dialogModel: PromptDialogModel<*, *>) {
        if (dialogModel !is BiometricPromptDialogModel) {
            logger.warn("no interactive host for this prompt kind: {}", dialogModel::class.simpleName)
            throw PromptUiNotAvailableException()
        }
        val hostActivity = activity ?: throw PromptUiNotAvailableException()
        // multipaz's displayPrompt calls this launcher, re-checks that dialogState has a collector
        // ("bound") as soon as it returns, and only then emits DialogShownState and waits on its
        // result channel. So subscribe before returning (UNDISPATCHED) and stay bound for the
        // activity's lifetime; a destroyed activity unbinds and the next prompt relaunches.
        hostActivity.lifecycleScope.launch(start = CoroutineStart.UNDISPATCHED) {
            dialogModel.dialogState.collect { state ->
                if (state !is PromptDialogModel.DialogShownState) return@collect
                val parameters = state.parameters
                val authenticated = showForUnlock(
                    cryptoObject = parameters.cryptoObject,
                    title = parameters.reason.toPromptText().first,
                    subtitle = parameters.reason.toPromptText().second,
                    userAuthenticationTypes = parameters.userAuthenticationTypes
                )
                state.resultChannel.send(authenticated)
            }
        }
    }

    /**
     * Shows the platform biometric/device-credential prompt for a SecureArea key unlock.
     *
     * @return true when the user authenticated. false on cancel/dismiss — the unlock path turns
     *     that into `KeyLockedException` and the presentation aborts.
     * @throws PromptUiNotAvailableException when no activity is in the foreground.
     */
    suspend fun showForUnlock(
        cryptoObject: BiometricPrompt.CryptoObject?,
        title: String,
        subtitle: String,
        userAuthenticationTypes: Set<UserAuthenticationType>
    ): Boolean {
        val hostActivity = activity.also {
            if (it == null) logger.warn("no resumed activity to host the biometric prompt")
        } ?: throw PromptUiNotAvailableException()
        val authenticators = buildSet {
            if (UserAuthenticationType.BIOMETRIC in userAuthenticationTypes) {
                add(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            }
            if (UserAuthenticationType.LSKF in userAuthenticationTypes) {
                add(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            }
        }
        require(authenticators.isNotEmpty()) { "no usable authentication type for the prompt" }
        val deviceCredentialAllowed = BiometricManager.Authenticators.DEVICE_CREDENTIAL in authenticators

        return withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { continuation ->
                val executor = ContextCompat.getMainExecutor(hostActivity)
                val callback = object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(true)
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        // Negative button, user cancel, lockout, no biometrics enrolled: a
                        // refusal is reported as false — the caller fails closed, it does not
                        // retry into another prompt.
                        if (continuation.isActive) continuation.resume(false)
                    }
                }
                val prompt = BiometricPrompt(hostActivity, executor, callback)
                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setAllowedAuthenticators(authenticators.reduce { acc, flag -> acc or flag })
                    .apply {
                        // setNegativeButtonText is required unless DEVICE_CREDENTIAL is allowed
                        // (then the system renders its own cancel affordance).
                        if (!deviceCredentialAllowed) {
                            setNegativeButtonText(hostActivity.getString(android.R.string.cancel))
                        }
                    }
                    .build()
                continuation.invokeOnCancellation {
                    runCatching { prompt.cancelAuthentication() }
                }
                try {
                    if (cryptoObject != null) prompt.authenticate(promptInfo, cryptoObject)
                    else prompt.authenticate(promptInfo)
                } catch (e: Exception) {
                    // CryptoObject binding races (key invalidated, enrolment changed): fail
                    // closed rather than crash the presentation flow.
                    if (continuation.isActive) continuation.resume(false)
                }
            }
        }
    }

    /**
     * Reason text for the prompt. multipaz's own `defaultConvertToHumanReadable` resolves the
     * localized `key_unlock_present_*` strings for `PresentmentUnlockReason`; for a plain
     * `Reason.HumanReadable` the title/subtitle are used as-is.
     */
    private fun Reason.toPromptText(): Pair<String, String> = when (this) {
        is Reason.HumanReadable -> title to (subtitle ?: "")
        else -> "Unlock your document" to "Authenticate to share this document"
    }
}
