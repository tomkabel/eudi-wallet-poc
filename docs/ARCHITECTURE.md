# Architecture

How the pieces in this repository fit together, and where the trust boundaries are.
Derived from the working tree on branch `fix/arf-3.0.0-audit-remediation`, 12 September 2026; first
derived at `6d25b66` and revised since, with every `file:line` citation re-checked against the tree on
that date. Where a README and the code disagree, this document notes the disagreement
rather than resolving it silently; each claim names the file it was read from.

> **Imported from ee-eudiw at `e368fc1`.** This describes that repository. Its `README.md`,
> `AGENTS.md` and `.gitmodules` citations are ee-eudiw's files, and `ui-mock-bilt-me` was not
> imported. The Android holder that §"The Android holder, outside this tree" places in a
> separate fork is `app/` in this repository. The verifier's Go module is now
> `github.com/tomkabel/eudi-wallet-poc/verifier/go`.

## 1. What is in the tree, and the decision that shapes it

One repository holds two things on purpose (`AGENTS.md`:12-14):

- `spec/EE-EUDIW-TS-1.0.md` — **the deliverable**: an independent Estonian EUDIW
  technical specification, 187 normative `EE-<AREA>-<nnn>` requirements, traceable to an
  ARF high-level requirement, an implementing act or Estonian statute, with Annex E carrying the
  matrix as an extract (105 of the 187); content revision 1.4 (`README.md`:32; `AGENTS.md`:16; `spec/EE-EUDIW-TS-1.0.md`:7).
- `issuer/`, `wallet/`, `verifier/`, `zk-age-poc/` — **the reference implementation**:
  Python mints and presents, Go verifies over the Rust Longfellow runtime, and
  `zk-age-poc/` is the standalone measured PoC (`AGENTS.md`:14).

The single most important architectural decision is which of the two is authoritative:
**the specification is the deliverable and the code must match it.** `AGENTS.md`:26
states the rule operationally — when documents and code disagree, find out which is
wrong and fix that one; do not reconcile prose to a broken implementation. Two
consequences visible in the tree: the demo exercises the spec's zero-knowledge
proof-of-age profile rather than a product surface (`README.md`:3-4), and the PoC's gaps
are named rather than hidden (`issuer/README.md`:87-104, `wallet/README.md`:57-67,
`verifier/README.md`:261-314, `README.md`:86-92 for the negative controls).

## 2. The end-to-end flow

One presentation, with the real endpoints and functions:

```text
issuer/mint_ee_poa.py --out out --batch 30
  ec.generate_private_key(SECP256R1)                       mint_ee_poa.py:186
  mint_one(): IssuerSignedItem per element wrapped in tag 24,
              MSO >= 256 bytes, COSE_Sign1 issuerAuth (ES256),
              deviceSignature over the issuer's own placeholder transcript
  writes out/000/.../{mdoc.bin, device_key.pem, params.txt, vector.json}
         out/issuers.json                                  mint_ee_poa.py:274-320

verifier/zkverify          POST /present/new  {"element": "age_over_18"}
  oid4vp.AgeQuery("proof_of_age", <doctype>, <ns>, element)  present.go:83
  store.New(): 12-byte session id, 32-byte nonce, expected_now  session.go:59-93
  -> 201 {id, request_uri, wallet_uri, result_uri}             present.go:90-96

wallet/present.py          GET /present/request/{id}
  <- {response_type, response_mode: direct_post, client_id, nonce, state,
      dcql_query, expected_now}                                present.go:50-61
  session_transcript(client_id, nonce, None, response_uri)     present.py:39-51
  resign_device_auth(): deviceSignature over
      Tag24(DeviceAuthentication = ["DeviceAuthentication", transcript,
                                    docType, DeviceNameSpacesBytes])  present.py:54-66
  subprocess: ee_poa_demo <session-dir> age_over_18             present.py:140-146
      -> proof.bin, request.json                               ee_poa_demo.rs:108-131

                           POST /present/response/{id}  {"vp_token": {...}}
verifier/zkverify
  store.Claim(id)     — marks the nonce answered, before verification  session.go:124-140
  check():
    trust.For(doc_type)             — issuer keys from the verifier's own file  present.go:204
    zk.CheckCircuit(version, num_attributes) — circuit allowlist, first     present.go:209
    s.Transcript()                  — recomputed, never received from the wallet  session.go:156-160
    zk.Verify(...)                  — cgo -> zkv_verify -> Rust runtime     zk.go:58-99
  -> 200 {"valid": true, "detail": "age_over_18 = 0xf5, issued by EE-EUDIW demo issuer"}
                                                                   present.go:224-225
GET /present/result/{id}            — the relying party's own page polls  present.go:237-247
```

