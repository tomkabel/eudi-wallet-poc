---
source: docs/research/2026-10-23-innovation-assessment.md (612 lines, v1.0, 23 October 2026)
extracted: 2026-10-23
extractor: PRD research subagent
purpose: PRD input — product-relevant intent, faithful to the source's numbers, quotes and evidence labels
labels: "[VERIFIED] tool output / quoted primary source; [JUDGEMENT] author's analysis over verified facts; [ESTIMATE] number with stated method; [ASSUMPTION] unverified premise; [UNVERIFIED] could not be established; [REC] recommendation"
---

# Structured extract — Innovation assessment (fork of `open-eid/eudi-wallet-poc` with a ZK proof-of-age spine)

Extraction discipline: every label, number and quotation below is the source's own. Nothing has been
upgraded. Where the source's own arithmetic does not close, the file is marked **[EXTRACT NOTE —
DISCREPANCY]** and the source's figure is preserved unchanged.

Source metadata: research date 23 October 2026 (Europe/Tallinn), version 1.0. Subject
`github.com/tomkabel/eudi-wallet-poc`, branch `ee-eudiw-zk-import`, HEAD
`2d38febab0a89857a759485b3465dc218d2510c6` (12 October 2026); fork of
`github.com/open-eid/eudi-wallet-poc` at `0866e946874f666caa40e85ac8fc8fb22d71fc0` (v0.6.6,
26 May 2026). Companion documents the source names: `2026-10-23-estonia-alignment-assessment.md`
and `2026-10-23-shared-appendices.md` (competitive matrix A1, requirements traceability matrix A2,
roadmap A3, risk register A4, source register A5). Those companions were **not** read for this
extract; pointers to them are reproduced only where the source itself relies on them.

---

## 1. Thesis (what this document says the product is and should become)

The fork "is not a copy of the Estonian PoC with extra commits". It is a roughly **3.5-week build**
(18 September – 12 October 2026) `[VERIFIED]` that turns the upstream Android demo into a
**four-component system** — Android wallet, Go verifier, Python issuer, Python holder — around
**one narrow, unusually well-specified claim**:

> "an ISO 18013-5 mdoc age predicate proved with a Longfellow zero-knowledge proof, carried in
> OpenID4VP, with the verifier and the wallet both refusing to accept less." `[VERIFIED]`

Build size `[VERIFIED]`: **216 commits** over the upstream release, **480 files**,
**+62,578 / −2,286 lines**.

Five findings the assessment presents as driving everything (all `[VERIFIED]` except where marked):

1. **The fork's carrier is the Commission's own ZKP dialect.** The fork's `mso_mdoc_zk` profile
   (v1.0, 25 September 2026) fixes the DCQL format identifier `mso_mdoc_zk`, a
   `meta.zk_system_type` registry, the proving system `longfellow-libzk-v1` and the `circuit_hash`
   parameter — and the Commission's **TS13** ("ZKP based on arithmetic circuits", v1.0,
   15 December 2025) specifies exactly `"format": "mso_mdoc_zk"`, `"system": "longfellow-libzk-v1"`,
   `params.circuit_hash`, plus a `ZkDocument` CDDL matching the fork's documented response shape.
   The `zk_system_type` field names are the same strings in the fork's Go verifier
   (`verifier/go/oid4vp/dcql.go`) and Kotlin wallet
   (`app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt`). "This is not a private
   convention: it is the emerging EU profile." `[VERIFIED]`
2. **The EU's own age-verification blueprint mandates exactly the scheme the fork already runs.**
   The Commission's AV Profile (Annex A, `av-doc-technical-specification`, repository pushed
   2 September 2026) fixes the doctype `eu.europa.ec.av.1`, the single mandatory attribute
   `age_over_18`, the ZkSystemSpec parameters `circuit_hash, version, num_attributes, block_enc_hash,
   block_enc_sig`, transport over the W3C Digital Credentials API, and states that "the only
   Zero-Knowledge Proof system in scope is the system identified by `longfellow-libzk-v1` … An
   implementation SHALL use that system." `[VERIFIED]` The fork already mints and consumes
   `eu.europa.ec.av.1`, keys its age policy on it, and prefers it to its own Estonian attestation
   `[VERIFIED]`. The source's conclusion: "The gap between the fork and the EU's age-verification
   blueprint is a **conformance-test and packaging gap, not a cryptographic redesign**."
3. **The market gap is verifier- and relying-party-side, not wallet-side.** Across ten verified
   comparators (Appendix A1) none combines ZKP age proofs with a Go/Rust verifier and a published
   negative-control test suite: the Commission's reference wallet documents ZKP as "an optional
   feature", multipaz ships the prover library but not a verifier product, and walt.id, EUDIPLO,
   inji, wwWallet and impierce claim no ZKP at all `[VERIFIED]`. "The distinctive artefact here is
   the **verifying** side plus its refusals."
4. **The honest-measurement discipline is itself the smallest-risk differentiator.**
   `[JUDGEMENT]` — "A funding or procurement evaluator cannot easily manufacture that; it is the
   difference between a demo and evidence." Evidence cited below (§6, §9).
5. **The fork's weakest position is upstream abandonment and standardisation timing.** Upstream had
   one commit in the last 12 months, 0 open issues and PRs, 3 stars and a single contributor as of
   23 October 2026 `[VERIFIED]`; ISO/IEC 18013-5 second edition — which defines the `ZkRequest` /
   `ZkDocument` structures the whole approach leans on — has an expected publication date that has
   slipped from Q3 2026 to Q1 2027 per the Commission's own tracker `[VERIFIED]`; ZKP was explicitly
   removed from the DCHP initial proposal, leaving OpenID4VP issue #627 open for ~16 months
   `[VERIFIED]`. "**Every innovation below is therefore a bet on an unfinished standard.**" The
   source's mitigation: "not to wait but to be the implementation that a standardisation venue tests
   against, which is what the carrier profile and fixtures position the fork to be."

---

## 2. Target users / JTBD named by the source

| Opportunity | Named target user (source wording) | Job the product does for them |
|---|---|---|
| O1 | "A national wallet programme (Estonia's RIA procurement team), an attestation provider issuing proof-of-age, or a relying party that must accept the EU age-verification blueprint and wants to test against a real implementation rather than a description." | Test a claim of AV-Profile conformance instead of asserting it; a programme adopting the EU blueprint can reuse the fork's evidence rather than rebuild the test oracle. |
| O2 | "Estonian e-service operators and their integrators — the public-sector services that must accept certified wallets from end-2026, and the private-sector services under the later obligation." | Accept *other Member States'* wallets with no Estonian wallet to test against; run acceptance tests years before the national wallet ships. |
| O3 | "Wallet providers and relying parties that must act on the relying party's registered intended use rather than on the request's own claims." | Replace a blanket refusal with a lawful, explainable decision; satisfy an ARF logging duty at the same time. |
| O4 | "Wallet providers, relying parties and programme owners who must agree on which circuits are acceptable, and auditors who must answer 'which circuit proved this?' months later." | Agree one accepted circuit set from one document; give auditors a provenance answer. |
| O5 | "Implementers of wallets and verifiers who must interoperate on a ZK carrier that no OpenID specification yet defines." | Resolve "which carrier" integration surprises; cheap credibility via published negative-control results. |
| O6 | "Attestation providers and wallet programmes in Member States whose law has several age thresholds, and relying parties that must ask for one of them." | Turn "the relying party asked for 21" into the right single predicate without ever disclosing a `false` one. |

