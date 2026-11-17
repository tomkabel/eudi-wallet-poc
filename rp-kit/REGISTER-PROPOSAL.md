# RP-register interface proposal — version 1

**Prepared:** 13 November 2026. **Audience:** the Estonian programme's competent
authority, registrar, access/registration-certificate providers and service
owners. **Status:** prepared for publication and service-owner review; neither
public publication nor a human usability verdict is evidenced here.

## Purpose and scope

This proposes the smallest register lookup that the shipped RP service can
consume now, with reproducible synthetic controls and a route to an
authority-backed implementation. It implements the local policy in
[ADR-007](../docs/decisions/ADR-007-RP-REGISTER-LOOKUP.md) and shares the six-field
model in [ADR-005](../docs/decisions/ADR-005-INTENDED-USE-SOURCE.md). The executable
details are in the [RP-register contract](REGISTER-CONTRACT.md); this proposal
adds the authority decisions, standards mapping and review handoff.

A positive result permits the local verifier to proceed with its existing
presentation workflow. It establishes neither legal entitlement to request
data, authenticated ownership of an RP identity, successful proof verification,
nor the User's approval to disclose. Registration, request authentication,
User approval and proof verification remain separate obligations. In
particular, Topic 27 `Reg_01b` makes the registered contact, service, attribute
and purpose information a transparency mechanism without pre-authorisation;
the fail-closed gate is this fork's privacy policy, not a claim that a registrar
has legally approved each request.

This is a local projection, not an implementation of the full TS5 public API
or TS6 registration data set, and not a certification or national authority
endorsement. No production access- or registration-certificate parser is
delivered by this proposal. The proposal remains useful even if superseded:
the identity, refusal, bounded local cache-age and test contracts are explicit inputs
to procurement and integration, rather than assumptions reconstructed later.

