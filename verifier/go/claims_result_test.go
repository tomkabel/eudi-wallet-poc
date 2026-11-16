package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
)

// The result surface must hand a relying party a boolean, not a proof to parse.
// A zero-knowledge AV presentation proves age_over_18; a disclosed presentation
// does not prove anything and must not be distilled.

func avQuery() oid4vp.DCQL {
	return oid4vp.AgeQuery("proof_of_age", "ee.riik.poa.1", "ee.riik.poa.1", "age_over_18")
}

func TestProvenClaimsDistilsZKAgePredicate(t *testing.T) {
	got := provenClaims(avQuery(), true, []string{"mso-mdoc-zk-cbor"})
	if got == nil || got["age_over_18"] != true {
		t.Fatalf("provenClaims(zk, valid) = %v, want {age_over_18:true}", got)
	}
	if len(got) != 1 {
		t.Fatalf("provenClaims invented extra claims: %v", got)
	}
}

func TestProvenClaimsRefusesWhatWasNotProved(t *testing.T) {
	cases := []struct {
		name     string
		q        oid4vp.DCQL
		valid    bool
		carriers []string
	}{
		{"invalid proof", avQuery(), false, []string{"mso-mdoc-zk-cbor"}},
		{"disclosed presentation", avQuery(), true, []string{"interim-json"}},
		{"mixed carriers", avQuery(), true, []string{"mso-mdoc-zk-cbor", "interim-json"}},
		{"no carrier", avQuery(), true, nil},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			if got := provenClaims(c.q, c.valid, c.carriers); got != nil {
				t.Fatalf("provenClaims = %v, want nil", got)
			}
		})
	}
}

func TestProvenClaimsIgnoresNonPredicateAttributes(t *testing.T) {
	q := oid4vp.AgeQuery("proof_of_age", "ee.riik.poa.1", "ee.riik.poa.1", "family_name")
	if got := provenClaims(q, true, []string{"mso-mdoc-zk-cbor"}); got != nil {
		t.Fatalf("distilled a non-predicate attribute into a claim: %v", got)
	}
}

// TestResultExposesProvenClaims drives the handler the relying party polls.
func TestResultExposesProvenClaims(t *testing.T) {
	st := oid4vp.NewStore(5 * time.Second)
	defer st.Stop()
	p := &presenter{store: st}
	s, err := st.New("rp", "https://rp.example/response", avQuery(), nil)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := st.Claim(s.ID); err != nil {
		t.Fatal(err)
	}
	if _, err := st.CompleteWithClaims(s.ID, true, "age_over_18 proved in zero knowledge",
		map[string]any{"age_over_18": true}); err != nil {
		t.Fatal(err)
	}
	w := httptest.NewRecorder()
	p.handleResult(w, httptest.NewRequest(http.MethodGet, "/present/result/"+s.ID, nil))
	if w.Code != http.StatusOK {
		t.Fatalf("status %d: %s", w.Code, w.Body.String())
	}
	var body struct {
		Valid  bool           `json:"valid"`
		Claims map[string]any `json:"claims"`
	}
	if err := json.Unmarshal(w.Body.Bytes(), &body); err != nil {
		t.Fatal(err)
	}
	if !body.Valid || body.Claims["age_over_18"] != true {
		t.Fatalf("result = %s, want valid + claims.age_over_18=true", w.Body.String())
	}

	// A session with no proven predicate must omit the field entirely.
	plain, err := st.New("rp", "https://rp.example/response", avQuery(), nil)
	if err != nil {
		t.Fatal(err)
	}
	w2 := httptest.NewRecorder()
	p.handleResult(w2, httptest.NewRequest(http.MethodGet, "/present/result/"+plain.ID, nil))
	if _, present := jsonBody(t, w2)["claims"]; present {
		t.Fatalf("unanswered session exposed claims: %s", w2.Body.String())
	}
}

func jsonBody(t *testing.T, w *httptest.ResponseRecorder) map[string]any {
	t.Helper()
	var m map[string]any
	if err := json.Unmarshal(w.Body.Bytes(), &m); err != nil {
		t.Fatal(err)
	}
	return m
}
