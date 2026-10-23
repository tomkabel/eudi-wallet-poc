# Reconciliation — PRD fork docs (DESIGN.md, docs/CONFORMITY.md, PRODUCT.md)

**Pass:** post-finalisation reconciliation, by an independent subagent.
**Date:** 2026-10-23. **Working tree:** `2d38feb` (plus uncommitted agent files; nothing in this pass is committed).
**PRD:** `_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md` (1628 lines) and its `addendum.md` (187 lines).

## Method

Read in full: `DESIGN.md` (303 lines), `docs/CONFORMITY.md` (202 lines), `PRODUCT.md` (65 lines), then the PRD and
addendum. Every claim the PRD makes *about* those three files was then tested against the file, and every claim the
PRD makes about repository state was tested against the repository (code, tests, workflows, `sandbox/`,
`_bmad-output/planning-artifacts/epics.md`, `docs/sota-audit-ROLLUP.md`).

**Tier rule applied:** *critical* = the PRD asserts something a cited file or the repository contradicts;
*high* = a materially dropped constraint, caveat or claim about state; *medium* = a misattributed or unverifiable
citation, or a misquoted source label; *low* = precision.

**Totals: 3 critical · 4 high · 6 medium · 3 low = 16 findings.**

---

## CRITICAL

### C1 — FR-36 asserts a phrase that appears nowhere in the file it cites
- **PRD** (`prd.md:828`, FR-36 consequence, written as a pass/fail check): *"The phrase "no compliance or
  certification claim" appears in the conformity note and remains true of every artefact it indexes."*
- **Source** (`docs/CONFORMITY.md:10`): *"Nothing here is a certification claim."*
- **Repository:** the exact string occurs **zero** times in the repository (`grep -rn` over `docs/`, `spec/`,
  `README.md` → no match).
- **Why it matters:** FR-36 is a v1 deliverable and its consequence is testable; as written the check cannot pass
  without editing the conformity note to carry a sentence the PRD assumes it already has. Either the PRD quotes
  the note's actual wording, or FR-36 correctly becomes an edit instruction rather than a verification.

### C2 — "CertificateChainValidator … has zero direct unit tests today" is false
- **PRD** (`prd.md:1103-1105`, SM-8): *"`CertificateChainValidator` — the PKIX trust gate — has zero direct unit
  tests today … The release closes those three gaps or records them."* Repeated at `prd.md:1256-1263` (§9.2 SG-1:
  *"a PKIX trust gate with zero direct unit tests"*).
- **Source of the claim** (`docs/sota-audit-ROLLUP.md:36-37`, audit at `732d57e`): *"CertificateChainValidator (the
  PKIX trust gate) has ZERO direct unit tests; only SD-JWT test asserts nothing; :zk-conformance runs in NO
  workflow."*
- **Repository:** `app/src/test/kotlin/ee/cyber/wallet/security/CertificateChainValidatorTest.kt` exists — 338
  lines, a cert-minting harness plus `@Test`s including *"chain anchored at the trusted root is accepted"*,
  *"intermediate chain rooted at the trusted anchor is accepted"*, *"foreign root is refused"*, *"expired leaf is
  refused"*. Added in `d909a35` ("E1-E4: fail-closed LOTL trust, digest-pinned keystore, soft-fail CRL
  revocation"), which is **not** an ancestor of the audit commit `732d57e` — i.e. the test post-dates the audit.
- **Why it matters:** the PRD's own NFR-16 (*"refreshed at each release … No artefact may imply a published
  finding is fixed unless a test or a tool run establishes it"*) is exactly the rule this finding violates in
  reverse: it republishes a stale audit item as present-tense state and books work that is already done. Same
  class of defect as the `issuerAuth` row the PRD correctly catches.

