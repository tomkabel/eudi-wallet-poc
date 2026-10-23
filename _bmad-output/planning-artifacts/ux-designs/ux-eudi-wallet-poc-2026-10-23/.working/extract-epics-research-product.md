# UX extract — eudi-wallet-poc v1 surfaces

Working extract for the UX run (DESIGN.md + EXPERIENCE.md spines). Sources read in full:
`_bmad-output/planning-artifacts/epics.md` (1792 lines), `PRODUCT.md` (65 lines),
`docs/research/2026-10-23-innovation-assessment.md` (612), `docs/research/2026-10-23-estonia-alignment-assessment.md` (490),
`docs/research/2026-10-23-shared-appendices.md` (324). PRD and spec were consulted **only to resolve identifiers**
(§E) because two of the four v1 surfaces are named in PRD numbering. Nothing here is invented; every claim carries a
`path:line`.

## 0. Numbering warning (read before citing anything)

- `epics.md` carries **its own** FR1–FR66 and NFR1–NFR10 (`epics.md:87-158`, `epics.md:162-171`). It is a
  different scheme from the PRD's FR-n. Cite as `epics FR66` vs `PRD FR-45`.
- The two colliding numbers that matter here: **`epics FR14`** = "Refusal that is distinguishable from user
  cancellation in the UI contract" (`epics.md:100`), which is the implementation of **`PRD FR-26`**
  (`prds/prd-eudi-wallet-poc-2026-10-23/prd.md:743`). Do not read `epics FR45` (`epics.md:131`, ZKP_03/04/ACP_03
  design notes) as PRD FR-45.
- `epics.md` was written before the PRD arrived: "**There is no `prd.md` and no `architecture.md` under
  `planning_artifacts`** … A `prds/` extract set appeared under `planning_artifacts` while this document was being
  reviewed; it is another session's work and **these stories were not derived from it**" (`epics.md:52-56`).
  This is the mechanical cause of the gaps in §1.2 and §1.3.

## 0.1 Coverage verdict for the four v1 surfaces

| v1 surface | epics.md coverage | Verdict |
|---|---|---|
| Sandbox site behavioural spine + PRD FR-34 / FR-6 / FR-26 / FR-30 states | 0 stories; the word "sandbox" occurs **once** in the file, in NFR10 | **NO STORY** (one indirect exception: PRD FR-26 is implemented by `epics 3.6`, but for the RP-kit path, not the site) |
| Android wallet consent gates PRD FR-45…FR-48 | 0 stories; the word "consent" occurs **0** times in the file | **NO STORY for all four gates** |
| AV-Profile conformance report | Epic 1, Stories 1.1–1.6 | covered |
| Relying-party acceptance kit integrator experience | Epic 3, Stories 3.1–3.8 (+ Epic 11 Stories 11.1/11.2/11.3/11.5) | covered |

The backlog states this itself: "**No `bmad-ux` spine exists for this repository, so the UX section is empty.** The
two user-facing requirements the source documents do raise are carried as implementation stories rather than UX
specification work: FR19 (Topic 19 / DASH_03 presentation-log fields, Epic 4) and FR62 (accessibility conformance,
Epic 11)." (`epics.md:185-190`)

---

## 1. epics.md — what each story implies for a screen or a behaviour

### 1.1 The only UI-binding requirements in epics.md

- **NFR10** (verbatim): "The sandbox and wallet UI hold WCAG 2.1 AA as the accessibility baseline, with EN 301 549
  as the presumption." (`epics.md:171`) Bound to stories 11.3 and 11.7 only, "How it is checked: Story 11.3's audit
  report" (`epics.md:281`). The wallet's own surfaces are therefore bound to 11.7, which is a *data-minimisation*
  story (§1.2 surface 2).
- **epics FR53**: "Refusal with a user-visible reason for an unmapped threshold, and an assertion that no `false`
  predicate is ever transmitted." (`epics.md:139`)
- **epics FR66** (verbatim, the tier-label duty): "The plain fallback stays visible and refusable under
  `EE-ZKP-055a`'s own condition: where a request for the EE-PoA or `eu.europa.ec.av.1` is *not* authenticated by a
  registration certificate — an unsigned Digital Credentials API request (`EE-PRO-004`), an `org-iso-mdoc` request
  without `readerAuth` chaining to a trusted access CA (`EE-PRO-025`), or a proximity request without `readerAuth` —
  and the device is ZKP-capable, the wallet presents over Tier 2 only and refuses a request that admits only a plain
  mdoc." (`epics.md:152-158`)

