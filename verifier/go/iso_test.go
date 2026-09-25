package main

// End-to-end tests of the ISO 18013-7 Annex C endpoints over httptest (plan
// step 2a, item 7). The proof inside the full-flow fixture is the committed
// step0-multipaz one — its transcript is the OpenID4VP redirect one, so the
// full dcapi flow cannot verify it (that is the point of the flipped-proof
// case); the positive FFI path is covered by TestStep0* and the transcript is
// pinned separately in oid4vp. What this file proves is the HTTP contract:
// guard order, status codes, and that no unsolicited spec id reaches the FFI.

import (
	"crypto/ecdh"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/x509"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"encoding/pem"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/ee-eudiw/verifier/go/circuits"
	"github.com/tomkabel/ee-eudiw/verifier/go/oid4vp"
	"github.com/tomkabel/ee-eudiw/verifier/go/zk"
)

const (
	testOrigin = "https://verifier.example.ee"
	specID71   = "longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121"
)

// isoHarness is a presenter wired to a temp trust store and the committed
// registry, as main() wires it.
type isoHarness struct {
	p      *presenter
	st     *oid4vp.Store
	regDir string
}

func newISOHarness(t *testing.T) *isoHarness {
	t.Helper()
	regDir := t.TempDir()
	reg, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	// Trust store fixture: the step0-multipaz issuer, read from the fixture's
	// own certificate so a regenerated snapshot cannot drift from the key the
	// committed proof was made over.
	pemBytes, err := os.ReadFile("zk/testdata/step0-multipaz/issuer_dsc.pem")
	if err != nil {
		t.Fatalf("read fixture issuer pem: %v", err)
	}
	blk, _ := pem.Decode(pemBytes)
	if blk == nil {
		t.Fatal("fixture issuer_dsc.pem holds no PEM block")
	}
	cert, err := x509.ParseCertificate(blk.Bytes)
	if err != nil {
		t.Fatalf("parse fixture issuer cert: %v", err)
	}
	pub, ok := cert.PublicKey.(*ecdsa.PublicKey)
	if !ok || pub.Curve != elliptic.P256() {
		t.Fatal("fixture issuer key is not ECDSA P-256")
	}
	uncompressed := elliptic.Marshal(elliptic.P256(), pub.X, pub.Y)
	pkx := "0x" + hex.EncodeToString(uncompressed[1:33])
	pky := "0x" + hex.EncodeToString(uncompressed[33:65])
	trustPath := filepath.Join(regDir, "issuers.json")
	issuers := `{"issuers":[{"name":"EE-EUDIW demo issuer","doc_type":"eu.europa.ec.av.1",` +
		`"namespace":"eu.europa.ec.av.1","pkx":"` + pkx + `","pky":"` + pky + `"}]}`
	if err := os.WriteFile(trustPath, []byte(issuers), 0o644); err != nil {
		t.Fatalf("trust store: %v", err)
	}
	trust, err := oid4vp.LoadTrustStore(trustPath)
	if err != nil {
		t.Fatalf("load trust store: %v", err)
	}
	st := oid4vp.NewStore(time.Minute)
	return &isoHarness{
		p: &presenter{
			store: st, trust: trust, registry: reg,
			clientID: "x509_san_dns:verifier.example.ee",
			baseURL:  "http://127.0.0.1:1", docType: "eu.europa.ec.av.1", nsID: "eu.europa.ec.av.1",
			sem:         make(limiter, 1),
			dcapiOrigin: testOrigin,
			offered:     reg.Accepted(),
		},
		st: st,
	}
}

