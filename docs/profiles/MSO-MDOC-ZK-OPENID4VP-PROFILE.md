# `mso_mdoc_zk` for OpenID4VP — a profile of the de-facto ZK carrier

**Date:** 24 September 2026. **Status:** proposal. Nothing in this document is normative
anywhere yet: the carrier is in no specification (OpenID4VP 1.0 Final and the 1.1 editor's
draft have no ZK text; HAIP has none), DCHP issue
[#17 "ZKP Support"](https://github.com/openid/dchp/issues/17) is still in the parking lot,
and `draft-google-cfrg-libzk` is an unadopted individual draft. This profile pins down what
the shipping implementations actually agree on, so that a standardisation venue has a
tested target to converge on rather than a blank page.

**Inputs:** [`docs/analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md`](../analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md)
(the wire shape as multipaz 0.99.0 serialises it),
[`docs/planning/STEP8-7-RECORD.md`](../planning/STEP8-7-RECORD.md) (verifier side,
fixture-verified) and the fork's `docs/planning/STEP8-7-WALLET-RECORD.md` (wallet side).
**Evidence base:** every wire claim below is either fixture-verified against a multipaz
0.99.0-generated `DeviceResponse` (`verifier/go/zk/testdata/step8-7-openid4vp-zk/`, asserted
through the Go test `TestStep87OpenID4VPZkVerify`) or read directly from the implementation
code; anything else carries **[UNVERIFIED]** inline, per the repository's evidence discipline.
The real-device interop diff is PENDING-DEVICE (plan §8.7); until it runs, byte-level claims
are host-side facts about multipaz 0.99.0, not about every shipping wallet.

---

## 1. Scope

One question: *what do Google Wallet, Multipaz, SUNET/vc and SIROS actually put on the wire
when they carry a Longfellow zero-knowledge proof over OpenID4VP, and what minimum contract
makes that traffic safe?* The four implementations ship the same carrier — a DCQL credential
format `mso_mdoc_zk` answered by a `DeviceResponse.zkDocuments` list — but no document
defines it. This profile is that document, written from this repository's fixture-verified
implementation (plan §8.7) and the fork's wallet side.

Out of scope: proximity transport (§11.3 of the specification keeps ISO/IEC 18013-5 device
retrieval there), issuance, and the ISO 18013-7 Annex C dcapi carrier, which shares one
`ZkDocument` encoding with this profile but rides a different request/response envelope
(step 2a).

## 2. The format identifier

**Profile rule 1.** The DCQL credential format identifier is the string `mso_mdoc_zk`.

- It is what the de-facto implementations send in `credentials[].format`
  (`verifier/go/oid4vp/dcql.go` `FormatMsoMdocZk`; the fork's
  `DeviceRequestParser.FORMAT_MSO_MDOC_ZK`). The sibling ISO/IEC 18013-5 second-edition
  mechanism — `zkRequest.systemSpecs` inside a `DeviceRequest` — uses **no** DCQL format at
  all; only this OpenID4VP carrier needs an identifier.
- **There is no registry to put it in yet.** OpenID4VP 1.0 Final (9 July 2025) names exactly
  three credential formats (`mso_mdoc`, `dc+sd-jwt`, `jwt_vc_json` — checked against the
  Final text on 24 September 2026) and its IANA Considerations (Appendix E) registers only
  `vp_token` response types: 1.0 created **no** credential-format registry, so there is no
  IANA or Designated-Experts home for `mso_mdoc_zk` to be filed into. Registration is one of
  the things DCHP #17 would have to create (see
  [`DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md`](DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md)).
