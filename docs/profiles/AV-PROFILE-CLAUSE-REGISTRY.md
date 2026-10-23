# AV Profile clause registry

The [machine-readable registry](av/clause-registry.json) accounts for the retained
text of Age Verification Profile Annex A, sections A.1–A.9, at public upstream
commit **`014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`**, committed
**2026-09-01T21:59:02Z**. It records test mappings and explicit exclusions for v1.
It is a traceability inventory, not an executed conformance report or certification.

The source is the European Commission's public
`eu-digital-identity-wallet/av-doc-technical-specification` repository. The commit
and date were checked against its GitHub commit API. The commit identifies the
document revision; no release number is inferred from it. Every mapped test is
marked `partial`: a useful assertion can exist while most of a clause remains
unevidenced. The offline checker does not run those tests or produce a pass for
their clauses. [AV fixtures](AV-PROFILE-FIXTURES.md) add scoped assertions; the
conformance runner and issuance work remain later stories.

## Retained source and attribution

| File | Purpose |
|---|---|
| [av/annex-a-normative.md](av/annex-a-normative.md) | Unchanged 17,899-byte prefix of the upstream file, ending immediately before A.10 |
| [av/source.json](av/source.json) | Full revision/date, canonical URLs, attribution, extraction rule and SHA-256 digests |
| [av/LICENSE.upstream](av/LICENSE.upstream) | Exact upstream license notice: © 2025 European Commission, Creative Commons Attribution 4.0 International |
| [av/clause-registry.json](av/clause-registry.json) | Ordered source blocks, clause dispositions and real test catalogue |

The source's wording, whitespace, references and normative-status marker are
preserved. This registry and this explanatory document are additions. A.10
(comparison) and A.11 (examples) are outside the retained normative excerpt.
Their original bytes are included in the full-source digest, not in the registry's
coverage denominator. A.11 is now retained separately in
[av/annex-a-examples.md](av/annex-a-examples.md) with
[example provenance](av/examples-source.json), without changing that denominator.
No private repository material was imported.

The unchanged excerpt's relative terminology link resolves in its upstream
directory, rather than in this checkout. Use this companion reference map;
the retained source bytes are preserved:

| Source location and relative target | Pinned public upstream target |
|---|---|
| A.2 terminology: `../../../docs/architecture-and-technical-specifications.md` | [Operational, Security, Product, and Architecture Specifications](https://github.com/eu-digital-identity-wallet/av-doc-technical-specification/blob/014dc2fd0f9332eeabe1eef70d234ed2aef20fd7/docs/architecture-and-technical-specifications.md) |

| Digest | SHA-256 |
|---|---|
| Full original upstream file | `54808f94484cb75e7b495b508f0c17568e41b7ca7c3657799c7b432a770538a9` |
| Retained A.1–A.9 prefix | `72af44bd3c04a341b1a49c0ef5bb6d975dc355009f3d381962f6aefd183ba8d3` |
| Upstream license notice | `8e7a9299a281b71fa9ad3f978bcbd5c4d8809b21b2ab51ca5e173e3a16ed6ccb` |

The checker independently pins these values in code, checks the metadata against
them, and hashes the retained excerpt and license bytes. The full original file
is not vendored: offline validation checks its *recorded* digest against the fixed
pin; rechecking all original bytes requires downloading the pinned public source
during a refresh. Changing both metadata and retained bytes cannot silently update
the pin without an explicit change to the checker.

## Schema and source accounting

Both JSON files use `schema_version: 1`. The validator in
[tests/check_av_registry.py](../../tests/check_av_registry.py) defines the accepted
fields and rejects unknown fields and duplicate object keys at every nesting
level in both JSON documents. The registry contains:

- `source_revision`, `source_excerpt_sha256` and `evidence_status`, binding it to
  the retained source and to `mappings only; conformance not executed`.
- `source_blocks`, an ordered inventory derived directly from the retained
  Markdown. Each nonempty paragraph, list item, table row and fenced code block
  has an ID, section, heading anchor, original inclusive start/end lines, exact
  excerpt and SHA-256. Blank lines and Markdown headings provide structure.
  The `(normative)` marker also receives a disposition.
- `tests`, a catalogue keyed by `repository-relative-path::GoTestSymbol`, with
  the actual file/symbol, assertions, limits, manual execution command and build
  prerequisites. The validator checks real Go test declarations after replacing
  comments and literals with whitespace that preserves token separation. Mapped
  files with explicit `//go:build` or legacy `// +build` constraints are
  conservatively rejected: evaluating conditional Go build selection is outside
  the supported reference boundary. Filenames beginning with `.` or `_` and
  implicit OS/architecture suffixes (such as `_windows_test.go`) are also rejected.
  Current mapped files have no such constraints. Supported declarations use an
  unaliased `*testing.T` parameter, including multiline forms with a trailing comma.
  Catalogue files live under `verifier/go/`; commands must select the file's exact
  package and mapped test with `cd verifier/go && go test ./package -run ^TestName$`
  (using `.` for the module's root package). The validator does not execute them.
- `clauses`, ordered by their source position. Each has a stable `AV-A.N-NNN`
  ID (or `AV-ANNEX-NNN` for the document marker), a `block_id`, one or more
  `normative_strength` labels, an exact excerpt, and exactly one disposition.
  Dot suffixes partition a compound block into separate clauses. A source
  location is obtained by resolving `block_id` to `source_blocks`; upstream
  anchors resolve against the pinned file path in `source.json`.

A disposition is either `test: {id, coverage, scope, limits}` or a nonempty
`out_of_scope_for_v1` string beginning `out of scope for v1: `. A mapping references
one catalogue test. `coverage` is `partial` or `full`; all current mappings are
partial. Even a future `full` mapping would describe assertion scope, not an
executed result. Exclusions explain the missing property or the source's
non-obligation status; they are not passes.

`normative_strength` preserves uppercase `MUST`, `MUST NOT`, `SHALL`, `SHALL NOT`,
`SHOULD` and `MAY`, and can label `declarative`, `conditional`, `recommendation`,
`permission` or `informative` text. Labels are editorial classifications; source
wording governs. For example, the proof-property list inherits `SHALL` from its
introductory sentence. Mandatory and conditional table cells remain inventoried
even where the source uses no uppercase keyword. A.1 overview and A.2 terminology
receive explicit section dispositions, as do scope statements and informative
notes.

The checker derives the complete source-block denominator afresh from retained
text. It requires exact equality with the recorded blocks, complete clause
coverage, and exact ordered concatenation of clause excerpts within each block.
Deleting both a block and its clauses therefore fails. So do omitted text,
overlaps, duplicate clause/test IDs, changed order/locations, broken dispositions,
missing test files/symbols and orphan test catalogue entries. This proves
structural accounting. Semantic interpretation, assertion scope and exclusion
honesty still require review.

## What the current mappings establish

There are **86 source blocks, 94 clause rows, nine partial test mappings and 85
explicit exclusions**. Counts include explanatory and non-obligation text, so they
are not a count of independent normative obligations or a coverage percentage.

| Clause | Mapped test | Assertion boundary |
|---|---|---|
| `AV-A.4-002` | `TestAVProfileLocalContainers` | AV fixture identity and request construction; no issuance enforcement or proof verification |
| `AV-A.6-007` | `TestEncryptionInfoShape` | Selected CBOR array/map structure; not every member/type or wire encoding |
| `AV-A.6-008` | `TestDeviceRequestBuild` | Selected CBOR request fields and a byte vector; not the complete inherited ISO schema or AV parameter tuple |
| `AV-A.6-011` | `TestHPKERoundTrip` | Local encryption round trip and tampering rejection with arbitrary plaintext; not a full DeviceResponse |
| `AV-A.7-001.1.1` | `TestGenerateKeyProducesUsableP256` | Local JWK key generation properties; not every AP/AVI/RP |
| `AV-A.7-001.1.2` | `TestJWSSignVerifyRoundTrip` | Local signing/verification primitive; not all profile operations |
| `AV-A.8-002` | `TestAVProfileRequiredParameters` | Actual local allowlist accepts the positive tuple and refuses all five missing/tampered fields; no scheme-owner authority |
| `AV-A.8-003` | `TestAVProfileExternalContainers` | Exact public literals and direct binary decodes, selected outer containers and version-6 tuples; no ciphertext decryption or proof verification |
| `AV-A.8-011` | `TestRegistryRejectsUnlisted` | Local circuit allowlist refusal; not the scheme owner's accepted set or full presentation processing order |

The registry records these material gaps and limits inline:

- The mint adds `issuing_country`; it cannot prove the AV attribute allowlist.
- The Go ISO verifier parses ZK documents. Plain ISO mdoc fallback and the A.9
  prohibition on refusing it solely because it is plain remain unevidenced.
- Android HAIP's `direct_post.jwt` policy differs from A.6's `direct_post`
  requirement. No runtime policy is changed to create a mapping.
- The unofficial OpenID4VP `mso_mdoc_zk` path is outside this profile's ZK
  transport. Its successful tests are not counted as AV conformance.
- AV fixture/container and parameter-tuple assertions are documented in the
  fixture guide; invocation and AVSUA-equivalent issuance remain unevidenced.

The pin verifies Annex A itself. Its unversioned ISO/OpenID/ARF/TS3 references
remain inherited dependencies; this inventory neither expands them into invented
requirements nor asserts independently verified editions. **[UNVERIFIED]**:
external standards publication/edition details, external proof publication
details, and the scheme owner's separately published accepted-circuit set and
authority. These unresolved dependencies are called out in the relevant rows.
Local circuit acceptance does not establish scheme acceptance.

## Running the checks

From the repository root, using only Python's standard library:

```sh
python3 tests/check_av_registry.py
python3 -m unittest discover -s tests -p 'test_av_registry.py'
```

Or use `make test-av-registry`. Success prints mapped/excluded counts and exits
zero. Invalid input prints `AV registry: FAIL` with the offending block, clause,
pin or test and exits nonzero. Mutation tests exercise the coverage, provenance,
disposition and reference failures, including a deleted symbol whose name remains
in a comment. The same two commands run in `make check` and the fast CI job;
changes under `docs/profiles/av/`, this guide and the documentation index trigger
that workflow. Neither check needs a network connection, Go, Rust or cryptographic
dependencies. Executing the catalogue's Go commands is separate; the local
allowlist test needs the Rust staticlib built by `make deps`.

## Refreshing the pin

1. Choose a full revision in the public upstream repository. Verify its commit
   timestamp using the primary Git API, retrieve the original file and `LICENSE`
   at that revision, and record any source/publication claims that remain
   `[UNVERIFIED]`. Do not import private repository copies.
2. Hash the full original bytes. Extract the exact byte prefix before the A.10
   comparison heading, preserving the final preceding blank lines, and hash that
   excerpt. Retain the original license notice and hash it. Review upstream
   attribution/license changes. For the current source, extraction is:

   ```python
   raw = original_path.read_bytes()
   excerpt = raw[:raw.index(b"## A.10. Comparison with other profiles")]
   ```

3. Review the complete source diff, including the A.1/A.2 dispositions, table
   cells, encodings, inherited requirements, recommendations, permissions and
   conditional notes. Reassess the exclusion boundary if upstream renumbers or
   changes the status of A.10/A.11.
4. Update the retained files, `source.json` and the checker's independently
   reviewed `PIN`/extraction rule together, including the attribution text.
   Regenerate the block inventory using
   `source_blocks()` from the checker, then manually reconcile every clause.
   Preserve existing clause IDs for the same requirement; allocate unused IDs
   for new requirements and keep rows in source order. Block IDs are positional
   within the newly pinned source. Review removed IDs and downstream consumers
   explicitly; do not silently repurpose them.
5. Inspect each referenced test's assertions. Update scoped limits and explicit
   exclusions; do not infer new evidence from names, fixtures or test existence.
   Catalogue references must resolve and have at least one clause mapping.
6. Update counts and this guide, run both offline commands and `make check`,
   update the positive test's expected counts, and review the source/registry diff
   together. When updating the supported Go toolchain, review its known OS and
   architecture filename suffixes against `GO_PLATFORM_SUFFIXES` in the checker.
   A future runner must separately
   execute conformance checks and record results; this refresh creates no such
   results.
