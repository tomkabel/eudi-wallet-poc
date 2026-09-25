# Reproducing a zero-knowledge `age_over_18` proof over an ISO 18013-5 mdoc

Measured 5 Sept 2026 on a 2-vCPU / 8 GB Linux cloud sandbox. No GPU, no special hardware.

```bash
git clone https://github.com/google/longfellow-zk.git
git -C longfellow-zk checkout --detach 61a8a735964d1b22bccf79bf14ef6767249cdf92
git -C longfellow-zk rev-parse HEAD
cd longfellow-zk/rust
rustc --version
cargo --version
cargo build --release -p mdoc-zk-runtime          # 50.6 s, no system deps
# A fresh clone has no examples/ directory, so the mkdir is not optional.
mkdir -p applications/mdoc_zk/runtime/examples
cp /path/to/ee-eudiw/zk-age-poc/*.rs applications/mdoc_zk/runtime/examples/
cd applications/mdoc_zk/runtime
cargo run --release --example age_demo --features testonly
```

Without the `mkdir` the `cp` fails. `--example age_demo` then fails too, loudly — but the
plural `cargo build --examples`, which is what `.github/workflows/ci.yml` runs, matches no
targets, prints a warning and **exits 0**. That looks exactly like a build that worked and
produces no binary, which is why the workflow asserts `test -x` on the binary afterwards.

The reported run used Longfellow revision
`61a8a735964d1b22bccf79bf14ef6767249cdf92`,
`rustc 1.97.0 (2d8144b78 2026-07-07) (Amazon Linux 1.97.0-2.amzn2023)`, and
`cargo 1.97.0 (c980f4866 2026-06-30) (Amazon Linux 1.97.0-2.amzn2023)`.

Output:

```text
attributes in test mdoc:
   age_over_18              cbor=[f5]
   nym                      cbor=[67, 31, 32, 33, 31, 32, 34, 34]

circuit  : 8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121
           (480913 bytes compressed) loaded in 4.84ms
PROVE    : 360276 bytes in 740ms
VERIFY   : Ok("ok") in 265ms
VERIFY(tampered): Err(GeneralFailure)
```

The demo does not report its own memory use, so peak RSS is measured by wrapping it. On this
machine `/usr/bin/time` is not installed, and the shell builtin `time` does not report memory,
so:

```bash
python3 -c 'import resource, subprocess, sys
subprocess.run([sys.argv[1]], check=True)
print("peak RSS : %.0f MB" % (resource.getrusage(resource.RUSAGE_CHILDREN).ru_maxrss / 1024))
' ../../../target/release/examples/age_demo
```

That prints `peak RSS : 112 MB` on an 8-vCPU host from a `-C target-cpu=x86-64-v3` build, which
is the same figure previously recorded for the 2-vCPU run above. That earlier figure came with
no documented command, which is why one is given here; the agreement is what you would expect,
since the working set is the circuit and the witness rather than anything the core count
changes. `ru_maxrss` is in KiB on Linux.

## What this proves

Given an ECDSA-signed ISO 18013-5 mdoc and the issuer's P-256 public key, the prover
produces a proof that:

1. the issuer's signature over the MSO is valid,
2. the requested attribute (`age_over_18`) is present with value CBOR `true`,
3. the holder can sign the session transcript with the attestation's device key,
4. the credential is inside its validity window,

**without revealing the mdoc, the signature, the salts, or any other attribute.**
Nothing linkable to the issuer's signature leaves the device, which is the property
ARF §7.4.3.5.3 says salted-hash selective disclosure cannot deliver.

## Notes

- `provider::materialize(7, 1)` loads a pre-built v7 circuit for 1 requested attribute.
  Circuits are per `(version, num_attributes)`; asking for 2 attributes needs a different
  ~200–500 KB blob. `CURRENT_ZK_SPECS` circuits are *not* shipped pre-built — they must be
  compiled with `mdoc_zk_compile::compile_all_circuits(...)`, which is slow. Use a prior
  spec for demos.
- The test vector lives in `mdoc_zk_testcases::vectors::TEST_DATA` — a complete fixture with
  issuer key, CBOR transcript, and an mdoc whose first element is `age_over_18 = true`.
  You do not need to mint an mdoc to get started.
- Proof size ~360 KB. Fine over HTTP; awkward over QR/NFC/BLE.
- `google/longfellow-zk` states two independent security reviews are still in progress.
  Do not ship this to production.
