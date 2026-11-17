"use strict";

// English for every [data-t] node. Estonian is the markup itself, read once at load.
const EN = {
  skip: "Skip to content",
  utility: "Independent development project · not an RIA service",
  brand: "ZK age proof", brand_sub: "Sandbox · EUDI Wallet",
  nav_label: "Page sections", nav_journey: "Journey", nav_cheat: "Try to cheat", nav_limits: "Limits",
  notice_html: 'This is a <strong>sandbox</strong> for the independent project <a href="https://github.com/tomkabel/eudi-wallet-poc">tomkabel/eudi-wallet-poc</a>. It is not an application of the Estonian Information System Authority (RIA) or the Potential consortium, and neither has endorsed it. The people are made up, and all data is deleted when the server stops.',
  h1: "Prove you’re over 18. Keep everything else.",
  lead: "This sandbox walks you through a real zero-knowledge proof, step by step. You register a test identity, your wallet receives a proof-of-age credential, and a web shop asks whether you are of age. The shop gets a “yes” backed by a mathematical proof, and it never sees your name or your birth date.",
  real: "Everything here runs for real: the credential is signed, the proof is computed, and the verifier checks it. There are no pre-recorded answers.",
  start: "Start",
  cast_h: "Who’s involved",
  cast_you: "You", cast_you_d: "The holder. You decide what to share.",
  cast_reg: "Population Register (sandbox)", cast_reg_d: "The issuer. Signs the proof-of-age credential.",
  cast_wallet: "Your wallet", cast_wallet_d: "Holds the credential and computes the proof.",
  cast_shop: "Veinikelder (test shop)", cast_shop_d: "The relying party. Must check your age.",
  rail_label: "Steps",
  s1_short: "Register", s2_short: "Get the credential", s3_short: "Visit the shop",
  s4_short: "Give consent", s5_short: "The proof runs", s6_short: "What the shop learned",
  s1_h: "Tell the Population Register who you are",
  s1_p: "In real life, the issuer reads your age from the Population Register over X-Road. In the sandbox it believes the date you type. That is a deliberate shortcut, which is why it says so here.",
  personas: "Use a ready-made test person", p_mari: "Mari Maasikas, 34", p_karl: "Karl Kask, 16",
  given: "First name", family: "Last name", birth: "Date of birth",
  birth_hint: "Your birth date goes to the issuer only. It is not put in the credential.",
  enrol_btn: "Register and issue the credential",
  s2_h: "Your wallet receives a proof-of-age credential",
  s2_p: "The issuer turned your birth date into a few yes/no answers and signed them. The credential is an ISO 18013-5 mdoc, and every value in it is salted and hashed. Your wallet keeps it like a document in your pocket.",
  cred_title: "Proof of age", cred_signed: "Signed", cred_valid: "Valid", cred_issuer: "Issuer key", cred_size: "Size",
  absent_h: "What the credential does not contain",
  absent_name: "Your name", absent_birth: "Your birth date", absent_id: "Personal ID code", absent_photo: "Photo",
  to_shop: "Go to the shop",
  s3_h: "Veinikelder has to check your age",
  s3_p: "Alcohol may only be sold to adults. Usually that means showing an ID card, which reveals far more than one answer. Here the shop asks just one thing: are you over 18?",
  shop_label: "Veinikelder (test shop)", shop_badge: "test shop",
  product: "Saaremaa apple wine, dry", product_meta: "0.75 l · 11.5% · alcoholic beverage",
  gate: "Before you order, confirm you are at least 18.",
  shop_btn: "Confirm age with EUDI Wallet",
  s4_h: "Your wallet asks for permission",
  s4_p: "The shop sent an OpenID4VP request. Your wallet shows you exactly who is asking and for what. Nothing moves until you agree.",
  consent_who: "Requested by", consent_asks: "Asks", consent_q: "Are you over 18?",
  consent_share: "The shop sees", consent_share_d: "One answer: yes. Plus proof that a trusted issuer signed that answer.",
  consent_hide: "The shop does not see", consent_hide_d: "Your name, birth date, other age answers, the issuer’s signature, or your device key.",
  consent_enc: "Encryption", consent_nonce: "One-time nonce",
  consent_yes: "Share the proof", consent_no: "Decline",
  declined: "You declined. The shop received nothing. You can try again if you like.",
  s5_h: "Your wallet computes a zero-knowledge proof",
  s5_p: "The wallet proves that its credential contains <code>age_over_18 = true</code>, signed by a trusted issuer, without showing the credential itself. The proof is bound to this exact request, so it can’t be reused anywhere else.",
  ph_request: "Request read", ph_bind: "Bound to this session", ph_prove: "Zero-knowledge proof computed",
  ph_prove_note: "This is the longest step: the Longfellow circuit checks the signatures and hashes inside the proof.",
  ph_send: "Encrypted and sent", ph_verify: "The shop checked the proof",
  log: "Show the technical log",
  s6_h: "What the shop learned",
  learned_h: "The shop learned", kept_h: "Stayed with you",
  figures_note: "These figures were measured just now, during your own run.",
  to_cheat: "Now try to cheat", restart: "Start over with a new person",
  cheat_h: "Try to cheat",
  cheat_p: "A defence is worth as much as the attacks it holds against. Each attempt below runs for real: a proof is computed and the verifier decides. Each takes about as long as your own proof did.",
  cheat_locked: "The attempts unlock once you finish the journey.",
  a_replay_h: "Send the same proof twice",
  a_replay_p: "An attacker captures a valid response and posts it again. The nonce is single-use, so the second post must be refused.",
  a_tamper_h: "A proof made for another session",
  a_tamper_p: "The wallet proves against the wrong nonce, as if the proof had been made for a different shop. The prover is happy to do it, but the verifier recomputes the session itself.",
  a_lie_h: "A minor lies",
  a_lie_p: "We register 16-year-old Karl and force the prover to prove a value that is <code>false</code> in his credential. The proof itself is sound, but it proves the wrong thing.",
  a_run: "Run the attempt",
  limits_h: "What the sandbox simplifies",
  limits_p: "The cryptography is real. The world around it is deliberately simple. A real system has to solve each of these.",
  l1_h: "The register believes you.", l1_p: "Your age comes from the date you type, not from the Population Register (EE-POA-006/007).",
  l2_h: "The keys live on the server.", l2_p: "The device key is a file on this machine, not in a phone’s secure element (EE-SEC-001). The Android wallet keeps it in StrongBox.",
  l3_h: "The proof is computed on the server.", l3_p: "In real life the phone computes the proof. Here the same prover binary runs on the server.",
  l4_h: "The request is unsigned.", l4_p: "The shop does not prove its identity with an access certificate (EE-RP-002/003).",
  l5_h: "One proof at a time.", l5_p: "If someone else is computing a proof right now, you wait in line.",
  footer_html: 'An independent fork of <a href="https://github.com/open-eid/eudi-wallet-poc">open-eid/eudi-wallet-poc</a>. Not affiliated with RIA, the Ministry of Justice and Digital Affairs, or any Estonian public authority. The design follows the <a href="https://github.com/TEHIK-EE/tedi-design-system">TEDI design system</a>.',
  footer_spec: "Technical specification EE-EUDIW-TS-1.0",
};

