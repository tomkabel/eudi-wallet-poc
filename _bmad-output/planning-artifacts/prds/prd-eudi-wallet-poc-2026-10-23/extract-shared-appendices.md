# Extract — Shared appendices (competitive matrix, requirement index, roadmap, risk register, sources)

**Source document:** `docs/research/2026-10-23-shared-appendices.md` (324 lines, file size 39,809 bytes).
**Source commit context:** fork `tomkabel/eudi-wallet-poc`, branch `ee-eudiw-zk-import`, HEAD `2d38feb` (12 October 2026); upstream `open-eid/eudi-wallet-poc` at `0866e94` (v0.6.6, 26 May 2026).
**Companion documents digested separately:** `docs/research/2026-10-23-innovation-assessment.md` (Report 1) and `docs/research/2026-10-23-estonia-alignment-assessment.md` (Report 2).
**Extracted:** 23 October 2026, for the PRD `prd-eudi-wallet-poc-2026-10-23`.

This is a faithful extract, not an analysis. Table cells are reproduced verbatim from the source, including its own hedges ("not verified", "n/a", "poC"). Where the source labels a cell `[UNVERIFIED]` or "not verified", that label is carried through unchanged — it is never upgraded. Sections are reproduced in full; nothing is summarised away, and no comparator, requirement, phase, risk or source has been added that the document does not contain.

## Label discipline (as the source document itself defines it)

The source document's own label set, stated at its head (lines 5–6) and used consistently:

| label | source definition |
|---|---|
| `[VERIFIED]` | a tool output or a primary source quoted in the repository or a fetched document |
| `[JUDGEMENT]` | the author's analysis over verified facts |
| `[ESTIMATE]` | a number produced by a stated method with stated assumptions |
| `[ASSUMPTION]` | an unverified premise the conclusion depends on |
| `[UNVERIFIED]` | could not be established |

**Reconciliation note for the PRD author.** The PRD brief's label vocabulary is `[REC]` / `[VERIFIED]` / `[ESTIMATE]` / `[UNVERIFIED]`. The source document does **not** use a `[REC]` label; it uses `[JUDGEMENT]` for what the brief calls a recommendation (e.g. the five A6 recommendations), and it uses `[ASSUMPTION]` where an unverified premise carries a conclusion. This extract preserves the source's labels verbatim and does **not** translate `[JUDGEMENT]` into `[REC]` — the two are not equivalent, because the source reserves `[JUDGEMENT]` for analysis-over-verified-facts and states each judgement's basis, whereas a `[REC]` in the brief is a bare recommendation. Treat every `[JUDGEMENT]` below as *recommendation-grade but source-hedged*; treat every `[ASSUMPTION]` as a premise a PRD must not silently promote to fact.

---

<a id="a1"></a>
## A1 — Competitive matrix

**Selection and classification (source text).** Twelve entries: the fork and its upstream, plus ten projects selected for overlapping users, capabilities or integration role. *Direct alternatives* are wallets or verifiers a buyer could substitute for the fork's role; *complementary components* are things the fork could use or complement rather than replace. "Not verified" means the field could not be established from a fetched artefact — never that the capability is absent.

Maintenance figures are GitHub API values read on 23 October 2026 (last push, latest release, open issues, stars). Where the README was the only source, the column says so.

### A1.1 Direct alternatives

