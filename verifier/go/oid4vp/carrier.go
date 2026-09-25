package oid4vp

import (
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"errors"
	"fmt"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/cborsub"
)

// The carrier layer: one interface over every response encoding this verifier
// speaks for a ZK presentation. Today there are two in live use —
//
//   - InterimJSON, this repository's versioned stand-in envelope (the
//     ZKPresentation JSON), and
//   - MsoMdocZkCBOR, the de-facto carrier the shipping implementations emit
//     (a base64url CBOR DeviceResponse carrying zkDocuments),
//
// and ISO/IEC 18013-5 2e's ZkDocument carrier when that edition lands. The
// interface is what turns the next carrier into an implementation rather than
// a refactor (docs/profiles/MSO-MDOC-ZK-OPENID4VP-PROFILE.md v1.0 §1).
//
// Dispatch never guesses on holder bytes. The query's credential format names
// the carrier for mso_mdoc_zk sessions; a plain-format session sniffs the
// entry bytes ('{' → JSON envelope, a CBOR head → DeviceResponse), and a
// -carrier flag overrides both for tests. Sniffing only picks which strict
// parser runs — it never widens what is accepted, because whichever carrier
// parses still applies its own query/doctype/zk_system refusals.

// Carrier decodes one vp_token entry (or, for a future ISO dcapi carrier, an
// ISO dcapi payload) into a presentation the verifier can check.
// Implementations: InterimJSON (v1, current), MsoMdocZkCBOR (de-facto
// DeviceResponse.zkDocuments), ISOZkDocument (2e, on arrival).
type Carrier interface {
	// Name is the carrier's identifier, for logs and the audit trail.
	Name() string
	// Parse decodes raw holder-controlled bytes against a validated query.
	// Every implementation applies the same refusal set: the response answers
	// exactly this query (doctype, namespace, element, predicate value) and
	// names a zk system this verifier accepts.
	Parse(raw []byte, q CredentialQuery) (*CheckedPresentation, error)
}

// CheckedPresentation is what any carrier must yield: the identity fields, the
// proof, and the binding inputs, normalized so the verification path never
// learns which encoding it looked at. What is deliberately NOT here: the
// issuer public key (the trust store decides, never the presentation — the
// interim encoding's own invariant, kept for every carrier).
type CheckedPresentation struct {
	// ZKSystem is the presentation's own proving-system label. The allowlist
	// keys on the registry circuit, never here, so this is audited, not
	// trusted.
	ZKSystem string
	// CircuitSpecID is the circuit-set identifier the presentation names:
	// zkSystemId on the CBOR carrier, derived from the envelope's zk_system/
	// version/num_attributes on the interim carrier.
	CircuitSpecID string
	// Proof is the raw Longfellow proof bytes.
	Proof []byte
	// DocType, Namespace and AttrID name what was presented and disclosed.
	DocType           string
	Namespace, AttrID string
	// DisclosedTrue is the presentation's claim about the predicate's value,
	// already checked against the query's value constraint (EE-ZKP-021(b) on
	// both carriers).
	DisclosedTrue bool
	// TranscriptFlow is the flow the proof was made for. The verifier derives
	// exactly this flow's transcript from its own session data (never from
	// the response); the two flows are never interchangeable, and this field
	// is what keeps response handling on the one binding the proof carries
	// (transcript.go — the F2 lesson made structural).
	TranscriptFlow Flow
	// Timestamp is the wallet's own timestamp claim the proof takes as its
	// Now input — set on the CBOR carrier (ZkDocumentData.timestamp), empty on
	// the interim carrier, where the verifier fixes Now itself.
	Timestamp string
	// MSOX5Chain is the issuer certificate the presentation reveals (CBOR
	// carrier only); nil on the interim carrier. It may select among trusted
	// issuers, never supply one.
	MSOX5Chain []byte
	// Version and NumAttributes come from the interim envelope (the CBOR
	// carrier leaves them 0: its circuit identity is CircuitSpecID, resolved
	// against the registry).
	Version, NumAttributes uint32
}

// ErrNoZkDocument marks the legal empty answer on the CBOR carrier: the wallet
// presented nothing provable. Callers turn it into whatever "not a yes" means
// on their path (a 200 valid:false here), never into a parse failure.
var ErrNoZkDocument = errors.New("no zkDocument: the wallet presented nothing provable in this response")

// InterimJSON is the interim encoding this repository introduced so the flow
// could be built before any carrier existed: the ZKPresentation JSON
// envelope, base64url inside the vp_token. It is a versioned stand-in, not an
// interop claim, and it deliberately does not carry the issuer public key;
// the de-facto carrier is MsoMdocZkCBOR. It stays a Carrier so existing
// vectors and the plain path keep working unchanged.
type InterimJSON struct{}