// Strings built at runtime, in both languages.
const D = {
  et: {
    title: "ZK vanusetõend · liivakast",
    desc: "Tõesta, et oled üle 18, ilma sünnikuupäeva näitamata. Päris nullteadmustõestus, algusest lõpuni.",
    step_of: (n) => `Samm ${n}/6`,
    yes: "jah", no: "ei",
    err_name: "Sisesta ees- ja perekonnanimi.",
    err_birth: "Sisesta kehtiv sünnikuupäev, mis ei ole tulevikus.",
    err_backend: "Liivakasti server ei vastanud. Kontrolli, kas server töötab, ja proovi uuesti.",
    err_expired: "Seanss aegus. Alusta poes uuesti.",
    err_rate: "Liiga palju katseid lühikese aja jooksul. Proovi mõne minuti pärast uuesti.",
    err_busy: "Liivakast on praegu hõivatud: mitu tõestust on juba järjekorras. Proovi minuti pärast uuesti.",
    sum1: (n, a) => `${n} registreeritud. Väljastaja arvutas vanuseks ${a} ja unustas sünnikuupäeva.`,
    sum2: (y) => `Tunnistus on rahakotis: age_over_18 = ${y}.`,
    sum3: "Pood saatis päringu: kas oled üle 18?",
    sum4: "Andsid nõusoleku jagada ühte vastust.",
    sum5: (s) => `Tõend arvutati ja saadeti ${s} sekundiga.`,
    sum5_fail: "Rahakott ei suutnud tõendit arvutada.",
    enc: (k) => `Vastus krüpteeritakse poe ${k}-võtmele (direct_post.jwt). Isegi server ei loe seda vahepeal.`,
    enc_plain: "Krüpteerimata (direct_post).",
    queued: "Keegi teine arvutab parasjagu tõendit. Oled järjekorras…",
    live_phase: (p) => `${p} – tehtud.`,
    bytes: (b) => `${fmt(b / 1000, 0)} kB`,
    proved: (b, s) => `${fmt(b / 1000, 0)} kB, ${fmt(s, 1)} s`,
    ok_h: "Vanus kinnitatud. Tellimust saab jätkata.",
    ok_p: (d) => `Kontrollija vastus: ${d}`,
    no_h: "Vanust ei kinnitatud. Pood ei müü.",
    refused_p: "Sinu tunnistuses on age_over_18 = false. Rahakott ei saa tõestada midagi, mis ei ole tõsi, ja pood ei saanud midagi.",
    invalid_p: (d) => `Kontrollija lükkas tõendi tagasi: ${d}`,
    learned: [["Üks vastus: age_over_18 = true", "Ainus väli, mida päring küsis"], ["Et selle allkirjastas usaldatud väljastaja", "Tõestatud ringskeemi sees, mitte näidatud"], ["Et tõend on tehtud just selle päringu jaoks", "Nonss ja vastusevõti on tõendiga seotud"]],
    learned_none: [["Mitte midagi", "Rahakott ei saatnud ühtegi tõendit"]],
    kept: (n) => [[n, "Nimi"], ["Sünnikuupäev", "Ei jõudnud kunagi tunnistusse"], ["age_over_16 ja age_over_21", "Olid tunnistuses, kuid jäid avaldamata"], ["Väljastaja allkiri ja soolad", "Ilma nendeta ei saa sind järgmisel ostul ära tunda"], ["Seadmevõti", "Allkirjastas seansi, kuid ei lahkunud rahakotist"]],
    fig_proof: "Tõendi suurus", fig_prove: "Tõendi arvutamine", fig_verify: "Poe kontroll", fig_total: "Kokku",
    a_running: (s) => `Käib… ${s} s`,
    a_blocked: "Tõkestatud",
    a_broken: "Läbi läks – see on viga",
    a_replay_ok: (s) => `Esimene postitus: kehtiv. Teine: HTTP ${s} – seanss on juba vastatud.`,
    a_tamper_ok: (d) => `Kontrollija: valid:false – ${d}`,
    a_lie_ok: (d) => `Kontrollija: valid:false – ${d}`,
    a_unexpected: "Ootamatu vastus. Vaata serveri logi.",
  },
  en: {
    title: "ZK age proof · sandbox",
    desc: "Prove you are over 18 without showing your birth date. A real zero-knowledge proof, end to end.",
    step_of: (n) => `Step ${n} of 6`,
    yes: "yes", no: "no",
    err_name: "Enter a first and last name.",
    err_birth: "Enter a valid date of birth that isn’t in the future.",
    err_backend: "The sandbox backend didn’t answer. Check that the server is running and try again.",
    err_expired: "The session expired. Start again at the shop.",
    err_rate: "Too many attempts in a short time. Try again in a few minutes.",
    err_busy: "The sandbox is busy: several proofs are already queued. Try again in a minute.",
    sum1: (n, a) => `${n} registered. The issuer worked out age ${a} and forgot the birth date.`,
    sum2: (y) => `The credential is in your wallet: age_over_18 = ${y}.`,
    sum3: "The shop sent a request: are you over 18?",
    sum4: "You agreed to share one answer.",
    sum5: (s) => `The proof was computed and sent in ${s} seconds.`,
    sum5_fail: "The wallet could not compute a proof.",
    enc: (k) => `The response is encrypted to the shop’s ${k} key (direct_post.jwt). Not even the server can read it in transit.`,
    enc_plain: "Unencrypted (direct_post).",
    queued: "Someone else is computing a proof right now. You’re in the queue…",
    live_phase: (p) => `${p}: done.`,
    bytes: (b) => `${fmt(b / 1000, 0)} kB`,
    proved: (b, s) => `${fmt(b / 1000, 0)} kB, ${fmt(s, 1)} s`,
    ok_h: "Age confirmed. The order can go ahead.",
    ok_p: (d) => `The verifier answered: ${d}`,
    no_h: "Age not confirmed. The shop won’t sell.",
    refused_p: "Your credential says age_over_18 = false. The wallet can’t prove something that isn’t true, and the shop received nothing.",
    invalid_p: (d) => `The verifier rejected the proof: ${d}`,
    learned: [["One answer: age_over_18 = true", "The only field the request asked for"], ["That a trusted issuer signed it", "Proved inside the circuit, never shown"], ["That the proof was made for this request", "The nonce and response key are bound into it"]],
    learned_none: [["Nothing", "The wallet sent no proof"]],
    kept: (n) => [[n, "Name"], ["Birth date", "Never went into the credential"], ["age_over_16 and age_over_21", "In the credential, but not disclosed"], ["The issuer’s signature and salts", "Without them, the shop can’t recognise you next time"], ["Device key", "Signed the session, never left the wallet"]],
    fig_proof: "Proof size", fig_prove: "Computing the proof", fig_verify: "Shop’s check", fig_total: "Total",
    a_running: (s) => `Running… ${s} s`,
    a_blocked: "Blocked",
    a_broken: "Got through – that’s a bug",
    a_replay_ok: (s) => `First post: valid. Second: HTTP ${s} – session already answered.`,
    a_tamper_ok: (d) => `Verifier: valid:false – ${d}`,
    a_lie_ok: (d) => `Verifier: valid:false – ${d}`,
    a_unexpected: "Unexpected answer. Check the server log.",
  },
};

