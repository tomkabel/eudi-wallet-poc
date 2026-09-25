package oid4vp

// Tests for the ISO 18013-7 Annex C path (plan §5, step 2a, item 7). The
// DeviceRequest vector under zk/testdata/step2a-iso-annex-c/ (provenance in
// GENERATED-BY) is compared, not rewritten, so the fork's wallet work and
// future steps can replay it; -update regenerates it deliberately.
//
// Nothing here calls the FFI (package oid4vp does not link it); the dcapi
// flow tests that do are in package main's iso_test.go.

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"flag"
	"os"
	"strings"
	"testing"

	"github.com/tomkabel/ee-eudiw/verifier/go/circuits"
	"github.com/tomkabel/ee-eudiw/verifier/go/internal/cborsub"
)

const (
	testOrigin       = "https://verifier.example.ee"
	testEncryptionID = "longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121"
)

func mustSpecs(t *testing.T) []circuits.Circuit {
	t.Helper()
	reg, err := circuits.Load("../../circuits.json")
	if err != nil {
		t.Fatalf("load circuits.json: %v", err)
	}
	return reg.Accepted()
}

// TestDCAPIHandoverVector pins the handover bytes against the fork's
// generateDCApiHandover: [tstr, tstr] hashed, wrapped as ["dcapi", bstr].
func TestDCAPIHandoverVector(t *testing.T) {
	infoB64 := "aSQ-BJY" // any opaque string; the page passes text
	handover, err := DCAPIHandover(infoB64, testOrigin)
	if err != nil {
		t.Fatalf("DCAPIHandover: %v", err)
	}
	info := append([]byte{0x82}, cborText(infoB64)...)
	info = append(info, cborText(testOrigin)...)
	sum := sha256.Sum256(info)
	want := append([]byte{0x82}, cborText("dcapi")...)
	want = append(want, cborBytes(sum[:])...)
	if string(handover) != string(want) {
		t.Fatalf("handover mismatch:\n got %x\nwant %x", handover, want)
	}
}

// TestISOTranscriptVector pins [null, null, DCApiHandover] — the third element
// matching MDocUtils.generateDCApiHandover's output shape.
func TestISOTranscriptVector(t *testing.T) {
	st, err := ISOTranscript("abc", testOrigin)
	if err != nil {
		t.Fatalf("ISOTranscript: %v", err)
	}
	if st[0] != 0x83 || st[1] != 0xf6 || st[2] != 0xf6 {
		t.Fatalf("transcript does not start [null, null, ...]: %x", st[:3])
	}
	if len(st) < 4 || st[3] != 0x82 {
		t.Fatalf("third element is not the handover array: %x", st)
	}
}

// TestHPKERoundTrip seals to a session key pair and opens through the same
// path the server uses, with the transcript as info and an empty aad.
func TestHPKERoundTrip(t *testing.T) {
	priv, info, infoB64, err := NewHPKEKeyPair()
	if err != nil {
		t.Fatalf("keygen: %v", err)
	}
	if len(info.Nonce) != 32 {
		t.Fatalf("nonce is %d bytes, want 32", len(info.Nonce))
	}
	if !strings.Contains(infoB64, "-") && strings.ContainsAny(infoB64, "+/") {
		t.Fatalf("encryptionInfo is not base64url: %q", infoB64)
	}
	transcript, err := ISOTranscript(infoB64, testOrigin)
	if err != nil {
		t.Fatalf("transcript: %v", err)
	}
	plaintext := []byte("device response bytes")
	ct, err := SealTo(priv.PublicKey(), transcript, plaintext)
	if err != nil {
		t.Fatalf("seal: %v", err)
	}
	got, err := Open(priv, transcript, ct)
	if err != nil {
		t.Fatalf("open: %v", err)
	}
	if string(got) != string(plaintext) {
		t.Fatalf("round trip mismatch: %q", got)
	}

	// One flipped bit in the ciphertext must fail the open, not decode.
	ct[len(ct)-1] ^= 0x01
	if _, err := Open(priv, transcript, ct); err == nil {
		t.Fatal("a flipped ciphertext bit opened cleanly")
	}
}

