# Reconcile — design system & shipped site

Run: `ux-eudi-wallet-poc-2026-10-23`. Written at Finalize, input-reconciliation pass.
Canonical decisions: `.memlog.md` (37 entries). Decisions read: every entry that names a token, a
surface, a component, a copy rule or the root authority — L10, L11, L13, L14, L17, L18, L19, L21,
L23, L24, L29, L30, L31, L32, L33, L34, L35, L36, L37, L39, L40, L41.

Inputs reconciled against, read in full:

| Input | Lines | Used for |
|---|---|---|
| `DESIGN.md` (repo root, shipped design authority) | 303 | tokens, rules, components, Do's and Don'ts |
| `.impeccable/design.json` | 122 | machine form of the root (schemaVersion 2) |
| `.impeccable/surfaces/sandbox-static-index-html.md` | 26 | direction contract, scope, constraints |
| `docs/CONFORMITY.md` | 202 | the wallet's documented on-screen behaviour and claim limits |
| `app/src/main/kotlin/ee/cyber/wallet/ui/theme/{Colors,Theme}.kt` | 17 / 84 | what the wallet actually ships (for the "bridge" claim) |
| `_bmad-output/planning-artifacts/prds/…/prd.md` | 1809 | FR-16, FR-28…FR-36, FR-45…FR-48, NG-2/NG-5, NFR-1/2/3/12/13/14/15 |
| `.working/extract-sandbox.md`, `.working/extract-wallet.md` | 472 / 234 | shipped surface states, wallet theming and screens |

Verdict labels used below: **contradiction** (two statements cannot both hold), **gap** (a decision
is silent where the input is binding), **verified** (checked, holds).

---

## 1. Verdict on the four questions asked

| Question | Verdict |
|---|---|
| Does the collision rule (root `DESIGN.md` canonical for the sandbox, imported by reference, no token restated/renamed/redefined) hold against what was decided? | **No, on three counts** — the coverage clause excludes the sandbox while L30/L37/L41 design new sandbox states (C1); "root Do's and Don'ts hold everywhere" contradicts the run's own English-only report and single independence notice (C2); "no different visual world" sits against the wallet's platform-conventional M3 stance (C3). |
| Does the wallet's status-hue pinning redefine root tokens? | **Not as written, but it is not reference-only either** — it selects half of each status family, leaves the caution family and M3's unmapped yellow unaddressed, and its "only family bridge" claim is already false in the shipped code (C4). |
| Does the FR-34 degraded-boot page or the tri-state additions contradict the root Do's and Don'ts (DESIGN.md:290-303), the Service Counter north star, or CONFORMITY's documented on-screen behaviour? | **The north star: no** (its text is scoped to "the page", DESIGN.md:179-183). **CONFORMITY: no direct contradiction.** **The root rules: yes, in three places** — L41 can reproduce the invisible-block bug it claims to fix and can put a refusal block beside a green check line (C5); the tri-state vocabulary has no family assignment while Caution is already spent on this page (C6); the degraded page is a page type the root does not describe and the one style it invites is illegal (C7). |

---

## 2. Contradictions

### C1. The collision rule's coverage clause excludes the surface the run then designs on
- **Decision.** L21: *"the repo-root DESIGN.md is IMPORTED as the canonical visual identity for the
  sandbox surface; this run's DESIGN.md covers only the surfaces the root does not — the Android
  wallet consent-gate surfaces, the AV-Profile conformance report, the RP acceptance kit."*
