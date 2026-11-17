package ee.cyber.wallet.util

import android.content.Context
import androidx.annotation.RawRes
import ee.cyber.wallet.BuildConfig
import io.grpc.okhttp.OkHttpChannelBuilder
import org.slf4j.LoggerFactory
import java.net.URI
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@JvmSynthetic
internal fun Context.getCertificates(
    @RawRes resId: Int
): List<X509Certificate> =
    resources.openRawResource(resId).use {
        CertificateFactory.getInstance("X509")
            .generateCertificates(it).map { cert -> cert as X509Certificate }
    }

private class TrustAllX509TrustManager : X509TrustManager {
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
}

/**
 * TLS for the backend RPC channels. Debuggable builds trust any server
 * certificate so the self-signed dev backends work; every other build uses
 * the platform trust store, so a release can never ship MITM-able.
 */
/**
 * TLS for https; plaintext only for a debug build talking to a loopback host (the `local`
 * build types, via adb reverse). Anything else — e.g. a release build whose RPC URL was
 * misconfigured to http — is refused rather than sending wallet credentials in the clear.
 */
internal fun OkHttpChannelBuilder.useTransportFor(uri: URI): OkHttpChannelBuilder = when {
    uri.scheme == "https" -> useTransportSecurityForBuild()
    BuildConfig.DEBUG && uri.host in setOf("localhost", "127.0.0.1", "::1") -> usePlaintext()
    else -> throw IllegalStateException("Refusing plaintext gRPC to $uri: https required")
}

internal fun OkHttpChannelBuilder.useTransportSecurityForBuild(): OkHttpChannelBuilder {
    useTransportSecurity()
    if (BuildConfig.DEBUG) {
        LoggerFactory.getLogger("Certificates").warn("Using unsafe trust manager for development")
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(TrustAllX509TrustManager()), null)
        }
        sslSocketFactory(sslContext.socketFactory)
    }
    return this
}
