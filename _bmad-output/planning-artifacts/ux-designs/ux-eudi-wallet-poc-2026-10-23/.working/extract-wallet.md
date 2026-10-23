# Extract: existing Android wallet UI (screens, theming, consent patterns, localisation)

Source of truth: working tree at `2d38feb` ("Enforce the rev 1.6 age-attestation rules in the wallet and fixtures"), repo `/home/notroot/Documents/eudi-wallet-poc`.
Scope read: `app/src/main/kotlin/ee/cyber/wallet/ui/**`, `app/src/main/kotlin/ee/cyber/wallet/MainActivity.kt`, `app/src/main/kotlin/ee/cyber/wallet/WalletApplication.kt`, `app/src/main/res/values{,-et,-ru}/strings.xml`, `app/src/main/kotlin/ee/cyber/wallet/{domain,data,security}` where the UI depends on it.
Everything below is read from the files listed. Where a thing does not exist the text says **absent** rather than inferring.

---

## (a) Per-screen inventory

### Navigation topology

- Two top-level graphs plus one out-of-graph Activity.
  - `AppRoute.Boarding` ("boarding") / `AppRoute.Main` ("main") — `ui/Navigation.kt:20-23`.
  - Start destination chosen at runtime: `if (appState.isActivated) AppRoute.Main.route else AppRoute.Boarding.route` — `ui/navigation/app/WalletApp.kt:102`.
  - `WalletAppState.isActivated = isPinCreated && documents?.isNotEmpty() == true`; `isRegistered = walletCredentials?.isRegistered() == true && isPinCreated` — `WalletApp.kt:37-40`.
  - Boarding start destination: `if (appState.isRegistered) BoardingRoute.Activation.route else BoardingRoute.Welcome.route` — `ui/navigation/boarding/BoardingNavGraph.kt:40`.
  - Consequence: any restart before a document has been issued re-enters the boarding wizard at **Activation**, not at the PIN pad.
- Deep links are declared per route. Presentation `MainRoute.Presentation` carries seven patterns: `haip://present`, `eudi-openid4vp://`, `mdoc-openid4vp://`, `mdl-openid4vp://`, `av://`, `openid4vp://`, `haip-vp://` — `ui/navigation/main/MainRoute.kt:53-61`. Issuance `haip://issue?state=` — `MainRoute.kt:125`. Boarding PID offer `haip://pid?binding_token=` — `ui/navigation/boarding/BoardingRoute.kt:22`.
- Deep-link delivery: `WalletApp.kt:86-99` (300 ms delay, then `navController.navigate(deeplink)`).
- Transitions: `fadeIn()` / `fadeOut()` only — `WalletApp.kt:103-104`.

### Boarding (onboarding) graph — `BoardingNavGraph.kt:36-154`

| Route | Composable | Purpose | Reached from | Leads to |
|---|---|---|---|---|
| `welcome` (`BoardingRoute.kt:17`) | `WelcomeScreen` `WelcomeScreen.kt:55`, content `:70` | Product entry: funding logo, title, subtitle, language picker, "Create PIN" | boarding start when not registered (`BoardingNavGraph.kt:40`) | `PinActivity` with `PinFlow.CREATE` (`BoardingNavGraph.kt:44-47`) |
| `activation` (`BoardingRoute.kt:18`) | `ActivationScreen` `ActivationScreen.kt:44`, content `:49`, item card `:79` | Two-path chooser: same device / other device | primary button on Welcome; boarding start when registered | `AuthenticationMeans` (mocks) or external `AppConfig.pidProviderUrl` (real) for SAME_DEVICE; `CrossDeviceTutorial` for CROSS_DEVICE — `BoardingNavGraph.kt:52-64` |
| `boarding_offer` (`BoardingRoute.kt:19-31`) | `CredentialOfferScreen` `offer/CredentialOfferScreen.kt:102`, content `:111`, offer card `:160` | Shows issuer name + verified-party badge + fields, Continue / Cancel | deep link `haip://pid?binding_token=` | `boarding_issuance?bindingToken=` — `BoardingNavGraph.kt:71`; Cancel → `navigateUp()` `:72` |
| `boarding_issuance?bindingToken=` (`BoardingRoute.kt:33-44`) | `IssuanceScreen` `issuance/IssuanceScreen.kt:95`, content `:141`, `DocumentContent` `:152`, `RetrievingContent` `:169` | Retrieve + preview credential, "Add to wallet"; PIN gate `CONFIRM_ISSUANCE` | offer screen; scanner; Mobile-ID success | `boarding_error` on error (`BoardingNavGraph.kt:85-91`); in this graph `onContinueClicked = {}` `:81` |
| `boarding_error?error=` (`BoardingRoute.kt:46-56`) | `AppErrorScreen` `error/AppErrorScreen.kt:27` | Terminal error card + Close | issuance failure | `navigateUp()` `BoardingNavGraph.kt:99` |
| `boarding_cross_device_tutorial` (`BoardingRoute.kt:58`) | `CrossDeviceTutorialScreen` `crossdevice/CrossDeviceTutorial.kt:46`, `TutorialItem` `:85` | Four numbered steps; "Open QR code reader" | Activation CROSS_DEVICE | `boarding_scanner` `BoardingNavGraph.kt:106` |
| `boarding_scanner` (`BoardingRoute.kt:59`) | `ScannerScreen` `scanner/Scanner.kt:56`, `Scanner` `:69`, `ScannerContent` `:104`, `NoPermissionContent` `:91` | Camera permission gate + QR scan | tutorial | `boarding_issuance?bindingToken=<token>` (pops scanner) `BoardingNavGraph.kt:113-121` |
| `boarding_authentication_means` (`BoardingRoute.kt:60`) | `AuthenticationMeansScreen` `boarding/AuthenticationScreen.kt:50` | Mock auth-method list: ID-card+PIN, biometry, Mobile-ID, on-premise service (title literally "DEMO", `:54`) | Activation SAME_DEVICE under `AppConfig.useMocks` | only Mobile-ID is wired: `MobileIdAuthentication` `BoardingNavGraph.kt:127` |
| `boarding_mobile_id_authentication` (`BoardingRoute.kt:61`) | `MobileIdAuthenticationScreen` `AuthenticationScreen.kt:84` | Phone + personal code (prefilled `37200000766` / `38001085718`, `:85-86`), Confirm | means list | `MobileIdControlCode` |
| `boarding_mobile_id_control_code` (`BoardingRoute.kt:62`) | `MobileIdControlCodeScreen` `AuthenticationScreen.kt:131` | Full-black screen, hardcoded control code "1234" `:152`, Confirm | previous | `MobileIdPinCode` |
| `boarding_mobile_id_pin_code` (`BoardingRoute.kt:63`) | `MobileIdPin1Screen` `AuthenticationScreen.kt:193` + `NumPadView` `:248` | Full-black screen, numpad PIN | previous | `MobileIdSuccess` |
| `boarding_mobile_id_success` (`BoardingRoute.kt:64`) | `MobileIdSuccessScreen` `AuthenticationScreen.kt:223` | Green tick + Continue | previous | `boarding_issuance` with literal token `"mock_binding_token"` `BoardingNavGraph.kt:151` |