| # | project | role & target users | maturity (as its own docs state) | licence | maintenance (2026-10-23) | standards & versions claimed | capabilities relevant here | extensibility | Estonia relevance | documented limitations |
|---|---|---|---|---|---|---|---|---|---|---|
| 1 | **`tomkabel/eudi-wallet-poc` — the fork** (`ee-eudiw-zk-import`, `2d38feb`) | full ZK spine: Android wallet + Go verifier + Python issuer + Python holder; users: researcher, Estonian programme, RP integrator | self-labelled PoC with a 211-requirement specification and a published measurement programme; **not** a product | MIT `[VERIFIED]` | pushed 2026-10-12; no releases; 0 ★; 216 commits ahead of upstream, 0 behind | OpenID4VP 1.0 (DCQL, `direct_post.jwt`), OpenID4VCI 1.0, ISO 18013-5 mdoc, ISO 18013-7 DC API, TS13 `mso_mdoc_zk`/`longfellow-libzk-v1`, AV Profile `eu.europa.ec.av.1` | real Longfellow proving (~6.1 s) and verification (~2.5 s on 2 vCPU); circuit allowlist; dual carriers; hardware-backed keys; on-device `issuerAuth`; 13-case e2e with negative controls; `make check` green `[VERIFIED]` | clean seams: carrier interface, circuit registry, per-doctype trust store, JVM conformance module | **high** — it is Estonian-specific by design (`ee.riik.poa.1`, isikukood rationale, statutory thresholds) | no certification path; no real issuer; batch issuance missing; proximity Tier-2 unmeasured; **one stale conformity row contradicts the code** |
| 2 | `open-eid/eudi-wallet-poc` — upstream (v0.6.6, `0866e94`) | Estonian PoC Android wallet; users: Estonian programme (historical) | PoC, released and then dormant | MIT `[VERIFIED]` | pushed 2026-05-26; **1 commit in 12 months**; 0 open issues/PRs; 3 ★; 1 contributor `[VERIFIED]` | OpenID4VP/4VCI through `eu.europa.ec.eudi` libraries; mdoc; proximity reader | wallet app, mock PID/mDL, DigiDoc-adjacent UX | upstream of the fork's whole tree | **high** (historical: the 2023 Estonian PoC lineage) | no ZKP; no verifier; no spec; no active maintenance |
| 3 | `eu-digital-identity-wallet/eudi-app-android-wallet-ui` | reference wallet application (end user); users: programme integrators, Member States | "EUDI Wallet Prototype"; releases are explicitly **dev/demo builds, not production-ready** | EUPL-1.2 `[VERIFIED]` | pushed 2026-10-22 (default branch 2026-10-15); latest demo `2026.10.43` (2026-10-02); 229 ★ | OpenID4VP v1 with DCQL; ISO 18013-5 proximity `[VERIFIED from README]` | reference UI/UX and flows; consumes Wallet Core | modular; builds on Wallet Core | medium (it is the model any Estonian wallet build will start from) | registration-certificate checking **off by default**; explicit "not for production" |
| 4 | `eu-digital-identity-wallet/eudi-lib-android-wallet-core` | Android SDK orchestrating documents, transfer, OpenID4VCI/VP, trust, revocation | initial development release, v0.31.0, "strongly recommend not putting this version into production" | Apache-2.0 `[VERIFIED]` | pushed 2026-10-21; v0.31.0 (2026-10-02); 64 ★; 39 open issues | OpenID4VCI 1.0; OpenID4VP 1.0 with DCQL; **ZKP documented as "an optional feature that can be enabled in your application"** `[VERIFIED]` | the Commission's own ZKP documentation and Longfellow integration; proximity BLE (NFC ❌) | designed as a library for national apps | medium-high (the likeliest base for a national wallet) | credential reuse policy only partially implemented; "Notify credential issuer" ❌; not production-ready |
| 5 | `openwallet-foundation/multipaz` | Kotlin Multiplatform credential libraries, sample apps, test issuer/verifier services; users: app and SDK builders | pre-1.0, released every 4–8 weeks, 1.0 expected late 2026/early 2027; **API and on-disk data may change** | Apache-2.0 `[VERIFIED]` | pushed 2026-10-22; `0.102.0` (2026-10-22, prior 0.101.0 2026-09-10, 0.100.0 2026-07-08); 297 ★; 37 open issues | ISO/IEC 18013-5:2021 incl. proximity; SD-JWT VC; OpenID4VCI/VP `[VERIFIED]` | the prover library the fork uses (`multipaz-longfellow`) and the `LongfellowZkSystem` naming TS13 adopted | multiplatform, actively extended | medium (a component, not a rival) | no packaged iOS XCFramework; pre-1.0 churn |
| 6 | `walt-id/waltid-identity` | suite: issuer + holder wallet + verifier, APIs and white-label apps | open-source community edition of a commercial line; OpenID4VCI conformance results still "coming soon" | Apache-2.0 `[VERIFIED]` | pushed 2026-10-23; v1.2.0 (2026-10-15); 319 ★; 28 open issues | claims OpenID4VCI 1.0, OpenID4VP 1.0, HAIP, W3C VC, SD-JWT VC, mdoc/mDL `[VERIFIED from README]`; **ZKP not claimed** | broad credential-type coverage; multi-ecosystem | pluggable credential formats | medium (could host an Estonian issuer/RP) | "new open source portals and web wallet app still being built"; v1 APIs slated for deprecation |
| 7 | `Sphereon-Opensource/SIOP-OID4VP` | TypeScript library for SIOPv2 + OpenID4VP | **archived** pointer; successor is an "early development stage" monorepo | archived repo: no licence declared; successor Apache-2.0 `[VERIFIED]` | archived repo last commit **2024-07-15**; successor: OID4VCI draft v15 only on `develop` `[VERIFIED]` | successor README: OID4VCI draft v15; breaking changes expected | historical OpenID4VP reference in TypeScript | — | low | archived; successor unpublished-quality; **not a viable dependency** |
| 8 | `inji/inji-openid4vp` | Kotlin/Swift library for OpenID4VP request handling | alpha: latest release `v1.0.0-alpha.1` | MPL-2.0 `[VERIFIED]` | pushed 2026-10-21 (default branch 2026-03-06); v1.0.0-alpha.1 (2026-08-13); 1 ★; 3 open issues | **OpenID4VP draft 21 and draft 23 — not 1.0 Final**; DIF Presentation Exchange, not DCQL `[VERIFIED from README]` | MOSIP/Inji ecosystem integration | library, embedding host wallet | low-medium | README contradicts itself on supported credential formats; scope-fetch unsupported |
| 9 | `wwWallet/wallet-frontend` | web-first wallet (PWA) holder frontend, part of a suite | not stated in README `[UNVERIFIED]` | BSD-2-Clause (GUnet, SUNET, Yubico) `[VERIFIED]` | pushed 2026-10-21; `v0.6.2-1` (2026-10-15); 47 ★; 28 open issues | OpenID4VCI 1.0, OpenID4VP 1.0 with DCQL (from its docs site) `[VERIFIED]` | browser-based holder; multi-issuer | web stack, easy to embed | low-medium | "does not enforce an authenticator attestation policy"; **no high LoA** (PRF software keys); no ZKP claimed |
| 10 | `german-national-wallet/de-eudi-wallet-android` | national wallet Android app, fork of the EC reference app | read-only source mirror of a production-bound app; **not a reproducible build** of the Play distribution | EUPL-1.2 `[VERIFIED]` | pushed 2026-10-12; **0 releases, 0 tags, issues disabled**; 26 ★ | OpenID4VCI built from source; OpenID4VP not named in the README `[UNVERIFIED]` | a real national-wallet codebase for comparison | one-way mirror — contributions not possible | **medium-high as a model** (how a Member State ships a national wallet), low as a dependency | every endpoint is a placeholder on `.example.invalid`; unsigned release builds; read-only |

### A1.2 Complementary components (not substitutes)

| # | project | role | why it is complementary, not a rival | licence / activity |
|---|---|---|---|---|
| 11 | `EUDIPLO/eudiplo` | middleware bridging a backend to EUDI wallets (issuers and verifiers) — **not a wallet** | it does the RP/issuer abstraction the fork's O2 kit would build a thin version of; explicitly no mdoc proximity and no W3C VCDM; **ZKP not claimed** `[VERIFIED]` | Apache-2.0; pushed 2026-10-22; v9.1.0 (2026-10-10); 55 ★; OIDF conformance tested per README |
| 12 | `impierce/openid4vc` | Rust OpenID4VC workspace (core, vci, vp, siopv2, manager) — no wallet app | a possible Rust-side building block; tracks OpenID4VCI/VP 1.0 Finals; **no tags, no releases, not published**; ZKP not claimed `[VERIFIED]` | Apache-2.0; pushed 2026-10-21; 42 ★; 33 open issues |