### C3 — "`:zk-conformance` runs in no CI workflow" is false
- **PRD** (`prd.md:1104`, SM-8; also §7.2's framing of it as one of "those three gaps").
- **Repository:** `.github/workflows/app.yml:47-48` — `name: ZK conformance (byte-identical fixtures)` /
  `run: ./gradlew :zk-conformance:test`, in the `unit-test` job, with `push`/`pull_request` path filters that
  include `zk-conformance/**` (`app.yml:12`, `:20`) and a comment explaining the design (*"ZK conformance rides
  with the app workflow (devsecops C6-rider)"*).
- **Source of the claim:** the same audit line (`docs/sota-audit-ROLLUP.md:37`) and TIER 2 (`:58`,
  *":zk-conformance + go -race missing from CI"*).
- **Why it matters:** two of SM-8's three "gaps" are not gaps. The one real one is the third — `EudiSdJwtTest.kt`
  has a single `@Test` and no assertion (`app/src/test/kotlin/ee/cyber/wallet/sdjwt/EudiSdJwtTest.kt:53-54`,
  the verify calls at `:98` and `:126-134` commented out) — and SM-8 as written dilutes it.

---

## HIGH

### H1 — the PRD names one stale row in the conformity note; the note carries a second, and the PRD contradicts it
- **PRD** (`prd.md:812-815`, §4.6): the note is *"currently violated: the conformity note's `issuerAuth` row states
  that on-device `issuerAuth` validation is absent while the wallet code performs it"*; R12 (`prd.md:1419`) repeats
  it as the single documentation-contradicts-code risk. The `issuerAuth` half is **verified correct** —
  `docs/CONFORMITY.md:160` says *"E6 status: absent — mdoc credentials load with no issuerAuth chain check"*, while
  `app/src/main/kotlin/ee/cyber/wallet/domain/documents/CredentialToDocumentMapper.kt:82-114` performs exactly that
  check (signature + PKIX anchoring) and is wired at `:171` (`verified = verifyIssuerAuth()`), landed in `704a66d`.
- **Source the PRD did not name** (`docs/CONFORMITY.md:95-99`, under **"What this wallet cannot claim today"**,
  never struck): *"**No OpenID4VP ZK carrier exists.** OpenID4VP 1.0 defines no way to carry a ZK request or a
  `ZkDocument`, and the AV Profile's plain fallback is available where the transport is OpenID4VP — so over
  OpenID4VP this wallet presents plain only."* The same file contradicts this at `:78-80` (*"Over OpenID4VP the
  spine now runs the de-facto `mso_mdoc_zk` carrier … e2e-verified end to end in `tests/e2e.sh`"*) and at
  `:100-112` (the 25 September 2026 carrier status note).
- **PRD** (`prd.md:344-356`, FR-1): the OpenID4VP `mso_mdoc_zk` carrier is marked **(shipped)** and primary.
- **Why it matters:** FR-38 (`prd.md:847-850`) promises *"every other hand-maintained row is verified against the
  code before it is edited"*. This is a second row of exactly the issuerAuth kind, in the same file, on the
  headline capability — and it is the one an evaluator is most likely to read, because it sits under a heading
  that says "cannot claim". The PRD's OQ-6 (`prd.md:1586-1592`) gestures at the standards question but not at the
  note's self-contradiction.

### H2 — NG-11 misstates what the cited file says about status lists, and its §18 pointer is dangling
- **PRD** (`prd.md:967-970`, NG-11): *"**No status or revocation mechanism for the age attestation.** The EE-PoA
  is a non-qualified EAA with no status mechanism, and status lists are minted into credentials without being
  fetched or validated anywhere today. This is a known gap recorded in §18, not a feature."*
- **Source** (`docs/CONFORMITY.md:164`): *"Status lists | EE-PoA **mints none** (EE-POA-013: consumed, not revoked;
  EE-POA-017); AV RP-side status fetch **deferred** — the Go verifier has no plain-mdoc path (all presentations
  route through ZK carriers that hide the status claim), so there is no post-Select hook to hang the fetch on"*;
  `:185`: the AV attestation is issued *"with the EE-PoA's epoch validity and **no status reference**"*;
  `:195`: *"the Go verifier fetches no status list today"*.
- **Repository:** the status lists that *are* minted are on the **upstream mock PID and mDL fixtures**
  (`CredentialIssuanceServiceMock.kt`, `PidProviderServiceMock.kt`, `MdlProviderServiceMock.kt`), not on either age
  attestation — which is what CONFORMITY says.
- **Second defect:** *"recorded in §18"* is false. No §18 item (OQ-1 … OQ-14, `prd.md:1545-1611`) mentions status,
  revocation or status lists; the only home for it is §6.2 (`prd.md:1041`). A reader chasing the cross-reference
  finds nothing.
- **Why it matters:** the sentence as placed reads as "our age credentials carry a status list we never check" —
  the opposite of the cited note — and it drops CONFORMITY's scoped reason for the deferral (no plain-mdoc path to
  hang the fetch on), turning a bounded deferral into an unbounded "known gap".

