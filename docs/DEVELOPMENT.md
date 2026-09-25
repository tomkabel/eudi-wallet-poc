# Development and local setup

Bringing the whole stack up on one machine, running it, and what "deployment" does and does
not mean here, and the checks that define green (see "Running the checks locally"). It
covers the ZK spine imported from ee-eudiw; the Android wallet builds with Gradle as the
root README describes.

Four local processes: `issuer/` and `wallet/` are Python CLIs (`mint_ee_poa.py`,
`present.py`); `zk-age-poc/` is the Rust example `ee_poa_demo`, built in the Longfellow
checkout; `verifier/` is `zkverify`, a Go HTTP server over a Rust staticlib in
`zkverify-ffi/`.

## Evidence discipline

- Version numbers, dates, identifiers and legal citations are checked against
  primary sources. Nothing is asserted from memory of the standards landscape.
- Claims that could not be verified against a primary source are marked
  `[UNVERIFIED]` inline and collected in spec §23. If you cannot verify
  something, mark it — do not quietly upgrade a guess to prose.
- When documents and code disagree, find out which is wrong and fix that one.
  Reconciling the prose to a broken implementation is not a fix.

## Prerequisites

- **Python 3.12** — the version CI pins in both jobs. The scripts need `cbor2` and
  `cryptography` (`pip install cbor2 cryptography`); the lint check uses
  `pip install ruff==0.16.6`. There is **no requirements file and no Makefile in this
  repository** — those plain installs in `.github/workflows/ci.yml` are the whole story.
- **Go at the version in [`../verifier/go/go.mod`](../verifier/go/go.mod)** (currently
  `go 1.26`); CI reads it from that file. The module has no third-party dependencies and
  no `go.sum`. Its `zk` package is cgo linking a Rust staticlib, so the build also needs a C
  toolchain.
- **A Rust toolchain** for the prover and the FFI. Nothing here pins one, and CI installs
  none of its own — it uses what the `ubuntu-latest` image carries. `zk-age-poc/REPRODUCE.md`
  records rustc/cargo 1.97.0 for its measured run; a record, not a requirement.
- **Tools**: `jq` and `ip` (iproute2) for `demo/demo.sh`; `curl` for `tests/e2e.sh`;
  `shellcheck` for the checks below.
- **Submodule**: `git submodule update --init` fetches `eudi-arf`, the ARF the specification
  is written against, pinned at `6373eee` (`v3.0.0` plus 17 commits, one of which rewrote
  `ISSU_33b`). It is needed for the spec baseline, not for the two flows below. Commit the
  gitlink when you bump it, and name the revision in the message.

## The Longfellow sibling checkout

`verifier/zkverify-ffi` path-depends on `../../../longfellow-zk`, so `google/longfellow-zk`
must sit **next to** this repository (`<parent>/eudi-wallet-poc`, `<parent>/longfellow-zk`). The
revision is pinned as `LONGFELLOW_REV` at the top of `.github/workflows/ci.yml` — a path
dependency has no lockfile entry. CI fetches that commit without cloning the history, and
checks what it got:

```bash
# run from <parent>
# LONGFELLOW_REV is pinned in .github/workflows/ci.yml under `env:`
LONGFELLOW_REV=61a8a735964d1b22bccf79bf14ef6767249cdf92
git init -q longfellow-zk
git -C longfellow-zk remote add origin https://github.com/google/longfellow-zk.git
git -C longfellow-zk fetch -q --depth 1 origin "$LONGFELLOW_REV"
git -C longfellow-zk checkout -q --detach FETCH_HEAD
got="$(git -C longfellow-zk rev-parse HEAD)"
[ "$got" = "$LONGFELLOW_REV" ] || { echo "got $got, want $LONGFELLOW_REV"; exit 1; }
```

[`../verifier/README.md`](../verifier/README.md) shows the plain clone-and-checkout
equivalent. Either way the directory name must be `longfellow-zk`.

## Build both

From the repository root:

```bash
# prover — the zk-age-poc examples, built inside the Longfellow tree
mkdir -p ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples
cp zk-age-poc/*.rs ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples/
# RUSTFLAGS: see Troubleshooting for why this baseline
(cd ../longfellow-zk/rust/applications/mdoc_zk/runtime && RUSTFLAGS="-C target-cpu=x86-64-v3" cargo build --release --examples --features testonly)
test -x ../longfellow-zk/rust/target/release/examples/ee_poa_demo

# verifier — the Rust staticlib, then the server binary
(cd verifier/zkverify-ffi && cargo build --release)
(cd verifier/go && go build -o ../zkverify .)
```

The `mkdir` is not optional (a fresh checkout has no `examples/`) and the `test -x` is not
decoration — see Troubleshooting. `zkverify-ffi` pins the same `x86-64-v3` baseline in its
own `.cargo/config.toml`, so the verifier build needs no `RUSTFLAGS`.

## Running the checks locally

`.github/workflows/ci.yml` is the definition of green. The `fast` job needs no Rust
toolchain and runs in seconds:

