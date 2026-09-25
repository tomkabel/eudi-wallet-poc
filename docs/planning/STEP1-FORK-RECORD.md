# Step 1 record: the fork, and making it unmistakably not RIA's app

**Date:** 23 September 2026. **Plan:** [`EUDI-WALLET-POC-CONFORMANCE-PLAN.md`](EUDI-WALLET-POC-CONFORMANCE-PLAN.md)
§4 step 1 and §10 D1. **Executed:** the same day, from the local checkout
`~/Documents/eudi-wallet-poc` (`master` five commits ahead of `origin/master` at v0.6.6, `0866e94`).

## What was done

1. **Fork created.** `gh repo fork open-eid/eudi-wallet-poc --clone=false`, authenticated as
   `tomkabel`: <https://github.com/tomkabel/eudi-wallet-poc> (public — per D1, a fork's visibility
   cannot later be changed). Added to the local clone as remote `fork`
   (`git@github.com:tomkabel/eudi-wallet-poc.git`).
2. **Fork `master` = upstream v0.6.6, nothing more.** The five local commits (`705739c`…`46a64c8`)
   were not on any remote when the fork was created, so the fresh fork inherited upstream's state exactly; the explicit
   `git push fork 0866e94:refs/heads/master` was a no-op ("Everything up-to-date"). They lack the
   DCO sign-off open-eid's `CONTRIBUTING.md` requires, so per D2 they stay off `master` and off any
   PR until step 0 cross-verification and sign-off.
3. **Branch `step1-fork-identity`** created in the local clone on top of the five local commits (the
   identity commit needs a base, and §4 step 1 says "push the five commits to a branch"), one commit
   `ba5ba2d` "Make the fork unmistakably not RIA's app", pushed to the fork, which published the five
   commits on the fork's branch; they stay off fork `master` and off any PR (D2). Fork `master` stays
   at `0866e94`.

## The three changes (one commit, seven files)

| File | Change |
|---|---|
| `app/build.gradle.kts` | `applicationId = "ee.ria.wallet"` → `"ee.tomkabel.walletpoc"` (line 40 → 36 after edits). Removed the `google-services`, `firebase-crashlytics` and `firebase-appdistribution` gradle plugins, the `firebaseAppDistribution { … }` block in `debug`, the `com.google.firebase.appdistribution.gradle.firebaseAppDistribution` import, and the Firebase dependencies (BOM, `firebase-analytics`, `firebase-crashlytics`). |
| `build.gradle.kts` (root) | Removed the three `apply false` plugin aliases (`google.services`, `firebase.crashlytics`, `firebase.distribution`) so the neutralization is total, not per-module. |
| `app/google-services.json` | Removed from git tracking (was tracked upstream at v0.6.6 — already public, so this is about reporting, not secrecy); added `app/google-services.json` to `.gitignore`. The file remains on disk locally, untracked. |
| `app/src/main/res/values/strings.xml`, `values-et/strings.xml` | `app_name` → "EUDI Wallet PoC (independent)" in both languages. The upstream name was "EE Wallet" — no RIA/Potential string existed to replace, so the plan's name change is applied directly. |
| `README.md` | Independent-fork notice added as the first line: independent of RIA, not RIA's app, not affiliated with or endorsed by RIA or the Potential consortium, builds report nowhere. |
| `.gitignore` | `app/google-services.json` appended. |

`LICENSE.txt` untouched — MIT's one condition is the notice, which stays (plan §4 step 1).

## Verification evidence

All run in `~/Documents/eudi-wallet-poc` on branch `step1-fork-identity`, 23 September 2026:

```text
$ git grep -n 'ee\.ria\.wallet'            → NO MATCHES
$ git grep -n 'potential-wallet-bdf1d'     → NO MATCHES
$ git grep -in 'firebase' -- '*.kts'       → NO MATCHES
$ git ls-files | grep google-services      → NOT TRACKED (no output)
$ git grep -n 'applicationId =' -- app/build.gradle.kts
app/build.gradle.kts:36:        applicationId = "ee.tomkabel.walletpoc"
$ git ls-remote fork
0866e946874f666caa40e85acf8fc8fb22d71fc0  HEAD
0866e946874f666caa40e85acf8fc8fb22d71fc0  refs/heads/master
ba5ba2df43ef9f667dfd9edb210e3cf7e68be401  refs/heads/step1-fork-identity
0866e946874f666caa40e85acf8fc8fb22d71fc0  refs/tags/v0.6.6
```

Fork `master` = `0866e94` full hash `0866e946874f666caa40e85acf8fc8fb22d71fc0`, identical to the
fork's `v0.6.6` tag: exactly upstream's release state. Branch head `ba5ba2d` pushed.

Gradle sanity (full build deliberately deferred — device work is steps 2+):

```text
$ ./gradlew help                    → BUILD SUCCESSFUL (Gradle 8.11.1)
$ ./gradlew help --offline          → FAILED, unrelated: the local dependency cache is
                                      incomplete for AGP's own graph (io.grpc:grpc-stub:1.57.2
                                      among 13 similar "no cached version" failures). Cache
                                      issue, not caused by these edits; the online run proves
                                      the edited scripts configure.
$ ./gradlew :app:dependencies --configuration debugCompileClasspath → exit 0
```

Two residues, recorded rather than hidden:

- `debugCompileClasspath` still lists transitive `firebase-encoders`/`firebase-components`/
  `firebase-annotations` — inert support libraries reached through Google Play services and ML
  dependencies, not Analytics or Crashlytics. With the google-services plugin gone there is no
  `FirebaseApp` initialization and no project to report into.
- The plan's open item stands: whether the Potential funding banner may be reused on a derived
  work was not checked **[UNVERIFIED]**.

## Acceptance (plan §4, step 1)

"Fork builds under its own `applicationId`, reporting nowhere" — configuration succeeds under
`ee.tomkabel.walletpoc` with every Firebase reporting path removed; the APK-level confirmation
arrives with the device work of step 2.
