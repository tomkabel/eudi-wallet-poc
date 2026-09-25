package ee.cyber.wallet.ui.mvi

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

interface ViewState

interface ViewEvent

interface ViewSideEffect

abstract class MviViewModel<Event : ViewEvent, UiState : ViewState, Effect : ViewSideEffect> : ViewModel() {

    private val initialState: UiState by lazy { initialState() }
    abstract fun initialState(): UiState

    private val _state: MutableState<UiState> by lazy { mutableStateOf(initialState) }
    val state: State<UiState> by lazy { _state }

    private val _event: MutableSharedFlow<Event> = MutableSharedFlow()

    // D15 (jvm L12): RENDEZVOUS (the default) makes sendEffect() suspend until a collector
    // is actively receiving — for one-shot navigation/UI effects that is a lost-effect trap:
    // a `sendEffect` racing the screen's (re)subscription (rotation, process restore, a
    // collectAsState that lands a frame late) parks the viewModelScope coroutine, and an
    // effect emitted while nobody is subscribed is dropped the moment the channel hits its
    // zero buffer. A small buffer (16) absorbs a burst of effects across one UI frame —
    // trySend would be the alternative but silently discards; we prefer bounded buffering
    // over loss. Effects are consumed promptly by the single screen collector, so the
    // default BUFFERED capacity (64) is far above anything these flows emit in practice.
    private val _effect: Channel<Effect> = Channel(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        subscribeToEvents()
    }

    fun sendEvent(event: Event) {
        viewModelScope.launch { _event.emit(event) }
    }

    protected open fun setState(reducer: UiState.() -> UiState) {
        val newState = state.value.reducer()
        _state.value = newState
    }

    private fun subscribeToEvents() {
        viewModelScope.launch {
            _event.collect {
                handleEvents(it)
            }
        }
    }

    abstract suspend fun handleEvents(event: Event)

    protected fun sendEffect(builder: () -> Effect) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }
}
