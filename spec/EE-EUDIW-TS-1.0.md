# EE-EUDIW — Estonian European Digital Identity Wallet
## Technical Specification, with a Zero-Knowledge Proof of Age Profile

| | |
|---|---|
| **Document identifier** | EE-EUDIW-TS-1.0 |
| **Version** | 1.4 (draft for review) |
| **Date** | 17 September 2026 (v1.3: 16 September 2026; v1.2: 12 September 2026; v1.1: 7 September 2026; v1.0: 5 September 2026) |
| **Baseline** | EUDI ARF **v3.0.0** (tagged 2026-07-21; the GitHub release was published 2026-07-23) — read at commit **6373eee** (2026-07-23, 17 commits past the tag; §2.3) · Reg. (EU) No 910/2014 as amended by (EU) 2024/1183 · CIR 2024/2977, 2024/2979, 2024/2980, 2024/2981, 2024/2982 as amended by **CIR (EU) 2026/1731** · CIR 2025/846–849 (848 as amended by **CIR 2026/1730**) · **CIR 2025/2160** · CIR 2026/798 · CIR 2015/1502 · CIR 2024/482 |
| **Changes in 1.1** | §9.7.1 prices the non-qualified-EAA choice (EE-POA-022/023) · §14.1 makes pseudonyms a mandatory core function and constrains the pseudonym key (EE-PRI-017–019) · §15.1 new, on mass signing and seals (EE-SIG-010–016) · §17.1 rewritten against the CIR 2024/2981 enacting text; EE-SEC-020–023 added while existing requirement identifiers retain their meanings · §22 items 9–11, §23 items 18–23 · revised 12 September 2026: §10.4/§10.8 AV Profile provenance, `EE-REV-001`, §22 items 12–14 |
| **Changes in 1.2** | Response to an adversarial review, triaged against primary sources. **One factual correction:** §9.3 EE-POA-005 asserted that no registration duty attaches at a gambling venue's door; **HasMS § 37(8)–(10) attaches one**, penalised under § 89 at up to €20,000, so gambling is now an identification use case in *both* channels (EE-POA-005/005a), `age_over_21` is demoted to Optional in §9.2, and Annex B.3 becomes an anti-pattern. **New material:** §4.2 EE-SEC-001a/b/c on the remote WSCD's per-presentation visibility · §9.5 EE-POA-011a/011b on batch key generation off the critical path · §9.7 option (c) rejection reasoned, EE-POA-024 designates the AV Profile attestation as the cross-border path · §10.5 EE-ZKP-022a on the 109-bit soundness error against the ACM floor, with the AVA_VAN.5 confusion disentangled · §10.7 EE-ZKP-044–046 on the transport budget the proof-generation budget omits · §10.8 EE-ZKP-054–056 closing the capability-signal downgrade trapdoor · §14.1 EE-PRI-018a/018b resolving the WUA contradiction and preserving WebAuthn interoperability · §15.1.2 EE-SIG-017/018 for offices where a seal is unavailable · §20.1 EE-CNF-006/007 · §21.1 R13–R16 · §22 items 15–18 · §23 items 24–28 · **then refined the same day against a further review:** EE-POA-013 makes destruction of consumed key material mandatory (SHALL, matching EE-POA-011b) · EE-POA-024 conditions AV-Profile selection on the request accepting it and on the threshold matching · **§ 94** recorded alongside § 89 as the remote-channel penalty (§9.3, §21.1 R8, Annexes A and E) · Annex B.3 states its PID example as conditionally valid, names the document-name gap, and defines the physical-document fallback |
| **Changes in 1.3** | Integration of five standalone analyses now filed under `docs/analysis/`. **Three factual corrections.** §10.5 stated a **128-bit ACM floor**; ACM v2.0 §1.1 sets "at least 125 bits" for *recommended* mechanisms and §1.3 accepts 100 for legacy — there is no 128-bit floor, and the shortfall bears on `ZKP_08`, not on `ZKP_07` (carried through EE-ZKP-022a, §21.1 R15, §22 item 17, §23 item 25). §10.2 and §21 placed the **Topic G refinement round in Iteration 7**; the ARF's own `docs/discussion-topics/README.md` at the pinned baseline puts it in **Iteration 6, 23 September – 18 November 2026** — a window that opens the week this revision was written. §11.2's DC API Working Draft date moves to **4 September 2026**. **Rewritten:** §10.3 now states the two available readings of `ZKP_08` and which one each authority uses, rather than asserting a single answer, and EE-ZKP-003 requires the assessment to record its reading; §1 item 4 follows. **Resolved:** §23 item 6 — there is no ACM v3.0 final text; the April 2026 *working draft* was read in full. **New material:** §4.2 on what the local-native default rests on, what Android RKP's 14-day key rotation does and does not close, and RIA's own PoC calling its remote-HSM service a mock · §9.5 on batch linkability at the attestation layer · §10.5 on the two ACM-conformance gaps an assessor will raise · §10.9 on the v3.0 draft demoting classical asymmetric mechanisms · §11.1/§11.3 EE-PRO-010a and EE-PRO-013 distinguishing the DC API cross-device path from the proximity-less redirect path, plus the EE-PRO-003 prefix split · §17.1.1 recording Google's split-certification argument and the ENISA draft's AVA_VAN.3 routing as named positions rather than settled law, and the scheme's own published roadmap · §19 EE-GOV-032 on binding a presentation to a session · §23 items 29–30. Requirement count 176 → **178**. |
| **Changes in 1.4** | Completion of the requirement-level integration revision 1.3 left open. **Four obligations that were stated in prose with a capitalised SHALL and no identifier are repaired**, since such a sentence binds while being invisible to Annex E: §17.1.1's treatment of a split-certification scoping becomes **EE-SEC-024**; §17.1.1's duty to name the version and date of a draft methodology becomes a clause on **EE-SEC-021**, which already governs naming the methodology; §4.2's DPIA sentence and §9.5's measured-figures sentence were each already carried by a numbered requirement or an open item (EE-PRI-020, §23 item 26) and become descriptive. **New material:** §10.5 **EE-ZKP-022b** puts the two ACM-conformance gaps an assessor will raise — `SHA256-AES256CTR` against ACM §7, GF(2^128) against §4.2/§4.3 — into the security target as an assumption record, completing the map EE-ZKP-003 requires for primitives that *have* ACM entries; §10.5 **EE-ZKP-022c** instantiates Track A at the ACM's *recommended* level for the components the wallet controls and records ECDSA/P-256 and SHA-256 as a backward-compatibility dependency, since the wallet does not choose the issuer's signature algorithm (§22 item 19, §23 item 6). **Corrected:** Annex E's coverage figures contradicted themselves — the text claimed 98 identifiers and then reported 96 in the next sentence. Expanding the first column gives 105 identifiers. **Requirement-count regex corrected:** the count's area pattern required exactly three letters, which silently excluded the two-letter `RP` area (`EE-RP-001`–`EE-RP-006`) and so undercounted every revision by six; widened to `{2,4}`, it now matches all 105 matrix identifiers. Requirement count 178 → 181 with the three added here, → **187** once the pattern is corrected. |
| **Target jurisdiction** | Republic of Estonia |
| **Status** | Independent technical specification. Not issued by RIA, the Ministry of Justice and Digital Affairs, or any Estonian public authority. |

---

## 0. About this document

### 0.1 Purpose

This document specifies a complete, implementable Estonian European Digital Identity Wallet ("EE‑EUDIW") that (a) satisfies the binding requirements of the amended eIDAS Regulation and its implementing acts, (b) tracks the current state of the art in the EUDI Wallet Architecture and Reference Framework, and (c) adds a **zero‑knowledge Proof‑of‑Age profile** that is materially stronger than the salted‑hash selective disclosure the ARF's mandatory baseline permits.

It is written for four audiences: the Estonian Wallet Provider and its contracted operator; PID and attestation providers connected to X‑tee; relying parties integrating age checks and identification; and the conformity assessment body that will certify the wallet under CIR 2024/2981.

### 0.2 Requirement language

The key words **SHALL**, **SHALL NOT**, **SHOULD**, **SHOULD NOT**, and **MAY** are to be interpreted as described in BCP 14 (RFC 2119, RFC 8174) when, and only when, they appear in capitals.

Normative requirements defined by this document carry identifiers of the form `EE-<AREA>-<nnn>`. Areas are: `GOV` (governance), `ONB` (onboarding), `PID`, `POA` (proof of age), `ZKP`, `PRO` (protocols), `WUA`, `REV` (revocation), `RP` (relying party), `SIG` (signing), `PRI` (privacy), `SEC` (security), `ACC` (accessibility), `CNF` (conformance).

Every `EE-*` requirement in this document is traceable to an ARF high‑level requirement, an implementing act, or Estonian statute; Annex E is the traceability matrix.

### 0.3 Evidence discipline

Version numbers, dates, identifiers and legal citations in this document were checked against primary sources between 3 and 5 September 2026, and again on 7 September 2026 for the material added in v1.1, and the draft was then put through an adversarial verification pass whose corrections are incorporated. The v1.1 pass corrected two claims that v1.0 had backwards: that CIR 2024/2981 fixes no evaluation level (it fixes AVA_VAN.5 for the WSCA and, conditionally, the WSCD — see §17.1.1), and that Art. 5c(3) establishes *transitory* schemes (it does not — see §17.1). Claims that could not be verified against a primary source are marked **[UNVERIFIED]** inline and collected in §23. Where a source was read at second hand, the text says so. Nothing in this document is asserted from memory of the standards landscape; where the landscape is unsettled — and in the ZKP area it is deeply unsettled — this document says so rather than pretending a choice has been made. Material added on 12 September
2026 — the AV Profile's scheme selection (§10.4), its fallback and acceptance rules (§10.8) and the
answer recorded at §23 item 8 — was read from the Commission's age-verification portal on that date,
and the three divergences recorded in §22 items 12–14 were checked against the ARF high-level
requirements in the `eudi-arf` checkout at the same time.

The v1.2 pass was a second adversarial review, and it found one substantive factual error. §9.3 had asserted that "no registration duty attaches to the doorman" at a gambling venue. The consolidated text of Hasartmänguseadus was read at Riigi Teataja on 12 September 2026: **§ 37(8)–(10) attaches a full identification, document-copying and database-registration duty at entry to a gaming location**, and **§ 89** penalises breach at up to 100 fine units or €20,000 for a legal person. The correction is carried through §9.2, §9.3, §22 item 6, §21.1 R8, Annex A and Annex B.3. Note that the review that prompted it reached the right conclusion from the wrong provisions — it cited RahaPTS, whose customer-due-diligence trigger for gambling organisers is a €2,000 stake or payout under § 19(3), not entry. Both the conclusion and the correct basis are recorded, because a specification that cites the wrong statute for a right answer will be corrected by the first regulator who reads it. The remaining v1.2 material is requirement-level and is listed in the header table; where the review's premises did not survive checking — the AVA_VAN.5 "contradiction", the claim that §9.7 had not evaluated short-lived PuB-EAAs, and the reading of EE-PRI-018 as a rule about WebAuthn conveyance — the text now says why, rather than silently conceding or silently ignoring.

The v1.3 pass was not a review but an integration: five standalone analyses, written between 7 and 14 September 2026 and now filed under `docs/analysis/`, were read back against this text to find where they contradicted it. They found three factual errors, listed in the header table. Two are worth naming here because of how they arose. The **128-bit ACM floor** was a plausible round number that no source states — ACM v2.0 sets 125 bits for *recommended* mechanisms — and it survived two adversarial reviews because it made the specification look more conservative than the truth, which is the direction of error a reviewer is least likely to challenge. The **Topic G iteration** was checked against the ARF's own discussion-topic index in the pinned submodule, which is in this repository: the round is Iteration 6 and it is open now, not Iteration 7 and a month away. Where an analysis and this text disagreed on something neither could settle, the disagreement is recorded in §23 rather than resolved by preference.

### 0.4 What is deliberately not here

This is not a procurement document, a cost model, a service design, or a DPIA. It does not specify the Estonian relying‑party register's internal database schema (RIA owns that), nor the operator's SRE practices. It does specify the interfaces at which those things meet the wallet.

---

## 1. Executive summary

**The position Estonia is in.** Estonia must provide at least one EUDI Wallet by **24 December 2026** — 24 months after CIR 2024/2977 and its four sibling acts entered into force on 24 December 2024 (Art. 5a(1) of Reg. 910/2014 as amended). RIA opened an international procurement for the wallet's development and five‑year operation on **18 May 2026**, with applications closing **29 June 2026**, run as a competitive dialogue over a 96‑month contract; no award had been announced as of 5 September 2026, and RIA's own published estimate is that contracting "may take approximately one year" from launch if unchallenged. Estonia will therefore **miss the December 2026 provision deadline**, and this specification is written on that assumption. What Estonia can and must hit is the *acceptance* side: public bodies accepting other Member States' wallets from end‑2026, and the relying‑party register that CIR 2025/848 requires from 24 December 2026.

**The design thesis.** Estonia has an unusual, and unusually awkward, starting position for privacy‑preserving identity. The `isikukood` — the personal identification code — is a plaintext encoding of date of birth and sex, it is printed on every document, it is embedded verbatim in every ID‑card certificate's `serialNumber` as `PNOEE-<isikukood>`, and the state's own Eesti äpp broadcasts it as an unauthenticated barcode for loyalty‑card use. A national identity culture built on a universally‑known, DOB‑bearing identifier is the single strongest argument in Europe for building age proofs that reveal *nothing but the predicate*. This specification treats that as the design driver.

**The four load‑bearing technical decisions.**

1. **Proof of age is a separate attestation, not a PID attribute.** The EUDI PID Rulebook removed the age attributes in v1.1 (4 September 2025) — the changelog entry reads *"Age verification attributes removed, following CIR 2024/2977"* — and the current v1.7 (17 July 2026) mandatory set is `family_name`, `given_name`, `birth_date`, `birth_place`, `nationality`, `portrait`. There is no `age_over_18` in the PID any more. Age proof therefore lives in a dedicated Proof‑of‑Age attestation, and this specification defines the Estonian one (`ee.riik.poa.1`) alongside mandatory interoperability with the Commission's AV Profile attestation (`eu.europa.ec.av.1`).

2. **Unlinkability is specified in two tiers, and the tiers are honestly labelled.** The ARF is explicit that salted‑hash formats cannot eliminate Attestation‑Provider linkability: *"The only viable mitigation is to adopt Zero-Knowledge Proofs (ZKPs) as a proof mechanism instead of relying on salted-attribute hashes."* Tier 1 (mandatory, deployable today) is once‑only batched attestations per ARF Method A, which defeats relying‑party linkability but not issuer‑verifier collusion. Tier 2 (specified in full, gated behind conformance criteria) is a genuine ZKP presentation. This document does not let an implementer claim Tier 2 privacy while shipping Tier 1 — which is precisely what the EU's own age‑verification app has been criticised for.

3. **The ZKP track is dual, because the standards are.** Track A is `longfellow-libzk-v1` — Google's Ligero‑based zkSNARK over ECDSA/mdoc, the scheme the Commission's AV Profile selected, for age verification (§10.4 sources the selection), published in IACR Communications in Cryptology in May 2026 and reviewed by Trail of Bits, ISRG and an academic panel. Track B is the multi‑message‑signature route of the Commission's TS14 — BBS‑family credentials with a separate Schnorr‑type ECDSA proof of possession for device binding, converging in ETSI TS 119 476‑2. Track A ships; Track B is where the ARF's full requirement set (ZKP_03, ZKP_04, predicate range proofs, issuer hiding) can actually be met. An Estonian wallet that hardcodes either one will be wrong within two years.

4. **The compliance gap is stated, not papered over.** ARF requirement **ZKP_08** reads: *"A ZKP scheme SHALL rely solely on algorithms included in the ECCG Agreed Cryptographic Mechanisms v2.0."* No candidate ZKP scheme — not Longfellow, not BBS, not any pairing‑friendly curve — appears by name in ECCG ACM v2.0 or its v3.0 working draft. **Whether that settles the question depends on a reading of ZKP_08 that no authority has given.** Read as a rule about listed *mechanisms*, ZKP_08 is unsatisfiable by every candidate scheme and will stay so for years; read as a rule about *primitives and assumptions* — the reading the Commission's own Technical Specifications, ETSI TR 119 476-1 and ANSSI apply elsewhere — a hash-based system such as Longfellow already meets it, subject to three parameter-level gaps (§10.3, §10.5). This specification therefore requires a documented crypto‑agility plan and an explicit conditional‑compliance statement, rather than a compliance claim that cannot be supported.

