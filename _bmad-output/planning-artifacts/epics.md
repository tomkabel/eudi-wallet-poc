---
status: final
stepsCompleted: [1, 2, 3, 4]
inputDocuments:
  - docs/research/2026-10-23-estonia-alignment-assessment.md
  - docs/research/2026-10-23-innovation-assessment.md
  - docs/research/2026-10-23-shared-appendices.md
  - spec/EE-EUDIW-TS-1.0.md
  - docs/CONFORMITY.md
  - docs/MEASUREMENTS.md
  - PRODUCT.md
    - docs/planning/EE-EUDIW-ZK-IMPORT-PLAN.md
  - _bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md
  - docs/sota-audit-ROLLUP.md
excludedDocuments: []
---

# eudi-wallet-poc - breakdown of the 23 October 2026 recommendations

## Overview

This document decomposes every recommendation, suggestion, enhancement, proposal and idea recorded in
the 23 October 2026 research corpus into epics and stories with testable acceptance criteria. The
three source documents are:

- **Report 1** — `docs/research/2026-10-23-innovation-assessment.md`: six ranked opportunities
  (O1–O6), an initial-release order, and the ways the assessment could be wrong.
- **Report 2** — `docs/research/2026-10-23-estonia-alignment-assessment.md`: five prioritised
  recommendations (R1–R5), a 41-row requirements matrix, five use cases (U1–U5), integration
  pathways, five proposed benchmarks, and the unresolved-question list.
- **Report 3** — `docs/research/2026-10-23-shared-appendices.md`: competitive matrix A1,
  requirement index A2, roadmap A3 (D1–D3, M1–M3, P1–P3, R1–R2), risk register A4 (R1–R15), source
  register A5, and the shared-change/tradeoff analysis A6.

**This is a brownfield repository.** No starter template applies and no scaffolding story exists:
Story 1.1 is a clause-by-clause gap analysis, because the code it assesses already ships. The
system under assessment is `verifier/` (Go + Rust FFI), `app/` (Kotlin/Android), `issuer/` and
`wallet/` (Python), `zk-conformance/` (JVM), `tests/` and `spec/EE-EUDIW-TS-1.0.md`.

**Source labels carried from the corpus.** `[REC]` recommendation; `[UNVERIFIED]` not established.
Every story below cites the source item it implements; nothing in this breakdown is invented. A
premise the corpus did not establish is marked `[UNVERIFIED]` in the story that rests on it.

**Reading a `Source:` line.** The codes are the corpus's own registers and they collide across
documents, so each citation is prefixed by its register: `rec. R3` is a Report 2 recommendation;
`risk R4` is Appendix A4's risk register; `A3 P1` is Appendix A3's roadmap phase; `App. A1` is the
competitive matrix; `matrix S8` is Report 2's requirements-matrix row; `U3` is a use case; `Q4` an
unresolved question; `L2` and `E1` are requirements-matrix rows in the legislation and Estonia
blocks.

**Reviewed 2026-10-23.** Findings and what was done about them:
`_bmad-output/reviews/2026-10-23-epics-review.md`.

**Extended 2026-11-04.** Epics 13 and 14 and Stories 3.9, 4.8, 6.7 and 8.10 were added from an analysis
of the maintainer's public sibling repository [zero-trust-octagon](https://github.com/tomkabel/zero-trust-octagon). They are not derived from the
23 October research corpus: each `Source:` line cites the analysis and the zero-trust-octagon file it
borrows a *method* from. No zero-trust-octagon text or code is copied. Its own audit
(`AUDIT_REPORT_2026-10-04.md`) records about 320 findings and an engineering layer that does not match its
`src/`, so nothing there counts as evidence for a claim in this repository. Every legal, jurisdictional and
sovereignty proposition in the new stories is `[UNVERIFIED]` until checked against a primary source.

**Readiness gate: CONCERNS (proceed).** An architecture record does not exist yet: `bmad-architecture` has not run, which is why
Stories 3.1, 4.1 and 5.1 each open by making their own decision. The PRD landed after this
document was written (see `Reconciliation with the PRD` below), so these stories are
decomposed from the research corpus and the specification, not from it. The recorded intent is `docs/research/`, with `spec/EE-EUDIW-TS-1.0.md`
supplying identifiers, budgets and divergences as evidence rather than as a source of stories (see
`Deliberately not made into stories`). A `prds/` extract set appeared under `planning_artifacts`
while this document was being reviewed; it is another session's work and these stories were not
derived from it. A developer can implement these stories because each one
names the file it touches and the source paragraph it derives from, but two gaps are real rather than
cosmetic: (1) the registration-certificate profile the Epic 4 stories parse is still moving, and its
building blocks (`IntendedUseSource`, fixture generator, fail-closed default) are sequenced first for
that reason; (2) no architecture decision record exists for the `rp-kit/` deployment profile, the
circuit-registry document format or the certificate data model — Stories 4.1, 5.1 and 3.1 each
open with the interface or ADR before the implementation, so the missing decision is
made in the story rather than assumed. Reported as CONCERNS per the readiness gate; the user
pre-authorised proceeding.

## Epics

| # | epic | source | stories | priority |
|---|---|---|---|---|
| 1 | EU AV-Profile conformance kit | O1 (4.75), D1+M1, R1 | 1.1–1.6 | initial release |
| 2 | ZK carrier conformance suite | O5 (3.95), M2, R1 | 2.1–2.4 | initial release |
| 3 | Estonian relying-party acceptance kit | O2 (4.30), D3+P1, R2 | 3.1–3.9 | initial release |
| 4 | Registration-certificate-aware request policy engine | O3 (4.10), P2 | 4.1–4.8 | second wave |
| 5 | Circuit-hash registry and distribution service | O4 (4.05), P3 | 5.1–5.6 | second wave |
| 6 | Measurement programme | D2, M3, §7.1, R4 | 6.1–6.7 | parallel with Epics 1–3 |
| 7 | Documentation honesty | R3, risk R12 | 7.1–7.11 | cheap prerequisite |
| 8 | Standards watch and open-risk reporting | R5, A9–A12, A14–A16, risks R1–R3, R7, R10, R11, R14 | 8.1–8.10 | continuous |
| 9 | Statutory age-threshold toolkit | O6 (3.80), A3 R2 | 9.1–9.6 | gated on Epic 5 |
| 10 | Issuance, trust and revocation completeness | L4, S2, S9, S11 | 10.1–10.5 | pilot prerequisite |
| 11 | Production readiness for a pilot relying party | A3 R1 | 11.1–11.8 | pilot prerequisite |
| 12 | Consent integrity and user-presence gates | PRD §4.7 (FR-45–FR-48), audit findings | 12.1–12.4 | v1 requirement |
| 13 | Compellability dossier | zero-trust analysis 2026-11-04 | 13.1–13.3 | 13.3 early; 13.2 and 13.1 second wave |
| 14 | Test honesty | zero-trust analysis 2026-11-04 | 14.1–14.2 | cheap prerequisite |

## Requirements Inventory

### Functional Requirements

FR1: A machine-readable AV-Profile clause registry mapping each normative clause to a test id.
FR2: AV-profile fixtures minted against `eu.europa.ec.av.1` with the profile's `ZkSystemSpec` tuple (`circuit_hash, version, num_attributes, block_enc_hash, block_enc_sig`).
FR3: A conformance runner producing a per-clause pass/fail report for any candidate wallet, issuer or verifier.
FR4: `av-vci://` invocation support and an AVSUA-equivalent attestation path at issuance.
FR5: The AV conformance report reproducible by a third party from a clean checkout with documented commands.
FR6: A documented carrier conformance runner covering the 13 existing e2e cases, with published vectors.
FR7: A failing carrier case names the binding property that failed, never a generic "invalid".
FR8: The carrier suite exercised against at least one external implementation, result published pass or fail.
FR9: An RP-side acceptance kit deployable as a service profile built from the Go verifier.
FR10: A TARA-facing adapter contract, with an integration test against a TARA test client.
FR11: RP-register lookup as a pluggable, fail-closed interface with a shipped mock.
FR12: A published RP-register interface proposal (design D3).
FR13: Four acceptance cases: foreign wallet with PID, foreign wallet with AV attestation, unattested ZK request, unregistered relying party.
FR14: Refusal that is distinguishable from user cancellation in the UI contract.
FR15: An integrator README plus a measured time-to-first-successful-verification over at least three integrators.
FR16: An `IntendedUseSource` interface, introduced before any certificate implementation.
FR17: Registration/access certificate parsing producing a typed `RegisteredIntendedUse`.
FR18: A two-predicate request allowed only when the relying party's registered intended use names both predicates.
FR19: Presentation-log fields (ARF Topic 19 / DASH_03) populated from the registration certificate.
FR20: Fail-closed behaviour when the certificate is absent, unparseable, or names no use.
FR21: A signed circuit-list document format plus a generator CLI.
FR22: Verifier fetch and pinned-key signature validation of the circuit list, local list as pinned fallback, fail-closed with no network and no list.
FR23: Wallet `ZkPresenter` scheme resolution from the fetched registry.
FR24: A tamper test (one flipped byte observed as refusal) and a two-instance agreement test.
FR25: An audit query returning circuit provenance for a given presentation.
FR26: On-device proving figures: p50/p95/p99 approval-to-proof-ready, cold and warm, plus peak memory, per device.
FR27: Verification capacity figures on the relying party's planned instance type.
FR28: End-to-end measurement of a ~360 KB proof transferred over BLE, and a Tier-2 proximity ZK presentation.
FR29: A regression test pinning the refusal cost after the 54–58 s defect fix.
FR30: A register latency/availability impact profile once the register interface exists.
FR31: The stale `CONFORMITY.md` `issuerAuth` row corrected, and other hand-maintained rows audited against the code.
FR32: Conformity statuses re-derived from tests rather than hand-maintained.
FR33: "Conformant"/"certified" language removed; replaced by "supported by these tests, in this environment".
FR34: A clause-by-clause HAIP 1.0 statement.
FR35: A clause map against EC TS13 (`mso_mdoc_zk` supported, `zk-jwt` missing).
FR36: A clause map against the ECCG Agreed Cryptographic Mechanisms v2.0 for the non-ZKP primitives, plus an algorithm inventory.
FR37: A written narrow-scope statement: a boolean age predicate on one attestation, with the general ARF ZKP vision explicitly out of scope.
FR38: A written attestation-ordering statement: `eu.europa.ec.av.1` preferred for every relying party, `ee.riik.poa.1` as fallback, both paths retained.
FR39: A written `ZKP_08`/ECCG tension report for programme conversations, with a re-check trigger on catalogue revision.
FR40: An ISO/IEC 18013-5 second-edition watch: version-labelled fixtures and a documented rename/adapt plan.
FR41: A `zk-jwt` (SD-JWT VC ZKP) feasibility study and fixture round-trip.
FR42: A per-path unlinkability statement backed by a test asserting no tracking identifier is added (`ZKP_07`).
FR43: A LoA-High authentication test alongside a ZKP presentation (`ZKP_09`).
FR44: The prepared DCHP #17 submission text filed (user-gated) and OpenID4VP #627 tracked.
FR45: Design notes recording `ZKP_03` (attestation-to-PID binding), `ZKP_04` (pseudonyms) and `ACP_03` as out of scope.
FR46: Readiness for a second ZK family (BBS/TS14) without a dialect collision: one format family per scheme.
FR47: An upstream-dormancy maintenance position: vendoring, upgrade cadence and succession.
FR48: A licence and vendored-artefact inventory (Maven mirror, vendored `coseJava`, pinned pre-1.0 dependencies).
FR49: A documented Estonian statutory threshold-to-circuit map with legal citations.
FR50: An accepted-circuit set covering those thresholds.
FR51: A negotiation library implementing ISO §7.2.5 "nearest true above request" with the fork's stricter single-predicate and withhold-false rules.
FR52: Table-driven boundary tests per threshold, including the day before, the day of, a leap-day birth and an incomplete date.
FR53: Refusal with a user-visible reason for an unmapped threshold, and an assertion that no `false` predicate is ever transmitted.
FR54: A legal review of the threshold list and a published unsupported-threshold policy.
FR55: Real issuer integration, tested against the Commission's issuer service.
FR56: Batch issuance for `ee.riik.poa.1` and the AV attestation.
FR57: Real Wallet Unit Attestation issuance (`EE-WUA-007`/`EE-WUA-008`, TS3).
FR58: An end-to-end status-list/revocation demonstration against a real list with a revoked fixture.
FR59: A mobile-device security test matrix read directly from CIR 2024/2979.
FR60: An operations runbook exercised in a game day.
FR61: Monitoring, an availability target and an incident process for a deployed verification service.
FR62: An accessibility conformance audit against EN 301 549 and WCAG 2.1 AA (ARF Topic 54, `ACC_01`/`ACC_02`).
FR63: An external security review of the deployed service with no unresolved high findings.
FR64: Certificate Transparency for access certificates (`CT_01`–`CT_06`) with SCT test vectors.
FR65: Cross-border onboarding without a national identifier: a multi-credential session whose log names both credential ids.
FR66: The plain fallback stays visible and refusable under `EE-ZKP-055a`'s own condition: where a request for the EE-PoA or `eu.europa.ec.av.1` is *not* authenticated by a registration certificate — an unsigned Digital Credentials API request (`EE-PRO-004`), an `org-iso-mdoc` request without `readerAuth` chaining to a trusted access CA (`EE-PRO-025`), or a proximity request without `readerAuth` — and the device is ZKP-capable, the wallet presents over Tier 2 only and refuses a request that admits only a plain mdoc. The absence of authentication is the least-trusted case, never the absence of a ZK requirement.
FR67: A requester name shown on the consent screen is authenticated: the `readerAuth` chain is validated to a trust anchor before any name is rendered as verified, and an unvalidated common name never reaches the screen as a verified one (PRD FR-45).
FR68: Credential keys require user presence: the platform's user verification is bound to the key operation itself, so proof generation, plain presentation and signing each carry the gate or the ungated path is removed (PRD FR-46).
FR69: The consent gate resists guessing: failed attempts are counted in state that survives process restart, with a lockout threshold and an escalation path stated where it is published (PRD FR-47).
FR70: A proximity share carries the same consent as a remote one: the share passes the consent gate before a document is emitted, and the screen states what is shared and with whom on the same terms as the remote path (PRD FR-48).
FR71: The relying-party kit answers a standard reverse-proxy external-authorization call (nginx `auth_request` or Envoy ExtAuthz), so a route is gated on a verified predicate without custom verifier-side code; the adapter fails closed.
FR72: Every input to the request-policy decision is typed GRANT-capable or DENY-only, and no DENY-only input can turn a refusal into an allow.
FR73: The BLE round-trip distribution is measured direct and relayed, with a stated separability result or a stated negative result.
FR74: The fork is assessed against the eight axioms of the Zero-Trust Octagon, with a verdict and a falsification list per axiom.
FR75: A published trust-path verdict names the operator behind every key and attestation chain a verdict depends on, classified GRANT-capable or DENY-only, for a claim fixed in advance.
FR76: The circuit list requires t valid signatures from n pinned signers in different legal domains.
FR77: Published evidence reports carry an EU qualified electronic time stamp that an offline script verifies.
FR78: Each mock-to-real seam has a contract test showing the mock and the real counterpart agree per case.
FR79: Each fail-closed guard is shown necessary by a scripted mutation that makes a test fail.

