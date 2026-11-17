# Epic 3 Context: Estonian relying-party acceptance kit

<!-- Generated from planning artifacts. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Deliver an acceptance kit an Estonian e-service operator can run: a deployable verifier service, a TARA-facing integration contract, a replaceable register lookup with an explicit fail-closed default, an interface proposal, and reproducible acceptance results. The planning premise is a gap between foreign-wallet acceptance and the national wallet's availability, while no public RP-register API is established. Treat the dates, legal obligations and external-service availability as planning context requiring primary-source verification before making a current public claim.

## Stories

- Story 3.1: Deployable RP service profile
- Story 3.2: TARA-facing adapter contract
- Story 3.3: Fail-closed RP-register interface and mock
- Story 3.4: RP-register interface proposal
- Story 3.5: Four Estonian acceptance cases
- Story 3.6: Refusal distinguishable from cancellation
- Story 3.7: Integrator README and timed onboarding
- Story 3.8: Parallel-path integration note
- Story 3.9: Gateway adapter for the rp-kit

## Requirements & Constraints

The service profile must start using documented commands, with an architecture decision recorded before implementation. Preserve encrypted `direct_post.jwt` as the default; plain `direct_post` requires explicit opt-in. A callback that never arrives must terminate through the documented session TTL and expose status identifying the session, so an RP can determine the outcome.

External prerequisites must remain visible. TARA integration requires the appropriate RIA agreement and a test client; unavailable service or missing agreement must fail closed and cannot produce a passed integration result. A register lookup must distinguish absent configuration from an unreachable, timed-out or failing configured service. Any cache has a bounded age and an explicit logged stale-data decision. A mock is an implementation for tests, never evidence that a public register exists. The proposal remains useful if later superseded by the authority's interface.

Acceptance evidence covers foreign PID and age-verification attestations alongside refusals for unattested requests and unregistered RPs. Preserve the unauthenticated-request policy across Digital Credentials API, remote `org-iso-mdoc` and proximity shapes: on a ZK-capable device, a plain-mdoc-only request cannot bypass the Tier-2 requirement when the request lacks the required authentication. Pinned circuit fallback must let wallet and verifier resolve the same registry document version.

Publish reproducible commands and machine-readable outcomes. Timed onboarding needs at least three human integrators without prior ZK knowledge, an explicit method, participant and failure counts, and a distribution including honest treatment of participants who never succeed. Do not fabricate external participation or verification. Estimates and measurements carry their assumptions and methods.

Keep the build secretless and exclude `demo-data/` from commits. Code changes must meet the repository's documented checks; explain any unavailable check rather than imply it passed. Do not introduce compliance or certification claims. Product descriptions carry the narrow scope: one boolean predicate, one attestation, two carriers, with the stated cryptographic and measurement limitations. A new predicate, ZK system, carrier or attestation requires an explicit scope decision. Index new documents in `docs/README.md` and catalogue new test suites in `tests/README.md`.

## Technical Decisions

No deployment-profile ADR or general architecture decision was established by the planning artefacts; Story 3.1 must make the deployment choice rather than assume one. Keep the wallet-facing route parallel to TARA. Do not present X-Road transport or RP-register access as an established wallet interface without evidence. Authentication alternatives such as Smart-ID and Mobile-ID are separate from EUDI presentation routes.

Use interfaces before external integrations: the register lookup is replaceable and ships with a fail-closed default and a mock; request policy will later obtain intended use through a separate interface. Existing carrier dispatch and transcript construction remain the adaptation points for carrier evolution, and the circuit allowlist continues to identify a circuit by id, hash and parameters. Deployment work must preserve these boundaries.

The gateway adapter uses nginx external authorization or Envoy ExtAuthz to gate a route on the verifier's existing result. It must not implement an independent verification decision. Missing or expired sessions and unavailable verifier or session storage deny access. Acceptance logs name circuit provenance without adding a holder identifier. The planning decision treats this as a new integration surface within the existing proof scope.

## UX & Interaction Patterns

The interface contract distinguishes policy refusal, holder cancellation and technical failure, including timeout or abort. Those distinctions must survive the fixed dispatch path and gateway response rather than collapse into a generic denial. The integrator README must take a new integrator from checkout to reproduced acceptance results and show the gateway path when that adapter exists. No independent UX spine is supplied by the planning artefacts.

## Cross-Story Dependencies

The deployable profile is the foundation for the acceptance kit. The register interface and mock support the proposal and acceptance scenarios; the four-case runner and outcome contract support measured onboarding. Story 3.9 explicitly depends on 3.1 and 3.7, and its timing evidence connects to Story 6.6. Story 3.5's fallback agreement intersects Epic 5's wallet/verifier circuit distribution. Production operational hardening belongs to Epic 11. The later issuance proposal explicitly removed a proposed Story 3.1 dependency from Story 10.6 unless this repository's verifier is actually used for nested PID presentation.
