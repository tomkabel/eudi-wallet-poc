---
name: eudi-wallet-poc — v1 new surfaces
description: >-
  Visual identity for the three surfaces the sandbox's canonical DESIGN.md does not cover — the
  Android wallet's consent-gate surfaces (PRD FR-45…FR-48), the EU AV-Profile conformance report
  (FR-11…FR-16), and the Estonian relying-party acceptance kit (FR-21…FR-27).
status: draft
updated: 2026-10-23
imports:
  - file: '../../../../DESIGN.md'
    name: ZK age-proof sandbox (repo-root DESIGN.md)
    role: >-
      Canonical visual identity for the sandbox surface, and the source of every token value this
      file references. The sandbox keeps that identity untouched; this file adds no token to it.
    rule: >-
      Reference only. No root token is restated, renamed or redefined here, and no root value is
      overridden. Where this file and the root file disagree, the root file wins.
colors:
  # Declared, not defined. Every entry below is a reference into the imported root family, so this
  # file deliberately carries no hex; resolution is colors.<name> → root DESIGN.md → colors.<name>.
  # Root tokens the new surfaces do not use: {colors.footer-text}, {colors.cellar-plaster}.
  state-blue: '{colors.state-blue}'
  state-blue-deep: '{colors.state-blue-deep}'
  state-navy: '{colors.state-navy}'
  blue-mist: '{colors.blue-mist}'
  blue-haze: '{colors.blue-haze}'
  blue-steel: '{colors.blue-steel}'
  ink: '{colors.ink}'
  slate: '{colors.slate}'
  slate-light: '{colors.slate-light}'
  paper: '{colors.paper}'
  paper-grey: '{colors.paper-grey}'
  fog: '{colors.fog}'
  hairline: '{colors.hairline}'
  pewter: '{colors.pewter}'
  input-stroke: '{colors.input-stroke}'
  pass-green: '{colors.pass-green}'
  pass-wash: '{colors.pass-wash}'
  pass-border: '{colors.pass-border}'
  refuse-red: '{colors.refuse-red}'
  refuse-wash: '{colors.refuse-wash}'
  refuse-border: '{colors.refuse-border}'
  caution-ink: '{colors.caution-ink}'
  caution-wash: '{colors.caution-wash}'
  caution-border: '{colors.caution-border}'
  info-border: '{colors.info-border}'
typography:
  # Wallet — platform convention, stated semantically. The app defines no custom Typography()
  # today and uses the Compose Material 3 default scale directly; this file re-specifies none of it.
  display-small:
    note: 'Platform convention — Compose Material 3 Display Small'
  headline-medium:
    note: 'Platform convention — Compose Material 3 Headline Medium (requester name, success titles)'
  title-large:
    note: 'Platform convention — Compose Material 3 Title Large (app header)'
  title-medium:
    note: 'Platform convention — Compose Material 3 Title Medium (PIN surface title, party line)'
  body-large:
    note: 'Platform convention — Compose Material 3 Body Large'
  body-medium:
    note: 'Platform convention — Compose Material 3 Body Medium (consent copy, cause lines)'
  body-small:
    note: 'Platform convention — Compose Material 3 Body Small (captions, footer notes)'
  label-medium:
    note: 'Platform convention — Compose Material 3 Label Medium'
  report-and-kit:
    note: >-
      The root type ramp, inherited whole: {typography.display}, {typography.headline},
      {typography.title}, {typography.title-small}, {typography.lead}, {typography.lead-compact},
      {typography.body}, {typography.body-small}, {typography.label}, {typography.small},
      {typography.caption}, {typography.data}, {typography.figure}, {typography.figure-compact},
      {typography.clock}, {typography.icon}. Machine values (clause ids, commands, hashes, log
      lines) use {typography.data}; the Mono-Is-Data Rule applies unchanged.
