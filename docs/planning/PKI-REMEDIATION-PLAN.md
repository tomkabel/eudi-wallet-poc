# PKI remediation plan

Status: P0–P2 implemented; P3–P5 implemented where no decision, money or hardware blocks them; see "Implementation status". Source: the 2026-11-04 PKI critique (items C1–C16) plus one finding it missed (C0). Branch: `ee-eudiw-zk-import`. Order is fixed; no dates are promised.

## Implementation status (2026-11-04)

Each line names its judge. "Verified" means the judge was run locally and passed; CI has not yet run on the pushed commit.

| Item | State | Judge and result |
|---|---|---|
| NEW-P0-1 (C0) | Done. Trust-all removed from the validator, settings, preferences (proto field 4 reserved) and strings. | `validator has no trust-all switch in any build` (reflection); gate `trust-all`. Verified. |
| NEW-P0-2 (C7) | Scan and gate done; purge waits on D5. | gitleaks 8.30.1 and trufflehog over 241 commits: no live secret. Findings are test fixtures, public status-list JWTs, BMAD story keys, and upstream's Firebase key in five pre-fork release commits (removed at `7d171e2`; upstream's to rotate). The `.p12` files open with `changeit`: both doc-signer stores hold P-256 private keys for TEST DSes under TEST IACA Root CA; `lotl-keystore.p12` holds only a trusted certificate (TEST LOTL signer); `debug.keystore` holds the debug signing key. None signed anything real, so nothing is rotated. `security.yml` scans full history and first proves a seeded key turns it red. Verified. |
| NEW-S1-1 (C14) | Done, except detekt (not added: plugin plus lockfile and verification-metadata churn for one more linter). | govulncheck, gosec with fingerprint baseline (47 findings, G706/G115/G304), pip-audit, CodeQL (Go, Python, Kotlin), `:app:lintRelease` with 23 fatal security checks and a baseline. Seeded: vulnerable `golang.org/x/text` (govulncheck exit 3), `cryptography==41.0.0` (pip-audit exit 1), empty `X509TrustManager` (lint exit 1). Verified locally; CodeQL runs only in CI. |
| NEW-S1-2 (C6) | Done. One file, `verifier/go/internal/algpolicy/policy/algorithms.json`: Go embeds it, the app packages it as a resource. | Shared vectors `policy_sha1`, `policy_rsa1024`, `policy_p521` refused and `policy_p384_sha384`, `policy_rsa2048` accepted by both engines. Verified. |
| NEW-S1-3 (C8) | Done. `-ignore-trust-freshness` exists only with `-tags devflags`. | `TestIgnoreTrustFreshnessIsDevBuildOnly`: default build exits non-zero naming the flag. Verified. |
| NEW-S1-4 (C11) | Done. Five mock services moved to `src/local_mocks`; bindings split via `src/real`. The release variant was re-enabled so it can be compiled and linted. | Release bytecode has no `*Mock` class; the same check on `local_mocks` finds them. Verified. Open: `MockAttestationChallengeSource` is still bound in every build type. |
| NEW-S1-5 (C15) | Done, as `trust/MANIFEST.json` (JSON so the gate needs only the stdlib). | Gate `manifest`: 16 artifacts, digests, dates read from the files, private keys test-only, 30-day expiry margin. Seeded tests for each failure. Verified. |
| NEW-S1-6 (C16) | Done. `.agents/skills` is a symlink to `.claude/skills` (they were byte-identical apart from graphify). `statuslists/` stays until D2. | — |
| NEW-S2-1 (C3) | Done. Go `VerifyChain` (x509.Verify, profile checks), Kotlin `ChainProfile.MDOC_DS`; issuer trust stores carry `iaca`, mandatory in signed stores; the mint issues its DSC from an IACA. | Shared vectors (expired, not-yet-valid, wrong/missing EKU, no digitalSignature, CA DS, foreign root, self-signed, pathLen) refused by both engines; `TestSelectIssuerChainValidatesDS`; `issuer/test_mint_custody.py`; carrier e2e with freshly minted IACA-signed credentials passes. Verified. |
| NEW-S2-2 (C5) | Done. `TrustDomain`: LOTL anchors apply only to OpenID4VP client and reader authentication. LOTL certificates are kept only while a service of the configured type is `granted` or `recognisedatnationallevel`. | `lotl-only anchor cannot validate a reader certificate under static-only policy`; `LotlStatusFilterTest`. Verified. Not done: RFC 5280 name constraints on LOTL intermediates (no constrained intermediates exist in the pilot lists to test against). |
| NEW-S2-3 (C9) | Done. Trust store indexed by SHA-256 of the SPKI; `issuer_kid` hint on the plain path; load-time cap removed. | `TestPlainPathFiftyIssuersOneVerify`: 50 issuers, exactly one ZK verify. Verified. |
| NEW-S2-4 (C4) | Done. `tests/vectors/trust/cases.json` (18 cases) run by both engines. | The suite found a real disagreement: the JDK ignores an anchor's pathLenConstraint, Go enforces it. Kotlin now enforces it. 18/18 agree. Verified. |
| NEW-S3-1 (C1) | Waits on D1. Behaviour unchanged (soft-fail on availability; a verified CRL that names a certificate refuses it). | — |
| NEW-S3-2 (C2) | Go verifier done (`oid4vp.StatusChecker`, `statuslist+jwt`, ES256, key per list URI, `sub` match, ttl/exp, fail closed when stale). Kotlin not started. Mock status URIs are gated out of release sources. | `TestStatusListVerdicts`: revoked, suspended, out-of-range, swapped-payload, foreign-key, wrong-sub, expired and blackholed-after-ttl all refused. Verified. |
| NEW-S3-3 | Not started; needs 10.1 and 10.3. Status is deliberately not wired into the ZK paths: a status index is a per-credential identifier and would link presentations. | — |
| NEW-S4-1 (C10) | Step 1 done: production EC keys (proofs, presentations) are generated and used in AndroidKeyStore through `SecureAreaKeyManager`; `EncryptedKeyStoreManager` (BKS wrapped by a Keystore AES key) holds only the RSA keys the wallet provider's `generateKey` path uses. Steps 2–4 wait on D3 and a device. | — |
| NEW-S4-2 | Mint side done: persisted keys need `EE_MINT_KEY_PASSPHRASE` and must be encrypted PKCS#8 outside `--test-mode`. Production KMS/HSM waits on D4. | `issuer/test_mint_custody.py`. Verified. |
| C12 | Unchanged (stories 4.2–4.6, 11.6). | — |
| C13 | Partly done: every mirrored file is pinned and records a licence (gate `mirror`). `gradle/verification-metadata.xml` does not cover `org.cose:cose-java` (it lists only `com.augustcellars.cose`), so the mirror pin is the only integrity check on it. The SNAPSHOT is not replaced. `com.signerry:androidawt` has no licence on record [UNVERIFIED]. | Gate `mirror`. Verified. |

