# Documents

What each document here is. The specification itself is
[`../spec/EE-EUDIW-TS-1.0.md`](../spec/EE-EUDIW-TS-1.0.md). Documents marked *ee-eudiw* were
imported from that repository at `e368fc1` and describe it as it stood then.

| Document | What it is |
|---|---|
| [`CONFORMITY.md`](CONFORMITY.md) | This wallet's conformity statement against the specification (`EE-ZKP-003`, `EE-ZKP-060`) |
| [`MEASUREMENTS.md`](MEASUREMENTS.md) | The on-device measurement harness and its results table |
| [`DEVELOPMENT.md`](DEVELOPMENT.md) | Building and running the ZK spine, and the checks that define green |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | *ee-eudiw.* How the issuer, wallet, verifier and prover fit together, and the trust boundaries |
| [`profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`](profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md) | *ee-eudiw.* The `mso_mdoc_zk` OpenID4VP carrier as implemented, field by field |
| [`profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md`](profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md) | *ee-eudiw.* Issue text for DCHP #17 on ZKP support |
| [`decisions/ADR-002-STRICT-CBOR-SUBSET-DECODER.md`](decisions/ADR-002-STRICT-CBOR-SUBSET-DECODER.md) | *ee-eudiw.* Why the ISO dcapi path decodes a strict CBOR subset in-house |
| [`analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md`](analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md) | *ee-eudiw.* The de-facto `mso_mdoc_zk` carrier in OpenID4VP |
| [`analysis/ZKP08-ANALYSIS.md`](analysis/ZKP08-ANALYSIS.md) | *ee-eudiw.* ARF `ZKP_08` against the Agreed Cryptographic Mechanisms |
| [`planning/EE-EUDIW-ZK-IMPORT-PLAN.md`](planning/EE-EUDIW-ZK-IMPORT-PLAN.md) | The plan that brought the specification and the ZK spine into this repository |
| `planning/STEP*-RECORD.md` | Execution records of the conformance plan's steps. The wallet-side ones were written here; `STEP0-CROSS-VERIFY`, `STEP1-FORK`, `STEP2A`, `STEP6-VERIFIER`, `STEP7`, `STEP8` and `STEP8-7` are *ee-eudiw* |
