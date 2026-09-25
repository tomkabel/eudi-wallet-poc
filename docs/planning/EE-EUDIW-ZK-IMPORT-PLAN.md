# Plan: import the EE-EUDIW spec and the ZK age-proof spine

Source: `~/Documents/ee-eudiw` at `e368fc1` (`main`, "Merge pull request #27 from
tomkabel/step8-7-mso-mdoc-zk"). Target: this repository, branch `ee-eudiw-zk-import`, based on
`master` at `edafc46`.

**Status, 25 September 2026: executed.** Steps 1–6 are on this branch. Deviations from the
text below:

- **Publication (answered: option 3, now narrowed).** The code, the spec and the ZK docs are
  published. Three documents are held back for now, not "the analyses and records": the
  conformance plan, `OPEN-EID-POC-CRITIQUE.md` and `CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md`. The
  step records are published. The comments in five
  Go files, one GENERATED-BY file and one Kotlin KDoc still name
  `docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md`, which resolves once that plan is
  cleared.
- **Step 5.** On freshly generated fixtures, `TestStep87HandleResponse` fails, because it
  requires a proof older than the timestamp window and a fresh one is accepted as valid.
  That is the expected behaviour on fresh fixtures, and one more reason to restore the
  committed bytes. The generators for step 2a and 8.7 now also write a `fork commit:` line
  from `git describe`. The committed files, generated before that, carry none, and none was
  added.
- **Step 1.** `eudi-arf` is fetched over https.

Revision 2 of this plan. Revision 1 pinned `c8a42d1`, a pre-rebase commit that no ref in ee-eudiw
contains (only the reflog, `HEAD@{343}`). `main` differs from it in 32 files of the import set.
The differences include the spec, a new `issuer/test_mint_device_key.py`, `zk/registry.go` →
`circuits/registry.go` and the step 2a `device_request.cbor` fixture. `main`'s spec is
byte-identical to the one `docs/CONFORMITY.md` states conformity against (`551fb65`). Every
figure below was re-measured at `e368fc1`.

## Gate before any push: publication

`tomkabel/ee-eudiw` is **private** and `fork` (`tomkabel/eudi-wallet-poc`) is **public**.
Pushing any step below publishes material that is private today. That includes the
specification and `OPEN-EID-POC-CRITIQUE.md`, a critique of `open-eid/eudi-wallet-poc` that
would sit on a public fork of that same repository. It also includes
`CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md`, which describes ee-eudiw as private.

The options put to you (you chose 3; see Status):

1. Publish: push to `fork` as each step lands.
2. Keep it private: add a private remote for this branch and push there.
3. Publish the code and the spec, and hold the analyses and records back.

The CI check in step 4 needs a pushed branch, so it waits on this answer too.

Whatever the answer, no imported file links to `github.com/tomkabel/ee-eudiw`. Outside readers
can't open a private repo, so such a link is dead for them. A reference to an ee-eudiw
document that isn't imported becomes plain text: "ee-eudiw `docs/…` at `e368fc1`".

## Scope

**Copied in full, no choice involved:**

| From ee-eudiw | Tracked files | What it is |
|---|---|---|
| `spec/EE-EUDIW-TS-1.0.md` | 1 | The whole specification, rev 1.4, 1562 lines, byte-for-byte |
| `zk-age-poc/` | 3 | `age_demo.rs`, `ee_poa_demo.rs` (the prover), `REPRODUCE.md` |
| `issuer/` | 3 | `mint_ee_poa.py` (EE-PoA mdoc minting), `test_mint_device_key.py`, README |
| `wallet/` | 3 | `present.py` (Python holder), `test_transcript.py`, README |
| `verifier/` | 76 | Go verifier (`oid4vp`, `zk`, `circuits`, `internal/cborsub`), `zkverify-ffi` Rust staticlib, `include/zkverify.h`, `circuits.json`, `verify_demo.py`, all `testdata/` fixtures, README |
| `tests/` | 3 | `e2e.sh`, `load_test.py`, README |
| `demo/` | 2 | `demo.sh`, README |

**Not copied:** build output (`verifier/zkverify` at ~19 MB, `zkverify-ffi/target/` at 149 MB,
`__pycache__/`, `.ruff_cache/`) and `demo-data/`, which holds a minted holder private key. ee-eudiw's
own `.gitignore` excludes all of them, and `git archive` never sees them.

**Decided with you (decision-picker, logged in `.decisions.log`):**

| Question | Your pick |
|---|---|
| Base branch | `master` at `edafc46`, which carries the wallet side and the fixture generators (fork PRs #1–#13) |
| Docs beyond the spec | ZK-relevant docs only (list in step 3) |
| Submodules | `eudi-arf` only, pinned at `6373eee`. `ui-mock-bilt-me` stays out |
| CI | Port the ZK `fast` and `e2e` jobs. No Gradle job |

**Defaulted by me. Tell me if you want otherwise:**

- **History:** plain copy. Each commit message cites `ee-eudiw@e368fc1`. `git subtree` or
  `filter-repo` would bring in 208 commits whose history is unrelated to this repo,
  including the `eudi-arf`/`ui-mock` churn.
- **Layout:** mirror ee-eudiw's top-level paths. None of the seven directories exists here.
  Keeping them at the top level means `zkverify-ffi`'s `../../../longfellow-zk` path dependency
  still resolves (`~/Documents/longfellow-zk` is at the pinned `61a8a73`), and the
  `shellcheck tests/*.sh` glob works unchanged.
- **Verbatim first, edits second:** steps 2 and 3 each land as two commits. The first is the
  untouched copy, and every later change is a readable diff against it.
- **Records and analyses stay as written.** They are dated evidence. Their
  `github.com/tomkabel/ee-eudiw/...` test-output lines and "in ee-eudiw" prose stay unchanged.
  Only live documents (READMEs, `ARCHITECTURE.md`, `DEVELOPMENT.md`, `CONFORMITY.md`,
  `MEASUREMENTS.md`) get edited.
- **Repo meta** (`AGENTS.md`, `CONTRIBUTING.md`, `_bmad/`, `skills-lock.json`, `.directory`):
  not copied. The parts of `AGENTS.md` worth keeping move into `docs/DEVELOPMENT.md` in step 4.

## Findings that shape the steps

1. **The fixture generators write into a sibling ee-eudiw checkout.**
   `zk-conformance/build.gradle.kts:23` defaults `step0.eeEudiw` to
   `rootDir.resolveSibling("ee-eudiw")`. The three fixture tests write there, and
   `Step0CrossVerifyTest` reads `step0-rust-prover/` from there. If nothing changes, then after
   the import:
   - `./gradlew :zk-conformance:test` checks against ee-eudiw's copy, not this repo's, and
     rewrites that other working tree.
   - A fresh clone of this repo skips every fixture test (`ZkConformanceSupport.kt:60`,
     `assumeTrue`).

   Step 5 points the generators at this repository. Until then, don't run `:zk-conformance:test`.
2. **Provenance comes from two places.** The GENERATED-BY text in three files is written by
   Kotlin:
   - `step0-multipaz/GENERATED-BY` (`Step0CrossVerifyTest.kt:120`)
   - `step2a-iso-annex-c/device_response_GENERATED-BY` (`Step2aDeviceResponseFixtureTest.kt:63`)
   - `step8-7-openid4vp-zk/GENERATED-BY` (`Step87OpenID4VPFixtureTest.kt:105`)

   `step2a-iso-annex-c/GENERATED-BY` belongs to the Go test `TestDeviceRequestBuild`, and
   `step6-device-pubkey/GENERATED-BY` to `mint_ee_poa.py` + `ee_poa_demo`. A fix that edits
   only the fixture files is undone the next time the fixtures are regenerated, so step 5
   changes the Kotlin strings as well. `step0-multipaz/GENERATED-BY` also carries a
   hand-added `fork commit:` line that the generator doesn't write. That line is kept.
3. **`docs/planning/STEP6-RECORD.md` exists in both repos.** Here it is the wallet-side record,
   and in ee-eudiw it is the verifier-side one. The ee-eudiw file becomes
   `STEP6-VERIFIER-RECORD.md`, which matches `STEP8-7-WALLET-RECORD.md`. Only prose refers to
   it: this repo's `STEP6-RECORD.md:107` and ee-eudiw's `STEP7-RECORD.md:108`. ee-eudiw's
   `docs/README.md` links to it but isn't copied.
4. **Links that leave the imported set.** Relative links: `AGENTS.md` from `docs/DEVELOPMENT.md`
   and `tests/README.md`, and `ANDROID-DEMO-IMPLEMENTATION-PLAN.md` from
   `EUDI-WALLET-POC-CONFORMANCE-PLAN.md:930`. The `AGENTS.md` links get repointed to
   `docs/DEVELOPMENT.md`, and the Android plan link becomes plain text (see the gate).
   `CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md` names three uncopied ee-eudiw paths in backticks. It is a
   dated analysis of ee-eudiw at `5e6628a`, so it stays as written.
5. **Live links from this repo into ee-eudiw.** `docs/CONFORMITY.md:4` links the spec at `551fb65`,
   and `docs/MEASUREMENTS.md:4` links the conformance plan at `blob/main`. Both become in-repo
   links. The spec is byte-identical to `551fb65`, so CONFORMITY's statement keeps its meaning,
   and it gains a note on where the file came from.
6. **The Go module path is `github.com/tomkabel/ee-eudiw/verifier/go`.** It is used in `go.mod`,
   25 import lines and `docs/ARCHITECTURE.md:95`. It gets renamed to
   `github.com/tomkabel/eudi-wallet-poc/verifier/go`.
7. **Already fixed upstream, so nothing to do here:** `DEVELOPMENT.md` and `go.mod` agree on
   `go 1.26`. CI covers the whole Go module: the fast job runs `./oid4vp/... ./internal/...
   ./circuits/...` and e2e runs `. ./zk/...`.

## Steps

One step, one commit, except steps 2 and 3, which are two commits each. Nothing is pushed until
the publication gate is answered.

### 1. Spec + ARF submodule

- `git -C ~/Documents/ee-eudiw show e368fc1:spec/EE-EUDIW-TS-1.0.md > spec/EE-EUDIW-TS-1.0.md`.
  The source is the pinned commit, not the working tree.
- `git submodule add https://github.com/eu-digital-identity-wallet/eudi-doc-architecture-and-reference-framework.git eudi-arf`,
  then `git -C eudi-arf checkout 6373eee`. Use https, not SSH: anonymous `clone --recursive`
  must work. The commit message names the revision.

Check: `cmp spec/EE-EUDIW-TS-1.0.md <(git -C ~/Documents/ee-eudiw show 551fb65:spec/EE-EUDIW-TS-1.0.md)`.
This also proves that the spec matches the revision `CONFORMITY.md` cites.
`git -C eudi-arf rev-parse --short HEAD` prints `6373eee`.

### 2. ZK code

**2a, verbatim:** `git -C ~/Documents/ee-eudiw archive e368fc1 zk-age-poc issuer wallet verifier tests demo | tar -x -C ~/Documents/eudi-wallet-poc`.
`archive` takes tracked files only, so build output and keys can't leak in.

**2b, edits:**
- `.gitignore` gets ee-eudiw's entries that aren't here yet: `target/`, `*.proof`,
  `verifier/zkverify`, `out/`, `demo-data/`, `__pycache__/`, `*.pyc`, `.ruff_cache/`,
  `.pytest_cache/`.
- Rename the Go module path (finding 6) in `go.mod` and the 25 imports.
- In the two ee-eudiw-owned GENERATED-BY files (`step2a-iso-annex-c/GENERATED-BY`,
  `step6-device-pubkey/GENERATED-BY`), only the wording changes. The three generator-owned files
  wait for step 5. Fixture bytes stay untouched.

Check, from the repo root. It mirrors ee-eudiw `main`'s `ci.yml`:

```bash
(cd verifier/go && test -z "$(gofmt -l .)" && go vet ./... && go test ./oid4vp/... ./internal/... ./circuits/...)
python3 -m compileall -q issuer verifier wallet tests
ruff check --isolated --select F,E9 --exclude eudi-arf .
python3 wallet/test_transcript.py
python3 issuer/test_mint_device_key.py
shellcheck --severity=warning tests/*.sh
mkdir -p ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples
cp zk-age-poc/*.rs ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples/
(cd ../longfellow-zk/rust/applications/mdoc_zk/runtime && RUSTFLAGS="-C target-cpu=x86-64-v3" cargo build --release --examples --features testonly)
test -x ../longfellow-zk/rust/target/release/examples/ee_poa_demo
(cd verifier/zkverify-ffi && cargo build --release)
(cd verifier/go && go build -o ../zkverify . && EE_BENCH_SKIP=1 go test . ./zk/...)
EE_PROVER="$PWD/../longfellow-zk/rust/target/release/examples/ee_poa_demo" tests/e2e.sh
```

Pass criteria: every command exits 0, and `e2e.sh` reports all five of its cases as passing:
1. happy path, `valid: true`
2. a proof against a different nonce, `valid: false`
3. a replayed `vp_token`, HTTP `410`
4. an under-18 holder proving `age_over_18 = false`, `valid: false` on the value
5. the prover refusing a false predicate by default

The committed fixtures verify in `go test . ./zk/...`. Also,
`git diff --stat <2a>..HEAD -- zk-age-poc issuer wallet verifier tests demo` lists only
`go.mod`, the 14 Go files that carry the 25 imports, and the two GENERATED-BY files.

### 3. ZK-relevant docs

**3a, verbatim**, from `git archive e368fc1 docs`:

| Source | Destination |
|---|---|
| `docs/ARCHITECTURE.md`, `docs/DEVELOPMENT.md` | same |
| `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`, `DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md` | same |
| `docs/decisions/ADR-002-STRICT-CBOR-SUBSET-DECODER.md` | same |
| `docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md` | same |
| `docs/planning/STEP0-CROSS-VERIFY-RECORD.md`, `STEP1-FORK-RECORD.md`, `STEP2A-RECORD.md`, `STEP7-RECORD.md`, `STEP8-RECORD.md`, `STEP8-7-RECORD.md` | same |
| `docs/planning/STEP6-RECORD.md` | `docs/planning/STEP6-VERIFIER-RECORD.md` (finding 3) |
| `docs/analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md`, `ZKP08-ANALYSIS.md`, `OPEN-EID-POC-CRITIQUE.md`, `CROSS-DEVICE-LOGIN-GAP-ANALYSIS.md` | same |

Left out:
- `ANDROID-DEMO-IMPLEMENTATION-PLAN.md` (ui-mock)
- the other eight `docs/analysis/*` files: `ARF-3.0.0-VERIFICATION-AUDIT`,
  `PUBLICATION-READINESS`, `REPORT`, `REPOSITORY-STRUCTURE-CRITIQUE`, `SPLITKEY-FIT-ANALYSIS`,
  `THESIS-ANALYSIS-Mults-2025`, `VERIFICATION-AND-URL-ANALYSIS` and `critique-triage`
- `docs/marketing/`, and `docs/design/` (Bilt-me UI)
- `ADR-001-submodules.md`, which describes ee-eudiw's two-submodule layout
- the gitignored third-party PDFs

**3b, edits:**
- Link fixes from findings 4 and 5, plus the module path in `ARCHITECTURE.md:95`.
- Prose pointers for finding 3: this repo's `STEP6-RECORD.md:107` names
  `STEP6-VERIFIER-RECORD.md`.
- A new, short `docs/README.md` that indexes only what exists here. ee-eudiw's index lists
  documents that weren't copied.

Check: a one-off script resolves every relative `](…)` link in `spec/`, `docs/` and the
imported READMEs against the working tree, with zero misses.
`grep -rn 'github.com/tomkabel/ee-eudiw' spec docs zk-age-poc issuer wallet verifier tests demo`
matches only the historical records and analyses that the "stay as written" default names.

### 4. CI and check conventions

- Port `.github/workflows/ci.yml` from ee-eudiw `e368fc1` with these changes:
  - `path: ee-eudiw` / `working-directory: ee-eudiw` become `eudi-wallet-poc`, and every
    `hashFiles('ee-eudiw/…')` and `go-version-file` changes to match.
  - The `ruff` exclude becomes `eudi-arf` only.
  - `on:` gets a `paths:` filter (`verifier/**`, `zk-age-poc/**`, `issuer/**`, `wallet/**`,
    `tests/**`, `demo/**`, `.github/workflows/ci.yml`). Without it, every Kotlin-only push
    pays for the e2e Rust build.
  - `LONGFELLOW_REV` stays `61a8a73…`.
- `docs/DEVELOPMENT.md` gets `AGENTS.md`'s "Evidence discipline", "Running the checks locally"
  and "Generated files and their pitfalls" sections, adapted to this repo. The submodules
  table shrinks to `eudi-arf`.

Check: once the gate allows a push, both jobs pass on GitHub Actions. Until then, the step 2
block passing locally stands in.

### 5. Point the fixture generators at this repository

- `zk-conformance/build.gradle.kts`: the fixture root defaults to `rootDir`, not the sibling
  `ee-eudiw`. The `-D` override stays for other layouts. Rename `step0.eeEudiw` to
  `zk.fixtureRoot` and `EeEudiw` to `FixtureRoot`, and drop `assumePresent()`'s skip: in this
  repo `verifier/go/zk` always exists, so a missing tree is a failure, not a skip.
- In the three generator-owned GENERATED-BY strings (finding 2), `(cd eudi-wallet-poc && ./gradlew …)`
  becomes `./gradlew …` from the repo root, and "consumed by: ee-eudiw verifier/…" becomes
  "consumed by: verifier/…". Apply the same text to the three committed files. KDoc that
  says "ee-eudiw's verifier" now says "the verifier in `verifier/go`".

Check:
1. `./gradlew :zk-conformance:test` passes AgeProofRoundTrip, Step0CrossVerify,
   Step2aDeviceResponseFixture and Step87OpenID4VPFixture, with none skipped, and
   `git -C ~/Documents/ee-eudiw status --short` is unchanged.
2. `(cd verifier/go && EE_BENCH_SKIP=1 go test . ./zk/...)` passes against the regenerated
   fixtures.
3. `git diff -- '*GENERATED-BY'` shows only the dropped `fork commit:` line in
   `step0-multipaz/GENERATED-BY`. Restore that line. Then
   `git restore verifier/go/zk/testdata` for the regenerated bytes: the committed fixtures are
   the ones the records measure (e.g. the 360,180-byte proof in STEP8-7 and the profile).

### 6. README + graph

- Add a README section "EE-EUDIW specification and ZK age-proof spine" with pointers to `spec/`
  and `docs/DEVELOPMENT.md`, and the measured numbers with their source (`demo/README.md`):
  prove 6.1–6.5 s, verify 2.5 s, proof ~360 KB (359,988 B). Quote ee-eudiw README's status
  line verbatim: "**Status: independent draft.** Not issued by RIA, …".
- Run `graphify update .`. It's local only: `graphify-out/` is untracked.

## Done when

- The checks for steps 1, 2, 3 and 5 are green locally, and both CI jobs are green once the
  gate allows a push.
- `git diff --stat edafc46..<2a> -- zk-age-poc issuer wallet verifier tests demo` has 90 added
  files, and `diff -r` against a fresh `git archive e368fc1` of those directories is empty
  at `<2a>`. The same holds for 3a against `docs/`.
- `git diff <2a>..HEAD` and `git diff <3a>..HEAD` show only the edits listed here.

## Out of scope

Merging `ee-eudiw-zk-import` into `master` or opening a PR against `open-eid` is a separate
decision, and so is removing the copied parts from ee-eudiw or turning ee-eudiw into a
spec-only repo.