### Non-Functional Requirements

NFR1: No compliance or certification claim appears in any repository artefact; compliance is conferred by an assessment body, not by a repository.
NFR2: Every published figure states its baseline, method, assumptions and confidence; an unmeasured row says so explicitly (`PENDING-DEVICE`) rather than being estimated silently.
NFR3: Every new lookup or policy path fails closed — register, certificate, and circuit list alike.
NFR4: Fixtures and rules are version-labelled against the external document version they were built from.
NFR5: No regression: `make check` exits 0; `./gradlew :app:testLocal_mocksUnitTest` (the task CI runs) stays at 169 `@Test` across 26 test files with 0 failures; `:zk-conformance:test` green; `go vet ./...` clean.
NFR6: The pinned Python inputs in `tests/requirements-ci.txt` stay pinned, because unpinned `cbor2`/`cryptography` shift the golden transcript vectors.
NFR7: Every effort estimate carries its method; a range is never presented as a commitment.
NFR8: Determinism and reproducibility: a third party reproduces each published result from a clean checkout using only documented commands.
NFR9: The build stays secretless and nothing from `demo-data/` is committed.
NFR10: The sandbox and wallet UI hold WCAG 2.1 AA as the accessibility baseline, with EN 301 549 v3.2.1 as the binding instrument the specification's `EE-ACC-001`–`EE-ACC-004` make a `SHALL` (PRD NFR-12).
NFR11: The fork stays a strict superset of upstream: upstream's Android unit suite passes unchanged (PRD NFR-10).
NFR12: Language coverage is stated per surface: the wallet offers Estonian, Russian and English (`EE-ACC-004`); the sandbox site is Estonian-first with an English toggle. No string exists in one language only on the surface it belongs to (PRD NFR-13).
NFR13: A refusal names its cause and is distinguishable from a retryable error and from user cancellation — in the UI, in the log and in the acceptance cases (PRD NFR-14).
NFR14: The narrow claim travels with the product wherever it is described: one boolean predicate, one attestation, two carriers, and the limits (`ZKP_08`, 109-bit circuits, no device measurements) (PRD NFR-15).
NFR15: Published findings are reported, not implied fixed: the repository's own audit (`docs/sota-audit-ROLLUP.md`, 124 findings, none critical) is referenced by severity count, refreshed each release, and the state of each high finding is an explicit row (PRD NFR-16).
NFR16: No silent scope widening: a new predicate, ZK system, carrier or attestation is a scope decision recorded in the PRD, not an implementation detail (PRD NFR-17).

### Additional Requirements

- **Brownfield, no starter template.** Story 1.1 is a gap analysis against an existing tree. No
scaffolding story is created.
- `verifier/go/oid4vp/carrier.go` (`Carrier` interface, sniff dispatch) is the single adaptation point for ISO second-edition drift; `TranscriptForFlow`/`Flow` is the single transcript entry point and flow mixing is structurally banned. New carrier work must not bypass either.
- The circuit allowlist keys on `id` + `circuit_hash` + params; the allowlist accepts both `longfellow-libzk-v1` and `org.iso.mdoc.zk` labels. FR21–FR25 must preserve that keying.
- `direct_post.jwt` is the verifier default; plain `direct_post` requires an explicit flag. No story may invert that default.
- The carrier profile of record is `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`; `docs/profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md` is prepared and user-gated.
- Plan records land in `docs/planning/` and every document is indexed in `docs/README.md`; new documents introduced by these stories must be indexed there.
- Tests and what each proves are catalogued in `tests/README.md`; new suites (AV clause runner, carrier runner, threshold boundary tests) must be added to that catalogue.
- `docs/CONFORMITY.md`, `docs/MEASUREMENTS.md` and the `STEP*-RECORD.md` files are hand-maintained prose, so a file being touched at HEAD does not make a row current. Epic 7 stories must verify a row against the code before editing it.
- A story that produces a document names the output path in its own acceptance criteria and adds the document to `docs/README.md`'s index, so a clause map or a statement cannot be closed with its artefact filed anywhere. Stories that produce code name the file they touch.

### UX Design Requirements

No `bmad-ux` spine exists for this repository, so the UX section is empty. The two user-facing
requirements the source documents do raise are carried as implementation stories rather than UX
specification work: FR19 (Topic 19 / DASH_03 presentation-log fields, Epic 4) and FR62
(accessibility conformance, Epic 11).

### Coverage Map

| FR | story |
|---|---|
| FR1 | 1.1 |
| FR2 | 1.2 |
| FR3 | 1.3 |
| FR4 | 1.4, 1.5 |
| FR5 | 1.6 |
| FR6 | 2.1 |
| FR7 | 2.2 |
| FR8 | 2.4 |
| FR9 | 3.1 |
| FR10 | 3.2, 3.8 |
| FR11 | 3.3 |
| FR12 | 3.4 |
| FR13 | 3.5 |
| FR14 | 3.6 |
| FR15 | 3.7 |
| FR16 | 4.1 |
| FR17 | 4.2, 4.6 |
| FR18 | 4.3 |
| FR19 | 4.4 |
| FR20 | 4.5, 4.6 |
| FR21 | 5.1, 5.6 |
| FR22 | 5.2 |
| FR23 | 5.3 |
| FR24 | 5.4 |
| FR25 | 5.5 |
| FR26 | 6.1, 6.6 |
| FR27 | 6.2, 6.6 |
| FR28 | 6.3 |
| FR29 | 6.4 |
| FR30 | 6.5 |
| FR31 | 7.1, 7.2 |
| FR32 | 7.3 |
| FR33 | 7.4, 7.10, 7.11 |
| FR34 | 7.5 |
| FR35 | 7.6 |
| FR36 | 7.7 |
| FR37 | 7.8 |
| FR38 | 7.9 |
| FR39 | 8.1 |
| FR40 | 8.2, 2.3 |
| FR41 | 8.3 |
| FR42 | 8.4 |
| FR43 | 8.5 |
| FR44 | 8.6 |
| FR45 | 8.7 |
| FR46 | 8.8 |
| FR47 | 8.9 |
| FR48 | 11.8 |
| FR49 | 9.1 |
| FR50 | 9.2 |
| FR51 | 9.3 |
| FR52 | 9.4 |
| FR53 | 9.5 |
| FR54 | 9.6 |
| FR55 | 10.1 |
| FR56 | 10.2 |
| FR57 | 10.3 |
| FR58 | 10.4 |
| FR59 | 10.5 |
| FR60 | 11.1, 11.5 |
| FR61 | 11.2 |
| FR62 | 11.3 |
| FR63 | 11.4 |
| FR64 | 11.6 |
| FR65 | 11.7 |
| FR66 | 4.7 |
| FR67 | 12.1 |
| FR68 | 12.2 |
| FR69 | 12.3 |
| FR70 | 12.4 |
| FR71 | 3.9 |
| FR72 | 4.8 |
| FR73 | 6.7 |
| FR74 | 8.10 |
| FR75 | 13.1 |
| FR76 | 13.2 |
| FR77 | 13.3 |
| FR78 | 14.1 |
| FR79 | 14.2 |

_Rows deliberately left without a story, and the specification requirements this backlog does not
carry, are listed in `Deliberately not made into stories` under Sequencing._

**NFR binding.** The Coverage Map joins functional requirements to stories. Non-functional
requirements bind differently — they are properties every story of a class must hold, so they are
bound here by class rather than by story.

| NFR | Bound to | How it is checked |
|---|---|---|
| NFR1 no compliance claim | every story | Story 7.4's vocabulary pass; review of any document a story publishes |
| NFR2 figures state method | 6.1, 6.2, 6.3, 6.6, 9.6 | Story 6.6's reporting review |
| NFR3 fail closed | 3.3, 4.1, 4.5, 4.7, 5.2, 5.3 | each story's refusal case |
| NFR4 version labels | 1.2, 2.3, 4.2, 5.1, 8.2 | Story 2.3's labelling assertion |
| NFR5 no regression | every code story | Definition of Done below |
| NFR6 pinned Python inputs | every story that runs the Python helpers | `tests/requirements-ci.txt` review; transcript tests re-run on change |
| NFR7 estimates carry method | 7.11, 9.6, 6.6 | Story 7.11's rule |
| NFR8 third-party reproduction | 1.6, 2.1, 2.4, 3.7, 6.1 | each story's published report and commands |
| NFR9 secretless build | every story | `make check`; review of any committed file |
| NFR10 WCAG 2.1 AA baseline | 11.3, 11.7 | Story 11.3's audit report |
| NFR11 strict superset of upstream | every story that changes `app/` | upstream's suite passes unchanged in CI |
| NFR12 language coverage per surface | 11.7, 7.4 | review of each surface's strings |
| NFR13 honest failure surfaces | 3.6, 12.1, 12.3, 12.4 | each story's three-outcome criterion |
| NFR14 narrow claim travels | 7.4, 7.8, 10.2 | Story 7.4's vocabulary pass |
| NFR15 findings reported, not implied fixed | 7.2, 7.3 | the audit rollup's severity count per release |
| NFR16 no silent scope widening | every story | a scope change is recorded against the PRD's guardrails (§9), not inside a story |

**Definition of Done for every code story:** `make check` exits 0; `./gradlew
:app:testLocal_mocksUnitTest` stays at 169 `@Test` across 25 files with 0 failures;
`:zk-conformance:test` is green; `go vet ./...` is clean. A story that cannot satisfy one of these
records why in its own acceptance criteria.

**Reconciliation with the PRD (2026-10-23).** `prd.md` reached `status: final` at 05:05, after
this
document was written, and it names this file as its decomposition. Both are kept, and the
relationship
is stated rather than left to inference.

**Identifiers.** The PRD numbers its own requirements `FR-n`
across 48 of them and its own `NFR-n` across 17, deliberately distinct from this document's
`FR1`–`FR70`
and `NFR1`–`NFR16` (PRD assumption A-6 records the choice and leaves renumbering open). Neither
side is
renumbered here, because both are cited from outside: this document's ids are what the sprint
tracker and
the coverage map join on, and the PRD's are what its own sections and audit rows reference. The
feature
sections correspond as §4.1↔Epics 4, 6, 10; §4.2↔Epic 1; §4.3↔Epic 2; §4.4↔Epic 3; §4.5↔Epic 11's
sandbox and accessibility stories; §4.6↔Epics 7 and 8; §4.7↔Epic 12.

**What the backlog gained from
reconciliation.** The PRD's four v1 gates in §4.7 exist to close the product's own audit findings
and had
no story here: they are FR67–FR70 and Epic 12. Its NFR-10 and NFR-13–NFR-17 were unbound; they are
NFR11–NFR16 and the binding table. Its §18 adds six open questions this document did not carry —
OQ-6
(the AV Profile mandates ZK over the DC API while this fork profiles the OpenID4VP carrier as
primary, so
every artefact must say which path is mandated and which is profiled-but-unofficial), OQ-11
(whether the
proximity path keeps advertising itself where an age request is refused), OQ-12 (whether anyone
hosts
the sandbox publicly), OQ-16 (whether the two-predicate refusal costs a lawful use case), OQ-9
(nobody
owns the risk register) and OQ-14 (whether an EU-level role is wanted at all). They are recorded
here
rather than duplicated: the sprint's open-questions section points at the PRD's §18 as the fuller
list.


**One PRD figure this repository contradicts.** PRD NFR-5 states `169

Test` over 26 test files; the tree
holds 169 across **25** files that contain `

Test` (26 of the 27 `.kt` files under `app/src/test` are
named `*Test.kt`). This document's NFR5 and its Definition of Done carry the verified count, and
the
discrepancy is recorded here so it is corrected in the PRD rather than propagating.

## Epic 1: EU AV-Profile conformance kit

Convert the AV-Profile work already in the tree into an artefact an EU programme can test against. The
cryptography, the doctype `eu.europa.ec.av.1`, the mandated system `longfellow-libzk-v1` and the
carrier all exist; what is missing is a clause registry, AV-specific fixtures with the profile's
parameter tuple, a runner, and a published report a third party can reproduce. The in-repository
kit can be prepared without an external participant or device. Completed acceptance requires
an independent wallet or verifier run for Story 1.3, publication and an independently attributed
clean-checkout reproduction for Story 1.6; local or synthetic runs do not satisfy those gates.
Source: Report 1 O1; Report 2 R1; Appendix A3 phases D1 and M1.

### Story 1.1: AV Profile clause registry

As a national wallet programme evaluating age-verification stacks,
I want a machine-readable statement of each AV-Profile normative clause with the test id that covers it,
So that I can see which clauses are evidenced and which are explicitly out of scope for v1.

**Acceptance Criteria:**

- **Given** the AV Profile (Annex A) is pinned at a stated document version,
- **When** the clause registry is produced,
- **Then** every normative clause in the pinned version has either a test id or an explicit "out
of scope for v1" note,
- **And** the registry records the pinned version so clause drift is a visible maintenance cost.

**Covers:** FR1. **Source:** O1 proposed capability (a); D1 deliverable and exit criteria.

### Story 1.2: AV-profile fixtures

