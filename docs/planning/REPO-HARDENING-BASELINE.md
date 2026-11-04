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

Taken 2026-11-04 through the tiltrank rubric plus a user ask. Each answer was the user's own pick; the ranking log is `docs/planning/.decisions.log` (gitignored).

| # | Decision | Pick | Notes |
|---|---|---|---|
| 1 | Mock `.p12` private keys | Remove from HEAD only; no history rewrite | Both are `test_only` in `trust/MANIFEST.json`; the fork has 0 stars and 0 forks. Treated as public test material. |
| 2 | Signerry artefacts | gh-pages stays the registry; drop the binaries from main | gh-pages already hosts 152 signerry paths and `settings.gradle.kts` resolves from it without credentials. GitHub Packages excluded: Maven downloads need a token. |
| 3 | C10 rename | After this branch lands | |
| 4 | `_bmad-output/` | Retire to a read-only archive branch | Ranking was contested. Probe: no CI, Makefile, test or tool reads it; BMAD config, `AGENTS.md` and `docs/README.md` do. |
| 5 | Dependency updater | **Renovate** (user overrode the recommendation, Dependabot with `cooldown`) | Dependabot has a native `cooldown` option, so the plan's stated reason for Renovate no longer holds; the user chose Renovate anyway. Needs the Renovate app installed on the repo by an admin. |
| 6 | Repo settings | Private vulnerability reporting and CODEOWNERS now; branch protection and required checks after CI is green | |

## Phase 2 results (2026-11-04)

**C2 re-scoped: nothing to dedupe.** `.agents/skills` is a tracked symlink to `../.claude/skills` (the 1 vs 238 count in finding 4 is one symlink against 238 files under 34 skill directories), so there is a single skill tree and no mirror script or CI drift check was added. `.codex/hooks.json` (bmad-loop relay hooks) and `.claude/CLAUDE.md` (the `/graphify` trigger) do not repeat `AGENTS.md`; both stay.

**C3: root state files.** Writers identified from file content, timestamps and tool help:

| Entries | Writer | Action |
|---|---|---|
| `antibodies.json`, `auto_ingest_state.json`, `boot_*.json`, `calibration_state.json`, `checkpoint-working-set-v1.json`, `daemon_*.json`, `document_*.json`, `graph_snapshot.json`, `ingest_roots.json`, `plasticity_state.json`, `temporal_state_v1.json`, `tremor_state.json`, `trust_state.json`, `checkpoint-store/`, `.m1nd-checkpoint-namespace-*.lease/` | `m1nd` (`m1nd-mcp`, one run on 2026-10-23; not running now, not configured for this repo in any MCP config). It exposes `--runtime-dir` and `--registry-dir` flags but no env var or settings file, and nothing launches it with them here. | Moved (20 entries) to `$XDG_STATE_HOME/eudi-wallet-poc/m1nd/` (`~/.local/state/eudi-wallet-poc/m1nd/`). |
| `brag-output/` (2 MB video, plan, share copy) | `brag-slim` skill run on 2026-10-04 | Moved to `~/.local/state/eudi-wallet-poc/brag-output/`. |
| `.benchmarks/` | `pytest-benchmark`; empty directory | Removed (empty). It returns when pytest runs. |
| `.decisions.log` | `tiltrank` skill, by design in the working directory; it rejects a log path outside the working directory or `$TILTRANK_LOG_BASE` | Kept: it is the user's decision record (39 KB). Setting `TILTRANK_LOG_BASE` would be user-level and affect every project. |

`.gitignore` lines are kept: m1nd and tiltrank write to the working directory with no per-repo setting, so they will reappear. `ls -A` at the root now shows only tracked entries plus tool directories that were already ignored (`.bmad-loop`, `.code-review-graph`, `.gradle`, `.hermes`, `.kotlin`, `.playwright-mcp`, `.pytest_cache`, `.ruff_cache`, `.venv`, `build`, `graphify-out`) and `.decisions.log`. Those are outside the plan's list.

**C4: `_bmad-output/` retired.** Branch `archive/bmad-output-2026-11-04` (pushed to `fork`) holds the full tree: 123 files under `_bmad-output/`. The working tree removed 121 and kept `planning-artifacts/epics.md` and `implementation-artifacts/sprint-status.yaml`. `docs/README.md` (14 rows replaced by one archive row), `AGENTS.md`, and the 11 in-file path mentions in `epics.md` and `sprint-status.yaml` now point to `archive/bmad-output-2026-11-04:<path>`. `_bmad/*.yaml` output folders still resolve (both kept directories exist). Pending user confirmation: GitHub Issues and a Project for the backlog (public, outward-facing; not created).

Not rewritten, by design: 170 mentions of removed paths in `docs/profiles/av/follow-through-2026-10-23/provisional-agent-local-report.{json,html}` (a captured `git status` inside a frozen evidence report, hashed by `audit-record.json`) and 2 in `.bmad-loop/operator/*.json` (run records). Neither is a link.

**T4.** Root tracked files: 21 before, 21 after (`git ls-files | grep -vc /`; `eudi-arf` is a submodule). Nothing at the root was misplaced and unreferenced; `SECURITY_ANALYSIS_REPORT.md` is left for Phase 7.

**Judges.** `make check` exit 0; `make publish-guard` exit 0; `gitleaks detect` no leaks (247 commits); `lychee --offline` over `docs`, `AGENTS.md`, `README.md`, `spec`, `_bmad-output`: the only errors are `README.md` root-relative links and one link to the absent `docs/architecture-and-technical-specifications.md` in `docs/profiles/AV-PROFILE-CLAUSE-REGISTRY.md`, both older than this phase.
