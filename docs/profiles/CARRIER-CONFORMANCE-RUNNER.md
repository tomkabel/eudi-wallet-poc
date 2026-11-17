# Carrier conformance runner

## Coverage model

The runner reports two separate layers:

1. `tests/e2e.sh` retains its fifteen existing assertions and appends three
   transcript-input controls (16–18) and an empty-presentation refusal (19). Each assertion emits one machine-readable case record with its
   case id, path label, observed carrier (or `NOT_OBSERVED` when the assertion
   has no presentation bytes), declared response mode, declared outcome, binding_property and result. Carrier and
   response mode are independent fields. In particular, `direct_post.jwt`
   describes encrypted response transport; it does not imply a carrier.
2. Go tests parse and dispatch carriers, then
   `TestCarrierVectorsVerifyIdentically` checks the two committed encodings
   against the same proof and transcript. This vector layer is where both
   encodings are covered. The E2E suite does not claim a cross-carrier matrix.

The E2E proof assertions use the OpenID4VP interim PoC carrier; the empty
response case observes the CBOR carrier. The suite retains the
`openid4vp-interim-poc` path label. The Digital Credentials API is the mandated
ZK presentation path. OpenID4VP `mso_mdoc_zk` is profiled but unofficial and
must not be read as EU endorsement.

## Result vocabulary

The Go dispatch layer exposes stable error identities for `missing-entry`,
`unknown-carrier` and `malformed-envelope`. A missing `vp_token` query id names
that id and is not treated as a parse failure. `{` selects the interim JSON
parser even for malformed JSON, while CBOR map and array heads select the
strict CBOR parser. Other leading bytes are `unknown-carrier`. Recognized but
malformed envelopes are `malformed-envelope`. An undecryptable compact JWE is
reported by the response handler as a decryption failure before any
presentation parsing. None of these outcomes is a binding-property failure.

The verifier response includes the carrier names selected by its parser; the
E2E recorder reads those response fields rather than assigning the carrier in
advance. Cases rejected before carrier parsing have an empty carrier list.

Each E2E result is `PASS` or `FAIL`; a child startup/build problem is `ERROR`,
and an assertion with no child record is `NOT_EXECUTED`. The runner validates
that every numbered assertion found in `tests/e2e.sh` occurs exactly once and
refuses malformed, duplicate, unknown or unclassified records. Its report
includes suite commands, exit codes and output, case rows, vector outcomes and
inventory diagnostics. The vector suite setup is `ERROR` when its required Rust
static library is absent. The vector status describes the paired same-proof
test; a failing leg is identified in that suite's retained output.

The six deliberate violations name nonce, single-use, predicate-value,
client_id, response_uri and jwk_thumbprint in case-specific binding_property
fields. The runner validates those fields, preserves assertion_result separately
from metadata validation errors, and prints the supplied property even on FAIL.
Other cases require not-applicable. Case 19 reports no-presentation separately.
See [Carrier failure reasons](CARRIER-FAILURE-REASONS.md) for runtime prefixes,
HTTP semantics, controlled attribution and the limits of transcript-binding.

## Vectors and provenance

`tests/vectors/carrier-v1/manifest.json` identifies `tools/build_cbor_fixture.py`
and the source fixture produced by the Multipaz 0.99.0 wallet at the step 8.7
stage. It records the byte hashes of the interim JSON and CBOR vector files and
states that both wrap the same proof. The CBOR wrapper reuses the source
`zkDocument` bytes verbatim. The runner never regenerates vectors; the Go tests
assert the committed files and verify both wraps over the OpenID4VPHandover
transcript.

## Run locally

Install the pinned dependencies and build the documented Rust spine:

```sh
make py-deps
make deps
make e2e
```

`make e2e` calls `tests/run_carrier_conformance.py`, which runs the parser
tests, vector verification and E2E script in that order. To retain a JSON
report directly, after building the prerequisites run:

```sh
EE_PROVER="../longfellow-zk/rust/target/release/examples/ee_poa_demo" \
  EE_VERIFIER="$(pwd)/verifier/zkverify" \
  python3 tests/run_carrier_conformance.py --json /tmp/carrier-report.json
```

The focused fake-child controls are
`python3 -m unittest discover -s tests -p 'test_carrier_conformance.py'` and
are part of `make check`. They require no Rust toolchain. CI runs the same
focused controls in its fast job and runs the full runner in the E2E job.

## External target and retained attempt

The selected public implementation is **Multipaz sample wallet 0.99.0**, at
canonical source commit `7486ddbca490038c3580513d8273b9bbf18c6b70`. Its public
upstream URL, tag resolution, rationale and preparation commands are recorded in
[external-target.json](carrier/external-target.json). Existing carrier fixtures
derive from this release's ZK serialization, which makes it an attributable
independent implementation target; fixtures alone prove no compatibility.

