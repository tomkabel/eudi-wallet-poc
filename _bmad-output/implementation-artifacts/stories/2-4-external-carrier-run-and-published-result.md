---
story_id: "2.4"
story_key: 2-4-external-carrier-run-and-published-result
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["2.1", "2.2", "2.3"]
decision_gate: null
external_gate: "Independent wallet/verifier candidate, recorded revision and permission for any publication step."
source_contract_sha256: 0472d38a7b17011df7a6bf16784a132e9d1c49b8904165013b97c60d18741d76
---

# Story 2.4: External carrier run and published result

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 2.4](../../planning-artifacts/epics.md#story-24-external-carrier-run-and-published-result).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 2.4: External carrier run and published result

As a programme owner deciding what to test against,
I want the carrier suite's result against an external implementation published whether it passes or fails,
So that the claim is checkable rather than asserted.

**Acceptance Criteria:**

- **Given** an external implementation (the Commission's reference wallet or a multipaz sample) as
target,
- **When** the suite runs against it,
- **Then** the result is published with the target named and its revision recorded,
- **And** a failure is published as a failure,
- **And** the reason the target was chosen is recorded in the report.
- **And** an external target that is unreachable or does not build is published as `BLOCKED` with
the build or reach failure output, distinct from a test failure, and the report never omits the
target,
**Blocked by (external):** the external implementation this story tests against, and its revision,
sit outside this repository. Where the external party or hardware is unavailable, the story still
closes its in-repository half and records the state as blocked with the reason and date — never as
passed, and never with a fabricated result.
**Covers:** FR8. **Source:** O5 acceptance criteria (ii); O5 validation; M2 unresolved decision.

## Analysis and implementation boundaries

Choose and document a target using reproducibility and protocol fit. Prepare the adapter and exact clean-checkout instructions locally. Retain target build logs, actual clause/case outcomes and disagreements; selecting a target does not establish execution.

**Prerequisites for full acceptance:** [Story 2.1](2-1-carrier-runner-over-existing-cases.md), [Story 2.2](2-2-binding-property-failure-naming.md), [Story 2.3](2-3-version-labelled-carrier-fixtures.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** Independent wallet/verifier candidate, recorded revision and permission for any publication step.

## Existing entry points

- [docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md](../../../docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md)
- [tests/vectors/carrier-v1/manifest.json](../../../tests/vectors/carrier-v1/manifest.json)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/CARRIER-EXTERNAL-RESULTS.md`
- `docs/profiles/carrier/external-results.json`

These are planned destinations, not files created by this story-preparation pass.
Reuse a destination already introduced by a prerequisite instead of duplicating it.
Index delivered documents in `docs/README.md` and new checks in `tests/README.md`.

## Tasks for later execution

- [ ] Recheck the current entry points and source contract; record any changed premise,
  resolve the listed decisions, and confirm prerequisite and external-evidence scope.
- [ ] Deliver the behavior or analysis described above in the proposed destinations,
  preserving the shared trust, disclosure, versioning and scope constraints.
- [ ] Verify the targeted cases below, map retained evidence to every backlog acceptance
  criterion, and run the applicable shared checks from [README.md](README.md).
- [ ] Record observed outcomes, failures and limitations with revision/environment;
  leave unavailable operator evidence pending and update lifecycle tracking only then.

## Targeted verification plan

A reachable failing candidate produces FAIL with evidence; unreachable or unbuildable produces BLOCKED with cause and target identity. Publish only when separately authorized; a local substitute cannot close the independent gate.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