**What is new here relative to the EU baseline.** Issuance integrity anchored in the population register over X‑tee rather than a client‑asserted document read (§9.4, closing the enrolment flaw found in the Commission's reference issuer); a national circuit‑governance registry with transparency logging (§10.6); a downgrade‑resistance regime for the plain‑mdoc fallback (§10.8); an explicit gambling carve‑out reconciling Hasartmänguseadus § 53(1)'s mandatory player identification with the anonymous age path (§9.3, Annex A); and a hard prohibition on `isikukood` in any age‑proof code path (§7.4, Annex D).

---

## 2. Regulatory baseline

### 2.1 Union law

| Instrument | Relevance | In force / applies |
|---|---|---|
| Reg. (EU) No 910/2014 as amended by Reg. (EU) **2024/1183** | The wallet obligation itself | Amendment in force 20 May 2024 |
| **CIR (EU) 2024/2977** | PID and EAA issued to wallets | 24 Dec 2024 |
| **CIR (EU) 2024/2979** | Integrity and core functionalities | 24 Dec 2024 |
| **CIR (EU) 2024/2980** | Ecosystem notifications to the Commission | 24 Dec 2024 |
| **CIR (EU) 2024/2981** | Certification of wallet solutions | 24 Dec 2024 |
| **CIR (EU) 2024/2982** | Protocols and interfaces | 24 Dec 2024 |
| **CIR (EU) 2025/846** | Cross-border identity matching | applies 24 Dec 2026 |
| **CIR (EU) 2025/847** | Reaction to wallet security breaches | 2025 |
| **CIR (EU) 2025/848** (amended by **2026/1730**) | Registration of wallet-relying parties | applies 24 Dec 2026 |
| **CIR (EU) 2025/849** | List of certified wallets | 2025 |
| **CIR (EU) 2025/1569** (amended by **2026/1735**) | QEAA and PuB-EAA | 2025 |
| **CIR (EU) 2026/798** | Remote onboarding at LoA substantial + additional procedures | adopted 7 Apr 2026; in force 28 Apr 2026 |
| **CIR (EU) 2026/1731** | Amends 2977/2979/2980/2982 as regards applicable standards; adds Art. 14a (Trust Mark display); Annex II now points to **ETSI TS 119 472-1 V1.2.1** with PID per clause 5 (SD-JWT VC) and clause 6 (ISO mdoc) | in force 11 Aug 2026 |

Deadlines that bind this specification:

- **24 December 2026** — Member State provides at least one wallet (Art. 5a(1)); RP registration regime applies; cross-border identity matching applies; Art. 45e authentic-source verification measures due.
- **24 December 2027** — private relying parties in the sectors listed in Art. 5f(2) must accept the wallet on voluntary user request.

Articles that generate concrete requirements below: 5a(3) open source; 5a(4)(a)–(g) user functions including selective disclosure, pseudonyms, dashboard, erasure, portability; 5a(5)(b) the wallet SHALL NOT inform attestation providers of attestation use; 5a(5)(d) LoA High; 5a(5)(g) QES free of charge for natural persons; 5a(13) wallet free of charge; 5a(14) no collection of unnecessary usage information and logical data separation; 5a(15) voluntary use with no disadvantage; **5a(16)(a)** no tracking, linking or correlating of transactions or user behaviour by attestation providers *or any other party* after issuance unless explicitly authorised; **5a(16)(b)** enable privacy-preserving techniques ensuring unlinkability where identification is not required; 5b(2)(c) and 5b(3) registered intended use and the prohibition on requesting data beyond it; 5b(9) relying parties SHALL NOT refuse pseudonyms where identification is not required by law; 11a(2) measures to prevent profiling in identity matching; 45f(8) PuB-EAA issuers provide a wallet interface; 45h(1)–(3) service separation.

The **Digital Omnibus** proposal COM(2025) 837 of 19 November 2025 touches Reg. 910/2014 only through the ENISA Single Entry Point for incident reporting; it does not amend Art. 5a, 5b, 5c or 5f, and it does not move any wallet deadline. A separate, simultaneous proposal — COM(2025) 836 — became the Digital Omnibus on AI, adopted 8 July 2026 as Reg. (EU) 2026/1744 (OJ 24 July 2026, in force 27 July 2026); it amends the AI Act only. **No wallet deadline has been legally moved.**

### 2.2 Estonian law

**E-identimise ja e-tehingute usaldusteenuste seadus (EUTS)** is the national trust-services act. Note the correct title — "Elektroonilise identifitseerimise ja e-tehingute usaldusteenuste seadus" is not the name of any Estonian act. EUTS § 2 already assigns RIA the Art. 7(f), 9 and 12 tasks under Reg. 910/2014.

The eIDAS 2 transposition bill — *"E-identimise ja e-tehingute usaldusteenuste seaduse, riigilõivuseaduse ja karistusseadustiku muutmise seadus"* — went to public consultation 30 July–6 August 2026 and was submitted to the Government session on 19 August 2026 (ref. 8‑1/5650‑1). Its substantive content, as drafted:

- EUTS § 1(1) extended to cover *"Euroopa digiidentiteedikukru pakkumine"*.
- A **state duty** to make a voluntary, state-backed EUDI Wallet available to residents.
- **RIA designated principal competent authority** — supervision, authorisations, the relying-party register, notifications.
- Authorisation procedure and state fee for the wallet provider: **€1,030 initial / €770 renewal** per trust service or wallet (riigilõivuseadus § 235¹).
- QTSP liability insurance annual aggregate raised **€1 M → €2 M**.
- New EUTS § 23³: breach of §§ 5, 6(1) or 13(1) punishable by up to **€5,000,000 or 1 % of worldwide annual turnover**, whichever is higher; RIA the extrajudicial body (§ 23⁴).
- **Karistusseadustik § 350** amended so the EUDI wallet counts as a *tähtis isiklik dokument* (important personal document).

**[UNVERIFIED]** — as of 5 September 2026 no Riigikogu bill number (SE) was found for this act, and it does not appear to have been introduced in the Riigikogu. All requirements in this document that depend on it are marked conditional.

Other Estonian instruments that bind the design: **Isikut tõendavate dokumentide seadus (ITDS)** (§ 2(3) digital document; § 20 digital ID; §§ 15⁴–15⁵ ABIS; the June 2025 amendment, adopted 4 June 2025 by 75 votes, giving the Eesti äpp identity-document force); **Isikuandmete kaitse seadus** with GDPR; **Küberturvalisuse seadus** as amended for NIS2, in force January 2026, expanding covered entities from roughly 3,500 to 6,500; **Alkoholiseadus**, **Tubakaseadus**, **Hasartmänguseadus** for age thresholds (Annex A); **Rahvastikuregistri seadus** and **Avaliku teabe seadus** for the authentic sources.

### 2.3 The ARF and the numbered Technical Specifications

ARF **v3.0.0** (tagged 2026-07-21; the GitHub release was published 2026-07-23) is the current release and the baseline for this document; `https://eudi.dev/versions.json` confirms 3.0.0 carries the `latest` alias among 28 published versions. The revision this document was written against is commit `6373eee` (2026-07-23), 17 commits past that tag — the same day the release was published. One high-level requirement changed in those commits: `ISSU_33b` (`AS-AP-10-060`, commit `f0560a8` "Fixing ISSU_33b to align with catalogue of schemes"), which now turns on conformity with the attestation scheme catalogue under CIR (EU) 2025/1569. The other 16 commits are editorial (Annex 2 heading rendering, changelog and home-page wording). The HLR set is 725 rows across 34 topics at both revisions, and this document cites no requirement whose text changed between them. v3.0.0 aligns with the July 2026 amending CIRs, introduces the **Relying Party Services** concept, adds trust-anchor retrieval from both Trusted Lists (ETSI TS 119 612) and **Lists of Trusted Entities** (ETSI TS 119 602), and introduces the **Functional Conformance Assessment Framework** (§7.5, `https://conformance.eudi.dev/`) for Annex III of CIR 2024/2981.

Annex 2 of ARF 3.0.0 carries **725 high-level requirements across 34 topics** (the authoritative source is `hltr/high-level-requirements.csv`; topic numbers are non-contiguous, running from 1 to 56 with gaps), each with a topic-scoped identifier (`ZKP_01`) and a harmonized identifier (`EW-DM-53-001`). The topics this document depends on most: 1 (online access), 3 (PID rulebook), 6 (RP authentication and user approval), 7 (revocation), 9 (wallet unit attestation), 10 (issuance), 11 (pseudonyms), 12 (attestation rulebooks), 16 (signing), 19 (dashboard), 27 (registration), 31 (notification), 38 (wallet revocation), 40 (installation and activation), 43 (embedded disclosure policies), 44 (registration certificates), 51 (deletion), 52 (RP intermediaries), **53 (zero-knowledge proofs)**, 54 (accessibility), 55 (certificate transparency).

The Commission's numbered Technical Specifications relevant here: **TS3** Wallet Unit Attestation (v1.5.2, 26 May 2026); **TS4** ZKP implementation (v1.0.1, 30 Jan 2026); **TS5** v1.5 (20 Aug 2026) and **TS6** v1.2.2 (20 Aug 2026) relying-party registration formats and content — note TS5 v1.4 (15 Jul 2026) introduced the `WalletRelyingPartyService` class and the RP Service identifier relied on in §5.2; **TS10** portability and export; **TS13** ZKPs from arithmetic circuits (v1.0.1, 30 Jan 2026); **TS14** ZKPs from multi-message signatures (v1.0, 27 Feb 2026). TS13 and TS14 both carry the disclaimer that they are not final and will be handed to ETSI for refinement under **ETSI TS 119 476‑2** (early draft v0.0.4, 8 July 2026, STF 705, rapporteur Peter Altmann).


---

## 3. Roles and the Estonian institutional mapping

| ARF role | Estonian assignment | Basis |
|---|---|---|
| **Wallet Provider** | **RIA** (Riigi Infosüsteemi Amet), under the Ministry of Justice and Digital Affairs | Art. 5a(2)(a); the transposition bill's state duty. RIA opened the operator procurement 18 May 2026 |
| **Wallet Operator** (not an ARF role; contractual) | Contractor selected in the RIA procurement — 96-month contract, minimum 5 years of service operation, competitive dialogue, 3–5 applicants expected at stage two. **[UNVERIFIED — no award announced as of 5 Sep 2026]** | Estonian public procurement |
| **PID Provider** | **RIA**, sourcing from the identity-documents database (PPA as controller, SMIT as processor) and the Population Register (Ministry of the Interior as controller, SMIT as processor), over X-tee. The procurement folds PID issuance and management into the operator's scope | Art. 5a(5)(f), CIR 2024/2977 |
| **Registrar** (relying parties, providers) | **RIA**. MVP plus guidance planned end-2026/early 2027; access-certificate design still open per RIA's own published statement | CIR 2025/848 as amended by 2026/1730; Art. 5b(1) |
| **Access Certificate Authority** | **[UNVERIFIED / open]** — RIA states *"Juurdepääsu sertifikaatide täpne lahendus on veel väljatöötamisel"* | CIR 2025/848 Art. 7, Annex IV |
| **QEAA Provider** | SK ID Solutions AS and other Estonian QTSPs on the EU Trusted List | Art. 45d |
| **PuB-EAA Provider** | Public bodies responsible for authentic sources — PPA (documents), Transpordiamet (mDL), Tervisekassa (EHIC), Maksu- ja Tolliamet (tax), Haridus- ja Teadusministeerium (qualifications) | Art. 45f; Annex VII |
| **Authentic sources** | Rahvastikuregister; isikut tõendavate dokumentide andmekogu; ABIS (biometrics only); sectoral registers, all reached over **X-tee** | Art. 45e; Annex VI |
| **Certificate issuer for ID-1 documents** | **Zetes Estonia OÜ** since 17 November 2025 (Thales-manufactured cards); **SK ID Solutions** for the legacy IDEMIA estate | id.ee |
| **Supervisory / competent authority** | **RIA** | EUTS § 2; transposition bill |
| **DPA** | **Andmekaitse Inspektsioon** | IKS |
| **CAB** | Estonia designates the conformity assessment body under Reg. (EU) No 910/2014 Art. 5c(1); the certification body is accredited by a national accreditation body appointed pursuant to Reg. (EC) No 765/2008 and in accordance with EN ISO/IEC 17065:2012 | Reg. (EU) No 910/2014 Art. 5c(1); CIR 2024/2981 Art. 9(1) |

**EE-GOV-001 (SHALL).** The Wallet Provider SHALL be a public body of the Republic of Estonia. Where operation is contracted, the contract SHALL make the operator a processor under GDPR Art. 28 and SHALL NOT permit the operator to derive any secondary use of transaction, presentation or attestation-usage data.

**EE-GOV-002 (SHALL).** All application software components installed on user devices SHALL be published under an OSI-approved licence, per Art. 5a(3). Components not installed on user devices MAY be withheld only on a published, duly justified security ground, and the justification SHALL be published.

**EE-GOV-003 (SHALL NOT).** The Wallet Provider and the Operator SHALL NOT combine personal data processed for wallet provision with data from any other service they provide, per Art. 5a(14) applying Art. 45h(3) mutatis mutandis.

### 3.1 Relationship to Eesti äpp

**Eesti äpp** (the state mobile application, `ee.riik.eesti.app`, built by Net Group, roughly 320,000 downloads as of August 2026 — the successor to the abandoned "mRiik" project) is **not** the EUDI Wallet and SHALL NOT be conflated with it. RIA has stated the wallet is a separately procured product and that the integration between the two "will become clear as work progresses"; RIA describes the wallet as *"täiendav riiklik isikutuvastusvahend ID-kaardi kõrval"* — an additional national identification means alongside the ID-card.

Eesti äpp today offers three distinct and legally unequal operations, and the distinction matters for this specification:

1. **QR identity proof** (since 7 July 2026) — a QR code valid 3 minutes, readable only by another Eesti äpp instance whose holder is themselves logged in; the verifier sees the person's data and document photo for 30 seconds. This is the only mode that legally constitutes identity verification, under the ITDS amendment in force July 2025.
2. **`isikukood` barcode** — legally equivalent to reading the code aloud; zero identity assurance.
3. **In-app display of document data** — RIA states explicitly this is not identity verification.

**EE-GOV-004 (SHOULD).** Eesti äpp SHOULD be positioned as a relying party of the EE-EUDIW rather than as a parallel credential surface, and its `isikukood` barcode function SHOULD be deprecated in favour of an EE-PoA or PID presentation once the wallet is available. Mode 2 is a standing privacy regression: it publishes date of birth and sex, in the clear, to any camera.

---

## 4. Architecture

### 4.1 Components

```
┌──────────────────────── User device ────────────────────────┐
│  EE-EUDIW Wallet Instance (open source, Art. 5a(3))         │
│  ├─ Presentation layer  (DC API / OpenID4VP / ISO 18013-5)  │
│  ├─ Credential store    (mdoc CBOR + SD-JWT VC)             │
│  ├─ ZKP prover          (Track A libzk / Track B MMS)       │
│  ├─ Dashboard + transaction log (Topic 19, local only)      │
│  └─ WSCA  ──────────────┐                                   │
└─────────────────────────┼───────────────────────────────────┘
                          │
              ┌───────────┴───────────┐
              │  WSCD (one of four)   │
              │  native / eSIM /      │
              │  external card /      │
              │  remote HSM           │
              └───────────────────────┘

┌──────────────── Wallet Provider back end (RIA + Operator) ──┐
│  WUA service (WIA + KA issuance, TS3)                       │
│  Wallet Unit revocation service (Topic 38)                  │
│  Support & maintenance (Topic 56)                           │
└─────────────────────────────────────────────────────────────┘

┌──────────────── Issuance plane ─────────────────────────────┐
│  PID Provider (RIA)      ──┐                                │
│  EE-PoA Provider (RIA)   ──┼─ OpenID4VCI 1.0 + HAIP 1.0     │
│  PuB-EAA providers       ──┘                                │
│        └── X-tee security servers ── authentic sources      │
└─────────────────────────────────────────────────────────────┘

┌──────────────── Trust plane ────────────────────────────────┐
│  EC LoTE (PID + PuB-EAA anchors) · EU Trusted List (QEAA)   │
│  EE Registrar (RP register, TS5/TS6) · EE ACA               │
│  EE ZK Circuit Registry (§10.6, national, CT-logged)        │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 WSCD strategy for Estonia

The ARF (§4.5) recognises four WSCD architectures: remote (HSM), local external (smart card), local internal (eSIM/embedded SE), and local native (device-embedded, WSCA integrated into the OS). A WSCD is by definition assessed at LoA High.

**EE-SEC-001 (SHALL).** The EE-EUDIW SHALL support at least two WSCD architectures in production: **local native** (Android StrongBox / iOS Secure Enclave) as the default, and **remote HSM** as the fallback for devices without an adequate secure element and for the QSCD path.

*Rationale.* Estonia cannot afford a device-capability cliff. It has roughly 1.17 million valid ID-cards and near-universal eID usage; a wallet that excludes older Android hardware excludes exactly the demographic least able to fall back to a card reader. The remote-HSM route is also how the QES-with-the-wallet requirement (Art. 5a(5)(g), free QES for all natural persons) is most cheaply met, and Estonia already operates the relevant pattern at national scale through Smart-ID's SplitKey threshold scheme and Mobile-ID.

*What the local-native default actually rests on, and what it does not settle.* Android's Compatibility Definition Document requires a StrongBox backend to be certified against a Secure IC Protection Profile (BSI-CC-PP-0084-2014 or BSI-CC-PP-0117-2022) or evaluated by an accredited laboratory with a vulnerability assessment at High attack potential; key material is offloaded to the filesystem wrapped under a hardware Master Key that a factory reset rotates, and key usage policy is enforced inside the secure element against an HMAC'd Hardware Authentication Token that the Rich OS cannot forge (Google, *High Assurance Digital Credentials on Android*, August 2026, §3.2–§3.4 — a platform vendor's account of its own platform, which is what it is good evidence for). On that footing the local-native default meets the assurance demand of CIR 2024/2981 Annex IV head-on, where the remote-HSM fallback must argue the §3(2) justification (§17.1.1). It does **not** settle the boundary question: a certified secure IC is a certified key *store*, and whether that certificate encloses the WSCA's critical operations or only the WSCD beneath a software WSCA is unresolved — see §23 item 29.

*The cost of that fallback, stated plainly.* A remote WSCD holds the key on the provider's side. Every presentation that signs with it — Tier 1 mdoc and Tier 2 ZKP alike — requires a round trip to the WSCD provider at the moment of presentation. The provider therefore learns that this user presented something, when, and from where, on every transaction. That is a per-presentation activity log held by a single party, and it defeats the unlinkability that §10 is built to obtain, *before* the proof system is reached. No cryptography in the presentation path repairs it: the signing oracle sees the request. This is a structural property of remote WSCDs, not a defect of any particular implementation, and it is the reason the following three requirements exist.

**EE-SEC-001a (SHALL).** The remote-HSM WSCD SHALL be offered as a **fallback**, never as the default on a device whose local-native WSCD meets §12, and the wallet SHALL NOT migrate a user from local native to remote without an explicit, revocable choice.

**EE-SEC-001b (SHALL).** Before a user is activated on a remote-HSM WSCD, the wallet SHALL disclose in plain language that the WSCD provider will learn the time of every presentation made with that wallet unit, and that unobservability (`ZKP_07`) is consequently **not** achieved against that provider. The disclosure SHALL be repeated in the wallet's privacy dashboard (§14) and SHALL NOT be a one-time activation dialogue. The wallet SHALL NOT label a presentation made over a remote WSCD as unobservable.

**EE-SEC-001c (SHALL).** The remote WSCD provider SHALL retain WSCD invocation records only as long as is necessary for security incident investigation, SHALL NOT retain relying-party identity or attestation content in those records, and the retention period SHALL be published. Where the provider is also the Wallet Provider, the §4.3 data separation SHALL extend to these records.

*Residual.* Even with EE-SEC-001c, the provider's timing log remains. The honest statement is that a remote-WSCD user obtains Tier 1 unlinkability against *relying parties* and against the *attestation issuer*, but not against the *WSCD provider*. Users who need the full §10 property need a device with a local-native WSCD. Where the WSCD provider is a state body, this is a state-held activity log over wallet use, and EE-PRI-020 is what carries it into the DPIA under §16 as one. Estonia's threshold-signature experience (SplitKey) is relevant to key security, not to this problem; splitting the key across two parties does not stop either of them seeing the invocation.

*Estonia's own proof of concept records the same distance.* RIA's published Android proof of concept, `open-eid/eudi-wallet-poc`, states in `app/src/main/proto/api/wallet_provider.proto` that its wallet-provider service **mocks** a remote HSM, "does NOT aim to fulfill any security requirements", and that a secure implementation "would require at minimum … a Signature Activation Module (SAM) … [and] an additional authentication factor". That is the national PoC's own assessment of the gap between a remote-WSCD demonstration and a remote WSCD, and it names the same two components §15.1 does.

**EE-SEC-002 (SHALL).** A single physical component MAY implement both a QSCD and a WSCD, but SHALL be certified as both.

**EE-SEC-003 (SHALL NOT).** A WSCA SHALL NOT enable export of private keys from a WSCD (ARF `WUA_16a` / `AS-WP-09-023`).

**EE-SEC-004 (SHOULD).** Where the local-native WSCD is used, the wallet SHOULD consume platform key attestation (Android Key/ID Attestation with StrongBox where available; iOS App Attest plus Secure Enclave key custody) as *input* to the Wallet Provider's Key Attestation, and SHALL NOT pass a platform attestation chain through to relying parties. Note the standing asymmetry: Apple exposes no key-attestation certificate chain equivalent to Android's, which is why TS3 has the Wallet Provider re-sign a KA rather than relaying a platform chain. Note also the published research on bypassing Android hardware attestation; the residual-risk analysis under §17 SHALL treat platform attestation as strong evidence, not proof.

*What the platform's own privacy design buys, and what it leaves.* Android's Remote Key Provisioning scopes attestation keys per application package and rotates them roughly every 14 days, so the correlation window colluding issuers could exploit narrows to keys minted in the same app inside one rotation epoch — it narrows, it does not close (Google, *op. cit.*, §4.3 and Annex A). EE-SEC-004's proxying through the Wallet Provider removes issuer correlation at the price of the Wallet Provider holding the enrolment metadata, and §4.3 separation is what bounds that. The fully operator-independent alternative — verifying the hardware certification chain in zero knowledge on the device, so that no party need be trusted with the enrolment metadata — is not available: Google states that extending the on-device Longfellow framework to do it "would require non-trivial research and development" (*op. cit.*, Annex A). Estonia should not plan against it.

### 4.3 Data separation

**EE-SEC-005 (SHALL).** The wallet SHALL logically separate personal data used for wallet provision from data used for any other service, per Art. 5a(14), and the separation SHALL be a certified property, not a policy statement.

---

## 5. Trust infrastructure

### 5.1 Trust anchors

ARF v3.0.0 ch. 6 defines the model, and it is **X.509 plus ETSI trusted lists — not OpenID Federation**. Trust anchors for PID Providers and PuB-EAA Providers are published in a **Commission-maintained List of Trusted Entities (LoTE)** per **ETSI TS 119 602**; QEAA Provider anchors go to the Art. 22 **Trusted List** per ETSI TS 119 612; non-qualified EAA Provider anchors are not in the Commission LoTE.

**EE-GOV-010 (SHALL).** Relying parties SHALL retrieve trust anchors from both the LoTE and the Trusted List, per ARF 3.0.0's new dual-retrieval requirement, and SHALL validate the LOTL chain (transition to LOTL version 6 began 14 April 2026 with changed trust anchors).

**EE-GOV-011 (SHOULD).** For mdoc proximity presentation, the Estonian ecosystem SHOULD publish IACA trust anchors through a **VICAL** (ISO/IEC 18013-5 Annex C) in addition to the LoTE entry, and SHOULD document the mapping. Reconciling VICAL with LoTE is an open integration point in the EU ecosystem; Estonia should not wait for it to be solved centrally before its mDL and EE-PoA readers work.

### 5.2 Relying party registration and access certificates

CIR 2025/848 (as amended by 2026/1730) requires each Member State to operate a register of wallet-relying parties, published online, electronically signed or sealed, machine- and human-readable, with a common API (**TS5**) and a common information set (**TS6**, v1.2.2, 20 Aug 2026). Registration is **transparency-oriented**: ARF `Reg_01b` (`AS-MS-27-003`) states that Member States collect registration information only for transparency and **SHALL NOT apply any pre-authorisation process**.

**EE-RP-001 (SHALL).** The Estonian Registrar SHALL implement TS5 and TS6, SHALL publish current *and historic* Wallet-Relying Party Access Certificates (TS5 Annex II.2.1(c)), SHALL notify suspension or cancellation within 24 hours (CIR 2025/848 Art. 9), and SHALL retain registration records for 10 years (Art. 10).

**EE-RP-002 (SHALL).** Each relying party SHALL register its **intended use, including an indication of the data to be requested** (Art. 5b(2)(c)) and SHALL NOT request data beyond it (Art. 5b(3)). Each registered **Relying Party Service** SHALL carry a service identifier and a trade name in the access certificate, per ARF `Reg_33`/`Reg_34` and TS5.

**EE-RP-003 (SHALL).** The wallet SHALL display, before user approval, the relying party's and the service's trade names from the access certificate, a user-friendly description of the registered intended use, and a link to the applicable privacy policy (ARF `RPA_06`, `RPA_10`).

**EE-RP-004 (SHOULD).** Estonia SHOULD issue **Registration Certificates** (Topic 44, `RPRC_*`) rather than relying solely on dynamic register lookup. Registration certificates let the wallet check entitlement offline and without a network call that leaks the presentation event; dynamic lookup at presentation time reintroduces a phone-home. Registration certificates are optional under CIR 2025/848; choosing them is a privacy decision, not a convenience one.

**EE-RP-005 (SHALL).** Relying-party intermediaries are deemed relying parties (Art. 5b(10)) and SHALL NOT store transaction content data. Estonian payment and retail integrators acting for merchants fall squarely in this category.

**EE-RP-006 (SHALL).** Relying parties SHALL NOT refuse a pseudonym where identification is not required by law (Art. 5b(9)). Estonian relying parties habitually key customer records on `isikukood`; this requirement forbids demanding it where law does not.

### 5.3 Certificate transparency

**EE-GOV-012 (SHOULD).** Estonia SHOULD implement ARF Topic 55 (Certificate Transparency, `CT_01`–`CT_06`) for access certificates and for the ZK circuit registry defined in §10.6, so that mis-issuance of an access certificate or silent substitution of a proving circuit is publicly detectable.

---

## 6. Onboarding and activation

### 6.1 Assurance requirement

Art. 5a(5)(d) requires LoA High. CIR 2026/798 (7 April 2026) permits onboarding by an eID means at LoA **substantial** combined with additional remote onboarding procedures that together reach High.

Estonia's notified eID scheme (OJ 2018/C 401/08, notified 7 November 2018, **LoA High**) covers: ID card, residence permit card, Digi-ID, e-Residency Digi-ID, Mobile-ID, and the Diplomatic identity card.

**Smart-ID is not in the notified scheme.** RIA assessed it as LoA High in August 2019 and TARA and GovSSO accept it; SK ID Solutions is a QTSP and Smart-ID is an eIDAS-certified QSCD; Smart-ID is separately recognised at the highest assurance level in Belgium. But the EC's notified-schemes register contains no Smart-ID entry for Estonia. This is a real constraint and it is frequently glossed over in Estonian discussion.

**EE-ONB-001 (SHALL).** Wallet activation SHALL be performed using an eID means within Estonia's notified LoA High scheme, or using an onboarding procedure conforming to CIR 2026/798 and CEN/TS 18098 (ratified 17 March 2026, available 3 June 2026).

**EE-ONB-002 (MAY).** Smart-ID MAY be used for activation only if (a) Estonia notifies it under Art. 9, or (b) it is used as the LoA-substantial component of a CIR 2026/798 composite procedure with documented additional measures. Absent (a) or (b), Smart-ID SHALL NOT be the sole activation means. Implementers should note the practical constraint that Smart-ID+ entry into state e-services requires a supported default browser and no private-browsing mode.

**EE-ONB-003 (SHALL).** The wallet SHALL support ID-card activation over NFC using the card's PACE/CAN path in addition to a contact reader, and SHALL support both the legacy SK/IDEMIA and the current **Zetes/Thales** trust chains.

### 6.2 The November 2025 discontinuity

On 17 November 2025 Estonia changed both card manufacturer (IDEMIA → **Thales**) and certificate issuer (SK ID Solutions → **Zetes Estonia OÜ**), and stood up its own state root CA. Every Estonian relying party must now support two chains, two OCSP responders (`http://ocsp.eidpki.ee` for the new chain) and two LDAP directories. Certificate **suspension is no longer possible** on Thales cards — only revocation. Cross-border authentication with the new cards was unavailable at first and was restored **from 1 July 2026**.

Per the Zetes certificate profile and the live CA certificates at `crt.eidpki.ee`: root CA `EEGovCA2025` (`O=Zetes Estonia OÜ, C=EE`, self-signed) and intermediate `ESTEID2025`, both **ECDSA on NIST P-384 (`secp384r1`), signed `ecdsa-with-SHA384`**, subject `serialNumber` of the form `PNOEE-<isikukood>` per ETSI EN 319 412-1 §5.1.3, with `CN` of the form `SURNAME,GIVENNAME,ISIKUKOOD`, separate AUTH and QES certificates, and per-document-type policy OIDs (e-resident Digi-ID `1.3.6.1.4.1.51361.2.1.1.6`; diplomatic ID `1.3.6.1.4.1.51455.2.1.1`).

**EE-ONB-004 (SHALL).** Activation SHALL verify the card certificate against the correct chain and its OCSP responder, and SHALL NOT assume a single issuer.

**EE-ONB-005 (SHALL).** Activation SHALL NOT copy the certificate `serialNumber` (`PNOEE-<isikukood>`) into any attestation subject identifier other than the PID's `personal_administrative_number`. See §7.4 and Annex D.

### 6.3 e-Residents

Estonia has over 142,000 e-residents from more than 185 countries (official dashboard, last updated 16 July 2026). e-Residency is moving toward a cardless, mobile model in 2027, with remote biometric capture contracted to X Infotech.

**EE-ONB-006 (SHOULD).** The EE-EUDIW SHOULD support e-Residency Digi-ID activation, and the resulting PID SHOULD carry an `attestation_legal_category` or equivalent marker distinguishing e-residency from residence, so relying parties do not mistake an e-resident for a resident. e-Residents are not Estonian residents and the distinction has tax, benefit and legal consequences.

**EE-ONB-007 (SHALL NOT).** An e-Residency-derived PID SHALL NOT be used to satisfy an EE-PoA issuance whose statutory basis requires residence.

### 6.4 Wallet unit activation and revocation

**EE-ONB-008 (SHALL).** Activation SHALL conform to ARF Topic 40 (34 requirements, numbered `WIAM_01`–`WIAM_21` with letter suffixes), including the WSCD-related requirements.

**EE-ONB-009 (SHALL).** The wallet SHALL be revocable on explicit user request, on security compromise, and on death or cessation (Art. 5a(9)), per ARF Topic 38 (`WURevocation_*`). Death is a live case in Estonia: the Population Register is authoritative and revocation SHALL be driven from it over X-tee rather than waiting for a family member's request.

**EE-ONB-010 (SHALL).** Estonia SHALL implement CIR 2025/847: on a security breach of the wallet, the Member State suspends provision and use **without undue delay**.

---

## 7. Person Identification Data

### 7.1 Format and identifiers

Per the PID Rulebook **v1.7 (17 July 2026)** in the attestation-rulebooks catalog (moved out of ARF Annex 3.01 at ARF 2.5.0):

| | |
|---|---|
| ISO/IEC 18013-5 doctype | `eu.europa.ec.eudi.pid.1` |
| ISO/IEC 18013-5 namespace | `eu.europa.ec.eudi.pid.1` |
| SD-JWT VC `vct` (base) | `urn:eudi:pid:1` |
| SD-JWT VC `vct` (Estonian domestic) | `urn:eudi:pid:ee:1` |

**EE-PID-001 (SHALL).** The Estonian PID SHALL be issued in **both** encodings — ISO/IEC 18013-5 mdoc and IETF SD-JWT VC — per CIR 2024/2979 Annex II as replaced by CIR 2026/1731, which points to ETSI TS 119 472-1 V1.2.1 clause 5 (SD-JWT VC) and clause 6 (mdoc). W3C VCDM 2.0 remains optional and out of scope for Estonian PID.

### 7.2 Attributes

Mandatory (PID Rulebook v1.7): `family_name`, `given_name`, `birth_date`, `birth_place`, `nationality`, `portrait`. Mandatory inclusion of `portrait` applies from 24 months after entry into force of the Regulation amending CIR 2024/2977, and the user may opt out where applicable.

Mandatory metadata (CIR 2024/2977 §2.4): `issuing_authority`, `issuing_country`.

Optional (CIR 2024/2977): `resident_address`, `resident_country`, `resident_state`, `resident_city`, `resident_postal_code`, `resident_street`, `personal_administrative_number`, `family_name_birth`, `given_name_birth`, `sex`, `email_address`, `mobile_phone_number`. Optional metadata: `expiry_date`, `issuance_date`, `document_number`, `issuing_jurisdiction`. Rulebook-specific optional: `trust_anchor`, `attestation_legal_category`.

**There are no age attributes in the PID.** The PID Rulebook changelog for v1.1 (4 September 2025) records: *"Age verification attributes removed, following CIR 2024/2977."* `age_over_18`, `age_over_NN`, `age_in_years` and `age_birth_year` existed in earlier ARF drafts and are gone from the PID data model — but the ARF 3.0.0 baseline this document is written against still names them, conditionally: the note to `PID_12` reads "The value of the `age_over_18`, `age_over_NN`, or `age_in_years` attributes, **if present**, changes whenever the User ... has a relevant birthday", and the Annex 5.02 design guide uses `age_over_NN` as its example attribute. The substantive claim stands — no age attribute is in the PID data model — but a reader who checks "gone" against the stated baseline will find these references left in place. Any Estonian design that plans to answer an age question from the PID is building against a superseded rulebook.

**EE-PID-002 (SHALL).** The Estonian PID SHALL carry `personal_administrative_number` = the `isikukood`, and `issuing_country` = `EE`.

**EE-PID-003 (SHALL NOT).** The Estonian PID SHALL NOT be presented to satisfy an age check. Age checks SHALL be satisfied by an EE-PoA or an EU AV Profile attestation (§9).

### 7.3 Sourcing

**EE-PID-004 (SHALL).** PID attributes SHALL be sourced from the **Population Register** (authoritative for person data; controller Ministry of the Interior, processor SMIT) and the **identity-documents database** (controller PPA, processor SMIT) over **X-tee**, with the query and response logged in the security-server chain per X-tee's *jälgitavus* principle.

**EE-PID-005 (SHALL).** Portrait retrieval SHALL go through the documents database. Biometric templates in **ABIS** SHALL NOT be exposed to the wallet or to relying parties; ABIS deliberately holds only biometrics and no biographical data, and its data is access-restricted with criminal-procedure use gated on prosecutorial authorisation.

**EE-PID-006 (SHALL).** X-tee queries made for PID issuance SHALL be visible to the data subject through **Andmejälgija**, the existing data-tracker surface, in addition to the wallet's own dashboard. Estonians already expect to see who queried them; the wallet should not create a blind spot in a transparency mechanism that already exists.

### 7.4 The isikukood problem

The `isikukood` (EVS 585:2007) is an 11-digit code `SYYMMDDSSSC`. Digit 1 encodes century *and* sex (1/2 = 1800s M/F, 3/4 = 1900s, 5/6 = 2000s, 7/8 = 2100s); digits 2–7 are the birth date as `YYMMDD`; digits 8–10 are a serial; digit 11 is a two-round mod-11 checksum with first-round weights `1 2 3 4 5 6 7 8 9 1` and second-round weights `3 4 5 6 7 8 9 1 2 3`. Full algorithm in Annex D.

Consequences that this specification treats as binding:

- The `isikukood` **is** the date of birth and sex, in the clear. Disclosing it discloses exact age.
- It is printed on documents, embedded in every ID-card certificate `serialNumber` as `PNOEE-<isikukood>` and in the `CN`, and is broadcast by Eesti äpp as a plaintext barcode.
- It is a stable, lifelong, universally-used correlator across every Estonian public and private database.

**EE-PID-007 (SHALL NOT).** No EE-PoA attestation, ZK proof, pseudonym, key identifier, device-binding value, salt, or transport identifier used in an age-verification flow SHALL be derived from, contain, or be correlatable to the `isikukood`.

**EE-PID-008 (SHALL NOT).** A relying party performing only an age check SHALL NOT request `personal_administrative_number`, `birth_date`, `sex`, `portrait`, or any PID attribute, and its registered intended use SHALL NOT include them. The Registrar SHALL reject such a registration for an age-check-only service.

**EE-PRI-001 (SHOULD).** Estonia SHOULD treat the wallet rollout as the occasion to begin retiring `isikukood` as a *proof* of anything, reducing it to what it actually is — a database join key with no authentication value.

---

## 8. Attestation catalogue for Estonia

| Attestation | Type | doctype / vct | Provider | Priority |
|---|---|---|---|---|
| PID | PID | `eu.europa.ec.eudi.pid.1` / `urn:eudi:pid:ee:1` | RIA | P0 |
| **EE Proof of Age** | Non-qualified EAA (§9.7; divergence §22 item 8) | `ee.riik.poa.1` (§9) | RIA | **P0** |
| EU AV Profile Proof of Age | EAA | `eu.europa.ec.av.1` | RIA (interop) | **P0** |
| mDL | PuB-EAA | `org.iso.18013.5.1.mDL` | Transpordiamet | P1 |
| EHIC | PuB-EAA | per DC4EU rulebook | Tervisekassa | P2 |
| Powers and mandates (Pääsuke) | PuB-EAA | national | RIA | P2 |
| Tax residency / arrears certificate | PuB-EAA | national | Maksu- ja Tolliamet | P2 |
| Educational qualification | QEAA/PuB-EAA | per DC4EU | HTM / Eesti Hariduse Infosüsteem | P3 |
| e-Residency status | PuB-EAA | national | PPA | P3 |

**EE-GOV-020 (SHALL).** The mDL SHALL be issued as ISO/IEC 18013-5 mdoc only. The mDL Rulebook states that mDLs issued to a Wallet Unit SHALL NOT be implemented as SD-JWT VC.

**EE-GOV-021 (SHALL).** Every Estonian attestation SHALL have a published Attestation Rulebook conforming to the ARF Attestation Rulebook template and registered in the Commission's catalogue of attestations (ARF Topic 25/26, TS11).


---

## 9. The Estonian Proof-of-Age attestation (EE-PoA)

### 9.1 Why a dedicated attestation

Three independent reasons:

1. **The PID no longer carries age attributes** (§7.2). There is nothing to selectively disclose.
2. **A PID presentation is a heavier legal act than an age check.** Presenting PID engages identification; an age check must not. Art. 5b(9) forbids relying parties from refusing pseudonyms where identification is not required by law, and Estonian alcohol and tobacco law requires establishing *age*, not identity.
3. **Batched one-time attestations and ZK proofs need a credential whose entire attribute set is disclosable.** A PoA attestation containing only booleans can be presented in full with no privacy loss; a PID cannot.

The Commission's Age Verification Manual confirms the direction: the age use case runs on a "proof of age attestation" in ISO mdoc format issued by trusted authorities and listed on a Commission-maintained trusted list, not on a PID attribute. Denmark's AltID has already shipped the same shape — a Proof-of-Age attestation issued as a batch of one-time-use mdocs, deleted after each presentation, with no status list.

### 9.2 Identifiers and data model

| | |
|---|---|
| Estonian doctype / namespace | `ee.riik.poa.1` |
| EU interoperability doctype / namespace | `eu.europa.ec.av.1` |
| Format | ISO/IEC 18013-5 mdoc (CBOR), mandatory |
| Format | IETF SD-JWT VC, optional (`vct` = `urn:ee:poa:1`) |
| Legal class | Non-qualified EAA (Art. 45b(2)), issued by RIA on behalf of the Population Register; see §9.7.1 (EE-POA-020) |

Attributes in namespace `ee.riik.poa.1`:

| Identifier | Type | Presence | Estonian statutory basis |
|---|---|---|---|
| `age_over_18` | `bool` | Mandatory | Alkoholiseadus § 47; Tubakaseadus § 28; toto **and lottery** (HasMS § 34(3), as amended RT I, 30.12.2025, 2, in force 01.01.2026); general majority |
| `age_over_16` | `bool` | Optional | No current Estonian statutory gate sits at 16 — Hasartmänguseadus § 34(3) raised lottery to 18 with effect from 1 January 2026. Retained only because **HasMS § 83(1)** was not conformed and still penalises enabling lottery play by a person under **16**, and for sectoral and cross-border use. Omit in v1 unless a relying party demonstrates a legal basis |
| `age_over_21` | `bool` | Optional | **No Estonian statutory gate that an anonymous predicate can serve.** Hasartmänguseadus § 34(2) sets 21 for games of chance, but both channels that enforce it — remote play (§ 53(1)) and premises entry (§ 37(8)–(10)) — are identification duties, not age checks; see §9.3. Retained for cross-border and sectoral use only. Omit in v1 unless a relying party demonstrates a legal basis |
| `age_over_63` | `bool` | Optional | Pension-age discounts (threshold is rising on a statutory schedule; see note) |
| `age_over_NN` | `bool` | Optional, repeatable, `NN ≠` the above | Sector-specific |
| `issuing_country` | `tstr` | Mandatory | `EE` |
| `issuing_authority` | `tstr` | Mandatory | |
| `expiry_date` | `tdate` | Mandatory | §9.6 |

**EE-POA-001 (SHALL).** The EE-PoA SHALL contain **only** boolean age predicates and the three metadata attributes above. It SHALL NOT contain `birth_date`, `age_in_years`, `age_birth_year`, `sex`, name, portrait, document number, or `personal_administrative_number`.

*Rationale.* `age_in_years` and `age_birth_year` are effectively date of birth to within a year and are a strong correlator; the Commission's own AV Profile carries `age_over_18` alone for exactly this reason. Denmark's AltID carries multiple thresholds including `age_over_67` and this specification follows that pattern — but bounded thresholds only, never a numeric age.

**EE-POA-002 (SHALL).** The EE-PoA SHALL be presentable in full. A relying party SHALL request only the predicates it is legally entitled to, and the wallet SHALL enforce the registered intended use.

**EE-POA-003 (SHALL).** The wallet SHALL also support the EU AV Profile attestation `eu.europa.ec.av.1` with its single mandatory attribute `age_over_18` (boolean), for cross-border acceptance by relying parties that integrated against the Commission's blueprint. The two attestations SHALL be issued from the same authoritative source in the same issuance transaction.

**Note on `age_over_63`.** Estonia's statutory pension age is on a rising schedule and is being indexed to life expectancy. A boolean pegged to a moving statutory threshold is a maintenance hazard. **[UNVERIFIED — the current and scheduled pension ages were not re-verified in this session.]** Implementers SHOULD parameterise this threshold in the rulebook rather than hardcoding it, or omit it in v1.

### 9.3 The gambling carve-out

**Hasartmänguseadus § 34(2)** sets 21 as the minimum age for games of chance and remote games of skill; § 34(3) sets 18 for toto and, since 1 January 2026, for lotteries. Age is not, however, the operative duty in either channel. Estonian gambling law imposes a full **identification and registration** duty on both sides of the counter, and it does so independently of the age bar:

- **Remote gambling — HasMS § 53(1).** The operator SHALL verify each player's identity and register, for every player, forename and surname, **`isikukood` (or, where the player has none, date of birth)**, and the date and time of entering and leaving the gaming environment. (Note: § 55 concerns player *information* — time played, stakes, winnings — and § 56 concerns blocking access to illegal remote gambling; neither creates the registration duty, and both are frequently miscited.) Breach of the § 53(1) duty is an offence under **§ 94**, punishable by up to 200 fine units or, for a legal person, **€26,000** (RT I, 30.12.2025, 2) — heavier than the entry duty below, not lighter.
- **Physical premises — HasMS § 37(8)–(10).** The organiser of a game of chance is obliged to establish the identity of **every person entering the gaming location**, and on doing so to register: forename and surname; `isikukood` or, failing that, date of birth; the **name, serial number, and place and date of issue of the identity document**; and the time and date of arrival. Under § 37(9) the entrant presents an identity document and **a copy is taken of its personal-data page**, the data going into an electronic database; under § 37(10) the organiser checks that database against the presented document before admitting the person. Breach is an offence under **§ 89**, punishable by up to 100 fine units or, for a legal person, **€20,000** (RT I, 30.12.2025, 2).
- **Self-exclusion — HasMS § 39.** The *hasartmängu mängimise piirangutega isikute nimekiri* is a sub-register of the taxpayers' register held by Maksu- ja Tolliamet; § 39(8) obliges organisers of games of chance, toto and classical lotteries to take measures ensuring a listed person cannot play. Screening a person against a name-keyed list is impossible from an anonymous predicate.

That is the exact opposite architecture to anonymous age proof, in **both** channels. It is not a flaw in the design; it is a legal requirement that overrides unlinkability.

**EE-POA-004 (SHALL).** Remote gambling SHALL be treated as an **identification** use case, not an age-proof use case. A licensed remote gambling operator SHALL request a PID presentation, not an EE-PoA, and its registered intended use SHALL reflect this.

**EE-POA-005 (SHALL NOT).** The `age_over_21` predicate SHALL NOT be used to satisfy HasMS § 53(1) player registration, and SHALL NOT be used to admit a person to a gaming location under HasMS § 37(8)–(10). Entry to a *õnnemängu mängukoht* is an identification use case: the organiser SHALL request a PID presentation carrying at least name, `isikukood` and identity-document data, and its registered intended use SHALL reflect this.

**EE-POA-005a (SHALL).** Where a relying party registers an intended use citing any provision of Hasartmänguseadus, the Registrar SHALL refuse registration of an EE-PoA predicate request and SHALL admit only a PID request. Games of chance, toto, remote games of skill and lotteries are all covered: the § 39(8) self-exclusion screen applies across them and cannot be satisfied from a predicate.

*Rationale.* Conflating these produces the worst outcome available: a relying party that believes an anonymous proof discharged a statutory identification duty, and a regulator that discovers this after the fact — here under the penal provision each channel carries: **§ 94**, up to €26,000 for a legal person on the remote duty, and **§ 89**, up to €20,000 on the entry-identification duty. The earlier revision of this specification asserted that "no registration duty attaches to the doorman." That was wrong; § 37(8)–(10) attaches one, and a heavier one than § 53(1) in that it compels a document copy. The correction removes the only Estonian statutory use case the `age_over_21` predicate had, which is why §9.2 demotes it to Optional. A narrow carve-out appears to survive on the face of the statute: the § 37(8) duty falls on the *organiser of a game of chance* in respect of a *gaming location*, and § 37(6¹) contemplates persons under 18 being present in a toto location with a segregated toto area, which suggests a toto venue is not within the entry-identification duty. **[This is an inference from the drafting, not a verified reading, and no confirmation was sought from Maksu- ja Tolliamet — UNVERIFIED; see §23 item 28.]** No EE-PoA predicate is defined at a threshold that serves such a venue, and none should be added on that basis alone. The AML route is **not** the operative one and is miscited in most commentary: gambling organisers are obliged entities under **RahaPTS § 2(1) p 3**, but § 19(3) triggers customer due diligence only at a stake or payout of **€2,000** or more, not at the door.

### 9.4 Issuance integrity — the enrolment problem

Independent analysis of the Commission's reference age-verification issuer found that its `mdocFormatter` accepts a client-reported birth date and signs a credential **without server-side ICAO passive authentication (SOD/DSC chain validation) or chip active authentication**, meaning the issuer holds no cryptographic proof that a genuine document was read, and a modified client can obtain a valid attestation for any age. **[Sourced to Yivi's published security analysis; not independently reproduced in this session — UNVERIFIED as to current code state.]**

