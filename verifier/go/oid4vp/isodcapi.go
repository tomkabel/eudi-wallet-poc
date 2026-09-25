package oid4vp

import (
	"crypto/ecdh"
	"crypto/rand"
	"crypto/sha256"
	"errors"
	"fmt"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/cborsub"
)

// This file is the verifier half of an ISO/IEC 18013-7 Annex C presentation
// made through the W3C Digital Credentials API ("dcapi"), plan §5 of
// docs/planning/EUDI-WALLET-POC-CONFORMANCE-PLAN.md. The OpenID4VP redirect
// path (transcript.go) never decodes CBOR from the holder; this path does,
// through cborsub (ADR-002).

// Isolated is the base of every error this file and hpke.go return. A caller
// distinguishes "the presentation is wrong" from anything else with errors.Is;
// the message text is for whoever can act on it.
var Isolated = errors.New("isodcapi: rejected")

func fail(format string, args ...any) error {
	return fmt.Errorf("%w: %s", Isolated, fmt.Sprintf(format, args...))
}

// SessionTZ is the slack allowed around the session window when judging the
// holder-supplied ZkDocumentData.timestamp (plan §5.3).
const SessionTZ = 60 * time.Second

// EncryptionInfo is ISO 18013-7 Annex C's EncryptionInfo:
//
//	EncryptionInfo = ["dcapi", {nonce: bstr, recipientPublicKey: COSE_Key}]
//
// The verifier builds it once per session and never accepts it back from the
// wire: it is an input to the transcript the wallet's proof binds, so letting
// the holder supply it would let the holder pick the transcript.
type EncryptionInfo struct {
	// Nonce is the HPKE nonce, 32 bytes here (multipaz generates 32).
	Nonce []byte
	// RecipientPublicKey is the verifier's HPKE recipient public key as an
	// encoded COSE_Key map, embedded as is (no tag 24).
	RecipientPublicKey []byte
}

// BuildEncryptionInfo encodes EncryptionInfo.
func BuildEncryptionInfo(e EncryptionInfo) ([]byte, error) {
	if len(e.Nonce) == 0 {
		return nil, fail("encryptionInfo nonce is empty")
	}
	if len(e.RecipientPublicKey) == 0 {
		return nil, fail("encryptionInfo recipientPublicKey is empty")
	}
	return encryptionInfoCBOR(e), nil
}

// cborTag24 wraps b as tag 24 over a byte string.
func cborTag24(b []byte) []byte {
	out := cborHead(6, 24)
	return append(out, cborBytes(b)...)
}

// coseKeyP256 encodes an uncompressed P-256 point as a COSE_Key the way
// multipaz writes it for recipientPublicKey: a map carrying kty EC2 (1: 2),
// alg ES256 (3: -7), crv P-256 (-1: 1), and x/y as bstr. The key is only ever
// fed to HPKE, never to a signature check; multipaz reads it as a plain
// EcPublicKey.
func coseKeyP256(uncompressed []byte) ([]byte, error) {
	if len(uncompressed) != 65 || uncompressed[0] != 0x04 {
		return nil, fail("recipient public key is not an uncompressed P-256 point (65 bytes), got %d bytes", len(uncompressed))
	}
	// map(5); COSE labels -1, -2, -3 are major-1 values 0, 1, 2.
	m := []byte{0xa5}
	m = append(m, cborHead(0, 1)...)
	m = append(m, cborHead(0, 2)...) // 1: 2 (EC2)
	m = append(m, cborHead(0, 3)...)
	m = append(m, cborHead(1, 6)...) // 3: -7 (ES256)
	m = append(m, cborHead(1, 0)...)
	m = append(m, cborHead(0, 1)...) // -1: 1 (P-256)
	m = append(m, cborHead(1, 1)...) // -2: x
	m = append(m, cborBytes(uncompressed[1:33])...)
	m = append(m, cborHead(1, 2)...) // -3: y
	m = append(m, cborBytes(uncompressed[33:65])...)
	return m, nil
}

