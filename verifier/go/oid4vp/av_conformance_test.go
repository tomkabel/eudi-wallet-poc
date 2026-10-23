package oid4vp

import (
	"bytes"
	"encoding/base64"
	"os"
	"strings"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

// Audit probes call candidate primitives; these are scoped checks, not a full
// implementation of the inherited ISO schemas or cryptographic proof rules.
func TestAVConformanceRequestParts(t *testing.T) {
	_, _, wire, err := NewHPKEKeyPair()
	if err != nil {
		t.Fatal(err)
	}
	request, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", mustSpecs(t))
	if err != nil {
		t.Fatal(err)
	}
	if len(wire) == 0 || len(request) == 0 {
		t.Fatal("RP request must contain encryptionInfo and deviceRequest")
	}
}
func TestAVConformanceEncryptionEncoding(t *testing.T) {
	_, _, wire, err := NewHPKEKeyPair()
	if err != nil {
		t.Fatal(err)
	}
	raw, err := base64.RawURLEncoding.DecodeString(wire)
	if err != nil || strings.Contains(wire, "=") {
		t.Fatal("encryptionInfo must be unpadded base64url")
	}
	if len(raw) == 0 {
		t.Fatal("empty encryptionInfo")
	}
}
func TestAVConformanceDeviceEncoding(t *testing.T) {
	raw, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", mustSpecs(t))
	if err != nil {
		t.Fatal(err)
	}
	wire := Base64URL(raw)
	decoded, err := base64.RawURLEncoding.DecodeString(wire)
	if err != nil || strings.Contains(wire, "=") || !bytes.Equal(raw, decoded) {
		t.Fatal("deviceRequest must round trip as unpadded base64url CBOR")
	}
	if len(decoded) == 0 {
		t.Fatal("empty deviceRequest")
	}
}
func TestAVConformanceResponseEncoding(t *testing.T) {
	raw := BuildDCAPIEnvelope([]byte{1, 2}, []byte{3, 4})
	wire := Base64URL(raw)
	decoded, err := base64.RawURLEncoding.DecodeString(wire)
	if err != nil || strings.Contains(wire, "=") || !bytes.Equal(raw, decoded) {
		t.Fatal("EncryptedResponse must round trip as unpadded base64url CBOR")
	}
	if len(decoded) == 0 {
		t.Fatal("empty encrypted response")
	}
}
func TestAVConformanceEnvelope(t *testing.T) {
	enc, ct, err := ParseEnvelope(BuildDCAPIEnvelope([]byte{1, 2}, []byte{3, 4}))
	if err != nil || !bytes.Equal(enc, []byte{1, 2}) || !bytes.Equal(ct, []byte{3, 4}) {
		t.Fatal("EncryptedResponse must retain enc and cipherText byte strings")
	}
	for name, raw := range map[string][]byte{"discriminator": {0x82, 0x61, 'x', 0xa0}, "enc_type": {0x82, 0x65, 'd', 'c', 'a', 'p', 'i', 0xa2, 0x63, 'e', 'n', 'c', 0x01, 0x6a, 'c', 'i', 'p', 'h', 'e', 'r', 'T', 'e', 'x', 't', 0x41, 0x01}, "truncated": {0x82}, "missing_fields": {0x82, 0x65, 'd', 'c', 'a', 'p', 'i', 0xa0}} {
		t.Run("negative_"+name, func(t *testing.T) {
			if _, _, err := ParseEnvelope(raw); err == nil {
				t.Fatal("malformed EncryptedResponse unexpectedly accepted")
			}
		})
	}
}
func TestAVConformanceHPKE(t *testing.T) {
	priv, _, _, err := NewHPKEKeyPair()
	if err != nil {
		t.Fatal(err)
	}
	info := []byte("AV transcript")
	pt := []byte{0xa1, 0x66, 's', 't', 'a', 't', 'u', 's', 0}
	ct, err := SealTo(priv.PublicKey(), info, pt)
	if err != nil {
		t.Fatal(err)
	}
	got, err := Open(priv, info, ct)
	if err != nil || !bytes.Equal(got, pt) {
		t.Fatal("HPKE DeviceResponse round trip failed")
	}
	t.Run("negative_ciphertext", func(t *testing.T) {
		bad := append([]byte(nil), ct...)
		bad[len(bad)-1] ^= 1
		if _, err := Open(priv, info, bad); err == nil {
			t.Fatal("tampered HPKE accepted")
		}
	})
	t.Run("negative_transcript", func(t *testing.T) {
		if _, err := Open(priv, []byte("other transcript"), ct); err == nil {
			t.Fatal("wrong HPKE transcript accepted")
		}
	})
	t.Run("negative_recipient", func(t *testing.T) {
		other, _, _, err := NewHPKEKeyPair()
		if err != nil {
			t.Fatal(err)
		}
		if _, err := Open(other, info, ct); err == nil {
			t.Fatal("foreign HPKE recipient accepted")
		}
	})
	t.Run("negative_short", func(t *testing.T) {
		if _, err := Open(priv, info, []byte{1}); err == nil {
			t.Fatal("short HPKE ciphertext accepted")
		}
	})
}
func TestAVConformanceCircuitAllowlist(t *testing.T) {
	meta := &Meta{ZkSystemType: []ZkSystemTypeSpec{avLocalSpec()}}
	if _, err := ZkSystemTypeAllowlist(meta, mustSpecs(t)); err != nil {
		t.Fatal(err)
	}
	t.Run("negative_unlisted", func(t *testing.T) {
		if _, err := ZkSystemTypeAllowlist(meta, nil); err == nil {
			t.Fatal("unlisted circuit accepted")
		}
	})
}

func TestAVConformanceDeviceRequestProperties(t *testing.T) {
	if os.Getenv("AV_CONFORMANCE") != "1" {
		t.Skip("AV_CONFORMANCE=1 enables AV audit probes")
	}
	request, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", mustSpecs(t))
	if err != nil || len(request) == 0 {
		t.Fatal("AV DeviceRequest construction failed")
	}
	if _, err := DeviceRequest("", "eu.europa.ec.av.1", "age_over_18", nil); err == nil {
		t.Fatal("empty docType accepted")
	}
	if _, err := DeviceRequest("eu.europa.ec.av.1", "", "age_over_18", nil); err == nil {
		t.Fatal("empty namespace accepted")
	}
	if _, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "", nil); err == nil {
		t.Fatal("empty element accepted")
	}
	t.Run("negative_too_many_specs", func(t *testing.T) {
		specs := make([]circuits.Circuit, 24)
		for i := range specs {
			specs[i] = mustSpecs(t)[0]
		}
		if _, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", specs); err == nil {
			t.Fatal("oversized systemSpecs accepted")
		}
	})
	t.Run("empty_specs_accept", func(t *testing.T) {
		if _, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", nil); err != nil {
			t.Fatal(err)
		}
	})
}

func TestAVConformanceTranscriptProperties(t *testing.T) {
	if _, err := DCAPIHandover("", "https://rp.example"); err == nil {
		t.Fatal("empty encryptionInfo accepted")
	}
	if _, err := DCAPIHandover("abc", ""); err == nil {
		t.Fatal("empty origin accepted")
	}
	a, err := DCAPIHandover("abc", "https://rp.example")
	if err != nil {
		t.Fatal(err)
	}
	b, err := DCAPIHandover("abc", "https://other.example")
	if err != nil {
		t.Fatal(err)
	}
	if bytes.Equal(a, b) {
		t.Fatal("origin is not bound into DC API handover")
	}
}

func TestAVConformanceSystemSpecProperties(t *testing.T) {
	if _, err := zkSystemSpec(mustSpecs(t)[0]); err != nil {
		t.Fatal(err)
	}
}
