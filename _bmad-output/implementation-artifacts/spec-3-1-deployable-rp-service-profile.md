---
title: '3.1 Deployable RP service profile'
type: feature
created: '2026-11-11'
status: done
baseline_revision: 94d5e458e5491496b349d175f7aa3530ca2333e3
review_loop_iteration: 0
followup_review_recommended: false
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** The Go verifier exists, but an operator lacks a deployable RP profile. Sessions with no callback can disappear without an identifiable terminal outcome.

**Approach:** Implement accepted ADR-004 over the existing binary, document reproducible startup, and make callback expiry observable and identifiable.

## Boundaries & Constraints

**Always:** Preserve ADR-004, encrypted direct_post.jwt by default, persisted operator-managed P-256 response keys mode 0600, existing verification and drain logic. State TTL and bounded expiry observation window accurately. Run real local process tests. Index documentation/tests. You are not alone; accommodate others' edits. Never write, stage, commit or revert _bmad-output/implementation-artifacts/sprint-status.yaml regardless of its state. Never commit demo-data/. Respect AGENTS.md and evidence discipline.

**Never:** Add another service implementation, weaken verification, expose development/downgrade flags as deployment modes, implement later TARA/register/gateway stories, fabricate external verification or push. Actual public deployment/domain/vendor actions are not this story's acceptance requirements. Do not modify this intent-contract.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Profile startup | Documented command, persistent key, local trust inputs | Health works; fetched request uses direct_post.jwt; JWKS survives restart | Failure exits nonzero |
| Pending callback | Created session before TTL, no callback | Same id and pending status | No error |
| Missing callback | After TTL, repeated polls and janitor sweep | 410 names id and expiry during documented bounded window | Unknown id returns 404 naming requested id |
| Expired mutation | Claim/completion after TTL | Refused; no revival or sensitive payload retained | ErrExpired within observation window |
| Invalid TTL | Nonpositive TTL flag | Startup refuses | Nonzero exit |
| Plain control | Plain mode alone, then both explicit flags | Alone refuses; paired negative control starts plain | Profile never enables these flags |

</intent-contract>

## Code Map

- `docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md` — accepted before implementation; profile, systemd unit, env template, key/drain lifecycle.
- `verifier/go/main.go` — flags around 365, response validation 439, keys 453, routes/drain 584; validate positive TTL.
- `verifier/go/oid4vp/session.go` — Get/Claim/Complete and sweep delete expiry; retain bounded metadata-only outcome.
- `verifier/go/present.go` — handleResult/statusFor around 752; terminal errors need id.
- `verifier/go/iso.go` — handleDCAPIResult around 227; equivalent status contract.
- `rp-kit/` — new deployment profile, same verifier/zkverify binary.
- `Makefile`, `.github/workflows/ci.yml` — add bounded profile test and path filters with Rust prerequisites.
- `docs/DEVELOPMENT.md` — deployment text says profile absent; update.
- `docs/README.md`, `tests/README.md` — index profile and tests.

## Tasks & Acceptance

**Execution:**
- [x] `rp-kit/README.md`, `rp-kit/zkverify.service`, `rp-kit/zkverify.env.example` — ADR profile with complete build/key/config/foreground/systemd/status/drain/rotation commands, local acceptance path and explicit HTTPS proxy boundary; private persistent key, restricted account, direct ExecStart.
- [x] `verifier/go/oid4vp/session.go` and adjacent tests — bounded expiry metadata after sweep, late mutation refusal, sensitive payload removal, retention/capacity cleanup.
- [x] `verifier/go/main.go`, `present.go`, `iso.go` and tests — positive TTL validation, identifiable terminal errors; HTTP missing-callback and unknown-id tests on both result routes.
- [x] `tests/` profile harness, `Makefile`, `.github/workflows/ci.yml` — bounded real process startup/default request/key persistence/plain controls/invalid TTL/timeout; register make test-rp-kit and CI execution.
- [x] `docs/DEVELOPMENT.md`, `docs/README.md`, `tests/README.md` — operational guidance and indexes.

