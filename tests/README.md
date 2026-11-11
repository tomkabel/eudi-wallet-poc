# Tests

Every test and check in this repository, where it lives, and what it proves.
`.github/workflows/ci.yml` is the definition of green; the conventions note is
[`docs/DEVELOPMENT.md`](../docs/DEVELOPMENT.md) ("Running the checks locally").

Unit tests sit beside the code they test — Go under `verifier/go/`, the
cross-language transcript check in `wallet/`. This directory holds the
end-to-end and load harnesses and offline registry checks. Do not move or rename anything here: ci.yml
invokes `tests/e2e.sh` by path and shellchecks the globs `scripts/*.sh`,
`tests/*.sh`, `tools/*.sh` and `demo/*.sh`.

## Carrier conformance runner

`python3 tests/run_carrier_conformance.py --json /tmp/carrier-report.json` runs
the Go carrier dispatch/parser checks, the committed carrier-vector verifier
test, then `tests/e2e.sh`. The E2E script keeps its human-readable assertions
and writes JSONL case observations to a private path supplied by the runner.
The report has one ordered row for each of the 19 numbered assertions, with
`path_label`, observed carrier, declared response mode, declared outcome, binding_property, assertion_result and result. Cases that do not
carry a presentation (for example, proving the response key is published) have
carrier `NOT_OBSERVED`; encrypted transport mode does not infer one. Child
failures remain `FAIL`/`ERROR`, and cases the E2E child never reports remain
`NOT_EXECUTED`.

The E2E inventory exercises the OpenID4VP interim PoC path. Its rows do not
claim that all 19 assertions exercise both encodings. The second layer runs the
same-proof committed vectors for `interim-json` and `mso-mdoc-zk-cbor`, whose
byte hashes and provenance are in `tests/vectors/carrier-v1/manifest.json`.
The runner requires `make deps` (including the Rust static library, prover and
verifier) and `make py-deps` (the locked Python inputs); `make e2e` invokes
it. `make check` runs only its fake-child inventory controls and remains
Rust-free.

Run the focused runner controls with
`python3 -m unittest discover -s tests -p 'test_carrier_conformance.py'`.
These test event order, exact/unique inventory, malformed records and missing
child behavior. Local execution establishes repository behavior only; it does
not prove independent-wallet behavior or external conformance. The OpenID4VP
`mso_mdoc_zk` path is profiled but unofficial; the Digital Credentials API is
the mandated ZK path. Story 2.4 remains the independent evidence gate. See the
[runner profile](../docs/profiles/CARRIER-CONFORMANCE-RUNNER.md).

## AV Profile registry checks (Python standard library, offline)

- `python3 tests/check_av_registry.py` validates the public Annex A revision/date
  and digest pins, retained excerpt/license bytes, complete ordered source-block
  and clause accounting, exactly one scoped test mapping or explicit v1 exclusion,
  and real referenced Go test files/symbols. It derives source blocks directly
  from retained Markdown, including declarative/inherited requirements and table
  rows. It prints mapped/excluded counts and exits nonzero on drift.
- `python3 -m unittest discover -s tests -p 'test_av_registry.py'` runs malformed
  input controls for missing coverage (even when block and clause entries are
  removed together), duplicate IDs, changed order/text/pins, empty or conflicting
  dispositions, dangling mappings, missing files/symbols and orphan tests.
- Both commands run through `make test-av-registry`, `make check` and the fast CI
  job. They require no network, Go/Rust build or third-party Python dependencies.
  They establish traceability structure, not executed AV conformance. Mapped
  protocol tests have their own build requirements and are not run by this
  validator. The full original digest is checked against fixed metadata; only
  the normative excerpt and license bytes are retained for offline hashing.

The [registry guide](../docs/profiles/AV-PROFILE-CLAUSE-REGISTRY.md) documents the
schema, source pin, 20 partial mappings, exclusions and refresh procedure.

## AV Profile fixture checks (Python standard library and Go, no Rust)

- `python3 tools/build_av_fixtures.py --check` compares all 49 committed files in
  `tests/vectors/av-profile/`, including the version/provenance manifest, against
  an in-memory rebuild; missing/extra files and changed source/provenance pins fail.
  This command writes nothing and never reserializes decoded golden CBOR.
