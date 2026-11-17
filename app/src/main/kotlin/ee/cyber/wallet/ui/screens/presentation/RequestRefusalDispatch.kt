package ee.cyber.wallet.ui.screens.presentation

import ee.cyber.wallet.domain.AppError
import ee.cyber.wallet.domain.presentation.ProtocolRefusal
import ee.cyber.wallet.ui.screens.dcapi.DcEffect
import ee.cyber.wallet.ui.screens.dcapi.DcUiState
import ee.cyber.wallet.ui.screens.proximity.Effect
import ee.cyber.wallet.ui.screens.proximity.UiState

/** Actual retained-state and wire/UI effect mapping, with Android transport operations injected. */
internal object RequestRefusalDispatch {
    fun dcProtocol(
        current: DcUiState,
        reason: ProtocolRefusal,
        store: (DcUiState) -> Unit,
        effect: (DcEffect) -> Unit
    ) {
        store(current.copy(isLoading = false, protocolRefusal = reason))
        effect(DcEffect.Refused(reason))
    }

    fun dcAge(
        current: DcUiState,
        reason: AppError,
        store: (DcUiState) -> Unit,
        effect: (DcEffect) -> Unit
    ) {
        store(current.copy(isLoading = false, plainRefusal = reason))
        effect(DcEffect.RefusedPlainFallback)
    }

    fun proximityAge(
        current: UiState,
        reason: AppError,
        store: (UiState) -> Unit,
        stop: (Boolean) -> Unit,
        effect: (Effect) -> Unit
    ) {
        stop(true)
        store(current.copy(refusal = reason, credentials = emptyList(), sessionTranscript = null))
        effect(Effect.ProximityRequestNoMatch)
    }
}
