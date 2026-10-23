# Innovation assessment — fork of `open-eid/eudi-wallet-poc` with a zero-knowledge proof-of-age spine

**Research date:** 23 October 2026 (Europe/Tallinn). **Author:** research pass commissioned in this
repository. **Version:** 1.0.

**Subject:** `github.com/tomkabel/eudi-wallet-poc`, branch `ee-eudiw-zk-import`, HEAD
`2d38febab0a89857a759485b3465dc218d2510c6` (12 October 2026), fork of
`github.com/open-eid/eudi-wallet-poc` at `0866e946874f666caa40e85ac8fc8fb22d71fc0` (v0.6.6,
26 May 2026).

**Companion documents:** the Estonia alignment assessment
([`2026-10-23-estonia-alignment-assessment.md`](2026-10-23-estonia-alignment-assessment.md)) and the
shared appendices ([`2026-10-23-shared-appendices.md`](2026-10-23-shared-appendices.md) — competitive
matrix A1, requirements traceability matrix A2, roadmap A3, risk register A4, source register A5).

**Evidence labels used throughout.** `[VERIFIED]` a tool output or a primary source quoted in this
repository or a fetched document. `[JUDGEMENT]` the author's analysis over verified facts.
`[ESTIMATE]` a number produced by a stated method with stated assumptions. `[ASSUMPTION]` an
unverified premise the conclusion depends on. `[UNVERIFIED]` could not be established.
`[REC]` recommendation.

---

## 1. Executive summary

The fork is not a copy of the Estonian PoC with extra commits. It is a roughly 3.5-week build
(18 September – 12 October 2026; 216 commits over the upstream release, 480 files, +62,578/−2,286
lines) `[VERIFIED]` that turns the upstream Android demo into a four-component system — Android
wallet, Go verifier, Python issuer, Python holder — around one narrow, unusually well-specified
claim: **an ISO 18013-5 mdoc age predicate proved with a Longfellow zero-knowledge proof, carried in
OpenID4VP, with the verifier and the wallet both refusing to accept less.** `[VERIFIED]`

Five findings drive this assessment.

1. **The fork's carrier is the Commission's own ZKP dialect.** The fork's `mso_mdoc_zk` profile
   (v1.0, 25 September 2026) fixes a DCQL format identifier `mso_mdoc_zk`, a `meta.zk_system_type`
   registry, the proving system `longfellow-libzk-v1` and the `circuit_hash` parameter
   `[VERIFIED]` — and the Commission's **TS13** ("ZKP based on arithmetic circuits", v1.0,
   15 December 2025) specifies exactly `"format": "mso_mdoc_zk"`, `"system": "longfellow-libzk-v1"`,
   `params.circuit_hash`, plus a `ZkDocument` CDDL matching the fork's documented response shape
   `[VERIFIED]`. The `zk_system_type` field names in the fork's Go verifier and Kotlin wallet
   (`verifier/go/oid4vp/dcql.go`, `app/src/main/kotlin/ee/cyber/wallet/util/DeviceRequestParser.kt`)
   are the same strings. This is not a private convention: it is the emerging EU profile.
   `[VERIFIED]`

2. **The EU's own age-verification blueprint mandates exactly the scheme the fork already runs.**
   The Commission's AV Profile (Annex A, `av-doc-technical-specification`, repository pushed
   2 September 2026) fixes the doctype `eu.europa.ec.av.1`, the single mandatory attribute
   `age_over_18`, the ZkSystemSpec parameters `circuit_hash, version, num_attributes, block_enc_hash,
   block_enc_sig`, transport of the proof over the W3C Digital Credentials API, and states that
   "the only Zero-Knowledge Proof system in scope is the system identified by `longfellow-libzk-v1`
   … An implementation SHALL use that system." `[VERIFIED]` The fork already mints and consumes
   `eu.europa.ec.av.1`, keys its age policy on it, and prefers it to its own Estonian attestation
   `[VERIFIED]`. The gap between the fork and the EU's age-verification blueprint is a
   conformance-test and packaging gap, not a cryptographic redesign.