### 1.2 Sandbox site spine + PRD FR-34 / FR-6 / FR-26 / FR-30

**No epic and no story in `epics.md` addresses the sandbox site as a user surface.** There is no sandbox epic, no
flow story, no bilingual story, no independence-notice story, no ledger story, no cheat-and-fail story. The single
mention is NFR10 (`epics.md:171`).

Story-by-story mapping of the four PRD-numbered states:

| PRD state | epics.md analogue | Story name (verbatim) | Screen / behaviour implied |
|---|---|---|---|
| **PRD FR-34** — "A missing toolchain produces an honest failure, not a fake (shipped)"; "the page says so and names the documented command (`make deps`) instead of rendering a simulated result" (`prd.md:836-844`) | **none** | — | No story. The same honesty principle is carried onto *report* surfaces, not the site: "a clause that errors, and a candidate that will not start, each produce their own result state (`ERROR`/`NOT_EXECUTED`) and **never `PASS`**" (`epics.md:345-346`, Story 1.3); "the published result includes failures rather than only passes" (`epics.md:399`, Story 1.6); the blocked-state rule ("records the state as blocked with the reason and date — never as passed, and never with a fabricated result", `epics.md:352-354`) |
| **PRD FR-6** — "Weaker requests are refused, with the reason shown and the tier labelled (shipped)"; "The user sees a dedicated message for a refusal, distinguishable from a retryable network error and from the user having cancelled" (`prd.md:453-469`) | **epics 8.11** (epics FR66) | `Plain-fallback visibility guard` (`epics.md:1345`) | AC: "the fallback remains visible in logs **and the interface** for each of those three shapes"; "the record states which tier was used and why"; the given-state names the UI: "**Tier 2 is labelled in the UI**" (`epics.md:1353-1359`). Related: proximity "no silent downgrade to a plain mdoc" (`epics.md:920-921`, Story 6.3) |
| **PRD FR-26** — "A refusal is distinguishable from a cancellation (new)"; "The UI contract distinguishes the two states, and an end-to-end case exercises each" (`prd.md:743-750`) | **epics 3.6** (epics FR14) | `Refusal distinguishable from cancellation` (`epics.md:582`) | "As a holder whose request was refused, I want the refusal to be distinguishable from my own cancellation, So that I can tell a policy decision apart from my own action." AC: "the two outcomes are distinguishable in the interface contract" + a test on the fixed dispatch path (`epics.md:584-593`). **Scoped to the RP-kit / verifier path, not to the sandbox page** |
| **PRD FR-30** — "The visitor can try to cheat and watch it fail (shipped)"; "The site offers at least one deliberate attack … and shows the verifier's named refusal" (`prd.md:796-804`) | **none** | — | No story. The nearest artefacts are the negative-control suites aimed at integrators, not visitors: Story 2.1's "a `vp_token` entry matching neither carrier, and bytes matching no family at all, are reported as their own outcomes" (`epics.md:427-429`) and Story 2.2's named-property failures (below) |

**Nearest-negative-control surface, worth borrowing for the site's refuse-and-label behaviour (these *do* have
stories):** Story 2.2 "Binding-property failure naming" (`epics.md:436`) — "a failing case names the binding property
that failed, So that I can fix the right thing instead of guessing at a generic 'invalid'" (`epics.md:438-439`);
"no failure path emits a generic 'invalid' in place of the property name" (`epics.md:447`). Story 1.4
`av-vci:// invocation` (`epics.md:357`) — "an unrecognised invocation is refused loudly rather than silently
ignored" (`epics.md:368`).

### 1.3 Android wallet consent gates (PRD FR-45 … FR-48) — NO STORY, gate by gate

`grep -c consent` over `epics.md` = 0. Gate-by-gate, with the closest existing story:

