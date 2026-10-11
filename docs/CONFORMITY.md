# Conformity note

This file states where the wallet stands against
[`EE-EUDIW-TS-1.0`](../spec/EE-EUDIW-TS-1.0.md)
(content revision 1.6, 12 October 2026, edited in this repository; revision 1.5 was the ee-eudiw
9f5778a text byte for byte) as of
`step6-issuance-consumption` (910ddc4) plus the branch this file lands on. It is the
`EE-ZKP-003` conformity statement the specification requires a wallet's documentation to carry,
plus the inventory `EE-ZKP-060` asks for and the build-provenance statement an assessor will ask
for first. Nothing here is a certification claim. "The conformance plan" and "plan §x" below
mean `docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` in the private ee-eudiw repository; it is
not mirrored into this public fork.

## 1. The ZKP_08 position, stated conditionally (EE-ZKP-003)

**This wallet does not assert unqualified `ZKP_08` compliance, and must not be assessed as
claiming one.** ARF `ZKP_08` requires a ZKP scheme to rely solely on algorithms included in the
ECCG Crypto Module (ACM). Whether Longfellow — the proof system this wallet ships through
multipaz 0.99.0 (`multipaz-longfellow`) — satisfies that depends on a reading of `ZKP_08` no
authority has given. The specification's §10.3 states the two available readings; **the reading
this wallet's assessment applies is reading (a): primitives and assumptions** — the ACM as a
catalogue of primitives and accepted assumptions, the way the Commission's own Technical
Specifications, ETSI TR 119 476-1 and ANSSI use it elsewhere. Reading (b) (the proof system must
itself be listed) is unsatisfiable by any scheme available in any certification window: the ACM
lists no ZKP scheme, and the v3.0 working draft's Annex C would require years of
standardisation before one could even be submitted. Recording the reading is the point of
`EE-ZKP-003`: an assessment that leaves it implicit cannot be reproduced.

Under reading (a), the primitive-to-ACM map for what this wallet runs:

| Element | ACM v2.0 status |
|---|---|
| SHA-256 (Merkle hashing, Fiat–Shamir, transcript binding) | Listed. The v3.0 working draft demotes it to *admissible* (spec §10.9). Inside the statement being proven (the MSO digests) that is a backward-compatibility dependency per `EE-ZKP-022c`; as the Merkle and Fiat–Shamir hash it is a component the wallet controls, which `EE-ZKP-022c` requires at the *recommended* level of the applicable ACM — one of the conditions below. |
| AES-256 (the `AES256CTR` half of the challenge expander) | Listed as a primitive, and still *recommended* in the v3.0 working draft (spec §10.9); **the construction is not** — `SHA256-AES256CTR` as a bare Fiat–Shamir challenge PRF is not one of the SP 800-90A DRBG constructions ACM §7 lists. Covered by `EE-ZKP-022b`'s assumption map, not by an ACM entry. |
| ECDSA over P-256 (issuer signature and device signature, inside the circuit and out) | Listed. The v3.0 working draft demotes it to *admissible* (spec §10.9); per `EE-ZKP-022c` it is a backward-compatibility dependency on already-issued credentials and certified WSCDs, not a design choice. |
| GF(2^128) arithmetic and the Reed–Solomon code over it | Carries no computational hardness assumption (an arithmetic setting, not a mechanism), so it falls outside ACM §4.2/§4.3's rules rather than contrary to them — recorded per `EE-ZKP-022b`. |
| The MAC linking the two circuit fields (the ECDSA circuit over the P-256 base field and the SHA-256/CBOR circuit over GF(2^128)) | No ACM entry. `EE-ZKP-022b` requires its record too: whether it carries a computational hardness assumption and, if not, the information-theoretic soundness bound relied on. Neither the specification nor the reference repository states that bound for the v7 parameters, so this entry is **open**, not recorded. |

