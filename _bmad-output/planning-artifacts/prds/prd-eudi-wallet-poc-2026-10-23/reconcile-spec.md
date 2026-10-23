# Reconciliation: `spec/EE-EUDIW-TS-1.0.md` against `prd.md`

**Purpose.** Report where the PRD makes a claim about the specification that the specification
does not support, or where a requirement family the PRD's stated scope clearly covers is absent.
This document does not argue that the PRD *should* carry any requirement; scope judgements belong
to the maintainer (§6.1/§6.2 of the PRD).

**Inputs.**
`spec/EE-EUDIW-TS-1.0.md` (revision 1.7 draft, 12 October 2026, 1695 lines, 211 unique
`EE-<AREA>-<nnn>` identifiers over 14 families), read directly; `extract-spec.md` used for the
requirement index and family counts only. PRD under review:
`_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md` (1628 lines) and
`addendum.md` beside it. Corroborating repository records are cited only where marked.

**Citation convention.** Every spec claim below quotes the requirement verbatim. `EE-*`
identifiers are the specification's; `FR-n`/`NFR-n`/`NG-n`/`PG-n`/`UJ-n`/`OQ-n`/`R-n` are the
PRD's.

**Headline.** The PRD cites **10** of the specification's 211 requirements
(`EE-CNF-005/006/007`, `EE-POA-006/007`, `EE-PRO-010a`, `EE-WUA-008`, `EE-ZKP-045a`,
`EE-ZKP-051`, `EE-ZKP-055a`); the addendum adds `EE-ZKP-021b`, which is not a specification
identifier at all (see M-3). Two of the ten are misattributed, and the single sentence that
claims spec conformance for the EE-PoA (`FR-8`) is contradicted by the specification's own
§23 item 26.

---

## Coverage map

| Family | Cited / total | Families whose subject the PRD's scope clearly covers |
|---|---|---|
| EE-ZKP (zero-knowledge proofs) | 3 / 38 | yes — the product's core claim |
| EE-POA (proof of age) | 2 / 35 | yes — `FR-8` claims to implement this family's rules |
| EE-PRO (protocols) | 1 / 20 | yes — both carriers and the verifier are shipped surfaces |
| EE-PRI (privacy) | 0 / 18 | yes — `PG-1…PG-7`, `OR-5`, `DG-1…DG-7` make privacy claims |
| EE-CNF (conformance) | 3 / 7 | yes — `EE-CNF-005` is cited, so the family is in scope |
| EE-RP (relying party) | 0 / 6 | yes — the acceptance kit is an RP-side product |
| EE-ACC (accessibility) | 0 / 4 | yes — `NFR-12`, `FR-35` set the accessibility baseline |
| EE-WUA | 1 / 8 | partly — the glossary defines the WUA |
| EE-PID (9), EE-REV (7), EE-SEC (18), EE-GOV (14), EE-ONB (11), EE-SIG (16) | 0 / 75 | no — outside the fork's scope except as noted in Appendix B |

For reference, the specification's own EE-PoA binding targets split issuer-only 15, wallet-only
12, mixed/other 4 of 35 — relevant to L-1.

---

## Critical

### C-1. `FR-8` states a batch size the specification calls non-conformant, and claims spec conformance in the same breath

**PRD, `FR-8`** ("The EE-PoA attestation follows its consumption and validity rules (shipped)"):
> "The product can issue and consume EE-PoA attestations **without breaking the spec's
> once-only and validity rules**. … Attestations are issued in **a batch of three** in one
> transaction; a plain presentation consumes its attestation and a ZK presentation consumes
> none."

**Spec, `EE-POA-011 (SHALL)`:** "Batch size SHALL be **at least 30**, except on a device whose
WSCD cannot sustain 30 keys, where `EE-POA-011b` governs and sets the batch size instead."

**Spec, `EE-POA-011b (SHALL)`:** "Where the device's WSCD can sustain 30 concurrent attestation
keys, `EE-POA-011` applies unchanged and the batch SHALL be at least 30. Where it cannot, and
**only** where it cannot, the wallet SHALL issue a batch of the largest size the WSCD is
**measured** to sustain … **A batch reduced for any reason other than measured WSCD capacity —
throughput tuning, issuer convenience, storage economy — is non-conformant.**"

**Spec, §23 item 26** (open issues): "a **batch-of-three** `batchCreateKey` in one transaction now
exists — **three keys, below `EE-POA-011`'s floor of 30, and not a measured `EE-POA-011b`
maximum**."

**Why this is a discrepancy.** The specification is aware of this fork's exact behaviour and
names it as below the floor and unjustified by measurement. The PRD states the same fact as
"without breaking the spec's … rules", and never mentions `EE-POA-011`, the floor of 30, the
measured-capacity justification, the dashboard disclosure a reduced batch requires
(`EE-POA-011b`, `EE-POA-015`), or §23 item 26's open status. A reader of the PRD would conclude
the issuance posture is conformant.