### H3 — CONFORMITY's recorded deviation of this fork's own verifier is dropped entirely
- **Source** (`docs/CONFORMITY.md:199`): *"Reference verifier response encryption | **Deviation, recorded.** The Go
  verifier encrypts `direct_post.jwt` with `ECDH-ES+A256KW`/A256GCM (`verifier/go/internal/jose/jwe.go`), and its
  JWK carries no `alg`. HAIP 1.0 and EE-PRO-001 name direct `ECDH-ES`, which is all the wallet accepts, so **the
  Android redirect path cannot answer this verifier** (its unsigned request objects were already refused)."*
- **PRD:** FR-1 (`prd.md:351-354`) states the wallet's response encryption rule and its refusal; §12's dependency
  table (`prd.md:1387-1398`) lists TARA, the register, X-Road, e-Residency, Smart-ID, external ZK
  implementations, the AV trusted list, standards venues, the host toolchain and pre-1.0 dependencies — **no row
  for the fork's own verifier deviating from the profile its own wallet enforces.** The PRD is silent.
- **Why it matters:** this is not an external gap; it is the fork's two shipped halves failing to interoperate on
  the redirect path, recorded by the conformity note and *absent from the PRD*. An integrator following §12 lands
  on it immediately. It also belongs in the same "rows corrected / deviations declared" set as FR-38.

### H4 — FR-42 asks for a `ZKP_07` claim the cited file has already, and deliberately, refused
- **PRD** (`prd.md:890-900`, FR-42): *"A per-path unlinkability statement exists, backed by a test asserting that no
  tracking identifier is added to a presentation (`ZKP_07`); where the property cannot be claimed, it is recorded
  as not claimed rather than as satisfied."*
- **Source** (`docs/CONFORMITY.md:120-125`): *"**No unobservability (`ZKP_07`) claim, per WSCD architecture
  (`EE-CNF-007`).** Every credential device key, including the one behind the ZK path, is an EC key on the local
  native WSCD … There is therefore one WSCD architecture to state the property for, **no `ZKP_07` claim is made
  for it here, and none could be for a remote-HSM WSCD**."*
- **PRD elsewhere:** §15 DG-7 (`prd.md:1470-1472`) and NG-8 (`prd.md:957-960`) carry the `ZKP_04` pseudonymity
  non-claim, but neither carries the `ZKP_07` refusal or its `EE-CNF-007` anchor.
- **Why it matters:** the caveat is the load-bearing part. FR-42 is new work with a downstream story (Epic 8); as
  written it invites a story to *claim* `ZKP_07` on the strength of a test, when the conformity note already
  refused the claim for a structural reason (one local WSCD architecture) that a test cannot change.

---

## MEDIUM

### M1 — the cited epic summary is off by three (`FR1–FR63` → `FR1–FR66`)
- **PRD** (`prd.md:29`, §0): *"`_bmad-output/planning-artifacts/epics.md` already decomposes the same research
  corpus into 11 epics and 75 stories, with its own **FR1–FR63** and NFR1–NFR10 numbering"*. Repeated in
  `addendum.md:186-187`.
- **File:** `epics.md` defines **FR1–FR66** (66 definitions at `:84-152`; FR63 `:149`, FR64 `:150` — Certificate
  Transparency for access certificates — FR65 `:151`, FR66 `:152`) and NFR1–NFR10 (`:164-171`) ✓.
- **Also:** 11 epics `:288-1581` ✓, but **76** story headings (`### Story n.m`), not 75. The PRD's own A-6
  (`prd.md:1625`) rests on this numbering being described correctly.

