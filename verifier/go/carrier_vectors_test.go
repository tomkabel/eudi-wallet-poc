package main

import (
	"bytes"
	"crypto/ecdsa"
	"crypto/elliptic"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"os"
	"path/filepath"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/zk"
)

// W1 dual-carrier golden test: the SAME multipaz proof, wrapped once in the
// interim-JSON carrier and once in the de-facto CBOR DeviceResponse carrier,
// must verify identically through the carrier layer + the transcript registry
// + the Rust runtime. The vectors are tests/vectors/carrier-v1/ (the profile
// v1.0 references them); the transcript must be the B.2.6.1 OpenID4VPHandover
// derived through TranscriptForFlow — and the ISO dcapi transcript must still
// refuse the proof (the F2 check, again, through the new seam).
//
// The stale committed timestamp means the positive leg runs against the
// fixture transcript with the window check bypassed (the window belongs to
// the HTTP layer, TestStep87HandleResponse covers it); what is carrier-level
// here is the PARSE + NORMALIZE + TRANSCRIPT-BIND + VERIFY chain, which is
// what must not change across carriers.
func TestCarrierVectorsVerifyIdentically(t *testing.T) {
	if _, err := os.Stat("../zkverify-ffi/target/release/libzkverify.a"); err != nil {
		t.Skip("FFI staticlib not built")
	}
	vecDir := filepath.Join("..", "..", "tests", "vectors", "carrier-v1")
	interimB64, err := os.ReadFile(filepath.Join(vecDir, "device_response.json.b64"))
	if err != nil {
		t.Fatalf("read the interim vector: %v", err)
	}
	cborB64, err := os.ReadFile(filepath.Join(vecDir, "device_response.cbor.b64"))
	if err != nil {
		t.Fatalf("read the cbor vector: %v", err)
	}
	interim, err := base64.RawURLEncoding.DecodeString(string(bytes.TrimRight(interimB64, "\n")))
	if err != nil {
		t.Fatalf("the interim vector is not base64url: %v", err)
	}
	cborRaw, err := base64.RawURLEncoding.DecodeString(string(bytes.TrimRight(cborB64, "\n")))
	if err != nil {
		t.Fatalf("the cbor vector is not base64url: %v", err)
	}

	// The query both carriers answer (the fixture's own, from request.json).
	meta := loadStep87FixtureMeta(t)
	q, err := oid4vp.ZkAgeQuery("age_credential", meta.DocType, meta.Namespace, meta.AttrID,
		zkCircuitFor(t, 7, 1)).Single()
	if err != nil {
		t.Fatalf("query: %v", err)
	}

	// Parse both carriers THROUGH the carrier layer (explicit override, the
	// -carrier seam): the same query, one entry each.
	interimToken := oid4vp.VPToken{
		q.ID: []string{base64.RawURLEncoding.EncodeToString(interim)},
	}
	cborToken := oid4vp.VPToken{
		q.ID: []string{base64.RawURLEncoding.EncodeToString(cborRaw)},
	}
	cpInterim, carrierInterim, err := interimToken.ParseCarrier(q, oid4vp.InterimJSON{})
	if err != nil {
		t.Fatalf("interim carrier parse: %v", err)
	}
	cpCBOR, carrierCBOR, err := cborToken.ParseCarrier(q, oid4vp.MsoMdocZkCBOR{})
	if err != nil {
		t.Fatalf("cbor carrier parse: %v", err)
	}
	if carrierInterim.Name() != "interim-json" || carrierCBOR.Name() != "mso-mdoc-zk-cbor" {
		t.Fatalf("carrier names: %q / %q", carrierInterim.Name(), carrierCBOR.Name())
	}
	if !bytes.Equal(cpInterim.Proof, cpCBOR.Proof) {
		t.Fatal("the two carriers did not normalize to the same proof bytes")
	}
	// The subject of the profile's rule: reads must agree on the identity
	// fields and the binding, whatever the encoding.
	if cpInterim.DocType != cpCBOR.DocType || cpInterim.AttrID != cpCBOR.AttrID {
		t.Fatalf("identity fields differ across carriers: %+v vs %+v", cpInterim, cpCBOR)
	}

	// Trust: the fixture's issuer, enrolled as a relying party would.
	issuerJSON, err := os.ReadFile("zk/testdata/step8-7-openid4vp-zk/device_response_issuer.json")
	if err != nil {
		t.Fatalf("read device_response_issuer.json: %v", err)
	}
	trustPath := filepath.Join(t.TempDir(), "issuers.json")
	if err := os.WriteFile(trustPath, issuerJSON, 0o644); err != nil {
		t.Fatalf("write trust store: %v", err)
	}
	trust, err := oid4vp.LoadTrustStore(trustPath)
	if err != nil {
		t.Fatalf("load trust store: %v", err)
	}
	leaf, err := oid4vp.LeafCertificate([][]byte{cpCBOR.MSOX5Chain})
	if err != nil {
		t.Fatalf("leaf certificate: %v", err)
	}
	pub, ok := leaf.PublicKey.(*ecdsa.PublicKey)
	if !ok {
		t.Fatal("issuer leaf key is not ECDSA")
	}
	uncompressed := elliptic.Marshal(elliptic.P256(), pub.X, pub.Y)
	issuer, err := trust.Select(cpCBOR.DocType,
		"0x"+hex.EncodeToString(uncompressed[1:33]),
		"0x"+hex.EncodeToString(uncompressed[33:65]))
	if err != nil {
		t.Fatalf("issuer selection: %v", err)
	}

	// The transcript comes through the registry, for the flow the carrier
	// reported — the seam checkZk runs. The proof is bound to the B.2.6.1
	// OpenID4VPHandover over the fixture's handover parameters.
	transcript, err := oid4vp.TranscriptForFlow(cpCBOR.TranscriptFlow, oid4vp.TranscriptParams{
		ClientID:    meta.ClientID,
		Nonce:       meta.Nonce,
		ResponseURI: meta.ResponseURI,
	})
	if err != nil {
		t.Fatalf("TranscriptForFlow: %v", err)
	}

	registry, err := circuits.Load("../circuits.json")
	if err != nil {
		t.Fatalf("registry: %v", err)
	}
	var circuit circuits.Circuit
	for _, c := range registry.Accepted() {
		if c.SpecID() == cpCBOR.CircuitSpecID {
			circuit = c
			break
		}
	}
	if circuit.Hash == "" {
		t.Fatalf("presentation spec %q is not in the registry", cpCBOR.CircuitSpecID)
	}

	req := zk.Request{
		Version:       circuit.Version,
		NumAttributes: circuit.NumAttributes,
		PKx:           issuer.PKx,
		PKy:           issuer.PKy,
		DocType:       cpCBOR.DocType,
		Namespace:     cpCBOR.Namespace,
		AttrID:        cpCBOR.AttrID,
		AttrCBOR:      []byte{0xf5},
		Now:           cpCBOR.Timestamp,
		Transcript:    transcript,
		Proof:         cpCBOR.Proof,
	}
	// The committed proof binds the fixture's 2026-09-24 timestamp, which the
	// window check (HTTP-layer concern) would refuse today; the carrier-level
	// assertion is the crypto: verify the proof over the right transcript.
	if err := zk.Verify(req); err != nil {
		t.Fatalf("the CBOR-wrapped proof did not verify over the B.2.6.1 transcript: %v", err)
	}

	// The interim wrap must verify byte-identically: same request, same proof.
	reqInterim := req
	reqInterim.Proof = cpInterim.Proof
	if err := zk.Verify(reqInterim); err != nil {
		t.Fatalf("the interim-wrapped proof did not verify identically: %v", err)
	}

	// One flipped bit anywhere in the proof must fail (either carrier).
	flipped := make([]byte, len(cpCBOR.Proof))
	copy(flipped, cpCBOR.Proof)
	flipped[len(flipped)/2] ^= 0x01
	reqBad := req
	reqBad.Proof = flipped
	if err := zk.Verify(reqBad); err == nil {
		t.Fatal("a one-bit-flipped proof verified through the carrier layer")
	}

	// The F2 check through the new seam: the same proof over the OTHER flow's
	// transcript must fail.
	isoTranscript, err := oid4vp.TranscriptForFlow(oid4vp.FlowDcapiISO, oid4vp.TranscriptParams{
		EncryptionInfoB64: "not-the-real-encryption-info",
		Origin:            "https://verifier.example.com",
	})
	if err != nil {
		t.Fatalf("ISO transcript: %v", err)
	}
	reqWrong := req
	reqWrong.Transcript = isoTranscript
	if err := zk.Verify(reqWrong); err == nil {
		t.Fatal("the OpenID4VP-bound proof verified over a dcapi transcript")
	}

	// And the committed vector's transcript file must equal the registry's
	// derivation byte for byte — the vector cannot drift from the code.
	vecTranscript, err := os.ReadFile(filepath.Join(vecDir, "device_response_transcript.bin"))
	if err != nil {
		t.Fatalf("read the vector transcript: %v", err)
	}
	if !bytes.Equal(vecTranscript, transcript) {
		t.Fatal("the committed vector transcript differs from TranscriptForFlow's derivation")
	}
}

// loadStep87FixtureMeta reads the step 8.7 request.json (the handover
// parameters the committed proof binds).
func loadStep87FixtureMeta(t *testing.T) struct {
	ClientID, Nonce, ResponseURI string
	DocType, Namespace, AttrID   string
	Timestamp                    string
} {
	t.Helper()
	b, err := os.ReadFile("zk/testdata/step8-7-openid4vp-zk/request.json")
	if err != nil {
		t.Fatalf("read request.json: %v", err)
	}
	var m struct {
		ClientID    string `json:"client_id"`
		Nonce       string `json:"nonce"`
		ResponseURI string `json:"response_uri"`
		DocType     string `json:"doc_type"`
		Namespace   string `json:"namespace"`
		AttrID      string `json:"attr_id"`
		Timestamp   string `json:"timestamp"`
	}
	if err := json.Unmarshal(b, &m); err != nil {
		t.Fatalf("parse request.json: %v", err)
	}
	return struct {
		ClientID, Nonce, ResponseURI string
		DocType, Namespace, AttrID   string
		Timestamp                    string
	}{m.ClientID, m.Nonce, m.ResponseURI, m.DocType, m.Namespace, m.AttrID, m.Timestamp}
}