### A1.3 Where the fork actually differs (comparison-scoped)

| capability | fork | upstream | best comparator |
|---|---|---|---|
| Longfellow ZK proving in a production-shaped path | yes, measured (~6.1 s) `[VERIFIED]` | no | `eudi-lib-android-wallet-core` documents ZKP as an optional feature; multipaz ships the library |
| ZK **verifier** with circuit allowlist as a service | yes (Go, 47 files/11,225 lines) `[VERIFIED]` | no | none in the set |
| `mso_mdoc_zk` carrier matching TS13 identifiers | yes (both carriers, committed vectors) `[VERIFIED]` | no | none in the set |
| `eu.europa.ec.av.1` consumption with an age-predicate policy | yes `[VERIFIED]` | no | not claimed by any comparator |
| Negative-control conformance suite (replay, predicate, carrier, encrypted-session) | yes (13 e2e cases + unit tests) `[VERIFIED]` | no | none in the set |
| National-wallet production pathway (certification, provisioning, support) | no | no | `de-eudi-wallet-android` (mirror), `eudi-app-android-wallet-ui` (prototype) |

**Sources for A1** are the repos themselves plus the GitHub API; the per-field evidence quotes and the "not verified" reasons collected during this research are in `/home/notroot/.hermes/cache/scratch/eudi-research/competitors.json` (10 comparator objects, each with `evidence_quote` and `source` per field) — a working artefact of this pass, not part of the repository. `[VERIFIED]`

**Interpretation the source draws (Report 1 §1, finding 3).** Of the ten verified comparators, none combines ZKP age proofs with a Go/Rust verifier and a published negative-control test suite: the Commission's reference wallet documents ZKP as "an optional feature", multipaz ships the prover library but not a verifier product, and walt.id, EUDIPLO, inji, wwWallet and impierce claim no ZKP at all `[VERIFIED]`. The distinctive artefact is the **verifying** side plus its refusals. Novelty is scoped to this comparison set only — a README silence is not evidence of absence.

---

<a id="a2"></a>
## A2 — Requirement-status index

Compact index of Report 2 §3. "★" marks rows where repository evidence was read directly rather than taken from documentation.

| id | requirement (short) | source | status | report § |
|---|---|---|---|---|
| L1 | wallet availability per Member State | Reg. (EU) 2024/1183; RIA FAQ | n/a | 3.1 |
| L2 | RP acceptance duty (EE: public end-2026, private end-2027) | RIA FAQ; Ministry 01.10.2026 | partial ★ | 3.1 |
| L3 | wallet certification | CIR 2024/2981; FCAF | n/a | 3.1 |
| L4 | EAA/PID issuance framework | CIR 2024/2977 (amended) | partial ★ | 3.1 |
| L5 | integrity & core functionalities | CIR 2024/2979 (amended) | partial ★ | 3.1 |
| L6 | protocols and interfaces | CIR 2024/2982 (amended) | supported ★ | 3.1 |
| A1 | remote + proximity presentation | ARF Topic 1/24 | partial ★ | 3.2 |
| A2 | data minimisation / batch issuance | ARF Topic 10 | partial ★ | 3.2 |
| A3 | dashboard & transaction log fields | ARF Topic 19 (DASH_01–03d) | partial ★ | 3.2 |
| A4 | ZKP_01 generic predicate functions | ARF v3.0.0 §A.2.3.31 | partial ★ | 3.2 |
| A5 | ZKP_02 possession of an attestation | ibid. | supported ★ | 3.2 |
| A6 | ZKP_03 binding of an attestation to a PID | ibid. | missing | 3.2 |
| A7 | ZKP_04 verifiable user pseudonym | ibid. | missing | 3.2 |
| A8 | ZKP_05 remote and proximity | ibid. | partial ★ | 3.2 |
| A9 | ZKP_06 proofs over mdoc **and** SD-JWT VC | ibid. | partial ★ | 3.2 |
| A10 | ZKP_07 no added tracking information | ibid. | partial | 3.2 |
| A11 | ZKP_08 ECCG Agreed Cryptographic Mechanisms v2.0 | ibid. | **not verified (unsatisfiable as written)** | 3.2, §8 |
| A12 | ZKP_09 LoA High preserved | ibid. | partial | 3.2 |
| A13 | accessibility (EN 301 549 presumption) | ARF Topic 54 (ACC_01/02) | partial | 3.2 |
| A14 | Certificate Transparency for access certificates | ARF Topic 55 (CT_01–06) | missing | 3.2 |
| A15 | wallet-provider support & maintenance | ARF Topic 56 (WPSM_01–) | n/a | 3.2 |
| A16 | cryptographic binding of attestations via ZKP | ARF `ACP_03` | missing | 3.2 |
| S1 | OpenID4VP 1.0 | Final 2025-07-09 | supported ★ | 3.3 |
| S2 | OpenID4VCI 1.0 | Final 2025-09-16 | partial | 3.3 |
| S3 | HAIP 1.0 | Final 2025-12-24 | partial ★ | 3.3 |
| S4 | W3C DC API + ISO 18013-7 Annex C | WD 2026-09-04 | partial ★ | 3.3 |
| S5 | ISO/IEC 18013-5:2021 | 2021 | supported ★ | 3.3 |
| S6 | ISO/IEC DIS 18013-5 2e (`ZkRequest`/`ZkDocument`) | draft; EPD Q1 2027 | partial | 3.3 |
| S7 | EC TS13 (`mso_mdoc_zk`, `zk-jwt`, `ZkDocument`) | v1.0 2025-12-15 | partial — `mso_mdoc_zk` supported, `zk-jwt` missing ★ | 3.3 |
| S8 | EC AV Profile (`eu.europa.ec.av.1`) | repo pushed 2026-09-02 | partial ★ | 3.3 |
| S9 | EC TS3 Wallet Unit Attestation | TS3 | partial | 3.3 |
| S10 | EC TS5/TS6 RP registration | TS5, TS6 | missing | 3.3 |
| S11 | revocation / status lists | ARF Topic 13; status-list specs | partial ★ | 3.3 |
| S12 | ECCG Agreed Cryptographic Mechanisms v2.0 (non-ZKP primitives) | April 2025 | partial | 3.3 |
| S13 | EC TS4 maturity position | v1.0.1 2026-01-30 | n/a | 3.3 |
| E1 | Estonian acceptance via TARA | Ministry 01.10.2026 | missing | 3.4 |
| E2 | RP register constrains requested attributes | Ministry 01.10.2026; RIA | missing | 3.4 |
| E3 | Estonian statutory thresholds / 28 February rule | fork spec Annex A | partial ★ | 3.4 |
| E4 | isikukood / date-of-birth minimisation | fork spec Annex D | supported ★ | 3.4 |
| E5 | WCAG 2.1 AA baseline | PRODUCT.md; Estonian practice | partial | 3.4 |
| E6 | national launch 2028; procurement unawarded | Ministry; RIA | n/a | 3.4 |

