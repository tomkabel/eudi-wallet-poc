# AV report reproduction

This independent proof of concept publishes narrow executed checks, including
failures. It does not establish endorsement, certification, complete AV or ISO
conformance, or production trust. The offline [HTML report](av/report.html) and
[sibling JSON](av/report.json) describe the same audit. Their profile version is
the full source revision in [source.json](av/source.json),
`014dc2fd0f9332eeabe1eef70d234ed2aef20fd7`, with the retained source and excerpt
hashes. The runner validates the registry through the shared offline validator
before executing probes and records the actual registry SHA-256 and path in
both artifacts. No separate semantic profile version is invented.

The historical report pair above remains tied to its clean implementation
revision. The separately retained [23 October follow-through package](av/follow-through-2026-10-23/README.md)
contains the hardened runner's current **provisional dirty-workspace audit**,
input/artifact hashes and transcript. It has not been published externally and
does not replace the historical clean evidence or satisfy independent acceptance.

The Digital Credentials API is the mandated AV ZK presentation path. OpenID4VP
`mso_mdoc_zk` is a profiled, unofficial carrier. The Go audit checks selected
containers, request properties and primitives for one boolean age predicate on
one attestation. Scope and limits appear on each mapped clause. Android/JVM
AVSUA issuance evidence from [Story 1.5](AVSUA-ISSUANCE.md) remains separate;
this report preserves the registry's corresponding exclusions.

## Start from the recorded implementation commit

Use a fresh parent directory so `make deps` can create the required sibling
`longfellow-zk` without altering another checkout. Read `repository.revision`
from the published JSON and substitute that **full implementation commit** in
the command below. The later commit containing the generated artifacts is not
the evidence revision. A report generated before implementation is committed
will explicitly say `repository.dirty: true`; it is provisional evidence and
must be regenerated from the clean committed implementation before publication.

Prerequisites: Git, GNU Make, Python 3.12 (the CI version), Go at the version
specified by [go.mod](../../verifier/go/go.mod), a Rust/Cargo toolchain, a C/C++
compiler and Linux on a CPU advertising the x86-64-v3 feature baseline. Network
access fetches the public repository, pinned Longfellow source and dependencies.
The Rust toolchain is recorded, not pinned by this project. JDK 17 is the Android
baseline; the Go runner does not require Java, but records the installed JDK
version or `UNAVAILABLE` honestly. See [development setup](../DEVELOPMENT.md).

```bash
mkdir av-reproduction
cd av-reproduction
git clone https://github.com/tomkabel/eudi-wallet-poc.git eudi-wallet-poc
cd eudi-wallet-poc
git checkout --detach <full-implementation-commit-from-report.json>
git rev-parse HEAD
git status --porcelain --untracked-files=all
```

The status output must be empty before setup. Do not copy `demo-data/` or private
keys from an existing checkout. No private repository is needed. The report
records Git changes at audit start; it never adds `+dirty` to a canonical commit
hash. An empty status is evidence of checkout cleanliness, not an attestation
of historical build flags or the static library's compiled source revision.

## Build and run in one sitting

Start timing **before the clone above** for total elapsed time. Record UTC start
and end, any interruptions and elapsed wall time separately for setup/build and
the audit. Keep a terminal transcript outside the checkout; for example, start
`script -q /tmp/av-reproduction-session.txt` before cloning, then `exit` when done.
Record the actual commands and their return codes. A local agent run is local
verification; only an independent human or organization supplies third-party
evidence. Do not claim a fixed duration before measuring the host and network.

From the clean checkout, use these commands. Keep the environment and output
artifacts outside the tree to preserve a clean evidence revision:

```bash
python3 -m venv /tmp/av-reproduction-venv
. /tmp/av-reproduction-venv/bin/activate
python3 -m pip install -r tests/requirements-ci.txt
export RUSTFLAGS='-C target-cpu=x86-64-v3'
make deps
test -x ../longfellow-zk/rust/target/release/examples/ee_poa_demo
test -f verifier/zkverify-ffi/target/release/libzkverify.a
git -C ../longfellow-zk rev-parse HEAD
git status --porcelain --untracked-files=all
python3 tests/run_av_conformance.py --json /tmp/av-report.json --html /tmp/av-report.html
printf 'audit exit: %s\n' "$?"
```

