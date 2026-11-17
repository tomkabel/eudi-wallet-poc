package ee.cyber.wallet.domain.provider.wallet

import android.content.ContextWrapper
import androidx.datastore.core.DataStore
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.Curve
import com.nimbusds.jose.jwk.ECKey
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import ee.cyber.wallet.UserPreferencesProto
import ee.cyber.wallet.data.database.KeyAttestationEntity
import ee.cyber.wallet.data.database.dao.KeyAttestationDao
import ee.cyber.wallet.data.datastore.UserPreferencesDataSource
import ee.cyber.wallet.di.IssuerModule
import ee.cyber.wallet.domain.credentials.Credential
import ee.cyber.wallet.domain.credentials.CredentialIssuanceServiceMock
import ee.cyber.wallet.domain.credentials.DocType
import ee.cyber.wallet.domain.provider.ageverification.WalletProviderBatchAgeIssuer
import ee.cyber.wallet.security.*
import id.walt.mdoc.doc.MDoc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.cose.java.OneKey
import org.junit.Before
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.interfaces.ECPublicKey
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.ECGenParameterSpec
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import kotlin.test.*

/** Executes the actual Android mock AP mint and actual batch transaction, with Android storage faked. */
class AvsuaIssuanceTest {
    private val credentials = WalletInstanceCredentials("must-not-appear-account", "must-not-appear-secret")
    private val av = Credential.AgeVerificationCredential(
        ageOver13 = true, ageOver15 = true, ageOver16 = true, ageOver18 = true,
        ageOver21 = false, ageOver23 = false, ageOver25 = false, ageOver27 = false,
        ageOver28 = false, ageOver40 = false, ageOver60 = false, ageOver65 = false,
        ageOver67 = false, locationStatus = Credential.LocationStatus("1", "https://demo.invalid/status")
    )

    private class MemoryKeys : KeyStoreManager {
        val store = KeyStore.getInstance("BKS").apply { load(null, null) }
        var reads = 0
        override fun keyStore() = store.also { reads++ }
        override fun containsKey(alias: String) = store.containsAlias(alias)
        override fun deleteKey(alias: String) = store.deleteEntry(alias)
        override fun clearAll() = store.aliases().toList().forEach { store.deleteEntry(it) }
        override fun createKeyPair(alias: String, keyAlgorithm: String, certSignAlgorithm: String, paramSpec: AlgorithmParameterSpec) {
            val pair = KeyPairGenerator.getInstance(keyAlgorithm).apply { initialize(paramSpec) }.generateKeyPair()
            store.setKeyEntry(alias, pair.private, charArrayOf(), arrayOf(certificate(pair, certSignAlgorithm)))
        }
    }

    private class Preferences : DataStore<UserPreferencesProto> {
        override val data = MutableStateFlow(UserPreferencesProto.getDefaultInstance())
        override suspend fun updateData(transform: suspend (UserPreferencesProto) -> UserPreferencesProto) =
            transform(data.value).also { data.value = it }
    }

    private class Rows : KeyAttestationDao {
        val rows = mutableMapOf<String, KeyAttestationEntity>()
        override suspend fun getById(id: String) = rows.getValue(id)
        override suspend fun insert(entity: KeyAttestationEntity) { rows[entity.id] = entity }
        override suspend fun deleteById(id: String) { rows.remove(id) }
        override suspend fun deleteAll() = rows.clear()
        override suspend fun getByType(keyType: String) = rows.values.filter { it.keyType == keyType }
    }

    private class BatchKeys : SecureAreaBatchKeyManager {
        val live = mutableMapOf<String, SecureAreaDeviceKey>()
        val deleted = mutableListOf<String>()
        override suspend fun batchCreateKey(count: Int): SecureAreaKeyBatch = SecureAreaKeyBatch(List(count) {
            val pair = ecPair()
            val jwk = ECKey.Builder(Curve.P_256, pair.public as ECPublicKey).build()
            val key = SecureAreaDeviceKey(
                "device-${live.size}",
                org.multipaz.crypto.EcPublicKeyDoubleCoordinate(org.multipaz.crypto.EcCurve.P256, jwk.x.decode(), jwk.y.decode()),
                org.multipaz.crypto.X509CertChain(listOf(org.multipaz.crypto.X509Cert(kotlinx.io.bytestring.ByteString(certificate(pair, "SHA256withECDSA").encoded)))),
                HardwareBacking.TEE
            )
            live[key.keyId] = key
            key
        })
        override suspend fun deleteKey(keyId: String) { deleted += keyId; live.remove(keyId) }
        override suspend fun keyExists(keyId: String) = keyId in live
        override suspend fun deleteAllKeys() = live.clear()
    }

