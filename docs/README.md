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
| [`decisions/ADR-003-MINIMAL-JWE.md`](decisions/ADR-003-MINIMAL-JWE.md) | Why response encryption is a hand-rolled minimal JWE and `direct_post.jwt` is the default |
| [`decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md`](decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md) | The `rp-kit/` deployment profile: the `zkverify` binary under a documented flag set, persisted response key, drain-as-rotation, TARA alongside |
| [`decisions/ADR-005-INTENDED-USE-SOURCE.md`](decisions/ADR-005-INTENDED-USE-SOURCE.md) | The wallet's `IntendedUseSource` interface and the typed, version-pinned registration-certificate model |
| [`decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md`](decisions/ADR-006-CIRCUIT-LIST-DOCUMENT.md) | The signed circuit-list document format, its pinned-key validation and the in-memory provenance ring |
| [`decisions/ADR-007-RP-REGISTER-LOOKUP.md`](decisions/ADR-007-RP-REGISTER-LOOKUP.md) | The fail-closed RP-register lookup interface with its bounded, published cache age |
| [`decisions/ADR-008-CARRIER-PATH-PLACEMENT.md`](decisions/ADR-008-CARRIER-PATH-PLACEMENT.md) | DC API as the mandated ZK path, the OpenID4VP carrier as profiled-but-unofficial, with named enforcement points |
| [`analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md`](analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md) | *ee-eudiw.* The de-facto `mso_mdoc_zk` carrier in OpenID4VP |
| [`analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md`](analysis/REV-1.6-OPEN-ITEMS-RESEARCH.md) | Research on spec rev 1.6's open items: the 29 February rule and the external anchors of the 18 new requirements |
| [`analysis/ZKP08-ANALYSIS.md`](analysis/ZKP08-ANALYSIS.md) | *ee-eudiw.* ARF `ZKP_08` against the Agreed Cryptographic Mechanisms |
| [`planning/EE-EUDIW-ZK-IMPORT-PLAN.md`](planning/EE-EUDIW-ZK-IMPORT-PLAN.md) | The plan that brought the specification and the ZK spine into this repository |
| `planning/STEP*-RECORD.md` | Execution records of the conformance plan's steps. The wallet-side ones were written here; `STEP0-CROSS-VERIFY`, `STEP1-FORK`, `STEP2A`, `STEP6-VERIFIER`, `STEP7`, `STEP8` and `STEP8-7` are *ee-eudiw* |
| [`research/2026-10-23-innovation-assessment.md`](research/2026-10-23-innovation-assessment.md) | Innovation assessment of this fork against upstream and ten comparators, with scored opportunities |
| [`research/2026-10-23-estonia-alignment-assessment.md`](research/2026-10-23-estonia-alignment-assessment.md) | Estonia and EU alignment assessment: requirements traceability matrix, use cases, integration pathways, quantified effects |
| [`research/2026-10-23-shared-appendices.md`](research/2026-10-23-shared-appendices.md) | Shared appendices for both assessments: competitive matrix, requirement-status index, roadmap, risk register, source register |
| [`../_bmad-output/planning-artifacts/epics.md`](../_bmad-output/planning-artifacts/epics.md) | The sprint backlog: 11 epics and 75 stories decomposing every recommendation, suggestion and proposal in the three research documents, with the tracking file at `../_bmad-output/implementation-artifacts/sprint-status.yaml` |
| [`../_bmad-output/planning-artifacts/architecture.md`](../_bmad-output/planning-artifacts/architecture.md) | The architecture record: component and trust-boundary map, the decisions ADR-004…ADR-008 that unblock Epics 3, 4 and 5, the mock→real seams, and the corrections to `ARCHITECTURE.md` where the tree has moved |
