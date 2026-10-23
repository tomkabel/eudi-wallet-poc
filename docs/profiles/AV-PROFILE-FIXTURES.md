# AV Profile fixtures

The [fixture set](../../tests/vectors/av-profile/manifest.json) packages the public
AV Profile Annex A containers and the repository's existing minted AV proof and
locally generated requests. The document version is the full public revision
**`014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`**, committed
**2026-09-01T21:59:02Z**, also pinned by the
[clause registry](AV-PROFILE-CLAUSE-REGISTRY.md). No release number is inferred.
Every artifact has a manifest entry recording this profile revision, origin,
source revision/path, byte count and SHA-256.

The data uses document type and namespace `eu.europa.ec.av.1`, system
`longfellow-libzk-v1`, and the five named `ZkSystemSpec` parameters:
`circuit_hash`, `version`, `num_attributes`, `block_enc_hash`, `block_enc_sig`.

| Source | Contents and version | Evidence boundary |
|---|---|---|
| Public A.11, `external/` | All 32 fenced literal bodies, including plain/ZKP DC API requests, encrypted responses, diagnostic forms, transcripts, the plain OpenID4VP example, and illustrative issuance bodies | Exact non-normative examples; retaining illustrative issuance text implements no issuance flow |
| Public encodings, `external/` | Ten binary files decoded directly from original base64url or hex: two DeviceRequests, two EncryptionInfo objects, two encrypted responses, one plain DeviceResponse and three transcripts | Container/byte fidelity; encrypted responses stay opaque |
| Local registry, `local/zk-system-spec.json`, `device-request.cbor`, `dcql.json` | Independently constructed version-7 tuple and requests, compared with runtime Go builders | Local construction and allowlist validation |
| Existing Multipaz fixture, `local/device-response.cbor`, `proof-request.json`, `proof-provenance.txt` | Verbatim copies from `verifier/go/zk/testdata/step8-7-openid4vp-zk/` | Previously minted AV proof bound to `OpenID4VPHandover`; copying does not make a DC API proof |

The public ZKP request retains four **version-6** tuples, with 1–4 attributes.
The local accepted tuple is **version 7**, one attribute, block sizes **4151/4096**,
hash `8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121`.
The local source snapshot is full revision
`cc112f6a0a7705e98fad38b9eac83ba0bb55d96c`; the builder independently pins the
original circuit-registry and minted-fixture hashes. These local fixtures establish
no scheme-owner circuit approval. **[UNVERIFIED]**: scheme-owner accepted-set
publication/authority and the external standards editions referenced by Annex A.

The copied proof's DeviceResponse version is `1.1`; the external examples use
`1.0`. These container versions differ from the profile document revision and
the circuit version. The manifest keeps the profile, circuit and source versions
separate. The locally constructed request uses the runtime's `zkRequired: true`
and adds its compatibility `id` alias and `zkRequest.version`; the unchanged
upstream ZKP request has `zkRequired: false` and neither addition.

## Retained source

[av/annex-a-examples.md](av/annex-a-examples.md) is the unchanged **1,111,842-byte**
suffix of the public source, from `## A.11. Examples` through EOF (source byte
offsets `[20029, 1131871)`). [av/examples-source.json](av/examples-source.json)
records extraction, attribution, full revision and digests. Its SHA-256 is
`c907940c18a291fce6b231bc2419fc25433218d89368553582771d695e28afe2`.
The complete upstream file's SHA-256 remains
`54808f94484cb75e7b495b508f0c17568e41b7ca7c3657799c7b432a770538a9`.
The original full file is not vendored; offline checks pin its recorded digest
and hash the retained suffix and license bytes.

The source is © 2025 European Commission under Creative Commons Attribution 4.0
International; [av/LICENSE.upstream](av/LICENSE.upstream) retains its exact notice.
The examples are unchanged; fixture packaging, manifest and commentary are
additions. No private repository material was imported. The A.1–A.9 normative
prefix and the registry's source denominator remain intact; A.11 adds no
normative clauses.

## Rebuilding and checking

From the repository root, using only Python's standard library:

```sh
python3 tools/build_av_fixtures.py          # deliberately write deterministic output
python3 tools/build_av_fixtures.py --check  # read-only, including manifest and complete file inventory
python3 -m unittest discover -s tests -p 'test_av_fixtures.py'
cd verifier/go && go test ./oid4vp -run '^TestAVProfile'
```

