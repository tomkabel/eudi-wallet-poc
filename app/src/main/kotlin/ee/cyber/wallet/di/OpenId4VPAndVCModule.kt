package ee.cyber.wallet.di

import android.content.Context
import com.nimbusds.jose.EncryptionMethod
import com.nimbusds.jose.JWEAlgorithm
import com.nimbusds.jose.JWSAlgorithm
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ee.cyber.wallet.AppConfig
import ee.cyber.wallet.R
import ee.cyber.wallet.crypto.CryptoProvider
import ee.cyber.wallet.data.datastore.AuthorizationStateDataSource
import ee.cyber.wallet.domain.credentials.OpenId4VCIManager
import ee.cyber.wallet.domain.documents.CredentialToDocumentMapper
import ee.cyber.wallet.domain.presentation.DcqlRequestProcessor
import ee.cyber.wallet.domain.presentation.OpenId4VPManager
import ee.cyber.wallet.security.CertificateChainValidator
import ee.cyber.wallet.security.FetchLOTLCertificatesDSS
import ee.cyber.wallet.util.JsonSupport
import ee.cyber.wallet.util.getCertificates
import eu.europa.ec.eudi.openid4vp.CoseAlgorithm
import eu.europa.ec.eudi.openid4vp.JarConfiguration
import eu.europa.ec.eudi.openid4vp.ResponseEncryptionConfiguration
import eu.europa.ec.eudi.openid4vp.SiopOpenId4VPConfig
import eu.europa.ec.eudi.openid4vp.SupportedClientIdPrefix
import eu.europa.ec.eudi.openid4vp.SupportedRequestUriMethods
import eu.europa.ec.eudi.openid4vp.VPConfiguration
import eu.europa.ec.eudi.openid4vp.VpFormatsSupported
import eu.europa.ec.eudi.openid4vp.X509CertificateTrust
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import org.slf4j.LoggerFactory
import java.security.cert.X509Certificate
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object OpenId4VPAndVCModule {

    @Singleton
    @Provides
    fun providesOpenId4VPManager(
        @ApplicationContext context: Context,
        @Dispatcher(WalletDispatchers.IO) dispatcher: CoroutineDispatcher,
        cryptoProviderFactory: CryptoProvider.Factory
    ): OpenId4VPManager {
        val openId4VPConfig = SiopOpenId4VPConfig(
            supportedClientIdPrefixes = listOf(
                SupportedClientIdPrefix.X509SanDns(simpleCertificateChainValidator(context.getCertificates(R.raw.trusted))),
                SupportedClientIdPrefix.X509Hash(simpleCertificateChainValidator(context.getCertificates(R.raw.trusted))),
                SupportedClientIdPrefix.RedirectUri
            ),
            jarConfiguration = JarConfiguration(
                supportedAlgorithms = listOf(
                    JWSAlgorithm.ES256, JWSAlgorithm.ES384, JWSAlgorithm.ES512,
                    JWSAlgorithm.RS256, JWSAlgorithm.RS384, JWSAlgorithm.RS512,
                    JWSAlgorithm.PS256, JWSAlgorithm.PS384, JWSAlgorithm.PS512
                ),
                supportedRequestUriMethods = SupportedRequestUriMethods.Default
            ),
            responseEncryptionConfiguration = ResponseEncryptionConfiguration.Supported(
                supportedAlgorithms = listOf(JWEAlgorithm.ECDH_ES),
                supportedMethods = listOf(EncryptionMethod.A128CBC_HS256, EncryptionMethod.A256GCM, EncryptionMethod.A128GCM)
            ),
            vpConfiguration = VPConfiguration(
                vpFormatsSupported = VpFormatsSupported(
                    msoMdoc = VpFormatsSupported.MsoMdoc(
                        issuerAuthAlgorithms = listOf(CoseAlgorithm(-7)),
                        deviceAuthAlgorithms = listOf(CoseAlgorithm(-7)),
                    ),
                    sdJwtVc = VpFormatsSupported.SdJwtVc.HAIP
                )
            ),
            clock = Clock.systemDefaultZone()
        )
        return OpenId4VPManager(
            dispatcher = dispatcher,
            openId4VPConfig = openId4VPConfig,
            cryptoProviderFactory = cryptoProviderFactory,
            httpClient = createHttpClient()
        )
    }

    @Singleton
    @Provides
    fun provideOpenId4VCIManager(
        authorizationStateDataSource: AuthorizationStateDataSource,
        credentialToDocumentMapper: CredentialToDocumentMapper,
        cryptoProviderFactory: CryptoProvider.Factory
    ): OpenId4VCIManager {
        return OpenId4VCIManager(
            httpClientFactory = { createHttpClient() },
            issuerUrl = AppConfig.issuerUrl,
            credentialToDocumentMapper = credentialToDocumentMapper,
            authorizationStateDataSource = authorizationStateDataSource,
            cryptoProviderFactory = cryptoProviderFactory
        )
    }

    @Singleton
    @Provides
    fun provideDcqlRequestProcessor(): DcqlRequestProcessor {
        return DcqlRequestProcessor()
    }

    @Singleton
    @Provides
    fun provideFetchLOTLCertificates(
        @ApplicationContext context: Context
    ): FetchLOTLCertificatesDSS {
        return FetchLOTLCertificatesDSS(context)
    }

//    @Singleton
//    @Provides
//    fun provideProofSigner(walletKeyProvider: WalletKeyProvider): ProofSigner = ProofSigner.make(
//        privateKey = walletKeyProvider.key,
//        publicKey = BindingKey.Jwk(jwk = walletKeyProvider.key.toPublicJWK()),
//        algorithm = walletKeyProvider.algorithm
//    )

    private fun simpleCertificateChainValidator(trustAnchors: List<X509Certificate>) = X509CertificateTrust {
        CertificateChainValidator.validateCertificateChain(it, trustAnchors, false)
    }

    private val httpLogger = LoggerFactory.getLogger("HTTP")

    private fun createHttpClient(): HttpClient = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = AppConfig.clientReadTimeoutSeconds.toLong() * 1000
            socketTimeoutMillis = AppConfig.clientReadTimeoutSeconds.toLong() * 1000
            connectTimeoutMillis = AppConfig.clientConnectTimeoutSeconds.toLong() * 1000
        }
        defaultRequest {
            header("connection", "close")
        }
        install(ContentNegotiation) {
            json(JsonSupport.json)
        }
        install(UserAgent) {
            agent = AppConfig.userAgent
        }
        install(Logging) {
            this.logger = object : Logger {
                override fun log(message: String) {
                    message.chunked(4000).forEach {
                        httpLogger.info(it)
                    }
                }
            }
            level = LogLevel.entries.firstOrNull { it.name == AppConfig.clientLogLevel } ?: LogLevel.NONE
        }

        followRedirects = false
        expectSuccess = false
    }

}