rounded:
  # Wallet — platform convention; the app defines no custom Shapes{}.
  extra-small:
    note: 'Platform convention — Compose Material 3 Shapes.extraSmall'
  small:
    note: 'Platform convention — Compose Material 3 Shapes.small'
  medium:
    note: 'Platform convention — Compose Material 3 Shapes.medium (cards)'
  large:
    note: 'Platform convention — Compose Material 3 Shapes.large'
  extra-large:
    note: 'Platform convention — Compose Material 3 Shapes.extraLarge (sheets, dialogs)'
  report-and-kit:
    note: >-
      The root radius scale, inherited whole: {rounded.hairline} (code wells),
      {rounded.default} (cards, clause rows, notices), {rounded.sheet} (anything standing for
      another party's surface), {rounded.pill} (controls, the language toggle).
spacing:
  # Wallet — the Android 4dp baseline grid. No spacing token exists in the app; these are the
  # conventions its own components already follow.
  wallet-grid:
    note: >-
      Android 4dp grid. Screen content padding 16dp horizontal / 24dp vertical (the app's own
      AppContent padding); VSpace / HSpace / WSpace are its only spacing helpers. Minimum
      interactive target 48dp.
  report-and-kit:
    note: >-
      The root scale, inherited whole: {spacing.s1}, {spacing.s2}, {spacing.s3}, {spacing.s4},
      {spacing.s5}, {spacing.s6}, {spacing.s7}, {spacing.s8}.
components:
  # The two surfaces whose visual world is the root family. Entries reference root tokens only —
  # the wallet's Material 3 role bridge is a mapping, recorded in the Components section below,
  # and is declared here as no value at all.
  report-shell:
    background: '{colors.paper}'
    textColor: '{colors.ink}'
    rounded: '{rounded.default}'
  report-summary-strip:
    background: '{colors.paper-grey}'
    borderColor: '{colors.hairline}'
    rounded: '{rounded.default}'
  report-clause-row:
    background: '{colors.paper}'
    borderColor: '{colors.hairline}'
    rounded: '{rounded.default}'
  report-state-pass:
    ink: '{colors.pass-green}'
    wash: '{colors.pass-wash}'
    border: '{colors.pass-border}'
  report-state-fail:
    ink: '{colors.refuse-red}'
    wash: '{colors.refuse-wash}'
    border: '{colors.refuse-border}'
  report-state-error:
    ink: '{colors.caution-ink}'
    wash: '{colors.caution-wash}'
    border: '{colors.caution-border}'
  report-state-not-executed:
    ink: '{colors.slate-light}'
    wash: '{colors.paper-grey}'
    border: '{colors.hairline}'
  report-state-out-of-scope:
    ink: '{colors.slate}'
    wash: '{colors.fog}'
    border: '{colors.hairline}'
  report-independence-notice:
    background: '{colors.blue-mist}'
    borderColor: '{colors.info-border}'
    rounded: '{rounded.default}'
  kit-case-card:
    background: '{colors.paper}'
    borderColor: '{colors.hairline}'
    rounded: '{rounded.default}'
  kit-outcome-refused:
    ink: '{colors.refuse-red}'
    wash: '{colors.refuse-wash}'
    border: '{colors.refuse-border}'
  kit-outcome-cancelled:
    ink: '{colors.slate}'
    wash: '{colors.paper-grey}'
    border: '{colors.hairline}'
  kit-outcome-retryable:
    ink: '{colors.caution-ink}'
    wash: '{colors.caution-wash}'
    border: '{colors.caution-border}'
  kit-install-command:
    fontFamily: '{typography.data}'
    background: '{colors.fog}'
    rounded: '{rounded.hairline}'
  kit-language-toggle:
    background: '{colors.paper}'
    textColor: '{colors.state-blue}'
    activeIndicator: '{colors.state-blue}'
    rounded: '{rounded.pill}'
---

# Design System: eudi-wallet-poc — v1 new surfaces

## Brand & Style

This document is a delta, not a second design system. The sandbox's visual world — "The Service Counter" in repo-root `DESIGN.md` — is the project's identity, and it stays exactly as it ships. What this file covers is the three surfaces that world does not reach: the Android wallet's consent-gate surfaces, the EU AV-Profile conformance report, and the Estonian relying-party acceptance kit.

Two visual worlds, chosen for two different reasons.

The **wallet** follows the platform: Compose Material 3, as an Android app should. A citizen who has used any Android app already knows the gestures, the shapes and the system prompts; a bespoke visual language here would buy nothing and cost familiarity. The single deliberate bridge back to the project's family is semantic status colour — the states the four consent gates introduce must read the same on the phone as they do on the page (see *The status bridge* under Components). Everything else is platform default.

The **conformance report** and the **acceptance kit** render in the root token family, because both are read as documents by people who already met this project through the sandbox, and because both must be reproducible artefacts rather than products. They use the root's neutrals, its status triad, its type ramp and its hairline discipline — by reference, never by re-derivation.

What holds across all three: the product principles the PRD pins. Show, don't claim — every number comes from the run that just happened. Say what the counterparty did not learn as plainly as what it did. Name the shortcuts where they occur. And, for the surfaces this release introduces: a report that hides its out-of-scope clauses is not a report. The wallet's consent screen is held to the same discipline in the negative: its claims must be true (PRD SG-2), so a requester is never dressed as verified when the chain did not validate.

No mockups or wireframes exist in this run yet: the four consent-gate surfaces and the conformance report are the key-screen candidates, and until those land both spines are the contract they will be checked against — and win on conflict with any mock, wireframe or import.

Nothing new is invented visually. No token is added to the root family, no different aesthetic world is opened, and no state branding appears anywhere (PRD NG-13, FR-33).

## Colors

The new surfaces make almost no new colour demands: the root's neutrals carry the documents, and the root's status triad — a light wash, a 1px border in the status hue, dark status ink — is the entire vocabulary of state. All of it by reference (`{colors.*}` in the frontmatter resolves into the root family).

**Neutrals.** `{colors.ink}` is text, `{colors.slate}` and `{colors.slate-light}` are guide narration and captions, `{colors.paper}` and `{colors.paper-grey}` alternate the report's surfaces, `{colors.fog}` is the code and command well, `{colors.hairline}` is every divider. `{colors.state-blue}` is the only thing that acts: the report's and the kit's controls, links and active language.

**Status triad, and what each state means on these surfaces.**

- **Pass** (`{colors.pass-green}` ink, `{colors.pass-wash}`, `{colors.pass-border}`) — used *only* for a result a test established in a named environment: a clause whose test ran and passed, the kit's first successful verification. It never means "compliant", "certified" or "done well"; those claims do not exist in this product's vocabulary.
- **Refuse** (`{colors.refuse-red}` ink, `{colors.refuse-wash}`, `{colors.refuse-border}`) — a refusal and nothing else: a clause whose test failed, an unattested ZK request refused, an unregistered relying party refused, an unverified requester refused, a predicate held false.
- **Caution** (`{colors.caution-ink}` ink, `{colors.caution-wash}`, `{colors.caution-border}`) — a limit rather than a verdict: a clause recorded `ERROR`, a retryable failure, an unmeasured or `PENDING-DEVICE` value, an out-of-scope statement.
- **Neutral negative** (`{colors.slate}` or `{colors.slate-light}` ink on `{colors.paper-grey}` or `{colors.fog}`, `{colors.hairline}` border) — negative outcomes that are not judgements: `NOT_EXECUTED`, a case the operator cancelled, an out-of-scope clause with its reason.

The distinction that matters most: **a refusal never borrows the retryable-error look, and a cancellation never borrows the refusal's.** That is a copy of the root's own discipline into a second surface family, and it is why the report and the kit need the neutral negative family at all — without it, "we did not do this" and "this failed" would share one colour.

**The wallet's colours.** The app keeps its existing scheme. The one bridge to this family is the status mapping recorded under Components; no root hue is transplanted into the app's neutrals, background, or brand roles, and Material 3's default `secondaryContainer`/`tertiary*` slots are the only ones this release claims (see the mapping table).

Rules carried from the root, unchanged: **the Tinted Status Rule** — status never colours a whole region, it is a wash plus a border plus ink; **the One Acting Blue Rule** — only `{colors.state-blue}` is clickable at rest. Colour is never the only signal: every state on every new surface also carries a word.

Never: a green anything standing for "compliant"; a red full-page or full-row fill; a state colour used decoratively; a colour used to mean two different states on two surfaces.

## Typography

**Wallet.** Compose Material 3's default scale is the specification. The app defines no custom typography, and this spine adds none: the requester name is `Headline Medium`, consent copy and cause lines `Body Medium`, footer and caption text `Body Small`, the PIN surface's title `Title Medium`. Weight emphasis today is set ad hoc (semibold/bold) rather than by token; that is a technical detail for `bmad-architecture`, not a visual decision this document takes.

**Report and kit.** The root type ramp, whole: `{typography.display}` for the report's opening statement, `{typography.headline}` for section headings, `{typography.title}` for clause groups and case names, `{typography.body}` for prose, `{typography.label}` for field captions, `{typography.data}` for clause identifiers, commands, hashes and log lines, `{typography.figure}` for measured figures, `{typography.caption}` for the state labels (set the way the root sets its tags and badges).

Two rules follow from the root and are binding here:

- **The Mono-Is-Data Rule.** Monospace marks machine values and nothing else. A clause id, a test id, a command, a host class, a commit hash, a log line — yes. A state label, a heading, a clause description — never.
- **Figures are carried, not decorated.** A measured figure is set in `{typography.figure}` with its baseline, method and confidence adjacent; a row with no measurement says so in words (`PENDING-DEVICE` or `unmeasured`, with the reason) rather than showing a plausible number. There is no typographic treatment that makes an unmeasured figure look stronger than it is.

## Layout & Spacing

**Wallet.** The Android 4dp grid, the app's existing 16dp horizontal / 24dp vertical content padding, 48dp minimum targets. The consent gate is a single column: requester state → what is being shared and with whom → per-attribute list → footer note(s) → actions. Nothing new is stacked onto it.

**Report.** One column on the root container and the root scale. Top to bottom: the report shell's title block and independence notice → the **front summary strip** (clause counts by state, and the run's verdicts, before any clause) → the clause list in the AV-Profile's own order, one row per clause, **in-scope and out-of-scope clauses adjacent in their natural positions** → the per-clause detail when opened (clause text reference, the test id that covers it or the out-of-scope reason, the run's evidence) → the reproducibility block (commit, profile revision, host class, the commands that produced the run, pinned inputs).

