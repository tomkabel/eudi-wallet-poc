---
story_id: "4.7"
story_key: 4-7-plain-fallback-visibility-guard
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: null
external_gate: null
source_contract_sha256: 8b1874625b6e0d5984be6a12f8b3242ec5c2adb62bb3382d53b325c0cbb2154f
---

# Story 4.7: Plain-fallback visibility guard

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 4.7](../../planning-artifacts/epics.md#story-47-plain-fallback-visibility-guard).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 4.7: Plain-fallback visibility guard

As a holder whose device can prove,
I want a request that cannot be answered with ZK to be visibly labelled and refusable rather than silently answered plainly,
So that the plain fallback cannot become a privacy regression.

**Acceptance Criteria:**

- **Given** `EE-ZKP-055a` refuses a plain-mdoc-only request on a ZK-capable device when the
request is *not* authenticated by a registration certificate — an unsigned Digital Credentials API
request (`EE-PRO-004`), an `org-iso-mdoc` request without `readerAuth` chaining to a trusted
access CA (`EE-PRO-025`), or a proximity request without `readerAuth` — and Tier 2 is labelled in
the UI,
- **When** the guard is verified,
- **Then** the fallback remains visible in logs and the interface for each of those three shapes,
- **And** the record states which tier was used and why,
- **And** a test asserts the refusal on a ZK-capable device for each shape,
- **And** a certificate-authenticated request is *not* refused by this guard, because
`EE-ZKP-055a` does not reach it,
- **And** the `EE-ZKP-055a` row in `docs/CONFORMITY.md` is cited as the behaviour's evidence,

**Covers:** FR66. **Source:** risk R7 mitigation; the corpus's disclosure-control requirement sits with the certificate policy because both decide what the wallet may present, and `EE-ZKP-055a`'s own condition is certified by Story 4.3's fixture.

## Analysis and implementation boundaries

Keep authentication, device ZK capability and accepted response formats as separate inputs. Apply EE-ZKP-055a to all three unauthenticated shapes; certificate-authenticated requests leave this specific guard and still face other policy checks.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** None identified beyond the shared contract.

**External acceptance boundary:** None identified for the stated local acceptance scope.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt)
- [app/src/main/kotlin/ee/cyber/wallet/domain/presentation/PresentationTier.kt](../../../app/src/main/kotlin/ee/cyber/wallet/domain/presentation/PresentationTier.kt)
- [app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt](../../../app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt)
- [docs/CONFORMITY.md](../../../docs/CONFORMITY.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/profiles/UNAUTHENTICATED-AGE-REQUESTS.md`

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

Cover three transport shapes times plain-only versus ZK-capable requests and incapable versus capable devices. Assert actual no-send for refused plain response, visible tier/reason, and no blanket exemption from other guards for authenticated requests.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
