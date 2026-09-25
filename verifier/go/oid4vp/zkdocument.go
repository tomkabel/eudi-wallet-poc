package oid4vp

import (
	"crypto/x509"
	"errors"
	"fmt"

	"github.com/tomkabel/ee-eudiw/verifier/go/internal/cborsub"
)

// ISO 18013-7 Annex C response-side containers this file owns: the dcapi
// envelope and the issuer-chain leaf dig. Everything here parses
// holder-controlled bytes with cborsub (ADR-002) and treats every deviation
// from the expected shape as a refusal, not a fallback. The DeviceRequest
// build, the ZkDocument parse and the timestamp window live in isodcapi.go.

// ParseEnvelope splits ["dcapi", {enc, cipherText}] into its two byte strings.
// Any other shape — wrong label, wrong arity, non-text keys, missing fields —
// is an error.
func ParseEnvelope(envelope []byte) (enc, cipherText []byte, err error) {
	root, err := cborsub.Decode(envelope, cborsub.DefaultLimits())
	if err != nil {
		return nil, nil, fmt.Errorf("dcapi envelope: %w", err)
	}
	if root.Kind != cborsub.KArray || len(root.Array) != 2 {
		return nil, nil, fmt.Errorf("dcapi envelope is not a 2-element array")
	}
	if root.Array[0].Kind != cborsub.KText || root.Array[0].Text != "dcapi" {
		return nil, nil, fmt.Errorf("dcapi envelope label is not the text \"dcapi\"")
	}
	m := &root.Array[1]
	if m.Kind != cborsub.KMap {
		return nil, nil, fmt.Errorf("dcapi envelope payload is not a map")
	}
	for _, k := range m.Map {
		if k.Key.Kind != cborsub.KText {
			return nil, nil, fmt.Errorf("dcapi envelope has a non-text key")
		}
		switch k.Key.Text {
		case "enc", "cipherText":
		default:
			return nil, nil, fmt.Errorf("dcapi envelope has unexpected key %q", k.Key.Text)
		}
	}
	encV, ok, err := m.MapGet("enc")
	if err != nil {
		return nil, nil, err
	}
	if !ok {
		return nil, nil, fmt.Errorf("dcapi envelope has no enc")
	}
	if encV.Kind != cborsub.KBytes {
		return nil, nil, fmt.Errorf("dcapi envelope enc is a %s, want bytes", encV.Kind)
	}
	enc = encV.Bytes
	ctV, ok, err := m.MapGet("cipherText")
	if err != nil {
		return nil, nil, err
	}
	if !ok {
		return nil, nil, fmt.Errorf("dcapi envelope has no cipherText")
	}
	if ctV.Kind != cborsub.KBytes {
		return nil, nil, fmt.Errorf("dcapi envelope cipherText is a %s, want bytes", ctV.Kind)
	}
	cipherText = ctV.Bytes
	return enc, cipherText, nil
}

// LeafCertificate digs the leaf X.509 certificate out of an msoX5chain value
// as parsed by ZkDocument: a byte string, or a byte string inside nested
// arrays (one level per issuer namespace, 18013-5 §9.1.2.2.2-3). The first
// certificate found is the leaf this verifier needs.
func LeafCertificate(chains [][]byte) (*x509.Certificate, error) {
	if len(chains) == 0 || len(chains[0]) == 0 {
		return nil, errors.New("msoX5chain is empty")
	}
	leaf, err := x509.ParseCertificate(chains[0])
	if err != nil {
		return nil, fmt.Errorf("issuer certificate does not parse: %v", err)
	}
	return leaf, nil
}