// NewHPKEKeyPair generates the session's HPKE P-256 key pair and the
// EncryptionInfo that names it, with a fresh 32-byte nonce. The private half
// stays in the Session; the public half goes out inside the COSE_Key.
func NewHPKEKeyPair() (*ecdh.PrivateKey, EncryptionInfo, string, error) {
	nonce := make([]byte, 32)
	if _, err := rand.Read(nonce); err != nil {
		return nil, EncryptionInfo{}, "", fmt.Errorf("isodcapi: nonce: %w", err)
	}
	priv, err := ecdh.P256().GenerateKey(rand.Reader)
	if err != nil {
		return nil, EncryptionInfo{}, "", fmt.Errorf("isodcapi: hpke keygen: %w", err)
	}
	cose, err := coseKeyP256(priv.PublicKey().Bytes())
	if err != nil {
		return nil, EncryptionInfo{}, "", err
	}
	info := EncryptionInfo{Nonce: nonce, RecipientPublicKey: cose}
	raw, err := BuildEncryptionInfo(info)
	if err != nil {
		return nil, EncryptionInfo{}, "", err
	}
	return priv, info, Base64URL(raw), nil
}

// DCAPIHandover builds the DCApiHandover CBOR the PoC's
// MDocUtils.generateDCApiHandover builds (MDocUtils.kt:96-107):
//
//	handoverInfo  = [tstr encryptionInfoB64, tstr origin]  ; the hashed bytes
//	DCApiHandover = ["dcapi", bstr sha-256(handoverInfo)]
//
// EncryptionInfo enters as its base64url TEXT — the page passes strings, and
// the fork's Kotlin hashes the same two text strings.
func DCAPIHandover(encryptionInfoB64, origin string) ([]byte, error) {
	if encryptionInfoB64 == "" || origin == "" {
		return nil, fail("encryptionInfo and origin are both required")
	}
	info := append([]byte{0x82}, cborText(encryptionInfoB64)...)
	info = append(info, cborText(origin)...)
	sum := sha256.Sum256(info)
	out := append([]byte{0x82}, cborText("dcapi")...)
	out = append(out, cborBytes(sum[:])...)
	return out, nil
}

// ISOTranscript is ISO 18013-7 Annex C's SessionTranscript:
//
//	SessionTranscript = [null, null, DCApiHandover]
//
// The verifier computes it from its own stored EncryptionInfo and configured
// origin, never from the response (plan §5.3). Named ISOTranscript because
// transcript.go's OpenID4VP redirect-path SessionTranscript keeps its name.
func ISOTranscript(encryptionInfoB64, origin string) ([]byte, error) {
	handover, err := DCAPIHandover(encryptionInfoB64, origin)
	if err != nil {
		return nil, err
	}
	st := []byte{0x83, 0xf6, 0xf6} // [null, null, ...]
	return append(st, handover...), nil
}

// DeviceRequest is ISO 18013-5's DeviceRequest, built for the age predicate:
//
//	DeviceRequest = {version: "1.0", docRequests: [{itemsRequest: tag24(ItemsRequest)}]}
//	ItemsRequest  = {docType, nameSpaces: {ns: {element: false}},
//	                 requestInfo: {zkRequest: {version: 1, systemSpecs: [...],
//	                                           zkRequired: true}}}
//
// version is a tstr: multipaz 0.99.0 DeviceRequest.fromDataItem and the PoC's
// DeviceRequestParser.kt:109 both read it with asTstr, which throws on a uint.
// The zkRequest mirrors multipaz's ZkRequest, whose fromDataItem requires
// zkRequired and, per spec, zkSystemId, system and params; each spec also
// carries id, the key the PoC reads (DeviceRequestParser.kt:164-178), and
// params (circuit_hash, version, num_attributes, block_enc_hash,
// block_enc_sig).
func DeviceRequest(docType, namespace, element string, specs []circuits.Circuit) ([]byte, error) {
	if docType == "" || namespace == "" || element == "" {
		return nil, fail("deviceRequest needs docType, namespace and element")
	}
	if len(specs) > 23 {
		return nil, fail("%d systemSpecs exceeds one head byte", len(specs))
	}
	// nameSpaces: {namespace: {element: false}} — the "is this attribute false?"
	// phrasing the ZK predicate proves over.
	claims := append([]byte{0xa1}, cborText(element)...)
	claims = append(claims, 0xf4) // false
	ns := append([]byte{0xa1}, cborText(namespace)...)
	ns = append(ns, claims...)

	// requestInfo.zkRequest
	sysSpecs := []byte{0x80 | byte(len(specs))}
	for _, c := range specs {
		spec, err := zkSystemSpec(c)
		if err != nil {
			return nil, err
		}
		sysSpecs = append(sysSpecs, spec...)
	}
	zkReq := []byte{0xa3}
	zkReq = append(zkReq, cborText("version")...)
	zkReq = append(zkReq, 0x01)
	zkReq = append(zkReq, cborText("systemSpecs")...)
	zkReq = append(zkReq, sysSpecs...)
	zkReq = append(zkReq, cborText("zkRequired")...)
	zkReq = append(zkReq, 0xf5) // true: design 2a is ZK only
	reqInfo := []byte{0xa1}
	reqInfo = append(reqInfo, cborText("zkRequest")...)
	reqInfo = append(reqInfo, zkReq...)

	items := []byte{0xa3}
	items = append(items, cborText("docType")...)
	items = append(items, cborText(docType)...)
	items = append(items, cborText("nameSpaces")...)
	items = append(items, ns...)
	items = append(items, cborText("requestInfo")...)
	items = append(items, reqInfo...)

	docReq := []byte{0xa1}
	docReq = append(docReq, cborText("itemsRequest")...)
	docReq = append(docReq, cborTag24(items)...)
	req := []byte{0xa2}
	req = append(req, cborText("version")...)
	req = append(req, cborText("1.0")...)
	req = append(req, cborText("docRequests")...)
	req = append(req, 0x81) // array(1)
	req = append(req, docReq...)
	return req, nil
}

