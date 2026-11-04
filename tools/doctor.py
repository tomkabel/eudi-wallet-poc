#!/usr/bin/env python3
"""make doctor: check that this machine can build and test the repo.

Prints one line per failed check, a summary line, and exits 1 when any check failed.
Minimums come from verifier/go/go.mod (Go) and mise.toml (everything else), so the pins
live in one place. `make deps` records the built prover's sha256 and the Longfellow revision
it was built from next to the binary; the doctor re-checks both.

  DOCTOR_SKIP="android java"   skip check groups: toolchain, java, android, prover, longfellow
  DOCTOR_VERBOSE=1             also print passing checks
  DOCTOR_SIBLING=DIR           check this longfellow-zk checkout instead of ../longfellow-zk
"""
import hashlib
import importlib.metadata
import os
import pathlib
import re
import shutil
import subprocess
import sys
import tomllib

ROOT = pathlib.Path(__file__).resolve().parent.parent
SIBLING = pathlib.Path(os.environ.get("DOCTOR_SIBLING") or ROOT.parent / "longfellow-zk")
SKIP = set(os.environ.get("DOCTOR_SKIP", "").split())
VERBOSE = bool(os.environ.get("DOCTOR_VERBOSE"))
EXAMPLES = SIBLING / "rust" / "target" / "release" / "examples"
PROVER = EXAMPLES / "ee_poa_demo"
FFI = ROOT / "verifier" / "zkverify-ffi"
results = []  # (group, name, error-or-None)


def vtuple(s):
    return tuple(int(x) for x in re.findall(r"\d+", s)[:3])


def run(*cmd, cwd=None):
    try:
        p = subprocess.run(cmd, cwd=cwd, capture_output=True, text=True, timeout=30)
    except (OSError, subprocess.SubprocessError) as e:
        return None, str(e)
    return p.returncode, (p.stdout + p.stderr).strip()


def check(group, name, fn):
    if group in SKIP:
        return
    try:
        err = fn()
    except Exception as e:  # a crashing check is a failed check, never a pass
        err = f"{type(e).__name__}: {e}"
    results.append((group, name, err))


def at_least(tool, have, want):
    if not have:
        return f"{tool} not found (want >= {want})"
    if vtuple(have) < vtuple(want):
        return f"{tool} {have} < {want}"


def tool_version(cmd, pattern):
    if not shutil.which(cmd[0]):
        return None
    _, out = run(*cmd)
    m = re.search(pattern, out)
    return m.group(1) if m else None


def pins():
    return tomllib.loads((ROOT / "mise.toml").read_text())["tools"]


def go():
    m = re.search(r"^go (\S+)", (ROOT / "verifier/go/go.mod").read_text(), re.M)
    if not m:
        return "no `go` directive in verifier/go/go.mod"
    return at_least("go", tool_version(["go", "version"], r"go(\d+\.\d+(?:\.\d+)?)"), m.group(1))


def python():
    v = ".".join(map(str, sys.version_info[:3]))
    return at_least("python3", v, pins()["python"])


def rust():
    want = pins()["rust"]
    return (at_least("rustc", tool_version(["rustc", "--version"], r"rustc (\S+)"), want)
            or at_least("cargo", tool_version(["cargo", "--version"], r"cargo (\S+)"), want))


def ruff():
    return at_least("ruff", tool_version(["ruff", "--version"], r"ruff (\S+)"), pins()["ruff"])


def shellcheck():
    if not shutil.which("shellcheck"):
        return "shellcheck not found (make lint runs it)"


def java17():
    candidates = [os.environ.get("JAVA_HOME"), *map(str, sorted(pathlib.Path("/usr/lib/jvm").glob("*17*")))]
    bins = [pathlib.Path(c) / "bin" / "java" for c in candidates if c] + [shutil.which("java")]
    for b in filter(None, bins):
        _, out = run(str(b), "-version")
        m = re.search(r'version "(\d+)', out)
        if m and m.group(1) == "17":
            return None
    return "no JDK 17 found (JAVA_HOME, /usr/lib/jvm/*17*, PATH); Gradle here needs 17"


def python_pins():
    """Installed Python packages must be the ones uv.lock pins (it holds the golden-vector inputs)."""
    lock = tomllib.loads((ROOT / "uv.lock").read_text())["package"]
    bad = []
    for pkg in lock:
        if pkg.get("source", {}).get("virtual"):  # the project itself
            continue
        try:
            have = importlib.metadata.version(pkg["name"])
        except importlib.metadata.PackageNotFoundError:
            have = "missing"
        if have != pkg["version"]:
            bad.append(f"{pkg['name']} {have} != {pkg['version']}")
    if bad:
        return "; ".join(bad) + " (make py-deps; the transcript vectors are golden)"


