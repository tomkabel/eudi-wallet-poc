package ee.cyber.wallet.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.cyber.wallet.R
import ee.cyber.wallet.domain.documents.CredentialToDocumentMapper
import ee.cyber.wallet.util.getCertificates
import java.security.cert.CertificateFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConvertersModule {

    @Singleton
    @Provides
    fun providesCredentialToDocumentConverter(
        @ApplicationContext context: Context
    ): CredentialToDocumentMapper {
        // E6: the mdoc issuer chains anchor at the IACA root the dev setup ships
        // (assets/keys/iaca_root.cer.pem — the same root the verifier README's
        // "Configure issuer chain" step imports). SD-JWT x5c keeps the trusted.pem
        // set. The two lists are deliberately separate so tightening one never
        // silently moves the other.
        val iacaRoot = context.assets.open("keys/iaca_root.cer.pem").use { stream ->
            CertificateFactory.getInstance("X509").generateCertificate(stream) as java.security.cert.X509Certificate
        }
        return CredentialToDocumentMapper(
            trustAnchors = context.getCertificates(R.raw.trusted),
            issuerAnchors = listOf(iacaRoot)
        )
    }
}
