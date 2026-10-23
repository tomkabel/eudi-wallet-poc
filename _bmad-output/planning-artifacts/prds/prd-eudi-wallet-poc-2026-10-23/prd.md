---
title: EE-EUDIW ZK Age-Verification Wallet (independent fork of eudi-wallet-poc)
status: final
created: 2026-10-23
updated: 2026-10-23
---

# PRD: EE-EUDIW ZK Age-Verification Wallet

*Working title — confirm.*

## 0. Document Purpose

This PRD is for the maintainer of this fork first, and for the readers the fork is built
to convince second: an assessor, an Estonian relying party considering integration, a
Ministry of Justice and Digital Affairs or RIA reader, and an EU programme contact. It is
written to be source-extracted by downstream workflows — `bmad-ux`, `bmad-architecture`,
`bmad-create-epics-and-stories` — so its vocabulary is fixed in §3 Glossary, its features
are grouped with globally numbered FRs nested under them, and its inferences are tagged
`[ASSUMPTION: …]` inline and indexed in §19. Disagreements between corpora are recorded in
§18 Open Questions rather than resolved by assertion.

It builds on inputs that already exist and does not duplicate them. The intent record is
`docs/research/2026-10-23-innovation-assessment.md`,
`docs/research/2026-10-23-estonia-alignment-assessment.md`,
`docs/research/2026-10-23-shared-appendices.md`, `spec/EE-EUDIW-TS-1.0.md`, `PRODUCT.md`
and `docs/CONFORMITY.md`. Measurement evidence lives in `docs/MEASUREMENTS.md`; the
carrier of record is `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`; the design system
for the sandbox site is `DESIGN.md`. `_bmad-output/planning-artifacts/epics.md` already
decomposes the same research corpus into 11 epics and 76 stories, with its own FR1–FR66
and NFR1–NFR10 numbering — this PRD is the missing document *above* it, and its identifiers
are deliberately distinct (`FR-1`, `NFR-1`, `UJ-1`) so the two can be read side by side.
Depth that belongs to a downstream document rather than to this PRD is kept in
`addendum.md` beside it.

Evidence discipline is inherited from the intent record and is not negotiable here: claims
carry the record's own labels — `[VERIFIED]`, `[ESTIMATE]`, `[JUDGEMENT]`, `[UNVERIFIED]`
— and nothing in this PRD upgrades one. Every repository fact this PRD asserts was verified
against the working tree at commit `2d38feb`, and where a published artefact and the tree
disagree, the tree wins and the artefact is corrected (FR-38, SM-8).

## 1. Vision

This fork is an independent build on top of open-eid's EE Wallet PoC that turns a demo
into five runnable, assessable components — an Android wallet, a Go verifier over a Rust
staticlib, a Python issuer and holder, a conformance runner, and a relying-party
acceptance kit — around one narrow and unusually well-specified claim: an ISO/IEC 18013-5
mdoc age predicate proved with a Longfellow zero-knowledge proof, carried over OpenID4VP
and over the ISO/IEC 18013-7 Annex C Digital Credentials API path, with the wallet and the
verifier *both* refusing to accept less. The claim is not a cryptographic invention. The
Commission's Age Verification Profile fixes the doctype `eu.europa.ec.av.1`, the single
mandatory attribute `age_over_18`, and the proving system `longfellow-libzk-v1` — "an
implementation SHALL use that system"; and this fork already mints and consumes that
attestation and prefers it to its own Estonian `ee.riik.poa.1`. The gap between the fork
and the EU's age-verification blueprint is, in the record's words, a conformance-test and
packaging gap, not a cryptographic redesign `[VERIFIED]`.

So the product is the evidence. An assessor, an integrator or a Ministry reader clones the
repository, runs the documented commands, and reaches their own verdict without having to
trust a page of prose. Every published number comes from the run that just happened; every
refusal the system performs is published as a test rather than asserted in a claim; every
obligation it does not meet is written down as unmet. The sandbox site and the
relying-party acceptance kit are the two places that evidence is consumed by a human —
the visitor's guided six-step proof and the integrator's first successful verification —
not the whole of it. The warrant for building it this way is that the assessment of
zero-knowledge age verification is a *reproducibility* problem before it is a technology
problem: the scheme is mandated, the comparators have not published verifier-side
negative controls, and a funding or procurement evaluator cannot manufacture that
evidence easily `[JUDGEMENT]`.

Why it matters, and what it must never claim, are the same sentence. Estonian personal
identification codes embed the date of birth, so an age check answered plainly
over-discloses by construction. Estonian public-sector services must be ready to accept
*other Member States'* certified wallets from end-2026 while the national wallet launches
only in 2028 — which makes the relying-party side the near-term opening `[VERIFIED]`.
Against that, the fork today meets no compliance or certification claim and must not make
one, ZKP_08 is a conditional reading rather than an assertion, the ISO Annex C path has no
on-device evidence yet, and every build it can produce is debug-derived `[VERIFIED]`. The
Vision's discipline follows: the product earns attention by being *assessable and honest
about its limits*, and it fails the moment it claims more than its tests support.

`[ASSUMPTION: the first reader is an assessor or programme evaluator assessing the ZK age
path, and the Ministry/RIA reader is served by the same artefact — inferred from the
launch-grade stakes answer and the intent record; the follow-up question asking for this
in the user's own words went unanswered.]`

## 2. Target User

The intent record's own reading, which this section follows rather than re-derives: "the
primary buyer is a programme/regulator-adjacent actor or an integrator, not an end user;
the wallet holder appears only as the subject of privacy guarantees (no `false` predicate
transmitted, at most one predicate per presentation)." `[VERIFIED]` Four actors follow
from it — a programme-adjacent assessor, an Estonian relying party and its integrator, the
holder, and the visitor to the sandbox site — plus the attestation provider the record
names for the threshold work.

### 2.1 Jobs To Be Done

Functional:

- **Test a conformance claim instead of asserting one.** A wallet programme, an
  attestation provider or a relying party that must meet the EU age-verification blueprint
  wants a real implementation to test against, not a description. (O1)
- **Answer "does my service work?" before the test target exists.** An Estonian
  e-service operator must accept foreign certified wallets from end-2026 with no Estonian
  wallet to test against until 2028 and no register API to check registrations against.
  (O2, problem P1)
- **Ask for the minimum and be able to show it.** An age gate needs a boolean; an
  Estonian isikukood carries a birth date, so a plain age check over-discloses. (P2)
- **Answer "which circuit proved this?" months later.** Wallet providers, relying parties
  and programme owners must agree one accepted circuit set, and an auditor must be able to
  trace a presentation to the circuit that produced it. (O4)
- **Resolve "which carrier" without integration surprises.** Implementers of wallets and
  verifiers must interoperate on a ZK carrier that no OpenID specification yet defines.
  (O5)
- **Turn "the relying party asked for 21" into the right single predicate** where national
  law has several age thresholds, without ever transmitting a `false` predicate. (O6)

Emotional and social:

- **Not overclaim.** The maintainer's standing commitment is that no repository artefact
  asserts compliance or certification, and that a shortfall is declared rather than
  smoothed. The social cost of getting this wrong is a public correction, which is the
  class of project this is trying not to be.
- **Be believed without being trusted.** A reader who has never met the maintainer should
  be able to reproduce the verdict from a clean checkout.

Contextual:

- Procurement for the Estonian wallet is unawarded, RIA's competent-authority role sits in
  a bill still before the Riigikogu, the register has no published API, and the ISO
  second edition has not shipped. Anything built now must degrade gracefully and fail
  closed when the institutional design lands. (P3)

### 2.2 Non-Users (v1)

- **End consumers as a market.** This fork publishes no APK and is not distributed to
  holders; it is built from source by the people the PRD addresses.
- **Anyone seeking certification, or a certification path.** There is no certification
  scheme, `EE-CNF-005` forbids certifying a demo or debug build, and every build this fork
  can produce is debug-derived.
- **A production relying party.** No real issuer exists; issuance is a mock or a CLI.
- **Card-present venues doing a ZK age check.** The proximity ZK path is pending and
  explicitly optional in the fork's spec, and on a ZK-capable device an age request over
  the proximity or redirect path is refused outright today rather than answered plainly.
- **Other ZK schemes, and other Member States.** `longfellow-libzk-v1` is the only system
  in scope for the AV attestation, and the Estonian material in this PRD does not
  generalise to another country's implementation.
- **Wallet providers seeking to bypass the EUDI trust framework.** The product consumes
  trust anchors; it does not create them.

### 2.3 Key User Journeys

Five journeys, each captured from a real surface rather than invented: UJ-1 from the
sandbox site's documented six-step flow (`DESIGN.md`), UJ-2 to UJ-5 from the intent
record's own use cases U1, U2, U4 and U5. The record's U3 (offline/proximity age proof at
a venue) is deliberately *not* a journey here — see §6.2 — because its dependency is
pending and its acceptance evidence does not exist.

`[ASSUMPTION: protagonist names and their one-line context are drafted by the assistant
from the intent record's actor types, not narrated by the user. Every journey's beats,
outcome and edge case are traceable to the record or to `DESIGN.md`; the names are
placeholders to be corrected or replaced.]`

- **UJ-1. Rasmus sees the proof before he believes it.**
  - **Persona + context:** Rasmus, a journalist writing about state digital identity, has
    read claims about zero-knowledge age checks and does not trust claims.
  - **Entry state:** no account, no phone, a desktop browser; arrives cold at the sandbox
    site in Estonian (the toggle offers English).
  - **Path:** picks a labelled test persona (synthetic name and birth date) → registers it
    against the sandbox's Population Register → receives the age attestation → follows the
    step rail to the test shop → the shop asks for `age_over_18` → watches the proof
    timeline fill while the clock counts → reads the ledger of what the shop learned
    against what stayed with him → clicks the sandbox's own "try to cheat" control.
  - **Climax:** the ledger, both columns, each number from the run that just happened —
    and the tamper attempt failing in front of him rather than being described.
  - **Resolution:** he has a reproducer he can cite; every step is repeatable, and the
    page's independence notice tells him whose product this is not.
  - **Edge case:** the prover or verifier is not built on the host, so the guided flow
    cannot run; the site says so and names the documented command instead of showing a
    simulated result.

- **UJ-2. Mari buys wine without handing over her birth date.** *(record U1)*
  - **Persona + context:** Mari, a Tallinn resident who wants the order to go through and
    does not want another birth date copy in another database.
  - **Entry state:** authenticated to an Estonian online service; wallet installed and
    holding an age attestation; TARA-shaped session context established.
  - **Path:** the service asks for `age_over_18` over OpenID4VP with an `mso_mdoc_zk`
    DCQL query → the wallet resolves the held circuit → the consent screen names the
    requester and the channel → she approves → the proof is produced and encrypted to the
    relying party's published key (`direct_post.jwt`) → the verifier checks the transcript,
    the circuit allowlist and the proof.
  - **Climax:** the order completes; the relying party's log holds a boolean and no
    attribute value.
  - **Resolution:** exactly one predicate was disclosed and the wallet labelled the
    presentation as ZK or as a documented fallback.
  - **Edge case:** the relying party asks for a second age predicate, or for a predicate
    the wallet holds `false`; the request is refused or the no-match answer is returned,
    with a stated reason to the user, and no `false` predicate is ever transmitted.

- **UJ-3. Liis accepts a foreign proof her service has never seen.** *(record U2)*
  - **Persona + context:** Liis runs the verification side of an Estonian service that has
    been told to accept other Member States' certified wallets.
  - **Entry state:** her service deployed against the O1/O2 artefacts; no Estonian wallet
    to test with; the accepted circuit set unpublished.
  - **Path:** a foreign AV-profile holder presents over the DC API → the request carries
    the AV attestation with the `ZkSystemSpec` parameter tuple → her verifier resolves the
    circuit against its registry → validates the proof → returns a verdict, encrypted.
  - **Climax:** a foreign proof verifies at her service without a birth date crossing the
    boundary, and the AV-Profile conformance run reports the same result for this fork's
    verifier and for an external implementation.
  - **Resolution:** she can state which clauses she has evidence for and which she does
    not; a non-`longfellow-libzk-v1` system is refused.
  - **Edge case:** the circuit registry is unreachable; the verifier fails closed and says
    which lookup failed instead of allowing the presentation.

- **UJ-4. Tanvi onboards without an isikukood.** *(record U4)*
  - **Persona + context:** Tanvi is an e-resident opening an Estonian service account; she
    has no Estonian personal identification code and does not want her full date of birth
    on file.
  - **Entry state:** holds a PID and an age attestation; the service needs an attribute
    subset plus an age predicate in one session.
  - **Path:** the service asks for both credentials in one session → the wallet answers
    both, with the age predicate proved and the PID attributes selectively disclosed.
  - **Climax:** onboarding completes; the session transcript carries no isikukood anywhere
    and the log names both credential identifiers.
  - **Resolution:** the service holds what it asked for and nothing more; the wallet shows
    what was disclosed and to whom.
  - **Edge case:** one of the two credentials is missing; the session fails with the
    missing credential named, rather than completing partially.

