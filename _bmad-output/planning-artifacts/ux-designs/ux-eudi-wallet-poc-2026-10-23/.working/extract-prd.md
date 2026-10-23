# Extract — PRD + addendum for the UX design run (eudi-wallet-poc)

Sources, both in `_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/`:

- `prd.md` — 1809 lines, front-matter `status: final`, `created: 2026-10-23`, `updated: 2026-10-23`.
- `addendum.md` — 188 lines.

Identifier scheme used throughout is the **PRD's own**: `FR-1`…`FR-48`, `NFR-1`…`NFR-17`,
`UJ-1`…`UJ-5`, `SM-1`…`SM-9`, `CM-1`…`CM-7`, `PG-1`…`PG-7`, `SG-1`…`SG-6`, `OQ-1`…`OQ-16`,
`A-1`…`A-9`, `NG-1`…`NG-19`, `R1`…`R16`, `DG-1`…`DG-7`, `OR-1`…`OR-7`, `CG-1`…`CG-3`,
`Q1`…`Q5`, `B1`…`B5`. **This is NOT `epics.md`'s `FR1–FR66` / `NFR1–NFR10`** (11 epics, 76
stories). `prd.md:29-32` states the two schemes are deliberately distinct so both can be read
side by side; `A-6` (`prd.md:1806`) is the assumption that records it.

All section references below are `prd.md` line numbers unless prefixed `addendum.md`.

Sections the task named, and whether they exist: **2.3** ✓ (prd.md:152), **4.5** ✓ (:763),
**4.7** ✓ (:986), **6.1** ✓ (:1130), **6.2** ✓ (:1149), **8** ✓ (:1313), **9.1** ✓ (:1380),
**9.2** ✓ (:1411), **13** ✓ (:1572), **15** ✓ (:1621), **16** ✓ (:1646), **18** ✓ (:1717). Every
named section exists; nothing is missing.

---

## 1. §2.3 Key User Journeys — UJ-1 … UJ-5, VERBATIM (prd.md:152-248)

Preamble, prd.md:152-158, verbatim:

> ### 2.3 Key User Journeys
>
> Five journeys, each captured from a real surface rather than invented: UJ-1 from the
> sandbox site's documented six-step flow (`DESIGN.md`), UJ-2 to UJ-5 from the intent
> record's own use cases U1, U2, U4 and U5. The record's U3 (offline/proximity age proof at
> a venue) is deliberately *not* a journey here — see §6.2 — because its dependency is
> pending and its acceptance evidence does not exist.

Assumption covering the protagonist names, prd.md:160-163, verbatim:

> `[ASSUMPTION: protagonist names and their one-line context are drafted by the assistant
> from the intent record's actor types, not narrated by the user. Every journey's beats,
> outcome and edge case are traceable to the record or to `DESIGN.md`; the names are
> placeholders to be corrected or replaced.]`

### UJ-1 (prd.md:165-181) — VERBATIM

> - **UJ-1. Rasmus sees the proof before he believes it.**
>   - **Persona + context:** Rasmus, a journalist writing about state digital identity, has
>     read claims about zero-knowledge age checks and does not trust claims.
>   - **Entry state:** no account, no phone, a desktop browser; arrives cold at the sandbox
>     site in Estonian (the toggle offers English).
>   - **Path:** picks a labelled test persona (synthetic name and birth date) → registers it
>     against the sandbox's Population Register → receives the age attestation → follows the
>     step rail to the test shop → the shop asks for `age_over_18` → watches the proof
>     timeline fill while the clock counts → reads the ledger of what the shop learned
>     against what stayed with him → clicks the sandbox's own "try to cheat" control.
>   - **Climax:** the ledger, both columns, each number from the run that just happened —
>     and the tamper attempt failing in front of him rather than being described.
>   - **Resolution:** he has a reproducer he can cite; every step is repeatable, and the
>     page's independence notice tells him whose product this is not.
>   - **Edge case:** the prover or verifier is not built on the host, so the guided flow
>     cannot run; the site says so and names the documented command instead of showing a
>     simulated result.

### UJ-2 (prd.md:183-199) — VERBATIM

> - **UJ-2. Mari buys wine without handing over her birth date.** *(record U1)*
>   - **Persona + context:** Mari, a Tallinn resident who wants the order to go through and
>     does not want another birth date copy in another database.
>   - **Entry state:** authenticated to an Estonian online service; wallet installed and
>     holding an age attestation; TARA-shaped session context established.
>   - **Path:** the service asks for `age_over_18` over OpenID4VP with an `mso_mdoc_zk`
>     DCQL query → the wallet resolves the held circuit → the consent screen names the
>     requester and the channel → she approves → the proof is produced and encrypted to the
>     relying party's published key (`direct_post.jwt`) → the verifier checks the transcript,
>     the circuit allowlist and the proof.
>   - **Climax:** the order completes; the relying party's log holds a boolean and no
>     attribute value.
>   - **Resolution:** exactly one predicate was disclosed and the wallet labelled the
>     presentation as ZK or as a documented fallback.
>   - **Edge case:** the relying party asks for a second age predicate, or for a predicate
>     the wallet holds `false`; the request is refused or the no-match answer is returned,
>     with a stated reason to the user, and no `false` predicate is ever transmitted.

### UJ-3 (prd.md:201-215) — VERBATIM

> - **UJ-3. Liis accepts a foreign proof her service has never seen.** *(record U2)*
>   - **Persona + context:** Liis runs the verification side of an Estonian service that has
>     been told to accept other Member States' certified wallets.
>   - **Entry state:** her service deployed against the O1/O2 artefacts; no Estonian wallet
>     to test with; the accepted circuit set unpublished.
>   - **Path:** a foreign AV-profile holder presents over the DC API → the request carries
>     the AV attestation with the `ZkSystemSpec` parameter tuple → her verifier resolves the
>     circuit against its registry → validates the proof → returns a verdict, encrypted.
>   - **Climax:** a foreign proof verifies at her service without a birth date crossing the
>     boundary, and the AV-Profile conformance run reports the same result for this fork's
>     verifier and for an external implementation.
>   - **Resolution:** she can state which clauses she has evidence for and which she does
>     not; a non-`longfellow-libzk-v1` system is refused.
>   - **Edge case:** the circuit registry is unreachable; the verifier fails closed and says
>     which lookup failed instead of allowing the presentation.

### UJ-4 (prd.md:217-230) — VERBATIM

> - **UJ-4. Tanvi onboards without an isikukood.** *(record U4)*
>   - **Persona + context:** Tanvi is an e-resident opening an Estonian service account; she
>     has no Estonian personal identification code and does not want her full date of birth
>     on file.
>   - **Entry state:** holds a PID and an age attestation; the service needs an attribute
>     subset plus an age predicate in one session.
>   - **Path:** the service asks for both credentials in one session → the wallet answers
>     both, with the age predicate proved and the PID attributes selectively disclosed.
>   - **Climax:** onboarding completes; the session transcript carries no isikukood anywhere
>     and the log names both credential identifiers.
>   - **Resolution:** the service holds what it asked for and nothing more; the wallet shows
>     what was disclosed and to whom.
>   - **Edge case:** one of the two credentials is missing; the session fails with the
>     missing credential named, rather than completing partially.

### UJ-5 (prd.md:232-247) — VERBATIM

> - **UJ-5. Sander gets his first successful verification.** *(record U5)*
>   - **Persona + context:** Sander is the integrator at an Estonian service operator; his
>     deadline is the end-2026 acceptance window and his target is a service that answers
>     "does this work?" honestly.
>   - **Entry state:** nothing deployed; no register access; the sandbox's synthetic personas
>     are the only test identities.
>   - **Path:** deploys the relying-party acceptance kit against the documented commands →
>     runs the four cases (a wallet with a PID, a wallet with an AV attestation, an
>     unattested ZK request, an unregistered relying party) → sees each refusal name its
>     cause → wires the register interface against the shipped mock.
>   - **Climax:** a first successful verification, with the time to it measured and reported,
>     and a refusal that is distinguishable from the user having cancelled.
>   - **Resolution:** he has a proposal for the register interface instead of a guess, and a
>     bounded, logged, fail-closed fallback for when the register does not answer.
>   - **Edge case:** the mock register is configured absent or the certificate names no
>     intended use; the lookup fails closed and the refusal says so.

---

## 2. §4.5 The sandbox site (prd.md:763-856)

Description, prd.md:763-770, verbatim:

> ### 4.5 The sandbox site
>
> **Description:** the product's public face and the only surface a visitor uses without
> building anything. It drives the real issuer, prover and verifier for a guided six-step
> flow, ends in a ledger contrasting what the test shop learned with what stayed with the
> visitor, and names its own shortcuts where they happen. Its visual world is pinned in
> `DESIGN.md` (TEDI-derived, ria.ee chrome, "The Service Counter"): that document is the
> design authority and this PRD does not restate it. Realizes UJ-1.

### FR-28: Every shown number comes from the run that just happened (shipped) — prd.md:774-783

> A visitor can trust that the figures and verdicts on the page are the output of the run
> they triggered, not copy. Realizes UJ-1.
>
> **Consequences (testable):**
> - The registration, attestation, proof and verdict steps execute against the real issuer,
>   prover and verifier; no step returns a canned result.
> - Any figure shown (proof size, timings, verdict) is rendered from the server's response
>   for that run.

