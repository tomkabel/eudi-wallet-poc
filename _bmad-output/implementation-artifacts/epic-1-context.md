# Epic 1 Context: EU AV-Profile conformance kit

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Turn the existing age-verification work into a reproducible evidence kit that a wallet programme, issuer, verifier implementer or evaluator can run and inspect. The kit connects a pinned EU Age Verification Profile to a machine-readable clause registry, profile-specific fixtures, executable checks and a published report. Its value is showing exactly what was exercised, what failed and what remains outside the release's scope; it does not confer certification. Registry and fixture preparation need neither hardware nor an external partner, while an independent implementation and third-party reproduction remain necessary evidence for the completed kit.

## Stories

- Story 1.1: AV Profile clause registry
- Story 1.2: AV-profile fixtures
- Story 1.3: AV conformance runner
- Story 1.4: av-vci:// invocation
- Story 1.5: AVSUA-equivalent issuance path
- Story 1.6: Published AV report and external reproduction

## Requirements & Constraints

Pin the profile document revision and account for every normative clause. Each clause must resolve to a test identifier or an explicit out-of-scope-for-v1 reason. Preserve the profile's natural clause order, including excluded clauses, so exclusions remain visible alongside coverage. The overall kit covers at least 20 clauses and includes at least eight negative controls that must fail closed. A test identifier and a registry entry establish traceability; only an executed result establishes evidence.

Fixtures use the profile's own doctype and parameter names, identify their source revision and include an externally originating container example. Preserve committed fixture bytes with deterministic rebuilding and drift checks rather than decoding and reserializing golden CBOR. Clause-to-test references must remain checkable when the pinned document changes; removed clauses must leave detectable orphan references.

The runner must report clause and test identifiers, the result state and the specific failed property. Missing evidence, startup failures and execution errors cannot become successful results: distinguish `ERROR` and `NOT_EXECUTED` from `PASS` and `FAIL`. Machine-readable output must exit nonzero for an in-scope failure. Publish failures and exclusions as well as passes. Exercise both this repository and an independent implementation; unavailable external evidence stays explicitly unperformed.

Reproduction starts from a clean checkout using documented commands. Record the exact commit, profile revision, host CPU baseline, Rust toolchain and `RUSTFLAGS`, JDK version and Longfellow sibling revision. Python inputs remain pinned because encoding changes alter golden vectors. Environment mismatches must be distinguishable from profile failures. A maintainer's hosted run does not establish independent reproduction.

Keep evidence claims narrow: one boolean age predicate on one attestation, the declared carriers and their limitations. Do not add predicates, systems or attestations silently, imply certification, or fabricate measurements. Builds remain secretless; device private keys and `demo-data/` must never enter commits. Index new documentation and catalogue new checks with what they actually prove. Preserve the repository's existing CI baseline and upstream Android test compatibility.

## Technical Decisions

Use `eu.europa.ec.av.1` and the profile's `ZkSystemSpec` tuple: `circuit_hash`, `version`, `num_attributes`, `block_enc_hash`, `block_enc_sig`. The mandatory AV system is `longfellow-libzk-v1`; the kit does not introduce another ZK family. Issuance coverage must expose the `av-vci://` trigger and the AVSUA-equivalent requirement without describing mock providers as real issuance.

Keep carrier authority explicit in report labels: the Digital Credentials API is the mandated AV ZK path; OpenID4VP `mso_mdoc_zk` is a profiled, unofficial path. Carrier adaptation must respect the single carrier interface and transcript entry point, preserving structural separation between flows. Circuit acceptance remains keyed by identifier, circuit hash and parameters, including the accepted system labels. Encrypted `direct_post.jwt` remains the verifier default; a plain response requires an explicit downgrade choice.

The governing architecture is fail closed: absent or ambiguous trust evidence refuses with a named cause. Local issuer stores, circuit lists, status lists and provider mocks are demonstration inputs until their real replacements are implemented; successful checks against them must not imply production trust. Keep PRD requirement numbers distinct from backlog requirement numbers and use story identifiers when connecting them.

## UX & Interaction Patterns

The report is an English-only, self-contained generated HTML artifact committed with its reproducibility instructions. Use the root design family in a single column: title and independence notice, summary counts for every result state, clauses in profile order, expandable evidence, then reproduction details. Identify profile revision, commit and host class prominently.

Clause rows show a textual state and a test identifier or exclusion reason inline. Keep exclusions in their natural positions with the same row geometry. Details reveal the evidence and named failure property. Rows must support keyboard use, and state words must remain understandable to screen readers and monochrome print; colour alone conveys no verdict. Missing or ambiguous evidence receives its own visible treatment and never borrows a neighbouring clause's result.

## Cross-Story Dependencies

The registry establishes the version and identifiers consumed by fixtures, the runner and the report. Fixtures and issuance checks supply evidence to the runner; the published report depends on those results and reproduction metadata. Independent-candidate execution and third-party reproduction require evidence beyond a local run. Documentation honesty work proceeds alongside this epic, and the carrier suite shares its versioning and evidence discipline. Device measurement work runs in parallel and gates performance claims, not registry completion.