- **UJ-5. Sander gets his first successful verification.** *(record U5)*
  - **Persona + context:** Sander is the integrator at an Estonian service operator; his
    deadline is the end-2026 acceptance window and his target is a service that answers
    "does this work?" honestly.
  - **Entry state:** nothing deployed; no register access; the sandbox's synthetic personas
    are the only test identities.
  - **Path:** deploys the relying-party acceptance kit against the documented commands →
    runs the four cases (a wallet with a PID, a wallet with an AV attestation, an
    unattested ZK request, an unregistered relying party) → sees each refusal name its
    cause → wires the register interface against the shipped mock.
  - **Climax:** a first successful verification, with the time to it measured and reported,
    and a refusal that is distinguishable from the user having cancelled.
  - **Resolution:** he has a proposal for the register interface instead of a guess, and a
    bounded, logged, fail-closed fallback for when the register does not answer.
  - **Edge case:** the mock register is configured absent or the certificate names no
    intended use; the lookup fails closed and the refusal says so.

## 3. Glossary

Downstream workflows and readers use these terms exactly. FRs, UJs and SMs use them
verbatim; introducing a synonym anywhere in this PRD is a discipline violation.

- **Accepted circuit set** — the circuits a relying party may accept a proof over, owned by
  the AV solution's scheme owner. Unpublished as of 23 October 2026 `[VERIFIED]`.
- **AV attestation** — the age attestation of the Commission's Age Verification Profile,
  doctype `eu.europa.ec.av.1`, whose single mandatory attribute is `age_over_18`.
- **Age predicate** — a boolean claim of the form `age_over_NN`. This product discloses at
  most one per presentation and never transmits one held `false`.
- **Carrier** — the wire shape that carries a ZK request and its answer. Two exist here:
  `mso_mdoc_zk` over OpenID4VP (de-facto, profiled in this fork) and the ISO/IEC 18013-7
  Annex C shape over the DC API.
- **Carrier conformance suite** — the runner and published vectors covering the existing
  carrier end-to-end cases (opportunity O5).
- **Circuit** — the compiled proving circuit a proof is made over; identified by a circuit
  hash. A circuit is accepted only when its identifier and parameters match a registry
  entry.
- **Circuit registry** — the verifier-side list of accepted circuits, and its signed
  distribution document. Not to be confused with the *accepted circuit set*.
- **Conformance kit** — the EU AV-Profile conformance runner and its published per-clause
  report (opportunity O1).
- **DC API** — the W3C Digital Credentials API. The Commission's AV Profile carries ZK
  proofs only over this path and over the ISO/IEC 18013-7 Annex C transport.
- **Debug-derived build** — a build produced from the `local_mocks` variant with debug
  code signing. Every APK this fork can produce is debug-derived, so no assessment result
  transfers to it and `EE-CNF-005` applies.
- **EE-PoA** — `ee.riik.poa.1`, the Estonian proof-of-age attestation defined by
  `spec/EE-EUDIW-TS-1.0.md`; a non-qualified EAA carrying no status mechanism.
- **EE-PoA Provider** — the role that issues EE-PoA and computes its predicates
  server-side. Distinct from the wallet, and the binding target of most `EE-POA-*`
  requirements.
- **Evidence label** — `[VERIFIED]` (tool output or quoted primary source), `[ESTIMATE]`
  (a number with a stated method), `[JUDGEMENT]` (analysis over verified facts, source-
  hedged), `[UNVERIFIED]` (not established). Carried through from the intent record.
- **Fail closed** — on absent, unparseable or unreachable input, refuse the operation and
  say which lookup failed, rather than allowing it or substituting a default.
- **Holder** — the natural person operating the wallet, and the subject of the privacy
  guarantees. Not a buyer of this product.
- **Intended use** — what a relying party is registered as entitled to request, read from
  its registration or access certificate. Where it is not readable, the policy falls back
  to refusal.
- **isikukood** — the Estonian personal identification code. It embeds the date of birth,
  which is why an age check answered with it over-discloses.
- **Issuer-chain disclosure** — a proof that carries `msoX5chain` reveals the issuing
  authority to the relying party even though no attribute value crosses. It is a stated
  limit of the privacy position, not an oversight, and it appears with PG-2 and NFR-15.
- **Linkability** — the property that two presentations cannot be tied to one holder.
  Tier 1 (plain, once-only attestations) defeats relying-party linkability but not
  issuer–verifier collusion; Tier 2 adds nothing against a colluding issuer and verifier
  either. The per-path position is stated per path with a test behind it (FR-42).
- **Longfellow** — the proving system mandated by the AV Profile, identified on the wire as
  `longfellow-libzk-v1`. The only ZK system in scope.
- **mso_mdoc_zk** — the de-facto OpenID4VP ZK carrier format identifier, profiled in this
  repository at v1.0. A citable reference, not a standard.
- **PID** — Personal Identification Data, the identity credential; SD-JWT VC or mdoc.
- **Proof of age presentation** — a presentation whose answer is a zero-knowledge proof
  over an age predicate, rather than a disclosed attribute.
- **Relying party (RP)** — the service that asks for data or a predicate and receives the
  answer; also the party that must be registered and attributable.
- **Relying-party acceptance kit** — the deployable RP-side service profile, its four
  acceptance cases and its integrator documentation (opportunity O2).
- **Sandbox site** — the static bilingual site that drives the real issuer, prover and
  verifier for a visitor, without an Android phone.
- **Test shop** — the sandbox site's synthetic relying party, labelled as such.
- **Tier 1** — the once-only, batched, plain attestation posture. Mandatory and deployable
  today.
- **Tier 2** — the zero-knowledge proof posture. Specified in full, gated by `EE-CNF-006`
  and `EE-CNF-007`, and operated in this fork as a pilot below the ACM's recommended
  level.
- **Verifier** — the component that checks a proof: this fork's Go service over the
  Longfellow Rust staticlib.
- **Wallet** — the Android holder application in `app/`.
- **Wallet Unit Attestation (WUA)** — the attestation about the wallet instance, issued by
  the Wallet Provider and never presented at the relying-party interface.
- **WSCD** — the secure cryptographic device holding credential keys. Local native in this
  fork; the remote-HSM path is a mock.
- **ZKP_08** — the ARF requirement pinning a ZK scheme to the ECCG Agreed Cryptographic
  Mechanisms v2.0. This product asserts the conditional reading (a), primitives and
  assumptions, and does not claim the requirement is met.

## 4. Features

Each feature is a coherent capability. FRs are numbered globally and carry their own
testable consequences. FRs marked **(shipped)** describe behaviour the repository already
has and that this PRD protects from regression — for a brownfield product those are
requirements too, and the file names beside them are the code that holds them. FRs marked
**(new)** do not exist yet.

**A consequence that permits "or record it as unmeasured / not obtained / out of scope" is
satisfied only by a record that names the reason, the date, and the evidence that would
close it.** v1 may discharge at most three of them (FR-15, FR-22, FR-24, FR-27 and FR-42 each
contain one, so at least two must be closed); a fourth means the release is being defined
by what it did not do, and that is a decision, not a line item.

### 4.1 The proof-of-age presentation spine

**Description:** the working spine everything else is built on. A holder holding an age
attestation answers an age request from a relying party with a proof instead of an
attribute; a verifier checks that proof and returns a verdict; both sides refuse the
weaker thing. It runs on two carriers — `mso_mdoc_zk` over OpenID4VP and the ISO/IEC
18013-7 Annex C shape over the Digital Credentials API — and it is exercised end to end by
`tests/e2e.sh` (13 assertions, both response modes, one- and two-credential sessions),
which exercises the OpenID4VP side only: the script's own header says the `/present/*`
flow is all it exercises. The DC API carrier is covered by Go unit tests over the same
fixtures (`verifier/go/oid4vp/isodcapi.go`, `iso_test.go`), not end to end `[VERIFIED]`.
Realizes UJ-2, UJ-3, UJ-4.

**Functional Requirements:**

#### FR-1: An age predicate is proved over the OpenID4VP ZK carrier (shipped)

A holder can answer a relying party's `age_over_18` request over OpenID4VP with an
`mso_mdoc_zk` DCQL query by producing a Longfellow proof, without disclosing any
attribute. Realizes UJ-2.

**Consequences (testable):**
- The end-to-end case for the OpenID4VP ZK path passes in `tests/e2e.sh`.
- The response is `direct_post.jwt`, encrypted with direct `ECDH-ES` under P-256 with
  `A128GCM` or `A256GCM`; a non-P-256 request is refused before the consent screen and
  again before any document leaves. `direct_post.jwt` is the default and plain
  `direct_post` requires an explicit flag.
- **A cleartext path exists behind an explicit verifier flag.** `-allow-unencrypted-response`
  (`verifier/README.md`) is a test-only path that the end-to-end suite exercises, and
  `-allow-false-predicate` is likewise a negative-control path. Both are non-default,
  neither may be presented as a supported deployment mode, and an artefact that describes
  the verifier without them is incomplete (see FR-5).
- The presented attestation doctype is `ee.riik.poa.1` or `eu.europa.ec.av.1`, and the
  disclosed payload contains a boolean and no attribute value.

#### FR-2: An age predicate is proved over the DC API carrier (shipped, fixture-verified)

A holder can answer an age request delivered over the W3C Digital Credentials API through
the ISO/IEC 18013-7 Annex C transport. Realizes UJ-3.

**Consequences (testable):**
- The Annex C request is parsed from `navigator.credentials.get` parameters, the proof is
  produced and the response is HPKE-encrypted to the verifier's key, byte-compatible with
  the reference verifier's stdlib HPKE.
- **The claim is stated as fixture-verified only.** No document in the repository states
  the Annex C path has been exercised on a device until a recorded device run exists;
  while that run is missing, the status is `PENDING-DEVICE`.

**Out of Scope:** the on-device session itself (see §6.2); this FR covers the
implementation and its fixtures.

#### FR-3: The verifier validates a proof and returns an encrypted verdict (shipped)

A relying party can send a ZK presentation to the verifier and receive a verdict that
binds the presentation to the session. Realizes UJ-2, UJ-3.

**Consequences (testable):**
- The verifier checks, in order, the session transcript, the circuit allowlist
  (identifier + `circuit_hash` + parameters), the proof, and the doctype, namespace and
  attribute identifier of the query, and rejects on any mismatch with a reason naming the
  failed check.
- A CFB/`false` predicate value, a malformed hex value, a wrong doctype, a wrong namespace
  or an unsupported `zk_system` each cause rejection (the existing `vptoken_test.go`
  9-case table).
- Changing any bound transcript parameter (`client_id`, `nonce`, `response_uri`, the
  `jwkThumbprint`) changes the transcript bytes, which is what prevents replay at another
  verifier.

#### FR-4: At most one age predicate per presentation (shipped)

A holder can complete a presentation only when the request names exactly one age
predicate. Realizes UJ-2.

**Consequences (testable):**
- A request naming more than one `age_over_NN`, required or optional, is refused on the
  OpenID4VP, DC API and proximity paths (`AgePredicatePolicy`).
- A claim-less DCQL query for an age doctype requests `age_over_18` only
  (`DcqlRequestProcessor`).

**Out of Scope:** a lawful two-predicate request. ISO/IEC 18013-5 §7.2.5 permits
requesting several thresholds and selecting the nearest true one above the request; this
product is deliberately stricter until a registered intended use can authorise otherwise
(§6.2).

#### FR-5: A predicate held false is never transmitted (shipped)

A holder whose attestation holds the requested predicate false can never cause that
`false` to reach the relying party. Realizes UJ-2.

**Consequences (testable):**
- The user is told, and the relying party receives the no-match answer — nothing sent over
  OpenID4VP, `RESULT_CANCELED` over the DC API, session terminated over proximity.
- **Two independent defences hold, and the test estate proves both.** In the default
  configuration the prover refuses to construct a proof of a false predicate — the guard is
  a panic and the end-to-end suite asserts that refusal. The verifier separately refuses a
  false predicate *even when a sound proof of one is presented*: `tests/e2e.sh` posts a
  proof of `age_over_18 = false` behind the explicit `--allow-false-predicate` flag and
  asserts the answer is `valid:false` with a message stating it accepts only `0xf5`. The
  flag is a negative control, not a supported path, and the repository's own label for the
  binding it proves is `EE-ZKP-021(b)`.
- **What the claim does and does not cover.** "Never transmitted" is therefore a statement
  about the default configuration, enforced at the prover and independently at the verifier —
  not a claim that a false-predicate proof can never reach a wire. The sandbox site's third
  attack is the same negative control in front of a visitor.
- **The wire-level assertion this FR promised does not exist yet.** Nothing asserts that no
  payload carries a false predicate outside the flag; until it is written, the claim rests on
  the prover's guard and the verifier's refusal, both of which are tested.

#### FR-6: Weaker requests are refused, with the reason shown and the tier labelled (shipped)

A holder on a ZK-capable device can be certain that an age request which cannot be
satisfied by a proof is refused rather than answered plainly, and that the user is told
why. Realizes UJ-2, UJ-3.

**Consequences (testable):**
- A DC API or proximity age request that is unauthenticated (no `readerAuth` chaining to a
  trusted access CA) or carries no satisfiable ZK request is refused before consent
  (`HolderObligations.refusePlainFallback`).
- The last attestation of either age doctype is never consumed: on a ZK-capable device it
  goes over Tier 2 or is refused, and the refusal is written to the activity log as
  linkable.
