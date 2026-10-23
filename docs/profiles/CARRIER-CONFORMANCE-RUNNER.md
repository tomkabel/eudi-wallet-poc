# Carrier conformance runner

## Coverage model

The runner reports two separate layers:

1. `tests/e2e.sh` retains its 13 existing cryptographic, transport and negative
   assertions. Each assertion emits one machine-readable case record with its
   case id, path label, observed carrier (or `NOT_OBSERVED` when the assertion
   has no presentation bytes), response mode, outcome and result. Carrier and
   response mode are independent fields. In particular, `direct_post.jwt`
   describes encrypted response transport; it does not imply a carrier.
2. Go tests parse and dispatch carriers, then
   `TestCarrierVectorsVerifyIdentically` checks the two committed encodings
   against the same proof and transcript. This vector layer is where both
   encodings are covered. The E2E suite does not claim a 13-case cross-carrier
   matrix.

The E2E assertions currently use the OpenID4VP interim PoC carrier and the
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
pip install -r tests/requirements-ci.txt
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

## Current external evidence state

As of 2026-10-23, external evidence is `BLOCKED`. No independent target or
revision has been selected or run against this suite; target selection and the
independent run belong to Story 2.4. The JSON report repeats this state with
its generation date. This state is separate from local suite pass/fail results.

## Evidence boundary

These commands execute this repository's wallet, verifier, prover and checked-in
fixtures. They do not establish independent-wallet behavior, third-party
verifier behavior, scheme-owner acceptance or external conformance. No result
from this local runner substitutes for the independent implementation and
published evidence required by Story 2.4.
