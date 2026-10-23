# Extract — Estonia EUDI Wallet alignment and practical application assessment

**Source:** `/home/notroot/Documents/eudi-wallet-poc/docs/research/2026-10-23-estonia-alignment-assessment.md`
(490 lines, version 1.0, research date 23 October 2026, Europe/Tallinn).
**Subject of the source:** `github.com/tomkabel/eudi-wallet-poc`, branch `ee-eudiw-zk-import`, HEAD `2d38feb`
(12 October 2026) — an independent fork of `open-eid/eudi-wallet-poc` v0.6.6 (`0866e94`) with a
zero-knowledge proof-of-age spine.
**Companion sources read for this extract:** `docs/research/2026-10-23-shared-appendices.md`
(A1 competitive matrix, A2 requirement-status index, A3 implementation roadmap, A4 risk register,
A5 source register, A6 shared changes/tradeoffs) — the roadmap assignment of R1–R5 below is drawn
from A3.
**Extract purpose:** verbatim-grade input to the PRD for this fork. Section numbering follows the
source document. Nothing here is upgraded in evidence status; the source's own labels
(`[VERIFIED]`, `[JUDGEMENT]`, `[ESTIMATE]`, `[ASSUMPTION]`, `[UNVERIFIED]`, `[REC]`) are carried through.

**Source's own scoping statement (verbatim intent):** it is a **technical gap assessment** of one PoC
fork against applicable EU and Estonian requirements. It is **not** a conformity assessment, a
certification opinion, or legal advice: no accredited body has evaluated this code, the fork is not a
notified provider, and no claim of compliance — as opposed to "supported by evidence in this
repository" — is made.

---

## 0. Thesis (source §1, Executive summary)

Where can this fork provide the most credible value to Estonia's transition? **In three places, and in
one of them it is already most of the way there.**

