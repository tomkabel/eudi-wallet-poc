---
status: drafted
prepared: 2026-10-23
scope: analysis and story writing only
stories: 80
epics: 12
existing_detailed_specs: 6
new_story_drafts: 74
implementation_performed: false
inputDocuments:
  - _bmad-output/planning-artifacts/epics.md
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md
  - _bmad-output/planning-artifacts/ux-designs/ux-eudi-wallet-poc-2026-10-23/EXPERIENCE.md
  - _bmad-output/implementation-artifacts/sprint-status.yaml
  - _bmad-output/implementation-artifacts/epic-1-retro-2026-10-23.md
  - AGENTS.md
---

# Story analysis and preparation — 23 October 2026

All **80 existing stories across 12 epics** now have a detailed planning record:
the six existing Epic 1 specs and 74 new individual drafts under the configured
`implementation-artifacts/stories/` directory. Each new file preserves its
authored story and acceptance criteria, adds specific work boundaries and
verification cases, identifies existing entry points and proposed outputs, and
records prerequisites and unresolved decisions. The [shared preparation contract](../implementation-artifacts/stories/README.md)
defines execution evidence and constraints. The [JSON manifest](story-analysis-2026-10-23.json)
provides the same inventory with source hashes and dependency metadata.

This is a preparation pass. No implementation, fixture regeneration, application
test, benchmark, external assessment, deployment, publication or external message
was performed. Sprint statuses, existing specs, retrospective action statuses,
canonical epics, the PRD and the specification were preserved. New files remain
drafts; they are not a blanket `ready-for-dev` decision. All 74 unfinished stories
outside Epic 1 retain `backlog` status. Any historical implementation/test outcome
quoted below belongs to its cited record, not a fresh run.

## BMad position

`bmad-help` resolves this project to BMad Method. The PRD and architecture are
marked final, the epics document has all four steps complete, and sprint tracking
already exists. The UX experience spine is a draft. The next useful planning
operation is refinement of the existing stories, which this pass supplies; a new
requirements-discovery ceremony is unnecessary to write these drafts.

The backlog's original “no architecture exists” concern is stale:
[architecture.md](architecture.md) and ADR-004 through ADR-008 now establish the
RP deployment, certificate model, circuit-list format, register interface and
carrier placement. Their existence closes the missing-record concern, but does
not resolve every cross-document conflict below. This pass therefore does not
flip the old global gate to PASS.

For a later session, optional **[RV] Review**, `bmad-review`, can review this
analysis or selected drafts. The existing required **[CE] Create Epics and
Stories**, `bmad-create-epics-and-stories`, and **[SP] Sprint Planning**,
`bmad-sprint-planning`, already have completed artifacts; revise/revalidate them
when decisions change the contract. **[BD] Build**, `bmad-build`, is the next
implementation workflow only when implementation is requested. Start each later
workflow in a fresh context with a specific story path and this analysis.

## Existing Epic 1 evidence and follow-through

The tracker records Stories 1.1, 1.2, 1.4 and 1.5 as `done`; Stories 1.3 and 1.6
remain `awaiting-operator`. The [retrospective](../implementation-artifacts/epic-1-retro-2026-10-23.md)
records a **rejected** epic acceptance verdict, despite the retrospective itself
being `done`. Its local audit retained three actual policy failures and missing
independent evidence. A green runner test suite is not a passing AV audit.

Retain the retrospective's existing action ids rather than creating duplicate
stories or pretending the actions were completed:

| Existing action id suffix (`epic-1-retro-…`) | Owning record / future acceptance evidence |
|---|---|
| `independent-candidate` | Story 1.3: independently built candidate, revision, actual adapter results and clause evidence |
| `independent-reproduction` | Story 1.6: authorized publication through `fork`, independent clean-checkout execution, timing, transcript, artifacts and discrepancy review |
| `adapter-text` | Story 1.3 runner hardening: malformed output bytes yield reportable ERROR rather than an uncaught decode exception |
| `adapter-duplicate-keys` | Story 1.3: conflicting raw JSON keys cannot overwrite FAIL with PASS |
| `adapter-timeout-cleanup` | Story 1.3: timeout terminates and reaps descendants; delayed child-marker regression |
| `adapter-startup-state` | Story 1.3: nonexistent candidate is NOT_EXECUTED, distinguishable from executed-candidate ERROR |
| `success-verdict-test` | Stories 1.3/1.6: all-passing synthetic control proves exit code/report agreement without claiming independent evidence |
| `external-dependency-wording` | Canonical epic overview/sequence later needs to distinguish local kit preparation from externally gated acceptance |

Epic 2's runner draft carries these adapter lessons forward. None of these
actions was implemented or marked complete here. `epic-1: backlog` is also stale
relative to delivered stories; its owning lifecycle must reconcile it separately.

## Decisions and evidence gaps carried by the drafts

These are planning findings. A “gate” applies to the affected behavior or claim,
not every preparatory task in the story. Roles below are suggested decision
owners, not accepted personal assignments. External claims that were not
refreshed against primary sources in this pass remain **[UNVERIFIED]**.

