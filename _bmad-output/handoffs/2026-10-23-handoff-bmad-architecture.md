# Handoff — run `bmad-architecture` on eudi-wallet-poc

Paste everything below the rule into a **fresh context window** in
`/home/notroot/Documents/eudi-wallet-poc`. Written 2026-10-23, after the backlog was reconciled with
the PRD that landed mid-session.

Saved at `_bmad-output/handoffs/2026-10-23-handoff-bmad-architecture.md`.

---

## Task: produce the architecture record for this repository

You are running the **`bmad-architecture`** skill (BMad Method, plan phase, `required=true`,
`preceded-by bmad-prd`). Load it with `skill_view(name='bmad-architecture')` and follow it. Resolve its
customization first:

```
uv run _bmad/scripts/resolve_customization.py --skill .agents/skills/bmad-architecture \
  --project-root . --key workflow
uv run _bmad/scripts/resolve_config.py --project-root .
```

`planning_artifacts` = `_bmad-output/planning-artifacts/` (your output goes there),
`implementation_artifacts` = `_bmad-output/implementation-artifacts/`, `project_knowledge` = `docs/`.

### Why this is being run now

The backlog is content-final but its readiness gate reads **CONCERNS** for exactly one reason: no
architecture record exists. Three stories in it deliberately defer a decision to themselves —
**Story 3.1** (the `rp-kit/` deployment profile), **Story 4.1** (the registration-certificate data
model) and **Story 5.1** (the circuit-list document format) — and two more need an interface fixed
before implementation can start (**Stories 3.3/3.4** the register-lookup contract, **Story 4.1**'s
`IntendedUseSource`). Closing this gate turns each of those stories' first acceptance criterion from
"record the ADR" into "follow the ADR", and unblocks Epics 3, 4 and 5.

Story 1.1 (`1-1-av-profile-clause-registry`) does **not** need this record. It can be built now,
in parallel, and is the sprint tracker's own next recommendation.

### Read these first, in this order