The compliance that remains is **conditional, not already met**, on three things the
specification itself defines: the soundness floor of `EE-ZKP-022a`, the assumption map of
`EE-ZKP-022b` covering the PRF gap and the MAC above, and the recommended-tier ciphersuite
selection of `EE-ZKP-022c`. The circuits this wallet bundles (v7, one attribute) carry a
**109-bit soundness error** against the ACM v2.0 §1.1 recommended level of "at least 125 bits"
(above the 100 bits §1.3 accepts for legacy mechanisms). The figure is the specification's
(§10.5, `EE-ZKP-022`): Longfellow's `mdoc_zk.h` annotates the v7 parameters
`~109 bits statistical security`, while `draft-google-cfrg-libzk-02` states the same profiles
"have been analyzed to provide at least 115 bits of security"; the specification uses the lower
figure throughout, and so does this note. Either is below 125. Until an audited ≥125-bit parameter
set exists upstream and is adopted into the verifier's registry, this wallet's ZK path is a
pilot-grade mechanism: the shortfall is declared here, and the wallet SHALL NOT be advertised
as meeting the ACM's recommended level. The migration trigger is the upstream release: when
`google/longfellow-zk` publishes audited ≥125-bit parameters, the wallet's bundled
`multipaz-longfellow` must move to them and the verifier-side registry entry must change with
it (`EE-ZKP-004`'s replaceability is what makes that a swap, not a re-issue).

## 2. Cryptographic inventory (EE-ZKP-060)

Per algorithm, its quantum exposure, its migration path, and which exposure is soundness
(forgery) and which is privacy (de-anonymisation):

| Algorithm | Use | Quantum exposure | Exposure class | Migration path |
|---|---|---|---|---|
| SHA-256 | Circuit hashing, Fiat–Shamir, transcripts | Grover (~2^128 collision work at 256-bit output) — no practical break foreseen | Soundness (and transcript binding) | Hash-function swap inside the circuit; `EE-ZKP-061`'s reason to prefer a hash-based soundness argument is exactly this |
| AES-256 | Challenge expansion (CTR as bare PRF) | Grover; 256-bit keys keep a wide margin | Soundness | Part of the ciphersuite selection `EE-ZKP-022c` governs |
| ECDSA P-256 | Issuer (MSO) and device (DeviceAuthentication) signatures | Shor — broken by a CRQC | Both: forgery of MSOs (soundness) and, via device keys, impersonation (soundness); not directly privacy | Post-quantum signature inside the circuit statement or hybrid issuer signatures; the presentation path migrates with the issuer, the WSCD path with the secure area |
| ECDH P-256 (HPKE DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM) | DC API response encryption | Shor — broken by a CRQC | Privacy (response confidentiality) | HPKE suite negotiation to a PQ/hybrid KEM when the profile defines one |
| ECDH-ES + A256KW / A256GCM (compact JWE) | OpenID4VP `direct_post.jwt` response encryption (ADR-003, default on) | Shor — broken by a CRQC | Privacy (response confidentiality) | Same hybrid-KEM posture as the HPKE row when a profile defines one |
| Longfellow v7 circuits | ZK age proof | No known quantum speedup against the hash-based soundness argument beyond Grover-on-SHA-256 | Soundness | Re-parameterisation to ≥125 bits (`EE-ZKP-022a`); the scheme's soundness resting only on hash functions is why no scheme migration is needed |

What this wallet **can claim today**:

- The ZK age proof (`age_over_18` over `ee.riik.poa.1` / `eu.europa.ec.av.1`) works over the
  Digital Credentials API (`org-iso-mdoc`, ISO/IEC 18013-7 Annex C) path — fixture-verified, not
  yet exercised end to end on a device — and the two Longfellow halves cross-verify: this fork's multipaz 0.99.0 prover against the reference verifier's Rust
  runtime and the reverse, both directions PASS with committed fixtures (step 0 of the
  conformance plan, `verifier/go/zk/testdata/step0-multipaz/` and `testdata/step0-rust-prover/`
  in the reference repository, 23 September 2026).
- Over OpenID4VP the spine now runs the de-facto `mso_mdoc_zk` carrier (profile v1.0 above)
  with `direct_post.jwt` response encryption on by default (ADR-003), e2e-verified end to end
  in `tests/e2e.sh` (13 assertions, both response modes, both carriers, one- and
  two-credential sessions). The relying-party verify budget is honestly re-derived rather than
  met: host verification is ~95% Longfellow sumcheck arithmetic, p95 3.2–4.9 s across rounds on
  an i5-8365U-class host, and the 1.0 s p95 figure is now recorded as the device-side prover
  budget with the verifier budget expressed as a capacity statement — see
  [`docs/MEASUREMENTS.md`](MEASUREMENTS.md) "Host verify optimization log" (W2, 25 September
  2026), where all three host optimization candidates (Rust PGO, FFI scratch arena, Go PGO)
  were measured and rejected on evidence.
- Device keys are generated in hardware: multipaz `AndroidKeystoreSecureArea`, StrongBox where
  the device advertises `FEATURE_STRONGBOX_KEYSTORE`, TEE otherwise, with no API that can
  return private-key material; `ee.riik.poa.1` is issued over them in one transaction and a
  plain presentation consumes the attestation while a ZK presentation consumes nothing.

What this wallet **cannot claim today**:

- **No OpenID4VP ZK carrier exists.** OpenID4VP 1.0 defines no way to carry a ZK request or a
  `ZkDocument`, and the AV Profile's plain fallback is available where the transport is
  OpenID4VP — so over OpenID4VP this wallet presents plain only. The ZK path runs over the ISO
  Annex C DC API path until a carrier is defined (conformance plan finding S4; the reference
  repository carries the Topic G input).
  **Carrier status note (25 September 2026):** a *de-facto* OpenID4VP ZK carrier
  (`mso_mdoc_zk`) is deployed by four implementations and is now profiled, fixture-verified
  and dual-carrier-tested in this repository — see
  [`docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`](profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md)
  **v1.0 (25 September 2026)** and its committed test vectors
  [`tests/vectors/carrier-v1/`](../tests/vectors/carrier-v1/). The profile is a citable
  reference, not a standard: the registration, registry and venue questions it closes in
  profile scope remain open in every standards body (OpenID DCHP #17 — submission prepared
  but unfiled, user-gated; ISO/IEC 18013-5 2e — watchlist at
  [`docs/analysis/ISO-18013-5-2E-BYTE-DIFF-CHECKLIST.md`](analysis/ISO-18013-5-2E-BYTE-DIFF-CHECKLIST.md)).
  Until one of those venues adopts the carrier, the conformity claim above stands: no
  standardized DCQL query for ZKP exists, and this document claims conformity to
  specifications, not to this repository's own profile.
- **The ISO Annex C ZK path has no on-device evidence.** The reference verifier's
  `/present/dcapi` endpoint and this wallet's proving code both exist and are unit-tested, but
  the end-to-end loop — `navigator.credentials.get` on Chrome/Android with GMS core
  credentials, the platform passing `zkRequest` params through, HPKE byte-equality against the
  verifier's stdlib `crypto/hpke`, consumption on-device — is PENDING-DEVICE in both
  repositories' step records. Until it is exercised, the Annex C claim above rests on fixtures,
  not on a device session.
- **No unobservability (`ZKP_07`) claim, per WSCD architecture (`EE-CNF-007`).** Every
  credential device key, including the one behind the ZK path, is an EC key on the local native
  WSCD above (`LocalCryptoProvider` → `AndroidKeystoreSecureArea`); only RSA keys route to the
  wallet provider's remote signing service (`RemoteCryptoProvider`, a mock in `local_mocks`), and
  no credential this fork issues uses one. There is therefore one WSCD architecture to state the
  property for, no `ZKP_07` claim is made for it here, and none could be for a remote-HSM WSCD.
- Every build this fork can produce is debug-derived — see §3 — so nothing here has the
  provenance a certification assessment starts from.

## 3. Build provenance: every build is debug-derived

**Every APK this repository can produce is debug-derived, and this is not expected to change
until the plan §8.6 signing work lands.** `app/build.gradle.kts` disables the `release` build
type outright (`androidComponents.beforeVariants` sets `enable = false` for it) and the default
variant is `local_mocks` — a debug-derived type with all backend services mocked
(`USE_MOCKS = true`). The release signing configuration exists but references project
properties no build here supplies, and no release keystore is part of this repository.

Consequences, stated plainly:

- `EE-CNF-005` (no certification of a demo or debug build) and `EE-ZKP-002`'s demo-build
  clause apply to **every** build of this fork, not to a misconfigured one. No conformity
  assessment result transfers to any future build until §8.6's release-grade variant exists:
  no mocks, no Firebase (already removed at step 1), `trust_all_validator` compiled out, LOTL
  pointed at the real list, and signed with the fork's own key.
- §8.6 removes the "debug" half of that objection only. The "demo" half — the issuer being a
  CLI (`mint_ee_poa.py`) or the PoC's mock — stands until a real issuer exists, and this note
  will say so when that changes.

## Status

| Item | State |
|---|---|
| `EE-ZKP-003` statement | Recorded above; reading (a) declared; conditional, not asserted |
| `EE-ZKP-060` inventory | Recorded above; soundness and privacy exposure separated |
| `ZKP_08` | Shortfall declared (109-bit v7 circuits, conditional reading (a)); pilot-grade. Soundness figure: **EE-ZKP-022a** — 109-bit computation soundness against the **125-bit ACM recommended level** (per spec rev 1.3) |
| `EE-CNF-007` | Credential keys on the local native WSCD only; no `ZKP_07` claim |
| Build provenance | Debug-derived only; §8.6 pending |
| On-device evidence | None; PENDING-DEVICE items in `docs/planning/STEP5-RECORD.md` and `STEP6-RECORD.md` |
| Device key attestation | Mock WP self-signed; **EE-SEC-004 is SHOULD** (spec:243 in rev 1.6; 236 in rev 1.5) — no SHALL mandates attestation on the presentation path; challenge UUID unverified (`AttestationChallenge.kt:9-12`) |
| issuerAuth on-device validation | E6 status: absent — mdoc credentials load with no issuerAuth chain check (SD-JWT path verifies); row updates when E6 lands |
| iaca.crl | Removed (E7): expired 2025-05-20, never wired; revocation now fetches from chain distribution points (E4 soft-fail policy) |
| EE-PRO-010a redirect flow | Documented deviation (B2): `RedirectUri` client-id prefix removed — JAR-signed requests (`x509_san_dns`/`x509_hash`) are the only accepted client identifiers. Note: EE-PRO-003 as of rev 1.5 no longer permits `x509_san_dns`; see the EE-PRO-003 row |
| EE-PRO-003 client identifier prefix | **Not met in two respects.** (1) Spec rev 1.5 corrects EE-PRO-003 to HAIP 1.0 §5: `x509_hash` for every signed request. The wallet accepts it, and still also accepts `x509_san_dns` (`ClientIdPrefixPolicy.kt`), which the in-repo Go verifier defaults to (`verifier/go/main.go`, `-client-id`) and which pre-HAIP test verifiers used **[UNVERIFIED for verifier.eudiw.dev]**; dropping it is a one-line change. (2) The wallet does not check that `response_uri` matches a `dNSName` SAN of the access certificate, which rev 1.5 requires where the certificate profile carries one — conditional, open per spec §23 item 31 |
| Status lists | EE-PoA mints none (EE-POA-013: consumed, not revoked; EE-POA-017); AV RP-side status fetch **deferred** — the Go verifier has no plain-mdoc path (all presentations route through ZK carriers that hide the status claim), so there is no post-Select hook to hang the fetch on; fold into the signed-DCAPI/EE-PRO-010a workstream when a plain path exists |
| B5 `expected_origins` | Resolved as documented deviation, folded here (signed-DCAPI carrier deferred with EE-PRO-010a) |
| EE-PRO-001 response encryption | Enforced on the redirect path, twice: `PresentationRequestViewModel` refuses before the consent screen, and `OpenId4VPManager.sendResponse` refuses again before any document leaves (a typed `ResponseRefused`, shown as the unencrypted-request message, not a retryable network error). A request that is not `direct_post.jwt` encrypted under ECDH-ES/P-256 with A128GCM or A256GCM is refused; the user sees a dedicated message at once, and the refusal is written to the activity log for each requested document type the wallet knows. The verifier is sent a negative answer in the background, from the application scope so closing the error screen does not cancel it, **only on `direct_post` modes** — for fragment and query modes the library builds a redirect, which the wallet does not follow. A128CBC-HS256 is no longer advertised; request objects are accepted under ES256/384/512 only. The library already restricts `alg`/`enc` for `*.jwt` modes; what the policy adds is the refusal of cleartext modes and the P-256 check |
| EE-PRO-002 Presentation Exchange | Not on the wire: the `eudi-openid4vp` 0.11.1 jar contains no `presentation_definition` handling (checked in the jar), and its request model carries DCQL. `eudi-prex` remains as an **in-app matcher** for ISO 18013-5 and DC API `DeviceRequest`s (`toPresentationDefinition`), never parsed from or sent to a verifier. Replacing it is a refactor, not a conformance fix |
| EE-PRO-007 SIOPv2 | Refused before consent: the library resolves `id_token` and `vp_token id_token` requests to SIOP request types, and `handleRequestObject` refuses everything other than `OpenId4VPAuthorization`. The request object is still fetched and its signature checked first |
| EE-PRO-004 unsigned requests | Redirect flows: met — only JAR-signed `x509_*` requests are accepted. DC API: not applicable today; the wallet answers `org-iso-mdoc` only and refuses OpenID4VP over the DC API, so the unsigned-request handling rev 1.5 requires (accept, show the origin, say it is unauthenticated) binds the first OpenID4VP-over-DC-API code |
| EE-PRO-010 channel indication | The consent screen names the channel: a QR scanned by the wallet is labelled cross-device with no proximity check; a deep link is labelled as a link, with the warning that a camera-scanned link may also be cross-device — the wallet cannot tell those apart, so it does not claim "same-device". The scanner is recognised by its `uri` navigation argument, not by the absence of an intent, which Navigation attaches to every route (`ScannerChannelTest`) |
| EE-PRO-010a out-of-band code | **Not met.** The BCP 247 §6.1 code the user matches across devices needs a verifier-side field no standard defines; the scanned-QR path stays available, labelled, until it does |
| EE-PRO-023 pre-authorized code | Not applicable today: the wallet starts issuance itself with the authorization code flow and parses no Credential Offer. The requirement binds the first offer-handling code |
| EE-PRO-024 key binding | **Partially met.** The OpenID4VCI path (mDL) refuses `NoProofRequired` rather than store an unbound credential. Its authorization state is single-use, so a replayed redirect finds nothing; a failed or abandoned issuance releases its key, as does an mDL the user is shown and leaves without accepting; no key a stored attestation row names is ever released (checked in the database, not through the document mapper). Not met: PID issuance (gRPC) and the EE-PoA/AV mock issuers do not go through OpenID4VCI; no path checks that the issued credential's key (`cnf.jwk`, `deviceKeyInfo`) is the key the wallet proved; and the unbound SD-JWT VC label and the `require_cryptographic_holder_binding` check (`DcqlRequestProcessor`) are not implemented. Deferred issuance fails with a stated reason; both refusals show the user why |
| EE-POA-011c refill timing | **Not met, no scheduler.** The mock provider issues a batch when the user asks; there is no low-water refill to defer. The requirement binds the first automatic refill |
| EE-REV-006 status index | Not applicable: EE-PoA carries no status reference (EE-POA-017), and the mock PID/mDL issuers are upstream test fixtures |
| Spec rev 1.6 rows | Added 12 October 2026. The code has not changed since rev 1.5; every status below is against the code as it stands |
| EE-POA-013 last attestation (rev 1.6) | **Not met.** `EePoaConsumption.consumeAfterPresentation` never consumes the last attestation, but nothing prefers Tier 2 for it, shows the linkable-across-relying-parties notice, asks for approval, crosses the low-water mark or schedules a refill |
| EE-POA-025 re-issuance 14 days before expiry | **Not met, no scheduler** (as EE-POA-011c) |
| EE-POA-026 7-day `ValidityInfo` epoch | **Not met** in the mock issuer: `validFrom` is the issuance day at midnight UTC and `validUntil` the day plus the maximum validity (`CredentialIssuanceServiceMock`, `AgeVerificationProviderServiceMock`); binds the Provider |
| EE-POA-027/028 predicate timing, incomplete dates of birth | Not applicable (binds the Provider). The mock reads a mock-user flag, not a date of birth; `sandbox/server.py` uses the host's local date. Neither applies Europe/Tallinn or the 29 February rule (spec §23 item 36) |
| EE-POA-029 birthday refresh | Provider half met incidentally (mock `validUntil` is not birthday-derived). Wallet half **not met**: no scheduler |
| EE-POA-030 one predicate per presentation | **Not met.** No check limits a presentation to one `age_over_NN` or treats a claim-less query as `age_over_18` only |
| EE-POA-031 `false` predicates withheld | **Not met.** The mock 17-year-old user is issued `age_over_18 = false`; no code withholds it or answers "no matching credential" |
| EE-POA-032 one Wallet Unit; delete on revocation | **Not met.** No previous-unit disclosure, no EE-PoA deletion on Wallet Unit revocation |
| EE-POA-003 rules applied to `eu.europa.ec.av.1` | **Not met.** The AV attestation is issued as a single document, not a batch, and is not consumed (`AgeVerificationProviderServiceMock.issueEePoaBatch`) |
| EE-POA-024 AV preference for every relying party | **Not met.** No credential-preference logic between the two doctypes |
| EE-ZKP-045a budget does not override EE-ZKP-055 | Not applicable today: no proximity Tier 2 path and no recorded pair measurement. Binds the first proximity ZK code |
| EE-ZKP-055a unauthenticated requests | **Not met.** `HolderObligations.refusePlainFallback` keys on the request (`proofRequested`), not on a registration certificate, and an unauthenticated request that asks for no proof gets the plain path |
| EE-ZKP-062 relying-party clock | Not assessed: no wallet-side expiry gate was found on the presentation path; the RP-side check is the verifier's |
| EE-PRO-025 `readerAuth` | **Partially met.** `DeviceRequestParser` checks the `readerAuth` signature and the consent screen shows the subject only on a pass; the chain is not validated to the Estonian ACA or a trusted list, and an unauthenticated `org-iso-mdoc` request is not labelled as such or refused |
| EE-PRO-026 `request_uri` lifetime | Reference verifier: 3-minute default session TTL (`verifier/go/main.go`, `-session-ttl`). Single-use and the wallet's "not answered" notice not assessed |
| EE-SEC-001 (rev 1.6) | Met for the default: local native only; the remote-HSM path is a mock and stays out of any certified deployment until EE-SEC-001's condition is met |
| EE-WUA-007 no stable WUA identifier | Not assessed: the mock wallet provider's WIA/KA content was not reviewed against it |
| EE-WUA-008 AVSUA-equivalent | **Not met.** No AV attestation provider integration |
| EE-REV-002 (RP clause), EE-REV-007 | Not applicable (bind relying parties); the Go verifier fetches no status list today |
| EE-PID-009, EE-ONB-011 | Not applicable (bind the PID Provider and the Rulebook) |
| EE-PRO-023 (rev 1.6), EE-GOV-032 (rev 1.6) | Not applicable (bind issuers using pre-authorized offers, and TARA/GovSSO as relying parties) |
| `[REC]` requirements (spec §0.1, Annex E) | Not applicable to the wallet by definition: recommendations to authorities |
| Reference verifier response encryption | **Deviation, recorded.** The Go verifier encrypts `direct_post.jwt` with `ECDH-ES+A256KW`/A256GCM (`verifier/go/internal/jose/jwe.go`), and its JWK carries no `alg`. HAIP 1.0 and EE-PRO-001 name direct `ECDH-ES`, which is all the wallet accepts, so the Android redirect path cannot answer this verifier (its unsigned request objects were already refused). The Python holder and the e2e tests are unaffected. It also defaults its client identifier to `x509_san_dns:verifier.example.ee` (`main.go`, `-client-id`), which EE-PRO-003 rev 1.5 no longer permits |

This note is updated when any row above changes. It is not pushed anywhere and asserts nothing
beyond what the two repositories' records show.
