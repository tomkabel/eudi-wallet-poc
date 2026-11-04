#!/usr/bin/env python3
"""Checks for the mint's trust chain and key custody (PKI plan NEW-S2-1, NEW-S4-2).

- The DS certificate is issued by the IACA in issuers.json, under the ISO DS
  profile, and its key is the pinned pkx/pky.
- A persisted key outside --test-mode needs a passphrase and must be encrypted;
  an unencrypted one is refused.

    python3 issuer/test_mint_custody.py
"""
import base64
import json
import os
import pathlib
import subprocess
import sys
import tempfile

from cryptography import x509
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec

MINT = pathlib.Path(__file__).resolve().parent / "mint_ee_poa.py"


def mint(out, *extra, env=None):
    return subprocess.run([sys.executable, str(MINT), "--out", str(out), "--over", "16", "18", "21", *extra],
                          capture_output=True, text=True, env={**os.environ, **(env or {})})


def main() -> int:
    with tempfile.TemporaryDirectory() as tmp:
        tmp = pathlib.Path(tmp)
        run = mint(tmp / "a")
        assert run.returncode == 0, run.stderr
        entry = json.loads((tmp / "a/issuers.json").read_text())["issuers"][0]
        iaca = x509.load_der_x509_certificate(base64.b64decode(entry["iaca"]))
        dsc = x509.load_pem_x509_certificate((tmp / "a/issuer_dsc.pem").read_bytes())
        dsc.verify_directly_issued_by(iaca)
        assert dsc.issuer != dsc.subject, "DSC is still self-signed"
        assert iaca.extensions.get_extension_for_class(x509.BasicConstraints).value.ca
        eku = dsc.extensions.get_extension_for_class(x509.ExtendedKeyUsage).value
        assert x509.ObjectIdentifier("1.0.18013.5.1.2") in eku, "DSC lacks the mdlDS EKU"
        nums = dsc.public_key().public_numbers()
        assert (f"0x{nums.x:064x}", f"0x{nums.y:064x}") == (entry["pkx"], entry["pky"])

        keys = dict(issuer=str(tmp / "issuer.pem"), iaca=str(tmp / "iaca.pem"))
        persist = ["--issuer-key", keys["issuer"], "--iaca-key", keys["iaca"]]

        run = mint(tmp / "b", *persist, env={"EE_MINT_KEY_PASSPHRASE": ""})
        assert run.returncode != 0 and "EE_MINT_KEY_PASSPHRASE" in run.stderr, run.stderr
        assert not os.path.exists(keys["issuer"]), "a refused run wrote a key"

        run = mint(tmp / "b", "--issuer-key", keys["issuer"], "--test-mode")
        assert run.returncode == 2 and "go together" in run.stderr, run.stderr

        run = mint(tmp / "b", *persist, env={"EE_MINT_KEY_PASSPHRASE": "pw"})
        assert run.returncode == 0, run.stderr
        assert b"ENCRYPTED PRIVATE KEY" in pathlib.Path(keys["issuer"]).read_bytes()
        run = mint(tmp / "c", *persist, env={"EE_MINT_KEY_PASSPHRASE": "pw"})
        assert run.returncode == 0, run.stderr
        b = json.loads((tmp / "b/issuers.json").read_text())["issuers"][0]
        c = json.loads((tmp / "c/issuers.json").read_text())["issuers"][0]
        assert (b["pkx"], b["pky"]) == (c["pkx"], c["pky"]), "persisted DS key changed between runs"
        later = x509.load_pem_x509_certificate((tmp / "c/issuer_dsc.pem").read_bytes())
        later.verify_directly_issued_by(x509.load_der_x509_certificate(base64.b64decode(b["iaca"])))

        plain = tmp / "plain.pem"
        plain.write_bytes(ec.generate_private_key(ec.SECP256R1()).private_bytes(
            serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        run = mint(tmp / "d", "--issuer-key", str(plain), "--iaca-key", keys["iaca"],
                   env={"EE_MINT_KEY_PASSPHRASE": "pw"})
        assert run.returncode != 0 and "unencrypted" in run.stderr, run.stderr
        run = mint(tmp / "d", "--issuer-key", str(plain), "--iaca-key", str(tmp / "iaca2.pem"), "--test-mode",
                   env={"EE_MINT_KEY_PASSPHRASE": ""})
        assert run.returncode == 0, run.stderr

    print("ok — IACA-signed DSC with the ISO profile, encrypted custody outside --test-mode")
    return 0


if __name__ == "__main__":
    sys.exit(main())