| PRD gate | Required screen/behaviour (PRD) | epics.md | Nearest existing story |
|---|---|---|---|
| **FR-45** — "A requester name shown at consent is authenticated (new)" | "the screen says the requester is unverified and the default is refusal, with the reason visible (FR-6)"; "no unvalidated CN reaches the screen as a verified name" (`prd.md:999-1010`) | **no story** | Epic 4 Story 4.2 `Registration-certificate parsing` (`epics.md:658`) produces the typed `RegisteredIntendedUse` carrying "trade name, unique identifier, service identifier, Member State, intended use and privacy-policy URL" (`epics.md:668`) — data for the screen, no rendering duty; Story 4.4 `Topic 19 log fields from the certificate` (`epics.md:690`) is the log side; Story 4.3 `Certificate-driven two-predicate decision` (`epics.md:674`) adds one user-facing sentence only: "**it is allowed and the user sees an explanation of the revealed range**" (`epics.md:684`) |
| **FR-46** — "Credential keys require user presence (new)" | "Key use is gated by a platform user-authentication prompt whose result the key operation depends on, not by an application-level flag alone" (`prd.md:1012-1022`) | **no story** | Story 10.5 `Mobile-device security test matrix` (`epics.md:1565`) — given-state only: "hardware-backed keys, PIN verification and key cleanup on failure are implemented" (`epics.md:1573`); no AC requires a user-presence gate. Story 7.5 (`epics.md:1050`) names "per-presentation unlock" as existing HAIP behaviour (`epics.md:1058`) |
| **FR-47** — "The consent gate resists guessing (new)" | "Failed attempts are counted in persistent state that survives process restart, with a lockout threshold and an escalation path stated in the artefact that publishes it"; relaunch must not reset the count (`prd.md:1024-1033`) | **no story** | none. Lockout wording appears nowhere in `epics.md` |
| **FR-48** — "A proximity share carries the same consent as a remote one (new)" | "The proximity share passes the consent gate before a document is emitted; today it emits with no gate at all"; "The screen states what is shared and with whom on the same terms as the remote path" (`prd.md:1035-1044`) | **no story** | Story 6.3 `Proximity BLE transfer measurement` (`epics.md:907`) covers the proximity *path's timing and refusal*, never a consent step: "a refusal is raised when a ZK response is impossible, with no silent downgrade to a plain mdoc" (`epics.md:920-921`) |

Why they are missing, in the backlog's own words: the stories were not derived from the PRD (`epics.md:52-56`), and
the PRD records the four gates as decided *into* v1 on 2026-10-23 (`prd.md:1722-1731`; guardrails `prd.md:1429-1435`).

### 1.4 AV-Profile conformance report (Epic 1 + claim discipline) — covered

Epic `EU AV-Profile conformance kit` (`epics.md:288`); reader-facing scope in its own overview: "Convert the
AV-Profile work already in the tree into an artefact **an EU programme can test against**" (`epics.md:290-291`).

| Story (verbatim) | line | Screen / behaviour implied |
|---|---|---|
| `AV Profile clause registry` | `epics.md:297` | Reader taste of a report: "I want a machine-readable statement of each AV-Profile normative clause **with the test id that covers it**, So that I can see which clauses are evidenced and which are explicitly out of scope for v1" (`epics.md:300-301`); every clause resolves to a test id or "an explicit 'out of scope for v1' note" (`epics.md:307`) |
| `AV-profile fixtures` | `epics.md:312` | No UI. Version-labelled fixtures (`epics.md:322-327`) |
| `AV conformance runner` | `epics.md:331` | The per-clause report surface: "prints a per-clause pass/fail report" (`epics.md:334`); "each result names its clause and test id" (`epics.md:342`); three distinct result states, incl. `NOT_EXECUTED` (`epics.md:345-346`) |
| `av-vci:// invocation` | `epics.md:357` | A loud, non-silent refusal for an unrecognised invocation (`epics.md:368`) |
| `AVSUA-equivalent issuance path` | `epics.md:373` | No UI |
| `Published AV report and external reproduction` | `epics.md:388` | The report as a *reproducible* artefact: "I want the AV-Profile conformance report published together with the commands that regenerate it, So that I can reproduce it from a clean checkout in one sitting, **including the failures**" (`epics.md:390-392`); "the report version is tied to the pinned AV Profile document version" (`epics.md:400`) |

Copy constraints on that report (binding, from Epic 7 `Documentation honesty`, `epics.md:979`): Story 7.4
`Evidence-scoped compliance and estimate vocabulary` (`epics.md:1034`) — "no document claims conformance or
certification" and the permitted wording is "these requirements are supported by these tests, in this environment"
(`epics.md:1038-1045`); story 7.5 requires the HAIP statement to "not claim profile conformance"
(`epics.md:1061`); Story 7.11 `Estimate method discipline` (`epics.md:1145`); `epics FR33` (`epics.md:119`) and
`NFR1` (`epics.md:162`).

### 1.5 Relying-party acceptance kit — integrator experience (Epic 3) — covered

Epic `Estonian relying-party acceptance kit` (`epics.md:489`); its pitch is the buyer's window: "**It is the only
recommendation a buyer can act on before 2028.**" (`epics.md:495`)