The current RIA production endpoint, supported API version, onboarding/trust
requirements and Estonian certificate profile/CT arrangements are
**[UNVERIFIED]** for this proposal. This is not evidence that an API is absent.
Historical absence/design wording in ADR-005/007 and the research corpus is
historical context, not a verified current service-availability claim. See the
[source record](#dated-source-record) and [spec §23 item 40](../spec/EE-EUDIW-TS-1.0.md#23-open-issues-and-unverified-claims).

## Lookup and model

The shipped [Go package](../verifier/go/rpregister/register.go) exposes:

```go
type RPRegister interface {
    IntendedUse(RegisterIdentity) (RegisteredIntendedUse, error)
}
```

`RegisterIdentity` is the exact pair `(uniqueIdentifier, serviceIdentifier)`.
The entity comes from server `-client-id`, the service from
`-rp-register-service` (default `age-verification`). No case folding, alias
expansion, bare-host matching or holder-supplied identity grants permission. A
stored session's ClientID must still equal the server ClientID. The synthetic
`x509_san_dns:…` ClientIDs are local identifiers; they do not prove an
authority-issued EU-wide identifier or an authenticated certificate binding.

There is one model on the RP and planned wallet sides, exactly these six JSON
fields; a Go slice represents the unique set of doctype/predicate pairs:

| Field | Local representation and validation |
|---|---|
| `tradeName` | Nonempty display string; no surrounding whitespace; at most 512 bytes |
| `uniqueIdentifier` | Exact entity lookup identifier; nonempty, no surrounding whitespace, at most 512 bytes |
| `serviceIdentifier` | Exact service lookup identifier, separately scoped to the entity; same text bounds |
| `memberState` | Two uppercase ASCII letters; the implementation does not check assignment against a country-code list |
| `intendedUses` | Nonempty array of unique `{doctype,predicate}` pairs; doctype uses the same text bounds, predicate matches `^age_over_[0-9]{2}$` |
| `privacyPolicyUrl` | Explicit HTTPS URL with hostname and no URL credentials, or explicit `null`; missing or blank refuses |

Every JSON key must be present and spelled exactly, including the explicit
privacy-policy absence marker. Unknown fields, duplicate keys at any object
depth, case variants, trailing JSON, malformed/empty records, identity mismatch
and duplicate pairs refuse. File and HTTP documents are capped at 1 MiB.
`memberState` validation is syntactic, not ISO-list validation or a determination
of establishment. The predicate grammar does not establish a statutory age
threshold; the synthetic corpus covers only 16, 18, 21 and 65.

Each requested credential must have supported local format `mso_mdoc` or
`mso_mdoc_zk`, an explicit doctype and nonempty claims. Every claim must be
exactly `[doctype, age_over_NN]` and match a registered pair. This verifier's
namespace equals the doctype: a same-named predicate on a different doctype or
namespace does not match. One unauthorised claim refuses the whole request; a
record containing unrelated allowed uses cannot rescue it. The gate permits
registered requests; it does not change wallet predicate/held-false policy.

## Transport and failure policy

The shipped deployment defaults to neither provider being configured. Select
exactly one of `-rp-register-file` (file-backed synthetic mock, loaded at
startup) or `-rp-register-url` (provisional HTTP client). The latter is a
consumer of a proposed endpoint, not a register server supplied by the kit.

For the HTTP seam the configured URL receives GET query parameters
`uniqueIdentifier` and `serviceIdentifier`. A 200 body is one complete six-field
object; 404 means no exact identity; every other HTTP status is a configured
service failure. Remote endpoints require HTTPS with normal Go certificate
verification. Plain HTTP is allowed only for literal loopback IP hosts, not
the hostname `localhost`. URL credentials and fragments refuse; redirects are
never followed; this transport disables environment-proxy discovery. The
absolute timeout covers connect, response headers and body. TLS server
verification does not establish a registrar signing authority or validate a
TS5 JWS.

These are the errors and the verifier's outward responses; provider HTTP
statuses above are distinct from these presentation-gate statuses:

| Error | Cause | Presentation HTTP / stable log reason |
|---|---|---|
| `ErrNoImplementation` | No provider, including nil/typed-nil | 503 / `missing-register-prerequisite` |
| `ErrRegisterUnreachable` | Connection/DNS failure, absolute timeout, provider non-200 except 404, or excess cold lookup admission | 503 / `register-error` |
| `ErrNotFound` | Exact entity/service absent, including provider 404 | 403 / `register-not-found` |
| `ErrInvalid` | Malformed/incomplete/oversized or identity-mismatched registration | 403 / `register-invalid` |
| `ErrUnauthorized` | Disallowed credential/claim, or stored ClientID mismatch | 403 / `register-unauthorized` |

Other provider errors also receive the fixed `register-error` response; their
text is not exposed. A cold lookup error refuses without needing any cache
entry. Health and result polling remain available. Creation denial creates no
session; release denial sends no request material; callback denial does not
claim the session or make a new `valid=true` decision.

The [registration gate](../verifier/go/register.go) runs for redirect
`startSession`, `startSessionMulti`, `handleRequest`, `handleResponse` in
[present.go](../verifier/go/present.go), and DCAPI `handleDCAPINew`,
`handleDCAPIRequest`, `handleDCAPIPage`, `handleDCAPIResponse` in
[iso.go](../verifier/go/iso.go). Release/callback paths re-evaluate the stored
ClientID and query; the DCAPI request/page paths recheck session lifetime after
lookup. Existing encryption, admission-before-Claim, single-use, drain and
proof verification mechanics still apply.

## Published cache and resource bounds

The deployed defaults are published in the
[environment template](zkverify.env.example), the contract and startup log:

| Setting | Default | Meaning |
|---|---|---|
| `-rp-register-cache-ttl` / `RP_REGISTER_CACHE_TTL` | `15m` | Maximum local positive-entry age, exclusive |
| `-rp-register-cache-capacity` / `RP_REGISTER_CACHE_CAPACITY` | `256` | Maximum positive records per cache instance |
| `-rp-register-timeout` / `RP_REGISTER_TIMEOUT` | `2s` | Absolute HTTP lookup deadline |

TTL and capacity must be positive. Local age starts at insertion after the
provider lookup and validation complete. A validated entry permits lookup
success only at age `0 <= age < TTL`; reading does not reset insertion time.
The TTL bounds additional local retention, not the age of the authority's
underlying data: source/replication freshness remains the authority's
obligation. A locally fresh
positive entry may therefore permit the request during a provider outage or
before the provider's removal/change is observed. The log records
`cache-hit-within-ttl`, age, TTL, `provenance=bounded-positive-cache` and
`network=not-contacted`. The cache makes no network probe on that path, so it
cannot truthfully assert that the provider is currently unreachable. This
explicit local cache-age observation implements ADR-007's outage exception;
it is not a live-register freshness claim.

At exactly TTL or later, discard and log `stale-discard-relookup`, then query
again. A negative clock age also discards. A provider failure, missing record
or changed unauthorised use after discard refuses, never falls back to stale
data. Errors are never cached. Capacity eviction removes the oldest inserted
positive record and logs `capacity-eviction`; records and returned values are
defensive copies. A file provider reload requires restart. Cached data is
process-local, lost at restart, and has no revocation push/invalidation feed.

A short mutex guards entries; one provider lookup per cache instance is
admitted without queuing. Excess cold lookups immediately return
`ErrRegisterUnreachable`; fresh hits remain accessible. The HTTP transport has
at most eight connections per host. The 1 MiB body cap and 2s default deadline
bound the HTTP operation; a future non-HTTP provider must supply its own
execution bound. No per-key synchronisation objects are retained.

The authority must decide whether strictly less than 15m of additional local
retention after lookup completion is acceptable alongside its source/replication
freshness guarantees, and publish any different deployed value. This is not
a 15m bound on total permission staleness. This adopts the
published-age/fail-closed design rule of local `EE-REV-007`; the register is
not a status list, so this is not a claim that that requirement applies to it.

## Reproduce the synthetic controls

From the repository root, with the pinned toolchain and dependencies described
in [DEVELOPMENT.md](../docs/DEVELOPMENT.md):

```sh
make py-deps
make deps
make test-go-fast
make test-go-zk
make test-rp-kit
```

The test targets rebuild/use current source as described in
[tests/README.md](../tests/README.md#rp-register-story-33). No authority endpoint,
certificate or holder device is needed for these controls. After the full
targets, the register-specific tests can be re-run visibly without Go's
success cache:

```sh
(cd verifier/go && go test -race -count=1 -v ./rpregister/...)
(cd verifier/go && EE_BENCH_SKIP=1 go test -race -count=1 -v -run '^TestRegister' .)
```

| Existing control | What a passing run demonstrates |
|---|---|
| `TestStrictModelAndMock`, `TestJSONRequiresCompleteSixFields`, `TestStrictJSONCorpus` | Strict six fields, corpus version/shape/size and actual missing identity |
| `TestHTTPContractControls`, `TestHTTPBodyDeadline` | Exact identity query, positive/404/5xx/redirect/malformed/mismatch/oversize responses, unsafe URL refusal and bounded body read |
| `TestNoImplementationAndInvalidProviderNeverCache` | Nil/unconfigured and invalid provider cannot populate positive cache |
| `TestCacheBoundariesIsolationCopies`, `TestConcurrentCache`, `TestCacheBoundedColdAdmission` | Fresh outage exception, exact TTL boundary, stale discard, no error caching, key isolation, defensive copies, oldest eviction and bounded concurrent work |
| `TestRegisterHTTPGates`, `TestRegisterConfiguredFailuresHTTP`, `TestRegisterFreshStaleHTTP` | Redirect/DCAPI missing/omitted/disallowed refusals, cold connection/timeout/5xx failures and fresh-to-stale changes |
| `TestRegisterAllClaims`, `TestRegisterMultiCreationRefusal`, `TestRegisterStoredPolicy`, `TestRegisterDCAPIExpiresDuringLookup`, `TestRegisterNilAndSensitiveErrors` | Every claim and stored policy checked, no denied material/session claim, expiry recheck and fixed non-sensitive errors |
| `make test-rp-kit` / `tests/rp_kit_test.py` | Actual shipped unit/environment defaults refuse both flows; explicitly enabled mock supports local encrypted profile checks |

The versioned [shared corpus](register/synthetic-v1.json), documented in
[register/README.md](register/README.md), contains exactly three invented
positive identities: `x509_san_dns:verifier.example.ee`,
`x509_san_dns:rp.example.ee` and `x509_san_dns:localhost`, each with service
`age-verification`. Each allows 16/18/21/65 for both `ee.riik.poa.1` and
`eu.europa.ec.av.1`. The negative identity `x509_san_dns:omitted.example` is
absent, never represented by a supposedly negative registration. Privacy
policy is explicitly `null`. None is an authority registration.

For manual startup use the complete [RP service walkthrough](README.md#build-and-local-acceptance),
including temporary trust inputs, persisted P-256 response key and encrypted
defaults. It explicitly passes `-rp-register-file` to opt into the corpus.
Starting the same profile with both register options empty gives the missing
prerequisite refusal. Do not use the mock to represent real legal permission.

## ARF compliance and gap note

The baseline is the canonical `eudi-arf` submodule commit
`6373eee10b6e80225c7ce706a5ff1775fb799b22`, not a floating ARF version name.
Requirement IDs below are read from the pinned
[HLTR CSV](../eudi-arf/hltr/high-level-requirements.csv) and
[Annex 2 topic text](../eudi-arf/docs/annexes/annex-2/annex-2.02-high-level-requirements-by-topic.md).
The pinned TS5/TS6 pages are pointers to a separate standards repository;
its exact reviewed revision and verified document labels are recorded below.
Rows state local implementation, proposed adaptation or gaps, never overall
compliance. Empty/deleted HLTR slots are not treated as obligations.

| Topic / source IDs | Shipped behavior or proposal | Remaining gap / authority decision |
|---|---|---|
| 27 — `Reg_01`, `Reg_01a`, `Reg_02`–`Reg_04`, `Reg_06` (`AS-MS-27-001/002/004/005/006/008`) | Local read-only lookup and mock provide an integrator seam | No national registration process, full TS6 data, signed TS5 responses, public human-readable registry or full public API |
| 27 — `Reg_01b` (`AS-MS-27-003`) | Local predicate gate restricts the fork's request behavior | Registry transparency is not legal pre-authorisation; establish appropriate legal basis separately |
| 27 — `Reg_07`–`Reg_09`, `Reg_24`–`Reg_26`, `Reg_29` (`AS-MS-27-009/010/011/035/036/037/040`) | No registration/update/identification/cancellation service; file mock is startup-only | Registrar owns identity checks, lifecycle, audit history and cancellation; adapter must evaluate current lifecycle before projection |
| 27 — `Reg_10`–`Reg_10d`, `Reg_11`–`Reg_13` (`AS-MS-27-012`–`016`, `018`–`020`) | Exact entity/service pair is retained locally | No access/registration-certificate issuance, CA policy, certificate revocation or trusted-list validation |
| 27 — `Reg_31`–`Reg_35` (`AS-MS-27-042`–`048`) | Entity name/id and service id are separate; mismatch refuses | Synthetic ClientID is not the EU-wide certificate identifier; separate service trade name, intermediary bindings and certificate contents are absent |
| 6 — `RPA_01`, `RPA_01a`, `RPA_02`–`RPA_05` (`AS-WP-06-001`–`006`, excluding empty `RPA_02a` / `AS-RP-06-001`) | Local gate is additional request policy only | Access-certificate request signatures, replay protection of authenticated requests, notified Access CA trust anchors and wallet-controlled authentication remain separate obligations; unsigned requests are not made authenticated by lookup |
| 6 — `RPA_06a` (`AS-WP-06-008`) | Local RP-side refusal is not the wallet's authentication-failure UI | If RP authentication fails for any reason, the Wallet Unit must notify the User and either withhold the requested attributes or let the User choose whether to present them; the Wallet Provider selects between those two policies. Notification and that wallet policy are not implemented or evidenced by this register seam |
| 6 — `RPA_06`, `RPA_07`–`RPA_08`, `RPA_10`, `RPA_10a`, `RPA_11` (`AS-WP-06-007/009/010/011/012/013/015/016/017`) | Name, pair identifiers and optional privacy URL support a future wallet projection | No proof of wallet User authentication/approval or UI here; entity and service names, localised intended-use description and applicable privacy policy are needed; `null` is an honest local absence, not TS5 policy completeness |
| 44 — `RPRC_01`–`RPRC_04a`, `RPRC_06`–`RPRC_12` (`EW-DM-44-001`–`008`, `010`–`017`) | Six fields are a limited projection useful for consistency tests | No signed registration certificate, chain/LoTE validation, revocation, service display name, deletion-contact/DPA data or certificate per intended-use/service combination |
| 44 — `RPRC_17`, `RPRC_17a`, `RPRC_19`, `RPRC_20`, `RPRC_21` (`EW-DM-44-019/020/023/025/027`) | Gate requires every locally requested pair and exact identity | No by-value single applicable registration certificate transfer or wallet certificate validation; RP-side lookup cannot replace them. The pinned ARF permits wallet-provider risk-policy choices/warnings for some failures; this fork's strict refusal is local policy, not the sole ARF behavior |
| 19 — `DASH_03`, `DASH_03a` (`AS-WP-19-006/007`) | Four identity fields can seed the future wallet log; local gate logs contain no holder attribute values | No complete wallet dashboard/log claim: registrar URL, contacts, DPA, intended-use purpose and other required fields are absent; RP diagnostics are not the User's transaction log |
| 55 — `CT_01`–`CT_06` (`AS-MS-55-001`–`006`) | No CT behavior supplied | CT log/CPS/monitoring, SCT inclusion and wallet SCT verification are authority/CA/wallet work. `CT_01`/`CT_03` have availability conditions; these do not erase `CT_04`–`CT_06`. Estonian deployment arrangements remain unverified |

### TS5 / TS6 projection, not a second registration vocabulary

The reviewed primary text labels TS5 **1.6.1 (10 November 2026)** and TS6
**1.2.2 (20 August 2026)**. These are the document labels at the separate
reviewed revision, not labels inferred from the ARF gitlink. TS5 §2 supplies
classes and §3 the common API; TS6 §2 enumerates the common registered data.

| Local field / operation | Primary model relationship | Projection loss and migration rule |
|---|---|---|
| `tradeName` | TS5 `WalletRelyingParty.tradeName` is optional `[0..1]`; TS6 trade-name row also names mandatory `WalletRelyingPartyService.serviceTradeName` | The local model requires a nonempty entity trade name; an absent entity tradeName refuses projection under the current strict model. Do not invent a fallback or substitute the service name. One local string cannot carry both names or localised purpose |
| `uniqueIdentifier` | TS5 inherited `identifier` / `ServiceReference.identifier`; TS6 identifier row | Local string omits typed identifier scheme and registrar provenance. Resolve the authority's exact certificate identifier; never relabel a DNS ClientID as an EUID |
| `serviceIdentifier` | TS5 `WalletRelyingPartyService.serviceIdentifier` and `ServiceReference` | Retain entity scope. TS5 §2.4.14 uses an identifier/service/registryURI triple across registrars; local identity lacks registryURI and depends on its configured provider |
| `memberState` | Country of establishment / TS6 country-indicator information and verified registry context | Local field is not itself a full TS6 identifier encoding; retain authority provenance and any scheme-specific rules rather than infer establishment from a DNS suffix |
| `intendedUses[].doctype/predicate` | TS5 `WalletRelyingPartyService.intendedUses[].credentials` with `format`, `meta.doctype_value`, `claims`; TS6 data-requested and purpose rows | Local flattened pairs omit intendedUseIdentifier, purpose/localisations, format distinctions, lifecycle and richer paths. Select one applicable registered intended use before projection; never union distinct purposes/services to manufacture permission |
| `privacyPolicyUrl` | TS5 `IntendedUse.privacyPolicy` (Policy objects); TS6/TS7-related policy information | One URL/null cannot express mandatory TS5 policy objects/localisations; an authority adapter must validate the selected intended use's applicable policy before projecting it |
| GET configured URL, two local query keys; 200 object / 404 absent | TS5 §3.1.2 GET `/wrp` supports `identifier`, `serviceIdentifier` and other filters, pagination and JWS-signed results; no query matches is 200 empty array. GET `/wrp/{identifier}/services/{serviceIdentifier}` returns a signed parent RP payload isolated to that service, with 404 for an absent RP or service | The shared use of 404 does not make the local seam TS5: its query keys and unsigned six-field object differ from the richer signed service-isolated payload. A future adapter must validate JWS/trust and select the exact active intended use; an existing RP/service does not establish that the selected use exists. Map absent RP/service or missing selected use to `ErrNotFound`, never a union of other uses |

TS6 also includes legal identity, contact/support, service descriptions,
public-sector status, entitlements/sub-entitlements, intermediary associations
and attestation-provider information; TS5 adds registry/lifecycle and
certificate-related structures. None is made unnecessary by the six-field
projection. In particular `intendedUses` here is not TS5's full `IntendedUse`
class, and local authorization success is not a registered entitlement.
Ambiguous intended-use selection cannot be resolved by widening the six
fields; refuse and explicitly revise the interface/profile if the authority
needs a selector or richer data.

## Privacy, trust and certificate migration

An RP backend performs the lookup; the wallet does not call the register per
presentation. This avoids introducing a register observer of wallet
presentation timing. RP lookups can still reveal RP usage timing to the
provider; bound access logs and disclose this when selecting a deployment.
The local diagnostic logs contain stable refusal/cache decisions and age/TTL,
not holder attributes, tokens, keys, query bodies or provider error text.
Minimise request-path observability rather than logging full registrations.

The file mock trusts an explicitly configured local file. The provisional HTTP
client trusts configured transport TLS; it does not verify signed authority
data, certificate chains, status, CT, or EU trusted lists. Neither proves that
the configured ClientID belongs to this service. Before a real integration,
the authority and operator need to establish these trust bindings.

Migration should retain the negative controls and revise the interface only
where the following live-integration prerequisites require it:

1. Pin an authority API/data/certificate profile and record its provenance,
   identifier scheme, registrar URI, trusted signer/CA sources, lifecycle and
   revocation/freshness rules. Review the 15m additional-local-retention budget
   separately from source/replication freshness guarantees.
2. Implement a separate authority adapter, validating the full signed response
   and current lifecycle before producing the same six fields. Reject
   ambiguous identity/service/intended-use matches, unsupported structures and
   partial parses. Do not add a generic unknown-field/union fallback to the
   existing strict seam. The current six fields and insertion-age-only cache
   cannot honor a selected intended use's future `revokedAt` expiry after the
   initial lookup. Before live use, explicitly revise the interface/cache to
   retain and cap validity at the authority's applicable deadline, or refuse
   that integration; validating lifecycle only before caching is insufficient.
   Re-run all local failures and add authority vectors.
3. Keep access certificates (RP request authentication) distinct from
   registration certificates (registered intended-use data). Supply/validate
   them according to the chosen profiles, including request signatures,
   trust chains, lifetime/status and applicable CT rules; an HTTP lookup or
   positive cache entry is not a replacement certificate.
4. Introduce the wallet's certificate-backed `IntendedUseSource` behind the
   fail-closed ADR-005 interface and a version-labelled, pinned profile. Refuse
   unsupported newer labels with `version-unsupported`; bound older-version
   compatibility explicitly. Parse per request, with no wallet register cache.
   A future certificate-versus-register comparison must bind the same RP,
   service and selected `intendedUseIdentifier`: `RPRC_09` issues separate
   certificates per intended-use/service combination, so different uses of
   the same service are not interchangeable. The current six-field projection
   loses that identifier; comparison and live adaptation require an explicit
   profile/interface revision, never a union of purposes. For that same
   selected use, disagreement must refuse as `intended-use-mismatch`, never
   choose the wider permissions. Authenticated delivery of the corresponding
   register projection to the wallet remains an unresolved design dependency;
   no mechanism or executable comparison is supplied here, and this proposal
   implies no per-wallet online register lookup.
5. Reuse the same [synthetic corpus](register/README.md) in Story 4.6's future
   certificate generator without widening uses. That generator and production
   parser are future work, not delivered evidence. Snapshot only the required
   identity/intended-use metadata for future Topic 19 logs, never attribute
   values; do not retain a wallet certificate cache.

## Decisions requested from the authority

These are review inputs, not prerequisites to publishing the proposal:

| Decision / owner | Concrete answer needed |
|---|---|
| Registrar / programme authority | Authoritative API and supported TS5/TS6 revisions; signed-response trust/bootstrap; stable identifier/service/registry binding and exact intendedUseIdentifier selection, preservation and authenticated projection delivery for wallet comparison |
| Registrar / RP service owner | Registration/update/cancellation workflow; test identities, legal basis and privacy-policy obligations; service trade name and localised purpose/data mappings |
| Registrar / operator | Availability, rate/size limits and error mapping; source/replication freshness, maximum additional local retention and validity-deadline caps or refusal; any revocation invalidation feed; whether the 15m local default is acceptable |
| Access CA / registration-certificate provider | Chosen access versus registration profiles, issuance, validity/revocation, trusted-list discovery, certificate/request binding and CT/SCT responsibilities |
| Wallet provider / DPA contact | User-facing identity/approval/warning policy; Topic 19 fields, registrar/deletion/DPA contacts and metadata retention/export |
| Estonian service owner | Whether these inputs, errors and reproduction steps are usable for an actual service; specific changes needed |

An unavailable answer remains an explicit gap; it does not turn into a
synthetic authority claim or prevent this proposal being put on the table.

## Publication, human review and supersession

Story 3.4's [acceptance criteria](../_bmad-output/planning-artifacts/epics.md#story-34-rp-register-interface-proposal)
require publication. The D3
[roadmap exit criterion](../docs/research/2026-10-23-shared-appendices.md#a3)
requires an Estonian service owner to review the proposal and say whether it
is usable. A committed document and passing mock tests alone satisfy neither
external obligation. No publication URL, named service-owner review or human
verdict is supplied by this change.

Operator handoff:

1. Publish the reviewed proposal on the public `fork` remote or another agreed
   public channel, linking the exact committed revision and in-repo contract,
   corpus and primary-source record. Record the actual URL, revision and
   publication date; do not push to upstream `origin`. Agent execution does
   not push or send external messages.
2. Arrange review by an Estonian service owner. Have them follow the reproduction
   steps and assess identity/service selection, refusal handling, outage/TTL
   acceptability, privacy/trust gaps and needed authority decisions. Record
   reviewer name/role, review date, reviewed revision, usable/not-usable verdict
   and concrete changes, with permission to publish that evidence.
3. Address the feedback and retain the publication/review evidence in the
   story record. Keep the terminal state `awaiting-operator` with imperative
   `operator_actions` until both publication and D3 usability evidence exist;
   do not infer a human verdict from automated review or orchestration state.

If an authority contract supersedes this proposal, retain version 1 and its
source pin, link a dated replacement, list changed identity/model/error/cache
rules and re-run the relevant controls. Do not retrospectively describe the
synthetic seam as having implemented the replacement.

## Dated source record

Read and checked **13 November 2026** using public sources only:

| Source | Pin / evidence and limits |
|---|---|
| Local shipped seam and policy | Repository baseline `634b1d14c0844d53e82a8923161e6207d4f65412`; [contract](REGISTER-CONTRACT.md), [Go implementation](../verifier/go/rpregister/register.go), [handler gate](../verifier/go/register.go), [unit controls](../verifier/go/rpregister/register_test.go), [handler controls](../verifier/go/register_test.go), ADR-005/007. Runtime claims describe those sources, not a new authority implementation |
| Canonical ARF HLTR | Both `git ls-tree HEAD eudi-arf` and `git -C eudi-arf rev-parse HEAD` resolve `6373eee10b6e80225c7ce706a5ff1775fb799b22`. [CSV](../eudi-arf/hltr/high-level-requirements.csv) supplies the IDs; [Annex 2](../eudi-arf/docs/annexes/annex-2/annex-2.02-high-level-requirements-by-topic.md) supplies the matching topic text. This note is about that revision, not an assertion of the latest ARF |
| TS5/TS6 source resolution | The pinned [TS5 pointer](../eudi-arf/docs/technical-specifications/ts5-common-formats-and-api-for-rp-registration-information.md) and [TS6 pointer](../eudi-arf/docs/technical-specifications/ts6-common-set-of-rp-information-to-be-registered.md) contain no model/version text. Their public standards repository was resolved at `cfc383e44294d65cfa6cccbbff6c927f0df91f9a` and the primary Markdown read at that immutable revision; versions below are separately pinned, not retroactively attributed to the ARF commit |
| TS5 primary text | Public `eu-digital-identity-wallet/eudi-doc-standards-and-technical-specifications`, `docs/technical-specifications/ts5-common-formats-and-api-for-rp-registration-information.md` at the revision above: version table 1.6.1 dated 10.11.2026; §§2, 3.1 and Annex A. SHA-256 of raw Markdown: `db0ddd4c420be6da130b1e0933860ccffd758af64c6899e8320d4367f3257119` |
| TS6 primary text | Same public repository/revision, `docs/technical-specifications/ts6-common-set-of-rp-information-to-be-registered.md`: version table 1.2.2 dated 2026-08-20; §2 data table. SHA-256: `03b198deb4f8151ef73c755598a399a951163d6d6b3918e03ea56c372009d77c` |
| Public-source reuse | TS5/TS6 identify European Union authorship and CC BY 4.0. This proposal paraphrases their structures and records projection differences; it does not vendor or reproduce their full text |
| Story/D3 scope | [Story 3.4](../_bmad-output/planning-artifacts/epics.md#story-34-rp-register-interface-proposal), [D3 roadmap](../docs/research/2026-10-23-shared-appendices.md#a3) and [Story 3.3 implementation record](../_bmad-output/implementation-artifacts/spec-3-3-fail-closed-rp-register-interface-and-mock.md). Research/ADR statements about RIA are not current primary authority evidence |
| Current Estonian authority arrangements | **[UNVERIFIED]**: production register endpoint/API version, availability/onboarding and authority trust/certificate/CT profile have not been established against a current RIA primary specification here. Request those from the authority and record their dated primary evidence; failed search is not proof of absence. Collected in spec §23 item 40 |

To retrieve the reviewed TS texts from the repository identified by the pinned
in-repo pointers, substitute the recorded revision for the pointer's `main`
reference. For a byte-checkable reproduction (network required):

```sh
python3 - <<'PY'
import hashlib
import urllib.request
revision = "cfc383e44294d65cfa6cccbbff6c927f0df91f9a"
base = "https://raw.githubusercontent.com/eu-digital-identity-wallet/"
base += "eudi-doc-standards-and-technical-specifications/" + revision
expected = {
    "ts5-common-formats-and-api-for-rp-registration-information.md":
        "db0ddd4c420be6da130b1e0933860ccffd758af64c6899e8320d4367f3257119",
    "ts6-common-set-of-rp-information-to-be-registered.md":
        "03b198deb4f8151ef73c755598a399a951163d6d6b3918e03ea56c372009d77c",
}
for name, digest in expected.items():
    url = base + "/docs/technical-specifications/" + name
    with urllib.request.urlopen(url, timeout=20) as response:
        data = response.read()
    if hashlib.sha256(data).hexdigest() != digest:
        raise SystemExit("source mismatch: " + name)
    print(name, digest, "PASS")
PY
```

This record verifies the cited technical text, not the legal force,
applicability dates or current national deployment of every instrument it
references. No new legal-deadline or production-conformance claim is made.
