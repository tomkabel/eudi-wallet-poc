---
title: '1.2 AV-profile fixtures'
type: 'feature'
created: '2026-10-23'
status: 'done'
baseline_revision: cc112f6a0a7705e98fad38b9eac83ba0bb55d96c
review_loop_iteration: 0
followup_review_recommended: false
context: []
warnings: [oversized]
deferred: []
---

<intent-contract>

## Intent

**Problem:** Existing AV proof vectors lack a profile-version-labelled fixture set connecting Annex A containers and all five named ZkSystemSpec parameters to executable validation. Without byte drift and clause-reference checks, changed encodings or removed clauses could still appear as evidence.

**Approach:** Retain the pinned public Annex A examples, deterministically extract their literal containers and package the existing minted AV proof plus locally generated AV requests with version/provenance metadata. Exercise all required parameters and preserve committed CBOR bytes; register narrowly scoped fixture assertions against the pinned normative clauses.

## Boundaries & Constraints

**Always:** Use `eu.europa.ec.av.1`, `longfellow-libzk-v1` and `circuit_hash`, `version`, `num_attributes`, `block_enc_hash`, `block_enc_sig`. Label every artifact through a manifest with full source revision and origin. Preserve external example bytes and existing minted proof CBOR verbatim; compare rebuilt output with committed bytes. Keep public examples' version-6 tuples distinct from the local version-7 accepted tuple. Scope tests to what they execute. Preserve existing registry provenance and source denominator. Leave `sprint-status.yaml` entirely under orchestrator ownership, including concurrent changes.

**Never:** Import private repository content; commit device keys or `demo-data/`; claim synthetic containers are newly generated cryptographic proofs; reserialize decoded golden CBOR; imply scheme-owner circuit approval, certification, successful decryption of upstream encrypted examples, or AV conformance for the unofficial OpenID4VP ZK carrier. Do not implement the conformance runner, issuance, or operator-only work from later stories.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|---|---|---|---|
| Rebuild | Retained pinned source, local circuit and minted proof | Identical complete fixture files and version-labelled manifest | Exit 0 |
| Drift | Changed fixture bytes, manifest, source or missing/extra output | Read-only drift check identifies mismatch | Nonzero exit |
| Tuple validation | Positive AV tuple or each missing/tampered field | Accept local positive tuple; refuse every missing/tampered field using verifier allowlist | Test fails on unexpected acceptance |
| Clause removal | Catalogue test loses its pinned clause mapping | Registry validation reports orphan reference or missing source coverage | Nonzero exit |

</intent-contract>

## Code Map

- `_bmad-output/planning-artifacts/epics.md:381` — Story 1.2 intent/criteria, read-only; prior spec `spec-1-1-av-profile-clause-registry.md` supplies continuity.
- `docs/profiles/av/source.json` and `annex-a-normative.md` — pin public revision `014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`, full SHA-256 `54808f94484cb75e7b495b508f0c17568e41b7ca7c3657799c7b432a770538a9`; normative prefix intentionally excludes A.10/A.11. Public full source verified in `/tmp/av-annex-a-public-014dc2f.md`; A.11 is non-normative and contains literal DC API plain/ZKP requests, encrypted responses, diagnostic responses/transcripts and OID4VP examples. Retain separately with exact extraction and digest.
- `tools/build_cbor_fixture.py` — established byte-preservation pattern; `zk_document_entry_bytes` copies encoded entry, never round-trips golden CBOR. `verifier/go/zk/testdata/step8-7-openid4vp-zk/device_response.cbor` is already minted by Multipaz for AV with an OpenID4VP transcript; copying it does not establish DC API proof validity.
- `verifier/circuits.json` — local version-7 tuple, one attribute, block sizes 4151/4096 and hash `8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121`; upstream examples contain four version-6 tuples. Neither is scheme-owner authority.
- `verifier/go/oid4vp/isodcapi.go` — `DeviceRequest` generates AV CBOR with nested params; `dcql.go` — `ZkSystemTypeAllowlist` compares every named parameter to accepted circuits. Missing numerics decode to zero and fail against current nonzero tuple.
- `verifier/go/oid4vp/carrier_test.go` — existing independent wrapper drift checks; `isodcapi_test.go` demonstrates cborsub decoding. Fast package tests need no Rust link.
- `tests/check_av_registry.py` — validates real Go symbols, canonical commands and both dangling and orphan catalogue references; `tests/test_av_registry.py` has orphan mutation control. Preserve counts dynamically when adding tests. `AV-A.4-002`, `AV-A.8-002`/`003` need reviewed partial mappings if assertions cover them.
- `Makefile`, `.github/workflows/ci.yml` — fast Go packages and offline registry already run; add read-only builder check consistently. `tests/requirements-ci.txt` pins CBOR dependencies if needed; prefer stdlib-only extraction.