```bash
(cd verifier/go && test -z "$(gofmt -l .)" && go vet ./... && go test ./oid4vp/... ./internal/... ./circuits/...)
python3 -m compileall -q issuer verifier wallet tests
pip install ruff==0.16.6 cbor2 cryptography   # ruff pinned as in ci.yml
ruff check --isolated --select F,E9 --exclude eudi-arf .
python3 wallet/test_transcript.py             # the Python half of the transcript pin
python3 issuer/test_mint_device_key.py
shellcheck --severity=warning tests/*.sh
```

The `e2e` job runs "Build both" above, then the packages that link the staticlib and the
end-to-end script:

```bash
(cd verifier/go && EE_BENCH_SKIP=1 go test . ./zk/...)
EE_PROVER="$PWD/../longfellow-zk/rust/target/release/examples/ee_poa_demo" tests/e2e.sh
```

`EE_BENCH_SKIP=1` skips the step 8 percentile run, a host measurement rather than a check.
`shellcheck` globs `tests/*.sh` and CI invokes `tests/e2e.sh` by path, so keep both where
they are.

## Run the whole stack

Both flows build nothing — run the build first.

### `demo/demo.sh`

```bash
# terminal A: the verifier, bound to the machine's LAN address
demo/demo.sh serve

# terminal B, one presentation per invocation
demo/demo.sh prove adult     # valid:true
demo/demo.sh prove minor     # refused on the predicate value
demo/demo.sh prove tamper    # proof bound to a different nonce
demo/demo.sh prove replay    # 410, the nonce is single-use

# with a sibling-checkout prover (the script's default is this author's path):
EE_PROVER=../longfellow-zk/rust/target/release/examples/ee_poa_demo demo/demo.sh prove adult
```

`serve` mints `demo-data/{adult,minor}` on first use and merges both issuers into one trust
store; `EE_PORT` changes the port. Credentials land in `demo-data/`, gitignored on purpose
(it holds the holder's device private key; the issuer key stays in memory for the run)
and re-minted on demand.

### `tests/e2e.sh`

```bash
EE_PROVER=../longfellow-zk/rust/target/release/examples/ee_poa_demo tests/e2e.sh
```

`EE_PROVER` is required — the script exits immediately without it. It expects the verifier
at `verifier/zkverify` (override: `EE_VERIFIER`), picks a free port, and mints credentials
into a fresh `mktemp` work directory it removes on exit, so nothing lands in `demo-data/`.
This is the script the e2e CI job runs.

## What "deployment" means here

Bluntly: nothing in this repository is deployed, and nothing here is ready to be. No
deployment directory, no Dockerfile, no compose file, no Kubernetes manifest and no service
unit exists for this stack; the one Dockerfile in the working tree,
`eudi-arf/docker/Dockerfile`, belongs to the upstream ARF document repository inside the
submodule, where it builds the ARF's PDFs.

What exists is what you start by hand: three CLIs and one HTTP server on a local or LAN
address. CI builds and tests; it does not package, publish or deploy anything.

The intended production model — a wallet instance with a WSCD on the user's device, a
wallet-provider backend, an issuance plane over X-tee, a trust plane — is drawn in the
specification (§4.1) and mapped to directories in the README's layout table; neither is
implemented here. The spec leaves operations out ("nor the operator's SRE practices",
§0.4); the code says the same: Longfellow's security reviews are in progress and
`REPRODUCE.md` says "Do not ship this to production"; the verifier's README lists what it
does not do yet (unsigned request objects, in-memory sessions, a local JSON trust store, a
per-process concurrency limit); the Python wallet's README notes there is no UI and no WSCD.

## Generated files and their pitfalls

- `demo-data/` holds **the holder's freshly minted device private key**. It is gitignored
  on purpose and `demo/demo.sh` re-mints on demand. The issuer key is generated in memory
  for the run and never written — only its public half reaches `issuers.json`. Never move
  this directory into a tracked path, never commit anything out of it.
- `verifier/zkverify` (built binary), `target/` and `__pycache__/` are ignored. If
  something you added shows up in `git status` under one of these, extend `.gitignore`
  rather than committing it.

## Troubleshooting

**The prover build exits 0 and produces no binary.** The plural `cargo build --examples`
does not fail when `examples/` is missing or empty: it matches no targets, prints a warning
and exits 0, which looks exactly like a build that worked. Skip the `mkdir` or the `cp` and
you get a green build with no `ee_poa_demo`. That is why CI asserts `test -x` on the binary
afterwards; keep that assertion in any script of your own.

**The prover dies with SIGILL on a virtualised host.** The build succeeds; on some hosts the
binary then dies with an illegal-instruction signal. Cause: Longfellow's own
`rust/.cargo/config.toml` sets `-C target-cpu=native`, and the virtualised host used here
advertises AVX-512 features that at least one `native`-selected instruction cannot execute.
The fix, as CI and the FFI do it: build the prover with
`RUSTFLAGS="-C target-cpu=x86-64-v3"`; `verifier/zkverify-ffi/.cargo/config.toml` pins the
same baseline for the verifier side.

**One linker warning is expected.** Building the FFI prints `missing .note.GNU-stack
section implies executable stack`, from a hand-written assembly object in the upstream
`sha2` crate. Upstream's, not ours.
