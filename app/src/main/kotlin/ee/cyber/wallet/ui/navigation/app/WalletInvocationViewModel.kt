package ee.cyber.wallet.ui.navigation.app

import androidx.lifecycle.ViewModel
import ee.cyber.wallet.domain.credentials.InvocationRefusal
import ee.cyber.wallet.domain.credentials.InvocationRefusalReason
import ee.cyber.wallet.domain.credentials.WalletInvocation
import ee.cyber.wallet.domain.credentials.dispatchWalletInvocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Activity-retained, bounded inbox. AV grant values never enter this state. */
class WalletInvocationViewModel : ViewModel() {
    data class Entry(val id: Long, val invocation: WalletInvocation, val providerUnavailable: Boolean = false)

    private val mutableEntries = MutableStateFlow<List<Entry>>(emptyList())
    val entries = mutableEntries.asStateFlow()
    private var nextId = 0L

    fun receive(raw: String) {
        if (raw.length > 65536) {
            enqueue(WalletInvocation.Refused(InvocationRefusal("invocation", InvocationRefusalReason.MALFORMED)))
            return
        }
        dispatchWalletInvocation(
            raw,
            enterIssuance = { enqueue(WalletInvocation.AvIssuance(it)) },
            navigate = { enqueue(WalletInvocation.ExistingLink(it)) },
            refuse = { enqueue(WalletInvocation.Refused(it)) }
        )
    }

    private fun enqueue(invocation: WalletInvocation) {
        val current = mutableEntries.value
        val overflow = current.lastOrNull()?.invocation as? WalletInvocation.Refused
        if (overflow?.refusal?.reason == InvocationRefusalReason.CAPACITY) {
            // Coalesce only capacity rejections, explicitly reporting their count. Never discard an earlier refusal.
            val count = overflow.refusal.refusedCount
            val updated = overflow.copy(refusal = overflow.refusal.copy(refusedCount = if (count < Int.MAX_VALUE) count + 1 else count))
            mutableEntries.value = current.dropLast(1) + current.last().copy(invocation = updated)
        } else if (current.size >= CAPACITY) {
            mutableEntries.value = current + Entry(
                nextId++,
                WalletInvocation.Refused(InvocationRefusal("invocation_queue", InvocationRefusalReason.CAPACITY))
            )
        } else if (current.size == 1 && current.first().invocation is WalletInvocation.AvIssuance && invocation is WalletInvocation.AvIssuance) {
            // A fresh external invocation may replace a visible valid offer, including an identical offer.
            mutableEntries.value = listOf(Entry(nextId++, invocation))
        } else {
            mutableEntries.value = current + Entry(nextId++, invocation)
        }
    }

    fun providerUnavailable(id: Long) {
        updateHead(id) { it.copy(providerUnavailable = true) }
    }

    fun navigationRefused(id: Long, refusal: InvocationRefusal) {
        updateHead(id) { it.copy(invocation = WalletInvocation.Refused(refusal)) }
    }

    fun dismiss(id: Long) {
        if (mutableEntries.value.firstOrNull()?.id == id) mutableEntries.value = mutableEntries.value.drop(1)
    }

    private fun updateHead(id: Long, update: (Entry) -> Entry) {
        val current = mutableEntries.value
        if (current.firstOrNull()?.id == id) mutableEntries.value = listOf(update(current.first())) + current.drop(1)
    }

    companion object {
        const val CAPACITY = 16
    }
}
