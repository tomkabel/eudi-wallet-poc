# Critique of the Dock Labs "issue, don't just verify" post

Date: 2026-11-06. Prompt: [critique-prompt.md](critique-prompt.md). Evidence base: [carwash-objection.md](carwash-objection.md) ("the memo"). No primary text outside the memo was read in this session, so every citation the memo does not contain is marked [UNVERIFIED].

<claims>

- C1 (factual): Most companies view EUDI wallets only as a way to verify customers.
- C2 (commercial): The bigger value lies in the new credentials companies issue, not in verification.
- C3 (factual): A telco can use EUDI identity verification to issue an account-holder credential.
- C4 (factual): An employer can issue credentials for employment, role, or authority to act for the company.
- C5 (factual): Other businesses can independently verify who issued these credentials and whether they are valid.
- C6 (factual): Relying parties could rely on these credentials instead of re-collecting documents, "where the applicable rules allow it".
- C7 (predictive): Once hundreds of millions of people hold EUDI wallets, thousands of companies will move from verifying to issuing.
- C8 (predictive): Banks, employers, insurers, telecoms and platforms in particular will issue.
- C9 (commercial, implied): Issuers have an incentive to hand customers portable relationship facts that other businesses can rely on.
- C10 (commercial, implied): A vendor (Dock Labs) can build a viable business by enabling this issuance.
- C11 (factual, implied): An EUDI identity check is enough to found the trust that third parties place in a derived credential.

</claims>

<quotes>

- Q1 (C1): memo silent.
- Q2 (C2, C9): "A relationship fact is worth issuing only if someone other than the issuer has a decision that depends on it."
- Q3 (C9, C7, C8): "Businesses voluntarily share relationship facts with third parties in two settings only: with rivals inside give-to-get reciprocity clubs where each contributor also consumes the pool (credit bureaus, claims databases), and even there dominant holders withhold or must be mandated; and with non-rivals when an intermediary charges the verifier and takes a verification cost off the issuer (The Work Number, telco KYC Match). In every case found, the fact flows business-to-hub under the hub's contract. No case was found of an issuer voluntarily handing the customer a portable fact that rivals can rely on, which is the model the post describes."
- Q4 (C7, C8, C9): "After nine and a half years in which any organisation could register as an issuer, no voluntary business-issued relationship credential is relied on by a different business."
- Q5 (C7, C8): "Banks did not issue. Bank facts enter the wallet through the scheme operator, via iDIN (the banks' paid identity service) or a test payment."
- Q6 (bounds Q4/Q5): "Yivi is small and Dutch; unregistered requestors can verify, so demand is undercounted; the EUDI wallet will have far more holders and a statutory acceptance duty (Regulation (EU) 2024/1183 Art. 5f(2)–(3))."
- Q7 (C3): "Telecoms | subscription, tenure | none | ESS § 28¹ holds numbers and porting only; § 111¹(2) subscriber data for authorities only | — | no state source; commercial channel is CAMARA KYC Match, per-operator contract"
- Q8 (C4, C6): "Employers | employment | töötamise register | MKS § 25¹; tax secret, disclosure with written consent (MKS § 26(2)) | the employee, via an e-MTA tax-data certificate | state source" and "authority to act for a company | äriregister | ÄRS § 30 (public) … | anyone; ≤ €2 per query, current card free (ÄRS § 32) | state source"
- Q9 (C4, C6, C8): "Where the state already holds the fact (employment, authority to act, motor insurance in Estonia), a business credential duplicates a free or near-free authentic source, and eIDAS 2 Art. 45e routes qualified attestations to that source."
- Q10 (C8): "The EU's own list of attributes that must be verifiable against public authentic sources (eIDAS 2 Annex VI, via Art. 45e) covers mandates (item 9), professional qualifications (item 8) and company data (item 11). It omits bank accounts, employment status, insurance, telecom subscriptions and platform accounts: the EU itself leaves those to private sources."
- Q11 (C10): "The hub model the evidence supports is the one the wallet rules out: the wallet SHALL NOT inform attestation providers of attestation use (Art. 5a(5)(b)) and SHALL prevent tracking by attestation providers after issuance (Art. 5a(16)(a)) … Dock's own documentation describes an issuer that "sees that a credential … was shared with a specific Verifier" (recon-summary.md). Pay-per-verification through the wallet therefore collides with the regulation."
- Q12 (C9): "Rival sharing holds only inside give-to-get clubs" and "Dominant holders defect: "a few large creditors decided to suspend reporting [credit limits] for competitive reasons" (Fed 2003)."
- Q13 (C9, steelman): "Equifax The Work Number, 4 million+ employers furnishing payroll data free because it "relieves them of the administrative burden and expense of responding to third-party employment verification requests"; verifiers pay"
- Q14 (C5): memo silent on the cryptographic mechanics; Q11 bears on what the issuer learns after issuance.
- Q15 (C6, C11): memo silent on AMLR.
- Q16 (C3, telco): "telco tenure for a lender's fraud check (already sold via KYC Match, so the credential competes with an API)"
- Q17 (C8): "Count: 1 of 5 sectors has a state source for the fact the post names (employment and representation), 2 are partial (banks, insurance), 2 have none (telecoms, platforms)."
- Q18 (limits): Open, [UNVERIFIED]: "whether the employment register records job title; whether the e-MTA certificate is free; … telco KYC Match commercial volumes."