- `python3 -m unittest discover -s tests -p 'test_av_fixtures.py'` checks source,
  local input, provenance, manifest and output corruption plus complete inventory
  failures and read-only CLI behavior.
- `cd verifier/go && go test ./oid4vp -run '^TestAVProfile'` independently checks
  all 32 A.11 literal bodies, ten direct binary decodes, selected outer containers,
  AV docType/namespace/age predicate, nested tuple values and local request bytes
  against runtime `DeviceRequest`/`ZkAgeQuery`. The actual allowlist accepts the
  local positive tuple and refuses all five missing and five tampered parameters,
  an unknown system and all four external version-6 tuples.
- `make test-av-fixtures`, `make check` and fast CI run the Python checks; the Go
  tests run through that job's existing package tests. No network, new keys or
  cryptographic proof generation is required.

See the [fixture guide](../docs/profiles/AV-PROFILE-FIXTURES.md) for regeneration
and version/origin details. Public examples remain version 6; local data uses
version 7. Encrypted examples stay opaque and the copied minted proof retains
its OpenID4VPHandover binding. These tests do not verify proofs or establish AV
transport conformance, scheme-owner circuit acceptance or certification.

## AVSUA-equivalent issuance (Android JVM)

- `AvsuaIssuanceTest` in `app/src/test/kotlin/ee/cyber/wallet/domain/provider/wallet/`
  calls the actual `CredentialIssuanceServiceMock` mint, decodes its AV mdoc docType
  and bound device key, and verifies the issuer signature. It refuses withheld
  bundle/instance/key evidence at the outer boundary and public AV helper;
  separately tampered instance and key signatures; an untrusted signer with a
  supplied x5c; wrong key/audience/transaction/challenge and signed bindings;
  expired/future/missing/oversized validity; absent/negative integrity and absent,
  wrong-value or wrong-type key-security assertions. Fresh contexts/tokens across
  local APs omit account identifiers in claims and JOSE headers and reject replay.
  Separate controls isolate the AP context's original key binding and cross-AP
  ownership with shared provider trust and holder key. Fixed-clock controls
  isolate exact token and AP context expiry, including expiry during trust
  resolution and refusal after expired-context pruning; concurrent reuse allows
  exactly one mint. Additional controls
  cover malformed/type/algorithm/jti/demo markers, absent or failing trust,
  cancellation, provider reset and signing-failure retry. Actual application
  factory tests establish configured trust before asset loading and evidence
  forwarding to the real mint. The real `WalletProviderBatchAgeIssuer` loop runs
  with fake Android key storage and Room, forwards evidence for generated keys,
  rolls back all keys/rows on a later mint refusal or evidence-provider failure
  and cancellation, and preserves EE-PoA issuance
  without AVSUA. The platform keystore and actual platform integrity are outside
  JVM coverage; generated JVM issuer keys replace Android asset loading only.
- The `local_mocks` issuer keys are not tracked: Gradle runs `tools/gen-mock-keys.sh` (also `make keys`) before the asset merge and writes
  them, with a throwaway IACA root they chain to, into the gitignored `app/src/local_mocks/assets/keys/`.
- Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ANDROID_HOME=/opt/android-sdk ./gradlew :app:testLocal_mocksUnitTest :app:assembleLocal_mocks`.
  To select this test class use the unit task alone with
  `--tests ee.cyber.wallet.domain.provider.wallet.AvsuaIssuanceTest`.
- [AVSUA-ISSUANCE.md](../docs/profiles/AVSUA-ISSUANCE.md) documents the primary
  requirements, local format, configured provider trust, refusal causes and
  demonstration-only assertions. This is local mock equivalence, not TS3 or
  external-provider conformance. Registry A.5 exclusions remain: its runner
  executes Go probes and does not execute these JVM tests.

## AV issuance invocation (Android JVM and adb surface)

- `AvVciInvocationTest` in `app/src/test/kotlin/ee/cyber/wallet/domain/credentials/`
  exercises the pure parser and the actual shared dispatch callbacks used by
  WalletApp: valid offers, inert public grants, missing/empty offers, strict JSON
  and duplicate keys, invalid percent escapes/UTF-8, query duplicates/conflicts,
  encoded separators and one-pass decoding, invalid issuer/configuration IDs,
  authority/path/fragment refusals, unsupported query parameters and schemes,
  preserved presentation/onboarding generic callbacks and visible navigation
  failures. Every refusal asserts its full parameter/reason and that the entry
  callback was never invoked; Unicode percent-hex digits are rejected.
  Regressions also cover issuer query/empty-port/bidi-control refusals, bounded
  JSON depth and exact-limit/oversized AV offers.
- `WalletInvocationViewModelTest` in `app/src/test/kotlin/ee/cyber/wallet/ui/navigation/app/`
  covers retained typed/provider state, FIFO refusals, repeated-offer reset,
  stale callback protection, bounded capacity/counting and oversized AV/existing
  links without retained raw data. These two classes run with the unit command
  below; use `--tests ee.cyber.wallet.domain.credentials.AvVciInvocationTest
  --tests ee.cyber.wallet.ui.navigation.app.WalletInvocationViewModelTest` to
  select only invocation coverage.
  Run `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew :app:testLocal_mocksUnitTest`
  (the Android CI unit suite). JVM Android methods are stubbed; this establishes
  parser/dispatch behavior, not manifest or Compose surface evidence.
- `python3 tests/test_av_vci_android.py --help` documents the standard-library
  adb smoke harness. Install the current `:app:installLocal_mocks` APK on an
  unlocked Android 31+ emulator/device and run
  `python3 tests/test_av_vci_android.py --serial emulator-5554`. The test verifies
  VIEW/BROWSABLE manifest resolution, cold/warm valid entry with distinct issuers,
  visible `eu.europa.ec.av.1`, Cancel and provider-unavailable Continue, named
  missing/malformed/unsupported explanations, known-scheme unmatched navigation,
  and an unknown explicit intent. Night-mode recreation checks cold/warm valid,
  provider-unavailable and refusal state; queued refusals must precede the next
  valid offer. Existing onboarding/presentation links reach NavController and
  destination UI. Cancel/Close must actually dismiss the previous dialog.
  Recreation must change the local Activity instance before restored UI is
  accepted. A full inbox followed by five excess arrivals must eventually show
  the exact localized capacity count and allow dismissal. Duplicate query
  parameters and malformed percent input are also checked on Android.
  API33+ executes English and Estonian and restores the original app locale and
  night mode in `finally`. API31/32 require two separately configured language
  passes with `--legacy-locale en` and `--legacy-locale et`; no unsupported locale
  shell command is issued. The harness force-stops without clearing app data.
  An unavailable runtime is a verification gap, never a surface-test pass.
  The App workflow runs this harness on Android 35 for app and harness changes;
  it installs `local_mocks` and checks both languages in an emulator job.

See [the invocation guide](../docs/profiles/AV-VCI-INVOCATION.md) for grammar,
example generation and the normative `av-vci://` versus public-example `av://`
discrepancy. These checks cover external issuance entry only; the provider is
not integrated, no mock is minted and no AVSUA/conformance claim follows.

## PKI gates and trust vectors

From [the PKI remediation plan](../docs/planning/PKI-REMEDIATION-PLAN.md). Each
gate has a seeded failure that proves it turns red.

