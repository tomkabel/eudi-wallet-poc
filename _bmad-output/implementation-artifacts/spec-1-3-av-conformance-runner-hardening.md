---
title: '1.3 AV runner retrospective hardening'
type: 'bugfix'
created: '2026-10-23'
status: 'done'
route: 'dispatch'
baseline_commit: 6cf84df5806ecd22792eade3b8ced8d14f148202
review_loop_iteration: 0
context: [AGENTS.md]
---

<frozen-after-approval reason="user authorized finishing Epic 1 sequentially">

## Intent

**Problem:** Retrospective F3–F7 show that malformed adapter bytes can abort reports,
duplicate JSON keys can overwrite FAIL, timeout leaves descendants running, shell
startup failures have the wrong state, and the successful audit exit lacks a regression.

**Approach:** Harden the existing adapter boundary and exercise successful and
unsuccessful audits through generated JSON/HTML. Keep independent candidate
acceptance explicitly pending when no real candidate evidence exists.

## Boundaries & Constraints

**Always:** Own only `tests/run_av_conformance.py`, `tests/test_av_conformance.py`,
`docs/profiles/AV-CONFORMANCE-RUNNER.md`, related `tests/README.md` contract text,
and this spec. Preserve others' edits; other agents share the checkout. Keep
known AV policy failures, pins, registry scope and production defaults. Retain
quoted command arguments with `shlex.split` and execute argv directly rather
than an implicit shell, so executable startup and executed failure are distinct.
Reject malformed UTF-8 in either stream and duplicate JSON keys at every depth.
Timeout must terminate adapter process-group descendants and wait for cleanup.
Evidence tests are synthetic and must say so. Existing story remains
`awaiting-operator`; parent owns lifecycle and original story evidence.

**Never:** Commit, stage others' work, push, contact external parties, import
private material, touch `demo-data/`, edit `sprint-status.yaml`, change
cryptographic policy, fabricate independent candidate/reproduction evidence, or
modify Story 1.6's report/reproduction artifacts. Shell operators/expansion are
not interpreted; users needing a shell must explicitly invoke their script or
shell as the adapter, whose failed inner candidate startup must be reported
`NOT_EXECUTED` in schema results.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Invalid text | stdout or stderr contains malformed UTF-8 | Every mapped external clause is ERROR; JSON and HTML survive with stream named | Audit exit 1 |
| Conflicting JSON | Repeated top-level, candidate or nested result key, especially FAIL then PASS | Entire external candidate is ERROR with duplicate-key cause | Never accept overwritten evidence |
| Timeout | Adapter spawns delayed child, potentially holding pipes open | Process group terminated and direct process reaped before returning; no delayed marker | ERROR with timeout cause |
| Startup | Missing executable, malformed quoting or empty argv | NOT_EXECUTED for candidate that cannot launch | Named startup detail |
| Executed failure | Valid executable exits nonzero including 126/127 | ERROR with exit/diagnostic | Never confuse exit status with missing executable |
| Success control | Synthetic passing Go mapped/probe evidence, eight negative passes, valid actual adapter subprocess | main returns 0; matching JSON verdict, sufficient thresholds, zero failed/environment results, readable HTML | Still labels external reproduction UNPERFORMED |

</frozen-after-approval>

## Code Map

- `tests/run_av_conformance.py::external_results` currently uses shell/text
  subprocess.run and permissive json.loads. Reuse mapped-clause result expansion
  and external reported revision provenance. Keep no-command handoff unchanged.
- `tests/run_av_conformance.py::local_results/main` supplies actual Go event
  accounting, thresholds and both artifacts. Reuse these in success controls;
  mock only local Go evidence and known host diagnostic baseline where needed.
- `tests/test_av_conformance.py::payload/adapter/call_main/run_audit` provides
  synthetic adapter construction and artifact capture. Existing tests cover 27
  cases including known local FAILs, HTML escaping and supplementary failures.
- `docs/profiles/AV-CONFORMANCE-RUNNER.md`, `tests/README.md` are public adapter
  contract; update quoting, startup/exit, text, duplicate and timeout semantics.
