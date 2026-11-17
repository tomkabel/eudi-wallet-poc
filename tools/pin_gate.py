#!/usr/bin/env python3
"""Pin gate (hardening target T2): every external reference in CI and container files must be
pinned by content, not by a name that can move.

  - a `uses:` in .github/ (workflows and composite actions) must be `owner/repo[/path]@<40-hex sha>`;
    `./local` actions and `docker://image@sha256:<64-hex>` are fine;
  - a `FROM` in any Dockerfile must carry `@sha256:<64-hex>` (or name an earlier build stage, or `scratch`).

Exceptions live in tools/pin-gate.allow as `path:line-text-fragment  # reason`; a stale entry fails too.
zizmor (security.yml) checks the same `uses:` rule and more; this gate also covers Dockerfiles.

usage: pin_gate.py [--root DIR]
"""
import argparse
import pathlib
import re
import subprocess
import sys

USES = re.compile(r"^\s*(?:-\s+)?uses:\s*['\"]?([^\s'\"#]+)")
SHA = re.compile(r"^[^@\s]+@[0-9a-f]{40}$")
DOCKER_DIGEST = re.compile(r"^docker://\S+@sha256:[0-9a-f]{64}$")
FROM = re.compile(r"^\s*FROM\s+(?:--\S+\s+)*(\S+)(?:\s+AS\s+(\S+))?", re.I)
DIGEST = re.compile(r"@sha256:[0-9a-f]{64}$")


def allowlist(root):
    p = root / "tools" / "pin-gate.allow"
    out = {}
    if p.is_file():
        for ln in p.read_text().splitlines():
            body = ln.split("#", 1)[0].strip()
            if body:
                path, _, frag = body.partition(":")
                out[(path, frag)] = False
    return out


def tracked(root):
    z = subprocess.run(["git", "ls-files", "-z"], cwd=root, check=True, capture_output=True).stdout
    return [f for f in z.decode().split("\0") if f and (root / f).is_file()]


def violations(root):
    root = pathlib.Path(root)
    allowed = allowlist(root)
    bad = []

    def excused(path, text):
        for key in allowed:
            if key[0] == path and key[1] in text:
                allowed[key] = True
                return True
        return False

    for f in tracked(root):
        name = pathlib.PurePosixPath(f).name
        if f.startswith(".github/") and f.endswith((".yml", ".yaml")):
            for n, line in enumerate((root / f).read_text().splitlines(), 1):
                m = USES.match(line)
                if not m or m.group(1).startswith("./"):
                    continue
                ref = m.group(1)
                if not (SHA.match(ref) or DOCKER_DIGEST.match(ref)) and not excused(f, line):
                    bad.append(f"{f}:{n}: `uses: {ref}` is not pinned to a 40-hex commit SHA")
        elif name == "Dockerfile" or name.startswith("Dockerfile.") or name.endswith(".Dockerfile"):
            stages = set()
            for n, line in enumerate((root / f).read_text().splitlines(), 1):
                m = FROM.match(line)
                if not m:
                    continue
                image, stage = m.group(1), m.group(2)
                if not (DIGEST.search(image) or image == "scratch" or image in stages) and not excused(f, line):
                    bad.append(f"{f}:{n}: `FROM {image}` has no @sha256: digest")
                if stage:
                    stages.add(stage)
    bad += [f"tools/pin-gate.allow: `{p}:{frag}` matched nothing; drop the entry"
            for (p, frag), used in allowed.items() if not used]
    return bad


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default=pathlib.Path(__file__).resolve().parent.parent)
    found = violations(ap.parse_args().root)
    for line in found:
        print(f"pin-gate: {line}", file=sys.stderr)
    sys.exit(1 if found else 0)
