#!/usr/bin/env bash
# End-to-end check of the OpenID4VP presentation flow against a freshly minted
# attestation: the happy path, the two binding defences (transcript, single-use
# nonce), and the predicate-value binding on both sides of it.
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

# 1. Mint the two identities: one over 18, one not.
python3 "$repo/issuer/mint_ee_poa.py" --out "$work/adult" --over 16 18 21 >"$work/mint-adult.log"
python3 "$repo/issuer/mint_ee_poa.py" --out "$work/minor" --over 16 --under 18 21 >"$work/mint-minor.log"

# Both were minted by different issuer keys, so the trust store holds both.
python3 - "$work/adult/issuers.json" "$work/minor/issuers.json" "$work/issuers.json" <<'PY'
import json, sys
out = {"issuers": []}
for path in sys.argv[1:-1]:
    out["issuers"] += json.load(open(path))["issuers"]
json.dump(out, open(sys.argv[-1], "w"), indent=2)
PY

"$verifier" -registry "$repo/verifier/circuits.json" -issuers "$work/issuers.json" \
	-addr "127.0.0.1:$port" -base-url "$base" >"$work/verifier.log" 2>&1 &
server_pid=$!

for _ in $(seq 1 50); do
	if curl -fsS "$base/healthz" >/dev/null 2>&1; then break; fi
	sleep 0.2
done
curl -fsS "$base/healthz" >/dev/null || { echo "verifier did not start:"; cat "$work/verifier.log"; exit 1; }

# present runs the wallet and echoes the verifier's answer, whatever it is.
present() {
	local log="$1"; shift
	python3 "$repo/wallet/present.py" --credential "$1" --verifier "$base" \
		--prover "$prover" "${@:2}" >"$log" 2>&1 || true
}

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

echo
if [ "$failures" -eq 0 ]; then
	echo "PASS: 5/5"
else
	echo "FAIL: $failures of 5 assertions failed"
fi
exit "$((failures > 0))"