### Main graph — `MainNavGraph.kt`

| Route | Composable | Purpose | Reached from | Leads to |
|---|---|---|---|---|
| `dashboard` (`MainRoute.kt:26`) | `DashboardScreen` `dashboard/DashboardScreen.kt:66`; `DashboardNavigationHandler` `dashboard/DashboardNavigationHandler.kt:3-11`; nav handler `DashboardScreen.kt:109`, footer `:83`, actions `:149` | Home: primary actions (Scan QR code, Start proximity), nav list (My documents, Activity log, Settings) | main graph start destination (`MainNavGraph.kt:45`) | scanner `MainNavGraph.kt:53`; proximity `:54`; documents/activity/settings `:56-58`; both primary actions disabled when no documents (`DashboardScreen.kt:102,157,171`) |
| `documents` (`MainRoute.kt:28`) | `MyDocumentsScreen` `documents/MyDocumentsScreen.kt:83`, `MyDocuments` `:99`, `EmptyList` `:202`, `DocumentCard` `:259`, `DocumentTypesModalBottomSheet` `:160` | Credential list; FAB "Add new document" `:110` opening a doc-type sheet | dashboard; post-onboarding | offer for MDL / age-verification / EE-PoA `MainNavGraph.kt:121-128` |
| `documents/{documentId}` (`MainRoute.kt:40-49`) | `DocumentScreen` `document/DocumentScreen.kt:70`, content `:116`; `DocumentCardView` `document/DocumentCardView.kt` | Credential detail, QR-code card `DocumentScreen.kt:153-168`, Delete in overflow menu `:117-131` + confirm dialog `:98-112` | documents list | delete → back |
| `offer?type={type}` (`MainRoute.kt:91-103`) | `CredentialOfferScreen` (shared with boarding) | Issuer disclosure + Continue; `VerifiedParty()` at `CredentialOfferScreen.kt:129` | documents FAB | `issuance?type=` `MainNavGraph.kt:137-145` |
| `issuance?type=&issueMock…` (`MainRoute.kt:105-132`) | `IssuanceScreen` (shared) | Retrieve + add credential; PIN gate `CONFIRM_ISSUANCE` `IssuanceScreen.kt:124-126` | offer screen | back to documents |
| `presentation?uri=&pin=` (`MainRoute.kt:51-86`) | `PresentationScreen` `presentation/PresentationRequestScreen.kt:183`, `Presentation` `:224`, `PresentationContent` `:323`, `SuccessContent` `:254`, `LoadingContent` `:314`, `PresentationRequestCard` `:398`, `PresentationOptionalField` `:450`, `PresentationField` `:424` | Remote (OpenID4VP) consent screen: requester name, verified badge, channel notice, per-attribute disclosure switches, Cancel / Share | QR scanner (`MainNavGraph.kt:66-72`), 7 deep links, DC-API refusal path | success auto-closes in 5 s (`:251`); error → `error?error=` `MainNavGraph.kt:96-104` |
| `proximity` (`MainRoute.kt:89`) | `ProximityScreen` `proximity/ProximityScreen.kt:61`, `Proximity` `:70`, `ProximityQrCodeContent` `:271`, `ProximityRequestContent` `:144`, `ProximityRequestNoMatchContent` `:120`, `NoPermissionContent` `:306` | BLE/QR proximity: engage screen, then consent screen; PIN gate `:82-88, 97-99` | dashboard action | back on response/cancel |
| `scanner` (`MainRoute.kt:88`) | `ScannerScreen` (shared) | Scan a verifier QR | dashboard action | `presentation` with `ARG_URI` |
| `activity_logs` (`MainRoute.kt:146`) | `ActivityLogScreen` `activity/ActivityLogScreen.kt:67`, content `:73`, `ActivityItem` `:105` | Transaction log list with linkable/unlinkable tier summary `:88-93` | dashboard nav | `activity_log?id=` `MainNavGraph.kt:216` |
| `activity_log?id={id}` (`MainRoute.kt:147-157`) | `LogEntryScreen` `activity/LogEntryScreen.kt` (218 lines) | Single disclosure record detail | log list | back |
| `settings` (`MainRoute.kt:32`) | `SettingsScreen` `settings/SettingsScreen.kt:60`, content `:99`, delete-all confirm `:87` | Language row `:113`, BLE mode `:114`, trust-all-validator (debug-only) `:115-117`, DC-API disclosure `:118`, LOTL status `:119-121`, wipe `:123` | dashboard nav | `language` `MainNavGraph.kt:194` |
| `language` (`MainRoute.kt:36`) | `LanguageScreen` `settings/language/LanguageScreen.kt:40`, content `:54` | Language picker (EN/ET/RU) | settings | back |
| `error?error=` (`MainRoute.kt:134-144`) | `AppErrorScreen` (shared) | Error card + Close | presentation/issuance failures | back |
| `mock_pid_authentication` (`MainRoute.kt:159`) | `PidAuthenticationScreen` `boarding/PidAuthenticationScreen.kt:37` | Mock consent screen showing the stored PID to a hardcoded `tara.ria.ee` `:47` | — | `mock_pid_sharing_consent_pin` `MainNavGraph.kt:243` |
| `mock_pid_sharing_consent_pin` (`MainRoute.kt:160`) | `PinEntryScreen` reused — `MainNavGraph.kt:243-266` | Mock share-consent run | mock PID screen | `mock_pid_sharing_success` |
| `mock_pid_sharing_success` (`MainRoute.kt:161`) | `SuccessContent` reused — `MainNavGraph.kt:267-276` | Mock success | mock consent | issuance (`issueMockMdl`) |

