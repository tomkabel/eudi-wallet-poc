# Product

<!-- impeccable:product-schema 1 -->

## Platform

web (the sandbox site); the wallet itself is the Android app in `app/`.

## Stack

Sandbox site: plain static HTML/CSS/JS served by a Python standard-library server
(`sandbox/server.py`) that drives the real issuer, prover and verifier. Chosen by the
assistant (the user picked "real ZK end to end"); no framework, no build step.

## Users

Reviewers, relying parties, and curious citizens who want to see a zero-knowledge
proof of age work end to end, without an Android phone and without reading the spec.
They arrive cold and leave understanding what the shop learned and what it did not.

## Product Purpose

An independent fork of the EE EUDI Wallet PoC that adds a zero-knowledge proof of age
(`age_over_18`, Longfellow over ISO 18013-5 mdoc, OpenID4VP). The sandbox site lets a
visitor register a test identity, receive a proof-of-age credential, and prove they
are over 18 to a test shop, with every step executed for real.

## Positioning

The proof is real: a 360 KB Longfellow proof generated on the server in about six
seconds and verified by the Go verifier in about two and a half seconds. The shop
learns one boolean and nothing else: no birth date, no name.

## Capabilities and Constraints

- Real flow: `issuer/mint_ee_poa.py` → `wallet/present.py` + `ee_poa_demo` prover →
  `verifier/zkverify` (`direct_post.jwt`, encrypted).
- The sandbox "Population Register" computes age predicates from a typed birth date.
  That is the trust-the-client enrolment weakness EE-POA-006/007 warns about, and the
  site says so.
- Needs the built prover and verifier on the host (`make deps`).
- Bilingual: Estonian and English, with a toggle.

## Brand Commitments

- Visual world pinned by the user: copy or infer from ria.ee, the TEDI design system
  (TEHIK). Roboto, TEDI colour tokens, Material Symbols icons.
- Not RIA's product: no state coat of arms and no RIA logo. Every page carries the
  independence notice (see README).

## Evidence on Hand

Measured figures in `README.md` and `verifier/README.md` (prove 6.1–6.5 s, verify
~2.5 s, proof ~360 KB). Test personas are synthetic and labelled as such.

## Product Principles

1. Show, don't claim: every number on the page comes from the run that just happened.
2. Say what the shop did not learn, as plainly as what it did.
3. Name the sandbox's shortcuts where they happen, not in a footnote.

## Accessibility & Inclusion

Estonian public-sector baseline: WCAG 2.1 AA. Keyboard-complete, screen-reader
announcements for long-running steps, and reduced-motion support.