PRD-relevant inference from the above (the source's own emphasis, not an addition): **the primary
buyer is a programme/regulator-adjacent actor or an integrator, not an end user**; the wallet holder
appears only as the subject of privacy guarantees (no `false` predicate transmitted, at most one
predicate per presentation).

---

## 3. What the fork is, against upstream (source §2 table, verbatim content)

| dimension | upstream `open-eid/eudi-wallet-poc` | this fork |
|---|---|---|
| licence / visibility | MIT, public, 3 ★, 1 contributor | MIT, public (fork visibility is fixed at creation), 0 ★ |
| last activity | 2026-05-26 (v0.6.6); 1 commit in 12 months; 0 open issues/PRs `[VERIFIED]` | 2026-10-12; 216 commits ahead of upstream `master`, 0 behind `[VERIFIED]` |
| components | Android wallet app (`app/`) | app + Go verifier (`verifier/go`, 47 files / 11,225 lines) + Rust FFI staticlib (`verifier/zkverify-ffi`, 215 lines) + Python issuer (542 lines) + Python holder (469 lines) + stdlib sandbox server (644 lines) + JVM conformance module (`zk-conformance`, 699 lines) `[VERIFIED]` |
| protocols | OpenID4VP/siop via `eu.europa.ec.eudi` libraries, OpenID4VCI, mdoc, proximity reader | the same libraries (siop-openid4vp 0.11.1, sdjwt 0.12.0, openid4vci 0.6.1, prex 0.4.0), plus multipaz 0.99.0 and `mso_mdoc_zk` with a dual-carrier (interim JSON + de-facto CBOR `DeviceResponse`) verifier `[VERIFIED]` |
| ZKP | none | Longfellow prover (`ee_poa_demo`, built from a pinned `../longfellow-zk`), Go verifier over the Rust staticlib, circuit allowlist, `ZkDocument` carrier, on-device prover via `multipaz-longfellow` `[VERIFIED]` |
| attestations | upstream PID/mDL mocks | plus `ee.riik.poa.1` (fork's Estonian proof-of-age) and `eu.europa.ec.av.1` (the Commission's AV Profile doctype) `[VERIFIED]` |
| specification | none in-tree | `spec/EE-EUDIW-TS-1.0.md`, 284 KB, 211 numbered requirements across 14 areas (EE-ZKP 147, EE-POA 136, EE-SEC 62, EE-PRO 59, EE-PRI 35, EE-SIG 30, EE-GOV 27, EE-PID 26, EE-ONB 22, EE-REV 19, EE-WUA 18, EE-RP 17, EE-CNF 17, EE-ACC 7) `[VERIFIED]`, revision 1.7 dated 12 October 2026 |
| tests | upstream unit tests | 27 Kotlin unit-test files / 169 `@Test`, 5 JVM conformance tests, 13-case `tests/e2e.sh`, drain and load harnesses, committed cross-language transcript vectors and `tests/vectors/carrier-v1/` `[VERIFIED]` |
| "green" | upstream CI | `make check` — gofmt, `go vet`, `compileall`, ruff, shellcheck, `go test -race` (oid4vp, internal/cborsub, internal/jose, circuits), `wallet/test_transcript.py` (3 golden vectors), `issuer/test_mint_device_key.py`, `sandbox/test_age.py` — re-run in a venv with the pinned inputs (`cbor2==6.1.4`, `cryptography==50.0.1`, `jwcrypto==1.6.1`) on 23 October 2026: **exit 0** `[VERIFIED]` |

**[EXTRACT NOTE — DISCREPANCY]** The 14 per-area requirement counts listed by the source sum to
**622**, while the same sentence states "**211** numbered requirements across 14 areas". The
assessment does not reconcile the two. Both figures are reproduced here as given; do not quote
either as settled without checking `spec/EE-EUDIW-TS-1.0.md`.

**Inherited / fork-specific / planned / missing — the source keeps these strictly separate.**

- *Inherited from upstream (not a differentiator):* the wallet application skeleton, the
  `eu.europa.ec.eudi` JVM library integration, the mock PID/issuance services, the proximity reader,
  the `app/` build system. Upstream's `master` is an ancestor of the fork's HEAD
  (`git merge-base --is-ancestor` verified), so nothing upstream was traded away `[VERIFIED]`.
- *Fork-specific and verified in code or tests:* the ZK spine (prover, verifier, FFI, circuits
  registry, LLVM-free allowlist), the carrier profile and dual carriers, `direct_post.jwt` with an
  in-house minimal JWE (ADR-003), the EE-PoA and AV attestation paths, hardware-backed SecureArea
  keys, LOTL fail-closed trust with CRL soft-fail, on-device `issuerAuth` verification, the
  measurement harness, and the specification itself `[VERIFIED]`.
- *Documented plans:* the roadmap items in the specification §21 and the step records in
  `docs/planning/`; the DCHP #17 submission text prepared but **unfiled**
  (`docs/profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md`) `[VERIFIED]`.
- *Missing (verified absent, not merely undocumented):* no wallet-provider certification path
  (`CIR (EU) 2024/2981` / the Commission's FCAF at `conformance.eudi.dev`), no RP registration
  certificate consumption beyond `ClientIdPrefixPolicy`, no proximity path for Tier-2 ZK, no
  end-to-end real-issuer integration (issuance is served by mocks and the demo issuer), and no
  published interoperability run against the Commission's reference wallet `[VERIFIED as absent
  from the repository; a claim that no such run exists anywhere would be [UNVERIFIED].]`

**One documentation/code divergence found during the assessment [REC].** `docs/CONFORMITY.md:160`
still records on-device `issuerAuth` validation as "absent — mdoc credentials load with no
`issuerAuth` chain check", while `CredentialToDocumentMapper.kt:82-114` performs exactly that check
(leaf-signature validation through `org.cose` plus PKIX chain anchoring at the configured IACA roots)
and `CredentialToDocumentMapperMdocVerificationTest.kt` asserts both the anchored and the unanchored
case `[VERIFIED]`. The source's recommendation: "entries that contradict the code do not help the
fork's credibility argument and this one should be corrected before the note is shown to an
evaluator." The file was touched by the HEAD commit and the row is still stale.

---

## 4. Method and scoring rubric (source §3) — needed to read the priorities honestly

- **Candidate generation.** Comparator set selected for overlapping users, capabilities or
  integration role, then verified field by field from repository READMEs, in-repo documentation and
  the GitHub API (Appendix A1). **Ten comparators were verified.** Complementary components
  (multipaz, EUDIPLO, impierce/openid4vc, the Commission's TS/AV documents) are labelled as such
  and **not counted as direct alternatives**.
- **Novelty framing (scope limit).** "Every claim of novelty below is scoped to this comparison set —
  the fork, its upstream, the ten comparators, and the EU specification artefacts fetched on
  23 October 2026. A broader novelty claim ('nobody anywhere has done this') is not supported by the
  evidence and is not made." Where a comparator's capability is *unknown* rather than absent the
  matrix says "not verified" and the opportunity is scored down. "a README silence about ZKP is not
  proof that a project has none, so the differentiation scores below rest on artefacts that were
  read, not on silences."
- **Rubric (1–5, applied independently by the author from the evidence in §2 and A1).**

| score | user impact | technical differentiation | implementation feasibility | Estonia relevance |
|---|---|---|---|---|
| 5 | unblocks a regulated transaction that cannot be completed today, or removes a disclosure with legal consequence | no comparator in the set does it, and it is not a copy of a public artefact | ≤ 2 engineer-weeks reusing existing components, evidence already committed | on a documented Estonian milestone or obligation (RIA / Ministry statement, or the spec's own roadmap) |
| 4 | materially shortens a workflow or removes a disclosure users would notice | one comparator partially does it | 1–2 engineer-months | supports an Estonian obligation indirectly |
| 3 | saves effort for integrators | several comparators do a weaker version | 3–6 engineer-months | useful; not on the critical path |
| 2 | measurable but minor | commodity | 6–12 engineer-months | no Estonian driver found |
| 1 | cosmetic | already standardised and freely available | blocked on a third party nobody here controls | irrelevant to Estonia |

- **Ranking formula (source's own).** `priority = 0.35·impact + 0.25·differentiation + 0.20·feasibility
  + 0.20·Estonia relevance`; weights "favour user impact and differentiation because the fork is a
  research artefact whose value is its evidence, not its throughput; feasibility is held at 0.20 so
  that externally-blocked work cannot reach the top by being cheap to describe."
- **Evidence confidence reported separately from score** (high score on thin evidence ≠ high score on
  measured facts). **High** = enabling capability verified in code or tests *and* the external driver
  is a fetched primary source; **medium** = one of the two missing; **low** = driver is a delegated
  research pass, a press report, or a draft standard that may yet change.

**[EXTRACT NOTE — DISCREPANCY]** Applying the source's stated formula to its own rubric scores gives
O1 4.80, O2 4.35, O3 4.15, O4 4.05, O5 4.00, O6 3.80; the source publishes 4.75, 4.30, 4.10, 4.05,
3.95, 3.80. The **rank order is identical**; only the printed values are slightly lower than the
formula yields. The PRD should cite the ranking, not treat the decimals as reproducible.

---

## 5. Opportunities, in detail

Each entry preserves: target user, problem + evidence, existing approaches and their limits, proposed
capability and affected components/interfaces, distinction from upstream and comparators, acceptance
criteria (= candidate functional requirements / success metrics), implementation approach + effort +
dependencies, benefits/risks/validation, scores and confidence.

### O1 — EU AV-Profile conformance kit: `eu.europa.ec.av.1` + `longfellow-libzk-v1` over the DC API
**Priority 4.75** (impact 5, differentiation 5, feasibility 4, EE relevance 5). **Confidence: high.**

- **Problem and evidence.** "The Commission's AV Profile is normative, not exploratory": doctype
  `eu.europa.ec.av.1`; attribute set `age_over_18` (mandatory) plus optional `age_over_NN`; "A Proof
  of Age Attestation SHALL NOT include any other attribute"; the ZKP system in scope is
  `longfellow-libzk-v1` and "An implementation SHALL use that system"; proof transported over the W3C
  Digital Credentials API; parameters are the ZkSystemSpec tuple at ISO/IEC DIS 18013-5 second edition
  §10.2.7 `[VERIFIED]`. The fork implements the cryptographic and carrier parts and the *Estonian*
  profile around them, but there is "**no conformance test suite, no profile-conformance statement
  against the AV document, and no published interop run against the Commission's or any third party's
  AV implementation**" `[VERIFIED as absent]`. Consequence: "A programme that adopts the EU blueprint
  therefore cannot reuse the fork's evidence as-is; it would rebuild the test oracle."
- **Existing approaches and their limits.** The Commission publishes the AV documentation and a
  development environment (`ageverification.dev`); an open-source implementation of the proving
  library is referenced from the profile (`google/longfellow-zk`, now `longfellow-zk/longfellow-zk`,
  Apache-2.0, v0.9 31 March 2026) `[VERIFIED]`. "What is not visible in any fetched artefact is an
  independent, **negative-control** conformance kit for the *combined* profile — the tests that
  demonstrate a wrong `circuit_hash`, a `false` predicate, a replayed proof or an unanchored
  `issuerAuth` are refused." The fork already contains those tests for its own carrier
  (`verifier/go/vptoken_test.go`, `tests/e2e.sh` cases 2/4/5/9/10) `[VERIFIED]`; they are not
  generalised to the AV document's parameter names or published as a conformance artefact.
- **Proposed capability.** An `av-profile-conformance` package in this repository with (a) a
  machine-readable statement of each AV-Profile normative clause with a test id, (b) fixtures minted
  against `eu.europa.ec.av.1` with the AV Profile's `ZkSystemSpec` parameter tuple, including the
  container-format examples from the profile's Annex A, and (c) a runner that produces a pass/fail
  report per clause for any candidate wallet, issuer or verifier. **Affected components:** Go verifier
  (query parsing and required-parameter validation), issuer (attestation data model and doctype
  registry), wallet (DC API request handling), `tests/vectors/`. **Interfaces:** DC API
  (`navigator.credentials.get` with the `org-iso-mdoc`/`zk` protocol entry), the AV profile's
  `av-vci://` invocation, and the AVSUA-equivalent attestation the profile expects at issuance (the
  fork's spec already carries `EE-WUA-008` for this) `[VERIFIED]`. **Dependencies:** nothing outside
  the repository; ISO second-edition clause numbering is reached through the AV Profile's own citation
  rather than through ISO, "whose site could not be fetched through the available tooling"
  `[UNVERIFIED]`.
- **Distinction.** Upstream has no ZKP at all `[VERIFIED]`. Within the comparator set only
  `eudi-lib-android-wallet-core` documents ZKP — as "an optional feature that can be enabled in your
  application" `[VERIFIED]` — and no comparator publishes a negative-control conformance kit over
  `longfellow-libzk-v1`. "The distinction is therefore not the scheme (the scheme is mandated by the
  EU, not invented here) but the *verification evidence*: the fork would be the first artefact in this
  set that lets a third party check a claim of AV-Profile conformance rather than assert it."
