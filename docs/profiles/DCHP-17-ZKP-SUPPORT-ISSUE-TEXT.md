# DCHP #17 issue text — prepared 24 September 2026

Target: [openid/dchp#17 "ZKP Support"](https://github.com/openid/dchp/issues/17)
(opened 27 July 2026, `parking-lot` label, body: "ZKP support was removed from the initial
proposal and we need to discuss further and create a section for it."). The text below is
prepared as a comment on that issue. It reports deployed behaviour rather than proposing a
specification, because the implementations exist and the question the working group faces
is whether to adopt what ships or replace it. Companion document: the
`mso_mdoc_zk` for OpenID4VP profile (`docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`),
which pins the same facts with fixture references. Every claim below is either
fixture-verified against multipaz 0.99.0 or marked as not independently checked.

---

**Comment for #17 — deployed ZK carrier exists, four questions for this issue**

Following up on the parking-lot state of this issue: a ZK carrier over OpenID4VP is
already shipping, so the format-vs-presentation-mode question this issue needs to decide
has a de-facto answer on the wire today. Reporting what we measured and verified, and the
four decisions the issue would need to take.

**What ships today.** A DCQL credential format `mso_mdoc_zk`:

- Request: `credentials[]` entry with `format: "mso_mdoc_zk"`, `meta.doctype_value` (as for
  `mso_mdoc`), `meta.zk_system_type` — a list of `{ system, id, circuit_hash,
  num_attributes, version, block_enc_hash, block_enc_sig }` — and, in at least one shipped
  verifier's examples, `meta.verifier_message` (semantics undefined anywhere we could find).
- Response: `vp_token[<credential id>]` is a base64url CBOR `DeviceResponse` with a
  top-level `zkDocuments` array; each entry is `{ proof: bstr, documentData:
  #6.24(bstr ZkDocumentData) }`, `ZkDocumentData = { zkSystemId, docType, timestamp,
  issuerSigned, deviceSigned, msoX5chain }`.
- Binding: the proof binds the normal ISO/IEC 18013-5 `SessionTranscript` with the
  OpenID4VP handover (Appendix B.2.6.1 `[null, null, ["OpenID4VPHandover",
  sha256(cbor([client_id, nonce, jwk_thumbprint, response_uri]))]]`); the transcript is a
  public input to the proof.
- The `system` string in live use is `longfellow-libzk-v1` (the multipaz/Google Longfellow
  system), while the ISO/IEC 18013-5 second-edition dcapi path names the same system
  `org.iso.mdoc.zk`. The proof itself is defined in `draft-google-cfrg-libzk`, still an
  individual IETF draft.

We verified the byte-level shape end to end against a multipaz 0.99.0 wallet-generated
response and an independent verifier implementation (proof ~360 KB per 1-attribute
presentation, committed fixture, transcript verified in both directions — the proof
verifies over the B.2.6.1 transcript and does NOT verify over the ISO dcapi transcript).
Not independently checked by us: Google Wallet's exact current request examples beyond
their published verifier documentation, and SUNET's and SIROS's implementations beyond
their public PRs.

**Why this matters for #17.** The EU Age Verification Profile (Annex A) routes ZKP to the
Digital Credentials API "since no standardized DCQL query for requesting Zero-Knowledge
Proof is available" — but a de-facto DCQL query exists and interoperates across at least
four implementations. Adopting it (or replacing it deliberately) is strictly better than
leaving it unregistered, because unregistered wire behaviour drifts.

**Four questions this issue would need to take, with our measurement-based input:**

1. **Format or presentation mode?** The de-facto answer is a *format* (`mso_mdoc_zk`),
   with the proving system named inside `meta.zk_system_type` per credential query, not a
   presentation-level `presentation_definition`-style mode. Working with a format keeps
   per-credential circuit negotiation (each query names exactly which circuits can answer
   it) and reuses the existing `mso_mdoc` meta machinery.
2. **Registration home?** OpenID4VP 1.0 created no credential-format registry (its IANA
   section registers only response types), so `mso_mdoc_zk` today has nowhere to be
   registered. If this WG adopts the format, a registry (with a `zk_system_type` sub-registry
   or Designated-Experts review of its fields) is a prerequisite; the `system` identifier
   arguably belongs with the proving-system draft (`draft-google-cfrg-libzk` → CFRG/IETF),
   with OpenID registering only the format identifier and the meta parameter.
3. **`verifier_message` semantics?** Shipped but undefined everywhere. We treat it as
   reserved-to-be-defined and ignore it on both wallet and verifier sides — an
   unauthenticated verifier instruction that changes wallet behaviour would be a downgrade
   surface. If the WG defines it, it should be display-only unless it is authenticated.
4. **Transcript binding confirmation?** The de-facto carrier binds the proof to the
   standard B.2.6.1 `OpenID4VPHandover` transcript, same as a plain mdoc response — no ZK
   -specific transcript is needed. We confirmed this byte-for-byte against a real
   Longfellow prover (host side; the real-device diff is still pending). Writing this down
   in the (future) ZK section would close the question implementers currently have to
   reverse-engineer.

One consumer-side note from a deployment that must window-check responses: the carrier's
`ZkDocumentData.timestamp` is the wallet's own clock claim, and the proof takes it as its
validity input — so the section (whenever written) needs a freshness rule, not just a
format definition. We use the ISO dcapi path's ±60 s session window as the starting point.

We are not asking the WG to bless Longfellow specifically — the same section could carry
any `system` — only to decide the four structural questions above, where deployed behaviour
already exists to point at.
