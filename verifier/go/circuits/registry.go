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
	// accepted is keyed by hash (the lookup EE-ZKP-023 names); acceptedList
	// mirrors the same entries so Lookup can match on (version, numAttributes)
	// without ever computing a hash for a circuit that is not accepted.
	accepted     map[string]Circuit
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

// NewRegistry builds a registry from a slice of circuits.
func NewRegistry(circuits []Circuit) *Registry {
	r := &Registry{accepted: make(map[string]Circuit, len(circuits))}
	for _, c := range circuits {
		if c.DeprecatedOn == "" {
			r.accepted[c.Hash] = c
			r.acceptedList = append(r.acceptedList, c)
		}
	}
	return r
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
	return NewRegistry(doc.Circuits), nil
}

// Lookup returns the accepted entry for (version, numAttributes). It matches
// the published tuple only and computes no hash, so a holder-chosen pair that
// is not accepted costs a slice scan and nothing more (plan defect S11).
func (r *Registry) Lookup(version, numAttributes uint32) (Circuit, bool) {
	r.mu.RLock()
	defer r.mu.RUnlock()
	var found Circuit
	ok := false
	for _, c := range r.acceptedList {
		if c.Version == version && c.NumAttributes == numAttributes {
			found, ok = c, true
		}
	}
	return found, ok
}

// Accepted lists the circuits currently accepted.
func (r *Registry) Accepted() []Circuit {
	r.mu.RLock()
	defer r.mu.RUnlock()
	out := make([]Circuit, 0, len(r.acceptedList))
	out = append(out, r.acceptedList...)
	return out
}
