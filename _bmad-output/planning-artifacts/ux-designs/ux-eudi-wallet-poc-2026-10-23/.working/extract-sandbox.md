# Extract — sandbox surface (shipped) — working notes for the v1 UX run

Sources read for this extract (all read in full; no screenshots opened):

| # | File | Lines | What it is |
|---|---|---|---|
| 1 | `docs/CONFORMITY.md` | 202 | The wallet's conformance note (`EE-ZKP-003` statement, `EE-ZKP-060` inventory, build provenance, status table) |
| 2 | `sandbox/static/index.html` | 332 | The shipped sandbox page (Estonian is the markup language) |
| 3 | `sandbox/static/app.js` | 511 | All behaviour, all English strings, all runtime-built strings |
| 4 | `sandbox/static/app.css` | 403 | TEDI tokens resolved, every visual state |
| 5 | `sandbox/server.py` | 468 | Backend the page actually drives (not named in the task brief; read to establish the failure paths and where numbers come from) |
| 6 | `DESIGN.md` (repo root) | 303 | Canonical design authority for this surface |
| 7 | `.impeccable/design.json` | 122 | Machine form of 6 (`schemaVersion: 2`) |
| 8 | `.impeccable/surfaces/sandbox-static-index-html.md` | 26 | The surface contract (direction contract) |
| 9 | `sandbox/check.py`, `sandbox/deploy/{deploy.sh,Dockerfile,compose.yml}`, `Makefile:67-69` | — | Read only to answer "does a not-built state exist anywhere?" |

Supporting reads (not extract sources, used only to name the requirement IDs correctly): `_bmad-output/planning-artifacts/prds/prd-eudi-wallet-poc-2026-10-23/prd.md` FR-6 (453-469), FR-26 (743-751), FR-30 (796-806), FR-31 (808-816), FR-32 (818-826), FR-33 (828-834), FR-34 (836-845), FR-35 (847-857).

Screenshots exist at `.impeccable/review/` (`desktop.png`, `desktop-top.png`, `first-viewport.png`, `live.png`, `mobile.png`, `m-top.png`, `s2.png`–`s6.png`, `m-s2/m-s3/m-s4/m-s6.png`). Not opened, per the brief. The surface contract is the text source.

---

## (a) `docs/CONFORMITY.md` — the rows that describe what is visible on a screen

The note is about the **Android wallet and the specification**, not about the sandbox web page. Only one row names the sandbox at all (row A below). Everything else here describes the Android wallet's on-screen behaviour, which is the nearest thing to a refusal/state vocabulary that exists in the repo — and is where the FR-6/FR-26 vocabulary is already worked out.

### Screen-visible rows

**A. `EE-POA-027/028` — predicate timing, incomplete dates of birth — `CONFORMITY.md:180`**
> "Binds the Provider; **the fixtures follow it.** The Kotlin mock computes `age_over_18` from the mock register's date of birth on the Europe/Tallinn day, with a 29 February birthday on 28 February (`AgeReckoning`); **the sandbox issuer does the same (`age_on`, `register_today`)**."

The only CONFORMITY row that touches the sandbox. It is an implementation statement, not a screen statement — but it is the rule that makes the sandbox's displayed age match law (TsUS 135(1)/136(2)/136(4), cited at `server.py:32-34`), and it is the sandbox's one conformity anchor.

**B. `EE-PRO-001` response encryption — `CONFORMITY.md:166`. The refusal-cause pattern the run needs already exists here, in the wallet.**
> "`PresentationRequestViewModel` refuses before the consent screen, and `OpenId4VPManager.sendResponse` refuses again before any document leaves (a typed `ResponseRefused`, **shown as the unencrypted-request message, not a retryable network error**). … **the user sees a dedicated message at once**, and the refusal is written to the activity log for each requested document type … The verifier is sent a negative answer in the background, from the application scope **so closing the error screen does not cancel it**."

Elements: refusal is typed; refusal has its own message; refusal is explicitly distinguished from a retryable error; refusal is logged; the negative answer survives the user dismissing the screen. The sandbox implements none of this typing.

**C. `EE-PRO-010` channel indication — `CONFORMITY.md:170`. The consent screen labels the channel.**
> "The consent screen names the channel: a QR scanned by the wallet is labelled cross-device with no proximity check; a deep link is labelled as a link, with the warning that a camera-scanned link may also be cross-device — **the wallet cannot tell those apart, so it does not claim "same-device"**."

Precedent for the shape "label the tier, state the limit of what you know".

**D. `EE-POA-013` last attestation (rev 1.6) — `CONFORMITY.md:177`. Refusal-before-consent plus a named notice.**
> "A device that cannot prove **shows the last-attestation notice with the EE-ZKP-042 notice**, and **tapping share is the approval**; the row is logged linkable"

**E. `EE-POA-031` `false` predicates withheld — `CONFORMITY.md:183`**
> "A requested predicate held `false` is never returned: **the user is told**, and the relying party gets the no-match answer (OpenID4VP: nothing sent; DC API: `RESULT_CANCELED`; proximity: session terminated)."

Two-sided statement: what the user sees, and what the counterparty receives. Directly relevant to the sandbox's `refused_p` copy, which also states the counterparty got nothing (`app.js:107`/`147`).

**F. `EE-ZKP-055a` unauthenticated requests — `CONFORMITY.md:188`** — "refused before consent"; no access-CA anchors are configured, so every DC API and proximity request is unauthenticated.

**G. `EE-PRO-007` SIOPv2 — `CONFORMITY.md:168`** — "Refused before consent".

**H. `EE-PRO-024` key binding — `CONFORMITY.md:173`** — "Deferred issuance **fails with a stated reason**; **both refusals show the user why**".

**I. `EE-PRO-025` `readerAuth` — `CONFORMITY.md:190`** — "the consent screen shows the subject **only on a pass**"; no anchors configured, so no request counts as authenticated.

**J. `EE-PRO-026` `request_uri` lifetime — `CONFORMITY.md:191`** — reference verifier: 3-minute default session TTL; "the wallet's **'not answered' notice not assessed**" — an unbuilt state named as unassessed.

**K. `EE-POA-025` re-issuance 14 days before expiry — `CONFORMITY.md:178` and `EE-POA-029` birthday refresh — `CONFORMITY.md:181`** — "My documents offers the age attestations again from that day"; "renewal is offered from that day".

**L. `EE-POA-030` one predicate per presentation — `CONFORMITY.md:182`** — a request naming more than one `age_over_NN` is refused; a claim-less DCQL query for an age doctype requests `age_over_18` only.

### Rows that constrain what may be said on screen (not screen behaviour, but binding on copy)

