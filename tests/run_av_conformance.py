#!/usr/bin/env python3
"""Run AV-linked Go probes and emit fail-closed JSON/readable evidence."""
import argparse, hashlib, json, os, re, shlex, signal, subprocess, sys
import check_av_registry
from av_report import OPERATOR_ACTIONS, complete_candidate, git_record, host_record, render_html
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "docs/profiles/av/clause-registry.json"
PACKAGES = {"verifier/go/oid4vp/av_conformance_test.go": "./oid4vp", "verifier/go/av_conformance_test.go": "."}

def symbols(path):
    return re.findall(r"(?m)^func (TestAVConformance\w+)\(t \*testing\.T\)", path.read_text())

def reject_json_constant(value):
    raise ValueError(f"nonfinite JSON constant: {value}")

def external_results(command, timeout, registry):
    mapped = [c for c in registry.get("clauses", []) if c.get("test")]
    if not command:
        return {"name":"unconfigured external candidate","revision":"unavailable","results":[{"clause_id":c["id"],"test_id":c["test"]["id"],"property":c["test"]["id"].split("::")[-1],"scope":c["test"]["scope"],"limits":c["test"]["limits"],"state":"NOT_EXECUTED","detail":"external candidate unavailable; configure --candidate-command"} for c in mapped],"operator_actions":["Build an independent candidate at a recorded revision.","Implement the JSON adapter contract in tests/README.md.","Rerun with --candidate-command."]}
    payload = {}
    try:
        argv = shlex.split(command)
        if not argv:
            raise ValueError("empty adapter argv")
        proc = subprocess.Popen(argv, cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                start_new_session=True)
    except (OSError, ValueError) as exc:
        state, detail = "NOT_EXECUTED", f"adapter could not start: {exc}"
    else:
        try:
            stdout_bytes, stderr_bytes = proc.communicate(timeout=timeout)
        except subprocess.TimeoutExpired:
            # Kill the original process group even after the direct adapter exits.
            try:
                os.killpg(proc.pid, signal.SIGKILL)
            except ProcessLookupError:
                pass
            state, detail = "ERROR", f"adapter timed out after {timeout}s; process group terminated"
            try:
                proc.communicate(timeout=1)
            except subprocess.TimeoutExpired:
                # Deliberately detached sessions can retain pipes outside our group.
                proc.stdout.close()
                proc.stderr.close()
                detail += "; output drain timed out after 1s; detached pipe holders may remain"
            proc.wait()  # Reap the killed direct adapter, independently of output pipe EOF.
        else:
            streams = {}
            for stream, data in (("stdout", stdout_bytes), ("stderr", stderr_bytes)):
                try:
                    streams[stream] = data.decode("utf-8")
                except UnicodeDecodeError as exc:
                    state, detail = "ERROR", f"adapter {stream} contains malformed UTF-8: {exc}"
                    break
            else:
                try:
                    payload = json.loads(streams["stdout"], object_pairs_hook=check_av_registry.unique_object,
                                         parse_constant=reject_json_constant)
                    # JSON escapes can introduce surrogates despite valid UTF-8 transport.
                    json.dumps(payload, ensure_ascii=False, allow_nan=False).encode("utf-8")
                except (ValueError, UnicodeError, RecursionError) as exc:
                    payload = {}
                    state, detail = "ERROR", f"malformed adapter JSON/text: {exc}"
                    if proc.returncode:
                        detail = f"adapter exited {proc.returncode}: {streams['stderr'].strip()}; " + detail
                else:
                    if proc.returncode: state,detail="ERROR",f"adapter exited {proc.returncode}: {streams['stderr'].strip()}"
                    elif not isinstance(payload,dict) or type(payload.get("schema_version")) is not int or payload["schema_version"]!=1 or not isinstance(payload.get("candidate"),dict) or not isinstance(payload.get("results"),list): state,detail="ERROR","adapter requires integer schema_version=1, candidate object and results array"
                    elif not all(isinstance(payload["candidate"].get(k),str) and payload["candidate"][k].strip() for k in ("name","revision")): state,detail="ERROR","candidate name and revision are required"
                    else: state,detail="OK","validated adapter results"
    candidate=payload.get("candidate",{}) if isinstance(payload,dict) else {}
    if not isinstance(candidate, dict): candidate = {}
    supplied={}
    if state=="OK":
        for result in payload["results"]:
            if not isinstance(result,dict) or not all(isinstance(result.get(k),str) and result[k].strip() for k in ("test_id","property","state","evidence")) or result["state"] not in {"PASS","FAIL","ERROR","NOT_EXECUTED"}:
                state,detail="ERROR","adapter result has malformed evidence or state";supplied={};break
            if result["test_id"] in supplied:
                state,detail="ERROR","adapter duplicated a test_id";supplied={};break
            supplied[result["test_id"]]=result
        mapped_ids = {c["test"]["id"] for c in mapped}
        if state == "OK" and set(supplied) - mapped_ids:
            state, detail = "ERROR", "adapter returned an unmapped test_id"
            supplied = {}
    results=[]
    for clause in mapped:
        test=clause["test"]; item=supplied.get(test["id"])
        item_state=item["state"] if item and state=="OK" else (state if state!="OK" else "NOT_EXECUTED")
        results.append({"clause_id":clause["id"],"test_id":test["id"],"property":item.get("property",test["id"].split("::")[-1]) if item else test["id"].split("::")[-1],"scope":test["scope"],"limits":test["limits"],"state":item_state,"detail":item["evidence"] if item and state=="OK" else detail if state!="OK" else "adapter omitted mapped test evidence"})
    return {"name":candidate.get("name","invalid external candidate"),"revision":candidate.get("revision","unavailable"),"revision_provenance":"adapter-reported; not independently verified by the runner","results":results}

