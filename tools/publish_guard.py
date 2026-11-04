#!/usr/bin/env python3
"""Publish guard: fail when a tracked path could publish a private key.

The fork is public. A path fails unless it is listed in tools/publish-guard.allow when it is
  - under demo-data/ (holds the holder's device private key),
  - a keystore: *.p12, *.pfx, *.jks, *.keystore, or
  - a PEM-family file (*.pem, *.key, *.p8, *.pk8, *.crt, *.cer, *.asc) with a PRIVATE KEY header.
gitleaks covers the same ground by content for any filename; this guard is the cheap, named
allowlist that also catches keystores gitleaks cannot read.

usage: publish_guard.py [--root DIR]    (default: the repo this file lives in)
"""
import argparse
import pathlib
import re
import subprocess
import sys

KEYSTORE = re.compile(r"\.(p12|pfx|jks|keystore)$", re.I)
PEMLIKE = re.compile(r"\.(pem|key|p8|pk8|crt|cer|asc)$", re.I)
HEADER = re.compile(rb"-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----")


def allowlist(root):
    p = root / "tools" / "publish-guard.allow"
    if not p.is_file():
        return set()
    lines = (ln.split("#", 1)[0].strip() for ln in p.read_text().splitlines())
    return {ln for ln in lines if ln}


def violations(root):
    root = pathlib.Path(root)
    allowed = allowlist(root)
    tracked = subprocess.run(["git", "ls-files", "-z"], cwd=root, check=True, capture_output=True
                             ).stdout.decode().split("\0")
    bad = []
    for f in filter(None, tracked):
        if f in allowed or not (root / f).is_file():
            continue
        if f.startswith("demo-data/"):
            bad.append(f"{f}: demo-data/ holds the holder's device key and must never be tracked")
        elif KEYSTORE.search(f):
            bad.append(f"{f}: keystore tracked outside tools/publish-guard.allow")
        elif PEMLIKE.search(f) and HEADER.search((root / f).read_bytes()):
            bad.append(f"{f}: PEM with a PRIVATE KEY header tracked outside tools/publish-guard.allow")
    stale = [a for a in allowed if not (root / a).is_file()]
    bad += [f"tools/publish-guard.allow: {a} is not a tracked file; drop the entry" for a in stale]
    return bad


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=pathlib.Path(__file__).resolve().parent.parent)
    found = violations(ap.parse_args().root)
    for line in found:
        print(f"publish-guard: {line}", file=sys.stderr)
    sys.exit(1 if found else 0)