- `CONFORMITY.md:14-27` — "**This wallet does not assert unqualified `ZKP_08` compliance, and must not be assessed as claiming one**"; the reading applied is declared because "an assessment that leaves it implicit cannot be reproduced".
- `CONFORMITY.md:43-54` — the bundled v7 circuits carry a **109-bit soundness error** against the ACM v2.0 recommended "at least 125 bits"; "this wallet's ZK path is a **pilot-grade mechanism**: the shortfall is declared here, and **the wallet SHALL NOT be advertised as meeting the ACM's recommended level**."
- `CONFORMITY.md:70-92` — what the wallet **can** claim today (ZK age proof over `ee.riik.poa.1` / `eu.europa.ec.av.1`; fixture-verified, "not yet exercised end to end on a device"; the host verify budget is "**honestly re-derived rather than met**", p95 3.2–4.9 s on an i5-8365U-class host).
- `CONFORMITY.md:93-119` — what it **cannot**: "**No OpenID4VP ZK carrier exists**" in the standard; the `mso_mdoc_zk` carrier is "a citable reference, **not a standard**"; "**The ISO Annex C ZK path has no on-device evidence**" — PENDING-DEVICE.
- `CONFORMITY.md:120-125` — **no unobservability (`ZKP_07`) claim**.
- `CONFORMITY.md:129-147` — "**Every APK this repository can produce is debug-derived**"; `EE-CNF-005`, `EE-ZKP-002`.
- `CONFORMITY.md:158` — "On-device evidence: **None**".

**Absent in `CONFORMITY.md`:** any row describing the sandbox page's screen, any sandbox requirement identifier, any sandbox accessibility or bilingual statement, and any sandbox failure-mode statement. The word "sandbox" appears exactly once in the file (`:180`).

---

## (b) The sandbox as implemented

### b.1 The six steps

The six labels and their short rail forms live in the markup, not in a data structure:

| Step | `<h2>` (ET) | Rail short form | Anchor | Step section |
|---|---|---|---|---|
| 1 | Ütle rahvastikuregistrile, kes sa oled | Registreeru | `#step-1` | `index.html:84-117` |
| 2 | Rahakott saab vanusetõendi | Saa tunnistus | `#step-2` | `index.html:120-155` |
| 3 | Veinikelder peab su vanust kontrollima | Mine poodi | `#step-3` | `index.html:158-185` |
| 4 | Rahakott küsib sinult luba | Anna nõusolek | `#step-4` | `index.html:188-219` |
| 5 | Rahakott arvutab nullteadmustõestuse | Tõend arvutatakse | `#step-5` | `index.html:222-246` |
| 6 | Mida pood teada sai | Mida pood teada sai | `#step-6` | `index.html:249-273` |

Rail markup `index.html:70-80`; EN short forms `app.js:20-21`; nav shortcuts in the masthead point at `#journey`, `#cheat`, `#limits` (`index.html:34-36`).

How the journey advances: one function, `go(n)` (`app.js:203-224`). It sets every `.step` to `done` / `active` / `locked` (`app.js:207`), mirrors that on the rail items with `aria-current` and `tabIndex` (`app.js:210-216`), calls `summarise(i)` for every step below `n` (`app.js:217`, `226-239`), updates the mobile counter (`app.js:218`), then focuses and scrolls the target step — smooth, or `auto` under reduced motion (`app.js:219-223`). Transitions:

- Step 1 → 2 after `POST /api/enrol` resolves (`app.js:296-300`).
- Step 2 → 3 by the "Go to the shop" button (`app.js:326`).
- Step 3 → 4 after `/api/shop/start` **and** `/api/wallet/fetch` resolve (`app.js:329-341`).
- Step 4 → 5 on "Share the proof", which also starts `runProof()` (`app.js:357`).
- Step 5 → 6 by a 700 ms `setTimeout` after the stream's `done` event (`app.js:411`), which also renders the result and unlocks the cheat band.
- Restart ("Start over with a new person", `app.js:446-452`) resets the form, hides every `.step__summary`, clears holder/cred/consent/session/run, `go(1)`, focuses the first field. It does **not** re-lock the cheat band.

Steps 1–5 each carry a hidden `.step__summary` line (`index.html:116`, `154`, `184`, `218`, `245`) that becomes a green check-circle sentence once passed (`app.js:226-239`; strings `sum1`–`sum5`, `sum5_fail` at `app.js:92-97`/`132-137`). Step 6 has no summary element.

Numbers in the step copy are substituted live: `sum1` uses the visitor's name and computed age; `sum2` uses the predicate value; `sum5` uses the elapsed seconds (`app.js:229-235`). `sum5_fail` ("The wallet could not compute a proof") is the failure variant of step 5's summary.

### b.2 The proof timeline (signature component)

Markup `index.html:229-243`, CSS `app.css:268-296`, behaviour `app.js:359-417`.

- Five phases, in fixed order: `request` → `bind` → `prove` → `send` → `verify` (`app.js:360`; markup `index.html:232-236`). Labels ET `index.html:232-236`, EN `app.js:50-52`.
- **Pre-rendered in the markup** as empty marks with no `data-state`, in the DOM before any proof runs.
- `phase(name, state, detail)` (`app.js:361-366`) sets `data-state` on the `<li>`, writes the per-phase detail text, and — for `done`/`failed` only — announces to the sighted-hidden live region (`app.js:365`, region at `index.html:238`).
- Phase detail is parsed out of the prover's own stdout, line by line, over SSE (`app.js:394-402`): `request:` → the request line; `transcript: N bytes, sha256 X` → `SessionTranscript N B · sha256 X`; `PROVE: N bytes in S s` → the `proved` string (`app.js:103`/`143`); `response: HTTP nnn` → `direct_post.jwt · HTTP nnn`.
- The clock: `#proof-time` counts from `performance.now()` on a 100 ms interval (`app.js:381-385`, `392`), rendered through `fmt(s, 1)`, and finally set to the response timestamp rather than the wall time the page waited (`app.js:409`).
- The raw log: a `<details class="log">` disclosure (`index.html:239-242`) appended to line by line (`app.js:396`), monospace, `app.css:294-296`.
- Visual states (`app.css:280-291`): **pending** = hollow 24 px circle, `--border-secondary`; **active** = blue border with the right edge transparent plus `spin .9s linear infinite`, and the phase's `.phase__note` becomes visible (only the `prove` phase has one, `index.html:234`); **done** = filled State Blue with a white check SVG; **failed** = filled Refuse Red with a white cross. The spine between phases fills by `scaleY(0 → 1)` over 0.6 s per completed phase (`app.css:275-279`).
- Failure mapping (`app.js:405-408`, `412-416`): verifier says invalid → whichever phase was reached gets `failed` with the verifier's detail; `prover_refused` truthy → the **`prove`** phase is marked failed with the literal detail `age_over_18 = false`; a stream/network throw → the currently-active phase gets `failed` with `errText(x)`; in the catch path no verdict is rendered and step 6 is not reached.
- The elapsed clock is `aria-hidden="true"` (`index.html:230`) — it is never announced. Only phase completion/failure and the queue notice reach assistive tech.