1. **Age verification is the fork's natural fit, because the EU has just made it normative and Estonia
   will inherit that requirement.** The Commission's AV Profile fixes the doctype `eu.europa.ec.av.1`,
   a single mandatory attribute `age_over_18`, the ZKP system `longfellow-libzk-v1` ("An implementation
   SHALL use that system"), the parameter tuple `circuit_hash, version, num_attributes, block_enc_hash,
   block_enc_sig`, and transport over the W3C Digital Credentials API `[VERIFIED]`. The fork already
   mints and consumes `eu.europa.ec.av.1`, keys its age-predicate policy on both that attestation and
   its own `ee.riik.poa.1`, prefers the EU attestation for every relying party, and proves with
   Longfellow over the same carrier the Commission's **TS13** names (`mso_mdoc_zk`,
   `longfellow-libzk-v1`, `circuit_hash`) `[VERIFIED]`. Nothing in the comparator set of ten projects
   combines those three things `[VERIFIED — Appendix A1]`.
2. **Estonia's own wallet is not the near-term opportunity; Estonian relying parties are.** The Estonian
   wallet's public launch is planned for **2028** per the Ministry of Justice and Digital Affairs, while
   from the **end of 2026** Estonian public-sector e-services must accept *other* Member States'
   certified wallets, with the private sector from the end of 2027 and TARA named as the central
   service being updated for that acceptance `[VERIFIED]`. The fork's Go verifier and its
   negative-control test suite are the artefacts an Estonian service needs in that window; the missing
   piece is the institutional layer (the Relying Party Register has no published API, and no TARA
   adapter exists) `[VERIFIED as absent]`.
3. **The privacy argument has an Estonian-specific sharp edge.** Estonian personal identification codes
   embed date of birth, so a plain age check discloses more than the transaction needs; the fork's
   design premise is precisely to prove an age predicate without the date of birth, and its carrier
   profile records the residue honestly (the proof still reveals `msoX5chain`) `[VERIFIED]`. For an
   Estonian service, that is a data-minimisation claim it can defend, not a slogan.

**The three findings that most constrain Estonia's transition, all verified on 23 October 2026:**

- The procurement is **still unawarded** and RIA's own page (updated 30 September 2026) says development
  is at the tender stage with roughly a year to contract signature; no award announcement exists in any
  reachable source `[VERIFIED]`, and the tender register itself could not be read through the available
  tooling `[UNVERIFIED]`.
- The **Relying Party Register has no published technical specification or API**; the access-threshold
  certificate approach is expressly still being designed `[VERIFIED]`.
- **X-Road has no documented relationship to the EUDI Wallet, OpenID4VP or OpenID4VCI.** The string
  "OpenID" occurs zero times across RIA's X-tee page, `x-road.global` and the X-Road repository landing
  page `[VERIFIED as an absence]`. Any integration story that quietly assumes otherwise is unsupported.

### 0.1 Requirements status at a glance (source §1)

Of **41 requirement rows assessed, 5 are supported** by verified repository evidence, **23 partially
supported**, **7 missing**, **5 not applicable** to a PoC fork, and **1 not verified** — that last row
being the ARF's own `ZKP_08`, which is unsatisfiable as written because the cryptographic catalogue it
names lists no ZKP scheme. **No row is "compliant"**, because compliance is a status conferred by an
assessment body, not by a repository.

Tally, machine-counted (A2): **41 rows — 5 supported** (L6, A5, S1, S5, E4) · **23 partial** ·
**7 missing** (A6, A7, A14, A16, S10, E1, E2) · **5 not applicable** (L1, L3, A15, S13, E6) ·
**1 not verified** (A11, whose requirement is unsatisfiable as written).

---

## 1. Prioritised recommendations R1–R5, with roadmap assignment (source §9 + A3/A6)

The source states four recommendations in the executive summary (§1) numbered R1–R4 — the fourth being
the device measurement programme — and **five** in §9. §9 is authoritative for numbering: R4 is the
device programme and R5 ("keep the standards gap visible") appears only in §9. Recorded here as a
discrepancy in the source, not resolved.

**R1 — Build the EU AV-Profile conformance kit (O1).** It is the only work here that converts existing,
tested code into a capability the EU has specified normatively, and it has no external blocker.
*Acceptance:* ≥ 20 clauses, ≥ 8 negative controls, a third party reproduces the report from a clean
checkout. `[REC]`
*Roadmap assignment (A3):* deliverable **M1 — AV conformance kit (O1)**, 4–8 weeks, 3–6 engineer-weeks
(dependency-weighted 4–8), 1–2 engineers, depends on **D1 — AV Profile gap analysis** (2–4 weeks,
1 engineer, 2–4 engineer-weeks). Exit criteria: ≥ 20 clauses, ≥ 8 negative controls, external
reproduction from a clean checkout. A6 lists it as serving both objectives: converts the carrier from a
fork convention into an EU-recognised profile.

**R2 — Build the Estonian relying-party acceptance kit and *propose* the register interface (O2).**
Deliver the four acceptance cases plus a register-interface proposal, fail-closed by default. "This is
the only recommendation a buyer can act on before 2028." `[REC]`
*Roadmap assignment (A3):* deliverable **P1 — Estonian RP acceptance kit (O2)**, 8–12 weeks,
6–10 engineer-weeks, 2 engineers + 1 Estonian service partner, depends on **M1** and **D3 — RP-register
interface proposal** (2–3 weeks, 1 engineer + 1 reviewer, 2–3 engineer-weeks). Exit criteria: four cases
pass; time-to-first-successful-verification measured on ≥ 3 integrators. Sequencing rationale: M1 is
ordered before P1 because P1's four acceptance cases depend on the AV-path fixtures M1 produces.

**R3 — Fix the repository's own honesty defects before showing it to anyone.** `CONFORMITY.md`'s
`issuerAuth` row contradicts the code (`[VERIFIED]`); replace "conformant" language with "supported by
these tests"; and publish the clause maps that are missing (HAIP, AV Profile, TS13) rather than implying
them. `[REC]`
*Roadmap assignment (A3):* no dedicated phase row. A6 lists it as a shared change — "fixing the stale
conformity row and banning 'conformant' language (R3)" — raising credibility with any technical
evaluator and removing a statement the Estonian programme would otherwise have to correct itself.
Cross-referenced by risk **R12** in A4 ("Documentation contradicts the code", likelihood H present,
impact M).

**R4 — Run the device measurement programme before making any performance claim.** The harness exists and
the host figures already miss the fork's own `EE-ZKP-040` budget (p95 4.91 s vs 1.0 s) `[VERIFIED]`. "A
programme that hears a performance claim from a laptop and then measures a phone will discount
everything else in the repository." `[REC]`
*Roadmap assignment (A3):* deliverable **D2 — Device measurement programme**, 2–6 weeks, hardware-gated,
1 engineer + devices, 1–2 engineer-weeks engineering + procurement lead time. Exit criteria: figures
reported per device, or rows explicitly left unmeasured. Devices: Pixel 8 + one mid-range device. A6
lists it as supplying the numbers the specification currently marks pending and answering the first
question any procurement asks about a wallet.

**R5 — Keep the standards gap visible.** Report the `ZKP_08`/ECCG tension and the ISO timing to whoever
is buying, in writing, with the sources. "A PoC whose authors name the open risks is more useful to RIA's
2028 programme than one that does not." `[REC]` / `[JUDGEMENT]`
*Roadmap assignment (A3):* no phase row. Cross-referenced by risk **R3** in A4 ("`ZKP_08`'s unsatisfiable
requirement is resolved in a way that invalidates the current scheme", likelihood L, impact H) and by the
external-dependency note on roadmap item **R2 — Optional: statutory age-threshold toolkit (O6)**, which
waits on "**circuits accepted by a scheme owner (external, unbounded)**".

**Related roadmap deliverables the source names (O-items; A3).** O1 = AV conformance kit (M1). O2 =
Estonian RP acceptance kit (P1). **O3 = registration-certificate policy (P2, 10–16 weeks, 8–14
engineer-weeks, 2 engineers)** — implements the Estonian register's stated rule before the register
exists. **O4 = circuit registry (P3, 6–10 weeks, 4–8 engineer-weeks)**. **O5 = carrier conformance suite
(M2, 2–4 weeks, 2–4 engineer-weeks)** — publishes the negative controls; exit: runs against this
repository and ≥ 1 external implementation. **O6 = statutory age-threshold toolkit (A3 "R2 — Optional",
6–12 engineer-weeks + external unbounded wait)**. A3 also carries **M3 — Verification-budget hardening**
(2–3 weeks) and **R1 — Production readiness for a pilot RP** (12–20 weeks, 10–18 engineer-weeks, mostly
non-code: runbook, monitoring, incident process, availability target, accessibility audit closure,
security review).

**Dates deliberately not promised (A3, `[JUDGEMENT]`):** nothing in the roadmap is dated in calendar
terms except the external deadlines (end-2026, end-2027, 2028), because "a dated roadmap would be an
`[ASSUMPTION]` wearing a schedule's clothes".

---

## 2. Requirements traceability matrix — all 41 rows (source §3, status column prose intact)

Status vocabulary (source): **supported** (verified evidence in this repository), **partial**,
**missing**, **n/a** (not a duty of a PoC fork), **not verified**. "Evidence" is a repository path or a
fetched source; "verification method" is how the required change would be checked.

### 2.1 EU legal and certification duties

| id | requirement (source, version, date) | applies to the fork's role? | current capability + evidence | status | required change | verification method |
|---|---|---|---|---|---|---|
| L1 | Wallet availability, at least one wallet per Member State (Reg. (EU) 2024/1183; RIA states the deadline as end-2026) | no — the fork is not a notified wallet provider | none claimed | **n/a** | none; relevant only as market context | — |
| L2 | Acceptance by public-sector relying parties from end-2026 (EE), private sector end-2027 | yes — the Go verifier is an RP-side artefact | OpenID4VP 1.0 verifier with DCQL, `direct_post.jwt` default, single-use sessions (`410` on replay), 13-case e2e (`verifier/go/`, `tests/e2e.sh`) | **partial** | RP-register/registration-certificate awareness; TARA-side adapter; a deployable service profile | run the four acceptance cases of O2 against a sample Estonian service |
| L3 | Wallet certification under CIR 2024/2981 and the FCAF | no — certification applies to providers | none | **n/a** | none; do not use "conformant"/"certified" language | — |
| L4 | EAA/PID issuance framework (CIR 2024/2977, as amended) | yes — the fork issues EAAs | Python issuer mints `ee.riik.poa.1` and `eu.europa.ec.av.1`; mock wallet-provider attestation; `issuer/test_mint_device_key.py` green | **partial** | real issuer integration; batch issuance (a gap the repository's own analysis records); WUA per TS3 | interop test against the Commission's issuer service |
| L5 | Integrity and core functionalities (CIR 2024/2979) | yes | hardware-backed keys via multipaz `AndroidKeystoreSecureArea` (StrongBox where available), PIN verification, key cleanup on failure (`app/.../security/`) | **partial** | mobile-device security requirements read directly from the CIR (the article text could not be fetched) | device-level test matrix on the named device set |
| L6 | Protocols and interfaces (CIR 2024/2982) | yes | OpenID4VP 1.0 + OpenID4VCI 1.0 + DC API + ISO 18013-5 mdoc; library versions pinned in `gradle/libs.versions.toml` | **supported** | none known | cross-implementation interop run |

### 2.2 ARF v3.0.0 high-level requirements relevant to this role

| id | requirement | applies? | current capability + evidence | status | required change | verification method |
|---|---|---|---|---|---|---|
| A1 | Topic 1/24 remote and proximity presentation | yes | remote: verified in tests; proximity: reader path present, Tier-2 proximity explicitly optional until measured (`EE-CNF-006`) | **partial** | measured proximity throughput (see B3) | on-device proximity measurement |
| A2 | Topic 10 data minimisation, unlinkability, batch issuance | yes | ZK path shows one boolean; plain path labelled as linkable in the UI (`PresentationTier`); batch issuance for EE-PoA is a recorded gap | **partial** | batch issuance; per-presentation unlinkability analysis documented per path | fixture-based disclosure test per path |
| A3 | Topic 19 dashboard/log (DASH_01–DASH_03d, incl. no attribute values in the log, and RP identity fields from registration/access certificates) | yes | presentation journal and tier labels exist; certificate-derived RP fields do not | **partial** | registration-certificate-fed log fields (O3) | test asserting each DASH_03 field |
| A4 | Topic 53 ZKP_01 — generic predicate functions hiding all attributes | yes | implemented only for a boolean age predicate (`AgePredicatePolicy`, `verifier/go/oid4vp/vptoken_test.go` requires `f5` and rejects integers) | **partial** | generality beyond age predicates is out of scope and should be stated as such | tests already assert the negation cases |
| A5 | Topic 53 ZKP_02 — prove possession of an attestation of a given type | yes | doctype/namespace/attr-id binding enforced, wrong doctype refused | **supported** | none | existing tests |
| A6 | Topic 53 ZKP_03 — privacy-preserving binding of an attestation to a PID | no | not implemented; the fork's age attestations are issued separately from the PID by design | **missing** | would require an ACP-style binding scheme (ARF `ACP_03` says SHOULD use ZKP) | not implemented; needs a design first |
| A7 | Topic 53 ZKP_04 — derivation of a verifiable User pseudonym | no | not implemented | **missing** | needs a pseudonym scheme; the fork's spec covers pseudonyms only as a label/policy question | design + fixtures |
| A8 | Topic 53 ZKP_05 — usable in remote **and proximity** flows | yes | remote verified; proximity Tier 2 not demonstrated | **partial** | proximity ZK presentation | on-device, measured |
| A9 | Topic 53 ZKP_06 — proofs for already issued ISO 18013-5 / SD-JWT VC formats (SHOULD) | yes | mdoc yes (Longfellow over mdoc); SD-JWT VC with ZKP no — the fork's ZK path is mdoc-only; TS13 also defines a `zk-jwt` format the fork does not implement | **partial** | `zk-jwt` path if a relying party needs SD-JWT-based ZKPs | fixture round-trip |
| A10 | Topic 53 ZKP_07 — no additional tracking information | yes | one proof per presentation over the encrypted response; proof size and the `msoX5chain` residue are documented (`docs/profiles/…PROFILE.md` §constraint) | **partial** | an explicit unlinkability statement per path, backed by a test that no identifier is added | log/carrier inspection test |
| A11 | Topic 53 ZKP_08 — rely solely on ECCG Agreed Cryptographic Mechanisms v2.0 | yes | the scheme is the one the EU AV Profile mandates, but the v2.0 catalogue contains no ZKP scheme | **not verified** (unsatisfiable as written) | none technically; the fork's spec already tracks this tension — restate it in any programme conversation | re-check the catalogue when it is revised |
| A12 | Topic 53 ZKP_09 — no loss of LoA High capability | yes | compatible in principle; not tested | **partial** | LoA-High authentication test alongside a ZKP presentation | pilot test |
| A13 | Topic 54 ACC_01/ACC_02 — accessibility (Directive 2016/2102; Directive (EU) 2019/882; ETSI EN 301 549 presumption) | yes for a wallet/sandbox UI | PRODUCT.md commits to WCAG 2.1 AA; reduced-motion and screen-reader announcements implemented; **no ETSI EN 301 549 test evidence** | **partial** | an accessibility conformance test pass against EN 301 549 | third-party accessibility audit |
| A14 | Topic 55 CT_01–CT_06 — Certificate Transparency for access certificates | yes for the verifier's reader/access-certificate path | not implemented; the fork validates chains and CRLs | **missing** | SCT verification when access certificates are added | CT test vectors |
| A15 | Topic 56 WPSM_01– — wallet-provider monitoring and maintenance | no | operator duty; the fork is a PoC | **n/a** | none | — |
| A16 | Topic 18 `ACP_03` — cryptographic binding of attestations SHOULD use a ZKP | no | not implemented | **missing** | design work; out of the proof-of-age scope | — |

### 2.3 Technical specifications and standards

| id | requirement | applies? | current capability + evidence | status | required change | verification method |
|---|---|---|---|---|---|---|
| S1 | OpenID4VP 1.0 (Final, 9 Jul 2025) | yes | transcript per Appendix B.2.6.1 with cross-language golden vectors; DCQL; multi-credential sessions | **supported** | none | existing vectors |
| S2 | OpenID4VCI 1.0 (Final, 16 Sep 2025) | yes | library `eudi-lib-jvm-openid4vci-kt` 0.6.1; mock issuance paths | **partial** | real issuer; batch issuance | issuance interop test |
| S3 | HAIP 1.0 (Final, 24 Dec 2025) | yes | encrypted response by default, reversible only by an explicit logged flag; client-id prefix policy; per-presentation unlock | **partial** | a clause-by-clause HAIP statement | conformance checklist run |
| S4 | W3C Digital Credentials API (Working Draft 4 Sep 2026) + ISO/IEC 18013-7 Annex C | yes | DC API request handling, protocol dispatch, registry-doc-type-only registration, refusals distinguishable from cancellation in the fixed path | **partial** | AV-Profile-shaped DC API requests (`zk` protocol) covered by O1 | fixture-driven tests |
| S5 | ISO/IEC 18013-5:2021 (mdoc) | yes | mdoc issuance/consumption, `issuerAuth` verified on device with PKIX anchoring | **supported** | none | existing tests |
| S6 | ISO/IEC DIS 18013-5 second edition (`ZkRequest`/`ZkDocument`, §10.2.7 params) | yes | the fork implements the de-facto carrier and TS13's CDDL; the ISO text itself could not be read | **partial** | rename/adapt if the published edition differs | re-run vectors against the published edition |
| S7 | EC TS13 (arithmetic-circuit ZKP): `mso_mdoc_zk`, `zk-jwt`, `ZkSystemSpec`, `ZkDocument`, `longfellow-libzk-v1`, `circuit_hash` | yes | `mso_mdoc_zk` + `zk_system_type` + `longfellow-libzk-v1` + `circuit_hash` present in `verifier/go/oid4vp/dcql.go` and `DeviceRequestParser.kt`; `ZkDocumentData` shape documented | **partial** — `mso_mdoc_zk` supported, `zk-jwt` missing | `zk-jwt` if needed; a clause map | O1's clause-by-clause runner |
| S8 | EC AV Profile (`eu.europa.ec.av.1`, `av-vci://`, AVSUA at issuance, DC API transport) | yes | doctype, single mandatory attribute, ZK scheme and carrier supported (`CredentialType.AGE_VERIFICATION`, `AgePredicatePolicy`, wallet mints and consumes it); `av-vci://` invocation and AVSUA-equivalent issuance not verified present | **partial** | `av-vci://` invocation, AVSUA path, clause map | O1 conformance kit |
| S9 | EC TS3 (Wallet Unit Attestation) | yes | WUA handled in mocks; spec requirement `EE-WUA-007/008` written | **partial** | real WUA issuance | interop with a wallet-provider service |
| S10 | EC TS5/TS6 (RP registration formats and data) | yes for the RP side | not implemented; only client-id prefix policy | **missing** | consume registration data (O3) | fixture certificates |
| S11 | Status lists / revocation | yes | status-list handling split out; CRL soft-fail policy with a documented flip; per-doctype trust-store scoping | **partial** | end-to-end status-check demonstration against a real list | revocation test with a revoked fixture |
| S12 | ECCG Agreed Cryptographic Mechanisms v2.0 | yes | algorithms used are standard (P-256, ECDH-ES, AES-GCM, SHA-256) but no clause map exists and the catalogue covers no ZKP scheme | **partial** | a clause map for the non-ZKP primitives | algorithm inventory review |
| S13 | EC TS4 (ZKP implementation) position | context | the fork's spec cites it honestly | **n/a** | none | — |

### 2.4 Estonia-specific rows

| id | requirement / commitment | applies? | current capability + evidence | status | required change | verification method |
|---|---|---|---|---|---|---|
| E1 | Estonian acceptance deadline with TARA as the integration point (Ministry, 01.10.2026) | yes | no TARA adapter; the verifier is protocol-complete but institution-blind | **missing** | TARA-facing contract + RP register plug points (O2) | integration test against a TARA test client |
| E2 | RP register: request only what is registered (Ministry, 01.10.2026) | yes | not implemented; the wallet refuses two-predicate requests outright because it cannot read registrations | **missing** | O3 | certificate fixtures + refusal tests |
| E3 | Estonian statutory age thresholds and the 28 February rule (fork spec Annex A; `EE-POA-027`–`031`) | yes | rules implemented and tested in the wallet; accepted circuit set not published | **partial** | O6 | boundary tests per threshold |
| E4 | isikukood/date-of-birth minimisation | yes | the fork's central premise: the ZK path reveals one boolean, and `docs/analysis/…` plus spec Annex D explain why the isikukood must not appear | **supported** | none | existing negative tests (`f4`, integer, wrong attr refused) |
| E5 | Estonia's public-sector accessibility baseline (WCAG 2.1 AA) | yes for the sandbox UI | committed in PRODUCT.md; implemented browser-side | **partial** | audit evidence | accessibility audit report |
| E6 | Estonian wallet launch 2028; procurement unawarded | n/a | context only | **n/a** | none | — |

### 2.5 Alignment assessment narrative (source §4)

**eIDAS 2.0 obligations.** The duties that touch this code are the acceptance duty (L2) and the issuance
framework (L4/L5/L6). The fork is protocol-complete for acceptance and issuance in the
OpenID4VP/OpenID4VCI sense and missing the institutional layer that Estonian acceptance will actually
run on (TARA, RP register). The wallet-side duties (certification, monitoring) are provider duties and
are **not** met by a PoC; they are marked n/a rather than partial so that no one reads this matrix as a
compliance score. Because EUR-Lex returned HTTP 500 through the available web tooling on repeated
attempts, article-level citations could not be verified against the Regulation text itself; the dates
and duties used here therefore rest on RIA's and the Ministry's own statements and on the ARF
`[JUDGEMENT]`.

**ARF specifications.** Of the ARF areas that bear on the fork's role, two are effectively met (remote
presentation mechanics; possession and predicate binding), six are partial, four are missing and three
are not applicable. The striking result is A11: the ARF's own certificate requirement (`ZKP_08`) cannot
be satisfied by any scheme today, which is a finding about the specification, not about this fork, and
it should be reported upward rather than engineered around. Similarly A6/A7/A16 show that the fork
implements the *narrow* ZKP case (a boolean predicate on one attestation) and not the general ARF ZKP
vision (binding to a PID, pseudonyms, general attribute predicates). That narrowness is a deliberate,
documented choice and it is also the reason the work is achievable; it should be stated in exactly
those terms to any programme.

