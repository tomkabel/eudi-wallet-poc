---
title: '2.4 External carrier run and published result'
type: 'feature'
created: '2026-11-11'
status: awaiting-operator
operator_actions:
  - Publish the reviewed carrier evidence from commit a2141151a30875bdf343f73831f75cde8f005961 through the maintainer-approved public fork channel.
  - Reproduce the documented pinned Multipaz vector run independently and retain the operator identity, source revision, commands and unedited result artifacts.
followup_review_recommended: false
deferred: []
baseline_revision: '62ccf9292cac0d1e2633a2ee2050b47d859d3337'
baseline_commit: 'a1a8c81c141145ca7e280b4591464c15e65f1377'
route: 'dispatch'
review_loop_iteration: 0
warnings: [oversized]
story_key: '2-4-external-carrier-run-and-published-result'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-2-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The carrier runner reports only repository-local behavior and hardcodes an unnamed external blocker. Programme owners cannot inspect an attributable external attempt or reproduce it.

**Approach:** Add bounded external-target execution and evidence reporting alongside the existing local runner. Select the public Multipaz 0.99.0 sample wallet at its verified source commit, attempt the available build/run path, and retain the actual result and diagnostics in repository publication artifacts. Complete the repository half even when external execution is blocked; expose the remaining operator/publication gate honestly.

## Boundaries & Constraints

**Always:** Name the target, full canonical source commit, selection rationale, dated attempt, host, exact commands, exit codes, diagnostic output and coverage limitations in JSON and human-readable evidence. Select Multipaz because existing carrier fixtures derive from its 0.99.0 ZK serialization; tag 0.99.0 resolves to commit `7486ddbca490038c3580513d8273b9bbf18c6b70` from public upstream. Verify the checkout before attributing executed results. Report build/reach/setup unavailability as BLOCKED, executed assertion failures as FAIL and malformed evidence as ERROR. Require complete validated case/vector accounting before an external PASS. Distinguish independent implementation evidence from independent operator evidence. Preserve the existing sprint-status edits approved by the user.

**Never:** Infer external success from local Go tests, cached fixtures, upstream self-tests, a successful build, or an adapter exit code alone. Fabricate execution, omit the chosen target or drop failed cases. Change cryptographic acceptance, golden bytes, pinned dependencies or transport defaults. Copy private sibling content, touch demo-data, file external issues, commit or push without separate authorization. Prepared repository artifacts do not establish public publication or independent reproduction.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|---------------|----------------------------|----------------|
| Complete external run | Pinned target and complete valid evidence | Per-case/carrier outcomes retained; PASS only with every required assertion passed | Keep local results separate |
| Assertion failed | Valid external evidence containing FAIL | Overall FAIL and original failed evidence retained | Never convert to BLOCKED |
| Target unavailable | Build, reach, device or adapter prerequisite fails | Named dated BLOCKED target with actual command/output and unexecuted inventory | No inferred assertions |
| Evidence invalid | Missing/duplicate/unknown cases, wrong metadata, malformed JSON, revision mismatch or invalid encoding | ERROR with diagnostics and retained execution evidence | No passing external gate |
| Process fails | Startup error, crash, timeout or excessive output | Bounded execution with diagnostic and explicit setup/protocol disposition | Kill/reap timed-out descendants |

</frozen-after-approval>

## Code Map

- `tests/run_carrier_conformance.py:run_conformance`, `report_passed`, `run_command`: preserve local parser/vector/19-case checks and CI exit semantics; reuse bounded process execution where suitable. external_evidence is currently a placeholder.
- `tests/test_carrier_conformance.py:setup_root`, `record`: reusable inventory and injectable process controls; preserve local success semantics.
- `tests/run_av_conformance.py:external_results`: reference for strict JSON, argv splitting, provenance limits and cleanup; do not couple carrier reporting to the AV registry.
- `docs/profiles/CARRIER-CONFORMANCE-RUNNER.md`, `tests/README.md`: stale story-2.4 placeholder; document external invocation, schema and evidence limits.
- `Makefile:test-carrier-conformance`, `.github/workflows/ci.yml`: CI already invokes this make target; add offline controls without external network/build prerequisites.
- `zk-conformance/src/test/kotlin/ee/cyber/wallet/zk/Step87OpenID4VPFixtureTest.kt`: existing Multipaz-based fixture generation alone cannot satisfy external execution.

