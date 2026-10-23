---
title: '1.4 av-vci:// invocation'
type: 'feature'
created: '2026-10-23'
status: 'done'
baseline_revision: 81d1ae0d6884d16fd710bfad7766fb0575f1c8d4
review_loop_iteration: 0
followup_review_recommended: false
context: []
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Android does not advertise or handle the AV Profile issuance trigger. Unmatched deep links are only logged, making refusals invisible.

**Approach:** Receive `av-vci://` links at the wallet ingress, validate their inline credential offer, and enter a visible issuance flow for `eu.europa.ec.av.1`. Refuse unknown invocations and absent or malformed parameters visibly with the offending parameter named.

## Boundaries & Constraints

**Always:** Preserve existing supported presentation and onboarding links. Validate before entering issuance. Display the offered issuer without asserting trust. Distinguish entering issuance from obtaining an attestation; external AV issuance is not integrated and must report that explicitly when continuation is attempted. Keep mock issuance separate. Catalogue tests in `tests/README.md` and index documentation in `docs/README.md`.

**Never:** Modify or revert `sprint-status.yaml`; commit `demo-data/`; copy private material; claim complete issuance, AVSUA or conformance from invocation handling; decode a whole URI or silently discard rejected links.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Valid invocation | `av-vci://?credential_offer=` plus encoded JSON with HTTPS `credential_issuer` and exactly `["proof_of_age"]` configuration IDs | Enter AV issuance UI naming `eu.europa.ec.av.1` and the supplied issuer; Cancel exits; Continue reports provider integration unavailable, without mock issuance | No entry error |
| Missing offer | Bare AV invocation or empty offer | Do not enter issuance | Visible refusal names `credential_offer` |
| Bad offer | Bad percent escape, UTF-8, JSON, wrong JSON types, duplicate/conflicting query inputs | Do not enter issuance | Visible refusal names offending parameter |
| Invalid issuer | Absent, nonstring, non-HTTPS or invalid issuer URL | Do not enter issuance | Name `credential_issuer` |
| Wrong credential | Missing, nonarray, empty, wrong/multiple configuration IDs | Do not enter issuance | Name `credential_configuration_ids` |
| Unsupported invocation | Unknown scheme, AV authority/path/fragment or unsupported query such as `credential_offer_uri` | Do not navigate or perform network IO | Visible refusal names scheme/invocation or unsupported parameter |
| Existing route | Previously supported presentation/onboarding URI | Existing navigation remains available | Navigation exceptions now produce visible refusal |
| Lifecycle | Initial intent and a subsequent intent received in running wallet | Both reach the same dispatch and valid/refusal UI | No silent catch |

</intent-contract>

## Code Map

- `app/src/main/kotlin/ee/cyber/wallet/ui/navigation/app/WalletInvocationViewModel.kt` — review fix: bounded Activity-retained typed inbox, FIFO refusal acknowledgement, continuation state and counted overflow. AV grants are not retained. Process-death restoration is outside this state retention guarantee.
- `app/src/test/kotlin/ee/cyber/wallet/ui/navigation/app/WalletInvocationViewModelTest.kt` — typed-state, queue, overflow, stale callback and size-limit regressions.