| Story (verbatim) | line | Integrator-facing behaviour implied |
|---|---|---|
| `Deployable RP service profile` | `epics.md:498` | "I have something to run acceptance tests against **instead of a library to assemble**" (`epics.md:501`); "it starts from the documented commands **with no undocumented step**" (`epics.md:508`) |
| `TARA-facing adapter contract` | `epics.md:514` | Contract records where the RIA agreement is a prerequisite "rather than hiding it" (`epics.md:525`); wallet path documented as a parallel path (`epics.md:526`) |
| `Fail-closed RP-register interface and mock` | `epics.md:530` | "the request is refused **with the missing prerequisite named in the log**" (`epics.md:540`); "never a silent allow" (`epics.md:542`) |
| `RP-register interface proposal` | `epics.md:546` | Addressed to the competent authority; must state "the refusal semantics and a fail-closed default" (`epics.md:556`) |
| `Four Estonian acceptance cases` | `epics.md:562` | "each produces a documented, **machine-readable** result"; the missing-registration and unattested cases refuse (`epics.md:572-574`); a recorded fifth deadlock case (`epics.md:574-578`) |
| `Refusal distinguishable from cancellation` | `epics.md:582` | Two distinguishable outcome states in the interface contract (§1.2) |
| `Integrator README and timed onboarding` | `epics.md:597` | "a README that takes me to four reproduced results **in under a day**" (`epics.md:600`); "time-to-first-successful-verification is measured with a stopwatch and reported as a distribution over at least three integrators" (`epics.md:608`); externally blocked on three human integrators (`epics.md:611-613`) |
| `Parallel-path integration note` | `epics.md:616` | A written note for architects: X-Road has "no documented relationship to the EUDI Wallet, OpenID4VP or OpenID4VCI" (`epics.md:626-627`) |

Adjacent operator surfaces: Story 11.1 `Operations runbook` (`epics.md:1590`), 11.2 `Monitoring availability and
incident process` (`epics.md:1606`), 11.3 `Accessibility conformance audit` (`epics.md:1622`), 11.5
`Runbook game day` (`epics.md:1660`), Story 6.5 `Register latency and availability profile` (`epics.md:948`).
Story 11.3's scoping sentence is the only place a UI boundary is asked for: "the report states which parts of the
interface were and were not in scope" (`epics.md:1634`).

---

## 2. PRODUCT.md — users, positioning, principles, accessibility (verbatim where short)

- **Platform**: "web (the sandbox site); the wallet itself is the Android app in `app/`." (`PRODUCT.md:7`)
- **Users** (verbatim): "Reviewers, relying parties, and curious citizens who want to see a zero-knowledge proof of
  age work end to end, without an Android phone and without reading the spec. **They arrive cold and leave
  understanding what the shop learned and what it did not.**" (`PRODUCT.md:17-19`)
- **Product Purpose**: fork of the EE EUDI Wallet PoC adding a ZK proof of age (`age_over_18`, Longfellow over
  ISO 18013-5 mdoc, OpenID4VP); "lets a visitor register a test identity, receive a proof-of-age credential, and
  prove they are over 18 to a test shop, **with every step executed for real**" (`PRODUCT.md:23-26`).
- **Positioning** (verbatim): "The proof is real: a 360 KB Longfellow proof generated on the server in about six
  seconds and verified by the Go verifier in about two and a half seconds. **The shop learns one boolean and nothing
  else: no birth date, no name.**" (`PRODUCT.md:30-32`)
- **Constraints carrying UX weight**: the sandbox "Population Register" computes age predicates from a typed birth
  date — "That is the trust-the-client enrolment weakness EE-POA-006/007 warns about, and **the site says so**"
  (`PRODUCT.md:38-40`); "Bilingual: Estonian and English, with a toggle" (`PRODUCT.md:42`).
- **Brand commitments**: "Visual world pinned by the user: copy or infer from ria.ee, the TEDI design system
  (TEHIK). Roboto, TEDI colour tokens, Material Symbols icons." (`PRODUCT.md:46-47`); "**Not RIA's product**: no
  state coat of arms and no RIA logo. Every page carries the independence notice (see README)." (`PRODUCT.md:48-49`)
- **The three product principles** (verbatim, `PRODUCT.md:58-60`):
  1. "Show, don't claim: every number on the page comes from the run that just happened."
  2. "Say what the shop did not learn, as plainly as what it did."
  3. "Name the sandbox's shortcuts where they happen, not in a footnote."
- **Accessibility commitment** (verbatim, `PRODUCT.md:64-65`): "Estonian public-sector baseline: WCAG 2.1 AA.
  Keyboard-complete, screen-reader announcements for long-running steps, and reduced-motion support."