def uv_lock_fresh():
    if not shutil.which("uv"):
        return "uv not found (make py-deps needs it)"
    rc, out = run("uv", "lock", "--check", cwd=ROOT)
    if rc != 0:
        return "uv.lock is out of date with pyproject.toml (run `uv lock`): " + out.splitlines()[-1] if out else "uv lock --check failed"


def android():
    cands = [os.environ.get("ANDROID_HOME"), os.environ.get("ANDROID_SDK_ROOT")]
    lp = ROOT / "local.properties"
    if lp.is_file():
        m = re.search(r"^sdk\.dir=(.+)$", lp.read_text(), re.M)
        cands.append(m and m.group(1).strip())
    for c in filter(None, cands):
        if (pathlib.Path(c) / "platforms").is_dir():
            return None
    return "no Android SDK with platforms/ (ANDROID_HOME, ANDROID_SDK_ROOT or local.properties sdk.dir)"


def rustflags():
    rf = os.environ.get("RUSTFLAGS", "")
    if "target-cpu=native" in rf:
        return "RUSTFLAGS has target-cpu=native, which builds SIGILL binaries on virtualised hosts; use -C target-cpu=x86-64-v3"
    cfg = FFI / ".cargo" / "config.toml"
    if "target-cpu=x86-64-v3" not in cfg.read_text():
        return f"{cfg.relative_to(ROOT)} does not pin target-cpu=x86-64-v3"


def executable(path, hint):
    def f():
        if not (path.is_file() and os.access(path, os.X_OK)):
            return f"{path} missing or not executable ({hint})"
    return f


def staticlib():
    p = FFI / "target/release/libzkverify.a"
    if not p.is_file():
        return f"{p.relative_to(ROOT)} missing (go test links it; make deps)"


def pinned_rev():
    return (FFI / "longfellow-rev.txt").read_text().strip()


def prover_record():
    rec = EXAMPLES / "ee_poa_demo.built"
    if not rec.is_file():
        return f"{rec} missing: the prover was not built by make deps (it records its sha256 and revision)"
    if not PROVER.is_file():
        return "no prover binary to checksum"
    sha, rev = rec.read_text().split()
    have = hashlib.sha256(PROVER.read_bytes()).hexdigest()
    if have != sha:
        return f"prover sha256 {have[:12]} != recorded {sha[:12]}: binary changed since make deps built it"
    if rev != pinned_rev():
        return f"prover was built from {rev[:12]}, pinned is {pinned_rev()[:12]}: rerun make deps"


def sibling_rev():
    if not (SIBLING / ".git").exists():
        return f"{SIBLING} is not a git checkout (make deps)"
    _, head = run("git", "-C", str(SIBLING), "rev-parse", "HEAD")
    if head != pinned_rev():
        return f"{SIBLING.name} is at {head[:12]}, pinned is {pinned_rev()[:12]} (make deps)"


def sibling_clean():
    rc, out = run("git", "-C", str(SIBLING), "diff", "--stat", "HEAD")
    if rc != 0 or out:
        return f"{SIBLING.name} has tracked modifications or is not a repository:\n{out[:300]}"


def main():
    check("toolchain", "go", go)
    check("toolchain", "python3", python)
    check("toolchain", "rust", rust)
    check("toolchain", "ruff", ruff)
    check("toolchain", "shellcheck", shellcheck)
    check("toolchain", "python pins (uv.lock)", python_pins)
    check("toolchain", "uv.lock matches pyproject.toml", uv_lock_fresh)
    check("toolchain", "RUSTFLAGS / target-cpu", rustflags)
    check("java", "java 17", java17)
    check("android", "Android SDK", android)
    check("prover", "prover binary (test -x)", executable(PROVER, "make deps"))
    check("prover", "prover checksum and revision", prover_record)
    check("prover", "libzkverify.a", staticlib)
    check("prover", "verifier binary (test -x)", executable(ROOT / "verifier/zkverify", "make deps"))
    check("longfellow", "longfellow-zk at the pinned revision", sibling_rev)
    check("longfellow", "longfellow-zk has no tracked modifications", sibling_clean)
    failed = 0
    for _, name, err in results:
        if err:
            failed += 1
            print(f"FAIL {name}: {err}")
        elif VERBOSE:
            print(f"ok   {name}")
    skipped = f", skipped: {' '.join(sorted(SKIP))}" if SKIP else ""
    print(f"doctor: {len(results) - failed}/{len(results)} checks passed{skipped}")
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
