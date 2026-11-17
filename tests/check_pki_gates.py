#!/usr/bin/env python3
"""PKI gates (docs/planning/PKI-REMEDIATION-PLAN.md). Each gate fails CI when one
of the plan's fixed defects comes back.

    python3 tests/check_pki_gates.py                 # every gate, exit 1 on any failure
    python3 tests/check_pki_gates.py --update        # refresh digests and dates in trust/MANIFEST.json
    python3 tests/check_pki_gates.py --gosec R.json  # fail on gosec findings not in the baseline

Gates:
  trust-all     no chain-validation bypass in the wallet (C0)
  mocks         no *Mock class or mock status URI in sources compiled outside
                local_mocks (C11, NEW-S3-2)
  freshness     -ignore-trust-freshness only in devflags builds (C8)
  manifest      every trust artifact listed with a matching SHA-256, truthful
                validity dates, and notAfter at least 30 days away (C15)
  mirror        every vendored Maven file is pinned by SHA-256 and names its
                licence (C13)
  key-material  every private key is test-only, listed, and the only key files
                the secret scanner's allowlist excuses (C7)

Needs `cryptography` for certificate dates (tests/requirements-ci.txt).
"""
import argparse
import datetime as dt
import hashlib
import json
import pathlib
import re
import subprocess
import sys
import tomllib

ROOT = pathlib.Path(__file__).resolve().parent.parent
MANIFEST = "trust/MANIFEST.json"
EXPIRY_MARGIN = dt.timedelta(days=30)
# Files that are trust material by name. Everything matching must be in the manifest.
TRUST_PATTERN = re.compile(r"\.(p12|pfx|jks|keystore|pem|cer|crt|der|crl|key)$|^statuslists/")


def tracked(root):
    out = subprocess.run(["git", "ls-files", "-z"], cwd=root, capture_output=True, check=True).stdout
    return [p for p in out.decode().split("\0") if p]


def gate_trust_all(root, files):
    bad = []
    for f in files:
        if f.startswith("app/src/") and f.endswith(".kt") and not f.startswith("app/src/test/"):
            for n, line in enumerate((root / f).read_text().splitlines(), 1):
                if re.search(r"\bsetTrustAll\b|\btrustAll\b|\btrustAllValidator\b", line):
                    bad.append(f"{f}:{n}: chain-validation bypass: {line.strip()}")
    return bad


def gate_mocks(root, files):
    bad = []
    for f in files:
        if f.endswith(".kt") and (f.startswith("app/src/main/") or f.startswith("app/src/real/")
                                  or f.startswith("app/src/release/")):
            for n, line in enumerate((root / f).read_text().splitlines(), 1):
                if re.search(r"\b(class|object)\s+\w+Mock\b", line):
                    bad.append(f"{f}:{n}: mock class outside local_mocks: {line.strip()}")
                if "aarmam.github.io" in line:
                    bad.append(f"{f}:{n}: mock status-list/trust URI outside local_mocks: {line.strip()}")
    return bad


def gate_freshness(root, files):
    bad = []
    for f in files:
        if f.startswith("verifier/") and f.endswith(".go") and not f.endswith("_test.go"):
            text = (root / f).read_text()
            if re.search(r'flag\.\w+\(\s*"ignore-trust-freshness"', text) and "//go:build devflags" not in text:
                bad.append(f"{f}: -ignore-trust-freshness registered outside a devflags build file")
    return bad


def cert_facts(path, entry):
    """(not_before, not_after, has_private_key) for a trust artifact; dates are
    ISO strings or None when the file carries no certificate."""
    from cryptography import x509
    from cryptography.hazmat.primitives.serialization import pkcs12

    data = path.read_bytes()
    certs, private = [], b"PRIVATE KEY-----" in data
    if path.suffix in (".p12", ".pfx", ".keystore"):
        pw = entry.get("store_password", "").encode() or None
        key, cert, extra = pkcs12.load_key_and_certificates(data, pw)
        certs = ([cert] if cert else []) + list(extra or [])
        private = key is not None
    elif b"-----BEGIN CERTIFICATE-----" in data:
        certs = x509.load_pem_x509_certificates(data)
    elif path.suffix in (".cer", ".crt", ".der"):
        certs = [x509.load_der_x509_certificate(data)]
    if not certs:
        return None, None, private
    nb = min(c.not_valid_before_utc for c in certs)
    na = min(c.not_valid_after_utc for c in certs)
    return nb.strftime("%Y-%m-%dT%H:%M:%SZ"), na.strftime("%Y-%m-%dT%H:%M:%SZ"), private