- **Acceptance criteria.** (i) the kit runs against **at least two independent implementations** (this
  fork's verifier and at least one external wallet or verifier) and reports a clause-by-clause result;
  (ii) **≥ 20 named clauses executed, of which ≥ 8 are negative controls that fail closed**;
  (iii) a third party reproduces the report from a clean checkout using only documented commands, in
  one sitting.
- **Effort.** **3–6 engineer-weeks** `[ESTIMATE]`, method: derived from the size of the existing
  comparable slice — the carrier profile and its dual-carrier fixtures
  (`verifier/go/oid4vp/carrier.go` plus `tests/vectors/carrier-v1/`) were built inside a 216-commit,
  3.5-week campaign; "this work is a generalisation of that slice, not a new one."
- **Benefits / risks / validation.** Benefit: converts existing code into a capability an EU programme
  can procure against, **with no cryptographic risk**. Risks: the AV Profile is revised (the fork's own
  spec records a 1 September 2026 revision `[VERIFIED]`), so the kit must pin a document version and
  treat clause drift as a maintenance cost; the ISO second-edition text is not reachable, so parameter
  semantics are "taken at one remove". Validation: run the kit against the Commission's AV development
  environment and publish the result, **including failures**.

### O2 — Estonian relying-party acceptance kit: accept EUDI wallets before the national wallet exists
**Priority 4.30** (impact 5, differentiation 4, feasibility 3, EE relevance 5). **Confidence: high on
the obligation dates, low on the register interface's final shape.**

- **Problem and evidence.** Estonia's own wallet launch is planned for **2028** ("Eesti digikukru
  avalik kasutuselevõtt on kavandatud 2028. aastasse"), while "Juba 2026. aasta lõpust peavad Eesti
  riiklikud e-teenused olema valmis aktsepteerima Euroopa Liidu liikmesriikide sertifitseeritud
  digikukruid", with RIA's **TARA** as the stated central integration point and the private sector from
  the end of 2027 `[VERIFIED]`. RIA confirms three Member States were ahead and had applied for
  cross-border approval as of May 2026 `[VERIFIED]`. Consequence: "a window in which Estonian relying
  parties must accept *other Member States'* wallets with no Estonian wallet to test against, and no
  published Estonian RP-side reference implementation" `[VERIFIED as absent from the fetched RIA pages
  and the `open-eid` organisation]`. "The Relying Party Register itself is at MVP stage with its
  access-certificate solution still being designed, and no technical specification or API for it is
  published" `[VERIFIED]`.
- **Existing approaches and their limits.** EUDIPLO is the closest verified comparator — open-source
  middleware bridging a backend to EUDI wallets, OpenID4VCI 1.0 / OpenID4VP 1.0, OIDF conformance
  tested, mdoc and SD-JWT VC both marked supported, **proximity explicitly out of scope** `[VERIFIED]`.
  "Using it does not answer the Estonian question, because it neither speaks TARA nor consumes the
  Estonian RP register, and it is a dependency rather than an artefact a service owner can inspect for
  its own refusals."
- **Proposed capability.** An RP-side integration kit: a thin OpenID4VP 1.0 / DC API relying-party
  service built from this fork's Go verifier, plus (a) a TARA-facing adapter contract, (b) the
  RP-register lookup as a pluggable, **fail-closed** interface with a documented "no entry ⇒ refuse"
  default, and (c) an acceptance test suite covering the four requests an Estonian service will
  actually meet: **a foreign wallet with a PID; a foreign wallet with an AV attestation; an unattested
  ZKP request** (the least-trusted case the fork's spec already governs in `EE-ZKP-055a` `[VERIFIED]`);
  **and a request whose RP registration is missing.** Affected components: `verifier/go/oid4vp`,
  `verifier/go/present.go`, and a new `rp-kit/` deployment profile. Interfaces: OpenID4VP 1.0
  `direct_post.jwt`, the DC API, TARA's OIDC surface (the fork's spec records TARA as the Estonian
  authentication broker `[VERIFIED]`), and the RP register API — "**which does not exist in published
  form, so the kit must define the interface and ship a mock**" `[VERIFIED as absent]`.
- **Distinction.** Upstream has no verifier at all — the Go verifier is fork-specific `[VERIFIED]`.
  EUDIPLO covers the protocol bridge but not the Estonian institutional layer. No comparator in the set
  was found to ship a TARA-side or register-aware acceptance kit `[VERIFIED for the ten comparators]`.
  "Within the researched set, this is the only opportunity whose buyer already has a dated obligation
  and no product."
- **Acceptance criteria.** (i) a sample Estonian-style service completes all four request cases with a
  documented, machine-readable result; (ii) the missing-registration and unattested-request cases
  refuse, and **the refusal is distinguishable from cancellation in the UI contract** (the fork's own
  `DcApiRequestDispatch` work shows why this matters); (iii) an integrator with no prior ZK knowledge
  installs the kit from the README **in under a day** and reproduces the four results. **Timed.**
- **Effort.** **6–10 engineer-weeks** `[ESTIMATE]`, method: the verifier is 11,225 lines of Go
  including tests; the new work is configuration, two pluggable interfaces with mocks, and four
  end-to-end cases — "roughly the size of the existing dual-carrier slice plus the sandbox deployment
  work in the commit history." Dependencies: RIA's RP register interface (unpublished), TARA
  integration terms, and a test wallet on the Estonian side — **all three external**; the kit is
  designed so mocks let the work proceed without them, and "the first external dependency to become
  real replaces a mock, not a design."
- **Benefits / risks / validation.** Benefit: lets Estonian services run acceptance tests years before
  the national wallet ships; gives RIA a working target for the register interface. Risks: the
  register's eventual specification may differ from the mock; the kit could be superseded if RIA
  publishes its own. Mitigation: publish the interface contract as a proposal, "which is useful even if
  superseded." Validation: run the four cases in front of an Estonian service owner and measure
  **time-to-first-successful-verification**.

### O3 — Registration-certificate-aware request policy engine
**Priority 4.10** (impact 5, differentiation 4, feasibility 3, EE relevance 4). **Confidence: medium**
— "the need is verified from a primary ARF source, the wallet-side absence from the repository, but the
certificate profile's stability is not."

- **Problem and evidence.** The fork's own specification refuses requests that would disclose more than
  one age predicate *unless* "the relying party's registered intended use names each of them", and
  records plainly that the wallet does not read registration certificates, so such a request is
  "always refused here" `[VERIFIED]` (`AgePredicatePolicy.kt`, `EE-POA-030`). Two consequences: the
  wallet is **stricter than ISO/IEC 18013-5 §7.2.5** (a deliberate divergence the spec documents in
  §22) `[VERIFIED]`; and it **cannot distinguish a legitimate two-predicate request from an
  over-reaching one**. In parallel the ARF's **Topic 19** requires a presentation log to contain the
  relying party's **trade name, unique identifier, service identifier, Member State, intended use and
  privacy-policy URL** — information that lives in the registration and access certificates the fork
  does not yet read `[VERIFIED]`. The Estonian RP register is being built with exactly this rule
  ("tohivad küsida ainult registreeringus märgitud andmeid") `[VERIFIED]`.
- **Existing approaches and their limits.** Registered-intended-use enforcement is being placed in the
  ecosystem's registries (the ARF's RP registration topics, the Estonian register), not in wallets; "a
  wallet therefore inherits the obligation without an implementation to copy." No comparator in the set
  was found to ship an intended-use policy engine `[VERIFIED for the ten comparators]`. The fork's
  `ClientIdPrefixPolicyTest` shows the shape of the work — "a small, testable policy class over client
  identifiers — applied to a much weaker signal than a certificate."
- **Proposed capability.** A policy module shared by the wallet and the verifier that (a) parses the
  registration/access certificate chain, (b) exposes a typed `RegisteredIntendedUse`, (c) drives the
  existing age-predicate decision and the Topic 19 log fields from it, and (d) **fails closed when the
  certificate is absent, unparseable, or names no use**. Affected components:
  `app/.../domain/presentation/` (`AgePredicatePolicy`, `HolderObligations`, presentation screens),
  `verifier/go/oid4vp` (request authentication), `certificates` parsing in `app/.../security/`.
  Interfaces: the ARF's registration certificate profile, ISO 18013-5 `readerAuth`, and the Topic 19
  log contract.
- **Distinction.** Upstream has nothing comparable; the fork today has "a conservative approximation and
  says so" `[VERIFIED]`. "The differentiation is precision under a legal rule, not a new protocol."
- **Acceptance criteria.** (i) a two-predicate request bearing a certificate whose intended use names
  both predicates **is allowed, with a user-facing explanation of the revealed range**; (ii) the same
  request without a certificate, or with one that does not name both, **is refused**; (iii) the
  presentation log contains the Topic 19 fields populated from the certificate, verified by a test
  asserting **each field**; (iv) **no regression in the existing 169 Kotlin unit tests**.
- **Effort.** **8–14 engineer-weeks** `[ESTIMATE]`, method: certificate parsing and policy work of
  similar shape in this repository (the LOTL/CRL trust work and `ClientIdPrefixPolicy`) was completed
  within the 3.5-week campaign; "this is larger because it introduces a data model." Introduced as an
  interface first (`IntendedUseSource`) with the certificate implementation and a fixture generator
  second, "so that the Estonian register or the EU registries can be plugged in without redesign."
  Dependencies: a reachable registration-certificate profile (ARF annexes) and, for the Estonian case,
  the future register API.
- **Benefits / risks / validation.** Benefit: replaces a blanket refusal with a lawful, explainable
  decision and satisfies a documented ARF logging duty simultaneously. Risks: certificate profiles in
  the ARF are still moving; "**a mis-parsed intended use could *loosen* a privacy control, so the
  default must be refusal and the loosening must be covered by explicit negative tests**." Validation:
  property tests over generated certificates, plus a manual review of the log output against the Topic
  19 field list.

### O4 — Circuit-hash registry and distribution service
**Priority 4.05** (impact 4, differentiation 5, feasibility 4, EE relevance 3). **Confidence: high.**

- **Problem and evidence.** Both TS13 and the AV Profile require the prover system and the circuit to be
  identified by parameters — `circuit_hash` in TS13's ZkSystemSpec, and the tuple `circuit_hash,
  version, num_attributes, block_enc_hash, block_enc_sig` in the AV Profile `[VERIFIED]` — while the AV
  Profile states that "the set of circuits accepted for the purposes of this profile, identified by
  their circuit hashes, is published and maintained **by the scheme owner separately from this
  document**" `[VERIFIED]`. "In other words the standard deliberately leaves a distribution and trust
  problem to the ecosystem." The fork today solves it locally: an allowlist compiled into the verifier
  (`verifier/circuits.json`, `verifier/go/circuits/`, `GET /circuits`), a per-doctype trust-store
  scoping, and a `Circuit.SpecID()` label format
  `<system>_<version>_<num_attributes>_<block_enc_hash>_<block_enc_sig>_<circuit_hash>` `[VERIFIED]`.
  "Every operator doing this independently is how two parties end up accepting different circuits while
  both believing they follow the same profile."
- **Existing approaches and their limits.** Trust-list distribution in the EUDI ecosystem is handled for
  providers (notification, trusted lists) but **not for circuits**; the Commission's specifications name
  the gap and stop. No comparator was found to publish a circuit registry service `[VERIFIED for the
  ten comparators]`. longfellow-zk itself ships circuits with the library, so "which circuits exist" is
  answerable only by reading a specific library version `[VERIFIED]`.
- **Proposed capability.** A minimal, **signed** circuit registry: a static JSON document listing
  accepted circuits (system, version, attributes, block-encryption parameters, hashes, minimum library
  version), published with a signature and an HTTP endpoint the verifier and the wallet can fetch,
  validate against a pinned key, and audit. Affected components: `verifier/go/circuits`, the verifier's
  startup self-check, the wallet's `ZkPresenter` scheme resolution, and `docs/profiles/`. Interfaces:
  the existing `GET /circuits` shape, "promoted from a local list to a fetched, signed list with the
  local list retained as a pinned fallback (fail-closed)."
- **Distinction.** Neither upstream nor any verified comparator ships this; TS13/AV name the need
  `[VERIFIED]`. "The novelty is small in code and large in interoperability effect — it converts a
  per-operator convention into a checkable artefact."
- **Acceptance criteria.** (i) a verifier started with **no network and no local list refuses
  everything (fail-closed)**, with a log naming the missing prerequisite; (ii) with a fetched and
  signature-valid list it accepts **exactly** the listed circuits, demonstrated by a test that flips
  **one byte** in the list and observes refusal; (iii) **two independently operated instances** configure
  the same accepted set from one document; (iv) an audit query returns the circuit provenance for a
  given presentation.
- **Effort.** **4–8 engineer-weeks** `[ESTIMATE]`, method: comparable to the existing LOTL
  fetch/validate work (`FetchLOTLCertificates.kt`, `CertificateChainValidator`), completed inside the
  campaign. Dependencies: none external, "but adoption depends on a scheme owner (the Commission or a
  Member State) agreeing to host a list — so **ship it as a community registry with a documented
  succession path**."
- **Benefits / risks / validation.** Benefit: removes an interoperability landmine; gives auditors a
  provenance answer. Risks: an unsigned or compromised list is a **supply-chain hazard** — mitigation is
  signature verification with a pinned key and fail-closed behaviour; "maintaining a list nobody adopts
  is dead weight" — mitigation is to keep the document generator in-repo and cheap. Validation: a
  two-instance agreement test plus a tamper test.

### O5 — `mso_mdoc_zk` carrier conformance suite (both carriers, published vectors)
**Priority 3.95** (impact 4, differentiation 4, feasibility 5, EE relevance 3). **Confidence: high.**

- **Problem and evidence.** OpenID4VP 1.0 Final (9 July 2025) contains **no ZK text at all**
  `[VERIFIED — programmatic search of the 398,599-character specification text]`; the request to extend
  DCQL for mdoc ZK proofs (**OpenID4VP issue #627**) has been open since **3 June 2025**; and in DCHP,
  "ZKP support was removed from the initial proposal" with the issue parked `[VERIFIED]`. Meanwhile two
  carriers exist in the field — the interim JSON envelope and the CBOR `DeviceResponse` with
  `zkDocuments` — and the fork already normalises both behind one interface with committed golden
  vectors (`tests/vectors/carrier-v1/`, `TestCarrierTableBothCarriers`,
  `TestGoldenProofNormalizesIdentically`, `TestCarrierVectorsVerifyIdentically`) `[VERIFIED]`. TS13
  defines the CDDL for the `ZkDocument`, "so the shape is now pinned by a Commission document even
  though the transport binding is not."
- **Existing approaches and their limits.** The Commission's own conformance site
  (`conformance.eudi.dev`) publishes the FCAF for wallets, and its content is explicitly "under active
  development" `[VERIFIED]`; **nothing fetched covers the ZK carrier**. "Consumers therefore write
  their own tests, which is why 'which carrier' is a recurring integration surprise."
- **Proposed capability.** Publish the existing vectors and normalisation tests as a small, documented
  conformance suite with a runner: given a wallet's response, report which carrier it used, whether the
  proof verifies, and which binding properties hold (**client_id, nonce, response_uri, key thumbprint,
  single use, predicate value**). Affected components: `verifier/go/oid4vp/carrier.go`, `present.go`,
  `tests/vectors/`, and a thin CLI. Interfaces: OpenID4VP `direct_post`/`direct_post.jwt` and the DC API.
- **Distinction.** "This is packaging of existing, tested work; the differentiation is that the tests are
  *negative* (a wrong nonce, a flapped bit, a replayed token, a plain response to an encrypted session)
  rather than a happy-path demo" — verified in `tests/e2e.sh` cases 2–5 and 9–10 `[VERIFIED]` and absent
  from every comparator's published material in this set `[VERIFIED]`.
- **Acceptance criteria.** (i) the suite runs against this repository's wallet and verifier and reports
  **all 13 existing cases**; (ii) **at least one external implementation** (the Commission's reference
  wallet or multipaz sample) is exercised, **with the result published whether it passes or fails**;
  (iii) a failing case **names the binding property that failed, not a generic "invalid"**.
- **Effort.** **2–4 engineer-weeks** `[ESTIMATE]` — "this is the smallest opportunity because the tests
  exist." Method: wrap `tests/e2e.sh` and the Go tests in a documented runner; add the external target
  last. Dependencies: an external implementation to test against (multipaz sample apps are available,
  Apache-2.0 `[VERIFIED]`).
- **Benefits / risks / validation.** Benefit: cheap credibility; "a published failure against a reference
  implementation is more useful to a programme than another claim of success." Risks: the carrier may be
  superseded by whatever ISO second edition and DCHP settle on, "so the suite must be version-labelled."
  Validation: run it, publish the report, including the external result.

### O6 — Statutory age-threshold toolkit beyond 18
**Priority 3.80** (impact 4, differentiation 4, feasibility 3, EE relevance 4). **Confidence: medium** —
"the need and the rules are verified, the legal threshold list for Estonia is analysis, and circuit
acceptance is external."

- **Problem and evidence.** The AV Profile focuses on `age_over_18` and treats `age_over_NN` as optional,
  while pointing at ISO/IEC 18013-5 §7.2.5 ("nearest 'true' attestation above request") for
  implementations that use it `[VERIFIED]`. The fork's specification is stricter: **at most one predicate
  per presentation, `false` predicates withheld**, and the **28 February birth-date rule** it derived
  from ISO §7.2.5, across an Estonian statutory annex `[VERIFIED — EE-POA-027–EE-POA-031, spec Annex A]`.
  "What does not exist anywhere in the fetched material is a *set of accepted circuits* covering the
  thresholds a national law actually uses, plus a negotiation library that turns 'the relying party asked
  for 21' into the right single predicate without ever disclosing a `false` one — **which is exactly where
  wallets get it wrong**."
- **Existing approaches and their limits.** Circuits travel with the proving library; the AV profile's
  accepted set is maintained "by the scheme owner … separately from this document" `[VERIFIED]`; "nothing
  fetched enumerates a multi-threshold set with its legal mapping."
- **Proposed capability.** A documented threshold-to-circuit map for Estonia's statutory ages, an
  accepted-circuit set for those thresholds (**subject to O4's registry**), and a small negotiation
  library implementing §7.2.5's "nearest true above request" rule with the fork's stricter single-predicate
  and withhold-false rules, with tests including the 28 February and incomplete-date cases the spec already
  analyses `[VERIFIED]`.
- **Distinction.** AV Profile covers 18; no comparator was found to publish a multi-threshold toolkit; the
  fork's own rules are **stricter than ISO §7.2.5** and documented as a deliberate divergence `[VERIFIED]`.
- **Acceptance criteria.** (i) for each mapped threshold, a proof verifies only for a holder whose age meets
  it, demonstrated by **table-driven tests at the boundary (the day before, the day of, a leap-day
  birth)**; (ii) a request for a threshold the wallet does not hold is **refused with a user-visible
  reason, never answered with a neighbouring threshold**; (iii) **no `false` predicate is ever
  transmitted**, asserted over all fixtures.
- **Effort.** **6–12 engineer-weeks** `[ESTIMATE]` **plus an unbounded external wait for scheme-owner
  acceptance**; feasibility scored 3 "for that reason" — "Circuit parameters per threshold are the long
  dependency: a circuit must be generated and accepted by the scheme owner, so this work is partly
  externally gated." Dependencies: O4's registry; longfellow circuit generation; a legal review of the
  threshold list.
- **Benefits / risks / validation.** Benefit: "closes a real gap for Estonian services that are not
  age-18-gated (**alcohol and tobacco at 18/21, other restrictions at 16**)." Risks: extra circuits raise
  prover cost and proof size (already **~353–360 KB and ~6.1 s proving** `[VERIFIED]`); "legal mapping
  errors are consequential" — mitigation is to publish the legal mapping **with its citations** and treat
  any unmapped threshold as unsupported. Validation: boundary tests per threshold plus a legal review by
  the programme owner.

---

## 6. Ranking table (source §5, reproduced verbatim)

| # | opportunity | impact | diff. | feas. | EE rel. | priority | evidence confidence | external blocker |
|---|---|---|---|---|---|---|---|---|
| O1 | EU AV-Profile conformance kit | 5 | 5 | 4 | 5 | **4.75** | high | none |
| O2 | Estonian RP acceptance kit | 5 | 4 | 3 | 5 | **4.30** | high (dates) / low (register API) | RP register interface, TARA terms |
| O3 | Registration-certificate policy engine | 5 | 4 | 3 | 4 | **4.10** | medium | ARF certificate profile stability |
| O4 | Circuit-hash registry service | 4 | 5 | 4 | 3 | **4.05** | high | a scheme owner to adopt it |
| O5 | `mso_mdoc_zk` carrier conformance suite | 4 | 4 | 5 | 3 | **3.95** | high | an external implementation to test |
| O6 | Statutory age-threshold toolkit | 4 | 4 | 3 | 4 | **3.80** | medium | circuits accepted by a scheme owner |

---

## 7. Order of work / initial release (source §5) — the source's explicit build sequence

**Initial release: O1, O5, O2 — "in that order".**

- **O1** "because it is the only opportunity that converts code already in the tree into a capability the
  EU recognises, with no external gate — and because the AV Profile's normative text names the exact
  scheme and parameter set the fork uses" `[VERIFIED]`.
- **O5** "because the tests exist, the cost is 2–4 engineer-weeks `[ESTIMATE]`, and an honest published
  result against an external implementation is the strongest available credibility signal for everything
  else."
- **O2** "because it is the only one an Estonian buyer can act on before 2028, and its deliverable — four
  documented acceptance cases plus a proposed register interface — has value even if RIA never adopts it"
  `[JUDGEMENT]`.

**Second wave: O3 and O4** — "both reduce risk in others' integrations, and **O4 is a prerequisite for
O6**." **O6 "should wait for O4 and for a decision by whoever owns the accepted circuit set."**

Interpretation stated by the executive summary: "O1 is the only one that converts existing code into an
EU-recognised capability, O5 is nearly complete already, and O2 is the only one whose value an Estonian
buyer can act on before the national wallet ships in 2028."

---

## 8. Non-goals / explicitly out of scope, as the source states them

The source never writes a "non-goals" heading; these are the exclusions and scope limits it does state,
each with its label. They are safe to promote to PRD Non-Goals only with the label preserved.

- A broader novelty claim ("nobody anywhere has done this") "is not supported by the evidence and is not
  made"; novelty is scoped to the fork + upstream + ten comparators + EU specification artefacts fetched
  23 October 2026 `[JUDGEMENT/scope limit]`.
- **Proximity is out of scope** where the source notes it: EUDIPLO marks proximity explicitly out of
  scope `[VERIFIED - comparator fact]`, and the fork has **no proximity path for Tier-2 ZK**
  `[VERIFIED as absent from the repository]`.
- **No wallet-provider certification path** exists (`CIR (EU) 2024/2981` / FCAF at `conformance.eudi.dev`)
  — absent `[VERIFIED as absent]`.
- **No end-to-end real-issuer integration** — issuance is served by mocks and the demo issuer
  `[VERIFIED as absent]`.
- **No published interop run against the Commission's reference wallet** `[VERIFIED as absent]`.
- The DCHP #17 submission text is "prepared but **unfiled**" `[VERIFIED]`.
- O6's multi-threshold work is deliberately gated behind O4 and scheme-owner acceptance; "any unmapped
  threshold [is to be treated] as unsupported."
- O1's dependency note: ISO second-edition clause numbering is reached through the AV Profile's citation,
  not through ISO directly `[UNVERIFIED]`; "a claim that no such run exists anywhere would be
  [UNVERIFIED]" is likewise a scope boundary on the absence claims.

---

## 9. Performance, quality and other measurable facts (NFR-relevant material in the source)

- **Budget miss, published honestly.** `EE-ZKP-040` budgets **≤ 1.0 s p95**; measured **p95 4.91 s on an
  i5-8365U** `[VERIFIED]` — the fork "publishes host measurements that contradict its own budgets".
- **Documented defect and its fix.** The fork documents a defect "that inverted an earlier claim — that
  refusing an unaccepted circuit cost **54–58 s for one circuit version instead of milliseconds** —
  together with the **24 September 2026 fix**" `[VERIFIED]`.
- **Proof cost.** Proof size **~353–360 KB** and **~6.1 s proving** `[VERIFIED]` (quoted again under O6 as
  the reason extra circuits raise prover cost).
- **Test estate.** 27 Kotlin unit-test files / 169 `@Test`; 5 JVM conformance tests; 13-case
  `tests/e2e.sh`; drain and load harnesses; committed cross-language transcript vectors;
  `tests/vectors/carrier-v1/` `[VERIFIED]`.
- **Green state.** `make check` exit 0 on 23 October 2026, in a venv with `cbor2==6.1.4`,
  `cryptography==50.0.1`, `jwcrypto==1.6.1` `[VERIFIED]`.
- **Cross-cutting behaviours the source treats as invariants across opportunities** (each verified in the
  fork today, each carried into acceptance criteria): **fail-closed** on missing/unparseable trust input
  (O3, O4, O2 register lookup); **negative controls** as first-class deliverables (O1 ≥ 8, O5, O6);
  **no `false` predicate ever transmitted** and **at most one predicate per presentation** (O6, EE-POA
  rules); **refusal must be distinguishable from cancellation** (O2, UI contract); **LOTL fail-closed
  trust with CRL soft-fail** and **hardware-backed SecureArea keys** as existing fork properties.

---

## 10. Compliance / regulatory surface named in the source

- **Commission AV Profile** (Annex A, `av-doc-technical-specification`, repository pushed 2 September
  2026): doctype `eu.europa.ec.av.1`; `age_over_18` mandatory, `age_over_NN` optional; "A Proof of Age
  Attestation SHALL NOT include any other attribute"; only ZKP system in scope is `longfellow-libzk-v1`;
  "An implementation SHALL use that system"; transport over the W3C Digital Credentials API;
  ZkSystemSpec tuple `circuit_hash, version, num_attributes, block_enc_hash, block_enc_sig`; accepted
  circuit set published by the scheme owner separately from the document; `av-vci://` invocation;
  AVSUA-equivalent attestation at issuance. A revision dated 1 September 2026 is recorded in the fork's
  own spec `[VERIFIED]`. Development environment: `ageverification.dev`.