- **Evidence on hand**: "Measured figures in `README.md` and `verifier/README.md` (prove 6.1–6.5 s, verify ~2.5 s,
  proof ~360 KB). Test personas are synthetic and labelled as such." (`PRODUCT.md:53-54`)

---

## 3. Research corpus — UX-bearing items only

### 3.1 What the corpus says about users, surfaces and honest labelling

- **Who the users are** (competitive matrix row 1): "full ZK spine … users: researcher, Estonian programme, RP
  integrator"; maturity: "self-labelled PoC with a 211-requirement specification and a published measurement
  programme; **not a product**" (`docs/research/2026-10-23-shared-appendices.md:33`). Documented limitation in the
  same row: "no certification path; no real issuer; batch issuance missing; proximity Tier-2 unmeasured; **one stale
  conformity row contradicts the code**" (`shared-appendices.md:33`).
- **The verifying side plus its refusals is the product's distinctive artefact**: "The distinctive artefact here is
  the **verifying** side plus its refusals." (`innovation-assessment.md:62`)
- **Tier labelling exists in code and is claimed as current capability**: "ZK path shows one boolean; **plain path
  labelled as linkable in the UI (`PresentationTier`)**" (`estonia-alignment-assessment.md:178`, matrix row A2).
- **Tier labels are a log/journal feature, certificate-derived requester fields are not**: "presentation journal and
  tier labels exist; certificate-derived RP fields do not" (`estonia-alignment-assessment.md:179`, matrix row A3 —
  ARF Topic 19 `DASH_01–DASH_03d`).
- **Refusal vs cancellation is asserted as existing behaviour on the fixed dispatch path**: "DC API request
  handling, protocol dispatch, registry-doc-type-only registration, **refusals distinguishable from cancellation in
  the fixed path**" — status **partial** (`estonia-alignment-assessment.md:201`, matrix row S4).
- **What a refusal must look like to a holder** (O2 acceptance criterion ii, verbatim): "the missing-registration and
  unattested-request cases refuse, and **the refusal is distinguishable from cancellation in the UI contract** (the
  fork's own `DcApiRequestDispatch` work shows why this matters)" (`innovation-assessment.md:305-308`).
- **What a refusal must look like for an unmapped threshold** (O6 acceptance criterion ii): "a request for a
  threshold the wallet does not hold is refused **with a user-visible reason**, never answered with a neighbouring
  threshold" (`innovation-assessment.md:538-541`).
- **Two-predicate requests are refused today, controller-blind**: "not implemented; the wallet refuses
  two-predicate requests outright because it cannot read registrations" (`estonia-alignment-assessment.md:217`,
  matrix row E2); same finding from the O3 problem statement — "the wallet does not read registration certificates,
  so such a request is 'always refused here'" (`innovation-assessment.md:338-341`). The intended improvement is
  user-facing: "(i) A two-predicate request bearing a certificate whose intended use names both predicates is
  allowed, **with a user-facing explanation of the revealed range**" (`innovation-assessment.md:368-369`).
- **O3's affected components name the screens explicitly**: "`app/.../domain/presentation/` (`AgePredicatePolicy`,
  `HolderObligations`, **presentation screens**), `verifier/go/oid4vp` (request authentication), `certificates`
  parsing in `app/.../security/`" (`innovation-assessment.md:360-363`).
- **Plain-fallback privacy regression — "must remain visible in logs and UI"** (risk register): "the wallet labels
  the tier and the record; `EE-ZKP-055a` refuses plain-mdoc-only requests when the device is ZK-capable; **the
  fallback must remain visible in logs and UI**"; open sub-question: "whether an RP will accept a refusal instead of
  a plain presentation in practice" (`shared-appendices.md:176`, risk R7).
- **Proof size is filed as a usability risk**: "**Proof size (~360 KB) breaks a proximity or mobile-network
  budget**" — category "technical / **usability**" (`shared-appendices.md:175`, risk R6); mitigation "treat
  proximity Tier-2 as optional until measured".
- **Verification cost risk**: p95 4.91 s against a 1.0 s budget; "measure on target hardware … publish honest
  figures" (`shared-appendices.md:174`, risk R5).
- **Register on the critical path**: "never silently allow on lookup failure" (`shared-appendices.md:178`, risk R9);
  same rule recommended as a tradeoff decision: "interface-first with mocks, a bounded cache, and an explicit,
  logged decision about stale data — never a silent allow" (`shared-appendices.md:313-316`).
