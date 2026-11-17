"""Offline AV report provenance and rendering (Python standard library only)."""
import hashlib
import html
import json
import os
import platform
import subprocess
from collections import Counter
from pathlib import Path

GO_ENV_KEYS = ("GOFLAGS", "GOWORK", "GOENV", "CGO_ENABLED", "CC", "CXX", "CGO_CFLAGS",
               "CGO_CPPFLAGS", "CGO_CXXFLAGS", "CGO_FFLAGS", "CGO_LDFLAGS",
               "GOOS", "GOARCH", "GOAMD64", "GOTOOLCHAIN")

STATES = ("PASS", "FAIL", "ERROR", "NOT_EXECUTED", "EXCLUDED")
INDEPENDENCE = "Independent proof of concept; no endorsement or certification is implied."
SCOPE = ("Narrow checks of one boolean age predicate on one attestation and selected container, "
         "request and cryptographic primitives. Passing a probe does not establish full AV or ISO conformance. "
         "The Digital Credentials API is the mandated AV ZK path; OpenID4VP mso_mdoc_zk is a "
         "profiled, unofficial carrier. JVM AVSUA issuance evidence is separate from this Go audit; "
         "its registry exclusions remain visible here.")
OPERATOR_ACTIONS = [
    "Build and execute an independent candidate at a recorded source revision using the adapter contract in tests/README.md.",
    "Have an independent human or organization regenerate the report from a clean checkout using docs/profiles/AV-REPORT-REPRODUCTION.md.",
    "Record elapsed time, host, exact commands, transcript and both artifacts; review differences and publish the evidence at an operator-selected location.",
]


def command_record(command, cwd):
    try:
        proc = subprocess.run(command, cwd=cwd, text=True, capture_output=True, timeout=10)
        return {"command": command, "returncode": proc.returncode,
                "output": (proc.stdout + proc.stderr).rstrip(),
                "state": "OK" if proc.returncode == 0 else "UNAVAILABLE"}
    except (OSError, subprocess.TimeoutExpired) as exc:
        return {"command": command, "returncode": None, "output": str(exc), "state": "UNAVAILABLE"}


def git_record(path):
    # --show-toplevel prevents a missing sibling .git from resolving a parent repository.
    top = command_record(["git", "rev-parse", "--show-toplevel"], path) if path.is_dir() else None
    if not top or top["state"] != "OK" or Path(top["output"]).resolve() != path.resolve():
        return {"revision": "unavailable", "dirty": None, "changes": [], "state": "UNAVAILABLE"}
    rev = command_record(["git", "rev-parse", "--verify", "HEAD^{commit}"], path)
    status = command_record(["git", "status", "--porcelain", "--untracked-files=all"], path)
    return {"revision": rev["output"] if rev["state"] == "OK" else "unavailable",
            "dirty": bool(status["output"]) if status["state"] == "OK" else None,
            "changes": status["output"].splitlines(),
            "state": "OK" if rev["state"] == status["state"] == "OK" else "UNAVAILABLE"}