- Until a registry exists, conformance to this profile means: the identifier is spelled
  exactly `mso_mdoc_zk`, is case-sensitive, and a query naming an unknown format is refused,
  never approximated (this verifier's `Single()` refuses anything but `mso_mdoc` and
  `mso_mdoc_zk`; EE-ZKP-051's fail-closed discipline on the wallet side).

## 3. `meta.zk_system_type`: the parameter registry

**Profile rule 2.** An `mso_mdoc_zk` credential query MUST carry
`meta.doctype_value` (a valid ISO/IEC 18013-5 doctype identifier, same rule as `mso_mdoc`)
and a non-empty `meta.zk_system_type` array. A query that advertises no proving system
cannot be allowlisted and is malformed, not merely unanswerable (`dcql.go` `Single()`;
the fork's parser fails closed the same way — `MsoMdocZkParsingTest` case 5).

**Profile rule 3.** Each `zk_system_type` entry carries exactly the fields the two shipping
implementations agree on, and they are the fields the Estonian ZK Circuit Registry (EE-ZKP-030)
already publishes:

| Field | Type | Meaning | Verified against |
|---|---|---|---|
| `system` | text | proving-system identity string | fixture + multipaz 0.99.0 bytecode |
| `id` | text | circuit-set identifier; the de-facto label format is `<system>_<version>_<num_attributes>_<block_enc_hash>_<block_enc_sig>_<circuit_hash>` | fixture, `multipaz-circuits.txt`, `circuits.Circuit.SpecID()` |
| `circuit_hash` | text | hash of the circuit (hex; e.g. `8d079211…2121` for v7/1-attribute) | fixture, step 0 |
| `num_attributes` | number | attribute count the circuit proves (1–4 in the shipped sets) | `multipaz-circuits.txt` |
| `version` | number | circuit version (shipped: 6 and 7) | `multipaz-circuits.txt` |
| `block_enc_hash` | number | Longfellow block size, hash field | fixture |
| `block_enc_sig` | number | Longfellow block size, signature field | fixture |

The multipaz 0.99.0 bundle ships eight such circuits (v6 and v7, 1–4 attributes,
`verifier/go/zk/testdata/step0-multipaz/multipaz-circuits.txt`); the v7 1-attribute circuit
this profile's fixture exercises is
`longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121`.

**Profile rule 4 (the `system` string).** Two system strings are in live use for the same
underlying scheme, and both are fixture-verified in this repository:

- `longfellow-libzk-v1` — what a de-facto wallet puts in `zk_system_type.system` and in the
  response's `zkSystemId` (multipaz `LongfellowZkSystem.getName()`, verified in the 0.99.0
  bytecode and in the generated fixture). **This is the string this profile registers** for
  the OpenID4VP carrier, because it is what the shipping wallets emit.
- `org.iso.mdoc.zk` — what the ISO dcapi `DeviceRequest` path names the same system in its
  `zkRequest.systemSpecs` (`isodcapi.go` `DeviceRequest`/`SystemName`, step 2a fixture).

Receivers conforming to this profile accept either label and MUST NOT key trust on it: the
allowlist matches on `id` + `circuit_hash` + the numeric tuple, never on the label
(`oid4vp.ZkSystemTypeAllowlist` in `dcql.go`; the fork maps entries into the same
`ZkSystemSpec` model regardless of source). A one-label policy with label-keyed trust would
be a weaker contract than the implementations already enforce.

**Profile rule 5 (allowlisting).** Every advertised entry MUST match a registry-listed
circuit on **all** of `id`, `circuit_hash`, `version`, `num_attributes`,
`block_enc_hash`, `block_enc_sig`, checked before any proving/verifying work; every entry is
checked, not just the first, so a query advertising one accepted and one unknown circuit is
refused in full (EE-ZKP-023's check-before-verify; `ZkSystemTypeAllowlist`; the fixture test
`TestStep87UnregisteredCircuitRefusal` pins the refusal, including a tampered
`circuit_hash` under a real `id`). This is also a resource rule: the match is against
published entries only, and no holder-supplied number ever reaches the FFI hash computation
(the registry's S11 inversion fix).

## 4. `meta.verifier_message`

**Profile rule 6.** `meta.verifier_message` is an OPTIONAL text field whose semantics are
**undefined**. It appears in Google's example request; no document defines what a wallet
should do with it. Both halves of this project carry it and never act on it: the Go side
parses it into `Meta.VerifierMessage` and ignores it; the fork does not parse it at all
(a verifier-sent instruction is not a rule the wallet obeys from the wire).

This profile registers the field as **reserved**: receivers MUST ignore it, and its
semantics — a display string for the holder? a policy hint? nothing? — are explicitly
tabled for DCHP #17. Until defined there, any wallet behaviour triggered by the field is a
profile violation, because an unauthenticated verifier instruction that changes wallet
behaviour is a downgrade surface (the same reasoning the EE-PRO-013 origin check applies to
platform-asserted origins).

## 5. Freshness: `ZkDocumentData.timestamp`

The response (§7) carries, inside each `zkDocument`'s tag-24 `documentData`, a `timestamp` —
the wallet's own claim about when the document was assembled. multipaz 0.99.0 serialises it
as CBOR tag 0 (standard date/time) over an RFC 3339 text string, seconds precision, no
fractional seconds (both fixture-pinned, `isodcapi.go` `ParseZkDocument`; fractional seconds
are refused per ISO/IEC 18013-5 clauses 7.1 and 9.1.2.4).

**Profile rule 7 (the freshness window).** The verifier MUST window-check the presented
`timestamp` against its own clock: the value MUST lie within
`[session_created − 60 s, now + 60 s]` — the ISO dcapi path's `CheckTimestampWindow`
window with its 60-second slack (`SessionTZ`), which plan §8.7 adopted as the starting
point for this transport. This repository's OpenID4VP verification (the presenter's `checkZk`,
rule (a) in `present.go`) applies it unchanged; that path is library-level, since
`/present/new` does not yet issue `mso_mdoc_zk` queries (`verifier/README.md`). The slack absorbs clock skew between wallet and verifier, not
replay margin.

**Profile rule 8 (the proof binds the timestamp).** On this carrier the Longfellow proof
takes `ZkDocumentData.timestamp` as its `Now` input — the proof itself establishes (d) of
EE-ZKP-021, that the attestation is within validity, *as of the wallet's own timestamp*.
The window check is therefore load-bearing, not decorative: it is what keeps a stale
document from being replayed with its proof intact. The fixture's
`request.json` records the proof-bound timestamp (`2026-09-24T14:19:58Z`) and the Go test
re-derives the window from it.

**Profile rule 9 (session freshness outside the document).** The verifier's own session
controls (nonce single-use via `Store.Claim`, session expiry) apply on this path exactly as
on the plain path; the carrier changes what the response looks like, not when a response
may be accepted. A proof made for another `client_id`, another `response_uri` or another
nonce will not verify (§6), and the fixture test asserts the cross-transport replay refusal
directly: a proof bound to the OpenID4VP handover does NOT verify over the ISO dcapi
transcript, and vice versa.

## 6. Transcript binding (stated here because DCHP #17 asks)

No new transcript machinery is needed for ZK over OpenID4VP. The proof binds the **same**
ISO/IEC 18013-5 `SessionTranscript` the plain mdoc response uses, with the OpenID4VP
handover:

```
SessionTranscript = [null, null, ["OpenID4VPHandover", h]]
h = SHA-256(cbor([client_id, nonce, jwk_thumbprint, response_uri]))
```

— OpenID4VP 1.0 Appendix B.2.6.1 (invocation via redirects), with the third handover
element the RFC 7638 JWK thumbprint of the response-encryption key under `direct_post.jwt`,
and `null` for the unencrypted `direct_post` case (the fixture uses `null`;
`transcript.go` implements both, golden-vector tested). This construction is
**fixture-verified in both directions** against multipaz 0.99.0: a proof generated over the
hand-built B.2.6.1 transcript verifies under the Rust runtime, and does not verify over the
ISO dcapi transcript (first check anyone has done of these bytes — SUNET's own note
records that their transcript was never verified against a real device's; that device diff
stays PENDING-DEVICE). For a DC-API-transported request the analogous construction is
Appendix B.2.6.2 `OpenID4VPDCAPIHandover` — implemented in this repository only as far as
the plain path's `ISOTranscript`, not yet ZK-proven over this carrier [UNVERIFIED for the
ZK case; PENDING-DEVICE].

