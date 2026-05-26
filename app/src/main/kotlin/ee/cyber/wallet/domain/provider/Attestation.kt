package ee.cyber.wallet.domain.provider

import android.os.Parcelable
import ee.cyber.wallet.domain.credentials.CredentialType
import ee.cyber.wallet.domain.provider.wallet.KeyAttestation
import kotlinx.parcelize.Parcelize

@Parcelize
data class Attestation(
    val id: String,
    val credential: String,
    val type: CredentialType,
    val keyAttestation: KeyAttestation,
    val issuedAt: Long = kotlin.time.Clock.System.now().toEpochMilliseconds()
) : Parcelable