### FR-29: The ledger shows both columns (shipped) — prd.md:785-794

> A visitor can see what the test shop learned and what stayed with them, side by side, at
> the moment it happened. Realizes UJ-1.
>
> **Consequences (testable):**
> - The ledger renders the disclosed boolean and the withheld attributes as two contrasting
>   columns with per-item captions.
> - The "kept"/"learned" wording matches the run's actual payload; a change to what is
>   disclosed changes the ledger without a copy edit.

### FR-30: The visitor can try to cheat and watch it fail (shipped) — prd.md:796-804

> A visitor can mount the obvious tamper attempt and see the refusal happen in front of
> them. Realizes UJ-1.
>
> **Consequences (testable):**
> - The site offers at least one deliberate attack (for example a modified age value or a
>   replayed proof) and shows the verifier's named refusal.
> - The attack's outcome is the verifier's real response, not a scripted animation.

### FR-31: The sandbox's shortcuts are named where they occur (shipped) — prd.md:806-815

> A visitor can tell which parts of the demonstration are shortcuts. Realizes UJ-1.
>
> **Consequences (testable):**
> - The synthetic Population Register's client-side age computation is named at the point it
>   happens as the trust-the-client enrolment weakness the spec warns about
>   (`EE-POA-006`/`EE-POA-007`).
> - The synthetic test identities are labelled as synthetic within the flow, not only in the
>   footer.

### FR-32: The site is bilingual, Estonian first (shipped) — prd.md:817-825

> An Estonian reader can read every string in Estonian, and an English reader in English.
> Realizes UJ-1.
>
> **Consequences (testable):**
> - Every string exists in both languages; the toggle is present on every page; Estonian is
>   the default.
> - A string added in one language without the other fails the site's own check.

### FR-33: The independence notice is present and the state branding is absent (shipped) — prd.md:827-834

> A visitor can never mistake this for RIA's product. Realizes UJ-1.
>
> **Consequences (testable):**
> - The independence notice appears in the utility bar, the info alert and the footer.
> - No RIA logo and no state coat of arms appears anywhere in the site or its assets, and no
>   page claims endorsement.

### FR-34: A missing toolchain produces an honest failure, not a fake (shipped) — prd.md:836-844

> A visitor whose host cannot run the spine can still learn how to make it work. Realizes
> UJ-1.
>
> **Consequences (testable):**
> - When the prover or verifier is not built, the page says so and names the documented
>   command (`make deps`) instead of rendering a simulated result.
> - No page shows a proof timeline or a verdict the host did not actually produce.

### FR-35: The site holds the public-sector accessibility baseline (shipped) — prd.md:846-855

> A visitor using a keyboard or a screen reader can complete every step. Realizes UJ-1.
>
> **Consequences (testable):**
> - WCAG 2.1 AA as the stated baseline with EN 301 549 as the presumption: keyboard-complete,
>   visible focus, contrast held by the pinned tokens in `DESIGN.md`, and no reliance on
>   colour alone.
> - Long-running steps (the proof) announce progress and completion to assistive technology,
>   and reduced-motion is honoured (the proof mark renders as a static state).

---

## 3. §4.7 Consent integrity and user-presence gates (prd.md:986-1044)

Description, prd.md:986-995, verbatim:

> ### 4.7 Consent integrity and user-presence gates
>
> **Description:** the wallet-side findings the product's own audit raised on the paths the
> product *advertises*: a name on the consent screen that has not been authenticated, a
> consent gate that can be guessed, and device keys that can be used without the user present.
> They are in v1 because §9.2 says the consent screen's claims must be true and that key use
> requires user presence, and today neither holds. Realizes UJ-2, UJ-4. These four are the
> release's only requirements that exist to close findings rather than to add capability —
> the audit's remaining items (supply-chain pinning, main-thread blocking, debug logging, the
> mock trust surfaces, the release path) are a hygiene release, not this one.

### FR-45: A requester name shown at consent is authenticated (new) — prd.md:999-1010

> A holder can trust that the relying party named on the consent screen is the relying party
> that signed the request. Realizes UJ-2.
>
> **Consequences (testable):**
> - The `readerAuth` chain is validated to a trust anchor before any requester name is
>   rendered; no unvalidated CN reaches the screen as a verified name.
> - When the chain does not validate, the screen says the requester is unverified and the
>   default is refusal, with the reason visible (FR-6).
> - A negative test presents a certificate carrying an attacker-chosen CN and asserts it is
>   not rendered as verified.

### FR-46: Credential keys require user presence (new) — prd.md:1012-1022

> A holder's credential keys can be used only after the user authenticates, with the
> platform's user verification bound to the key operation itself. Realizes UJ-2, UJ-4.
>
> **Consequences (testable):**
> - Key use is gated by a platform user-authentication prompt whose result the key operation
>   depends on, not by an application-level flag alone.
> - Proof generation, plain presentation and signing each carry the gate, or the ungated path
>   is removed rather than left reachable.
> - A test asserts that a key operation attempted without user authentication fails.

### FR-47: The consent gate resists guessing (new) — prd.md:1024-1033

> A holder's consent gate cannot be brute-forced. Realizes UJ-2.
>
> **Consequences (testable):**
> - Failed attempts are counted in persistent state that survives process restart, with a
>   lockout threshold and an escalation path stated in the artefact that publishes it.
> - Relaunching the application does not reset the count; the existing behaviour, where the
>   counter resets on relaunch and a "remote check" is a simulated delay, is removed rather
>   than documented.

### FR-48: A proximity share carries the same consent as a remote one (new) — prd.md:1035-1044

> A holder presenting in proximity consents explicitly before anything is emitted. Realizes
> UJ-2.
>
> **Consequences (testable):**
> - The proximity share passes the consent gate before a document is emitted; today it emits
>   with no gate at all.
> - The screen states what is shared and with whom on the same terms as the remote path.
> - A test asserts the gate is present on the proximity path.

---

## 4. §6 MVP Scope (prd.md:1124-1194)

### 4.1 §6.1 In Scope for v1 (prd.md:1130-1147) — VERBATIM

> ### 6.1 In Scope for v1
>
> | Item | What ships | Record's phase |
> |---|---|---|
> | AV-Profile clause registry and gap analysis | a machine-readable clause map with a test id or an out-of-scope note per clause | D1 |
> | RP-register interface proposal | a written interface, mocks, a fail-closed default, ARF topic mapping | D3 |
> | EU AV-Profile conformance kit | FR-11 … FR-16: fixtures, runner, per-clause report, external reproduction | M1 / O1 |
> | `mso_mdoc_zk` carrier conformance suite | FR-17 … FR-20: runner over the existing cases, published vectors, one external implementation, version-managed drift | M2 / O5 |
> | Estonian relying-party acceptance kit | FR-21 … FR-27: deployment profile, TARA adapter contract, mocked register, four acceptance cases, measured time-to-first-verification | P1 / O2 |
> | The presentation spine | FR-1 … FR-10: preserved and regression-protected, not extended | existing |
> | The sandbox site | FR-28 … FR-35: preserved as the public face | existing |
> | Claim and evidence discipline | FR-36 … FR-44: the banned-vocabulary check, corrected status rows, the published clause maps, the `ZKP_08` report | R3 + record §6 |
> | Consent integrity and user-presence gates | FR-45 … FR-48: an authenticated requester name at consent, a user-presence gate on credential keys, a consent gate that resists guessing, and consent on the proximity path | maintainer decision, 2026-10-23 (OQ-8) |
>
> `[ASSUMPTION: the sandbox site and the spine are in v1 as shipped behaviour rather than as
> new work. Both already exist and both are what the release's evidence is demonstrated on,
> so excluding them would make the release's claims untestable. Confirm if you would rather
> v1 be a forward-only release with no regression-protection obligations.]`

Preceding framing, prd.md:1124-1128, verbatim:

> ## 6. MVP Scope
>
> The version the user selected: the innovation assessment's initial release — the
> conformance kit, the carrier suite and the relying-party acceptance kit — with the two
> discovery items those three depend on folded in.

### 4.2 §6.2 Out of Scope for v1 (prd.md:1149-1171) — VERBATIM

> ### 6.2 Out of Scope for v1
>
> - **Device measurement programme (D2, hardware-gated).** Consequence, stated rather than
>   implied: **v1 makes no performance claim, and every device row stays `PENDING-DEVICE`.**
>   No usability figure, no BLE transfer time, no StrongBox key-generation time, no proving
>   budget is asserted.
> - **Verification-budget hardening (M3).** Consequence: v1 states verification as a
>   *capacity* figure on a named host class, or states nothing. The `p95 ≤ 1.0 s` budget is
>   declared unmet and is not repeated as a target.
> - **Registration-certificate-aware request policy (O3, phase P2).** The two-predicate
>   request stays refused (NG-9) until this lands.
> - **Circuit-hash registry service (O4, phase P3).** The verifier's static allowlist stays;
>   no distributed signed list in v1.
> - **Statutory age-threshold toolkit (O6, phase R2, externally gated).**
> - **Production readiness for a pilot relying party (R1).** No operations runbook,
>   monitoring, incident process or availability target in v1; the absence is stated so no
>   reader assumes a service they can depend on.
> - **On-device exercise of the ISO/IEC 18013-7 Annex C path.** Fixture-verified only
>   (FR-2); the device run belongs to D2.
> - **Automatic background re-issuance** of the age attestation; re-issuance is user-started.
> - **Status lists, revocation and the Trusted List of Lists hardening** beyond what exists.
> - **Proximity Tier-2 ZK**, per NG-6.
> - **Filing the DCHP #17 submission**, per NG-18.