- **Commission TS13** "ZKP based on arithmetic circuits", v1.0, 15 December 2025: `"format":
  "mso_mdoc_zk"`, `"system": "longfellow-libzk-v1"`, `params.circuit_hash`, `ZkDocument` CDDL `[VERIFIED]`.
- **ISO/IEC 18013-5 second edition**: defines `ZkRequest`/`ZkDocument`; §10.2.7 parameter semantics; §7.2.5
  "nearest 'true' attestation above request"; expected publication slipped **Q3 2026 → Q1 2027** per the
  Commission's tracker `[VERIFIED]`; the ISO text itself **could not be read** (`iso.org` returned no
  content through the available web tooling) `[UNVERIFIED]`.
- **OpenID4VP 1.0 Final** (9 July 2025): contains no ZK text (programmatic search of the
  398,599-character text) `[VERIFIED]`; issue **#627** open since 3 June 2025.
- **DCHP**: "ZKP support was removed from the initial proposal" `[VERIFIED]`; the fork's #17 issue text is
  unfiled.
- **ARF**: Topic 19 presentation-log fields (trade name, unique identifier, service identifier, Member
  State, intended use, privacy-policy URL) `[VERIFIED]`; Topic G refinement round is open; requirement
  **`ZKP_08`** demands schemes relying solely on the **ECCG Agreed Cryptographic Mechanisms v2.0**, and
  "that catalogue contains no ZKP scheme — so as written the requirement is **unsatisfiable by any
  scheme, including the one the EU's AV Profile mandates**" `[VERIFIED]`.