## Tasks & Acceptance

**Ownership:** The implementation worker owns all files/tasks below and may update this spec’s checklist. You are not alone in the codebase: preserve others’ edits and adapt to concurrent changes. Never edit or revert `sprint-status.yaml`.

**Execution:**
- [x] `docs/profiles/av/annex-a-examples.md` and example provenance metadata — retain exact A.11 public bytes from pinned source with digest/extraction/attribution, without altering normative prefix.
- [x] `tools/build_av_fixtures.py`, `tests/vectors/av-profile/` — deterministic complete fixture set/manifest, exact external containers (including binary decoded base64url), copied minted AV CBOR, local AV request and tuple; provide read-only `--check` drift mode including manifest and file inventory.
- [x] `verifier/go/oid4vp/av_profile_test.go` — independent container/doctype/system/tuple assertions, external raw-byte checks and all five missing/tampered-parameter negative controls using actual allowlist. Assert generated request compatibility with runtime `DeviceRequest`; accurately distinguish carrier and provenance.
- [x] `tests/test_av_fixtures.py`, `tests/test_av_registry.py` — meaningful source/output/manifest corruption controls and explicit removal-to-orphan regression; no golden CBOR reserialization.
- [x] `docs/profiles/av/clause-registry.json` — register real Go fixture tests with explicit partial coverage limits; retain original source clauses/order and honest exclusions.
- [x] `Makefile`, `.github/workflows/ci.yml`, `docs/profiles/AV-PROFILE-FIXTURES.md`, `docs/README.md`, `tests/README.md` — wire checks into fast build, document regeneration, exact version/origin and limits, index docs/spec.

**Acceptance Criteria:**
- Given the pinned AV Profile, when fixtures are built, then the set includes Annex A's external container examples and AV-specific minted data/requests carrying all five profile parameter names, with every artifact version-labelled through its manifest.
- Given a clean checkout, when the documented read-only checks and fast CI run, then regenerated bytes match every committed artifact and the tuple is exercised against actual verifier validation, without reserializing decoded CBOR fixtures.
- Given a missing/tampered required parameter or modified fixture/provenance/manifest, when checks run, then the defect causes a failing check rather than successful evidence.
- Given registry test IDs and pinned clause mappings, when a mapped clause is removed, then build validation refuses dangling/orphan evidence.

## Spec Change Log

- 2026-10-23: Corrected the code-map clause references: the pinned source has no
  `AV-A.9-006` or `AV-A.9-007`. The implemented assertions map partially to the
  existing `AV-A.4-002` (document type), `AV-A.8-002` (system/parameters) and
  `AV-A.8-003` (container structure). No source clause or ID was added, removed,
  renumbered or reordered.

## Review Triage Log

### 2026-10-23 — Review pass