### b.3 The ledger and the other step-6 components

- **Verdict** (`index.html:255`, `app.js:425-434`, `app.css:299-307`): a `role="status"` region, class `verdict--ok` or `verdict--no`, icon `verified_user` or `gpp_bad`, a bold headline (`ok_h` / `no_h`) and one paragraph of detail (`ok_p(detail)` / `refused_p` / `invalid_p(detail)`).
- **Ledger** (`index.html:256-265`, `app.js:435-436`, `app.css:308-319`): two columns — "Pood sai teada / The shop learned" on white, "Jäi sinu teada / Stayed with you" on Grey Paper. Each item is a bold line plus a `<small>` caption (`app.js:420-424`). The learned column is three content rows keyed off `ok` (`learned` vs `learned_none`, `app.js:109-110`/`149-150`); the kept column is five rows and interpolates the visitor's name (`app.js:111`/`151`).
- **Figures strip** (`index.html:266-267`, `app.js:437-444`, `app.css:320-325`): up to four cells — proof size, proof computation, shop's check, total. Built conditionally: size and computation only if `run.prove` exists; the shop's check only if `run.verifyS != null`; total always. Under it, `figures_note`: "Need arvud on mõõdetud just praegu, sinu enda läbimise käigus." / "These figures were measured just now, during your own run." (`index.html:267`, `app.js:56`/`152`).
- **Step-6 CTA row** (`index.html:268-271`): "Proovi nüüd petta" (anchor to `#cheat`) and "Alusta uue isikuga" (restart).

### b.4 EVERY user-visible state that exists today, and where each renders

**Step-level and rail states**

| State | Where set | How it looks | File:line |
|---|---|---|---|
| `active` | `go()` | State Blue border + the one shadow; body visible; body rises 8 px over .5 s *only* under `no-preference` | `app.js:207`; `app.css:177`, `186-189` |
| `locked` | `go()` | Grey Paper ground, Light Slate heading, **body `display:none`** | `app.js:207`; `app.css:178-181` |
| `done` | `go()` | Collapsed head, grey heading, body hidden, green check-circle summary line beneath | `app.js:207`, `226-239`; `app.css:181-183` |
| rail `active` / `done` / `locked` | `go()` | dot ring + halo / filled blue dot / non-interactive; mobile: a 4 px segment bar | `app.js:210-216`; `app.css:160-164`, `369-372` |
| "Step n of 6" counter | `go()`, `rerender()` | text only under 1000 px, `aria-hidden` | `app.js:194`, `218`; `app.css:150`, `365` |

**Form and backend states**

| State | Trigger | Renders as | File:line |
|---|---|---|---|
| Name missing | submit | field `aria-invalid` + refuse wash; `#enrol-error` alert with icon + "Enter a first and last name." | `app.js:292`, `260`; `index.html:112`; `app.css:203`, `206` |
| Birth date invalid/future/>130 yrs | submit or server `birth_date` | same, `err_birth` | `app.js:293`, `302` |
| Backend unreachable / 5xx (502 `backend`) | fetch throws, no code | `err_backend`: "The sandbox backend didn't answer. Check that the server is running and try again." | `app.js:88`/`128`, `255-258`; `server.py:386-388` |
| Rate limited (429 on enrol/shop) | `POST` refused | `err_rate`: "Too many attempts in a short time…" | `app.js:90`/`130`, `257`; `server.py:335-343` |
| Rate limited mid-stream | SSE `fail {error:"rate"}` | `err_rate` | `server.py:419-421` |
| Session expired / holder gone (404 `expired`) | fetch or SSE | `err_expired`: "The session expired. Start again at the shop." | `app.js:89`/`129`; `server.py:382-383`, `424-425` |
| Sandbox busy (queue > 4) | SSE `fail {error:"busy"}` | `err_busy`: "The sandbox is busy…" | `app.js:91`/`131`; `server.py:241-242`, `426-427` |
| Queued behind another proof | SSE `queued` | live region + the `request` phase detail both set to "Someone else is computing a proof right now. You're in the queue…" | `app.js:402`, `100`/`140`; `server.py:245` |
| Button busy | any `aria-busy="true"` submit | Fog fill, Light Slate text, `progress` cursor, label kept, button disabled | `app.js:294`, `331`, `483`; `app.css:138-139` |

**Consent (step 4)**

| State | Trigger | Renders as | File:line |
|---|---|---|---|
| Consent sheet populated | after `/api/wallet/fetch` | client id, doctype path + requested value, encryption tier, nonce (first 16 chars) | `app.js:344-350`; `index.html:195-216` |
| Encryption tier labelled from the real request | `response_mode` | `direct_post.jwt` + key curve, or "Unencrypted (direct_post)" | `app.js:98-99`/`138-139`, `348` |
| **Declined** | "Keeldu" / "Decline" | `#consent-declined` is unhidden and the journey returns to step 3, focusing the shop button | `app.js:351-356`; `index.html:215` |

**Proof-run and result states**

| State | Renders as | File:line |
|---|---|---|
| Phase pending / active / done / failed | §b.2 | `app.css:280-291` |
| Phase completion announced | live region gets "«phase» – tehtud." / "«phase»: done." | `app.js:365`, `101`/`141` |
| Queue announced | live region + request detail | `app.js:402` |
| Verdict PASS | `verdict--ok`, `verified_user`, "Vanus kinnitatud. Tellimust saab jätkata." + "Kontrollija vastus: …" | `app.js:428-434` |
| Verdict REFUSED — prover refused a false predicate | `verdict--no`, `gpp_bad`, "Vanust ei kinnitatud. Pood ei müü." + "Sinu tunnistuses on age_over_18 = false. Rahakott ei saa tõestada midagi, mis ei ole tõsi, ja pood ei saanud midagi." | `app.js:407`, `432` |
| Verdict REFUSED — verifier rejected the proof | same card, detail = "Kontrollija lükkas tõendi tagasi: «detail»" | `app.js:432` |
| Failed run with no verdict | only the failed phase carries `errText(x)`; step 6 stays locked | `app.js:412-416` |
| Ledger, learned column | 3 rows on pass; on non-pass "Mitte midagi / Rahakott ei saatnud ühtegi tõendit" | `app.js:435`, `109-110` |
| Figures | as §b.3 | `app.js:437-444` |