---

## 5. §8 Non-Functional Requirements — ALL, with their text (prd.md:1313-1376)

Header, prd.md:1313-1315: "## 8. Non-Functional Requirements / Cross-cutting and binding on
every feature."

**NFR-1 — No status claim.** (prd.md:1317-1319, verbatim)

> No artefact states or implies compliance, conformity or certification. Assessment is
> conferred by a body, not by this repository. A check fails `make check` on the banned
> vocabulary.

**NFR-2 — Every figure is labelled.** (prd.md:1320-1322, verbatim)

> Every published number states its baseline, method, assumptions and confidence; a range is
> never presented as a commitment; an unmeasured row says `PENDING-DEVICE` rather than being
> estimated silently.

**NFR-3 — Fail closed, always.** (prd.md:1323-1326, verbatim)

> Every lookup and policy path — register, certificate, circuit list, trust anchor — refuses
> on absent, unparseable or ambiguous input and names the failed lookup. Silent fallback is
> prohibited; where a fallback is legitimate it is labelled to the user and written to the
> log.

**NFR-4 — Version-labelled fixtures.** (prd.md:1327-1329, verbatim)

> Every fixture and rule set names the external document version it was built from, and
> nothing is regenerated from the implementation under test.

**NFR-5 — No regression.** (prd.md:1330-1332, verbatim)

> `make check` exits 0; `:app:testLocal_mocksUnitTest` (the task CI runs) stays at 169 `@Test`
> over 26 test files with zero failures; `:zk-conformance:test` is green; `go vet ./...` is
> clean; the end-to-end suite passes.

**NFR-6 — Verification is stated as capacity, not latency.** (prd.md:1333-1338, verbatim)

> No document repeats the spec's `p95 ≤ 1.0 s` verification budget as a target. Verification
> is reported as verifications/second on a named instance type, with percentiles and the
> measurement method, or it is reported as unmeasured. The two published figures come from
> different host classes and must not be compared: 2,414 ms per valid proof on a 2 vCPU
> instance, and mean 4.02 s / p50 3.83 s / p95 4.91 s on an Intel i5-8365U.

**NFR-7 — Proving figures carry their host class.** (prd.md:1339-1341, verbatim)

> The only proving figures publishable today are the host ones (~6.1 s, proof ~353–360 KB);
> no device-side proving or transfer figure exists and none is asserted.

**NFR-8 — Third-party reproducibility.** (prd.md:1342-1343, verbatim)

> A reader with the documented prerequisites reproduces every published result from a clean
> checkout using only documented commands.

**NFR-9 — The build stays secretless and nothing from `demo-data/` is committed.** (prd.md:1344-1346, verbatim)

> The holder's device private key never enters the repository; no credential this fork issues
> is committed.

**NFR-10 — Strict superset of upstream.** (prd.md:1347-1348, verbatim)

> Upstream's Android unit suite passes unchanged; the fork adds behaviour and removes none.

**NFR-11 — Dependency pinning.** (prd.md:1349-1352, verbatim)

> Pre-1.0 dependencies are pinned by coordinate with a target commit hash where the build
> system allows it; Python inputs stay pinned in `tests/requirements-ci.txt`, because
> unpinned `cbor2` or `cryptography` shift the golden transcript vectors.

**NFR-12 — Accessibility.** (prd.md:1353-1358, verbatim)

> WCAG 2.1 AA is the stated baseline, and for the wallet the specification makes Directive
> (EU) 2019/882 and **EN 301 549 v3.2.1** a `SHALL` (`EE-ACC-001`–`EE-ACC-004`) — that is the
> binding instrument, and the sandbox site is held to the same baseline. Keyboard-complete,
> visible focus, contrast held by the pinned tokens, no colour-only signalling, announcements
> for long-running steps, reduced-motion honoured. No EN 301 549 audit evidence exists yet
> (R13).

**NFR-13 — Language coverage, stated per surface.** (prd.md:1359-1362, verbatim)

> The wallet must offer Estonian, Russian and English (`EE-ACC-004`); the sandbox site is
> bilingual Estonian-first with an English toggle (`DESIGN.md` is its authority). The gap
> between the two is recorded rather than smoothed: no string exists in one language only
> *on the surface it belongs to*.

**NFR-14 — Honest failure surfaces.** (prd.md:1363-1365, verbatim)

> A refusal names its cause and is distinguishable from a retryable error and from user
> cancellation, in the UI, in the log and in the acceptance cases.

**NFR-15 — The narrow claim is stated wherever the product is described.** (prd.md:1366-1368, verbatim)

> One boolean predicate, one attestation, two carriers, and the limits (`ZKP_08`, 109-bit
> circuits, no device measurements) travel with the claim.

**NFR-16 — Findings are published, not implied to be fixed.** (prd.md:1369-1373, verbatim)

> The repository's own audit (`docs/sota-audit-ROLLUP.md`: 124 findings, none critical,
> including consent-gating, key-user-presence and verifier-authentication items) is
> referenced by severity count, and refreshed at each release. No artefact may imply a
> published finding is fixed unless a test or a tool run establishes it. The state of each
> high finding is an explicit row.

**NFR-17 — No silent scope widening.** (prd.md:1374-1376, verbatim)

> A capability that would extend the claim — a new predicate, a new ZK system, a new carrier,
> a new attestation — is a PRD change, not an implementation detail.

---

## 6. §9.1 Privacy Guardrails — PG-1 … PG-7 (prd.md:1380-1409)

Intro, prd.md:1380-1382, verbatim: "These are the product's reason to exist and they are not
negotiable by a feature request."

**PG-1 — The isikukood never crosses the boundary.** (prd.md:1384-1388, verbatim)

> The Estonian personal identification code embeds the date of birth, which is why an age
> check answered with it over-discloses by construction. A relying party performing only an
> age check must not receive it, nor `birth_date`, `sex`, `portrait` or any other PID
> attribute a plain presentation would carry.

**PG-2 — One predicate, one boolean.** (prd.md:1389-1390, verbatim)

> At most one age predicate per presentation, and the disclosed payload is a boolean and
> nothing else.

**PG-3 — No `false` predicate is ever transmitted.** (prd.md:1391-1392, verbatim)

> A held-false predicate produces the no-match answer, never a transmitted `false`.

**PG-4 — No silent downgrade.** (prd.md:1393-1395, verbatim)

> Where a presentation is a documented fallback rather than a proof, the user is told and the
> log records it. A refusal on a ZK-capable device is preferred to an unlabelled plain
> answer.

**PG-5 — No central age log, with the specification's own carve-out stated.** (prd.md:1396-1403, verbatim)

> There is no central record of age presentations, *other than* the operator's minimal,
> aggregated, non-identifying service telemetry, which must not include relying-party
> identity at per-event granularity (`EE-PRI-023`). The wallet's own activity log is local,
> distinguishes linkable from new-visit presentations, and is the user's. The relying party's
> log carries no attribute value. The Art. 5a(4)(d) dashboard (`EE-PRI-014`) and the erasure
> and data-protection-report duties are **out of scope for v1** (§6.2) rather than silently
> dropped; `EE-PRI` is the largest requirement family the v1 scope does not touch.

**PG-6 — Fail-closed is a privacy control, not an availability nuisance.** (prd.md:1404-1406, verbatim)

> A register a relying party cannot read produces a refusal; a missing circuit produces a
> refusal; an unreachable trust list produces a refusal. Uptime never wins over refusal.

**PG-7 — The register sits on the critical path and that is a known cost.** (prd.md:1407-1409, verbatim)

> It must be designed interface-first with mocks, a bounded cache and an explicit, logged
> stale decision. A silent allow on lookup failure is prohibited (R9).

---

## 7. §9.2 Security Guardrails — SG-1 … SG-6 (prd.md:1411-1441)

Intro, prd.md:1413, verbatim: "This product publishes its own audit, therefore its audit is a
product constraint."

**SG-1 — Published findings are published, not implied fixed, and their age is stated.** (prd.md:1415-1428, verbatim)

> The audit of record (`docs/sota-audit-ROLLUP.md`, 124 findings, none critical) was taken at
> commit `732d57e`; the PRD describes `2d38feb`. Its live TIER 1 items are: a four-digit PIN
> with no lockout gating all consent, no user-presence gate on device keys (no biometric
> prompt anywhere), a proximity share with no PIN step, `readerAuth` chains never validated
> against a trust anchor (so an attacker-chosen CN reaches the consent screen), unsigned
> OpenID4VP authorization requests with a bare `client_id` claim (`EE-PRO-004` governs the
> unsigned request, `EE-PRO-003` the client-id prefix; `EE-PRO-010a`, the cross-device DC API
> requirement, is a separate unimplemented item), `runBlocking` on main-thread signing paths,
> and a mutable COSE-signing snapshot dependency. **Two audit rows are stale and were closed
> on 2026-09-25** — the `CertificateChainValidator` unit-test gap and the `:zk-conformance`
> CI gap (SM-8). A finding's severity is a claim about a commit, and any artefact quoting
> this audit states which commit it was taken at.

