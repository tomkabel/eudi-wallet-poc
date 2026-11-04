---
story_id: "5.3"
story_key: 5-3-wallet-scheme-resolution-from-the-registry
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: ["5.1", "5.2"]
decision_gate: "G5 runtime-stamp compatibility; G6 authenticated list-version binding; G7 fallback precedence."
external_gate: "Wallet runtime metadata must be available from the bundled artifact."
source_contract_sha256: c1fad9208f81454b00c4343dbc52437565eb943f52e795cf5ae57a5997a05f00
---

# Story 5.3: Wallet scheme resolution from the registry

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 5.3](../../planning-artifacts/epics.md#story-53-wallet-scheme-resolution-from-the-registry).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 5.3: Wallet scheme resolution from the registry

As a holder,
I want the wallet's `ZkPresenter` to resolve its scheme from the same document the verifier uses,
So that wallet and verifier cannot disagree about which circuit is acceptable.

**Acceptance Criteria:**

- **Given** a fetched and validated registry document,
- **When** the wallet resolves a proving scheme,
- **Then** it uses that document,
- **And** on a failed or absent document the wallet resolves from the same pinned local list the
verifier falls back to, recording the pinned document version it used, and refuses the ZK path
only when that list also has no acceptable circuit — the offline rule is stated once for both
sides so a registry outage cannot leave the verifier accepting a pinned set the wallet will not
present against,
- **And** the resolution is covered by a test.
- **And** the resolved document version is recorded on the presentation, and the verifier refuses
a presentation resolved against a different version, naming the skew, so a version difference
never surfaces as an unexplained refusal,
**Covers:** FR23. **Source:** O4 affected components; O4 acceptance criteria (iii).

## Analysis and implementation boundaries

Consume the same signed document as the verifier and generate the local wallet asset from it. Define how list_version travels in a presentation and is bound to authenticated/session evidence before changing a carrier format; no arbitrary unsigned field may establish provenance.

**Prerequisites for full acceptance:** [Story 5.1](5-1-signed-circuit-list-document-and-generator.md), [Story 5.2](5-2-verifier-fetch-with-pinned-key-validation.md)

**Decision/evidence gate:** G5 runtime-stamp compatibility; G6 authenticated list-version binding; G7 fallback precedence.

**External acceptance boundary:** Wallet runtime metadata must be available from the bundled artifact.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkPresenter.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/ZkPresenter.kt)
- [docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md](../../../docs/decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/CircuitListResolver.kt`

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

Assert matching network and offline resolution, generated-asset byte drift, expiry, missing circuit, stamp mismatch and list-version skew. A skew result must name the skew; do not claim current carrier support for a field not yet defined.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