**Cheat band**

| State | Renders as | File:line |
|---|---|---|
| Locked | `#cheat-locked` hint visible, all three buttons `disabled` | `index.html:281`, `287`/`294`/`301`; `app.js:462-465` |
| Unlocked | hint hidden, buttons enabled | `app.js:411`, `462-465` |
| Running | "Käib… n s" / "Running… n s" ticking each 100 ms | `app.js:469-485`, `113`/`153` |
| Blocked | green `is-blocked` wash, shield icon, "Tõkestatud" + the outcome code | `app.js:475-478`, `114`/`154` |
| **Got through** | red `is-broken` wash, warning icon, "Läbi läks – see on viga" / "Got through – that's a bug" | `app.js:475-478`, `115`/`155` |
| Unexpected | plain text "Ootamatu vastus. Vaata serveri logi." | `app.js:495`, `119`/`159` |
| Attack error | plain `errText` text inside the same box | `app.js:496`, `473` |
| Outcome vocabulary per attack | replay: "First post: valid. Second: HTTP 410 …"; tamper and lie: "Verifier: valid:false – «detail»" | `app.js:492-493`, `116-118`/`156-158` |

### b.5 Refusal vs cancellation vs retryable error — what exists today

Three render paths exist; they are **not** systematically distinguishable, and the cancellation case does not reach the screen.

| Case | Which code path | What the visitor sees | Distinguishable? |
|---|---|---|---|
| Refusal (the wallet/prover refuses because the predicate is `false`) | `app.js:407` → `renderResult()` `app.js:426-434` | verdict card, refuse wash, `gpp_bad`, `no_h`, `refused_p` naming the cause `age_over_18 = false`, plus a failed `prove` phase carrying the same literal | Yes — causes named, own card, own colour semantics |
| Refusal (verifier rejects a valid-looking proof) | `app.js:408`, `432` | same card, `invalid_p` with the verifier's detail; failed phase detail = that detail | Yes, in shape; the two refusals share one card class |
| Retryable error (backend, rate, expired, busy, stream drop) | `showError()` `app.js:260` in steps 1 and 3, or a `failed` phase in step 5, or plain text in an attack box | a red message with an error icon, or a red phase detail — no verdict card, no result, no ledger | Partly: different container, but the same icon and the same refuse ink as a refusal's message |
| Cancellation (decline at consent) | `app.js:351-356` | `#consent-declined` is unhidden — but it sits inside `#step-4 .step__body`, and the same handler calls `go(3)`, which sets step 4 to `locked`; `app.css:181` hides `.step[data-state="locked"] .step__body`. **The declined message is set visible inside a `display:none` subtree and is never rendered.** Step 3 gains no message of its own, `summarise(4)` is not called on this path (`app.js:217` runs only `i < n` = 1,2), and nothing is announced | **No.** Cancellation is silent on screen and in the live region |

A second decline-related observation: `state.session`/`state.consent` are cleared (`app.js:353`) and the request file on the server is consumed once (`server.py:231`), so a "try again" is a fresh session — but the copy promises "you can try again if you like" (`app.js:47`/`215`) from a message that is not shown.

### b.6 The language toggle and how strings switch

- **Placement**: two buttons in the navy utility bar, `ET` / `EN`, `aria-pressed` on each, wrapped in `role="group"` (`index.html:20-23`). The group's `aria-label` is the hardcoded bilingual `"Keel / Language"` and has **no** `data-t-aria`, so it never switches (`index.html:20`). The button captions never switch either. The utility bar's own adaptive string is `utility` (`index.html:19`, `app.js:6`/`9`).
- **Markup default**: Estonian is the markup (`<html lang="et">`, `index.html:2`; all `data-t` nodes carry Estonian text).
- **Boot**: the Estonian dictionary is built by reading the DOM once — `textContent`, or `innerHTML` when the key ends in `_html` or the node contains `<code>` (`app.js:505`). `[data-t-aria]` values are captured the same way under an `aria:` prefix (`app.js:506`).
- **First language shown**: `localStorage["lang"]`, else `navigator.language.startsWith("et") ? "et" : "en"` (`app.js:509-511`). Estonian is the markup default but **not necessarily the language a non-Estonian browser first sees** (relevant to FR-32's "Estonian is the default").
- **The English dictionary**: `EN`, `app.js:4-77` — flat keys, one per `data-t` node. The runtime strings (anything with a substituted value) are a second dictionary `D.et` / `D.en` (`app.js:80-161`) whose values may be functions: `step_of`, `sum1-5`, `enc`, `live_phase`, `bytes`, `proved`, `ok_p`, `invalid_p`, `kept`, `a_running`, `a_replay_ok`, `a_tamper_ok`, `a_lie_ok`.
- **`setLang(l)`** (`app.js:173-190`): sets `document.documentElement.lang`; walks every `[data-t]` node and writes `textContent`, or `innerHTML` when the key ends in `_html` or the string contains `<code>` (`app.js:179`); writes `[data-t-aria]` labels; updates `aria-pressed`; sets `document.title` and the meta description from `D`; persists to `localStorage`; then calls `rerender()`.
- **`rerender()`** (`app.js:192-200`) is the completeness guarantee for a switch mid-journey: the mobile counter, the credential block, the consent block, every `done` step's summary, the step-6 result, and each attack card's own `_render` are all rebuilt. Every machine value it re-renders has been kept in `state` (`app.js:170`), so nothing is lost by switching.
- **Number and date formatting follow the language**: `fmt` uses `Intl.NumberFormat("et-EE" | "en-GB")` (`app.js:167`) — decimal comma in Estonian ("0,0 s"), decimal point in English. Dates use `Intl.DateTimeFormat` with the same locale pair (`app.js:321`).
- **Not switched by the toggle** (written in markup, never keyed): the shop hostname `veinikelder.test` (`index.html:166`), the price `12,90 €` (`index.html:173`), the doctype `ee.riik.poa.1` in the credential head (`index.html:132`), the `ET`/`EN` captions, the language group's label, and the SVG bottle (`index.html:169`). The product name and meta line *are* keyed (`product`, `product_meta`).
- **Independence notice is keyed in both languages in all three places** (`notice_html`, `footer_html`, `utility`) — `index.html:19`, `45`, `327` against `app.js:6`, `9`, `75`.
- **Absent**: a string-parity check. `sandbox/check.py` drives the HTTP API end to end (it asserts the story, not the strings) and nothing in `Makefile` or `tests/` compares the two dictionaries, so a one-language string cannot currently fail the site's own check (FR-32's second consequence is not implemented).