*Suggested fix:* restate `FR-8` as "a batch of three, **below `EE-POA-011`'s floor of 30**, not
justified by a measured `EE-POA-011b` maximum, and therefore a declared deviation (spec §23
item 26)" and drop the conformance claim.

### C-2. The feature NFR reclassifies the specification's verifier budget as a "device-side prover budget"

**PRD, §4.1 feature-specific NFRs:**
> "The specification's own budgets (prove ≤ 2.0 s Tier 2 / 3.5 s, verify ≤ 1.0 s p95, ≤ 250 MB,
> transport ≤ 4.0 s) are not met … **No document may state the 1.0 s figure as the verification
> budget; it is recorded as the device-side prover budget**, with the verifier expressed as a
> capacity statement instead (see §8 NFR-6 and §6.2)."

**Spec, `EE-ZKP-040 (SHALL)`:** "a ZKP age presentation SHALL complete **proof generation in
≤ 2.0 s at p95 and ≤ 3.5 s at p99** … and **the verifier SHALL complete verification in ≤ 1.0 s
at p95**."

**Why this is a discrepancy.** In the specification the 1.0 s figure *is* the verifier's budget;
the prover's budgets are 2.0 s / 3.5 s. The PRD's own `NFR-6` states it correctly ("No document
repeats the spec's `p95 ≤ 1.0 s` verification budget as a target"), so §4.1 contradicts both the
specification and `NFR-6`. Declaring the budget unmet is honest and is not the problem; naming it
something the spec does not name it is.

*Suggested fix:* delete "recorded as the device-side prover budget" and keep `NFR-6`'s wording —
the 1.0 s is the spec's verifier budget on the reference device class, declared unmet here.

---

## High

### H-1. The `EE-POA` family: 2 of 35 cited, and the two are the Provider's, not the wallet's

The only `EE-POA-*` identifiers the PRD cites are `EE-POA-006`/`EE-POA-007` (`FR-31`), both of
which bind the **issuer / EE-PoA Provider** ("Age predicates SHALL be computed … from the
authentic source"), cited there only as "the trust-the-client enrolment weakness the spec warns
about". The requirements the wallet is bound by, all SHALLs, and all in the scope of the PRD's
own `FR-8`/`FR-9` claims, are absent:

| Identifier | Spec requirement (abridged) | Relation to the PRD |
|---|---|---|
| `EE-POA-010` | "issued as a batch of single-use technical attestations (ARF Method A), each bound to a distinct key pair, … via the `proofs` parameter" | `FR-8`, `FR-9` |
| `EE-POA-011a` | "Batch key generation SHALL NOT occur on the presentation critical path … SHALL generate the key set and obtain the batch **ahead of need** … when the remaining batch falls below a low-water mark" | absent |
| `EE-POA-011c` | "A refill that the low-water mark … makes due SHALL NOT start in the same session as the presentation that crossed the mark … SHOULD be deferred to the next charging-and-connected trigger" | absent |
| `EE-POA-012` | "All attestations in a batch SHALL carry **identical `ValidityInfo` timestamps** … Uncoarsened timestamps are themselves a linking vector" | `FR-8`'s validity bullet does not carry it |
| `EE-POA-013` | "the wallet SHALL delete it, **SHALL destroy the corresponding WSCD key material** … Destruction is mandatory and not opportunistic" | `FR-8` covers consumption, not key destruction |
| `EE-POA-014` | "Re-issuance SHALL go to the same Wallet Unit, using a device-bound refresh token per OpenID4VCI §14.5" | absent |
| `EE-POA-015` | "The wallet SHALL log each re-issuance event in the user-visible dashboard" | absent (see H-4) |
| `EE-POA-016` | "EE-PoA validity SHALL NOT exceed **3 months**, and the user SHALL be re-identified at least every 3 months" | absent |
| `EE-POA-024` | AV attestation "SHALL be the designated age-proof path", conditional on negotiation and threshold match | see H-7 |
| `EE-POA-025` | 14-day re-issuance start | see H-2 |
| `EE-POA-026` | shared document-signer certificate per epoch; uniform optional-predicate set | `FR-8` carries only the 7-day epoch |
| `EE-POA-032` | one Wallet Unit per person | absent |

**Why this is a discrepancy.** `FR-8` claims the family is implemented "without breaking the
spec's … rules" while 33 of its 35 requirements are uncited, including the batch-management rules
on which the claim rests. Substantive coverage of `EE-POA-030`/`031` exists in `PG-2`/`PG-3`, and
of `EE-POA-017` in `NG-11`, but the identifiers and their conditions are not carried.

### H-2. `EE-POA-025` is demoted rather than declared unmet

**PRD, `FR-8`:** "**Out of Scope:** the spec's 14-day re-issuance lead as an automatic background
issuance; re-issuance here is user-started (§6.2)." Also `FR-8`: "Renewal is **offered** from the
day the earliest held `validUntil` falls inside the re-issuance window."

**Spec, `EE-POA-025 (SHALL)`, binding target wallet:** "The wallet SHALL start re-issuance no
later than 14 days before the `validUntil` of the attestations it holds, independently of the
low-water mark of `EE-POA-011a`."

**Why this is a discrepancy.** The requirement is not about "automatic background issuance"; it
is a wallet SHALL to *start* re-issuance on a schedule, and `EE-POA-011c` governs the trigger.
The PRD turns a binding obligation on its own component into an out-of-scope product choice and
never records it as unmet, where it does declare the performance budgets unmet (`NFR-6`) and
`EE-WUA-008` unmet (`FR-15`). The record's own conformity note agrees it is only "Partially met"
(`docs/CONFORMITY.md` row for `EE-POA-025`).

### H-3. The `EE-PRO` family: 1 of 20 cited, and that one is misattributed

**Misattribution.** PRD `SG-1` and `OQ-8` list "unsigned OpenID4VP authorization requests with a
bare `client_id` claim (`EE-PRO-010a` unimplemented)".

**Spec, `EE-PRO-010a (SHALL)`:** "Cross-device presentation SHALL use the W3C Digital Credentials
API, where the QR is generated by the browser and proximity is established by the CTAP hybrid
transport …"

`EE-PRO-010a` is the cross-device DC API requirement. The unsigned-request requirement is
`EE-PRO-004 (SHALL)`: "In redirect flows, unsigned request objects SHALL NOT be accepted except
where OpenID4VP 1.0 permits them with the `redirect_uri` prefix, which SHALL NOT be used by
Estonian relying parties." The client-identifier requirement is `EE-PRO-003 (SHALL)`:
"Relying party authentication SHALL use the `x509_hash` client identifier prefix for every signed
request, redirect and Digital Credentials API alike".

**Dropped wallet SHALLs on the surfaces the PRD ships** (`FR-1`, `FR-2`, `FR-3`, `FR-21`):

- `EE-PRO-002 (SHALL)`: "Presentation Exchange SHALL NOT be used."
- `EE-PRO-007 (SHALL NOT)`: "SIOPv2 SHALL NOT be used, and the wallet SHALL refuse a request whose
  `response_type` asks for an `id_token`" — the spec records this as a deliberate divergence from
  the EU baseline (§22 item 20); the PRD carries no divergence list.
- `EE-PRO-003`: `x509_hash` for every signed request. The record states the shipped verifier
  "defaults its client identifier to `x509_san_dns:verifier.example.ee` …, which `EE-PRO-003`
  rev 1.5 no longer permits" (`docs/CONFORMITY.md`), and the PRD's `FR-21` makes that verifier
  the RP-side deployment profile without noting it.
- `EE-PRO-004`, `EE-PRO-013` ("the wallet SHALL refuse the request, before any proving work,
  where the platform-asserted origin is not among [`expected_origins`]"), `EE-PRO-025`
  (`readerAuth` for `org-iso-mdoc` and proximity), `EE-PRO-010` (same-device DC API preferred),
  `EE-PRO-011` (device retrieval, "SHALL NOT use server retrieval").
- `EE-PRO-026 (SHALL)`: "A `request_uri` and the nonce it carries SHALL be single-use and SHALL
  expire within 3 minutes of creation … The 3 minutes are this specification's choice."
- `EE-PRO-020`–`024`: issuance protocol (batch `proofs` parameter, Notification Endpoint,
  encryption, pre-authorized code flow with `tx_code`).

**Why this matters.** One identifier is cited and it points at the wrong requirement; the family
that governs both carriers the PRD calls primary is otherwise unrepresented, including two
SHALL NOTs (`EE-PRO-002`, `EE-PRO-007`) that a conformance reader would look for first.

### H-4. The `EE-PRI` family: 0 of 18 cited while `PG-5`, `OR-5` and `DG-5` make privacy-log claims

**PRD, `PG-5`:** "**No central age log.** There is no central record of age presentations. The
wallet's own activity log is local, distinguishes linkable from new-visit presentations, and is
the user's." `OR-5` repeats it; `DG-5` adds "no product telemetry is introduced by this release".

- **`EE-PRI-023 (SHALL)`**, binding target "wallet / programme (Wallet Provider)": "No central log
  of age-verification events SHALL be created, **other than the operator's minimal, aggregated,
  non-identifying service telemetry, which SHALL NOT include relying party identity at per-event
  granularity.**" The PRD states an absolute prohibition and drops the qualifier and the
  per-event-identity limit on the operator's telemetry.
- **`EE-PRI-014 (SHALL)`**: "The wallet SHALL provide the dashboard required by Art. 5a(4)(d):
  the transaction log; the list of relying parties and data exchanged; a one-step GDPR Art. 17
  erasure request per TS7 v1.0 and ARF Topic 48; and a one-step report of a relying party to the
  DPA per TS8 v1.0 and ARF Topic 50. The DPA endpoint SHALL be Andmekaitse Inspektsioon."
  `EE-ZKP-053 (SHALL)`: the log records the tier per presentation "and the **dashboard** SHALL
  surface the count of linkable versus unlinkable presentations." `EE-POA-015 (SHALL)`: "The
  wallet SHALL log each re-issuance event in the user-visible dashboard." The PRD has no
  dashboard requirement at all; `PG-5` gives the log two of these five properties.
- **`EE-PRI-020 (SHALL)`**: "A DPIA SHALL be completed before pilot and SHALL specifically
  address: the `isikukood`'s correlator role; issuer-side visibility of issuance and re-issuance
  events; the residual linkability of the Tier 1 path; the ZKP circuit registry as a trust root…"
  The PRD's `DG-1` records "no DPIA is claimed" as an assumption keyed to "no real personal data",
  which is not the spec's trigger ("before pilot") and not its subject list.
- **`EE-PRI-021 (SHALL)`**: alignment with EDPB Statement 1/2025 on Age Assurance — absent.
- **`EE-PRI-022 (SHALL)`**, binding an RP subject to a statutory age gate: "Self-declaration SHALL
  NOT be used as age assurance…" — absent, though the acceptance kit is the RP-side artefact.

### H-5. The `EE-ACC` family: 0 of 4 cited while `NFR-12` and `FR-35` set the accessibility baseline

**PRD, `NFR-12`/`FR-35`:** "**WCAG 2.1 AA is the stated baseline with EN 301 549 as the
presumption**, across the sandbox site and the wallet".

**Spec, `EE-ACC-001 (SHALL)`:** "The wallet SHALL **conform to Directive (EU) 2019/882**
(Art. 5a(21)) and **EN 301 549 v3.2.1**, and SHALL satisfy ARF Topic 54 (`ACC_01`, `ACC_02`) and
ARF ch. 8."

The spec makes EN 301 549 v3.2.1 and Directive 2019/882 a conformance SHALL, with WCAG as a
component of it, not a baseline standing above it; the PRD calls EN 301 549 "the presumption" and
names no Directive (EU) 2019/882 anywhere in §10's instrument list (which does carry EN 301 549
v3.2.1). **`EE-ACC-004 (SHOULD)`**: "The wallet SHOULD be available in Estonian, **Russian** and
English at launch" — the PRD's `NFR-13` ("Bilingual, Estonian first. No string exists in one
language only") neither meets nor records the dropped SHOULD. `EE-ACC-002` (voluntary use, no
disadvantage, ID-card/Mobile-ID paths retained) and `EE-ACC-003` (non-smartphone activation) are
also absent; §12 lists Mobile-ID/Smart-ID only as "Out of scope in v1", which is a different
proposition from `EE-ACC-002`'s "SHALL remain fully supported".

### H-6. `EE-RP-003` is partially dropped from the consent screen the PRD calls load-bearing

**PRD, `FR-10`:** "The consent screen shows the requester, the requested predicate and the channel
before the proof is produced." `UJ-2`: "the consent screen names the requester and the channel."

**Spec, `EE-RP-003 (SHALL)`, binding target wallet:** "The wallet SHALL display, before user
approval, the relying party's **and the service's** trade names **from the access certificate**, a
**user-friendly description of the registered intended use**, and a **link to the applicable
privacy policy** (ARF `RPA_06`, `RPA_10`)."

Two of the three required elements (intended-use description, privacy-policy link) and the
service's trade name are absent from the PRD's consent-screen requirement. The PRD defines
"Intended use" in the glossary but no FR requires displaying it. Relatedly, `EE-RP-002 (SHALL)`
(the RP registers its intended use and "SHALL NOT request data beyond it") and `EE-RP-005`
(intermediaries "SHALL NOT store transaction content data") are uncited, and `EE-RP-006`
("Relying parties SHALL NOT refuse a pseudonym where identification is not required by law") is
absent from an acceptance kit whose four cases are the RP's contract with the ecosystem.

### H-7. The attestation ordering is asserted as an existing property; the spec makes it conditional

**PRD, §1 Vision:** "this fork already mints and consumes that attestation **and prefers it to its
own Estonian `ee.riik.poa.1`**." **PRD, `FR-40`:** "An attestation-ordering statement exists:
`eu.europa.ec.av.1` preferred for every relying party, `ee.riik.poa.1` as fallback, both paths
retained."

**Spec, `EE-POA-024 (SHALL)`:** "…the **EU AV Profile attestation `eu.europa.ec.av.1` … SHALL be
the designated age-proof path**. Where the AV attestation the wallet holds carries the requested
`age_over_NN`, the wallet SHALL prefer it to the EE-PoA for every relying party … **after
negotiating that the presentation request accepts `eu.europa.ec.av.1`** … Where the request does
not accept the AV Profile attestation, or asks for a threshold the AV attestation does not carry,
the wallet SHALL continue with a supported credential … and **SHALL NOT serve an unmet threshold
by presenting the nearest one it holds**."

The spec's preference is conditional in three ways (request accepts the doctype; threshold match;
registered intended use admits the fallback) and carries a prohibition the PRD does not restate.
The repository's own conformity record marks the requirement "**Not met.** No credential-preference
logic between the two doctypes" (`docs/CONFORMITY.md`), so the Vision's "prefers it to its own
Estonian `ee.riik.poa.1`" is unsupported by the record as well as by the spec's conditions.