The full endpoint surface, all in `verifier/go`: `POST /present/new` starts a session
(`present.go`:74-106); `GET /present/request/{id}` serves the unsigned authorization
request (`present.go`:109-117); `POST /present/response/{id}` is the `direct_post`
endpoint that consumes the `vp_token` (`present.go`:120-177); `GET /present/result/{id}`
returns the outcome (`present.go`:352-362); `POST /zkverify` is a low-level harness,
registered only under `-unsafe-dev-api` and a `404` otherwise (`main.go`:205-206,
264-276); `GET /circuits`, `GET /issuers`, `GET /healthz` expose the accepted circuits,
the trust store and liveness (`main.go`:291-299).

`presenter.check` (`present.go`:188-248) pulls the single credential query out of the
session (`dcql.go`:135-178), parses the `vp_token` envelope (`vptoken.go`:36-92), checks
the presented value against the query's value constraint (`vptoken.go`:97-118,
`EE-ZKP-021(b)`), then tries each trusted issuer for the doctype in turn and returns on
the first success (`present.go`:227-242). The negative controls that prove the binding
holds are `tests/e2e.sh`:66-121 (five assertions) and `wallet/README.md`:50-55.

## 3. Components

| Path | Language | Entry point | Responsibility | Talks to the next one via |
|---|---|---|---|---|
| `spec/` | Markdown | `EE-EUDIW-TS-1.0.md` | the normative deliverable | n/a — the code cites it |
| `issuer/mint_ee_poa.py` | Python 3 (`cbor2`, `cryptography`) | CLI, `python3 mint_ee_poa.py` | mints EE-PoA `DeviceResponse`s, singly or as a batch; writes the verifier's trust-store entry | files on disk: `mdoc.bin`, `device_key.pem`, `params.txt`, `issuers.json` |
| `wallet/present.py` | Python 3 (`cbor2`, `cryptography`, `urllib`) | CLI, `python3 present.py` | fetches the request, derives the transcript, re-signs `deviceAuth`, invokes the prover, posts the `vp_token` | HTTP to the verifier; **subprocess** to the Rust proving binary |
| `zk-age-poc/*.rs` | Rust | copied into longfellow-zk and built there | the `ee_poa_demo` proving example; `REPRODUCE.md` holds the standalone measurements | built binary invoked as a subprocess by the wallet |
| `verifier/go` | Go 1.26 (module `github.com/tomkabel/eudi-wallet-poc/verifier/go`) | `go build -o ../zkverify .` | the relying-party HTTP service: OpenID4VP 1.0, DCQL, trust store, circuit allowlist; the ISO 18013-7 Annex C (dcapi) endpoints and page (`iso.go`, `iso.html`, off unless `-dcapi-origin` is set) | **cgo staticlib** into `zkverify-ffi` |
| `verifier/zkverify-ffi` | Rust, `crate-type = ["staticlib", "rlib"]` | `cargo build --release` | C ABI (`zkv_verify`, `zkv_circuit_hash`) over the Longfellow Rust runtime | statically linked into the Go binary |
| `verifier/go/oid4vp` | Go package | — | sessions, DCQL, session transcript, trust store, `vp_token` parsing; for ISO 18013-7 Annex C (dcapi) the DeviceRequest, EncryptionInfo, HPKE and ZkDocument parsing (`isodcapi.go`, `hpke.go`, `session_iso.go`, `zkdocument.go`) | in-process from `present.go` / `iso.go` / `main.go` |
| `verifier/go/internal/cborsub` | Go package | — | the strict CBOR subset decoder for the holder-supplied DeviceResponse (ADR-002) | in-process from `oid4vp` |
| `verifier/go/zk` | Go package | — | the cgo wrapper: error mapping, hash lookup | in-process |
| `verifier/go/circuits` | Go package | — | the circuit registry: load, tuple lookup, spec ids; plain Go, so `oid4vp` does not link cgo | in-process |
| `verifier/circuits.json` | JSON data | — | the circuit allowlist (`EE-ZKP-030`) | read at startup by `circuits.Load` |
| `demo/demo.sh` | Bash | `serve` / `prove adult\|minor\|tamper\|replay` | drives the whole spine for the Android recording | runs the verifier and the wallet |
| `tests/e2e.sh`, `tests/load_test.py` | Bash, Python | CI and manual | end-to-end assertions; concurrency/shed-load test | drives the built binaries |
| `eudi-arf`, `ui-mock-bilt-me` | submodules | — | the ARF the spec is written against; the Expo/Android demo UI | pinned gitlinks (`.gitmodules`:1-6) |

