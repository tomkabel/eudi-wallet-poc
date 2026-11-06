---
title: 'Recon: verify the PID, then issue a derived attestation'
type: 'technical + domain + competitive'
topic: 'PID-authenticated issuance of a derived (mandate/representation) attestation: feasibility, Estonian and EU anchors, who ships it'
decision: 'correct-course (add an Epic 10 story) vs park vs spec'
source: 'native run (bmad-deep-recon, standard preset, 3 parallel researchers + lead spot-checks); input forge-brief.md'
status: complete
preset: 'standard'
validation: 'normal'
created: '2026-11-06'
updated: '2026-11-06'
claims: {verified: 14, unverified: 9, overturned: 1}
---

# Recon: verify the PID, then issue a derived attestation

**Decision this research serves:** whether to act on the forge brief's recommendation (`forge-brief.md`). The options are `bmad-correct-course` to add one Epic 10 story, park, or `bmad-spec` for a separate product.

Run autonomously on 2026-11-06. Every choice went through `tiltrank` (decision log at the end). The working files are in `recon/`: digests in `recon/digests/`, the claims ledger in `recon/.memlog.md`. Statuses: **verified** means checked against a primary text this run, or against two independent sources. `[UNVERIFIED]` means a single source or an absence finding.

## Executive summary

**Correct-course, but reshaped. The forge brief's verdict holds; its story shape does not.**

1. **The flow is feasible without changing the pinned library, and the Commission already ships the issuer half.** OpenID4VCI 1.0 Final leaves the issuer's authentication method open. It describes "presentation of additional credentials during issuance" only as a non-normative use case [1]. The library mechanism for it, the Interactive Authorization Endpoint, was pulled from 1.0 and sits in an unfinished 1.1 draft with an open, formally verified attack [2][3][4]. The Commission's reference issuer already runs the 1.0-compatible variant, an OpenID4VP presentation of the PID nested in the authorization step. It does this same-device and cross-device, and per credential it configures which PID attributes it asks for [9][10][11][13]. It also ships a **Power of Representation** attestation, `eu.europa.ec.eudi.por.1`, with no holder name or birth date [12]. So the "demo RP-as-issuer" the forge brief proposed to build already exists as a configuration of the Story 10.1 target. The remaining work is on the wallet side.
2. **The Pääsuke-shaped public-sector mandate attestation (PuB-EAA) has no anchor, which contradicts the forge brief.**
   - No Estonian plan to issue mandates as wallet attestations was found. Pääsuke is an online authorisation oracle for state e-services, not an issuer [23][24][25] `[UNVERIFIED: absence]`.
   - The European Business Wallets proposal, COM(2025) 838 of 19 Nov 2025 (date verified), makes a mandate something the **company grants inside its business wallet**. A business wallet may issue attestations into a person's wallet. Business registers attest only company identity and the EUID [15][16].
   - ARF 3.0.0 dropped wallets for legal persons, and its only representation rules cover one person representing another [19][20][21][22].
   - The mock should therefore follow the reference `por.1` / EWC "Signatory Rights" shape [12][36], not a PuB-EAA label.
3. **The post's commercial thesis fails on primary evidence.** Dock's own docs say its paid-verification issuer "sees that a credential … was shared with a specific Verifier" [32]. No documented scheme keeps the issuer blind to the verifier [32][40] `[UNVERIFIED: absence]`. No vendor documents PID-gated issuance as a product [30][31][34][35] `[UNVERIFIED: absence]`. No Estonian or Nordic relying party was found paying for a business-issued attestation `[UNVERIFIED: absence]`. The pilots found "who pays" unanswered [38].

**Biggest caveat.** Nothing cryptographically binds the presented PID to the key of the attestation that is issued. OIDF issue #742 shows the cross-session attack for the 1.1 draft [4]. That the 1.0 nested variant shares the gap is the researchers' inference. The story must record it as a known limit, not claim binding.

## Technical: can the wallet do presentation during issuance?

- **Specification.** OpenID4VCI 1.0 is Final (16 Sep 2025) [1].
  - It has no Interactive Authorization Endpoint. A full-text check this run found no occurrence of "interactive" [1].
  - Presenting during issuance appears only as Appendix H.5, "Wallet-Initiated Issuance during Presentation (Requires Presentation of Additional Credentials During Issuance)". How the issuer authenticates the user at the Authorization Endpoint is out of scope [1].
  - The endpoint was removed before Final over open security questions [2]. It lives in the OpenID4VCI 1.1 editor's draft on GitHub (last commit 2026-10-15), which is not final [3]. Its open issues include #742, a Tamarin-verified attack in which the attestation carries the victim's claims but is bound to the attacker's key [4]. Confidence: high.