- Where a presentation is a documented fallback rather than a proof, the wallet labels it
  as such to the user and in its log.
- The user sees a dedicated message for a refusal, distinguishable from a retryable
  network error and from the user having cancelled (see also FR-26).

#### FR-7: Multi-credential sessions are answered per credential (shipped)

A holder can answer a session that requests two credentials — for example a PID attribute
subset plus an age attestation — in one approval. Realizes UJ-4.

**Consequences (testable):**
- Both credential identifiers appear in the session's log; a missing credential fails the
  session with the missing identifier named rather than completing partially.
- The session transcript carries no `isikukood` and no full date of birth in any covered
  scenario.

#### FR-8: The EE-PoA attestation follows its consumption and validity rules, with one declared shortfall (shipped)

The product can issue and consume EE-PoA attestations within the specification's once-only
and validity rules, **with one exception it declares rather than satisfies**: the batch size.
Realizes UJ-2.

**Consequences (testable):**
- Attestations are issued in a batch of three in one transaction; a plain presentation
  consumes its attestation and a ZK presentation consumes none.
- **The batch of three is a shortfall, not a satisfied rule.** The specification requires a
  batch of *at least 30* (`EE-POA-011`, with `EE-POA-011b` allowing a reduction only on
  measured WSCD capacity), and `spec/EE-EUDIW-TS-1.0.md` §23 item 26 already records this
  fork's three as below that floor and not a measured capacity maximum. No artefact may
  describe this product as meeting `EE-POA-011`, and no reader may infer that the
  once-only posture is exercised at the specified scale.
- The last attestation of either age doctype is never returned (`lastAttestation`).
- `signed`, `validFrom` and `validUntil` are floored to a 7-day epoch counted from
  1970-01-01 (`AgeIssuanceConstants.validity`); the predicates of the two age doctypes
  derive from the mock register's date of birth on the Europe/Tallinn day, with a
  29 February birthday treated as 28 February (`AgeReckoning.effectiveBirthDate`).
- Renewal is offered from the day the earliest held `validUntil` falls inside the
  re-issuance window (`AgeAttestationRenewal`); accepting replaces the batch.

**Out of Scope:** the spec's 14-day re-issuance lead as an automatic background issuance;
re-issuance here is user-started (§6.2).

#### FR-9: Credential keys are generated in hardware (shipped)

A holder's credential keys can only be created and used in the device's secure area.
Realizes UJ-2, UJ-4.

**Consequences (testable):**
- Keys are generated through the platform secure area — StrongBox where
  `FEATURE_STRONGBOX_KEYSTORE` is advertised, TEE otherwise — with no API in the codebase
  that returns private-key material.
- No credential this fork issues uses a remote signing path; the remote-HW path is a mock
  and is excluded from any deployment claim.

#### FR-10: The relying party never receives an attribute value in an age presentation (shipped)

A relying party that asked for an age predicate can learn a boolean and nothing else.
Realizes UJ-2, UJ-3.

**Consequences (testable):**
- An age presentation's payload and the relying party's log contain no attribute value
  (the ARF `DASH_03a` expectation).
- The consent screen shows the requester, the requested predicate and the channel before
  the proof is produced.

**Feature-specific NFRs:**
- The specification's own budgets are **not met on this hardware and are declared unmet**.
  `EE-ZKP-040` assigns proof generation ≤ 2.0 s (Tier 2) / 3.5 s and **verification
  ≤ 1.0 s p95**; `EE-ZKP-041` caps prover memory at 250 MB; `EE-ZKP-044` budgets transport
  at ≤ 4.0 s. Host verification measures p95 3.2–4.9 s across rounds on an i5-8365U-class
  host, ~95 % of it Longfellow sumcheck arithmetic. The ≤ 1.0 s figure is therefore
  declared unmet as the *verifier* budget and is not to be restated as a prover budget or
  as a target; the verifier is expressed as a capacity statement instead (see §8 NFR-6 and
  §6.2).
- The refusal cost after the 54–58 s defect fix is pinned by a regression test so it
  cannot drift back silently.

### 4.2 EU AV-Profile conformance kit

**Description:** the first deliverable whose absence is felt outside this repository. A
relying party, a provider or a programme can find out whether an implementation meets the
Commission's Age Verification Profile instead of being told it does. The kit is a
clause-by-clause registry, fixtures minted against the profile's own doctype and
`ZkSystemSpec` tuple, and a runner that produces a per-clause report a third party can
reproduce. Realizes UJ-3, UJ-5.

**Functional Requirements:**

#### FR-11: A normative clause registry exists (new)

A maintainer can look up any AV-Profile normative clause and see the test that
establishes it, or an explicit statement that it is not covered. Realizes UJ-3.

**Consequences (testable):**
- Every normative clause in the profile has either a test identifier or an "out of scope
  for v1" note with a reason; the registry is machine-readable.
- The registry covers at least 20 clauses.

#### FR-12: AV-profile fixtures are minted against the profile's own parameters (new)

An implementer can run the kit against fixtures that use the profile's doctype and its
`ZkSystemSpec` parameter tuple (`circuit_hash`, `version`, `num_attributes`,
`block_enc_hash`, `block_enc_sig`). Realizes UJ-3.

**Consequences (testable):**
- Fixtures carry doctype `eu.europa.ec.av.1` and the profile's tuple, and are
  version-labelled against the profile revision they were built from.
- At least one fixture reproduces a container-format example originating outside this
  repository, and the document names the source revision of each tuple it uses.

#### FR-13: A conformance runner produces a per-clause report (new)

A candidate wallet, issuer or verifier can be run against the kit and receive a per-clause
pass/fail result. Realizes UJ-3, UJ-5.

**Consequences (testable):**
- The runner's output is machine-readable, names each clause and each verdict, and exits
  non-zero when any in-scope clause fails.
- At least 8 of the covered clauses are negative controls that must fail closed.
- **The kit runs against at least two independent implementations and reports a
  clause-by-clause result for each** — the record's own acceptance criterion for O1. One of
  the two may be this fork's verifier or issuer; at least one may not be (CM-1, CM-2).
- A failing clause names the property that failed, never a generic "invalid" (see FR-18).

#### FR-14: The report is reproducible by a third party (new)

A reader who has never met the maintainer can reproduce the published report from a clean
checkout. Realizes UJ-3.

**Consequences (testable):**
- The published report names the exact commit, the profile revision, the host class, and
  the commands; following only those commands reproduces the same per-clause verdicts.
- Every Python input the run needs is pinned (`tests/requirements-ci.txt`, because
  unpinned `cbor2`/`cryptography` shift the golden transcript vectors).
- **The Rust toolchain is not pinned** — `docs/DEVELOPMENT.md` states that nothing in the
  repository pins one — so a reproducible report must name the toolchain, host class and
  commit it used, or the run is not reproducible at all.

#### FR-15: The AV issuance path and its invocation are covered (new)

An attestation provider or wallet can exercise the AV issuance path, including the
`av-vci://` invocation and an AVSUA-equivalent attestation path at issuance. Realizes
UJ-3.

