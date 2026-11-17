# Dock Labs paid verification and "verifiable identification" against eIDAS 2, the implementing acts and ARF 3.0.0

Date: 2026-11-07. Companion to [critique.md](critique.md), whose open item on Art. 5a(5)(b), 5a(16)(a), 45b and 45h this closes, and to [recon-summary.md](recon-summary.md).

## Verdict

Outside the EUDI Wallet, what Dock does is lawful as far as eIDAS goes. Art. 5a regulates European Digital Identity Wallets and nothing else. A Truvera wallet with a closed ecosystem of issuers and verifiers is not an EUDI Wallet, so Art. 5a does not bind it. GDPR still does, and so does AMLR wherever a credential carries a KYC result ([critique.md](critique.md), C6).

Inside the EUDI Wallet, the paid-verification product cannot be deployed as built. Four independent grounds each block it on their own:

1. **Format.** An EAA issued to a wallet unit must comply with ETSI TS 119 472-1 clauses 2–6, which cover SD-JWT VC and mdoc. Dock's KVAC credentials are W3C VCs, and W3C VCs are clause 7, which the law leaves out (G1).
2. **Validation.** An EAA must carry a digital signature made with the issuer's private key, together with everything needed to validate it. A KVAC-locked credential can be validated only with the issuer's secret, provided live during each verification (G2).
3. **Tracking.** After issuance, the issuer learns that a credential of schema S went to verifier V at time t. Art. 5a(16)(a) says the technical framework shall not allow that unless the user explicitly authorises it, and Dock's consent step authorises the verification, not issuer knowledge. ARF `ZKP_07` forbids the extra issuer round-trip outright, with no consent exception (G3).
4. **Offline use.** Unlocking needs the issuer online, so the credential cannot be presented in proximity or offline. Art. 5a(4)(a) and 5a(5)(a)(iii) require offline presentation "where appropriate" (G4).

One correction to [critique.md](critique.md) C10: it rested the collision partly on Art. 5a(5)(b). That point binds only the wallet ("not provide any information to trust service providers of electronic attestations of attributes about the use of those electronic attestations"). In Dock's flow the verifier contacts the issuer, not the wallet, so 5a(5)(b) is not breached on its letter. The binding constraint is Art. 5a(16)(a), and that article has a consent exception the critique did not mention. The C10 verdict stands, on a narrower and stronger basis.

What eIDAS does **not** forbid is charging verifiers. No provision bars a commercial arrangement between a non-qualified EAA provider and a relying party. Art. 5a(13) makes the wallet free for natural persons, not attestations for relying parties. The prohibited part is the per-transaction signal that metering needs. A compliant version is set out under "What a compliant version looks like".

## Sources read

Primary text was fetched and read on 2026-11-06/07. Quotes below are verbatim from these files.

