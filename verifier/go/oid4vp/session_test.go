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

// expiredFullStore builds a store holding maxSessions entries whose Created
// stamps all lie ttl in the past by the fake clock, plus a fake clock that
// starts at base and is moved by advance.
func expiredFullStore(ttl time.Duration) (*Store, func(time.Duration)) {
	base := time.Unix(1_700_000_000, 0)
	st := NewStore(ttl)
	st.now = func() time.Time { return base }
	for i := 0; i < maxSessions; i++ {
		id := fmt.Sprintf("session-%d", i)
		st.sessions[id] = &Session{ID: id, Created: base}
	}
	clock := base
	st.now = func() time.Time { return clock }
	return st, func(d time.Duration) { clock = clock.Add(d) }
}

func TestStoreCompleteAndGetConcurrently(t *testing.T) {
	st := NewStore(time.Minute)
	s, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil)
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
	created, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil)
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
	s, err := st.NewWith("https://verifier.example.com", uri, "nonce", testQuery(), nil)
	if err != nil {
		t.Fatalf("NewWith: %v", err)
	}
	if s.ID != "step87" || s.ResponseURI != uri {
		t.Fatalf("ID %q, ResponseURI %q; want step87 and the full URI", s.ID, s.ResponseURI)
	}
	if _, err := st.NewWith("https://verifier.example.com", uri, "other", testQuery(), nil); !errors.Is(err, ErrIDInUse) {
		t.Fatalf("second NewWith on the same URI: %v, want %v", err, ErrIDInUse)
	}
	if got, _ := st.Get("step87"); got == nil || got.Nonce != "nonce" {
		t.Fatal("the refused NewWith replaced the live session")
	}
	if _, err := st.NewWith("https://verifier.example.com", "https://verifier.example.com/", "n", testQuery(), nil); err == nil {
		t.Fatal("a response URI with no final path segment was accepted")
	}
}

// A session created with a thumbprint is an encrypted-response session: the
// transcript gains the thumbprint element and plain responses are refused.
// Both constructors must pin this, and nil must leave the plain mode intact.
func TestStoreThumbprintSetsEncryptedResponse(t *testing.T) {
	tp := make([]byte, 32)
	for i := range tp {
		tp[i] = byte(i)
	}

	st := NewStore(time.Minute)
	plain, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil)
	if err != nil {
		t.Fatalf("New(nil): %v", err)
	}
	if plain.RequireEncryptedResponse || plain.JWKThumbprint() != nil {
		t.Fatal("New(nil) produced an encrypted-response session")
	}
	enc, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), tp)
	if err != nil {
		t.Fatalf("New(tp): %v", err)
	}
	if !enc.RequireEncryptedResponse {
		t.Fatal("New(tp) did not require encrypted responses")
	}

	// NewWith: both sides pinned, same nonce and URI as the fixture tests use.
	uri := "https://verifier.example.com/present/response/tp87"
	st2 := NewStore(time.Minute)
	enc2, err := st2.NewWith("https://verifier.example.com", uri, "nonce", testQuery(), tp)
	if err != nil {
		t.Fatalf("NewWith(tp): %v", err)
	}
	if !enc2.RequireEncryptedResponse || enc2.JWKThumbprint() == nil {
		t.Fatal("NewWith(tp) did not set the encrypted response mode")
	}
	plain2, err := st2.NewWith("https://verifier.example.com", uri+"2", "nonce", testQuery(), nil)
	if err != nil {
		t.Fatalf("NewWith(nil): %v", err)
	}
	if plain2.RequireEncryptedResponse {
		t.Fatal("NewWith(nil) required encrypted responses")
	}

	// The transcripts must differ exactly by the thumbprint element, and the
	// encrypted one must equal the direct SessionTranscript call.
	base, err := SessionTranscript(plain.ClientID, plain.Nonce, nil, plain.ResponseURI)
	if err != nil {
		t.Fatalf("SessionTranscript(nil): %v", err)
	}
	if got, _ := plain.Transcript(); string(got) != string(base) {
		t.Fatal("the plain session's transcript differs from the nil-thumbprint call")
	}
	bound, err := SessionTranscript(enc.ClientID, enc.Nonce, tp, enc.ResponseURI)
	if err != nil {
		t.Fatalf("SessionTranscript(tp): %v", err)
	}
	if got, _ := enc.Transcript(); string(got) != string(bound) {
		t.Fatal("the encrypted session's transcript differs from the thumbprint-bound call")
	}
	if string(base) == string(bound) {
		t.Fatal("the thumbprint did not reach the transcript")
	}
}

