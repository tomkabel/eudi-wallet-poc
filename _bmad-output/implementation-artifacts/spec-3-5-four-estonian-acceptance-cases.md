---
title: '3.5 Four Estonian acceptance cases'
type: feature
created: '2026-11-13'
status: awaiting-operator
baseline_revision: f1c7e1263844109edb39184bc1d5b7bd213b5e48
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/DEVELOPMENT.md'
  - '{project-root}/docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md'
  - '{project-root}/docs/decisions/ADR-008-CARRIER-PATH-PLACEMENT.md'
warnings: [oversized]
deferred: []
operator_actions:
  - "Execute a fresh genuine foreign PID wallet session with authorized issuer trust/access, and supply its external adapter evidence."
  - "Execute a fresh genuine foreign AV wallet session with authorized issuer trust/access, and supply its external adapter evidence."
---

<intent-contract>

## Intent

**Problem:** Story 3.5 (FR13) lacks reproducible machine-readable evidence for foreign PID, foreign AV, unattested requests, unregistered RPs, shared-registry offline agreement and two further unauthenticated request shapes. Local EE fixtures cannot establish foreign-wallet acceptance.

**Approach:** Ship a seven-case runner, genuine local process/policy/fallback tests, strict foreign-wallet execution evidence intake and indexed instructions. Complete agent-executable behavior, then hand off human-only credential/device/access prerequisites honestly.

## Boundaries & Constraints

**Always:** Preserve encrypted default, real registration gates, per-document ZK resolution, EE-ZKP-055a and ADR-006/008. Test actual request dispatch/refusal effects before response, not only Boolean policy. The fifth case exercises actual wallet/verifier consumption of the same signed pinned document absent fetched registry, with observed version agreement: implement the minimal prerequisite, never a permanent skip or hash-only surrogate. Signed fallback obeys pinned-key signature, expiry and runtime compatibility with no per-presentation fetch. Reuse Circuit/SpecID and uniqueness/deprecation rules. Distinguish local controls from independent foreign evidence and unsupported shapes. Generate holder/signing keys outside checkout; commit only public artifacts. Use full canonical Git revisions. Index new docs/tests. You are not alone: preserve others' edits. Delegation is authorized; assign ownership and these restrictions. Commit agent work before terminal handoff.

**Never:** Write, stage, commit or revert sprint-status.yaml; orchestration states do not verify work. Commit demo-data/secrets, push, send external messages, import private material, manufacture foreign PID, claim legal permission/certification, weaken trust or add a new predicate/system/carrier/attestation. Do not alter this intent-contract. Adapter exit zero, fixtures or stale files cannot prove fresh foreign sessions. Human-only obligations require awaiting-operator, nonempty imperative operator_actions and matching Auto Run Result; never blocked for those obligations.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Foreign PID/AV | Fresh genuine external sessions with trust/access prerequisites | Separate named observed outcomes and canonical source/session provenance | Unavailable stays NOT_EXECUTED with operator action, never local-fixture PASS |
| Unattested DCAPI | Unsigned OpenID plain-only request, capable device | Refusal before release; actual unsupported-protocol reason is honest | Do not claim unsupported protocol executes Tier-2 policy |
| Unregistered RP | Absent exact RP in synthetic register, both creation routes | 403 register-not-found with no session | Config/network errors cannot substitute expected refusal |
| Offline agreement | No fetched list, shared valid signed fallback | Both actual resolvers record same list_version and usable SpecID | Missing/tampered/stale/incompatible document or version skew refuses |
| ISO/proximity | Real plain-only wire request without trusted readerAuth, capable device | Stored unauthenticated reason and refusal effects, no response dispatch | No downgrade; signed-untrusted is still unauthenticated |
| Runner integrity | Missing/duplicate/unknown/malformed/skipped evidence, child failure/timeout | Seven ordered rows, non-pass retained, overall nonzero | Bounded children/output; no exit-only or missing-row pass |

</intent-contract>

## Code Map