**Consequences (testable):**
- The kit exercises `av-vci://` invocation and records its result pass or fail.
- The AVSUA-equivalent path is either exercised or explicitly recorded as out of scope
  with the reason (the spec's `EE-WUA-008` is currently unmet).

#### FR-16: In-scope and out-of-scope clauses are both published (new)

A reader can tell what the kit does *not* claim. Realizes UJ-3.

**Consequences (testable):**
- The published report lists out-of-scope clauses with reasons, adjacent to the covered
  ones, not in a footnote.
- The report contains no word from the banned claim vocabulary (FR-36).

### 4.3 `mso_mdoc_zk` carrier conformance suite

**Description:** the cheapest credibility signal available, because the work is already
done and only needs running in public: a documented runner over the existing carrier cases,
published vectors, and at least one result against an implementation that is not this one.
Realizes UJ-3.

**Functional Requirements:**

#### FR-17: A carrier runner covers the existing cases and publishes vectors (new)

An implementer can run the carrier cases and compare against published vectors. Realizes
UJ-3.

**Consequences (testable):**
- The runner covers the cases already exercised by `tests/e2e.sh` and publishes its
  vectors under a versioned directory.
- Vectors are regenerated only from the *other* language's implementation, never from the
  implementation under test.

#### FR-18: A failing case names the binding property (new)

An implementer whose carrier fails can learn which property is wrong. Realizes UJ-3.

**Consequences (testable):**
- Every failure message names the binding property (transcript, thumbprint, circuit
  parameters, attribute identifier) rather than returning a generic refusal.
- A deliberately corrupted vector produces that named failure in the suite's own tests.

#### FR-19: One external implementation is exercised and the result published (new)

A reader can see that this carrier interoperates with something that is not this
repository — pass or fail. Realizes UJ-3.

**Consequences (testable):**
- The suite runs against at least one external implementation and the result is published
  whether it passes or fails.
- The published result names the external implementation, its version, and the date.

#### FR-20: Carrier drift is version-managed (new)

An implementer can tell which carrier version a fixture belongs to and what would change
if ISO/IEC 18013-5's second edition lands differently. Realizes UJ-3.

**Consequences (testable):**
- Fixtures are version-labelled against the external document version they were built
  from.
- `verifier/go/oid4vp/carrier.go` remains the single adaptation point for a carrier change,
  and `TranscriptForFlow`/`Flow` remains the single transcript entry point; a documented
  rename/adapt plan exists for the second edition's `ZkRequest`/`ZkDocument` shapes.

### 4.4 Estonian relying-party acceptance kit

**Description:** the only deliverable in this release whose reader already has a dated
obligation and no product. An Estonian service operator and their integrator can deploy an
RP-side profile built from the Go verifier, run the four requests an Estonian service will
actually meet, see refusals that name their cause, and wire a register interface that does
not exist yet through a shipped mock. Realizes UJ-5.

**Functional Requirements:**

#### FR-21: The kit deploys as a documented service profile (new)

An operator can stand up an RP-side verification service from the Go verifier using one
documented install path. Realizes UJ-5.

**Consequences (testable):**
- The kit is a deployment profile over the existing verifier, not a fork of it; installing
  it requires no code change to the verifier.
- A reader with no prior ZK knowledge reaches a first successful verification by following
  the README alone, in under a day, and the measured time is published.

#### FR-22: A TARA-facing adapter contract exists and is exercised (new)

An Estonian service can integrate the wallet flow with TARA as its authentication broker.
Realizes UJ-5.

**Consequences (testable):**
- An adapter contract is published with its interface, its assumptions about TARA's OIDC
  surface, and the points where it is a mock.
- An integration test runs against a TARA test client, or the absence of one is recorded
  as an unmet dependency with the reason.

#### FR-23: The register lookup is pluggable, fail-closed, and mocked (new)

An operator can run the kit with no register available and still get a lawful, explainable
answer. Realizes UJ-5.

**Consequences (testable):**
- The register lookup is an interface with a shipped mock and a documented
  "no entry ⇒ refuse" default; an absent, unparseable or ambiguous entry refuses rather
  than allows.
- A refusal names which lookup failed, and a bounded cache with an explicit stale policy is
  the only permitted optimisation; a stale decision is logged as such, never silent.
- With no network and no list, the kit fails closed.

#### FR-24: The register interface is proposed, not guessed (new)

The maintainer can put a concrete interface proposal in front of the register's owner
before the owner publishes one. Realizes UJ-5.

**Consequences (testable):**
- A written interface proposal exists, with the ARF relying-party topics mapped against it
  and the fail-closed behaviour specified.
- An Estonian service owner has reviewed it and said whether it is usable, or the review
  is recorded as not yet obtained.

#### FR-25: The four acceptance cases are the acceptance test (new)

An operator can prove their service handles the four requests it will actually meet.
Realizes UJ-5.

**Consequences (testable):**
- All four cases have a documented, machine-readable result: a foreign wallet with a PID; a
  foreign wallet with an AV attestation; an unattested ZK request; a request whose relying
  party registration is missing.
- The last two cases refuse, and the missing-registration case names the register as the
  cause.

#### FR-26: A refusal is distinguishable from a cancellation (new)

A user and an operator can tell a refusal from the user having cancelled. Realizes UJ-5.

**Consequences (testable):**
- The UI contract distinguishes the two states, and an end-to-end case exercises each.
- The operator's log records the refusal reason for the relying party's own diagnostics
  without recording an attribute value.

#### FR-27: The integrator's first verification is measured (new)

A programme reader can see how long integration actually took rather than an estimate.
Realizes UJ-5.

**Consequences (testable):**
- Time-to-first-successful-verification is measured on at least three integrators and
  published with the baseline, method and confidence.
- If fewer than three integrators can be found, the row is published as unmeasured with
  the reason, rather than as an estimate.

### 4.5 The sandbox site

**Description:** the product's public face and the only surface a visitor uses without
building anything. It drives the real issuer, prover and verifier for a guided six-step
flow, ends in a ledger contrasting what the test shop learned with what stayed with the
visitor, and names its own shortcuts where they happen. Its visual world is pinned in
`DESIGN.md` (TEDI-derived, ria.ee chrome, "The Service Counter"): that document is the
design authority and this PRD does not restate it. Realizes UJ-1.

**Functional Requirements:**

#### FR-28: Every shown number comes from the run that just happened (shipped)

A visitor can trust that the figures and verdicts on the page are the output of the run
they triggered, not copy. Realizes UJ-1.

**Consequences (testable):**
- The registration, attestation, proof and verdict steps execute against the real issuer,
  prover and verifier; no step returns a canned result.
- Any figure shown (proof size, timings, verdict) is rendered from the server's response
  for that run.

#### FR-29: The ledger shows both columns (shipped)

A visitor can see what the test shop learned and what stayed with them, side by side, at
the moment it happened. Realizes UJ-1.

**Consequences (testable):**
- The ledger renders the disclosed boolean and the withheld attributes as two contrasting
  columns with per-item captions.
- The "kept"/"learned" wording matches the run's actual payload; a change to what is
  disclosed changes the ledger without a copy edit.

#### FR-30: The visitor can try to cheat and watch it fail (shipped)

A visitor can mount the obvious tamper attempt and see the refusal happen in front of
them. Realizes UJ-1.

**Consequences (testable):**
- The site offers at least one deliberate attack (for example a modified age value or a
  replayed proof) and shows the verifier's named refusal.
- The attack's outcome is the verifier's real response, not a scripted animation.

#### FR-31: The sandbox's shortcuts are named where they occur (shipped)

A visitor can tell which parts of the demonstration are shortcuts. Realizes UJ-1.

**Consequences (testable):**
- The synthetic Population Register's client-side age computation is named at the point it
  happens as the trust-the-client enrolment weakness the spec warns about
  (`EE-POA-006`/`EE-POA-007`).
- The synthetic test identities are labelled as synthetic within the flow, not only in the
  footer.

#### FR-32: The site is bilingual, Estonian first (shipped)

An Estonian reader can read every string in Estonian, and an English reader in English.
Realizes UJ-1.

**Consequences (testable):**
- Every string exists in both languages; the toggle is present on every page; Estonian is
  the default.
- A string added in one language without the other fails the site's own check.

#### FR-33: The independence notice is present and the state branding is absent (shipped)

A visitor can never mistake this for RIA's product. Realizes UJ-1.

**Consequences (testable):**
- The independence notice appears in the utility bar, the info alert and the footer.
- No RIA logo and no state coat of arms appears anywhere in the site or its assets, and no
  page claims endorsement.

#### FR-34: A missing toolchain produces an honest failure, not a fake (shipped)

A visitor whose host cannot run the spine can still learn how to make it work. Realizes
UJ-1.

**Consequences (testable):**
- When the prover or verifier is not built, the page says so and names the documented
  command (`make deps`) instead of rendering a simulated result.
- No page shows a proof timeline or a verdict the host did not actually produce.

#### FR-35: The site holds the public-sector accessibility baseline (shipped)

A visitor using a keyboard or a screen reader can complete every step. Realizes UJ-1.

**Consequences (testable):**
- WCAG 2.1 AA as the stated baseline with EN 301 549 as the presumption: keyboard-complete,
  visible focus, contrast held by the pinned tokens in `DESIGN.md`, and no reliance on
  colour alone.
- Long-running steps (the proof) announce progress and completion to assistive technology,
  and reduced-motion is honoured (the proof mark renders as a static state).

### 4.6 Documentation, evidence and claim discipline

**Description:** not a paper exercise — this is the feature that makes the rest of the
product the thing it claims to be. It is also the one that is currently violated: the
conformity note's `issuerAuth` row states that on-device `issuerAuth` validation is absent
while the wallet code performs it, and the research corpus identifies that as a defect to
fix before an evaluator sees it. It serves every journey by making the artefact auditable —
UJ-3 and UJ-5 in particular.

**Functional Requirements:**

#### FR-36: No artefact asserts compliance or certification (new)

A reader can trust that no repository document claims a status no outside body has
conferred. Realizes UJ-3, UJ-5.

**Consequences (testable):**
- A check in `make check` fails on the banned vocabulary ("conformant", "certified",
  "compliant", "meets the profile") in documentation, replaced by "supported by these
  tests, in this environment".
- The conformity note's own words remain true of every artefact it indexes —
  `docs/CONFORMITY.md` states "Nothing here is a certification claim." — and this PRD's
  paraphrase of that commitment may not be quoted as the note's text.

#### FR-37: Every published figure carries its baseline, method and confidence (new)

A reader can tell what a number is worth. Realizes UJ-3, UJ-5.

**Consequences (testable):**
- Every figure in a published artefact states its baseline, method, assumptions and
  confidence; a range is never presented as a commitment.
- An unmeasured row says so explicitly (`PENDING-DEVICE`) rather than being estimated
  silently.

#### FR-38: Conformity statuses are derived from tests, and stale rows are corrected (new)

A reader can rely on a status row being current with the code at the commit it names.
Realizes UJ-3.

**Consequences (testable):**
- The `issuerAuth` row is corrected against the code, and every other hand-maintained row
  is verified against the code before it is edited.
- **A second row is already known to be wrong and is named here so it cannot be missed:**
  the carrier section of the conformity note states that no OpenID4VP ZK carrier exists and
  that the wallet presents plain only over OpenID4VP, which contradicts both its own earlier
  text and FR-1's shipped behaviour. Both rows are corrected in v1.
- No status row is claimed without a test or a quoted primary source behind it, and the
  document names the commit its rows describe.

#### FR-39: The missing external clause maps are published (new)

A programme reader can see where this implementation stands against the instruments this
product's claims touch. Realizes UJ-3.

**Consequences (testable):**
- A clause-by-clause statement against HAIP 1.0 exists.
- A clause map against EC TS13 exists, stating that `mso_mdoc_zk` is supported and
  `zk-jwt` is not.
- A clause map and algorithm inventory against the ECCG Agreed Cryptographic Mechanisms
  v2.0 exists and separates soundness exposure from privacy exposure.

#### FR-40: The scope of the claim is written down (new)

A reader can tell exactly how narrow the product's claim is, and what it deliberately does
not cover. Realizes UJ-3.

**Consequences (testable):**
- A narrow-scope statement exists: a boolean age predicate on one attestation, with the
  general ARF ZKP vision explicitly out of scope.
- An attestation-ordering statement exists: `eu.europa.ec.av.1` preferred for every
  relying party, `ee.riik.poa.1` as fallback, both paths retained.
- Design notes record `ZKP_03` (attestation-to-PID binding), `ZKP_04` (pseudonyms) and
  `ACP_03` as out of scope with reasons.

#### FR-41: The ZKP_08 tension is reported rather than resolved (new)

A programme contact can see the honest position on the requirement this product cannot
meet. Realizes UJ-3.

**Consequences (testable):**
- A written report states that ARF `ZKP_08` pins ZK schemes to the ECCG mechanisms
  catalogue, that the catalogue names no ZK scheme, and that the reading applied here is
  reading (a), conditional, with a re-check trigger on catalogue revision.
- The 109-bit soundness figure of the bundled v7 circuits is declared against the ACM's
  125-bit recommended floor, and no artefact states or implies the recommended level is
  met.

#### FR-42: Unlinkability and authentication are stated with a test behind them (new)

A reader can see a per-path unlinkability position that a test supports, and a level-of-
assurance check beside a proof. Realizes UJ-3.

**Consequences (testable):**
- A per-path unlinkability statement exists, backed by a test asserting that no tracking
  identifier is added to a presentation; and the conformity note's **reasoned refusal** to
  claim `ZKP_07` (`docs/CONFORMITY.md`) is carried as the statement, not replaced by a
  partial claim or quietly upgraded. Where the property cannot be claimed, it is recorded as
  not claimed.
- A LoA-High authentication test runs alongside a ZKP presentation (`ZKP_09`), or its
  absence is recorded with the reason.

#### FR-43: Standards engagement is recorded, including what has not been filed (new)

A reader can see the state of this product's standards engagement without being told it is
further along than it is. Realizes UJ-3.

**Consequences (testable):**
- The prepared OpenID DCHP #17 submission text exists in the repository and its status is
  stated as prepared and not filed, with the filing decision recorded as user-gated.
- OpenID4VP #627 is tracked with its current state and date.
- **Every deliberate protocol deviation is listed with its rationale** in one place, not
  scattered: today that includes the verifier's `ECDH-ES+A256KW` divergence and the
  round-3 encoding deviations recorded in the addendum. A reader comparing this product to
  a specification must be able to find them, and a deviation discovered but not listed is a
  defect of the same class as a stale status row (FR-38).

#### FR-44: The maintenance exposure of a dormant upstream is written down (new)

A reader assessing sustainability can see that this fork carries the maintenance. Realizes
UJ-3.

**Consequences (testable):**
- A written position covers vendoring, upgrade cadence for pinned pre-1.0 dependencies and
  the fork's succession.
- A licence and vendored-artefact inventory exists (the Maven mirror, vendored `coseJava`,
  pinned pre-1.0 dependencies).
- The repository is verified to be a strict superset of upstream (zero commits behind) at
  the commit the position names.

### 4.7 Consent integrity and user-presence gates

**Description:** the wallet-side findings the product's own audit raised on the paths the
product *advertises*: a name on the consent screen that has not been authenticated, a
consent gate that can be guessed, and device keys that can be used without the user present.
They are in v1 because §9.2 says the consent screen's claims must be true and that key use
requires user presence, and today neither holds. Realizes UJ-2, UJ-4. These four are the
release's only requirements that exist to close findings rather than to add capability —
the audit's remaining items (supply-chain pinning, main-thread blocking, debug logging, the
mock trust surfaces, the release path) are a hygiene release, not this one.

**Functional Requirements:**

#### FR-45: A requester name shown at consent is authenticated (new)

A holder can trust that the relying party named on the consent screen is the relying party
that signed the request. Realizes UJ-2.

**Consequences (testable):**
- The `readerAuth` chain is validated to a trust anchor before any requester name is
  rendered; no unvalidated CN reaches the screen as a verified name.
- When the chain does not validate, the screen says the requester is unverified and the
  default is refusal, with the reason visible (FR-6).
- A negative test presents a certificate carrying an attacker-chosen CN and asserts it is
  not rendered as verified.

#### FR-46: Credential keys require user presence (new)

A holder's credential keys can be used only after the user authenticates, with the
platform's user verification bound to the key operation itself. Realizes UJ-2, UJ-4.

**Consequences (testable):**
- Key use is gated by a platform user-authentication prompt whose result the key operation
  depends on, not by an application-level flag alone.
- Proof generation, plain presentation and signing each carry the gate, or the ungated path
  is removed rather than left reachable.
- A test asserts that a key operation attempted without user authentication fails.

#### FR-47: The consent gate resists guessing (new)

A holder's consent gate cannot be brute-forced. Realizes UJ-2.

**Consequences (testable):**
- Failed attempts are counted in persistent state that survives process restart, with a
  lockout threshold and an escalation path stated in the artefact that publishes it.
- Relaunching the application does not reset the count; the existing behaviour, where the
  counter resets on relaunch and a "remote check" is a simulated delay, is removed rather
  than documented.

#### FR-48: A proximity share carries the same consent as a remote one (new)

A holder presenting in proximity consents explicitly before anything is emitted. Realizes
UJ-2.

**Consequences (testable):**
- The proximity share passes the consent gate before a document is emitted; today it emits
  with no gate at all.
- The screen states what is shared and with whom on the same terms as the remote path.
- A test asserts the gate is present on the proximity path.

## 5. Non-Goals (Explicit)

Each of these is a boundary the intent record itself states, with its own label preserved.

### 5.1 Claim boundaries

- **NG-1: No broader novelty claim.** "Nobody anywhere has done this" is not supported by
  the evidence and is not made. Novelty is scoped to this fork, upstream, ten comparators
  and the EU artefacts fetched on 23 October 2026 `[JUDGEMENT / scope limit]`. A claim that
  no other implementation exists is likewise `[UNVERIFIED]`, because comparator README
  silence is not absence.
- **NG-2: No compliance, conformity or certification claim.** No artefact states or implies
  that this product is conformant, compliant, certified or assessed. Status is conferred by
  an assessment body, not by a repository (see FR-36).
- **NG-3: Not a certification service.** The conformance kit produces evidence for someone
  else's assessment; it is not an accredited test laboratory and its report is not a
  certificate.
- **NG-4: Not legal advice.** The regulatory material in this PRD and in the record is
  technical assessment. The Ministry's bill sits before the Riigikogu, `ZKP_08`'s legal
  question is open, and nothing here is a legal opinion.
- **NG-5: No claim on the ACM's recommended level.** The bundled circuits are 109-bit
  against the Agreed Cryptographic Mechanisms' 125-bit recommended floor for recommended
  mechanisms. The gap is declared, not argued away.

### 5.2 Capability boundaries

- **NG-6: Proximity Tier-2 ZK is out of scope.** The wallet has a proximity presentation
  path, but there is no proximity path for a Tier-2 ZK proof, and the transfer budget for a
  ~360 KB proof over BLE is unmeasured `[VERIFIED as absent]`.
- **NG-7: No ZK system other than `longfellow-libzk-v1`.** The AV Profile states that
  support for any other ZK system does not constitute conformance; a non-matching
  `zk_system` is refused rather than mapped.
