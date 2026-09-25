#!/usr/bin/env bash
# Drain semantics (W4): SIGTERM must stop NEW presentations (503 + Retry-After
# on /present/new, draining=true on /healthz for the load balancer) while the
# listener stays open for an already-started session — that session's response
# POST must still verify and return valid:true inside the drain window.
#
#   EE_PROVER=.../target/release/examples/ee_poa_demo tests/drain_test.sh
#
# Builds nothing. Build the prover and ./verifier/zkverify first (make deps).
# A short -session-ttl and an explicit -drain-timeout keep the run fast.
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
prover="${EE_PROVER:?set EE_PROVER to the ee_poa_demo binary}"
verifier="${EE_VERIFIER:-$repo/verifier/zkverify}"

for bin in "$prover" "$verifier"; do
	[ -x "$bin" ] || { echo "not executable: $bin" >&2; exit 1; }
done

work="$(mktemp -d -t ee-drain-XXXXXX)"
server_pid=""
cleanup() {
	[ -n "$server_pid" ] && kill -9 "$server_pid" 2>/dev/null || true
	rm -rf "$work"
}
trap cleanup EXIT

port="$(python3 -c 'import socket;s=socket.socket();s.bind(("127.0.0.1",0));print(s.getsockname()[1]);s.close()')"
base="http://127.0.0.1:$port"

failures=0
pass() { printf '  PASS  %s\n' "$1"; }
fail() { printf '  FAIL  %s\n' "$1"; failures=$((failures + 1)); }

# Mint one adult attestation and start the verifier in plain mode (no JWE
# plumbing needed to exercise the drain; e2e.sh covers the encrypted modes).
python3 "$repo/issuer/mint_ee_poa.py" --out "$work/adult" --over 18 >"$work/mint.log"
"$verifier" -registry "$repo/verifier/circuits.json" -issuers "$work/adult/issuers.json" \
	-addr "127.0.0.1:$port" -base-url "$base" \
	-response-mode direct_post -allow-unencrypted-response \
	-session-ttl 30s -drain-timeout 20s >"$work/verifier.log" 2>&1 &
server_pid=$!
for _ in $(seq 1 50); do
	if curl -fsS "$base/healthz" >/dev/null 2>&1; then break; fi
	sleep 0.2
done
curl -fsS "$base/healthz" >/dev/null || { echo "verifier did not start:"; cat "$work/verifier.log"; exit 1; }

# Phase 1: open a session the way wallet/present.py does, fetch the request
# object, build the transcript, re-sign the mdoc against it and run the prover
# — everything except the response POST. The session now sits in the store,
# waiting for its wallet.
python3 - "$base" "$prover" "$work/adult" "$repo" "$work" <<'PY'
import base64, json, os, subprocess, sys, tempfile, urllib.request

base, prover, cred, repo, work = sys.argv[1:6]

def http_json(url, payload=None):
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, method="POST",
                                 headers={"Content-Type": "application/json"} if data else {})
    with urllib.request.urlopen(req, timeout=30) as r:
        return json.load(r)

started = http_json(base + "/present/new", {"element": "age_over_18"})
req = json.load(urllib.request.urlopen(started["request_uri"], timeout=30))

# The session transcript, exactly as wallet/present.py derives it (plain mode:
# the thumbprint element is null).
import cbor2, hashlib
info = cbor2.dumps([req["client_id"], req["nonce"], None, req["response_uri"]], canonical=True)
digest = hashlib.sha256(info).digest()
transcript = cbor2.dumps([None, None, ["OpenID4VPHandover", digest]], canonical=True)

sys.path.insert(0, os.path.join(repo, "wallet"))
import present as wallet  # noqa: E402

cq = req["dcql_query"]["credentials"][0]
ns, element = cq["claims"][0]["path"]
params = open(os.path.join(cred, "params.txt")).read().splitlines()
doc_type = params[3]
device_key = wallet.serialization.load_pem_private_key(
    open(os.path.join(cred, "device_key.pem"), "rb").read(), password=None)
