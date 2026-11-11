---
title: '3.3 Fail-closed RP-register interface and mock'
type: feature
created: '2026-11-12'
status: done
baseline_revision: 8fdce0b5c0fa379a443f435e221abae6526bf719
review_loop_iteration: 0
followup_review_recommended: true
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/DEVELOPMENT.md'
  - '{project-root}/docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md'
  - '{project-root}/docs/decisions/ADR-005-INTENDED-USE-SOURCE.md'
  - '{project-root}/docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md'
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Story 3.3 requires a replaceable RP-register lookup and mock. Current presentation endpoints issue requests without checking registered intended uses; an absent service must never silently become an allow.

**Approach:** Implement ADR-007's public Go interface, six-field typed registration, shared synthetic file-backed corpus and bounded positive cache. Gate existing presentation surfaces, distinguish missing implementation from configured failure and missing entry, and prove behavior with executed HTTP controls.

## Boundaries & Constraints

**Always:** ADR-007 resolves cache precedence: positive entries strictly younger than configurable TTL (default 15m) may allow without a network lookup; log the bounded cache decision and age. At or beyond TTL discard/relookup; never rescue errors with stale data, cache errors or refresh age on read. Bound capacity, network time/body and concurrency safety. Missing/nil implementation always refuses. Preserve RPRegister.IntendedUse(RegisterIdentity) (RegisteredIntendedUse, error), ErrNoImplementation, ErrRegisterUnreachable and ErrNotFound. Mirror ADR-005 fields and doctype-qualified predicate pairs. Match server-configured entity/service identity; validate nonempty registration and every credential/claim, including namespace. All redirect/DCAPI creation, request-release and callback surfaces enforce policy using stored session ClientID/query when available. Preserve encryption, drain, shedding-before-Claim and single-use/result mechanics. Log stable reasons and cache/stale decisions without holder data or secrets. Shared synthetic corpus has three positive RPs; omit negative identity for true absence. Own this story's register package, verifier integration, fixtures, tests/docs, build wiring and impacted local mock opt-ins. You are not alone; preserve others' changes.

**Never:** Write, stage, commit or revert sprint-status.yaml; its status is orchestration, not verification. Commit demo-data/secrets, push, import private ee-eudiw material, bypass nil policy for tests, modify this intent-contract, expand wallet/certificate parsing, or claim an authority API exists. The proposed HTTP adapter is a local contract seam, not public-service evidence. Story 3.3 requires no human-only action: finish done on passing local ACs; if an actual human-only AC emerges, complete/commit all agent work and finalize awaiting-operator with nonempty imperative operator_actions and matching Auto Run Result.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Unconfigured | No register/nil | Refuse both flows, no session or request release | ErrNoImplementation, log missing prerequisite |
| Registered | Exact identity and all allowed doctype/predicate pairs | Create/fetch and proceed to existing verification | Explicit mock evidence only |
| Missing/disallowed | Omitted RP, mismatched identity, malformed/empty use, wrong doctype/namespace/predicate or one unauthorized multi-query claim | Refuse without valid=true or request release | Named not-found/invalid/unauthorized |
| Configured service failure, cold cache | Connection/DNS error, absolute timeout, HTTP 5xx | Refuse distinctly from unconfigured | Register error logged, no cache hit required |
| Fresh cache | Matching positive key strictly within TTL, provider possibly down | May allow with explicit age/provenance/cache-hit-within-ttl log | Do not imply network freshness |
| Stale cache | Exact TTL boundary or older, provider failure/not-found | Discard/relookup/refuse; log stale decision | No stale fallback |
| Isolation/resources | Different key, capacity, concurrent access, caller-mutated returned record | No cross-identity allow or unbounded cache; defensive copies | Race-safe bounded eviction |
| Session lifetime | Registration expires/changes after creation | Request/callback re-evaluates stored identity/query | Refuse material release or valid=true on denial |

</intent-contract>

## Code Map

