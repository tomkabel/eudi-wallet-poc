---
title: '1.6 Published AV report and external reproduction'
type: 'feature'
created: '2026-10-23'
status: awaiting-operator
baseline_revision: cd3503f22efba227deb37103c7ca1f3c365fc1e3
review_loop_iteration: 0
followup_review_recommended: false
context: [AGENTS.md, DESIGN.md]
warnings: [oversized]
deferred: []
operator_actions:
  - "Review and commit the hardened implementation, regenerate the provisional follow-through audit from that clean full revision, and publish the implementation and retained evidence through fork; preserve the historical report at 3cce11df6ec4e9148ebd4859cde95aabf7e650fc separately."
  - "Have an independent human or organization run the documented clean-checkout procedure in one sitting and return elapsed times, host metadata, exact commands, return codes, transcript and both report artifacts."
  - "Arrange execution of an independent candidate at a recorded revision through the existing JSON adapter and retain its failures and unavailable evidence alongside the local result."
  - "Review the independent evidence and discrepancies, then publish the attributed report and reproduction record at the chosen public location."
---

<intent-contract>

## Intent

**Problem:** Story 1.6 requires a committed AV Profile report that includes failures and can be regenerated from a clean checkout by a third party. The existing runner emits partial JSON without the required host record or a readable published artifact.

**Approach:** Extend the existing runner to emit self-contained HTML with sibling JSON and precise profile, source and host provenance. Document and exercise clean-checkout regeneration; leave independent candidate execution and independent third-party reproduction explicitly unperformed until an operator supplies that external evidence.

## Boundaries & Constraints

**Always:** Preserve all result states, failures, natural registry clause order and inline exclusion reasons. Show exact canonical Git revisions, pinned profile source, CPU/baseline, RUSTFLAGS configuration versus observed environment, Rust and JDK versions, and sibling Longfellow revision/mismatch. Separate environment errors from property failures. Use the root design token family, English, a single column, keyboard-accessible expandable evidence and textual states/counts. Identify this independent PoC and its narrow checks, without implying endorsement or certification. Account for clean/dirty evidence honestly. Workers are not alone: accommodate others' changes without reverting them. Index docs and catalogue checks.

**Never:** Write, stage or revert sprint-status.yaml; commit demo-data; access private repositories; invent independent results; turn missing evidence into PASS; remove known failures to produce green; push or contact third parties. Parent handles commits/final status. Actual third-party participation and external publication are operator work, not grounds for blocked status: final status must be awaiting-operator with nonempty imperative operator_actions.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Local report | Actual local Go evidence including known failures | HTML and JSON agree; all 94 clauses appear in profile order per candidate with test or exclusion reason | Nonzero conformance exit retained |
| Missing/invalid evidence | Skipped local, absent candidate, missing/ambiguous mapped result | NOT_EXECUTED or ERROR visibly distinct from FAIL/PASS; no borrowed evidence | Nonzero exit |
| Host mismatch | Missing/wrong sibling, unavailable tools, incompatible CPU or SIGILL/build startup error | Named environment record/diagnostic separate from property verdict | Never represent startup error as conformance FAIL |
| Untrusted report text | Adapter names/evidence containing HTML | Literal escaped text; offline HTML requires no network resources | No script execution |
| Reproduction | Fresh clean checkout at recorded code revision | Documented commands generate inspectable artifacts including failures; changing timestamp/host is distinguished from semantic result changes | Independent reproduction remains unperformed |

</intent-contract>

## Code Map

- `tests/run_av_conformance.py` — existing JSON CLI, local go test JSON events, external adapter contract and strict nonzero audit; add report/environment support here or a small imported module under tests.
- `tests/test_av_conformance.py` — executable runner controls; full local test requires existing Rust staticlib. Extend with artifact-facing, error, metadata and escaping regressions.
- `docs/profiles/av/clause-registry.json` — ordered 94-clause denominator, 20 mapped clauses, exclusions and narrow scope/limits. Preserve semantics.
- `docs/profiles/av/source.json`, `annex-a-normative.md` — full primary-source revision and retained excerpt hashes. Reuse rather than invent a version label.
- `verifier/zkverify-ffi/longfellow-rev.txt`, `.cargo/config.toml`, `Makefile`, `scripts/bootstrap-longfellow.sh` — pinned sibling, x86-64-v3 build configuration and bootstrap; no JDK required by Go runner but record installed version or unavailable.
- `tests/README.md:222` — existing invocation and adapter contract; keep commands consistent.
- `docs/README.md` — all-doc index. `docs/DEVELOPMENT.md` — environment/build troubleshooting.
- `DESIGN.md`, `_bmad-output/implementation-artifacts/epic-1-context.md` — visual family and evidence discipline; previous Story 1.5 keeps JVM AVSUA evidence separate from Go runner exclusions.

