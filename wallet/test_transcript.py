#!/usr/bin/env python3
"""Cross-language golden vectors for the OpenID4VP 1.0 SessionTranscript.

The verifier is in Go and the holder is here in Python, and the two derive the
transcript independently. It is never transmitted — it is an input to the ZK
circuit — so if they disagree by one byte every proof fails verification with an
error that says nothing about why. The agreement has to be pinned by a test.

The same vectors are asserted from the Go side in
`verifier/go/oid4vp/transcript_vectors_test.go`. Both files must be updated
together, and neither may be regenerated from the implementation it is testing:
to change a vector, compute it from the *other* language and paste it in.

    python3 test_transcript.py           # run the checks
    python3 test_transcript.py --print   # print the vectors, to paste into Go
"""

import hashlib
import importlib.util
import json
import pathlib
import sys

CLIENT_ID = "x509_san_dns:verifier.example.ee"
NONCE = "s1U6zVsPQ0GQ0hZ4mQ0h0w"
RESPONSE_URI = "https://verifier.example.ee/present/response/abc"

# direct_post: the response is not encrypted, so jwkThumbprint is null.
UNENCRYPTED = (
    "83f6f682714f70656e494434565048616e646f766572582034"
    "08b9522985938a5b2501c88b62866ab854c42b62a7e8be4e257d5b0f674cef"
)

# direct_post.jwt: jwkThumbprint is the RFC 7638 SHA-256 thumbprint of the
# verifier's response-encryption key. Any 32 bytes exercise the encoding.
ENCRYPTED = (
    "83f6f682714f70656e494434565048616e646f766572582047"
    "afacda2c896b96bc0d1062e2b81345e504d3ec2a4c7d521458360953bf662e"
)

# direct_post.jwt with a REAL key: the RFC 7638 thumbprint of the EC P-256
# key below (public half of RFC 7520 §5.5 Figure 120), hashed over the
# {"crv","kty","x","y"} member set. The Go side asserts the same bytes in
# verifier/go/oid4vp/transcript_jwk_test.go from the same JWK literal — the
# two-sided pin for a key a wallet would actually publish.
JWK_X = "Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0"
JWK_Y = "HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw"
REAL_JWK_THUMBPRINT = hashlib.sha256(json.dumps(
    {"crv": "P-256", "kty": "EC", "x": JWK_X, "y": JWK_Y},
    separators=(",", ":"), sort_keys=True).encode()).digest()
ENCRYPTED_REAL_KEY = (
    "83f6f682714f70656e494434565048616e646f76657258202a"
    "498fe27b5e6a0f68911225669f840203824880aa67678612cfb6064f759f03"
)

# A stand-in for a real JWK thumbprint, derived the same way on both sides so
# the vector is reproducible without shipping a key.
THUMBPRINT = hashlib.sha256(b"EE-EUDIW transcript test vector").digest()


def _load_present():
    """Import present.py by path; it is a script, not an installed module."""
    path = pathlib.Path(__file__).resolve().parent / "present.py"
    spec = importlib.util.spec_from_file_location("present", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def main() -> int:
    present = _load_present()
    got_plain = present.session_transcript(CLIENT_ID, NONCE, None, RESPONSE_URI).hex()
    got_enc = present.session_transcript(CLIENT_ID, NONCE, THUMBPRINT, RESPONSE_URI).hex()
    got_real = present.session_transcript(
        CLIENT_ID, NONCE, REAL_JWK_THUMBPRINT, RESPONSE_URI).hex()

    if "--print" in sys.argv:
        print(f"thumbprint      : {THUMBPRINT.hex()}")
        print(f"unencrypted     : {got_plain}")
        print(f"encrypted       : {got_enc}")
        print(f"real-jwk thumb  : {REAL_JWK_THUMBPRINT.hex()}")
        print(f"encrypted (jwk) : {got_real}")
        return 0

    failures = []
    if got_plain != UNENCRYPTED:
        failures.append(f"direct_post transcript:\n  got  {got_plain}\n  want {UNENCRYPTED}")
    if got_enc != ENCRYPTED:
        failures.append(f"direct_post.jwt transcript:\n  got  {got_enc}\n  want {ENCRYPTED}")
    if UNENCRYPTED == ENCRYPTED:
        failures.append("the two vectors are identical; the jwkThumbprint is not reaching the transcript")
    # The real-key vector: same JWK literal the Go test thumbprints, so this
    # pins the wallet against transcript_jwk_test.go, not just the stand-in.
    if got_real != ENCRYPTED_REAL_KEY:
        failures.append(
            "direct_post.jwt (real RFC 7638 thumbprint) transcript:\n"
            f"  got  {got_real}\n  want {ENCRYPTED_REAL_KEY}")
    if REAL_JWK_THUMBPRINT.hex() != (
            "1ec4856a5c30df23fe74efa558662015cc95e47db6a1270815ce15d98e863ada"):
        failures.append("the RFC 7638 thumbprint of the pinned JWK is not the published vector")

    if failures:
        print("FAIL — this wallet does not agree with the Go verifier:", file=sys.stderr)
        for f in failures:
            print("  " + f.replace("\n", "\n  "), file=sys.stderr)
        return 1

    print("ok — 3 transcript vectors match the Go verifier")
    return 0


if __name__ == "__main__":
    sys.exit(main())
