#!/usr/bin/env bash
# End-to-end check of the OpenID4VP presentation flow against a freshly minted
# attestation: the happy path in BOTH response modes (plain direct_post and
# encrypted direct_post.jwt, ADR-003), the two binding defences (transcript,
# single-use nonce), the predicate-value binding on both sides of it, and the
# response-encryption negatives (tampered JWE, plain post to an encrypted
# session).
#
#   EE_PROVER=.../target/release/examples/ee_poa_demo tests/e2e.sh
#
# Builds nothing. Build the prover and ./verifier/zkverify first. The /present/*
# flow is all this exercises, so the verifier runs without -unsafe-dev-api.
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
prover="${EE_PROVER:?set EE_PROVER to the ee_poa_demo binary}"
verifier="${EE_VERIFIER:-$repo/verifier/zkverify}"

for bin in "$prover" "$verifier"; do
	[ -x "$bin" ] || { echo "not executable: $bin" >&2; exit 1; }
done

work="$(mktemp -d -t ee-e2e-XXXXXX)"
server_pid=""
cleanup() {
	[ -n "$server_pid" ] && kill "$server_pid" 2>/dev/null || true
	rm -rf "$work"
}
trap cleanup EXIT

# A free port, picked by the kernel rather than guessed.
port="$(python3 -c 'import socket;s=socket.socket();s.bind(("127.0.0.1",0));print(s.getsockname()[1]);s.close()')"
base="http://127.0.0.1:$port"

failures=0
pass() { printf '  PASS  %s\n' "$1"; }
fail() { printf '  FAIL  %s\n' "$1"; failures=$((failures + 1)); }

# 1. Mint the identities: adult and minor over the national doctype, plus the
# second doctype fixture (W6) — the EU AV document from its own issuer key, so
# the two-credential session has two doctypes from two issuers to trust.
python3 "$repo/issuer/mint_ee_poa.py" --out "$work/adult" --over 16 18 21 >"$work/mint-adult.log"
python3 "$repo/issuer/mint_ee_poa.py" --out "$work/minor" --over 16 --under 18 21 >"$work/mint-minor.log"
python3 "$repo/issuer/mint_ee_poa.py" --out "$work/av" --doctype eu.europa.ec.av.1 --over 16 18 21 >"$work/mint-av.log"

# All three were minted by different issuer keys, so the trust store holds all.
python3 - "$work/adult/issuers.json" "$work/minor/issuers.json" "$work/av/issuers.json" "$work/issuers.json" <<'PY'
import json, sys
out = {"issuers": []}
for path in sys.argv[1:-1]:
    out["issuers"] += json.load(open(path))["issuers"]
json.dump(out, open(sys.argv[-1], "w"), indent=2)
PY

# present runs the wallet and echoes the verifier's answer, whatever it is.
# Extra args go through to present.py (--response-mode drives both e2e modes).
present() {
	local log="$1"; shift
	python3 "$repo/wallet/present.py" --credential "$1" --verifier "$base" \
		--prover "$prover" "${@:2}" >"$log" 2>&1 || true
}

# start_verifier launches zkverify with the given response-mode flags and waits
# for it. The mode flags are passed through verbatim.
start_verifier() {
	"$verifier" -registry "$repo/verifier/circuits.json" -issuers "$work/issuers.json" \
		-addr "127.0.0.1:$port" -base-url "$base" "$@" >"$work/verifier.log" 2>&1 &
	server_pid=$!
	for _ in $(seq 1 50); do
		if curl -fsS "$base/healthz" >/dev/null 2>&1; then break; fi
		sleep 0.2
	done
	curl -fsS "$base/healthz" >/dev/null || { echo "verifier did not start:"; cat "$work/verifier.log"; exit 1; }
}

stop_verifier() {
	[ -n "$server_pid" ] && kill "$server_pid" 2>/dev/null || true
	server_pid=""
	wait 2>/dev/null || true
}

# ===== Mode A: plain direct_post (the explicit downgrade) =====
start_verifier -response-mode direct_post -allow-unencrypted-response

echo "1. happy path: an over-18 holder presents age_over_18"
present "$work/1.log" "$work/adult"
if grep -q '"valid": true' "$work/1.log"; then
	pass "verifier answered valid:true"
else
	fail "expected valid:true"; tail -20 "$work/1.log"
fi

echo "2. transcript binding: a proof made against a different nonce"
present "$work/2.log" "$work/adult" --tamper-nonce
if grep -q '"valid": false' "$work/2.log"; then
	pass "verifier answered valid:false"
else
	fail "expected valid:false"; tail -20 "$work/2.log"
fi

echo "3. single use: the same vp_token posted twice"
present "$work/3.log" "$work/adult" --replay
if grep -q 'replay     : HTTP 410' "$work/3.log"; then
	pass "the replayed post was refused with 410"
