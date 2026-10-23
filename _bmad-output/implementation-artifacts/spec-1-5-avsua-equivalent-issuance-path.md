---
title: '1.5 AVSUA-equivalent issuance path'
type: 'feature'
created: '2026-10-23'
status: 'done'
baseline_revision: 064240fe5d0b196195ce8a2d0b15cb7ca4a6695b
review_loop_iteration: 0
followup_review_recommended: false
context: [AGENTS.md]
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** The Android AV issuance path supplies a key attestation but no wallet instance integrity attestation. It can issue `eu.europa.ec.av.1` without the AVSUA equivalent required by EE-WUA-008.

**Approach:** Produce provider-signed instance and key evidence, accompany the AV issuance request with both, and require validation at the attestation provider before minting. Exercise the actual local issuance service with a withholding control. This is the offline mock equivalent; Story 10.3 separately owns real wallet-provider interoperability.

## Boundaries & Constraints

**Always:** Use the pinned public Annex A.5 and spec §12 as evidence. Bind both signed parts to the issuance context and the actual issuance key, validate signatures against an explicitly configured provider trust key (never trust a supplied x5c alone), validity, and integrity assertion. Use fresh issuance identifiers; never put account identifiers or holder private keys into the evidence. Name refusal causes. Keep evidence issuance-side, out of credential attributes and presentations. Mark mock integrity/security assertions and trust as demonstration inputs. Preserve non-AV issuance and batch rollback. Workers share this codebase: accommodate others' changes and never revert their edits. Catalogue tests and index documentation.

**Never:** Write, stage, commit or revert orchestrator-owned `sprint-status.yaml`; commit `demo-data/`; read or copy private repositories; claim production integrity, TS3 conformance or external issuance from local evidence; make external av-vci Continue silently mint mocks. The Python mint remains a fixture generator, not the Android issuance implementation.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| AV issuance | Fresh signed instance and key evidence for expected issuance key/context | Provider validates both before issuing an AV mdoc bound to that key | No error |
| Withheld evidence | Bundle, instance part or key part missing | No AV credential minted | Named AVSUA refusal |
| Invalid signatures/trust | Tampered token or signed by untrusted provider | No AV credential minted | Named signature/trust refusal |
| Wrong binding | Different issuance key, audience or transaction challenge | No AV credential minted | Named binding refusal |
| Invalid lifetime/integrity | Expired/future token or absent/negative integrity assertion | No AV credential minted | Named validity/integrity refusal |
| Repeated issuance | Fresh transactions to same or another provider | Evidence has fresh session values and no stable wallet/account identifiers | No error |
| Non-AV issuance | Existing PID, mDL or EE-PoA request without AVSUA | Existing contract remains supported | Existing errors |

</intent-contract>

## Code Map

- `docs/profiles/av/annex-a-normative.md:148` — pinned A.5: AVIA integrity plus KA key security, both solution-provider signed; WIA/KA reuse permitted. `source.json` pins full upstream revision.
- `spec/EE-EUDIW-TS-1.0.md:915` — EE-WUA-008 issuance-side equivalence; -007 disallows stable identifiers. Full real WUA/status/interoperability is separate Story 10.3 in epics.md:1677.
- `app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/WalletProviderServiceMock.kt` — existing independent provider signing key and public-only `attestKey`; extend with an explicit mock AVSUA operation and provider verification seam.
- `app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/WalletProviderService.kt` — production/mock service contract; unsupported providers must explicitly refuse the new operation.
- `app/src/main/kotlin/ee/cyber/wallet/domain/provider/ageverification/WalletProviderBatchAgeIssuer.kt` — actual SecureArea keys, wallet-provider call, mint callback and rollback; forward issuance evidence here.
- `app/src/main/kotlin/ee/cyber/wallet/di/IssuerModule.kt:75` — wire evidence production and issuer trust without trusting token headers.
- `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/CredentialIssuanceService{,Mock}.kt` — outer issuance boundary and public `issueMDocAgeVerification` helper; guard both against bypass. Update RPC implementation compatibility if contract changes.
- `app/src/test/kotlin/ee/cyber/wallet/domain/provider/wallet/WalletProviderServiceMockAttestationTest.kt` — in-memory BKS/JVM provider-signature patterns. Existing EePoaIssuanceTest uses a fake issuer and alone cannot establish this story's acceptance.
- `app/src/main/kotlin/ee/cyber/wallet/ui/screens/issuance/IssuanceViewModel.kt:150` — actual UI calls the batch issuer. Externally supplied av-vci offers retain explicit unavailable-provider continuation.

