# ISO/IEC 18013-5 2e watch: byte-level diff checklist for §10.2.7 (ZkDocument)

Short checklist note, 25 September 2026 (companion to
[`OPENID4VP-MSO-MDOC-ZK-CARRIER.md`](OPENID4VP-MSO-MDOC-ZK-CARRIER.md), which carries the
analysis; this file only lists what to diff). When the second edition publishes §10.2.7,
run every item below against the multipaz 0.99.0 serialization this repository pinned
(`verifier/go/oid4vp/zkdocument.go` `ParseZkDocument`, fixture
`zk/testdata/step2a-iso-annex-c/`). Each diff is one line in the carrier interface
(`oid4vp/carrier.go`): a new `ISOZkDocument` Carrier is ~100 lines + a fixture if — and
only if — the diffs say the encoding stayed compatible or cleanly versionable.

## The checklist

1. **zkDocument container.** Is the 2e zkDocument still exactly
   `{ proof: bstr, documentData: #6.24(bstr) }`? Any new mandatory member (version?
   algorithm suite?) breaks the two-key assumption `ParseZkDocument` enforces.
2. **Tag 24 wrapper.** Does `documentData` remain tag 24 over a bstr containing the
   ZkDocumentData map (not an inline map, not tag 24 over the map directly)?
3. **ZkDocumentData field set.** Is it still the six keys
   `zkSystemId, docType, timestamp, issuerSigned, deviceSigned, msoX5chain` — same names,
   same order-insensitivity? Watch for: `zkSystemId` renamed/moved to the zkDocument
   level, extra fields (circuit id split from system id?), `params` appearing.
4. **`zkSystemId` vs `system` label.** Does 2e name the proving system
   `org.iso.mdoc.zk` (the draft label this repo's `SystemName` holds) or something new,
   and does the id label format stay
   `<system>_<version>_<num_attributes>_<block_enc_hash>_<block_enc_sig>_<circuit_hash>`?
   The allowlist keys on id+circuit_hash+params, so a label change is an audit-line change,
   not a trust change — but the sniff order in `zkSystemForSpecID` must learn the new
   prefix.
5. **Timestamp encoding.** Still tag 0 over RFC 3339 UTC, seconds precision, exactly 20
   bytes (`CheckTimestampWindow` refuses anything else)? A numeric-offset or
   fractional-seconds allowance in 2e must NOT be copied into the window check without a
   circuit-slot re-check (the Longfellow timestamp input is a fixed 20-byte slot).
6. **issuerSigned / deviceSigned shape.** Still maps of namespace → array of
   `{elementIdentifier, elementValue}`? `elementValue` still raw CBOR booleans for
   predicates (EE-ZKP-021(b) reads them on the OpenID4VP carrier via
   `checkDisclosedValue`)?
7. **msoX5chain.** Still bare bstr for one certificate / array of bstrs for a chain, leaf
   first? `LeafCertificate` unwraps exactly the one-element form.
8. **DeviceResponse placement.** Where do zkDocuments live in a 2e DeviceResponse: still
   the top-level `zkDocuments` key multipaz uses (plus the older
   `documents[].zkDocument` form both are accepted in)? Does `status: 0` remain the
   success value?
9. **Transcript binding.** Does 2e §10.2.7 say (or imply) which SessionTranscript the
   proof binds — and does it stay the dcapi handover for 18013-7 Annex C, with the
   OpenID4VP handover (B.2.6.1) for the redirect carrier? The two flows are never
   interchangeable (`oid4vp.TranscriptForFlow`); a 2e statement either way is a fixture
   to generate, not an assumption to make.
10. **Proof format.** Any change to the Longfellow proof serialization itself (version
    byte, block encoding) belongs to `draft-google-cfrg-libzk`, not 2e — but diff the
    proof bstr of a 2e-generated vector against `step0-multipaz/proof.bin` anyway and
    record which revision produced it.

## On arrival

- Generate a 2e vector (wallet side), commit it next to the step 2a fixture with a
  `GENERATED-BY` naming the edition/revision.
- Implement `ISOZkDocument` as a third `Carrier` (name it, e.g. `iso-18013-5-2e`), reuse
  `CheckedPresentation`, and add it to the carrier table test (both carriers × valid /
  tampered / wrong-query / wrong zk_system becomes three).
- If the diffs show 2e is byte-compatible with the multipaz shape: the existing
  `MsoMdocZkCBOR` carrier may accept it under an alias — record that as a profile v1.x
  note, not a silent fallthrough.
