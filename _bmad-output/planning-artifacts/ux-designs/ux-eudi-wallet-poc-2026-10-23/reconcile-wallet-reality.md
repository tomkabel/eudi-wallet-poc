# Reconcile — wallet UX decisions vs the Android wallet's code

Territory: `app/src/main/kotlin/ee/cyber/wallet/ui/screens/**`, `ui/components/**`, `ui/navigation/**`,
`ui/theme/**`, `security/**` (PinVerifier, SecureAreaKeyManager, PromptDialogHost), `data/**` (persistence),
`app/src/main/res/values{,-et,-ru}/strings.xml`.
Baseline: `2d38feb` (tree clean in this territory; only `AGENTS.md`, `docs/README.md` modified and
`.agents/`, `.claude/` untracked). Inputs: workspace `.memlog.md`, `.decisions.log`, `.working/extract-wallet.md`.

Method: every claim below was re-read in the code at `2d38feb`; the extract was used as a lead, not as
evidence. Two extract details and one PRD premise are corrected (§8). Path note: the requester component
lives at `ui/components/VerifiedParty.kt`, not `ui/screens/components/` (no such directory).

**Headline:** three of the four FRs rest on premises about the code that are no longer true at `2d38feb`,
and the biometric premise is false. The UX *components* the run specifies are mostly cheap; the *data*
they need is what is missing, and two of the four decisions collide with configured behaviour
(no reader anchors ⇒ proximity can never be "verified"; hard refusal on the proximity consent surface
would refuse every proximity share).

---

## 1. FR-45 — two-state requester replacing the unconditional badge

### 1.1 What the code actually is (verified)

- `ui/components/VerifiedParty.kt:18-31` — `fun VerifiedParty()`, **no parameters**, unconditional
  icon + `R.string.presentation_verified_party`. Six render sites on five surfaces:
  `PresentationRequestScreen.kt:335`, `ProximityScreen.kt:156` (consent) and `:132` (no-match),
  `DigitalCredentialsScreen.kt:156`, `CredentialOfferScreen.kt:129`, `PidAuthenticationScreen.kt:51`.
- Proximity requester name is never assigned: `ProximityViewModel.kt:474` `val verifier: String = ""`;
  the whole `ui/screens/proximity/` package has no assignment (grep: only reads at
  `ProximityScreen.kt:128,137,152,174`). Consequences already visible in the product:
  an empty `headlineMedium` title, and `presentation_footer_note`
  (`values/strings.xml:87` "…share with the **%1$s**!") rendering with a blank substitution
  (`ProximityScreen.kt:174`).
- The **unverified** state is already emitted today, under the badge, on the proximity path:
  `ProximityViewModel.kt:260-276` `ageRefusal()` computes `authenticated` from
  `readerAuthenticated && readerChainTrusted` and, with no anchors configured, gets `false`;
  `HolderObligations.plainRefusalReason(proofRequested = false, authenticated = false)`
  (`domain/presentation/HolderObligations.kt:72-74`) returns `PRESENTATION_UNAUTHENTICATED_REQUEST`
  ("Sharing stopped. This party did not prove who it is…", `values/strings.xml:94` / `values-et:94`).
  `ProximityScreen.kt:136` renders exactly that string directly below `VerifiedParty()` at `:132`.
  FR-45's contradiction is therefore already true in-product in the refusal branch; the decisions
  address the consent branch.

### 1.2 Where a "verified" requester could come from — per surface

