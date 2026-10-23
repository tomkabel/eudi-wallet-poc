package ee.cyber.wallet.ui.navigation.app

import ee.cyber.wallet.domain.credentials.InvocationRefusal
import ee.cyber.wallet.domain.credentials.InvocationRefusalReason
import ee.cyber.wallet.domain.credentials.WalletInvocation
import java.net.URLEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WalletInvocationViewModelTest {
    private fun valid(issuer: String = "https://issuer.example") = "av-vci://?credential_offer=" + URLEncoder.encode(
        """{"credential_issuer":"$issuer","credential_configuration_ids":["proof_of_age"],"grants":{"secret-code":"do-not-retain"}}""",
        "UTF-8"
    )

    @Test
    fun `retained typed outcomes preserve provider state and refuse before the next offer`() {
        val inbox = WalletInvocationViewModel()
        inbox.receive("av-vci://")
        inbox.receive(valid())
        val refused = inbox.entries.value.first()
        assertEquals(WalletInvocation.Refused(InvocationRefusal("credential_offer", InvocationRefusalReason.MISSING)), refused.invocation)
        assertEquals(2, inbox.entries.value.size)
        assertFalse(inbox.entries.value.toString().contains("do-not-retain"))
        inbox.dismiss(refused.id)
        val offered = inbox.entries.value.single()
        inbox.providerUnavailable(offered.id)
        assertTrue(inbox.entries.value.single().providerUnavailable)
        // New Compose/Activity consumers observe the same retained typed state.
        assertEquals(offered.invocation, inbox.entries.value.single().invocation)
        inbox.receive(valid())
        assertFalse(inbox.entries.value.single().providerUnavailable)
        assertTrue(offered.id != inbox.entries.value.single().id)
    }

    @Test
    fun `later invocations cannot replace an undismissed refusal or earlier pending outcome`() {
        val inbox = WalletInvocationViewModel()
        inbox.receive(valid("https://first.example"))
        inbox.receive("unknown://")
        inbox.receive(valid("https://second.example"))
        val original = inbox.entries.value
        assertEquals(3, original.size)
        inbox.dismiss(original.first().id)
        assertEquals(original[1], inbox.entries.value.first())
        inbox.receive(valid("https://third.example"))
        assertEquals(original[1], inbox.entries.value.first())
        inbox.dismiss(original[1].id)
        assertEquals(original[2], inbox.entries.value.first())
    }

    @Test
    fun `bounded inbox explicitly counts capacity refusals while preserving existing outcomes`() {
        val inbox = WalletInvocationViewModel()
        repeat(WalletInvocationViewModel.CAPACITY) { inbox.receive("unknown://$it") }
        val originals = inbox.entries.value
        repeat(5) { inbox.receive(valid()) }
        assertEquals(WalletInvocationViewModel.CAPACITY + 1, inbox.entries.value.size)
        assertEquals(originals, inbox.entries.value.take(originals.size))
        val refusal = (inbox.entries.value.last().invocation as WalletInvocation.Refused).refusal
        assertEquals(InvocationRefusal("invocation_queue", InvocationRefusalReason.CAPACITY, 5), refusal)
        assertFalse(inbox.entries.value.toString().contains("do-not-retain"))
    }

    @Test
    fun `oversized existing links become typed refusals without retaining raw data`() {
        val inbox = WalletInvocationViewModel()
        inbox.receive("https://issuer.example/" + "s".repeat(65536))
        assertEquals(
            WalletInvocation.Refused(InvocationRefusal("invocation", InvocationRefusalReason.MALFORMED)),
            inbox.entries.value.single().invocation
        )
    }

    @Test
    fun `oversized AV offers become typed refusals without retaining raw data`() {
        val inbox = WalletInvocationViewModel()
        inbox.receive("av-vci://?credential_offer=" + "x".repeat(65536))
        assertEquals(
            WalletInvocation.Refused(InvocationRefusal("invocation", InvocationRefusalReason.MALFORMED)),
            inbox.entries.value.single().invocation
        )
        assertFalse(inbox.entries.value.toString().contains("x".repeat(128)))
    }

    @Test
    fun `generic navigation heads complete or become a visible refusal without losing the next entry`() {
        val inbox = WalletInvocationViewModel()
        inbox.receive("haip://pid?binding_token=synthetic")
        inbox.receive(valid())
        val head = inbox.entries.value.first()
        assertTrue(head.invocation is WalletInvocation.ExistingLink)
        inbox.navigationRefused(head.id, InvocationRefusal("invocation", InvocationRefusalReason.NAVIGATION))
        assertEquals(WalletInvocation.Refused(InvocationRefusal("invocation", InvocationRefusalReason.NAVIGATION)), inbox.entries.value.first().invocation)
        inbox.dismiss(head.id)
        assertTrue(inbox.entries.value.single().invocation is WalletInvocation.AvIssuance)
        inbox.dismiss(head.id) // A stale callback cannot consume the next outcome.
        assertEquals(1, inbox.entries.value.size)
        inbox.dismiss(inbox.entries.value.first().id)
        assertTrue(inbox.entries.value.isEmpty())
    }
}
