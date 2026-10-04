---
version: 1
slug: "sandbox-static-index-html"
primary_target: "sandbox/static/index.html"
related_targets: []
---

Scope: sandbox/static/index.html — the guided ZK age-proof sandbox. Mode: Operate, narrated (the visitor completes a task while being told what each step means).

Audience: reviewers, relying parties, curious citizens; no phone, no spec. Job: register a test identity, get the credential, prove age to a shop, see what the shop learned.

Constraints: real backend (sandbox/server.py); ET/EN toggle; WCAG 2.1 AA; independence notice on the page. Visual world pinned by the user: ria.ee / TEDI. No concept roll, because the brief pinned the world.

## Direction contract

THESIS: a state e-service that walks one citizen through one transaction, step by step, the way eesti.ee and ria.ee services do. Refuses the crypto-demo default of a dark terminal and glowing nodes.

OWN-WORLD: TEDI tokens. White ground, #005aa3 brand, #151926 text, #e1e2e5 hairlines, 4px card radius, pill buttons, Roboto 300 headings, Material Symbols Outlined. A blue utility bar on top, as on ria.ee.

STORY: you register, your wallet holds a yes/no, the shop asks, you consent, a real proof runs, the shop learns one bit. Then try to cheat, and watch each attempt fail for real.

FIRST VIEWPORT: the utility bar and white header with the wordmark and ET/EN. The H1 "Prove you're over 18. Keep everything else." sits in a left 7/12 column with the lead and a primary "Start" pill. The right 5/12 holds the cast of four actors as a vertical ledger: you, register, wallet, shop. The independence notice is an info alert under the header.

FORM: a TEDI vertical stepper with a sticky rail. The signature interaction is the live proof timeline in step 5: phases fill as the real prover's output streams, with an elapsed counter and the measured byte/second figures. It closes with a two-column ledger of "what the shop learned / what it never saw". Seed: pinned-by-brief (no roll).

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