## Tasks & Acceptance

**Execution:**
- [x] `tests/run_av_conformance.py` and optional `tests/av_report.py` — generate complete readable report and JSON provenance; retain existing CLI compatibility and nonzero failure behavior. Preserve per-test Go assertion output as failure evidence; label mandated DC API versus profiled unofficial OpenID4VP mso_mdoc_zk accurately. Correct package-specific error diagnostics if needed so failures carry their actual cause.
- [x] `tests/test_av_conformance.py` (and optional report-specific tests) — execute matrix cases through generated HTML/JSON and CLI, including all states, ordered exclusions, escaping, missing evidence and environment diagnostics. Wire any new test filename into `Makefile` and `.github/workflows/ci.yml`.
- [x] `docs/profiles/AV-REPORT-REPRODUCTION.md` — document exact clean-checkout setup, pinned Python install, make deps with baseline, runner command/output and expected exit 1, timing protocol for one sitting, environment mismatch handling and external evidence handoff. No claimed independent reproduction.
- [x] `docs/profiles/av/report.json`, `docs/profiles/av/report.html` — retain actual executed evidence with profile/version, host metadata and visible failures. Parent regenerates final artifacts at a clean committed implementation revision after review; no recursive self-commit claim.
- [x] `docs/README.md`, `tests/README.md` — index report/reproduction guide and catalogue added checks; update CI path filters if needed.

**Acceptance Criteria:**
- Given the pinned profile and an executed local audit, when opening the committed offline HTML and JSON, then both identify the same full source revision, host and candidate commit and include failures, unavailable evidence and exclusions as well as passes.
- Given a clean checkout and only documented commands, when executing the kit locally, then the report regenerates with the required CPU baseline, RUSTFLAGS, JDK and Longfellow revision metadata and environment problems are recognizable.
- Given no independent operator evidence, when finishing agent work, then artifacts state external reproduction is unperformed and the final spec records awaiting-operator plus specific outstanding external actions, without claiming the third-party acceptance criterion passed.

## Spec Change Log

## Review Triage Log

### 2026-10-23 — Review pass