**Acceptance Criteria:**
- Given checkout and documented prerequisites, when rp-kit commands run, then the existing verifier starts without undocumented setup and serves pending then identifiable expiry without wallet callback.
- Given ADR-004 predates implementation, when the profile ships, then its one-binary, key, encryption and drain decisions are preserved.
- Given default profile startup, when a request is fetched, then response_mode is direct_post.jwt; given binary plain alone it refuses and with both explicit flags the negative control succeeds.
- Given no callback, when TTL passes, then repeated result polls including after cleanup return 410 with session id in the documented retention window, and DCAPI has equivalent identity.

## Spec Change Log

## Review Triage Log

### 2026-11-12 — Review pass
- verdicts: 17 findings — high 0, medium 5, low 3, false 9, maybe-false 0
- findings (reviewer report order):
  - `[false]` `[reject]` Blind 1: duplicate expiry queue entries — all production expireLocked callers hold the mutex and remove the live session; subsequent accesses use missingLocked, so the same payload cannot expire twice.
  - `[false]` `[reject]` Blind 2: reused ids orphan expiry entries — put calls missingLocked, which removes both map and list entries after retention and rejects reuse during retention.
  - `[low]` `[reject]` Blind 3: multiple clock reads during sweep — wall time crossing a deadline within one sweep merely expires eligible records on that pass; fake-clock tests use a fixed clock. Adding clock parameters has negligible everyday benefit.
  - `[false]` `[reject]` Blind 4: read-only state directory prevents rotation — service only reads its persisted key; README rotation explicitly uses external sudo file replacement, outside the service mount namespace.
  - `[medium]` `[patch]` Blind 5: registry warning obscured by real-integration wording — README now labels the systemd example and shipped registry local acceptance only and requires reviewed trust/registry inputs.
  - `[low]` `[reject]` Blind 6: scheduler delay can overrun observation window — normal assertions finish around 5.2 seconds within the 8-second deadline. A host pause exceeding the entire window can fail any timed integration test; extra retries cannot recover expired metadata and add complexity.
  - `[low]` `[reject]` Blind 7: ephemeral port reservation race — real but unlikely in ordinary local execution; startup errors expose the conflict. Reservation cannot stay bound while verifier binds; adding retry orchestration is disproportionate.
  - `[medium]` `[patch]` Blind 8: test rebuild replaces checkout binary — Make now builds a temporary binary and cleans it on exit.
  - `[medium]` `[patch]` Verification 1: profile tests bypass unit/env — harness now reads committed EnvironmentFile and ExecStart, stages paths, preserves empty arguments and launches that configuration. Changed key-path/mode mutation controls fail.
  - `[medium]` `[patch]` Verification 2: late mutations tested only after deletion — added fake-clock first-operation tests for ConsumeRequestFetch and AttachISO, asserting payload removal, refusal and retained expiry.
  - `[medium]` `[patch]` Edge 1: inherited EE_VERIFIER selects stale binary — grouped with Blind 8; Make explicitly supplies the freshly built temporary executable. Parent reruns with an intentionally stale inherited path.
  - `[false]` `[reject]` Intent 1: conditional operator handoff — authoritative Story 3.1 in epics.md requires a deployable profile, documented startup, ADR, encryption and expiry; no human-only public action is required. Final done is appropriate.
  - `[false]` `[reject]` Intent 2: deployed versus tested surface — verified configuration gap is patched above; unit syntax is separately validated. Public HTTPS and actual host installation are documented boundaries, not required external deployment ACs.
  - `[false]` `[reject]` Intent 3: added harness omits intermediate drain assertions — drain implementation is unchanged and existing tests/drain_test.sh covers admission, health and successful in-flight completion; this change inherits that contract.
  - `[false]` `[reject]` Intent 4: diff alone cannot prove executed tests/commit/result — parent command outcomes establish execution; task completion, result and commit are finalized after review. This is an intermediate snapshot, not a behavioral defect.
  - `[false]` `[reject]` Intent 5: ADR chronology absent from diff — git history directly establishes ADR-004 already present at fc8793ed6f6d20b5660de709f5c04116b2d997e7 before baseline 94d5e458e5491496b349d175f7aa3530ca2333e3.
  - `[false]` `[reject]` Intent 6: orchestrator boundary — reviewer confirms no sprint-status mutation; boundary is respected, not a defect.

