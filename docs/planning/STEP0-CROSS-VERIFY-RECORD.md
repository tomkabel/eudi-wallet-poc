# Step 0 record: the two ZK halves cross-verify

Plan: `EUDI-WALLET-POC-CONFORMANCE-PLAN.md` §4 step 0, finding F1. Date: 2026-09-23.

## What was run

The fork's `:zk-conformance` JVM module (`Step0CrossVerifyTest`, multipaz-longfellow-jvm
0.99.0) proved `age_over_18` over an `eu.europa.ec.av.1` mdoc and wrote a fixture to
`verifier/go/zk/testdata/step0-multipaz/` in this repository. The Go test
`verifier/go/zk/step0_cross_verify_test.go` verifies that proof through
`zk.Verify` (the Rust Longfellow runtime via zkverify-ffi), after a registry check.
The reverse direction uses the fixture the Rust `ee_poa_demo` prover wrote to
`testdata/step0-rust-prover/`, verified by the same JVM module with multipaz's
own `verifyProof` ("verify done: ok" in the Gradle log).

## Result

Both directions verified:

- multipaz 0.99.0 proof → Rust runtime (`TestStep0MultipazFixture`): PASS (6.0 s
  `go test` wall time: registry check plus two `Verify` calls, including the rejected
  one-bit flip; not an `EE-ZKP-040` latency figure).
- Rust `ee_poa_demo` proof → multipaz (`verifyEePoaDemoProofWithMultipaz`): PASS,
  "verify done: ok"; tampering rejected ("merkle_check failed", Verification Code 5).

Control: `TestStep0RustProverFixture` (Rust proof through this repo's own verifier)
passes, so a cross-direction failure would have pointed at the builds, not at the
harness. F1 is resolved: **the two halves are compatible. No incompatibility to
report.**

## One harness defect found and fixed

The first run failed with `zk: proof did not verify: GeneralFailure` — not a
Longfellow build incompatibility but an empty timestamp input: the multipaz fixture
writes the field as `timestamp`, the Go loader read `now`. The loader now accepts
either key (`step0_cross_verify_test.go`, `loadFixture`). Recorded because the plan
(§4 step 0) attributes any failing direction to the Longfellow builds, and a shallow
input-shaping bug looks exactly like a cross-runtime failure until the inputs are
compared side by side.

## Circuit inventory (F1, all eight now paired with hashes)

From `multipaz-circuits.txt` (multipaz-longfellow-jvm-0.99.0 bundled systemSpecs):

| Label | Version | Attrs | Circuit hash |
|---|---|---|---|
| longfellow-libzk-v1_6_1_4096_2945 | 6 | 1 | 137e5a75…97c7c6 |
| longfellow-libzk-v1_6_2_4025_2945 | 6 | 2 | b4bb6f01…a9764e |
| longfellow-libzk-v1_6_3_4121_2945 | 6 | 3 | b2211223…6c2010 |
| longfellow-libzk-v1_6_4_4283_2945 | 6 | 4 | c70b5f44…23c49d6 |
| longfellow-libzk-v1_7_1_4151_4096 | 7 | 1 | 8d079211…6182121 |
| longfellow-libzk-v1_7_2_4265_4096 | 7 | 2 | 6a581068…dbbcd5ad |
| longfellow-libzk-v1_7_3_4307_4096 | 7 | 3 | 8ee4849a…7d6d252f |
| longfellow-libzk-v1_7_4_4415_4096 | 7 | 4 | 5aebdaaa…fb278e460 |

`circuits.json` lists only version 7 / 1 attribute; version 6 is not accepted.
A relying party running this verifier rejects multipaz proofs made under version-6
circuits or multi-attribute circuits — that is the EE-ZKP-030 allowlist behaving as
specified, not a defect.

## Where things live

- Fixture + provenance: `verifier/go/zk/testdata/step0-multipaz/` (committed),
  `GENERATED-BY` records multipaz version and the generating command.
- Rust-prover fixture: `verifier/go/zk/testdata/step0-rust-prover/` (committed).
- Go test: `verifier/go/zk/step0_cross_verify_test.go` (committed on branch
  `step0-cross-verify`).
- Fork side: `tomkabel/eudi-wallet-poc` commit "Add the step 0 cross-verify test
  against ee-eudiw's verifier" (`zk-conformance/` `build.gradle.kts` systemProperty
  forwarding + `Step0CrossVerifyTest.kt`), first committed as `68acbc2` and since
  rebased as `fd56b59` on branch `fixture-device-response`, with identical
  `zk-conformance/` content. It was committed about four minutes after the fixture's
  timestamp, so that the generating working tree equals that commit is
  [UNVERIFIED]. At that commit `build.gradle.kts` hardcodes this machine's sibling
  path as a default; the follow-up "Default the step 0 fixture paths to the sibling
  ee-eudiw checkout" resolves it from the fork's root instead, and the
  `-Dstep0.eeEudiw=` override works for any other layout. Commits destined for an
  upstream PR get DCO sign-off per plan D2.
- Transcript: the subagent driving the first pass died on a model-provider outage;
  its Gradle log survives in the fork's test output and this record.
