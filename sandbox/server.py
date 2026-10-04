#!/usr/bin/env python3
"""The ZK age-proof sandbox: a guided, real end-to-end run in the browser.

    python3 sandbox/server.py            # then open http://127.0.0.1:8000

Nothing here is simulated. Registration mints an ee.riik.poa.1 attestation with
issuer/mint_ee_poa.py, the shop opens an OpenID4VP session at verifier/zkverify,
and the wallet step runs wallet/present.py with the Longfellow prover, streaming
its output to the page. The server only sequences them and serves static/.

It is a sandbox, and says where: the "Population Register" believes the birth
date you type (EE-POA-006/007), device keys are PEM files on this host
(EE-SEC-001), and holders live in a temp directory that dies with the process.
"""
import argparse, atexit, base64, datetime as dt, http.server, json, os, re, secrets, shutil
import signal, socket, subprocess, sys, tempfile, threading, time
import urllib.error, urllib.parse, urllib.request
from collections import OrderedDict

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STATIC = os.path.join(REPO, "sandbox", "static")
THRESHOLDS = (16, 18, 21)
MAX_HOLDERS = 200
NAME_RE = re.compile(r"^[^\x00-\x1f<>]{1,60}$")
ID_RE = re.compile(r"^[A-Za-z0-9_-]{16,64}$")
MAX_WAITING = 4          # proofs queued behind the running one; more get "busy"
PROVE_TIMEOUT = 240      # seconds before a wallet run is killed
# Per-client budgets over a sliding 10-minute window. A journey costs 1 enrol,
# 1 shop start and 1 proof; the three cheat attempts add 1 enrol, 3 starts and
# 3 proofs. These allow a few full runs and stop anyone from pinning the CPU.
LIMITS = {"enrol": 12, "shop": 24, "present": 10}
WINDOW = 600


class RateLimit:
    """Sliding-window counter per (client, bucket). In memory, like everything else."""

    def __init__(self):
        self.hits: dict[tuple, list] = {}
        self.lock = threading.Lock()

    def allow(self, client: str, bucket: str) -> bool:
        now = time.monotonic()
        with self.lock:
            if len(self.hits) > 10_000:                    # forget idle clients
                self.hits = {k: v for k, v in self.hits.items() if v and now - v[-1] < WINDOW}
            q = [t for t in self.hits.get((client, bucket), []) if now - t < WINDOW]
            if len(q) >= LIMITS[bucket]:
                self.hits[(client, bucket)] = q
                return False
            q.append(now)
            self.hits[(client, bucket)] = q
            return True


