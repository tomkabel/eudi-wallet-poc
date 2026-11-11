# RP service profile

This is the [ADR-004](../docs/decisions/ADR-004-RP-KIT-DEPLOYMENT-PROFILE.md)
deployment profile for the existing `verifier/zkverify` binary. The local
acceptance path checks startup, encrypted requests, persistent keys and missing
callbacks; it does not constitute production certification or public deployment.

## Build and local acceptance

From the repository root on Linux, use the toolchains pinned in `mise.toml`,
Python dependencies in `uv.lock`, and OpenSSL. Rust needs an x86-64-v3 host.
See [development prerequisites](../docs/DEVELOPMENT.md).

```sh
make py-deps
make deps
DOCTOR_SKIP=android make doctor
make test-rp-kit
```

`make deps` creates the required sibling `../longfellow-zk`, builds the Rust
staticlib/prover, and builds Go with the linked revision. `make test-rp-kit`
builds a temporary Go binary from current sources (requires that staticlib),
explicitly tests that binary, then removes it. It reads and stages the shipped
systemd unit/environment configuration and runs bounded
local processes with temporary trust inputs and keys. No wallet, domain,
proxy or external service is required.

For foreground startup, mint local test trust material outside the checkout.
The generated holder private key is test data: never commit or publish it.

```sh
export RP_STATE="$(mktemp -d)"
chmod 700 "$RP_STATE"
.venv/bin/python3 issuer/mint_ee_poa.py --out "$RP_STATE/issuer" --over 18
(umask 077; openssl ecparam -name prime256v1 -genkey -noout -out "$RP_STATE/response-key.pem")
chmod 600 "$RP_STATE/response-key.pem"
```

```sh
./verifier/zkverify \
  -addr 127.0.0.1:8080 -base-url http://127.0.0.1:8080 \
  -client-id x509_san_dns:localhost \
  -registry "$PWD/verifier/circuits.json" \
  -issuers "$RP_STATE/issuer/issuers.json" \
  -longfellow-rev-file "$PWD/verifier/zkverify-ffi/longfellow-rev.txt" \
  -response-key-file "$RP_STATE/response-key.pem" \
  -session-ttl 3m -max-concurrent-verify 1 -drain-timeout 6m
```

In another terminal, check health, create a session and poll it (these manual
commands additionally require `jq`; the automated harness does not):

```sh
BASE=http://127.0.0.1:8080
curl -fsS "$BASE/healthz"
SESSION=$(curl -fsS -H 'Content-Type: application/json' \
  -d '{"element":"age_over_18"}' "$BASE/present/new")
ID=$(printf '%s' "$SESSION" | jq -r .id)
curl -fsS "$(printf '%s' "$SESSION" | jq -r .request_uri)" | jq .response_mode
curl -i "$BASE/present/result/$ID"
# After 3 minutes without callback, repeat during the following 3 minutes.
curl -i "$BASE/present/result/$ID"
curl -i "$BASE/present/result/unknown-id"
```

A request URI can be fetched once. Response mode is `direct_post.jwt`, the
binary default; the profile supplies no response-mode override. The profile
configures persisted response keys, local registry/issuer inputs, session TTL
(default **3 minutes**), concurrency, drain budget (binary default **twice the TTL**)
and optionally an exact `-dcapi-origin`. DCAPI is off by default. ADR-004's
excluded development/downgrade flags are not supported deployment modes. Only
the test harness exercises explicit plain negative controls.

## Session status and bounded retention

Both `/present/result/{id}` and `/present/dcapi/result/{id}` return `200`
with the same `id`, `answered:false`, `valid:false` while pending. Without a
callback the session expires at TTL: `410` returns
`{"id":"…","error":"oid4vp: session expired"}`. Repeated polling and
janitor cleanup retain this metadata for **one additional TTL from the original
expiry deadline**, never from a poll. Nonce, query, key references, HPKE state
and verdict/detail payloads leave the store. Late mutations cannot revive it.

Retention is separately bounded to **10,000 outcomes**, evicting the oldest
recorded outcome first at capacity. Live sessions have their own 10,000 cap.
The janitor runs at TTL/4 with a 1-second floor. At capacity, after retention,
or after process restart an id may become unknown: `404` still names the
requested id. Poll promptly and save outcomes in your RP for durable business
records. Sessions/outcomes are in memory; persistent keys stabilize JWKS and
thumbprints across restarts, **not sessions**. A `404` does not prove absence
of a previous presentation. No wallet callback is needed to observe expiry.