- **Decision.** L30 adds a new page on the sandbox surface (degraded boot, "the site's own visual
  identity"); L37 and L41 add typed states, blocks, icons and titles to the sandbox's step 4/5 rail.
- **Input.** The root is the sandbox's authority and is named for it: `DESIGN.md:2` (`name: ZK
  age-proof sandbox`), `DESIGN.md:175` (`# Design System: ZK age-proof sandbox`). Its
  components that the new states extend are the root's: `DESIGN.md:271` (step card states),
  `DESIGN.md:285` (proof timeline incl. the failed mark), `DESIGN.md:247` (rail, "Step n of 6").
- **Conflict.** Every new sandbox state is a change to a component the root owns, but L21 declares
  this run's `DESIGN.md` to cover only the three non-sandbox surfaces. Either the run edits the root
  authority (an act the collision rule neither authorises nor mentions — and the root carries no
  "generated" marker, unlike the surface files), or its own coverage clause is false.
- **Smallest fix.** Amend L21 to `…covers only the surfaces the root does not: the wallet, the
  report, the kit, plus the sandbox's new states (degraded boot, the typed end-states). Root
  components are extended in place; nothing in the root is renamed or revalued.` Or state that the
  root is edited by this run and record the exact edit set.

### C2. "Root Do's and Don'ts hold everywhere" contradicts two Tom-picked decisions
- **Decision.** L21: *"root Do's and Don'ts (DESIGN.md:290-303) hold everywhere."*
- **Decision.** L31: the report is *"root token family, committed to the repo, independence
  notice"* (one notice, placement unspecified). L33: *"the conformance report is English-only"*.
- **Input.** `DESIGN.md:295` — *"**Do** carry the independence notice in the utility bar, the info
  alert and the footer."* `DESIGN.md:296` — *"**Do** ship Estonian and English for every string,
  Estonian first."* The root's own utility-bar component pairs the notice with the toggle:
  `DESIGN.md:169-172` and `DESIGN.md:280` (*"with the independence notice on the left and ET/EN on
  the right"*). The PRD scopes both to the sandbox site — `prd.md:832` (FR-33, three placements) and
  `prd.md:823-824` (FR-32, the toggle on every page) — while `prd.md:1359-1362` (NFR-13) legislates
  **language per surface** and forbids a half-language surface only *"on the surface it belongs to"*.
- **Conflict.** L21 makes `:295` and `:296` bind the report; L31 and L33 do not satisfy them. The
  contradiction is L21↔L31/L33, not L33↔the root: L33 is compliant with NFR-13 (`prd.md:1359-1362`)
  and with the root's own scoping (`prd.md:832`), and only L21's "everywhere" breaks it. Mechanism:
  the report cannot use the root's `utility-bar` component (its own described pairing) without the
  ET/EN the run decided against.
- **Smallest fix.** Narrow L21 to *"holds everywhere the sandbox identity is worn"*, then state, per
  new surface, which Do's are inherited and which are exempt: the report inherits `:293` (claims tied
  to the run/runner), `:299`, `:300`, `:301`, `:302`, `:303`; ``:295`` becomes "notice present, one
  placement named" and `:296` becomes NFR-13's per-surface record. The acceptance kit (ET-first with
  EN, L33) already satisfies `:296` — say so.

### C3. "No different visual world" vs the wallet's platform-conventional M3
- **Decision.** L21: *"no different visual world"*. L34: wallet is *"PLATFORM-CONVENTIONAL
  MATERIAL 3 … everything else platform default. No root token transplant."* L29: the wallet's M3
  theming *"is a technical constraint to flag to bmad-architecture, not a visual identity to
  re-spec."*
- **Input.** The root's world is specific and not M3: Roboto 300 display and pill buttons
  (`DESIGN.md:186-188`, `:262-264`), 4px cards / 8px sheets (`DESIGN.md:188`, `:257`), one shadow on
  the active step (`DESIGN.md:251`), Material Symbols filled only for status marks (`DESIGN.md:241`).
  The wallet ships M3 defaults with no custom `Typography`, no custom `Shapes`, no elevation or
  spacing tokens (extract-wallet §b).
- **Conflict.** Three statements from the same run pull apart: "no different visual world" (L21),
  "platform default everywhere else" (L34), "not a visual identity to re-spec" (L29) — while L34 does
  re-specify the wallet's semantic status colours, which L29 explicitly puts on the flag-to-
  architecture side. On the wallet, TEDI's pill/4px/one-shadow world is *replaced* by M3, so "no
  different visual world" cannot be read literally.
