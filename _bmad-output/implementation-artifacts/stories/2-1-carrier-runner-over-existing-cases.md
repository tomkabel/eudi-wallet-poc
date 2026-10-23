---
story_id: "2.1"
story_key: 2-1-carrier-runner-over-existing-cases
status: review
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: "External candidate evidence is required for full acceptance; use 2.4 for the independent run."
source_contract_sha256: 97bf37f5283d3303fada1df2e450d30aae6a8d61204b5a0d7581a2ad604d2395
---

# Story 2.1: Carrier runner over existing cases

This is the planning record for the backlog contract and implementation boundaries.
The in-repository implementation is complete and in review. Independent external
evidence remains blocked for Story 2.4; local results do not imply full external
acceptance. The unchecked
planning tasks below are not the delivery checklist. Read the
[shared preparation contract](README.md) and the
[complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 2.1](../../planning-artifacts/epics.md#story-21-carrier-runner-over-existing-cases).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 2.1: Carrier runner over existing cases

As an implementer of a wallet or verifier,
I want a documented runner that executes the 13 existing end-to-end cases and reports which carrier a response used,
So that "which carrier" stops being an integration surprise.

**Acceptance Criteria:**

- **Given** the existing `tests/e2e.sh` cases and the Go carrier tests,
- **When** the runner is executed,
- **Then** all 13 cases are reported, both carriers are covered, and the response mode (plain or
encrypted) is named per case,
- **And** the published vectors under `tests/vectors/carrier-v1/` accompany the runner,
- **And** the suite is added to `tests/README.md` and wired into `make check` or the matching CI job (or the story records why it is excluded and how it is run instead),
- **And** a `vp_token` entry matching neither carrier, bytes matching no supported family, and
an undecryptable JWE are reported as their own outcomes: unknown carrier bytes do not fall
through to JSON parsing, and JWE decryption failures are named as decryption rather than as a
binding-property failure,
**Blocked by (external):** the external implementation named in Story 2.4. Where the external
party or hardware is unavailable, the story still closes its in-repository half and records the
state as blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR6. **Source:** O5 proposed capability; M2 deliverable and exit criteria.

## Analysis and implementation boundaries

Wrap actual existing cases without replacing their cryptographic checks; inventory case ids before retaining the historical count of 13. Emit carrier, response mode, path label, case id and observed outcome; transport/decryption failures have their own states. Apply the Epic 1 adapter lessons rather than inheriting unchecked subprocess output.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** External candidate evidence is required for full acceptance; use 2.4 for the independent run.

## Existing entry points

- [tests/e2e.sh](../../../tests/e2e.sh)
- [verifier/go/oid4vp/carrier.go](../../../verifier/go/oid4vp/carrier.go)
- [verifier/go/carrier_vectors_test.go](../../../verifier/go/carrier_vectors_test.go)
- [tests/vectors/carrier-v1/manifest.json](../../../tests/vectors/carrier-v1/manifest.json)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `tests/run_carrier_conformance.py`
- `docs/profiles/CARRIER-CONFORMANCE-RUNNER.md`

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

Assert every inventoried case is emitted exactly once; unknown family, unmatched vp_token entry, malformed envelope, undecryptable JWE and subprocess failure cannot become PASS. Record independent results separately from synthetic adapter tests.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