- `app/src/main/AndroidManifest.xml` — exported singleTask MainActivity has VIEW/BROWSABLE filters but no av-vci.
- `app/src/main/kotlin/ee/cyber/wallet/MainActivity.kt:61` and `:83` — forwards initial intent and onNewIntent to WalletApp.
- `app/src/main/kotlin/ee/cyber/wallet/ui/navigation/app/WalletApp.kt:84` — central loaded-app ingress currently navigates every URI and logs failures. Reuse this path; ensure handling waits for loaded state, survives cold start, consumes new intents once, and displays refusal.
- `app/src/main/kotlin/ee/cyber/wallet/ui/navigation/main/MainRoute.kt:91` and `MainNavGraph.kt:133` — current AV offer proceeds to a mock; do not reuse that continuation for externally supplied offers.
- `app/src/main/kotlin/ee/cyber/wallet/ui/screens/issuance/IssuanceViewModel.kt:125` — real AV issuance explicitly absent. Story 1.4 adds entry, not full provider exchange.
- `docs/profiles/av/annex-a-normative.md:138` — pinned primary source A.5 requires av-vci scheme, permits internal issuance interface. It does not define query grammar.
- `docs/profiles/av/annex-a-examples.md:61` — primary public examples give inline credential_offer, HTTPS credential_issuer, proof_of_age configuration IDs and grants. Examples spell av://, unlike normative av-vci://; document this distinction and the supported subset. Grants must remain data, never execute a supplied endpoint or expose codes in logs/UI.
- `app/src/test/kotlin/ee/cyber/wallet/` — JUnit/kotlin.test and coroutine tests; Android methods are stubbed, so pure parser/dispatch tests alone do not establish Android surface evidence.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/AndroidManifest.xml` — register av-vci VIEW/BROWSABLE — enable OS dispatch.
- [x] `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/AvVciInvocation.kt` — implement pure typed parser/dispatch with one-pass strict query decoding and named refusals — support matrix and avoid query smuggling. Reject unknown query inputs, duplicates, non-root path/authority/fragments; accept public inline offer fields including optional grants as inert data. Use strict JSON and refuse duplicate object keys if parser otherwise accepts ambiguity.
- [x] `app/src/main/kotlin/ee/cyber/wallet/ui/navigation/app/WalletApp.kt` and a dedicated invocation UI under `ui/screens/issuance/` — integrate dispatch before generic navigation, show actual issuer/doctype with Cancel and Continue, explicitly refuse unintegrated provider on Continue, show unknown-navigation errors — make outcomes visible without mock fallback or trust badges. Use resources in `app/src/main/res/values/strings.xml` for user-facing text.
- [x] `app/src/test/kotlin/ee/cyber/wallet/` — test parser and actual shared dispatch callbacks across every matrix scenario — assert refusals never invoke entry callback and existing links reach generic navigation.
- [x] `tests/test_av_vci_android.py` — add reproducible adb surface smoke test for manifest resolution, cold/warm valid intents, missing/malformed named refusals, unknown explicit intent, and visible provider-unavailable continuation. Run against an emulator if available; report exact limits if no Android runtime is available.
- [x] `tests/README.md`, `docs/profiles/AV-VCI-INVOCATION.md`, `docs/README.md` — name tests, supported grammar/examples and commands, source discrepancy, and entry-only scope.

**Acceptance Criteria:**
- Given a valid inline AV offer, when Android receives it at cold start or via onNewIntent, then the wallet enters a visible issuance flow for `eu.europa.ec.av.1` and the supplied issuer.
- Given an unknown or malformed invocation, when the wallet receives it, then the wallet visibly refuses it with offending parameters named and never enters issuance.
- Given the invocation test suite, when following `tests/README.md`, then reproducible tests exercise valid, invalid and unknown ingress behavior and retain existing route compatibility.

## Spec Change Log

## Review Triage Log

### 2026-10-23 — Review pass

- verdicts: 20 findings — high 0, medium 10, low 5, false 5, maybe-false 0
- findings:
  - `[medium]` `[patch]` Blind: Activity recreation loses active or pending invocation — reproduced on API 35 with night-mode recreation; persist typed pending/visible state and continuation state.
  - `[medium]` `[patch]` Blind: buffered invocations overwrite refusals — collection writes one state without awaiting rendering; retain refusals until dismissed and queue following outcomes.
  - `[low]` `[patch]` Blind: unbounded raw ingress — exported Activity buffers raw data before validation; bound the existing ingress while fixing pending state.
  - `[medium]` `[patch]` Blind: Unicode digits accepted as percent hex — Kotlin digitToIntOrNull admits non-ASCII digits; restrict the existing decoder to ASCII hex.
  - `[medium]` `[patch]` Blind: smoke locale command unavailable on API 31/32 — set-app-locales begins at API 33; use an explicit older-runtime locale prerequisite.
  - `[medium]` `[patch]` Blind: dismissal clicks have no outcome assertion — following intents can hide a broken dismiss handler; await title disappearance.
  - `[medium]` `[patch]` Blind: compatibility only tests a callback — changed ingress must also exercise real onboarding/presentation navigation and an unmatched supported scheme.
  - `[low]` `[patch]` Blind: Estonian UI falls back to English — app advertises Estonian; add matching resources and surface assertion.
  - `[medium]` `[patch]` Verification: Cancel/Close assertions can pass with inert handlers — same demonstrated test gap as blind dismissal finding; assert absence before subsequent intent.
  - `[low]` `[patch]` Verification: refusal tests ignore reason — missing can mutate to malformed without a failing test; compare complete refusal and visible reason.
  - `[medium]` `[patch]` Edge: recreation drops warm-intent outcome — same reproduced state-loss defect; preserve active and pending state.
  - `[medium]` `[patch]` Edge: queued outcomes overwrite refusal — same collection defect; serialize refusal acknowledgement.
  - `[low]` `[patch]` Edge: non-ASCII percent escapes enter issuance — same decoder defect; reject with regression cases.
  - `[low]` `[patch]` Edge: API31/32 smoke aborts on locale service — same harness prerequisite defect; retain declared runtime support with an appropriate fallback.
  - `[false]` `[reject]` Intent: JVM callbacks do not establish Android receipt — true limit of JVM tests, but API 35 smoke has now executed manifest resolution, cold/warm/repeated entry and visible refusals successfully.
  - `[false]` `[reject]` Intent: spec records surface execution pending — review snapshot preceded the completed run; final verification records executed outcomes, so this is stale evidence rather than an implementation defect.
  - `[medium]` `[patch]` Intent: existing-route compatibility lacks NavController evidence — same verified surface gap; add actual route checks.
  - `[false]` `[reject]` Intent: unknown scheme uses explicit intent — the AC applies when the wallet receives a URI, and an explicit intent demonstrates exactly that; Android need not discover arbitrary schemes.
  - `[false]` `[reject]` Intent: no complete provider acquisition — Story 1.4 requires entering issuance; public source and epic distinguish the trigger from provider/AVSUA integration. UI explicitly states unavailable integration and makes no acquisition claim.
  - `[false]` `[reject]` Intent: diff cannot establish workflow/commit completion — workflow finalization follows review; no source defect and final commit will supply this evidence.


### 2026-10-23 — Follow-up review pass

- verdicts: 21 findings — high 0, medium 6, low 7, false 8, maybe-false 0
- findings:
  - `[medium]` `[patch]` Blind: issuer queries accepted — checked OpenID4VCI 1.0 §12.2.1 primary text; issuer identifiers exclude queries. Reject rawQuery and add positive/negative controls; preserve one-pass literal percent decoding in a path.
  - `[low]` `[patch]` Blind: explicit empty port accepted — java.net.URI reports port -1 for a trailing colon. Reject an authority ending in a colon and retain explicit valid-port acceptance.
  - `[medium]` `[patch]` Blind: deeply nested extensions can overflow recursive validation — the reviewer reproduced 500 levels on a 256 KiB stack. Limit Jackson input nesting to 64 before tree validation and assert named refusal for nested JSON.
  - `[medium]` `[patch]` Blind: bidi controls can obscure a displayed issuer — URI accepts these path characters and the UI displays the supplied string. Refuse directional controls in issuer identifiers and add a regression.
  - `[medium]` `[patch]` Blind: recreation test can accept unchanged pre-recreation text — wait for a different Local Activity identity from dumpsys before accepting restored UI; normalize automatic/custom night mode before toggling.
  - `[low]` `[reject]` Blind: uppercase scheme parsing exceeds manifest resolution — Android scheme matching is case-sensitive; the normative trigger is lowercase av-vci. Document parser-versus-OS matching explicitly. Arbitrary case variants would require a much broader dispatch mechanism for no demonstrated ordinary-use defect.
  - `[low]` `[patch]` Blind: capacity visibility/dismissal lacks a surface test — add a full-inbox scenario, drain each earlier refusal and assert the localized count of five plus dismissal.
  - `[low]` `[patch]` Blind: query/empty-port validation tests absent — add both refusals with valid no-port and explicit-port controls, alongside the issuer validation fixes above.
  - `[low]` `[patch]` Blind: duplicate-query and percent-error Android examples absent — add a duplicate inline offer and malformed percent input to the runtime refusal matrix.
  - `[low]` `[patch]` Blind: AV size boundary untested — assert exact 65,536-character parser acceptance and next-character refusal; assert oversized AV ingress retains a typed refusal without raw data.
  - `[medium]` `[patch]` Edge: automatic dark mode may not recreate Activity — same harness defect as the blind recreation finding; normalize mode and require changed Local Activity identity.
  - `[medium]` `[patch]` Verification: normal CI never invokes the Android harness — add an App workflow Android 35 emulator job, local_mocks installation and both-locale smoke invocation; include harness edits in workflow triggers. Hosted execution remains unobserved until CI runs.
  - `[low]` `[patch]` Verification: counted refusal tested only as typed state — same capacity-rendering gap; assert the visible count and actual Close outcome in both languages.
  - `[false]` `[reject]` Intent: root slash versus unsupported paths — the executable grammar consistently permits only empty or root location; meaningful paths remain refused. This is a root spelling of the same invocation, not an alternate routed resource.
  - `[false]` `[reject]` Intent: refusal parameter attribution varies for limits/unsafe names — precise ordinary-field attribution is tested; generic query/invocation labels avoid echoing unsafe input and capacity has its own explicit message. No silent refusal or issuance bypass follows.
  - `[false]` `[reject]` Intent: offer extension typing admits different readings — optional grants are objects, extensions are inert data, duplicate keys/invalid strings are refused, and only issuer enters retained AV state. No field is executed or surfaced as trust.
  - `[false]` `[reject]` Intent: bounded existing links and representative route coverage — existing schemes retain their original navigation strings below the documented ingress bound; executed parser tests cover every listed scheme and Android smoke exercises actual onboarding/presentation. No reachable legitimate oversized route regression was demonstrated.
  - `[false]` `[reject]` Intent: process-death restoration is not guaranteed — the contract requires initial and subsequent received intents; Activity recreation is additionally retained. Restoring an already consumed intent after process death is not a newly received invocation and is explicitly outside the retention guarantee.
  - `[false]` `[reject]` Intent: unknown schemes use explicit Android delivery — the requirement applies when the wallet receives the URI; explicit delivery tests this without claiming Android discovers every unknown scheme.
  - `[false]` `[reject]` Intent: rejected-input network absence uses inspected control flow — refusal parsing/rendering has no network call; dispatch tests forbid both entry and navigation callbacks. Packet capture would not reveal an additional branch in this path.
  - `[false]` `[reject]` Intent: entry-only continuation lacks a credential-store assertion — Continue only marks providerUnavailable in retained state; it never calls issuance/storage and says no attestation was obtained. No alternative mint path was found.

Follow-up patched entries: high 0, medium 5, low 4 (12 finding rows, grouped by demonstrated shared causes); deferred 0; rejected 9. Follow-up review recommendation: false, because this follow-up patched no high entry. Existing deferred-work ledger entries were neither read for resolution nor changed.

## Design Notes

The implemented subset follows the public inline offer example and normative scheme. Remote offer resolution and provider issuance require future integration; explicitly refuse them rather than making network requests or minting mock documents. Human-only operator actions are not part of these acceptance criteria.

## Verification

**Commands:**
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testLocal_mocksUnitTest` — Android unit regression suite passes, including every matrix parser/dispatcher row.
- `make check` — existing fast checks pass.
- `python3 tests/test_av_vci_android.py --help` — documents adb prerequisites and invocation; run its smoke check with a local_mocks installation when an emulator is available. Inspect and record runtime availability honestly.


