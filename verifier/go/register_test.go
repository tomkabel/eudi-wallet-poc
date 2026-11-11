package main

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/rpregister"
	"log"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"testing"
	"time"
)

// Existing transcript vectors have fixed client identifiers. This explicit
// synthetic source uses the shared uses while binding to that fixture identity.
type fixtureRegister struct {
	record rpregister.RegisteredIntendedUse
}

func (f fixtureRegister) IntendedUse(id rpregister.RegisterIdentity) (rpregister.RegisteredIntendedUse, error) {
	r := f.record
	r.UniqueIdentifier = id.UniqueIdentifier
	r.ServiceIdentifier = id.ServiceIdentifier
	return r, nil
}
func testRegister(t *testing.T) rpregister.RPRegister {
	t.Helper()
	m, err := rpregister.LoadFile("../../rp-kit/register/synthetic-v1.json")
	if err != nil {
		t.Fatal(err)
	}
	r, err := m.IntendedUse(rpregister.RegisterIdentity{UniqueIdentifier: "x509_san_dns:verifier.example.ee", ServiceIdentifier: "age-verification"})
	if err != nil {
		t.Fatal(err)
	}
	return fixtureRegister{r}
}
func TestRegisterHTTPGates(t *testing.T) {
	h := newISOHarness(t)
	defer h.st.Stop()
	var logs bytes.Buffer
	old := log.Writer()
	log.SetOutput(&logs)
	defer log.SetOutput(old)
	for _, flow := range []string{"redirect", "dcapi"} {
		t.Run(flow, func(t *testing.T) {
			create := h.p.handleNew
			prefix := "/present"
			if flow == "dcapi" {
				create = h.p.handleDCAPINew
				prefix = "/present/dcapi"
			}
			call := func(handler http.HandlerFunc, path, body, method string) *httptest.ResponseRecorder {
				r := httptest.NewRecorder()
				handler(r, httptest.NewRequest(method, path, strings.NewReader(body)))
				return r
			}
			h.p.register = nil
			if r := call(create, prefix+"/new", "{}", "POST"); r.Code != 503 || !strings.Contains(r.Body.String(), "missing-register-prerequisite") {
				t.Fatalf("missing: %d %s", r.Code, r.Body)
			}
			m, err := rpregister.LoadFile("../../rp-kit/register/synthetic-v1.json")
			if err != nil {
				t.Fatal(err)
			}
			h.p.register = m
			if r := call(create, prefix+"/new", `{"element":"age_over_99"}`, "POST"); r.Code != 403 {
				t.Fatalf("unauthorized: %d %s", r.Code, r.Body)
			}
			r := call(create, prefix+"/new", "{}", "POST")
			if r.Code != 201 {
				t.Fatalf("positive: %d %s", r.Code, r.Body)
			}
			var session struct{ ID string }
			if err := json.Unmarshal(r.Body.Bytes(), &session); err != nil {
				t.Fatal(err)
			}
			h.p.register = nil
			if flow == "dcapi" {
				if r := call(h.p.handleDCAPIPage, prefix+"/"+session.ID, "", "GET"); r.Code != 503 {
					t.Fatalf("page gate: %d %s", r.Code, r.Body)
				}
			}
			fetch := h.p.handleRequest
			callback := h.p.handleResponse
			if flow == "dcapi" {
				fetch = h.p.handleDCAPIRequest
				callback = h.p.handleDCAPIResponse
			}
			if r := call(fetch, prefix+"/request/"+session.ID, "", "GET"); r.Code != 503 {
				t.Fatalf("release gate: %d %s", r.Code, r.Body)
			}
			if r := call(callback, prefix+"/response/"+session.ID, `{"vp_token":{},"response":"AA"}`, "POST"); r.Code != 503 {
				t.Fatalf("callback gate: %d %s", r.Code, r.Body)
			}
			s, err := h.st.Get(session.ID)
			if err != nil || s.Answered {
				t.Fatalf("denied callback consumed session: %v", err)
			}
			h.p.register = m
			if r := call(fetch, prefix+"/request/"+session.ID, "", "GET"); r.Code != 200 {
				t.Fatalf("allowed release: %d %s", r.Code, r.Body)
			}
			original := h.p.clientID
			h.p.clientID = "x509_san_dns:omitted.example"
			if r := call(create, prefix+"/new", "{}", "POST"); r.Code != 403 {
				t.Fatalf("absence: %d %s", r.Code, r.Body)
			}
			h.p.clientID = original
		})
	}
	if !strings.Contains(logs.String(), "missing-register-prerequisite") || !strings.Contains(logs.String(), "register-not-found") {
		t.Fatal(logs.String())
	}
}
func TestRegisterAllClaims(t *testing.T) {
	p := &presenter{register: testRegister(t), clientID: "fixture", registerService: "age-verification"}
	q := oid4vp.AgeQuery("a", "ee.riik.poa.1", "ee.riik.poa.1", "age_over_18")
	if err := p.authorizeRegister("fixture", q); err != nil {
		t.Fatal(err)
	}
	q.Credentials = append(q.Credentials, oid4vp.AgeQuery("b", "eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_99").Credentials...)
	if p.authorizeRegister("fixture", q) == nil {
		t.Fatal("unauthorized second credential allowed")
	}
	q.Credentials = q.Credentials[:1]
	q.Credentials[0].Claims = append(q.Credentials[0].Claims, oid4vp.ClaimPath{Path: []string{"wrong.namespace", "age_over_18"}})
	if p.authorizeRegister("fixture", q) == nil {
		t.Fatal("unauthorized second claim allowed")
	}
}
func TestRegisterConfiguredFailuresHTTP(t *testing.T) {
	var logs bytes.Buffer
	old := log.Writer()
	log.SetOutput(&logs)
	defer log.SetOutput(old)
	defer func() {
		if !strings.Contains(logs.String(), "reason=register-error") {
			t.Fatal("configured failure log missing", logs.String())
		}
	}()
	h := newISOHarness(t)
	defer h.st.Stop()
	for _, mode := range []string{"5xx", "timeout", "connection"} {
		t.Run(mode, func(t *testing.T) {
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if mode == "timeout" {
					time.Sleep(40 * time.Millisecond)
				}
				w.WriteHeader(503)
			}))
			defer server.Close()
			if mode == "connection" {
				server.Close()
			}
			source, err := rpregister.NewHTTP(server.URL, 10*time.Millisecond)
			if err != nil {
				t.Fatal(err)
			}
			h.p.register = source
			for _, handler := range []http.HandlerFunc{h.p.handleNew, h.p.handleDCAPINew} {
				rec := httptest.NewRecorder()
				handler(rec, httptest.NewRequest("POST", "/", strings.NewReader("{}")))
				if rec.Code != 503 || !strings.Contains(rec.Body.String(), "register-error") {
					t.Fatalf("%d %s", rec.Code, rec.Body)
				}
			}
		})
	}
}

