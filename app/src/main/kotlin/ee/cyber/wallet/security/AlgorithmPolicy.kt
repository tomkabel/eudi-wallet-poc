package ee.cyber.wallet.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey

/**
 * PKI plan NEW-S1-2: the shared certificate algorithm policy. The bytes are
 * `verifier/go/internal/algpolicy/policy/algorithms.json`, packaged as a Java resource
 * (app/build.gradle.kts), the same file the Go verifier embeds.
 */
internal object AlgorithmPolicy {

    private class Policy(val sigAlgs: Set<String>, val curves: Set<String>, val rsaMinBits: Int)

    // A missing or broken policy throws here, and every validation that reaches it fails closed.
    private val policy: Policy by lazy {
        val text = AlgorithmPolicy::class.java.classLoader!!.getResourceAsStream("algorithms.json")
            ?.use { it.readBytes().decodeToString() }
            ?: error("algorithms.json is not on the classpath")
        val root = Json.parseToJsonElement(text).jsonObject
        fun strings(key: String) = root.getValue(key).jsonArray.map { it.jsonPrimitive.content }.toSet()
        Policy(strings("x509_signature_algorithms"), strings("ec_curves"), root.getValue("rsa_min_bits").jsonPrimitive.int)
    }

    /** Returns null when [cert] meets the policy, else the reason it does not. */
    fun violation(cert: X509Certificate): String? {
        if (cert.sigAlgOID !in policy.sigAlgs) {
            return "signature algorithm ${cert.sigAlgOID} is not in the algorithm policy"
        }
        return when (val key = cert.publicKey) {
            is ECPublicKey -> {
                val curve = SubjectPublicKeyInfo.getInstance(key.encoded).algorithm.parameters as? ASN1ObjectIdentifier
                if (curve?.id in policy.curves) null else "curve ${curve?.id} is not in the algorithm policy"
            }
            is RSAPublicKey -> {
                val bits = key.modulus.bitLength()
                if (bits >= policy.rsaMinBits) null else "RSA-$bits is below the policy minimum of ${policy.rsaMinBits} bits"
            }
            else -> "public key type ${key.algorithm} is not in the algorithm policy"
        }
    }
}
