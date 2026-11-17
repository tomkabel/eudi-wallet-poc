# ADR-008 — DC API is the mandated ZK path; the OpenID4VP carrier is profiled-but-unofficial

**Status:** accepted, 23 October 2026. Turns PRD OQ-6's recommendation into
an architectural rule with a named enforcement point.

## Context

The EU AV Profile carries ZK only over the Digital Credentials API path;
no standard DCQL query for ZK exists over OpenID4VP, where the profile
expects a plain mDoc. This fork, meanwhile, profiles the OpenID4VP
`mso_mdoc_zk` carrier as its primary conformance and e2e surface
(`docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`). Both surfaces are real
and both are maintained — but a reader who cannot tell which is mandated
could infer EU endorsement of the second, which does not exist.

## Decision

**The rule.** In every artefact this repository publishes, the ZK path
over the **Digital Credentials API** is the **mandated** path — it is the
only path on which the AV Profile carries a ZK, and the only path on which
`EE-AGE-2` may be certified (`EE-CNF-006`). The **OpenID4VP `mso_mdoc_zk`
carrier** is **profiled-but-unofficial**: first-class in this fork's
conformance suite, e2e spine and fixtures, carrying no EU endorsement. Both
are maintained; neither is dropped (OQ-6's recommendation, adopted).

**Enforcement points, named.** The rule is enforced where a reader meets
the work, so it cannot be missed by accident:

1. **The profile of record** — `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md`
   states its own placement ("profiled-but-unofficial; the mandated ZK path
   is the DC API") in its header, and every carrier profile document
   published after this ADR does the same.
2. **The two conformance runners label every report.** The AV conformance
   runner (Story 1.3) runs only over the DC API path and labels its output
   `path: dc-api (AV Profile mandated)`. The carrier conformance runner
   (Epic 2) labels its output `path: openid4vp (profiled, unofficial)`. A
   report without a path label fails its own review (Story 7.4's
   vocabulary pass).
3. **The audit trail carries the machine-readable tag** — `Carrier.Name()`
   identifies which carrier served each presentation in logs and the
   provenance ring (ADR-006), so a path question is answered from data, not
   from a document's current wording.

**What this does not change.** The carrier layer's engineering invariants
are untouched: `carrier.go` remains the single adaptation point,
`TranscriptForFlow` the single transcript entry point, and the interim
JSON envelope remains a versioned stand-in, explicitly not an
interoperability claim. The rule governs *labelling and claims*, not the
code's shape.

## Consequences

- Stories 1.3, 2.1 and 7.4 each carry a labelling criterion this ADR makes
  checkable: an unlabelled path claim in a published artefact is a defect.
- `docs/CONFORMITY.md` rows that speak of a ZK presentation state the path
  they cover (this is part of Story 7.2's audit of hand-maintained rows).
- If a future ARF/TS13 edition mandates ZK over OpenID4VP, this ADR's rule
  inverts by a new ADR — the labels move, the enforcement points stay.

## Alternatives rejected

- **Drop the OpenID4VP carrier** (make the fork DC-API-only): abandons the
  fork's own conformance spine and e2e surface, and severs the only path
  where two independent implementations currently interoperate.
- **Promote the OpenID4VP carrier to co-mandated:** a claim no EU instrument
  supports; the whole point of the rule is to prevent that inference.
- **State the placement once, in this ADR, and move on:** the handoff's
  question exists precisely because a single statement in one document
  does not travel; the enforcement points are what make the rule survive
  artefact drift.
