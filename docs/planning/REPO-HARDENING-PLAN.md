# Repository hardening plan

Status: proposed, 2026-11-04. Nothing in this plan has been executed. Every phase opens with its evidence and closes on an outside judge: a command, a CI job or a measurement, never a self-assessment.

Goal beyond the goal: a clean clone of this fork can be built, tested, scanned and released by anyone, and the repository itself keeps those properties without relying on memory or on a human noticing a problem.

Scope: the thirteen findings from the repository critique of 2026-11-04. Each finding carries an ID (`C1`…`C13`) so later phases, commits and reviews can refer to it.

| ID | Finding (short) |
|---|---|
| C1 | Private keys and keystores tracked in git, on a public fork |
| C2 | Agent tooling duplicated and sprawling (skill trees, tool dirs) |
| C3 | Runtime and daemon state written into the repo root |
| C4 | Three planning and backlog systems in one tree |
| C5 | Build depends on a hidden sibling directory and prose folklore |
| C6 | Binary jars and AARs committed under `.github/` |
| C7 | Lint and type gates thin for Python, Kotlin and Rust |
| C8 | Python dependencies pinned but not locked |
| C9 | Android test coverage thin; `debug` build variant is a footgun |
| C10 | Naming overlaps ("wallet" twice, ZK code in three places) |
| C11 | Governance files missing (security policy, owners, release, SBOM, signing) |
| C12 | Documentation sprawl and point-in-time reports at the root |
| C13 | Container base image unpinned |

## Baseline (measured before any change)

These numbers are the judge for the hard targets. Phase 0 records them in `docs/planning/REPO-HARDENING-BASELINE.md`.