### M2 — PRODUCT.md's product principles are attributed to "the record" and PRODUCT.md is never cited in the body
- **Source** (`PRODUCT.md:56-60`): *"## Product Principles — 1. Show, don't claim: every number on the page comes
  from the run that just happened. 2. Say what the shop did not learn, as plainly as what it did. 3. Name the
  sandbox's shortcuts where they happen, not in a footnote."*
- **PRD** (`prd.md:1492-1496`, §16): *"**The product principles the record pins**, which every surface is judged
  against, are: 1. Show, don't claim … 2. Say what the shop did not learn as plainly as what it did. 3. Name the
  sandbox's shortcuts where they happen, not in a footnote."*
- **Repository:** the three principles appear **only** in `PRODUCT.md` — they are not in `docs/research/` or the
  extract corpus. `PRODUCT.md` is named exactly once in the whole PRD (`prd.md:25`, the §0 input list) and is never
  cited for a claim, despite §0 listing it as one of the two documents the PRD builds on. PRODUCT.md's own
  "Product Principles" heading also does not say the record pins them.

### M3 — "the sandbox site's documented six-step flow (`DESIGN.md`)" is not in DESIGN.md
- **PRD** (`prd.md:154-155`, §2.3): *"UJ-1 from the sandbox site's documented six-step flow (`DESIGN.md`)"*; and
  A-2 (`prd.md:1621`): *"every beat, outcome and edge case traces to the record or `DESIGN.md`"*. Also
  `prd.md:62-63` ("the visitor's guided six-step proof") and `prd.md:717-718` (§4.5 description).
- **DESIGN.md** (`:247`): *"The journey is a 240px sticky step rail beside a single column of step cards. Under
  1000px the rail becomes a sticky "**Step n of 6**" line with a six-segment progress bar"* — the rail, the count
  and the responsive variant are documented; the **six steps' content is not** anywhere in DESIGN.md.
- **Repository:** the six steps exist, in the site itself — `sandbox/static/index.html:74-78` (rail) and
  `:84-249` (`data-step="1"` … `"6"`).
- **Why it matters:** UJ-1's beats are real and the site carries them, but the attribution and A-2's traceability
  claim point at a file that does not contain them; they trace to the sandbox site, which the PRD never names as
  the source of the journey.

### M4 — the accessibility baseline is stated without its source, and PRODUCT.md's commitment is left uncited
- **Source** (`PRODUCT.md:62-65`): *"## Accessibility & Inclusion — Estonian public-sector baseline: **WCAG 2.1
  AA**. Keyboard-complete, screen-reader announcements for long-running steps, and reduced-motion support."*
- **DESIGN.md:** states **no accessibility baseline at all**. Its accessibility-relevant content is one ratio
  (`:212`, pewter/input-stroke *"(≥3:1 against white)"*), one focus ring (`:276`), and reduced-motion
  (`:285`, the proof mark's *"static Blue Mist fill under reduced motion"*). No WCAG, no EN 301 549, no keyboard
  or screen-reader sentence.
- **PRD** (`prd.md:797-806` FR-35; `prd.md:1204-1207` NFR-12): *"WCAG 2.1 AA as the stated baseline with EN 301 549
  as the presumption: keyboard-complete, visible focus, **contrast held by the pinned tokens in `DESIGN.md`**, and
  no reliance on colour alone"*; §10 (`prd.md:1357`) repeats the pair. **Neither file is cited**, and EN 301 549
  comes from the research corpus (`extract-shared-appendices.md:183` A13 — *"ETSI EN 301 549 presumption"*), where
  it is labelled partial and without test evidence.
- **Why it matters:** PRODUCT.md is the file that commits the project to WCAG 2.1 AA; the PRD carries the duty as
  if self-evident and adds EN 301 549 unlabelled. R13 (`prd.md:1420`) does carry the "no EN 301 549 evidence" half.

### M5 — FR-36's banned-vocabulary check would fail on the conformity note it names
- **PRD** (`prd.md:825-827`, FR-36): *"A check in `make check` fails on the banned vocabulary ("conformant",
  "certified", "compliant", "meets the profile") in documentation"*; `:828`: the phrase *"remains true of every
  artefact it indexes."*
