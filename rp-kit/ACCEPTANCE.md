# Estonian acceptance evidence

Story 3.5 records seven fixed cases. An expected refusal passes a control; it is
not an accepted presentation. Local EE credentials cannot establish foreign
wallet acceptance. The Digital Credentials API is the AV Profile mandated ZK
path; OpenID4VP is profiled and unofficial (ADR-008). These checks imply no
certification, legal permission, external trust agreement or public register.

The [13 November 2026 local execution](evidence/acceptance-2026-11-13/README.md)
retains five local PASS controls, two foreign NOT_EXECUTED cases and the
awaiting-operator handoff, with explicit dirty-workspace provenance.

The [16 November clean-commit execution](evidence/acceptance-2026-11-16/README.md)
records the completed review repairs, five local passes and explicit foreign-session
operator handoff against the exact implementation revision.

## Execute

Use the [development setup](../docs/DEVELOPMENT.md), pinned Python dependencies,
JDK 17, Android SDK and built Longfellow prover/staticlib:

```sh
make py-deps
make deps
make doctor
make test-estonian-acceptance
JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
  .venv/bin/python3 tests/run_estonian_acceptance.py \
  --json /tmp/estonian-acceptance.json
```

Without an external adapter, the last command **exits 1**: foreign PID and AV
remain NOT_EXECUTED. Five agent-executable rows should pass and report
status awaiting-operator with imperative operator_actions. Report result is
INCOMPLETE, never PASS. Keys and credentials are generated in private temporary
directories outside the checkout and removed when the local process exits.
Never copy holder/private-key output into a public evidence directory.

The runner builds a fresh verifier with its full linked Longfellow revision,
stages the shipped service profile, exercises both creation endpoints using an
absent exact RP identifier, verifies 403/register-not-found and no released
session, then tests the real handlers' zero live-session allocation. A positive
registered control mints a local EE AV, runs the actual linked prover and posts
an encrypted direct_post.jwt response. Its verified session is supplementary
local evidence only, outside the two foreign rows.

The authoritative EstonianViewModelAcceptanceTest enters the actual provider
and proximity ViewModels with real JSON/CBOR/transfer inputs. It uses real
credential matcher results and observes capability evaluation, retained terminal
refusal, empty consent state and the transport boundary remaining unused, even
after share/PIN actions. Incapable-device controls reach consent. The smaller
EstonianAcceptanceDispatchTest remains a helper regression suite. Fresh JVM XML
from the actual ViewModel suite and SignedCircuitFallbackTest must contain
successful, unskipped tests and named observations; old XML cannot pass. Go
tests run with -count=1. A child timeout, failure, excessive output, missing
marker, invalid schema, unknown/duplicate/missing case or skipped suite fails
closed and preserves all seven ordered result rows. Children have process-group
cleanup, a 900-second default limit (configurable 1–3600), and a 4 MiB output
limit. Commands run directly without a shell.

## Cases and observations

| Ordered case_id | Required observed evidence |
|---|---|
| foreign-pid | Fresh external PID wallet session, foreign country, source/session provenance, request/response/verification artifacts |
| foreign-av | Separate fresh external AV wallet session and artifacts; never the local EE AV control |
| unattested-openid-dcapi | Actual unsupported unsigned OpenID protocol reason, refusal effects and zero response; this shape cannot claim Tier-2 execution |
| unregistered-rp | Both /present/new and /present/dcapi/new return 403/register-not-found with no allocation/released session |
| shared-offline-fallback | Wallet and verifier consume the same signed pinned document without fetched registry, validate it and resolve the same usable SpecID/list_version |
| unauthenticated-iso-mdoc | Plain-only real org-iso-mdoc wire request without trusted readerAuth refuses before response on a capable device |
| unauthenticated-proximity | Actual RequestReceived event without trusted readerAuth stores refusal and stops transfer before response on a capable device |

Each row contains state (PASS, FAIL, ERROR, NOT_EXECUTED), an observed outcome
and evidence. The report includes the canonical repository revision and dirty
status, complete implementation content manifests before and after execution,
exact commands/exit codes/output, JVM artifact
hashes, path labels, supplementary local proof and operator_actions. A dirty
working tree is disclosed; it is not a clean release assertion. Diagnostics and
non-pass rows survive to the JSON output and produce a nonzero exit. The manifests
include relevant tracked and untracked implementation files, including the
resolver, both ViewModels and authoritative tests; build output, evidence,
documentation and private demo-data are excluded. A source mutation invalidates
all five local claims. A missing/unreadable Longfellow pin is a named ERROR with
all seven rows retained.

## Signed offline fallback

The verifier's signed [circuits.json](../verifier/circuits.json) is copied as a
generated wallet build resource. The public release pin, generator, validation
and regeneration commands live in the
[circuit-list guide](../verifier/go/circuits/README.md). Both actual production
consumers report list_version, document_sha256, resolved spec_id and fetched=false.
The runner requires identical observations and matches their hash to the pinned
document. It does not infer resolution from file hashes alone. Missing, tampered,
expired, unknown-key, incompatible runtime or version-skewed lists refuse. Native
circuit hash/tuple/parameter matching and deprecation rules still apply. No list
fetch occurs per presentation. The exact production wallet factory is tested
with injected native/list inputs, preserving native capability when a list
fails. Named missing/invalid/stale/incompatible/skew failures remain inspectable,
including expiry after loading. Resolver tests require the full requested
ID/version/hash/attribute-count/block-parameter identity. The Go handlers recheck
current registry validity before allocation, request delivery and verification,
while retaining the session's offered-circuit allowlist. The shared
[optional metadata vectors](../tests/vectors/circuits/optional-metadata.json)
test both decoders without changing the pinned signature: optional text permits
omission/null/string, audits permits omission/null/an array of strings and
rejects null elements. Hosted/fetched distribution and provenance-ring
scope remain the later Epic 5 stories; this is the offline prerequisite.

