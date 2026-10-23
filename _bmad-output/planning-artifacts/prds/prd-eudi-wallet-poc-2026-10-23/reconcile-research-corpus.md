# Reconciliation: `prd.md` + `addendum.md` against the research corpus

**Date:** 2026-10-23 · **Method:** source-first. The three research documents were read in full
(`docs/research/2026-10-23-innovation-assessment.md` = **IA**,
`docs/research/2026-10-23-estonia-alignment-assessment.md` = **EA**,
`docs/research/2026-10-23-shared-appendices.md` = **SA**), then `prd.md` (1,628 lines) and
`addendum.md` (187 lines), then every candidate finding was checked line by line against the PRD
(grep + read) and, where the PRD cites a code fact, against the code. Anything the PRD already
carries is not reported. Line numbers are file:line. `spec/EE-EUDIW-TS-1.0.md` and the extracts are
cited where the PRD's own §0 names the spec as part of the intent record.

**This is reconciliation, not review.** No edits to `prd.md` are proposed or implied.

## Counts

| tier | meaning | count |
|---|---|---|
| **critical** | the PRD is wrong: it asserts something a source contradicts, or presents a gap as shipped | **2** |
| **high** | a material idea, constraint or label the source insisted on is lost | **8** |
| **medium** | a figure, caveat, criterion or attribution lost or shifted; no contradiction | **12** |
| **low** | context, identifier or detail lost; nothing load-bearing | **10** |
| | **total findings** | **32** |

Also checked and found **clean** (six items) — listed at the end so no reader re-opens them.

---

# CRITICAL

## C-1. Batch issuance is presented as shipped and rule-compliant; the corpus records it as a gap, and the batch size stated (three) is the size the spec explicitly forbids