const $ = (s, r = document) => r.querySelector(s);
const $$ = (s, r = document) => [...r.querySelectorAll(s)];
let lang = "et";
const ET = {};
const fmt = (n, d) => new Intl.NumberFormat(lang === "et" ? "et-EE" : "en-GB", { minimumFractionDigits: d, maximumFractionDigits: d }).format(n);
const t = (k, ...a) => { const v = D[lang][k]; return typeof v === "function" ? v(...a) : v; };

const state = { step: 1, holder: null, name: "", age: 0, over18: false, session: null, run: null, holders: {} };

// ---------- language ----------
function setLang(l) {
  lang = l;
  document.documentElement.lang = l;
  for (const el of $$("[data-t]")) {
    const k = el.dataset.t;
    const v = l === "et" ? ET[k] : EN[k] ?? ET[k];
    if (k.endsWith("_html") || /<code>/.test(v)) el.innerHTML = v; else el.textContent = v;
  }
  for (const el of $$("[data-t-aria]")) {
    const k = el.dataset.tAria;
    el.setAttribute("aria-label", l === "et" ? ET["aria:" + k] : EN[k]);
  }
  for (const b of $$("[data-lang]")) b.setAttribute("aria-pressed", String(b.dataset.lang === l));
  document.title = t("title");
  $('meta[name="description"]').content = t("desc");
  try { localStorage.setItem("lang", l); } catch {}
  rerender();
}