**SG-2 — The consent screen's claims must be true.** (prd.md:1429-1432, verbatim)

> A requester name shown to the user is a claim about authentication. `readerAuth` chains are
> validated against a trust anchor before a name is rendered, and an unvalidated requester is
> shown as unverified with refusal as the default (FR-45). **Decided 2026-10-23: this is in
> v1** (OQ-8 resolved).

**SG-3 — Keys require user presence and the consent gate resists guessing.** (prd.md:1433-1435, verbatim)

> Device-key use is gated by platform user verification, and the consent gate counts failed
> attempts in state that survives a restart (FR-46, FR-47). **Decided 2026-10-23: both are in
> v1.**

**SG-4 — No artefact implies a finding is closed without a test or a tool run.** (prd.md:1436, verbatim — the PRD's own single sentence, not an excerpt)

> No artefact implies a finding is closed without a test or a tool run.

**SG-5 — The supply chain is pinned.** (prd.md:1437-1439, verbatim)

> Pre-1.0 and snapshot dependencies carry their coordinate and target commit; the Maven
> mirror and vendored `coseJava` are inventoried with their upstream licence and revision
> (R11, R14).

**SG-6 — `demo-data/` never enters the repository.** (prd.md:1440-1441, verbatim)

> It holds the holder's device private key.

---

## 8. §13 Risk and Mitigations (prd.md:1572-1595) — VERBATIM

> ## 13. Risk and Mitigations
>
> The register below is the record's, carried row for row. **No row has an owner, because the
> record assigns none** — ownership is an open question (§18, OQ-9). Each row therefore
> carries the condition under which it should be revisited.
>
> | # | Risk | L / I | Mitigation | Revisit when |
> |---|---|---|---|---|
> | R1 | ISO/IEC 18013-5 second edition ships `ZkRequest`/`ZkDocument` differently from the draft the carrier approximates | M / H | version-label every fixture; keep `oid4vp/carrier.go` the single adaptation point; re-run vectors on publication | the standard publishes (currently Q1 2027) — its text could not be read at all during the record's research `[UNVERIFIED]` |
> | R2 | A ZK system other than Longfellow becomes the EU-preferred one (the BBS-family TS14 path) | L–M / H | keep other families out of the carrier but keep the one-format-family-per-scheme rule so a second can be added without a dialect collision | ETSI TS 119 476-2's direction resolves |
> | R3 | `ZKP_08`'s unsatisfiable requirement is resolved in a way that invalidates the current scheme | L / H | state the tension in writing wherever a programme is told about compliance; track the ECCG catalogue's next revision | the catalogue adds a ZK entry, or the ARF relaxes the requirement |
> | R4 | No scheme owner adopts the circuit registry (O4), or the accepted set lands elsewhere | M / M | ship it as a generator plus a hosted instance with a documented succession path; keep a local pinned list as fallback | the AV Profile names the maintainer of the accepted set |
> | R5 | Verification cost makes a relying party's economics unattractive (p95 4.91 s against a 1.0 s budget on the fork's own host) | M–H / M | measure on target hardware (B2); admit control per credential; cache nothing that weakens binding; publish honest figures | D2/M3 run, or an RP states a peak-rate assumption |
> | R6 | Proof size (~360 KB) breaks a proximity or mobile-network budget | M / M | measure BLE transfer (B3); keep proximity Tier-2 optional until measured | a proximity Tier-2 requirement appears |
> | R7 | Privacy regression via the plain fallback path | M / H | the wallet labels the tier and the record; `EE-ZKP-055a` refuses plain-mdoc-only requests on a ZK-capable device; the fallback stays visible in logs and UI | an RP reports it cannot complete a flow it needs |
> | R8 | The registration-certificate policy loosens a control through a parsing error | L–M / H | default to refusal; property tests over generated certificates; explicit negative tests on the loosening path | O3 is built (phase P2) |
> | R9 | Register on the critical path — a fail-closed lookup makes every presentation depend on a service that does not exist yet | H / M | mock now, interface-first; bounded cache with an explicit stale policy; never silently allow | RIA publishes an interface or an availability target |
> | R10 | Upstream is dormant, so the fork carries the whole maintenance load | H / M | strict superset (0 commits behind, verified); vendor what is needed; keep upgrades pinned and routine | upstream resumes; or a second maintainer appears |
> | R11 | Licence and provenance risk in a derived work (MIT base, but vendored/mirrored artefacts ship) | L–M / M | inventory every vendored artefact with upstream licence and revision; keep the MIT notice; import nothing from the private repository without written permission | the inventory is produced (currently the mirror's contents are unenumerated `[UNVERIFIED]`) |
> | R12 | Documentation contradicts the code (the stale `issuerAuth` row) | H / M | fix the row; add a check that statuses are re-derived from tests rather than hand-maintained | v1 ships — this row is in scope (FR-38) |
> | R13 | Accessibility duty unmet at audit time (WCAG 2.1 AA claimed, no EN 301 549 evidence) | M / M | run an accessibility audit as a milestone exit criterion | any public-sector audit is scheduled |
> | R14 | Third-party dependency on a pre-1.0 library (multipaz pinned, eudi libraries pre-1.0) | H / M | pin, constrain transitively, and keep the conformance module to detect breakage early | multipaz 1.0, or a Longfellow naming change |
> | R15 | A programme reads an `[ESTIMATE]` as a commitment | M / M | keep method and assumptions attached to every number; never present a range without its baseline | any figure is quoted externally |
> | R16 | **The product's own audit carries unmitigated high findings on paths it advertises** (consent gating, key user presence, consent-screen claims, verifier authentication) | H / H | publish them (SG-1, NFR-16); decide release scope (§18, OQ-8); do not present the consent screen's claims as verified while the chain is unvalidated | the v1 release decision on OQ-8 is taken — **assistant-added row, not present in the record's A4** |

---

## 9. §15 Data Governance — DG-1 … DG-7 (prd.md:1621-1644) — VERBATIM

> ## 15. Data Governance
>
> - **DG-1 — v1 processes no real personal data.** Test identities are synthetic and labelled
>   as such; the "Population Register" is a sandbox panel that computes age predicates from a
>   typed birth date; the relying-party kit runs against mocks. This is why no DPIA is
>   claimed for v1 `[ASSUMPTION: a DPIA is not applicable while no real personal data is
>   processed; if a real issuer or a real register enters the deployment, this needs its own
>   assessment.]`
> - **DG-2 — The holder's device private key is never committed.** `demo-data/` is excluded
>   from the repository by policy and by check (SG-6, NFR-9).
> - **DG-3 — Data minimisation is enforced on the wire, not promised in prose.** The
>   transcript carries no isikukood and no full date of birth in any covered scenario (FR-7);
>   the age presentation carries a boolean and nothing else (FR-10, PG-2).
> - **DG-4 — Retention is ephemeral by design.** Sessions are single-use with a TTL, and a
>   replayed session returns `410`. The sandbox holds a run's state only for that run.
> - **DG-5 — No analytics, no telemetry.** The fork carries no Firebase Analytics or
>   Crashlytics, and no product telemetry is introduced by this release.
> - **DG-6 — The register lookup is a new data flow with no governance yet.** A fail-closed
>   lookup puts a register query on every presentation's critical path. Who sees that query,
>   what it reveals about a presentation, and how long it is retained are undecided and are
>   an open question (§18, OQ-3).
> - **DG-7 — Pseudonymity is not claimed.** ARF `ZKP_04` pseudonyms are out of scope (NG-8);
>   the per-path unlinkability position is stated per path with a test behind it (FR-42), and
>   where it cannot be claimed it is recorded as not claimed.

---

## 10. §16 Platform, Surfaces and Tone — IN FULL (prd.md:1646-1673) — VERBATIM

> ## 16. Platform, Surfaces and Tone
>
> **Surfaces this release ships or preserves:**
>
> | Surface | Form | Notes |
> |---|---|---|
> | Android wallet | Kotlin, Jetpack Compose, JDK 17; `:app:installLocal_mocks` for a device | `debug` targets placeholder backends and fails onboarding after PIN entry |
> | Verifier | Go services over a Rust staticlib (`libzkverify.a` through cgo) | needs `make deps`; `go vet ./...` clean; `zk` package tests need the Rust build |
> | Issuer / holder | Python | inputs pinned in `tests/requirements-ci.txt` |
> | Sandbox site | static HTML/CSS/JS served by a Python standard-library server, bilingual | no framework, no build step |
> | Conformance runner | JVM module today (`:zk-conformance`), extended by the AV and carrier runners | `-Dzk.regenerate=true` is the only way committed fixtures change |
> | Relying-party acceptance kit | a Go deployment profile, new in v1 | must not fork the verifier (FR-21) |
> | Documentation | `docs/`, `tests/README.md`, `docs/README.md` index | every new document is indexed (SM-7) |
>
> **Tone and aesthetic.** `DESIGN.md` is the design authority for the sandbox site — the
> TEDI-derived visual world, ria.ee chrome, Roboto and Material Symbols, the visitor's step
> rail and ledgers, Estonian first. This PRD does not restate it and does not override it.
> The wallet follows the platform conventions of its own host. The product principles the
> record pins, which every surface is judged against, are:
>
> 1. **Show, don't claim** — every number on a page comes from the run that just happened.
> 2. **Say what the shop did not learn as plainly as what it did.**
> 3. **Name the sandbox's shortcuts where they happen, not in a footnote.**
>
> To these the PRD adds one, for the surfaces this release introduces (the conformance
> reports and the acceptance kit):
>
> 4. **A report that hides its out-of-scope clauses is not a report** (FR-16, CM-1).

---

## 11. §18 Open Questions — OQ-1 … OQ-16 (prd.md:1717-1792) — VERBATIM

> ## 18. Open Questions
>
> **Phase-blockers — both resolved on 2026-10-23 before finalisation, and recorded here
> rather than deleted:**
>
> - **OQ-8 — the audit's TIER 1 findings — RESOLVED: the four user-facing gates are in v1.**
>   The maintainer directed that the four gates be closed in this release: `readerAuth` chain
>   validation to a trust anchor, a user-presence gate on credential keys, a consent gate that
>   resists guessing, and a consent step on the proximity share — carried as FR-45 … FR-48
>   (§4.7), with SM-9 and CM-7 as their metric and counter-metric. The audit's remaining
>   findings — supply-chain pinning, main-thread blocking, debug logging, the mock trust
>   surfaces and the release path — are left to a hygiene release, and their state is
>   published (SG-1, NFR-16) rather than implied fixed. Two related items the decision did
>   *not* cover stay open and visible in SG-1 and SM-8: signed authorization requests
>   (`EE-PRO-004`, with the `EE-PRO-003` client-id prefix) and `EE-PRO-010a`.
> - **OQ-10 — the missing external partner — RESOLVED: the kit ships, and SM-3 and SM-4 are
>   restated as unachieved.** No Estonian service owner or integrator is engaged, and the
>   record's own three external dependencies (register API, TARA terms, an Estonian test
>   wallet) are all absent. The relying-party acceptance kit therefore ships with
>   maintainer-produced acceptance evidence, SM-3 and SM-4 carry the reason and a revisit
>   condition instead of a pass (both are marked in §7.1), and any self-produced result is
>   labelled as self-produced wherever it is published. Recruiting a partner stays a goal,
>   not a release gate.
>
> **Open questions carried from the record** (none of these may be silently resolved in the
> product):
>
> - **OQ-1 — Is `ZKP_08` satisfiable at all?** The ARF requirement pins ZK schemes to the
>   ECCG mechanisms catalogue, and the catalogue names no ZK scheme — including the mandated
>   one. The record calls this "the largest single uncertainty". The product reports it
>   (FR-41) and does not resolve it.
> - **OQ-2 — Who owns the accepted circuit set, and who would host a registry?** The AV
>   Profile defers the set to a "scheme owner" without naming one. O4's value depends on the
>   answer.
> - **OQ-3 — Will RIA publish a register interface, and what will its access certificates
>   look like?** Also: will certificates be CT-logged, and what does a register lookup expose
>   about a presentation (DG-6)?
> - **OQ-4 — Is Article 45b(2) about an *issuer* class or a *compliant* class?** The record
>   calls this the single most consequential unresolved legal question. It is a legal
>   question and this PRD does not answer it (NG-4).
> - **OQ-5 — Does the ISO second edition's `ZkRequest`/`ZkDocument` shape differ from the
>   draft the carrier approximates?** The text could not be read during the record's
>   research `[UNVERIFIED]`.
> - **OQ-6 — Given that the AV Profile carries ZK only over the DC API path, is the
>   OpenID4VP `mso_mdoc_zk` carrier worth maintaining as a first-class surface?** The
>   landscape pass found the profile expects a plain mDoc over OpenID4VP because no standard
>   DCQL query for ZK exists there, while this fork's profile and this release both treat the
>   OpenID4VP carrier as primary. Recommended: keep both, but state in every artefact which
>   path is mandated (DC API) and which is profiled-but-unofficial (OpenID4VP), so no reader
>   infers EU endorsement of the second.
> - **OQ-7 — Which national thresholds beyond 18 are actually required?** O6 is gated on this
>   and on scheme-owner acceptance; unmapped thresholds stay unsupported.
> - **OQ-9 — Who owns the risk register?** No row in the record has an owner and this
>   product has one maintainer, so every risk's response depends on one person. Assigning
>   owners (even if only to name the maintainer) is a decision this PRD cannot take.
> - **OQ-11 — Should the proximity path keep advertising itself to relying parties at all?**
>   The wallet has a proximity screen; on a ZK-capable device an age request there is
>   refused. Either the refusal is stated to the venue as the product's behaviour, or the
>   proximity age path is removed from the advertised surface. Undecided.
> - **OQ-12 — Who hosts the sandbox site publicly, if anyone?** The site is the product's
>   public face and today runs on a host the visitor must build. Hosting it changes the
>   evidence story (a stranger's run is not a stranger's reproduction) and needs a decision.
> - **OQ-13 — Which external implementation should the carrier suite target first?** The
>   record leaves this open between the multipaz sample and the reference wallet. The
>   landscape pass adds the Commission's iOS library and Longfellow's own Go verifier service
>   as candidates.
> - **OQ-14 — Does the project want an EU-level role at all?** The record's own non-goals
>   keep this product out of certification, out of standards filing and out of other Member
>   States' implementations. If the answer changes, §2's audience changes with it.
> - **OQ-15 — Will RIA adopt the AV Profile's attestation, and is a foreign AV attestation
>   admissible to an Estonian relying party?** The record raises both and neither is settled
>   in published form; the relying-party kit's four acceptance cases assume admissibility.
> - **OQ-16 — Does a two-predicate refusal cost the product a lawful use case in practice?**
>   The record's scenario material suggests a service that needs "over 18 and over 21" would
>   have to make two requests; whether that is acceptable to an Estonian service owner is
>   untested and NG-9 does not move until O3 lands.