### H-8. The two conformance SHALLs the cheap route depends on are absent

**Spec, `EE-CNF-001 (SHALL)`:** "The wallet SHALL obtain **OpenID Foundation self-certification**
for OpenID4VP 1.0, OpenID4VCI 1.0 and HAIP 1.0 … Suites are free, open source and runnable in
CI." **Spec, `EE-CNF-002 (SHALL)`:** "The wallet SHALL pass the FCAF test set at
`conformance.eudi.dev` for the functional requirements in Annex III of CIR 2024/2981."

The PRD's §2.2/`NG-16`/§9.4 present the certification route as simply unavailable to this fork
("`CIR (EU) 2024/2981` and the FCAF functional-conformance route are not available to this fork"),
which is a claim about an external scheme, and the PRD's own conformance-kit feature (`FR-11`…
`FR-16`) is the adjacent artefact. `EE-CNF-003`/[REC] and `EE-CNF-004`/[REC] are authority
recommendations and correctly absent.

---

## Medium

### M-1. `EE-CNF-005` is generalised beyond its scope
**PRD, §2.2:** "There is no certification scheme, `EE-CNF-005` **forbids certifying a demo or
debug build**"; `NG-16` and §9.4 repeat it.
**Spec, `EE-CNF-005 (SHALL)`, binding target "wallet instance":** "A wallet instance SHALL NOT be
certified **as EE-AGE-2** on the basis of a demo or debug build variant."
The spec bars one specific conformance claim (`EE-AGE-2`) on such a build; the PRD converts it
into a bar on certification as such.