// zkSystemSpec encodes one ZkSystemSpec from a registry entry. The params come
// from the registry file, which the verifier hashed at startup — the holder
// never supplies them.
func zkSystemSpec(c circuits.Circuit) ([]byte, error) {
	// params: circuit_hash, version, num_attributes, block_enc_hash,
	// block_enc_sig — the labels the PoC's matchZkSystemSpec reads
	// (DigitalCredentialsViewModel.kt:384-392). All five come from the registry
	// entry.
	params := []byte{0xa5}
	params = append(params, cborText("circuit_hash")...)
	params = append(params, cborText(c.Hash)...)
	params = append(params, cborText("version")...)
	params = append(params, cborHead(0, uint64(c.Version))...)
	params = append(params, cborText("num_attributes")...)
	params = append(params, cborHead(0, uint64(c.NumAttributes))...)
	params = append(params, cborText("block_enc_hash")...)
	params = append(params, cborHead(0, uint64(c.BlockEncHash))...)
	params = append(params, cborText("block_enc_sig")...)
	params = append(params, cborHead(0, uint64(c.BlockEncSig))...)

	spec := []byte{0xa4}
	spec = append(spec, cborText("zkSystemId")...) // multipaz ZkRequest.fromDataItem
	spec = append(spec, cborText(c.SpecID())...)
	spec = append(spec, cborText("id")...) // the PoC's DeviceRequestParser
	spec = append(spec, cborText(c.SpecID())...)
	spec = append(spec, cborText("system")...)
	spec = append(spec, cborText(SystemMultipaz)...)
	spec = append(spec, cborText("params")...)
	spec = append(spec, params...)
	return spec, nil
}

// ZkDocument is one entry of DeviceResponse.zkDocuments in multipaz 0.99.0's
// serialization (plan §5.2; verified against ZkDocument.toDataItem /
// ZkDocumentData.toDataItem bytecode and the step2a-iso-annex-c fixture):
//
//	zkDocument = { proof: bstr, documentData: #24(<inline map>) }
//	inline map  = { zkSystemId: tstr, docType: tstr, timestamp: #0(tstr),
//	                issuerSigned: {ns: [{elementIdentifier, elementValue}]},
//	                deviceSigned: {ns: [...]}, msoX5chain: bstr|[*bstr] }
//
// The verifier uses the identity fields and the proof; IssuerSigned and
// DeviceSigned are kept as the raw CBOR of their maps (the elements are maps,
// not byte strings, and no rule here reads inside them).
type ZkDocument struct {
	ZkSystemSpecID string
	DocType        string
	Timestamp      string
	IssuerSigned   []byte
	DeviceSigned   []byte
	MSOX5Chain     []byte
	Proof          []byte
}