class Sandbox:
    def __init__(self, prover: str, verifier: str, work: str):
        self.prover, self.work = prover, work
        self.issuer_key = os.path.join(work, "issuer.pem")
        self.holders: "OrderedDict[str, dict]" = OrderedDict()
        self.sessions: dict[str, dict] = {}
        self.lock = threading.Lock()
        # ponytail: one proof at a time. Proving is ~6 s of every core; raise this
        # (or queue per client) only if the sandbox is shared by many visitors.
        self.proving = threading.BoundedSemaphore(1)
        self.waiting = 0

        # The verifier trusts one issuer key, fixed before it starts: mint once
        # to create the key and its trust-store entry, then reuse it per holder.
        self._mint(os.path.join(work, "bootstrap"), [16, 18, 21], [])
        shutil.copy(os.path.join(work, "bootstrap", "issuers.json"), os.path.join(work, "issuers.json"))

        port = free_port()
        self.verifier_url = f"http://127.0.0.1:{port}"
        self.verifier_log = open(os.path.join(work, "verifier.log"), "w")
        self.verifier = subprocess.Popen(
            [verifier, "-registry", os.path.join(REPO, "verifier", "circuits.json"),
             "-issuers", os.path.join(work, "issuers.json"),
             "-addr", f"127.0.0.1:{port}", "-base-url", self.verifier_url],
            stdout=self.verifier_log, stderr=subprocess.STDOUT)
        for _ in range(100):
            try:
                urllib.request.urlopen(f"{self.verifier_url}/healthz", timeout=1)
                break
            except OSError:
                if self.verifier.poll() is not None:
                    break
                time.sleep(0.1)
        if self.verifier.poll() is not None:
            self.verifier_log.flush()
            with open(self.verifier_log.name) as f:
                sys.exit("zkverify did not start:\n" + "".join(f.readlines()[-15:]))

    def close(self):
        if self.verifier.poll() is None:
            self.verifier.terminate()
            try:
                self.verifier.wait(5)
            except subprocess.TimeoutExpired:
                self.verifier.kill()

    def _mint(self, out: str, over: list, under: list) -> str:
        cmd = [sys.executable, os.path.join(REPO, "issuer", "mint_ee_poa.py"), "--out", out,
               "--issuer-key", self.issuer_key, "--over", *map(str, over)]
        if under:
            cmd += ["--under", *map(str, under)]
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=60)
        if proc.returncode != 0:
            raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())
        return proc.stdout

    # -- registration --------------------------------------------------------

    def enrol(self, body: dict) -> dict:
        given, family = str(body.get("given", "")).strip(), str(body.get("family", "")).strip()
        if not NAME_RE.match(given) or not NAME_RE.match(family):
            raise ValueError("name")
        try:
            born = dt.date.fromisoformat(str(body.get("birth_date", "")))
        except ValueError:
            raise ValueError("birth_date") from None
        today = dt.date.today()
        age = today.year - born.year - ((today.month, today.day) < (born.month, born.day))
        if born > today or age > 130:
            raise ValueError("birth_date")

        over = [n for n in THRESHOLDS if age >= n]
        under = [n for n in THRESHOLDS if age < n]
        hid = secrets.token_urlsafe(18)
        out = os.path.join(self.work, "holders", hid)
        log = self._mint(out, over, under)

        mso = re.search(r"MSO (\d+) B, DeviceResponse (\d+) B", log)
        validity = re.search(r"validity\s*:\s*(\S+) \.\. (\S+)", log)
        pkx = re.search(r"issuer pk x\s*:\s*(\S+)", log)
        with self.lock:
            self.holders[hid] = {"dir": out, "over18": age >= 18}
            while len(self.holders) > MAX_HOLDERS:
                _, old = self.holders.popitem(last=False)
                shutil.rmtree(old["dir"], ignore_errors=True)
        # The name and birth date stop here: they are not in the attestation
        # and not stored (EE-POA-001, EE-PID-007). The page keeps the name.
        return {
            "holder": hid,
            "predicates": {f"age_over_{n}": age >= n for n in THRESHOLDS},
            "doctype": "ee.riik.poa.1",
            "issuing_country": "EE",
            "valid_from": validity.group(1) if validity else None,
            "valid_until": validity.group(2) if validity else None,
            "mso_bytes": int(mso.group(1)) if mso else None,
            "device_response_bytes": int(mso.group(2)) if mso else None,
            "issuer_pkx": pkx.group(1) if pkx else None,
        }

    # -- the shop ------------------------------------------------------------

    def shop_start(self) -> dict:
        status, started = http_json(f"{self.verifier_url}/present/new", {"element": "age_over_18"})
        if status != 201:
            raise RuntimeError(f"verifier refused to start a session: {status}")
        sid = secrets.token_urlsafe(18)
        with self.lock:
            now = time.time()
            for k in [k for k, s in self.sessions.items() if now - s["t"] > 600]:
                shutil.rmtree(self.sessions.pop(k)["dir"], ignore_errors=True)
            self.sessions[sid] = {"t": now, "request_uri": started["request_uri"],
                                  "result_uri": started["result_uri"],
                                  "dir": tempfile.mkdtemp(prefix="session-", dir=self.work)}
        return {"session": sid, "request_uri": started["request_uri"]}

    def shop_result(self, sid: str) -> dict:
        with self.lock:
            s = self.sessions.get(sid)
        if not s:
            raise KeyError(sid)
        return http_json(s["result_uri"])[1]

    # -- the wallet ----------------------------------------------------------

    def wallet_fetch(self, sid: str) -> dict:
        """Fetch the request object once (request_uri is single-fetch), for consent."""
        with self.lock:
            s = self.sessions.get(sid)
        if not s:
            raise KeyError(sid)
        status, body = http_json(s["request_uri"])
        if status != 200:
            raise RuntimeError(f"verifier did not serve the request: {status}")
        path = os.path.join(s["dir"], "request.json")
        with open(path, "w") as f:
            json.dump(body, f)
        with self.lock:
            s["request_file"] = path
        # A signed request (JAR) is shown from its payload; present.py verifies it.
        req = json.loads(b64u(body["request"].split(".")[1])) if "request" in body else body
        jwks = (req.get("client_metadata") or {}).get("jwks") or {}
        return {
            "client_id": req.get("client_id"),
            "nonce": req.get("nonce"),
            "response_mode": req.get("response_mode"),
            "expected_now": req.get("expected_now"),
            "dcql_query": req.get("dcql_query"),
            "response_key": (jwks.get("keys") or [{}])[0].get("crv"),
            "signed_request": "request" in body,
        }

    def present(self, hid: str, sid: str, mode: str, emit) -> None:
        with self.lock:
            h, s = self.holders.get(hid), self.sessions.get(sid)
            # One answer per request: the file is taken, so a second click on
            # the same session cannot re-run the wallet.
            request_file = s.pop("request_file", None) if s else None
        if not h or not request_file:
            raise KeyError("holder or session")
        cmd = [sys.executable, "-u", os.path.join(REPO, "wallet", "present.py"),
               "--credential", h["dir"], "--verifier", self.verifier_url,
               "--prover", self.prover, "--request-file", request_file]
        cmd += {"normal": [], "replay": ["--replay"], "tamper": ["--tamper-nonce"],
                "lie": ["--allow-false-predicate"]}[mode]
        if not self.proving.acquire(blocking=False):
            with self.lock:
                if self.waiting >= MAX_WAITING:
                    raise OverflowError("busy")
                self.waiting += 1
            try:
                emit("queued", {})
                self.proving.acquire()
            finally:
                with self.lock:
                    self.waiting -= 1
        try:
            start = time.monotonic()
            proc = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
            killer = threading.Timer(PROVE_TIMEOUT, proc.kill)
            killer.start()
            lines = []
            try:
                assert proc.stdout is not None
                for line in proc.stdout:
                    line = line.rstrip("\n")
                    lines.append(line)
                    emit("line", {"text": line, "t": round(time.monotonic() - start, 2)})
            finally:
                if proc.poll() is None:      # the browser left: stop proving for nobody
                    proc.kill()
                code = proc.wait()
                killer.cancel()
        finally:
            self.proving.release()
        emit("done", {"code": code, **parse_outcome(lines)})