The [2026-11-11 attempt](carrier/external-run-2026-11-11/README.md) and its
[JSON report](carrier/external-run-2026-11-11/report.json) retain the actual host,
verified checkout, argv, exit codes and output. The build exceeded its 120-second
bound (exit 143 after group termination); adb reported no connected devices.
All 19 cases and both carrier vectors remain `NOT_EXECUTED`, and the overall
external result is `BLOCKED`. The separately dated
[supplement](carrier/external-run-2026-11-11/supplement-2026-11-11.md) retains
later clone/toolchain diagnostics and hashes of the unchanged original artifacts.
These are repository-prepared agent evidence, with public publication and
independent operator reproduction still outstanding. No target adapter is shipped.

The separately retained [redispatch attempt](carrier/external-rerun-2026-11-11/README.md)
observed a connected device and another 120-second build timeout. The
[extended build attempt](carrier/external-build-2026-11-11/README.md) allowed
600 seconds, installed target-requested SDK components and reached build tasks,
then timed out. Neither executed carrier assertions. These newer observations
do not alter the earlier no-device observation or its original artifact bytes.

The [host execution and vector feed](carrier/external-host-2026-11-11/README.md)
then built and ran the pinned Multipaz JVM APIs. Its upstream proof-generation
self-test passed. Separately, feeding the two exact repository vectors produced
CBOR proof verification PASS and interim-JSON parser FAIL. This is a partial
external compatibility failure, with all 19 flow assertions still NOT_EXECUTED;
neither Gradle success nor the successful CBOR leg establishes full-suite PASS.
The vector harness lives outside the pristine target source and preserves actual
carrier observations rather than relabelling native CBOR as interim JSON.

## Explicit external invocation

Ordinary checks never download or execute external code. Prepare the public
checkout outside this repository explicitly, as described in external-target.json,
then run from this repository:

```sh
python3 tests/run_external_carrier.py \
  --checkout /tmp/eudi-carrier-multipaz-0.99.0 \
  --prepare-command 'adb devices -l' \
  --prepare-command './gradlew --no-daemon --console=plain :samples:testapp:assembleDebug' \
  --timeout 120 --output-limit 1048576 \
  --json /tmp/external-carrier.json --markdown /tmp/external-carrier.md
```

Without an adapter this records preparation only and remains `BLOCKED`, even
when the build succeeds. A suitable execution environment (device/emulator or a
supported host path), a runnable target, and an adapter exercising the actual
target are engineering/environment prerequisites. Implementing the adapter is
engineering work; independent reproduction and maintainer publication are the
human handoff. Once implemented,
add `--adapter-command '/absolute/path/to/adapter --its-option value'`. Repeated
preparation commands and the adapter use `shlex.split` argv parsing, no shell,
and execute in the external checkout. Each command has a separate time and
combined stdout/stderr byte bound (defaults 120 seconds and 1 MiB); timeout or
excess output terminates members of the original dedicated process group.
Successful parent completion also cleans up remaining members of that group.
Descendants that detach into another session (for example with `setsid`) are
outside this containment guarantee.

Before preparation, the runner verifies the checkout's HEAD, exact origin URL
and clean tracked/untracked status. Preparation and adapter execution are
followed by revision/cleanliness checks before attribution. The report records
the suite's own source revision, dirty state and inventory/vector hashes. These
observations delimit source attribution; they cannot prove an arbitrary adapter
actually drove the target or establish independent operator evidence.

## Adapter contract

The adapter inherits the environment and receives these absolute paths and pin:

| Variable | Meaning |
|---|---|
| `EE_EXTERNAL_CARRIER_EVIDENCE` | Fresh private output path; write one UTF-8 JSON object here, separately from logs |
| `EE_EXTERNAL_CARRIER_REVISION` | Required canonical Multipaz commit |
| `EE_EXTERNAL_CARRIER_CHECKOUT` | Verified external checkout and command working directory |
| `EE_EXTERNAL_CARRIER_ROOT` | Repository root containing case inventory and committed vectors |

The evidence object requires integer `schema_version: 1`, `target` exactly
`{"name": "Multipaz sample wallet 0.99.0", "revision": "7486ddbca490038c3580513d8273b9bbf18c6b70"}`,
`cases`, and `carrier_vectors`. Unknown/missing/duplicate cases or carriers,
incorrect metadata, duplicate JSON keys, nonfinite constants, invalid UTF-8,
symlinks/nonregular files and oversized evidence are `ERROR`. Raw evidence bytes
from successfully read bounded regular files are retained as base64 alongside
readable evidence and process diagnostics, including when subsequent decoding or
validation fails. Evidence rejected before a successful bounded read has no raw
byte retention.

`cases` must contain exactly one row for each string case ID `1` through `19`.
Each row requires `case_id`, `path_label: "openid4vp-interim-poc"`, `carriers`
(an observed list, including duplicates where required),
`declared_response_mode`, `declared_outcome`, `binding_property`, `result`
(`PASS` or `FAIL`) and nonempty `evidence` describing the executed assertion.
Metadata must match this inventory exactly:

| Case | carriers | declared_response_mode | declared_outcome | binding_property |
|---|---|---|---|---|
| 1 | interim-json | direct_post | proof-verified | not-applicable |
| 2 | interim-json | direct_post | transcript-binding-refused | nonce |
| 3 | interim-json | direct_post | replay-refused | single-use |
| 4 | interim-json | direct_post | false-predicate-refused | predicate-value |
| 5 | empty | direct_post | prover-guard-refused or prover-guard-failed | not-applicable |
| 6 | empty | direct_post.jwt | response-key-published | not-applicable |
| 7 | interim-json | direct_post.jwt | proof-verified | not-applicable |
| 8 | empty | direct_post.jwt | encrypted-wire-assertion | not-applicable |
| 9 | empty | direct_post.jwt | decryption-failed | not-applicable |
| 10 | empty | direct_post | plain-response-refused | not-applicable |
| 11 | empty | direct_post.jwt | encrypted-default-confirmed | not-applicable |
| 12 | interim-json, interim-json | direct_post | multi-credential-proof-verified | not-applicable |
| 13 | empty | direct_post | missing-entry | not-applicable |
| 14 | empty | direct_post | unknown-carrier | not-applicable |
| 15 | empty | direct_post | malformed-envelope | not-applicable |
| 16 | interim-json | direct_post.jwt | transcript-binding-refused | client_id |
| 17 | interim-json | direct_post.jwt | transcript-binding-refused | response_uri |
| 18 | interim-json | direct_post.jwt | transcript-binding-refused | jwk_thumbprint |
| 19 | mso-mdoc-zk-cbor | direct_post | no-presentation | not-applicable |

Here `empty` means `[]`; the two entries in case 12 mean
`["interim-json", "interim-json"]`. Case 5 requires `prover-guard-refused` for
`PASS` and `prover-guard-failed` for `FAIL`. `carrier_vectors` requires exactly one row
for each of `interim-json` and `mso-mdoc-zk-cbor`, each containing `carrier`,
`result` (`PASS` or `FAIL`) and nonempty assertion `evidence`. The complete
`payload()` helper in `tests/test_external_carrier.py` illustrates the schema
with synthetic evidence used only by offline controls. An adapter exit code or
that synthetic payload alone is never external execution evidence.

Exit codes are `0` = complete validated external `PASS`, `1` = retained assertion
`FAIL`, `2` = setup/reach/build/device/adapter `BLOCKED` (also argparse usage
errors, which produce no reports), and `3` = evidence/protocol/attribution
`ERROR`. Runtime metadata errors produce both reports with `ERROR`. Valid
failed assertions remain `FAIL` even if the adapter exits nonzero; failed source
attribution instead takes `ERROR` precedence while retaining assertion rows. A
passing payload with a failed process cannot pass. No external state changes the
local runner's exit semantics.

## Evidence boundary

These commands execute this repository's wallet, verifier, prover and checked-in
fixtures. They do not establish independent-wallet behavior, third-party
verifier behavior, scheme-owner acceptance or external conformance. No result
from this local runner substitutes for the independent implementation and
published evidence required by Story 2.4.

## Offline version evidence and maintenance

Run the stdlib-only assertion without Rust or network access:

    python3 tests/check_carrier_versions.py
    make test-carrier-conformance

The make target runs that assertion, its mutation controls, the existing
fake-child runner regressions and all external runner controls; it is included
in make check. External controls use synthetic evidence and bounded local
subprocesses, with no network, device or upstream build prerequisite. The runner invokes
the same validator. Bad evidence marks the committed-vector suite and both
carrier rows ERROR, retains item-specific diagnostics and prevents a passing
report even when every child proof test passes.

The profile's Version header is authoritative. Every numbered rule has a visible
carrier-profile/source label and its own derivation in
[carrier-versions.json](carrier-versions.json). Each of the five published
payload/support files has a manifest provenance entry. Circuit versions (6/7),
Multipaz release 0.99.0 and external document revisions identify different things.
The validator discovers all numbered rule markers and all files recursively under
carrier-v1 except manifest.json; discovered files, hashes and provenance must
agree, with no duplicate rules or orphan metadata. Evidence paths and uncertainty
references resolve locally. Validation reads files only and never refreshes sources.

For a deliberate revision:

1. Verify changed external identifiers against primary sources; record what was
   checked and retain unresolved provenance as [UNVERIFIED] with a spec §23 item.
   Never invent an ISO second-edition draft revision or call local policy ISO law.
2. Review every rule-specific derivation and every fixture's source references.
   Update the authoritative profile Version, all visible labels and matching
   inventory/manifest labels together. A header-only bump must fail every old label.
3. Run the commands above and make check. Run the existing Go byte/crypto checks
   with the documented staticlib prerequisite. Labels alone do not require byte
   regeneration. The builder preserves reviewed per-file provenance, derives the
   version from the profile and refuses stale or incomplete evidence.

This inventory covers only the published carrier-v1 set. AV vectors and original
source fixtures retain their separate provenance. Its wire-shape evidence is
host-side Multipaz fixture evidence, not an independently confirmed ISO draft or
device-conformance claim. Existing profile uncertainty remains visible; spec §23
item 38 records the unavailable exact ISO revision; the OpenID4VP 1.0 Final
identifier and handover section were checked against the versioned primary HTML.