    @Before
    fun setup() { Security.addProvider(BouncyCastleProvider()) }

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant() = now
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }

    private inner class Fixture(
        audience: String = CredentialIssuanceServiceMock.AV_AUDIENCE,
        apClock: Clock = Clock.systemUTC(),
        verifierClock: Clock = apClock,
        trustKey: (() -> JWK?)? = null
    ) {
        val keys = MemoryKeys()
        val provider = WalletProviderServiceMock(keys, Dispatchers.Unconfined)
        val holder = ECKey.Builder(Curve.P_256, ecPair().public as ECPublicKey).build()
        var mintKeyLoads = 0
        var failSigning = false
        val issuerPair = ecPair()
        val issuerCertificate = certificate(issuerPair, "SHA256withECDSA")
        val issuer = CredentialIssuanceServiceMock(
            ContextWrapper(null), Dispatchers.Unconfined, UserPreferencesDataSource(Preferences()),
            AvsuaVerifier(trustKey ?: { provider.avsuaTrustKey() }, verifierClock),
            avClock = apClock,
            avAudience = audience,
            issuerKeyLoader = { _, alias ->
                mintKeyLoads++
                if (failSigning) throw IllegalStateException("test issuer signing key unavailable")
                ECKey.Builder(Curve.P_256, issuerPair.public as ECPublicKey).privateKey(issuerPair.private)
                    .keyID(alias).x509CertChain(listOf(com.nimbusds.jose.util.Base64.encode(issuerCertificate.encoded))).build()
            }
        )
        suspend fun key(jwk: JWK = holder) = provider.attestKey("holder-alias", KeyType.EC, jwk, credentials)
        suspend fun request(jwk: JWK = holder): AvIssuanceRequest {
            val context = issuer.createAvIssuanceContext(jwk)
            return AvIssuanceRequest(context, provider.attestAvIssuance(context, jwk, credentials))
        }
        fun resign(
            token: String,
            header: JWSHeader = SignedJWT.parse(token).header,
            change: JWTClaimsSet.Builder.() -> Unit
        ): String {
            val jwt = SignedJWT.parse(token)
            return SignedJWT(header, JWTClaimsSet.Builder(jwt.jwtClaimsSet).apply(change).build()).also {
                it.sign(RSASSASigner(JWK.load(keys.store, WalletProviderServiceMock.PROVIDER_KEY_ALIAS, charArrayOf()) as RSAKey))
            }.serialize()
        }
        suspend fun refused(cause: AvsuaRefusalCause, request: AvIssuanceRequest?, key: KeyAttestation? = null) {
            val before = mintKeyLoads
            assertEquals(cause, assertFailsWith<AvsuaRefusal> { issuer.issueCredential(av, key ?: key(), request) }.causeCode)
            assertEquals(before, mintKeyLoads, "refusal must precede all credential-signing key access")
        }
    }

    @Test
    fun `actual mint validates both parts and returns AV mdoc bound to issuance key`() = runTest {
        val f = Fixture()
        val result = f.issuer.issueCredential(av, f.key(), f.request())
        val mdoc = MDoc.fromCBORHex(result.credential)
        assertEquals(DocType.AGE_VERIFICATION.uri, mdoc.docType.value)
        val deviceKey = OneKey(com.upokecenter.cbor.CBORObject.DecodeFromBytes(mdoc.MSO!!.deviceKeyInfo.deviceKey.toCBOR()))
        assertContentEquals(f.holder.toECPublicKey().encoded, deviceKey.AsPublicKey().encoded)
        assertTrue(mdoc.verifySignature(f.issuer.ageVerificationIssuerCryptoProvider(), "mdl_issuer"))
        assertEquals(setOf("eu.europa.ec.av.1"), mdoc.nameSpaces)
        assertEquals(listOf("age_over_18"), mdoc.getIssuerSignedItems("eu.europa.ec.av.1").map { it.elementIdentifier.value })
        assertNull(mdoc.MSO!!.status)
    }

    @Test
    fun `bundle and either signed part withheld refuse at actual mint boundary and public helper`() = runTest {
        val f = Fixture()
        val request = f.request()
        f.refused(AvsuaRefusalCause.MISSING_BUNDLE, null)
        f.refused(AvsuaRefusalCause.MISSING_BUNDLE, request.copy(evidence = null))
        f.refused(AvsuaRefusalCause.MISSING_INSTANCE, request.copy(evidence = request.evidence!!.copy(instanceAttestation = null)))
        f.refused(AvsuaRefusalCause.MISSING_KEY, request.copy(evidence = request.evidence!!.copy(keyAttestation = null)))
        assertEquals(AvsuaRefusalCause.MISSING_BUNDLE, assertFailsWith<AvsuaRefusal> { f.issuer.issueMDocAgeVerification(av, f.key()) }.causeCode)
        assertEquals(0, f.mintKeyLoads)
    }

    @Test
    fun `tampered and untrusted signatures including supplied x5c cannot establish trust`() = runTest {
        val f = Fixture()
        val request = f.request()
        for (instancePart in listOf(true, false)) {
            val token = if (instancePart) request.evidence!!.instanceAttestation!! else request.evidence!!.keyAttestation!!
            val pieces = token.split('.').toMutableList()
            val signature = com.nimbusds.jose.util.Base64URL(pieces[2]).decode().also { it[0] = (it[0].toInt() xor 1).toByte() }
            pieces[2] = com.nimbusds.jose.util.Base64URL.encode(signature).toString()
            val tampered = pieces.joinToString(".")
            val bundle = if (instancePart) request.evidence!!.copy(instanceAttestation = tampered) else request.evidence!!.copy(keyAttestation = tampered)
            f.refused(AvsuaRefusalCause.SIGNATURE_OR_TRUST, request.copy(evidence = bundle))
        }
        val untrusted = Fixture()
        val foreign = untrusted.provider.attestAvIssuance(request.context, f.holder, credentials)
        val foreignKey = JWK.load(untrusted.keys.store, WalletProviderServiceMock.PROVIDER_KEY_ALIAS, charArrayOf()) as RSAKey
        val jwt = SignedJWT.parse(foreign.instanceAttestation)
        val suppliedCertificate = SignedJWT(JWSHeader.Builder(jwt.header).x509CertChain(foreignKey.x509CertChain).build(), jwt.jwtClaimsSet)
            .also { it.sign(RSASSASigner(foreignKey)) }.serialize()
        f.refused(AvsuaRefusalCause.SIGNATURE_OR_TRUST, request.copy(evidence = foreign.copy(instanceAttestation = suppliedCertificate)))
    }

    @Test
    fun `wrong key audience transaction and challenge refuse including signed token claims`() = runTest {
        val f = Fixture()
        val request = f.request()
        f.refused(AvsuaRefusalCause.BINDING, request, Fixture().key())
        for (context in listOf(request.context.copy(audience = "wrong"), request.context.copy(transactionId = "wrong"), request.context.copy(challenge = "wrong"))) {
            f.refused(AvsuaRefusalCause.BINDING, request.copy(context = context))
        }
        val changes: List<JWTClaimsSet.Builder.() -> Unit> = listOf(
            { audience("wrong") }, { claim("transaction_id", "wrong") }, { claim("nonce", "wrong") },
            { claim("issuance_key_jkt", "wrong") }, { issuer("wrong") }
        )
        for (instancePart in listOf(true, false)) for (change in changes) {
            val bundle = request.evidence!!
            val changed = if (instancePart) bundle.copy(instanceAttestation = f.resign(bundle.instanceAttestation!!, change = change))
            else bundle.copy(keyAttestation = f.resign(bundle.keyAttestation!!, change = change))
            f.refused(AvsuaRefusalCause.BINDING, request.copy(evidence = changed))
        }
    }

    @Test
    fun `context stays bound to original key even with trusted evidence for another key`() = runTest {
        val f = Fixture()
        val context = f.issuer.createAvIssuanceContext(f.holder)
        val otherKey = ECKey.Builder(Curve.P_256, ecPair().public as ECPublicKey).build()
        val evidence = f.provider.attestAvIssuance(context, otherKey, credentials)
        f.refused(AvsuaRefusalCause.BINDING, AvIssuanceRequest(context, evidence), f.key(otherKey))
    }

    @Test
    fun `expired future missing and oversized lifetimes refuse both signed parts`() = runTest {
        val f = Fixture()
        val request = f.request()
        val now = Instant.now()
        val changes: List<JWTClaimsSet.Builder.() -> Unit> = listOf(
            { issueTime(Date.from(now.minusSeconds(180))); notBeforeTime(Date.from(now.minusSeconds(180))); expirationTime(Date.from(now.minusSeconds(60))) },
            { issueTime(Date.from(now.plusSeconds(60))) }, { notBeforeTime(Date.from(now.plusSeconds(60))) },
            { expirationTime(null) }, { issueTime(null) }, { notBeforeTime(null) },
            { expirationTime(Date.from(now.plusSeconds(3600))) }
        )
        for (instancePart in listOf(true, false)) for (change in changes) {
            val bundle = request.evidence!!
            val changed = if (instancePart) bundle.copy(instanceAttestation = f.resign(bundle.instanceAttestation!!, change = change))
            else bundle.copy(keyAttestation = f.resign(bundle.keyAttestation!!, change = change))
            f.refused(AvsuaRefusalCause.VALIDITY, request.copy(evidence = changed))
        }
    }

    @Test
    fun `missing and negative integrity and missing key security assertions refuse`() = runTest {
        val f = Fixture()
        val request = f.request()
        for (value in listOf(null, false)) {
            val token = f.resign(request.evidence!!.instanceAttestation!!) { claim("integrity_ok", value) }
            f.refused(AvsuaRefusalCause.INTEGRITY, request.copy(evidence = request.evidence!!.copy(instanceAttestation = token)))
        }
        for ((value, cause) in listOf(null to AvsuaRefusalCause.KEY_SECURITY,
            "wrong-assertion" to AvsuaRefusalCause.KEY_SECURITY, 42 to AvsuaRefusalCause.MALFORMED)) {
            val token = f.resign(request.evidence!!.keyAttestation!!) { claim("key_security", value) }
            f.refused(cause, request.copy(evidence = request.evidence!!.copy(keyAttestation = token)))
        }
    }

    @Test
    fun `fresh sessions across APs contain no account identifiers or holder key material and cannot replay`() = runTest {
        val f = Fixture()
        val other = Fixture("urn:ee:wallet:another-demo-ap")
        val requests = listOf(f.request(), f.request(), other.request())
        assertEquals(3, requests.map { it.context.transactionId }.toSet().size)
        assertEquals(3, requests.map { it.context.challenge }.toSet().size)
        val ids = mutableSetOf<String>()
        for (request in requests) for (token in listOf(request.evidence!!.instanceAttestation!!, request.evidence!!.keyAttestation!!)) {
            val jwt = SignedJWT.parse(token)
            assertEquals(setOf("alg", "typ"), jwt.header.toJSONObject().keys)
            val decoded = jwt.header.toString() + jwt.jwtClaimsSet.toString()
            for (identifier in listOf(credentials.instanceId, credentials.instancePassword, "holder-alias", WalletProviderServiceMock.PROVIDER_KEY_ALIAS)) {
                assertFalse(decoded.contains(identifier))
            }
            val claims = jwt.jwtClaimsSet
            assertTrue(ids.add(claims.jwtid))
            assertEquals(setOf("iss", "aud", "jti", "iat", "nbf", "exp", "transaction_id", "nonce", "issuance_key_jkt", "demo_only") +
                if (claims.getClaim("integrity_ok") != null) "integrity_ok" else "key_security", claims.claims.keys)
            assertFalse(claims.toString().contains(credentials.instanceId))
            assertFalse(claims.toString().contains(credentials.instancePassword))
        }
        f.issuer.issueCredential(av, f.key(), requests[0])
        f.refused(AvsuaRefusalCause.BINDING, requests[0])
        other.issuer.issueCredential(av, other.key(), requests[2])
    }

    @Test
    fun `foreign AP context refuses with shared provider trust and holder key`() = runTest {
        val f = Fixture()
        // Same audience, provider trust and holder key: only AP transaction ownership differs.
        val other = Fixture(trustKey = { f.provider.avsuaTrustKey() })
        val request = f.request()
        val key = f.key()
        other.refused(AvsuaRefusalCause.BINDING, request, key)
        val otherContext = other.issuer.createAvIssuanceContext(f.holder)
        val otherRequest = AvIssuanceRequest(otherContext, f.provider.attestAvIssuance(otherContext, f.holder, credentials))
        f.refused(AvsuaRefusalCause.BINDING, otherRequest, key)
        f.issuer.issueCredential(av, key, request)
        other.issuer.issueCredential(av, key, otherRequest)
        assertEquals(1, f.mintKeyLoads)
        assertEquals(1, other.mintKeyLoads)
    }

    @Test
    fun `real batch forwards actual generated key evidence and rolls back all keys and rows on refusal`() = runTest {
        val f = Fixture()
        val keys = BatchKeys()
        val rows = Rows()
        var withhold = false
        var calls = 0
        val batch = WalletProviderBatchAgeIssuer(
            keys, f.provider, rows, { credentials },
            { credential, key, request ->
                calls++
                f.issuer.issueCredential(credential, key, if (withhold && calls % 3 == 2) null else request)
            },
            { f.issuer.createAvIssuanceContext(it) }, Dispatchers.Unconfined
        )
        val results = batch.issueBatch(av, 3)
        assertEquals(3, results.size)
        assertEquals(3, rows.rows.size)
        for (result in results) {
            val key = keys.live.getValue(result.keyAttestation.keyId).jwk()
            assertEquals(key.computeThumbprint(), result.keyAttestation.jwk.computeThumbprint())
            val bound = OneKey(com.upokecenter.cbor.CBORObject.DecodeFromBytes(MDoc.fromCBORHex(result.credential).MSO!!.deviceKeyInfo.deviceKey.toCBOR()))
            assertContentEquals(key.toECPublicKey().encoded, bound.AsPublicKey().encoded)
        }
        withhold = true
        assertEquals(AvsuaRefusalCause.MISSING_BUNDLE, assertFailsWith<AvsuaRefusal> { batch.issueBatch(av, 3) }.causeCode)
        assertEquals(3, keys.deleted.size)
        assertEquals(3, rows.rows.size)
        assertEquals(3, keys.live.size)
        assertTrue(keys.deleted.none { it in rows.rows })
    }

    @Test
    fun `evidence provider failure and cancellation roll back batch keys and rows`() = runTest {
        for (failure in listOf(IllegalStateException("provider unavailable"), CancellationException("provider cancelled"))) {
            val f = Fixture()
            val keys = BatchKeys()
            val rows = Rows()
            var evidenceCalls = 0
            val provider = object : WalletProviderService by f.provider {
                override suspend fun attestAvIssuance(context: AvIssuanceContext, issuanceKey: JWK, credentials: WalletInstanceCredentials): AvsuaEvidence {
                    evidenceCalls++
                    assertTrue(rows.rows.isNotEmpty(), "key registration precedes evidence generation")
                    throw failure
                }
            }
            val batch = IssuerModule().createWalletProviderBatchAgeIssuer(
                keys, provider, rows, { credentials }, f.issuer, Dispatchers.Unconfined
            )
            assertSame(failure, assertFailsWith<Exception> { batch.issueBatch(av, 3) })
            assertEquals(1, evidenceCalls)
            assertEquals(3, keys.deleted.size)
            assertTrue(keys.live.isEmpty())
            assertTrue(rows.rows.isEmpty())
            assertEquals(0, f.mintKeyLoads)
        }
    }

    @Test
    fun `EE-PoA still mints without AVSUA through actual service and batch`() = runTest {
        val f = Fixture()
        val keys = BatchKeys()
        val rows = Rows()
        val batch = WalletProviderBatchAgeIssuer(
            keys, f.provider, rows, { credentials },
            { credential, key, request -> assertNull(request); f.issuer.issueCredential(credential, key) },
            { fail("non-AV must not create an AV context") }, Dispatchers.Unconfined
        )
        val day = java.time.LocalDate.now().plusDays(14)
        val poa = Credential.EePoaCredential(true, "EE", "Demo issuer", kotlinx.datetime.LocalDate(day.year, day.monthValue, day.dayOfMonth))
        assertEquals(DocType.EE_POA.uri, MDoc.fromCBORHex(batch.issueBatch(poa, 1).single().credential).docType.value)
    }

    @Test
    fun `PID and mDL retain issuance without AVSUA`() = runTest {
        val f = Fixture()
        val date = kotlinx.datetime.LocalDate(2000, 1, 1)
        val expiry = kotlinx.datetime.LocalDate(2030, 1, 1)
        val status = Credential.LocationStatus("1", "https://demo.invalid/status")
        val pid = Credential.MdocPidCredential(
            familyName = "Demo", givenName = "Holder", birthDate = date, birthPlace = "EE",
            nationality = listOf("EE"), personalAdministrativeNumber = "demo", familyNameBirth = "Demo",
            givenNameBirth = "Holder", expiryDate = expiry, issuingAuthority = "Demo", issuingCountry = "EE",
            locationStatus = status, issuanceDate = date
        )
        val mdl = Credential.MdocMdlCredential(
            familyName = "Demo", givenName = "Holder", birthDate = date, issueDate = date, expiryDate = expiry,
            issuingCountry = "EE", issuingAuthority = "Demo", documentNumber = "demo", portrait = byteArrayOf(),
            drivingPrivileges = emptyList(), unDistinguishingSign = "EST", locationStatus = status
        )
        assertEquals(DocType.PID.uri, MDoc.fromCBORHex(f.issuer.issueCredential(pid, f.key()).credential).docType.value)
        assertEquals(DocType.MDL.uri, MDoc.fromCBORHex(f.issuer.issueCredential(mdl, f.key()).credential).docType.value)
    }

    @Test
    fun `both evidence tokens use one captured provider signing key`() = runTest {
        val f = Fixture()
        val context = f.issuer.createAvIssuanceContext(f.holder)
        val before = f.keys.reads
        val evidence = f.provider.attestAvIssuance(context, f.holder, credentials)
        assertEquals(before + 1, f.keys.reads, "load the provider key only once for both signatures")
        f.issuer.issueCredential(av, f.key(), AvIssuanceRequest(context, evidence))
    }

    @Test
    fun `trust supplier failure is named and supplier cancellation propagates`() = runTest {
        val failed = Fixture(trustKey = { throw IllegalStateException("trust store unavailable") })
        failed.refused(AvsuaRefusalCause.SIGNATURE_OR_TRUST, failed.request())
        val cancellation = CancellationException("cancel trust resolution")
        val cancelled = Fixture(trustKey = { throw cancellation })
        assertSame(cancellation, assertFailsWith<CancellationException> {
            cancelled.issuer.issueCredential(av, cancelled.key(), cancelled.request())
        })
        assertEquals(0, cancelled.mintKeyLoads)
    }

    @Test
    fun `default absent provider trust refuses before issuer key access`() = runTest {
        val f = Fixture()
        var loads = 0
        val issuer = CredentialIssuanceServiceMock(
            ContextWrapper(null), Dispatchers.Unconfined, UserPreferencesDataSource(Preferences()),
            issuerKeyLoader = { _, _ -> loads++; fail("untrusted request reached issuer keys") }
        )
        val context = issuer.createAvIssuanceContext(f.holder)
        val request = AvIssuanceRequest(context, f.provider.attestAvIssuance(context, f.holder, credentials))
        assertEquals(AvsuaRefusalCause.SIGNATURE_OR_TRUST, assertFailsWith<AvsuaRefusal> {
            issuer.issueCredential(av, f.key(), request)
        }.causeCode)
        assertEquals(0, loads)
    }

    @Test
    fun `token expiry at exact fixed time refuses each part and one second before succeeds`() = runTest {
        val now = Instant.parse("2026-10-23T12:00:00Z")
        for (instancePart in listOf(true, false)) {
            val f = Fixture(apClock = Clock.fixed(now, ZoneOffset.UTC))
            val request = f.request()
            val bundle = request.evidence!!
            fun dated(token: String, expires: Instant) = f.resign(token) {
                issueTime(Date.from(now.minusSeconds(60)))
                notBeforeTime(Date.from(now.minusSeconds(60)))
                expirationTime(Date.from(expires))
            }
            // The pending AP context is fresh; only this part's exp equals the verifier clock.
            val expired = AvsuaEvidence(
                dated(bundle.instanceAttestation!!, if (instancePart) now else now.plusSeconds(60)),
                dated(bundle.keyAttestation!!, if (instancePart) now.plusSeconds(60) else now)
            )
            f.refused(AvsuaRefusalCause.VALIDITY, request.copy(evidence = expired))
            val valid = AvsuaEvidence(dated(bundle.instanceAttestation!!, now.plusSeconds(1)), dated(bundle.keyAttestation!!, now.plusSeconds(1)))
            f.issuer.issueCredential(av, f.key(), request.copy(evidence = valid))
            assertEquals(1, f.mintKeyLoads)
        }
    }

    @Test
    fun `context expires at exact fixed time independently of valid token lifetimes`() = runTest {
        val start = Instant.parse("2026-10-23T12:00:00Z")
        val apClock = MutableClock(start)
        // Token time stays inside both signed parts' validity so AP expiry is the only refusal.
        val f = Fixture(apClock = apClock, verifierClock = Clock.fixed(start.plusSeconds(1), ZoneOffset.UTC))
        suspend fun request(): AvIssuanceRequest {
            val request = f.request()
            fun dated(token: String) = f.resign(token) {
                issueTime(Date.from(start)); notBeforeTime(Date.from(start)); expirationTime(Date.from(start.plusSeconds(300)))
            }
            return request.copy(evidence = AvsuaEvidence(dated(request.evidence!!.instanceAttestation!!), dated(request.evidence!!.keyAttestation!!)))
        }
        val expired = request()
        val valid = request()
        apClock.now = start.plusSeconds(299)
        f.issuer.issueCredential(av, f.key(), valid)
        apClock.now = start.plusSeconds(300)
        f.refused(AvsuaRefusalCause.VALIDITY, expired)
    }

    @Test
    fun `context expiring during trust resolution refuses before minting with still valid evidence`() = runTest {
        val start = Instant.parse("2026-10-23T12:00:00Z")
        val clock = MutableClock(start)
        lateinit var f: Fixture
        f = Fixture(apClock = clock, trustKey = {
            clock.now = start.plusSeconds(300)
            f.provider.avsuaTrustKey()
        })
        val request = f.request()
        // Refresh both tokens near the context deadline; they remain valid after that deadline.
        fun renewed(token: String) = f.resign(token) {
            issueTime(Date.from(start.plusSeconds(299)))
            notBeforeTime(Date.from(start.plusSeconds(299)))
            expirationTime(Date.from(start.plusSeconds(599)))
        }
        val evidence = AvsuaEvidence(renewed(request.evidence!!.instanceAttestation!!), renewed(request.evidence!!.keyAttestation!!))
        clock.now = start.plusSeconds(299)
        f.refused(AvsuaRefusalCause.VALIDITY, request.copy(evidence = evidence))
        assertEquals(start.plusSeconds(300), clock.instant())
    }

    @Test
    fun `expired context refuses before and after pruning without issuer key access`() = runTest {
        val start = Instant.parse("2026-10-23T12:00:00Z")
        val clock = MutableClock(start)
        val f = Fixture(apClock = clock)
        val request = f.request()
        clock.now = start.plusSeconds(300)
        f.refused(AvsuaRefusalCause.VALIDITY, request)
        f.issuer.createAvIssuanceContext(f.holder)
        f.refused(AvsuaRefusalCause.BINDING, request)
    }

    @Test
    fun `concurrent reuse allows exactly one request to reach issuer keys`() = runTest {
        val barrier = CyclicBarrier(2)
        lateinit var f: Fixture
        f = Fixture(trustKey = {
            barrier.await(10, TimeUnit.SECONDS)
            f.provider.avsuaTrustKey()
        })
        val request = f.request()
        val key = f.key()
        val results = List(2) {
            async(Dispatchers.Default) { runCatching { f.issuer.issueCredential(av, key, request) } }
        }.awaitAll()
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(AvsuaRefusalCause.BINDING, (results.single { it.isFailure }.exceptionOrNull() as AvsuaRefusal).causeCode)
        assertEquals(1, f.mintKeyLoads)
    }

    @Test
    fun `malformed type algorithm jti and demo marker controls refuse each part at actual boundary`() = runTest {
        val f = Fixture()
        val request = f.request()
        for (instancePart in listOf(true, false)) {
            val bundle = request.evidence!!
            val token = if (instancePart) bundle.instanceAttestation!! else bundle.keyAttestation!!
            val jwt = SignedJWT.parse(token)
            val controls = listOf(
                AvsuaRefusalCause.MALFORMED to "not-a-jwt",
                AvsuaRefusalCause.MALFORMED to f.resign(token) { claim("nonce", 42) },
                AvsuaRefusalCause.MALFORMED to f.resign(token) { claim("demo_only", "true") },
                AvsuaRefusalCause.BINDING to f.resign(token, JWSHeader.Builder(jwt.header).type(JOSEObjectType("wrong-type")).build()) {},
                AvsuaRefusalCause.SIGNATURE_OR_TRUST to f.resign(token, JWSHeader.Builder(JWSAlgorithm.PS256).type(jwt.header.type).build()) {},
                AvsuaRefusalCause.BINDING to f.resign(token) { jwtID(null) },
                AvsuaRefusalCause.BINDING to f.resign(token) { jwtID("") },
                AvsuaRefusalCause.INTEGRITY to f.resign(token) { claim("demo_only", null) },
                AvsuaRefusalCause.INTEGRITY to f.resign(token) { claim("demo_only", false) }
            )
            for ((cause, modified) in controls) {
                val evidence = if (instancePart) bundle.copy(instanceAttestation = modified) else bundle.copy(keyAttestation = modified)
                f.refused(cause, request.copy(evidence = evidence))
            }
        }
    }

    @Test
    fun `provider reset rejects old evidence and accepts new evidence for a pending context`() = runTest {
        val f = Fixture()
        val request = f.request()
        val key = f.key()
        val oldTrust = f.provider.avsuaTrustKey().computeThumbprint()
        f.keys.clearAll()
        assertNotEquals(oldTrust, f.provider.avsuaTrustKey().computeThumbprint())
        f.refused(AvsuaRefusalCause.SIGNATURE_OR_TRUST, request, key)
        val renewed = request.copy(evidence = f.provider.attestAvIssuance(request.context, f.holder, credentials))
        f.issuer.issueCredential(av, key, renewed)
        assertEquals(1, f.mintKeyLoads)
    }

    @Test
    fun `signing failure consumes context and retry requires fresh transaction`() = runTest {
        val f = Fixture()
        val request = f.request()
        val key = f.key()
        f.failSigning = true
        assertEquals("test issuer signing key unavailable", assertFailsWith<IllegalStateException> {
            f.issuer.issueCredential(av, key, request)
        }.message)
        assertEquals(1, f.mintKeyLoads)
        f.failSigning = false
        f.refused(AvsuaRefusalCause.BINDING, request, key)
        f.issuer.issueCredential(av, key, f.request())
        assertEquals(2, f.mintKeyLoads)
    }

    private class AssetLoadingReached : RuntimeException()

    private class CountingContext : ContextWrapper(null) {
        var assetLoads = 0
        override fun getAssets(): android.content.res.AssetManager {
            assetLoads++
            throw AssetLoadingReached()
        }
    }

    @Test
    fun `application factory configures provider trust before actual asset loader`() = runTest {
        val f = Fixture()
        val context = CountingContext()
        val issuer = IssuerModule().providesCredentialIssuanceService(
            Dispatchers.Unconfined, context, UserPreferencesDataSource(Preferences()), f.provider
        )
        val transaction = issuer.createAvIssuanceContext(f.holder)
        val request = AvIssuanceRequest(transaction, f.provider.attestAvIssuance(transaction, f.holder, credentials))
        assertFailsWith<AvsuaRefusal> { issuer.issueCredential(av, f.key()) }
        assertEquals(0, context.assetLoads)
        assertFailsWith<AssetLoadingReached> { issuer.issueCredential(av, f.key(), request) }
        assertEquals(1, context.assetLoads)
    }

    @Test
    fun `application batch factory forwards AV context and evidence to configured issuer`() = runTest {
        val f = Fixture()
        val keys = BatchKeys()
        val rows = Rows()
        val batch = IssuerModule().createWalletProviderBatchAgeIssuer(
            keys, f.provider, rows, { credentials }, f.issuer, Dispatchers.Unconfined
        )
        val result = batch.issueBatch(av, 1).single()
        assertEquals(1, f.mintKeyLoads)
        val mdoc = MDoc.fromCBORHex(result.credential)
        assertEquals(DocType.AGE_VERIFICATION.uri, mdoc.docType.value)
        val bound = OneKey(com.upokecenter.cbor.CBORObject.DecodeFromBytes(mdoc.MSO!!.deviceKeyInfo.deviceKey.toCBOR()))
        assertContentEquals(keys.live.getValue(result.keyAttestation.keyId).jwk().toECPublicKey().encoded, bound.AsPublicKey().encoded)
        assertEquals(setOf(result.keyAttestation.keyId), rows.rows.keys)
    }

    @Test
    fun `unsupported wallet provider explicitly refuses AV operation`() = runTest {
        val provider = WalletProviderServiceRpc("https://unavailable.invalid", Dispatchers.Unconfined)
        assertEquals(AvsuaRefusalCause.UNSUPPORTED_PROVIDER, assertFailsWith<AvsuaRefusal> {
            provider.attestAvIssuance(AvIssuanceContext("a", "b", "c"), Fixture().holder, credentials)
        }.causeCode)
    }

    companion object {
        private fun ecPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        private fun certificate(pair: KeyPair, algorithm: String): java.security.cert.X509Certificate {
            val name = X500Name("CN=local-demo")
            val cert = JcaX509v3CertificateBuilder(name, BigInteger.ONE, Date(0), Date.from(Instant.now().plusSeconds(3600)), name, pair.public)
                .build(JcaContentSignerBuilder(algorithm).build(pair.private))
            return JcaX509CertificateConverter().getCertificate(cert)
        }
    }
}
