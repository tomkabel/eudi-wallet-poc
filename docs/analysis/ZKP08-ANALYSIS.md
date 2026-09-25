# ZKP_08 and the Agreed Cryptographic Mechanisms

## Can any zero-knowledge scheme qualify — and is a new one worth designing?

*An expert analysis for the EE-EUDIW project · Tom Kristian Abel · 8 September 2026 · v1.0*

---

## 0. Bottom line

The sentence in EE-EUDIW-TS §10.3 — *"No candidate ZKP scheme currently satisfies ZKP_08"* — is correct under one reading of the requirement and wrong under the other, and the two readings lead to opposite engineering conclusions. This document resolves as much of that as the primary sources allow.

Four findings, in order of consequence:

**There is no ACM v3.0 final text.** What exists is a working draft, dated April 2026 and published by ENISA for public review on 2 June 2026, with the review closing at the end of July 2026. It was read in full. The applicable version today is still v2.0 (April 2025), which is also the version ZKP_08 pins by name. Neither document mentions zero-knowledge proofs, pairings, bilinear maps, BBS, commitments, Merkle trees, extension fields or Fiat–Shamir — not once. The spec's `[UNVERIFIED]` marker can be replaced by a verified statement, but the statement is "the final does not exist", not "the final was read".

**Under the reading the Commission's own technical-specification authors and ETSI actually use, one scheme meets the primitive-level necessary condition and is a candidate — "qualifies" is reserved below for the completed ACM-conformance profile, not this state.** Longfellow ZK (Frigo–Shelat, `google/longfellow-zk`, `draft-google-cfrg-libzk-02`) invokes exactly three cryptographic primitives: SHA-256, AES-256 in counter mode, and — as the statement being proven — ECDSA over P-256. All three are in ACM v2.0. Its security reduces to the collision resistance of SHA-256 in the random-oracle model; everything else in it is information-theoretic. Three evaluator-facing gaps remain, all fixable by parameter and ciphersuite choices rather than by new cryptography: its statistical soundness (~109 bits in the shipped profile, "at least 115" in the draft) is below the ACM's 125-bit "recommended" threshold; it expands Fiat–Shamir challenges with AES-CTR as a bare PRF rather than an SP 800-90A DRBG; and it does part of its arithmetic over GF(2^128), which is not a hardness setting but will pattern-match on the ACM's prime-field-only rule for curves.

**Under the other reading, nothing qualifies and nothing can.** If "rely solely on algorithms included in the ACM" means the proof system must *itself* be a listed mechanism, ZKP_08 is unsatisfiable until the ECCG lists a ZKP — and the v3.0 draft's new Annex C says a mechanism must be standardised, stable for two years and peer-reviewed before it can even be submitted. That is a 2028-plus horizon on any cadence the ECCG has shown, and no proposal to list one exists.

**Designing a new scheme is the wrong lever.** The ACM's curve list forbids the companion-curve trick (Tom-256 / T-256) that every discrete-log-based approach to proving ECDSA depends on; pairings are absent; the q-SDH-type assumptions under BBS# and BBS are not expressible in terms of the listed ECDLP primitive; strong-RSA is ruled out by ETSI in terms. What is left is the hash-based proof-system family — which is where Longfellow already sits, at the performance frontier. The feasible and high-leverage work is not a new scheme but an **ACM-conformance profile** of the existing one: 125-bit soundness, SHA-384 or SHAKE256 (both "recommended" in the v3.0 draft, where SHA-256 and P-256 are demoted to "admissible"), DRBG-based challenge expansion, an explicit assumption map to ACM Note 1, and a submission to the ARF Topic G refinement round that opens on 23 September 2026. Weeks, not years; and the hash-based proof system itself survives the post-quantum transition, which no pairing-based scheme does — though the *statement being proven*, an ECDSA/P-256 credential or device signature, does not: that signature's own security still rests on the classical discrete-log assumption, and no amount of quantum-resistant proving changes what it is a proof of.

---

## 1. What was actually read

Everything below was retrieved on 8 September 2026 from primary sources and saved in full. Where a document could not be reached, this says so.

**ACM v2.0** — "European Cybersecurity Certification Group, Sub-group on Cryptography, Agreed Cryptographic Mechanisms, Version 2.0, April 2025", 53 pages, posted on ENISA's certification site on 6 May 2025 as the "applicable version". Lineage stated in §1.6: "It is base [sic] on the SOG-IS Agreed Cryptographic Mechanisms document."

