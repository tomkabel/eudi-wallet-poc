---
title: '3.2 TARA-facing adapter contract'
type: feature
created: '2026-11-12'
status: awaiting-operator
baseline_revision: c5171e8c7a70caad0d616006870a36cb201cef17
review_loop_iteration: 0
followup_review_recommended: true
context:
  - '{project-root}/AGENTS.md'
  - '{project-root}/docs/DEVELOPMENT.md'
  - '{project-root}/docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md'
warnings: [oversized]
deferred: []
operator_actions:
  - "Obtain institutional approval and the current RIA agreement or SLA for TARA demo access."
  - "Register the test application and exact HTTPS callback with RIA and receive client credentials through the approved channel."
  - "Configure the HTTPS proxy, protected client environment and a PEM bundle containing only current TARA-approved root certificates."
  - "Set runtime prerequisite confirmations only after approval, run make test-tara-integration, complete a real browser authentication and retain the resulting report and exit status."
---

<intent-contract>

## Intent

**Problem:** Story 3.2 requires a defined TARA OIDC authentication leg and an integration test against a registered TARA test client; neither exists. A live run needs an institution's RIA agreement, registered HTTPS callback and client credentials that this agent cannot obtain.

**Approach:** Deliver a narrow, executable reference adapter and bounded live integration runner alongside the RP profile, with offline protocol controls and an honest machine-readable external attempt. Complete and commit agent work, then hand off human prerequisites as awaiting-operator.

## Boundaries & Constraints

**Always:** Follow ADR-004's parallel authentication/presentation boundaries. TARA authorization code flow uses server-held credentials, state bound to the initiating browser, nonce, exact registered callback, discovery issuer, RS256 signature selected by kid from JWKS, issuer/audience/azp/time/assurance checks and single-use expiring transactions. Gate every new flow and callback on explicit runtime agreement confirmation and registered test-client configuration. Network errors/timeouts deny authentication and make integration BLOCKED; malformed or invalid authentication must never PASS. Bounded network/body/transaction/run resources; no TLS verification disabling or redirected credential POSTs. Live issuer is exactly https://tara-test.ria.ee; any synthetic provider override is confined to tests and cannot yield live PASS. Document certificate trust and operational responsibilities from primary sources. Use locked cryptography/jwcrypto rather than expanding the verifier's ES256 JOSE surface. Run real offline HTTP/provider controls. You are not alone; preserve others' edits. Own only this story's adapter, runner, tests, documentation and build registration. Never write, stage, commit or revert sprint-status.yaml; it is orchestrator-owned regardless of state. Never commit demo-data or credentials. No push.

**Never:** Claim mock or discovery-only success is a live TARA integration. Add TARA into the wallet verifier or treat age predicate as identity. Ask for or invent institutional approval, secrets or successful human authentication. Log tokens, codes, credentials, callback query strings or personal identifiers in reports. Modify this intent-contract.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Local protocol success | Synthetic provider, valid signed code-flow token and browser binding | Reference client authenticates, separate local-test evidence | Never live evidence |
| Missing prerequisites | Absent runtime RIA confirmation or client configuration | No login or token exchange, integration BLOCKED | Named missing prerequisite, nonzero runner |
| Unreachable provider | Connection failure, timeout or HTTP failure | No authenticated result, integration BLOCKED | Sanitized reason |
| Invalid response | Bad state/browser binding/nonce/issuer/audience/azp/signature/kid/time/assurance or malformed response | Refuse authentication | Failed integration, no secrets |
| Replay or expired flow | Repeated callback or transaction past TTL | Refuse and retain no reusable authorization | No authenticated result |
| Cancellation | Provider returns access_denied with valid binding | Explicit cancelled outcome | No authenticated result |
| External run | Approved registered test client and valid real browser authentication | PASS only after complete verified code exchange against pinned TARA test issuer | Absent human prerequisites remains BLOCKED |

</intent-contract>

## Code Map

