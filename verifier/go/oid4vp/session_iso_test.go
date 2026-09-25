package oid4vp

import (
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

// TestNewISOCleansUpOnFailure pins the store-hygiene contract of NewISO: any
// post-New failure must remove the half-built session — its HPKE private key
// included — instead of stranding it in the store until TTL.
func TestNewISOCleansUpOnFailure(t *testing.T) {
	st := NewStore(time.Minute)
	specs := mustSpecs(t)
	if len(specs) == 0 {
		t.Fatal("circuits.json lists no accepted circuits to offer")
	}

	// Duplicate offered spec id: the failure the fix targeted.
	dup := make([]circuits.Circuit, 0, len(specs)+1)
	dup = append(dup, specs...)
	dup = append(dup, specs[0])
	_, err := st.NewISO("client.example", "https://verifier.example/response", testQuery(), testOrigin, dup)
	if err == nil {
		t.Fatal("NewISO accepted a duplicate offered spec id")
	}
	if !strings.Contains(err.Error(), "duplicate offered spec id") {
		t.Errorf("error %q does not name the duplicate spec id", err)
	}
	if n := st.lenSessionsForTest(); n != 0 {
		t.Fatalf("store holds %d sessions after failed NewISO, want 0", n)
	}

	// Control: a clean offer still creates exactly one live session.
	s, err := st.NewISO("client.example", "https://verifier.example/response", testQuery(), testOrigin, specs)
	if err != nil {
		t.Fatalf("NewISO (clean): %v", err)
	}
	if after := st.lenSessionsForTest(); after != 1 {
		t.Fatalf("store holds %d sessions after clean NewISO, want 1", after)
	}
	if s.SessionISO() == nil {
		t.Fatal("clean NewISO returned a session without the dcapi extension")
	}
}

// TestNewISORefusesEmptyOrigin: the origin is hashed into the handover, so an
// empty one is refused before anything is stored.
func TestNewISORefusesEmptyOrigin(t *testing.T) {
	st := NewStore(time.Minute)
	specs := mustSpecs(t)
	if _, err := st.NewISO("client.example", "https://verifier.example/response", testQuery(), "", specs); err == nil {
		t.Fatal("NewISO accepted an empty origin")
	}
	if n := st.lenSessionsForTest(); n != 0 {
		t.Errorf("store holds %d sessions, want 0 (origin check precedes New)", n)
	}
}

// TestAttachISOMissingSession: attaching to an unknown id is ErrNoSession.
func TestAttachISOMissingSession(t *testing.T) {
	st := NewStore(time.Minute)
	err := st.AttachISO("no-such-session", &ISOExtension{}, testOrigin)
	if err != ErrNoSession {
		t.Fatalf("AttachISO on unknown id = %v, want %v", err, ErrNoSession)
	}
}

// lenSessionsForTest reports the store size; test-only, same package.
func (st *Store) lenSessionsForTest() int {
	st.mu.Lock()
	defer st.mu.Unlock()
	return len(st.sessions)
}