- **Source** (`docs/CONFORMITY.md:35`): *"a backward-compatibility dependency on already-issued credentials and
  **certified** WSCDs"*; `:192`: *"the remote-HSM path is a mock and stays out of any **certified** deployment"*.
- **Why it matters:** both uses are adjectives on third-party objects, not claims about this product — but a
  substring check cannot tell the difference, so FR-36 as specified would flag the file the PRD wants it to bless.
  The check needs an allow-list rule or a narrower pattern before it can be green, and CM-5's "don't go vague"
  counter-metric makes silently weakening it unattractive.

### M6 — the record's own tally label is rewritten in §10
- **Source** (`extract-shared-appendices.md:121`, the PRD's cited source for the tally): *"41 rows — 5 supported ·
  23 partial · 7 missing · 5 not applicable · **1 not verified** (A11, whose requirement is unsatisfiable as
  written)."* The addendum carries it faithfully (`addendum.md:127-130`: *"1 not verified · 0 compliant"*).
- **PRD** (`prd.md:1352-1355`): *"5 rows supported, 23 partial, 7 missing, 5 not applicable, **1 unsatisfiable** —
  and no row "compliant". The single unsatisfiable row is ARF `ZKP_08`."*
- **Why it matters:** §0 (`prd.md:36-39`) makes label preservation non-negotiable ("claims carry the record's own
  labels … nothing in this PRD upgrades one"), and the record's label for A11 is *not verified* — "unsatisfiable as
  written" is its parenthetical. The PRD also pairs the two counts in the addendum in a way that reads as 42 rows
  if a reader adds them (5+23+7+5+1+1). Keep the record's word and the parenthetical.

---

## LOW

### L1 — "169 `@Test` over 26 files" is loose arithmetic
- **PRD** (`prd.md:1095-1096` SM-6, `prd.md:1182-1184` NFR-5): *"`:app:testLocal_mocksUnitTest` … stays at 169
  `@Test` over 26 files with zero failures"* (inherited verbatim from `epics.md:166`).
- **Repository:** 169 `@Test` ✓ — but they sit in **25** files. 26 is the count of files *named* `*Test.kt`; one of
  them, `app/src/test/kotlin/ee/cyber/wallet/SampleTest.kt`, contains no `@Test`. Either say "25 files" or "26
  `*Test.kt` files", since this line is a regression gate.

### L2 — the refusal on the redirect/proximity paths is stated more broadly than the source rows
- **PRD** (`prd.md:145`, §2.2): *"on a ZK-capable device an age request over the proximity or redirect path is
  refused outright today rather than answered plainly"*; FR-6 (`prd.md:424-434`) similarly.
- **Source:** the *outright* refusal on those two paths is stated inside the last-attestation row
  (`docs/CONFORMITY.md:177`); the general rule comes from a different row (`:188`, EE-ZKP-055a: *"every DC API and
  proximity request is unauthenticated: on a ZK-capable device an age request without a satisfiable ZK request is
  refused before consent"*).
- **Why it matters:** the conclusion is right, but it is a two-row derivation; the PRD states it as a single fact
  a reader cannot check against either row alone.

### L3 — "contrast held by the pinned tokens in `DESIGN.md`" over-reads the token file
- **PRD** (`prd.md:803`, FR-35).
- **DESIGN.md** states exactly one ratio (`:212`, ≥3:1) and it governs a **stroke**, not text; there is no contrast
  map for text or status washes in the file.
- **Why it matters:** FR-35's other clauses are all rooted in DESIGN.md (focus ring `:276`, reduced-motion `:285`,
  the status triad `:189`/`:216-218`); this one clause implies a table that does not exist.

---

## Claims checked and found accurate (no action)

Recorded so the maintainer does not re-open them:

- **§4.6 on the `issuerAuth` row** (`prd.md:812-815`): exact against `docs/CONFORMITY.md:160`, and the code really
  does perform the check (`CredentialToDocumentMapper.kt:82-114`, wired at `:171`, commit `704a66d`). The research
  corpus does say it "should be corrected before the note is shown to an evaluator"
  (`extract-innovation-assessment.md:141-142,653`; `extract-shared-appendices.md:172` R12).
- **CONFORMITY.md's identity** (`prd.md:4,26`): the file *is* the `EE-ZKP-003` statement plus the `EE-ZKP-060`
  inventory (`docs/CONFORMITY.md:1-12`) ✓.
- **§4.5 and §16 carry DESIGN.md correctly**: "The Service Counter" is the file's north star (`DESIGN.md:179`);
  TEDI-derived and ria.ee-inferred chrome (`:181`); Roboto + Material Symbols (`:241`); "Estonian first"
  (`:183`, `:296`); the independence notice in utility bar / info alert / footer (`:295`, matched in
  `sandbox/static/index.html:19,45,327`); no RIA logo or coat of arms (`:299`).
- **FR-28 … FR-35 are genuinely shipped**: six steps (`index.html:84-249`), ledger with learned/kept columns
  (`:256-263`), the cheat band with three real attacks — replay, session-tamper, lying predicate (`:277-302`),
  ET default with an ET/EN toggle (`:2`, `:20-22`), synthetic personas labelled inside the flow (`:45`), honest
  missing-toolchain failure naming `make deps` (`sandbox/server.py:446`).
- **FR-3's "9-case table"**: `verifier/go/oid4vp/vptoken_test.go:42-84` is exactly nine cases, including the CBOR
  `false` (`0xf4`), non-hex, non-boolean, wrong doctype/namespace/attr_id and unsupported `zk_system` ✓.
- **NFR-16's audit figure**: `docs/sota-audit-ROLLUP.md:2` (*"Total findings: 124"*) and `:19` (*"## Critical:
  none"*) ✓, with the TIER 1 items at `:22-37` matching SG-1's list.
- **`tests/e2e.sh` 13 assertions, both carriers, both response modes** ✓ (`:405`, `PASS: 13/13`).
- **Also accurate**: the 109-bit v7 figure against the 125-bit ACM recommended floor
  (`CONFORMITY.md:42-48`), the conditional reading (a) of `ZKP_08` (`:14-27`), `EE-CNF-005` on every debug-derived
  build (`:129-147`), the "~0.42 verifications/s" arithmetic (2,414 ms → 0.414/s) and the Q3 bandwidth/price
  arithmetic (360 KB × 10^6 = 360 GB at €0.05-0.12/GB = €18-43), the `AgeIssuanceConstants.validity` /
  `AgeReckoning.effectiveBirthDate` / `AgeAttestationRenewal` / `lastAttestation` / `AgePredicatePolicy` /
  `refusePlainFallback` code references, the `cbor2`/`cryptography` pinning note, the §9.4 technical constraints
  (they match `AGENTS.md` line for line), replay returning `410` (`verifier/go/jar_test.go:96`,
  `iso_test.go:170-173`), and every file §0 names as an input, all of which resolve (`docs/MEASUREMENTS.md`,
  `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`, `docs/research/2026-10-23-*.md`,
  `_bmad-output/planning-artifacts/epics.md`).

---

## Suggested disposition, in the order that costs least

1. **C2, C3** — delete or re-scope the three SM-8 gaps; the third (SD-JWT assertions) is the only live one. Move
   the CCV and CI items to a "closed since the audit" line, which is itself good evidence.
2. **C1, M5** — quote `CONFORMITY.md:10` correctly, and give FR-36's check an allow-list so it can be green.
3. **H1** — add the OpenID4VP carrier bullet (`CONFORMITY.md:95-99`) to R12 / FR-38 as a second row to correct; it
   is one edit to the note and removes the only flat contradiction an evaluator will find.
4. **H2, H3** — fix NG-11's wording and §18 pointer; add a §12 row for the recorded verifier deviation.
5. **H4, M3, M4, M2** — attribute the `ZKP_07` refusal, re-anchor UJ-1 and A-2 to the sandbox site, cite
   PRODUCT.md for WCAG 2.1 AA, and correct "the record pins" for the three principles.
6. **M1, M6, L1, L2, L3** — one-line precision fixes.