Implementation verification (2026-10-23):

- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest` passed, including eight new parser/shared-dispatch tests.
- `make check`, `python3 tests/test_av_vci_android.py --help` and `git diff --check` passed.
- Initial API 35 x86_64 Android surface smoke passed manifest resolution, cold/warm/repeated valid entry, Cancel, provider-unavailable Continue, and six named refusal scenarios. The expanded review-regression run also passed both locales, as recorded below.

Final repository verification (2026-10-23):

- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest :app:assembleLocal_mocks` passed: 182 tests, zero failures/errors/skips. All eight parser/dispatch and five retained-inbox tests ran. An earlier concurrent-build KSP cache failure was resolved by stopping Gradle and clearing generated KSP cache/output; the serial rerun passed.
- `make check` passed after review fixes. Smoke `--help`, Python compilation and diff whitespace checks passed.
- Matrix audit: valid/missing/malformed/issuer/configuration/unsupported/existing-link rows are covered by executed JVM tests; lifecycle and visible UI behavior are checked by the adb surface run. Inbox tests add queue/capacity/provider-state coverage.

Follow-up verification (2026-10-23):

- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest :app:assembleLocal_mocks` passed after parser hardening. The final unit rerun, after strengthening the valid-JSON oversized-input assertion, also passed: 185 tests, zero failures/errors/skips, including ten parser/dispatch and six retained-inbox tests.
- `make check` passed after the parser fixes. `actionlint .github/workflows/app.yml`, workflow/spec YAML parsing, Python compilation, smoke `--help`, and `git diff --check` passed.
- OpenID4VCI 1.0 §12.2.1 was checked against the public primary source on 2026-10-23: credential issuer identifiers permit an optional port/path and exclude query/fragment components.
- The follow-up targeted review of the patched parser, tests, harness and workflow reported no additional correctness defects. Device execution exposed two harness issues that were corrected: exact-button lookup must await an exact node, and the last queued refusal must expect the capacity dialog next.
- `python3 -u tests/test_av_vci_android.py --serial emulator-5554 --timeout 60` passed on Android 35 x86_64 with the patched local_mocks APK, in English and Estonian. It executed manifest resolution; cold/warm/repeated entry; Cancel and provider-unavailable Continue; changed-Activity recreation; FIFO missing/malformed refusals; complete localized reasons and Close dismissal; malformed percent/UTF-8 and duplicate query inputs; actual onboarding/presentation destinations; and the visible overflow count of five with dismissal.
- Initial follow-up device attempts encountered cold-start ANRs, and subsequent attempts remained behind a stale system ANR dialog that survived force-stop. The successful run followed explicit dialog dismissal, package speed compilation and a 540×1200 display at 210 dpi. The ANR trace showed the main thread runnable in Compose text layout; its cause was not conclusively established. The committed harness does not dismiss ANR dialogs automatically.

## Auto Run Result

Status: done. This was a follow-up review of the implementation after baseline `81d1ae0d6884d16fd710bfad7766fb0575f1c8d4`, starting at revision `9cbe03d8f7e4fbdf2cb7f637e42d7fab56ecd13e`.

The wallet receives the lowercase Android `av-vci://` trigger, validates an inline offer before entry, and displays its supplied issuer and `eu.europa.ec.av.1`. Refusals remain visible until acknowledged, survive Activity recreation and retain FIFO ordering with bounded capacity. Continue explicitly reports that provider integration is unavailable. This follow-up rejects issuer queries, explicit empty ports and directional controls, bounds JSON nesting, strengthens boundary and Android surface regressions, and adds the surface harness to App CI.

