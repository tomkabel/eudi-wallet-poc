# Step 4 record — protocol and registry hygiene

Fork: `/home/notroot/Documents/eudi-wallet-poc`, branch `step4-protocol-hygiene`
(rebased onto `fork/master` `f281010`, which carries the step 3 and step 5 review fixes; first
written on `98f880c` = step5-hardware-keys). Plan: §4 item 4 of
`docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` in `ee-eudiw`. Everything below is in the
fork repo unless a path says otherwise. One commit per sub-item, in plan order:

| Commit | Item |
|---|---|
| `b732edf` | 4a — dispatch the DC API request on its `protocol` field |
| `2242fa9` | 4b — register docTypes only, gate DC API disclosure behind a setting |
| `7da6580` | 4c — the EE-ZKP-004 presentation interface |
| `38cc01f` | 4d — readerAuth certificate subject on the consent screen |
| `99692be` | 4e — `androidx.credentials` SNAPSHOT → 1.6.0 stable (the only version change) |

Unit tests: 51 (37 on the rebased base, +14 here), 0 failures, 2 skipped as on the base
(`SampleTest`, `IssuanceTest`); `:app:compileDebugKotlin` +
`:app:testDebugUnitTest` green before each commit; full `:app:testDebug` and `:zk-conformance:test`
green at the end (§6 below).

## 4a — dispatch on `protocol`

`DigitalCredentialsViewModel.processRequest` never read the `protocol` field: it took
`requests[0]` and fed `data` straight to the payload parser, so a request speaking a protocol the
wallet does not answer crashed somewhere inside the parser as a bare `JSONException` (plan F8,
finding at plan §3 "state" row for `EE-PRO-013`).

- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/DcApiProtocol.kt` (new) —
  `DcApiProtocol` (the supported set) and `DcApiRequestDispatch.dispatch(protocolNames)`, a pure
  JVM-testable walk over the `requests[]` entries' `protocol` fields. It returns
  `Take(index, protocol)` for the first supported entry, `Unsupported(protocolName)` when no
  entry is supported (name preserved for the error), `Empty` for an empty list. A missing
  `protocol` field is unsupported, not guessed from payload bytes.
- `DigitalCredentialsViewModel.kt` — `requests[]` is walked by `DcApiRequestDispatch` before any
  payload is touched; the unsupported/empty cases send a specific `DcEffect.Error` and return.
  Only the payload of the first supported entry is parsed.
- Strings: `error_presentation_unsupported_protocol` (en + et).

Deviations:

- The supported set is `org-iso-mdoc` ONLY. The first draft also listed `openid4vp` as supported;
  the new dispatch test failed immediately (`Take(0)` for an `openid4vp` entry, whose payload the
  Annex C parser cannot read — the very crash this item removes, one layer down). OpenID4VP over
  the DC API is §8.2 future work, so `openid4vp` is refused as unsupported until it lands. The
  unit test `refuses openid4vp as unsupported until section 82 work lands` pins this.
- F8's distinguishable-error mechanism (`PendingIntentHandler.setGetCredentialException` versus
  `RESULT_CANCELED`) is still NOT implemented — refusals remain indistinguishable from a user
  cancellation at the Activity boundary (`DigitalCredentialsActivity` ends `DcEffect.Error` with
  `setResult(RESULT_CANCELED)`). Step 4a delivers the specific error inside the wallet (distinct
  from `JSONException`, distinct from the EE-ZKP-051 circuit refusal); the wire-level
  distinguishability remains open, recorded as a PENDING-DEVICE/§8 item below.

PENDING-DEVICE: none for 4a itself; the refusals-look-like-cancellation gap is a code-level item
carried for the F8 work, testable only once a relying-party harness drives the DC API end to end.

## 4b — registry per ARF OIA_08e/08f

Before: `DigitalCredentialsRegistrar.toCBORBytes()` registered, for every mdoc, its docType AND
each element's name and value (`namespaces` map) with Credential Manager on every issuance — the
holder's name, birthdate and isikukood reached the platform (plan F5, OIA_08e
`EW-PIO-01-018`). There was no off-switch (OIA_08f `EW-PIO-01-019`).

- `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/RegistryDocType.kt` (new) — the
  registration payload: `id` + `docType` per document, nothing else. The matcher matches on
  docType alone; the wallet does claim-level matching after launch, so the wallet may appear in
  the picker for requests it cannot fully answer — accepted by the ARF note to OIA_08e.
- `DigitalCredentialsRegistrar.kt` — `toCBORBytes()` now encodes the `RegistryDocType` list; the
  mdoc map carries NO `namespaces` member. Attribute names and values never leave the wallet.
- OIA_08f global setting: `user_preferences.proto` field 6 `dc_api_disclosure_enabled` (inverted
  in the datasource mapping so an unset proto3 bool reads as OIA_08e's default-on);
  `UserPreferences.dcApiDisclosureEnabled`; `DocumentRepository.isDcApiDisclosureEnabled`;
  `DigitalCredentialsRegistrar.registerCredentials()` returns early and CLEARS the registry when
  the switch is off — with disclosure disabled the platform learns nothing about the wallet's
  documents (and forgets what it knew). `SettingsScreen`/`SettingsViewModel` expose the switch;
  toggling re-runs registration. The OIA_08f SHOULD (per-attestation selection after a disable)
  is not built — PoC scope, noted in the plan.
- `RegistryDocTypeTest` — decodes the CBOR payload and asserts id+docType only, and greps the
  payload text for `family_name`/`isikukood`/`birthdate` to prove no attribute data escapes.

PENDING-DEVICE: the real `identitycredentialmatcher.wasm` behaviour against a `namespaces`-less
payload (does the GMS matcher accept the reduced structure and still show the picker entry?) is
verifiable only on a device with Play services' Identity Credential Manager. The CBOR shape
follows the previous structure minus the attribute map; JVM-side, the structure is pinned by
`RegistryDocTypeTest`.

## 4c — the EE-ZKP-004 interface

Before: the view model called `LongfellowZkSystem` directly for every doctype that had specs
(F11), picked a circuit by attribute count (F13) and computed the tier in its own `when` from
facts beside the attempt rather than from the attempt (F15). The base already keyed ZK specs by
docType and refused the whole response when an age proof failed (step 3 review fixes).

- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkPresenter.kt` (new) — the interface:
  `presentation(schemeId, document, sessionTranscript) : ZkPresentation`, where
  `ZkPresentation` is `Proved(zkDocument)` or `Unavailable(reason)` with reasons
  `NO_SCHEME_REQUESTED` / `PROVER_UNAVAILABLE` / `UNKNOWN_SCHEME` / `PROVER_FAILED`. The scheme
  id (a `ZkSystemSpec.id`) travels in the proof — multipaz fills `ZkDocumentData.zkSystemSpecId`
  from the resolved spec, so the verifier sees which scheme the proof was made over.
  `LongfellowZkPresenter` is the implementation; prover exceptions become `PROVER_FAILED`
  (F15's JVM-failure half; the native-abort half remains a recorded risk). `resolveSchemeId(zkSystem, requested, numAttributes)` resolves the request to a scheme
  id through the same JVM-tested `HolderObligations.strongestMatchingSpec` rule as before.
- `DigitalCredentialsViewModel.kt` — the share loop asks the presenter per document; proving is
  scoped to `requiresZkProof()` (the age doctypes, `eu.europa.ec.av.1`), other doctypes get
  `schemeId = null` → `NO_SCHEME_REQUESTED` and are never sent to the prover (F11). The scheme
  is resolved over the credential's own docType's specs. The tier follows the presenter's reason
  (`NO_SCHEME_REQUESTED` via `HolderObligations.tierFor(...)`, `PROVER_UNAVAILABLE` →
  `PLAIN_DEVICE_INCAPABLE`, `UNKNOWN_SCHEME` → `PLAIN_NO_MATCHING_CIRCUIT`), so the log cannot
  disagree with the attempt. `PROVER_FAILED` on an age document keeps step 3's refusal
  (`PLAIN_PROOF_FAILED` row, `AppError.PRESENTATION_PROOF_FAILED`, nothing sent). The EE-ZKP-042
  expected tier treats a non-age document as unsatisfiable, since it is never proved, so the
  pre-share notice matches what is sent. The duplicated circuit-match helper is deleted. The response-level escalation (`escalateToResponseTier`, step 3) is unchanged and now
  runs over the per-document tiers.
- `HolderObligations.kt` — `tierFor(zkUsed, proofRequested, zkCapable)` (per-document tier),
  `responseIsIdentifying(documents)` (any document discloses a non-predicate value → the response
  is identifying), `combinedRequestNotice(documents)` (a proved predicate alongside a plain
  identifying document in one response), and `DisclosedDocument(proofUsed, disclosesValue)`.
- Acceptance test (plan §4 item 4c, verbatim scenario):
  `HolderObligationsTest."age_over_18 plus a PID attribute logs linkable and shows the
  combined-request notice"` — an `age_over_18` predicate proved over its own scheme plus a PID
  text attribute in the same response asserts `responseIsIdentifying`, every row of the escalated
  tier list `isLinkable`, and `combinedRequestNotice` true. Supporting tests: a pure-predicate
  response stays unlinkable with no combined notice; an identifying value disclosed *inside* a
  proof still makes the response identifying (F15's "the circuit will disclose `family_name`");
  a plain identifying document with no proof in the request shows no combined notice (the
  ordinary EE-ZKP-042 notice covers it); the per-document tier table.

Deviations:

- `responseIsIdentifying` first gated on `proofUsed && disclosesValue`; the acceptance test
  failed (a plain PID row is not `proofUsed`) and the test was right — the plan's rule is "mark
  every row of a response linkable when ANY document in it discloses a non-predicate attribute",
  regardless of which document proved. Fixed to `any { it.disclosesValue }`.
- Per-doc-request `zkRequest` scoping: `resolveSchemeId` runs per credential over the specs of
  that credential's own docType (the base's docType-keyed map) and its own checked-field count,
  with the doctype gate in front. Two doc requests for the same docType share one spec list.
- The ZK tier copy is unchanged: it still says only that the issuer's signature was not shared.
  Scoping proving to the age doctypes does not by itself make a zero-knowledge row unlinkable
  (the proof can disclose a value; the response can carry a plain document), so the step 3 copy
  audit's wording stands.
- The combined-request notice is a decision-core function with tests; wiring it as a distinct
  pre-share notice string on the DC API screen is NOT done — the existing EE-ZKP-042 pre-share
  notice already fires for this state (the response is expected-linkable), and the plan's
  acceptance is that the notice is shown, which the existing notice path satisfies. A distinct
  combined-request wording would go through the step-3d copy audit; recorded as optional polish.

PENDING-DEVICE: `LongfellowZkPresenter` prove path on-device (the scheme id round trip through
`ZkDocumentData.zkSystemSpecId` and the verifier reading it) needs a device run; the JVM
`:zk-conformance` suite pins the prover round trip itself.

## 4d — surface reader authentication

Before: `DeviceRequestParser` parsed and checked `readerAuth` (and exposed
`readerCertificateChain`), but nothing outside the parser consumed it — the consent screen named
the platform-asserted origin only (plan F7, `EE-RP-003`).

- `DigitalCredentialsViewModel.processRequest` — takes the first doc request carrying a reader
  certificate chain, reads the leaf certificate's subject CN (multipaz `X509Cert.subject`,
  an `X500Name`; the `CN` component), and stores it in `DcUiState.readerSubject`.
