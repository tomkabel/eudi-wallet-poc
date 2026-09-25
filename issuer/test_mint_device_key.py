#!/usr/bin/env python3
"""Checks for the mint's --device-public-key path.

The Go tests read static fixture bytes and no demo or e2e script passes
--device-public-key, so a swapped x/y slice or a wrong-length placeholder in
mint_ee_poa.py would otherwise ship silently.

    python3 issuer/test_mint_device_key.py
"""

import importlib.util
import pathlib
import subprocess
import sys
import tempfile

from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

MINT = pathlib.Path(__file__).resolve().parent / "mint_ee_poa.py"


def _load_mint():
    """Import mint_ee_poa.py by path; it is a script, not an installed module."""
    spec = importlib.util.spec_from_file_location("mint_ee_poa", MINT)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {MINT}")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def main() -> int:
    mint = _load_mint()
    pub = ec.generate_private_key(ec.SECP256R1()).public_key()
    want = pub.public_numbers()
    xy = want.x.to_bytes(32, "big").hex() + want.y.to_bytes(32, "big").hex()
    pem = pub.public_bytes(serialization.Encoding.PEM,
                           serialization.PublicFormat.SubjectPublicKeyInfo).decode()

    for form in ("0x04" + xy, "0X04" + xy, "04" + xy, xy, pem):
        got = mint.load_device_public_key(form).public_numbers()
        assert got == want, f"{form[:12]}…: parsed to a different key"

    p384 = ec.generate_private_key(ec.SECP384R1()).public_key().public_bytes(
        serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo).decode()
    try:
        mint.load_device_public_key(p384)
    except SystemExit:
        pass
    else:
        raise AssertionError("a P-384 key was accepted")

    assert mint.zero_device_sig() == bytes(64)

    # One holder key over a batch would make the batch linkable (EE-POA-010).
    with tempfile.TemporaryDirectory() as tmp:
        out = pathlib.Path(tmp) / "out"
        run = subprocess.run([sys.executable, str(MINT), "--out", str(out),
                              "--batch", "2", "--device-public-key", "0x04" + xy],
                             capture_output=True, text=True)
        assert run.returncode == 2, f"exit {run.returncode}: {run.stderr}"
        assert "--batch > 1" in run.stderr, run.stderr
        assert not out.exists(), "a refused batch wrote output"

    print("ok — device public key parsing, placeholder signature, batch refusal")
    return 0


if __name__ == "__main__":
    sys.exit(main())