// TestEncryptionInfoShape decodes the emitted EncryptionInfo and checks the
// Annex C shape: ["dcapi", {nonce: bstr, recipientPublicKey: COSE_Key}], the
// key a bare map (0xa5) as multipaz writes and reads it, not tag 24.
func TestEncryptionInfoShape(t *testing.T) {
	_, info, _, err := NewHPKEKeyPair()
	if err != nil {
		t.Fatalf("keygen: %v", err)
	}
	raw, err := BuildEncryptionInfo(info)
	if err != nil {
		t.Fatalf("build: %v", err)
	}
	// ["dcapi", {...}]: 0x82, text "dcapi" (5 bytes -> 0x65), map head 0xa2
	if raw[0] != 0x82 || raw[1] != 0x65 || string(raw[2:7]) != "dcapi" || raw[7] != 0xa2 {
		t.Fatalf("EncryptionInfo shape wrong: %x", raw[:8])
	}
	if !strings.Contains(hex.EncodeToString(raw), "6e6f6e6365") { // "nonce"
		t.Fatal("nonce key missing")
	}
	key := append([]byte{0x72}, "recipientPublicKey"...)
	i := bytes.Index(raw, key)
	if i < 0 || i+len(key) >= len(raw) || raw[i+len(key)] != 0xa5 {
		t.Fatalf("recipientPublicKey is not a bare COSE_Key map: %x", raw[7:])
	}
}

// updateVectors lets a deliberate DeviceRequest change rewrite the committed
// vector: go test ./oid4vp -run TestDeviceRequestBuild -update. Without it
// the test only compares, so a change cannot silently rewrite the bytes the
// fork's wallet parses.
var updateVectors = flag.Bool("update", false, "rewrite zk/testdata/step2a-iso-annex-c/device_request.cbor")

// TestDeviceRequestBuild builds the DeviceRequest for the committed circuits
// and re-parses it with cborsub: what the verifier offers must be exactly what
// a strict decoder reads back, the spec id must round-trip, and the bytes must
// equal the committed vector.
func TestDeviceRequestBuild(t *testing.T) {
	specs := mustSpecs(t)
	req, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", specs)
	if err != nil {
		t.Fatalf("DeviceRequest: %v", err)
	}
	root, err := cborsub.Decode(req, cborsub.DefaultLimits())
	if err != nil {
		t.Fatalf("built request does not decode: %v", err)
	}
	// The keys multipaz 0.99.0's DeviceRequest/ZkRequest.fromDataItem read
	// with a throwing get (and version as a tstr, as the PoC's parser does too).
	get := func(v *cborsub.Value, k string) *cborsub.Value {
		t.Helper()
		got, ok, err := v.MapGet(k)
		if err != nil || !ok {
			t.Fatalf("DeviceRequest lacks %q: %v", k, err)
		}
		return got
	}
	if v := get(root, "version"); v.Kind != cborsub.KText || v.Text != "1.0" {
		t.Fatalf(`DeviceRequest version is %s %q, want the tstr "1.0"`, v.Kind, v.Text)
	}
	items, err := cborsub.Decode(get(&get(root, "docRequests").Array[0], "itemsRequest").Bytes, cborsub.DefaultLimits())
	if err != nil {
		t.Fatalf("itemsRequest: %v", err)
	}
	zkReq := get(get(items, "requestInfo"), "zkRequest")
	if v := get(zkReq, "zkRequired"); v.Kind != cborsub.KBool || !v.Bool {
		t.Fatal("zkRequest.zkRequired is not true")
	}
	spec := &get(zkReq, "systemSpecs").Array[0]
	if get(spec, "zkSystemId").Text != specs[0].SpecID() || get(spec, "id").Text != specs[0].SpecID() {
		t.Fatal("systemSpecs[0] does not carry the spec id under both zkSystemId and id")
	}
	if sys := get(spec, "system").Text; sys != SystemMultipaz {
		t.Fatalf("systemSpecs[0].system = %q, want %q", sys, SystemMultipaz)
	}
	// The spec id must appear in the bytes: the holder echoes it back.
	if !strings.Contains(hex.EncodeToString(req), hex.EncodeToString([]byte(specs[0].SpecID()))) {
		t.Fatal("the built request does not carry the offered spec id")
	}
	const vector = "../zk/testdata/step2a-iso-annex-c/device_request.cbor"
	if *updateVectors {
		if err := os.WriteFile(vector, req, 0o644); err != nil {
			t.Fatalf("write vector: %v", err)
		}
	}
	want, err := os.ReadFile(vector)
	if err != nil {
		t.Fatalf("read vector: %v", err)
	}
	if !bytes.Equal(req, want) {
		t.Fatalf("DeviceRequest differs from the committed vector (rerun with -update if intended)\n got %x\nwant %x", req, want)
	}
}