def gate_manifest(root, files, now):
    bad = []
    entries = json.loads((root / MANIFEST).read_text())["artifacts"]
    listed = {e["path"]: e for e in entries}
    for f in files:
        if TRUST_PATTERN.search(f) and f not in listed:
            bad.append(f"{f}: trust material not listed in {MANIFEST}")
    for path, e in listed.items():
        p = root / path
        if not p.is_file():
            bad.append(f"{path}: listed in {MANIFEST} but missing")
            continue
        for field in ("sha256", "source", "owner", "test_only", "purpose"):
            if field not in e:
                bad.append(f"{path}: manifest entry lacks {field}")
        digest = hashlib.sha256(p.read_bytes()).hexdigest()
        if e.get("sha256") != digest:
            bad.append(f"{path}: SHA-256 {digest} does not match the manifest; review the change, then --update")
        try:
            nb, na, private = cert_facts(p, e)
        except Exception as exc:  # unreadable trust material is itself a finding
            bad.append(f"{path}: cannot be read as trust material: {exc}")
            continue
        if (e.get("not_before"), e.get("not_after")) != (nb, na):
            bad.append(f"{path}: manifest dates {e.get('not_before')}..{e.get('not_after')} differ from the file's {nb}..{na}")
        if private and not e.get("test_only"):
            bad.append(f"{path}: holds a private key but is not marked test_only")
        if private != bool(e.get("private_key")):
            bad.append(f"{path}: private_key is {e.get('private_key')} in the manifest, {private} in the file")
        if na and e.get("expiry_gate", True):
            left = dt.datetime.fromisoformat(na.replace("Z", "+00:00")) - now
            if left < EXPIRY_MARGIN:
                bad.append(f"{path}: notAfter {na} is {left.days} days away (gate: {EXPIRY_MARGIN.days}); rotate it")
    return bad


def gate_key_material(root, files):
    bad = []
    entries = {e["path"]: e for e in json.loads((root / MANIFEST).read_text())["artifacts"]}
    cfg = tomllib.loads((root / ".gitleaks.toml").read_text())
    for al in cfg.get("allowlists", []):
        desc = al.get("description", "")
        for rx in al.get("paths", []):
            hits = [f for f in files if re.search(rx, f)]
            if desc.startswith("History-only key material"):
                # Excuses a key that earlier commits carry; at HEAD the path must be gone.
                bad += [f".gitleaks.toml history-only entry {rx!r} matches tracked {f}" for f in hits]
            elif desc.startswith("Test-only key material"):
                if not hits:
                    bad.append(f".gitleaks.toml allowlists {rx!r}, which matches no tracked file; drop it")
                for f in hits:
                    if not entries.get(f, {}).get("test_only"):
                        bad.append(f".gitleaks.toml excuses {f}, which is not a test_only manifest entry")
            else:
                # Any other allowlisted path must not hold a private key (the corrected T1 judge).
                for f in hits:
                    if b"PRIVATE KEY-----" in (root / f).read_bytes():
                        bad.append(f".gitleaks.toml allowlists {f}, which holds a private key")
    for f in files:
        if f.startswith("demo-data/"):
            bad.append(f"{f}: demo-data/ holds the holder's device key and must never be tracked")
    return bad


GOSEC_BASELINE = "verifier/go/.gosec-baseline.json"