- `DigitalCredentialsScreen` — the consent screen renders the subject under the origin, present
  only when the reader authenticated the request; absent otherwise (no placeholder, no claim).

Deviations:

- The full subject string (O, OU…) would over-share screen space; the CN is the human-facing
  name and is what `EE-RP-003`'s successor steps need. The raw chain remains available for
  §8.3 (access-certificate trade names, intended use).
- No trust judgement is made or implied: the subject is displayed as read from the certificate.
  Whether that chain validates against a reader trust store is §8.3 scope and is deliberately
  not started here.

PENDING-DEVICE: the ISO 18013-7 Annex C verifier in this repo's own tooling does not sign
`readerAuth`, so the subject path is exercised only against a reader that signs. Verifying the
rendered subject against such a reader is a device item.

## 4e — dependency hygiene (evaluation, recorded per plan)

Evaluated 2026-09-24, against Google Maven / Maven Central metadata fetched live (HTTP status
checks, not memory), offline build verified after each change that landed:

| Dependency | Current | Candidate | Decision |
|---|---|---|---|
| `androidx.credentials` (+ `credentials-play-services-auth`) | `1.6.0-SNAPSHOT` (s01.oss.sonatype snapshots repo) | **1.6.0 stable** (published; `1.7.0-alpha03` exists, not considered) | **BUMPED.** Pure androidx: runtime deps (annotation 1.8.1, biometric 1.1.0, core 1.15.0, kotlin-stdlib 2.1.20, coroutines 1.9.0, jspecify 1.0.0 per the published `.module`) are all at-or-below what the app already resolves; no multipaz coupling. Compile + 51 unit tests green online AND offline after the bump. Removes the SNAPSHOT repo dependency the plan called removable. API surface used (`DigitalCredential`, `GetDigitalCredentialOption`, `PendingIntentHandler`, `ExperimentalDigitalCredentialApi`) unchanged — no source change needed. |
| `registry-*` (`androidx.credentials.registry:*`) | `1.0.0-alpha03` | `1.0.0-alpha05` (published, verified 200 on all four artifacts the app pulls) | **NOT BUMPED.** Alpha-to-alpha on a stack the app does not compile against directly (no `androidx.credentials.registry` import anywhere in `app/src` — the registrar speaks to GMS `IdentityCredentialManager` and a bundled WASM matcher; the registry artifacts ride the classpath). A bump that cannot be exercised by any test or compile-time reference is risk with no observable payoff; deferred to the step that actually consumes the registry APIs. |
| multipaz / multipaz-android / multipaz-longfellow | `0.99.0` | `0.100.0` (2026-07-08), `0.101.0` (2026-09-10) | **NOT BUMPED, per plan.** `eudi-iso18013-data-transfer` 0.10.0 pins `multipaz-android`/`multipaz-android-legacy` **0.94.0** at runtime (verified in its published POM); the app already constrains every multipaz artifact to 0.99.0 to keep core and -android in step, and 0.99.0 is the lowest that publishes `multipaz-longfellow`. Moving to 0.100.0/0.101.0 changes the bundled `libzkp.so` and the circuit set (0.100.0 alone lists "ZKP improvements … surfacing ZKP errors to Kotlin"), which is exactly the step-0 re-run trigger the plan names: the ZK conformance expectations (`:zk-conformance`), the F13 circuit-registry facts, and the step-0 records would all need re-verification. Release notes 0.100.0/0.101.0 show nothing this step needs (CDN parsing, PKCS#12, web dev tools, transaction-data rework, AKI reader identifiers). |
| `play-services-identity-credentials` | `16.0.0-alpha08` | `16.0.0-alpha12` exists | **NOT BUMPED** — same reasoning as registry-* (alpha GMS surface, not exercised by tests; the DC API path is device-verified, not JVM-verified). |

Net version changes: `androidx-credentials = "1.6.0-SNAPSHOT"` → `"1.6.0"` in
`gradle/libs.versions.toml` (commit `99692be`). Everything else stays. The pin conflict
(data-transfer 0.94.0 vs multipaz 0.99.0+) is unchanged and remains a step-0-gated decision.

PENDING-DEVICE: none — the bump is compile- and test-verified on the JVM; the DC API behaviour
under 1.6.0 stable is the same code path this step already exercises on device.

## Verification

- `:app:compileDebugKotlin` + `:app:testDebugUnitTest`: green before every commit
  (38 → 44 → 46 → 51 → 51 → 51 tests on the original base, 0 failures). After the rebase onto
  `f281010`: 51 tests, 0 failures, 2 skipped.
- Full `:app:testDebug` and `:zk-conformance:test`: green at the end of the step (final run
  after the rebase: 51 unit tests, 0 failures; `:zk-conformance:test` 4 tests, 0 failures).
- New JVM tests: `DcApiRequestDispatchTest` (7), `RegistryDocTypeTest` (2),
  `HolderObligationsTest` +5 (EE-ZKP-004 acceptance block).
- Pushed as `step4-protocol-hygiene` (PR #7); `fixture-device-response`, `master`, `step0..5`
  untouched.