- **NG-8: No general-purpose ZK capability.** One boolean predicate, on one age
  attestation. The broader ARF ZKP vision — attribute-range proofs, predicate suites,
  pseudonyms bound to proofs — is out of scope, and `ZKP_03`, `ZKP_04` and `ACP_03` are
  recorded as out of scope rather than partially implemented.
- **NG-9: No lawful two-predicate presentation.** Where ISO/IEC 18013-5 §7.2.5 would permit
  requesting several thresholds and answering the nearest true one above the request, this
  product refuses until a registered intended use can authorise otherwise.
- **NG-10: No multi-threshold library.** No age gate sits at 16 in the material this
  product relies on, and the 21 threshold is an *identification* duty rather than a gate
  (`EE-POA-004`, `EE-POA-005a`), including the gambling carve-out the specification
  describes; the specification does not carry those thresholds as predicates. Any threshold
  beyond 18 is therefore gated behind the circuit registry and scheme-owner acceptance, and
  an unmapped threshold is treated as unsupported rather than approximated. This PRD does
  not assert that Estonian law sets gates at 16 or 21.
- **NG-11: No status or revocation mechanism for the age attestation.** The EE-PoA is a
  non-qualified EAA carrying no status mechanism, and the AV attestation carries no status
  reference either (`docs/CONFORMITY.md`). Status lists are minted into the upstream mock
  PID and mDL credentials, and nothing in this product fetches or validates a status list
  end to end. This is a known gap carried at §6.2, not a feature, and no reader may infer
  that a revocation posture exists for the age path.
- **NG-12: No real issuer, and no production issuance.** Issuance is a mock or a CLI; no
  state PID provider, no real Population Register, no qualified attestation.

### 5.3 Institutional boundaries

- **NG-13: Not RIA's product and not a state wallet.** No endorsement, no RIA logo, no
  coat of arms, no Firebase project; the independence notice is present on every surface.
- **NG-14: Not a register, an intermediary or a trust anchor.** The product consumes trust
  lists and lookups; it does not operate them. RIA is named as the competent authority in
  a bill, and the register's technical design is not this product's to decide.
- **NG-15: No X-Road integration.** No documented relationship between X-Road or X-tee and
  EUDI presentation exists, and any belief that X-Road transports `vp_token`s is
  unsupported. TARA is the named integration point.
- **NG-16: No wallet-provider certification path.** `CIR (EU) 2024/2981` and the FCAF
  functional-conformance route are not available to this fork, and `EE-CNF-005` forbids
  certifying a demo or debug build — which is the only kind of build it can produce.
- **NG-17: No published interop run against the Commission's reference wallet.** As of
  23 October 2026 no such run exists in this repository or in the comparators examined
  `[VERIFIED as absent]`. The carrier suite is what would produce one (FR-19).
- **NG-18: No standards submission made on the reader's behalf.** The OpenID DCHP #17 text
  is prepared and unfiled, and filing is user-gated; no document may describe it as
  submitted.
- **NG-19: No commercial commitment.** This PRD fixes no price, no SLA, no support
  obligation and no delivery date. It is a product intent document for a public,
  grant-adjacent artefact.

## 6. MVP Scope

The version the user selected: the innovation assessment's initial release — the
conformance kit, the carrier suite and the relying-party acceptance kit — with the two
discovery items those three depend on folded in.

### 6.1 In Scope for v1

| Item | What ships | Record's phase |
|---|---|---|
| AV-Profile clause registry and gap analysis | a machine-readable clause map with a test id or an out-of-scope note per clause | D1 |
| RP-register interface proposal | a written interface, mocks, a fail-closed default, ARF topic mapping | D3 |
| EU AV-Profile conformance kit | FR-11 … FR-16: fixtures, runner, per-clause report, external reproduction | M1 / O1 |
| `mso_mdoc_zk` carrier conformance suite | FR-17 … FR-20: runner over the existing cases, published vectors, one external implementation, version-managed drift | M2 / O5 |
| Estonian relying-party acceptance kit | FR-21 … FR-27: deployment profile, TARA adapter contract, mocked register, four acceptance cases, measured time-to-first-verification | P1 / O2 |
| The presentation spine | FR-1 … FR-10: preserved and regression-protected, not extended | existing |
| The sandbox site | FR-28 … FR-35: preserved as the public face | existing |
| Claim and evidence discipline | FR-36 … FR-44: the banned-vocabulary check, corrected status rows, the published clause maps, the `ZKP_08` report | R3 + record §6 |
| Consent integrity and user-presence gates | FR-45 … FR-48: an authenticated requester name at consent, a user-presence gate on credential keys, a consent gate that resists guessing, and consent on the proximity path | maintainer decision, 2026-10-23 (OQ-8) |

`[ASSUMPTION: the sandbox site and the spine are in v1 as shipped behaviour rather than as
new work. Both already exist and both are what the release's evidence is demonstrated on,
so excluding them would make the release's claims untestable. Confirm if you would rather
v1 be a forward-only release with no regression-protection obligations.]`

### 6.2 Out of Scope for v1

- **Device measurement programme (D2, hardware-gated).** Consequence, stated rather than
  implied: **v1 makes no performance claim, and every device row stays `PENDING-DEVICE`.**
  No usability figure, no BLE transfer time, no StrongBox key-generation time, no proving
  budget is asserted.
- **Verification-budget hardening (M3).** Consequence: v1 states verification as a
  *capacity* figure on a named host class, or states nothing. The `p95 ≤ 1.0 s` budget is
  declared unmet and is not repeated as a target.
- **Registration-certificate-aware request policy (O3, phase P2).** The two-predicate
  request stays refused (NG-9) until this lands.
- **Circuit-hash registry service (O4, phase P3).** The verifier's static allowlist stays;
  no distributed signed list in v1.
- **Statutory age-threshold toolkit (O6, phase R2, externally gated).**
- **Production readiness for a pilot relying party (R1).** No operations runbook,
  monitoring, incident process or availability target in v1; the absence is stated so no
  reader assumes a service they can depend on.
- **On-device exercise of the ISO/IEC 18013-7 Annex C path.** Fixture-verified only
  (FR-2); the device run belongs to D2.
- **Automatic background re-issuance** of the age attestation; re-issuance is user-started.
- **Status lists, revocation and the Trusted List of Lists hardening** beyond what exists.
- **Proximity Tier-2 ZK**, per NG-6.
- **Filing the DCHP #17 submission**, per NG-18.

### 6.3 Deferred, Sequenced After v1

In the record's own order, and deliberately undated except for the external deadlines
(end-2026 public-sector acceptance, end-2027 regulated private sector, 2028 national
launch). A dated roadmap would be an `[ASSUMPTION]` wearing a schedule's clothes
`[JUDGEMENT]`.

1. **D2** device measurement programme, then **M3** verification-budget hardening — in that
   order, because a capacity figure without a device figure would be published on a host
   that is not the target.
2. **Replacing the mocked register** with the real interface once RIA publishes one; the
   first external dependency to become real replaces a mock, not a design.
3. **O3** registration-certificate-aware policy, which is what makes NG-9 revocable.
4. **O4** circuit registry, which is a prerequisite for O6.
5. **O6**, gated on O4 and on a scheme owner accepting the circuits.
6. **R1** production readiness for a pilot relying party — the largest phase and mostly
   non-code.

`[ASSUMPTION: the risk register's rows carry no owners because the intent record assigns
none (its appendix A4 has no owner column). §13 therefore names a revisit condition per
row instead of an owner, and ownership is an open question in §18.]`

## 7. Success Metrics

Every metric below is stated so that a third party can check it, and each names the
counter-metric that would catch it being gamed.

### 7.1 Primary Metrics

- **SM-1 — An AV-Profile conformance report exists and someone else reproduces it.**
  Target: a published report covering ≥ 20 clauses with ≥ 8 fail-closed negative controls,
  reproduced from a clean checkout by a person with no prior contact with this project.
  Baseline today: no such report exists. Realizes FR-11 … FR-16.
- **SM-2 — The carrier suite has been run against an implementation that is not this one,
  and the result is published pass or fail.** Target: one external implementation, named,
  versioned and dated in the published result. Baseline today: `tests/vectors/carrier-v1/`
  exists and no external run does. Realizes FR-19.
- **SM-3 — An Estonian service owner completes the four acceptance cases and states
  whether the register interface proposal is usable.** Target: one service owner, one
  written statement, published whether favourable or not. Realizes FR-24, FR-25.
  **Status at v1: unachieved, and recorded as such.** No service owner is engaged, so the
  kit ships with acceptance evidence produced by the maintainer — which CM-2 says does not
  satisfy this metric. Revisit when a service owner runs the four cases.
- **SM-4 — Time-to-first-successful-verification is measured, not estimated.** Target: a
  distribution over ≥ 3 integrators with the stopwatch method stated. Realizes FR-27.
  **Status at v1: unmeasured, and published as unmeasured.** Integrator count at v1 is
  zero; the row states that rather than estimating. Revisit with the first integrator.
- **SM-5 — No artefact asserts compliance or certification, and the stale status rows are
  corrected.** Target: the banned-vocabulary check green in `make check`, and the
  `issuerAuth` row and every other hand-maintained row verified against the code. Realizes
  FR-36, FR-38.

### 7.2 Secondary Metrics and Guardrails

- **SM-6 — Nothing that was green is no longer green.** `make check` exits 0;
  `:app:testLocal_mocksUnitTest` stays at 169 `@Test` over 26 files with zero failures;
  `:zk-conformance:test` is green and its committed fixtures stay byte-identical unless
  `-Dzk.regenerate=true` was used deliberately; `go vet ./...` is clean; `tests/e2e.sh`
  reports its 13 cases as they stand or more.
- **SM-7 — Every new document is indexed.** Any document the release introduces appears in
  `docs/README.md`, and any new test suite appears in `tests/README.md` with what it
  proves.
- **SM-8 — The audit's stale findings are re-dated against HEAD, and the one live gap is
  closed.** The published audit (`docs/sota-audit-ROLLUP.md`, taken at commit `732d57e`)
  claims the PKIX trust gate `CertificateChainValidator` has zero direct unit tests and that
  `:zk-conformance` runs in no CI workflow. **Neither is true at this PRD's commit
  (`2d38feb`)**: `app/src/test/kotlin/ee/cyber/wallet/security/CertificateChainValidatorTest.kt`
  carries 14 `@Test` cases against the real validator, and `.github/workflows/app.yml` runs
  `./gradlew :zk-conformance:test`. Both landed on 2026-09-25, after the audit's commit. The
  third claim — that the SD-JWT test asserts nothing — is still true. This is the defect
  class FR-38 and NFR-16 exist to fix, and it applies to the audit's own rows as much as to
  the conformity note's: no artefact may repeat a stale finding.
- **SM-9 — The four consent-integrity gates are closed, each with a test behind it.** The
  consent screen names no unauthenticated requester; credential keys require user presence;
  the consent gate resists guessing across a restart; and a proximity share carries consent.
  Target: four negative tests, one per gate, each failing if the gate is removed. Realizes
  FR-45 … FR-48.

### 7.3 Counter-Metrics

- **CM-1 — Counter to SM-1: clause count is not the metric.** A registry that grows by
  adding clauses with no negative control behind them is worse than a small one. If the
  covered-clause count rises while the negative-control count does not, the release has
  regressed in the thing that mattered.
- **CM-2 — Counter to SM-1: "external reproduction" must mean external.** A reproduction
  performed by anyone involved in building the artefact does not satisfy SM-1.
- **CM-3 — Counter to SM-3 and SM-4: integration speed must not be bought by dropping
  refusals.** The four acceptance cases include two refusals; a kit that reaches first
  success faster by not exercising them has failed.
- **CM-4 — Counter to SM-4: the integrator count must not be met with a weaker kit.** A
  kit that only works against its own mock register is not evidence of integration.
- **CM-5 — Counter to SM-5: the banned-vocabulary check must not be satisfied by going
  vague.** Deleting a "conformant" claim is only correct if the sentence still says what
  the tests support; a claim replaced by silence fails this counter-metric.
- **CM-6 — Counter to every metric here: honesty must not cost more than it buys.** The
  product's differentiator is honestly labelled evidence; an artefact that is
  scrupulously honest about trivia while leaving the load-bearing figures unlabelled has
  missed the point.
- **CM-7 — Counter to SM-9: consent integrity must not be bought with breakage.** Closing
  the four gates must not turn a legitimate presentation into an unattributable failure:
  every new refusal names its cause (NFR-14), and the negative tests assert refusal
  *reasons*, not merely refusal.

### 7.4 Quantified Effects and Their Confidence

Carried from the record with its labels intact. These are the *only* effects figures this
PRD permits; the €21.65 M procurement value is sponsored-press only and no market sizing
may be derived from it (NG-1 applies).

- **Q1 — Development-effort saving from reusing the verifier, carrier and fixtures:
  ~6–12 engineer-months** `[ESTIMATE]`, confidence **low–medium**. Method: 11,440 net lines
  ÷ 1,200 net lines per engineer-month for reviewed, test-bearing security code. The
  divisor is a rule of thumb, and the same work was produced here in ~3.5 calendar weeks
  with AI assistance, so the figure describes human re-creation cost, not this project's
  elapsed cost.