Found while implementing, not in the critique:
- `util/Certificates.kt` still installs a trust-all `X509TrustManager` for gRPC in debuggable builds (the `local_mocks` build installed on phones included). It is frozen in the lint baseline, not fixed.
- A one-line Kotlin `HostnameVerifier { _, _ -> true }` does not trigger lint's `BadHostnameVerifier`; the lint gate does not cover that form.
- `./gradlew spotlessCheck` fails on the base commit (ktlint unresolvable under `FAIL_ON_PROJECT_REPOS`); CI does not run it.

## Goal beyond the goal

Every certificate or credential the wallet or verifier accepts traces to an operator-named anchor, has a live status check that fails closed, and has a test that breaks when any of that stops being true.

## Types

- **Altitude (fits).** Outcome: a pilot relying party can defend each accept or refuse decision. Ladder: (1) every critique item closed by a test that fails before and passes after; (2) the defect classes blocked by CI gates, not by review; (3) a recurring PKI audit that re-runs the critique each release and fails on regression.
- **Depth (fits).** Senior-PKI methods: RFC 5280 path validation, ISO/IEC 18013-5 IACA→DS trust, signed status lists, HSM/KMS-backed issuance, differential testing across the Kotlin and Go verifiers, secret scanning over full history.
- **Range (fits).** Crew, each with their method: PKI auditor (trust model), ETSI/eIDAS assessor (trusted lists, algorithm policy), red-team pentester (debug path, CRL blackholing, key extraction), HSM operations engineer (custody), Android platform-security engineer (Keystore, StrongBox, attestation), supply-chain engineer (provenance, Gradle verification).
- **Skipped.** Speed: the same work faster adds no standard.

## Hard target

- Zero open critique items at the end of P4.
- Zero secret-scanner findings on HEAD; the history report reviewed.
- Revoked, blackholed and stale fixtures refused by both verifiers.
- Differential suite agrees on 100% of shared cases.

## Outside judge

- Named tests per item, each failing before and passing after.
- gitleaks and trufflehog on HEAD and full history.
- Revocation fixtures from story 10.4, run in CI.
- Differential suite, Kotlin against Go.
- govulncheck, gosec, CodeQL, detekt and Android lint output.
- Human: external security review (11.4); counsel for 13.1.

