package oid4vp

import (
	"encoding/base64"
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

// Parse pulls the single presentation matching the query's credential id out
// of a vp_token and checks it answers the question that was asked. The entry's
// bytes name the carrier (the mso_mdoc_zk format's CBOR DeviceResponse, or the
// interim JSON envelope; sniffed here for a plain-format query), and the
// checks each carrier applies are carrier.go's, not this method's.
//
// ZKPresentation below stays the versioned-interim-encoding envelope this
// method originally parsed; it is an explicit interim encoding, not an
// interop claim. Callers wanting one presentation with one shape across
// carriers use the CheckedPresentation this returns.
func (t VPToken) Parse(q CredentialQuery) (*ZKPresentation, []byte, error) {
	entries, ok := t[q.ID]
	if !ok {
		return nil, nil, fmt.Errorf("vp_token has no entry for credential id %q", q.ID)
	}
	if len(entries) != 1 {
		return nil, nil, fmt.Errorf("expected exactly one presentation for %q, got %d", q.ID, len(entries))
	}
	raw, err := DecodeBase64URL(entries[0])
	if err != nil {
		return nil, nil, fmt.Errorf("presentation is not base64url: %w", err)
	}
	carrier, err := CarrierForBytes(raw, q, nil)
	if err != nil {
		return nil, nil, err
	}
	cp, err := carrier.Parse(raw, q)
	if err != nil {
		return nil, nil, err
	}
	if carrier != Carrier(InterimJSON{}) {
		// The CBOR carrier's presentation carries everything the interim
		// envelope's fields name except the fields the envelope format
		// predates; surface it as the interim struct so callers keep one
		// type, with the envelope-only fields (block sizes, version) coming
		// from the allowlisted circuit as before.
		return &ZKPresentation{
			ZKSystem:      cp.ZKSystem,
			Version:       0, // the CBOR carrier defers to the registry circuit
			NumAttributes: 0,
			DocType:       cp.DocType,
			Namespace:     cp.Namespace,
			AttrID:        cp.AttrID,
			AttrCBORHex:   "f5",
			ProofB64:      base64.RawStdEncoding.EncodeToString(cp.Proof),
		}, cp.Proof, nil
	}
	return interimFromChecked(cp)
}

// ParseCarrier is the carrier-agnostic form of Parse: the same vp_token entry
// selection, returning the normalized CheckedPresentation plus the carrier
// that parsed it. Dispatch order: an explicit override (the -carrier flag),
// the query's format, then the first-byte sniff (carrier.go).
func (t VPToken) ParseCarrier(q CredentialQuery, override Carrier) (*CheckedPresentation, Carrier, error) {
	entries, ok := t[q.ID]
	if !ok {
		return nil, nil, fmt.Errorf("vp_token has no entry for credential id %q", q.ID)
	}
	if len(entries) != 1 {
		return nil, nil, fmt.Errorf("expected exactly one presentation for %q, got %d", q.ID, len(entries))
	}
	raw, err := DecodeBase64URL(entries[0])
	if err != nil {
		return nil, nil, fmt.Errorf("presentation is not base64url: %w", err)
	}
	carrier, err := CarrierForBytes(raw, q, override)
	if err != nil {
		return nil, nil, err
	}
	cp, err := carrier.Parse(raw, q)
	if err != nil {
		return nil, nil, err
	}
	return cp, carrier, nil
}

// interimFromChecked rebuilds the interim envelope from a CheckedPresentation
// the interim carrier produced — a plain field copy, so the two entry points
// never disagree on what the envelope held.
func interimFromChecked(cp *CheckedPresentation) (*ZKPresentation, []byte, error) {
	return &ZKPresentation{
		ZKSystem:      cp.ZKSystem,
		Version:       cp.Version,
		NumAttributes: cp.NumAttributes,
		DocType:       cp.DocType,
		Namespace:     cp.Namespace,
		AttrID:        cp.AttrID,
		AttrCBORHex:   "f5",
		ProofB64:      base64.RawStdEncoding.EncodeToString(cp.Proof),
	}, cp.Proof, nil
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