**Tally, machine-counted from the rows above (source text): 41 rows — 5 supported** (L6, A5, S1, S5, E4) · **23 partial** · **7 missing** (A6, A7, A14, A16, S10, E1, E2) · **5 not applicable** (L1, L3, A15, S13, E6) · **1 not verified** (A11, whose requirement is unsatisfiable as written). Report 2 §1 states the same tally.

*Extractor's count check:* the 41 source rows, the five supported, seven missing, five n/a and one not-verified entries, and the 23 remaining partial rows are consistent with the tally stated. No row was added or dropped.

---

<a id="a3"></a>
## A3 — Implementation roadmap

Phases: **D** discovery and validation, **M** minimum viable implementation, **P** pilot integration, **R** production readiness. Every date and effort below that depends on an assumption is labelled. Reference points: Estonia's public wallet launch **2028** and its acceptance duty from **end-2026** (public sector) / **end-2027** (private) `[VERIFIED]`; the procurement is unawarded, and RIA's own text allows ~1 year to contract signature `[VERIFIED]`.

Phases carry the labels **D1–D3**, **M1–M3**, **P1–P3**, **R1–R2** (eleven phases; R2 is explicitly optional). Each item's source is the roadmap table below.

| phase | deliverables | dependencies | roles | effort `[ESTIMATE]` | exit criteria | unresolved decisions |
|---|---|---|---|---|---|---|
| **D1 — AV Profile gap analysis** (2–4 weeks) | clause-by-clause map of the AV Profile against the repository, with a test id per clause; list of missing fixtures | AV Profile text (have it); DC API test rig | 1 engineer | 2–4 engineer-weeks | every clause has a test id or an explicit "out of scope for v1" note | which clauses are in the initial release; whether `av-vci://` invocation is in scope |
| **D2 — Device measurement programme** (2–6 weeks, hardware-gated) | p50/p95/p99 proving, peak memory, BLE transfer, TEE/StrongBox key generation, per device | Pixel 8 + one mid-range device | 1 engineer + devices | 1–2 engineer-weeks engineering + procurement lead time | figures reported per device, or rows explicitly left unmeasured | device set (plan names Pixel 8 and one mid-range device); StrongBox anomaly follow-up |
| **D3 — RP-register interface proposal** (2–3 weeks) | a proposed registration/access-certificate interface with mocks, a fail-closed default, and a compliance note against the ARF RP topics | none external | 1 engineer + 1 reviewer | 2–3 engineer-weeks | an Estonian service owner reviews the proposal and says whether it is usable | whether RIA will expose such an interface; whether certificates will be CT-logged (A14) |
| **M1 — AV conformance kit (O1)** (4–8 weeks) | clause registry, AV fixtures incl. external container-format examples, runner, published report | D1 | 1–2 engineers | 3–6 engineer-weeks (dependency-weighted: 4–8) | ≥ 20 clauses, ≥ 8 negative controls, external reproduction from a clean checkout | external implementation to test against |
| **M2 — Carrier conformance suite (O5)** (2–4 weeks) | documented runner over the 13 existing cases, published vectors, external result | existing tests | 1 engineer | 2–4 engineer-weeks | the suite runs against this repository and ≥ 1 external implementation; failures named by binding property | which external target first (multipaz sample vs the reference wallet) |
| **M3 — Verification-budget hardening** (2–3 weeks) | capacity figures on the RP instance type; admission control tuned; refusal-cost regression test | M2 rig | 1 engineer | 2–3 engineer-weeks | a stated verifications/second figure with a load test; refusal cost bounded (the 54–58 s defect is already fixed, keep a regression test) | peak-rate assumption (nothing official exists; must be agreed with the RP) |
| **P1 — Estonian RP acceptance kit (O2)** (8–12 weeks) | RP service profile, TARA adapter contract, register plug points with mocks, four acceptance cases, integrator README | M1, D3 | 2 engineers + 1 Estonian service partner | 6–10 engineer-weeks | four cases pass; time-to-first-successful-verification measured on ≥ 3 integrators | TARA integration terms; whether the service partner will publish a joint result |
| **P2 — Registration-certificate policy (O3)** (10–16 weeks) | `IntendedUseSource` interface, certificate-backed implementation, Topic 19 log fields, negative tests | ARF RP topics; certificate profile stability | 2 engineers | 8–14 engineer-weeks | two-predicate request allowed only with a naming certificate; log fields populated; no unit-test regressions | certificate profile version to target; whether Estonian register data can be modelled now |
| **P3 — Circuit registry (O4)** (6–10 weeks) | signed circuit list, fetch/validate with pinned key, tamper test, two-instance agreement test | none external | 1–2 engineers | 4–8 engineer-weeks | tampered list refused; two instances configure the same set from one document | who hosts the list; succession plan if no scheme owner adopts it |
| **R1 — Production readiness for a pilot RP** (12–20 weeks, mostly non-code) | operations runbook, monitoring, incident process, availability target, accessibility audit closure, security review of the deployed service | P1; security review | 2 engineers + ops + security reviewer | 10–18 engineer-weeks + review time | an external security review with no unresolved high findings; an accessibility audit pass; an operational runbook exercised in a game day | whether the pilot runs on RIA infrastructure; data-protection assessment scope |
| **R2 — Optional: statutory age-threshold toolkit (O6)** | threshold-to-circuit map, accepted circuit set, negotiation library, boundary tests | P3 + **circuits accepted by a scheme owner (external, unbounded)** | 1–2 engineers + legal review | 6–12 engineer-weeks + external wait | boundary tests pass per threshold; no `false` predicate transmitted anywhere | which thresholds; whether narrower circuits are acceptable to the scheme owner |