</quotes>

<critique>

Worst flaw first.

1. C9, issuer incentive. **Fails, on the evidence available.** The post never says why an issuer would hand a customer a portable fact that rivals can rely on, and that is the whole thesis. The memo's best evidence shows voluntary sharing only into hubs that either bind rivals to give-to-get reciprocity or charge the verifier (Q3, Q12, Q13), and Yivi has had nine and a half years with no voluntary cross-business relationship credential (Q4). The verdict leans on v2's last sentence, which rests on absence of evidence: "no case was found" is not proof that none exists, and Yivi undercounts demand (Q6).

2. C10, the implied business model. **Fails, as far as it depends on per-verification revenue.** The model the evidence supports, where the hub sees each verification and bills for it, is the one the wallet forbids: it must not tell attestation providers about use (Art. 5a(5)(b)) and must prevent provider tracking after issuance (Art. 5a(16)(a)) (Q11). Dock's own documentation, as summarised in recon-summary.md, describes an issuer that sees which verifier received a credential (Q11). That file was not supplied here, so the quote is second-hand. The post states no pricing, so a model that charges issuers per credential, or verifiers per trust-registry lookup, stays untested. For the short seller's question, the issuer-side revenue story does not survive the regulation; a flat issuance-SaaS story might.

3. C6 and C11, reliance replacing re-verification. **Holds partly, and fails for the AML use case.** The memo is silent on AMLR (Q15), so this rests on primary text not read in this session. AMLR Art. 22 sets out how identity is verified and, as I understand it, names EUDI wallets and qualified trust services as acceptable means [UNVERIFIED: Regulation (EU) 2024/1624 Art. 22(6)]. Arts. 48–49 let an obliged entity rely on another obliged entity's CDD, while the relying entity keeps final responsibility and must be able to get the underlying data on request [UNVERIFIED: AMLR Arts. 48–49]. A telco or an ordinary employer is not an obliged entity, so its credential cannot be a reliance basis. It is at most an unverified data point, and the derived credential adds nothing over presenting the PID itself (Art. 22). A bank-issued credential could fall under Art. 48, but then the bank must hand over CDD data on request, which a wallet-held credential does not provide [UNVERIFIED]. The post's own hedge, "where the applicable rules allow it", carries most of the weight here.

4. C7 and C8, thousands of issuers across five sectors. **Fails as stated for Estonia; untestable EU-wide.** In 1 of 5 sectors the state already holds the fact (Q17). For employment and authority to act, a business credential duplicates a free or near-free authentic source that Art. 45e points qualified attestations to (Q8, Q9). Banks did not issue in Yivi (Q5). The memo's Limits paragraph (Q6) bounds this: the EUDI wallet's scale and the acceptance duty in Art. 5f(2)–(3) may change the outcome, which is why the EU-wide prediction is untestable rather than false. Whether the employment register records job title, which bears on "role", is open (Q18).

5. C2, issuing beats verifying. **Holds partly.** It holds only where the three filters pass (see the steelman); the carwash test (Q2) shows that most relationship facts have no third-party decision depending on them. Nothing in the memo sizes the issuing market against the verifying market, so "bigger" is an assertion.

6. C3, the telco example. **Holds technically; fails commercially for the product owner.** No state source for subscription or tenure exists, so the telco really is the only source (Q7). But the operator already sells that fact per query through KYC Match under a per-operator contract (Q7, Q16). A wallet credential that it cannot observe or bill after issuance (Q11) replaces a paid query with a free one. The answer to the product owner is cannibalisation unless issuing cuts a cost greater than lost query revenue. KYC Match volumes are open (Q18), so the size of that cannibalisation is unknown.

