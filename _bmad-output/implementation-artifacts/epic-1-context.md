# Epic 1 Context: EU AV-Profile conformance kit

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Give programmes, relying parties and implementers an inspectable evidence kit for the EU Age Verification Profile: a pinned clause registry, versioned fixtures, executable checks and a report another party can regenerate. The result must explain the checks' boundaries and expose failure or missing evidence as clearly as success. Local preparation can proceed without a partner or device, but completed acceptance requires an independent implementation and independent reproduction. The epic overview's broader claim of no external dependency conflicts with those explicit acceptance requirements; do not use it to waive them.

## Stories

- Story 1.1: AV Profile clause registry
- Story 1.2: AV-profile fixtures
- Story 1.3: AV conformance runner
- Story 1.4: av-vci:// invocation
- Story 1.5: AVSUA-equivalent issuance path
- Story 1.6: Published AV report and external reproduction

## Requirements & Constraints

The machine-readable registry must account for every normative clause in a stated profile revision. Each clause needs an executable test reference or an explicit exclusion reason. Maintain at least 20 covered clauses and at least eight executed fail-closed negative controls; growing the clause count alone is insufficient evidence. Profile changes must make missing or orphaned references detectable.

Fixtures must use the profile's own attestation type and parameter tuple, retain their source revision and include at least one container originating outside this repository. Preserve golden bytes with deterministic drift checks rather than decoding and reserializing them. Keep Python dependencies pinned because encoding changes affect the transcript vectors.

Run candidates against the registry and retain per-clause machine results identifying the test and any failed property. Report at least two independent implementations, one of which may be this fork. A failure, malformed evidence or execution error must never become a pass. Distinguish property failure from a candidate that did not start and a test that did not execute; preserve unavailable external evidence and its dated reason. The audit returns nonzero for in-scope failures, missing required evidence or unmet coverage thresholds. An adapter or synthetic harness check cannot establish independent implementation evidence.

Publish the failures and exclusions together with the passes. Reproduction must start from a clean checkout and documented prerequisites, record exact code and profile revisions, commands, return codes and host class, and produce comparable per-clause outcomes. Record the CPU baseline, observed flags, Rust toolchain, installed JDK and sibling Longfellow revision so environment failures are distinguishable from profile failures. A maintainer or implementation agent's reproduction does not satisfy independent reproduction.

Keep claims limited to one boolean age predicate on one attestation and the declared carriers. Do not imply certification, endorsement, production issuance, production trust or device performance. Keep unmeasured device figures explicitly unmeasured and published findings open until verification supports closure. Never silently introduce a new predicate, system, carrier or attestation. Preserve upstream behaviour and the shared verification baseline: fast checks, Android unit tests, JVM conformance, Go vet and end-to-end tests. Historical test counts are not a ceiling or fresh evidence. The build remains secretless; holder device keys and demo-data must never be committed. Index documentation and catalogue new checks by what they prove.

## Technical Decisions

The profile-specific attestation is `eu.europa.ec.av.1`, and the supported ZK family is `longfellow-libzk-v1`. Use all five profile tuple fields: `circuit_hash`, `version`, `num_attributes`, `block_enc_hash` and `block_enc_sig`. Label externally sourced and locally generated fixtures separately without treating their source as circuit approval.

The Digital Credentials API is the profile's mandated ZK path. This project's OpenID4VP `mso_mdoc_zk` path is profiled and unofficial; report labels must preserve that distinction. Preserve the single carrier adaptation boundary and single transcript entry point, including structural prevention of flow mixing. Preserve allowlist decisions keyed by system identifier, circuit hash and parameters. Encrypted `direct_post.jwt` stays the verifier default; no report improvement authorizes downgrading runtime security policy to remove a reported mismatch.

Trust decisions fail closed with named causes when evidence is missing, malformed or ambiguous. Mocks and local files are demonstration inputs, even where the surrounding validation machinery is real. Invocation and issuance checks must keep local AVSUA equivalence separate from real issuer or wallet-provider participation. Keep backlog and PRD requirement numbering separate; story identifiers connect their records.

## UX & Interaction Patterns

Provide generated self-contained HTML alongside machine-readable output. The report is explicitly English-only and uses the project's root visual family. Show identity, independence notice, profile revision, code revision and host class prominently, followed by summary counts, the clause list and reproducibility details. Every displayed count and verdict belongs to the named run.

Keep clauses in profile order, including exclusions in their natural positions. Each row exposes its test identifier or exclusion reason without requiring a footnote, with expandable evidence and the named failed property. Distinguish PASS, FAIL, ERROR, NOT_EXECUTED and excluded scope in text; colour cannot carry the meaning alone. Make disclosures keyboard operable with visible focus, support narrow viewports and avoid dependencies on network resources. A report from an earlier revision must remain visibly tied to that revision. State missing second-implementation evidence plainly rather than implying external acceptance.

## Cross-Story Dependencies

Registry identifiers and source pins anchor fixtures, probes and reports; later stories reuse them rather than inventing versions. Invocation and issuance provide their own evidence, while the reporting story retains the runner's actual outcomes and reproducibility metadata. Independent candidate execution and independently attributed reproduction are separate acceptance gates. Carrier-suite and documentation-honesty work share the same provenance, vocabulary and partial-scope constraints. Device measurements govern performance claims and cannot be substituted by a conformance or local-host run.