### b.7 Numbers and metrics — the only-from-the-run-that-happened rule

Every measured figure on the page originates in a subprocess or a response the visitor's own click caused:

| Figure | Where it comes from | Rendered |
|---|---|---|
| Proof size (`fig_proof`) | the prover's `PROVE: N bytes in S s` line, parsed from the live stream | `app.js:400`, `438` |
| Proof computation (`fig_prove`) | same line's seconds | `app.js:400`, `438` |
| Shop's check (`fig_verify`) | `respondedAt − prove.at`, both event timestamps | `app.js:401`, `439` |
| Total (`fig_total`) | the `response: HTTP …` timestamp, falling back to the wall clock | `app.js:409`, `440` |
| The clock beside the timeline | `performance.now()` deltas every 100 ms | `app.js:381-385`, `392` |
| Credential size, validity window, issuer key prefix | regexes over the issuer CLI's stdout (`MSO N B, DeviceResponse N B`, `validity: … .. …`, `issuer pk x: …`) | `server.py:152-171`; `app.js:322-324` |
| The visitor's age | computed client-side from the typed date (`ageOf`) and displayed in step 1's summary | `app.js:280-283`, `230` |

The page states this rule in its own copy: `figures_note` — "These figures were measured just now, during your own run." (`index.html:267`). The credential's "Signed" tag is the **one status label that is not derived from a check**: it is markup, rendered unconditionally (`index.html:134`).

### b.8 Reduced motion

| Element | Under `prefers-reduced-motion: reduce` | File:line |
|---|---|---|
| Active step reveal (8 px rise, .5 s) | not applied at all — the animation lives inside `@media (prefers-reduced-motion: no-preference)` | `app.css:186-189` |
| The active proof phase mark (spinning arc) | `animation: none`, with `border-right-color: var(--brand)` and a static Blue Mist fill — **the proof mark goes static, as DESIGN.md claims** | `app.css:287`, `293` |
| Scroll after a step change | `scrollIntoView({behavior: "auto"})` instead of `"smooth"` | `app.js:222` |
| Everything else | **ungated**: `html { scroll-behavior: smooth }` (`app.css:51`) still smooth-scrolls every rail, masthead, intro "Start" and step-6 anchor jump; rail dot background/border/colour transitions (.3 s, `app.css:158`); the timeline spine `scaleY` fill (.6 s, `app.css:277`); the phase mark's background/border transition (.3 s, `app.css:280`); the 100 ms clock ticker and attack timer (`app.js:383`, `485`) update text rapidly but do not animate | — |

The live region is the reduced-motion-independent channel: phase completions and failures are announced as text (`app.js:365`), and the queue notice too (`app.js:402`).

### b.9 The independence notice

Present in **three** places, in both languages, as DESIGN.md requires (`DESIGN.md:295`):

1. Utility bar, short form: "Iseseisev arendusprojekt · ei ole RIA teenus" / "Independent development project · not an RIA service" — `index.html:19`, `app.js:6`/`9`.
2. Info alert under the masthead, long form: names the repository, states the project is not an application of RIA or the Potential consortium, that neither has endorsed it, that the people are invented and that data is deleted when the server stops — `index.html:43-46`, `app.js:9`.
3. Footer: independent fork of `open-eid/eudi-wallet-poc`, "Not affiliated with RIA, the Ministry of Justice and Digital Affairs, or any Estonian public authority", design follows TEDI — `index.html:327`, `app.js:75`.

The wordmark and favicon are bespoke (an "A"-like glyph on a State Blue tile, `index.html:10`, `30`). No RIA logo and no state coat of arms anywhere. The footer also links the technical specification (`index.html:328`, `app.js:76`).

### b.10 The not-built path — **absent on screen**

- `sandbox/server.py:444-446`: if the prover or the verifier binary is not executable, the process prints to **stderr** and exits: `sys.exit(f"{name} not found at {path}; run \`make deps\` or pass --{name}")`. The HTTP server never binds, so there is no page, no 5xx, no DOM, nothing to style.
- `sandbox/server.py:108-111`: if `zkverify` starts and then dies, the sandbox exits with `"zkverify did not start:\n"` plus the verifier log's last 15 lines — again stderr only.
- `sandbox/deploy/deploy.sh:26`: `[ -x "$bin" ] || { echo "missing $bin; run make deps" >&2; exit 1; }` — the build host, not the page. `sandbox/deploy/compose.yml` runs the sandbox with `restart: unless-stopped` and `read_only: true`, so the deployed container would crash-loop with a bare 502 at the tunnel rather than show a page.
- `sandbox/deploy/Dockerfile` also mentions `make deps`, in a comment only.
- The page has **no** not-built string, no probe of toolchain health, and no route for one: the only backend-health message is `err_backend` ("The sandbox backend didn't answer. Check that the server is running and try again.", `app.js:88`/`128`), reachable only when the server *is* up and a call fails, and framed as retryable.
- The second half of FR-34's obligation **is** structurally held: the timeline only advances from real SSE lines (`app.js:394-402`), and the verdict and figures only render after a `done` event (`app.js:404-411`, `198`). The five phase rows exist in the DOM before any run, but nothing in them claims a run happened.

So: `make deps` is named on stderr, in a shell script and in a Docker comment — **never on a page**.

### b.11 The three target requirements against what exists