**ACM v3.0 draft** — file `20260507-acm-draft.pdf`, 56 pages. The cover reads "Version draft document April 2026"; every page carries a "Working Draft" watermark; the "3.0" designation appears only in ENISA's listing and the changelog filename. ENISA's news item of 2 June 2026: "Version 2.0 was adopted mid-2025 and the ECCG subgroup on cryptography … has developed a new version 3 submitted to public review until the end of July 2026." The draft still contains bracketed editorial notes ("[Remark: it would be consistent to remove the use of 100-bit HMAC keys …]"). ENISA's page today still shows v2.0 as applicable and v3 as "Draft for public review". No adoption, ECCG opinion, or later news item was found. **The final does not exist.**

**ARF v3.0.0 Annex 2**, Topic 53, verbatim: *"ZKP_08 (SHALL). A ZKP scheme SHALL rely solely on algorithms included in the [ECCG Agreed Cryptographic Mechanisms v2.0]."* The same "rely solely on algorithms included in" phrase governs scope-rate-limited pseudonyms (PA_23) and cryptographic binding of attestations (ACP_02); the general rule OIA_03 uses "SHALL only use cryptographic algorithms included in". Version history: through ARF v2.7.3 (12 November 2025) the requirement read "algorithms standardised by a standardisation organisation recognised by the Commission or in a standard recognised by the Commission"; the ACM wording appeared in v2.8.0 (2 February 2026) alongside every other ACM reference in the annex, with a change log that says only "Several issues raised via Confluence and GitHub have been resolved". No public rationale.

**The Commission's ZKP specifications** TS4 (ZKP implementation), TS13 (`longfellow-libzk-v1`, arithmetic circuits) and TS14 (multi-message signatures); the Topic G discussion paper v1.4 and the GitHub threads #408, #485/#356, #467; ETSI TR 119 476-1 V1.3.1 (August 2025) and its predecessor V1.2.1; BSI TR-02102-1 (2026-01); ANSSI PG-083 v3.00 (20 March 2026); the June 2024 Cryptographers' Feedback; CIR 2026/1731 and 2026/1735 as far as they touch the ACM.

**The candidate schemes** from their primary sources: Frigo–Shelat ePrint 2024/2010 and the IACR CiC version (4 May 2026), `draft-google-cfrg-libzk-02` (22 July 2026), the `google/longfellow-zk` source at commit `61a8a73` (2 September 2026), the Trail of Bits report (18 August 2025) and ISRG-01; BBS# ePrint 2025/619 and its NIST WPEC 2024 abstract; SAAC ePrint 2025/513; Crescent ePrint 2024/2013; `draft-cllz-cfrg-ecdsa-pop-00` and ePrint 2026/965; Vega ePrint 2025/2094 and `microsoft/vega-prover`; U-Prove Crypto Spec V1.1 Rev 3 and the C# SDK; ACL (CCS 2013); CL01; the HPI/SPRIND cluster (ePrint 2026/330, 2025/1981, 2025/1995).