## Tasks & Acceptance

**Execution:**
- [x] `tests/run_external_carrier.py` — implement target metadata, bounded preparation/execution, strict adapter evidence validation and JSON/Markdown reports. Require the existing numbered inventory and both carriers; separate setup failures from assertions.
- [x] `tests/test_external_carrier.py` — test every matrix row through deterministic fake-child controls and representative subprocess failures; prove local PASS cannot satisfy external acceptance.
- [x] `tests/run_carrier_conformance.py`, `tests/test_carrier_conformance.py` — replace unselected-target placeholder with an accurate external-run pointer; preserve local CI behavior and report compatibility.
- [x] `docs/profiles/carrier/external-target.json` — record reviewed upstream URL, tag, canonical commit, rationale and reproducible preparation instructions; ordinary checks never download or execute external code.
- [x] `docs/profiles/carrier/external-run-2026-11-11/` — attempt the pinned public sample build/run outside the repository; retain JSON, Markdown and relevant output. Record observed device/adapter gaps and operator steps instead of declaring success.
- [x] `Makefile`, `tests/README.md`, `docs/profiles/CARRIER-CONFORMANCE-RUNNER.md`, `docs/README.md` — wire offline controls, document semantics and index new evidence documents.
- [x] This story spec — finalize awaiting-operator with a nonempty operator_actions YAML list of imperative strings and matching Auto Run Result, and commit reviewed changes. Never write, stage or revert orchestrator-owned sprint-status.yaml.

**Acceptance Criteria:**
- Given a named pinned external implementation, when the documented attempt runs, then reports retain its revision, selection rationale and actual result even if it fails or cannot build/run.
- Given a clean checkout, when offline controls and the documented external command run, then an integrator can reproduce reporting and identify prerequisites without confusing local validation with external compatibility.
- Given only repository preparation or agent execution, when completion is recorded, then public publication and independent operator evidence remain explicitly outstanding.

## Implementation Notes

### Redispatch scope (2026-11-11)

Existing artifacts are historical evidence, not proof of current integration.
Inspection found missing external controls in Makefile, stale local-report target
metadata, absent adapter documentation and missing docs index entries. Restore
these integrations and verify afresh, preserving original attempt bytes. The user
now authorizes committing repository work; this supersedes the older frozen
commit restriction. Never write, stage or revert sprint-status.yaml. Finalize
awaiting-operator only for maintainer publication and independent-operator
actions, with matching frontmatter and Auto Run Result. Adapter engineering
is not a human prerequisite; a genuine bounded upstream build failure may be
retained as BLOCKED external evidence without making the story itself blocked.

Document invocation, adapter environment and schema, exact case/vector metadata,
exit codes, bounded execution and source-attribution limits. Explicitly state that
no target adapter is shipped. Run every external control and make check afresh.

Repository implementation is complete; external acceptance remains awaiting operator.
The external runner validates the public pinned checkout before attribution, bounds
all commands, separates setup/protocol/assertion outcomes and requires all 19 cases
and both carriers for PASS. Raw adapter bytes and diagnostics are retained.

The actual 2026-11-11 agent attempt verified Multipaz 0.99.0 at canonical commit
7486ddbca490038c3580513d8273b9bbf18c6b70. The public sample build exceeded its
120-second bound (exit 143 after group termination); adb reported no connected
devices. All external assertions and both vectors remain NOT_EXECUTED. Evidence
is prepared under docs/profiles/carrier/external-run-2026-11-11/; no publication,
independent operator reproduction or certification is claimed. Sprint status
preserves the prior approved edits and records awaiting-operator separately.


## Spec Change Log

Review completion: verified patch findings corrected without changing frozen intent. Added dirty-source execution boundaries, descriptor-safe evidence reads, attribution ERROR precedence, successful-child descendant cleanup, runtime ERROR artifacts, CLI exit regressions, suite provenance and safe Markdown. Later clone/toolchain diagnostics are explicitly supplemental; original attempt bytes preserved. No review findings deferred.