Use an unused venv and output path for each attempt. The required Longfellow
revision comes from [longfellow-rev.txt](../../verifier/zkverify-ffi/longfellow-rev.txt).
`make deps` fetches it at `../longfellow-zk`, installs the prover examples and
builds both Rust and Go components. The directory name matters to the Cargo
path dependency. The explicit exported `RUSTFLAGS` applies the CPU baseline to
both builds and is also visible in the audit environment. The FFI Cargo config
pins the same baseline; `Makefile` applies it to the prover. An observed unset
`RUSTFLAGS` does not invalidate configured build flags, and a currently set
variable does not prove what compiled an existing static library. The report
separates those facts and fingerprints the library. Its Rust flags
`required_baseline` names the required policy; the FFI configuration text is
observed separately and is not inferred from that constant.

The runner exits **1**, because known AV property failures and missing external
evidence are retained. This expected audit result does not mean the build
failed. Current local evidence has 20 mapped clauses: 17 PASS, 3 FAIL, 10 passing
negative controls, plus 74 EXCLUDED clauses. The unconfigured external candidate
has 20 NOT_EXECUTED and the same 74 exclusions. Supplementary probes are
reported separately from these clause counts. Actual regenerated output is the
authority: investigate any difference. Coverage thresholds count executed
PASS/FAIL clauses and passing negative controls, never exclusions or missing
results. All selected probes also retain their named terminal state and assertion
output, including supplementary probes without a clause mapping. A supplementary
FAIL, ERROR or NOT_EXECUTED makes the audit nonzero even if mapped clauses and
negative-control thresholds succeed. Negative controls retain named passing,
failing and skipped states; only unambiguous passes from completed package
executions count toward the threshold. Timeout/build/startup failures cannot
donate passes. The CLI retains `--json`; omitting `--html` writes the matching `.html`
path. `--skip-local` records NOT_EXECUTED and exits nonzero; it is an adapter
control, not reproduction evidence.

Open `/tmp/av-report.html` directly in a browser. It needs no server or network
resources. Native expandable evidence supports keyboard navigation. Inspect
its state counts, full ordered clauses, failure assertions, exact Go commands
and package exit codes, all named probe results, named negative controls and host record
alongside `/tmp/av-report.json`. The pinned source revision, candidate commit,
cleanliness and counts must agree. A timestamp, hostname, CPU, toolchain, path
or file modification time difference is provenance variation. A different
state, assertion, clause order, exclusion reason or profile hash is a semantic
or source difference and must be explained, not normalized away. Record a
JSON/HTML hash in the handoff if transferring the artifacts.

The host record also captures effective `go env -json` settings from the Go
module directory: `GOFLAGS` (including overlay flags), `GOWORK`, `GOENV`,
`CGO_ENABLED`, `CC`/`CXX`, CGO compiler/linker flags, `GOOS`/`GOARCH`/`GOAMD64`
and `GOTOOLCHAIN`, alongside the observed environment. Compare these settings
before treating two clean source checkouts as equivalent executions. Current
Go settings do not attest historical compilation. The Python report suite runs
in fast CI with synthetic evidence and again in e2e after the staticlib build,
where its actual local-audit regression must execute.

## Recognize environment problems

An unavailable repository revision or cleanliness record adds the named
`repository_provenance` ERROR while retaining inspectable artifacts. Missing,
unreadable or invalid required host/source inputs stop before probes with the
artifact/source validation diagnostic and exit 2.

The JSON `host.diagnostics` and candidate `diagnostics` record execution problems
separately from property verdicts. A missing/wrong Longfellow sibling is named
with its observed and pinned revisions; run `make deps` in the fresh directory.
The report records sibling dirtiness too: installed prover example files can
make the sibling dirty without changing HEAD. That observation does not prove
the linked library's source contents. A missing Rust toolchain/JDK is reported
as unavailable; only the tools needed for an execution can stop that execution.

A missing static library normally produces a linker error for packages `.` and
`./zk`, while packages without cgo can still execute. The diagnostic stays with
its actual package. A build failure, unavailable Go binary, timeout, incompatible
CPU, or `SIGILL` startup error produces ERROR evidence, not a failed conformance
property. Rebuild with `RUSTFLAGS='-C target-cpu=x86-64-v3'`; do not use
`target-cpu=native` to hide a mismatch. Advertised CPU flags are only a host
observation, not a runtime instruction-set or operating-system attestation.

