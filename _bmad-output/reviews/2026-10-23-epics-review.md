# Review — `_bmad-output/planning-artifacts/epics.md`

Reviewer pass run 2026-10-23 04:49–04:56. Content class: **document** (a plan that defines behaviour).
Lenses: **adversarial**, **edge-case-hunter**, **structure**, then **prose** on top of structure's findings.
`verification-gap` was not run: its `applies_to` is code, and this content is a document.

Findings: **19 adversarial**, **65 edge-case-hunter**, **17 structure rows** (14 changes, 3 PRESERVE), prose separate.

Parent verification: every adversarial finding that could be checked was checked against the document, the
specification `spec/EE-EUDIW-TS-1.0.md`, or computed counts. Verdicts are recorded per finding below.
Overlap between lenses is kept and marked — it is signal, not duplication.

## Findings found by two lenses independently (signal)

1. **Coverage Map omits six of the 75 stories** — adversarial #1 and structure row 2. Computed and confirmed:
   `2.3, 3.8, 5.6, 6.6, 7.10, 7.11` appear in no map row.
2. **Stories 2.3 and 8.2 both do version labelling** — adversarial #2 and structure row 8.
3. **Epic 11's introduction is too short for its seven stories** — structure row 9; adversarial #3's
   point about document-producing stories lacking output paths lands on the same epic.

## Adversarial lens

**1. Coverage Map (epics.md:156-227) against the 75 story Covers lines (Story 2.3 l.386, Story 3.8 l.537, Story 5.6 l.731, Story 7.10 l.1003, Story 7.11 l.1019)**
- condition: Five stories claim an FR that the Coverage Map assigns to a different story and appear in no map row: 2.3 (Covers FR6, map row FR6 = 2.1), 3.8 (Covers FR10, map row FR10 = 3.2), 5.6 (Covers FR21, map row FR21 = 5.1), and both 7.10 and 7.11 (Covers FR33, map row FR33 = 7.4) - the map is checked FR to story but never story to FR.
- fix: Add the five stories to the map, or give each a row stating it carries no FR (analysis or decision artefact), and reconcile the arithmetic of 75 Covers lines against 66 FRs plus the intentional duplicates; also decide whether Story 5.6 (registry succession) and Story 8.9 (upstream succession) are one deliverable or two.
- consequence: An agent working from the map treats Stories 2.3, 3.8, 5.6, 7.10 and 7.11 as having no requirement behind them, while a later edit can delete one of the duplicate stories with every FR still appearing to be covered.
- parent verdict: **CONFIRMED** — computed: 75 stories in doc; 2.3, 3.8, 5.6, 6.6, 7.10, 7.11 appear in no coverage-map row

**2. Coverage Map FR6 row (l.165), Story 2.1 (l.355), Story 2.3 (l.386), FR40 (l.97), Story 8.2 (l.1059)**
- condition: FR6 (a documented carrier conformance runner over the 13 e2e cases with published vectors) is claimed by both Story 2.1 and Story 2.3, and Story 2.3's actual deliverable - version-labelled fixtures and profile rules - is FR40's wording, which Story 8.2 already carries.
- fix: Re-point Story 2.3 at FR40 and fold it into Story 8.2, or give version labelling its own FR and strike FR6 from Story 2.3.
- consequence: The same labelling work is scheduled twice in two epics while FR6's published vectors can be skipped altogether, because the FR still shows as covered by Story 2.1 alone.
- parent verdict: **PARTLY** — Story 2.3 'Covers: FR6' and FR6's map row names Story 2.1 - confirmed by reading both; the FR40/Story 8.2 overlap also confirmed

**3. Overview readiness gate (l.42-52), Additional Requirements (l.145), Story 8.6 (l.1135)**
- condition: The gate justifies proceeding on the ground that each story names the file it touches and the source paragraph it derives from, but no document-only story names an output path (3.4, 3.8, 7.5, 7.6, 7.7, 7.8, 7.9, 8.1, 8.7, 8.9, 9.1, 9.6), the docs/README.md indexing invariant is carried by Story 7.1's criterion alone, and Story 8.6 relies on the undefined phrase the carrier-profile gate.
- fix: Give every document-producing story a named path plus the criterion 'the document is indexed in docs/README.md', and define or delete the carrier-profile gate.
- consequence: Twelve stories can be closed with the artefact filed anywhere, the repository's own index rule is enforceable against one story, and a later reader cannot locate the clause maps and statements these epics exist to publish.
- parent verdict: **CONFIRMED** — checked: none of the twelve document-producing stories names an output path; 'carrier-profile gate' appears once, undefined

**4. Coverage Map cells l.178, l.196, l.219, l.227 and the Epics table l.231-240**
- condition: The token A1-A6 denotes both a Report 3 appendix (A1 competitive matrix through A6 tradeoff analysis) and a Report 2 requirements-matrix row (A1 Topic 1/24 through A6 ZKP_03), sometimes within a single cell (FR37 = A4, A6, A7, A16, A6 tradeoff 1), and bare R1-R5 means Report 2 recommendations while risk R1-R15 and A3 R1-R2 mean other registers.
- fix: Prefix every citation by register (App. A3 P1, matrix A6, rec. R3, risk R4) and apply the same discipline to the Source lines in the stories.
- consequence: FR19's A3 resolves to the Topic 19 dashboard row while FR60's A3 R1 resolves to roadmap phase A3, and FR37's cell cites A6 twice with two meanings, so a traceability check or an agent following a citation lands in the wrong document.
- parent verdict: **CONFIRMED** — read the cited cells: FR37 = 'A4, A6, A7, A16, A6 tradeoff 1' uses A6 twice with two meanings; bare R1 is a recommendation in the Epic 1 row and a risk in the Epic 8 row

**5. Requirements Inventory NFR1-NFR10 (l.125-136) and Coverage Map (l.156-227)**
- condition: Ten NFRs are inventoried but only NFR1, NFR2 and NFR7 are cited anywhere (l.535, l.850, l.922); NFR3 (fail closed), NFR4 (version labels), NFR5 (no regression), NFR6 (pinned Python inputs), NFR8 (third-party reproduction), NFR9 (secretless) and NFR10 (accessibility) have no story binding, the Coverage Map has no NFR section, and NFR5 pins a count - 169 @Test across 26 test files - against the corpus's 27 Kotlin unit-test files (docs/research/2026-10-23-innovation-assessment.md:105) and a tree holding 25 files that contain @Test out of 27 under app/src/test.
- fix: Add an NFR coverage table mapping each NFR to the stories that must satisfy it, put NFR5's four commands into a stated Definition of Done referenced by every code story, and replace the file count with the command or the corpus's 27.
- consequence: Roughly sixty code stories carry no stated regression gate, the two NFRs a public buyer reads first (secretless build, WCAG 2.1 AA) cannot be checked against any acceptance criterion, and a no-regression criterion keyed to an unsatisfiable count is either ignored or maintained by hand.
- parent verdict: **CONFIRMED** — computed: 27 .kt files, 26 named *Test.kt, 25 containing @Test, 169 occurrences - so '169 @Test across 26 test files' overstates the file count