- `rp-kit/README.md:166` — replace later-story TARA note with contract link. ADR-004 explicitly puts TARA alongside existing verifier.
- `verifier/go/main.go:611`, `present.go:164,752` — unchanged wallet routes and age-only result; no authentication identity contract. `internal/jose/jws.go` is intentionally ES256-only; do not widen it.
- `tests/run_external_carrier.py` — report concepts only: external PASS/FAIL/BLOCKED/ERROR, canonical revision, explicit provenance.
- `Makefile:126`, `.github/workflows/ci.yml` fast job — register local suite in check and fast CI; live runner stays opt-in. Existing rp-kit/tests path filters suffice.
- `pyproject.toml`, `uv.lock` — pinned cryptography and jwcrypto available; no unpinned dependency installation.
- Public primary source `https://e-gov.github.io/TARA-Doku/TechnicalSpecification`, fetched 2026-11-12, labels itself v1.21 03.09.2025: sections 4/5 authorization code, Basic form encoding, state/session binding, nonce, RS256/JWKS, issuer, audience, iat/exp, assurance; section 7 institutional application/test credentials/HTTPS callback; section 11 demo issuer. Verify joining terms against RIA public joining page and record exact source provenance. Never import private ee-eudiw content.

## Tasks & Acceptance

**Execution:**
- [x] `rp-kit/tara/` — implement narrow reference adapter and CLI integration runner with runtime prerequisite checks, browser-bound single-use transactions, bounded resources, secure OIDC validation and sanitized terminal JSON. A local backend behind the registered HTTPS proxy is acceptable; document exact setup. Keep live/test configuration boundaries explicit.
- [x] `tests/test_tara_contract.py` — local provider/HTTP controls covering matrix including generated RSA signatures and all validation failures; test CLI reports and absent-prerequisite no-network behavior. External successful human flow is an operator action, never a synthetic assertion.
- [x] `rp-kit/TARA-CONTRACT.md`, `rp-kit/README.md` — document inputs/outcomes, OIDC trust and cookie/session boundary, RIA prerequisite, registered client setup/run/reproduction, certificate CA trust, fail-closed behavior, explicit parallel wallet path and operator steps. Primary-source evidence and any remaining uncertainties are explicit.
- [x] `Makefile`, `.github/workflows/ci.yml`, `tests/README.md`, `docs/README.md` — offline CI registration and documentation indexes; separate opt-in live target.
- [x] `rp-kit/tara/evidence/` — execute actual live runner with available configuration; retain sanitized BLOCKED attempt with canonical revision, command/date, outcomes and missing actions. Record no fabricated PASS.

**Acceptance Criteria:**
- Given the committed contract and registered TARA test client, when the documented runner completes a browser login, then it validates the real OIDC exchange and emits PASS only after all checks; unperformed external authentication is explicitly outstanding.
- Given absent RIA agreement or unreachable TARA, when the adapter runs, then it denies authentication and records integration BLOCKED rather than passed.
- Given an integrator follows the contract, when selecting wallet presentation, then documentation routes to the existing verifier in parallel with TARA without inferring identity from an age proof.
- Given human-only prerequisites remain, when local work and reviews finish, then all agent work is committed and spec frontmatter and final Auto Run Result say awaiting-operator with nonempty operator_actions imperative strings.

## Spec Change Log

## Review Triage Log

