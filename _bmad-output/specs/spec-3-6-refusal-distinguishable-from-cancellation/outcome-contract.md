# Outcome and acceptance contract

## Semantic categories

The names below express a local interface contract, not Android, browser or OpenID wire identifiers. Successful and pending presentations retain their current meaning.

| Category | Required evidence | Holder-facing meaning | Safe cause examples |
|---|---|---|---|
| Policy refusal | A specific policy/support decision was made | The wallet or service refused this request, with its reason | Unsupported protocol; forbidden plain fallback; unauthenticated request; known register-not-found decision |
| Holder cancellation | An explicit holder cancel action while the request is still active | You cancelled this request | Consent cancel; explicit provider cancel; other UI actions only when defined and tested as cancellation |
| Technical failure | Execution failed, timed out or ended without evidence of holder cancellation | The request could not complete | Unexpected parse/prover/transport failure; timeout; non-user abort; unknown or opaque platform failure |

A register lookup outage or failed lookup is not evidence of a missing registration. A bare exception, empty credential list, disconnected peer or missing response does not prove a policy decision or holder action. Preserve existing reason-specific policy decisions; do not classify from message substring alone. These rules describe causality, not new register/proof policy.

## Cause preservation

- Preserve the terminal cause already known for the request. Closing or navigating back from its refusal/failure notice acknowledges that outcome; it does not create holder cancellation.
- Only a cancel action while active creates holder cancellation. A lifecycle/coroutine cancellation, provider error, timeout or unexplained abort uses technical/unknown unless a prior explicit holder action establishes cancellation.
- Repeated Close/Back/cancel events and late callbacks cannot relabel an established terminal outcome or release a presentation. Allow a deliberate new attempt only as a new request, not by clearing the old cause into success.
- Retain the distinction in observable UI state, terminal effects/result translation, safe diagnostic records and acceptance observations at boundaries the project controls. Show a specific reason for refusal and a neutral technical message for unknown failure.
- Where an external platform collapses causes, record the limitation and preserve local evidence; an opaque browser error must not be labelled as the holder cancelling. Do not promise three standardized browser exceptions or reuse a generic result as proof of causality.
- Never expose credential values, proof bytes, keys or raw request/exception payloads to make this distinction. Reuse safe reason identities and existing logging controls. Retryability is separate from the outcome category; no blanket retry promise is implied.

## Required regression matrix

| Case | Expected observation | Boundary assertion |
|---|---|---|
| Unsupported protocol on real requests[] dispatch | Policy refusal with UNSUPPORTED_PROTOCOL | Retained refusal, visible reason, zero payload-processing continuation and zero response release |
| Existing plain-fallback/authentication policy refusal | Policy refusal with original safe reason | Refusal survives subsequent share/authentication actions; zero response release |
| Close or Back after refusal | Original policy refusal | Final result and log do not become holder cancellation |
| Explicit holder cancel before completion | Holder cancellation | Distinct interface/result/log observation; zero response release |
| Unexpected processing or response-generation exception | Technical failure | Safe reason retained and displayed; no cancellation label or success response |
| Injected timeout | Technical failure with timeout cause | Not refusal or holder cancellation; zero late response release |
| Non-user abort or lifecycle interruption | Technical failure/aborted where a result can be observed | Not holder cancellation; never fabricate an acknowledgement after the process cannot report one |
| Opaque external error/abort | Technical failure with unknown cause | UI does not claim policy refusal or holder cancellation |
| Close after failure; repeated/late terminal events | Original technical failure or established outcome | No relabelling, duplicate completion or response release |
| Known unregistered-RP rejection | Policy refusal at RP result/display boundary | Preserve register-not-found; distinguish from request/lookup failure and cancellation |
| Valid supported request and existing incapable-device control | Existing successful/consent behavior | Classification changes do not break consent, protocol selection or permitted fallback |

Run these against actual production dispatch and ViewModel orchestration, with injectable failures and observable effect/result consumers. Also test the screen-to-event and activity/result mapping: assertions on a standalone category enum or RequestRefusalDispatch alone miss the existing defect. Exercise first terminal transition, acknowledgement and a subsequent event. Scope adjacent proximity/OpenID regression coverage to touched shared code.

## Evidence and completion

Use deterministic local cases to prove classification; no genuine foreign credential or live register is required for those cases. Connect case observations to the actual UI/interface category and safe reason, not expected labels emitted without execution. Existing Story 3.5 report verdicts PASS, FAIL, ERROR and NOT_EXECUTED describe tests, not holder outcomes.

Preserve all seven existing acceptance IDs and report ordering. Supplement their evidence or add separate regression tests without rewriting historical reports or implying the two external foreign-wallet rows ran. The adopted acceptance contract remains authoritative for freshness and operator handoff. Record exact commands and observed results; mark unexecuted platform checks explicitly. This specification defines required verification and contains no assertion that implementation tests have run.