### M-2. The glossary misstates what gates Tier 2
**PRD, §3:** "**Tier 2** — the zero-knowledge proof posture. Specified in full, **gated by
`EE-CNF-006` and `EE-CNF-007`**…"
**Spec, `EE-CNF-006 (SHALL)`:** "EE-AGE-2 and EE-PROX conformance for a **proximity** Tier 2
presentation SHALL require measured end-to-end evidence against `EE-ZKP-044` … Tier 2 in proximity
is **optional** within EE-PROX until §23 item 24 is closed by measurement, and EE-AGE-2 **MAY be
certified on the Digital Credentials API path alone** … with the certificate stating that proximity
Tier 2 is not covered."
**Spec, `EE-CNF-007 (SHALL)`:** "A wallet instance SHALL NOT be certified as achieving
unobservability (`ZKP_07`) **on a remote-HSM WSCD**."
`EE-CNF-006` gates the *proximity* half only and expressly does not gate the DC API half;
`EE-CNF-007` bites only on the remote-HSM architecture, which the PRD's glossary itself calls a
mock. Neither is a gate on "Tier 2" as such.

### M-3. `EE-ZKP-021b` does not exist in the specification
**Addendum D2:** "anti-downgrade gates (`EE-ZKP-021b`/`EE-ZKP-051`)".
The spec's `EE-ZKP-021 (SHALL)` is a single requirement whose clauses are (a)–(d) — "The proof
SHALL establish, without revealing the credential: (a) a valid issuer signature over the MSO;
(b) presence of the requested attribute with value `true`; (c) the wallet's ability to sign the
session nonce with the attestation's device key; (d) that the attestation is within its validity
period." There is no `EE-ZKP-021b`. The anti-downgrade gates in the spec are `EE-ZKP-051`,
`EE-ZKP-055` and `EE-ZKP-055a`.

