# Forge brief: verify the PID, then issue a derived attestation

**Idea under test.** A LinkedIn post by the CEO of Dock Labs (`idea.md`, not committed): businesses that verify a customer's EUDI PID should then issue their own credentials (telco account holder, employment, role or authority to act, bank customer, insurer) that other businesses verify, instead of re-verifying from scratch.

**Question decided here.** Should *this* repository pursue "verify PID, then issue a derived EAA", and if so in what narrow first form?

**Outcome: HARDENED, after narrowing.** Run autonomously on 2026-11-06 via `bmad-forge-idea`. Every choice went through `tiltrank`; the log is below and in `.decisions.log` / `decisions/` next to this file. The session record is `.memlog.md`.

## Verdict

Mostly no to the post's thesis, with a narrow yes for this repository.

- **No** to re-attested identity. A business attestation that re-states who the holder is gives a weaker statement than the PID it came from. No separate product either: nobody is clearly paying, the market has two sides that each wait for the other, and vendors already sell issuance platforms.
- **Yes** to one gated story. It makes testable a flow the repository's own specification already describes: an issuer authenticates the holder by an OpenID4VP presentation of the PID nested inside OpenID4VCI, then issues a non-PID attestation carrying a fact the PID cannot carry. The issuer carries every relying-party obligation while it asks for the PID (`spec/EE-EUDIW-TS-1.0.md` §11.5, "The two protocols nest").

## Recommended next step

**`bmad-correct-course`**: add one story to Epic 10 (no new epic), gated behind Stories 3.1 and 10.1. Its refusal criterion depends on Story 4.1 and its revocation criterion on Story 10.4. Not `bmad-spec`, because no separate product survives the payer analysis. Not drop, because the slice tests the specification's own text and reuses the Epic 4 policy engine.

The close runner-up was **park**: record the position and change nothing in the backlog. Epics 3, 4 and 10 are all still `backlog` in `sprint-status.yaml`, so parking costs nothing today. Correct-course should weigh that, and may still choose to park.

## The first slice

**Story (proposed): PID-authenticated issuance of a non-PID attestation (§11.5 nesting).** A demo issuer runs its OpenID4VCI authorization step as a relying party.

1. It requests an OpenID4VP presentation of the PID, limited to the attribute it needs to look the holder up, under a registered intended use that names that attribute.
2. It issues a **mock mandate attestation**: represented entity's registry code, scope or role, validity, and a status-list index. It is shaped after the spec §8 catalogue row "Powers and mandates (Pääsuke), PuB-EAA, RIA, P2" and carries no PID attributes beyond holder key binding.

The wallet treats the issuer as a relying party.

Acceptance should prove four things:

- An issuer that asks for more PID attributes than its intended use names is refused (Epic 4 `IntendedUseSource`, fail-closed).
- The issued attestation discloses no `family_name` or `birth_date`.
- A revoked mandate fails at the verifier (Story 10.4 status list).
- The artefact is labelled a mock and claims neither Art. 45f compliance nor PuB-EAA status.

Parts it reuses:

- `verifier/go` (OpenID4VP)
- `issuer/` (mdoc minting)
- the wallet's `OpenId4VCIManager` authorization-code path
- `statuslists/`

The issuance-log separation required by Art. 45h(1) becomes a design note.

## How the idea changed under pressure

| Before | After (locked) |
|---|---|
| A business re-attests the identity it verified | Identity re-assertion is a non-goal. A derived attestation is worth issuing only for a fact the PID cannot carry: account standing, tenure, role, mandate |
| Telcos, employers, banks and insurers as issuers | In Estonia, mandates and representation sit with the state (Pääsuke, Business Register). The strongest derived attestation here is a PuB-EAA, not a business EAA. That contradicts the post for this jurisdiction |
| A market of thousands of issuers | No payer found for a product. A reference slice only, gated |
| The derived attestation replaces the PID | It can be *less* identifying than the PID. "Representative of X" or "account in good standing" can be presented with no name or birth date. That is the one case where it is strictly better |

## Strongest objections and how they were answered