Note for the record: the §18 heading count is 16 identifiers (OQ-1 … OQ-16); OQ-8 and OQ-10
are listed first as resolved phase-blockers, the remaining fourteen under "Open questions
carried from the record".

---

## 12. One-line index of every FR-1 … FR-48, SM-1 … SM-9, CM-1 … CM-7, A-1 … A-9

Titles below are verbatim from the PRD's own headings/lines; the gloss after the em dash is a
compression of that item's own text, not new content. `(shipped)` / `(new)` is the PRD's own
marker; `(shipped, fixture-verified)` is FR-2's.

### FRs

| # | prd.md | One line |
|---|---|---|
| FR-1 | 360 | An age predicate is proved over the OpenID4VP ZK carrier (shipped) — `mso_mdoc_zk` DCQL query answered with a Longfellow proof, no attribute disclosed; `direct_post.jwt` default; cleartext and false-predicate paths exist only behind explicit verifier flags. |
| FR-2 | 380 | An age predicate is proved over the DC API carrier (shipped, fixture-verified) — ISO/IEC 18013-7 Annex C parsed from `navigator.credentials.get`, HPKE-encrypted; claim stated as fixture-verified only, on-device run `PENDING-DEVICE`. |
| FR-3 | 396 | The verifier validates a proof and returns an encrypted verdict (shipped) — ordered checks (transcript, allowlist, proof, doctype/namespace/attribute), each rejection naming the failed check. |
| FR-4 | 413 | At most one age predicate per presentation (shipped) — more than one `age_over_NN`, required or optional, refused on OpenID4VP, DC API and proximity. |
| FR-5 | 429 | A predicate held false is never transmitted (shipped) — the user is told and the relying party receives the no-match answer (nothing over OpenID4VP, `RESULT_CANCELED` over DC API, session terminated over proximity); the prover guard and the verifier's separate refusal both hold and both are asserted; the claim covers the **default configuration** only — not that a false-predicate proof can never reach a wire (behind `-allow-false-predicate`, a proof of `age_over_18 = false` is posted and `valid:false` returned). **The sandbox site's third attack is the same negative control in front of a visitor.** |
| FR-6 | 453 | Weaker requests are refused, with the reason shown and the tier labelled (shipped) — unauthenticated or unsatisfiable ZK request refused before consent; dedicated refusal message distinguishable from retryable error and from cancellation. |
| FR-7 | 471 | Multi-credential sessions are answered per credential (shipped) — both credential identifiers in the log; missing credential fails the session naming it; no isikukood and no full DOB in the transcript. |
| FR-8 | 482 | The EE-PoA attestation follows its consumption and validity rules, with one declared shortfall (shipped) — batch of three declared as below the spec's ≥30; user-started renewal from the re-issuance window. |
| FR-9 | 508 | Credential keys are generated in hardware (shipped) — StrongBox where advertised, TEE otherwise; no API returns private-key material; remote-HW path is a mock. |
| FR-10 | 520 | The relying party never receives an attribute value in an age presentation (shipped) — boolean and nothing else; consent screen shows requester, predicate and channel before the proof is produced. |
| FR-11 | 554 | A normative clause registry exists (new) — every AV-Profile clause has a test id or an "out of scope for v1" note with a reason; machine-readable; ≥ 20 clauses. |
| FR-12 | 564 | AV-profile fixtures are minted against the profile's own parameters (new) — doctype `eu.europa.ec.av.1` and the `ZkSystemSpec` tuple, version-labelled. |
| FR-13 | 576 | A conformance runner produces a per-clause report (new) — machine-readable, names each clause and verdict, non-zero exit on in-scope failure, ≥ 8 negative controls, run against ≥ 2 independent implementations. |
| FR-14 | 590 | The report is reproducible by a third party (new) — commit, profile revision, host class and commands named; Python inputs pinned; Rust toolchain unpinned so a report must name it. |
| FR-15 | 604 | The AV issuance path and its invocation are covered (new) — `av-vci://` invocation exercised pass or fail; AVSUA-equivalent path exercised or recorded out of scope with reason. |
| FR-16 | 615 | In-scope and out-of-scope clauses are both published (new) — out-of-scope clauses with reasons **adjacent** to the covered ones, not in a footnote; no banned claim vocabulary. |
| FR-17 | 633 | A carrier runner covers the existing cases and publishes vectors (new) — covers what `tests/e2e.sh` exercises; vectors regenerated only from the other language's implementation. |
| FR-18 | 644 | A failing case names the binding property (new) — transcript, thumbprint, circuit parameters or attribute identifier, never a generic refusal; a corrupted vector produces that named failure. |
| FR-19 | 653 | One external implementation is exercised and the result published (new) — pass or fail, named, versioned, dated. |
| FR-20 | 663 | Carrier drift is version-managed (new) — fixtures version-labelled to the external document version; `carrier.go` stays the single adaptation point. |
| FR-21 | 685 | The kit deploys as a documented service profile (new) — deployment profile over the existing verifier, no fork; a reader with no prior ZK knowledge reaches first successful verification via the README alone in under a day, time published. |
| FR-22 | 696 | A TARA-facing adapter contract exists and is exercised (new) — interface and assumptions published with the mock points named; integration test or absence recorded as an unmet dependency. |
| FR-23 | 707 | The register lookup is pluggable, fail-closed, and mocked (new) — absent, unparseable or ambiguous entry refuses; refusal names which lookup failed; bounded cache with an explicit, logged stale policy. |
| FR-24 | 720 | The register interface is proposed, not guessed (new) — written interface proposal with ARF relying-party topics mapped and fail-closed behaviour specified; an Estonian service owner has reviewed it or the review is recorded as not yet obtained. |
| FR-25 | 731 | The four acceptance cases are the acceptance test (new) — PID wallet, AV-attestation wallet, unattested ZK request, unregistered relying party; the last two refuse and the missing-registration case names the register as the cause. |
| FR-26 | 743 | A refusal is distinguishable from a cancellation (new) — the UI contract distinguishes the two states with an e2e case for each; the log records the refusal reason and no attribute value. |
| FR-27 | 752 | The integrator's first verification is measured (new) — time-to-first-successful-verification on ≥ 3 integrators with baseline, method and confidence; fewer than three is published as unmeasured with the reason. |
| FR-28 | 774 | Every shown number comes from the run that just happened (shipped) — registration, attestation, proof and verdict run against the real components; every figure rendered from that run's server response. |
| FR-29 | 785 | The ledger shows both columns (shipped) — disclosed boolean and withheld attributes as two contrasting columns with per-item captions; wording tracks the actual payload without a copy edit. |
| FR-30 | 796 | The visitor can try to cheat and watch it fail (shipped) — ≥ 1 deliberate attack showing the verifier's named refusal, which is the verifier's real response and not a scripted animation. |
| FR-31 | 806 | The sandbox's shortcuts are named where they occur (shipped) — the client-side age computation is named at the point it happens; synthetic identities labelled inside the flow, not only in the footer. |
| FR-32 | 817 | The site is bilingual, Estonian first (shipped) — every string in both languages, toggle on every page, Estonian default; a one-language string fails the site's own check. |
| FR-33 | 827 | The independence notice is present and the state branding is absent (shipped) — notice in utility bar, info alert and footer; no RIA logo or coat of arms anywhere; no endorsement claim. |
| FR-34 | 836 | A missing toolchain produces an honest failure, not a fake (shipped) — the page says the prover or verifier is not built and names `make deps`; no page shows a timeline or verdict the host did not produce. |
| FR-35 | 846 | The site holds the public-sector accessibility baseline (shipped) — WCAG 2.1 AA with EN 301 549 as presumption; keyboard-complete, visible focus, token-held contrast, no colour-only signalling; long-running steps announce; reduced motion honoured. |
| FR-36 | 868 | No artefact asserts compliance or certification (new) — `make check` fails on the banned vocabulary ("conformant", "certified", "compliant", "meets the profile"). |
| FR-37 | 881 | Every published figure carries its baseline, method and confidence (new) — no range presented as a commitment; unmeasured rows say `PENDING-DEVICE`. |
| FR-38 | 891 | Conformity statuses are derived from tests, and stale rows are corrected (new) — the `issuerAuth` row corrected, the carrier row's contradiction corrected, no row claimed without a test or quoted source. |
| FR-39 | 906 | The missing external clause maps are published (new) — HAIP 1.0 clause-by-clause, EC TS13 (`mso_mdoc_zk` supported, `zk-jwt` not), ECCG mechanisms with soundness vs privacy exposure separated. |
| FR-40 | 918 | The scope of the claim is written down (new) — narrow scope (one boolean on one attestation); attestation-ordering statement (AV preferred, EE-PoA fallback); `ZKP_03`, `ZKP_04`, `ACP_03` out of scope with reasons. |
| FR-41 | 931 | The ZKP_08 tension is reported rather than resolved (new) — the conditional reading (a) and the re-check trigger stated; 109-bit circuits declared against the 125-bit floor. |
| FR-42 | 944 | Unlinkability and authentication are stated with a test behind them (new) — per-path unlinkability statement with a test asserting no tracking identifier; the reasoned refusal to claim `ZKP_07` carried, not upgraded. |
| FR-43 | 958 | Standards engagement is recorded, including what has not been filed (new) — DCHP #17 prepared and not filed, OpenID4VP #627 tracked, every deliberate protocol deviation listed with its rationale in one place. |
| FR-44 | 973 | The maintenance exposure of a dormant upstream is written down (new) — vendoring, upgrade cadence and succession position; licence and vendored-artefact inventory; strict superset verified. |
| FR-45 | 999 | A requester name shown at consent is authenticated (new) — `readerAuth` chain validated to a trust anchor before any name renders; unvalidated ⇒ shown as unverified with refusal as the default and the reason visible. |
| FR-46 | 1012 | Credential keys require user presence (new) — a platform user-authentication prompt the key operation depends on, on proof generation, plain presentation and signing. |
| FR-47 | 1024 | The consent gate resists guessing (new) — failed attempts counted in persistent state surviving restart, with a lockout threshold and escalation path stated; the relaunch reset and the simulated "remote check" removed. |
| FR-48 | 1035 | A proximity share carries the same consent as a remote one (new) — the consent gate passes before a document is emitted; what is shared and with whom stated on the same terms as the remote path. |

