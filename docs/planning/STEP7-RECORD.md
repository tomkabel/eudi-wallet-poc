# Step 7 record — close the loop

Plan: `EUDI-WALLET-POC-CONFORMANCE-PLAN.md` §4 item 7. Date: 2026-09-24.
Branches: `step7-close-the-loop` (this repository) and `step7-conformity-note`
(`eudi-wallet-poc`). This repository's `step7-close-the-loop` is PR #25; the
fork's `step7-conformity-note` is pushed to `tomkabel/eudi-wallet-poc`.

## 1. The holder, recorded in this repository's documents

`docs/ARCHITECTURE.md` §6 gains a closing subsection, "The Android holder,
outside this tree", and the four READMEs (root, `issuer/`, `wallet/`,
`verifier/`) each gain a short pointer. Every one states the same three facts,
each checked against the repos before writing:

- The fork: <https://github.com/tomkabel/eudi-wallet-poc>, an independent fork of
  [`open-eid/eudi-wallet-poc`](https://github.com/open-eid/eudi-wallet-poc), at revision
  **`step6-issuance-consumption @ 910ddc4`** (full hash
  `910ddc49cd6821e927d965054801d78c5400d6f8`). The branch chain was verified by
  ancestry, not by name: `step1-fork-identity` (`ba5ba2d`) →
  `step3-holder-obligations` (`906f7c4`) → `step5-hardware-keys` (`98f880c`) →
  `step4-protocol-hygiene` (`a9f7c0b`) → `step6-issuance-consumption`
  (`910ddc4`), each an ancestor of the next (`git merge-base --is-ancestor`).
- What the fork implements (plan §4 steps 1, 3, 5, 6, wallet side): the fork's
  independent identity and Firebase removal (step 1), the `EE-ZKP-042` pre-share
  notice and `EE-ZKP-051` strict refusal (step 3), multipaz 0.99.0
  `AndroidKeystoreSecureArea` device keys — StrongBox where
  `FEATURE_STRONGBOX_KEYSTORE`, TEE otherwise — with no private-key accessor
  (step 5), protocol and registry hygiene (step 4), and `ee.riik.poa.1`
  issuance in one transaction with consumption on plain presentation and a ZK
  presentation consuming nothing (step 6).
- What remains pending: all device measurement. Plan §6's step-8 measurement
  programme (verification latency, approval-to-decision over DC API, BLE
  throughput, key generation in TEE and StrongBox, the device set itself) has
  produced no numbers, because no device has been run. The intended device set
  — one Pixel 8, one mid-range phone (e.g. a Samsung Galaxy A-series) — is a plan decision
  (D5), not a measurement; spec §23 item 12 stays open. Key-generation timing
  and slot capacity are unmeasured (item 26); audited ≥125-bit circuit
  parameters are unmeasured and unlocated (item 25).

## 2. Spec §23 items 12, 25 and 26

- **Item 25** now records what step 0 measured — that the multipaz 0.99.0 JVM
  prover's proof verifies under this repository's Rust Longfellow runtime and
  the reverse, both directions PASS with committed fixtures
  (`docs/planning/STEP0-CROSS-VERIFY-RECORD.md`, 23 September 2026) — and what
  it did not: no audited ≥125-bit parameter set exists or is located, circuits
  stay v7 at 109-bit soundness. Open.
- **Item 26** now records what step 5 did — `AndroidKeystoreSecureArea`
  (StrongBox/TEE selection) replacing the software BKS path, `batchCreateKey(3)`
  in one transaction — and what it did not measure: key-generation time and
  slot capacity, PENDING-DEVICE (JVM tests cannot create Android
  Keystore/StrongBox keys; no device has been run). Open.
- **Item 12** now records the intended device set from plan §6/D5 (Pixel 8 +
  mid-range phone, e.g. a Samsung Galaxy A-series) with the explicit statement
  that the devices have not been used and no installed-base claim follows.
  Open.

No requirement count changed; no prose outside §23 moved.

## 3. F13 status: verified, no code change

F13 asked for the `EE-ZKP-030` registry fields this repository's accepted-circuit
set was missing. Verified complete and consistent as left by step 2a; no code
was changed in this step:

- `verifier/circuits.json` carries `circuit_hash`, `version`, `num_attributes`,
  `upstream_tag`, `accepted_on`, `audits`, and the F13 fields
  `block_enc_hash: 4151`, `block_enc_sig: 4096` — the values the committed
  `verifier/go/zk/testdata/step0-multipaz/multipaz-circuits.txt` lists for the
  7/1 circuit, byte-identical in the composed label
  `longfellow-libzk-v1_7_1_4151_4096_8d079211…6182121`.
- The `Circuit` struct (`verifier/go/circuits/registry.go:34-47`) mirrors those fields
  with matching JSON tags, and `Circuit.SpecID()` composes the multipaz label
  from them; `verifier/go/oid4vp/isodcapi.go` sends `block_enc_hash`/
  `block_enc_sig` in the `ZkRequest` `params`, which the PoC's
  `matchZkSystemSpec` reads.
- `EE-ZKP-030`'s full field list also names the upstream *commit* and the
  *claimed soundness error in bits with its source*; the local stand-in carries
  neither (the 109-bit figure lives in the spec §10.5 prose, not in the JSON).
  That is a known gap of the stand-in, recorded here rather than fixed in this
  step: F13's blocking part — a `ZkRequest` that cannot carry the full
  `ZkSystemSpec` parameters, so no wallet matches — is closed.

## 4. The fork's conformity note

`eudi-wallet-poc` branch `step7-conformity-note` (off
`step6-issuance-consumption`) carries `docs/CONFORMITY.md`:

- the `EE-ZKP-003` conditional `ZKP_08` statement — the fork records the
  reading its assessment applies (reading (a), primitives and assumptions, spec
  §10.3), which primitives the scheme uses and their ACM v2.0 status, the
  conditional gaps (the `SHA256-AES256CTR` challenge PRF, the 109-bit
  soundness), and that it does not assert unqualified `ZKP_08` compliance;
- the `EE-ZKP-060` inventory — what the fork can and cannot claim today: the
  ZK age proof over the DC API path works and is cross-verified against the
  Rust runtime (step 0 fixtures); no OpenID4VP ZK carrier exists (plan S4), so
  the OpenID4VP path is plain-only; the ISO 18013-7 Annex C ZK path is
  verifier-side plus wallet-code-side but unexercised end to end on a device;
- the plain statement that every build the fork can produce is debug-derived
  (`app/build.gradle.kts` disables the `release` variant; `local_mocks` is the
  default) until the plan §8.6 signing work lands — so `EE-CNF-005`'s and
  `EE-ZKP-002`'s demo/debug objections apply to every build, and nothing here
  is a certification claim.

## 5. Remains open

- Everything device-bound: plan §6's measurement programme (step 8) and every
  PENDING-DEVICE item in `STEP2A-RECORD.md`, the fork's `STEP5-RECORD.md` and
  the fork's `STEP6-RECORD.md`.
- The upstream-commit and soundness-fields gap of the local registry stand-in
  (§3 above), and §23 items 24–28 generally.