- `verifier/go/present.go:22,164,209,227,262,287` — presenter dependency; redirect creation, request release, callback. `oid4vp/dcql.go:46` defines claims; validate before indexing every credential.
- `verifier/go/iso.go:67,140,187` — DCAPI creation/fetch/callback; preserve admission/Claim ordering.
- `verifier/go/main.go:364,562,611` — flags, injection, route setup; unconfigured server stays healthy but refuses presentations.
- `docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md` — mandatory interface, TTL and fresh-cache exception; ADR-005 fixes six fields and qualified predicate identity. Local architecture evidence, not current RIA availability evidence.
- `rp-kit/zkverify.service:11`, `zkverify.env.example`, `README.md:34` — deployment default remains unconfigured; local walkthrough explicitly selects mock.
- `tests/rp_kit_test.py:25,220,262,325` — actual shipped unit/env staging; retain default refusal control and explicit synthetic positive setup.
- `verifier/go/iso_test.go:45`, `carrier_flag_test.go:53`, related HTTP presenter helpers — inject explicit mock where gates run; no nil bypass.
- `tests/e2e.sh:111`, `tests/drain_test.sh:39`, `sandbox/server.py:96`, `demo/demo.sh:49` — explicit synthetic registration for existing positive flows, corpus covers their ages/doctypes.
- `Makefile:110`, `.github/workflows/ci.yml` — standalone race suite in fast CI; root handlers require Rust staticlib. Index new suites/docs in `tests/README.md`, `docs/README.md`.
- Code-review graph returned unrelated RootLayout repo/revision; source investigation is evidence instead.

## Tasks & Acceptance

**Execution:**
- [x] `verifier/go/rpregister/` — public interface/default, strict model validation, file mock, bounded cache. Add bounded HTTP adapter for expressly provisional local contract, verified HTTPS remotely and loopback HTTP for local controls only; refuse redirects.
- [x] `rp-kit/register/` — shared version-labelled synthetic three-RP corpus, absent negative identity and reuse notes for Story 4.6 without claiming future fixtures exist.
- [x] `verifier/go/main.go`, `present.go`, `iso.go` and handler tests — configuration, all presentation gates and matrix controls through real HTTP handlers/log capture.
- [x] `rp-kit/REGISTER-CONTRACT.md`, service/env/README and impacted demos/harnesses — publish identity/JSON/interface/error/cache/timeout/eviction contract and reproducible mock commands; preserve unconfigured deployment/encrypted defaults.
- [x] `Makefile`, CI, `tests/rp_kit_test.py`, test/document indexes — register unit/race/process tests; repair affected checks through explicit synthetic opt-in.

**Acceptance Criteria:**
- Given no register implementation, when either presentation entry executes, then it refuses and logs the named missing register prerequisite.
- Given the shipped mock and all registered uses, when both flows execute, then they use the same interface; omitted RP or unauthorized claim refuses.
- Given configured unreachable, timed-out or 5xx service and no cached entry, when presentation executes, then it refuses and distinctly logs register error.
- Given cache entries before or at/after the published age bound, when authorization runs, then only the fresh entry can allow and cache/stale decisions are logged; stale failures refuse.
- Given completed implementation, when checks/review finish, then all matrix cases have executed passing controls and orchestrator bookkeeping remains untouched.

## Spec Change Log

## Review Triage Log