// Re-render everything built from data, so a language switch mid-journey is complete.
function rerender() {
  $("#rail-count").textContent = t("step_of", state.step);
  if (state.cred) renderCredential(state.cred);
  if (state.consent) renderConsent(state.consent);
  for (const s of $$(".step[data-state='done']")) summarise(+s.dataset.step);
  if (state.run?.finished) renderResult();
  for (const a of $$(".attack")) if (a._render) a._render();
}

// ---------- steps ----------
function go(n, focus = true) {
  state.step = n;
  for (const s of $$(".step")) {
    const i = +s.dataset.step;
    s.dataset.state = i < n ? "done" : i === n ? "active" : "locked";
    s.setAttribute("aria-current", i === n ? "step" : "false");
  }
  for (const li of $$(".rail__list li")) {
    const i = +li.dataset.for;
    li.dataset.state = i < n ? "done" : i === n ? "active" : "locked";
    const a = li.querySelector("a");
    if (i === n) a.setAttribute("aria-current", "step"); else a.removeAttribute("aria-current");
    a.tabIndex = i > n ? -1 : 0;
  }
  for (let i = 1; i < n; i++) summarise(i);
  $("#rail-count").textContent = t("step_of", n);
  if (focus) {
    const el = $(`#step-${n}`);
    el.focus({ preventScroll: true });
    el.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" });
  }
}