Profile rule 10 follows: **transcript bytes are a verifier-side secret in construction, not
in content** — `client_id`, `nonce` and `response_uri` come from the stored session, never
from the response (`present.go` `checkZk`, `Session.Transcript()`), because a transcript the
response can influence is a transcript the response can bind to something else.

## 7. Response shape and response-mode sizing

**Response shape (fixture-pinned).** `vp_token[<credential id>]` is a **base64url CBOR
`DeviceResponse`** carrying a top-level `zkDocuments` array; each entry is
`{ proof: bstr, documentData: #6.24(bstr ZkDocumentData) }`, and `ZkDocumentData` is
`{ zkSystemId, docType, timestamp, issuerSigned, deviceSigned, msoX5chain }`. The
`zkSystemId` lives inside `ZkDocumentData` (not on the zkDocument); `timestamp` is tag 0;
`msoX5chain` is a bare bstr for a single certificate, an array of bstrs for a longer chain.
Zero zkDocuments is a *legal empty answer* ("presented nothing provable", served
`valid:false`), more than one is refused, and the doctype must match the query
(`vptoken.go` `ParseZkVPToken` / `MatchQueryZkDocument`). Holder-controlled bytes keep the
strict-CBOR discipline (ADR-002): every shape deviation is a refusal, never a fallback.

