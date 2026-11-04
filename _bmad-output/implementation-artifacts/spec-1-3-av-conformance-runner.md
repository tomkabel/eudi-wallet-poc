---
title: '1.3 AV conformance runner'
type: 'feature'
created: '2026-10-23'
status: 'awaiting-operator'
operator_actions:
  - Build an independent wallet or verifier at a recorded revision and provide its executable audit adapter.
  - Run the adapter against all mapped test IDs and retain machine-readable evidence.
---

# 1.3 AV conformance runner

<intent-contract>

## Intent

**Problem:** The AV registry and fixtures provide traceability but no executable
candidate report that distinguishes failures from missing evidence.

**Approach:** Run real registry-linked verifier probes, report at least 20
distinct clauses and eight fail-closed negative controls, and expose an
external-candidate adapter whose unavailable evidence remains `NOT_EXECUTED`.

**Boundaries:** Keep partial scope explicit, preserve registry and fixture pins,
never fabricate external evidence, and never modify `sprint-status.yaml`.

</intent-contract>

## Implementation

The runner executes 20 distinct registry-mapped clauses and eight negative
controls against the repository verifier. It emits readable and JSON evidence,
keeps failures and unavailable candidates distinct, and is wired into the fast
checks. See `docs/profiles/AV-CONFORMANCE-RUNNER.md`.

## Review Triage Log

### 2026-10-23 — Review pass

- verdicts: 7 findings — high 0, medium 1, low 4, false 2, maybe-false 0
- findings:
  - `[low]` `[patch]` Index link used the deleted `-2` spec filename — corrected to this canonical spec.
  - `[low]` `[patch]` Blind review independently found the same broken index link — same verified outcome as the edge-case finding; link corrected.
  - `[medium]` `[patch]` Negative controls were counted from source names — count now comes from passing executed Go JSON subtests; the positive empty-spec case is no longer named as a negative.
  - `[low]` `[patch]` Blind review separately observed the source-count gap — same root cause and fix as the verification-gap finding.
  - `[low]` `[patch]` Report revision omitted dirty working-tree state — report now appends `+dirty` when tested code differs from HEAD.
  - `[low]` `[patch]` Unknown external adapter test IDs were ignored — adapter now rejects IDs absent from the pinned registry.
  - `[false]` `[reject]` Intent audit reported no intent contract in the final canonical spec — the contract is present; its review diff snapshot predated its addition.
  - `[false]` `[reject]` Intent audit cited an obsolete `-2` filename — the canonical spec is now at the id-keyed path and the documentation link resolves to it.

## Auto Run Result

Status: awaiting-operator

The in-repository runner, 20 clause mappings, ten executed negative controls,
regressions, documentation and CI wiring are complete. `make check` passes.
The full audit exits 1 as designed: clauses AV-A.6-016, AV-A.6-017 and
AV-A.6-018 fail against current fallback request behavior, and mapped results
for the unavailable independent candidate are `NOT_EXECUTED`.

No complete AV conformance claim is made. The orchestrator-owned
`sprint-status.yaml` was not modified.

Review pass: one verification-gap finding was patched by counting only passing
executed Go negative subtests. One broken index link was corrected. The report
records the checkout as `+dirty` while uncommitted changes are tested.


## 2026-10-23 — Retrospective follow-through

The [runner hardening record](spec-1-3-av-conformance-runner-hardening.md)
completes local fixes for retrospective F3–F7: malformed UTF-8 and escaped
surrogates, conflicting JSON keys, process-group timeout cleanup with bounded
drain, argv startup-versus-executed failure states, and synthetic full-audit
success verification. Independent review also added excessive-nesting,
nonfinite-number and exact schema-version controls. Both public adapter
contracts describe the changed argv semantics and timeout ownership boundary.

Verification at dirty checkout HEAD
`6cf84df5806ecd22792eade3b8ced8d14f148202`: runner38/38 PASS and
`make check` exit0; parent executed Android212/JVM6 tests and Go-ZK race checks
successfully. The actual local audit still retains its three policy failures;
synthetic evidence does not establish independent execution. Output quotas
remain a documented, separately deferred resource-budget issue. All review
findings and commands are retained in the linked record.

Status stays `awaiting-operator`. Required remaining action: build and execute
an independent candidate at its recorded revision, retain every mapped clause
outcome through the documented adapter, and review that evidence. Story1.6 owns
publication and independent reproduction separately.
