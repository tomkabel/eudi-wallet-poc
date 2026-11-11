# External TARA test attempt

Actual command: make test-tara-integration. The runner child returned 1; make
returned 2. attempt-2026-11-12.json is its terminal stdout JSON (make's command
echo excluded). The filename uses the Tallinn local date, 12 November 2026;
the exact UTC timestamp in the report is 11 November 2026, before local midnight.

The canonical baseline revision names the checkout HEAD; source_sha256 binds
the two then-uncommitted executable files used for the attempt. This is agent
execution evidence in a working tree, not a claim of an already-committed source
revision or a successful live integration. Reproduce against the final committed
revision after operator setup. No provider/network authentication was attempted:
runtime agreement confirmation, registered-client confirmation, client ID, secret
and callback were absent. Outcome BLOCKED and authenticated=false are required.
The JSON retains imperative operator actions; no secrets, callback queries,
codes, tokens or personal identifiers were recorded.

## Fresh post-review attempt

The unchanged attempt-2026-11-12.json above is historical: its executable hashes
describe the earlier implementation and must not be treated as current hashes.
After review fixes, make test-tara-integration was actually run again;
[attempt-2026-11-12-review.json](attempt-2026-11-12-review.json) retains the fresh
terminal report, baseline HEAD, current executable hashes and effective runner
port/timeout. The runner again returned 1 and make returned 2 with BLOCKED,
before any provider network call, because the institutional/runtime prerequisites
and client configuration remain absent. Neither report establishes live success.