def gosec_fingerprints(root, report):
    """rule + file + the flagged source line, so findings survive line shifts.
    Findings in cgo-generated files outside the module are dropped."""
    from collections import Counter
    module = (root / "verifier/go").resolve()
    out = Counter()
    for i in json.loads(pathlib.Path(report).read_text())["Issues"]:
        f = pathlib.Path(i["file"]).resolve()
        if not f.is_relative_to(module):
            continue
        n = int(str(i["line"]).split("-")[0])
        line = f.read_text().splitlines()[n - 1].strip()
        out[f"{i['rule_id']} {f.relative_to(root)} {line}"] += 1
    return out


def gate_gosec(root, report, write):
    found = gosec_fingerprints(root, report)
    path = root / GOSEC_BASELINE
    if write:
        path.write_text(json.dumps(sorted(found.elements()), indent=1) + "\n")
        return []
    from collections import Counter
    base = Counter(json.loads(path.read_text()))
    return [f"new gosec finding: {fp}" for fp in sorted((found - base).elements())]


MIRROR = ".github/maven-mirror"


def gate_mirror(root, files):
    bad = []
    doc = json.loads((root / MIRROR / "manifest.json").read_text())
    pinned = {f"{MIRROR}/site/maven/{a['path']}": a for a in doc["artifacts"]}
    by_name = {(pathlib.Path(f).name, a["sha256"]) for f, a in pinned.items()}
    for f in files:
        if f.startswith(f"{MIRROR}/site/maven/") and not f.endswith("maven-metadata.xml") and f not in pinned:
            # The snapshot path holds byte-identical copies of the timestamped files.
            if (pathlib.Path(f).name, hashlib.sha256((root / f).read_bytes()).hexdigest()) not in by_name:
                bad.append(f"{f}: mirrored file not pinned in {MIRROR}/manifest.json")
    for f, a in pinned.items():
        p = root / f
        if p.is_file() and hashlib.sha256(p.read_bytes()).hexdigest() != a["sha256"]:
            bad.append(f"{f}: bytes do not match the pinned sha256")
        if not a.get("licence"):
            bad.append(f"{f}: no licence recorded")
        if "SNAPSHOT" in a["path"] and "/org/cose/" not in f:
            bad.append(f"{f}: new SNAPSHOT in the mirror")
    return bad


def update(root):
    doc = json.loads((root / MANIFEST).read_text())
    for e in doc["artifacts"]:
        p = root / e["path"]
        e["sha256"] = hashlib.sha256(p.read_bytes()).hexdigest()
        e["not_before"], e["not_after"], e["private_key"] = cert_facts(p, e)
    (root / MANIFEST).write_text(json.dumps(doc, indent=1, ensure_ascii=False) + "\n")


GATES = {"trust-all": gate_trust_all, "mocks": gate_mocks, "freshness": gate_freshness,
         "key-material": gate_key_material}
REPO_ONLY = {"mirror": gate_mirror}  # need the real repository, not a seeded tree


def run(root, now):
    files = tracked(root)
    failures = {name: g(root, files) for name, g in GATES.items()}
    failures["manifest"] = gate_manifest(root, files, now)
    for name, g in REPO_ONLY.items():
        if (root / MIRROR / "manifest.json").is_file():
            failures[name] = g(root, files)
    return failures


def main():
    ap = argparse.ArgumentParser(description="PKI gates")
    ap.add_argument("--update", action="store_true", help="recompute digests and dates in the manifest")
    ap.add_argument("--root", type=pathlib.Path, default=ROOT)
    ap.add_argument("--gosec", metavar="REPORT", help="gosec -fmt=json report to compare with the baseline")
    ap.add_argument("--write-gosec-baseline", action="store_true")
    args = ap.parse_args()
    now = dt.datetime.now(dt.timezone.utc)
    if args.gosec:
        bad = gate_gosec(args.root, args.gosec, args.write_gosec_baseline)
        print("\n".join(bad) or "ok   gosec (no finding outside the baseline)")
        sys.exit(1 if bad else 0)
    if args.update:
        update(args.root)
    failures = run(args.root, now)
    for name, bad in failures.items():
        print(f"{'FAIL' if bad else 'ok  '} {name}")
        for b in bad:
            print(f"     {b}")
    sys.exit(1 if any(failures.values()) else 0)


if __name__ == "__main__":
    main()