As an implementer interoperating on the EU age-verification blueprint,
I want fixtures minted against `eu.europa.ec.av.1` carrying the profile's `ZkSystemSpec` parameter tuple,
So that a verifier's required-parameter validation is exercised by the profile's own parameter names rather than a fork convention.

**Acceptance Criteria:**

- **Given** the AV Profile's `ZkSystemSpec` tuple (`circuit_hash, version, num_attributes,
block_enc_hash, block_enc_sig`),
- **When** the AV fixtures are generated,
- **Then** the fixtures include the container-format examples from the profile's Annex A,
- **And** each fixture is version-labelled with the document version it was built from,
- **And** the fixture builder is pinned by a drift test against the committed bytes (no
re-serialisation of a decoded CBOR fixture),
- **And** every registry test id resolves to a clause in the pinned version, so a clause removed
in a later revision leaves an orphaned test id that fails the build rather than reading as
evidence,

**Covers:** FR2. **Source:** O1 proposed capability (b); existing fixture-rebuild rules.

### Story 1.3: AV conformance runner

As a programme owner choosing a wallet or verifier,
I want a runner that executes the clause registry against a candidate implementation and prints a per-clause pass/fail report,
So that conformance is checked rather than asserted.

**Acceptance Criteria:**

- **Given** the clause registry and a candidate implementation under test,
- **When** the runner is executed,
- **Then** at least 20 clauses are executed, of which at least 8 are negative controls that fail
closed,
- **And** each result names its clause and test id,
- **And** the runner reports the result for this repository's verifier and for at least one
external wallet or verifier,
- **And** a clause that errors, and a candidate that will not start, each produce their own result
state (`ERROR`/`NOT_EXECUTED`) and never `PASS`, so an unrun clause cannot read as evidence,
- **And** the runner and the clause registry are added to `tests/README.md` with what each proves,
and the runner is wired into `make check` or the matching CI job (or the story records why it is
excluded and how it is run instead),

**Blocked by (external):** at least one external wallet or verifier as the candidate under test.
Where the external party or hardware is unavailable, the story still closes its in-repository half
and records the state as blocked with the reason and date — never as passed, and never with a
fabricated result.
**Covers:** FR3. **Source:** O1 acceptance criteria (i)–(ii); M1 deliverable and exit criteria.

### Story 1.4: av-vci:// invocation

As a wallet implementer following the AV blueprint,
I want the `av-vci://` invocation path handled,
So that an AV attestation can be obtained through the profile's own issuance trigger.

**Acceptance Criteria:**

- **Given** an `av-vci://` invocation,
- **When** the wallet receives it,
- **Then** the issuance flow for `eu.europa.ec.av.1` is entered,
- **And** an unrecognised invocation is refused loudly rather than silently ignored,
- **And** the behaviour is covered by a test named in `tests/README.md`.
- **And** an `av-vci://` invocation whose parameters are absent or unparseable is refused with the
offending parameter named, never entered and never ignored,
**Covers:** FR4. **Source:** S8 required change; O1 interfaces.

### Story 1.5: AVSUA-equivalent issuance path

As an attestation provider issuing proof-of-age,
I want the wallet-unit-attestation equivalent the AV Profile expects at issuance to be produced,
So that the AV issuance path is shaped the way the profile requires rather than the way the mock happens to work.

**Acceptance Criteria:**

- **Given** the AV Profile's AVSUA expectation and spec requirement `EE-WUA-008`,
- **When** an AV attestation is issued,
- **Then** the AVSUA-equivalent attestation accompanies it,
- **And** the path is exercised by a test that fails when the attestation is withheld.

**Covers:** FR4. **Source:** S8; spec `EE-WUA-008`; Report 2 §3.3 S8 required change.

### Story 1.6: Published AV report and external reproduction

As a third party assessing a conformance claim,
I want the AV-Profile conformance report published together with the commands that regenerate it,
So that I can reproduce it from a clean checkout in one sitting, including the failures.

**Acceptance Criteria:**

- **Given** a clean checkout and only the documented commands,
- **When** a third party runs the kit,
- **Then** the report is reproduced in one sitting,
- **And** the published result includes failures rather than only passes,
- **And** the report version is tied to the pinned AV Profile document version.
- **And** the kit records the host profile it ran on — CPU baseline, `RUSTFLAGS`, JDK version and
the sibling `../longfellow-zk` revision — so a third party's SIGILL or missing sibling reads as an
environment mismatch rather than a conformance failure,
**Covers:** FR5. **Source:** O1 acceptance criteria (iii); O1 validation.

## Epic 2: ZK carrier conformance suite

Publish the negative-control tests that already exist, as a runner with a named-binding-property
failure model, then exercise at least one external implementation. The source documents order this
second in the initial release, yet recommend publishing its result *first* among external claims,
because it is expected to pass and because an honest published failure is worth more than another
assertion of success.
Source: Report 1 O5; Appendix A3 M2; Report 2 R1; Appendix A6 tradeoff 4.

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

### Story 2.2: Binding-property failure naming

As an integrator debugging a rejected presentation,
I want a failing case to name the binding property that failed,
So that I can fix the right thing instead of guessing at a generic "invalid".

**Acceptance Criteria:**

- **Given** a response that violates `client_id`, `nonce`, `response_uri`, key thumbprint, single
use, or the predicate value,
- **When** the runner evaluates it,
- **Then** the failure names that property,
- **And** no failure path emits a generic "invalid" in place of the property name.
- **And** a well-formed response carrying no provable presentation is reported as its own outcome
(`no-presentation`, refused), not as a binding-property failure,
**Covers:** FR7. **Source:** O5 acceptance criteria (iii).

### Story 2.3: Version-labelled carrier fixtures

As a maintainer watching ISO/IEC 18013-5 second edition,
I want every carrier fixture and profile rule labelled with the draft or document version it was built from,
So that a published edition that diverges is a relabelling job rather than an untraceable breakage.

**Acceptance Criteria:**

- **Given** the carrier fixtures and the carrier profile v1.0,
- **When** the labelling is applied,
- **Then** each fixture and each profile rule states the external document version it derives
from,
- **And** the labels are asserted by a test, and the assertion checks each label equals the
*current* carrier-profile version, so a superseded label fails rather than passing as labelled,
- **And** the assertion covers the rules in `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md` as
well as the fixtures, so a new profile rule cannot be added unlabelled.

**Covers:** FR40. **Source:** M2 exit criteria; risk R1 mitigation; O5 risks.

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

## Epic 3: Estonian relying-party acceptance kit

Estonian public-sector services must accept other Member States' certified wallets from the end of 2026
while the Estonian wallet launches in 2028 and the Relying Party Register has no published API. This epic
builds the artefact those services need in that window: a deployable RP-side service profile, a
TARA-facing contract, a fail-closed register interface with a mock, a published interface proposal, and
four acceptance cases. It is the only recommendation a buyer can act on before 2028.
Source: Report 1 O2; Report 2 R2, §5.1 P1, §6 integration pathways; Appendix A3 D3 and P1; Appendix A6 tradeoffs 3 and 5.

### Story 3.1: Deployable RP service profile

As an Estonian e-service operator,
I want a deployable relying-party service profile built from the Go verifier,
So that I have something to run acceptance tests against instead of a library to assemble.

**Acceptance Criteria:**

- **Given** the existing `verifier/go` service,
- **When** the `rp-kit/` deployment profile is produced,
- **Then** it starts from the documented commands with no undocumented step,
- **And** the deployment profile's decisions are recorded as an ADR before implementation,
- **And** `direct_post.jwt` remains the default with plain `direct_post` behind the explicit flag.
- **And** a session whose `response_uri` callback never arrives expires after the stated session
TTL and answers with the session id named, so the relying party is never left without a status,
**Covers:** FR9. **Source:** O2 proposed capability; O2 affected components; A3 P1 deliverable.

### Story 3.2: TARA-facing adapter contract

As an integrator wiring a state e-service,
I want a TARA-facing adapter contract with an integration test against a TARA test client,
So that the authentication leg is defined rather than assumed.

**Acceptance Criteria:**

- **Given** TARA's OIDC/OAuth2 surface and the requirement that joining needs an agreement with
RIA,
- **When** the adapter contract is implemented,
- **Then** an integration test runs against a TARA test client,
- **And** the contract records where RIA agreement is a prerequisite rather than hiding it,
- **And** the wallet-facing path is documented as a parallel path, not as something inside TARA.
- **And** TARA unreachable, or the RIA agreement absent at run time, fails the adapter closed and
records the integration test as blocked rather than passed,
**Covers:** FR10. **Source:** E1; §6 integration pathways (TARA row) `[REC]`.

### Story 3.3: Fail-closed RP-register interface and mock

As a relying party that must request only what is registered,
I want the register lookup to be a pluggable interface that refuses when it has no entry,
So that a service that does not exist yet cannot silently become an allow.

**Acceptance Criteria:**

- **Given** the register interface with no implementation configured,
- **When** a presentation is attempted,
- **Then** the request is refused with the missing prerequisite named in the log,
- **And** a shipped mock satisfies the interface for tests,
- **And** the contract includes a bounded cache age and an explicit, logged decision about stale
data — never a silent allow.
- **And** a register that is configured but unreachable, timing out or answering 5xx refuses the
presentation and logs the register error — that state is distinct from no implementation
configured, and a cache hit is never required for the refusal to hold,
**Covers:** FR11. **Source:** E2; O2 proposed capability (b); risk R9 mitigation; A6 tradeoff 3.

### Story 3.4: RP-register interface proposal

As the Estonian programme's competent authority,
I want a written register interface proposal with mocks and a compliance note against the ARF RP topics,
So that the interface is on the table rather than reconstructed after the procurement is awarded.

**Acceptance Criteria:**

- **Given** the mock-backed interface and the ARF relying-party registration topics,
- **When** the proposal is written and published,
- **Then** it states the proposed lookup contract, the refusal semantics and a fail-closed
default,
- **And** it is published even though RIA has no published API and the access-certificate approach
is still in design,
- **And** the proposal records that it is useful even if superseded.

**Covers:** FR12. **Source:** D3 deliverable and exit criteria; O2 risks and mitigation; A6 tradeoff 5.

### Story 3.5: Four Estonian acceptance cases

As an Estonian service owner,
I want the four requests a real service will meet executed with machine-readable results,
So that I can demonstrate acceptance before the national wallet exists.

**Acceptance Criteria:**

- **Given** the RP kit, a foreign wallet with a PID, a foreign wallet with an AV attestation, an
unattested ZK request, and a request from an unregistered relying party,
- **When** the four cases run,
- **Then** each produces a documented, machine-readable result,
- **And** the missing-registration and unattested-request cases refuse,
- **And** a fifth case is recorded for the state two faithful implementations of FR22 and FR23
would otherwise deadlock on: the verifier resolves circuits from its pinned local fallback while
the wallet has no fetched registry, so the wallet's resolution falls back to the same pinned
document version rather than refusing the ZK path while the verifier waits,
- **And** the least-trusted unattested case follows the policy that `EE-ZKP-055a` already governs.
- **And** the two remaining unauthenticated shapes `EE-ZKP-055a` governs are covered as well as
the least-trusted one: an `org-iso-mdoc` request without `readerAuth` chaining to a trusted access
CA, and a proximity request without `readerAuth`, each refusing a plain-mdoc-only request on a
ZK-capable device,
**Covers:** FR13. **Source:** O2 acceptance criteria (i); U5; L2 verification method.

### Story 3.6: Refusal distinguishable from cancellation

As a holder whose request was refused,
I want the refusal to be distinguishable from my own cancellation,
So that I can tell a policy decision apart from my own action.

**Acceptance Criteria:**

- **Given** a refusing policy path and a user cancellation,
- **When** both are presented in the UI contract,
- **Then** the two outcomes are distinguishable in the interface contract,
- **And** a test asserts the distinction on the fixed dispatch path.
- **And** a third interface state exists for a technical failure, a timeout or an aborted session,
so an infrastructure failure is never shown to the holder as a policy refusal or as their own
cancellation,
**Covers:** FR14. **Source:** O2 acceptance criteria (ii); U5 acceptance criteria; S4 current capability note.

### Story 3.7: Integrator README and timed onboarding

As an integrator with no prior ZK knowledge,
I want a README that takes me to four reproduced results in under a day,
So that the kit's value is measurable rather than claimed.

**Acceptance Criteria:**

- **Given** a clean checkout and the README,
- **When** an integrator with no prior ZK knowledge follows it,
- **Then** the four acceptance results are reproduced within a working day,
- **And** time-to-first-successful-verification is measured with a stopwatch and reported as a
distribution over at least three integrators,
- **And** the measurement's method and assumptions accompany the number.
- **And** every participant is recorded, including those who never reach a first successful
verification, and the reported figure carries both the count and the failure count,
**Blocked by (external):** three human integrators with no prior ZK knowledge. Where the external
party or hardware is unavailable, the story still closes its in-repository half and records the
state as blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR15. **Source:** O2 acceptance criteria (iii); §7.1 benchmark 4; NFR7.

### Story 3.8: Parallel-path integration note

As an architect mapping the wallet onto Estonian infrastructure,
I want a written note stating that the wallet-facing integration is a parallel path and that X-Road has no documented relationship to the wallet protocols,
So that a false architectural premise does not enter a planning document.

**Acceptance Criteria:**

- **Given** the verified 0-hit result for "OpenID" across RIA's X-tee page, `x-road.global` and
the X-Road repository landing page,
- **When** the integration note is written,
- **Then** it records that X-Road has no documented relationship to the EUDI Wallet, OpenID4VP or
OpenID4VCI,
- **And** it names transporting `vp_token`s over X-Road and looking registrations up over X-Road
as proposed interfaces lacking verified support,
- **And** it names e-Residency/Digi-ID as a constraint and Smart-ID/Mobile-ID as a second
authentication route, not as EUDI presentation routes.

**Covers:** FR10. **Source:** §6 integration pathways (X-Road, e-Residency, Smart-ID rows); §5.1 P4.

### Story 3.9: Gateway adapter for the rp-kit

As a site operator who fronts a service with nginx or Envoy,
I want the rp-kit verifier to answer the proxy's external-authorization call,
So that a route can be gated on a verified predicate without custom code, and Story 3.7's timed onboarding includes this path.

**Acceptance Criteria:**

