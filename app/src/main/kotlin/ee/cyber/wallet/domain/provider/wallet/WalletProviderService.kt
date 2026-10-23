package ee.cyber.wallet.domain.provider.wallet

import com.nimbusds.jose.jwk.JWK

interface WalletProviderService {
    /** Unsupported real providers must refuse until their issuance protocol is implemented. */
    suspend fun attestAvIssuance(context: AvIssuanceContext, issuanceKey: JWK, credentials: WalletInstanceCredentials): AvsuaEvidence =
        throw AvsuaRefusal(AvsuaRefusalCause.UNSUPPORTED_PROVIDER)

    suspend fun registerWalletInstance(deviceData: DeviceData): WalletInstanceCredentials
    fun supportsKey(keyType: KeyType): Boolean
    suspend fun activateInstance(personalDataAccessToken: ByteArray, credentials: WalletInstanceCredentials)
    suspend fun generateKey(keyType: KeyType, credentials: WalletInstanceCredentials): KeyAttestation
    suspend fun sign(keyAttestation: KeyAttestation, dataToBeSigned: ByteArray, credentials: WalletInstanceCredentials): ByteArray
    suspend fun attestKey(keyId: String, keyType: KeyType, jwk: JWK, credentials: WalletInstanceCredentials): KeyAttestation
}
