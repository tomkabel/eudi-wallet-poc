# Tests

Every test and check in this repository, where it lives, and what it proves.
`.github/workflows/ci.yml` is the definition of green; the conventions note is
[`docs/DEVELOPMENT.md`](../docs/DEVELOPMENT.md) ("Running the checks locally").

Unit tests sit beside the code they test — Go under `verifier/go/`, the
cross-language transcript check in `wallet/`. This directory holds only the
end-to-end and load harnesses. Do not move or rename anything here: ci.yml
invokes `tests/e2e.sh` by path and shellchecks the glob `tests/*.sh`.

## Unit tests beside the code

### `verifier/go/oid4vp/` — package oid4vp (Go, no Rust toolchain)

Run by the `fast` CI job as `go test ./oid4vp/...`.

- `transcript_test.go` — the OpenID4VP session transcript and DCQL query.
  - `TestSessionTranscriptShape` (:18): the transcript is CBOR starting `83 f6 f6 82 71` (an array of three: null, null, and a 2-element array beginning with the text "OpenID4VPHandover"), ending in a 32-byte bstr (`58 20`) equal to the SHA-256 of the handover-info encoding.
  - `TestSessionTranscriptBinding` (:46): three subtests assert that changing any bound parameter — client_id, nonce, response_uri — changes the transcript bytes. This is the property that stops a proof made for one verifier being replayed at another.
  - `TestEncryptedResponseUsesThumbprint` (:68): a 32-byte jwkThumbprint changes the transcript; a shorter thumbprint is rejected with an error.
  - `TestDCQLSingle` (:83): `AgeQuery(...).Single()` destructures to the requested namespace and element; an empty `DCQL{}` is rejected.
- `transcript_vectors_test.go` — cross-language golden vectors, the Go side.
  - `TestSessionTranscriptGoldenVectorUnencrypted` (:47) and `TestSessionTranscriptGoldenVectorEncrypted` (:58) compare the Go derivation against fixed hex vectors (`vecUnencrypted`, `vecEncrypted`, :30-36) that the Python wallet asserts in `wallet/test_transcript.py`. `TestSessionTranscriptVectorsDiffer` (:72) guards that the encrypted case is actually exercised.
  - The header comment (:18-21) sets the update rule: both sides change together, and a vector must be regenerated from the *other* language, never from the implementation it is testing.
- `session_test.go` — the one-shot session store.
  - `TestStoreNewCleansExpiredSessionsBeforeEnforcingLimit` (:15): with `maxSessions` (10,000, `session.go:44`) sessions live, `New` returns `ErrStoreFull`; once one has expired, `New` succeeds and the store stays at the limit; `ExpectedNow` matches `Created` formatted UTC.
  - `TestStoreCompleteAndGetConcurrently` (:39): 1,000 concurrent `Complete` calls against 1,000 concurrent `Get` calls; `Get` never observes a partially written result.
  - `TestStoreReturnsSnapshotsAndCompletesUnderLock` (:82): mutating a returned session does not mutate the store; `Get` returns the completed result.
- `vptoken_test.go` — `vp_token` validation against the query.
  - `TestVPTokenParseBindsTheQueriedValue` (:42): a 9-case table. CBOR true (`0xf5`) is accepted for the queried attribute; each of the following is rejected with an error containing the named substring: CBOR false `0xf4` ("f4" — a proof the holder is *not* over 18), `zz` ("not hex"), integer `18f5` ("18f5"), empty value ("only accepts"), wrong doctype, wrong namespace, wrong attr_id ("discloses"), unsupported zk_system ("groth16").
  - `TestDCQLSingleRequiresBooleanValues` (:88): the query must carry `values: [true]`; `Single()` rejects nil, empty, `["true"]` and `[true, 18]`.

### `verifier/go/zk/` — package zk (Go, links the Rust staticlib)

`zk_test.go`; package `zk` links `libzkverify.a` through cgo (`zk.go:11`), so
its test binary cannot be linked without a Rust build. The `e2e` job runs it;
the `fast` job cannot.

