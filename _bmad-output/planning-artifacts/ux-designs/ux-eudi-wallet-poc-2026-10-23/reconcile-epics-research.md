# Reconciliation: the UX run's decisions against `epics.md`, the research corpus and `PRODUCT.md`

**Date:** 2026-10-23 · **Decisions reconciled:** all 37 entries in this run's `.memlog.md`
(canonical), read in full. · **Inputs:** `_bmad-output/planning-artifacts/epics.md` (**EPICS**),
`docs/research/2026-10-23-innovation-assessment.md` (**IA**),
`docs/research/2026-10-23-estonia-alignment-assessment.md` (**EA**),
`docs/research/2026-10-23-shared-appendices.md` (**SA**), `PRODUCT.md` (**PT**).

**Method.** Source-first. Every input was read; each decision that names, rests on or contradicts
these five documents was then checked line by line (grep + read) against them. Line numbers are
`file:line`. This document records only what the inputs say and what the decisions do; **no finding
is inferred beyond a quoted line**. Where a dropped item is nevertheless carried by the PRD (a
different input group) that is stated so no reader double-reports it.

**Excluded by instruction.** The backlog gap — EPICS has no story for the sandbox site or for the
four wallet consent gates (already logged at `MEM:28`; corroborated: "sandbox" occurs once in EPICS,
at `EPICS:171`) — is not re-reported.

## Counts

| tier | meaning | count |
|---|---|---|
| **critical** | a decision asserts what an input contradicts, or the run's own artefact would state something a source refutes | **1** |
| **high** | a load-bearing input duty, acceptance criterion or interface the run would never address | **4** |
| **medium** | a caveat, criterion, constraint or surface lost or shifted; no contradiction | **7** |
| **low** | detail, label or binding lost; nothing load-bearing | **3** |
| | **total findings** | **15** |

Also corrected: **3 premises of the brief that the inputs do not support** (§ Corrections), and
**2 citation defects** in the run's own extraction (§ Citation integrity). Seventeen further
decision/input pairs were checked and found **clean** (§ Checked and clean).

---

# CRITICAL

## C-1. The privacy-wording rule "at most one predicate" contradicts the backlog's certificate-driven two-predicate path

**Decision.** `MEM:16` — "Privacy wording PG-1..PG-7: **at most one predicate, a boolean and
nothing else**; no false predicate transmitted; …"

**Input.** `EPICS:104` — "FR18: A two-predicate request allowed only when the relying party's
registered intended use names both predicates." `EPICS:714-729` (Story 4.3, *Certificate-driven
two-predicate decision*, **Covers:** FR18) — `EPICS:725`: "**Then** it is allowed and **the user sees
an explanation of the revealed range**". `EPICS:728`: "a certificate naming both predicates does not
override the held-false rule". The opportunity behind the epic states the same change of state:
`IA:341-342` — the wallet today "refuses requests that would disclose more than one age predicate
*unless* \"the relying party's registered intended use names each of them\", and records plainly that
the wallet does not read registration certificates, so such a request is \"always refused here\"",
and `IA:361-364` lists the affected components as "*`app/.../domain/presentation/`
(`AgePredicatePolicy`, `HolderObligations`, **presentation screens**)*".