## Corrections to the critique

- **C0 (new, high for debug distribution).** `CertificateChainValidator.kt:65` sets `trustAll = enabled && BuildConfig.DEBUG`, and the branch at line 111 accepts any chain. The toggle is a user preference (`UserPreferencesDataSource.kt:59`). Release builds are safe; debug builds, the ones AGENTS.md tells you to install on the phone, accept any chain on a user switch.
- **C1 conflicts with backlog.** Story 10.4 records "CRL soft-fail is the documented policy". The critique recommends fail-closed. Resolved by decision D1.
- **C7 unverified.** The three `.p12` files could not be opened without a password, so whether they hold private keys is unknown. The `device_key.pem` finding is verified.
- **C10 open.** The code wires `EncryptedKeyStoreManager` (`SecurityModule.kt:35`) and also contains `SecureAreaKeyManager`. Which one signs in production is not established.

## Coverage map

| Item | Defect | Phase | Backlog link | Proposed ID |
|---|---|---|---|---|
| C0 | Debug trust-all reachable by preference | P0 | none | NEW-P0-1 |
| C7 | Test key material tracked | P0 | 11.8 (partial) | NEW-P0-2 |
| C6 | No explicit algorithm or key-size policy | P1 | none | NEW-S1-2 |
| C8 | Freshness bypass flag in release binary | P1 | none | NEW-S1-3 |
| C11 | Mocks in `src/main` | P1 | 14.1 | NEW-S1-4 |
| C14 | Lint is not a security gate | P1 | none | NEW-S1-1 |
| C15 | Trust material scattered, no expiry alarm | P1 | 13.1 (inventory only) | NEW-S1-5 |
| C16 | Duplicate skill trees; orphan `statuslists/` | P1 | none | NEW-S1-6 |
| C3 | DS certificate never chain-validated | P2 | 4.2 (adjacent) | NEW-S2-1 |
| C5 | Static and LOTL anchors unioned | P2 | 13.1 (adjacent) | NEW-S2-2 |
| C9 | Issuer cap set by ZK cost | P2 | none | NEW-S2-3 |
| C4 | Kotlin and Go trust engines can disagree | P2 | 14.1 (partial) | NEW-S2-4 |
| C1 | Revocation soft-fail; wallet has it off | P3 | 10.4 (conflicts) | NEW-S3-1 |
| C2 | Status lists read by no verifier | P3 | FR58, 10.3 | NEW-S3-2, NEW-S3-3 |
| C10 | Android key custody is wrapped PKCS#12 | P4 | 10.3, 13.1 | NEW-S4-1 |
| C7/C9 | Issuer key is a plaintext PEM on disk | P4 | 10.1 | NEW-S4-2 |
| C12 | Certificate parsing and CT in backlog | P5 | 4.2–4.6, 11.6 | existing |
| C13 | Vendored mirror has no integrity, SNAPSHOT dependency | P5 | 11.8 | extend 11.8 |

## Phases

### P0 — Contain (now; blocks everything else)

- **NEW-P0-1 Debug trust-all (C0).**
  1. Confirm no release path reaches `setTrustAll`.
  2. Remove the preference-driven switch from `CertificateChainValidator`, or confine it to the test source set.
  3. Unit test: trust-all is unreachable with `BuildConfig.DEBUG` true and false.
  4. CI grep gate: `setTrustAll` has no production caller.
  Judge: the unit test and the grep gate. Size S.
- **NEW-P0-2 Key material in git (C7).**
  1. Run gitleaks and trufflehog over full history.
  2. For each key file (`device_key.pem`, the three `.p12`, `app/debug.keystore`), record whether it is test-only and whether it ever signed anything real.
  3. Rotate any key that was ever a real signer. None is expected.
  4. Purge from history only after D5.
  5. Add pre-commit and CI secret scanning with push protection, and seed a test `PRIVATE KEY` block to prove the gate turns red.
  Judge: scanner clean on HEAD; seeded key fails CI. Size S–M.
- **P0 exit.** Both items merged to `ee-eudiw-zk-import`; no open high-severity item.

### P1 — Foundations (gates everything in P2)

