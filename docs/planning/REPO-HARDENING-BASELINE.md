# Repository hardening baseline

Measured 2026-11-04 on branch `ee-eudiw-zk-import` by Phase 0 of [REPO-HARDENING-PLAN.md](REPO-HARDENING-PLAN.md). Every number names the command that reproduces it. No secret values appear here.

## Start values

| Item | Value | Command |
|---|---|---|
| Tracked files | 1132 (plan text said 1325) | `git ls-files \| wc -l` |
| Root-level tracked files | 20 | `git ls-files \| grep -vc /` |
| Root-level ignored state files | 19 | `git status --ignored --short \| grep '^!!' \| grep -v / \| wc -l` |
| Files under `.github/maven-mirror/` | 123, of which 42 `.jar`/`.aar` | `git ls-files .github/maven-mirror \| wc -l` |
| Tracked files under `.agents/skills` / `.claude/skills` | 1 / 238 (not a full duplicate, contrary to the plan) | `git ls-files <dir> \| wc -l` |
| Kotlin main / test files | 189 / 30 | `git ls-files app/src/<set> \| grep -c '\.kt$'` |
| `security/` main / test files | 16 / 8 | same, filtered on `/security/` |
| Ruff rules in CI and `make lint` | `F`, `E9`, `--isolated`; no config file | `.github/workflows/ci.yml`, `Makefile` |
| `pyproject.toml`, `ruff.toml`, `pyrightconfig.json` | none | `ls` |
| `make lint` linters | gofmt, go vet, compileall, ruff F/E9, shellcheck | `Makefile` |
| Container base | `FROM debian:trixie-slim`, no digest | `sandbox/deploy/Dockerfile:10` |
| gitleaks, full history | 0 findings, 244 commits, 18.97 MB, 16.6 s | `gitleaks detect --log-opts=--all --redact=100 --no-banner` |
| OpenSSF Scorecard | not measured (CLI not installed; needs a network scan) | `scorecard --repo=github.com/tomkabel/eudi-wallet-poc` |
| Clean-clone build time | not measured (needs the Phase 3 devcontainer to be meaningful) | — |

## What Phase 0 found that changes the plan

1. **T1 already passes, and that is the problem.** `.gitleaks.toml` (from the PKI remediation plan) allowlists the private-key fixtures by path, so gitleaks reports zero. T1 as written ("zero findings outside an explicit, reviewed allowlist") is satisfied today and proves nothing about whether keys are tracked. Phase 1 must test the allowlist's contents, not the scan result: the judge becomes "the allowlist names no path that holds a private key".
2. **Keystore contents** (`openssl pkcs12 -info -nokeys`, bag counts only, password is the documented mock value):
   - `app/src/local_mocks/assets/keys/doc_signer_mdl_issuer.p12`: 1 certificate bag, **1 shrouded key bag** (private key).
   - `app/src/local_mocks/assets/keys/doc_signer_pid_issuer.p12`: 1 certificate bag, **1 shrouded key bag** (private key).
   - `app/src/main/assets/certs/lotl-keystore.p12`: 1 trusted certificate entry (`lotl-signer`), **no key bag**. The plan's Phase 1 step for the LOTL anchor ("if it holds private material") does not apply; it is public trust data.
3. `verifier/go/zk/testdata/step0-rust-prover/device_key.pem` is declared test material in its own README. `issuer/mint_ee_poa.py` also writes a `device_key.pem` under `demo-data/` (never tracked).
4. `.agents/skills` is not a duplicate of `.claude/skills` (1 vs 238 tracked files); C2's "make `.agents/skills` canonical" needs re-scoping before any move.
5. Tooling present on this machine: gitleaks, openssl, keytool, uv, ruff, pyright, pre-commit, lychee, docker. Missing: trufflehog, zizmor, prek, scorecard, cosign, vale.

## Decisions

Recorded only once the user answers; none have been taken. See "Decisions needed" in the plan.