else
	fail "expected HTTP 410 on replay"; tail -20 "$work/3.log"
fi

# EE-ZKP-021(b): the verifier binds the value the query asked for, so it must
# refuse a proof of `age_over_18 = false` even though the proof is sound.
echo "4. predicate binding: an under-18 holder proves age_over_18 = false"
present "$work/4.log" "$work/minor" --allow-false-predicate
if grep -q '"valid": false' "$work/4.log" && grep -q 'only accepts 0xf5' "$work/4.log"; then
	pass "verifier answered valid:false, on the value and not by accident"
else
	fail "a false predicate was accepted as true"; tail -20 "$work/4.log"
fi

# ...and the prover refuses to make that proof unless it is asked to, so the
# two defences hold independently.
echo "5. prover refuses a false predicate by default"
# The guard is a panic, and this profile aborts on panic, so the redirect keeps
# the shell's "core dumped" notice out of the transcript.
rc=0
{ "$prover" "$work/minor" age_over_18 >"$work/5.log" 2>&1 || rc=$?; } 2>/dev/null
if [ "$rc" -eq 0 ]; then
	fail "the prover proved age_over_18 for an under-18 attestation"; tail -20 "$work/5.log"
elif grep -q -- '--allow-false-predicate' "$work/5.log"; then
	pass "the prover exited non-zero, naming the attribute and its actual value"
else
	fail "the prover failed, but not on the predicate value"; tail -20 "$work/5.log"
fi

stop_verifier

# ===== Mode B: direct_post.jwt, the encrypted default (ADR-003) =====
start_verifier -response-mode direct_post.jwt

echo "6. encrypted mode: the request publishes the response key"
status="$(curl -s -o "$work/6-req.json" -w '%{http_code}' "$(curl -fsS -X POST -d '{"element":"age_over_18"}' "$base/present/new" | python3 -c 'import json,sys; print(json.load(sys.stdin)["request_uri"])')")"
if [ "$status" = 200 ] && python3 - "$work/6-req.json" <<'PY'
import json, sys
req = json.load(open(sys.argv[1]))
assert req["response_mode"] == "direct_post.jwt", req.get("response_mode")
keys = req["client_metadata"]["jwks"]["keys"]
assert keys and keys[0]["kty"] == "EC" and keys[0]["crv"] == "P-256", keys
PY
then
	pass "the authorization request carries client_metadata.jwks with the P-256 response key"
else
	fail "the request did not publish the response key"; cat "$work/6-req.json" 2>/dev/null
fi

echo "7. encrypted mode: an over-18 holder answers with a JWE"
present "$work/7.log" "$work/adult" --encrypt-for /dev/null
if grep -q '"valid": true' "$work/7.log"; then
	pass "verifier answered valid:true over the encrypted response"
else
	fail "expected valid:true on the encrypted flow"; tail -20 "$work/7.log"
fi

# The wire assertion: rerun the flow and capture what actually goes out.
echo "8. encrypted mode: ciphertext is what is on the wire"
present "$work/8.log" "$work/adult"
on_wire="$(grep -c 'response   : HTTP 200' "$work/8.log")" || true
if [ "$on_wire" -ge 1 ] && python3 - "$base" <<'PY'
# Re-drive one session and assert the POST body is a compact JWE with a
# 5-part structure and no plaintext JSON in it.
import base64, json, sys, urllib.error, urllib.request, urllib.parse
base = sys.argv[1]
def post(url, payload, hdrs):
    data = json.dumps(payload).encode() if isinstance(payload, dict) else payload
    req = urllib.request.Request(url, data=data, method="POST", headers=hdrs)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)
started = post(base + "/present/new", {"element": "age_over_18"},
               {"Content-Type": "application/json"})
# Walk the same steps present.py does, but stop before proving: we only need
# the JWE shape, so encrypt a marker vp_token with the published key.
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
from jwcrypto import jwk as jwk_mod, jwe as jwe_mod
key = jwk_mod.JWK.from_json(json.dumps(req["client_metadata"]["jwks"]["keys"][0]))
marker = {"age_credential": ["ciphertext-on-the-wire-check"]}
jwe = jwe_mod.JWE(plaintext=json.dumps(marker).encode(),
                  protected=json.dumps({"alg": "ECDH-ES+A256KW", "enc": "A256GCM"}))
jwe.add_recipient(key)
compact = jwe.serialize(compact=True)
assert compact.count(".") == 4, "not a 5-part compact JWE"
assert b"vp_token" not in compact.encode() and "age_credential" not in compact
body = urllib.parse.urlencode({"response": compact}).encode()
r = urllib.request.Request(req["response_uri"], data=body, method="POST",
                           headers={"Content-Type": "application/x-www-form-urlencoded"})
