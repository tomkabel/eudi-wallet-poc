# Landscape extract — external state of play for the EUDI Wallet / ZK age-proof PoC

**Compiled:** 23 October 2026, ~04:50–05:40 EEST (machine clock: `Fri Oct 23 01:46 UTC 2026`).
**Scope:** the EU Age Verification Profile, the EUDI Wallet ecosystem, ZK age proofs in the
wild, Estonia, and the delta against `docs/research/2026-10-23-shared-appendices.md`.
**Method:** `web_search`/`web_extract` plus GitHub REST via `gh api` for repository facts
(commit dates, release tags, file contents). Local firecrawl (`http://asus:3002/v1/search`,
`/v1/scrape`) used where search returned empty.

**Reading notes.** "Primary" = the publisher's own artefact (spec text, repo commit, EUR-Lex,
regulator page, trust-list XML). "Secondary" = press, vendor blog, aggregator.
`[UNVERIFIED]` = could not open or could not confirm. Figures from this repo's own docs are
labelled **per the fork's docs** and are not treated as independent evidence.

---

## E1. The EU Age Verification Profile (`eu.europa.ec.av.1`)

### E1.1 Status and who maintains it

| fact | evidence | date | type |
|---|---|---|---|
| The AV technical specification is a European Commission artefact, published as docs + portal; repo `eu-digital-identity-wallet/av-doc-technical-specification` (EUPL-1.2, 99★, org "European Age Verification solution documentation") | `https://github.com/eu-digital-identity-wallet/av-doc-technical-specification` (GitHub API: `created_at 2025-03-26`, `pushed_at 2026-09-02`) | read 2026-10-23 | primary |
| Current revision: release branch `release/1.1.0` merged 2026-09-02; the normative files (Architecture, AV Profile, Annex B, Threat Model, diagrams) were uploaded in the 2026-09-01 commits. The previous release was 1.0.9 (2026-03-19). **The public portal labels the current revision "What's new · August 2026"; the exact published version string was not observed** `[UNVERIFIED]` | repo commit feed; `https://ageverification.dev/` | 2026-09-02 / 2026-09-01 | primary |
| Structure: `docs/architecture-and-technical-specifications.md` (the OSPSA = Operational, Security, Product, Architecture specification), `docs/annexes/annex-A/annex-A-av-profile.md`, `docs/annexes/annex-B/annex-B-zkp.md`, root `AV-threat-model.md` | repo contents listing | read 2026-10-23 | primary |
| **A Threat Model now exists** (`AV-threat-model.md`, added in the same 2026-09-01 batch) | repo commit "Threat model" 2026-09-01T21:56Z | 2026-09-01 | primary |
| The AV app itself ("mini wallet") is built under contract by Scytáles + T-Systems; blueprint published 14 Jul 2025, "feature ready" 15 Apr 2026 | `https://digital-strategy.ec.europa.eu/en/policies/eu-age-verification`; `https://www.biometricupdate.com/202604/breaking-down-the-european-commissions-white-label-age-verification-app` | 15 Apr 2026 (vendor identity: secondary) | mixed |

### E1.2 Normative content that matters to this fork

**(a) Data model.** "The document type for Proof of Age attestation SHALL be `eu.europa.ec.av.1`"
— a profile of ISO/IEC 18013-5 + ISO/IEC 23220-2 attribute schemas
(`annex-A-av-profile.md`; also served at
`https://ageverification.dev/av-doc-technical-specification/docs/annexes/annex-A/annex-A-av-profile/`, both read 2026-10-23, primary).

**(b) Presentation mechanism — ZKP is now the mandated default.** AV Profile §A.5:

> "The preferred mechanism is a Zero-Knowledge Proof defined in Section A.8. A plain ISO mDoc
> presentation SHALL be used as the fallback where, and only where, either (a) the User's device
> does not support Zero-Knowledge Proof generation, or (b) **the OpenID for Verifiable
> Presentations transport is used**."

Consequence, stated in the same document: over OpenID4VP the profile expects plain mDoc, because
"no standardized DCQL query for requesting Zero-Knowledge Proof is available". ZKP presentation is
specified over the **W3C Digital Credentials API** (ISO/IEC 18013-7 Annex C).
(primary; text read 2026-10-23)

**(c) The ZK system is named, and it is Longfellow.** AV Profile §A.8:

> "Where the User's device provides the necessary platform support, the AVI SHALL support the
> generation of Zero-Knowledge Proofs using the solution detailed in: 'Matteo Frigo and abhi shelat,
> Anonymous credentials from ECDSA, IACR Communications in Cryptology, vol. 3, no. 1, 2026' …
> For this version of the profile, **the only Zero-Knowledge Proof system in scope is the system
> identified by `longfellow-libzk-v1`** … using the circuit and parameters identified by the
> ZkSystemSpec parameters (`circuit_hash`, `version`, `num_attributes`, `block_enc_hash`,
> `block_enc_sig`) as defined in ISO/IEC DIS 18013-5 (Second Edition), Section 10.2.7. **An
> implementation SHALL use that system. Support for any other Zero-Knowledge Proof system does not
> constitute conformance with this profile.** … The set of circuits accepted for the purposes of
> this profile, identified by their circuit hashes, **is published and maintained by the scheme
> owner separately from this document.**"