- **Three variants, not interoperable.** OIDF issue #730 (open) lists them [5]:
  1. separate flows;
  2. **OpenID4VP nested in the authorization step**, the only variant that conforms to 1.0;
  3. the 1.1 endpoint or First-Party Apps.

  One implementer reports that variant 2 works with both EUDI reference wallets, provided the wallet keeps the issuance state and the authorization page and the presentation's return URL open in one browser session [5] `[UNVERIFIED: single report]`. Animo/Paradym does not support variant 2 because the browser hop loses the issuance session [5].
- **The pinned library.** `eudi-lib-jvm-openid4vci-kt` 0.6.1 (2025-01-14) targets OpenID4VCI **draft 14** [6]. Alignment with 1.0 arrived in v0.9.0 (2025-09-30) [7]; the latest is v0.16.0 (2026-11-03) [8]. No release note, issue or default-branch code mentions interactive authorization or presentation during issuance [6][7][8] `[UNVERIFIED: absence]`. For variant 2 that does not matter: the presentation happens in the browser during the authorization-code redirect, outside the library.
- **The reference issuer** is `eudi-srv-web-issuing-eudiw-py`, v0.11.0 (2026-11-04).
  - Its README lists "Dynamic Credential Request ✅" [9]. `app/routes/oid4vp.py` "Supports both same-device and cross-device flows" and detects same-device by `response_code` + `session_id` [10]. The lead re-read both.
  - Its user guide walks through PID authentication followed by the credential form [11].
  - Per credential, `auth_method_supported_credencials` selects `PID_login` (OpenID4VP), and `dynamic_issuing` names the exact PID attributes requested [13].
  - It ships `eu.europa.ec.eudi.por.1` (mdoc and SD-JWT VC): `legal_person_identifier`, `legal_name`, `full_powers`, `eService`, `effective_from_date`/`effective_until_date`. The values are user-entered and the issuer label is "Test QEAA Issuer" [12].
  - Whether the public deployment serves `por.1` behind `PID_login` is not public `[UNVERIFIED]`. Self-hosting the open-source issuer removes that dependency.

  The README's authentication sentence names only an eIDAS node, OAuth2 or a form. The feature table and the source show the OpenID4VP path, so the source wins (the contradiction is logged in the memlog).
- **The reference wallet** routes an OpenID4VP request that arrives mid-issuance back into issuance (`DeepLinkType.DYNAMIC_PRESENTATION`, `DeepLinkHelper.kt`). This sits in the app layer, not in wallet-core [14]. Confidence: medium-high; the trigger condition was not read.
- **This wallet (repository fact, not research evidence).**
  - `OpenId4VCIManager` opens the authorization URL with `uriHandler.openUri`, which uses the default browser. The presentation screen opens the presentation's `redirect_uri` the same way, so the browser session is shared.
  - `MainActivity` is `singleTask` and registers the `openid4vp`, `haip-vp` and `eudi-openid4vp` deep links. The authorization state persists in `AuthorizationStateDataSource`.
  - Gaps: `credentialConfigurationIds` is hard-coded to `[MDL]`, and the issuer URL is a single `BuildConfig` value. Nothing tests re-entry mid-issuance.

**Answer to Q1:** yes, through variant 2 with no library change. The issuer side exists in the reference issuer. The wallet needs a configurable credential configuration and a re-entry test. Two open points: the 1.1 endpoint is neither final nor in the library, and draft-14 0.6.1 against a 1.0 issuer is a Story 10.1 interop risk `[UNVERIFIED: inference]`.

## Domain: will mandates be a state attestation in Estonia?

- **European Business Wallets.**
  - The proposal is COM(2025) 838 final, "Brussels, 19.11.2025" [15][16] (verified; matches the forge brief).
  - Procedure 2025/0358(COD) [17][18]:
    - The Council adopted its general approach on 9 Jun 2026.
    - ITRE's report A10-0240/2026 went into trilogues with no first-reading position.
    - The Council says the authorisation system "is not intended to affect any power of attorney or legal mandates provided under national or Union law" [18].
  - A **mandate** is the owner's authorisation for an authorised representative to operate *that* business wallet (Art. 3(18)–(19)) [15].
  - Business wallets may "issue electronic attestations of attributes to … European Digital Identity Wallets" (Art. 5(1)(f)) [15].
  - Registers supply owner identification data (name and EUID, Arts. 8–9). They do not attest "person X may represent Y" [15].
  - The article analysis rests on one researcher's reading of the primary text `[UNVERIFIED: single read]`.
