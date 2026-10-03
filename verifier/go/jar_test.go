package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
)

// jarHarness is a presenter wired for the JAR tests: signed request key on.
func jarHarness(t *testing.T) (*presenter, *oid4vp.Session) {
	t.Helper()
	h := newISOHarness(t)
	k, err := jose.GenerateKey()
	if err != nil {
		t.Fatalf("GenerateKey: %v", err)
	}
	k.Kid = "test-kid"
	h.p.requestKey = &k
	s := h.newISOSession(t)
	return h.p, s
}

func TestSignedJARRoundTrip(t *testing.T) {
	p, s := jarHarness(t)
	out, err := p.signedRequest(s)
	if err != nil {
		t.Fatalf("signedRequest: %v", err)
	}
	jar, ok := out.(signedJAR)
	if !ok {
		t.Fatalf("signedRequest returned %T, want signedJAR", out)
	}
	payload, err := jose.VerifyJWSWithKey(jar.JWS, jar.JWK, jose.JWSTypObject)
	if err != nil {
		t.Fatalf("published jwk does not verify the JWS: %v", err)
	}
	if jar.JWK.Kid != p.requestKey.Kid {
		t.Fatalf("served jwk kid %q differs from the signing key's %q", jar.JWK.Kid, p.requestKey.Kid)
	}
	var req authorizationRequest
	if err := json.Unmarshal(payload, &req); err != nil {
		t.Fatalf("JWS payload is not a request object: %v", err)
	}
	if req.Nonce != s.Nonce || req.ClientID != s.ClientID {
		t.Fatalf("JAR body does not round-trip the session: nonce %q/%q client %q/%q",
			req.Nonce, s.Nonce, req.ClientID, s.ClientID)
	}
	if len(req.DCQLQuery.Credentials) == 0 {
		t.Fatal("JAR body lost the DCQL query")
	}
}

func TestSignedJARUntrustedKeyFails(t *testing.T) {
	p, s := jarHarness(t)
	out, err := p.signedRequest(s)
	if err != nil {
		t.Fatalf("signedRequest: %v", err)
	}
	jar := out.(signedJAR)
	other, err := jose.GenerateKey()
	if err != nil {
		t.Fatal(err)
	}
	if _, err := jose.VerifyJWSWithKey(jar.JWS, other, jose.JWSTypObject); err == nil {
		t.Fatal("a JAR signed by the verifier verified under a foreign key")
	}
}

func TestUnsignedRequestStaysBareObject(t *testing.T) {
	h := newISOHarness(t)
	s := h.newISOSession(t)
	out, err := h.p.signedRequest(s)
	if err != nil {
		t.Fatalf("signedRequest: %v", err)
	}
	if _, ok := out.(authorizationRequest); !ok {
		t.Fatalf("without a request key the request must stay a bare object, got %T", out)
	}
}

func TestRequestFetchIsSingleUse(t *testing.T) {
	p, s := jarHarness(t)
	rec := httptest.NewRecorder()
	p.handleRequest(rec, httptest.NewRequest(http.MethodGet, "/present/request/"+s.ID, nil))
	if rec.Code != http.StatusOK {
		t.Fatalf("first fetch: %d %s", rec.Code, rec.Body.String())
	}
	rec = httptest.NewRecorder()
	p.handleRequest(rec, httptest.NewRequest(http.MethodGet, "/present/request/"+s.ID, nil))
	if rec.Code != http.StatusGone {
		t.Fatalf("replayed fetch must be 410 Gone, got %d %s", rec.Code, rec.Body.String())
	}
}

func TestRequireTLSBaseURL(t *testing.T) {
	good := []string{
		"https://verifier.example.ee",
		"http://127.0.0.1:8080",
		"http://localhost:8080",
		"http://[::1]:8080",
	}
	for _, b := range good {
		if err := requireTLSBaseURL(b); err != nil {
			t.Errorf("requireTLSBaseURL(%q) = %v, want nil", b, err)
		}
	}
	bad := []string{
		"http://verifier.example.ee",
		"ftp://verifier.example.ee",
	}
	for _, b := range bad {
		if err := requireTLSBaseURL(b); err == nil {
			t.Errorf("requireTLSBaseURL(%q) accepted a non-https external base URL", b)
		} else if !strings.Contains(err.Error(), "https") && !strings.Contains(err.Error(), "scheme") {
			t.Errorf("unexpected error shape for %q: %v", b, err)
		}
	}
}
