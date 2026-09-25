# zkverify — a relying-party verifier, in Go

Verifies Longfellow zero-knowledge proofs over ISO/IEC 18013-5 mdocs. The service
learns whether the attribute was present, and nothing else — it never sees the
credential, the issuer's signature, the salts, or any other attribute.

## Why this exists

`google/longfellow-zk` ships a Go verifier at `reference/verifier-service/`, but it
links the **C++** `libmdoc_static`, so you need a C++ toolchain and the full CMake
build. This links the **Rust** implementation instead, through a small C ABI
(`zkverify-ffi/`), which means:

- `cargo build` and cgo, no C++ toolchain;
- a second, independently written Go binding against the implementation Google says
  it intends to ship in production;
- a natural place to put the checks a relying party actually owes the user —
  starting with the circuit allowlist.

## Build and run

```bash
# 1. longfellow-zk must sit next to this repository
git clone https://github.com/google/longfellow-zk.git ../longfellow-zk
git -C ../longfellow-zk checkout --detach 61a8a735964d1b22bccf79bf14ef6767249cdf92

# 2. static library with the C ABI  (~1 min)
cd verifier/zkverify-ffi && cargo build --release && cd ..

# 3. the service
cd go && go build -o ../zkverify . && cd ..
./zkverify -registry circuits.json -issuers <issued>/issuers.json -addr 127.0.0.1:8080
```

`issuers.json` is written by [`../issuer`](../issuer) on every run — it is the trust-store
entry for the key that signed those attestations. Nothing ships in the repo, because a
placeholder issuer key in version control is a trap.

`zkverify-ffi/.cargo/config.toml` pins `-C target-cpu=x86-64-v3`. Longfellow's own
`rust/.cargo/config.toml` sets `-C target-cpu=native`, which produced a **SIGILL**
binary on the virtualised host used here.

Expect one linker warning — `missing .note.GNU-stack section implies executable
stack`, from a hand-written assembly object in the Rust `sha2` crate. It is
upstream's, not ours.

## Two ways in

`/present/*` runs a full **OpenID4VP 1.0** exchange: the verifier issues a DCQL query,
the holder proves against the session transcript, and the issuer key comes from the
verifier's own trust store. That is the flow a wallet would use.

`POST /zkverify` checks one proof directly, with the caller supplying the issuer key
and `now`. It is the low-level API — useful for testing, wrong as a presentation
endpoint, because a caller who chooses the issuer key can mint their own attestation
and prove anything. It is therefore **only registered under `-unsafe-dev-api`**, and
404s otherwise; the flag logs a warning at startup.

## OpenID4VP

```text
POST /present/new              -> {id, request_uri, wallet_uri, result_uri}
GET  /present/request/{id}     -> the authorization request, carrying dcql_query
POST /present/response/{id}    -> vp_token   (direct_post)
GET  /present/result/{id}      -> the outcome, for the relying party's own page
GET  /issuers                  -> the trusted attestation providers
```

The DCQL query is one credential and one claim — Presentation Exchange is not used, having
been removed from OpenID4VP before Final (`EE-PRO-002`):

```json
{"credentials": [{
  "id": "proof_of_age",
  "format": "mso_mdoc",
  "meta": {"doctype_value": "ee.riik.poa.1"},
  "claims": [{"path": ["ee.riik.poa.1", "age_over_18"], "values": [true]}]
}]}
```

`values` is required, not decorative. A claim that names no value is answered just as
well by `age_over_18 = false`, so the verifier rejects an unconstrained age query and
refuses any presentation whose CBOR value is not one the query asked for
(`EE-ZKP-021(b)`).

### The session transcript is the whole point

`oid4vp/transcript.go` implements OpenID4VP 1.0 Appendix B.2.6.1:

```text
SessionTranscript = [null, null, OpenID4VPHandover]
OpenID4VPHandover = ["OpenID4VPHandover", sha256(OpenID4VPHandoverInfoBytes)]
OpenID4VPHandoverInfo = [clientId, nonce, jwkThumbprint, responseUri]
```

`jwkThumbprint` is the RFC 7638 SHA-256 thumbprint of the verifier's response-encryption
key when the response is encrypted, and **null** otherwise. This PoC uses `direct_post`,
so it is null.