- **ARF.**
  - v3.0.0 (2026-07-23) is the latest release [19]. It "removed" wallet units for legal persons "in view of the development of a separate business wallet" (re-read by the lead) [20].
  - Representation requirements cover only natural-to-natural representation. Topic 29 RP_01/RP_02 require a distinct attestation type, a visible scope, and short life or revocability [21]. Discussion Topic I, still in refinement, excludes representing a legal person [22].
- **Estonia.**
  - Pääsuke manages access authorisations for agencies' e-service environments [23]. Connected systems query it online through an "oracle". It issues no portable credential [24].
  - RIA's wallet Phase III analysis (8 Apr 2026) never mentions Pääsuke, the Business Register or representation rights [25]. Neither does RIA's wallet page [45]. `[UNVERIFIED: absence]`
  - The spec §8 row "Powers and mandates (Pääsuke), PuB-EAA, RIA, P2" therefore has no public RIA source behind it.
- **Pilot precedent.**
  - EWC rulebook RB004 "Signatory Rights" is a role/mandate attestation issued by **business registers** to an organisational or a natural-person wallet. It carries the EUID and `signatory_rule`. The rulebook is marked draft, "modifications … are expected" [36].
  - EWC RFC005 (legal-person PID) needs LoA-high authentication plus a check of representation rights. LoA high was unavailable in the pilot [37].

**Answer to Q2:** not on any public evidence. EU law is moving mandates into business wallets: they are granted by the company and may be issued into a person's wallet, and registers attest the company. That is closer to the post's "business issues it" than the forge brief allowed, but it is a business-wallet attestation, not an attestation derived from verifying a PID. In ARF 3.0.0, Topic 29 RP_01/RP_02 covers only person-to-person representation; nothing covers a person representing a company.

## Competitive: who ships it, who pays

- **Shipping code.**
  - OWF Credo has a `presentation` authorization flow ("authorization is completed using an OpenID4VP presentation during issuance") [28].
  - OWF identity-common-ts has the 1.1 endpoint, marked experimental [29].
  - The reference issuer is covered above [9].
  - Vendor documentation (walt.id [30], Truvera [31], Signicat [34], Procivis [35], Paradym) shows no PID-gated issuance. IDnow, Lissi and Truvity searches returned homepages only. `[UNVERIFIED: absence]`
- **Pilots.** The LSP primaries were not read; a secondary synthesis [38] reports:
  - who pays for issuance and verification is "not satisfactorily answered";
  - representation, including business mandates, lacks a harmonised legal frame;
  - NOBID issued Italian fiscal codes from a Norwegian PID.

  Confidence: medium.
- **Paid demand (EE/Nordics).** None found for business-issued attestations `[UNVERIFIED: absence]`. eID pricing is precedent only. Norwegian BankID reseller list prices from 1 Jan 2026 are NOK 1.34 per high-level authentication and NOK 4.46 per signature. New customers have been on a fixed per-user price since 1 Oct 2026; existing customers move to it on 1 Jan 2027 [39].
- **An issuer blind to the verifier while paid per verification: not found.**
  - Dock/Truvera KVAC: "The Issuer sees that a credential based on a specific schema was shared with a specific Verifier, but learns nothing about the Holder" (re-read by the lead) [32]. It also requires a closed ecosystem [33].
  - cheqd hides the holder behind a flat-price encrypted status list, not the payer [40].

**Answer to Q3:** issuance gated on a PID presentation ships in open source (the reference issuer, Credo) but not as a documented vendor product. No paid verifier demand was found. Dock's "privacy-preserving monetisation" is holder-private but **not issuer-unlinkable**, which conflicts with the ARF unlinkability goal the forge brief cited.

## Re-check of the forge brief's `[UNVERIFIED]` items