**Technical standards.** The fork's strongest alignment is with the Commission's own exploratory
technical specifications rather than with the settled OpenID standards: TS13's identifiers and CDDL
match, and the AV Profile's mandated system and parameter tuple match. Against the *final* standards
(OpenID4VP 1.0, OpenID4VCI 1.0, HAIP 1.0) the fork is a good citizen — encrypted responses by default,
bound transcripts, published JWKS, executable negative tests — but it has no clause-by-clause statement
against any of them, which is the cheapest remaining credibility gap to close.

**Certification and formal compliance: reserved.** Nothing in this assessment supports a compliance or
certification claim. The FCAF is itself under active development, and the certification scheme for
wallets is a duty of providers, not of a fork. The only sentence this repository can honestly make
today is: *these requirements are supported by these tests, in this environment.*

---

## 3. Use cases U1–U5 in full (source §5.2)

### U1 — Zero-knowledge age check at an Estonian online service (domestic, wallet-to-RP)

- **Actors:** holder (this wallet), relying party (an Estonian e-service, registered in the RP register
  once it exists), attestation provider (the mock issuer today; state provider later).
- **Trigger:** *(not a separate field in the source; derived from the workflow)* the RP issues an
  OpenID4VP request containing a `mso_mdoc_zk` DCQL query for `age_over_18`.
