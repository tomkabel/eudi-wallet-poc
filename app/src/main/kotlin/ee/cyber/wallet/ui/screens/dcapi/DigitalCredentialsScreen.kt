package ee.cyber.wallet.ui.screens.dcapi

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ee.cyber.wallet.R
import ee.cyber.wallet.domain.credentials.CredentialAttribute
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.presentation.PresentationTier
import ee.cyber.wallet.domain.presentation.zkNoticeRes
import ee.cyber.wallet.ui.components.AppContent
import ee.cyber.wallet.ui.components.DocumentCardHeader
import ee.cyber.wallet.ui.components.FadedProgressIndicator
import ee.cyber.wallet.ui.components.FullScreenFadedScrimProgressIndicator
import ee.cyber.wallet.ui.components.HDivider
import ee.cyber.wallet.ui.components.PrimaryButton
import ee.cyber.wallet.ui.components.SecondaryButton
import ee.cyber.wallet.ui.components.VSpace
import ee.cyber.wallet.ui.components.VerifiedParty
import ee.cyber.wallet.ui.components.WSpace
import ee.cyber.wallet.ui.components.rememberBase64DecodedBitmap
import ee.cyber.wallet.ui.screens.documents.docType
import ee.cyber.wallet.ui.screens.documents.label
import ee.cyber.wallet.ui.screens.presentation.MatchedField

@Composable
fun DigitalCredentialsScreen(
    state: DcUiState,
    onEvent: (DcEvent) -> Unit
) {
    Box {
        val emptyFields = state.credentials.all { credential ->
            credential.fields.isEmpty()
        }
        AppContent {
            if (state.plainRefusal != null) {
                RefusedPlainContent(state.plainRefusal.resId, onCancel = { onEvent(DcEvent.OnCancelClicked) })
            } else if (state.protocolRefusal != null) {
                RefusedPlainContent(state.protocolRefusal.messageRes, onCancel = { onEvent(DcEvent.OnCancelClicked) })
            } else if (state.isLoading && emptyFields) {
                LoadingContent()
            } else if (state.credentials.isEmpty() && !state.isLoading) {
                NoMatchContent(onCancel = { onEvent(DcEvent.OnCancelClicked) })
            } else {
                DcPresentationContent(state = state, onEvent = onEvent)
            }
        }
        FullScreenFadedScrimProgressIndicator(visible = state.isLoading && !emptyFields)
    }
}

/**
 * EE-ZKP-051, strict reading: the device can prove, but the relying party advertised only circuits
 * this wallet does not hold, or the proof failed. The plain fallback would be linkable, so it is
 * refused and the user is told why instead of being silently downgraded. Also shows the EE-PRO-013
 * protocol refusal (F8).
 */
@Composable
private fun RefusedPlainContent(@StringRes message: Int, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        VSpace(24.dp)
        SecondaryButton(
            text = stringResource(R.string.close_btn),
            onClick = onCancel
        )
    }
}

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Processing document request",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        VSpace(24.dp)
        FadedProgressIndicator()
    }
}

@Composable
private fun NoMatchContent(onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_presentation_match_error),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold
        )
        VSpace(24.dp)
        SecondaryButton(
            text = stringResource(R.string.cancel_btn),
            onClick = onCancel
        )
    }
}