- **Given** a route configured with nginx `auth_request` (or Envoy ExtAuthz) pointing at the adapter,
- **When** a request arrives without a verified-predicate session,
- **Then** the proxy receives a refusal that leads to the presentation flow, and a request carrying a valid session passes,
- **And** the allow or deny is the verifier's existing result, not a second implementation of it,
- **And** the adapter fails closed: verifier unreachable, session store unavailable and expired session each deny, one test per case,
- **And** the adapter log names the circuit id, circuit hash and list version of the accepting presentation and no identifier of the holder,
- **And** refusal, cancellation and technical failure reach the proxy as distinct outcomes,
- **And** the maintainer confirmed on 2026-11-04 that the adapter is an integration surface and not a new predicate, ZK system, carrier or attestation, so it is not the scope widening NFR16 rules out and Story 7.8's scope statement is unchanged,
- **And** the adapter's documentation and the integrator README carry the narrow claim of NFR14 (one boolean predicate, one attestation, two carriers, and the stated limits),
- **And** the integrator README (Story 3.7) shows this path and its timed result is reported with its method (Story 6.6).

**Covers:** FR71. **Source:** zero-trust analysis 2026-11-04 (the Go ExtAuthz gateway pattern in zero-trust-octagon `docs/engineering/`, as pattern only); Stories 3.1 and 3.7. **Depends on:** 3.1, 3.7. **Decision:** kept, with the NFR14 criterion above (maintainer, 2026-11-04).

## Epic 4: Registration-certificate-aware request policy engine

The wallet currently refuses any request that would disclose more than one age predicate, because it cannot
read the relying party's registration. That blanket refusal is stricter than ISO/IEC 18013-5 §7.2.5 and
cannot distinguish a legitimate two-predicate request from an over-reaching one. This epic replaces it
with a lawful decision driven by the certificate, and populates the ARF Topic 19 log fields from the same
source. It is introduced interface-first so a fail-closed default exists before any parsing does.
Source: Report 1 O3; Appendix A3 P2; risk R8.

### Story 4.1: IntendedUseSource interface

As the maintainer of the age-predicate policy,
I want the intended-use decision behind an interface before any certificate parsing exists,
So that the Estonian register or an EU registry can be plugged in without redesign.

**Acceptance Criteria:**

- **Given** the existing `AgePredicatePolicy` behaviour,
- **When** `IntendedUseSource` is introduced,
- **Then** the policy consumes the interface and its absence yields refusal,
- **And** the interface's data model is recorded as an ADR before the certificate implementation
starts,
- **And** no existing unit test regresses.
- **And** a source that is present but throws or times out refuses under the same fail-closed
default with `source-error` logged, distinct from the interface being absent,
**Covers:** FR16. **Source:** O3 implementation approach ("interface first"); A3 P2 deliverable.

### Story 4.2: Registration-certificate parsing

As a wallet that must act on the relying party's registered intended use,
I want the registration/access certificate chain parsed into a typed `RegisteredIntendedUse`,
So that the wallet can distinguish a lawful request from an over-reaching one.

**Acceptance Criteria:**

- **Given** a registration/access certificate chain,
- **When** it is parsed,
- **Then** a typed `RegisteredIntendedUse` is produced, carrying the relying party's trade name,
unique identifier, service identifier, Member State, intended use and privacy-policy URL,
- **And** an unparseable certificate yields refusal rather than a partial value,
- **And** the certificate profile version targeted is recorded, because the ARF annexes are still
moving.
- **And** a certificate whose profile version is newer than the version the parser targets is
refused with `version-unsupported` rather than partially parsed as the older profile,
**Covers:** FR17. **Source:** O3 proposed capability (a)–(b); A3; S10; O3 risks.

### Story 4.3: Certificate-driven two-predicate decision

As a relying party with a legitimate two-predicate use case,
I want a request naming both predicates in my registered intended use to be allowed, and everything else refused,
So that a precise legal rule replaces a blanket refusal.

**Acceptance Criteria:**

- **Given** a two-predicate request bearing a certificate whose intended use names both
predicates,
- **When** the request is presented,
- **Then** it is allowed and the user sees an explanation of the revealed range,
- **And** the same request without a certificate, or with one not naming both, is refused,
- **And** the loosening path is covered by explicit negative tests.
- **And** a certificate naming both predicates does not override the held-false rule: where the
wallet holds one predicate false it is withheld and the decision is recorded, so the loosened path
cannot transmit a false predicate,
- **And** a request naming three or more predicates is refused explicitly — the rule keys on the
registered intended use naming *each* requested predicate, and the upper bound is stated rather
than implied,
**Covers:** FR18. **Source:** O3 acceptance criteria (i)–(ii); E2; risk R8.

### Story 4.4: Topic 19 log fields from the certificate

As an auditor reading a presentation log,
I want the ARF Topic 19 relying-party fields populated from the certificate and no attribute values recorded,
So that the log satisfies the documented logging duty without becoming a disclosure surface.

**Acceptance Criteria:**

- **Given** a certificate-backed presentation,
- **When** the log entry is written,
- **Then** the DASH_03 fields are populated from the certificate,
- **And** a test asserts each field individually,
- **And** no attribute value appears anywhere in the log (`DASH_03a`).
- **And** a named field that the certificate does not carry is logged with an explicit absent
marker, never left blank, and each field is asserted individually for the absent case,
**Covers:** FR19. **Source:** A3 required change and verification method; O3 acceptance criteria (iii).

### Story 4.5: Fail-closed defaults and certificate property tests

As a privacy engineer,
I want refusal to be the default whenever the certificate is absent, unparseable or names no use, proven by property tests,
So that a parsing error cannot quietly loosen a privacy control.

**Acceptance Criteria:**

- **Given** generated certificates including malformed, empty-intended-use and absent cases,
- **When** the property tests run,
- **Then** every such case refuses,
- **And** the default-to-refusal behaviour is asserted independently of the happy path,
- **And** the existing 169 Kotlin unit tests show no regression.
- **And** the property-test set includes the expired, revoked and untrusted-anchor certificates
the generator mints, and each is asserted to refuse,
**Covers:** FR20. **Source:** O3 acceptance criteria (iv); risk R8 mitigation; O3 validation.
### Story 4.6: Certificate fixture generator

As the maintainer of the certificate policy,
I want a generator that mints the registration-certificate fixtures every Epic 4 test depends on,
So that Story 4.5's property tests have inputs and the fail-closed suite exists before the parsing
it guards.
**Acceptance Criteria:**
- **Given** the corpus names a fixture generator second in O3's implementation approach and no
story delivered one,
- **When** the generator is built,
- **Then** it mints at least: a valid certificate naming one predicate; one naming two; one naming
no intended use; a malformed one; one with an untrusted anchor; a tampered signature; an expired
one; a not-yet-valid one; one revoked; and one whose profile version exceeds the version the
parser targets,
- **And** the generator is pinned by a drift test against the committed fixture bytes,
- **And** Story 4.5's property tests consume it, and the fail-closed default (Story 4.1) is
asserted before Story 4.2's parsing exists, so the guards land before the code they guard,
- **And** the fixtures are added to `tests/README.md` with what each one proves,
**Covers:** FR17, FR20. **Source:** O3 implementation approach ("the certificate implementation
and a fixture generator second"); risk R8 mitigation; review finding 8.

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

### Story 4.8: Evidence-class typing of policy inputs

As a wallet policy maintainer,
I want every input to the request-policy decision typed GRANT-capable or DENY-only,
So that a declared or unattested input can add friction or refuse, and can never be the reason a request is allowed.

**Acceptance Criteria:**

- **Given** the inputs the Story 4.3 decision reads (registration-certificate fields, plain request fields such as a declared purpose, the relying-party name as presented, the register lookup result, cache state),
- **When** each is classified,
- **Then** a table beside the decision code records its class and reason, GRANT-capable only for an input that is cryptographically verified and revocable by a body the wallet trusts, and DENY-only for anything declared or merely cached, and ADR-005 gains an addendum citing it,
- **And** a property test over generated input combinations shows monotonicity: changing a DENY-only input never turns a refusal into an allow,
- **And** reclassifying one input as GRANT-capable in a scripted mutation makes that test fail,
- **And** the Story 4.7 plain fallback is covered by the same property: the fallback never grants,
- **And** an input with no class fails the build or the suite instead of defaulting to GRANT.

**Covers:** FR72. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon Appendix F grant/deny table and Axiom 7, as method only; its legal claims are not imported); Stories 4.3, 4.5, 4.7. **Depends on:** 4.1, 4.3, 4.5.

## Epic 5: Circuit-hash registry and distribution service

The AV Profile states that the accepted circuit set is published and maintained by the scheme owner
separately from the document; TS13 requires the circuit to be identified by `circuit_hash`. The repository
solves this locally with a compiled allowlist. When every operator does that independently, two parties end up accepting different circuits while
both believe they follow the same profile. This epic ships a
signed document plus fetch/validate/audit, keeping the local list as a pinned fallback.
Source: Report 1 O4; Appendix A3 P3; risk R4.

### Story 5.1: Signed circuit-list document and generator

As a scheme owner or community maintainer,
I want a signed JSON circuit-list document format plus a CLI that issues it,
So that "which circuits do we accept" is one checkable document instead of a per-operator convention.

**Acceptance Criteria:**

- **Given** the fields the standards require (system, version, attributes, block-encryption
parameters, hashes, minimum library version),
- **When** the generator emits a document,
- **Then** the document is signed and the signature is verifiable against a pinned key,
- **And** the document format is recorded as an ADR before the verifier work starts,
- **And** the generator stays in-repo so maintaining a list is cheap.

**Covers:** FR21. **Source:** O4 proposed capability; O4 implementation approach.

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

### Story 5.5: Circuit provenance audit query

As an auditor answering "which circuit proved this?" months later,
I want an audit query returning the circuit provenance for a given presentation,
So that provenance survives the transaction.

**Acceptance Criteria:**

- **Given** a completed presentation,
- **When** the audit query is issued,
- **Then** it returns the circuit provenance for that presentation,
- **And** the response includes the registry document version the circuit was accepted under.
- **And** an unknown, refused or aged-out presentation id returns a distinguished not-found result
— never an empty or inferred provenance — and the retention boundary itself is tested,
**Covers:** FR25. **Source:** O4 acceptance criteria (iv).

### Story 5.6: Registry hosting and succession path

As a prospective adopter,
I want the registry shipped as a generator plus a hosted instance with a documented succession path,
So that adoption does not depend on who happens to host it today.

**Acceptance Criteria:**

- **Given** that the AV Profile names no maintainer for the accepted set,
- **When** the registry is published,
- **Then** it ships as a generator plus a hosted instance,
- **And** a documented succession path names what happens if no scheme owner adopts it,
- **And** the local pinned list is documented as the fallback for an ownerless period.

**Covers:** FR21. **Source:** O4 dependencies; risk R4 mitigation.

## Epic 6: Measurement programme

Every performance and usability figure in the source material that a programme would ask about is either
host-side only or recorded `PENDING-DEVICE`. This epic runs the measurements instead of extrapolating
them, and states the method with each result. It is the one epic that must not be compressed into
documentation: a programme that hears a laptop figure and then measures a phone discounts everything
else. Source: Report 2 R4, §7.1, Q2–Q4; Appendix A3 D2 and M3; risks R5, R6, R9.

### Story 6.1: On-device proving measurements

As a programme evaluating the wallet on real hardware,
I want p50/p95/p99 approval-to-proof-ready, cold and warm, plus peak memory, reported per device,
So that the pending figures stop being pending and the specification's device-side prover budgets
(`EE-ZKP-040`: ≤ 2.0 s p95 and ≤ 3.5 s p99 approval-to-proof-ready; `EE-ZKP-041`: ≤ 250 MB peak)
are checked on real hardware.

**Acceptance Criteria:**

- **Given** the committed `ZkProofBenchmark.kt` harness and the named device set (a Pixel 8 and
one mid-range device),
- **When** the programme is run,
- **Then** figures are reported per device, cold and warm, with `VmHWM` peak memory,
- **And** the results are assessed against `EE-ZKP-040`'s device-side prover budgets
(approval-to-proof-ready ≤ 2.0 s p95 and ≤ 3.5 s p99) and `EE-ZKP-041`'s ≤ 250 MB peak-memory
bound, and a miss is recorded as a miss in `docs/MEASUREMENTS.md` with the device and date named,
- **And** any row that remains unmeasured is left explicitly `PENDING-DEVICE` with a dated reason,
- **And** `docs/MEASUREMENTS.md` is updated with the method, the baseline and the confidence.
- **And** a device whose figures exceed the budgets has a pinned consequence rather than a note:
proximity Tier 2 is withheld for that device and `docs/MEASUREMENTS.md` records the miss, asserted
by the harness threshold check,
**Blocked by (external):** a Pixel 8 and one mid-range device. Where the external party or
hardware is unavailable, the story still closes its in-repository half and records the state as
blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR26. **Source:** A1 required change; U3 dependencies; §7.1 benchmark 1; Q4; D2 deliverable.

### Story 6.2: Verification capacity on target hardware

As a relying party sizing an instance,
I want verification capacity measured on the instance type the relying party plans to run,
So that budgets are planned from measurement rather than invention.

**Acceptance Criteria:**

- **Given** `BenchmarkMultipazVerify` and the percentiles harness,
- **When** the benchmark is re-run on the relying party's planned instance type,
- **Then** a stated verifications-per-second figure with its percentiles is published for that
instance type,
- **And** the existing host figures (p50 ≈ 3.24–3.83 s, p95 ≈ 4.10–4.91 s on an i5-8365U; ≈ 2.7
core-seconds per verify, ≈ 1,300
verifies/hour/core on a quiet host) are published as a capacity statement for a named hardware class rather than as a missed device budget,
- **And** `EE-ZKP-040`'s verifier-side ≤ 1.0 s p95 is re-expressed as a hardware-class-named
per-verification latency plus a throughput figure the relying party provisions for, rather than as
one 1.0 s p95 spanning both the phone and the server, since the same requirement sets the device
prover's ≤ 2.0 s p95 separately,
- **And** the peak-rate assumption used to size anything is labelled as an assumption agreed with
the relying party.
- **And** the relying party's required rate is stated, and a published figure that falls below it
is marked as below requirement rather than presented as a neutral capacity figure,
**Covers:** FR27. **Source:** §7.1 benchmark 2; Q2; risk R5 mitigation; M3.