- **Flow / workflow:** RP requests `age_over_18` over OpenID4VP with a `mso_mdoc_zk` DCQL query; the
  wallet resolves the circuit, proves, encrypts to the RP's published key (`direct_post.jwt`); the
  verifier checks the transcript, the circuit allowlist, the proof, and returns `valid:true`.
- **Credentials/data:** `ee.riik.poa.1` or `eu.europa.ec.av.1`; disclosed: one boolean.
- **Dependencies:** circuit registry; RP registration; TARA-shaped session context (optional).
- **Outcome (expected):** the RP learns a boolean and nothing else; the wallet labels whether the
  presentation was ZK or a documented fallback.
- **Acceptance criteria / evidence:** e2e case 1 passes with the proof path; the RP's log contains no
  attribute value (ARF DASH_03a); a `false` predicate is refused rather than transmitted.

### U2 — Cross-border age check against the Commission's AV blueprint

- **Actors:** EU holder (an AV-profile wallet), Estonian RP, the AV solution's scheme owner.
- **Trigger:** *(derived)* the RP asks for the AV attestation via the DC API.
- **Flow / workflow:** RP asks for the AV attestation via the DC API; the holder proves `age_over_18`
  with `longfellow-libzk-v1`; the RP verifies with its own verifier.
- **Dependencies:** AV-profile conformance (O1); DC API `zk` protocol handling; accepted circuit set.
- **Outcome:** an Estonian service accepts a foreign AV proof without seeing a birth date.
- **Acceptance criteria / evidence:** the O1 kit reports the same result for this fork's verifier and
  for an external implementation; a non-`longfellow-libzk-v1` system is refused.

### U3 — Offline/proximity age proof at a venue