| Check | What fails it | Run |
|---|---|---|
| `tests/check_pki_gates.py` | a `.gitleaks.toml` allowlist naming a path that holds a private key at HEAD; trust-all switch back in the wallet; `*Mock` class or mock status URI outside `local_mocks`; `-ignore-trust-freshness` outside a `devflags` build; trust material unlisted in `trust/MANIFEST.json`, changed without `--update`, or under 30 days from `notAfter`; a private key not marked test-only; a mirrored Maven file unpinned or without a licence | `make pki-gates` |
| `tests/test_pki_gates.py` | a gate that stays green on its seeded defect | `make pki-gates` |
| `tests/vectors/trust/cases.json` | Go (`oid4vp.TestSharedTrustVectors`) or Kotlin (`SharedTrustVectorsTest`) disagreeing with a case's `expect`: IACA→DS profile, path length, expiry, algorithm policy. Regenerate with `tools/build_trust_vectors.py` | both unit suites |
| `issuer/test_mint_custody.py` | a self-signed DSC; an unencrypted persisted issuer key outside `--test-mode` | `make test-py` |
| `tools/publish_guard.py`, `tests/test_publish_guard.py` | a tracked `demo-data/` path, `*.p12`/`*.jks`/`*.keystore`, or PEM with a `PRIVATE KEY` header outside `tools/publish-guard.allow`; the test plants a key in a throwaway repo and asserts the guard fails | `make publish-guard` |
| `tools/pin_gate.py`, `tests/test_pin_gate.py` | a `uses:` under `.github/` not pinned to a 40-hex commit SHA, or a Dockerfile `FROM` without `@sha256:`, outside `tools/pin-gate.allow` (one reasoned entry: the sandbox image, Phase 6); the test plants unpinned references in a throwaway repo | `make pin-gate` |
| `security.yml` | gitleaks (pinned by commit) over full history and a seeded key it must flag, the publish guard, the pin gate, zizmor (blocking, version 1.32.0, online audits), pip-audit over the `uv export` of `uv.lock`, govulncheck, gosec outside `verifier/go/.gosec-baseline.json` | CI |
| `sbom.yml` | CycloneDX SBOMs, uploaded as artefacts: Gradle (`./gradlew cyclonedxBom`), Rust (`cargo cyclonedx` on `verifier/zkverify-ffi`), Python (`cyclonedx-py` over the `uv sync --locked` environment) | CI |
| `mirror.yml`, `.github/maven-mirror/` | `fetch.py` rebuilds the whole gh-pages Maven mirror from `manifest.json` (sha256 per file; upstream sources, plus `vendored/` for the cose-java snapshot upstream no longer serves) and fails on any mismatch | CI (weekly, manual) |
| `app.yml` | `:app:lintRelease` security checks outside `app/lint-baseline.xml`; a `*Mock` class in release bytecode | CI |
| `codeql.yml` | CodeQL `security-extended` alerts for Go, Python, Kotlin | CI |

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

- `TestCircuitHashKnown` (:12): `CircuitHash(7, 1)` equals the pinned hash `8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121`.
- `TestCircuitHashUnknown` (:23): `CircuitHash(99, 1)` returns an error.
- `TestRegistryRejectsUnlisted` (:29): a registry listing only hash `deadbeef` rejects `Check(7, 1)` — an unlisted circuit cannot pass the accepted-circuit allowlist (EE-ZKP-023).

### `wallet/test_transcript.py` — cross-language vectors, the Python side