### SMs (§7.1 Primary Metrics prd.md:1200-1224; §7.2 Secondary prd.md:1225-1249)

| # | prd.md | One line |
|---|---|---|
| SM-1 | 1202 | An AV-Profile conformance report exists and someone else reproduces it — ≥ 20 clauses with ≥ 8 fail-closed negative controls, reproduced from a clean checkout by a person with no prior contact with the project. |
| SM-2 | 1206 | The carrier suite has been run against an implementation that is not this one, and the result is published pass or fail — one external implementation, named, versioned and dated. |
| SM-3 | 1210 | An Estonian service owner completes the four acceptance cases and states whether the register interface proposal is usable — **Status at v1: unachieved, and recorded as such** (no service owner engaged; maintainer-produced evidence does not satisfy it per CM-2). |
| SM-4 | 1216 | Time-to-first-successful-verification is measured, not estimated — **Status at v1: unmeasured, and published as unmeasured** (integrator count zero). |
| SM-5 | 1220 | No artefact asserts compliance or certification, and the stale status rows are corrected — banned-vocabulary check green in `make check`; `issuerAuth` row and every other hand-maintained row verified against the code. |
| SM-6 | 1227 | Nothing that was green is no longer green — `make check` exits 0; `:app:testLocal_mocksUnitTest` stays at 169 `@Test` over 26 files; `:zk-conformance:test` green with byte-identical fixtures; `go vet ./...` clean; `tests/e2e.sh` 13 cases. |
| SM-7 | 1232 | Every new document is indexed — in `docs/README.md`, and every new test suite in `tests/README.md` with what it proves. |
| SM-8 | 1235 | The audit's stale findings are re-dated against HEAD, and the one live gap is closed — `CertificateChainValidator` test gap and `:zk-conformance` CI gap both closed on 2026-09-25 and **neither is true at this PRD's commit**; the SD-JWT "asserts nothing" claim is still true. |
| SM-9 | 1245 | The four consent-integrity gates are closed, each with a test behind it — four negative tests, one per gate, each failing if the gate is removed. |