**Source.** EA:178 (requirement row A2, status **partial**): "ZK path shows one boolean; plain path
labelled as linkable in the UI (`PresentationTier`); **batch issuance for EE-PoA is a recorded
gap**". EA:169 (row L4, status **partial**, required change): "real issuer integration; **batch
issuance (a gap the repository's own analysis records)**". EA:199 (row S2, partial): required change
"real issuer; **batch issuance**". SA:33 (fork's own row, *documented limitations*): "no
certification path; no real issuer; **batch issuance missing**"; SA:85 (index row A2 = partial).
The spec the PRD's §0 adopts as part of the intent record: `spec/EE-EUDIW-TS-1.0.md:509`
(`EE-POA-010`, SHALL — batch of single-use attestations, one key pair per attestation) and
`spec/EE-EUDIW-TS-1.0.md:515`, `:523` (`EE-POA-011b`: "the batch SHALL be at least 30" where the
WSCD can sustain it, otherwise "the largest size the WSCD is measured to sustain"; "**There is no
third case, and no reading on which a wallet may quietly ship a batch of five.**").

**PRD.** `prd.md:447-461` — "#### FR-8: The EE-PoA attestation follows its consumption and validity
rules **(shipped)** … Attestations are issued **in a batch of three** in one transaction". The batch
size is real in the code (`AgeVerificationProviderServiceMock.kt:26` `BATCH_SIZE = 3`, asserted by
`EePoaIssuanceTest.kt:105`), but the PRD never carries the corpus's "batch issuance … gap /
missing" status, never mentions `EE-POA-011`/`EE-POA-011b`, and its §10 distribution
(`prd.md:1352-1355`) reports 23 partial rows without any reader being able to find which.

**Why critical.** A row the corpus marks *partial* three times and *missing* once is presented as
shipped behaviour that "follows the spec's once-only and validity rules", and the number carried
(three) is below the "batch of five" the spec names as impermissible. Either the gap closed between
the research date and this PRD (then §10 and FR-8 need the reason) or the label was upgraded.

## C-2. NG-10 presents the 21 and 16 thresholds as statutory age gates; the intent record says neither is a gate an anonymous predicate can serve

**Source.** IA:551-552 (O6 benefit): "closes a real gap for Estonian services that are not
age-18-gated (**alcohol and tobacco at 18/21, other restrictions at 16**)". EA:218 (row E3, partial
— thresholds "implemented and tested in the wallet; accepted circuit set not published").
EA:270 lists the collision points as "alcohol and tobacco sales, **gambling**, online content".
Against that, the spec the PRD adopts: `spec/EE-EUDIW-TS-1.0.md:441` — `age_over_16` is Optional
because "**No current Estonian statutory gate sits at 16** — Hasartmänguseadus § 34(3) raised lottery
to 18"; `:442` — `age_over_21`: "**No Estonian statutory gate that an anonymous predicate can
serve**"; `:465-481` (§9.3) — both channels that enforce 21 are "identification duties, not age
checks", and `EE-POA-005a` (SHALL) makes the Registrar refuse an EE-PoA predicate request for any
`Hasartmänguseadus` intended use.

**PRD.** `prd.md:964-966` — "**NG-10: No multi-threshold library.** Statutory thresholds beyond 18
(**the 21s and 16s national law carries**) are gated behind the circuit registry and scheme-owner
acceptance". The PRD mentions no gambling, tobacco or alcohol anywhere (verified: grep over
`prd.md` for `gambling|tobacco|alcohol|Hasartmäng|Method A|RKP` returns three matches, all the word
"method").

**Why critical.** The PRD states a legal premise the record contradicts and gives the wrong reason
for the omission (external circuit acceptance, where the record's reason is that the duty is
identification and must not be discharged by a predicate). It also drops the record's own risk row
for this (`spec/EE-EUDIW-TS-1.0.md:1244`, R8: "Relying parties treat EE-PoA as discharging a
gambling identification duty").

---

# HIGH

## H-1. Tier 1's stated limit is dropped from the glossary definition and from the limits list

**Source.** `spec/EE-EUDIW-TS-1.0.md:70`: "Tier 1 (mandatory, deployable today) is once-only batched
attestations per ARF Method A, **which defeats relying-party linkability but not issuer-verifier
collusion**"; `:521`: "A batch minted in one sitting falls inside a single Android RKP key-rotation
epoch (roughly 14 days) and is therefore attested by one per-app attestation key, so batch members
**remain mutually linkable at the attestation layer**"; extract-spec.md:408 carries both.

**PRD.** `prd.md:308-309` — "**Tier 1** — the once-only, batched, plain attestation posture.
Mandatory and deployable today." The PRD declares §3 the vocabulary downstream workflows must use
verbatim (`prd.md:251-252`) and lists the claim's limits in NFR-15 as "(`ZKP_08`, 109-bit circuits,
no device measurements)" (`prd.md:1212-1214`) — the collusion limit and the attestation-layer limit
appear nowhere in `prd.md`.

## H-2. The `msoX5chain` residue is dropped

**Source.** EA:50: "its carrier profile records the residue honestly (**the proof still reveals
`msoX5chain`**) `[VERIFIED]`"; EA:180 (row A10): "proof size and the `msoX5chain` residue are
documented (`docs/profiles/…PROFILE.md` §constraint)".

**PRD.** Nowhere. FR-1's consequence is "the disclosed payload contains a boolean and no attribute
value" (`prd.md:355-356`), PG-2 is "the disclosed payload is a boolean and nothing else"
(`prd.md:1235-1236`), and NFR-15's limits list omits it.

**Why high.** The source calls this the honest residue of its own privacy claim; the PRD's privacy
guardrails read as unqualified without it. (Note: a certificate chain is not "an attribute value",
so the two are not formally contradictory — but the caveat the source insisted on is gone.)

## H-3. TS4's own maturity position is dropped

**Source.** EA:100: TS4 — "At present, **no existing ZKP approach can be deemed fully suitable or
mature enough for direct integration into the EUDI Wallet**" `[VERIFIED]`, and EA:146 (tooling-gaps
table, who is hurt = procurement teams): "an EU programme cannot simply mandate ZKP today; it must
fund pilots and evidence".

**PRD.** `prd.md:1330` lists "TS4 v1.0.1 (ZKP implementation)" as an instrument with no position
attached. No FR, NG or OQ carries the Commission's own maturity statement.

## H-4. The gambling carve-out is absent from the PRD

**Source.** `spec/EE-EUDIW-TS-1.0.md:465-481` (§9.3 "The gambling carve-out"), `:475`
(`EE-POA-004`, SHALL: remote gambling is an **identification** use case, a PID presentation not an
EE-PoA), `:479` (`EE-POA-005a`, SHALL: the Registrar refuses EE-PoA predicate registration for any
`Hasartmänguseadus` intended use), `:1244` (risk R8 with the § 94 / § 89 penalties). extract-spec.md:346
(§22 divergence 6): "all gambling excluded from the anonymous age path".