## Tasks & Acceptance

**Execution:**
- [x] `app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/` — add typed issuance context/evidence, mock provider generation and strict validation; document local wire format rather than inventing a normative TS3 format.
- [x] `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/` — require validated AVSUA evidence at the real mock AV mint boundary, with named refusal and no bypass through the public helper; preserve other credential types.
- [x] `app/src/main/kotlin/ee/cyber/wallet/domain/provider/ageverification/WalletProviderBatchAgeIssuer.kt`, `app/src/main/kotlin/ee/cyber/wallet/di/IssuerModule.kt` — carry signed evidence for actual generated keys through each AV issuance; preserve cleanup.
- [x] `app/src/test/kotlin/ee/cyber/wallet/` — execute every matrix row, including actual issuance service success with decoded mdoc key/doctype and withholding at the same boundary. Test production batch plumbing with fakes for Android-only keystore/database where necessary, not a copied transaction function.
- [x] `docs/profiles/AVSUA-ISSUANCE.md`, `docs/README.md`, `tests/README.md` — describe primary requirements, flow, format, commands, scope and mock trust limits. Registry A.5 exclusions must remain honest: its runner currently executes Go probes only; do not map JVM test names as runnable coverage.

**Acceptance Criteria:**
- Given the pinned AV Profile and EE-WUA-008, when the wallet's local AV issuance request reaches its attestation provider, then provider-signed instance integrity and key attestations accompany it and are validated before the `eu.europa.ec.av.1` credential is minted.
- Given the same issuance path with AVSUA withheld, when its executable test calls the actual provider boundary, then it refuses issuance with an AVSUA cause and produces no credential.
- Given documented reproduction commands, when the suite runs, then positive and negative issuance outcomes are reproducible and explicitly limited to local mock equivalence.

## Spec Change Log

## Review Triage Log

### 2026-10-23 — Review pass