### M-4. "There is no proximity path for a Tier-2 ZK proof" — and the citation cuts the other way
**PRD, `NG-6`:** "The wallet has a proximity presentation path, but **there is no proximity path
for a Tier-2 ZK proof**"; **addendum D1.4** attaches `EE-ZKP-045a` to that sentence.
**Spec, `EE-CNF-006`:** presupposes a proximity Tier 2 presentation and makes it optional pending
measurement; the EE-PROX profile carries it. **Spec, `EE-ZKP-045a (SHALL)`:** "A transport-budget
failure SHALL NOT override `EE-ZKP-055`. Where the relying party's registration certificate
asserts ZKP capability, the wallet SHALL, on a failure or a prior measurement under `EE-ZKP-045`,
**retry over Tier 2 — on another transport where both ends offer one — or refuse the
presentation, and SHALL NOT fall back to Tier 1**."
`EE-ZKP-045a` presumes the proximity Tier 2 path exists and forbids falling back to Tier 1; it is
evidence for the PRD's "refused outright" behaviour, not for the path's absence. The spec's own
record (`docs/CONFORMITY.md`) states the correct position for that identifier: "Not applicable
today: no proximity Tier 2 path and no recorded pair measurement. Binds the first proximity ZK
code."

### M-5. `FR-6` states a refusal where `EE-ZKP-055a` requires a Tier-2 presentation
**PRD, `FR-6`:** "A DC API or proximity age request that is **unauthenticated** (no `readerAuth`
chaining to a trusted access CA) **or** carries no satisfiable ZK request **is refused before
consent**."
**Spec, `EE-ZKP-055a (SHALL)`:** "Where a request … is not authenticated by a registration
certificate … **and the device is ZKP-capable, the wallet SHALL present over Tier 2 only, and
SHALL refuse a request that admits only a plain-mdoc response.** The absence of authentication
SHALL be treated as the least-trusted case, never as the absence of a ZKP requirement."
An unauthenticated request that a proof *can* satisfy must be answered over Tier 2; only one that
admits solely a plain-mdoc response is refused. The PRD's bullet collapses the two triggers with
"or". The PRD's `R7` states the requirement correctly, so this is a wording defect with a
consequence.

