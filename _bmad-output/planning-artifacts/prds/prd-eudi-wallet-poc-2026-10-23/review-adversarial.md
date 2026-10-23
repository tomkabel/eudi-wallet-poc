# Adversarial review — `prd.md` (EE-EUDIW ZK Age-Verification Wallet)

**Reviewer role:** adversarial reviewer. Two lenses, both applied: (1) edge cases along the
PRD's own branching paths (each FR consequence, each NFR, each §9 guardrail, each §6.2
out-of-scope line); (2) the assessor's scepticism — can the reader the PRD names actually
check the load-bearing claims, and what would they need that the PRD does not say exists.

**Subject:** `prd.md` (1628 lines) and `addendum.md`, reviewed 2026-10-23.

**Method.** Every claim below about the PRD quotes its words. Every claim about the
repository was checked against the working tree at the current checkout, and carries
`file:line`. Where a claim is checkable in the repo, the review says so; where it is not
checkable at all, it says that instead — the two are different findings and are not
conflated. Repository state verified for this review: `app/src/test` holds **169 `@Test`
across 26 files** (matches NFR-5/SM-6 exactly); `tests/e2e.sh` ends `PASS: 13/13`;
`docs/sota-audit-ROLLUP.md` states "Total findings: 124" and "Critical: none";
`tests/vectors/carrier-v1/` exists; `.github/workflows/{ci,app}.yml` and `Makefile` are as
read below.

**Tally: 2 critical · 6 high · 5 medium · 3 low.**

---

## Critical

### C1 — SM-8 and SG-1 assert two repo facts that are false at the commit the PRD describes, and SM-8 contradicts NFR-5/SM-6 in the same section

**PRD says (§7.2, prd.md:1103-1105):**

> "**SM-8 — The untested trust gate gets tests.** `CertificateChainValidator` — the PKIX
> trust gate — has **zero direct unit tests today**, `:zk-conformance` **runs in no CI
> workflow**, and the SD-JWT test asserts nothing. The release closes those three gaps or
> records them."

**PRD says again (§9.2 SG-1, prd.md:1262-1263):**

> "…and a PKIX trust gate with **zero direct unit tests**."

**Repository evidence:**

- `app/src/test/kotlin/ee/cyber/wallet/security/CertificateChainValidatorTest.kt:36` —
  `class CertificateChainValidatorTest`, **14 `@Test` methods**, and its own header
  (lines 23-27) states: *"Every case the plan names is exercised against the real PKIX
  `CertificateChainValidator` with locally minted chains — no Android, no network."*
  So *direct* unit tests of `CertificateChainValidator` exist, and they test the real class,
  not a mock. The 169/26 count the PRD asserts in NFR-5 includes them (this file is
  `CertificateChainValidatorTest.kt: 14` in that count).
- `.github/workflows/app.yml:47-48` — a step named `ZK conformance (byte-identical fixtures)`
  running `./gradlew :zk-conformance:test`, in a workflow whose `paths:` triggers include
  `zk-conformance/**` (app.yml:12, :20). `:zk-conformance` **does** run in a CI workflow.
- Only the third sub-claim survives:
  `app/src/test/kotlin/ee/cyber/wallet/sdjwt/EudiSdJwtTest.kt:52-53` carries
  `// TODO: fix this test` and the body is `println`/commented-out assertions — it asserts
  nothing. One of three true.

