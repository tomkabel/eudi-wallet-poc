---
title: 'Sprint change proposal: PID-authenticated issuance of a representation attestation'
skill: bmad-correct-course
date: 2026-11-06
mode: batch
scope: moderate
status: approved and applied (autonomous run; no user answer existed, see decision log)
inputs:
  - _bmad-output/planning-artifacts/derived-credentials/forge-brief.md
  - _bmad-output/planning-artifacts/derived-credentials/recon-summary.md
---

# Sprint change proposal: PID-authenticated issuance of a representation attestation

## 1. Issue summary

**Trigger.** Not a story in flight. The trigger is a pressure-tested idea: a post claimed that a business can verify a holder's PID and then issue its own derived attestation, which relying parties would pay to verify. `bmad-forge-idea` (`derived-credentials/forge-brief.md`) found the commercial thesis mostly fails. Only one case is strictly better than presenting the PID: a fact the PID cannot carry, such as a mandate or role, presented without name or birth date. `bmad-deep-recon` (`derived-credentials/recon-summary.md`, 2026-11-06) tested that case and supersedes the forge brief's story shape.

**Category.** New requirement emerging from research. It is not a failure of existing work.

**Problem statement.** Spec §11.5 ("The two protocols nest") says an issuer may authenticate the holder inside the OpenID4VCI authorization step by requesting an OpenID4VP presentation of the PID. No story exercises this, so the claim is untested. Separately, spec §8 lists "Powers and mandates (Pääsuke)" as a P2 PuB-EAA from RIA, and the recon found no public source for that row.

**Evidence (recon-summary.md; identifiers re-checked this run with `gh api`).**

- OpenID4VCI 1.0 Final (16 Sep 2025) leaves the issuer's authentication method open. Presentation during issuance appears only as non-normative Appendix H.5. The Interactive Authorization Endpoint sits in the unfinished 1.1 draft [1][2][3].
- The Commission reference issuer `eudi-srv-web-issuing-eudiw-py` already runs the 1.0-compatible variant: an OpenID4VP presentation of the PID nested in authorization, configured per credential through `auth_method_supported_credencials` → `PID_login` and `dynamic_issuing` [9][10][13]. Checked: `api_docs/add_credential.md` names both keys. The latest release is v0.11.0, published 2026-11-04.
- It ships `eu.europa.ec.eudi.por_mdoc` (doctype `eu.europa.ec.eudi.por.1`) with no holder name or birth date [12]. Checked: `por_mdoc.json` claims are `legal_person_identifier`, `legal_name`, `full_powers`, `eService`, `effective_from_date`, `effective_until_date` and the issuance metadata. The issuer label is "Test QEAA Issuer".
- This wallet pins `eudi-lib-jvm-openid4vci-kt` 0.6.1 (`gradle/libs.versions.toml:36`). That release (2025-01-14) targets draft 14, and alignment with 1.0 arrived in v0.9.0 (2025-09-30) [6][7]. Both dates were checked. `OpenId4VCIManager.kt:49` hard-codes `credentialConfigurationIds` to `[MDL]`.
- Pääsuke is an online authorisation oracle, not an issuer [23][24]. RIA's wallet Phase III analysis and wallet page do not mention it [25][45] `[UNVERIFIED: absence]`.

## 2. Impact analysis

**Epic impact.** Epic 10 gains one story, 10.6, and can still be completed as planned. No other epic changes. Epics 1 and 2 and every in-progress story (1-3, 1-6, 2-1, 2-2) are untouched. No epic becomes obsolete, and no new epic is needed.

**Story impact.**

- New Story 10.6 depends on Stories 10.1, 4.1 and 10.4, all in backlog. Their text is unchanged.
- The recon dropped the Story 3.1 gate that the forge brief proposed: the reference issuer delegates the nested presentation to its own configured verifier (`dynamic_presentation_url`) [10]. Re-add the gate only if this repository's verifier is wired in.

**Artifact conflicts.**

- *PRD.* NFR16 (PRD NFR-17) treats a new attestation as a PRD scope decision. The PRD exists only on the read-only branch `archive/bmad-output-2026-11-04`, so it cannot be edited (AGENTS.md). The decision is recorded in epics.md's Overview ("Extended 2026-11-06") and in this proposal. PRD NG-12 ("no real issuer, no production issuance") still holds: the issuer is self-hosted test software, and the attestation is a labelled mock.
- *Architecture.* No `bmad-architecture` record exists, and `docs/ARCHITECTURE.md` is unaffected. Story 10.6 touches `OpenId4VCIManager` configuration and the issuance re-entry path. Story 10.1 owns any library upgrade.
- *UX.* No UX spine exists (epics.md "UX Design Requirements"). The mock label is an acceptance criterion of 10.6.
- *Spec.* The §8 Pääsuke row needs `[UNVERIFIED]`, and §23 collects such items, so a new item 37 is added.

