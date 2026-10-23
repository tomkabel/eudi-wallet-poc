# Local AVSUA-equivalent issuance

The Android local mock AV issuance path sends provider-signed instance integrity
and key security evidence to its attestation provider before the provider mints
`eu.europa.ec.av.1`. Withholding either signed part refuses issuance with a named
`AVSUA refusal`. Evidence stays on the issuance interface: the returned mdoc has
only `age_over_18`, its normal MSO and issuer signature. No AVSUA is added to
credential attributes or relying-party presentations.

The primary requirements are the pinned public [Annex A.5](av/annex-a-normative.md)
("AVS Unit Attestation") and [spec §12](../../spec/EE-EUDIW-TS-1.0.md), especially
EE-WUA-007 and EE-WUA-008. A.5 describes an AVIA for instance integrity and a KA
for key security, both solution-provider signed, and permits reuse of WIA/KA.
The public [source pin](av/source.json) records revision
`014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`. This implementation uses that retained
source; it introduces no normative TS3 wire format.

## Flow and trust

1. `WalletProviderBatchAgeIssuer` creates the actual SecureArea batch keys and
   obtains the existing key attestation and key-registry row for each key.
2. For AV only, `CredentialIssuanceServiceMock.createAvIssuanceContext` creates a
   configured audience, fresh transaction ID and random challenge for that public
   key. The audience identifies the configured local attestation provider; transaction IDs
   and challenges are fresh for every key and issuance, including retries.
3. `WalletProviderServiceMock.attestAvIssuance` receives the public key, issuance
   context and mock credentials, and returns two JWTs signed by the provider's
   independent RSA key.
   Instance authentication is a demonstration assumption: this mock does not
   check the supplied account credentials. Account credentials, holder key aliases
   and certificate chains are omitted from the evidence. Both JWTs use a single
   captured provider signer/key snapshot.
4. The batch forwards `AvIssuanceRequest(context, evidence)` alongside the
   credential request and its existing key attestation. The AP checks its pending
   context and public-key binding, then verifies both JWT signatures against its
   explicitly configured provider public key, token type/issuer, audience,
   transaction, challenge, key thumbprint, lifetime and assertions. Token `x5c`
   headers never establish trust. Both the outer issuance method and public
   `issueMDocAgeVerification` helper use this guard before accessing issuer keys.
5. The AP checks context expiry again after evidence verification and consumes
   the pending context before signing, preventing replay or
   concurrent reuse. A signing failure requires a fresh transaction. Batch
   failures retain the existing cleanup: delete all batch keys and their rows,
   preserving rows for any keys that survive deletion. No partial result list is
   returned. PID, mDL and EE-PoA retain their existing issuance contract without
   AVSUA.

DI explicitly supplies `AvsuaVerifier` with the mock provider's public trust key.
The provider key lives in the mock BKS keystore and can rotate when account data
is cleared; the trust supplier follows that local lifecycle. Old evidence then
fails signature validation and new evidence can use the new key. Constructing a
mock AP without configured trust, or failure to resolve its configured trust key,
refuses AV issuance with `SIGNATURE_OR_TRUST`; coroutine cancellation propagates. Unsupported wallet providers
refuse the new operation with `UNSUPPORTED_PROVIDER`; the production RPC issuer
still supports its existing PID operations only. External `av-vci` Continue
retains the visible provider-unavailable behavior from
[Story 1.4](AV-VCI-INVOCATION.md).

## Local demonstration wire format

`AvIssuanceContext` holds `audience`, `transactionId` and `challenge`.
`AvsuaEvidence` holds separate compact-JWS `instanceAttestation` and
`keyAttestation` strings; null bundle or parts are explicit withholding controls.
The local JWTs use RS256 and `typ` values `local-demo-avia+jwt` and
`local-demo-ka+jwt`, respectively. Both contain:

| Claim | Local meaning |
|---|---|
| `iss` | `urn:ee:wallet:mock-provider`, a provider identifier, not a wallet/account identifier |
| `aud` | Attestation provider audience from the pending issuance context |
| `jti` | Independently random token identifier |
| `iat`, `nbf`, `exp` | Issued/not-before/expiry times; lifetime at most 300 seconds, no future issuance or not-before time and no expired token |
| `transaction_id`, `nonce` | Fresh transaction ID and challenge from the AP |
| `issuance_key_jkt` | RFC 7638 public-key thumbprint of the key used in this issuance |
| `demo_only` | Must be `true`; labels these assertions as demonstration inputs |

The instance token additionally requires `integrity_ok: true`; the key token
requires `key_security: "demo-assertion-only"`. The key thumbprint binds the
issuance public key without copying a holder certificate or stable account ID.
The normal batch generates a fresh holder key per credential. Reusing a public
key would naturally repeat that key's thumbprint; no account or wallet identifier
is added by this format.

Refusals expose an `AvsuaRefusal.causeCode`: `MISSING_BUNDLE`, `MISSING_INSTANCE`,
`MISSING_KEY`, `SIGNATURE_OR_TRUST`, `BINDING`, `VALIDITY`, `INTEGRITY`,
`KEY_SECURITY`, `MALFORMED` or `UNSUPPORTED_PROVIDER`. A context is accepted only
by the AP that created it, for its original public key and five-minute window.
An expired context still in pending storage refuses with `VALIDITY`. Creating a
new context prunes expired entries; submitting a pruned transaction then refuses
with `BINDING`, like any unknown transaction. Both outcomes prevent minting.

## Reproduction and scope

From the repository root, with JDK 17 and the Android SDK installed:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest :app:assembleLocal_mocks
```

For focused JVM coverage, add
`--tests ee.cyber.wallet.domain.provider.wallet.AvsuaIssuanceTest` to the unit-test
task alone. Tests execute the actual mock issuance service and decode its mdoc
docType and device key. Negative controls withhold the whole bundle and each
part, tamper each signature, supply an untrusted provider certificate, change
bindings, invalidate each token's lifetime and remove or negate integrity.
The same production batch loop runs with fakes for Android SecureArea storage
and Room, verifies evidence for the actual generated keys and checks rollback.
Fixed-clock expiry controls, concurrent reuse, provider reset, signing-failure
retry and actual application factory trust/forwarding are also exercised.
Controls isolate the pending context's original key binding from signed-token
key binding, reject another AP's transaction with shared trust and holder key,
check generated JOSE headers for identifiers, and exercise wrong-value/type key
security assertions. A clock advancing during trust resolution must refuse an
expired context even when both evidence tokens remain valid. Provider failure
and cancellation during evidence generation must roll back batch keys and rows.
Issuer-key loading is replaced with generated JVM test keys; mdoc construction
and signing execute the real service implementation.

This is offline local mock equivalence only. The mock provider asserts integrity
and key security as demo inputs; it does not inspect platform integrity or prove
hardware security. Its local trust key is a demonstration trust anchor. These
checks do not establish TS3 conformance, real wallet-provider interoperability,
status/revocation support, external issuance, scheme acceptance or certification.
Story 10.3 owns real wallet-provider interoperability. The Python issuer remains a
fixture generator and does not implement this Android path.

The [AV clause registry](AV-PROFILE-CLAUSE-REGISTRY.md) and
[runner](AV-CONFORMANCE-RUNNER.md) still execute Go probes only. A.5 remains an
explicit exclusion there; these separate JVM results must not be represented as
runnable registry mappings.
