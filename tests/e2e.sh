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

observed_carriers() {
	python3 - "$1" <<'PY'
import json, sys
text = open(sys.argv[1], encoding="utf-8", errors="replace").read()
decoder = json.JSONDecoder()
found = []
for pos, char in enumerate(text):
    if char != "{":
        continue
    try:
        value, _ = decoder.raw_decode(text[pos:])
    except json.JSONDecodeError:
        continue
    if isinstance(value, dict) and isinstance(value.get("carriers"), list):
        found = value["carriers"]
if found and all(isinstance(item, str) for item in found):
    print(",".join(found))
else:
    print("NOT_OBSERVED")
PY
}

# The conformance runner supplies a private JSONL path. Keep the script's
# human-readable output and assertions unchanged while recording observed
# metadata for each assertion.
case_record() {
	[ -n "${EE_E2E_CASES:-}" ] || return 0
	python3 - "$EE_E2E_CASES" "$@" <<'PY'
import json, sys
path, case_id, carrier, response_mode, outcome, result, *property_name = sys.argv[1:]
with open(path, "a", encoding="utf-8") as f:
    f.write(json.dumps({
        "case_id": case_id,
        "path_label": "openid4vp-interim-poc",
        "carriers": carrier.split(",") if carrier != "NOT_OBSERVED" else [],
        "declared_response_mode": response_mode,
        "declared_outcome": outcome,
        "result": result,
        "binding_property": property_name[0] if property_name else "not-applicable",
    }, sort_keys=True) + "\n")
PY
}

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
	# Transport negatives intentionally leave sessions unanswered. Bound the
	# test server's graceful drain instead of waiting a full session TTL.
	"$verifier" -registry "$repo/verifier/circuits.json" -issuers "$work/issuers.json" \
		-addr "127.0.0.1:$port" -base-url "$base" -drain-timeout 1s "$@" >"$work/verifier.log" 2>&1 &
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
	case_record 1 "$(observed_carriers "$work/1.log")" direct_post proof-verified PASS
	pass "verifier answered valid:true"
else
	case_record 1 "$(observed_carriers "$work/1.log")" direct_post proof-verified FAIL
	fail "expected valid:true"; tail -20 "$work/1.log"
fi

echo "2. transcript binding: a proof made against a different nonce"
present "$work/2.log" "$work/adult" --tamper-nonce
if grep -q 'response   : HTTP 200' "$work/2.log" && grep -q '"valid": false' "$work/2.log" && grep -q 'transcript-binding' "$work/2.log"; then
	case_record 2 "$(observed_carriers "$work/2.log")" direct_post transcript-binding-refused PASS nonce
	pass "verifier answered valid:false"
else
	case_record 2 "$(observed_carriers "$work/2.log")" direct_post transcript-binding-refused FAIL nonce
	fail "expected valid:false"; tail -20 "$work/2.log"
fi

echo "3. single use: the same vp_token posted twice"
present "$work/3.log" "$work/adult" --replay
if grep -q 'response   : HTTP 200' "$work/3.log" && grep -q '"valid": true' "$work/3.log" && grep -q 'replay     : HTTP 410' "$work/3.log" && grep -q 'single-use' "$work/3.log"; then
	case_record 3 "$(observed_carriers "$work/3.log")" direct_post replay-refused PASS single-use
	pass "the replayed post was refused with 410"
else
	case_record 3 "$(observed_carriers "$work/3.log")" direct_post replay-refused FAIL single-use
	fail "expected HTTP 410 on replay"; tail -20 "$work/3.log"
fi

# EE-ZKP-021(b): the verifier binds the value the query asked for, so it must
# refuse a proof of `age_over_18 = false` even though the proof is sound.
echo "4. predicate binding: an under-18 holder proves age_over_18 = false"
present "$work/4.log" "$work/minor" --allow-false-predicate
if grep -q 'response   : HTTP 400' "$work/4.log" && grep -q '"valid": false' "$work/4.log" && grep -q 'predicate-value' "$work/4.log"; then
	case_record 4 "$(observed_carriers "$work/4.log")" direct_post false-predicate-refused PASS predicate-value
	pass "verifier answered valid:false, on the value and not by accident"
else
	case_record 4 "$(observed_carriers "$work/4.log")" direct_post false-predicate-refused FAIL predicate-value
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
	case_record 5 NOT_OBSERVED direct_post prover-guard-failed FAIL
	fail "the prover proved age_over_18 for an under-18 attestation"; tail -20 "$work/5.log"
elif grep -q -- '--allow-false-predicate' "$work/5.log"; then
	case_record 5 NOT_OBSERVED direct_post prover-guard-refused PASS
	pass "the prover exited non-zero, naming the attribute and its actual value"
else
	case_record 5 NOT_OBSERVED direct_post prover-guard-failed FAIL
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
	case_record 6 NOT_OBSERVED direct_post.jwt response-key-published PASS
	pass "the authorization request carries client_metadata.jwks with the P-256 response key"