- **`CIR (EU) 2024/2981`** and the Commission's FCAF at `conformance.eudi.dev` — the wallet-provider
  certification path the fork does not have `[VERIFIED as absent]`.
- **Estonian obligations** (RIA): state e-services ready to accept certified EU wallets **from end-2026**;
  private sector **from end-2027**; national wallet launch planned **2028**; TARA as the central
  integration point; three Member States ahead with cross-border approval applications as of May 2026;
  Estonian RP register at MVP stage with access-certificate solution still being designed and no published
  API; register rule "tohivad küsida ainult registreeringus märgitud andmeid" `[VERIFIED]`.
- **longfellow-zk** (`google/longfellow-zk`, now `longfellow-zk/longfellow-zk`), Apache-2.0, v0.9
  31 March 2026 `[VERIFIED]`.
- **Note on licence posture:** fork and upstream are **MIT, public**; "fork visibility is fixed at
  creation" `[VERIFIED]`. No licence risk is raised by the assessment.

Also carried from the fork's own spec: `EE-ZKP-055a` (unattested ZKP request — the least-trusted case),
`EE-WUA-008` (AVSUA-equivalent attestation), `EE-POA-027`–`EE-POA-031` (single predicate, withheld false,
Estonian statutory annex), `EE-POA-030` (age-predicate policy), `EE-ZKP-040` (the ≤1.0 s p95 budget),
spec §21 (roadmap), §22 (deliberate divergences), §23 per the workspace convention (collected
`[UNVERIFIED]` items) `[VERIFIED]`.

