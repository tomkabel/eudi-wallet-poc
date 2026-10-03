package oid4vp

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
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
	// Validity window (B3, signed trust store): RFC 3339, empty = unbounded.
	// A retired issuer keeps its line with not_after set — the append-only
	// rule forbids deleting it.
	NotBefore string `json:"not_before,omitempty"`
	NotAfter  string `json:"not_after,omitempty"`
}

// TrustStore maps a doctype to the issuers accepted for it.
type TrustStore struct {
	byDocType map[string][]Issuer
	// now, when set, enforces each issuer's not_before/not_after on every
	// lookup — so a long-running verifier stops trusting an issuer the moment
	// it expires, and a retired issuer stays listed but unusable.
	now func() time.Time
}

// maxIssuersPerDocType caps how many issuer keys one doctype may carry. Each
// key runs a ~2.5s ZK verification before Select rejects it, so a bloated
// doctype stretches every presentation toward the 60s WriteTimeout: 4 keys ≈
// 10s worst case, with headroom. A national trust list for one doctype will
// not plausibly exceed it.
const maxIssuersPerDocType = 4

// validP256Hex checks a 0x-prefixed coordinate: exactly 64 hex chars (32
// bytes, a padded P-256 coordinate), decoding cleanly. Select compares the
// hex strings it produced against presentation-derived hex, so a key that
// never round-trips would otherwise fail to match everywhere — silently
// trusting nothing instead of loudly trusting bad input.
func validP256Hex(s string) bool {
	h, ok := strings.CutPrefix(s, "0x")
	if !ok || len(h) != 64 {
		return false
	}
	_, err := hex.DecodeString(h)
	return err == nil
}

// LoadTrustStore reads trusted issuers from a JSON file (the unsigned
// development shape; production uses LoadSignedTrustStore).
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
	return buildTrustStore(doc.Issuers)
}

// buildTrustStore validates and indexes an issuer list (A8 rules).
func buildTrustStore(issuers []Issuer) (*TrustStore, error) {
	ts := &TrustStore{byDocType: map[string][]Issuer{}}
	for _, is := range issuers {
		if is.DocType == "" {
			return nil, fmt.Errorf("trust store: issuer %q has no doc_type", is.Name)
		}
		if !validP256Hex(is.PKx) || !validP256Hex(is.PKy) {
			return nil, fmt.Errorf("trust store: issuer %q (%s) needs 0x-prefixed 64-hex-char pkx/pky (a P-256 coordinate), got pkx %q pky %q",
				is.Name, is.DocType, is.PKx, is.PKy)
		}
		// Select compares hex strings, so both sides are held in one case.
		is.PKx, is.PKy = strings.ToLower(is.PKx), strings.ToLower(is.PKy)
		ts.byDocType[is.DocType] = append(ts.byDocType[is.DocType], is)
		if n := len(ts.byDocType[is.DocType]); n > maxIssuersPerDocType {
			return nil, fmt.Errorf("trust store: doctype %s lists %d issuers, more than the %d-key cap (each key runs a ~2.5s ZK verify before Select rejects it, and %d keys would stretch every presentation toward the 60s write timeout)",
				is.DocType, n, maxIssuersPerDocType, n)
		}
	}
	if len(ts.byDocType) == 0 {
		return nil, fmt.Errorf("trust store lists no issuers")
	}
	return ts, nil
}

// For returns the issuers accepted for a doctype.
func (ts *TrustStore) For(docType string) ([]Issuer, error) {
	is := ts.byDocType[docType]
	if ts.now != nil {
		now := ts.now()
		var live []Issuer
		for _, cand := range is {
			if cand.validAt(now) {
				live = append(live, cand)
			}
		}
		is = live
	}
	if len(is) == 0 {
		return nil, fmt.Errorf("no trusted issuer for doctype %q", docType)
	}
	return is, nil
}

// validAt reports whether now falls inside the issuer's window. The bounds
// were parsed once at load (checkWindow), so a parse failure here cannot
// happen; it is treated as outside the window regardless.
func (is Issuer) validAt(now time.Time) bool {
	if is.NotBefore != "" {
		nb, err := time.Parse(time.RFC3339, is.NotBefore)
		if err != nil || now.Before(nb) {
			return false
		}
	}
	if is.NotAfter != "" {
		na, err := time.Parse(time.RFC3339, is.NotAfter)
		if err != nil || now.After(na) {
			return false
		}
	}
	return true
}