- verdicts: 18 findings — high 0, medium 3, low 8, false 7, maybe-false 0
- findings (in layer report order):
  - `[low]` `[patch]` Blind: credentials described as authenticating despite being unused — source confirms the comment overstates mock behavior; correct comment and disclose the demonstration assumption.
  - `[low]` `[reject]` Blind: pending contexts have no capacity bound or idle cleanup — real in-process retention, but no exported context endpoint exists, normal flow has three contexts per batch, new context creation prunes expired entries, and account/process lifetime bounds idle retention. Everyday use does not approach problematic volume; adding capacity state/cleanup machinery is disproportionate here.
  - `[low]` `[reject]` Blind: failure leaves pending contexts — valid observation, but retryable failed evidence deliberately does not consume the context and all contexts become unusable after five minutes. Key/row rollback already executes. Adding an abandonment API and failure-state protocol exceeds a direct correction for this uncommon local retention issue.
  - `[low]` `[patch]` Blind: provider key loaded separately for each token — capture one signer snapshot per bundle. Concurrent account deletion can still refuse issuance, which is correct fail-closed behavior; no cross-reset availability guarantee is made.
  - `[medium]` `[patch]` Blind: trust supplier throws outside named refusal handling — current supplier can fail during keystore access; translate failures to SIGNATURE_OR_TRUST and test the cause.
  - `[low]` `[patch]` Blind: sequential replay does not exercise concurrent consumption — add an actual concurrent boundary regression proving only one caller reaches minting.
  - `[low]` `[patch]` Blind: wall-clock tests omit exact token and pending-context expiry — add controllable-clock boundaries using existing seams.
  - `[low]` `[patch]` Blind: missing controls for typ/algorithm/jti/demo_only/malformed input — add actual-boundary named-refusal cases.
  - `[low]` `[patch]` Blind: missing absent-trust/reset/signing-failure lifecycle controls — add regressions for documented behavior.
  - `[medium]` `[patch]` Verification: expired token also has nbf after exp — pre-verified test gap; set all dates consistently so expired-now is the only failed property, for each signed part.
  - `[medium]` `[patch]` Verification: independently constructed test callbacks bypass application DI — pre-verified wiring gap; exercise actual factory trust and forwarding with narrow JVM storage/asset seams.
  - `[false]` `[reject]` Intent: local issuance surface — auditor confirms the actual mock mint and real batch loop implement the local-equivalence reading supported by separate real-provider Story 10.3; no divergence to fix.
  - `[false]` `[reject]` Intent: real integrity/key security absent — both assertions are explicitly demo-only in code and docs; this story does not assert verified platform integrity, while Story 10.3 owns real provider integration.
  - `[false]` `[reject]` Intent: external provider acceptance absent — unsupported providers explicitly refuse; the epic separates operational interoperability into Story 10.3, so local-equivalence evidence does not imply external acceptance.
  - `[false]` `[reject]` Intent: AVSUA not embedded in response — pinned A.5 and EE-WUA-008 expressly target the AP during issuance; the request-side implementation matches the cited requirement.
  - `[false]` `[reject]` Intent: JVM evidence does not establish device UI/platform/external issuance — accurate limitation already disclosed, without an AC requiring a device UI run. The real service and batch logic execute; the separate concrete DI gap above is patched.
  - `[false]` `[reject]` Intent: A.5 remains excluded from Go runner — accurate explicit exclusion avoids inventing runnable JVM mappings in a Go-only runner and is not a missing local issuance behavior.
  - `[false]` `[reject]` Intent: diff cannot establish final status/commit/skill execution — these are workflow finalization steps, not defects at this in-review snapshot. User awaiting-operator condition does not apply because no human-only action is required.

Edge Case Hunter returned no findings. Surviving findings route to patches: direct corrections and tests of existing state/branches, without changing acceptance criteria. No deferred entry is added.

### 2026-10-23 — Follow-up review pass