- **NEW-S1-1 Security gate in CI (C14).** govulncheck and gosec for Go; CodeQL for Go, Kotlin and Python; detekt and Android lint with security rules; pip-audit. Baseline file, fail on new findings only. Judge: a seeded vulnerable dependency and a seeded insecure call each turn CI red. Size M.
- **NEW-S1-2 Algorithm policy (C6).** One policy file read by both engines: allowed signature algorithms (ES256 now, a slot for ES384 and ML-DSA later), minimum RSA and EC sizes, hash policy. Negative vectors: SHA-1, RSA-1024, unlisted curve, on both sides. Reference: ETSI TS 119 312 [UNVERIFIED revision]. Size M.
- **NEW-S1-3 Freshness bypass (C8).** Build-tag `-ignore-trust-freshness` out of release builds (`verifier/go/main.go:373`). Startup refuses the flag in release. Judge: a release build with the flag exits non-zero. Size S.
- **NEW-S1-4 Mocks to flavor (C11).** Move `CredentialIssuanceServiceMock`, `MdlProviderServiceMock` and `AgeVerificationProviderServiceMock` to the `local_mocks` source set. Judge: CI greps the release dex for mock classes and fails on any. Size S–M.
- **NEW-S1-5 Trust manifest (C15).** `trust/MANIFEST.yaml` lists every trust artifact with SHA-256, source, owner, `notBefore`/`notAfter` and a test-only flag. Covers `lotl-keystore.p12`, `lotl-signer.cer`, `iaca_root.cer.pem`, `trusted.pem` and the test PKI. CI validates digests, fails on a changed file without a manifest update, and fails when any `notAfter` is under 30 days. Size S.
- **NEW-S1-6 Layout (C16).** Keep one canonical skill tree and generate or link the other. Decide `statuslists/` per D2. Size S.
- **P1 exit.** Security gate green on its baseline; manifest covers all trust material; zero mock classes in release.

### P2 — Trust model rebuild

- **NEW-S2-1 IACA→DS path validation (C3).**
  1. Test PKI generated at test time: IACA root, DS leaf with profile extensions (BasicConstraints CA:FALSE, KeyUsage digitalSignature, EKU per profile).
  2. Go: `x509.Verify` with `Roots` set to the trust-store IACAs and explicit `KeyUsages`. Key pinning stays as a second check.
  3. Replace the mint's self-signed DSC (`issuer/mint_ee_poa.py:176-204`) with an IACA-signed one. This changes DSC bytes; the transcript vectors are golden (AGENTS.md), so regenerate deliberately and re-verify with the pinned requirements.
  4. Negative vectors refused in both engines with a stated reason: expired DS, wrong EKU, DS not under IACA, pathLen violated, self-signed.
  Judge: the negative set passes in both engines; positives accepted. Size L.
- **NEW-S2-2 Trust domains (C5).** Separate static anchors from LOTL-derived anchors (`CertificateChainValidator.kt:123-128`). Constrain LOTL-derived intermediates with RFC 5280 name constraints. Filter LOTL entries by trusted-list service type and status (ETSI TS 119 612 [UNVERIFIED revision]). Judge: a LOTL-only anchor cannot validate a reader certificate under static-only policy. Size M.
- **NEW-S2-3 Index by key digest (C9).** Key the Go trust store by SHA-256 of the DS SPKI as well as doctype. Match in the clear, then run ZK once per presentation. Remove `maxIssuersPerDocType` (`trust.go`). Judge: with 50 issuers per doctype, one ZK run per presentation; existing `trust_test.go` still passes. Size M.
- **NEW-S2-4 Differential suite (C4).** Shared vector set under `tests/vectors/trust/`, run by Go and Kotlin, outcomes compared per case. Extends 14.1 to the verifiers. Judge: any disagreement fails CI. Size M.
- **P2 exit.** Items C3, C4, C5 and C9 closed by tests in both engines.

### P3 — Status and revocation

- **NEW-S3-1 Revocation policy (C1), after D1.** Verified-CRL revocation hard-fails (already true). Stale CRL past `nextUpdate` fails closed for issuer and reader trust. Soft-fail only for an unreachable CRL, with a bounded grace period and a log line. Rewrite 10.4's acceptance text to match. Judge: 10.4 fixtures: revoked refused, blackholed refused, stale refused. Size M.
- **NEW-S3-2 Token Status List verifier (C2).** Go first, then Kotlin. Fetch the list; verify the JWT (`typ: statuslist+jwt`, ES256) against a trust-store key for the status issuer; honour `ttl` and `exp`; read the index bit; fail closed when stale; cache by `ttl`. Reference: IETF Token Status List draft [UNVERIFIED current revision]. Judge: revoked index refused, tampered list refused, stale list refused, and the mock URI unreachable in release. Size L.
- **NEW-S3-3 Wire into issuance and WUA.** Status checked on each presentation, depending on 10.1 (real issuer) and 10.3 (WUA). Judge: 10.4 end-to-end run against the real list. Size M.
- **P3 exit.** C1 and C2 closed; FR58 satisfied.