### Out-of-graph surfaces (Activities)

- `PinActivity` — `pin/PinActivity.kt:21`, `@AndroidEntryPoint`, own `setContent` + `WalletTheme` (hardcoded `isSystemInDarkTheme()`, `:32`). Hosts `PinEntryScreen` `PinEntryScreen.kt:147` and, after creation, `PinCreatedScreen` `:181`. Returns `Result.Success(pin)` / `Result.Failure` / `Result.Cancelled` via `PinActivityResultContract` (`pin/PinActivityResultContract.kt`, `PinActivity.kt:64-70`). Entered from Welcome, Presentation, Issuance, Proximity, and the mock-consent route.
- `DigitalCredentialsActivity` — `dcapi/DigitalCredentialsActivity.kt:21` region (97 lines), also `WalletTheme(useDarkTheme = isSystemInDarkTheme())` `:34`. Screen: `DigitalCredentialsScreen.kt:47`; `RefusedPlainContent` `:79`, `LoadingContent` `:100`, `NoMatchContent` `:114`, `DcPresentationContent` `:135`, `DcPresentationRequestCard` `:245`, `DcPresentationOptionalField` `:279`. Launched by the Android Digital Credentials API framework, not by the wallet's NavHost. Consent surface shows origin + `VerifiedParty()` `:156` and, when the certificate subject is available, `readerSubject` below the badge with an explicit comment that the certificate's trust is *not* validated there (`:153-157`, `DigitalCredentialsViewModel.kt:207-211, 247, 312, 761, 772`).

### Prompt / dialog host

- `ui/prompt/PromptDialogHost.kt` — singleton `PromptDialogHost` `:41`, `suspend fun launch` `:55-79`, `showForUnlock` `:88-149`. Only `BiometricPromptDialogModel` has a host (`:56`); every other multipaz prompt kind — including the consent prompt — throws `PromptUiNotAvailableException` (`:58, :60`), documented at `:38-39` as "NFC scan, passphrase, consent) has no host in this PoC and reports unavailable the same way". The prompt is bound to the key operation via `CryptoObject` (parameter `:89`, passed `:71`, `prompt.authenticate(promptInfo, cryptoObject)` `:140`) and fails closed on cancel/error (`:116-121`, `:143-145`).

---

## (b) Theming

- **Design system: Compose Material 3.** `androidx.compose.material3.*` throughout; `MaterialTheme(colorScheme = LightColors, content = content)` — `ui/theme/Theme.kt:64-67`. View system in use: Jetpack Compose with Navigation-Compose + Hilt; `AppCompatActivity` hosts (`MainActivity.kt:38`, `PinActivity.kt:21`).
- **Theme entry point:** `WalletTheme` — `ui/theme/Theme.kt:50-68`. Also `WalletThemePreviewSurface` `:70-77`, preview annotations `PreviewThemes` `:80-81` and `PreviewThemesSmallScreen` `:83-84`.
- **Dark mode: effectively absent.** `WalletTheme(useDarkTheme: Boolean = isSystemInDarkTheme())` `Theme.kt:52` accepts the flag but **never uses it** — `MaterialTheme` always receives `LightColors` (`:65`). `LightColors = lightColorScheme(...)` `:17-48`. No dark colour scheme exists. Only a light `/values` resource config is present (no `values-night/`).
  - The *preference* exists end-to-end: `DarkThemeConfig` enum `ui/model/UserPreferences.kt:3`, persisted by `UserPreferencesDataSource.setDarkThemeConfig` `data/datastore/UserPreferencesDataSource.kt:24-36`, honoured **only** by `MainActivity.shouldUseDarkTheme` `MainActivity.kt:95-103`. `PinActivity.kt:32` and `DigitalCredentialsActivity.kt:34` bypass it with `isSystemInDarkTheme()`.
  - Status bar colour is hardcoded to the light primary: `window.statusBarColor = LightColors.primary.toArgb()` `Theme.kt:59`.