## Review Triage Log

- Edge 1 — medium, patch: preparation only checks HEAD; modifying a tracked file leaves HEAD unchanged and reaches adapter. Add existing cleanliness check before adapter.
- Edge 2 — medium, patch: separate pathname lstat/stat/open permit FIFO replacement and an unbounded open. Read through a nonblocking no-follow descriptor and fstat that descriptor.
- Edge 3 — medium, patch: FAIL branch precedes attribution failure, so failed revision checks still attribute failed assertions. Prioritize ERROR while retaining failed rows.
- Verification 1 — medium, patch: filed mutation passed all 17 controls after disabling initial/final cleanliness guards. Add dirty-tree regression cases at both execution boundaries.
- Verification 2 — medium, patch: filed return-zero mutation passes all controls because none call main. Test exact 0/1/2/3 mapping and written artifacts.
- Blind 1 — medium, patch: verified same FAIL-before-attribution defect as Edge 3; preserve rows but refuse attribution.
- Blind 2 — medium, patch: verified same post-preparation cleanliness gap as Edge 1.
- Blind 3 — medium, patch: verified same pathname race as Edge 2; opened descriptor must be the validated/read object.
- Blind 4 — medium, patch: run_command successful-exit branch only waits for parent; a fork closing stdout can survive. Ensure original process group is terminated after completion and cover this exact child behavior.
- Blind 5 — medium, patch: runtime target metadata/read exceptions fall into parser.error and return 2 without artifacts. Keep argv failures in argparse; retain runtime ERROR report and exit 3.
- Blind 6 — medium, patch: report identifies target but lacks the runner revision/dirty state and input hashes, preventing exact suite attribution. Add observed suite source and inventory/vector hashes as report metadata.
- Blind 7 — low, rejected: current docs supply required fields and test payload() provides a complete working example. A new adapter scaffold/export adds public surface beyond a direct correction; no shipped adapter is claimed.
- Blind 8 — medium, patch: fixed tilde fences accept a closing tilde line from target diagnostics. Choose a fence longer than content runs and escape inline untrusted report text; add hostile-output controls.
- Blind 9 — medium, patch: source-resolution clone diagnostic is a summary rather than retained output. Replace with a separately dated actual bounded clone rerun and exact output, preserving original build evidence.
- Blind 10 — medium, patch: build host record omits selected Java/SDK/Gradle diagnostics. Retain a separately dated bounded host-diagnostics supplement; avoid implying it preceded the original build.

## Verification

- `make test-carrier-conformance` — existing version/runner and new external controls pass offline.
- `make check` — established fast CI gates pass; run make doctor first if builds misbehave.
- Execute the pinned target attempt with bounded time/output; retain actual output, distinguishing upstream build/self-tests from carrier assertions.
- `git diff --check` and evidence inspection — no whitespace errors, secrets, private imports or golden-byte changes; reports agree and retain failed/unexecuted coverage.

### Executed verification (2026-11-11)

- make test-carrier-conformance: PASS; 22 version controls, 20 existing local
  runner controls and 17 external controls. Final focused rerun passed after
  allowing a 3-second bounded startup for the descendant timeout control; the
  assertion still requires the spawned descendant to be dead/zombie.
- make check: PASS (complete exit 0), including repository lint ratchet, Go
  vet/race controls, Rust checks, Android spotless/detekt and offline suites.
- New runner/control files: full repository Ruff families PASS and Pyright
  0 errors. git diff --check PASS.
- Actual external attempt: BLOCKED with retained commands/output; no carrier
  assertion executed. JSON/Markdown agree on 19 unexecuted cases and two vectors.
- No cryptographic acceptance, golden bytes, pins, transport defaults, private
  imports or demo-data changes; no commit, push or external filing performed.

### Final parent verification

Final `make check` exited 0 after all review patches, including 22 version, 20 local-runner and 26 external controls. Log: `/tmp/story24-final-parent-check.log`. All matrix rows executed and passed; CLI/runtime errors, dirty attribution, descriptor races, silent descendants and hostile report text have regression controls. Final `git diff --check` passed. Three review layers completed; every finding has an individual triage row, all accepted patches fixed, none deferred.