### Story 6.3: Proximity BLE transfer measurement

As a holder at a venue,
I want a Tier-2 proximity ZK presentation measured end to end over BLE,
So that a ~360 KB proof either fits a stated budget or is documented as not fitting one.

**Acceptance Criteria:**

- **Given** the proximity path and a ~360 KB proof,
- **When** the transfer is measured with the wallet's own timestamps,
- **Then** the measured transfer time is published against `EE-ZKP-044`'s end-to-end ≤ 4.0 s p95
budget (user approval to the relying party's accept/reject decision, inclusive of proving,
transfer and verification), which `EE-CNF-006` makes a precondition of proximity Tier 2,
- **And** a refusal is raised when a ZK response is impossible, with no silent downgrade to a
plain mdoc,
- **And** an over-budget or previously-measured-slow transport pair results in a Tier-2 retry or a
refusal — never a fall to Tier 1 — wherever the relying party's registration certificate asserts
ZKP capability (`EE-ZKP-045a`), and a pair is not recorded as exceeding the budget on a
measurement taken against such a relying party,
- **And** proximity Tier-2 stays marked optional if it remains unmeasured.
- **And** a transfer interrupted mid-way — a BLE disconnect or a truncated payload — discards the
partial payload and is recorded as refused or retried, never as a completed measurement,
**Covers:** FR28. **Source:** A8 required change; U3 workflow and acceptance criteria; §7.1 benchmark 3; risk R6.

### Story 6.4: Verification budget and refusal-cost regression

As a verifier operator,
I want the refusal cost pinned by a regression test and the admission control tuned on measured figures,
So that the 54–58 s refusal defect cannot return unnoticed.

**Acceptance Criteria:**

- **Given** the 24 September 2026 fix for the 54–58 s refusal defect,
- **When** a refusal-cost regression test runs,
- **Then** the refusal cost is pinned to a stated numeric bound (a p95 measured on the same
percentile harness the capacity figures come from) and asserted on every run, failing the run when
the bound is exceeded,
- **And** admission control is tuned to the measured capacity rather than to a guess,
- **And** the previously rejected host-side optimisations (Rust PGO, FFI scratch arena, Go PGO)
are not re-attempted without a new profile showing a changed cost distribution.
- **And** the bound's numeric value and the harness that produced it are published in
`docs/MEASUREMENTS.md`, so the regression test fails on a number a reader can find,
**Covers:** FR29. **Source:** M3 deliverable and exit criteria; Report 1 §1 finding 4; `docs/MEASUREMENTS.md` optimization log.

### Story 6.5: Register latency and availability profile

As a relying party whose every presentation depends on a register lookup,
I want the register's latency and availability impact profiled once its interface exists,
So that a fail-closed lookup on the critical path is a known cost rather than a surprise.

**Acceptance Criteria:**

- **Given** a register implementation (mock, or the real one when it exists),
- **When** the presentation path is profiled,
- **Then** the added latency and the behaviour under register unavailability are recorded,
- **And** the bounded-cache and stale-data decisions from Story 3.3 are exercised by the profile.

**Covers:** FR30. **Source:** §7.1 benchmark 5; risk R9.

### Story 6.6: Honest measurement reporting

As a reader of this repository's measurements,
I want every figure to carry its baseline, method, assumptions and confidence, with unmeasured rows saying so,
So that no estimate is read as a measurement.

**Acceptance Criteria:**

- **Given** the existing measurement prose and the new figures,
- **When** the reporting is reviewed,
- **Then** every figure states its baseline, method, assumptions and confidence,
- **And** no effort or cost range appears without its method,
- **And** unmeasured items are labelled explicitly rather than omitted.

**Covers:** FR26, FR27. **Source:** NFR2; NFR7; A5.1 measurements row; risk R15.

### Story 6.7: Proximity relay separability

As a security reviewer of the proximity path,
I want the round-trip time of the BLE exchange measured directly and through a relay,
So that any claim that timing detects relay rests on a measured separation or is withdrawn.

**Acceptance Criteria:**

- **Given** the proximity transfer measured in Story 6.3, on at least the device models of Story 6.1,
- **When** round-trip times are recorded for a direct exchange and for the same exchange relayed through a second phone over a network link, with N per condition stated,
- **Then** the report gives both distributions (p50, p95, p99), the relay overhead, and whether one threshold separates them at a stated false-reject rate on the direct path,
- **And** a negative result, no separating threshold, is reported as such and the proximity documentation then says timing does not detect relay,
- **And** no threshold is taken from another project's documents,
- **And** the method states devices, OS versions, distance, radio interference and relay set-up (Story 6.6), and an unmeasured row says so,
- **And** this story adds no timing check to the wallet; adding one is a separate decision that uses this result.

**Covers:** FR73. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon `docs/engineering/nfc-relay-threat-defense.md` and `anti-relay-cryptographic-channel-binding.md` as a source of hypotheses only; its own audit, finding X5, records contradictory thresholds of 25 ms and 40 ms). **Depends on:** 6.1, 6.3. **External gate:** devices (`PENDING-DEVICE`).

## Epic 7: Documentation honesty

One conformity row contradicts the code, "conformant" language is used where only evidence exists,
and four clause maps are missing. Stories 1-4 are the cheapest work in the backlog and everything
else depends on the repository being worth believing, so they run alongside the start of Epic 1.
Source: Report 2 R3; Report 1 §2; risk R12; Appendix A6.

### Story 7.1: Fix the stale issuerAuth conformity row

As a technical evaluator reading `docs/CONFORMITY.md`,
I want the row that records on-device `issuerAuth` validation as absent corrected,
So that the note does not contradict the code it describes.

**Acceptance Criteria:**

- **Given** `docs/CONFORMITY.md` records on-device `issuerAuth` validation as "absent", while
`CredentialToDocumentMapper.kt` performs the leaf-signature and PKIX-anchoring check with tests
for the anchored and unanchored cases,
- **When** the row is corrected,
- **Then** the row describes the verified behaviour and cites both sides by `path:line`,
- **And** the correction keeps "documented" and "verified in code" as separate statements,
- **And** the change is recorded in `docs/README.md`'s index if the document set changes.

**Covers:** FR31. **Source:** Report 1 §2 divergence; R3; risk R12.

### Story 7.2: Audit remaining hand-maintained conformity rows

As a maintainer,
I want the remaining hand-maintained rows in `docs/CONFORMITY.md`, `docs/MEASUREMENTS.md` and the `STEP*-RECORD.md` files checked against the code,
So that the one found row is not the only one.

**Acceptance Criteria:**

- **Given** the hand-maintained prose files,
- **When** each status-bearing row is checked,
- **Then** the symbol named in the row is located in `app/src/main`, `verifier/go` or `tests/`
before the row is accepted or corrected,
- **And** every corrected or confirmed row cites `path:line`,
- **And** the audit's scope and its remaining un-audited rows are stated rather than implied.
- **And** a third audit outcome exists for a row whose named symbol cannot be located anywhere in
the tree: the row is struck as describing removed code rather than left standing as current,
**Covers:** FR31. **Source:** risk R12 residual uncertainty ("the repository has more rows of this kind that were not audited").

### Story 7.3: Test-derived conformity statuses

As a maintainer,
I want conformity-note statuses re-derived from tests instead of hand-maintained,
So that the note cannot drift from the code between sessions.

**Acceptance Criteria:**

- **Given** the conformity note's status rows,
- **When** the derivation is built,
- **Then** statuses come from test evidence rather than prose editing,
- **And** the derivation runs as part of the repository's checks,
- **And** a row whose test disappears becomes visible rather than silently stale.

**Covers:** FR32. **Source:** risk R12 mitigation; R3.

### Story 7.4: Evidence-scoped compliance and estimate vocabulary

As someone showing this repository to a programme,
I want "conformant" and "certified" removed and every estimate to carry its method,
So that the only sentence the repository makes is "these requirements are supported by these tests, in this environment".

**Acceptance Criteria:**

- **Given** the repository's documents and artefacts,
- **When** the vocabulary pass is complete,
- **Then** no document claims conformance or certification,
- **And** the supported-by-evidence formulation is used instead,
- **And** every effort or cost range carries the method and assumptions that produced it.

**Covers:** FR33. **Source:** R3; L3; A11; §4 certification reserved; NFR1; NFR7; risk R15.

### Story 7.5: HAIP 1.0 clause statement

As an evaluator comparing the fork against a final standard,
I want a clause-by-clause statement against HAIP 1.0,
So that the cheapest remaining credibility gap against the settled OpenID profiles is closed.

**Acceptance Criteria:**

- **Given** HAIP 1.0 Final (24 December 2025) and the fork's existing behaviour (encrypted
response by default, client-id prefix policy, per-presentation unlock),
- **When** the clause statement is produced,
- **Then** every clause is marked supported, partially supported or not applicable, with the
evidence for each,
- **And** the statement does not claim profile conformance.
- **And** a clause that cannot be assessed has its own member in the status set, so an
unassessable clause is never recorded as satisfied or as not applicable,
**Covers:** FR34. **Source:** S3 required change and verification method; §4 technical standards paragraph.

### Story 7.6: TS13 clause map

As an implementer,
I want a clause map against EC TS13,
So that `mso_mdoc_zk` support and the `zk-jwt` gap are both visible instead of implied.

**Acceptance Criteria:**

- **Given** TS13 v1.0 (`mso_mdoc_zk`, `zk-jwt`, `ZkSystemSpec`, `ZkDocument`,
`longfellow-libzk-v1`, `circuit_hash`),
- **When** the clause map is produced,
- **Then** `mso_mdoc_zk` is marked supported with its evidence in `verifier/go/oid4vp/dcql.go` and
`DeviceRequestParser.kt`,
- **And** `zk-jwt` is marked missing,
- **And** the map records that TS13 states it is not intended to be a final specification and
hands over to ETSI TS 119 476-2.
- **And** every TS13 construct the map covers — `ZkSystemSpec`, `ZkDocument` and `circuit_hash` as
well as the two formats — carries a stated marking, not only the formats,
**Covers:** FR35. **Source:** S7; O1 deliverables note.

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

### Story 7.8: Narrow-scope statement for the ZKP work

As a programme owner deciding whether this work fits,
I want a written statement that the implementation covers a boolean age predicate on one attestation and that the general ARF ZKP vision is out of scope,
So that the deliberate narrowness is stated rather than discovered.

**Acceptance Criteria:**

- **Given** the ARF's general ZKP requirements (`ZKP_01` generic predicates, `ZKP_03` binding,
`ZKP_04` pseudonyms, `ACP_03`),
- **When** the scope statement is written,
- **Then** it names each of those requirements as out of scope with a reason,
- **And** it states that generality depends on schemes and specifications that do not exist yet,
- **And** it is phrased so that a narrow measured implementation is not presented as a general
one.

**Covers:** FR37. **Source:** A4 required change; A6, A7, A16; A6 tradeoff 1 ("stay narrow and say so").

### Story 7.9: Attestation ordering statement

As an Estonian programme deciding which attestation to prefer,
I want the `eu.europa.ec.av.1`-first ordering documented together with the `ee.riik.poa.1` fallback,
So that the interoperability choice and its political tradeoff are both visible.

**Acceptance Criteria:**

- **Given** the spec's existing choice to prefer the EU AV attestation for every relying party,
- **When** the ordering statement is written,
- **Then** it records both paths, the ordering, and the reason the ordering favours
interoperability,
- **And** it names the risk that an Estonian programme may want its own credential first,
- **And** both paths remain implemented.

**Covers:** FR38. **Source:** A6 tradeoff 2.

### Story 7.10: National-wallet production comparison

As a programme owner,
I want a comparison against the reference wallet app and the German national wallet build covering certification, provisioning and support,
So that the production pathway gap is described in the terms a procurement uses.

**Acceptance Criteria:**

- **Given** `eudi-app-android-wallet-ui` (dev/demo builds, not production-ready) and
`de-eudi-wallet-android` (read-only mirror, placeholder endpoints, unsigned release builds),
- **When** the comparison is written,
- **Then** it states what a national-wallet production pathway requires that this repository does
not have,
- **And** it records the difference between a one-way source mirror and a reproducible build,
- **And** it does not claim a certification path exists.

**Covers:** FR33. **Source:** A1.1 rows 3 and 10; A1.3 row 6; risk R12; L3.

### Story 7.11: Estimate method discipline

As a recipient of this repository's effort figures,
I want the method and assumptions attached to every effort or cost figure this repository
publishes, including the 6–12 engineer-month re-creation range (Report 2 Q1) that is human cost
rather than agent-assisted effort,
So that a range is never read as a commitment.

**Acceptance Criteria:**

- **Given** every published figure of effort or cost, whether derived from agent-assisted work or
from human re-creation estimates (Report 2 Q1's 6–12 engineer-months carries its own baseline,
method and confidence),
- **When** any estimate is published,
- **Then** the method, the comparable slice it was derived from and the assumption set accompany
it,
- **And** no estimate appears in a schedule-shaped form,
- **And** the agent-assisted baseline is stated so a conventional team can size its own effort.

**Covers:** FR33. **Source:** risk R15 mitigation; Report 1 §6 ("effort estimates are one-person, agent-assisted baselines").

## Epic 8: Standards watch and open-risk reporting

Every opportunity in the corpus is a bet on an unfinished standard: ISO second edition has slipped to Q1
2027, ZKP was removed from DCHP's initial proposal, OpenID4VP #627 has been open for ~16 months, and
`ZKP_08` names a catalogue with no ZKP scheme. This epic keeps the open risks visible in writing and prepares for a second ZK family. The disclosure controls it used to carry are Stories 4.7 and 11.8, where the work they belong to lives. Source: Report 2 R5, §8; Appendix A4 risks R1–R3, R7, R10–R11, R14; Appendix A6.

### Story 8.1: ZKP_08 and ECCG tension report

As a programme owner being told about compliance,
I want a written report of the `ZKP_08`/ECCG tension with its sources and a re-check trigger,
So that nobody is told `ZKP_08` is met.

**Acceptance Criteria:**