// ParseZkDocument reads one zkDocument out of a decoded DeviceResponse. It
// reads only; every security rule lives in the server's ISO check.
func ParseZkDocument(v *cborsub.Value) (*ZkDocument, error) {
	if v.Kind != cborsub.KMap {
		return nil, fail("zkDocument is a %s, want a map", v.Kind)
	}
	// multipaz's ZkDocument.toDataItem emits exactly two keys: proof and
	// documentData (the latter tag 24 over the encoded ZkDocumentData map).
	// Anything else is not a multipaz zkDocument.
	proof, ok, err := v.MapGet("proof")
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, fail("zkDocument has no %q", "proof")
	}
	if proof.Kind != cborsub.KBytes {
		return nil, fail("zkDocument.proof is a %s, want bytes", proof.Kind)
	}
	ddTagged, ok, err := v.MapGet("documentData")
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, fail("zkDocument has no %q", "documentData")
	}
	if ddTagged.Kind != cborsub.KTag24 {
		return nil, fail("zkDocument.documentData is a %s, want tag 24 over the encoded ZkDocumentData", ddTagged.Kind)
	}
	dd, err := cborsub.Decode(ddTagged.Bytes, cborsub.DefaultLimits())
	if err != nil {
		return nil, fmt.Errorf("documentData content: %w", err)
	}
	if dd.Kind != cborsub.KMap {
		return nil, fail("documentData is a %s, want a map", dd.Kind)
	}
	d := &ZkDocument{Proof: proof.Bytes}
	// The spec id lives inside ZkDocumentData under the key zkSystemId
	// (ZkDocumentData.toDataItem, first put) — not on the zkDocument itself.
	specV, ok, err := dd.MapGet("zkSystemId")
	if err != nil {
		return nil, err
	}
	if !ok {
		return nil, fail("documentData has no zkSystemId")
	}
	if specV.Kind != cborsub.KText {
		return nil, fail("documentData.zkSystemId is a %s, want text", specV.Kind)
	}
	d.ZkSystemSpecID = specV.Text
	for _, kv := range []struct {
		key string
		dst *string
	}{
		{"docType", &d.DocType},
		{"timestamp", &d.Timestamp},
	} {
		val, ok, err := dd.MapGet(kv.key)
		if err != nil {
			return nil, err
		}
		if !ok {
			return nil, fail("documentData has no %q", kv.key)
		}
		// timestamp serializes as tag 0 over a tstr; docType as a plain tstr.
		// Both carry the same text shape, and the window check below parses it.
		if kv.key == "timestamp" && val.Kind != cborsub.KTag0 {
			return nil, fail("documentData.timestamp is a %s, want tag 0 (standard date/time)", val.Kind)
		}
		if kv.key != "timestamp" && val.Kind != cborsub.KText {
			return nil, fail("documentData.%s is a %s, want text", kv.key, val.Kind)
		}
		*kv.dst = val.Text
	}
	for _, kv := range []struct {
		key string
		dst *[]byte
	}{
		{"issuerSigned", &d.IssuerSigned},
		{"deviceSigned", &d.DeviceSigned},
	} {
		val, ok, err := dd.MapGet(kv.key)
		if err != nil {
			return nil, err
		}
		if !ok {
			return nil, fail("documentData has no %q", kv.key)
		}
		// Maps of namespace -> array of {elementIdentifier, elementValue},
		// kept as their raw CBOR; a byte string is not accepted in their
		// place.
		if val.Kind != cborsub.KMap {
			return nil, fail("documentData.%s is a %s, want a map of namespaces", kv.key, val.Kind)
		}
		*kv.dst = val.Raw
	}
	if mc, ok, err := dd.MapGet("msoX5chain"); err != nil {
		return nil, err
	} else if ok {
		// X509CertChain.toDataItem emits a bare bstr for a single certificate
		// and an array of bstrs only for a longer chain. The verifier consumes
		// the leaf, so unwrap the one-element form.
		switch mc.Kind {
		case cborsub.KBytes:
			d.MSOX5Chain = mc.Bytes
		case cborsub.KArray:
			if len(mc.Array) == 0 || mc.Array[0].Kind != cborsub.KBytes {
				return nil, fail("documentData.msoX5chain is an array without a leading certificate")
			}
			d.MSOX5Chain = mc.Array[0].Bytes
		default:
			return nil, fail("documentData.msoX5chain is a %s, want bytes or an array of bytes", mc.Kind)
		}
	}
	return d, nil
}

// ParseZkDocuments walks the DeviceResponse's Documents array and returns every
// element that carries a zkDocument. A plain document element is skipped: the
// PoC's response builder carries unprovable documents as plain documents
// (DigitalCredentialsViewModel.kt:349-356). The ISO handler treats a response
// with no zkDocument at all as invalid — the whole point of the path is the
// proof.
func ParseZkDocumentsBytes(deviceResponse []byte) ([]*ZkDocument, error) {
	root, err := cborsub.Decode(deviceResponse, cborsub.DefaultLimits())
	if err != nil {
		return nil, err
	}
	return ParseZkDocuments(root)
}

