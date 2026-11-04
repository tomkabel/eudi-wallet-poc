---
story_id: "7.7"
story_key: 7-7-eccg-clause-map-and-algorithm-inventory
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 pin ECCG catalogue version and mechanism scope."
external_gate: "Official Agreed Cryptographic Mechanisms catalogue and primary primitive references."
source_contract_sha256: d879cb88c41a33dc91490ac4d28a61582ae1c69fb46fb4592ce3e6db52b2e8e5
---

# Story 7.7: ECCG clause map and algorithm inventory

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 7.7](../../planning-artifacts/epics.md#story-77-eccg-clause-map-and-algorithm-inventory).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 7.7: ECCG clause map and algorithm inventory

As a compliance reader,
I want a clause map against the ECCG Agreed Cryptographic Mechanisms v2.0 for the non-ZKP primitives plus an algorithm inventory,
So that the primitives in use are traceable to the catalogue the ARF names.

**Acceptance Criteria:**

- **Given** the primitives in use (P-256, ECDH-ES, AES-GCM, SHA-256),
- **When** the clause map and inventory are produced,
- **Then** each primitive is mapped to its catalogue entry,
- **And** the inventory states that the catalogue covers no ZKP, anonymous-credential, BBS or
zero-knowledge scheme,
- **And** the map does not claim that the ZKP scheme is covered.

**Covers:** FR36. **Source:** S12; A11; §2.1 conflict paragraph.

## Analysis and implementation boundaries

Inventory actual cryptographic operations, including new registry Ed25519 if present at execution time, with purpose, key size and code path. Map non-ZKP primitives to exact catalogue entries; keep the Longfellow scheme and its soundness claim separately scoped.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 pin ECCG catalogue version and mechanism scope.

**External acceptance boundary:** Official Agreed Cryptographic Mechanisms catalogue and primary primitive references.

## Existing entry points

- [docs/analysis/ZKP08-ANALYSIS.md](../../../docs/analysis/ZKP08-ANALYSIS.md)
- [docs/decisions/ADR-003-MINIMAL-JWE.md](../../../docs/decisions/ADR-003-MINIMAL-JWE.md)
- [verifier/go/oid4vp/](../../../verifier/go/oid4vp/)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/conformity/ECCG-ALGORITHM-INVENTORY.md`

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

Verify each algorithm is actually used and each catalogue match includes constraints rather than name-only similarity. Mark unknowns and excluded ZKP coverage explicitly; do not infer scheme approval from approved component primitives.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
