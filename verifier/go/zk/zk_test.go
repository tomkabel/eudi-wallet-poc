package zk

import (
	"errors"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

func TestCircuitHashKnown(t *testing.T) {
	const want = "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121"
	got, err := CircuitHash(7, 1)
	if err != nil {
		t.Fatalf("CircuitHash(7,1): %v", err)
	}
	if got != want {
		t.Fatalf("CircuitHash(7,1) = %s, want %s", got, want)
	}
}

func TestCircuitHashUnknown(t *testing.T) {
	if _, err := CircuitHash(99, 1); err == nil {
		t.Fatal("expected an error for a circuit that does not exist")
	}
}

func TestRegistryRejectsUnlisted(t *testing.T) {
	reg := circuits.NewRegistry([]circuits.Circuit{{Hash: "deadbeef", Version: 7, NumAttributes: 1}})
	if _, err := CheckCircuit(reg, 7, 1); err == nil {
		t.Fatal("expected the real circuit to be rejected by a registry that does not list it")
	}
}

// TestCheckCircuitLooksUpBeforeHashing pins the S11 order: a tuple the
// registry does not publish is refused without an FFI call. Hashing first
// would still refuse (8, 1), but only after ~54 s of circuit generation.
func TestCheckCircuitLooksUpBeforeHashing(t *testing.T) {
	const real71 = "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121"
	reg := circuits.NewRegistry([]circuits.Circuit{{Hash: real71, Version: 7, NumAttributes: 1}})
	start := time.Now()
	hash, err := CheckCircuit(reg, 8, 1)
	if !errors.Is(err, ErrCircuit) || !strings.Contains(err.Error(), "no accepted circuit") {
		t.Fatalf("CheckCircuit(8, 1) = %v, want the no-accepted-circuit refusal", err)
	}
	if hash != "" {
		t.Fatalf("an unmatched tuple returned hash %q; it must not be hashed", hash)
	}
	if d := time.Since(start); d > time.Second {
		t.Fatalf("refusing an unlisted tuple took %s; the lookup is not ahead of the hash", d)
	}
}