func TestRegisterFreshStaleHTTP(t *testing.T) {
	for _, flow := range []string{"redirect", "dcapi"} {
		for _, outcome := range []string{"failure", "not-found", "unauthorized"} {
			t.Run(flow+"/"+outcome, func(t *testing.T) {
				h := newISOHarness(t)
				defer h.st.Stop()
				mock, err := rpregister.LoadFile("../../rp-kit/register/synthetic-v1.json")
				if err != nil {
					t.Fatal(err)
				}
				mode := "positive"
				var lock sync.Mutex
				server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
					lock.Lock()
					m := mode
					lock.Unlock()
					if m == "not-found" {
						w.WriteHeader(404)
						return
					}
					if m == "failure" {
						w.WriteHeader(503)
						return
					}
					id := rpregister.RegisterIdentity{UniqueIdentifier: r.URL.Query().Get("uniqueIdentifier"), ServiceIdentifier: r.URL.Query().Get("serviceIdentifier")}
					record, err := mock.IntendedUse(id)
					if err != nil {
						w.WriteHeader(404)
						return
					}
					if m == "unauthorized" {
						record.IntendedUses = []rpregister.DoctypePredicate{{Doctype: "ee.riik.poa.1", Predicate: "age_over_65"}}
					}
					_ = json.NewEncoder(w).Encode(record)
				}))
				defer server.Close()
				source, err := rpregister.NewHTTP(server.URL, time.Second)
				if err != nil {
					t.Fatal(err)
				}
				var logs bytes.Buffer
				cache, err := rpregister.NewCache(source, 2*time.Second, 2, func(f string, a ...any) { fmt.Fprintf(&logs, f, a...) })
				if err != nil {
					t.Fatal(err)
				}
				h.p.register = cache
				create := h.p.handleNew
				prefix := "/present"
				fetch := h.p.handleRequest
				callback := h.p.handleResponse
				if flow == "dcapi" {
					create = h.p.handleDCAPINew
					prefix = "/present/dcapi"
					fetch = h.p.handleDCAPIRequest
					callback = h.p.handleDCAPIResponse
				}
				rec := httptest.NewRecorder()
				create(rec, httptest.NewRequest("POST", prefix+"/new", strings.NewReader("{}")))
				if rec.Code != 201 {
					t.Fatal(rec.Body)
				}
				var session struct{ ID string }
				_ = json.Unmarshal(rec.Body.Bytes(), &session)
				lock.Lock()
				mode = outcome
				lock.Unlock()
				rec = httptest.NewRecorder()
				fetch(rec, httptest.NewRequest("GET", prefix+"/request/"+session.ID, nil))
				if rec.Code != 200 {
					t.Fatalf("fresh deny: %d %s", rec.Code, rec.Body)
				}
				if !strings.Contains(logs.String(), "cache-hit-within-ttl") {
					t.Fatal("fresh cache observation missing")
				}
				time.Sleep(2100 * time.Millisecond)
				rec = httptest.NewRecorder()
				callback(rec, httptest.NewRequest("POST", prefix+"/response/"+session.ID, strings.NewReader("{\"vp_token\":{},\"response\":\"AA\"}")))
				want := 503
				if outcome != "failure" {
					want = 403
				}
				if rec.Code != want {
					t.Fatalf("stale allowed: %d %s", rec.Code, rec.Body)
				}
				stored, err := h.st.Get(session.ID)
				if err != nil || stored.Answered {
					t.Fatalf("denial claimed session: %v", err)
				}
				if !strings.Contains(logs.String(), "cache-hit-within-ttl") || !strings.Contains(logs.String(), "stale-discard-relookup") {
					t.Fatal(logs.String())
				}
			})
		}
	}
}