- **Colour tokens** — `ui/theme/Colors.kt:5-17`: `blue_600` `#005AA3`, `natural_200` `#F0F0F2`, `natural_900` `#131317`, `natural_800` `#30323B`, `natural_700` `#454854`, `natural_600` `#686B78`, `white` `#FFFFFF`, `red_600` `#D73E3E`, `red_700` `#AC3232`, `yellow_600` `#FFB511`, `green_600` `#308653`, `pink` `#FB7185`, `colorsBlueBlue300` `#38BDF8`.
- **Role mapping** (`Theme.kt:17-48`): `primary=blue_600`, `onPrimary=white`, `primaryContainer=natural_200`, `onPrimaryContainer=natural_900`, `background=natural_200`, `onBackground=natural_900`, `secondary=white`, `onSecondary=natural_900`, `surface=natural_200`, `onSurface=natural_900`, `surfaceVariant=white`, `onSurfaceVariant=natural_900`, `surfaceTint=white`, `tertiary=white`, `onTertiary=natural_900`, `error=red_600`, `onError=white`, `errorContainer=red_600`, `onErrorContainer=white`, `scrim=natural_600`.
  - Commented out at `:38-46` and therefore falling through to M3 defaults: `secondaryContainer`, `onSecondaryContainer`, `tertiaryContainer`, `onTertiaryContainer`, `outline`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, `outlineVariant`. **There is no semantic "warning"/"caution" role** — `yellow_600` exists as a raw token (`Colors.kt:14`) and is not mapped to any scheme role.
- **Typography: absent.** No `Typography(...)` definition anywhere in `app/src/main/kotlin` (verified by search). All screens use the Material 3 default type scale directly. Styles actually used: `displaySmall` (`PresentationRequestScreen.kt:290`), `headlineMedium` (requester name, success titles — e.g. `:332`, `:316`), `titleLarge` (`AppHeader.kt:69`), `titleMedium` (`PinEntryScreen.kt:239`), `bodyLarge`, `bodyMedium`, `bodySmall`, `labelMedium`. Font weights are set ad hoc via `fontWeight = FontWeight.SemiBold/Bold` rather than by token.
- **Shape / elevation:** no custom `Shapes`, no custom elevation tokens — M3 `Card` defaults used (`ActivationScreen.kt:80`, `ActivityLogScreen.kt:106`, `DocumentScreen.kt:155`).
- **Spacing:** ad hoc `dp` literals with two helpers — `VSpace` / `HSpace` / `WSpace` (`components/Spaces.kt`), plus a repeated `padding(horizontal = 16.dp, vertical = 24.dp)` content padding inside `AppContent` (`components/AppContent.kt:55-64`). No spacing scale token.
- **Shared chrome:** `AppContent` (header slot + centred padded column, `AppContent.kt:50-64`), `AppHeader` (logo row, `AppHeader.kt:33-42`), `SimpleNavigationHeader` (back arrow + title + overflow menu, `AppHeader.kt:59-74`), `Menu`/`MenuItem` (`components/Menu.kt:26-110`), `PrimaryButton`/`SecondaryButton` (min 160×48 dp, `components/Buttons.kt:18-65`), `HDivider` (`components/Divider.kt`), `ErrorCard` (`components/Error.kt`), `NothingToShow` (`components/NothingToShow.kt`), `FadedProgressIndicator` / `FullScreenFadedScrimProgressIndicator` (`components/ProgressIndicators.kt`), `ContentAlpha` / `ContentColor` (`ui/util/`).

---

## (c) Existing consent / authorisation / prompt patterns

### 1. The consent screen is one repeated hand-rolled layout, not a component

The remote path, proximity path and DC-API path all render the same shape, copy-pasted: requester name (`headlineMedium`, SemiBold) → `VerifiedParty()` badge → notice copy → per-attribute `Switch` list → footer note → Cancel / Share.

- Remote: `PresentationRequestScreen.kt:330-393` (name `:330-334`, badge `:335`, channel notice `:340-351`, "following data" line `:353-357`, card `:359-362`, footer `:364-371`, last-age-attestation notice `:374-384`, buttons `:387-393`).
- Proximity: `ProximityScreen.kt:151-198` (name `:151-155`, badge `:156`, "following data" `:158-162`, card `:164-167`, footer `:169-176`, last-age-attestation notice `:179-189`, buttons `:192-198`). Duplicated again in the no-match variant `:127-139`. `PresentationRequestCard` / `PresentationOptionalField` are **duplicated** in `ProximityScreen.kt:203-268`, not shared with `PresentationRequestScreen.kt:398-489`.
- DC API: `DigitalCredentialsScreen.kt:146-244` (badge `:156`, `readerSubject` `:157-164`, per-field rows `:245-311`, Cancel / Share).

### 2. Verified-party treatment: **the unverified state is absent**

- `components/VerifiedParty.kt:18-31` — hardcoded: `Icons.Outlined.VerifiedUser` at `16.dp`, tint `primary`, text `R.string.presentation_verified_party`. It takes **no parameters** and has no unverified/suspect variant.
- Rendered unconditionally on every requester surface: `PresentationRequestScreen.kt:335`, `ProximityScreen.kt:132` and `:156`, `DigitalCredentialsScreen.kt:156`, `CredentialOfferScreen.kt:129`, `PidAuthenticationScreen.kt:51`.
- Strings: `presentation_verified_party` = "Verified party" (`values/strings.xml:83`), ET "Autenditud osapool" (`values-et/strings.xml:83`). There is **no** string for an unverified/unknown requester anywhere in `values/` or `values-et/` (searched for `verified`, `unverified`, `unvalidated`, `trust` in both files and in `AppError.kt`).
- The comment at `DigitalCredentialsScreen.kt:153-157` is the only place in the UI that admits the badge may over-claim; it is a code comment, not a user-facing state.

### 3. Where a requester name comes from