- _bmad-output/planning-artifacts/epics.md:690: authoritative Story 3.5; valid Epic 3 context and Story 3.4 spec loaded for continuity.
- tests/rp_kit_test.py:24 profile_command / :121 main: staged shipped profile, private temporary issuer/keys and fresh binary HTTP harness. verifier/go/register.go authorizeRegister/registerGate and register_test.go TestRegisterHTTPGates: absent RP is 403/register-not-found; missing implementation is 503.
- tests/e2e.sh:64 case_record and :85 mint / :100 holder: actual linked proof path; local EE/AV only. issuer/mint_ee_poa.py:308,:365 supports PoA/AV and hardcodes EE, no PID. wallet/present.py prints HTTP response and exits true only for valid proof.
- tests/run_carrier_conformance.py:250 run_command: bounded process-group/output controls; tests/av_report.py:39 git_record/file_record: canonical revision/content provenance. Reuse without changing existing inventories.
- app/src/main/kotlin/ee/cyber/wallet/domain/presentation/HolderObligations.kt:63: shared refusal core. DigitalCredentialsViewModel.processRequest: actual provider entry, supports org-iso-mdoc only; computes reader trust and refuses before response. ProximityViewModel RequestReceived reaches ageRefusal and stopPresentation(true), state.refusal/effect. Resolve VM file paths with rg; introduce narrow injectable capability/request seams if necessary.
- app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt and its ReaderAuthTest:195,:219: real missing-readerAuth / signed-untrusted fixtures. Existing generic core tests are insufficient. Do not assert request-level any-auth proves per-document authentication.
- app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkCapability.kt:20 currently addDefaultCircuits only; ZkPresenter.kt:83 resolveSchemeId currently bundled hash/count matching. Add actual shared-document fallback consumption while preserving per-doc helpers and native circuit matching.
- verifier/go/circuits/registry.go:114 Load currently drops envelope metadata; preserve Circuit/NewRegistry rules while adding ADR-006 validation/resolution. verifier/circuits.json unsigned/no document version; main.go:523 validates entries against FFI.
- docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md:60 requires same signed generated wallet asset, expiry/runtime/signature and version drift checks. Broader hosted/fetched distribution remains Stories 5.1-5.3; implement prerequisite sufficient for real offline case and document remaining scope.
- app/build.gradle.kts:390 kotlin.test/coroutines-test, no Mockito/Robolectric. JDK17 /usr/lib/jvm/java-17-openjdk; test XML app/build/test-results/testLocal_mocksUnitTest.
- Makefile:123,:165; .github/workflows/ci.yml:96,:340; .github/workflows/app.yml: appropriate gates and path filters. rp-kit/README.md, docs/README.md, tests/README.md: indexes.

## Tasks & Acceptance

**Execution:**
- [x] tests/run_estonian_acceptance.py and tests/test_estonian_acceptance.py: seven fixed rows, real surface evidence, source/input provenance, strict fresh external adapter contract, bounded children and malformed/timeout/output controls; JSON CLI and nonzero incomplete/error semantics.
- [x] verifier/go/circuits/, wallet presentation package, shared public signed artifact and generated build asset/tests: implement minimal real ADR-006 offline fallback with validation and version observability/drift controls; preserve tuple/hash/params and no committed private key. Add reproducible generation procedure.
- [x] DCAPI/proximity VM entry seams and app/src/test/kotlin/ acceptance tests: real wire requests/transfer events through production dispatch; assert refusal reason/effects and zero response. Capable/incapable and signed-untrusted controls where relevant; honest unsupported unsigned-OpenID refusal.
- [x] tests/ local acceptance helper as needed: actual absent registration on both HTTP routes and real local AV proof using locked issuer/prover, supplementary local evidence only.
- [x] Makefile and CI/Android filters: integrate fast report controls plus actual linked/JVM acceptance execution in appropriate jobs.
- [x] rp-kit/ACCEPTANCE.md and existing indexes: case schema, commands, observed refusal names, fallback provenance, evidence limits and imperative operator handoff. Do not answer unresolved foreign-AV legal question.
- [x] This spec: executed checks/review/canonical commits and terminal status/operator actions.

**Acceptance Criteria:**
- Given RP kit prerequisites, when documented runner executes, then seven ordered rows record observed evidence, without foreign acceptance claims absent genuine foreign sessions.
- Given missing RP or unauthenticated capable-device inputs, when actual creation/request paths execute, then both register routes and all three shapes refuse before release with observed reasons.
- Given no fetched list, when actual wallet/verifier resolvers use the same signed fallback, then both record same list_version and wallet resolves offered circuit; invalid inputs/skew never pass.
- Given human-only prerequisites remain after all agent work, when finalized, then work is committed and spec plus Auto Run Result say awaiting-operator with nonempty imperative operator_actions.

## Spec Change Log