- **Fail-closed defaults are stipulated in four places** the corpus names: register (`shared-appendices.md:178`),
  certificate (`shared-appendices.md:177`, risk R8), circuit list (`shared-appendices.md:143`), and the wallet path
  (`estonia-alignment-assessment.md:217`).
- **Measurement honest-reporting is a corpus-level duty**: "Every figure below states its baseline, method,
  assumptions and confidence. Where the evidence does not support a number, the entry says so and proposes the
  measurement instead." (`estonia-alignment-assessment.md:347-349`); "unmeasured items are labelled explicitly
  rather than omitted" (`epics.md:975` restating NFR2/NFR7). For UX: any number shown to a user inherits this duty.
- **What the corpus expects for v1 outcomes** (use cases, all user-visible):
  - U1: "the wallet **labels whether the presentation was ZK or a documented fallback**"; "a `false` predicate is
    refused rather than transmitted" (`estonia-alignment-assessment.md:289-292`).
  - U3: "refusal when a ZK response is impossible, **with no silent downgrade**" (`estonia-alignment-assessment.md:309-310`).
  - U4: "the session's log names both credential ids; any missing credential fails the session with the id named; no
    isikukood appears anywhere in the wire transcript" (`estonia-alignment-assessment.md:318-319`).
  - U5: "time-to-first-successful-verification measured and reported; **refusals distinguishable from user
    cancellation**" (`estonia-alignment-assessment.md:328-329`).

### 3.2 Carrier naming — Digital Credentials API vs OpenID4VP `mso_mdoc_zk`

- The AV blueprint's **transport is the W3C Digital Credentials API**: the AV Profile fixes "transport of the proof
  over the W3C Digital Credentials API" (`innovation-assessment.md:50`); "transport over the W3C Digital Credentials
  API `[VERIFIED]`" (`estonia-alignment-assessment.md:32-33`).
- The **format identity is `mso_mdoc_zk`**, and it is the Commission's own dialect, not a fork convention: "The
  fork's `mso_mdoc_zk` profile … fixes a DCQL format identifier `mso_mdoc_zk`, a `meta.zk_system_type` registry, the
  proving system `longfellow-libzk-v1` and the `circuit_hash` parameter … and the Commission's **TS13** specifies
  exactly `"format": "mso_mdoc_zk"`, `"system": "longfellow-libzk-v1"`, `params.circuit_hash` … The `zk_system_type`
  field names in the fork's Go verifier and Kotlin wallet (`verifier/go/oid4vp/dcql.go`,
  `app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt`) **are the same strings**. This is not a private
  convention: it is the emerging EU profile." (`innovation-assessment.md:35-44`)
- **Two carriers exist in the field and only one is pinned**: "two carriers exist in the field — the interim JSON
  envelope and the CBOR `DeviceResponse` with `zkDocuments` — and the fork already normalises both behind one
  interface with committed golden vectors"; "TS13 defines the CDDL for the `ZkDocument`, so the shape is now pinned
  by a Commission document **even though the transport binding is not**" (`innovation-assessment.md:465-470`).
- Interfaces the carrier work names: "OpenID4VP `direct_post`/`direct_post.jwt` and the DC API"
  (`innovation-assessment.md:480-482`).
- Carrier identification as a user-of-the-suite question: "given a wallet's response, report **which carrier** it
  used, whether the proof verifies, and which binding properties hold (client_id, nonce, response_uri, key
  thumbprint, single use, predicate value)" (`innovation-assessment.md:477-480`); the fork's matrix row: "`mso_mdoc_zk`
  carrier matching TS13 identifiers — yes (both carriers, committed vectors)" (`shared-appendices.md:57`); TS13 row:
  "`mso_mdoc_zk` supported, `zk-jwt` missing" (`shared-appendices.md:106`).
- **UX consequence for naming**: a screen or report that says "Digital Credentials API" is naming the transport the
  EU blueprint mandates, while "`mso_mdoc_zk`" names the format; the corpus records that the transport binding is
  unpinned, so any user-facing label that fuses the two overstates the standard's state (cf. the compliance-language
  ban, `shared-appendices.md:145`: "an 'is this compliant?' answer cannot be given at all; **compliance language
  must be avoided**").

### 3.3 What users are told on refusal, lockout or an unverified requester

- **Refusal**: covered (§3.1) — distinguishable from cancellation (`innovation-assessment.md:305-308`,
  `estonia-alignment-assessment.md:201`), user-visible reason for an unmet threshold
  (`innovation-assessment.md:538-541`), tier label so a fallback is never silent
  (`estonia-alignment-assessment.md:178`, `shared-appendices.md:176`).
