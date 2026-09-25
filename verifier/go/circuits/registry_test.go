package circuits

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func baseCircuit() Circuit {
	return Circuit{
		Hash: "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
		// Version 6 is the retired generation in every fixture below;
		// version 7 is live.
		Version: 7, NumAttributes: 1, BlockEncHash: 4151, BlockEncSig: 4096,
	}
}

// TestDeprecatedInvisibleToMapAndList: a DeprecatedOn entry loads fine but is
// invisible to both Lookup and Accepted — deprecation is the legal v6→v7
// transition, not a second accepted circuit.
func TestDeprecatedInvisibleToMapAndList(t *testing.T) {
	dep := baseCircuit()
	dep.Version = 6
	dep.DeprecatedOn = "2026-01-01"
	live := baseCircuit()
	reg, err := NewRegistry([]Circuit{dep, live})
	if err != nil {
		t.Fatalf("NewRegistry: %v", err)
	}
	if got, ok := reg.Lookup(6, 1); ok {
		t.Errorf("Lookup(6,1) returned %+v, want invisible deprecated entry", got)
	}
	if got, ok := reg.Lookup(7, 1); !ok || got.Hash != live.Hash {
		t.Errorf("Lookup(7,1) = (%+v, %v), want the live entry", got, ok)
	}
	if n := len(reg.Accepted()); n != 1 {
		t.Errorf("Accepted() lists %d circuits, want 1 (deprecated excluded)", n)
	}
}

// TestLookupMapAndListAgreement: everything Accepted() lists is findable via
// Lookup, and the count matches.
func TestLookupMapAndListAgreement(t *testing.T) {
	var circs []Circuit
	for i := 0; i < 4; i++ {
		c := baseCircuit()
		c.Hash = strings.Repeat(string(rune('a'+i)), 64)
		c.NumAttributes = uint32(i + 1)
		circs = append(circs, c)
	}
	reg, err := NewRegistry(circs)
	if err != nil {
		t.Fatalf("NewRegistry: %v", err)
	}
	acc := reg.Accepted()
	if len(acc) != 4 {
		t.Fatalf("Accepted() = %d entries, want 4", len(acc))
	}
	for _, c := range acc {
		got, ok := reg.Lookup(c.Version, c.NumAttributes)
		if !ok || got.Hash != c.Hash {
			t.Errorf("Lookup(%d,%d) = (%+v, %v), want hash %s", c.Version, c.NumAttributes, got, ok, c.Hash)
		}
	}
}

// TestDuplicateLiveTupleRejected pins the post-B-L1 semantics: two live
// entries claiming one (version, numAttributes) tuple make the offer
// ambiguous and must fail the load.
func TestDuplicateLiveTupleRejected(t *testing.T) {
	a := baseCircuit()
	b := baseCircuit()
	b.Hash = strings.Repeat("b", 64)
	_, err := NewRegistry([]Circuit{a, b})
	if err == nil {
		t.Fatal("NewRegistry accepted a duplicate live (version, numAttributes) tuple")
	}
	if !strings.Contains(err.Error(), "duplicate live circuit") {
		t.Errorf("error %q does not name the duplicate-tuple problem", err)
	}
}

// TestDuplicateHashRejected: the same hash twice used to silently overwrite;
// now it is an explicit load error, even across versions.
func TestDuplicateHashRejected(t *testing.T) {
	a := baseCircuit()
	b := baseCircuit()
	b.NumAttributes = 2 // different tuple, same hash
	_, err := NewRegistry([]Circuit{a, b})
	if err == nil {
		t.Fatal("NewRegistry accepted a duplicate circuit hash")
	}
	if !strings.Contains(err.Error(), "duplicate circuit hash") {
		t.Errorf("error %q does not name the duplicate-hash problem", err)
	}
}

// TestDeprecatedTwinsAllowed: deprecation exempts an entry from the tuple
// check (the v6→v7 transition), including against a live twin.
func TestDeprecatedTwinsAllowed(t *testing.T) {
	dep := baseCircuit()
	dep.Version = 6
	dep.DeprecatedOn = "2026-01-01"
	live := baseCircuit()
	reg, err := NewRegistry([]Circuit{dep, live})
	if err != nil {
		t.Fatalf("NewRegistry rejected a deprecated/live tuple overlap: %v", err)
	}
	if len(reg.Accepted()) != 1 {
		t.Errorf("Accepted() = %d entries, want the single live one", len(reg.Accepted()))
	}
}

// TestAcceptedReturnsCopy: mutating what Accepted() returns must not reach
// the registry's own list.
func TestAcceptedReturnsCopy(t *testing.T) {
	reg, err := NewRegistry([]Circuit{baseCircuit()})
	if err != nil {
		t.Fatalf("NewRegistry: %v", err)
	}
	got := reg.Accepted()
	got[0].Hash = "tampered"
	if again := reg.Accepted(); again[0].Hash == "tampered" {
		t.Fatal("Accepted() exposed the internal slice: mutation leaked through")
	}
}

func writeRegistry(t *testing.T, body string) string {
	t.Helper()
	path := filepath.Join(t.TempDir(), "circuits.json")
	if err := os.WriteFile(path, []byte(body), 0o644); err != nil {
		t.Fatalf("write registry: %v", err)
	}
	return path
}

// TestLoadErrorMatrix: missing file, unparseable JSON and an empty circuits
// array are all load errors naming the cause.
func TestLoadErrorMatrix(t *testing.T) {
	if _, err := Load(filepath.Join(t.TempDir(), "absent.json")); err == nil {
		t.Error("Load of a missing file returned no error")
	}
	if _, err := Load(writeRegistry(t, "{not json")); err == nil {
		t.Error("Load of unparseable JSON returned no error")
	}
	if _, err := Load(writeRegistry(t, `{"circuits":[]}`)); err == nil {
		t.Error("Load of an empty circuits array returned no error")
	}
	if _, err := Load(writeRegistry(t, `{"circuits":[{"circuit_hash":"`+strings.Repeat("a", 64)+`","version":7,"num_attributes":1}]}`)); err != nil {
		t.Errorf("Load of a valid registry failed: %v", err)
	}
}