- `TestCircuitHashKnown` (:5): `CircuitHash(7, 1)` equals the pinned hash `8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121`.
- `TestCircuitHashUnknown` (:16): `CircuitHash(99, 1)` returns an error.
- `TestRegistryRejectsUnlisted` (:22): a registry listing only hash `deadbeef` rejects `Check(7, 1)` — an unlisted circuit cannot pass the accepted-circuit allowlist (EE-ZKP-023).

### `wallet/test_transcript.py` — cross-language vectors, the Python side

- Loads `wallet/present.py` by path and asserts `present.session_transcript()` reproduces both hex vectors and that the two differ; prints `ok — 2 transcript vectors match the Go verifier` and exits 0, or lists failures and exits 1. `--print` emits the vectors to paste into the Go test.
- Run by the `fast` CI job (`python3 wallet/test_transcript.py`, the job's "the wallet's half of the transcript pin" step), or by hand: `python3 test_transcript.py` from `wallet/`.

## `tests/e2e.sh` — the presentation flow and its negative controls

Drives the whole flow against freshly minted attestations: builds nothing,
mints an adult credential (`--over 16 18 21`) and a minor one (`--over 16
--under 18 21`), merges both issuer lists into one trust store, starts
`verifier/zkverify` on a kernel-picked free port, waits for `/healthz`, then
drives `wallet/present.py` once per case and greps the verifier's answer out
of the wallet output. Requires `EE_PROVER` (the `ee_poa_demo` binary);
`EE_VERIFIER` optionally overrides the verifier path; the wallet needs python
`cbor2` and `cryptography`. Five assertions:

1. Happy path (:66-72): the adult proves `age_over_18`; output must contain `"valid": true`.
2. Transcript binding (:74-80): `--tamper-nonce` makes the proof cover a different nonce; the verifier must answer `"valid": false`.
3. Single use (:82-88): `--replay` posts the same `vp_token` twice; the second post must be refused with `HTTP 410` (the store's `ErrAlreadyUsed` maps to `StatusGone`, `present.go:253-254`); the script greps `replay     : HTTP 410`.
4. Predicate binding (:90-98): the minor proves `age_over_18 = false` with `--allow-false-predicate`; the verifier must answer `"valid": false` with a detail that words the refusal as "only accepts 0xf5" (`vptoken.go:116`) — refused on the value, not by accident.
5. The prover's own guard (:100-113): running `ee_poa_demo` for the minor without the override must exit non-zero and name `--allow-false-predicate` — the two defences hold independently.

Output is `PASS: 5/5` or `FAIL: n of 5 assertions failed`; exit status is
non-zero when any assertion fails. Note what is absent: there is no
untrusted-issuer case (the script deliberately trusts both minted issuer keys,
which is why case 4 fails on the predicate value rather than on trust) and no
unlisted-circuit POST case; unlisted-circuit rejection is covered only by
`TestRegistryRejectsUnlisted` above.

## `tests/load_test.py` — admission control under 200 concurrent verifications

- Fires `--requests` (default 200) concurrent POSTs to `/zkverify`, started with `-unsafe-dev-api` (the endpoint 404s without it), each carrying one well-formed proof whose middle byte is flipped — still the right circuit, so a rejection costs a full ~2.5 s verification rather than a parse error.
- Asserts: the verifier process is still alive; every response is `200` or `503` (a dropped connection is recorded as `ERROR ...` and fails); peak OS thread count is below the request count and below 1,000 (Go aborts the process at 10,000 threads); at least one request was shed with `503`, so the admission limit was genuinely exercised.
- Demonstrates why the limit exists: `zk.Verify` is a cgo call that pins an OS thread for its duration, so without shed-load behaviour 200 simultaneous proofs would walk the process toward Go's thread cap — a remote kill switch, one HTTP request per thread.
- Mints and proves once (~15 s the first time); `--work DIR` reuses a previous run's proof; `--max-concurrent-verify` is passed to the verifier (0 leaves it at its default). Not invoked by ci.yml; run it by hand against a built prover and verifier.

## What CI runs, and where the Rust toolchain matters

`fast` (no Rust toolchain): `gofmt -l .` in `verifier/go` (output is the
signal — `gofmt -l` exits 0 either way), `go vet ./...` (type-checks the cgo
package without linking, covering every package), `go test ./oid4vp/...
./internal/... ./circuits/...`,
`python3 -m compileall -q issuer verifier wallet tests`, `ruff check
--isolated --select F,E9 --exclude eudi-arf .` (ruff 0.16.6),
`python3 wallet/test_transcript.py` (the wallet's half of the golden vectors; the
step installs `cbor2` and `cryptography`, which `present.py` imports),
and `shellcheck --severity=warning tests/*.sh`.

`e2e` (needs a Rust toolchain): checks out `google/longfellow-zk` as a sibling
of this repository at `LONGFELLOW_REV` (61a8a735964d1b22bccf79bf14ef6767249cdf92),
builds the prover examples with `RUSTFLAGS=-C target-cpu=x86-64-v3` and asserts
`ee_poa_demo` exists (`test -x`), builds the verifier
(`cargo build --release` in `verifier/zkverify-ffi`, then
`go build -o ../zkverify .`), runs `go test . ./zk/...`, then `pip install cbor2
cryptography` and `tests/e2e.sh` with `EE_PROVER` set. The `zk` and package
`main` tests are here and not in `fast` because package `zk` links the Rust
staticlib through cgo and `main` imports it — neither test binary can be linked
without the Rust build. Between the two jobs
each package is tested exactly once.

## Running the checks locally

The `fast` job, from the repository root:

```bash
cd verifier/go
gofmt -l .
go vet ./...
go test ./oid4vp/... ./internal/... ./circuits/...
cd ../..
python3 -m compileall -q issuer verifier wallet tests
pip install ruff==0.16.6
ruff check --isolated --select F,E9 --exclude eudi-arf .
pip install cbor2 cryptography          # present.py imports them
python3 wallet/test_transcript.py
shellcheck --severity=warning tests/*.sh
```

The `e2e` job — `longfellow-zk` checked out as a sibling at `LONGFELLOW_REV`:

```bash
export RUSTFLAGS=-C target-cpu=x86-64-v3   # ci.yml sets this on the prover build step
mkdir -p ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples
cp zk-age-poc/*.rs ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples/
(cd ../longfellow-zk/rust/applications/mdoc_zk/runtime && cargo build --release --examples --features testonly)
(cd verifier/zkverify-ffi && cargo build --release)
(cd verifier/go && go build -o ../zkverify .)
(cd verifier/go && go test . ./zk/...)
pip install cbor2 cryptography
EE_PROVER=../longfellow-zk/rust/target/release/examples/ee_poa_demo tests/e2e.sh
```

By hand, not in CI:

```bash
EE_PROVER=../longfellow-zk/rust/target/release/examples/ee_poa_demo python3 tests/load_test.py
```

`EE_PROVER` must point at the `ee_poa_demo` binary; with it unset, `e2e.sh`
exits immediately ("set EE_PROVER to the ee_poa_demo binary"). Neither harness
builds anything — build the prover and `verifier/zkverify` first.

## What is not covered

- Issuer trust: `oid4vp/trust.go` loads and validates a trust store, but no automated test asserts that a presentation from an issuer absent from it is rejected (`verifier/README.md`'s measured table records a manual case: issuer not in the trust store → `200` `valid:false`).
- The verifier's HTTP surface (`main.go`, `present.go`: `/present/new`, `/present/response`, `/present/result`, `/healthz`, `/zkverify`): no unit tests; exercised only indirectly by `tests/e2e.sh` and `tests/load_test.py`.
- `issuer/mint_ee_poa.py` and the wallet's presentation logic beyond the transcript: no unit tests; `compileall` only checks that they parse.
- Rust: neither `verifier/zkverify-ffi/` nor `zk-age-poc/` contains `#[cfg(test)]`, and ci.yml runs no `cargo test`; the Rust side is covered only by building and by e2e.sh case 5.
- `tests/load_test.py` is not invoked by ci.yml — run it by hand. (Only `tests/e2e.sh` runs in CI; the root `README.md` says so.)
- No coverage measurement anywhere.
