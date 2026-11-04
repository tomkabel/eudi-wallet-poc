---
story_id: "10.5"
story_key: 10-5-mobile-device-security-test-matrix
status: backlog
planning_status: drafted
prepared: 2026-10-23
depends_on: []
decision_gate: "G10 obtain primary CIR text; G15 device matrix must reflect current hardening rather than historical audit premises."
external_gate: "Named physical devices and supported platform authentication/security capabilities."
source_contract_sha256: 8c5c50c7b47deeebed2792a437c51262014d20065a3437c56efd2d0cf4b6e79b
---

# Story 10.5: Mobile-device security test matrix

Planning only. Tasks below are unexecuted. Delivery status remains `backlog`;
drafting is not a readiness approval. Read the [shared preparation contract](README.md)
and the [complete analysis](../../planning-artifacts/story-analysis-2026-10-23.md).

## Backlog contract

Source: [epics.md — Story 10.5](../../planning-artifacts/epics.md#story-105-mobile-device-security-test-matrix).
The excerpt preserves authored acceptance criteria, including historical premises;
the analysis below identifies corrections and gates for later implementation.

### Story 10.5: Mobile-device security test matrix

As a security reviewer,
I want a device-level test matrix derived from the mobile-device security requirements in CIR 2024/2979,
So that the integrity and core-functionality claim rests on a matrix rather than on a list of features.

**Acceptance Criteria:**

- **Given** hardware-backed keys, PIN verification and key cleanup on failure are implemented, and
the CIR article text could not be fetched during research,
- **When** the matrix is produced,
- **Then** each requirement row is sourced from the regulation text or marked unverified,
- **And** the matrix is executed on the named device set,
- **And** rows that cannot be verified are reported as unverified rather than assumed.
- **And** a requirement row that is verifiable and fails on the named device set is recorded as a
failure, not as merely unverified, so a device that fails a security requirement is visible,
**Covers:** FR59. **Source:** L5 required change and verification method.

## Analysis and implementation boundaries

Derive requirement rows directly from regulation text, marking inaccessible clauses UNVERIFIED. Cross-reference Epic 12 gates and current key cleanup behavior; distinguish a test plan from an executed matrix.

**Prerequisites for full acceptance:** No story prerequisite for local preparation.

**Decision/evidence gate:** G10 obtain primary CIR text; G15 device matrix must reflect current hardening rather than historical audit premises.

**External acceptance boundary:** Named physical devices and supported platform authentication/security capabilities.

## Existing entry points

- [app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaKeyManager.kt](../../../app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaKeyManager.kt)
- [docs/sota-audit-ROLLUP.md](../../../docs/sota-audit-ROLLUP.md)
- [spec/EE-EUDIW-TS-1.0.md](../../../spec/EE-EUDIW-TS-1.0.md)

These paths exist at drafting time. They identify where to investigate; they
are not a claim that the requested behavior is absent throughout each module.

## Proposed outputs

- `docs/security/MOBILE-DEVICE-SECURITY-MATRIX.md`

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

For every row retain source, device/API/build, command/manual method and observed PASS/FAIL/unverified/not-run. A known device failure is FAIL, not UNVERIFIED; an emulator cannot establish hardware-backed protection.

**Verification state:** not executed by this drafting pass. No new runtime,
interop, benchmark, legal or assessment result is claimed.