### M-6. A per-presentation registry/register fetch is designed in where the spec treats it as a privacy violation
**PRD, `UJ-3` edge case:** "the circuit registry is unreachable; the verifier fails closed and says
which lookup failed". **PRD, `PG-7`:** "**The register sits on the critical path and that is a
known cost** … a bounded cache and an explicit, logged stale decision."
**Spec, `EE-ZKP-032 (SHALL)`:** "Relying parties SHALL **cache the registry and SHALL NOT fetch it
per presentation. A per-presentation fetch is a phone-home and violates `ZKP_07`.**"
**Spec, `EE-RP-004 (SHOULD, [REC])`:** "Estonia SHOULD issue Registration Certificates … rather
than relying solely on dynamic register lookup … dynamic lookup at presentation time
**reintroduces a phone-home**. Registration certificates are optional under CIR 2025/848; choosing
them is a privacy decision, not a convenience one." (Spec §22 divergence 3 records the same
position.)
The PRD presents the lookup as an availability/cost problem and never notes that the spec's
mechanism of choice — read entitlement from the registration certificate — removes it, nor that
the same shape of fetch is prohibited for the circuit registry. `EE-ZKP-032` and `EE-ZKP-033`
([REC] 30-day deprecation) are also absent; `R4`'s mitigation ("ship it as a generator plus a
hosted instance") has the product hosting what `EE-ZKP-030 (SHALL, [REC])` assigns to RIA and
what `EE-ZKP-031` requires to be signed, append-only and CT-logged — per spec §0.1 a wallet "is
neither conformant nor non-conformant to a `[REC]` clause", so this must be stated as a design
choice, not a conformance question.

### M-7. `FR-4`/`NG-9` cite the ISO permission and omit the spec's own condition
**PRD, `FR-4`:** "A holder can complete a presentation only when the request names **exactly one**
age predicate…" **Out of Scope:** "ISO/IEC 18013-5 §7.2.5 permits requesting several thresholds …
this product is deliberately stricter until a registered intended use can authorise otherwise."
**Spec, `EE-POA-030 (SHALL)`:** "A query naming more than one predicate SHALL be refused **unless
the relying party's registered intended use names each of them**, and where it does, the wallet
SHALL tell the user before approval what age range the combination reveals."
The PRD is substantively correct and self-declared as stricter, but it attributes the permission
to ISO/IEC 18013-5 and never states that the specification itself carries the carve-out and the
accompanying disclosure duty — the reader cannot tell whether the PRD's rule is the spec's or
stricter than it.