### 2026-11-12 — Follow-up review pass
- verdicts: 16 findings — high 0, medium 4, low 3, false 9, maybe-false 0
- Scheduling: the fourth reviewer hit the three-child concurrency limit and launched when a slot freed. All four layers completed before triage or repair. Edge-case hunter returned no findings. Board and demo-data were excluded from the staged review diff.
- findings (report order):
  - [medium] [patch] Verification 1: Claim/Complete first-operation retention untested — extended the first-operation matrix to Claim and previously claimed Complete, with payload removal and retained expiry before/after sweep. Deletion mutants in both methods fail the new cases.
  - [low] [reject] Blind 1: obsolete outcomes may occupy capacity during sweep — real at the 10,000-outcome cap, where the documented FIFO policy permits eviction. Reordering collection or adding insertion scans adds complexity for a capacity-limited corner with negligible everyday impact.
  - [false] [reject] Blind 2: sub-500ms TTL requires a minimum — polling between TTL and twice TTL yields expiry; a first sweep after twice TTL correctly discards the outcome. README measures retention from the original deadline and states the one-second janitor floor, not a new window beginning at cleanup.
  - [low] [reject] Blind 3: first beyond-window read differs from later reads — a live expired payload can yield one extra ErrExpired before becoming unknown. This lies beyond the promised observation window; new branching to unify behavior outside that window adds negligible everyday value.
  - [low] [patch] Blind 4: fixed template drain versus binary default — README now explicitly distinguishes the binary automatic default from the independent six-minute template value; changing SESSION_TTL alone does not update DRAIN_TIMEOUT.
  - [false] [reject] Blind 5: restricted account may not access installation — no failing path is shown. Documented install modes make executable/config readable and assign state/key ownership to the service account. Read-only protections permit reads. Actual account/sandbox execution remains explicitly outside staged-process evidence.
  - [medium] [patch] Blind 6: CI lacks native unit validation — the CI-invoked profile harness now runs systemd-analyze verify with the executable path staged. Missing validator is explicitly skipped; this host exercised it successfully.
  - [medium] [patch] Blind 7: permission assertion covers only source key — profile_command now asserts 0600 on the staged installed key passed to the process. A mutation changing only the installed key to 0644 is rejected.
  - [medium] [patch] Blind 8: replacement-key rotation untested — real processes now cover file swap while running, unchanged running JWKS, drain/restart, changed JWKS and stable replacement on another restart, with mode 0600.
  - [false] [reject] Intent 1: installed service versus staged surface — no service defect demonstrated. Committed command/config and native unit syntax are exercised; actual host account/sandbox execution is explicitly outside evidence, and public deployment is excluded by intent.
  - [false] [reject] Intent 2: expiry could require a timestamp — matrix requires identifiable expiry, not a new timestamp field. Both routes return id and ErrExpired, with the deadline/retention documented.
  - [false] [reject] Intent 3: unconditional observation guarantee — intent explicitly requests bounded observation; README discloses capacity and restart limits.
  - [false] [reject] Intent 4: not all drain semantics exercised by profile — carried: unchanged drain logic and existing tests/drain_test.sh cover admission, draining health and in-flight completion. Profile tests SIGTERM exit; no regression identified.
  - [false] [reject] Intent 5: no valid encrypted proof in profile test — intent requires preservation, verification implementation is unchanged, and parent make test-go-zk exercises main/crypto with race detection. Profile tests encrypted request defaults; no weakening demonstrated.
  - [false] [reject] Intent 6: diff cannot prove execution — carried: parent command outcomes establish execution, recorded in Auto Run Result. Intermediate diff is not terminal verification evidence.
  - [false] [reject] Intent 7: ADR absent from diff — carried: ADR-004 exists unchanged, accepted before baseline as already established by canonical history evidence in the first review. An unchanged ADR is absent from a diff by design.