## Fresh external adapter contract

No external adapter, genuine foreign PID/AV or access credentials are shipped.
An operator supplies an executable adapter and a clean independent Git checkout:

```sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk \
  .venv/bin/python3 tests/run_estonian_acceptance.py \
  --json /tmp/estonian-acceptance.json \
  --adapter /absolute/path/to/operator-adapter \
  --foreign-source /absolute/path/to/independent-wallet-source
```

The source must have independent Git history; another clone/worktree of this
repository is rejected. The adapter runs in that clean source checkout and must
build or export the inspected wallet package during this execution. Its
environment contains:

| Variable | Meaning |
|---|---|
| EE_ACCEPTANCE_RUN_ID | Fresh UUID to echo in the report |
| EE_ACCEPTANCE_CHALLENGE | Fresh challenge that must be in the request and verifier observation |
| EE_ACCEPTANCE_OUTPUT | Initially absent evidence.json path in a fresh private directory |
| EE_ACCEPTANCE_OUTPUT_DIR | Directory for this execution's artifacts |

Write one JSON object to EE_ACCEPTANCE_OUTPUT:

```json
{
  "schema": "ee-estonian-external/1",
  "run_id": "<environment run id>",
  "challenge": "<environment challenge>",
  "source_revision": "<full 40-character canonical clean source Git revision>",
  "wallet_build": {
    "run_id": "<environment run id>",
    "challenge": "<environment challenge>",
    "source_path": "<absolute independent checkout path>",
    "source_revision": "<same full clean revision>",
    "adapter_sha256": "<SHA-256 of executed adapter>",
    "package_id": "<inspectable wallet application/build identity>",
    "built_at": "<UTC RFC3339 timestamp during adapter execution>",
    "artifact": {"path": "wallet.apk", "sha256": "<SHA-256 of inspected package>"}
  },
  "records": [
    {
      "case_id": "foreign-pid",
      "state": "PASS",
      "outcome": "presentation-accepted",
      "session": {
        "id": "<new unique observed verifier session>",
        "challenge": "<environment challenge>",
        "credential_kind": "PID",
        "wallet": "<named independent wallet/device/build>",
        "source_revision": "<same full source revision>",
        "wallet_build_sha256": "<same wallet package SHA-256>",
        "issuing_country": "<two-letter country other than EE>",
        "observed_at": "<UTC RFC3339 timestamp during adapter execution>"
      },
      "artifacts": {
        "request": {"path": "pid-request.json", "sha256": "<SHA-256>"},
        "response": {"path": "pid-response.json", "sha256": "<SHA-256>"},
        "verification": {"path": "pid-verification.json", "sha256": "<SHA-256>"}
      }
    },
    {
      "case_id": "foreign-av",
      "state": "NOT_EXECUTED",
      "operator_action": "Obtain authorized foreign AV access and execute a fresh wallet session."
    }
  ]
}
```

For each executed case, all three artifact JSON objects name session_id; the
request and verification include the fresh challenge. The response includes
received=true and verification includes valid=true for a PASS. Preserve observed
wire request/response data in each artifact's wire object. This adapter contract
uses an OpenID request with nonce equal to the fresh challenge and state equal
to the observed session id; the response preserves that state and a nonempty
vp_token. The verifier observation supplies verified_nonce, verified_session_id,
request_wire_sha256 and response_wire_sha256 from the actual verified exchange.
Wire digests use SHA-256 of UTF-8 JSON with sorted keys, compact separators,
unescaped Unicode and no trailing newline. A fresh wrapper around stale wire
cannot pass these checks. The verification names executed=true, the verifier build and
trust_input_sha256 (the actual public trust configuration digest). Status fields
alone cannot substitute wire and trust observations. Redact holder data before
publication. Every artifact is inside this fresh output directory,
hash-bound and at most 4 MiB before hashing or decoding. Nesting beyond 256 JSON
levels is a named error. The wallet package is streamed for hashing and its
identity/digest/execution binding is retained in the report; retain the package
in the operator evidence location for inspection. Both case rows are mandatory and must have distinct
fresh session ids; AV uses credential_kind AV. PASS requires actual acceptance.
FAIL and ERROR retain named observed outcomes and executed-session provenance.
NOT_EXECUTED requires a nonempty imperative operator_action. Source changes
during execution, reused timestamps/challenges, path escape, duplicate keys,
missing artifacts or an adapter that only exits zero cannot pass. The final
report retains the observed artifact contents and digests so temporary output
cleanup does not remove the evidence needed to review its decision.

An adapter contract is evidence intake, not independent certification of its
operator's assertions. Review raw artifacts, issuer trust and source provenance
before relying on an external result; never populate these fields from local
fixtures. The runner does not resolve the outstanding foreign-AV legal question.

## Operator handoff

1. Obtain a genuine foreign PID wallet and the issuer trust/access prerequisites
   through an authorized operator; execute and retain a fresh session.
2. Obtain genuine foreign AV credentials and authorized service/device access;
   establish the appropriate use basis with the competent parties and execute a
   separate fresh session. Do not infer legal permission from this PoC.
3. Implement and review the adapter above against those actual sessions, rerun
   the complete command, inspect both named outcomes and preserve the JSON plus
   raw artifacts in an operator-controlled evidence location.

These human-only obligations remain awaiting-operator. CI runs fast integrity
controls, actual linked local process/proof controls and wallet JVM suites in
their respective jobs; it supplies no foreign credentials and cannot claim
foreign acceptance.
