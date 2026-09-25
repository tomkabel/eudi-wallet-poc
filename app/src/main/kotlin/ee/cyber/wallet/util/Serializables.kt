package ee.cyber.wallet.util

import android.util.Base64
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

// TODO: Serialization of AuthorizationRequestPrepared fails for
//  eu.europa.ec.eudi:eudi-lib-jvm-openid4vci-kt:0.6.0 due to
//  eu.europa.ec.eudi.openid4vci.CredentialConfigurationIdentifier
//  and eu.europa.ec.eudi.openid4vci.CredentialIdentifier not
//  implementing java.io.Serializable; As a workaround, we serialize
//  using Jackson and Base64 encoding.
//
// D15 (jvm L1): persisted issuer-authorization state must not silently grow fields. FAIL_ON_
// UNKNOWN_PROPERTIES (Jackson's default-on for plain readValue) is explicitly configured here
// because a library upgrade that adds/renames a property inside AuthorizationRequestPrepared
// would otherwise deserialize a stale persisted blob into a half-populated request object and
// resume an issuance flow with wrong state; failing loudly surfaces the drift at the next
// startup instead. This is the strict-mode *decision* record: strict is correct while the
// payload's producer and consumer ship in the same app binary — there is no forward-compat
// window to bridge. A versioned envelope ({"v":1, ...}) was considered and rejected: with a
// single in-app producer/consumer the version is always 1, and it would suggest a migration
// path that does not exist.
val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

fun <T> T.toBase64Json(): String {
    val jsonString = mapper.writeValueAsString(this)
    return Base64.encodeToString(jsonString.toByteArray(), Base64.NO_WRAP)
}

inline fun <reified T> String.fromBase64Json(): T {
    val jsonString = Base64.decode(this, Base64.NO_WRAP).toString(Charsets.UTF_8)
    return mapper.readValue(jsonString, T::class.java)
}
