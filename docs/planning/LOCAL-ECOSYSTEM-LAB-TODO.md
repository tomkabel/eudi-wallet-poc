# TODO — Local EUDI ecosystem lab

Written 2026-11-04. Status: idea, not started. Repositories below were checked on GitHub on 2026-11-04;
all were active and not archived.

**Goal.** Every claim the fork makes today rests on its own mocks or on "blocked". Run open-source
reference services locally, pinned by commit, so each claim is tested against an independent
implementation of its counterpart service. When the real service appears, only the endpoint changes.

**What it changes.** Around a dozen "Blocked by (external)" stories can move from *blocked* to
*passed against `reference-impl@commit`*. None of them can move to *passed against the real service*,
and the evidence must keep that distinction visible.

## Next step

- [ ] **Compatibility spike.** Run `eudi-srv-wallet-provider` and `eudi-srv-pid-issuer` locally at
  pinned commits and check whether the `local_mocks` / `LOCAL` app talks to them. The app's `LOCAL`
  endpoints (`localhost:6565` wallet-provider RPC, `:4200` / `:6543` PID provider; `app/build.gradle.kts`)
  target open-eid's own backends. Whether those speak the same protocol as the EC services is
  `[UNVERIFIED]`. The answer decides whether the lab is configuration work or an adapter project.
- [ ] Check whether the EC services ship container images or must be built from source `[UNVERIFIED]`.

## Blockers a local stand-in unlocks

- [ ] **10.1: real issuer integration.** Stand-ins: `eu-digital-identity-wallet/eudi-srv-pid-issuer`,
  `eudi-srv-issuer-oidc-py`, `av-srv-web-issuing-avw-py`. Unlocks an OpenID4VCI interop run of the pinned
  `openid4vci-kt` 0.6.1, recorded with the service revision. Cannot claim interop with the Commission's
  *service*.
- [ ] **10.3: real WUA issuance.** Stand-in: `eudi-srv-wallet-provider`. Unlocks real WUA issuance, the
  case where a WUA is issued but cannot be validated, and an unavailable-provider test with fault
  injection. Cannot claim certification of a production wallet provider.
- [ ] **10.4: revocation with a revoked fixture.** Stand-ins: `eudi-srv-statuslist-py` (ASL/ARL),
  `eudi-lib-kmp-statium` on the client, and a local CRL server. Unlocks revoked and expired fixtures and a
  CRL past `nextUpdate`, none of which a live service will produce on demand. This story was never really
  externally blocked.
- [ ] **13.1 and 4.x: trust lists (LOTL, `CA/RPaccess`).** Stand-ins: `eudi-srv-web-trustedlist-manager-py`
  (LoTE), `esig/dss` to sign, `eudi-srv-trust-validator` (ETSI TS 119 612). Unlocks a local EE trusted list
  under a local LOTL, with negative cases the real LOTL cannot give: a withdrawn service, a rolled-back
  sequence number, a wrong signer, an expired list.
- [ ] **3.3, 3.4, 4.2, 4.6, 6.5: RP register and registration certificates (risk R9, rated High).**
  Stand-ins: `eudi-srv-web-relyingparty-registration-py` and `eudi-srv-web-relyingparty-registration-rprc-signer-java`.
  The ADR-007 file mock gets a real HTTP counterpart, Story 6.5's latency profile becomes measurable, and
  Story 3.4's proposal can be compared with an actual registrar API. Cannot stand in for RIA's API, which
  still does not exist.
- [ ] **3.2 and 3.8: TARA.** Stand-ins: `e-gov/GovSSO-Mock` (active) and `e-gov/TARA-Mock` (stale, last
  pushed 2023-02). The adapter's integration test runs instead of recording *blocked*. Does not replace the
  RIA agreement or a TARA test client.
- [ ] **2.4 and 1.3: independent verifier or target.** Stand-ins: `eudi-srv-verifier-endpoint`,
  `av-srv-verifier-endpoint`, `italia/eudi-wallet-it-python` (an Italian RP), `NiceLeader/oid4vc-conformance`
  (a headless wallet). Gives independent *implementation* evidence, not independent *operator* evidence,
  so 1.6 and 2.4 stay `awaiting-operator`.
- [ ] **11.7: cross-border trust.** Stand-in: an Italian OpenID Federation trust anchor
  (`italia/spid-cie-oidc-django`) as a second Member State next to the local EE trusted list. Tests whether
  the wallet or RP handles a foreign trust framework.
- [ ] **11.6 and 13.3: CT log and qualified timestamp.** Stand-ins: a self-hosted CT log and an RFC 3161
  TSA. Covers the behaviour when the CT log is unreachable, and the timestamp format. Cannot claim eIDAS
  qualification.

**Not unlocked by any stand-in:** the human integrators (3.7), the legal reviewer (9.6), the security
reviewer (11.4), the game-day operator (11.5), on-device numbers (6.1, 12.2). Emulator figures are not
device figures.

## Plan

1. [x] Map the blockers to stand-ins (above).
2. [ ] **The lab.** One compose file with the reference services, each pinned by commit: a local EE
   trusted list and LOTL, a registrar, a wallet provider, a status list, a GovSSO mock, and an Italian
   federation as the second Member State. Fault injection covers unreachable, 5xx and slow responses.
   Wired into the `LOCAL` / `local_mocks` variants only.
3. [ ] **Graded evidence.** Each result records its evidence class: `real-service`,
   `reference-impl@commit` or `own-mock` (the vocabulary of Stories 4.8 and 7.4). Story 14.1 contract
   tests show the mock and the reference agree on a shared case set. When a real service ships, the same
   suite produces the `real-service` row, and the conformity matrix is derived from it (7.3).

## Guardrails

- [ ] Lab trust anchors (the local LOTL and its self-made CAs) never reach a release build or a phone
  build outside `LOCAL` / `local_mocks`. Otherwise they are a trust bypass. Story 11.8 already flags mock
  trust surfaces.
- [ ] Lab keys stay out of `demo-data/` and are never committed.
- [ ] No result is labelled `real-service` without a real service behind it.

## Pass

Each story above is green at `reference-impl@commit`. A 14.1 contract test shows the mock and the
reference agree on a shared case set, and fails when a field is renamed. No release build contains a lab
trust anchor. No result is labelled `real-service`.

The outside judges are reference services accepting the fork's artefacts: `eudi-srv-trust-validator`
accepting the local trusted list, `eudi-srv-verifier-endpoint` verifying the wallet's presentation.
A self-hosted OIDF conformance suite could be another `[UNVERIFIED: not checked]`.
