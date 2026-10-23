# PRD Quality Review — EE-EUDIW ZK Age-Verification Wallet

Reviewer: rubric walker (bmad-prd Validate intent). Rubric:
`.agents/skills/bmad-prd/assets/prd-validation-checklist.md`.
Reviewed: `prd.md` (1,628 lines) and `addendum.md` (187 lines).
Spot-checks were run against the repository at `2d38feb` (2026-10-12): code, tests, CI
workflows, `Makefile`, `docs/`, `spec/`, and `_bmad-output/planning-artifacts/epics.md`.

## Overall verdict

This is an unusually honest, decision-shaped PRD: the thesis ("the product is the evidence")
is stated as a bet rather than a slogan, every one of the 44 FRs carries testable
consequences, the 19 non-goals and 6 counter-metrics do real work, and the rejected options
in `addendum.md` D1 name what was given up instead of smoothing everything to neutral. What
is at risk is the one thing the document sells — that its claims are checked against the
code and the record. Four of its own navigation references are wrong, its pointer to the
sibling `epics.md` cites stale counts, and the baseline in SM-8 and SG-1 asserts two facts
about the repository that stopped being true before this PRD was written.

## Decision-readiness — strong

The PRD behaves like a decision document. §18 opens with two **Phase-blockers** separated
from the questions "carried from the record", each with a stated recommendation and an
explicitly named decider: "This is a scope decision the maintainer must take, not the
analyst." `addendum.md` D1 goes further than most PRDs ever do — D1.2 names "the strongest
argument against the choice" for the rejected Vision centre, D1.3 records "the steelman for
the rejected option", and D1.1 admits the rubric's separation was "marginal". R16 is
labelled in bold as an "assistant-added row, not present in the record's A4", which is the
kind of disclosure that usually gets quietly omitted. The trade-off at the centre of the
release — SG-2's "consent screen's claims must be true" against shipping v1 without
`readerAuth` chain validation — is surfaced as a tension rather than resolved by assertion.

Two things keep this from being cleaner than it is. §18 declares that its phase-blockers
"are resolved or explicitly deferred before finalisation", and the document in front of me
is the finalised one with both still open (see finding 6). And the PRD never uses the
`[NOTE FOR PM]` convention the rubric looks for; it carries its tensions in §18's
"Recommended:" lines and in SG-2/SG-3 instead, which is functionally better but leaves the
callouts un-greppable for a downstream reader who was told to look for them.

### Findings

