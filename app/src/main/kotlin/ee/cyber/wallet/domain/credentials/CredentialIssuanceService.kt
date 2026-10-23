package ee.cyber.wallet.domain.credentials

import ee.cyber.wallet.domain.provider.Attestation
import ee.cyber.wallet.domain.provider.wallet.*
import com.nimbusds.jose.jwk.JWK

interface CredentialIssuanceService {
    suspend fun issueCredential(credential: Credential, keyAttestation: KeyAttestation, avIssuance: AvIssuanceRequest? = null): Attestation

    suspend fun createAvIssuanceContext(issuanceKey: JWK): AvIssuanceContext =
        throw AvsuaRefusal(AvsuaRefusalCause.UNSUPPORTED_PROVIDER)

    fun supports(credentialType: CredentialType): Boolean
}
