
- source_spec: `_bmad-output/implementation-artifacts/spec-1-3-av-conformance-runner-hardening.md`
  summary: Define adapter stdout/stderr and parsing resource budgets for the AV conformance runner.
  evidence: Independent review observed that communicate buffered unlimited adapter output before this follow-through; streams and payload parsing still have no quota, so a faulty adapter may exhaust host resources. A quota/streaming protocol and retained ERROR artifact behavior need separate design and tests.
