# Plan: import the EE-EUDIW spec and the ZK age-proof spine

Source: `~/Documents/ee-eudiw` at `c8a42d1` ("Profile the de-facto mso_mdoc_zk OpenID4VP
carrier"). Target: this repository, branch `ee-eudiw-zk-import`.

This file is the plan only. Nothing below has been executed yet except creating the branch.

## Scope

**Copied in full, no choice involved:**

| From ee-eudiw | Tracked files | What it is |
|---|---|---|
| `spec/EE-EUDIW-TS-1.0.md` | 1 | The whole specification, rev 1.4, 1559 lines, byte-for-byte |
| `zk-age-poc/` | 3 | `age_demo.rs`, `ee_poa_demo.rs` (the prover), `REPRODUCE.md` |
| `issuer/` | 2 | `mint_ee_poa.py` (EE-PoA mdoc minting), README |
| `wallet/` | 3 | `present.py` (Python holder), `test_transcript.py`, README |
| `verifier/` | 76 | Go verifier (`oid4vp`, `zk`, `internal/cborsub`), `zkverify-ffi` Rust staticlib, `include/zkverify.h`, `circuits.json`, `verify_demo.py`, all `testdata/` fixtures, README |
| `tests/` | 3 | `e2e.sh`, `load_test.py`, README |
| `demo/` | 2 | `demo.sh`, README |

**Not copied:** the built `verifier/zkverify` binary (14.6 MB), `zkverify-ffi/target/` (149 MB),
`__pycache__/`, `.ruff_cache/`, `demo-data/`. All of these are build output or secrets, and
ee-eudiw's own `.gitignore` excludes them.

**Decided with you (decision-picker, logged in `.decisions.log`):**

| Question | Your pick |
|---|---|
| Base branch | Integration branch: `step8-7-wallet-side` plus the three fixture branches. All four have since merged into `master` (fork PRs #1–#13), so the base is `master` at `edafc46` |
| Docs beyond the spec | ZK-relevant docs only (list in step 3) |
| Submodules | `eudi-arf` only, pinned at `6373eee`. `ui-mock-bilt-me` stays out |
| CI | Port the ZK `fast` and `e2e` jobs. No Gradle job |

**Defaulted by me. Tell me if you want otherwise:**

- **History:** plain copy. Each commit message cites `ee-eudiw@c8a42d1`, and the source
  repo keeps the history. `git subtree` or `filter-repo` would bring in about 130 commits
  whose history is unrelated to this repo, including the `eudi-arf`/`ui-mock` churn.
- **Layout:** mirror ee-eudiw's top-level paths. None of the seven directories exists
  here. Keeping them at the top level also means `zkverify-ffi`'s
  `../../../longfellow-zk` path dependency still resolves: `~/Documents/longfellow-zk` is
  already checked out at the pinned `61a8a73`. It also keeps the `shellcheck tests/*.sh`
  glob working unchanged.
- **Repo meta** (`AGENTS.md`, `CONTRIBUTING.md`, `_bmad/`, `skills-lock.json`,
  `.directory`): not copied. The parts of `AGENTS.md` worth keeping move into
  `docs/DEVELOPMENT.md` in step 4.

## Findings that shape the steps

1. **The fixture generators are on `master`.** `verifier/go/zk/testdata/*/GENERATED-BY`
   points at `:zk-conformance` tests: `Step0CrossVerifyTest`, `Step2aDeviceResponseFixtureTest`,
   `ZkConformanceSupport` and `Step87OpenID4VPFixtureTest`, all on `master` since fork PRs #3,
   #4 and #12 merged.
2. **`docs/planning/STEP6-RECORD.md` exists in both repos.** The wallet branch has the
   wallet-side step 6 record and ee-eudiw has the verifier-side one. The ee-eudiw file
   becomes `STEP6-VERIFIER-RECORD.md`, which matches the existing `STEP8-7-WALLET-RECORD.md`
   naming.
3. **The spec's cross-references were written for this combination.** The spec and code
   link to `docs/CONFORMITY.md`, which already exists on the wallet branch, and to
   `eudi-arf/docs/discussion-topics/README.md`, which resolves once the submodule is added.
4. **Links that point outside the chosen doc set:** `AGENTS.md` (2 links),
   `docs/planning/ANDROID-DEMO-IMPLEMENTATION-PLAN.md` (1),
   `docs/analysis/CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md` (2) and
   `docs/analysis/OPEN-EID-POC-CRITIQUE.md` (1). Both analyses are about this wallet, so
   they come across. The `AGENTS.md` links get repointed to `docs/DEVELOPMENT.md`. The
   ui-mock plan gets a pinned `github.com/tomkabel/ee-eudiw/blob/c8a42d1/...` URL.
5. **The Go module path is `github.com/tomkabel/ee-eudiw/verifier/go`**, used in 17 import
   lines. It gets renamed to `github.com/tomkabel/eudi-wallet-poc/verifier/go` so the path
   doesn't point at another repo.
6. **Existing drift, carried over rather than fixed silently:** `go.mod` says `go 1.26`
   but `DEVELOPMENT.md` says `go 1.24`. Also, CI never runs the root package tests of
   `verifier/go` (`iso_test.go`, `step8_7_openid4vp_zk_test.go`), because the jobs run only
   `./oid4vp/...` and `./zk/...`. Both are fixed in step 4, each as its own commit.

## Steps

One commit per step, on `ee-eudiw-zk-import`, pushed to `fork`.

### 0. Integration base

Nothing to merge: the branch sits on `master`, which already carries the wallet and fixture work.

Check: `./gradlew :zk-conformance:test` passes (AgeProofRoundTrip, Step0CrossVerify,
Step2aDeviceResponseFixture, Step87OpenID4VPFixture), with none skipped.

### 1. Spec + ARF submodule

- `cp ~/Documents/ee-eudiw/spec/EE-EUDIW-TS-1.0.md spec/`, then `cmp` it against the
  source.
- `git submodule add git@github.com:eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework.git eudi-arf`,
  then `git -C eudi-arf checkout 6373eee`. The commit message names the revision.

Check: the Annex E `grep` recipe reproduces the 105 of 187 figure, and the requirement
count stays at 187.

### 2. ZK code

- `git -C ~/Documents/ee-eudiw archive c8a42d1 zk-age-poc issuer wallet verifier tests demo | tar -x -C ~/Documents/eudi-wallet-poc`.
  `archive` takes tracked files only, so build output can't leak in.
- `.gitignore` gets ee-eudiw's entries that aren't here yet: `target/`, `*.proof`,
  `verifier/zkverify`, `out/`, `demo-data/`, `__pycache__/`, `*.pyc`, `.ruff_cache/`,
  `.pytest_cache/`.
- Rename the Go module path (finding 5) in `go.mod` and the 17 imports.
- Fix the `GENERATED-BY` provenance lines: `(cd eudi-wallet-poc && ./gradlew …)` becomes
  `./gradlew …` from the repo root. The "consumed by: ee-eudiw verifier/…" wording
  becomes `verifier/…`. The fixture bytes stay untouched.

Check, from the repo root:

```bash
(cd verifier/go && test -z "$(gofmt -l .)" && go vet ./... && go test ./oid4vp/...)
python3 -m compileall -q issuer verifier wallet tests
ruff check --isolated --select F,E9 --exclude eudi-arf .
python3 wallet/test_transcript.py
shellcheck --severity=warning tests/*.sh
mkdir -p ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples
cp zk-age-poc/*.rs ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples/
(cd ../longfellow-zk/rust/applications/mdoc_zk/runtime && RUSTFLAGS="-C target-cpu=x86-64-v3" cargo build --release --examples --features testonly)
test -x ../longfellow-zk/rust/target/release/examples/ee_poa_demo
(cd verifier/zkverify-ffi && cargo build --release)
(cd verifier/go && go build -o ../zkverify . && go test ./...)
EE_PROVER=../longfellow-zk/rust/target/release/examples/ee_poa_demo tests/e2e.sh
```

Pass criteria: `e2e.sh` prints `valid: true`, and every negative control fails the way it
should: wrong nonce rejected, replay gets `410`, untrusted issuer rejected, unlisted
circuit refused. The copied fixtures still verify in `go test ./zk/...`.

### 3. ZK-relevant docs

| Source | Destination |
|---|---|
| `docs/ARCHITECTURE.md`, `docs/DEVELOPMENT.md` | same |
| `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`, `DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md` | same |
| `docs/decisions/ADR-002-STRICT-CBOR-SUBSET-DECODER.md` | same |
| `docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` | same |
| `docs/planning/STEP0-CROSS-VERIFY-RECORD.md`, `STEP1-FORK-RECORD.md`, `STEP2A-RECORD.md`, `STEP7-RECORD.md`, `STEP8-RECORD.md`, `STEP8-7-RECORD.md` | same |
| `docs/planning/STEP6-RECORD.md` | `docs/planning/STEP6-VERIFIER-RECORD.md` (finding 2) |
| `docs/analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md`, `ZKP08-ANALYSIS.md`, `OPEN-EID-POC-CRITIQUE.md`, `CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md` | same |

Left out: `ANDROID-DEMO-IMPLEMENTATION-PLAN.md` (ui-mock), the other `docs/analysis/*`
(procurement, thesis, publication and repo-structure critiques), `docs/marketing/`,
`docs/design/` (Bilt-me UI), `ADR-001-submodules.md` (it describes ee-eudiw's two-submodule
layout, which no longer holds here), and the gitignored third-party PDFs.

Link fixes (finding 4) and a new, short `docs/README.md` that indexes only what exists here.
The ee-eudiw index lists documents that were not copied.

Check: a one-off script resolves every relative `](…)` link in `spec/`, `docs/`, and the
copied READMEs against the working tree. There must be zero misses, and every remaining
`ee-eudiw` link must be a pinned `c8a42d1` URL.

### 4. CI and check conventions

- `.github/workflows/ci.yml` from ee-eudiw, changed as follows:
  - `path: ee-eudiw` / `working-directory: ee-eudiw` become `eudi-wallet-poc`, and every
    `hashFiles('ee-eudiw/…')` and `go-version-file` changes to match.
  - Only `eudi-arf` is excluded in `ruff`.
  - `LONGFELLOW_REV` stays `61a8a73…`.
- Separate commit: add a `go test .` step for the root package (finding 6) in the `e2e`
  job, because the root package links the staticlib.
- Separate commit: reconcile `DEVELOPMENT.md`'s Go version with `go.mod`.
- `docs/DEVELOPMENT.md` gets `AGENTS.md`'s "Running the checks locally", "Evidence
  discipline" and "Generated files and pitfalls" sections, adapted to this repo. The
  submodules table shrinks to `eudi-arf`.

Check: push the branch and both jobs pass on GitHub Actions. Until then, the step 2 block
passing locally is the stand-in.

### 5. README + graph

- Add a README section "EE-EUDIW specification and ZK age-proof spine" with a pointer to
  `spec/`, `docs/DEVELOPMENT.md` and the measured numbers (prove ~360 KB in 6.1–6.5 s,
  verify 2.5 s), quoted with their source (`demo/README.md`). State the independent-draft
  status line verbatim.
- `graphify update .`

## Done when

- `./gradlew :zk-conformance:test`, the step 2 block, the step 3 link check and both CI
  jobs are green.
- `cmp` confirms the spec matches the source byte for byte.
- `git diff --stat edafc46` shows the source set plus the edits listed here and
  nothing else.

## Out of scope

Merging `ee-eudiw-zk-import` into `master` or opening a PR against `open-eid` is a separate
decision, and so is removing the copied parts from ee-eudiw or turning ee-eudiw into a
spec-only repo.