1. **Re-attestation is weaker than the source.** *Answered by narrowing.* The PID is issued under a LoA-high scheme (Art. 5a(11)). A business non-qualified EAA gets only Art. 45b(1): it cannot be denied legal effect merely for being electronic or non-qualified. Paper-equivalence (Art. 45b(2)) is reserved for QEAAs and public-sector attestations from an authentic source.
2. **AML reliance does not work this way.** *Answered by exclusion.*
   - AMLR Art. 48 allows reliance only on other **obliged entities**, and telcos and employers are not obliged entities.
   - Ultimate CDD responsibility stays with the bank that relies.
   - Art. 49(2) requires copies of the CDD information on request. A credential carries the claim, not the legal basis.
   - Banks can already identify directly with eID means at substantial or high assurance (Art. 22(6)(b)), which includes the PID.

   The option "bank issues a CDD-completed attestation" was rejected (D5).
3. **Who pays?** *Not answered for a product. Answered for the slice.*
   - The wallet is free to natural persons (Art. 5a(13)).
   - Under the ARF's unlinkability goals the issuer should not see presentations, so a per-verification fee cannot be metered at presentation time.
   - Art. 45h(1) bars an EAA provider from combining EAA-service data with its other services. That blunts the retention and upsell motive.
   - Norwegian BankID shows relying parties do pay per transaction for bank-proofed identity: reseller list prices on 1.1.2026 were NOK 1.34 for a high-level authentication and NOK 4.46 for a signature. But BankID is a single consortium scheme with the bank in the loop, which is the opposite of the EUDI model.
   - Dock advertises "privacy-preserving credential monetization". How it squares with unlinkability is `[UNVERIFIED]`.
4. **Two-sided cold start.** *Not answered.*
   - GSMA Mobile Connect "failed to scale due to a lack of defined standards", with separate contracts per operator (Vonage, a vendor source).
   - What survives in telco identity is **KYC Match**: an API that checks data against operator records, with the operator in the loop and paid by the verifier. It is not a credential.
   - Canada's bank-consortium network Verified.Me launched in 2019, and SecureKey was acquired by Interac in 2021. Reading that as a failure is `[UNVERIFIED]`.
   - The slice sidesteps this objection; a product would not.
5. **Scope creep against a repository with unstarted epics.** *Partly answered.* Story 7.8's "stay narrow" covers ZKP scope, not issuance, so it does not bar the slice. The slice is one story, gated, and starts after 3.1 and 10.1. Park remains a fair alternative (D7 was fragile).
6. **Mandates need revocation.** *Accepted as a dependency.* A revoked PuB-EAA "shall lose its validity from the moment of its revocation" (Art. 45f(4)), so the slice depends on Story 10.4.
7. **The orchestrator's premise that `ee.riik.poa.1` is a power-of-attorney credential.** *Corrected.* It is the Estonian **Proof of Age** attestation (spec §9). Mandates appear only as the §8 catalogue row above.

## Who pays, who benefits

| Role | Pays | Benefit | Verdict |
|---|---|---|---|
| Issuer (business) | Issuance build, Art. 19a duties, registration and vetting (ARF `Reg_01`, `Reg_21`), and liability, which for a non-qualified provider falls on the claimant under Art. 13 (spec §9.7.1, not re-checked this session) | Fewer manual "proof of account" requests; no data reuse (Art. 45h) | Weak motive |
| Issuer (state, mandates) | Public budget | Statutory service; the EU Business Wallet proposal (19 Nov 2025) pushes representation online | Plausible |
| Verifier | Integration, registration with intended use (Art. 5b(2)(c), (3)) | Skips document checks for facts the PID lacks | Real, but only once issuers exist |
| Holder | Nothing (Art. 5a(13)) | Shows less than the PID would | Real |

## What an open-source PoC adds that vendors do not sell

Dock (Truvera) sells credential issuance, a white-label wallet and SDK, ecosystem management and paid verification. This repository cannot and should not compete with that. What it can add is **spec-traceable compliance evidence**:

- a testable reference for the issuer acting as a relying party (intended-use-limited PID request at issuance, access certificate, response encryption);
- a wallet that refuses an over-asking issuer.

That is a conformance artefact, not a platform.

## Evidence

Status: **V** = primary text read this session; **S** = secondary source read this session; **U** = `[UNVERIFIED]`.

| Claim | Source | Status |
|---|---|---|
| Art. 45b(1)–(3): legal effects of an EAA; paper-equivalence only for QEAAs and public-sector attestations from an authentic source | Regulation (EU) 2024/1183, https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=OJ:L_202401183 | V |
| Art. 45f(1)–(4): public-sector EAA requirements; revoked means invalid immediately | same | V |
| Art. 45g(1): providers let users request and obtain EAAs; Art. 45h(1)–(2): no combining EAA-service data with other services; logical separation | same | V |
| Art. 5b(1)–(3), (9): relying-party registration, intended use, no data beyond it, relying party validates PID and EAA | same | V |
| Art. 5a(11) LoA high; Art. 5a(13) wallet free to natural persons; Art. 5a(14) wallet provider data separation | same | V |
| Art. 19a(1): non-qualified trust service provider duties, including the 24-hour breach notice | same | V |
| A recital names "powers of representation and e-mandates" as a wallet goal | same | V |
| AMLR Art. 48 (reliance on obliged entities only; responsibility stays with the relying entity), Art. 49 (CDD copies on request), Art. 50 (AMLA guidelines by 10 July 2027) | Regulation (EU) 2024/1624, https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=OJ:L_202401624 | V |
| AMLR Art. 22(6)(b): eID means at substantial or high assurance for CDD | same | V |
| AMLR applies from 10 July 2027, with an exception for some obliged entities | same | V |
| GDPR Art. 5(1)(b) purpose limitation; Art. 6(4) compatibility test | Regulation (EU) 2016/679, https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32016R0679 | V |
| ARF: non-qualified EAA providers register (`Reg_01`, `Reg_01a`), receive access certificates (`Reg_10`), and are approved by a Member State policy (`Reg_21`) | `eudi-arf/docs/annexes/annex-2/annex-2.02-high-level-requirements-by-topic.md` (ARF commit 6373eee) | V |
| ARF: an Attestation Provider authenticates the user where needed, by a method out of the ARF's scope | `eudi-arf/docs/main/06-trust-model.md` §6.6 issuance step 1 | V |
| ARF embedded disclosure policies (§6.6.2.8, Topic 43); whether they apply to non-qualified EAAs is unclear | `eudi-arf/docs/main/06-trust-model.md` | V (existence) / U (scope) |
| Spec §11.5 already describes OpenID4VP nested in OpenID4VCI, with the issuer bound by relying-party duties | `spec/EE-EUDIW-TS-1.0.md` L891 | V |
| `ee.riik.poa.1` is Proof of Age; mandates are a P2 PuB-EAA by RIA | `spec/EE-EUDIW-TS-1.0.md` §8, §9 | V |
| Epics 3, 4 and 10 are all `backlog` | `_bmad-output/implementation-artifacts/sprint-status.yaml` | V |
| Pääsuke is RIA's central system for managing access authorisations for company representatives | https://www.ria.ee/riigi-infosusteem/personaalriik/keskne-volituste-haldamise-infosusteem-paasuke | S |
| European Business Wallets proposal published 19 Nov 2025 | https://digital-strategy.ec.europa.eu/en/library/proposal-regulation-establishment-european-business-wallets ; Council ST 7659/2026 https://data.consilium.europa.eu/doc/document/ST-7659-2026-INIT/en/pdf | V |
| Mobile Connect failed to scale for lack of standards; separate contracts per operator | https://www.vonage.com/blog/aduna-vonage/ (vendor) | S |
| KYC Match checks user data against operator records | https://opengateway.telefonica.com/en/apis/kyc-match ; https://www.gsma.com/solutions-and-impact/gsma-open-gateway/austria-camara-api-launch/ | S |
| Norwegian BankID reseller prices on 1.1.2026 | https://stoe.no/tjenester/id-tjenester/priser | S |
| Verified.Me launched 2019; Interac acquired SecureKey 2021; "failed" | https://www.cbc.ca/news/business/canada-big-banks-launch-verified-me-network-data-identify-theft-1.5118471 ; https://www.interac.ca/en/content/news/interac-acquires-securekey-digital-id-services-for-canada-five-things-you-need-to-know/ | S (facts) / U (failure) |
| Dock sells issuance, a wallet SDK, ecosystems and paid verification; it frames "verifier pays issuer" as the main fee model | https://www.dock.io/ ; https://www.dock.io/post/eudi-wallet-3-key-business-models-fee-structures-explained | S |
| Smart-ID's history of reusing bank onboarding in the Baltics | not found this session | U |