- Tracked files: 1325. Tracked private-key material: at least `verifier/go/zk/testdata/step0-rust-prover/device_key.pem` (PKCS#8 P-256) and the `.p12` files under `app/src/main/assets/` and `app/src/local_mocks/assets/keys/`.
- Duplicate skill sets: `.agents/skills` and `.claude/skills` hold the same skill set (`.claude` has one extra, `graphify`).
- Root-level tracked files: 17. Root-level untracked-but-present state files: at least 8, all listed in `.gitignore`.
- Committed binary artefacts: 40+ `.jar` and `.aar` files under `.github/maven-mirror/`.
- Kotlin: 190 main files, 31 test files under `app/src/test`.
- Ruff rule set in CI: `F` and `E9` only. No `pyproject.toml`, no `ruff.toml`, no pyright or mypy config.
- Makefile `lint`: gofmt and `go vet` only.
- Container: `sandbox/deploy/Dockerfile` uses `FROM debian:trixie-slim` with no digest.
- Not yet measured: OpenSSF Scorecard score, gitleaks full-history result, clean-clone build time. Phase 0 measures all three.

## Hard targets

Each target has a number and a command that proves it. A missed target with real numbers is a valid result; a changed definition is not.

- T1: `gitleaks detect --log-opts=--all` reports zero findings outside an explicit, reviewed allowlist.
- T2: zero external references in CI that are not pinned by commit SHA or by digest (checked by `zizmor` and a grep gate for `FROM` lines).
- T3: from a fresh clone, one command (`make bootstrap`) inside the devcontainer, followed by `make check`, finishes green. Target: under 15 minutes on a GitHub-hosted runner. Measured in Phase 3.
- T4: `git ls-files | wc -l` at the repo root equals the number of intended top-level entries, and the root contains no runtime state.
- T5: OpenSSF Scorecard reaches at least 8/10 after Phase 6. Baseline measured in Phase 0.
- T6: Android onboarding (install to PIN to first credential) passes as an automated flow on an emulator in CI on every PR that touches `app/`.
- T7: every release artefact is signed, ships an SBOM, and verifies with `cosign verify-blob` in a CI job that starts from the published artefact alone.

## Crew

The phases use six perspectives. Each one has a method and tool; none of them is a vocabulary swap.

- Supply-chain and secrets auditor: gitleaks, trufflehog (verified-only mode), zizmor, OpenSSF Scorecard, `git filter-repo` for history work.
- Release engineer: release-please, cosign (keyless), slsa-github-generator, CycloneDX (`cyclonedx-gradle`, `cargo-cyclonedx`, `cyclonedx-py`).
- Build and developer-experience engineer: devcontainer or Nix flake, mise for pinned tools, `uv` for Python, `dorny/paths-filter` for path-scoped CI.
- Android QA engineer: Gradle Managed Devices, Maestro flows for onboarding, Roborazzi for screenshot baselines.
- Technical writer and documentation librarian: lychee (links), Vale (prose), MADR 4.0 for ADRs, a generated conformance matrix from `docs/profiles/av/clause-registry.json`.
- Independent red reviewer: a fresh agent or human with no hand in the phase, given the diff and the judge output only, not the author's conclusion.

## Phases

Order follows risk: contain first, then clean, then make reproducible, then harden the supply chain, then add gates, then govern and release. Each phase lists its findings, its work, its judge, and its exit gate.

### Phase 0: Measure and decide (no code change)

Findings: all. Work:
- Record the baseline above in `docs/planning/REPO-HARDENING-BASELINE.md`: gitleaks full-history output (counts only, no secret values), Scorecard score, clean-clone build time on a fresh container.
- Confirm what the committed `.p12` keystores contain (certificates only, or private keys) using `openssl pkcs12 -info -nokeys` and a count of key bags, without printing key material.
- Confirm which tests consume `device_key.pem`: `verifier/go/zk/step0_cross_verify_test.go` and `zk-conformance/build.gradle.kts` reference the fixture.
- Record user decisions (see "Decisions needed").

Judge: the baseline file exists and each number in it is reproducible from a named command.
Exit gate: decisions recorded; baseline committed.

### Phase 1: Contain secrets (C1)

Work:
- Add gitleaks as a pre-commit hook (via prek or pre-commit) and as a blocking CI job. Use a `.gitleaks.toml` with path allowlists that name each allowed fixture and its reason.
- Replace the tracked `device_key.pem` with a generator: a Go test helper that mints a P-256 key and the mdoc it binds at test time, using a fixed seed so the golden vectors stay stable. If a golden vector depends on the exact bytes, pin the vector to the generator's output and regenerate once with `-Dzk.regenerate=true`-style gating (the existing project rule), then cross-verify with the Go `./zk/...` tests.
- Replace the tracked mock `.p12` files with a `make keys` target that generates them with `keytool` into a gitignored directory. Keep only public certificates and PEM trust lists in git.
- If Phase 0 shows that `lotl-keystore.p12` holds private material, move the LOTL trust anchor to a PEM trust list and fetch the live LOTL with a pinned checksum, as already done for the list's signature.
- Add a publish guard: a CI job that fails when a path under `demo-data/`, a `*.p12`, a `*.jks` other than `app/debug.keystore`, or a PEM with a `PRIVATE KEY` header is tracked outside the allowlist.

Judge: T1 passes; the publish guard fails on a planted test key in a throwaway branch and passes on the real tree; the Go and Gradle test suites still pass.
Exit gate: T1 green. History purge (below) is decided and recorded.

Risk: removing files from HEAD does not remove them from history. A history rewrite on a public fork is destructive and outward-facing; it requires explicit confirmation before it runs (see Decisions needed). Rotation is not needed for the step0 device key, which its README declares as test material, but the decision to treat it as a public secret must be recorded.

### Phase 2: Clean the working tree (C2, C3, C4)

Work:
- C2: make `.agents/skills` the canonical skill tree. Replace `.claude/skills` with a generated mirror (a script under `tools/` and a CI check that fails on drift), or drop the copy and load skills from the plugin marketplace. Keep one instruction source: `AGENTS.md`, imported by `CLAUDE.md` as it already is.
- C2: remove the tracked copies of agent tooling state that duplicate each other: `.codex/hooks.json` and `.claude/CLAUDE.md` should be checked against `AGENTS.md`, and only the non-duplicated content kept.
- C3: move desktop-tool state (`antibodies.json`, `auto_ingest_state.json`, `boot_*.json`, `calibration_state.json`, `checkpoint-working-set-v1.json`, `daemon_*.json`, `.decisions.log`, `checkpoint-store/`, `.benchmarks/`, `brag-output/`) to `$XDG_STATE_HOME/<tool>/<repo-hash>` or to the tool's own default. Remove the matching `.gitignore` lines once nothing writes to the root.
- C4: stop tracking `_bmad-output/` generated artefacts. Move the open backlog to GitHub Issues and a Project, with the epic and story IDs as labels so `docs/README.md` links keep resolving. Keep `docs/decisions/` (ADRs) as the decision record.

Judge: T4 holds (`git ls-files` at root matches the intended list; `ls -la` shows no state files); `diff -r .agents/skills <mirror>` is empty in CI; the story IDs in Issues cover every ID in `epics.md`.
Exit gate: T4 green; no duplicate skill tree; backlog source of truth recorded in `docs/README.md`.

### Phase 3: Make the build reproducible (C5, C10)

Work:
- C5: pin longfellow-zk as a git submodule at the revision CI already asserts (the existing "assert the built longfellow matches the pinned revision" step). Replace `make deps` with `git submodule update --init` plus a checksum check on the built prover.
- C5: add `make doctor`, which checks the toolchain versions, the prover with `test -x`, `RUSTFLAGS`, and the Android SDK, and prints one line per failed check. Replace the prose pitfalls in `AGENTS.md` with `make doctor` checks and a single line each pointing to the check.
- C5: add `.cargo/config.toml` that sets `target-cpu=x86-64-v3` for the prover build, so the SIGILL pitfall is no longer a README instruction.
- C5: add a devcontainer (`.devcontainer/devcontainer.json`) or a Nix flake, and pin tool versions with `mise.toml`. `make bootstrap` installs nothing outside the container.
- C10: path-scoped CI with `dorny/paths-filter`, so `app/` changes run Android jobs, `verifier/` changes run Go and Rust, and `issuer/` changes run Python.
- C10: rename by domain, not by ownership: `app/` stays (Gradle convention), `wallet/` is renamed to the role it actually plays (for example `holder-py/`), and ZK code is grouped under `verifier/` or a single `zk/` component, not split across `zk-age-poc/`, `verifier/go/zk/` and `verifier/zkverify-ffi/`. Use `git mv` so history follows the files.
- C10 is the largest churn in the plan. Do it in its own PR with no functional change, after the build works from the devcontainer, and only after the decision below.

Judge: T3 measured; `make doctor` fails when the prover binary is removed and passes when it is present; a clean clone in the devcontainer runs `make check` green.
Exit gate: T3 green with the measured time recorded.

### Phase 4: Harden the supply chain (C6, C8)

Work:
- C6: remove the `.github/maven-mirror/` binaries from HEAD. Consume the artefacts from Maven Central, GitHub Packages, or a caching proxy (Cloudsmith or Artifactory). If the signerry fork artefacts exist only in the mirror, publish them to a registry the project controls, with its checksums, before removing the copies. `mirror.yml` then becomes a publish job for the proxy, or is retired.
- C6: add Gradle dependency verification: `gradle/verification-metadata.xml` generated with `./gradlew --write-verification-metadata sha256` and reviewed. CI runs with verification on.
- C8: move Python to `uv`: `pyproject.toml` with `uv.lock`, CI runs `uv sync --locked --require-hashes`. Retire `tests/requirements-ci.txt` once the lock reproduces the golden transcript bytes. The `AGENTS.md` warning about unpinned `cbor2` and `cryptography` becomes a lock-file check.
- Replace Dependabot with Renovate, using `minimumReleaseAge` (a cooldown window before adopting a new release) and grouped updates. Keep the four ecosystems Dependabot covers today.
- Run `zizmor` over `.github/workflows/` as a blocking job. Fix findings; do not suppress them without a recorded reason.
- Generate SBOMs in CI: `cyclonedx-gradle`, `cargo-cyclonedx`, `cyclonedx-py`.

Judge: T2 holds in zizmor output; `uv sync --locked --require-hashes` passes on a clean runner; Gradle verification passes; the Python golden vectors reproduce byte-for-byte.
Exit gate: no binary artefacts under `.github/`; lock checks green.

### Phase 5: Add quality gates and Android tests (C7, C9)

Work:
- C7 Python: ruff with a ratchet. Add `[tool.ruff]` in `pyproject.toml`, keep `F` and `E9` as the blocking set, and add the wider rule set one rule family at a time with a baseline file and `--statistics` in CI, so new violations fail and old ones are paid down. Add `ruff format --check` on new files only, then on the tree once the ratchet has reached it. Add pyright in basic mode on `issuer/` and `wallet/` first.
- C7 Kotlin: Spotless with ktlint, and detekt with a baseline. Block new violations.
- C7 Rust: `cargo clippy -- -D warnings` and `cargo fmt --check` for `verifier/zkverify-ffi/`.
- C7 Go: golangci-lint v2 with a config file, run from `make lint`. Extend the `lint` target to call every linter so `make lint` means one thing.
- C7: add `pre-commit`/`prek` hooks for the same set, and run `pre-commit run --all-files` in CI.
- C9 Android: make `localMocks` the default flavor for development and make `debug` fail the build when a `BuildConfig` host is a placeholder `*.example.com`. The failure message names the flavor to use.
- C9 Android tests: add a Maestro flow for install, onboarding, PIN entry and first credential, run on an emulator with Gradle Managed Devices or the `reactivecircus/android-emulator-runner` action on a KVM-enabled runner. Add Roborazzi screenshot baselines for the onboarding screens. Route app logs through an exporter (a structured log sink written to a file in the test artefact) so the diagnostic gap in `AGENTS.md` closes.
- C9: add tests for the security-sensitive packages that already have none (`security/` has a test for six of its fourteen files).

Judge: T6 passes on an emulator in CI; `make lint` runs every linter and fails on a planted violation in each language; ruff, detekt and clippy report zero new violations against their baselines.
Exit gate: T6 green; every gate fails on a planted violation.

### Phase 6: Governance, release and container (C11, C13)

Work:
- C11: add `SECURITY.md` pointing to GitHub private vulnerability reporting, with the response window stated. Enable private vulnerability reporting in repo settings (requires admin).
- C11: add `.github/CODEOWNERS` for each path component.
- C11: release-please for `CHANGELOG.md` and version tags. Each release runs cosign keyless signing and the SLSA generator, and attaches the SBOMs from Phase 4.
- C11: a publish boundary guard in CI (from Phase 1) extended to check that no file references the private repository's URLs or held-document list, which `AGENTS.md` now asks people to remember.
- C13: pin `debian:trixie-slim` by digest. Run Trivy or Docker Scout in CI. Consider a distroless or Chainguard base once the sandbox's runtime needs are listed.

Judge: T5 measured again (target 8/10); `cosign verify-blob` passes on a release artefact in a separate job; the image digest in `compose.yml` and the Dockerfile match.
Exit gate: T5 and T7 green.

### Phase 7: Documentation (C12)

Work:
- Archive point-in-time reports. Move `SECURITY_ANALYSIS_REPORT.md` to `docs/archive/` with its date in the filename, and link it from `docs/README.md` as historical. Add a single `docs/SECURITY.md`-style living position if one is still needed, or fold it into the new `SECURITY.md`.
- Keep the `docs/planning/STEP*` records as history with a header line saying they are historical. Git already holds their history; the index entry suffices.
- Generate `docs/CONFORMITY.md`'s matrix from `docs/profiles/av/clause-registry.json`, so the matrix cannot drift from the registry. Check it in CI.
- Convert `docs/profiles/av/annex-a-examples.md` (about 1 MB) into structured data (JSON or YAML) with a rendered view generated from it.
- Add lychee (links) and Vale (prose, a house style file) as CI jobs. Add the doc-drift check that already exists as a skill as a CI job, not an agent step.
- ADRs: migrate `docs/decisions/` to MADR 4.0 headers. Keep the decision text unchanged.

Judge: lychee and Vale green; the generated `CONFORMITY` matrix diff is empty on a clean run; `docs/README.md` links resolve.
Exit gate: no root-level point-in-time report; generated matrix committed.

### Phase 8: Sustain (all)

Work:
- Ratchets stay on: ruff, detekt and clippy baselines only shrink; Scorecard runs weekly; gitleaks runs on the full history weekly.
- A quarterly repeat of this plan's baseline, using the same commands, reported as numbers in `docs/planning/`.
- Add the baseline commands to `make audit` so the next review is one command.

Judge: the quarterly numbers move in the right direction, or the doc records why they did not.
Exit gate: `make audit` exists and runs.

## Dependencies between phases

- Phase 1 must finish before any history rewrite and before Phase 2 adds commits, so the rewrite does not have to be repeated.
- Phase 3 is required before Phase 5 (gates need pinned toolchains) and before Phase 4's Python lock (the lock runs in the devcontainer).
- Phase 4's SBOMs and signing are required by Phase 6's release job.
- Phase 7 can run in parallel with Phases 4 and 5 after Phase 2.
- Phase 3's C10 rename is the last structural change; nothing in Phases 4 to 7 should depend on old paths when it lands. Run it when no other branch is open against the renamed paths, or accept the rebase cost explicitly.

## Decisions needed (user-only)

These block the phases named. Nothing else in the plan waits on them.

1. History rewrite for the tracked key material and keystores (Phase 1). It rewrites a public fork's history and invalidates existing clones. Recommended: rewrite only if Phase 0 shows that the `.p12` files contain private keys; for the step0 test key, the declared test-material status makes removal from HEAD sufficient.
2. Where the signerry artefacts live after removal from git (Phase 4): a GitHub Packages registry under the fork's owner, or a paid proxy (Cloudsmith or Artifactory). The paid option spends money and needs approval.
3. Whether to do the monorepo rename (Phase 3, C10) now or after the current branch lands. Recommended: after.
4. Whether `_bmad-output/` is retired from git entirely or kept as a read-only archive branch (Phase 2, C4).
5. Renovate replacing Dependabot (Phase 4): recommended yes; needs installing the Renovate app on the repo.
6. GitHub repo settings that need an admin: private vulnerability reporting, branch protection, required status checks, CODEOWNERS handles.

## What counts as done

The plan is done when T1 to T7 are each green in CI with the measured value recorded in `docs/planning/REPO-HARDENING-BASELINE.md` next to the start value, and the independent red reviewer has read the final diff against this document and recorded any mismatch. A target that is missed stays in the file as missed, with its number.
