# Extract — `spec/EE-EUDIW-TS-1.0.md`

Extraction for the PRD of this fork. Source: `/home/notroot/Documents/eudi-wallet-poc/spec/EE-EUDIW-TS-1.0.md`, read programmatically (1695 lines / 283,970 bytes); every requirement row below is reproduced from the document, not sampled. Requirement count **211**, reproducing the document's own checkable figure (its Annex E gives `grep -oE '\*\*EE-[A-Z]{2,4}-[0-9]{3}[a-z]? \(' spec/EE-EUDIW-TS-1.0.md | sort -u | wc -l  # 211 requirements`; my parse returns the same 211 unique identifiers).

Rules applied in this extract: modal verbs are copied verbatim (`SHALL`, `SHALL NOT`, `SHOULD`, `MAY`); identifiers are quoted exactly; anything the document marks open, `[UNVERIFIED]` or awaiting an authority is recorded as open; `[REC]` items are flagged as recommendations to authorities, not product requirements.

---

## 1. Spec identity and structure

| | |
|---|---|
| Title | *EE-EUDIW — Estonian European Digital Identity Wallet*, subtitle "Technical Specification, with a Zero-Knowledge Proof of Age Profile" |
| Version / date | **1.7 (draft for review)**, 12 October 2026. Predecessors: v1.0 5 Sep 2026, v1.1 7 Sep, v1.2 12 Sep, v1.3 16 Sep, v1.4 17 Sep, v1.5 11 Oct, v1.6 12 Oct 2026 |
| Target jurisdiction | Republic of Estonia |
| Status | **Independent technical specification. Not issued by RIA, the Ministry of Justice and Digital Affairs, or any Estonian public authority.** It therefore cannot bind an authority; see §0.1 of the spec |
| Baseline | EUDI ARF **v3.0.0** (tagged 2026-07-21, read at commit `6373eee`, 2026-07-23); Reg. (EU) 910/2014 as amended by (EU) 2024/1183; CIR 2024/2977, 2024/2979, 2024/2980, 2024/2981, 2024/2982 (as amended by 2026/1731); CIR 2025/846–849 (848 as amended by 2026/1730); CIR 2025/2160; CIR 2026/798; CIR 2015/1502; CIR 2024/482 |
| Requirement language | BCP 14 (RFC 2119/8174) key words, capitals only. Identifiers of the form `EE-<AREA>-<nnn>`, with letter suffixes (`EE-POA-005a`, `EE-ZKP-022b`, `EE-SEC-001c`, `EE-ZKP-055a`) |
| Areas (14) | `GOV` governance · `ONB` onboarding · `PID` · `POA` proof of age · `ZKP` · `PRO` protocols · `WUA` · `REV` revocation · `RP` relying party · `SIG` signing · `PRI` privacy · `SEC` security · `ACC` accessibility · `CNF` conformance |
| Addressee convention | A requirement or clause addressed to RIA **as an authority**, the Registrar, the national certification-scheme owner or "Estonia" is a **`[REC]`** recommendation, marked after the identifier. `[REC]` items still count in the 211 but **do not count toward wallet conformance**. RIA in its *operational* roles (Wallet Provider, PID Provider, EE-PoA Provider) is *not* `[REC]` |
| Traceability | Annex E is the matrix: **68 rows covering 126 of the 211 identifiers; 85 remain to be enumerated**. 28 of the 211 are `[REC]` — 22 wholly, 6 in a named clause only; wallet conformance is assessed against the remaining **189** identifiers plus the non-`[REC]` clauses of those six |

### Sections

§0 About this document (0.1 Purpose · 0.2 Requirement language · 0.3 Evidence discipline · 0.4 What is deliberately not here) · §1 Executive summary · §2 Regulatory baseline (2.1 Union law · 2.2 Estonian law · 2.3 The ARF and the numbered Technical Specifications) · §3 Roles and the Estonian institutional mapping (3.1 Relationship to Eesti äpp) · §4 Architecture (4.1 Components · 4.2 WSCD strategy for Estonia · 4.3 Data separation) · §5 Trust infrastructure (5.1 Trust anchors · 5.2 Relying party registration and access certificates · 5.3 Certificate transparency) · §6 Onboarding and activation (6.1 Assurance · 6.2 The November 2025 discontinuity · 6.3 e-Residents · 6.4 Wallet unit activation and revocation) · §7 Person Identification Data (7.1 Format and identifiers · 7.2 Attributes · 7.3 Sourcing · 7.4 The `isikukood` problem) · §8 Attestation catalogue for Estonia · §9 The Estonian Proof-of-Age attestation (EE-PoA) (9.1–9.7, 9.7.1 The price of option (a), stated) · §10 Zero-knowledge proof profile (10.1–10.9) · §11 Protocols (11.1 Baseline · 11.2 The 18013-7 Annex B version gap · 11.3 Cross-device flows · 11.4 Transaction data and QES · 11.5 Issuance specifics) · §12 Wallet Unit Attestation · §13 Revocation and status · §14 Pseudonyms, consent, and disclosure policies (14.1, 14.2) · §15 Signing with the wallet (15.1 Mass signing, 15.1.1, 15.1.2) · §16 Privacy and data protection (16.1) · §17 Security and certification (17.1, 17.1.1, 17.2) · §18 Accessibility and inclusion · §19 Coexistence and migration · §20 Interoperability and conformance (20.1 Conformance profiles) · §21 Roadmap and dependencies (21.1 Risk register extract) · §22 Deliberate divergences from the EU baseline · §23 Open issues and unverified claims.

### Annexes

**A** Estonian statutory age thresholds · **B** DCQL request examples (B.1 age-check-only ZKP-required · B.2 cross-border fallback to the EU AV attestation · B.3 anti-pattern: gambling premises access · B.4 anti-pattern: what a relying party SHALL NOT send for an age check) · **C** EE-PoA CDDL sketch (informative) · **D** The `isikukood`, and why it must not appear in an age proof (D.1 structure EVS 585:2007, D.2 why decisive, D.3 what a ZK proof does not fix) · **E** Traceability matrix (extract) · **F** Normative and informative references.

### What the document is authoritative for

For an implementer, it is authoritative for the **Estonian wallet solution's own obligations and for the artefacts that solution must produce or consume** — the EE-PoA attestation and its data model, the ZKP profile, the protocol baseline, WUA handling, revocation, pseudonym and consent surfaces, signing, accessibility, and the conformance profiles. It also states obligations it places on issuers, relying parties and a Registrar, and it recommends (never binds) measures to Estonian authorities. It is explicitly **not** a procurement document, cost model, service design or DPIA; it does not specify the relying-party register's schema or the operator's SRE practices, though it does specify the interfaces where those meet the wallet (§0.4).

### Revision-history signals (from the header table and Annex E)

Requirement count trajectory: 162 (v1.1) → 182 (1.2) → 184 (1.3) → 187 (1.4) → 193 (1.5) → 211 (1.6, unchanged in 1.7). Revision 1.7 "closes the two items revision 1.6 left open" and adds no requirement. Corrections recorded across revisions include: the gambling entry-identification duty (HasMS § 37(8)–(10)) in 1.2, reversing a contrary 1.1 assertion; the ACM floor being **125 bits** for *recommended* mechanisms rather than the earlier "128-bit floor"; the ARF Topic G refinement round sitting in **Iteration 6 (23 Sep – 18 Nov 2026)**; and Annex E's own counting-rule errors (the two-letter `RP` area was excluded by a three-letter regex). Version markers show the document is still under active adversarial review, with `[UNVERIFIED]` markers inline and collected in §23.

---

## 2. Requirement families

Ranges below are **document order, not contiguous**: `EE-ZKP-*` runs to `-062` with 38 of the numbers used; `EE-POA-*` to `-032` with 35 used.

| Family | Subject | First … last id (document order) | Count | Modal split | Principal binding targets | Where |
|---|---|---|---|---|---|---|
| `EE-GOV-*` | Governance, role assignment, attestation catalogue, coexistence | EE-GOV-001 … 14 ids | 14 | SHALL 9, SHALL NOT 1, SHOULD 4 | authority (Estonia / Wallet Provider) (8); issuer (attestation Provider) (2); relying party (1) | §19, §3, §5, §8 |
| `EE-SEC-*` | Security, architecture, certification | EE-SEC-001 … 18 ids | 18 | SHALL 16, SHALL NOT 1, SHOULD 1 | wallet / wallet solution (15); national certification-scheme owner [REC] (2); WSCD provider (1) | §17, §4 |
| `EE-RP-*` | Relying-party registration and access certificates | EE-RP-001 … 6 ids | 6 | SHALL 5, SHOULD 1 | relying party (3); Registrar [REC] (1); wallet (1) | §5 |
| `EE-ONB-*` | Onboarding and activation | EE-ONB-001 … 11 ids | 11 | SHALL 8, SHALL NOT 1, SHOULD 1, MAY 1 | wallet (8); issuer (EE-PoA Attestation Rulebook / Provider) (1); issuer (EE-PoA Provider, e-residency case) (1) | §6 |
| `EE-PID-*` | Person Identification Data | EE-PID-001 … 9 ids | 9 | SHALL 6, SHALL NOT 3 | issuer (PID Provider) (6); wallet + relying party (1); wallet + issuer (age-verification code paths) (1) | §7 |
| `EE-PRI-*` | Privacy, pseudonyms, consent, disclosure policies | EE-PRI-001 … 18 ids | 18 | SHALL 15, SHALL NOT 1, SHOULD 2 | wallet (11); wallet / programme (Wallet Provider) (4); authority (Estonia) [REC] (1) | §14, §16, §7 |
| `EE-POA-*` | Estonian Proof-of-Age (EE-PoA) | EE-POA-001 … 35 ids | 35 | SHALL 33, SHALL NOT 2 | issuer (EE-PoA Provider) / data model (9); wallet (8); issuer (EE-PoA Provider) (6) | §9 |
| `EE-ZKP-*` | Zero-knowledge proof profile | EE-ZKP-001 … 38 ids | 38 | SHALL 33, SHOULD 5 | wallet (26); authority (Estonia) [REC] (3); RIA (circuit-registry operator) [REC] (3) | §10 |
| `EE-PRO-*` | Protocols (OpenID4VCI/VP/HAIP, ISO 18013-5) | EE-PRO-001 … 20 ids | 20 | SHALL 17, SHALL NOT 1, SHOULD 2 | wallet (12); wallet + issuer (3); relying party + wallet (2) | §11 |
| `EE-WUA-*` | Wallet Unit Attestation | EE-WUA-001 … 8 ids | 8 | SHALL 6, SHALL NOT 2 | Wallet Provider (WUA issuance) (4); wallet + Wallet Provider (2); Wallet Provider + wallet (no RP presentation) (1) | §12 |
| `EE-REV-*` | Revocation and status | EE-REV-001 … 7 ids | 7 | SHALL 6, SHALL NOT 1 | issuer (Provider) (3); wallet + relying party (1); relying party (1) | §13 |
| `EE-SIG-*` | Signing and QES | EE-SIG-001 … 16 ids | 16 | SHALL 11, SHALL NOT 1, SHOULD 4 | wallet (12); wallet / deploying organisation (2); remote signing service (issuer-side) (1) | §15 |
| `EE-ACC-*` | Accessibility and inclusion | EE-ACC-001 … 4 ids | 4 | SHALL 3, SHOULD 1 | wallet (4) | §18 |
| `EE-CNF-*` | Interoperability and conformance | EE-CNF-001 … 7 ids | 7 | SHALL 6, SHOULD 1 | wallet instance (5); authority (Estonia) [REC] (2) | §20 |

---

## 3. Full requirement index (211 rows)

Modal verbs and identifiers reproduced exactly. "Binding target (as addressed)" is the requirement's own subject matter, read from its text; where a requirement carries an `[REC]` clause the non-`[REC]` clause is named first.

| # | Identifier | Modal verb | Binding target (as addressed in the text) | Subject (one line) |
|---|---|---|---|---|
| 1 | `EE-GOV-001` | SHALL | authority (Estonia / Wallet Provider) | The Wallet Provider SHALL be a public body of the Republic of Estonia. |
| 2 | `EE-GOV-002` | SHALL | authority (Estonia / Wallet Provider) | All application software components installed on user devices SHALL be published under an OSI-approved licence, per Art. 5a(3). |
| 3 | `EE-GOV-003` | SHALL NOT | authority (Estonia / Wallet Provider) | The Wallet Provider and the Operator SHALL NOT combine personal data processed for wallet provision with data from any other service they provide,… |
| 4 | `EE-GOV-004` | SHOULD | authority (Estonia / Wallet Provider) | Eesti äpp SHOULD be positioned as a relying party of the EE-EUDIW rather than as a parallel credential surface, and its `isikukood` barcode functio… |
| 5 | `EE-SEC-001` | SHALL | wallet / wallet solution | The EE-EUDIW SHALL support **local native** (Android StrongBox / iOS Secure Enclave) as the default WSCD architecture in production. |
| 6 | `EE-SEC-001a` | SHALL | wallet / wallet solution | The remote-HSM WSCD SHALL be offered as a **fallback**, never as the default on a device whose local-native WSCD meets §12, and the wallet SHALL NO… |
| 7 | `EE-SEC-001b` | SHALL | wallet / wallet solution | Before a user is activated on a remote-HSM WSCD, the wallet SHALL disclose in plain language that the WSCD provider will learn the time of every pr… |
| 8 | `EE-SEC-001c` | SHALL | WSCD provider | The remote WSCD provider SHALL retain WSCD invocation records only as long as is necessary for security incident investigation, SHALL NOT retain re… |
| 9 | `EE-SEC-002` | SHALL | wallet / wallet solution | A single physical component MAY implement both a QSCD and a WSCD, but SHALL be certified as both. |
| 10 | `EE-SEC-003` | SHALL NOT | wallet / wallet solution | A WSCA SHALL NOT enable export of private keys from a WSCD (ARF `WUA_16a` / `AS-WP-09-023`). |
| 11 | `EE-SEC-004` | SHOULD | wallet / wallet solution | Where the local-native WSCD is used, the wallet SHOULD consume platform key attestation (Android Key/ID Attestation with StrongBox where available;… |
| 12 | `EE-SEC-005` | SHALL | wallet / wallet solution | The wallet SHALL logically separate personal data used for wallet provision from data used for any other service, per Art. 5a(14), and the separati… |
| 13 | `EE-GOV-010` | SHALL | relying party | Relying parties SHALL retrieve trust anchors from both the LoTE and the Trusted List, per ARF 3.0.0's new dual-retrieval requirement, and SHALL val… |
| 14 | `EE-GOV-011` | SHOULD | authority (Estonia / Wallet Provider) | For mdoc proximity presentation, the Estonian ecosystem SHOULD publish IACA trust anchors through a **VICAL** (ISO/IEC 18013-5 Annex C) in addition… |
| 15 | `EE-RP-001` | SHALL | Registrar [REC] | The Estonian Registrar SHALL implement TS5 and TS6, SHALL publish current *and historic* Wallet-Relying Party Access Certificates (TS5 Annex II.2.1… |
| 16 | `EE-RP-002` | SHALL | relying party | Each relying party SHALL register its **intended use, including an indication of the data to be requested** (Art. 5b(2)(c)) and SHALL NOT request d… |
| 17 | `EE-RP-003` | SHALL | wallet | The wallet SHALL display, before user approval, the relying party's and the service's trade names from the access certificate, a user-friendly desc… |
| 18 | `EE-RP-004` | SHOULD | authority (Estonia) [REC] | Estonia SHOULD issue **Registration Certificates** (Topic 44, `RPRC_*`) rather than relying solely on dynamic register lookup. |
| 19 | `EE-RP-005` | SHALL | relying party | Relying-party intermediaries are deemed relying parties (Art. 5b(10)) and SHALL NOT store transaction content data. |
| 20 | `EE-RP-006` | SHALL | relying party | Relying parties SHALL NOT refuse a pseudonym where identification is not required by law (Art. 5b(9)). |
| 21 | `EE-GOV-012` | SHOULD | authority (Estonia / Wallet Provider) | Estonia SHOULD implement ARF Topic 55 (Certificate Transparency, `CT_01`–`CT_06`) for access certificates and for the ZK circuit registry defined i… |
| 22 | `EE-ONB-001` | SHALL | wallet | Wallet activation SHALL be performed using an eID means within Estonia's notified LoA High scheme, or using an onboarding procedure conforming to C… |
| 23 | `EE-ONB-002` | MAY | wallet | Smart-ID MAY be used for activation only if (a) Estonia notifies it under Art. 9, or (b) it is used as the LoA-substantial component of a CIR 2026/… |
| 24 | `EE-ONB-003` | SHALL | wallet | The wallet SHALL support ID-card activation over NFC using the card's PACE/CAN path in addition to a contact reader, and SHALL support both the leg… |
| 25 | `EE-ONB-004` | SHALL | wallet | Activation SHALL verify the card certificate against the correct chain and its OCSP responder, and SHALL NOT assume a single issuer. |
| 26 | `EE-ONB-005` | SHALL | wallet | Activation SHALL NOT copy the certificate `serialNumber` (`PNOEE-<isikukood>`) into any attestation subject identifier other than the PID's `person… |
| 27 | `EE-ONB-006` | SHOULD | wallet | The EE-EUDIW SHOULD support e-Residency Digi-ID activation, and the resulting PID SHOULD carry an `attestation_legal_category` or equivalent marker… |
| 28 | `EE-ONB-011` | SHALL | issuer (EE-PoA Attestation Rulebook / Provider) | The EE-PoA Attestation Rulebook SHALL name the authoritative date-of-birth source for e-residents. |
| 29 | `EE-ONB-007` | SHALL NOT | issuer (EE-PoA Provider, e-residency case) | An e-Residency-derived PID SHALL NOT be used to satisfy an EE-PoA issuance whose statutory basis requires residence. |
| 30 | `EE-ONB-008` | SHALL | wallet | Activation SHALL conform to ARF Topic 40 (34 requirements, numbered `WIAM_01`–`WIAM_21` with letter suffixes), including the WSCD-related requireme… |
| 31 | `EE-ONB-009` | SHALL | wallet | The wallet SHALL be revocable on explicit user request, on security compromise, and on death or cessation (Art. 5a(9)), per ARF Topic 38 (`WURevoca… |
| 32 | `EE-ONB-010` | SHALL | authority (Estonia) [REC] | Estonia SHALL implement CIR 2025/847: on a security breach of the wallet, the Member State suspends provision and use **without undue delay**. |
| 33 | `EE-PID-001` | SHALL | issuer (PID Provider) | The Estonian PID SHALL be issued in **both** encodings — ISO/IEC 18013-5 mdoc and IETF SD-JWT VC — per CIR 2024/2979 Annex II as replaced by CIR 20… |
| 34 | `EE-PID-002` | SHALL | issuer (PID Provider) | The Estonian PID SHALL carry `personal_administrative_number` = the `isikukood`, and `issuing_country` = `EE`. |
| 35 | `EE-PID-003` | SHALL NOT | wallet + relying party | The Estonian PID SHALL NOT be presented to satisfy an age check. |
| 36 | `EE-PID-004` | SHALL | issuer (PID Provider) | PID attributes SHALL be sourced from the **Population Register** (authoritative for person data; controller Ministry of the Interior, processor SMI… |
| 37 | `EE-PID-005` | SHALL | issuer (PID Provider) | Portrait retrieval SHALL go through the documents database. Biometric templates in **ABIS** SHALL NOT be exposed to the wallet or to relying partie… |
| 38 | `EE-PID-006` | SHALL | issuer (PID Provider) | X-tee queries made for PID issuance SHALL be visible to the data subject through **Andmejälgija**, the existing data-tracker surface, in addition t… |
| 39 | `EE-PID-009` | SHALL | issuer (PID Provider) | A change in the Population Register to any attribute a PID carries, including `personal_administrative_number`, SHALL cause the PID Provider to rev… |
| 40 | `EE-PID-007` | SHALL NOT | wallet + issuer (age-verification code paths) | No EE-PoA attestation, ZK proof, pseudonym, key identifier, device-binding value, salt, or transport identifier used in an age-verification flow SH… |
| 41 | `EE-PID-008` | SHALL NOT | relying party [REC clause: Registrar] | A relying party performing only an age check SHALL NOT request `personal_administrative_number`, `birth_date`, `sex`, `portrait`, or any PID attrib… |
| 42 | `EE-PRI-001` | SHOULD | authority (Estonia) [REC] | Estonia SHOULD treat the wallet rollout as the occasion to begin retiring `isikukood` as a *proof* of anything, reducing it to what it actually is… |
| 43 | `EE-GOV-020` | SHALL | issuer (attestation Provider) | The mDL SHALL be issued as ISO/IEC 18013-5 mdoc only. The mDL Rulebook states that mDLs issued to a Wallet Unit SHALL NOT be implemented as SD-JWT VC. |
| 44 | `EE-GOV-021` | SHALL | issuer (attestation Provider) | Every Estonian attestation SHALL have a published Attestation Rulebook conforming to the ARF Attestation Rulebook template and registered in the Co… |
| 45 | `EE-POA-001` | SHALL | issuer (EE-PoA Provider) / data model | The EE-PoA SHALL contain **only** boolean age predicates and the three metadata attributes above. |
| 46 | `EE-POA-002` | SHALL | relying party + wallet | The EE-PoA SHALL be presentable in full. A relying party SHALL request only the predicates it is legally entitled to, and the wallet SHALL enforce… |
| 47 | `EE-POA-030` | SHALL | wallet (with relying-party query limits) | A presentation of an EE-PoA or of `eu.europa.ec.av.1` SHALL disclose at most one `age_over_NN` predicate. |
| 48 | `EE-POA-031` | SHALL | wallet (with relying-party query limits) | Every predicate SHALL be issued with its true value. Where the predicate a request asks for is held as `false`, the wallet SHALL show this to the u… |
| 49 | `EE-POA-003` | SHALL | wallet | The wallet SHALL also support the EU AV Profile attestation `eu.europa.ec.av.1` with its single mandatory attribute `age_over_18` (boolean), for cr… |
| 50 | `EE-POA-004` | SHALL | relying party (gambling operators) | Remote gambling SHALL be treated as an **identification** use case, not an age-proof use case. |
| 51 | `EE-POA-005` | SHALL NOT | relying party (gambling operators) | The `age_over_21` predicate SHALL NOT be used to satisfy HasMS § 53(1) player registration, and SHALL NOT be used to admit a person to a gaming loc… |
| 52 | `EE-POA-005a` | SHALL | Registrar [REC] | Where a relying party registers an intended use citing any provision of Hasartmänguseadus, the Registrar SHALL refuse registration of an EE-PoA pre… |
| 53 | `EE-POA-006` | SHALL | issuer (EE-PoA Provider) / data model | EE-PoA age predicates SHALL be computed **server-side by the PID/PoA Provider from the Population Register date of birth retrieved over X-tee**. |
| 54 | `EE-POA-007` | SHALL | issuer (EE-PoA Provider) | Where onboarding used a document read (NFC passport or ID-card chip), the Provider SHALL perform full **passive authentication** — SOD signature va… |
| 55 | `EE-POA-008` | SHALL | issuer (EE-PoA Provider) | The Provider SHALL re-verify the register value at each re-issuance and SHALL NOT re-sign a cached predicate. |
| 56 | `EE-POA-027` | SHALL | issuer (EE-PoA Provider) | A predicate `age_over_NN` SHALL evaluate to `true` from 00:00 Europe/Tallinn time on the NN-th anniversary of the register date of birth. |
| 57 | `EE-POA-028` | SHALL | issuer (EE-PoA Provider) | Where the register date of birth is incomplete, the Provider SHALL compute predicates from the latest date consistent with it: a year alone as 31 D… |
| 58 | `EE-POA-010` | SHALL | issuer (EE-PoA Provider) / data model | EE-PoA SHALL be issued as a batch of single-use technical attestations (ARF Method A), each bound to a distinct key pair, using OpenID4VCI 1.0 batc… |
| 59 | `EE-POA-011` | SHALL | issuer (EE-PoA Provider) / data model | Batch size SHALL be at least **30**, **except on a device whose WSCD cannot sustain 30 keys, where EE-POA-011b governs and sets the batch size inst… |
| 60 | `EE-POA-011a` | SHALL | wallet | Batch key generation SHALL NOT occur on the presentation critical path. |
| 61 | `EE-POA-011b` | SHALL | wallet | The wallet SHALL treat WSCD key-slot capacity as a bounded resource, and SHALL destroy the key material of consumed attestations under EE-POA-013 s… |
| 62 | `EE-POA-011c` | SHALL | wallet | A refill that the low-water mark of EE-POA-011a makes due SHALL NOT start in the same session as the presentation that crossed the mark, nor at a f… |
| 63 | `EE-POA-012` | SHALL | issuer (EE-PoA Provider) / data model | All attestations in a batch SHALL carry **identical `ValidityInfo` timestamps** — `signed`, `validFrom` and `validUntil` coarsened so that hour, mi… |
| 64 | `EE-POA-026` | SHALL | issuer (EE-PoA Provider) / data model | `ValidityInfo` SHALL be coarsened beyond the batch to a common **7-day epoch** across all holders: `validFrom` rounded down, and `validUntil` round… |
| 65 | `EE-POA-013` | SHALL | wallet | A plain-mdoc (non-ZKP) EE-PoA presentation SHALL consume the attestation: the wallet SHALL delete it, SHALL destroy the corresponding WSCD key mate… |
| 66 | `EE-POA-014` | SHALL | wallet + issuer (re-issuance) | Re-issuance SHALL go to the same Wallet Unit, using a device-bound refresh token per OpenID4VCI §14.5 (ARF `ISSU_65`). |
| 67 | `EE-POA-025` | SHALL | wallet | The wallet SHALL start re-issuance no later than **14 days** before the `validUntil` of the attestations it holds, independently of the low-water m… |
| 68 | `EE-POA-015` | SHALL | wallet | The wallet SHALL log each re-issuance event in the user-visible dashboard. |
| 69 | `EE-POA-032` | SHALL | issuer (EE-PoA Provider) | The Provider SHALL issue EE-PoA to at most one Wallet Unit per person at a time. |
| 70 | `EE-POA-016` | SHALL | issuer (EE-PoA Provider) / data model | EE-PoA validity SHALL NOT exceed **3 months**, and the user SHALL be re-identified at least every 3 months as a condition of re-issuance. |
| 71 | `EE-POA-017` | SHALL NOT | issuer (EE-PoA Provider) / data model | EE-PoA SHALL NOT use a status list, an OCSP-style check, or ISO/IEC 18013-5 server retrieval. |
| 72 | `EE-POA-018` | SHALL | wallet / programme (risk register) | The residual risk — an EE-PoA remaining valid for up to 3 months after the underlying entitlement changes — SHALL be documented in the risk register. |
| 73 | `EE-POA-029` | SHALL | wallet | The Provider SHALL NOT derive `validUntil` from the date of birth. |
| 74 | `EE-POA-019` | SHALL | wallet + issuer (no server retrieval) | ISO/IEC 18013-5 **server retrieval SHALL NOT be used** for any Estonian attestation, PoA or otherwise. |
| 75 | `EE-POA-020` | SHALL | issuer (EE-PoA Provider) / data model | The EE-PoA SHALL be issued as **option (a)** — a non-qualified electronic attestation of attributes issued by a public sector body, published on an… |
| 76 | `EE-POA-021` | SHALL | relying party (qualified case) | Where a relying party has a legal requirement for a *qualified* age attestation, it SHALL request a PID presentation with `birth_date` under a regi… |
| 77 | `EE-POA-022` | SHALL | issuer (EE-PoA Provider) | The PoA Provider SHALL implement Art. 19a(1) and CIR (EU) 2025/2160, and the risk-management policy SHALL name the EE-PoA service explicitly rather… |
| 78 | `EE-POA-023` | SHALL | wallet + issuer (EE-PoA Provider) | This specification SHALL NOT claim Art. 45b(2) paper-equivalence or Art. 45b(3) cross-border recognition for the EE-PoA. The Attestation Rulebook S… |
| 79 | `EE-POA-024` | SHALL | wallet + issuer (EE-PoA Provider) | Because the EE-PoA's cross-border standing is unresolved, the **EU AV Profile attestation `eu.europa.ec.av.1` of EE-POA-003 SHALL be the designated… |
| 80 | `EE-ZKP-001` | SHALL | wallet | The EE-EUDIW SHALL implement a ZKP presentation path for EE-PoA and for the EU AV Profile attestation, conforming to this section. |
| 81 | `EE-ZKP-002` | SHALL | wallet | The wallet SHALL NOT claim, in its user interface, its documentation, or any conformity statement, that presentations are unlinkable by the attesta… |
| 82 | `EE-ZKP-003` | SHALL | wallet | The wallet's conformity documentation SHALL state the ZKP_08 position explicitly and conditionally: which primitives are used, which are in ACM v2.… |
| 83 | `EE-ZKP-004` | SHALL | wallet | The wallet SHALL be **crypto-agile**: the ZKP scheme SHALL be a replaceable module behind a stable internal interface, with scheme identity carried… |
| 84 | `EE-ZKP-010` | SHALL | wallet | The EE-EUDIW SHALL implement **Track A** as its initial ZKP path. |
| 85 | `EE-ZKP-011` | SHOULD | wallet | The EE-EUDIW SHOULD implement **Track B** as a second scheme once ETSI TS 119 476-2 stabilises, and SHALL treat Track A as insufficient for ZKP_03,… |
| 86 | `EE-ZKP-020` | SHALL | wallet | The Track A scheme SHALL be `longfellow-libzk-v1` as defined in Frigo & Shelat (IACR CiC 3(1), 2026; ePrint 2024/2010) and specified in `draft-goog… |
| 87 | `EE-ZKP-021` | SHALL | wallet | The proof SHALL establish, without revealing the credential: (a) a valid issuer signature over the MSO; (b) presence of the requested attribute wit… |
| 88 | `EE-ZKP-022` | SHALL | wallet | The implementation SHALL be **`longfellow-zk` v0.9 (31 March 2026) or later**, or a bit-compatible implementation. |
| 89 | `EE-ZKP-022a` | SHALL | authority/Estonia [REC] (presentation clause: wallet) | Estonia SHALL NOT certify a Track A deployment as meeting `ZKP_08` on circuit parameters whose soundness error is below **125 bits**, and SHALL ado… |
| 90 | `EE-ZKP-022b` | SHALL | wallet | The security target for a Track A deployment SHALL record, for each element of the scheme that has no ACM entry, whether that element carries a com… |
| 91 | `EE-ZKP-022c` | SHALL | wallet | For every component of the Track A scheme the wallet controls — the Merkle and Fiat–Shamir hash, the challenge expander, and the soundness paramete… |
| 92 | `EE-ZKP-062` | SHALL | wallet | The validity-period check of EE-ZKP-021(d), and the same check on the plain-mdoc path, SHALL be evaluated against the relying party's clock with a… |
| 93 | `EE-ZKP-023` | SHALL | wallet | The wallet SHALL declare the negotiated `zkSystemId` in the presentation, and the relying party SHALL verify the `circuit_hash` against a published… |
| 94 | `EE-ZKP-024` | SHOULD | authority (Estonia) [REC] | Estonia SHOULD track the Rust reimplementation maintained by ISRG (`zk-cred-longfellow`) as a second, independently-written verifier, and SHOULD re… |
| 95 | `EE-ZKP-025` | SHALL | wallet | A ZKP presentation SHALL NOT consume the attestation. Unlike a plain-mdoc presentation, a ZK proof does not reveal a linkable signature, so the sam… |
| 96 | `EE-ZKP-030` | SHALL | RIA (circuit-registry operator) [REC] | RIA SHALL operate an **Estonian ZK Circuit Registry** that SHALL mirror the accepted-circuit set the EU AV scheme owner publishes and MAY only rest… |
| 97 | `EE-ZKP-031` | SHALL | RIA (circuit-registry operator) [REC] | The registry SHALL be signed, machine-readable, versioned, and append-only, and SHALL be logged to a Certificate Transparency-style log per ARF Top… |
| 98 | `EE-ZKP-032` | SHALL | relying party | Relying parties SHALL cache the registry and SHALL NOT fetch it per presentation. |
| 99 | `EE-ZKP-033` | SHALL | RIA (circuit-registry operator) [REC] | A circuit SHALL be deprecated in the registry no later than 30 days after a soundness-affecting finding is published against it, and the registry S… |
| 100 | `EE-ZKP-040` | SHALL | wallet | On the reference device class, a ZKP age presentation SHALL complete proof generation in **≤ 2.0 s** at p95 and **≤ 3.5 s** at p99, measured from u… |
| 101 | `EE-ZKP-041` | SHALL | wallet | Peak prover memory SHALL NOT exceed **250 MB**. |
| 102 | `EE-ZKP-042` | SHALL | wallet | Where the budget cannot be met on a given device, the wallet SHALL fall back to the Tier 1 path per §10.8 and SHALL tell the user, in plain languag… |
| 103 | `EE-ZKP-044` | SHALL | wallet | The budget in EE-ZKP-040 covers proof generation only. A separate **transport budget** SHALL be met end to end: from user approval to the relying p… |
| 104 | `EE-ZKP-045` | SHALL | wallet | Where the EE-ZKP-044 budget is not met on a device-and-terminal pair, the wallet SHALL fall back to Tier 1 under EE-ZKP-042, with the same honest l… |
| 105 | `EE-ZKP-045a` | SHALL | wallet | A transport-budget failure SHALL NOT override EE-ZKP-055. Where the relying party's registration certificate asserts ZKP capability, the wallet SHA… |
| 106 | `EE-ZKP-046` | SHOULD | authority (Estonia) [REC] | Estonia SHOULD treat proof size, not proof time, as the primary selection criterion when Track B matures. |
| 107 | `EE-ZKP-043` | SHOULD | wallet | The reference device class SHOULD be defined as the median Estonian smartphone by installed base at the time of certification, not by flagship hard… |
| 108 | `EE-ZKP-050` | SHALL | relying party | The relying party SHALL accept a plain-mdoc EE-PoA presentation conforming to the data model, consistent with the AV Profile's acceptance requirement. |
| 109 | `EE-ZKP-051` | SHALL | wallet | The **wallet** SHALL NOT offer the plain-mdoc path where the device is capable of the ZKP path and the relying party advertises ZKP support. |
| 110 | `EE-ZKP-052` | SHALL | relying party [REC clause: Registrar] | Relying parties SHALL advertise ZKP support in their request. [REC] (the Registrar clause) Where an Estonian relying party is registered for an age… |
| 111 | `EE-ZKP-053` | SHALL | wallet | The wallet's transaction log SHALL record, per presentation, which tier was used, and the dashboard SHALL surface the count of linkable versus unli… |
| 112 | `EE-ZKP-054` | SHALL | wallet [REC clause: Registrar] | ZKP verification capability SHALL be an **attested attribute of the relying party's registration**, recorded by the Registrar under TS5/TS6 and car… |
| 113 | `EE-ZKP-055` | SHALL | wallet | Where the relying party's registration certificate asserts ZKP capability and the device is ZKP-capable, the wallet SHALL present over Tier 2 and S… |
| 114 | `EE-ZKP-055a` | SHALL | wallet | Where a request for an EE-PoA or for `eu.europa.ec.av.1` is not authenticated by a registration certificate — an unsigned Digital Credentials API r… |
| 115 | `EE-ZKP-056` | SHALL | Registrar [REC] | From the date set under EE-ZKP-052, an Estonian relying party registered for an age-check-only intended use whose registration does not assert ZKP… |
| 116 | `EE-ZKP-060` | SHALL | wallet | The wallet's cryptographic inventory SHALL identify, per algorithm, its quantum exposure and its migration path, and SHALL distinguish **soundness*… |
| 117 | `EE-ZKP-061` | SHOULD | authority (Estonia) [REC] | Estonia SHOULD prefer, at equal maturity, a ZKP scheme whose soundness rests only on hash functions, because it is upgradeable by swapping the issu… |
| 118 | `EE-PRO-001` | SHALL | wallet | The EE-EUDIW SHALL conform to **HAIP 1.0**. This entails, at minimum: ES256 mandatory to support; SHA-256 for digests in both formats; authorizatio… |
| 119 | `EE-PRO-002` | SHALL | wallet | Presentation Exchange SHALL NOT be used. It was removed from OpenID4VP in April 2025 before Final; `presentation_definition`, `presentation_definit… |
| 120 | `EE-PRO-003` | SHALL | relying party + wallet | Relying party authentication SHALL use the `x509_hash` client identifier prefix for every signed request, redirect and Digital Credentials API alik… |
| 121 | `EE-PRO-004` | SHALL | wallet | In redirect flows, unsigned request objects SHALL NOT be accepted except where OpenID4VP 1.0 permits them with the `redirect_uri` prefix, which SHA… |
| 122 | `EE-PRO-013` | SHALL | wallet | A signed Digital Credentials API request SHALL carry `expected_origins`, and the wallet SHALL refuse the request, before any proving work, where th… |
| 123 | `EE-PRO-007` | SHALL NOT | wallet | SIOPv2 SHALL NOT be used, and the wallet SHALL refuse a request whose `response_type` asks for an `id_token`, alone or alongside a `vp_token`. |
| 124 | `EE-PRO-005` | SHALL | wallet | Where the two conflict, Estonian implementations SHALL follow **HAIP 1.0 over OpenID4VP 1.0**, and SHALL document the deviation from 18013-7 Annex… |
| 125 | `EE-PRO-006` | SHOULD | wallet | Implementations SHOULD additionally support the ISO/IEC TS 23220-4:2026 profile, which ARF §5.4.2 indicates may replace references to 18013-5 as it… |
| 126 | `EE-PRO-010` | SHALL | wallet | Same-device presentation over the W3C Digital Credentials API SHALL be the preferred remote flow. |
| 127 | `EE-PRO-010a` | SHALL | wallet | Cross-device presentation SHALL use the W3C Digital Credentials API, where the QR is generated by the browser and proximity is established by the C… |
| 128 | `EE-PRO-011` | SHALL | wallet | Proximity presentation SHALL follow ISO/IEC 18013-5 **device retrieval**, and SHALL NOT use server retrieval. |
| 129 | `EE-PRO-014` | SHOULD | relying party | A relying party serving the general public online SHOULD accept both DC API protocols the EE-EUDIW answers — OpenID4VP 1.0 (`openid4vp-v1-unsigned`… |
| 130 | `EE-PRO-025` | SHALL | relying party + wallet | An `org-iso-mdoc` request over the Digital Credentials API, and an ISO/IEC 18013-5 proximity request, SHALL be authenticated by ISO/IEC 18013-5 rea… |
| 131 | `EE-PRO-026` | SHALL | wallet | A `request_uri` and the nonce it carries SHALL be single-use and SHALL expire within **3 minutes** of creation — the default `-session-ttl` of the… |
| 132 | `EE-PRO-012` | SHALL | wallet | Authorisation of a qualified electronic signature from within the wallet SHALL use the OpenID4VP `transaction_data` mechanism (an array of base64ur… |
| 133 | `EE-PRO-020` | SHALL | issuer | Batch issuance SHALL use the `proofs` (plural) parameter of OpenID4VCI 1.0, with an `attestation` proof type carrying a Wallet-Provider-signed key… |
| 134 | `EE-PRO-021` | SHALL | wallet + issuer | The wallet SHALL call the Notification Endpoint with `credential_accepted`, `credential_failure` or `credential_deleted` as applicable, and the iss… |
| 135 | `EE-PRO-022` | SHALL | wallet + issuer | Credential requests and responses SHALL be encrypted per OpenID4VCI §10. |
| 136 | `EE-PRO-023` | SHALL | issuer | Where an issuer uses the Pre-Authorized Code Flow for a PID, an EE-PoA, `eu.europa.ec.av.1`, or any qualified or public-sector-issued attestation,… |
| 137 | `EE-PRO-024` | SHALL | wallet + issuer | Every PID, every ISO/IEC 18013-5 attestation and every EE-PoA SHALL be cryptographically bound, by an OpenID4VCI proof of possession at issuance, t… |
| 138 | `EE-WUA-001` | SHALL | Wallet Provider (WUA issuance) | The WIA SHALL attest wallet identifiers, version, certification details and a `client_status` object; technical validity SHALL be under 24 hours, w… |
| 139 | `EE-WUA-002` | SHALL | wallet + Wallet Provider | The KA SHALL contain information about the certification of the WSCA/WSCD or keystore (`WUA_01`), and during PID issuance the wallet SHALL provide… |
| 140 | `EE-WUA-003` | SHALL | Wallet Provider (WUA issuance) | Only cryptographic algorithms in ECCG ACM v2.0 SHALL be used when issuing, presenting or verifying a WIA or KA (`WUA_04`). |
| 141 | `EE-WUA-004` | SHALL NOT | Wallet Provider + wallet (no RP presentation) | A WUA SHALL NOT be presented to a relying party. |
| 142 | `EE-WUA-005` | SHALL | Wallet Provider (WUA issuance) | WUA revocation SHALL use IETF Token Status List; user-requested WSCD/keystore revocation SHALL be supported (mandatory under the per-KA index optio… |
| 143 | `EE-WUA-006` | SHALL | issuer (Attestation Provider) | Attestation Providers SHALL check KA revocation at issuance. |
| 144 | `EE-WUA-007` | SHALL NOT | Wallet Provider (WUA issuance) | A WIA or KA SHALL NOT carry an identifier that is stable across Attestation Providers or across issuance sessions, other than the Wallet Provider's… |
| 145 | `EE-WUA-008` | SHALL | wallet + Wallet Provider | The wallet SHALL be able to present, to an AV attestation provider issuing `eu.europa.ec.av.1`, an attestation equivalent to the AV Profile's AVS U… |
| 146 | `EE-REV-001` | SHALL | issuer (Provider) | For SD-JWT VC, revocation SHALL use the Attestation Status List mechanism specified by IETF Token Status List (ARF `VCR_11a`). |
| 147 | `EE-REV-002` | SHALL | wallet + relying party | Status list fetches SHALL be batched, cached, and decoupled in time from presentations, so that a status fetch does not reveal a presentation event. |
| 148 | `EE-REV-007` | SHALL | relying party | A relying party SHALL set, and publish, a maximum age for its cached copy of each status list it relies on, not exceeding that list's `ttl`. |
| 149 | `EE-REV-006` | SHALL | issuer (Provider) | ARF `VCR_17` requires each status index to be assigned at random. |
| 150 | `EE-REV-003` | SHALL NOT | issuer (Provider) | EE-PoA SHALL NOT use any status mechanism (§9.6). |
| 151 | `EE-REV-004` | SHALL | wallet | PID and attestation deletion SHALL follow ARF Topic 51 (`PAD_*`), and wallet unit revocation ARF Topic 38. |
| 152 | `EE-REV-005` | SHALL | authority (qualified-attestation providers) | Revocation of a qualified attestation SHALL be irreversible (Art. 45d(4), 45f(4)). |
| 153 | `EE-PRI-017` | SHALL | wallet | Pseudonym generation SHALL conform to the Annex V reference — WebAuthn Level 2 as published 8 April 2021 at that dated URL — and conformance SHALL… |
| 154 | `EE-PRI-018` | SHALL NOT | wallet | A pseudonym key pair SHALL NOT be created with self attestation, and SHALL NOT be created outside the WSCD. The wallet SHALL NOT rely on the WebAut… |
| 155 | `EE-PRI-018a` | SHALL | wallet | The binding of the pseudonym key to the WSCD SHALL be evidenced **to the Wallet Provider**, at key creation, through the key attestation chain of §… |
| 156 | `EE-PRI-018b` | SHALL | wallet | The wallet SHALL remain interoperable with a relying party that performs an unmodified WebAuthn ceremony. |
| 157 | `EE-PRI-019` | SHALL | wallet + relying party | Where a relying party requests a pseudonym together with a PID or attestation presentation in a single transaction (the Art. 14(2) combination case… |
| 158 | `EE-PRI-010` | SHALL | wallet | The wallet SHALL support pseudonym generation per ARF Topic 11 (`PA_01`–`PA_31`) and CIR 2024/2979 Art. 14, stored encrypted and locally (Art. 5a(4… |
| 159 | `EE-PRI-011` | SHOULD | wallet | Pseudonym derivation SHOULD follow the BBS per-verifier-linkability construction (`draft-irtf-cfrg-bbs-per-verifier-linkability-03`) where Track B… |
| 160 | `EE-PRI-012` | SHALL | wallet | Attributes SHALL be presented only after user approval (`OIA_06`), with selective disclosure supported (`OIA_07`), and approval SHALL be obtained b… |
| 161 | `EE-PRI-013` | SHALL | wallet | Embedded disclosure policies SHALL be implemented per ARF Topic 43 (`EDP_01`–`EDP_11`), and the wallet SHALL inform the user where a relying party… |
| 162 | `EE-PRI-014` | SHALL | wallet | The wallet SHALL provide the dashboard required by Art. 5a(4)(d): the transaction log; the list of relying parties and data exchanged; a one-step G… |
| 163 | `EE-PRI-015` | SHALL | wallet | The transaction log SHALL be stored **locally** and SHALL NOT be replicated to the Wallet Provider, the Operator, or any issuer. |
| 164 | `EE-PRI-016` | SHALL | wallet | Data portability and export SHALL follow **TS10 v1.2**, including the Migration Object format, and migration to a different wallet solution SHALL f… |
| 165 | `EE-SIG-001` | SHALL | wallet | The wallet SHALL support creation of qualified electronic signatures using a QSCD, **free of charge for all natural persons** (Art. 5a(5)(g), 5a(13… |
| 166 | `EE-SIG-002` | SHALL | wallet | Remote signing SHALL use the CSC API and ETSI TS 119 432 profiles, with the signed-document binding carried in OpenID4VP `transaction_data`. |
| 167 | `EE-SIG-003` | SHALL | wallet | Signature containers SHALL be **ASiC-E (`.asice`)** with XAdES, at level **LT** minimum and **LTA** where long-term archival is required. |
| 168 | `EE-SIG-004` | SHALL | wallet | The wallet SHALL NOT create new DDOC containers and SHOULD support reading BDOC and DDOC for backward compatibility only. |
| 169 | `EE-SIG-005` | SHOULD | wallet | Validation SHOULD be delegated to **SiVa**, the Estonian signature validation service, for formats not natively supported. |
| 170 | `EE-SIG-006` | SHOULD | wallet | Where the wallet offers encryption, it SHOULD use **CDOC2**, which becomes the DigiDoc default in autumn 2026, and SHOULD NOT introduce new CDOC1 u… |
| 171 | `EE-SIG-007` | SHALL | wallet | Advanced signature and seal formats recognised by public bodies SHALL follow **CIR (EU) 2026/248** (2 February 2026), which repealed Implementing D… |
| 172 | `EE-SIG-010` | SHALL | wallet | Where the wallet authorises more than one signature from a single user authentication, the signature session SHALL be bound to **exactly one SAD en… |
| 173 | `EE-SIG-011` | SHALL | wallet | The consent screen SHALL display the **count** of documents to be signed and SHALL make each document's identity inspectable before authorisation. |
| 174 | `EE-SIG-012` | SHALL | remote signing service (issuer-side) | Before QSCD activation, the remote signing service SHALL verify an authenticated, ordered binding of every (`documentReference`, `documentDigest`,… |
| 175 | `EE-SIG-013` | SHALL | wallet | Where expression of will is a goal of the signature, the wallet SHALL implement a **non-trivial invocation interaction** per ETSI TS 119 101 SCP 51… |
| 176 | `EE-SIG-014` | SHALL NOT | wallet | A single user authentication SHALL NOT be reused across signature sessions. |
| 177 | `EE-SIG-015` | SHOULD | wallet / deploying organisation | Automated or high-volume signing performed **on behalf of an Estonian public body or other legal person**, where no individual's expression of will… |
| 178 | `EE-SIG-016` | SHALL | wallet / deploying organisation | Where a workload is nonetheless implemented as batched natural-person signatures, the risk register SHALL record the reason a seal was rejected, an… |
| 179 | `EE-SIG-017` | SHALL | wallet | Where an office-holder's signature is required by law and a seal is unavailable, the wallet SHALL bound the enumerated document set of a single SAD… |
| 180 | `EE-SIG-018` | SHOULD | authority (Estonia) [REC] | Estonia SHOULD determine, before pilot, for which statutory offices a qualified electronic **seal** in the office's own name is legally sufficient,… |
| 181 | `EE-PRI-020` | SHALL | wallet / programme (Wallet Provider) | A DPIA SHALL be completed before pilot and SHALL specifically address: the `isikukood`'s correlator role; issuer-side visibility of issuance and re… |
| 182 | `EE-PRI-021` | SHALL | wallet / programme (Wallet Provider) | The design SHALL align with **EDPB Statement 1/2025 on Age Assurance** (adopted 11 February 2025), in particular its recommendation that due consid… |
| 183 | `EE-PRI-022` | SHALL | relying party (statutory age gates) | Self-declaration SHALL NOT be used as age assurance for any Estonian relying party subject to a statutory age gate. |
| 184 | `EE-PRI-023` | SHALL | wallet / programme (Wallet Provider) | No central log of age-verification events SHALL be created, other than the operator's minimal, aggregated, non-identifying service telemetry, which… |
| 185 | `EE-PRI-024` | SHALL | wallet / programme (Wallet Provider) | Cross-border identity matching under Art. 11a SHALL implement CIR 2025/846 with measures preventing profiling (Art. 11a(2)). |
| 186 | `EE-SEC-010` | SHALL | wallet / wallet solution | The wallet solution SHALL be certified under CIR 2024/2981 against ARF ch. |
| 187 | `EE-SEC-011` | SHALL | wallet / wallet solution | Certification SHALL be valid for at most 5 years with a vulnerability assessment at least every 2 years; an unremedied vulnerability SHALL cause ca… |
| 188 | `EE-SEC-012` | SHALL | wallet / wallet solution | Functional conformance SHALL be demonstrated against the **Functional Conformance Assessment Framework** introduced in ARF v3.0.0 §7.5 (`https://co… |
| 189 | `EE-SEC-023` | SHALL | national certification-scheme owner [REC] | The national scheme SHALL be implemented as a **type 6 scheme** under EN ISO/IEC 17067:2013 §5.3.8, and SHALL use, where available and applicable,… |
| 190 | `EE-SEC-020` | SHALL | wallet / wallet solution | The security target for the WSCA SHALL claim **AVA_VAN.5** unless the CAB is given a written justification under Annex IV §3(2), and any such justi… |
| 191 | `EE-SEC-024` | SHALL | wallet / wallet solution | Where the security target for a WSCA relies on a **split-certification scoping** — a target of evaluation confined to the secure element and a cryp… |
| 192 | `EE-SEC-021` | SHALL | national certification-scheme owner [REC] | The evaluation methodology for the **wallet instance** SHALL be named explicitly in the national scheme, since Annex IV §5(2) imposes none. |
| 193 | `EE-SEC-022` | SHALL | wallet / wallet solution | Where Annex IV §2(3) applies, the security target for the WSCD SHALL claim **AVA_VAN.5**. |
| 194 | `EE-SEC-013` | SHALL | wallet / wallet solution | Where no wallet-specific Protection Profile exists at the time of evaluation, the security target SHALL explicitly enumerate the assets, threats an… |
| 195 | `EE-SEC-014` | SHALL | wallet / wallet solution | Penetration testing SHALL specifically include an attempt to obtain an EE-PoA for a false age via a modified client, and the test SHALL be repeated… |
| 196 | `EE-ACC-001` | SHALL | wallet | The wallet SHALL conform to Directive (EU) 2019/882 (Art. 5a(21)) and **EN 301 549 v3.2.1**, and SHALL satisfy ARF Topic 54 (`ACC_01`, `ACC_02`) an… |
| 197 | `EE-ACC-002` | SHALL | wallet | Wallet use SHALL be voluntary, with no disadvantage for non-users (Art. 5a(15)). |
| 198 | `EE-ACC-003` | SHALL | wallet | A non-smartphone activation path SHALL exist — ID-card with a reader on a desktop, with the wallet's WSCD provisioned as a remote HSM, from the dat… |
| 199 | `EE-ACC-004` | SHOULD | wallet | The wallet SHOULD be available in Estonian, Russian and English at launch. |
| 200 | `EE-GOV-030` | SHALL | wallet | The wallet SHALL coexist with, and SHALL NOT replace, the ID-card, Mobile-ID and Smart-ID. The transposition bill's own framing — and the governmen… |
| 201 | `EE-GOV-031` | SHALL | wallet (onboarding flows) | Mobile-ID SIM replacement SHALL be complete before **19 May 2027**, after which unreplaced SIMs stop working for both authentication and signing. |
| 202 | `EE-GOV-032` | SHALL | relying party (TARA/GovSSO) [REC clause: Estonia] | TARA and GovSSO SHALL be extended to accept an EE-EUDIW presentation as an authentication method, so that the wallet reaches the existing 60+ integ… |
| 203 | `EE-GOV-033` | SHALL | authority (Estonia / Wallet Provider) | ID-card e-services SHALL complete migration to **Web eID**; backward compatibility for legacy TLS client-certificate authentication is removed at t… |
| 204 | `EE-GOV-034` | SHOULD | authority (Estonia / Wallet Provider) | Estonia SHOULD reuse the wallet's relying-party register and access-certificate infrastructure for TARA/GovSSO relying parties, rather than operati… |
| 205 | `EE-CNF-001` | SHALL | wallet instance | The wallet SHALL obtain **OpenID Foundation self-certification** for OpenID4VP 1.0, OpenID4VCI 1.0 and HAIP 1.0. |
| 206 | `EE-CNF-002` | SHALL | wallet instance | The wallet SHALL pass the FCAF test set at `conformance.eudi.dev` for the functional requirements in Annex III of CIR 2024/2981. |
| 207 | `EE-CNF-003` | SHOULD | authority (Estonia) [REC] | Estonia SHOULD participate in the Commission's Interoperability Test Bed events (the December 2025 Launchpad ran 420 peer-to-peer tests with 60 tes… |
| 208 | `EE-CNF-004` | SHALL | authority (Estonia) [REC] | Estonia SHALL contribute its Track A ZKP verifier to the APTITUDE pilot, which Estonia joined in spring 2026 and in which RIA leads the Estonian side. |
| 209 | `EE-CNF-005` | SHALL | wallet instance | A wallet instance SHALL NOT be certified as EE-AGE-2 on the basis of a demo or debug build variant. |
| 210 | `EE-CNF-006` | SHALL | wallet instance | EE-AGE-2 and EE-PROX conformance for a **proximity** Tier 2 presentation SHALL require **measured** end-to-end evidence against EE-ZKP-044 on the r… |
| 211 | `EE-CNF-007` | SHALL | wallet instance | A wallet instance SHALL NOT be certified as achieving unobservability (`ZKP_07`) on a remote-HSM WSCD. Where a deployment offers both WSCD architec… |

---

## 4. Normative external anchors the document cites

The document anchors every `EE-*` requirement to an ARF high-level requirement, an implementing act or an Estonian statute (Annex E). The anchors it relies on, and what each governs here:

| Anchor | What it governs in this document |
|---|---|
| **EUDI ARF v3.0.0**, 2026-07-21, read at commit `6373eee`; Annex 2 = **725 HLRs across 34 topics** (topic numbers non-contiguous, 1–56) | The baseline for every derived requirement; introduces the Relying Party Services concept, dual trust-anchor retrieval, and the Functional Conformance Assessment Framework (§7.5) |
| ARF Topic 53 (`ZKP_01`–`ZKP_09`, harmonized `EW-DM-53-001…008`) | The ZKP requirement set the whole §10 profile answers; `ZKP_08` (ECCG ACM algorithms) is the unsatisfied one |
| ARF Topic 18 (`ACP_02` SHALL, `ACP_03` SHOULD) | Cryptographic binding of combined presentations |
| ARF Topic 40 (`WIAM_01`–`WIAM_21`, 34 requirements with letter suffixes) | Wallet installation and activation (EE-ONB-008) |
| ARF Topic 38 (`WURevocation_*`) · Topic 51 (`PAD_*`) · Topic 34 (`Mig_*`) | Wallet-unit revocation, deletion, portability |
| ARF Topic 3 (`PID_01`–`PID_21`) · Topic 42 (`QTSPAS_*`) · Topic 9 (`WUA_01`–`WUA_37`) | PID Rulebook conformance, PuB-EAA rules, Wallet Unit Attestation |
| ARF Topic 1 (`OIA_*`) · Topic 6 (`RPA_06/07/08/10`) · Topic 27 / 44 / 52 (`Reg_*`, `RPRC_*`, `RPI_*`) | Online access, relying-party authentication and user approval, RP registration/certificates/intermediaries |
| ARF Topic 10 (`ISSU_61/64/65`) · Topic 11 (`PA_01`–`PA_31`) · Topic 12 (`ARB_*`) · Topic 43 (`EDP_01`–`EDP_11`) | Batch issuance, pseudonyms, attestation rulebooks, embedded disclosure policies |
| ARF Topic 16 (`QES_*`) · Topic 19 (`DASH_*`) · Topic 46/48/50 (erasure, DPA report) · Topic 54 (`ACC_01/02`) · Topic 55 (`CT_01`–`CT_06`) · Topic 56 | Signing, dashboard/transaction log, accessibility, certificate transparency, support & maintenance |
| **EU Age Verification (AV) Technical Specification** and Annexes A–B (`ageverification.dev`; Annex A read at commit `014dc2f`, 1 Sep 2026) | The interoperable age-proof path: doctype **`eu.europa.ec.av.1`**, mandatory `age_over_18` with optional `age_over_NN` (§A.4.2); ZKP system **`longfellow-libzk-v1` is the only in-scope system and "An implementation SHALL use that system"** (§A.8); ZKP available **only over the W3C DC API** (§A.6); the **AVS Unit Attestation**; the §A.9 non-rejection rule for the plain fallback; §A.11's example version-6 circuit. Scheme owner publishes the accepted-circuit set separately |
| **OpenID4VC HAIP 1.0** (`-07`, 24 Dec 2025) | Protocol baseline: ES256 mandatory, DCQL mandatory, response encryption mandatory, `x509_hash` client-identifier prefix for signed requests, response type `vp_token` (excludes SIOPv2) |
| **OpenID4VCI 1.0** (Final, `-17`) | Issuance: batch `proofs` parameter, §10 encryption, §14.5 refresh token, notification endpoint, PKCE/DPoP/`request_uri` |
| **OpenID4VP 1.0** (Final, `-30`) | Presentation: DCQL, `transaction_data`, §6.1 holder binding, §14.4 account-keying claims, Annex A.2 `expected_origins` |
| **ISO/IEC 18013-5:2021** and **DIS 2nd ed.** (stage 40.60, publication expected Q4 2026) | mdoc, device retrieval, `ValidityInfo`, and — in the 2nd edition — ASL/ARL revocation and the **`ZkRequest`/`ZkSystemSpec`/`ZkDocument`** structures (§10.2.7) that carry the ZKP transport |
| **ISO/IEC TS 18013-7:2025** (Annex B profiles OpenID4VP **draft 18**, Annex C DC API) · **ISO/IEC TS 23220-2/-3/-4:2026, -6:2025** | Online mdoc; the version gap EE-PRO-005 resolves in favour of HAIP, and the 23220-4 profile EE-PRO-006 SHOULD support |
| **W3C Digital Credentials API** (WD 4 Sep 2026) · **RFC 10027 / BCP 247** | Browser API transport (the preferred remote flow) and cross-device flow security |
| **RFC 9901** SD-JWT · `draft-ietf-oauth-sd-jwt-vc-19` · `draft-ietf-oauth-status-list-21` · `draft-ietf-oauth-attestation-based-client-auth-10` · RFC 9449 DPoP · RFC 9101 JAR · RFC 9207 | SD-JWT VC encoding and type metadata, status lists, client authentication, the HAIP protocol stack |
| **ETSI** TS 119 612 (Trusted Lists) · TS 119 602 (List of Trusted Entities) · TS 119 472-1/-2/-3 · TS 119 101 v1.1.1 (SCP 49–52) · TS 119 431-1 v1.3.1 · TS 119 432 v1.3.1 · **TS 119 476-2** (early draft v0.0.4, STF 705) · EN 419 241-1/-2 · EN 17640:2018 (FITCEM) · EN 301 549 v3.2.1 | Trust lists, the LoTE, PID/attestation format profiles, signature creation/validation and sole-control (SCP), remote signing, the convergence point for **Track B** ZKP work, remote-signing modules, the wallet-instance evaluation methodology, accessibility |
| **EC Technical Specifications**: TS1 v1.2 · **TS3 v1.5.2** (WUA) · **TS4 v1.0.1** (ZKP implementation) · TS5 v1.5 / TS6 v1.2.2 (RP registration) · TS7 v1.0 (erasure) · TS8 v1.0 (DPA report) · TS9 · TS10 v1.2 (portability) · TS11 · TS12 · **TS13 v1.0.1** (ZKPs from arithmetic circuits → Track A) · **TS14 v1.0** (ZKPs from multi-message signatures → Track B) · FCAF (`conformance.eudi.dev`) | The Commission's own specifications for WUA, ZKP tracks, RP registration formats, erasure/report, portability, and functional conformance testing |
| **ECCG Agreed Cryptographic Mechanisms v2.0** (April 2025) and the **v3.0 working draft** (April 2026, reviewed to end-July 2026) | What `ZKP_08` pins by name. v2.0 §1.1 sets **"at least 125 bits"** for *recommended* mechanisms and §1.3 accepts 100 bits for legacy; neither version mentions zero-knowledge proofs, pairings or commitments; the v3.0 draft demotes every classical asymmetric mechanism to *admissible* |
| **CIR 2024/2981** (Annex IV methods, Annex VIII assessment report) | The certification obligations: AVA_VAN.5 for the WSCA (unconditional) and WSCD (conditional on Art. 3(3)(b)), the §3(2) lower-level justification route, and §5(2)'s "shall consider" EN 17640 for the wallet instance |
| **CIR 2024/2979** Annex II (as replaced by CIR 2026/1731) and Annex V (Art. 14 pseudonyms, WebAuthn Level 2 pinned by dated URL) | PID format obligations; pseudonym generation hard-pinned to WebAuthn L2 (8 Apr 2021) |
| **CIR 2025/848** (as amended by 2026/1730) · **CIR 2025/847** · **CIR 2025/846** · **CIR 2025/1569 Art. 4(3)** as replaced by **2026/1735** · **CIR 2025/2160** · **CIR 2026/798** · **CIR 2026/248** · **CIR 2026/1731** | RP register and access certificates; suspension of wallet provision on breach; Art. 11a cross-border identity matching; the >24 h revocation duty that forces the EE-PoA out of the PuB-EAA class; non-qualified trust-service risk management; onboarding procedures (with CEN/TS 18098); recognised advanced signature/seal formats; amended attestation annexes |
| **Reg. (EU) 910/2014 as amended by 2024/1183** — Arts. 3, 5, 5a, 5b, 5c, 5f, 11a, 13, 17(3)(b), 19a(1), 22, 26, 35, 36, 45b, 45d, 45e, 45f, 45h; Annexes II, VI, VII | The statutory source of most obligations: provision deadline 24 Dec 2026, activation at LoA High, registration and intended use, certification, liability, cross-border matching, non-qualified provider duties, PID/PuB-EAA requirements |
| **Dir. (EU) 2019/882, GDPR, CIR 2015/1502, CIR 2024/482 (EUCC)** | Accessibility, data protection (incl. Art. 8, 17, 28), the LoA-High definition and "attackers with high attack potential", the EU cybersecurity certification framework |
| **Estonian law** — EUTS · ITDS · IKS · Küberturvalisuse seadus · Rahvastikuregistri seadus · Avaliku teabe seadus · **Alkoholiseadus § 46–47** · **Tubakaseadus § 27–28** · **Hasartmänguseadus §§ 34, 37(6¹,8–10), 39(8), 53(1), 83(1), 89, 94** · Riigilõivuseadus · Karistusseadustik · **TsÜS §§ 8, 134–137** · **EVS 585:2007** · RahaPTS § 2(1) p 3, § 19(3) | The Estonian statutory duties that decide the age-check and identification use cases (Annex A), age reckoning, the `isikukood` structure, and the AML rules the document says are *not* the operative basis |
| **Cryptographic literature and audits**: Frigo & Shelat, *Anonymous Credentials from ECDSA* (IACR CiC 3(1), 2026) · `draft-google-cfrg-libzk-02` · BBS drafts (`-signatures-10`, `-blind-signatures-03`, `-per-verifier-linkability-03`) · `draft-cllz-cfrg-ecdsa-pop-00` · Trail of Bits (Aug 2025) and ISRG (Oct 2025) audits of Longfellow · Ligero academic panel (Dec 2025) · *Cryptographers' Feedback on the EU Digital Identity's ARF* (19 Jun 2024) | The Track A/B selection, the 109-bit soundness figure and its provenance, the Fiat–Shamir/challenge-expander questions, and the unlinkability argument §10 is built on |


---

## 5. What the document leaves open or leaves to authorities

### 5.1 `[REC]` — recommendations to authorities, not product requirements (28 identifiers)

Per Annex E: **22 wholly `[REC]`**, **6 in a named clause only**. Wallet conformance is assessed against the other 189 identifiers and the non-`[REC]` clauses of these six.

* **Estonia ("Estonia" / the Government), whole:** `EE-GOV-001`, `EE-GOV-004`, `EE-GOV-011`, `EE-GOV-012`, `EE-GOV-034`, `EE-RP-004`, `EE-ONB-010`, `EE-PRI-001`, `EE-ZKP-024`, `EE-ZKP-046`, `EE-ZKP-061`, `EE-SIG-018`, `EE-CNF-003`, `EE-CNF-004`.
* **Registrar, whole:** `EE-RP-001`, `EE-POA-005a`, `EE-ZKP-056`.
* **RIA as circuit-registry operator, whole:** `EE-ZKP-030`, `EE-ZKP-031`, `EE-ZKP-033`.
* **National certification-scheme owner, whole:** `EE-SEC-021`, `EE-SEC-023`.
* **Named clause only:** `EE-PID-008` (Registrar refusal), `EE-ZKP-052` and `EE-ZKP-054` (Registrar clauses), `EE-ZKP-022a` (certification and adoption clauses), `EE-SEC-010` (national-scheme clause), `EE-GOV-032` (extension of TARA and GovSSO).
* Reviewed for the tag and **left untagged** because they address no authority: `EE-ZKP-032` (relying parties cache the registry), `EE-ZKP-055` (the wallet's Tier 2 duty).

### 5.2 §23 — Open issues and unverified claims (36 items)

The document's own words: *"Every item here should be re-checked before this document is used to build anything."* Status as the document itself records it:

**Open (unresolved):** 1 RIA procurement outcome / whether the Commission Reference Implementation becomes the codebase · 2 named PID Provider · 3 the EUTS/RLS/KarS amendment (approved by Government 1 Oct 2026, before the Riigikogu; **"All §2.2 requirements are conditional on it"**) · 5 re-notification of the Thales/Zetes cards under eIDAS · 7 ENISA candidate scheme status · 9 whether the EU AV app's ZKP path ships enabled in production builds · 11 Estonian pension-age schedule for `age_over_63` · 12 Estonian median device installed base (no measurement exists; PENDING-DEVICE) · 13 Estonian language-share data for `EE-ACC-004` · 14 Commission Art. 49 review report (due 21 May 2026, not found) · 16 HasMS § 83(1) still says 16 where § 34(3) says 18 — an unconformed cross-reference · **18 whether Art. 45b(2) describes an *issuer* class or a *compliant* class — "the single most consequential unresolved legal question in this document"** · 19 no published attack-potential methodology for the wallet instance · 20 whether any Estonian scheme document asserts an EAL · 21 WebAuthn Level 3 vs Annex V's pinned Level 2 · 23 the Estonian national pre-certification route · **24 ISO/IEC 18013-5 device-retrieval throughput for a ~400 KB payload — "the largest unmeasured engineering assumption in the Tier 2 path"** · **25 whether audited ≥125-bit Longfellow circuit parameters exist or are planned** (circuits remain v7 at 109-bit soundness) · 26 Android StrongBox key-generation throughput and key-slot capacity for the batch of ≥30 (PENDING-DEVICE) · 27 whether Estonian law compels a natural-person QES rather than permitting a qualified seal · 28 whether HasMS § 37(8) reaches toto/lottery venues, and whether § 37(9)'s document-copy duty can be discharged electronically · 29 where the certified boundary falls inside a local-native WSCD · 30 no accredited Estonian CAB and no chosen evaluation standard · 34 no `ZKP_08` clarification out of ARF Topic G (re-check after 18 Nov 2026) · 36 the statutory basis for age reckoning (`EE-POA-027`, `EE-POA-028`).

**Marked `[UNVERIFIED]` at the point of use:** 19 (attack-potential guidance for mobile apps), 31 whether Estonian access certificates carry a `dNSName` SAN (bindable by `EE-PRO-003`/`EE-PRO-013`), 32 Android RKP rotation period (two Google sources differ), 33 the EU AV accepted-circuit set against `EE-ZKP-022`, 35 the notified-schemes status on the day the LoA-High onboarding is assessed.

**Resolved or answered during revisions (recorded as such):** 4 Estonian ID-card key algorithm — **resolved**, P-384/`secp384r1`; 6 ECCG ACM v3.0 final content — **resolved**, there is no final v3.0 text (only the April 2026 working draft); 8 whether ZKP was promoted SHOULD→SHALL — **answered for the current revision**, a SHALL on capable devices over the DC API only; 10 the EU AV Profile attribute set — **answered**, `age_over_NN` is an optional issuance attribute beside mandatory `age_over_18`. Item 17 (Vega) is recorded as *"Promising; do not build on it yet."*

### 5.3 §22 — Deliberate divergences from the EU baseline (21 items)

Recorded so a conformity assessor finds them quickly: age as a dedicated attestation with bounded thresholds (1); predicates computed from the Population Register, not a client-side document read (2); registration certificates recommended over dynamic lookup (3); ZKP support required of age-check-only registrants (4); HAIP 1.0 wins over 18013-7 Annex B (5); all gambling excluded from the anonymous age path (6); **no status list for the EE-PoA** (7); **the EE-PoA is a non-qualified EAA, not a PuB-EAA** (8); pseudonym keys constrained beyond CIR 2024/2979 Annex V (9); cross-relying-party pseudonym unlinkability required — *this specification's requirement*, not the implementing act's (10); high-volume organisational signing routed to seals (11); `mdoc` status carried on a Token Status List in the interim (12); a registration naming PID attributes for an age-check-only use is rejected by the Registrar (13); `EE-ACC-001` requires EN 301 549 **v3.2.1** where `ACC_01/02` name v1.1.2 (14); ZKP capability as an attested registration attribute, not a self-declaration (15); unobservability claimed per WSCD architecture, not per wallet solution (16); Track A operated as a pilot below the ACM recommended level (17); proximity conformance requires measured end-to-end evidence (18); Track A instantiated at the *recommended* tier for wallet-controlled components (19); **SIOPv2 refused** (20); an age presentation discloses one predicate and never a `false` one (21).

### 5.4 Numbers the document itself chooses (and says so)

These are the specification's own engineering choices, not values inherited from an EU instrument: the **7-day `ValidityInfo` epoch** (`EE-POA-026`), the **14-day re-issuance lead** (`EE-POA-025`), the **3-month maximum validity** (`EE-POA-016`), the **batch floor of 30** (`EE-POA-011`, lowered only on a device whose WSCD cannot sustain 30 keys, per `EE-POA-011b`), the **3-minute `request_uri` lifetime** (`EE-PRO-026`), the **10-minute pre-authorized code lifetime** (`EE-PRO-023`), the **60-second clock tolerance** (`EE-ZKP-062`), the **≤2.0 s p95 / ≤3.5 s p99 proof-generation budget**, the **≤1.0 s p95 verification budget**, the **≤250 MB peak prover memory**, and the **≤4.0 s p95 end-to-end transport budget** (`EE-ZKP-040`, `-041`, `-044`). It also declines to fix a circuit version ("the circuit versions in the accepted set of `EE-ZKP-030`; this specification does not fix a version") and **states no EAL requirement** (EAL4 appears only in a recital of CIR 2024/2981).

### 5.5 §21 — Roadmap and dependencies (not requirements, but schedule-critical)

Operator contract ~mid-2027 (RIA estimate) · EUTS/RLS/KarS amendment in force: unknown · public launch of *Digikukkur*: **2028** · RP register MVP + guidance: end-2026/early 2027 · access-certificate design published: open · public-sector acceptance of foreign wallets: **24 Dec 2026** · ISO/IEC 18013-5 2nd ed.: **Q4 2026 expected — "the single biggest schedule dependency for the ZKP profile"** · SD-JWT VC as RFC: unscheduled · ETSI TS 119 476-2 stabilisation: unknown · EE-AGE-2 pilot: aligned with the 2028 launch · private-sector acceptance obligation: **24 Dec 2027**. §21.1 adds a risk register R1–R21 (e.g. R2 `ZKP_08` remains unsatisfiable; R13 the remote-WSCD provider-side activity log, "certain, by construction, for that population" and **not mitigable in the presentation path**; R18 the `[REC]` authority duties not being adopted; R20 no accredited CAB and no national scheme by Q4 2027).


---

## 6. Product surfaces the document defines (roles, artefacts, documents)

These are *not* requirements; they are the things a PRD will need to name, own and staff.

### 6.1 Roles (Estonian assignment in §3, with `[UNVERIFIED]` markers where the document has none)

| ARF role | Estonian assignment | Basis |
|---|---|---|
| Wallet Provider | **RIA**, under the Ministry of Justice and Digital Affairs | Art. 5a(2)(a) |
| Wallet Operator (contractual, not an ARF role) | Contractor from the RIA procurement — 96-month contract, competitive dialogue; **[UNVERIFIED — no award announced as of 5 Sep 2026]** | Estonian public procurement |
| PID Provider | **RIA**, sourcing from the identity-documents database and the Population Register over X-tee; the procurement folds PID issuance into the operator's scope | Art. 5a(5)(f), CIR 2024/2977 |
| **EE-PoA Provider** | RIA (§9, on behalf of the Population Register) | Art. 45b(2) reading; §9.7 |
| Registrar (relying parties, providers) | **RIA**; MVP plus guidance end-2026/early 2027 | CIR 2025/848 as amended |
| Access Certificate Authority (ACA) | **[UNVERIFIED / open]** — RIA states the exact access-certificate solution is still being worked out | CIR 2025/848 Art. 7, Annex IV |
| QEAA Provider | SK ID Solutions AS and other Estonian QTSPs on the EU Trusted List | Art. 45d |
| PuB-EAA Provider | PPA (documents), Transpordiamet (mDL), Tervisekassa (EHIC), Maksu- ja Tolliamet (tax), HTM (qualifications) | Art. 45f; Annex VII |
| Authentic sources | Rahvastikuregister; isikut tõendavate dokumentide andmekogu; ABIS (biometrics only); sectoral registers — all over **X-tee** | Art. 45e; Annex VI |
| Certificate issuer, ID-1 documents | **Zetes Estonia OÜ** since 17 Nov 2025; SK ID Solutions for the legacy IDEMIA estate | id.ee |
| Supervisory / competent authority | **RIA** | EUTS § 2 |
| DPA | **Andmekaitse Inspektsioon** | IKS |
| CAB | Designated under Art. 5c(1); accredited per EN ISO/IEC 17065:2012 — **no accredited body exists yet (§23 item 30)** | CIR 2024/2981 Art. 9(1) |
| WSCD provider (remote-HSM path only) | Not named; carries its own disclosure and retention duties (`EE-SEC-001b`, `-001c`) | §4.2 |

Also named as non-wallet surfaces that must interact: **Eesti äpp** (`ee.riik.eesti.app`) — expressly *not* the EUDI Wallet; **TARA / GovSSO**; **SiVa** (signature validation); **Andmejälgija** (data-subject query log); the **EU LoTE** and Art. 22 **Trusted List**; the **national ZK circuit registry**; the **RIA/operator back end** (WUA service, wallet-unit revocation service, support & maintenance).

### 6.2 Wallet architecture surfaces (§4.1)

Wallet Instance (presentation layer · credential store for mdoc CBOR and SD-JWT VC · ZKP prover · dashboard + transaction log · WSCA) · WSCD, one of four architectures (native / eSIM / external card / remote HSM) · Wallet Provider back end (WUA issuance service, wallet-unit revocation service, support & maintenance) · issuance plane (PID Provider, EE-PoA Provider, PuB-EAA providers, X-tee security servers) · trust plane (Commission LoTE, EU Trusted List, EE Registrar, EE ACA, national ZK circuit registry).

### 6.3 Credentials and attestations defined or required

`eu.europa.ec.eudi.pid.1` PID (both ISO/IEC 18013-5 mdoc **and** SD-JWT VC; `vct` base `urn:eudi:pid:1`, Estonian `urn:eudi:pid:ee:1`) · **`ee.riik.poa.1` EE-PoA** (mdoc mandatory, optional SD-JWT VC with `vct` `urn:ee:poa:1`; attributes `age_over_18` mandatory, `age_over_16`/`age_over_21`/`age_over_63`/`age_over_NN` optional, plus `issuing_country`, `issuing_authority`, `expiry_date`) · **`eu.europa.ec.av.1`** EU AV Profile attestation · catalogue (§8): mDL, EHIC, powers and mandates, tax residency certificate, educational qualification, e-Residency status.

### 6.4 Documents and artefacts the solution must produce

Attestation Rulebooks (one per Estonian attestation, registered in the Commission's catalogue) · the **Wallet Instance Attestation (WIA)** and **Key Attestation (KA)** and the AVSUA-equivalent instance attestation · **access certificates** and **Registration Certificates** · the **signed, machine-readable, append-only ZK circuit registry** with per-circuit soundness levels · **status lists** and the relying-party status-cache-age publication · the **transaction log and privacy dashboard** · the **DPIA**, the **risk register**, the **cryptographic inventory**, the **security target** (per component) and the **assumption map** for `EE-ZKP-022b` · the **conformity documentation / conditional `ZKP_08` statement**. Annex B's DCQL examples, including the two anti-patterns, are the interface artefacts relying parties will copy.

### 6.5 Conformance profiles (§20.1)

| Profile | Contents | Mandatory for |
|---|---|---|
| **EE-CORE** | PID in both encodings; OpenID4VCI/VP/HAIP; WUA; dashboard; erasure and DPA reporting; pseudonyms; revocation; portability | All wallet instances |
| **EE-AGE-1** | EE-PoA and `eu.europa.ec.av.1`; batch once-only issuance; coarsened `ValidityInfo`; single-use consumption; downgrade signalling | All wallet instances |
| **EE-AGE-2** | EE-AGE-1 plus Track A ZKP presentation, circuit-registry validation, performance budget incl. the proximity transport budget, honest labelling; MAY be certified on the DC API path alone | Wallet instances on capable devices; relying parties registered for age-check-only use from a date set by RIA |
| **EE-QES** | Wallet-based QES, ASiC-E LT/LTA, `transaction_data` binding, SiVa validation | All wallet instances (Art. 5a(5)(g)) |
| **EE-PROX** | ISO/IEC 18013-5 proximity device retrieval; mDL; EE-PoA over the selected transport, within `EE-ZKP-044` where Tier 2; Tier 2 in proximity optional until §23 item 24 closes | Wallet instances; mDL relying parties |

### 6.6 Two capability tiers the PRD inherits

**Tier 1** (mandatory, deployable today) — once-only batched attestations per ARF Method A; defeats relying-party linkability, not issuer–verifier collusion. **Tier 2** (specified in full, gated behind conformance criteria) — a genuine ZKP presentation; does not consume the attestation. The document is explicit that an implementer may not claim Tier 2 privacy while shipping Tier 1.

