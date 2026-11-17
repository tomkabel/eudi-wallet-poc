#!/usr/bin/env python3
"""Mint the shared trust vectors (PKI plan NEW-S2-1, NEW-S2-4).

Writes tests/vectors/trust/cases.json: certificate chains with the outcome both
engines must reach. The Go verifier (oid4vp.VerifyChain) and the Android wallet
(CertificateChainValidator) each run every case, and CI fails when either one
disagrees with `expect`. That makes the file the differential suite: two engines,
one oracle.

Keys and signatures are fresh on each run, so the bytes change on regeneration.
The outcomes do not. Every case validates at the fixed instant AT, so nothing
here expires in wall-clock time.

    python3 tools/build_trust_vectors.py            # rewrite cases.json
"""
import base64
import datetime as dt
import json
import pathlib
import subprocess
import sys
import tempfile

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, rsa
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID, ObjectIdentifier

OUT = pathlib.Path(__file__).resolve().parent.parent / "tests/vectors/trust/cases.json"
AT = dt.datetime(2030, 1, 1, tzinfo=dt.timezone.utc)
VALID = (dt.datetime(2025, 1, 1, tzinfo=dt.timezone.utc), dt.datetime(2035, 1, 1, tzinfo=dt.timezone.utc))
MDL_DS = ObjectIdentifier("1.0.18013.5.1.2")  # ISO/IEC 18013-5 id-mdl-kp-mdlDS


def name(cn):
    return x509.Name([x509.NameAttribute(NameOID.COUNTRY_NAME, "EE"),
                      x509.NameAttribute(NameOID.ORGANIZATION_NAME, "PKI-VECTOR TEST"),
                      x509.NameAttribute(NameOID.COMMON_NAME, cn)])


def p256():
    return ec.generate_private_key(ec.SECP256R1())


def cert(cn, key, issuer=None, *, ca=False, path_len=None, ku="ds", eku=(MDL_DS,),
         validity=VALID, hash_alg=None):
    """issuer is (cert, key) or None for self-signed."""
    icert, ikey = issuer if issuer else (None, key)
    b = (x509.CertificateBuilder()
         .subject_name(name(cn))
         .issuer_name(icert.subject if icert else name(cn))
         .public_key(key.public_key())
         .serial_number(x509.random_serial_number())
         .not_valid_before(validity[0]).not_valid_after(validity[1])
         .add_extension(x509.BasicConstraints(ca=ca, path_length=path_len), critical=True)
         .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()), critical=False))
    if icert:
        b = b.add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(ikey.public_key()), critical=False)
    if ku == "ca":
        b = b.add_extension(x509.KeyUsage(False, False, False, False, False, True, True, False, False), critical=True)
    elif ku == "ds":
        b = b.add_extension(x509.KeyUsage(True, False, False, False, False, False, False, False, False), critical=True)
    elif ku == "certsign-only":
        b = b.add_extension(x509.KeyUsage(False, False, False, False, False, True, False, False, False), critical=True)
    if eku:
        b = b.add_extension(x509.ExtendedKeyUsage(list(eku)), critical=True)
    if hash_alg is None:
        hash_alg = hashes.SHA256()
    return b.sign(ikey, hash_alg), key


def sha1_leaf(issuer):
    """ecdsa-with-SHA1 leaf under issuer. cryptography >= 47 refuses SHA-1
    signing, so openssl signs this one."""
    icert, ikey = issuer
    with tempfile.TemporaryDirectory() as d:
        d = pathlib.Path(d)
        pem = serialization.Encoding.PEM
        (d / "ca.pem").write_bytes(icert.public_bytes(pem))
        (d / "ca.key").write_bytes(ikey.private_bytes(pem, serialization.PrivateFormat.PKCS8,
                                                       serialization.NoEncryption()))
        (d / "leaf.key").write_bytes(p256().private_bytes(pem, serialization.PrivateFormat.PKCS8,
                                                          serialization.NoEncryption()))
        (d / "ext.cnf").write_text("basicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature\n"
                                   "extendedKeyUsage=critical,1.0.18013.5.1.2\n")
        run = lambda *a: subprocess.run(a, cwd=d, check=True, capture_output=True)
        run("openssl", "req", "-new", "-key", "leaf.key", "-subj", "/C=EE/O=PKI-VECTOR TEST/CN=SHA1 DS",
            "-out", "leaf.csr")
        run("openssl", "x509", "-req", "-in", "leaf.csr", "-CA", "ca.pem", "-CAkey", "ca.key", "-sha1",
            "-set_serial", "4242", "-not_before", "20250101000000Z", "-not_after", "20350101000000Z",
            "-extfile", "ext.cnf", "-out", "leaf.pem")
        return x509.load_pem_x509_certificate((d / "leaf.pem").read_bytes())


def iaca(cn="Vector IACA", path_len=0, key=None):
    return cert(cn, key or p256(), ca=True, path_len=path_len, ku="ca", eku=())


def der(c):
    return base64.b64encode(c.public_bytes(serialization.Encoding.DER)).decode()


def case(name_, profile, chain, anchors, expect, why):
    return {"name": name_, "profile": profile, "chain": [der(c) for c in chain],
            "anchors": [der(c) for c in anchors], "expect": expect, "why": why}