The adjacency rule is a layout rule, not a courtesy: a reader must never have to leave the list, reach a footnote, or open a secondary document to discover that a clause is out of scope for v1 (PRD FR-16). The front summary exists so that the reader learns the shape of the result before the detail, and it reports exactly the counts the run produced — including failures.

The root's breakpoints apply (compact 1000px, phone 640px — `.impeccable/design.json:32-33`); the reader's laptop is the primary device and nothing in the report requires a wide viewport.

**Kit.** The install path is one short block of copyable commands; the four acceptance cases follow as four equal cards in the order a first-time integrator meets them (PRD wallet, AV-attestation wallet, unattested ZK request, unregistered relying party). Each card is self-contained — preconditions, expected verdict, refusal cause, log line — so that failing case 3 never sends the reader back to cases 1 and 2. Layout follows the root's scale and container.

**Sandbox.** The root layout is unchanged. The new typed state blocks render inside the step card of the rail step where the run stopped; the degraded-boot page is its own full page with no rail and no proof timeline. Neither adds a band, a stripe or a new grid.

## Elevation & Depth

Flat, with the root's **One Lift Rule** intact: only the sandbox's active step is elevated, and nothing else casts a shadow at rest. The report and the kit use hairlines and tonal layering (`{colors.paper}` over `{colors.paper-grey}` over `{colors.fog}`) and no shadow at all — a document that lifts its own clauses is decorating, not informing. The wallet uses Material 3's own elevation for the consent sheet and dialogs, and for nothing else; no elevation marks a state or a severity.