// A thumbprint that is not exactly a SHA-256 digest is a configuration error,
// refused at session creation — not a session that fails per presentation.
func TestStoreRejectsWrongThumbprintSize(t *testing.T) {
	st := NewStore(time.Minute)
	if _, err := st.New("v", "https://v/response", testQuery(), []byte{1, 2, 3}); err == nil {
		t.Fatal("New accepted a 3-byte thumbprint")
	}
	if _, err := st.NewWith("v", "https://v/response/tp", "n", testQuery(), make([]byte, 31)); err == nil {
		t.Fatal("NewWith accepted a 31-byte thumbprint")
	}
}

// storeLen reads the session count under the lock: the janitor sweeps
// concurrently, so tests must never read the map bare (-race catches it).
func storeLen(st *Store) int {
	st.mu.Lock()
	defer st.mu.Unlock()
	return len(st.sessions)
}

// TestStoreJanitorReapsExpiredWithoutPutPenalty: with every one of maxSessions
// entries expired on a fake clock, the janitor's tick reclaims them all and
// put stays O(1) — the new session is admitted immediately after the tick,
// which the old sweep-on-insert behaviour satisfied differently (it swept
// inside put) but now costs no scan on any insert. The fake clock decides
// expiry, so no real time passes beyond the janitor's tick interval.
func TestStoreJanitorReapsExpiredWithoutPutPenalty(t *testing.T) {
	ttl := 10 * time.Second              // janitor tick = ttl/4 = 2.5s (1s floor no-ops)
	st, advance := expiredFullStore(ttl) // 10,000 entries, all expired by the fake clock
	t.Cleanup(st.Stop)

	// Full of expired entries: a put is refused — put does not sweep.
	if _, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil); !errors.Is(err, ErrStoreFull) {
		t.Fatalf("New() error = %v, want %v", err, ErrStoreFull)
	}

	// One tick: the janitor reaps every expired entry, under lock.
	advance(ttl + 1) // the entries' Created lapses (they were built pre-tick)
	deadline := time.Now().Add(10 * time.Second)
	for storeLen(st) != 0 {
		if time.Now().After(deadline) {
			t.Fatalf("janitor did not reap the expired entries (%d left)", storeLen(st))
		}
		time.Sleep(5 * time.Millisecond)
	}

	// O(1) insert: the 10,001st session is admitted with no O(n) penalty.
	s, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil)
	if err != nil {
		t.Fatalf("New after janitor reap: %v", err)
	}
	if n := storeLen(st); n != 1 {
		t.Fatalf("session count = %d, want 1", n)
	}
	if s.ExpectedNow != s.Created.UTC().Format("2006-01-02T15:04:05Z") {
		t.Fatalf("ExpectedNow = %q, does not match Created", s.ExpectedNow)
	}
}

// TestStoreStopEndsJanitor: Stop terminates the janitor goroutine and is
// idempotent; after Stop, a lapsed session is no longer reaped on its own.
// The session lives on the fake timeline (Created = the fake now), because
// Session.Expired itself reads the wall clock.
func TestStoreStopEndsJanitor(t *testing.T) {
	ttl := time.Second
	base := time.Unix(1_700_000_000, 0)
	st := NewStore(ttl)
	st.now = func() time.Time { return base }
	st.sessions["s"] = &Session{ID: "s", Created: base}
	st.Stop()
	st.Stop() // idempotent

	st.mu.Lock()
	n := len(st.sessions)
	st.mu.Unlock()
	if n != 1 {
		t.Fatalf("session count = %d, want 1", n)
	}

	// After Stop the janitor is gone: a lapsed session stays until something
	// drives the sweep by hand. (One tick's worth of reap, with no goroutine.)
	st.now = func() time.Time { return base.Add(2 * ttl) }
	st.sweep()
	st.mu.Lock()
	n = len(st.sessions)
	st.mu.Unlock()
	if n != 0 {
		t.Fatal("sweep did not reap the lapsed session")
	}
}

