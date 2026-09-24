# Step 8.7 record — the `mso_mdoc_zk` wallet side

Plan: `ee-eudiw/docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` §8.7. Branch:
`step8-7-wallet-side` (fork `eudi-wallet-poc`), off `step8-review-fixes` at `d7a234f`.
Not pushed.

## What was built

The wallet half of plan §8.7: the de-facto OpenID4VP `mso_mdoc_zk` DCQL carrier parsed into the
same `ZkSystemSpec` model the ISO 18013-5 path uses, so EE-ZKP-051's strict refusal and the
per-doc-request spec scoping (second review, finding 2) apply unchanged, plus ECDSA-only device
auth and the EE-ZKP-042 issuer-disclosure consent line on this path.

### 1. DCQL parsing (`DeviceRequestParser.kt`)

`zkSpecsByDocTypeFromDcql(query: JsonObject)` reads DCQL `credentials[]` entries with
`format: "mso_mdoc_zk"` and maps each `meta.zk_system_type` entry (system, id, circuit_hash,
num_attributes, version, block_enc_hash, block_enc_sig) into a `ZkSystemSpec(id, system)` with
those fields as `addParam` params — the same param names the ISO `zkRequest.params` map carries,
keyed by `meta.doctype_value` exactly as `zkSpecsByDocType` keys the ISO path. An entry missing a
required field is skipped (fails closed); a non-numeric `num_attributes`/`version`/`block_*` is
left unset so `getParam<Long>` returns null and the match fails. First entry per doctype wins,
mirroring the ISO duplicate-docType rule.

Parsing lives at this level, not in a library: eudi-lib-jvm-siop-openid4vp-kt has no
`mso_mdoc_zk` notion, and multipaz's own `DcqlQuery` (checked in the 0.99.0 gradle-cache jars —
it exists, and its `DcqlCredentialQuery` only exposes `getMdocDocType`/`getVctValues`, not the
zk meta) is not on the fork's classpath. The shape is small enough that direct JSON parsing keeps
the dependency question closed.

### 2. View-model wiring (`DigitalCredentialsViewModel.kt`)

`processRequest` reads the optional `dcqlQuery` field of the DC API request's `data` payload and
merges the parsed specs into the ISO map per doctype — first entry wins per docType, in both
sources and across them — so `satisfiableOverRefusable`, `refusePlainWhenSpecsUnsatisfiable` and
the response loop resolve proofs over one uniform spec map. A request that advertises an
unregistered circuit over DCQL is refused by the SAME EE-ZKP-051 code path as over ISO.

### 3. ECDSA-only device auth

The `mso_mdoc_zk` response signs `DeviceAuthentication` with the SecureArea key through
`SecureAreaCOSECryptoProvider` (ES256 COSE_Sign1, raw r||s) — this was already the only device
signing path since step 5 (`deviceCryptoProvider` hard-requires `KeyType.EC`). The new gate makes
the requirement explicit on this path: a non-EC attestation is refused with a logged
`IllegalStateException` before anything is signed, rather than answered with a MAC Longfellow
cannot work with.

### 4. Issuer-disclosure consent (EE-ZKP-042)

`DcUiState.issuerDisclosesToVerifier` is set when the request carried ZK specs from either
source, and `DigitalCredentialsScreen` renders a new line on the consent screen:

> The issuer of this attestation will be visible to the requesting party.

(`presentation_issuer_disclosure_notice`; Estonian:
"Selle tõendi väljaandja on nähtav andmeid küsivale osapoolele.") This is the `msoX5chain`
consequence in plain terms: the carrier puts the issuer's certificate chain into every
`ZkDocument`, so the verifier learns which issuer stands behind the attestation even when the
claims themselves are proved zero-knowledge. `values-ru` only carries language-picker strings,
so there is nothing to translate there.

### 5. Response encoding — verified, unchanged

`zkDocuments` already flows through the `ZkPresenter` seam into the response:
`LongfellowZkPresenter.presentation()` produces the `ZkDocument`, and `zkDeviceResponse` encodes
it with multipaz's `buildDeviceResponse { addZkDocument(...) }` — byte-compatible with the
`step8-7-openid4vp-zk` fixture the `:zk-conformance` `Step87OpenID4VPFixtureTest` wrote and the
sibling repo's Go test verifies. No encoder change was needed; this step only widens when proofs
get produced (DCQL-carried specs now resolve).

## Tests

`MsoMdocZkParsingTest` (8, JVM):

1. `zk_system_type` parses into `ZkSystemSpec` with every param the ISO path names (id verbatim,
   system, circuit_hash, num_attributes, version, block_enc_hash, block_enc_sig).
2. A held circuit resolves — against the REAL bundled Longfellow circuits
   (`LongfellowZkSystem().addDefaultCircuits()`, not a mirror fixture); the HELD spec's own id is
   what resolves, and the verifier allowlists by the query's own circuit_hash.
3. An unregistered `circuit_hash` triggers the full EE-ZKP-051 refusal chain (no scheme
   resolution + `refusePlainFallback(zkCapable, proofRequested, satisfiable=true-to-refuse)`).
4. An `mso_mdoc` query parses to nothing.
5. An `mso_mdoc_zk` query without `zk_system_type` parses to nothing (EE-ZKP-023's mirror: an
   advertisement-less query cannot be allowlisted).
6. An entry missing `circuit_hash`/`num_attributes` fails closed.
7. First entry per doctype wins on a duplicate.
8. Two doctypes key independently.

Gates at this branch's HEAD: `:app:testDebugUnitTest` 94 tests / 0 failures (86 pre-existing +
8 new), `:zk-conformance:test` green (`AgeProofRoundTripTest` proves and verifies against the
bundled circuits), `:app:compileDebugKotlin` clean.

## Deviations

- The DC API `data` payload's DCQL field is read as `dcqlQuery` (the OpenID4VP request-object
  field name); Chrome's `org-iso-mdoc` entries carry no such field and are unaffected. A request
  that carries BOTH an ISO `zkRequest` and a DCQL query for the same doctype is governed by the
  ISO specs (first source wins) — an attacker-shaped duplicate the per-doc-request scoping
  already treats conservatively.
- The ECDSA gate throws rather than degrading: a MAC-based device signature on the ZK path is a
  protocol violation, not a fallback (Longfellow binds the proof to the transcript the EC
  signature covers).
- `verifier_message` is not parsed: as the analysis doc records, its semantics are undocumented
  and a verifier-sent instruction is not a rule the wallet obeys from the wire.

## PENDING-DEVICE

1. End-to-end: a real OpenID4VP verifier session (the sibling repo's `ZkAgeQuery` server) against
   the DC API path — the JVM tests prove parsing, refusal and resolution, not the network flow.
2. `dcqlQuery` field name/presence on actual Chrome/Google Wallet requests — the carrier is
   unregistered (DCHP #17 scoping), so the wire location of the DCQL query on the DC API
   transport is provisional until a device capture exists.
3. Consent-screen rendering of the issuer-disclosure line (Compose screenshot or device).
4. StrongBox/TEE device-key path with the ECDSA gate on real hardware.