---

## 11. Risks and uncertainties the source itself states

**Risk / uncertainty content in the body of this document** (a fuller risk register is said to live in
companion Appendix A4, which was not read for this extract):

1. **Betting on an unfinished standard.** ISO second-edition publication slipped Q3 2026 → Q1 2027 so
   "the *planned* structures may still change"; ZKP removed from DCHP's initial proposal; OpenID4VP #627
   open ~16 months; ARF Topic G refinement open `[VERIFIED]`.
2. **`ZKP_08` vs ECCG v2.0 contradiction.** As written, the ARF requirement cannot be satisfied by any
   scheme, including the mandated one: "That tension is unresolved and is the largest single uncertainty
   in this assessment" `[VERIFIED]`.
3. **Upstream abandonment.** 1 commit/12 months, 0 open issues/PRs, 3 ★, 1 contributor; fork is a
   single-contributor public fork with 0 ★ `[VERIFIED]`.
4. **AV Profile revision drift.** 1 September 2026 revision recorded; the kit must pin a version and
   absorb clause drift as maintenance cost (O1).
5. **ISO text unreadable.** Parameter semantics "taken at one remove" from TS13/AV; if the published
   second edition renames the structures, "O5's vector labels need renaming and O1's clause map needs a
   new source" `[UNVERIFIED]`.