@Composable
private fun DcPresentationContent(
    modifier: Modifier = Modifier,
    state: DcUiState,
    onEvent: (DcEvent) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = state.verifier,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        // EE-RP-003 (4d, F7): when the reader authenticated the request, the consent screen shows
        // the readerAuth certificate subject — a relying-party name from the request itself, not
        // just the platform-asserted origin. Absent when the reader did not authenticate. It sits
        // below the "verified party" badge, which belongs to the origin: the certificate's trust
        // is not validated (§8.3), so the badge must not read as vouching for this name.
        VerifiedParty()
        state.readerSubject?.let { subject ->
            Text(
                text = subject,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
        VSpace(24.dp)
        Text(
            style = MaterialTheme.typography.bodyMedium,
            text = stringResource(R.string.presentation_requests_following_data_to),
            textAlign = TextAlign.Center
        )
        VSpace(24.dp)
        DcPresentationRequestCard(
            state = state,
            onEvent = onEvent
        )
        VSpace(24.dp)
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            text = stringResource(R.string.presentation_footer_note, state.verifier),
            textAlign = TextAlign.Center
        )
        state.expectedPlainTier?.let { tier ->
            VSpace(24.dp)
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyMedium,
                // EE-ZKP-042: the user is told, before sharing, that this presentation can be
                // linked by the issuer. The wording depends on whether the party asked for a
                // proof the wallet cannot give, or never asked for one at all.
                text = stringResource(tier.zkNoticeRes()),
                textAlign = TextAlign.Center
            )
        }
        VSpace(24.dp)
        WSpace()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally)
        ) {
            SecondaryButton(
                text = stringResource(R.string.cancel_btn),
                onClick = { onEvent(DcEvent.OnCancelClicked) }
            )
            PrimaryButton(
                text = stringResource(R.string.share_btn),
                enabled = !state.shareDisabled,
                onClick = { onEvent(DcEvent.OnShareClicked) }
            )
        }
    }
}

@Composable
private fun DcPresentationRequestCard(state: DcUiState, onEvent: (DcEvent) -> Unit) {
    Column {
        state.credentials.forEach { credential ->
            Card(modifier = Modifier.padding(bottom = 16.dp)) {
                DocumentCardHeader(docType = credential.credentialType.docType())
                HDivider()
                Column {
                    credential.fields.forEach { field ->
                        DcPresentationOptionalField(
                            matchedField = field,
                            isMandatory = true,
                            docType = credential.credentialType.docType()
                        ) { checked ->
                            onEvent(DcEvent.OnOptionalFieldChange(field, checked))
                        }
                        HDivider()
                    }
                    credential.optionalFields.forEach { field ->
                        DcPresentationOptionalField(
                            matchedField = field,
                            isMandatory = false,
                            docType = credential.credentialType.docType()
                        ) { checked ->
                            onEvent(DcEvent.OnOptionalFieldChange(field, checked))
                        }
                        HDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun DcPresentationOptionalField(
    matchedField: MatchedField,
    isMandatory: Boolean,
    docType: DocType,
    onCheck: (Boolean) -> Unit
) {
    Row(
        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = matchedField.field.label(docType),
                style = MaterialTheme.typography.bodySmall
            )
            if (matchedField.field.name == CredentialAttribute.ORG_ISO_18013_5_1_PORTRAIT.fieldName ||
                matchedField.field.name == CredentialAttribute.MDOC_PID_1_PORTRAIT.fieldName ||
                matchedField.field.name == CredentialAttribute.JWT_PID_1_PICTURE.fieldName ||
                matchedField.field.name == CredentialAttribute.ORG_ISO_18013_5_1_BIOMETRIC_TEMPLATE_FACE.fieldName ||
                matchedField.field.name == CredentialAttribute.ORG_ISO_18013_5_1_BIOMETRIC_TEMPLATE_FINGER.fieldName ||
                matchedField.field.name == CredentialAttribute.ORG_ISO_18013_5_1_BIOMETRIC_TEMPLATE_SIGNATURE_SIGN.fieldName ||
                matchedField.field.name == CredentialAttribute.ORG_ISO_18013_5_1_BIOMETRIC_TEMPLATE_IRIS.fieldName ||
                matchedField.field.name == CredentialAttribute.ORG_ISO_18013_5_1_SIGNATURE_USUAL_MARK.fieldName
            ) {
                rememberBase64DecodedBitmap(base64Image = matchedField.field.value)?.let {
                    Image(bitmap = it, contentDescription = "")
                } ?: Text(
                    text = stringResource(R.string.presentation_image_stub),
                    style = MaterialTheme.typography.titleMedium
                )
            } else {
                Text(
                    text = matchedField.field.value,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        WSpace()
        Switch(
            checked = matchedField.checked,
            onCheckedChange = { onCheck(it) },
            colors = if (isMandatory) {
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                )
            } else {
                SwitchDefaults.colors(
                    checkedThumbColor = Color.DarkGray,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            }
        )
    }
}