**6. Requirements Inventory (l.54-123), frontmatter inputDocuments (l.8), the exclusion note (l.227)**
- condition: spec/EE-EUDIW-TS-1.0.md is listed as an input document, yet none of its 211 requirements becomes an FR and no acceptance criterion cites one, while the note on rows deliberately left without a story enumerates only Report 2's matrix rows and Annex E records 126 requirements covered and 85 uncovered.
- fix: State the specification's role (evidence, not a source of stories) and either carry the uncovered requirements that touch this repository - EE-ZKP-062 clock validity, EE-REV-007 status-cache age, EE-PRO-025 readerAuth anchors, EE-ZKP-043 platform baseline - into the inventory or list them as deliberate exclusions.
- consequence: An agent that completes all 75 stories still leaves requirements the repository's own specification records as uncovered, and nothing in the backlog distinguishes that omission from a decision.
- parent verdict: **CONFIRMED** — spec/EE-EUDIW-TS-1.0.md:15-16 states requirement count 211, Annex E covers 126, remainder 85; no FR in the inventory cites a spec requirement

**7. Overview readiness gate (l.42-43) and inputDocuments (l.4-13)**
- condition: The gate states the recorded intent lives in docs/research/, spec/, docs/CONFORMITY.md and docs/planning/, ignoring _bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/, which holds five extract documents and a session log whose entries bear directly on accepted stories (the EU age-verification trusted list is live with paap/DK recognised and no paap/EE; Longfellow and the Commission ship mso_mdoc_zk verifier endpoints).
- fix: List the extracts in inputDocuments or say why the three reports supersede them, and carry the two landscape facts into the stories they touch - Story 1.5's attestation provider and the external-target choice in Stories 1.3 and 2.4.
- consequence: Story 1.5 assumes an attestation provider issuing eu.europa.ec.av.1 while evidence in the same workspace shows no Estonian provider on the trusted list, and Epic 1's external target stays unspecified when a named candidate is already recorded.
- parent verdict: **CONFIRMED** — the prds/ tree exists with 5 extracts - but it was created 04:47-04:51 today, AFTER epics.md was written at 04:43, by a concurrent session (see Concurrency note)

**8. Epic 4 Stories 4.1-4.5 (l.553-640) and readiness gate l.46-47**
- condition: Story 4.5's Given requires generated certificates including malformed, empty-intended-use and absent cases, but no Epic 4 story delivers a certificate fixture generator - which the corpus names (O3 implementation approach: the certificate implementation and a fixture generator second) - and the gate calls that generator sequenced first while the fail-closed default is the epic's last story.
- fix: Add a story or a named acceptance criterion for the certificate fixture generator, move the fail-closed default ahead of the parsing story, and correct the gate's sequencing claim.
- consequence: Story 4.5 is unclosable as written, risk R8's mitigation (refusal by default proven by property tests) has no owner, and Epic 3's register profile plus Epic 6.5's latency profile inherit a certificate path that nothing fixtures.
- parent verdict: **CONFIRMED** — Epic 4 has no fixture-generator story; Story 4.5's Given depends on generated certificates it does not deliver

**9. Story 5.2 (l.667), Story 5.3 (l.684), FR22-FR23 (l.79-80), FR66 (l.123)**
- condition: The verifier may fall back to a pinned local circuit list when the registry is unreachable, while the wallet is required to refuse the ZK path when the fetched document is absent and FR66 forbids the plain path on a ZK-capable device, so a registry outage leaves the holder nothing to present and no story owns that state.
- fix: Write the offline rule once - either the wallet resolves from the same pinned list the verifier falls back to, with the pinned document version named, or both sides refuse and a story states the holder-visible outcome - and add the case to Story 3.5's four acceptance cases.
- consequence: Two faithful implementations of FR22 and FR23 deadlock on a registry outage in exactly the pilot scenario the kit and the register-availability profile are built for, and the failure surfaces only at integration.
- parent verdict: **PARTLY** — FR22 (verifier pinned fallback) and FR23 (wallet refuses) are both in the document as written; the offline deadlock follows, but registry-outage handling is arguably Story 3.5/6.5 territory

**10. Story 6.1 (l.755-770) and Story 6.2 (l.772-787)**
- condition: The 1.0 s p95 budget is called the 1.0 s p95 prover budget checked on a device and one 1.0 s p95 spanning both the phone and the server, while spec EE-ZKP-040 (spec/EE-EUDIW-TS-1.0.md:749) sets proof generation at 2.0 s p95 and 3.5 s p99 on the device and verification at 1.0 s p95 - two budgets, not one.
- fix: State both budgets with their identifiers in Stories 6.1 and 6.2 (prover EE-ZKP-040 at 2.0 s p95 and 3.5 s p99 device-side; verifier 1.0 s p95, reached at p95 4.91 s on the measured host) and delete the single-budget premise.
- consequence: An agent measures the device prover against the verifier's number, so a compliant 2.0 s p95 result is reported as a breach, or the host's missed 1.0 s p95 is reported as a device finding.
- parent verdict: **CONFIRMED** — spec:749 sets two budgets in one requirement (device prover 2.0 s p95 / 3.5 s p99; verifier 1.0 s p95). Story 6.1's So-that line still reads 'the 1.0 s p95 prover budget is checked on a device'

**11. Epic 6 (l.747-850) and Story 6.3 (l.789-803)**
- condition: No Epic 6 criterion cites the specification identifiers behind its budgets - EE-ZKP-040, EE-ZKP-041 and EE-ZKP-044 appear nowhere in the document - and Story 6.3 measures a ~360 KB proof over BLE against a stated budget without naming EE-ZKP-044's 4.0 s p95 transport budget or the EE-CNF-006 gate, while EE-ZKP-045a (a transport budget never overrides EE-ZKP-055) is absent although Story 8.11 defines the same tension.
- fix: Cite EE-ZKP-040 and EE-ZKP-041 in Story 6.1, EE-ZKP-044 with the 4.0 s p95 figure and EE-CNF-006 in Story 6.3, and add EE-ZKP-045a to Story 6.3 or Story 8.11.
- consequence: The measurement programme can publish a proximity figure that satisfies no budget the specification states, and the rule that stops a transport-budget miss being answered by dropping to a plain mdoc is never put in front of the implementer.
- parent verdict: **CONFIRMED** — spec:751 EE-ZKP-041 250 MB; spec:755 EE-ZKP-044 4.0 s p95 transport, precondition of proximity Tier 2 via EE-CNF-006; spec:761 EE-ZKP-045a. None is cited in Epic 6