- **Given** `ZKP_08` requires a scheme relying solely on the ECCG Agreed Cryptographic Mechanisms
v2.0 and that catalogue lists no ZKP scheme, while the AV Profile mandates `longfellow-libzk-v1`,
- **When** the report is written,
- **Then** it states the tension, names both primary sources, and says the requirement is
unsatisfiable as written,
- **And** it states that this is a finding about the specification rather than about this fork,
- **And** a re-check trigger is recorded on the catalogue's next revision.

**Covers:** FR39. **Source:** R5; A11; risk R3; §2.1 conflict paragraph; §8 conflicts table.

### Story 8.2: ISO 18013-5 second-edition watch

As a maintainer of the carrier layer,
I want a documented watch with version-labelled fixtures and a rename/adapt plan,
So that the published edition is a bounded change rather than a rewrite.

**Acceptance Criteria:**

- **Given** the ISO/IEC 18013-5 second edition's publication slipping from Q3 2026 to Q1 2027
`[UNVERIFIED]` (from the ISO project page, not the standard) and the ISO text being unreachable
during research,
- **When** the watch is established,
- **And** the fixtures and profile rules stay version-labelled under Story 2.3's assertion (this
story keeps the watch and the rename/adapt plan, not a second labelling duty),
- **And** `docs/analysis/ISO-18013-5-2E-BYTE-DIFF-CHECKLIST.md` is extended with the rename/adapt
steps,
- **And** the carrier interface stays the single adaptation point and the vectors are re-run when
the edition lands,
- **And** `ZKPresentation` remains labelled an interim versioned encoding rather than an
interoperability claim.

**Covers:** FR40. **Source:** S6 required change; U2 dependencies; risk R1; §6 "the ISO second-edition text could not be read".

### Story 8.3: zk-jwt path feasibility

As a relying party that needs proofs over SD-JWT VC,
I want a feasibility study for the `zk-jwt` format with a fixture round-trip,
So that the mdoc-only limitation is a known, costed gap rather than an assumption.

**Acceptance Criteria:**

- **Given** TS13 defines a `zk-jwt` format and the fork's ZK path is mdoc-only,
- **When** the study is completed,
- **Then** it states whether a `zk-jwt` path is feasible, at what cost, and what it would change,
- **And** a fixture round-trip demonstrates the finding or records why it cannot be demonstrated
yet,
- **And** the conclusion is reflected in the TS13 clause map.

**Covers:** FR41. **Source:** A9 required change; S7; U2 (`ZKP_06`).

### Story 8.4: Per-path unlinkability statement

As a privacy engineer,
I want an explicit unlinkability statement per path backed by a test asserting that no tracking
identifier is added,
So that `ZKP_07` is addressed with evidence rather than assertion.

**Acceptance Criteria:**

- **Given** one proof per presentation over the encrypted response and the documented `msoX5chain`
residue,
- **When** the statement and test are produced,
- **Then** an unlinkability statement exists per path (ZK and plain fallback),
- **And** a log/carrier inspection test asserts no tracking identifier is added,
- **And** the residue that remains is stated rather than omitted.

**Covers:** FR42. **Source:** A10 required change and verification method.

### Story 8.5: LoA-High authentication with a ZKP presentation

As a relying party that must preserve LoA High,
I want a test where a LoA-High authentication happens alongside a ZKP presentation,
So that `ZKP_09` moves from "compatible in principle" to tested.

**Acceptance Criteria:**

- **Given** TARA and Smart-ID/Mobile-ID as a second authentication route,
- **And** a route that is unavailable or an authentication that fails at demonstration time is
recorded as the demonstration's outcome, not left undefined,
- **When** the pilot test is run,
- **Then** a LoA-High authentication alongside a ZKP presentation is demonstrated,
- **And** the result is recorded with its environment and method,
- **And** the finding is reflected in the requirements matrix.

**Covers:** FR43. **Source:** A12 required change and verification method; §6 Smart-ID/Mobile-ID row.

### Story 8.6: DCHP #17 filing and issue tracking

As an implementer who wants a published carrier,
I want the prepared DCHP #17 submission text filed and the two OpenID issues tracked,
So that the standards gap this carrier fills is visible where it can be closed.

**Acceptance Criteria:**

- **Given** `docs/profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md` is prepared and unfiled,
- **When** the filing is performed,
- **Then** the submission is made with the user's explicit approval (this step is user-gated),
- **And** the mutual verification of spec fields follows the carrier-profile gate,
- **And** OpenID4VP issue #627 and openid/dchp issue #17 are tracked with their status.
- **And** a filing the user withholds approval for is recorded as withheld for that reason, so a
withheld submission is distinguishable from unfinished work,
**Covers:** FR44. **Source:** Report 1 §2 (prepared but unfiled); A5.3.

### Story 8.7: Missing ZKP_03 ZKP_04 ACP_03 design notes

As a reader of the requirements matrix,
I want short design notes recording `ZKP_03`, `ZKP_04` and `ACP_03` as out of scope with what each would require,
So that the missing rows are explained rather than merely marked missing.

**Acceptance Criteria:**

- **Given** A6 (`ZKP_03` binding of an attestation to a PID), A7 (`ZKP_04` pseudonym derivation)
and A16 (`ACP_03`) are marked missing,
- **When** the notes are written,
- **Then** each note states what a scheme for it would require and why this repository does not
implement one,
- **And** `ACP_03`'s SHOULD-use-ZKP wording is quoted,
- **And** no note presents the gap as planned work.

**Covers:** FR45. **Source:** A6, A7, A16; A6 tradeoff 1.

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

### Story 8.9: Upstream-dormancy maintenance position

As a maintainer,
I want the maintenance position recorded and a succession note written,
So that the fork carrying the whole load is a stated condition rather than a discovered one.

**Acceptance Criteria:**

- **Given** upstream has one commit in 12 months, 0 open issues/PRs and a single contributor, and
the fork is 0 commits behind (the corpus records this as judgement from the repository's activity,
not as a published policy),
- **When** the position is recorded,
- **Then** it states that the fork is a strict superset and vendors what it needs,
- **And** library upgrades stay routine and pinned in `gradle/libs.versions.toml`,
- **And** the absence of a second maintainer is named rather than glossed.

**Covers:** FR47. **Source:** risk R10.

### Story 8.10: Octagon assessment of the fork

As the maintainer,
I want this fork assessed against the eight axioms of the Zero-Trust Octagon, with a falsification list for each,
So that the fork's trust claims meet an external structured method and what fails is published.

**Acceptance Criteria:**

- **Given** the axioms and the architecture validation checklist (Appendix B) of zero-trust-octagon, pinned by commit,
- **When** each axiom is applied to the wallet, verifier, registry and rp-kit as they stand,
- **Then** the document gives a per-axiom verdict (holds, fails or not applicable, with reason) and a falsification list: the evidence that would show the verdict wrong,
- **And** Axiom 3 (unbypassable mediation) is assessed against the plain fallback and the plain `direct_post` flag, and Axiom 4 (continuous verification) against status-list and registry freshness, and each outcome is recorded whichever way it falls,
- **And** where the document speaks of quorum or fault tolerance it states n and f, and does not call a voting scheme that tolerates f = 0 Byzantine (zero-trust-octagon audit, finding X3),
- **And** no verdict is worded as compliance or certification (NFR1),
- **And** each failing verdict becomes a story or a recorded risk, and the document is indexed in `docs/README.md`.

**Covers:** FR74. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon Octagon and Appendix B). **Depends on:** 4.7, 7.8.

## Epic 9: Statutory age-threshold toolkit

The AV Profile covers 18 and treats `age_over_NN` as optional; Estonian law has several thresholds
(alcohol and tobacco at 18/21, other restrictions at 16). This epic builds the threshold-to-circuit map,
the accepted-circuit set and a negotiation library implementing §7.2.5's "nearest true above request" with
the fork's stricter single-predicate and withhold-false rules. It is externally gated: a circuit must be
generated and accepted by a scheme owner, so it waits on Epic 5 and on a decision by whoever owns the
accepted set. Source: Report 1 O6; Appendix A3 R2; spec Annex A; `EE-POA-027`–`EE-POA-031`; risk E3.

### Story 9.1: Statutory threshold-to-circuit map

As an attestation provider in a Member State with several age thresholds,
I want a documented threshold-to-circuit map with legal citations,
So that the thresholds are traceable to law rather than to memory.

**Acceptance Criteria:**

- **Given** the Estonian statutory ages and the fork's existing rules,
- **When** the map is published,
- **Then** each threshold carries its legal citation,
- **And** any threshold that cannot be mapped is treated as unsupported rather than guessed,
- **And** the map states its version so a statutory change is a visible revision.

**Covers:** FR49. **Source:** O6 proposed capability; E3; O6 risks (legal mapping errors are consequential).

### Story 9.2: Accepted circuit set per threshold

As a relying party asking for a specific threshold,
I want an accepted circuit set covering the mapped thresholds,
So that a proof for the requested threshold can actually be produced.

**Acceptance Criteria:**

- **Given** the Epic 5 registry and the mapped thresholds,
- **When** the accepted set is assembled,
- **Then** each mapped threshold has a named circuit,
- **And** an unmapped threshold yields refusal,
- **And** the external wait for scheme-owner acceptance is recorded as the state of each entry
rather than as a blocked story.

**Covers:** FR50. **Source:** O6 proposed capability; O6 dependencies (`circuit acceptance is external`); risk R4.

### Story 9.3: Nearest-true-above negotiation library

As a wallet implementer,
I want a negotiation library turning "the relying party asked for 21" into the right single predicate without ever disclosing a `false` one,
So that the rule is implemented once rather than improvised per request.

**Acceptance Criteria:**

- **Given** ISO/IEC 18013-5 §7.2.5 and `EE-POA-030` as the specification states it (at most one
predicate per presentation, `false` predicates withheld, and the one exception — a relying party
whose *registered intended use names each* requested predicate),
- **When** the library is written,
- **Then** it implements nearest-true-above with the stricter constraints applied,
- **And** the divergence from ISO §7.2.5 is documented as deliberate,
- **And** the library is covered by tests rather than by the policy class's existing assertions
alone,
- **And** a certificate-backed two-predicate request that Story 4.3 admits is not refused
downstream, asserted end to end with Story 4.3's fixture,
- **And** the divergence from ISO §7.2.5 is recorded against spec §22 item 21,

**Covers:** FR51. **Source:** O6 proposed capability; spec §22 divergence; `EE-POA-027`–`EE-POA-031`.

### Story 9.4: Threshold boundary tests

As a programme owner verifying a threshold claim,
I want table-driven boundary tests per threshold including the day before, the day of, a leap-day birth and an incomplete date,
So that the boundary cases are proven rather than assumed.

**Acceptance Criteria:**

- **Given** the mapped thresholds and the existing leap-day/incomplete-date analysis,
- **When** the boundary suite runs,
- **Then** for each threshold a proof verifies only for a holder whose age meets it,
- **And** the day before, the day of, a leap-day birth and an incomplete date are each covered,
- **And** the 28 February rule derived from ISO §7.2.5 is asserted explicitly,
- **And** the boundary set includes the day *after* the threshold turning point as well as the day
before, and an impossible or future date of birth is distinguished from an incomplete one,
- **And** the suite is added to `tests/README.md` and wired into `make check` or the matching CI job (or the story records why it is excluded and how it is run instead),

incomplete date, and neither is answered as a plain false predicate,
before, so an off-by-one in the comparison cannot pass the suite,
**Covers:** FR52. **Source:** O6 acceptance criteria (i); E3 verification method; spec Annex A.

### Story 9.5: Refusal with a user-visible reason

As a holder who does not hold the requested threshold,
I want a refusal with a reason, never a neighbouring threshold,
So that a threshold I am not entitled to is not silently answered with a different one.

**Acceptance Criteria:**

- **Given** a request for a threshold the wallet does not hold,
- **When** the presentation is attempted,
- **Then** it is refused with a user-visible reason,
- **And** no neighbouring threshold is transmitted in its place,
- **And** an assertion over all fixtures shows no `false` predicate is ever transmitted.

**Covers:** FR53. **Source:** O6 acceptance criteria (ii)–(iii); U1 acceptance criteria.

### Story 9.6: Legal review of the threshold list

As the programme owner,
I want the threshold list legally reviewed and an unsupported-threshold policy published,
So that any unmapped threshold is explicit rather than incidental.

**Acceptance Criteria:**

- **Given** the statutory threshold map,
- **When** the legal review completes,
- **Then** the review's outcome and reviewer role are recorded,
- **And** the unsupported-threshold policy states what happens for an unmapped threshold,
- **And** the extra circuits' cost effect on prover time and proof size (~353–360 KB, ~6.1 s
proving) is recorded alongside the benefit.
- **And** a threshold whose legal citation the reviewer rejects is unmapped and stops proving,
rather than staying mapped on a citation that no longer supports it,
**Blocked by (external):** a legal reviewer for the threshold citations. Where the external party
or hardware is unavailable, the story still closes its in-repository half and records the state as
blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR54. **Source:** O6 validation; O6 risks; §7 Q3.

## Epic 10: Issuance, trust and revocation completeness

The requirements matrix marks issuance, the Wallet Unit Attestation and revocation as partial: issuance is
served by mocks and the demo issuer, the WUA is a mock with written requirements, and status-list handling
is split out with a revoked-fixture test missing. These are pilot prerequisites rather than research work.
Source: L4, S2, S9, S11, A2; Report 2 §7.1.

### Story 10.1: Real issuer integration

As an integrator,
I want issuance exercised against the Commission's issuer service rather than a mock,
So that the issuance framework claim is backed by interop evidence.

**Acceptance Criteria:**

- **Given** the pinned `eudi-lib-jvm-openid4vci-kt` 0.6.1 and the mock issuance paths,
- **When** the integration test runs against the Commission's issuer service,
- **Then** the result is recorded with the service's revision and environment,
- **And** the requirements matrix's issuance row is updated from its evidence,
- **And** the mock paths remain available for offline tests.
- **And** the Commission's issuer service being unavailable or absent at test time is recorded as
blocked with the reason, so an unreachable service never leaves the interop claim unstated,
**Blocked by (external):** the Commission's issuer service, available and reachable. Where the
external party or hardware is unavailable, the story still closes its in-repository half and
records the state as blocked with the reason and date — never as passed, and never with a
fabricated result.
**Covers:** FR55. **Source:** L4 required change and verification method; S2 required change.