6. **Comparator understatement.** "a README silence is not evidence of absence; if, say, the Commission's
   reference wallet ships ZKP in an unreleased build, O1's differentiation falls from 'first to publish
   evidence' to 'another test kit'" `[JUDGEMENT]`.
7. **Effort estimates are one-person, agent-assisted baselines** — derived from a campaign produced with
   AI assistance; "A conventional team should expect a substantially larger calendar effort; the
   estimates are stated to expose the method, not to promise a schedule" `[ESTIMATE]`.
8. **Per-opportunity risks** as recorded in §5: O2 register spec may differ from the mock / RIA may
   publish its own kit; O3 moving ARF certificate profiles and the risk that "a mis-parsed intended use
   could *loosen* a privacy control"; O4 unsigned-or-compromised list as a supply-chain hazard and
   "maintaining a list nobody adopts is dead weight"; O5 carrier superseded by ISO/DCHP settlement; O6
   unbounded external wait plus legal mapping errors.
9. **External blockers per ranking table:** O1 none; O2 RP register interface + TARA terms; O3 ARF
   certificate profile stability; O4 a scheme owner to adopt it; O5 an external implementation to test;
   O6 circuits accepted by a scheme owner.
10. **Reputational/credibility risk already identified and actionable:** the stale `docs/CONFORMITY.md:160`
    `issuerAuth` row "should be corrected before the note is shown to an evaluator" `[REC]`.