## Shapes

**Wallet.** Material 3 shape roles, unmodified: `Shapes.medium` for the consent card, `Shapes.extraLarge` for sheets and dialogs, the platform's full-round treatment for the QR card and pill chips. Status is a wash plus a 1px border — never a shape, never a stripe.

**Report and kit.** The root scale: `{rounded.default}` for clause rows, case cards, notices and the summary strip; `{rounded.hairline}` for code wells; `{rounded.pill}` for the kit's language toggle, set the way the root sets its language switch. Circles stay reserved for state marks only.

## Components

### The status bridge — the wallet's only family inheritance

The wallet is platform-conventional; the one exception is that its semantic states wear this project's status hues, so that "refused" looks the same on the phone as it does on the page. Recorded as a mapping, never as a transplanted palette.

| Wallet (Material 3) role | Source in the root family | Used for |
|---|---|---|
| `error` | `{colors.refuse-red}` | Refusal ink, the error icon, the unverified-requester state |
| `onError` | `{colors.paper}` | Ink on a refuse-red fill |
| `errorContainer` | `{colors.refuse-wash}` | The refusal card's wash |
| `onErrorContainer` | `{colors.refuse-red}` | Ink on that wash (root Tinted Status Rule: dark status ink on a light wash) |
| `secondaryContainer` | `{colors.refuse-wash}` | Today unmapped in `ui/theme/Theme.kt:38-46`; claimed for the refusal card |
| `onSecondaryContainer` | `{colors.refuse-red}` | Ink on that wash |
| `tertiary` | `{colors.pass-green}` | The verified state of the two-state requester component |
| `tertiaryContainer` | `{colors.pass-wash}` | The verified state's wash |
| `onTertiaryContainer` | `{colors.pass-green}` | Ink on that wash |

