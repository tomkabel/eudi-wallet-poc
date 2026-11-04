#!/usr/bin/env python3
"""Python lint ratchet: ruff rule counts, pyright error counts, and format drift of new files.

F and E9 stay the hard gate (pyproject.toml, `ruff check .`). This script holds the wider
rule families and pyright to a committed baseline, tools/lint-baseline.json: a rule whose
count rises, a rule that appears, or a file whose pyright error count rises fails. Counts
that fall are reported so the baseline can shrink (`--update` rewrites it; review the diff).

    python3 tools/lint_ratchet.py            # ruff + pyright + format of new files
    python3 tools/lint_ratchet.py ruff       # one check
    python3 tools/lint_ratchet.py --update   # accept the current numbers (they may only go down in review)
"""

import json
import pathlib
import subprocess
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
BASELINE = ROOT / "tools" / "lint-baseline.json"
FORMAT_BASE = ROOT / "tools" / "ratchet-base.txt"
# Families added on top of pyproject's blocking F,E9. Add one at a time; never remove one.
RATCHET_FAMILIES = "E,W,I,B,UP,SIM,C4,RUF,S"


def run(cmd):
    return subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True)


def ruff_counts():
    r = run(
        [
            "ruff",
            "check",
            "--select",
            "F,E9," + RATCHET_FAMILIES,
            "--output-format",
            "json",
        ]
    )
    if r.returncode not in (0, 1):
        sys.exit(f"ruff failed:\n{r.stderr}")
    counts = {}
    for d in json.loads(r.stdout or "[]"):
        counts[d["code"]] = counts.get(d["code"], 0) + 1
    return counts


def pyright_counts():
    r = run(["pyright", "--outputjson"])
    if r.returncode not in (0, 1):
        sys.exit(f"pyright failed:\n{r.stdout}\n{r.stderr}")
    counts = {}
    for d in json.loads(r.stdout)["generalDiagnostics"]:
        if d["severity"] == "error":
            f = str(pathlib.Path(d["file"]).relative_to(ROOT))
            counts[f] = counts.get(f, 0) + 1
    return counts


def compare(name, base, now):
    """Print the numbers; return the failure lines (a key that rose or is new)."""
    bad = [
        f"{name}: {k} {base.get(k, 0)} -> {v}"
        for k, v in sorted(now.items())
        if v > base.get(k, 0)
    ]
    gone = sum(base.values()) - sum(min(base.get(k, 0), v) for k, v in now.items())
    print(
        f"{name}: {sum(now.values())} now, {sum(base.values())} in baseline, "
        f"{len(bad)} rose or new, {gone} paid down"
    )
    return bad


def format_check():
    base = FORMAT_BASE.read_text().strip()
    if run(["git", "cat-file", "-e", base + "^{commit}"]).returncode:
        return [
            f"format: {base} is not in this clone (shallow? run `git fetch --unshallow`)"
        ]
    added = run(
        ["git", "diff", "--name-only", "--diff-filter=A", base, "--", "*.py"]
    ).stdout.split()
    untracked = run(
        ["git", "ls-files", "--others", "--exclude-standard", "--", "*.py"]
    ).stdout.split()
    files = sorted(
        f
        for f in {*added, *untracked}
        if (ROOT / f).is_file()
        and not f.startswith(("eudi-arf/", "_bmad/", ".agents/", ".claude/"))
    )
    r = run(["ruff", "format", "--check", *files]) if files else None
    print(f"format: {len(files)} new python file(s) since {base[:7]} checked")
    return (
        []
        if r is None or r.returncode == 0
        else ["format: " + r.stdout.strip() + r.stderr.strip()]
    )


def main(argv):
    update = "--update" in argv
    which = [a for a in argv if not a.startswith("-")] or ["ruff", "pyright", "format"]
    data = (
        json.loads(BASELINE.read_text())
        if BASELINE.exists()
        else {"ruff": {}, "pyright": {}}
    )
    failures = []
    if "ruff" in which:
        now = ruff_counts()
        if update:
            data["ruff"], data["ruff_families"] = now, RATCHET_FAMILIES
        failures += compare("ruff", data["ruff"], now)
    if "pyright" in which:
        now = pyright_counts()
        if update:
            data["pyright"] = now
        failures += compare("pyright", data["pyright"], now)
    if update:
        BASELINE.write_text(json.dumps(data, indent=1, sort_keys=True) + "\n")
        print(f"baseline written: {BASELINE.relative_to(ROOT)}")
    if "format" in which:
        failures += format_check()
    for f in failures:
        print("FAIL", f, file=sys.stderr)
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
