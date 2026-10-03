# EUDI Wallet POC - Security & Code Quality Analysis Report

**Date:** 2026-10-04
**Scope:** Full repo — Android app (Kotlin, 181 files), Go OID4VP/ZK verifier (47 files), Python issuer/wallet/verifier, Rust FFI crate, ZK conformance/PoC code, build & CI config.
**Method:** 5 parallel independent code-reading passes, one per component, each reading full source (not pattern-grep) and current uncommitted diffs.

**This report supersedes the previous version of this file**, which only scanned the Android app with shallow pattern-matching and declared "no critical issues." That conclusion does not hold — see Critical findings below, several of which are in the previous report's own scope and were missed.

---

## Executive Summary

**Critical:** 3 (two in actively-edited, uncommitted code) · **High:** 4 · **Medium:** 7 · **Low:** 6

The Go verifier service and the Rust FFI/ZK-proof code are both unusually well-hardened — fail-closed checks, careful locking, documented fixes for prior defects. The Android app's core crypto/keystore code (`crypto/`, most of `security/`) is solid. The problems cluster in: (1) code currently being edited on this branch, (2) data layer error handling (swallowed exceptions, partial-failure cleanup), and (3) a few long-standing soft-fail security controls (revocation checking, reader trust).

---

## Critical

### 1. Digital Credentials registration leaks attribute data to the platform
**`app/src/main/kotlin/ee/cyber/wallet/domain/credentials/DigitalCredentialsRegistrar.kt:73-89`** (uncommitted)

The new inline CBOR builder in `registerCredentials()` adds a full `namespaces` map — every field's namespace URI and element name — to the payload registered with the platform's Digital Credentials matcher (Google Play Services / Chrome, running outside the wallet process). This directly contradicts the doc comments still present in the same file: the ARF OIA_08e note just above it ("the platform learns... docType — but never attribute names or values", L69-72) and the docstring on the now-orphaned `toRegistryDocTypes()`/`toCBORBytes()` helpers (L128-139, "nothing else... NO namespaces member"). The change leaks which specific attributes (e.g. `age_over_18`, `nationality`, `resident_address`) each stored credential holds to an out-of-process component — a privacy regression the file itself documents as forbidden. Also looks unfinished (`Add(it.name); Add("")`, single-byte `bitmap`).

### 2. Uncaught exception in ProximityViewModel kills the event loop / crashes on biometric cancel
**`app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt`** (uncommitted)