**Sequencing rationale (source text, verbatim).** D1/D2/D3 are cheap and each removes a specific uncertainty that would otherwise re-appear as rework in M1/P1. M1 and M2 are the two "convert what exists into evidence" deliverables and are ordered before P1 because P1's four acceptance cases depend on the AV-path fixtures M1 produces. R1 is deliberately the largest non-code phase: the repository has no operations, monitoring or incident surface, and no amount of cryptography compensates for that in a pilot.

**Dates deliberately not promised (source text, verbatim).** Nothing above is dated in calendar terms except the external deadlines (end-2026, end-2027, 2028). The reason is that the queue of work depends on the unawarded procurement and on a register interface that does not exist; a dated roadmap would be an `[ASSUMPTION]` wearing a schedule's clothes. `[JUDGEMENT]`

---

<a id="a4"></a>
## A4 — Risk register

Likelihood and impact: L/M/H. "Residual uncertainty" states what remains after mitigation. No risk row in the source document carries a named owner (the "owner" column the PRD brief anticipates does **not** exist in this source — see the note after the table).

| # | risk | type | likelihood | impact | mitigation | residual uncertainty |
|---|---|---|---|---|---|---|
| R1 | **ISO/IEC 18013-5 second edition ships `ZkRequest`/`ZkDocument` differently from the draft the carrier approximates** | interoperability / standards | M | H | version-label every fixture and profile rule; keep the carrier interface (`oid4vp/carrier.go`) as the single adaptation point; re-run vectors on publication | the ISO text could not be read at all during this research `[UNVERIFIED]`, so the divergence size is unknown |
| R2 | **A ZKP system other than Longfellow becomes the EU-preferred one (e.g. the BBS-family TS14 path)** | interoperability | L–M | H | keep BBS/MSS out of the carrier but keep the format-identifier rule (one format family per scheme) so a second family can be added without a dialect collision | ETSI TS 119 476-2's direction; the ARF refinement round's outcome |
| R3 | **`ZKP_08`'s unsatisfiable requirement is resolved in a way that invalidates the current scheme** | regulatory / standards | L | H | state the tension in writing wherever a programme is told about compliance; track the ECCG catalogue's next revision | whether the catalogue will add a ZK entry, or the ARF will relax the requirement |
| R4 | **No scheme owner adopts the circuit registry (O4), or the accepted set lands somewhere else** | adoption | M | M | ship the registry as a generator plus a hosted instance with a documented succession path; keep the local pinned list as the fallback | who the scheme owner is — the EU AV Profile says the set is maintained "separately" without naming the maintainer |
| R5 | **Verification cost makes an RP's economics unattractive** (p95 4.91 s against a 1.0 s budget on the fork's own host) | technical / performance | M–H | M | measure on target hardware (M3); admit control per credential; cache nothing that weakens binding; publish honest figures | on-device and on-target-host numbers do not exist yet (PENDING-DEVICE) |
| R6 | **Proof size (~360 KB) breaks a proximity or mobile-network budget** | technical / usability | M | M | measure BLE transfer (D2); treat proximity Tier-2 as optional until measured (the fork's spec already does this) | no BLE measurement exists; the 360 KB figure is host-side |
| R7 | **Privacy regression via the plain fallback path** — a request that cannot be answered with ZK is answered plainly | privacy | M | H | the wallet labels the tier and the record; `EE-ZKP-055a` refuses plain-mdoc-only requests when the device is ZK-capable; the fallback must remain visible in logs and UI | whether an RP will accept a refusal instead of a plain presentation in practice |
| R8 | **The registration-certificate policy loosens a control through a parsing error** | security | L–M | H | default to refusal; property tests over generated certificates; the loosening path is covered by explicit negative tests | the certificate profile's stability; the ARF annexes are moving |
| R9 | **Register on the critical path** — a fail-closed register lookup makes every presentation depend on a service that does not exist yet | adoption / availability | H (today) | M | mock now, interface-first design; cache with a bounded age and an explicit stale-policy decision; never silently allow on lookup failure | whether RIA will publish an interface and its availability target `[UNVERIFIED]` |
| R10 | **Upstream is dormant** (one commit in 12 months, 0 open issues/PRs, 1 contributor), so the fork carries the whole maintenance load | maintenance | H | M | the fork is a strict superset (0 commits behind, verified) and vendors what it needs; keep library upgrades routine and pinned (`libs.versions.toml`) | whether upstream resumes; the fork has no second maintainer `[JUDGEMENT]` |
| R11 | **Licence and provenance risk in a derived work** — MIT is permissive, but the fork ships vendored/mirrored dependency artefacts (a published Maven mirror under `.github/maven-mirror`, a vendored `coseJava` mirror) | licensing | L–M | M | inventory every vendored artefact with its upstream licence and revision; keep the MIT notice; do not import material from a private repository without written permission | the mirror's contents were not enumerated during this research `[UNVERIFIED]` |
| R12 | **Documentation contradicts the code** — `docs/CONFORMITY.md` still reports on-device `issuerAuth` validation as absent | credibility | H (present) | M | fix the row; add a check that the conformity note's statuses are re-derived from tests rather than hand-maintained | the repository has more rows of this kind that were not audited in this pass `[ASSUMPTION]` |
| R13 | **Accessibility duty unmet at audit time** (WCAG 2.1 AA claimed; no EN 301 549 evidence) | regulatory / adoption | M | M | run an accessibility audit as a milestone exit criterion (R1 in A3) | what an Estonian public-sector audit will require for a wallet UI |
| R14 | **Third-party dependency on a pre-1.0 library** (multipaz 0.99.0 pinned; pre-1.0 churn; eudi libraries pre-1.0) | technical / maintenance | H | M | pin versions (already done), constrain transitively (already done), and keep the conformance module to detect breakage early | when multipaz 1.0 lands and whether it changes the Longfellow naming |
| R15 | **A programme reads an `[ESTIMATE]` as a commitment** — effort ranges here are derived from agent-assisted work | commercial | M | M | keep the method and assumptions attached to every number; never present a range without its baseline | the receiving organisation's tolerance for estimates |

**Owner field — absent in the source.** The PRD brief asks for "likelihood, impact, mitigation and any owner". The source A4 table has columns *#, risk, type, likelihood, impact, mitigation, residual uncertainty* — there is **no owner column and no owner named anywhere in a risk row**. Do not invent one. The nearest thing to an accountable party appears in the A3 roadmap's *roles* column and in the mitigations (e.g. "an Estonian service owner reviews the proposal" for D3; "the receiving organisation" for R15).

**Related qualitative risk material lives outside A4** (carried here for completeness, labelled as it appears): Report 1 §6 "How this assessment could be wrong" enumerates four classes of error — the comparator set may understate others' ZKP work `[JUDGEMENT]`; the ISO second-edition text could not be read `[UNVERIFIED]`; effort estimates are one-person, agent-assisted baselines `[ESTIMATE]`; and the whole class of opportunities depends on a standard that is still open, with `ZKP_08` unsatisfiable as written being "the largest single uncertainty in this assessment."

---

<a id="a5"></a>
## A5 — Source register

Access date for all entries: **23 October 2026**, unless stated. Retrieval method: local Firecrawl endpoint `http://asus:3002` (`/v1/scrape`, `/v1/map`) for web content; authenticated `gh api` / `gh search` for GitHub; direct repository reads for the fork's own files.

### A5.1 Repository evidence (this checkout, HEAD `2d38feb`)

*Class: `[VERIFIED]` — direct local reads.*

| title | path | version / date | notes |
|---|---|---|---|
| Repository README | `README.md` | 2026-10-11 | layout, measured figures, independent-fork notice |
| Product description | `PRODUCT.md` | — | users, positioning, accessibility commitment |
| Conformity statement | `docs/CONFORMITY.md` | touched by HEAD commit 2026-10-12 | **contains the stale `issuerAuth` row (§2 note, R12)** |
| Measurements | `docs/MEASUREMENTS.md` | 2026-09-25 device block | pending-device rows; host verification figures |
| Technical specification | `spec/EE-EUDIW-TS-1.0.md` | rev 1.7, 2026-10-12 | 211 requirements; §22 divergences; §23 unverified items |
| Carrier profile | `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md` | v1.0, 2026-09-25 | format id, `zk_system_type` registry, response shape |
| Verifier documentation | `verifier/README.md` | — | measured cases; refusal-cost honesty note; encrypted flow |
| Test catalogue | `tests/README.md` | — | every test and what it proves |
| Build/check definitions | `Makefile`, `.github/workflows/ci.yml`, `tests/requirements-ci.txt` | — | `make check` targets; pinned Python inputs |
| Fork record | `docs/planning/STEP1-FORK-RECORD.md` | 2026-09-23 | fork identity changes and verification evidence |
| Code read directly | `verifier/go/oid4vp/dcql.go`, `carrier.go`, `vptoken.go`; `app/src/main/kotlin/ee/cyber/wallet/**` (`AgePredicatePolicy.kt`, `PresentationTier.kt`, `CredentialToDocumentMapper.kt`, `DeviceRequestParser.kt`, `ZkProofBenchmark.kt`, `ClientIdPrefixPolicyTest.kt`, `CredentialToDocumentMapperMdocVerificationTest.kt`), `tests/vectors/carrier-v1/*` | HEAD | basis for every "supported/partial" status marked ★ in A2 |
| Verification run | `make check` in a venv with `tests/requirements-ci.txt` | 2026-10-23 | exit 0; gofmt/vet/ruff/shellcheck/`go test -race`/Python vectors green |

### A5.2 EU legislation, specifications and technical publications

*Class: primary EU sources, fetched `[VERIFIED]` unless a row says otherwise.*

| title | publisher | URL | version / date |
|---|---|---|---|
| EU Digital Identity Wallet — home and framework summary | European Commission (Digital Building Blocks) | `ec.europa.eu/digital-building-blocks/sites/spaces/EUDIGITALIDENTITYWALLET/pages/694487738/` | page last updated Jan 2026 |
| Architecture and Reference Framework (ARF) | European Commission / EUDI Wallet | `https://eudi.dev/latest/` | v3.0.0; changelog 2026-07-21; release published 2026-07-23 |
| ARF Annex 2.02 — High-Level Requirements by topic (Topic 53 ZKP; Topic 54 accessibility; Topic 55 CT; Topic 56 WPSM) | European Commission | `https://eudi.dev/latest/annexes/annex-2/annex-2.02-high-level-requirements-by-topic/` | v3.0.0 |
| ARF Topic G discussion paper (ZKP) | European Commission | `https://eudi.dev/latest/discussion-topics/` | v1.4, 2025-03-30; non-normative |
| Functional Conformance Assessment Framework | European Commission | `https://conformance.eudi.dev/latest/` | "content under active development" |
| EUDI Wallet reference implementation docs | European Commission | `https://docs.eudi.dev/latest/` | feature map / roadmap / repositories |
| TS4 — ZKP implementation in the EUDI Wallet | European Commission | `github.com/eu-digital-identity-wallet/eudi-doc-standards-and-technical-specifications/…/ts4-zkp.md` | v1.0 2025-05-21; v1.0.1 2026-01-30 |
| TS13 — ZKP based on arithmetic circuits | European Commission | `…/ts13-zksnarks.md` | v1.0 2025-12-15 |
| TS14 — ZKP based on multi-message signatures | European Commission | `…/ts14-zkps-from-mms.md` | v1.0 2026-02-27 |
| Age Verification solution — Annex A (AV Profile) and Annex B (ZKP) | European Commission | `github.com/eu-digital-identity-wallet/av-doc-technical-specification` | repository created 2025-03-26, pushed 2026-09-02; 99 ★ |
| Age Verification development environment | European Commission | `https://ageverification.dev/` | accessed 2026-10-23 |
| ECCG Agreed Cryptographic Mechanisms v2.0 | ENISA / ECCG | `certification.enisa.europa.eu/…/a845662b-…` (via EC issue #416) | v2.0, April 2025 (ARF calls it v2, 2025-05) — **contains no ZKP scheme** |
| ISO/IEC 18013-5 second edition status | European Commission tracker (not ISO) | `github.com/eu-digital-identity-wallet/eudi-doc-standards-and-technical-specifications/issues/84` | update 2026-09-22: FDIS approval step, EPD Q1 2027 |
| longfellow-zk (Google ZK library) | longfellow-zk project | `https://github.com/longfellow-zk/longfellow-zk` | Apache-2.0; v0.9 2026-03-31; pushed 2026-10-21; reviews incl. 2025-08-18 |
| `draft-google-cfrg-libzk-03` | IETF (individual draft) | `https://www.ietf.org/archive/id/draft-google-cfrg-libzk-03.txt` | claims ≥ 115 bits for `(p256,132,7)` |
| Regulation (EU) 2024/1183 article text | EUR-Lex | `eur-lex.europa.eu/…` | **not retrieved — HTTP 500 through the available tooling** (see Report 2 §8) |

### A5.3 OpenID specifications

| title | URL | version / date |
|---|---|---|
| OpenID for Verifiable Presentations 1.0 | `https://openid.net/specs/openid-4-verifiable-presentations-1_0.html` | Final, 2025-07-09; **no ZK text** |
| OpenID for Verifiable Credential Issuance 1.0 | `https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html` | Final, 2025-09-16 |
| OpenID4VC High Assurance Interoperability Profile 1.0 | `https://openid.net/specs/openid4vc-high-assurance-interoperability-profile-1_0.html` | Final, 2025-12-24 |
| OpenID4VP issue #627 — Support for mdoc ZKP requests | `https://github.com/openid/OpenID4VP/issues/627` | open, created 2025-06-03 |
| openid/dchp issue #17 — ZKP Support | `https://github.com/openid/dchp/issues/17` | open, `parking-lot`, created 2026-07-27 |

### A5.4 Estonia

*Class: primary Estonian government/operator sources unless a row says otherwise.*

| title | publisher | URL | date |
|---|---|---|---|
| EUDI Wallet programme page (EN) | RIA | `ria.ee/en/state-information-system/electronic-identity-eid-and-trust-services/eudi-wallet` | last updated 22.07.2026 |
| Digikukkur page (ET; target of `digikukkur.ee`) | RIA | `ria.ee/riigi-infosusteem/elektroonilise-identiteedi-teenused/digikukkur-eudi-wallet` | last updated 30.09.2026 |
| EUDI Wallet projects (APTITUDE, POTENTIAL) | RIA | `…/eudi-wallet-projects` | accessed 2026-10-23 |
| Procurement launch news | RIA | `ria.ee/en/news/ria-has-launched-procurement-find-eudi-wallet-service-provider` | 18.05.2026 |
| Market consultation (pre-procurement) | RIA | `…/we-invite-market-participants-participate-market-consultation-estonian-digital-wallet` | 25.06.2025 |
| eID ecosystem page | RIA | `…/electronic-identity-eid` | figures as at July 2026 |
| Authentication services (TARA) | RIA | `…/authentication-services` | accessed 2026-10-23 |
| X-tee page | RIA | `ria.ee/en/state-information-system/data-exchange-platforms/data-exchange-layer-x-tee` | accessed 2026-10-23 |
| Government approves the wallet bill (2028 launch, TARA, register, EUTS amendment) | Ministry of Justice and Digital Affairs | `justdigi.ee/uudised/valitsus-kiitis-heaks-seaduseelnou-euroopa-liidu-digiidentiteedikukru-kasutuselevotuks` | 01.10.2026 |
| X-Road architecture | X-Road / NIIS | `https://x-road.global/architecture` | accessed 2026-10-23 |
| Live eID counters | SK ID Solutions (id.ee) | `https://www.id.ee/en/` | read 2026-10-23 (ID-card 1,383,604 updated 16.10.2026; Smart-ID EE 799,658 updated 22.10.2026) |
| e-Residency | Republic of Estonia | `https://www.e-resident.gov.ee/` | accessed 2026-10-23; **no EUDI reference** |
| ESTONIA procurement value (~€21.65 M) | Postimees (sponsored content) | `postimees.ee/8484327/…` | 04.06.2026 — **press only, sponsored** |
| Bill and register coverage | Postimees | `postimees.ee/8518894/…` | 31.07.2026 |
| APTITUDE country page (shared content) | APTITUDE consortium | `aptitude.digital-identity-wallet.eu/countries/estonia/` | Estonia joined spring 2026; "more than 141" organisations |

### A5.5 Comparator repositories (metadata read 2026-10-23 via GitHub API)

*Class: `[VERIFIED]` GitHub API metadata.*

`eu-digital-identity-wallet/eudi-app-android-wallet-ui` (EUPL-1.2, 229 ★, demo 2026.10.43) · `eu-digital-identity-wallet/eudi-app-ios-wallet-ui` (EUPL-1.2, 90 ★) · `eu-digital-identity-wallet/eudi-lib-android-wallet-core` (Apache-2.0, 64 ★, v0.31.0) · `eu-digital-identity-wallet/eudi-lib-jvm-siop-openid4vp-kt` (Apache-2.0, 36 ★) · `eu-digital-identity-wallet/eudi-srv-web-verifier-endpoint` (Apache-2.0, 67 ★, v0.12.0) · `eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py` (Apache-2.0, 33 ★, v0.10.0) · `openwallet-foundation/multipaz` (Apache-2.0, 297 ★, 0.102.0) · `walt-id/waltid-identity` (Apache-2.0, 319 ★, v1.2.0) · `Sphereon-Opensource/SIOP-OID4VP` (archived, last commit 2024-07-15) · `inji/inji-openid4vp` (MPL-2.0, v1.0.0-alpha.1) · `wwWallet/wallet-frontend` (BSD-2-Clause, v0.6.2-1) · `german-national-wallet/de-eudi-wallet-android` (EUPL-1.2, 0 releases, issues disabled) · `EUDIPLO/eudiplo` (Apache-2.0, v9.1.0) · `impierce/openid4vc` (Apache-2.0, 0 tags/0 releases) · `open-eid/eudi-wallet-poc` (MIT, 3 ★, v0.6.6) · `tomkabel/eudi-wallet-poc` (MIT, 0 ★).

Per-field evidence quotes and the explicit "not verified" list are in the research workspace (`competitors.json`, 10 objects; `zk/INDEX.md`; `estonia/ANSWERS.md`) produced during this pass.

**Source-register caveats a PRD must carry forward** (verbatim from A5): the Regulation (EU) 2024/1183 article text was **not retrieved** (HTTP 500); the ~€21.65 M procurement value is **press only, sponsored**; the e-Residency page carries **no EUDI reference**; the ISO/IEC 18013-5 2e status comes from a **Commission tracker, not ISO**; the ECCG catalogue **contains no ZKP scheme**; and the A1 per-field quotes live in a scratch workspace outside the repository, which is "a working artefact of this pass, not part of the repository."

---

<a id="a6"></a>
## A6 — Shared changes and the tradeoff between the two objectives

### Shared changes (serve both the innovation and the Estonia-alignment objectives)

| shared change | innovation effect | Estonia effect |
|---|---|---|
| AV-Profile clause registry and fixtures (O1) | converts the carrier from a fork convention into an EU-recognised profile | is the exact artefact the Estonian programme will need if it adopts the EU AV blueprint |
| Registration-certificate policy engine (O3) | replaces a blanket refusal with a lawful, explainable decision — a capability no comparator has | implements the Estonian register's stated rule ("only what is registered may be requested") before the register exists |
| Circuit registry (O4) | removes an interoperability landmine every implementer currently solves privately | gives an Estonian RP or wallet a single source for "which circuits do we accept" |
| Carrier conformance suite (O5) | publishes the negative controls that make the claim checkable | gives Estonian services a way to test a foreign wallet before 2028 |
| Fixing the stale conformity row and banning "conformant" language (R3) | raises credibility with any technical evaluator | removes a statement the Estonian programme would otherwise have to correct itself |
| Device measurement programme (D2/R4) | supplies the numbers the specification currently marks pending | answers the first question any procurement asks about a wallet |

### Tradeoffs between the two objectives (all five, verbatim, with their labels and recommendations)

1. **Narrow-age vs broad-generality.** Innovation would favour extending toward ARF's general ZKP vision (binding to a PID, pseudonyms, general attribute predicates — A6, A7, A16). Estonia's near-term need is a single boolean at a relying party. **Recommendation: stay narrow and say so.** The general work depends on schemes and specifications that do not exist; a narrow implementation that is measured beats a broad one that is asserted. `[JUDGEMENT]`
2. **Estonian attestation vs EU attestation.** The fork's own spec already chooses to prefer `eu.europa.ec.av.1` for every relying party. That maximises cross-border alignment and makes the Estonian `ee.riik.poa.1` the fallback — which is the right order for interoperability and the riskier order politically, since an Estonian programme may want its own credential first. **Recommendation: keep the spec's order, keep both paths, and let the programme choose.** `[JUDGEMENT]`
3. **Register on the critical path.** A fail-closed register lookup (E2) is the privacy-correct design and it makes every presentation depend on a service that does not exist yet (R9). **Recommendation: interface-first with mocks, a bounded cache, and an explicit, logged decision about stale data — never a silent allow.** `[JUDGEMENT]`
4. **Evidence speed vs evidence depth.** Publishing an external conformance result early (O5) buys credibility quickly but risks a public failure; running the full device programme first (D2) buys defensible numbers but delays any external claim. **Recommendation: publish the carrier suite result first (it is expected to pass), and hold performance claims until D2 completes.** `[JUDGEMENT]`
5. **Procurement timing.** If the Estonian procurement is awarded while this work is in flight, the winning provider will set the technical direction and the RF-side kit (O2) may be superseded. That is a reason to finish D3 (the interface proposal) early, so the proposal is on the table rather than reconstructed. `[JUDGEMENT]`

---

## Extraction completeness note

Every section of the source document has been reproduced: A1 (all 12 comparators across every column, plus the comparison-scoped differentiation table), A2 (all 41 requirement rows and the tally), A3 (all 11 numbered phases D1–D3 / M1–M3 / P1–P3 / R1–R2 with each item's source, plus the sequencing rationale and the "dates deliberately not promised" statement), A4 (all 15 risks R1–R15 with type, likelihood, impact, mitigation and residual uncertainty), A5 (all five source-register sub-tables and the access/retrieval method) and A6 (all six shared changes and all five tradeoffs, each with its `[JUDGEMENT]` label — four carry an explicit bolded **Recommendation:**, and the fifth, "Procurement timing", states its recommendation only in prose, exactly as the source does).

**Two sections the PRD brief anticipates that this document does not supply, stated plainly rather than filled in:**
- **Risk owners.** The A4 register carries no owner column and names no owner in any row. Owner assignment must come from elsewhere (the PRD author, or an A3 roadmap role) — it cannot be extracted from this source.
- **Calendar dates for the roadmap.** A3 deliberately dates nothing except external deadlines (end-2026, end-2027, 2028) and states why: a dated roadmap would be "an `[ASSUMPTION]` wearing a schedule's clothes." `[JUDGEMENT]`

All other product-relevant intent in the document — the differentiation claims, requirement statuses, phasing, risk mitigations, source provenance and objective tradeoffs — is above. Nothing has been upgraded from `[UNVERIFIED]` or `[ASSUMPTION]`, and no comparator, requirement, phase, risk or source has been added.