- **Lockout**: **nothing.** The research corpus contains no lockout, failed-attempt-count or consent-gate-resilience
  material at all. The terms `lockout`, `trust anchor`, `user presence` and `consent` return **0 hits** across all
  three research documents. The only device-security sentence is: "hardware-backed keys via multipaz
  `AndroidKeystoreSecureArea` (StrongBox where available), **PIN verification**, key cleanup on failure
  (`app/.../security/`)" — status partial, required change "mobile-device security requirements read directly from
  the CIR" (`estonia-alignment-assessment.md:170`, matrix row L5).
- **Unverified requester**: also **absent from the corpus**. Nothing in `docs/research/` describes a requester-name
  claim on a consent screen, an unvalidated CN, or what to show when the chain does not validate. The nearest corpus
  statements are the certificate-derived log/registration duties (`estonia-alignment-assessment.md:179`,
  matrix row A3; `estonia-alignment-assessment.md:217`, row E2) and O3's "user-facing explanation of the revealed
  range" (`innovation-assessment.md:368-369`). **The unverified-requester screen has no research backing and no
  story; its only source is the PRD** (§E).

### 3.4 Accessibility in the corpus

- ARF Topic 54 / `ACC_01`–`ACC_02`: "applies? **yes for a wallet/sandbox UI** — PRODUCT.md commits to WCAG 2.1 AA;
  reduced-motion and screen-reader announcements implemented; **no ETSI EN 301 549 test evidence** — status
  **partial**", required change "an accessibility conformance test pass against EN 301 549", verification "third-party
  accessibility audit" (`estonia-alignment-assessment.md:189`, matrix row A13).
- Estonia's baseline row: "Estonia's public-sector accessibility baseline (WCAG 2.1 AA) — **yes for the sandbox UI** —
  committed in PRODUCT.md; implemented browser-side — **partial** — required change: audit evidence"
  (`estonia-alignment-assessment.md:220`, row E5; index mirror `shared-appendices.md:117`).
- Risk register: "**Accessibility duty unmet at audit time** (WCAG 2.1 AA claimed; no EN 301 549 evidence)" —
  regulatory / adoption, M/M; mitigation "run an accessibility audit as a milestone exit criterion"; open input:
  "**what an Estonian public-sector audit will require for a wallet UI**" (`shared-appendices.md:182`, risk R13).
  Roadmap phase R1 exit criteria include "**accessibility audit closure**" (`shared-appendices.md:147`).
- Consequence for the spines: the accessibility commitment is claimed for **both surfaces**, and evidence is the
  only missing half — so DESIGN.md's accessibility section can state the standard, not the pass.

### 3.5 Tracing the three spec codes the brief names

`EE-ACC-004`, `EE-PRI-014` and `EE-PRI-023` do **not** appear in any research-corpus file. Their only in-repo
sources are the specification, and the PRD's reconciliation of it:

- **`EE-ACC-004 (SHOULD)`** — "The wallet SHOULD be available in Estonian, **Russian** and English at launch.
  Estonia's Russian-speaking population is a material share of the resident population and an Estonian-only wallet
  would predictably reproduce existing digital-inclusion gaps. **[The current share was not researched in this
  session — UNVERIFIED.]**" (`spec/EE-EUDIW-TS-1.0.md:1170`); listed as an unverified gap at `spec/EE-EUDIW-TS-1.0.md:1314`.
  The corpus records only that the spec has "EE-ACC 7" requirements, revision 1.7 dated 12 October 2026
  (`innovation-assessment.md:104`) — no tri-lingual duty, no Russian, anywhere in `docs/research/`.
- **`EE-PRI-014 (SHALL)`** — "The wallet SHALL provide the dashboard required by Art. 5a(4)(d): the transaction log;
  the list of relying parties and data exchanged; a one-step GDPR Art. 17 erasure request per **TS7 v1.0** and ARF
  Topic 48; and a one-step report of a relying party to the DPA per **TS8 v1.0** and ARF Topic 50. The DPA endpoint
  SHALL be Andmekaitse Inspektsioon." (`spec/EE-EUDIW-TS-1.0.md:982`; mapping row `spec/EE-EUDIW-TS-1.0.md:1648`).
- **`EE-PRI-023 (SHALL)`** — "No central log of age-verification events SHALL be created, other than the operator's
  minimal, aggregated, non-identifying service telemetry, which SHALL NOT include relying party identity at
  per-event granularity." (`spec/EE-EUDIW-TS-1.0.md:1075`)
