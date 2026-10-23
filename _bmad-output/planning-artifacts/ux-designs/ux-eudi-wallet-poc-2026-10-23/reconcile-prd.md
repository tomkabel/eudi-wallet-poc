# Reconciliation: PRD + addendum against the UX run's decisions

**Purpose.** Report (1) every `.memlog.md` decision that touches PRD content and whether it
contradicts the PRD's own text, and (2) UX-relevant PRD content that no decision — and no
spine direction — carries. This document does not argue that any decision should change; a
contradiction with the PRD is resolved by the maintainer editing one side or the other.

**Inputs.**
`_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md` (1809 lines,
status: final) and `addendum.md` beside it (188 lines), read directly. Decisions: this run's
`.memlog.md` (37 entries, lines 6–42 — canonical) and `.decisions.log` (rubric verdicts only,
no PRD claims). Repository code cited where a contradiction is between the PRD's *status
word* and the tree (AGENTS.md: the tree wins).

**Citation convention.** `FR-n`/`NFR-n`/`NG-n`/`PG-n`/`SG-n`/`DG-n`/`OR-n`/`R-n`/`SM-n`/
`CM-n`/`OQ-n`/`A-n`/`UJ-n` are the PRD's own identifiers (PRD §0, A-6); `M-Lnn` is the
memlog entry at line `nn`. PRD quotes are verbatim with line numbers.

**State of the spine.** `EXPERIENCE.md` and `DESIGN.md` in this run folder are front-matter-only
drafts (4 lines, `status: draft`, no body) at the time of this reconciliation; the memlog's
entry 42 says spine distillation is *next*. So "carried by spine direction" could only be
assessed against the memlog's decisions. Everything in Part 3 is therefore owned by nothing
until the spine is written — the list is what the spine must not omit, not an accusation.

**Headline.** No decision contradicts the PRD on scope, privacy, accessibility or claim
discipline — those citations check out one by one (Part 1). Four statements do contradict
PRD text: the FR-34 "(shipped)" classification (C-1), FR-48's premise (C-2, which the PRD
states in two places and which the tree refutes), the blanket "no performance claim at all"
(C-3, where the PRD contradicts itself), and shipping the four new gate surfaces' strings in
two of the three languages NFR-13 makes a `must` (C-4). Separately, **17 items of
UX-relevant PRD content have no carrier** (Part 3), of which the sharpest are the wallet-side
refusal/tier-label messaging (DR-1), the proximity refusal surface and OQ-11 (DR-5), the
activity-log surface (DR-4), FR-32's unbuilt string-parity check against the run's own new
strings (DR-14), and §4's own rule that v1 may discharge at most three unmeasured
consequences — the kit as designed carries more (DR-13).

---

## Part 1 — Decision-by-decision check

Every memlog entry that makes a PRD-content claim, against the PRD line that governs it.
Verdict vocabulary: **clean** (consistent, PRD-anchored), **clean\*** (consistent but the
decision's wording is looser than the PRD's — see Part 2/Part 4), **fills a hole** (the PRD
is silent and the decision states the rule), **contradicts** (see Part 2).