**Sizes (measured, committed fixture, 24 September 2026):**

| Quantity | Bytes |
|---|---|
| `device_response.cbor` (the whole vp_token value) | 360,859 |
| `zkDocuments[0].proof` (the Longfellow proof bstr) | 360,180 |

**Profile rule 11 (response modes).** Because one credential's answer is ~360 KB of CBOR —
~481 kB once base64url-encoded into the JSON `vp_token` — the redirect-channel response
modes are constrained as follows:

- **`direct_post.jwt` is the mandatory response mode** for this carrier where the
  ecosystem mandates response encryption (HAIP 1.0, and therefore EE-PRO-001 for the
  Estonian ecosystem: `direct_post.jwt` for redirect-based presentation, ECDH-ES response
  encryption mandatory). `direct_post` (unencrypted) is tolerated only where the ecosystem
  permits it — this PoC uses it, which is why the fixture's `jwk_thumbprint` is `null`; a
  conforming Estonian deployment sets the thumbprint, which changes `h` and hence the
  transcript (§6), so the mode choice is made **before** the request is issued, never
  negotiated after the proof exists.
- **Front-channel redirect modes are excluded by arithmetic, not policy:** a ~481 kB
  base64url payload cannot ride a query string or fragment.
- **Proximity is out of scope for this profile** (§1), but the number matters there too: a
  ~400 KB proof is §23 item 24's unmeasured BLE figure — mso_mdoc_zk does not solve
  EE-ZKP-044's transport budget, and this profile MUST NOT be cited as if it did.

## 8. Coexistence with BBS/JWP (Track B)

The wallet must one day carry both this carrier and the Commission's TS14 multi-message-
signature track (BBS-family credentials in a JWP container, converging in ETSI TS 119
476-2). The profile's coexistence rules, each grounded in what step 8.7 actually built:

- **Rule 12 — one format identifier per scheme family.** `mso_mdoc_zk` is mdoc+Longfellow
  by construction: `ZkDocumentData` carries `issuerSigned`/`deviceSigned` mdoc structures.
  TS14 §2.4 declares mdoc and SD-JWT VC structurally incompatible with MMS ZKPs and proposes
  a JWP-based container — that container, when it exists, is a **different** DCQL format
  with its own `system` registry, not a new value of `zk_system_type.system` inside
  `mso_mdoc_zk`. A response mixing schemes in one credential answer is refused (one
  zkDocument per query, §7).
- **Rule 13 — scheme identity rides the presentation, in the same place.** EE-ZKP-004's
  crypto-agility requirement ("scheme identity carried in the presentation") is met on this
  carrier by `zkSystemId` in the response and `zk_system_type.system` in the query; a Track
  B format must put its identity in the analogous slot so a verifier dispatches on format
  and system before any parse or FFI work, exactly as `present.go` dispatches on
  `cq.Format`.
- **Rule 14 — one refusal path, scheme-agnostic.** The wallet's EE-ZKP-051 refusal and the
  per-doc-request spec scoping run on the same `ZkSystemSpec` model regardless of which
  transport or scheme delivered the specs (the fork's DCQL parser feeds the same map the
  ISO path uses) — a second scheme plugs into the step 4c `ZkPresenter` seam without a
  second consent or refusal code path.
- **Rule 15 — the issuer-disclosure consent line is carrier-independent in wording.** The
  `msoX5chain` disclosure (below) is this carrier's instance of a general fact: ZK proofs
  can hide claims and still reveal the issuer. The fork's consent line ("The issuer of this
  attestation will be visible to the requesting party", EE-ZKP-042 discipline) applies to
  any scheme whose presentation carries issuer-identifying material.

