# step0-rust-prover fixture

Minted by `issuer/mint_ee_poa.py` with a throwaway issuer key.

The holder's device private key is not committed. No test reads it: the Go test
(`../../step0_cross_verify_test.go`) verifies `proof.bin` against the committed
`mdoc.bin`, which binds only the device public key. A commit before this
change carried the key as `device_key.pem` (test material, never bound to a real
credential); it remains in git history. To re-sign `deviceAuth`, mint a fresh
fixture with `issuer/mint_ee_poa.py`, which writes the key under the gitignored
`demo-data/`.