### 3.1 The language boundary, precisely

There are two Go/Python→Rust boundaries, and they are different mechanisms.

**Verifier → Rust: cgo staticlib, no subprocess and no environment variable.** The Go
package `zk` carries the cgo directives (`verifier/go/zk/zk.go`:9-14):
`#cgo LDFLAGS: -L${SRCDIR}/../../zkverify-ffi/target/release -lzkverify`, including
`verifier/include/zkverify.h`. The Rust side is `zkverify-ffi`, a crate whose
`crate-type` includes `staticlib` (`verifier/zkverify-ffi/Cargo.toml`:9), and whose
dependency is a **path dependency** on the Longfellow runtime:
`mdoc-zk-runtime = { path = "../../../longfellow-zk/rust/applications/mdoc_zk/runtime" }`
(`Cargo.toml`:12). That is why the two trees must be siblings (`ci.yml`:19-22) and why
the Longfellow revision is pinned in CI (`LONGFELLOW_REV`,
`61a8a735964d1b22bccf79bf14ef6767249cdf92`, `ci.yml`:13-15; the same revision in
`verifier/README.md`:23-25). Build order: `cargo build --release` in `zkverify-ffi`,
then `go build -o ../zkverify .` (`verifier/README.md`:27-31; `ci.yml`:179-185). The Go
code reads flags only — there is no `os.Getenv` anywhere in `verifier/go`.

**Wallet → Rust: subprocess.** `present.py` takes `--prover` (default: the `EE_PROVER`
environment variable, else `~/longfellow-zk/rust/target/release/examples/ee_poa_demo`)
and runs it as a child process with the session directory and the predicate name
(`wallet/present.py`:87-89, 140-146). The binary is built from `zk-age-poc/*.rs`, which
are *copied into* the longfellow-zk tree before building (`ci.yml`:170-173,
`issuer/README.md`:124-130); this repository does not vendor longfellow-zk. Demo
defaults: `EE_PROVER` and `EE_PORT` (`demo/demo.sh`:14-15); `tests/e2e.sh` additionally
honours `EE_VERIFIER` (`tests/e2e.sh`:14).

Build note from the same sources: Longfellow's own `rust/.cargo/config.toml` sets
`-C target-cpu=native`, which produced a SIGILL binary on the virtualised host used here;
CI and `zkverify-ffi` pin `-C target-cpu=x86-64-v3` instead
(`verifier/zkverify-ffi/.cargo/config.toml`:1-4; `ci.yml`:161-165; `issuer/README.md`:117-140).

### 3.2 CI

`.github/workflows/ci.yml` is the definition of green, in two jobs (`AGENTS.md`:29-60):

- **`fast`** (`ci.yml`:25-113), no Rust toolchain: `gofmt -l` (`:45-55`), `go vet ./...`
  (`:57-61`), `go test ./oid4vp/... ./internal/... ./circuits/...` (`:63-68`) — every
  package whose test binary links without the Rust staticlib — `python3 -m compileall`
  (`:70-71`), `ruff check --isolated --select F,E9` (`:73-85`),
  `python3 wallet/test_transcript.py` (`:87-99`), `python3 issuer/test_mint_device_key.py`
  (`:101-105`), `shellcheck --severity=warning tests/*.sh` (`:107-113`).
- **`e2e`** (`ci.yml`:115-201): clone longfellow-zk at the pinned revision (`:135-144`),
  copy `zk-age-poc/*.rs` in and `cargo build --release --examples --features testonly`
  (`:160-173`), assert the binary exists with `test -x` (`:175-177`) because
  `cargo build --examples` exits 0 without producing it, build the FFI and the verifier
  (`:179-185`), `go test . ./zk/...` (`:187-194`) — package `main` and `zk`, the two that
  link it — run `tests/e2e.sh` with `EE_PROVER` (`:196-201`).