// TestStoreFullRefusesUntilJanitorSweeps: insert never ranges the map — the janitor owns
// expiry — so a full store refuses with ErrStoreFull until the janitor's tick
// reclaims the expired slots (TestStoreJanitorReaps...), and the only expiry
// decision put makes is about the session being inserted.
func TestStoreFullRefusesUntilJanitorSweeps(t *testing.T) {
	st := NewStore(time.Minute)
	t.Cleanup(st.Stop)
	live := &Session{ID: "live", Created: time.Now()}
	st.sessions["live"] = live
	st.sessions["stale"] = &Session{ID: "stale", Created: time.Now().Add(-2 * time.Minute)}

	// Not full: the stale entry survives — put does not sweep.
	if _, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil); err != nil {
		t.Fatalf("New into a non-full store: %v", err)
	}
	if _, ok := st.sessions["stale"]; !ok {
		t.Fatal("non-full put swept expired sessions; expiry belongs to the janitor")
	}

	// Full: put refuses without touching the map, even though a slot is expired.
	for i := storeLen(st); i < maxSessions; i++ {
		id := fmt.Sprintf("session-%d", i)
		st.sessions[id] = &Session{ID: id, Created: time.Now()}
	}
	if _, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil); !errors.Is(err, ErrStoreFull) {
		t.Fatalf("New() error = %v, want %v (put must not sweep)", err, ErrStoreFull)
	}

	// The janitor's sweep reclaims the expired slot and put succeeds again.
	st.sweep()
	if _, err := st.New("verifier.example", "https://verifier.example/response", testQuery(), nil); err != nil {
		t.Fatalf("New after janitor sweep: %v", err)
	}
	if _, ok := st.sessions["stale"]; ok {
		t.Fatal("the sweep did not reclaim the expired slot")
	}
}

// TestStorePutRefusesAlreadyExpiredSession: the one expiry check put does is
// on the session being inserted — an already-expired session never enters the
// store, not even for one janitor tick. The fake clock is pushed a full
// lifetime AHEAD of the real wall clock, because Session.Expired (which put's
// check must agree with) compares Created against the real now.
func TestStorePutRefusesAlreadyExpiredSession(t *testing.T) {
	st := NewStore(time.Minute)
	t.Cleanup(st.Stop)
	base := time.Now().Add(10 * time.Minute)
	st.now = func() time.Time { return base }
	s := &Session{ID: "late", Created: base.Add(-2 * time.Minute), Nonce: "n", ClientID: "v", ResponseURI: "https://v/response/late"}
	if _, err := st.put(s); !errors.Is(err, ErrExpired) {
		t.Fatalf("put() error = %v, want %v", err, ErrExpired)
	}
	if n := storeLen(st); n != 0 {
		t.Fatalf("the expired session entered the store (%d entries)", n)
	}
}

// TestCompleteRefusesUnclaimedAndExpired: a verdict may only attach to a
// session the response handler claimed, and only while it is live.
func TestCompleteRefusesUnclaimedAndExpired(t *testing.T) {
	st := NewStore(time.Minute)
	q := testQuery()
	claimed, err := st.New("verifier.example", "https://verifier.example/response", q, nil)
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	if _, err := st.Claim(claimed.ID); err != nil {
		t.Fatalf("Claim: %v", err)
	}
	if _, err := st.Complete(claimed.ID, true, "ok"); err != nil {
		t.Fatalf("Complete on a claimed session: %v", err)
	}

	unclaimed, err := st.New("verifier.example", "https://verifier.example/response", q, nil)
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	if _, err := st.Complete(unclaimed.ID, true, "premature"); !errors.Is(err, ErrNoSession) {
		t.Fatalf("Complete before Claim = %v, want %v", err, ErrNoSession)
	}

	expired, err := st.New("verifier.example", "https://verifier.example/response", q, nil)
	if err != nil {
		t.Fatalf("New: %v", err)
	}
	if _, err := st.Claim(expired.ID); err != nil {
		t.Fatalf("Claim: %v", err)
	}
	st.mu.Lock()
	st.sessions[expired.ID].Created = time.Now().Add(-2 * st.ttl)
	st.mu.Unlock()
	if _, err := st.Complete(expired.ID, true, "late"); !errors.Is(err, ErrExpired) {
		t.Fatalf("Complete on an expired session = %v, want %v", err, ErrExpired)
	}
	if _, ok := st.sessions[expired.ID]; ok {
		t.Fatal("expired session left in the store after Complete refused it")
	}
}