def file_record(path):
    if not path.is_file():
        return {"exists": False}
    return {"exists": True, "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "size_bytes": path.stat().st_size, "modified_ns": path.stat().st_mtime_ns}


def host_record(root):
    cpu_text = Path("/proc/cpuinfo").read_text() if Path("/proc/cpuinfo").is_file() else ""
    fields = {}
    flag_sets = []
    for line in cpu_text.splitlines():
        key, sep, value = line.partition(":")
        if sep:
            fields.setdefault(key.strip(), value.strip())
            if key.strip() in ("flags", "Features"):
                flag_sets.append(set(value.split()))
    flags = set.intersection(*flag_sets) if flag_sets else set()
    # Linux exposes lzcnt as abm on some CPUs. This records advertised flags, not a runtime proof.
    required = {"avx", "avx2", "bmi1", "bmi2", "f16c", "fma", "movbe", "xsave", "ssse3", "sse4_1", "sse4_2", "popcnt", "cx16", "lahf_lm"}
    missing = sorted(required - flags)
    if not flags.intersection({"abm", "lzcnt"}):
        missing.append("abm/lzcnt")
    if not flags.intersection({"pni", "sse3"}):
        missing.append("pni/sse3")
    baseline_state = "MISMATCH" if platform.machine() not in ("x86_64", "AMD64") else "UNKNOWN" if not flags else "MISMATCH" if missing else "ADVERTISED"
    config = root / "verifier/zkverify-ffi/.cargo/config.toml"
    pin = (root / "verifier/zkverify-ffi/longfellow-rev.txt").read_text().strip()
    sibling = root.parent / "longfellow-zk"
    lf = git_record(sibling)
    lf.update({"path": "../longfellow-zk", "pinned_revision": pin,
               "revision_matches_pin": lf["revision"] == pin})
    diagnostics = []
    if not lf["revision_matches_pin"]:
        diagnostics.append({"name": "longfellow_revision", "state": "ERROR",
                            "detail": f"sibling revision {lf['revision']} differs from pin {pin}; run make deps"})
    if baseline_state == "MISMATCH":
        diagnostics.append({"name": "cpu_baseline", "state": "ERROR", "detail": "x86-64-v3 advertised feature mismatch: " + ", ".join(missing)})
    staticlib = file_record(root / "verifier/zkverify-ffi/target/release/libzkverify.a")
    tools = {name: command_record(cmd, root) for name, cmd in {
        "go": ["go", "version"], "rustc": ["rustc", "--version", "--verbose"],
        "cargo": ["cargo", "--version"], "jdk": ["java", "-version"]}.items()}
    for name, record in tools.items():
        if record["state"] != "OK":
            diagnostics.append({"name": name, "state": "UNAVAILABLE", "detail": record["output"] + ("; JDK is not required by the Go runner" if name == "jdk" else "")})
    go_environment = command_record(["go", "env", "-json", *GO_ENV_KEYS], root / "verifier/go")
    try:
        values = json.loads(go_environment["output"]) if go_environment["state"] == "OK" else {}
        if not isinstance(values, dict):
            raise ValueError("go env did not return an object")
    except ValueError as exc:
        values = {}
        go_environment.update(state="UNAVAILABLE", detail=str(exc))
    go_environment.update(values=values, observed_environment={k: os.environ.get(k) for k in GO_ENV_KEYS},
                          assessment_limit="Current effective Go settings, including overlay flags in GOFLAGS; not an attestation of historical compilation.")
    if go_environment["state"] != "OK":
        diagnostics.append({"name": "go_environment", "state": "UNAVAILABLE", "detail": go_environment.get("detail", go_environment["output"])})
    return {"hostname": platform.node(), "os": platform.platform(), "architecture": platform.machine(),
            "python": platform.python_version(),
            "cpu": {"model": fields.get("model name", fields.get("Hardware", "unavailable")),
                    "logical_cpus": os.cpu_count(), "required_baseline": "x86-64-v3",
                    "baseline_state": baseline_state, "missing_advertised_flags": missing if flags else [],
                    "advertised_flags": sorted(flags),
                    "assessment_limit": "Advertised flags only; OS support and the actual binary instruction set are not attested."},
            "rustflags": {"required_baseline": "-C target-cpu=x86-64-v3",
                          "makefile": "Makefile: RUSTFLAGS assignment applies to prover build",
                          "ffi_config_path": str(config.relative_to(root)),
                          "ffi_config": config.read_text(),
                          "observed_environment": {k: os.environ.get(k) for k in ("RUSTFLAGS", "CARGO_ENCODED_RUSTFLAGS", "CARGO_BUILD_RUSTFLAGS", "CARGO_TARGET_X86_64_UNKNOWN_LINUX_GNU_RUSTFLAGS")},
                          "build_attestation": "Configuration and current environment only; historical compiler flags and linked source revision are not proven by this runner."},
            "tools": tools, "go_environment": go_environment, "longfellow": lf, "staticlib": staticlib, "diagnostics": diagnostics}


def complete_candidate(candidate, registry):
    by_clause = {}
    for result in candidate["results"]:
        by_clause.setdefault(result["clause_id"], []).append(result)
    rows = []
    for clause in registry["clauses"]:
        row = {"clause_id": clause["id"], "excerpt": clause["excerpt"]}
        if not clause.get("test"):
            row.update(state="EXCLUDED", detail=clause["out_of_scope_for_v1"])
        else:
            matches = by_clause.get(clause["id"], [])
            mapping = clause["test"]
            row.update(test_id=mapping["id"], scope=mapping["scope"], limits=mapping["limits"])
            if len(matches) == 1:
                row.update(matches[0])
            else:
                row.update(state="ERROR" if matches else "NOT_EXECUTED", property=mapping["id"].split("::")[-1],
                           detail="ambiguous mapped result" if matches else "missing mapped result")
        rows.append(row)
    candidate["clauses"] = rows
    counts = Counter(row["state"] for row in rows)
    candidate["counts"] = {state: counts[state] for state in STATES}
    return candidate


def render_html(report, json_name):
    esc = lambda value: html.escape(str(value), quote=True)
    def evidence(title, value):
        return f'<details><summary>{esc(title)}</summary><pre>{esc(json.dumps(value, indent=2, ensure_ascii=False))}</pre></details>'
    parts = ['<!doctype html><html lang="en"><head><meta charset="utf-8">',
             '<meta name="viewport" content="width=device-width, initial-scale=1">',
             '<meta http-equiv="Content-Security-Policy" content="default-src \'none\'; style-src \'unsafe-inline\'; base-uri \'none\'; form-action \'none\'">',
             '<title>AV Profile evidence report</title><style>',
             'body{margin:0;color:#151926;background:#fff;font:1rem/1.5 Roboto,"Segoe UI","Helvetica Neue",Arial,sans-serif} '
             'header,footer{background:#003662;color:white;padding:16px 24px} main{max-width:960px;margin:auto;padding:32px 24px} '
             'h1{font-size:clamp(2rem,1.2rem + 3vw,3.25rem);font-weight:300;line-height:1.12} h2{font-size:1.75rem;font-weight:400;margin-top:48px} '
             'article{border:1px solid #e1e2e5;border-radius:4px;padding:16px;margin:12px 0} '
             'h3{font-size:1.125rem;margin:0 0 8px} a,summary{color:#005aa3} a:focus-visible,summary:focus-visible{outline:3px solid #005aa3;outline-offset:4px} '
             'summary{cursor:pointer;padding:8px 0} pre{background:#f0f0f2;padding:16px;white-space:pre-wrap;overflow-wrap:anywhere;font:0.875rem/1.5 ui-monospace,monospace} '
             'code{font:0.875rem/1.5 ui-monospace,monospace;overflow-wrap:anywhere} p{overflow-wrap:anywhere} '
             '.state{display:inline-block;border:1px solid #838494;border-radius:4px;padding:4px 8px;background:#f9f9f9} '
             '.PASS{color:#266b42;background:#eaf3ee;border-color:#599e75} .FAIL,.ERROR{color:#ac3232;background:#fbecec;border-color:#df6565} '
             '.NOT_EXECUTED{color:#664807;background:#fff0cf;border-color:#ba830d} .notice{border:1px solid #337bb5;background:#e7f0f6;padding:16px;border-radius:4px} '
             '@media(max-width:640px){main{padding:24px 16px}} @media print{header,footer{background:white;color:#151926} details{display:block} pre{white-space:pre-wrap}}',
             '</style></head><body><header>' + esc(INDEPENDENCE) + '</header><main>',
             '<h1>AV Profile evidence report</h1>', '<p class="notice">' + esc(SCOPE) + '</p>',
             f'<p>Profile version / full source revision: <code>{esc(report["profile"]["revision"])}</code></p>',
             f'<p>Repository commit: <code>{esc(report["repository"]["revision"])}</code>; dirty at audit start: {esc(report["repository"]["dirty"])}</p>',
             f'<p>Generated: {esc(report["generated_at"])}. Host: {esc(report["host"]["hostname"])}; {esc(report["host"]["cpu"]["model"])}; required CPU baseline: x86-64-v3.</p>',
             f'<p>Sibling JSON artifact: <code>{esc(json_name)}</code>. Audit exit: {report["audit_exit_code"]}.</p>',
             '<h2>Summary</h2>']
    for candidate in report["candidates"]:
        parts.append(f'<p>{esc(candidate["name"])}: ' + '; '.join(f'{state}: {candidate["counts"][state]}' for state in STATES) + '</p>')
    parts.append(evidence("Audit counts and thresholds", report["summary"]))
    parts.append('<h2>Environment diagnostics</h2><p>Environment errors describe execution conditions separately from property failures. Unavailable optional tools do not imply a property failure.</p>')
    diagnostics = report["host"]["diagnostics"] + [d for c in report["candidates"] for d in c.get("diagnostics", [])]
    parts.append(evidence("Environment and package execution diagnostics", diagnostics))
    for candidate in report["candidates"]:
        parts.append(f'<section><h2>{esc(candidate["name"])}</h2><p>Candidate revision: <code>{esc(candidate["revision"])}</code></p>')
        for title, key in (("Go commands, package exit codes and output", "executions"),
                           ("All named probe results, including supplementary probes", "probe_results"),
                           ("Named negative control outcomes", "negative_results"),
                           ("Negative controls eligible for threshold counting", "negative_count_eligible")):
            if key in candidate:
                parts.append(evidence(title, candidate[key]))
        for row in candidate["clauses"]:
            state = row["state"]
            parts.append(f'<article><h3><span class="state {state}">{state}</span> {esc(row["clause_id"])}</h3>')
            if state == "EXCLUDED":
                parts.append('<p>' + esc(row["detail"]) + '</p>')
            else:
                parts.append(f'<p>Test: <code>{esc(row["test_id"])}</code></p><p>Property: <code>{esc(row["property"])}</code></p>')
            parts.append(evidence("Clause and evidence", row))
            parts.append('</article>')
        parts.append('</section>')
    parts.append('<h2>Reproduction details</h2><p>External reproduction is UNPERFORMED. An agent-local checkout is local verification and cannot establish independent reproduction.</p>')
    parts.append('<p>Follow docs/profiles/AV-REPORT-REPRODUCTION.md at the recorded implementation commit. A later artifact commit may contain this evidence without changing the evidence commit.</p>')
    parts.append('<ol>' + ''.join('<li>' + esc(action) + '</li>' for action in report["external_reproduction"]["operator_actions"]) + '</ol>')
    for title, value in (("Profile source and hashes", report["profile"]), ("Clause registry bytes and path", report["registry"]), ("Repository cleanliness at audit start", report["repository"]), ("Host and build configuration", report["host"]), ("Invocation", report["invocation"]), ("External reproduction status", report["external_reproduction"])):
        parts.append(evidence(title, value))
    parts.append('</main><footer>' + esc(INDEPENDENCE) + '</footer></body></html>\n')
    return '\n'.join(parts)
