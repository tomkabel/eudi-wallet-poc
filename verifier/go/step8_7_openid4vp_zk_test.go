package main

// Step 8.7 of docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md: the de-facto
// mso_mdoc_zk carrier over OpenID4VP, proven end to end. The fixture under
// zk/testdata/step8-7-openid4vp-zk was written by the fork's :zk-conformance
// Step87OpenID4VPFixtureTest through multipaz 0.99.0's real serialization
// (buildDeviceResponse + addZkDocument) with the proof bound to the B.2.6.1
// OpenID4VPHandover transcript built from request.json's client_id, nonce and
// response_uri. The Go side re-derives those transcript bytes from the session
// parameters — never from a blob the fixture ships — so the test pins the
// transcript construction itself, not just a pairing that happens to exist.
//
// This file lives in package main because it drives the presenter (checkZk)
// and zk.Verify together.

import (
	"bytes"
	"crypto/ecdsa"
	"crypto/elliptic"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
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

// step87Fixture is the request-side metadata the fork records next to the
// response: everything a session would have been created from.
type step87Fixture struct {
	ClientID       string  `json:"client_id"`
	Nonce          string  `json:"nonce"`
	ResponseURI    string  `json:"response_uri"`
	JwkThumbprint  *string `json:"jwk_thumbprint"`
	DocType        string  `json:"doc_type"`
	Namespace      string  `json:"namespace"`
	AttrID         string  `json:"attr_id"`
	AttrCBORHex    string  `json:"attr_cbor_hex"`
	Timestamp      string  `json:"timestamp"`
	Version        uint32  `json:"version"`
	NumAttributes  uint32  `json:"num_attributes"`
	ZkSystemSpecID string  `json:"zk_system_spec_id"`
}

// loadStep87Fixture reads the fixture and returns the raw DeviceResponse bytes
// plus its metadata.
func loadStep87Fixture(t *testing.T) (raw []byte, meta step87Fixture) {
	t.Helper()
	dir := "zk/testdata/step8-7-openid4vp-zk"
	raw, err := os.ReadFile(filepath.Join(dir, "device_response.cbor"))
	if err != nil {
		t.Fatalf("read device_response.cbor: %v", err)
	}
	metaJSON, err := os.ReadFile(filepath.Join(dir, "request.json"))
	if err != nil {
		t.Fatalf("read request.json: %v", err)
	}
	if err := json.Unmarshal(metaJSON, &meta); err != nil {
		t.Fatalf("parse request.json: %v", err)
	}
	if meta.JwkThumbprint != nil {
		t.Fatal("the fixture was generated for direct_post; jwk_thumbprint must be null")
	}
	return raw, meta
}

// step87Session builds the session the fixture's request would have created:
// same client id, nonce, response URI and advertised circuit, so the transcript
// the production Transcript() derives is exactly the one the proof binds.
func step87Session(t *testing.T, meta step87Fixture) (*oid4vp.Session, oid4vp.CredentialQuery) {
	t.Helper()
	circuit := zkCircuitFor(t, meta.Version, meta.NumAttributes)
	store := oid4vp.NewStore(time.Hour)
	s, err := store.NewWith(meta.ClientID, meta.ResponseURI, meta.Nonce,
		oid4vp.ZkAgeQuery("age_credential", meta.DocType, meta.Namespace, meta.AttrID, circuit))
	if err != nil {
		t.Fatalf("session: %v", err)
	}
	cq, err := s.Query.Single()
	if err != nil {
		t.Fatalf("session query: %v", err)
	}
	// Profile rule 4: stock multipaz answers only the label its ZkSystem is
	// registered under.
	if got := cq.Meta.ZkSystemType[0].System; got != oid4vp.SystemMultipaz {
		t.Fatalf("ZkAgeQuery advertises system %q, want %q", got, oid4vp.SystemMultipaz)
	}
	return s, cq
}

// loadStep87Issuer enrolls the fixture's minted issuer the way a relying party
// enrolls a real one: public half into a trust store, then selection by the
// presentation's own msoX5chain leaf.
func loadStep87Issuer(t *testing.T, msoX5Chain []byte, docType string) oid4vp.Issuer {
	t.Helper()
	dir := "zk/testdata/step8-7-openid4vp-zk"
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
	leaf, err := oid4vp.LeafCertificate([][]byte{msoX5Chain})
	if err != nil {
		t.Fatalf("leaf certificate: %v", err)
	}
	pub, ok := leaf.PublicKey.(*ecdsa.PublicKey)
	if !ok {
		t.Fatal("issuer leaf key is not ECDSA")
	}
	uncompressed := elliptic.Marshal(elliptic.P256(), pub.X, pub.Y)
	issuer, err := trust.Select(docType,
		"0x"+hex.EncodeToString(uncompressed[1:33]),
		"0x"+hex.EncodeToString(uncompressed[33:65]))
	if err != nil {
		t.Fatalf("issuer selection: %v", err)
	}
	return issuer
}

// TestStep87OpenID4VPZkVerify reads the fork-generated fixture, rebuilds the
// OpenID4VP session around its handover parameters, and walks the exact path
// the server's checkZk walks: parse the vp_token, match the query, allowlist
// the advertised circuit, select the issuer, verify over the B.2.6.1
// OpenID4VPHandover transcript. A one-bit-flipped proof must fail.
func TestStep87OpenID4VPZkVerify(t *testing.T) {
	raw, meta := loadStep87Fixture(t)
	s, cq := step87Session(t, meta)

	// The vp_token entry: base64url of the raw DeviceResponse bytes, exactly
	// what a wallet puts at vp_token[<credential id>].
	token := oid4vp.VPToken{cq.ID: []string{base64.RawURLEncoding.EncodeToString(raw)}}

	docs, err := token.ParseZkVPToken(cq)
	if err != nil {
		t.Fatalf("ParseZkVPToken on the real multipaz DeviceResponse: %v", err)
	}
	d, err := oid4vp.MatchQueryZkDocument(docs, cq)
	if err != nil {
		t.Fatalf("MatchQueryZkDocument: %v", err)
	}
	if d == nil {
		t.Fatal("the fixture carries no zkDocument")
	}
	if d.DocType != meta.DocType {
		t.Fatalf("docType %q, want %q", d.DocType, meta.DocType)
	}
	if d.ZkSystemSpecID != meta.ZkSystemSpecID {
		t.Fatalf("zkSystemId %q, want the fixture's %q", d.ZkSystemSpecID, meta.ZkSystemSpecID)
	}
	if len(d.MSOX5Chain) == 0 {
		t.Fatal("msoX5chain not unwrapped from the fixture")
	}
	if d.Timestamp != meta.Timestamp {
		t.Fatalf("timestamp %q, want the proof-bound %q", d.Timestamp, meta.Timestamp)
	}

	// Allowlist: the zkDocument's spec must be one of the circuits the query
	// advertised, and all of those must be registry-listed (what checkZk does).
	registry, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	advertised, err := oid4vp.ZkSystemTypeAllowlist(cq.Meta, registry.Accepted())
	if err != nil {
		t.Fatalf("the query's own zk_system_type is not allowlisted: %v", err)
	}
	circuit, known := zkCircuitForSpec(advertised, d.ZkSystemSpecID)
	if !known {
		t.Fatalf("fixture spec %q is not among the advertised circuits", d.ZkSystemSpecID)
	}

	issuer := loadStep87Issuer(t, d.MSOX5Chain, d.DocType)

	// The transcript is rebuilt from the session, not read from the fixture:
	// this is the B.2.6.1 construction the whole step exists to pin. It must
	// be the redirect handover, not the ISO dcapi transcript the step 2a
	// fixture ships — different bytes, provably.
	transcript, err := s.Transcript()
	if err != nil {
		t.Fatalf("session transcript: %v", err)
	}
	isoTranscript := mustISOFixtureTranscript(t)
	if string(transcript) == string(isoTranscript) {
		t.Fatal("the OpenID4VP handover and the ISO dcapi transcript are the same bytes")
	}

	req := zk.Request{
		Version:       circuit.Version,
		NumAttributes: circuit.NumAttributes,
		PKx:           issuer.PKx,
		PKy:           issuer.PKy,
		DocType:       d.DocType,
		Namespace:     meta.Namespace,
		AttrID:        meta.AttrID,
		AttrCBOR:      []byte{0xf5},
		Now:           d.Timestamp,
		Transcript:    transcript,
		Proof:         d.Proof,
	}
	if err := zk.Verify(req); err != nil {
		t.Fatalf("the multipaz mso_mdoc_zk proof did not verify over the OpenID4VP handover: %v", err)
	}

	// One flipped bit anywhere in the proof must fail.
	flipped := make([]byte, len(d.Proof))
	copy(flipped, d.Proof)
	flipped[len(flipped)/2] ^= 0x01
	req.Proof = flipped
	if err := zk.Verify(req); err == nil {
		t.Fatal("a one-bit-flipped mso_mdoc_zk proof verified over the OpenID4VP handover")
	}

	// Cross-transport replay: the same proof over the ISO dcapi transcript
	// (the step 2a fixture's binding) must fail. The fork side asserts the
	// mirror image; together they pin that one proof is bound to one handover.
	req.Transcript = isoTranscript
	if err := zk.Verify(req); err == nil {
		t.Fatal("an OpenID4VP-bound proof verified over the ISO dcapi transcript")
	}
}

// TestStep87UnregisteredCircuitRefusal pins the allowlist discipline: a DCQL
// query advertising an unregistered zk_system_type is refused before any FFI
// call. ZkSystemTypeAllowlist never touches zk.CircuitHash, so the refusal
// costs nothing runtime-shaped.
func TestStep87UnregisteredCircuitRefusal(t *testing.T) {
	registry, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	accepted := registry.Accepted()

	unknown := oid4vp.ZkSystemTypeSpec{
		System:        "longfellow-libzk-v1",
		ID:            "longfellow-libzk-v1_9_1_9999_9999_0000000000000000000000000000000000000000000000000000000000000000",
		CircuitHash:   "0000000000000000000000000000000000000000000000000000000000000000",
		NumAttributes: 1,
		Version:       9,
		BlockEncHash:  9999,
		BlockEncSig:   9999,
	}

	// A query advertising only the unknown circuit.
	q := oid4vp.DCQL{Credentials: []oid4vp.CredentialQuery{{
		ID:     "age_credential",
		Format: oid4vp.FormatMsoMdocZk,
		Meta: &oid4vp.Meta{
			DoctypeValue: "eu.europa.ec.av.1",
			ZkSystemType: []oid4vp.ZkSystemTypeSpec{unknown},
		},
		Claims: []oid4vp.ClaimPath{{Path: []string{"eu.europa.ec.av.1", "age_over_18"}, Values: []any{true}}},
	}}}
	cq, err := q.Single()
	if err != nil {
		t.Fatalf("the query itself is malformed: %v", err)
	}
	if _, err := oid4vp.ZkSystemTypeAllowlist(cq.Meta, accepted); err == nil {
		t.Fatal("an unregistered zk_system_type was allowlisted")
	}

	// A query mixing the accepted circuit with the unknown one must also be
	// refused — the unknown entry may not ride along.
	acceptedSpec := oid4vp.ZkSystemTypeSpec{
		System:        "longfellow-libzk-v1",
		ID:            accepted[0].SpecID(),
		CircuitHash:   accepted[0].Hash,
		NumAttributes: accepted[0].NumAttributes,
		Version:       accepted[0].Version,
		BlockEncHash:  accepted[0].BlockEncHash,
		BlockEncSig:   accepted[0].BlockEncSig,
	}
	q.Credentials[0].Meta.ZkSystemType = []oid4vp.ZkSystemTypeSpec{acceptedSpec, unknown}
	if _, err := oid4vp.ZkSystemTypeAllowlist(cq.Meta, accepted); err == nil {
		t.Fatal("a query mixing an accepted and an unknown circuit was allowlisted")
	}

	// A tampered hash on an otherwise real spec id is refused too.
	tampered := acceptedSpec
	tampered.CircuitHash = "ffffffff" + acceptedSpec.CircuitHash[8:]
	q.Credentials[0].Meta.ZkSystemType = []oid4vp.ZkSystemTypeSpec{tampered}
	if _, err := oid4vp.ZkSystemTypeAllowlist(cq.Meta, accepted); err == nil {
		t.Fatal("a tampered circuit_hash under a real spec id was allowlisted")
	}

	// And the happy path: the accepted circuit alone passes, and what comes
	// back is the registry entry itself.
	q.Credentials[0].Meta.ZkSystemType = []oid4vp.ZkSystemTypeSpec{acceptedSpec}
	out, err := oid4vp.ZkSystemTypeAllowlist(cq.Meta, accepted)
	if err != nil {
		t.Fatalf("the accepted circuit was refused: %v", err)
	}
	if len(out) != 1 || out[0].Hash != accepted[0].Hash {
		t.Fatal("the allowlist did not return the registry entry")
	}

	// Single's own guard: an mso_mdoc_zk query with no zk_system_type at all
	// is malformed, regardless of the allowlist.
	q.Credentials[0].Meta.ZkSystemType = nil
	if _, err := q.Single(); err == nil {
		t.Fatal("an mso_mdoc_zk query with no zk_system_type was accepted")
	}
}

// TestStep87UnansweredSession pins the "not a yes" outcomes that are not FFI
// errors: an empty DeviceResponse (no zkDocuments) is a nil answer, and a
// zkDocument for another doctype is a refusal.
func TestStep87UnansweredSession(t *testing.T) {
	cq, err := oid4vp.ZkAgeQuery("age_credential", "eu.europa.ec.av.1",
		"eu.europa.ec.av.1", "age_over_18", circuits.Circuit{
			Hash:          "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
			Version:       7,
			NumAttributes: 1,
			BlockEncHash:  4151,
			BlockEncSig:   4096,
		}).Single()
	if err != nil {
		t.Fatalf("query: %v", err)
	}
	if d, err := oid4vp.MatchQueryZkDocument(nil, cq); err != nil || d != nil {
		t.Fatalf("an empty DeviceResponse must be a nil answer, got %v, %v", d, err)
	}
	wrong := &oid4vp.ZkDocument{DocType: "eu.europa.ec.pid.1", ZkSystemSpecID: "x"}
	if _, err := oid4vp.MatchQueryZkDocument([]*oid4vp.ZkDocument{wrong}, cq); err == nil {
		t.Fatal("a zkDocument for another doctype was accepted")
	}
}

// postStep87 drives checkZk the way a wallet does: a fresh presenter wired as
// newISOHarness wires one (committed registry, a trust store holding the
// fixture's issuer), a session carrying the fixture's handover parameters, and
// the DeviceResponse POSTed through handleResponse to /present/response/<id>.
func postStep87(t *testing.T, deviceResponse []byte) *httptest.ResponseRecorder {
	t.Helper()
	_, meta := loadStep87Fixture(t)
	reg, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	issuerJSON, err := os.ReadFile("zk/testdata/step8-7-openid4vp-zk/device_response_issuer.json")
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
	st := oid4vp.NewStore(time.Minute)
	s, err := st.NewWith(meta.ClientID, meta.ResponseURI, meta.Nonce,
		oid4vp.ZkAgeQuery("age_credential", meta.DocType, meta.Namespace, meta.AttrID,
			zkCircuitFor(t, meta.Version, meta.NumAttributes)))
	if err != nil {
		t.Fatalf("session: %v", err)
	}
	p := &presenter{store: st, trust: trust, registry: reg, sem: make(limiter, 1)}
	body, err := json.Marshal(map[string]oid4vp.VPToken{"vp_token": {
		"age_credential": {base64.RawURLEncoding.EncodeToString(deviceResponse)},
	}})
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/present/response/"+s.ID, bytes.NewReader(body))
	req.Header.Set("Content-Type", "application/json")
	p.handleResponse(rec, req)
	return rec
}

// TestStep87HandleResponse runs checkZk through the HTTP handler. A positive
// answer is out of reach here: checkZk windows the timestamp against
// time.Now(), and the committed proof binds 2026-09-24T14:19:58Z. Each case
// below fails at a different rule, which pins the rule order.
func TestStep87HandleResponse(t *testing.T) {
	raw, meta := loadStep87Fixture(t)
	docs, err := oid4vp.ParseZkDocumentsBytes(raw)
	if err != nil || len(docs) != 1 {
		t.Fatalf("parse fixture: %v (%d docs)", err, len(docs))
	}
	d := docs[0]
	fresh := time.Now().UTC().Format("2006-01-02T15:04:05Z")

	// 1. The fixture as-is: the dispatch reaches checkZk and the stale
	// timestamp is refused by CheckTimestampWindow. The detail tells this
	// apart from the plain path's 400s.
	rec := postStep87(t, raw)
	if rec.Code != http.StatusBadRequest || !strings.Contains(rec.Body.String(), "session window") {
		t.Fatalf("stale fixture: got %d %s, want 400 naming the session window", rec.Code, rec.Body.String())
	}

	// 2. An unlisted zkSystemId, still with the stale timestamp: 403 rather
	// than the window's 400 or zk.Verify's 200, so the refusal runs before both.
	unlisted := zkResponse(t, "longfellow-libzk-v1_9_9_DEADBEEF", meta.Timestamp, d.MSOX5Chain, d.Proof)
	rec = postStep87(t, unlisted)
	if rec.Code != http.StatusForbidden || !strings.Contains(rec.Body.String(), "not offered in this session") {
		t.Fatalf("unlisted spec: got %d %s, want 403", rec.Code, rec.Body.String())
	}

	// 3. The real spec id and proof under a fresh timestamp: every rule passes
	// and zk.Verify runs over the session's transcript, but the proof is bound
	// to the old timestamp, so the answer is 200 valid:false.
	if _, err := os.Stat("../zkverify-ffi/target/release/libzkverify.a"); err != nil {
		t.Skip("FFI staticlib not built")
	}
	rewrapped := zkResponse(t, d.ZkSystemSpecID, fresh, d.MSOX5Chain, d.Proof)
	rec = postStep87(t, rewrapped)
	if rec.Code != http.StatusOK || !strings.Contains(rec.Body.String(), `"valid":false`) {
		t.Fatalf("re-timestamped proof: got %d %s, want 200 valid:false", rec.Code, rec.Body.String())
	}
}

// --- helpers shared by the step 8.7 tests ---

func zkCircuitFor(t *testing.T, version, numAttributes uint32) circuits.Circuit {
	t.Helper()
	registry, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	for _, c := range registry.Accepted() {
		if c.Version == version && c.NumAttributes == numAttributes {
			return c
		}
	}
	t.Fatalf("no registry circuit for version %d with %d attributes", version, numAttributes)
	return circuits.Circuit{}
}

func zkCircuitForSpec(accepted []circuits.Circuit, specID string) (circuits.Circuit, bool) {
	for _, c := range accepted {
		if c.SpecID() == specID {
			return c, true
		}
	}
	return circuits.Circuit{}, false
}

// mustISOFixtureTranscript reads the step 2a fixture's ISO dcapi transcript —
// the different binding the cross-transport negative control needs.
func mustISOFixtureTranscript(t *testing.T) []byte {
	t.Helper()
	b, err := os.ReadFile("zk/testdata/step2a-iso-annex-c/device_response_transcript.bin")
	if err != nil {
		t.Fatalf("read the ISO transcript fixture: %v", err)
	}
	return b
}
