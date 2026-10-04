---
name: ZK age-proof sandbox
description: A guided, real zero-knowledge age check, set in the Estonian state e-service idiom (TEDI / ria.ee).
colors:
  state-blue: "#005aa3"
  state-blue-deep: "#004277"
  state-navy: "#003662"
  blue-mist: "#d0e1ee"
  blue-haze: "#e7f0f6"
  blue-steel: "#99bdda"
  ink: "#151926"
  slate: "#4b4e62"
  slate-light: "#5d6071"
  paper: "#ffffff"
  paper-grey: "#f9f9f9"
  fog: "#f0f0f2"
  hairline: "#e1e2e5"
  pewter: "#9293a4"
  input-stroke: "#838494"
  pass-green: "#266b42"
  pass-wash: "#eaf3ee"
  pass-border: "#599e75"
  refuse-red: "#ac3232"
  refuse-wash: "#fbecec"
  refuse-border: "#df6565"
  caution-ink: "#664807"
  caution-wash: "#fff0cf"
  caution-border: "#ba830d"
  info-border: "#337bb5"
  cellar-plaster: "#f6efe9"
  footer-text: "#d2d3d8"
typography:
  display:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "clamp(2rem, 1.2rem + 3vw, 3.25rem)"
    fontWeight: 300
    lineHeight: 1.12
    letterSpacing: "-0.015em"
  headline:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1.75rem"
    fontWeight: 400
    lineHeight: 1.25
  title:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1.25rem"
    fontWeight: 700
    lineHeight: 1.35
  body:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  lead:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1.1875rem"
    fontWeight: 400
    lineHeight: 1.55
  label:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.4
  data:
    fontFamily: "ui-monospace, SFMono-Regular, Menlo, Consolas, monospace"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
  figure:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 300
    lineHeight: 1.2
  figure-compact:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "2.5rem"
    fontWeight: 300
    lineHeight: 1
  clock:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "3.5rem"
    fontWeight: 300
    lineHeight: 1
  title-small:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1.125rem"
    fontWeight: 700
    lineHeight: 1.35
  lead-compact:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1.0625rem"
    fontWeight: 400
    lineHeight: 1.55
  body-small:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.5
  small:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 400
    lineHeight: 1.4
  caption:
    fontFamily: "Roboto, 'Segoe UI', 'Helvetica Neue', Arial, sans-serif"
    fontSize: "0.75rem"
    fontWeight: 700
    lineHeight: 1.3
  icon:
    fontFamily: "'Material Symbols Outlined'"
    fontSize: "1.25em"
    fontWeight: 400
    lineHeight: 1
    fontVariation: "'FILL' 0, 'wght' 400, 'opsz' 24"
rounded:
  hairline: "2px"
  default: "4px"
  sheet: "8px"
  pill: "360px"
spacing:
  s1: "4px"
  s2: "8px"
  s3: "12px"
  s4: "16px"
  s5: "24px"
  s6: "32px"
  s7: "48px"
  s8: "72px"
components:
  button-primary:
    backgroundColor: "{colors.state-blue}"
    textColor: "{colors.paper}"
    rounded: "{rounded.pill}"
    padding: "8px 24px"
    height: "40px"
  button-primary-hover:
    backgroundColor: "{colors.state-blue-deep}"
  button-primary-active:
    backgroundColor: "{colors.state-navy}"
  button-secondary:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.state-blue}"
    rounded: "{rounded.pill}"
    padding: "8px 24px"
    height: "40px"
  button-secondary-hover:
    backgroundColor: "{colors.blue-haze}"
  input:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.default}"
    padding: "8px 12px"
    height: "40px"
  chip:
    backgroundColor: "{colors.paper}"
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    padding: "4px 16px 4px 12px"
    height: "36px"
  step-card:
    backgroundColor: "{colors.paper}"
    rounded: "{rounded.default}"
    padding: "24px 32px"
  credential-head:
    backgroundColor: "{colors.state-navy}"
    textColor: "{colors.paper}"
    rounded: "{rounded.sheet}"
    padding: "16px 24px"
  utility-bar:
    backgroundColor: "{colors.state-navy}"
    textColor: "{colors.paper}"
    padding: "8px 0"
---

# Design System: ZK age-proof sandbox

## Overview

**Creative North Star: "The Service Counter"**

