package oid4vp

import (
	"errors"
	"fmt"
	"sync"
	"testing"
	"time"
)

func testQuery() DCQL {
	return AgeQuery("proof_of_age", "eu.europa.ec.eudi.poa.1", "eu.europa.ec.eudi.poa.1", "age_over_18")
}

func TestStoreNewCleansExpiredSessionsBeforeEnforcingLimit(t *testing.T) {
	st := NewStore(time.Minute)
	for i := 0; i < maxSessions; i++ {
		id := fmt.Sprintf("session-%d", i)
		st.sessions[id] = &Session{ID: id, Created: time.Now()}
	}

	if _, err := st.New("verifier.example", "https://verifier.example/response", testQuery()); !errors.Is(err, ErrStoreFull) {
		t.Fatalf("New() error = %v, want %v", err, ErrStoreFull)
	}

	st.sessions["session-0"].Created = time.Now().Add(-2 * time.Minute)
	s, err := st.New("verifier.example", "https://verifier.example/response", testQuery())
	if err != nil {
		t.Fatalf("New() after expiry: %v", err)
	}
	if len(st.sessions) != maxSessions {
		t.Fatalf("session count = %d, want %d", len(st.sessions), maxSessions)
	}
	if s.ExpectedNow != s.Created.UTC().Format("2006-01-02T15:04:05Z") {
		t.Fatalf("ExpectedNow = %q, does not match Created", s.ExpectedNow)
	}
}

func TestStoreCompleteAndGetConcurrently(t *testing.T) {
	st := NewStore(time.Minute)
	s, err := st.New("verifier.example", "https://verifier.example/response", testQuery())
	if err != nil {
		t.Fatalf("New(): %v", err)
	}
	if _, err := st.Claim(s.ID); err != nil {
		t.Fatalf("Claim(): %v", err)
	}

	start := make(chan struct{})
	var wg sync.WaitGroup
	wg.Add(2)
	go func() {
		defer wg.Done()
		<-start
		for i := 0; i < 1_000; i++ {
			valid := i%2 == 0
			if _, err := st.Complete(s.ID, valid, fmt.Sprint(valid)); err != nil {
				t.Errorf("Complete(): %v", err)
				return
			}
		}
	}()
	go func() {
		defer wg.Done()
		<-start
		for i := 0; i < 1_000; i++ {
			got, err := st.Get(s.ID)
			if err != nil {
				t.Errorf("Get(): %v", err)
				return
			}
			if got.Detail != "" && got.Detail != fmt.Sprint(got.Valid) {
				t.Errorf("Get() returned partial result: %+v", got)
				return
			}
		}
	}()
	close(start)
	wg.Wait()
}

func TestStoreReturnsSnapshotsAndCompletesUnderLock(t *testing.T) {
	st := NewStore(time.Minute)
	created, err := st.New("verifier.example", "https://verifier.example/response", testQuery())
	if err != nil {
		t.Fatalf("New(): %v", err)
	}
	created.Valid = true
	created.Detail = "changed outside store"

	claimed, err := st.Claim(created.ID)
	if err != nil {
		t.Fatalf("Claim(): %v", err)
	}
	if claimed.Valid || claimed.Detail != "" {
		t.Fatalf("Claim() returned externally mutated result: %+v", claimed)
	}

	completed, err := st.Complete(created.ID, true, "verified")
	if err != nil {
		t.Fatalf("Complete(): %v", err)
	}
	completed.Detail = "changed snapshot"

	got, err := st.Get(created.ID)
	if err != nil {
		t.Fatalf("Get(): %v", err)
	}
	if !got.Valid || got.Detail != "verified" {
		t.Fatalf("Get() = %+v, want completed result", got)
	}
}

// NewWith keys the session on the response URI's last segment, the ID
// handleResponse reads from /present/response/<id>, and refuses to replace a
// live session with the same ID.
func TestStoreNewWithIDIsTheResponsePathSegment(t *testing.T) {
	st := NewStore(time.Minute)
	uri := "https://verifier.example.com/present/response/step87"
	s, err := st.NewWith("https://verifier.example.com", uri, "nonce", testQuery())
	if err != nil {
		t.Fatalf("NewWith: %v", err)
	}
	if s.ID != "step87" || s.ResponseURI != uri {
		t.Fatalf("ID %q, ResponseURI %q; want step87 and the full URI", s.ID, s.ResponseURI)
	}
	if _, err := st.NewWith("https://verifier.example.com", uri, "other", testQuery()); !errors.Is(err, ErrIDInUse) {
		t.Fatalf("second NewWith on the same URI: %v, want %v", err, ErrIDInUse)
	}
	if got, _ := st.Get("step87"); got == nil || got.Nonce != "nonce" {
		t.Fatal("the refused NewWith replaced the live session")
	}
	if _, err := st.NewWith("https://verifier.example.com", "https://verifier.example.com/", "n", testQuery()); err == nil {
		t.Fatal("a response URI with no final path segment was accepted")
	}
}
