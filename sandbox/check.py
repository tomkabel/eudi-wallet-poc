#!/usr/bin/env python3
"""Drive the running sandbox the way the page does, and assert the story holds.

    python3 sandbox/server.py --port 8765 &   python3 sandbox/check.py http://127.0.0.1:8765

Adult: proves age_over_18 and the shop sees valid:true; the replay is refused.
Minor: the prover refuses; lying past it, the verifier refuses on the value.
Takes about a minute: every case is a real proof.
"""
import json, sys, urllib.request

base = sys.argv[1] if len(sys.argv) > 1 else "http://127.0.0.1:8000"


def post(path, body):
    req = urllib.request.Request(base + path, json.dumps(body).encode(), {"Content-Type": "application/json"})
    with urllib.request.urlopen(req) as r:
        return json.load(r)


def present(holder, mode):
    shop = post("/api/shop/start", {})
    consent = post("/api/wallet/fetch", {"session": shop["session"]})
    assert consent["dcql_query"]["credentials"][0]["claims"][0]["path"][1] == "age_over_18", consent
    events = []
    with urllib.request.urlopen(f"{base}/api/present?holder={holder}&session={shop['session']}&mode={mode}",
                                timeout=180) as r:
        event = None
        for raw in r:
            line = raw.decode().rstrip("\n")
            if line.startswith("event: "):
                event = line[7:]
            elif line.startswith("data: "):
                events.append((event, json.loads(line[6:])))
    done = [d for e, d in events if e == "done"]
    assert done, events
    with urllib.request.urlopen(f"{base}/api/shop/result?session={shop['session']}") as r:
        return done[0], json.load(r)


adult = post("/api/enrol", {"given": "Mari", "family": "Maasikas", "birth_date": "1991-05-14"})
minor = post("/api/enrol", {"given": "Karl", "family": "Kask", "birth_date": "2010-03-02"})
assert adult["predicates"]["age_over_18"] and not minor["predicates"]["age_over_18"]
assert "Mari" not in json.dumps(adult) and "1991" not in json.dumps(adult)

done, shop = present(adult["holder"], "normal")
assert done["code"] == 0 and done["response"]["valid"] is True, done
print("PASS adult proves age_over_18; shop result:", json.dumps(shop)[:120])

done, _ = present(adult["holder"], "replay")
assert done["replay"]["status"] == 410, done
print("PASS replay refused with 410")

done, _ = present(minor["holder"], "normal")
assert done.get("prover_refused"), done
print("PASS minor: prover refuses a false predicate")

done, _ = present(minor["holder"], "lie")
assert done["response"]["valid"] is False, done
print("PASS minor lying past the prover: verifier answers valid:false")
