#!/usr/bin/env python3
"""Shed-load test: 200 concurrent verifications against one verifier.

    EE_PROVER=.../target/release/examples/ee_poa_demo python3 tests/load_test.py

zk.Verify is a cgo call that runs for ~2.5 s, and a goroutine inside cgo pins
its OS thread for the whole call. Go aborts the process past 10,000 threads, so
before the admission limit this endpoint was a remote kill switch: one HTTP
request per thread, no credential needed. This fires 200 well-formed proofs at
once and asserts the verifier answers all of them -- 200 or 503, never a dropped
connection -- while its OS thread count stays bounded.

The proof is real (a tampered bit makes it invalid) because a malformed proof is
rejected in ~20 ms and would never reach the expensive path. Minting and proving
take ~15 s; pass --work DIR to reuse a previous run's proof.

Builds nothing. Build the prover and ./verifier/zkverify first.
"""
import argparse
import base64
import json
import os
import pathlib
import socket
import subprocess
import sys
import threading
import time
import urllib.error
import urllib.request

REPO = pathlib.Path(__file__).resolve().parent.parent


def free_port() -> int:
    s = socket.socket()
    s.bind(("127.0.0.1", 0))
    port = s.getsockname()[1]
    s.close()
    return port


def threads_of(pid: int) -> int:
    """The kernel's own count of OS threads in the process."""
    with open(f"/proc/{pid}/status") as f:
        for line in f:
            if line.startswith("Threads:"):
                return int(line.split()[1])
    raise RuntimeError("no Threads: line in /proc status")


def prepare(work: pathlib.Path, prover: str) -> dict:
    """Mint an attestation and prove age_over_18 over it, once."""
    cred = work / "cred"
    if not (cred / "proof.bin").exists():
        cred.mkdir(parents=True, exist_ok=True)
        subprocess.run([sys.executable, str(REPO / "issuer" / "mint_ee_poa.py"),
                        "--out", str(cred), "--over", "18"],
                       check=True, stdout=subprocess.DEVNULL)
        subprocess.run([prover, str(cred), "age_over_18"],
                       check=True, stdout=subprocess.DEVNULL)
    req = json.loads((cred / "request.json").read_text())
    transcript = (cred / req["transcript"]).read_bytes()
    proof = bytearray((cred / req["proof"]).read_bytes())
    # Flip a bit in the middle: still a well-formed proof of the right circuit,
    # so it costs a full verification to reject rather than failing to parse.
    proof[len(proof) // 2] ^= 0x01
    payload = {k: req[k] for k in ("version", "num_attributes", "pkx", "pky",
                                   "doc_type", "namespace", "attr_id",
                                   "attr_cbor_hex", "now")}
    payload["transcript_b64"] = base64.b64encode(transcript).decode()
    payload["proof_b64"] = base64.b64encode(bytes(proof)).decode()
    return payload


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--prover", default=os.environ.get("EE_PROVER", ""),
                    help="ee_poa_demo binary (or set EE_PROVER)")
    ap.add_argument("--verifier", default=os.environ.get(
        "EE_VERIFIER", str(REPO / "verifier" / "zkverify")))
    ap.add_argument("--requests", type=int, default=200)
    ap.add_argument("--max-concurrent-verify", type=int, default=0,
                    help="passed to the verifier; 0 leaves it at its default")
    ap.add_argument("--work", default="", help="reuse a directory's minted proof")
    a = ap.parse_args()
    if not a.prover:
        print("set EE_PROVER to the ee_poa_demo binary", file=sys.stderr)
        return 1

    work = pathlib.Path(a.work) if a.work else pathlib.Path(
        os.environ.get("TMPDIR", "/tmp")) / "ee-load-test"
    work.mkdir(parents=True, exist_ok=True)
    print(f"preparing a proof in {work} (~15 s the first time)")
    payload = json.dumps(prepare(work, a.prover)).encode()
    print(f"payload: {len(payload) / 1024:.0f} KiB, {a.requests} concurrent requests")

    port = free_port()
    argv = [a.verifier,
            "-registry", str(REPO / "verifier" / "circuits.json"),
            "-issuers", str(work / "cred" / "issuers.json"),
            "-addr", f"127.0.0.1:{port}",
            "-base-url", f"http://127.0.0.1:{port}",
            "-unsafe-dev-api"]
    if a.max_concurrent_verify:
        argv += ["-max-concurrent-verify", str(a.max_concurrent_verify)]
    log = open(work / "verifier.log", "w")
    srv = subprocess.Popen(argv, stdout=log, stderr=subprocess.STDOUT)

    try:
        url = f"http://127.0.0.1:{port}"
        for _ in range(50):
            try:
                urllib.request.urlopen(url + "/healthz", timeout=1).read()
                break
            except (urllib.error.URLError, TimeoutError):
                time.sleep(0.2)
        else:
            print(f"verifier did not start; see {work / 'verifier.log'}", file=sys.stderr)
            return 1

        peak = threads_of(srv.pid)
        baseline = peak
        sampling = True

        def sample():
            nonlocal peak
            while sampling:
                try:
                    peak = max(peak, threads_of(srv.pid))
                except (OSError, RuntimeError):
                    return  # process gone; the assertions below catch it
                time.sleep(0.02)

        sampler = threading.Thread(target=sample, daemon=True)
        sampler.start()

        statuses: dict[str, int] = {}
        lock = threading.Lock()
        start_together = threading.Barrier(a.requests)

        def fire():
            req = urllib.request.Request(
                url + "/zkverify", data=payload,
                headers={"Content-Type": "application/json"})
            start_together.wait()
            try:
                with urllib.request.urlopen(req, timeout=120) as r:
                    key = str(r.status)
                    r.read()
            except urllib.error.HTTPError as e:
                key = str(e.code)
                e.read()
            except Exception as e:  # a dropped connection is a failure, not a status
                key = f"ERROR {type(e).__name__}: {e}"
            with lock:
                statuses[key] = statuses.get(key, 0) + 1

        t0 = time.time()
        workers = [threading.Thread(target=fire) for _ in range(a.requests)]
        for w in workers:
            w.start()
        for w in workers:
            w.join()
        wall = time.time() - t0
        sampling = False
        sampler.join(timeout=1)

        alive = srv.poll() is None
        print()
        print("responses by status:")
        for k in sorted(statuses):
            print(f"  {k:<40} {statuses[k]}")
        print(f"threads: {baseline} at rest, {peak} peak (Go aborts at 10000)")
        print(f"wall: {wall:.1f}s   process alive: {alive}")
        print()

        failures = []
        if not alive:
            failures.append(f"the verifier died (exit {srv.returncode})")
        bad = {k: v for k, v in statuses.items() if k not in ("200", "503")}
        if bad:
            failures.append(f"non-200/503 outcomes: {bad}")
        # Fewer threads than concurrent requests is the whole point: one thread
        # per in-flight verification is what walks the process into Go's cap.
        if peak >= a.requests:
            failures.append(f"peak threads {peak} >= {a.requests} concurrent requests")
        if peak >= 1000:
            failures.append(f"peak threads {peak} is not bounded well below 10000")
        if not statuses.get("503"):
            failures.append("nothing was shed; the limit was never exercised")

        for f in failures:
            print(f"  FAIL  {f}")
        if failures:
            print(f"\nFAIL: {len(failures)} assertion(s) failed")
            return 1
        print("PASS: every request answered, threads bounded, process up")
        return 0
    finally:
        srv.terminate()
        srv.wait(timeout=10)
        log.close()


if __name__ == "__main__":
    sys.exit(main())