Both halves of the transcript pin are enforced: the Go vectors by `go test ./oid4vp/...`
(`verifier/go/oid4vp/transcript_vectors_test.go`:47-77) and the Python half by the fast
job's transcript-pin step (`ci.yml`:87-99).

## 4. Trust and verification boundary

### 4.1 What the verifier must trust

- **The trust store** — a local JSON file mapping doctype to issuer P-256 keys
  (`verifier/go/oid4vp/trust.go`:32-54). The verifier takes issuer keys from here and
  nowhere else (`trust.go`:10-24); the presentation deliberately does not carry the
  issuer public key (`vptoken.go`:11-19). In the demo it is the `issuers.json` the issuer
  wrote (`mint_ee_poa.py`:367-370), loaded with `-issuers` (`main.go`:199, 225-228). The
  spec's stand-in target is the Commission's LoTE plus the Art. 22 Trusted List
  (`EE-GOV-010`, `spec/EE-EUDIW-TS-1.0.md`:224; `trust.go`:15-17).
- **The circuit allowlist** — `circuits.json` loaded by `circuits.Load`
  (`verifier/go/circuits/registry.go`:71-87). The specification puts this check *before* proof
  verification (`EE-ZKP-023`, `verifier/go/circuits/registry.go`:16-22) and requires the set to be cached
  rather than fetched per presentation (`EE-ZKP-032`, `verifier/go/circuits/registry.go`:20-22). Why a
  relying party owes the user one, in the spec's words: *"The `circuit_hash` allowlist is
  a trust root. Whoever controls it controls what proofs verify, and a silent substitution
  is an undetectable soundness break."* (`spec`:590). Without it the wallet names the
  circuit (`vptoken.go`:21, 70-72) and a verifier that trusts that name verifies a proof of
  something other than what it thinks it asked for. The verifier README lists the allowlist
  among "the checks a relying party actually owes the user" (`verifier/README.md`:17-18).
- **Its own session state.** The nonce and `expected_now` are fixed at session creation
  (`session.go`:72-88); the verifier recomputes the transcript from them
  (`session.go`:235-239) and verifies the validity window against *its* `expected_now`
  (`present.go`:205-207, 235); the `expected_now` a wallet might send in the response
  body is ignored (`present.go`:143-146). The request object is unsigned, so `client_id`
  is a claim, not a credential (`present.go`:38-43) — the named `EE-RP-002/003` gap.
- **Its own build and clock.** The Rust runtime is statically linked; the FFI catches
  Rust panics per call and maps them to an error code (`lib.rs`:74, 120-130;
  `zk.go`:94-96). `ExpectedNow` derives from the session's creation time
  (`session.go`:88), so a wrong system clock is a wrong validity check.

It must **not** trust: the issuer key from the presentation (`trust.go`:10-24,
`present.go`:209), any caller-supplied `now` outside the dev harness (`main.go`:265-274),
or "an answer" as proof of anything the circuit did not assert — the presented CBOR value
is matched against the query's constraint (`vptoken.go`:97-118). Transport is plain
`direct_post`, unencrypted, with a null `jwkThumbprint` in the transcript
(`session.go`:235-239; `verifier/README.md`:96-98, 268-269): the transcript buys binding,
not confidentiality.

### 4.2 The session transcript

`oid4vp/transcript.go` implements OpenID4VP 1.0 Appendix B.2.6.1 (`transcript.go`:12-48):
`SessionTranscript = [null, null, ["OpenID4VPHandover",
sha256(cbor([clientId, nonce, jwkThumbprint, responseUri]))]]`. The wallet re-signs
`deviceAuth` over that structure at presentation time — the issuer's own `deviceSignature`
is a placeholder because the issuer never saw the session (`wallet/README.md`:26-33,
`present.py`:54-66). Because the transcript is an input to the circuit and is never
transmitted, the two independent implementations are pinned together by golden vectors
asserted from both sides: `wallet/test_transcript.py`:23-42 and
`verifier/go/oid4vp/transcript_vectors_test.go`:24-45, covering `direct_post` (null
thumbprint) and `direct_post.jwt` (a 32-byte one). Neither side may regenerate a vector
from the implementation it is testing (`wallet/test_transcript.py`:9-12).

### 4.3 Single use, and what it covers