function summarise(i) {
  const p = $(`#step-${i} .step__summary`);
  if (!p) return;
  const text = {
    1: () => t("sum1", state.name, state.age),
    2: () => t("sum2", state.over18 ? "true" : "false"),
    3: () => t("sum3"),
    4: () => t("sum4"),
    5: () => state.run?.prove ? t("sum5", fmt(state.run.respondedAt ?? state.run.elapsed, 1)) : t("sum5_fail"),
  }[i]?.();
  if (!text) return;
  p.replaceChildren(icon("check_circle"), document.createTextNode(text));
  p.hidden = false;
}

function icon(name) {
  const s = document.createElement("span");
  s.className = "ms"; s.setAttribute("aria-hidden", "true"); s.textContent = name;
  return s;
}

async function api(path, body) {
  const r = await fetch(path, { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body ?? {}) });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) throw Object.assign(new Error(j.error || r.status), { code: j.error });
  return j;
}

// Map a failed call (fetch error code or SSE fail event) to a sentence.
function errText(x) {
  const c = x?.code || x?.error;
  return t({ rate: "err_rate", busy: "err_busy", expired: "err_expired" }[c] || "err_backend");
}

function showError(el, msg) { el.replaceChildren(icon("error"), document.createTextNode(msg)); el.hidden = false; }

// ---------- 1: register ----------
const PERSONAS = {
  mari: { given: "Mari", family: "Maasikas", years: 34, md: "05-14" },
  karl: { given: "Karl", family: "Kask", years: 16, md: "03-02" },
};
function personaDate(p) {
  const d = new Date();
  let y = d.getFullYear() - p.years;
  const [m, day] = p.md.split("-").map(Number);
  if (d.getMonth() + 1 < m || (d.getMonth() + 1 === m && d.getDate() < day)) y -= 1;
  return `${y}-${p.md}`;
}
for (const b of $$("[data-persona]")) b.addEventListener("click", () => {
  const p = PERSONAS[b.dataset.persona];
  $("#given").value = p.given; $("#family").value = p.family; $("#birth_date").value = personaDate(p);
  $("#enrol button[type=submit]").focus();
});

function ageOf(iso) {
  const b = new Date(iso + "T00:00:00"), n = new Date();
  return n.getFullYear() - b.getFullYear() - ((n.getMonth() < b.getMonth() || (n.getMonth() === b.getMonth() && n.getDate() < b.getDate())) ? 1 : 0);
}

$("#birth_date").max = new Date().toISOString().slice(0, 10);
$("#enrol").addEventListener("submit", async (e) => {
  e.preventDefault();
  const f = e.currentTarget, err = $("#enrol-error"), btn = f.querySelector("button[type=submit]");
  err.hidden = true;
  for (const i of $$("input", f)) i.removeAttribute("aria-invalid");
  const given = f.given.value.trim(), family = f.family.value.trim(), birth = f.birth_date.value;
  if (!given || !family) { f[!given ? "given" : "family"].setAttribute("aria-invalid", "true"); f[!given ? "given" : "family"].focus(); return showError(err, t("err_name")); }
  if (!birth || new Date(birth) > new Date() || ageOf(birth) > 130) { f.birth_date.setAttribute("aria-invalid", "true"); f.birth_date.focus(); return showError(err, t("err_birth")); }
  btn.setAttribute("aria-busy", "true"); btn.disabled = true;
  try {
    const r = await api("/api/enrol", { given, family, birth_date: birth });
    Object.assign(state, { holder: r.holder, name: `${given} ${family}`, age: ageOf(birth), over18: r.predicates.age_over_18, cred: r });
    state.holders[r.predicates.age_over_18 ? "adult" : "minor"] = r.holder;
    renderCredential(r);
    go(2);
  } catch (x) {
    showError(err, x.code === "birth_date" ? t("err_birth") : x.code === "name" ? t("err_name") : errText(x));
  } finally { btn.removeAttribute("aria-busy"); btn.disabled = false; }
});