**PRD.** Nowhere. `prd.md` mentions no gambling at all (grep above), and §2 JTBD is built from
EA:270's list, which *includes* gambling.

**Why high.** A SHALL-class carve-out and the register's refusal duty are legal constraints on what
the product may be asked to do; the PRD's Estonia material reads as if gambling age checks are in
scope.

## H-5. O1's acceptance criterion "runs against at least two independent implementations" is dropped; only the carrier suite gets an external run

**Source.** IA:234 (O1 acceptance criteria): "(i) Technical differentiation: **the kit runs against
at least two independent implementations (this fork's verifier and at least one external wallet or
verifier)** and reports a clause-by-clause result". SA:141 (roadmap M1, *unresolved decisions*):
"external implementation to test against".

**PRD.** FR-11…FR-16 (`prd.md:510-573`) require a clause registry, AV fixtures, a runner, a
reproducible report and published out-of-scope clauses. FR-12 requires "At least one fixture
reproduces a container-format example originating outside this repository" — a *fixture*, not a run
against an implementation. The only external-implementation requirement is FR-19
(`prd.md:604-612`), which belongs to the carrier suite (O5). OQ-13 (`prd.md:1605-1608`) scopes the
external-target question to the carrier suite as well.

**Why high.** The record's own testable acceptance criterion for its top-ranked opportunity is not
in the PRD, and the ledger of what v1 claims to prove reads narrower than the record's.

## H-6. Two of the record's unresolved questions are missing from §18

**Source.** EA:429-431: "Will the RIA programme adopt the EU AV Profile for age checks, or the
fork's `ee.riik.poa.1`, or both? The fork's spec prefers the AV attestation for every RP; **no
Estonian statement says otherwise, and none endorses it either**". EA:434-435: "**Would an Estonian
service be permitted to accept a *foreign* AV attestation before the Estonian register exists?**
Regulatory question, not a technical one. `[UNVERIFIED]`".

**PRD.** §18 (`prd.md:1545-1611`) carries OQ-1…OQ-14. Neither question appears; OQ-14 is about an
EU-level role, OQ-3 about the register's interface. FR-40 (`prd.md:872-873`) states the AV-first
ordering as a written statement without recording that no Estonian source endorses it.

## H-7. The mandated library's own audit history is dropped

**Source.** EA:110: Google Longfellow ZK — "independent reviews including **Trail of Bits (report
dated 2025-08-18)** and an **ISRG-reported MDOC circuit under-constraint patched in v0.8.4**"
`[VERIFIED]`; SA:230 lists the IETF draft; extract-spec.md:316 ties these to "the 109-bit soundness
figure **and its provenance**".

**PRD.** FR-41 (`prd.md:877-888`) declares the 109-bit figure against the 125-bit floor; nothing
anywhere records that the library has two independent reviews or that an under-constraint defect in
the mdoc circuit was found and patched.

**Why high.** The PRD makes its own audit a product constraint (§9.2, NFR-16) and buys the library's
soundness story without the provenance the source attaches to it.

## H-8. The PRD's instrument list omits the PID/EAA and protocols regulations the record names, while saying the list is "named by the intent record"

**Source.** EA:95 / extract-estonia-alignment.md:418: "CIR **2024/2977** (PID/EAA), 2024/2979
(integrity, core functionalities), 2024/2980 (notifications), **2024/2982 (protocols/interfaces)**,
2025/848; amended by CIR 2026/1730, 2026/1731, 2026/1735" — and these are exactly the instruments
behind matrix rows L4 (issuance framework, partial) and L6 (protocols and interfaces, **supported**).
extract-spec.md:17 lists the baseline as "CIR 2024/2977, 2024/2979, 2024/2980, 2024/2981, 2024/2982
…"; extract-spec.md:312 also carries **CIR 2026/248**.

**PRD.** `prd.md:1314-1323`: "**Instruments named by the intent record:** … **Implementing
regulations and their amendments:** CIR 2024/2979, 2980, 2981; 2025/846, 847, 848 (as amended by
2026/1730); 2025/1569 (as amended by 2026/1735); 2160; 2026/798; and 2026/1731" — 2977, 2982 and
2026/248 are absent.