- **Q2 — Operating cost of verification: ~0.42 verifications/s per 2 vCPU instance, i.e.
  ~€0.9k–2.1k/year at a 10 % duty cycle against a 10 verifications/s peak** `[ESTIMATE]`,
  confidence **low**. The peak rate is illustrative and the price assumption moves. The
  defensible action is B2, not this number.
- **Q3 — Bandwidth: a proof is ~353–360 KB, so ~€18–43 per million presentations at
  €0.05–0.12/GB** `[ESTIMATE]`; confidence **medium** on the size (measured) and **low** on
  the price. The size is a product constraint for proximity long before it is a cost
  constraint.
- **Q4 — Usability: no defensible number exists.** Every required measurement is
  `PENDING-DEVICE` because hardware was unavailable on 25 September 2026. The harness
  (`ZkProofBenchmark.kt`) is committed and JVM-green; only the prover wiring waits. A
  usability claim today would be inventing a result.
- **Q5 — Security benefits: properties, not percentages.** On-device `issuerAuth`
  verification, LOTL fail-closed trust, CRL soft-fail with a documented flip,
  hardware-backed keys, single-use sessions (replay returns `410`), a fail-closed circuit
  allowlist and pinned JWE profiles are all `[VERIFIED]` as behaviours. No cost-avoidance
  figure is claimed `[JUDGEMENT]`.

**B1–B5 are what would replace Q2–Q4 with measurements:** on-device proving percentiles and
peak memory on the named device set (B1); verification capacity on the scheme's target
instance type (B2); end-to-end BLE transfer of a ~360 KB proof inside a stated budget (B3);
time-to-first-successful-verification over ≥ 3 integrators (B4); the register lookup's
latency and availability impact once the interface exists (B5). B4 is inside v1; B1, B2, B3
and B5 are not (§6.2), so their rows stay unmeasured and are published as such.

## 8. Non-Functional Requirements

Cross-cutting and binding on every feature.

- **NFR-1 — No status claim.** No artefact states or implies compliance, conformity or
  certification. Assessment is conferred by a body, not by this repository. A check fails
  `make check` on the banned vocabulary.
- **NFR-2 — Every figure is labelled.** Every published number states its baseline, method,
  assumptions and confidence; a range is never presented as a commitment; an unmeasured row
  says `PENDING-DEVICE` rather than being estimated silently.
- **NFR-3 — Fail closed, always.** Every lookup and policy path — register, certificate,
  circuit list, trust anchor — refuses on absent, unparseable or ambiguous input and names
  the failed lookup. Silent fallback is prohibited; where a fallback is legitimate it is
  labelled to the user and written to the log.
- **NFR-4 — Version-labelled fixtures.** Every fixture and rule set names the external
  document version it was built from, and nothing is regenerated from the implementation
  under test.
- **NFR-5 — No regression.** `make check` exits 0; `:app:testLocal_mocksUnitTest` (the task
  CI runs) stays at 169 `@Test` over 26 test files with zero failures; `:zk-conformance:test`
  is green; `go vet ./...` is clean; the end-to-end suite passes.
- **NFR-6 — Verification is stated as capacity, not latency.** No document repeats the
  spec's `p95 ≤ 1.0 s` verification budget as a target. Verification is reported as
  verifications/second on a named instance type, with percentiles and the measurement
  method, or it is reported as unmeasured. The two published figures come from different
  host classes and must not be compared: 2,414 ms per valid proof on a 2 vCPU instance, and
  mean 4.02 s / p50 3.83 s / p95 4.91 s on an Intel i5-8365U.
- **NFR-7 — Proving figures carry their host class.** The only proving figures publishable
  today are the host ones (~6.1 s, proof ~353–360 KB); no device-side proving or transfer
  figure exists and none is asserted.
- **NFR-8 — Third-party reproducibility.** A reader with the documented prerequisites
  reproduces every published result from a clean checkout using only documented commands.
- **NFR-9 — The build stays secretless and nothing from `demo-data/` is committed.** The
  holder's device private key never enters the repository; no credential this fork issues
  is committed.
- **NFR-10 — Strict superset of upstream.** Upstream's Android unit suite passes unchanged;
  the fork adds behaviour and removes none.
- **NFR-11 — Dependency pinning.** Pre-1.0 dependencies are pinned by coordinate with a
  target commit hash where the build system allows it; Python inputs stay pinned in
  `tests/requirements-ci.txt`, because unpinned `cbor2` or `cryptography` shift the golden
  transcript vectors.
- **NFR-12 — Accessibility.** WCAG 2.1 AA is the stated baseline, and for the wallet the
  specification makes Directive (EU) 2019/882 and **EN 301 549 v3.2.1** a `SHALL`
  (`EE-ACC-001`–`EE-ACC-004`) — that is the binding instrument, and the sandbox site is
  held to the same baseline. Keyboard-complete, visible focus, contrast held by the pinned
  tokens, no colour-only signalling, announcements for long-running steps, reduced-motion
  honoured. No EN 301 549 audit evidence exists yet (R13).
- **NFR-13 — Language coverage, stated per surface.** The wallet must offer Estonian,
  Russian and English (`EE-ACC-004`); the sandbox site is bilingual Estonian-first with an
  English toggle (`DESIGN.md` is its authority). The gap between the two is recorded rather
  than smoothed: no string exists in one language only *on the surface it belongs to*.
- **NFR-14 — Honest failure surfaces.** A refusal names its cause and is distinguishable
  from a retryable error and from user cancellation, in the UI, in the log and in the
  acceptance cases.
- **NFR-15 — The narrow claim is stated wherever the product is described.** One boolean
  predicate, one attestation, two carriers, and the limits (`ZKP_08`, 109-bit circuits, no
  device measurements) travel with the claim.
- **NFR-16 — Findings are published, not implied to be fixed.** The repository's own audit
  (`docs/sota-audit-ROLLUP.md`: 124 findings, none critical, including consent-gating,
  key-user-presence and verifier-authentication items) is referenced by severity count, and
  refreshed at each release. No artefact may imply a published finding is fixed unless a
  test or a tool run establishes it. The state of each high finding is an explicit row.
- **NFR-17 — No silent scope widening.** A capability that would extend the claim — a new
  predicate, a new ZK system, a new carrier, a new attestation — is a PRD change, not an
  implementation detail.

## 9. Constraints and Guardrails

### 9.1 Privacy Guardrails

These are the product's reason to exist and they are not negotiable by a feature request.

- **PG-1 — The isikukood never crosses the boundary.** The Estonian personal
  identification code embeds the date of birth, which is why an age check answered with it
  over-discloses by construction. A relying party performing only an age check must not
  receive it, nor `birth_date`, `sex`, `portrait` or any other PID attribute a plain
  presentation would carry.
- **PG-2 — One predicate, one boolean.** At most one age predicate per presentation, and
  the disclosed payload is a boolean and nothing else.
- **PG-3 — No `false` predicate is ever transmitted.** A held-false predicate produces the
  no-match answer, never a transmitted `false`.
- **PG-4 — No silent downgrade.** Where a presentation is a documented fallback rather than
  a proof, the user is told and the log records it. A refusal on a ZK-capable device is
  preferred to an unlabelled plain answer.
- **PG-5 — No central age log, with the specification's own carve-out stated.** There is no
  central record of age presentations, *other than* the operator's minimal, aggregated,
  non-identifying service telemetry, which must not include relying-party identity at
  per-event granularity (`EE-PRI-023`). The wallet's own activity log is local,
  distinguishes linkable from new-visit presentations, and is the user's. The relying
  party's log carries no attribute value. The Art. 5a(4)(d) dashboard (`EE-PRI-014`) and the
  erasure and data-protection-report duties are **out of scope for v1** (§6.2) rather than
  silently dropped; `EE-PRI` is the largest requirement family the v1 scope does not touch.
- **PG-6 — Fail-closed is a privacy control, not an availability nuisance.** A register a
  relying party cannot read produces a refusal; a missing circuit produces a refusal; an
  unreachable trust list produces a refusal. Uptime never wins over refusal.
- **PG-7 — The register sits on the critical path and that is a known cost.** It must be
  designed interface-first with mocks, a bounded cache and an explicit, logged stale
  decision. A silent allow on lookup failure is prohibited (R9).

### 9.2 Security Guardrails

This product publishes its own audit, therefore its audit is a product constraint.

- **SG-1 — Published findings are published, not implied fixed, and their age is stated.**
  The audit of record (`docs/sota-audit-ROLLUP.md`, 124 findings, none critical) was taken
  at commit `732d57e`; the PRD describes `2d38feb`. Its live TIER 1 items are: a four-digit
  PIN with no lockout gating all consent, no user-presence gate on device keys (no
  biometric prompt anywhere), a proximity share with no PIN step, `readerAuth` chains never
  validated against a trust anchor (so an attacker-chosen CN reaches the consent screen),
  unsigned OpenID4VP authorization requests with a bare `client_id` claim (`EE-PRO-004`
  governs the unsigned request, `EE-PRO-003` the client-id prefix; `EE-PRO-010a`, the
  cross-device DC API requirement, is a separate unimplemented item),
  `runBlocking` on main-thread signing paths, and a mutable COSE-signing snapshot
  dependency. **Two audit rows are stale and were closed on 2026-09-25** — the
  `CertificateChainValidator` unit-test gap and the `:zk-conformance` CI gap (SM-8). A
  finding's severity is a claim about a commit, and any artefact quoting this audit states
  which commit it was taken at.
- **SG-2 — The consent screen's claims must be true.** A requester name shown to the user
  is a claim about authentication. `readerAuth` chains are validated against a trust anchor
  before a name is rendered, and an unvalidated requester is shown as unverified with
  refusal as the default (FR-45). **Decided 2026-10-23: this is in v1** (OQ-8 resolved).
- **SG-3 — Keys require user presence and the consent gate resists guessing.** Device-key
  use is gated by platform user verification, and the consent gate counts failed attempts in
  state that survives a restart (FR-46, FR-47). **Decided 2026-10-23: both are in v1.**
- **SG-4 — No artefact implies a finding is closed without a test or a tool run.**
- **SG-5 — The supply chain is pinned.** Pre-1.0 and snapshot dependencies carry their
  coordinate and target commit; the Maven mirror and vendored `coseJava` are inventoried
  with their upstream licence and revision (R11, R14).
- **SG-6 — `demo-data/` never enters the repository.** It holds the holder's device
  private key.

### 9.3 Cost and Effort Guardrails

- **CG-1 — Every effort figure carries its method.** The record's ranges are one-person,
  agent-assisted baselines; Q1 explains why the same work appeared in ~3.5 calendar weeks
  here. A receiving team must expect larger calendar effort, and no range is a commitment
  (R15).
- **CG-2 — No market sizing.** The €21.65 M procurement value is sponsored-press only; no
  market size, TAM or ROI figure may be derived from it or from a single ID-card count.
- **CG-3 — Infrastructure cost is not a v1 commitment.** The only cost figures are the
  low-confidence estimates in §7.4, and the defensible action is to measure (B2), not to
  quote them.

### 9.4 Technical Constraints

- The Android app targets the `local_mocks` variant for device install; the `debug`
  variant points at placeholder `*.example.com` backends and fails onboarding after PIN
  entry. Every build this fork can produce is debug-derived, which is why `EE-CNF-005`
  applies to all of them (NG-16).
- The Go packages that link the Rust staticlib fail at the linker until `make deps` has
  run, and `make deps` places `google/longfellow-zk` as the sibling `../longfellow-zk`.
- The prover must be built with `RUSTFLAGS="-C target-cpu=x86-64-v3"`, because Longfellow's
  `target-cpu=native` produces SIGILL binaries on virtualised hosts.
- `cargo build --examples` exits 0 with no binary when `examples/` is missing or empty, so
  the prover must be `test -x`'d after any build.
- The sandbox site is static HTML/CSS/JS served by a Python standard-library server with no
  framework and no build step; the wallet is Kotlin with Jetpack Compose on JDK 17.
- Nothing in `demo-data/` is ever committed.
- The Android unit-test task CI runs is `:app:testLocal_mocksUnitTest`, not `test`.

## 10. Compliance and Regulatory

This section states the regulatory surface the product's claims touch, and the posture
toward it. **The posture is that the product makes no compliance claim** (NG-2), and the
purpose of listing the instruments is to let a reader see which of them the evidence
addresses.

**Instruments named by the intent record:**

- **Regulation (EU) 2024/1183** amending Regulation (EU) 910/2014 — the eIDAS amendment
  that creates the wallet obligation. Article-level text could not be retrieved during the
  record's research (EUR-Lex returned HTTP 500), so this PRD cites no article.
- **Implementing regulations and their amendments:** CIR 2024/2979, 2980, 2981;
  2025/846, 847, 848 (as amended by 2026/1730); 2025/1569 (as amended by 2026/1735);
  2160; 2026/798; and 2026/1731, reported but not opened `[UNVERIFIED]`. CIR 2024/2981
  carries the certification scheme and the trusted-list obligations; CIR 2015/1502 the
  assurance levels.
- **The EU Digital Identity Wallet Architecture and Reference Framework (ARF)**
  v3.0.0, released 23 July 2026 — the baseline the specification is pinned to, whose
  `ZKP_01`–`ZKP_09` requirements are the profile's source and whose Topic 19 defines
  presentation-log fields.
- **The Age Verification Profile** (`eu.europa.ec.av.1`) and its OSPSA — the instrument
  that mandates Longfellow and the single `age_over_18` attribute.