Files in the reviewed change since the baseline:

- `app/src/main/AndroidManifest.xml`, `MainActivity.kt` and `ui/navigation/app/WalletApp.kt` — Android receipt, shared initial/subsequent intent dispatch and visible outcomes.
- `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/AvVciInvocation.kt` — strict typed parsing, one-pass query decoding, bounded JSON and issuer validation.
- `app/src/main/kotlin/ee/cyber/wallet/ui/navigation/app/WalletInvocationViewModel.kt` — retained typed inbox, FIFO acknowledgement, provider state and counted capacity refusal.
- `app/src/main/kotlin/ee/cyber/wallet/ui/screens/issuance/AvVciInvocationDialog.kt` and `app/src/main/res/values{,-et}/strings.xml` — entry/refusal dialogs and English/Estonian messages.
- `app/src/test/kotlin/ee/cyber/wallet/domain/credentials/AvVciInvocationTest.kt` and `app/src/test/kotlin/ee/cyber/wallet/ui/navigation/app/WalletInvocationViewModelTest.kt` — parser, dispatch, lifecycle-state, capacity and exact-limit regressions.
- `tests/test_av_vci_android.py` — actual Android receipt/navigation, changed Activity identity, complete localized refusals, exact button waits and counted overflow checks.
- `.github/workflows/app.yml` — Android 35 surface job and harness path triggers.
- `docs/profiles/AV-VCI-INVOCATION.md`, `docs/README.md` and `tests/README.md` — supported subset, primary-source discrepancy, lifecycle limits, test commands and CI coverage.
- This spec — individual follow-up finding verdicts, verification evidence and final result.

