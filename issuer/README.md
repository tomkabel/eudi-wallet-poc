# EE-PoA issuer (demo)

Mints a proof-of-age attestation as an ISO/IEC 18013-5 `DeviceResponse`, signed by a key
generated here, so the ZK demo runs end to end on **our own attestation** rather than on
Google's mDL test vector.

```bash
pip install cbor2 cryptography
python3 mint_ee_poa.py --out out
```

```text
doctype      : ee.riik.poa.1
elements     : age_over_18, issuing_country, issuing_authority, expiry_date
validity     : 2026-09-07T00:00:00Z .. 2026-12-06T00:00:00Z  (shared across the batch, EE-POA-012)
issuer pk x  : 0x058d800692dc62805e6980e72c096102c7b480e313a189964b434e8dc084cc42

  [000] MSO 436 B, DeviceResponse 1663 B -> out/mdoc.bin
```

Then prove `age_over_18` over it, disclosing nothing else — see
[`../zk-age-poc/ee_poa_demo.rs`](../zk-age-poc/ee_poa_demo.rs):

```text
disclosing : age_over_18 — and nothing else
circuit    : 8d079211…16182121 (480913 bytes) in 5.4 ms
PROVE      : 360212 bytes in 6.98 s
VERIFY     : Ok("ok")  in 2.79 s
VERIFY(wrong issuer key): Err(GeneralFailure)
VERIFY(tampered proof)  : Err(GeneralFailure)
```

## Batches

`--batch N` mints N single-use attestations (`EE-POA-010`/`EE-POA-011`) into `out/000 …`,
each with its own device key and its own salts, all sharing one issuer key and one
coarsened `ValidityInfo`:

```console
$ python3 mint_ee_poa.py --out out --batch 30
...
  [000] MSO 506 B, DeviceResponse 1932 B -> out/000/mdoc.bin
  ...
batch        : 30 single-use attestations (EE-POA-010/011)
  distinct issuer signatures : 30/30   ok
  distinct ValidityInfo      : 1   ok (identical across batch)
```

Those two lines are the property being claimed. No two members share a signature, so
signature reuse cannot link them; every member is byte-identical in `ValidityInfo`, so
issuance time cannot link them either. What this does **not** buy is unlinkability
against the issuer, who saw the whole batch leave — that is what the ZK path in
[`../zk-age-poc`](../zk-age-poc) is for.

`EE-POA-013` requires each attestation to be presented at most once and then deleted by
the holder. That is the wallet's job, not the issuer's, and this repository's
[`../wallet`](../wallet) does not do it — see its own limitations section.

## What it builds

`DeviceResponse` → `documents[0]` → `issuerSigned` { `nameSpaces`, `issuerAuth` } +
`deviceSigned` { `nameSpaces`, `deviceAuth` }.

- **IssuerSignedItem** per element: `{digestID, random(32), elementIdentifier, elementValue}`,
  each wrapped in CBOR tag 24 and hashed into `MSO.valueDigests`.
- **MSO** with ISO key order `version, digestAlgorithm, docType, valueDigests, deviceKeyInfo,
  validityInfo`. The circuit requires the MSO map to be **≥ 256 bytes** so that its tag-24
  wrapper uses the two-byte length form `d8 18 59 <u16>`; the script refuses to emit a
  smaller one.
- **issuerAuth** — COSE_Sign1 over `Tag24(MSO)`, ES256, with a self-signed document signer
  certificate in the `x5chain` (label 33) unprotected header.
- **deviceSignature** — COSE_Sign1 over `Tag24(DeviceAuthentication)` where
  `DeviceAuthentication = ["DeviceAuthentication", SessionTranscript, docType, DeviceNameSpacesBytes]`,
  detached payload, ES256.

## Spec conformance, and where it deliberately stops

Follows EE-EUDIW-TS-1.0:

- **EE-POA-001** — boolean age predicates only. No `birth_date`, no `age_in_years`, no `sex`,
  no name, no portrait, no `personal_administrative_number`.