// newISOSession drives handleDCAPINew and returns the session snapshot.
func (h *isoHarness) newISOSession(t *testing.T) *oid4vp.Session {
	t.Helper()
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/present/dcapi/new", strings.NewReader(`{}`))
	h.p.handleDCAPINew(rec, req)
	if rec.Code != http.StatusCreated {
		t.Fatalf("new: %d %s", rec.Code, rec.Body.String())
	}
	var created struct {
		ID string `json:"id"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &created); err != nil {
		t.Fatalf("new body: %v", err)
	}
	s, err := h.st.Get(created.ID)
	if err != nil {
		t.Fatalf("get: %v", err)
	}
	return s
}

// dcapiWrap builds the page's response envelope: HPKE over the transcript,
// single-shot ciphertext, wrapped ["dcapi", {enc, cipherText}], base64url.
func dcapiWrap(t *testing.T, priv *ecdh.PrivateKey, transcript, plaintext []byte) string {
	t.Helper()
	ct, err := oid4vp.SealTo(priv.PublicKey(), transcript, plaintext)
	if err != nil {
		t.Fatalf("seal: %v", err)
	}
	env := oid4vp.BuildDCAPIEnvelope(ct[:65], ct[65:])
	b, err := json.Marshal(map[string]string{"response": oid4vp.Base64URL(env)})
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	return string(b)
}

// post sends one request at the handler and returns recorder + session.
func (h *isoHarness) post(t *testing.T, id, body string) *httptest.ResponseRecorder {
	t.Helper()
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/present/dcapi/response/"+id, strings.NewReader(body))
	h.p.handleDCAPIResponse(rec, req)
	return rec
}

// TestISOUnsolicitedSpecIsRefusedFast is the S12 guard: a response naming a
// spec id the session never offered gets 403 before any FFI call. The check
// here is behavioural: it returns fast (the FFI takes ~2.5s, this must not).
func TestISOUnsolicitedSpecIsRefusedFast(t *testing.T) {
	h := newISOHarness(t)
	s := h.newISOSession(t)
	ext := s.SessionISO()

	// A syntactically fine envelope whose plaintext is a DeviceResponse with a
	// spec id that was not offered. The HPKE open succeeds (it is our key), so
	// the 403 comes from the allowlist — reached without an FFI call.
	plain := mustUnsolicitedResponse(t)
	body := dcapiWrap(t, ext.HPKEPrivate, mustTranscript(t, s), plain)
	start := time.Now()
	rec := h.post(t, s.ID, body)
	if rec.Code != http.StatusForbidden {
		t.Fatalf("unsolicited spec: got %d %s, want 403", rec.Code, rec.Body.String())
	}
	if time.Since(start) > 2*time.Second {
		t.Fatalf("unsolicited spec took %s; the allowlist is not ahead of the FFI", time.Since(start))
	}
	// Replay of the same body: the session burned, 410.
	rec2 := h.post(t, s.ID, body)
	if rec2.Code != http.StatusGone {
		t.Fatalf("replay: got %d, want 410", rec2.Code)
	}
}

// TestDCAPIPageShowsSessionElement: the page names the element the session
// asks for, not a fixed one, and html/template quotes the id for its JS context.
func TestDCAPIPageShowsSessionElement(t *testing.T) {
	h := newISOHarness(t)
	rec := httptest.NewRecorder()
	h.p.handleDCAPINew(rec, httptest.NewRequest(http.MethodPost, "/present/dcapi/new",
		strings.NewReader(`{"element":"age_over_21"}`)))
	var created struct {
		ID string `json:"id"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &created); err != nil || created.ID == "" {
		t.Fatalf("new: %d %s", rec.Code, rec.Body.String())
	}
	rec = httptest.NewRecorder()
	h.p.handleDCAPIPage(rec, httptest.NewRequest(http.MethodGet, "/present/dcapi/"+created.ID, nil))
	page := rec.Body.String()
	if rec.Code != http.StatusOK || !strings.Contains(page, "<code>age_over_21</code>") ||
		strings.Contains(page, "age_over_18") {
		t.Fatalf("page does not show the session's element: %d\n%s", rec.Code, page)
	}
	if !strings.Contains(page, `const SESSION_ID = "`+created.ID+`";`) {
		t.Fatalf("session id is not a quoted JS string:\n%s", page)
	}
}

// TestISOFlippedCiphertextBit: one flipped bit in the envelope must be a 400
// "could not be decrypted", not a decode error further down.
func TestISOFlippedCiphertextBit(t *testing.T) {
	h := newISOHarness(t)
	s := h.newISOSession(t)
	ext := s.SessionISO()
	plain := mustUnsolicitedResponse(t)
	body := dcapiWrap(t, ext.HPKEPrivate, mustTranscript(t, s), plain)
	raw, err := base64.RawURLEncoding.DecodeString(bodyResponse(body))
	if err != nil {
		t.Fatalf("body: %v", err)
	}
	raw[len(raw)-1] ^= 0x01 // flip in the ciphertext tail
	flipped, err := json.Marshal(map[string]string{"response": oid4vp.Base64URL(raw)})
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	rec := h.post(t, s.ID, string(flipped))
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("flipped ciphertext: got %d %s, want 400", rec.Code, rec.Body.String())
	}
}