Proof statement the circuit must establish (all four): AP signature verifiable; the requested
attribute present **and true**; the AVI can sign the nonce verifiably with the key inside the
attestation; the attestation is within its validity period. RP duties: verify the circuit hash
against the accepted set **before** verifying the proof, and reject non-accepted circuits; an RP
SHALL also be able to verify the plain mDoc fallback — and SHALL NOT reject a presentation merely
because it used the fallback. (primary)

**(d) Concrete parameter tuple appears in the profile's own example.** AV Profile §A.11
("W3C Digital Credentials API with ZKP") shows
`"zkSystemId": "longfellow-libzk-v1_6_1_4096_2945_137e5a75ce72735a37c8a72da1a8a0a5df8d13365c2ae3d2c2bd6a0e7197c7c6"`
i.e. version 6, `num_attributes` 1, `block_enc_hash` 4096, `block_enc_sig` 2945.
Note the divergence: Google's own `zk-system-spec.md` example uses
`"block_enc_hash": 4025` with the same `circuit_hash` and `version`. Two published examples, two
different block-enc parameters — exactly the "which circuits do we accept" ambiguity the fork's
roadmap item O4 targets. (both primary, read 2026-10-23)

**(e) Lifecycle rules (OSPSA §3.4).** Plain mDoc attestations are single-use and issued in
**batches (30 recommended)**; ZKP presentations MAY be reused within validity; validity
recommended ≤ **3 months**; re-identification at least every 3 months; **attestation revocation is
not required** for age verification; `ValidityInfo` timestamps should be coarsened (`hh`/`mm`/`ss`
equal) because they leak linkability. (primary)

**(f) Trust anchor.** AV TL is a trusted list under **eIDAS Article 22**, published on the eIDAS
Dashboard, ETSI **TS 119 612**. APs must use a document-signer cert per ETSI EN 319 411-1 NCP from a
QTSP or run their own NCP trust-anchor CA. Trust in AV apps is anchored **at issuance**, via the
"AVS Unit Attestation" (AVSUA) or equivalent; **registration of RPs and of AV app providers is not
required**. The envisaged **EU Age Verification Scheme** will hold a trusted list of AV solution
providers; **until then, validation is anchored in certificates lodged with the Commission and
published in the list of compliant Age Verification Apps** — an explicitly interim arrangement.
(OSPSA §2.6, §3.4.4; primary)

**(g) Threat model requirement.** Before issuing, the AP must verify the requesting app is a
genuine, untampered instance of a compliant AV solution, "using the AVS Unit Attestation mechanism
or an internal mechanism of equivalent assurance", explicitly to defeat repackaged apps, emulator
farms and "large-scale onward supply of valid attestations to minors". (OSPSA §3.4.4; primary)

### E1.3 The Commission technical specifications (TS4 / TS13 / TS14)

| spec | current version | date | state | evidence |
|---|---|---|---|---|
| TS4 — ZKP implementation in EUDI Wallet | **v1.0.1** | 2026-01-30 | final-then-editorial; references Topic G discussion paper v1.4 | `…/docs/technical-specifications/ts4-zkp.md` version table (primary) |
| TS13 — ZKPs based on arithmetic circuits | **v1.0.1** | 2026-01-30 | **explicitly exploratory**: "captures exploratory work … will be tested in the Age Verification. It is not intended to be a final specification. Instead, it will be handed over to ETSI for further refinement … under ETSI TS 119 476-2" | `ts13-zksnarks.md` note + version table (primary) |
| TS14 — ZKPs from multi-message signatures | **v1.0** | 2026-02-27 | same handover note to ETSI TS 119 476-2 | `ts14-zkps-from-mms.md` (primary) |
| ETSI DTS 119 476-2 track | exists as a tracked item in the EC repo | issue opened 2026-01-01, still open | `gh search issues "ETSI DTS 119 476-2"` (primary) |

The last commits to TS4/TS13/TS14 were Feb–Mar 2026 (licensing/editorial), i.e. **none of the three
ZKP specifications has moved since winter 2026**, while the AV Profile moved in September 2026.

### E1.4 What remains open (as of 2026-10-23)

- The **accepted-circuit set is not in the profile**; it is deferred to "the scheme owner" and was
  not found published anywhere in this pass `[UNVERIFIED]`.
- The **EU Age Verification Scheme** and the **list of compliant AV apps / list of trusted AV
  solution providers** are announced but not observed. OSPSA calls the current certificate-lodging
  arrangement interim.
- The AV Profile's own out-of-scope list: level of assurance *high*, re-issuance and
  revocation mechanisms, RP registration/trusted lists, trusted lists of AV app providers,
  proximity presentation.
- TS13/TS14 are exploratory and headed to ETSI — i.e. the wallet-side ZKP normative home is not
  yet a finished standard.

---

## E2. The EUDI Wallet ecosystem

### E2.1 Specification and legal state