**12. FR66 (l.123) and Story 8.11 (l.1204-1218)**
- condition: Both present EE-ZKP-055a as a general guard that refuses plain-mdoc-only requests on a ZK-capable device, dropping the condition the specification attaches to it - that the request is not authenticated by a registration certificate (spec/EE-EUDIW-TS-1.0.md:791, with EE-PRO-025 and EE-PRO-004).
- fix: Reword FR66 and Story 8.11's Given to the specification's condition, cite the EE-ZKP-055a row in docs/CONFORMITY.md, and keep the tier labelling and the refusal in the same criterion.
- consequence: Implemented literally, the guard refuses the plain path on certificate-authenticated requests too, which is neither the specification's rule nor the corpus's R7 mitigation, and it contradicts the certificate-driven decisions Epic 4 ships in the same backlog.
- parent verdict: **CONFIRMED** — spec:791 EE-ZKP-055a is conditioned on the request NOT being authenticated by a registration certificate, with three named shapes (EE-PRO-004, EE-PRO-025, proximity). FR66 and Story 8.11 both drop the condition

**13. Story 9.3 (l.1261-1275), Story 4.3 (l.594), spec EE-POA-030 (spec/EE-EUDIW-TS-1.0.md:455) and spec §22 item 21 (l.1286)**
- condition: Story 9.3's Given restates the fork's stricter rule as at most one predicate per presentation with no mention of EE-POA-030's exception for a registered intended use that names each predicate - the exception Epic 4 exists to implement.
- fix: Extend Story 9.3's Given to EE-POA-030 as written and cross-reference Story 4.3 and FR18, so the negotiation library and the policy engine cannot diverge.
- consequence: The library re-imposes the blanket single-predicate refusal Epic 4 replaces, so a certificate-backed two-predicate request passes the policy engine and is then refused downstream.
- parent verdict: **CONFIRMED** — spec:455 EE-POA-030 carries the exception 'unless the relying party's registered intended use names each of them'; spec:1286 records it as a divergence from ISO 7.2.5. Story 9.3 omits the exception

**14. Story 1.3 (l.284-299), Story 9.4 (l.1277-1291), Additional Requirements l.146, Stories 1.4, 2.1 and 5.4**
- condition: The invariant names the AV clause runner, the carrier runner and the threshold boundary tests as new suites that must be added to tests/README.md, but only Stories 1.4, 2.1 and 5.4 carry such a criterion; the AV runner and the boundary suite do not.
- fix: Add the catalogue criterion to Stories 1.3 and 9.4 and to any other story that creates a suite, stating what the suite proves.
- consequence: The two suites these epics add the most work to are absent from the catalogue the repository uses to say what is proven and by what, which is the drift the Additional Requirements section exists to prevent.
- parent verdict: **CONFIRMED** — checked Stories 1.3 and 9.4: neither carries the tests/README.md catalogue criterion that Stories 1.4, 2.1 and 5.4 do

**15. Stories 1.3 (l.284), 2.1 (l.355), 4.5 (l.626), 5.4 (l.700), 9.4 (l.1277) against Story 7.3 (l.892-906)**
- condition: Story 7.3 alone requires its derivation to run as part of the repository's checks, while no story wires the new runners or suites into make check or CI and NFR5's green definition covers only the pre-existing tasks.
- fix: Give every suite-creating story the criterion that the suite runs in make check or the matching CI job, or that the story records why it is excluded and how it is run instead.
- consequence: New suites pass once at review and then rot, and a green repository holding a runner nobody executes is the failure Story 7.3's own rationale names for hand-maintained rows.
- parent verdict: **CONFIRMED** — no suite-creating story requires its suite to run in make check or CI; only Story 7.3 requires a check

**16. Stories 1.3 (l.284), 2.4 (l.401), 3.7 (l.521), 6.1 (l.755), 9.6 (l.1309), 10.1 (l.1332), 10.3 (l.1364), 11.3 (l.1449), 11.4 (l.1465) against Story 8.6 (l.1124)**
- condition: Nine stories can only be closed with an external person, service or device - an external wallet or verifier, the Commission's issuer service, three human integrators, a legal reviewer, an audit firm, a security reviewer, a named handset - yet only Story 8.6's criterion carries a marker (this step is user-gated) and only Epic 9's introduction carries a gating note.
- fix: Add a Blocked by (external) line to each such story and name the acceptable fallback evidence, following Story 6.1's existing pattern of leaving a row PENDING-DEVICE with a dated reason.
- consequence: An agent assigned Story 2.4 or Story 10.1 either fabricates a result or reports the in-repo half as done, and the external claim the FR exists to produce is never made or never recorded as impossible.
- parent verdict: **CONFIRMED** — by inspection: 2.4, 3.7, 6.1, 9.6, 10.1, 10.3, 11.3, 11.4 need an external party or device; only 8.6 is marked user-gated

**17. Story 7.11 (l.1019-1033), Coverage Map (l.156-227), NFR7 (l.133)**
- condition: Story 7.11's Given narrows estimate discipline to effort ranges derived from agent-assisted work, while Report 2's Q1 range (6-12 engineer-months of re-creation cost) is explicitly human re-creation cost, and Q1 is cited nowhere in the document - the map cites Q2 and Q4 and Story 9.6 cites Q3.
- fix: Widen Story 7.11's Given to every effort or cost figure this repository publishes, and add Q1's range with its baseline, method and confidence to Story 7.11 or Story 7.10.
- consequence: The one commercial figure a buyer would read is the one figure no story governs, so it can be restated without its method or its low-to-medium confidence.
- parent verdict: **PARTLY** — Story 7.11's Given does narrow to agent-assisted effort ranges, and Q1 is cited nowhere - but Q1 is a cost figure, not an effort estimate, so the fix is to widen the story rather than move Q1

