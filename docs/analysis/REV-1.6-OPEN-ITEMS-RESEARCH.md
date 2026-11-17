# Spec rev 1.6: research on the two open items

Research of 12 October 2026 on what remained open after spec revision 1.6 (`a5cf237`): the 29 February rule in EE-POA-027 (§23 item 36), and the external anchors of the eighteen new requirements whose conformity rows read "not met". Sources were read through the self-hosted Firecrawl instance on the date above.

## 1. The 29 February rule (EE-POA-027, §23 item 36)

### Finding

Estonian law gives **28 February**, from 00:00, and the only published application of the statute to age says so in substance. 1 March has no Estonian source.

### Evidence

Tsiviilseadustiku üldosa seadus, consolidated text at [Riigi Teataja](https://www.riigiteataja.ee/akt/TsÜS), read 12 October 2026:

- § 135(1): "Tähtaja kulgemine algab järgmisel päeval pärast selle kalendripäeva või sündmuse saabumist, millega määrati kindlaks tähtaja algus, kui seadusest või lepingust ei tulene teisiti."
- § 135(2): "Tähtaeg lõpeb tähtpäeva saabumisel."
- § 136(2): "Kui tähtpäeva saabumine on määratud aastates arvutatava tähtajaga, saabub tähtpäev viimase aasta vastaval kuul ja päeval."
- § 136(4): "Kui käesoleva paragrahvi lõigetes 1 ja 2 sätestatud korras määratav tähtpäev satub kuule, milles vastavat kuupäeva ei ole, loetakse tähtpäev saabunuks selle kuu viimasel päeval."
- § 137(1): "Kui tähtpäeva saabumine on määratud päevades või suuremates ajaühikutes arvutatava tähtajaga, möödub tähtaeg tähtpäeva saabumise päeval kell 24.00, kui seadusest ei tulene teisiti."

§ 136(4) refers to subsection 2, the rule for periods counted in years, so it covers an anniversary falling in a February without a 29th: the *tähtpäev* is the last day of February, 28 February.

[Jurist Aitab, "Millal loetakse isik täisealiseks"](https://www.juristaitab.ee/et/millal-loetakse-isik-taisealiseks) applies exactly these provisions to age. Birth is the event (§ 7(2)), the 18-year period runs from the following day (§ 135(1)), the *tähtpäev* falls on the corresponding month and day (§ 136(2)), and "isik täisealiseks tema 18. sünnipäeval … on isik täielikult teovõimeline alates 18. sünnipäeva saabumisest". That is the start of the birthday, which matches EE-POA-027's 00:00 and resolves the § 137(1) end-of-day reading that item 36 left open. The page does not discuss 29 February, but its method plus § 136(4) gives 28 February.

The source's standing limits how far it can be relied on. Jurist Aitab is the state-funded legal-help portal, run since 1 January 2026 by Eesti Juristide Liit MTÜ (per the Jõhvi and Kohtla-Järve municipal notices). It is legal guidance, not a court ruling.

### What was searched and not found

- **Case law.** Riigikohus searches for leap-year or age-reckoning rulings found nothing. No Estonian judgment applying § 136(4) to a date of birth was found.
- **Commentary.** The commented edition (*Tsiviilseadustiku üldosa seadus. Kommenteeritud väljaanne*, 2010; new edition reviewed in Juridica 2023/2) sits behind juuraveeb.ee's paywall and was not read.
- **Age-gating statutes.** Tubakaseadus § 27(1) defines *alaealine* only as "alla kaheksateistaastane isik", with no timing rule. KarS and Rahvastikuregistri seadus contain no age-reckoning rule either. Alkoholiseadus and Hasartmänguseadus did not render through the scraper; they are expected to follow the same pattern but were not read.
- **EU instruments.** The EU Age Verification Blueprint technical specification ([architecture](https://docs.ageverification.dev/av-doc-technical-specification/docs/architecture-and-technical-specifications/), [Annex A AV Profile](https://docs.ageverification.dev/av-doc-technical-specification/docs/annexes/annex-A/annex-A-av-profile/)) has no rule for leap days, time zones or dates of birth. The ARF says only that `age_over_NN` values change "whenever the User … has a relevant birthday" (PID provider requirement, ARF Annex 2.02). Neither instrument fixes the rule, so it is national.
- **Other jurisdictions** differ: New Zealand uses 28 February, the United Kingdom 1 March (timeanddate.com summary, secondary). This confirms that there is no common default to fall back on.

### Recommendation

Replace EE-POA-027's 1 March with **28 February at 00:00 Europe/Tallinn**, citing TsÜS § 135(1)–(2) and § 136(2), (4) as applied to age by Jurist Aitab. Reduce item 36 from "no provision found" to "no court ruling or commentary confirms the analogy" and keep it `[UNVERIFIED]` for that narrower point. Consequences of each choice:

- **1 March** is under-inclusive. A person who is 18 under the only published Estonian reading is refused for one day in three years out of four. The predicate never says "18" too early, but the spec cannot cite a source for it.
- **28 February** follows the statute. A relying party that accepts the predicate on 28 February is accepting someone Estonian civil law treats as 18.

The maintainer confirmed 28 February on 12 October 2026; revision 1.7 adopts it. A written opinion from Justiits- ja Digiministeerium (which handles legal-language and civil-code questions) would move it from analogy to authority, if certainty is wanted before a production Provider exists.

## 2. The eighteen rev 1.6 requirements: external anchors

Each row says what the external source fixes and what the specification adds on its own. It does not change what the code has to do; the conformity rows still apply.

| Requirement | External anchor | Status of the EE figure or rule |
|---|---|---|
| EE-POA-013 delete on plain presentation | AV spec §3.4: present a Proof of Age attestation "only once" and then remove it from the batch; ZKP presentations may be reused within validity. ARF `WIAM_21` is a **SHOULD** (check the WSCD for once-only assets that can no longer be presented) | EE makes deletion and key destruction a SHALL. That is a deliberate strengthening of a SHOULD; the citation should say "strengthens `WIAM_21`" |
| EE-POA-003 batch for `eu.europa.ec.av.1` | AV spec: Attestation Provider SHALL support batch issuance, recommended batch of **30** | Aligned |
| EE-POA-025 re-issue 14 days before `validUntil` | AV spec §3.4.2: re-issuance requires re-identification at least every **3 months**; no lead time given | 14 days is EE's own figure; label it as such |
| EE-POA-026 7-day `ValidityInfo` epoch | AV spec: Attestation Provider SHALL set `ValidityInfo` timestamps "with a precision that limits linkability"; no granularity given. ARF `OIA` requirement: relying-party instances discard timestamps once no longer needed | 7 days is EE's own figure for an unspecified precision |
| EE-POA-029 `validUntil` not birthday-derived | AV spec: claims carry no date of birth, so re-issuance needs re-enrolment | Aligned in intent |
| EE-POA-030 one predicate per presentation | ISO/IEC 18013-5 §7.2.5 (quoted in [multipaz#241](https://github.com/openwallet-foundation/multipaz/issues/241)): a reader "shall not request more than two `age_over_NN`", the mdoc "should not return more than two" | EE is stricter (one, unless registered). This is a divergence from ISO and is **not listed in §22** |
| EE-POA-031 `false` withheld | ISO/IEC 18013-5 §7.2.5 "nearest true attestation" behaviour returns `age_over_NN: false` when no true value at or above the threshold exists; AV Annex A says §7.2.5 "may be considered" | EE departs from ISO behaviour. Add to §22. The exact §7.2.5 wording was read only second-hand: **[UNVERIFIED]** |
| EE-PRO-026 `request_uri` 3 minutes, single use | OpenID4VP 1.0 gives no lifetime for `request_uri`. Its only time rule is that the wallet rejects an expired Verifier Attestation JWT "subject to allowable clock skew" | 3 minutes is EE's own figure; it matches the Go verifier's `-session-ttl` default |
| EE-ZKP-062 60 s clock tolerance | RFC 7519 §4.1.4: "some small leeway, usually no more than a few minutes, to account for clock skew" | 60 s is within RFC 7519; EE's own figure |
| EE-PRO-023 pre-authorized code, `tx_code`, 10 min | OpenID4VCI 1.0 §4.1.1: the code "MUST be short lived and single use"; `tx_code` is RECOMMENDED over a separate channel; no figure | 10 minutes is EE's own figure, as the spec already says |
| EE-REV-007 RP cache age ≤ `ttl` | Token Status List `draft-ietf-oauth-status-list-21` §5.1: `ttl` is RECOMMENDED and gives the maximum time a token may be cached before a fresh copy SHOULD be retrieved | Aligned; fail-closed for PID is EE's addition |
| EE-ZKP-055a, EE-PRO-025, EE-ZKP-045a | ISO/IEC 18013-5 reader authentication; ARF relying-party registration (Topic X refinement still in discussion) | No external rule on how a wallet treats unauthenticated age requests; EE's own choice |
| EE-WUA-007, EE-WUA-008 | ARF TS3 (WUA/KA) and AV Annex A §A.5 (AVS Unit Attestation, "internal mechanism" allowed if outcomes are met) | Aligned with AV Annex A; no ARF requirement text banning a stable WUA identifier was found by search |
| EE-POA-024, -028, -032; EE-SEC-001; EE-PID-009; EE-ONB-011; EE-GOV-032 | No external figure; national or architectural choices | — |

### What this means for "not met"

None of the "not met" rows depends on an unresolved external question. Each is an implementation gap with a fixed target. Ranked by user-visible effect and by how much shared code each change needs:

1. **EE-POA-030, EE-POA-031** (wallet presentation filter). One check at the point where the DCQL query is matched to the credential: refuse more than one `age_over_NN` without registered intended use, and answer "no matching credential" for a held `false`. This is the smallest change, it is privacy-relevant, and it is testable in `:app:testLocal_mocksUnitTest`.
2. **EE-ZKP-055a** (`HolderObligations.refusePlainFallback`). Key the decision on the absence of an authenticated registration certificate, not on `proofRequested`.
3. **EE-POA-013, EE-POA-003** (consumption). Extend `EePoaConsumption` to the AV doctype, add the last-attestation path (prefer Tier 2, show the notice).
4. **EE-POA-025, EE-POA-029** (scheduler). One scheduled job covers both the 14-day lead and the birthday refresh. The app has no background scheduler today (no `androidx.work` dependency), so this is the largest item.
5. **EE-POA-026, EE-POA-027/028** (mock issuer). Floor `validFrom`/`validUntil` to the 7-day epoch, compute predicates from a date of birth in Europe/Tallinn with the chosen 29 February rule. These bind the Provider; in this repository they only make the fixtures honest.
6. **EE-PRO-025, EE-WUA-008, EE-POA-032** need trust infrastructure (Estonian ACA, AV provider, Wallet Unit revocation) that does not exist here yet, so they stay "not met" by design.

## 3. Spec edits (made in revision 1.7)

- EE-POA-027: 1 March → 28 February; rewrite the *Why* paragraph around § 136(4) and Jurist Aitab; narrow §23 item 36.
- §22: add EE-POA-030 and EE-POA-031 as divergences from ISO/IEC 18013-5 §7.2.5.
- EE-POA-013 / Annex E: cite `WIAM_21` as a SHOULD that EE strengthens.
- EE-POA-025, EE-POA-026, EE-PRO-026, EE-ZKP-062: mark 14 days, 7 days, 3 minutes and 60 seconds as this specification's choices, as EE-PRO-023 already does for its 10 minutes.