| Surface | Name source today | Can it ever be *verified* under FR-45? |
|---|---|---|
| Remote OpenID4VP (`PresentationRequestScreen`) | `client.legalName() ?: client.id` (`PresentationRequestViewModel.kt:425`) | Yes — but **not via readerAuth**: the accepted client-id prefixes are only `X509SanDns` and `X509Hash`, both resolved through `CertificateChainValidator` against the trusted set; bare `RedirectUri` is deliberately excluded (`di/ClientIdPrefixPolicy.kt:9-22`, wired `di/OpenId4VPAndVCModule.kt:68`, anchors `di/ConvertersModule.kt:33` → `R.raw.trusted`). Already pre-consent enforced. |
| DC-API (`DigitalCredentialsScreen`) | `verifier = origin` (`DigitalCredentialsViewModel.kt:307`) + `readerSubject` (`:207`, badge + name `:151-164`) | No. `DeviceRequestParser` is constructed **without** `trustedReaderRoots` (`:183`), and the code says so: "No anchors are configured on this path yet, so every DC API request counts as unauthenticated" (`:209-211`). |
| Proximity | never assigned (`:474`) | No. `DeviceRequestParser(...)` at `ProximityViewModel.kt:265` passes no roots; `ageRefusal`'s comment records the same fact (`:257-258`). |
| Credential offer | `credentialType.issuerName()` — a **static string per credential type** (`ui/screens/documents/Extensions.kt:221-227`); the VM holds only `credentialType` + attribute list (`CredentialOfferViewModel.kt:17-22`) | Not evaluable: there is no issuer/requester identity in the offer state at all, and no chain. |
| Mock PID sharing | literal `"tara.ria.ee"`, hardcoded at `PidAuthenticationScreen.kt:47` | No — it is a constant. |