The holder signs `deviceAuth` over this at presentation time — not at issuance. A proof
made for a different `client_id`, `response_uri` or `nonce` hashes to a different
transcript and does not verify. `wallet/present.py` derives the same bytes independently
in Python, and the two implementations are checked against each other.

### Measured, full flow

| case | result |
|---|---|
| valid presentation | `200` `valid:true` — `age_over_18 = 0xf5, issued by EE-EUDIW demo issuer` |
| proof bound to a different nonce | `200` `valid:false` — *no trusted issuer's key verifies this proof* |
| replay of a valid vp_token | `410` — *session already answered* |
| issuer not in the trust store | `200` `valid:false` |
| presentation answering a different claim than the query asked | `400` — *presentation discloses "age_over_21" but the query asked for "age_over_18"* |

Proving takes ~6.1 s and verification ~2.5 s on 2 vCPU; the rest is negligible.

### Who fixes `now`

The circuit checks the attestation's validity window against a timestamp that prover and
verifier must agree on. The verifier puts `expected_now` in the request object and verifies
against the value it issued, so the holder cannot pick an instant at which an expired
attestation would still verify. That parameter is an extension, not part of OpenID4VP.

### The vp_token encoding is interim

ISO/IEC 18013-5 second edition defines a `ZkDocument` inside the `DeviceResponse` (§10.2.7
of the DIS) for exactly this, and that edition is unpublished. Until it lands, the
presentation is a small versioned JSON envelope, base64url-encoded, defined in
`oid4vp/vptoken.go`. It is a stand-in so the flow can be built and tested, **not** an
interoperability claim. It deliberately does not carry the issuer public key.

## API

`POST /zkverify` — requires `-unsafe-dev-api`; 404 without it.

```json
{
  "version": 7, "num_attributes": 1,
  "pkx": "0x…", "pky": "0x…",
  "doc_type": "ee.riik.poa.1", "namespace": "ee.riik.poa.1",
  "attr_id": "age_over_18", "attr_cbor_hex": "f5",
  "now": "2026-09-05T00:00:00Z",
  "transcript_b64": "…", "proof_b64": "…"
}
```

```json
{ "valid": true,
  "circuit_hash": "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
  "attr_id": "age_over_18", "attr_cbor_hex": "f5", "took_ms": 2414 }
```

`GET /circuits` lists the accepted circuits. `GET /healthz` is a liveness probe.

## Measured

2 vCPU, against attestations minted by [`../issuer`](../issuer):

| case | result | |
|---|---|---|
| valid proof | `200 valid:true` | 2414 ms |
| valid proof, different batch member | `200 valid:true` | 2427 ms |
| one bit flipped in the proof | `200 valid:false` — `proof did not verify` | 2492 ms |
| circuit not in the registry | `403` — rejected before verification | **11 ms** |

That last row is the point of doing the allowlist check first: an unrecognised
circuit costs milliseconds to refuse, not 2.4 s to verify.

That 11 ms is from the 2-vCPU set and has not been reproduced here, on different
hardware. What the re-measurement did establish is that **there is no single refusal
cost**, because `Registry.Check` computes the circuit hash through the FFI *before* it
looks anything up, and that call's cost depends entirely on which circuit the caller
named. On 8 vCPU (`-C target-cpu=x86-64-v3`), timing `CircuitHash` in isolation:

| `version` requested | cost of refusing it |
|---|---|
| 1–4 — unknown to the runtime | ~3 µs; it fails before doing any work |
| 5–7 — known, 1 to 4 attributes | 3.4–9.1 ms |
| 8 — known, 1 to 4 attributes | **54–58 s** |

HISTORICAL MEASUREMENT, DEFECT FIXED 24 Sep 2026: the table above described
`Registry.Check` as it *was* — hashing through the FFI before looking anything
up. `zk.CheckCircuit` now matches `(version, num_attributes)` against the registry's
published entries first and hashes only what survives, so refusing an
unaccepted circuit costs a map scan (~µs) at every version. The measurement
stays because it is what motivated the fix and because `CircuitHash`'s
per-version cost is still real for the paths that legitimately call it (the
startup self-check, the verification itself).