| Source | Version read |
|---|---|
| Dock, [Paid credential verification](https://www.dock.io/paid-credential-verification) | live page, 2026-11-06 |
| Dock, [Verifiable Identification: What It Is and How It Works](https://www.dock.io/post/verifiable-identification) | published 15 May 2026 |
| Truvera docs, [Monetizing credentials](https://docs.truvera.io/truvera-workspace/monetizing-credentials.md) and [Verify credentials](https://docs.truvera.io/truvera-workspace/verify-credentials.md) | live, 2026-11-07 |
| Regulation (EU) No 910/2014 as amended by [Regulation (EU) 2024/1183](https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32024R1183) | OJ text, Arts. 5a, 5b, 5f, 45b–45h |
| CIR (EU) [2024/2977](https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32024R2977), [2024/2979](https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32024R2979), [2024/2982](https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32024R2982) as amended by [CIR (EU) 2026/1731](https://eur-lex.europa.eu/legal-content/EN/TXT/HTML/?uri=CELEX:32026R1731) | OJ L 2026/1731, 22.7.2026 |
| ETSI [TS 119 472-1 V1.2.1 (2026-02)](https://www.etsi.org/deliver/etsi_ts/119400_119499/11947201/01.02.01_60/ts_11947201v010201p.pdf) | made binding by CIR 2026/1731 Annex IV |
| ARF [3.0.0](https://eudi.dev/3.0.0/main/), §2.3, ch. 4 §4.2.4, ch. 5, ch. 7 §7.4.3.5, Discussion Paper A §2.3; HLRs from `hltr/high-level-requirements.csv` | git tag `v3.0.0`, commit `c64f2cb` (725 HLRs) |

EUR-Lex's "Amended by" field lists no amendment for 2024/2977, 2024/2979 or 2024/2982, although CIR 2026/1731's title says it amends them. The amendments were read from the 2026/1731 text itself. No consolidated version was available. `[UNVERIFIED: no later amendment after 2026/1731]`

## What Dock actually does

From Dock's own text, not inferred:

- The business model: "you issue a reusable credential. That credential travels with the business or user, and every time a third party verifies it, you charge for it." The ecosystem administrator "assigns a per-verification price to each credential schema", and Truvera charges "a small percentage fee for each verification transaction" (paid page).
- The gate: the relying party is shown the fee "before the credential is unlocked", and KVAC "ensures that credentials can only be verified by participants in your ecosystem" (paid page).
- The mechanism: "During the verification process, the issuer is asked to provide an additional secret for the specific credential schema in order to unlock it." The verifier sends the issuer a randomised pair (A, B) and asks "whether A equals B \* issuer-secret-key. Here A is the MAC from holder's credential but randomized so issuer can't identify it" (Truvera docs).
- What the issuer learns: "The Issuer sees that a credential based on a specific schema was shared with a specific Verifier, but learns nothing about the Holder … The Issuer needs to use the same schema for enough credentials for Holders to benefit from herd privacy" (Truvera docs). The marketing page says the same thing in other words: the administrator "sees that an ID credential from a specific issuer and based on a specific schema was shared with a verifier".
- A side effect: "When verifying ecosystem bound credentials with paid schemas, the issuance date can not be revealed" (Truvera verify docs).
- Consent: "Users must give explicit consent for each verification" (paid page).
- Status: Dock's pricing page lists "Monetize: Charge credential verification" as a plan feature, and marks one plan "available in Q3".

This is a designated-verifier design. Verification needs the issuer's secret key, so it is cryptographically impossible without the issuer's cooperation in real time. That property makes billing enforceable, and the same property causes each of the conflicts below.

## Ground by ground

### G1. Format: KVAC W3C credentials are not a listed EAA format

- CIR 2024/2977 Art. 4(1), as replaced by CIR 2026/1731 Art. 1(2): "Electronic attestations of attributes issued to wallet units shall comply with at least one of the standards set out in Annex II of Implementing Regulation (EU) 2024/2979."
- CIR 2024/2979 Annex II, as replaced by CIR 2026/1731 Annex IV: "The technical specifications set out in clauses 2 to 6 of ETSI TS 119 472-1 V1.2.1 (2026-02) apply." In that TS, clause 5 is "Implementation of EAA based on SD-JWT VC", clause 6 is "Implementation of EAA based on ISO/IEC-mdoc", and clause 7, "Implementation of EAA based on W3C Verifiable Credentials (JSON-LD W3C-VC)", lies outside the referenced range.
- CIR 2026/1731 recital 1 states the intent: wallets "should also support this format when the new profiles on the W3C VCDM format are available."
- The ARF is more permissive: `ARB_01a` allows W3C VCDM v2.0 for non-qualified EAAs, ARF §5.4.1 marks wallet support "Optional", and `ISSU_29` notes that a non-qualified EAA provider "may choose to issue attestations in the format specified in [W3C VCDM v2.0]". `ARB_04` then requires the Rulebook to reference a presentation and selective-disclosure specification "approved by an EU standardisation body or by the European Digital Identity Cooperation Group". The ARF is not law. Where it is wider than the CIR, the CIR governs.
- **Result: fails today.** Interpretive margin: clause 4 of the TS is format-neutral semantics, so one could argue that a JSON-LD EAA meeting clause 4 "complies with" Annex II. That reading would empty the clause 2–6 boundary, and recital 1 contradicts it. Even if W3C VCs are profiled later, a KVAC proof also has to pass G2.

### G2. Validation: an EAA must be publicly verifiable without the issuer

- ETSI TS 119 472-1 `EAA-4.6.1-01`, inside the binding clauses 2–6: "An EAA shall incorporate a digital signature generated by the EAA issuer using its private key." `EAA-4.6.1-07` adds that the signing certificate shall meet the TS 119 412-6 EAA profile, which means X.509, not a DID document.
- CIR 2024/2977 Art. 4(3), unchanged by 2026/1731: "Providers of electronic attestations of attributes shall ensure that electronic attestations of attributes issued to wallet units contain the information necessary for authentication and validation of those electronic attestations of attributes."
- Art. 5b(9) places validation on the relying party: "Relying parties shall be responsible for carrying out the procedure for authenticating and validating person identification data and electronic attestation of attributes requested from European Digital Identity Wallets."
- A KVAC credential is authenticated by a MAC under the issuer's secret key, and Dock's own description says verification needs the issuer to "provide an additional secret … in order to unlock it". The credential therefore does not contain what is needed to validate it, and the relying party cannot carry out validation alone.
- Inference, `[UNVERIFIED]`: Dock might attach a public signature beside the MAC. If it did, any verifier could validate without paying, and the lock would be decorative. The commercial design only works if no publicly verifiable proof is presented.
- **Result: fails.** This is the deepest conflict, because it is architectural rather than a choice of format. Paid unlocking and an EAA that the relying party can validate itself are mutually exclusive by construction.

### G3. Tracking: the issuer learns about transactions after issuance

- Art. 5a(16)(a): the technical framework shall "not allow providers of electronic attestations of attributes or any other party, after the issuance of the attestation of attributes, to obtain data that allows transactions or user behaviour to be tracked, linked or correlated, or knowledge of transactions or user behaviour to be otherwise obtained, unless explicitly authorised by the user".
- Dock's issuer, and Truvera as "any other party", obtain the following for each verification: the schema, the verifier's identity and the time. The final limb of 5a(16)(a), "knowledge of transactions … otherwise obtained", does not require the user to be identified. Learning that a transaction took place at verifier V is knowledge of a transaction. Herd privacy is per issuer and per schema (Dock: "The Issuer needs to use the same schema for enough credentials"). With a small herd, timing makes it correlatable as well: one holder issued on Monday and one verification at a lender on Tuesday.
- The exception: "unless explicitly authorised by the user". Dock's consent covers the verification. The fee notice goes to the verifier, and nothing in Dock's material says the holder is told that the issuer will learn the verifier's identity. Even with a holder-facing disclosure, the EUDI Wallet provides no screen for authorising issuer knowledge. Under Art. 5a(5)(b) the wallet itself may not inform the issuer. So the exception would have to be implemented by the relying party, outside the certified wallet's user interface. Whether that counts as authorisation the technical framework "allows" is open. `[UNVERIFIED: no guidance or case law found]`
- The ARF, which has no consent exception:
  - `ZKP_07`: "A ZKP scheme SHALL NOT introduce any additional communication or information that could be used to track or link User activity during, before, or after proof generation." KVAC is an anonymous-credential (ZKP) scheme, and the paid unlock is exactly such an additional communication.
  - `VCR_15` (on revocation lists): a Relying Party Instance "SHOULD NOT request the relevant Attestation Status List … each time an attestation is presented", and should download it "at a time and from a location unrelated to the presentation". Paid unlocking is a contact at presentation time, and it carries the verifier's identity.
  - ARF §4.2.4: "measures are taken to prevent Users from being tracked by Relying Parties, PID Providers, or Attestation Providers."
  - Discussion Paper A §2.3 defines Attestation Provider linkability as the case where, "after it has received an attestation, a Relying Party notifies the Attestation Provider". Dock's protocol makes that notification mandatory and removes the holder correlator from it. In the threat model's own terms, the residual leak is the verifier and time, not the holder.
- **Result: conflicts.** The conflict is with the regulation unless the user explicitly authorises it, and with ARF `ZKP_07` in any case.
- To be fair to Dock: against relying parties, Dock is *better* than the EUDI baseline. BBS/KVAC proofs are unlinkable across verifiers, which serves ARF §2.3's "Prevent tracking by Relying Parties" and Art. 5a(16)(b) more strongly than salted-hash SD-JWT VC and mdoc do. For those formats, Discussion Paper A concedes that issuer linkability "cannot be technically prevented". Dock's leak is a different one: it runs toward the issuer.

### G4. Offline and proximity

- Art. 5a(4)(a) and 5a(5)(a)(iii) require presentation "online and, where appropriate, in offline mode". `EDP_10`'s note says proximity presentations "must be able to be done without an internet connection".
- Paid unlocking needs the issuer online for each verification, so a paid credential cannot be presented in proximity or offline. ARF §5.4.4 adds that W3C VCDM attestations "cannot be used in proximity presentations using [ISO/IEC 18013-5]".
- **Result: fails** for any use case where offline presentation is "appropriate", such as age checks at a counter or a mobile driving licence at a roadside stop. It is not decisive for remote-only use cases.

### Secondary points

- **Access gating.** EUDI's tool for "only these relying parties" is the embedded disclosure policy: Art. 5a(5)(e), CIR 2024/2979 Annex III, and `EDP_02` ("Authorised relying parties only"). It is advisory. Under `EDP_07` the wallet lets the user "deny or allow the presentation" whatever the policy's outcome. Dock's lock is a hard gate that makes the credential unverifiable to any registered relying party that has not paid, which overrides the holder's choice under Art. 5a(4)(a) ("under the sole control of the user"). The law does not expressly ban a hard gate for a non-qualified EAA, but G2 does.
- **Truvera as intermediary.** Art. 5b(10): "Intermediaries acting on behalf of relying parties shall be deemed to be relying parties and shall not store data about the content of the transaction." Truvera "tracks an anonymized record of all the verifications". Whether a record of schema, issuer, verifier and time is "content of the transaction" is open. A strict reading says it is. `[UNVERIFIED]`
- **Art. 45h(1).** EAA providers "shall not combine personal data relating to the provision of those services with personal data from any other services offered by them or their commercial partners." Billing records are holder-anonymous, so this is probably not engaged. It becomes engaged if a small herd makes them personal data.

## The "Verifiable Identification" post, claim by claim

| # | Dock's claim (verbatim) | Check | Verdict |
|---|---|---|---|
| V1 | VCs are "independently checkable without contacting the original issuing authority"; "the verifier does not need to contact the issuer" | True for signed SD-JWT VC and mdoc EAAs. **Contradicted by Dock's own paid product**, where "the issuer is asked to provide an additional secret" at every verification | Holds for EUDI formats; false for Dock's paid credentials |
| V2 | "Verifiable identification rests on two open standards published by the W3C": VCs and DIDs | The binding EAA formats are SD-JWT VC and mdoc (G1). W3C VCDM waits for ETSI profiles (CIR 2026/1731 recital 1). Issuer keys must sit in an X.509 certificate under TS 119 412-6 (`EAA-4.6.1-07`), not a DID | Wrong for EUDI |
| V3 | The check is "free from reliance on a centralized intermediary" | EUDI relies on registrars (Art. 5b), access and registration certificates and trusted lists. Dock's own paid model needs an ecosystem administrator and Truvera's billing hub | Wrong for both EUDI and Dock's paid product |
| V4 | "a KYC provider verifies a customer once and issues a verifiable identity credential … across multiple products and partner institutions" | Reliance on another firm's CDD falls under AMLR Arts. 48–49, open to obliged entities only, with the relying entity keeping responsibility ([critique.md](critique.md) C6) `[UNVERIFIED: AMLR text not re-read here]` | Holds partly |
| V5 | "credentials are not linked across different verifiers without the holder's consent" | True for BBS/KVAC proofs. For mdoc and SD-JWT VC in EUDI it depends on once-only batches (`ISSU_37`, method A) | Holds for Dock's format |
| V6 | mDL: an over-18 holder "can share only the yes/no result" | True: ISO 18013-5 `age_over_NN` data elements. Linkability across verifiers is not addressed, see V5 | Holds |
| V7 | Healthcare revocation is detected "immediately" | ARF `VCR_15` recommends cached status lists downloaded at intervals the relying party chooses, and `VCR_14` requires a risk analysis when status is stale | Overstated |
| V8 | "Issue once, verify everywhere, store nothing" | AML record-keeping requires relying parties to keep CDD records. ARF §7.4.3.5.1 asks relying parties to discard only the *unique fixed elements* | Overstated |
| V9 | Biometric-bound credentials "without centralizing or storing raw biometric data" | Dock's binding mechanism was not read. EUDI binds attestations to a WSCA/WSCD key, with user authentication done locally, and treats the portrait under CIR 2024/2977 Art. 3a (inserted by 2026/1731) | Untested |
| V10 | AI-agent credentials | Outside the EUDI Wallet, which serves natural and legal persons (Art. 5a(4)) | Not an eIDAS question |

## What a compliant version looks like

Dock's objective is that the issuer is paid when its attestation creates value for a relying party, while the holder stays private. Within the EUDI rules that objective can be met by moving the billing signal off the presentation path:

1. **Format and validation.** Issue SD-JWT VC or mdoc with an X.509-backed signature (G1, G2), presented over OpenID4VP/HAIP (`OIA_08`, `OIA_08a`) or ISO 18013-5. Any registered relying party can validate.
2. **Price access, not presentations.** Sell relying parties a flat or tiered licence to accept the attestation type. The licence is enforced contractually and listed in an `EDP_02` "authorised relying parties only" policy, so unlicensed relying parties trigger a warning to the user. The cost: `EDP_11` requires the issuer to revoke an attestation whenever its policy changes, so every new licensee forces re-issuance of the fleet. That makes the per-relying-party list expensive at scale. `EDP_03` (a specific root of trust) points at registration-certificate CAs that the issuer does not control.
3. **Metering, if needed, from the relying party's side.** The relying party self-reports volumes and is audited, much like the furnisher-pays-nothing, verifier-pays model of The Work Number ([carwash-objection.md](carwash-objection.md)). Aggregate monthly counts per relying party do not let anyone track a user. Whether they amount to "knowledge of transactions" under 5a(16)(a) is open, and a per-day or per-session granularity would bring it closer. `[UNVERIFIED]`
4. **Price issuance.** Charge per credential issued or per active holder, the model [critique.md](critique.md)'s steelman already reached.

What stays out of reach: an issuer that can *prove* each verification happened without learning it. No documented scheme does this (recon-summary.md, `[UNVERIFIED: absence]`). It would need something like the relying party paying into an issuer-blind accumulator, which is research, not a product.

## Consequences for this repo

- [critique.md](critique.md) C10 stands, but its legal basis is now Art. 5a(16)(a), ARF `ZKP_07`, ETSI `EAA-4.6.1-01` and CIR 2024/2977 Art. 4(3). Art. 5a(5)(b) does not carry it.
- Story 10.6 (PID-authenticated issuance of `por.1` as mdoc) is untouched. It already uses a listed format and a signature that verifiers check offline. It also confirms in passing that the EUDI-native path for derived credentials is signed mdoc or SD-JWT VC, not KVAC.

## Open

- Whether "explicitly authorised by the user" in Art. 5a(16)(a) can be satisfied by a consent the relying party collects outside the certified wallet's interface. Look for Commission, EDPB or Cooperation Group guidance.
- Whether Truvera's paid verification works with SD-JWT VC or mdoc at all, or only with Dock's own BBS/KVAC W3C credentials. Its OID4VP integration guide (recon [31]) documents verification without a paid path.
- A consolidated text of CIR 2024/2977, 2024/2979 and 2024/2982 after 2026/1731, to confirm no later amendment.
- Dock's biometric-binding mechanism (V9).

## How this was done (/be-ambitious)

- Goal beyond the goal: decide whether a per-verification revenue model can survive inside the EUDI Wallet, and if not, what the nearest compliant model is. This feeds the derived-credentials decision behind Story 10.6.
- Depth: start from the raw legal text, meaning the OJ articles, the amending CIR, the ETSI TS it makes binding and the ARF HLR CSV at a pinned commit, not summaries of them. Then check Dock's mechanism, as Dock's own documentation describes it, against each one.
- Judge: every verdict above cites a verbatim provision, and every inference is marked. A reader can check any line against the linked text. The hard target was a verdict per ground, with no ground resting on a summary of the law. G1–G4 meet it. 5a(16)(a)'s consent exception and 5b(10) remain interpretive and are marked as such.