### 2026-11-12 — Review pass
- verdicts: 21 findings — high 0, medium 11, low 2, false 8, maybe-false 0
- Scheduling: all four layers completed before triage. Three child slots required intent-alignment to launch after Blind Hunter returned. No layers skipped.
- findings (report arrival order):
  - `[medium]` `[patch]` Blind 1: inactivity timeout does not bound trickled headers/framing — verified open/read can stay active beyond body deadline; enforce an absolute provider deadline and real slow-response controls.
  - `[medium]` `[patch]` Blind 2: discovery algorithm metadata accepts wrong shapes or raises TypeError — membership expression has no list/type guard; validate and test malformed types.
  - `[medium]` `[patch]` Blind 3: mixed error/success and incomplete token response accepted — callback reads only id_token; validate success shape and refuse error mixtures.
  - `[medium]` `[patch]` Blind 4: initiating assurance/configuration not bound to transaction — callback uses replaced current Config; bind or invalidate on changed configuration with regression.
  - `[medium]` `[patch]` Blind 5: unrelated callback ends legitimate run — handler writes terminal FAIL for unknown state; refuse stray requests while continuing, retain terminal refusal for a known pending attempt.
  - `[low]` `[reject]` Blind 6: two login tabs overwrite the single cookie — real single-run demonstration limitation, unlikely in ordinary instructed one-login use; supporting concurrent tabs adds binding/session complexity. Cookie mismatch fails closed.
  - `[low]` `[patch]` Blind 7: report omits direct invocation options — record effective nonsecret port/timeout and invocation mode for reproducibility.
  - `[medium]` `[patch]` Blind 8: live CA construction untested by HTTP controls — add generated offline TLS positive and certificate rejection cases exercising the same restricted context.
  - `[false]` `[reject]` Blind 9: intermediate spec lacks terminal handoff — this is explicitly an in-review snapshot; workflow finalization follows review and user-required awaiting-operator/operator_actions is carried in intent, not yet due.
  - `[medium]` `[patch]` Verification 1: authorization request fields unchecked — preverified response_type mutation passes all 17 tests; assert complete URL/query contract and nondefault high assurance.
  - `[medium]` `[patch]` Verification 2: HTTP refusal-to-result mapping unchecked — preverified FAIL-to-PASS mutation passes all 17 tests; exercise known-transaction rejection through real handler, status, cookie and sanitized terminal result.
  - `[false]` `[reject]` Intent 1: frontmatter handoff absent — intermediate review state is expressly allowed by auditor's reading 3; parent finalizes after fixes and commits.
  - `[false]` `[reject]` Intent 2: Auto Run Result absent — terminal result is written during finalization after this mandatory review, not evidence of an implementation defect.
  - `[false]` `[reject]` Intent 3: local provider does not establish live TARA success — correct limit explicitly documented and permitted by human-only exception; no live PASS claimed.
  - `[false]` `[reject]` Intent 4: missing-prerequisite attempt establishes only unavailable prerequisites — this is exactly the recorded BLOCKED/false result, not a claim of external authentication.
  - `[false]` `[reject]` Intent 5: operator attestations do not verify agreement online — declared contract and operator handoff accurately require institutional action; no online agreement verification is claimed.
  - `[false]` `[reject]` Intent 6: board bookkeeping not verification — protected file has no staged or working-tree diff; tests and explicit command results establish local evidence.
  - `[false]` `[reject]` Intent 7: diff does not prove commit state — reviewed-diff commits and canonical revisions are verified during finalization; intermediate snapshot cannot establish terminal work, and makes no such claim.
  - `[medium]` `[patch]` Edge 1: trickled headers/chunk framing escape deadline — same verified root cause as Blind 1; same absolute deadline repair and real controls.
  - `[medium]` `[patch]` Edge 2: null/numeric algorithm metadata escapes refusal — same verified root cause as Blind 2; same explicit list/string validation.
  - `[medium]` `[patch]` Edge 3: HTTP protocol exceptions escape sanitized handling — _json catches network errors but not HTTPException; map malformed status/framing to Blocked and test both.


## Design Notes

The reference adapter is an integration demonstration, not a new deployable authentication service profile. App session creation/authorization remains the e-service's responsibility after validation. Agreement confirmation is an operator attestation, not online RIA agreement verification. A no-credentials external result stays BLOCKED even if local protocol controls pass.

## Verification

- `make test-tara-contract` — every local matrix scenario passes, including real HTTP mock provider and runner error semantics.
- `make check` — repository fast/lint checks pass.
- `make test-tara-integration` — actual available-environment attempt; absent prerequisite must exit nonzero with BLOCKED JSON, retained as evidence rather than represented as successful integration.
- `git diff --check` — clean. Inspect staged files to exclude sprint-status.yaml, demo-data and sensitive data.

### Matrix audit before review

Parent executed all 17 local tests through make check. Local success/Basic encoding, prerequisite/no-network gates, callback state/browser/replay/expiry, cancellation, claim/signature/key matrices, provider HTTP/redirect/connection/timeout, capacity/body/config bounds, real runner cookie/callback and timeout, and CLI semantics all passed. External row uses its specified missing-prerequisite branch: actual runner produced BLOCKED with no authentication. Approved real TARA/browser success remains a human operator action, not claimed covered by a synthetic test.

### Repairs and final matrix verification