- `readerAuthSubject()` (`util/DeviceRequestParser.kt:446-454`) takes the CN of a
  **signature-valid but not trust-validated** chain, on purpose ("Trust anchors and the chain-to-anchor
  decision are §8.3 scope"). It is the helper that would populate the name — and it is precisely the
  thing FR-45 forbids feeding the screen. Only its unit test and the DC-API path use it
  (`DigitalCredentialsViewModel.kt:207`); proximity never calls it.
- No production call site passes `trustedReaderRoots` (grep over `app/src/main`): the only trust store in
  the app is the **issuer** anchor set `R.raw.trusted`.

### 1.3 Verdict — implementable, with two collisions

- **Component work: small.** 31 lines, one string per locale, six call sites.
- **Data work: not in the design's gift.** `readerChainTrusted` is `false` by configuration on both
  reader-auth paths, so under the decision as recorded ("no validation ⇒ unverified, refusal default, no
  proceed path") **proximity and DC-API can never present**. That contradicts the code, which shares any
  non-age document over proximity behind the D12 PIN (`ProximityViewModel.kt:361-365`) and shares from
  the DC-API path with **no PIN at all** (the DC-API Activity has no `PinActivityResultContract`; grep
  shows launchers only in boarding, issuance, proximity, presentation, main nav). Either anchor
  provisioning lands (an architecture/contract task — no reader anchor source exists in the repo), or the
  design must say the proximity/DC-API consent surfaces are permanently unverified-refusal, which is a
  functional regression that collides with FR-48.
- **The offer surface has no input for the rule.** Making "unverified ⇒ refuse" apply to
  `CredentialOfferScreen` would refuse all issuance (the name is a hardcoded type label). The design
  needs an explicit carve-out, or the two-state rule is unimplementable there.
- **The rule as worded names the wrong mechanism for the remote path** ("the `readerAuth` chain is
  validated to a trust anchor before any requester name is rendered", prd.md:1005-1010): that path has
  no `readerAuth`; its equivalent is client-id prefix + X509 chain, and it is already enforced.
- **A name surface the decision set misses:** `PinEntryScreen.kt:238-239` renders the party as a bold
  `titleMedium` straight from `PinActivity`'s `Input.party` (`PinActivity.kt:39-40`). Today the proximity
  PIN screen prints the literal "Proximity reader" (`ProximityViewModel.kt:363,454`) and the mock route
  prints "tara.ria.ee" (`MainNavGraph.kt:246`). Same trust question, no badge, not in the surface list.
- **A refusal affordance is needed in an existing state:** `ProximityScreen.kt:120-141`
  (`ProximityRequestNoMatchContent`) has **no buttons at all**; it relies on back navigation. Its refusal
  sentence (`:137`) is also a hardcoded English string — and it is that same unlocalised sentence that
  shows the blank name.

## 2. FR-46 — user presence bound to key operations

### 2.1 The premise is false at `2d38feb` (strongest single contradiction)

The PRD's audit line (`prd.md:1416-1428`): "no user-presence gate on device keys (**no biometric prompt
anywhere**)" — an audit of record taken at `732d57e`. At HEAD the biometric prompt exists, end to end:

- Keystore gate: `security/SecureAreaKeyManager.kt:70-74` (presentation keys) and `:121-125` (batch keys)
  `setUserAuthenticationRequired(true, PRESENTATION_KEY_AUTH_TIMEOUT = 60 s (:218), AUTH_TYPES =
  {LSKF, BIOMETRIC} (:221-222))`.
- Prompt trigger: `SecureAreaCOSECryptoProvider.kt:49-52` passes
  `Reason.HumanReadable("Unlock your document" / "Authenticate to share this document")` into
  `secureArea.sign(...)` (`:79`).
- Prompt UI: `ui/prompt/PromptDialogHost.kt:55-79` (multipaz `uiLauncher`),
  `:87-149` (`BiometricPrompt` bound to the keystore `CryptoObject` at `:140`; negative/cancel/lockout →
  `false` → fail closed, `:116-121,143-145`); host activity wiring `WalletApplication.kt:72-97`;
  rationale `SecureAreaKeyManager.kt:151-161`.
- Attached to all three presentation paths: `domain/presentation/OpenId4VPManager.kt:292` (remote),
  `ProximityViewModel.kt:397-405`, `DigitalCredentialsViewModel.kt:556-566`. Device signing is EC-only
  and non-EC attestations are refused before signing (`DigitalCredentialsViewModel.kt:524-538`), so no
  ungated credential-signing path survives for presentations. Proof generation (the Longfellow prover,
  `domain/presentation/ZkPresenter.kt:57-77`) uses no key; the DeviceAuthentication signature that
  precedes it **is** the gated operation.
- Git: closed after the audit commit — `27bf030` (D13 keys), `f818398` (D14 Stage-2), `a4a4aeb`
  (prompt host), `0ac52af`; all descendants of `732d57e` (verified with `git merge-base --is-ancestor`).

⇒ FR-46's substantive AC ("key operations gated by a platform prompt, not an app-level flag") is
already met for EC credential keys. The design must not specify a *new* gate; it must specify the copy,
order, failure and localisation of the gate that exists.

### 2.2 What is genuinely missing (i.e. the FR-46 UX work)

1. **Copy is hardcoded English in Kotlin, not a string resource:** `PromptDialogHost.kt:158-159`
   ("Confirm it's you", "Unlock your document"), `SecureAreaCOSECryptoProvider.kt:50-51`. In an EN/ET app
   the only explanation of the second gate is untranslated (§8, §6.5).
2. **The prompt is the system dialog** (`androidx.biometric.BiometricPrompt`,
   `PromptDialogHost.kt:3-4`): title/subtitle are the only controllable text, the negative button is the
   platform's `android.R.string.cancel` (`:133`), the error string is the platform's. No restyling, no
   extra fields, no per-screen layout. `PresentmentUnlockReason` (multipaz's own localised key-unlock
   copy) is deferred and never constructed in this app (`:151-154`).
3. **Stage-2 fires only when the 60 s window has lapsed.** The gate is per *unlock*, not per operation:
   two shares inside the window raise one prompt. Any copy claiming "every sharing asks for your
   biometrics" is false; the honest wording is "when the wallet has not unlocked your key in the last
   minute".
4. **Failure is silent and, on the remote path, actively misleading.** Proximity maps a cancelled prompt
   to `Effect.ProximityCancel → onCancel()` with no message (`ProximityViewModel.kt:378-380,419-425`;
   `ProximityScreen.kt:96`). The remote path swallows `KeyLockedException` inside its `runCatching`
   (`PresentationRequestViewModel.kt:281,311-321`) and shows `Error.ConnectionError(true)` — i.e. the
   user who cancels the fingerprint prompt is told the **connection failed** and offered a retry. The
   DC-API path falls through to its generic `DcEffect.Error`
   (`DigitalCredentialsActivity.kt:80-83`). This contradicts the run's own refusal-vs-cancellation
   constraint (memlog, "Refusal constraints") more directly than any FR-45 detail does.
5. **The two-gate sequence is unexplained.** One tap on a consent screen raises the PIN Activity first
   (`ProximityScreen.kt:97-99`, `PresentationRequestScreen.kt:202-204`) and then the biometric prompt
   inside signing. No screen says why there are two challenges.
6. **Scope of "device keys".** Credential keys are gated; the wallet-instance/provider JWS keys are not:
   they sign through `LocalCryptoProvider`/`EncryptedKeyStoreManager` (`crypto/CryptoProvider.kt:30,81`,
   `crypto/LocalCryptoProvider.kt:45-51,153`, `crypto/RemoteCryptoProvider.kt:32`) and the BKS store key
   is explicitly `setUserAuthenticationRequired(false)` (`security/AndroidEncryptionManager.kt:58`).
   FR-46's "or the ungated path is removed" bites here — the decision must say whether those keys are in
   scope, because removing that path would break wallet registration and issuance.

## 3. FR-47 — brute-force-resistant consent gate + full honesty

### 3.1 The persistence layer is real and already correct

`PinRecord` (`security/PinVerifier.kt:13-18`), 5 attempts / 30-minute lock (`:51-64`, `:85-86`),
`isLocked` (`:45`), and a durable write before the caller sees the answer
(`data/datastore/UserSessionDataSource.kt:35-44`, columns `:57-63`). The in-memory UI counter is also
exactly as described: `TOTAL_ATTEMPTS = 3` (`PinEntryViewModel.kt:26`) carried in `PinData.ConfirmPin`
(`:39-44`), decremented `:197-207`, `Result.Failure` at `:205-206`, copy `:439-443`, and a simulated
delay `:130-131` (`// FIXME: do real remote call?`) before `verifyPin` at `:137`.

The per-launch reset is **structural, not a constant**: `PinActivity.kt:39-42` constructs a fresh
`PinData` on every launch, and `PinActivityResultContract.Input` (`:16-20`) has no field for attempts or
lock state — there is no channel through which a launch could resume a count. `MainNavGraph.kt:246`
constructs the same shape without an attempt count. Removing the in-memory counter is therefore a
contract change, not a deletion.

### 3.2 What the decision does not carry

- **`verifyPin` returns `Boolean`, nothing else** (`UserSessionDataSource.kt:35-44`) — no
  `failedAttempts`, no `lockedUntil`. `PinError` has three cases and no `Locked`
  (`PinEntryViewModel.kt:58-62`); `Result` has `Success/Failure/Cancelled` and no locked outcome
  (`PinActivityResultContract.kt:22-26`). **"2 attempts remaining" needs a new data path** (new API on
  the data source, new error/result variant, new state) plus new EN+ET strings. This is a
  data-layer item to hand to architecture, not a UI item.
- **Semantics trap:** at the moment of locking, the record resets `failedAttempts` to 0 and keeps only
  `lockedUntil` (`PinVerifier.kt:59-60`). A screen deriving "attempts remaining = 5 − failedAttempts"
  would therefore display "5 attempts remaining" exactly while locked. The counter must be specified
  against both fields, and the design should note that the budget is a fresh 5 after the lock expires.
- **No lockout vocabulary anywhere:** no lock/attempt string beyond `values:72` / `values-et:72`
  (`pin_incorrect_pin_attempts_left`, "Incorrect PIN. Attempts left: %1$s"), no `AppError` value for a
  lock (`domain/AppError.kt:8-28`), no `LogEntryEntity` support (`data/database/Entities.kt:34-51`).
  `isLocked` has no production caller at all — tests only (`PinVerifierTest.kt`).
- **There is no app-entry gate to hang the lockout on.** `WalletApp.kt:100-108` branches only on
  `isActivated`; a locked user can still open the wallet, browse documents, settings and the activity
  log. "The count survives restart" is only observable at the *next* PIN prompt. FR-47's "full honesty"
  (counter, duration, escalation, recovery) has no carrier surface today — it is new navigation or a new
  `Input`/`Result` shape, not a restyle.
- **Recovery today = wait 30 minutes, or wipe everything.** `AccountRepository.kt:48-56` clears the
  session (including the PIN record), keys, database and preferences; there is no PIN change / reset /
  forgot-PIN flow anywhere (only `setPin` at creation, `PinEntryViewModel.kt:182`). Copy implying a
  recovery path would overstate what exists.
- **One budget, one source.** Today the UI stops the user at 3 per launch while the store escalates at 5;
  once the 3 is removed the store is the only counter — the design should state the visible budget is 5
  and name the store as its source (it also decides the ET/EN wording of "attempts remaining").
- Minor: removing the `delay(1000)` changes perceived timing to the KDF cost (~250-400 ms,
  `PinVerifier.kt:29-31`), so any "checking…" state must be sized for the Argon2 verify, not a fake second.

## 4. FR-48 — proximity consent parity

- **Premise stale (already flagged in memlog):** the proximity share **is** PIN-gated — `authenticated`
  flag `ProximityViewModel.kt:86-88`, gate `:361-365`, release `:342-345`, incorrect PIN keeps the BLE
  session `:346-351`; launcher `ProximityScreen.kt:81-88,97-99` (D12, commit `27bf030`).
- **Duplication is worse than "card internals":** three private per-surface cards —
  `PresentationRequestCard` at `ProximityScreen.kt:203` and `PresentationRequestScreen.kt:398`,
  `DcPresentationRequestCard` at `DigitalCredentialsScreen.kt:245` — and
  `PresentationOptionalField` duplicated with an identical signature at `ProximityScreen.kt:229` and
  `PresentationRequestScreen.kt:450` (`DcPresentationOptionalField` at `DigitalCredentialsScreen.kt:279`).
  They take different state types (`UiState` vs `DcUiState`) and are `private` ⇒ unification needs a
  shared component **and** a shared model, not a function move.
- **Parity gaps the "same terms" decision must enumerate:**
  - channel notice exists only on remote (`PresentationRequestScreen.kt:341-350`; `values:86-87`);
    proximity has no equivalent;
  - `shareDisabled` disables Share on remote (`:392`) but proximity's `PrimaryButton` has no `enabled`
    parameter (`ProximityScreen.kt:197`) even though its state computes it
    (`ProximityViewModel.kt:310-317`) ⇒ wiring, not copy;
  - ZK/tier notices (`presentation_zk_notice_*`, `presentation_issuer_disclosure_notice`) are remote +
    DC-API only; proximity shows only the last-attestation notice (`:177-189`);
  - cancel semantics differ deliberately (see next point).
- **Bug the parity decision must not inherit:** `PresentationRequestScreen.kt:187-189` builds
  `Event.OnCancelClicked` and **discards it** (a bare expression) ⇒ cancelling the PIN on the remote
  consent screen does nothing. Any "consent parity" statement that says a cancelled confirmation returns
  the user is contradicted by the remote path today; it also affects FR-45's "Refuse/Cancel only" model.
- **Consent is not per-share, as stated.** `authenticated` is an in-VM flag that is never reset
  (`ProximityViewModel.kt:88,342-345`) and the remote path's is a per-process `MutableStateFlow`
  (`PresentationRequestViewModel.kt:187`) that survives across successive presentations. A second
  request in the same session is released without a new PIN. If the design claims "each share carries
  consent", the flags must be scoped — that is a code decision the run does not carry.

## 5. Cross-cutting conflicts (navigation, theming, persistence, locale)

- **Navigation.** No entry gate (`WalletApp.kt:100-108`). The PIN is an **out-of-graph Activity** driven by
  `ActivityResultContract`; its `Input`/`Result` are `@Parcelize` shapes (`:16-26`) consumed by five
  launchers (`BoardingNavGraph.kt:44`, `IssuanceScreen.kt:96`, `ProximityScreen.kt:82`,
  `PresentationRequestScreen.kt:184`, `MainNavGraph.kt:243`), so widening them for a lockout/unverified
  state touches five call sites plus two Activities. Deep links (`MainRoute.kt:53-61`) navigate straight
  onto the presentation consent screen (`WalletApp.kt:86-99`), so any new refusal state must be reachable
  with no back stack.
- **Theming — light-only is real, but the preference is not absent.** `ui/theme/Theme.kt:64-67` ignores
  `useDarkTheme` and always applies `LightColors` (`:17-48`); there is no `darkColorScheme` and no
  `values-night/`. But `DarkThemeConfig` exists and is **persisted** (`ui/model/UserPreferences.kt:3`,
  `MainActivity.kt:95-103`, `data/datastore/UserPreferencesDataSource.kt:24-36`) — a design that says
  "there is no theme preference" would be wrong; one that says "the app is light-only in effect" is
  right. The status bar is hardwired (`Theme.kt:59,67`), so a refusal screen cannot ask for a dark
  status-bar treatment.
- **Colour roles.** M3 light scheme + brand hues exist (`theme/Colors.kt`); `primary`/`error` are mapped,
  **no warning role** and `yellow_600` (`Colors.kt:14`) is unmapped (commented-out roles,
  `Theme.kt:38-46`).  "Pass-green / refuse-red and their washes" is implementable (`green_600`/`red_600`
  used at `PinEntryScreen.kt:189,197`, `PresentationRequestScreen.kt:292,301`,
  `ActivityLogScreen.kt:113`, `LogEntryScreen.kt:201`), but a
  third "caution" tone for unverified/locked needs a new mapping for both light **and** a non-existent
  dark variant. Status is today carried by icon+colour pairs (e.g. `ActivityLogScreen.kt:113`), which is
  reusable — there is no colour-only signalling to fix.
- **Motion.** The only animations in the app: stage `fadeIn/fadeOut` transitions
  (`WalletApp.kt:103-104`), the PIN shake (`PinEntryScreen.kt:274,415-435`), and
  `FadedVisibility` (`ui/components/Visibility.kt`) wrapping `AnimatedVisibility`. A collapsible "why"
  for FR-45 can reuse `FadedVisibility`; nothing else exists to reuse.
- **Accessibility.** No `semantics`/`liveRegion` anywhere in `app/src/main` (grep); decorative icons all
  carry `contentDescription = ""` (`VerifiedParty.kt:23` and ~20 sites). "Announcements for
  long-running steps" and "visible focus / keyboard-complete" are net-new work with nothing to reuse.
- **Timing limits the decisions don't touch.** The success surface auto-closes after **5 s**
  (`PresentationRequestScreen.kt:250`, `:261-270`), used by the presentation success and the mock-PID
  success route (`MainNavGraph.kt:269`) — a WCAG 2.2.1 (Timing Adjustable) item in the same decision
  family as the lockout honesty, carried by no decision.
- **Persistence shape.** The only durable gate state is `UserSessionProto` (hash/salt/attempts/lockedUntil,
  `UserSessionDataSource.kt:50-63`). There is no store for a consent decision or an "unverified
  acknowledged" fact, and the activity log's `party` column is **non-null** (`Entities.kt:40-41`) — a
  design that logs a refusal from an unverified requester must define that string (today the only options
  in code are an untrusted name or the generic `R.string.log_entry_proximity_party`, `values:241`/
  `values-et:238`).
- **Locale reach.** `values/` and `values-et/` carry the same 223 keys (verified by key-set diff; the only
  differences are ordering); `values-ru/strings.xml` holds **4** strings and `RU` is not even offered —
  `LanguageResource` lists it (`ui/util/LanguageResource.kt:14`) but `supportedLanguages = [EN, ET]`
  (`di/ResourceModule.kt:22`), so the picker shows two languages. Every new gate/unverified/lock string
  therefore costs 2 entries, and a decision that writes Russian copy specifies a locale the app cannot
  select.

## 6. Wallet UX reality no decision carries (dropped ideas)

Ordered by bite:

1. **The unvalidated requester name is persisted and re-shown later.** `PresentationRequestViewModel.kt:382-388`
   and `:437-443` write `party = verifierName()` (= `client.legalName() ?: client.id`, `:425`) into
   `LogEntryEntity.party`, which the activity-log screen renders. FR-45 is scoped to the consent screen;
   the same attacker-influenceable string becomes the user's durable record of "who asked".
2. **The "verified" state is unreachable in production.** No reader anchors are configured on either
   reader-auth path (DC-API states it in-code, `DigitalCredentialsViewModel.kt:209-211`; proximity passes
   none, `ProximityViewModel.kt:265`), and the only trust store is the issuer set `R.raw.trusted`. The
   design's verified branch is currently dead code by configuration.
3. **Proximity already shows badge + "this party did not prove who it is" on one screen**
   (`ProximityScreen.kt:132-137`) — the FR-45 contradiction is visible in the shipped UI, in the branch
   the decisions don't name; and that refusal sentence is untranslated with a blank name.
4. **The PIN screen is a requester-name surface too** (`PinEntryScreen.kt:238-239`), fed by
   `Input.party`; on the mock route the requester is the literal `"tara.ria.ee"`
   (`MainNavGraph.kt:246`, `PidAuthenticationScreen.kt:47`) — the only "verified-looking" name in the app
   is a constant.
5. **The biometric prompt's copy is untranslated and lives in Kotlin** (`PromptDialogHost.kt:158-159`,
   `SecureAreaCOSECryptoProvider.kt:50-51`), so the app's only explanation of the second gate cannot be
   localised through `strings.xml` as-is.
6. **Cancelling the biometric prompt is reported as a connection failure** on the remote path
   (`PresentationRequestViewModel.kt:311-321`) and silently aborts on proximity
   (`ProximityViewModel.kt:419-425`) — the cancellation-vs-refusal distinction the run requires has no
   implementation on any path.
7. **Two consecutive challenges for one tap** (PIN Activity, then the system prompt inside signing) with
   no screen linking them.
8. **The remote consent screen's PIN-cancel path is dead code** (`PresentationRequestScreen.kt:187-189`).
9. **No launch gate** (`WalletApp.kt:100-108`): the wallet is browsable unauthenticated, and a lockout
   cannot be surfaced at launch without new navigation.
10. **Lockout recovery = wait, or destroy the wallet** (`AccountRepository.kt:48-56`); no PIN reset flow.
11. **The activity log has no vocabulary for the new states**: no `AppError` for a lock or for an
    unverified refusal (`AppError.kt:8-28`); the log is the app's durable record and FR-45/47's outcomes
    would be invisible in it.
12. **Success auto-closes in 5 s** (`PresentationRequestScreen.kt:250,261-270`; `MainNavGraph.kt:269`) —
    a timing limit and a "did it go?" anxiety untouched while refusal honesty gets full specification.
13. **No first-run/consent education surface** for either gate: nothing explains the PIN, the biometric
    window, or what "verified party" means — the badge is an assertion with no explanatory carrier.
14. **Untranslated UI English in the states users see most**: "Processing document request"
    (`PresentationRequestScreen.kt:316`, `DigitalCredentialsScreen.kt:103`), plus `delete_btn` in
    `values-et/strings.xml:6` — the ET surface already ships one English string.
15. **Zero-motion, zero-announcement baseline**: the design's motion/a11y asks have no existing
    infrastructure (no live region, no focus/state announcements, no reduced-motion switch anywhere).

## 7. Verdict per decision

| Decision | Implementable on this code? | Required non-UX work |
|---|---|---|
| FR-45 two-state requester (component, copy, refusal-first) | Component yes; verified state unreachable; offer surface has no input; proximity refusal branch already contradicts | Reader anchor provisioning (architecture); carve-out for offer/mock-PID; decide remote-path criterion (client-id prefix + X509, not readerAuth) |
| FR-46 user-presence prompt | **Already implemented** (Stage-1 60 s + Stage-2 system prompt, all three presentation paths) | Localise the prompt copy (move it into `strings.xml`); fix cancel-reported-as-network-error; state window semantics; scope of non-credential keys |
| FR-47 persisted lockout + full honesty | Persistence yes; UI/limits no | New data path to expose attempts/lock, new `PinError`/`Result` variants, new EN+ET strings, and a carrier surface (entry gate or PIN screen) — touches `Input`/`Result` consumed by 5 launchers |
| FR-48 proximity parity | Gate already exists; duplication confirmed | Shared consent card + shared model; wire `shareDisabled`/channel notice; fix the remote PIN-cancel no-op; scope per-share vs per-process consent |

## 8. Corrections to the extract (none load-bearing for the decisions except #1)

1. The PRD audit's FR-46 premise ("no biometric prompt anywhere", `prd.md:1418`) is stale at `2d38feb`;
   the extract's own section (c)6 already records the prompt. **Decisions must cite the extract, not the
   audit line.**
2. `AppError` has **20** entries (`domain/AppError.kt:8-28`), not 21.
3. "No string for an unverified/unknown requester anywhere" is not quite right:
   `error_presentation_unauthenticated_request` exists with ET parity (`values/strings.xml:94`,
   `values-et/strings.xml:94`). What does not exist is a *state* (badge variant, default refusal at the
   name).
4. "Five surfaces" is five distinct surfaces / **six** render sites (proximity has two), and the list
   omits the PIN screen's party line (`PinEntryScreen.kt:239`), which the two-state rule logically covers.
