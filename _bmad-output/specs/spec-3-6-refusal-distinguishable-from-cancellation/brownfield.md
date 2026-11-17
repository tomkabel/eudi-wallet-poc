# Brownfield scope and evidence

Baseline inspected on 2026-11-17 at repository HEAD `2c3ae4f8e98f827bbdeb2897fe0e1f6d273f3317`. These are source observations, not a new executed acceptance report. Paths below are relative to the repository root.

## Starting behavior

| File or seam | Observed behavior and consequence |
|---|---|
| `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/DcApiProtocol.kt` | `DcApiRequestDispatch` chooses the first supported protocol or returns Empty/Unsupported; `ProtocolRefusal` already provides localized reason identities. |
| `app/src/main/kotlin/ee/cyber/wallet/domain/presentation/WalletRequestDispatch.kt` | Production `dcApi` delegates protocol selection and refuses before payload processing; `agePlain` stops forbidden fallback before its response continuation. Preserve these decisions. |
| `app/src/main/kotlin/ee/cyber/wallet/ui/screens/presentation/RequestRefusalDispatch.kt` | Retains protocol/plain refusal in DC UI state and stores proximity refusal while stopping transfer; retain the safe cause through completion. |
| `app/src/main/kotlin/ee/cyber/wallet/ui/screens/dcapi/DigitalCredentialsViewModel.kt` | `OnCancelClicked` unconditionally emits `DcEffect.Cancel`; unexpected request/response exceptions emit Error, while coroutine cancellation is rethrown. There is no retained third terminal outcome in `DcUiState`. |
| `app/src/main/kotlin/ee/cyber/wallet/ui/screens/dcapi/DigitalCredentialsScreen.kt` | Refusal notices have a Close button wired to `OnCancelClicked`, conflating acknowledgement with cancellation downstream. |
| `app/src/main/kotlin/ee/cyber/wallet/ui/screens/dcapi/DigitalCredentialsActivity.kt` | Both Cancel and Error finish with `RESULT_CANCELED`; refusal effects keep the notice visible but have no distinct final provider result. |
| `verifier/go/iso.html` | The DC API demo catches exceptions as generic Failed text; it cannot establish holder intent from that catch. |
| `app/src/main/kotlin/ee/cyber/wallet/ui/screens/presentation/PresentationRequestViewModel.kt` | OpenID presentation already distinguishes `ResponseRefused` from connection failure in one send path; use as adjacent context, not evidence that Story 3.6 is complete. |

## Implementation boundary

The primary slice runs from real provider input through `DigitalCredentialsViewModel`, `WalletRequestDispatch.dcApi`, retained refusal state, the holder screen and the activity result consumer. Keep the existing MVI/state, localized messages and callback seams where suitable; choose the smallest design that preserves category and safe cause across this path. Exact new type and API names are implementation choices.

Carry observable causes through the RP result/display boundary used by the acceptance kit, including known missing registration and technical lookup failure. Do not infer remote holder intent where no structured signal reaches the RP. Any proposed Android provider exception or browser mapping must first be checked against the pinned dependencies' official contract and demonstrated where claimed. No such mapping is selected or verified by this spec.

The spec does not require new protocol support, an unrelated OpenID/proximity rewrite or a live operator environment. If shared code changes those paths, retain their refusal/consent/response guarantees with focused regression tests.

## Existing verification anchors

- `app/src/test/kotlin/ee/cyber/wallet/domain/presentation/DcApiRequestDispatchTest.kt` covers pure protocol selection; preserve it, but it cannot prove final outcome distinction.
- `app/src/test/kotlin/ee/cyber/wallet/domain/presentation/EstonianViewModelAcceptanceTest.kt` enters actual provider/proximity ViewModels and observes retained refusal, effects and zero release. Extend that level of production orchestration coverage for the outcome matrix.
- `app/src/test/kotlin/ee/cyber/wallet/domain/presentation/EstonianAcceptanceDispatchTest.kt` covers shared wire/refusal helpers; it remains supplementary to the ViewModel and result-consumer tests.
- `verifier/go/estonian_acceptance_test.go` and the adopted RP acceptance guide anchor registration rejection and local evidence. Keep existing control meanings and external NOT_EXECUTED cases.

The Android CI unit command is `./gradlew :app:testLocal_mocksUnitTest` with JDK 17. Run focused affected tests during implementation, then the Android CI suite; use the repository development/acceptance setup for touched Go or acceptance-runner paths. A docs-only spec run validates the contract and its links, not application behavior.

## Source preservation

| Source claim | Contract location |
|---|---|
| epics.md Story 3.6 holder goal; FR14 interface distinction | SPEC Why; CAP-1 and CAP-2 |
| Story 3.6 refusing policy path and cancellation presented distinctly | CAP-1/CAP-2; semantic categories and regression matrix |
| Story 3.6 assertion on fixed dispatch path | SPEC constraint; required regression matrix; production boundary above |
| Story 3.6 third state for technical failure, timeout or aborted session | CAP-3; cause preservation; technical/timeout/abort matrix rows |
| epics.md NFR13: named cause distinct from retryable error and cancellation in UI, log, acceptance cases | SPEC constraints; safe cause and separate retryability; matrix/evidence observations |
| research/2026-10-23-innovation-assessment.md O2 criterion ii: missing-registration and unattested-request refusal in UI contract | Unsupported-request and register-not-found cases; RP boundary |
| research/2026-10-23-estonia-alignment-assessment.md U5 refusal/cancellation criterion | SPEC Why and interface success; integrator observation |
| research/2026-10-23-shared-appendices.md S4 historical DC API partial-capability entry | Current-source starting behavior above; no historical standards/version claim adopted |

Research references are limited to the cited requirement, not the broader O2 kit, onboarding timing or historical market/legal assertions. Story 3.5 acceptance evidence is adopted rather than copied or reclassified.