| fact | evidence | date | type |
|---|---|---|---|
| **ARF v3.0.0 is still the latest release**; released 2026-07-23. Prior: v2.9.0 (2026-05-21), v2.8.0 (2026-02-02). Next artefact in the release feed is **`Topic_AA_rr_paper_v0.8` (2026-10-14)**, a discussion artefact, not an ARF revision | `gh api repos/…/eudi-doc-architecture-and-reference-framework/releases` | read 2026-10-23 | primary |
| ARF v3.0.0 aligns with the amending implementing regulations and introduces the **Functional Conformance Assessment Framework (FCAF)** | releases feed + `https://www.biometricupdate.com/202607/conformance-framework-for-eu-digital-identity-wallets-arrives-with-arf-v3-0` | 2026-07-23 | primary + secondary |
| Amending CIRs of **15 July 2026**, OJ 22.7.2026, in force: **CIR (EU) 2026/1730** amending CIR (EU) 2025/848 (RP registration; swaps NCP-for-ETSI-EN-319-411-1 requirement for **ETSI TS 119 475 V1.2.1 (2026-03)**, adds a reference to the **national wallet-relying party register**); **CIR (EU) 2026/1735** amending CIR (EU) 2025/1569 (applicable standards; adds **ETSI TS 119 478 V1.1.1 (2026-01)** for authentic-source interfaces) | `https://data.europa.eu/eli/reg_impl/2026/1730/oj`, `…/2026/1735/oj` | 2026-07-15 | primary |
| A third amendment, **CIR (EU) 2026/1731**, is reported as amending CIR 2024/2977 (PID/EAA). Verified only via the ARF release note text surfaced in search — **not opened directly** `[UNVERIFIED]` | ARF release notes snippet | 2026-07-15 | secondary |
| **Certification is two-phase**: national transitory schemes now (mandated by **CIR 2024/2981**, adopted 28 Nov 2024); a harmonised CSA scheme later (ENISA candidate scheme requested; only **EUCC** available today; EUCS/EU5G pending). Wallet Solutions are **always certified to assurance level High**; composite/top-level certificate over component certifications; **FCAF covers functional conformance only** — not performance, scalability, penetration or availability | `https://eudi.dev/3.0.0/main/07-wallet-solution-certification-and-risk-management/` | ARF v3.0.0 (2026-07-23), read 2026-10-23 | primary |
| FCAF's authoritative source is `https://conformance.eudi.dev/` (the `/latest/introduction/` path 404s; versioned path `0.0.7` exists) | `https://conformance.eudi.dev/0.0.7/introduction/` | read 2026-10-23 | primary |
| **No ZKP mention in the reference wallet's Android core README** — the EC reference wallet does not advertise ZKP support | `gh api …/eudi-lib-android-wallet-core/contents/README.md` grep | read 2026-10-23 | primary (negative) |

### E2.2 Reference implementation releases (GitHub API, read 2026-10-23)

