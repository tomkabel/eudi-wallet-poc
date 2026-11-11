package main

import (
	"encoding/json"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
)

func avAuditSession(t *testing.T) (*presenter, *oid4vp.Session) {
	t.Helper()
	if os.Getenv("AV_CONFORMANCE") != "1" {
		t.Skip("AV_CONFORMANCE=1 enables AV audit including known nonconformance")
	}
	st := oid4vp.NewStore(time.Minute)
	t.Cleanup(st.Stop)
	key, err := jose.GenerateKey()
	if err != nil {
		t.Fatal(err)
	}
	tp, err := key.Thumbprint()
	if err != nil {
		t.Fatal(err)
	}
	s, err := st.New("x509_san_dns:verifier.example.ee", "https://verifier.example.ee/present/response", oid4vp.AgeQuery("proof_of_age", "eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18"), tp)
	if err != nil {
		t.Fatal(err)
	}
	return &presenter{register: testRegister(t), registerService: "age-verification", clientID: s.ClientID, store: st, responseKey: &key, baseURL: "https://verifier.example.ee"}, s
}
func TestAVConformanceResponseType(t *testing.T) {
	p, s := avAuditSession(t)
	if p.request(s).ResponseType != "vp_token" {
		t.Fatal("AV fallback response_type must be vp_token")
	}
}
func TestAVConformanceResponseMode(t *testing.T) {
	p, s := avAuditSession(t)
	if got := p.request(s).ResponseMode; got != "direct_post" {
		t.Fatalf("AV fallback requires direct_post; candidate emits %s", got)
	}
}
func TestAVConformanceQueryTransport(t *testing.T) {
	p, s := avAuditSession(t)
	w := httptest.NewRecorder()
	p.finishNew(w, s, nil)
	var result map[string]string
	if err := json.Unmarshal(w.Body.Bytes(), &result); err != nil {
		t.Fatal(err)
	}
	if strings.Contains(result["wallet_uri"], "request_uri=") {
		t.Fatal("AV fallback prohibits request_uri; candidate emits request object transport")
	}
}
func TestAVConformanceClientID(t *testing.T) {
	p, s := avAuditSession(t)
	req := p.request(s)
	if !strings.HasPrefix(req.ClientID, "redirect_uri:") || req.ClientID != "redirect_uri:"+req.ResponseURI {
		t.Fatalf("AV fallback requires redirect_uri client identification matching response_uri; got %s", req.ClientID)
	}
}
func TestAVConformanceNonce(t *testing.T) {
	p, s := avAuditSession(t)
	if p.request(s).Nonce == "" {
		t.Fatal("AV fallback nonce missing")
	}
}
func TestAVConformanceDCQL(t *testing.T) {
	p, s := avAuditSession(t)
	q := p.request(s).DCQLQuery
	if _, err := q.Validate(1); err != nil {
		t.Fatal(err)
	}
	if len(q.Credentials) != 1 || q.Credentials[0].Format != "mso_mdoc" {
		t.Fatal("AV fallback requires ISO mdoc DCQL")
	}
}
func TestAVConformanceState(t *testing.T) {
	p, s := avAuditSession(t)
	if p.request(s).State != s.ID {
		t.Fatal("optional AV fallback state must retain session identity when emitted")
	}
}
