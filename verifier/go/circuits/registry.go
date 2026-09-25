// Package circuits is the relying party's accepted-circuit registry.
//
// It is plain Go on purpose: package zk links the Rust staticlib through cgo,
// and oid4vp needs these types without linking it (ci.yml's fast job tests
// oid4vp with no Rust toolchain). Confirming an entry's hash is an FFI call, so
// that half lives in zk.CheckCircuit.
package circuits

import (
	"encoding/json"
	"fmt"
	"os"
	"sync"
)

// Registry is the relying party's set of accepted proving circuits.
//
// EE-EUDIW-TS-1.0 EE-ZKP-023 requires a relying party to check the circuit hash
// against a published accepted-circuit set BEFORE verifying a proof, and
// EE-ZKP-032 requires that set to be cached rather than fetched per
// presentation, because a per-presentation fetch is a phone-home that would
// defeat ZKP_07.
type Registry struct {
	mu sync.RWMutex
	// byTuple is keyed by the published (version, numAttributes) tuple — the
	// lookup callers make (plan defect S11). byHash is keyed by hash (the
	// lookup EE-ZKP-023 names) and exists to reject duplicate hashes at load;
	// acceptedList mirrors the live entries so Accepted() keeps file order.
	byTuple      map[uint64]Circuit
	byHash       map[string]Circuit
	acceptedList []Circuit
}

// Circuit is one entry of the registry, mirroring the fields EE-ZKP-030 requires
// a national registry to publish.
type Circuit struct {
	Hash          string `json:"circuit_hash"`
	Version       uint32 `json:"version"`
	NumAttributes uint32 `json:"num_attributes"`
	// BlockEncHash and BlockEncSig are the Longfellow block sizes the
	// multipaz label carries (e.g. longfellow-libzk-v1_7_1_4151_4096_8d07…);
	// they take part in the zkSystemSpec id and nothing else.
	BlockEncHash uint32   `json:"block_enc_hash,omitempty"`
	BlockEncSig  uint32   `json:"block_enc_sig,omitempty"`
	UpstreamTag  string   `json:"upstream_tag,omitempty"`
	AcceptedOn   string   `json:"accepted_on,omitempty"`
	DeprecatedOn string   `json:"deprecated_on,omitempty"`
	Audits       []string `json:"audits,omitempty"`
}

// SpecID is the zkSystemSpec id the verifier offers in a DeviceRequest and
// matches against a response's zkDocument.zkSystemSpecId. It is the
// multipaz-longfellow label (verifier/go/zk/testdata/step0-multipaz/
// multipaz-circuits.txt) built from the entry itself, so it is stable across
// the offer and the lookup and never a free-form holder string.
func (c Circuit) SpecID() string {
	return fmt.Sprintf("longfellow-libzk-v1_%d_%d_%d_%d_%s",
		c.Version, c.NumAttributes, c.BlockEncHash, c.BlockEncSig, c.Hash)
}

// tupleKey packs (version, numAttributes) into one map key.
func tupleKey(version, numAttributes uint32) uint64 {
	return uint64(version)<<32 | uint64(numAttributes)
}

// NewRegistry builds a registry from a slice of circuits.
//
// A duplicate live (version, numAttributes) tuple is a load error: two live
// entries for one tuple make the offer ambiguous — Lookup could not say which
// circuit a response proves. A duplicate hash is a load error too (it meant a
// silent overwrite); deprecated entries are exempt from the tuple check
// because deprecation is the legal v6→v7 transition, but two live twins of one
// hash still are not.
func NewRegistry(circuits []Circuit) (*Registry, error) {
	r := &Registry{
		byTuple: make(map[uint64]Circuit, len(circuits)),
		byHash:  make(map[string]Circuit, len(circuits)),
	}
	for _, c := range circuits {
		if c.DeprecatedOn == "" {
			key := tupleKey(c.Version, c.NumAttributes)
			if prev, dup := r.byTuple[key]; dup {
				return nil, fmt.Errorf("duplicate live circuit for (version %d, %d attributes): %s and %s both claim it",
					c.Version, c.NumAttributes, prev.Hash, c.Hash)
			}
			if prev, dup := r.byHash[c.Hash]; dup {
				return nil, fmt.Errorf("duplicate circuit hash %s: entries (%d, %d) and (%d, %d) both claim it",
					c.Hash, prev.Version, prev.NumAttributes, c.Version, c.NumAttributes)
			}
			r.byTuple[key] = c
			r.byHash[c.Hash] = c
			r.acceptedList = append(r.acceptedList, c)
		}
	}
	return r, nil
}

// Load reads a registry from a JSON file.
func Load(path string) (*Registry, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read circuit registry: %w", err)
	}
	var doc struct {
		Circuits []Circuit `json:"circuits"`
	}
	if err := json.Unmarshal(b, &doc); err != nil {
		return nil, fmt.Errorf("parse circuit registry: %w", err)
	}
	if len(doc.Circuits) == 0 {
		return nil, fmt.Errorf("circuit registry %s lists no circuits", path)
	}
	return NewRegistry(doc.Circuits)
}

// Lookup returns the accepted entry for (version, numAttributes). It matches
// the published tuple only and computes no hash, so a holder-chosen pair that
// is not accepted costs one map probe and nothing more (plan defect S11).
func (r *Registry) Lookup(version, numAttributes uint32) (Circuit, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	c, ok := r.byTuple[tupleKey(version, numAttributes)]
	return c, ok
}

// Accepted lists the circuits currently accepted.
func (r *Registry) Accepted() []Circuit {
	r.mu.RLock()
	defer r.mu.RUnlock()
	out := make([]Circuit, 0, len(r.acceptedList))
	out = append(out, r.acceptedList...)
	return out
}