- **Commission technical specifications:** TS3 v1.5.2 (WUA), TS4 v1.0.1 (ZKP
  implementation), TS5/TS6 (relying-party register), TS10 (portability), TS13 v1.0.1 and
  TS14 v1.0 (the two ZKP tracks — both explicitly exploratory, handed toward ETSI
  TS 119 476-2). **TS4's own maturity position travels with any citation of it: it states
  that no existing ZKP approach can be deemed fully suitable or mature enough for direct
  integration.** A product that cites TS4 as an authority while omitting that sentence is
  misquoting it.
- **Directive (EU) 2019/882** (the European Accessibility Act) and **EN 301 549 v3.2.1**
  — the binding accessibility instrument for the wallet per `EE-ACC-001`, which the PRD
  carries at NFR-12. Directive (EU) 2018/1972 and Directive 2016/2102 are outside this
  product's surface.
- **HAIP 1.0**, **OpenID4VCI 1.0**, **OpenID4VP 1.0**, the **W3C Digital Credentials API**
  (working draft, 4 September 2026), and **RFC 10027 / BCP 247**.
- **ISO/IEC 18013-5:2021** and its second edition (slipped to Q1 2027); **ISO/IEC
  TS 18013-7:2025** and its Annex C; the **ISO/IEC TS 23220** series.
- **ETSI** TS 119 612 and TS 119 602 (lists of trusted lists), TS 119 472-1/2/3, TS 119 101,
  TS 119 431-1, TS 119 432, TS 119 476-2; **EN 419 241**, **EN 17640**, **EN 301 549
  v3.2.1** (accessibility), **EN 419 221-5**.
- **ECCG Agreed Cryptographic Mechanisms v2.0** (v3.0 in draft) — the 125-bit floor for
  recommended mechanisms, and the catalogue that names no ZK scheme, which is the `ZKP_08`
  tension (FR-41, R3).
- **Recommendation (EU) 2026/1035** — 31 December 2026 for age-verification availability.
- **Estonia:** a bill amending the e-identimise ja e-tehingute usaldusteenuste seadus (the
  eIDAS transposition), the State Fees Act and the Penal Code; approved by the Government
  on 1 October 2026 and before the Riigikogu, making RIA the competent authority and
  creating a register of persons relying on the wallet. The Riigi Teataja text is
  client-rendered and was not retrievable, so **no provision of Estonian law is cited here**
  `[UNVERIFIED]`.

**Evidence position against the record's own 41-row requirement index:** 5 rows supported,
23 partial, 7 missing, 5 not applicable, 1 unsatisfiable — and **no row "compliant"**. The
single unsatisfiable row is ARF `ZKP_08`. This distribution is the honest summary of where
the product stands, and no artefact may summarise it more favourably.

**Accessibility duty:** WCAG 2.1 AA is the Estonian public-sector baseline and EN 301 549
the presumption (NFR-12). No EN 301 549 audit evidence exists yet (R13).

## 11. Stakeholders and Approvals

**Who the product serves** — an assessor or programme evaluator; an Estonian relying party
and its integrator; a Ministry of Justice and Digital Affairs or RIA reader; an EU
programme contact; the holder, as the subject of the privacy guarantees; and the visitor to
the sandbox site.

**Named parties whose decisions the product depends on, and what each owes it:**

| Party | Relationship | What the product needs from them |
|---|---|---|
| RIA (Estonian Information System Authority) | Competent authority under the bill before the Riigikogu; named as the authentication broker and register operator in the record | the register's interface, an integration agreement for TARA, and a statement on whether the interface proposal is usable |
| Ministry of Justice and Digital Affairs | Owner of the bill and of the Estonian timeline | the publication of the act's text so citations can be verified |
| The AV solution's scheme owner | Owner of the accepted circuit set | the accepted set, published; and a decision on who hosts a circuit registry |
| An Estonian service owner (partner) | The party D3 and SM-3 depend on | a review of the register interface proposal and a published acceptance result |
| The certification ecosystem (national scheme → ENISA/CSA) | The only route to a certified wallet | nothing in v1; the product claims no status on it (NG-16) |
| Upstream `open-eid/eudi-wallet-poc` | The fork's base, dormant at one commit in twelve months | nothing; the fork carries its own maintenance (R10) |
| The maintainer (Tom Kristian Abel) | Sole maintainer and the only approver of this document | the decisions reserved to them: filing DCHP #17 (NG-18); any import from the private `ee-eudiw` repository; publication of anything derived from it; committing nothing from `demo-data/` |

**Approvals — what this document does not have.** No Estonian authority, no scheme owner
and no certification body has approved, endorsed or assessed this product or this PRD, and
nothing in the product may imply otherwise (NG-13). The fork is independent; RIA is named
as a stakeholder because the product is built to be useful to it, not because it
commissioned or approved anything.

## 12. Integration and Dependencies