- **EE-PID-007** — nothing in the attestation is derived from or correlatable to an `isikukood`.
- **EE-POA-012** — `signed`, `validFrom` and `validUntil` are coarsened to midnight UTC, so a
  batch cannot be correlated by timestamp.
- **EE-POA-016** — validity defaults to 90 days and the flag is capped in intent at three months.

Not yet implemented, and named rather than hidden:

- **EE-POA-006/007** — predicates here come from CLI flags, not from the Population Register
  over X-tee, and no ICAO passive authentication is performed. This is exactly the
  trust-the-client enrolment weakness the spec warns about in §9.4; a real issuer must compute
  the predicate server-side from the authentic source.
- **EE-POA-010** — `--batch N` mints the batch, but it writes files to a directory. There is
  no OpenID4VCI 1.0 issuance here, so no `proofs` parameter and no key attestation covering
  the key set, which is the rest of what the requirement asks for.
- The device key is generated in software, not in a WSCD (**EE-SEC-001**). With
  `--device-public-key KEY` the mint instead binds the attestation to a key the
  wallet already generated inside its WSCD — hex (`0x04||x||y` or bare
  coordinates) or PEM — and writes `device_public_key.pem`; no private half is
  imported and the minted `deviceSignature` is an all-zero placeholder the
  wallet re-signs at presentation time (the circuits verify the device
  signature inside the proof, so nothing can prove over the placeholder). It
  binds one key, so it is refused with `--batch N > 1` (**EE-POA-010** wants a
  distinct key per attestation).

The WSCD path itself — a real Android holder generating those keys in
`AndroidKeystoreSecureArea`, issuing `ee.riik.poa.1` over them in one
transaction, and consuming an attestation on plain presentation — lives in the
independent fork [`tomkabel/eudi-wallet-poc`](https://github.com/tomkabel/eudi-wallet-poc) at
`step8-measurement-harness @ 2396245` (branch chain `step1-fork-identity` →
`step3-holder-obligations` → `step5-hardware-keys` → `step4-protocol-hygiene` →
`step6-issuance-consumption` → `step7-conformity-note` → `step8-measurement-harness`, by commit as
recorded in `../docs/planning/STEP7-RECORD.md` and `../docs/planning/STEP8-RECORD.md`); see [`docs/ARCHITECTURE.md` §6](../docs/ARCHITECTURE.md) and the
fork's `docs/CONFORMITY.md`. The device work (batch of ≥ 30, key-generation timing, slot
capacity — spec §23 item 26) is pending.

## Note on `-C target-cpu=native`

`google/longfellow-zk`'s `rust/.cargo/config.toml` sets `-C target-cpu=native`. On the
virtualised host used here that produces a binary which dies with **SIGILL** — the CPU
advertises AVX-512 (f/dq/cd/bw/vl/vnni) but at least one instruction `native` selects traps.
Building with `-C target-cpu=x86-64-v3` is stable:

```bash
# A fresh longfellow-zk clone has no examples/ directory, so the mkdir is not optional.
mkdir -p ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples
cp zk-age-poc/*.rs ../longfellow-zk/rust/applications/mdoc_zk/runtime/examples/
cd ../longfellow-zk/rust/applications/mdoc_zk/runtime
RUSTFLAGS="-C target-cpu=x86-64-v3" cargo build --release --example ee_poa_demo --features testonly
```

Without the `mkdir` the `cp` fails. `--example ee_poa_demo` then fails too, loudly — but the
plural `cargo build --examples` used by CI matches no targets, prints a warning and **exits
0**, which looks exactly like a build that worked. `.github/workflows/ci.yml` therefore asserts
`test -x` on the binary afterwards; do the same if you script this.

Earlier in the same session a `native` build proved in 0.74 s against 6.98 s for `x86-64-v3`,
so the vector width is worth real money on a server-side prover — but those two measurements
straddle the point at which `native` stopped running here, so treat the ratio as indicative
rather than a controlled benchmark.