Repository half complete in the earlier run; external carrier assertions, public publication and independent reproduction were outstanding. The original BLOCKED attempt and original artifact hashes are preserved by the separately dated supplement. No local commit was created in that run. The current redispatch explicitly authorizes a local commit; push remains outside this run.


### 2026-11-11 — Redispatch review pass
- verdicts: 13 observations — high 0, medium 8, low 2, false 3, maybe-false 0
- findings:
  - [medium] [patch] Blind: historical evidence was redated by local execution — added fixed attempt_date alongside local observation date; future-clock regression passes.
  - [medium] [patch] Blind: setsid descendants escape process-group cleanup — narrowed documentation to original dedicated process-group members; detached sessions explicitly excluded.
  - [low] [patch] Blind: raw evidence retention overstated — documented pre-read rejection and bounded regular-file retention limits.
  - [low] [patch] Blind: case 5 outcome pairing omitted — documented exact PASS/refused and FAIL/failed pairing from the validator.
  - [false] [reject] Blind: intermediate in-review lacks final operator_actions/result/commit — expected pending finalization; finalization below supplies required fields and supersedes old no-commit prose.
  - [medium] [patch] Edge: detached sessions survive group termination — same demonstrated root cause as Blind; corrected scope of guarantee rather than introducing cgroup public requirements.
  - [medium] [patch] Verification: SIGKILL success-branch regression unprotected — added synchronized SIGTERM-ignoring child to existing successful-parent test; both variants pass.
  - [false] [reject] Intent: intermediate integration diff does not show final lifecycle/commit — expected workflow staging, resolved by terminal finalization; not a code defect.
  - [medium] [patch] Intent: offline controls do not establish external assertions — retained fresh real bounded build attempts separately; every assertion remains NOT_EXECUTED, never inferred from controls.
  - [medium] [patch] Intent: adapter implementation misassigned to operator — corrected documentation to engineering/environment work; host feasibility attempted separately, independent reproduction/publication only are operator responsibilities.
  - [false] [reject] Intent: external BLOCKED differs from story lifecycle — external outcome and awaiting-operator lifecycle deliberately describe different surfaces.
  - [medium] [patch] Intent: sprint ownership cannot be proven by diff alone — no sprint mutation made; verify original SHA-256 and absence from staged/committed change set.
  - [medium] [patch] Feasibility audit: historic no-device observation no longer current — new separately dated attempt records connected device; preserve original bytes rather than rewrite history.

No deferred review finding. Review corrections preserve original cryptographic inputs,
runner case inventory, dependencies and target pin. The finding about editing this
spec is rejected as a review route per workflow; lifecycle fields are finalized as
an explicit user requirement, not by changing acceptance to fit implementation.


### 2026-11-11 — Supplemental harness review
- verdicts: 4 findings — high 0, medium 3, low 0, false 1, maybe-false 0
- findings:
  - [medium] [patch] Host harness observed provenance without enforcing it — require canonical source, exact origin and clean before/after boundaries before attribution.
  - [medium] [patch] Gradle status alone could label failed/missing vector assertions PASS — separate process status from compatibility result and require complete fresh two-vector evidence.
  - [medium] [patch] Gradle UP-TO-DATE could omit fresh evidence after deletion — force actual selected test execution and validate corresponding test XML.
  - [false] [reject] Detached-session containment in supplemental execution — documented original process-group limitation already applies; no all-descendant containment is claimed.

The actual initial vector feed executed both rows: interim-json FAIL at the target
CBOR parser, mso-mdoc-zk-cbor PASS through target parser and native verifier.
Upstream SystemTest independently generated a fresh proof and verified it. All
19 flow cases remain NOT_EXECUTED; partial vector failure is not a full-suite PASS.

## Auto Run Result

Status: awaiting-operator

Implemented and committed the reviewed repository work as

a2141151a30875bdf343f73831f75cde8f005961

before finalizing this status. Public publication and independent operator
reproduction remain explicitly outstanding; no push or external message was sent.
The matching frontmatter operator_actions contains only these human actions.

### Delivered change