**Not reachable:** ETSI TS 119 476-2 (STF 705, not published — the Commission's tracker says a stable draft is expected 30 November 2026 and publication 28 February 2027); EN 419 241 (irrelevant here); the BSI 2025 ZKP study (not found); Dutch, Swedish or Estonian NCCA positions on ZKP crypto (not found).

---

## 2. What ZKP_08 requires — the two readings

The requirement is one sentence and the sentence has never been interpreted by anyone with authority to do so. The Commission has not answered the question on GitHub (Orange asked directly on 4 April 2025 — "Did we miss anything?" — and got no reply); no NCCA has published a position; the ACM does not mention ZKPs; and ETSI TS 119 476-2, which is where the Commission's TS13 says "cryptographic validation" will be handed, is not out.

**Reading (a) — the primitives reading.** "Algorithms" means the atomic cryptographic primitives and the hardness assumptions behind them. A proof system qualifies if every cryptographic primitive it invokes, and every hardness assumption its security reduces to, is on the list. Support:

- The ACM's own stated design principle, §1.2: *"A guiding principle in this document is that agreed cryptographic constructions should rely on agreed cryptographic primitives"*, and the layered structure that follows — primitives (§2, §4), constructions (§3, §5), protocols (§6, TLS only).
- ANSSI PG-083 v3.00: the rules *"s'appliquent alors aux problèmes mathématiques sur lesquels ces primitives reposent"* — the method is to evaluate a mechanism by the mathematical problems it rests on.
- ETSI TR 119 476-1's practice throughout: salted-hash mdoc and SD-JWT are "SOG-IS approved" because their components are; CL signatures "are also not possible to construct using SOG-IS approved inputs"; "SOG-IS has not approved the BLS12-381 curves"; BBS# "leverages a SOG-IS approved holder binding cryptographic protocol (ECDSA)". Every one of these is a component-level judgement.
- The Commission's TS13, the only conformance-adjacent sentence in any of the three ZKP specifications: Ligero and Longfellow *"only rely on the security of a hash function such as SHA-256"*.
- The ARF's own word choice: "rely solely on algorithms" for the three constructions (ZKP, pseudonyms, binding) against "only use cryptographic algorithms" for the general case. A drafter who thought of a ZKP scheme as a listable mechanism would have written "SHALL be an agreed mechanism". *(This last point is my inference from the text, not a sourced statement.)*

**Reading (b) — the enumeration reading.** The ACM is a closed list; a mechanism is "agreed" only if listed; a ZKP scheme is a construction, not a primitive, and no ZKP construction is listed; therefore nothing qualifies. Support:

- ACM Note 1-AgreedPrimitive states agreed primitives as a *necessary* condition only: *"In order for a construction to be considered agreed, only agreed primitives shall be used, unless explicitly stated otherwise."* Nothing in the document says a construction built from agreed primitives is thereby agreed. The §5 preamble repeats the necessity direction: "in order for a cryptographic construction to be agreed, it has to be based on agreed primitives."
- The v3.0 draft's new Annex C on adding mechanisms: a candidate must be *"standardised; … stable for at least two years; … [have] a generic purpose and several applications; … security level at least 125 bits; … submitted to peer reviews, security proofs or security analysis by academics, side-channel analysis, tentative of fault attacks"*, and *"a non-standardised mechanism has few chances to be integrated in ACM."*
- CIR 2026/1731 inserts into CIR 2024/2979 an Article 5a under which wallet providers *"shall … use only the cryptographic mechanisms referred to in Annex Ia"* — the ACM — although scoped to the wallet-instance–WSCA channel, not to presentations.
- ETSI's own workshop slides (Elfors, September 2024): zk-SNARKs are "Not yet approved by SOG-IS, ETSI nor the ARF."

**What the regulators actually say sits between the two.** BSI TR-02102-1 (2026): mechanisms not listed *"werden vom BSI nicht zwangsläufig als unsicher beurteilt"*, but *"der Schluss [ist] falsch, dass kryptographische Systeme, die als Grundkomponenten nur … empfohlene Verfahren verwenden, automatisch sicher sind"*, and the recommendations do not pre-empt state evaluation and approval. ANSSI: *"L'évaluation du niveau de robustesse global du mécanisme doit être effectuée avec soin, même si des primitives conformes au référentiel sont employées."* Both say: agreed primitives are necessary, evaluation of the whole construction is still required, and neither the list nor its absence decides the outcome.

**My assessment.** Reading (a) is the one every technical body in the chain actually uses when it writes about specific schemes, and it is the only reading under which ZKP_08 is a requirement rather than a prohibition. Reading (b) is the one a conformity assessor without cryptographic depth will reach for, because it requires no judgement. The correct position for a national specification is therefore not to pick one silently but to state that the scheme meets the *necessary* condition of Note 1 under reading (a), to map every primitive and assumption to an ACM entry, and to require the national certification scheme to record which reading it applies — because under (b) the requirement is void and the scheme owner should be made to say so in writing.

---

## 3. Does any candidate rely solely on ACM algorithms?

The test applied is reading (a) at its strictest: every cryptographic primitive invoked must be an ACM v2.0 entry, and every hardness assumption the security proof reduces to must be one the ACM's listed primitives already embody (ECDLP on a listed curve, integer factoring at listed sizes, collision resistance of a listed hash, PRF-security of a listed cipher). Information-theoretic components carry no assumption and are not counted against a scheme — with a note where an evaluator is likely to count them anyway.

| Scheme | Cryptographic primitives invoked | Assumptions the proof reduces to | Pairings / trusted setup | Issuer signature | Under (a) | Under (b) |
|---|---|---|---|---|---|---|
| **Longfellow / libzk** | SHA-256 (Merkle, FS seed); AES-256-CTR (FS challenge PRF); ECDSA P-256 / SHA-256 (statement) | Collision resistance of SHA-256; ROM | No / No | ECDSA P-256 unchanged | **Yes, with three evaluator-facing gaps (below)** | No |
| **BBS#** (Orange) | P-256 group ops; SHA-256; ECDSA or EC-Schnorr in the SE | DL, DDH, **gap-DL, gap-q-SDH, q-DL**; one-more unforgeability of raw ECDSA (GGM); ROM | No / No | MAC_BBS algebraic MAC (issuer-secret-key; not in any HSM) | **Partial** — hardware side yes; credential layer rests on q-SDH-type assumptions the ACM does not embody | No |
| **SAAC_DDH** (CHLT25) | Prime-order group (curve unspecified); hash for FS; Fischlin-transform oNIP | **DDH only**; ROM | No / No | CMZ14 algebraic MAC via helper server | **Yes on assumptions** — DDH in P-256 is what ECDH/ECIES (both agreed) already rest on; but no device-binding construction, no named curve, helper-server metadata vs ZKP_07 | No |
| **SAAC_BBS** | as SAAC_DDH | Gap q-SDH; ROM | No / No | KVAC-BBS | Partial, as BBS# | No |
| **Crescent** (MSR) | Groth16 over **BN254**; Pedersen; **Poseidon**; SHA-256 | Pairing assumptions (KoE-type); trusted, circuit-specific setup | **Yes / Yes** | ES256 or RS256 | **No** — curve, hash and setup all outside | No |
| **ECDSA-PoP** (`draft-cllz-cfrg-ecdsa-pop-00`) | Pedersen over **BLS12-381**; **Tom-256**; SHA-256 | DL in BLS12-381 and in Tom-256 ("a less-studied curve"); ROM | Credential is BBS on BLS12-381 | BBS | **No** — two unlisted curves | No |
| **Vega** (MSR, 2026) | Hyrax PCS (Pedersen) over **T-256**; **Keccak-256** transcript; SHA-256 | DL in T-256; ROM | No / No | ECDSA P-256 | **No** — unlisted curve; Keccak padding variant unverified | No |
| **U-Prove** (P-256 parameter set) | P-256; SHA-2 | DL; no security proof for Brands blind signatures; ROS attack on concurrent issuance | No / No | Brands blind signature | Primitives yes; **fails ZKP_01/ZKP_07 functionally** — single-show, linkable across uses | No |
| **ACL** (Baldimtsi–Lysyanskaya) | Prime-order group; hash | DDH + DL; ROM | No / No | Abe-type blind signature | Primitives yes; single-show, batch-issuance model | No |
| **CL-RSA / Idemix** | RSA modulus (safe-prime product) | **Strong RSA**; DDH mod n | No / No | CL signature | **No** — "not possible to construct using SOG-IS approved inputs" (ETSI TR 119 476-1 §4.4.1.5) | No |
| **BBS+ / PS** on BLS12-381 | Pairing curves | q-SDH / PS assumption | **Yes** / No | BBS | **No** | No |

### 3.1 Longfellow, examined closely

The claim in the paper is exact and is borne out by the source: *"our proof system itself only relies on SHA-256 as its complexity assumption."* The Merkle tree is SHA-256 (RFC 6234). The Fiat–Shamir transcript ciphersuite is `SHA256-AES256CTR`: the transcript is hashed with SHA-256 and challenges are expanded as `Block[i] = AES256(SEED, ID(i))`. The ECDSA-verification circuit runs over F_p256, the base field of P-256, "because efficiently verifying elliptic curve operations over curve P256 requires the circuit to be defined over a specific 256-bit field"; the SHA-256 and CBOR-parsing circuit runs over GF(2^128) = GF(2)[x]/(x^128+x^7+x^2+x+1) with a GF(2^16) subfield for compact Ligero encoding; the two are linked by an information-theoretic MAC `ax+b` over GF(2^128). The Ligero commitment uses a Reed–Solomon code and the sumcheck is a plain interactive proof. There is no Poseidon, no algebraic hash, no non-standard curve, no trusted setup. The pinned commit also ships ML-DSA-44 test circuits under `lib/circuits/tests/pq/ml_dsa/`, which matters for §5.

Three things an evaluator will raise, none of which is a cryptographic defect:

**Soundness below the ACM's recommended threshold.** ACM §1.1 defines recommended mechanisms as providing "at least 125 bits of security"; §1.3: "100 bits of security are acceptable for legacy mechanisms." The shipped v7 profile is annotated in `mdoc_zk.h` as `kLigeroNreqv7 = 132; // ~109 bits statistical security`; the -02 draft says the (p256, 132, 7) and (GF(2^128), 132, 7) profiles "have been analyzed to provide at least 115 bits of security". Either number places the scheme, by the ACM's own yardstick, in the legacy/admissible band. This is a parameter, not a design: raising `NREQ` (and/or lowering the rate) buys soundness linearly in proof size. What the exact cost is at 125 bits was not measured here.

**Challenge expansion is not a listed DRBG.** AES-256 is agreed; AES-256-in-counter-mode-as-a-PRF is a construction the ACM does not list (its §7 lists HMAC_DRBG, Hash_DRBG and CTR_DRBG per SP 800-90A). Swapping the expander for Hash_DRBG or for SHAKE256 — an XOF the v3.0 draft adds in its new §2.4 — is a ciphersuite change with no effect on the proof system.

**GF(2^128) will be misread.** ACM §4.3: "only elliptic curves defined over prime fields are agreed"; §4.2 restricts finite-field discrete logarithms to GF(p). Both rules govern *hardness settings*. Longfellow's binary field carries no hardness assumption — it is the arithmetic in which a circuit is expressed and a code is evaluated — but a conformity assessor who greps for "GF(2" will find it and will need the distinction explained in the security target. The alternative, running the SHA-256 circuit over F_p256, is precisely what the binary field was chosen to avoid.

Two independent reviews exist. Trail of Bits (July 2025) found 13 issues, two High — the circuit ID not being checked on deserialisation, and an mdoc attribute-check bypass — plus an undetermined-severity finding that the ECDSA circuit allowed off-curve intermediate points; none concerns the choice of primitives. ISRG-01 (October 2025, fixed in v0.8.4) was an under-constrained CBOR index in the hash circuit. A Ligero-commissioned academic analysis (December 2025) states "two main security theorems". This is more independent scrutiny than any other candidate has had.

### 3.2 BBS#, examined closely

The authors' claim is carefully scoped and is accurate as scoped: BBS# *"only depends on the hardware implementation of well-known digital signature schemes such as ECDSA (ISO/IEC 14888-3) or ECSDSA … using classical elliptic curves"*; the NIST WPEC abstract adds "listed in the SOG-IS Crypto Working Group document on agreed cryptographic mechanisms". That is the holder-binding half. The credential half is an algebraic MAC (`A = (Cm)^{1/(skI+e)}`) whose unforgeability is proven under gap-q-SDH, gap-DL and q-DL in a pairing-free group — assumptions the paper itself describes as "In our context, we have no pairings, but a DDH oracle provided by our gap-DL and gap-q-DL challengers." The ACM lists ECDLP on named curves; it does not embody q-SDH, and nothing in it lets an assessor treat q-SDH-in-P-256 as agreed by virtue of P-256 being agreed. This is the same structure ETSI uses to rule out CL-RSA: RSA is listed, strong-RSA is not, so CL "cannot be constructed from SOG-IS approved inputs". Two further costs: the issuer-side MAC is supported by no HSM (TS4: "the operations required from the Provider side are not currently supported by existing Hardware Security Modules"), and public verifiability needs either an online issuer or pre-fetched blind DLEQ proofs, which TS14 flags against ZKP_07 on metadata grounds. TS14 also records that Cheon-type attacks reduce q-SDH security below the nominal curve level for BBS; whether P-256's group order gives that attack traction against BBS# specifically was not verified here.

### 3.3 The answer to the question as asked

*Do any ZK candidate schemes currently exist that rely solely on algorithms in ACM v2.0?* Under reading (a): **yes — Longfellow**, with the three parameter-level gaps above; **SAAC_DDH** on its assumptions but not as a deployable system; **BBS#** for its hardware half only. Under reading (b): **no, and none can** until the ECCG lists a ZKP mechanism. The spec's flat "no candidate qualifies" is therefore true only under (b) and should not be stated without saying so.

---

## 4. The structural result: the ACM collapses the design space

This is the finding that decides whether a new scheme is worth designing, so it is worth stating precisely.

Proving an ECDSA-P-256 verification in zero knowledge requires arithmetic in F_p, the base field of P-256. There are exactly three families of proof system in the literature, and the ACM's curve list treats them very differently.

**Discrete-log-based proof systems** (Bulletproofs, Hyrax, Σ-protocols, Spartan-with-Pedersen) need a group whose *scalar* field is F_p. P-256's own scalar field is F_n, not F_p. The only way to get native arithmetic is a companion curve whose order equals p — Tom-256 (used by ZKAttest, CDLS and `draft-cllz-cfrg-ecdsa-pop`) or T-256 (used by Vega). Neither is on the ACM list, and §4.3 says only the listed prime-field curves are agreed. `draft-cllz` itself concedes the companion curve "introduces a discrete-log assumption over an additional, less-studied curve". The non-native alternative — emulating F_p arithmetic inside F_n — is what Crescent does to reach 2.6 million constraints. **This family is closed under the ACM.**

**Pairing-based systems** (Groth16, KZG/PLONK, BBS, PS) need BN254, BLS12-381 or BLS24-509. None is listed; "pairing" and "bilinear" occur zero times in either ACM version; TS14 notes the IETF pairing-curve draft expired in 2023; ETSI records that SOG-IS "has not approved the BLS12-381 curves". **Closed.**

**Hash-based systems** (Ligero, Brakedown, FRI/STARKs, Binius, and MPC-in-the-head / VOLE-in-the-head) need only a hash function and, optionally, a PRF. They can be instantiated over *any* field, including F_p256 natively, because the commitment is a Merkle tree over codeword symbols rather than a group element. Their soundness is statistical; their only computational assumption is collision resistance in the random-oracle model. Every primitive they need is listed. **Open — and it is the only family that is.**

Two corollaries. First, Longfellow is not one option among several; it is the reference point of the only admissible family, and its architecture (F_p256 for the curve arithmetic, a small binary field for the hash circuit, a hash-based IOP over both) is close to forced by the constraints. A "new" ACM-conformant scheme would be a new instantiation of the same family, competing with Longfellow on prover time, proof size and evaluator legibility. Second, the hash-based family is the only one that survives the post-quantum transition the ACM v3.0 draft is organised around: it demotes every classical asymmetric mechanism, P-256 and ECDSA included, to "admissible", and keeps only PQC, AES-192/256, 384/512-bit hashes and SHAKE256 at "recommended". A hash-based IOP proves ML-DSA verification the same way it proves ECDSA verification — the pinned Longfellow commit already carries ML-DSA-44 circuits — whereas a pairing- or DL-based scheme is tied to a curve that the same document is retiring.

---

## 5. Feasibility of a new scheme that "fully qualifies"

### 5.1 Under reading (b): not a design problem

Nothing that can be drafted qualifies under (b). What qualifies is an ACM listing, and the v3.0 draft's Annex C sets the entry conditions: standardised, stable for two years, generic purpose with several use cases, ≥125-bit security, peer-reviewed with side-channel and fault analysis. Longfellow is the only candidate with a standards track (`draft-google-cfrg-libzk`, and ISO/IEC JTC 1/SC 17 WG10 for mdoc ZKPs per ETSI); BBS# is "being standardised by AFNOR" and ISO/IEC AWI 24843. None is a published standard today. **[INFERRED]**, assuming the observed ECCG cadence continues and that a qualifying standard must exist for two years before it can even be submitted for listing: on the ECCG's demonstrated cadence — v1.3 in 2023, v2.0 in 2025, v3 drafted in 2026 — the earliest a ZKP could appear is a v4 in 2028 or later, and only after a standard has existed for two years. **Nobody in Estonia can shorten that by writing cryptography.** They could shorten it by contributing to the CFRG draft and by asking their NCCA to raise the question in the ECCG sub-group, which is a different kind of work.

### 5.2 Under reading (a): not needed

A hash-based IOP with SHA-256/AES-256 already meets the necessary condition. The productive question is what would make an NCCA evaluator *comfortable* signing off on it, and the answer is a **conformance profile** — a document plus a ciphersuite plus a parameter set — rather than a new scheme. Its contents, each traceable to an ACM clause:

**Primitives, chosen from the v3.0 "recommended" tier.** Merkle hash: SHA-384 or SHA3-384 (R in v3; SHA-256 becomes A). Fiat–Shamir seed: the same hash. Challenge expansion: SHAKE256 (new §2.4, R) or Hash_DRBG/SHA-384 (§7, R) — retiring AES-CTR-as-PRF. Circuit identifier: SHA-384 of the circuit. The statement proven — ECDSA-P-256/SHA-256 over an ISO 18013-5 MSO — uses mechanisms that are A in v3; the justification the ACM itself gives for A mechanisms, "backward compatibility", applies literally: these are the algorithms the issued credentials and certified secure elements already use, and the wallet cannot change them.

**Soundness at 125 bits.** Raise `NREQ` and/or lower the Ligero rate until the analysed soundness, in both the F_p256 and GF(2^128) profiles, meets ACM §1.3's recommended level. Measure proof size and prover time before and after on the reference device class in EE-EUDIW-TS §18. This is the one item with a real performance cost and it should be quantified, not assumed.

**An assumption map to Note 1.** One table: each component of the scheme → primitive or information-theoretic → ACM section → R/A status → the theorem in the paper or the CiC version that reduces to it. The binary field, the Reed–Solomon code, the sumcheck, and the cross-field MAC each get a line saying "no computational assumption; statistical soundness error ≤ 2^-125 under parameters above". This is the document that turns "GF(2^128)" from a red flag into a footnote.

**Evidence package.** The Trail of Bits and Ligero reviews; the ISRG finding and fix; the CiC peer-reviewed version; `draft-google-cfrg-libzk` at its current revision; and the observation that CIR 2024/2981 Art. 4(2)(c) lets a national scheme refer to "technical specifications that meet the requirements set out in Annex II to Regulation (EU) No 1025/2012" — the door through which an IETF RFC or ISO TS enters certification without waiting for the ACM.

**Effort.** The profile document and the assumption map are weeks of work for one person who has read the paper and the code. The ciphersuite is a contained change in `lib/merkle`, `docs/specs/fs.md` and the transcript code, with a test-vector update; the ISRG Rust port would need the same. The parameter change is a constant plus a benchmark. Getting the profile *adopted* — into the CFRG draft, into TS13's ciphersuite registry, and in front of an NCCA — is the long pole, and it is a standards-process pole, not a research one.

**Where to put it.** The ARF Topic G refinement round is scheduled for **23 September to 18 November 2026**. That is the venue for two things: a proposed clarification of ZKP_08 — that "rely solely on algorithms" is to be read at the level of primitives and hardness assumptions per ACM Note 1, with the ACM's security thresholds applied to the scheme's soundness — and the conformance profile itself as evidence that the requirement is satisfiable. The ACM v3 public review closed at the end of July 2026 and cannot be used. Estonia's NCCA is the route into the ECCG sub-group for anything longer-term.

### 5.3 What a genuinely new scheme would have to beat

If someone nonetheless wanted to design a new scheme rather than profile the existing one, the bar is: ACM-only primitives (so hash-based or MPC-in-the-head), native F_p256 arithmetic, proving an ECDSA-P-256/SHA-256 mdoc signature with device binding, on a mid-range phone, in under the ~800 ms the current Longfellow profile achieves on a Pixel 6 Pro (TS13 §3.3), with a proof small enough for a QR-scale proximity flow, transparent setup, and a security proof in the ROM with ≥125-bit soundness. MPC-in-the-head (the FAEST lineage, provably secure from AES and SHA alone) satisfies the primitive constraint most cleanly of all but pays for it in proof size and prover time on circuits of this scale; Binius-style binary-tower systems would be faster but more exotic to an evaluator than the thing they replace. A solo researcher could produce a paper in this space; producing something a CAB would prefer to a reviewed, standards-tracked implementation with two independent audits is a multi-year effort with an uncertain payoff. **Recommendation: do not.**

---

## 6. Consequences for EE-EUDIW-TS §10.3

The opening paragraph of the current §10.3 should be replaced; the two existing observations (Longfellow's primitive-level reading, and the Cheon-attack note on BBS/BLS12-381) and EE-ZKP-003/004 stay. Proposed wording for the opening, and two new requirements numbered after the existing EE-ZKP-004:

> ### 10.3 The ZKP_08 problem, stated precisely
>
> `ZKP_08` requires a ZKP scheme to "rely solely on algorithms included in the [ECCG Agreed Cryptographic Mechanisms v2.0]". The ACM (v2.0, April 2025; a v3.0 working draft was published for review on 2 June 2026 and has not been adopted) lists no zero-knowledge proof system, no pairing-friendly curve, no commitment scheme and no extension field, and its Note 1 makes agreed primitives a necessary but not a sufficient condition for a construction to be agreed. No authority has interpreted the requirement. Two readings exist:
>
> **(a)** the requirement is met when every cryptographic primitive invoked, and every hardness assumption the security proof reduces to, is an ACM entry. This is the reading the Commission's TS13, ETSI TR 119 476-1 and ANSSI PG-083 apply in practice. Under it, a hash-based proof system whose only assumptions are the collision resistance of a listed hash and the random-oracle model qualifies; Longfellow ZK (SHA-256, AES-256, ECDSA/P-256) is such a system, subject to raising its soundness to the ACM's 125-bit recommended level and replacing AES-CTR challenge expansion with a listed DRBG or XOF.
>
> **(b)** the requirement is met only by a proof system that is itself a listed mechanism. Under it nothing qualifies, and — given ACM v3.0 draft Annex C's entry conditions — nothing can before roughly 2028.
>
> **EE-ZKP-005 (SHALL).** The Estonian national certification scheme SHALL record which reading of `ZKP_08` it applies. Where it applies reading (a), the security target for the ZKP profile SHALL contain an assumption map tracing every component to an ACM entry or to an information-theoretic soundness bound of at least 2^-125. Where it applies reading (b), the scheme SHALL state that `ZKP_08` is unsatisfiable at the time of evaluation and SHALL NOT treat the absence of a listed ZKP as a finding against the wallet.
>
> **EE-ZKP-006 (SHALL).** The Track A profile (§10.5) SHALL be instantiated with mechanisms marked *recommended* in the current ACM for every component the wallet controls — hash, XOF or DRBG, and soundness parameters — and SHALL document the *admissible* status of ECDSA/P-256 and SHA-256 in the statement being proven as a backward-compatibility dependency on issued credentials and certified WSCDs.
>
> Pairing-based schemes (BBS, PS, Groth16) and schemes needing a companion curve (Tom-256, T-256) do not qualify under either reading and SHALL NOT be adopted for Track A. BBS# qualifies for its holder-binding half only; its credential layer rests on q-SDH-type assumptions the ACM does not embody.

The two `[UNVERIFIED]` markers in the current §10.3 come out: the v3.0 draft was read in full and contains nothing on ZKPs or pairings; the v3.0 final was not read because it does not exist. §23 item 6 should say so.

---

## 7. What remains unverified

The cost in proof size and prover time of raising Longfellow's soundness from ~109/115 to 125 bits — a parameter change, measurable in the pinned repo, not measured here. Whether Cheon-type attacks on q-SDH have traction against BBS# instantiated on P-256. Whether Vega's `Keccak256Transcript` uses SHA3-256 padding or Ethereum Keccak padding. The contents of ETSI TS 119 476-2, which is unpublished. Whether any NCCA has an unpublished position on ZKP evaluation. Whether `draft-google-cfrg-libzk-03` (a working file dated 26 August 2026 in the repo) has been submitted to the datatracker. Whether Estonia's NCCA has a seat or a channel into the ECCG sub-group on cryptography.

---

## Sources

ENISA, *EUCC Guidelines on Cryptography* page and the ACM v2.0 (April 2025), ACM v3 draft (April 2026, published 2 June 2026) and changelog (12 May 2026) · ENISA news, 2 June 2026, "Participate in the public review of the new draft Agreed Cryptographic Mechanisms" · EUDI ARF v3.0.0 Annex 2 (21 July 2026), v2.7.3 and v2.8.0 · Topic G Discussion Paper v1.4 and GitHub discussions #408, #485/#356, #467 · Commission TS4, TS13, TS14 · ETSI TR 119 476-1 V1.3.1 (2025-08) and V1.2.1 (2024-07) · ANSSI PG-083 v3.00 (20 March 2026) · BSI TR-02102-1 (2026-01) · Cryptographers' Feedback on the ARF (19 June 2024) · CIR (EU) 2026/1731, 2026/1735 · Frigo & Shelat, ePrint 2024/2010 and IACR CiC 3(1), 4 May 2026 · `draft-google-cfrg-libzk-02` (22 July 2026) · `google/longfellow-zk` at `61a8a735` (2 September 2026) · Trail of Bits report (18 August 2025); ISRG-01 · Desmoulins, Dumanois, Kane & Traoré, ePrint 2025/619; NIST WPEC 2024 abstract · Chairattana-Apirom, Harding, Lysyanskaya & Tessaro, ePrint 2025/513 · Paquin, Policharla & Zaverucha, ePrint 2024/2013 · Celi, Lehmann, Levin & Zacharakis, `draft-cllz-cfrg-ecdsa-pop-00` and ePrint 2026/965 · Kaviani & Setty, ePrint 2025/2094 and `microsoft/vega-prover` · U-Prove Cryptographic Specification V1.1 Rev 3 and `uprove-csharp-sdk` · Baldimtsi & Lysyanskaya, CCS 2013 · Camenisch & Lysyanskaya, ePrint 2001/019 · Bormann & Lehmann, ePrint 2026/330; ePrint 2025/1981, 2025/1995.

*Every version number, date, quotation and code constant above was checked against the named source on 8 September 2026. Items in §7 are labelled unverified and should be treated as such.*