- verdicts: 11 findings — high 0, medium 3, low 5, false 3, maybe-false 0
- findings (layer report order: verification gap, then blind hunter; edge returned no findings and intent audit was descriptive):
  - `[medium]` `[patch]` Verification: writing CLI can stop writing while tests remain green — the reviewer removed `write_bytes` in an isolated checkout and all nine original controls passed. Added a subprocess regression that deletes the output tree, runs the documented no-argument command, compares every output byte/inventory and then requires `--check` success.
  - `[low]` `[patch]` Blind: a symlinked output root is followed — independently reproduced `check(root) == 49` through a symlink to matching files; generation likewise followed the root. Added a direct output-root symlink refusal to both check/write paths and a regression ensuring the external target remains unchanged.
  - `[medium]` `[patch]` Blind: automatic line-ending conversion corrupts clean-checkout pins — independently reproduced CRLF changes with `git -c core.autocrlf=true checkout-index`. Added narrow `-text` rules for retained source, generated fixtures and pinned local inputs, preserving exact upstream blank lines and their five narrow whitespace exceptions. CI now also triggers on attribute changes. An isolated converted checkout preserves all 58 checked source/input/output paths and passes the actual CLI check.
  - `[low]` `[patch]` Blind: counts do not prove unique artifact paths/fence coverage — the old Go loop permitted repeated manifest entries to replace an omitted literal while preserving counts (the Python byte gate still caught manifest edits). Added explicit duplicate-path/fence rejection and complete fence coverage from 1 through 32.
  - `[false]` `[reject]` Blind: external EncryptionInfo requires new semantic/runtime validation — the story requires inclusion and byte fidelity; all ten direct binary decodes are checked, and the registry explicitly claims selected outer structures rather than complete inherited ISO schemas or runtime interoperability for every example. The immutable source/digests and exact base64 comparisons disprove silent corruption of the included container. No complete external EncryptionInfo semantic result is claimed.
  - `[false]` `[reject]` Blind: transcripts must be recomputed through runtime binding — retained transcript hex is independently decoded and compared byte-for-byte, while the entire source is pinned. The mappings claim selected outer container checks and expressly leave end-to-end DC API operation unevidenced; the stated missing full binding is not a claimed or required fixture-inclusion result.
  - `[low]` `[patch]` Blind: external system ID comparison only checks its prefix — confirmed the old condition did not establish internal tuple-ID consistency. The assertion now reconstructs the complete ID from system, version, attribute count, both block sizes and hash and compares exact equality.
  - `[low]` `[patch]` Blind: malformed tuples only tested as sole entries — confirmed current runtime loops over every entry, but the new fast fixture controls lacked a selective-later-parameter regression oracle (the existing integration test checks mixed unknown circuits). Every mutation now runs alone, after a valid tuple and before a valid tuple through the actual allowlist.
  - `[low]` `[patch]` Blind: identity omissions/mismatch lack fixture controls — runtime already rejects these states, but the new fixture controls covered only unknown system. Added missing/tampered system and ID controls, including both mixed-offer orders, without changing runtime policy.
  - `[false]` `[reject]` Blind: fixed version-7 labels can silently disagree with the circuit — every local source byte is independently pinned before the tuple is read, and Go separately asserts the exact version-7 tuple/manifest. Changing the circuit version fails the pin; a deliberate source refresh must review pins, versions and independent assertions as documented. The current builder cannot emit the proposed inconsistent state from an accepted input.
  - `[medium]` `[patch]` Blind: no executable writing-CLI coverage — same verified root cause as the verification-gap finding; fixed by the same absent-output subprocess regression.
- Grouped patches: seven entries — medium 2, low 5; eight finding rows patched. Rejected findings: three false requirement/refresh claims. Deferred entries: zero.
- All four review layers completed before triage. The intent auditor confirms the implementation supplies deterministic fixture preparation/local validation, preserves the orchestration constraints and requires no human-only action. A stronger end-to-end native DC API reading is beyond the fixture story's explicit inclusion/version/drift/reference criteria and belongs to executed presentation evidence; no such evidence is claimed here.
- The concurrency limit permitted three initial reviewers; the fourth launch was retried when the first reviewer finished. No layer was skipped.

### 2026-10-23 — Follow-up review pass