3. **The market gap is verifier- and relying-party-side, not wallet-side.** Of ten verified
   comparators ([Appendix A1](2026-10-23-shared-appendices.md#a1)), none combines ZKP age proofs
   with a Go/Rust verifier and a published negative-control test suite: the Commission's reference
   wallet documents ZKP as "an optional feature", multipaz ships the prover library but not a
   verifier product, walt.id, EUDIPLO, inji, wwWallet and impierce claim no ZKP at all
   `[VERIFIED]`. The distinctive artefact here is the **verifying** side plus its refusals.

4. **The honest-measurement discipline is itself the smallest-risk differentiator.** The fork
   publishes host measurements that contradict its own budgets (`EE-ZKP-040` ≤ 1.0 s p95; measured
   p95 4.91 s on an i5-8365U), and it documents a defect that inverted an earlier claim — that
   refusing an unaccepted circuit cost 54–58 s for one circuit version instead of milliseconds —
   together with the 24 September 2026 fix `[VERIFIED]`. A funding or procurement evaluator cannot
   easily manufacture that; it is the difference between a demo and evidence. `[JUDGEMENT]`

5. **The fork's weakest position is upstream abandonment and standardisation timing.** Upstream had
   one commit in the last 12 months, 0 open issues and PRs, 3 stars and a single contributor as of
   23 October 2026 `[VERIFIED]`; ISO/IEC 18013-5 second edition — which defines the `ZkRequest` /
   `ZkDocument` structures the whole approach leans on — has an expected publication date that has
   slipped from Q3 2026 to Q1 2027 per the Commission's own tracker `[VERIFIED]`, and ZKP was
   explicitly removed from the DCHP initial proposal, leaving OpenID4VP issue #627 open for ~16
   months `[VERIFIED]`. Every innovation below is therefore a bet on an unfinished standard. The
   mitigation is not to wait but to be the implementation that a standardisation venue tests
   against, which is what the carrier profile and fixtures position the fork to be.

**Strongest differentiation opportunities, ranked** (§5): O1 EU AV-Profile conformance kit
(priority 4.75/5), O2 Estonia relying-party acceptance kit (4.30), O3 registration-certificate
policy engine (4.10), O4 circuit-hash registry service (4.05), O5 `mso_mdoc_zk` carrier conformance
suite (3.95), O6 statutory age-threshold toolkit (3.80). **For an initial release, take O1, O5 and
O2 in that order**: O1 is the only one that converts existing code into an EU-recognised capability,
O5 is nearly complete already, and O2 is the only one whose value a Estonian buyer can act on before
the national wallet ships in 2028.

---

## 2. What the fork is, against upstream

Detail and sources: [Appendix A1](2026-10-23-shared-appendices.md#a1) and
[source register A5](2026-10-23-shared-appendices.md#a5).

| dimension | upstream `open-eid/eudi-wallet-poc` | this fork |
|---|---|---|
| licence / visibility | MIT, public, 3 ★, 1 contributor | MIT, public (fork visibility is fixed at creation), 0 ★ |
| last activity | 2026-05-26 (v0.6.6); 1 commit in 12 months; 0 open issues/PRs `[VERIFIED]` | 2026-10-12; 216 commits ahead of upstream `master`, 0 behind `[VERIFIED]` |
| components | Android wallet app (`app/`) | app + Go verifier (`verifier/go`, 47 files / 11,225 lines) + Rust FFI staticlib (`verifier/zkverify-ffi`, 215 lines) + Python issuer (542 lines) + Python holder (469 lines) + stdlib sandbox server (644 lines) + JVM conformance module (`zk-conformance`, 699 lines) `[VERIFIED]` |
| protocols | OpenID4VP/siop via `eu.europa.ec.eudi` libraries, OpenID4VCI, mdoc, proximity reader | the same libraries (siop-openid4vp 0.11.1, sdjwt 0.12.0, openid4vci 0.6.1, prex 0.4.0), plus multipaz 0.99.0 and `mso_mdoc_zk` with a dual-carrier (interim JSON + de-facto CBOR `DeviceResponse`) verifier `[VERIFIED]` |
| ZKP | none | Longfellow prover (`ee_poa_demo`, built from a pinned `../longfellow-zk`), Go verifier over the Rust staticlib, circuit allowlist, `ZkDocument` carrier, on-device prover via `multipaz-longfellow` `[VERIFIED]` |
| attestations | upstream PID/mDL mocks | plus `ee.riik.poa.1` (fork's Estonian proof-of-age) and `eu.europa.ec.av.1` (the Commission's AV Profile doctype) `[VERIFIED]` |
| specification | none in-tree | `spec/EE-EUDIW-TS-1.0.md`, 284 KB, 211 numbered requirements across 14 areas (EE-ZKP 147, EE-POA 136, EE-SEC 62, EE-PRO 59, EE-PRI 35, EE-SIG 30, EE-GOV 27, EE-PID 26, EE-ONB 22, EE-REV 19, EE-WUA 18, EE-RP 17, EE-CNF 17, EE-ACC 7) `[VERIFIED]`, revision 1.7 dated 12 October 2026 |
| tests | upstream unit tests | 27 Kotlin unit-test files / 169 `@Test`, 5 JVM conformance tests, 13-case `tests/e2e.sh`, drain and load harnesses, committed cross-language transcript vectors and `tests/vectors/carrier-v1/` `[VERIFIED]` |
| "green" | upstream CI | `make check` — gofmt, `go vet`, `compileall`, ruff, shellcheck, `go test -race` (oid4vp, internal/cborsub, internal/jose, circuits), `wallet/test_transcript.py` (3 golden vectors), `issuer/test_mint_device_key.py`, `sandbox/test_age.py` — re-run in a venv with the pinned inputs (`cbor2==6.1.4`, `cryptography==50.0.1`, `jwcrypto==1.6.1`) on 23 October 2026: **exit 0** `[VERIFIED]` |

**Inherited, fork-specific, planned, missing — kept separate.**

- *Inherited from upstream (not a differentiator):* the wallet application skeleton, the
  `eu.europa.ec.eudi` JVM library integration, the mock PID/issuance services, the proximity reader,
  the `app/` build system. Upstream's `master` is an ancestor of the fork's HEAD
  (`git merge-base --is-ancestor` verified), so nothing upstream was traded away `[VERIFIED]`.
- *Fork-specific and verified in code or tests:* the ZK spine (prover, verifier, FFI, circuits
  registry, LLVM-free allowlist), the carrier profile and dual carriers, `direct_post.jwt` with an
  in-house minimal JWE (ADR-003), the EE-PoA and AV attestation paths, hardware-backed
  SecureArea keys, LOTL fail-closed trust with CRL soft-fail, on-device `issuerAuth` verification,
  the measurement harness, and the specification itself `[VERIFIED]`.
- *Documented plans:* the roadmap items in the specification §21 and the step records in
  `docs/planning/`; the DCHP #17 submission text prepared but **unfiled**
  (`docs/profiles/DCHP-17-ZKP-SUPPORT-ISSUE-TEXT.md`) `[VERIFIED]`.
- *Missing (verified absent, not merely undocumented):* no wallet-provider certification path
  (`CIR (EU) 2024/2981` / the Commission's FCAF at `conformance.eudi.dev`), no RP registration
  certificate consumption beyond `ClientIdPrefixPolicy`, no proximity path for Tier-2 ZK, no
  end-to-end real-issuer integration (issuance is served by mocks and the demo issuer), and no
  published interoperability run against the Commission's reference wallet `[VERIFIED as absent
  from the repository; a claim that no such run exists anywhere would be [UNVERIFIED].`

**One documentation/code divergence, found during this assessment.** `docs/CONFORMITY.md:160`
still records on-device `issuerAuth` validation as "absent — mdoc credentials load with no
`issuerAuth` chain check", while `CredentialToDocumentMapper.kt:82-114` performs exactly that check
(leaf-signature validation through `org.cose` plus PKIX chain anchoring at the configured IACA roots)
and `CredentialToDocumentMapperMdocVerificationTest.kt` asserts both the anchored and the
unanchored case `[VERIFIED]`. The conformity note is stale on this row even though the file was
touched by the HEAD commit; entries that contradict the code do not help the fork's credibility
argument and this one should be corrected before the note is shown to an evaluator. `[REC]`

---

## 3. Method: how candidates were found, scored and ranked

**Candidate generation.** The comparator set was selected for overlapping users, capabilities or
integration role, then verified field by field from repository READMEs, in-repo documentation and
the GitHub API ([Appendix A1](2026-10-23-shared-appendices.md#a1)). Ten comparators were verified;
the shortfall risk the task warns about does not arise — every selected project is an implementation
or specification artefact in the EUDI/OpenID4VC space, and each is either a wallet, a wallet
library, a verifier, an issuer/rp middleware, or the standard itself. Complementary components
(multipaz, EUDIPLO, impierce/openid4vc, the Commission's TS/AV documents) are labelled as such and
not counted as direct alternatives.

**Novelty framing.** Every claim of novelty below is scoped to this comparison set — the fork, its
upstream, the ten comparators, and the EU specification artefacts fetched on 23 October 2026. A
broader novelty claim ("nobody anywhere has done this") is not supported by the evidence and is not
made. Where a comparator's capability is *unknown* rather than absent, the matrix says "not
verified" and the opportunity is scored down rather than asserted as unique. This matters
concretely: a README silence about ZKP is not proof that a project has none, so the differentiation
scores below rest on artefacts that were read, not on silences.

**Scoring rubric (1–5, applied independently by the author from the evidence in §2 and A1).**

| score | user impact | technical differentiation | implementation feasibility | Estonia relevance |
|---|---|---|---|---|
| 5 | unblocks a regulated transaction that cannot be completed today, or removes a disclosure with legal consequence | no comparator in the set does it, and it is not a copy of a public artefact | ≤ 2 engineer-weeks reusing existing components, evidence already committed | on a documented Estonian milestone or obligation (RIA / Ministry statement, or the spec's own roadmap) |
| 4 | materially shortens a workflow or removes a disclosure users would notice | one comparator partially does it | 1–2 engineer-months | supports an Estonian obligation indirectly |
| 3 | saves effort for integrators | several comparators do a weaker version | 3–6 engineer-months | useful; not on the critical path |
| 2 | measurable but minor | commodity | 6–12 engineer-months | no Estonian driver found |
| 1 | cosmetic | already standardised and freely available | blocked on a third party nobody here controls | irrelevant to Estonia |

**Ranking method.** priority = 0.35·impact + 0.25·differentiation + 0.20·feasibility + 0.20·Estonia
relevance. Weights favour user impact and differentiation because the fork is a research artefact
whose value is its evidence, not its throughput; feasibility is held at 0.20 so that
externally-blocked work cannot reach the top by being cheap to describe.

**Evidence confidence is reported separately from the score**, because a high score on thin evidence
is a different risk from a high score on measured facts. Confidence is **high** when the enabling
capability is verified in code or tests *and* the external driver is a fetched primary source,
**medium** when one of the two is missing, and **low** when the driver is a delegated research
pass, a press report, or a draft standard that may yet change.

---

## 4. Opportunities

### O1 — EU AV-Profile conformance kit: `eu.europa.ec.av.1` + `longfellow-libzk-v1` over the DC API

**Target user.** A national wallet programme (Estonia's RIA procurement team), an attestation
provider issuing proof-of-age, or a relying party that must accept the EU age-verification blueprint
and wants to test against a real implementation rather than a description.

**Problem and evidence.** The Commission's AV Profile is normative, not exploratory: the doctype is
`eu.europa.ec.av.1`, the attribute set is `age_over_18` (mandatory) plus optional `age_over_NN`, "A
Proof of Age Attestation SHALL NOT include any other attribute", the ZKP system in scope is
`longfellow-libzk-v1` and "An implementation SHALL use that system", the proof is transported over
the W3C Digital Credentials API, and the parameters are the ZkSystemSpec tuple at ISO/IEC DIS
18013-5 second edition §10.2.7 `[VERIFIED]`. The fork today implements all of the cryptographic and
carrier parts and the *Estonian* profile around them, but there is **no conformance test suite, no
profile-conformance statement against the AV document, and no published interop run against the
Commission's or any third party's AV implementation** `[VERIFIED as absent]`. A programme that
adopts the EU blueprint therefore cannot reuse the fork's evidence as-is; it would rebuild the test
oracle.

**Existing approaches and their limits.** The Commission publishes the AV documentation and a
development environment (`ageverification.dev`) and an open-source implementation of the proving
library is referenced from the profile (`google/longfellow-zk`, now `longfellow-zk/longfellow-zk`,
Apache-2.0, v0.9 31 March 2026) `[VERIFIED]`. What is not visible in any fetched artefact is an
independent, negative-control conformance kit for the *combined* profile — the tests that
demonstrate a wrong `circuit_hash`, a `false` predicate, a replayed proof or an unanchored
`issuerAuth` are refused. The fork already contains those tests for its own carrier
(`verifier/go/vptoken_test.go`, `tests/e2e.sh` cases 2/4/5/9/10) `[VERIFIED]`; they are simply not
generalised to the AV document's parameter names and published as a conformance artefact.

**Proposed capability.** A `av-profile-conformance` package in this repository with (a) a
machine-readable statement of each AV-Profile normative clause with a test id, (b) fixtures minted
against `eu.europa.ec.av.1` with the AV Profile's `ZkSystemSpec` parameter tuple, including the
container-format examples from the profile's Annex A, and (c) a runner that produces a pass/fail
report per clause for any candidate wallet, issuer or verifier. Affected components: the Go verifier
(query parsing and required-parameter validation), the issuer (attestation data model and doctype
registry), the wallet (DC API request handling), and `tests/vectors/`. Interfaces: DC API
(`navigator.credentials.get` with the `org-iso-mdoc`/`zk` protocol entry), the AV profile's
`av-vci://` invocation, and the AVSUA-equivalent attestation the profile expects at issuance
(the fork's spec already carries `EE-WUA-008` for this) `[VERIFIED]`. Dependencies: nothing outside
the repository; the ISO second-edition clause numbering is reached through the AV Profile's own
citation rather than through ISO, whose site could not be fetched through the available tooling
`[UNVERIFIED]`.

**Distinction from upstream and verified competitors.** Upstream has no ZKP at all
`[VERIFIED]`. Within the comparator set, only `eudi-lib-android-wallet-core` documents ZKP — as "an
optional feature that can be enabled in your application" `[VERIFIED]` — and no comparator publishes
a negative-control conformance kit over `longfellow-libzk-v1`. The distinction is therefore not the
scheme (the scheme is mandated by the EU, not invented here) but the *verification evidence*: the
fork would be the first artefact in this set that lets a third party check a claim of AV-Profile
conformance rather than assert it.

**Acceptance criteria.** (i) Technical differentiation: the kit runs against at least two
independent implementations (this fork's verifier and at least one external wallet or verifier) and
reports a clause-by-clause result; (ii) functionality delivered: ≥ 20 named clauses executed, of
which ≥ 8 are negative controls that fail closed; (iii) gap addressed: a third party reproduces the
report from a clean checkout using only documented commands, in one sitting.

**Implementation approach, effort, dependencies.** Reuse `verifier/go/oid4vp/*` and the fixture
generator; add the clause registry and the AV-specific fixtures; document the run. **Effort: 3–6
engineer-weeks** — `[ESTIMATE]` derived from the size of the existing comparable slice (the
carrier profile and its dual-carrier fixtures, `verifier/go/oid4vp/carrier.go` plus
`tests/vectors/carrier-v1/`, were built inside a 216-commit, 3.5-week campaign; this work is a
generalisation of that slice, not a new one). Technical justification: the ZK path, circuits, and
carriers already exist and pass tests; the new work is measurement and packaging, which is the kind
of work this repository has demonstrably been able to finish.

**Benefits, risks, validation.** Benefit: converts existing code into a capability an EU
programme can procure against, with no cryptographic risk. Risks: the AV Profile is revised
(the fork's own spec records a 1 September 2026 revision `[VERIFIED]`), so the kit must pin a
document version and treat clause drift as a maintenance cost; and the ISO second-edition text is
not reachable, so parameter semantics are taken at one remove. Validation: run the kit against the
Commission's AV development environment and publish the result, including failures.

**Scores:** impact 5, differentiation 5, feasibility 4, Estonia relevance 5 → **priority 4.75**.
**Evidence confidence: high** (both the enabling capability and the external driver were read
directly).

---

### O2 — Estonian relying-party acceptance kit: accept EUDI wallets before the national wallet exists

**Target user.** Estonian e-service operators and their integrators — the public-sector services
that must accept certified wallets from end-2026, and the private-sector services under the later
obligation.

**Problem and evidence.** Estonia's own wallet launch is planned for **2028** ("Eesti digikukru
avalik kasutuselevõtt on kavandatud 2028. aastasse"), while "Juba 2026. aasta lõpust peavad Eesti
riiklikud e-teenused olema valmis aktsepteerima Euroopa Liidu liikmesriikide sertifitseeritud
digikukruid", with RIA's TARA as the stated central integration point and the private sector from the
end of 2027 `[VERIFIED]`. RIA confirms three Member States were ahead and had applied for
cross-border approval as of May 2026 `[VERIFIED]`. The consequence is a window in which Estonian
relying parties must accept *other Member States'* wallets with no Estonian wallet to test
against, and no published Estonian RP-side reference implementation `[VERIFIED as absent from the
fetched RIA pages and the `open-eid` organisation]`. The Relying Party Register itself is at MVP
stage with its access-certificate solution still being designed, and no technical specification or
API for it is published `[VERIFIED]`.

**Existing approaches and their limits.** EUDIPLO is the closest verified comparator — open-source
middleware bridging a backend to EUDI wallets, OpenID4VCI 1.0 / OpenID4VP 1.0, OIDF conformance
tested, with mdoc and SD-JWT VC both marked supported and proximity explicitly out of scope
`[VERIFIED]`. Using it does not answer the Estonian question, because it neither speaks TARA nor
consumes the Estonian RP register, and it is a dependency rather than an artefact a service owner
can inspect for its own refusals.

**Proposed capability.** An RP-side integration kit: a thin OpenID4VP 1.0 / DC API relying-party
service built from this fork's Go verifier, plus (a) a TARA-facing adapter contract, (b) the
RP-register lookup as a pluggable, fail-closed interface with a documented "no entry ⇒ refuse"
default, and (c) an acceptance test suite covering the four requests an Estonian service will
actually meet: a foreign wallet with a PID, a foreign wallet with an AV attestation, an unattested
ZKP request (the least-trusted case the fork's spec already governs in `EE-ZKP-055a` `[VERIFIED]`),
and a request whose RP registration is missing. Affected components: `verifier/go/oid4vp`,
`verifier/go/present.go`, and a new `rp-kit/` deployment profile. Interfaces: OpenID4VP 1.0
`direct_post.jwt`, the DC API, TARA's OIDC surface (the fork's spec records TARA as the Estonian
authentication broker `[VERIFIED]`), and the RP register API — **which does not exist in published
form, so the kit must define the interface and ship a mock** `[VERIFIED as absent]`.

**Distinction from upstream and verified competitors.** Upstream has no verifier at all — the Go
verifier is fork-specific `[VERIFIED]`. EUDIPLO covers the protocol bridge but not the Estonian
institutional layer. No comparator in the set was found to ship a TARA-side or register-aware
acceptance kit `[VERIFIED for the ten comparators]`. Within the researched set, this is the only
opportunity whose buyer already has a dated obligation and no product.

**Acceptance criteria.** (i) A sample Estonian-style service completes all four request cases with a
documented, machine-readable result; (ii) the missing-registration and unattested-request cases
refuse, and the refusal is distinguishable from cancellation in the UI contract (the fork's own
`DcApiRequestDispatch` work shows why this matters); (iii) an integrator with no prior ZK knowledge
installs the kit from the README in under a day and reproduces the four results. Timed.

**Implementation approach, effort, dependencies.** Fork the verifier into a service profile, add the
plug interfaces and mocks, write the acceptance suite. **Effort: 6–10 engineer-weeks**
`[ESTIMATE]`, method: the verifier is 11,225 lines of Go including tests; the new work is
configuration, two pluggable interfaces with mocks, and four end-to-end cases — roughly the size of
the existing dual-carrier slice plus the sandbox deployment work in the commit history. Dependencies:
RIA's RP register interface (unpublished), TARA integration terms, and a test wallet on the
Estonian side. All three are external; the kit is designed so that the mocks let the work proceed
without them, and the first external dependency to become real replaces a mock, not a design.

**Benefits, risks, validation.** Benefit: it lets Estonian services run acceptance tests years
before the national wallet ships, and it gives RIA a working target for the register interface.
Risks: the register's eventual specification may differ from the mock, and the kit could be
superseded if RIA publishes its own. Mitigation: publish the interface contract as a proposal, which
is useful even if superseded. Validation: run the four cases in front of an Estonian service owner
and measure time-to-first-successful-verification.

**Scores:** impact 5, differentiation 4, feasibility 3, Estonia relevance 5 → **priority 4.30**.
**Evidence confidence: high** on the obligation dates, **low** on the register interface's final
shape.

---

### O3 — Registration-certificate-aware request policy engine

**Target user.** Wallet providers and relying parties that must act on the relying party's
registered intended use rather than on the request's own claims.

**Problem and evidence.** The fork's own specification refuses requests that would disclose more
than one age predicate *unless* "the relying party's registered intended use names each of them",
and records plainly that the wallet does not read registration certificates, so such a request is
"always refused here" `[VERIFIED]` (`AgePredicatePolicy.kt`, `EE-POA-030`). Two independent
consequences follow: the wallet is stricter than ISO/IEC 18013-5 §7.2.5 (a deliberate divergence the
spec documents in §22) `[VERIFIED]`, and it cannot distinguish a legitimate two-predicate request
from an over-reaching one. At the same time the ARF's Topic 19 requires a presentation log to
contain the relying party's trade name, unique identifier, service identifier, Member State,
intended use and privacy-policy URL — information that lives in the registration and access
certificates the fork does not yet read `[VERIFIED]`. The Estonian RP register is being built with
exactly this rule ("tohivad küsida ainult registreeringus märgitud andmeid") `[VERIFIED]`.

**Existing approaches and their limits.** Registered-intended-use enforcement is being placed in
the ecosystem's registries (the ARF's RP registration topics, the Estonian register), not in
wallets; a wallet therefore inherits the obligation without an implementation to copy. No comparator
in the set was found to ship an intended-use policy engine `[VERIFIED for the ten comparators]`. The
fork's `ClientIdPrefixPolicyTest` shows the shape of the work — a small, testable policy class over
client identifiers — applied to a much weaker signal than a certificate.

**Proposed capability.** A policy module shared by the wallet and the verifier that (a) parses the
registration/access certificate chain, (b) exposes a typed `RegisteredIntendedUse`, (c) drives the
existing age-predicate decision and the Topic 19 log fields from it, and (d) fails closed when the
certificate is absent, unparseable, or names no use. Affected components: `app/.../domain/presentation/`
(`AgePredicatePolicy`, `HolderObligations`, presentation screens), `verifier/go/oid4vp` (request
authentication), `certificates` parsing in `app/.../security/`. Interfaces: the ARF's registration
certificate profile, ISO 18013-5 `readerAuth`, and the Topic 19 log contract.

**Distinction.** Upstream has nothing comparable; the fork today has a conservative approximation
and says so `[VERIFIED]`. The differentiation is precision under a legal rule, not a new protocol.

**Acceptance criteria.** (i) A two-predicate request bearing a certificate whose intended use names
both predicates is allowed, with a user-facing explanation of the revealed range; (ii) the same
request without a certificate, or with one that does not name both, is refused; (iii) the
presentation log contains the Topic 19 fields populated from the certificate, verified by a test
asserting each field; (iv) no regression in the existing 169 Kotlin unit tests.

**Implementation approach, effort, dependencies.** Introduced as an interface first
(`IntendedUseSource`) with the certificate implementation and a fixture generator second, so that
the Estonian register or the EU registries can be plugged in without redesign. **Effort: 8–14
engineer-weeks** `[ESTIMATE]`, method: certificate parsing and policy work of similar shape in this
repository (the LOTL/CRL trust work and the `ClientIdPrefixPolicy`) was completed within the
3.5-week campaign; this is larger because it introduces a data model. Dependencies: a reachable
registration-certificate profile (ARF annexes) and, for the Estonian case, the future register API.

**Benefits, risks, validation.** Benefit: replaces a blanket refusal with a lawful, explainable
decision, and satisfies a documented ARF logging duty at the same time. Risks: certificate profiles
in the ARF are still moving; a mis-parsed intended use could *loosen* a privacy control, so the
default must be refusal and the loosening must be covered by explicit negative tests. Validation:
property tests over generated certificates, plus a manual review of the log output against the Topic
19 field list.

**Scores:** impact 5, differentiation 4, feasibility 3, Estonia relevance 4 → **priority 4.10**.
**Evidence confidence: medium** — the need is verified from a primary ARF source, the wallet-side
absence from the repository, but the certificate profile's stability is not.

---

### O4 — Circuit-hash registry and distribution service

**Target user.** Wallet providers, relying parties and programme owners who must agree on which
circuits are acceptable, and auditors who must answer "which circuit proved this?" months later.

**Problem and evidence.** Both the Commission's TS13 and the AV Profile require the prover system
and the circuit to be identified by parameters — `circuit_hash` in TS13's ZkSystemSpec, and the
tuple `circuit_hash, version, num_attributes, block_enc_hash, block_enc_sig` in the AV Profile
`[VERIFIED]` — while the AV Profile states that "the set of circuits accepted for the purposes of
this profile, identified by their circuit hashes, is published and maintained by the scheme owner
separately from this document" `[VERIFIED]`. In other words the standard deliberately leaves a
distribution and trust problem to the ecosystem. The fork today solves it locally: an allowlist
compiled into the verifier (`verifier/circuits.json`, `verifier/go/circuits/`, `GET /circuits`), a
per-doctype trust-store scoping, and a `Circuit.SpecID()` label format
`<system>_<version>_<num_attributes>_<block_enc_hash>_<block_enc_sig>_<circuit_hash>` `[VERIFIED]`.
Every operator doing this independently is how two parties end up accepting different circuits while
both believing they follow the same profile.

**Existing approaches and their limits.** Trust-list distribution in the EUDI ecosystem is handled
for providers (notification, trusted lists) but not for circuits; the Commission's specifications
name the gap and stop. No comparator in the set was found to publish a circuit registry service
`[VERIFIED for the ten comparators]`. longfellow-zk itself ships circuits with the library, so
"which circuits exist" is answerable only by reading a specific library version `[VERIFIED]`.

**Proposed capability.** A minimal, signed circuit registry: a static JSON document listing accepted
circuits (system, version, attributes, block-encryption parameters, hashes, minimum library
version), published with a signature and an HTTP endpoint the verifier and the wallet can fetch,
validate against a pinned key, and audit. Affected components: `verifier/go/circuits`, the verifier's
startup self-check, the wallet's `ZkPresenter` scheme resolution, and `docs/profiles/`. Interfaces:
the existing `GET /circuits` shape, promoted from a local list to a fetched, signed list with the
local list retained as a pinned fallback (fail-closed).

**Distinction.** Neither upstream nor any verified comparator ships this; TS13/AV name the need
`[VERIFIED]`. The novelty is small in code and large in interoperability effect — it converts a
per-operator convention into a checkable artefact.

**Acceptance criteria.** (i) A verifier started with no network and no local list refuses
everything (fail-closed), with a log naming the missing prerequisite; (ii) with a fetched and
signature-valid list it accepts exactly the listed circuits, demonstrated by a test that flips one
byte in the list and observes refusal; (iii) two independently operated instances configure the same
accepted set from one document; (iv) an audit query returns the circuit provenance for a given
presentation.

**Implementation approach, effort, dependencies.** JSON + signature + fetch/caching + a CLI to
issue the document. **Effort: 4–8 engineer-weeks** `[ESTIMATE]`, method: comparable to the existing
LOTL fetch/validate work (`FetchLOTLCertificates.kt`, `CertificateChainValidator`), which was
completed inside the campaign. Dependencies: none external, but adoption depends on a scheme owner
(the Commission or a Member State) agreeing to host a list — so ship it as a community registry with
a documented succession path.

**Benefits, risks, validation.** Benefit: removes an interoperability landmine, and gives auditors a
provenance answer. Risks: an unsigned or compromised list is a supply-chain hazard — mitigation is
signature verification with a pinned key and fail-closed behaviour; and maintaining a list nobody
adopts is dead weight — mitigation is to keep the document generator in-repo and cheap. Validation:
two-instance agreement test plus a tamper test.

**Scores:** impact 4, differentiation 5, feasibility 4, Estonia relevance 3 → **priority 4.05**.
**Evidence confidence: high.**

---

### O5 — `mso_mdoc_zk` carrier conformance suite (both carriers, published vectors)

**Target user.** Implementers of wallets and verifiers who must interoperate on a ZK carrier that no
OpenID specification yet defines.

**Problem and evidence.** OpenID4VP 1.0 Final (9 July 2025) contains no ZK text at all
`[VERIFIED — programmatic search of the 398,599-character specification text]`; the request to
extend DCQL for mdoc ZK proofs (OpenID4VP issue #627) has been open since 3 June 2025; and in DCHP,
"ZKP support was removed from the initial proposal" with the issue parked `[VERIFIED]`. Meanwhile
two carriers exist in the field — the interim JSON envelope and the CBOR `DeviceResponse` with
`zkDocuments` — and the fork already normalises both behind one interface with committed golden
vectors (`tests/vectors/carrier-v1/`, `TestCarrierTableBothCarriers`,
`TestGoldenProofNormalizesIdentically`, `TestCarrierVectorsVerifyIdentically`) `[VERIFIED]`. TS13
defines the CDDL for the `ZkDocument`, so the shape is now pinned by a Commission document even
though the transport binding is not.

**Existing approaches and their limits.** The Commission's own conformance site
(`conformance.eudi.dev`) publishes the FCAF for wallets, and its content is explicitly "under active
development" `[VERIFIED]`; nothing fetched covers the ZK carrier. Consumers therefore write their
own tests, which is why "which carrier" is a recurring integration surprise.

**Proposed capability.** Publish the existing vectors and normalisation tests as a small, documented
conformance suite with a runner: given a wallet's response, report which carrier it used, whether
the proof verifies, and which binding properties hold (client_id, nonce, response_uri, key
thumbprint, single use, predicate value). Affected components: `verifier/go/oid4vp/carrier.go`,
`present.go`, `tests/vectors/`, and a thin CLI. Interfaces: OpenID4VP `direct_post`/`direct_post.jwt`
and the DC API.

**Distinction.** This is packaging of existing, tested work; the differentiation is that the tests
are *negative* (a wrong nonce, a flapped bit, a replayed token, a plain response to an encrypted
session) rather than a happy-path demo — verified in `tests/e2e.sh` cases 2–5 and 9–10 `[VERIFIED]`,
and absent from every comparator's published material in this set `[VERIFIED]`.

**Acceptance criteria.** (i) The suite runs against this repository's wallet and verifier and
reports all 13 existing cases; (ii) at least one external implementation (the Commission's reference
wallet or multipaz sample) is exercised, with the result published whether it passes or fails;
(iii) a failing case names the binding property that failed, not a generic "invalid".

**Implementation approach, effort, dependencies.** Wrap `tests/e2e.sh` and the Go tests in a
documented runner; add the external target last. **Effort: 2–4 engineer-weeks** `[ESTIMATE]` — this
is the smallest opportunity because the tests exist. Dependencies: an external implementation to
test against (multipaz sample apps are available, Apache-2.0 `[VERIFIED]`).

**Benefits, risks, validation.** Benefit: cheap credibility; a published failure against a reference
implementation is more useful to a programme than another claim of success. Risks: the carrier may be
superseded by whatever ISO second edition and DCHP settle on, so the suite must be version-labelled.
Validation: run it, publish the report, including the external result.

**Scores:** impact 4, differentiation 4, feasibility 5, Estonia relevance 3 → **priority 3.95**.
**Evidence confidence: high.**

---

### O6 — Statutory age-threshold toolkit beyond 18

**Target user.** Attestation providers and wallet programmes in Member States whose law has several
age thresholds, and relying parties that must ask for one of them.

**Problem and evidence.** The Commission's AV Profile focuses on `age_over_18` and treats
`age_over_NN` as optional, while pointing at ISO/IEC 18013-5 §7.2.5 ("nearest 'true' attestation
above request") for implementations that use it `[VERIFIED]`. The fork's specification goes further
and stricter: at most one predicate per presentation, `false` predicates withheld, and the 28
February birth-date rule it derived from ISO §7.2.5, across an Estonian statutory annex
`[VERIFIED — `EE-POA-027`–`EE-POA-031`, spec Annex A]`. What does not exist anywhere in the fetched
material is a *set of accepted circuits* covering the thresholds a national law actually uses, plus a
negotiation library that turns "the relying party asked for 21" into the right single predicate
without ever disclosing a `false` one — which is exactly where wallets get it wrong.

**Existing approaches and their limits.** Circuits travel with the proving library; the AV profile's
accepted set is maintained "by the scheme owner … separately from this document" `[VERIFIED]`;
nothing fetched enumerates a multi-threshold set with its legal mapping.

**Proposed capability.** A documented threshold-to-circuit map for Estonia's statutory ages, an
accepted-circuit set for those thresholds (subject to O4's registry), and a small negotiation
library implementing §7.2.5's "nearest true above request" rule with the fork's stricter
single-predicate and withhold-false rules, with tests including the 28 February and incomplete-date
cases the spec already analyses `[VERIFIED]`.

**Distinction.** AV Profile covers 18; no comparator in the set was found to publish a
multi-threshold toolkit; the fork's own rules are stricter than ISO §7.2.5 and documented as a
deliberate divergence `[VERIFIED]`.

**Acceptance criteria.** (i) For each mapped threshold, a proof verifies only for a holder whose age
meets it, demonstrated by table-driven tests at the boundary (the day before, the day of, a leap-day
birth); (ii) a request for a threshold the wallet does not hold is refused with a user-visible
reason, never answered with a neighbouring threshold; (iii) no `false` predicate is ever transmitted,
asserted over all fixtures.

**Implementation approach, effort, dependencies.** Circuit parameters per threshold are the
long dependency: a circuit must be generated and accepted by the scheme owner, so this work is
partly externally gated. **Effort: 6–12 engineer-weeks** `[ESTIMATE]` plus an unbounded external
wait for scheme-owner acceptance; feasibility is scored 3 for that reason. Dependencies: O4's
registry; longfellow circuit generation; a legal review of the threshold list.

**Benefits, risks, validation.** Benefit: closes a real gap for Estonian services that are not
age-18-gated (alcohol and tobacco at 18/21, other restrictions at 16). Risks: extra circuits raise
prover cost and proof size (already ~353–360 KB and ~6.1 s proving `[VERIFIED]`), and legal mapping
errors are consequential — mitigation is to publish the legal mapping with its citations and treat
any unmapped threshold as unsupported. Validation: boundary tests per threshold plus a legal review
by the programme owner.

**Scores:** impact 4, differentiation 4, feasibility 3, Estonia relevance 4 → **priority 3.80**.
**Evidence confidence: medium** — the need and the rules are verified, the legal threshold list for
Estonia is analysis, and circuit acceptance is external.

---

## 5. Ranking and what to build first

| # | opportunity | impact | diff. | feas. | EE rel. | priority | evidence confidence | external blocker |
|---|---|---|---|---|---|---|---|---|
| O1 | EU AV-Profile conformance kit | 5 | 5 | 4 | 5 | **4.75** | high | none |
| O2 | Estonian RP acceptance kit | 5 | 4 | 3 | 5 | **4.30** | high (dates) / low (register API) | RP register interface, TARA terms |
| O3 | Registration-certificate policy engine | 5 | 4 | 3 | 4 | **4.10** | medium | ARF certificate profile stability |
| O4 | Circuit-hash registry service | 4 | 5 | 4 | 3 | **4.05** | high | a scheme owner to adopt it |
| O5 | `mso_mdoc_zk` carrier conformance suite | 4 | 4 | 5 | 3 | **3.95** | high | an external implementation to test |
| O6 | Statutory age-threshold toolkit | 4 | 4 | 3 | 4 | **3.80** | medium | circuits accepted by a scheme owner |

**Initial release: O1, O5, O2.**

- **O1** because it is the only opportunity that converts code already in the tree into a capability
  the EU recognises, with no external gate — and because the AV Profile's normative text names the
  exact scheme and parameter set the fork uses `[VERIFIED]`.
- **O5** because the tests exist, the cost is 2–4 engineer-weeks `[ESTIMATE]`, and an honest
  published result against an external implementation is the strongest available credibility signal
  for everything else.
- **O2** because it is the only one an Estonian buyer can act on before 2028, and its deliverable —
  four documented acceptance cases plus a proposed register interface — has value even if RIA never
  adopts it `[JUDGEMENT]`.

O3 and O4 are the natural second wave: both reduce risk in others' integrations, and O4 is a
prerequisite for O6. O6 should wait for O4 and for a decision by whoever owns the accepted circuit
set.

## 6. How this assessment could be wrong

- **The comparator set may understate others' ZKP work.** Ten comparators were verified, but a
  README silence is not evidence of absence; if, say, the Commission's reference wallet ships ZKP in
  an unreleased build, O1's differentiation falls from "first to publish evidence" to "another test
  kit". This is why O1's acceptance criterion is *external* reproduction, not novelty. `[JUDGEMENT]`
- **The ISO second-edition text could not be read.** `iso.org` returned no content through the
  available web tooling, so `ZkRequest`/`ZkDocument` and the §10.2.7 parameter semantics are taken
  from the Commission's TS13 and the AV Profile rather than from ISO `[UNVERIFIED]`. If the published
  second edition renames these structures, O5's vector labels need renaming and O1's clause map needs
  a new source. The expected publication date has been reported as slipping (Q3 2026 → Q1 2027) by
  the Commission's own tracker, which also means the *planned* structures may still change.
- **Effort estimates are one-person, agent-assisted baselines.** They are derived from the size and
  pace of comparable work in this repository (216 commits, 480 files, +62,578 lines in ~3.5 weeks
  `[VERIFIED]`), which was produced with AI assistance. A conventional team should expect a
  substantially larger calendar effort; the estimates are stated to expose the method, not to
  promise a schedule. `[ESTIMATE]`
- **The whole class of opportunities depends on a standard that is still open.** ZKP was removed from
  DCHP's initial proposal and the ARF's Topic G refinement round is open; the ARF's own requirement
  `ZKP_08` demands schemes relying solely on the ECCG Agreed Cryptographic Mechanisms v2.0, and that
  catalogue contains no ZKP scheme — so as written the requirement is unsatisfiable by any scheme,
  including the one the EU's AV Profile mandates `[VERIFIED]`. That tension is unresolved and is the
  largest single uncertainty in this assessment.