### 2026-11-12 — Review pass
- verdicts: 22 findings — high 0, medium 16, low 1, false 5, maybe-false 0
- Scheduling: all four layers completed before triage. Three child slots required intent-alignment to launch after verification-gap returned; no layers skipped.
- findings (report arrival order):
  - `[medium]` `[patch]` Gap 1: slow-body test cannot detect removed absolute deadline — preverified mutation leaves eventual empty-body refusal green; delayed valid body and exact bounded timeout assertion required.
  - `[medium]` `[patch]` Gap 2: multi-credential creation gate lacks denial control — preverified removed startSessionMulti gate escapes helper/single controls; add actual multi HTTP denial.
  - `[medium]` `[patch]` Gap 3: stored identity/query not distinguished from presenter defaults — preverified substitution escapes aligned fixtures; test mismatched stored policy at every HTTP release/callback surface.
  - `[medium]` `[patch]` Blind 1: duplicate/case-variant JSON overwrites uses — encoding/json accepts conflicts and case-insensitive matches; reject duplicates recursively and require exact existing model keys.
  - `[low]` `[reject]` Blind 2: memberState ZZ passes alphabetic validation — real metadata weakness against ADR-005 ISO wording, but memberState does not grant uses and ordinary EE fixtures are valid; maintaining a country allowlist adds complexity for an unlikely metadata error.
  - `[medium]` `[patch]` Blind 3: nil function/map/slice implementation can panic — existing reflection checks only Pointer; extend nil-capable checks with typed nil function control.
  - `[medium]` `[patch]` Blind 4: provider mutex creates unbounded outage queue — each cold waiter incurs another timeout; shorten lock and bound lookup admission with existing refusal error, preserving fresh hits.
  - `[medium]` `[patch]` Blind 5: arbitrary provider error returned to caller — registerGate emits err.Error from replaceable adapter; use fixed category text and sensitive-error control.
  - `[medium]` `[patch]` Blind 6: fresh handler window is flaky — 40ms includes session scheduling; make fresh observation deterministic, expire after confirmed hit.
  - `[medium]` `[patch]` Blind 7: HTTP negatives accept wrong error sentinel — only nonnil asserted; assert exact invalid/unreachable/not-found categories.
  - `[medium]` `[patch]` Blind 8: stale lifecycle tests omit removed entry/permission — add 404/refreshed unauthorized record for both flows and unclaimed denied callback assertions.
  - `[medium]` `[patch]` Blind 9: strict file refusal boundaries unprotected — add malformed corpus version, identities/pairs, fields, trailing JSON and size controls.
  - `[medium]` `[patch]` Edge 1: cold callers queue without bound — same verified mutex defect as Blind 4; same bounded-admission repair.
  - `[medium]` `[patch]` Edge 2: DCAPI request may release expired session after lookup — Get precedes slow gate without subsequent store check; revalidate request/page after lookup and test expiry during success.
  - `[medium]` `[patch]` Edge 3: duplicate field replaces uses — same verified parser defect as Blind 1; same strict decoder repair.
  - `[medium]` `[patch]` Edge 4: nil nonpointer source reaches method — same verified defect as Blind 3; same nil checks.
  - `[false]` `[reject]` Intent 1: literal outage refusal differs from cache — ADR-007 explicitly resolves precedence for bounded fresh cache, and implementation/logs match.
  - `[medium]` `[patch]` Intent 2: helper coverage does not prove multi outer path — verified Gap 2/3 overlap; add multi/stored HTTP controls while retaining resource unit tests.
  - `[false]` `[reject]` Intent 3: local failure is not authority integration — story requires interface/mock rather than live authority acceptance; provisional seam is explicit and establishes required failure behavior.
  - `[false]` `[reject]` Intent 4: positive process evidence opts into mock — intended default is refusal, proven by shipped-profile control; positives make no authority registration claim.
  - `[false]` `[reject]` Intent 5: snapshot lacks final commit/result — correct intermediate in-review state; finalization and canonical commit checks follow review.
  - `[false]` `[reject]` Intent 6: operator/terminal handoff absent — no human-only AC in Story 3.3, so operator condition inapplicable; terminal done follows review.

Repairs applied: all twelve root-cause patches listed above are present; their targeted controls and parent full checks pass. No deferrals.

## Design Notes

Fresh-cache authorization is bounded staleness per ADR-007, not proof a live register was contacted. A proposed HTTP implementation gives actual connection/timeout/5xx controls without claiming a RIA interface. Polling results remains available after refusal; new valid=true decisions require approval.

## Verification

- `make check` — fast checks/lint/standalone race controls pass.
- `make test-go-zk` — linked root/zk tests including redirect/DCAPI gates pass.
- `make test-rp-kit` — real profile processes prove default refusal and explicit mock success.
- `make e2e` — existing proof/carrier/drain flows pass with explicit synthetic registration.
- `git diff --check` — clean; staged files exclude sprint-status.yaml and demo-data.

## Implementation Result — 2026-11-12

Implemented the public six-field RP-register model, strict file mock and
provisional bounded HTTP adapter, positive-only TTL/capacity cache with defensive
copies, and registration authorization across both presentation flows. Shipped
service defaults remain unconfigured and encrypted; positive local harnesses
explicitly select the shared three-RP synthetic corpus. Published contract, reuse
notes, tests and documentation indexes are updated.

Executed controls:
- make check — PASS (including standalone register race suite and lint).
- make test-go-zk — PASS (root HTTP gates and zk tests, race detector).
- make test-rp-kit — PASS (staged shipped unit/environment; default refusal and
  explicit synthetic positives, persisted keys/expiry/rotation).
- make e2e — PASS (all 19 assertions, both carrier vectors).
- tests/drain_test.sh with pinned prover and local venv — PASS, 3/3.
- Targeted latest register HTTP/cache race tests and golangci-lint — PASS.
- git diff --check — PASS. No sprint-status.yaml or demo-data changes/staging.

No operator-only acceptance remains. The HTTP adapter is a local proposal and
synthetic success is not evidence of an authority service or registration.

## Auto Run Result

Status: done