- Remote: `PresentationRequestViewModel.kt:425` `verifierName() = client.legalName() ?: client.id`; written into state at `:382`, `:385`, `:409`, `:439`. `UiState.verifier: String = ""` `:65`; `UiState.scanned` records QR-scanner origin `:66-68`.
- Proximity: `ProximityViewModel.kt:474` `val verifier: String = ""` and **nothing ever assigns it** (searched `verifier` across the proximity package: only reads in the screen and this default). The proximity consent screen therefore renders an empty `headlineMedium` line above the "Verified party" badge. The PIN screen it launches is given the literal `PROXIMITY_READER_NAME = "Proximity reader"` (`ProximityViewModel.kt:454`, used `:363`).
- DC API: `readerSubject = readerAuthSubject(docRequests)` `DigitalCredentialsViewModel.kt:207`, gated by `readerAuthenticated && readerChainTrusted` `:211`; both the subject and the trust flag reach state (`:247`, `:312`, `:761`, `:772`).
- Proximity reader trust inputs exist but no anchors are configured: comment at `ProximityViewModel.kt:257-258` "No reader trust anchors are configured yet, so every reader is unauthenticated here"; the age policy reads `readerAuthenticated && readerChainTrusted` at `:265-266`.
- Credential-offer / mock-PID screens display a name with **no** validation input at all (`CredentialOfferScreen.kt:111-129` — issuer name from `issuerName()` at `:123`; `PidAuthenticationScreen.kt:46-51` — the latter hardcodes `tara.ria.ee` `:47`).

### 4. PIN consent gate

- `PinFlow` = `CREATE`, `CONFIRM_PRESENTATION`, `CONFIRM_ISSUANCE` (`pin/PinEntryViewModel.kt` region `:26-45`; `PinActivity.kt:38-42`). `PinData.ConfirmPin` carries `party` and `attemptsLeft` `:39-45`.
- Presentation: `PresentationRequestScreen.kt:184-193` launcher; `Result.Success → Event.UserAuthenticated`, `Result.Cancelled → Event.OnCancelClicked` (note: the event is constructed and discarded at `:188`), `Result.Failure → Event.IncorrectPin`. Gate raised from `PresentationRequestViewModel.kt:229-235` (`onShareClicked` sends `Effect.AuthenticateWithPin(state.value.verifier)` when `!authenticated`); `authenticated` is a per-process `MutableStateFlow(false)` `:187`.
- Proximity: `ProximityScreen.kt:82-88` launcher (`Success → UserAuthenticated`, `Cancelled → Unit` (session kept), `Failure → IncorrectPin`); gate at `ProximityViewModel.kt:361-365` with the same per-process `authenticated` flag; failure path deliberately keeps the BLE session `:346-351`.
- Issuance: `IssuanceScreen.kt:96-103, 124-129` (`Cancelled, Failure → onBackClicked()`).
- PIN screen UI: `PinEntryScreen.kt:147` (host), `PinEntry` `:206`, `PinEntryContent` `:227` (title from `PinData`, party on its own line `:239`), `NumPadView` `:255`, `Dots` `:297`, `NumPad` `:312`, error text `:438-443`. `PIN_LENGTH = 6` (`PinEntryViewModel.kt:25`), `TOTAL_ATTEMPTS = 3` `:26`.
- Creation flow: 6-digit entry → confirmation screen → `PinCreatedScreen` `PinEntryScreen.kt:181`; `PinPolicy.MIN_LENGTH = 6` and digits-only (`security/PinVerifier.kt:101-106`).

### 5. Lockout state — persisted, but **no UI reads it** (load-bearing for FR-47)

- Storage layer already implements durable lockout: `security/PinVerifier.kt` — `PinRecord(hash, salt, failedAttempts, lockedUntil)` `:13-18`; `isLocked` `:45`; `verify` returns `false` **even for the correct PIN** while locked `:51-53`; `MAX_ATTEMPTS = 5` `:85`; `LOCK_DURATION_MS = 30 min` `:86`. Doc comment `:10-11`: "Stored in the encrypted user-session DataStore, so the lockout survives process death".
- Durable write: `data/datastore/UserSessionDataSource.kt:35-44` — `dataStore.updateData { … }` persists the updated record *before* `verifyPin` returns; columns written at `:57-63`. `setPin` `:25-29`, `clearAll` `:46-48`.
- Where the UI uses it: exactly one call site — `PinEntryViewModel.kt:137` `userSessionDataSource.verifyPin(entered)` inside the confirm branch, after a `delay(1000)` marked `// FIXME: do real remote call?` `:130-131`.
- What the UI shows: its **own** in-memory counter `attemptsLeft` starting at `TOTAL_ATTEMPTS = 3` (`PinEntryViewModel.kt:26, 39-45`), decremented at `:194-208`; at 0 it emits `Effect.Result.Failure` `:205-207`. Error copy is `pin_incorrect_pin_attempts_left` = "Incorrect PIN. Attempts left: %1$s" (`values/strings.xml:72`; ET at `values-et/strings.xml:72`).
- Gaps that are **absent** in the UI layer: no read of `PinVerifier.isLocked` / `lockout` state (searched: only `PinVerifier.kt` and `UserSessionDataSource.kt` reference `isLocked`/`lockedUntil`/`MAX_ATTEMPTS`); no string mentioning a lock, a wait, or a remaining time (`lock`, `try again` searches return only `pin_pins_don_t_match_please_try_again` and `presentation_close_and_try_again`); no app-entry lock screen (`WalletApp.kt:100-108` only branches on `isActivated`); no recovery path beyond a failed navigation.
- Counter mismatch to note: UI stops at 3 attempts, storage escalates at 5 — so the app can emit `Result.Failure` while the persisted record is not yet at its threshold, and a locked record can still be presented as "Incorrect PIN" with a smaller number.
- Failure → navigation: presentation `Event.IncorrectPin → logTransaction + onCancel()` (`PresentationRequestViewModel.kt:172-175`) → `Effect.Back.Cancel` → `navigateUp`; proximity `:346-351` (no navigation); issuance `IssuanceScreen.kt:99-102` → `onBackClicked()`.