| Gate | Affected stories | Finding and required resolution |
|---|---|---|
| G1 | 3.3, 3.4, 6.5; dependent acceptance cases | Story 3.3 says an unreachable register refuses. ADR-007 explicitly permits an allow from a within-TTL cached entry during an outage. Record one consistent contract, including exact TTL boundary and log state, before implementing the disputed allow. The ADR's choice is explicit, but canonical story wording needs reconciliation. Suggested owner: architect/product owner. |
| G2 | 4.2–4.6 | Pin a concrete certificate profile, comparable version label, source gitlink and authenticated binding to the requesting RP. A local typed model does not prove a national certificate profile exists. Start with a synthetic, clearly labeled fixture profile if national material is unavailable. Suggested owner: architect. |
| G3 | 4.2, 4.3 and RP integration | ADR-005 requires refusal when certificate and register disagree, while forbidding a wallet register lookup. Specify how authenticated register comparison evidence reaches the decision, who supplies it and how identity/freshness are bound. Do not add a wallet timing-leaking network lookup by implication. Suggested owner: architect/security reviewer. |
| G4 | 3.5 | The blanket “unattested ZK request refuses” case is underspecified. EE-ZKP-055a requires Tier 2 for an unauthenticated age request and refusal of **plain-only** requests on capable devices; authentication absence alone is not the same as missing registration or unsupported proof. Name which prerequisite each case violates and align expected results. Suggested owner: product owner/architect. |
| G5 | 5.1–5.3, 9.2 | ADR-006 compares minimum-library versions with consumer stamps, including a Git revision and a wallet artifact version. Git hashes have no meaningful less-than ordering. Define a comparable version/compatibility registry or explicit accepted-stamp mapping and failure behavior. Suggested owner: architect. |
| G6 | 5.1, 5.3 | A presentation must name the circuit-list version, but the carrier/transcript placement and authenticated binding of that version are unspecified. Define the extension and its downgrade/skew behavior before adding a field. Preserve current carrier adaptation points and scope guards. Suggested owner: architect. |
| G7 | 5.2, 5.3, 5.6 | Define invalid-remote-document versus unavailable-network fallback precedence, rollback handling and expiry for both fetched and local documents. ADR-006 already requires fallback expiry; a signed old document cannot be assumed acceptable forever. Suggested owner: architect/operator. |
| G8 | 6.1, 6.2, 6.4 | Pin the target hardware/sample protocol and calibrate refusal bounds. Five cold runs cannot support a strong p99 claim. Capacity reporting must retain the specification's separate 1.0 s p95 verifier bound and explicitly report a miss rather than reinterpreting it away. Suggested owner: measurement lead. |
| G9 | 6.3 | PRD NG-6 says proximity Tier-2 ZK is absent and out of v1 scope. Measuring a real path needs that path first; do not create a transport implementation inside a measurement story without a recorded scope decision. Physical transfer evidence remains pending. Suggested owner: product owner/architect. |
| G10 | External clause maps, standards watch, comparisons, issuer/device studies | Revalidate the actual primary document editions, legal provisions, institutional claims and live dependency pins before publishing present-tense findings. Preserve unsupported dates/identifiers as [UNVERIFIED]. A local draft does not refresh the standards landscape. Suggested owner: document author/reviewer. |
| G11 | 8.3, 8.8 | Feasibility and dispatcher isolation do not authorize adding zk-jwt, BBS/MSS or another runtime proof family. Record a separate PRD scope decision for such an implementation. Syntax round-trip and cryptographic proof round-trip must be distinguished. Suggested owner: product owner/architect. |
| G12 | 9.1–9.6 | The epic's 16/21 age-gate premise conflicts with PRD NG-10 and the spec's identification-duty distinctions. Treat beyond-18 thresholds as unsupported until primary legal mapping, review and circuit acceptance exist. Deferred planning is permitted; enabling thresholds is not implied. Suggested owner: legal reviewer/product owner. |
| G13 | 9.3, 9.5 | “Nearest true above” and “never a neighbouring threshold” need one selection table specifying authorized negotiation versus forbidden substitution. Do not silently implement contradictory rules. Preserve false withholding and the registered two-predicate exception. Suggested owner: product owner/architect. |
| G14 | 10.4 | Credential status lists and certificate CRLs have different semantics. PRD NG-11 says the age attestations carry no status mechanism. Choose a suitable revocable credential and keep CRL soft-fail separate from credential-status policy. Suggested owner: architect. |
| G15 | 10.5, 12.1–12.4 | Several audit-era premises are stale: PIN state is persisted, keys already have platform-authentication configuration, and proximity already asks for PIN. Target actual remaining enforcement, UI and reuse gaps. Confirm escalation/recovery language, DC API gate coverage and requester's trust state without replacing existing hardening. Suggested owner: security/UX maintainer. |
| G16 | 11.2, 11.4 | A sponsor/operator must choose SLO window/target, incident severities/roles, representative review deployment and release policy. Draft the configuration and review package without inventing a service commitment or external assessment. Suggested owner: service owner. |
| G17 | 11.6 | Pin the CT protocol/profile, log trust policy, SCT/inclusion distinction and outage behavior. National CT adoption remains unresolved. Synthetic vectors cannot establish real log inclusion. Suggested owner: architect/security reviewer. |
| G18 | 11.7 | ADR-004 excludes `-multi-credentials` from the supported RP deployment profile, while onboarding needs a multi-credential session. Decide whether this is an isolated acceptance harness or an approved profile extension before claiming deployable cross-border onboarding. Suggested owner: architect/product owner. |

## Verified local premises that change the work

The following are source-inspection findings from this pass; they are not fresh
runtime validation:

- [PinVerifier.kt](../../app/src/main/kotlin/ee/cyber/wallet/security/PinVerifier.kt)
  carries the five-attempt/30-minute lock in `PinRecord`.
  [UserSessionDataSource.kt](../../app/src/main/kotlin/ee/cyber/wallet/data/datastore/UserSessionDataSource.kt)
  persists verification/counters through `DataStore.updateData` before returning.
  Story 12.3 should reconcile the UI's separate counter and artificial delay,
  not build a second persistent verifier.
- [ProximityViewModel.kt](../../app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityViewModel.kt)
  checks authentication before its share path. Story 12.4 needs gate-coverage,
  session-reuse and consent-content verification, rather than assuming no gate.
- [DeviceRequestParser.kt](../../app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt)
  separately computes signature authentication and trusted-chain state.
  `readerAuthSubject` selects signature-authenticated requests, which is not
  enough for a verified requester claim. Story 12.1 must propagate both trust
  inputs to display consumers.
