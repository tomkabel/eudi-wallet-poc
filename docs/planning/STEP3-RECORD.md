# Step 3 record — holder UI obligations

Fork: `/home/notroot/Documents/eudi-wallet-poc`, branch `step3-holder-obligations`
(base `ba5ba2d` = step1-fork-identity). Plan: §4 step 3 of
`docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` in `ee-eudiw`. Everything below is in the
fork repo unless a path says otherwise.

## 3a — the EE-ZKP-042 notice

The user is told, before sharing, that the presentation about to be made can be linked by the
issuer. The wording differs by cause: the relying party asked for a proof the wallet cannot give
(`presentation_zk_notice_proof_requested`) versus never asked for one at all
(`presentation_zk_notice_not_requested`).

- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/HolderObligations.kt` — pure decision
  core: `expectedPlainTier(zkCapable, proofRequested, satisfiable)` returns the linkable tier the
  presentation would fall back to, or null when a ZK proof will be produced.
- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/HolderObligations.kt` —
  `PresentationTier.zkNoticeRes()` picks the wording from the tier.
- `app/src/main/kotlin/ee/cyber/wallet/ui/screens/dcapi/DigitalCredentialsViewModel.kt` —
  `expectedPlainTier(specs, credentials)` maps multipaz `ZkSystemSpec`s onto the decision core and
  stores the result in `DcUiState.expectedPlainTier` as soon as a match exists, before any share.
- `app/src/main/kotlin/ee/cyber/wallet/ui/screens/dcapi/DigitalCredentialsScreen.kt` —
  `DcPresentationContent` renders the notice under the footer note whenever
  `expectedPlainTier != null`, i.e. strictly before the Share button can act.
- Strings: `values/strings.xml` + `values-et/strings.xml`, `presentation_zk_notice_*` (both en and
  et).

The notice fires on `PLAIN_NOT_REQUESTED`, `PLAIN_NO_MATCHING_CIRCUIT` and
`PLAIN_DEVICE_INCAPABLE` — every linkable case. When a ZK proof will be produced, no notice is
shown and nothing linkable is shared.

## 3b — the EE-ZKP-051 refusal, strict reading

Strict reading implemented for the age doctypes: a wallet that is ZK-capable in general but
cannot satisfy any advertised spec refuses the plain path instead of falling back leniently. The
lenient fallback is explicitly not implemented: an advertisement of a circuit hash no wallet
holds must not work as a downgrade lever.

- `DigitalCredentialsViewModel.kt` — `CredentialType.requiresZkProof()` scopes the refusal to the
  age doctypes (this fork has exactly one: `AGE_VERIFICATION`, `eu.europa.ec.av.1`).
- `DigitalCredentialsViewModel.kt` — `refusePlainWhenSpecsUnsatisfiable(...)`, called first thing
  in `onShareClicked` before anything is signed: delegates the decision to
  `HolderObligations.refusePlainFallback`, then logs a transaction row with
  `tier = PLAIN_NO_MATCHING_CIRCUIT` **and** `error = AppError.PRESENTATION_NO_MATCHING_CIRCUIT`,
  sets `DcUiState.plainRefused`, and sends `DcEffect.RefusedPlainFallback`.
- `app/src/main/kotlin/ee/cyber/wallet/domain/AppError.kt` — new distinct refusal error
  `PRESENTATION_NO_MATCHING_CIRCUIT` (F8-style distinguishable).
- `DigitalCredentialsScreen.kt` — `RefusedPlainContent`: distinct refusal state showing
  `error_presentation_no_matching_circuit` with a Close button; no share path is offered.
- `DigitalCredentialsActivity.kt` — the new effect is handled (logged; UI shows the refusal state).
- Device genuinely not ZK-capable (`zkSystem == null`) or party did not ask (`zkSystemSpecs`
  empty): plain with the 3a notice stays allowed — `refusePlainFallback` returns false for both.

Strings: `error_presentation_no_matching_circuit` (en + et).

## 3c — EE-ZKP-053 tier on every path, and the count

- Redirect path: `app/src/main/kotlin/ee/cyber/wallet/ui/screens/presentation/PresentationRequestViewModel.kt`
  — `logTransaction` now passes `tier = PresentationTier.PLAIN_NOT_REQUESTED` on every row it
  writes. That is literally true: the redirect path cannot receive a ZK request.
