# Addendum — EE-EUDIW ZK Age-Verification Wallet PRD

Material that belongs to a downstream document (architecture, solution design, UX spec) or
that earned a place here but does not fit the PRD's own narrative. Nothing here is a PRD
requirement; where the PRD needs a fact from this file, it states that fact there.

---

## D1. Options considered and rejected

The PRD's shape was decided through three scored rubrics, each of which escalated before it
was asked. Scores, bands and separations are in `.decisions.log` beside this file; the
rejected alternatives and the reason each lost are here, because a reviewer asking "why is
the Vision framed this way" should get an answer rather than a reconstruction.

### D1.1 Entry point — section order

Chosen: **hybrid — capability-first spine with journeys captured where a real session
exists.** Rejected: capability-first with no UJ section (loses the two journeys a real user
actually runs: the visitor's six-step sandbox flow and the integrator's first verification);
journey-led (inverts the document for an assessor-facing product where four of five
components are not UI). The rubric's separation was *marginal* and the order flipped when
the hybrid's journeys were rated one band lower, which is why §2.3 keeps each journey
anchored to a documented flow rather than an invented one.

### D1.2 Vision centre of gravity

Chosen: **evidence-first — the product is the assessable evidence estate.** Rejected:
Estonian relying-party enablement as the centre (the strongest argument against the choice:
the relying-party kit is "the only recommendation a buyer can act on before 2028"); standards
influence as the centre (the record ranks it below both, and the fork's own components are
not built for it). The decision rested on the record's own language — "a conformance-test and
packaging gap, not a cryptographic redesign"; "the distinctive artefact here is the verifying
side plus its refusals"; "a research artefact whose value is its evidence, not its
throughput" — and on the fact that the fork's public front door ships no APK.

### D1.3 v1 scope

Chosen: **the innovation assessment's initial release — O1, O5, O2 — with phases D1 and D3
folded in.** Rejected: the roadmap's M-phase alone, which would defer the relying-party kit
(the roadmap's own sequencing rationale argues for exactly this order, because P1's four
acceptance cases depend on M1's fixtures — recorded as the steelman for the rejected
option); and the pilot phases P2/P3, which would put a registration-certificate policy and a
circuit registry into a release whose value is evidence rather than capability.

### D1.4 Not carried as a user journey

The record's use case U3 (offline/proximity age proof at a venue) is a non-goal rather than
a journey: the wallet has a proximity presentation screen
(`app/src/main/kotlin/ee/cyber/wallet/ui/screens/proximity/`), but there is no proximity path
for a Tier-2 proof (`EE-ZKP-045a`), the BLE transfer budget for a ~360 KB proof is
unmeasured, and on a ZK-capable device an age request over the proximity path is refused
outright (`docs/CONFORMITY.md`). A journey whose climax is a refusal is a real journey, but
it is the *refusal* being advertised, not a proof, and it belongs in the open question at
§18/OQ-11 rather than in §2.3. An "assessor journey" was likewise excluded: the reproduction
workflow is real, but it belongs in success metrics and NFR-8, not in a user journey.

---

## D2. Transport and mechanism detail

Technical-how for the architecture pass. The PRD states capabilities; this states the wire.

- **OpenID4VP carrier.** DCQL with the format identifier `mso_mdoc_zk`, a `meta.zk_system_type`
  registry, and a `ZkSystemSpec` parameter tuple of `circuit_hash`, `version`,
  `num_attributes`, `block_enc_hash`, `block_enc_sig`. The response is `direct_post.jwt`:
  direct `ECDH-ES` under P-256 with `A128GCM` or `A256GCM`, encrypted to the relying party's
  published key. The carrier profile of record is
  `docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md` v1.0, with committed vectors under
  `tests/vectors/carrier-v1/`. `verifier/go/oid4vp/carrier.go` is the single adaptation point.
- **DC API / Annex C carrier.** Parsed from `navigator.credentials.get` parameters,
  HPKE-encrypted to the verifier's key, byte-compatible with the reference verifier's
  standard-library HPKE. Fixture-verified; no device run exists.
- **Transcript binding.** The session transcript is CBOR opening `83 f6 f6 82 71`
  (null, null, a two-element array beginning `OpenID4VPHandover`) and closing with a 32-byte
  bstr equal to the SHA-256 of the handover-info encoding. Changing `client_id`, `nonce`,
  `response_uri` or a 32-byte `jwkThumbprint` changes the transcript bytes; a shorter
  thumbprint is rejected. `TranscriptForFlow`/`Flow` is the single transcript entry point and
  mixing flows is structurally banned.
- **Proof and circuit.** Longfellow (`longfellow-libzk-v1`), ~353–360 KB per proof, ~6.1 s
  proving on the development host, verification 2,414 ms per valid proof on a 2 vCPU host
  (and mean 4.02 s, p50 3.83 s, p95 4.91 s on an i5-8365U-class host, ~95 % of it sumcheck
  arithmetic). The bundled v7 circuits are 109-bit; the ECCG recommended floor is 125-bit.
- **Round-3 deviations recorded as deliberate and documented** (with their rationale, not
  as oversights): the agreement key precedes `client_id` in the authenticator data; the
  per-presentation JWE profile pins RFC 3394 §4.6 with an asserted cross-language token;
  credential dynamic data is carried in a CBOR int key (`val`) with no AAD JWE and an
  explicit key-binding gate at consumption; the ISO dcapi path decodes a strict CBOR subset
  in-house rather than delegating to upstream parsers.