- [SecureAreaKeyManager.kt](../../app/src/main/kotlin/ee/cyber/wallet/security/SecureAreaKeyManager.kt)
  already configures user authentication. Story 12.2 must audit operation-bound
  enforcement across proof/plain/signing/export paths, including failure and
  stale-authentication behavior; configuration alone does not establish coverage.
- Epic 1's Story 1.5 records batch-issuer rollback and cancellation tests.
  Story 10.2 starts by checking that existing batch implementation, rather than
  duplicating a loop because the research once described batch issuance as absent.
- The exact old 169-test count is historical. The retrospective records a later
  212-test execution; this pass runs neither suite. Use actual current execution
  evidence at delivery time, while retaining upstream regression coverage.

## Suggested sequencing for later work

1. Keep Stories 1.3/1.6 and their existing operator actions open. Review the
   adapter-hardening follow-ups before trusting new external output.
2. Prepare/review 7.1, 7.2, 7.4, 7.8 and 7.11 early to prevent unsupported
   claims. 7.3 follows the row audit; primary-source clause maps can proceed
   independently when their sources are available.
3. Treat Epic 12 as a v1 security requirement, not a low-priority consequence
   of its number. Begin current-path analysis for 12.1/12.2/12.3 independently;
   complete 12.4 after their shared trust, presence and lockout behavior is clear.
4. Follow 2.1 → 2.2/2.3 → 2.4. Local runner development can precede independent
   candidate availability; full external claims wait for that evidence.
5. For the RP kit, draft 3.1 and 3.6, resolve G1 for 3.3, and finalize 3.4
   from the shared model/mock. TARA 3.2 needs actual access. Stage 3.5's base
   cases, then its Epic 5 offline-registry case; 3.7 waits for the resulting
   reproducible cases and three integrators. 3.8 can be written independently.
6. For certificate policy, use **4.1 → 4.6 → 4.5 → 4.2 → 4.3/4.4**.
   Establish rejecting-interface properties first, then rerun them against the
   parser when it exists. This avoids a parser/test dependency cycle. 4.7's
   unauthenticated-transport guard can be analyzed independently.
7. For registry work, resolve G5–G7, then 5.1 → 5.2 → 5.3/5.5; 5.4 exercises
   shared resolution and 5.6 needs a real host/custodian. Keep provenance bounded
   to process lifetime as ADR-006 decides, despite the story's months-later prose.
8. Draft measurement protocols early. Execute 6.1/6.2 only on named hardware,
   6.4 after bound calibration, 6.5 after register behavior is fixed; keep 6.3
   behind G9. 6.6 governs reporting throughout.
9. Keep Epic 9 as deferred planning until G12/G13 and external acceptance are
   resolved. Independent feasibility/watch notes in Epic 8 are continuous work.
10. Real issuance/WUA/revocation and device security evidence in Epic 10, followed
    by pilot operations/reviews in Epic 11, require their actual services/devices
    and sponsor. Preparatory documents are useful before those inputs arrive.

The dependency graph in the manifest is acyclic. It models full acceptance;
staged preparation described above prevents unnecessarily serializing drafts.
This is an ordering recommendation, not a schedule or effort commitment.

## Coverage and canonical-document follow-ups

Every existing story id is accounted for once and matches its sprint key. This
does **not** mean every PRD requirement has a complete story or every normative
specification requirement is delivered. The backlog deliberately excludes many
specification obligations; its existing out-of-scope decisions remain intact.

Canonical prose still needs a later reconciliation: the malformed indentation
in `epics.md`'s `inputDocuments` list; stale story/count and test-count claims;
the “no architecture/UX exists” paragraphs; the “no external party” Epic 1
overview/sequence; duplicate sentence fragments in Stories 2.1 and 9.4; and the
specific G1/G4/G12/G13 contradictions above. These drafts preserve the source
contract rather than rewriting those decisions by implication. UX notes also
contain superseded “no Epic 12” statements and should not be read as the current
backlog inventory.