- [Retrospective](epic-1-retro-2026-10-23.md) owns F3–F7; original
  [Story 1.3](spec-1-3-av-conformance-runner.md) retains external acceptance.

## Tasks & Acceptance

- [x] `tests/run_av_conformance.py` — harden adapter launch, process-group timeout,
  explicit UTF-8 decoding and strict duplicate-key parsing while preserving results.
- [x] `tests/test_av_conformance.py` — execute all matrix cases, including real
  malformed byte and conflicting raw JSON subprocesses, delayed descendant marker
  cleanup, missing executable versus executed exit127, and all-pass full audit.
- [x] `docs/profiles/AV-CONFORMANCE-RUNNER.md`, `tests/README.md` — reconcile the
  public adapter contract and clearly scope synthetic controls.
- [x] This spec — retain actual checks, review dispositions and remaining evidence.

**Acceptance Criteria:**
- Given invalid adapter boundaries, when running the audit, then artifacts retain
  per-clause ERROR or NOT_EXECUTED with the specific cause and no false PASS.
- Given synthetic sufficient passing evidence, when invoking main, then return0,
  JSON audit_exit_code0, zero failures and readable HTML agree.
- Given absent independent evidence, when completing local hardening, then Story
  1.3 is still awaiting-operator with independent execution actions outstanding.

## Implementation Notes

No irreversible operations. Existing dirty planning files are user work and
preserved under the user's authorized implementation scope. The parent runs
unchanged shared Gradle and Go-ZK suites once for both remaining stories.

## Spec Change Log

- 2026-10-23: Implemented direct quoted-argv launch, strict UTF-8 in both streams,
  duplicate-key rejection through the existing registry JSON hook, and timeout
  process-group termination with direct-adapter reaping. JSON-escaped unpaired
  surrogates are rejected as malformed text before report printing/writing.
- 2026-10-23: Added eight regression methods (35 total), using actual raw-byte
  and raw-JSON subprocesses, startup/exit126/127 controls, literal quoted argv,
  pipe-holding descendants with live/exited adapter parents, and a synthetic
  successful full audit. Local Go evidence and the sufficient host diagnostic
  baseline in the successful control are synthetic; independent candidate
  execution and reproduction remain absent.

## Review Triage Log

- Implementation clarification: POSIX process groups terminate descendants;
  `communicate()` drains pipes and reaps the direct adapter. Host init owns
  orphaned descendant reaping. The regression checks child termination (absent
  or zombie in `/proc`), direct `waitpid` already reaped, and no delayed marker.
- Implementation edge case: escaped unpaired surrogate values or keys can pass
  byte decoding and JSON parsing but break artifact output. Rejected before
  accepting payload metadata; raw-subprocess controls retain ERROR artifacts.
- Formal review dispositions are owned by the parent review workflow.


### 2026-10-23 — Independent review pass

All three directed lenses completed before triage. Blind findings retained
individually before grouping; edge overlaps retain their own rows. Verification
review reported no gaps. Parent traced malformed numeric/version behavior and
reproduced the deep JSON failure at 100,000 nested objects.

| Finding | Verdict / disposition | Evidence |
|---|---|---|
| Blind 1: deep JSON abort | medium / patch | A 100,000-level object raised uncaught RecursionError; add exception to existing malformed-payload handler and real artifact control. |
| Blind 2: escaped pipe-holder hangs cleanup | medium / patch | killpg owns only original process group; unbounded second communicate waits for escaped holder. Bound drain, close captures and retain direct reap; test escaped child with finally cleanup. |
| Blind 3: unlimited output resources | medium / defer | communicate already buffered unlimited output before this change; no stream quota or parser resource budget existed. New quota/streaming policy is pre-existing work rather than direct correction. |
| Blind 4: nonfinite JSON constants pass | medium / patch | Python default json accepts nonstandard constants even in unused nested fields; reject via existing parse call and cover artifacts. |
| Blind 5: bool/float schema version passes | low / patch | Python equality accepts true/1.0 as1; require exact int in existing schema guard and add controls. |
| Blind 6: child readiness assumes1s | low / patch | PID file can be absent under scheduling pressure even when termination is correct; separate readiness deadline from timeout clock in test supervision. |
| Blind 7: timeout test can hang suite | medium / patch | call_main synchronously runs cleanup without outer deadline; supervise timeout regression independently with fixture process cleanup. |
| Edge 1: escaped pipe-holder hangs cleanup | medium / patch | Same verified unbounded second communicate as Blind2. |
| Edge 2: nonfinite JSON constants pass | medium / patch | Same default json parsing as Blind4. |
| Edge 3: invalid-boundary no-PASS claim | medium / patch | Same NaN acceptance violates existing malformed-evidence requirement, addressed with Blind4 controls. |

