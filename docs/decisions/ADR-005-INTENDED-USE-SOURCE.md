# ADR-005 — `IntendedUseSource` is a fail-closed interface; the certificate model is typed and version-pinned

**Status:** accepted, 23 October 2026. Records the decision Story 4.1 (FR16)
deferred to this record, and fixes the data model Story 4.2 (FR17) parses
into. The wallet-side counterpart of ADR-007 (RP side).

## Context

`AgePredicatePolicy`
(`app/src/main/kotlin/ee/cyber/wallet/domain/presentation/AgePredicatePolicy.kt`)
refuses any request naming more than one `age_over_NN` predicate, because
the wallet cannot read the relying party's registration. That blanket
refusal is stricter than ISO/IEC 18013-5 §7.2.5 and is recorded as a
divergence (spec §22 item 21, `EE-POA-030`). Epic 4 replaces it with a
lawful decision driven by the relying party's registration/access
certificate — introduced **interface-first**, so a fail-closed default
exists before any parsing does.

The certificate profile is still moving: the ARF annexes are in flux and RIA
has no published register API (PRD OQ-3). Whatever lands, the wallet must
never loosen a privacy control on a parse error.

## Decision

**The interface.** `IntendedUseSource` lives in
`ee.cyber.wallet.domain.presentation`, next to the policy that consumes
it. One operation: resolve the intended use for the relying party
authenticating the request. The return type is a **sealed, fail-closed
result**, not a nullable: the two failure states are distinct and both
refuse.

| State | Behaviour |
|---|---|
| no source configured | refuse, reason `intended-use-source-absent` — today's blanket refusal, now with a name |
| source present but throws / times out | refuse under the same default, reason `source-error` logged — distinct from absent (Story 4.1) |
| resolved use names every requested predicate | the policy may allow (Story 4.3) |

`AgePredicatePolicy` consumes the interface; its single-predicate and
held-false rules (`EE-POA-030`/`031`) are unchanged and bind *after* the
source decision — a certificate can widen which requests are askable, never
what is disclosed about a predicate the wallet holds false.

**The data model.** `RegisteredIntendedUse` (Story 4.2) is typed:

```
RegisteredIntendedUse(
  tradeName: String,            // displayed per EE-PRO-010
  uniqueIdentifier: String,     // the RP's registered unique id
  serviceIdentifier: String,   // the specific registered service
  memberState: String,         // ISO 3166-1 alpha-2
  intendedUses: Set<DoctypePredicate>,  // closed vocabulary: (doctype, age_over_NN) pairs,
                                       // doctype-qualified to match the wallet's credential
                                       // attribute model — never a bare field-name match
  privacyPolicyUrl: String?,    // absent → explicit absent marker, never blank
)
```

An unparseable certificate yields refusal rather than a partial value. A
parseable certificate naming no use refuses (FR20) — an empty
`intendedUses` is a refusal, not a vacuous allow.

**The decision's granularity is per request.** `EE-POA-030` bounds a
*presentation*: a request naming `age_over_18` on one credential and
`age_over_21` on another is two predicates at request level and needs the
certificate to name both. `AgePredicatePolicy.decide`'s per-credential
shape is an implementation detail, not the rule; the source decision runs
once per request, across every matched credential.

**No wallet-side cache.** The certificate travels *with* the request
(`readerAuth` chaining to a trusted access CA, `EE-PRO-025`); it is parsed
per presentation and nothing is retained. The bounded-cache decision is
therefore: bound = one request lifetime. This keeps the wallet free of a
store that could be subpoenaed and matches `EE-PRI-023`'s spirit. (The
RP-side register cache is ADR-007's problem, not this one.) The Topic 19
/ `DASH_03` log fields (Story 4.4) are **snapshotted into the log record
at parse time** — the snapshot, not the certificate, is what survives the
request, so "nothing is retained" and "fields populated from the
certificate" are one rule, not two.

**Cross-side consistency.** The certificate the wallet parses and the
register entry the relying party resolves through (ADR-007) describe the
**same registration**: when a certificate's `intendedUses` disagrees with
the register entry for the same RP identity, the wallet refuses with a
named reason (`intended-use-mismatch`) — never the wider of the two.
ADR-007's file-backed mock and Story 4.6's fixture generator share one
fixture corpus, so both epics test against the same data and cannot
introduce a mismatch by testing against different values.

**Version discipline.** The parser targets the ARF access-certificate
topics as of this repository's pinned `eudi-arf` submodule revision,
recorded as an in-repo, version-labelled profile document (`NFR4`). The
compared identifier is **the profile document's version label** (e.g.
`access-cert-profile/1`) — one identifier, used both as the parser's
target and as the field a certificate claims; it is not the submodule
revision itself, which would give Story 4.6's fixture nothing comparable
to exceed. A certificate claiming a **newer** label than the parser
targets is refused with `version-unsupported` — never partially parsed as
the older profile (Story 4.2), and Story 4.6's newer-version fixture
asserts that the refusal actually fires. Older labels parse if the
in-repo profile says they are compatible; otherwise the same refusal.
When the ARF moves, the profile's version label moves with it and the
drift is a visible maintenance cost, not a silent behaviour change.

**Ordering.** The fail-closed default (Story 4.1) and the property tests
(Story 4.5) land **before** the parsing they guard (Story 4.6's sequencing):
the guards exist when the code they guard arrives.

## Consequences

- Epic 4's stories implement against a fixed contract: Story 4.1 the
  interface, 4.2 the parser, 4.3 the two-predicate decision, 4.4 the Topic
  19 log fields, 4.5/4.6 the property tests and fixtures.
- The blanket refusal becomes the *default* rather than the *rule*; the
  lawful two-predicate case is reachable only through a resolved,
  certificate-named use (closing PRD OQ-16's product question for the
  wallet side).
- The closed `intendedUses` vocabulary means a new predicate family is a
  scope decision (NFR16/backlog), not a string the parser happens to
  accept.
- Holding `uniqueIdentifier` and `serviceIdentifier` distinct keeps the
  Topic 19 / `DASH_03` log fields (Story 4.4) attributable to the right
  entity, and no attribute value ever enters the log.

## Alternatives rejected

- **Look the intended use up in a register from the wallet:** a network
  lookup per presentation leaks presentation timing to a third party and
  creates a wallet-side store; the certificate already arrives
  authenticated with the request.
- **A nullable/optional return with a null-means-refuse convention:** one
  careless `?:` inverts the fail-closed default; a sealed result makes the
  refusal states the only non-happy path the compiler allows.
- **Parsing the certificate inline in the policy:** couples the privacy
  control to a moving profile format — the interface exists so the Estonian
  register or an EU registry can be plugged in without redesign (Story
  4.1's stated goal).
- **Tolerating unknown future profile versions by best-effort parsing:**
  exactly the silent-loosening path FR20 exists to prevent.