The PRD's degraded-boot surface (FR-34) and the draft UX's full sandbox behavior
are not clearly decomposed into dedicated backlog stories. Story 3.6 covers the
typed outcome boundary, but should not silently absorb every sandbox feature.
Verify current behavior and make an explicit coverage decision in a later
backlog reconciliation; no new feature id is invented by this pass.

## Complete story index

The table below links all 80 records. `Decision` names a gate from the analysis;
`external` means an external boundary is described in the story (which may be a
future claim limitation, not a blocker for drafting). Exact gates and staged
work are in each record. Existing Epic 1 records retain their delivery history.

| Story / detailed record | Delivery status | Acceptance prerequisites | Decision / external boundary |
|---|---|---|---|
| [1.1 AV Profile clause registry](../implementation-artifacts/spec-1-1-av-profile-clause-registry.md) | `done` | — | — |
| [1.2 AV-profile fixtures](../implementation-artifacts/spec-1-2-av-profile-fixtures.md) | `done` | 1.1 | — |
| [1.3 AV conformance runner](../implementation-artifacts/spec-1-3-av-conformance-runner.md) | `awaiting-operator` | 1.1, 1.2 | external |
| [1.4 av-vci:// invocation](../implementation-artifacts/spec-1-4-av-vci-invocation.md) | `done` | — | — |
| [1.5 AVSUA-equivalent issuance path](../implementation-artifacts/spec-1-5-avsua-equivalent-issuance-path.md) | `done` | — | — |
| [1.6 Published AV report and external reproduction](../implementation-artifacts/spec-1-6-published-av-report-and-external-reproduction.md) | `awaiting-operator` | 1.3 | external |
| [2.1 Carrier runner over existing cases](../implementation-artifacts/stories/2-1-carrier-runner-over-existing-cases.md) | `backlog` | — | external |
| [2.2 Binding-property failure naming](../implementation-artifacts/stories/2-2-binding-property-failure-naming.md) | `backlog` | 2.1 | — |
| [2.3 Version-labelled carrier fixtures](../implementation-artifacts/stories/2-3-version-labelled-carrier-fixtures.md) | `backlog` | 2.1 | — |
| [2.4 External carrier run and published result](../implementation-artifacts/stories/2-4-external-carrier-run-and-published-result.md) | `backlog` | 2.1, 2.2, 2.3 | external |
| [3.1 Deployable RP service profile](../implementation-artifacts/stories/3-1-deployable-rp-service-profile.md) | `backlog` | — | — |
| [3.2 TARA-facing adapter contract](../implementation-artifacts/stories/3-2-tara-facing-adapter-contract.md) | `backlog` | 3.1 | external |
| [3.3 Fail-closed RP-register interface and mock](../implementation-artifacts/stories/3-3-fail-closed-rp-register-interface-and-mock.md) | `backlog` | — | G1, external |
| [3.4 RP-register interface proposal](../implementation-artifacts/stories/3-4-rp-register-interface-proposal.md) | `backlog` | 3.3 | G1, external |
| [3.5 Four Estonian acceptance cases](../implementation-artifacts/stories/3-5-four-estonian-acceptance-cases.md) | `backlog` | 3.1, 3.3, 3.6, 4.7, 5.2, 5.3 | G4, external |
| [3.6 Refusal distinguishable from cancellation](../implementation-artifacts/stories/3-6-refusal-distinguishable-from-cancellation.md) | `backlog` | — | — |
| [3.7 Integrator README and timed onboarding](../implementation-artifacts/stories/3-7-integrator-readme-and-timed-onboarding.md) | `backlog` | 3.1, 3.5 | external |
| [3.8 Parallel-path integration note](../implementation-artifacts/stories/3-8-parallel-path-integration-note.md) | `backlog` | — | — |
| [4.1 IntendedUseSource interface](../implementation-artifacts/stories/4-1-intendedusesource-interface.md) | `backlog` | — | — |
| [4.2 Registration-certificate parsing](../implementation-artifacts/stories/4-2-registration-certificate-parsing.md) | `backlog` | 4.1, 4.6, 4.5 | G2, G3, external |
| [4.3 Certificate-driven two-predicate decision](../implementation-artifacts/stories/4-3-certificate-driven-two-predicate-decision.md) | `backlog` | 4.2, 4.5 | G2, G3, external |
| [4.4 Topic 19 log fields from the certificate](../implementation-artifacts/stories/4-4-topic-19-log-fields-from-the-certificate.md) | `backlog` | 4.2 | — |
| [4.5 Fail-closed defaults and certificate property tests](../implementation-artifacts/stories/4-5-fail-closed-defaults-and-certificate-property-tests.md) | `backlog` | 4.1, 4.6 | G2, external |
| [4.6 Certificate fixture generator](../implementation-artifacts/stories/4-6-certificate-fixture-generator.md) | `backlog` | 4.1 | G2, external |
| [4.7 Plain-fallback visibility guard](../implementation-artifacts/stories/4-7-plain-fallback-visibility-guard.md) | `backlog` | — | — |
| [5.1 Signed circuit-list document and generator](../implementation-artifacts/stories/5-1-signed-circuit-list-document-and-generator.md) | `backlog` | — | G5, G6, external |
| [5.2 Verifier fetch with pinned-key validation](../implementation-artifacts/stories/5-2-verifier-fetch-with-pinned-key-validation.md) | `backlog` | 5.1 | G5, G7, external |
| [5.3 Wallet scheme resolution from the registry](../implementation-artifacts/stories/5-3-wallet-scheme-resolution-from-the-registry.md) | `backlog` | 5.1, 5.2 | G5, G6, G7, external |
| [5.4 Tamper and two-instance agreement tests](../implementation-artifacts/stories/5-4-tamper-and-two-instance-agreement-tests.md) | `backlog` | 5.2, 5.3 | external |
| [5.5 Circuit provenance audit query](../implementation-artifacts/stories/5-5-circuit-provenance-audit-query.md) | `backlog` | 5.2 | — |
| [5.6 Registry hosting and succession path](../implementation-artifacts/stories/5-6-registry-hosting-and-succession-path.md) | `backlog` | 5.1, 5.2 | G7, external |
| [6.1 On-device proving measurements](../implementation-artifacts/stories/6-1-on-device-proving-measurements.md) | `backlog` | — | G8, external |
| [6.2 Verification capacity on target hardware](../implementation-artifacts/stories/6-2-verification-capacity-on-target-hardware.md) | `backlog` | — | G8, external |
| [6.3 Proximity BLE transfer measurement](../implementation-artifacts/stories/6-3-proximity-ble-transfer-measurement.md) | `backlog` | 6.1 | G9, external |
| [6.4 Verification budget and refusal-cost regression](../implementation-artifacts/stories/6-4-verification-budget-and-refusal-cost-regression.md) | `backlog` | 6.2 | G8, external |
| [6.5 Register latency and availability profile](../implementation-artifacts/stories/6-5-register-latency-and-availability-profile.md) | `backlog` | 3.3 | G1, external |
| [6.6 Honest measurement reporting](../implementation-artifacts/stories/6-6-honest-measurement-reporting.md) | `backlog` | — | — |
| [7.1 Fix the stale issuerAuth conformity row](../implementation-artifacts/stories/7-1-fix-the-stale-issuerauth-conformity-row.md) | `backlog` | — | — |
| [7.2 Audit remaining hand-maintained conformity rows](../implementation-artifacts/stories/7-2-audit-remaining-hand-maintained-conformity-rows.md) | `backlog` | 7.1 | — |
| [7.3 Test-derived conformity statuses](../implementation-artifacts/stories/7-3-test-derived-conformity-statuses.md) | `backlog` | 7.2 | — |
| [7.4 Evidence-scoped compliance and estimate vocabulary](../implementation-artifacts/stories/7-4-evidence-scoped-compliance-and-estimate-vocabulary.md) | `backlog` | — | — |
| [7.5 HAIP 1.0 clause statement](../implementation-artifacts/stories/7-5-haip-1-0-clause-statement.md) | `backlog` | — | G10, external |
| [7.6 TS13 clause map](../implementation-artifacts/stories/7-6-ts13-clause-map.md) | `backlog` | — | G10, external |
| [7.7 ECCG clause map and algorithm inventory](../implementation-artifacts/stories/7-7-eccg-clause-map-and-algorithm-inventory.md) | `backlog` | — | G10, external |
| [7.8 Narrow-scope statement for the ZKP work](../implementation-artifacts/stories/7-8-narrow-scope-statement-for-the-zkp-work.md) | `backlog` | — | — |
| [7.9 Attestation ordering statement](../implementation-artifacts/stories/7-9-attestation-ordering-statement.md) | `backlog` | — | — |
| [7.10 National-wallet production comparison](../implementation-artifacts/stories/7-10-national-wallet-production-comparison.md) | `backlog` | — | G10, external |
| [7.11 Estimate method discipline](../implementation-artifacts/stories/7-11-estimate-method-discipline.md) | `backlog` | — | — |
| [8.1 ZKP_08 and ECCG tension report](../implementation-artifacts/stories/8-1-zkp_08-and-eccg-tension-report.md) | `backlog` | 7.7 | G10, external |
| [8.2 ISO 18013-5 second-edition watch](../implementation-artifacts/stories/8-2-iso-18013-5-second-edition-watch.md) | `backlog` | 2.3 | G10, external |
| [8.3 zk-jwt path feasibility](../implementation-artifacts/stories/8-3-zk-jwt-path-feasibility.md) | `backlog` | 7.6 | G11, external |
| [8.4 Per-path unlinkability statement](../implementation-artifacts/stories/8-4-per-path-unlinkability-statement.md) | `backlog` | — | — |
| [8.5 LoA-High authentication with a ZKP presentation](../implementation-artifacts/stories/8-5-loa-high-authentication-with-a-zkp-presentation.md) | `backlog` | 3.2 | external |
| [8.6 DCHP #17 filing and issue tracking](../implementation-artifacts/stories/8-6-dchp-17-filing-and-issue-tracking.md) | `backlog` | — | external |
| [8.7 Missing ZKP_03 ZKP_04 ACP_03 design notes](../implementation-artifacts/stories/8-7-missing-zkp_03-zkp_04-acp_03-design-notes.md) | `backlog` | 7.8 | — |
| [8.8 Second ZK family readiness](../implementation-artifacts/stories/8-8-second-zk-family-readiness.md) | `backlog` | 2.1 | G11, external |
| [8.9 Upstream-dormancy maintenance position](../implementation-artifacts/stories/8-9-upstream-dormancy-maintenance-position.md) | `backlog` | — | G10, external |
| [9.1 Statutory threshold-to-circuit map](../implementation-artifacts/stories/9-1-statutory-threshold-to-circuit-map.md) | `backlog` | — | G12, external |
| [9.2 Accepted circuit set per threshold](../implementation-artifacts/stories/9-2-accepted-circuit-set-per-threshold.md) | `backlog` | 5.1, 5.2, 9.1 | G12, G5, external |
| [9.3 Nearest-true-above negotiation library](../implementation-artifacts/stories/9-3-nearest-true-above-negotiation-library.md) | `backlog` | 4.3, 9.1, 9.2 | G12, G13, external |
| [9.4 Threshold boundary tests](../implementation-artifacts/stories/9-4-threshold-boundary-tests.md) | `backlog` | 9.1, 9.2, 9.3 | G12, G13, external |
| [9.5 Refusal with a user-visible reason](../implementation-artifacts/stories/9-5-refusal-with-a-user-visible-reason.md) | `backlog` | 9.3 | G13, external |
| [9.6 Legal review of the threshold list](../implementation-artifacts/stories/9-6-legal-review-of-the-threshold-list.md) | `backlog` | 9.1 | G12, external |
| [10.1 Real issuer integration](../implementation-artifacts/stories/10-1-real-issuer-integration.md) | `backlog` | — | G10, external |
| [10.2 Batch issuance](../implementation-artifacts/stories/10-2-batch-issuance.md) | `backlog` | 8.4 | external |
| [10.3 Real Wallet Unit Attestation issuance](../implementation-artifacts/stories/10-3-real-wallet-unit-attestation-issuance.md) | `backlog` | — | G10, external |
| [10.4 Revocation end-to-end with a revoked fixture](../implementation-artifacts/stories/10-4-revocation-end-to-end-with-a-revoked-fixture.md) | `backlog` | — | G14, external |
| [10.5 Mobile-device security test matrix](../implementation-artifacts/stories/10-5-mobile-device-security-test-matrix.md) | `backlog` | — | G10, G15, external |
| [11.1 Operations runbook](../implementation-artifacts/stories/11-1-operations-runbook.md) | `backlog` | 3.1 | — |
| [11.2 Monitoring availability and incident process](../implementation-artifacts/stories/11-2-monitoring-availability-and-incident-process.md) | `backlog` | 3.1, 3.3 | G16, external |
| [11.3 Accessibility conformance audit](../implementation-artifacts/stories/11-3-accessibility-conformance-audit.md) | `backlog` | — | G10, G15, external |
| [11.4 External security review](../implementation-artifacts/stories/11-4-external-security-review.md) | `backlog` | 3.1 | G16, external |
| [11.5 Runbook game day](../implementation-artifacts/stories/11-5-runbook-game-day.md) | `backlog` | 11.1, 11.2 | external |
| [11.6 Certificate Transparency for access certificates](../implementation-artifacts/stories/11-6-certificate-transparency-for-access-certificates.md) | `backlog` | 4.2 | G17, external |
| [11.7 Cross-border onboarding without a national identifier](../implementation-artifacts/stories/11-7-cross-border-onboarding-without-a-national-identifier.md) | `backlog` | 10.1, 3.5 | G18, external |
| [11.8 Dependency and vendored-artefact hygiene](../implementation-artifacts/stories/11-8-dependency-and-vendored-artefact-hygiene.md) | `backlog` | — | external |
| [12.1 Requester name authenticated at consent](../implementation-artifacts/stories/12-1-requester-name-authenticated-at-consent.md) | `backlog` | — | G15, external |
| [12.2 Credential keys require user presence](../implementation-artifacts/stories/12-2-credential-keys-require-user-presence.md) | `backlog` | — | G15, external |
| [12.3 Consent gate resists guessing](../implementation-artifacts/stories/12-3-consent-gate-resists-guessing.md) | `backlog` | — | G15, external |
| [12.4 Proximity share carries consent](../implementation-artifacts/stories/12-4-proximity-share-carries-consent.md) | `backlog` | 12.1, 12.2, 12.3 | — |

## Planning verification

Verification is limited to the authored planning artifacts: inventory equality
with the 80 epic/sprint story ids, 74 unique new files plus six retained specs,
source-contract hashes, an acyclic dependency graph, existing source entry
points, local Markdown link targets, YAML/JSON structure and documentation diff
checks. The final handoff reports the observed results. No application behavior
is inferred from these planning checks.

Observed planning checks: **80 unique story ids** matched the epic document and
sprint tracker; **74 new drafts** preserved their source contract and delivery
status; all six existing spec paths resolved; the full dependency graph was
acyclic; all new YAML frontmatter and the JSON manifest parsed; **656 local
Markdown link targets** across the story collection and this analysis existed;
and the epic/sprint hashes still matched the drafting inputs. `git diff --check`
passed. Application tests were not run because this pass changes documentation
and planning data only.