**Technical impact.**

- The story needs a self-hosted reference issuer at a pinned revision for its test environment.
- It may force the 0.6.1 → ≥0.9.0 library upgrade, which Story 10.1 decides.
- No CI, deployment or golden-vector impact until the story is implemented.

## 3. Recommended approach

**Direct Adjustment.** Add Story 10.6 within Epic 10 and annotate spec §8 and §23.

- **Effort:** low for the backlog change. The story itself is medium, plus the upgrade risk 10.1 may raise.
- **Risk:** low. It is additive, revertible in one commit, and no sprint reordering is needed.
- **Timeline:** none. The story enters as backlog behind 10.1.

Alternatives considered:

- **Park** (the recon's runner-up) is right only if 10.1 stays blocked and self-hosting is ruled out. The story's Blocked-by line keeps that exit open.
- **MVP review** and **rollback** do not apply: nothing completed conflicts with the change.

Known limits the story records rather than solves:

- No cryptographic link between the presented PID and the attestation key (OIDF #742 for the 1.1 draft; for the 1.0 variant this is an inference).
- The authorization page and the presentation return URL must share one user agent (OIDF #730, variant 2).

## 4. Detailed change proposals

### 4.1 Stories (`_bmad-output/planning-artifacts/epics.md`)

```
Story: [10.6] PID-authenticated issuance of a representation attestation
Section: new story after 10.5

OLD: (none)

NEW: user story + seven acceptance criteria:
  - nested PID presentation re-enters the wallet mid-issuance; issuance
    completes from persisted authorization state with por.1 stored
  - over-asking issuer refused fail-closed via Story 4.1 IntendedUseSource;
    issuance aborted, key released
  - stored attestation discloses no family_name / birth_date
  - revoked por.1 refused at the verifier via Story 10.4
  - attestation labelled mock (user-entered values, "Test QEAA Issuer",
    no Art. 45f / PuB-EAA claim)
  - record states the two limits (#742, #730 variant 2)
  - credentialConfigurationIds and issuer URL configurable
  Depends on: 10.1, 4.1, 10.4. Blocked by: 10.1. Covers: FR80.

Rationale: recon-summary.md "Proposed Story 10.6" and "Dependencies to add".
```

Supporting edits in the same file:

| Section | OLD | NEW |
|---|---|---|
| Epics table, row 10 | source `L4, S2, S9, S11`; stories `10.1–10.5` | source adds `derived-credentials recon 2026-11-06`; stories `10.1–10.6` |
| Functional Requirements | ends at FR79 | adds FR80 (nested PID presentation during issuance; mock `por.1`, no name/birth date; over-asking issuer refused) |
| Coverage Map | ends at FR79 → 14.2 | adds FR80 → 10.6 |
| NFR binding, NFR3 | `3.3, 4.1, 4.5, 4.7, 5.2, 5.3` | adds `10.6` (its refusal case) |
| Overview | "Extended 2026-11-04" paragraph | adds "Extended 2026-11-06" paragraph with the NFR16 scope record |

### 4.2 Sprint tracking (`_bmad-output/implementation-artifacts/sprint-status.yaml`)

- Add `10-6-pid-authenticated-issuance-of-a-representation-attestation: backlog` under `epic-10`, after 10-5.
- Set `last_updated` to `11-06-2026 00:00`.
- Epic status, order and every other key are unchanged.

### 4.3 Specification (`spec/EE-EUDIW-TS-1.0.md`)

| Section | OLD | NEW |
|---|---|---|
| §8 row | `Powers and mandates (Pääsuke)` | `Powers and mandates (Pääsuke) **[UNVERIFIED]** (§23 item 37)` |
| §23 | items 1–36 | adds item 37: Pääsuke is an oracle, not an issuer; RIA's wallet documents are silent; representation is heading to business wallets (COM(2025) 838 final, Arts. 3(18)–(19), 5(1)(f); procedure 2025/0358(COD) in trilogue); ARF 3.0.0 removed legal-person wallet units, and Topic 29 `RP_01`/`RP_02` cover only natural-to-natural representation. `[UNVERIFIED]` |

No version bump or "Changes in" row was added: this is an annotation, not a requirement change, and the requirement count stays 211. If the maintainer wants every spec edit to carry a revision row, that is a follow-up.

### 4.4 PRD, Architecture, UX

None. See §2.

## 5. Implementation handoff

**Scope: Moderate.** It is a backlog addition with cross-story dependencies.

- **PO/SM (sprint planning):** sequence 10.6 behind 10.1. Do not pull it forward while 10.1 is blocked. Re-check the recon's staleness map; the earliest date is 2026-12-01 for the business-wallet trilogue status.
- **Developer (when 10.1 lands):** create the story file, pin the reference issuer revision, and settle whether 0.6.1 interoperates. The open questions are carried in recon-summary.md "Open questions".
- **Maintainer:** decide whether spec annotations need a revision row.

**Success criteria.**

- epics.md and sprint-status.yaml agree on 10.6.
- FR80 maps to 10.6.
- Spec §8 and §23 cross-reference each other.
- `make check` passes.
- The story's claims trace to recon-summary.md sources [1]–[13].

## 6. Checklist record

| Item | Status | Note |
|---|---|---|
| 1.1 trigger | [x] | forge brief → recon, no in-flight story |
| 1.2 problem | [x] | new requirement from research |
| 1.3 evidence | [x] | recon sources; identifiers re-checked this run |
| 2.1 current epic | [x] | Epic 10 completes as planned plus 10.6 |
| 2.2 epic changes | [x] | add one story |
| 2.3 remaining epics | [x] | dependencies on 4.1 and 10.4 only; 3.1 gate dropped |
| 2.4 obsolete/new epics | [N/A] | none |
| 2.5 reorder | [N/A] | not reordered (constraint) |
| 3.1 PRD | [!] | archived read-only; NFR16 record placed in epics.md |
| 3.2 architecture | [N/A] | no architecture record |
| 3.3 UX | [N/A] | no UX spine; mock label in AC |
| 3.4 other artifacts | [x] | spec §8/§23 |
| 4.1 direct adjustment | Viable | low effort, low risk |
| 4.2 rollback | Not viable | nothing to roll back |
| 4.3 MVP review | Not viable | MVP unaffected |
| 4.4 selected | [x] | Direct Adjustment |
| 5.1–5.5 | [x] | sections 1–5 above |
| 6.1–6.2 review | [x] | judge results below |
| 6.3 approval | [x] | pre-authorised tiltrank pick (C7), not a user answer |
| 6.4 sprint-status | [x] | 10-6 added |
| 6.5 handoff | [x] | section 5 |

## 7. Decision log

The run was autonomous and the user was unavailable. Every fork went through `tiltrank`; pre-authorised by the orchestrator. Payloads are in `derived-credentials/decisions/C1.json`–`C7.json`, and records are appended to `derived-credentials/.decisions.log` (lines 11–17). No pick diverged from the recommendation, and none escalated.

| # | Question | Options | Winner | Band / separation | Why |
|---|---|---|---|---|---|
| C1 | Mode | batch; incremental | Batch | Separated / robust | No one to approve each edit, so incremental adds nothing |
| C2 | Path forward | direct adjustment; park; MVP review; rollback | Direct adjustment | Separated / robust | The recon recommends it; park stays the documented exit |
| C3 | FR handling | new FR80 + coverage row; map under FR55; no FR | FR80 → 10.6 | Separated / robust (closest of the run, with C5) | Every story has its own FR; FR55 is "tested against the Commission's issuer", which 10.6 extends but does not restate |
| C4 | NFR16 record (PRD archived) | record in epics.md + proposal; treat as fixture; edit archive (excluded: read-only) | Record in epics.md + proposal | Separated / robust | The label said "Sequencing"; it was placed in the Overview "Extended" paragraph beside the 2026-11-04 precedent instead. This corrects line 14 of `.decisions.log` |
| C5 | Scope class | moderate; minor; major | Moderate | Separated / robust | Cross-story dependencies make it more than a direct edit |
| C6 | Proposal path | skill default `planning-artifacts/sprint-change-proposal-2026-11-06.md`; `derived-credentials/sprint-change-proposal.md`; both | Skill default | Separated / robust, lowest stability of the run | The orchestrator deferred to the skill's configured path. Runner-up: co-locating with the inputs in `derived-credentials/` |
| C7 | Approval (steps 3–5) | approve; drop §23 entry; hold 10.6 until 10.1 | Approve | Separated / robust | Matches the requested change; holding is already encoded as "Blocked by: 10.1" |