The page behaves like an Estonian state e-service: one citizen, one transaction, one step at a time, at a counter that tells you exactly what it is doing with your data. It is built on TEDI (TEHIK's design system), and its chrome is inferred from ria.ee: a navy utility bar, a white masthead, a calm white ground, and the state blue as the only accent that acts. Nothing glows and nothing pretends to be a terminal. The cryptography is shown as a public-service procedure the visitor can follow, not as spectacle.

Density is moderate and forms are generous. The guide voice sits in grey secondary text; the actions sit in blue pills. The one moment the page raises its voice is the live proof: a vertical timeline that fills as the real prover reports, beside a large light-weight clock. The bilingual toggle (ET first, EN second) is part of the identity, not a utility.

**Key Characteristics:**
- White ground, navy chrome, one acting blue.
- Roboto, with light-weight (300) display headings as TEDI sets them.
- Pill buttons, 4px cards, 8px "sheets" for anything that represents another party's screen.
- Status is TEDI's alert triad (green / red / amber), each one a wash plus a border plus dark ink.
- Every number shown comes from the run that just happened.

## Colors

A restrained public-sector palette: neutrals carry almost everything, and the state blue marks what you can act on or where you are.

### Primary
- **State Blue** (state-blue): primary buttons, links, the active step's border, the rail's done dots, and the proof timeline's fill. It is the colour of "you can act here" and "this happened".
- **Deep State Blue** (state-blue-deep): hover for blue controls; icon ink inside tinted circles.
- **State Navy** (state-navy): the ria.ee utility bar, the credential's header band, the "try to cheat" band, and pressed buttons. Navy is chrome and authority; it is never a button at rest.

### Secondary
- **Blue Mist** (blue-mist): the info alert's wash, the active step dot's halo, and icon backdrops in the cast list.
- **Blue Haze** (blue-haze): hover wash for secondary controls and chips; the "who's involved" panel; the row of the claim being asked for.
- **Blue Steel** (blue-steel): the credential's badge icon and quiet text on navy.

### Neutral
- **Ink** (ink): body text and headings. It is also the footer ground.
- **Footer Text** (footer-text): secondary text on the Ink footer.
- **Slate** (slate) and **Light Slate** (slate-light): guide narration, hints, and secondary labels.
- **Paper** (paper), **Grey Paper** (paper-grey) and **Fog** (fog): page ground, locked steps and the "kept" ledger column, and code and log wells.
- **Hairline** (hairline): every divider and card border.
- **Pewter** (pewter) and **Input Stroke** (input-stroke): secondary-button borders and input strokes (≥3:1 against white).
- **Cellar Plaster** (cellar-plaster): the one warm surface, used only behind the test shop's product, so the shop reads as somebody else's site.

### Status
- **Pass** (pass-green ink, pass-wash, pass-border): verified, blocked attack, credential signed.
- **Refuse** (refuse-red ink, refuse-wash, refuse-border): verification refused, invalid input, "not in the credential" marks.
- **Caution** (caution-ink, caution-wash, caution-border): the "test shop" badge and the age gate.

**The One Acting Blue Rule.** Only State Blue is clickable at rest. Navy, mist and haze are surfaces, never controls.

**The Tinted Status Rule.** Status never colours a whole region. It is always a light wash, a 1px border in the status hue, and text in the dark status ink, as TEDI's alerts are.

## Typography

**Display Font:** Roboto 300 (with Segoe UI, Helvetica Neue, Arial)
**Body Font:** Roboto 400/700
**Data Font:** the system monospace stack, for identifiers, hashes, nonces, doctypes and the technical log only

**Character:** One workhorse sans throughout, as on every Estonian state site. Hierarchy comes from weight contrast: light display against bold titles.

### Hierarchy
- **Display** (300, clamp 2–3.25rem, 1.12): the one page H1. Big numbers (the proof clock, the figures) use the same light weight.
- **Headline** (400, 1.75rem, 1.25): section headings; step headings at 1.5rem while a step is active.
- **Title** (700, 1.25rem, 1.35): card and attack titles, and the credential name.
- **Lead** (400, 1.1875rem, 1.55, Slate): the opening paragraph of a section.
- **Body** (400, 1rem, 1.5): narration, capped at 68ch.
- **Label** (400, 0.875rem): field labels, hints, and the "Requested by" and "Asks" captions.
- **Steps between** (TEDI's own intermediate sizes): title-small 1.125rem for compact titles, body-small 0.9375rem for dense rows and ledger items, small 0.8125rem for the utility bar, captions under ledger items and figure labels, caption 0.75rem bold for tags and badges.
- **Figures** (300, 1.5rem; 2.5–3.5rem for the proof clock): measured numbers, always tabular.
- **Icons:** Material Symbols Outlined, as TEDI ships it, self-hosted. Filled only for status marks (check, verified, refused).

**The Mono-Is-Data Rule.** Monospace marks machine values (`ee.riik.poa.1`, `age_over_18`, hashes, the log) and nothing else. It is never decoration.

## Layout

The container is 1240px with 24px gutters (16px under 640px). The intro is a 7/5 split: thesis and action on the left, the cast ledger on the right. The journey is a 240px sticky step rail beside a single column of step cards. Under 1000px the rail becomes a sticky "Step n of 6" line with a six-segment progress bar, and every split collapses to one column. Spacing follows one scale (4, 8, 12, 16, 24, 32, 48, 72), with more space above a heading than below it. Full-bleed bands (the navy cheat band, the grey limits band, the ink footer) pace the long scroll after the journey.

## Elevation & Depth

Flat by default, as TEDI is. Depth comes from tonal layering (white over grey paper over fog) and 1px hairlines. There is exactly one shadow: a soft two-layer lift on the active step card, so the step you are on sits slightly above the rest.

**The One Lift Rule.** Only the active step is elevated. Nothing else casts a shadow at rest.

## Shapes

Small, square-shouldered corners: 4px on cards, inputs, alerts and tags; 8px on "sheets" that stand for another party's surface (the credential, the shop window, the wallet's consent sheet); 2px on code wells. Buttons and chips are full pills (360px), as TEDI's main buttons are. Circles are for step dots, phase marks and icon backdrops only. Borders are 1px; the single 4px border is the bottom indicator under the active language.

## Components

### Buttons
- **Shape:** full pill (360px), 40px tall (48px for the intro's Start).
- **Primary:** State Blue with white text. Hover goes to Deep State Blue, and pressed to Navy.
- **Secondary:** white with blue text and a Pewter border. On hover the wash turns to Blue Haze and the border to State Blue.
- **Busy / disabled:** a Fog fill with Light Slate text, plus a `progress` or `not-allowed` cursor. A busy button keeps its label.

### Chips
- **Style:** white pills with a Pewter border, a blue leading icon and ink text. They fill the form with a test persona and never toggle state.

### Cards / Containers
- **Step card:** white, 1px hairline, 4px corners. Active: a State Blue border and the one lift. Locked: Grey Paper with Light Slate headings. Done: collapsed to its heading plus a green check-circle summary line.
- **Sheets:** 8px corners, for the credential, the shop window and the consent sheet.

### Inputs / Fields
- **Style:** white, an Input Stroke 1px border, 4px corners, 40px tall, and a State Blue caret.
- **Hover / Focus:** a blue border on hover; on focus a Slate border plus a 2px Blue Mist ring.
- **Error:** a Refuse wash and border, with the message below in refuse ink and an icon.

### Navigation
- **Utility bar:** navy, 13px, with the independence notice on the left and ET/EN on the right. The active language carries a 2px white underline.
- **Masthead:** white, 80px, with the wordmark (blue tile plus a two-line name) and text links that take a blue underline on hover.
- **Step rail:** numbered 32px dots on a 2px hairline spine. Done dots are filled blue; the active dot has a blue ring and halo.

### Proof timeline (signature)
A vertical list of five phases, each with a 24px mark on a hairline spine. Pending marks are empty. The active mark is a spinning blue arc (a static Blue Mist fill under reduced motion), and done marks are filled blue with a white check. A failed mark is filled refuse red with a white cross. The spine fills downward (scaleY) as each phase completes. A 3.5rem light clock counts up beside it, and the raw log sits behind a disclosure.

### Ledger (signature)
Two columns, "the shop learned" on white and "stayed with you" on Grey Paper, with hairline rows and a small caption under each item. Under it, a figures strip in light 1.5rem numerals with hairline separators.

## Do's and Don'ts

### Do:
- **Do** keep every claim on screen tied to this run: sizes, timings and verdicts come from the server's output, never from copy.
- **Do** name the sandbox's shortcuts where they occur, in Slate narration, not in a footnote.
- **Do** carry the independence notice in the utility bar, the info alert and the footer.
- **Do** ship Estonian and English for every string, Estonian first.

### Don't:
- **Don't** use RIA's logo, the state coat of arms, or "RIA" as a brand. This is not RIA's product.
- **Don't** put an uppercase tracked label (an eyebrow) above a heading.
- **Don't** add a thick coloured stripe on one side of a card. Status is a wash plus a 1px border.
- **Don't** render cryptography as spectacle: no dark terminal grounds, no glowing nodes, no matrix rain.
- **Don't** use monospace for anything that isn't a machine value.