- verdicts: 12 findings — high 0, medium 2, low 10, false 0, maybe-false 0
- findings, in layer report order:
  - `[low]` `[patch]` Verification gap: individual output-file symlink refusal had no regression control — the reviewer's isolated deletion of the guard left all eleven prior fixture tests green and allowed an external target to be overwritten. Added a subprocess control requiring nonzero exit, preservation of the symlink, and unchanged external target bytes and modification time.
  - `[medium]` `[patch]` Blind: an output ancestor symlink redirects generation — reproduced with `tests/vectors` pointing outside the output tree: checking returned 49 and generation replaced an external marker. Extended the existing directory refusal to every output-path component below the trusted checkout root; added read-only and writing-CLI controls for both `tests` and `tests/vectors`.
  - `[low]` `[reject]` Blind: a late invalid destination can leave earlier fixture outputs rebuilt — reproduced a late directory failure after the first output was rebuilt. Those earlier writes are the requested deterministic rebuild, and the invalid destination fails visibly; this rare manually altered output layout does not justify adding preflight/transactional write machinery to the regeneration command. Only `--check` promises no writes on failure.
  - `[low]` `[reject]` Blind: an expected fixture path replaced by a FIFO can block generation — reproduced with an explicitly created FIFO and a bounded subprocess timeout. This requires replacing a committed regular fixture with a special device-side filesystem object, is unlikely in everyday fixture work, and would require additional file-type guards; rejected under the workflow's low-finding rule.
  - `[low]` `[patch]` Blind: NUL-free CBOR/binary data is emitted as undecodable text in a normal Git diff — the initial review diff failed UTF-8 decoding at byte 1184282 despite `--binary`. Added narrow `binary` attributes for fixture CBOR/transcript binaries; an ordinary Git diff now decodes as UTF-8 without temporary attribute overrides.
  - `[low]` `[patch]` Blind: the partial system mapping omits the existing legacy alias exception — confirmed current carrier policy accepts `org.iso.mdoc.zk` with the valid tuple. Added a compatibility assertion and explicitly recorded the exception in both matching registry limits and the guide; no runtime policy was changed or AV system exclusivity claimed.
  - `[low]` `[patch]` Blind: a constant local-tuple implementation could ignore the supplied accepted set — all prior calls used the same populated set, while the runtime contract also serves callers with filtered offers. Added refusals for nil, empty and unrelated accepted sets so a hardcoded tuple cannot satisfy the fixture controls.
  - `[low]` `[patch]` Blind: complete missing offers lack direct fast allowlist controls — the existing integration control exercises `Single`, while this suite previously supplied a populated list to every direct call. Added direct refusals for nil metadata, an absent list and an empty list.
  - `[low]` `[patch]` Blind: malformed JSON parameter representations lack fixture controls — current typed decoding or allowlisting refuses null, booleans, negative, fractional and uint32-overflow representations, but those paths had no fixture assertions. Added all five representations for each required parameter, accepting refusal at either decoding or validation.
  - `[low]` `[reject]` Blind: manifest identifies the generator path but not its own digest/revision — confirmed the recorded baseline revision identifies the pinned data inputs, predating the generator. The documented consumer rebuilds using the generator in the checked-out repository and compares exact committed output; manifest-only recovery of a generator is not that consumer. Additional generator-identification metadata and its refresh bookkeeping are not justified by an observed drift escape in everyday use.
  - `[low]` `[patch]` Blind: copied provenance names the wrong package for the existing proof test — confirmed `TestStep87OpenID4VPZkVerify` resides in `verifier/go/step8_7_openid4vp_zk_test.go`, package `main`. Preserved the copied provenance bytes and added the correct source link, prerequisite and executable command to the guide; ran that exact test successfully.
  - `[medium]` `[patch]` Edge: a symlink at an output ancestor bypasses the direct-root guard — same independently reproduced boundary as the blind ancestor finding. Fixed by the same output-component refusal and both ancestor regression layouts.