**FR-30 — the visitor can try to cheat and watch it fail (existing surface, `#cheat`, `index.html:277-306`).**
Three attack cards, each with an icon, a title, an explanation of the attack, a `data-t="a_run"` button and an `aria-live="polite"` output box. The **tamper** card is `index.html:290-296` (`data-mode="tamper"`, icon `swap_horiz`, "A proof made for another session"): the wallet proves against the wrong nonce and the verifier recomputes the session itself. The other two are `replay` (same proof twice; expects HTTP 410) and `lie` (a 16-year-old forced past the prover; expects `valid:false`).
Mechanism: the client requests a fresh shop session, enrols a holder if one is not cached (`app.js:456-461`; the "lie" case enrols Karl), opens the SSE stream in that mode (`app.js:490`; modes map to prover flags at `server.py:237-238`), and prints the verifier's own answer. Blocked/broken/error rendering is `app.js:469-479` against `app.css:337-344`. One global `attackBusy` disables all three while any runs (`app.js:455`, `481-500`).
Locked state: the band is a full-bleed navy section that exists from first paint; the buttons are disabled and `#cheat-locked` reads "Katsed avanevad, kui oled teekonna läbinud." / "The attempts unlock once you finish the journey."
Gaps for the design run: there is **no attack-level explanation of what "wrong nonce" means at the moment of failure** beyond the verifier's machine detail string; the outcome vocabulary (`Blocked` / `Got through – that's a bug` / `Unexpected answer. Check the server log.`) is the whole state set; the tamper case is the only one whose failure mode the copy predicts; and no per-attack retry or cleared state exists — once a card has a result, `_render` keeps it until the page is reloaded, apart from the language toggle re-rendering the same result in the other language.

**FR-6 / FR-26 — refusal named, and distinguishable from cancellation and retryable error.** See §b.5. Present: two refusals that name their cause; one retryable-error family with four named variants; the raid tier label (`direct_post.jwt` vs unencrypted) derived from the fetched request. Absent: any typed distinction in the UI between refusal, cancellation and retryable error; any cancellation state at all on screen; any log/record of the refusal reason (the visitor's technical log shows the raw prover stdout, not a reason field).

**FR-34 — honest failure when the toolchain is missing.** See §b.10. Absent on this surface.

---

## (c) `DESIGN.md` — canonical authority for this surface

Root `DESIGN.md` is the shipped visual identity. For this run it stays canonical: no token renamed, no Do/Don't contradicted.

### The North Star, verbatim (`DESIGN.md:179-183`)

> **Creative North Star: "The Service Counter"**
>
> The page behaves like an Estonian state e-service: one citizen, one transaction, one step at a time, at a counter that tells you exactly what it is doing with your data. It is built on TEDI (TEHIK's design system), and its chrome is inferred from ria.ee: a navy utility bar, a white masthead, a calm white ground, and the state blue as the only accent that acts. Nothing glows and nothing pretends to be a terminal. The cryptography is shown as a public-service procedure the visitor can follow, not as spectacle.
>
> Density is moderate and forms are generous. The guide voice sits in grey secondary text; the actions sit in blue pills. The one moment the page raises its voice is the live proof: a vertical timeline that fills as the real prover reports, beside a large light-weight clock. The bilingual toggle (ET first, EN second) is part of the identity, not a utility.

Key Characteristics (`DESIGN.md:185-190`): white ground, navy chrome, one acting blue · Roboto with light-weight 300 display headings · pill buttons, 4 px cards, 8 px "sheets" for another party's screen · status is TEDI's alert triad (green / red / amber), each a wash plus a border plus dark ink · **every number shown comes from the run that just happened**.

### The journey as DESIGN.md describes it

DESIGN.md does **not** enumerate the six steps. Its journey statements are:
- `DESIGN.md:247` — "The journey is a 240px sticky step rail beside a single column of step cards. Under 1000px the rail becomes a sticky **"Step n of 6"** line with a six-segment progress bar, and every split collapses to one column."
- `DESIGN.md:284-285` — the proof timeline is a "vertical list of **five phases**… The spine fills downward (scaleY) as each phase completes. A 3.5rem light clock counts up beside it, and the raw log sits behind a disclosure."
- `DESIGN.md:287-288` — the ledger: "Two columns, "the shop learned" on white and "stayed with you" on Grey Paper… Under it, a figures strip in light 1.5rem numerals with hairline separators."

The six step names exist only in `index.html:73-78` and `app.js:20-21` (§b.1). The surface contract's STORY (`…md:20`) narrates them without numbering them.

### Do's and Don'ts, VERBATIM (`DESIGN.md:290-303`)

> ## Do's and Don'ts
>
> ### Do:
> - **Do** keep every claim on screen tied to this run: sizes, timings and verdicts come from the server's output, never from copy.
> - **Do** name the sandbox's shortcuts where they occur, in Slate narration, not in a footnote.
> - **Do** carry the independence notice in the utility bar, the info alert and the footer.
> - **Do** ship Estonian and English for every string, Estonian first.
>
> ### Don't:
> - **Don't** use RIA's logo, the state coat of arms, or "RIA" as a brand. This is not RIA's product.
> - **Don't** put an uppercase tracked label (an eyebrow) above a heading.
> - **Don't** add a thick coloured stripe on one side of a card. Status is a wash plus a 1px border.
> - **Don't** render cryptography as spectacle: no dark terminal grounds, no glowing nodes, no matrix rain.
> - **Don't** use monospace for anything that isn't a machine value.

The four named rules, verbatim (`DESIGN.md:220-222`, `243`, `253`):
> **The One Acting Blue Rule.** Only State Blue is clickable at rest. Navy, mist and haze are surfaces, never controls.
>
> **The Tinted Status Rule.** Status never colours a whole region. It is always a light wash, a 1px border in the status hue, and text in the dark status ink, as TEDI's alerts are.
>
> **The Mono-Is-Data Rule.** Monospace marks machine values (`ee.riik.poa.1`, `age_over_18`, hashes, the log) and nothing else. It is never decoration.
>
> **The One Lift Rule.** Only the active step is elevated. Nothing else casts a shadow at rest.

### Token NAMES by family (names only — new surfaces reference these; the frontmatter holds the values)

**Colors (27, `DESIGN.md:5-31`)** — grouped as DESIGN.md itself groups them (`DESIGN.md:196-218`):
- Primary: `state-blue`, `state-blue-deep`, `state-navy`
- Secondary: `blue-mist`, `blue-haze`, `blue-steel`
- Neutral: `ink`, `footer-text`, `slate`, `slate-light`, `paper`, `paper-grey`, `fog`, `hairline`, `pewter`, `input-stroke`, `cellar-plaster`
- Status — Pass: `pass-green`, `pass-wash`, `pass-border`; Refuse: `refuse-red`, `refuse-wash`, `refuse-border`; Caution: `caution-ink`, `caution-wash`, `caution-border`
- Also in the frontmatter, grouped under Status by use: `info-border` (`DESIGN.md:29`)

**Typography (16, `DESIGN.md:32-114`)**: `display`, `headline`, `title`, `body`, `lead`, `label`, `data`, `figure`, `figure-compact`, `clock`, `title-small`, `lead-compact`, `body-small`, `small`, `caption`, `icon`.

**Rounded (4, `DESIGN.md:115-119`)**: `hairline`, `default`, `sheet`, `pill`.

**Spacing (8, `DESIGN.md:120-128`)**: `s1`, `s2`, `s3`, `s4`, `s5`, `s6`, `s7`, `s8`.

**Components (7 groups, 10 entries, `DESIGN.md:129-172`)**: `button-primary` (+ `button-primary-hover`, `button-primary-active`); `button-secondary` (+ `button-secondary-hover`); `input`; `chip`; `step-card`; `credential-head`; `utility-bar`.

Naming detail worth carrying: in DESIGN.md prose the human names differ from the token names in places — "Deep State Blue" is `state-blue-deep`, "Grey Paper" is `paper-grey`, "Light Slate" is `slate-light`, "Pass/Refuse/Caution" is the ink member of each status family (`pass-green`, `refuse-red`, `caution-ink`).

### Crosswalk: shipped CSS variables → DESIGN.md token names (`app.css:10-48`)

The stylesheet resolves the frontmatter into CSS custom properties. Names differ, so new surfaces should go through the DESIGN.md names, not the CSS ones:

| `app.css` variable | DESIGN.md token |
|---|---|
| `--text-primary` | `ink` |
| `--text-secondary` | `slate` |
| `--text-tertiary` | `slate-light` |
| `--text-brand` | `state-blue` |
| `--surface-primary` | `paper` |
| `--surface-secondary` | `paper-grey` |
| `--surface-tertiary` | `fog` |
| `--surface-hover` | `blue-haze` |
| `--brand` | `state-blue` |
| `--brand-dark` | `state-blue-deep` |
| `--brand-darker` | `state-navy` |
| `--brand-tint` | `blue-mist` |
| `--brand-mid` | `blue-steel` |
| `--border-primary` | `hairline` |
| `--border-secondary` | `pewter` |
| `--input-border` | `input-stroke` |
| `--success-text` | `pass-green` |
| `--success-bg` | `pass-wash` |
| `--success-border` | `pass-border` |
| `--success-strong` | no DESIGN.md token — a second pass green (used for step-summary check icons, `app.css:176`) |
| `--danger-text` | `refuse-red` |
| `--danger-bg` | `refuse-wash` |
| `--danger-border` | `refuse-border` |
| `--warning-text` | `caution-ink` |
| `--warning-bg` | `caution-wash` |
| `--warning-border` | `caution-border` |
| `--info-bg` | (no frontmatter token) — the info alert's wash is `blue-mist` by value, `app.css:37` |
| `--info-border` | `info-border` |
| `--radius` | `rounded.default` |
| `--radius-lg` | `rounded.sheet` |
| `--pill` | `rounded.pill` |
| `--font`, `--mono` | typography `display`…`body` family, and `data` |
| `--shadow` | the one lift (`DESIGN.md:251`, `253`) |
| `--ease` | motion `ease-out-expo` (design.json) |
| `--wrap` | 1240 px container (`DESIGN.md:247`) |
| `--s1`…`--s8` | `spacing.s1`…`s8` |

Two colours live only in `app.css` and have no DESIGN.md token: the shop product panel `#f6efe9` is `cellar-plaster`, but the bottle SVG's wine colour `#4a1d2f` and the `.skip` link's white text are raw values (`app.css:69`, `243`, `index.html:169`). The `--success-strong` green `#308653` is a second pass green with no frontmatter entry.