### CMs (§7.3 Counter-Metrics, prd.md:1251-1274)

| # | prd.md | One line |
|---|---|---|
| CM-1 | 1253 | Counter to SM-1: clause count is not the metric — a registry growing without negative controls behind the new clauses has regressed. |
| CM-2 | 1257 | Counter to SM-1: "external reproduction" must mean external — a reproduction by anyone involved in building the artefact does not satisfy SM-1. |
| CM-3 | 1259 | Counter to SM-3 and SM-4: integration speed must not be bought by dropping refusals — the four cases include two refusals. |
| CM-4 | 1262 | Counter to SM-4: the integrator count must not be met with a weaker kit — a kit that only works against its own mock register is not evidence of integration. |
| CM-5 | 1264 | Counter to SM-5: the banned-vocabulary check must not be satisfied by going vague — a claim replaced by silence fails. |
| CM-6 | 1267 | Counter to every metric here: honesty must not cost more than it buys — honest about trivia while leaving load-bearing figures unlabelled misses the point. |
| CM-7 | 1271 | Counter to SM-9: consent integrity must not be bought with breakage — every new refusal names its cause (NFR-14) and the negative tests assert refusal *reasons*, not merely refusal. |

### A-1 … A-9 (§19 Assumptions Index, prd.md:1794-1809)

| # | prd.md | One line |
|---|---|---|
| A-1 | 1801 | The first reader is an assessor or programme evaluator, and the Ministry/RIA reader is served by the same artefact. |
| A-2 | 1802 | UJ protagonist names and one-line contexts — drafted from the record's actor types, not narrated by the user. |
| A-3 | 1803 | The sandbox site and the presentation spine are in v1 as shipped behaviour rather than as new work. |
| A-4 | 1804 | Risk rows carry no owner; revisit conditions stand in for ownership. |
| A-5 | 1805 | No DPIA is applicable to v1 because no real personal data is processed. |
| A-6 | 1806 | PRD identifiers are kept distinct from `epics.md`'s `FR1–FR66` / `NFR1–NFR10` (11 epics, 76 stories). |
| A-7 | 1807 | The O6 multi-threshold estimate excludes the unbounded external wait for scheme-owner-accepted circuits. |
| A-8 | 1808 | The intent record's count discrepancy is resolved: the specification holds 211 requirements across 14 families (`EE-AGE-1`/`EE-AGE-2` are conformance-profile names, not requirements). |
| A-9 | 1809 | The user's decisions on entry point, Vision and v1 scope, plus the two phase-blocker decisions, were taken from scored rubrics whose separation was contested in every case. |

---

## 13. addendum.md — mechanism / transport detail and rejected alternatives

`addendum.md:1-5` framing, verbatim:

> # Addendum — EE-EUDIW ZK Age-Verification Wallet PRD
>
> Material that belongs to a downstream document (architecture, solution design, UX spec) or
> that earned a place here but does not fit the PRD's own narrative. Nothing here is a PRD
> requirement; where the PRD needs a fact from this file, it states that fact there.

### 13.1 D1. Options considered and rejected (addendum.md:9-56) — affects what users see

D1 preamble, addendum.md:11-14, verbatim:

> The PRD's shape was decided through three scored rubrics, each of which escalated before it
> was asked. Scores, bands and separations are in `.decisions.log` beside this file; the
> rejected alternatives and the reason each lost are here, because a reviewer asking "why is
> the Vision framed this way" should get an answer rather than a reconstruction.

**D1.1 Entry point — section order** (addendum.md:16-24), verbatim:

> Chosen: **hybrid — capability-first spine with journeys captured where a real session
> exists.** Rejected: capability-first with no UJ section (loses the two journeys a real user
> actually runs: the visitor's six-step sandbox flow and the integrator's first verification);
> journey-led (inverts the document for an assessor-facing product where four of five
> components are not UI). The rubric's separation was *marginal* and the order flipped when
> the hybrid's journeys were rated one band lower, which is why §2.3 keeps each journey
> anchored to a documented flow rather than an invented one.

**D1.2 Vision centre of gravity** (addendum.md:26-35), verbatim:

> Chosen: **evidence-first — the product is the assessable evidence estate.** Rejected:
> Estonian relying-party enablement as the centre (the strongest argument against the choice:
> the relying-party kit is "the only recommendation a buyer can act on before 2028"); standards
> influence as the centre (the record ranks it below both, and the fork's own components are
> not built for it). The decision rested on the record's own language — "a conformance-test and
> packaging gap, not a cryptographic redesign"; "the distinctive artefact here is the verifying
> side plus its refusals"; "a research artefact whose value is its evidence, not its
> throughput" — and on the fact that the fork's public front door ships no APK.

**D1.3 v1 scope** (addendum.md:37-44), verbatim:

> Chosen: **the innovation assessment's initial release — O1, O5, O2 — with phases D1 and D3
> folded in.** Rejected: the roadmap's M-phase alone, which would defer the relying-party kit
> (the roadmap's own sequencing rationale argues for exactly this order, because P1's four
> acceptance cases depend on M1's fixtures — recorded as the steelman for the rejected
> option); and the pilot phases P2/P3, which would put a registration-certificate policy and a
> circuit registry into a release whose value is evidence rather than capability.

**D1.4 Not carried as a user journey** (addendum.md:46-56), verbatim — this is the one that
directly shapes the UX journey set:

> The record's use case U3 (offline/proximity age proof at a venue) is a non-goal rather than
> a journey: the wallet has a proximity presentation screen
> (`app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/`), but there is no proximity path
> for a Tier-2 proof (`EE-ZKP-045a`), the BLE transfer budget for a ~360 KB proof is
> unmeasured, and on a ZK-capable device an age request over the proximity path is refused
> outright (`docs/CONFORMITY.md`). A journey whose climax is a refusal is a real journey, but
> it is the *refusal* being advertised, not a proof, and it belongs in the open question at
> §18/OQ-11 rather than in §2.3. An "assessor journey" was likewise excluded: the reproduction
> workflow is real, but it belongs in success metrics and NFR-8, not in a user journey.

### 13.2 D2. Transport and mechanism detail (addendum.md:60-95) — affects what users see

Preamble, addendum.md:62: "Technical-how for the architecture pass. The PRD states
capabilities; this states the wire."

- **OpenID4VP carrier** (addendum.md:64-70), verbatim:
  > DCQL with the format identifier `mso_mdoc_zk`, a `meta.zk_system_type` registry, and a
  > `ZkSystemSpec` parameter tuple of `circuit_hash`, `version`, `num_attributes`,
  > `block_enc_hash`, `block_enc_sig`. The response is `direct_post.jwt`: direct `ECDH-ES`
  > under P-256 with `A128GCM` or `A256GCM`, encrypted to the relying party's published key.
  > The carrier profile of record is `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md` v1.0,
  > with committed vectors under `tests/vectors/carrier-v1/`. `verifier/go/oid4vp/carrier.go`
  > is the single adaptation point.
- **DC API / Annex C carrier** (addendum.md:71-73), verbatim:
  > Parsed from `navigator.credentials.get` parameters, HPKE-encrypted to the verifier's key,
  > byte-compatible with the reference verifier's standard-library HPKE. Fixture-verified; no
  > device run exists.
- **Transcript binding** (addendum.md:74-79), verbatim:
  > The session transcript is CBOR opening `83 f6 f6 82 71` (null, null, a two-element array
  > beginning `OpenID4VPHandover`) and closing with a 32-byte bstr equal to the SHA-256 of the
  > handover-info encoding. Changing `client_id`, `nonce`, `response_uri` or a 32-byte
  > `jwkThumbprint` changes the transcript bytes; a shorter thumbprint is rejected.
  > `TranscriptForFlow`/`Flow` is the single transcript entry point and mixing flows is
  > structurally banned.
- **Proof and circuit** (addendum.md:80-83), verbatim — UX-visible because timing and size
  are what a visitor watches:
  > Longfellow (`longfellow-libzk-v1`), ~353–360 KB per proof, ~6.1 s proving on the
  > development host, verification 2,414 ms per valid proof on a 2 vCPU host (and mean
  > 4.02 s, p50 3.83 s, p95 4.91 s on an i5-8365U-class host, ~95 % of it sumcheck
  > arithmetic). The bundled v7 circuits are 109-bit; the ECCG recommended floor is 125-bit.
- **Round-3 deviations recorded as deliberate and documented** (addendum.md:84-89), verbatim:
  > (with their rationale, not as oversights): the agreement key precedes `client_id` in the
  > authenticator data; the per-presentation JWE profile pins RFC 3394 §4.6 with an asserted
  > cross-language token; credential dynamic data is carried in a CBOR int key (`val`) with no
  > AAD JWE and an explicit key-binding gate at consumption; the ISO dcapi path decodes a
  > strict CBOR subset in-house rather than delegating to upstream parsers.