- **[medium]** The PRD's own finalisation gate is unmet in a finalised document (§18
  preamble) — the section says the phase-blockers "make the PRD unsafe to hand to UX,
  architecture or story breakdown as-is, and are resolved or explicitly deferred before
  finalisation", yet OQ-8 and OQ-10 are both open in a document whose §0 says it exists "to
  be source-extracted by downstream workflows". A downstream extractor that honours the
  PRD's own reading of itself must stop, and nothing at the top of the document tells it to.
  *Fix:* either lift a one-line status into §0 ("two phase-blockers open: do not extract
  §4.2–§4.6 until OQ-8 and OQ-10 are decided") or restate the two OQs as explicitly
  deferred with the deferral recorded as the decision.
- **[low]** No `[NOTE FOR PM]` callouts at the real tensions (§9.2 SG-2/SG-3, §13 R16, §18) —
  the tensions are there and are named well, but the rubric's callout convention is absent
  entirely, so a reader scanning for deferred decisions by tag finds none.
  *Fix:* tag SG-2/SG-3 and OQ-8/OQ-10 with `[NOTE FOR PM]`; the content does not need to
  change.

## Substance over theater — strong

Every category the rubric asks me to police is clean. **Persona theater:** the five
journeys are load-bearing, not decorative — each is bound to features by a "Realizes UJ-n"
line and to a protagonist with an entry state, a climax and an edge case; §2.3 states where
each journey comes from ("UJ-1 from the sandbox site's documented six-step flow
(`DESIGN.md`), UJ-2 to UJ-5 from the intent record's own use cases U1, U2, U4 and U5"), and
the one use case that would have been decorative (U3, "a journey whose climax is a refusal")
is excluded with its reasoning in `addendum.md` D1.4. **Innovation theater:** the
differentiation claim was *narrowed* by later evidence rather than kept
(`addendum.md` D3: "'The only ZK verifier' is no longer sayable; the surviving claim is
narrower"), and NG-1 explicitly refuses the "nobody anywhere has done this" claim. That is
the opposite of the failure mode. **NFR theater:** all 17 NFRs carry product-specific
thresholds or named artefacts (`NFR-5` names the task CI runs and the count, `NFR-6` names
the instance type and forbids the 1.0 s figure, `NFR-9` names `demo-data/`), with no
"scalable / secure / reliable" boilerplate anywhere. **Vision theater:** §1's Vision could
not be swapped into another PRD — it is built on this fork's own facts (the AV Profile's
SHALL, the Estonian isikukood over-disclosure, the end-2026/2028 window).

The one observation worth recording is that the actor list in §2 ("Four actors follow from
it — a programme-adjacent assessor, an Estonian relying party and its integrator, the
holder, and the visitor to the sandbox site — plus the attestation provider") plus five UJ
protagonists sits above the rubric's "more than four personas" smell threshold. Here it is
not furniture: the fifth actor is a real dependency (§11 names the attestation provider's
counterpart as a stakeholder who owes the product a decision), and no persona invents a
decision the PRD does not otherwise make.

### Findings

- **[low]** The actor set is larger than the rubric's four-persona line, and one actor
  ("the attestation provider the record names for the threshold work") appears in §2 and is
  then never referenced by any UJ, SM or FR (§2) — every other actor has at least one
  journey or feature attached. *Fix:* either thread the attestation provider into NG-10/O6
  explicitly (it is the scheme-owner counterpart OQ-2 and OQ-7 depend on) or cut it from the
  actor list and leave it in §11 as the stakeholder it actually is.

## Strategic coherence — strong

There is a thesis and the prioritisation follows from it. §1's "So the product is the
evidence" resolves directly into the release order in §6.1 — the carrier suite is described
in §4.3 as "the cheapest credibility signal available, because the work is already done and
only needs running in public", and the release is built around evidence artefacts rather
than capability. The decision that follows from the thesis is stated in `addendum.md` D1.2
with the counter-argument preserved. Success metrics validate the thesis rather than
measuring activity: SM-1 through SM-5 all ask "was the evidence produced and reproduced by
someone else", the one SM that could have become a DAU-style activity metric (SM-4,
time-to-first-verification) is explicitly a distribution with a stated stopwatch method, and
**counter-metrics are named for every primary** — CM-1 (clause count is not the metric),
CM-2 ("external reproduction must mean external"), CM-6 ("honesty must not cost more than it
buys"). CM-6 in particular is the rarest thing in a PRD of this type: a counter-metric
aimed at the document's own virtue.

The gap against the rubric is small: the rubric asks for "coherent MVP scope kind —
problem-solving, experience, platform, or revenue — with scope logic that matches". §6's
scope logic is coherent and legible (an evidence/problem-solving scope), but it is never
named as a kind, so a downstream reader cannot tell whether the release is being judged as
a platform build.

### Findings

- **[low]** The MVP scope kind is never named (§6) — the logic is clearly problem-solving
  (produce third-party-reproducible evidence) rather than platform or revenue, but the
  section states scope contents without stating the kind it is optimising for.
  *Fix:* one sentence in §6's preamble naming the scope kind and the test it implies.
- **[low]** Q2's "defensible action is B2, not this number" asks the reader to act on a
  measurement programme that §6.2 defers out of v1 (§7.4, §6.2) — the sentence is honest
  about the estimate but points at an action the release does not take, which slightly
  weakens the "what do I do next" reading of §7.4.
  *Fix:* state that B2 is post-v1 and that v1's action is the OR-2 capacity statement.

## Done-ness clarity — adequate

This is the strongest mechanical property of the document and the weakest structural one at
the same time. Every FR (FR-1 … FR-44) has an explicit `**Consequences (testable):**` block
with verifiable conditions — I found no "handles X gracefully", no "reasonable performance",
no "user-friendly" in any FR (the single "degrade gracefully" in the document is in a JTBD
context sentence in §2.1, not a requirement). FR-11 ("at least 20 clauses"), FR-13 ("at
least 8 … negative controls"), FR-19 ("at least one external implementation") and SM-6's
count-based target are all countable. FR-8 is a model FR for brownfield done-ness: it names
the batch of three, the 7-day epoch from 1970-01-01, the 29 February rule and the renewal
window — and every one of those checked out against the code (see Shape fit).

The weakness is the escape hatch. At least six FRs in §4.2–§4.6 accept "or record that you
did not do it" as a passing state: FR-15 ("either exercised or explicitly recorded as out of
scope with the reason"), FR-22 ("or the absence of one is recorded as an unmet dependency"),
FR-24 ("or the review is recorded as not yet obtained"), FR-27 ("or an explicit 'unmeasured,
N integrators available' row"), FR-42 ("where the property cannot be claimed, it is recorded
as not claimed") and FR-23's adjacent "recorded as such". Individually each is defensible
for an evidence-first product; collectively they mean an implementer can satisfy the entire
§4.6 feature and a good share of §4.2–§4.5 by writing paragraphs. CM-5 partially guards this
("the banned-vocabulary check must not be satisfied by going vague") but only for the claim
discipline feature, not for the kits. The second weakness is that two FRs' acceptance is not
decidable by the implementer — FR-24's "An Estonian service owner has reviewed it" and
FR-27's "measured on at least three integrators" both need a partner that §18/OQ-10 says
does not exist. That is disclosed, but it is disclosed in §18 while §6.1 still lists the
resulting FRs as v1 deliverables with no marker.

### Findings

- **[medium]** Six FRs accept a written record of non-achievement as their satisfying state,
  so "done" is not decidable by the implementer alone (§4.2 FR-15, §4.3 FR-22, §4.4 FR-24
  and FR-27, §4.6 FR-42) — a release could meet every consequence in §4.6 and most of §4.2
  by documenting absence, and nothing in §7.1 distinguishes "kit built and exercised" from
  "kit built and honestly not exercised". *Fix:* split the state into two per FR — an
  unconditional deliverable and an explicitly nullable external outcome — and give the
  nullable half a single SM so the difference is visible in §7.
- **[medium]** FR-24 and FR-27 are listed in §6.1 as v1 deliverables while their acceptance
  conditions require an external partner that does not exist (§6.1, §4.4, §18/OQ-10) — the
  PRD discloses the dependency in §18 and recommends the remedy there, but the scope table
  and the FR consequences read as achievable as written. *Fix:* mark both in §6.1 as
  "conditional on a recruited partner (OQ-10)" so the scope table and the OQ agree in one
  glance.
- **[low]** FR-20's consequence "(a) documented rename/adapt plan exists" is testable only as
  file existence (§4.3) — it is the one consequence in the suite whose satisfaction cannot be
  distinguished from a stub. *Fix:* require the plan to name the specific
  `ZkRequest`/`ZkDocument` fields that would change and the single adaptation point.

## Scope honesty — strong

This is where the PRD is at its best. Omissions are explicit rather than inferable: 19
numbered non-goals, §6.2's out-of-scope items each carrying a stated *consequence* ("v1
makes no performance claim, and every device row stays `PENDING-DEVICE`"; "No operations
runbook, monitoring, incident process or availability target in v1; the absence is stated so
no reader assumes a service they can depend on"), §6.3's deferred work in the record's own
order, and de-scoping that is proposed rather than done silently. The `[ASSUMPTION: …]`
convention is used at five real tensions and indexed in §19 with a "What would settle it"
column — A-1 even records that the settling evidence is the user's own words and that the
question "went unanswered", and A-5's DPIA assumption carries its own revisit trigger
inline. `addendum.md` D5 discloses that market sizing is "deliberately absent" and why, and
§7.4 restricts itself to "the *only* effects figures this PRD permits".

Open-items density is high — 14 open questions, 9 indexed assumptions, a two-item
phase-blocker list — and the rubric's warning about high counts applies to green-light-to-
build PRDs. Here the count is proportionate and mostly *institutional* rather than
self-inflicted: OQ-2/OQ-3/OQ-4/OQ-5/OQ-6/OQ-7/OQ-9 are questions about bodies that do not
exist yet (a scheme owner, RIA, the ECCG, ISO), which the PRD cannot close by deciding
harder. The two it could close are exactly the two it flags as blockers. I am not treating
the density as a defect; I am noting that the PRD's own reading of it is correct.

### Findings

- **[low]** The escape-hatch clauses noted under Done-ness are a second, unlabelled form of
  de-scoping (see that dimension) — §6.2 names what is out of scope, but the "or record it
  as not obtained" branch quietly permits a requirement to de-scope itself after the
  release decision. *Fix:* as under Done-ness; the scope section itself needs no change.
- **[low]** §2.2's non-user list and §5's non-goals overlap without cross-referencing (§2.2,
  §5) — "Card-present venues doing a ZK age check" appears as a non-user and again as NG-6,
  and "anyone seeking certification" appears as a non-user and again as NG-3/NG-16, so
  genuine non-goals (NG-9, NG-15, NG-19) are harder to find among the restatements.
  *Fix:* have §2.2 point at the NG numbers instead of restating them.

## Downstream usability — adequate

The machinery for source extraction is in place and better than most: §3's Glossary is
declarative about its own authority ("Downstream workflows and readers use these terms
exactly … introducing a synonym anywhere in this PRD is a discipline violation"), its 34
entries include the two pairs that would otherwise drift (circuit registry vs accepted
circuit set, explicitly "Not to be confused with"; Tier 1 vs Tier 2; EE-PoA vs EE-PoA
Provider), and the ID space is clean — FR-1…FR-44 contiguous with no gaps or duplicates,
UJ-1…5, SM-1…8, CM-1…6, NG-1…19, NFR-1…17, OQ-1…14, R1…R16, all a single numbering per
family. Every UJ has a named protagonist with context inline. Each §4.x feature section can
be pulled out alone.

What keeps it at adequate rather than strong is that the document's own navigation contract
is the least reliable part of it. §0 tells the reader two things that are wrong about where
content lives (§9 and §8, below), points at `epics.md` with stale counts, and two FR
consequences carry cross-references to the wrong FR — and these are exactly the pointers a
story-creation pass follows. The downstream cost is small in volume (four references) and
material in kind, because a reader who follows "§8 Open Questions" to the NFR section may
reasonably conclude the disagreements are not recorded at all.

### Findings

- **[medium]** §0 points at the wrong sections for the two things it promises (§0) — "its
  inferences are tagged `[ASSUMPTION: …]` inline and indexed in **§9**" (the Assumptions
  Index is §19; §9 is Constraints and Guardrails) and "the disagreement is recorded in
  **§8 Open Questions**" (Open Questions is §18; §8 is Non-Functional Requirements). Both
  numbers are off by ten, which suggests a numbering shift that §0 did not follow. *Fix:*
  correct both to §19 and §18; grep for any other `§`-ten references while doing it.
- **[medium]** Two FR cross-references in consequence text resolve to the wrong FR (§4.1
  FR-6, §4.2 FR-16) — FR-6 says a refusal message is "distinguishable from … the user having
  cancelled (see also **FR-27**)", but FR-27 is the integrator time measurement and FR-26 is
  "A refusal is distinguishable from a cancellation"; FR-16 says the report "contains no word
  from the banned claim vocabulary (**FR-37**)", but FR-37 is the figure-labelling FR and
  FR-36 is the banned-vocabulary FR. Story creation reading either line lands on the wrong
  requirement. *Fix:* FR-27 → FR-26 and FR-37 → FR-36.
- **[medium]** §0's description of `epics.md` is stale in two of its three numbers (§0,
  §19 A-6, `addendum.md` D7) — it says "11 epics and 75 stories, with its own FR1–FR63 and
  NFR1–NFR10 numbering". The file holds 11 epics and **76** `### Story` headings, and its
  requirements inventory runs **FR1–FR66** (FR64 Certificate Transparency, FR65 cross-border
  onboarding, FR66 plain-fallback visibility, each with a coverage-map row). NFR1–NFR10 is
  correct. Because this is the PRD's only pointer to the sibling artefact and A-6
  recommends the two be read side by side, the error is in the one place a mapping reader
  would trust. *Fix:* update §0, A-6 and D7 to "76 stories" and "FR1–FR66", or state the
  counts as "as of the commit this PRD names".
- **[low]** The Assumptions Index roundtrip is one-directional (§19) — all five inline
  `[ASSUMPTION: …]` tags (lines 82, 160, 1016, 1063, 1454) are indexed as A-1…A-5, but A-6,
  A-7, A-8 and A-9 have no inline tag anywhere in the body, so four of the nine entries
  cannot be found from the text they qualify. *Fix:* add the inline tags in §0 (A-6, A-8),
  §6.3 (A-7) and §1/§6 (A-9), or mark the four as index-only assumptions.

## Shape fit — adequate

The brownfield shape is handled deliberately. FRs are marked **(shipped)** or **(new)**
against a stated convention ("FRs marked (shipped) describe behaviour the repository already
has and that this PRD protects from regression — for a brownfield product those are
requirements too"), each FR cites the symbols that hold the behaviour, `addendum.md` D1.4
explains what was **not** carried as a journey and why, and §16 tables the surfaces with
their real constraints (`:app:installLocal_mocks` for a device, `debug` fails onboarding
after PIN entry; `-Dzk.regenerate=true` as the only way fixtures change). The regulatory
shape is also right for this product type: constraint traceability is present to the clause
level (`ZKP_08`, `EE-POA-026`, `EE-ZKP-055a`, `DASH_03a`, `EE-CNF-005`) and UJs are kept
thin rather than forced onto an assessor audience — `addendum.md` D6 explicitly declines a
journey for the assessor and the institutional readers.

I spot-checked five (shipped) references against the code, and four of five are accurate to
a level that is unusual in a PRD:

- **FR-1** — "13 assertions, both response modes, both carriers, one- and two-credential
  sessions" in `tests/e2e.sh`: the file has exactly 13 `pass "…"` assertions covering both
  modes, the tamper/plain/two-credential cases. Accurate.
- **FR-3** — "the existing `vptoken_test.go` 9-case table": the table in
  `verifier/go/oid4vp/vptoken_test.go` has exactly 9 cases (CBOR true, CBOR false, non-hex,
  integer, empty, wrong doctype, wrong namespace, wrong attr_id, unsupported zk_system).
  Accurate.
- **FR-4** — `AgePredicatePolicy` refusing multi-predicate requests "on the OpenID4VP, DC
  API and proximity paths": the policy exists and is wired into
  `PresentationRequestViewModel.kt`, `DigitalCredentialsViewModel.kt` and
  `ProximityViewModel.kt`. Accurate.
- **FR-8** — `AgeIssuanceConstants.validity` flooring to a 7-day epoch from 1970-01-01,
  `AgeReckoning.effectiveBirthDate` with the 29 February rule, batch of three,
  `lastAttestation`, `AgeAttestationRenewal` with its 14-day lead: all present in
  `domain/provider/ageverification/` with the stated constants (`VALIDITY_EPOCH_DAYS = 7`,
  `EE_POA_MAX_VALIDITY_DAYS = 90`, `LEAD_DAYS = 14`) and the class comment literally says
  "1970-01-01". Accurate.
- **SM-8 / SG-1** — false as written. See the finding below.
- **FR-38 / §4.6** — verified in the PRD's favour: `docs/CONFORMITY.md`:160 does say the
  `issuerAuth` row is "absent — mdoc credentials load with no issuerAuth chain check", while
  `CredentialToDocumentMapper.kt` performs on-device `issuerAuth` verification (signature
  against the presented leaf plus an IACA anchoring check, tagged "E6"). The PRD's claim
  that the documentation contradicts the code is correct.

What the dimension is missing is the distinction the rubric names explicitly for brownfield
PRDs: "new UJs and existing UJs must be distinguished". §2.3 presents all five journeys
uniformly. Three of them (UJ-1 on the shipped sandbox site, UJ-2 on the shipped spine, UJ-4
on FR-7 which is marked shipped) depict behaviour that exists today, while UJ-3 and UJ-5
partly or wholly depict v1 work. A UX or story pass cannot tell which journeys are
descriptions of existing surfaces and which are specifications of new ones.

### Findings

- **[high]** SM-8's baseline and SG-1's TIER 1 list assert two repository facts that were
  false before this PRD was written (§7.2 SM-8, §9.2 SG-1) — SM-8 states
  "`CertificateChainValidator` — the PKIX trust gate — has **zero direct unit tests today**,
  `:zk-conformance` runs in **no CI workflow**, and the SD-JWT test asserts nothing", and
  SG-1 repeats the first as a TIER 1 audit finding. In fact
  `app/src/test/kotlin/ee/cyber/wallet/security/CertificateChainValidatorTest.kt` exists with
  14 `@Test` cases exercising the real PKIX validator over locally minted chains, and
  `./gradlew :zk-conformance:test` runs in `.github/workflows/app.yml` (with an explicit
  comment explaining why it rides the app workflow). Both landed on 2026-09-25 in commits
  `d909a35` and `e6c2ce5`, i.e. after the audit commit `732d57e` that the claims come from;
  the third claim is still true (`EudiSdJwtTest` carries `// TODO: fix this test` and
  asserts nothing). The PRD inherited the audit's counts without re-deriving them at its own
  commit `2d38feb`, which is precisely the failure mode FR-38, NFR-16 and R12 say the
  release exists to fix — and it is the one class of error a reader of a document whose
  differentiator is "every claim is checked against the code" will treat as disqualifying.
  *Fix:* re-check the audit's per-item claims against `2d38feb` before publication, correct
  SM-8's baseline to the two remaining gaps (SD-JWT assertions, and anything the re-check
  finds), and add the date-or-commit qualifier to SG-1's TIER 1 list so inherited audit
  findings cannot read as today's state.
- **[medium]** New and existing user journeys are not distinguished (§2.3) — the PRD marks
  FRs (shipped)/(new) but presents UJ-1…UJ-5 as a single set, even though three of the five
  describe shipped surfaces and two describe v1 work; the rubric requires the distinction
  for brownfield documents. *Fix:* add a "(shipped surface)" / "(v1)" label to each UJ
  heading, or one line in §2.3 naming which journeys are descriptions and which are
  specifications.
- **[low]** §4's preamble calls the cited symbols "file names", and one of them is not a
  file (§4 intro, FR-8) — "the file names beside them are the code that holds them", but
  `AgeIssuanceConstants` is an object declared inside
  `domain/provider/ageverification/AgeVerificationProviderServiceMock.kt`; there is no
  `AgeIssuanceConstants.kt`. Every other citation (`AgeReckoning.effectiveBirthDate`,
  `HolderObligations.refusePlainFallback`, `AgePredicatePolicy`, `DcqlRequestProcessor`,
  `AgeAttestationRenewal`, `lastAttestation`, `carrier.go`, `TranscriptForFlow`) resolves.
  *Fix:* say "the symbols beside them" rather than "the file names", or make FR-8 cite the
  containing file.

## Mechanical notes

- **ID continuity.** FR-1…FR-44, UJ-1…5, SM-1…8, CM-1…6, NG-1…19, NFR-1…17, OQ-1…14, R1…R16,
  PG-1…7, SG-1…6, CG-1…3, OR-1…7, DG-1…7 all contiguous, no duplicates, no gaps. FR-44 is the
  highest FR and there are 44 FR headings.
- **OQ ordering.** §18 opens with OQ-8 and OQ-10 (the phase-blockers) before OQ-1…OQ-7, then
  resumes numerically. Deliberate and signposted, but a reader following the numbers meets
  them out of order; a "(blockers, listed first)" note or a renumber would remove the hitch.
- **Broken cross-references.** Two wrong FR references (FR-6 → FR-27 should be FR-26; FR-16 →
  FR-37 should be FR-36) and two wrong § references in §0 (§9 → §19; §8 → §18). All four are
  recorded as findings above rather than repeated here.
- **Assumptions Index roundtrip.** Inline → index is complete (5/5). Index → inline is 5/9:
  A-6, A-7, A-8 and A-9 have no inline `[ASSUMPTION]` tag.
- **§19 A-8's count is reproducible but narrower than stated.** "211 unique
  `EE-<AREA>-<nnn>` identifiers across 14 families" reproduces exactly under a three-digit
  reading (`EE-[A-Z]+-\d{3}`, suffix letters kept): 211 across ZKP 38 · POA 35 · PRO 20 ·
  SEC 18 · PRI 18 · SIG 16 · GOV 14 · ONB 11 · PID 9 · WUA 8 · REV 7 · CNF 7 · RP 6 · ACC 4,
  matching A-8's enumeration figure for figure — so the number is sound. But the
  specification also contains a fifteenth family, `EE-AGE-1` and `EE-AGE-2`, which use
  one-digit numbers and are therefore excluded by the unstated rule; a reader grepping for
  `EE-<AREA>-<nnn>` gets 213 identifiers in 15 families, not 211 in 14. Since A-8's "What
  would settle it" column says "Nothing — this is verified; recorded so no reader re-opens
  it", the exclusion should be stated rather than left to be rediscovered.
- **Evidence labels are used and not upgraded.** Spot-checks found no case of a label being
  strengthened: the `[VERIFIED as absent]` tags in NG-6 and NG-17, the
  `[UNVERIFIED — absence of evidence]` on the procurement award in §17, the
  `[ESTIMATE]`/confidence pairing in §7.4 and the `[JUDGEMENT]` on §1's reproducibility
  warrant all read as the record's own strength. The two exceptions to that discipline are
  factual errors rather than label upgrades, and are recorded above (SM-8/SG-1, §0's
  `epics.md` counts).
- **Required sections.** §0–§19 present, with the rubric's expected sections all carried:
  Vision, Target User + JTBD + Non-Users + UJs, Glossary, Features with FRs, Explicit
  Non-Goals, MVP Scope with in/out/deferred, Success Metrics with counter-metrics,
  NFRs, Constraints and Guardrails, Compliance, Stakeholders, Integration, Risk,
  Operational, Data Governance, Platform, Why Now, Open Questions, Assumptions Index.
- **Documented-but-unverified claims.** None found that the PRD presents as verified. The
  claims it cannot check (Article 45b(2)'s reading, the procurement award, the ISO second
  edition's text, the Riigi Teataja text, the mirror's contents) are each tagged, and each
  appears in §18 or §19 as well as inline.