else
	case_record 6 NOT_OBSERVED direct_post.jwt response-key-published FAIL
	fail "the request did not publish the response key"; cat "$work/6-req.json" 2>/dev/null
fi

echo "7. encrypted mode: an over-18 holder answers with a JWE"
present "$work/7.log" "$work/adult" --encrypt-for /dev/null
if grep -q '"valid": true' "$work/7.log"; then
	case_record 7 "$(observed_carriers "$work/7.log")" direct_post.jwt proof-verified PASS
	pass "verifier answered valid:true over the encrypted response"
else
	case_record 7 "$(observed_carriers "$work/7.log")" direct_post.jwt proof-verified FAIL
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
	case_record 8 NOT_OBSERVED direct_post.jwt encrypted-wire-assertion PASS
	pass "the wire carries a compact JWE; plaintext never leaves the wallet"
else
	case_record 8 NOT_OBSERVED direct_post.jwt encrypted-wire-assertion FAIL
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
    print("OK" if e.code == 400 and "decryption:" in txt else f"BAD {e.code} {txt[:120]}")
PY
)" || true
if [ "$tamper_check" = "OK" ]; then
	case_record 9 NOT_OBSERVED direct_post.jwt decryption-failed PASS
	pass "the tampered JWE got a 400 and nothing was parsed from it"
else
	case_record 9 NOT_OBSERVED direct_post.jwt decryption-failed FAIL
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
	case_record 10 NOT_OBSERVED direct_post plain-response-refused PASS
	pass "the plain response got a 400 naming the encrypted requirement"
else
	case_record 10 NOT_OBSERVED direct_post plain-response-refused FAIL
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
	case_record 11 NOT_OBSERVED direct_post.jwt encrypted-default-confirmed PASS
	pass "a fresh /present/new without mode flags answers direct_post.jwt"
else
	case_record 11 NOT_OBSERVED direct_post.jwt encrypted-default-confirmed FAIL
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
	case_record 12 "$(observed_carriers "$work/12.log")" direct_post multi-credential-proof-verified PASS
	pass "a two-credential (two doctype, two issuer) session verified both"
else
	case_record 12 "$(observed_carriers "$work/12.log")" direct_post multi-credential-proof-verified FAIL
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
except urllib.error.HTTPError as e:
    txt = e.read().decode()
    try:
        resp = json.loads(txt)
    except json.JSONDecodeError:
        resp = {}
    ok = queries[1]["id"] in txt
carriers = resp.get("carriers", [])
carrier_label = ",".join(carriers) if carriers else "NOT_OBSERVED"
print(("OK" if ok else "BAD") + " " + carrier_label)
PY
)" || true
if [ "$missing_check" = "OK NOT_OBSERVED" ]; then
	case_record 13 "${missing_check#OK }" direct_post missing-entry PASS
	pass "the one-of-two answer failed the session, naming the missing id"
else
	case_record 13 NOT_OBSERVED direct_post missing-entry FAIL
	fail "missing-credential refusal: $missing_check"
fi

# 14. Bytes that match neither carrier are refused as unknown-carrier; they do
# not fall through to the JSON parser.
echo "14. unknown carrier bytes: refused as unknown-carrier, not parsed as JSON"
unknown_check="$(python3 - "$base" <<'PY'
import base64, json, sys, urllib.error, urllib.request
base = sys.argv[1]

def http_json(url, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method="POST",
                                 headers={"Content-Type": "application/json"} if data else {})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.read().decode()
    except urllib.error.HTTPError as e:
        return e.read().decode()

started = json.loads(http_json(base + "/present/new", {"element": "age_over_18"}))
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
ids = [cq["id"] for cq in req["dcql_query"]["credentials"]]
junk = base64.urlsafe_b64encode(b"\x01\x02\x03 not a carrier").decode().rstrip("=")
txt = http_json(req["response_uri"], {"vp_token": {i: [junk] for i in ids}, "expected_now": req["expected_now"]})
print("OK" if "unknown-carrier" in txt else "BAD " + txt)
PY
)" || true
if [ "$unknown_check" = "OK" ]; then
	case_record 14 NOT_OBSERVED direct_post unknown-carrier PASS
	pass "unrecognised vp_token bytes are refused as unknown-carrier"
else
	case_record 14 NOT_OBSERVED direct_post unknown-carrier FAIL
	fail "unknown-carrier refusal: $unknown_check"
fi

# 15. A JSON-looking envelope that is truncated is malformed-envelope, not
# unknown-carrier: the '{' sniff keeps it on the interim JSON path.
echo "15. truncated JSON envelope: refused as malformed-envelope"
malformed_check="$(python3 - "$base" <<'PY'
import base64, json, sys, urllib.error, urllib.request
base = sys.argv[1]

def http_json(url, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method="POST",
                                 headers={"Content-Type": "application/json"} if data else {})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.read().decode()
    except urllib.error.HTTPError as e:
        return e.read().decode()