`make test-av-fixtures` runs the two read-only Python checks. They also run in
`make check` and fast CI; the Go fixture tests run with that job's package tests.
No network, key generation, Rust staticlib or random proof generation is needed.

The copied `proof-provenance.txt` retains its original `consumed by` line
verbatim. That line names `verifier/go/zk`, but the existing
`TestStep87OpenID4VPZkVerify` test resides in
[`verifier/go/step8_7_openid4vp_zk_test.go`](../../verifier/go/step8_7_openid4vp_zk_test.go),
package `main`. With the Rust staticlib available from `make deps`, run it using
`cd verifier/go && go test . -run '^TestStep87OpenID4VPZkVerify$'`.
This existing test verifies the copied proof's OpenID4VP binding.

The builder copies exact fenced body bytes, decodes binary strings directly and
copies the existing minted CBOR verbatim. It never decodes and reserializes a
golden CBOR fixture. Only the new local request uses its small CBOR encoder;
Go independently checks its complete bytes against runtime `DeviceRequest`.
`--check` rebuilds in memory and fails on source/provenance pin changes, changed
output bytes, changed manifest, or missing/extra files. It writes nothing on
success or failure. Deliberate rebuilds refuse unexpected files instead of
silently deleting them.
Both checking and generation refuse symlinks at `tests`, `tests/vectors` and
`tests/vectors/av-profile` below the checkout root; generation also refuses
individual output-file symlinks.

Five literal bodies retain upstream trailing blank lines. Their narrow
`.gitattributes` rules disable only Git's blank-at-EOF warning; the byte drift
check still enforces every byte, including those lines.

The same file marks `docs/profiles/av/**`, `tests/vectors/av-profile/**`,
`verifier/circuits.json` and the three pinned original minted inputs
(`device_response.cbor`, `request.json`, `GENERATED-BY` under
`verifier/go/zk/testdata/step8-7-openid4vp-zk/`) as `-text`. Git preserves their
committed bytes without line-ending conversion, including with `core.autocrlf=true`,
so a fresh checkout retains the source, input and output hash pins. The five
blank-at-EOF whitespace rules remain in effect.
CBOR and binary transcript fixtures also have Git's `binary` attribute so
NUL-free encodings appear as binary patches in reviews.

To refresh the public source, follow the registry's primary-source verification
procedure, re-extract the exact suffix, and review the fixed provenance/digest
pins in `tools/build_av_fixtures.py` alongside the retained bytes and metadata.
Review `LITERALS` if fences change. A local circuit/proof refresh similarly
requires reviewing the full source revision and `LOCAL_PINS`. Rebuild, inspect
the complete manifest and byte diff, then run `make check`. Do not use fixture
regeneration to overwrite an unexpected change. No command here regenerates
cryptographic proofs or device keys.

## Executed assertion boundaries

`TestAVProfileLocalContainers` checks the AV identity, namespace and age predicate
in local requests and the copied minted document; all five tuple values; exact
runtime request compatibility; and retained OpenID4VP proof provenance.
`TestAVProfileRequiredParameters` accepts the local positive tuple and refuses
each of the five missing and five tampered fields through the actual verifier
allowlist, retaining the original ID so a tuple field cannot hide behind it.
It also refuses missing offers, empty or unrelated accepted sets, and malformed
JSON parameter representations at decoding or validation. Existing carrier
policy accepts the legacy `org.iso.mdoc.zk` alias; the compatibility assertion and
registry limits make that exception explicit without claiming AV system exclusivity.
`TestAVProfileExternalContainers` independently extracts literal bytes, checks
all ten binary decodes, checks selected outer container fields, and keeps all
four external version-6 tuples distinct from the local version-7 accepted set.

The Python corruption controls exercise source, local inputs, provenance,
manifest, output bytes and inventory failures, including CLI read-only behavior.
The registry maps the three Go tests partially to `AV-A.4-002`, `AV-A.8-002` and
`AV-A.8-003`. Removing a clause fails source accounting; retaining its text as an
exclusion while dropping its mapping fails with an orphan catalogue reference.
Registry validation checks traceability and does not execute tests.

These checks establish neither upstream ciphertext decryption nor external proof
verification. The copied minted proof uses the unofficial OpenID4VP ZK carrier;
it supplies no AV DC API transport conformance evidence. Full inherited ISO/proof
schemas, issuance, plain fallback, operator validation, certification and the
conformance runner remain outside this fixture work.