### M-8. Spec §23 open items treated as settled
**PRD, `FR-9`:** "Keys are generated through the platform secure area — StrongBox where
`FEATURE_STRONGBOX_KEYSTORE` is advertised, TEE otherwise" — stated as shipped behaviour with no
pointer to **§23 item 26**: "Android **StrongBox key-generation throughput and key-slot capacity**
… on the reference device class, for the batch of ≥30 required by `EE-POA-011` … **No
key-generation time and no slot capacity were measured** … The item stays open, `PENDING-DEVICE`."
(`§6.2` does say no StrongBox timing is asserted and no device row leaves `PENDING-DEVICE`, so the
gap is the citation and the link to C-1, not the substance.) Two further §23 items bearing on PRD
claims are unmentioned: **item 13** ("Estonian language-share data for `EE-ACC-004`", cf. H-5) and
**item 31** ("Whether Estonian access certificates carry a `dNSName` subject alternative name
against which `response_uri` and `expected_origins` can be checked (`EE-PRO-003`,
`EE-PRO-013`)", cf. H-3 and `OQ-3`, which covers access certificates but not this binding).

### M-9. Four spec-chosen numbers are absent, and the two the PRD keeps lose their provenance
The specification is careful to mark its own choices: `EE-PRO-026` — "The 3 minutes are **this
specification's choice**: OpenID4VP 1.0 sets no lifetime for a `request_uri`"; `EE-PRO-023` —
"this document sets **10 minutes** as its own choice, since neither OpenID4VCI 1.0 nor HAIP 1.0
fixes a figure"; `EE-POA-026` — "The **7-day epoch is this specification's choice**: the AV
specification requires only that `ValidityInfo` timestamps be set 'with a precision that limits
the linkability information' and names no granularity"; `EE-ZKP-062` — validity judged on the
relying party's clock "±60 s" (spec-chosen). `EE-POA-016` (3-month validity and re-identification)
is the fifth.
The PRD carries the 7-day epoch (`FR-8`) and the 14-day lead (`FR-8`) with no attribution, and
drops the other four entirely — including `EE-PRO-026`, which the record's own conformity note
tracks ("Reference verifier: 3-minute default session TTL … Single-use and the wallet's 'not
answered' notice not assessed"), and `EE-ZKP-062` ("Not assessed: no wallet-side expiry gate was
found on the presentation path"). No PRD sentence mis-ascribes any of these to EU law; the finding
is that a conformance reader cannot tell which numbers the spec chose from which the AV Profile or
an implementing act requires.

### M-10. `EE-PRO-003`'s `response_uri` binding is stated nowhere, and the verifier contradicts it
Covered inside H-3 as a dropped SHALL; recorded separately because the PRD's `FR-3`/`FR-21` make
the Go verifier a shipped surface whose recorded default (`x509_san_dns:verifier.example.ee`)
`EE-PRO-003` no longer permits, while spec §23 item 31 leaves the `dNSName` half open. Either the
PRD carries the requirement and the deviation, or the verifier's default is changed; today neither
is written down.

---

## Low

- **L-1.** §3 Glossary: "**EE-PoA Provider** — … the binding target of **most** `EE-POA-*`
  requirements." The spec's own split is issuer-only 15 / wallet-only 12 / mixed 4 of 35; the
  Provider is the largest single target, not a majority, and 12+ requirements (including the
  batch-management and last-attestation rules `FR-8` rests on) bind the wallet.
- **L-2.** `EE-POA-019 (SHALL)` ("ISO/IEC 18013-5 **server retrieval SHALL NOT be used** for any
  Estonian attestation") and `EE-PRO-011 (SHALL)` ("Proximity presentation SHALL follow ISO/IEC
  18013-5 **device retrieval**, and SHALL NOT use server retrieval") are absent even though `FR-2`
  and the proximity surface are in scope.
- **L-3.** `EE-ZKP-002 (SHALL)` — "The wallet SHALL NOT claim … that presentations are unlinkable
  by the attestation provider unless the ZKP path is enabled **and** the relevant relying parties
  can verify it … This specification treats it as a conformance failure, not a roadmap item" — is
  the requirement closest to the PRD's `FR-42`, and is uncited. `EE-ZKP-001` (implement a ZKP path
  for EE-PoA and the AV attestation), `EE-ZKP-020`/`022a`/`022b`/`022c` (Track A binding and the
  125-bit rule), `EE-ZKP-042`/`045` (Tier 1 fallback and its plain-language notice),
  `EE-ZKP-060` (cryptographic inventory including quantum exposure) and `EE-ZKP-024`/[REC]
  (validate against a second, independently written verifier — cf. `FR-19`'s single external
  implementation) are likewise absent from a 38-requirement family with 3 citations.
- **L-4.** The PRD's instrument list (§10) omits **Directive (EU) 2019/882**, which `EE-ACC-001`
  makes the legal basis of the accessibility claim (see H-5), and the specification's §22
  divergence list is not carried anywhere, although `EE-PRO-007` and `EE-PRO-010a` both record
  deliberate divergences from the EU baseline that a conformance reader will ask about.
- **L-5.** §2.2 calls `spec/EE-EUDIW-TS-1.0.md` "the fork's spec" and "explicitly optional in the
  fork's spec". The specification is an independent document in this repository whose own §0.1
  states it "is not issued by any Estonian authority"; §23 item 24 is the condition on which
  proximity Tier 2 is optional, and the PRD drops it.

---

## Appendix A — identifiers the PRD and addendum never cite, by family

Machine-derived from the specification's requirement headings (`**EE-* (`) and from `prd.md` +
`addendum.md`. Families marked **in scope** carry findings above or are named in the PRD's own
scope statements.

| Family | Coverage | Uncited |
|---|---|---|
| EE-ZKP (38) | 3 | 001, 002, 003, 004, 010, 011, 020, 021, 022, 022a, 022b, 022c, 023, 024, 025, 030, 031, 032, 033, 040, 041, 042, 043, 044, 045, 046, 050, 052, 053, 054, 055, 056, 060, 061, 062 |
| EE-POA (35) | 2 | 001, 002, 003, 004, 005, 005a, 008, 010, 011, 011a, 011b, 011c, 012, 013, 014, 015, 016, 017, 018, 019, 020, 021, 022, 023, 024, 025, 026, 027, 028, 029, 030, 031, 032 |
| EE-PRO (20) | 1 (misattributed) | 001, 002, 003, 004, 005, 006, 007, 010, 011, 012, 013, 014, 020, 021, 022, 023, 024, 025, 026 |
| EE-PRI (18) | 0 | 001, 010–024 (all 18) |
| EE-SEC (18) | 0 | all — outside the PRD's scope; `EE-SEC-001a/b/c` (remote-HSM per-presentation visibility) touch the PRD's WSCD glossary entry |
| EE-SIG (16) | 0 | all — signing is out of the fork's scope |
| EE-GOV (14) | 0 | all — `EE-GOV-030` (coexist with ID-card/Mobile-ID/Smart-ID), `EE-GOV-031` (Mobile-ID SIM replacement by 19 May 2027) and `EE-GOV-032` (TARA/GovSSO extension) touch the PRD's §12 rows |
| EE-ONB (11) | 0 | all — onboarding is out of the fork's scope |
| EE-PID (9) | 0 | all — `EE-PID-007` (`isikukood` non-correlation) and `EE-PID-008` (age-check-only RPs request no PID attribute) are the spec's form of the PRD's `PG-1`; `EE-PID-003` (PID SHALL NOT be presented to satisfy an age check) is uncited |
| EE-WUA (8) | 1 | 001–007 |
| EE-CNF (7) | 3 | 001, 002 (SHALLs — H-8), 003, 004 ([REC]) |
| EE-REV (7) | 0 | all — `EE-REV-003` (no status mechanism for EE-PoA) is the spec's form of `NG-11` |
| EE-RP (6) | 0 | 001, 002, 003, 005, 006 (**in scope** — H-6); 004 ([REC]) |
| EE-ACC (4) | 0 | 001–004 (**in scope** — H-5) |

## Appendix B — `[REC]` requirements in the PRD's sight

Spec §0.1: "A requirement, or a clause of one, whose addressee is RIA acting as an authority, the
Registrar, the scheme owner … is therefore a **recommendation to that authority**, marked `[REC]`
… a wallet is neither conformant nor non-conformant to a `[REC]` clause." The PRD never restates
an authority-bound `[REC]` requirement as a product obligation, which is correct; the one place it
runs the other way is `FR-13`/`R4`, where the product would host a registry that
`EE-ZKP-030`/`031`/`033` (all `[REC]`) assign to RIA (M-6). The authority-bound `[REC]`
requirements the PRD's own dependencies touch and does not name:
`EE-ZKP-030`/`031`/`033` (registry, its properties, 30-day deprecation),
`EE-ZKP-052` (Registrar clause: require ZKP support of age-check-only registrants),
`EE-ZKP-054` (Registrar clause: ZKP capability as an attested registration attribute),
`EE-RP-001` (Registrar implements TS5/TS6 and publishes current *and historic* access
certificates), `EE-RP-004` (registration certificates over dynamic lookup — M-6),
`EE-PRI-001` (retiring `isikukood` as a proof), `EE-CNF-003` (test-bed participation),
`EE-CNF-004` (Track A verifier contributed to the APTITUDE pilot, "the cheapest available route to
third-party validation of an unusual component" — the aim of the PRD's `FR-19`), `EE-POA-005a`,
`EE-GOV-032` (Estonia clause).

## Appendix C — what would close this

1. Add a short "spec conformance position" table to the PRD: per family, the identifiers the PRD
   relies on, the ones it deliberately does not carry, and the ones it fails — with `FR-8`
   restated per C-1/H-1/H-2 and `H-7` corrected against the record.
2. Add the divergence list (spec §22) and the open-item list (spec §23) the PRD's claims sit on,
   which is what turns `FR-38`'s "statuses derived from tests" into a spec-facing check.
3. Fix the two identifier errors (H-3, M-3) and the two modal/behaviour statements (C-2, M-5).
4. Carry the missed conformance and accessibility SHALLs (`EE-CNF-001`/`002`, `EE-ACC-001`) or
   declare each unmet, in the same voice the PRD already uses for `EE-WUA-008` and the 1.0 s
   budget.