type registerFunc func(rpregister.RegisterIdentity) (rpregister.RegisteredIntendedUse, error)

func (f registerFunc) IntendedUse(id rpregister.RegisterIdentity) (rpregister.RegisteredIntendedUse, error) {
	return f(id)
}
func TestRegisterNilAndSensitiveErrors(t *testing.T) {
	h := newISOHarness(t)
	defer h.st.Stop()
	for _, source := range []rpregister.RPRegister{registerFunc(nil), registerFunc(func(rpregister.RegisterIdentity) (rpregister.RegisteredIntendedUse, error) {
		return rpregister.RegisteredIntendedUse{}, errors.New("secret-token=holder-private")
	})} {
		h.p.register = source
		var logs bytes.Buffer
		old := log.Writer()
		log.SetOutput(&logs)
		r := httptest.NewRecorder()
		h.p.handleNew(r, httptest.NewRequest("POST", "/present/new", strings.NewReader("{}")))
		log.SetOutput(old)
		if r.Code != 503 || strings.Contains(r.Body.String(), "secret-token") || strings.Contains(logs.String(), "secret-token") {
			t.Fatalf("leaked/panicked: %d %s %s", r.Code, r.Body, logs.String())
		}
	}
}
func TestRegisterMultiCreationRefusal(t *testing.T) {
	h := newISOHarness(t)
	defer h.st.Stop()
	h.p.demoCredentials = 2
	h.p.maxCredentials = 2
	base := testRegister(t)
	id := rpregister.RegisterIdentity{UniqueIdentifier: h.p.clientID, ServiceIdentifier: h.p.registerService}
	record, err := base.IntendedUse(id)
	if err != nil {
		t.Fatal(err)
	}
	record.IntendedUses = []rpregister.DoctypePredicate{{Doctype: h.p.docType, Predicate: "age_over_18"}}
	for _, source := range []rpregister.RPRegister{nil, fixtureRegister{record}} {
		h.p.register = source
		before := h.st.LiveSessions()
		rec := httptest.NewRecorder()
		h.p.handleNew(rec, httptest.NewRequest("POST", "/present/new", strings.NewReader("{}")))
		if (rec.Code != 503 && rec.Code != 403) || h.st.LiveSessions() != before || strings.Contains(rec.Body.String(), "request_uri") {
			t.Fatalf("multi created: %d %s", rec.Code, rec.Body)
		}
	}
}
func TestRegisterStoredPolicy(t *testing.T) {
	for _, surface := range []string{"redirect-request", "redirect-callback", "dcapi-page", "dcapi-request", "dcapi-callback"} {
		for _, policy := range []string{"client", "query"} {
			t.Run(surface+"/"+policy, func(t *testing.T) {
				h := newISOHarness(t)
				defer h.st.Stop()
				client := h.p.clientID
				q := oid4vp.AgeQuery("proof_of_age", h.p.docType, h.p.nsID, "age_over_18")
				if policy == "client" {
					client = "x509_san_dns:wrong.example"
				} else {
					q.Credentials[0].Claims[0].Path[1] = "age_over_99"
				}
				if err := h.p.authorizeRegister(h.p.clientID, oid4vp.AgeQuery("default", h.p.docType, h.p.nsID, "age_over_18")); err != nil {
					t.Fatal("default not authorized", err)
				}
				var s *oid4vp.Session
				var err error
				if strings.HasPrefix(surface, "dcapi") {
					s, err = h.st.NewISO(client, h.p.baseURL+"/response", q, h.p.dcapiOrigin, h.p.offered)
				} else {
					s, err = h.st.New(client, h.p.baseURL+"/response", q, nil)
				}
				if err != nil {
					t.Fatal(err)
				}
				handler := h.p.handleRequest
				path := "/present/request/" + s.ID
				method := "GET"
				body := ""
				switch surface {
				case "redirect-callback":
					handler = h.p.handleResponse
					path = "/present/response/" + s.ID
					method = "POST"
					body = `{"vp_token":{}}`
				case "dcapi-page":
					handler = h.p.handleDCAPIPage
					path = "/present/dcapi/" + s.ID
				case "dcapi-request":
					handler = h.p.handleDCAPIRequest
					path = "/present/dcapi/request/" + s.ID
				case "dcapi-callback":
					handler = h.p.handleDCAPIResponse
					path = "/present/dcapi/response/" + s.ID
					method = "POST"
					body = `{"response":"AA"}`
				}
				rec := httptest.NewRecorder()
				handler(rec, httptest.NewRequest(method, path, strings.NewReader(body)))
				if rec.Code != 403 || strings.Contains(rec.Body.String(), "encryption_info") || strings.Contains(rec.Body.String(), "nonce") || strings.Contains(rec.Body.String(), "valid\":true") {
					t.Fatalf("stored policy: %d %s", rec.Code, rec.Body)
				}
				stored, err := h.st.Get(s.ID)
				if err != nil || stored.Answered {
					t.Fatalf("denied session claimed: %v", err)
				}
			})
		}
	}
}
func TestRegisterDCAPIExpiresDuringLookup(t *testing.T) {
	for _, surface := range []string{"page", "request"} {
		t.Run(surface, func(t *testing.T) {
			h := newISOHarness(t)
			defer h.st.Stop()
			st := oid4vp.NewStore(50 * time.Millisecond)
			defer st.Stop()
			h.p.store = st
			q := oid4vp.AgeQuery("proof_of_age", h.p.docType, h.p.nsID, "age_over_18")
			s, err := st.NewISO(h.p.clientID, h.p.baseURL+"/response", q, h.p.dcapiOrigin, h.p.offered)
			if err != nil {
				t.Fatal(err)
			}
			entered := make(chan struct{})
			release := make(chan struct{})
			source := h.p.register
			h.p.register = registerFunc(func(id rpregister.RegisterIdentity) (rpregister.RegisteredIntendedUse, error) {
				close(entered)
				<-release
				return source.IntendedUse(id)
			})
			handler := h.p.handleDCAPIPage
			path := "/present/dcapi/" + s.ID
			if surface == "request" {
				handler = h.p.handleDCAPIRequest
				path = "/present/dcapi/request/" + s.ID
			}
			rec := httptest.NewRecorder()
			done := make(chan struct{})
			go func() { defer close(done); handler(rec, httptest.NewRequest("GET", path, nil)) }()
			<-entered
			deadline := time.Now().Add(time.Second)
			for {
				_, err = st.Get(s.ID)
				if errors.Is(err, oid4vp.ErrExpired) {
					break
				}
				if time.Now().After(deadline) {
					close(release)
					<-done
					t.Fatal("session did not expire")
				}
				time.Sleep(time.Millisecond)
			}
			close(release)
			<-done
			if rec.Code != 410 || strings.Contains(rec.Body.String(), "encryption_info") || strings.Contains(rec.Body.String(), "<html") {
				t.Fatalf("expired release: %d %s", rec.Code, rec.Body)
			}
		})
	}
}