- Makefile and the local runner/tests: include all external controls in fast CI,
  identify the selected target/revision and historical attempt date, and clean up
  remaining original-process-group members after successful parent exit.
- External runner/tests: separate engineering work from operator publication and
  independent reproduction; regressions cover SIGTERM-resistant descendants.
- Runner profile, tests README and docs index: document invocation, full adapter
  schema, evidence boundaries, exact failure semantics and reproducible commands.
- Fresh sample attempts: retain actual 120-second and 600-second build timeouts,
  connected-device observations, command output and source provenance separately
  from unchanged historical evidence.
- Host evidence and outside-source harness: actually execute the pinned Multipaz
  public parser/native verifier on both exact repository carrier vectors, validate
  provenance before/after, force fresh tests, retain XML and input hashes, and
  derive compatibility from assertions separately from process success.

### Actual result and coverage

Target: Multipaz 0.99.0 at canonical source revision
7486ddbca490038c3580513d8273b9bbf18c6b70. Public tag resolution was rechecked
with git ls-remote. Its fixture relationship and independent implementation are
the recorded selection rationale.

The final partial compatibility result is FAIL: the CBOR carrier parsed and its
native proof verified (code 0), while exact interim JSON bytes failed the target
CBOR parser. The target harness process succeeded and one fresh XML test body
executed both feeds. This demonstrates the particular parser/verifier surface,
not absence of every possible Multipaz integration. All 19 flow cases remain
NOT_EXECUTED; this is not a full-suite PASS or certification. The separate
upstream self-test generated a fresh proof successfully but does not count as a
repository carrier assertion.

Full flow/session interoperability, target-aware metadata and a complete 19-case
adapter remain explicitly outside the demonstrated partial result. They are
engineering work, not human operator actions or a prerequisite to publishing the
actual partial failure required by this story. No device/provider unavailability
is asserted for the current host. Publication must retain these coverage limits.

### Review and verification

Four review layers completed, plus a host feasibility audit and supplemental
harness review. Every observation has an individual verdict in the triage log.
Accepted patches address date attribution, cleanup scope and SIGKILL coverage,
raw-byte retention wording, case-5 pairing, engineering/operator separation,
source attribution, process-vs-assertion status and fresh test enforcement.
Rejected observations concern expected intermediate lifecycle state, a deliberate
distinction between external outcome and story lifecycle, or an already explicit
process-group limitation. No review item is deferred. The final reviewer confirmed
the three supplemental defects resolved; its transient missing standalone vector
file observation occurred during the forced rerun, and the final file exists and
agrees with embedded observations.

Follow-up review recommended: false. This redispatch is a follow-up convergence
pass: patched high 0; no specific unverified correctness risk remains in the
reviewed partial-result surface. Full-suite coverage remains an explicit limit.

- Final make check: PASS, exit 0, after all implementation/harness changes; log
  /tmp/story24-final-verified-check.log. It includes 22 version, 20 local-runner
  and 26 external controls, plus the existing complete fast CI gates.
- Supplemental host evidence controls: PASS, 6 tests; source, missing/duplicate
  evidence, assertion FAIL despite process PASS, stale tests and process errors.
- New Python full Ruff families, formatting and Pyright: PASS.
- Actual corrected target vector rerun: process PASS, compatibility FAIL, two
  vector observations; all input hashes match the committed harness inputs.
- Original artifact SHA-256 values match the original supplement. Target checkout
  remains at its canonical revision with clean source before and after execution.
- git diff --check and staged whitespace check: PASS. Markdown presentation
  whitespace is normalized; JSON preserves command-output bytes.
- sprint-status.yaml was never written, staged or reverted; its SHA-256 remains
  44f8dece2af730cdbf927334246039ec2a5d0ddb16c40d8fe9cca0bdede7b6b9.
  It is absent from this run's commit changes. No demo-data or private content,
  cryptographic inputs, golden vectors, pinned dependencies or transport defaults
  changed.

### Operator actions owed

1. Publish the reviewed carrier evidence through the maintainer-approved public
   fork channel, including failures, provenance and explicit unexecuted coverage.
2. Reproduce the documented pinned target run independently and retain operator
   identity, source revision, exact commands and unedited artifacts.