// Name implements Carrier.
func (InterimJSON) Name() string { return "interim-json" }

// Parse is the former VPToken.Parse: pull the single presentation out of the
// JSON envelope, check it answers the question that was asked, and normalize
// it into a CheckedPresentation.
func (InterimJSON) Parse(raw []byte, q CredentialQuery) (*CheckedPresentation, error) {
	p, proof, err := parseInterimJSON(raw, q)
	if err != nil {
		return nil, err
	}
	// matchesQueryValue has already refused non-boolean and query-mismatched
	// values, so the attr CBOR here is one of the two single-byte encodings.
	attrCBOR, err := hex.DecodeString(p.AttrCBORHex)
	if err != nil {
		return nil, fmt.Errorf("attr_cbor_hex is not hex: %w", err)
	}
	return &CheckedPresentation{
		ZKSystem:       p.ZKSystem,
		CircuitSpecID:  interimCircuitSpecID(p.ZKSystem, p.Version, p.NumAttributes),
		Proof:          proof,
		DocType:        p.DocType,
		Namespace:      p.Namespace,
		AttrID:         p.AttrID,
		DisclosedTrue:  len(attrCBOR) == 1 && attrCBOR[0] == 0xf5,
		TranscriptFlow: FlowRedirectB261,
		Version:        p.Version,
		NumAttributes:  p.NumAttributes,
	}, nil
}

// interimCircuitSpecID derives the interim envelope's circuit identity in the
// label shape the de-facto carrier writes natively, without the fields the
// envelope never carried (block sizes, circuit hash). It is an audit label,
// never an allowlist key: the interim path allowlists on (version,
// num_attributes) via the registry, as it always has.
func interimCircuitSpecID(zkSystem string, version, numAttributes uint32) string {
	return fmt.Sprintf("%s_%d_%d", zkSystem, version, numAttributes)
}