---

## (d) `.impeccable/design.json`

- `schemaVersion: 2` (`:2`); `generatedAt: "2026-10-04T17:20:00+03:00"` (`:3`); `title: "Design System: ZK age-proof sandbox"` (`:4`).
- **JSON top-level content**: `schemaVersion`, `generatedAt`, `title`, `extensions`, `components`, `narrative`. There are **no** top-level `colors`, `typography`, `rounded`, `spacing` or `components.<token>` maps of the kind DESIGN.md's frontmatter carries — the token frontmatter is not mirrored here; `components` is a display-oriented array of seven entries, and the prose lives in `narrative`.
- **`extensions` holds five groups** (`:5-35`): `colorMeta`, `typographyMeta`, `shadows`, `motion`, `breakpoints`.

**Token group structure and tonal ramps**

| Group | Entries | Shape |
|---|---|---|
| `colorMeta` | 10 of DESIGN.md's 27 colours (`:7-16`) | `{ role, displayName, canonical, tonalRamp? }` — `role` ∈ `primary` \| `secondary` \| `neutral` \| `status` |
| `typographyMeta` | 4 of 16 (`:19-22`) | `{ displayName, purpose }` |
| `shadows` | 1 (`:25`) | `{ name: "step-lift", value, purpose }` |
| `motion` | 2 (`:28-29`) | `{ name: "ease-out-expo" }`, `{ name: "phase-spin" }` |
| `breakpoints` | 2 (`:32-33`) | `{ name: "compact" = 1000px }`, `{ name: "phone" = 640px }` |

Tonal ramps exist for **three** colours, each a nameless 8-entry array of hex strings — the steps have no identifiers, only index positions:

- `state-blue.tonalRamp` (`:7`), light-to-dark order **reversed**: it runs dark → light, `#001a33` … `#e7f0f6`; `canonical #005aa3` sits at index 4. Steps 0 and 1 (`#001a33`, `#002a52`) are **not** DESIGN.md colours.
- `state-navy.tonalRamp` (`:8`), same dark → light shape, `#001224` … `#e3ebf2`; canonical `#003662` at index 3. Steps 0, 1, 2 and 4-7 are not DESIGN.md colours.
- `ink.tonalRamp` (`:11`), light-value order dark → light, `#151926` … `#f9f9f9`; canonical at index 0. Steps 1-4 coincide with `slate`/`slate-light`/`pewter` values; index 6 is `hairline`, index 7 is `paper-grey`.
- `state-blue-deep`, `blue-steel`, `pewter`, `input-stroke`, and every status/`*-wash`/`*-border` except `caution-border` have **no** `colorMeta` entry and **no** ramp.

**Values included here only where the name alone is ambiguous:**
- `caution-border` (`:15`) — `displayName: "Caution"`, `canonical: "#ba830d"`. The one place where the display name and the token name disagree: the family is `caution-ink` / `caution-wash` / `caution-border`, but only the ink member's hue is recorded under the border key's name.
- `pass-green` (`:13`) `displayName "Pass"`, `refuse-red` (`:14`) `displayName "Refuse"` — same pattern: the human family name attached to the ink member.
- `#308653` (`--success-strong`) appears nowhere in design.json.

**`components` — 7 entries** (`:37-90`): `Primary Button` (`refersTo: "button-primary"`, `:38-43`), `Secondary Button` (`button-secondary`, `:46-51`), `Text Field` (`input`, `:54-59`), `Persona Chip` (`chip`, `:62-67`), `Utility Bar` (`utility-bar`, `kind: "nav"`, `:70-75`), `Info Alert` (`kind: "card"`, **no** `refersTo`, `:78-82`), `Proof Phase` (`kind: "custom"`, **no** `refersTo`, `:85-89`). All seven carry `description`, `html` and `css`; the first five also carry `refersTo`, and the last two (`Info Alert`, `Proof Phase`) carry no `refersTo`.
Two divergences from the shipped CSS worth knowing before reuse: the `ds-btn-primary`/`ds-btn-secondary` samples omit the `aria-busy`/disabled treatment that `app.css:138-139` defines, and the `Proof Phase` sample draws its done mark with an inset `box-shadow` ring (`:89`) where the shipped component uses an inline white-check SVG background (`app.css:289`). No entry exists for: step card, tag/badge, credential, consent sheet, proof clock, ledger, verdict, figures strip, attack card, rail, log disclosure, or the danger/warning alert variants.