Implemented Story 3.3: a public fail-closed RPRegister interface, strict six-field registration model, shared three-RP synthetic file mock, provisional bounded HTTP adapter, positive-only TTL/capacity cache and registration policy gates across redirect/DCAPI creation, release and callback surfaces. Default deployments remain healthy but refuse presentations without a configured register.

Files changed:
- verifier/go/rpregister/register.go — model, strict JSON/file/HTTP adapters and bounded positive cache with nonblocking cold admission.
- verifier/go/rpregister/register_test.go — absent/malformed/file/HTTP/deadline/TTL/capacity/copy/concurrency controls.
- verifier/go/register.go and register_test.go — all-claim policy, fixed refusal messages, outer HTTP gates, lifecycle and stored-session tests.
- verifier/go/main.go, present.go and iso.go — runtime configuration, registration gates and post-lookup expiry checks.
- verifier/go/iso_test.go, carrier_flag_test.go, av_conformance_test.go and step8_7_openid4vp_zk_test.go — explicit synthetic registration in existing gated fixtures.
- rp-kit/register/README.md and synthetic-v1.json — versioned shared three-RP corpus; omitted negative identity and future certificate-generator reuse boundary.
- rp-kit/REGISTER-CONTRACT.md, README.md, zkverify.service and zkverify.env.example — published refusal/cache/identity/resource contract and unconfigured deployment defaults.
- tests/rp_kit_test.py, e2e.sh, drain_test.sh, demo/demo.sh and sandbox/server.py — real default-refusal acceptance and explicit mock opt-ins for existing positive demonstrations.
- Makefile and .github/workflows/ci.yml — standalone register race suite in fast checks.
- docs/README.md and tests/README.md — documentation/test indexes.
- This story spec — immutable intent, implementation record, complete individual review triage and terminal result.

Review: four layers completed, 22 individual findings triaged: high 0, medium 16, low 1, false 5, maybe-false 0. Twelve distinct medium root-cause patches applied (16 medium rows include four overlaps), zero deferred. Repairs reject duplicate/case-variant JSON, handle all nil-capable adapters, bound cold lookup admission, sanitize replacement-provider errors, recheck DCAPI expiry, strengthen timeout/category/file controls and add multi-creation/stored-policy/lifecycle tests. Each patch row in the Review Triage Log records its specific repair. Five rejected false findings concern the ADR's explicit cache precedence, local-versus-authority evidence, intended mock opt-in, and intermediate commit/operator status. One rejected low finding is the alphabetical-only country-code metadata check: it cannot grant a predicate and adding a maintained country allowlist for an unlikely metadata defect was judged unnecessary complexity. All six rejections retain individual reasons in the log.

Follow-up review recommended: true. This first pass patched twelve medium entries. Specific remaining independent-review risk: the newly strict recursive JSON decoder and nonblocking cache admission were repaired after the four review layers; targeted races and full regressions validate them, but a second independent security review has not examined those repaired paths. This recommendation does not imply a failing acceptance criterion.

Parent verification after repairs:
- make check — PASS, including lint, standalone race suite and fast regressions.
- make test-go-zk — PASS, root HTTP/crypto and zk suites with race detector.
- make test-rp-kit — PASS, actual staged unit/environment default refusal and explicit synthetic positive profile.
- make e2e — PASS, all 19 assertions and both carrier vectors.
- Separate tests/drain_test.sh with pinned prover and locked venv — PASS, 3/3 on the repaired tree.
- Uncached verbose package/handler matrix controls — PASS. Matrix audit maps unconfigured/registered/missing to TestRegisterHTTPGates/StrictModelAndMock, configured failure to TestRegisterConfiguredFailuresHTTP/HTTPContractControls/HTTPBodyDeadline, fresh/stale to CacheBoundariesIsolationCopies/RegisterFreshStaleHTTP, isolation/resources to ConcurrentCache/CacheBoundedColdAdmission, and session lifetime to RegisterStoredPolicy/RegisterDCAPIExpiresDuringLookup; no skips.
- Full baseline diff and repair diff read by parent; git diff --check and staged whitespace checks PASS. Protected sprint-status.yaml and demo-data excluded from staging/commit and unchanged by this run. No push.

No human-only acceptance work is required by this interface/mock story, so terminal status is done rather than awaiting-operator. Synthetic/local success establishes no authority API, registration, external interoperability or certification. Fresh-cache allows intentionally accept bounded staleness under ADR-007. The memberState validator checks two uppercase letters, not membership in an ISO country list. Replacement RPRegister adapters must bound their own I/O; the shipped HTTP adapter enforces its absolute timeout.