| Forge item | Result |
|---|---|
| How Dock's monetisation squares with unlinkability | **Resolved:** it doesn't. The issuer learns the verifier [32][33] |
| Verified.Me "failed" | **Overturned.** Renamed "Interac verification service" and still sold [41][42]. The consumer app was retired in Sep 2024 (secondary report of an Interac notice) |
| Smart-ID reused bank onboarding | **Verified.** Bank-link registration gives a non-qualified "Basic" account usable only by banks. Bank branches registered users from 2017 [43][44] |
| Embedded disclosure policies for non-qualified EAAs | **Resolved:** EDP_01 allows them for QEAA, PuB-EAA and non-qualified EAA. The law requires them only for the first two [21] |
| AMLA reliance guidelines | **Open.** Due 10 Jul 2027 [26]. Neither published nor consulted as of today [27] |
| Art. 45h(1) applied to a telco; spec §9.7.1 liability | Not re-checked (out of scope for this decision) |

## Cross-dimension insights

- **The issuer half, the payload and the revocation path all sit in one Commission codebase.** The reference issuer has `PID_login`, `dynamic_issuing` and `por.1` [12][13], and its configuration declares a token status list service. The issuer half is therefore not new work, and Story 10.1 already targets that issuer. Building the brief's in-repo demo issuer would duplicate it.
- **The law and the technology point at different issuers.** A mandate in law is heading for business wallets [15][20]. The demonstrable technique is a PID presentation nested in issuance [1][5]. The story should test the technique and label the payload a mock. It should not claim a legal model that is still in trilogue.
- **The only "strictly better than the PID" case survives.** `por.1` carries no name or birth date [12], so the forge brief's single "yes" is real and cheap to show.

## Contrary evidence

The red-team pass was off (default). The strongest case for **park**:

- Story 10.1 is externally blocked and every gating epic is still backlog.
- The 1.0 variants do not interoperate [5].
- The binding gap (#742) [4] means the demo proves the flow, not its security.
- The IAE in 1.1 may replace variant 2 within a year [3].

Against that: the story records a test result, not a product commitment. Self-hosting the issuer removes the external block. An unproven claim in spec §11.5 becomes one test.

## Implication for the decision: **correct-course** (reshaped)

Recommend `bmad-correct-course` with **one Epic 10 story**. The shape differs from the forge brief in three ways:

1. **No new in-repo issuer.** Use a self-hosted `eudi-srv-web-issuing-eudiw-py` at a pinned revision.
2. **Payload `eu.europa.ec.eudi.por.1`**, labelled a mock, with EWC RB004 cited as the pilot precedent. Do not use a "Pääsuke PuB-EAA" label.
3. **Drop the 3.1 gate.** The reference issuer delegates the nested presentation to its configured verifier (`dynamic_presentation_url`) [10]. Re-add the gate only if the repository's own verifier is wired in.

**Proposed Story 10.6: PID-authenticated issuance of a representation attestation (nested OpenID4VP).**

> As a holder, I want the wallet to complete an issuance whose authorization step asks for a PID presentation, so that spec §11.5's nesting is exercised and an issuer that asks for too much is refused.
>
> **Given** a self-hosted reference issuer at a recorded revision, with `eu.europa.ec.eudi.por_mdoc` under `PID_login` and a `dynamic_issuing` entry naming the one PID attribute its intended use names,
> **When** the wallet starts authorization-code issuance on the same device,
> **Then** the issuer's OpenID4VP request re-enters the wallet mid-issuance. The PID presentation completes, and the issuance completes from the persisted authorization state with `por.1` stored.
> **And** an issuer whose nested request asks for PID attributes beyond its registered intended use is refused fail-closed (Story 4.1 `IntendedUseSource`). The issuance is aborted and its key released.
> **And** a test asserts that the stored attestation discloses no `family_name` or `birth_date`.
> **And** a revoked `por.1` is refused at the verifier (Story 10.4 status list).
> **And** the artefact is labelled a mock: user-entered values, the issuer's "Test QEAA Issuer" label, no Art. 45f/PuB-EAA claim.
> **And** the record states two limits. The presented PID and the attestation's key are not cryptographically linked (OIDF #742). The authorization page and the presentation's return URL must share one user agent (OIDF #730 variant 2).

**Dependencies to add:**

- **Story 10.1, real issuer integration.** The new story is blocked by 10.1. This covers interop of the pinned 0.6.1 (draft 14) against the issuer, which may force the library upgrade to ≥ 0.9.0 for 1.0 [6][7]. Also: make `credentialConfigurationIds` and the issuer URL configurable.
- **Story 4.1:** the refusal criterion depends on it.
- **Story 10.4:** the revocation criterion depends on it.
- **Spec follow-up for the same correct-course.** Annotate the spec §8 "Powers and mandates (Pääsuke) PuB-EAA RIA P2" row `[UNVERIFIED]`, add it to §23, and cite COM(2025) 838 and ARF 3.0.0 for where representation is heading.

**Not `bmad-spec`:** no payer, no issuer-blind monetisation, and no vendor demand were found. **Park** remains the runner-up. It is the right call if 10.1 stays blocked and self-hosting is ruled out.

## Open questions

- Is `por.1` live behind `PID_login` on the public `issuer.eudiw.dev`? Read the deployed metadata, or self-host.
- Does wallet library 0.6.1 interoperate with reference issuer v0.11.0 at all? Story 10.1 settles it.
- Does #742's key/claims split apply to the 1.0 nested variant? Ask on OIDF #730/#742, or analyse the binding in `app/routes/oid4vp.py`.
- Does Parliament's report A10-0240/2026 move mandates toward register-issued attestations? Read the report.
- Does Estonia have an unpublished mandate plan? RIA's rollout task table (`Tegevuste-tabel-EUDIW-valjaandmiseks.xlsx`) was not opened.
- The EWC, POTENTIAL and NOBID primary deliverables were not read; only the Meeco synthesis was.

## Decision log

Forge decisions D1–D8 are in `forge-brief.md` and `.decisions.log`, unchanged. Recon decisions below were pre-authorised by the orchestrator; no user answer existed. Both are logged in `.decisions.log` and `recon/.memlog.md`.

| # | Question | Options | Winner | Band / separation | Why |
|---|---|---|---|---|---|
| R1 | How to run the recon | standard preset, 3 researchers by type; deep preset, 6 researchers + red team; inline, no subagents | Standard, 3 researchers by type | Separated / robust | Three independent questions; "minutes, not hours" |
| R2 | Implication for the decision (options self-authored) | correct-course reshaped, wallet-side story against the reference issuer's `por.1`; correct-course as forged, in-repo issuer + Pääsuke PuB-EAA; park; amend 10.1 instead; spec | Correct-course, reshaped | Separated / robust | The reference issuer already ships the issuer half and a representation payload [12][13]. The Pääsuke PuB-EAA framing has no anchor [23][25]. Runner-up: park |

## Sources

| # | Supports | Publisher | Pub date | Accessed | Confidence |
|---|---|---|---|---|---|
| [1] | VCI 1.0 Final; no IAE; Appendix H.5 | [OpenID Foundation](https://openid.net/specs/openid-4-verifiable-credential-issuance-1_0.html) | 2025-09-16 | 2026-11-06 | high |
| [2] | IAE removed before Final | [OIDF DCP WG, issue #473](https://github.com/openid/OpenID4VCI/issues/473) | 2025-10-16 | 2026-11-06 | high |
| [3] | IAE in the 1.1 editor's draft | [OIDF, OpenID4VCI/1.1](https://github.com/openid/OpenID4VCI/tree/main/1.1) | 2026-10-15 | 2026-11-06 | high |
| [4] | Cross-session key/claims attack | [OIDF DCP WG, issue #742](https://github.com/openid/OpenID4VCI/issues/742) | 2026-05-30 | 2026-11-06 | high |
| [5] | Three variants; variant 2 reports; Animo position | [OIDF DCP WG, issue #730](https://github.com/openid/OpenID4VCI/issues/730) | 2026-05-21 | 2026-11-06 | medium |
| [6] | 0.6.1 = draft 14 | [EC EUDI, v0.6.1](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vci-kt/releases/tag/v0.6.1) | 2025-01-14 | 2026-11-06 | high |
| [7] | 1.0 alignment in v0.9.0 | [EC EUDI, v0.9.0](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vci-kt/releases/tag/v0.9.0) | 2025-09-30 | 2026-11-06 | high |
| [8] | Latest v0.16.0 | [EC EUDI, v0.16.0](https://github.com/eu-digital-identity-wallet/eudi-lib-jvm-openid4vci-kt/releases/tag/v0.16.0) | 2026-11-03 | 2026-11-06 | high |
| [9] | Reference issuer: Dynamic Credential Request | [EC EUDI, eudi-srv-web-issuing-eudiw-py](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py) | 2026-11-04 | 2026-11-06 | high |
| [10] | Same-device and cross-device OpenID4VP in authorization | [EC EUDI, app/routes/oid4vp.py](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py/blob/main/app/routes/oid4vp.py) | 2026-11 | 2026-11-06 | high |
| [11] | PID-authentication walkthrough | [EC EUDI, issuer guide](https://issuer.eudiw.dev/static/issuer_guides/credential_offer_dynamic.html) | undated | 2026-11-06 | high |
| [12] | `por.1` doctype and claims | [EC EUDI, por_mdoc.json](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py/blob/main/app/metadata_config/credentials_supported/por_mdoc.json) | 2026-11 | 2026-11-06 | high |
| [13] | `PID_login` and `dynamic_issuing` configuration | [EC EUDI, api_docs/add_credential.md](https://github.com/eu-digital-identity-wallet/eudi-srv-web-issuing-eudiw-py/blob/main/api_docs/add_credential.md) | 2026-11 | 2026-11-06 | high |
| [14] | Reference wallet `DYNAMIC_PRESENTATION` | [EC EUDI, DeepLinkHelper.kt](https://github.com/eu-digital-identity-wallet/eudi-app-android-wallet-ui/blob/main/ui-logic/src/main/java/eu/europa/ec/uilogic/navigation/helper/DeepLinkHelper.kt) | 2026-10 | 2026-11-06 | medium |
| [15] | COM(2025) 838, Arts. 3, 5, 8, 9 | [EUR-Lex](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX:52025PC0838) | 2025-11-19 | 2026-11-06 | high (date) / medium (article reading) |
| [16] | EBW publication date | [European Commission](https://digital-strategy.ec.europa.eu/en/library/proposal-regulation-establishment-european-business-wallets) | 2025-11-19 | 2026-11-06 | high |
| [17] | EBW procedure status | [European Parliament OEIL](https://oeil.europarl.europa.eu/oeil/en/procedure-file?reference=2025/0358(COD)) | live | 2026-11-06 | high |
| [18] | Council general approach; mandate carve-out | [Council of the EU](https://www.consilium.europa.eu/en/press/press-releases/2026/06/09/european-business-wallets-council-adopts-negotiating-position/) | 2026-06-09 | 2026-11-06 | high |
| [19] | ARF v3.0.0 latest | [EC ARF](https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework/releases/tag/v3.0.0) | 2026-07-23 | 2026-11-06 | high |
| [20] | Legal-person wallets removed | [EC ARF, main §3](https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework/blob/main/docs/main/03-roles-within-the-eudi-wallet-ecosystem.md) | 2026-07 | 2026-11-06 | high |
| [21] | Topic 29 RP_01/02; Topic 43 EDP_01 | [EC ARF, Annex 2](https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework/blob/main/docs/annexes/annex-2/annex-2.02-high-level-requirements-by-topic.md) | 2026-07 | 2026-11-06 | high |
| [22] | Topic I scope and status | [EC ARF, discussion topic I](https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework/blob/main/docs/discussion-topics/i-natural-person-representing-another-natural-person.md) | undated | 2026-11-06 | high |
| [23] | Pääsuke scope | [RIA](https://www.ria.ee/riigi-infosusteem/personaalriik/keskne-volituste-haldamise-infosusteem-paasuke) | live | 2026-11-06 | high |
| [24] | Pääsuke oracle model | [RIA, terms 2024](https://www.ria.ee/sites/default/files/documents/2024-03/Paasuke-uldtingimused-2024.pdf) | 2024-03 | 2026-11-06 | high |
| [25] | No mandate plan in the wallet analysis | [RIA, Phase III analysis](https://www.ria.ee/sites/default/files/documents/2026-05/Digikukru-III-etapi-analuus.pdf) | 2026-04-08 | 2026-11-06 | medium |
| [26] | AMLR reliance guidelines due 10 Jul 2027 | [EUR-Lex, Reg. 2024/1624](https://eur-lex.europa.eu/eli/reg/2024/1624/oj/eng) | 2024-06-19 | 2026-11-06 | high |
| [27] | No reliance consultation | [AMLA](https://www.amla.europa.eu/policy/public-consultations_en) | live | 2026-11-06 | high |
| [28] | Credo presentation-during-issuance flow | [OpenWallet Foundation, credo-ts](https://github.com/openwallet-foundation/credo-ts) | not retrieved | 2026-11-06 | high (code) |
| [29] | identity-common-ts 1.1 IAE (experimental) | [OWF Labs](https://github.com/openwallet-foundation-labs/identity-common-ts) | not retrieved | 2026-11-06 | high (code) |
| [30] | walt.id issuer authentication via OAuth/OIDC only | [walt.id](https://docs.walt.id/community-stack/issuer2/configurations/config-files/authentication-service) | undated | 2026-11-06 | medium |
| [31] | Truvera issuance and verification as separate steps | [Dock Labs](https://docs.truvera.io/truvera-api/openid/openid-issuance-and-verification-integration-guide.md) | undated | 2026-11-06 | medium |
| [32] | KVAC paid verification reveals the verifier to the issuer | [Dock Labs, docs](https://docs.truvera.io/truvera-workspace/monetizing-credentials.md) | undated | 2026-11-06 | high |
| [33] | Closed-ecosystem paid verification | [Dock Labs](https://www.dock.io/paid-credential-verification) | 2026 | 2026-11-06 | high |
| [34] | Signicat: PID verification only | [Signicat](https://developer.signicat.com/docs/eid-hub/identity-wallets/integration-guide/test/) | undated | 2026-11-06 | medium |
| [35] | Procivis: no PID-gated issuance page | [Procivis](https://docs.procivis.ch/eidas) | undated | 2026-11-06 | low |
| [36] | EWC RB004 Signatory Rights | [EWC consortium](https://github.com/EWC-consortium/eudi-wallet-rulebooks-and-schemas/blob/main/rulebooks/rb_004_signatory_rights.md) | not retrieved | 2026-11-06 | high |
| [37] | EWC RFC005 legal-person PID issuance | [EWC consortium](https://github.com/EWC-consortium/eudi-wallet-rfcs/blob/main/ewc-rfc005-issue-legal-person-identification-data.md) | not retrieved | 2026-11-06 | high |
| [38] | LSP synthesis: payer unanswered, representation unharmonised | [Meeco (secondary)](https://media.meeco.me/public-assets/reports/Meeco_Report_EUDI_Large_Scale_Pilots.pdf) | 2026-01 | 2026-11-06 | medium |
| [39] | Norwegian BankID reseller prices | [Stø](https://stoe.no/tjenester/id-tjenester/priser) | 2026-01-01 | 2026-11-06 | high |
| [40] | cheqd verifier-pays-issuer privacy model | [cheqd](https://docs.cheqd.io/product/studio/payments/learn/privacy-considerations) | undated | 2026-11-06 | high |
| [41] | Interac acquires SecureKey services | [Interac](https://www.interac.ca/en/content/news/interac-corp-acquires-exclusive-rights-to-securekey-digital-id-services-for-canada/) | 2021-10-01 | 2026-11-06 | high |
| [42] | Verified.Me renamed, still sold | [Interac](https://www.interac.ca/en/content/the-digital-authentication-and-verification-timeline/) | live | 2026-11-06 | high |
| [43] | Smart-ID Basic via bank link | [SK ID Solutions](https://github.com/SK-EID/smart-id-documentation/blob/master/docs/modules/ROOT/pages/demo.adoc) | 2026-04-30 | 2026-11-06 | high |
| [44] | Bank-branch Smart-ID registration | [SK ID Solutions](https://www.skidsolutions.eu/news/smart-id-can-now-be-applied-for-in-bank-branches/) | 2017-09-20 | 2026-11-06 | high |
| [45] | RIA wallet page: no mandate plan | [RIA](https://www.ria.ee/riigi-infosusteem/elektroonilise-identiteedi-teenused/digikukkur-eudi-wallet) | live | 2026-11-06 | medium |

## Staleness map

These are the fastest-ageing claims; re-check dates come from `recon_kit.py staleness`. **Earliest re-check: 2026-12-01**, the EBW trilogue status [17][18]. Use Refresh to re-run them.

| Claim | Class | Re-check by |
|---|---|---|
| EBW procedure status and mandate articles [15][17][18] | regulatory | 2026-12-01 |
| Reference issuer `PID_login`/`por.1`; library has no IAE [9][12][13][6]–[8] | capability / version | 2027-02-01 |
| Vendor capability absences [30]–[35] | capability | 2027-02-01 |
| Estonian mandate plan absent [25][45] | plan | 2027-04-01 |
| OpenID4VCI 1.1 IAE status, #742 [3][4] | spec | 2027-04-01 |
| AMLA reliance guidelines [27] | regulatory | before 2027-07-10 |