// TestISOFlippedProofBit: a well-formed response carrying the committed
// multipaz proof with one bit flipped reaches zk.Verify and comes back
// valid:false at 200 (an answer, not an error).
func TestISOFlippedProofBit(t *testing.T) {
	if _, err := os.Stat("../zkverify-ffi/target/release/libzkverify.a"); err != nil {
		t.Skip("FFI staticlib not built")
	}
	h := newISOHarness(t)
	s := h.newISOSession(t)
	ext := s.SessionISO()
	plain := mustFlippedProofResponse(t, s)
	body := dcapiWrap(t, ext.HPKEPrivate, mustTranscript(t, s), plain)
	rec := h.post(t, s.ID, body)
	if rec.Code != http.StatusOK {
		t.Fatalf("flipped proof: got %d %s, want 200", rec.Code, rec.Body.String())
	}
	if !strings.Contains(rec.Body.String(), `"valid":false`) {
		t.Fatalf("flipped proof must be valid:false, got %s", rec.Body.String())
	}
}

// TestStep2aMultipazDeviceResponse is the acceptance proof for the fixed
// parser (finding 2): the committed device_response.cbor was written by the
// fork's :zk-conformance module through multipaz 0.99.0's real serialization
// path (buildDeviceResponse + addZkDocument, the wallet's own code). The test
// drives ParseZkDocumentsBytes over it, checks the spec id against the session
// allowlist, and runs zk.Verify on the parsed proof with the transcript the
// fixture ships — the whole server path without the HTTP layer. One flipped
// proof bit must fail.
func TestStep2aMultipazDeviceResponse(t *testing.T) {
	dir := "zk/testdata/step2a-iso-annex-c"
	raw, err := os.ReadFile(filepath.Join(dir, "device_response.cbor"))
	if err != nil {
		t.Fatalf("read device_response.cbor: %v", err)
	}
	transcript, err := os.ReadFile(filepath.Join(dir, "device_response_transcript.bin"))
	if err != nil {
		t.Fatalf("read device_response_transcript.bin: %v", err)
	}

	// Parse: the acceptance bar the old parser could not reach — it demanded
	// documentData as an inline map where multipaz writes it tag-24 wrapped
	// (#24 over a ZkDocumentData map), zkSystemSpecId
	// on the zkDocument where multipaz writes zkSystemId inside the data, and
	// a plain tstr timestamp where multipaz writes tag 0.
	docs, err := oid4vp.ParseZkDocumentsBytes(raw)
	if err != nil {
		t.Fatalf("ParseZkDocumentsBytes on the real multipaz DeviceResponse: %v", err)
	}
	if len(docs) != 1 {
		t.Fatalf("got %d zkDocuments, want exactly 1", len(docs))
	}
	d := docs[0]
	if d.DocType != "eu.europa.ec.av.1" {
		t.Fatalf("docType %q", d.DocType)
	}
	if d.ZkSystemSpecID != specID71 {
		t.Fatalf("spec id %q, want the offered circuits.json spec", d.ZkSystemSpecID)
	}
	if len(d.MSOX5Chain) == 0 {
		t.Fatal("msoX5chain not unwrapped from the fixture")
	}
	// The timestamp must be the tag-0 text, RFC 3339 clean.
	if _, err := time.Parse(time.RFC3339, d.Timestamp); err != nil {
		t.Fatalf("timestamp %q: %v", d.Timestamp, err)
	}

	// Spec allowlist: the id the fixture carries must be the one this
	// verifier offers (what the server's rule (c) does with ext.BySpecID).
	reg, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	var circuit circuits.Circuit
	offered := false
	for _, c := range reg.Accepted() {
		if c.SpecID() == d.ZkSystemSpecID {
			circuit, offered = c, true
		}
	}
	if !offered {
		t.Fatalf("fixture spec %q is not in the registry allowlist", d.ZkSystemSpecID)
	}

	// Trust: the fixture ships the minted issuer's public half; enroll it as
	// a relying party would and select by the parsed msoX5chain leaf, exactly
	// as the server does (rule (b)).
	issuerJSON, err := os.ReadFile(filepath.Join(dir, "device_response_issuer.json"))
	if err != nil {
		t.Fatalf("read device_response_issuer.json: %v", err)
	}
	trustPath := filepath.Join(t.TempDir(), "issuers.json")
	if err := os.WriteFile(trustPath, issuerJSON, 0o644); err != nil {
		t.Fatalf("write trust store: %v", err)
	}
	trust, err := oid4vp.LoadTrustStore(trustPath)
	if err != nil {
		t.Fatalf("load trust store: %v", err)
	}
	leaf, err := oid4vp.LeafCertificate([][]byte{d.MSOX5Chain})
	if err != nil {
		t.Fatalf("leaf certificate: %v", err)
	}
	pub, ok := leaf.PublicKey.(*ecdsa.PublicKey)
	if !ok {
		t.Fatal("issuer leaf key is not ECDSA")
	}
	uncompressed := elliptic.Marshal(elliptic.P256(), pub.X, pub.Y)
	issuer, err := trust.Select(d.DocType,
		"0x"+hex.EncodeToString(uncompressed[1:33]),
		"0x"+hex.EncodeToString(uncompressed[33:65]))
	if err != nil {
		t.Fatalf("issuer selection: %v", err)
	}

	// Verify: the real multipaz proof against the Rust runtime this repo links.
	req := zk.Request{
		Version:       circuit.Version,
		NumAttributes: circuit.NumAttributes,
		PKx:           issuer.PKx,
		PKy:           issuer.PKy,
		DocType:       d.DocType,
		Namespace:     "eu.europa.ec.av.1",
		AttrID:        "age_over_18",
		AttrCBOR:      []byte{0xf5},
		Now:           d.Timestamp,
		Transcript:    transcript,
		Proof:         d.Proof,
	}
	if err := zk.Verify(req); err != nil {
		t.Fatalf("the multipaz DeviceResponse proof did not verify end to end: %v", err)
	}

	// One flipped bit anywhere in the proof must fail.
	flipped := make([]byte, len(d.Proof))
	copy(flipped, d.Proof)
	flipped[len(flipped)/2] ^= 0x01
	req.Proof = flipped
	if err := zk.Verify(req); err == nil {
		t.Fatal("a one-bit-flipped proof from the real DeviceResponse verified")
	}
}