**18. Overview l.38-40 against every story's Given (for example Story 8.2 l.1067, Story 10.5 l.1404, Story 11.6 l.1505, Story 6.2 l.775-785)**
- condition: The document declares a source-label vocabulary (REC, VERIFIED, ESTIMATE, UNVERIFIED) and then carries no label into any of the 75 stories, so the corpus's UNVERIFIED-from-ISO slip date, R10's JUDGEMENT, R11's UNVERIFIED and R12's ASSUMPTION notes arrive as plain Given facts.
- fix: Carry the corpus sentence's label into each Given (for example Q1 2027 UNVERIFIED from ISO) or add a Premises not established line per story, as the Open Questions section already does at l.1577.
- consequence: An agent cannot tell an established premise from an assumption, so an unverified date or third-party behaviour is built on as fact and later restated without its qualifier.
- parent verdict: **CONFIRMED** — the Overview declares REC/VERIFIED/ESTIMATE/UNVERIFIED and no story Given carries a label

**19. Sequencing and recorded decisions (l.1529-1550)**
- condition: It states that epic numbering follows the research corpus's own ranking - the opportunities in priority order, then the Estonia-alignment items - but Epics 6, 7 and 8 are Report 2's R4, R3 and R5 out of order, Epic 3 is R2, and Epic 6 is Appendix A3's D2/M3 rather than an Estonia-alignment item.
- fix: Either renumber to the stated rule or state the rule actually used (O1-O6 by ranking, then the appendix roadmap phases, then the remaining Report 2 recommendations) and drop the traceability claim attached to the epic numbers.
- consequence: A reader taking the epic number as the signal the table invites places the measurement programme above the documentation-honesty work the same document says must run first, and cannot recover the source ordering from the numbers.
- parent verdict: **CONFIRMED** — read the section against the epic table: Epics 6, 7 and 8 are R4, R3, R5 out of order, Epic 3 is R2, and Epic 6 is Appendix A3's D2/M3, so the stated numbering rule does not hold

## Edge-case-hunter lens

65 unhandled-path findings. Each names a location, the condition it found unhandled, a closing guard and
the consequence. Grouped by the epic the location belongs to.

### Epic 1 (5 findings)

- `:263-264 (Story 1.1)` — A clause removed or renumbered in a newer profile revision leaves its test id unflagged
  - guard: assert every registry test_id resolves to a clause in the pinned version and fail on an orphaned test id; diff pinned vs current version to report removed clauses
  - consequence: An orphaned test id reads as evidence for a clause that no longer exists
- `:292-296 (Story 1.3)` — A clause that errors, or a candidate that will not start, has no result state
  - guard: extend the per-clause result set to PASS|FAIL|ERROR|NOT_EXECUTED and assert a crashing candidate prints ERROR for every unrun clause, never PASS
  - consequence: A crashed or unexecuted clause is silently absent from the report
- `:308-311 (Story 1.4)` — A recognised av-vci:// URI whose parameters are absent or malformed is unhandled
  - guard: test("av-vci:// with missing credential_offer or unparseable payload refuses loudly with the offending parameter named")
  - consequence: A malformed AV invocation is neither entered nor refused loudly
- `:324-327 (Story 1.5)` — AVSUA attestation present but expired, wrongly anchored or wrongly bound is unhandled
  - guard: test("AVSUA present but expired or from an untrusted issuer refuses issuance") alongside the withheld-attestation test
  - consequence: An invalid AVSUA passes a suite that only withholds the attestation
- `:339-343 (Story 1.6)` — Reproduction host assumptions (CPU baseline, JDK, sibling longfellow-zk) are unstated
  - guard: have the kit print and record the host profile (x86-64-v3 RUSTFLAGS, JDK 17, ../longfellow-zk present) before running the documented commands
  - consequence: A third party's SIGILL or missing sibling dir reads as a conformance failure

### Epic 2 (7 findings)

- `:363-367 (Story 2.1)` — A vp_token entry matching neither carrier, or an unknown -carrier name, has no case state
  - guard: assert the runner reports carrier=unmatched and refuses for bytes that are neither '{' nor a CBOR map/array head, instead of falling through to InterimJSON
  - consequence: Unrecognised response encodings are reported as interim-JSON parse failures
- `:365 (Story 2.1)` — An encrypted direct_post.jwt response that fails to decrypt has no named outcome
  - guard: add a case asserting an undecryptable JWE is refused with response_mode=encrypted and reason=jwe-decrypt-failed
  - consequence: Decryption failure is indistinguishable from a plain-mode binding failure
- `:379-382 (Story 2.2)` — A response no carrier can parse is a failure outside the six named properties
  - guard: test("malformed or empty vp_token entry refuses naming the parse failure, never a binding property and never 'invalid'")
  - consequence: Integrators cannot tell a parse failure from a property mismatch
- `:379-382 (Story 2.2)` — A well-formed response carrying no provable presentation is neither failure nor pass
  - guard: assert the runner reports the ErrNoZkDocument empty answer as its own outcome (no-presentation, valid:false), not as a binding-property failure
  - consequence: The legal empty answer is reported as a property violation
- `:396-397 (Story 2.3)` — Label drift: the assertion checks a label exists, not that it matches the current version
  - guard: assert fixture_version == the current carrier-profile version and fail on superseded labels, not merely on a missing label
  - consequence: A fixture labelled to a superseded draft still passes the labelling test
- `:396-397 (Story 2.3)` — The Then covers profile rules; the asserted test guards fixtures only
  - guard: extend the assertion to parse docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md rules and fail on any rule with no version label
  - consequence: A new profile rule can be added with no version label
- `:409-413 (Story 2.4)` — The external target is unreachable or does not build
  - guard: publish state=BLOCKED with the build/reach failure output, distinct from a test failure, and assert the report never omits the target
  - consequence: A blocked external run is indistinguishable from a pass or an omission

### Epic 3 (8 findings)

- `:434-438 (Story 3.1)` — The wallet never returns to response_uri; session expiry behaviour is unstated
  - guard: document and test: a session whose response_uri callback never arrives expires after -session-ttl and answers 404/410 naming the session id
  - consequence: A never-arriving callback leaves the relying party hanging with no status
- `:450-454 (Story 3.2)` — TARA test client unreachable, or the RIA agreement absent at run time
  - guard: assert the adapter fails closed and logs agreement-missing when TARA is unreachable, and that the integration test reports blocked rather than passed
  - consequence: An unreachable TARA reads as a passing adapter contract
- `:466-470 (Story 3.3)` — Register configured but unreachable or timing out is not 'no implementation'
  - guard: test("register timeout or 5xx refuses the presentation and logs the register error; a cache hit before expiry is not required")
  - consequence: A hung register either blocks the presentation or silently allows it
- `:466-470 (Story 3.3)` — Register returns an entry for another relying party, or an expired or withdrawn entry
  - guard: assert the lookup matches the requesting relying-party id and rejects entries past validity, logging entry-mismatch and entry-expired as refusals
  - consequence: An other-party or lapsed registration row is treated as an allow
