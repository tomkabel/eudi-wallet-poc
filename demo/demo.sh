#!/usr/bin/env bash
# Demo driver for the Android recording: one verifier on a LAN-reachable URL,
# and one wallet run per take.
#
#   terminal A:  demo/demo.sh serve
#   terminal B:  demo/demo.sh prove [adult|minor|tamper|replay]
#
# Mints demo-data/{adult,minor} on first use. Builds nothing — build the prover
# and ./verifier/zkverify first. The credential directories survive every run,
# so a take can be repeated without re-minting.
set -euo pipefail

repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
prover="${EE_PROVER:-/home/notroot/Documents/longfellow-zk/rust/target/release/examples/ee_poa_demo}"
port="${EE_PORT:-8080}"
data="$repo/demo-data"

# The address the phone dials. Asking the routing table for the source address
# of the default route picks the LAN interface, not tailscale0 or a docker
# bridge, which is what listing every global-scope address would have done.
lan_ip() { ip -4 route get 1.1.1.1 | sed -n 's/.* src \([0-9.]*\).*/\1/p'; }

mint() {
	[ -f "$data/adult/mdoc.bin" ] || python3 "$repo/issuer/mint_ee_poa.py" --out "$data/adult" --over 16 18 21
	[ -f "$data/minor/mdoc.bin" ] || python3 "$repo/issuer/mint_ee_poa.py" --out "$data/minor" --over 16 --under 18 21
	# Each mint run generates its own issuer key, so the verifier has to trust
	# both or the minor take fails on an untrusted issuer rather than on the
	# predicate value — which is the whole point of that take.
	jq -s '{issuers: (map(.issuers) | add)}' \
		"$data/adult/issuers.json" "$data/minor/issuers.json" >"$data/issuers.json"
}

case "${1:-}" in
serve)
	mint
	ip="$(lan_ip)"
	echo "verifier: http://$ip:$port  (phone and laptop must share this network)"
	exec "$repo/verifier/zkverify" \
		-registry "$repo/verifier/circuits.json" \
		-issuers "$data/issuers.json" \
		-addr "0.0.0.0:$port" \
		-base-url "http://$ip:$port"
	;;
prove)
	[ -x "$prover" ] || { echo "not executable: $prover (set EE_PROVER)" >&2; exit 1; }
	base="http://$(lan_ip):$port"
	case "${2:-adult}" in
	adult) set -- --credential "$data/adult" ;;
	# The prover refuses a false predicate by default, so proving one takes the
	# override; the take is about the verifier refusing it, not the prover.
	minor) set -- --credential "$data/minor" --allow-false-predicate ;;
	tamper) set -- --credential "$data/adult" --tamper-nonce ;;
	replay) set -- --credential "$data/adult" --replay ;;
	*) echo "usage: demo.sh prove [adult|minor|tamper|replay]" >&2; exit 1 ;;
	esac
	exec python3 "$repo/wallet/present.py" --verifier "$base" --prover "$prover" "$@"
	;;
*)
	echo "usage: demo.sh serve | demo.sh prove [adult|minor|tamper|replay]" >&2
	exit 1
	;;
esac
