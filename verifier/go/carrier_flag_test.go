package main

import (
	"encoding/base64"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
)

// The -carrier seam at the HTTP boundary: the same plain-path (interim JSON)
// presentation answers identically whether the verifier sniffs (default) or
// forces the interim carrier via -carrier. The forced CBOR carrier must
// refuse the JSON envelope loudly (400), never fall back.

func plainCarrierHarness(t *testing.T, carrier oid4vp.Carrier) (*presenter, *oid4vp.Session) {
	t.Helper()
	reg, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	// The step 8.7 issuer JSON works for the doctype the fixtures use; the
	// plain path here only needs A trust store entry to reach the refusal
	// under test (or acceptance in the FFI-gated case).
	issuerJSON, err := os.ReadFile("zk/testdata/step8-7-openid4vp-zk/device_response_issuer.json")
	if err != nil {
		t.Fatalf("read issuer json: %v", err)
	}
	trust, err := oid4vp.LoadTrustStore(writeTemp(t, issuerJSON))
	if err != nil {
		t.Fatalf("trust store: %v", err)
	}
	st := oid4vp.NewStore(time.Minute)
	q := oid4vp.AgeQuery("age_credential", "eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18")
	s, err := st.NewWith("x509_san_dns:verifier.example.com",
		"https://verifier.example.com/present/response/plain",
		"plain-carrier-nonce-0123456789abcdef", q, nil)
	if err != nil {
		t.Fatalf("session: %v", err)
	}
	return &presenter{store: st, trust: trust, registry: reg, sem: make(limiter, 1), carrier: carrier}, s
}

func writeTemp(t *testing.T, b []byte) string {
	t.Helper()
	p := t.TempDir() + "/issuers.json"
	if err := os.WriteFile(p, b, 0o644); err != nil {
		t.Fatal(err)
	}
	return p
}

func postPlain(t *testing.T, p *presenter, s *oid4vp.Session, raw []byte) *httptest.ResponseRecorder {
	t.Helper()
	body, err := json.Marshal(map[string]oid4vp.VPToken{"vp_token": {
		"age_credential": {base64.RawURLEncoding.EncodeToString(raw)},
	}})
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest("POST", "/present/response/"+s.ID, strings.NewReader(string(body)))
	req.Header.Set("Content-Type", "application/json")
	p.handleResponse(rec, req)
	return rec
}

func plainEnvelope(t *testing.T) []byte {
	t.Helper()
	proof, err := os.ReadFile("zk/testdata/step8-7-openid4vp-zk/device_response.cbor")
	if err != nil {
		t.Fatalf("fixture: %v", err)
	}
	docs, err := oid4vp.ParseZkDocumentsBytes(proof)
	if err != nil || len(docs) != 1 {
		t.Fatalf("parse fixture: %v", err)
	}
	env := map[string]any{
		"zk_system":      "longfellow-libzk-v1",
		"version":        7,
		"num_attributes": 1,
		"doc_type":       "eu.europa.ec.av.1",
		"namespace":      "eu.europa.ec.av.1",
		"attr_id":        "age_over_18",
		"attr_cbor_hex":  "f5",
		"proof_b64":      base64.StdEncoding.EncodeToString(docs[0].Proof),
	}
	raw, err := json.Marshal(env)
	if err != nil {
		t.Fatal(err)
	}
	return raw
}

func TestPlainPathCarrierOverride(t *testing.T) {
	raw := plainEnvelope(t)

	// Default (nil carrier): the plain query sniffs, accepts the JSON
	// envelope, and reaches the circuit/trust rules. Both harnesses here get
	// valid:false (this proof is a CBOR-bound proof, not a plain-path one) —
	// what matters is the envelope was NOT refused as malformed.
	p, s := plainCarrierHarness(t, nil)
	if rec := postPlain(t, p, s, raw); rec.Code == http.StatusBadRequest {
		t.Fatalf("default dispatch refused the interim envelope: %s", rec.Body.String())
	}

	// Forced interim carrier: same acceptance.
	p, s = plainCarrierHarness(t, oid4vp.InterimJSON{})
	if rec := postPlain(t, p, s, raw); rec.Code == http.StatusBadRequest {
		t.Fatalf("forced interim carrier refused the interim envelope: %s", rec.Body.String())
	}

	// Forced CBOR carrier over JSON bytes: a loud refusal, never a fallback.
	// (The CBOR decode error is logged server-side with its own wording — the
	// cborsub.ErrDecode contract — so the body says "accepted CBOR subset".)
	p, s = plainCarrierHarness(t, oid4vp.MsoMdocZkCBOR{})
	rec := postPlain(t, p, s, raw)
	if rec.Code != http.StatusBadRequest ||
		!strings.Contains(rec.Body.String(), "accepted CBOR subset") {
		t.Fatalf("forced CBOR carrier over interim bytes: got %d %s, want a 400 naming the CBOR subset",
			rec.Code, rec.Body.String())
	}
}
