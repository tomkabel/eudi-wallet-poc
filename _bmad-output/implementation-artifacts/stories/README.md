---
planning_status: drafted
date: 2026-10-23
scope: story analysis and writing only
---

# Story preparation contract

These files expand the existing backlog for later implementation. They do not
authorize implementation, publication, external messages, deployment, or changes
to delivery status. The [analysis and complete index](../../planning-artifacts/story-analysis-2026-10-23.md)
covers all 80 stories. The six existing Epic 1 specs remain their detailed records;
the other 74 stories have individual files here.

## How to read a story

`status` is copied from the sprint tracker at drafting time. `planning_status`
describes the draft, not completed delivery. `depends_on` lists prerequisites to
full acceptance; work explicitly described as preparatory can start earlier.
`decision_gate` means the contract needs a recorded decision before the affected
implementation. `external_gate` names evidence that local tests cannot supply.
Neither a draft nor a proposed output is proof that the output exists.

The “Backlog contract” section preserves the authored story and acceptance
criteria, including historical premises and disputed statements. Read its
“Analysis and implementation boundaries” alongside it: conflicts are recorded,
not silently resolved by copying old prose. Source item codes refer to the three
research documents listed in the epic document; use those documents' own source
registers to reach primary sources. Backlog `FRn`/`NFRn` and PRD `FR-n`/`NFR-n`
are separate namespaces. Join records on story id.

## Shared constraints

- Read the matching story in [epics.md](../../planning-artifacts/epics.md), the
  [architecture record](../../planning-artifacts/architecture.md), relevant ADRs,
  and the PRD before implementation. The UX experience spine is **draft** and
  contains stale backlog counts; use its surface observations as design input,
  not as an approved override of the PRD or the specification.
- Preserve `Carrier` dispatch and `TranscriptForFlow` as the adaptation points.
  Retain `direct_post.jwt` as the default. Label the AV Profile's DC API path and
  this fork's profiled, unofficial OpenID4VP carrier separately (ADR-008).
- A mock, local fixture, generated report, or successful harness test proves
  only its stated scope. Independent execution, real services, hardware,
  legal review and external assessments require their own evidence. Record
  unavailable evidence with reason/date; never manufacture a passing result.
- Verify external versions, legal propositions and current institutional claims
  against primary sources at execution time. This drafting pass does not refresh
  those external sources. Inherited unverified propositions remain
  **[UNVERIFIED]**, especially the beyond-18 threshold rationale, standards
  schedules, procurement dates, certification claims and current upstream activity.
  Collect unresolved specification claims in spec §23 when those claims are
  actually added or changed; this pass makes no normative specification changes.
- Never import private ee-eudiw material without maintainer confirmation. Never
  commit `demo-data/`, real credentials, device keys, provider secrets, or private
  signing keys. Proposed fixture keys must be clearly synthetic test material.
- For wallet surfaces, cover ET/EN/RU and report missing translations explicitly;
  sandbox surfaces use ET/EN. Keep refusal, cancellation and technical failure
  distinct in UI, logs and machine results. No policy refusal offers an accidental
  “continue anyway.” Apply the accessibility baseline to every changed surface.
- New documents must be indexed in `docs/README.md`; new tests must be catalogued
  in `tests/README.md`, with what they prove. This directory is indexed as a
  collection through the complete story index rather than 74 duplicate rows.

## Verification plan for later implementation

The unchecked tasks in these files are proposed work. No application test,
benchmark, legal review, device execution or external run was performed by this
planning pass. Planning checks validate inventory, links, dependencies and the
documentation diff only.

For a code story, the backlog's shared definition of done still applies:
`make check`, `./gradlew :app:testLocal_mocksUnitTest`,
`./gradlew :zk-conformance:test`, and `go vet ./...` from `verifier/go`.
Run `make deps` before packages that link Rust; use the sibling
`../longfellow-zk` and the documented CPU baseline. Add `make test-go-zk` and
`make e2e` where proof, carrier or verifier behavior changes. Regenerate golden
fixtures only deliberately; cross-verify regenerated JVM fixtures in Go.
Install pinned Python requirements from `tests/requirements-ci.txt`.

The backlog's exact “169 tests” count is historical, not a ceiling and not a
current passing result. The Epic 1 retrospective records a later 212-test run;
neither is a fresh run here. Preserve the upstream suite, record actual execution
counts and failures, and explain any unavailable shared check in the story.
Documentation-only work needs source/claim review and link/diff checks, not a
fabricated runtime test result.

## Completion evidence required later

Each delivered story must retain its revision, changed paths, commands and return
codes, environment, acceptance-criterion mapping, failure output, and remaining
limitations. A report must retain failing and unexecuted cases. Operator-dependent
stories remain incomplete until their own acceptance criteria are met; local
preparation may be recorded separately. Sprint state changes belong to the
implementation or planning lifecycle that owns them, not to this drafting pass.