- `:498-502 (Story 3.5)` — The two credential-bearing cases have no stated expected outcome
  - guard: state and assert accept/refuse for all four cases in the machine-readable result, including the foreign-PID and foreign-AV cases
  - consequence: Half the acceptance cases can pass with no defined expectation
- `:498-502 (Story 3.5)` — EE-ZKP-055a governs three unauthenticated shapes; only the least-trusted one is asserted
  - guard: add cases for an org-iso-mdoc request without reader authentication and a proximity request without reader authentication, each refusing plain-mdoc-only requests
  - consequence: Two of the three unauthenticated downgrade paths stay untested
- `:514-517 (Story 3.6)` — A technical failure, timeout or aborted session is a third outcome
  - guard: add a third UI-contract state (error/indeterminate) and assert a verifier timeout shows neither the refusal nor the cancellation message
  - consequence: An infrastructure failure is shown to the holder as a policy refusal
- `:529-532 (Story 3.7)` — An integrator who never reaches a first successful verification is outside the distribution
  - guard: record every participant including failures and abandonments, and report n and the failure count alongside the distribution
  - consequence: Failures are dropped, so the reported onboarding time is optimistic

### Epic 4 (8 findings)

- `:570-574 (Story 4.1)` — IntendedUseSource present but throwing or timing out is not 'absence'
  - guard: assert a throwing or timed-out source refuses with source-error logged, distinct from interface-absent, under the same fail-closed default
  - consequence: A failing register source is treated as no source, or crashes the policy
- `:586-590 (Story 4.2)` — Certificate parses but its signature, chain anchor or validity period is invalid
  - guard: assert refusal for untrusted-anchor, tampered-signature, expired and not-yet-valid chains, naming the failing check
  - consequence: A forged or expired certificate parses into a trusted intended use
- `:586-590 (Story 4.2)` — Certificate profile version newer than the version the parser targets
  - guard: assert a certificate whose profile version exceeds the targeted version refuses and logs version-unsupported rather than being partially parsed
  - consequence: A newer certificate profile is silently mis-parsed as the older one
- `:602-606 (Story 4.3)` — A request naming three or more predicates: the rule keys on 'both'
  - guard: table-test predicate counts 1..3 with a certificate naming them all and assert N>2 refuses, or define the upper bound explicitly
  - consequence: A three-predicate request has no defined decision
- `:602-606 (Story 4.3)` — Certificate names both predicates but the wallet holds one predicate false
  - guard: assert the PREDICATE_HELD_FALSE decision still governs with a certificate present, and no false predicate is transmitted
  - consequence: The loosened path may transmit a false predicate the tests assumed impossible
- `:618-622 (Story 4.4)` — Presentation not backed by a certificate: DASH_03 field content is unstated
  - guard: assert the log entry for an unattested or plain-fallback presentation marks the relying-party fields absent rather than omitting the entry
  - consequence: Non-certificate presentations log with no defined audit fields
- `:618-622 (Story 4.4)` — Certificate present but one named field is absent (e.g. privacy-policy URL)
  - guard: test each field individually for the absent case and assert an explicit absent marker, never a silent blank
  - consequence: An incomplete certificate leaves an unnamed gap in an audit log
- `:634-638 (Story 4.5)` — Certificate states beyond malformed, empty-use and absent are not generated
  - guard: add expired, revoked and untrusted-anchor generators to the property-test set and assert each refuses
  - consequence: Expired or revoked registrations are outside the fail-closed suite

### Epic 5 (7 findings)

- `:659-663 (Story 5.1)` — Signing-key rotation: a document signed by a successor key is refused
  - guard: assert the pinned key set holds at least two keys and that a document signed by a non-pinned successor key refuses with key-not-pinned logged
  - consequence: A scheme owner's key rollover silently stops circuit-list updates
- `:675-680 (Story 5.2)` — A signature-valid but stale document: no validity window or issue date
  - guard: add issued_at/expires_at to the document and assert a document past expiry refuses and logs stale-list rather than accepting withdrawn circuits
  - consequence: A withdrawn circuit stays acceptable indefinitely off a cached list
- `:675-680 (Story 5.2)` — A listed entry whose minimum library version exceeds the running library
  - guard: assert an entry with minimum-library-version greater than the verifier's own version is refused with that reason although its signature and hash are valid
  - consequence: A circuit this build cannot run is accepted as allowed
- `:692-696 (Story 5.3)` — A valid document that holds no circuit for the requested predicate
  - guard: assert the wallet refuses the ZK path with no-matching-circuit named, distinct from a registry-fetch failure
  - consequence: A valid-but-unhelpful registry is reported as a fetch failure
- `:692-696 (Story 5.3)` — Wallet and verifier resolve different registry document versions
  - guard: record the resolved document version in the presentation and assert the verifier refuses a differing version with version-skew named
  - consequence: A version skew between wallet and verifier surfaces as an unexplained refusal
- `:708-712 (Story 5.4)` — Instances configured at different times, or one on the pinned fallback, report different sets
  - guard: pin the document version and fetch time in the comparison and assert the test fails on a version mismatch rather than on a set difference
  - consequence: A legitimate version difference is reported as a two-party disagreement
- `:724-728 (Story 5.5)` — Audit query for an unknown, refused or aged-out presentation id
  - guard: assert the query returns a distinguished not-found result for an unknown or refused id, and test the retention boundary
  - consequence: An unknown presentation id returns an empty or fabricated provenance

### Epic 6 (5 findings)

- `:765-767 (Story 6.1)` — Device figures that exceed the stated budgets have no pinned outcome
  - guard: define the consequence of a budget miss (proximity Tier 2 withheld, MEASUREMENTS.md records the miss) and assert it in the harness threshold check
  - consequence: A device that misses 2.0 s p95 is reported with no decision
- `:782-785 (Story 6.2)` — Measured throughput falls below the rate the relying party must provision for
  - guard: state the relying party's required rate and assert the published figure is marked below-requirement when it falls short
  - consequence: A capacity shortfall is published as a neutral figure
- `:799-801 (Story 6.3)` — 'A stated budget' is unnamed and a measured transfer over budget has no outcome
  - guard: name EE-ZKP-044's <= 4.0 s p95 as the budget and assert an over-budget transfer withholds proximity Tier 2 and records the miss
  - consequence: The proximity claim cannot pass or fail against an unnamed bound
- `:800 (Story 6.3)` — Transfer interrupted mid-way by a BLE disconnect or a truncated payload
  - guard: test a mid-transfer disconnect: the partial payload is discarded and the session refused or retried, never recorded as a completed measurement
  - consequence: A partial transfer is measured as a fast successful presentation