mdoc = wallet.resign_device_auth(open(os.path.join(cred, "mdoc.bin"), "rb").read(),
                                 device_key, transcript, doc_type)

session_dir = tempfile.mkdtemp(prefix="ee-drain-")
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
    import shutil
    shutil.rmtree(session_dir, ignore_errors=True)

entry = base64.urlsafe_b64encode(json.dumps({
    "zk_system": "longfellow-libzk-v1",
    "version": rj["version"], "num_attributes": rj["num_attributes"],
    "doc_type": doc_type, "namespace": ns, "attr_id": element,
    "attr_cbor_hex": rj["attr_cbor_hex"],
    "proof_b64": base64.b64encode(proof).decode(),
}).encode()).decode().rstrip("=")

# Park the answer for phase 2, after the SIGTERM.
json.dump({"response_uri": req["response_uri"],
           "session_id": req["response_uri"].rstrip("/").split("/")[-1],
           "body": {"vp_token": {cq["id"]: [entry]}, "expected_now": req["expected_now"]}},
          open(os.path.join(work, "session.json"), "w"))
print("session ready:", req["response_uri"])
PY

echo "1. SIGTERM: new presentations are refused, the listener stays up"
kill -TERM "$server_pid"

new_status="$(curl -s -o "$work/new-503.json" -w '%{http_code}' -X POST -d '{"element":"age_over_18"}' "$base/present/new")" || true
retry_after="$(curl -s -o /dev/null -D - -X POST -d '{"element":"age_over_18"}' "$base/present/new" | tr -d '\r' | grep -i '^retry-after:' || true)"
if [ "$new_status" = "503" ] && [ -n "$retry_after" ]; then
	pass "/present/new answers 503 with $(echo "$retry_after" | tr -d '\n')"
else
	fail "during drain /present/new = $new_status, retry-after: ${retry_after:-none}"; cat "$work/new-503.json" 2>/dev/null
fi

healthz="$(curl -s "$base/healthz")" || true
if [ "$healthz" = '{"draining":true}' ]; then
	pass "/healthz reports {\"draining\":true} so the LB stops routing"
else
	fail "/healthz during drain = $healthz, want {\"draining\":true}"
fi

echo "2. the started session still completes inside the drain window"
result="$(python3 - "$work" <<'PY'
import json, os, sys, urllib.request

work = sys.argv[1]
s = json.load(open(os.path.join(work, "session.json")))
req = urllib.request.Request(s["response_uri"], data=json.dumps(s["body"]).encode(),
                             method="POST", headers={"Content-Type": "application/json"})
with urllib.request.urlopen(req, timeout=60) as r:
    print(json.dumps(json.load(r)))
PY
)" || true
if echo "$result" | grep -q '"valid": true'; then
	pass "the pre-signal session verified valid:true after SIGTERM"
else
	fail "the pre-signal session did not verify during drain"; echo "$result" | head -5
fi

echo "3. after the drain the process exits cleanly"
waited=0
while kill -0 "$server_pid" 2>/dev/null && [ "$waited" -lt 30 ]; do
	sleep 1
	waited=$((waited + 1))
done
if kill -0 "$server_pid" 2>/dev/null; then
	fail "the verifier did not exit within 30s of the drain"
	kill -9 "$server_pid" 2>/dev/null || true
elif wait "$server_pid" 2>/dev/null; then
	pass "the verifier exited 0 after draining"
else
	# bash reaps the trap's kill as the exit status; check the log instead.
	if grep -q "drained cleanly" "$work/verifier.log"; then
		pass "the verifier exited after a clean drain"
	else
		fail "the verifier exited non-zero without a clean drain"; tail -5 "$work/verifier.log"
	fi
fi
server_pid=""

echo
if [ "$failures" -eq 0 ]; then
	echo "PASS: 3/3"
else
	echo "FAIL: $failures of 3 assertions failed"
fi
exit "$((failures > 0))"
