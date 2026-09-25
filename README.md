> **This is an independent fork of [`open-eid/eudi-wallet-poc`](https://github.com/open-eid/eudi-wallet-poc), maintained outside RIA (Estonian Information System Authority). It is not RIA's app, is not affiliated with or endorsed by RIA or the Potential consortium, and reports to no Firebase project: its builds carry no Firebase Analytics or Crashlytics.**

<img src="app/src/main/assets/potential_logo.png" alt="Potential. For European Digital Identity. Co-funded by the European Union."  style="width: 400px;"/>
Funded by the European Union. Views and opinions expressed are however those of the author(s) only and do not 
necessarily reflect those of the European Union or Potential Consortium. Neither the European Union nor the granting 
authority can be held responsible for them.

# EUDI Wallet PoC (independent fork of EE Wallet PoC)

Proof of Concept EE Digital Identity Wallet application for Android.

The solution supports remote presentation flows for Personal Identification Data (PID) in SD-JWT and mDOC format and
Mobile Driving License (mDL) in mDOC format, plus a zero-knowledge proof-of-age presentation
(`mso_mdoc_zk` over OpenID4VP and the Android DC API) that discloses whether the holder is over
18 — and nothing else. See "Wallet-side ZK age-proof integration" below.

The issuance flow is based on
the [OpenID for Verifiable Credential Issuance Draft 14](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0-14.html)
and the presentation flow is based on the
[OpenID for Verifiable Presentations Draft 22](https://openid.net/specs/openid-4-verifiable-presentations-1_0-22.html).

## Testing using pre-built package

This fork publishes no APK. The release linked below is upstream's, and installs RIA's app (`ee.ria.wallet`), not
this fork; to run this fork, build it from source.

1. Download and install
   the [wallet application (the APK can be found under Assets)](https://github.com/open-eid/eudi-wallet-poc/releases).
2. Open the application and follow the instructions to create a new wallet and issue a Personal Identification Data
   (PID) and Mobile Driving License (mDL).
   credential. [Demo (link to instruction video)](https://github.com/user-attachments/assets/2ad34855-f81b-4595-8bc2-ed438670835e)
3. Go to the online Verifier service https://verifier.eudiw.dev.
   
   > NB! This verifier service is independent of this project and may not be available or may not be compatible at the
   time of your testing.
4. Insert the following [certificate](iaca/iaca_root.cer.pem) using the `Configure issuer chain`
   menu. [Demo (link to instruction video)](https://github.com/user-attachments/assets/2e0a8cf7-c951-4bd0-8a83-05fc5fce8962)
5. Select either or both `PID` (supported formats are `vc+sd-jwt` and `mso_mdoc`) and `mDL` credentials to
   verify. [Demo (link to instruction video)](https://github.com/user-attachments/assets/7e757d80-ee34-47e8-9435-db4cdbe86056)
6. Open the wallet link (or scan the generated QR code if its cross-device
   flow). [Demo (link to instruction video)](https://github.com/user-attachments/assets/70bf9f31-11c2-4342-8e44-55ec099af6c9)
7. Follow the instructions in the wallet application to present the requested
   credentials. [Demo (link to instruction video)](https://github.com/user-attachments/assets/0179c7bb-ebb6-4540-9998-ffec93df39e0)
8. Verify the presented credentials in the online Verifier service.

## Testing with other verifiers

It is possible to test with any verifier as long as a compatible protocol and credential formats are used and required
certificates are trusted by wallet and verifier.

1. The verifier certificate in PEM format must be added to [trusted.pem](/app/src/main/res/raw/trusted.pem).
2. The APK must be built according to the instructions below.
3. The wallet [certificate](iaca/iaca_root.cer.pem) must be trusted by the verifier.

For example, you can set up your own verifier using the
[reference implementation verifier service](https://github.com/eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint-23220-4-kt?tab=readme-ov-file#run-all-verifier-components-together).

> NB! This verifier implementation is independent of this project and appropriate version needs to be used, supporting
> the OpenID for Verifiable Presentations Draft 22+.

## Prerequisites

* JDK 17+
* [Android Studio](https://developer.android.com/studio) or [IntelliJ IDEA](https://www.jetbrains.com/idea) with
  [Android](https://plugins.jetbrains.com/plugin/22989-android) plugin
* Android SDK API 35

No credentials are needed to build: the signerry/aarmam fork artifacts resolve from
a public Maven mirror on gh-pages, seeded by `.github/workflows/mirror.yml`.

If you build from a command line be sure to have `ANDROID_HOME` env variable pointing to Android SDK folder.

## Quick-Start

For the development it is recommended to use IDE to run gradle tasks. The Android emulator in IDE can also be used to
run the app locally.

Alternatively, tasks can be run from the command line:

* `./gradlew build` - builds and assembles debug and release APKs
* `./gradlew assembleDebug` - builds and assembles only debug APK
* `./gradlew test` - run tests

Assembled APKs can be found here: `build/outputs/apk/`

> The app uses `local_mocks` as the default build variant. This build variant is used to run the app with all the
> backend services mocked. This is useful for development and testing purposes.

> The release build restricts ABIs to `arm64-v8a` and `x86_64`: the bundled Longfellow prover
> (`multipaz-longfellow`) ships `libzkp.so` for those two ABIs only. The wallet still installs
> on other devices, but those devices get plain mdoc presentations, not ZK ones.

> The ZK spine (issuer, holder, verifier, prover) has its own prerequisites — Go, a Rust
> toolchain, Python 3.12 with `cbor2` and `cryptography` — documented in
> [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md). The list above covers only the Android app.

## Wallet-side ZK age-proof integration

Beyond the imported spine, this fork wires zero-knowledge proof of age into the Android
wallet itself:

* **ZK presentations in the wallet.** The Longfellow prover runs in-app through
  `multipaz-longfellow` (`ZkPresenter`, proof generation off the main thread). Where the
  verifier's request carries a ZK age query, the wallet proves `age_over_18` without
  disclosing the birth date.
* **Presentation tiers and downgrade resistance.** Every presentation is labelled with a tier
  (`PresentationTier`, `HolderObligations`): zero-knowledge, plain mdoc, proof-failed or
  not-requested. On a device capable of proving, a failed proof refuses the presentation
  instead of silently falling back to the linkable plain mdoc; tier and linkable-row counts
  surface on the activity log.
* **DC API path.** Requests arriving through the Android Credential Manager are dispatched on
  their protocol field (`DcApiProtocol`), the registry registers docTypes only, disclosure is
  gated behind a settings switch, and the reader-auth certificate subject is shown on the
  consent screen after its signature is verified.
* **`mso_mdoc_zk` over OpenID4VP.** `DeviceRequestParser` parses the `mso_mdoc_zk` DCQL format
  into per-docType ZK system specs (`zk_system_type` id + circuit hash + params, ECDSA-only
  device auth), and the wallet emits a `vp_token` carrying the Longfellow proof. The carrier
  is written up field by field in
  [`docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`](docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md).
* **Key management through Android SecureArea.** Device keys live in the multipaz
  `AndroidKeystoreSecureArea` with a StrongBox/TEE selection policy, per-alias lifecycle
  (creation, attestation, deletion on wipe) and no private-key accessor; `deviceAuth` is signed
  by the SecureArea key.
* **EE-PoA issuance and consumption.** The mock provider issues the `ee.riik.poa.1`
  proof-of-age attestation in once-only batches (`WalletProviderBatchAgeIssuer`, validity
  anchored to the issuance day) and consumes one batch entry per presentation, after the
  response is built.
* **On-device measurement harness.** `ZkProofBenchmark` times approval-to-proof-ready and reads
  prover peak memory (`VmHWM`) for the plan §6 device rows; the table and its PENDING-DEVICE
  discipline live in [`docs/MEASUREMENTS.md`](docs/MEASUREMENTS.md).
* **ABI restriction** to `arm64-v8a` and `x86_64` (see Quick-Start above) is a consequence of
  the prover dependency.

Where the wallet stands against the specification, requirement by requirement:
[`docs/CONFORMITY.md`](docs/CONFORMITY.md).

## Repository layout

| Path | What it is |
|---|---|
| `app/` | The Android wallet (this fork's main development surface) |
| `spec/` | The EE-EUDIW technical specification (`EE-EUDIW-TS-1.0.md`) |
| `eudi-arf/` | The EUDI ARF checkout the specification's citations are checked against (pinned) |
| `issuer/`, `wallet/`, `verifier/`, `zk-age-poc/` | The imported ZK spine: Python issuer, Python holder, Go verifier over the Rust Longfellow runtime, Rust prover examples |
| `zk-conformance/` | JVM fixture tests: the ZK round trip, and the step 0 / 2a / 8-7 cross-checks against the verifier's fixtures |
| `docs/` | Conformity note, measurements, development guide, architecture, profiles, analyses and plan records — indexed in [`docs/README.md`](docs/README.md) |
| `demo/` | The Android age-proof demo recording runbook (`demo.sh`) |
| `tests/` | End-to-end (`e2e.sh`) and load (`load_test.py`) harnesses |
| `tools/` | Development helpers: `deeplink.sh` (takes the deeplink as an argument), `log-level.sh`, `proxy.sh` |
| `iaca/`, `statuslists/` | Issuer trust anchor and status-list fixtures (upstream) |

## Checks and CI

`.github/workflows/ci.yml` is the definition of green for the ZK spine: a `fast` job (Python
lint, Go vet and unit tests — no Rust toolchain) and an `e2e` job that builds the Rust
staticlib and prover and runs `tests/e2e.sh`. `.github/workflows/app.yml` runs the Android
app's unit tests (`:app:testLocal_mocksUnitTest`) on app or Gradle changes, kept apart so
Kotlin-only pushes don't pay for the Rust build. The signerry/aarmam fork artifacts resolve
from a public Maven mirror on the fork's gh-pages (published by
`.github/workflows/mirror.yml`), so the app build needs no credentials and fork PRs run
the same job without secrets. Running everything locally, including the `-Dzk.regenerate=true`
fixture regeneration flag, is documented in
[`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md); every test and check is catalogued in
[`tests/README.md`](tests/README.md).

## EE-EUDIW specification and ZK age-proof spine

[`spec/EE-EUDIW-TS-1.0.md`](spec/EE-EUDIW-TS-1.0.md) is an independent Estonian EUDIW technical
specification with a zero-knowledge proof-of-age profile. The Python issuer and holder, the Go
verifier over the Rust Longfellow runtime, and the prover examples that exercise it live in
`issuer/`, `wallet/`, `verifier/` and `zk-age-poc/`. All were imported from ee-eudiw at `e368fc1`.
The Go verifier sheds oversize or over-concurrent requests before reading bodies (a body-read
admission pool four times the verification limit) and bounds holder input — 20-byte fields,
cumulative CBOR budgets — before it reaches the prover's fixed-size slots, so hostile input is
an error rather than a crash.

> **Status: independent draft.** Not issued by RIA, the Ministry of Justice and Digital Affairs, or any
> Estonian public authority, and not affiliated with them.

Building, running and the checks that define green: [`docs/DEVELOPMENT.md`](docs/DEVELOPMENT.md).
Measured on the demo host ([`demo/README.md`](demo/README.md)): prove 6.1–6.5 s, verify 2.5 s,
proof ~360 KB (359,988 B). Where this wallet stands against the specification:
[`docs/CONFORMITY.md`](docs/CONFORMITY.md).