`Store.Claim` marks a session answered before any verification work begins
(`session.go`:176-194, called at `present.go`:166`); a replayed `vp_token` gets
`410 session already answered` (`present.go`:364-372`, verified by `tests/e2e.sh`:82-88
and `wallet/present.py`:178-183`). Two limits on what that buys:

- It protects the **nonce**, not the attestation. A ZK presentation SHALL NOT consume the
  attestation (`EE-ZKP-025`, `spec`:588`), while `EE-POA-013` requires the wallet to
  delete a plain-mdoc attestation after one presentation (`spec`:451`) — and
  `wallet/present.py` does not implement that half (`wallet/README.md`:63-67`).
- Sessions are in memory only and expire after `-session-ttl` (default 3 minutes,
  `main.go`:204`); `maxSessions` is 10,000 (`session.go`:53`). Nothing is persisted,
  deliberately: "a verifier that keeps presentation records is a verifier that can be
  subpoenaed for them" (`session.go`:44-46`).

### 4.4 The concurrency bound

`zk.Verify` is a cgo call that holds an OS thread for its whole (~2.5 s) run, and Go
aborts the process past 10,000 threads, so unbounded arrival was a remote kill switch
(`main.go`:64-69`). The limiter is a non-blocking channel: over the bound, requests are
shed with `503` and `Retry-After: 3` rather than queued (`main.go`:80-95`,
`present.go`:153-162`). The default bound is `runtime.NumCPU()`, flag
`-max-concurrent-verify` (`main.go`:207-222`). In `handleResponse` the slot is taken
before `Claim` and released after `Complete`, so a shed request never burns a session
whose reply can no longer be sent (`present.go`:153-163`), and one slot covers the whole
issuer loop in `check` (`present.go`:181-183, 227-242`). `tests/load_test.py` fires 200
concurrent verifications and asserts 200-or-503, never a dropped connection
(`load_test.py`:2-11`). The bound is per process (`verifier/README.md`:304-307`).

### 4.5 Fail-closed

| Situation | Behaviour | Where |
|---|---|---|
| registry file unreadable or empty at startup | process exits (`log.Fatalf`) | `verifier/go/circuits/registry.go`:72-87; `main.go`:225-228 |
| registry entry whose hash disagrees with the binary at startup | process exits (`log.Fatalf`) — plan §5.3 self-check | `main.go`:232-241 |
| trust store unreadable, invalid, or empty | process exits | `trust.go`:32-54; `main.go`:243-246 |
| `-max-concurrent-verify` < 1 | process exits | `main.go`:214-216 |
| doctype has no trusted issuer | `403`, no verification | `present.go`:210-213 |
| circuit not in the allowlist | `403`, no verification | `present.go`:215-219 |
| zkSystemSpecId not offered in this session (dcapi path) | `403`, no verification, no FFI | `iso.go`:296-300 |
| no such session | `404` | `present.go`:364-373 |
| session expired or already answered | `410` | `present.go`:364-373 |
| malformed body / wrong method | `400` / `405` | `present.go`:74-96, 120-151 |
| all verification slots busy | `503` + `Retry-After: 3`, before `Claim` | `present.go`:153-162; `main.go`:93-95 |
| proof does not verify | `200 {"valid": false}` — "an answer, not a failure" | `main.go`:181-187; `present.go`:243-246; `iso.go`:346 |
| issuer key not trusted (redirect path) | `200 {"valid": false}`, "no trusted issuer's key verifies this proof" | `present.go`:243-246 |
| issuer key not trusted (dcapi path) | `403`, "no trusted issuer matches this presentation" | `iso.go`:309-313 |
| issuer would emit an MSO under 256 bytes | mint refuses; the circuit requires the two-byte tag-24 length form | `mint_ee_poa.py`:314-316; `issuer/README.md`:65-69 |
| prover asked for a predicate the attestation says is false | prover panics naming `--allow-false-predicate`; verifier refuses `0xf4` on the query's value | `ee_poa_demo.rs`:74-78; `tests/e2e.sh`:92-113 |
| prover run fails inside the wallet | wallet exits non-zero | `present.py`:143-146 |
| replay accepted (the defence failed) | wallet exits non-zero | `present.py`:178-183 |

Error text that could name internal state stays in the server log; callers get a category
(`verifyErrorCategory`, `main.go`:100-111).