### Story 10.2: Batch issuance

As a holder who does not want presentations linkable,
I want batch issuance for `ee.riik.poa.1` and the AV attestation,
So that the recorded data-minimisation gap is closed.

**Acceptance Criteria:**

- **Given** batch issuance for `ee.riik.poa.1` is a recorded gap,
- **When** it is implemented,
- **Then** multiple credentials are issued for one issuance event,
- **And** a fixture-based disclosure test documents what each path reveals per presentation,
- **And** the unlinkability statement of Story 8.4 is updated to cover the batched path.
- **And** a batch issued partially — fewer credentials than requested, or a mid-batch failure — is
detected and reported, because a partial batch silently reduces the unlinkability the story
claims,
**Covers:** FR56. **Source:** A2 required change and verification method; S2; L4.

### Story 10.3: Real Wallet Unit Attestation issuance

As a wallet provider,
I want real WUA issuance per TS3 rather than a mock,
So that the wallet-unit obligation is met by an implementation instead of a written requirement.

**Acceptance Criteria:**

- **Given** `EE-WUA-007`/`EE-WUA-008` are written and the WUA paths are mock-only,
- **When** real issuance is implemented,
- **Then** an interop test runs against a wallet-provider service,
- **And** the mock remains for offline work,
- **And** the requirements matrix's WUA row is updated from its evidence.
- **And** a WUA that is issued but cannot be validated, and a wallet-provider service that is
unavailable, are both outcomes of the story rather than a test that checks issuance alone,
**Blocked by (external):** a wallet-provider service that issues a real WUA. Where the external
party or hardware is unavailable, the story still closes its in-repository half and records the
state as blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR57. **Source:** S9 required change and verification method; `EE-WUA-007`/`008`.

### Story 10.4: Revocation end-to-end with a revoked fixture

As a relying party,
I want an end-to-end status-check demonstration against a real list with a revoked fixture,
So that revocation is exercised rather than split into partial handling.

**Acceptance Criteria:**

- **Given** status-list handling is split out and CRL soft-fail is the documented policy,
- **When** the demonstration runs,
- **Then** a revoked fixture is refused end to end,
- **And** a certificate named in a verified CRL is refused, while a CRL that cannot be fetched is
logged only,
- **And** a CRL past its `nextUpdate` is neither treated as current nor silently ignored,
- **And** the policy's flip condition is stated in the demonstration's record.

**Covers:** FR58. **Source:** S11 required change and verification method.

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

## Epic 11: Production readiness for a pilot relying party

The largest non-code epic, and the only one whose exit criterion is a state rather than an
artefact: a pilot relying party can run this service and show an auditor why it should be trusted.
The repository has no operations, monitoring or incident surface today, no accessibility evidence
and no external security review, and no amount of cryptography compensates for any of the three.
Exit: every story closed with its artefact in place, the game day passed, and no unresolved high
security finding. Source: Appendix A3 R1; risks R13; A13; A14; U4.

### Story 11.1: Operations runbook

As an operator running the verification service for a pilot,
I want an operations runbook,
So that routine operation is documented rather than improvised.

**Acceptance Criteria:**

- **Given** the deployed service profile from Epic 3,
- **When** the runbook is written,
- **Then** it covers start, stop, drain (SIGTERM with `-drain-timeout`, `503`/`Retry-After`,
`draining:true` on `/healthz`), upgrade and rollback,
- **And** it names the failure modes with a first diagnostic step each,
- **And** it is exercised in Story 11.5.
- **And** a drain whose `-drain-timeout` expires with requests still in flight has a stated
operator behaviour, so an unfinished drain is not improvised,
**Covers:** FR60. **Source:** A3 R1 deliverable; `tests/drain_test.sh` behaviour.

### Story 11.2: Monitoring availability and incident process

As a service owner,
I want monitoring, a stated availability target and an incident process,
So that a pilot has an operational surface.

**Acceptance Criteria:**

- **Given** the service exposes `/healthz`,
- **When** monitoring is configured,
- **Then** an availability target is stated numerically,
- **And** the incident process names who is notified, the severity levels and the post-incident
record,
- **And** the register's unavailability behaviour is covered because it is on the critical path.
- **And** a breach of the numeric availability target raises an incident at a stated severity, so
a target breach cannot pass without an incident,
**Covers:** FR61. **Source:** A3 R1 deliverable; risk R9.

### Story 11.3: Accessibility conformance audit

As a public-sector buyer under the accessibility duty,
I want an EN 301 549 conformance audit alongside the WCAG 2.1 AA commitment,
So that the accessibility claim has evidence.

**Acceptance Criteria:**

- **Given** PRODUCT.md commits to WCAG 2.1 AA, reduced-motion and screen-reader announcements are
implemented, and no EN 301 549 evidence exists,
- **When** the audit completes,
- **Then** an audit report exists with its findings and their resolution,
- **And** the audit is recorded as a milestone exit criterion for the pilot,
- **And** the report states which parts of the interface were and were not in scope.
- **And** an audit finding whose resolution is deferred or refused blocks the exit milestone, so
the milestone cannot be declared met with findings still open,
**Blocked by (external):** an accessibility auditor. Where the external party or hardware is
unavailable, the story still closes its in-repository half and records the state as blocked with
the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR62. **Source:** A13 required change; E5; risk R13; A3 R1 exit criteria.

### Story 11.4: External security review

As a pilot sponsor,
I want an external security review of the deployed service with no unresolved high findings,
So that the pilot is not run on unreviewed code.

**Acceptance Criteria:**

- **Given** the deployed service and its trust boundaries,
- **When** the external review completes,
- **Then** no high finding remains unresolved,
- **And** the review's scope and the reviewer's independence are recorded,
- **And** any accepted residual risk is stated with its rationale.
- **And** a review that cannot be completed before the pilot is recorded as not performed,
distinct from a clean review, so the two are never confused,
**Blocked by (external):** an independent security reviewer. Where the external party or hardware
is unavailable, the story still closes its in-repository half and records the state as blocked
with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR63. **Source:** A3 R1 exit criteria.

### Story 11.5: Runbook game day

As an operator,
I want the runbook exercised in a game day,
So that it is proven rather than written.

**Acceptance Criteria:**

- **Given** the runbook and the deployed service,
- **When** the game day is run,
- **Then** a drain, a rollback and one named failure mode are exercised,
- **And** defects found in the runbook are fixed and the runbook re-issued,
- **And** the exercise's date, participants and outcome are recorded.
- **And** a game day that aborts partway because the environment is unavailable is recorded as
aborted, not as an exercised runbook,
**Blocked by (external):** an operator to exercise the runbook. Where the external party or
hardware is unavailable, the story still closes its in-repository half and records the state as
blocked with the reason and date — never as passed, and never with a fabricated result.
**Covers:** FR60. **Source:** A3 R1 exit criteria ("an operational runbook exercised in a game day").

### Story 11.6: Certificate Transparency for access certificates

As a relying party that trusts an access certificate,
I want SCT verification when access certificates are added, with CT test vectors,
So that the CT requirements are implemented rather than deferred.

**Acceptance Criteria:**

- **Given** `CT_01`–`CT_06` are missing and the current implementation validates chains and CRLs
only,
- **When** SCT verification is implemented,
- **Then** CT test vectors cover the accepted and rejected cases,
- **And** the open question of whether Estonian access certificates will be CT-logged is recorded
as unresolved,
- **And** the requirements matrix's CT rows are updated from their evidence.
- **And** an unreachable CT log leaves a stated acceptance behaviour — refusal or a logged
deferral — rather than an undefined one,
**Covers:** FR64. **Source:** A14 required change and verification method; §8 unresolved questions.

### Story 11.7: Cross-border onboarding without a national identifier

As an e-resident or cross-border holder,
I want a service to request a PID attribute subset plus an AV attestation in one session,
So that onboarding completes without the isikukood or full date of birth being disclosed.

**Acceptance Criteria:**

- **Given** multi-credential sessions are supported and verified by tests, and real PID issuance
is Story 10.1,
- **When** the onboarding flow is exercised,
- **Then** the session's log names both credential ids,
- **And** a missing credential fails the session with the id named,
- **And** no isikukood appears anywhere in the wire transcript.
- **And** a credential held in a form the wallet cannot present is reported as unpresentable and
named, distinct from a credential that is absent,
**Covers:** FR65. **Source:** U4 workflow, dependencies and acceptance criteria.

### Story 11.8: Dependency and vendored-artefact hygiene

As a licensing and security reviewer,
I want every vendored or mirrored artefact inventoried with its upstream licence and revision, and pre-1.0 dependencies watched,
So that provenance and churn risk are both known.

**Acceptance Criteria:**

- **Given** the published Maven mirror under `.github/maven-mirror`, the vendored `coseJava`
mirror, and the pinned pre-1.0 dependencies (multipaz, the `eu.europa.ec.eudi` libraries) — the
corpus records the newer-mirror licence position as `[UNVERIFIED]`, so it is established during
this story rather than assumed,
- **When** the inventory is produced,
- **Then** every vendored artefact is listed with its upstream licence and revision,
- **And** the MIT notice is retained and no material from a private repository is imported without
written permission,
- **And** the pre-1.0 pins are recorded with the breakage-detection role of the conformance module
named.

**Covers:** FR48. **Source:** risk R11; risk R14.

## Epic 12: Consent integrity and user-presence gates

The PRD's §4.7, and the only part of this backlog that exists to close findings rather than to add
capability. The product's own audit raised them on the paths the product advertises: a
relying-party
name on the consent screen that was never authenticated, device keys usable without the user
present,
a consent gate that can be guessed, and a proximity share that emits with no gate at all. The PRD
puts
all four in v1 §9.2, so they are requirements here rather than hygiene. The audit's remaining
items —
supply-chain pinning, main-thread blocking, debug logging, the mock trust surfaces and the release
path
— are an explicitly separate hygiene release and are not stories here.

Source: the PRD's §4.7
(FR-45–FR-48), which took them from `docs/sota-audit-ROLLUP.md` and the per-area audit files; risk
R13.

### Story 12.1: Requester name authenticated at consent

As a holder reading the consent screen,
I want the relying party named there to be the relying party that signed the request,
So that the screen's claim is true rather than a rendered string.

**Acceptance Criteria:**

- **Given** a request carrying `readerAuth` and a consent screen that renders a requester name,
- **When** the name is shown,
- **Then** the `readerAuth` chain is validated to a trust anchor before any name renders as verified,
- **And** an unvalidated common name is never rendered as verified, and when the chain does not validate the screen says the requester is unverified with refusal as the default,
- **And** a negative test presents a certificate carrying an attacker-chosen common name and asserts it is not rendered as verified,
- **And** the outcome is distinguishable from a retryable error and from user cancellation, per NFR13.

**Covers:** FR67. **Source:** PRD §4.7 FR-45; `docs/sota-audit-ROLLUP.md` consent-integrity
finding; `EE-PRO-025`.

### Story 12.2: Credential keys require user presence

As a holder whose device is unlocked and in someone else's hands,
I want my credential keys usable only after I authenticate,
So that a proof cannot be produced without me.

**Acceptance Criteria:**

- **Given** the platform's user-authentication capability and the wallet's key operations,
- **When** a key is used,
- **Then** the operation depends on a platform user-authentication result rather than on an application-level flag alone,
- **And** proof generation, plain presentation and signing each carry the gate, or the ungated path is removed rather than left reachable,
- **And** a test asserts that a key operation attempted without user authentication fails,
- **And** a device without the capability is refused rather than silently downgraded, recorded as blocked if hardware is absent.

**Blocked by (external):** a device that supports platform user authentication. Where it is
unavailable, the story closes its in-repository half and records the state as blocked with the
reason — never as passed.

**Covers:** FR68. **Source:** PRD §4.7 FR-46.

### Story 12.3: Consent gate resists guessing

As a holder,
I want the consent gate to be unguessable rather than a speed bump,
So that physical access to an unlocked device is not enough to approve a presentation.

**Acceptance Criteria:**

- **Given** the consent gate and its attempt counter,
- **When** attempts fail,
- **Then** failed attempts are counted in persistent state that survives process restart, with a lockout threshold and an escalation path stated in the artefact that publishes it,
- **And** relaunching the application does not reset the count,
- **And** the existing behaviour, where the counter resets on relaunch and the check is a simulated delay, is removed rather than documented,
- **And** a test asserts the count survives a restart.

**Covers:** FR69. **Source:** PRD §4.7 FR-47.

### Story 12.4: Proximity share carries consent

As a holder presenting in proximity,
I want to consent explicitly before anything is emitted,
So that the proximity path is not a way around the gate the remote path applies.

**Acceptance Criteria:**

- **Given** the proximity presentation path,
- **When** a share is requested,
- **Then** it passes the consent gate before a document is emitted, where today it emits with no gate at all,
- **And** the screen states what is shared and with whom on the same terms as the remote path,
- **And** a test asserts the gate is present on the proximity path,
- **And** the relation to Story 4.7 is stated: this story gates the share; Story 4.7 decides whether a ZKP-capable device answers at all.

**Covers:** FR70. **Source:** PRD §4.7 FR-48.

## Epic 13: Compellability dossier