The lesson generalises and is worth stating flatly: **a zero-knowledge proof at presentation time does not repair a trust-the-client enrolment.** A ZKP proves faithfully whatever the issuer signed; if the issuer signed a lie, the ZKP proves the lie unlinkably.

Estonia is in an exceptionally strong position here because it does not need to trust a document read at all.

**EE-POA-006 (SHALL).** EE-PoA age predicates SHALL be computed **server-side by the PID/PoA Provider from the Population Register date of birth retrieved over X-tee**. The wallet, the device, and any client-supplied document read SHALL NOT be an input to the predicate value.

**EE-POA-007 (SHALL).** Where onboarding used a document read (NFC passport or ID-card chip), the Provider SHALL perform full **passive authentication** — SOD signature validation, DSC chain validation to a trusted CSCA, and data-group hash comparison — and SHALL perform **chip authentication or active authentication** where the chip supports it. This requirement applies to the identity binding, not to the age value, which comes from the register regardless.

**EE-POA-008 (SHALL).** The Provider SHALL re-verify the register value at each re-issuance and SHALL NOT re-sign a cached predicate.

### 9.5 Batch issuance and one-time use (Tier 1 unlinkability)

ARF §7.4.3.5.2 defines four mitigations for relying-party linkability. **Method A, once-only attestations, is mandatory** for wallet solutions; Method B, limited-time attestations, is also mandatory and only partially mitigates; Methods C (rotating batch) and D (per-relying-party) are optional.

**EE-POA-010 (SHALL).** EE-PoA SHALL be issued as a batch of single-use technical attestations (ARF Method A), each bound to a distinct key pair, using OpenID4VCI 1.0 batch issuance via the `proofs` parameter and a key attestation covering the whole key set.

**EE-POA-011 (SHALL).** Batch size SHALL be at least **30**, **except on a device whose WSCD cannot sustain 30 keys, where EE-POA-011b governs and sets the batch size instead**. Batch size SHOULD otherwise be tuned to observed usage. The Commission's AV specification recommends 30. First-time batch issuance SHALL require only a single user authentication regardless of batch size (ARF `ISSU_61`).

**EE-POA-011a (SHALL).** Batch key generation SHALL NOT occur on the presentation critical path. The wallet SHALL generate the key set and obtain the batch **ahead of need** — on re-issuance scheduling under EE-POA-014, on a charging-and-connected trigger, or at latest when the remaining batch falls below a low-water mark — and SHALL NOT begin a key generation run in response to a relying party's request.

**EE-POA-011b (SHALL).** The wallet SHALL treat WSCD key-slot capacity as a bounded resource, and SHALL destroy the key material of consumed attestations under EE-POA-013 so that capacity is returned. Where the device's WSCD can sustain 30 concurrent attestation keys, EE-POA-011 applies unchanged and the batch SHALL be at least 30. Where it cannot, and **only** where it cannot, the wallet SHALL issue a batch of the largest size the WSCD is measured to sustain, SHALL NOT fail issuance, SHALL record the reduced size and the resulting reduction in Method A coverage in the dashboard under EE-POA-015, and SHALL disclose it to the user in the same plain terms EE-ZKP-042 requires for a tier downgrade. A batch reduced for any reason other than measured WSCD capacity — throughput tuning, issuer convenience, storage economy — is **non-conformant**.

*A limit of Method A that batching does not reach.* A batch minted in one sitting falls inside a single Android RKP key-rotation epoch (roughly 14 days) and is therefore attested by one per-app attestation key, so batch members remain mutually linkable **at the attestation layer** even though §9.5 makes them unlinkable at the mdoc layer. EE-SEC-004 keeps that layer away from relying parties by attesting to the Wallet Provider only, which is what bounds the exposure; it does not remove it. Tier 2 (§10) is the only tier where the property holds against the attestation layer as well.

*The conformance outcome, stated once.* A device that holds 30 keys is conformant at 30 or more, and at nothing less. A device that cannot is conformant at its measured maximum, with that maximum recorded and disclosed; it is not excused from EE-POA-010's once-only model, only from the count. There is no third case, and no reading on which a wallet may quietly ship a batch of five.

