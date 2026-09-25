#!/usr/bin/env python3
"""Holder side of an OpenID4VP 1.0 presentation, driven from the command line.

    python3 present.py --credential ../issuer/out/000 --verifier http://127.0.0.1:8080

Fetches the authorization request, derives the session transcript from it,
re-signs deviceAuth over that transcript, proves the requested predicate in zero
knowledge, and posts the vp_token.

The point of re-signing is that a presentation must be bound to *this* verifier
and *this* nonce. The issuer never sees the session, so the device signature it
shipped at issuance is a placeholder; the holder replaces it here, which is what
a real wallet does inside its WSCD.
"""
import argparse, base64, hashlib, json, os, shutil, subprocess, sys, tempfile
import urllib.error
import urllib.request
import cbor2
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, utils as asym_utils

SIG_STRUCT_PREFIX = bytes.fromhex("846a5369676e61747572653143a1012640")
DEVICE_AUTH_HEADER = bytes.fromhex("8474") + b"DeviceAuthentication"
DEVICE_NAMESPACES_BYTES = bytes.fromhex("d81841a0")


def bstr_header(n: int) -> bytes:
    if n < 24:      return bytes([0x40 | n])
    if n < 0x100:   return bytes([0x58, n])
    if n < 0x10000: return bytes([0x59]) + n.to_bytes(2, "big")
    return bytes([0x5A]) + n.to_bytes(4, "big")


def tstr_header(n: int) -> bytes:
    if n < 24:    return bytes([0x60 | n])
    if n < 0x100: return bytes([0x78, n])
    return bytes([0x79]) + n.to_bytes(2, "big")


def session_transcript(client_id: str, nonce: str, jwk_thumbprint, response_uri: str) -> bytes:
    """OpenID4VP 1.0 Appendix B.2.6.1.

    SessionTranscript = [null, null, OpenID4VPHandover]
    OpenID4VPHandover = ["OpenID4VPHandover", sha256(OpenID4VPHandoverInfoBytes)]
    OpenID4VPHandoverInfo = [clientId, nonce, jwkThumbprint, responseUri]

    jwkThumbprint is null unless the response is encrypted (direct_post.jwt).
    Must produce the same bytes as verifier/go/oid4vp/transcript.go.
    """
    info = cbor2.dumps([client_id, nonce, jwk_thumbprint, response_uri], canonical=True)
    digest = hashlib.sha256(info).digest()
    return cbor2.dumps([None, None, ["OpenID4VPHandover", digest]], canonical=True)


def resign_device_auth(mdoc: bytes, device_key, transcript: bytes, doc_type: str) -> bytes:
    """Replace deviceSignature so it covers this session's transcript."""
    doc = cbor2.loads(mdoc)
    device_auth_cbor = (DEVICE_AUTH_HEADER + transcript +
                        tstr_header(len(doc_type)) + doc_type.encode() +
                        DEVICE_NAMESPACES_BYTES)
    payload = bytes.fromhex("d818") + bstr_header(len(device_auth_cbor)) + device_auth_cbor
    der = device_key.sign(SIG_STRUCT_PREFIX + bstr_header(len(payload)) + payload,
                          ec.ECDSA(hashes.SHA256()))
    r, s = asym_utils.decode_dss_signature(der)
    sig = r.to_bytes(32, "big") + s.to_bytes(32, "big")
    doc["documents"][0]["deviceSigned"]["deviceAuth"]["deviceSignature"][3] = sig
    return cbor2.dumps(doc)


def http_json(url: str, payload=None, method=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method=method,
                                 headers={"Content-Type": "application/json"} if data else {})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, json.load(e)
    except (urllib.error.URLError, TimeoutError) as e:
        raise SystemExit(f"verifier unreachable at {url}: {e}") from e


