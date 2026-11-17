# AV audit follow-through — 23 October 2026

This is a **provisional, unpublished, agent-local audit of an uncommitted
workspace** after Story 1.3's adapter hardening. It is not a clean-checkout
reproduction or independent candidate result. Stories 1.3 and 1.6 remain
`awaiting-operator`.

The actual runner generated the [offline HTML](provisional-agent-local-report.html) and [JSON](provisional-agent-local-report.json)
together. Both identify base revision
`6cf84df5806ecd22792eade3b8ced8d14f148202` with `repository.dirty: true`.
The [audit record](audit-record.json) retains SHA-256 hashes for 159 source,
configuration, profile and fixture inputs, verified unchanged during execution;
it also hashes both reports and the [command transcript](audit-transcript.txt).
The [implementation patch](implementation.patch), against that exact base
commit, preserves the changed runner/test bytes; apply it and verify the hashes
to recover those dirty implementation inputs. The [capture and verification
procedure](capture-audit.py) and [verification transcript](verification-transcript.txt)
retain the exact checks, including the embedded Go HTML asset. The report
filenames themselves identify their provisional agent-local status. The report separately fingerprints the existing Rust staticlib
and records effective tools and environment. This was not a fresh or hermetic
build and does not attest historical compiler flags.

The audit ran for **1.981 seconds** and returned **1**, retaining 17 local PASS,
3 local FAIL, 10 passing negative controls and 20 external NOT_EXECUTED results.
Each candidate retains all 94 clauses, including 74 explicit exclusions. There
were no environment errors. The three local failures remain AV-A.6-016
(`direct_post`), AV-A.6-017 (request transport) and AV-A.6-018 (client
identification); this work preserves the encrypted production default.

The pinned profile/registry, summary, ordered clause states and mappings, and
named negative-control states agree with the [historical clean audit](../report.json).
Observed mapped assertion differences are AV-A.6-010
(`TestAVConformanceEnvelope`) subtest order, AV-A.8-003
(`TestAVProfileExternalContainers`) duration 0.11s to 0.07s, and AV-A.8-011
(`TestRegistryRejectsUnlisted`) duration 0.01s to 0.00s. The exact
historical/current details are retained in the audit record.
Host, timing, checkout revision/dirtiness and invocation are recorded separately. All four historical report/reproduction files remain
byte-for-byte unchanged, with their hashes retained in the new record.

For actual acceptance, first review and commit the hardened code, regenerate
from that clean full revision and publish through `fork`. An independent wallet
or verifier must supply revision-attributed adapter results; an independent
human or organization must then follow the [clean-checkout guide](../../AV-REPORT-REPRODUCTION.md)
and return attribution, host, exact commands, return codes, timing, transcript
and both artifacts. Review and publish discrepancies before closing either
story. Local regression success, this report and an adapter's name do not
establish independence. No external publication or contact occurred here.