7. C4, the employer example. **Holds technically; weak where a register exists.** Employers can issue such credentials, but in Estonia employment and representation already come from the töötamise register and äriregister (Q8). Whether the e-MTA certificate is free is open (Q18). The example is stronger outside Estonia, where no register is open (memo, "Where the post's opportunity actually is").

8. C5, independent verification. **Holds, at the level the post states it.** Issuer authentication and validity checks are what the EUDI attestation rules provide for, including relying-party checks against trust lists [UNVERIFIED: Regulation (EU) 2024/1183 Arts. 45b and 45h for (Q)EAA issuers]. The memo is silent on mechanics (Q14). The constraint that matters commercially is Q11, not the cryptography.

9. C1, "most companies look at it the wrong way". **Untestable.** No survey is cited, and the memo is silent (Q1).

</critique>

<steelman>

The narrowest market that passes all three filters is **proof of non-motor insurance cover presented to a non-insurer relying party**: travel medical insurance for a Schengen visa application, or home-contents and tenant-liability cover shown to a landlord.

- Filter 1, the business is the only source: Estonia has a register for motor cover only; "home, life, travel have no register" (memo, Test 2, insurance row). The insurer's own policy record is the only authoritative source of travel or home cover.
- Filter 2, the relying party is not the issuer's rival: a consulate (or its visa contractor) checking that the applicant holds travel medical cover, or a landlord checking tenant liability cover, sells no insurance and gains nothing competitive from the fact.
- Filter 3, the issuer pays today to answer verification requests: insurers issue cover certificates and handle confirmation calls from consulates and landlords. That they bear a measurable cost per request is an inference by analogy with The Work Number (Q13), not something the memo shows [UNVERIFIED].

Who pays: the insurer, at issuance. Issuing replaces the cost of answering confirmations, which is the Work Number logic without the hub, because the wallet bars the insurer from seeing or billing each presentation (Q11). The relying party verifies free. A vendor in this market sells issuance tooling to insurers, flat or per credential issued, never per verification.

</steelman>

<open>

- AMLR Arts. 22, 48, 49 (C6, C11): read the Official Journal text (CELEX 32024R1624) for whether Art. 22 names EUDI wallets, whether Art. 48 limits reliance to obliged entities, and whether Art. 49 requires CDD data to be supplied on request. If non-obliged-entity attestations can count as a reliable independent source, the C6 verdict moves to "holds partly" for AML.
- **Closed 2026-11-07 in [dock-eudi-compliance.md](dock-eudi-compliance.md):** C10 stands, but on Art. 5a(16)(a), ARF `ZKP_07`, ETSI TS 119 472-1 `EAA-4.6.1-01` and CIR 2024/2977 Art. 4(3), not on 5a(5)(b), which binds only the wallet. Original item: eIDAS 2 Arts. 5a(5)(b), 5a(16)(a), 45b, 45h (C5, C10): read the EUR-Lex text of Regulation (EU) 2024/1183. If unlinkability binds only the wallet and not issuer-run status or revocation endpoints, a metered model may survive and C10 moves to "holds partly".
- Dock's model (C10): read Truvera's pricing page and the documentation behind recon-summary.md. Check whether the issuer-sees-verifier feature works with EUDI-format credentials (SD-JWT VC, mdoc) or only with Dock's own format.
- KYC Match economics (C3): get operator price lists or volumes from CAMARA members or Open Gateway reports. Small volumes weaken the cannibalisation objection.
- A counterexample to v2's last sentence (C9): search for any issuer that gives customers a portable fact rivals rely on outside a hub, such as a bank reference letter accepted by competing lenders, or SheerID's and airline status-match sources, which the memo has not tested. One documented case turns C9 from "fails" to "holds partly".
- Employment register scope (C4, C8): check whether the töötamise register records job title (MKS § 25¹ and its implementing regulation) and whether the e-MTA certificate is free. If job title is not recorded, "role" credentials pass filter 1 in Estonia.
- Steelman filter 3: get consulate or landlord confirmation volumes and insurer handling costs. Without them, the "who pays" logic rests on analogy.
- Post-launch census: rerun the Yivi-style count on EUDI trust lists once registered (Q)EAA providers are published. That is the direct test of C7.

</open>