1. **`_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md`** — `status: final`,
   117 KB. 48 FRs under 7 features (§4.1–§4.7), 17 NFRs (§8), 16 open questions (§18, two already
   resolved and recorded), 9 assumptions (§19). Its §0 names `bmad-architecture` as a consumer and says
   the identifiers are meant to be source-extracted. **It is the authority on scope.** Read in full,
   but especially: §5 Non-Goals, §6 MVP Scope, §9 Constraints and Guardrails (PG-1…PG-5 are the
   product's reason to exist and are not negotiable by a feature request), §10 Compliance, §12
   Integration and Dependencies, §13 Risk, §14 Operational, §15 Data Governance, §16 Platform.
2. **`_bmad-output/planning-artifacts/epics.md`** — the decomposition: 12 epics, 80 stories, 70 FRs,
   16 NFRs. Two sections are load-bearing for you:
   - **`### Additional Requirements`** — the invariants your record must either preserve or change
     explicitly. Each is stated as a prohibition with the alternative named.
   - **`Reconciliation with the PRD`** — explains that the PRD's `FR-n`/`NFR-n` and this document's
     `FR1`–`FR70`/`NFR1`–`NFR16` are deliberately distinct namespaces (PRD assumption A-6), and maps
     §4.x to epics. Choose one namespace for the architecture record and say which; do not renumber
     either document.
3. **`docs/ARCHITECTURE.md`** — the imported ee-eudiw architecture, derived from the tree on
   12 September 2026 with `file:line` citations. Treat it as a **source, not the record**: it describes
   that repository, its own `README.md`/`AGENTS.md` citations are ee-eudiw's, and the Android holder it
   places "in a separate fork" is `app/` here. Re-verify any citation you reuse.
4. **`spec/EE-EUDIW-TS-1.0.md`** — 211 requirements across 14 families; Annex E records 126 covered and
   85 uncovered. Architecture-relevant: `EE-ZKP-040` (two budgets in one requirement: device prover
   ≤ 2.0 s p95 / ≤ 3.5 s p99, verifier ≤ 1.0 s p95), `EE-ZKP-041` (≤ 250 MB), `EE-ZKP-044`
   (≤ 4.0 s p95 transport, precondition of proximity Tier 2 via `EE-CNF-006`), `EE-ZKP-045a`
   (a transport miss must not override `EE-ZKP-055`), `EE-ZKP-055a` (plain fallback refused only where
   the request is *not* certificate-authenticated), `EE-POA-030` (one predicate, with the
   registered-intended-use exception), `EE-PRO-004`/`EE-PRO-025` (`readerAuth` anchors),
   `EE-WUA-007`/`008`, `EE-ACC-001..004`, `EE-PRI-023`, and §22 for the recorded divergences.
5. `docs/CONFORMITY.md`, `docs/MEASUREMENTS.md`, `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`,
   `docs/DEVELOPMENT.md`, `tests/README.md`, `docs/sota-audit-ROLLUP.md`, `DESIGN.md`, `PRODUCT.md`.

### The system as it exists — verify each of these against the tree, do not take them on trust

**Components.** `app/` (Android/Kotlin, Gradle, namespace `ee.cyber.wallet`, applicationId
`ee.tomkabel.walletpoc`, compileSdk 35 / minSdk 31, JDK 17, modules `crypto data di domain security ui
util`); `verifier/go` (Go module `github.com/tomkabel/eudi-wallet-poc/verifier/go`, go 1.26, built
binary `verifier/zkverify`); `verifier/zkverify-ffi` (Rust staticlib over `../longfellow-zk`);
`verifier/include`; `issuer/` (Python: `mint_ee_poa.py`); `wallet/` (Python: `present.py`);
`sandbox/` (Python server + `static/`, drives mint → session → present for real, and says where it is a
sandbox); `zk-conformance/` (JVM Gradle module, `:zk-conformance:test`); `tests/` (e2e + vectors);
`spec/`; `statuslists/`; `eudi-arf/`; `demo/`; `scripts/`.

**Load-bearing invariants** (from the backlog's Additional Requirements — confirm each in code before
you write them down):
- `verifier/go/oid4vp/carrier.go` — the `Carrier` interface plus first-byte sniff dispatch — is the
  single adaptation point for ISO second-edition drift.
- `Flow` / `TranscriptForFlow` in `verifier/go/oid4vp` is the single transcript entry point; carriers
  declare their flow and the package is written so a carrier cannot mix what it hashes.
- The circuit allowlist keys on `id` + `circuit_hash` + params, and accepts both `longfellow-libzk-v1`
  and `org.iso.mdoc.zk` as system labels.
- `direct_post.jwt` is the verifier default (ADR-003); plain `direct_post` needs
  `-response-mode direct_post -allow-unencrypted-response`, and `-allow-false-predicate` is likewise a
  negative-control path. Neither may be described as a supported deployment mode.
- `app/build.gradle.kts` restricts ABIs to `arm64-v8a` and `x86_64`, because `multipaz-longfellow`
  ships `libzkp.so` for those two only — "installable" and "able to prove" are the same set.
- The four provider surfaces are **mocks today**: `PidProviderServiceMock`, `MdlProviderServiceMock`,
  `AgeVerificationProviderServiceMock` (and the wallet-unit attestation path) under
  `app/src/main/kotlin/ee/cyber/wallet/domain/provider/`. The issuer, the register and the status list
  are likewise mock-or-file-backed. Naming every mock→real seam is part of your job.
- Deployment constraints that already exist in code: `SIGTERM` drain with `-drain-timeout`, `503` +
  `Retry-After` on `/present/new`, `draining: true` on `/healthz` (`tests/drain_test.sh`), and
  `max-concurrent-verify = NumCPU` with one verify ≈ 2.7 core-seconds.

### Decisions the architecture record must make

1. **`rp-kit/` deployment profile** (Story 3.1) — what the relying-party service actually is: process
   model, config surface, key management, the `direct_post.jwt` default, the drain behaviour, and how
   TARA sits alongside it. Story 3.2 records that the wallet-facing path is a **parallel** path, not
   inside TARA, and that X-Road has no documented relationship to these protocols.
2. **Registration-certificate data model** (Story 4.1) — `IntendedUseSource` interface first, then the
   typed `RegisteredIntendedUse` of Story 4.2 (trade name, unique identifier, service identifier,
   Member State, intended use, privacy-policy URL), the fail-closed default, and the bounded-cache and
   logged-stale-data decisions. The certificate profile is still moving; say what version you target
   and what an unsupported version does.
3. **Circuit-list document format** (Story 5.1) — fields, signing key and pinning, `issued_at`/
   `expires_at`, the minimum-library-version field, the local pinned fallback, and the audit query
   (Story 5.5). The AV Profile defers the accepted set to an unnamed "scheme owner" (PRD OQ-2): decide
   what the fork does while that is unanswered.
4. **Register-lookup contract** (Stories 3.3/3.4) — the pluggable interface, refusal semantics, cache
   age, what happens when the register is unreachable versus absent, and what the fork publishes as its
   interface proposal (design D3).
5. **Two-carrier placement** (PRD OQ-6) — the AV Profile mandates ZK over the DC API path while this
   fork profiles the OpenID4VP `mso_mdoc_zk` carrier as primary. The PRD recommends keeping both and
   stating in every artefact which path is mandated and which is profiled-but-unofficial. Turn that
   into an architectural rule with the enforcement point named.
6. **Mock→real seams** — issuer, register, WUA, status list, trust anchors. For each: what exists, what
   replaces it, what enforces the boundary meanwhile, and what must not be presented as real.
7. If it is architectural, take a position on **PRD OQ-11** (does the proximity path keep advertising
   itself where an age request is refused?) and **PRD OQ-12** (who hosts the sandbox). If they are
   product decisions rather than architecture, say so and leave them.

### Constraints you must not break

- **Policy.** Push only to the `fork` remote; `origin` is upstream open-eid — never push there. `fork`
  is public, so copying anything from the private `ee-eudiw` repository publishes it. Never commit
  anything from `demo-data/`. Any version number, date, identifier or legal citation needs a primary
  source, with the rest marked `[UNVERIFIED]` (collected in spec §23).
- **NFR-1 (PRD NFR-1, backlog NFR1):** no artefact may state or imply compliance, conformity or
  certification. Assessment is conferred by a body.
- **The regression gate** every code story in the backlog inherits: `make check` exits 0;
  `./gradlew :app:testLocal_mocksUnitTest` stays at 169 `@Test` across 25 files with 0 failures;
  `:zk-conformance:test` green; `go vet ./...` clean.
- **Evidence discipline.** State the method next to every number. The two published verification
  figures come from different host classes and must not be compared (2,414 ms per valid proof on a
  2 vCPU instance; mean 4.02 s / p50 3.83 s / p95 4.91 s on an i5-8365U).
- **Do not edit `epics.md`, the PRD, or the sprint tracker.** If a decision invalidates a story,
  report the change you recommend and stop — a story edit is a separate step.
- **ADR numbering:** `docs/decisions/` holds `ADR-002-STRICT-CBOR-SUBSET-DECODER.md` and
  `ADR-003-MINIMAL-JWE.md`. The next free number is `ADR-004`; confirm where `ADR-001` lives
  (upstream/ee-eudiw) before claiming that number. ADR-003 is load-bearing: it is why
  `direct_post.jwt` is the default.

### Environment traps that will cost you an hour each

- Go packages `.` and `./zk/...` link the Rust staticlib and fail at the **linker** until `make deps`
  has run. `make deps` places `google/longfellow-zk` as the sibling `../longfellow-zk` — keep that
  directory name.
- Build the prover through `make deps`, or with `RUSTFLAGS="-C target-cpu=x86-64-v3"`. Longfellow's
  `target-cpu=native` produces SIGILL binaries on virtualised hosts.
- `cargo build --examples` exits 0 with no binary when `examples/` is missing or empty; always
  `test -x` the prover afterwards.
- Python deps come from `pip install -r tests/requirements-ci.txt`, never unpinned `cbor2` /
  `cryptography` — the transcript vectors are golden and an encoder change shifts their bytes.
- `.github/workflows/ci.yml` defines two jobs: `fast (lint, vet, unit tests)` = `make check`, and
  `e2e (prover + verifier, tests/e2e.sh)` = `make test-go-zk` + `make e2e`.
- Reading a document is not evidence it is current: `docs/CONFORMITY.md`, `docs/MEASUREMENTS.md` and
  the `STEP*-RECORD.md` files are hand-maintained prose. Locate the symbol in the code before you
  repeat a row.

### Output contract

- **`_bmad-output/planning-artifacts/architecture.md`** — the record: the component and trust-boundary
  map (correcting `docs/ARCHITECTURE.md` where the tree has moved), the decisions above with their
  trade-offs, the mock→real seams, and a pointer to each ADR. Frontmatter with `status` and the input
  documents, as `epics.md` has.
- **ADRs** in `docs/decisions/`, numbered from ADR-004, each with context / decision / consequences /
  alternatives considered and rejected.
- **`docs/README.md`** — add an index row for anything new you place under `docs/`.
- **A short report back** naming: each decision made, the story acceptance criteria it satisfies, any
  backlog story the decision invalidates or re-scopes, and anything you could not establish (marked
  `[UNVERIFIED]`).

### Done means

1. A developer can implement Stories 3.1, 4.1 and 5.1 without inventing a decision.
2. Every invariant under `### Additional Requirements` in `epics.md` is either preserved by the record
   or explicitly changed, with the reason stated.
3. Every mock→real seam is named, with what replaces it and what must not be presented as real.
4. `make lint` exits 0; nothing committed; `git status` shows only your new files.
5. The remaining gate finding in `epics.md`'s Overview — the one naming the three deferred ADRs — is
   recorded as closed, so the readiness gate can move from CONCERNS to PASS.
