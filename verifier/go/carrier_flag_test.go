package main

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"net/url"
	"os"
	"reflect"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/zk"
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

func assertResponseCarriers(t *testing.T, rec *httptest.ResponseRecorder, want ...string) {
	t.Helper()
	var body struct {
		Carriers []string `json:"carriers"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("response JSON: %v: %s", err, rec.Body.String())
	}
	if len(body.Carriers) != len(want) {
		t.Fatalf("response carriers = %v, want %v", body.Carriers, want)
	}
	for i := range want {
		if body.Carriers[i] != want[i] {
			t.Fatalf("response carriers = %v, want %v", body.Carriers, want)
		}
	}
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
	} else {
		assertResponseCarriers(t, rec, "interim-json")
	}

	// Forced interim carrier: same acceptance.
	p, s = plainCarrierHarness(t, oid4vp.InterimJSON{})
	if rec := postPlain(t, p, s, raw); rec.Code == http.StatusBadRequest {
		t.Fatalf("forced interim carrier refused the interim envelope: %s", rec.Body.String())
	} else {
		assertResponseCarriers(t, rec, "interim-json")
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
	assertResponseCarriers(t, rec, "mso-mdoc-zk-cbor")
}

// These refusals must finish before proof work, and polling must retain the
// same diagnosis returned to the holder.
func TestBindingFailureNamesAndPolling(t *testing.T) {
	original := zkVerify
	calls := 0
	zkVerify = func(zk.Request) error { calls++; return zk.ErrInvalid }
	t.Cleanup(func() { zkVerify = original })
	for _, tc := range []struct {
		name, want string
		status     int
		raw        []byte
	}{
		{"predicate", "predicate-value", http.StatusBadRequest, []byte(strings.Replace(string(plainEnvelope(t)), "f5", "f4", 1))},
		{"empty", "no-presentation", http.StatusBadRequest, []byte{0xa2, 0x66, 's', 't', 'a', 't', 'u', 's', 0x00, 0x6b, 'z', 'k', 'D', 'o', 'c', 'u', 'm', 'e', 'n', 't', 's', 0x80}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			p, s := plainCarrierHarness(t, nil)
			t.Cleanup(p.store.Stop)
			rec := postPlain(t, p, s, tc.raw)
			if rec.Code != tc.status || !strings.Contains(rec.Body.String(), tc.want) {
				t.Fatalf("refusal: %d %s", rec.Code, rec.Body.String())
			}
			var response map[string]any
			if err := json.Unmarshal(rec.Body.Bytes(), &response); err != nil {
				t.Fatal(err)
			}
			poll := httptest.NewRecorder()
			p.handleResult(poll, httptest.NewRequest("GET", "/present/result/"+s.ID, nil))
			var result map[string]any
			if err := json.Unmarshal(poll.Body.Bytes(), &result); err != nil {
				t.Fatal(err)
			}
			if result["detail"] != response["detail"] || result["valid"] != false || result["answered"] != true {
				t.Fatalf("poll: %v response: %v", result, response)
			}
			// Even a malformed body posted after an answer is single-use, without
			// re-verification or parsing a second presentation.
			replay := httptest.NewRecorder()
			p.handleResponse(replay, httptest.NewRequest("POST", "/present/response/"+s.ID, strings.NewReader("garbage")))
			if replay.Code != http.StatusGone || !strings.Contains(replay.Body.String(), "single-use") {
				t.Fatalf("replay: %d %s", replay.Code, replay.Body.String())
			}
		})
	}
	if calls != 0 {
		t.Fatalf("deterministic refusals performed %d proof calls", calls)
	}
}

func TestOpaqueProofFailureHasNeutralName(t *testing.T) {
	original := zkVerify
	zkVerify = func(zk.Request) error { return zk.ErrInvalid }
	t.Cleanup(func() { zkVerify = original })
	p, s := plainCarrierHarness(t, nil)
	t.Cleanup(p.store.Stop)
	rec := postPlain(t, p, s, plainEnvelope(t))
	if rec.Code != http.StatusOK || !strings.Contains(rec.Body.String(), "transcript-binding:") {
		t.Fatalf("opaque failure: %d %s", rec.Code, rec.Body.String())
	}
	for _, guessed := range []string{"client_id", "nonce", "response_uri", "jwk_thumbprint"} {
		if strings.Contains(rec.Body.String(), guessed) {
			t.Fatalf("opaque failure guessed %s: %s", guessed, rec.Body.String())
		}
	}
	poll := httptest.NewRecorder()
	p.handleResult(poll, httptest.NewRequest("GET", "/present/result/"+s.ID, nil))
	if !strings.Contains(poll.Body.String(), "transcript-binding:") {
		t.Fatalf("poll lost category: %s", poll.Body.String())
	}
}

func TestDecryptionFailureDoesNotParseOrVerify(t *testing.T) {
	original := zkVerify
	calls := 0
	zkVerify = func(zk.Request) error { calls++; return nil }
	t.Cleanup(func() { zkVerify = original })
	p, _ := plainCarrierHarness(t, nil)
	t.Cleanup(p.store.Stop)
	key, err := jose.GenerateKey()
	if err != nil {
		t.Fatal(err)
	}
	p.responseKey = &key
	tp, err := key.Thumbprint()
	if err != nil {
		t.Fatal(err)
	}
	q := oid4vp.AgeQuery("age_credential", "eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18")
	s, err := p.store.New("client", "https://example.com/present/response", q, tp)
	if err != nil {
		t.Fatal(err)
	}
	wrongKey, err := jose.GenerateKey()
	if err != nil {
		t.Fatal(err)
	}
	encrypted, err := jose.Encrypt(wrongKey.Public(), []byte("not JSON or a presentation"))
	if err != nil {
		t.Fatal(err)
	}
	req := httptest.NewRequest("POST", "/present/response/"+s.ID, strings.NewReader(url.Values{"response": {encrypted}}.Encode()))
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")
	rec := httptest.NewRecorder()
	p.handleResponse(rec, req)
	if rec.Code != http.StatusBadRequest || !strings.Contains(rec.Body.String(), "decryption:") {
		t.Fatalf("decrypt refusal: %d %s", rec.Code, rec.Body.String())
	}
	if calls != 0 {
		t.Fatalf("decryption failure ran %d proof calls", calls)
	}
	polled, err := p.store.Get(s.ID)
	if err != nil || polled.Answered || polled.Detail != "" {
		t.Fatalf("transport failure completed session: %+v %v", polled, err)
	}
}

func assertCompletedPolling(t *testing.T, p *presenter, s *oid4vp.Session, response *httptest.ResponseRecorder) map[string]any {
	t.Helper()
	var body map[string]any
	if err := json.Unmarshal(response.Body.Bytes(), &body); err != nil {
		t.Fatal(err)
	}
	poll := httptest.NewRecorder()
	p.handleResult(poll, httptest.NewRequest("GET", "/present/result/"+s.ID, nil))
	var result map[string]any
	if err := json.Unmarshal(poll.Body.Bytes(), &result); err != nil {
		t.Fatal(err)
	}
	if poll.Code != http.StatusOK || result["answered"] != true || result["valid"] != body["valid"] || result["detail"] != body["detail"] {
		t.Fatalf("polling differs: %d %v response: %v", poll.Code, result, body)
	}
	return result
}

func nativeCarrierHarness(t *testing.T, count int) (*presenter, *oid4vp.Session, []byte) {
	t.Helper()
	p, _ := plainCarrierHarness(t, nil)
	t.Cleanup(p.store.Stop)
	raw, meta := loadStep87Fixture(t)
	docs, err := oid4vp.ParseZkDocumentsBytes(raw)
	if err != nil || len(docs) != 1 {
		t.Fatalf("fixture: %v", err)
	}
	circuit := zkCircuitFor(t, meta.Version, meta.NumAttributes)
	query := oid4vp.ZkAgeQuery("age_credential", meta.DocType, meta.Namespace, meta.AttrID, circuit)
	if count == 2 {
		second := query.Credentials[0]
		second.ID = "later_credential"
		query.Credentials = append(query.Credentials, second)
	}
	p.maxCredentials = count
	p.sem = make(limiter, count)
	s, err := p.store.NewMulti("client", "https://example.com/present/response", query, count, nil)
	if err != nil {
		t.Fatal(err)
	}
	d := docs[0]
	return p, s, zkResponse(t, d.ZkSystemSpecID, time.Now().UTC().Format("2006-01-02T15:04:05Z"), d.MSOX5Chain, d.Proof)
}

func TestNativeZKFailureNamesAndPolling(t *testing.T) {
	original := zkVerify
	t.Cleanup(func() { zkVerify = original })
	for _, empty := range []bool{true, false} {
		t.Run(fmt.Sprintf("empty=%t", empty), func(t *testing.T) {
			calls := 0
			zkVerify = func(zk.Request) error { calls++; return zk.ErrInvalid }
			p, s, raw := nativeCarrierHarness(t, 1)
			want, wantCalls := "transcript-binding:", 1
			if empty {
				raw = []byte{0xa2, 0x66, 's', 't', 'a', 't', 'u', 's', 0x00, 0x6b, 'z', 'k', 'D', 'o', 'c', 'u', 'm', 'e', 'n', 't', 's', 0x80}
				want, wantCalls = "no-presentation:", 0
			}
			rec := postPlain(t, p, s, raw)
			if rec.Code != http.StatusOK || !strings.Contains(rec.Body.String(), want) {
				t.Fatalf("native refusal: %d %s", rec.Code, rec.Body.String())
			}
			result := assertCompletedPolling(t, p, s, rec)
			if result["valid"] != false || calls != wantCalls {
				t.Fatalf("result=%v proof calls=%d want=%d", result, calls, wantCalls)
			}
			if !empty {
				for _, guessed := range []string{"client_id", "nonce", "response_uri", "jwk_thumbprint"} {
					if strings.Contains(rec.Body.String(), guessed) {
						t.Fatalf("opaque CBOR failure guessed %s: %s", guessed, rec.Body.String())
					}
				}
			}
		})
	}
}

func TestMultiLaterDeterministicRefusalBeforeProof(t *testing.T) {
	original := zkVerify
	t.Cleanup(func() { zkVerify = original })
	for _, empty := range []bool{true, false} {
		t.Run(fmt.Sprintf("empty=%t", empty), func(t *testing.T) {
			calls := 0
			zkVerify = func(zk.Request) error { calls++; return nil }
			p, s, first := nativeCarrierHarness(t, 2)
			second := append([]byte(nil), first...)
			want := "predicate-value"
			if empty {
				second = []byte{0xa2, 0x66, 's', 't', 'a', 't', 'u', 's', 0x00, 0x6b, 'z', 'k', 'D', 'o', 'c', 'u', 'm', 'e', 'n', 't', 's', 0x80}
				want = "no-presentation"
			} else {
				old := append(tText("elementValue"), 0xf5)
				changed := append(tText("elementValue"), 0xf4)
				second = bytes.Replace(second, old, changed, 1)
				if bytes.Equal(first, second) {
					t.Fatal("predicate mutation did not change fixture")
				}
			}
			token := oid4vp.VPToken{
				"age_credential":   {base64.RawURLEncoding.EncodeToString(first)},
				"later_credential": {base64.RawURLEncoding.EncodeToString(second)},
			}
			// Establish that the first entry is independently parse-clean.
			if _, _, err := token.ParseCarrier(s.Query.Credentials[0], nil); err != nil {
				t.Fatalf("first entry: %v", err)
			}
			body, err := json.Marshal(map[string]any{"vp_token": token})
			if err != nil {
				t.Fatal(err)
			}
			req := httptest.NewRequest("POST", "/present/response/"+s.ID, bytes.NewReader(body))
			req.Header.Set("Content-Type", "application/json")
			rec := httptest.NewRecorder()
			p.handleResponse(rec, req)
			if rec.Code != http.StatusBadRequest || !strings.Contains(rec.Body.String(), want) || !strings.Contains(rec.Body.String(), "later_credential") {
				t.Fatalf("multi refusal: %d %s", rec.Code, rec.Body.String())
			}
			result := assertCompletedPolling(t, p, s, rec)
			if result["valid"] != false || calls != 0 {
				t.Fatalf("result=%v proof calls=%d", result, calls)
			}
		})
	}
}

func TestSuccessfulResponseReplayPreservesCompletedResult(t *testing.T) {
	original := zkVerify
	calls := 0
	zkVerify = func(zk.Request) error { calls++; return nil }
	t.Cleanup(func() { zkVerify = original })
	p, s := plainCarrierHarness(t, nil)
	t.Cleanup(p.store.Stop)
	first := postPlain(t, p, s, plainEnvelope(t))
	if first.Code != http.StatusOK {
		t.Fatalf("initial: %d %s", first.Code, first.Body.String())
	}
	before := assertCompletedPolling(t, p, s, first)
	if before["valid"] != true || calls != 1 {
		t.Fatalf("initial result=%v calls=%d", before, calls)
	}
	replay := postPlain(t, p, s, plainEnvelope(t))
	if replay.Code != http.StatusGone || !strings.Contains(replay.Body.String(), "single-use") {
		t.Fatalf("replay: %d %s", replay.Code, replay.Body.String())
	}
	after := assertCompletedPolling(t, p, s, first)
	if !reflect.DeepEqual(before, after) || calls != 1 {
		t.Fatalf("replay changed result: before=%v after=%v calls=%d", before, after, calls)
	}
}