- `:815-817 (Story 6.4)` — The refusal-cost bound has no named value, so the regression has no threshold
  - guard: pin the bound (e.g. refusal latency < 1 s p95 on the same percentile harness) and fail the run when it is exceeded
  - consequence: The 54-58 s refusal defect can return under a bound that never fails

### Epic 7 (4 findings)

- `:886-888 (Story 7.2)` — A row whose named symbol cannot be located anywhere (a removed symbol)
  - guard: add a third audit outcome, symbol-not-found, striking the row as describing removed code, alongside accepted and corrected
  - consequence: A row describing removed code is left standing as current
- `:902-904 (Story 7.3)` — A row whose test exists but is skipped or disabled reads as evidence
  - guard: assert the derivation reads executed-test counts and fails a row whose test is @Ignore'd or t.Skip'd, not only a deleted one
  - consequence: A disabled test keeps its status green and the drift stays invisible
- `:932-934 (Story 7.5)` — A clause that cannot be assessed has no member in the three-status set
  - guard: add an UNVERIFIED status, mirroring spec section 23, and assert a clause whose source text is unreadable is marked unverified, not not-applicable
  - consequence: An unassessable clause is recorded as satisfied or inapplicable
- `:947-951 (Story 7.6)` — TS13 items other than mso_mdoc_zk and zk-jwt get no stated marking
  - guard: require a status per listed item (ZkSystemSpec, ZkDocument, longfellow-libzk-v1, circuit_hash) and assert none is left unmarked
  - consequence: Four named TS13 constructs are missing from the clause map

### Epic 8 (3 findings)

- `:1116-1120 (Story 8.5)` — LoA-High authentication fails or its route is unavailable during the ZKP demonstration
  - guard: record the failing branch: assert the combined flow does not complete on a failed LoA-High authentication and that the route used is recorded
  - consequence: The pilot demonstration has no defined outcome when authentication fails
- `:1132-1136 (Story 8.6)` — The user withholds approval for the user-gated filing
  - guard: record an explicit state (filing withheld by user, with date) so the story is not silently open, and assert issue tracking proceeds regardless
  - consequence: A withheld filing is indistinguishable from unfinished work
- `:1164-1168 (Story 8.8)` — Enforcement in the dispatch path has no refusal branch for bytes matching no family
  - guard: make CarrierForBytes return an unmatched-family error instead of defaulting to InterimJSON and assert the dispatch test refuses an unrecognised format
  - consequence: One format family per scheme is unenforced at the dispatch default

### Epic 9 (5 findings)

- `:1253-1257 (Story 9.2)` — An entry still awaiting scheme-owner acceptance: presentation behaviour is unstated
  - guard: assert a presentation for a threshold whose entry state is pending-acceptance refuses with circuit-not-accepted, distinct from unmapped
  - consequence: A pending circuit is either refused or used with no defined rule
- `:1285-1289 (Story 9.4)` — The day after the threshold is absent from the enumerated boundary set
  - guard: add the day-after row per threshold (false on the day, true from the day after) and assert the transition in both directions
  - consequence: An off-by-one in the age comparison passes the boundary suite
- `:1287-1289 (Story 9.4)` — An invalid calendar date or a future birth date: 'incomplete date' is a different input
  - guard: add rows for 31 February, a birth date in the future and an attestation with empty date fields, asserting each refuses rather than answering false
  - consequence: An impossible or future birth date is answered as a plain false predicate
- `:1301-1305 (Story 9.5)` — Holder holds the threshold but cannot prove: prover failure or exhausted attestation
  - guard: assert a distinct reason for proof-unavailable and exhausted-attestation, reusing the existing last-attestation and prover-failure strings
  - consequence: A prover failure is shown as a threshold the holder does not have
- `:1317-1321 (Story 9.6)` — The legal review finds a mapped threshold unsupported by its citation
  - guard: assert the review outcome table forces that threshold to unsupported (a map revision) when the citation fails, and that the circuit set follows
  - consequence: A citation the reviewer rejects stays mapped and keeps proving

### Epic 10 (6 findings)

- `:1338-1344 (Story 10.1)` — The Commission's issuer service is unavailable or absent at test time
  - guard: record state=BLOCKED with the service response when the issuer is unreachable and assert the mock path still passes offline
  - consequence: An unreachable Commission service leaves the interop claim unstated
- `:1356-1360 (Story 10.2)` — A batch issued partially: fewer credentials than requested, or a mid-batch failure
  - guard: assert a partial batch leaves the holder with a defined set (usable complete credentials only) and records the shortfall, never a silent single credential
  - consequence: A partial batch silently reduces the unlinkability the story claims
- `:1372-1376 (Story 10.3)` — A WUA issued but rejected, or the wallet-provider service unavailable
  - guard: assert a WUA the wallet or verifier rejects fails issuance naming the claim, and the interop test records blocked when the provider is unreachable
  - consequence: A WUA that cannot be validated passes a test that only checks issuance
- `:1388-1392 (Story 10.4)` — A CRL present but past its nextUpdate: neither verified nor unavailable
  - guard: assert an expired CRL follows the stated soft-fail/flip policy with stale-crl logged, and state that the flip condition covers it
  - consequence: A stale CRL is used as a current revocation source, or silently ignored
- `:1388-1392 (Story 10.4)` — A CRL whose signature does not verify or whose issuer is unknown
  - guard: assert an unverifiable CRL refuses the certificate and logs crl-signature-invalid, rather than being treated as the availability case
  - consequence: A forged or mismatched CRL soft-fails as an availability failure
- `:1404-1408 (Story 10.5)` — A verifiable requirement row that fails on the device set has no state
  - guard: add a failed state and its rule (pilot gated) and assert a device without a usable hardware-backed key yields failed, not unverified
  - consequence: A device failing a security requirement is reported as merely unverified

### Epic 11 (7 findings)

- `:1427-1429 (Story 11.1)` — -drain-timeout expires with requests still in flight
  - guard: document and test the expiry branch: remaining in-flight requests are cut at the deadline, with the exit code and log line named
  - consequence: Operators have no defined behaviour for a drain that cannot finish
- `:1443-1445 (Story 11.2)` — The numeric availability target is breached with no trigger tying it to an incident severity
  - guard: assert the monitoring config fires each severity from the numeric target and record the target-to-severity mapping in the runbook
  - consequence: A target breach raises no incident because no trigger was defined
- `:1458-1461 (Story 11.3)` — An audit finding whose resolution is deferred or refused before the exit milestone
  - guard: require a disposition per finding (fixed, deferred with owner and date, accepted with rationale) and assert an open finding blocks the milestone exit
  - consequence: The milestone can be declared met with audit findings still open
