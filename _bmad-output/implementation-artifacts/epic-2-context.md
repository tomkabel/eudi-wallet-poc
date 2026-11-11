# Epic 2 Context: ZK carrier conformance suite

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Make carrier interoperability checkable by publishing the existing negative controls as a reproducible runner, explaining rejected responses through the binding property that failed, and recording the result against an external implementation. This is an initial-release deliverable: a published failure provides useful evidence, while an unsupported compatibility assertion does not. The current planning directory contains the epic breakdown but no standalone PRD, architecture record, product brief or UX spine; this context therefore distils the epic and its shared constraints. The later issuance course correction explicitly leaves Epic 2 unchanged.

## Stories

- Story 2.1: Carrier runner over existing cases
- Story 2.2: Binding-property failure naming
- Story 2.3: Version-labelled carrier fixtures
- Story 2.4: External carrier run and published result

## Requirements & Constraints

Expose the existing thirteen end-to-end cases, cover both supported carriers, and state each case's response mode. Published vectors must accompany the runner so an integrator can reproduce the result from a clean checkout using documented commands. Catalogue the suite and what it proves in the test documentation. Execute it in the fast checks or corresponding CI job, or record an exclusion and explicit invocation.

Rejected presentations must identify the property that failed. A generic rejection description must not replace a binding-property diagnosis. Refusal because no provable presentation exists is its own outcome, distinct from a failed binding. Unsupported carrier material and JWE decryption failure also need distinct outcomes; neither should be misrepresented as a binding violation. These distinctions are part of the suite's debugging contract.

Fixtures and profile rules must identify the external document version they implement. Compare each label against the current carrier-profile version instead of accepting any nonempty or historical label. Cover rules and fixtures together so newly introduced rules cannot silently escape labelling. Standards drift should remain traceable to a specific profile revision.

The external run must name the selected implementation, record its revision, explain its selection and publish its actual result. A failed case remains a failure. An implementation that cannot be built or reached is blocked, with the relevant output, distinct from test failure, and remains visible in the report. Missing external availability never becomes a fabricated pass. Repository-local work can complete independently while the external gate remains explicitly blocked with a reason and date.

Repository artefacts must not imply certification or an independent compliance assessment. Bound claims by the tested environment and the product's narrow scope: one boolean predicate on one attestation through two carriers. Preserve the stated limits concerning ZKP policy, circuit security and absent device measurements; carrier-suite success does not resolve those limits. A new predicate, carrier, attestation or ZK system requires an explicit scope decision.

Keep the build secretless and never commit anything from `demo-data/`. Preserve pinned Python dependencies and golden transcript inputs. Required regression checks remain applicable; if a check cannot execute, record the reason rather than treating missing execution as success. The planning baseline names `make check`, Android local-mocks unit tests, JVM ZK-conformance tests and Go vet. Obtain current test counts from execution instead of presenting an old planning count as fresh evidence.

New documents require a concrete output path and inclusion in `docs/README.md`; plan records belong under `docs/planning/`. Test documentation must explain what each suite proves. A recent prose edit alone does not establish that its evidence is current.

## Technical Decisions

Carrier adaptation must continue through the established `Carrier` interface and sniff dispatch, the single adaptation point for ISO second-edition changes. Transcript construction must use the flow-specific entry point and retain the structural prohibition on mixing flows. Diagnostic additions must preserve those boundaries.

Encrypted `direct_post.jwt` remains the verifier default. Plain `direct_post` requires explicit opt-in; conformance work must not invert that default. Unknown carrier bytes must not fall through into JSON parsing merely to obtain another error category.

The profile of record is `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`. Prepared standards-submission material remains user-gated; suite implementation does not authorize filing it. Preserve circuit allowlist identity based on identifier, circuit hash and parameters, including both existing system labels. Reporting changes must not weaken acceptance decisions or reinterpret unsupported material as another format.

## Cross-Story Dependencies

The runner and vectors established by Story 2.1 provide the common surface for property diagnoses, version checks and external execution. Story 2.2 refines the rejection model; Story 2.3 makes the suite maintainable across profile changes; Story 2.4 supplies independently attributable interoperability evidence. The external implementation is outside this repository, so local test coverage and external evidence keep separate completion states. Standards-watch work in Epic 8 relies on the version-labelled fixtures and the same carrier adaptation boundary. Broader evidence publication must distinguish a repository test result from an external compatibility claim.