// parseInterimJSON is the original VPToken.Parse body, kept verbatim so the
// interim encoding's semantics do not drift under the refactor.
func parseInterimJSON(raw []byte, q CredentialQuery) (*ZKPresentation, []byte, error) {
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
	if p.ZKSystem != SystemMultipaz {
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

// MsoMdocZkCBOR is the de-facto OpenID4VP ZK carrier: the vp_token's value is
// a base64url CBOR DeviceResponse whose top-level zkDocuments list carries
// multipaz 0.99.0's zkDocument serialization. ParseZkDocumentsBytes is the
// parser this wraps — the same one the ISO dcapi path reads, because the
// de-facto carrier and the ISO Annex C carrier share one ZkDocument encoding
// (docs/analysis/OPENID4VP-MSO-MDOC-ZK-CARRIER.md §2). The proof binds the
// B.2.6.1 OpenID4VPHandover transcript, not the ISO dcapi one.
type MsoMdocZkCBOR struct{}

// Name implements Carrier.
func (MsoMdocZkCBOR) Name() string { return "mso-mdoc-zk-cbor" }

// Parse decodes the DeviceResponse, picks the one zkDocument answering the
// query, and normalizes it. The refusal set is the interim carrier's: doctype,
// namespace, element, predicate value, zk system — checks that were previously
// split across MatchQueryZkDocument and the presenter, now applied where every
// carrier applies them.
func (MsoMdocZkCBOR) Parse(raw []byte, q CredentialQuery) (*CheckedPresentation, error) {
	docs, err := ParseZkDocumentsBytes(raw)
	if err != nil {
		return nil, err
	}
	d, err := MatchQueryZkDocument(docs, q)
	if err != nil {
		return nil, err
	}
	if d == nil {
		return nil, ErrNoZkDocument
	}
	sys, err := zkSystemForSpecID(d.ZkSystemSpecID)
	if err != nil {
		return nil, err
	}
	// EE-ZKP-021(b), CBOR form: the disclosed elementValue must be the
	// predicate value the query asked for, read from the response's own
	// issuerSigned — not assumed from the proof's soundness.
	if err := checkDisclosedValue(d.IssuerSigned, q); err != nil {
		return nil, err
	}
	return &CheckedPresentation{
		ZKSystem:       sys,
		CircuitSpecID:  d.ZkSystemSpecID,
		Proof:          d.Proof,
		DocType:        d.DocType,
		Namespace:      q.Namespace(),
		AttrID:         q.Element(),
		DisclosedTrue:  true, // checkDisclosedValue refused anything else
		TranscriptFlow: FlowRedirectB261,
		Timestamp:      d.Timestamp,
		MSOX5Chain:     d.MSOX5Chain,
	}, nil
}

// zkSystemForSpecID maps a zkSystemSpec id to its system label. The
// de-facto/ISO split is spelling: both labels name the same scheme and the
// allowlist never keys on the label (profile rule 4), so the label is read
// from the id's own `<system>_` prefix and audited, not trusted.
func zkSystemForSpecID(specID string) (string, error) {
	for _, sys := range []string{SystemMultipaz, SystemName} {
		prefix := sys + "_"
		if len(specID) > len(prefix) && specID[:len(prefix)] == prefix {
			return sys, nil
		}
	}
	return "", fmt.Errorf("zkSystemId %q does not name a supported zk system", specID)
}

// checkDisclosedValue walks the parsed issuerSigned namespaces and refuses
// unless the query's element is disclosed there with exactly the boolean value
// the query accepts. multipaz writes NameSpaces as
// {ns: [{elementIdentifier, elementValue}]}; only this one value is read, so
// anything else in the map is ignored.
func checkDisclosedValue(issuerSigned []byte, q CredentialQuery) error {
	v, err := cborsub.Decode(issuerSigned, cborsub.DefaultLimits())
	if err != nil {
		return fmt.Errorf("issuerSigned: %w", err)
	}
	nsV, ok, err := v.MapGet(q.Namespace())
	if err != nil {
		return err
	}
	if !ok {
		return fmt.Errorf("issuerSigned has no namespace %q", q.Namespace())
	}
	if nsV.Kind != cborsub.KArray {
		return fmt.Errorf("issuerSigned[%q] is a %s, want an array of elements", q.Namespace(), nsV.Kind)
	}
	for i := range nsV.Array {
		el := &nsV.Array[i]
		// The element may be an inline map or tag 24 over a bstr (the
		// ISO/IEC 18013-5 element serialization); unwrap the tag form.
		inner := el
		if el.Kind == cborsub.KTag24 {
			if inner, err = cborsub.Decode(el.Bytes, cborsub.DefaultLimits()); err != nil {
				return fmt.Errorf("issuerSigned[%q][%d]: %w", q.Namespace(), i, err)
			}
		}
		idV, _, err := inner.MapGet("elementIdentifier")
		if err != nil {
			return err
		}
		valV, _, err := inner.MapGet("elementValue")
		if err != nil {
			return err
		}
		if idV.Kind != cborsub.KText || idV.Text != q.Element() {
			continue
		}
		if valV.Kind != cborsub.KBool {
			return fmt.Errorf("elementValue for %q is a %s, want a boolean", q.Element(), valV.Kind)
		}
		if !valV.Bool {
			return fmt.Errorf("presentation proves %s = 0xf4, but the query only accepts 0xf5 (true)",
				q.Element())
		}
		return nil
	}
	return fmt.Errorf("issuerSigned[%q] does not disclose %q", q.Namespace(), q.Element())
}

// ParseCarrierName resolves a -carrier flag value to a Carrier. Empty means
// auto (format dispatch, sniffing plain-format entries). Unknown names are an
// error, never a fallback: a test that asks for one carrier and gets another
// must fail loudly.
func ParseCarrierName(name string) (Carrier, error) {
	switch name {
	case "":
		return nil, nil
	case InterimJSON{}.Name():
		return InterimJSON{}, nil
	case MsoMdocZkCBOR{}.Name():
		return MsoMdocZkCBOR{}, nil
	default:
		return nil, fmt.Errorf("unknown carrier %q (known: %s, %s)",
			name, InterimJSON{}.Name(), MsoMdocZkCBOR{}.Name())
	}
}

// CarrierForBytes picks the carrier for one vp_token entry: an explicit
// override wins (the -carrier flag, for tests), an mso_mdoc_zk query names
// its carrier, and anything else is sniffed off the first byte — '{' is the
// interim JSON envelope, a CBOR map or array head is the DeviceResponse. The
// de-facto DeviceResponse is a map at top level (verified against the multipaz
// fixture); the array head is accepted for the sniff so an unexpected-but-CBOR
// envelope gets the CBOR carrier's specific error, not a JSON one.
func CarrierForBytes(raw []byte, q CredentialQuery, override Carrier) (Carrier, error) {
	if override != nil {
		return override, nil
	}
	if q.Format == FormatMsoMdocZk {
		return MsoMdocZkCBOR{}, nil
	}
	if len(raw) > 0 {
		if raw[0] == '{' {
			return InterimJSON{}, nil
		}
		if raw[0]>>5 == 4 || raw[0]>>5 == 5 { // CBOR major 4 (array) or 5 (map)
			return MsoMdocZkCBOR{}, nil
		}
	}
	return InterimJSON{}, nil
}