- Grouped patches: eight entries — medium 1, low 7 — covering nine finding rows. Rejected: three low findings for the reasons above. No deferred entries were added, inspected, reopened or changed.
- All four review layers completed before triage. The intent audit describes fixture preservation/local validation as the implemented reading; full native AV semantics are not claimed. The fourth layer launched after a concurrency slot freed. The edge layer's initial decode failure was resolved by rendering actual binary patches, and it then completed its full review.

## Design Notes

The examples demonstrate container formats, not normative circuit acceptance. Their literal version-6 tuples remain intact even though local minted AV data uses version 7. Repackaging the existing real proof satisfies fixture preparation without new keys, random proof generation or timestamp drift; its OpenID4VP binding is explicit. No human-only external actions are required.

## Verification

**Commands:**
- `python3 tools/build_av_fixtures.py --check` — complete committed-byte and provenance check, no writes.
- `python3 -m unittest discover -s tests -p 'test_av_fixtures.py'` — corruption/drift controls pass.
- `cd verifier/go && go test ./oid4vp -run '^TestAVProfile'` — outer fixture assertions and parameter refusals pass.
- `make check` — existing fast baseline plus fixture and registry checks pass.
- `git diff --check` — no whitespace errors.

## Implementation Result

Implemented the fixture preparation and narrowly scoped executable assertions.
The retained A.11 suffix is exactly 1,111,842 public source bytes with independent
digest/extraction/attribution metadata. The complete upstream source digest was
rechecked, and both the A.11 suffix and unchanged normative prefix equal their
original source slices. No private source or device keys were used.

The deterministic set contains 49 files: 32 exact fenced literal bodies, ten
direct binary base64url/hex decodes, six local artifacts and the manifest. Every
artifact records the full profile/source revisions, origin, hash and byte count.
External version-6 tuples remain unchanged; the local version-7 tuple, requests
and copied minted proof are explicitly distinct. The builder uses only stdlib,
copies golden CBOR without reserialization, and checks complete output inventory,
manifest and source/provenance pins without writing. Provenance validation also
distinguishes JSON booleans from numeric values.

Three new Go tests independently check external literal/binary bytes and outer
containers, AV identities and nested tuple values, runtime request compatibility,
and actual allowlist behavior. All five missing and five tampered parameter
controls fail closed; unknown systems and the four external version-6 tuples
are refused against the local accepted set. Nine Python fixture tests cover
corruption and read-only behavior. The registry retains all 86 source blocks and
94 clauses, with nine partial mappings, 85 explicit exclusions and nine real Go
test references. Its 37 controls include explicit removal-to-orphan regression.

The Makefile and fast CI run read-only fixture and corruption checks; existing
fast Go package tests execute the Go assertions. The fixture guide documents
rebuilds, exact versions/origins and limits; documentation/test indexes and the
existing registry guide reflect the added assertions.

Verification completed:

- `python3 tools/build_av_fixtures.py --check` — passed; all 49 files match.
- `python3 -m unittest discover -s tests -p 'test_av_fixtures.py'` — passed; nine tests.
- `cd verifier/go && go test ./oid4vp -run '^TestAVProfile'` — passed.
- `python3 tests/check_av_registry.py` — passed; 86 blocks / 94 clauses / nine mappings / 85 exclusions / nine symbols.
- `python3 -m unittest discover -s tests -p 'test_av_registry.py'` — passed; 37 tests.
- `make check` — passed, including Go race tests and all existing fast checks.
- After the final provenance-type/manifest-label refinement, reran the affected
  builder drift check, fixture corruption suite and Python lint; all passed.
- `git diff --check` — passed.

No implementation tasks remain. The documented scope excludes new cryptographic
proof generation, upstream ciphertext decryption/proof verification, scheme-owner
accepted-set approval, certification, AV conformance for the unofficial OpenID4VP
ZK carrier, issuance and the later conformance runner. The existing minted proof
retains its OpenID4VPHandover binding. `sprint-status.yaml` was not edited.

## Auto Run Result

Status: done