**`narrative`** (`:92-121`): `northStar: "The Service Counter"` (`:93`), `overview` (the Overview prose of DESIGN.md:179-183, `:94`), `keyCharacteristics` (the five bullets, `:95-101`), `rules` (the four named rules with a `section` field, `:102-107`), `dos` (4, `:109-112`), `donts` (5, `:115-119`). The `dos`/`donts` here are the same sentences as `DESIGN.md:292-303` with the leading bold "Do"/"Don't" inlined — textually identical otherwise, and safe to quote from either.

---

## (e) Surface contract — `.impeccable/surfaces/sandbox-static-index-html.md`

Verbatim, the six fields:

> **THESIS** (`:16`): a state e-service that walks one citizen through one transaction, step by step, the way eesti.ee and ria.ee services do. Refuses the crypto-demo default of a dark terminal and glowing nodes.
>
> **OWN-WORLD** (`:18`): TEDI tokens. White ground, #005aa3 brand, #151926 text, #e1e2e5 hairlines, 4px card radius, pill buttons, Roboto 300 headings, Material Symbols Outlined. A blue utility bar on top, as on ria.ee.
>
> **STORY** (`:20`): you register, your wallet holds a yes/no, the shop asks, you consent, a real proof runs, the shop learns one bit. Then try to cheat, and watch each attempt fail for real.
>
> **FIRST VIEWPORT** (`:22`): the utility bar and white header with the wordmark and ET/EN. The H1 "Prove you're over 18. Keep everything else." sits in a left 7/12 column with the lead and a primary "Start" pill. The right 5/12 holds the cast of four actors as a vertical ledger: you, register, wallet, shop. The independence notice is an info alert under the header.
>
> **FORM** (`:24`): a TEDI vertical stepper with a sticky rail. The signature interaction is the live proof timeline in step 5: phases fill as the real prover's output streams, with an elapsed counter and the measured byte/second figures. It closes with a two-column ledger of "what the shop learned / what it never saw". Seed: pinned-by-brief (no roll).
>
> **FINISH** (`:26`): unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance

Also in the file, and binding on the surface: **Scope/Mode** (`:8`) — "the guided ZK age-proof sandbox. Mode: Operate, narrated (the visitor completes a task while being told what each step means)"; **Audience** (`:10`) — "reviewers, relying parties, curious citizens; no phone, no spec. Job: register a test identity, get the credential, prove age to a shop, see what the shop learned."; **Constraints** (`:12`) — "real backend (sandbox/server.py); ET/EN toggle; WCAG 2.1 AA; independence notice on the page. Visual world pinned by the user: ria.ee / TEDI. No concept roll, because the brief pinned the world."

---

## Gaps: things that do not exist on this surface

- An on-screen **not-built** state (FR-34). No string, no node, no health probe; `make deps` appears only on stderr, in `deploy.sh`, and in a Dockerfile comment.
- A rendered **cancellation** state. The declined message is written into a subtree that is `display:none` by the time it is unhidden.
- A **typed distinction** between refusal, cancellation and retryable error. No shared reason field; refusals are named in prose, errors are named in prose, and both use the same error icon and refuse ink.
- A **string-parity check** between `EN` and the Estonian markup (FR-32's own-check consequence). `sandbox/check.py` asserts the story over HTTP and never touches the dictionaries.
- A **refusal/cancellation record** the visitor can see. The technical log shows raw prover stdout; there is no reason field, no timestamped event list, and nothing survives the language toggle other than the values kept in `state`.
- Any **attack-level state tutorial** at the moment of failure in the cheat band: the outcome vocabulary is `Blocked` / `Got through – that's a bug` / `Unexpected answer. Check the server log.` plus the verifier's machine detail string.
- A **per-attack reset**: a completed attack card keeps its result until reload.
- An **announced clock**: `#proof-time` is `aria-hidden`; only phase transitions and the queue are announced.
- Reduced-motion gating for smooth-scroll anchors, the rail-dot and spine transitions (`app.css:51`, `158`, `277`, `280`).
- design.json entries for most of the shipped component inventory, and `colorMeta` for 17 of 27 colours (§d).

## Findings that would change the shape of new UX work

1. **FR-34 as stated cannot be satisfied by styling an existing state — the state does not exist.** The toolchain failure happens before the HTTP server binds (`server.py:444-446`, `:108-111`), so there is no page to put it on. A new surface has to be reachable when the process is not serving (a separate entry point, or the server starting in a degraded mode that still serves the page and refuses at the run).
2. **The cancellation state is currently invisible for a structural reason** (`app.js:351-356` unhides a node in a step that the same handler locks, `app.css:181`). Any FR-26 work has to decide where a cancellation is recorded on screen — the step the visitor returns to has no message slot today.
3. **The refusal vocabulary the run needs already exists in `CONFORMITY.md`, in the wallet, not in the sandbox** (typed `ResponseRefused`, a dedicated non-retryable message, logged reason, background negative answer, channel labelled, "show the subject only on a pass"). Reusing its categories is grounded; inventing new ones is not.
4. **The one tier label the sandbox already derives from the real request** is the encryption mode (`direct_post.jwt` vs unencrypted, `app.js:348`, `server.py:219`) — the precedent for FR-6's "tier labelled" is presentational and cheap to extend.
5. **design.json's ramps are the only place extra blue/ink steps exist**, and they have no step names; three of the twenty-seven colours carry ramps, all in reverse order. Any new surface that needs an intermediate tone must name it, because nothing here does.
6. **The two Do-rules most at risk from new states** are "Every number shown comes from the run that just happened" and "Status never colours a whole region" — a new failure state that adds a stripe, a hero colour block, or a plausible-looking figure would break DESIGN.md on its face, not merely in spirit.
7. **The Estonian-first rule has a loose end**: the markup is Estonian, but boot re-selects the language from `navigator.language` (`app.js:510-511`), and several visible strings (`veinikelder.test`, `12,90 €`, `ee.riik.poa.1`, the ET/EN captions, the "Keel / Language" label) are not keyed at all.