### P4 — Key custody

- **Needs first:** D3 (custody path) and D4 (HSM/KMS spend).
- **NEW-S4-1 Android custody (C10).**
  1. Establish which manager signs in production (`SecurityModule.kt:35` against `SecureAreaKeyManager`).
  2. Generate signing keys non-exportable in AndroidKeyStore or StrongBox, bound to user authentication.
  3. Verify key attestation chains to the Google root, requiring a stated security level and patch level.
  4. Delete the path not chosen.
  Judge: on a device, key export fails; the attestation verifier refuses a software-level fixture. Device access unknown (see What it needs). Size L.
- **NEW-S4-2 Issuer key custody (C7/C9 issuer side).** Keep the PoC mint's in-process key only behind an explicit test mode, with the PEM encrypted (`issuer/mint_ee_poa.py:285-289`). Production: KMS or HSM via PKCS#11 or a cloud KMS, signing through a remote-signing API such as the CSC API, with a key-ceremony record. Judge: production issuer path contains no file-based key; the mint refuses an unencrypted key outside test mode. Size M, plus spend.
- **Dependency.** The 13.1 inventory names the custodian for every key.
- **P4 exit.** No exportable signing key on any production path; attestation verified.

### P5 — Transparency and supply chain

- **Parse before trust (C12).** Stories 4.2–4.6 are prerequisites for reader auth and CT. 4.5 property tests include expired, revoked and untrusted-anchor certificates.
- **CT (C12).** Story 11.6: SCT verification with `CT_01`–`CT_06` vectors. An unreachable log has a stated behaviour. Whether Estonian access certificates are CT-logged stays recorded as unresolved.
- **Mirror (C13).** Extend 11.8: every file under `.github/maven-mirror` gets a checksum and an upstream licence. Check whether `gradle/verification-metadata.xml` covers the mirror before adding anything. Replace the `cose-java` `WALT-SNAPSHOT` with a pinned release or a fork. SLSA-style provenance for mirror builds [UNVERIFIED applicability].
- **P5 exit.** CT rows updated from evidence; mirror inventory complete with no SNAPSHOT in crypto dependencies.

### P6 — Assurance and regression

- 11.4 external security review, scoped to P2–P4 items.
- 11.5 game day adds drills: revoked, blackholed and stale status; key loss.
- Recurring PKI audit: re-run this critique each release. Each open item becomes a failing test or a tracked story. The judge is the diff between runs.
- PQC: watch ML-DSA certificate profiles (NIST FIPS 204 is final; profiles still settling [UNVERIFIED]). Crypto-agility comes from the S1-2 policy file. No build work.
- Expiry gate from S1-5 stays on in CI.

## Decisions for the maintainer

Each has a recommendation. Phases that depend on one do not start until it is answered. Route these through `/tiltrank` per the maintainer's usual practice.

- **D1 Stale or unreachable CRL.** Recommend fail-closed for issuer and reader trust; soft-fail only for unreachable with a logged grace period. Conflicts with 10.4's current text.
- **D2 Status-list host.** Recommend a controlled host under the fork's own pages, and retire the `aarmam.github.io` URI in mocks. Test lists stay visibly test-only.
- **D3 Android custody path.** Recommend AndroidKeyStore or StrongBox; retire the wrapped PKCS#12 path.
- **D4 HSM or KMS vendor and budget.** Spends money; needs approval.
- **D5 History purge of key material.** Rewrites history on a public fork; needs approval. Rotation happens regardless.
- **D6 Publication.** AGENTS.md says the fork is public. This plan names open security gaps. Recommend publishing after P0 closes, or keeping the security detail out of the public tree.

## What it needs

Already present: the repository; Go and Kotlin test suites (`trust_test.go`, `CertificateChainValidatorTest.kt`); the mint for generating test PKI; `tests/vectors/`; CI workflows.

Missing:
- HSM or KMS (D4).
- A real issuer and a live CRL and status-list endpoint (10.1, 10.3).
- ISO/IEC 18013-5 reference vectors [UNVERIFIED availability].
- An Android device with StrongBox and attestation support [UNVERIFIED availability].
- Counsel for 13.1.
- An external security reviewer (11.4).

## Verification limits

The critique findings come from grep and targeted reads; no test was run. Standard revisions named here come from memory and are marked `[UNVERIFIED]`. The `NEW-*` IDs are proposals; add them to `epics.md` and `sprint-status.yaml` after D1–D6 are decided.

## Next

P0-1 and P0-2 can start with the access present. P0-2's purge step waits for D5; the scan and rotation steps do not.
