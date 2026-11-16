# Clean implementation acceptance execution, 16 November 2026

The [report](report.json) and [transcript](transcript.txt) record a fresh complete
runner execution from clean implementation commit
`a47d364f84f0094e7ab302ff01d786f921af1d7f`. The report records dirty=false,
no changed paths, identical implementation manifests before/after execution,
and no diagnostics. This evidence and the terminal story record are committed
separately after the execution; they do not change the measured implementation.

Executed with JDK 17 and locked Python dependencies:

    JAVA_HOME=/usr/lib/jvm/java-17-openjdk .venv/bin/python3 tests/run_estonian_acceptance.py --json /tmp/story35-clean-acceptance.json

Observed: INCOMPLETE, expected exit 1, awaiting-operator,
agent_controls_passed=true. Five local controls PASS; genuine foreign PID and
foreign AV remain NOT_EXECUTED. The supplementary encrypted local EE AV proof
verified successfully and is not foreign acceptance. Both actual fallback
consumers resolved the same signed document, version and native SpecID without
fetching a registry. Actual ViewModel tests observed named refusals and zero
responses before consent/release.

Fresh [ViewModel XML](TEST-ee.cyber.wallet.domain.presentation.EstonianViewModelAcceptanceTest.xml)
and [fallback XML](TEST-ee.cyber.wallet.domain.presentation.SignedCircuitFallbackTest.xml)
contain 10 successful tests total, zero failures/errors/skips. The runner forced
all 40 Gradle tasks; BUILD SUCCESSFUL in 7m 6s. The separately executed full
Android suite passed 35 suites and 243 tests, zero failures/errors/skips:
[summary](full-unit-summary.json), [transcript](full-unit-tests.txt).

The clean implementation commit also passed the complete [make check](make-check.txt).
[Linked verification](linked-checks.txt) records make test-go-zk and make test-rp-kit
passing against the same implementation behavior before formatting-only finalization.
Historical [13 November evidence](../acceptance-2026-11-13/README.md) is preserved.

No genuine foreign wallet session, certification or legal authorization is claimed.
Execute fresh genuine foreign PID and AV wallet sessions with authorized issuer
trust/access and supply external adapter evidence using the
[acceptance guide](../../ACCEPTANCE.md). Holder and response keys were generated
outside the checkout and removed by the harness; no private key is retained here.
