---
story_id: "8.8"
story_key: 8-8-second-zk-family-readiness
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["2.1"]
decision_gate: "G11 future proof families need a separate scope decision; only dispatch isolation is in this story."
external_gate: "No BBS provider is needed to test refusal of unsupported families."
source_contract_sha256: 37a98fa0b871941c8edec5477f0a71dca7ff1763a3b1347438082a0e135ae5ec
---

# Story 8.8: Second ZK family readiness

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 8.8](../../planning-artifacts/epics.md#story-88-second-zk-family-readiness).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 8.8: Second ZK family readiness

As an implementer watching ETSI TS 119 476-2,
I want the carrier to keep one format family per scheme so a BBS-family path can be added without a dialect collision,
So that a change of EU-preferred system is an addition rather than a redesign.

**Acceptance Criteria:**

- **Given** TS14 (multi-message signatures) is exploratory and the ARF refinement round is open,
- **When** the format-identifier rule is stated and enforced,
- **Then** one format family per scheme is the rule, enforced in the dispatch path,
- **And** BBS/MSS stays out of the carrier,
- **And** the rule is recorded where a second family would be added.
- **And** the dispatch path has a refusal branch for bytes matching no format family, so
one-family-per-scheme is enforced at the default rather than only on the paths that name a family,
**Covers:** FR46. **Source:** risk R2 mitigation.

## Analysis and implementation boundaries

Document one family per scheme at the existing dispatcher. Retain unsupported-family refusal without implementing BBS/MSS or overloading Longfellow bytes with a new dialect. Pin any TS14 maturity claim or mark it UNVERIFIED.

**Prerequisites for full acceptance:** [Story 2.1](2-1-carrier-runner-over-existing-cases.md)

**Decision/evidence gate:** G11 future proof families need a separate scope decision; only dispatch isolation is in this story.

**External acceptance boundary:** No BBS provider is needed to test refusal of unsupported families.

## Existing entry points

- [verifier/go/oid4vp/carrier.go](../../../verifier/go/oid4vp/carrier.go)
- [verifier/go/oid4vp/carrier_test.go](../../../verifier/go/oid4vp/carrier_test.go)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/PROOF-FAMILY-DISPATCH.md`

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

Test known family, unknown family, malformed payload and an ambiguous candidate envelope; verify unsupported input cannot select another scheme through sniff fallback or flow mixing.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