So a refusal over HTTP was milliseconds only for the first two rows: 20 sequential POSTs
of a real 353 KB proof naming an unknown `version` came back in a median of 5.7 ms, and
that time was request decoding, not hashing. The claim the row exists to support — that
the allowlist is far cheaper than verifying — held for those. It inverted for the last
row, where refusing cost twenty times more than the 2.4 s verification the check was
meant to avoid. That was the defect fixed above; the last bullet below records it.

### Verifying a multipaz proof (step 8)

Plan §6's third measurement row — verification time for multipaz proofs at the relying
party, `EE-ZKP-040` (≤ 1.0 s p95) — measured on this host (Intel i5-8365U @ 1.60 GHz,
8 threads, Rust built with `-C target-cpu=x86-64-v3`), 24 September 2026, by timing
`zk.Verify` over the step 0 multipaz fixture:

| statistic | value |
|---|---|
| benchmark mean (`BenchmarkMultipazVerify`, 10 iterations) | 4.02 s/op — 512 B/op, 1 alloc/op |
| p50 over 20 individually timed runs | 3.83 s |
| p95 over the same runs | 4.91 s |
| max | 5.15 s |

Reproduce with:

```bash
cd verifier/go
go test ./zk/ -bench BenchmarkMultipazVerify -benchtime 10x -run '^$'
go test ./zk/ -run TestMultipazVerifyLatencyPercentiles -v
```

The 1.0 s p95 budget is **not met on this host, and these are host figures, not device
figures**. The proof is a phone prover's output (multipaz 0.99.0, the step 0 fixture) and
the verifying runtime is the Rust Longfellow this verifier links in production, but a phone
verifying through multipaz's `libzkp.so` on phone silicon is a different machine, and plan
§6 routes the budget check through the device set. What this run establishes is the
host-side baseline the device figure will be read against, and that a multipaz proof costs
the relying party the same order of time to verify as the Rust-prover proofs in the 2.4 s
table above — verification dominates the per-presentation cost either way. The percentile
test records rather than fails on budget overrun, because a developer laptop or a shared CI
runner is not provisioned for the budget; `EE_BENCH_FAIL=1` re-arms the check where the
host is known to be quiet.

## The Android wallet that answers this verifier