// TestISOTimestampWindowNegative pins rule (a) at the API level: a document
// whose timestamp is outside the window is refused 400.
func TestISOTimestampWindowNegative(t *testing.T) {
	created := time.Now().Add(-time.Hour)
	old := created.Add(-2 * time.Minute).UTC().Format("2006-01-02T15:04:05Z")
	if err := oid4vp.CheckTimestampWindow(created, time.Now(), old); err == nil {
		t.Fatal("a timestamp 62s before session creation must be refused")
	}
	future := time.Now().Add(2 * time.Minute).UTC().Format("2006-01-02T15:04:05Z")
	if err := oid4vp.CheckTimestampWindow(time.Now(), time.Now(), future); err == nil {
		t.Fatal("a timestamp 120s in the future must be refused")
	}
}

func mustTranscript(t *testing.T, s *oid4vp.Session) []byte {
	t.Helper()
	tr, err := s.ISOTranscript()
	if err != nil {
		t.Fatalf("transcript: %v", err)
	}
	return tr
}

func bodyResponse(body string) string {
	var m map[string]string
	_ = json.Unmarshal([]byte(body), &m)
	return m["response"]
}

// cborText/cborBytes equivalents for the test builders (package main has no
// encoder of its own; the shapes mirror oid4vp's).
func tText(s string) []byte { return append(cborHead(3, uint64(len(s))), s...) }
func tBytes(b []byte) []byte {
	return append(cborHead(2, uint64(len(b))), b...)
}
func tHead(major byte, n uint64) []byte {
	mt := major << 5
	switch {
	case n < 24:
		return []byte{mt | byte(n)}
	case n < 1<<8:
		return []byte{mt | 24, byte(n)}
	case n < 1<<16:
		return []byte{mt | 25, byte(n >> 8), byte(n)}
	default:
		return []byte{mt | 26, byte(n >> 24), byte(n >> 16), byte(n >> 8), byte(n)}
	}
}

