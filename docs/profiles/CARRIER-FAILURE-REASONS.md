# Carrier failure reasons

This is the local diagnostic contract for the carrier runner and OpenID4VP
response handler. It describes repository evidence, not certification or an
independent conformance assessment.

| Name | Meaning | HTTP behavior |
| --- | --- | --- |
| transcript-binding | Opaque proof rejection; no individual input can be identified | 200, valid:false |
| predicate-value | Disclosed boolean violates the verifier-owned query, before proof work | 400, valid:false |
| single-use | Session already answered; no second proof verification | 410 |
| no-presentation | Well-formed CBOR contains no provable zkDocument | 200, valid:false for mso_mdoc_zk queries; 400 on the plain sniff and multi-credential paths |
| decryption | JWE authentication/decryption failed, before presentation parsing | 400, session remains unanswered |
| missing-entry | Required vp_token query entry absent | 400, valid:false |
| unknown-carrier | Bytes match neither supported carrier | 400, valid:false |
| malformed-envelope | Recognized carrier bytes fail envelope parsing | 400, valid:false |

The parser and session error identities remain available through errors.Is.
Multi-credential parsing remains all-or-error: an empty credential refuses the
whole response with its credential id and no-presentation name (400), before
any proof work. A completed response's detail is persisted at
/present/result/<id>. Transport failures before Claim do not complete a polling
verdict. Replays do not overwrite the original completed result.

Runtime transcript-binding is neutral: it does not establish that the transcript
caused a rejection or distinguish client_id, nonce, response_uri, jwk_thumbprint,
proof corruption or a trusted issuer pairing from an arbitrary invalid proof.
Proof bytes, keys and runtime internals are never returned for this diagnosis.

Only controlled cases deliberately changing one input name that component. E2E
case 2 names nonce; 3 single-use; 4 predicate-value; appended cases 16–18 name
client_id, response_uri and jwk_thumbprint. Mutually exclusive wallet tamper
options change one transcript input while retaining the real POST destination
and JWE key. Thumbprint tampering requires encrypted transport. Case 19 sends
well-formed empty CBOR and requires no-presentation. The existing fifteen IDs
and both committed vectors remain unchanged.

Case JSONL supplies binding_property; non-binding cases say not-applicable. The
runner validates the case-specific property rather than supplying an expected
label. Reports and summaries retain the supplied property and assertion_result
even on ERROR or FAIL. Omitted records remain NOT_EXECUTED. Missing, generic or
wrong property labels cannot produce a pass. See the
[runner](CARRIER-CONFORMANCE-RUNNER.md) for reproducible commands and the
local/external evidence boundary.
