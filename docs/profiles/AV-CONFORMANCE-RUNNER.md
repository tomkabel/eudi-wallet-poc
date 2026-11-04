# AV conformance runner

`python3 tests/run_av_conformance.py --json av-conformance.json` runs the Go
tests referenced by the pinned clause registry plus the dedicated local AV
probes. Each report row records a candidate, clause, test, property, scope,
limits, state and evidence. Unique clause coverage and negative controls are
counted separately; negative controls count only passing executed Go
subtests. The registry retains its complete 94-clause denominator;
20 clauses currently have executable mappings.

Every invocation also writes self-contained offline HTML, defaulting to the JSON
path with an `.html` suffix, or selected with `--html`. Both artifacts retain
all 94 clauses per candidate in profile order, including exclusions, and record
host/build provenance separately from property failures. See the
[clean-checkout reproduction guide](AV-REPORT-REPRODUCTION.md) and
[published report](av/report.html).

The audit exits nonzero when an in-scope property fails, errors or is not
executed, when external candidate evidence is unavailable, or when coverage
falls below 20 clauses/eight negative controls. Existing fallback-policy
differences are reported as failures; production policy is not adjusted to
make this report pass. A partial primitive probe is not complete AV or ISO
conformance evidence.

An external adapter can be supplied with `--candidate-command '<command>'`.
The runner uses `shlex.split` to preserve quoted arguments and executes the
resulting argv directly. Shell operators, variable expansion and globbing are
not interpreted. For example, use `--candidate-command 'python3 "/path with spaces/adapter.py"'`.
To use a shell, explicitly name a shell or executable script as the adapter.
A missing executable, malformed quoting or empty argv is `NOT_EXECUTED` with
startup detail. Once the executable starts, any nonzero exit (including 126 or
127) is `ERROR` with its diagnostic. An explicitly invoked shell/script must
report an inner candidate that could not start as `NOT_EXECUTED` in schema
results; its exit status cannot prove that distinction to the runner.

Both stdout and stderr must be valid UTF-8. The adapter must print JSON to
stdout with integer `schema_version: 1` (not `true` or `1.0`), a `candidate` object containing
nonempty `name` and `revision`, and a `results` array. Each result must include
the mapped `test_id`, a named `property`, a state (`PASS`, `FAIL`, `ERROR`, or
`NOT_EXECUTED`) and nonempty `evidence`. Omitted rows become
`NOT_EXECUTED`; malformed text/JSON, duplicate object keys at any depth,
duplicate test IDs, crashes and timeouts become `ERROR` for every mapped
external clause. JSON text containing unpaired surrogate escapes is also
rejected, as are nonfinite numbers (`NaN`, `Infinity`, `-Infinity` or overflow)
and JSON nested beyond the parser's supported depth. Conflicting keys are never
accepted by silently overwriting evidence.
JSON and HTML retain the specific error, including the stream with invalid
UTF-8. `--timeout` bounds each test command and adapter execution. On adapter
timeout the runner kills the original POSIX process group, including members
holding output pipes open. It allows up to one additional second to drain
output, closes remaining pipes and reaps the direct adapter before returning.
Children that deliberately start a detached session are outside that group;
they may survive and retain pipes, which the bounded drain reports as ERROR.
Host init owns reaping orphaned children. Output-size quotas remain a
pre-existing limitation: captured stdout/stderr have no byte limit.

No independent wallet or verifier is configured in this checkout. The report
marks each externally mapped clause `NOT_EXECUTED` and lists the setup actions
needed to collect independent evidence. This is an operator handoff, not a
claim that the external candidate passed.

Fast CI runs the harness regressions with `make check`; it does not require the
full audit to be green. Synthetic subprocess controls exercise invalid boundary
data, startup/exit failures, descendant cleanup and a complete passing audit
through JSON and HTML. That passing control mocks local Go evidence and host
diagnostics; it is not independent candidate or reproduction evidence. Even a
successful audit retains external reproduction `UNPERFORMED` and its operator
actions. See [`../../tests/README.md`](../../tests/README.md)
for commands and the adapter contract.