// zkResponse builds multipaz 0.99.0's DeviceResponse shape by hand:
//
//	{status: 0, documents: [{zkDocument: {proof,
//	 documentData: #24({zkSystemId, docType, timestamp: #0(...),
//	 issuerSigned: {ns: [...]}, deviceSigned: {}, msoX5chain})}}]}
//
// The same bytes ZkDocument.toDataItem writes (see ParseZkDocument). Good
// enough for the negative paths: every one of them is refused before any field
// here is used for a signature check.
func zkResponse(t *testing.T, specID, timestamp string, chain, proof []byte) []byte {
	t.Helper()
	// ZkDocumentData: tag 24 over the encoded map. (The wire key is
	// documentData — the ZkDocument constructor property name — even though
	// the class it carries is ZkDocumentData.)
	dd := []byte{0xa6} // zkSystemId, docType, timestamp, issuerSigned, deviceSigned, msoX5chain
	pair := func(k string, v []byte) { dd = append(dd, append(tText(k), v...)...) }
	pair("zkSystemId", tText(specID))
	pair("docType", tText("eu.europa.ec.av.1"))
	// timestamp: tag 0 over a tstr.
	pair("timestamp", append([]byte{0xc0}, tText(timestamp)...))
	// issuerSigned: {ns: [{elementIdentifier, elementValue}]} — the element
	// shape no rule on this path reads inside, but the parser checks it.
	issuerSigned := append([]byte{0xa1}, tText("eu.europa.ec.av.1")...)
	issuerSigned = append(issuerSigned, 0x81) // array of one element
	issuerSigned = append(issuerSigned, 0xa2) // {elementIdentifier, elementValue}
	issuerSigned = append(issuerSigned, append(tText("elementIdentifier"), tText("age_over_18")...)...)
	issuerSigned = append(issuerSigned, append(tText("elementValue"), []byte{0xf5}...)...)
	pair("issuerSigned", issuerSigned)
	pair("deviceSigned", []byte{0xa0})
	pair("msoX5chain", tBytes(chain))
	ddWrapped := append([]byte{0xd8, 0x18}, tBytes(dd)...) // tag 24 over bstr
	zkd := []byte{0xa2}
	zkd = append(zkd, append(tText("proof"), tBytes(proof)...)...)
	zkd = append(zkd, append(tText("documentData"), ddWrapped...)...)
	// documents = [{zkDocument: {...}}]
	entry := append([]byte{0xa1}, tText("zkDocument")...)
	entry = append(entry, zkd...)
	wrapped := append([]byte{0x81}, entry...)
	dr := []byte{0xa2}
	dr = append(dr, tText("status")...)
	dr = append(dr, 0x00)
	dr = append(dr, tText("documents")...)
	dr = append(dr, wrapped...)
	return dr
}

func mustUnsolicitedResponse(t *testing.T) []byte {
	t.Helper()
	return zkResponse(t, "longfellow-libzk-v1_9_9_DEADBEEF", "2026-09-24T12:00:00Z", []byte{0x30}, []byte{0x01})
}

func mustFlippedProofResponse(t *testing.T, s *oid4vp.Session) []byte {
	t.Helper()
	proof, err := os.ReadFile("zk/testdata/step0-multipaz/proof.bin")
	if err != nil {
		t.Fatalf("read fixture proof: %v", err)
	}
	proof[len(proof)/2] ^= 0x01
	pemBytes, err := os.ReadFile("zk/testdata/step0-multipaz/issuer_dsc.pem")
	if err != nil {
		t.Fatalf("read fixture chain: %v", err)
	}
	blk, _ := pem.Decode(pemBytes)
	if blk == nil {
		t.Fatal("fixture issuer_dsc.pem holds no PEM block")
	}
	return zkResponse(t, specID71, time.Now().UTC().Format("2006-01-02T15:04:05Z"), blk.Bytes, proof)
}

func cborHead(major byte, n uint64) []byte { return tHead(major, n) }
