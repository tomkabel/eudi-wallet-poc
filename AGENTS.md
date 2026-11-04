<!-- bmad:context -->
<!-- Verified 2026-10-23 against 2d38feb. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->
## eudi-wallet-poc

Independent fork of open-eid's EE Wallet PoC: an Android wallet (Kotlin, Gradle, `app/`) plus an imported ZK age-proof spine — Python issuer and holder, a Go verifier over a Rust Longfellow staticlib — and the EE-EUDIW specification (`spec/`). Plans and step records live in `docs/planning/`; every doc is indexed in `docs/README.md`.

## Policy

- Push only to the `fork` remote; `origin` is upstream open-eid. Never push to `origin`.
- `fork` is public. Copying anything from the private ee-eudiw repo into this one publishes it — get the maintainer's confirmation first; some ee-eudiw documents are held back on purpose. Link in-repo files, not ee-eudiw URLs.
- Check version numbers, dates, identifiers and legal citations against primary sources; mark what you cannot verify `[UNVERIFIED]` (collected in spec §23) — `docs/DEVELOPMENT.md` "Evidence discipline".
- Never commit anything from `demo-data/`: it holds the holder's device private key.

## Where things are

- ZK spine build, run and troubleshooting: `docs/DEVELOPMENT.md`; every test and check: `tests/README.md`.
- The sprint backlog lives in `_bmad-output/implementation-artifacts/sprint-status.yaml`; the epics and stories behind it are in `_bmad-output/planning-artifacts/epics.md`, sourced from the research corpus in `docs/research/`. Read the story before implementing it. The retired BMAD artefacts (PRD, story drafts, specs, retrospectives) live on the read-only branch `archive/bmad-output-2026-11-04` of `fork`.
- Wallet vs OpenID4VP 1.0 / HAIP 1.0, requirement by requirement: `docs/CONFORMITY.md`.

## Running and verifying

- `.github/workflows/ci.yml` (ZK spine) and `app.yml` (Android) define green. `make check` is CI's fast job; `make test-go-zk` and `make e2e` cover the e2e job.
- Install Python deps with `pip install -r tests/requirements-ci.txt`, not unpinned `cbor2`/`cryptography`: the transcript vectors are golden and an encoder change shifts their bytes. `make doctor` flags drift from the pins.
- Go packages `.` and `./zk/...` link the Rust staticlib and fail at the linker until `make deps` has run. `make deps` places `google/longfellow-zk` as the sibling `../longfellow-zk` at the revision in `verifier/zkverify-ffi/longfellow-rev.txt`; keep that directory name.
- `make doctor` is the first thing to run when a build misbehaves: it checks the toolchain versions (pinned in `mise.toml`), the prover with `test -x` and its recorded checksum, the sibling revision, the `target-cpu=x86-64-v3` pin (Longfellow's own `native` produces SIGILL binaries on virtualised hosts) and the Android SDK, one line per failure. `make bootstrap` sets it all up inside the devcontainer.
- `./gradlew :zk-conformance:test` writes to `zk-conformance/build/`; only `-Dzk.regenerate=true` rewrites committed fixtures, after which rerun the Go `./zk/...` tests to cross-verify.
- Android unit tests: `./gradlew :app:testLocal_mocksUnitTest` (what CI runs), JDK 17.

## Known pitfalls

- On a phone, install `:app:installLocal_mocks`, not `installDebug`: `debug` targets the TEST environment's placeholder `*.example.com` backends and onboarding fails after PIN entry. App slf4j logs don't reach logcat — diagnose by driving the UI.
<!-- /bmad:context -->