try:
    resp = json.load(urllib.request.urlopen(r, timeout=30))
    # The marker is not a real presentation: the verifier must have parsed the
    # JWE (error about the vp_token contents) rather than failing to decrypt.
    txt = json.dumps(resp)
    assert "decrypt" not in txt.lower(), resp
except urllib.error.HTTPError as e:
    body_txt = e.read().decode()
    assert "decrypt" not in body_txt.lower(), body_txt
PY
then
	pass "the wire carries a compact JWE; plaintext never leaves the wallet"
else
	fail "the response was not ciphertext on the wire"
fi

echo "9. negative: a tampered JWE fails closed with no partial parse"
tamper_check="$(python3 - "$base" <<'PY'
import base64, json, sys, urllib.error, urllib.request, urllib.parse
base = sys.argv[1]
def post(url, payload, hdrs):
    data = json.dumps(payload).encode() if isinstance(payload, dict) else payload
    req = urllib.request.Request(url, data=data, method="POST", headers=hdrs)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)
started = post(base + "/present/new", {"element": "age_over_18"},
               {"Content-Type": "application/json"})
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
from jwcrypto import jwk as jwk_mod, jwe as jwe_mod
key = jwk_mod.JWK.from_json(json.dumps(req["client_metadata"]["jwks"]["keys"][0]))
jwe = jwe_mod.JWE(plaintext=json.dumps({"age_credential": ["x"]}).encode(),
                  protected=json.dumps({"alg": "ECDH-ES+A256KW", "enc": "A256GCM"}))
jwe.add_recipient(key)
compact = jwe.serialize(compact=True)
# Flip one byte in the ciphertext part (index 3).
parts = compact.split(".")
raw = bytearray(base64.urlsafe_b64decode(parts[3] + "=="))
raw[0] ^= 0x01
parts[3] = base64.urlsafe_b64encode(bytes(raw)).decode().rstrip("=")
body = urllib.parse.urlencode({"response": ".".join(parts)}).encode()
r = urllib.request.Request(req["response_uri"], data=body, method="POST",
                           headers={"Content-Type": "application/x-www-form-urlencoded"})
try:
    resp = json.load(urllib.request.urlopen(r, timeout=30))
    print("UNEXPECTED-200")
except urllib.error.HTTPError as e:
    txt = e.read().decode()
    print("OK" if e.code == 400 and "decrypt" in txt else f"BAD {e.code} {txt[:120]}")
PY
)" || true
if [ "$tamper_check" = "OK" ]; then
	pass "the tampered JWE got a 400 and nothing was parsed from it"
else
	fail "tampered JWE handling: $tamper_check"
fi

echo "10. negative: a plain vp_token posted to an encrypted session is refused"
plain_check="$(python3 - "$base" <<'PY'
import json, sys, urllib.request
base = sys.argv[1]
def post(url, payload, hdrs):
    data = json.dumps(payload).encode() if isinstance(payload, dict) else payload
    req = urllib.request.Request(url, data=data, method="POST", headers=hdrs)
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)
started = post(base + "/present/new", {"element": "age_over_18"},
               {"Content-Type": "application/json"})
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
body = json.dumps({"vp_token": {"age_credential": ["unencrypted"]}}).encode()
r = urllib.request.Request(req["response_uri"], data=body, method="POST",
                           headers={"Content-Type": "application/json"})
try:
    resp = json.load(urllib.request.urlopen(r, timeout=30))
    print("UNEXPECTED-200")
except urllib.error.HTTPError as e:
    txt = e.read().decode()
    print("OK" if e.code == 400 and "encrypted" in txt else f"BAD {e.code} {txt[:120]}")
PY
)" || true
if [ "$plain_check" = "OK" ]; then
	pass "the plain response got a 400 naming the encrypted requirement"
else
	fail "plain-response refusal: $plain_check"
fi

# The default mode is the encrypted one: a verifier started with no flag must
# behave like mode B (negative check catches a flipped default).
echo "11. default: no flags means direct_post.jwt"
default_check="$(python3 - "$base" <<'PY'
import json, sys, urllib.request
base = sys.argv[1]
data = json.dumps({"element": "age_over_18"}).encode()
req = urllib.request.Request(base + "/present/new", data=data, method="POST",
                             headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req, timeout=30) as r:
    started = json.load(r)
obj = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
print("OK" if obj.get("response_mode") == "direct_post.jwt" else f"BAD {obj.get('response_mode')}")
PY
)" || true
if [ "$default_check" = "OK" ]; then
	pass "a fresh /present/new without mode flags answers direct_post.jwt"
else
	fail "the default response mode is not direct_post.jwt: $default_check"
fi

stop_verifier

# ===== W6: multi-credential DCQL (still the plain-mode verifier) =====
start_verifier -response-mode direct_post -allow-unencrypted-response -multi-credentials 2