- The corpus does touch the adjacent capability, without the code: "presentation journal and tier labels exist;
  certificate-derived RP fields do not" (`estonia-alignment-assessment.md:179`).

---

## 4. Gaps and conflicts that change the UX shape

| # | Finding | Evidence |
|---|---|---|
| G-1 | The sandbox site — the product's only no-build surface and the whole of PRODUCT.md's user story — has **no story and no UX spine**. The backlog says so outright. | `epics.md:185-190`; "sandbox" appears once, `epics.md:171` |
| G-2 | All four wallet consent gates decided into v1 (PRD FR-45…FR-48) have **no story**, because the backlog predates the PRD decision. The wallet surface in epics.md is represented only by log fields (Story 4.4) and an accessibility audit scoped to "parts of the interface". | `epics.md:52-56`, `epics.md:1634`; `prd.md:1722-1731` |
| G-3 | Lockout (PRD FR-47) and unverified-requester messaging (PRD FR-45) have **no source in the research corpus** — the design must come from the PRD's guardrails, not from research. | §3.3; `prd.md:1429-1435` |
| G-4 | The tri-lingual duty (Estonian/Russian/English) is a wallet `SHOULD` that no research document mentions, while the sandbox is stated as **bilingual Estonian-first**. Two language models in one product. | `spec/EE-EUDIW-TS-1.0.md:1170`; `PRODUCT.md:42`; `prd.md:1359-1362` |
| G-5 | The Art. 5a(4)(d) dashboard (`EE-PRI-014`) is **out of scope for v1**, while `EE-PRI-023`'s no-central-log carve-out is in force; the wallet's activity log is described as local and the user's. A consent/activity surface must therefore not promise the deferred dashboard. | `prd.md:1396-1403` |
| G-6 | Tier labelling (`epics 8.11`) and refusal-vs-cancellation (`epics 3.6`) are the only two UI-contract duties with stories, and both are worded for the RP/verifier path — neither is worded for the sandbox visitor. | `epics.md:1345-1365`, `epics.md:582-595` |
| G-7 | "Compliance language must be avoided" is a corpus-level constraint on all copy: no "conformant"/"certified"/"meets the profile" anywhere, including a UI. | `shared-appendices.md:145`, `epics.md:119`, `epics.md:1038-1045` |
| G-8 | Every number shown must carry baseline, method and confidence; unmeasured rows are labelled rather than omitted. This binds sandbox figures (proof size, timings) and report figures alike. | `estonia-alignment-assessment.md:347-349`; `PRODUCT.md:53-54`; `epics.md:163` |
| G-9 | Accessibility is claimed for both surfaces with **zero EN 301 549 evidence**, and the open question is literally "what an Estonian public-sector audit will require for a wallet UI". | `estonia-alignment-assessment.md:189`, `:220`; `shared-appendices.md:182` |
| G-10 | Proof size (~360 KB) is filed as a usability risk, not only a technical one. | `shared-appendices.md:175` |

---

## 5. §E — PRD/spec anchors (labels only; these are *not* among the three named sources)

Needed to resolve the PRD numbering the brief uses. Quoted only as identifier anchors:

- `PRD FR-6` weaker requests refused, reason shown, tier labelled — `prd.md:453-469`.
- `PRD FR-26` refusal distinguishable from cancellation — `prd.md:743-750`.
- `PRD FR-30` the visitor can try to cheat and watch it fail — `prd.md:796-804`.
- `PRD FR-34` a missing toolchain produces an honest failure, not a fake; names `make deps` — `prd.md:836-844`.
- `PRD FR-45` … `FR-48` the four consent gates — `prd.md:999-1044`.
- Sandbox §4.5 description (six-step guided flow, ledger, shortcuts named where they happen; `DESIGN.md` is the
  design authority) — `prd.md:763-770`.
- v1 scope guardrails: NFR-12 accessibility `prd.md:1353-1358`; NFR-13 language coverage per surface
  `prd.md:1359-1362`; NFR-14 honest failure surfaces `prd.md:1363-1365`; PG-4 no silent downgrade `prd.md:1393-1395`;
  PG-5 no central log + dashboard deferred `prd.md:1396-1403`; SG-2 consent claims true `prd.md:1429-1432`;
  SG-3 keys require user presence, gate resists guessing `prd.md:1433-1435`; OQ-8 resolved (four gates in v1)
  `prd.md:1722-1731`; OQ-10 resolved (kit ships, SM-3/SM-4 restated as unachieved) `prd.md:1732-1739`.