- 2026-11-13: Implemented runner, actual process/dispatch controls and signed pinned-document prerequisite from the intent contract. Runtime compatibility is deliberately exact: Git revisions cannot be ordered, so the wallet's pinned native artifact maps to the full checked Longfellow revision and every offered SpecID/hash/parameter is validated. Public document version 1 expires 2027-05-13T00:00:00Z; private signing key generated outside checkout. Source baseline remains f1c7e1263844109edb39184bc1d5b7bd213b5e48. Parent workflow retains review/terminal-status/commit ownership.

## Review Triage Log

### 2026-11-13 — Review pass

- verdicts: 23 findings — high 3, medium 15, low 0, false 5, maybe-false 0.
- Collection: all four layers returned before classification or repairs. Order below is coverage, blind, intent, then edge, matching final-report arrival. Coverage findings are pre-verified under the review workflow. Other claims were traced through their actual guards/callers; graph caller analysis confirmed the three production DCAPI resolveSchemeId uses. Grouped entries retain individual rows. All repairs are internal adoption tests, normalization or guards for demonstrated states; they preserve the intent and introduce no public presentation protocol.
- findings:
  - `[medium]` `[patch]` Coverage C1, actual VM adoption is untested — helper tests manually compose dispatch/policy/refusal and cannot detect a missing call in either ViewModel. Add actual VM orchestration controls through matching/capability to retained refusal and real response boundaries (group VM).
  - `[medium]` `[patch]` Coverage C2, production capability adoption is untested — the fallback test builds its own RegistryZkSystem, so raw production addDefaultCircuits would evade it. Exercise the exact production construction using controlled native/list inputs (group factory).
  - `[high]` `[patch]` Blind B1, expired signed lists remain usable through cached DCAPI offers — main snapshots Accepted while handleDCAPINew uses p.offered and verifyZkDocument trusts ext.BySpecID without current Lookup. Recheck current acceptance at creation and verification and test crossing expiry (group expiry).
  - `[high]` `[patch]` Blind B2, production resolver bypasses exact requested identity/parameters — resolveSchemeId extracts only hashes and held count/version; its DCAPI callers never invoke RegistryZkSystem.getMatchingSystemSpec. Require exact offered identity/parameters before resolving and test malformed production inputs (group resolver).
  - `[medium]` `[patch]` Blind B3, named registry prerequisite is discarded — PinnedCircuitList.loaded.getOrNull erases failure reasons and an empty spec list becomes a generic circuit miss. Preserve the actual registry reason through retained refusal/evidence and runtime expiry (group reason).
  - `[medium]` `[patch]` Blind B4, actual VM acceptance wiring is untested — confirmed the same manually composed helper boundary as C1; actual ViewModel orchestration controls address this independently logged outcome (group VM).
  - `[medium]` `[patch]` Blind B5, null deprecation disagrees across consumers — Go unmarshals null to empty string, while Kotlin primitive content is the nonempty string null. Align optional metadata semantics and exercise shared null/empty/wrong-type/nonempty cases (group metadata).
  - `[medium]` `[patch]` Blind B6, oversized foreign artifacts are hashed before bounded decoding — file_record calls read_bytes before read_json checks stat size. Read bounded bytes once before hash/decode/retention and reject oversize (group artifact-bound).
  - `[medium]` `[patch]` Blind B7, independent build provenance is not established — execute rejects only identical paths and validate_foreign accepts any other clean checkout; actual wallet executable is unidentified. Reject another local-repo checkout and bind inspectable build identity/digest to supplied source (group foreign-source).
  - `[high]` `[patch]` Blind B8, refreshed wrappers can repackage old wire exchanges — request and verification challenge are sibling metadata and wire is merely nonempty. Inspect request nonce/session binding and bind verifier observations to retained request/response wire digests; reject replayed wire with new wrappers (group foreign-replay).
  - `[medium]` `[patch]` Blind B9, generator signs a representation it does not emit — CanonicalPayload(data) precedes MarshalIndent(doc), which omits explicit empty optional fields. Sign the normalized emitted representation and round-trip test it (group generator).
  - `[medium]` `[patch]` Blind B10, dirty implementation provenance is incomplete/unstable — inputs omit both ViewModels and policy/parser sources and are collected after execution without comparing the source snapshot. Capture relevant source manifest before execution and reject changes during it (group local-source).
  - `[medium]` `[patch]` Blind B11, missing runtime pin prevents any report — read_text of longfellow-rev.txt is outside error handling. Retain named prerequisite ERROR and all seven rows on I/O failure (group prerequisite).
  - `[false]` `[reject]` Intent I1, foreign execution remains outstanding — accurate descriptive limitation, not a defect: the user explicitly authorizes awaiting-operator after agent work; both retained rows say NOT_EXECUTED and no foreign acceptance is claimed. Final handoff will enumerate those actions.
  - `[medium]` `[patch]` Intent I2, wallet tests exercise a narrower boundary than actual VM orchestration — the described gap is real and shares C1/B4's missing adoption coverage; add actual VM path controls while retaining the disclosed physical-device limit (group VM).
  - `[false]` `[reject]` Intent I3, unsupported unsigned OpenID does not execute Tier-2 policy — the production dispatcher actually refuses this protocol before payload handling, records UNSUPPORTED_PROTOCOL and emits no response. The frozen contract explicitly requires that honest outcome and disallows claiming policy execution for it; supported ISO/proximity cases separately exercise policy.
  - `[false]` `[reject]` Intent I4, offline evidence is paired resolver execution — both real consumers resolve the signed artifact, and runner compares observed version/hash/SpecID. That is the contract's offline agreement criterion; the report does not claim a wallet-to-verifier device presentation.
  - `[false]` `[reject]` Intent I5, terminal frontmatter/result is absent during review — in-review is the required workflow state at this stage and finalization is explicitly pending. No implementation repair is warranted; terminal awaiting-operator and operator_actions belong to finalization, and spec-edit findings are rejected under this workflow.
  - `[false]` `[reject]` Intent I6, implementation is uncommitted during review — the workflow commits the reviewed tree after patches/verification. Dirty execution is disclosed; no completed release commit is claimed. Canonical commits and clean-tree checks belong to finalization.
  - `[medium]` `[patch]` Edge E1, artifact hashing precedes the 4 MiB guard — independently confirmed read_bytes ordering; same bounded-read repair as B6, separately retained (group artifact-bound).
  - `[medium]` `[patch]` Edge E2, deeply nested bounded JSON aborts reporting — decode can raise RecursionError, absent from foreign intake exception handling. Convert that reachable parse failure into retained ERROR rows and test it (group depth).
  - `[medium]` `[patch]` Edge E3, missing/unreadable Longfellow input aborts reporting — same unguarded pre-build read as B11, separately retained; prerequisite error handling addresses it (group prerequisite).
  - `[medium]` `[patch]` Edge E4, explicitly empty optional source fields break release signing — verified the source-sign/normalized-emit mismatch independently of B9; normalized signing plus CLI round-trip covers it (group generator).