// ---------- 2: credential ----------
function renderCredential(c) {
  const dl = $("#cred-claims");
  dl.replaceChildren(...Object.entries(c.predicates).map(([k, v]) => {
    const row = document.createElement("div");
    if (k === "age_over_18") row.className = "is-asked";
    const dt = document.createElement("dt"); const code = document.createElement("code"); code.textContent = k; dt.append(code);
    const dd = document.createElement("dd"); dd.className = v ? "yes" : "no";
    dd.append(icon(v ? "check_circle" : "cancel"), document.createTextNode(t(v ? "yes" : "no")));
    row.append(dt, dd); return row;
  }), (() => {
    const row = document.createElement("div");
    const dt = document.createElement("dt"); const code = document.createElement("code"); code.textContent = "issuing_country"; dt.append(code);
    const dd = document.createElement("dd"); dd.textContent = c.issuing_country; row.append(dt, dd); return row;
  })());
  const d = (s) => s ? new Intl.DateTimeFormat(lang === "et" ? "et-EE" : "en-GB", { dateStyle: "medium", timeZone: "UTC" }).format(new Date(s)) : "–";
  $("#cred-valid").textContent = `${d(c.valid_from)} – ${d(c.valid_until)}`;
  $("#cred-issuer").textContent = c.issuer_pkx ? `${c.issuer_pkx.slice(0, 18)}…` : "–";
  $("#cred-size").textContent = c.device_response_bytes ? `${fmt(c.device_response_bytes, 0)} B` : "–";
}
$("[data-next='3']").addEventListener("click", () => go(3));

// ---------- 3: shop ----------
$("#shop-start").addEventListener("click", async (e) => {
  const btn = e.currentTarget, err = $("#shop-error");
  err.hidden = true; btn.setAttribute("aria-busy", "true"); btn.disabled = true;
  try {
    const s = await api("/api/shop/start");
    const c = await api("/api/wallet/fetch", { session: s.session });
    state.session = s.session; state.consent = c;
    renderConsent(c);
    $("#consent-declined").hidden = true;
    go(4);
  } catch (x) { showError(err, errText(x)); }
  finally { btn.removeAttribute("aria-busy"); btn.disabled = false; }
});

// ---------- 4: consent ----------
function renderConsent(c) {
  $("#c-client").textContent = c.client_id;
  const q = c.dcql_query.credentials[0];
  $("#c-path").textContent = `${q.meta.doctype_value} › ${q.claims[0].path.join(" › ")} = ${JSON.stringify(q.claims[0].values?.[0])}`;
  $("#c-enc").textContent = c.response_mode === "direct_post.jwt" ? t("enc", c.response_key || "P-256") : t("enc_plain");
  $("#c-nonce").textContent = `${c.nonce.slice(0, 16)}…`;
}
$("#consent-no").addEventListener("click", () => {
  $("#consent-declined").hidden = false;
  state.session = null; state.consent = null;
  go(3, false);
  $("#shop-start").focus();
});
$("#consent-yes").addEventListener("click", () => { go(5); runProof(); });

// ---------- 5: the proof ----------
const PHASES = ["request", "bind", "prove", "send", "verify"];
function phase(name, st, detail) {
  const li = $(`#phases [data-phase="${name}"]`);
  li.dataset.state = st;
  if (detail !== undefined) $(`#d-${name}`).textContent = detail;
  if (st === "done" || st === "failed") $("#proof-live").textContent = t("live_phase", li.querySelector("strong").textContent);
}