The former known inversion is fixed: `zk.CheckCircuit` now matches the
presentation's `(version, num_attributes)` against the registry's published
entries *before* any hash is computed, and only an accepted entry earns the
FFI call that confirms its hash (`verifier/go/zk/zk.go`, `CheckCircuit`). Holder-supplied
numbers can no longer make the refusal itself cost 54–58 s; the dcapi path
additionally refuses any zkSystemSpecId the session did not offer before the
registry is consulted at all.

### 4.6 Measured behaviour, with sources

Numbers are not interchangeable between binaries or hosts — quote each with its source:

| What | Numbers | Source |
|---|---|---|
| `age_demo`, Google's test mDL, 2 vCPU | circuit 480,913 B loaded in 4.84 ms; PROVE 360,276 B in 740 ms; VERIFY `Ok("ok")` in 265 ms; peak RSS 112 MB; runtime build 50.6 s | `zk-age-poc/REPRODUCE.md`:12, 37-40, 55-59 |
| `ee_poa_demo` over this issuer's attestation | PROVE 360,212 B in 6.98 s; VERIFY in 2.79 s; wrong issuer key and tampered proof `Err(GeneralFailure)` | `issuer/README.md`:26-30 |
| full `present` flow, 2 vCPU | proving ~6.1 s, verification ~2.5 s; valid 2414 ms, different batch member 2427 ms, flipped bit 2492 ms, unknown circuit 11 ms (not reproduced since) | `verifier/README.md`:105-115, 157-195 |
| recording run, 8 vCPU, `-C target-cpu=native` | PROVE 6.1–6.5 s, VERIFY 2.5 s, proof ~360 KB, ~16.7 s wall clock — the figures the demo README says to quote, instead of the 740 ms/265 ms pair | `demo/README.md`:11-24 |
| one batch of 30 | 30/30 distinct issuer signatures, 1 distinct `ValidityInfo` | `issuer/README.md`:40-47; printed by `mint_ee_poa.py`:426-435 |

## 5. Data and key handling in the demo

- **Issuer key** — generated fresh, in memory, per mint run (`mint_ee_poa.py`:263); it is
  never written to disk. Only the public key (`pkx`, `pky`) is published into
  `issuers.json`, `params.txt` and `vector.json` (`mint_ee_poa.py`:353-370, 413-419).
  Consequence: a re-mint is a new issuer key, so `demo.sh` and `tests/e2e.sh` merge
  several `issuers.json` files into one trust store (`demo/demo.sh`:26-30;
  `tests/e2e.sh`:40-47). The self-signed document-signer certificate is written as
  `issuer_dsc.pem` (`mint_ee_poa.py`:170-198, 384`).
- **Device key** — one per attestation, generated at mint time (`mint_ee_poa.py`:291`),
  written to `device_key.pem` with mode `0600` (the script tightens the descriptor before
  writing, because `os.open`'s mode does not change an existing file:
  `mint_ee_poa.py`:397-412`). The comment there states the intended contrast: a real
  wallet keeps this in a WSCD and it never leaves the device (`mint_ee_poa.py`:385-387`;
  `EE-SEC-001`/`EE-SEC-003`, `spec`:202-208`). Here it is a PEM file next to the credential —
  "exactly the thing a real wallet must not do" (`wallet/README.md`:31-33`).
- **Salts and timestamps** — fresh 32 random bytes per element per attestation
  (`mint_ee_poa.py`:298-305`); the batch shares one coarsened `ValidityInfo` (midnight
  UTC, `mint_ee_poa.py`:258-261`).
- **`demo-data/` is gitignored because it holds a private key** (`.gitignore`:6-9;
  `demo/README.md`:53-55; `AGENTS.md` §"Generated files and their pitfalls").
  `demo/demo.sh` mints `demo-data/{adult,minor}` on first use and merges their issuer
  entries (`demo/demo.sh`:23-31`). The tree also gitignores the built
  `verifier/zkverify` binary and `out/` (`.gitignore`:4-5`).
- Precision: `demo-data/` holds the holder's device private key, not the issuer's.
  `mint_ee_poa.py` writes only `device_key.pem` as a private key (`:295-310`); the
  issuer key is in memory for the run (`:186`) and nothing in the flow needs it
  after minting. `.gitignore`:6-9, `demo/README.md`:53-55, `docs/DEVELOPMENT.md`:94-96
  and `AGENTS.md` (§"Generated files and their pitfalls") now all say this.