- verdicts: 22 findings/observations — high 0, medium 3, low 8, false 11, maybe-false 0
- findings (Blind Hunter, Edge Case Hunter, Verification Gap, then the auditor's eleven surface observations):
  - `[low]` `[reject]` Blind: pending-context capacity and abandonment — carried: the previous pass already rejected unbounded idle retention and failed-request abandonment at this location. New creation prunes expired entries; five-minute validity prevents reuse, and the proposed cache/abandonment protocol is disproportionate to uncommon local retention. No existing deferred entry is reopened.
  - `[medium]` `[patch]` Blind: context can expire during trust/signature verification — verified: expiry was checked only before validation. Recheck immediately before context consumption; a controllable-clock regression advances during trust resolution while both signed tokens remain valid. Grouped with the Edge Case Hunter's same defect.
  - `[low]` `[reject]` Blind: concurrent first provider-key creation can race — the unsynchronized initializer predates this story (confirmed at baseline). Normal batch issuance first calls attestKey and then creates evidence sequentially, so new AV operations already have a signer; simultaneous first use can fail closed but does not bypass trust. Introducing shared keystore lifecycle synchronization for this uncommon pre-existing race exceeds a direct correction.
  - `[low]` `[patch]` Blind: pruning changes expired-transaction refusal from VALIDITY to BINDING — both outcomes already prevent minting. Document retained-expired versus unknown/pruned semantics and execute both outcomes with no issuer-key access.
  - `[low]` `[patch]` Blind: evidence-provider failure/cancellation lacks batch rollback controls — add a test throwing each after registration/context creation through the production batch factory, asserting original exception identity, zero mint-key loads and deletion of all batch keys/rows. The associated abandonment suggestion is carried from the previously rejected pending-state protocol; that protocol is not added.
  - `[low]` `[patch]` Blind: cross-AP tests do not isolate transaction ownership — use shared provider trust, audience and holder key in two APs; reject each foreign transaction and then successfully mint each AP's own transaction.
  - `[low]` `[patch]` Blind: privacy checks omit JOSE headers — assert the exact alg/typ header allowlist on generated tokens and check decoded headers plus claims for account, password and key-alias sentinels.
  - `[low]` `[patch]` Blind: key-security wrong-value/type branches lack controls — add trusted signed wrong-string and numeric assertions, requiring KEY_SECURITY and MALFORMED before issuer-key access.
  - `[low]` `[reject]` Blind: provider clock is not injectable for deterministic expiry tests — actual provider output already passes the real mint's signature and lifetime checks; fixed-clock tests intentionally isolate AP/verifier boundaries using signed dated evidence. Adding a provider constructor parameter solely to duplicate those deterministic controls is more than a direct correction, with no demonstrated everyday failure in timestamp generation.
  - `[medium]` `[patch]` Edge: pending context expires during validation — same verified defect as Blind's expiry finding; the post-verification deadline check and advancing-clock test fix both. No additional state or public surface is introduced.
  - `[medium]` `[patch]` Verification: original pending-context key binding is not independently tested — pre-verified gap accepted. Create context for A, sign both parts for B with configured provider trust, submit B's key attestation, and assert BINDING before issuer-key loading. This isolates the pending thumbprint guard from the token thumbprint guard.
  - `[false]` `[reject]` Intent: signed parts and withholding at mint — carried: actual mock mint/helper guards and withholding tests meet the explicit local-service surface. No UI withholding acceptance criterion exists.
  - `[false]` `[reject]` Intent: signature, trust, binding, validity and assertions — the actual service tests invoke the verifier with configured trust and independently signed negative controls. The timing defect is separately recorded and patched above; local demonstration claims are explicit.
  - `[false]` `[reject]` Intent: evidence for actual issuance keys — production batch forwarding and decoded mdoc device-key comparisons exercise actual generated public keys through fake platform storage. The original-context binding test gap is separately patched above.
  - `[false]` `[reject]` Intent: complete Android factory composition is not exercised — carried: factory trust reaches the actual asset-loader boundary and the shared batch factory reaches actual minting. Platform asset/keystore composition remains a disclosed device-verification limit; the intent explicitly requires execution of the actual local service, which these tests provide.
  - `[false]` `[reject]` Intent: withholding/rollback lacks UI and Room/Keystore execution — carried: actual service withholding and production batch rollback match the requested local-equivalence surface; running Android UI/platform evidence is not claimed.
  - `[false]` `[reject]` Intent: key thumbprint repeats when a caller reuses a key — fresh normal batch keys and fresh transaction/challenge/token identifiers satisfy the intended issuance flow. The required actual-key binding necessarily exposes that public key's thumbprint; the guide discloses manual key reuse and no stable account/wallet identifier is added.
  - `[false]` `[reject]` Intent: tests do not exercise every non-AV variant/error — AV validation is confined to the AGE_VERIFICATION branch; PID/mDL/EE-PoA implementation paths remain unchanged and execute successfully without evidence. No non-AV regression or altered error path was identified.
  - `[false]` `[reject]` Intent: named causes are not established in UI — the intent's named issuance refusals are exposed and asserted as AvsuaRefusal.causeCode at the actual service boundary; a new UI presentation requirement is not in the supplied contract.
  - `[false]` `[reject]` Intent: external Continue behavior is not re-executed — carried: this diff does not change external invocation handling; it preserves and documents Story 1.4's unavailable-provider behavior. Existing invocation checks remain in the full unit suite; no external issuance is claimed.
  - `[false]` `[reject]` Intent: evidence isolation lacks a new presentation test — carried: AVSUA exists on the issuance interface, minted AV data contains only age_over_18, and presentation code does not receive the evidence. No presentation change or evidence leakage was identified.
  - `[false]` `[reject]` Intent: primary-source and catalogue alignment — carried: pinned A.5/spec references, indexed guide and honest Go-runner exclusion support the local claim. The auditor confirms alignment rather than identifying a defect.

The auditor supplied eleven descriptive table observations, each recorded above; its enumerated readings and concluding limitations restate those observations. No review finding requires a human decision, intent rewrite, code re-derivation or change to the orchestrator's board. The original implementation subagent is not available in this follow-up invocation, so the parent applies the direct patches.

## Design Notes

The bundle accompanies the request to the AP; it is not embedded in the credential returned to relying parties. A provider-signed demo integrity assertion proves plumbing and validation, not platform integrity. No human-only external action is part of these ACs. Preserve the external-provider limitation from Story 1.4.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest :app:assembleLocal_mocks` — all Android unit checks and DI/assembly pass, including actual AV issuance and all negative controls.
- `make check` — existing fast CI baseline passes.
- `git diff --check` — no whitespace errors.

Verification before review (2026-10-23): Android unit/assembly command and make check both exited 0; 196 Android tests ran with zero failures/errors/skips, including all 11 AVSUA tests. Parent independently reran both commands successfully. Matrix audit matched all seven rows to executed AvsuaIssuanceTest cases: real mint/key binding; bundle/part withholding; both signature controls; key/context/token bindings; lifetime/integrity; session freshness/replay; PID/mDL/EE-PoA compatibility. Batch test also executed production forwarding and rollback. git diff --check passed.

Review fixes applied (2026-10-23): one signer snapshot now produces both tokens; comments and the guide disclose unchecked mock instance authentication; trust supplier failures become SIGNATURE_OR_TRUST while cancellation propagates. The expired-token control now isolates expiry, and tests cover malformed claim types, missing/negative claims, exact token/context boundaries, concurrent single consumption, absent trust, provider reset, and signing failure/retry. Application factory tests execute configured trust up to the actual asset loader and the shared internal batch factory through real minting. No public test-only API was added. A misplaced cleanup-interface KDoc was moved back to its declaration.

## Auto Run Result

Status: done. Follow-up review of the existing implementation completed; no human decision is required.

The Android local AV path produces provider-signed instance integrity and key-security evidence for each generated issuance key, forwards both to the actual mock attestation provider, and validates configured trust, signatures, context/key bindings, lifetime and demo assertions before minting `eu.europa.ec.av.1`. Withheld evidence refuses before signing-key access. Evidence stays outside credentials and presentations. Non-AV issuance and batch rollback remain covered.

This follow-up fixes a context-expiry race: trust resolution or signature validation can cross the pending context's deadline while refreshed evidence remains valid. The AP now rechecks the deadline immediately before consumption. Five additional test methods isolate original-context key binding, foreign AP transactions with shared trust/key, provider exception/cancellation rollback, expiry during trust resolution, and post-pruning refusal. Existing methods now check generated JOSE headers and wrong-value/type key-security assertions. Documentation explains the expiry and refusal behavior.

Files changed in the reviewed implementation since the baseline:

- `app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/Avsua.kt` — typed local evidence/context, named refusals and trusted-key validation.
- `app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/WalletProviderService.kt` — explicit unsupported-provider refusal for AV evidence.
- `app/src/main/kotlin/ee/cyber/wallet/domain/provider/wallet/WalletProviderServiceMock.kt` — two signed demo assertions using one provider signer snapshot and explicit local trust.
- `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/CredentialIssuanceService.kt` — issuance-context and evidence contract.
- `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/CredentialIssuanceServiceMock.kt` — guarded actual/helper mint boundaries and pending contexts; this pass adds the final deadline check.
- `app/src/main/kotlin/ee/cyber/wallet/domain/credentials/RpcCredentialIssuanceService.kt` — compatibility with the optional evidence parameter; existing production PID behavior.
- `app/src/main/kotlin/ee/cyber/wallet/domain/provider/ageverification/WalletProviderBatchAgeIssuer.kt` — actual-key evidence forwarding with batch cleanup.
- `app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaKeyManager.kt` — public-key batch storage interface for execution of the real batch loop in JVM tests.
- `app/src/main/kotlin/ee/cyber/wallet/di/IssuerModule.kt` — configured mock trust and context/evidence forwarding through the shared factory.
- `app/src/test/kotlin/ee/cyber/wallet/domain/provider/wallet/AvsuaIssuanceTest.kt` — actual mdoc mint/decoded key verification and 27 boundary, privacy, lifecycle, factory and rollback tests.
- `docs/profiles/AVSUA-ISSUANCE.md` — pinned requirements, local format, flow, reproduction and mock limits; this pass documents final expiry checking and retained/pruned refusal semantics.
- `docs/profiles/av/clause-registry.json` — honest A.5 exclusion distinguishing separate JVM evidence from executable Go coverage.
- `docs/README.md` — guide index.
- `tests/README.md` — executed test catalogue, including this pass's new controls.
- This spec — preserved intent and previous triage, appended individual follow-up verdicts and final verification/result.

Review breakdown for this follow-up: 22 findings/observations, individually recorded in the new triage entry (high 0, medium 3, low 8, false 11). Eight patch rows form seven root-cause entries: high 0, medium 2, low 5. The duplicate context-expiry finding shares one fix. No entries were deferred and the existing `deferred: []` is unchanged. Four independent layers completed; the tool's three-child concurrency limit required launching the fourth after a slot opened. All reports were collected before triage.

Rejected findings: pending capacity/abandonment retains the prior rejection because validity is bounded and an additional lifecycle protocol is disproportionate; concurrent first signer creation is an uncommon pre-existing fail-closed race outside the sequential normal batch path; injecting a provider clock adds a surface without a demonstrated timestamp failure. Eleven auditor observations were rejected as defects because actual mint enforcement, configured validation, generated-key forwarding, local factory execution, withholding/rollback, normal fresh-key sessions, unchanged non-AV paths, named service refusals, unchanged external Continue, issuance-only evidence and accurate source/catalogue claims match the supplied contract. Platform/UI composition and external acceptance remain disclosed verification limits. Every individual reason is recorded in the triage log.

Follow-up review recommended: false. This is a follow-up pass with no high-severity patched entry; medium/low patch volume alone does not request another pass. All retained fixes have executable coverage. Production platform integrity and a complete installed-device issuance run remain unestablished by these JVM results.

Verification after the patches (2026-10-23):

- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest --tests ee.cyber.wallet.domain.provider.wallet.AvsuaIssuanceTest --no-build-cache` — exit 0; all 27 focused AVSUA tests pass.
- `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest :app:assembleLocal_mocks --no-build-cache` — exit 0; unit task executed, 212 tests, zero failures/errors/skips, including all 27 AVSUA tests; assembly passed.
- `make check` — exit 0 after patches; Go race tests, Python/lint checks, transcript, registry, fixture and conformance-runner checks pass.
- `git diff --check` — exit 0 after final result serialization.
- Follow-up verdict counts audited; intent-contract matches its pre-run committed content exactly. The orchestrator-owned board's bytes match the pre-run SHA-256; its pre-existing uncommitted change is deliberately preserved and excluded from this run's stage/commit/cleanliness scope. Existing deferred-work ledger entries were not opened, edited or resolved.

Residual limits: mock instance authentication, platform integrity, hardware security and trust are demonstration assumptions. JVM issuer keys replace asset loading and fake storage replaces Android Keystore/Room. These results establish local mock issuance equivalence only, without production integrity, revocation/status, TS3 wire conformance, external-provider issuance or certification. No push was performed and no demo-data was staged or committed.
