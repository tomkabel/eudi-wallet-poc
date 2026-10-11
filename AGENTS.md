<!-- bmad:context -->
<!-- Verified 2026-10-11 against 0579cc2. Managed by bmad-project-context; edits inside this block are replaced on refresh. Keep anything you want preserved outside the markers. -->
## eudi-wallet-poc

Independent fork of open-eid's EE Wallet PoC: an Android wallet (Kotlin, Gradle, `app/`) plus an imported ZK age-proof spine — Python issuer and holder, a Go verifier over a Rust Longfellow staticlib — and the EE-EUDIW specification (`spec/`). Plans and step records live in `docs/planning/`; every doc is indexed in `docs/README.md`.

## Policy

- Push only to the `fork` remote; `origin` is upstream open-eid. Never push to `origin`.
- `fork` is public. Copying anything from the private ee-eudiw repo into this one publishes it — get the maintainer's confirmation first; some ee-eudiw documents are held back on purpose. Link in-repo files, not ee-eudiw URLs.
- Check version numbers, dates, identifiers and legal citations against primary sources; mark what you cannot verify `[UNVERIFIED]` (collected in spec §23) — `docs/DEVELOPMENT.md` "Evidence discipline".
- Never commit anything from `demo-data/`: it holds the holder's device private key.

## Where things are

- ZK spine build, run and troubleshooting: `docs/DEVELOPMENT.md`; every test and check: `tests/README.md`.
- Wallet vs OpenID4VP 1.0 / HAIP 1.0, requirement by requirement: `docs/CONFORMITY.md`.

## Running and verifying

- `.github/workflows/ci.yml` (ZK spine) and `app.yml` (Android) define green. `make check` is CI's fast job; `make test-go-zk` and `make e2e` cover the e2e job.
- Install Python deps with `pip install -r tests/requirements-ci.txt`, not unpinned `cbor2`/`cryptography`: the transcript vectors are golden and an encoder change shifts their bytes.
- Go packages `.` and `./zk/...` link the Rust staticlib and fail at the linker until `make deps` has run. `make deps` places `google/longfellow-zk` as the sibling `../longfellow-zk`; keep that directory name.
- Build the prover through `make deps`, or with `RUSTFLAGS="-C target-cpu=x86-64-v3"` — Longfellow's `target-cpu=native` produces SIGILL binaries on virtualised hosts.
- `./gradlew :zk-conformance:test` writes to `zk-conformance/build/`; only `-Dzk.regenerate=true` rewrites committed fixtures, after which rerun the Go `./zk/...` tests to cross-verify.
- Android unit tests: `./gradlew :app:testLocal_mocksUnitTest` (what CI runs), JDK 17.

## Known pitfalls

- On a phone, install `:app:installLocal_mocks`, not `installDebug`: `debug` targets the TEST environment's placeholder `*.example.com` backends and onboarding fails after PIN entry. App slf4j logs don't reach logcat — diagnose by driving the UI.
- `cargo build --examples` exits 0 with no binary when `examples/` is missing or empty; always `test -x` the prover afterwards.
<!-- /bmad:context -->