**Why high.** In a PRD whose product issues EAAs and whose §10 exists so a reader can "see which of
them the evidence addresses", the PID/EAA regulation is missing while the preamble asserts the list
is the record's.

---

# MEDIUM

## M-1. §10's distribution relabels the record's fifth category

Record: SA:120-123, "**41 rows — 5 supported** … **23 partial** · **7 missing** · **5 not
applicable** · **1 not verified** (A11, whose requirement is unsatisfiable as written)"; EA:69-72
states the same; the PRD's own addendum D4 says "**1 not verified**". PRD `prd.md:1352-1355`:
"5 rows supported, 23 partial, 7 missing, 5 not applicable, **1 unsatisfiable** … The single
unsatisfiable row is ARF `ZKP_08`." The distribution is presented as "the record's own", so
substituting the category name in it is a label change (`unsatisfiable` is the source's *parenthetical*
about that row, not its status).

## M-2. Test-file count carried as 26 where the record says 27

IA:105 and extract-innovation-assessment.md:111, :561: "**27 Kotlin unit-test files** / 169
`@Test`". PRD `prd.md:1096` and `:1183`: "169 `@Test` over **26** files / **26** test files". The
repository has 27 `.kt` files under `app/src/test`, one of which (`test/TestUtils.kt`) holds no
tests, so 26 is defensible — but unlike A-8 (which resolves the identifier-count discrepancy
explicitly, `prd.md:1627`) the PRD never says it re-derived this figure.

## M-3. The 115-bit IETF-draft claim is dropped while the 109-bit figure is kept

EA:110 and SA:230: "`draft-google-cfrg-libzk-03` **claims ≥ 115 bits** for the `(p256, 132, 7)`
profile `[VERIFIED as quoted from the draft]`". PRD FR-41 (`prd.md:886-888`) carries only the
109-bit bundled-circuit figure against the 125-bit floor.

## M-4. Every per-phase effort estimate except two is dropped

SA:136-148 carries `[ESTIMATE]` ranges per phase — D1 2–4, D2 1–2, D3 2–3, M1 3–6 (dependency-
weighted 4–8), M2 2–4, M3 2–3, P1 6–10, P2 8–14, P3 4–8, R1 10–18 engineer-weeks. The PRD carries
only Q1 (`prd.md:1134-1139`, 6–12 engineer-months) and O6 via A-7 (`prd.md:1626`, 6–12 weeks), while
CG-1 (`prd.md:1280-1283`) refers to "**the record's ranges**" as if more were present.

## M-5. `ageverification.dev` is dropped as a validation target and as an OQ-13 candidate

IA:202 and :253-254: "The Commission publishes the AV documentation and a development environment
(`ageverification.dev`) … **Validation: run the kit against the Commission's AV development
environment and publish the result, including failures**"; extract-innovation-assessment.md:693
carries it as an open question. The PRD's §12 lists external implementations without it
(`prd.md:1394`) and OQ-13 (`prd.md:1605-1608`) names the multipaz sample, the reference wallet, the
iOS library and Longfellow's Go verifier service only.

## M-6. The named device set is dropped; B1/B3 point at "the named device set"

SA:139-140 (D2 deliverable and unresolved decision): "Pixel 8 + one mid-range device"; EA:398-400:
"on the named device set (**a Pixel 8 and one mid-range device per the plan**)". PRD
`prd.md:1158-1163`: "on-device proving percentiles and peak memory **on the named device set** (B1)"
— the set is never named anywhere in `prd.md` or `addendum.md`.

## M-7. The procurement-award supersession risk and its mitigation are dropped

SA:321-324 (A6 tradeoff 5): "**If the Estonian procurement is awarded while this work is in flight,
the winning provider will set the technical direction and the RF-side kit (O2) may be
superseded.** That is a reason to finish D3 (the interface proposal) early, so the proposal is on the
table rather than reconstructed." The PRD §13 (`prd.md:1406-1423`) has no row for it and §6.3
(`prd.md:1045-1061`) does not carry the sequencing reason.

## M-8. TARA's integration constraint — "a parallel path, not assumed inside TARA" — is dropped

