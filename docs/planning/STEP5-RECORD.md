# STEP5-RECORD — Hardware-backed device keys (conformance plan §4 item 5)

Branch: `step5-hardware-keys`. multipaz pinned at `0.99.0`
(`gradle/libs.versions.toml:41`, `multipaz-android` as a direct
`implementation` dependency in `app/build.gradle.kts` ~line 312).

## What was done

`EncryptedKeyStoreManager`'s software BKS EC-key path is replaced by multipaz
`AndroidKeystoreSecureArea` for wallet device keys (finding F2):

- `SecureAreaKeyManager.kt` — generates EC P-256 keys inside Android Keystore
  through `AndroidKeystoreSecureArea.createKey` with
  `AndroidKeystoreCreateKeySettings` (`Algorithm.ESP256`,
  `setUseStrongBox(selection.useStrongBox())`, `setAttestationChallenge` via
  the settings-builder `ByteString` challenge). Signing goes through
  `AndroidKeystoreSecureArea.sign(keyId, data, Reason.Unspecified)`; the class
  exposes no method that can return private-key bytes — only
  `SecureAreaDeviceKey` (keyId, `EcPublicKey`, `X509CertChain` attestation,
  `HardwareBacking`) crosses its API. Key metadata (public half + attestation
  chain) is stored by multipaz in a private SQLite database
  (`secure_area.db`) in the app's files dir.
- `SecureAreaSelection.kt` — StrongBox when the device advertises
  `FEATURE_STRONGBOX_KEYSTORE`, TEE otherwise. The Android feature lookup is
  performed in `DeviceSecureAreaSelection` (Hilt-provided in
  `SecurityModule`); the pure decision logic is JVM-testable.
- `AttestationChallenge.kt` — `AttestationChallengeSource` with
  `MockAttestationChallengeSource` standing in for the (mock) wallet provider
  challenge.
- `SecureAreaCOSECryptoProvider.kt` — signs COSE `DeviceAuthentication`
  (deviceSignature) through the SecureArea key instead of walt.id's
  `SimpleCOSECryptoProvider`. The prover path needed no change (F2: it never
  touched the key).
- `LocalCryptoProvider` / `RemoteCryptoProvider` / `CryptoProvider` routed to
  the SecureArea key manager; the private-key accessor was dropped.
- Credentials held under BKS keys are re-issued, not migrated: importing a
  key into a secure area is an import, which EE-SEC-003 forbids. No migration
  code exists.
- User authentication (biometric gate) is §8.5 scope and is deliberately not
  configured on these keys.

## Deviations and notes

- `AndroidKeystoreSecureArea` exposes no alias listing in 0.99.0, so there is
  no bulk clear; `clearAll()` is a no-op and keys are deleted per-alias from
  the wallet's keyAttestation records.
- Hilt does not register a `suspend` `@Provides`, which silently dropped the
  `SecureAreaKeyManager` binding and failed `:app:hiltJavaCompileDebug` (only
  visible in the full unit-test build, not in `:app:compileDebugKotlin`).
  Fixed by `runBlocking` in `SecurityModule.providesSecureAreaKeyManager`,
  the same convention `ProximityModule` already uses for
  `SoftwareSecureArea.create`.

## PENDING-DEVICE (requires real hardware, not JVM-testable)

- Sign-and-verify loop on StrongBox EC keys (Pixel 8 anomaly recorded at
  `AndroidEncryptionManager.kt:75`): the loop is exercised on-device only;
  JVM unit tests cannot create Android Keystore/StrongBox keys. The JVM
  surface (StrongBox/TEE selection, attestation-challenge contract) is
  covered by `SecureAreaSelectionTest`.
- Attestation chain reporting `StrongBox` vs `TrustedEnvironment` root of
  trust on a real device (Pixel 8 StrongBox, TEE-only emulator/device).
- Key attestation against the (mock) wallet provider's challenge end to end
  (challenge bytes visible in the attestation extension).
- Proximity + remote presentation flows end to end with the SecureArea
  signature (`ProximityViewModel`, `DigitalCredentialsViewModel`).

## Verification

- `ANDROID_HOME=/opt/android-sdk ./gradlew :app:compileDebugKotlin --offline`
  — exit 0.
- `ANDROID_HOME=/opt/android-sdk ./gradlew :app:testDebugUnitTest --offline`
  — BUILD SUCCESSFUL: 19 tests, 0 failures, 0 errors (2 skipped as
  instrumented-only): SecureAreaSelectionTest 2/2, HolderObligationsTest
  14/14, EudiSdJwtTest 1/1, SampleTest + IssuanceTest skipped-by-assumption.
- `ANDROID_HOME=/opt/android-sdk ./gradlew :zk-conformance:test --offline`
  — BUILD SUCCESSFUL: AgeProofRoundTripTest tests=1 failures=0 (the ZK part
  of acceptance; the presentation path compiles in the app module above).
