# AV conformance runner

`python3 tests/run_av_conformance.py --json av-conformance.json` runs the Go
tests referenced by the pinned clause registry plus the dedicated local AV
probes. Each report row records a candidate, clause, test, property, scope,
limits, state and evidence. Unique clause coverage and negative controls are
counted separately; negative controls count only passing executed Go
subtests. The registry retains its complete 94-clause denominator;
20 clauses currently have executable mappings.

The audit exits nonzero when an in-scope property fails, errors or is not
executed, when external candidate evidence is unavailable, or when coverage
falls below 20 clauses/eight negative controls. Existing fallback-policy
differences are reported as failures; production policy is not adjusted to
make this report pass. A partial primitive probe is not complete AV or ISO
conformance evidence.

An external adapter can be supplied with `--candidate-command '<command>'`.
It must print JSON with `schema_version: 1`, a `candidate` object containing
nonempty `name` and `revision`, and a `results` array. Each result must include
the mapped `test_id`, a named `property`, a state (`PASS`, `FAIL`, `ERROR`, or
`NOT_EXECUTED`) and nonempty `evidence`. Omitted rows become
`NOT_EXECUTED`; malformed output, duplicate IDs, crashes and timeouts become
`ERROR`. `--timeout` bounds each test command and adapter execution.

No independent wallet or verifier is configured in this checkout. The report
marks each externally mapped clause `NOT_EXECUTED` and lists the setup actions
needed to collect independent evidence. This is an operator handoff, not a
claim that the external candidate passed.

Fast CI runs the harness regressions with `make check`; it does not require the
full audit to be green. See [`../../tests/README.md`](../../tests/README.md)
for commands and the adapter contract.