The holder side is not in this repository. It is the independent fork
[`tomkabel/eudi-wallet-poc`](https://github.com/tomkabel/eudi-wallet-poc) (of
[`open-eid/eudi-wallet-poc`](https://github.com/open-eid/eudi-wallet-poc)) at
`step8-measurement-harness @ 2396245` (branch chain `step1-fork-identity` →
`step3-holder-obligations` → `step5-hardware-keys` → `step4-protocol-hygiene` →
`step6-issuance-consumption` → `step7-conformity-note` → `step8-measurement-harness`, by commit as
recorded in `../docs/planning/STEP7-RECORD.md` and `../docs/planning/STEP8-RECORD.md`), which proves against this registry's `ZkSystemSpec` over the
ISO 18013-7 Annex C DC API path (step 0 cross-verified the two Longfellow halves; see
[`docs/planning/STEP0-CROSS-VERIFY-RECORD.md`](../docs/planning/STEP0-CROSS-VERIFY-RECORD.md)).
The on-device presentation against this verifier's Annex C endpoint is still pending —
see [`docs/ARCHITECTURE.md` §6](../docs/ARCHITECTURE.md) and [`docs/CONFORMITY.md`](../docs/CONFORMITY.md).

## The circuit registry

`circuits.json` is this relying party's accepted-circuit set. EE-EUDIW-TS-1.0
requires a relying party to check a proof's `circuit_hash` against a published set
**before** verifying it (`EE-ZKP-023`) and to cache that set rather than fetch it per
presentation (`EE-ZKP-032`) — a per-presentation fetch is a phone-home that would
defeat `ZKP_07`.

Each entry carries what `EE-ZKP-030` requires a national registry to publish: the
hash, the circuit version and attribute count, the upstream release it came from, the
audits covering it, and acceptance and deprecation dates. Deprecated entries are
loaded and ignored, so removing a circuit is an append, not a delete.

The file here is a local stand-in. A real one is signed, append-only and
CT-logged (`EE-GOV-012`).

## What this does not do yet

- **The request object is unsigned.** A production verifier signs it as a JAR and
  authenticates with an `x509_san_dns` client identifier backed by a relying-party access
  certificate (`EE-RP-002`/`EE-RP-003`). A wallet has nothing here to check, so the
  `client_id` is a claim, not a credential — which is why the trust store binds issuers
  rather than verifiers.
- **`direct_post`, not `direct_post.jwt`.** HAIP 1.0 makes response encryption mandatory.
  Unencrypted responses are why `jwkThumbprint` is null in the transcript.
- **No OpenID4VP Digital Credentials API path.** The ISO 18013-7 Annex C path
  (`/present/dcapi/*`, `iso.go`) does use the Digital Credentials API; OpenID4VP over it
  does not exist here. No `dc_api.jwt`, no `expected_origins`, and
  `oid4vp/transcript.go` derives only `OpenID4VPHandover` — not the
  `OpenID4VPDCAPIHandover` of OpenID4VP 1.0 B.2.6.2. `EE-PRO-010a` (SHALL) requires cross-device
  presentation to use the Digital Credentials API; a redirect-based flow SHALL NOT be offered as
  a silent fallback, and where offered at all SHALL be explicit, user-selected, labelled as
  lacking a proximity check, and carry the BCP 247 §6.1 mitigations. For OpenID4VP this verifier
  implements only the redirect flow, without any of those SHALL-level safeguards — it is a fallback path, not a
  generally conformant one — and the `wallet_uri` it returns (`present.go`:93) is a string no
  client here consumes.
- **One attribute per proof.** Circuits are per `(version, num_attributes)`; asking for
  two thresholds at once needs the 2-attribute circuit.
- **No downgrade signalling.** `EE-ZKP-054`–`EE-ZKP-056` require ZKP capability to be an
  *attested* attribute of the relying party's registration, carried in the registration
  certificate — not a self-declaration in the request, because a relying party wanting a
  linkable presentation can simply omit the declaration. This verifier implements neither
  side of that: there is no registration certificate concept here at all (it has no relying-party
  access certificate of any kind, see the request-object bullet above), and the request this
  verifier sends carries no ZKP-capability field, declared or otherwise — a missing declaration
  is not checked for and has no effect, because nothing here reads one.
- **`mso_mdoc_zk` has no HTTP entry yet.** The de-facto OpenID4VP ZK carrier (plan §8.7,
  `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`) is parsed and verified at the library
  level: `oid4vp` builds the `ZkAgeQuery` and parses the `DeviceResponse`, and the presenter's
  `checkZk` verifies it, reached from `POST /present/response/{id}` for any session whose
  query is `mso_mdoc_zk`. But `POST /present/new` has no format switch and issues only
  `mso_mdoc` queries, so no running server creates such a session; only the tests
  (`step8_7_openid4vp_zk_test.go`) do, through `Store.NewWith`.
- **Sessions are in memory.** Nothing is persisted, deliberately — a verifier that keeps
  presentation records is a verifier that can be asked for them.
- **The trust store is a local JSON file.** `issuers.json` stands in for the Commission's
  List of Trusted Entities and the Art. 22 Trusted List (`EE-GOV-010`). It is read once at
  startup: there is no signature over it, no freshness check and no way to withdraw an
  issuer without a restart.
- **The concurrency limit is per process.** `-max-concurrent-verify` bounds admission on
  one instance: there is no shared limiter across replicas, no queueing, and no per-client
  fairness, so a single caller can take every slot and the rest get `503`. It stops the
  thread exhaustion it was written for; it is not rate limiting.
- **The allowlist check did unbounded work on a caller-chosen number** — fixed 24 Sep 2026. `Registry.Check`
  used to hash the circuit named by `version` and `num_attributes` before consulting the accepted
  set, and both came from the presentation: naming `version: 8` cost the verifier ~54 s of
  CPU inside a concurrency slot before the refusal. `zk.CheckCircuit` now matches the tuple against
  the registry's own entries first and hashes only what survives, and the ISO 18013-7
  Annex C path refuses any zkSystemSpecId the session did not offer before the registry
  is consulted at all (`iso.go`, rule (c) of plan §5.3).
