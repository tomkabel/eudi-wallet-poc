# The de-facto OpenID4VP ZK carrier: `mso_mdoc_zk`

Analysis note, 24 September 2026. Input to plan §8.2 and the new §8.7. Sources verified where a
primary source was reachable; anything short of that is marked **[UNVERIFIED]**.

## 1. Verdict

A de-facto carrier for Longfellow ZK proofs over OpenID4VP exists: a DCQL credential format called
`mso_mdoc_zk`, carrying the proof in a `DeviceResponse.zkDocuments` list. It is shipped today by
Google Wallet (verifier documentation), Multipaz (wallet and verifier side), SUNET/vc (a Go
verifier with cgo into Longfellow, PR #576, merged 20 August 2026) and SIROS (wallet-side work,
SDK PR #99) — **[UNVERIFIED]** beyond the Google docs page and the Multipaz source tree, both of
which were checked on 24 September 2026.

It is **not in any specification**. OpenID4VP 1.0 and the 1.1 editor's draft (commit 23 September
2026) and HAIP contain no ZK text. The EU Age Verification Profile Annex A states ZKP is not
supported over OpenID4VP "since no standardized DCQL query for requesting Zero-Knowledge Proof is
available" and routes ZKP over the Digital Credentials API instead. OpenID DCHP issue #17 is still
deciding the basics: whether ZK is a separate format or a presentation mode, how queries work, how
the transcript binds. The Longfellow scheme itself is an individual IETF draft
(`draft-google-cfrg-libzk`) with no working-group adoption. EUDI TS14 covers BBS/JWP-style ZK and
calls DCQL extensions "necessary" without defining any.

## 2. The wire shape, taken from the Multipaz source (as of 16 September 2026)

Request — a DCQL credential query:

- `format: "mso_mdoc_zk"`
- `meta.doctype_value`
- `meta.zk_system_type`: a list of `{ system: "longfellow-libzk-v1", id, circuit_hash,
  num_attributes, version, block_enc_hash, block_enc_sig }` — exactly the fields
  `verifier/circuits.json` and the fork's `multipaz-circuits.txt` already carry
- `meta.verifier_message` (Google's example; semantics undocumented)
- `claims` as for `mso_mdoc`

Response — `vp_token[<id>]` is a base64url CBOR `DeviceResponse` with a top-level `zkDocuments`
list; each entry is `{ proof: bstr, documentData: #6.24(bstr ZkDocumentData) }`, and
`ZkDocumentData` is `{ zkSystemId, docType, timestamp, issuerSigned, deviceSigned, msoX5chain }`.

Binding: the proof is bound to the normal mdoc `SessionTranscript` with the OpenID4VP handover
(redirect) or the DC API handover; the transcript is a public input to the Longfellow verifier.

This repository's `ParseZkDocument` (step 2a, commit `c38c7b6`) parses precisely this shape —
`zkDocuments` at top level, `documentData` as `#6.24(bstr)`, `zkSystemId` inside, tag-0 timestamp —
because it was aligned against multipaz 0.99.0's real serialization with a generated fixture. The
de-facto carrier and the ISO Annex C carrier share one ZkDocument encoding.

## 3. Constraints to accept

- The device signature must be ECDSA; MAC-based device authentication does not work with Longfellow.
- `msoX5chain` is revealed: the verifier learns who issued the credential (the plan's S6 concern
  about per-batch certificates applies verbatim on this path).
- The proof format depends on an unadopted IETF individual draft.
- The step 0 multipaz proof is 360,564 B (`verifier/go/zk/testdata/step0-multipaz/proof.bin`);
  the `vp_token` envelope around it is unmeasured. Check it against the chosen `response_mode`
  before relying on the redirect flow (`direct_post.jwt` is likely needed). Spec §23 item 24
  is about a ~400 KB payload over proximity device retrieval, a different transport.

## 4. What is already in this repository (plan step 2a and step 0)

- Go verifier linking Longfellow via cgo/FFI (`zkverify-ffi`) — the SUNET PR #576 shape.
- Strict CBOR subset decoder handling the `zkDocuments` shape (`internal/cborsub`, tag 0 and 24).
- `ParseZkDocument`/`ParseZkDocuments` on the real multipaz 0.99.0 serialization, proven by a
  fork-generated fixture (`verifier/go/zk/testdata/step2a-iso-annex-c/`).
- Circuit allowlist keyed on the published tuple, with `block_enc_hash`/`block_enc_sig` and a
  spec-id label byte-identical to multipaz's (`zk.CheckCircuit`, `circuits.Circuit.SpecID()`).
- Transcript-as-public-input wiring on the ISO path (`ISOTranscript`, HPKE info binding).

What is missing for `mso_mdoc_zk` over OpenID4VP: DCQL parsing of the format and its `meta`, the
OpenID4VP handover variant of the transcript (B.2.6.1 exists for OID4VP in `oid4vp/`), response
encoding of `zkDocuments` in `vp_token`, and interop against a real Multipaz verifier/wallet.

## 5. Where something new is warranted

Nobody has written down what the de-facto implementations actually agree on. A short profile —
"`mso_mdoc_zk` for OpenID4VP" — could pin down:

1. registration of the format identifier;
2. a registry for the `zk_system_type` parameters (system, id, circuit_hash, block sizes);
3. the semantics of `meta.verifier_message` (Google's example leaves it undefined);
4. freshness rules for `ZkDocumentData.timestamp` on this transport;
5. how circuits are distributed and pinned (this repository's signed `circuits.json` stand-in is a
   candidate sketch);
6. response-size guidance vs `response_mode`;
7. coexistence with BBS/JWP (EUDI TS14) so one wallet can carry both.

Candidate venues: OpenID DCP working group / DCHP issue #17 (where the format-vs-mode question is
open), or as input to ARF Topic G alongside the plan's existing D4 commitments.

## 6. Relation to the plan's own recommendation

Plan §11 S4 recommends keeping ISO Annex C as the ZK path and OpenID4VP for plain and PID
presentations, because a DCQL extension "interoperates with nothing". This note changes that
premise: a DCQL extension now interoperates with Google Wallet, Multipaz, SUNET and SIROS. §8.7
therefore takes the second way forward the plan named — define the DCQL extension — but as an
alignment with the de-facto carrier rather than an invention, plus the profile write-up.