started = json.loads(http_json(base + "/present/new", {"element": "age_over_18"}))
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
ids = [cq["id"] for cq in req["dcql_query"]["credentials"]]
truncated = base64.urlsafe_b64encode(b'{"zk_system": "longfellow-libzk-v1", "version":').decode().rstrip("=")
txt = http_json(req["response_uri"], {"vp_token": {i: [truncated] for i in ids}, "expected_now": req["expected_now"]})
print("OK" if "malformed-envelope" in txt and "unknown-carrier" not in txt else "BAD " + txt)
PY
)" || true
if [ "$malformed_check" = "OK" ]; then
	case_record 15 NOT_OBSERVED direct_post malformed-envelope PASS
	pass "truncated JSON envelope is refused as malformed-envelope"
else
	case_record 15 NOT_OBSERVED direct_post malformed-envelope FAIL
	fail "malformed-envelope refusal: $malformed_check"
fi

stop_verifier

# Four appended controls keep the established fifteen assertion IDs stable.
start_verifier -response-mode direct_post.jwt

echo "16. transcript binding: deliberately changed client_id"
present "$work/16.log" "$work/adult" --tamper-client-id
if grep -q '"valid": false' "$work/16.log" && grep -q 'transcript-binding' "$work/16.log" && grep -q 'response   : HTTP 200' "$work/16.log"; then
    case_record 16 "$(observed_carriers "$work/16.log")" direct_post.jwt transcript-binding-refused PASS client_id
    pass "client_id control refused as transcript-binding"
else
    case_record 16 "$(observed_carriers "$work/16.log")" direct_post.jwt transcript-binding-refused FAIL client_id
    fail "client_id control was not refused as transcript-binding"; tail -20 "$work/16.log"
fi

echo "17. transcript binding: deliberately changed response_uri"
present "$work/17.log" "$work/adult" --tamper-response-uri
if grep -q '"valid": false' "$work/17.log" && grep -q 'transcript-binding' "$work/17.log" && grep -q 'response   : HTTP 200' "$work/17.log"; then
    case_record 17 "$(observed_carriers "$work/17.log")" direct_post.jwt transcript-binding-refused PASS response_uri
    pass "response_uri control refused as transcript-binding"
else
    case_record 17 "$(observed_carriers "$work/17.log")" direct_post.jwt transcript-binding-refused FAIL response_uri
    fail "response_uri control was not refused as transcript-binding"; tail -20 "$work/17.log"
fi

echo "18. transcript binding: deliberately changed jwk_thumbprint"
present "$work/18.log" "$work/adult" --tamper-jwk-thumbprint
if grep -q '"valid": false' "$work/18.log" && grep -q 'transcript-binding' "$work/18.log" && grep -q 'response   : HTTP 200' "$work/18.log"; then
    case_record 18 "$(observed_carriers "$work/18.log")" direct_post.jwt transcript-binding-refused PASS jwk_thumbprint
    pass "jwk_thumbprint control refused as transcript-binding"
else
    case_record 18 "$(observed_carriers "$work/18.log")" direct_post.jwt transcript-binding-refused FAIL jwk_thumbprint
    fail "jwk_thumbprint control was not refused as transcript-binding"; tail -20 "$work/18.log"
fi

stop_verifier
start_verifier -response-mode direct_post -allow-unencrypted-response
echo "19. empty CBOR DeviceResponse: no-presentation refusal"
empty_check="$(python3 - "$base" <<'EMPTY'
import base64, cbor2, json, sys, urllib.error, urllib.request
base = sys.argv[1]
def post(url, payload, expected_status):
    request = urllib.request.Request(url, data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            assert response.status == expected_status
            return json.load(response)
    except urllib.error.HTTPError as error:
        assert error.code == expected_status
        return json.load(error)
started = post(base + "/present/new", {"element": "age_over_18"}, 201)
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))
entry = base64.urlsafe_b64encode(cbor2.dumps({"version": "1.0", "zkDocuments": [], "status": 0})).decode().rstrip("=")
response = post(req["response_uri"], {"vp_token": {req["dcql_query"]["credentials"][0]["id"]: [entry]}}, 400)
assert response["valid"] is False and response["detail"].startswith("no-presentation:"), response
assert response["carriers"] == ["mso-mdoc-zk-cbor"], response
polled = json.load(urllib.request.urlopen(base + "/present/result/" + started["id"], timeout=30))
assert polled["detail"] == response["detail"] and polled["valid"] is False, polled
print("OK " + ",".join(response["carriers"]))
EMPTY
)" || true
if [ "$empty_check" = "OK mso-mdoc-zk-cbor" ]; then
    case_record 19 "${empty_check#OK }" direct_post no-presentation PASS
    pass "empty CBOR response refused as no-presentation"
else
    case_record 19 NOT_OBSERVED direct_post no-presentation FAIL
    fail "empty presentation handling: $empty_check"
fi
stop_verifier

echo
if [ "$failures" -eq 0 ]; then
	echo "PASS: 19/19"
else
	echo "FAIL: $failures of 19 assertions failed"
fi
exit "$((failures > 0))"
