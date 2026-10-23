package ee.cyber.wallet.ui.screens.issuance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ee.cyber.wallet.R
import ee.cyber.wallet.domain.credentials.AvVciOffer
import ee.cyber.wallet.domain.credentials.InvocationRefusal
import ee.cyber.wallet.domain.credentials.InvocationRefusalReason

/** External issuance has its own continuation, entirely separate from mock document issuance. */
@Composable
fun AvVciInvocationDialog(
    offer: AvVciOffer?,
    refusal: InvocationRefusal?,
    providerUnavailable: Boolean,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    if (offer == null && refusal == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (refusal != null) R.string.invocation_refused_title else R.string.av_invocation_title))
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (refusal != null) {
                    val reason = when (refusal.reason) {
                        InvocationRefusalReason.MISSING -> R.string.invocation_missing
                        InvocationRefusalReason.MALFORMED -> R.string.invocation_malformed
                        InvocationRefusalReason.UNSUPPORTED -> R.string.invocation_unsupported
                        InvocationRefusalReason.NAVIGATION -> R.string.invocation_navigation_failed
                        InvocationRefusalReason.CAPACITY -> R.string.invocation_capacity
                    }
                    Text(
                        if (refusal.reason == InvocationRefusalReason.CAPACITY) stringResource(reason, refusal.refusedCount)
                        else stringResource(reason, refusal.parameter)
                    )
                } else if (offer != null) {
                    Text(stringResource(R.string.av_invocation_document, offer.docType))
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.av_invocation_issuer, offer.credentialIssuer))
                    Text(stringResource(R.string.av_invocation_unverified_issuer))
                    if (providerUnavailable) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.av_invocation_provider_unavailable))
                    }
                }
            }
        },
        confirmButton = {
            if (offer != null && !providerUnavailable) {
                TextButton(onClick = onContinue) { Text(stringResource(R.string.av_invocation_continue)) }
            } else {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.av_invocation_close)) }
            }
        },
        dismissButton = {
            if (offer != null && !providerUnavailable) {
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.av_invocation_cancel)) }
            }
        }
    )
}