Repairs were sent in one workflow-format message to the original implementation agent, /root/implement_story35. Thirteen grouped patch entries: high 3, medium 10. No deferrals or intent/spec loopbacks. Review repairs were present when work resumed on 16 November 2026. Final verification and terminal handoff are recorded below.

## Design Notes

An expected refusal is a passing control, never an accepted presentation. Unsupported unsigned OpenID DCAPI must report its actual reason. Shared fallback is a necessary fifth-case prerequisite; broader hosted distribution stays Epic 5. Keep foreign PID/AV visible and unperformed when no external evidence exists.

## Verification

### Executed implementation checks (2026-11-13)

- make doctor: 16/16 checks passed.
- make test-estonian-acceptance: 17 tests passed (strict inventories/duplicate JSON keys/nonstandard constants, fresh session/source/artifact intake, named failure retention, stale/skipped JVM rejection, zero-exit rejection, matching actual fallback observations and bounded child/descendant/output controls).
- Python ruff blocking plus full ratchet families and format checks: all three new Python files passed. pyright: 0 errors. New/updated document local links resolve. git diff --check passed.
- make estonian-acceptance-linked: passed actual staged HTTP absent-registration controls and supplementary encrypted local EE AV proof, with linked TestEstonianAbsentRegistrationNoSession proving zero live-session allocation on both creation routes. Report intentionally INCOMPLETE/nonzero.
- Delegated fallback checks: make test-go-fast; make test-go-zk; go test -race ./circuits ./cmd/circuit-list passed. Selected JVM execution passed 8 dispatch plus 3 fallback tests, no skips/failures/errors; the integrated runner independently forces fresh execution.
- Prior full integrated runner: all five local rows PASS, foreign PID and foreign AV NOT_EXECUTED, no diagnostics, result INCOMPLETE, status awaiting-operator and two imperative operator actions. Final retained execution after full native-parameter delegation hardening is recorded in the acceptance evidence index.
- Final integrated runner after parameter hardening: BUILD SUCCESSFUL (40 tasks executed, 3m52s JVM phase); five local PASS, two foreign NOT_EXECUTED, no diagnostics, expected exit 1/INCOMPLETE and awaiting-operator. [Retained report, transcript and both unskipped JVM XML suites](../../rp-kit/evidence/acceptance-2026-11-13/README.md) explicitly identify the dirty implementation snapshot at full base revision f1c7e1263844109edb39184bc1d5b7bd213b5e48. Final report is local engineering evidence, not independent foreign acceptance.
- Parent-verification Kotlin lint follow-up: all 22 introduced detekt findings repaired using extracted matching/JSON-guard helpers, typed transport/reader/age-policy context, explicit imports/constants and reduced fixture arguments; no baseline changes or new suppressions. make lint-kotlin passed. Complete JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testLocal_mocksUnitTest --console=plain passed (34 suites, 236 tests, zero skips/failures/errors); [separate follow-up evidence](../../rp-kit/evidence/acceptance-2026-11-13/followup-kotlin-lint/summary.json) preserves the earlier report unchanged. git diff --check passed.

