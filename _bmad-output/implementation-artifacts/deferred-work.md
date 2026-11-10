
## Deferred from: code review (2026-11-10)

Code review of story 2-1 (commit 9d9f12a, carrier runner).

- Archived spec `spec-2-1-carrier-runner-over-existing-cases.md` has `status: 'done'`, but the story is `review`; the spec's Code Map lists `direct_post.jwt` for cases 6-11 while `tests/e2e.sh` records case 10 as `direct_post`. The file is not on HEAD (retired in 06fc1b6), so fix it only if the archive is revived. Pre-existing.
- Final-code E2E evidence is missing: cases 12-13 and the last response/report edits were not re-run. Settle with `make e2e` before marking 2-1 in-repo complete.