## 6. What is deliberately not built

Scope, drawn from the components' own statements and the spec's scope section
(`spec`:36-40`).

- **No wallet application.** "A wallet. There is no UI, no user approval step, no
  dashboard, no attestation storage or selection, no WSCD, and no batch management — the
  credential directory is passed in on the command line" (`wallet/README.md`:57-61`).
  The `ui-mock-bilt-me` submodule is a UI mock with no bridge to the verifier — "Nothing
  in the UI talks to the verifier … the two are recorded side by side" — and its stepper
  is a timed animation (`demo/README.md`:3-5, 82-87`).
- **No Android holder, and no cross-device path.** None of this repository's own implementation
  directories (`issuer/`, `wallet/`, `verifier/`, `zk-age-poc/`, `demo/`, `tests/`) contain Android
  source of any kind — no `.kt`, no `.gradle`, no `AndroidManifest.xml` — and the verifier implements
  OpenID4VP over the redirect flow only (its one Digital Credentials API path is ISO 18013-7
  Annex C, `iso.go`): `verifier/go/oid4vp/transcript.go` derives `OpenID4VPHandover`
  and not the `OpenID4VPDCAPIHandover` of OpenID4VP 1.0 B.2.6.2, and neither `dc_api.jwt`
  nor `expected_origins` is used by any code in those directories (the spec discusses both
  normatively, and this document and the READMEs name them descriptively, which is different
  from an implementation consuming them). The `ui-mock-bilt-me` submodule is Android-adjacent
  only through its `node_modules` — Expo/React Native dependency internals, not source authored
  for this project. The spec's preferred remote flow
  (`EE-PRO-010`, `EE-PRO-010a`) therefore has no counterpart here, and the `wallet_uri`
  string `present.go` returns is consumed by nothing in this repository. The gap is
  analysed in ee-eudiw's `docs/analysis/CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md`.
- **No PID issuance, no EU AV Profile attestation.** The only thing minted is
  `ee.riik.poa.1` as an mdoc (`mint_ee_poa.py`:48`). The spec defines interop with
  `eu.europa.ec.av.1` (`spec`:387; `EE-POA-003`, `spec`:411`) but the issuer has no such
  path.
- **No OpenID4VCI.** Batch issuance here writes files to a directory; there is no
  `proofs` parameter and no key attestation (`issuer/README.md`:93-95`), so the rest of
  `EE-POA-010` (`spec`:445`) is unimplemented.
- **No authoritative-source enrolment.** Predicates come from CLI flags, not the
  Population Register over X-tee, and no ICAO passive authentication is performed — the
  trust-the-client weakness the spec warns about in §9.4 (`issuer/README.md`:89-92`;
  `spec`:427`).
- **No WSCD or HSM.** Both device and issuer keys are software keys
  (`issuer/README.md`:96`; `wallet/README.md`:31-33`).
- **No revocation or status checking.** The presentation envelope has no status field
  (`vptoken.go`:20-29`) and `check` consults no status source (`present.go`:188-248`); the
  spec's §13 has no counterpart in this tree.
- **From the verifier's own list** (`verifier/README.md`:261-314`): the request object is
  unsigned (no JAR, no relying-party access certificate); `direct_post` not
  `direct_post.jwt`; one attribute per proof (circuits are per `(version, num_attributes)`,
  `dcql.go`:131-134`); no downgrade signalling (`EE-ZKP-052`); sessions in memory; a local
  JSON trust store read once at startup with no signature, freshness check, or revocation
  without a restart; and a per-process concurrency limit with no queueing or per-client
  fairness.
- **The `vp_token` encoding is interim.** ISO/IEC 18013-5 second edition defines a
  `ZkDocument` (§10.2.7 of the DIS) that is not yet published; until then the envelope is
  a versioned JSON stand-in, explicitly not an interoperability claim (`vptoken.go`:11-19`;
  `verifier/README.md`:124-130`).
- **Not for production.** Upstream states two independent security reviews are in
  progress; "Do not ship this to production" (`zk-age-poc/REPRODUCE.md`:86-87`).

### The Android holder, outside this tree