- **Verifier properties worth carrying into architecture:** a strict CBOR subset parser with
  shared budgets, server-recomputed transcripts (the holder never gets a vote), single-use
  TTL sessions with shed-before-claim, allowlist-before-FFI plus a startup hash self-check,
  trust-store key pinning that selects but never adds, anti-downgrade gates
  (`EE-ZKP-042`, `EE-ZKP-045`, `EE-ZKP-045a`, `EE-ZKP-050`, `EE-ZKP-051`), and cgo
  thread-pinning-aware admission control.

---

## D3. Competitive picture (12 comparators — summary)

The full matrix, every column verbatim, is in `extract-shared-appendices.md` appendix A1.

What it establishes: the scheme is not this fork's invention (the Commission mandates it),
so differentiation rests on verification evidence. Within the ten-plus comparators examined
on 23 October 2026, none paired ZKP age proofs with a Go/Rust verifier and a published
negative-control test suite; five of six differentiation rows read "none in the set" for the
best comparator. Nobody was ahead on *ZK age-proof verification*: the Commission's reference
wallet described ZKP as "an optional feature", multipaz shipped the prover library without a
verifier product, and walt.id, EUDIPLO, inji, wwWallet and impierce claimed no ZKP at all.

What the later landscape pass changed: that differentiation is weaker than the record
states. Longfellow itself now ships a Go HTTP verifier service for `mso_mdoc_zk` with
Docker; the Commission ships verifier endpoints, a trust validator and an iOS Longfellow ZKP
library; and Google Wallet documents `mso_mdoc_zk` as a public relying-party integration
step. "The only ZK verifier" is no longer sayable; the surviving claim is narrower —
*verifier-side conformance and negative-control evidence, published*. The PRD states the
narrower claim (§1, NG-1) for that reason.

Others are ahead on everything production-shaped: a national-wallet pathway
(`de-eudi-wallet-android`, `eudi-app-android-wallet-ui`), certification tooling, and real
issuer integration. This fork has no certification path, no real issuer and no proximity
Tier-2 measurement.

---

## D4. Requirement index (41 rows — distribution)

From `extract-shared-appendices.md` A2 and `extract-estonia-alignment.md` §2 (all 41 rows
with their status prose intact): **5 supported · 23 partial · 7 missing · 5 not applicable ·
1 not verified · 0 compliant**, with `ZKP_08` the single unsatisfiable row. The PRD carries
the distribution (§10) because it is the honest summary; the row-level detail belongs to the
research corpus and to whichever downstream document needs clause-level traceability.

---

## D5. Sizing and cost data, with method

The PRD carries the headline figures (§7.4). The methods, so that a reader can attack them:

- **Q1 — 6–12 engineer-months.** 11,440 net new lines (the verifier's 11,225 lines including
  tests, plus 215 lines of Rust FFI) ÷ 1,200 net lines per engineer-month for reviewed,
  test-bearing security code. Confidence low–medium: the divisor is a rule of thumb and the
  same work was produced here in ~3.5 calendar weeks with AI assistance, so the figure
  describes human re-creation cost.
- **Q2 — ≈ 0.42 verifications/s per 2 vCPU instance; €0.9k–2.1k/year at a 10 % duty cycle.**
  From the measured 2,414 ms per valid proof, treating verification as single-threaded, with
  an invented 10 verifications/s peak and a €0.02–0.05 per vCPU-hour price.
  Confidence low.
- **Q3 — ~360 GB per million presentations; €18–43 per million.** From the measured
  353–360 KB proof size and a €0.05–0.12/GB egress price. Confidence medium on the size,
  low on the price.
- **Q4 — none.** Every figure is `PENDING-DEVICE`; the harness exists and the prover wiring
  does not.
- **Market sizing: deliberately absent.** The €21.65 M procurement value is sponsored-press
  only, the eID counts conflict (RIA ≈1.2 M against id.ee 1,383,604), and the APTITUDE
  figures disagree (117 against 141). The record's TAM-style figures (6,500 RIA employees /
  2,533 engine / 3,071 EIS in a sibling effort) are not carried here and no market size may
  be derived from any of them (CG-2).

---

## D6. Actor detail beyond the journeys

- **The holder** appears in this PRD only as the subject of privacy guarantees. The record's
  own reading: the wallet holder is not the buyer, and the product's promise to them is
  narrower than the product's promise to a relying party — one boolean, no false predicate,
  no central log, no silent downgrade.
- **The e-resident** (UJ-4) is a constraint case rather than a market: no Estonian personal
  identification code, so any design that assumes one is wrong for them, and the two-credential
  session is what makes their onboarding complete.
- **The venue reader** is the actor this PRD deliberately does not serve yet (D1.4).
- **The assessor** is the first reader in §1's [ASSUMPTION] and has no journey because their
  session is the reproduction workflow (NFR-8, SM-1) rather than a product surface.
- **Institutional readers** (RIA, the Ministry, the scheme owner, EU programme contacts) are
  stakeholders in §11, not users; their decisions are dependencies, not journeys.

---

## D7. Bookkeeping

- `.memlog.md` — every decision, change, assumption and override, append-only.
- `.decisions.log` — the tiltrank runs: options, scores, bands, separations, escalations.
- `extract-innovation-assessment.md`, `extract-estonia-alignment.md`,
  `extract-shared-appendices.md`, `extract-spec.md`, `landscape-extract.md` — the evidence
  this PRD was distilled from, in full, with the source corpora's labels intact.
- `review-*.md`, `reconcile-*.md` — the finalisation passes.
- The PRD's own numbering is distinct from `_bmad-output/planning-artifacts/epics.md`'s
  `FR1–FR66` / `NFR1–NFR10` (11 epics, 76 stories); both may be read side by side (§0, A-6).