## Decision log

Run autonomously: the orchestrator pre-authorised every pick, and no user answer existed for any of them. Bands come from the tiltrank rubric; no scores are shown.

| # | Question | Options | Winner | Band / separation | Why |
|---|---|---|---|---|---|
| D1 | Session goal | clarify; test; make better | Test whether it holds up | Separated / robust | The orchestrator asked for a verdict, so attack the central claim |
| D2 | First branch | central claim; economics; legal; project fit | Central claim | Separated / robust | Every other branch depends on whether a derived attestation beats asking for the PID again |
| D3 | Persona line-up | analyst + bank AML officer; architect + telco product owner; PM + small verifier; rotate | Rotate the three pairs by branch | Contested / fragile; escalated | Probe: the forge skill requires rotating voices. Low stakes |
| D4 | What the derived attestation carries | relationship facts; re-attested identity; both | Relationship facts the PID cannot carry | Separated / robust | Re-attestation is weaker than the PID (Art. 45b) |
| D5 | Demo payload for the slice | mandate (Pääsuke-shaped PuB-EAA, mock); telco account standing; employer role; bank CDD attestation | Mandate | Contested / marginal; escalated | Probe: spec §8 lists mandates and no private relationship attestation; telco facts are already sold as KYC Match. Runner-up: telco tests the post's actual thesis |
| D6 | Economics | blocks a product but not a slice; kills it for this repo; verifier-pays makes a product viable | Blocks a product, not a slice | Separated / robust | No payer and a two-sided cold start for a product; the slice costs only repository time |
| D7 | Verdict | fold via correct-course (gated); park; drop; separate product via spec | Fold, gated behind 3.1 and 10.1 | Contested / fragile; escalated | Probe: Story 7.8's narrowness is about ZKP only, and spec §11.5 already describes the flow. Runner-up: park costs nothing while the epics are unstarted |
| D8 | Write the brief | write; memlog only | Write forge-brief.md | Separated | Requested output |

Personas used, all voiced by one model: Mary (analyst), Winston (architect) and John (PM) from the BMad pool, plus three generated roles: a bank AML officer, a telco product owner and a small verifier (a rental platform).

## Open questions for deep recon

1. **Slice cost.** Can the wallet (`eudi-lib-jvm-openid4vci-kt` 0.6.1 plus the app's authorization-code path) handle an OpenID4VP request that arrives *during* an OpenID4VCI authorization on the same device? Does the Commission's reference issuer (the Story 10.1 target) implement PID-authenticated issuance?
2. **Payload choice.** Will Estonia issue mandates and representation as a PuB-EAA from Pääsuke and the Business Register? How does the European Business Wallets proposal split representation credentials between personal and business wallets? And does Pääsuke's scope, access authorisations for e-services, cover general powers of representation?
3. **The post's thesis in Estonia.** Is there a documented Estonian verifier need for a *business-issued* relationship attestation the PID cannot meet, and would anyone pay for it? Does any verifier-pays-issuer mechanism work without breaking ARF unlinkability (Dock's claim)?

Also open:

- AMLA's reliance guidelines (Art. 50, due 10 July 2027).
- Whether embedded disclosure policies apply to non-qualified EAAs.
- How Art. 45h(1) applies to a telco issuing from its contract data.
- Smart-ID's history of reusing bank onboarding.