// ParseZkDocuments walks a DECODED DeviceResponse (cborsub root) and returns
// every zkDocument it carries. multipaz 0.99.0 puts them under the top-level
// zkDocuments key (DeviceResponse.toDataItem, and the fixture this repository
// ships in zk/testdata/step2a-iso-annex-c); a documents entry carrying a
// zkDocument member is accepted as well, because the fork's redirect path can
// still send the older wrapping. A plain document element is skipped: the PoC
// carries unprovable documents as plain documents
// (DigitalCredentialsViewModel.kt:349-356). The ISO handler treats a response
// with no zkDocument at all as invalid — the whole point of the path is the
// proof.
func ParseZkDocuments(root *cborsub.Value) ([]*ZkDocument, error) {
	st, ok, err := root.MapGet("status")
	if err != nil {
		return nil, err
	}
	if !ok || st.Kind != cborsub.KUint || st.Uint != 0 {
		return nil, fail("DeviceResponse.status is not 0")
	}
	out := &[]*ZkDocument{}
	// zkDocuments first: the multipaz key. Both arms must run, because the
	// fork can mix a plain documents entry with a zkDocuments entry in one
	// response (addDocument + addZkDocument).
	zkArr, zkok, err := root.MapGet("zkDocuments")
	if err != nil {
		return nil, err
	}
	if zkok {
		if zkArr.Kind != cborsub.KArray {
			return nil, fail("DeviceResponse.zkDocuments is a %s, want an array", zkArr.Kind)
		}
		for i := range zkArr.Array {
			el := &zkArr.Array[i]
			d, err := ParseZkDocument(el)
			if err != nil {
				return nil, fmt.Errorf("zkDocuments[%d]: %w", i, err)
			}
			*out = append(*out, d)
		}
	}
	docs, ok, err := root.MapGet("documents")
	if err != nil {
		return nil, err
	}
	if !ok {
		// multipaz omits an empty documents array, so a zkDocuments-only
		// response has no documents key at all.
		if !zkok {
			return nil, fail("DeviceResponse has neither documents nor zkDocuments")
		}
		return *out, nil
	}
	if docs.Kind != cborsub.KArray {
		return nil, fail("DeviceResponse.documents is a %s, want an array", docs.Kind)
	}
	for i := range docs.Array {
		el := &docs.Array[i]
		if el.Kind != cborsub.KMap {
			return nil, fail("Documents[%d] is a %s, want a map", i, el.Kind)
		}
		zkd, ok, err := el.MapGet("zkDocument")
		if err != nil {
			return nil, err
		}
		if !ok {
			continue // a plain document: not this path's concern
		}
		d, err := ParseZkDocument(zkd)
		if err != nil {
			return nil, fmt.Errorf("Documents[%d]: %w", i, err)
		}
		*out = append(*out, d)
	}
	return *out, nil
}

// CheckTimestampWindow judges the holder-supplied ZkDocumentData.timestamp.
// The redirect path lets the verifier pick `now` itself; here the proof is
// generated for the wallet's own timestamp, so the verifier must verify against
// that value — and bound it, or a wallet holding an expired attestation could
// prove validity at a moment of its choosing (plan §5.3, EE-ZKP-021(d)).
// The upper anchor is the verifier's own clock at receipt, so the holder still
// picks the moment within [session created, receipt] ± SessionTZ; the session
// TTL is what bounds that choice.
//
// Only the UTC "Z" form is accepted: it is what multipaz emits and what the
// Longfellow circuit's fixed 20-byte timestamp slot holds. A numeric offset
// is valid RFC 3339 but 25 bytes long, so it could never verify anyway.
func CheckTimestampWindow(sessionCreated, now time.Time, timestamp string) error {
	if timestamp == "" {
		return fail("documentData.timestamp is empty")
	}
	ts, err := time.Parse(time.RFC3339, timestamp)
	if err != nil || len(timestamp) != 20 || timestamp[19] != 'Z' {
		return fail("documentData.timestamp %q is not RFC 3339 UTC (yyyy-MM-ddTHH:mm:ssZ)", timestamp)
	}
	if !ts.Equal(ts.Truncate(time.Second)) {
		return fail("documentData.timestamp %q has fractional seconds; ISO/IEC 18013-5 clauses 7.1 and 9.1.2.4 forbid them", timestamp)
	}
	low := sessionCreated.Add(-SessionTZ)
	high := now.Add(SessionTZ)
	if ts.Before(low) {
		return fail("documentData.timestamp %s is before the session window (earliest %s)",
			timestamp, low.UTC().Format(time.RFC3339))
	}
	if ts.After(high) {
		return fail("documentData.timestamp %s is after the session window (latest %s)",
			timestamp, high.UTC().Format(time.RFC3339))
	}
	return nil
}
