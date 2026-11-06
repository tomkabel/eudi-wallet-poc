# The carwash objection, tested

Date: 2026-11-06. Companion to [forge-brief.md](forge-brief.md) and [recon-summary.md](recon-summary.md).

## The post and the objection

A Dock Labs LinkedIn post (early November 2026) argues that once a customer proves identity with an EUDI wallet, banks, employers, insurers, telecoms and platforms will issue their own relationship credentials (account holder, employment, role, authority to act) that other businesses verify, and that "thousands of companies" will move from verifying to issuing.

The intuition behind the objection: a carwash can issue a perfectly verifiable loyalty credential and Toyota still does not care. A relationship fact is worth issuing only if someone other than the issuer has a decision that depends on it.

## Claim as first stated (v1)

1. A relationship fact's value to third parties correlates with the issuer's incentive *not* to issue it, because the valuable facts are the ones that lower switching costs or help rivals.
2. The only counterexamples are compelled by regulation.
3. In Estonia the authoritative source for most of the post's examples (employment, mandate, insurance, account ownership) is already a state register or a mandated scheme.

Pass criteria set in advance: every claim cites a primary source; the census has real counts; the objection survives the strongest counterexample or is revised to fit it.

## Test 1: census of a live attribute wallet (Yivi, Netherlands)

Source: the signed production scheme `privacybydesign/pbdf-schememanager`, commit `f360419fb9998403bda57dffafc4e62183bcc376` (6 Nov 2026); repository created 2017-04-10. Requestor list: `privacybydesign/pbdf-requestors` (30 registered requestors). Method: every `Issues/*/description.xml` read and classified by hand.

16 issuers, 52 credential types (14 deprecated).