## Local acceptance systemd installation

The unit uses a restricted account, direct `ExecStart`, a loopback listener,
and an operator-managed P-256 private key with mode **0600**. This systemd
example, the generated trust store in `RP_STATE`, and the shipped circuit
registry are **local acceptance only**. The shipped registry's audits prohibit
production use. Before any real integration, require independently reviewed
trust and circuit-registry inputs suitable for that integration; replacing
these files alone does not establish compliance or production readiness.

```sh
sudo useradd --system --home-dir /var/lib/zkverify --no-create-home --shell /usr/sbin/nologin zkverify
sudo install -d -m 0755 /usr/local/libexec /etc/zkverify
sudo install -d -o zkverify -g zkverify -m 0700 /var/lib/zkverify
sudo install -m 0755 verifier/zkverify /usr/local/libexec/zkverify
sudo install -m 0644 verifier/circuits.json /etc/zkverify/circuits.json
sudo install -m 0644 verifier/zkverify-ffi/longfellow-rev.txt /etc/zkverify/longfellow-rev.txt
sudo install -m 0644 "$RP_STATE/issuer/issuers.json" /etc/zkverify/issuers.json
sudo install -o zkverify -g zkverify -m 0600 "$RP_STATE/response-key.pem" /var/lib/zkverify/response-key.pem
sudo install -m 0644 rp-kit/zkverify.env.example /etc/zkverify/zkverify.env
sudoedit /etc/zkverify/zkverify.env
sudo install -m 0644 rp-kit/zkverify.service /etc/systemd/system/zkverify.service
sudo systemd-analyze verify /etc/systemd/system/zkverify.service
sudo systemctl daemon-reload
sudo systemctl enable --now zkverify
sudo systemctl status zkverify
sudo journalctl -u zkverify -n 50 --no-pager
curl -fsS http://127.0.0.1:8080/healthz
```

Use absolute paths. Empty `TRUST_ROOT` means operator-managed unsigned local
trust inputs; for signed inputs install their P-256 public PEM in
`/etc/zkverify` and set `TRUST_ROOT` to it. Bad signature/freshness fails
startup. Set `MAX_CONCURRENT_VERIFY` for host CPU budget (binary default is
CPU count; the template conservatively uses one). If changing `DRAIN_TIMEOUT`,
also keep `TimeoutStopSec` longer than the budget plus shutdown's 10-second
grace. The template explicitly sets `DRAIN_TIMEOUT=6m`; changing `SESSION_TTL`
alone does not change that independent setting. Startup failures exit nonzero;
`SESSION_TTL` must be strictly positive.

**HTTPS proxy boundary:** the binary serves HTTP on loopback. The operator
must provide a separate HTTPS reverse proxy, certificate, DNS and firewall.
Set `BASE_URL` to its externally reachable HTTPS URL and `CLIENT_ID`
consistently; forward `/present/` and `/healthz` without rewriting paths.
Keep keys, config and logs private. Only local loopback acceptance uses an
HTTP base URL. This profile does not configure a proxy or establish a
certified wallet trust relationship.

## Drain, restart and manual rotation

SIGTERM closes new-session admission (`503`, `Retry-After: 3`); `/healthz`
reports `draining:true`. Existing response/result routes remain available
until pending sessions finish or the budget expires.

```sh
sudo systemctl stop zkverify  # SIGTERM; waits for drain
sudo systemctl start zkverify
# Or: sudo systemctl restart zkverify
```

For foreground use `kill -TERM "$PID"` then `wait "$PID"` with the actual
verifier PID. Rotation is file swap → drain → restart. The running process
retains its old key until exit:

```sh
sudo sh -c 'umask 077; openssl ecparam -name prime256v1 -genkey -noout -out /var/lib/zkverify/response-key.next.pem'
sudo chown zkverify:zkverify /var/lib/zkverify/response-key.next.pem
sudo chmod 0600 /var/lib/zkverify/response-key.next.pem
sudo mv /var/lib/zkverify/response-key.next.pem /var/lib/zkverify/response-key.pem
sudo systemctl restart zkverify
curl -fsS http://127.0.0.1:8080/present/jwks.json
```

Ordinary restart with the same key preserves JWKS; rotation changes it. Drain
before exit to avoid interrupting sessions. The [TARA adapter contract](TARA-CONTRACT.md)
defines the parallel OIDC authentication path, offline controls and opt-in live
runner. Register and circuit gateway integrations are separate stories.
