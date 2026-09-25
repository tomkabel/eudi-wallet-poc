package oid4vp

import (
	"encoding/json"
	"fmt"
	"os"
	"strings"
)

// Issuer is one trusted attestation provider.
//
// The verifier gets issuer keys from here, never from the presentation. A
// wallet that could supply the issuer key it wants checked could mint its own
// attestation and prove anything about it — the proof would be perfectly valid
// and completely worthless. In the real ecosystem this file stands in for the
// Commission's List of Trusted Entities and the Art. 22 Trusted List
// (EE-GOV-010).
type Issuer struct {
	Name      string `json:"name"`
	DocType   string `json:"doc_type"`
	Namespace string `json:"namespace"`
	PKx       string `json:"pkx"`
	PKy       string `json:"pky"`
}

// TrustStore maps a doctype to the issuers accepted for it.
type TrustStore struct {
	byDocType map[string][]Issuer
}

// LoadTrustStore reads trusted issuers from a JSON file.
func LoadTrustStore(path string) (*TrustStore, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read trust store: %w", err)
	}
	var doc struct {
		Issuers []Issuer `json:"issuers"`
	}
	if err := json.Unmarshal(b, &doc); err != nil {
		return nil, fmt.Errorf("parse trust store: %w", err)
	}
	ts := &TrustStore{byDocType: map[string][]Issuer{}}
	for _, is := range doc.Issuers {
		if is.DocType == "" || !strings.HasPrefix(is.PKx, "0x") || !strings.HasPrefix(is.PKy, "0x") {
			return nil, fmt.Errorf("trust store: issuer %q needs doc_type and 0x-prefixed pkx/pky", is.Name)
		}
		// Select compares hex strings, so both sides are held in one case.
		is.PKx, is.PKy = strings.ToLower(is.PKx), strings.ToLower(is.PKy)
		ts.byDocType[is.DocType] = append(ts.byDocType[is.DocType], is)
	}
	if len(ts.byDocType) == 0 {
		return nil, fmt.Errorf("trust store %s lists no issuers", path)
	}
	return ts, nil
}

// For returns the issuers accepted for a doctype.
func (ts *TrustStore) For(docType string) ([]Issuer, error) {
	is, ok := ts.byDocType[docType]
	if !ok || len(is) == 0 {
		return nil, fmt.Errorf("no trusted issuer for doctype %q", docType)
	}
	return is, nil
}

// Select chooses the trusted issuer key matching an mso_mdoc presentation. The
// candidate keys come from this store and only this store; whatever x5chain the
// presentation carries may at most pick between them, never add to them. A
// presentation whose key matches no trusted issuer is refused — a wallet that
// could supply the issuer key it wants checked could mint its own attestation
// and prove anything about it (see Issuer).
func (ts *TrustStore) Select(docType, pkx, pky string) (Issuer, error) {
	is, err := ts.For(docType)
	if err != nil {
		return Issuer{}, err
	}
	for _, cand := range is {
		if cand.PKx == strings.ToLower(pkx) && cand.PKy == strings.ToLower(pky) {
			return cand, nil
		}
	}
	return Issuer{}, fmt.Errorf(
		"issuer key %s/%s is not in the trust store for doctype %q; msoX5chain may select among trusted keys, never supply them", pkx, pky, docType)
}

// All lists every trusted issuer.
func (ts *TrustStore) All() []Issuer {
	var out []Issuer
	for _, v := range ts.byDocType {
		out = append(out, v...)
	}
	return out
}