**Constraint to carry forward (both tracks):** mso_mdoc_zk reveals `msoX5chain` — the
verifier learns who issued the credential even though the claims are zero-knowledge (the
plan's S6 per-batch-certificate concern applies verbatim). BBS-family Track B can hide the
issuer; that is a privacy *advantage* of Track B this profile does not pretend away, and it
is one more reason EE-ZKP-011 keeps Track B on the roadmap.

## 9. What is not settled (kept visible, per §23 discipline)

- **Real-device interop** (a real Google Wallet/Multipaz wallet against this verifier over
  HTTP, transcript bytes diffed): PENDING-DEVICE. Everything above is host-verified against
  multipaz 0.99.0.
- **The DC API transport's request field** carrying the DCQL query on real Chrome/Google
  Wallet requests: the fork reads `dcqlQuery`, which matches the OpenID4VP request-object
  field name, but no device capture exists — PENDING-DEVICE (fork record item 2).
- **Google Wallet verifier documentation, SUNET/vc PR #576, SIROS SDK PR #99** as carriers
  of the same shape: Google's docs page and the Multipaz source tree were checked as
  recorded in the analysis note of 24 September 2026. SUNET/vc PR #576 was re-read on
  25 September 2026: merged 20 August 2026, it adds `mso_mdoc_zk` verification to
  vc-verifier and matches `zk_system_type`. SIROS SDK PR #99 stays **[UNVERIFIED]**.
- **OpenID4VP 1.1 editor's draft state** (no ZK text as of the 23 September 2026 commit the
  analysis read): **[UNVERIFIED]** beyond that read. The 1.0 Final text was re-checked
  directly for this profile on 24 September 2026 (no ZK text, no credential-format
  registry).
- **The EU Age Verification Profile Annex A** statement that ZKP is routed to the DC API
  because "no standardized DCQL query for requesting Zero-Knowledge Proof is available":
  quoted from the analysis note; **[UNVERIFIED]** against the AV profile text in this
  session. (The AV profile's own §A.8 `longfellow-libzk-v1` selection is verified — spec
  §10.4.)
- **Proof size across wallets and devices:** one fixture, one circuit (v7/1), one host. The
  ~360 KB figure is an order of magnitude for the 1-attribute case, not a distribution.
- **Soundness floor:** the shipped circuits are v7 at 109-bit soundness
  (`kLigeroNreqv7 = 132`), below the ACM v2.0 recommended 125 bits — EE-ZKP-022a's pilot
  constraint binds this carrier exactly as it binds the ISO path. Nothing here changes
  ZKP_08's status.

## 10. References

- `docs/analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md` — the carrier analysis this profile
  operationalises (§5 of that note lists the seven gaps; this document closes them).
- `docs/planning/STEP8-7-RECORD.md` — verifier-side record: DCQL parsing, allowlist,
  response path, fixture, findings.
- Fork `eudi-wallet-poc` branch `step8-7-wallet-side`, `docs/planning/STEP8-7-WALLET-RECORD.md`
  — wallet side: DCQL parsing into `ZkSystemSpec`, ECDSA-only device auth, consent line.
- Fixture: `verifier/go/zk/testdata/step8-7-openid4vp-zk/` (`GENERATED-BY` names the
  generating test; `request.json` the handover parameters).
- Code: `verifier/go/oid4vp/dcql.go` (format, meta, allowlist), `vptoken.go` (response
  parse), `transcript.go` (B.2.6.1), `isodcapi.go` (`ZkDocument`, `CheckTimestampWindow`),
  `verifier/go/circuits/registry.go` (circuit registry), `verifier/go/present.go` (`checkZk`).
- Specification hooks: EE-ZKP-004, -011, -020, -021, -022a, -023, -030, -032, -042, -045,
  -051; EE-PRO-001, -002, -003, -010a, -013; EE-ZKP-044 budget and §23 items 24–25.
- Venue: OpenID DCHP issue [#17 "ZKP Support"](https://github.com/openid/dchp/issues/17)
  (parking lot, opened 27 July 2026) — issue text prepared as
  [`DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md`](DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md); alternatively
  ARF Topic G alongside the plan's D4 commitments.
