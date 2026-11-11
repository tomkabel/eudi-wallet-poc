# Multipaz host target evidence — 2026-11-11

The partial external compatibility result is **FAIL**: the exact CBOR carrier vector parsed and its native proof verified successfully; the exact interim JSON carrier bytes failed the target CBOR parser with `java.lang.IllegalArgumentException: Out of bounds decoding data`. The Gradle process succeeded. All 19 flow cases remain **NOT_EXECUTED**, explicitly inventoried in `host-vectors.json`; this is not a full carrier-suite PASS, independent operator evidence, or certification.

Target: public Multipaz 0.99.0, revision `7486ddbca490038c3580513d8273b9bbf18c6b70`. The rationale is its existing fixture relationship and attributable independent implementation. `host-vectors.json` records the full target metadata, exact input/source SHA-256 hashes, suite source revision/dirty state, and successful before/after canonical target revision, origin URL and clean-source checks.

## Actual observations

- `host-proof.json` / `host-proof.md` retain the original successful upstream host self-test. It generated a fresh 323,708-byte proof and native verification returned code 0; `upstream-SystemTest.xml` reports one test, zero failures/errors. This uses upstream test inputs, exercises zero repository flow assertions and proves this host can run the native target library. The original attempt predates the stronger report classifier and does not itself record an origin check.
- `host-vectors.json` / `host-vectors.md` retain the final forced vector feed with separate `process_status=PASS`, `compatibility_result=FAIL`, and `result=FAIL`. `vector-results.json` holds the two actual observations; `host-vectors-test.xml` proves one test body executed. The fixture transcript is passed directly into the native proof verifier.
- `host-vectors-attempt-1.*` preserves the initial Gradle init-script error in an included build. `host-vectors-attempt-2.*` preserves a 120-second configuration timeout. Both executed zero vector assertions. `host-vectors-attempt-3.*`, `vector-results-attempt-3.json` and `RepositoryCarrierVectorTest-attempt-3.xml` retain the first actual vector feed; its historical top-level PASS means process success only and is superseded by the final assertion-derived FAIL.

The Kotlin harness sends the exact base64url-decoded published carrier bytes through the unmodified target's public `Cbor.decode` and `DeviceResponse.fromDataItem` APIs. It neither wraps interim JSON in CBOR nor claims that this target API is a general carrier dispatcher. The interim failure demonstrates unsupported input at this concrete parser surface; it does not establish absence of every possible target integration. For the CBOR input, it selects the target's advertised matching circuit and invokes `LongfellowZkSystem.verifyProof`.

## Reproduce

From this repository root, with the reviewed target at `/tmp/eudi-carrier-multipaz-0.99.0`:

```sh
python3 docs/profiles/carrier/external-host-2026-11-11/run-host-proof.py --vectors
python3 docs/profiles/carrier/external-host-2026-11-11/run-host-proof.py
python3 docs/profiles/carrier/external-host-2026-11-11/test_host_evidence.py -v
```

The harness adds an outside-checkout JVM test source through an external Gradle init script and never changes target source. It uses offline Gradle configuration on demand, a 600-second bound and 4 MiB retained output through `tests/run_external_carrier.execute`. The `jvmTest` task is forced to execute; old XML and vector evidence are removed before each attempt. Successful exact provenance, one fresh successful XML test, and two unique complete valid vector rows are required. Any vector FAIL makes the compatibility result FAIL; missing/malformed evidence, nonzero process status, or unavailable/mismatched provenance is ERROR. Six synthetic controls cover provenance, missing/duplicate rows, assertion failure despite process success, absent fresh test execution, and nonzero process outcomes.

Presentation trims trailing whitespace; JSON retains original command output. Fenced and inline Markdown use the existing external runner's safe renderers. The shared command runner bounds its process group; this is not a cgroup isolation claim.

## Remaining scope

The full 19-case flow mapping, request/credential injection, response capture, replay/JWE/multicredential controls and target-aware carrier metadata are further agent-doable engineering if a later task requires a full-suite PASS. They are not prerequisites to publishing this honestly failed partial observation. Native CBOR must never be relabelled as interim JSON, and local verifier/session policy must not be attributed to the target wallet.

Human/external work remaining is maintainer publication and independent operator reproduction. No unavailable device/provider prerequisite is established here.