All patch routes above were applied by the original implementer and parent-read against the resulting diff. Duplicate Blind/Edge deadline and metadata findings share repairs. Grouped patch counts: high 0, medium 9, low 1 (10 root causes); 12 individual patch rows, 9 rejections, zero deferrals. The only rejected real limitation is the low-benefit multiple-login-tab behavior of this single-run demonstration. Each false rejection retains its specific reason above.

The expanded suite executes 24 tests: original protocol matrix plus exact authorization query/high assurance, malformed discovery algorithm types, complete/error-mixed token responses, configuration-change refusal, real slow headers/chunk framing and malformed HTTP, generated TLS trust/hostname/expiry/invalid-bundle controls, stray callback continuation and known-state wrong-browser terminal refusal, effective CLI options. The generated HTTPS provider patches URL-origin validation only within its test to exercise the live restricted TLS context; no runtime issuer override is added.

Parent additionally registers the unavailable current RIA agreement wording in spec section 23, as required by repository evidence discipline. Existing normative clauses remain unchanged.

## Auto Run Result

Status: awaiting-operator

Agent-implementable Story 3.2 work is complete and committed at `9193bf71ffd712fb48fce304af5389aa2b19a690`. The external integration result is BLOCKED because institutional authorization, registered test-client credentials and HTTPS callback are unavailable; this is a runtime test outcome, not the story status. No live TARA authentication or production approval is claimed.

Files changed:
- `rp-kit/tara/adapter.py` — runtime gates, browser-bound code flow, strict RS256/JWKS/claim validation, configuration binding and bounded fail-closed networking.
- `rp-kit/tara/run_integration.py` — opt-in HTTPS-proxy/browser callback runner with sanitized terminal reports and effective options.
- `tests/test_tara_contract.py` — 24 offline protocol, real HTTP, slow/malformed network, generated TLS and runner regressions.
- `rp-kit/TARA-CONTRACT.md`, `rp-kit/README.md` — parallel TARA/wallet boundary, exact setup, trust and operator instructions.
- `rp-kit/tara/evidence/` — unchanged historical and fresh post-review actual missing-prerequisite attempts, current executable hashes and evidence limits.
- `Makefile`, `.github/workflows/ci.yml`, `tests/README.md`, `docs/README.md` — offline suite registration, opt-in live target and indexes.
- `spec/EE-EUDIW-TS-1.0.md` section 23 — unverified current agreement terms.
- This story spec — intent, matrix, per-finding review triage and operator handoff.

Review: all four layers completed. Twenty-one findings individually logged: high 0, medium 11, low 2, false 8, maybe-false 0. Ten root-cause patches applied (medium 9, low 1); duplicate reports share the same repairs. Zero deferrals. Nine rejections consist of eight specifically refuted intermediate/evidence claims and one low-benefit multiple-tab limitation, with reasons in the Review Triage Log.

Follow-up review recommended: true under the first-pass multiple-medium-patch rule. Specific remaining unverified risk: interoperability of the repaired code flow, restricted CA trust and real browser/proxy callback against the registered TARA test service. Offline controls establish local behavior only; an operator-run external review should inspect the retained live outcome before treating the integration as accepted.

Verification performed by the parent after repairs:
- `make check`: PASS, including all 24 TARA controls, Go vet/lint/race suites, Python lint/ratchet/format, Rust checks, Android static checks and existing fast regression suites. Parent also ran the initial 17-test implementation through make check before repairs.
- `make test-tara-integration`: actual missing-prerequisite invocation returns runner 1 / make 2 with BLOCKED and authenticated=false. Fresh retained attempt hashes match committed adapter and runner bytes.
- `git diff --check` and staged whitespace check: PASS. Parent inspected complete baseline diff and repair diff.
- Matrix audit: all local/error branches executed and passed; successful real external branch is explicitly owed by the operator and never replaced by synthetic success.
- Reviewed implementation committed before final handoff metadata. No sprint-status.yaml mutation/staging/commit/revert, demo-data commit or push.

Operator actions are the nonempty imperative YAML list in frontmatter. Complete the RIA agreement/test-client registration, HTTPS and credential/trust configuration, then run and retain the real browser integration. The reference is a bounded single-process demonstration; application sessions, operational certificate revocation handling and multiple concurrent login tabs are not established by this evidence.