- verdicts: 20 findings/observations — high 0, medium 8, low 6, false 6, maybe-false 0
- findings, in report arrival order (Verification Gap, Blind Hunter, Edge Case Hunter, Intent Alignment):
  - `[medium]` `[patch]` Verification: negative-control accounting is skipped in fast CI — synthetic package-qualified pass/fail/skip, timeout and SIGILL controls now execute in fast CI; the actual local audit suite also runs after the e2e staticlib build.
  - `[medium]` `[patch]` Verification: Boolean-only dirtiness assertion cannot detect falsely clean evidence — actual temporary Git repositories now assert canonical revision and exact clean, tracked-modified and untracked states. This also corrected whitespace stripping of porcelain output.
  - `[false]` `[reject]` Blind: provisional artifacts identify the pre-implementation dirty revision — the review snapshot deliberately labels provisional dirty evidence and reserves clean committed regeneration for parent finalization; no published clean claim was made. Final artifacts are replaced after the implementation commit.
  - `[medium]` `[patch]` Blind: supplementary probe failures disappear — every selected probe now retains its named state/assertion, and supplementary FAIL/ERROR/NOT_EXECUTED independently makes the audit nonzero. Synthetic mapped-pass/eight-negative-pass controls isolate this condition.
  - `[medium]` `[patch]` Blind: HTML omits executions and negative evidence — expandable candidate details now include exact commands, package exit/output, all probes, raw named negative outcomes and threshold eligibility; HTML assertions verify visibility.
  - `[low]` `[reject]` Blind: failed second output write can leave artifacts from different runs — real failure-path limitation, but the CLI already reports artifact-write ERROR/exit 2. Two-file atomic publication with rollback adds a recovery protocol for an uncommon failed write; it exceeds a direct correction. Compare timestamps/hashes and do not publish failed attempts, as the guide requires.
  - `[medium]` `[patch]` Blind: unavailable Git provenance has no audit error — named repository_provenance ERROR now records unavailable revision/cleanliness and makes the audit nonzero while retaining artifacts.
  - `[low]` `[patch]` Blind: registry bytes/invariants lack direct runner provenance — runner now reuses check_av_registry.validate before probes and records registry path/SHA-256 in both artifacts; malformed-registry and hash checks execute.
  - `[low]` `[patch]` Blind: effective Go overlays/workspace/compiler flags are unrecorded — go env -json captures relevant effective settings alongside observed environment with an explicit historical-compilation limit; overlay/workspace/compiler regression passes.
  - `[medium]` `[patch]` Blind: full audit regression never runs in CI with staticlib — e2e now asserts staticlib existence and executes the report suite after the build. Shared root cause with Verification's negative-accounting gap.
  - `[low]` `[patch]` Blind: hardcoded configured_baseline can contradict configuration — renamed to required_baseline; actual FFI configuration remains observed separately. A changed-config regression proves the distinction.
  - `[low]` `[patch]` Blind: unchecked host filesystem reads cause traceback — required host/source read failures now emit named validation ERROR/exit 2 before probes; missing, unreadable and invalid-text controls execute.
  - `[medium]` `[patch]` Edge: unmapped probe failure can vanish despite sufficient negative passes — same supplementary-retention fix as Blind; all fail/skip/ambiguous variants are tested through the final verdict and HTML.
  - `[low]` `[patch]` Edge: hardlinked output paths overwrite JSON with HTML — samefile augments the existing resolved-path collision check; actual hardlink test proves refusal before either write.
  - `[medium]` `[patch]` Edge: failed/skipped negative controls are invisible in HTML — same expandable execution/probe/negative evidence fix as Blind; raw states remain visible even when ineligible for threshold counting.
  - `[false]` `[reject]` Intent: tests do not execute documented clean setup — accurate intermediate verification boundary, explicitly reserved for parent finalization; local clean clone/build/run follows review and remains distinct from independent evidence.
  - `[false]` `[reject]` Intent: recorded baseline differs from uncommitted generator — accurate provisional state, explicitly dirty; parent finalization replaces it with clean implementation-commit evidence, without changing runtime code afterward.
  - `[false]` `[reject]` Intent: independent human reproduction remains absent — user expressly requires awaiting-operator for external human work; report/guide disclose UNPERFORMED and no local test claims independence.
  - `[false]` `[reject]` Intent: spec lacks final operator frontmatter in review snapshot — this is workflow finalization, not a code defect; parent sets matching awaiting-operator/result and nonempty imperative operator_actions after committing implementation.
  - `[false]` `[reject]` Intent: independent candidate handoff is broader than reproduction alone — candidate execution and independent reproduction are explicitly separate outstanding evidence, inherited from the existing Story 1.3 runner/epic kit. The report does not substitute one for the other or claim either executed.

Thirteen patched findings group into ten entries: five medium (supplementary outcomes, HTML evidence, Git provenance, CI/negative accounting, actual cleanliness verification) and five low (registry provenance/validation, effective Go settings, baseline label, controlled input reads, hardlink collision). No findings are deferred. All four layers completed before classification; three child slots required the fourth launch after a slot opened. No code or board reverts occurred.

## Design Notes

Report identity uses the full profile revision as its version, not an invented semantic release. Report evidence refers to a clean implementation commit; a later artifact commit contains that evidence. Third-party reproduction means an independent human/organization runs documented commands and records elapsed time, host, command transcript and artifacts; another agent-local checkout is only local verification.

## Verification

- `python3 -m unittest discover -s tests -p 'test_av_conformance.py' -v` — all report/runner regressions pass, including full local audit when staticlib exists.
- `make check` — baseline passes.
- `python3 tests/run_av_conformance.py --json /tmp/av-story-1-6.json --html /tmp/av-story-1-6.html` — artifacts exist, expected audit exit 1 with known local FAIL and external NOT_EXECUTED, correct profile and complete ordered clauses.
- `git diff --check` — no whitespace errors.
- Parent performs documented clean-checkout reproduction at committed implementation revision and records its elapsed time and limits; external operator evidence remains outstanding.

