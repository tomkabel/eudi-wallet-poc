# Step 2a record — ISO 18013-7 Annex C, ZK-only

**Status:** complete on this host, 24 September 2026. Branch
`step2a-iso-annex-c` off `step0-cross-verify` (PR #22). Opened as PR #23.

This is plan §5 design '2a: ZK only' — the verifier requests the age predicate
through a `DeviceRequest` carrying `zkRequest.systemSpecs`, delivers it through
the W3C Digital Credentials API, and accepts back an HPKE-encrypted
`DeviceResponse` whose `zkDocuments` carry the Longfellow proofs. No plain
documents are requested; a response that answers without a ZK document is a
`valid: false` answer, not an error.

## Implemented

### 1. Session extension (plan §5.1)

- `oid4vp/session_iso.go`: `ISOExtension` — the session's HPKE P-256 private
  key, the EncryptionInfo string, the offered spec ids and their registry
  entries. `Store.NewISO` mints all of it at session creation; `Store.AttachISO`
  installs it on the *stored* session (an earlier draft mutated the snapshot
  `Store.New` returns — the store copy kept `iso` nil, which the full-flow test
  caught).
- Ciphersuite fixed to DHKEM(P-256, HKDF-SHA256)/HKDF-SHA256/AES-128-GCM via
  the Go 1.26 stdlib package `crypto/hpke` (`oid4vp/hpke.go`). The plan
  anticipated an API mismatch on Go 1.27; there was none beyond the shape
  itself: `hpke.Seal`/`hpke.Open` are the single-shot forms (the first `Seal`
  returns `enc||ct` directly), and `NewDHKEMPrivateKey` wraps the `ecdh`
  key. go.mod bumped 1.24 → 1.26 (the vet gate names `go1.26` for these
  symbols); host toolchain is Go 1.27.1.
- `EncryptionInfo = ["dcapi", {nonce: bstr(32), recipientPublicKey: COSE_Key}]`
  (`oid4vp/isodcapi.go`, `oid4vp/hpke.go`). The COSE_Key is a bare map — kty
  EC2 / alg ES256 / crv P-256 with x/y as bstrs — not tag 24 over a bstr (an
  earlier draft wrapped it, which every known reader refuses). Evidence, from
  multipaz 0.99.0 bytecode: the writer (`VerificationUtil`) puts
  `EcPublicKey.toCoseKey().toDataItem()`, a bare map; the reader
  (`digitalCredentialsMdocApiProtocol`) calls `DataItem.getAsCoseKey()`, whose
  `CoseKey.fromDataItem` requires a `CborMap`; the fork reads it the same way
  (`DigitalCredentialsViewModel.kt`:242, `.asCoseKey`). [UNVERIFIED against
  the ISO/IEC 18013-7 Annex C text, which is paywalled.]
- `-dcapi-origin` flag (`main.go`): the origin is configuration, never a
  request header — the browser would otherwise choose the bytes the handover
  hashes. Without the flag the dcapi routes are not registered and the path is
  off.

### 2. Static presentation page

- `iso.html` embedded in `iso.go` (`go:embed`), same styling family as the
  redirect path's pages. `navigator.credentials.get({digital: {requests:
  [{protocol: 'org-iso-mdoc', data: {deviceRequest, encryptionInfo}}]}})`
  (W3C Digital Credentials Editor's Draft, 4 September 2026:
  `DigitalCredentialRequestOptions.requests`) with both values base64url from
  `GET /present/dcapi/request/<id>`; posts `{response: credential.data.response}`
  (`DigitalCredential.data` is an object) to `POST /present/dcapi/response/<id>`;
  polls `GET /present/dcapi/result/<id>`. The page names the element the
  session asks for.
- Routes: `/present/dcapi/new`, `/request/`, `/response/`, `/result/`, page at
  `/present/dcapi/<id>`.

### 3. DeviceRequest build

- `oid4vp/isodcapi.go` `DeviceRequest(docType, namespace, element, specs)`:
  `{version: "1.0", docRequests: [{itemsRequest: tag24({docType, nameSpaces: {ns:
  {element: false}}, requestInfo: {zkRequest: {version: 1, systemSpecs:
  [...], zkRequired: true}}})}]}`. Each spec carries the multipaz label
  (`circuits.Circuit.SpecID`) under both `zkSystemId` and `id`,
  `system: longfellow-libzk-v1`, and `params` {circuit_hash, version,
  num_attributes, block_enc_hash, block_enc_sig} — the labels the PoC's
  `matchZkSystemSpec` reads (`DigitalCredentialsViewModel.kt`:384-392).
- **Corrected after review.** The first version of this request could not be
  parsed by either wallet. `version` was the uint `1`; multipaz 0.99.0
  `DeviceRequest.fromDataItem` and the PoC's `DeviceRequestParser.kt`:109 both
  read it with `asTstr`, which throws. multipaz `ZkRequest.fromDataItem` reads
  `zkSystemId`, `system`, `params` and `zkRequired` with a throwing `get`; the
  request had only `id` (the key the PoC reads, kept). And the `system` label
  was `org.iso.mdoc.zk`: multipaz's `MdocPresentment` resolves it with
  `ZkSystemRepository.lookup(spec.system)` against `ZkSystem.getName()`, and
  `LongfellowZkSystem.getName()` is `longfellow-libzk-v1`, so a stock multipaz
  wallet would have skipped every spec ("no compatible ZkSpec"). The PoC never
  hit that lookup, because it calls `LongfellowZkSystem` directly. Checked
  against multipaz 0.99.0 bytecode, and by parsing both vectors with
  `DeviceRequest.fromDataItem` from the 0.99.0 jar in a plain JVM: the old
  vector throws "Failed requirement.", the new one parses. [UNVERIFIED against
  the ISO/IEC 18013-5 second-edition text for the key names and the system
  identifier, which is not published.]
- `circuits.json` gained `block_enc_hash: 4151, block_enc_sig: 4096` (the
  values the committed `multipaz-circuits.txt` lists for 7/1) — without them
  the label is `..._0_0_...` and no wallet matches it.
- Vector: `verifier/go/zk/testdata/step2a-iso-annex-c/device_request.cbor`,
  compared byte for byte by the build test (rewritten only under `-update`),
  re-parsed with `cborsub`, provenance in `GENERATED-BY`.

### 4. Response endpoint

`iso.go` `isoCheck`, same guard order as the redirect path:
slot (`sem.tryAcquire` before `Claim`, so a 503 never burns the session) →
`Claim` (replay = 410) → transcript recomputed from the verifier's own stored
EncryptionInfo + configured origin (`Session.ISOTranscript`, never wire data) →
HPKE open (single-shot `enc||ct`, info = transcript CBOR, aad empty) →
`cborsub` strict decode → per-zkDocument rules → `zk.Verify` (ErrInvalid → 200
`valid:false`; other errors → 400 category).

### 5. Strict CBOR subset decoder

- `internal/cborsub` (+ ADR-002 in `docs/decisions/`): definite lengths,
  shortest-form heads, maps/arrays/bstr/tstr/uint/tag24/bools/null only,
  duplicate keys rejected, hard limits (depth 32, 1024 items, 1 MiB strings),
  fail closed. Fuzz target `FuzzDecode` seeded from the committed vectors;
  5-second fuzz run clean (176k execs).
- Tests cover accept/reject tables, depth/items/length bombs, duplicate keys,
  non-minimal heads, truncated inputs.

### 6. §5.3 rules

- **(a) timestamp window**: `CheckTimestampWindow` — the value comes from
  `ZkDocumentData.timestamp`, RFC 3339 without fractional seconds (ISO
  18013-5 clauses 7.1/9.1.2.4), rejected outside `[session Created − 60 s,
  now + 60 s]`.
- **(b) trust store only**: `TrustStore.Select` — the leaf key of the
  presentation's `msoX5chain` may only pick among the trusted issuers for the
  doctype; a key outside the store is `403`.
- **(c) offered specs only**: the response's `zkSystemSpecId` must be in the
  session's `BySpecID` (403 otherwise, before the registry and before any FFI
  call); `version`/`num_attributes` for the FFI come from the registry entry
  behind the id.
- **No holder-supplied circuit parameter through the FFI**: `version` and
  `num_attributes` come from the registry entry behind the offered spec id,
  never from the response. The holder-derived inputs that do reach
  `zk.Verify` are each constrained: the proof bytes; the timestamp (rule (a),
  window-checked); the docType (equality-checked against the session's); and
  the `msoX5chain` leaf key, which only selects among trusted issuer keys
  (rule (b)).
- **Startup self-check**: every registry entry is hashed via `zk.CircuitHash`
  at startup; a mismatch is `log.Fatalf`.
- **S11 fixed**: `zk.CheckCircuit` matches `(version, num_attributes)` against
  the registry's published entries *before* computing any hash (FFI only for
  an accepted entry, as the confirmation). ARCHITECTURE.md §4.5 rewritten (the
  "one known inversion" section is now the fix), verifier README and root
  README updated with the historical measurement marked as fixed.
- **Registry out of the cgo package**: `oid4vp` needs the circuit types, and
  importing them from `zk` linked the Rust staticlib into its tests, which the
  CI `fast` job builds without. The registry now lives in the plain-Go package
  `verifier/go/circuits`; only the FFI hash confirmation stays in `zk`.

### 7. Vectors and tests

- `verifier/go/zk/testdata/step2a-iso-annex-c/`: `device_request.cbor` +
  `GENERATED-BY`; `device_response.cbor` (a complete multipaz 0.99.0
  `DeviceResponse` carrying a self-verified `zkDocument`, written by the fork's
  `:zk-conformance` `Step2aDeviceResponseFixtureTest`), with
  `device_response_transcript.bin` (the transcript the proof binds),
  `device_response_issuer.json` (the minted issuer's public key for the trust
  store) and `device_response_GENERATED-BY` provenance.
- `oid4vp/isodcapi_test.go`: handover bytes, ISO transcript shape, HPKE
  round-trip (fixed keys, flipped-bit negative), EncryptionInfo shape,
  DeviceRequest build + cborsub round-trip + spec id presence.
- `iso_test.go` (package main, httptest): unsolicited spec id → 403 fast
  (asserted < 2 s, vs the ~2.5 s an FFI call would take) with no FFI call,
  replay → 410, flipped ciphertext bit → 400, flipped proof bit → 200
  `valid:false` (exercises the real FFI with the committed multipaz proof),
  timestamp window negatives.
- `internal/cborsub/cbor_test.go` + fuzz corpus.

## Acceptance on this host

```
$ cd verifier/go
$ go build ./...          # ok
$ go vet ./...            # ok
$ gofmt -l .              # empty
$ go test ./... -count=1
ok   github.com/tomkabel/ee-eudiw/verifier/go              6.5s
ok   github.com/tomkabel/ee-eudiw/verifier/go/internal/cborsub  0.0s
ok   github.com/tomkabel/ee-eudiw/verifier/go/oid4vp       0.0s
ok   github.com/tomkabel/ee-eudiw/verifier/go/zk          22.4s   # includes TestStep0*
```

`TestStep0RustProverFixture` and `TestStep0MultipazFixture` stayed green
throughout (run explicitly after the registry change: `ok .../zk 34.0s`).

## PENDING-DEVICE

Everything that needs a real phone — the plan's device-side acceptance items,
unverifiable from this host:

1. `navigator.credentials.get` with protocol `org-iso-mdoc` on a Chrome/Android
   with GMS core credentials: does the platform accept the `zkRequest` bytes
   and the `block_enc_*` params in `params` (multipaz's `getMatchingSystemSpec`
   compares `num_attributes` and `circuit_hash` — verify the platform passes
   `params` through untouched).
2. The fork wallet's `matchZkSystemSpec` path against the served request: the
   spec id label must match the wallet's own bundled label byte-for-byte
   (`longfellow-libzk-v1_7_1_4151_4096_8d07…`).
3. HPKE interop end to end: the wallet's Tink-backed `Hpke.getEncrypter`
   (DHKEM_P256_HKDF_SHA256_HKDF_SHA256_AES_128_GCM) sealing to this verifier's
   COSE_Key, and `crypto/hpke` opening it — the transcript-in-info/empty-aad
   convention comes from the fork's own comment, but the byte equality of
   `info` has not been exercised against Tink.
4. ~~`zkDocument` field casing on the wire~~ **RESOLVED 2026-09-24 against
   multipaz 0.99.0 bytecode + a real fixture, no device needed.** The wire
   shape (verified against `ZkDocument.toDataItem`/`ZkDocumentData.toDataItem`/
   `DeviceResponse.toDataItem` decompilations and confirmed by decoding
   `zk/testdata/step2a-iso-annex-c/device_response.cbor`, written by the fork's
   `:zk-conformance` through the wallet's own
   `buildDeviceResponse{addZkDocument}.toDataItem()` path):
   `zkDocument = {proof: bstr, documentData: #24(map)}` (the wire key is
   `documentData`, carrying a `ZkDocumentData`); the spec id is *inside* that
   map under **`zkSystemId`**; `timestamp` is **tag 0 over a tstr** (UTC, whole
   seconds, `Z`); `issuerSigned`/`deviceSigned` are maps of namespace → array
   of `{elementIdentifier, elementValue}` maps; `msoX5chain` serializes as a
   **bare bstr for a single certificate** (X509CertChain's one-cert shortcut)
   and as an array of bstrs only for longer chains; `zkDocuments` is a
   **top-level DeviceResponse key** and an empty `documents` array is omitted;
   version is `1.0`, or `1.1` when `encryptedDocuments` is present.
   `ParseZkDocument`/`ParseZkDocuments` implement exactly this shape;
   `TestStep2aMultipazDeviceResponse` proves the committed fixture end to end
   (parse → spec allowlist → issuer selection → `zk.Verify` passes; one flipped
   proof bit fails). Remaining on-device risk is only the transport around the
   response (items 1-3, 5-6), not the zkDocument casing.
5. The `response` value posted back by the real DCAPI page: base64url
   (`Base64.UrlSafe.withPadding(ABSENT)`) — the decoder also tolerates padded
   standard base64, but the real thing must be confirmed once on device.
6. Origin binding: confirm the browser reports exactly the configured
   `-dcapi-origin` string to the wallet (the handover hashes it; a mismatch
   shows up as a proof that fails to verify, which is the correct failure but
   needs on-device evidence).

## Files

| What | Where |
|---|---|
| Session extension | `verifier/go/oid4vp/session_iso.go`, `session.go` |
| HPKE + EncryptionInfo + envelope | `verifier/go/oid4vp/hpke.go`, `envelope.go` |
| ISO primitives (transcript, request, ZkDocument, timestamp) | `verifier/go/oid4vp/isodcapi.go` |
| Envelope + leaf-cert parse | `verifier/go/oid4vp/zkdocument.go` |
| Endpoints + page | `verifier/go/iso.go`, `iso.html` |
| Strict decoder | `verifier/go/internal/cborsub/cbor.go`, `cbor_test.go` |
| Registry fix | `verifier/go/circuits/registry.go` (tuple lookup), `verifier/go/zk/zk.go` (`CheckCircuit`) |
| Server wiring | `verifier/go/main.go`, `present.go` |
| Trust store selection | `verifier/go/oid4vp/trust.go` |
| Tests | `verifier/go/iso_test.go`, `oid4vp/isodcapi_test.go` |
| Vector | `verifier/go/zk/testdata/step2a-iso-annex-c/` |
| ADR | `docs/decisions/ADR-002-STRICT-CBOR-SUBSET-DECODER.md` |