If setup/build fails, preserve its transcript and return code even if the runner
can still emit a diagnostic report. Do not describe that attempt as a successful
clean reproduction. Never convert missing or ambiguous results to PASS, borrow
another package's evidence, remove known failures, or relabel a missing external
candidate as executed. Artifact/source validation or output-write failures use
exit 2, distinct from the expected nonzero audit result.

## Independent evidence and publication handoff

External reproduction is **UNPERFORMED** until an independent operator supplies
the evidence. A maintainer or agent using a second checkout only establishes
local verification. The JSON records `external_reproduction.status` as
`awaiting-operator` and carries these outstanding actions:

1. Execute an independent candidate at a recorded revision using the JSON adapter
   contract in [tests/README.md](../../tests/README.md), then rerun with
   `--candidate-command '<adapter command>'`. Preserve its named properties and
   all result states. An adapter is an evidence transport, not proof of independence.
   The [runner contract](AV-CONFORMANCE-RUNNER.md) now executes quoted argv directly:
   shell operators, expansion and globbing are not interpreted. Put compound setup
   in an executable adapter script, or explicitly invoke a shell and have the
   adapter report an inner candidate's startup failure as `NOT_EXECUTED`.
2. Have an independent human or organization follow the clean-checkout commands
   in one sitting and retain start/end times, elapsed time, host record, exact
   source revision, command transcript, return codes and both generated artifacts.
3. Compare semantic results and provenance, document discrepancies and failed
   setup attempts, and supply attribution/independence details and artifact
   locations to the maintainer. Publish externally only as an operator action.

No third party has been contacted by this implementation, no independent result
is inferred, and the independent-reproduction acceptance criterion remains open.

## Recorded local verification

The committed [timing and command record](av/reproduction-record.json) and
[terminal transcript](av/reproduction-transcript.txt) retain the agent-local run
on 2026-10-23 at implementation commit
`3cce11df6ec4e9148ebd4859cde95aabf7e650fc`. The checkout was clean before and
after the audit. Total elapsed time was 317.320 seconds (5 minutes 17 seconds),
including `make deps` at 302.576 seconds and the audit at 6.466 seconds. Both
prover and verifier were built in fresh target directories; the Longfellow
sibling was fetched from its public source. Existing global dependency caches
were available. Python 3.12.13 ran the pinned dependencies; the report records
the actual CPU, Rust, Go and JDK observations. The audit exited 1 with the
expected 17 local PASS, 3 FAIL, 10 passing negative controls, external 20
NOT_EXECUTED and zero environment errors. Clause states/mappings/limits, named
negative outcomes and registry bytes agree with the reviewed workspace audit.

Because this implementation commit was unpushed, the clone used the local Git
repository instead of the documented public GitHub transport. The transcript
records that substitution. Public availability and independent third-party
reproduction remain outstanding operator work; this timing is one host's local
measurement and does not establish either independence or a universal duration.
Artifact hashes in the record identify the retained JSON, HTML and transcript.

## Retrospective follow-through

The [new workspace audit](av/follow-through-2026-10-23/README.md) exercises the
reviewed adapter-hardening checkout at base commit
`6cf84df5806ecd22792eade3b8ced8d14f148202`, explicitly dirty. Its 159 input hashes,
exact command, UTC timing, return code, host metadata and matching JSON/HTML are
retained separately. It returned 1 in 1.981 seconds with the same pinned
profile/registry, ordered clause outcomes, summary and named negative-control
states as the historical audit: local 17 PASS / 3 FAIL, 10 negative controls,
external 20 NOT_EXECUTED, 74 exclusions per candidate and no environment errors.
The historical four-file report/reproduction package is unchanged.

Story 1.3's supplied verification separately records 38 runner tests and
`make check` passing, plus the parent-run 212 Android tests, 6 JVM conformance
tests and Go-ZK race suite. Those checks prove their local scopes; none is an
independent reproduction. The uncommitted hardening still needs review, a clean
committed regeneration and publication through `fork`, followed by independent
candidate execution and independently attributed clean-checkout reproduction.
Story 1.6 remains `awaiting-operator`; no public availability, external
participation or acceptance is inferred from this workspace package.