Completed the requested follow-up review of Story 1.2 against full baseline
`cc112f6a0a7705e98fad38b9eac83ba0bb55d96c`. The implementation retains all
49 deterministic fixture files and their committed bytes: 32 literal Annex A
bodies, ten direct binary extractions, six local artifacts and the manifest.
The public profile revision is `014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`,
committed `2026-09-01T21:59:02Z`; its source digest, exact A.11 extraction,
full revision and date were revalidated against the public primary source.
All four local pinned inputs were independently compared with their recorded
Git baseline bytes. No private source or device keys were used.

Files changed in this follow-up:

- `tools/build_av_fixtures.py` — refuse symlinked output-path components below the checkout root for checking and generation.
- `tests/test_av_fixtures.py` — add ancestor and individual-file symlink regressions, bringing the fixture suite to thirteen tests.
- `verifier/go/oid4vp/av_profile_test.go` — add accepted-set, missing-offer, malformed-representation and legacy-alias controls to the existing fixture assertion.
- `.gitattributes` — give fixture CBOR and binary transcripts binary diff/merge treatment while preserving exact bytes.
- `docs/profiles/av/clause-registry.json` — state the existing legacy-system compatibility exception in both matching partial-coverage limits; source clauses, order, IDs and mapping counts are preserved.
- `docs/profiles/AV-PROFILE-FIXTURES.md` — explain the additional controls and locate the existing proof test accurately while preserving copied provenance.
- This spec — record all review verdicts, verification and the final hand-back status.

Review outcome: twelve individual findings, with high 0, medium 2, low 10,
false 0 and maybe-false 0. Eight grouped patches (medium 1, low 7) address nine
finding rows. Three low findings were rejected: transactional rebuilding for a
late invalid destination adds machinery for an unusual manually changed layout;
FIFO refusal adds guards for a special-file layout outside ordinary fixture
use; generator identity metadata serves manifest-only recovery rather than the
documented checkout-based rebuild. Each rejection and its evidence is recorded
in the triage log. No deferred work was added or changed.

Follow-up review recommended: false. This is a follow-up pass and patched no
high finding, so the workflow's convergence rule is satisfied. Native Windows
and macOS checkouts remain untested; the byte-preserving Git attributes and
normal binary-diff behavior were checked locally. No human decision is required
to finish this fixture story.

Verification after patches:

- `make check` — passed, including fast Go race tests, formatting/vet/lint, existing Python checks, all 37 registry controls and all thirteen fixture controls.
- `python3 tools/build_av_fixtures.py --check` — passed within `make check`; every one of the 49 committed files matches without writes.
- `cd verifier/go && go test ./oid4vp -run '^TestAVProfile'` — passed, including the added subtests.
- `cd verifier/go && go test . -run '^TestStep87OpenID4VPZkVerify$'` — passed using the already available Rust staticlib; the copied proof verifies under its existing OpenID4VP binding.
- `python3 tests/check_av_registry.py` — passed; 86 source blocks, 94 clauses, nine partial mappings, 85 exclusions and nine real test symbols.
- Ordinary `git diff --binary` from the baseline — valid UTF-8 with binary fixture patches and no temporary attribute override; `git check-attr` confirms binary treatment.
- Public source/API verification and local Git input comparisons — passed as described above.
- `git diff --check` — passed; staged whitespace verification is repeated at finalization.

Residual limits: public ciphertext remains opaque; selected container/local-policy
assertions do not establish native DC API or full inherited ISO conformance,
scheme-owner circuit approval, certification or issuance. The carrier still
accepts its documented legacy-system alias. Deliberate rebuilds may finish
earlier writes before a later destination error; an explicitly substituted FIFO
can block generation. These rejected low findings do not create operator actions.

Finalization scope is this story's reviewed files only. Concurrent orchestrator
changes, including sprint bookkeeping and any next-story artifact, are preserved
and excluded from this run's staging/commit. No existing deferred-work ledger
entry was modified or reopened. The user instruction reserving orchestration
files overrides the workflow's generic clean-whole-working-copy requirement.
No push is performed.