The holder side of the conformance plan
(ee-eudiw's `docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` §4)
is implemented in a separate fork, not here:
[`tomkabel/eudi-wallet-poc`](https://github.com/tomkabel/eudi-wallet-poc), an independent fork of
[`open-eid/eudi-wallet-poc`](https://github.com/open-eid/eudi-wallet-poc), at revision
`step8-measurement-harness @ 2396245` (branch chain `step1-fork-identity` →
`step3-holder-obligations` → `step5-hardware-keys` → `step4-protocol-hygiene` →
`step6-issuance-consumption` → `step7-conformity-note` → `step8-measurement-harness`, by commit as
recorded in `planning/STEP7-RECORD.md` and `planning/STEP8-RECORD.md`). The fork carries the wallet-side work of plan steps 1,
3–7 and the device half of the plan §6 measurement harness:
the fork's independent identity (Firebase reporting removed), the `EE-ZKP-042` pre-share notice and
the `EE-ZKP-051` strict refusal, multipaz 0.99.0 `AndroidKeystoreSecureArea` device keys
(StrongBox where `FEATURE_STRONGBOX_KEYSTORE`, TEE otherwise), the protocol and registry hygiene of
step 4, and `ee.riik.poa.1` issuance in one transaction with consumption on plain presentation.
Its conformity statement is the fork's `docs/CONFORMITY.md` (`step7-conformity-note`).

Everything device-bound is still pending: no device measurement has been made, key-generation
timings and slot capacity remain unmeasured (spec §23 item 26), and the on-device acceptance items
of plan §6 (one Pixel 8 and one mid-range phone (e.g. a Samsung Galaxy A-series) — the intended set, not a
measurement; §23 item 12 stays open) have not been run. The measurement harness exists on both
sides (the fork's `ZkProofBenchmark`, this repository's `TestMultipazVerifyLatencyPercentiles` over
the step-0 fixture, host figure recorded in `verifier/README.md`); the device rows are
PENDING-DEVICE (the fork's `docs/MEASUREMENTS.md` on `step8-measurement-harness`; this
repository's `docs/planning/STEP8-RECORD.md`).

## 7. Pointers: which spec sections are normative for which component

| Code | Normative source in `spec/EE-EUDIW-TS-1.0.md` |
|---|---|
| EE-PoA data model, identifiers, `ee.riik.poa.1` | §9.2 (`spec`:382-413`); Annex C CDDL sketch (`spec`:1185`); `EE-POA-001` (`spec`:405`) |
| `issuer --batch`, `EE-POA-010`–`013` | §9.5 batch issuance and one-time use (`spec`:441-455`); `EE-POA-012` timestamps (`spec`:449`) |
| `zk-age-poc` provable properties | §10.5 Track A profile, `EE-ZKP-020`–`025` (`spec`:576-589`); `EE-ZKP-021` (`spec`:580`) |
| `verifier/circuits.json`, `verifier/go/circuits/registry.go` | §10.6 circuit governance (`spec`:590-601`); `EE-ZKP-023` (`spec`:584`), `EE-ZKP-030`/`031`/`032` (`spec`:594-598`) |
| performance budget the measurements answer to | §10.7 (`spec`:602`) |
| `verifier/go/oid4vp`, `wallet/present.py` protocol surface | §11.1–11.2 (`spec`:655-687`); `EE-PRO-002` no Presentation Exchange (`spec`:672`); Annex B DCQL examples, B.1 age-check (`spec`:1098-1118`) |
| trust store, issuer acceptance | §5.1 trust anchors (`spec`:220`); `EE-GOV-010` (`spec`:224`) |
| the reason the PoA carries booleans only | §7.4 (`spec`:331`); Annex D (`spec`:1215`); `EE-PID-007` per `issuer/README.md`:82` |
| threat model the negative controls cover | §17.2 (`spec`:918`) |
| open issues and unverified claims | §23 (`spec`:1048`) — read before relying on any figure here |

Known code-vs-spec gaps to keep visible: the wallet does not consume attestations
(`EE-POA-013`, `spec`:451`), does not use a WSCD (`EE-SEC-001`, `spec`:202`), and the
verifier does not sign its request object or use a relying-party access certificate
(`EE-RP-002/003`, `spec`:234`, `present.go`:38-43`). `issuer/README.md`:87-104`,
`wallet/README.md`:57-67` and `verifier/README.md`:261-314` are the authoritative
per-component lists; this document summarises them and should not override them.

Running the checks is described in `AGENTS.md`:29-60 and `demo/README.md`:29-53, not
here — this file is about structure, not commands.