Broad verification and canonical implementation commit are now recorded in Auto Run Result below. Human-only foreign credential/device/trust/access prerequisites remain unperformed.

### Required final gates

- python3 -m unittest discover -s tests -p test_estonian_acceptance.py: runner integrity and bounded-child controls pass.
- make test-go-fast and make test-go-zk: registry unit/race and linked verifier regressions pass.
- make test-rp-kit: deployed profile controls pass.
- JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testLocal_mocksUnitTest: wallet request/fallback plus existing suites pass.
- Execute documented local acceptance runner; retain truthful JSON with foreign NOT_EXECUTED, actual process/policy/fallback evidence and nonzero incomplete verdict. Run every matrix-covering test, not only simulations.
- make check: fast/lint gates pass; run make doctor first for environment issues. Inspect full baseline diff, local links, asset equality and source provenance; git diff --check clean. Exclude sprint-status.yaml/demo-data from all staging.

- Parent verification caught the signed-envelope migration invalidating the existing AV fixture input digest. Updated only tools/build_av_fixtures.py and manifest input pins; existing circuit params/request/proof vector bytes and their original provenance remain unchanged. Full read-only fixture and mutation controls rerun before review.

### Parent verification before review — 13 November 2026

- Parent independently read full staged baseline diff plus repaired Kotlin sources and fixture-pin diff. make test-go-fast, make test-go-zk and make test-rp-kit PASS; linked root 83.958s, zk 39.992s and all eight profile groups.
- Full wallet suite PASS: 34 suites, 236 tests, zero skips/failures/errors; structural lint repair evidence retained separately. make lint-kotlin PASS without baseline changes or suppressions.
- make check PASS after correcting introduced Kotlin lint findings and explicit AV input-pin migration; 49 fixture files byte-identical, 13 fixture mutation controls PASS, 17 acceptance integrity controls PASS.
- Matrix audit covers actual wire/transfer dispatcher and retained effects, both real HTTP routes plus zero allocation, Go/native-wallet signed fallback and validation negatives, runner inventory/process bounds, and foreign evidence-contract controls. Genuine foreign sessions remain NOT_EXECUTED, never inferred from synthetic tests.
- 113 local document destinations resolve; both retained follow-up JVM suites are successful and unskipped. Sprint-status SHA-256 remains 5a56053a0571f8173290df0da1c4d5a4326837936afd686becca014572cf93cc; never staged.

### Resumed final verification — 16 November 2026

- Resumed the existing staged implementation and unstaged review repairs; preserved the frozen intent and historical 13 November evidence. Review patch groups now cover actual ViewModel adoption, production registry construction/reasons, live expiry at handlers, exact resolver identity, shared optional metadata, normalized CLI signing, bounded/depth-safe evidence parsing, independent build/replay binding, stable source snapshots and named missing-pin errors. No review findings were deferred.
- `make doctor`: 16/16 passed. `make test-go-zk test-rp-kit`: passed linked Go/race tests and all eight deployed-profile groups.
- Full Android local_mocks suite after structural repairs: 35 suites, 243 tests, zero failures/errors/skips. Assertions were preserved; extracted the circuit-spec fixture builder and consolidated thin proximity dependency wrappers to satisfy the existing complexity gate.
- Fixed provenance subprocess lint by resolving Git through PATH explicitly; fixed formatting with the existing formatters. No baseline or threshold changes.
- Fresh integrated execution before final formatting: five local PASS, foreign PID/AV NOT_EXECUTED, no diagnostics, INCOMPLETE/exit 1 and awaiting-operator. A clean implementation-commit execution will be retained with the terminal handoff.
- Sprint tracker is stale for 3.5 (`backlog`) and Epic 3 (`backlog` despite completed stories). The frozen story contract prohibits writing/staging that file, so its bytes remain unchanged; this spec is the evidence-backed story state.

