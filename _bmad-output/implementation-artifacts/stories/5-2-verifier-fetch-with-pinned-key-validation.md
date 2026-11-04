---
story_id: "5.2"
story_key: 5-2-verifier-fetch-with-pinned-key-validation
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["5.1"]
decision_gate: "G5 library compatibility; G7 invalid-fetch fallback and rollback policy."
external_gate: "No live registry host is needed for local HTTP boundary tests."
source_contract_sha256: c0c1993315645616fb93a29f8b7b50e0f0b1d23d349e2f516ef64f42e5a5dfc4
---

# Story 5.2: Verifier fetch with pinned-key validation

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 5.2](../../planning-artifacts/epics.md#story-52-verifier-fetch-with-pinned-key-validation).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 5.2: Verifier fetch with pinned-key validation

As a verifier operator,
I want the verifier to fetch the list, validate its signature against a pinned key, and refuse everything when it has neither network nor a local list,
So that the accepted circuit set is fail-closed and tamper-evident.

**Acceptance Criteria:**

- **Given** a verifier started with no network and no local list,
- **When** it receives a presentation,
- **Then** it refuses, with a log naming the missing prerequisite,
- **And** with a fetched, signature-valid list it accepts exactly the listed circuits,
- **And** a test that flips one byte in the list observes refusal,
- **And** the local pinned list remains available as a fallback and the allowlist still keys on
`id` + `circuit_hash` + params.
- **And** a listed entry whose minimum library version exceeds the verifier's own version is
refused with that reason even though its signature and hash are valid,
- **And** the document carries an issue date and an expiry, and a signature-valid document past
its expiry is refused as `stale-list` rather than continuing to accept withdrawn circuits,
**Covers:** FR22. **Source:** O4 acceptance criteria (i)–(ii); O4 risks (unsigned/compromised list).

## Analysis and implementation boundaries

Validate fetched and pinned local documents with the same code and expiry rules. Bound fetch time/size, preserve tuple matching and reject unsupported runtime requirements. G7 must say whether invalid remote evidence allows validated local fallback and how older signed list versions are treated.

**Prerequisites for full acceptance:** [Story 5.1](5-1-signed-circuit-list-document-and-generator.md)

**Decision/evidence gate:** G5 library compatibility; G7 invalid-fetch fallback and rollback policy.

**External acceptance boundary:** No live registry host is needed for local HTTP boundary tests.

## Existing entry points

- [verifier/go/circuits/registry.go](../../../verifier/go/circuits/registry.go)
- [verifier/go/main.go](../../../verifier/go/main.go)
- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `verifier/go/circuits/signed_list.go`
- `verifier/go/circuits/fetch.go`

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

Exercise no network/no local list, valid fetch, tamper, unknown key, expired fetched and fallback files, too-new runtime, duplicates, timeout and oversized payload. Assert refusal before expensive verification and never accept a circuit from an invalid document.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
