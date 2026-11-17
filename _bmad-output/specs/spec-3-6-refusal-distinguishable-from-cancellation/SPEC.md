---
id: SPEC-3-6-refusal-distinguishable-from-cancellation
companions:
  - outcome-contract.md
  - brownfield.md
  - ../../../rp-kit/ACCEPTANCE.md
sources:
  - ../../planning-artifacts/epics.md#story-36-refusal-distinguishable-from-cancellation
---

> **Canonical contract.** This SPEC and its companions define what to build and verify. Sources provide audit traceability; implementation observations in brownfield.md describe the starting point.

# Story 3.6: Refusal distinguishable from cancellation

## Why

Holders and RP integrators need to distinguish a wallet policy decision from the holder choosing to stop. The current DC API flow displays refusal reasons but closes refusals and technical errors through a cancellation result. Story 3.6 (FR14) requires refusal, holder cancellation and technical failure to remain distinguishable in the UI contract and on the fixed production dispatch path.

## Capabilities

- **CAP-1**
  - **intent:** Holders and integrators can recognize a policy refusal and its specific safe cause.
  - **success:** A real refusing dispatch produces the refusal category and reason in the interface, holder UI, diagnostic log and acceptance observation; closing the notice preserves the category and releases no presentation.
- **CAP-2**
  - **intent:** Holders can cancel an active presentation and have that action identified as their own choice.
  - **success:** An explicit holder cancellation produces an outcome distinct from refusal and technical failure; missing responses, generic platform errors and dismissal of a prior refusal never establish cancellation.
- **CAP-3**
  - **intent:** Holders and integrators can recognize technical failure, timeout or an aborted session without being told that policy or their own choice stopped the request.
  - **success:** Injected technical failure, timeout and non-user abort each produce the third interface category with a safe cause; unavailable platform attribution remains technical/unknown.

## Constraints

- Follow the outcome semantics and cause-preservation rules in outcome-contract.md; distinguish cause in the UI, logs and acceptance evidence as required by NFR13.
- Verify the actual production dispatch, retained state and result translation described in brownfield.md; helper-only tests do not satisfy the story.
- Preserve fail-closed policy, supported-protocol selection and consent boundaries: a pre-disclosure refusal, cancellation or failure releases no presentation and introduces no silent plain fallback.
- Platform result codes and browser exceptions are not evidence of holder intent by themselves. Verify any chosen transport mapping against primary sources and supported runtime behavior before claiming an end-to-end distinction.
- Retain existing acceptance case IDs, ordering, evidence states and external-evidence limits from the adopted acceptance contract; classify presentation outcomes separately from test verdicts.

## Non-goals

- New register, TARA, protocol, cryptographic or age-threshold functionality; a universal error taxonomy; new foreign-wallet acceptance or certification claims.
- A repository-wide presentation rewrite or modification of historical acceptance evidence.

## Success signal

A fixed-dispatch regression demonstrates policy refusal, explicit holder cancellation and technical failure as three different observable outcomes, then proves that acknowledging a refusal, timing out or aborting without holder intent cannot become user cancellation. Existing successful and fail-closed controls still pass; no unsupported external-platform claim is reported as executed evidence.

## Assumptions

- The fixed path is the existing DC API provider/ViewModel dispatch through refusal UI and activity result, with RP-facing translation where the cause is observable; shared proximity/OpenID seams need regression checks when touched.
- Unexpected parse/prover/transport failures belong to the technical category unless a known policy decision supplies the cause; no-match alone does not establish holder cancellation.

## Open Questions

- Which supported browser/provider/device combinations must demonstrate end-to-end distinction beyond the local UI/interface and fixed-dispatch tests? The local contract can proceed; external claims require observed behavior.
