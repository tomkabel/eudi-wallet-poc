# Agent-local acceptance execution, 13 November 2026

The [report](report.json), [transcript](transcript.txt) and retained JVM XML files
record the complete runner against an explicitly dirty implementation workspace
whose Git HEAD was f1c7e1263844109edb39184bc1d5b7bd213b5e48. That revision is the
base, not a claim that these implementation files were committed at that HEAD.
The report records dirty=true, changed paths, exact commands and content hashes
of inputs at completion. Later review/documentation-only changes may differ;
regenerate from the final committed tree for a clean release evidence claim.

Executed command (JDK17, locked Python environment):

    JAVA_HOME=/usr/lib/jvm/java-17-openjdk .venv/bin/python3 tests/run_estonian_acceptance.py --json /tmp/story35-acceptance-final.json

Observed result: INCOMPLETE, process exit 1, status awaiting-operator,
agent_controls_passed=true, no diagnostics. The five local controls PASS:
UNSUPPORTED_PROTOCOL for unsigned OpenID DCAPI; register-not-found on both real
HTTP routes with linked zero-allocation assertions; matching actual wallet and
verifier signed fallback observations; and PRESENTATION_UNAUTHENTICATED_REQUEST
for ISO/proximity plain-only inputs. A genuinely executed local EE AV proof is
supplementary only, using direct_post.jwt; it is not foreign acceptance.

The final fresh Gradle execution passed 8 dispatch and 3 fallback tests, no
skips/failures/errors, including same-ID wrong hash/block parameter rejection.
Both fallback consumers resolved version 1 of the same pinned signed document,
SHA-256 b34a8261cfd1aac39009a13143c9075efb3ef90db121ff5fee8f04e7c8dbc665,
and the same actual native SpecID. The generated resource copies are build
outputs; the XML checks them byte-for-byte against the public source artifacts.

Foreign PID and foreign AV remain NOT_EXECUTED. No independent wallet session,
foreign credential/device/access agreement or legal authorization was supplied.
The report's two imperative operator actions remain required. Follow the
[acceptance guide](../../ACCEPTANCE.md) to supply genuine fresh external evidence.
The report is local engineering evidence, not certification or a legal finding.

No holder credential or signing private key is retained here. All local holder
and ephemeral response keys were generated in system temporary directories and
removed by the harness. The temporary circuit release private key stayed outside
the checkout; only its public pin and signed public document are tracked.

## Kotlin lint follow-up

After parent verification found introduced Kotlin style/complexity issues,
structural helper/context refactors repaired them without baseline changes or
new suppressions. The historical report and transcripts above remain unchanged.
The [follow-up summary](followup-kotlin-lint/summary.json),
[lint transcript](followup-kotlin-lint/lint-transcript.txt), and
[full unit-test transcript](followup-kotlin-lint/unit-test-transcript.txt)
record make lint-kotlin passing and the complete Android local_mocks unit suite
passing: **34 suites, 236 tests, zero skips/failures/errors**. Both new suites'
fresh XML files are retained separately in that directory. This follow-up
preserves the same actual-time runtime expiry checks and does not rerun or
supersede the earlier complete native-proof acceptance report.
