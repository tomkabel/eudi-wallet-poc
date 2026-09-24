# Step 6 record: `ee.riik.poa.1`, one issuance transaction, and consumption

Plan: `EUDI-WALLET-POC-CONFORMANCE-PLAN.md` §4 item 6. Date: 2026-09-24.
Branches: `step6-ee-riik-poa` (ee-eudiw, repo side) and `step6-issuance-consumption` (fork, wallet side).

## Wallet side (this repository)

### Issuance shape (spec §9.2)

`Credential.EePoaCredential`: `age_over_18` (default true for the mock's adult users),
`issuing_country` `EE`, `issuing_authority`, `expiry_date` as `FullDateElement`. Validity spans at
most 90 days (`AgeIssuanceConstants.EE_POA_MAX_VALIDITY_DAYS`, EE-POA-016) anchored to the UTC
issuance day. `null` Status writes no status reference into the MSO (EE-POA-012).

The AV attestation (`eu.europa.ec.av.1`) is trimmed from the 13-threshold set to `age_over_18` only.

### One transaction, batch of three (EE-POA-003 / EE-POA-011a)

`WalletProviderBatchAgeIssuer` issues the EE-PoA batch and the AV attestation in one transaction:
the batch keys are created through `SecureAreaKeyManager.batchCreateKey(3)` — one Android Keystore
call for the whole batch, off the presentation critical path — each attested by the (mock) wallet
provider with its own provider key (step 5's fix) and registered in the `key_attestations` table so
cleanup keeps seeing every alias. Batch size 3 is the plan's deliberate choice: enough to test
consumption and never-the-last; ≥ 30 and OpenID4VCI batch issuance stay in §8.4.

### Consumption (EE-POA-013 / WIAM_21 / EE-ZKP-025)

`EePoaConsumption` (domain/presentation, JVM-tested) implements the rules:

- a plain presentation of `ee.riik.poa.1` consumes the attestation: the SecureArea key is deleted
  first, then the attestation row and its key-attestation row — `SecureAreaKeyCleanup`'s ordering,
  so the alias never becomes unreachable while the Keystore key lives on;
- it refuses when the presented attestation is the last EE-PoA left (never-the-last: Method A
  would degrade to Method B on an exhausted batch);
- a zero-knowledge presentation consumes nothing;
- non-EE-PoA doctypes are out of scope.

Wired after a successful presentation in both flows: `PresentationRequestViewModel.sendResponse`
(redirect path — consumption unconditional for EE-PoA rows, tier `PLAIN_NOT_REQUESTED`) and
`DigitalCredentialsViewModel.onShareClicked` (DC API path — consumption follows the escalated
per-response tier). Hilt binding in `RepositoriesModule`.

### Deviations

- Consumption lives behind the `EePoaConsumption` seam rather than inside the view models: only
  logging was JVM-testable there; the seam carries the acceptance tests.
- The mock's `AgeVerificationCredential` data model still carries threshold fields the mint now
  ignores (trimmed to `age_over_18`); the model is shared with older issuance paths.
- EE-PoA shares the mDL issuer key in the mock (distinct doctype, same mock issuer identity)
  pending a dedicated signer key.
- `IssuanceViewModel` routes both age doctypes through one mock issuance route.

### Tests

`EePoaIssuanceTest` (3): batch of three with one key per attestation in one transaction; batch-size
constant; §9.2 metadata constants. `EePoaConsumptionTest` (4): plain presentation consumes
attestation + key; never-the-last refusal; ZK leaves the count unchanged; non-EE-PoA out of scope.
Full suite at the step's HEAD: 58 unit tests, 0 failures (`:app:testDebugUnitTest`),
`:zk-conformance:test` green.

### PENDING-DEVICE

1. `SecureAreaKeyManager.batchCreateKey` on real Android Keystore/StrongBox hardware — the JVM
   tests exercise only the seam.
2. End-to-end on-device issuance of the 3+1 transaction through the mock issuance screen.
3. On-device consumption behaviour in both presentation flows, including that a ZK proof over
   `ee.riik.poa.1` leaves the batch intact.
4. Verification that Android Keystore key deletion (`deleteKey`) returns the slot (WIAM_21).
5. Registry re-registration after consumption so the platform does not offer consumed attestations.

## Repository side (ee-eudiw) — recorded in its own STEP6-RECORD.md

`mint_ee_poa.py --device-public-key` (F12), `--over` default `[18]` (S10), mock issuer public key
in the verifier trust store for both doctypes, and a Go test proving the verifier accepts a ZK
presentation of `ee.riik.poa.1` minted over a wallet-generated device key.
