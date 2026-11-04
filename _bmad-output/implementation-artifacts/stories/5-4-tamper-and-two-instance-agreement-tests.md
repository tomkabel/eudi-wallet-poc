---
story_id: "5.4"
story_key: 5-4-tamper-and-two-instance-agreement-tests
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["5.2", "5.3"]
decision_gate: null
external_gate: "Two independently operated instances are required for the stated independent agreement claim."
source_contract_sha256: e525f002d0f69f60a42363f4a36b99ddb855271e747f38011955d1f169b1c27f
---

# Story 5.4: Tamper and two-instance agreement tests

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 5.4](../../planning-artifacts/epics.md#story-54-tamper-and-two-instance-agreement-tests).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 5.4: Tamper and two-instance agreement tests

As an auditor checking a two-party integration,
I want a tamper test and a test where two independently operated instances configure the same set from one document,
So that agreement is demonstrated rather than assumed.

**Acceptance Criteria:**

- **Given** two independently operated verifier instances configured from one registry document,
- **When** each reports its accepted set,
- **Then** the sets are identical,
- **And** a tampered document is refused by both,
- **And** both tests are named in `tests/README.md` with what they prove, and wired into `make
check` or the matching CI job,
- **And** the comparison pins the document version and fetch time it compared, so a legitimate
version difference fails as a version mismatch rather than as a two-party disagreement,

**Covers:** FR24. **Source:** O4 acceptance criteria (ii)–(iii); O4 validation.

## Analysis and implementation boundaries

Provide a deterministic two-process regression first, then retain a separate independent-operator run. Compare accepted tuples and source document identity, not just circuit counts. Record fetch times without mistaking legitimate different revisions for equal-input disagreement.

**Prerequisites for full acceptance:** [Story 5.2](5-2-verifier-fetch-with-pinned-key-validation.md), [Story 5.3](5-3-wallet-scheme-resolution-from-the-registry.md)

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** Two independently operated instances are required for the stated independent agreement claim.

## Existing entry points

- [verifier/go/circuits/registry_test.go](../../../verifier/go/circuits/registry_test.go)
- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `tests/circuit_registry_agreement.sh`
- `docs/measurements/CIRCUIT-REGISTRY-AGREEMENT.md`

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

Flip one signed byte and require both consumers to refuse; compare identical input/version and then intentionally skew versions. Local subprocesses prove the regression only, not independent operation.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
