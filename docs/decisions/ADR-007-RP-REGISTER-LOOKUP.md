# ADR-007 — The RP-register lookup is one fail-closed interface with a bounded, published cache age

**Status:** accepted, 23 October 2026. Records the decision Stories 3.3/3.4
(FR11/FR12) defer to this record. The RP-side counterpart of ADR-005
(wallet side).

## Context

A relying party must request only what it is registered for. The Estonian
Relying Party Register has no published API and the access-certificate
approach is still in design (PRD OQ-3) — yet the RP kit must already behave
lawfully, and the fork must publish an interface proposal (design D3) so
the interface is on the table before procurement is awarded. A service that
does not exist yet cannot silently become an allow.

## Decision

**The interface.** One Go interface in the RP kit's package surface:

```go
// RPRegister resolves what a relying party is registered to request.
type RPRegister interface {
    IntendedUse(rp RegisterIdentity) (RegisteredIntendedUse, error)
}
```

The Go `RegisteredIntendedUse` mirrors ADR-005's Kotlin model
field-for-field (tradeName, uniqueIdentifier, serviceIdentifier,
memberState, intendedUses, privacyPolicyUrl) — the published proposal
(Story 3.4) presents **one** model on both sides of the wallet/RP protocol,
not two vocabularies to reconcile later.

**Three refusal states, all fail-closed and distinct:**

| Error | Cause | Behaviour |
|---|---|---|
| `ErrNoImplementation` | no register implementation configured | refuse the presentation; log the missing prerequisite by name |
| `ErrRegisterUnreachable` | configured but timing out / 5xx / DNS | refuse; log the register error — distinct from unconfigured |
| `ErrNotFound` | the register answers: no such relying party | refuse |

A refusal **never** requires a cache hit: every refusal path holds with or
without cached data. The cache exists to serve allows cheaply, never to
rescue a failing lookup into an allow.

**Cache.** Bounded, per register identity, TTL **15 minutes by default**
and configurable; the deployed value is **published in the interface
contract** and in the proposal. Semantics, stated as the contract's
explicit stale-data decision:

- An entry **within** TTL may serve an allow without a network round trip —
  including while the register is unreachable, and that fact is **logged**
  (`register-unreachable, cache-hit-within-ttl`) so the allow is visible,
  never silent. This is the precedence Story 3.3's "unreachable … refuses"
  clause reads *with*: an unreachable register refuses **absent a
  within-TTL cached allow** — the qualifier lives here, in the contract,
  so the story's wording and this ADR cannot be implemented apart.
- An entry **past** TTL is discarded and the lookup re-attempted; beyond-TTL
  data is treated as **absent** — the refusal path — never served as a
  silent allow.

The age bound is the register-side analogue of `EE-REV-007` (a relying
party publishes a maximum cache age and fails closed beyond it); the
register is not a status list, so the requirement itself does not bind —
the *design rule* is adopted, and the proposal says so.

**The mock and the proposal.** A file-backed mock satisfies the interface
and registers the **three positively-tested** acceptance-case relying
parties (Story 3.5); the unregistered-RP case uses an identity the mock
deliberately omits, so `ErrNotFound` is exercised by *absence* — not by a
fixture marked "unregistered" that a later refactor could silently
promote. The mock's fixture corpus is the same one Story 4.6's certificate
generator emits (ADR-005's cross-side consistency rule), so the wallet and
the RP kit can never test against disagreeing registrations. The interface
proposal (Story 3.4) is published even though RIA has no API and the
certificate approach is still in design, and records that it is useful
even if superseded — an interface on the table is worth more than a
perfect one reconstructed after procurement.

## Consequences

- Story 3.3 implements the interface, mock and refusal semantics; 3.4
  writes the proposal from this contract; 3.5's unregistered-RP case and
  6.5's latency profile exercise the cache decisions.
- When RIA publishes an API, it slots in as an `RPRegister` implementation —
  no other component changes. That is the pluggability Story 3.3 asked for.
- The 15-minute default is a figure the fork can defend (latency win,
  bounded staleness); the contract makes it a published tunable, not a
  hidden constant.

## Alternatives rejected

- **Serve stale data indefinitely with a logged warning:** a silent allow
  wearing a warning badge — exactly what Story 3.3's "never a silent allow"
  forbids.
- **No cache (always hit the register):** honest but makes every
  presentation pay the register's latency and couples availability to a
  service that (today) does not exist; FR30/Story 6.5 exist because the
  lookup is on the request path.
- **Cache refusal states too:** an unreachable register would then refuse
  *forever* after one failure — availability damage with no privacy gain,
  and it blurs the distinct states the interface exists to keep distinct.
- **A full ARF-shaped registration object instead of the six fields:**
  over-commits to a moving profile (OQ-3); the proposal stays minimal and
  version-pinned like ADR-005.