No intent change or new public protocol surface is needed for direct exception,
validation, bounded-drain and test corrections. Process-group termination stays
the ownership guarantee; an intentionally detached child is not silently claimed
terminated. Public docs will state that limit. The output-quota concern is
recorded separately for future resource-budget design.

## Verification

- PASS: `python3 -m unittest discover -s tests -p 'test_av_conformance.py' -v`
  — 35 tests, including the actual local audit against the available staticlib.
- PASS: `make check` — lint, fast Go race suites, transcript/issuer/sandbox,
  registry (37), fixtures (13), and conformance runner (35).
- PASS: `git diff --check`.
- Parent-reported shared checks: Android 212/JVM 6 tests without failures;
  Go-ZK race suite passed. These shared suites were not rerun by this worker.
- Original Story 1.3 remains `awaiting-operator`; its independent candidate
  adapter execution and human/organization reproduction actions are outstanding.
  Story 1.6 report and reproduction artifacts, pins, registry scope, known AV
  fallback failures and production defaults were not modified.


## Final verification and acceptance — 2026-10-23

Baseline/tested HEAD is `6cf84df5806ecd22792eade3b8ced8d14f148202`;
checkout is dirty, with both pre-existing planning changes and this uncommitted
follow-through. No clean-commit or independent execution claim is made.

- `python3 -m unittest discover -s tests -p 'test_av_conformance.py' -v`:
  exit0, 38 tests in38.457s, no failures/errors/skips. Retained local log:
  `/tmp/av-hardening-review-tests.log`.
- `make check`: exit0 after review patches; runner38 tests in34.853s,
  fixture13 and registry37 checks, lint/vet, Go race and existing Python controls
  passed. Retained local log: `/tmp/eudi-story1-3-hardening-check-final.log`.
- `git diff --check`: exit0. All six matrix rows have executed passing controls.
  The actual local audit regression retains the known three policy FAILs and
  external NOT_EXECUTED results. Synthetic successful aggregation is explicitly
  scoped and still marks external reproduction UNPERFORMED.
- Parent shared execution: JDK17 Android212 tests and JVM conformance6 tests,
  zero failures/errors/skips; `make test-go-zk` race checks exit0. Parent log:
  `/tmp/eudi-epic1-followthrough-gradle-executed.log`. No production proof or
  carrier code changed, so this harness follow-through did not rerun e2e.

All ten review rows are disposed: malformed/deep/nonfinite/version inputs
retain ERROR artifacts; supervised timeout tests pass for original-group and
escaped-session pipe holders. Cleanup permits one-second bounded drain,
terminates original-group members and reaps the direct adapter. Detached
sessions remain outside that ownership and are disclosed, with actual fixture
cleanup in tests. Output-size quotas are the single deferred pre-existing item
in `deferred-work.md`.

Local hardening is complete; original Story1.3 remains `awaiting-operator`.
The following retrospective actions now have implementation and passing
regressions: `epic-1-retro-adapter-text`,
`epic-1-retro-adapter-duplicate-keys`,
`epic-1-retro-adapter-timeout-cleanup`,
`epic-1-retro-adapter-startup-state`, and
`epic-1-retro-success-verdict-test`. Parent owns board transitions. Missing
independent candidate/revision and its actual mapped clause evidence remain
open. No commit, staging, push, publication, external contact or sprint-status
write was performed by this story agent.
