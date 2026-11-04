# Epic 2 Context: ZK carrier conformance suite

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Make the repository's ZK carrier behavior reproducible and diagnosable for wallet and verifier implementers. The suite should preserve and expose the existing negative controls, identify which binding failed, keep carrier fixtures traceable to their source version, and publish the outcome of an independent implementation run. Local checks and independent interoperability evidence are separate claims; a local pass cannot stand in for an external run.

## Stories

- Story 2.1: Carrier runner over existing cases
- Story 2.2: Binding-property failure naming
- Story 2.3: Version-labelled carrier fixtures
- Story 2.4: External carrier run and published result

## Requirements & Constraints

- Cover the existing end-to-end assertions and carrier vectors without weakening their cryptographic checks. Preserve the difference between OpenID4VP response transport modes and the carrier encoding; do not imply that all end-to-end cases exercise every carrier.
- Provide stable outcomes for malformed or unsupported carrier input, missing presentations, decryption failures, and each binding-property failure. Failures should identify the check that failed; diagnostics must not expose credential contents or turn an unclassified crash into a guessed property failure.
- Maintain fixture and profile provenance against the applicable external document revision. A new carrier rule or fixture must not silently inherit a stale label. Preserve vector bytes unless an explicitly reviewed regeneration is required.
- Reports must name the independent target and revision, preserve failures as failures, and distinguish an unavailable or unbuildable target as `BLOCKED`. Do not represent local synthetic evidence as independent interoperability.
- The OpenID4VP `mso_mdoc_zk` carrier is profiled but unofficial. The Digital Credentials API is the mandated ZK presentation path; conformance reporting must preserve this scope distinction.

## Technical Decisions

- Keep `verifier/go/oid4vp/carrier.go` as the carrier dispatch/adaptation point and `TranscriptForFlow`/`Flow` as the transcript entry point. Carrier parsing, transcript construction, and proof checks must agree on the same bound values.
- The verifier trusts its own session state and recomputes the transcript. Bound inputs include `client_id`, `nonce`, `response_uri`, and the verifier response-key thumbprint; these are not trusted from an unverified wallet response.
- Preserve the local circuit allowlist checks for circuit identifier, hash, and parameters, and report those separately from carrier and transcript failures. Keep response decryption failures upstream of presentation parsing so they remain distinguishable.
- Use the committed cross-language vectors as reproducible artifacts. Regeneration must use the other implementation and retain provenance; the implementation under test must not author its own expected vector.
- The external run is a separate evidence layer. Record target selection rationale, revision, date, commands, and outcome, including the evidence for a blocked result.

## Cross-Story Dependencies

- Story 2.1 establishes the runner and observable case results used by the failure vocabulary and later external execution.
- Stories 2.2 and 2.3 add diagnosable binding failures and version-labelled fixtures; both rely on the runner/vector coverage from 2.1.
- Story 2.4 depends on the local suite and its failure and version reporting being ready before an independent target is run. External implementation and revision are outside this repository and must be recorded rather than fabricated.