def local_results(registry, skip, timeout):
    tests = {t["id"]: t for t in registry["tests"]}
    for path in PACKAGES:
        for symbol in symbols(ROOT / path):
            tests.setdefault(path + "::" + symbol, {"path": path, "symbol": symbol})
    by_package = {}
    for test_id, test in tests.items():
        parent = Path(test["path"]).parent.relative_to("verifier/go").as_posix()
        package = "." if parent == "." else "./" + parent
        by_package.setdefault(package, {})[test["symbol"]] = test_id
    recorded = {}
    negatives = {}
    eligible_negatives = set()
    diagnostics = []
    executions = []
    env = os.environ.copy()
    env["AV_CONFORMANCE"] = "1"
    for package, package_tests in sorted(by_package.items()):
        if skip:
            for test_id in package_tests.values():
                recorded[test_id] = {"state": "NOT_EXECUTED", "detail": "local execution skipped by --skip-local", "package": package}
            continue
        pattern = "^(?:" + "|".join(re.escape(s) for s in sorted(package_tests)) + ")$"
        command = ["go", "test", "-json", "-count=1", package, "-run", pattern]
        failure = None
        try:
            proc = subprocess.run(command, cwd=ROOT / "verifier/go", env=env, text=True,
                                  capture_output=True, timeout=timeout)
            stdout, stderr, returncode = proc.stdout, proc.stderr, proc.returncode
        except (subprocess.TimeoutExpired, OSError) as exc:
            failure = f"{type(exc).__name__}: {exc}"
            stdout = getattr(exc, "stdout", "") or ""
            stderr = getattr(exc, "stderr", "") or ""
            if isinstance(stdout, bytes): stdout = stdout.decode(errors="replace")
            if isinstance(stderr, bytes): stderr = stderr.decode(errors="replace")
            returncode = None
        terminal = {}
        package_negatives = {}
        outputs = {}
        package_output = []
        for line in stdout.splitlines():
            try: event = json.loads(line)
            except json.JSONDecodeError:
                package_output.append(line)
                continue
            if not isinstance(event, dict): continue
            test_name, action = event.get("Test"), event.get("Action")
            if event.get("Output"):
                if test_name:
                    outputs.setdefault(test_name.split("/")[0], []).append(event["Output"])
                else:
                    package_output.append(event["Output"])
            if test_name and "/negative_" in test_name and action in ("pass", "fail", "skip"):
                package_negatives.setdefault(package + "::" + test_name, []).append(action)
            if test_name in package_tests and action in ("pass", "fail", "skip"):
                terminal.setdefault(test_name, []).append(action)
        package_detail = "".join(package_output) + stderr
        # A tool/build/startup failure is execution evidence, never a property verdict.
        startup_error = bool(re.search(r"SIGILL|signal: illegal instruction|\[build failed\]|build-fail|exec format error", stdout + stderr, re.I))
        execution_error = failure or startup_error or (returncode != 0 and not any("fail" in actions for actions in terminal.values()))
        execution = {"package": package, "command": command, "cwd": "verifier/go", "returncode": returncode,
                     "state": "ERROR" if execution_error else "COMPLETED", "output": package_detail,
                     "detail": failure or ("Go build/startup error" if startup_error else "Go command completed")}
        executions.append(execution)
        for name, actions in package_negatives.items():
            negatives[name] = {"pass": "PASS", "fail": "FAIL", "skip": "NOT_EXECUTED"}[actions[0]] if len(actions) == 1 else "ERROR"
        if not execution_error:
            for name in package_negatives:
                root_test = name.split("::", 1)[1].split("/", 1)[0]
                if terminal.get(root_test) in (["pass"], ["fail"]):
                    eligible_negatives.add(name)
        if execution_error:
            diagnostics.append({"name": "go_package_execution", "package": package, "state": "ERROR",
                                "detail": failure or package_detail or f"go test exited {returncode} without a property failure"})
        for symbol, test_id in package_tests.items():
            actions = terminal.get(symbol, [])
            detail = "".join(outputs.get(symbol, []))
            if failure or startup_error:
                state = "ERROR"
                detail = (failure or "Go build/startup error") + "\n" + detail + package_detail
            elif len(actions) > 1:
                state, detail = "ERROR", "ambiguous Go terminal results\n" + detail
            elif actions:
                state = {"pass": "PASS", "fail": "FAIL", "skip": "NOT_EXECUTED"}[actions[0]]
            else:
                state = "ERROR" if execution_error else "NOT_EXECUTED"
                detail = "missing Go test result\n" + package_detail
            recorded[test_id] = {"state": state, "detail": detail or "Go property probe; no assertion output", "package": package}
    entries = []
    for clause in registry["clauses"]:
        test = clause.get("test")
        if not test: continue
        evidence = recorded.get(test["id"], {"state": "NOT_EXECUTED", "detail": "missing mapped Go test"})
        entries.append({"clause_id": clause["id"], "test_id": test["id"], "property": test["id"].split("::")[-1],
                        "scope": test["scope"], "limits": test["limits"], **evidence})
    covered = len({e["clause_id"] for e in entries if e["state"] in ("PASS", "FAIL")})
    negative = sum(negatives[name] == "PASS" for name in eligible_negatives)
    mapped_ids = {c["test"]["id"] for c in registry["clauses"] if c.get("test")}
    probes = [{"test_id": test_id, "property": tests[test_id]["symbol"],
               "supplementary": test_id not in mapped_ids, **evidence}
              for test_id, evidence in recorded.items()]
    return {"name": "repository verifier", "results": entries, "diagnostics": diagnostics,
            "executions": executions, "probe_results": probes, "negative_results": negatives,
            "negative_count_eligible": sorted(eligible_negatives),
            "coverage": {"unique_clauses": covered, "negative_controls": negative}}


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--candidate-command", help="external adapter command returning schema_version=1 JSON")
    ap.add_argument("--skip-local", action="store_true", help="omit repository probes (useful for adapter-only checks)")
    ap.add_argument("--timeout", type=int, default=60)
    ap.add_argument("--json", default="av-conformance.json")
    ap.add_argument("--html", help="offline HTML output (default: JSON path with .html suffix)")
    ap.add_argument("--min-clauses", type=int, default=20)
    ap.add_argument("--min-negative", type=int, default=8)
    args = ap.parse_args()
    if args.timeout <= 0:
        ap.error("--timeout must be positive")
    json_path = ROOT / args.json
    html_path = ROOT / args.html if args.html else json_path.with_suffix(".html")
    if json_path.resolve() == html_path.resolve() or (json_path.exists() and html_path.exists() and json_path.samefile(html_path)):
        ap.error("--json and --html must be different paths")
    try:
        check_av_registry.validate(ROOT)
        registry_bytes = REGISTRY.read_bytes()
        registry = json.loads(registry_bytes)
        source = json.loads((REGISTRY.parent / "source.json").read_text())
        registry_record = {"path": str(REGISTRY.relative_to(ROOT)),
                           "sha256": hashlib.sha256(registry_bytes).hexdigest()}
        # Capture cleanliness before writing artifacts. Host inputs may be missing/unreadable.
        repository = git_record(ROOT)
        host = host_record(ROOT)
    except (OSError, UnicodeError, ValueError, KeyError) as exc:
        print(f"AV artifact/source validation ERROR: {exc}", file=sys.stderr)
        return 2
    if repository["state"] != "OK" or not re.fullmatch(r"[0-9a-f]{40}", repository["revision"]) or type(repository["dirty"]) is not bool:
        host["diagnostics"].append({"name": "repository_provenance", "state": "ERROR",
                                    "detail": "repository Git revision or cleanliness unavailable; inspect repository provenance"})
    local = local_results(registry, args.skip_local, args.timeout)
    local.update(revision=repository["revision"], dirty=repository["dirty"])
    candidates = [complete_candidate(local, registry),
                  complete_candidate(external_results(args.candidate_command, args.timeout, registry), registry)]
    covered = local["coverage"]["unique_clauses"]
    negative = local["coverage"]["negative_controls"]
    failed = sum(c["counts"][state] for c in candidates for state in ("FAIL", "ERROR", "NOT_EXECUTED"))
    supplementary_failed = sum(r["supplementary"] and r["state"] in ("FAIL", "ERROR", "NOT_EXECUTED") for r in local["probe_results"])
    failed += supplementary_failed
    environment_errors = sum(d["state"] == "ERROR" for d in host["diagnostics"]) + len(local["diagnostics"])
    summary = {"unique_local_clauses": covered, "negative_controls": negative,
               "minimum_clauses": args.min_clauses, "minimum_negative_controls": args.min_negative,
               "thresholds_met": covered >= args.min_clauses and negative >= args.min_negative,
               "failed_results": failed, "supplementary_failed_results": supplementary_failed, "environment_errors": environment_errors,
               "registry_clauses_per_candidate": len(registry["clauses"])}
    exit_code = 1 if failed or environment_errors or not summary["thresholds_met"] else 0
    report = {"schema_version": 1, "registry_revision": registry["source_revision"],
              "generated_at": datetime.now(timezone.utc).isoformat(), "profile": source,
              "repository": repository, "registry": registry_record, "host": host, "candidates": candidates, "summary": summary,
              "audit_exit_code": exit_code,
              "invocation": {"argv": sys.argv, "cwd": str(Path.cwd()), "local_skipped": args.skip_local,
                             "timeout_seconds": args.timeout, "go_environment": {"AV_CONFORMANCE": "1"}},
              "external_reproduction": {"state": "UNPERFORMED", "status": "awaiting-operator", "operator_actions": OPERATOR_ACTIONS}}
    print(f"AV conformance audit: {covered} unique local clauses; {negative} negative controls")
    for candidate in candidates:
        print(f"{candidate['name']} @ {candidate['revision']}: {candidate['counts']}")
        for entry in candidate["results"]:
            print(f"  {entry['state']} {entry['clause_id']} {entry['test_id']} :: {entry['property']} ({entry['detail']})")
    try:
        json_path.write_text(json.dumps(report, indent=2) + "\n")
        html_path.write_text(render_html(report, os.path.relpath(json_path, html_path.parent)))
    except OSError as exc:
        print(f"AV artifact write ERROR: {exc}", file=sys.stderr)
        return 2
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