// Run present.py through the server, streaming its output. The callbacks see
// each line; the promise resolves with the final outcome.
function stream(mode, holder, session, onLine, onQueued) {
  return new Promise((resolve, reject) => {
    const es = new EventSource(`/api/present?holder=${encodeURIComponent(holder)}&session=${encodeURIComponent(session)}&mode=${mode}`);
    es.addEventListener("line", (e) => onLine(JSON.parse(e.data)));
    es.addEventListener("queued", () => onQueued?.());
    es.addEventListener("done", (e) => { es.close(); resolve(JSON.parse(e.data)); });
    es.addEventListener("fail", (e) => { es.close(); reject(Object.assign(new Error("fail"), JSON.parse(e.data))); });
    es.onerror = () => { es.close(); reject(new Error("stream")); };
  });
}

function ticker(render) {
  const t0 = performance.now();
  const id = setInterval(() => render((performance.now() - t0) / 1000), 100);
  return () => clearInterval(id);
}

async function runProof() {
  const run = state.run = { lines: [], elapsed: 0 };
  for (const p of PHASES) phase(p, "pending", "");
  $("#log").textContent = "";
  phase("request", "active");
  const stop = ticker((s) => { run.elapsed = s; $("#proof-time").textContent = fmt(s, 1); });
  try {
    const out = await stream("normal", state.holder, state.session, ({ text, t: at }) => {
      run.lines.push(text);
      $("#log").textContent += text + "\n";
      let m;
      if ((m = text.match(/^request\s*:\s*(.*)/))) phase("request", "done", m[1]), phase("bind", "active");
      else if ((m = text.match(/^transcript\s*:\s*(\d+) bytes, sha256 (\S+)/))) phase("bind", "done", `SessionTranscript ${m[1]} B · sha256 ${m[2]}`), phase("prove", "active");
      else if ((m = text.match(/PROVE\s*:\s*(\d+) bytes in ([\d.]+)\s*s/))) { run.prove = { bytes: +m[1], s: +m[2], at }; phase("prove", "done", t("proved", +m[1], +m[2])); phase("send", "active"); }
      else if ((m = text.match(/^response\s*:\s*HTTP (\d+)/))) { run.respondedAt = at; run.verifyS = run.prove ? at - run.prove.at : null; phase("send", "done", `direct_post.jwt · HTTP ${m[1]}`); phase("verify", "active"); }
    }, () => { $("#proof-live").textContent = t("queued"); $("#d-request").textContent = t("queued"); });
    stop();
    run.out = out; run.finished = true;
    const ok = out.response?.valid === true;
    if (ok) phase("verify", "done", out.response.detail);
    else if (out.prover_refused) { phase("prove", "failed", "age_over_18 = false"); }
    else phase(run.respondedAt ? "verify" : "prove", "failed", out.response?.detail || "");
    $("#proof-time").textContent = fmt(run.respondedAt ?? run.elapsed, 1);
    state.holders.used = true;
    setTimeout(() => { renderResult(); go(6); unlockAttacks(); }, 700);
  } catch (x) {
    stop();
    const active = $("#phases [data-state='active']");
    if (active) phase(active.dataset.phase, "failed", errText(x));
  }
}

// ---------- 6: result ----------
function li([a, b]) {
  const el = document.createElement("li"); el.textContent = a;
  if (b) { const s = document.createElement("small"); s.textContent = b; el.append(s); }
  return el;
}
function renderResult() {
  const run = state.run, out = run.out, ok = out.response?.valid === true;
  const v = $("#verdict");
  v.className = `verdict verdict--${ok ? "ok" : "no"}`;
  const body = document.createElement("div");
  const h = document.createElement("strong"); h.textContent = t(ok ? "ok_h" : "no_h");
  const p = document.createElement("p");
  p.textContent = ok ? t("ok_p", out.response.detail) : out.prover_refused ? t("refused_p") : t("invalid_p", out.response?.detail || "");
  body.append(h, p);
  v.replaceChildren(icon(ok ? "verified_user" : "gpp_bad"), body);
  $("#learned").replaceChildren(...(ok ? D[lang].learned : D[lang].learned_none).map(li));
  $("#kept").replaceChildren(...D[lang].kept(state.name).map(li));
  const figs = [];
  if (run.prove) figs.push([t("fig_proof"), t("bytes", run.prove.bytes)], [t("fig_prove"), `${fmt(run.prove.s, 1)} s`]);
  if (run.verifyS != null) figs.push([t("fig_verify"), `${fmt(run.verifyS, 1)} s`]);
  figs.push([t("fig_total"), `${fmt(run.respondedAt ?? run.elapsed, 1)} s`]);
  $("#figures").replaceChildren(...figs.map(([k, val]) => {
    const d = document.createElement("div"), dt = document.createElement("dt"), dd = document.createElement("dd");
    dt.textContent = k; dd.textContent = val; d.append(dt, dd); return d;
  }));
}
$("#restart").addEventListener("click", () => {
  $("#enrol").reset();
  for (const s of $$(".step__summary")) s.hidden = true;
  Object.assign(state, { holder: null, cred: null, consent: null, session: null, run: null });
  go(1);
  $("#given").focus();
});