| # | Decision (memlog) | PRD anchor | Verdict |
|---|---|---|---|
| M-L10 | v1 surfaces: sandbox + wallet gates + conformance report + RP kit; scope items listed as FR-34, FR-6/FR-26, FR-30, FR-45…FR-48, FR-16, FR-21…FR-27, OR-6 | §6.1 :1130-1147; §4.5 :763-856; §4.7 :986-1044; §4.4 :675-761 | clean\* — see W-5 (FR-6 assigned to the sandbox) and C-1 (FR-34) |
| M-L11 | Out of scope, no screens: proximity Tier-2 ZK, EE-PRI-014 dashboard, status lists/revocation, real issuer onboarding, circuit-registry service, thresholds beyond 18, EU certification flow | NG-6 :1072-1074; PG-5 :1401-1403; NG-11 :1092-1097 + §6.2 :1169; NG-12 :1098-1099; §6.2 :1160-1161; NG-10 :1085-1091 + §6.2 :1162; NG-16 :1111-1113 | **clean** — `PG-5` names `EE-PRI-014` as "**out of scope for v1** (§6.2)"; all seven items match a non-goal or an §6.2 bullet verbatim |
| M-L12 | Journeys mirror §2.3 UJ-1…UJ-5 with recorded climax beats; venue/proximity is *not* a journey | §2.3 :152-248; addendum D1.4 :46-56; §2.2 :143-145; OQ-11 :1772-1775 | **clean** — the five personas and contexts are the PRD's; D1.4 is the source for the no-venue-journey decision (DR-5 covers what is left undesigned) |
| M-L13 | Copy constraints: no compliance claims; a number only from the run that just happened; unmeasured figures absent; "v1 makes no performance claim at all" | NFR-1 :1317-1319; NG-2 :1057-1059; FR-36 :868-879; FR-28 :774-783; §16 principle 1 :1666; NFR-2 :1320-1322; NFR-7 :1339-1341; §6.2 :1152 | clean on the claim ban and FR-28; **contradicts** on the performance line → **C-3**; wording of "absent" → **W-2** |
| M-L14 | Refusal distinguishable from cancellation/retryable error in UI, log, acceptance cases; every refusal names its cause; fail closed and say which lookup failed | FR-26 :743-750; NFR-14 :1363-1365; OR-6 :1615-1617; PG-6 :1404-1406; NFR-3 :1323-1326; FR-23 :707-718 | **clean** — NFR-14 is "Cross-cutting and binding on every feature" (:1315), so extending the typed distinction to the sandbox is a requirement, not an extension |
| M-L15 | Four gates: unverified requester never shown as verified; user-presence prompt bound to the key operation; lockout surviving restart; proximity share same consent as remote | FR-45 :999-1010; FR-46 :1012-1022; FR-47 :1024-1033; FR-48 :1035-1044; SG-2 :1429-1432; SG-3 :1433-1435; SM-9 :1245-1249; CM-7 :1271-1274 | **clean** on FR-45/46/47; FR-48's *premise* contradicts the tree → **C-2**; FR-48's *content* half unowned → DR-6 |
| M-L16 | Privacy wording PG-1…PG-7: one predicate, no false predicate, no central log with the EE-PRI-023 carve-out stated accurately, issuer-chain disclosure and Tier-1 limitability stated, never overstate | PG-1 :1384-1388; PG-2 :1389-1390; PG-3 :1391-1392; PG-5 :1396-1403; glossary *Issuer-chain disclosure* :294-296; glossary *Linkability* :297-300; NFR-15 :1366-1368 | **clean** — matches PG-2's and NFR-15's own wording. One wording risk → **W-3** (DG-5 says this fork introduces no telemetry) |
| M-L17 | Accessibility floor NFR-12: WCAG 2.1 AA stated, EN 301 549 v3.2.1 + Directive (EU) 2019/882 binding; keyboard-complete; visible focus; contrast by existing tokens; no colour-only signalling; announcements; reduced motion (proof mark static) | NFR-12 :1353-1358; FR-35 :846-855; §10 :1502-1505, :1529-1530 | **clean** — near-verbatim, including the static proof mark. The audit-evidence half is uncarried → DR-7 |
| M-L18 | Language per surface: sandbox Estonian-first + EN toggle; wallet's three locales unequal (values-ru a 7-line stub) so the spine states what the app offers, not what it should | NFR-13 :1359-1362 | **clean** — NFR-13 says the gap is "recorded rather than smoothed". New strings in two locales → **C-4** |
| M-L19 | Independence notice everywhere, no RIA logo/coat of arms; carrier naming per OQ-6: DC API mandated, `mso_mdoc_zk` over OpenID4VP profiled-but-unofficial | NG-13 :1103-1104; FR-33 :827-834; OQ-6 :1760-1766 | **clean** — reproduces OQ-6's own "Recommended" wording; note OQ-6 is listed as not silently resolvable (:1741-1742) and is *not* resolved here, only its recommended copy is applied. Carrier label needs a screen to live on → W-6 |
| M-L20 | Identifier discipline: PRD `FR-n`/`NFR-n`/… kept distinct from `epics.md` FR1–FR66/NFR1–NFR10 | §0 :29-32; A-6 :1806 | **clean** |
| M-L21 | DESIGN.md collision: root DESIGN.md imported as the canonical identity for the sandbox only; new DESIGN.md covers the wallet gates, report and kit; no token restated; root Do's/Don'ts hold | §16 :1660-1663 ("`DESIGN.md` is the design authority for the sandbox site … This PRD does not restate it and does not override it") | **clean**; the report/kit identity is a PRD silence the decision fills |
| M-L23/M-L30 | FR-34 has no state to style; form chosen = degraded boot (server binds and serves one honest full-page state instead of exiting), requiring a `server.py` behaviour change | FR-34 :836-844; §4 preamble :334-337; §6.1 :1140; A-3 :1803 | **contradicts the status label** → **C-1** (consistent with FR-34's *requirement text*) |
| M-L24/M-L41 | Sandbox tri-state (refusal/cancellation/retryable) prose-only today; placed in-place and step-anchored with typed icon/title/cause + `aria-live` | FR-26 :743-750; NFR-14 :1363-1365; FR-6 :468-469 | **clean** — required by NFR-14; structural note in W-5 about FR ownership |
| M-L25 | FR-45: no unverified state exists; `VerifiedParty.kt:18-31` renders unconditionally on five surfaces; proximity requester name never assigned (`ProximityViewModel.kt:474`) | FR-45 :1005-1010; SG-2 :1429-1431; SG-1 :1417-1424 | **clean** — agrees with the PRD's own SG-1 finding ("`readerAuth` chains never validated against a trust anchor (so an attacker-chosen CN reaches the consent screen)") |
| M-L26 | FR-47: the durable half exists (`PinVerifier` 5 attempts / 30-min lock persists; `UserSessionDataSource.verifyPin` writes before returning); the UI half is absent; the in-memory counter stops at 3 and resets per launch | FR-47 :1028-1033 | **clean** — FR-47's second consequence is exactly this removal. Status nuance: a "(new)" FR whose persistence half ships (contrast C-1) |
| M-L27 | FR-48's premise is stale: proximity is already PIN-gated (`ProximityScreen.kt:82-88, 97-99`); consent-card internals duplicated remote/proximity | FR-48 :1041-1042; SG-1 :1419 | **contradicts the tree** → **C-2** |
| M-L28 | No `epics.md` story exists for the sandbox site or the four wallet gates; the spine must not cite a story id for them | — (epics.md, not the PRD; A-6 :1806) | **clean** (out of PRD scope; recorded because it constrains spine wording) |
| M-L31 | Conformance report = generated self-contained HTML artefact, root tokens, independence notice, keyboard-complete, regenerated every run; published per FR-16, reproducible per SM-1/NFR-8 | FR-13 :576-588; FR-14 :590-602; FR-16 :615-622; SM-1 :1202-1205; NFR-8 :1342-1343; NG-13 :1103-1104 | clean; FR-13's "machine-readable" output and its two-implementation result are not named → **W-4**, DR-17 |
| M-L32 | Report clause ordering profile-ordered, in-scope and out-of-scope adjacent, front summary of counts and verdicts | FR-16 :619-621; §16 principle 4 :1670-1673 | **clean** — reproduces FR-16's "adjacent to the covered ones, not in a footnote" |
| M-L33 | Language split: report English-only; acceptance kit Estonian-first with an English toggle; stated per surface | NFR-13 :1359-1362 (names only the wallet and the sandbox) | **fills a hole** — the PRD does not name a language for the report or the kit; the decision supplies NFR-13's "stated per surface" for both |
| M-L34 | Wallet visual stance: platform-conventional Material 3, semantic status colours pinned to the root family, no token transplant | §16 :1663 ("The wallet follows the platform conventions of its own host.") | **clean** — the decision cites the correct line |
| M-L35 | Discrepancy to Tom: the PRD marks FR-34 "(shipped)" but no such state exists; §4.5's shipped-set overstates it; a PRD correction is suggested | §4 preamble :334-337; §4.5 :763-770; §6.1 :1140; A-3 :1803 | **contradicts** — this is C-1, raised by the run itself |
| M-L36 | Kit IA: one documented linear install path, then four self-contained re-entrant cases, each restating preconditions, expected verdict, refusal cause and log line | FR-21 :685-694; FR-25 :731-741; UJ-5 :232-247; OR-6 :1615-1617; CM-3 :1259-1261 | **clean** — FR-21's "one documented install path" and FR-25's four cases are met; the unmeasured-row duty is uncarried → DR-13 |
| M-L38 | New wallet gate strings ship in Estonian and English only; the Russian gap is recorded rather than filled | NFR-13 :1359-1362 | **contradicts** NFR-13's `must` for new wallet strings → **C-4** |
| M-L39 | FR-45 semantics: hard refusal with reason visible plus collapsible why (no trust anchor / unparseable chain / name mismatch); Refuse/Cancel only, no proceed path | FR-45 :1005-1010; FR-6 :468-469; CM-7 :1271-1274 | **clean** — "the default is refusal, with the reason visible (FR-6)" is FR-45's own consequence |
| M-L40 | FR-47 surface: visible remaining-attempts counter, lockout screen states duration *and escalation path*, wired to existing persistence | FR-47 :1028-1033; OR-1 :1599-1603; OR-6 :1615-1617 | clean on FR-47; "escalation path" needs precision → **W-1**; FR-47's "artefact that publishes it" is unnamed → **W-7** |
| M-L6/M-L7/M-L8/M-L9/M-L22/M-L29/M-L37/M-L42 | Working mode, stakes, timeout protocol, source list, extraction event, 11-resolution event, the two `[ASSUMPTION]` entries | §0 :14-21 (reader list) | **clean** — M-L7's four reader types are §0's own reader list; no requirement claims |

**Addendum.** No decision contradicts `addendum.md`; it holds no PRD requirement ("Nothing
here is a PRD requirement", :4-5). D1.4 (:46-56) is the source for M-L12's no-venue-journey
decision, D1.3 (:37-44) for §6.1's scope, D4/D5 (:126-158) are non-UX figures. Nothing in
the addendum's D2 transport detail is restated or contradicted by a decision.

---

## Part 2 — Contradictions

### C-1. FR-34 is required to be *new* work, but the PRD sells it as shipped and regression-protected

**PRD, §4 preamble (:334-337):**
> "FRs marked **(shipped)** describe behaviour the repository already has and that this PRD
> protects from regression"

**PRD, FR-34 (:836):** "### FR-34: A missing toolchain produces an honest failure, not a fake **(shipped)**"

**PRD, §6.1 (:1140):** "| The sandbox site | FR-28 … FR-35: preserved as the public face | existing |"
**PRD, A-3 (:1803):** "[the sandbox site and the presentation spine] are in v1 as shipped behaviour rather than as new work" — with the settling condition "An explicit forward-only decision".

**Decision (M-L30):** DEGRADED BOOT — "the server binds and serves one honest full-page state …
instead of exiting on stderr. **Requires a `server.py` behaviour change (exit -> serve)**".

**Tree:** `sandbox/server.py:444-446` — `sys.exit(f"{name} not found at {path}; run \`make deps\`…")`
before the server binds; `server.py:108-111` exits when `zkverify` fails to start;
`deploy.sh:26` runs `make deps` first. No page, string or probe exists (independently
confirmed by `.working/extract-sandbox.md` §"Gaps", finding 1).

**Verdict.** FR-34's *requirement text* (:842-844) is satisfied by the decision — it says
"the page says so and names the documented command (`make deps`)". The contradiction is
entirely with the **status word**: an FR whose delivery changes `exit` into `serve` is new
behaviour on §6.1's "preserved"/"existing" surface and cannot be "behaviour the repository
already has". The run already flagged this (M-L35). Resolution options: relabel FR-34
unshipped in a PRD update, or drop the "protects from regression" claim for it.

### C-2. FR-48's premise is stated twice in the PRD and is false in the tree

**PRD, FR-48 (:1041-1042):** "The proximity share passes the consent gate before a document is emitted; **today it emits with no gate at all**."
**PRD, SG-1 (:1419):** the live TIER 1 items include "**a proximity share with no PIN step**".

**Tree:** `app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/ProximityScreen.kt:82-88`
launches `PinActivityResultContract` before the share, with the in-code comment "D12 (mobile
F8): the share is PIN-gated — the launcher mirrors the remote path (`PresentationRequestScreen`)
verbatim"; refusal logic at `:97-99` + `ProximityViewModel.kt:340-365`.

**Decision (M-L27):** "FR-48's premise is STALE in this checkout… Re-read the FR-48 requirement
text against current code before specifying."

**Verdict.** Direct contradiction, and it propagates: SG-1 is a *published* statement about
this fork's own outstanding findings, so leaving it uncorrected contradicts FR-38/NFR-16's
discipline (status rows derived from tests/code) for the very release that closes the four
gates. FR-48's real remaining delta is the *content* half (DR-6), not the gate.

### C-3. "v1 makes no performance claim at all" — the PRD's own sentence is narrower than the decision's paraphrase, and the PRD contradicts itself

**Decision (M-L13):** "**v1 makes no performance claim at all** (PRD §6.2)".

**PRD, §6.2 (:1152):** "Device measurement programme (D2, hardware-gated). Consequence, stated
rather than implied: **v1 makes no performance claim**, and every device row stays
`PENDING-DEVICE`. No usability figure, no BLE transfer time, no StrongBox key-generation
time, no proving budget is asserted."

That sentence sits **inside the D2 bullet**, so its scope is device figures. Elsewhere the
PRD requires figure-bearing surfaces:

- **NFR-6 (:1333-1338):** "Verification is reported as verifications/second on a named
  instance type, with percentiles and the measurement method, or it is reported as unmeasured.
  The two published figures come from different host classes…: 2,414 ms per valid proof on a
  2 vCPU instance, and mean 4.02 s / p50 3.83 s / p95 4.91 s on an Intel i5-8365U."
- **OR-2 (:1604-1606):** "Capacity **is stated** as verifications per second per instance type, with percentiles and the host class."
- **FR-28 (:782-783):** "Any figure shown (**proof size, timings**, verdict) is rendered from the server's response for that run."
- **FR-27 (:752-761)** + SM-4 (:1216-1219): time-to-first-verification "measured and published with the baseline, method and confidence", or published as unmeasured.

**Verdict.** As a shorthand the decision is right; as the copy rule a spine will apply, it
over-broadens and would suppress NFR-6/OR-2 host-class capacity and the sandbox's
run-sourced proof size and timings. The fix is a qualifier — "no *device* performance claim;
host-class capacity per NFR-6/OR-2; run-sourced figures per FR-28" — and the PRD itself
should scope its §6.2 sentence, because §6.2 (:1152, "no performance claim") and NFR-6
(:1333, "is reported as verifications/second") cannot both be read as written.

### C-4. NFR-13 makes tri-lingual a `must` for the wallet; the run ships the four new gate surfaces in two locales

**PRD, NFR-13 (:1359-1360):** "The wallet **must** offer Estonian, Russian and English
(`EE-ACC-004`)… The gap between the two is recorded rather than smoothed: no string exists
in one language only *on the surface it belongs to*."

**Decision (M-L18, M-L38):** "the wallet's three locales are NOT equal — … `values-ru/strings.xml`
is a 7-line stub, so the tri-lingual duty … is unmet and the spine states what the app
actually offers"; "**New wallet gate strings ship in Estonian and English** (the app's two
complete locales)".

**Verdict.** Recording the *existing* stub is what NFR-13 sanctions ("recorded rather than
smoothed"). What has no PRD cover is the second sentence: FR-45…FR-48 are new wallet UI, and
NFR-13's rule is that a surface offers its languages, not that new strings may be added to
two of three. Two honest resolutions: (a) declare it as a shortfall in the PRD's own FR-8
style ("declares rather than satisfies", :485-496), with the reason and the evidence that
would close it (§4 rule, :339-343); or (b) translate the new strings. Note the duty's own
strength is arguable — the extract records `EE-ACC-004` as a spec `SHOULD`
(`.working/extract-epics-research-product.md` §3.5, quoting `spec/EE-EUDIW-TS-1.0.md:1170`)
while NFR-13 says `must`, so a PRD wording fix is a legitimate second exit.

---

## Part 3 — UX-relevant PRD content no decision or spine direction carries

Each item names the PRD text, the UI consequence, and why it is unowned (the memlog entry
that comes nearest is given). "Shipped-and-untouched" items are flagged, since §6.1
(:1139-1140) keeps the spine and the sandbox in v1 as preserved behaviour — but no decision
protects them, and the run is *adding* states to those surfaces.

### Load-bearing, no carrier at all

**DR-1. The wallet-side refusal, no-match and tier-label messaging.** FR-4 (:418-420)
refuses multi-predicate requests "on the OpenID4VP, DC API and proximity paths"; FR-5
(:435-436) "**The user is told**, and the relying party receives the no-match answer"; FR-6
(:466-469) "Where a presentation is a documented fallback rather than a proof, the wallet
**labels it as such to the user and in its log**" and "The user sees a dedicated message for
a refusal, distinguishable from a retryable network error and from the user having
cancelled"; PG-4 (:1393-1395) "No silent downgrade"; §13 R7 (:1586) "the fallback stays
visible in logs and **UI**". Nearest decisions: M-L39 (unverified-requester refusal only) and
M-L41 (sandbox tri-state). The wallet's other refusal surfaces — second-predicate refusal,
held-false no-match, tier/fallback label — have no screen, copy or placement anywhere in the
run. C-3's rule interacts here: a tier label is not a performance claim, so it survives.

**DR-2. Consent-screen content beyond the requester name, and the two-credential session.**
FR-10 (:528-529) requires the consent screen to show "the requester, **the requested
predicate and the channel** before the proof is produced" — only the requester half is
designed (M-L39). UJ-4 (:226-228) requires "the service holds what it asked for and nothing
more; **the wallet shows what was disclosed and to whom**", with FR-7 (:476-480) naming both
credential identifiers in the session log and a missing credential failing by name (:229-230).
No decision designs a disclosure summary, a two-credential consent, or the missing-credential
failure message. M-L16 constrains the wording; it does not place the surface.

**DR-3. Attestation renewal.** FR-8 (:502-503): "Renewal is offered from the day the earliest
held `validUntil` falls inside the re-issuance window (`AgeAttestationRenewal`); accepting
replaces the batch"; §6.2 (:1168) makes re-issuance user-started. An offer, an accept action
and a replaced-batch confirmation are all user-facing and none is carried.

**DR-4. The wallet's own activity log.** PG-5 (:1399-1400) "The wallet's own activity log is
local, **distinguishes linkable from new-visit presentations**, and is the user's"; OR-5
(:1612-1614) repeats it; FR-6 (:464-465) requires a refusal written "to the activity log as
linkable"; FR-42 (:950-951) requires a per-path unlinkability statement backed by a test;
DG-7 (:1642-1644) says where it cannot be claimed it is recorded as not claimed. M-L11
correctly defers the Art. 5a(4)(d) `EE-PRI-014` dashboard (PG-5 :1401-1403), but the log
itself is not the dashboard and nothing in the run designs it, its linkable/new-visit
marking, or its empty state. Nearest decision: M-L14 mentions "the log" only as an artefact
of the refusal distinction.

**DR-5. The proximity refusal surface, and OQ-11.** §2.2 (:143-145): "on a ZK-capable device
an age request over the proximity or redirect path is **refused outright today** rather than
answered plainly"; FR-6 (:460-462) makes that refusal a consequence; NG-6 (:1072-1074) puts
proximity Tier-2 out of scope; OQ-11 (:1772-1775) asks whether the proximity age path should
keep advertising itself, "Undecided". The run rules out a venue journey (M-L12) and all
proximity Tier-2 screens (M-L11) — but the refusal *is* v1 behaviour, on a shipped screen,
and no decision designs it, states it to the venue, or answers OQ-11's keep/remove choice.

**DR-6. FR-48's content half.** FR-48 (:1043): "The screen states **what is shared and with
whom on the same terms as the remote path**." M-L15 states the principle; M-L27 records that
the gate already exists and that the card internals are duplicated, so "the same terms"
cannot be achieved by editing one component; M-L27 ends asking for the FR text to be re-read
before specifying. The divergence list itself is in `.working/extract-wallet.md` §(g) FR-48:
proximity lacks a real requester identity, an authenticated-reader state, the channel notice
and the ZK-tier notice. No decision adopts it.

**DR-7. Accessibility audit evidence (R13).** NFR-12 (:1358): "**No EN 301 549 audit evidence
exists yet (R13)**"; §10 (:1529-1530) repeats it; §13 R13 (:1592) mitigation: "**run an
accessibility audit as a milestone exit criterion**". M-L2 puts an accessibility *lens* at
the Reviewer Gate and M-L17 states the standard — neither is audit evidence, and no decision
plans the audit, states the gap on the new surfaces (report, kit, four gates), or carries
R13's exit criterion.

**DR-8. Sandbox hosting (OQ-12) against UJ-1's entry state.** UJ-1 (:168) has Rasmus
"arrive cold at the sandbox site"; OQ-12 (:1776-1778) records that the site "today runs on a
host the visitor must build" and that hosting "needs a decision". M-L30's degraded boot
softens the build problem but is not a hosting decision; no decision addresses OQ-12, and it
is not in §6.2's out-of-scope list (:1149-1171), so it reads as in-scope-and-open.

**DR-9. The register lookup's data-flow disclosure (DG-6/OQ-3).** DG-6 (:1638-1641): "Who
sees that query, what it reveals about a presentation, and how long it is retained are
undecided and are an open question (§18, OQ-3)"; OQ-3 (:1751-1753) adds "what does a register
lookup expose about a presentation". The kit's reader is exactly the operator who must
answer that (§11 :1546; FR-23 :713-718 requires the "no entry ⇒ refuse" default and a logged
stale decision). M-L36 designs install + four cases; no decision covers what the operator is
told about the new data flow, or the refusal wording for a stale/ambiguous register answer
beyond M-L14's general rule.

**DR-10. FR-8's declared batch shortfall as claim discipline.** FR-8 (:491-496): "**The batch
of three is a shortfall, not a satisfied rule.** … No artefact may describe this product as
meeting `EE-POA-011`". M-L13's copy constraints name the compliance vocabulary and the
performance claim but not this specific declared shortfall; any wallet surface that shows
attestation counts, validity, or "one-time" posture needs the wording. (Medium confidence —
it only bites if such a surface exists; FR-8's once-only posture is user-visible in the
wallet's credential list.)

**DR-11. The `EE-PRI-023` telemetry carve-out versus DG-5 — a wording rule is needed, not
just accuracy.** M-L16: "no central age log (EE-PRI-023 telemetry carve-out stated accurately
if telemetry is mentioned at all)". PG-5 (:1396-1399) carries the carve-out for an *operator's*
telemetry; DG-5 (:1636-1637) states "**No analytics, no telemetry.** … no product telemetry is
introduced by this release." No decision states the scope rule (the carve-out describes a
deployment's operator, never this fork), so a surface that quotes the carve-out can imply
telemetry exists here, contradicting DG-5. Cheap to fix; currently unowned.

**DR-12. The kit's unmeasured rows and §4's own budget rule.** §4 open rule (:339-343): "A
consequence that permits 'or record it as unmeasured / not obtained / out of scope' is
satisfied only by **a record that names the reason, the date, and the evidence that would
close it**. v1 may discharge at most three of them (FR-15, FR-22, FR-24, FR-27 and FR-42 each
contain one, so at least two must be closed); **a fourth means the release is being defined
by what it did not do**." At v1, SM-3 and SM-4 are already unachieved (:1213-1219, §18 OQ-10
:1732-1739) and FR-22's TARA test, FR-24's owner review and FR-42's LoA test are conditional
(:703-704, :728-729, :955-956). M-L13's rule ("unmeasured figures absent") and M-L36's kit IA
neither state the row, nor the reason/date/evidence triple, nor the count. This is the
acceptance kit's most likely reviewer-facing failure and it is unowned. (See also W-2.)

**DR-13. FR-32's string-parity check, and FR-31's shortcut naming, against the run's own new
strings.** FR-32 (:825): "A string added in one language without the other **fails the site's
own check**". `.working/extract-sandbox.md` §"Gaps" records that no such check exists
("`sandbox/check.py` asserts the story over HTTP and never touches the dictionaries") and
that several visible strings are not keyed at all. FR-31 (:810-815) requires the synthetic
register's client-side age computation and the synthetic identities to be labelled. The run
adds new sandbox strings (tri-state copy, degraded-boot page) with no decision to add the
parity check or to name the FR-31 shortcuts — even though M-L33 fixes the *sandbox's*
language model to Estonian-first + EN toggle. **Shipped-and-untouched, but the run changes
the surface, so the protection belongs to this run.**

**DR-14. The sandbox's other shipped surfaces the run's delta can break.** FR-29 (:785-794,
the two-column ledger), FR-30 (:796-804, carried by M-L10), FR-33 (:827-834, carried by
M-L19), FR-35 (:846-855, carried by M-L17). The extract's own warning stands unowned:
DESIGN.md's Do-rules most at risk from new states are "Every number shown comes from the run
that just happened" and "Status never colours a whole region"
(`.working/extract-sandbox.md` finding 6, `DESIGN.md:290-303`). No decision states how the new
tri-state/refusal blocks avoid colouring a whole region or adding a figure.

### Unowned, lower confidence (flagged as such)

**DR-15. FR-13's two-implementation reporting.** FR-13 (:585-587): "The kit runs against at
least two independent implementations and reports a clause-by-clause result **for each**";
UJ-3's climax (:209-211) is that the run "reports the same result for this fork's verifier and
for an external implementation". M-L31/M-L32 design one artefact with a front summary; no
decision says how two implementation columns are presented (or how an external run's absence
at v1 is shown — SM-2 is unachieved, :1206-1209).

**DR-16. OQ-15's conditionality in the kit's copy.** OQ-15 (:1786-1788): "Will RIA adopt the
AV Profile's attestation, and is a foreign AV attestation admissible to an Estonian relying
party? … **the relying-party kit's four acceptance cases assume admissibility**." M-L36
reproduces the four cases without the assumption note; if the answer is "not admissible", a
fifth refusal case appears and CM-3 (:1259-1261) binds.

**DR-17. NFR-16/SG-4's publishing duty on the gates' own findings.** SG-4 (:1436): "No artefact
implies a finding is closed without a test or a tool run"; NFR-16 (:1369-1373) requires the
audit referenced by severity count with "the state of each high finding … an explicit row";
SG-1 (:1415-1428) requires any artefact quoting the audit to state its commit (`732d57e` vs
the PRD's `2d38feb`). M-L15/M-L39/M-L40 close the gates; no decision carries the duty that the
new surfaces (wallet settings/about, kit README, report) state the findings' state rather
than implying closure. PRD-side owner may be FR-38's artefact family rather than a UX surface;
listing it so the gate is not silently assumed published.

**DR-18. The biometric/user-presence prompt's own copy.** FR-46 (:1012-1022) requires the
gate; `.working/extract-wallet.md` §(g) FR-46 records that the prompt's reason copy is
hardcoded English (`PromptDialogHost.kt:156-159`), untranslated, and that a cancelled
biometric aborts the flow with no message. NFR-13 (:1362) forbids a string existing in one
language only *on the surface it belongs to*. M-L15 states the gate; no decision owns this
copy or the two-gates-back-to-back explanation.

---

## Part 4 — Wording-level tensions (no PRD text is contradicted; the copy rule is slacker than the PRD's)

- **W-1. "Escalation path" (M-L40) vs "no support channel" (OR-1 :1599-1603, OR-6 :1615-1617).**
  FR-47 (:1030) requires "a lockout threshold and an escalation path stated in the artefact
  that publishes it"; OR-1 claims no support window and OR-6 titles itself "Failure is
  diagnosable without a support channel". The lockout screen must therefore state a recovery
  the product actually has (wait N minutes, retry) and not imply a helpdesk.
- **W-2. "Unmeasured figures are absent" (M-L13) vs NFR-2 (:1321-1322), FR-37 (:888), §6.2
  (:1152).** All three require the row to **exist** and say `PENDING-DEVICE` "rather than
  being estimated silently". Absence and labelling are different instructions; the spine
  should say "labelled, never estimated".
- **W-3. The `EE-PRI-023` carve-out (M-L16) vs DG-5 (:1636-1637).** See DR-11.
- **W-4. "Self-contained HTML artefact" (M-L31) vs FR-13 (:582).** "The runner's output is
  **machine-readable**, names each clause and each verdict, and exits non-zero when any
  in-scope clause fails." An HTML report can carry embedded machine-readable data, or the
  runner can emit a sibling JSON; the decision as written names only the HTML artefact.
- **W-5. FR-6's ownership.** §6.1 (:1139-1140) maps FR-1…FR-10 to "the presentation spine"
  and FR-28…FR-35 to the sandbox. M-L10 assigns "FR-6/FR-26 refusal reason surface" to the
  sandbox surface. FR-6's subject is "A holder on a ZK-capable device" (:455) — the wallet.
  The sandbox mapping is defensible under NFR-14's cross-cutting rule, but as written it
  leaves the wallet half of FR-6 (tier label, dedicated refusal message) unowned: DR-1.
- **W-6. OQ-6's mandated-vs-unofficial carrier label (M-L19) needs a screen.** UJ-2's path
  runs over OpenID4VP `mso_mdoc_zk` (:188) — the profiled-but-unofficial carrier — and FR-10
  (:529) already puts "the channel" on the consent screen. Nothing in the run places the
  OQ-6 label there, so the copy rule has no carrier at v1.
- **W-7. FR-47's "artefact that publishes it" (:1030) is unnamed.** M-L40 puts the threshold
  and escalation on the lockout screen; the PRD's phrase implies a published artefact
  (README/kit doc/PRD row). Either the screen is that artefact or a document is missing.

---

## Coverage note

Checked and found consistent with the PRD's text, with no gap: FR-1…FR-5 handling in M-L16
(one predicate, no false predicate), FR-9 (hardware keys) via M-L15's FR-46, NG-1/NG-2/NG-5
and FR-36 via M-L13, NG-7/NG-8/NG-9/NG-10 via M-L11's out-of-scope list, NG-13/FR-33 via
M-L19, PG-1/PG-2/PG-3/PG-6/PG-7 via M-L14/M-L16, SG-2/SG-3/SM-9/CM-7 via M-L15/M-L39/M-L40,
NFR-3 via M-L14, NFR-15 via M-L16, OR-6 and FR-21/FR-25/FR-26/FR-27 via M-L14/M-L36, §16's
four principles via M-L19/M-L21/M-L32 and M-L34, A-6 via M-L20. §13's rows other than R7 and
R13 are non-UX or already owned (R12/FR-38 is the run's own FR-34 finding; R16 is closed by
OQ-8, :1722-1731). §15's DG-1…DG-4, DG-7 are either shipped-and-untouched sandbox behaviour
(DG-1 synthetic labelling, DG-4 run-scoped state) or carried by M-L16 (DG-7's "recorded as not
claimed"); DG-6 is DR-9.
