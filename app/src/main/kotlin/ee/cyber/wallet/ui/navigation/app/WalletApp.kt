package ee.cyber.wallet.ui.navigation.app

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ee.cyber.wallet.BuildConfig
import ee.cyber.wallet.domain.credentials.InvocationRefusal
import ee.cyber.wallet.domain.credentials.InvocationRefusalReason
import ee.cyber.wallet.domain.credentials.WalletInvocation
import ee.cyber.wallet.ui.screens.issuance.AvVciInvocationDialog
import ee.cyber.wallet.domain.documents.CredentialDocument
import ee.cyber.wallet.domain.documents.fullName
import ee.cyber.wallet.domain.provider.wallet.WalletInstanceCredentials
import ee.cyber.wallet.ui.AppRoute
import ee.cyber.wallet.ui.model.UserData
import ee.cyber.wallet.ui.navigation.boarding.boardingNavGraph
import ee.cyber.wallet.ui.navigation.main.mainNavGraph
import org.slf4j.LoggerFactory

data class WalletAppState(
    val walletCredentials: WalletInstanceCredentials?,
    val userData: UserData?,
    val documents: List<CredentialDocument>?,
    val navController: NavHostController
) {
    val isRegistered get() = walletCredentials?.isRegistered() == true && isPinCreated
    val isPinCreated get() = userData?.isPinCreated == true
    val isActivated get() = isPinCreated && documents?.isNotEmpty() == true
    val isLoaded get() = documents != null && userData != null

    val fullName: String
        get() = documents?.firstOrNull()?.fullName() ?: ""
}

@Composable
fun rememberAppState(
    walletCredentials: WalletInstanceCredentials? = null,
    user: UserData? = null,
    documents: List<CredentialDocument>? = null,
    navController: NavHostController = rememberNavController()
): WalletAppState = remember(user, documents, navController) {
    WalletAppState(
        walletCredentials = walletCredentials,
        userData = user,
        documents = documents,
        navController = navController
    )
}

private val logger = LoggerFactory.getLogger("WalletApp")

@Composable
fun WalletApp(viewModel: WalletAppViewModel, invocations: WalletInvocationViewModel) {
    val entries by invocations.entries.collectAsStateWithLifecycle()
    val active = entries.firstOrNull()
    val walletCredentials by viewModel.walletCredentials.collectAsStateWithLifecycle()
    val userData by viewModel.userData.collectAsStateWithLifecycle()
    val documents by viewModel.documents.collectAsStateWithLifecycle()
    val appState = rememberAppState(
        walletCredentials = walletCredentials,
        user = userData,
        documents = documents
    )
    DisposableEffect(Unit) {
        val navController = appState.navController
        val listener = NavController.OnDestinationChangedListener { _, destination, arguments ->
            if (BuildConfig.DEBUG) logger.debug("onDestinationChanged: {}, {}", destination, arguments)
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    if (appState.isLoaded) {
        LaunchedEffect(active?.id, active?.invocation) {
            val current = active ?: return@LaunchedEffect
            val link = current.invocation as? WalletInvocation.ExistingLink
            if (link != null) {
                try {
                    appState.navController.navigate(link.uri.toUri())
                    invocations.dismiss(current.id)
                } catch (_: Exception) {
                    invocations.navigationRefused(current.id, InvocationRefusal("invocation", InvocationRefusalReason.NAVIGATION))
                }
            }
        }
        NavHost(
            navController = appState.navController,
            startDestination = if (appState.isActivated) AppRoute.Main.route else AppRoute.Boarding.route,
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() }
        ) {
            boardingNavGraph(appState)
            mainNavGraph(appState)
        }
        if (active != null) key(active.id) {
            AvVciInvocationDialog(
                offer = (active.invocation as? WalletInvocation.AvIssuance)?.offer,
                refusal = (active.invocation as? WalletInvocation.Refused)?.refusal,
                providerUnavailable = active.providerUnavailable,
                onContinue = { invocations.providerUnavailable(active.id) },
                onDismiss = { invocations.dismiss(active.id) }
            )
        }
    }
}