Follow-up review: 21 findings, individually recorded above: high 0, medium 6, low 7, false 8. Twelve finding rows were patched across nine shared causes (entry verdicts: high 0, medium 5, low 4). Deferred: 0. Rejected: 9, with these reasons:

- Uppercase implicit scheme dispatch: low; lowercase is the normative trigger, and broader Android discovery would add complexity for an uncommon spelling. The parser/OS distinction is documented.
- Root slash: false; the executable grammar consistently accepts empty or root location and refuses meaningful paths.
- Safe generic refusal labels: false; ordinary fields retain precise attribution while unsafe input and capacity use explicit generic labels.
- Offer extension typing: false; extensions/grants are inert, only issuer enters retained entry state, and no supplied field is executed or asserted trustworthy.
- Existing-route bounds/coverage: false; supported schemes retain their navigation strings below the documented bound, and no legitimate oversized regression was demonstrated.
- Process-death restoration: false; the receipt contract covers initial/subsequent intents, with Activity recreation additionally retained. Process-death restoration is explicitly outside that guarantee.
- Explicit unknown-scheme delivery: false; this exercises the requirement when the wallet receives an invocation without claiming OS discovery of arbitrary schemes.
- Refusal network evidence: false; inspected refusal control flow contains no network call, and dispatch tests prohibit entry/navigation callbacks.
- Credential-store assertion on Continue: false; Continue only sets provider-unavailable state and calls neither issuance nor storage.

Follow-up review recommended: false. This follow-up patched no high entry. The targeted review after fixes found no further correctness defects.

Verification: 185 Android unit tests passed with zero failures/errors/skips; local_mocks assembly/installation, `make check`, the complete Android 35 English/Estonian surface run, workflow lint, YAML parsing, Python compilation/help and diff whitespace checks passed. Detailed commands, outcomes and runtime conditions are recorded in Verification above.

Residual limits: hosted execution of the new CI job has not been observed; API31/32 locale passes have not been executed; loaded-host cold-start ANR behavior remains uncharacterized beyond the recorded successful runtime configuration. External provider exchange, remote offer resolution, acquisition/AVSUA conformance and process-death restoration are not established by this entry story.

The orchestrator-owned working-tree change to `sprint-status.yaml` is preserved and excluded from this commit. No existing deferred-work ledger entry was modified, reopened or resolved, and this spec's `deferred` list remains empty. No human decision is needed for the completed invocation acceptance criteria. No push is performed.
