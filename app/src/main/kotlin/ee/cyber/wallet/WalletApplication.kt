package ee.cyber.wallet

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.HiltAndroidApp
import ee.cyber.wallet.di.ApplicationScope
import ee.cyber.wallet.domain.credentials.DigitalCredentialsRegistrar
import ee.cyber.wallet.ui.prompt.PromptDialogHost
import kotlinx.coroutines.CoroutineScope
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.multipaz.context.initializeApplication
import org.multipaz.prompt.AndroidPromptModel
import org.multipaz.prompt.PromptModel
import ee.cyber.wallet.security.LOTLInitializer
import java.security.Security
import javax.inject.Inject

@HiltAndroidApp
class WalletApplication : Application() {

    @Inject
    lateinit var lotlInitializer: LOTLInitializer

    @Inject
    lateinit var digitalCredentialsRegistrar: DigitalCredentialsRegistrar

    // JVM-L13/D11: the hand-rolled scope moved to the Hilt graph — this is the
    // same @ApplicationScope the repositories and the PIN/wipe flows use, so
    // application-lifetime work has one supervised home.
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        // Before super.onCreate(): Hilt injects there, and the injected graph already builds the
        // multipaz AndroidKeystoreSecureArea, which reads the multipaz application context.
        setupBouncyCastle()
        initializeApplication(this)
        super.onCreate()
        setupPromptModel()
        initializeLOTL()
        initializeDigitalCredentialsRegistry()
    }

    private fun setupBouncyCastle() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
    }

    /**
     * D14 (mobile F2, stage 2): install the multipaz [PromptModel] globally so presentation-time
     * SecureArea signing can unlock a Stage-1-gated key with the system BiometricPrompt/LSKF
     * dialog.
     *
     * `AndroidKeystoreDefaultKeyUnlockDataProvider.INSTANCE` is only consulted when
     * `AndroidKeystoreSecureArea.sign(..., reason)` hits a locked key; it resolves the prompt
     * model via `AndroidPromptModel.get()` (global fallback included), converts the unlock
     * reason to text and calls `showBiometricPrompt` with the keystore Signature CryptoObject.
     * Without this wiring a locked key fails with `PromptModelNotAvailableException` instead of
     * prompting — the fail-closed direction we keep deliberately: presentation stays blocked
     * until user presence happens.
     *
     * `setGlobal` is multipaz's documented mechanism for callers without a single coroutine
     * scope (`PromptModel.get()` checks the coroutine context first, global second); this app
     * signs from Hilt-provided singletons across several scopes, so the global registration is
     * the correct injection point. The launcher below hops to the resumed FragmentActivity's
     * main thread and shows the biometric prompt; with no activity in the foreground it fails
     * closed (KeyLockedException at the sign site, not a crash).
     */
    private fun setupPromptModel() {
        val promptModel = AndroidPromptModel.Builder(
            uiLauncher = { dialogModel ->
                PromptDialogHost.launch(dialogModel)
            }
        ).addCommonDialogs().build()
        PromptModel.Companion.setGlobal(promptModel)

        // The prompt host is whichever FragmentActivity is resumed — MainActivity, or
        // DigitalCredentialsActivity when Credential Manager launches the wallet into the
        // browser's task. Binding it per activity left the DC API path without a host, so every
        // locked-key signature there failed with PromptUiNotAvailableException.
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is FragmentActivity) PromptDialogHost.activity = activity
            }

            // Stopped, not paused: the biometric overlay itself may pause the host between two
            // signs of one presentation, but a backgrounded or destroyed activity must never be
            // asked to show a platform dialog.
            override fun onActivityStopped(activity: Activity) {
                if (PromptDialogHost.activity === activity) PromptDialogHost.activity = null
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (PromptDialogHost.activity === activity) PromptDialogHost.activity = null
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        })
    }

    private fun initializeLOTL() {
        lotlInitializer.initialize(applicationScope)
    }

    private fun initializeDigitalCredentialsRegistry() {
        digitalCredentialsRegistrar.initialize(applicationScope)
    }
}