// ---------- try to cheat ----------
let attackBusy = false;
async function holderFor(kind) {
  if (state.holders[kind]) return state.holders[kind];
  const p = PERSONAS[kind === "adult" ? "mari" : "karl"];
  const r = await api("/api/enrol", { given: p.given, family: p.family, birth_date: personaDate(p) });
  return (state.holders[kind] = r.holder);
}
function unlockAttacks() {
  $("#cheat-locked").hidden = true;
  for (const b of $$(".attack button")) b.disabled = attackBusy;
}
for (const card of $$(".attack")) {
  const btn = card.querySelector("button"), out = card.querySelector(".attack__out"), mode = card.dataset.mode;
  let result = null, secs = 0;
  card._render = () => {
    if (!result && !secs) return;
    out.className = "attack__out";
    if (!result) { out.textContent = t("a_running", fmt(secs, 0)); return; }
    if (result.error) { out.textContent = result.text; return; }
    const blocked = result.blocked;
    out.classList.add(blocked ? "is-blocked" : "is-broken");
    const h = document.createElement("strong"); h.append(icon(blocked ? "shield" : "warning"), document.createTextNode(t(blocked ? "a_blocked" : "a_broken")));
    const c = document.createElement("code"); c.textContent = result.text;
    out.replaceChildren(h, c);
  };
  btn.addEventListener("click", async () => {
    attackBusy = true;
    for (const b of $$(".attack button")) b.disabled = true;
    btn.setAttribute("aria-busy", "true");
    result = null; secs = 0;
    const stop = ticker((s) => { secs = s; card._render(); });
    try {
      const holder = await holderFor(mode === "lie" ? "minor" : "adult");
      const s = await api("/api/shop/start");
      await api("/api/wallet/fetch", { session: s.session });
      const o = await stream(mode, holder, s.session, () => {});
      const detail = o.response?.detail || "";
      if (mode === "replay") result = o.replay?.status === 410 ? { blocked: true, text: t("a_replay_ok", 410) } : { blocked: false, text: JSON.stringify(o.replay ?? o) };
      else if (o.response?.valid === false) result = { blocked: true, text: t(mode === "lie" ? "a_lie_ok" : "a_tamper_ok", detail) };
      else if (o.response?.valid === true) result = { blocked: false, text: JSON.stringify(o.response) };
      else result = { blocked: false, text: t("a_unexpected") };
    } catch (x) { result = { error: true, text: errText(x) }; }
    stop(); card._render();
    btn.removeAttribute("aria-busy");
    attackBusy = false;
    unlockAttacks();
  });
}

// ---------- boot ----------
for (const el of $$("[data-t]")) ET[el.dataset.t] = el.dataset.t.endsWith("_html") || el.querySelector("code") ? el.innerHTML : el.textContent;
for (const el of $$("[data-t-aria]")) ET["aria:" + el.dataset.tAria] = el.getAttribute("aria-label");
for (const b of $$("[data-lang]")) b.addEventListener("click", () => setLang(b.dataset.lang));
go(1, false);
let saved = null;
try { saved = localStorage.getItem("lang"); } catch {}
setLang(saved || (navigator.language?.startsWith("et") ? "et" : "en"));