**Where the wrong belief came from:** `docs/sota-audit-ROLLUP.md:36-37` ("CertificateChainValidator
… has ZERO direct unit tests; only SD-JWT test asserts nothing; :zk-conformance runs in NO
workflow") and `:58-59` (":zk-conformance + go -race missing from CI"). That audit is dated
`docs/sota-audit-ROLLUP.md:1` — *"eudi-wallet-poc @ 732d57e (branch ee-eudiw-zk-import)"* —
while the PRD's described state is `2d38feb` (`AGENTS.md`, verified block). The PRD
transcribed a stale audit row as present-tense fact.

**Why this matters to the named reader.** SM-8 is a *success metric with a counter-obligation*
("closes those three gaps or records them"). An assessor who checks it — one directory
listing and one grep of `.github/workflows/` — finds it false on two of three counts, in the
very section that claims the product's discipline is "statuses derived from tests"
(§4.6 / FR-38, prd.md:841-850) and "no artefact implies a finding is closed without a test"
(SG-4). It is also a flat internal contradiction: NFR-5/SM-6 (prd.md:1095-1099, 1182-1184)
count 169 `@Test` over 26 files, which *contains* `CertificateChainValidatorTest`'s 14, while
SM-8 says that class has none.

**Smallest fix:** re-derive the three sub-claims against the code at the commit the PRD
names, in the PRD's own words (e.g. "`CertificateChainValidatorTest` holds 14 direct tests
(filename); `:zk-conformance:test` runs in `app.yml:47`; `EudiSdJwtTest` asserts nothing"),
and either drop SM-8 or reduce it to the one gap that is real. Delete the SG-1 clause. Also
make NFR-16's "refreshed at each release" name the audit's commit, since the audit is
already behind the tree it is quoted about.

---

### C2 — §4.1 tags `tests/e2e.sh` as covering "both carriers" `[VERIFIED]`; it exercises one carrier only

**PRD says (§4.1 Description, prd.md:337-340):**

> "It runs on two carriers — `mso_mdoc_zk` over OpenID4VP and the ISO/IEC 18013-7 Annex C
> shape over the Digital Credentials API — and it is exercised end to end by `tests/e2e.sh`
> (13 assertions, both response modes, **both carriers**, one- and two-credential sessions)
> **`[VERIFIED]`**."

**Repository evidence:**

- `tests/e2e.sh:11-12` states in its own header: *"The `/present/*` flow is all this
  exercises."* Every one of the 13 cases drives `wallet/present.py` against `/present/*`.
  There is no DC API, no `navigator.credentials.get`, no HPKE case in the file.
- `tests/README.md:50-81` enumerates all 13 assertions; none is the Annex C path. The
  document even closes with what is absent (`tests/README.md:147-154`) and the DC API
  carrier is not among the covered items.
- The DC API carrier is real and separately tested, in different files:
  `verifier/go/oid4vp/isodcapi.go` (`docs/ARCHITECTURE.md:113`), cover page `iso.go`
  (`docs/ARCHITECTURE.md:111`), tests `verifier/go/iso_test.go` and
  `verifier/go/oid4vp/isodcapi_test.go` (`docs/planning/STEP2A-RECORD.md:240`) — the latter
  is picked up by the `fast` job's `./oid4vp/...` glob (`ci.yml:100`) and the former by the
  `e2e` job's `go test -race . ./zk/...` (`ci.yml:249`). So the DC API carrier **is**
  CI-checked — as unit tests, **not** end to end, and not by `e2e.sh`.

**Why this matters.** This is an unqualified `[VERIFIED]` on a claim that is false, in the
Description of the feature everything else rests on. It also contradicts two other places in
the same PRD: FR-2 is marked **"(shipped, fixture-verified)"** and its consequence says
*"**The claim is stated as fixture-verified only.** … the status is `PENDING-DEVICE`"*
(prd.md:358-369), and §6.2 lists *"On-device exercise of the ISO/IEC 18013-7 Annex C path.
Fixture-verified only (FR-2)"* as out of scope (prd.md:1038-1039). The PRD cannot both defer
the second carrier to `PENDING-DEVICE` and claim `e2e.sh` exercises it end to end.

It propagates into a deliverable: §4.3's Description calls the carrier suite *"the cheapest
credibility signal available, **because the work is already done** and only needs running in
public"* (prd.md:577-579), and FR-17 defines the runner as covering *"the cases already
exercised by `tests/e2e.sh`"* (prd.md:590-591). By that definition the carrier suite covers
the OpenID4VP carrier only — yet the Vision (prd.md:46-49) sells the product on a claim
carried over *both* paths. A reader who budgets the carrier suite on "the work is already
done" is budgeting for one carrier.

**Smallest fix:** delete "both carriers" from the §4.1 parenthesis (the other three items in
it survive scrutiny), and state where each carrier is actually evidenced — `tests/e2e.sh`
for OpenID4VP, `go test ./oid4vp/...` plus `go test . ./zk/...` for the Annex C path. Then
say in FR-17 which carrier its "existing cases" are.

---

## High

### H1 — FR-5's "never transmitted" is contradicted by a shipped, CI-exercised flag, and the assertion it promises does not exist

**PRD says (§4.1 FR-5, prd.md:407-416):**

> "**FR-5: A predicate held false is never transmitted (shipped)** — A holder whose
> attestation holds the requested predicate false can **never** cause that `false` to reach
> the relying party. … **Consequences (testable):** … An assertion in the test estate checks
> that no `false` predicate value appears anywhere in any transmitted payload."

**Repository evidence:**

- `tests/e2e.sh:111-117` (case 4) runs `present "$work/4.log" "$work/minor" --allow-false-predicate`
  and asserts the verifier answers `valid: false` **with** `only accepts 0xf5` — i.e. a
  presentation carrying CBOR `false` (`0xf4`) **was transmitted** and the verifier refused it
  on the value. `verifier/go/oid4vp/carrier.go:299` produces exactly that message:
  `"presentation proves %s = 0xf4, but the query only accepts 0xf5 (true)"`.
- The product's own public demo does this on purpose: `README.md:153-155` — *"Then it runs
  three real attacks (replay, wrong nonce, **a minor forcing a false predicate**) and shows
  each one refused"*; the attack is wired at `sandbox/server.py:237-238`
  (`"lie": ["--allow-false-predicate"]`) and offered on the page at
  `sandbox/static/index.html:297-302`. A recorded run of it exists in the tree
  (`brag-output/work/runs/minor.txt:12`: `"presentation proves age_over_18 = 0xf4"`).
- No assertion matching the promised one exists. Searches for `false predicate`,
  `no false`, `assertNoFalse`, and `0xf4` across the repo return only **refusals** (the Go
  9-case table at `tests/README.md:32`, the prover panic at `zk-age-poc/ee_poa_demo.rs:20,74-78`,
  e2e cases 4 and 5) — never a scan asserting absence of a `false` in a transmitted payload.

**Why this matters.** Two separate failures on one line. (a) The absolute ("never transmits",
and guardrail PG-3 at prd.md:1237-1238: *"No `false` predicate is ever transmitted"*) is
false under `--allow-false-predicate`, which is not a hidden debug path — it is how the
sandbox's third attack and `e2e.sh` case 4 work. An assessor who clicks "Proovi petta" on the
public sandbox **watches the wallet transmit a `false`**. That is exactly the class of
"wrong belief" this PRD exists to avoid, and it is the kind of thing the record's own product
principle 2 ("Say what the shop did not learn as plainly as what it did") was written against.
(b) The consequence *promises an assertion* as its testability warrant. Naming it is the
difference between a testable consequence and an aspiration — FR-3 shows the PRD knows how to
do this (it names `vptoken_test.go`'s 9-case table, prd.md:385-386). FR-5 names nothing,
and nothing exists.

**Smallest fix:** scope the sentence to the default path and name the override as the
boundary ("…unless the deliberate `--allow-false-predicate` test override is set, which the
e2e suite and the sandbox's third attack use to show the refusal"), and either point at the
assertion by file, or restate the consequence as the two refusals that do exist.

---

### H2 — FR-1 states cleartext responses are refused; the repo ships a flag that re-enables them, and the PRD's own cited evidence runs it

**PRD says (§4.1 FR-1, prd.md:352-354):**

> "The response is `direct_post.jwt`, encrypted with direct `ECDH-ES` under P-256 with
> `A128GCM` or `A256GCM`; **a cleartext-mode or non-P-256 request is refused** before the
> consent screen and again before any document leaves."

**Repository evidence:**

- `verifier/README.md:113-114` documents the escape hatch: *"Plain `direct_post` requires
  `-response-mode direct_post -allow-unencrypted-response`, an explicit, logged downgrade."*
- `tests/e2e.sh:82-91` opens with `start_verifier -response-mode direct_post -allow-unencrypted-response`
  and case 1 asserts a cleartext presentation succeeds: `grep -q '"valid": true'`. The
  cleartext flow is therefore not refused "before the consent screen" nor "before any
  document leaves" — it is exercised, and it passes, in the script FR-1 cites as its own
  consequence (prd.md:351) and which §4.1 tags `[VERIFIED]`.
- The string `-allow-unencrypted-response` appears **nowhere** in `prd.md` or `addendum.md`
  (checked across the PRD directory).
- Secondary: the `A128GCM`-or-`A256GCM` half is not asserted anywhere either. `e2e.sh:145-149`
  checks only `kty`/`crv` of the published key, and `e2e.sh:182-186` builds its *own* probe
  JWE with `A256GCM` — nothing asserts the verifier's accepted `enc` set.

**Why this matters.** An assessor checking FR-1's hardening claim will find a documented,
logged, CI-exercised downgrade that the PRD never mentions. The PRD is careful to name
explicit downgrades elsewhere — FR-6 ("Weaker requests are refused, with the reason shown and
the tier labelled"), PG-4 ("No silent downgrade") — so the omission reads as the claim being
broader than the system. Note the PRD's Description of the same feature does acknowledge two
response modes ("both response modes", prd.md:339), so the document internally admits the
plain mode exists while FR-1 says a cleartext request is refused.

**Smallest fix:** qualify FR-1 — "by default the response is `direct_post.jwt` …; the plain
`direct_post` mode exists only behind the verifier's `-allow-unencrypted-response` flag, is
logged as a downgrade, and is what `e2e.sh` Mode A exercises" — and either name the test that
pins the accepted `enc` values or drop the algorithm list from the consequence.

---

### H3 — FR-14's "every dependency the run needs is pinned" is denied by the repository's own development guide

**PRD says (§4.2 FR-14, prd.md:543-553):**

> "A reader who has never met the maintainer can reproduce the published report from a clean
> checkout. … **Every dependency the run needs is pinned** (the Python inputs stay pinned in
> `tests/requirements-ci.txt` …)."

**Repository evidence:**

- `docs/DEVELOPMENT.md:33-35`: *"**A Rust toolchain** for the prover and the FFI. **Nothing
  here pins one**, and CI installs none of its own — it uses what the `ubuntu-latest` image
  carries. `zk-age-poc/REPRODUCE.md` records rustc/cargo 1.97.0 for its measured run; **a
  record, not a requirement**."*
- `docs/sota-audit-ROLLUP.md:56` lists as a live CI/supply-chain finding: *"unpinned pip +
  **Rust toolchain**"*. (The same line's "no permissions block in ci.yml" is stale —
  `ci.yml:42-43` now has one — but the Rust clause is not.)
- The Go toolchain is pinned only indirectly (`ci.yml:65` reads `go-version-file`); the
  prover is built from a third-party sibling checkout at a pinned revision
  (`tests/README.md:105-107`), which a "clean checkout" of *this* repository does not contain.

**Why this matters.** FR-14 is the requirement that makes SM-1 possible ("reproduced from a
clean checkout by a person with no prior contact"). Its one stated *mechanism* is pinning,
and the repository says in writing that the Rust half is unpinned and CI does not pin it. A
stranger reproducing a conformance report today would be reproducing it under a toolchain the
repo explicitly declines to fix, and FR-14 gives them no method for recording which one they
used — while the companion FR-37/NFR-2 require every published figure to carry method and
confidence. The constraint is knowable and cheap to state (it is already written down in
`docs/DEVELOPMENT.md`); the PRD just asserts the opposite.

**Smallest fix:** replace "Every dependency the run needs is pinned" with what is true —
"the Python inputs are pinned in `tests/requirements-ci.txt`; the Go toolchain follows
`verifier/go/go.mod`; the Rust toolchain is **not** pinned (`docs/DEVELOPMENT.md`), so the
report records the rustc/cargo version it was produced with" — and add the toolchain version
to the report's required header beside "the exact commit, the profile revision, the host
class".

---

### H4 — FR-9's hardware-key claim is stated as shipped and testable, but v1 defers the only evidence class that could establish it, and its universal negative has no check

**PRD says (§4.1 FR-9, prd.md:466-476):**

> "**FR-9: Credential keys are generated in hardware (shipped)** — A holder's credential keys
> can **only** be created and used in the device's secure area. … **Consequences (testable):**
> Keys are generated through the platform secure area — StrongBox where
> `FEATURE_STRONGBOX_KEYSTORE` is advertised, TEE otherwise — **with no API in the codebase
> that returns private-key material.**"

**Repository evidence — what *is* checkable:**

- The selection *policy* is JVM-tested, against the real decision class, at
  `app/src/test/kotlin/ee/cyber/wallet/security/SecureAreaSelectionTest.kt:14-28` (2 tests:
  StrongBox when advertised, TEE otherwise). So the "StrongBox where advertised, TEE
  otherwise" half is a tested behaviour.
- That same file's header (lines 9-13) states the limit explicitly: *"the on-device
  attestation check is **PENDING-DEVICE** and tracked in `docs/planning/STEP5-RECORD.md`."*
- `docs/MEASUREMENTS.md:24` carries "Key generation in TEE and StrongBox; slot capacity" as
  **PENDING-DEVICE**, and `:102-106` states: *"The fork has **no `androidTest` source set
  yet** … Until then the harness's correctness rests on the JVM tests, and every device row
  stays PENDING-DEVICE."*
- §6.2 takes the whole class out of v1: *"Device measurement programme (D2, hardware-gated).
  Consequence, stated rather than implied: **v1 makes no performance claim, and every device
  row stays `PENDING-DEVICE`.** No … **StrongBox key-generation time** … is asserted."*
  (prd.md:1023-1026).

**Why this matters.** The two halves have different checkability, and the PRD presents them
as one tested consequence. The first half ("keys are generated through the platform secure
area") is a *device* property: the only artefact that could establish it is an on-device
attestation check, which `SECURE_AREA`'s own test header calls PENDING-DEVICE and which
`docs/MEASUREMENTS.md` says has no test source set and no hardware. So a `(shipped)` FR whose
consequence is labelled *testable* has no test for its central clause in v1. The second half
is a **universal negative over the whole codebase** ("no API in the codebase that returns
private-key material") — that is, in principle, statically checkable, but the PRD gives no
method, no test and no file, so a stranger cannot check it without reading every secure-area
and crypto-provider implementation, and must take the PRD's word. That is precisely the
move §4.6 (prd.md:810-815) says this feature exists to prevent.

**Smallest fix:** split the two clauses. Mark the policy clause tested by
`SecureAreaSelectionTest` and mark the hardware-backing clause `PENDING-DEVICE` with the
missing artefact named (the on-device attestation check, D2) — or state the negative with a
method the reader can run (e.g. "no type in `.../security/` exposes a private-key accessor;
enforced by <check>"), and if no such check exists, say the claim is unenforced.

---

### H5 — OQ-12 says the sandbox "today runs on a host the visitor must build"; the repository documents a public deployment and a hostname

**PRD says (§18 OQ-12, prd.md:1602-1604):**

> "**OQ-12 — Who hosts the sandbox site publicly, if anyone?** The site is the product's
> public face and **today runs on a host the visitor must build**. Hosting it changes the
> evidence story (a stranger's run is not a stranger's reproduction) and needs a decision."

**Repository evidence:**

- `README.md:148` — section heading `## Guided sandbox (zk.proksiabel.ee)`.
- `README.md:163-168` — *"`sandbox/deploy/` is **the public deployment**: a
  `debian:trixie-slim` image (read-only, no capabilities, rate-limited per client, one proof
  at a time with a bounded queue) behind a Cloudflare tunnel. `CF_API_TOKEN=… sandbox/deploy/deploy.sh
  tomkabel@asus` builds, ships and routes it."*
- `sandbox/deploy/compose.yml:1` — *"zk.proksiabel.ee: the sandbox, reachable only through a
  Cloudflare tunnel."* `sandbox/deploy/deploy.sh:2,15` routes that hostname;
  `sandbox/deploy/Dockerfile` exists.

**Why this matters.** The PRD's stated purpose for OQ-12 — *"a stranger's run is not a
stranger's reproduction"* — is exactly right, which is why getting the premise wrong matters:
the *stated* premise (no host exists; a visitor must build) is contradicted by a documented
deployment path, hostname and deploy script in the repository. Whether an instance is live
today cannot be determined from the repo, and the PRD does not say. Either way the reader is
given a false picture of the v1 surface: §16 lists the sandbox as a shipped surface and UJ-1
(prd.md:165-181) has a visitor *clicking a "try to cheat" control* on it, while the document
tells them no such reachable place exists. This also weakens SM-1's "no prior contact"
reproduction, which is the PRD's headline metric: the reader has to decide which of two
contradicting statements about reachability to believe.

**Smallest fix:** restate OQ-12 as the open question it actually is — "`sandbox/deploy/`
implements a public deployment at `zk.proksiabel.ee`; whether an instance is currently live
and whether it is the surface SM-1's reproduction should run against are undecided" — and
name the deploy path so a reader can find it.

---

### H6 — §4.6/FR-38 correct a `issuerAuth` row that the repository confirms is stale — but the PRD never says how a reader checks the correction, and names no test for the "every other row" obligation

**PRD says (§4.6 Description, prd.md:811-815):**

> "It is also the one that is currently violated: the conformity note's `issuerAuth` row
> states that on-device `issuerAuth` validation is absent **while the wallet code performs
> it**, and the research corpus identifies that as a defect to fix before an evaluator sees
> it."

**PRD says (FR-38, prd.md:841-850):**

> "The `issuerAuth` row is corrected against the code, and **every other hand-maintained row
> is verified against the code before it is edited.** … No status row is claimed without a
> test or a quoted primary source behind it…"

**Repository evidence — the PRD is right about the row, and the code is real:**

- `docs/CONFORMITY.md:160` states: *"issuerAuth on-device validation | E6 status: absent —
  mdoc credentials load with no issuerAuth chain check (SD-JWT path verifies); row updates
  when E6 lands"* — stale, as the row itself concedes.
- The code does perform it: `app/src/main/kotlin/ee/cyber/wallet/domain/documents/CredentialToDocumentMapper.kt:63`
  (*"E6 (identity A-M6 part 2): on-device issuerAuth verification for one mdoc"*), `:82`
  `private fun MDoc.verifyIssuerAuth()`, called at `:171`. Acceptance tests exist:
  `app/src/test/kotlin/ee/cyber/wallet/domain/documents/CredentialToDocumentMapperMdocVerificationTest.kt:34-43`
  (4 tests, *"E6 acceptance tests: the mapper's mdoc conversion performs on-device issuerAuth
  verification … before a credential can carry verified=true"*).

**Why this is still a finding.** The row correction is correct and the PRD is to be credited
for catching it. The finding is in the **second** half of FR-38, which is the part that
carries the obligation: *"every other hand-maintained row is verified against the code before
it is edited"* is an **unbounded obligation** — it does not say how many rows, which
document(s), who decides "verified", or what artefact records the verification. §13's R12
mitigation says the same thing and adds only *"add a check that statuses are re-derived from
tests rather than hand-maintained"* (prd.md:1419) — a check that does not exist and that R12
defers to "v1 ships". For the named reader (an assessor), the obligation is unenforceable as
written: any number of edited rows can satisfy it, and nothing distinguishes a verified row
from an unverified one. FR-38's own consequence ("the document names the commit its rows
describe") is the fix that is already half-present — it should be extended to the rows
themselves.

**Smallest fix:** bound it. Name the document(s) in scope, state the row count at the commit,
and require each verified row to carry the test or source that verifies it (FR-38 already
requires this for *claims*; extend it to rows). Record the row count in SM-5's target, which
currently says merely "every other hand-maintained row verified against the code"
(prd.md:1088-1091).

---

## Medium

### M1 — SM-6 and NFR-5 bundle tasks from two CI workflows under a command that runs neither

**PRD says (SM-6, prd.md:1095-1099):** *"**SM-6 — Nothing that was green is no longer green.**
`make check` exits 0; `:app:testLocal_mocksUnitTest` stays at 169 `@Test` …;
`:zk-conformance:test` is green and its committed fixtures stay byte-identical …; `go vet ./...`
is clean; `tests/e2e.sh` reports its 13 cases…"*

**Repository evidence:** `Makefile:47` — `check: lint test-go-fast test-py`. `make check` runs
no Gradle task at all. `:app:testLocal_mocksUnitTest` and `:zk-conformance:test` are run only
by `.github/workflows/app.yml:47-48`, which itself only triggers on `app/**`, `gradle/**`,
`*.gradle.kts`, `zk-conformance/**` (app.yml:6-21) — so an `app/**` change **does not** trigger
the workflow that runs `make check`'s checks, and a `verifier/**` change does not trigger the
workflow that runs the 169 tests.

**Why it matters:** the metric's headline sentence ("nothing that was green is no longer
green") is a claim about the gate, and the gate is three workflows with disjoint path
triggers and no single command. An assessor who runs `make check` at the PRD's word will
believe they have checked the 169 tests and the fixtures; they have checked neither. Fix:
write the metric as the union it is — "`make check` exits 0 **and** `app.yml`'s two Gradle
tasks stay green" — and note the path triggers, since that is what decides which of the two
runs on a given change.

### M2 — SM-7 treats `tests/README.md` as the complete inventory; a test suite the repo's own README documents is absent from it

**PRD says (SM-7, prd.md:1100-1102):** *"Any document the release introduces appears in
`docs/README.md`, and **any new test suite appears in `tests/README.md`** with what it proves."*

**Repository evidence:** `tests/README.md:3` claims *"**Every test and check in this
repository**, where it lives, and what it proves."* But `sandbox/check.py` exists (23 files
under `sandbox/` include `check.py`, `test_age.py`, `server.py`) and drives four real outcomes
end to end (`sandbox/check.py:1-8`: *"Adult: proves age_over_18 and the shop sees valid:true;
the replay is refused. Minor: the prover refuses; lying past it, the verifier refuses on the
value."*), and `README.md:160` documents it as the sandbox's check
(`python3 sandbox/check.py … # the four outcomes, end to end (~1 min)`). A search of
`tests/README.md` for `check.py` returns nothing; only `sandbox/test_age.py` is catalogued
(`tests/README.md:101`).

**Why it matters:** SM-7's mechanism is a document that already under-reports — so the metric
can be satisfied while the inventory stays incomplete, which is the failure mode SM-7 exists
to prevent. Fix: either add `sandbox/check.py` to the inventory as part of v1 (it is the
harness behind the sandbox claims FR-28/FR-30 rest on) or scope SM-7 to "any suite `make
check` or a workflow runs", which is the set `tests/README.md` actually covers today.

### M3 — The one "published host figure" is checkable, but not where the PRD's own measurement document points, and NFR-16 quotes a differently-dated audit

**PRD says (NFR-6 and OR-2, prd.md:1188-1189, 1433-1434):** *"The published host figure today
is **2,414 ms per valid proof on a 2 vCPU host**."*

**Repository evidence:** this figure is real and checkable — but in `verifier/README.md:222`
(`| valid proof | 200 valid:true | 2414 ms |`), with the host at `:218` and `:165`
(`verification ~2.5 s on 2 vCPU`), echoed at `docs/ARCHITECTURE.md:315`. It is **not** in
`docs/MEASUREMENTS.md`, which the PRD names in §0 (prd.md:26) as where *"Measurement evidence
lives"* — that file's only relying-party row (`docs/MEASUREMENTS.md:21`) is the *other* host
and the *other* number: *"MEASURED ON HOST, 24 Sep 2026: mean 4.02 s/op, p50 3.83 s, p95 4.91 s
… (i5-8365U @ 1.60 GHz, 8 threads)"*. Separately, NFR-16/SG-1 quote *"124 findings, none
critical"* (prd.md:1216, 1257), which is accurate (`docs/sota-audit-ROLLUP.md:2,:17`) but is
stated at `732d57e`/`ee-eudiw-zk-import` (rollup:1) while the PRD's state is `2d38feb` — and
neither NFR-16 nor SG-1 names a commit for the audit, although NFR-16 requires it to be
"refreshed at each release".

**Why it matters:** a stranger checking the PRD's two most-quoted numbers is sent to a
document that contains neither. Fix: cite `verifier/README.md:222` (2,414 ms / 2 vCPU) and
name the audit's commit in NFR-16/SG-1. Both are one-line edits and they are the difference
between a figure that is "checkable but undocumented in the PRD" and one that looks
unverifiable.

### M4 — FR-30's cheat surface is gated behind completing the flow, and the example it names is not one of the shipped attacks

**PRD says (§4.5 FR-30, prd.md:747-755):** *"A visitor **can** mount the obvious tamper
attempt and see the refusal happen in front of them. … The site offers at least one
deliberate attack (for example **a modified age value** or a replayed proof) and shows the
verifier's named refusal."*

**Repository evidence:** the surface is real and real-valued — `sandbox/static/index.html:277-302`
("Proovi petta") offers three attacks, wired at `sandbox/server.py:237-238` to
`--replay`, `--tamper-nonce` and `--allow-false-predicate`, and `README.md:153-155` states
*"Nothing on the page is pre-recorded."* But: the attacks are **locked until the journey
finishes** — `sandbox/static/index.html:281`: `<p class="hint" id="cheat-locked" data-t="cheat_locked">Katsed avanevad, kui oled teekonna läbinud.</p>`
("the attempts open once you have completed the journey"), and the section's own lead
(`:280`) says each attempt costs about as long as the visitor's own proof. So an unqualified
"a visitor can mount the attempt" only holds for a visitor who has already run a full
six-step flow. Second, none of the three shipped attacks is "a modified age value" — a ZK
proof cannot carry a modified value; the shipped cases are session-binding (`tamper-nonce`),
replay, and predicate-binding (`lie`). The FR's "for example" makes this survivable, but the
example a reader will remember is the one the product cannot demonstrate.

**Why it matters:** UJ-1's climax (prd.md:175-176) and FR-30 are the visitor's whole reason to
believe the page, and the precondition is unstated. Fix: state the gating in FR-30's
consequence ("after the guided flow completes, the visitor can run three named attacks …")
and replace the example with one of the three that exists.

### M5 — NFR-10 and R10 rest on a comparison a reader cannot perform from this repository

**PRD says (NFR-10, prd.md:1198-1199):** *"Strict superset of upstream. **Upstream's Android
unit suite passes unchanged**; the fork adds behaviour and removes none."* **R10
(prd.md:1417):** *"strict superset (**0 commits behind, verified**)"*.

**Repository evidence:** neither is checkable from this tree alone. `MIGRATION`-free
verification requires fetching `open-eid/eudi-wallet-poc` and diffing commit histories, and
the only in-repo statement of the lineage is the PRD's own extract
(`extract-shared-appendices.md:37`: *"216 commits ahead of upstream, 0 behind"*) and
`docs/research/2026-10-23-shared-appendices.md:33` — the same research corpus, not tool
output. `app.yml` runs only `:app:testLocal_mocksUnitTest`, which on the fork's own tree
cannot distinguish upstream's tests from the fork's; and the 169-test count in NFR-5 is a
*fork* total.

**Why it matters:** NFR-10 is a binding NFR stated in the present tense, and R10 is a risk
whose mitigation *is* the verification. A reader who wants to check "no commits behind" needs
network access, the upstream remote and the migration commit — none named. Fix: name the
upstream commit the comparison was made against and the command that performs it (the same
treatment FR-14 gives the report header), or mark NFR-10 `[UNVERIFIED]` with its method.

---

## Low

### L1 — §9.4's Android-task constraint is stated twice and describes only half the Gradle gate

**PRD says (§9.4, prd.md:1305 and §16, prd.md:1305):** *"The Android unit-test task CI runs is
`:app:testLocal_mocksUnitTest`, not `test`."* **Repository evidence:** broadly true
(`app.yml:49-50`), but `app.yml:47-48` also runs `./gradlew :zk-conformance:test` before it,
and that task is the one SM-6 pairs with the byte-identical-fixture obligation. A reader
using §9.4's sentence as the complete statement of the Android gate will run one of the two
tasks. Fix: name both tasks where §9.4 names one. (Related repo nit, not a PRD finding:
`ci.yml:3` still reads *"the Android app has no CI job"*, which the existence of `app.yml`
has outdated.)

### L2 — §4.1's FR-4 cites one policy class for three carriers, and the PRD does not say which path is evidenced

**PRD says (FR-4, prd.md:396-400):** *"A request naming more than one `age_over_NN` … is
refused on the OpenID4VP, DC API **and proximity** paths (`AgePredicatePolicy`)."*
**Repository evidence:** the refusal is 5 JVM tests in
`app/src/test/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicyTest.kt`, i.e. the
decision core — not the three paths. This is a legitimate architecture (the rollup praises the
"pure decision-core JVM-tested" pattern, `docs/sota-audit-ROLLUP.md:85`), and the PRD's §4.1
already makes the fixture-vs-device distinction for the DC API; FR-4 just does not repeat it.
Note the same sentence names proximity, which §6.2 and NG-6 remove from Tier-2 ZK — so the
refusal is the only proximity behaviour FR-4 can be claiming. Fix: say the policy is tested
once and the three call sites are the claim.

### L3 — The 13-assertion count is correct; the phrase attached to it is not, so it is quoted twice in incompatible ways

**PRD says (§4.1, prd.md:339):** *"13 assertions, both response modes, both carriers, one- and
two-credential sessions"*; **SM-6 (prd.md:1098-1099):** *"`tests/e2e.sh` reports its 13 cases
as they stand or more."*
**Repository evidence:** the count is exactly right — `tests/e2e.sh:405` prints `PASS: 13/13`
and `:407` `FAIL: $failures of 13 assertions failed`, and `tests/README.md:60` says
"Thirteen assertions". The problem is only the modifier in §4.1 (see C2). Fix: as C2.

---

## Checked and found sound (recorded so the review is not read as blanket scepticism)

These are load-bearing claims this reviewer tested against the tree and could not fault; they
are listed because an assessor will test them too.

- **NFR-5's test count is exact.** `169 @Test` over **26** files under `app/src/test/kotlin/ee/cyber/wallet/`,
  verified by count. The number is not approximate and the file count matches.
- **NFR-16/SG-1's "124 findings, none critical"** matches `docs/sota-audit-ROLLUP.md:2` and
  `:17` exactly.
- **SM-2's baseline** is accurate: `tests/vectors/carrier-v1/` exists and no external run is
  recorded.
- **§4.6/FR-38's `issuerAuth` claim is right about both sides** — the row (`docs/CONFORMITY.md:160`)
  is stale and the code really does verify `issuerAuth` on device
  (`CredentialToDocumentMapper.kt:82-112,171`, with 4 acceptance tests). The PRD correctly
  caught a real documentation defect. (The finding in H6 is about the *other* half of FR-38.)
- **FR-30's tamper demo exists and is not scripted** — `sandbox/static/index.html:283-302`,
  `sandbox/server.py:237-238`, `README.md:153-155`.
- **FR-4's `AgePredicatePolicy`** exists and is JVM-tested (5 tests).
- **§9.4's build constraints** (`make deps`, the `-C target-cpu=x86-64-v3` pin, the
  `test -x` on the prover, `demo-data/` never committed) all match `Makefile:10-24,64-65`,
  `ci.yml:204-212` and `AGENTS.md`.
- **The `[ASSUMPTION]`/`[VERIFIED]` index in §19** is internally consistent with the inline
  tags reviewed here, and §19 A-8's de-duplication of the 211-identifier count is the kind of
  re-derivation the rest of §7 should have had (see C1).
