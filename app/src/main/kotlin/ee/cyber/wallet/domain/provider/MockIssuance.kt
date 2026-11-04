package ee.cyber.wallet.domain.provider

import ee.cyber.wallet.domain.provider.ageverification.BatchAgeIssuer

/**
 * The mock mDL and age-verification issuers, implemented only in the local_mocks build
 * (`src/local_mocks`); every other build type binds a stub that refuses, so no mock issuer
 * class is compiled into them. Callers reach it only when `AppConfig.useMocks` is true.
 */
interface MockIssuance {
    suspend fun issueMdl(): Attestation
    suspend fun issueAgeVerification(batchIssuer: BatchAgeIssuer): List<Attestation>
}