- `:1474-1476 (Story 11.4)` — The external security review cannot be completed before the pilot
  - guard: record the blocked state with its procurement reason and assert the pilot exit criterion stays unmet until the review completes
  - consequence: An unperformed review is indistinguishable from a clean one
- `:1491-1493 (Story 11.5)` — The game day aborts partway because the environment is unavailable
  - guard: record an aborted outcome with the point of failure and assert an aborted exercise does not satisfy the runbook-exercised exit criterion
  - consequence: An aborted exercise is recorded as an exercised runbook
- `:1506-1508 (Story 11.6)` — The CT log is unreachable, so the SCT cannot be checked
  - guard: state and assert the CT unavailability policy (refuse, or soft-fail with a logged reason) as Story 10.4 does for CRLs
  - consequence: An unreachable CT log leaves acceptance behaviour undefined
- `:1523-1525 (Story 11.7)` — A credential present but in a form the wallet cannot present, or invalid
  - guard: assert a held-but-unpresentable credential (wrong format, expired, revoked) fails the session naming the credential id and the reason
  - consequence: A credential held in another form is reported as simply missing

## Structure lens

Purpose read: a reference-shaped artefact with two readers — the maintainer confirming corpus coverage and
ordering, and an implementation agent reading one story. Model: **Reference/Database** primary (MECE,
uniform per-item schema, random access), **Prompt/Task Definition** secondary (meta-first setup, explicit
execution order).

Judged against Reference: passes on schema and random access; **fails on the index** (coverage map
incomplete, no story index or contents) and on **IVIECE** (Epic 8 mixes privacy controls, standards watch
and dependency hygiene). The secondary lens adds one front-loading defect: the epics table — the
maintainer's decision surface — sits behind 2,089 words of inventory.

17 rows: 14 changes (MOVE ×4, QUESTION ×7, CONDENSE ×2, MERGE ×1 rolled up with 4 minor fixes) and 3
PRESERVE. Net 70 words cut if all changes are accepted (0.6% of 12,412 words); 570 words (4.6%) if the
conditional Coverage Map `Source item` column is also dropped. No length target was given. The three
PRESERVE rows deliberately forgo ~1,900 words of available cutting because the FR inventory, the
Covers/Source lines and the invariant echoes each serve a reader a reduction would strand.

Principal rows: MOVE the epics table above the inventory (0 words, front-loads the decision surface);
add a source-key legend for S/L/E/U/A/D/M/P/Q (the reason a Source line is unresolvable today); QUESTION
the missing map rows; bullet the 371 Given/When/Then lines so they stop collapsing into one paragraph in
rendered Markdown; CONDENSE the three epic intros that restate sequencing; MERGE the two 'not made into
stories' paragraphs that sit ~1,300 lines apart.

## Prose lens (ran on top of structure's findings)

Purpose read: turn three research reports into a reviewable, traceable turn of 11 epics and 75
testable stories, for the human maintainer and the AI implementation agents. Style guide: Microsoft
Writing Style Guide. 12 rows presented plus 5 minor fixes held back; net word effect ≈ +10 words
(<0.1% of 12,412). Every row helps the agent reader and costs the human reader nothing.

Parent verification of the prose lens's checkable claims (computed on the document):

| Prose claim | Verdict |
|---|---|
| Story 8.4 "backed by a test that no tracking identifier is added" is a broken relative clause | **CONFIRMED** — 1 occurrence; FR42 uses "asserting no tracking identifier is added" |
| Story 3.5 "follows the policy `EE-ZKP-055a` already governs" has a missing "that" | **CONFIRMED** — 1 occurrence |
| Story 6.1's So-that cites the 1.0 s p95 prover budget while its ACs assess ≤2.0 s p95 / ≤3.5 s p99 | **CONFIRMED** — third independent hit on this defect (adversarial #10, prose row 4) |
| `WPSM_01`– is an unterminated identifier range | **CONFIRMED, and it is this document's typo, not verbatim reproduction** — the lens hedged that it might be quoted from the source appendices; the appendix contains `WPSM_01` but not in that broken form |
| `EE-POA-027`–`031` abbreviates against line 1227's full form | **CONFIRMED** — 2 occurrences, inconsistent |
| "EE-PoA" as an identifier slips once where `ee.riik.poa.1` is used 5 times | **CONFIRMED** — 1 vs 5 occurrences |
| "optimizations" is the document's one US spelling in otherwise British English | **CONFIRMED** — `optimizations` 1, `optimisations` 0; `optimization log` 1 and must stay, it matches the `docs/MEASUREMENTS.md` heading |
| `[VERIFIED]` and `[ESTIMATE]` appear nowhere; `[REC]` and `[UNVERIFIED]` once each | **SUBSTANTIALLY CONFIRMED** — each label appears exactly once in the legend; only `[REC]` and `[UNVERIFIED]` are used in the body beyond it |
| "1,300 verifies/hour/core quiet" strands the adjective | **CONFIRMED** — 1 occurrence |
| "is how two parties end up…" (Epic 5) and "respectively" (Story 3.8) | **CONFIRMED** — 1 occurrence each |

## Disposition (applied 2026-10-23)

The document was revised in place from the pristine copy at
`/home/notroot/.hermes/cache/scratch/epics.pre-fixes.md` (83,239 chars / 1,584 lines → 98,238 chars /
1,791 lines). Every edit asserted its anchor before writing; the whole run aborted on any miss.

**Applied — the four specification-fidelity defects (all four confirmed against `spec/EE-EUDIW-TS-1.0.md`):**

| Finding | Fix |
|---|---|
| Story 6.1 cited the verifier's 1.0 s p95 as a *prover* budget | Story 6.1 now names `EE-ZKP-040`'s device prover budgets (≤ 2.0 s p95, ≤ 3.5 s p99) and `EE-ZKP-041`'s ≤ 250 MB; Story 6.2 re-expresses the verifier-side ≤ 1.0 s p95 as capacity. A budget miss now has a recorded outcome. |
| Story 6.3 measured against an unnamed budget | Names `EE-ZKP-044`'s ≤ 4.0 s p95 and `EE-CNF-006` as the Tier-2 precondition, and adds `EE-ZKP-045a`: an over-budget pair retries or refuses, never falls to Tier 1. |
| FR66 and Story 8.11 dropped `EE-ZKP-055a`'s condition | Both now carry the condition — the request is *not* authenticated by a registration certificate — and all three shapes (`EE-PRO-004` unsigned DC API, `EE-PRO-025` `org-iso-mdoc`, proximity). A certificate-authenticated request is explicitly not refused by this guard. |
| Story 9.3 omitted `EE-POA-030`'s exception | Restated as the specification states it, with the registered-intended-use exception, an end-to-end assertion against Story 4.3's fixture, and the spec §22 item 21 reference. |

