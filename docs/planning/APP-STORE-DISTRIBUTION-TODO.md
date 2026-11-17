# TODO — App store distribution

Written 2026-11-04. Status: analysis done, not started.

**Verdict.** Google Play is feasible only as a clearly labelled ZK age-proof *demo*, not as an identity
wallet. The Apple App Store is out of scope: there is no iOS app, so it would be a port, not a submission.

**Goal.** A stranger can try the ZK age proof on their own phone in two minutes, without building
anything and without the app impersonating a state ID.

## Blockers found in the repo (2026-11-04)

- [ ] **No release build.** `app/build.gradle.kts` disables the `release` build type, and `PROD`
      points at `https://eudi-wallet-provider.example.com`, so a release build fails onboarding like
      `debug` does.
- [ ] **The only working variant can't be uploaded.** `local_mocks` inherits `isDebuggable = true`, and
      Play rejects debuggable builds.
- [ ] **Issuer private keys ship in the APK.** `app/src/local_mocks/assets/keys/doc_signer_pid_issuer.p12`
      and `doc_signer_mdl_issuer.p12`. On a public store, anyone can unpack the APK, take the keys and
      sign PIDs that any verifier trusting the bundled IACA root will accept. An app that issues itself
      a government ID also invites Play's Impersonation policy
      ([answer 9888374](https://support.google.com/googleplay/android-developer/answer/9888374)).
      **This is the real blocker.**
- [ ] **Branding.** The app is named "EUDI Wallet PoC (independent)", and the fork's README shows the
      Potential "Co-funded by the EU" logo even though the fork has no funding. eIDAS 2 also restricts
      use of the EU Digital Identity Wallet name and trust mark; the exact article is `[UNVERIFIED]`.
- [ ] **Account gate.** A personal Play developer account created after 2023-11-13 must run a closed
      test with at least 12 testers opted in continuously for 14 days before it can publish to
      production ([answer 14151465](https://support.google.com/googleplay/android-developer/answer/14151465),
      checked 2026-11-04).
- [ ] **Store paperwork.** Data safety form, privacy policy URL, and reasons for the CAMERA, Bluetooth
      and location permissions (location is only there for the legacy Bluetooth permission on older
      Android versions).

Already fine: ABIs `arm64-v8a`/`x86_64`, `minSdk 31`, `targetSdk 35`. Play's target API level
requirement moves up each year, so recheck `targetSdk` at submission time.

## The plan, from least to most ambitious

1. [x] **Verdict and blockers.** This note.
2. [ ] **A `sandbox` build Play will accept.**
   - Release-signed through the existing `RELEASE_*` properties, not debuggable, R8 on.
   - No issuer keys in the APK: issuance comes from the public sandbox issuer (zk.proksiabel.ee),
     which mints synthetic personas.
   - Rename it (for example "ZK Age Proof Demo"), drop "EUDI" and the EU emblem, and label every
     screen "demo, not a legal ID".
3. [ ] **Install without asking the store's permission.**
   - Signed APK on GitHub Releases, installable with Obtainium. Maybe F-Droid; whether the
     dependency tree meets F-Droid's free-software rules is `[UNVERIFIED]`.
   - Register for Android developer verification. It has been required in Brazil, Indonesia,
     Singapore and Thailand since 2026-09-30 and goes global in 2027
     ([Android Developers blog](https://developer.android.com/developer-verification/guides)).
     Without it, sideloading on certified devices stops working too.

## Judge (what counts as a pass)

- `./gradlew :app:bundleSandboxRelease` succeeds.
- `aapt dump badging` shows the app is not debuggable.
- `unzip -l` on the AAB finds no `.p12`.
- An installed build completes issuance and a ZK age proof against zk.proksiabel.ee.
- Play's pre-launch report comes back clean, and the closed test is accepted after 14 days.

## Needs

| Item | State |
|---|---|
| Code, sandbox issuer, signing hooks | There |
| Play Console account ($25, maintainer's identity) | Missing |
| 12 closed-test testers | Missing |
| Privacy policy URL | Missing |
| Sandbox issuer reachable from phones (currently runs from the laptop) | Missing; redeploy to asus first |

## Next step

Step 2. It changes the app's name and branding, and it only pays off once the sandbox issuer is
hosted somewhere other than the laptop, so get the maintainer's go-ahead first.
