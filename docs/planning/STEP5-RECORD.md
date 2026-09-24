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
- Review finding 4: the mock wallet provider signs every key attestation with
  its OWN provider key (`WalletProviderServiceMock.PROVIDER_KEY_ALIAS`, an RSA
  key generated once into the same BKS keystore, matching how
  `CredentialIssuanceServiceMock` signs issued credentials) — never with the
  attested holder key's JWK. Nimbus cannot create a signer for a public-only
  EC JWK, and even if it could, that would need the holder private key in the
  wallet process, which EE-SEC-003 and this step forbid. `attestKey` refuses a
  private or symmetric JWK outright, so the attestation path requires no
  holder private-key material by construction (`WalletProviderServiceMockAttestationTest`
  proves it on the JVM; the JWS header alg/x5c follow the signer).
- Review finding 5: `AccountRepository.deleteAllData` now really deletes the
  SecureArea EC keys. `SecureAreaKeyCleanup` maps the EC rows of the
  `key_attestations` table (the alias registry; one row per generated key,
  inserted by `LocalCryptoProvider.generateSecureAreaKey`) onto per-alias
  `SecureAreaKeyManager.deleteKey` calls, then wipes the registry — all before
  `clearAllTables()` drops the same table. Runs behind the
  `SecureAreaKeyDeleter` seam (JVM-tested with a recording fake in
  `SecureAreaKeyCleanupTest`; the repository itself needs a database context).
  `deleteKey` is best-effort (it swallows per-key failures), so one broken
  alias cannot abort the wipe.

## Deviations and notes

- `AndroidKeystoreSecureArea` exposes no alias listing in 0.99.0, so there is
  no true bulk clear; `SecureAreaKeyManager.deleteAllKeys()` is a best-effort
  no-op sweep and the actual deletion is per-alias, driven by the wallet's
  keyAttestation records through `SecureAreaKeyCleanup` (review finding 5).
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
- The SecureArea half of review finding 5: that `SecureAreaKeyCleanup`'s
  per-alias `deleteKey` really removes Android Keystore keys on a device —
  the JVM test pins the mapping and ordering against a recording fake, but
  `AndroidKeystoreSecureArea.deleteKey` on real (StrongBox/TEE) keys, the
  `secure_area.db` metadata rows dying with the wipe, and deleteAllData on a
  device overall are PENDING-DEVICE.
- Proximity + remote presentation flows end to end with the SecureArea
  signature (`ProximityViewModel`, `DigitalCredentialsViewModel`).

## Verification

On this host (CachyOS, `ANDROID_HOME=/opt/android-sdk`):

- `ANDROID_HOME=/opt/android-sdk ./gradlew :app:compileDebugKotlin --offline`
  — exit 0 (re-verified after the review-fix commits).
- `ANDROID_HOME=/opt/android-sdk ./gradlew :app:testDebugUnitTest --offline`
  — BUILD SUCCESSFUL: 37 tests, 0 failures, 0 errors (2 skipped as
  instrumented-only): SecureAreaSelectionTest 2/2, HolderObligationsTest
  24/24 (incl. review-findings 6 and 7),
  WalletProviderServiceMockAttestationTest 4/4 (review finding 4),
  SecureAreaKeyCleanupTest 4/4 (review finding 5), EudiSdJwtTest 1/1,
  SampleTest + IssuanceTest skipped-by-assumption.
- Full rerun from clean: `./gradlew :app:testDebug :zk-conformance:test
  --offline --rerun-tasks` — BUILD SUCCESSFUL; 38 tests total across app and
  zk-conformance, 0 failures, 0 errors (2 skipped as instrumented-only).
- `ANDROID_HOME=/opt/android-sdk ./gradlew :zk-conformance:test --offline`
  — BUILD SUCCESSFUL: AgeProofRoundTripTest tests=1 failures=0 (the ZK part
  of acceptance; the presentation path compiles in the app module above).