Everything else stays as the wallet maps it today (`ui/theme/Theme.kt:17-48`); this document re-specifies none of the app's existing neutrals.

One gap is recorded rather than papered over: **Material 3 has no caution role**, and the wallet has no mapped "warning" slot — its raw `yellow_600` is not this family and must not stand in for it. The caution family is therefore applied at component level (the limit chip, the `ERROR`/retryable states, the unmeasured row) as a direct reference to `{colors.caution-ink}`, `{colors.caution-wash}` and `{colors.caution-border}`, with a 1px border in the status hue — the same triad, one slot short. Naming that missing role is a job for `bmad-architecture`, not a new token for this spine to mint.

### Wallet components

- **Requester identification (two-state).** Replaces the unconditional badge rendered today on five requester surfaces (`components/VerifiedParty.kt:18-31`; `PresentationRequestScreen.kt:335`, `ProximityScreen.kt:132` and `:156`, `DigitalCredentialsScreen.kt:156`, `CredentialOfferScreen.kt:129`, `PidAuthenticationScreen.kt:51`). The name is `Headline Medium`; the state word carries the meaning, the icon follows it. **Verified** — the existing badge icon plus the existing string `presentation_verified_party` ("Verified party" / "Autenditud osapool"), in the `tertiary` family. **Unverified** — no verified badge; the state word in `error` ink on the `errorContainer` wash with a 1px `{colors.refuse-border}` border, the validation failure stated in `Body Medium`, and a disclosure that reveals the chain's failure point on demand (no trust anchor configured, unparseable chain, name mismatch). Only Refuse and Cancel are offered. The refusal treatment is a card, not a region: no red page, no stripe.

- **Verification-cause disclosure.** A disclosure control on the consent card's requester block, closed by default, opening in place to the chain's failure point (no trust anchor configured / unparseable chain / name mismatch) set in `{typography.data}`-style machine terms. It is not certificate internals by default, it never introduces a control that is not Refuse or Cancel, and it carries no status colour of its own.
- **Consent card (one component).** The remote and proximity consent screens render one component, not two hand-rolled copies (`ProximityScreen.kt:203-268` vs `PresentationRequestScreen.kt:398-489`). `Shapes.medium`, `surfaceContainerLow`-style platform surface, 16dp/24dp content padding, one action row at the foot. Same content set on both paths: requester state, what is shared and with whom, the per-attribute list, the channel notice, the tier and last-attestation notices, the footer note.
- **Refusal block.** A card in the `error` family: state icon, state word, the named cause, and the actions that remain (there is no proceed action on an unverified requester). Distinguishable at a glance from both the cancellation and the retryable-error treatments, and logged with its reason and no attribute value.

