# wallet — the holder side

Drives a full OpenID4VP presentation from the command line: fetch the request, derive the
session transcript, re-sign `deviceAuth` over it, prove the predicate in zero knowledge,
post the `vp_token`.

```bash
python3 present.py --credential /path/to/issued/000 --verifier http://127.0.0.1:8080
```

```text
request    : ee.riik.poa.1 / ee.riik.poa.1 / age_over_18
client_id  : x509_san_dns:verifier.example.ee
nonce      : 3PyBCV5JMTAE0pHW…
transcript : 56 bytes, sha256 ef101819b8d2719d…
  PROVE      : 360308 bytes in 6.25 s
  VERIFY     : Ok("ok") in 2.50 s

response   : HTTP 200
{ "detail": "age_over_18 = 0xf5, issued by EE-EUDIW demo issuer", "valid": true }
```

`--prover` (or `EE_PROVER`) points at the `ee_poa_demo` binary built from
[`../zk-age-poc`](../zk-age-poc).

## Why it re-signs

The issuer ships a `deviceSignature`, but the issuer has never seen the session. So that
signature is a placeholder: the holder replaces it with one over **this** presentation's
session transcript, which is what binds the proof to one verifier and one nonce. A real
wallet does this inside its WSCD and the device key never leaves it (`EE-SEC-001`,
`EE-SEC-003`); here it is a PEM file next to the credential, which is exactly the thing a
real wallet must not do.

`session_transcript()` mirrors `verifier/go/oid4vp/transcript.go` and the two are checked
against each other — the whole flow is worthless if they disagree by a byte. The check is
a pair of golden vectors asserted from both sides, covering `direct_post` (null
`jwkThumbprint`) and `direct_post.jwt` (a 32-byte one):

```bash
python3 test_transcript.py            # assert this wallet agrees with the Go verifier
python3 test_transcript.py --print    # print the vectors, to paste into the Go test
```

Neither side may regenerate a vector from the implementation it is testing.

## Showing the properties hold

```bash
python3 present.py --credential … --tamper-nonce   # prove against a different nonce
python3 present.py --credential … --replay         # post the same vp_token twice
```

The first is accepted by the local prover and rejected by the verifier, which recomputes
the transcript from its own session. The second returns `410 session already answered`.

## What this is not

A wallet. There is no UI, no user approval step, no dashboard, no attestation storage or
selection, no WSCD, and no batch management — the credential directory is passed in on the
command line. It exists to exercise the verifier and to pin down the transcript derivation.

It also does **not** consume the attestation. `EE-POA-013` requires the holder to delete an
attestation once it has been presented; `present.py` removes only its temporary proving
directory and leaves the credential where it found it, so the same one can be presented again.
That is the wallet's half of the once-only property the issuer's batching provides, and it is
not implemented here.

The real-holder work this directory deliberately is not — an Android wallet holding
`ee.riik.poa.1` in hardware-backed keys, consuming on plain presentation, showing the
`EE-ZKP-042` notice — lives in the independent fork
[`tomkabel/eudi-wallet-poc`](https://github.com/tomkabel/eudi-wallet-poc), at
`step8-measurement-harness @ 2396245` (branch chain `step1-fork-identity` →
`step3-holder-obligations` → `step5-hardware-keys` → `step4-protocol-hygiene` →
`step6-issuance-consumption` → `step7-conformity-note` → `step8-measurement-harness`, by commit as
recorded in `../docs/planning/STEP7-RECORD.md` and `../docs/planning/STEP8-RECORD.md`); see [`docs/ARCHITECTURE.md` §6](../docs/ARCHITECTURE.md) and
[`docs/CONFORMITY.md`](../docs/CONFORMITY.md). The device-side acceptance items of plan §6 remain pending.