- **Actors:** holder, venue's reader device.
- **Trigger:** *(derived)* a proximity presentation at the venue's reader.
- **Flow / workflow:** proximity presentation with a ZK response instead of a plain mdoc.
- **Dependencies:** proximity Tier-2 implementation and measurement (currently pending and explicitly
  optional in the fork's spec).
- **Outcome:** the venue learns the boolean with no network dependency.
- **Acceptance criteria / evidence:** on-device measurement of BLE transfer for a ~360 KB proof inside a
  stated budget; refusal when a ZK response is impossible, with no silent downgrade.

### U4 — e-Resident or cross-border onboarding without a national identifier

- **Actors:** e-resident holder, Estonian service, issuer.
- **Trigger:** *(derived)* the service requests a PID attribute subset plus an AV attestation.
- **Flow / workflow:** the service requests a PID attribute subset plus an AV attestation, over two
  credentials in one session (the fork supports multi-credential sessions, verified by tests).
- **Dependencies:** real PID issuance; the register.
- **Outcome:** onboarding completes without the isikukood or full date of birth being disclosed.
- **Acceptance criteria / evidence:** the session's log names both credential ids; any missing credential
  fails the session with the id named; no isikukood appears anywhere in the wire transcript.

### U5 — A relying party integrating for the first time

- **Actors:** an Estonian service operator and its integrator.
- **Trigger:** *(derived)* first-time integration of an Estonian service against the wallet ecosystem.
- **Flow / workflow:** deploy the O2 acceptance kit, run the four cases (PID, AV attestation, unattested
  ZK request, unregistered RP), then wire the real register when it exists.
- **Dependencies:** O2; mocked register; a test wallet.
- **Outcome:** acceptance is demonstrated before 2028, and the register interface is proposed rather than
  guessed.
- **Acceptance criteria / evidence:** time-to-first-successful-verification measured and reported;
  refusals distinguishable from user cancellation.

### 3.1 The transition problems these use cases answer (source §5.1)

- **P1 — The acceptance window has no test target.** Estonian services must accept foreign wallets from
  end-2026, but the only Estonian wallet will not launch publicly until 2028 and the register they must
  check registrations against has no API. A service owner today cannot answer "does my service work?".
- **P2 — Age checks are the first collision point.** Age verification is the use case the EU has already
  specified in detail, and it is the one Estonian services most often need (alcohol and tobacco sales,
  gambling, online content). A plain date-of-birth disclosure from an Estonian isikukood is exactly what
  a minimisation-minded service wants to avoid.
- **P3 — The institutional mapping is unsettled.** RIA is the competent authority in a bill still before
  the Riigikogu; the register, the access certificates and the intermediary arrangements are all in
  design. Anything a third party builds now must degrade gracefully when the design lands.
- **P4 — X-Road is not the answer.** Teams reaching for the Estonian data-exchange layer as the wallet
  integration will not find an interface, because none is documented.

---

## 4. Integration pathways with Estonia's existing identity infrastructure (source §6)

| system | its actual role | documented interfaces | access requirements | trust relationship | integration constraint / blockers |
|---|---|---|---|---|---|
| **TARA** (state authentication service, RIA) | authentication broker for state e-services; carries ID-card, Mobile-ID, Smart-ID, EU eID; named by the Ministry as the service being updated so that state e-services can accept EUDI wallets `[VERIFIED]` | OIDC/OAuth2 (TARA's own surface); joining requires an application signed by a person with representation rights and an agreement with RIA `[VERIFIED]` | agreement with RIA; a registered service | RIA-operated; the RP trusts RIA's assertions | **No published EUDI adapter exists.** A wallet-facing integration must be built as a parallel path, not assumed inside TARA `[REC]`. Owner: RIA. |
| **X-Road / X-tee** | data-exchange layer between information systems; security servers expose REST and SOAP to service consumers and providers `[VERIFIED]` | Security Server message protocol (REST/SOAP); global configuration over HTTP; central services, TSAs and CAs `[VERIFIED]` | membership agreement signed in the self-service environment, a deployed security server, a service agreement `[VERIFIED]` | organisational trust between member organisations via certificates | **No documented relationship to the EUDI Wallet, OpenID4VP or OpenID4VCI** — 0 hits for "OpenID" on the three consulted sources `[VERIFIED as absence]`. **Proposed interfaces that lack verified support:** using X-Road to transport `vp_token`s, or to look up RP registrations. Do not design on that assumption. Owner: NIIS / X-Road operators. |
| **Relying Party Register** | ecosystem register of organisations permitted to request wallet data; registration limits which attributes may be requested `[VERIFIED]` | **none published**; access-certificate solution still being designed; MVP late 2026 / early 2027 `[VERIFIED]` | not yet defined | to be defined by RIA as competent authority | Any integration now must treat the register as an interface to be proposed, with a mock and fail-closed default `[REC]`. Owner: RIA. |
| **e-Residency / Digi-ID** | physical chip card for e-residents, activated in person, used with DigiDoc `[VERIFIED]` | DigiDoc/PKCS#11 stack; **no documented EUDI relationship** `[VERIFIED as absence]` | existing e-residency process | card issuer and PKI, not the wallet ecosystem | Constraint, not a pathway: e-residents cannot be assumed to hold a wallet credential; EUDI support must be an additive route. Owner: Republic of Estonia / PPA. |
| **Smart-ID / Mobile-ID (SK ID Solutions)** | private authentication and signing services; assurance assessed as "high" by an RIA-convened expert group `[VERIFIED]` | vendor SDKs and OIDC integrations | commercial agreement with SK ID Solutions | vendor-operated CA | A second authentication route, not an EUDI presentation route; useful for LoA-High steps alongside a wallet. Owner: SK ID Solutions. |

---

## 5. Quantified effects — Q1–Q5, verbatim (source §7)

Every figure below states its baseline, method, assumptions and confidence. Where the evidence does not
support a number, the entry says so and proposes the measurement instead. `[ESTIMATE]` figures are
produced by the stated arithmetic on verified inputs; they are not measured outcomes.

**Q1 — Development-effort saving from reusing the fork's verifier and carrier.**
Baseline: `verifier/go` is 47 files / 11,225 lines including tests; the Rust FFI is 215 lines;
`tests/vectors/carrier-v1/` holds 6 committed fixture files; `zk-conformance` is 699 lines
`[VERIFIED]`. Method: net new lines ÷ 1,200 net lines per engineer-month for reviewed, test-bearing
security code (a conventional single-engineer rate for this kind of work) ⇒ 11,440 ÷ 1,200 ≈ **9.5
engineer-months**, rounded to a range of **6–12 engineer-months**. Assumptions: the receiving team has
no equivalent carrier implementation; the fixtures transfer; nothing has to be re-derived from the ISO
text. Confidence: **low–medium** — the divisor is a rule of thumb, and the same work was produced here
in ~3.5 calendar weeks with AI assistance, so the number describes human re-creation cost, not the
fork's elapsed cost. `[ESTIMATE]`

**Q2 — Operating cost of verification.**
Baseline: measured 2,414 ms per valid proof on a 2 vCPU host, and mean 4.02 s / p50 3.83 s / p95 4.91 s
on an Intel i5-8365U `[VERIFIED]`. Method: treat verification as single-threaded work, so one 2 vCPU
instance sustains ≈ 1/2.4 ≈ **0.42 verifications/s ≈ 1,500 per hour**. For a peak of 10 verifications/s
(a plausible national-scheme burst, `[ASSUMPTION]`), that is ≈ 24 instances; at a cloud spot price of
€0.02–0.05 per vCPU-hour for a 2 vCPU instance (€0.04–0.10/hour), the steady-state peak cost is ≈
**€1–2.4 per hour of peak, i.e. €8.6k–21k/year if the peak is sustained 24/7** — an unrealistic bound;
a realistic duty cycle of 10 % gives **€0.9k–2.1k/year**. Confidence: **low** — the peak rate is
invented for illustration, prices move, and the fork's own `p95` figure already misses its 1.0 s budget
on its test host. The defensible action is to measure on the scheme's target hardware rather than to
reuse this number. `[ESTIMATE]` + proposed benchmark (§7.1).

**Q3 — Bandwidth.**
Baseline: a proof is ~353–360 KB and is encrypted into the response `[VERIFIED]`. Method: 1 M
presentations × 0.36 MB ≈ **360 GB** egress; at €0.05–0.12/GB that is **€18–43 per million
presentations**. Assumptions: the CBOR carrier is used and no compression applies. Confidence:
**medium** on the size (measured), **low** on the price. Practical effect: the size is a product
constraint for proximity (BLE transfer time is unmeasured — see §7.1) more than a cost constraint.
`[ESTIMATE]`

**Q4 — Usability.**
No defensible number can be given: the fork's own required measurements — approval-to-proof-ready
p50/p95/p99, peak prover memory, BLE throughput for a ~360 KB proof, and key generation in
TEE/StrongBox — are all recorded as **PENDING-DEVICE** in `docs/MEASUREMENTS.md` because hardware was
unavailable on 25 September 2026 `[VERIFIED]`. The harness (`ZkProofBenchmark.kt`) is committed and
JVM-green; only the `Prover` wiring waits. Any usability claim today would be inventing a result.
`[REC]` Run the programme; report per device.

**Q5 — Security benefits, stated qualitatively with evidence.**
On-device `issuerAuth` verification (leaf signature plus PKIX anchoring, with tests), LOTL fail-closed
trust, CRL soft-fail policy with documented flip, hardware-backed keys (StrongBox where available),
single-use sessions (replay returns `410`), fail-closed circuit allowlist, and JWE profiles pinned to
RFC 3394 §4.6 with a cross-language token `[VERIFIED]`. These are properties, not percentages; no
cost-avoidance figure is claimed. `[JUDGEMENT]`

### 5.1 Benchmarks to run instead of guessing (source §7.1) — proposed target and how measured

| # | benchmark | proposed target (exit criterion) | how it would be measured |
|---|---|---|---|
| B1 | **On-device proving programme** (harness exists) | figures reported per device, or the row explicitly left unmeasured | p50/p95/p99 approval-to-proof-ready, cold and warm, plus `VmHWM` peak memory, on the named device set (a Pixel 8 and one mid-range device per the plan) |
| B2 | **Verification capacity on the scheme's target hardware** | a stated verifications/second figure (per A3 M3 exit criterion); current host figure 0.42 verifications/s already misses the fork's own 1.0 s p95 budget | the fork's `BenchmarkMultipazVerify` plus percentiles, re-run on the RP's planned instance type |
| B3 | **Proximity transfer of a ~360 KB proof** over BLE, end to end | transfer inside a stated budget (U3 acceptance criterion) | end to end, with the wallet's own timestamps |
| B4 | **RP integration time** | measured and reported as a distribution over ≥ 3 integrators | time-to-first-successful-verification for an integrator using the O2 kit, measured with a stopwatch |
| B5 | **Register latency/availability impact** once the RP register interface exists | explicit, logged decision on stale data; never a silent allow | measured impact on every presentation, since a fail-closed lookup puts the register on the critical path `[JUDGEMENT]` |

---

## 6. Landscape the PRD must carry (source §2, condensed)

### 6.1 EU obligations, specifications and dates (source §2.1)

| item | version / date | relevance to the fork | evidence |
|---|---|---|---|
| European Digital Identity Framework — Regulation (EU) 2024/1183 amending 910/2014 | in force May 2024 | legal basis; article-level text **could not be fetched** (EUR-Lex returned HTTP 500 through the available web tooling, repeatedly) `[UNVERIFIED]` | EC wallet home page `[VERIFIED]` |
| Wallet availability duty | RIA states: 24 months after the relevant implementing acts; first acts in force 24 December 2024 ⇒ expected deadline end-2026 | dates the Estonian programme | RIA FAQ `[VERIFIED]` |
| Acceptance duties | Estonia: public sector from end-2026; private sector from end-2027 | the fork's verifier serves this role | RIA FAQ, Ministry 01.10.2026 `[VERIFIED]` |
| Implementing regulations referenced by the ARF v3.0.0 | CIR 2024/2977 (PID/EAA), 2024/2979 (integrity, core functionalities), 2024/2980 (notifications), 2024/2982 (protocols/interfaces), 2025/848; **amended by CIR 2026/1730, 2026/1731, 2026/1735** | the ARF release the fork's spec tracks | ARF v3.0.0 release notes, fetched `[VERIFIED]` |
| Certification of wallets | CIR 2024/2981; the Commission's Functional Conformance Assessment Framework (FCAF) at `conformance.eudi.dev`, content "under active development" | no certification pathway in the fork | FCAF home `[VERIFIED]` |
| Architecture and Reference Framework (ARF) | **v3.0.0** — changelog date 21 July 2026, GitHub release published 23 July 2026; introduces the FCAF | requirement source for ZKP, accessibility, CT, WPSM | eudi.dev/latest + GitHub API `[VERIFIED]` |
| ARF Topic 53 — ZKP high-level requirements | ARF v3.0.0 Annex 2 §A.2.3.31: **ZKP_01–ZKP_09** (eight SHALL, one SHOULD) | the fork's core claim | annex fetched in full `[VERIFIED]` |
| ARF Topic G (Discussion Paper, ZKP) | v1.4, 30 March 2025, explicitly **not normative**; refinement round open (Iteration 6, 23 Sep – 18 Nov 2026) | route by which ZKP requirements may change | eudi.dev discussion topics `[VERIFIED]` |
| EC technical specification TS4 — ZKP implementation | v1.0 21 May 2025, v1.0.1 30 January 2026: "At present, no existing ZKP approach can be deemed fully suitable or mature enough for direct integration into the EUDI Wallet" | states the Commission's own maturity position | TS4 fetched `[VERIFIED]` |
| EC technical specification TS13 — ZKP from arithmetic circuits | v1.0 15 December 2025: defines `mso_mdoc_zk`, `zk-jwt`, `ZkSystemSpec`, `ZkDocument`, system `longfellow-libzk-v1`, param `circuit_hash`; "not intended to be a final specification", to be handed to ETSI TS 119 476-2 | the carrier the fork implements | TS13 fetched `[VERIFIED]` |
| EC technical specification TS14 — ZKP from multi-message signatures | v1.0 27 February 2026, exploratory; also destined for ETSI TS 119 476-2 | the BBS-family alternative the fork does not implement | TS14 fetched `[VERIFIED]` |
| **EC Age Verification solution — AV Profile (Annex A)** | `av-doc-technical-specification` repo pushed 2 September 2026; doctype `eu.europa.ec.av.1`; `longfellow-libzk-v1` mandatory; DC API transport | the single closest EU artefact to the fork's capability | fetched `[VERIFIED]` |
| OpenID4VP 1.0 | Final, 9 July 2025 | the fork's presentation protocol; **contains no ZK text** (programmatic search over the full spec text: 0 hits) | spec fetched `[VERIFIED]` |
| OpenID4VCI 1.0 | Final, 16 September 2025 | issuance | spec fetched `[VERIFIED]` |
| OpenID4VC High Assurance Interoperability Profile (HAIP) 1.0 | Final, 24 December 2025 | the fork follows parts of it (encrypted response, client-id policy) without claiming the profile | spec fetched `[VERIFIED]` |
| OpenID4VP issue #627 ("Support for mdoc ZKP requests") | open, created 3 June 2025 | the standards gap the carrier fills | GitHub issue `[VERIFIED]` |
| openid/dchp issue #17 ("ZKP Support") | open, label `parking-lot`, created 27 July 2026; "ZKP support was removed from the initial proposal" | why no OpenID spec covers this yet | GitHub issue `[VERIFIED]` |
| ISO/IEC 18013-5 second edition | expected publication **Q1 2027** per the Commission's own tracker (2026-09-22 update; WG10 to approve FDIS 2026-09-30); defines `ZkRequest`/`ZkDocument` per TS13's summary | the structure the carrier approximates; `iso.org` could not be fetched `[UNVERIFIED from ISO]` | EC STS tracker issue #84 `[VERIFIED]` |
| Google Longfellow ZK (`longfellow-zk/longfellow-zk`) | Apache-2.0, v0.9 (31 March 2026), pushed 21 October 2026; independent reviews including Trail of Bits (report dated 2025-08-18) and an ISRG-reported MDOC circuit under-constraint patched in v0.8.4 | the proving library the EU AV Profile mandates | repo + reviews page `[VERIFIED]`; IETF draft `draft-google-cfrg-libzk-03` claims ≥ 115 bits for the `(p256, 132, 7)` profile `[VERIFIED as quoted from the draft]` |

**Standards conflict that must not be smoothed over (source §2.1).** ARF `ZKP_08` requires a ZKP scheme to
"rely solely on algorithms included in the [ECCG Agreed Cryptographic Mechanisms v2.0]", and that
catalogue — version 2.0, April 2025 (the ARF refers to it as v2, 2025-05) — contains no ZKP,
anonymous-credential, BBS or zero-knowledge scheme at all `[VERIFIED — the catalogue's text was fetched
and searched]`. As written, therefore, no scheme satisfies `ZKP_08`, including the `longfellow-libzk-v1`
scheme that the Commission's own AV Profile makes mandatory. Reported as an unresolved tension in the
source material; it does not change the engineering recommendation, but no one should tell a programme
that `ZKP_08` is "met".

### 6.2 Estonia's documented position (source §2.2)

| item | what is documented, verbatim where it matters | evidence |
|---|---|---|
| Responsible body | RIA (Riigi Infosüsteemi Amet) runs the EUDI Wallet programme, launched the procurement, and is designated competent authority in the draft act | RIA pages; Ministry 01.10.2026 `[VERIFIED]` |
| Status | "Eestis on digikukru arendamine jõudnud hankeetappi" — at tender stage; announced 18 May 2026; submission deadline 29 June 2026; roughly one year to contract signature if undisputed; procurement ID 10227365; five-year service | RIA FAQ (page updated 30.09.2026), RIA news 18.05.2026 `[VERIFIED]` |
| Award | **no award announcement found** as of 23.10.2026 | RIA pages, RIA sitemap, press searches `[VERIFIED as absent]`; register unreadable `[UNVERIFIED]` |
| Value | €21.65 M appears **only** in a Postimees "Sisuturundus" (sponsored) article of 4 June 2026; no official source states a value | `[VERIFIED that it is press-only]` |
| National launch | "Eesti digikukru avalik kasutuselevõtt on kavandatud 2028. aastasse." | Ministry 01.10.2026 `[VERIFIED]` |
| Acceptance | public sector from end-2026, private sector from end-2027; TARA to be updated for acceptance; Digikukkur to be a separate mobile app | Ministry 01.10.2026; RIA FAQ `[VERIFIED]` |
| Legal instrument | a bill amending the **e-identimise ja e-tehingute usaldusteenuste seadus** (Estonia's eIDAS transposition), the State Fees Act and the Penal Code; RIA as competent authority for supervision, permits, register, notifications; Digikukkur added to the Penal Code's list of important personal documents | Ministry 01.10.2026 `[VERIFIED]`; no Riigi Teataja text retrievable (site is client-rendered to the tooling) `[UNVERIFIED]` |
| Relying Party Register | part of the ecosystem; MVP targeted late 2026 / early 2027; registration in the register limits which attributes may be requested; market survey with Turu-uuringute AS from September 2026; **access-certificate solution still being designed; no published technical spec or API** | RIA programme page; Ministry `[VERIFIED]` |
| Programmes | POTENTIAL (2023 onward; RIA's own news) and **APTITUDE** (Estonia joined spring 2026); 2023 MVP built by Cybernetica AS; the PoC code is upstream of this fork | RIA projects page `[VERIFIED]`; participant count conflict — see §8 |
| eID ecosystem | ID-card ≈ 1.2 M valid (RIA, July 2026) vs 1,383,604 (id.ee counter, 16.10.2026); Mobile-ID ≈ 219,000 (RIA) vs 215,398 (id.ee); Smart-ID in Estonia 799,658 (id.ee, 22.10.2026); Digi-ID issuance to residents ended 1 May 2025, e-residents still receive cards; Smart-ID and Mobile-ID are SK ID Solutions products | RIA eID page; id.ee; e-resident.gov.ee `[VERIFIED]` |
| TARA | state authentication service: ID-card, Mobile-ID, Smart-ID, EU eID; cross-border EU eID since autumn 2018; Smart-ID+ since 26 Feb 2026; joining requires a signed agreement with RIA; to be updated for EUDI acceptance | RIA authentication services page; Ministry `[VERIFIED]` |
| X-Road | data exchange layer; Security Server offers REST and SOAP; joining requires a membership agreement, a security server, and a service agreement; **no documented relationship to the EUDI Wallet or OpenID4VP/OpenID4VCI** (0 hits for "OpenID" on the three consulted sources) | RIA X-tee page, x-road.global/architecture, X-Road repo `[VERIFIED]` |
| e-Residency | Digi-ID card, physical, activated in person, used with DigiDoc; **no official source ties e-Residency or Digi-ID to the EUDI Wallet** | e-resident.gov.ee, RIA eID page `[VERIFIED as absence]` |
| Estonian ZK / age-verification work | **none found** in any reachable official or press source; Cybernetica's cryptography page is a service statement ("secure multi-party computation, zero knowledge proofs…"), not a project | RIA studies page, Cybernetica, press searches `[VERIFIED as absence]` |

### 6.3 Tooling ecosystem gaps, with who is hurt (source §2.3)

| gap | who experiences it | practical consequence | evidence |
|---|---|---|---|
| No published ZK carrier in any OpenID specification | wallet and verifier implementers | each pair invents a dialect; interop is announced, not tested. OID4VP #627 open ~16 months; ZKP removed from DCHP's initial proposal | `[VERIFIED]` |
| ARF `ZKP_08` references a catalogue with no ZKP scheme | programme owners, auditors | an "is this compliant?" answer cannot be given at all; compliance language must be avoided | `[VERIFIED]` |
| TS4 states no ZKP approach is mature enough for direct integration | procurement teams | an EU programme cannot simply mandate ZKP today; it must fund pilots and evidence, which is what the AV Profile is doing | `[VERIFIED]` |
| ISO 18013-5 second edition still unpublished (EPD slid to Q1 2027) | everyone building on `ZkRequest`/`ZkDocument` | design churn risk; documentation must cite drafts and say so | `[VERIFIED]` |
| The Estonian RP register has no interface | Estonian service owners; the fork's O2 work | acceptance cannot be automated or tested; every service builds its own guess | `[VERIFIED]` |
| X-Road has no EUDI relationship | anyone assuming the Estonian data layer solves wallet integration | a false architectural premise in planning documents | `[VERIFIED as absence]` |
| Wallet-side ZKP is optional in the Commission's own library ("an optional feature that can be enabled in your application") | relying parties expecting ZK proofs | an RP cannot assume a wallet can prove; the fork's own `EE-ZKP-055a` handles the unattested case as least-trusted | `[VERIFIED]` |
| Verification cost is seconds, not milliseconds | relying parties at scale | budgets must be planned around ~2.4 s per verification on 2 vCPU; the fork's own 1.0 s p95 target is missed on its test host | `[VERIFIED]` |

---

## 7. Conflicts, unresolved questions, and what could not be verified (source §8)

### 7.1 Conflicts found and reported rather than resolved

| conflict | positions | effect on this analysis |
|---|---|---|
| ID-card count | RIA prose: ≈ 1.2 M valid (July 2026); id.ee live counter: 1,383,604 (16.10.2026) | affects any addressable-market sizing; both are official sources. Use ranges, never a point figure |
| APTITUDE participants | RIA projects page: 117 organisations from 13 countries; the APTITUDE site: "more than 141 public and private organisations from 13 European countries" | does not change Estonia's participation; quoted with both figures when a number is needed |
| ARF v3.0.0 date | CHANGELOG: 21 July 2026; GitHub release published 23 July 2026 | cite both; immaterial to content |
| `ZKP_08` vs the ECCG catalogue | the ARF requires algorithms from a catalogue that lists no ZKP scheme, while the AV Profile mandates `longfellow-libzk-v1` | decisive for wording: never tell a programme that `ZKP_08` is satisfied |
| Longfellow's standardisation state | AV Annex B (July 2026) reports the scheme is not yet a CFRG work item, while the datatracker shows an active IRTF research-group document as of 22 October 2026 | affects roadmap risk, not the technical plan; treat as moving |
| Procurement value | €21.65 M appears only in sponsored press | never present it as official |

### 7.2 Unresolved questions (source §8)

- Has the procurement been awarded? No announcement was found; the register was unreadable through the
  tooling. `[UNVERIFIED]`
- What will the RP-register API look like, and will access certificates be CT-logged (ARF Topic 55)?
- Will the RIA programme adopt the EU AV Profile for age checks, or the fork's `ee.riik.poa.1`, or both?
  The fork's spec prefers the AV attestation for every RP; no Estonian statement says otherwise, and none
  endorses it either.
- Will ISO/IEC 18013-5 second edition ship the structures the carrier approximates, and when? The
  Commission's tracker shows the expected date slipping to Q1 2027.
- Would an Estonian service be permitted to accept a *foreign* AV attestation before the Estonian
  register exists? Regulatory question, not a technical one. `[UNVERIFIED]`

### 7.3 Access failures, recorded as required (source §8)

- **Firecrawl `/v1/search` stopped returning results.** The first two searches at ~03:44 UTC succeeded;
  from ~03:50 UTC onward every query returned `{"data": [], "warning": "No search results found"}`
  regardless of phrasing, including queries that had worked minutes earlier. **Consequence and
  mitigation:** discovery shifted to `/v1/scrape` and `/v1/map` against known authoritative endpoints
  (RIA, the Commission's digital-building-blocks space, `eudi.dev`, OpenID), to the authenticated GitHub
  API (`gh api`, `gh search`), and to a delegated research pass that used the same endpoint. External
  *content extraction* — the part the task requires — worked throughout.
- **EUR-Lex returned HTTP 500** through Firecrawl for every variant tried (`eli/reg/2024/1183/oj`, the
  CELEX HTML and PDF forms, the consolidated 910/2014 text). **Consequence:** no article-level citation
  of Regulation (EU) 2024/1183 appears in this document; obligation dates come from RIA, the Ministry and
  the ARF, and are labelled as such.
- **`iso.org` returned no content** ("All scraping engines failed") for the 18013-5 pages.
  **Consequence:** ISO second-edition status and structure are taken from the Commission's own tracker
  and TS13, and are labelled `[UNVERIFIED from ISO]`.
- **The tender register (`riigihanked.riik.ee`) returned HTTP 500**; the Estonian press sites' search
  endpoints were mostly unusable (Angular shells, 404s, or ignoring the query), and `riigiteataja.ee`
  renders client-side to the tooling. **Consequence:** award status and statutory text are unverified,
  and are reported as such rather than assumed.
- **Delegated research passes.** Three delegated passes (competitor matrix, ZKP/standards landscape,
  Estonia programme) supplied evidence that is labelled wherever it was not re-fetched by the author:
  the ISO tracker quotes, the OpenID issue quotes, the Cybernetica MVP note, the Postimees articles, and
  the ECCG catalogue search. Load-bearing claims were re-fetched independently: the ARF Topic 53
  requirements, TS4/TS13/TS14, the AV Profile, the Ministry statement, the RIA pages, the OpenID spec
  publication dates, and all repository evidence. `[JUDGEMENT]`

---

## 8. Cross-cutting tradeoffs the PRD inherits (source A6)

**Changes that serve both objectives:** AV-Profile clause registry and fixtures (O1); registration-
certificate policy engine (O3) — implements the Estonian register's stated rule before the register
exists; circuit registry (O4) — single source for "which circuits do we accept"; carrier conformance
suite (O5) — lets Estonian services test a foreign wallet before 2028; fixing the stale conformity row
and banning "conformant" language (R3); device measurement programme (D2/R4) — answers the first question
any procurement asks about a wallet.

1. **Narrow-age vs broad-generality.** Innovation would favour extending toward ARF's general ZKP vision
   (binding to a PID, pseudonyms, general attribute predicates — A6, A7, A16). Estonia's near-term need
   is a single boolean at a relying party. **Recommendation: stay narrow and say so.** The general work
   depends on schemes and specifications that do not exist; a narrow implementation that is measured
   beats a broad one that is asserted. `[JUDGEMENT]`
2. **Estonian attestation vs EU attestation.** The fork's own spec already chooses to prefer
   `eu.europa.ec.av.1` for every relying party. That maximises cross-border alignment and makes the
   Estonian `ee.riik.poa.1` the fallback — which is the right order for interoperability and the riskier
   order politically, since an Estonian programme may want its own credential first.
   **Recommendation: keep the spec's order, keep both paths, and let the programme choose.** `[JUDGEMENT]`
3. **Register on the critical path.** A fail-closed register lookup (E2) is the privacy-correct design
   and it makes every presentation depend on a service that does not exist yet (A4 risk R9).
   **Recommendation: interface-first with mocks, a bounded cache, and an explicit, logged decision about
   stale data — never a silent allow.** `[JUDGEMENT]`
4. **Evidence speed vs evidence depth.** Publishing an external conformance result early (O5) buys
   credibility quickly but risks a public failure; running the full device programme first (D2) buys
   defensible numbers but delays any external claim. **Recommendation: publish the carrier suite result
   first (it is expected to pass), and hold performance claims until D2 completes.** `[JUDGEMENT]`
5. **Procurement timing.** If the Estonian procurement is awarded while this work is in flight, the
   winning provider will set the technical direction and the RP-side kit (O2) may be superseded. That is
   a reason to finish D3 (the interface proposal) early, so the proposal is on the table rather than
   reconstructed. `[JUDGEMENT]`

---

## 9. Evidence-label glossary carried into this extract

`[VERIFIED]` tool output or quoted primary source · `[JUDGEMENT]` reasoned position, not measured ·
`[ESTIMATE]` number with stated method and baseline · `[ASSUMPTION]` premise taken as given ·
`[UNVERIFIED]` not established · `[REC]` recommendation · `[VERIFIED as absent]` / `[VERIFIED as an
absence]` a documented search that returned nothing.