- **Predicate and policy refusals.** The existing pre-consent refusals, unchanged in shape and now using the same refusal treatment as the gates: more than one age predicate, a request the device cannot satisfy, a weaker or unsigned request on a ZK-capable device, a threshold the wallet does not hold. Each is a refusal card with its cause, never an empty list and never a greyed-out switch with no explanation.
- **Presence prompt.** Platform-owned, no app chrome: the system user-authentication sheet, with reason copy that names what is about to happen ("Unlock your document" / "Authenticate to share this document" are the strings today, English-hardcoded at `ui/prompt/PromptDialogHost.kt:156-159`), required in both complete locales. It is bound to the key operation; visually it is the platform's, and the wallet must not restyle or imitate it.

- **Consent gate (PIN).** The app-level gate that already exists on the remote, proximity and issuance paths, kept and re-wired. Visually it stays the platform-conventional numpad surface (`Title Medium` for the state line, the party line beneath it); the gate's *state* treatments — remaining attempts, lockout — are the two components below. It is not a substitute for the presence prompt and is never presented as one.
- **Attempt counter.** One line in `Body Medium`, error ink on the consent surface's own background, using the existing copy shape `pin_incorrect_pin_attempts_left` ("Incorrect PIN. Attempts left: %1$s"). Plain text, no badge, no dots, no animation.
- **Lockout surface.** A full-surface state in the error family: state word, the lockout duration in body text, the escalation path, and the single action available. Written out in words rather than a countdown animation; no colour-only signalling; the counter is gone from this state because there is nothing left to count.
- **Typed state block (in-place, step-anchored).** Not a new component: the root's `step-card` with the root's status triad. State icon, state word, named cause, in the step where the run stopped. No rail change, no timeline movement, no new band.

- **Cheat band.** The root's navy full-bleed band, unchanged: three attack cards, each with its outcome box. Blocked outcomes use the pass family, a got-through outcome uses the refuse family with the wording that names it as a bug, and an unexpected answer is set as plain text. No new band, no new colour.
- **Ledger and figures strip.** The root's two-column ledger and its figures strip, unchanged: "the shop learned" on `{colors.paper}`, "stayed with you" on `{colors.paper-grey}`, hairline rows, figures in `{typography.figure}` with the caption that says they were measured in this run.
- **Degraded-boot page.** The root identity, one page: utility bar with the independence notice, masthead, and one `step-card`-shaped block naming which binary is missing and the documented command (`make deps`). No rail, no proof timeline, no figures strip — a page that shows a timeline it did not run would break the root's first Do.

### Conformance report components

- **Report shell** — `{colors.paper}`, `{colors.ink}`, `{rounded.default}`, full-page single column; the title block names the artefact, the AV-Profile revision it was built against, the commit and the host class.
- **Front summary** — `report-summary-strip`: clause counts by state, the run's verdicts, and the run's date. Counts are the run's, not a target.
- **Clause row** — `report-clause-row`: clause identifier in `{typography.data}`, clause label, state label, and the test id or the out-of-scope reason inline. In-scope and out-of-scope rows share one row geometry so that adjacency reads as one list. Rows are keyboard-focusable and expand to detail.
- **State label** — a small tag (`{typography.caption}`) carrying a word, never a bare dot: `PASS`, `FAIL`, `ERROR`, `NOT_EXECUTED`, `OUT OF SCOPE FOR V1` (+ reason). The colours are `report-state-*`; the word is what a screen reader and a monochrome print both get.

- **Fail-closed reporting.** The report's own failure state: a clause whose evidence is missing, unreachable or ambiguous is reported as not evidenced, with the failed lookup named, and never as satisfied. It is rendered in the caution or neutral-negative treatment, never as a pass.
- **Reproducibility block** — commit, profile revision, host class, the commands that produced the run, and the pinned-input note (Python inputs pinned; the Rust toolchain is not, so it is named). Set in `{typography.data}` inside a `kit-install-command`-style well.
- **Independence notice** — `report-independence-notice`: the blue-mist wash with the `{colors.info-border}` hairline, text only.

### Acceptance-kit components

- **Install path** — one documented path, commands copyable, `{rounded.hairline}` wells on `{colors.fog}`; no second path offered anywhere.
- **Case card** — `kit-case-card`: case name, its own preconditions, expected verdict, expected refusal cause, the log line to expect. Four cards, equal weight, in first-meeting order.
- **Outcome states** — `kit-outcome-refused` / `kit-outcome-cancelled` / `kit-outcome-retryable`, each a wash plus a 1px border plus ink, each carrying its word and its named cause. The refusal state names what refused; the cancellation state names that the operator cancelled; the retryable state names what failed and why it can be retried.