- Loads `wallet/present.py` by path and asserts `present.session_transcript()` reproduces both hex vectors and that the two differ; prints `ok — 2 transcript vectors match the Go verifier` and exits 0, or lists failures and exits 1. `--print` emits the vectors to paste into the Go test.
- Run by the `fast` CI job (`python3 wallet/test_transcript.py`, the job's "the wallet's half of the transcript pin" step), or by hand: `python3 test_transcript.py` from `wallet/`.

## `tests/e2e.sh` — the presentation flow and its negative controls

Drives the whole flow against freshly minted attestations: builds nothing,
mints an adult credential (`--over 16 18 21`), a minor one (`--over 16
--under 18 21`) and the second-doctype fixture (`--doctype eu.europa.ec.av.1`,
W6), merges all three issuer lists into one trust store, starts
`verifier/zkverify` on a kernel-picked free port, waits for `/healthz`, then
drives `wallet/present.py` once per case and greps the verifier's answer out
of the wallet output. Requires `EE_PROVER` (the `ee_poa_demo` binary);
`EE_VERIFIER` optionally overrides the verifier path; the wallet needs python
`cbor2`, `cryptography` and `jwcrypto`. Nineteen assertions:

1. Happy path, plain mode: the adult proves `age_over_18`; output must contain `"valid": true`.
2. Transcript binding: `--tamper-nonce` makes the proof cover a different nonce; the verifier must answer `"valid": false`.
3. Single use: `--replay` posts the same `vp_token` twice; the second post must be refused with `HTTP 410` (the store's `ErrAlreadyUsed` maps to `StatusGone`); the script greps `replay     : HTTP 410`.
4. Predicate binding: the minor proves `age_over_18 = false` with `--allow-false-predicate`; the verifier must answer `"valid": false` — refused on the value, not by accident.
5. The prover's own guard: running `ee_poa_demo` for the minor without the override must exit non-zero and name `--allow-false-predicate` — the two defences hold independently.
6. Encrypted mode (ADR-003): the `direct_post.jwt` request publishes the response key in `client_metadata.jwks`.
7. Encrypted happy path: an over-18 holder answers with a JWE; `valid: true`.
8. Ciphertext on the wire: the posted body is a compact JWE, never plaintext.
9. Tampered JWE fails closed: a flipped byte gets a `400` with nothing parsed from it.
10. Plain post to an encrypted session: refused with `400`, naming the encrypted requirement.
11. Default mode: a fresh `/present/new` without flags answers `direct_post.jwt`.
12. Multi-credential (W6): with `-multi-credentials 2`, one session requests the national PoA and the AV doctype (two issuers); a wallet holding both answers both entries and the verifier answers `valid: true` with `all 2 credentials proved in zero knowledge`.
13. Multi-credential all-or-error: a response answering only one of two queried credentials fails the session, with the missing credential id named in the detail.
14. Unknown carrier bytes: a vp_token entry matching neither carrier is refused as `unknown-carrier`, not parsed as JSON.
15. Malformed envelope: a truncated JSON envelope is refused as `malformed-envelope`, not `unknown-carrier`.

16. Controlled client_id transcript mutation in encrypted mode: HTTP 200 valid:false with transcript-binding.
17. Controlled response_uri transcript mutation: same refusal, while posting to the original destination.
18. Controlled jwk_thumbprint transcript mutation: same refusal, while encrypting to the original response key.
19. Well-formed empty CBOR: HTTP 400 valid:false on the plain sniff path with no-presentation and the same persisted polling detail.

The six controlled binding cases require exact case-specific property labels;
missing/wrong/generic labels fail runner validation. Nonce, replay, predicate and
decryption assertions require stable runtime names. Runtime opaque proof failures
name transcript-binding without attributing an individual input. See
[failure reasons](../docs/profiles/CARRIER-FAILURE-REASONS.md).

Output is `PASS: 19/19` or `FAIL: n of 19 assertions failed`; exit status is
non-zero when any assertion fails. Note what is absent: there is no
untrusted-issuer case (the script deliberately trusts all minted issuer keys,
which is why case 4 fails on the predicate value rather than on trust) and no
unlisted-circuit POST case; unlisted-circuit rejection is covered only by
`TestRegistryRejectsUnlisted` above.

## `tests/load_test.py` — admission control under 200 concurrent verifications

- Fires `--requests` (default 200) concurrent POSTs to `/zkverify`, started with `-unsafe-dev-api` (the endpoint 404s without it), each carrying one well-formed proof whose middle byte is flipped — still the right circuit, so a rejection costs a full ~2.5 s verification rather than a parse error.
- Asserts: the verifier process is still alive; every response is `200` or `503` (a dropped connection is recorded as `ERROR ...` and fails); peak OS thread count is below the request count and below 1,000 (Go aborts the process at 10,000 threads); at least one request was shed with `503`, so the admission limit was genuinely exercised.
- Demonstrates why the limit exists: `zk.Verify` is a cgo call that pins an OS thread for its duration, so without shed-load behaviour 200 simultaneous proofs would walk the process toward Go's thread cap — a remote kill switch, one HTTP request per thread.
- Mints and proves once (~15 s the first time); `--work DIR` reuses a previous run's proof; `--max-concurrent-verify` is passed to the verifier (0 leaves it at its default). Not invoked by ci.yml; run it by hand against a built prover and verifier.

## AV conformance runner

`python3 tests/run_av_conformance.py --json /tmp/av-report.json --html /tmp/av-report.html` executes the
repository verifier's Go tests mapped by `docs/profiles/av/clause-registry.json`
and the dedicated `TestAVConformance*` probes. It prints per-clause results and
writes sibling JSON and offline English HTML reports. Omitting `--html` uses
the JSON filename with an `.html` suffix. Both carry canonical Git/profile
revisions, clean/dirty status, CPU baseline and advertised flags, Rust/Cargo/Go
and installed JDK versions (or unavailable), required CPU/Rust baseline versus
observed FFI configuration and environment, the sibling Longfellow
pin/revision/mismatch, registry path/SHA-256 and staticlib hash. Effective
`go env -json` captures overlay flags in GOFLAGS, workspace/GOENV, CGO settings,
compilers/flags, target and toolchain selection from the module directory;
current settings do not attest historical compilation.
Per-test Go assertion output is retained; package startup/build errors remain
ERROR diagnostics rather than conformance FAIL. Expandable HTML also includes
exact Go commands, package exit codes/output, all selected named probe results
and passing/failing/skipped negative controls. Supplementary probe
FAIL/ERROR/NOT_EXECUTED states affect the verdict independently of clause and
negative thresholds. All clauses appear per candidate
in profile order with inline test identifiers or exclusion reasons. See
[clean-checkout reproduction](../docs/profiles/AV-REPORT-REPRODUCTION.md) and the
[committed report](../docs/profiles/av/report.html). The pinned registry denominator remains the
94 retained clauses; this run maps 20 distinct clauses and records negative
controls separately. The audit is expected to exit nonzero while known AV
fallback mismatches remain and while no independent candidate is configured.
Do not interpret partial primitive checks as complete AV or ISO conformance.

An external adapter is invoked with `--candidate-command '<command>'`.
`shlex.split` preserves quoted arguments; the resulting argv executes directly,
without interpreting shell operators, expansion or globbing. For example,
`--candidate-command 'python3 "/path with spaces/adapter.py"'` retains the script
path as one argument. Shell users must explicitly invoke their script or shell
as the adapter. Missing executables, malformed quoting and empty argv become
`NOT_EXECUTED` with startup detail; a launched executable exiting nonzero,
including 126/127, becomes `ERROR` with its diagnostic. A shell/script adapter
must express failed inner candidate startup as `NOT_EXECUTED` schema results.

Both output streams must be valid UTF-8. The adapter must print one JSON object
to stdout with integer `schema_version: 1` (not `true` or `1.0`), a `candidate` object
containing nonempty `name` and `revision`, and a `results` array. Each result
must identify a registry `test_id`, a named `property`, a state (`PASS`, `FAIL`,
`ERROR`, or `NOT_EXECUTED`), and execution evidence. Missing or invalid evidence
must remain unavailable/error evidence. Independent candidate setup is not
available in this checkout; the report lists the operator actions instead of
inventing a result. Malformed UTF-8 in either stream, malformed JSON/text
(including unpaired surrogate escapes, nonfinite numbers and excessive
nesting), duplicate JSON object keys at any
depth, duplicate test IDs, crashes and timeouts make every external mapped row
`ERROR`, with the cause retained in JSON and HTML. `--timeout` bounds each
local Go command and adapter execution. Adapter timeouts kill its original POSIX
process group, including members holding output pipes open, allow up to one
additional second to drain output, then close remaining pipes and reap the
direct adapter before returning. Deliberately detached sessions are outside
that group and may survive; retained pipes produce a bounded-drain ERROR.
Host init reaps orphaned children. Captured output has no byte quota, a
pre-existing limitation.

`python3 -m unittest discover -s tests -p 'test_av_conformance.py'` tests local
coverage/negative-control accounting and retained known failure assertions;
complete ordered clauses and inline exclusions in JSON/HTML; all result states;
literal HTML escaping with no network resources; canonical provenance and
required baseline/observed configuration, effective Go settings and registry
validation/fingerprints; supplementary probe verdicts and pass-only qualified
negative counts; actual clean/modified/untracked Git evidence; skipped, missing,
malformed UTF-8 in both streams, conflicting raw JSON keys at all depths,
escaped surrogate text, excessive nesting, nonfinite numbers, exact integer
schema versions, quoted literal argv, startup versus executed 126/127,
and independently supervised timeout controls with child readiness, pipe-holding
process-group members, detached sessions, watchdogs and delayed-write controls;
package-specific linker errors, unavailable Go, SIGILL and host/JDK
diagnostics, unavailable Git provenance and unreadable host inputs; explicit
HTML output and resolved-path/hardlink collisions. These controls use
synthetic evidence where needed and do not establish independent conformance.
A successful audit control uses synthetic passing mapped/supplementary Go
events, eight negative passes, a sufficient host diagnostic baseline and an
actual synthetic adapter subprocess. It verifies return 0, matching JSON
verdict/thresholds/zero failures, readable HTML and retained operator actions.
They run through the existing Makefile/fast-CI target and again in e2e after
the staticlib build, so the actual local-audit case executes there. Fast CI
uses synthetic controls and skips only that actual audit when the library is
absent. External reproduction remains
UNPERFORMED with imperative operator actions in every report.

## What CI runs, and where the Rust toolchain matters

`fast` (no Rust toolchain): `gofmt -l .` in `verifier/go` (output is the
signal — `gofmt -l` exits 0 either way), `go vet ./...` (type-checks the cgo
package without linking, covering every package), `go test ./oid4vp/...
./internal/... ./circuits/...` with `-race`,
`python3 -m compileall -q issuer verifier wallet tests sandbox`, `ruff check
--isolated --select F,E9 --exclude eudi-arf,_bmad,.agents,.claude .` (ruff 0.16.6),
`python3 tests/check_av_registry.py` and
`python3 -m unittest discover -s tests -p 'test_av_registry.py'` (offline AV
traceability and mutation controls),
`python3 tools/build_av_fixtures.py --check` and
`python3 -m unittest discover -s tests -p 'test_av_fixtures.py'` (read-only fixture
bytes/provenance and corruption controls),
`python3 wallet/test_transcript.py` (the wallet's half of the golden vectors; the
step installs the locked `ci` group (`uv sync --locked`, hashes in `uv.lock`), since `present.py` imports
`cbor2` and `cryptography`), `python3 issuer/test_mint_device_key.py`,
`python3 sandbox/test_age.py` (the sandbox issuer's age reckoning, EE-POA-027), and
`shellcheck --severity=warning scripts/*.sh tests/*.sh tools/*.sh demo/*.sh`.
`python3 -m unittest discover -s tests -p 'test_doctor.py'` (`make test-doctor`: `tools/doctor.py`
fails, one line per check, with no prover and no longfellow checkout). `make check` runs the same set.

CI is path-scoped: a `changes` job (`dorny/paths-filter`) gates `fast` and `e2e` in `ci.yml` and the
two Android jobs in `app.yml`; `ci-ok` and `app-ok` are the checks to require, and they treat a
skipped job as success.

`e2e` (needs a Rust toolchain): checks out `google/longfellow-zk` as a sibling
of this repository at the revision committed in `verifier/zkverify-ffi/longfellow-rev.txt`
(61a8a735964d1b22bccf79bf14ef6767249cdf92, fetched by `scripts/bootstrap-longfellow.sh`),
builds the prover examples with `RUSTFLAGS=-C target-cpu=x86-64-v3` and asserts
`ee_poa_demo` exists (`test -x`), builds the verifier
(`cargo build --release` in `verifier/zkverify-ffi`, then
`go build -o ../zkverify .`), runs `go test -race . ./zk/...` and the Python
`test_av_conformance.py` suite with the actual local audit, then `uv sync --locked
--only-group ci` and `tests/e2e.sh` with `EE_PROVER` set. The `zk` and package
`main` tests are here and not in `fast` because package `zk` links the Rust
staticlib through cgo and `main` imports it — neither test binary can be linked
without the Rust build. Between the two jobs
each package is tested exactly once.

## Running the checks locally

From the repository root. The `fast` job:

```bash
pip install ruff==0.16.6 && make py-deps   # pinned as in ci.yml; py-deps builds .venv from uv.lock
make check          # lint + fast Go/Python tests + AV registry/fixture checks
```

The `e2e` job — `make deps` checks out `longfellow-zk` as a sibling at the revision
committed in `verifier/zkverify-ffi/longfellow-rev.txt` and builds the prover and the
verifier (expanded form: `docs/DEVELOPMENT.md` "Build both"):

```bash
make deps           # also records the prover's sha256 and revision; `make doctor` re-checks them
make test-go-zk     # go test -race . ./zk/..., with EE_BENCH_SKIP=1 as in CI
make e2e            # tests/e2e.sh against the built prover and verifier
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
- Rust: `verifier/zkverify-ffi/src/lib.rs` has one `#[cfg(test)]` unit test (`oversize_inputs_are_rejected_not_aborted`), but ci.yml runs no `cargo test`, so it only runs by hand; `zk-age-poc/` has none. In CI the Rust side is covered only by building and by e2e.sh case 5.
- `tests/load_test.py` is not invoked by ci.yml — run it by hand. (Only `tests/e2e.sh` runs in CI; the root `README.md` says so.)
- No coverage measurement anywhere.
