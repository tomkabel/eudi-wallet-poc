package ee.cyber.wallet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ee.cyber.wallet.di.ApplicationScope
import ee.cyber.wallet.domain.credentials.DigitalCredentialsRegistrar
import ee.cyber.wallet.security.LOTLInitializer
import kotlinx.coroutines.CoroutineScope
import org.bouncycastle.jce.provider.BouncyCastleProvider
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
        super.onCreate()
        setupBouncyCastle()
        initializeLOTL()
        initializeDigitalCredentialsRegistry()
    }

    private fun setupBouncyCastle() {
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.addProvider(BouncyCastleProvider())
    }

    private fun initializeLOTL() {
        lotlInitializer.initialize(applicationScope)
    }

    private fun initializeDigitalCredentialsRegistry() {
        digitalCredentialsRegistrar.initialize(applicationScope)
    }
}
