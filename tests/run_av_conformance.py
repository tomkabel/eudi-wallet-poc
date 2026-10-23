#!/usr/bin/env python3
"""Run AV-linked Go probes and emit fail-closed JSON/readable evidence."""
import argparse, json, os, re, subprocess, sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "docs/profiles/av/clause-registry.json"
PACKAGES = {"verifier/go/oid4vp/av_conformance_test.go": "./oid4vp", "verifier/go/av_conformance_test.go": "."}

def symbols(path):
    return re.findall(r"(?m)^func (TestAVConformance\w+)\(t \*testing\.T\)", path.read_text())

def external_results(command, timeout, registry):
    mapped = [c for c in registry.get("clauses", []) if c.get("test")]
    if not command:
        return {"name":"unconfigured external candidate","revision":"unavailable","results":[{"clause_id":c["id"],"test_id":c["test"]["id"],"property":c["test"]["id"].split("::")[-1],"scope":c["test"]["scope"],"limits":c["test"]["limits"],"state":"NOT_EXECUTED","detail":"external candidate unavailable; configure --candidate-command"} for c in mapped],"operator_actions":["Build an independent candidate at a recorded revision.","Implement the JSON adapter contract in tests/README.md.","Rerun with --candidate-command."]}
    try:
        proc=subprocess.run(command,cwd=ROOT,shell=True,text=True,capture_output=True,timeout=timeout)
    except subprocess.TimeoutExpired:
        state,detail="ERROR",f"adapter timed out after {timeout}s"; payload={}
    except OSError as exc:
        state,detail="NOT_EXECUTED",f"adapter could not start: {exc}"; payload={}
    else:
        try: payload=json.loads(proc.stdout)
        except json.JSONDecodeError as exc: payload={};state,detail="ERROR",f"malformed adapter JSON: {exc}"
        else:
            if proc.returncode: state,detail="ERROR",f"adapter exited {proc.returncode}"
            elif not isinstance(payload,dict) or payload.get("schema_version")!=1 or not isinstance(payload.get("candidate"),dict) or not isinstance(payload.get("results"),list): state,detail="ERROR","adapter requires schema_version=1, candidate object and results array"
            elif not all(isinstance(payload["candidate"].get(k),str) and payload["candidate"][k].strip() for k in ("name","revision")): state,detail="ERROR","candidate name and revision are required"
            else: state,detail="OK","validated adapter results"
    candidate=payload.get("candidate",{}) if isinstance(payload,dict) else {}
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
    return {"name":candidate.get("name","invalid external candidate"),"revision":candidate.get("revision","unavailable"),"results":results}

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--candidate-command", help="external adapter command returning schema_version=1 JSON")
    ap.add_argument("--skip-local", action="store_true", help="omit repository probes (useful for adapter-only checks)")
    ap.add_argument("--timeout", type=int, default=60)
    ap.add_argument("--json", default="av-conformance.json")
    ap.add_argument("--min-clauses", type=int, default=20)
    ap.add_argument("--min-negative", type=int, default=8)
    args = ap.parse_args()
    try:
        registry = json.loads(REGISTRY.read_text())
        if registry.get("source_revision") != "014dc2fd0f9332eeabe1eef70d234ed2aef20fd7": raise ValueError("registry revision mismatch")
    except Exception as exc:
        print(f"AV audit ERROR: {exc}", file=sys.stderr); return 2
    all_symbols = {s for p in PACKAGES for s in symbols(ROOT / p)}
    registry_symbols = {t["symbol"]: t for t in registry.get("tests", [])}
    all_symbols |= set(registry_symbols)
    states = {}
    negative_states = {}
    env = os.environ.copy(); env["AV_CONFORMANCE"] = "1"
    by_package = {p: [s for s in all_symbols if (registry_symbols.get(s, {}).get("path") in PACKAGES and PACKAGES[registry_symbols[s]["path"]] == p) or any(s in symbols(ROOT/f) and PACKAGES[f] == p for f in PACKAGES)] for p in set(PACKAGES.values())}
    for symbol, test in registry_symbols.items():
        rel=Path(test["path"]); package="./"+rel.parent.relative_to("verifier/go").as_posix() if rel.parent != Path("verifier/go") else "."
        by_package.setdefault(package, []).append(symbol)
    for package, package_symbols in by_package.items():
        if args.skip_local:
            for symbol in package_symbols:
                states[symbol] = "NOT_EXECUTED"
            continue
        if not package_symbols: continue
        try:
            pattern="^(?:"+"|".join(re.escape(s) for s in sorted(set(package_symbols)))+")$"
            proc = subprocess.run(["go", "test", "-json", "-count=1", package, "-run", pattern], cwd=ROOT / "verifier/go", env=env, text=True, capture_output=True, timeout=args.timeout)
        except (subprocess.TimeoutExpired, OSError) as exc:
            proc = None; error = str(exc)
        else: error = (proc.stderr or "")[-2000:]
        for line in (proc.stdout if proc else "").splitlines():
            try: event = json.loads(line)
            except json.JSONDecodeError: continue
            test_name = event.get("Test")
            action = event.get("Action")
            if test_name and "/negative_" in test_name and action in ("pass", "fail", "skip"):
                negative_states[test_name] = {"pass":"PASS", "fail":"FAIL", "skip":"NOT_EXECUTED"}[action]
            if test_name in all_symbols and action in ("pass", "fail", "skip"):
                states[test_name] = {"pass":"PASS", "fail":"FAIL", "skip":"NOT_EXECUTED"}[action]
        for symbol in all_symbols: states.setdefault(symbol, "ERROR" if proc is None or proc.returncode else "NOT_EXECUTED")
    entries=[]
    for clause in registry.get("clauses", []):
        mapping=clause.get("test")
        if not mapping: continue
        symbol=mapping["id"].split("::")[-1]
        if symbol in states: entries.append({"clause_id":clause["id"],"test_id":mapping["id"],"property":symbol,"scope":mapping["scope"],"limits":mapping["limits"],"state":states[symbol],"detail":error if states[symbol]=="ERROR" else "Go property probe"})
    covered={e["clause_id"] for e in entries if e["state"] in ("PASS","FAIL")}
    negative=sum(state == "PASS" for state in negative_states.values())
    report={"schema_version":1,"registry_revision":registry["source_revision"],"generated_at":datetime.now(timezone.utc).isoformat(),"candidates":[{"name":"repository verifier","revision":subprocess.run(["git","rev-parse","HEAD"],cwd=ROOT,text=True,capture_output=True).stdout.strip() + ("+dirty" if subprocess.run(["git","status","--porcelain"],cwd=ROOT,text=True,capture_output=True).stdout.strip() else ""),"results":entries,"coverage":{"unique_clauses":len(covered),"negative_controls":negative}},external_results(args.candidate_command,args.timeout,registry)]}
    failed=sum(e["state"] in ("FAIL","ERROR","NOT_EXECUTED") for c in report["candidates"] for e in c["results"])
    report["summary"]={"unique_local_clauses":len(covered),"negative_controls":negative,"thresholds_met":len(covered)>=args.min_clauses and negative>=args.min_negative,"failed_results":failed}
    print(f"AV conformance audit: {len(covered)} unique local clauses; {negative} negative controls")
    for c in report["candidates"]:
        print(f"{c['name']} @ {c['revision']}")
        for e in c["results"]: print(f"  {e['state']} {e['clause_id']} {e['test_id']} :: {e['property']} ({e['detail']})")
    (ROOT/args.json).write_text(json.dumps(report, indent=2)+"\n")
    return 1 if failed or not report["summary"]["thresholds_met"] else 0

if __name__ == "__main__": sys.exit(main())