| Surface | State | What the product does about it |
|---|---|---|
| **TARA** (RIA's authentication broker) | Named by RIA as the central integration point; no published EUDI adapter exists | FR-22: an adapter contract with its assumptions stated and a test against a TARA test client, or the absence recorded |
| **Relying Party Register** | No published specification or API; access-certificate design unfinished; MVP reported late-2026/early-2027 | FR-23, FR-24: interface-first, mocked, fail-closed, proposed rather than guessed (R9) |
| **X-Road / X-tee** | No documented relationship to EUDI or OpenID4VP; "OpenID" appears zero times in the sources examined | NG-15: named as *not* an integration path, so teams stop reaching for it (P4) |
| **e-Residency / Digi-ID** | A constraint on what a holder holds, not a pathway | Carried as the UJ-4 context and as an open question about real PID issuance (NG-12) |
| **Smart-ID / Mobile-ID** | Commercial agreement needed; a second authentication route for LoA-High steps | Out of scope in v1; recorded so nobody assumes it is bundled |
| **External ZK implementations** (the reference wallet, multipaz, SUNET/vc, the Commission's iOS Longfellow library, Longfellow's own Go verifier service) | Exist and are the counterparts an external run needs | FR-19: one of them is exercised and the result published, pass or fail |
| **The AV trusted list** | Live; exactly one recognised provider (Denmark's AltID) | Not consumed by this product; recorded so no reader assumes a supply of AV wallets exists today |
| **Standards venues** (OpenID DCHP, OpenID4VP issue #627, ISO/ETSI) | Open | FR-43: the prepared submission and its unfiled state; the open issue tracked with its date |
| **Host toolchain** | Rust staticlib, Go, JDK 17, pinned Python inputs, `make deps` | §9.4 constraints; NFR-8 reproducibility; NFR-11 pinning |
| **Pre-1.0 dependencies** (multipaz, eudi libraries, a COSE snapshot via the Maven mirror) | Churn expected; one snapshot dependency is mutable | R14, R11, SG-5: pinned by coordinate and commit, inventoried, constrained transitively |

## 13. Risk and Mitigations

The register below is the record's, carried row for row. **No row has an owner, because the
record assigns none** — ownership is an open question (§18, OQ-9). Each row therefore
carries the condition under which it should be revisited.

| # | Risk | L / I | Mitigation | Revisit when |
|---|---|---|---|---|
| R1 | ISO/IEC 18013-5 second edition ships `ZkRequest`/`ZkDocument` differently from the draft the carrier approximates | M / H | version-label every fixture; keep `oid4vp/carrier.go` the single adaptation point; re-run vectors on publication | the standard publishes (currently Q1 2027) — its text could not be read at all during the record's research `[UNVERIFIED]` |
| R2 | A ZK system other than Longfellow becomes the EU-preferred one (the BBS-family TS14 path) | L–M / H | keep other families out of the carrier but keep the one-format-family-per-scheme rule so a second can be added without a dialect collision | ETSI TS 119 476-2's direction resolves |
| R3 | `ZKP_08`'s unsatisfiable requirement is resolved in a way that invalidates the current scheme | L / H | state the tension in writing wherever a programme is told about compliance; track the ECCG catalogue's next revision | the catalogue adds a ZK entry, or the ARF relaxes the requirement |
| R4 | No scheme owner adopts the circuit registry (O4), or the accepted set lands elsewhere | M / M | ship it as a generator plus a hosted instance with a documented succession path; keep a local pinned list as fallback | the AV Profile names the maintainer of the accepted set |
| R5 | Verification cost makes a relying party's economics unattractive (p95 4.91 s against a 1.0 s budget on the fork's own host) | M–H / M | measure on target hardware (B2); admit control per credential; cache nothing that weakens binding; publish honest figures | D2/M3 run, or an RP states a peak-rate assumption |
| R6 | Proof size (~360 KB) breaks a proximity or mobile-network budget | M / M | measure BLE transfer (B3); keep proximity Tier-2 optional until measured | a proximity Tier-2 requirement appears |
| R7 | Privacy regression via the plain fallback path | M / H | the wallet labels the tier and the record; `EE-ZKP-055a` refuses plain-mdoc-only requests on a ZK-capable device; the fallback stays visible in logs and UI | an RP reports it cannot complete a flow it needs |
| R8 | The registration-certificate policy loosens a control through a parsing error | L–M / H | default to refusal; property tests over generated certificates; explicit negative tests on the loosening path | O3 is built (phase P2) |
| R9 | Register on the critical path — a fail-closed lookup makes every presentation depend on a service that does not exist yet | H / M | mock now, interface-first; bounded cache with an explicit stale policy; never silently allow | RIA publishes an interface or an availability target |
| R10 | Upstream is dormant, so the fork carries the whole maintenance load | H / M | strict superset (0 commits behind, verified); vendor what is needed; keep upgrades pinned and routine | upstream resumes; or a second maintainer appears |
| R11 | Licence and provenance risk in a derived work (MIT base, but vendored/mirrored artefacts ship) | L–M / M | inventory every vendored artefact with upstream licence and revision; keep the MIT notice; import nothing from the private repository without written permission | the inventory is produced (currently the mirror's contents are unenumerated `[UNVERIFIED]`) |
| R12 | Documentation contradicts the code (the stale `issuerAuth` row) | H / M | fix the row; add a check that statuses are re-derived from tests rather than hand-maintained | v1 ships — this row is in scope (FR-38) |
| R13 | Accessibility duty unmet at audit time (WCAG 2.1 AA claimed, no EN 301 549 evidence) | M / M | run an accessibility audit as a milestone exit criterion | any public-sector audit is scheduled |
| R14 | Third-party dependency on a pre-1.0 library (multipaz pinned, eudi libraries pre-1.0) | H / M | pin, constrain transitively, and keep the conformance module to detect breakage early | multipaz 1.0, or a Longfellow naming change |
| R15 | A programme reads an `[ESTIMATE]` as a commitment | M / M | keep method and assumptions attached to every number; never present a range without its baseline | any figure is quoted externally |
| R16 | **The product's own audit carries unmitigated high findings on paths it advertises** (consent gating, key user presence, consent-screen claims, verifier authentication) | H / H | publish them (SG-1, NFR-16); decide release scope (§18, OQ-8); do not present the consent screen's claims as verified while the chain is unvalidated | the v1 release decision on OQ-8 is taken — **assistant-added row, not present in the record's A4** |

## 14. Operational Requirements

- **OR-1 — v1 ships no service. It ships a reference deployment.** The relying-party
  acceptance kit is a deployment profile an operator runs; no availability target, no
  support window, no on-call rotation and no incident process is claimed, because R1 is
  deferred (§6.2). Stating this is the point: a reader must not infer an operational
  commitment from a deployment artefact.
- **OR-2 — Capacity is stated as verifications per second per instance type, with
  percentiles and the host class** (NFR-6). The only measured figure today is 2,414 ms per
  valid proof on a 2 vCPU host.
- **OR-3 — Refusal cost is bounded and regression-tested.** A refusal must not cost seconds
  of compute; the 54–58 s defect fixed on 24 September 2026 stays covered by a test.
- **OR-4 — The relying party's log carries the ARF Topic 19 fields and no attribute
  value.** What an operator can log about a presentation, and what it must not, is fixed
  here rather than left to the deployment.
- **OR-5 — Logging is local-first and central-free.** The wallet's activity log is the
  user's and distinguishes linkable from new-visit presentations; there is no central age
  log (PG-5).
- **OR-6 — Failure is diagnosable without a support channel.** Every refusal names its
  cause (NFR-14), and every published result names the commit, the document revision and
  the commands that produced it (NFR-8).
- **OR-7 — Deployment hygiene.** The build is secretless, the mirror is the only network
  dependency during a build, and no operator secret is committed (NFR-9).

## 15. Data Governance

- **DG-1 — v1 processes no real personal data.** Test identities are synthetic and labelled
  as such; the "Population Register" is a sandbox panel that computes age predicates from a
  typed birth date; the relying-party kit runs against mocks. This is why no DPIA is
  claimed for v1 `[ASSUMPTION: a DPIA is not applicable while no real personal data is
  processed; if a real issuer or a real register enters the deployment, this needs its own
  assessment.]`
- **DG-2 — The holder's device private key is never committed.** `demo-data/` is excluded
  from the repository by policy and by check (SG-6, NFR-9).
- **DG-3 — Data minimisation is enforced on the wire, not promised in prose.** The
  transcript carries no isikukood and no full date of birth in any covered scenario (FR-7);
  the age presentation carries a boolean and nothing else (FR-10, PG-2).
- **DG-4 — Retention is ephemeral by design.** Sessions are single-use with a TTL, and a
  replayed session returns `410`. The sandbox holds a run's state only for that run.
- **DG-5 — No analytics, no telemetry.** The fork carries no Firebase Analytics or
  Crashlytics, and no product telemetry is introduced by this release.
- **DG-6 — The register lookup is a new data flow with no governance yet.** A fail-closed
  lookup puts a register query on every presentation's critical path. Who sees that query,
  what it reveals about a presentation, and how long it is retained are undecided and are
  an open question (§18, OQ-3).
- **DG-7 — Pseudonymity is not claimed.** ARF `ZKP_04` pseudonyms are out of scope (NG-8);
  the per-path unlinkability position is stated per path with a test behind it (FR-42), and
  where it cannot be claimed it is recorded as not claimed.

## 16. Platform, Surfaces and Tone

**Surfaces this release ships or preserves:**

| Surface | Form | Notes |
|---|---|---|
| Android wallet | Kotlin, Jetpack Compose, JDK 17; `:app:installLocal_mocks` for a device | `debug` targets placeholder backends and fails onboarding after PIN entry |
| Verifier | Go services over a Rust staticlib (`libzkverify.a` through cgo) | needs `make deps`; `go vet ./...` clean; `zk` package tests need the Rust build |
| Issuer / holder | Python | inputs pinned in `tests/requirements-ci.txt` |
| Sandbox site | static HTML/CSS/JS served by a Python standard-library server, bilingual | no framework, no build step |
| Conformance runner | JVM module today (`:zk-conformance`), extended by the AV and carrier runners | `-Dzk.regenerate=true` is the only way committed fixtures change |
| Relying-party acceptance kit | a Go deployment profile, new in v1 | must not fork the verifier (FR-21) |
| Documentation | `docs/`, `tests/README.md`, `docs/README.md` index | every new document is indexed (SM-7) |

**Tone and aesthetic.** `DESIGN.md` is the design authority for the sandbox site — the
TEDI-derived visual world, ria.ee chrome, Roboto and Material Symbols, the visitor's step
rail and ledgers, Estonian first. This PRD does not restate it and does not override it.
The wallet follows the platform conventions of its own host. The product principles the
record pins, which every surface is judged against, are:

1. **Show, don't claim** — every number on a page comes from the run that just happened.
2. **Say what the shop did not learn as plainly as what it did.**
3. **Name the sandbox's shortcuts where they happen, not in a footnote.**

To these the PRD adds one, for the surfaces this release introduces (the conformance
reports and the acceptance kit):

4. **A report that hides its out-of-scope clauses is not a report** (FR-16, CM-1).

## 17. Why Now

Six dated facts, each from a primary source fetched for this PRD's landscape pass, each
moving the product's window:

1. **1–2 September 2026** — the Commission's Age Verification Profile revision made ZKP
   the preferred mechanism for age checks and named `longfellow-libzk-v1` as the only
   system in scope: "an implementation SHALL use that system. Support for any other
   Zero-Knowledge Proof system does not constitute conformance." The fork already runs that
   system, which is why its gap is packaging rather than cryptography.
2. **15 October 2026** — the Commission shipped `av-lib-ios-longfellow-zkp` v0.19.0, a
   Swift prover and verifier over Google's `MdocZK`. The mandated path now has a second
   implementation, eight days before this PRD.
3. **16 October 2026** — Google Wallet documented `mso_mdoc_zk` as a public relying-party
   integration step, recommending a reference verifier and the Longfellow verifier service.
   The carrier this fork profiles has a real counterpart.
4. **19 May and 3 June 2026** — the EU's age-verification trusted list went live with
   exactly one recognised provider (Denmark's AltID). The trust framework is real, and its
   supply is thin.
5. **29 April 2026** — Recommendation (EU) 2026/1035 set 31 December 2026 for age
   verification to be available, which is the deadline the Estonian acceptance duty sits
   against.
6. **17 September 2026** — the EU KIDS Act proposal added a lower threshold (15) and a
   consultation running to 3 December 2026, which is the direction the multi-threshold work
   (O6) anticipates.

Estonia's own clock: the procurement launched 18 May 2026 and closed 29 June 2026 at
€21.65 M over up to 96 months, with **no award found as of 23 October 2026**
`[UNVERIFIED — absence of evidence]`; the Government approved the eIDAS-transposition bill
on 1 October 2026; state e-services must accept other Member States' wallets from end-2026
with TARA as the stated integration point; the regulated private sector follows from
end-2027; and the national wallet launches in 2028. The window this product addresses is
the gap between end-2026 and 2028, in which Estonian services must accept wallets with no
Estonian wallet to test against. Estonia is not an age-verification app pilot and holds no
provider entry on the AV trusted list.

**What has not changed, and weakens the "why now" rather than strengthening it:** ARF is
still v3.0.0 (23 July 2026); TS4/TS13/TS14 have not moved since winter 2026 and are
explicitly exploratory, handed toward ETSI; no accepted circuit set has been published; and
no one who mandates on-device proving has published an on-device proving figure. A product
that claims a settled standard would be wrong on all four counts.

## 18. Open Questions

**Phase-blockers — both resolved on 2026-10-23 before finalisation, and recorded here
rather than deleted:**

- **OQ-8 — the audit's TIER 1 findings — RESOLVED: the four user-facing gates are in v1.**
  The maintainer directed that the four gates be closed in this release: `readerAuth` chain
  validation to a trust anchor, a user-presence gate on credential keys, a consent gate that
  resists guessing, and a consent step on the proximity share — carried as FR-45 … FR-48
  (§4.7), with SM-9 and CM-7 as their metric and counter-metric. The audit's remaining
  findings — supply-chain pinning, main-thread blocking, debug logging, the mock trust
  surfaces and the release path — are left to a hygiene release, and their state is
  published (SG-1, NFR-16) rather than implied fixed. Two related items the decision did
  *not* cover stay open and visible in SG-1 and SM-8: signed authorization requests
  (`EE-PRO-004`, with the `EE-PRO-003` client-id prefix) and `EE-PRO-010a`.
- **OQ-10 — the missing external partner — RESOLVED: the kit ships, and SM-3 and SM-4 are
  restated as unachieved.** No Estonian service owner or integrator is engaged, and the
  record's own three external dependencies (register API, TARA terms, an Estonian test
  wallet) are all absent. The relying-party acceptance kit therefore ships with
  maintainer-produced acceptance evidence, SM-3 and SM-4 carry the reason and a revisit
  condition instead of a pass (both are marked in §7.1), and any self-produced result is
  labelled as self-produced wherever it is published. Recruiting a partner stays a goal,
  not a release gate.

**Open questions carried from the record** (none of these may be silently resolved in the
product):

- **OQ-1 — Is `ZKP_08` satisfiable at all?** The ARF requirement pins ZK schemes to the
  ECCG mechanisms catalogue, and the catalogue names no ZK scheme — including the mandated
  one. The record calls this "the largest single uncertainty". The product reports it
  (FR-41) and does not resolve it.
- **OQ-2 — Who owns the accepted circuit set, and who would host a registry?** The AV
  Profile defers the set to a "scheme owner" without naming one. O4's value depends on the
  answer.
- **OQ-3 — Will RIA publish a register interface, and what will its access certificates
  look like?** Also: will certificates be CT-logged, and what does a register lookup expose
  about a presentation (DG-6)?
- **OQ-4 — Is Article 45b(2) about an *issuer* class or a *compliant* class?** The record
  calls this the single most consequential unresolved legal question. It is a legal
  question and this PRD does not answer it (NG-4).
- **OQ-5 — Does the ISO second edition's `ZkRequest`/`ZkDocument` shape differ from the
  draft the carrier approximates?** The text could not be read during the record's
  research `[UNVERIFIED]`.
- **OQ-6 — Given that the AV Profile carries ZK only over the DC API path, is the
  OpenID4VP `mso_mdoc_zk` carrier worth maintaining as a first-class surface?** The
  landscape pass found the profile expects a plain mDoc over OpenID4VP because no standard
  DCQL query for ZK exists there, while this fork's profile and this release both treat the
  OpenID4VP carrier as primary. Recommended: keep both, but state in every artefact which
  path is mandated (DC API) and which is profiled-but-unofficial (OpenID4VP), so no reader
  infers EU endorsement of the second.
- **OQ-7 — Which national thresholds beyond 18 are actually required?** O6 is gated on this
  and on scheme-owner acceptance; unmapped thresholds stay unsupported.
- **OQ-9 — Who owns the risk register?** No row in the record has an owner and this
  product has one maintainer, so every risk's response depends on one person. Assigning
  owners (even if only to name the maintainer) is a decision this PRD cannot take.
- **OQ-11 — Should the proximity path keep advertising itself to relying parties at all?**
  The wallet has a proximity screen; on a ZK-capable device an age request there is
  refused. Either the refusal is stated to the venue as the product's behaviour, or the
  proximity age path is removed from the advertised surface. Undecided.
- **OQ-12 — Who hosts the sandbox site publicly, if anyone?** The site is the product's
  public face and today runs on a host the visitor must build. Hosting it changes the
  evidence story (a stranger's run is not a stranger's reproduction) and needs a decision.
- **OQ-13 — Which external implementation should the carrier suite target first?** The
  record leaves this open between the multipaz sample and the reference wallet. The
  landscape pass adds the Commission's iOS library and Longfellow's own Go verifier service
  as candidates.
- **OQ-14 — Does the project want an EU-level role at all?** The record's own non-goals
  keep this product out of certification, out of standards filing and out of other Member
  States' implementations. If the answer changes, §2's audience changes with it.
- **OQ-15 — Will RIA adopt the AV Profile's attestation, and is a foreign AV attestation
  admissible to an Estonian relying party?** The record raises both and neither is settled
  in published form; the relying-party kit's four acceptance cases assume admissibility.
- **OQ-16 — Does a two-predicate refusal cost the product a lawful use case in practice?**
  The record's scenario material suggests a service that needs "over 18 and over 21" would
  have to make two requests; whether that is acceptable to an Estonian service owner is
  untested and NG-9 does not move until O3 lands.

## 19. Assumptions Index

Every inference this PRD makes that the intent record does not state, with the evidence it
rests on and the place it appears.

| # | Assumption | Where | Basis | What would settle it |
|---|---|---|---|---|
| A-1 | The first reader is an assessor or programme evaluator, and the Ministry/RIA reader is served by the same artefact | §1 | The launch-grade stakes answer plus the record's "primary buyer is a programme/regulator-adjacent actor or an integrator" | The user's own words — the follow-up question went unanswered |
| A-2 | UJ protagonist names and one-line contexts | §2.3 | Drafted from the record's actor types; every beat, outcome and edge case traces to the record or `DESIGN.md` | The user narrating one real session per journey |
| A-3 | The sandbox site and the presentation spine are in v1 as shipped behaviour | §6.1 | Both exist, and the release's claims are demonstrated on them | An explicit forward-only decision |
| A-4 | Risk rows carry no owner; revisit conditions stand in for ownership | §6.3, §13 | The record's A4 has no owner column and names none | OQ-9 |
| A-5 | No DPIA is applicable to v1 because no real personal data is processed | §15 | Test identities are synthetic; the register and issuer are mocks | A real issuer, a real register or a pilot deployment |
| A-6 | PRD identifiers (`FR-n`, `NFR-n`, `UJ-n`, `SM-n`, `NG-n`, `OQ-n`, `R-n`) are kept distinct from `epics.md`'s `FR1–FR66` / `NFR1–NFR10` (11 epics, 76 stories) | §0 | `epics.md` already defines its own numbering and a coverage map | A downstream decision to renumber one of the two |
| A-7 | The O6 multi-threshold estimate of 6–12 engineer-weeks excludes the unbounded external wait for scheme-owner-accepted circuits | §6.3 | The record labels the wait "external, unbounded" | A scheme owner naming an acceptance process |
| A-8 | The intent record's count discrepancy is resolved: the specification holds **211 requirements** across 14 families, and the corpus's "per-area counts summing to 622" double-counts | §0, §10 | Independently re-derived for this PRD by extracting and de-duplicating the identifier set from `spec/EE-EUDIW-TS-1.0.md` (211 requirement identifiers: ZKP 38 · POA 35 · PRO 20 · SEC 18 · PRI 18 · SIG 16 · GOV 14 · ONB 11 · PID 9 · WUA 8 · REV 7 · CNF 7 · RP 6 · ACC 4). A naive identifier extraction returns **213**, because `EE-AGE-1` and `EE-AGE-2` are **conformance-profile names** (the spec's §20), not requirements, and must not be counted as either — the count of 211 is the requirement count | Nothing — this is verified; recorded so no reader re-opens it |
| A-9 | The user's decisions on entry point, Vision and v1 scope — and the two phase-blocker decisions, FR-45 … FR-48 in v1 and SM-3/SM-4 restated as unachieved — were taken from scored rubrics whose separation was contested in every case | §1, §4.7, §6, §7.1, §18 | The user's own answers, recorded in `.memlog.md` and `.decisions.log` | A later reversal, which is a PRD change (NFR-17) |