def parse_outcome(lines: list) -> dict:
    """Pull the verifier's answers (and the replay's) out of present.py's output."""
    text = "\n".join(lines)
    out = {}
    for key, label in (("response", "response"), ("replay", "replay")):
        m = re.search(rf"^{label}\s*: HTTP (\d+)\n(\{{.*?^\}})", text, re.M | re.S)
        if m:
            try:
                out[key] = {"status": int(m.group(1)), **json.loads(m.group(2))}
            except json.JSONDecodeError:
                out[key] = {"status": int(m.group(1))}
    if "proving failed" in text:
        out["prover_refused"] = True
    return out


def http_json(url: str, body=None):
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method="POST" if data else "GET",
                                 headers={"Content-Type": "application/json"} if data else {})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.load(e)
        except json.JSONDecodeError:
            return e.code, {}


def b64u(s: str) -> bytes:
    return base64.urlsafe_b64decode(s + "=" * (-len(s) % 4))


def free_port() -> int:
    with socket.socket() as s:
        s.bind(("127.0.0.1", 0))
        return s.getsockname()[1]


def handler_for(box: Sandbox, trust_proxy: bool):
    limits = RateLimit()

    class Handler(http.server.SimpleHTTPRequestHandler):
        server_version = "sandbox"
        sys_version = ""

        def __init__(self, *a, **kw):
            super().__init__(*a, directory=STATIC, **kw)

        def list_directory(self, path):
            self.send_error(404)

        def client(self) -> str:
            # Behind the Cloudflare tunnel every peer is cloudflared; the
            # visitor is in CF-Connecting-IP. Trusted only when told to be.
            if trust_proxy and self.headers.get("CF-Connecting-IP"):
                return self.headers["CF-Connecting-IP"]
            return self.client_address[0]

        def limited(self, bucket: str) -> bool:
            if limits.allow(self.client(), bucket):
                return False
            self.send_response(429)
            self.send_header("Retry-After", str(WINDOW))
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-store")
            body = b'{"error":"rate"}'
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return True

        def end_headers(self):
            self.send_header("X-Content-Type-Options", "nosniff")
            self.send_header("Referrer-Policy", "no-referrer")
            self.send_header("Content-Security-Policy",
                             "default-src 'self'; img-src 'self' data:; frame-ancestors 'none'")
            if not self.path.startswith("/api/"):
                self.send_header("Cache-Control", "no-cache")   # revalidate: edits show up on reload
            super().end_headers()

        def log_message(self, format, *args):  # noqa: A002 — base-class name
            if not self.path.startswith("/fonts/"):
                super().log_message(format, *args)

        def send_json(self, status: int, obj):
            body = json.dumps(obj).encode()
            self.send_response(status)
            self.send_header("Content-Type", "application/json")
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)

        def do_POST(self):
            path = urllib.parse.urlparse(self.path).path
            try:
                length = int(self.headers.get("Content-Length") or 0)
                if length > 4096:
                    return self.send_json(413, {"error": "too_large"})
                body = json.loads(self.rfile.read(length) or b"{}")
                if path == "/api/enrol":
                    return None if self.limited("enrol") else self.send_json(201, box.enrol(body))
                if path == "/api/shop/start":
                    return None if self.limited("shop") else self.send_json(201, box.shop_start())
                if path == "/api/wallet/fetch":
                    if not ID_RE.match(str(body.get("session", ""))):
                        return self.send_json(400, {"error": "session"})
                    return self.send_json(200, box.wallet_fetch(body["session"]))
            except KeyError:
                return self.send_json(404, {"error": "expired"})
            except (ValueError, json.JSONDecodeError) as e:
                return self.send_json(400, {"error": str(e) or "bad_request"})
            except Exception as e:  # noqa: BLE001 — surface sandbox failures to the page
                self.log_error("%s", e)
                return self.send_json(502, {"error": "backend"})
            self.send_json(404, {"error": "not_found"})

        def do_GET(self):
            url = urllib.parse.urlparse(self.path)
            q = {k: v[0] for k, v in urllib.parse.parse_qs(url.query).items()}
            if url.path == "/api/shop/result":
                if not ID_RE.match(q.get("session", "")):
                    return self.send_json(400, {"error": "session"})
                try:
                    return self.send_json(200, box.shop_result(q["session"]))
                except KeyError:
                    return self.send_json(404, {"error": "session"})
            if url.path == "/api/present":
                return self.stream_present(q)
            if url.path.startswith("/api/"):
                return self.send_json(404, {"error": "not_found"})
            super().do_GET()

        def stream_present(self, q: dict):
            hid, sid, mode = q.get("holder", ""), q.get("session", ""), q.get("mode", "normal")
            if not ID_RE.match(hid) or not ID_RE.match(sid) or mode not in ("normal", "replay", "tamper", "lie"):
                return self.send_json(400, {"error": "bad_request"})
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream")
            self.send_header("Cache-Control", "no-store")
            self.end_headers()

            def emit(event: str, data: dict):
                self.wfile.write(f"event: {event}\ndata: {json.dumps(data)}\n\n".encode())
                self.wfile.flush()
            # EventSource cannot read a 429, so the refusal travels as an event.
            if not limits.allow(self.client(), "present"):
                return emit("fail", {"error": "rate"})
            try:
                box.present(hid, sid, mode, emit)
            except KeyError:
                emit("fail", {"error": "expired"})
            except OverflowError:
                emit("fail", {"error": "busy"})
            except (BrokenPipeError, ConnectionResetError):
                pass

    return Handler


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=8000)
    ap.add_argument("--prover", default=os.environ.get("EE_PROVER", os.path.join(
        os.path.dirname(REPO), "longfellow-zk", "rust", "target", "release", "examples", "ee_poa_demo")))
    ap.add_argument("--verifier", default=os.environ.get("EE_VERIFIER", os.path.join(REPO, "verifier", "zkverify")))
    ap.add_argument("--trust-proxy", action="store_true",
                    help="rate-limit on CF-Connecting-IP (only behind a Cloudflare tunnel)")
    args = ap.parse_args()
    for name, path in (("prover", args.prover), ("verifier", args.verifier)):
        if not os.access(path, os.X_OK):
            sys.exit(f"{name} not found at {path}; run `make deps` or pass --{name}")

    work = tempfile.mkdtemp(prefix="zk-sandbox-")
    box = Sandbox(args.prover, args.verifier, work)

    def cleanup():
        box.close()
        shutil.rmtree(work, ignore_errors=True)
    atexit.register(cleanup)
    signal.signal(signal.SIGTERM, lambda *_: sys.exit(0))

    server = http.server.ThreadingHTTPServer((args.host, args.port), handler_for(box, args.trust_proxy))
    server.daemon_threads = True
    print(f"sandbox    : http://{args.host}:{args.port}", flush=True)
    print(f"verifier   : {box.verifier_url} (log: {box.verifier_log.name})", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