**Why critical.** A rule the spine will state as product-level privacy wording ("one predicate, a
boolean and nothing else") is false against this repository's own backlog, and the conflicting
state is a **user-facing** one — the two-predicate case has an acceptance criterion about what "the
user sees". EPICS reaches it deliberately (`EPICS:1850-1851`: Epic 4 is "the second wave"), and the
PRD scopes the same ground explicitly (`prd.md:424` "**Out of Scope:** a lawful two-predicate
request"; `prd.md:1082` NG-9), so this is a **wording-scope defect, not a v1 behaviour defect**: the
rule needs a "for v1" qualifier plus a pointer to Story 4.3, or the spine will contradict the
backlog it is meant to serve.

---

# HIGH

## H-1. The acceptance kit's four cases are fewer than the backlog's five, and drop two `EE-ZKP-055a` shapes

**Decision.** `MEM:10` — "(4) RP acceptance kit integrator experience — one install path, **four
acceptance cases**, refusals that name their cause, measured time-to-first-verification";
`MEM:36` — "one documented linear install path, then **four** self-contained re-entrant acceptance
cases".

**Input.** `EPICS:586-607` (Story 3.5, *Four Estonian acceptance cases*, **Covers:** FR13) requires
four cases **and** — `EPICS:599-603` — "**And** a **fifth case is recorded** for the state two
faithful implementations of FR22 and FR23 would otherwise deadlock on: the verifier resolves circuits
from its pinned local fallback while the wallet has no fetched registry, so the wallet's resolution
falls back to the same pinned document version rather than refusing the ZK path while the verifier
waits", **and** — `EPICS:604-607` — "the two remaining unauthenticated shapes `EE-ZKP-055a` governs
are covered as well as the least-trusted one: an `org-iso-mdoc` request without `readerAuth` chaining
to a trusted access CA, and a proximity request without `readerAuth`, each refusing a plain-mdoc-only
request on a ZK-capable device". FR14 (`EPICS:100`) and Story 3.6 (`EPICS:610-626`) carry only the
refusal/cancellation/error distinction, which `MEM:14` does cover.

**Why high.** The kit's information architecture is the run's deliverable surface for FR13, and it is
stated as an inference ("`[ASSUMPTION]` … carried unless Tom objects"). As written it is narrower
than the acceptance criteria the kit must satisfy — a fifth, *recorded* case and two named refusal
shapes are missing. (No PRD story supplies the fifth case either: grep over `prd.md` for
`fifth|deadlock` returns nothing.)

## H-2. Epic 4 is neither a surface nor an out-of-scope item, and two of its acceptance criteria are user-facing

**Decision.** `MEM:10` names four surfaces; `MEM:11` lists seven things with "no screens designed
for": proximity Tier-2 ZK, consent/activity dashboard (EE-PRI-014), status lists/revocation, real
issuer onboarding, circuit-registry service, statutory thresholds beyond 18, EU-level certification
flow. The registration-certificate policy engine appears in **neither**.

**Input.** `EPICS:668-812` (Epic 4 — its stories cover FR16–FR20), whose user-visible acceptance criteria are
`EPICS:725` ("the user sees an explanation of the revealed range", Story 4.3) and `EPICS:736-752`
(Story 4.4, *Topic 19 log fields from the certificate*) — `EPICS:746`: "the DASH_03 fields are
populated from the certificate"; `EPICS:749-750`: "a named field that the certificate does not carry
is logged with an explicit absent marker, never left blank". The epic is not only code: `IA:361-364`
lists "presentation screens" among its affected components, and `EPICS:186-190` says the backlog's
own UX section is empty precisely because FR19 (Topic 19 / DASH_03, Epic 4) and FR62 (accessibility,
Epic 11) "are carried as implementation stories rather than UX specification work".

**Why high.** The run's scope statement is exhaustive-looking but silent on one of the two items
EPICS itself flags as user-facing, and on an epic whose acceptance criteria describe what a holder
sees and what is recorded for them. Out-of-scope is a fine answer; **unmentioned** is the defect.

## H-3. Two of `PRODUCT.md`'s three product principles, and the sandbox shortcut they require naming, are carried by no decision

**Input.** `PT:56-60` — "## Product Principles / 1. Show, don't claim: every number on the page comes
from the run that just happened. / 2. **Say what the shop did not learn, as plainly as what it did.**
/ 3. **Name the sandbox's shortcuts where they happen, not in a footnote.**" Plus `PT:38-40`: "The
sandbox \"Population Register\" computes age predicates from a typed birth date. That is the
trust-the-client enrolment weakness EE-POA-006/007 warns about, **and the site says so**", and
`PT:17-19`: users "arrive cold and leave understanding what the shop learned and what it did not".

**Decisions.** Principle 1 **is** carried, in its own wording: `MEM:13` — "a number appears on a
surface only if it came from the run that just happened (FR-28)". Principles 2 and 3 are not.
`MEM:16` carries the *fact* ("a boolean and nothing else … issuer-chain (`msoX5chain`) disclosure and
the Tier-1 linkability limit are stated, not hidden; never overstate the privacy position") but not
the duty to say, on the surface, what was **not** learned; no decision carries the in-place naming of
the Population-Register shortcut (verified: `population register` and `trust-the-client` occur **0**
times in IA, EA, SA and EPICS, once each in PT).

**Why high.** These are the product's own principles for the site the run calls surface (1), and
principle 3 is an honesty duty the product text states as an implementation fact. Carried elsewhere:
`prd.md:1667` (principle 2 as a PRD principle) and `prd.md:811-814` ("The synthetic Population
Register's client-side age computation is named at the point it happens as the trust-the-client
enrolment weakness the spec warns about"), so this is a decision-level drop rather than a product
loss — but the run's copy constraints, which are where such duties land, do not mention either.

## H-4. The corpus's accessibility position — status *partial*, no EN 301 549 evidence, audit as the required change — is dropped from the accessibility floor

**Decision.** `MEM:17` — "Accessibility floor NFR-12: WCAG 2.1 AA stated, EN 301 549 v3.2.1 and
Directive (EU) 2019/882 as the binding instrument; keyboard-complete; visible focus; contrast held by
existing tokens; no colour-only signalling; announcements for long-running steps; reduced motion
honoured". `MEM:7` adds the accessibility lens at the Reviewer Gate.

**Input.** `EA:189` (row A13, status **partial**) — "PRODUCT.md commits to WCAG 2.1 AA; reduced-motion
and screen-reader announcements implemented; **no ETSI EN 301 549 test evidence**", required change
"an accessibility conformance test pass against EN 301 549", verification "third-party accessibility
audit". `EA:220` (row E5, **partial**) — "committed in PRODUCT.md; implemented browser-side",
required change "audit evidence". `SA:96` (index row A13 = partial). `SA:182` (risk R13) —
"**Accessibility duty unmet at audit time** (WCAG 2.1 AA claimed; no EN 301 549 evidence)".

**Why high.** The decision states the standards and the implemented behaviours but records no
evidence gap, which is exactly the state the corpus marks partial three times. `PT:62-65` states the
commitment without qualification either, so the spine is the surface where the corpus's caveat has to
land — and the run's own rule (`MEM:13`, "say only what the tests support in this environment")
already implies it. Carried elsewhere: `prd.md:1358`, `prd.md:1529-1530` ("No EN 301 549 audit
evidence exists yet (R13)"). Existing EPICS story: `EPICS:1711-1730` (Story 11.3).

---

# MEDIUM

## M-1. The index and catalogue duties on the run's new artefacts are not carried

`EPICS:181` (Additional Requirements) — "Plan records land in `docs/planning/` and **every document
is indexed in `docs/README.md`**; new documents introduced by these stories must be indexed there";
`EPICS:182` — "Tests and what each proves are catalogued in `tests/README.md`; new suites (AV clause
runner, carrier runner, threshold boundary tests) must be added to that catalogue". `MEM:31` commits
the conformance report "to the repo" and `MEM:36` defines the kit's install path, but no decision
mentions either duty (grep: `docs/README` and `tests/README` appear 0 times in `.memlog.md`). The
report and the four-case kit are new documents/suites under both rules. Carried elsewhere:
`prd.md:1233`, `prd.md:1658` (SM-7).

## M-2. The synthetic-persona labelling duty is not carried

`PT:54` — "Test personas are synthetic and labelled as such." No decision mentions personas or their
labelling; `MEM:12` names the five journey personas without it. Carried elsewhere: `prd.md:1623-1624`
(DG-1), `prd.md:314`, `prd.md:814`.

## M-3. `PRODUCT.md`'s three positioning figures are unmatched to the run's number rule

`PT:30-32` — "The proof is real: a 360 KB Longfellow proof generated on the server in about six
seconds and verified by the Go verifier in about two and a half seconds"; `PT:53-54` — "Measured
figures in `README.md` and `verifier/README.md` (prove 6.1–6.5 s, verify ~2.5 s, proof ~360 KB)".
Against `MEM:13` ("a number appears on a surface only if it came from the run that just happened
(FR-28); unmeasured figures are absent …; v1 makes no performance claim at all") and `EPICS:163`
(NFR2 — "Every published figure states its baseline, method, assumptions and confidence"). No
decision says which of `PT`'s three figures may appear on the site and with what baseline label;
`PT:30-32` reads as a claim about host hardware that survived the deletion of its provenance
(`MEM:13` says "the run that just happened", while `PT`'s figures are the ones recorded in
`docs/MEASUREMENTS.md` — the 2026-09-25 block cited at `SA:202` — and in the two READMEs).
The PRD keeps them as explicitly host-scoped (`prd.md:1340`), so this is a labelling decision the run
still owes.

## M-4. Tier labelling at the point of presentation is not carried

`EPICS:801-805` (Story 4.7, **Covers:** FR66) — "… and **Tier 2 is labelled in the UI**" / "**Then**
the fallback remains visible in logs and the interface for each of those three shapes" / "**And** the
record states which tier was used and why". `EPICS:152-158` (FR66) — "The plain fallback stays
visible and refusable". Corpus: `EA:178` — "plain path **labelled as linkable in the UI**
(`PresentationTier`)" (row A2, partial); `SA:176` (risk R7) — "the fallback must remain visible in
logs and UI". `MEM:10` names "FR-6/FR-26 refusal reason surface" — and FR-6 in the PRD is "Weaker
requests are refused, with the reason shown **and the tier labelled**" (`prd.md:453`) — so the duty
is reachable only by reference: the decision text enumerates the reason, never the tier label, and
`MEM:16` carries the Tier-1 linkability *limit* in copy rather than the per-presentation label.
The phrase "Tier 2 is labelled in the UI" occurs once in EPICS (`EPICS:801`). The run's own
extraction surfaced the same duty as gap G-6 (".working/extract-epics-research-product.md":
"Tier labelling (`epics 8.11`) and refusal-vs-cancellation (`epics 3.6`) are the only two
UI-contract duties with stories, and both are worded for the RP/verifier path"); no decision
took it up, and the story id in that gap note is wrong (see Citation integrity).

## M-5. Corpus use case U3's acceptance criterion and risk R6's usability half have no covering decision

`EA:303-310` (U3, offline/proximity age proof at a venue) — "**Acceptance criteria:** on-device
measurement of BLE transfer for a ~360 KB proof inside a stated budget; **refusal when a ZK response
is impossible, with no silent downgrade**". `MEM:12` excludes the venue use case as a journey with a
reason, but no decision carries "no silent downgrade" as a designed state (the generic refusal
pattern at `MEM:14` does not name it). `SA:175` (risk R6, type "technical / **usability**") — "Proof
size (~360 KB) breaks a proximity or mobile-network budget"; the run carries the figure-discipline
half (`MEM:13`) but not the usability half. EPICS pairs the same exclusion with a pinned consequence
at `EPICS:961-962` ("proximity Tier 2 is withheld for that device"), which is consistent with
`MEM:12`.

## M-6. Story 9.5's user-visible refusal reason sits inside an epic the run declares out of scope

`EPICS:1527-1538` (Story 9.5, **Covers:** FR53) — "I want a refusal with a reason, never a
neighbouring threshold" / "**Then** it is refused with a **user-visible reason**"; `EPICS:139`
(FR53). `MEM:11` puts "statutory thresholds beyond 18" out of scope. The run's refusal pattern
(`MEM:14`) is generic — distinguishable cause, fail closed, name the lookup that failed — and does
not name an unmapped-threshold state, so the one interface element inside the excluded epic is
unaddressed rather than deferred.

## M-7. Story 11.3's interface-scoping sentence and EPICS's a11y binding are not carried

`EPICS:1724` (Story 11.3) — "**And** the report states which parts of the interface were and were not
in scope." That is a document-surface obligation on an audit report the run's surfaces do not name.
`EPICS:282` (NFR binding table) — "NFR10 WCAG 2.1 AA baseline | **11.3, 11.7** | Story 11.3's audit
report": EPICS binds the WCAG baseline to a data-minimisation story (11.7, `EPICS:1791`) and an audit
story, not to the consent gates the run designs (`MEM:15`, `MEM:17`). The run's per-surface floor
does not note the mismatch, so the two documents currently disagree about what carries the wallet's
accessibility duty.

---

# LOW

- **L-1. "The Digital Credentials API path is the mandated one" carries no qualifier.**
  `MEM:19`. Accurate against the AV Profile (`EA:32-33`, `IA:50` "transport of the proof over the
  W3C Digital Credentials API … \"An implementation SHALL use that system\"") — but the DC API itself
  is a Working Draft (`EA:201`, `SA:103`: "W3C Digital Credentials API (Working Draft 4 Sep 2026)")
  while the carrier's binding is unpinned (`IA:500-501` "the carrier may be superseded by whatever
  ISO second edition and DCHP settle on"; `SA:237` "OpenID for Verifiable Presentations 1.0 … **no ZK
  text**"). The copy ban covers the profiled carrier's unofficial status and is silent on this
  asymmetry, which is the one a stranger could read as an overstatement under `MEM:16`'s own
  "never overstate the privacy position".
- **L-2. The cold-arrival intent is carried only for the RP kit.** `PT:17-19` — users "arrive cold
  and leave understanding what the shop learned and what it did not"; `MEM:10` applies "design for a
  stranger with no support channel (OR-6)" to the acceptance kit only, and no decision states the
  equivalent bar for the sandbox visitor, who per `PT:6-7` is the product's platform.
- **L-3. `PRODUCT.md`'s evidence provenance is not carried.** `PT:53` — "Measured figures in
  `README.md` and `verifier/README.md`"; the run's number rule (`MEM:13`) does not name the files or
  the 2026-09-25 device block (`SA:202`) that would let a reader check an inherited figure against
  `EPICS:163`'s baseline/method/assumption/confidence requirement.

---

# Corrections: three premises the inputs do not support

These are stated so the brief's expectations are not propagated into the spine.

1. **`EE-ACC-004`, `EE-PRI-014` and `EE-PRI-023` are not corpus items.** All three appear **0 times**
   in IA, EA and SA — and 0 times in EPICS and PT (grep, exact match, all five files). Their source
   is the specification: `spec/EE-EUDIW-TS-1.0.md:1170` (`EE-ACC-004`, SHOULD, wallet available in
   Estonian, Russian and English), `:982` (`EE-PRI-014`, SHALL, the Art. 5a(4)(d) dashboard) and
   `:1075` (`EE-PRI-023`, SHALL, no central age log). **All three are carried by decisions**:
   `EE-ACC-004` at `MEM:18` ("the tri-lingual duty (EE-ACC-004) is unmet and the spine states what
   the app actually offers"), `EE-PRI-014` at `MEM:11` ("consent/activity dashboard (EE-PRI-014
   deferred)" — deliberately out of scope), `EE-PRI-023` at `MEM:16` ("no central age log (EE-PRI-023
   telemetry carve-out stated accurately if telemetry is mentioned at all)"). No dropped-corpus-item
   finding can be substantiated for any of them; the PRD carries all three in the same sense
   (`prd.md:1355`, `:1360`, `:1399-1401`).
2. **`PRODUCT.md`'s three product principles are not uniformly dropped.** Principle 1 is carried
   verbatim in substance at `MEM:13`; principles 2 and 3 are not (finding H-3).
3. **`PRODUCT.md`'s accessibility commitment is carried in full.** `PT:64-65` — "Estonian
   public-sector baseline: WCAG 2.1 AA. Keyboard-complete, screen-reader announcements for
   long-running steps, and reduced-motion support" — corresponds item for item to `MEM:17`
   (including the shipped proof-mark behaviour). What is missing is not the commitment but its
   evidence status (finding H-4).

---

# Citation integrity: two defects in the run's own extraction

`MEM:22` records the extraction as "verified byte-faithful". The following are line-reference defects
only — the extract's *content* was not re-verified against `epics.md` in full — but they matter
because downstream artefacts cite the extract's numbers.

1. **EPICS line numbers in `.working/extract-epics-research-product.md` no longer resolve.** The
   extract states `epics.md` (1,792 lines) with mtime 05:21; the file on disk is **1,910 lines**
   (mtime 05:23), so it was edited after the extraction and every later citation drifted — e.g. the
   extract cites `epics.md:582` for the Story 3.6 heading (line 582 is Story 3.4's closing line;
   Story 3.6 is at `EPICS:610`) and `epics.md:907` for Story 6.3's acceptance criteria (Story 6.3 is
   at `EPICS:993`). **Every line reference in this document was re-derived against the current
   file.**
2. **The extract cites a story that does not exist.** `.working/extract-epics-research-product.md`
   attributes the plain-fallback visibility guard to "epics 8.11 (`epics.md:1345`)". Epic 8 runs
   8.1–8.9, and `EPICS:790-810` is Story 4.7 in Epic 4 — which is also what the coverage map says
   (`EPICS:262`: "| FR66 | 4.7 |"). No Story 8.11 exists (grep).

---

# Checked and clean (do not re-open)

1. **`MEM:13` copy constraints** against `EPICS:162` (NFR1), `EPICS:119` (FR33) and `EA:145`
   ("an \"is this compliant?\" answer cannot be given at all; compliance language must be avoided") —
   consistent, and stricter than EPICS.
2. **`MEM:14` refusal constraints** against `EPICS:100` (FR14), `EPICS:610-626` (three distinct
   states, including "a technical failure, a timeout or an aborted session" at `EPICS:622`),
   `IA:305-309` (O2 acceptance criterion ii) and `EA:329` (U5) — consistent; the run's three states
   match EPICS's three.
3. **`MEM:17` accessibility standards** against `EPICS:171` (NFR10), `PT:64-65` and `EA:189`
   (which names Directive 2016/2102, Directive (EU) 2019/882 and the EN 301 549 presumption) —
   consistent; only the evidence status is missing (H-4).
4. **`MEM:18` language per surface** against `PT:42` ("Bilingual: Estonian and English, with a
   toggle") — consistent; the research corpus contains **no** language material (grep for
   `bilingual|language toggle|estonian-first` = 0 hits in IA, EA, SA), so no corpus position is
   contradicted.
5. **`MEM:19` independence notice** against `PT:48-49` ("Not RIA's product: no state coat of arms and
   no RIA logo. Every page carries the independence notice") — consistent, and the run extends "every
   page" to every surface. (`independence notice` = 0 hits in IA, EA, SA, EPICS.)
6. **`MEM:20` identifier discipline** — verified against the file: EPICS contains exactly 66 `FRn`
   and 10 `NFRn` inventory rows, and the run's PRD-side `UJ-n/FR-n/NFR-n/SM-n/…` scheme is disjoint.
7. **`MEM:21` DESIGN.md collision** — `PT:46-47` ("Visual world pinned by the user: copy or infer from
   ria.ee, the TEDI design system (TEHIK). Roboto, TEDI colour tokens, Material Symbols icons") is the
   sandbox's brand source and is not touched by the run's import decision.
8. **`MEM:26-27` (report) and `MEM:31-33`** against `EPICS:298-313` (Story 1.1: "every clause resolves
   to a test id or an explicit \"out of scope for v1\" note"), `EPICS:335-361` (Story 1.3: per-clause
   `ERROR`/`NOT_EXECUTED` states, "never `PASS`"), `EPICS:394-411` (Story 1.6: published with the
   regenerating commands, "including the failures"), `EPICS:162` (NFR1) — consistent, including the
   run's "no compliance vocabulary" rule and profile ordering. Report language (EN-only, `MEM:33`) is
   consistent with `PT:42` being scoped to the sandbox site (`PT:5-13`).
9. **`MEM:30` degraded boot** against `PT:41` ("Needs the built prover and verifier on the host
   (`make deps`)") — consistent: the state names `make deps`, so the constraint is stated, not
   removed. No EPICS or corpus requirement covers this state (no story; `MEM:23` already logs it).
10. **`MEM:34` wallet visual stance** against EPICS (no theming requirement exists: grep over EPICS
    for `Material|theme|compose` returns three unrelated lines, `EPICS:20`, `:935`, `:1823`) and
    `PT` (brand commitments scoped to the sandbox's visual world) — consistent.
11. **`MEM:37`/`MEM:41` sandbox tri-state** against `EPICS:619-624` and `EA:329` — consistent (see 2).
12. **`MEM:39` FR-45 unverified-requester semantics** against `IA:305-309` (O2 acceptance criterion
    ii: refusal "distinguishable from cancellation in the UI contract") — consistent; no EPICS or
    corpus requirement is contradicted (`MEM:28` already logs the missing story).
13. **`MEM:40` FR-47 lockout surface** against `EA:170` (row L5: "PIN verification" present, status
    partial, required change "mobile-device security requirements read directly from the CIR") and
    `EPICS:1648-1665` (Story 10.5) — consistent; neither carries a lockout interface, so the run adds
    rather than contradicts.
14. **`MEM:7` (accessibility lens as a gate)** against `EA:189`'s verification method
    ("third-party accessibility audit") — consistent as a process decision; the audit itself is H-4.
15. **`MEM:11`'s exclusion of the circuit-registry service, status lists and real issuer onboarding**
    against `EPICS:813-931` (Epic 5), `EPICS:1564-1647` (Epic 10) — consistent: those epics carry no
    holder-facing acceptance criterion (Epic 5's are service/log artefacts at `EPICS:822-931`;
    Story 10.4 and 10.5 are fixtures and a matrix), so excluding their screens contradicts nothing.
16. **`MEM:29` (Material 3, light-only, no custom typography as a technical constraint)**, and the
    EPICS "Definition of Done" (`EPICS:284`) — no interaction; not UX-relevant, recorded so no reader
    looks again.
17. **EPICS Epics 7's written statements (FR33–FR38, `EPICS:1127-1231`)** — the run's copy ban
    (`MEM:13`) covers their vocabulary; the statements themselves are repository documents, outside
    the run's four surfaces. Not a drop.