`onShareClicked` (~L293-365) and `handleRequestObject` (~L165) have no try/catch. `presentWithDeviceSignature` can throw `KeyLockedException` when the biometric prompt is cancelled or fails (per `PromptDialogHost`'s documented fail-closed contract). That exception propagates uncaught through `MviViewModel.subscribeToEvents()`'s bare `_event.collect` (`ui/mvi/MviViewModel.kt:54-60`), which has no exception boundary — this permanently kills the ViewModel's event-processing coroutine (Cancel/Retry stop responding) or crashes the app outright. `DigitalCredentialsViewModel.onShareClicked`, same screen family, already wraps the identical call in try/catch/finally and emits a proper error effect — the fix pattern exists two files away and just wasn't applied here.

### 3. DataStore writes swallow cancellation and discard I/O failures
**`app/src/main/kotlin/ee/cyber/wallet/data/datastore/UserPreferencesDataSource.kt`** (all setters, L21-109) and **`WalletInstanceCredentialsDataSource.kt`** (`updateCredentials`, `clearAll`, L16-28)

Both wrap `dataStore.updateData{}` in bare `runCatching{}` with no rethrow of `CancellationException`, and discard the `Result` entirely. A cancelled coroutine keeps running the write (breaks structured concurrency); a real I/O failure (disk full, corrupt proto) is silently dropped — callers believe the write succeeded. Sibling files in the exact same package, `AuthorizationStateDataSource.kt` and `UserSessionDataSource.kt`, already fixed this precise bug (documented in-code as "JVM-M1"). These two were missed.

---

## High

### 4. PIN lockout is trivially bypassed by process restart
**`app/src/main/kotlin/ee/cyber/wallet/security/PinVerifier.kt:46,96-97`**

Failed-attempt counters and lockout state live only in an in-memory `ConcurrentHashMap` on a process-wide singleton. Force-stopping the app, rebooting, or swiping it from recents and relaunching resets `failedAttempts`/`lockedUntil`, fully bypassing the 5-attempt/30-minute lockout. Acknowledged in-code as a known follow-up, but it is a real, easily exploited brute-force bypass on the PIN gate today.

### 5. Partial wipe on `deleteAllData()` can leave PIN/session data behind
**`app/src/main/kotlin/ee/cyber/wallet/data/repository/AccountRepository.kt:48-54`**

Only 3 of ~9 cleanup calls (`secureAreaKeyCleanup.deleteAll()`, `walletDatabase.clearAllTables()`, `encryptedKeyStoreManager.clearAll()`) are wrapped in `runCatching`. The rest — including `userSessionDataSource.clearAll()` (holds the PIN) and `userPreferencesDataSource.clearAll()` — are not. An exception partway through (e.g. `attestationDao.deleteAll()` throwing) aborts the remaining steps, leaving a "wiped" wallet with residual PIN/session state.

### 6. OAuth2/PKCE issuance state stored unencrypted
**`app/src/main/kotlin/ee/cyber/wallet/data/datastore/AuthorizationStateDataSource.kt`** + `di/AuthorizationStateDataSourceModule.kt`

Persists `AuthorizationRequestPrepared` (incl. PKCE verifier/state, via `util/Serializables.kt`'s Jackson+Base64 blob) in plaintext on disk. The sibling stores for PIN (`UserSessionDataSourceModule.kt`) and instance password (`WalletCredentialsDataSourceModule.kt`) explicitly wire `AndroidEncryptionManager` for the identical DataStore pattern. Looks like a missed case, not a deliberate choice.

### 7. Go verifier: `-max-credentials` config is silently ignored at response time
**`verifier/go/present.go:444`**

`presenter.check` calls `s.Query.Validate(0)`. Inside `DCQL.Validate`, `max <= 0` always falls back to `MaxCredentialsDefault` (4), ignoring `p.maxCredentials` — the field correctly populated from `-max-credentials` and used at session creation (`present.go:243`, `oid4vp/session.go`). If an operator raises `-max-credentials` above 4, sessions with 5+ credentials pass the real admission check but then **always fail at response time** with a misleading 500 "verifier configuration error." `p.maxCredentials` is stored for exactly this re-check and never read again — a genuine wiring bug, not a style issue.

---

## Medium

8. **`app/src/main/kotlin/ee/cyber/wallet/security/CertificateChainValidator.kt:143,150-176`** — Revocation checking soft-fails by design (`isRevocationEnabled = false`; manual CRL check only rejects when a CRL was both fetched and signature-verified). An attacker who can block/degrade access to the CRL distribution point (or simply waits out the already-expired in-tree CRL DP, per code comments) causes revocation checks to silently pass. Documented as intentional/temporary, but revoked certs are not currently rejected in practice.

9. **`app/src/main/kotlin/ee/cyber/wallet/domain/credentials/CredentialIssuanceServiceMock.kt:464-489`** — Mock issuer private keys (`doc_signer_pid_issuer.p12`, `doc_signer_mdl_issuer.p12`) loaded from app assets with hardcoded password `"changeit"`. Mock-only, but it's a real private signing key shipped inside the APK with a well-known password — confirm this class is excluded from release/production build variants.

10. **`app/src/main/kotlin/ee/cyber/wallet/di/ProximityModule.kt:21-25`** — `providesReaderTrustStore()` passes an empty `trustedCertificates` list. Reader authentication for mdoc/BLE proximity can never resolve a trusted chain — a functionally dead (fail-closed, but no-op) trust check.

11. **`app/src/main/kotlin/ee/cyber/wallet/data/repository/TransactionLogRepository.kt:16`** — `getTransactionLog(id: String) = logRecordDao.getById(id.toLong())`, unguarded `toLong()` throws uncaught `NumberFormatException` on malformed input.

12. **`app/src/main/kotlin/ee/cyber/wallet/domain/provider/{pid,wallet}/*ServiceRpc.kt`** — gRPC channel falls back to `usePlaintext()` whenever `rpcUrl`'s scheme isn't `https`; wallet-instance Basic-Auth credentials would be sent in the clear if `rpcUrl` is ever misconfigured to non-https. No code-level guard against that misconfiguration.

13. **`verifier/go/internal/jose/jwe.go:116-121`** (`aesKeyUnwrap`) — RFC 3394 integrity check breaks on the first mismatching byte (`for _, b := range a { if b != 0xA6 { return err } }`). Non-constant-time comparison of a MAC-like integrity register, exercised on every `direct_post.jwt` wallet response the verifier decrypts. Should use `crypto/subtle.ConstantTimeCompare`.

14. **`verifier/go/internal/jose/jwe.go:364-389`** (`concatKDF`) — `apv` is computed and passed by both `Encrypt` and `Decrypt` callers (comment: "apv = recipient's public point"), but the function hardcodes PartyVInfo's length to 0 and never appends it to `otherinfo` — dead parameter. Round-trips correctly today only because both sides silently agree on the no-op; the documented security property (binding KEK derivation to recipient identity) doesn't actually exist. Maintenance trap if either side is "fixed" independently later.

---

## Low

- **`app/src/main/kotlin/ee/cyber/wallet/security/EncryptedKeyStoreManager.kt:133`**, **`crypto/LocalCryptoProvider.kt:179`**, **`domain/provider/wallet/WalletProviderServiceMock.kt:171`** — BKS keystore entries protected with an empty-string per-entry password. Low risk since the whole BKS file is wrapped in AES-GCM via a non-exportable Keystore key, but per-entry protection is nil if the outer wrapper is ever defeated.
- **`verifier/go/main.go:427`** (`checkLongfellowRev`) — revision-pin path (`../zkverify-ffi/longfellow-rev.txt`) is relative to cwd. Launched from a different working directory (e.g. a systemd unit), `os.IsNotExist` makes the check return `nil` silently — a deployment misconfiguration can quietly disable a version-drift security control with zero log output.
- **`app/build.gradle.kts:98-109`** — `local_mocks` build type is `isDefault = true` with `USE_MOCKS=true`/`DEV=true` baked in; `release` is force-disabled via a `beforeVariants` gate. Known/intentional for the PoC stage; flag before this ever ships.
- **`eudi-arf/docker/Dockerfile:16`** — `FROM pandoc/extra:latest`, a floating tag not pinned by digest in the file itself (digest is captured downstream, per comment, but the committed Dockerfile isn't self-reproducible).
- **`eudi-arf/docker/Dockerfile:20`** — `USER root` set for `apk add` and never reverted; image runs as root from then on. Low impact (short-lived CI container).
- **`.github/workflows/cargo-deny.yml:30`** — `continue-on-error: true` means a known-vulnerable advisory in the Rust FFI dependency tree wouldn't fail CI today. Documented as an intentional, temporary baseline-building step.

---

## Clean / No Issues Found

- **Rust FFI crate** (`verifier/zkverify-ffi/src/lib.rs`) — every FFI entry point null-checks pointers, wraps logic in `catch_unwind` so panics can't cross the ABI boundary, `overflow-checks = true` in release specifically to catch the legacy padding-overflow class of bug. No unjustified `unsafe`.
- **`zk-age-poc/*.rs`** — predicate value proved is always the request-specified value, never blindly taken from the parsed attribute; both demos run negative controls (tampered proof, wrong issuer key) and assert failure; debug attribute dump gated behind an explicit opt-in env var.
- **`zk-conformance/`** — test-only, no skip/ignore markers bypassing verification.
- **Python** (`issuer/`, `wallet/`, `verifier/*.py`) — no unsafe deserialization, no hardcoded secrets, no SSRF/path-traversal exposure, thorough input validation (curve checks, mutual-exclusion, duplicate thresholds), device private keys written with `os.open(0o600)` + `fchmod` to avoid TOCTOU.
- **CI workflows** — least-privilege `permissions: contents: read` by default, `persist-credentials: false`, third-party actions pinned by commit SHA, write-token workflow never triggers on `pull_request`.
- **Go verifier core** — `oid4vp/session.go` (mutex-guarded store, idempotent shutdown), `oid4vp/trust.go` (signed trust store, append-only, freshness windows), `zk/zk.go` (circuit allowlist checked before FFI hashing, cgo buffers freed via defer), `internal/cborsub/cbor.go` (strict decoder with depth/item/byte budgets), `internal/jose/jwk.go`/`jws.go` (closed ES256 profile, `kid` never trusted for key selection).
- **Android crypto/security core** — `crypto/CryptoProvider.kt`, `RemoteCryptoProvider.kt`, `security/AesGcmStreamCodec.kt`, `AndroidEncryptionManager.kt`, `AndroidKeyStoreManager.kt`, `SecureAreaKeyManager.kt` (correct fail-closed cleanup/rollback ordering), `BouncyCastleHelper.kt`, `FetchLOTLCertificates.kt`/`LOTLInitializer.kt` (digest-pinned, fail-closed on missing/unsigned LOTL).
- **Android DI graph** (`di/*.kt`, 17 files) — no duplicate bindings, no circular dependencies, scoping consistent with actual lifetimes.
- **Room DAOs and migrations** — bind-param queries only, no injection surface; `MIGRATION_1_2` matches its schema JSON exactly.

---

## Priority

1. Fix items 1-3 before merging the current branch — they're in actively-edited code and two are outright regressions against the file's own documented invariants.
2. Items 4-7 (High) are pre-existing but real: PIN lockout bypass, partial-wipe data leakage, unencrypted PKCE state, and the Go `max-credentials` wiring bug.
3. Medium/Low items are tracked soft-fail controls (revocation, reader trust) and crypto hygiene (constant-time compare, dead KDF parameter) — worth scheduling, none are urgent regressions.

---

## Addendum — second pass (2026-10-04)

Five more parallel read-only reviews (app core, app UI, verifier/issuer/wallet, build/CI, tests/docs). Not yet manually verified. Findings already above (registrar namespace leak = #1, partial wipe = #5) are not repeated.

### High
- **`app/.../data/datastore/UserSessionDataSource.kt:17-23`** — `updatePin(pin: String)` stores the PIN in plaintext, while `PinVerifier` hashes it with Argon2. If anything still calls it, the plaintext copy defeats the hashing.
- **`verifier/go/oid4vp/trust.go:212-236`** — one issuer with a past `not_after` (or a future `not_before`) makes the whole signed trust store fail to load, so the verifier won't start. The documented way to retire an issuer is to add `not_after`, so following that rule breaks the verifier.
- **`verifier/go/present.go:138-167`** — the signed request object is served together with its own verification key, and nothing ties that key to `client_id` (`x509_san_dns`). Anyone who can rewrite the response can re-sign it.

### Medium
- **`verifier/go/main.go:539`** — issuer validity windows are checked only at startup, so a long-running verifier keeps trusting expired issuers. `LoadSignedTrustStore(prior=nil)` means the append-only check never runs. Once that is fixed, `trust.go:192-200` compares lowercased keys against raw hex, so a store written in uppercase hex would show every issuer as removed.
- **`verifier/go/present.go:152-157`** — the verifier writes the kid onto the shared `requestKeyPublic` on every request with no lock, which is a data race. The kid is also never set on the signing key, so the JWS header kid can be empty while the attached jwk carries one.
- **`app/.../domain/presentation/DcqlRequestProcessor.kt:170`** — in the mdoc path, `isRequired = intentToRetain || values != null` treats "verifier will keep this" as "claim is required". The SD-JWT path (L305) doesn't, so the two disagree. Missing claims are only logged as a warning, and `values` constraints are never compared against the actual value.
- **`app/.../data/repository/AccountRepository.kt:45-46`** — the `runCatching` results of `clearAllTables()` and `encryptedKeyStoreManager.clearAll()` are discarded, so the wipe reports success even if either fails.
- **`AndroidManifest.xml` + `ui/navigation/app/WalletApp.kt:91`** — any app can trigger the 8 unverified custom-scheme deep links, and `navigate(deeplink)` opens any destination they name. Only https uses `autoVerify`.
- **`ui/screens/dcapi/DigitalCredentialsActivity.kt:38`** — `LaunchedEffect(Unit) { processRequest(intent) }` reruns when the activity is recreated (e.g. rotation), so the request is processed again mid-flow.
- **`ui/prompt/PromptDialogHost.kt:61-73`** — each `launch()` adds a `dialogState` collector on `lifecycleScope` that is never cancelled, which could show the biometric prompt twice.
- **`WalletApplication.kt:84-92`** (uncommitted) — the prompt host is only cleared when the activity is destroyed, not when it is paused, so a prompt can launch against a backgrounded activity.
- **`.gitignore` / `gradle.properties:33-36`** — nothing ignores `*.jks` or `*.keystore`, and release signing points at an in-repo `release.keystore` in a public repo, so a plain `git add` would commit a real release key. The committed value also means the `RELEASE_KEYSTORE_FILE missing` guard in `app/build.gradle.kts:68` can never fire.
- **`.github/workflows/mirror.yml`** — `contents: write` applies to the whole workflow, the checkout keeps its credentials (no `persist-credentials: false`) while `fetch.py` runs, and `peaceiris/actions-gh-pages@v4` is pinned by tag. That action pushes the Maven mirror the app build trusts.
- **`.github/workflows/ci.yml:7-23`** — the path filters skip `scripts/**`, `tools/**` and `Makefile`, so changes there never run shellcheck or e2e.
- **Dead doc links** — `docs/CONFORMITY.md:10` and `docs/ARCHITECTURE.md:406` point to a missing `EUDI-WALLET-POC-CONFORMANCE-PLAN.md`. `docs/ARCHITECTURE.md:372` points to a missing `CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md`. `docs/MEASUREMENTS.md:306` points to a nonexistent `androidTest` runner.
- **Placeholder tests** — `IssuanceTest.kt:34` is `@Ignore`d and its body is commented out. `SampleTest.kt` says there are two ignored tests; there is one.

### Low
- `DigitalCredentialsRegistrar.kt:98-117`: registration failures log only at info level, and L100 is missing a separator (`"failed$it"`). `toRegistryDocTypes()`/`toCBORBytes()` are now dead code.
- `security/LOTLInitializer.kt:122`: any LOTL setup failure is swallowed and the wallet silently falls back to static trust anchors.
- `values-ru/strings.xml`: 4 of 213 strings translated.
- `tests/README.md:164`: says `zkverify-ffi` has no tests, but `lib.rs:197` has `#[cfg(test)]`. The line refs at L41/L43 are stale.
- `wallet/present.py:150-158`: crashes with `KeyError` when signed requests are on, and never checks the JAR signature.
- `jose/jws.go:94-130`: `VerifyJWSWithKey` accepts either `typ` (`JWT` or `trust-store+json`), so a request object could pass as a trust store if the two keys were ever shared.
- `tools/proxy.sh`, `tools/log-level.sh`: no `set -e`. `Makefile:23` reads the Longfellow rev without stripping whitespace (CI strips it), and `make e2e` doesn't install the Python packages it needs.
- Workflow `actions/*` steps are pinned by tag, not SHA.