**Applied — traceability and integrity:**

- Coverage Map: all 76 stories now appear (the six missing rows — 2.3, 3.8, 5.6, 6.6, 7.10, 7.11 — are
  added), computed and re-verified. Story 2.3 moved from FR6 to FR40, removing the double-scheduled
  labelling duty with Story 8.2.
- New **NFR binding** table plus a Definition of Done for every code story (NFR5's count corrected from
  "169 `@Test` across 26 test files" to 25 files, which is what the tree holds).
- Story 4.6 added: the certificate fixture generator Story 4.5 depended on and no story delivered.
- Source-key legend added, and the genuinely ambiguous register collisions fixed (`matrix A3`,
  `App. A4`, `matrix A6/A7/A16`, `matrix L1/L3` and so on), because bare `A1`–`A6` and `R1`–`R5` each
  denote two different registers.
- Spec role stated as evidence rather than as a source of stories, with the four uncovered requirements
  that touch this repository named as deliberate exclusions (`EE-ZKP-062`, `EE-REV-007`, `EE-PRO-025`,
  `EE-ZKP-043`).
- Eleven externally-blocked stories now carry a **Blocked by (external)** line naming the party and the
  fallback evidence, so none can be closed by fabricating a result.
- Offline rule stated once for the registry (Stories 5.3 and 3.5), closing the FR22/FR23 deadlock.
- Suites created by Stories 1.2, 1.3, 2.1, 5.4 and 9.4 now require the `tests/README.md` catalogue entry
  and wiring into `make check` or the matching CI job, so a new runner cannot pass once and rot.
- Premises the corpus did not establish are marked `[UNVERIFIED]` in the stories that rest on them
  (Story 8.2's ISO slip date, Story 8.9's upstream-dormancy judgement, Story 8.10's licence position).
- Sequencing states the numbering rule actually used; Epic 11 gained an exit criterion; the "not made
  into stories" material is one subsection instead of two, ~1,300 lines apart.

**Applied — editorial:** the epics table moved directly under the Overview with a `stories` index column;
the 393 acceptance-criteria lines bulletised so they stop rendering as one paragraph; cross-references
canonicalised to `Story N.M`; three epic introductions condensed and the ordering claims left to
Sequencing; the source-label legend trimmed to the labels actually used; the twelve prose rows and five
minor fixes (Story 8.4's broken relative clause, Story 3.5's missing "that", the unterminated
`WPSM_01` range — this document's typo, not the corpus's — `EE-POA-027`–`031` spelled out, the US
spelling, `EE-PoA` → `ee.riik.poa.1`, "verified-CRL-named" unpacked, "is how two parties" rewritten).

**Applied in a second pass (the author's "apply all"):**

- **44 more edge-case-hunter findings** became acceptance criteria on their own stories — the register
  timeout/skew/stale-list states, the certificate profile-version and three-predicate bounds, the
  held-false rule under a certificate, the absent audit field, mid-transfer BLE disconnect, the pinned
  refusal bound, removed-symbol and unassessable-clause outcomes, the withheld filing, the partial batch,
  a rejected WUA, a failing device-matrix row, an aborted game day, an unreachable CT log, drain-timeout
  expiry, and the rest. Three findings (6, 49, 50) were dropped as duplicates of criteria an earlier
  edit had already carried, leaving 41 net additions.
- **Epic 8 re-scoped, as the structure lens recommended.** Story 8.10 (dependency and vendored-artefact
  hygiene) moved to Epic 11 as **Story 11.8**, and Story 8.11 (plain-fallback guard) moved to Epic 4 as
  **Story 4.7**, where the same class of policy control lives. Epic 8 is renamed *Standards watch and
  open-risk reporting* and its introduction now points at 4.7 and 11.8 instead of claiming the disclosure
  controls. The sprint tracker regenerated with the two new keys and the two old ones dropped as orphans
  (both were `backlog`, so no progress was lost).
- **Coverage Map `Source item` column dropped** (structure row 14), leaving `| FR | story |`. The
  per-story `Source:` lines remain the provenance record, and all 66 rows survived the change.

**Not applied — cannot be:**

- **Adopting the concurrent session's PRD.** `_bmad-output/planning-artifacts/prds/` and
  `landscape-extract.md` are another session's unfinished work, and the stories are sourced from the
  corpus with a per-story citation. Adopting them would mean rewriting the backlog from a document that
  is still being written. Revisit if that session finishes.

**Verified after the edits:** 76 stories all present in the Coverage Map (now two columns); all 66 FRs
claimed by a story and defined; every story's number agrees with the epic it sits in; 434 acceptance
lines bulletised with zero unprefixed; 11 blocked-by markers; the sprint tracker regenerated clean
(`11 epics, 76 stories, 87 backlog, 0 changed, 0 warnings`, `validate: valid`, with the two renumbered
keys added and the two old ones dropped as backlog orphans); `make lint` exits 0.


### Reconciled with the PRD (same day)

`prd.md` reached `status: final` at 05:05, after this document was written, and names this file as its
decomposition. The reconciliation is recorded in `epics.md` itself (`Reconciliation with the PRD`):
the two identifier namespaces are kept and mapped rather than renumbered, the PRD's four v1 gates
(§4.7 `FR-45`–`FR-48`) became FR67–FR70 and **Epic 12** because the backlog had no story for them, its
`NFR-10` and `NFR-13`–`NFR-17` became NFR11–NFR16 with binding rows, and the six open questions the
backlog lacked are pointed at the PRD's §18. One PRD figure the repository contradicts is recorded
there: PRD NFR-5 states `169 @Test` over 26 test files; the tree holds 169 across 25 files containing
`@Test`. Net effect: 12 epics, 80 stories, 70 FRs, 16 NFRs, tracker regenerated clean.

## Concurrency note (found during parent verification)

`_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/` did **not** exist when
`epics.md` was generated at 04:43. It appeared at 04:47–04:51, while this review was running, holding
`extract-estonia-alignment.md`, `extract-innovation-assessment.md`, `extract-shared-appendices.md`,
`extract-spec.md` and `landscape-extract.md`. That is a concurrent session writing a PRD into the same
workspace. It makes three statements in `epics.md` stale — the readiness gate's claim that no PRD exists,
the `inputDocuments` list, and (once the PRD lands) the 'no architecture decision record' note.
