package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
)

func TestResultRoutesMissingCallback(t *testing.T) {
	for _, route := range []string{"/present/result/", "/present/dcapi/result/"} {
		t.Run(route, func(t *testing.T) {
			st := oid4vp.NewStore(1500 * time.Millisecond)
			defer st.Stop()
			p := &presenter{store: st}
			handler := p.handleResult
			if strings.Contains(route, "dcapi") {
				handler = p.handleDCAPIResult
			}
			q := oid4vp.AgeQuery("proof_of_age", "ee.riik.poa.1", "ee.riik.poa.1", "age_over_18")
			s, err := st.New("rp", "https://rp.example/response", q, nil)
			if err != nil {
				t.Fatal(err)
			}
			check := func(id string, status int, expired bool) {
				t.Helper()
				w := httptest.NewRecorder()
				handler(w, httptest.NewRequest(http.MethodGet, route+id, nil))
				var body map[string]any
				if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
					t.Fatal(err)
				}
				if w.Code != status || body["id"] != id {
					t.Fatalf("%d %s", w.Code, w.Body.String())
				}
				if expired && body["error"] != oid4vp.ErrExpired.Error() {
					t.Fatalf("expiry: %v", body)
				}
				if status == http.StatusOK && body["answered"] != false {
					t.Fatalf("pending: %v", body)
				}
			}
			check(s.ID, http.StatusOK, false)
			check("unknown", http.StatusNotFound, false)
			time.Sleep(time.Until(s.Created.Add(1600 * time.Millisecond)))
			check(s.ID, http.StatusGone, true)
			check(s.ID, http.StatusGone, true)
			time.Sleep(500 * time.Millisecond) // janitor tick has also passed
			check(s.ID, http.StatusGone, true)
		})
	}
}