Verification before review (2026-10-23): parent ran all 18 runner/report tests with no skips, make check (exit 0), and the actual audit (expected exit 1; 17 PASS, 3 FAIL, 10 negative controls, external 20 NOT_EXECUTED, 74 exclusions each). JSON/HTML have the pinned profile revision and zero environment errors. Browser checks using cached Chromium passed 188 ordered rows, keyboard expansion, no network resources and no horizontal overflow at 390px; desktop/mobile captures inspected. System Chromium could not launch because libsimdutf.so.36 is absent; cached Chromium provided working verification. Clean-checkout reproduction remains a finalization action. Matrix audit: local/report and reproduction rows covered by real audit plus final clean run; missing evidence covered by skipped/omitted/duplicate cases; host mismatch covered by linker, architecture, missing tool and CLI SIGILL cases; untrusted text covered by literal HTML/DOM checks.

Verification after review fixes (2026-10-23): parent ran all 27 report/runner tests successfully, with no skips; make check exited 0, including the same 27 checks, 37 registry checks, 13 fixture checks, Go race tests and existing Python/lint controls. Actual audit still exits 1 with 17 PASS, 3 FAIL, 10 passing negative controls, external 20 NOT_EXECUTED, 74 EXCLUDED each and zero environment errors. Cached Chromium offline/keyboard/order/mobile checks and git diff --check pass after fixes. Generated files remain provisional until clean committed regeneration.

## Auto Run Result

Status: awaiting-operator

Completed all agent-executable work and committed the reviewed implementation at
`3cce11df6ec4e9148ebd4859cde95aabf7e650fc`. The retained HTML and JSON were then
generated from a fresh clean checkout of that exact implementation commit using
newly built prover/staticlib/verifier outputs. An evidence commit contains those
artifacts and the final operator handoff; it does not claim to be the source
revision executed by its own report.

The report shows all 94 clauses per candidate in profile order with test IDs or
inline exclusions. It preserves 17 local PASS, 3 FAIL, 74 EXCLUDED and 10 passing
negative controls; the unconfigured external candidate has 20 NOT_EXECUTED and
74 EXCLUDED. All 26 selected local probes retain named results and assertions.
Supplementary failures now affect the verdict independently of clause/negative
thresholds. Exact Go commands, package exit codes, named negative outcomes and
eligibility appear in expandable HTML as well as JSON. The audit exit is 1,
with thresholds met and zero environment errors, because the three known
property failures and twenty unavailable external results remain visible.

Provenance records the pinned full AV source revision, validated registry hash,
clean implementation revision, host CPU/baseline, required versus observed Rust
configuration/environment, actual JDK/Rust/Go/Python versions, effective Go
settings, sibling Longfellow pin/revision/dirtiness and staticlib fingerprint.
These observations do not attest historical compiler flags or production trust.

Files changed:

- `tests/run_av_conformance.py` — compatible JSON/HTML CLI, complete local probe evidence, shared source validation, provenance/error handling and supplementary verdict accounting.
- `tests/av_report.py` — host/Git/configuration collection and escaped self-contained HTML with textual states, counts, ordered exclusions and native keyboard expansion.
- `tests/test_av_conformance.py` — 27 executed runner, artifact, provenance, negative-accounting, collision and environment controls.
- `.github/workflows/ci.yml` — report-document filter and actual report regression after the e2e staticlib build.
- `Makefile` — updated existing runner-test target description; new checks use that existing target.
- `docs/profiles/av/report.json`, `report.html` — actual clean-checkout executed evidence from the committed implementation.
- `docs/profiles/av/reproduction-record.json`, `reproduction-transcript.txt` — command/timing/return-code record, transcript and artifact hashes.
- `docs/profiles/AV-REPORT-REPRODUCTION.md` — clean setup/build/run instructions, environment troubleshooting, measured local result and independent operator protocol.
- `docs/profiles/AV-CONFORMANCE-RUNNER.md`, `tests/README.md`, `docs/README.md` — report/runner contracts, check catalogue and document index.
- This spec — immutable intent, implementation map, individual review triage, verification and matching operator frontmatter/result.