def build():
    root = iaca()
    ds = cert("Vector DS", p256(), root)
    out = [
        case("ds_valid", "mdoc_ds", [ds[0]], [root[0]], "accept", "DS under the IACA, ISO profile"),
        case("ds_valid_generic", "generic", [ds[0]], [root[0]], "accept", "same chain, generic profile"),
        case("ds_expired", "mdoc_ds",
             [cert("Expired DS", p256(), root, validity=(dt.datetime(2020, 1, 1, tzinfo=dt.timezone.utc),
                                                          dt.datetime(2025, 1, 1, tzinfo=dt.timezone.utc)))[0]],
             [root[0]], "refuse", "DS notAfter before AT"),
        case("ds_not_yet_valid", "mdoc_ds",
             [cert("Future DS", p256(), root, validity=(dt.datetime(2031, 1, 1, tzinfo=dt.timezone.utc),
                                                         dt.datetime(2035, 1, 1, tzinfo=dt.timezone.utc)))[0]],
             [root[0]], "refuse", "DS notBefore after AT"),
    ]
    wrong_eku = cert("Server DS", p256(), root, eku=(ExtendedKeyUsageOID.SERVER_AUTH,))[0]
    out += [
        case("ds_wrong_eku", "mdoc_ds", [wrong_eku], [root[0]], "refuse", "EKU serverAuth, not mdlDS"),
        case("ds_wrong_eku_generic", "generic", [wrong_eku], [root[0]], "accept", "generic profile ignores EKU"),
        case("ds_no_eku", "mdoc_ds", [cert("No-EKU DS", p256(), root, eku=())[0]], [root[0]], "refuse",
             "DS without the mdlDS EKU"),
        case("ds_no_digital_signature", "mdoc_ds", [cert("KU DS", p256(), root, ku="certsign-only")[0]],
             [root[0]], "refuse", "keyUsage lacks digitalSignature"),
        case("ds_is_ca", "mdoc_ds", [cert("CA DS", p256(), root, ca=True, path_len=0, ku="ds")[0]], [root[0]],
             "refuse", "DS marked CA:TRUE"),
        case("ds_foreign_root", "mdoc_ds", [cert("Foreign DS", p256(), iaca("Foreign IACA"))[0]], [root[0]],
             "refuse", "DS under an IACA that is not an anchor"),
        case("ds_self_signed", "mdoc_ds", [cert("Self DS", p256())[0]], [root[0]], "refuse",
             "self-signed DS, not under the IACA"),
    ]
    sub0 = cert("Sub CA", p256(), root, ca=True, path_len=0, ku="ca", eku=())
    out.append(case("pathlen_violated", "mdoc_ds", [cert("Deep DS", p256(), sub0)[0], sub0[0]], [root[0]],
                    "refuse", "IACA pathLen 0 forbids the sub-CA"))
    root1 = iaca("Vector IACA pathLen 1", path_len=1)
    sub1 = cert("Sub CA 1", p256(), root1, ca=True, path_len=0, ku="ca", eku=())
    out.append(case("intermediate_ok", "mdoc_ds", [cert("Sub DS", p256(), sub1)[0], sub1[0]], [root1[0]],
                    "accept", "IACA pathLen 1 allows one sub-CA"))
    out += [
        case("policy_sha1", "generic", [sha1_leaf(root)], [root[0]],
             "refuse", "ecdsa-with-SHA1 is not in the algorithm policy"),
        case("policy_rsa1024", "generic", [cert("RSA1024 DS", rsa.generate_private_key(65537, 1024), root)[0]],
             [root[0]], "refuse", "RSA-1024 is below the policy minimum"),
        case("policy_p521", "generic", [cert("P521 DS", ec.generate_private_key(ec.SECP521R1()), root)[0]],
             [root[0]], "refuse", "P-521 is not a policy curve"),
        case("policy_p384_sha384", "generic",
             [cert("P384 DS", ec.generate_private_key(ec.SECP384R1()), root, hash_alg=hashes.SHA384())[0]],
             [root[0]], "accept", "P-384 key, ecdsa-with-SHA384: both listed"),
    ]
    rsa_root = cert("RSA Root", rsa.generate_private_key(65537, 2048), ca=True, path_len=0, ku="ca", eku=())
    out.append(case("policy_rsa2048", "generic", [cert("RSA2048 leaf", rsa.generate_private_key(65537, 2048),
                                                        rsa_root)[0]], [rsa_root[0]], "accept",
                    "RSA-2048 with sha256WithRSAEncryption meets the policy"))
    return out


def main():
    cases = build()
    doc = {"_generated_by": "tools/build_trust_vectors.py", "at": AT.strftime("%Y-%m-%dT%H:%M:%SZ"),
           "profiles": {"mdoc_ds": "ISO/IEC 18013-5 DS: EKU 1.0.18013.5.1.2, keyUsage digitalSignature, not a CA",
                        "generic": "RFC 5280 path validation plus the algorithm policy"},
           "cases": cases}
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(doc, indent=1) + "\n")
    print(f"wrote {len(cases)} cases to {OUT}", file=sys.stderr)


if __name__ == "__main__":
    main()