### 6. Platform user-presence / biometric prompt

- `ui/prompt/PromptDialogHost.kt:88-149` is the app's whole prompt host: `showForUnlock` builds a `BiometricPrompt`, passes the `CryptoObject` into `prompt.authenticate(promptInfo, cryptoObject)` (`:140`) so the key operation depends on the prompt, and returns `false` on cancel/error → the call site raises `KeyLockedException` (`:34-36`, `:84`, `:116-121`, `:143-145`). Reason copy: for a multipaz `PresentmentUnlockReason` the wallet defers to multipaz's own localized `key_unlock_present_*` resources (`:151-154` — and grep finds no such strings in this app's `res/`, so the wrapper has no localisation control there); for a plain `Reason.HumanReadable` the title/subtitle pass through, and the `else` branch falls back to hardcoded English "Unlock your document" / "Authenticate to share this document" (`:156-159`).
- Wiring: `WalletApplication.kt:58-83` registers the `AndroidPromptModel` so the sign path prompts instead of throwing `PromptModelNotAvailableException`; documented as the deliberate fail-closed direction (`:61-62`, `:70`).
- Key side: `security/SecureAreaKeyManager.kt:155+` (`AndroidKeystoreSecureArea.sign(keyId, data, reason)`), `security/SecureAreaCOSECryptoProvider.kt:75`.
- Distance from the key operation: proximity signs off the main thread to avoid deadlock with the prompt (`ProximityViewModel.kt:398-408`) and any failure cancels the transfer (`:419-426`, `Effect.ProximityCancel` → `onCancel()` in the screen `:96`); the reason the prompt appears is not surfaced to the user in the wallet UI.
- The PIN gate and this prompt are **two separate gates**: PIN is application-level (`verifyPin` in `PinEntryViewModel`), the platform prompt is bound to the key. Nothing in the UI explains the second one.

### 7. Other permission / confirmation patterns

- `components/Dialogs.kt:32-62` — `ConfirmationDialog(title, text, confirm/cancel button text, cancellable)`, M3 `AlertDialog`. Used for document delete (`DocumentScreen.kt:98-112`) and delete-all wallet data (`SettingsScreen.kt:76-96`).
- `components/ExitConfirmation.kt:18-32` — double-back-to-exit via a `Toast` with `exit_confirmation` = "repeat to exit!" (ET `:14`).
- Camera permission via accompanist `requestPermission` (`Scanner.kt:57`), with a `CameraSettingsCard` fallback (`Scanner.kt:91-101`); BLE `BLUETOOTH_CONNECT` / `BLUETOOTH_ADVERTISE` (`ProximityScreen.kt:75-76`) with `NoPermissionContent` (`:306-316`).
- Settings uses `Switch`-style toggles as standing consent-like controls: BLE peripheral mode (`SettingsScreen.kt:185-211`, switch `:205`), trust-all validator (debug builds only, `:214-240`, switch `:234`), DC-API disclosure "Share documents with requesting websites" (`:247-273`, switch `:267`, persisted at `UserPreferencesDataSource.kt:73-81`), LOTL status row (`:276+`).
- Consent copy is not one string: the remote path adds a channel notice (`presentation_channel_scanned_notice` / `presentation_channel_link_notice`, `PresentationRequestScreen.kt:347`), a footer note (`presentation_footer_note`, `:369`), and a ZK-tier last-attestation notice (`:381`). Proximity renders only the footer note and the last-attestation notice (`ProximityScreen.kt:174, 186`).

---

## (d) Localisation reality

- `app/src/main/res/values/strings.xml`: **269 lines, 223 `<string>` entries.**
- `app/src/main/res/values-et/strings.xml`: **269 lines, 223 `<string>` entries**; key sets are byte-identical (`diff` of all `name="…"` lists is empty). This is a complete translation, not a stub.
- `app/src/main/res/values-ru/strings.xml`: **7 lines, 4 `<string>` entries** — the whole file:

  ```xml
  <resources>
      <!-- Language -->
      <string name="language_title">Язык</string>
      <string name="en_localized">Английский</string>
      <string name="et_localized">Эстонкий</string>
      <string name="ru_localized"/>
  </resources>
  ```
  So: only the language-screen chrome is translated; `et_localized` carries a typo ("Эстонкий" for "Эстонский") and `ru_localized` is empty. Every other string under `ru` falls back to the default `values/` (English) by Android resource fallback. `delete_btn` is also untranslated in ET ("Delete", `values-et/strings.xml:6`) — a translation gap inside the otherwise complete ET file.
- Locale plumbing: `LanguageResource` enum EN/ET/RU with `tag`, `resId`, `localizedResId` — `ui/util/LanguageResource.kt:8-20`. `AndroidLocaleManager` — `domain/AndroidLocaleManager.kt`: `supportedLocales` from `supportedLanguages` (fallback `en`) `:14`, `getApplicationLocale()` `:16-22`, `setApplicationLocale()` via `AppCompatDelegate.setApplicationLocales` `:24-27`. Persistence is AndroidX per-app locale, not DataStore.
- Locale-switching UI exists in exactly two places: the Welcome screen language cards (`WelcomeScreen.kt:111-123`, with two `workaround for Android 10` comments `:78`, `:81`, `:98`) and Settings → Language (`LanguageScreen.kt:69-76`; entered from `SettingsScreen.kt:113`). `LanguageViewModel` keeps `selected` derived from the current app locale (`LanguageViewModel.kt:36-41`) and switches at `:50-57`.
- Locale-specific assets exist: `res/drawable/` has `funding_logo.png`, `placeholder_logo.png`, `logo.png`; `res/drawable-et/` overrides `funding_logo.png` and `placeholder_logo.png`.
- No RTL handling, no per-screen locale override, no locale in the consent screens' copy beyond the shared strings above.

---

## (e) Onboarding wizard shape (what is asked at each step)

1. **Welcome** — language choice + "Create PIN" (`WelcomeScreen.kt:86-126`).
2. **Create PIN** — 6-digit PIN, entered twice; then `PinCreatedScreen` (`PinActivity.kt:31-58`, `PinEntryScreen.kt:181`, `PinEntryViewModel.kt:149-191`). Creation also registers the wallet instance (`PinEntryViewModel.kt:176-184`).
3. **Activation** — "Continue on this device" vs "Continue on another device" (`ActivationScreen.kt:64-74`). Mocks route same-device to the demo auth list; real builds open `AppConfig.pidProviderUrl` externally (`BoardingNavGraph.kt:54-64`).
4. **Mock identity auth (same device)** — method list (ID-card+PIN, biometry, Mobile-ID, on-premise; only Mobile-ID live, `AuthenticationScreen.kt:65-79`) → phone + personal code, prefilled with demo values (`:84-128`) → control code screen showing "1234" (`:131-190`) → numpad PIN on a black screen (`:193-220`) → success (`:223-245`). The personal code entered is stored to `SharedPreferences` as the mock PID (`PidAuthenticationViewModel.kt:24, 32`).
5. **Cross-device** — 4-step tutorial (`CrossDeviceTutorial.kt:38-43, 56-80`) → scan the issuer QR (`Scanner.kt`) → issuance with the scanned binding token.
6. **Credential offer** (either path or a `haip://pid` deep link) — issuer name + verified-party badge + the fields being offered, Continue / Cancel (`CredentialOfferScreen.kt`).
7. **Issuance** — progress while retrieving, then the credential preview card and "Add to wallet" (`IssuanceScreen.kt:141-185`). In the boarding graph Continue is a no-op (`BoardingNavGraph.kt:81`); the run reaches the main graph only once a document exists (`WalletApp.kt:37-39, 102`).

Re-entry shape (relevant to "survives restart"): a restart mid-wizard lands on **Activation** whenever `isPinCreated == true` even with zero documents (`BoardingNavGraph.kt:40` + `WalletApp.kt:38-39`).

---

## (f) Persistence patterns reachable from the UI layer (lockout-relevant)

- **User session (the PIN + lockout store):** `data/datastore/UserSessionDataSource.kt` over a DataStore proto `UserSessionProto`. Fields used: `pinHash`, `pinSalt`, `failedAttempts`, `lockedUntil` (`:50-63`); legacy plaintext `pin` field is migrated and cleared (`:52-53, :58`). `verifyPin` writes and returns atomically (`:31-44`). Consumed by exactly one UI ViewModel: `PinEntryViewModel.kt:12, 137`. Model surface for the app: `ui/model/UserSession.kt` → `isPinCreated` consumed by `WalletApp.kt:38`.
- **User preferences:** `data/datastore/UserPreferencesDataSource.kt` over `UserPreferencesProto` — `bleMode`, `trustAllValidator`, `dcApiDisclosureDisabled`, `darkThemeConfig`, `issuerKeyType`; writes `:24-81`. Consumed by `SettingsViewModel.kt:9, 53-62, 79-101` and `ProximityViewModel.kt:12, 334-338`.
- **SharedPreferences:** only the mock PID demo — `PidAuthenticationViewModel.kt:24, 32` (`MOCK_APP_PREFS` / `MOCK_USER_PID`). Not used for any real gate state.
- **Room:** activity log (`data/database`, `LogEntryEntity`), read by `ActivityLogViewModel`/`LogEntryScreenViewModel`. No gate state.
- **Keystore / SecureArea:** key material and the user-presence prompt path (`security/SecureAreaKeyManager.kt`, `WalletApplication.kt:58-83`). No persisted counter.
- **In-memory only (reset on restart):** `PresentationRequestViewModel.authenticated` (`:187`), proximity `authenticated`, the PIN screen's `attemptsLeft` (re-created at each `PinActivity` launch, `PinActivity.kt:39-42`), all `Effect`/`UiState` flags, and `savedStateHandle` state.
- **Net position for FR-47:** the durable half (count + `lockedUntil` in an encrypted DataStore, written before the answer is returned) already exists; the absent half is UI: no read of the persisted lockout, no lockout copy in any locale, no lock/remaining-time state, no recovery screen, and no app-entry gate that would consult it at launch.

---

## (g) Where FR-45 … FR-48 would plausibly land (evidence-anchored)

FR text read from `_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md` — FR-45 `:999-1010`, FR-46 `:1012-1022`, FR-47 `:1024-1033`, FR-48 `:1035-1044`.

### FR-45 — an unvalidated requester must never be shown as a verified name
- **Existing screens that must change:** `PresentationRequestScreen.kt:330-335` (remote), `ProximityScreen.kt:151-156` and `:127-132` (proximity + no-match), `DigitalCredentialsScreen.kt:146-164` (DC API), `CredentialOfferScreen.kt:122-129` (issuer), `PidAuthenticationScreen.kt:46-51` (mock).
- **New screen needed: no.** The work is (i) `components/VerifiedParty.kt:18` gaining a verified/unverified state (it has none today) and (ii) a conditional around the requester-name block on each consent surface, plus refusal-by-default copy.
- **Data available today:** remote `verifierName()` + the resolved request object (`PresentationRequestViewModel.kt:382-425`); DC API `readerSubject` + `readerChainTrusted` (`DigitalCredentialsViewModel.kt:207-211, 761, 772`); proximity `readerAuthenticated`/`readerChainTrusted` per `DeviceRequest` (`ProximityViewModel.kt:265-266`) but with no anchors configured (`:257-258`); offer and mock-PID surfaces have **no** validation input at all.
- **Gaps the design must name:** no unverified string in any locale; `state.verifier` is empty on the proximity path; the proximity/DC-API card code is duplicated rather than shared, so a single state change cannot be made in one place.

### FR-46 — device-key use needs a user-presence prompt bound to the key operation
- **Existing surfaces:** the platform prompt host `ui/prompt/PromptDialogHost.kt` (bound via `CryptoObject`, fail-closed `:34-36`, `:116-121`, `:140`) and the app-level PIN gate (`PinEntryScreen.kt`, `PinActivity.kt`).
- **New screen needed: no.** The UX-visible work is the prompt's reason copy — currently hardcoded English at `PromptDialogHost.kt:156-159`, therefore untranslated — and what the user is told when the key operation fails: a cancel surfaces as `KeyLockedException` and today aborts the flow (`WalletApplication.kt:61-62, 70`; proximity `ProximityViewModel.kt:378-380, 419-425` → `Effect.ProximityCancel` → `onCancel()` with no message). Two gates (PIN, then biometric) exist back-to-back with no UI explaining the second.
- **Reachability to check for the "ungated path removed" consequence:** which presentation/issuance paths reach a sign without the prompt, and whether the PIN callback's reconstructed-but-discarded event at `PresentationRequestScreen.kt:188` matters.

### FR-47 — the consent gate must resist guessing, survive restart, and say what happens
- **Existing screen that must change:** the PIN screen — `PinEntryScreen.kt` (title/party region `:227-251`, `NumPadView` `:255`, error copy `:438-443`) and its state holder `PinEntryViewModel.kt` (`attemptsLeft` `:39-45`, decrement `:194-208`, `Result.Failure` `:205-207`, the `delay(1000)` "FIXME: do real remote call?" `:130-131` that FR-47's text targets).
- **New screen needed: no, on the evidence.** Storage already satisfies the persistence half (`PinVerifier.kt:13-18, 45-64, 85-86`; `UserSessionDataSource.kt:31-44`). What is absent is any UI state, copy or recovery that reads `failedAttempts`/`lockedUntil`: no lockout string in `values/` or `values-et/`, no "try again in N minutes" surface, no app-lock screen at launch (`WalletApp.kt:100-108`), and a UI counter (3) that disagrees with the enforced threshold (5 attempts / 30 minutes).
- **Recovery path today:** nothing — presentation failure navigates back (`PresentationRequestViewModel.kt:172-175`), proximity logs and keeps the session (`ProximityViewModel.kt:346-351`), issuance returns to the previous screen (`IssuanceScreen.kt:99-102`). A recovery path would be new behaviour on the existing PIN screen (or a new state within it).

### FR-48 — a proximity share carries the same consent as a remote one
- **Existing screen that must change:** `ProximityScreen.kt` — consent content `:144-200`, no-match variant `:120-141`, engagement/QR content `:271+`, PIN gate `:82-88, 97-99`.
- **New screen needed: no.**
- **Observed divergence from the FR text worth flagging to the design run:** FR-48 states the proximity path "emits with no gate at all" (prd.md `:1041-1042`). In this checkout the proximity share **is** PIN-gated — `ProximityScreen.kt:82-88` + `ProximityViewModel.kt:340-365`, commented "D12 (mobile F8): the share is PIN-gated", with the refusal logic at `:361-365` and the deliberate "session stays alive" branch at `:346-351`. What the proximity consent surface still lacks versus the remote one is: a real requester identity (name empty, `ProximityViewModel.kt:474`), an authenticated-reader state (no anchors, `:257-258`), and the remote path's extra notices (channel notice and ZK-tier notice are absent; `ProximityScreen.kt:174, 186` only).
- **Shared-coverage risk:** the consent card internals are duplicated (`ProximityScreen.kt:203-268` vs `PresentationRequestScreen.kt:398-489`), so "the same terms as the remote path" cannot be achieved by editing one component.

---

## Loose ends observed while reading (not requested, kept because they affect UX work)

- Request/state classes are `@Parcelize`d into `savedStateHandle` (`PresentationRequestViewModel.kt:51-105, 127, 153`) — process death restores the consent screen's field selection but not the gate's `authenticated` flag (`:187`).
- Error enum for user-visible failures: `domain/AppError.kt:7-28` (21 entries, each with a `@StringRes`), e.g. `PRESENTATION_INCORRECT_PIN_ERROR :23`, `USER_CANCELLED :28`. There is no lockout/rate-limit error value.
- `Effect.AuthenticateWithPin(party)` carries the party string into the PIN screen title region, which is where a proximity user currently sees the literal "Proximity reader" (`ProximityViewModel.kt:454` → `PinEntryViewModel.kt:39-45` → `PinEntryScreen.kt:239`).
- Mock surfaces that are user-visible and labelled as such: "DEMO" (`AuthenticationScreen.kt:54`), black full-screen Mobile-ID screens (`:131-220`), mock PIDs hardcoded in the UI (`:85-86`, `PidAuthenticationScreen.kt:47`).
- No UI test source set exists (`app/src/test/kotlin/ee/cyber/wallet/test` holds unit tests only) — any FR-45…FR-48 "a test asserts" consequence has no existing UI harness to extend.
