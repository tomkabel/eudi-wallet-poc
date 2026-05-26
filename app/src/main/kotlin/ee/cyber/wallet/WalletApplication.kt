package ee.cyber.wallet

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import ee.cyber.wallet.domain.credentials.DigitalCredentialsRegistrar
import ee.cyber.wallet.security.LOTLInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.inject.Inject

@HiltAndroidApp
class WalletApplication : Application() {

    @Inject
    lateinit var lotlInitializer: LOTLInitializer

    @Inject
    lateinit var digitalCredentialsRegistrar: DigitalCredentialsRegistrar

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