*Why this is a requirement and not an implementation note.* Thirty distinct hardware-backed key pairs per batch, per attestation type, is a substantial ask of a discrete secure element. Generating a P-256 key in Android StrongBox is orders of magnitude slower than in the TEE, key slots are finite, and on some devices a long generation run is visible to the user as a stall. The failure this guards against is not theoretical but banal: a first-time issuance that appears to hang, or a presentation that stalls because the wallet chose that moment to refill. Pre-generation makes the cost invisible; a measured batch size makes it bounded. **[No StrongBox key-generation timings or slot-capacity limits were measured or sourced in this session — UNVERIFIED. Measured figures for the reference device class should be published before pilot and EE-POA-011's floor of 30 revisited against them; the open item is §23 item 26.]**

**EE-POA-012 (SHALL).** All attestations in a batch SHALL carry **identical `ValidityInfo` timestamps** — `signed`, `validFrom` and `validUntil` coarsened so that hour, minute and second are the same across the batch, per the ISO/IEC 18013-5 recommendation. Uncoarsened timestamps are themselves a linking vector and silently defeat the whole mechanism.

**EE-POA-013 (SHALL).** A plain-mdoc (non-ZKP) EE-PoA presentation SHALL consume the attestation: the wallet SHALL delete it, SHALL destroy the corresponding WSCD key material (ARF `WIAM_21`) so that the key-slot capacity EE-POA-011b treats as bounded is returned, and SHALL NOT present it again. The wallet SHALL NOT consume the last remaining attestation in the batch, since Method A degrades to Method B when the batch is exhausted. Destruction is mandatory and not opportunistic: EE-POA-011b's measured batch size depends on the capacity being returned.

**EE-POA-014 (SHALL).** Re-issuance SHALL go to the same Wallet Unit, using a device-bound refresh token per OpenID4VCI §14.5 (ARF `ISSU_65`).

**EE-POA-015 (SHALL).** The wallet SHALL log each re-issuance event in the user-visible dashboard. Batch re-issuance is itself a processing operation and the Spanish DPA has raised precisely this point in the ARF discussion process; transparency is the mitigation.

### 9.6 Validity and revocation

**EE-POA-016 (SHALL).** EE-PoA validity SHALL NOT exceed **3 months**, and the user SHALL be re-identified at least every 3 months as a condition of re-issuance.

**EE-POA-017 (SHALL NOT).** EE-PoA SHALL NOT use a status list, an OCSP-style check, or ISO/IEC 18013-5 server retrieval.

*Rationale.* Every online status check at presentation time hands the issuer a real-time record that this credential was just used, which is the exact harm Art. 5a(5)(b) and Art. 5a(16)(a) prohibit. Short validity plus single use is the correct trade. The Danish AltID design reaches the same conclusion — "PoA attestations do not make use of status (revocation) lists" — and the Commission's AV specification treats revocation as out of scope for age verification. ARF TS14 notes independently that Token Status Lists fail `ZKP_07`.

**EE-POA-018 (SHALL).** The residual risk — an EE-PoA remaining valid for up to 3 months after the underlying entitlement changes — SHALL be documented in the risk register. For age predicates the risk is asymmetric and benign: a person's `age_over_18` never becomes false. This is the specific property that makes revocation-free operation defensible for age and **not** for other attestations.

**EE-POA-019 (SHALL).** ISO/IEC 18013-5 **server retrieval SHALL NOT be used** for any Estonian attestation, PoA or otherwise.

### 9.7 The legal class of the EE-PoA — an unavoidable trade-off

There is a direct conflict between the privacy design above and the qualified-attestation rules, and it must be resolved deliberately rather than discovered during certification.

**CIR (EU) 2025/1569 Art. 4(3), as replaced by CIR (EU) 2026/1735**, requires QEAA and PuB-EAA providers, *whenever those attestations are issued with a validity period of more than 24 hours*, to revoke them in the listed circumstances, including at the subject's request. CIR 2026/1731's Annex IV sets the same 24-hour threshold (`EAA-4.2.13-03`: revocation is not required only where validity is 24 hours or less).

An EE-PoA with a 3-month validity and no status mechanism (EE-POA-016, EE-POA-017) therefore **cannot** be a PuB-EAA under Art. 45f. Three resolutions exist:

| Option | Consequence |
|---|---|
| **(a) Issue the EE-PoA as a non-qualified EAA** issued by a public sector body, listed on a national trusted list analogous to the Commission's AV Trusted List (ETSI TS 119 612, service type `PAA`) | Keeps the privacy design intact. Loses Art. 45b's automatic legal equivalence to a paper attestation and Art. 45b's cross-border recognition guarantee. Matches what Denmark's AltID and the Commission's own AV attestation actually are |
| **(b) Keep PuB-EAA status and implement revocation** | Reintroduces an issuer-visible status check at or near presentation time, defeating Art. 5a(5)(b) and `ZKP_07`. Not acceptable |
| **(c) Keep PuB-EAA status with ≤ 24-hour validity** | Escapes the revocation duty by the regulation's own carve-out, but forces daily re-issuance — a per-day issuer contact that is itself a coarse activity signal, and an availability dependency at the point of sale |

Option (c) is the one most often proposed as the obvious fix, and it is the one this specification rejects most deliberately, so the reasons are set out rather than left implied:

- **It converts a three-monthly issuer contact into a daily one.** Art. 5a(5)(b) is not satisfied by a contact that carries no relying-party identity; the issuer still learns that this user's wallet was provisioned for use today, every day, which over a year is a 365-point activity trace where the current design yields four. A coarse signal collected daily is not a small signal.
- **It makes the point of sale depend on issuer availability.** A user whose batch expired overnight and who is out of connectivity at a shop counter cannot prove their age at all. The three-month design degrades to Method B when a batch is exhausted; the 24-hour design degrades to failure.
- **It buys a contested benefit.** What (c) purchases is PuB-EAA status, and therefore a *stronger argument* for Art. 45b(2)/(3) — not a settled one, because the class question in item 4 below is unresolved either way.
- **It does not remove the obligation load.** A PuB-EAA provider carries the qualified-provider supervisory regime, which is a larger programme cost than the Art. 19a(1) load priced in §9.7.1.

Option (c) is therefore recorded as evaluated and rejected, not overlooked. It SHOULD be re-evaluated if the class question in item 4 is resolved against public-sector non-qualified attestations, because in that case the cross-border cost of (a) rises sharply and the daily-contact cost of (c) may become the lesser harm.

**EE-POA-020 (SHALL).** The EE-PoA SHALL be issued as **option (a)** — a non-qualified electronic attestation of attributes issued by a public sector body, published on an Estonian trusted list of Proof-of-Age attestation providers. The Attestation Rulebook SHALL state this class explicitly, and the wallet SHALL NOT display the EE-PoA as qualified.

**EE-POA-021 (SHALL).** Where a relying party has a legal requirement for a *qualified* age attestation, it SHALL request a PID presentation with `birth_date` under a registered intended use that names it, and SHALL accept that this transaction is identifying. There is currently no way to be simultaneously qualified, long-lived, and unlinkable, and this specification does not pretend otherwise.

#### 9.7.1 The price of option (a), stated

Option (a) is chosen knowingly, but the earlier text under-stated its cost. Three consequences follow from the non-qualified class and none of them is cosmetic.

**1. Regulatory obligations do not disappear; they change shape.** A non-qualified trust service provider is bound by **Art. 19a(1)**: risk-management policies covering registration and onboarding, procedural and administrative checks, and the management and implementation of trust services; plus breach notification to the supervisory body, affected individuals and — where of public interest — the public, "without undue delay and in any case no later than 24 hours" of becoming aware. **CIR (EU) 2025/2160** of 27 October 2025 lays down the reference standards, specifications and procedures for Art. 19a(1)(a); Art. 2 of that Regulation requires the risk-management policies to be specific to the trust services concerned, approved by the management body, and to state an overall risk-tolerance level and the risk criteria including likelihood, impact and level, taking account of cyber threat intelligence and vulnerabilities. Compliance is *presumed* only where those standards are met (recital 3).

**EE-POA-022 (SHALL).** The PoA Provider SHALL implement Art. 19a(1) and CIR (EU) 2025/2160, and the risk-management policy SHALL name the EE-PoA service explicitly rather than folding it into a generic RIA policy. The 24-hour breach-notification clock SHALL be operationalised, with a named duty holder, before pilot.

**2. Supervision is ex post, which is a programme risk and not a relief.** Art. 17(3)(b) confines the supervisory body to *ex post* supervisory activities for non-qualified providers, acting "when informed" of an alleged failure. There is no pre-market conformity assessment to catch a design error, and no supervisory sign-off to point at afterwards. The compensating control is this specification's own evaluation requirements and the transparency obligations in §14.

**3. Liability shifts onto the user.** **Art. 13** places the burden of proving intention or negligence of a *non-qualified* trust service provider on "the natural or legal person claiming the damage". For a qualified provider the position is reversed: intention or negligence "shall be presumed unless that qualified trust service provider proves that the damage … occurred without the intention or negligence" of that provider. A person wrongly refused alcohol, tobacco or premises entry because of a defective EE-PoA therefore starts from a materially worse evidential position than the same person refused on a qualified attestation. This is a real transfer of risk from the State to the individual and SHALL be recorded as such in the DPIA and the risk register.

**4. Paper-equivalence and cross-border recognition are at best unresolved.** **Art. 45b(2)** confers the same legal effect as a lawfully issued paper attestation on two classes: qualified EAAs, *and* "attestations of attributes issued by, or on behalf of, a public sector body responsible for an authentic source". **Art. 45b(3)** requires such public-sector attestations to be recognised in all Member States. The EE-PoA is issued by a public sector body from an authentic source (the Population Register) and on a literal reading falls inside both. But Art. 45f sets the *requirements* for that class, and an EE-PoA that deliberately does not meet the revocation duty in CIR 2025/1569 Art. 4(3) as replaced by CIR 2026/1735 does not meet Art. 45f. Whether Art. 45b(2) describes an issuer class or a compliant class is not settled by the text.

**EE-POA-023 (SHALL).** This specification SHALL NOT claim Art. 45b(2) paper-equivalence or Art. 45b(3) cross-border recognition for the EE-PoA. The Attestation Rulebook SHALL state that the question is open, and the wallet SHALL NOT present the EE-PoA to the user as legally equivalent to a document. Where equivalence is legally required, EE-POA-021 applies. See §23 item 18.

**EE-POA-024 (SHALL).** Because the EE-PoA's cross-border standing is unresolved, the **EU AV Profile attestation `eu.europa.ec.av.1` of EE-POA-003 SHALL be the designated cross-border age-proof path**, and, for a relying party whose registration certificate names a Member State other than Estonia, the wallet SHALL select it in preference to the EE-PoA **only after negotiating that the presentation request accepts `eu.europa.ec.av.1` and that the requested age attribute or threshold is the one the AV Profile carries (`age_over_18`)**. Where the request does not accept the AV Profile attestation, or asks for a threshold the AV Profile does not carry, the wallet SHALL continue with a supported credential — the EE-PoA, where the relying party's registered intended use admits it — and SHALL NOT serve an unmet threshold by presenting the nearest one it holds; where no supported credential exists, the failure SHALL be surfaced to the user. Because the two attestations are issued in the same transaction from the same authentic source (EE-POA-003), a successful negotiation costs the user nothing at presentation time.

*Why that is the answer to the cross-border objection, and what it does not answer.* The practical content of Art. 45b(3) recognition, for age proof specifically, is that a shop in another Member State accepts the presentation. That is delivered by conforming to the Commission's own blueprint attestation, which relying parties across the Union are integrating against, rather than by winning the Art. 45b(2) class argument for a national attestation. It is the same route Denmark's AltID takes. What it does not deliver is paper-equivalence for the EE-PoA itself, or recognition for any predicate beyond `age_over_18` — the AV Profile carries that one attribute only. A relying party abroad needing a different threshold has no interoperable anonymous route, and EE-POA-021 applies.

Non-qualified EAAs retain **Art. 45b(1)** in all cases: an attestation "shall not be denied legal effect or admissibility as evidence in legal proceedings on the sole ground that it is in electronic form or that it does not meet the requirements for qualified electronic attestations of attributes". That is the floor, and it is the floor this specification relies on.

---

## 10. Zero-knowledge proof profile

### 10.1 The requirement this profile answers

ARF v3.0.0, §7.4.3.5.3, verbatim:

> "Unlike Relying Party linkability, Attestation Provider linkability cannot be fully eliminated when using attestation formats using a proof mechanism based on salted hashes. The only viable mitigation is to adopt Zero-Knowledge Proofs (ZKPs) as a proof mechanism instead of relying on salted-attribute hashes."

The mechanism is identical in mdoc and SD-JWT VC — ARF §5.4.3 says SD-JWT VC's approach "is conceptually identical to the mechanism used for the same purpose in ISO/IEC 18013-5". Salting randomises per-attribute digests; it does not randomise the **issuer's signature over the MSO or JWT**, which is a static, globally unique value presented verbatim to every verifier. A verifier who shares that signature with the issuer, or with another verifier, links the presentations. Batching hides this from *relying parties*; it does nothing against the *issuer*.

This was the central finding of the June 2024 open letter *Cryptographers' Feedback on the EU Digital Identity's ARF*, signed by sixteen cryptographers including Camenisch, Lysyanskaya, Preneel, Troncoso, Tessaro and shelat, which stated that unlinkability "cannot be ensured under the assumption of collusion between IdP and RP, making this solution incompatible with Regulation 2024/1183."

**EE-ZKP-001 (SHALL).** The EE-EUDIW SHALL implement a ZKP presentation path for EE-PoA and for the EU AV Profile attestation, conforming to this section.

**EE-ZKP-002 (SHALL).** The wallet SHALL NOT claim, in its user interface, its documentation, or any conformity statement, that presentations are unlinkable by the attestation provider unless the ZKP path is enabled **and** the relevant relying parties can verify it. A ZKP implemented only in a demo build variant, or produced by a prover that no production verifier can consume, is not a privacy property. This has been a documented criticism of the EU age-verification app and this specification treats it as a conformance failure, not a roadmap item.

### 10.2 Derived requirements from ARF Topic 53

ARF v3.0.0 Annex 2 Topic 53 (`ZKP_01`–`ZKP_09`; harmonized identifiers `EW-DM-53-001`…`EW-DM-53-008`, with `ZKP_04` carrying `AS-AP-53-001`). Quoted from the ARF as retrieved on 5 September 2026:

- **ZKP_01 (SHALL)** — "A ZKP scheme SHALL provide support for the following generic functions, while hiding all attributes of PIDs or attestations: (i) generation of a proof that an (some) attribute(s) having a specific value is (are) included in a PID or attestation, (ii) generation of a proof that a PID or attestation is within its validity period, (iii) generation of a proof that a PID or attestation has not been revoked, and (iv) generation of a proof that a PID or device-bound attestation is bound to a key stored in the WSCA/WSCD or in a keystore of the Wallet Unit." Plus a SHOULD for (v): proof of issuance by a trusted provider without revealing which — **issuer hiding**, to be used only where necessary.
- **ZKP_02 (SHALL)** — "A ZKP scheme SHALL support proving possession of attestation of a given type."
- **ZKP_03 (level column: SHALL; body text: SHOULD)** — "A ZKP scheme SHOULD support the privacy-preserving binding of an attestation to a PID… (i) generation of a proof that the Wallet Unit stores an attestation and a PID and that the attestation includes a specific attribute, having a specific value, which is also present in the PID."
- **ZKP_04 (level column: SHALL; body text: SHOULD)** — "A ZKP scheme SHOULD support the derivation of a verifiable User pseudonym, by combining an attribute value that is unique for the User with Relying Party-specific context… (i) generation of a request for the issuance of an attestation that includes a secret attribute unique to the User, without revealing this attribute to the Attestation Provider, (ii) generation of an attestation presentation that includes a verifiable pseudonym derived from the secret attribute, a Relying Party identifier, and context-related information."
- **ZKP_05 (SHALL)** — "A ZKP scheme SHALL be usable in both remote and proximity presentation flows. While the inclusion of ZKP will introduce computational and verification delays, these delays SHALL NOT critically undermine or defeat the purpose of the Relying Party service (e.g. because of a critical impact on the User experience of the Wallet Unit)."
- **ZKP_06 (SHOULD)** — "A ZKP scheme SHOULD be able to generate proofs for already issued PIDs and attestations in the formats specified in ISO/IEC 18013-5 or SD-JWT VC."
- **ZKP_07 (SHALL)** — "A ZKP scheme SHALL NOT introduce any additional communication or information that could be used to track or link User activity during, before, or after proof generation."
- **ZKP_08 (SHALL)** — "A ZKP scheme SHALL rely solely on algorithms included in the ECCG Agreed Cryptographic Mechanisms v2.0."
- **ZKP_09 (SHALL)** — "Use of a ZKP scheme SHALL NOT prevent the Wallet Unit's ability to provide User authentication with Level of Assurance High."

Related, in ARF Topic 18 (*Combined presentations of attributes*): **ACP_02 (SHALL)** — a cryptographic binding scheme "SHALL rely solely on algorithms included in the ECCG Agreed Cryptographic Mechanisms v2.0"; **ACP_03 (SHOULD)** — it "SHOULD be implemented using a Zero-Knowledge Proof mechanism that satisfies the requirements specified in Topic 53."

The same ARF section that mandates ZKPs as the only viable mitigation opens with the note: *"Discussions on Zero-Knowledge Proofs (ZKPs) are ongoing. No specific ZKP has been selected to be supported by components in the EUDI Wallet ecosystem."* Topic G is reopened as a refinement round; it sits in **Iteration 6, 23 September – 18 November 2026**, alongside Topic F (Digital Credentials API) and Topic AC (cryptographic binding of attestations), per the ARF's own `docs/discussion-topics/README.md` at the pinned baseline commit. That window opens a week after this revision was written, and it is the only open venue for the ZKP_08 clarification §10.3 asks for — the ACM v3.0 public review closed at the end of July 2026. **The Union has stated a requirement and has not chosen a scheme.** That is the gap this section fills for Estonia, and it is why §10.4 specifies two tracks rather than one.

### 10.3 The ZKP_08 problem, stated plainly

**Whether any candidate ZKP scheme satisfies ZKP_08 depends on a reading of ZKP_08 that no authority has given.** The requirement says a ZKP scheme "SHALL rely solely on algorithms included in" the ACM. Two readings are available and they lead to opposite engineering conclusions:

- **(a) Primitives and assumptions.** The scheme relies solely on ACM-listed *primitives*, and its security reduces to assumptions the ACM already accepts. This is how the Commission's own Technical Specifications, ETSI TR 119 476-1 and ANSSI use the ACM elsewhere — the ACM is a catalogue of primitives, and Note 1 of its own §1 treats listing as a *necessary* condition on the primitives a mechanism uses, not as a whitelist of composed protocols. Under this reading Longfellow invokes SHA-256, AES-256 and ECDSA over P-256 and nothing else, all three listed in v2.0, with the proof system's soundness reducing to collision resistance of SHA-256 in the random-oracle model — but AES-256 being listed does not itself list the *construction* Longfellow builds from it: AES-256-in-counter-mode used as a bare Fiat–Shamir challenge PRF is not one of the SP 800-90A DRBG constructions ACM §7 lists, and the soundness error of the shipped circuits sits below the ACM's own recommended threshold. Compliance under this reading is therefore **conditional, not already met**, on satisfying EE-ZKP-022a's soundness floor, EE-ZKP-022b's assumption map covering exactly this PRF gap, and EE-ZKP-022c's recommended-tier ciphersuite — see §10.5.
- **(b) The scheme itself is enumerated.** The ACM must list the *proof system* by name. Under this reading **no candidate qualifies and none can for years**: the v3.0 working draft's new Annex C requires a mechanism to have been standardised, stable for two years and peer-reviewed before it may even be submitted for listing, and no ZKP proposal appears on any ECCG list.

The ACM itself does not resolve the question: neither v2.0 nor the v3.0 draft mentions zero-knowledge proofs, pairings, bilinear maps, BBS, commitments, Merkle trees, extension fields or Fiat–Shamir — not once. It does not list BBS, any pairing-friendly curve, or any zkSNARK, which is the fact reading (b) turns on.

*On the ACM's own version state, stated exactly.* The applicable version is **v2.0 (April 2025)**, which is the version ZKP_08 pins by name and which ENISA still shows as applicable. The **v3.0 text is a working draft**, dated April 2026 and published by ENISA for public review on 2 June 2026, the review closing at the end of July 2026; it was read in full on 8 September 2026. **There is no ACM v3.0 final text to read** — an earlier revision of this section marked this `[UNVERIFIED]` on the assumption that there was one.

Two further observations soften reading (b) without resolving it:

- Longfellow's soundness rests on hash functions — the IACR paper states the proof system "only relies on SHA-256 as its complexity assumption", with AES-256 used in the implementation, both listed in ACM v2.0 — with no pairings, no trusted setup, and no novel algebraic assumption. A defensible reading is that ZKP_08 is satisfied at the *primitive* level even though the *scheme* is not itself listed. This reading has not been endorsed by the ECCG.
- BBS-family schemes require pairing-friendly curves that are definitively not listed, and additionally face a security-level problem: Cheon's attack against q-SDH, with an explicit algorithm published applying it to BBS, means **BLS12-381 yields less than 128-bit security for BBS**. TS14's remedy is migration to BLS24-509, "expected to appear in an upcoming version of `draft-irtf-cfrg-pairing-friendly-curves`." CL and PS signatures are structurally immune and retain 128-bit security on BLS12-381.

**EE-ZKP-003 (SHALL).** The wallet's conformity documentation SHALL state the ZKP_08 position explicitly and conditionally: which primitives are used, which are in ACM v2.0, which are not, and what the migration trigger is. It SHALL NOT assert unqualified ZKP_08 compliance. It SHALL further record **which of the two readings above the assessment applies**, and carry, under reading (a), an assumption map from each primitive the scheme uses to its ACM entry, or, under reading (b), a written statement that the requirement is unsatisfiable by any scheme available in the certification window. Leaving the reading implicit is what produces an assessment that cannot be reproduced by the next assessor.

**EE-ZKP-004 (SHALL).** The wallet SHALL be **crypto-agile**: the ZKP scheme SHALL be a replaceable module behind a stable internal interface, with scheme identity carried in the presentation, so that a scheme change does not require re-issuing credentials or re-certifying the whole wallet.

### 10.4 Two tracks

| | **Track A — arithmetic circuits** | **Track B — multi-message signatures** |
|---|---|---|
| Scheme | `longfellow-libzk-v1` (Ligero IOP + sumcheck/GKR over ECDSA/SHA-256), no trusted setup | BBS / PS / KVAC family with a separate ECDSA proof of possession for device binding |
| EU spec | **TS13**, and normatively selected in the Commission's AV Profile | **TS14** |
| Works on already-issued mdoc/SD-JWT VC (ZKP_06) | **Yes** — this is its defining property | **No.** TS14 §2.4 declares mdoc and SD-JWT VC structurally incompatible with MMS ZKPs and puts ZKP_06 out of scope, proposing a new container based on IETF **JSON Web Proofs** reusing the SD-JWT VC data model |
| Predicate/range proofs, pseudonyms (ZKP_03, ZKP_04) | Not in the deployed circuit; the AV circuit proves the **boolean attribute is present and true**, not a range over `birth_date` | Yes — Bulletproofs or `Sharp` for ranges, Schnorr for equality, OR-proofs for set membership |
| Issuer hiding (ZKP_01(v)) | Not in the deployed circuit | Achievable |
| Device binding | Inside the circuit (all-in-ECDSA) | Separate proof; TS14 argues this is inherently lighter because the nonce is public and no hash need be proven in-circuit |
| Standardisation | `draft-google-cfrg-libzk-02` (2026-07-22), **individual draft, not CFRG-adopted**; at IETF 124 the adoption discussion favoured deferral until an IETF protocol requires it. Peer-reviewed: Frigo & Shelat, *Anonymous Credentials from ECDSA*, IACR CiC vol. 3 no. 1, 4 May 2026 | `draft-irtf-cfrg-bbs-signatures-10` (2026-01-08, RG document, expired 20 Jul 2026); blind signatures `-03`; per-verifier linkability `-03`; `draft-cllz-cfrg-ecdsa-pop-00` (2026-07-02) for the device-binding sub-proof |
| Audits | Trail of Bits (Aug 2025, under-constrained variable in the mdoc circuit); ISRG (Oct 2025, ISRG-01 CBOR index witness under-constrained, fixed in v0.8.4); academic panel (Dec 2025, two security theorems established) | Underlying signature schemes long-studied; the composed constructions less so |
| Maturity today | **Shipping** — Google Wallet since May 2025 (Bumble), Sparkasse partnership, `av-lib-ios-longfellow-zkp` in the EU AV codebase | **Not shipping** in any EU wallet |

*Source for the AV Profile selection.* The Commission's age-verification document is separate from the ARF and is not in the ARF checkout. Its Annex A §A.8 states: *"For this version of the profile, the only Zero-Knowledge Proof system in scope is the system identified by `longfellow-libzk-v1` ... An implementation SHALL use that system. Support for any other Zero-Knowledge Proof system does not constitute conformance with this profile."* (`ageverification.dev/av-doc-technical-specification/docs/annexes/annex-A/annex-A-av-profile/`, read 12 September 2026.) That is a selection **for the age-verification profile**; it is not the ecosystem-wide selection ARF §7.4.3.5.3 declines to make ("No specific ZKP has been selected to be supported by components in the EUDI Wallet ecosystem"). The two are consistent once the documents are distinguished.

**EE-ZKP-010 (SHALL).** The EE-EUDIW SHALL implement **Track A** as its initial ZKP path.

**EE-ZKP-011 (SHOULD).** The EE-EUDIW SHOULD implement **Track B** as a second scheme once ETSI TS 119 476-2 stabilises, and SHALL treat Track A as insufficient for ZKP_03, ZKP_04 and range predicates.

*Rationale.* Track A is the only route that produces unlinkable proofs over credentials Estonia can issue today with the certificates and secure elements that exist today, and it is the route the Commission, Google and the seven AV pilot Member States have taken. Track B is where the ARF's full requirement set can actually be satisfied. Committing to only one is the mistake.

### 10.5 Track A normative profile

**EE-ZKP-020 (SHALL).** The Track A scheme SHALL be `longfellow-libzk-v1` as defined in Frigo & Shelat (IACR CiC 3(1), 2026; ePrint 2024/2010) and specified in `draft-google-cfrg-libzk`, using the circuit and parameters identified by a `ZkSystemSpec` (`circuit_hash`, `version`, `num_attributes`, `block_enc_hash`, `block_enc_sig`) as defined in **ISO/IEC DIS 18013-5 (Second Edition) §10.2.7**.

**EE-ZKP-021 (SHALL).** The proof SHALL establish, without revealing the credential: (a) a valid issuer signature over the MSO; (b) presence of the requested attribute with value `true`; (c) the wallet's ability to sign the session nonce with the attestation's device key; (d) that the attestation is within its validity period.

**EE-ZKP-022 (SHALL).** The implementation SHALL be **`longfellow-zk` v0.9 (31 March 2026) or later**, or a bit-compatible implementation. v0.9 reduced peak prover memory from roughly 600 MB to under 200 MB, which is the difference between working and not working on mid-range Android hardware. Circuits **v7** (from v0.8.6, 13 January 2026) support MSOs up to 2551 bytes, roughly 50 attributes, at rate 7 / 132 columns for **109-bit soundness** — the figure is annotated `~109 bits statistical security` against `kLigeroNreqv7 = 132` in the implementation's own `mdoc_zk.h`, while `draft-google-cfrg-libzk-02` states that the same profiles "have been analyzed to provide at least 115 bits of security". The lower figure is used throughout this specification. Circuits v3 and v4 are deprecated (v0.8.5) and SHALL NOT be used.

*What 109 bits is, and what it is not.* The figure is the **soundness error** of the Ligero-style interactive oracle proof at those parameters: the work a cheating prover must do to make the verifier accept a false statement. It is not a key length, not a confidentiality margin, and not a property of the issuer signature — the ECDSA-P256 signature over the MSO and the SHA-256 hashing inside the circuit keep their own strengths regardless. Two consequences follow, and both are load-bearing:

1. **It is below the level ECCG ACM v2.0 sets for *recommended* mechanisms, which is "at least 125 bits" (§1.1) — though above the 100 bits §1.3 accepts for legacy ones.** The ACM sets no 128-bit floor; an earlier revision of this section said it did. `ZKP_08` requires the algorithms in the ZKP scheme to be ACM-agreed, and it is `ZKP_08` that the shortfall bears on — `ZKP_07` is the no-tracking rule, not an algorithm rule. This specification does not claim the gap away. 109 bits is a real shortfall against the recommended level, it is the largest parameter-level obstacle to Track A certification, and it is recorded as such in §21.1 and §23.
2. **It does not create a contradiction with AVA_VAN.5.** The vulnerability-analysis assurance requirement in CIR 2024/2981 Annex IV §2–§3 is imposed on the **WSCA and the WSCD** — the key store and its agent — and is conditional on the WSCD architecture; it is not imposed on the application-layer proof system, and the Regulation fixes no EAL for the wallet unit as a whole (recital 7 of CIR 2024/2981 is explanatory only). A wallet whose keys sit in an AVA_VAN.5-evaluated StrongBox and whose presentation-layer proof carries a 109-bit soundness error is internally consistent. It is merely, at the presentation layer, below the algorithm floor — which is a `ZKP_08` problem, not an AVA_VAN.5 one. Getting the provision right matters because the two are remedied differently: the algorithm floor is fixed upstream by re-parameterising the circuit; an AVA_VAN.5 finding would be fixed by changing the secure element.

**EE-ZKP-022a (SHALL).** Estonia SHALL NOT certify a Track A deployment as meeting `ZKP_08` on circuit parameters whose soundness error is below **125 bits**, and SHALL adopt ≥125-bit parameters as the accepted set as soon as an audited upstream release publishes them. Until then, a Track A deployment SHALL be operated as a pilot under §20 with the shortfall disclosed to the certification body, and SHALL NOT be advertised as meeting the ACM's recommended level. **[Whether the Longfellow maintainers have published, or intend to publish, a ≥125-bit parameter set — and at what prover cost — was not established in this session: UNVERIFIED.]**

*Two things an assessor will raise that are not defects, and should be answered before they are found.* The Fiat–Shamir ciphersuite `SHA256-AES256CTR` expands challenges with AES-256-CTR as a bare PRF, where ACM §7 lists only the SP 800-90A DRBG constructions (HMAC_DRBG, Hash_DRBG, CTR_DRBG); and the binary field GF(2^128) used by the hash and CBOR circuits will pattern-match against ACM §4.3's prime-field-only curve rule. Neither is a weakness — the field is an arithmetic setting carrying no hardness assumption, so it falls outside ACM §4.2/§4.3 rather than contrary to them, and the PRF construction is a ciphersuite choice, not a design constraint. Both are the kind of gap that is closed upstream by a parameter and ciphersuite selection rather than by new cryptography, which is why §10.3 treats an **ACM-conformance profile of an existing scheme** as the tractable work and designing a new scheme as the wrong lever. EE-ZKP-022b is where the answer is recorded, so that it is in the security target before an assessor asks for it rather than after.

**EE-ZKP-022b (SHALL).** The security target for a Track A deployment SHALL record, for each element of the scheme that has no ACM entry, whether that element carries a computational hardness assumption and, where it does not, the information-theoretic soundness bound relied on in its place. The record SHALL cover at least the `SHA256-AES256CTR` challenge expansion against ACM §7, the GF(2^128) arithmetic setting and the Reed–Solomon code evaluated over it against ACM §4.2 and §4.3, and the MAC linking the two circuit fields. This completes the assumption map EE-ZKP-003 requires under reading (a): that map runs from each primitive to its ACM entry, and an element with no entry is precisely the case it does not reach.

**EE-ZKP-022c (SHALL).** For every component of the Track A scheme the wallet controls — the Merkle and Fiat–Shamir hash, the challenge expander, and the soundness parameters governed by EE-ZKP-022a — the deployed instantiation SHALL use mechanisms at the *recommended* level of the applicable ACM, and SHALL NOT rely on a mechanism at the *admissible* level where a recommended one exists for the same function. The *admissible* status of ECDSA over P-256 and of SHA-256 **in the statement being proven** SHALL be documented as a backward-compatibility dependency on credentials already issued and WSCDs already certified, not as a design choice: the wallet does not select the issuer's signature algorithm or the secure element's key type, and SHALL NOT be assessed as though it did. A requirement written the other way would be unimplementable by any wallet, in Estonia or elsewhere.

Note the symmetry with §10.3, which applies the identical test to Track B: BBS on BLS12-381 also falls short of 128 bits, there because Cheon's attack on q-SDH erodes the curve's nominal level, with migration to BLS24-509 as the published remedy. Neither track currently clears the recommended level. That is a fact about the state of anonymous-credential cryptography in 2026, not a fact about either scheme's fitness relative to the other, and it is why §20 keeps Tier 1 independently certifiable rather than betting certification on either.

**EE-ZKP-023 (SHALL).** The wallet SHALL declare the negotiated `zkSystemId` in the presentation, and the relying party SHALL verify the `circuit_hash` against a published accepted-circuit set **before** verifying the proof, rejecting unknown circuits.

**EE-ZKP-024 (SHOULD).** Estonia SHOULD track the Rust reimplementation maintained by ISRG (`zk-cred-longfellow`) as a second, independently-written verifier, and SHOULD require the national verifier reference implementation to be validated against both. A single C++ implementation with three audits is still a single implementation.

**EE-ZKP-025 (SHALL).** A ZKP presentation SHALL NOT consume the attestation. Unlike a plain-mdoc presentation, a ZK proof does not reveal a linkable signature, so the same attestation MAY be reused within its validity period. This is the operational payoff of Track A and it SHALL be reflected in the batch-management logic: ZKP-capable devices need far smaller batches.

### 10.6 Circuit governance

The `circuit_hash` allowlist is a trust root. Whoever controls it controls what proofs verify, and a silent substitution is an undetectable soundness break.

**EE-ZKP-030 (SHALL).** RIA SHALL operate an **Estonian ZK Circuit Registry** publishing, for each accepted `ZkSystemSpec`: the `circuit_hash`, the `version`, `num_attributes`, `block_enc_hash`, `block_enc_sig`, the upstream release tag and commit, the audit reports covering it, the **claimed soundness error in bits and the source of that claim**, the acceptance date, and the deprecation date. The soundness level is a published, per-circuit property, not a footnote: a relying party with a statutory security floor of its own needs to be able to read it and refuse.

**EE-ZKP-031 (SHALL).** The registry SHALL be signed, machine-readable, versioned, and append-only, and SHALL be logged to a Certificate Transparency-style log per ARF Topic 55.

**EE-ZKP-032 (SHALL).** Relying parties SHALL cache the registry and SHALL NOT fetch it per presentation. A per-presentation fetch is a phone-home and violates `ZKP_07`.

**EE-ZKP-033 (SHALL).** A circuit SHALL be deprecated in the registry no later than 30 days after a soundness-affecting finding is published against it, and the registry SHALL record the finding.

### 10.7 Performance budget

Published figures, with sources, for the two tracks:

| Scheme | Prove | Verify | Proof size | Platform / source |
|---|---|---|---|---|
| Longfellow, full mdoc `age_over_18` | 1.2 s | 0.6 s | ~400 KB | Pixel 6 Pro, TS4 §2.2.1.4 citing Frigo & Shelat |
| Longfellow, same | ~800 ms | ~0.6 s | ~400 KB | Pixel 6 Pro, TS13 §3.3 |
| Longfellow, independent bench (no OpenSSL hw accel) | 816 ms | 492 ms | — | dyne.org |
| Longfellow, ECDSA sub-proof only | 60 ms | — | — | Frigo & Shelat |
| BBS+ | <2 ms | <2 ms | 272 + 32·i bytes | TS4 §2.1.1.3 |
| BBS# (Orange, pairing-free KVAC) | <2.5 ms; ~70 ms on Android StrongBox | — | 416 + 32·U bytes | TS4 §2.1.2.3; ePrint 2025/619 |
| ECDSA device binding, circuit (ePrint 2026/965) | <~400 ms | — | ~1.5 kB | "legacy phones" |
| ECDSA device binding, circuit-free | ~400 ms | — | ~122 kB | ibid. |
| Crescent (Groth16, trusted setup) | 29.2 ms online / 315 ms device-bound | 11.7 / 184 ms | 1019 bytes | Xeon W-2133; offline prove 19 s; **setup 580 MB** |
| Vega (Spartan+Nova+NeutronNova, no trusted setup) | 92 ms | 23 ms | 108 KB | Microsoft Research blog, 21 May 2026; prover key 464 KB. **Not in any EU spec; no independent audit found — UNVERIFIED** |
| Bulletproofs / `Sharp` range proof | ~10 ms | ~10 ms | <1 KB | laptop, <32-bit range, TS14 §5.2 |

**EE-ZKP-040 (SHALL).** On the reference device class, a ZKP age presentation SHALL complete proof generation in **≤ 2.0 s** at p95 and **≤ 3.5 s** at p99, measured from user approval to proof ready, and the verifier SHALL complete verification in **≤ 1.0 s** at p95. `ZKP_05` requires that delays not defeat the purpose of the relying party's service; a supermarket checkout is the binding constraint, not a website.

**EE-ZKP-041 (SHALL).** Peak prover memory SHALL NOT exceed **250 MB**.

**EE-ZKP-042 (SHALL).** Where the budget cannot be met on a given device, the wallet SHALL fall back to the Tier 1 path per §10.8 and SHALL tell the user, in plain language, that this presentation is linkable by the issuer. Silent degradation of a privacy property is prohibited.

**EE-ZKP-044 (SHALL).** The budget in EE-ZKP-040 covers proof generation only. A separate **transport budget** SHALL be met end to end: from user approval to the relying party's accept/reject decision, **≤ 4.0 s** at p95 over the proximity transport of §11.3, inclusive of proof generation, transfer and verification. This budget SHALL be met by measurement on the reference device class against a reference relying-party terminal, and the measurement SHALL be a precondition of the EE-AGE-2 and EE-PROX profiles in §20.

*Why this is separate, and why it may well be the binding constraint.* A Longfellow proof is roughly **400 KB**. ISO/IEC 18013-5 device retrieval over BLE moves it through GATT notifications sized to the negotiated MTU, and on the handset and terminal hardware actually deployed in retail the effective application-layer throughput is a small fraction of the nominal PHY rate. At 100 kB/s a 400 KB proof takes four seconds of transfer *after* a one-to-two-second proof generation; at 20 kB/s it takes twenty. The published Longfellow timings in the table above are prove-and-verify figures measured on a Pixel 6 Pro; **none of them includes proximity transport**, and no figure in this specification should be read as evidence that the combined path fits a checkout queue. **[No measurement of ISO 18013-5 BLE device-retrieval throughput for a payload of this size was performed or located in this session — UNVERIFIED. The four-second budget is a requirement derived from the `ZKP_05` usability constraint, not a claim that current hardware meets it.]**

**EE-ZKP-045 (SHALL).** Where the EE-ZKP-044 budget is not met on a device-and-terminal pair, the wallet SHALL fall back to Tier 1 under EE-ZKP-042, with the same honest labelling. A proximity ZKP presentation SHALL NOT be attempted where the wallet has previously measured the pair as exceeding the budget. NFC and QR-initiated Wi-Fi Aware device retrieval, where both ends support them, SHOULD be preferred over BLE for Tier 2 presentations precisely because of this payload size.

**EE-ZKP-046 (SHOULD).** Estonia SHOULD treat proof size, not proof time, as the primary selection criterion when Track B matures. The Track B candidates in the table above are two to three orders of magnitude smaller, which removes this constraint rather than budgeting around it.

**EE-ZKP-043 (SHOULD).** The reference device class SHOULD be defined as the median Estonian smartphone by installed base at the time of certification, not by flagship hardware. **[The Estonian installed-base distribution was not researched in this session — UNVERIFIED.]** Published platform baselines for the EU AV solution are iOS 27+ and Android 10+, described as indicative.

### 10.8 Downgrade resistance

The Commission's AV Profile states as an architectural assumption that *"the APs and RPs cannot pre-discover Age Verification App's capability"*, requires a relying party to **be able to verify both** a Zero-Knowledge Proof presentation and the plain ISO mDoc fallback presentation (Annex A §A.8), and forbids a relying party from rejecting a presentation *solely* on the ground that it uses the fallback (Annex A §A.9). The fallback is available where, and only where, the device cannot generate a Zero-Knowledge Proof or the transport is OpenID for Verifiable Presentations (Annex A §A.6). The practical consequence — that a relying party cannot distinguish a genuinely incapable device from a deliberate downgrade, and so cannot refuse the fallback — is an inference from those two requirements, not a quoted rule. It is nonetheless the operative constraint, and it is a real weakness.

**EE-ZKP-050 (SHALL).** The relying party SHALL accept a plain-mdoc EE-PoA presentation conforming to the data model, consistent with the AV Profile's acceptance requirement.

**EE-ZKP-051 (SHALL).** The **wallet** SHALL NOT offer the plain-mdoc path where the device is capable of the ZKP path and the relying party advertises ZKP support. Capability negotiation SHALL be resolved wallet-side, where the device's true capability is known, not relying-party-side.

**EE-ZKP-052 (SHALL).** Relying parties SHALL advertise ZKP support in their request. Where an Estonian relying party is registered for an age-check-only intended use, the Registrar SHOULD require ZKP support as a condition of registration from a date set by RIA, with a published transition period.

**EE-ZKP-053 (SHALL).** The wallet's transaction log SHALL record, per presentation, which tier was used, and the dashboard SHALL surface the count of linkable versus unlinkable presentations. Users cannot exercise Art. 5a(4)(d) transparency rights over a property they cannot see.

*The residual trapdoor, and how it is closed.* EE-ZKP-051 conditions the wallet's refusal on the relying party *advertising* ZKP support. A relying party that wants a linkable presentation therefore has a trivial move available: say nothing. It is under no obligation to advertise, EE-ZKP-050 obliges it to accept the fallback, and the wallet — taking the request at face value — offers Tier 1. The privacy guarantee then rests on the good faith of the party with the strongest incentive to defect, which is not a guarantee. The self-declared capability signal cannot be the control; an attested one has to be.

**EE-ZKP-054 (SHALL).** ZKP verification capability SHALL be an **attested attribute of the relying party's registration**, recorded by the Registrar under TS5/TS6 and carried in the registration certificate, not a self-declaration in the request. The wallet SHALL read capability from the registration certificate and SHALL disregard its absence from the request.

**EE-ZKP-055 (SHALL).** Where the relying party's registration certificate asserts ZKP capability and the device is ZKP-capable, the wallet SHALL present over Tier 2 and SHALL NOT offer the Tier 1 path, irrespective of what the request advertises. A request from such a relying party that is structured to admit only a plain-mdoc response SHALL be refused, and the refusal SHALL be logged and SHALL be reportable to the Registrar from the wallet in one action.

**EE-ZKP-056 (SHALL).** From the date set under EE-ZKP-052, an Estonian relying party registered for an age-check-only intended use whose registration does not assert ZKP capability SHALL have that registration refused or withdrawn. A relying party cannot hold a registration premised on data minimisation while declining the mechanism that delivers it.

*Limits, stated honestly.* This closes the trapdoor for **Estonian-registered** relying parties, because Estonia controls their registration. It does not close it for relying parties registered in another Member State, whose certificates Estonia must accept as issued and whose capability Estonia cannot compel. Against a foreign relying party the residual exposure remains, and the wallet's only remedy is the honest label of EE-ZKP-042 and the counter of EE-ZKP-053. Nor does any of this help where the device is genuinely incapable: EE-ZKP-050 stands, and it must, or the fallback ceases to be a fallback.

### 10.9 Post-quantum posture

The privacy of BBS-family anonymous credentials is **unconditional**, meaning a future quantum adversary could forge but not retroactively de-anonymise — everlasting privacy. Longfellow's soundness rests on SHA-256 as its stated complexity assumption and is plausibly post-quantum sound, but the *credentials* it proves statements about are ECDSA-signed and inherit ECDSA's quantum vulnerability at the issuance layer. No post-quantum anonymous credential scheme is yet mature enough for wide deployment; the cryptographers' letter names lattice candidates and the LaZer library as proofs of concept only.

The EU PQC roadmap targets high-risk systems by 2030 and full migration by 2035. ECCG ACM v2.0 approved ML-DSA, ML-KEM, FrodoKEM, SLH-DSA, XMSS and LMS, with a hybrid caveat for (M)LWE. The v3.0 working draft goes further and demotes **every** classical asymmetric mechanism — P-256 and ECDSA among them — to *admissible*, keeping only the post-quantum mechanisms, AES-192/256 and 384/512-bit hashes including SHAKE256 at *recommended*. That direction of travel is the strongest available argument for EE-ZKP-061's preference for a proof system whose soundness rests on hashes: a pairing-based scheme inherits the demotion, a hash-based one does not.

**EE-ZKP-060 (SHALL).** The wallet's cryptographic inventory SHALL identify, per algorithm, its quantum exposure and its migration path, and SHALL distinguish **soundness** exposure (forgery) from **privacy** exposure (de-anonymisation), because they have different deadlines and different remedies.

**EE-ZKP-061 (SHOULD).** Estonia SHOULD prefer, at equal maturity, a ZKP scheme whose soundness rests only on hash functions, because it is upgradeable by swapping the issuer signature inside the circuit without touching the proof system.


---

## 11. Protocols

### 11.1 Baseline

| Layer | Specification | Status verified 5 Sep 2026 |
|---|---|---|
| Issuance | **OpenID4VCI 1.0** (`-17`, 16 Sep 2025) | **Final** |
| Presentation | **OpenID4VP 1.0** (`-30`, July 2025; OIDF announcement 10 Jul 2025) | **Final** |
| Profile | **OpenID4VC HAIP 1.0** (`-07`, 24 Dec 2025; membership-approved 29 Dec 2025) | **Final** |
| Selective disclosure | **RFC 9901** SD-JWT (Nov 2025) | **Proposed Standard** |
| Credential type | `draft-ietf-oauth-sd-jwt-vc-18` (rev. 2026-08-03, posted 2026-08-10) | IESG state **"AD Evaluation::Revised I-D Needed"** — no Last Call issued; **not yet an RFC** |
| Status | `draft-ietf-oauth-status-list-21` (21 Jun 2026) | **RFC Editor queue**, no RFC number yet |
| Client auth | `draft-ietf-oauth-attestation-based-client-auth-10` (6 Jul 2026) | "I-D Exists" — **barely moving** |
| mdoc | ISO/IEC 18013-5:2021; **2nd edition at DIS stage 40.60**, ballot closed 27 Mar 2026, publication expected Q4 2026 | **Unpublished** — and it is the edition that adds ASL/ARL revocation and the `ZkRequest`/`ZkSystemSpec`/`ZkDocument` structures |
| mdoc online | ISO/IEC TS 18013-7:2025 (2nd ed., May 2025); Annex B profiles **OpenID4VP draft 18, not 1.0**; Annex C is the DC API binding | Published, "to be revised" |
| Generic eID | ISO/IEC TS 23220-2:2026, 23220-3:2026, **23220-4:2026 (10 Apr 2026)**, 23220-6:2025 | Published |
| Browser API | **W3C Digital Credentials API**, Working Draft 4 Sep 2026 | **Not yet CR** |
| Cross-device | **RFC 10027 / BCP 247**, Cross-Device Flows Security BCP, Aug 2026 | Published |

**EE-PRO-001 (SHALL).** The EE-EUDIW SHALL conform to **HAIP 1.0**. This entails, at minimum: ES256 mandatory to support; SHA-256 for digests in both formats; authorization code flow with PKCE `S256`, DPoP (RFC 9449) and authorization response issuer identification (RFC 9207) per FAPI 2.0; wallet attestation at the OAuth endpoints; key attestation; **DCQL mandatory**; **response encryption mandatory** (ECDH-ES on P-256 with A128GCM/A256GCM); trusted-authority queries by Authority Key Identifier; JAR with `request_uri` for redirect flows; `direct_post.jwt` for redirect-based presentation; and Digital Credentials API support for applicable flows.

**EE-PRO-002 (SHALL).** Presentation Exchange SHALL NOT be used. It was removed from OpenID4VP in April 2025 before Final; `presentation_definition`, `presentation_definition_uri` and `presentation_submission` do not exist in 1.0.

**EE-PRO-003 (SHALL).** Relying party authentication SHALL use the `x509_san_dns` client identifier prefix for redirect flows and `x509_hash` for signed Digital Credentials API requests, as HAIP 1.0 §5 requires, in both cases with the access certificate from the Estonian ACA. `verifier_attestation` and `openid_federation` prefixes SHALL NOT be used in the Estonian ecosystem, which follows the ARF's X.509-plus-trusted-lists model rather than OpenID Federation.

**EE-PRO-004 (SHALL).** Unsigned request objects SHALL NOT be accepted except where OpenID4VP 1.0 permits them with the `redirect_uri` prefix, which SHALL NOT be used by Estonian relying parties.

**EE-PRO-013 (SHALL).** A signed Digital Credentials API request SHALL carry `expected_origins`, and the wallet SHALL refuse the request, before any proving work, where the platform-asserted origin is not among them (OpenID4VP 1.0 Annex A.2). The wallet SHALL NOT rely on the browser-asserted origin alone: W3C DC API §10.4 states that the origin the browser supplies is not on its own sufficient to authenticate the requester.

### 11.2 The 18013-7 Annex B version gap

ISO/IEC TS 18013-7:2025 Annex B profiles **OpenID4VP draft 18**, not the Final 1.0. Annex D (OpenID4VP over the DC API) is targeted for the third edition, unpublished.

**EE-PRO-005 (SHALL).** Where the two conflict, Estonian implementations SHALL follow **HAIP 1.0 over OpenID4VP 1.0**, and SHALL document the deviation from 18013-7 Annex B in the conformance statement. This is a knowing, recorded divergence, not an oversight.

**EE-PRO-006 (SHOULD).** Implementations SHOULD additionally support the ISO/IEC TS 23220-4:2026 profile, which ARF §5.4.2 indicates may replace references to 18013-5 as it becomes available, and which the Commission's own AV verifier backend already targets.

### 11.3 Cross-device flows

RFC 10027 / BCP 247 names **cross-device consent phishing** and **cross-device session phishing**, describes nine attack scenarios and eighteen mitigations, and finds that proximity-enforced flows resist consent phishing better than proximity-less ones.

**EE-PRO-010 (SHALL).** Same-device presentation over the W3C Digital Credentials API SHALL be the preferred remote flow. QR-based cross-device flows SHALL implement the applicable BCP 247 mitigations and SHALL display, before approval, the relying party's trade name from the access certificate together with an explicit same-device/cross-device indication.

*Two things are called "scanning a QR code with the wallet", and only one of them has a proximity check.* In the **Digital Credentials API path**, the QR is generated by the *browser* on the requesting device, encodes a FIDO CTAP hybrid-transport bootstrap (`FIDO:/…`, CTAP 2.2 §11.5), is read by the phone's *system camera*, and brings the wallet in afterwards through the platform credential manager: the wallet never sees the QR, the Bluetooth advertisement or the tunnel, and proximity is established by the transport. In the **redirect path**, the wallet's own scanner reads an `openid4vp://` deep link or a `request_uri`, and nothing establishes that the two devices are in the same room. The ARF and the implementing acts steer to the first and discourage the second for cross-device use, and this specification follows them.

**EE-PRO-010a (SHALL).** Cross-device presentation SHALL use the W3C Digital Credentials API, where the QR is generated by the browser and proximity is established by the CTAP hybrid transport, with one narrow exception: a redirect-based cross-device flow reading a wallet-scanned `openid4vp://` QR MAY be offered only where it meets every condition in this paragraph, and SHALL be prohibited otherwise. It SHALL NOT be offered as a silent fallback (ARF `OIA_08c`; ETSI TS 119 472-2 `OIDFVP-HAIP-SUPPORT-03`, applicable through CIR 2026/1731 Annex XII); it SHALL instead be selected explicitly by the user, SHALL be labelled as lacking a proximity check, and SHALL carry the BCP 247 §6.1 mitigations including an out-of-band code the user matches across the two devices. A redirect-based flow that does not meet all three conditions SHALL NOT be offered at all.

**EE-PRO-011 (SHALL).** Proximity presentation SHALL follow ISO/IEC 18013-5 **device retrieval**, and SHALL NOT use server retrieval. BLE is the mandatory-to-implement device-retrieval transport; where both ends support NFC or Wi-Fi Aware device retrieval, the wallet SHOULD prefer them for Tier 2 presentations, whose payload is roughly a thousand times a plain mdoc response. The end-to-end budget of EE-ZKP-044 applies to whichever transport is selected, and a transport that cannot meet it SHALL NOT be used for a Tier 2 presentation.

### 11.4 Transaction data and QES

**EE-PRO-012 (SHALL).** Authorisation of a qualified electronic signature from within the wallet SHALL use the OpenID4VP `transaction_data` mechanism (an array of base64url-encoded JSON objects each carrying `type` and `credential_ids`), binding the presentation to the specific document being signed — the "what am I signing" property.

### 11.5 Issuance specifics

**EE-PRO-020 (SHALL).** Batch issuance SHALL use the `proofs` (plural) parameter of OpenID4VCI 1.0, with an `attestation` proof type carrying a Wallet-Provider-signed key attestation over the whole key set, rather than one proof per key.

**EE-PRO-021 (SHALL).** The wallet SHALL call the Notification Endpoint with `credential_accepted`, `credential_failure` or `credential_deleted` as applicable, and the issuer SHALL NOT infer *usage* from notifications. Notification is an issuance-lifecycle signal, not a presentation signal; treating it otherwise breaches Art. 5a(5)(b).

**EE-PRO-022 (SHALL).** Credential requests and responses SHALL be encrypted per OpenID4VCI §10.

---

## 12. Wallet Unit Attestation

Per **TS3 v1.5.2 (26 May 2026)**, the Wallet Unit Attestation comprises a **Wallet Instance Attestation (WIA)** and a **Key Attestation (KA)**, both JWTs signed by the Wallet Provider. "Wallet Trust Evidence (WTE)" is legacy terminology and SHALL NOT be used. ARF 2.1.0 removed all references to WUA on the wallet-to-relying-party presentation interface: **the WUA is an issuance-side artefact.**

**EE-WUA-001 (SHALL).** The WIA SHALL attest wallet identifiers, version, certification details and a `client_status` object; technical validity SHALL be under 24 hours, with a revocation maintenance period aligned to PID validity.

**EE-WUA-002 (SHALL).** The KA SHALL contain information about the certification of the WSCA/WSCD or keystore (`WUA_01`), and during PID issuance the wallet SHALL provide the PID Provider with a valid KA describing the WSCD that generated the PID private key (`WUA_05`).

**EE-WUA-003 (SHALL).** Only cryptographic algorithms in ECCG ACM v2.0 SHALL be used when issuing, presenting or verifying a WIA or KA (`WUA_04`). Note the contrast with §10.3: the WUA layer *can* satisfy this; the ZKP layer currently cannot.

**EE-WUA-004 (SHALL NOT).** A WUA SHALL NOT be presented to a relying party.

**EE-WUA-005 (SHALL).** WUA revocation SHALL use IETF Token Status List; user-requested WSCD/keystore revocation SHALL be supported (mandatory under the per-KA index option since TS3 v1.5.2).

**EE-WUA-006 (SHALL).** Attestation Providers SHALL check KA revocation at issuance.

---

## 13. Revocation and status

**EE-REV-001 (SHALL).** For SD-JWT VC, revocation SHALL use the Attestation Status List mechanism specified by IETF Token Status List (ARF `VCR_11a`). For `mdoc`, Estonia SHALL implement the **Attestation Status List or Attestation Revocation List** mechanism that ARF `VCR_11` requires per Annex 2 of the amended CIR 2024/2979 — already in force, its text copied from the forthcoming 2nd edition of ISO/IEC 18013-5 — on the Token Status List data structure until that edition publishes and the ASL/ARL wire formats are fixed. The interim choice diverges from `VCR_01`'s closed list of three `mdoc` methods and is recorded as §22 item 12.

**EE-REV-002 (SHALL).** Status list fetches SHALL be batched, cached, and decoupled in time from presentations, so that a status fetch does not reveal a presentation event.

**EE-REV-003 (SHALL NOT).** EE-PoA SHALL NOT use any status mechanism (§9.6).

**EE-REV-004 (SHALL).** PID and attestation deletion SHALL follow ARF Topic 51 (`PAD_*`), and wallet unit revocation ARF Topic 38.

**EE-REV-005 (SHALL).** Revocation of a qualified attestation SHALL be irreversible (Art. 45d(4), 45f(4)).

---

## 14. Pseudonyms, consent, and disclosure policies

### 14.1 Pseudonyms are a mandatory core function, not an optional feature

This is worth stating plainly because pseudonyms are routinely treated in programme documentation as a nice-to-have to be deferred past the first release. They are not. **CIR (EU) 2024/2979 Art. 14** is mandatory in both paragraphs:

> **Article 14 — Pseudonyms.** 1. Wallet units shall support the generation of pseudonyms for wallet users in compliance with the technical specifications set out in Annex V. 2. Wallet units shall support the generation, upon the request of a wallet-relying party, of a pseudonym which is specific and unique to that wallet-relying party and provide this pseudonym to the wallet-relying party, either standalone or in combination with any person identification data or electronic attribute attestation requested by that wallet-relying party.

Read the obligation precisely. It is to **support the generation of** pseudonyms — including on demand from a relying party, and including in combination with a PID or attestation presentation in the same transaction. Three separate provisions reinforce it: **Art. 5(1)** (use of user-chosen pseudonyms shall not be prohibited), **Art. 5a(4)(b)** (the wallet shall enable the user to generate pseudonyms and store them encrypted and locally), and **Art. 5b(9)** (relying parties shall not refuse the use of pseudonyms where identification is not required by law, restated here as EE-RP-006). A wallet that ships without pseudonyms is not a wallet with a missing feature; it fails a core-functionality implementing act.

**The technical specification is WebAuthn Level 2, pinned by dated URL.** CIR 2024/2979 **Annex V** ("Technical specifications for pseudonym generation referred to in Article 14") contains exactly one entry:

> WebAuthn – W3C Recommendation, 8 April 2021, Level 2, `https://www.w3.org/TR/2021/REC-webauthn-2-20210408/`

**EE-PRI-017 (SHALL).** Pseudonym generation SHALL conform to the Annex V reference — WebAuthn Level 2 as published 8 April 2021 at that dated URL — and conformance SHALL be asserted against **that text**, not against a later WebAuthn revision. **Web Authentication Level 3 became a W3C Recommendation on 25 August 2026**; because Annex V cites a dated snapshot, Level 3 is not the normative reference until the implementing act is amended. Where the wallet implements Level 3 behaviour, the security target SHALL record the delta.

**Two WebAuthn defaults are wrong for a wallet, and the regulation does not say so.** Annex V incorporates WebAuthn without profiling it, and WebAuthn's own defaults are tuned for consumer authenticators, not for a device certified at assurance level high:

- **`attestationConveyancePreference: "none"`** is WebAuthn's *default* value. Under it the relying party is "not interested in authenticator attestation" and receives an attestation statement in the `none` format. Applied literally, a wallet pseudonym would carry no evidence that the key sits in a WSCD.
- **Self attestation** (also called surrogate basic attestation) is the case where "the Authenticator does not have any specific attestation key pair. Instead it uses the credential private key to create the attestation signature" — detectable client-side by an all-zero AAGUID with format `packed` and no `x5c`. It is self-referential and proves nothing about the platform.

Neither is compatible with a wallet whose whole value proposition is that its keys are held in a component evaluated per §17.1.1. The relying party's assurance about the wallet comes from the **Wallet Unit Attestation** (§12) and from the relying party's own ability to verify wallet authenticity and validity under **Art. 5a(5)(a)(viii)**, not from a WebAuthn attestation statement — but the pseudonym key must still be bound to the same WSCD, or the pseudonym is a bare browser credential wearing a wallet's name.

**EE-PRI-018 (SHALL NOT).** A pseudonym key pair SHALL NOT be created with self attestation, and SHALL NOT be created outside the WSCD. The wallet SHALL NOT rely on the WebAuthn `none` default to discharge Art. 14.

**EE-PRI-018a (SHALL).** The binding of the pseudonym key to the WSCD SHALL be evidenced **to the Wallet Provider**, at key creation, through the key attestation chain of §12, in the same manner as attestation keys. It SHALL NOT be evidenced to the relying party by presenting a Wallet Unit Attestation: **EE-WUA-004 prohibits that**, and the WUA is an issuance-side artefact (§12). The relying party's assurance that it is dealing with a genuine, valid wallet unit comes from its verification of wallet authenticity under **Art. 5a(5)(a)(viii)** and from the registration and access certificate infrastructure of §5, not from the WebAuthn attestation object.

**EE-PRI-018b (SHALL).** The wallet SHALL remain interoperable with a relying party that performs an unmodified WebAuthn ceremony. Where such a relying party sends `attestationConveyancePreference: "none"`, the wallet SHALL complete the ceremony and return a `none`-format attestation statement as WebAuthn requires; the prohibition in EE-PRI-018 governs **how the key is created inside the wallet**, not what the wallet is permitted to convey in response to a conformant request. A wallet that refused such a ceremony would be non-conformant to the Annex V reference and would break every relying party that integrated against it.

*Why the distinction carries weight.* Reading EE-PRI-018 as a rule about conveyance would make it both unimplementable and self-contradictory — it would require the wallet to push attestation at relying parties that did not ask for it, in a ceremony whose defaults forbid that, while §12 simultaneously forbids showing them a WUA. Read as a rule about key creation, it says the one thing that actually matters and is actually enforceable: a pseudonym key is a WSCD key, provably so to the party that certifies the wallet unit. The relying party's trust path runs through wallet authenticity, which is where the Regulation put it.

**EE-PRI-019 (SHALL).** Where a relying party requests a pseudonym together with a PID or attestation presentation in a single transaction (the Art. 14(2) combination case), the wallet SHALL present the combination as a single consent event naming both and SHALL obtain the user's freely given, specific, informed and unambiguous affirmative opt-in to release the pseudonym. Passive continuation and approval of the PID or attestation disclosure alone SHALL NOT constitute consent to pseudonym release. The transaction log SHALL record that a pseudonym was released, the version of the consent notice displayed, and the affirmative user action.

> **Scope note on unlinkability.** CIR 2024/2979 requires a pseudonym "specific and unique to that wallet-relying party" (Art. 14(2)); it does **not** use the word unlinkable and imposes no cross-relying-party unlinkability requirement. Recital 14's supporting language ("should enable wallet users to authenticate themselves without providing wallet-relying parties with unnecessary information") is a *should* in a recital. The unlinkability property required by EE-PRI-010 below is therefore **this specification's requirement**, grounded in Art. 5a(16)(b) and Art. 5a(4)(b), not a restatement of CIR 2024/2979. It is stated here so a conformity assessor does not go looking for it in the implementing act.

### 14.2 Requirements

**EE-PRI-010 (SHALL).** The wallet SHALL support pseudonym generation per ARF Topic 11 (`PA_01`–`PA_31`) and CIR 2024/2979 Art. 14, stored encrypted and locally (Art. 5a(4)(b)), with per-relying-party derivation such that pseudonyms are constant per (user, relying party) pair and — as a requirement of this specification, per the scope note above — unlinkable across relying parties.

**EE-PRI-011 (SHOULD).** Pseudonym derivation SHOULD follow the BBS per-verifier-linkability construction (`draft-irtf-cfrg-bbs-per-verifier-linkability-03`) where Track B is implemented — a G1 point derived from a `nym_secret` and a `context_id`, with signer-contributed entropy blindly added at issuance — and SHOULD otherwise use a WSCD-held key derived per relying-party identifier.

**EE-PRI-012 (SHALL).** Attributes SHALL be presented only after user approval (`OIA_06`), with selective disclosure supported (`OIA_07`), and approval SHALL be obtained before any presentation including wallet-to-wallet (`RPA_07`).

**EE-PRI-013 (SHALL).** Embedded disclosure policies SHALL be implemented per ARF Topic 43 (`EDP_01`–`EDP_11`), and the wallet SHALL inform the user where a relying party has policy-based permission to access an attestation (Art. 5a(5)(e)).

**EE-PRI-014 (SHALL).** The wallet SHALL provide the dashboard required by Art. 5a(4)(d): the transaction log; the list of relying parties and data exchanged; a one-step GDPR Art. 17 erasure request per **TS7 v1.0** and ARF Topic 48; and a one-step report of a relying party to the DPA per **TS8 v1.0** and ARF Topic 50. The DPA endpoint SHALL be Andmekaitse Inspektsioon.

**EE-PRI-015 (SHALL).** The transaction log SHALL be stored **locally** and SHALL NOT be replicated to the Wallet Provider, the Operator, or any issuer. Art. 5a(14) forbids collecting information about wallet use that is not necessary for provision.

**EE-PRI-016 (SHALL).** Data portability and export SHALL follow **TS10 v1.2**, including the Migration Object format, and migration to a different wallet solution SHALL follow ARF Topic 34 (`Mig_*`).

---

## 15. Signing with the wallet

**EE-SIG-001 (SHALL).** The wallet SHALL support creation of qualified electronic signatures using a QSCD, **free of charge for all natural persons** (Art. 5a(5)(g), 5a(13)), per ARF Topic 16 (`QES_01`–`QES_26`) and Annexes 4.06–4.12.

**EE-SIG-002 (SHALL).** Remote signing SHALL use the CSC API and ETSI TS 119 432 profiles, with the signed-document binding carried in OpenID4VP `transaction_data`.

**EE-SIG-003 (SHALL).** Signature containers SHALL be **ASiC-E (`.asice`)** with XAdES, at level **LT** minimum and **LTA** where long-term archival is required. DigiDoc4j 6.0.0 introduced LTA support permitting repeated re-timestamping; the current release line is 6.1.0.

**EE-SIG-004 (SHALL).** The wallet SHALL NOT create new DDOC containers and SHOULD support reading BDOC and DDOC for backward compatibility only.

**EE-SIG-005 (SHOULD).** Validation SHOULD be delegated to **SiVa**, the Estonian signature validation service, for formats not natively supported. Note SiVa moved behind Cloudflare by end of June 2026, breaking IP allowlists; integrations SHALL NOT depend on source-IP allowlisting.

**EE-SIG-006 (SHOULD).** Where the wallet offers encryption, it SHOULD use **CDOC2**, which becomes the DigiDoc default in autumn 2026, and SHOULD NOT introduce new CDOC1 usage.

**EE-SIG-007 (SHALL).** Advanced signature and seal formats recognised by public bodies SHALL follow **CIR (EU) 2026/248** (2 February 2026), which repealed Implementing Decision (EU) 2015/1506.

### 15.1 Mass signing: sole control, intent, and what may be batched

Estonian practice already contains high-volume signing — bulk issuance of certificates and decisions, batch invoicing, document-management systems that sign hundreds of files on one authentication. Moving that onto a wallet-held QSCD raises a question this specification previously did not answer: **how many documents may one user authentication authorise?** The answer is not "as many as the UI allows", and it is not "one" either. It sits in three separate instruments that must be read together.

**(1) Sole control is a signature requirement, at the level of the signature.** Art. 26(c) requires that an advanced electronic signature "is created using electronic signature creation data that the signatory can, with a high level of confidence, use under his sole control". The comparison with **Art. 36(c)** for seals is instructive: there the wording is "under its control", with *sole* absent. Sole control is what a batch mechanism erodes, and it erodes silently, because nothing in a signed file records how many other files were authorised by the same gesture.

**(2) Intent is a separate requirement from sole control, and it is the one batching actually breaks.** **ETSI TS 119 101 V1.1.1 (2016-03) clause 8.1.6 "Signature invocation requirements"** — control objective: "Ensure that each signature generated is the result of an explicit signature invocation" — sets out:

- **SCP 49:** "The user interface shall limit accidental invocation of the signature process by the signer."
- **SCP 50:** "The SCA shall ensure that the signature is applied with the intent of the signer."
- **SCP 51:** "If the expression of will is a goal of the signature, prior to initiation of the signature process, the user interface shall request the signer to perform a non-trivial signature invocation interaction with the SCA that is unlikely to occur accidentally." *(Example given: scrolling to the end of the document before accepting, "and not just selecting 'next'".)*
- **SCP 52:** "The user interface shall convey clear information that a signature is going to be created."

SCP 51 is the binding one for batches, and note its condition: it applies **where expression of will is a goal of the signature**. That condition is what distinguishes a consent-bearing signature from a bulk authenticity operation, and it is the hinge of the whole design.

**(3) A single authorisation may cover many documents, but only under a precise condition.** **ETSI TS 119 431-1 V1.3.1 (2024-12) clause 6.3.1, requirement SIG-6.3.1-13 [CONDITIONAL]:**

> "In case the authentication is linked directly to the identity, the signature session shall be linked: 1) either exactly to one SAD which contains the references of all documents that shall be signed within this session; 2) or to multiple SAD values if and only if multiple consecutive signatures are applied to the same document, where each signature covers the previous ones."

So an open-ended authorisation is excluded. The permitted batch is a **closed, enumerated set of document references fixed at activation time and carried inside a single Signature Activation Data value.** The signer must be authorising *these* documents, not *a session*.

**Signature Activation Data.** The protection profile **EN 419 241-2** requires (requirement `R.SAD`) that the SAD combine the signer's strong authentication as specified in EN 419 241-1, a unique reference to the signing key where a particular key is not implied, and a given DTBS/R, protected in integrity and confidentiality — i.e. a three-way binding of *who*, *which key*, and *what data*. **[SOURCE NOTE]** EN 419 241-1 and -2 are CEN deliverables sold by national standards bodies and were not directly readable; this requirement text is quoted from a Common Criteria Security Target written against that Protection Profile, which reproduces `R.SAD` verbatim. **SCAL2** ("Sole Control Assurance Level 2") is defined in **EN 419 241-1** and referenced as such in ETSI TS 119 432 V1.3.1's abbreviation list; the substantive definition — two-factor authentication, enforced from within the HSM rather than by the signing application — is taken here from ENISA's *Assessment of Standards related to eIDAS* (November 2018) and is **[SECONDARY]**. Anyone drafting a security target must buy the normative text.

**EE-SIG-010 (SHALL).** Where the wallet authorises more than one signature from a single user authentication, the signature session SHALL be bound to **exactly one SAD enumerating the references of every document in the session**, per ETSI TS 119 431-1 SIG-6.3.1-13(1). An authorisation that is open-ended in time, in count, or in document set SHALL NOT be created.

**EE-SIG-011 (SHALL).** The consent screen SHALL display the **count** of documents to be signed and SHALL make each document's identity inspectable before authorisation. Where the count exceeds one, the count SHALL be presented as a primary element of the screen, not as detail behind a disclosure control.

**EE-SIG-012 (SHALL).** Before QSCD activation, the remote signing service SHALL verify an authenticated, ordered binding of every (`documentReference`, `documentDigest`, `signatureQualifier`) tuple to the corresponding document enumerated in the SAD and SHALL reject the authorisation request where the number of `documentDigests` supplied differs from the declared signature count, where any tuple differs in value or order from the SAD enumeration, where any supplied digest fails its integrity check, or where any requested `signatureQualifier` cannot be produced with the credential and remote QSCD presented.

**EE-SIG-013 (SHALL).** Where expression of will is a goal of the signature, the wallet SHALL implement a **non-trivial invocation interaction** per ETSI TS 119 101 SCP 51, and SHALL NOT satisfy it with a single confirmatory tap that is identical to the wallet's ordinary presentation-consent gesture. The interaction SHALL be distinguishable, in the user interface, from consenting to disclose an attribute.

**EE-SIG-014 (SHALL NOT).** A single user authentication SHALL NOT be reused across signature sessions. The signature activation SHALL expire with the session, and the wallet SHALL NOT offer a "remember this authorisation" or "sign all future documents" control.

#### 15.1.1 Where a seal is the right instrument

Where the signing party is an organisation and the purpose is **origin and integrity** rather than the expression of an individual's will, the correct instrument is an **electronic seal**, not an electronic signature applied many times over. The legal architecture supports this squarely:

- Art. 3(25): a seal is data attached to other data "to ensure the latter's **origin and integrity**" — nothing about will;
- Art. 3(24): the creator of a seal is a **legal person**;
- Art. 35(2): a qualified seal "shall enjoy the presumption of integrity of the data and of correctness of the origin of that data", which is a narrower and more accurate presumption than Art. 25(2)'s handwritten-signature equivalence for a QES;
- Art. 36(c): "under its control", not *sole* control;
- **ETSI TS 119 431-1 §5.3.2** expressly contemplates the signer being "a device or system operated by or on behalf of a natural or legal person", and **§4.1** notes that the EN 419241-1 sole-control requirements "may be applied mutatis mutandis to electronic seals … the reader may replace the term 'sole control' with 'control'".

**[UNVERIFIED — this specification's own conclusion, not a citation]** No ETSI deliverable or Commission instrument was found that *recommends* seals for high-volume automated signing. The recommendation below is drawn from the provisions above and is stated as this specification's engineering position, not as received guidance. Anyone relying on it should say so in their own terms.

**EE-SIG-015 (SHOULD).** Automated or high-volume signing performed **on behalf of an Estonian public body or other legal person**, where no individual's expression of will is being recorded, SHOULD be implemented as a qualified electronic **seal** under Art. 36 and Annex II rather than as batched natural-person signatures. Routing such workloads through a natural person's wallet QSCD imports a sole-control and intent obligation (Art. 26(c); SCP 50–52) that the workload does not need and cannot honestly satisfy.

**EE-SIG-016 (SHALL).** Where a workload is nonetheless implemented as batched natural-person signatures, the risk register SHALL record the reason a seal was rejected, and the DPIA SHALL address the position of a signatory who is asked to attest will over documents they have not individually read.

#### 15.1.2 The offices for which a seal is not available

EE-SIG-015 resolves the ordinary case by moving the workload to a legal person. It does not resolve the case where the law attaches the act to a **named natural person holding an office** — the notary, the bailiff, the sworn translator. For those, the signature is the office-holder's, a seal in the organisation's name is not a substitute, and the volume is real. The consent architecture of §15.1 applies to them at full strength, and it constrains throughput: SCP 51 is not satisfiable at scale, and EE-SIG-011's inspectability requirement does not become less binding because the list is long.

**EE-SIG-017 (SHALL).** Where an office-holder's signature is required by law and a seal is unavailable, the wallet SHALL bound the enumerated document set of a single SAD to a size at which EE-SIG-011's inspectability is genuinely achievable, and the deployment SHALL publish that bound. A batch size chosen for throughput, at which no signatory could inspect the set, SHALL NOT be used to claim conformance with EE-SIG-011 — the correct response is more authorisations, not a larger enumeration.

**EE-SIG-018 (SHOULD).** Estonia SHOULD determine, before pilot, for which statutory offices a qualified electronic **seal** in the office's own name is legally sufficient, and SHOULD amend the governing instruments where it is. This is a legislative question, not a wallet-design question, and the wallet cannot resolve it. **[Whether Estonian law compels a natural-person QES — as opposed to permitting a seal — for notarial acts (notariaadiseadus, tõestamisseadus) or enforcement acts (kohtutäituri seadus) was not verified in this session: UNVERIFIED.]**

*Stated plainly:* for these offices this specification imposes a real throughput cost and does not remove it. That is the correct outcome where the law requires an expression of an individual's will, and it is an argument for legislative change rather than for a weaker consent model.

---

## 16. Privacy and data protection

**EE-PRI-020 (SHALL).** A DPIA SHALL be completed before pilot and SHALL specifically address: the `isikukood`'s correlator role; issuer-side visibility of issuance and re-issuance events; the residual linkability of the Tier 1 path; the ZKP circuit registry as a trust root; the interaction between the wallet's local transaction log and the Population Register's Andmejälgija; and **the WSCD provider's per-presentation visibility on the remote-HSM path (§4.2, R13)**, which where the provider is a state body is a state-held activity log over wallet use and SHALL be assessed as such rather than as an incident-response by-product.

**EE-PRI-021 (SHALL).** The design SHALL align with **EDPB Statement 1/2025 on Age Assurance** (adopted 11 February 2025), in particular its recommendation that due consideration be given to *"technologies and architectures favouring user-held data and secure local processing (device-based), allowing properties such as unlinkability (from different parties' point of view and even in the case of collusions or data breaches) and selective disclosure."* Note the phrase "even in the case of collusions" — that is a ZKP requirement, not a batching requirement.

**EE-PRI-022 (SHALL).** Self-declaration SHALL NOT be used as age assurance for any Estonian relying party subject to a statutory age gate. The Commission's Art. 28 DSA guidelines (C/2025/5519, 14 July 2025) are explicit that self-declaration is not appropriate, and identify sale of alcohol, tobacco and nicotine products, access to pornographic content, and gambling as contexts where age *verification* rather than estimation is appropriate.

**EE-PRI-023 (SHALL).** No central log of age-verification events SHALL be created, other than the operator's minimal, aggregated, non-identifying service telemetry, which SHALL NOT include relying party identity at per-event granularity.

**EE-PRI-024 (SHALL).** Cross-border identity matching under Art. 11a SHALL implement CIR 2025/846 with measures preventing profiling (Art. 11a(2)).

### 16.1 A note on the Estonian policy position

Estonia's stated government position is sceptical of age-gating as a policy instrument. The Minister of Justice and Digital Affairs has said that *"a purely age-based technical access restriction fails to address the core issues surrounding youth safety online"* and that such hurdles are bypassable by using another account, device or VPN; Estonia bases its social-media minimum age on GDPR's 13-year floor and has declined to adopt the Commission's age-verification app, citing reported security flaws. Estonia and Belgium were the only two Member States that did not sign the October 2025 Jutland Declaration.

This specification takes no position on that policy debate. But it has a design consequence worth stating: **Estonia's reluctance to deploy the Commission's app is not a reason to omit an age attestation from the wallet.** The statutory age gates in Alkoholiseadus, Tubakaseadus and Hasartmänguseadus already exist and are already enforced badly — a 2024 test-purchasing study in Tallinn found not one venue asked an under-age-looking buyer for a document, and a 2022 study found documents were requested in only 49.3 % of hospitality alcohol test purchases. A privacy-preserving `age_over_18` proof improves compliance with existing law while *reducing* the data disclosed relative to the status quo, which is handing over a document that reveals name, photo, `isikukood`, date of birth and sex. The current alternative to a ZK age proof in Estonia is not "no age check" — it is the plaintext `isikukood` barcode in Eesti äpp.

Note also that TTJA already accepts an identity document presented via Eesti äpp as a basis for establishing age in e-commerce, with the requirement that the buyer open the photo. That is the baseline this specification improves on.

---

## 17. Security and certification

### 17.1 Certification path

**EE-SEC-010 (SHALL).** The wallet solution SHALL be certified under CIR 2024/2981 against ARF ch. 7. Certification SHALL use available European cybersecurity certification schemes under Art. 5c(2) for the requirements, or parts thereof, within their applicable scope. Estonia SHALL establish a national certification scheme under Art. 5c(3) for requirements not relevant for cybersecurity and for cybersecurity requirements to the extent that an available European cybersecurity certification scheme does not cover, or only partially covers, them; the draft national scheme SHALL be transmitted to the European Digital Identity Cooperation Group, which may issue opinions and recommendations.

> **Terminology.** These schemes are widely called *transitory*; that is not the Regulation's word. Art. 5c(3) is a gap-filling provision of permanent form — it applies to requirements not relevant for cybersecurity, and to cybersecurity requirements only to the extent an Art. 5c(2) scheme does not cover them. CIR 2024/2981 is adopted under **Art. 5c(6)** and never cites Art. 5c(3). The transitional effect comes from **CIR 2024/2981 Art. 21** ("Transition to a European cybersecurity certification scheme") and recital 26, read with **Art. 57(1) of Reg. (EU) 2019/881**, under which national schemes with the same scope cease issuing certifications after a defined transition period. A scheme document that describes itself as transitory has mis-stated its own legal basis.

**EE-SEC-011 (SHALL).** Certification SHALL be valid for at most 5 years with a vulnerability assessment at least every 2 years; an unremedied vulnerability SHALL cause cancellation (Art. 5c(4)).

**EE-SEC-012 (SHALL).** Functional conformance SHALL be demonstrated against the **Functional Conformance Assessment Framework** introduced in ARF v3.0.0 §7.5 (`https://conformance.eudi.dev/`) for Annex III of CIR 2024/2981, in addition to, not instead of, the security evaluation.

**EE-SEC-023 (SHALL).** The national scheme SHALL be implemented as a **type 6 scheme** under EN ISO/IEC 17067:2013 §5.3.8, and SHALL use, where available and applicable, European cybersecurity certification schemes under Reg. (EU) 2019/881 including the EUCC, and national schemes covered by the EUCC under CIR 2024/482 Art. 49 (CIR 2024/2981 Art. 4(1), 4(3)(b)).

### 17.1.1 What CIR 2024/2981 actually fixes — and what it leaves open

This is the point most often stated backwards in programme documentation, so it is set out here against the enacting text rather than the recitals. **CIR 2024/2981 Annex IV** ("Methods and procedures for evaluation activities") has seven sections; sections 2–5 are the ones that bind.

| Component | Annex IV | What is required |
|---|---|---|
| **WSCD** | §2(1) | The part of the critical operations implemented in the WSCD "shall guarantee the protection of the critical operations it performs against attacks by attackers with high attack potential in accordance with … (EU) 2015/1502" |
| **WSCD** | §2(3) | Assessed against assurance level high per CIR 2015/1502 as a prerequisite; **where Art. 3(3)(b) conditions are met**, the evaluation "shall include a vulnerability assessment as set out in EN ISO/IEC 15408-3:2022 at level **AVA_VAN.5**, as set out in Annex I of … (EU) 2024/482" |
| **WSCA** | §3(1)–(2) | Evaluated against "at least assurance level high"; the evaluation "shall include a vulnerability assessment, as set out in EN ISO/IEC 15408-3:2022 at level **AVA_VAN.5**" — **unconditionally** |
| **WSCD / WSCA** | §2(3), §3(2) | Both carve out: "**unless it is duly justified to the certification body that the security characteristics of the WSCA make it possible to use a lower assessment level while keeping the same overall assurance level high**" |
| **WSCD / WSCA** | §2(5), §3(4) | Where an EUCC certificate is relied on, the security target SHALL be checked for conformity to a protection profile recommended in the EUCC |
| **Wallet instance** | §5(2) | "the Common Criteria framework may not be suitable in all cases … National certification schemes **shall consider the use of** the EN 17640:2018 methodology" — *consider*, not *use* |
| **All components** | Art. 13(1)(g) | Vulnerability assessment "at the appropriate level", including design and where applicable source-code review, and "testing of the resistance of the wallet solution against attackers with high attack potential for assurance level 'high' pursuant to Section 2.2.1 of the Annex to Implementing Regulation (EU) 2015/1502" |

**EE-SEC-020 (SHALL).** The security target for the WSCA SHALL claim **AVA_VAN.5** unless the CAB is given a written justification under Annex IV §3(2), and any such justification SHALL be recorded in the certification assessment report (Annex VIII) together with the argument that overall assurance level high is preserved. A lower level SHALL NOT be adopted silently.

*A named position on the §3(2) justification.* Google's August 2026 white paper argues for a **split-certification** model: the AVA_VAN.5 target of evaluation is confined to the secure-element hardware and a minimal cryptographic service provider implementing key generation, key isolation and attestation, with the remainder of KeyMint a client outside the boundary, and with user authentication (the lock-screen knowledge factor via Weaver, the biometric trusted applications) kept outside the "WSCA core" on the ground that GUI and biometric pipelines have historically made AVA_VAN.5 infeasible; it claims architectural parity with remote cloud HSMs, which likewise delegate authentication to a lower-assurance local interface (*op. cit.*, §5.1–§5.3). **This is a platform vendor's argument addressed to regulators, not a determination by any certification body**, and in substance it is a justification of the kind Annex IV §3(2) contemplates. Estonia treats it as one, which is what EE-SEC-024 requires. **[UNVERIFIED]** — no ECCG, ENISA or CAB acceptance of this scoping was found (§23 item 29).

**EE-SEC-024 (SHALL).** Where the security target for a WSCA relies on a **split-certification scoping** — a target of evaluation confined to the secure element and a cryptographic service provider, with user authentication or any other critical operation placed outside the certified boundary — that scoping SHALL be treated as a justification under Annex IV §3(2), and EE-SEC-020's written justification and Annex VIII record SHALL apply to it unchanged. The certification assessment report SHALL state explicitly whether the certified boundary includes user authentication or excludes it, and a boundary that excludes it SHALL NOT be reported as a certified WSCA without that statement. A scoping argument is not a lower assessment level, but it reaches the same place by a different route, and Annex IV §3(2) is the only provision that licenses either.

**EE-SEC-021 (SHALL).** The evaluation methodology for the **wallet instance** SHALL be named explicitly in the national scheme, since Annex IV §5(2) imposes none. Where EN 17640:2018 (FITCEM) is used, the scheme SHALL state which of the three routes in §5(2)(a)–(c) it takes. Where the methodology relied on is itself a draft — the ENISA candidate scheme and its separately versioned security-requirements companion both are — the scheme SHALL name the version and the date of the text it relies on.

*The candidate answer, and why it is not yet an answer.* The ENISA draft routes the wallet instance to EN 17640 (FITCEM) at a target of **AVA_VAN.3**, via a separate ad-hoc working group document. The routing is consistent with Annex IV §5(2); the assurance level is a policy choice in a draft with no legal force, and the draft never cites Annex IV while softening the WSCA's unconditional "shall claim AVA_VAN.5" to "should". The divergence is contested in public: *ENISAGaps* (wellet.nl, 31 March 2026) reads the AVA_VAN.3-for-a-remote-HSM-WSCA position as resting on assumptions about the operating environment rather than on the secure cryptographic interface, and names an unmitigated repudiation risk (TR64) in the draft's own Appendix B; a second reading (Ubiqu) is vendor-interested in the other direction. A scheme document citing "the ENISA methodology" is citing a moving target, which is why EE-SEC-021 obliges it to name the version and the date. Whether a level below AVA_VAN.5 survives Annex IV §3(2) at all is a legal question, not an engineering one.

**EE-SEC-022 (SHALL).** Where Annex IV §2(3) applies, the security target for the WSCD SHALL claim **AVA_VAN.5**. Any justified use of a lower assessment level SHALL be documented in writing with the CAB and in the certification assessment report (Annex VIII), together with the argument that the security characteristics of the WSCA preserve the overall assurance level high. A lower level SHALL NOT be adopted silently.

**No EAL is fixed.** "EAL4" appears exactly once in CIR 2024/2981 — in **recital 7**, hedged as certification standards "such as Common Criteria … by EAL4 evaluation and advanced methodical vulnerability analysis, such as comparable to AVA_VAN.5", and further deferred to "at the latest when certification … is carried out following a European cybersecurity certification scheme adopted pursuant to Regulation (EU) 2019/881". It occurs nowhere in Articles 1–22 or Annexes I–IX. A recital is not an obligation. **This specification therefore states no EAL requirement**, and any Estonian scheme document that asserts one is stating a policy choice, not a citation.

**What "high attack potential" is anchored to.** CIR 2015/1502 Annex §2.2.1 ("Electronic identification means characteristics and design"), level High, reads: "The electronic identification means protects against duplication and tampering as well as **against attackers with high attack potential**." CIR 2015/1502 nowhere defines that phrase numerically or by reference to Common Criteria — the instrument contains no CC, EAL or AVA_VAN reference at all. The bridge from the eID assurance-level term to a CC vulnerability-analysis level is made by CIR 2024/2981 Annex IV, not by CIR 2015/1502, and only for the WSCD and WSCA.

**Status of the EU scheme.** ENISA published a **draft candidate EUDIW certification scheme v0.4.614 on 31 March 2026** with public review through April 2026 (ENISA's news item dates the launch 2 April and its announcement text 4 April; read it as early April). ENISA's own published roadmap of 15 April 2026 then puts ad-hoc working group validation on 16–17 April, ECCG comments closing **1 June 2026**, a second public consultation from **early July 2026**, and **ECCG finalisation targeted for September–October 2026**, followed by a Commission "Have Your Say" consultation on the draft implementing act. The companion security-requirements document is separately versioned **v0.5-614**, i.e. a less mature document than the scheme it accompanies. **[UNVERIFIED]** — as of 14 September 2026 the ENISA certification portal carried no EUDIW-specific item later than the April 2026 batch, so whether finalisation has occurred inside the scheme's own target window is unconfirmed. Read the scheme as on schedule against its own roadmap rather than as stalled; the window is open now. **No published Common Criteria Protection Profile specific to an EUDI wallet or to a WSCA was found**; the relevant existing PPs are the QSCD/SSCD family (EN 419211 series, EN 419241-2 for remote signing), and, for the secure-IC layer alone, the BSI-CC-PP-0084-2014 / BSI-CC-PP-0117-2022 family that Android's CDD already requires of a StrongBox backend (§4.2). A certified IC is not a certified WSCA; the gap is the application layer above it. Note that Annex IV §2(5) and §3(4) presuppose a *recommended* PP under the EUCC, so the absence of one is a live gap in the evaluation chain and not merely a documentation inconvenience.

**EE-SEC-013 (SHALL).** Where no wallet-specific Protection Profile exists at the time of evaluation, the security target SHALL explicitly enumerate the assets, threats and objectives it covers, and the CAB report SHALL record the absence of a PP as a limitation on comparability.

### 17.2 Threat model highlights

| Threat | Mitigation in this specification |
|---|---|
| Issuer–verifier collusion linking presentations | §10 ZKP path; Tier 1 labelled honestly per EE-ZKP-002 |
| Colluding verifiers linking presentations | §9.5 once-only batches; §10.5 ZKP |
| Timestamp fingerprinting across a batch | EE-POA-012 coarsened `ValidityInfo` |
| Downgrade to plain mdoc | §10.8, wallet-side capability resolution |
| Circuit substitution / under-constrained circuit | §10.6 registry, CT logging, audit-linked acceptance, 30-day deprecation |
| Trust-the-client enrolment | §9.4 register-sourced predicates, full passive authentication |
| Cross-device consent phishing | §11.3, BCP 247 mitigations, same-device preference |
| Over-asking by relying parties | §5.2 registered intended use, access certificate service identifiers, wallet-side enforcement |
| Platform key attestation bypass | §4.2 EE-SEC-004, attestation treated as evidence not proof |
| `isikukood` correlation | §7.4 EE-PID-007/008, Annex D |
| Two ID-card trust chains mishandled | §6.2 EE-ONB-003/004 |
| Wallet compromise at scale | CIR 2025/847 suspension; ARF Topic 38 revocation |
| **WSCD provider observes every presentation (remote-HSM path)** | **Not mitigated.** §4.2 EE-SEC-001a/b/c bound and disclose it; local-native WSCD is the only remedy. Recorded as residual in §21.1 R13 |
| **Relying party silently declines ZKP to force a linkable presentation** | §10.8 EE-ZKP-054/055/056, capability attested in the registration certificate. Estonian-registered relying parties only |
| **Proof too large to transfer inside the usability budget** | §10.7 EE-ZKP-044/045, measured end-to-end budget, transport preference, honest fallback |
| **Anonymous predicate offered where statute compels identification** | §9.3 EE-POA-004/005/005a, Registrar refuses predicate registration for gambling intended uses |

**EE-SEC-014 (SHALL).** Penetration testing SHALL specifically include an attempt to obtain an EE-PoA for a false age via a modified client, and the test SHALL be repeated at each release. The published flaw in the EU reference issuer is the canonical case.

---

## 18. Accessibility and inclusion

**EE-ACC-001 (SHALL).** The wallet SHALL conform to Directive (EU) 2019/882 (Art. 5a(21)) and **EN 301 549 v3.2.1**, and SHALL satisfy ARF Topic 54 (`ACC_01`, `ACC_02`) and ARF ch. 8.

**EE-ACC-002 (SHALL).** Wallet use SHALL be voluntary, with no disadvantage for non-users (Art. 5a(15)). The ID-card and Mobile-ID paths SHALL remain fully supported for every service that accepts the wallet.

**EE-ACC-003 (SHALL).** A non-smartphone activation path SHALL exist — ID-card with a reader on a desktop, with the wallet's WSCD provisioned as a remote HSM. The disclosure duty of **EE-SEC-001b** applies to this path in full: a user who takes it because they have no suitable smartphone SHALL be told, before activation, that the WSCD provider will see the timing of their presentations. Inclusion SHALL NOT be delivered by quietly giving the less-equipped user the more-observable wallet.

**EE-ACC-004 (SHOULD).** The wallet SHOULD be available in Estonian, Russian and English at launch. Estonia's Russian-speaking population is a material share of the resident population and an Estonian-only wallet would predictably reproduce existing digital-inclusion gaps. **[The current share was not researched in this session — UNVERIFIED.]**

---

## 19. Coexistence and migration

**EE-GOV-030 (SHALL).** The wallet SHALL coexist with, and SHALL NOT replace, the ID-card, Mobile-ID and Smart-ID. The transposition bill's own framing — and the government's stated policy of maintaining at least two reliable state-backed eID solutions — treats parallel operation as a resilience property, not a transitional cost.

**EE-GOV-031 (SHALL).** Mobile-ID SIM replacement SHALL be complete before **19 May 2027**, after which unreplaced SIMs stop working for both authentication and signing. Wallet onboarding flows that depend on Mobile-ID SHALL handle the replacement state and SHALL warn users. Note that phishing campaigns impersonating this SIM swap are already circulating.

**EE-GOV-032 (SHALL).** TARA and GovSSO SHALL be extended to accept an EE-EUDIW presentation as an authentication method, so that the wallet reaches the existing 60+ integrated public authorities without each integrating separately. A verified presentation is not by itself a login, and nothing in the Regulation or the implementing acts says how a relying party turns one into a session: the relying party SHALL bind completion to the browser context that began the flow, through a single-use pending-login record created before the QR or the DC API call is issued, and any claim used to key an account SHALL meet OpenID4VP 1.0 §14.4 — stable, locally unique, never reassigned, and scoped to its issuer. OpenID4VP §14.2 warns that redirect-back binding is unavailable in the cross-device case, which is where this rule earns its keep. An EE-PoA presentation carries no such claim by design and can only yield an age-gated anonymous session.

**EE-GOV-033 (SHALL).** ID-card e-services SHALL complete migration to **Web eID**; backward compatibility for legacy TLS client-certificate authentication is removed at the end of 2026.

**EE-GOV-034 (SHOULD).** Estonia SHOULD reuse the wallet's relying-party register and access-certificate infrastructure for TARA/GovSSO relying parties, rather than operating two parallel registries.

---

## 20. Interoperability and conformance

**EE-CNF-001 (SHALL).** The wallet SHALL obtain **OpenID Foundation self-certification** for OpenID4VP 1.0, OpenID4VCI 1.0 and HAIP 1.0. The programme was announced to open on 26 February 2026; OIDF's 7 August 2026 update states the suites are now complete and open for self-certification. Suites are free, open source and runnable in CI.

**EE-CNF-002 (SHALL).** The wallet SHALL pass the FCAF test set at `conformance.eudi.dev` for the functional requirements in Annex III of CIR 2024/2981.

**EE-CNF-003 (SHOULD).** Estonia SHOULD participate in the Commission's Interoperability Test Bed events (the December 2025 Launchpad ran 420 peer-to-peer tests with 60 testers from 16 countries) and in ETSI EAA Plugtests.

**EE-CNF-004 (SHALL).** Estonia SHALL contribute its Track A ZKP verifier to the APTITUDE pilot, which Estonia joined in spring 2026 and in which RIA leads the Estonian side. This is the cheapest available route to third-party validation of an unusual component.

### 20.1 Conformance profiles

| Profile | Contents | Mandatory for |
|---|---|---|
| **EE-CORE** | PID in both encodings; OpenID4VCI/VP/HAIP; WUA; dashboard; erasure and DPA reporting; pseudonyms; revocation; portability | All wallet instances |
| **EE-AGE-1** | EE-PoA and `eu.europa.ec.av.1`; batch once-only issuance; coarsened `ValidityInfo`; single-use consumption; downgrade signalling | All wallet instances |
| **EE-AGE-2** | EE-AGE-1 plus Track A ZKP presentation, circuit registry validation, performance budget **including the EE-ZKP-044 end-to-end transport budget**, honest-labelling | Wallet instances on capable devices; relying parties registered for age-check-only intended use from a date set by RIA |
| **EE-QES** | Wallet-based QES, ASiC-E LT/LTA, `transaction_data` binding, SiVa validation | All wallet instances (Art. 5a(5)(g)) |
| **EE-PROX** | ISO/IEC 18013-5 proximity device retrieval; mDL; EE-PoA over the selected device-retrieval transport, **within EE-ZKP-044 where the presentation is Tier 2** | Wallet instances; mDL relying parties |

**EE-CNF-005 (SHALL).** A wallet instance SHALL NOT be certified as EE-AGE-2 on the basis of a demo or debug build variant.

**EE-CNF-006 (SHALL).** EE-AGE-2 and EE-PROX conformance SHALL require **measured** end-to-end evidence against EE-ZKP-044 on the reference device class and a reference relying-party terminal. A declaration of conformance based on prove-and-verify timings alone SHALL NOT be accepted, since those omit the transport that dominates the Tier 2 path.

**EE-CNF-007 (SHALL).** A wallet instance SHALL NOT be certified as achieving unobservability (`ZKP_07`) on a remote-HSM WSCD. Where a deployment offers both WSCD architectures, the conformance claim SHALL state the property separately for each.

---

## 21. Roadmap and dependencies

| Milestone | Target | Dependency |
|---|---|---|
| Operator contract signed | ~mid-2027 on RIA's own estimate | Procurement outcome (**UNVERIFIED**) |
| EUTS/RLS/KarS amendment in force | Unknown | Riigikogu (**no SE number found**) |
| RP register MVP + guidance | End-2026 / early 2027 per RIA | RIA |
| Access certificate design published | Open per RIA | RIA |
| Public-sector acceptance of foreign wallets | **24 Dec 2026** | TARA/GovSSO extension |
| ISO/IEC 18013-5 2nd edition published | Q4 2026 expected | ISO — **the single biggest schedule dependency for the ZKP profile**, since it defines `ZkRequest`/`ZkSystemSpec`/`ZkDocument` in §10.2.7 |
| SD-JWT VC becomes an RFC | Unscheduled — at `-18`, AD evaluation, revised I-D needed | IETF OAuth WG |
| ETSI TS 119 476-2 stabilises | Unknown; early draft v0.0.4 of 8 Jul 2026 | ETSI STF 705 |
| ARF Topic G refinement round runs | Iteration 6, 23 Sep – 18 Nov 2026 | EDICG — the open venue for a `ZKP_08` clarification (§10.3) |
| EE-AGE-2 pilot | Q3 2027 | Above |
| Private-sector acceptance obligation | **24 Dec 2027** | Art. 5f(2) |

### 21.1 Risk register (extract)

| # | Risk | Likelihood | Impact | Response |
|---|---|---|---|---|
| R1 | Estonia misses the 24 Dec 2026 wallet deadline | **Near certain** | Legal exposure; reputational | Accept; prioritise the acceptance side and the RP register, which are separately mandated and achievable |
| R2 | ZKP_08 remains unsatisfiable; a CAB refuses to certify the ZKP path | High | EE-AGE-2 blocked | EE-ZKP-003 conditional statement; keep Tier 1 fully conformant and independently certifiable |
| R3 | ISO 18013-5 2nd ed. slips past Q4 2026 | Medium | ZKP transport unstandardised | Implement against DIS; version the `ZkSystemSpec` handling |
| R4 | A soundness bug is found in the accepted circuit | Medium | Age proofs forgeable | §10.6 registry, 30-day deprecation, dual-implementation verification |
| R5 | Longfellow never reaches CFRG adoption | Medium-high | Track A standardisation gap persists | Track B investment; ETSI TS 119 476-2 participation |
| R6 | Procurement challenge (*vaidlustus*) delays contract | Medium | Whole programme slips | Outside this specification's control; note in the programme plan |
| R7 | Two ID-card trust chains cause activation failures | Medium | Onboarding attrition | EE-ONB-003/004; test matrix covering both chains |
| R8 | Relying parties treat EE-PoA as discharging a gambling identification duty (HasMS § 53(1) remote, § 37(8)–(10) premises entry) | Medium | Regulatory breach; the two channels carry separate penalties: **§ 94** up to 200 fine units or **€26,000 for a legal person** for the remote duty, **§ 89** up to 100 fine units or **€20,000 for a legal person** for entry identification | EE-POA-004/005/005a; Registrar refusal of predicate registration for any Hasartmänguseadus intended use |
| R9 | Performance budget unmet on median hardware | Medium | Silent downgrade to Tier 1 | EE-ZKP-040/042; honest labelling; Track B's far lower proving cost |
| R10 | Apple/Google platform policy changes break the DC API path | Low-medium | Remote presentation degraded | Maintain OpenID4VP redirect fallback |
| R11 | A CAB or relying party insists the EE-PoA must be a PuB-EAA | Medium | Revocation forced; privacy property lost | §9.7 documented divergence; EE-POA-021 escape route via PID |
| R12 | SD-JWT VC changes materially before RFC (`vct`, type metadata, `dc+sd-jwt` media type) | Medium | Re-issuance of SD-JWT VC PIDs | Issue PID in both encodings from day one; version the `vct` |
| R13 | Remote-WSCD users accumulate a provider-side activity log | **Certain, by construction, for that population** | Unobservability lost against one party; a state-held trace where the provider is a state body | **Not mitigable in the presentation path.** EE-SEC-001a/b/c: fallback only, disclosed, minimised, retention published; DPIA under §16; drive local-native coverage up to shrink the affected population |
| R14 | Proof transport exceeds the usability budget at the counter | **Medium-high; unmeasured** | Tier 2 unusable in proximity; permanent Tier 1 in retail | EE-ZKP-044/045 measured budget as a profile precondition; transport preference; Track B proof sizes (EE-ZKP-046) |
| R15 | No audited ≥125-bit circuit parameter set appears upstream | Medium | `ZKP_08` shortfall becomes permanent; Track A uncertifiable (compounds R2) | EE-ZKP-022a pilot-only operation with the shortfall declared; registry publishes the level (EE-ZKP-030); Track B investment |
| R16 | Foreign relying parties decline ZKP to obtain linkable presentations | Medium | Downgrade trapdoor open cross-border | EE-ZKP-054–056 close it domestically only; raise capability attestation in EDICG; honest labelling and counters meanwhile |

---

## 22. Deliberate divergences from the EU baseline

Recorded so a conformity assessor can find them quickly.

1. **Age is a dedicated attestation with bounded thresholds**, not a single `age_over_18`. Justification: `age_over_18` is the only Estonian statutory threshold an anonymous predicate can serve, and it is mandatory; the remaining thresholds are **optional** and exist for non-statutory benefit gates (`age_over_63`), for the unconformed HasMS § 83(1) (`age_over_16`), and for cross-border and sectoral use. Revision 1.2 removed the gambling justification for `age_over_21`: HasMS § 34(2) sets 21, but both enforcement channels are identification duties (§ 53(1) remote, § 37(8)–(10) premises), so no anonymous predicate discharges either — see §9.3. The EU AV attestation is issued in parallel for interoperability and is the designated cross-border path (EE-POA-024).
2. **Age predicates are computed from the Population Register**, not from a client-side document read. Justification: §9.4; Estonia has the authentic source and does not need to trust the client.
3. **Registration certificates are recommended over dynamic register lookup** (EE-RP-004). Justification: dynamic lookup at presentation time is a phone-home.
4. **Relying parties registered for age-check-only intended use will be required to support ZKP** from a date set by RIA (EE-ZKP-052). Justification: the AV Profile's "never reject the fallback" rule is otherwise a permanent downgrade path.
5. **HAIP 1.0 wins over ISO/IEC TS 18013-7 Annex B** where they conflict (EE-PRO-005). Justification: Annex B profiles a superseded OpenID4VP draft.
6. **All gambling is excluded from the anonymous age path** (EE-POA-004, EE-POA-005, EE-POA-005a). Justification: HasMS § 53(1) mandates identification for remote play, and § 37(8)–(10) mandates identification, identity-document copying and database registration for **entry to a gaming location**, penalised under § 89 at up to €20,000 for a legal person, while the remote duty under § 53(1) is penalised under § 94 at up to €26,000. § 39(8) adds a name-keyed self-exclusion screen across games of chance, toto and classical lotteries. Revision 1.2 corrects revision 1.1's contrary assertion that no registration duty attached at the door.
7. **No status list for EE-PoA** (EE-POA-017). Justification: matches the Danish AltID and the EU AV design; the risk is benign for monotonic age predicates only.
8. **The EE-PoA is a non-qualified EAA, not a PuB-EAA** (EE-POA-020, §9.7). Justification: CIR 2025/1569 Art. 4(3) as replaced by CIR 2026/1735 compels revocation for qualified and public-sector attestations valid for more than 24 hours, which is irreconcilable with `ZKP_07`. The legal cost — priced in §9.7.1: Art. 19a and CIR 2025/2160 obligations retained, ex post supervision only, Art. 13 burden of proof on the user, and Art. 45b(2)/(3) status unresolved — is accepted knowingly.
9. **Pseudonym keys are constrained beyond CIR 2024/2979 Annex V** (EE-PRI-017/018, §14.1). Justification: Annex V incorporates WebAuthn Level 2 without profiling it; WebAuthn's `none` default and self attestation would permit a pseudonym key outside the WSCD. The constraint is additive and does not conflict with Annex V.
10. **Cross-relying-party pseudonym unlinkability is required** (EE-PRI-010). Justification: CIR 2024/2979 Art. 14(2) requires only per-relying-party specificity; unlinkability is imposed here on Art. 5a(16)(b), and is flagged as such so an assessor does not seek it in the implementing act.
11. **High-volume organisational signing is routed to seals** (EE-SIG-015). Justification: §15.1.1. This is this specification's own engineering position and is **not** drawn from any ETSI or Commission recommendation; see the marker there.

12. **`mdoc` status is carried on a Token Status List in the interim** (EE-REV-001, §13). Justification: ARF `VCR_01` closes the `mdoc` method set to short-lived credentials (≤ 24 h), the ASL and the ARL, all as specified per `VCR_11`; `VCR_11` specifies the ASL and ARL for ISO/IEC 18013-5 as Annex 2 of the amended CIR 2024/2979 and notes that the CIR text is itself copied from the forthcoming 2nd edition of ISO/IEC 18013-5. Estonia therefore carries `mdoc` status on the Token Status List structure — the ARF's own specification of the ASL for SD-JWT VC (`VCR_11a`) — rather than on a wire format that is not yet fixed, and records this as a divergence rather than a conformance claim. Whether the CIR Annex 2 ASL for `mdoc` is itself Token-Status-List-shaped was not verifiable from the baseline: no legal text is in the checkout.
13. **A registration for an age-check-only intended use that names PID attributes is rejected by the Registrar** (EE-PID-008, Annex B.4). Justification: ARF `Reg_01b` (`AS-MS-27-003`) requires Member States to collect registration information "only for the purpose of transparency" and forbids applying "any pre-authorisation process on it" — and the information it covers includes the attributes registered for each intended use. EE-PID-008 departs from that knowingly, enforcing, at registration, the prohibition EE-PID-008 itself imposes at presentation. Annex E routes EE-PID-008 to `Reg_01b` so that a conformity assessor sees the conflict; the rejection duty's compatibility with Art. 5b is not resolved here.
14. **`EE-ACC-001` requires ETSI EN 301 549 v3.2.1** (§18). Justification: `ACC_01` and `ACC_02` name EN 301 549 **V1.1.2** (2015-04), which predates WCAG 2.1 entirely. v3.2.1 is the edition cited in the Official Journal for the Web Accessibility Directive (Implementing Decision (EU) 2021/1339, 18 August 2021) and carries WCAG 2.1 AA; a revision incorporating WCAG 2.2 AA was in public review in November 2025. The ARF's reference is dated, not contradicted.
15. **ZKP verification capability is an attested registration attribute, not a self-declaration** (EE-ZKP-054–056, §10.8). Justification: the AV Profile's capability signal is carried in the relying party's request, which the relying party controls and may simply omit in order to obtain a linkable presentation. Estonia moves the signal into the registration certificate, where the Registrar controls it. This binds Estonian-registered relying parties only.
16. **Unobservability is claimed per WSCD architecture, not per wallet solution** (EE-SEC-001b, EE-CNF-007, §4.2). Justification: a remote WSCD sees the timing of every presentation. A solution-level `ZKP_07` claim covering both architectures would be false for the remote-WSCD population, and the ARF offers no vocabulary for a per-architecture claim.
17. **Track A is operated as a pilot while its soundness error is below the ACM's recommended level** (EE-ZKP-022a, §10.5). Justification: circuits v7 give a 109-bit soundness error against the "at least 125 bits" ACM v2.0 §1.1 sets for recommended mechanisms. Rather than claim `ZKP_08` conformance or abandon Track A, Estonia declares the shortfall to the certification body and confines Track A to pilot until audited ≥125-bit parameters exist.
18. **Proximity conformance requires measured end-to-end evidence** (EE-ZKP-044, EE-CNF-006, §10.7). Justification: no EU instrument budgets proof *transport*, and a ~400 KB payload over ISO 18013-5 device retrieval plausibly dominates the user-visible time. Estonia declines to certify against prove-and-verify figures alone.
19. **Track A is instantiated at the ACM's *recommended* level for the components the wallet controls** (EE-ZKP-022c, §10.5). Justification: `ZKP_08` asks only that the algorithms be ACM-listed, and the primitives Longfellow uses are listed in v2.0. The v3.0 working draft nonetheless demotes SHA-256 and every classical asymmetric mechanism to *admissible* (§10.9), so a deployment instantiated at the minimum of the day would need re-profiling on adoption of a document that has already been through public review. Estonia instantiates at the higher level now, and records the *admissible* mechanisms it cannot change — the issuer's ECDSA/P-256 signature and the SHA-256 hashing inside the credential — as a dependency rather than a choice. The tier list this is written against is a draft; see §23 item 6.

---

## 23. Open issues and unverified claims

Collected for the reader's convenience. Every item here should be re-checked before this document is used to build anything.

1. **RIA procurement outcome.** No award announced as of 5 Sep 2026. Determines whether the wallet is provided under Art. 5a(2)(a) or (b), and whether the Commission's Reference Implementation forms the codebase.
2. **Named PID Provider.** Not published; the procurement folds PID issuance into the operator's scope.
3. **EUTS/RLS/KarS amendment.** Submitted to the Government 19 Aug 2026; no Riigikogu SE number found. All §2.2 requirements are conditional on it.
4. ~~Estonian ID-card key algorithm.~~ **Resolved.** The live CA certificates at `crt.eidpki.ee` were retrieved and inspected: `CN=EEGovCA2025, O=Zetes Estonia OÜ, C=EE` is self-signed and issues `CN=ESTEID2025`; both are `id-ecPublicKey`, 384-bit, `secp384r1` (NIST P-384), signed `ecdsa-with-SHA384`. End-entity card key parameters were not separately confirmed.
5. **Re-notification of the Thales/Zetes cards under eIDAS.** Cross-border authentication was restored 1 July 2026, but the EC notified-schemes register still showed only the 2018 entry when last updated 2 Feb 2026.
6. ~~ECCG ACM v3.0 final content.~~ **Resolved.** There is no final v3.0 text. The April 2026 working draft was read in full on 8 September 2026 (published for public review 2 June 2026, review closed end of July); ENISA still lists **v2.0 (April 2025)** as the applicable version, which is also the version ZKP_08 pins by name. Neither version mentions zero-knowledge proofs, pairings, commitments or extension fields. What remains open is whether any later version ever lists a ZKP: the v3.0 draft's new Annex C requires a mechanism to be standardised, stable for two years and peer-reviewed before it may be submitted at all. Also open is whether the draft's *tiering* survives into an adopted text — its demotion of SHA-256 and of the classical asymmetric mechanisms to *admissible* is what EE-ZKP-022c and §22 item 19 are written against, which is why that requirement binds to "the applicable ACM" rather than to a named version.
7. **ENISA candidate scheme status** — specifically whether ECCG finalisation occurred inside the scheme's own **September–October 2026** target window (§17.1). Note this is separate from the Estonian national scheme, whose legal basis is Art. 5c(3) and whose transition is governed by CIR 2024/2981 Art. 21 — see §17.1.
8. **Whether ZKP has been promoted from SHOULD to SHALL** in any AV Profile revision after April 2026. **Partly answered.** Annex A as read on 12 September 2026 states that the *preferred* mechanism is a Zero-Knowledge Proof and restricts the plain ISO mDoc presentation to a fallback available only where the device cannot generate a proof or OpenID4VP is the transport (§A.6); it requires a relying party to be able to verify both (§A.8) and forbids rejection *solely* because the fallback was used (§A.9). That is not a blanket SHALL on ZKP support, so whether a later revision makes it one remains open.
9. **Whether the EU AV app's ZKP path is enabled in production builds.** Two independent published analyses say it is demo-only; not reproduced here.
10. **EU AV Profile attribute set** — whether it now carries thresholds beyond `age_over_18`. Annex A as read on 5 Sep 2026 defines `age_over_18` alone.
11. **Estonian pension age schedule** for the `age_over_63` predicate.
12. **Estonian median device installed base** for the reference device class. The device set the
   measurement programme (plan §6, D5) intends to use is decided — one Pixel 8 (which carries a
   recorded StrongBox anomaly from RIA's own PoC: a code comment at
   `app/src/main/kotlin/ee/cyber/wallet/security/AndroidEncryptionManager.kt:75` of
   `open-eid/eudi-wallet-poc` `master` (`0866e94`, v0.6.6), "commented out due to unknown issue on
   Pixel 8, where decrypted value is wrong", disabling StrongBox for the AES key that encrypts the
   stored PIN and wallet-instance credentials) and one mid-range phone (D5 gives a Samsung Galaxy
   A-series as an example) — but the devices have not been used: no measurement exists, and no
   claim about the Estonian installed base follows from the choice. The item stays open.
13. **Estonian language-share data** for EE-ACC-004.
14. **Commission Art. 49 review report**, due 21 May 2026 — publication not confirmed. Searched again on 14 September 2026 across the European Parliament's legislative train, the Commission's EUDI policy page, trade press and direct search: no trace of the report, of a press release announcing it, or of any secondary coverage. That is an absence of evidence, not evidence of absence, but it is a diligent absence.
15. **ARF Topic 40 requirement numbering.** Topic 40 carries 34 requirements numbered `WIAM_01`–`WIAM_21` with letter suffixes; cite by identifier, not by range arithmetic.
16. **HasMS § 83(1)** still penalises enabling lottery play by a person under **16** although § 34(3) now sets the age at 18 — an apparent unconformed cross-reference following RT I, 30.12.2025, 2. Confirm before relying on either.
17. **Vega** (Microsoft Research, 21 May 2026): 92 ms proving, 108 KB proofs, no trusted setup. A blog announcement with no independent audit and no presence in any EU specification. Promising; do not build on it yet.
18. **Whether Art. 45b(2) describes an issuer class or a compliant class.** The paragraph confers paper-equivalence on "attestations of attributes issued by, or on behalf of, a public sector body responsible for an authentic source"; Art. 45f sets requirements for that class. Whether an attestation from that issuer type that does not meet Art. 45f still attracts Art. 45b(2) and 45b(3) is not settled by the text and was not found resolved in guidance. §9.7.1 and EE-POA-023 take the conservative position. **This is the single most consequential unresolved legal question in this document.**
19. **No published attack-potential methodology for the wallet instance.** CIR 2024/2981 Annex IV §5(2) requires national schemes only to "consider the use of" EN 17640:2018. What methodology Estonia will name, and what "high attack potential" means for a mobile application as opposed to a secure element, are open. A candidate answer now exists — EN 17640/FITCEM at AVA_VAN.3, per the ENISA draft's working-group routing — but it lives in a non-binding draft that diverges from Annex IV (§17.1.1). **[UNVERIFIED]** — whether any JIL or SOG-IS attack-potential guidance applicable to mobile applications has been published was not confirmed in this pass; the smartcard guidance is not assumed transferable.
20. **Whether any Estonian scheme document asserts an EAL.** CIR 2024/2981 fixes none (recital 7 only, hedged). If a national document states one, it is a policy choice and should be identified as such.
21. **WebAuthn Level 3** became a W3C Recommendation on 25 August 2026, but CIR 2024/2979 Annex V pins Level 2 by dated URL. Whether the implementing act will be updated, and on what timeline, is unknown.
22. **EN 419 241-1 and -2 were not read.** SCAL2 and the `R.SAD` three-way binding in §15.1 are sourced from a Common Criteria Security Target and an ENISA publication respectively. Verify against the purchased normative text before drafting a security target.
23. **Estonian national pre-certification route.** The route by which an e-identification system is recognised nationally and its assurance level assessed as equivalent under Art. 8(2)(c) produces a national eID means, not a certified EUDI Wallet. What evidence produced for the first is reusable for the second is unplanned work and is not addressed by CIR 2024/2981.
24. **ISO/IEC 18013-5 device-retrieval throughput for a ~400 KB payload.** No measurement was performed or located, on BLE, NFC or Wi-Fi Aware, on any handset-and-terminal pair. EE-ZKP-044's four-second budget is derived from the `ZKP_05` usability constraint, not from evidence that it is achievable. **This is the largest unmeasured engineering assumption in the Tier 2 path** and it should be measured before any commitment to EE-AGE-2 in proximity.
25. **Whether audited ≥125-bit Longfellow circuit parameters exist or are planned**, and their cost in prover time, memory and proof size. EE-ZKP-022a is written against their absence. Step 0 of the conformance plan measured none of this — what it proved is that the two Longfellow halves verify each other's proofs: multipaz 0.99.0's JVM prover against this repository's Rust runtime and the reverse, both directions PASS with committed fixtures (`docs/planning/STEP0-CROSS-VERIFY-RECORD.md`, 23 September 2026). The circuits remain v7 at 109-bit soundness, below the ACM v2.0 recommended floor of 125 bits, so the item stays open.
26. **Android StrongBox key-generation throughput and key-slot capacity** on the reference device class, for the batch of ≥30 required by EE-POA-011. EE-POA-011a/011b are written to be safe without the figures; the figures should replace the assumption before pilot. Step 5 of the conformance plan replaced the fork wallet's software BKS EC keys with multipaz 0.99.0 `AndroidKeystoreSecureArea` — StrongBox where the device advertises `FEATURE_STRONGBOX_KEYSTORE`, TEE otherwise — and a batch-of-three `batchCreateKey` in one transaction now exists — three keys, below EE-POA-011's floor of 30, and not a measured EE-POA-011b maximum (fork `eudi-wallet-poc`, `step5-hardware-keys` / `step6-issuance-consumption`, PENDING-DEVICE records in that repository's `docs/planning/`). No key-generation time and no slot capacity were measured: JVM tests cannot create Android Keystore/StrongBox keys, and no device has been run. The item stays open, PENDING-DEVICE.
27. **Whether Estonian law compels a natural-person QES, as opposed to permitting a qualified seal, for notarial and enforcement acts.** Determines whether EE-SIG-015 resolves the high-volume case for those offices or whether EE-SIG-017's throughput cost is unavoidable. Legislative question; see §15.1.2.
28. **Whether HasMS § 37(8) reaches toto and lottery venues.** The duty is drafted on the *õnnemängu korraldaja* in respect of an *õnnemängu mängukoht*, and § 37(6¹) contemplates under-18s present in a toto location with a segregated area, which suggests it does not. No predicate in §9.2 is defined to serve such a venue and none should be added on this basis until the point is settled with Maksu- ja Tolliamet. The same enquiry should settle whether **§ 37(9)'s duty to copy the identity document's personal-data page** is discharged by a PID presentation or whether the physical document must still be produced — if the latter, the wallet adds nothing at a casino door and Annex B.3's corrected request is of no use there either. A second gap in the same item is not a legal question at all: § 37(8) item 3 requires the **name** of the identity document, and no PID attribute carries it (§7.2). Even on the most permissive reading of the copy duty, that item can only be recorded from the document itself.
29. **Where the certified boundary falls inside a local-native WSCD.** A StrongBox or Secure Enclave certificate is a certificate over a key *store*; whether it encloses the WSCA's critical operations or only the WSCD beneath a software WSCA is not separated by any source consulted, and EE-SEC-001's assurance argument depends on which. The same question in its sharpest published form is the split-certification scoping Google argues for — secure element plus a minimal cryptographic service provider as the target of evaluation, user authentication outside it (§17.1.1). No ECCG, ENISA or CAB response to that argument was found.
30. **No accredited Estonian conformity assessment body, and no chosen evaluation standard.** The 19 August 2026 bill memorandum still introduces wallet-CAB accreditation as a *new accreditation service*, and Cybernetica's D-25-53 §2.3.3.2 (8 April 2026) records that which security-evaluation standard Estonia will choose is not known. Every requirement in §17.1 presupposes a body that does not yet exist.


---

## Annex A — Estonian statutory age thresholds

| Domain | Age | Instrument | Duty on the checker |
|---|---|---|---|
| Alcohol — supply/transfer to a person | **18** | **Alkoholiseadus § 46, § 47(1)–(3)** | § 47(3): the transferor **must establish the acquirer's age on the basis of an identity document**, unless the acquirer is *ilmselgelt täisealine* ("obviously an adult") or personally known; if not obviously an adult and no document is shown, transfer is prohibited (RT I, 09.01.2018, 2, in force 19.01.2018) |
| Tobacco and nicotine products — sale | **18** | **Tubakaseadus § 27, § 28(2)–(3)**; penalties §§ 44–48 | § 28(2): the seller has a **duty to demand** the buyer's identity document and a **right to refuse** sale if none is produced. § 28(3) imposes a positive duty to establish the buyer's age from an identity document, with the same *obviously adult* / personally-known carve-out as Alkoholiseadus § 47(3) |
| Games of chance (*õnnemäng*), remote games of chance and remote games of skill; also presence in a games-of-chance venue | **21** | **Hasartmänguseadus § 34(2)** (narrow exception for Estonian-registered passenger ships: max stake **€50**, max win €2,000) | Operator; penalty HMS § 83 — up to 200 fine units or detention, legal person up to €26,000 (as amended RT I, 30.12.2025, 2, in force 01.01.2026). **An age predicate does not discharge this**: the entry duty in § 37(8)–(10) below is an identification duty, and it is the one actually performed at the door |
| **Entry to a gaming location (*õnnemängu mängukoht*)** | n/a (identification duty) | **Hasartmänguseadus § 37(8)–(10)** | § 37(8): the organiser **must establish the identity of every person entering**, registering forename and surname, **`isikukood` or, failing that, date of birth**, the **name, serial number, and place and date of issue of the identity document**, and the arrival time and date. § 37(9): the entrant presents an identity document and **a copy is taken of its personal-data page**, the data going into an electronic database. § 37(10): the organiser checks that database against the presented document before admitting the person. Penalty **§ 89** — up to 100 fine units, legal person up to **€20,000** (RT I, 30.12.2025, 2) |
| **Self-exclusion screening** (games of chance, toto, classical lottery) | n/a (identification duty) | **Hasartmänguseadus § 39**, esp. § 39(8) | The *hasartmängu mängimise piirangutega isikute nimekiri* is a sub-register of the taxpayers' register held by Maksu- ja Tolliamet; the organiser must take measures ensuring a listed person cannot play. A name-keyed list cannot be screened from an anonymous predicate |
| **Toto and lottery** | **18** | **Hasartmänguseadus § 34(3)** — "Totot ja loteriid ei tohi mängida alla 18-aastased isikud" (RT I, 30.12.2025, 2, in force 01.01.2026). Note § 83(1) still refers to 16 for lottery — an apparent unconformed cross-reference | Operator |
| Remote gambling — player registration | n/a (identification duty) | **Hasartmänguseadus § 53(1)** | Verify each player's identity; register forename, surname, **`isikukood` (or date of birth where the player has none)**, and the date and time of entering and leaving the gaming environment. Self-exclusion list held by Maksu- ja Tolliamet. Penalty **§ 94** — up to 200 fine units, legal person up to **€26,000** (RT I, 30.12.2025, 2). (§ 55 is player *information*; § 56 is blocking access to illegal remote gambling — neither creates this duty) |
| Data-processing consent (information society services) | **13** | GDPR Art. 8 as applied in Estonia; the stated basis for Estonia's social-media position | Service provider |

**The gambling rows are the important ones for this specification, and they point the other way from the rest of the table.** Alcohol and tobacco are *age* duties discharged by establishing that the buyer is over 18 — exactly what an anonymous predicate does, and better than a document, since the document also reveals name, photo and `isikukood`. Gambling is an *identification* duty in every channel: § 53(1) for remote play, § 37(8)–(10) at the door, § 39(8) for the self-exclusion screen. No anonymous predicate discharges any of them, and `age_over_21` is consequently Optional in §9.2, retained for cross-border and sectoral use rather than for any Estonian statutory gate. AML is not the operative basis and is widely miscited here: gambling organisers are obliged entities under **RahaPTS § 2(1) p 3**, but § 19(3) triggers customer due diligence only at a stake or payout of **€2,000** or more.

**Enforcement reality.** A 2024 Tervise Arengu Instituut test-purchasing study of Tallinn nightlife found **not one** venue asked an under-age-looking test buyer for a document; a 2022 TAI study found documents were requested in **49.3 %** of hospitality alcohol test purchases. Estonian retailers voluntarily card anyone appearing under **30** under a Kaupmeeste Liit agreement dating from 2016.

**E-commerce.** TTJA states the age-check duty applies to e-commerce, must be performed on the basis of an identity document, and that *"Ettevõtja võib vanuse tuvastada ka Eesti äpi kaudu esitatud isiku tõendava dokumendi alusel"* — the trader may also establish age on the basis of an identity document presented via the Eesti äpp, with the seller required to have the buyer open the photo. An EE-PoA presentation strictly dominates this on both assurance and privacy.

---

## Annex B — DCQL request examples

### B.1 Age-check-only, Estonian PoA, ZKP required

```json
{
  "dcql_query": {
    "credentials": [
      {
        "id": "ee_proof_of_age",
        "format": "mso_mdoc",
        "meta": { "doctype_value": "ee.riik.poa.1" },
        "claims": [
          { "path": ["ee.riik.poa.1", "age_over_18"] }
        ]
      }
    ]
  }
}
```

The relying party additionally signals ZKP support and requirement out of band per the ISO/IEC DIS 18013-5 2nd ed. `ZkRequest` structure, naming the accepted `ZkSystemSpec` set from the Estonian ZK Circuit Registry (§10.6).

### B.2 Cross-border fallback to the EU AV Profile attestation

```json
{
  "dcql_query": {
    "credentials": [
      {
        "id": "proof_of_age",
        "format": "mso_mdoc",
        "meta": { "doctype_value": "eu.europa.ec.av.1" },
        "claims": [
          { "path": ["eu.europa.ec.av.1", "age_over_18"] }
        ]
      }
    ]
  }
}
```

This is the query shape given in the Commission's AV Profile Annex A, reproduced here so Estonian relying parties integrate against the EU form without modification.

### B.3 Anti-pattern — gambling premises access

A relying party operating a gaming location **SHALL NOT** send this request. It was given as a permitted example in revision 1.1 and is reproduced here as the anti-pattern it is, because it is the mistake a casino integrator is most likely to make in good faith:

```json
{
  "dcql_query": {
    "credentials": [
      {
        "id": "ee_proof_of_age_21",
        "format": "mso_mdoc",
        "meta": { "doctype_value": "ee.riik.poa.1" },
        "claims": [
          { "path": ["ee.riik.poa.1", "age_over_21"] }
        ]
      }
    ]
  }
}
```

`age_over_21` proves the § 34(2) age bar and nothing else. Entry to an *õnnemängu mängukoht* is governed by **HasMS § 37(8)–(10)**, which requires the organiser to establish identity, record name, `isikukood` or date of birth, and the identity document's name, serial number and place and date of issue, to take a copy of its personal-data page, and to check the visitor database before admitting the person — on pain of **§ 89**, up to €20,000 for a legal person. **§ 39(8)** adds a name-keyed self-exclusion screen. A relying party that accepted the request above would have identified no one, screened no one, copied no document, and would be in breach at the moment it let the person through the door.

The correct request is a PID presentation. It is an ordinary identifying request and needs no anonymous-age machinery at all — but it is **conditionally valid, not a completed identification**, and the conditions are three:

1. **What it does supply.** Items 1 and 2 of § 37(8) in full (`family_name`, `given_name`, and `personal_administrative_number`, or `birth_date` where the PID carries no `isikukood`), and item 3's serial number and date of issuance (`document_number`, `issuance_date`).
2. **What no PID attribute can supply.** Item 3 also requires the **name** of the identity document — the document, not the credential and not its `vct`. The PID data model has no attribute for it: §7.2's inventory from CIR 2024/2977 reaches only `issuing_authority`, `issuing_country`, `expiry_date`, `issuance_date`, `document_number` and `issuing_jurisdiction`, and none of these is the document's name or type. `issuing_authority` is the authority, not the place of issue. No query can repair this: it is a gap in the data model, not in the request.
3. **What only a document can do.** § 37(9) requires the entrant to present an identity document and a copy to be taken of its personal-data page, and § 37(10) requires the visitor database to be checked against *that* document before admission. A presentation is neither a document nor a copy of one.

The request to send is this, with the claims the PID actually carries:

```json
{
  "dcql_query": {
    "credentials": [
      {
        "id": "gaming_location_entry",
        "format": "mso_mdoc",
        "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" },
        "claims": [
          { "path": ["eu.europa.ec.eudi.pid.1", "family_name"] },
          { "path": ["eu.europa.ec.eudi.pid.1", "given_name"] },
          { "path": ["eu.europa.ec.eudi.pid.1", "personal_administrative_number"] },
          { "path": ["eu.europa.ec.eudi.pid.1", "document_number"] },
          { "path": ["eu.europa.ec.eudi.pid.1", "issuing_authority"] },
          { "path": ["eu.europa.ec.eudi.pid.1", "issuance_date"] },
          { "path": ["eu.europa.ec.eudi.pid.1", "birth_date"] }
        ]
      }
    ]
  }
}
```

Per EE-POA-005a the Registrar SHALL refuse to register an EE-PoA predicate against any intended use citing Hasartmänguseadus, so the first request fails at registration rather than at the door.

**The physical-document fallback, defined.** Because item 3 is incomplete by construction and § 37(9)–(10) impose duties on the document itself, a PID presentation SHALL NOT be treated as discharging § 37(8)–(10):

- the entrant SHALL present the physical identity document;
- the organiser SHALL take the § 37(9) copy from that document and record the document's name, serial number and place and date of issue from it;
- where a PID presentation is used at all, it SHALL be an addition to that procedure, and SHALL NOT be represented to the entrant as replacing it.

Whether Maksu- ja Tolliamet reads § 37(9)'s copy duty, or item 3's document name, as satisfiable electronically is open, and §23 item 28 tracks both questions. Until they are answered, this specification SHALL NOT claim that the wallet replaces the identity document at a gaming-location door.

### B.4 Anti-pattern — what a relying party SHALL NOT send for an age check

```json
{
  "dcql_query": {
    "credentials": [
      {
        "id": "pid",
        "format": "mso_mdoc",
        "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" },
        "claims": [
          { "path": ["eu.europa.ec.eudi.pid.1", "birth_date"] },
          { "path": ["eu.europa.ec.eudi.pid.1", "personal_administrative_number"] }
        ]
      }
    ]
  }
}
```

Prohibited by EE-PID-003 and EE-PID-008. `birth_date` discloses exact age; `personal_administrative_number` is the `isikukood`, which discloses date of birth and sex on its own and is a lifelong national correlator. The Registrar SHALL refuse an age-check-only intended use that names either attribute.

---

## Annex C — EE-PoA CDDL sketch

Informative. Normative encoding follows ISO/IEC 18013-5 and the ARF Attestation Rulebook template.

```cddl
; Namespace: ee.riik.poa.1
; DocType:   ee.riik.poa.1

EEPoANamespace = {
  "age_over_18"      => bool,
  "age_over_16"      => bool,
  "age_over_21"      => bool,
  ? "age_over_63"    => bool,
  * age_over_NN      => bool,      ; NN not in {16,18,21,63}
  "issuing_country"  => tstr,      ; "EE"
  "issuing_authority"=> tstr,
  "expiry_date"      => full-date
}

age_over_NN = tstr .regexp "age_over_([0-9]{1,3})"

; ValidityInfo, coarsened per EE-POA-012:
; signed, validFrom, validUntil share identical hh:mm:ss across the batch.
; validUntil - validFrom <= 3 months  (EE-POA-016)
```

**Encoding rules** (inherited from the PID Rulebook's canonical-CBOR profile): integers as small as possible; lengths of `bstr`, `tstr`, arrays and maps as short as possible; no indefinite-length items; no fractional seconds; no local UTC offset — `time-offset` set to `"Z"`.

---

## Annex D — The isikukood, and why it must not appear in an age proof

### D.1 Structure (EVS 585:2007)

Format `SYYMMDDSSSC`, 11 digits.

**Digit 1 — century and sex:**

| Birth years | Male | Female |
|---|---|---|
| 1800–1899 | 1 | 2 |
| 1900–1999 | 3 | 4 |
| 2000–2099 | 5 | 6 |
| 2100–2199 | 7 | 8 |

**Digits 2–3** — last two digits of the birth year. **Digits 4–5** — month (01–12). **Digits 6–7** — day (01–31).

**Digits 8–10** — *järjekorranumber*, the serial distinguishing people born on the same day (000–999). For people born before 2013 it may encode a hospital identifier; since 1 January 2013 a healthcare institution requests a newborn's code over X-tee and serials are allocated strictly in request order. Codes issued outside Estonia — naturalisation, restored citizenship, residence permits, **e-residents** — use the issuing department's marker or wider ranges.

**Digit 11 — checksum, "Moodul 11", two rounds:**

```
w1 = [1,2,3,4,5,6,7,8,9,1]
w2 = [3,4,5,6,7,8,9,1,2,3]

s1 = sum(d[i] * w1[i] for i in 0..9)
r  = s1 mod 11
if r != 10:
    checksum = r
else:
    s2 = sum(d[i] * w2[i] for i in 0..9)
    r2 = s2 mod 11
    checksum = 0 if r2 == 10 else r2
```

### D.2 Why this is decisive

An `isikukood` is a date of birth and a sex, written in decimal, with a check digit. There is no computation to perform and no key to hold: `38001085718` says male, born 8 January 1980. It is printed on documents, it is the `serialNumber` in every ID-card certificate as `PNOEE-<isikukood>`, it is the `CN`, it appears in essentially every Estonian public and private database as a join key, and Eesti äpp emits it as a plaintext barcode for loyalty-card use — a mode RIA itself describes as legally equivalent to reading it aloud.

Andmekaitse Inspektsioon's position is that the code is not sensitive personal data and need not be concealed more than a name. That is a defensible position about the code's *confidentiality*. It is not a reason to route it through an age-verification flow, where its presence converts an unlinkable predicate proof into a nationally-correlatable identification event.

Hence EE-PID-007 and EE-PID-008: no age-verification code path touches the `isikukood`, in any form, including as a salt input, a key derivation input, a subject identifier, or a transport correlation value.

### D.3 A note on what a ZK proof does not fix

If an Estonian relying party asks for `isikukood` alongside an age proof — because its CRM keys on it, as most do — the age proof's unlinkability is worth nothing. The privacy property is a property of the *transaction*, not of the credential. This is why §5.2's registered-intended-use enforcement is load-bearing and not administrative boilerplate.

---

## Annex E — Traceability matrix (extract)

This matrix is an extract: its 55 rows cover **105 of the 187** requirement identifiers defined in this document. The remaining 82 rest on the ARF high-level requirements, implementing acts and Estonian statutes cited at each requirement in the body text, and are to be enumerated here in a later revision.

Both figures are measured, not counted by hand, and are reproducible from the source:

```bash
grep -oE '\*\*EE-[A-Z]{2,4}-[0-9]{3}[a-z]? \(' spec/EE-EUDIW-TS-1.0.md | sort -u | wc -l   # 187 requirements
awk '/^## Annex E/{f=1;next} f&&/^## /{exit} f' spec/EE-EUDIW-TS-1.0.md | grep -c '^| EE-'  # 55 rows
```

The coverage figure follows from the matrix itself: its first column names **105** distinct identifiers once the `/`-groups (`EE-POA-004/005/005a`, `EE-PID-007/008`, …) and the `–` ranges (`EE-SEC-001–005`, `EE-WUA-001–006`, …) are expanded, and every one of them is defined in §3–§20 — none is foreign to the requirement set. An earlier revision's count used an `[A-Z]{3}` area pattern that missed the two-letter `RP` area, undercounting the headline total by the six `EE-RP-001`–`EE-RP-006` identifiers (181 instead of 187) and, symmetrically, treating those six as outside the matrix's coverage rather than inside it. The pattern above matches two-, three- and four-letter areas and all 105 matrix identifiers, `EE-RP-*` included, are now counted in both figures: 187 − 105 = 82 identifiers are therefore still to be enumerated here.

Revisions 1.2 and 1.3 stated a coverage of 96 and then 98 identifiers against a remainder of 80. Expanding the first column of the 1.3 text gives 96 and a remainder of 82, so the pair was inconsistent in both revisions; the expansion is used here and the inherited pair is discarded.

Revisions up to 1.1 stated a total of 162. That figure was not reproducible by the count above, which yielded 156 for the same text; the 20 identifiers added in revision 1.2 brought the measured total to 176, the two added in revision 1.3 (EE-PRO-010a, EE-PRO-013) brought it to 178, and the three added in revision 1.4 (EE-SEC-024, EE-ZKP-022b, EE-ZKP-022c) brought it to 181 under the `[A-Z]{3}` pattern — and to **187** once the pattern is corrected to include the six two-letter-area `EE-RP-*` requirements that were always in the body text but never in the count. The measured figure is used here in preference to the inherited one.

| This document | ARF HLR / harmonized ID | Legal basis |
|---|---|---|
| EE-GOV-002 | — | Art. 5a(3) |
| EE-GOV-003 | — | Art. 5a(14); Art. 45h(3) |
| EE-GOV-010 | ARF 3.0.0 ch. 6 trust-anchor retrieval | CIR 2024/2980 |
| EE-GOV-012 | `CT_01`–`CT_06` (Topic 55) | — |
| EE-ONB-001 | `WIAM_01`–`WIAM_21` (Topic 40, 34 reqs) | Art. 5a(5)(d), 5a(24); CIR 2026/798 |
| EE-ONB-009 | `WURevocation_*` (Topic 38) | Art. 5a(9) |
| EE-ONB-010 | — | CIR 2025/847 |
| EE-PID-001 | `PID_01`–`PID_21` (Topic 3) | CIR 2024/2977; CIR 2024/2979 Annex II as replaced by CIR 2026/1731 |
| EE-PID-004 | `QTSPAS_*` (Topic 42) | Art. 45e; Annex VI |
| EE-PID-007/008 | `RPA_10`, `Reg_10d` (`AS-MS-27-016`); `Reg_01b` (`AS-MS-27-003`, divergence — §22 item 13) | Art. 5b(2)(c), 5b(3); GDPR Art. 5(1)(c) |
| EE-POA-001 | `ARB_*` (Topic 12) | Art. 45f; Annex VII |
| EE-POA-004/005/005a | — | Hasartmänguseadus § 37(8)–(10), § 39(8), § 53(1), § 89, § 94 |
| EE-POA-010/011 | `ISSU_61`, `ISSU_64` (Topic 10, Method A) | Art. 5a(16)(a) |
| EE-POA-011a/011b | `ISSU_61`; ARF §4.5 (WSCD capacity) | Art. 5a(16)(a) |
| EE-POA-012 | ARF §7.4.3.5.2 | Art. 5a(16)(b) |
| EE-POA-013 | `WIAM_21` | Art. 5a(16)(a) |
| EE-POA-014 | `ISSU_65` | — |
| EE-POA-017 | ARF §7.4.3.5.2; TS14 §5.9 | Art. 5a(5)(b) |
| EE-POA-022/023 | — | Art. 13, 17(3)(b), 19a, 45b(1)–(3); CIR (EU) 2025/2160 |
| EE-POA-024 | AV Profile Annex A (`eu.europa.ec.av.1`) | Art. 45b(3); §22 item 1 |
| EE-ZKP-001 | `ZKP_01`–`ZKP_09` (Topic 53) | Art. 5a(16)(b); Recital 14 |
| EE-ZKP-003 | `ZKP_08` (`EW-DM-53-007`) | — |
| EE-ZKP-020–025 | `ZKP_01`, `ZKP_02`, `ZKP_06` | — |
| EE-ZKP-022a | `ZKP_08` (`EW-DM-53-007`); ECCG ACM v2.0 | — ; §22 item 17 |
| EE-ZKP-022b | `ZKP_08` (`EW-DM-53-007`); ECCG ACM v2.0 §4.2, §4.3, §7, Note 1 to §1 | CIR 2024/2981 Annex IV §2(5), §3(4) (security target) |
| EE-ZKP-022c | `ZKP_08` (`EW-DM-53-007`); ECCG ACM v2.0 §1.1, §1.3 | — ; §22 item 19, §23 item 6 |
| EE-ZKP-030 (soundness level published) | `ZKP_08`; ARF Topic 55 | — |
| EE-ZKP-040/041 | `ZKP_05` | — |
| EE-ZKP-044–046 | `ZKP_05`; ISO/IEC DIS 18013-5 2nd ed. §8 (device retrieval) | — ; §22 item 18 |
| EE-ZKP-050–052 | AV Profile Annex A §A.6, §A.8, §A.9 (§10.8); §22 item 4 for EE-ZKP-052 | Art. 5a(4)(d) |
| EE-ZKP-053 | `DASH_*` (Topic 19) | Art. 5a(4)(d) |
| EE-ZKP-054–056 | `ZKP_07`; `Reg_*` (Topic 44, TS5/TS6) | Art. 5b(9); §22 item 15 |
| EE-PRO-001 | `OIA_*` (Topic 1) | CIR 2024/2982 as amended by CIR 2026/1731 |
| EE-PRO-010a/013 | `OIA_08c`, `OIA_08d` (Topic 1) | CIR 2026/1731 Annex XII → ETSI TS 119 472-2 `OIDFVP-HAIP-SUPPORT-03` |
| EE-PRO-012 | `QES_*` (Topic 16) | Art. 5a(5)(a)(xi) |
| EE-WUA-001–006 | `WUA_01`–`WUA_37` (Topic 9); TS3 v1.5.2 | CIR 2024/2979 |
| EE-REV-001 | `VCR_*` (Topic 7) | CIR 2024/2979 Annex 2 (via `VCR_11`); §22 item 12 |
| EE-RP-001–006 | `Reg_*` (Topic 27), `RPRC_*` (Topic 44), `RPI_*` (Topic 52) | Art. 5b; CIR 2025/848 as amended by 2026/1730 |
| EE-PRI-010/011 | `PA_01`–`PA_31` (Topic 11) | Art. 5a(4)(b), 5b(9); **CIR 2024/2979 Art. 14** |
| EE-PRI-017–019 | `PA_*` (Topic 11) | CIR 2024/2979 Art. 14, Annex V (WebAuthn L2, 8 Apr 2021); Art. 5(1), 5a(5)(a)(viii) |
| EE-PRI-013 | `EDP_01`–`EDP_11` (Topic 43) | Art. 5a(5)(e) |
| EE-PRI-014 | `DASH_*`, `DATA_DLT_*`, `RPT_DPA_*`; TS7, TS8 | Art. 5a(4)(d) |
| EE-PRI-016 | `Mig_*` (Topic 34); TS10 v1.2 | Art. 5a(4)(f)–(g) |
| EE-SIG-001 | `QES_01`–`QES_26` (Topic 16) | Art. 5a(5)(g), 5a(13) |
| EE-SIG-010–014 | `QES_*` (Topic 16) | Art. 26(c); ETSI TS 119 431-1 SIG-6.3.1-13; TS 119 101 §8.1.6 SCP 49–52; EN 419 241-2 `R.SAD` |
| EE-SIG-015/016 | — | Art. 3(24), 3(25), 35(2), 36(c), Annex II |
| EE-SIG-017/018 | `QES_*` (Topic 16) | Art. 26(c); ETSI TS 119 101 SCP 51; §23 item 27 |
| EE-SEC-001–005 | ARF §4.5; `WUA_16a` (`AS-WP-09-023`) | Art. 5a(5)(d), 5a(14) |
| EE-SEC-001a/b/c | ARF §4.5; `ZKP_07` (Topic 53) | Art. 5a(4)(d), 5a(14), 5a(16)(a); §22 item 16 |
| EE-PRI-018a/018b | `PA_*` (Topic 11); `WUA_*` (Topic 9, EE-WUA-004) | CIR 2024/2979 Art. 14, Annex V; Art. 5a(5)(a)(viii) |
| EE-CNF-006/007 | `ZKP_05`, `ZKP_07` | Art. 5c; CIR 2024/2981 |
| EE-SEC-010–012, EE-SEC-023 | ARF ch. 7; §7.5 FCAF | Art. 5c(2)–(6); CIR 2024/2981 Art. 4, 21 |
| EE-SEC-013, EE-SEC-020–022 | — | CIR 2024/2981 Annex IV §2, §3, §5; Art. 13(1)(g); CIR 2015/1502 Annex §2.2.1 |
| EE-SEC-024 | ARF ch. 7 (WSCA critical operations, §4.5) | CIR 2024/2981 Annex IV §3(2), Annex VIII; Art. 13(1)(g); §23 item 29 |
| EE-ACC-001 | `ACC_01`, `ACC_02` (Topic 54); ARF ch. 8 (standard version — §22 item 14) | Art. 5a(21); Dir. (EU) 2019/882 |

---

## Annex F — Normative and informative references

**Union law.** Reg. (EU) No 910/2014 as amended by Reg. (EU) 2024/1183 · Reg. (EU) 2019/881 (CSA) Art. 57(1) · CIR (EU) 2015/1502 · CIR (EU) 2024/482 (EUCC) · CIR (EU) 2024/2977, 2024/2979, 2024/2980, 2024/2981, 2024/2982 · CIR (EU) 2025/846, 2025/847, 2025/848, 2025/849, 2025/1566–1572, **2025/2160 (27 Oct 2025, non-qualified trust services risk management)** · CIR (EU) 2026/248, 2026/798, 2026/1730, 2026/1731, 2026/1735 · Reg. (EU) 2022/2065 (DSA) Art. 28 · Commission Guidelines on Art. 28(1) DSA, C/2025/5519, 14 July 2025 · Commission Recommendation (EU) 2026/1035 of 29 April 2026 · Dir. (EU) 2019/882 · Reg. (EU) 2016/679 (GDPR)

**Estonian law.** E-identimise ja e-tehingute usaldusteenuste seadus (EUTS) · Isikut tõendavate dokumentide seadus (ITDS) · Isikuandmete kaitse seadus · Küberturvalisuse seadus · Rahvastikuregistri seadus · Avaliku teabe seadus · Alkoholiseadus · Tubakaseadus · Hasartmänguseadus · Riigilõivuseadus · Karistusseadustik · EVS 585:2007

**EU technical.** EUDI ARF v3.0.0 (2026-07-21), read at commit `6373eee` (2026-07-23), `https://eudi.dev` · ARF Annex 2 (725 HLRs, 34 topics) · PID Rulebook v1.7 (2026-07-17) and mDL Rulebook, attestation-rulebooks-catalog · TS1 v1.2, TS3 v1.5.2, TS4 v1.0.1, TS5 v1.5, TS6 v1.2.2, TS7 v1.0, TS8 v1.0, TS9 v1.1, TS10 v1.2, TS11 v1.0.1, TS12 v1.0.1, **TS13 v1.0.1**, **TS14 v1.0** · EU Age Verification Technical Specification and Annexes A and B, `https://ageverification.dev` · FCAF, `https://conformance.eudi.dev`

**Standards.** OpenID4VCI 1.0 · OpenID4VP 1.0 · OpenID4VC HAIP 1.0 · RFC 9901 (SD-JWT) · `draft-ietf-oauth-sd-jwt-vc-19` · `draft-ietf-oauth-status-list-21` · `draft-ietf-oauth-attestation-based-client-auth-10` · RFC 9449 (DPoP) · RFC 9101 (JAR) · RFC 9207 · RFC 10027 / BCP 247 · ISO/IEC 18013-5:2021 and DIS 2nd ed. · ISO/IEC TS 18013-7:2025 · ISO/IEC TS 23220-2:2026, -3:2026, -4:2026, -6:2025 · ISO/IEC 27566-1:2025 · IEEE 2089.1 · W3C VCDM 2.0 · W3C Digital Credentials API (WD 2026-08-27) · ETSI TS 119 461, 119 471, 119 472-1/-2/-3, 119 475, 119 478, 119 602, 119 612, 119 615, **119 101 v1.1.1 (2016-03)**, **119 431-1 v1.3.1 (2024-12)**, 119 432 **v1.3.1 (2026-03)**, 119 172-4 · **EN 419 241-1 and -2** (not read; see §23 item 22) · **EN ISO/IEC 15408-3:2022** · **EN 17640:2018 (FITCEM)** · **EN ISO/IEC 17065:2012**, **EN ISO/IEC 17067:2013** · **W3C WebAuthn Level 2 (Rec. 8 Apr 2021)**; Level 3 (Rec. 25 Aug 2026, not the Annex V reference) · **ETSI TS 119 476-2 (early draft v0.0.4, 2026-07-08, STF 705)** · CEN/TS 18098 (ratified 2026-03-17) · EN 301 549 v3.2.1 · ECCG *Agreed Cryptographic Mechanisms* v2.0

**Cryptographic literature.** Frigo & Shelat, *Anonymous Credentials from ECDSA*, IACR Communications in Cryptology 3(1), 4 May 2026 (ePrint 2024/2010) · `draft-google-cfrg-libzk-02` · `draft-irtf-cfrg-bbs-signatures-10`, `-bbs-blind-signatures-03`, `-bbs-per-verifier-linkability-03` · `draft-cllz-cfrg-ecdsa-pop-00` (Celi, Lehmann, Levin, Zacharakis, 2026-07-02) and ePrint 2026/965 · Desmoulins, Dumanois, Kane & Traoré, *Making BBS Anonymous Credentials eIDAS 2.0 Compliant*, ePrint 2025/619 (BBS#) · Chairattana-Apirom, Harding, Lysyanskaya & Tessaro, *Server-Aided Anonymous Credentials*, ePrint 2025/513 · Paquin, Policharla & Zaverucha, *Crescent*, ePrint 2024/2013 · Ames, Hazay, Ishai & Venkitasubramaniam, *Ligero*, CCS 2017 · *Cryptographers' Feedback on the EU Digital Identity's ARF*, 19 June 2024

**Reviews and analyses.** Trail of Bits review of longfellow-zk (Aug 2025) · ISRG review (Oct 2025), finding ISRG-01 fixed in v0.8.4 · Ligero academic panel review (Dec 2025) · EDPB Statement 1/2025 on Age Assurance (11 Feb 2025) · Meeco, *EUDI Large Scale Pilots — Insights & Recommendations* (Jan 2026)

**Estonian sources.** RIA digikukkur pages and EUDI Wallet project pages · RIA procurement announcement, 18 May 2026 · id.ee (ID-card documentation, Thales transition, Mobile-ID SIM replacement, DigiDoc/SiVa/CDOC2) · repository.eidpki.ee (Zetes certificate profiles) · skidsolutions.eu (Smart-ID+, RP API v3) · x-road.global and NIIS · Riigi Teataja · TTJA age-verification guidance · Eesti äpp product pages

---

*Prepared 5 September 2026; revised 7 September 2026, and again on 12 September 2026 for §9.2–§9.7, §10.4, §10.8, §13, §21.1, §22, §23 and Annexes A, B and E. All version numbers, dates and identifiers verified against primary sources on 3–5 September 2026 and, for v1.1 material, on 7 September 2026, except where marked UNVERIFIED in §23. This is an independent technical specification and carries no official status.*