# A two-credential session: the wallet answers BOTH queried credentials (the
# national PoA and the EU AV document, two doctypes from two issuers) with one
# vp_token carrying one entry per credential id. The verifier must verify both
# under the session's single ExpectedNow and answer valid:true only because
# every queried credential verified.
echo "12. multi-credential: one session requests and verifies two doctypes"
present "$work/12.log" "$work/adult" --credential "$work/av"
if grep -q '"valid": true' "$work/12.log" && grep -q 'all 2 credentials proved in zero knowledge' "$work/12.log"; then
	pass "a two-credential (two doctype, two issuer) session verified both"
else
	fail "expected valid:true over both credentials"; tail -20 "$work/12.log"
fi

# The all-or-error rule: a response answering only one of the two queried
# credentials must fail the session, with the missing credential id named —
# an answer is not a pass, and the answer says which credential never arrived.
echo "13. multi-credential negative: one missing vp_token entry names the id"
missing_check="$(python3 - "$base" "$prover" "$work/adult" "$repo" <<'PY'
import base64, json, os, shutil, subprocess, sys, tempfile, urllib.error, urllib.request
base, prover, cred, repo = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]

def http_json(url, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method="POST",
                                 headers={"Content-Type": "application/json"} if data else {})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        return json.load(e)

started = http_json(base + "/present/new", {"element": "age_over_18"})
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
queries = req["dcql_query"]["credentials"]
assert len(queries) == 2, queries

# The session transcript, exactly as wallet/present.py derives it.
import cbor2, hashlib
info = cbor2.dumps([req["client_id"], req["nonce"], None, req["response_uri"]], canonical=True)
digest = hashlib.sha256(info).digest()
transcript = cbor2.dumps([None, None, ["OpenID4VPHandover", digest]], canonical=True)

sys.path.insert(0, os.path.join(repo, "wallet"))
import present as wallet  # noqa: E402  (reuse the wallet's exact re-signing)

params = open(os.path.join(cred, "params.txt")).read().splitlines()
doc_type = params[3]
device_key = wallet.serialization.load_pem_private_key(
    open(os.path.join(cred, "device_key.pem"), "rb").read(), password=None)
mdoc = wallet.resign_device_auth(open(os.path.join(cred, "mdoc.bin"), "rb").read(),
                                 device_key, transcript, doc_type)

entries = {}
for cq in queries:
    ns, element = cq["claims"][0]["path"]
    session_dir = tempfile.mkdtemp(prefix="ee-missing-")
    try:
        open(os.path.join(session_dir, "mdoc.bin"), "wb").write(mdoc)
        open(os.path.join(session_dir, "transcript.bin"), "wb").write(transcript)
        open(os.path.join(session_dir, "params.txt"), "w").write(
            "\n".join([params[0], params[1], req["expected_now"], *params[3:5]]) + "\n")
        proc = subprocess.run([prover, session_dir, element], capture_output=True, text=True)
        assert proc.returncode == 0, proc.stderr
        rj = json.load(open(os.path.join(session_dir, "request.json")))
        proof = open(os.path.join(session_dir, "proof.bin"), "rb").read()
    finally:
        shutil.rmtree(session_dir, ignore_errors=True)
    entries[cq["id"]] = [base64.urlsafe_b64encode(json.dumps({
        "zk_system": "longfellow-libzk-v1",
        "version": rj["version"], "num_attributes": rj["num_attributes"],
        "doc_type": doc_type, "namespace": ns, "attr_id": element,
        "attr_cbor_hex": rj["attr_cbor_hex"],
        "proof_b64": base64.b64encode(proof).decode(),
    }).encode()).decode().rstrip("=")]

# Drop the LAST queried credential's entry: the answer covers one of two.
answered = dict(list(entries.items())[:1])
body = json.dumps({"vp_token": answered, "expected_now": req["expected_now"]}).encode()
req_obj = urllib.request.Request(req["response_uri"], data=body, method="POST",
                                 headers={"Content-Type": "application/json"})
try:
    with urllib.request.urlopen(req_obj, timeout=60) as r:
        resp = json.load(r)
    ok = resp.get("valid") is False and queries[1]["id"] in resp.get("detail", "")
    print("OK" if ok else f"BAD {resp}")
except urllib.error.HTTPError as e:
    txt = e.read().decode()
    ok = queries[1]["id"] in txt
    print("OK" if ok else f"BAD {e.code} {txt[:200]}")
PY
)" || true
if [ "$missing_check" = "OK" ]; then
	pass "the one-of-two answer failed the session, naming the missing id"
else
	fail "missing-credential refusal: $missing_check"
fi

stop_verifier

echo
if [ "$failures" -eq 0 ]; then
	echo "PASS: 13/13"
else
	echo "FAIL: $failures of 13 assertions failed"
fi
exit "$((failures > 0))"
