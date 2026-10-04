#!/usr/bin/env bash
# Build the sandbox image here, ship it to the host, and route zk.proksiabel.ee
# to it through a Cloudflare tunnel.
#
#   CF_API_TOKEN=… sandbox/deploy/deploy.sh [ssh-target]      # default tomkabel@asus
#   BUILD_ONLY=1 sandbox/deploy/deploy.sh                     # just the image, to test locally
#   CF_API_TOKEN=… sandbox/deploy/deploy.sh local                # run it on this machine instead
#
# Needs: a `make deps` build (prover + zkverify), docker or podman here, docker
# compose on the host, and a Cloudflare token with Tunnel:Edit on the account
# and DNS:Edit on the zone. Idempotent: re-running rebuilds and restarts.
set -euo pipefail

target="${1:-tomkabel@asus}"
hostname="${ZK_HOSTNAME:-zk.proksiabel.ee}"
zone="${hostname#*.}"
tunnel_name="zk-sandbox"
remote_dir="zk-sandbox"
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
prover="${EE_PROVER:-$repo/../longfellow-zk/rust/target/release/examples/ee_poa_demo}"
verifier="${EE_VERIFIER:-$repo/verifier/zkverify}"
# The path the binaries were compiled to read their circuits from.
circuits="${EE_CIRCUITS:-$(cd "$(dirname "$prover")/../../.." && pwd)/applications/mdoc_zk/artifacts/circuits}"

for bin in "$prover" "$verifier"; do
	[ -x "$bin" ] || { echo "missing $bin; run make deps" >&2; exit 1; }
done

# 1. Image, from a staged context so the build sees only what ships.
ctx="$(mktemp -d)"
trap 'rm -rf "$ctx"' EXIT
mkdir -p "$ctx/bin" "$ctx/verifier" "$ctx/issuer" "$ctx/wallet" "$ctx/sandbox"
cp "$prover" "$verifier" "$ctx/bin/"
cp -r "$circuits" "$ctx/circuits"
cp "$repo/verifier/circuits.json" "$ctx/verifier/"
cp "$repo/issuer/mint_ee_poa.py" "$ctx/issuer/"
cp "$repo/wallet/present.py" "$ctx/wallet/"
cp "$repo/sandbox/server.py" "$ctx/sandbox/"
cp -r "$repo/sandbox/static" "$ctx/sandbox/"
fmt=(); docker --version | grep -qi podman && fmt=(--format docker)   # keep HEALTHCHECK
docker build "${fmt[@]}" --build-arg CIRCUITS_DIR="$circuits" -t zk-sandbox:latest \
	-f "$repo/sandbox/deploy/Dockerfile" "$ctx"
[ -n "${BUILD_ONLY:-}" ] && exit 0
: "${CF_API_TOKEN:?set CF_API_TOKEN}"

# 2. Tunnel, ingress and DNS, through the API. Prints the connector token.
token="$(python3 - "$hostname" "$zone" "$tunnel_name" <<'PY'
import base64, json, os, secrets, sys, urllib.request, urllib.error
hostname, zone, name = sys.argv[1:]
API = "https://api.cloudflare.com/client/v4"
def cf(method, path, body=None):
    req = urllib.request.Request(API + path, method=method,
        data=json.dumps(body).encode() if body is not None else None,
        headers={"Authorization": "Bearer " + os.environ["CF_API_TOKEN"], "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            out = json.load(r)
    except urllib.error.HTTPError as e:
        out = json.load(e)
    if not out.get("success"):
        sys.exit(f"cloudflare {method} {path}: {out.get('errors')}")
    return out["result"]
z = cf("GET", f"/zones?name={zone}")[0]
acc, zid = z["account"]["id"], z["id"]
found = cf("GET", f"/accounts/{acc}/cfd_tunnel?name={name}&is_deleted=false")
tid = found[0]["id"] if found else cf("POST", f"/accounts/{acc}/cfd_tunnel",
    {"name": name, "config_src": "cloudflare", "tunnel_secret": base64.b64encode(secrets.token_bytes(32)).decode()})["id"]
cf("PUT", f"/accounts/{acc}/cfd_tunnel/{tid}/configurations", {"config": {"ingress": [
    {"hostname": hostname, "service": "http://sandbox:8000"},
    {"service": "http_status:404"}]}})
record = {"type": "CNAME", "name": hostname, "content": f"{tid}.cfargotunnel.com", "proxied": True,
          "comment": "ZK age-proof sandbox (eudi-wallet-poc sandbox/deploy)"}
existing = cf("GET", f"/zones/{zid}/dns_records?name={hostname}")
if existing:
    cf("PUT", f"/zones/{zid}/dns_records/{existing[0]['id']}", record)
else:
    cf("POST", f"/zones/{zid}/dns_records", record)
print(cf("GET", f"/accounts/{acc}/cfd_tunnel/{tid}/token"))
PY
)"

# 3. Ship and start, in ~/zk-sandbox on the target. The token travels on
# stdin, never in argv or the repo.
on() { if [ "$target" = local ]; then (cd ~ && bash -c "$1"); else ssh "$target" "$1"; fi; }
if [ "$target" != local ]; then
	# podman names it localhost/zk-sandbox; retag to what compose.yml expects.
	loaded="$(docker save zk-sandbox:latest | gzip -1 | ssh "$target" 'gunzip | docker load' | awk '/Loaded image/ {print $NF}')"
	on "docker tag $loaded zk-sandbox:latest"
fi
on "mkdir -p $remote_dir"
on "cat > $remote_dir/compose.yml" < "$repo/sandbox/deploy/compose.yml"
printf 'TUNNEL_TOKEN=%s\n' "$token" | on "umask 077 && cat > $remote_dir/.env"
on "cd $remote_dir && docker compose up -d --force-recreate && docker compose ps"
echo "deployed: https://$hostname"