- **Register lookup.** The kit's register path, mocked and pluggable: an absent, unparseable or ambiguous entry refuses in the refusal treatment and names the register as the failed lookup; a legitimate stale decision is stated and logged rather than rendered as a success.
- **Language toggle** — `kit-language-toggle`: Estonian first, English second, active language marked by fill and weight as the root marks it, present on every kit surface.
- **Honesty block** — the kit's own statement that SM-3 and SM-4 are unachieved at v1 (no service owner engaged, no integrator count), and that any result produced by the maintainers is labelled self-produced. Set as body text; it is not a footnote and not a badge.

## Do's and Don'ts

### Carried from the root, binding on all three surfaces

Repo-root `DESIGN.md`'s own Do's and Don'ts (`DESIGN.md:290-303`) hold here without amendment — they are restated by reference, not paraphrased:

- **Do** keep every claim on screen tied to this run: sizes, timings and verdicts come from the server's or the runner's output, never from copy.
- **Do** name the shortcuts where they occur, in Slate narration, not in a footnote — on the new surfaces: a clause that was not exercised, a figure that was not measured, a case whose prerequisite is missing.
- **Do** carry the independence notice, in the placements the root names for the sandbox (utility bar, info alert, footer) and in the report's and the kit's own shells.
- **Do** ship Estonian and English for every string, Estonian first — on the sandbox and in the kit. The report's language record is English-only and is stated as such (EXPERIENCE.md → *Language Coverage per Surface*).
- **Don't** use RIA's logo, the state coat of arms, or "RIA" as a brand; nothing on any surface claims endorsement.
- **Don't** put an uppercase tracked eyebrow label above a heading.
- **Don't** add a thick coloured stripe on one side of a card; status is a wash plus a 1px border.
- **Don't** render cryptography as spectacle.
- **Don't** use monospace for anything that isn't a machine value.
- The four named rules — **One Acting Blue**, **Tinted Status**, **Mono-Is-Data**, **One Lift** — hold on the new surfaces exactly as the root states them.

### New rules for these surfaces

| Do | Don't |
|---|---|
| Inherit the root token family by reference; resolve `{colors.*}`, `{rounded.*}`, `{typography.*}`, `{spacing.*}` against root `DESIGN.md` | Restate, rename, redefine or "improve" a root token — in this file, in the report's stylesheet, or in Compose |
| Let the wallet be Material 3, with the status bridge as its only family inheritance | Transplant the root's neutrals, type ramp or shapes into the app |
| Publish in-scope and out-of-scope clauses adjacent, in profile order, each out-of-scope row naming its reason | Push out-of-scope clauses into a footnote, an appendix, or silence |
| Render every figure from the run that just happened, with its baseline, method and confidence | Show a stored, estimated, rounded or plausible-looking number |
| Write `PENDING-DEVICE` / unmeasured for a row with no measurement, and say why | Fill an unmeasured row with a placeholder figure |
| Distinguish refusal, cancellation and retryable error by icon, word and cause on every surface | Reuse the refusal treatment for a cancellation, or the retryable treatment for a refusal |
| Show a requester's verification state as two states, and refuse when it did not validate | Render a name as verified because a certificate supplied one, or offer a proceed-anyway path |
| Keep status to a wash, a 1px status border and status ink, with a word alongside | Colour a whole region, a whole row or a whole page with a status hue; signal with colour alone |
| State the kit's unachieved metrics (SM-3, SM-4) inside the kit, and label self-produced results as self-produced | Let a reader infer that a maintainer-run case is external acceptance evidence |
| Say which carrier is mandated (the Digital Credentials API path) and which is profiled but unofficial (OpenID4VP `mso_mdoc_zk`) wherever either is named | Present the ZK path as mandated over OpenID4VP, or fuse the transport and the format into one claim |
| Use the report's own vocabulary: what a test established, in a named environment | Any compliance, conformity, certification or assessment vocabulary — "conformant", "certified", "compliant", "meets the profile" — or an equivalent smuggled in as a green tick |