| Class | Issuers | Credentials |
|---|---|---|
| Government | 5: `gemeente`, `nijmegen`, `pilot-amsterdam`, `bzkpilot`, `minvws-cibg` | address, personal data, ID/passport data, UZI pilot |
| Scheme operator acting as bridge | 2: `pbdf`, `sidn-pbdf` | passport, ID card, driving licence, age, BIG, VOG, IBAN (via a micro-payment), bank-held identity (via iDIN), email, phone |
| Register-derived | 3: `signicat` (KVK trade register), `nuts` (AGB healthcare code), `chipsoft` (BSN) | authority to act for an organisation, provider codes |
| Regulation-compelled | 1: `cis` | "Digital Insurance Passport": a sanctions and incident-register check that its own description calls "mandatory by law" for insurers |
| Education | 1: `surf` | SURFsecureID, SURFdrive licence |
| Voluntary commercial | 4: `vgz`, `ivido`, `PubHubs`, `gebiedonline` | all closed-loop: the issuer is the only relying party (VGZ's `machtiging` lets a person submit claims *to VGZ* for another VGZ customer) |

Findings:

- After nine and a half years in which any organisation could register as an issuer, no voluntary business-issued relationship credential is relied on by a different business.
- Banks did not issue. Bank facts enter the wallet through the scheme operator, via iDIN (the banks' paid identity service) or a test payment.
- What crosses organisations is register-derived or legally compelled.

Limits: Yivi is small and Dutch; unregistered requestors can verify, so demand is undercounted; the EUDI wallet will have far more holders and a statutory acceptance duty (Regulation (EU) 2024/1183 Art. 5f(2)–(3)).

## Test 2: authoritative sources for the post's five sectors

All citations checked against Riigi Teataja or EUR-Lex text.

| Sector | Fact | Authoritative source | Law | Who can query | Verdict |
|---|---|---|---|---|---|
| Banks | account holder | täitmisregister (account holders and authorised users) | RahaPTS § 81(1)–(1²); TMS § 63; AMLD5 Art. 32a (CELEX 32018L0843) | public authorities only (TMS § 63(2)) | partial |
| | name matches IBAN | Verification of Payee | Reg. 260/2012 Art. 5c, inserted by Reg. 2024/886 (CELEX 32024R0886); in force for euro-area PSPs 9 Oct 2025 (Art. 5c(9)); free (Art. 5b(2)) | any payer, at payment time | |
| Employers | employment | töötamise register | MKS § 25¹; tax secret, disclosure with written consent (MKS § 26(2)) | the employee, via an e-MTA tax-data certificate | state source |
| | authority to act for a company | äriregister | ÄRS § 30 (public); ÄS § 181, § 307 (board), §§ 16–17 (procura) | anyone; ≤ €2 per query, current card free (ÄRS § 32) | state source |
| Insurers | motor cover and claims history | LKF register | LKindlS § 72; public plate query § 77(3); 5-year claims-history certificate within 7 days § 76(4)–(6); Directive 2009/103 Art. 16 as replaced by Directive 2021/2118 Art. 1(15) | anyone (cover); policyholder (history) | partial: motor only; home, life, travel have no register |
| Telecoms | subscription, tenure | none | ESS § 28¹ holds numbers and porting only; § 111¹(2) subscriber data for authorities only | — | no state source; commercial channel is CAMARA KYC Match, per-operator contract |
| Platforms | account, reputation | none | DMA Art. 6(9)–(10) export rights for gatekeepers; GDPR Art. 20; DAC7 seller statements (CELEX 32021L0514) are tax statements | — | no state source |

Count: 1 of 5 sectors has a state source for the fact the post names (employment and representation), 2 are partial (banks, insurance), 2 have none (telecoms, platforms).

The EU's own list of attributes that must be verifiable against public authentic sources (eIDAS 2 Annex VI, via Art. 45e) covers mandates (item 9), professional qualifications (item 8) and company data (item 11). It omits bank accounts, employment status, insurance, telecom subscriptions and platform accounts: the EU itself leaves those to private sources.

Leg 3 of v1 holds for employment and authority to act, partly for banks and insurance, and fails for telecoms and platforms.

## Test 3: the strongest voluntary commercial counterexample

**US consumer credit furnishing** refutes legs 1 and 2 as stated.

- Voluntary: "Reporting … is voluntary and historically has been" ([CFPB 2012](https://files.consumerfinance.gov/f/201212_cfpb_credit-reporting-white-paper.pdf)); "no state or federal law requires creditors … to report" ([Federal Reserve Bulletin 2003](https://www.federalreserve.gov/pubs/bulletin/2003/0203lead.pdf)); 15 U.S.C. 1681s-2 governs accuracy for those who furnish, not a duty to furnish.
- Scale: about 10,000 furnishers, 1.3 billion accounts a month (CFPB 2012).
- Rivals use it against the furnisher: on-time payers land on prescreened lists, "providing these borrowers with account offers from competing lenders" (CFPB 2012).

Runner-up: **Equifax The Work Number**, 4 million+ employers furnishing payroll data free because it "relieves them of the administrative burden and expense of responding to third-party employment verification requests"; verifiers pay ([Equifax FY2025 10-K](https://www.sec.gov/Archives/edgar/data/33185/000003318526000010/efx-20251231.htm)). It breaks leg 1 differently: when the relying party is not a rival, a valuable fact is shared because sharing saves the issuer money.

What survives from leg 1, as a tendency:

- Dominant holders defect: "a few large creditors decided to suspend reporting [credit limits] for competitive reasons" (Fed 2003).
- Voluntary positive-data sharing in Australia missed its 40% target and was mandated ([Treasury explanatory materials 2019](https://treasury.gov.au/sites/default/files/2019-08/c2019-t401119-explanatory-materials.pdf)).
- Rival sharing holds only inside give-to-get clubs: "organisations can only access the level of credit data that they themselves contribute" ([UK Principles of Reciprocity](https://www.cigb.co.uk/resources/principles-of-reciprocity/)).

## Claim as revised (v2)

> Businesses voluntarily share relationship facts with third parties in two settings only: with rivals inside give-to-get reciprocity clubs where each contributor also consumes the pool (credit bureaus, claims databases), and even there dominant holders withhold or must be mandated; and with non-rivals when an intermediary charges the verifier and takes a verification cost off the issuer (The Work Number, telco KYC Match). In every case found, the fact flows business-to-hub under the hub's contract. No case was found of an issuer voluntarily handing the customer a portable fact that rivals can rely on, which is the model the post describes.

Two further constraints narrow the post's market:

- Where the state already holds the fact (employment, authority to act, motor insurance in Estonia), a business credential duplicates a free or near-free authentic source, and eIDAS 2 Art. 45e routes qualified attestations to that source.
- The hub model the evidence supports is the one the wallet rules out: the wallet SHALL NOT inform attestation providers of attestation use (Art. 5a(5)(b)) and SHALL prevent tracking by attestation providers after issuance (Art. 5a(16)(a)); see [spec §2.1](../../../spec/EE-EUDIW-TS-1.0.md). Dock's own documentation describes an issuer that "sees that a credential … was shared with a specific Verifier" (recon-summary.md). Pay-per-verification through the wallet therefore collides with the regulation.

## Where the post's opportunity actually is

The facts that pass all three filters: the business is the only source; the relying party is not the issuer's rival; the issuer today pays to answer verification requests. Examples: non-motor insurance cover shown to a landlord, lender or visa office; employment and income where no state register is open (outside Estonia); telco tenure for a lender's fraud check (already sold via KYC Match, so the credential competes with an API). The carwash fails the third filter: nobody asks.

## Pass/fail against the judge

- Primary sources: met, except the items below.
- Census with real counts: met (16 issuers, 52 credentials, 30 requestors, pinned commit).
- Survives best counterexample: v1 did not; v2 is the revision that does. Its last sentence rests on absence of evidence.

Open, [UNVERIFIED]: whether the employment register records job title; whether the e-MTA certificate is free; statutory basis of Pääsuke; who can query Tervisekassa coverage; LexisNexis C.L.U.E. contribution being free of statutory duty; telco KYC Match commercial volumes. Not yet tested: Nordic BankID (identity, not a relationship fact), airline status match, SheerID's data sources.