func (is Issuer) checkWindow() error {
	if is.NotBefore != "" {
		if _, err := time.Parse(time.RFC3339, is.NotBefore); err != nil {
			return fmt.Errorf("issuer %q has unparsable not_before %q", is.Name, is.NotBefore)
		}
	}
	if is.NotAfter != "" {
		if _, err := time.Parse(time.RFC3339, is.NotAfter); err != nil {
			return fmt.Errorf("issuer %q has unparsable not_after %q", is.Name, is.NotAfter)
		}
	}
	return nil
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

// signedTrustDoc is the on-disk shape of a signed trust store: the issuers
// array exactly as LoadTrustStore reads it, plus the compact ES256 JWS that
// commits to it (B3). The signature is over the canonical JSON of the
// issuers array — the same bytes a parser would accept — so a re-ordered or
// re-serialized array cannot ride a foreign signature.
type signedTrustDoc struct {
	Issuers []Issuer `json:"issuers"`
	Sig     string   `json:"sig"`
}

// The per-entry validity window (not_before/not_after) rides on Issuer itself;
// zero values mean unbounded. Retired issuers keep their line with not_after
// set — the append-only rule forbids deleting it.

// LoadSignedTrustStore reads a signed trust store and verifies it:
//
//   - sig must verify under trustRoot (ES256, typ trust-store+json) over the
//     canonical issuers array;
//   - the issuers array must be append-only relative to prior: every issuer
//     in prior must appear in the new array (removed entry = load error —
//     the circuit-registry deprecation doctrine; retire an issuer by adding
//     not_after, never by deleting the line);
//   - per-entry not_before/not_after, when present, must parse; they are
//     enforced on every lookup against now (not once at load), unless
//     ignoreFreshness (the -ignore-trust-freshness dev escape) is set. An
//     entry outside its window is skipped, never a load error — retiring an
//     issuer with not_after must not stop the verifier from starting.
//
// The verified issuers are returned as a plain TrustStore; nothing downstream
// changes. Issuer keys still come only from this store; x5chain selects,
// never adds.
func LoadSignedTrustStore(path string, trustRoot *jose.JWK, prior *TrustStore, now func() time.Time, ignoreFreshness bool) (*TrustStore, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("read trust store: %w", err)
	}
	var doc signedTrustDoc
	if err := json.Unmarshal(b, &doc); err != nil {
		return nil, fmt.Errorf("parse trust store: %w", err)
	}
	if doc.Sig == "" {
		return nil, fmt.Errorf("trust store %s carries no signature; a bare issuers.json must be loaded with LoadTrustStore", path)
	}
	payload, err := jose.VerifyJWSWithKey(doc.Sig, *trustRoot, jose.JWSTypTrustStore)
	if err != nil {
		return nil, fmt.Errorf("trust store %s: signature does not verify under the trust root: %w", path, err)
	}
	var committed []Issuer
	if err := json.Unmarshal(payload, &committed); err != nil {
		return nil, fmt.Errorf("trust store %s: signed payload is not an issuers array: %w", path, err)
	}
	if len(committed) == 0 {
		return nil, fmt.Errorf("trust store %s commits to an empty issuer set", path)
	}
	// The signed payload is the authority; the unsigned array must match it
	// element for element, or the file was spliced after signing.
	if len(committed) != len(doc.Issuers) {
		return nil, fmt.Errorf("trust store %s: signed %d issuers, file carries %d", path, len(committed), len(doc.Issuers))
	}
	for i := range committed {
		if committed[i] != doc.Issuers[i] {
			return nil, fmt.Errorf("trust store %s: issuer %d differs from the signed payload", path, i)
		}
	}
	// Append-only: everything in prior must still be here. prior's keys are
	// lowercased by buildTrustStore, so compare lowercased.
	if prior != nil {
		kept := map[string]bool{}
		for _, is := range doc.Issuers {
			kept[is.DocType+"\x00"+strings.ToLower(is.PKx)+"\x00"+strings.ToLower(is.PKy)] = true
		}
		for _, was := range prior.All() {
			if !kept[was.DocType+"\x00"+was.PKx+"\x00"+was.PKy] {
				return nil, fmt.Errorf("trust store %s removed issuer %q (%s): retire it with not_after, never by deletion", path, was.Name, was.DocType)
			}
		}
	}
	for _, is := range doc.Issuers {
		if err := is.checkWindow(); err != nil {
			return nil, fmt.Errorf("trust store %s: %w", path, err)
		}
	}
	ts, err := buildTrustStore(doc.Issuers)
	if err != nil {
		return nil, err
	}
	if !ignoreFreshness {
		ts.now = now
	}
	return ts, nil
}