---

## 12. Why now — the timing argument in the source's own terms

- **A dated European obligation with no product.** Estonian public services must accept certified EU
  wallets from **end-2026**; the Estonian wallet is planned for **2028**; the private sector follows at
  **end-2027** `[VERIFIED]` — a multi-year window in which Estonian RPs must accept *foreign* wallets with
  nothing local to test against.
- **The EU blueprint already mandates the fork's exact scheme** (`longfellow-libzk-v1`, doctype
  `eu.europa.ec.av.1`), so the remaining gap is "a conformance-test and packaging gap, not a cryptographic
  redesign" `[VERIFIED]`.
- **The standard currently has no reference implementation to test against** — OpenID4VP has no ZK text,
  DCHP dropped it, ISO is late, and the Commission's FCAF "under active development" and silent on the ZK
  carrier `[VERIFIED]`. The source's stated play: "The mitigation is not to wait but to be the
  implementation that a standardisation venue tests against."
- **The tests and measurements already exist**, so credibility work is cheap now (O1 3–6 weeks, O5 2–4
  weeks) whereas rebuilding the oracle inside a programme is expensive `[VERIFIED + ESTIMATE]`.
- **Three Member States were already ahead with cross-border approval applications as of May 2026**,
  i.e. the market is moving before Estonia's own wallet exists `[VERIFIED]`.

---

## 13. Open questions / unresolved items that a PRD cannot silently close

1. Does the fork ship ZKP in any unreleased comparator build? (untestable here; drives O1's
   differentiation claim) `[UNVERIFIED]`.
2. What will ISO/IEC 18013-5 second edition actually name the structures and parameters? `[UNVERIFIED]`
3. How will `ARF ZKP_08` vs ECCG v2.0 be resolved — and can any ZKP scheme be conformant while it stands?
   `[VERIFIED as an open contradiction]`
4. What will RIA's RP register API and access-certificate scheme look like, and when? `[VERIFIED as
   unpublished]`
5. Who hosts the accepted-circuit set (O4) — Commission, Member State, or community registry with a
   succession path? `[REC: ship community-first]`
6. Which thresholds must Estonia's statutory mapping cover, per legal review? (source names 18/21 tobacco
   and alcohol and 16 other restrictions as the motivating examples) `[JUDGEMENT]`.
7. Who is the internal owner of these opportunities? The document is authored as "a research pass
   commissioned in this repository"; no team, budget, or schedule is assigned anywhere in it — every
   effort figure is an `[ESTIMATE]` for one agent-assisted person. **The PRD must source that externally.**
8. Will the Commission's AV development environment (`ageverification.dev`) or its reference wallet be
   available for the O1/O5 external-validation criteria — and is publishing failures against it acceptable
   to the maintainer? `[REC: publication including failures is the validation method]`
9. The `docs/CONFORMITY.md:160` correction is pending `[REC]`.

---

## 14. Assumptions the assessment rests on (stated in the source)

- `[ASSUMPTION]`-class premises (**note: the source defines the `[ASSUMPTION]` label in its header but
  never applies it in the body; the classification below is the extractor's reading, not the source's
  own labelling**): the AV Profile and TS13 text fetched on 23 October 2026 is the operative
  version; the comparator set of ten is representative of others' ZKP work; the Estonian
  obligation dates (end-2026 / end-2027 / 2028) hold as published by RIA; and effort estimates derived
  from one agent-assisted campaign transfer to any actual team.
- The source explicitly labels as `[ESTIMATE]`: all six effort figures and the priority scores' derivation
  method (with the arithmetic note in §4 above).
- The source explicitly labels as `[UNVERIFIED]`: ISO second-edition text and parameter semantics;
  existence of any interop run against the Commission's reference wallet anywhere (as opposed to absent
  from this repository); and, carried from the workspace convention, items collected in spec §23.

**For a PRD author:** the load-bearing `[VERIFIED]` claims are (a) the AV Profile's mandate of
`eu.europa.ec.av.1` + `longfellow-libzk-v1` + `age_over_18`, (b) the fork's existing implementation of
that scheme plus a working negative-control test estate, (c) the Estonian obligation dates, and (d) the
absence of any verifier-side ZKP conformance kit in the comparators. Everything about cost, order value
beyond ranking, and delivery date is `[ESTIMATE]` or `[JUDGEMENT]` and must be presented as such.