EA:337 (TARA row, integration constraint, `[REC]`): "**No published EUDI adapter exists.** A
wallet-facing integration must be built as a **parallel path, not assumed inside TARA**". PRD
`prd.md:1389` records only "no published EUDI adapter exists → FR-22".

## M-9. The "[VERIFIED as absence]" finding that no Estonian ZK or age-verification work exists is dropped

EA:138: "Estonian ZK / age-verification work — **none found** in any reachable official or press
source; Cybernetica's cryptography page is a service statement … not a project `[VERIFIED as
absence]`". The PRD carries the AV-trusted-list absence (`prd.md:1536-1537`) and the X-Road absence
(NG-15) but not this one, which is the positioning claim's Estonian-side support.

## M-10. Estonia's acceptance deadline is attributed to Recommendation (EU) 2026/1035 rather than to the duty the record derives it from

EA:93 (landscape row): "Wallet availability duty — RIA states: **24 months after the relevant
implementing acts; first acts in force 24 December 2024 ⇒ expected deadline end-2026**" (evidence:
RIA FAQ), and EA:94 separates "Acceptance duties" from the AV material. PRD §17 (`prd.md:1522-1524`):
"**29 April 2026** — Recommendation (EU) 2026/1035 set 31 December 2026 for age verification to be
available, **which is the deadline the Estonian acceptance duty sits against**." The record's
derivation (and the source it rests on) is not carried, and the Recommendation is an AV instrument,
not the acceptance duty. EA:234-235 is explicit: "the dates and duties used here therefore rest on
RIA's and the Ministry's own statements and on the ARF".

## M-11. OR-4 states the relying party's log "carries the ARF Topic 19 fields" while the record marks those fields missing

EA:179 (row A3, partial): "presentation journal and tier labels exist; **certificate-derived RP
fields do not**"; EA:207 (row S10, **missing**): "not implemented; only client-id prefix policy".
PRD `prd.md:1437-1439`: "**OR-4 — The relying party's log carries the ARF Topic 19 fields** and no
attribute value. What an operator can log about a presentation, and what it must not, is fixed here
rather than left to the deployment." The Topic 19 field list in the record (EA:179) includes the
RP's trade name, unique identifier, service identifier, Member State, intended use and privacy-policy
URL, i.e. exactly the certificate-derived fields the record says do not exist.

## M-12. The Estonian-vs-EU attestation ordering is carried as a statement but without its tradeoff

SA:308-312 (A6 tradeoff 2): the AV-first order "maximises cross-border alignment and … is **the
riskier order politically, since an Estonian programme may want its own credential first**.
**Recommendation: keep the spec's order, keep both paths, and let the programme choose.**
`[JUDGEMENT]`". PRD FR-40 (`prd.md:872-873`) requires the ordering statement and "both paths
retained" but carries neither the political risk nor "let the programme choose".

---

# LOW

- **L-1. Risk-register type column dropped.** SA:168-184 tags every risk by type
  (interoperability/standards, adoption, privacy, security, maintenance, licensing, credibility,
  regulatory, commercial); the PRD's §13 table (`prd.md:1406`) keeps L/I, mitigation and revisit only.
- **L-2. "In one sitting" dropped from the record's O1 criterion (iii).** IA:238: "a third party
  reproduces the report from a clean checkout using only documented commands, **in one sitting**";
  the PRD's FR-14/SM-1 (`prd.md:545-550`, `:1074-1077`) keeps the reproduction and drops the
  one-sitting bound.
- **L-3. OpenID4VP issue #627's age is dropped.** EA:144 "OID4VP #627 open **~16 months**"; IA:76-77
  the same. PRD FR-43 (`prd.md:910`): "tracked with its current state and date".
- **L-4. The ARF-date conflict is not carried.** EA:419 (conflicts table): "CHANGELOG: 21 July 2026;
  GitHub release published 23 July 2026 — **cite both**; immaterial to content". PRD `prd.md:1324-1325`
  cites the release date only.
- **L-5. Estonian programme context dropped.** EA:133: POTENTIAL (2023 onward) and APTITUDE
  (joined spring 2026), the 2023 MVP built by Cybernetica AS, and the APTITUDE count conflict
  (RIA: 117 organisations / APTITUDE site: "more than 141") recorded at EA:418; EA:132 also drops the
  Turu-uuringute AS market survey from September 2026.
- **L-6. eID-ecosystem figures and two dated facts dropped.** EA:134 (ID-card ≈1.2 M vs 1,383,604;
  Mobile-ID ≈219,000 vs 215,398; Smart-ID 799,658; **Digi-ID issuance to residents ended 1 May
  2025**) and EA:341 (Smart-ID/Mobile-ID assurance assessed "high" by an RIA-convened expert group).
  Only the "single ID-card count" ban survives, at CG-2 (`prd.md:1284-1285`).
- **L-7. `EE-ZKP-040` is dropped as the identifier of the missed budget.** EA:81, EA:481 and IA:65
  name the budget by requirement id ("`EE-ZKP-040` ≤ 1.0 s p95; measured …"); the PRD
  (`prd.md:489-497`, NFR-6) says "the specification's own budgets" without the id, although it uses
  `EE-CNF-005`, `EE-WUA-008`, `EE-POA-006/007`, `EE-ZKP-055a`, `EE-PRO-010a` elsewhere.
- **L-8. "Three Member States were ahead and had applied for cross-border approval" (May 2026)
  dropped.** IA:272-273.
- **L-9. Procurement detail dropped.** EA:126 ("**procurement ID 10227365**; five-year service;
  roughly one year to contract signature if undisputed"); landscape-extract.md:271 ("competitive
  dialogue; development + 5 years of operation"). PRD §17 keeps the value, the 96-month span and the
  no-award statement (`prd.md:1529-1531`).
- **L-10. FR-35's "(shipped)" title over an accessibility row the record marks partial.** EA:189
  (row A13, partial: "**no ETSI EN 301 549 test evidence**") and EA:220 (row E5, partial). PRD
  `prd.md:797-806`: "FR-35: The site holds the public-sector accessibility baseline (shipped)".
  Mitigated by NFR-12, §10 and R13, which state the audit evidence is missing.
- **L-11. Access failures other than EUR-Lex and Riigi Teataja are dropped.** EA:439-457 records the
  Firecrawl `/v1/search` outage, `iso.org`, `riigihanked.riik.ee` HTTP 500 and the three delegated
  research passes with which claims were labelled as not re-fetched. The PRD carries only the
  EUR-Lex HTTP 500 (`prd.md:1317-1318`) and the Riigi Teataja client-render (`prd.md:1348-1350`).
- **L-12. FR-21 forbids the approach the record prescribes.** IA:311: "**Fork the verifier into a
  service profile**, add the plug interfaces and mocks, write the acceptance suite". PRD
  `prd.md:641-643`: "The kit is a deployment profile over the existing verifier, **not a fork of
  it**; installing it requires no code change to the verifier" — in tension with FR-22/FR-23, which
  require an adapter contract and a pluggable register interface.

---

# Checked and clean (do not re-open)

1. **TS13 version.** EA:101 says "TS13 … v1.0 15 December 2025"; the landscape pass corrected this
   (landscape-extract.md:325: "TS13 **is v1.0.1 (2026-01-30)**, not v1.0 (2025-12-15)"). The PRD's
   "TS13 v1.0.1" (`prd.md:1331`) is right.
2. **"€21.65 M over up to 96 months".** consistent with landscape-extract.md:271 ("up to **96
   months** (development + 5 years of operation)"); EA:126's "five-year service" is the same fact.
3. **109-bit soundness.** Sourced, not invented: extract-spec.md:316 and :338 item 25 ("circuits
   remain v7 at 109-bit soundness").
4. **Q1–Q5 and B1–B5.** Carried with baselines, methods and confidence (`prd.md:1134-1163`) —
   including Q2's "unrealistic bound" arithmetic (`prd.md:1140-1143`) and Q4's PENDING-DEVICE
   reason. No upgrade found.
5. **The `p95 3.2–4.9 s` range in FR-10** is `docs/MEASUREMENTS.md:265-266`, not a PRD invention.
6. **Opportunity scoring, the 41-row tally and the O6/A-7 estimate** are carried (SM/§6.1/§10/A-7);
   only the *evidence-confidence* labels per opportunity (IA:257, :328, :390, :452, :505, :558) are
   dropped, which is a presentation loss rather than a figure loss.