def main() -> None:
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--credential", required=True,
                    help="directory holding mdoc.bin, params.txt and device_key.pem")
    ap.add_argument("--verifier", default="http://127.0.0.1:8080",
                    help="verifier base URL")
    ap.add_argument("--prover", default=os.environ.get(
        "EE_PROVER", os.path.expanduser("~/longfellow-zk/rust/target/release/examples/ee_poa_demo")),
        help="path to the ee_poa_demo prover binary")
    ap.add_argument("--element", default="age_over_18", help="predicate to disclose")
    ap.add_argument("--tamper-nonce", action="store_true",
                    help="prove against a different nonce, to show the binding holds")
    ap.add_argument("--replay", action="store_true",
                    help="post the same vp_token twice, to show the nonce is single-use")
    ap.add_argument("--allow-false-predicate", action="store_true",
                    help="prove a predicate the attestation says is false, to show the "
                         "verifier refuses it on its own")
    ap.add_argument("--keep", action="store_true", help="keep the temporary session directory")
    args = ap.parse_args()

    if not os.path.exists(args.prover):
        sys.exit(f"prover not found at {args.prover}; pass --prover or set EE_PROVER")

    # 1. Ask the verifier for a presentation request.
    status, started = http_json(f"{args.verifier}/present/new", {"element": args.element})
    if status != 201:
        sys.exit(f"could not start a presentation: {status} {started}")
    status, req = http_json(started["request_uri"])
    if status != 200:
        sys.exit(f"could not fetch the request object: {status} {req}")
    cq = req["dcql_query"]["credentials"][0]
    namespace, element = cq["claims"][0]["path"]
    print(f"request    : {cq['meta']['doctype_value']} / {namespace} / {element}")
    print(f"client_id  : {req['client_id']}")
    print(f"nonce      : {req['nonce'][:16]}…")

    # 2. Derive the transcript this presentation must be bound to.
    nonce = "A" * 43 if args.tamper_nonce else req["nonce"]
    transcript = session_transcript(req["client_id"], nonce, None, req["response_uri"])
    print(f"transcript : {len(transcript)} bytes, sha256 {hashlib.sha256(transcript).hexdigest()[:16]}…"
          + ("   (DELIBERATELY WRONG NONCE)" if args.tamper_nonce else ""))

    # 3. Re-sign deviceAuth over it and prove.
    src = args.credential
    params = open(os.path.join(src, "params.txt")).read().splitlines()
    doc_type = params[3]
    device_key = serialization.load_pem_private_key(
        open(os.path.join(src, "device_key.pem"), "rb").read(), password=None)
    mdoc = resign_device_auth(open(os.path.join(src, "mdoc.bin"), "rb").read(),
                              device_key, transcript, doc_type)

    session_dir = tempfile.mkdtemp(prefix="ee-present-")
    try:
        open(os.path.join(session_dir, "mdoc.bin"), "wb").write(mdoc)
        open(os.path.join(session_dir, "transcript.bin"), "wb").write(transcript)
        # The verifier fixes `now`; the holder does not get to choose it.
        open(os.path.join(session_dir, "params.txt"), "w").write(
            "\n".join([params[0], params[1], req["expected_now"], *params[3:5]]) + "\n")

        cmd = [args.prover, session_dir, element]
        if args.allow_false_predicate:
            cmd.append("--allow-false-predicate")
        proc = subprocess.run(cmd, capture_output=True, text=True)
        if proc.returncode != 0:
            print(proc.stdout, proc.stderr, sep="\n")
            sys.exit("proving failed")
        for line in proc.stdout.splitlines():
            if line.startswith(("PROVE", "VERIFY   ")):
                print(f"  {line}")

        request_json = json.load(open(os.path.join(session_dir, "request.json")))
        proof = open(os.path.join(session_dir, "proof.bin"), "rb").read()
    finally:
        if not args.keep:
            shutil.rmtree(session_dir, ignore_errors=True)
        else:
            print(f"session dir: {session_dir}")

    # 4. Post the vp_token.
    presentation = {
        "zk_system": "longfellow-libzk-v1",
        "version": request_json["version"],
        "num_attributes": request_json["num_attributes"],
        "doc_type": doc_type,
        "namespace": namespace,
        "attr_id": element,
        "attr_cbor_hex": request_json["attr_cbor_hex"],
        "proof_b64": base64.b64encode(proof).decode(),
    }
    vp_token = {cq["id"]: [base64.urlsafe_b64encode(
        json.dumps(presentation).encode()).decode().rstrip("=")]}

    body = {"vp_token": vp_token, "expected_now": req["expected_now"]}
    status, result = http_json(req["response_uri"], body)
    print(f"\nresponse   : HTTP {status}")
    print(json.dumps(result, indent=2))

    if args.replay:
        status2, result2 = http_json(req["response_uri"], body)
        print(f"\nreplay     : HTTP {status2}")
        print(json.dumps(result2, indent=2))
        if status2 == 200:
            sys.exit("REPLAY ACCEPTED — the nonce was not single-use")

    sys.exit(0 if result.get("valid") else 1)


if __name__ == "__main__":
    main()
