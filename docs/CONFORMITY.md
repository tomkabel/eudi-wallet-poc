# Conformity note

This file states where the wallet stands against
[`EE-EUDIW-TS-1.0`](https://github.com/tomkabel/ee-eudiw/blob/551fb656481f3fcf0c5b9a1c54be0b188571bf6f/spec/EE-EUDIW-TS-1.0.md)
(content revision 1.4, ee-eudiw 551fb65) as of
`step6-issuance-consumption` (910ddc4) plus the branch this file lands on. It is the
`EE-ZKP-003` conformity statement the specification requires a wallet's documentation to carry,
plus the inventory `EE-ZKP-060` asks for and the build-provenance statement an assessor will ask
for first. Nothing here is a certification claim. "The conformance plan" and "plan §x" below
mean `docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` in the same repository.

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
| SHA-256 (Merkle hashing, Fiat–Shamir, transcript binding) | Listed |
| AES-256 (the `AES256CTR` half of the challenge expander) | Listed as a primitive; **the construction is not** — `SHA256-AES256CTR` as a bare Fiat–Shamir challenge PRF is not one of the SP 800-90A DRBG constructions ACM §7 lists. Covered by `EE-ZKP-022b`'s assumption map, not by an ACM entry. |
| ECDSA over P-256 (issuer signature and device signature, inside the circuit and out) | Listed, at the *admissible* level — a backward-compatibility dependency on already-issued credentials and certified WSCDs, per `EE-ZKP-022c`, not a design choice. |
| GF(2^128) arithmetic and the Reed–Solomon code over it | Carries no computational hardness assumption (an arithmetic setting, not a mechanism), so it falls outside ACM §4.2/§4.3's rules rather than contrary to them — recorded per `EE-ZKP-022b`. |

The compliance that remains is **conditional, not already met**, on three things the
specification itself defines: the soundness floor of `EE-ZKP-022a`, the assumption map of
`EE-ZKP-022b` covering exactly the PRF gap above, and the recommended-tier ciphersuite
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
| Longfellow v7 circuits | ZK age proof | No known quantum speedup against the hash-based soundness argument beyond Grover-on-SHA-256 | Soundness | Re-parameterisation to ≥125 bits (`EE-ZKP-022a`); the scheme's soundness resting only on hash functions is why no scheme migration is needed |

What this wallet **can claim today**:

- The ZK age proof (`age_over_18` over `ee.riik.poa.1` / `eu.europa.ec.av.1`) works over the
  Digital Credentials API (`org-iso-mdoc`, ISO/IEC 18013-7 Annex C) path, and the two Longfellow
  halves cross-verify: this fork's multipaz 0.99.0 prover against the reference verifier's Rust
  runtime and the reverse, both directions PASS with committed fixtures (step 0 of the
  conformance plan, `verifier/go/zk/testdata/step0-multipaz/` and `testdata/step0-rust-prover/`
  in the reference repository, 23 September 2026).
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
- **The ISO Annex C ZK path has no on-device evidence.** The reference verifier's
  `/present/dcapi` endpoint and this wallet's proving code both exist and are unit-tested, but
  the end-to-end loop — `navigator.credentials.get` on Chrome/Android with GMS core
  credentials, the platform passing `zkRequest` params through, HPKE byte-equality against the
  verifier's stdlib `crypto/hpke`, consumption on-device — is PENDING-DEVICE in both
  repositories' step records. Until it is exercised, the Annex C claim above rests on fixtures,
  not on a device session.
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
| `ZKP_08` | Shortfall declared (109-bit v7 circuits, conditional reading (a)); pilot-grade |
| Build provenance | Debug-derived only; §8.6 pending |
| On-device evidence | None; PENDING-DEVICE items in `docs/planning/STEP5-RECORD.md` and `STEP6-RECORD.md` |

This note is updated when any row above changes. It is not pushed anywhere and asserts nothing
beyond what the two repositories' records show.