- The required broad gate exposed a pre-existing Python 3.14 decoder behavior change: `test_deep_adapter_json_preserves_error_artifacts` failed because 100,000 nested arrays no longer raised decoder recursion errors. Added an explicit iterative 256-container nesting guard in the AV runner; the unchanged regression turned green. This bounded compatibility repair preserves the existing malformed-evidence policy and tests.

- Final required checks PASS: `make check` gates through AV fixtures, then repaired AV conformance (38 tests), carrier/version/external suites and Estonian integrity (26 tests) rerun as their unchanged make targets. `make lint-py` passes after the AV compatibility fix. Kotlin formatter and detekt pass with unchanged baselines. `git diff --check` passes; 109 affected local document links resolve. Clean-commit integrated evidence and final commit identifiers follow in Auto Run Result.

## Auto Run Result

Status: awaiting-operator

Implementation commit: `a47d364f84f0094e7ab302ff01d786f921af1d7f`.

All agent-executable Story 3.5 work is complete and committed: seven-case runner,
actual ViewModel dispatch/refusal controls, real absent-registration HTTP gates,
local encrypted AV proof, shared signed offline fallback, strict foreign evidence
intake, CI integration and indexed reproduction instructions. The existing review
triage is resolved with no deferrals; final structural/lint and Python 3.14
compatibility repairs preserve assertions and baselines. Follow-up review
recommended: false; this is not a claim of a new independent reviewer verdict.

The [16 November clean-commit evidence](../../rp-kit/evidence/acceptance-2026-11-16/README.md)
records all five local cases PASS, both foreign cases NOT_EXECUTED, no diagnostics,
stable source manifests, expected INCOMPLETE/exit 1 and awaiting-operator.
The fresh selected JVM suites passed all 10 tests. The full wallet suite passed
35 suites/243 tests with zero failures/errors/skips. Complete `make check` passed
from the clean implementation commit, including all lint/security/fixture,
Go race, AV/carrier and 26 acceptance-integrity controls. Linked Go/ZK tests and
all eight deployed RP-profile groups passed. `make doctor` passed 16/16.

Execute the two imperative frontmatter operator actions to obtain genuine foreign
PID and AV acceptance evidence. No external session, physical-device execution,
certification, legal entitlement, push or external message is claimed. This
handoff and retained evidence are committed separately from the measured source.

Sprint-status remains byte-identical (SHA-256
`5a56053a0571f8173290df0da1c4d5a4326837936afd686becca014572cf93cc`)
under the frozen intent's explicit prohibition. Its Story 3.5 backlog and Epic 3
backlog values are stale; consult this evidence-backed handoff for current progress.
The next independent implementation story is 3.6; foreign acceptance remains an
operator obligation and must not be converted into done by local controls.

### Redispatch verification — 16 November 2026

Status: awaiting-operator

Rechecked the existing terminal handoff on redispatch rather than treating the
orchestrator's tracker as implementation evidence. `make test-estonian-acceptance`
passed all 26 tests again. The retained clean-commit report has seven ordered
rows, five local PASS controls, two foreign NOT_EXECUTED cases, no diagnostics,
and the two matching imperative operator actions. Its before/after source
manifests are identical. Comparing all 636 recorded entries with the current
checkout found only the acceptance guide's subsequent four-line link to that
same clean-commit evidence; implementation contents match the measured source.
This recheck does not claim a fresh integrated execution or independent review.

Existing implementation and evidence commits remain
`a47d364f84f0094e7ab302ff01d786f921af1d7f` and
`5bd9b4b5bc140d779bf42aba63e2e2cafe135b4d`, respectively. The working tree was
clean before this verification note. Sprint-status was neither written nor
reverted, and retains SHA-256
`5a56053a0571f8173290df0da1c4d5a4326837936afd686becca014572cf93cc`.
Both frontmatter operator actions remain owed; no additional agent implementation
work was identified by this recheck.