## Design Notes

Bound expiry metadata rather than keeping payloads. Document observation-window, capacity and restart limits honestly. Local acceptance deployment does not imply production certification.

## Verification

**Commands:**
- `make check` — fast CI checks pass.
- `make test-go-zk` — main/crypto tests pass with race detector.
- `make test-rp-kit` — all local process/profile assertions pass; store/HTTP matrix rows also covered by Go suites.
- `git diff --check` — no whitespace errors.
- `systemd-analyze verify rp-kit/zkverify.service` — validate unit where available, using safe staged install paths or recording exact host limitation.

## Auto Run Result

Status: done

Follow-up review of implementation at full revision `a7825fad893bf6c027bbcea602299fae0851c3d5`, against baseline `94d5e458e5491496b349d175f7aa3530ca2333e3`. The accepted ADR-004 profile remains a configuration over the existing verifier: encrypted requests, persisted private response key and identifiable bounded callback expiry. No human decision is needed for Story 3.1's local acceptance requirements.

Files changed in this follow-up:
- `verifier/go/oid4vp/session_test.go`: first-operation Claim and claimed Complete expiry retention, payload removal and before/after-sweep checks.
- `tests/rp_kit_test.py`: installed-key mode assertion, native systemd syntax verification with staged executable, and real-process replacement-key rotation/persistence acceptance.
- `rp-kit/README.md`: distinguish the binary's derived drain default from the template's explicit independent setting.
- `tests/README.md`: index added coverage and state the service-account/sandbox evidence boundary.
- This spec: complete per-finding follow-up triage and execution evidence.

Review: all four layers completed; edge-case hunter reported no findings. Sixteen findings individually triaged above: five patch entries (high 0, medium 4, low 1), zero deferrals, eleven rejections (nine refuted claims and two low-benefit corner cases). Every rejection's specific reason is recorded in the follow-up triage rows. Existing deferred-work entries and the spec's empty deferred list were not modified.

Follow-up review recommended: false. This is a follow-up pass, with no high patch entry and no specific remaining unverified risk requiring another code-review cycle. Actual installed service-account/sandbox behavior and external HTTPS deployment remain outside this local subprocess evidence; neither is represented as verified.

Verification performed on 2026-11-12 after the patches:
- `make check`: PASS, including lint/vet/format/ratchet, Android static checks, race-enabled store tests and Python regression/gate suites.
- `make test-go-zk`: PASS, race-enabled main/crypto suites (cached on the final rerun; an initial full execution also passed in 78.923s).
- `EE_VERIFIER=/definitely/stale make test-rp-kit`: PASS against the freshly built temporary executable; unit syntax, startup controls, encrypted defaults, redirect/DCAPI missing-callback identity, stable keys, replacement rotation and SIGTERM exit all passed.
- Mutation controls: PASS; reverting only Claim's or Complete's expiry retention to deletion fails the new first-operation case, and changing only the staged installed key to 0644 fails the mode assertion. Production source restored after each temporary mutation.
- `systemd-analyze verify`: direct unit validation initially reported the absent host-installed executable; verification with only ExecStart's executable path staged passed, and the updated process harness repeats that native check successfully. No host service was installed.
- `git diff --check`: PASS after removing the pre-existing trailing blank line in this spec.
- `graphify update .`: completed; reported existing parser limitations for three files and stale community-label warnings. Graph remains a navigation aid, not verification evidence.

Finalization is scoped to the five reviewed follow-up files. The pre-existing orchestrator change in `sprint-status.yaml` is intentionally left uncommitted and untouched, overriding the workflow's generic clean-tree requirement. No board status is used as evidence. No existing deferred-work ledger entry was changed, reopened or rewritten; no demo-data committed; no push performed.
