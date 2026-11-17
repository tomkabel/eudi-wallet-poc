package oid4vp

import (
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
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
	// IACA is the base64 DER of the issuer's IACA root (PKI plan NEW-S2-1).
	// When set, the presentation's document signer certificate must chain to
	// it under the ISO 18013-5 DS profile, on top of the key pin above. Signed
	// (production) stores require it; the unsigned dev store may omit it and
	// then pins the key alone.
	IACA string `json:"iaca,omitempty"`
}

// IACACert parses the issuer's IACA root; nil when the store carries none.
func (is Issuer) IACACert() (*x509.Certificate, error) {
	if is.IACA == "" {
		return nil, nil
	}
	der, err := base64.StdEncoding.DecodeString(is.IACA)
	if err != nil {
		return nil, fmt.Errorf("issuer %q: iaca is not base64: %w", is.Name, err)
	}
	c, err := x509.ParseCertificate(der)
	if err != nil {
		return nil, fmt.Errorf("issuer %q: iaca does not parse: %w", is.Name, err)
	}
	if !c.BasicConstraintsValid || !c.IsCA {
		return nil, fmt.Errorf("issuer %q: iaca %q is not a CA certificate", is.Name, c.Subject)
	}
	return c, nil
}

// TrustStore maps a doctype to the issuers accepted for it.
type TrustStore struct {
	byDocType map[string][]Issuer
	// byKey indexes the same issuers by docType+"/"+KeyID, so Select is a map
	// lookup however many keys a doctype carries. A slice because an
	// append-only signed store may list one key twice (a retired line and its
	// renewal); the first live entry wins, as the old linear scan did.
	byKey map[string][]Issuer
	// now, when set, enforces each issuer's not_before/not_after on every
	// lookup — so a long-running verifier stops trusting an issuer the moment
	// it expires, and a retired issuer stays listed but unusable.
	now func() time.Time
}

// p256SPKIPrefix is the DER SubjectPublicKeyInfo header for an uncompressed
// P-256 key (id-ecPublicKey, prime256v1, BIT STRING of 66 bytes); the 65-byte
// 0x04||X||Y point follows it. Built by hand rather than through
// x509.MarshalPKIXPublicKey so a key id exists for any well-formed coordinate
// pair, on-curve or not — the trust store has never checked curve membership.
var p256SPKIPrefix = []byte{
	0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x02, 0x01,
	0x06, 0x08, 0x2a, 0x86, 0x48, 0xce, 0x3d, 0x03, 0x01, 0x07, 0x03, 0x42, 0x00,
}

// KeyIDOf returns the key id of a P-256 key given as 0x-prefixed 64-hex-char
// coordinates: lowercase hex SHA-256 of its DER SubjectPublicKeyInfo.
func KeyIDOf(pkx, pky string) (string, error) {
	if !validP256Hex(pkx) || !validP256Hex(pky) {
		return "", fmt.Errorf("key %s/%s is not a pair of 0x-prefixed 64-hex-char P-256 coordinates", pkx, pky)
	}
	x, _ := hex.DecodeString(pkx[2:])
	y, _ := hex.DecodeString(pky[2:])
	der := append(append(append(append([]byte{}, p256SPKIPrefix...), 0x04), x...), y...)
	sum := sha256.Sum256(der)
	return hex.EncodeToString(sum[:]), nil
}

// KeyID is the issuer key's id (KeyIDOf its coordinates). Issuers in a loaded
// store always have one; a malformed hand-built Issuer yields "".
func (is Issuer) KeyID() string {
	kid, _ := KeyIDOf(is.PKx, is.PKy)
	return kid
}

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
	ts := &TrustStore{byDocType: map[string][]Issuer{}, byKey: map[string][]Issuer{}}
	for _, is := range issuers {
		if is.DocType == "" {
			return nil, fmt.Errorf("trust store: issuer %q has no doc_type", is.Name)
		}
		if !validP256Hex(is.PKx) || !validP256Hex(is.PKy) {
			return nil, fmt.Errorf("trust store: issuer %q (%s) needs 0x-prefixed 64-hex-char pkx/pky (a P-256 coordinate), got pkx %q pky %q",
				is.Name, is.DocType, is.PKx, is.PKy)
		}
		if _, err := is.IACACert(); err != nil {
			return nil, fmt.Errorf("trust store: %w", err)
		}
		// Select compares hex strings, so both sides are held in one case.
		is.PKx, is.PKy = strings.ToLower(is.PKx), strings.ToLower(is.PKy)
		ts.byDocType[is.DocType] = append(ts.byDocType[is.DocType], is)
		k := is.DocType + "/" + is.KeyID()
		ts.byKey[k] = append(ts.byKey[k], is)
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
	kid, err := KeyIDOf(strings.ToLower(pkx), strings.ToLower(pky))
	if err == nil {
		if is, ok := ts.lookup(docType, kid); ok {
			return is, nil
		}
	}
	if _, err := ts.For(docType); err != nil {
		return Issuer{}, err
	}
	return Issuer{}, fmt.Errorf(
		"issuer key %s/%s is not in the trust store for doctype %q; msoX5chain may select among trusted keys, never supply them", pkx, pky, docType)
}

// SelectByKeyID chooses the trusted issuer for a doctype whose KeyID is kid
// (a holder's hint on the interim envelope). Like Select, the hint only picks
// among trusted keys: an unknown id is refused, never added.
func (ts *TrustStore) SelectByKeyID(docType, kid string) (Issuer, error) {
	if is, ok := ts.lookup(docType, strings.ToLower(kid)); ok {
		return is, nil
	}
	if _, err := ts.For(docType); err != nil {
		return Issuer{}, err
	}
	return Issuer{}, fmt.Errorf(
		"issuer key id %q is not in the trust store for doctype %q; the hint may select among trusted keys, never supply them", kid, docType)
}

// lookup returns the first live issuer indexed under (docType, kid).
func (ts *TrustStore) lookup(docType, kid string) (Issuer, bool) {
	for _, cand := range ts.byKey[docType+"/"+kid] {
		if ts.now == nil || cand.validAt(ts.now()) {
			return cand, true
		}
	}
	return Issuer{}, false
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
//     ignoreFreshness (the -ignore-trust-freshness escape, devflags builds only) is set. An
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
		// NEW-S2-1: a production store pins the IACA as well as the key, so a
		// document signer certificate is chain-validated, not only matched.
		if is.IACA == "" {
			return nil, fmt.Errorf("trust store %s: issuer %q (%s) has no iaca; a signed store must name each issuer's IACA root", path, is.Name, is.DocType)
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
