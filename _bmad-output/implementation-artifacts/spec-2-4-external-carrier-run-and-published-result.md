---
title: '2.4 External carrier run and published result'
type: 'feature'
created: '2026-11-11'
status: 'awaiting-operator'
baseline_commit: 'a1a8c81c141145ca7e280b4591464c15e65f1377'
route: 'dispatch'
review_loop_iteration: 0
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
- [x] `_bmad-output/implementation-artifacts/sprint-status.yaml` — preserve prior edits; record repository completion separately from the external/operator gate using existing sprint conventions.

**Acceptance Criteria:**
- Given a named pinned external implementation, when the documented attempt runs, then reports retain its revision, selection rationale and actual result even if it fails or cannot build/run.
- Given a clean checkout, when offline controls and the documented external command run, then an integrator can reproduce reporting and identify prerequisites without confusing local validation with external compatibility.
- Given only repository preparation or agent execution, when completion is recorded, then public publication and independent operator evidence remain explicitly outstanding.

## Implementation Notes

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

Repository half complete; sprint remains `awaiting-operator` because external carrier assertions, public publication and independent reproduction are outstanding. The original BLOCKED attempt and original artifact hashes are preserved by the separately dated supplement. No local commit was created: the approved frozen boundary requires separate authorization for commit/push.