- Proximity path: `app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt`
  — after `transferManager.sendResponse`, each presented credential is logged with the disclosed
  attributes and `tier = PLAIN_NOT_REQUESTED` (a proximity reader cannot carry a ZK request
  either), party `log_entry_proximity_party` ("Proximity reader" / "Lähivoo lugeja"). Proximity
  rows did not exist in the log at all before this.
- `app/src/main/kotlin/ee/cyber/wallet/ui/screens/activity/ActivityLogScreen.kt` — the screen
  shows `activity_log_tier_summary` ("Presentations that can be linked to you by the issuer: N.
  Presentations the issuer cannot link: M."), computed only over rows that carry a tier so
  pre-tier rows do not skew it. Each row also shows a `tierLabel()` line.
- `app/src/main/kotlin/ee/cyber/wallet/ui/screens/documents/Extensions.kt` —
  `PresentationTier.tierLabel()` maps the tier to its string.
- Strings: `activity_log_tier_summary`, `tier_zero_knowledge`, `tier_plain_not_requested`,
  `tier_plain_no_matching_circuit`, `tier_plain_device_incapable` (en + et).

There is no `ActivityLogViewModel` count logic to unit test — the existing view model is a
pass-through (`transactionLogs` flow); the count is a pure function tested as
`HolderObligations.countLinkable`.

## 3d — copy audit (EE-ZKP-002, EE-POA-020, EE-POA-023)

Greps over both `values/` and `values-et/`: `anonym|unlink|privac|Priva|privacy`,
`anonüüm|jälgi|jäljetu|privaats|jäävad|ainult`, `document|dokument|equivalen|võrdväär|gleich`,
`qualified|qualif|kvalit`. All 250 lines of both files read in full.

Findings:

- No string claims anonymity or unlinkability of presentations. No string presents the EE-PoA as
  qualified (EE-POA-020: the age card carries no qualification wording anywhere).
- EE-POA-023 finding: `doc_type_age_verification_description` called the EE-PoA a "document"
  ("Age verification document" / "Age verification document") — exactly the presentation as a
  legally equivalent document the requirement forbids. Rewritten as an attestation.
- Two of the strings this step itself introduced initially over-claimed (caught in self-audit
  before commit): a bare "not linkable / not linkable" without naming who cannot link. EE-ZKP-002
  is about claims that hold on every build; a linkability claim scoped to the issuer is the one
  this fork can stand behind (the verifier always receives issuer signatures on the plain path,
  and the issuer always sees its own signature on ZK proofs of its circuits). Rewritten.

Every changed string, before → after:

| Name | Before (en) | After (en) |
|---|---|---|
| `doc_type_age_verification_description` | Age verification document | Electronic age attestation |
| `activity_log_tier_summary` | Presentations that can be linked to you by the issuer: %1$d. Presentations that cannot be linked: %2$d. | Presentations that can be linked to you by the issuer: %1$d. Presentations the issuer cannot link: %2$d. |
| `tier_zero_knowledge` | Zero-knowledge proof (not linkable) | Zero-knowledge proof (not linkable by the issuer) |

| Name | Before (et) | After (et) |
|---|---|---|
| `doc_type_age_verification_description` | Age verification document (untranslated English in the et file) | Elektrooniline vanusetõend |
| `activity_log_tier_summary` | Esitused, mille väljaandja saab sinuga siduda: %1$d. Esitused, mida pole võimalik siduda: %2$d. | Esitused, mille väljaandja saab sinuga siduda: %1$d. Esitused, mida väljaandja siduda ei saa: %2$d. |
| `tier_zero_knowledge` | Nullteadmise tõend (ei ole sidustatav) | Nullteadmise tõend (väljaandja ei saa siduda) |

New strings introduced by this step (en + et, audited in the same pass):
`presentation_zk_notice_proof_requested`, `presentation_zk_notice_not_requested`,
`error_presentation_no_matching_circuit`, `tier_plain_not_requested`,
`tier_plain_no_matching_circuit`, `tier_plain_device_incapable`,
`activity_log_tier_summary`, `log_entry_proximity_party`.

## 3e — DC API rows record what was shared (F9)

`DigitalCredentialsViewModel.onShareClicked` now builds a `kotlinx.serialization.json.JsonObject`
of the checked fields (`name -> value`) per credential and passes it to
`transactionLogRepository.addTransactionLog(attributes = ...)`, so DC API rows carry the same
disclosed attributes redirect rows already record. Proximity rows do the same (see 3c).

## Tests

`app/src/test/kotlin/ee/cyber/wallet/domain/presentation/HolderObligationsTest.kt` — JVM unit
tests over the pure decision core:

- EE-ZKP-051 strict refusal: refuse on unsatisfiable advertised specs; allow when satisfiable;
  allow when the device cannot prove at all (EE-ZKP-050 fallback stands); allow when the party
  never asked.
- EE-ZKP-042 wording: requested wording for `PLAIN_NO_MATCHING_CIRCUIT`/`PLAIN_DEVICE_INCAPABLE`;
  not-requested wording for `PLAIN_NOT_REQUESTED`; expected tier null when a proof will be
  produced; expected tier mirrors the refusal for unsatisfiable specs.
- EE-ZKP-053 count: linkable/unlinkable split; rows without a tier not counted; empty log is
  0/0.
- Strongest-circuit rule: highest version among allowed circuits for the attribute count; no
  match when the wallet holds none of the advertised circuits; attribute count must match.

Run: `./gradlew :app:testDebugUnitTest --tests 'ee.cyber.wallet.domain.presentation.HolderObligationsTest'`

Results: see "Verification" below.

## Verification

On this host (CachyOS, `ANDROID_HOME=/opt/android-sdk`):

- Warm-up pass: `./gradlew :app:compileDebugKotlin` — first run needed the network
  (3m 39s with dependency resolution); exit 0. Subsequent runs use the warm cache.
- After all step 3 changes: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests
  'ee.cyber.wallet.domain.presentation.HolderObligationsTest'` — `BUILD SUCCESSFUL in 32s`;
  `HolderObligationsTest` XML: `tests="14" skipped="0" failures="0" errors="0"`.
- Full suite: `./gradlew :app:testDebugUnitTest :zk-conformance:test` — `BUILD SUCCESSFUL in
  38s`; the pre-existing `AgeProofRoundTripTest > ageOver18ProofVerifiesAndTamperingIsRejected
  PASSED`, so the ZK round trip still verifies and tampering is still rejected.
- One intermediate failure was fixed before commit: the first test compile missed the
  `ee.cyber.wallet.R` import (Unresolved reference 'R' ×3) — no main-source error at any point.

## Deviations and PENDING-DEVICE

- **ZK round trip not exercised end to end on this host.** The prover needs `libzkp.so` on an
  arm64-v8a/x86_64 device or emulator plus a ZK-capable verifier terminal; the JVM unit tests
  cover the decision logic, the existing `zk-conformance` module covers the proof round trip
  offline. PENDING-DEVICE: 3a notice rendering, 3b refusal screen, 3c counter with real rows, and
  a real `zkSystemSpecs` advertisement driving `PLAIN_NO_MATCHING_CIRCUIT`.
- **"Age doctypes" plural.** The plan's strict reading is scoped to the age doctypes; this fork
  has exactly one (`AGE_VERIFICATION`, `eu.europa.ec.av.1`). `requiresZkProof()` is the single
  place to extend when a second one lands.
- **Strict refusal is per-request, not per-document.** If a request mixes the age doctype with
  other doctypes and the advertised specs are unsatisfiable, the whole presentation is refused
  (nothing is shared) rather than partially proving the non-age documents. Refusing everything is
  the conservative reading; mixed requests are not produced by the PoC verifier.
- **multipaz 0.99.0 API gaps.** `ZkSystemSpec` exposes params generically (`getParam`), so spec
  fingerprints are read via the same string keys (`circuit_hash`, `num_attributes`, `version`) the
  pre-existing `matchZkSystemSpec` used; no typed accessor exists in 0.99.0. The tier computation
  happens per response (per `onShareClicked`/per document), matching plan item 4c's "tier is
  computed per response, not per credential".
- **Gradle offline mode.** First resolution on this host needed the network (daemon warm-up and
  missing cache entries); after the first successful `:app:compileDebugKotlin` the cache is warm.
  If a later run fails offline, run it online — no cache-repair rabbit hole was entered beyond
  the one warm-up pass.
