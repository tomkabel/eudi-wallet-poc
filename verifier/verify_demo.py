#!/usr/bin/env python3
"""POST a proof produced by zk-age-poc/ee_poa_demo.rs to the Go verifier.

    python3 verify_demo.py <dir> [--url http://127.0.0.1:8080/zkverify]

<dir> is the directory the prover wrote request.json, transcript.bin and
proof.bin into.

This drives POST /zkverify, which the verifier only registers under
-unsafe-dev-api. Without that flag the endpoint is a 404, by design.
"""
import argparse, base64, json, os, sys, urllib.error, urllib.request

ap = argparse.ArgumentParser()
ap.add_argument("dir")
ap.add_argument("--url", default="http://127.0.0.1:8080/zkverify")
ap.add_argument("--tamper", action="store_true", help="flip a bit in the proof first")
# Verification takes ~2.5 s, and the verifier sheds load rather than queueing,
# so a request that has not answered in 30 s is not one that is nearly done.
ap.add_argument("--timeout", type=float, default=30.0, help="seconds to wait for the verifier")
a = ap.parse_args()

req = json.load(open(os.path.join(a.dir, "request.json")))
transcript = open(os.path.join(a.dir, req["transcript"]), "rb").read()
proof = bytearray(open(os.path.join(a.dir, req["proof"]), "rb").read())
if a.tamper:
    proof[len(proof) // 2] ^= 0x01

payload = {k: req[k] for k in ("version", "num_attributes", "pkx", "pky",
                               "doc_type", "namespace", "attr_id", "attr_cbor_hex", "now")}
payload["transcript_b64"] = base64.b64encode(transcript).decode()
payload["proof_b64"] = base64.b64encode(bytes(proof)).decode()

r = urllib.request.Request(a.url, data=json.dumps(payload).encode(),
                           headers={"Content-Type": "application/json"})
try:
    with urllib.request.urlopen(r, timeout=a.timeout) as resp:
        body = json.load(resp)
        status = resp.status
except urllib.error.HTTPError as e:
    body, status = json.load(e), e.code
except urllib.error.URLError as e:
    # Refused, unresolvable, or timed out. Note /zkverify is a 404 unless the
    # verifier was started with -unsafe-dev-api.
    sys.exit(f"could not reach the verifier at {a.url}: {e.reason}")
except TimeoutError:
    sys.exit(f"the verifier at {a.url} did not answer within {a.timeout:g}s")

print(f"HTTP {status}")
print(json.dumps(body, indent=2))
sys.exit(0 if body.get("valid") else 1)
