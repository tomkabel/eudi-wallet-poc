package oid4vp

import (
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"strings"
)

// ZKPresentation is what a wallet puts in the vp_token for a zero-knowledge
// presentation.
//
// INTERIM ENCODING. ISO/IEC 18013-5 second edition defines a ZkDocument inside
// the DeviceResponse (§10.2.7 of the DIS) for exactly this, but that edition is
// unpublished. Until it lands, this is an explicit, versioned stand-in so the
// flow can be built and tested; it is not an interoperability claim, and it
// deliberately does NOT carry the issuer public key — the verifier takes that
// from its own trust store.
type ZKPresentation struct {
	ZKSystem      string `json:"zk_system"`
	Version       uint32 `json:"version"`
	NumAttributes uint32 `json:"num_attributes"`
	DocType       string `json:"doc_type"`
	Namespace     string `json:"namespace"`
	AttrID        string `json:"attr_id"`
	AttrCBORHex   string `json:"attr_cbor_hex"`
	ProofB64      string `json:"proof_b64"`
}

// VPToken is the OpenID4VP 1.0 vp_token: DCQL credential id -> presentations.
type VPToken map[string][]string

// Parse pulls the single presentation matching the query's credential id out of
// a vp_token and checks it answers the question that was asked.
func (t VPToken) Parse(q CredentialQuery) (*ZKPresentation, []byte, error) {
	entries, ok := t[q.ID]
	if !ok {
		return nil, nil, fmt.Errorf("vp_token has no entry for credential id %q", q.ID)
	}
	if len(entries) != 1 {
		return nil, nil, fmt.Errorf("expected exactly one presentation for %q, got %d", q.ID, len(entries))
	}
	raw, err := base64.RawURLEncoding.DecodeString(entries[0])
	if err != nil {
		// tolerate padded base64url, which wallets do emit
		raw, err = base64.URLEncoding.DecodeString(entries[0])
		if err != nil {
			return nil, nil, fmt.Errorf("presentation is not base64url: %w", err)
		}
	}
	var p ZKPresentation
	if err := json.Unmarshal(raw, &p); err != nil {
		return nil, nil, fmt.Errorf("presentation is not a ZK presentation envelope: %w", err)
	}

	// The presentation must answer the query that was actually asked.
	if p.DocType != q.Meta.DoctypeValue {
		return nil, nil, fmt.Errorf("presentation doctype %q does not match the query's %q",
			p.DocType, q.Meta.DoctypeValue)
	}
	if p.Namespace != q.Namespace() {
		return nil, nil, fmt.Errorf("presentation namespace %q does not match the query's %q",
			p.Namespace, q.Namespace())
	}
	if p.AttrID != q.Element() {
		return nil, nil, fmt.Errorf("presentation discloses %q but the query asked for %q",
			p.AttrID, q.Element())
	}
	if p.ZKSystem != "longfellow-libzk-v1" {
		return nil, nil, fmt.Errorf("unsupported zk_system %q", p.ZKSystem)
	}

	proof, err := base64.RawStdEncoding.DecodeString(p.ProofB64)
	if err != nil {
		proof, err = base64.StdEncoding.DecodeString(p.ProofB64)
		if err != nil {
			return nil, nil, fmt.Errorf("proof is not base64: %w", err)
		}
	}
	attrCBOR, err := hex.DecodeString(p.AttrCBORHex)
	if err != nil {
		return nil, nil, fmt.Errorf("attr_cbor_hex is not hex: %w", err)
	}
	// EE-ZKP-021(b): the proof binds a value, and checking only the attribute's
	// identity accepts a proof that the predicate is FALSE as an answer to
	// "is this holder over 18?".
	if err := matchesQueryValue(p.AttrID, attrCBOR, q.Values()); err != nil {
		return nil, nil, err
	}
	return &p, proof, nil
}

// matchesQueryValue reports whether the presented CBOR is one of the values the
// query asked for. Only booleans reach here — Single rejects anything else — so
// the canonical encodings are the two single-byte simple values.
func matchesQueryValue(attrID string, attrCBOR []byte, want []any) error {
	acceptable := make([]string, 0, len(want))
	for _, v := range want {
		b, ok := v.(bool)
		if !ok {
			continue
		}
		enc := byte(0xf4)
		if b {
			enc = 0xf5
		}
		if len(attrCBOR) == 1 && attrCBOR[0] == enc {
			return nil
		}
		acceptable = append(acceptable, fmt.Sprintf("0x%02x (%t)", enc, b))
	}
	if len(acceptable) == 0 {
		return fmt.Errorf("the query names no acceptable value for %s", attrID)
	}
	return fmt.Errorf("presentation proves %s = 0x%x, but the query only accepts %s",
		attrID, attrCBOR, strings.Join(acceptable, " or "))
}

// ParseZkVPToken reads an mso_mdoc_zk vp_token entry (plan §8.7): the
// vp_token's value for the query's credential id is base64url CBOR — a
// DeviceResponse carrying top-level zkDocuments (multipaz's serialization, the
// same one step 2a's fixture pinned and ParseZkDocuments already reads).
//
// The device responses are holder-controlled bytes, so the strict-CBOR
// discipline applies here exactly as on the ISO path (ADR-002): every
// deviation from the expected shape is a refusal, not a fallback. No zkDocument
// at all is an answer ("presented nothing provable"), not a parse error — the
// caller decides what that is worth.
func (t VPToken) ParseZkVPToken(q CredentialQuery) ([]*ZkDocument, error) {
	entries, ok := t[q.ID]
	if !ok {
		return nil, fmt.Errorf("vp_token has no entry for credential id %q", q.ID)
	}
	if len(entries) != 1 {
		return nil, fmt.Errorf("expected exactly one presentation for %q, got %d", q.ID, len(entries))
	}
	raw, err := DecodeBase64URL(entries[0])
	if err != nil {
		return nil, fmt.Errorf("presentation is not base64url: %w", err)
	}
	docs, err := ParseZkDocumentsBytes(raw)
	if err != nil {
		return nil, err
	}
	return docs, nil
}

// MatchQueryZkDocument picks the one zkDocument answering the query and refuses
// anything else. The checks mirror the plain path's Parse (doctype, namespace,
// element) so both carriers answer the question that was actually asked.
func MatchQueryZkDocument(docs []*ZkDocument, q CredentialQuery) (*ZkDocument, error) {
	switch len(docs) {
	case 0:
		return nil, nil // an empty answer, not an error
	case 1:
	default:
		// Every accepted circuit proves the same 1-attribute predicate, so a
		// response claiming two zkDocuments is either confused or hostile;
		// both get the same refusal.
		return nil, fmt.Errorf("expected exactly one zkDocument, got %d", len(docs))
	}
	d := docs[0]
	if d.DocType != q.Meta.DoctypeValue {
		return nil, fmt.Errorf("zkDocument doctype %q does not match the query's %q",
			d.DocType, q.Meta.DoctypeValue)
	}
	if d.ZkSystemSpecID == "" {
		return nil, fmt.Errorf("zkDocument carries no zkSystemId")
	}
	return d, nil
}