| repo | latest release | date | stars | note |
|---|---|---|---|---|
| `eudi-app-android-wallet-ui` | `Wallet/Demo_Version=2026.10.43-Demo_Build=43` | 2026-10-02 | 229 (per fork's docs) | demo build, not a product |
| `eudi-lib-android-wallet-core` | v0.31.0 | 2026-10-02 | 64 | pre-1.0 |
| `eudi-app-ios-wallet-ui` | (in the AV family release rhythm) | — | 90 (per fork's docs) | — |
| `eudi-app-multiplatform-verifier-ui` | `2026.05.6-Public` | 2026-05-28 | 15 | official multiplatform verifier UI |
| `eudi-lib-android-verifier-core` | v0.1.0 | 2025-09-29 | 4 | ISO 18013-5 verification SDK |
| `eudi-wallet-reference-implementation-roadmap` | — | repo untouched since 2024-10-17; roadmap lives in a GitHub Project | 16 | roadmap is a Project board, not a doc |

Everything in the reference stack is still 0.x/demo-versioned. The end-2026 obligation is on Member
States to *offer* a wallet, not on the reference implementation to be production software.

### E2.3 The AV implementation family in the same GitHub org (read 2026-10-23)

This is new relative to the fork's comparator set, and it matters: **the Commission ships its own
age-verification wallet apps, issuer backend, verifier endpoints and Longfellow ZK libraries.**

| repo | release / activity | stars | what it is |
|---|---|---|---|
| `av-app-android-wallet-ui` | `2026.09-1` (2026-09-03); pushed 2026-09-04 | 308 | the reference AV app for Android |
| `av-app-ios-wallet-ui` | `2026.10.1` (2026-10-15) | 20 | the reference AV app for iOS |
| `av-lib-ios-longfellow-zkp` | **v0.19.0 (2026-10-15)**; Apache-2.0 | 4 | "A Swift library for zero-knowledge proof (ZKP) generation and verification of mdoc credentials using the Longfellow ZK system"; wraps Google's `MdocZK`; prover **and** verifier; iOS 16+, Swift 6.0+; bundles circuits; parses/matches `ZkSystemSpec` from DCQL JSON |
| `av-lib-android-zkp-age-icao` | pushed 2026-05-21 | 20 | "experimental Android library" proving age from **ICAO ePassports/ID cards**; circuit limited to RSA PKCS#1 v1.5 SHA-256 (DSC), RSASSA-PSS SHA-256 (SOD), CSC RSA-4096, DSC RSA-3072, TBSCertificate 1600 B; "initial development release version" |
| `av-srv-web-issuing-avw-py` | pushed 2026-05-15 | 5 | Attestation-Provider backend issuer, OID4VCI |
| `av-srv-verifier-endpoint` | pushed 2026-09-14 | 0 | verifier endpoint |
| `av-srv-trust-validator` | pushed 2026-10-12 | 0 | trust validation service |
| `av-web-verifier-ui`, `av-verifier-frontend-cinema`, `av-dc-api-backend`, `av-srv-web-verifier-endpoint-23220-4-kt`, `av-banking-demo-credential-offer-app-android` | 2026 | 1–15 | demo/UI/backend scaffolding |

(All primary: GitHub API, read 2026-10-23.)

### E2.4 The age-verification trusted list is live — and it has one entry

Fetched `http://trust.tech.ec.europa.eu/lists/age-verification/av-tl.xml` (ETSI TS 119 612 XML,
signed by the European Commission; list issue **2026-05-19T09:00Z**, **NextUpdate
2026-11-11T13:00Z**; scheme pages at `…/age-verification/scheme-information`, `/scheme-rules`,
`/status-determination/compliance-declaration`):

- **One Member State, one provider:** `paap/DK` — *Digitaliseringsstyrelsen / Danish Agency for
  Digital Government*, service **"AltID Bevisudstedelsesservice"** (`service-type/paa`), status
  **`recognized`**, last status change 2026-06-03.
- No other country, and **no `paap/EE`** — Estonia has no Proof of Age Attestation Provider listed.
- The list's own scope statement ties it to **DSA Article 28** compliance.
(primary, read/fetched 2026-10-23)

### E2.5 Political framing and clock

| fact | evidence | date | type |
|---|---|---|---|
| **Commission Recommendation (EU) 2026/1035** adopted; recommends Member States use the EU AV blueprint and make privacy-preserving age verification available **by 31 December 2026**; Commission will set up the **EU Age Verification scheme**, a **list of compliant AV solutions**, and a **list of trusted AP providers** | `https://digital-strategy.ec.europa.eu/en/library/commission-sets-out-common-approach-eu-wide-age-verification-technologies`; ELI `https://eur-lex.europa.eu/eli/reco/2026/1035/oj` | 29 Apr 2026 (page last updated 24 Sep 2026) | primary |
| DSA guidelines on protection of minors adopted July 2025 recommend effective age assurance, including for adult content and gambling | same page | Jul 2025 | primary (via EC page) |
| **EU KIDS Act proposal adopted 17 September 2026** — COM(2026) 680, CELEX 52026PC0681, plus SWD impact analysis; defines **15** as the age below which autonomous account creation on social networking/video-sharing platforms is restricted; consultation open **until 3 December 2026**; not law yet | `https://digital-strategy.ec.europa.eu/en/library/proposal-eu-kids-act-…`; `https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A52026PC0681` | 17 Sep 2026 (page last update 13 Oct 2026) | primary |
| Seven Member States pilot the AV app (Cyprus, Denmark, France, Greece, Ireland, Italy, Spain) | `https://www.openideurope.eu/digital-identity/eu-age-verification/` | 2026 | secondary |

---

## E3. ZK age proofs in the wild

### E3.1 Longfellow / libzk (the reference implementation the profile names)

| fact | evidence | date | type |
|---|---|---|---|
| Repo has moved/again exists under org **`longfellow-zk`**: `longfellow-zk/longfellow-zk`, Apache-2.0, **1,322★**, pushed **2026-10-21**; latest release **v0.9 (2026-03-31)** (prior: v0.8.6 2026-01-13, v0.8.5 2025-11-10) | GitHub API | read 2026-10-23 | primary |
| `docs/content/en/docs/protocols.md` documents the `mso_mdoc_zk` request format over OpenID4VP: "the special `mso_mdoc_zk` format value, and the inclusion of `zk_system_type` and `verifier_message`, which are defined in the ISO 18013-5 second edition, section 10.3.4" | repo file | read 2026-10-23 | primary |
| `zk-system-spec.md` gives the ZkSystemSpec fields and says: "**Currently there is no standard DCQL for OpenID4VP. We recommend using `mso_mdoc_zk` format to request an ISO mDL via OpenId4VP.**" | repo file | read 2026-10-23 | primary |
| **A Google-maintained Go verifier service exists**: `reference/verifier-service/server` — "a simple go server [that] exports a HTTP interface to running the Longfellow ZK-mdoc verifier. Namely, it verifies a given proof with respect to its claims and returns either an OK or a FAIL response, along with all of the claimed attributes", with a Dockerfile | repo file + Google RP docs link | read 2026-10-23 | primary |
| **Published measurements** exist but are library micro-benchmarks, not device or mdoc end-to-end numbers: `docs/…/benchmarks.md` (page dated 2025-07-05) reports single-threaded Mac M4 results, incl. "**the time to prove posession of 1 signature to <17ms**" and FFT/SHA tables, and states "All of our code runs single threaded for deployment purposes. It is important not to consume a user's battery." | repo file | page dated 2025-07-05, read 2026-10-23 | primary |

### E3.2 Google Wallet — `mso_mdoc_zk` is a documented third-party integration surface

The Google Wallet "Verify with Google Wallet" RP guide (page footer: **Last updated 2026-10-16**)
now lists ZKP as **Step D**, alongside steps A–C:

> "To support Zero-Knowledge Proofs (for example, verifying a user is over 18 without seeing their
> exact birthdate), change your request format to `mso_mdoc_zk` and provide the required
> `zk_system_type` configuration."

The same page's resource table recommends, for verifiers: Google's **Identity Verifiers Reference
Implementation**, the **Multipaz SDK** (OpenWallet Foundation), and the **"Longfellow ZKP Library &
Verifier Service — open-source library and Docker-based server for verifying Zero-Knowledge Proof
(`mso_mdoc_zk`) age and attribute assertions"**, plus the hosted `verifier.multipaz.org`.
Google Wallet's online path is ISO mdoc over OpenID4VP 1.0 (Android 9+ / supported browsers),
`response_mode` `dc_api.jwt` (or plain `dc_api`), `client_metadata.jwks` with an EC P-256
`ECDH-ES` key, `gw_rp_metadata_bytes` for RP metadata, and IACA-chain validation of `issuerAuth`.
(primary: `https://developers.google.com/wallet/identity/verify/accepting-ids-from-wallet-online`,
read 2026-10-23)

Timeline context, secondary: ZKP age verification launched in Google Wallet in May 2025 with Bumble
as launch partner; Longfellow ZK open-sourced July 2025; age credentials reported arriving in Play
services 26.32 (Aug 2026), with passport-derived age credentials reported for Estonia and Ireland
among others (June 2026). Source quality varies
(`https://blog.google/products-and-platforms/platforms/google-pay/google-wallet-age-identity-verifications/`,
`https://androidmagazine.eu/en/2026/08/20/google-wallet-age-credentials/`) — treat the June-2026
Estonia claim as **secondary and unconfirmed** `[UNVERIFIED]`.

### E3.3 Other implementations with `mso_mdoc_zk` in code

From `gh search code "mso_mdoc_zk"` (read 2026-10-23, primary):

| project | files | maturity signal |
|---|---|---|
| `openwallet-foundation/multipaz` (Kotlin Multiplatform, Apache-2.0, 297★) | `DcqlQuery.kt`, `QueryData.kt`, `DeviceRequest.kt`, `DcqlProcessedResponse.kt`, `OpenID4VP.kt`, `matcher/dcql.cpp`, verifier-server `verifier.js` | shipping SDK, has its own verifier server + hosted verifier |
| `SUNET/vc` (Swedish research-network identity stack, Go) | `pkg/mdoc/zk.go`, `pkg/openid4vp/dcql_zk.go`, `docs/EDUID_AGE_ZK_VEGA_PLAN.md`, `docs/ZK_PPID_VERIFICATION_PLAN.md`, README | **a second Go implementation of mdoc ZK, with its own plans** |
| `a-sit-plus/vck` (Austrian A-SIT, Kotlin MP) | `ARCHITECTURE.md`, `CredentialFormatEnum.kt`, OID4VCI `Extensions.kt` | library support |
| `eu-digital-identity-wallet/eudi-lib-ios-openid4vp-swift` | `OpenId4VPSpec.swift` | official EC iOS OpenID4VP library knows the format |
| `longfellow-zk/longfellow-zk` | `protocols.md`, `zk-system-spec.md` | — |
| this fork | — | the fork's own carrier profile |

### E3.4 Published measurements — what actually exists

- Google/longfellow-zk micro-benchmarks (§E3.1) — host CPU, single-threaded, **<17 ms per ECDSA
  signature proof** on Mac M4.
- Fork's own device/host numbers, e.g. ~6.1 s proving — **per the fork's docs**, not independently
  verified here.
- **No first-party on-device mdoc-ZK proving-time figure was found from the Commission, Scytáles/
  T-Systems, Google Wallet or the AV app repositories in this pass** `[UNVERIFIED]`. Proof
  generation cost on mid-range Android therefore remains unpublished by the parties who mandate it.
- `benchmarks.md` is the only benchmark artefact in the Longfellow repo; the AV repos carry no
  published performance page found in this pass `[UNVERIFIED]`.

---

## E4. Estonia

### E4.1 Programme status

| fact | evidence | date | type |
|---|---|---|---|
| **Competent authority:** RIA (Riigi Infosüsteemi Amet) is designated competent authority under the amending act (supervision, permits, register, notifications, organising wallet provision) | Ministry of Justice and Digital Affairs, `https://justdigi.ee/uudised/valitsus-kiitis-heaks-seaduseelnou-euroopa-liidu-digiidentiteedikukru-kasutuselevotuks` | 01.10.2026 | primary |
| **Legal status: the bill is not law.** Government approved the draft amendments to the **e-identimise ja e-tehingute usaldusteenuste seadus (EUTS)**, the **riigilõivuseadus** and the **karistusseadustik** on 01.10.2026; the package "now moves to the Riigikogu" (Riigikogu menetlusse) | same | 01.10.2026 | primary |
| **Bill content:** a **register of persons relying on the wallet** — companies/agencies wishing to request data via Digikukkur or to issue attestations into it "must register and **may request only the data marked in their registration**"; Digikukkur is added to the **list of important personal documents** in the Penal Code, giving it protection equivalent to state identity documents; public launch planned for **2028**; **from end-2026** Estonian state e-services must be ready to accept other Member States' certified wallets, with **TARA** being updated; **regulated/high-impact private sector from end-2027**; Digikukkur adds to, does not replace, ID-card/Mobile-ID/Smart-ID | same | 01.10.2026 | primary |
| **Procurement:** international tender announced **18 May 2026**; applications closed **29 June 2026**; value **€21.65 M**; up to **96 months** (development + 5 years of operation); competitive dialogue; RIA says "**if the procurement proceeds without disputes, contract signature may take approximately one year**" | `https://www.ria.ee/en/news/ria-has-launched-procurement-find-eudi-wallet-service-provider` (18.05.2026); RIA ET Digikukkur page `https://www.ria.ee/riigi-infosusteem/elektroonilise-identiteedi-teenused/digikukkur-eudi-wallet` (last updated 30.09.2026); value also in secondary press | 2026 | primary (value also secondary) |
| **No award observed.** Three searches, the RIA EN/ET pages and the procurement-register pointer surfaced no winning bidder, no contract signature and no award decision as of 2026-10-23 | searches + `https://www.fin.ee/en/public-procurement-state-aid-and-assets/public-procurement-policy/public-procurement-register` | 2026-10-23 | **absence of evidence, not evidence of absence** `[UNVERIFIED]` |
| **Relying-party market research running now:** "Alates septembrist 2026 teeb RIA koostöös Turu-uuringute ASiga RP turu-uuringut" (since September 2026 RIA, with Turu-uuringute AS, is running an RP market survey) | RIA ET Digikukkur page | page updated 30.09.2026 | primary |
| RP register MVP targeted Q4 2026; TARA foreign-wallet acceptance against 24 Dec 2026 | `https://myid.ie/business/eu-digital-wallet-tracker/estonia/` | 2026 | secondary |
| Estonia participates in APTITUDE; joined spring 2026 | `https://aptitude.digital-identity-wallet.eu/countries/estonia/` (content shared with RIA) | 2026 | secondary |
| **Estonia is not an AV-app pilot Member State** (pilots: CY, DK, FR, GR, IE, IT, ES) and **is not on the AV trusted list** (§E2.4) | see §E2.4, §E2.5 | 2026-10-23 | mixed |

### E4.2 Estonian age-verification / age-limit obligations

- **No published Estonian online age-verification duty was found in this pass.** Searches on Estonian
  terms returned the alcohol/tobacco/excise statutes (`https://www.riigiteataja.ee/akt/AS`,
  `https://www.riigiteataja.ee/akt/ATKEAS`) and tobacco-act amendment drafts, but nothing that places
  an online age-verification obligation on service providers. **Treat as
  `[UNVERIFIED]` — a negative result, not proof of absence.** A PRD must not assert that Estonia has
  such a duty without a Riigi Teataja citation.
- The binding EU-level instrument relevant today is **DSA Article 28** plus the Commission's July 2025
  minors guidelines; the AV trusted list itself is framed as an Art. 28 compliance tool (§E2.4).
- The **EU KIDS Act (15)** is a **proposal** (17 Sep 2026), not law; consultation runs to 3 Dec 2026.
- The fork's own docs cite Estonian age-limit provisions such as the Tobacco Act for offline/in-person
  checks — **per the fork's docs**, not re-verified here.

---

## E5. What CHANGED since 23 October 2026

**The honest answer: nothing.** Today's machine date is **23 October 2026** (`Fri Oct 23 01:46 UTC
2026`), the same day as `docs/research/2026-10-23-shared-appendices.md`. No external event
post-dates that file. There is no "since the previous pass" column to fill, and a PRD must not claim
one. What this pass *does* add is (a) facts the prior pass did not record, (b) corrections, and
(c) one contradiction to check. That is the useful delta:

**(a) New since the prior pass compiled its register (earlier evidenced, newly recorded here):**

1. **The age-verification trusted list is live and populated** — issue 2026-05-19, `NextUpdate`
   2026-11-11, with **Denmark/AltID** as the sole recognised Proof of Age Attestation Provider
   (§E2.4). The prior register did not have the AV TL at all.
2. **The Commission's own AV implementation family** — Android app (308★), iOS app, iOS Longfellow
   ZK library, ICAO ZKP library, AP issuer backend, verifier endpoints, trust validator (§E2.3).
3. **`av-lib-ios-longfellow-zkp` v0.19.0, 15 October 2026** — the EC's iOS Longfellow ZKP library
   (prover *and* verifier) landed **eight days ago**. Any statement that "the EU's own implementation
   has ZKP on Android only" was already stale by the prior pass's own date; the correct statement
   now is that EC iOS ZKP materialised mid-October 2026.
4. **Google's Go Longfellow verifier service**, and Google Wallet's documented **Step D —
   `mso_mdoc_zk`** RP integration (page updated 2026-10-16) (§E3.1–E3.2).
5. **EU KIDS Act proposal adopted 17 September 2026** (COM(2026)680; age 15; consultation to
   3 Dec 2026) — a second, lower age threshold now on a 2026 legislative clock (§E2.5).
6. **Recommendation (EU) 2026/1035** dated 29 April 2026 with the **31 December 2026** availability
   target, the EU AV scheme, the compliant-solutions list and the trusted-AP list (§E2.5).
7. **AV Threat Model** published in the AV v1.1.0 batch (2026-09-01) (§E1.1).
8. **AV lifecycle rules** — 30-attestation batches, ≤3-month validity, re-identification every 3
   months, revocation not required, coarsened `ValidityInfo` (§E1.2e).

**(b) Corrections to the fork's record:**

1. **TS13 is v1.0.1 (2026-01-30), not v1.0 (2025-12-15)**; TS4 is v1.0.1 (2026-01-30); TS14 is v1.0
   (2026-02-27). Source: the version tables inside the specs themselves (§E1.3).
2. **TS13/TS14 are explicitly exploratory** and headed to **ETSI TS 119 476-2**; TS13's own note says
   the approach "will be tested in the Age Verification" (§E1.3). Any text implying a final EU ZKP
   standard exists is wrong.
3. **ARF is still v3.0.0 (2026-07-23)**; v2.9.0 was 2026-05-21. There is no ARF v3.1/v3.0.1 as of
   today (§E2.1).

**(c) Contradiction to handle carefully:** the AV Profile's example tuple uses
`block_enc_hash 4096` while Google's `zk-system-spec.md` example uses `4025` for the same circuit
hash and version (§E1.2d). A PRD that quotes "the" parameter set must say which document it is
quoting.

---

## E6. Source list

| # | source | URL | date | establishes | P/S |
|---|---|---|---|---|---|
| 1 | AV Profile (Annex A) | `https://ageverification.dev/av-doc-technical-specification/docs/annexes/annex-A/annex-A-av-profile/`; raw: `https://github.com/eu-digital-identity-wallet/av-doc-technical-specification/blob/main/docs/annexes/annex-A/annex-A-av-profile.md` | read 2026-10-23 | doctype, ZKP-mandatory presentation, Longfellow-only ZK system, fallback rule, RP verification duties, §A.11 example tuple | P |
| 2 | AV OSPSA | `…/docs/architecture-and-technical-specifications.md` | read 2026-10-23 | scope, §2.6 AV-app-vs-wallet trust difference, §3.4 lifecycle, §3.4.4 trust/AV TL/interim arrangement, TS3 v1.5.2 ref | P |
| 3 | AV Annex B (ZKP) | `…/docs/annexes/annex-B/annex-B-zkp.md` | read 2026-10-23 | Fri2024/Ligero basis, circuit witness/statement, audience | P |
| 4 | AV repo metadata + commit feed | `gh api repos/eu-digital-identity-wallet/av-doc-technical-specification` | read 2026-10-23 | pushed 2026-09-02, `release/1.1.0`, threat-model commit 2026-09-01 | P |
| 5 | AV trusted list XML | `http://trust.tech.ec.europa.eu/lists/age-verification/av-tl.xml` | issued 2026-05-19; next update 2026-11-11 | DK/AltID recognised AP; no EE; DSA Art. 28 framing; ETSI TS 119 612 | P |
| 6 | EC age-verification policy page | `https://digital-strategy.ec.europa.eu/en/policies/eu-age-verification` | read 2026-10-23 | blueprint 14 Jul 2025 → feature-ready 15 Apr 2026; "mini wallet" framing; EU KIDS Act Sept 2026 | P |
| 7 | EC AV FAQ | `https://digital-strategy.ec.europa.eu/en/faqs/eu-age-verification-solution` | read 2026-10-23 | MS adapt/integrate; open-source availability | P |
| 8 | Recommendation page | `https://digital-strategy.ec.europa.eu/en/library/commission-sets-out-common-approach-eu-wide-age-verification-technologies` | 29 Apr 2026 (upd. 24 Sep 2026) | Reco (EU) 2026/1035; 31 Dec 2026 target; EU AV scheme; compliant-solutions and trusted-AP lists | P |
| 9 | EU KIDS Act proposal | `https://digital-strategy.ec.europa.eu/en/library/proposal-eu-kids-act-eu-keeping-internet-digital-spaces-accountable-and-trustworthy` | 17 Sep 2026 (upd. 13 Oct 2026) | COM(2026)680; age 15; consultation to 3 Dec 2026 | P |
| 10 | ARF releases | `gh api repos/…/eudi-doc-architecture-and-reference-framework/releases` | read 2026-10-23 | v3.0.0 2026-07-23; v2.9.0 2026-05-21; Topic_AA_rr v0.8 2026-10-14 | P |
| 11 | ARF §7 certification | `https://eudi.dev/3.0.0/main/07-wallet-solution-certification-and-risk-management/` | ARF v3.0.0 | CIR 2024/2981; national→CSA two-phase; LoA high; FCAF scope | P |
| 12 | CIR 2026/1730 | `https://data.europa.eu/eli/reg_impl/2026/1730/oj` | 15 Jul 2026, OJ 22.7.2026 | amends 2025/848; ETSI TS 119 475 V1.2.1; national RP register reference | P |
| 13 | CIR 2026/1735 | `https://data.europa.eu/eli/reg_impl/2026/1735/oj` | 15 Jul 2026, OJ 22.7.2026 | amends 2025/1569; ETSI TS 119 478 V1.1.1 authentic sources | P |
| 14 | TS4 / TS13 / TS14 | `…/docs/technical-specifications/{ts4-zkp,ts13-zksnarks,ts14-zkps-from-mms}.md` | version tables: 2026-01-30 / 2026-01-30 / 2026-02-27 | current versions; exploratory status; ETSI handover | P |
| 15 | longfellow-zk repo | `gh api repos/longfellow-zk/longfellow-zk` + `docs/…/{protocols,zk-system-spec,benchmarks}.md` | pushed 2026-10-21; benchmarks page 2025-07-05 | v0.9; mso_mdoc_zk + zk_system_type; "no standard DCQL"; verifier service; <17 ms/signature on M4 | P |
| 16 | Longfellow verifier service | `https://github.com/longfellow-zk/longfellow-zk/tree/main/reference/verifier-service/server` | read 2026-10-23 | Go HTTP verifier + Docker for mdoc ZK proofs | P |
| 17 | Google Wallet RP guide | `https://developers.google.com/wallet/identity/verify/accepting-ids-from-wallet-online` | last updated 2026-10-16 | Step D ZKP; `mso_mdoc_zk` + `zk_system_type`; libraries recommended (incl. Longfellow verifier service, Multipaz) | P |
| 18 | Google Wallet blog | `https://blog.google/products-and-platforms/platforms/google-pay/google-wallet-age-identity-verifications/` | 2025 | ZKP age verification in Google Wallet | P (marketing) |
| 19 | AV app family (10 repos) | `gh api orgs/eu-digital-identity-wallet/repos`; `av-app-android-wallet-ui`, `av-app-ios-wallet-ui`, `av-lib-ios-longfellow-zkp`, `av-lib-android-zkp-age-icao`, `av-srv-verifier-endpoint`, `av-srv-trust-validator`, `av-srv-web-issuing-avw-py` | read 2026-10-23 | EC's own AV implementations and their release maturity | P |
| 20 | `av-lib-ios-longfellow-zkp` README | `…/contents/README.md` | v0.19.0 2026-10-15 | Swift prover+verifier on Google `MdocZK`; iOS 16+/Swift 6; ZkSystemSpec from DCQL | P |
| 21 | `av-lib-android-zkp-age-icao` README | `…/contents/README.md` | pushed 2026-05-21 | experimental ICAO ZKP; narrow crypto constraints | P |
| 22 | `gh search code mso_mdoc_zk` | GitHub code search | 2026-10-23 | multipaz, SUNET/vc, a-sit-plus/vck, EC iOS OpenID4VP | P |
| 23 | Estonia — government bill approval | `https://justdigi.ee/uudised/valitsus-kiitis-heaks-seaduseelnou-euroopa-liidu-digiidentiteedikukru-kasutuselevotuks` | 01.10.2026 | EUTS/Riigilõiv/KarS amendments; RIA competent authority; RP register; 2028 launch; TARA by end-2026; private sector end-2027; Riigikogu next | P |
| 24 | Estonia — RIA programme page (EN) | `https://www.ria.ee/en/state-information-system/electronic-identity-eid-and-trust-services/eudi-wallet` | updated 22.07.2026 | tender announced 18.05.2026; acceptance duties; ~1 year to contract | P |
| 25 | Estonia — RIA Digikukkur page (ET) | `https://www.ria.ee/riigi-infosusteem/elektroonilise-identiteedi-teenused/digikukkur-eudi-wallet` | updated 30.09.2026 | RP market survey with Turu-uuringute AS since Sept 2026; acceptance duties | P |
| 26 | Estonia — RIA procurement news | `https://www.ria.ee/en/news/ria-has-launched-procurement-find-eudi-wallet-service-provider` | 18.05.2026 | procurement launch | P |
| 27 | Estonia — procurement value/press | `https://www.biometricupdate.com/202605/estonia-launches-e21-65m-procurement-for-eu-digital-identity-wallet`; `peopleofinternet.com` articles | 2026-05 | €21.65 M; 96 months; deadline 29 Jun 2026 | S |
| 28 | Estonia — wallet tracker | `https://myid.ie/business/eu-digital-wallet-tracker/estonia/` | 2026 | RP register MVP Q4 2026; TARA 24 Dec 2026 | S |
| 29 | Estonia — APTITUDE country page | `https://aptitude.digital-identity-wallet.eu/countries/estonia/` | 2026 | Estonia joined spring 2026 | S |
| 30 | AV pilots / blueprint explainer | `https://www.openideurope.eu/digital-identity/eu-age-verification/` | 2026 | 7 pilot MS; Apr 2026 recommendation | S |
| 31 | AV app deep dive (vendor) | `https://www.biometricupdate.com/202604/breaking-down-the-european-commissions-white-label-age-verification-app` | Apr 2026 | Scytáles + T-Systems; Android ZKP vs iOS batch tokens at that time | S |
| 32 | Conformance framework | `https://conformance.eudi.dev/0.0.7/introduction/` | read 2026-10-23 | CIR 2024/2981 basis for FCAF | P |
| 33 | Firecrawl (local) | `http://asus:3002/v1/search`, `/v1/scrape` | used 2026-10-23 | fallback retrieval when `web_search` returned empty | tool |

**Not opened / not confirmed in this pass:** `[UNVERIFIED]` marked inline above — notably
CIR 2026/1731's own text, the AV Profile's exact published version string, the accepted-circuit
set, any Commission-published proof-generation timing, and the reported June-2026 Google Wallet age
credentials in Estonia.