A buyer or pilot owner asks who can lawfully compel the keys behind a verdict, not where the bytes sit.
Nothing in Epics 1 to 12 answers that. This epic publishes the answer as evidence, makes the one registry
that decides which circuits verifiers accept resistant to a single signer, and timestamps the evidence the
programme publishes. The method comes from [zero-trust-octagon](https://github.com/tomkabel/zero-trust-octagon): a verdict is `true` or `false` for
a claim fixed in advance, with a falsification list, and inputs are classed GRANT-capable or DENY-only. The
analysis behind it argues the compellability question, not geography, is what a sovereignty claim must
test. That argument and every legal proposition in this epic are `[UNVERIFIED]` here.
Source: zero-trust analysis 2026-11-04.

### Story 13.1: Trust-path verdict for the wallet ecosystem

As a procurement or pilot owner asking who can lawfully compel the keys behind this system's verdicts,
I want a published inventory of every key and attestation chain a verdict depends on, classified by what it can do,
So that the question is answered with evidence and the failing rows are shown.

**Acceptance Criteria:**

- **Given** the verdict path: credential-key attestation (Android Keystore attestation roots), Wallet Unit Attestation (Story 10.3), issuer signing and its HSM, the circuit-list signer (Epic 5), the RP-register (Story 3.3), status lists, and the hosts that serve them,
- **When** each chain is traced to its root and the legal entity operating that root is named,
- **Then** each row is classed GRANT-capable (cryptographically verifiable and revocable by a supervisory or trust-list body) or DENY-only (a contract, a declaration or a revocable administrative act), with the reason,
- **And** the published verdict is `true` or `false` for this claim, fixed on 2026-11-04 before the inventory starts: "Every key and attestation root in the wallet ecosystem's verdict path has a named operating entity, with that entity's jurisdiction of incorporation and ultimate parent stated, for the device set and date the report names." A row whose operator, jurisdiction or parent cannot be established is listed as unknown and never inferred, and any unknown makes the verdict `false`,
- **And** the report pins the date and the device set it covers, because Android Remote Key Provisioning issues short-lived per-app attestation certificates (Android documentation, updated 2026-09-30), so a verdict does not outlive its inventory,
- **And** the report states, as a labelled finding and not as the claim, which roots chain to an EU trusted list,
- **And** the report states its boundary: it establishes technical facts and the compulsion surface, and whether a given law reaches a given entity is a legal conclusion for counsel,
- **And** the grant/deny classification links to zero-trust-octagon Appendix F rather than copying it, and the predicate version used is cited,
- **And** every legal, jurisdictional and "no EU scheme certifies X" proposition carries `[UNVERIFIED]` until checked against a primary source, and the report is indexed in `docs/README.md`.

**Covers:** FR75. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon Appendices F and G; its verification-practice plan). **Depends on:** 5.6, 10.3. **Decision:** claim text fixed (maintainer, 2026-11-04). **External gate:** counsel review before publication.

### Story 13.2: Threshold-signed circuit list

As a verifier operator,
I want the circuit list to require t valid signatures from n named signers in different legal domains,
So that no single signer, stolen key or court order can change which circuits verifiers accept.

**Acceptance Criteria:**

- **Given** the document format of ADR-006 and the verifier fetch of Story 5.2,
- **When** a list is issued with t-of-n signatures and the verifier holds the pinned set of n keys,
- **Then** the verifier accepts it only with at least t valid signatures from distinct pinned keys, and logs n and t,
- **And** a test with t−1 valid signatures observes refusal, and so does a test with t signatures of which two come from one key,
- **And** a test where one signer's key is compromised and signs an altered list observes refusal (t ≥ 2),
- **And** the document records n, t, and each signer's operating entity and jurisdiction,
- **And** the report says plainly that forging needs t compromised signers and issuing needs t available signers, and it makes no Byzantine-fault-tolerance claim,
- **And** signer rotation and removal are specified, and whether one signer may withdraw a circuit alone (fail-safe) is decided and recorded in an ADR addendum,
- **And** the Story 5.4 two-instance agreement test runs against a t-of-n document.

**Covers:** FR76. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon Axioms 6 and 10; its audit finding X3 on what a quorum does and does not tolerate). **Depends on:** 5.1, 5.2, 5.4. **Decision (maintainer, 2026-11-04):** the first tests and the report use synthetic keys with a 2-of-3 default, `t` and `n` parameterised, and the fixtures also run one other pair (for example 3-of-5) to show both are parameters. **Decision gate:** naming real independent signers stays a maintainer and community decision, and no institution or community has agreed to sign; until then synthetic keys prove the mechanism only and the report says one operator holds every key.

### Story 13.3: Qualified-timestamped evidence reports

As a third party relying on a published report,
I want each published evidence report to carry an EU qualified electronic time stamp,
So that "this report existed in this form on this date" is checkable without trusting the repository host.

**Acceptance Criteria:**

- **Given** the published AV report (Story 1.6), carrier result (Story 2.4) and measurement report (Story 6.6),
- **When** a report is published,
- **Then** its hash is time-stamped by a qualified trust service provider, SK ID Solutions AS (SK-TSA), chosen 2026-11-04, and the token is published beside the report,
- **And** an offline script verifies the token against the report and the provider's certificate chain and prints pass or fail, with a test on a deliberately altered report,
- **And** the story records the price and the issuance time found, and re-checks the provider's qualified status and the list's signature on the date of first use; on 2026-11-04 the Estonian trusted list (sequence 73, issued 2026-05-20, next update 2026-11-20) showed granted qualified time-stamp services for SK ID Solutions AS, read from a rendered copy with the list signature not verified, and price and issuance time stay `[UNVERIFIED]` until recorded,
- **And** the legal effect claimed is only what Regulation (EU) 2024/1183 (amending Regulation (EU) No 910/2014) gives, cited from the primary text at execution time (`[UNVERIFIED]` here),
- **And** a time stamp evidences date and integrity only: no report calls itself certified or compliant (NFR1),
- **And** the timestamp does not come from the hosting repository, so repository metadata is never the anchor.

**Covers:** FR77. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon verification-practice plan, correction 0.1: a qualified seal and timestamp from an EU provider instead of a public transparency log). **Depends on:** 1.6 (the mechanism can be proven on that report; 2.4 and 6.6 follow as they publish). **Decision:** provider chosen (maintainer, 2026-11-04); cost stays open.

## Epic 14: Test honesty

Epic 7 makes the documents honest. This epic does the same for the tests. The zero-trust-octagon audit
found that its 89 passing tests proved little, because they mocked an API shape the installed library no
longer used, and that a revocation key was written but never read, so revocation silently did nothing
(`AUDIT_REPORT_2026-10-04.md`, findings C-1 and X8). This backlog has the same exposure: it leans on
mock-to-real seams (`architecture.md`) and on fail-closed claims in Stories 3.3, 4.5, 10.4 and 12.3.
Source: zero-trust analysis 2026-11-04.

### Story 14.1: Mock-drift contract tests

As a maintainer who relies on mocks for the RP-register, issuer, Wallet Unit Attestation and status list,
I want each mock shown equivalent to its real counterpart on a shared case set,
So that a green suite does not prove only that a mock agrees with itself.

**Acceptance Criteria:**

- **Given** each mock-to-real seam listed in `architecture.md`, named in this story with its file,
- **When** one case set runs against the mock and against the real client, a recorded fixture or the vendored library's own types,
- **Then** both give the same outcome for every case, refusal outcomes included,
- **And** a deliberate change to a mock's shape (a renamed field) fails the contract test,
- **And** a seam with no real counterpart yet is listed as such with the story that will supply it (3.3, 10.1, 10.3, 10.4), never skipped silently,
- **And** the suite is catalogued in `tests/README.md` and wired into `make check` or the matching CI job.

**Covers:** FR78. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon audit finding C-1). **Depends on:** none for the seams that exist today; each later seam adds its cases when its story lands.

### Story 14.2: Mutation guard for fail-closed paths

As a reviewer of fail-closed claims,
I want each fail-closed guard shown necessary by removing it,
So that a guard no test reads cannot ship as protection.

**Acceptance Criteria:**

- **Given** the named fail-closed guards that exist when the story runs (circuit allowlist, `stale-list`, register fail-closed, certificate-absent default, plain-fallback guard, revocation check, consent lockout),
- **When** a scripted mutation removes or inverts a guard,
- **Then** at least one test fails,
- **And** CI lists the surviving mutants per guard, and a survivor is a recorded finding in `_bmad-output/implementation-artifacts/deferred-work.md` or a story, never a pass,
- **And** the tooling per language (Kotlin, Python, Go, Rust) is chosen in the story with its reason, and where no tool fits, hand-written mutation scripts over a guard list are acceptable,
- **And** the guard list lives in one file, so a new fail-closed claim is added together with its mutation.

**Covers:** FR79. **Source:** zero-trust analysis 2026-11-04 (zero-trust-octagon audit finding X8). **Depends on:** 14.1 is optional; each guard is covered once its owning story exists.

## Sequencing and recorded decisions

**Epic numbering follows the order the work appears in the corpus, not a single ranking.** Epics
1–5 are Report 1's opportunities by score, Epic 6 is Appendix A3's D2/M3, and Epics 7–11 are
Report 2's recommendations and the remaining alignment and production items. The numbers are a
stable handle for traceability; the execution order below is the ordering signal.

**Execution order.**

1. **Epic 7 Stories 1–4 come first as cheap prerequisites.** A corrected stale row and honest vocabulary
   cost almost nothing and are preconditions for showing any artefact to an evaluator; they run alongside
   the start of Epic 1, not after it.
2. **Epic 6 runs in parallel from the start.** It gates every performance claim but blocks no other epic.
3. **Epic 14 and Story 13.3 join the cheap prerequisites (added 2026-11-04).** Neither needs an external party. Then Story 4.8 and Story 13.2, then Story 13.1 once Stories 5.6 and 10.3 supply its inputs, then Stories 3.9, 6.7 and 8.10. None of these displaces Stories 2.1, 2.2 or Epic 7 Stories 1–4. Decisions taken 2026-11-04 with the tiltrank rubric: Story 3.9 stays with an NFR14 criterion, Story 13.1's claim text is fixed, Story 13.2 starts on synthetic keys at 2-of-3, and Story 13.3 uses SK ID Solutions AS. Gates still open: real independent signers for Story 13.2, counsel review before Story 13.1 is published, and the price and turnaround of Story 13.3.

**Primary sequence, as the source documents order it.** Epic 1 (O1, prepared first: it converts
existing code into an inspectable AV-Profile evidence kit; local preparation needs no external
participant or device, while Stories 1.3 and 1.6 require independent candidate and reproduction
evidence plus publication before completed acceptance),
then Epic 2 (O5, second: the tests exist), then Epic 3 (O2, third: the only recommendation a buyer can act
on before 2028). Epic 3's interface proposal (Story 3.4) is pulled as early as possible within Epic 3,
because an awarded procurement would otherwise set the technical direction first.

**Then:** Epic 4 and Epic 5 (the second wave; both reduce risk in others' integrations), Epic 9 after
Epic 5 and after a scheme owner's acceptance decision, and Epics 10 and 11 once a pilot relying party
exists. The sprint tracking file's own recommendation — start the first backlog story, `1-1-av-profile-clause-registry`
— matches this primary sequence.

**Recorded decisions from Appendix A6 (tradeoffs).**

- Stay narrow on ZKP scope and say so (Story 7.8) rather than extending toward the general ARF vision.
- Keep the spec's `eu.europa.ec.av.1`-first ordering, keep both paths, and let the programme choose (Story 7.9).
- Register integration is interface-first with mocks, a bounded cache, and an explicit logged stale-data
  decision — never a silent allow (Stories 3.3, 3.4, 6.5).
- Publish the carrier suite result first and hold performance claims until the device programme completes
  (Stories 2.4, 6.1, 6.6).
- Finish the register interface proposal early so it is on the table rather than reconstructed (Story 3.4).

**Cross-epic file churn considered and kept.** Epics 1, 2 and 8 all touch `verifier/go/oid4vp` and
`tests/vectors/`; Epics 4 and 7 both touch `app/.../domain/presentation/`; Epics 3, 4 and 6 all touch the
register path. The splits are kept because each epic has a distinct exit criterion and a distinct
feedback loop (a published clause report, a published external run, a certificate-policy decision),
and because consolidating them would produce one epic too large to complete or review in one pass.

**Deliberately not made into stories.** Three classes.

**Requirement rows with no required change**, where a story would be invented work: `matrix L1`
(wallet availability — not a fork duty), `matrix L3` (wallet certification — a provider duty, and
the reason the vocabulary rule FR33 exists), `matrix A5` (`ZKP_02`, already supported with
existing tests), `matrix A15` (`WPSM_01` and following — an operator duty), `matrix S1` (OpenID4VP
1.0, supported with committed transcript vectors), `matrix S5` (ISO/IEC 18013-5:2021, supported),
`matrix S13` (TS4's maturity position — context, not a requirement), `matrix E4` (isikukood
minimisation, already the implementation's central premise and covered by existing negative tests)
and `matrix E6` (the Estonian launch date and unawarded procurement — context). `matrix A11`
(`ZKP_08`) carries no engineering change either; it becomes Story 8.1, because the corpus's own
recommendation is to report the tension in writing rather than engineer around it.

**Specification requirements this backlog does not carry.** `spec/EE-EUDIW-TS-1.0.md` is an input
as *evidence*, not as a source of stories: it supplies the identifiers, budgets and divergence
records the stories cite. Its Annex E records 126 of 211 requirements covered by the repository
and 85 uncovered. Of those 85, the ones that touch this repository's own behaviour are out of
scope here and named so the omission is a decision rather than a gap — `EE-ZKP-062` (validity
judged on the relying party's clock, ±60 s), `EE-REV-007` (relying-party status-cache age),
`EE-PRO-025` (`readerAuth` anchors, carried only as the condition on `EE-ZKP-055a` in Stories 3.5 and 4.7) and `EE-ZKP-043` (platform baseline). Closing them belongs in a follow-on backlog
derived from Annex E.

**Course-of-action items that are decisions rather than buildable work:** which clauses are in the
initial AV release (Story 1.1 records them), which external implementation to test first (Story
2.4 records the choice), and whether RIA will expose a register interface or CT-log access
certificates (recorded as unresolved in Stories 3.4 and 11.6).

## Open questions carried into the sprint

These are recorded as open rather than converted into stories, because they are external or regulatory:

- Has the Estonian procurement been awarded? No announcement was found; the tender register could not be
  read. `[UNVERIFIED]` — no story can close it, and Story 3.4 is deliberately ordered to survive either answer.
- What will the RP-register API look like, and will access certificates be CT-logged? Tracked at Stories 3.4 and 11.6.
- Will RIA adopt the EU AV Profile for age checks, the fork's `ee.riik.poa.1`, or both? Story 7.9 records both paths.
- Will ISO/IEC 18013-5 second edition ship the structures the carrier approximates, and when? Tracked at Story 8.2.
- Would an Estonian service be permitted to accept a foreign AV attestation before the Estonian register exists?
  A regulatory question; noted at Story 3.5 rather than answered.
- Is the €21.65 M procurement value official? It appears only in sponsored press and must never be presented as official.

