# ADR-006 — The circuit list is one signed JSON document, Ed25519 over JCS, with an in-memory provenance ring

**Status:** accepted, 23 October 2026. Records the decision Story 5.1 (FR21)
deferred to this record, and fixes the fetch/validate semantics Stories 5.2,
5.3 and 5.5 implement.

## Context

The AV Profile defers the accepted circuit set to a "scheme owner" it does
not name (PRD OQ-2). Today every operator compiles its own local allowlist:
`verifier/circuits.json` — unsigned, commented, its own `_comment` saying
"in production this file is signed, append-only and CT-logged; this is a
local stand-in". When operators do that independently, two parties accept
different circuits while both believing they follow the same profile. The
circuit allowlist is a trust root (`EE-ZKP-023`/`030`): whoever controls it
controls what proofs verify.

The existing keying must be preserved (backlog "Additional Requirements"):
entries key on `id` + `circuit_hash` + params, the spec id being
`longfellow-libzk-v1_{version}_{num_attributes}_{block_enc_hash}_{block_enc_sig}_{hash}`
(`verifier/go/circuits/registry.go:54-57`), and the allowlist accepts both
`longfellow-libzk-v1` and `org.iso.mdoc.zk` labels (`dcql.go:21-22`).

## Decision

**Format.** One JSON document:

```
{
  "schema": "ee-circuits/1",
  "list_version": "<monotonic integer>",
  "issued_at": "<RFC 3339>",
  "expires_at": "<RFC 3339>",
  "min_library_version": "<Longfellow revision the entries were built/audited against>",
  "maintainer": "<who published this list>",
  "circuits": [ <Circuit …> ]
  "signatures": [ { "key_id": "<…>", "sig": "<base64url>" } ]
}
```

`circuits[]` is **exactly** the `verifier/go/circuits` `Circuit` struct
(`circuit_hash`, `version`, `num_attributes`, `block_enc_hash`,
`block_enc_sig`, `upstream_tag`, `accepted_on`, `deprecated_on`, `audits`) —
the keying, the tuple-uniqueness load errors and the deprecation transition
of `registry.go` are inherited verbatim, not re-expressed. Per-entry
identity is derived via `Circuit.SpecID()`; no new `id` or per-entry
`system` member is added. The **system labels the document accepts** —
both `longfellow-libzk-v1` and `org.iso.mdoc.zk`, matching
`zkAcceptedSystems` (`oid4vp/dcql.go`) — are a **document-level member**
(`systems: [...]`), which is where Story 5.1's `system` field is
satisfied.

**Signature.** Ed25519 over the RFC 8785 (JCS) canonicalization of the
document minus the `signatures` member. One signing key; the array is
multi-key ready for a future scheme owner without a format change. The
verifier pins the public key (`-circuit-list-key`, file) and fetches the
document from a configured URL (`-circuit-list-url`).

**The local fallback is the same format, and the same artifact.** The
pinned in-repo list extends today's `circuits.json` with the envelope
fields — and the wallet consumes the **same signed document** as a
generated build asset, with a drift test binding that asset to the
document's `list_version`, so when the network is absent, wallet and
verifier fall back to *the same pinned document version* — not two
independently-bundled sets that happen to resemble each other (Story 5.3's
deadlock case, exercised as Story 3.5's fifth acceptance case). The
offline rule is stated once for both sides: no acceptable circuit in
the pinned list either → refuse the ZK path, naming the missing
prerequisite.

**Refusals, each named.** No network and no local list → refuse, missing
prerequisite named. Flipped byte → refuse (Story 5.4's tamper test).
Signature-valid but past `expires_at` → refuse `stale-list`, never continue
accepting withdrawn circuits — and `expires_at` **binds the pinned fallback
like any fetched document**: an aged pinned file refuses `stale-list`,
which is why regenerating the pinned document is a release-cadence duty
and Story 5.6's succession path states what an ownerless period does when
it ages. An entry whose `min_library_version` exceeds the consumer's
linked runtime revision → refuse with that reason, even with valid
signature and hash. Unknown key → refuse. "Acceptable circuit" is
**defined once, for both sides**: an entry of a signature-valid,
unexpired, pinned-or-fetched document whose `min_library_version` is
≤ the consumer's runtime stamp — the verifier's build-time
`longfellow-rev.txt` stamp (`main.go` `-ldflags`) on the Go side, the
bundled `multipaz-longfellow` artifact version as the wallet's stamp —
so a circuit the verifier accepts and the wallet refuses (or the reverse)
is a stated stamp mismatch, not an unexplained refusal.

**Audit provenance (Story 5.5).** An **in-memory bounded ring** in the
verifier, capacity aligned with the session store bound (`maxSessions`,
10,000), **keyed by session id**, one entry per session aggregating that
session's verified credentials — `{carrier (the `Carrier.Name()` tag),
circuit SpecIDs, registry list_version, decided_at}` — so the multi-
credential case answers with one record, not an ambiguous one, and the
carrier tag ADR-008's third enforcement point relies on is part of the
record, not an afterthought. Retention boundary = **process lifetime**:
an aged-out, unknown or refused id returns a distinguished not-found —
never an empty or inferred provenance — and the retention boundary itself
is tested. Nothing is written to disk, preserving the store's stated
position that a verifier keeping persistent presentation records is a
verifier that can be subpoenaed for them.

**Generator.** An in-repo Go CLI (alongside `circuits`, so it shares the
struct and the load errors) that reads a circuits source file plus the
signing key and emits the signed document — maintaining a list stays cheap
(Story 5.1's "the generator stays in-repo").

**The ownership gap, stated honestly.** While no scheme owner is named
(OQ-2), the list is published as **community-maintained**: `maintainer`
names the publisher, the signing key is this repository's release key, and
**no artefact claims or implies scheme-owner authority**. Succession is
documented per Story 5.6: generator plus hosted instance plus a named path
if no owner adopts it, with the local pinned list as the ownerless-period
fallback. A scheme owner adopting the format replaces the pinned key; the
format does not change.

## Consequences

- Story 5.2 implements fetch + pinned-key validation against this format;
  5.3 the wallet-side resolution against the same document; 5.4 the tamper
  and two-instance agreement tests; 5.5 the provenance query and retention
  boundary.
- `circuits.json`'s envelope upgrade is backward-compatible in construction
  but the old bare shape stops being the operator contract; the migration
  is one generator run.
- The provenance ring gives auditors "which circuit proved this?" within
  the process's lifetime and honestly declines it beyond — the boundary is
  a stated property, not a gap.
- Ed25519 + JCS keeps the verification story small and reproducible by a
  third party (NFR8): one JCS canonicalization, one Ed25519 verify, and a
  JSON file a reviewer can read — with no CBOR tooling on the JSON path
  (and note: `jq` reads the document but does not implement JCS
  number serialization; the canonicalization step is code, not `jq`).

## Alternatives rejected

- **COSE_Sign1 over a CBOR list:** drags a CBOR signature stack into a JSON
  document ecosystem and complicates the generator for no consumer.
- **X.509 chain / CMS:** implies a certification authority nobody holds —
  manufacturing the exact authority the OQ-2 gap says does not exist.
- **Keeping per-operator allowlists with a documented merge convention:**
  the status quo this epic exists to end; a convention is not a
  checkable document.
- **Appending provenance to a persistent log file:** converts the verifier
  into a subpoena-able presentation record keeper, against the session
  store's own stated design.