- **Verifier properties worth carrying into architecture** (addendum.md:90-95), verbatim:
  > a strict CBOR subset parser with shared budgets, server-recomputed transcripts (the holder
  > never gets a vote), single-use TTL sessions with shed-before-claim, allowlist-before-FFI
  > plus a startup hash self-check, trust-store key pinning that selects but never adds,
  > anti-downgrade gates (`EE-ZKP-042`, `EE-ZKP-045`, `EE-ZKP-045a`, `EE-ZKP-050`,
  > `EE-ZKP-051`), and cgo thread-pinning-aware admission control.

### 13.3 Other addendum sections relevant to surface copy (not mechanism, but they bound it)

- **D3 Competitive picture (12 comparators — summary)** (addendum.md:99-122). What it changes
  for UX copy, verbatim from addendum.md:103-109:
  > What it establishes: the scheme is not this fork's invention (the Commission mandates it),
  > so differentiation rests on verification evidence. Within the ten-plus comparators examined
  > on 23 October 2026, none paired ZKP age proofs with a Go/Rust verifier and a published
  > negative-control test suite; five of six differentiation rows read "none in the set" for the
  > best comparator. Nobody was ahead on *ZK age-proof verification*: the Commission's reference
  > wallet described ZKP as "an optional feature", multipaz shipped the prover library without a
  > verifier product, and walt.id, EUDIPLO, inji, wwWallet and impierce claimed no ZKP at all.

  and verbatim from addendum.md:111-117:
  > What the later landscape pass changed: that differentiation is weaker than the record
  > states. Longfellow itself now ships a Go HTTP verifier service for `mso_mdoc_zk` with
  > Docker; the Commission ships verifier endpoints, a trust validator and an iOS Longfellow
  > ZKP library; and Google Wallet documents `mso_mdoc_zk` as a public relying-party
  > integration step. "The only ZK verifier" is no longer sayable; the surviving claim is
  > narrower — *verifier-side conformance and negative-control evidence, published*. The PRD
  > states the narrower claim (§1, NG-1) for that reason.
- **D4 Requirement index (41 rows — distribution)** (addendum.md:126-132): "**5 supported ·
  23 partial · 7 missing · 5 not applicable · 1 not verified · 0 compliant**, with `ZKP_08`
  the single unsatisfiable row."
- **D5 Sizing and cost data, with method** (addendum.md:136-158): Q1 6–12 engineer-months;
  Q2 ≈ 0.42 verifications/s per 2 vCPU instance; Q3 ~360 GB per million presentations;
  **Q4 none** ("Every figure is `PENDING-DEVICE`; the harness exists and the prover wiring
  does not."); and "**Market sizing: deliberately absent.**"
- **D6 Actor detail beyond the journeys** (addendum.md:162-175), verbatim on the two actors
  the UX run designs for:
  > - **The holder** appears in this PRD only as the subject of privacy guarantees. The
  >   record's own reading: the wallet holder is not the buyer, and the product's promise to
  >   them is narrower than the product's promise to a relying party — one boolean, no false
  >   predicate, no central log, no silent downgrade.
  > - **The e-resident** (UJ-4) is a constraint case rather than a market: no Estonian
  >   personal identification code, so any design that assumes one is wrong for them, and the
  >   two-credential session is what makes their onboarding complete.
  > - **The venue reader** is the actor this PRD deliberately does not serve yet (D1.4).
  > - **The assessor** is the first reader in §1's [ASSUMPTION] and has no journey because
  >   their session is the reproduction workflow (NFR-8, SM-1) rather than a product surface.
  > - **Institutional readers** (RIA, the Ministry, the scheme owner, EU programme contacts)
  >   are stakeholders in §11, not users; their decisions are dependencies, not journeys.
- **D7 Bookkeeping** (addendum.md:179-188): `.memlog.md`, `.decisions.log`, the four
  `extract-*.md` files, `review-*.md`, `reconcile-*.md`, and the restatement that the PRD's
  numbering is distinct from `epics.md`'s.

---

## 14. Cross-references the UX run needs from §§ not extracted in full

These are outside the named extraction list but are load-bearing for the v1 UX scope; each is
quoted verbatim at the cited line so nothing here is inferred.

- **§14 Operational Requirements, OR-6** (prd.md:1615-1617), verbatim:
  > **OR-6 — Failure is diagnosable without a support channel.** Every refusal names its
  > cause (NFR-14), and every published result names the commit, the document revision and
  > the commands that produced it (NFR-8).
- **§14 OR-1** (prd.md:1599-1603), verbatim:
  > **OR-1 — v1 ships no service. It ships a reference deployment.** The relying-party
  > acceptance kit is a deployment profile an operator runs; no availability target, no
  > support window, no on-call rotation and no incident process is claimed, because R1 is
  > deferred (§6.2). Stating this is the point: a reader must not infer an operational
  > commitment from a deployment artefact.
- **§14 OR-4** (prd.md:1609-1611), verbatim:
  > **OR-4 — The relying party's log carries the ARF Topic 19 fields and no attribute
  > value.** What an operator can log about a presentation, and what it must not, is fixed
  > here rather than left to the deployment.
- **§14 OR-5** (prd.md:1612-1614), verbatim:
  > **OR-5 — Logging is local-first and central-free.** The wallet's activity log is the
  > user's and distinguishes linkable from new-visit presentations; there is no central age
  > log (PG-5).
- **§3 Glossary entries that the FR/UJ text depends on, verbatim** (prd.md:254-329):
  - *Fail closed* (prd.md:285-286): "on absent, unparseable or unreachable input, refuse the
    operation and say which lookup failed, rather than allowing it or substituting a default."
  - *Issuer-chain disclosure* (prd.md:294-296): "a proof that carries `msoX5chain` reveals the
    issuing authority to the relying party even though no attribute value crosses. It is a
    stated limit of the privacy position, not an oversight, and it appears with PG-2 and
    NFR-15."
  - *Linkability* (prd.md:297-300): "the property that two presentations cannot be tied to one
    holder. Tier 1 (plain, once-only attestations) defeats relying-party linkability but not
    issuer–verifier collusion; Tier 2 adds nothing against a colluding issuer and verifier
    either. The per-path position is stated per path with a test behind it (FR-42)."
  - *Tier 1* (prd.md:315-316): "the once-only, batched, plain attestation posture.
    Mandatory and deployable today."
  - *Tier 2* (prd.md:317-319): "the zero-knowledge proof posture. Specified in full, gated
    by `EE-CNF-006` and `EE-CNF-007`, and operated in this fork as a pilot below the ACM's
    recommended level." — this is the "tier" FR-6 requires the wallet to label, and PG-4 the
    fallback label.
  - *Intended use* (prd.md:289-291): "what a relying party is registered as entitled to
    request, read from its registration or access certificate. Where it is not readable, the
    policy falls back to refusal."
  - *Test shop* (prd.md:314): "the sandbox site's synthetic relying party, labelled as such."
  - *Sandbox site* (prd.md:312-313): "the static bilingual site that drives the real issuer,
    prover and verifier for a visitor, without an Android phone."
  - *Age predicate* (prd.md:258-259): "a boolean claim of the form `age_over_NN`. This product
    discloses at most one per presentation and never transmits one held `false`."
- **§5 Non-Goals that constrain copy** (prd.md:1050-1122): NG-1 no broader novelty claim;
  NG-2 no compliance/conformity/certification claim; NG-3 not a certification service; NG-4
  not legal advice; NG-5 no claim on the ACM's recommended level; NG-6 proximity Tier-2 ZK out
  of scope; NG-7 no ZK system other than `longfellow-libzk-v1`; NG-8 no general-purpose ZK
  capability; NG-9 no lawful two-predicate presentation; NG-10 no multi-threshold library;
  NG-11 no status or revocation mechanism for the age attestation; NG-12 no real issuer;
  NG-13 not RIA's product and not a state wallet; NG-14 not a register, intermediary or trust
  anchor; NG-15 no X-Road integration; NG-16 no wallet-provider certification path; NG-17 no
  published interop run against the reference wallet; NG-18 no standards submission made on
  the reader's behalf; NG-19 no commercial commitment.
- **§4 opening rule on "unmeasured" consequences** (prd.md:339-343), verbatim:
  > **A consequence that permits "or record it as unmeasured / not obtained / out of scope" is
  > satisfied only by a record that names the reason, the date, and the evidence that would
  > close it.** v1 may discharge at most three of them (FR-15, FR-22, FR-24, FR-27 and FR-42 each
  > contain one, so at least two must be closed); a fourth means the release is being defined
  > by what it did not do, and that is a decision, not a line item.

---

## 15. What this extract deliberately does not contain

- No content from `epics.md`; its `FR1–FR66` / `NFR1–NFR10` numbering is not used or mapped
  here (prd.md:29-32, A-6).
- No content from `DESIGN.md`, the research corpus, `docs/CONFORMITY.md` or `PRODUCT.md` —
  each is referenced by the PRD but not reproduced. Where the PRD names `DESIGN.md` as the
  design authority (prd.md:769-770, 1660-1662), that naming is quoted; the document's own
  contents are not in these two files.
- No `EE-*` requirement text; only the identifiers the PRD cites.
- No inference: every line above is from `prd.md` or `addendum.md`, or is one of the two
  statements in this document's own front matter about identifier schemes and section
  existence.