Review breakdown: 20 individually triaged findings/observations (high 0,
medium 8, low 6, false 6). Thirteen patch rows form ten root-cause entries:
five medium and five low. No items are deferred. One low issue is rejected:
a second artifact write can fail after JSON replacement; exit 2 already names
the failure, and two-file atomic recovery adds disproportionate complexity to
an uncommon failed-write path. Six false defects are rejected with individual
reasons in the triage log: provisional dirty artifacts and their baseline are
explicit intermediate states replaced during finalization; clean-clone work
was a reserved parent step and is now completed; independent human work is
correctly outstanding under the user's awaiting-operator rule; final spec state
belongs to finalization; independent-candidate work is a separately labelled
existing epic obligation, not a claimed independent reproduction. No board
state was treated as verification evidence.

Follow-up review recommended: false. The first-pass medium patch count reaches
the usual recommendation threshold, but no specific unverified risk remains
in those patched entries after executable regressions, the full baseline,
fresh build/audit and browser checks. Independent external evidence remains
an explicit operator obligation rather than an unverified claim by this run.

Verification performed:

- Parent ran `python3 -m unittest discover -s tests -p 'test_av_conformance.py' -v` after patches: all 27 pass with no skips. The same suite also passed under isolated Python 3.12.13.
- Parent ran `make check` after patches: exit 0; Go race tests, lint/vet, existing Python checks, 37 registry checks, 13 fixture checks and 27 report/runner checks passed.
- Actual audit command from Verification: expected exit 1, both artifacts present with identical clause counts/order/profile and zero environment errors.
- Cached Chromium opened the final retained report offline: all 188 clause rows in order, keyboard expansion, no network resource requests and no horizontal overflow at 390px; desktop/mobile captures inspected. System Chromium failed to launch because libsimdutf.so.36 is absent, so cached Chromium supplied the browser verification.
- Fresh local clone at the implementation revision: Python 3.12 venv and pinned dependency install, exported x86-64-v3 RUSTFLAGS, `make deps` (exit 0), prover executable/staticlib existence checks, pinned sibling revision, clean Git status and audit (expected exit 1). Total 317.320 seconds, build 302.576 seconds, audit 6.466 seconds. The dirty flag remained false before and after the audit. All clause states/mappings/limits, named negative states and registry hash match the reviewed audit.
- The reproduction transcript/record retain exact commands, return codes and UTC start/end times. Fresh target directories were used; global dependency caches remained available. The local clone transport substitutes for public GitHub because the implementation is unpushed. No third-party independence or public transport success is inferred.
- `git diff --check` passes. Frontmatter parses as YAML with one nonempty imperative operator_actions string list, preserved intent-contract and unchanged orchestrator-board bytes. Final commits include every changed file; no demo-data is staged and no push is performed.

Residual limits: complete AV/ISO conformance, production integrity/trust,
independent candidate execution and actual third-party reproduction are not
established. Report publication through the public fork and independent
execution/attribution remain owed exactly as listed in operator_actions.
The guide records that source/host/timing variations require comparison and
that failed artifact-write attempts must not be published as matching pairs.

## 2026-10-23 — Retrospective follow-through

The [follow-through record](spec-1-6-av-report-follow-through.md) retains the
hardened runner's [new provisional workspace evidence](../../docs/profiles/av/follow-through-2026-10-23/README.md)
separately from the historical clean report and reproduction. The actual audit
at dirty base revision `6cf84df5806ecd22792eade3b8ced8d14f148202` returned 1
in 1.981 seconds: local 17 PASS / 3 FAIL, 10 passing negative controls, external
20 NOT_EXECUTED, 74 exclusions per candidate and zero environment errors. The new record hashes
159 source/configuration/profile/fixture inputs before execution and verifies
unchanged bytes after it, plus both reports and the exact-command transcript.
Pinned profile/registry, full summary, ordered clause states/mappings and named
negative-control states match the historical report. All four historical
report/reproduction files remain byte-for-byte unchanged.

Epic 1's overview and execution sequence now distinguish locally preparable
kit work from the independent candidate, publication and reproduction gates,
resolving retrospective F8 without changing acceptance criteria. The guide
links the hardened argv adapter contract. Documentation indexes the current
package, both follow-through specs and the deferred output-budget item.

Status remains `awaiting-operator`. No independent candidate or independently
attributed clean-checkout reproduction was provided, and no publication or
external contact occurred. The new uncommitted report is provisional;
review/commit, clean regeneration and publication through `fork` precede
independent execution and discrepancy review. Earlier clean local reproduction
remains historical evidence, not proof that these new fixes were cleanly rebuilt.
