package main

import (
	"crypto/ed25519"
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/zk"
	"net/http"
	"net/http/httptest"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/rpregister"
)

// The real handlers must reject an absent exact RP before allocating either
// session shape. HTTP process controls additionally exercise the staged profile.
func TestEstonianAbsentRegistrationNoSession(t *testing.T) {
	h := newISOHarness(t)
	defer h.st.Stop()
	register, err := rpregister.LoadFile("../../rp-kit/register/synthetic-v1.json")
	if err != nil {
		t.Fatal(err)
	}
	h.p.register = register
	h.p.clientID = "x509_san_dns:omitted.example.ee"
	for _, route := range []string{"/present/new", "/present/dcapi/new"} {
		before := h.st.LiveSessions()
		response := httptest.NewRecorder()
		request := httptest.NewRequest("POST", route, strings.NewReader("{}"))
		if route == "/present/new" {
			h.p.handleNew(response, request)
		} else {
			h.p.handleDCAPINew(response, request)
		}
		var body map[string]any
		if err := json.Unmarshal(response.Body.Bytes(), &body); err != nil {
			t.Fatal(err)
		}
		if response.Code != 403 || body["reason"] != "register-not-found" || h.st.LiveSessions() != before {
			t.Fatalf("route %s released a session or wrong refusal: %d %s", route, response.Code, response.Body)
		}
		if _, exists := body["id"]; exists {
			t.Fatal("refusal exposed a session id")
		}
	}
	t.Log(`ESTONIAN_REGISTRATION={"routes":2,"live_sessions_before":0,"live_sessions_after":0,"outcome":"register-not-found"}`)
}

// Cross expiry after session creation without a sleep or a wall-clock race.
// Old startup offers and ext.BySpecID must not extend the signed document.
func TestEstonianSignedRegistryExpiryAtHandlers(t *testing.T) {
	h := newISOHarness(t)
	defer h.st.Stop()
	bytes, err := os.ReadFile("../circuits.json")
	if err != nil {
		t.Fatal(err)
	}
	var doc circuits.Document
	if err := json.Unmarshal(bytes, &doc); err != nil {
		t.Fatal(err)
	}
	pub, key, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	now := time.Now().UTC()
	doc.IssuedAt = now.Add(-time.Hour).Format(time.RFC3339Nano)
	expiry := now.Add(time.Hour)
	doc.ExpiresAt = expiry.Format(time.RFC3339Nano)
	doc.Signatures = nil
	bytes, err = json.Marshal(doc)
	if err != nil {
		t.Fatal(err)
	}
	payload, err := circuits.CanonicalPayload(bytes)
	if err != nil {
		t.Fatal(err)
	}
	doc.Signatures = []circuits.Signature{{KeyID: "test", Sig: base64.RawURLEncoding.EncodeToString(ed25519.Sign(key, payload))}}
	bytes, err = json.Marshal(doc)
	if err != nil {
		t.Fatal(err)
	}
	public, err := json.Marshal(circuits.PublicKey{KeyID: "test", Key: base64.RawURLEncoding.EncodeToString(pub)})
	if err != nil {
		t.Fatal(err)
	}
	clock := func() time.Time { return now }
	reg, err := circuits.ValidateDocumentWithClock(bytes, public, doc.MinLibraryVersion, clock, clock)
	if err != nil {
		t.Fatal(err)
	}
	h.p.registry = reg
	h.p.offered = reg.Accepted()
	session := h.newISOSession(t)
	ext := session.SessionISO()
	// Use the offered spec in an encrypted, parsed response so the check is
	// exercised through the HTTP handler before timestamp/trust/native proving.
	plain := zkResponse(t, specID71, now.Format(time.RFC3339), []byte{0x30}, []byte{0x01})
	body := dcapiWrap(t, ext.HPKEPrivate, mustTranscript(t, session), plain)
	originalVerify := zkVerify
	calls := 0
	zkVerify = func(zk.Request) error { calls++; return nil }
	defer func() { zkVerify = originalVerify }()
	now = expiry
	for _, route := range []string{"/present/new", "/present/dcapi/new"} {
		before := h.st.LiveSessions()
		rec := httptest.NewRecorder()
		request := httptest.NewRequest(http.MethodPost, route, strings.NewReader("{}"))
		if route == "/present/new" {
			h.p.handleNew(rec, request)
		} else {
			h.p.handleDCAPINew(rec, request)
		}
		if rec.Code != http.StatusServiceUnavailable || !strings.Contains(rec.Body.String(), "stale-list") || h.st.LiveSessions() != before {
			t.Fatalf("expired creation %s: %d %s, sessions %d -> %d", route, rec.Code, rec.Body, before, h.st.LiveSessions())
		}
	}
	rec := httptest.NewRecorder()
	h.p.handleDCAPIRequest(rec, httptest.NewRequest(http.MethodGet, "/present/dcapi/request/"+session.ID, nil))
	if rec.Code != http.StatusServiceUnavailable || !strings.Contains(rec.Body.String(), "stale-list") {
		t.Fatalf("expired request: %d %s", rec.Code, rec.Body)
	}
	response := h.post(t, session.ID, body)
	if response.Code != http.StatusForbidden || !strings.Contains(response.Body.String(), "stale-list") || calls != 0 {
		t.Fatalf("expired response: %d %s, native calls %d", response.Code, response.Body, calls)
	}
}