- **Smallest fix.** Replace L21's phrase with *"no new invented visual world; the wallet stays
  platform-conventional Material 3 (L34) and the report/kit use the root token family"*, and split
  L29/L34 into two recorded lists: **to bmad-architecture** (light-only theme, `isSystemInDarkTheme`
  bypass in `PinActivity`/`DigitalCredentialsActivity`, no custom Typography/Shapes/Spacing tokens)
  and **specified here** (status hue pinning, the four gates' behaviour and copy).

### C4. The wallet's status pinning is neither reference-only nor a whole family, and its premise is stale
- **Decision.** L34: *"semantic status colours pinned to the root family hues (pass-green, refuse-red
  and their washes) recorded in the new DESIGN.md as the wallet's only family bridge."* L29: the spine
  *"references the root DESIGN.md token family by name and does not re-derive values."*
- **Input.** The root's status families are triads with a member L34 does not name:
  `pass-green`/`pass-wash`/**`pass-border`** (`DESIGN.md:20-22`), `refuse-red`/`refuse-wash`/
  **`refuse-border`** (`DESIGN.md:23-25`), and a third family `caution-ink`/`caution-wash`/
  `caution-border` (`DESIGN.md:26-28`) that `DESIGN.md:218` assigns to *"the 'test shop' badge and the
  age gate"*. The Tinted Status Rule makes the border constitutive: *"It is always a light wash, a 1px
  border in the status hue, and text in the dark status ink"* (`DESIGN.md:222`).
- **Input (the claim's premise).** The shipped wallet already carries two of the root's hues and not
  the third: `blue_600 = #005AA3` (`Colors.kt:5`) is `state-blue` (`DESIGN.md:5`) and is already the
  M3 `primary` (`Theme.kt:18`); `red_700 = #AC3232` (`Colors.kt:13`) **is** `refuse-red`
  (`DESIGN.md:23`); the M3 `error`/`errorContainer` roles point at a *different* red,
  `red_600 = #D73E3E` (`Theme.kt:33,35`); `yellow_600 = #FFB511` (`Colors.kt:14`) is raw and mapped to
  no role; `green_600 = #308653` (`Colors.kt:15`) equals the sandbox's tokenless
  `--success-strong`, and is not the root's `pass-green #266b42`.
- **Conflict (three, in order of force).**
  1. *"The wallet's only family bridge"* is false against the shipped code: `state-blue` is already
     the wallet's `primary` — a hue-level bridge outside pass/refuse — and `refuse-red` is already
     present as an unused token while the *mapped* error role is a different red. The decision must
     name which red becomes the M3 `error` role (`red_600` as shipped, or `refuse-red`), because the
     two are different values.
  2. Half a family: pinning ink + wash without the border drops the 1px status-hue border that
     `DESIGN.md:222` makes part of a status surface. M3 has no border role, so if the bridge is ink +
     wash only, the wallet's status surfaces are a different construction from the sandbox's — which is
     the reading L21's "no token … redefined" is meant to prevent.
  3. Caution is unaddressed: the root has a third family (`DESIGN.md:26-28`, `:218`) and the wallet
     has one unmapped amber (`yellow_600`). FR-47's lockout ("wait it out", L40) is the state most
     likely to need it. Nothing decides whether the wallet gets a caution family, and a status family
     added to the *wallet* is a new token in the root's name-space.
  4. Values recorded in Compose are restatements by construction. The bridge is compliant with L29
     only if the spine says *where* the value lives (a reference to `DESIGN.md` with a token-name
     comment per role), otherwise the Kotlin copy becomes the second definition L21 forbids.
- **Smallest fix.** Record the bridge as a **role mapping table**: M3 role → root token name → claim
  that the value is the root's; include the border members or state that M3 cannot carry them and how
  the wallet's status surfaces compensate; state the M3 `error` red explicitly; state the caution
  stance (mapped from `yellow_600`, or "the wallet ships no caution state at v1").

### C5. The tri-state placement can reproduce the bug it exists to fix
- **Decision.** L41: *"refusal, cancellation and retryable error each render as a typed block at the
  rail step where the run stopped, with distinct icon, title and cause… This fixes the current
  invisible cancellation (app.js:351-356 unhides the declined message inside step 4, then go(3) hides
  it)."*
- **Input.** Step visibility is a three-state machine and only one state shows a body:
  `DESIGN.md:271` — *"Active: a State Blue border and the one lift. Locked: Grey Paper with Light
  Slate headings. Done: collapsed to its heading plus a green check-circle summary line."* Shipped
  implementation: `app.js:207` sets the state; `app.css:178-181` (`locked` hides `.step__body`);
  `app.js:217,226-239` writes the green check summary for `done` steps.
- **Conflict.** L41 does not state the *step state* that must carry the block. On the cancellation
  path the current handler still calls `go(3)` (L24's own evidence, `app.js:351-356`), which locks
  step 4 — so a block anchored "where the run stopped" lands in a `display:none` subtree and is
  invisible again, exactly as before. On a `done` step the block would render beside the green
  check-circle summary line (`DESIGN.md:271`), so a cancellation would carry a success mark. A
  step card has one body slot; the decision is silent on which state it must be in.
- **Smallest fix.** Add to L41: the bearing step stays `active` until the state is acknowledged or
  the journey moves on; a step bearing a refusal or cancellation gets **no** green check summary
  (the summary slot is per-state, not per-step-index); and state what happens to steps already
  summarised below the failure.

### C6. Three typed states, no family assignment, and the third root family is already spent
- **Decision.** L37: *"typed states with distinct icon+title+cause, aria-live announcements"*. L41:
  *"with distinct icon, title and cause"*. L13/L14: distinguishability without colour-only
  signalling (also `prd.md:1357`, NFR-12).
- **Input.** The root assigns each family a meaning: `DESIGN.md:216` Pass = *"verified, blocked attack,
  credential signed"*; `:217` Refuse = *"verification refused, invalid input, 'not in the credential'
  marks"*; `:218` Caution = *"the 'test shop' badge and the age gate"* — and both `:218` uses are on
  this page. `DESIGN.md:222` forbids colouring a whole region; `DESIGN.md:301` forbids the stripe
  workaround; `DESIGN.md:241` allows filled icons only for status marks.
- **Input (today's behaviour).** The shipped site already renders a refusal and a retryable error with
  **the same icon and the same refuse ink**; only the container differs (extract-sandbox §b.5,
  `app.js:260` vs `app.js:426-434`).
- **Conflict.** A retryable error belongs to none of the three assignments in `:216-218`; the one
  unspent family is Caution, and Caution is already carrying two meanings on the same page. The
  decision lists three *differentiators* (icon, title, cause) but no family, so the implementation can
  legitimately keep the current one-family-for-two-states arrangement and still satisfy L37's words —
  and any attempt to fix it by colour meets `:222`/`:301`.
- **Smallest fix.** State the family per state (refusal = Refuse; cancellation = neutral, no status
  family, since a decline is not a fault; retryable error = Refuse wash **with** a non-filled error
  icon, distinguished by icon/title/cause only), and record that the retryable-error family assignment
  is an addition to `DESIGN.md:217`'s list — which, per C1, means deciding where that addition lives.

### C7. The degraded-boot page has no description in the root, and the obvious treatment is illegal
- **Decision.** L30: *"the server binds and serves one honest full-page state in the site's own visual
  identity (prover/verifier not built, names make deps, no rail, no timeline)"*.
- **Input.** The root describes the sandbox as a journey page: rail and step cards
  (`DESIGN.md:247`), the timeline (`DESIGN.md:285`). Its full-bleed bands are *"the navy cheat band,
  the grey limits band, the ink footer"* (`DESIGN.md:247`) — not status-coloured. `DESIGN.md:222`
  forbids a status-coloured region; `DESIGN.md:301` forbids the stripe. FR-34's own consequences are
  satisfied by L30's content (`prd.md:842-844`: names `make deps`, no timeline, no verdict) and the
  page is a legitimate subject for the north star's idiom (*"a counter that tells you exactly what it
  is doing with your data"*, `DESIGN.md:181`).
- **Conflict.** "In the site's own visual identity" is under-specified in exactly the dimension that
  matters: a page whose only content is a failure invites an amber or red full-page treatment, which
  `:222`/`:301` prohibit, and no root component fits it (no alert-page, no not-built component among
  `DESIGN.md:129-172`). Nothing in the decisions says which components the page uses or what its
  single status surface is. Also, the documented deploy path cannot produce it: `deploy.sh:26` exits
  the build host when a binary is missing and `compose.yml:9-10,23,27` runs `restart: unless-stopped`
  + `read_only: true`, so the page is reachable only by running `server.py` directly (extract-sandbox
  §b.10).
- **Smallest fix.** Specify: paper ground, the root's `Info Alert` pattern or a wash + 1px border
  status surface, the `data` typography for `make deps`, a State-Blue single action (One Acting Blue,
  `DESIGN.md:220`), and no full-bleed status band. State that the page is a new sandbox surface and
  (C1) where its spec lives.

### C8. L17's reduced-motion claim is true of the proof mark only
- **Decision.** L17: *"contrast held by existing tokens … reduced motion honoured — the shipped proof
  mark goes static under reduced motion, keep that."*
- **Input.** True for the mark: `DESIGN.md:285`, `app.css:293`. Not true site-wide: `app.css:51`
  (`html { scroll-behavior: smooth }`), `:158` (rail dot `.3s`), `:277-278` (spine `scaleY .6s`),
  `:280` (phase mark `.3s`) are ungated; only `:186` and `:293` gate anything (extract-sandbox §b.8).
  FR-35 phrases the obligation the same way the decision does, with the mark as its example
  (`prd.md:854-855`).
- **Conflict.** L17 records the floor as met on the evidence of one gate; the extract it was written
  from names four ungated motion paths. The decision does not contradict the input, but it leaves the
  site's own reduced-motion gap unspecified while presenting the floor as satisfied.
- **Smallest fix.** Either add the four paths to the spine's reduced-motion clause, or record them as
  a known gap with the FR-35 consequence they touch.

---

## 3. Checked and consistent (no contradiction found)

- **North star.** No decision contradicts `DESIGN.md:179-183`. Its text is scoped to *"the page"* (the
  sandbox); the run's departures (wallet M3, L34) are on surfaces the north star does not describe,
  and the report/kit inherit only the token family (L31). The degraded page is inside the idiom
  (L30, above).
- **CONFORMITY's documented on-screen behaviour.** No decision contradicts a row: L14's
  refusal/cancellation/retryable distinction is *stronger* than what `CONFORMITY.md` records, and
  L13/L16 only narrow what may be claimed (`CONFORMITY.md:17`, `:50`, `:140-144`). Nothing in the run
  asserts a status CONFORMITY withholds.
- **Contrast (computed, sRGB relative luminance).** L17's "contrast held by existing tokens" holds for
  every status pairing: pass-green on pass-wash 5.68, refuse-red on refuse-wash 5.62, caution-ink on
  caution-wash 7.46, ink on blue-mist 13.08, ink on cellar-plaster 15.38, blue-steel on state-navy
  6.25, white on state-blue 7.02, footer-text on ink 11.72; the two borders `DESIGN.md:212` claims
  "≥3:1 against white" compute at pewter 3.03 and input-stroke 3.69 — the claim is true, with 0.03 to
  spare on pewter. One caveat no decision carries: `#308653` (`--success-strong`, `app.css`, no
  DESIGN.md token) computes **4.51:1** on white — it passes text contrast by 0.01 and must not be
  moved onto any tinted ground.
- **Independence notice and branding.** L19 agrees with `DESIGN.md:295`/`:299` and `prd.md:832-834`;
  the shipped site already carries it in all three places in both languages (extract-sandbox §b.9).
- **Mono-is-data, one-lift, spectacle.** L13/L37/L41 keep monospace for machine values
  (`DESIGN.md:243`, `:303`) and add no shadow or dark ground (`DESIGN.md:251-253`, `:302`).
- **Shapes/radius.** New states reuse existing radii per `DESIGN.md:257`; no decision introduces a new
  radius, and the sheet radius stays reserved for another party's surface (`DESIGN.md:272`).

---

## 4. UX-relevant input content no decision carries (dropped ideas)

### 4a. Root `DESIGN.md` content

1. **The four named rules are outside L21's citation.** L21 extends `DESIGN.md:290-303` only. The rules
   with the sharpest consequences for new surfaces sit elsewhere: One Acting Blue (`:220`), Tinted
   Status (`:222`), Mono-Is-Data (`:243`), One Lift (`:253`). Nothing decides whether they bind the
   wallet, the report or the kit — yet the wallet's M3 buttons are not State Blue and M3 cards cast
   shadows by default (extract-wallet §b). Also uncarried: `design.json:102-107` records these four as
   `narrative.rules` with a `section` field, i.e. the machine form the new spine could reference.
2. **No decision touches `DESIGN.md:265`** — the busy/disabled treatment (*"a Fog fill with Light
   Slate text, plus a `progress` or `not-allowed` cursor. A busy button keeps its label"*). The four
   gates and the tri-state blocks introduce exactly the busy states this rule governs (L39, L40, L41);
   `design.json:43,51` deliberately omit it from its button samples.
3. **The shipped unconditional "Signed" tag remains an existing breach of `DESIGN.md:293`/FR-28.**
   `index.html:134` renders `tag--success` "Allkirjastatud" unconditionally, and the extract names it
   as *"the one status label that is not derived from a check"* (extract-sandbox §b.7). L13 restates
   the run-tied-numbers rule and no decision addresses the exception.
4. **`DESIGN.md:294`** (*"name the sandbox's shortcuts where they occur, in Slate narration"*) is
   carried for the sandbox only via L13's claim limits; the new gate copy's shortcut disclosures
   (mock issuer, mock WP, `app.js`/`server.py` honesty) have no decision naming the Slate-narration
   form on any surface.

### 4b. `.impeccable/design.json` structure — no decision mentions the file at all (L9 lists it as an
inherited source only)

1. **It is behind the shipped site and nothing says whether it is regenerated.** Its `components`
   array has no entry for the step card, credential, consent sheet, verdict, ledger, figures strip,
   attack card, rail, log disclosure, or the danger/warning alert variants (extract-sandbox §d,
   `design.json:37-90`); `Info Alert` and `Proof Phase` carry no `refersTo` (`:78-82`, `:85-89`); the
   button samples omit the `aria-busy`/disabled treatment (`:43`, `:51`); the `Proof Phase` sample
   draws its done mark with an inset `box-shadow` ring (`:89`) where the site uses a white-check SVG
   — so a generator reading it would reproduce two wrong components.
2. **"Import by reference" has no machine backing for most of the palette.** `colorMeta` covers 10 of
   27 colours (`design.json:7-16`); absent are `state-blue-deep`, `blue-steel`, `pewter`,
   `input-stroke`, and every status wash/border except `caution-border` (extract-sandbox §d). A new
   surface that must "reference the root token family by name" (L29) can only do so for those 10.
3. **Tonal ramps are unnameable.** Three ramps (`design.json:7`, `:8`, `:11`), dark→light, steps
   nameless, and two ramp endpoints are colours `DESIGN.md:5-31` does not define. No decision says
   whether the new surfaces may use these intermediate tones — which matters the moment C6 needs a
   second "bad" tone or C4 needs a caution tone.
4. **`colorMeta` naming collisions.** `caution-border` carries `displayName: "Caution"` while the
   family's ink member is `caution-ink` (`design.json:15`); `pass-green`/`refuse-red` carry the family
   names "Pass"/"Refuse" (`:13-14`). Any spine that cites design.json names rather than DESIGN.md
   names inherits this ambiguity (extract-sandbox §d, final row).

### 4c. Surface-contract constraints (`.impeccable/surfaces/sandbox-static-index-html.md`)

1. **`primary_target: "sandbox/static/index.html"`, `related_targets: []` (`:3-5`) — and no decision
   says who owns the new artifacts' contracts.** The degraded-boot page (L30), the generated report
   (L31) and the key-screen mocks (L42) add surfaces with no `.impeccable/surfaces/*.md` entry, while
   the contract model treats one file per surface.
2. **FINISH (`:26`) — "every shipping raster carrying its provenance".** No decision carries asset
   provenance, and the run's own plan produces key-screen mocks (L42). This is the only place in the
   inputs that makes provenance a completion condition.
3. **Scope/Mode (`:8`) — "Mode: Operate, narrated (the visitor completes a task while being told what
   each step means)".** The degraded page (L30) is not an operate-narrated surface; no decision states
   its mode or what it drops from the mode's obligations.
4. **Constraints (`:12`) — "Visual world pinned by the user: ria.ee / TEDI. No concept roll, because
   the brief pinned the world."** Nothing states whether this pinning extends to the wallet, the
   report or the kit — the exact question C3 leaves open.
5. **Audience (`:10`) — "reviewers, relying parties, curious citizens; no phone, no spec".** The gates
   and the report are designed for readers the contract's audience statement does not cover (an
   integrator with no support channel, L36); the audience record is not extended anywhere.

### 4d. CONFORMITY rows with UI consequences

1. **`CONFORMITY.md:166` (EE-PRO-001)** — three UX mechanisms no decision carries: the refusal is
   *"shown as the unencrypted-request message, not a retryable network error"* (behavioural guarantee
   behind L14's distinction); the negative answer is sent *"in the background, from the application
   scope so closing the error screen does not cancel it"* (dismissal semantics); the refusal is
   written *"to the activity log for each requested document type"* (one row per requested type).
   L14 carries the distinguishability rule, not these three.
2. **`CONFORMITY.md:170` (EE-PRO-010)** — the consent screen labels the channel (QR = cross-device
   with no proximity check; deep link = link, warning that a scanned link may also be cross-device)
   and *"does not claim 'same-device'"*. None of L15's four gates carries channel labelling.
3. **`CONFORMITY.md:177` (EE-POA-013)** — the last-attestation pattern: a device that cannot prove
   shows the last-attestation notice *with the EE-ZKP-042 notice*, *"tapping share is the approval"*,
   and the row is *"logged linkable"*. No decision carries the two-notice state or approval-by-share
   (the shipped screens implement it, extract-wallet §c.1); L16 carries the linkability disclosure
   only.
4. **`CONFORMITY.md:183` (EE-POA-031)** — the two-sided statement and its per-path vocabulary (the
   user is told; the RP gets nothing sent / `RESULT_CANCELED` / session terminated). L16 carries "no
   false predicate transmitted"; the per-path answers the gate copy must state are dropped — and
   FR-48's proximity gate is where they are needed.
5. **`CONFORMITY.md:188`, `:190`** — *"No access-CA anchors are configured, so every DC API and
   proximity request is unauthenticated"* and the consent screen *"shows the subject only on a pass"*.
   Consequence L39 does not state: on this checkout the FR-45 hard refusal applies to **essentially
   every** DC API and proximity request, so the spine must design the wallet's behaviour when no
   requester can ever be verified (a dead-end, not an edge case). `CONFORMITY.md:190`'s pass-only
   subject rule is also the precedent for the sandbox's consent sheet.
6. **`CONFORMITY.md:191` (EE-PRO-026)** — the wallet's *"'not answered' notice not assessed"*: a state
   named as unbuilt in the input, on a gate surface, and designed nowhere.
7. **`CONFORMITY.md:168`, `:166`, `:169`, `:173`, `:182`** — the library-level refusal *causes* the
   gate copy must name (SIOPv2 refused before consent; a request that is not `direct_post.jwt`
   encrypted under P-256 + A128GCM/A256GCM; unsigned requests over DC API; more than one
   `age_over_NN`; deferred issuance failing with a stated reason). L15 names three certificate-side
   causes (*"no trust anchor, unparseable chain, name mismatch"*) only.
8. **`CONFORMITY.md:158-160`** — *"On-device evidence: None"*, device-key attestation mock and
   self-signed, challenge UUID unverified, `issuerAuth` on-device validation absent. L13 carries the
   PENDING-DEVICE *figures* stance; the rows that constrain what a trust signal may imply on screen
   are uncarried.
9. **`CONFORMITY.md:129-147`** — *"Every APK this repository can produce is debug-derived"*,
   `EE-CNF-005`, and the issuer being a CLI or mock. L13 covers only the claim vocabulary; no decision
   says how that provenance travels with any UI evidence the run publishes.
10. **`CONFORMITY.md:199`** — the reference verifier's deviation (`ECDH-ES+A256KW`, JWK with no `alg`,
    `x509_san_dns` default) means *"the Android redirect path cannot answer this verifier"*. That is a
    named refusal with a cause and the obvious fourth acceptance case for the kit (L10, L36); no
    decision carries it.
11. **`CONFORMITY.md:1-12` and `:201`** — the note *is* the `EE-ZKP-003` statement, the `EE-ZKP-060`
    inventory and the build-provenance statement, and it is *"updated when any row above changes"*
    while *"it is not pushed anywhere"*. L31/L32 create a second, generated, profile-ordered report and
    no decision states the relationship (does the report include the spec-required wallet rows, or only
    AV-Profile clauses? which document carries `EE-ZKP-003`?). Without that, the generated report reads
    like the conformity statement and inherits FR-36's ban.

### 4e. PRD rows with UI consequences

1. **NG-5 and NFR-15 (`prd.md:1066-1068`, `:1366-1368`) are cited nowhere in the memlog.** They require
   the declared 109-bit shortfall and *"the limits (`ZKP_08`, 109-bit circuits, no device
   measurements)"* to travel with the claim — on the report (L31) that means a verdict vocabulary
   beyond pass/fail/out-of-scope: `CONFORMITY.md:17` (must not be read as claiming `ZKP_08`) and
   `:50` (SHALL NOT be advertised as meeting the ACM recommended level) describe a **conditional**
   state that L32's "front summary of counts and verdicts" has no slot for. A green per-clause PASS
   badge on such a clause is the concrete risk.
2. **FR-37 (`prd.md:881-889`) is cited nowhere** — every published figure states baseline, method,
   assumptions and confidence; L13 cites NFR-2/NFR-7 for its figures stance but not FR-37's per-figure
   label, which is a presentation rule on the committed report and on the kit's
   time-to-first-verification figure.
3. **`prd.md:1136-1141`** — the release-scope table names the *published clause maps* and the `ZKP_08`
   report as separate deliverables of the same claim-discipline work; the report decision (L31-L33)
   covers one artifact and no decision places the other two.
4. **FR-29 (`prd.md:785-794`)** — *"a change to what is disclosed changes the ledger without a copy
   edit"*. L13 restates the numbers rule; no decision carries the ledger's data-driven wording
   requirement, which the tri-state work (L41) touches because a non-pass run renders the
   `learned_none` variant.

---

## 5. What the gate should refuse to let through

1. L21 rewritten so its coverage clause names the sandbox's new states, and its "hold everywhere"
   qualified per surface (C1, C2).
2. The wallet bridge recorded as a role mapping with the border members, the M3 `error` red, and the
   caution stance stated (C4).
3. L41 extended with the required step state and the no-green-check rule for refused/cancelled steps
   (C5).
4. A family assignment per tri-state, and a decision on whether the retryable-error meaning joins
   `DESIGN.md:217` (C6).
5. One paragraph of degraded-page styling, and the statement that it is a new sandbox surface (C7).
6. The four named rules' scope stated explicitly (`DESIGN.md:220`, `:222`, `:243`, `:253`), since
   L21's citation covers `290-303` only (4a.1).
7. The report's clause *states* enumerated — covered / not covered / out of scope (with reason) /
   **conditional, gap declared** — before any colour is chosen (4e.1; `prd.md:1066-1068`,
   `CONFORMITY.md:17`, `:43-54`).
