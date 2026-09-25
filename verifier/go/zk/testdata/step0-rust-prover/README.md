# step0-rust-prover fixture

Minted by `issuer/mint_ee_poa.py` with a throwaway issuer key.

`device_key.pem` is a real P-256 private key (PKCS#8), committed on purpose:
the holder key the fixture's mdoc binds, so tests can re-sign `deviceAuth`.
It is test material only. It protects nothing, and nothing outside this
directory trusts it. Never reuse it.
