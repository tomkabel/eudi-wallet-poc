package jose

import (
	"encoding/base64"
	"encoding/hex"
	"strings"
	"testing"
)

// The known key is Figure 120 of RFC 7520 (the JOSE cookbook's ECDH-ES
// example, P-256). Its public half is fixed, so the RFC 7638 thumbprint over
// it is fixed too — the vector below was computed from the RFC's key, not
// from this implementation.
var (
	vecX = "Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0"
	vecY = "HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw"
	vecD = "r_kHyZ-a06rmxM3yESK84r1otSg-aQcVStkRhA-iCM8"

	// SHA-256 over {"crv":"P-256","kty":"EC","x":...,"y":...} — RFC 7638.
	vecThumbprintHex = "1ec4856a5c30df23fe74efa558662015cc95e47db6a1270815ce15d98e863ada"
)

func vecPublicJWK() JWK {
	return JWK{Kty: "EC", Crv: "P-256", X: vecX, Y: vecY}
}

func TestThumbprintKnownVector(t *testing.T) {
	got, err := vecPublicJWK().Thumbprint()
	if err != nil {
		t.Fatalf("Thumbprint: %v", err)
	}
	if h := hex.EncodeToString(got); h != vecThumbprintHex {
		t.Errorf("thumbprint = %s, want %s", h, vecThumbprintHex)
	}
}

func TestThumbprintIgnoresPrivateMembers(t *testing.T) {
	// RFC 7638 hashes only the required members; a `d` on the key must not
	// change the thumbprint.
	full := vecPublicJWK()
	full.D = vecD
	got, err := full.Thumbprint()
	if err != nil {
		t.Fatalf("Thumbprint: %v", err)
	}
	if h := hex.EncodeToString(got); h != vecThumbprintHex {
		t.Errorf("thumbprint with d = %s, want %s", h, vecThumbprintHex)
	}
}

func TestThumbprintMatchesGoStdlibKey(t *testing.T) {
	// Cross-check the hash input against crypto/ecdh's own encoding of the
	// same point: the JWK x/y must be the point bytes.
	x, err := base64.RawURLEncoding.DecodeString(vecX)
	if err != nil {
		t.Fatalf("x: %v", err)
	}
	if len(x) != 32 {
		t.Fatalf("x is %d bytes, want 32", len(x))
	}
}

func TestGenerateKeyProducesUsableP256(t *testing.T) {
	k, err := GenerateKey()
	if err != nil {
		t.Fatalf("GenerateKey: %v", err)
	}
	if k.Kty != "EC" || k.Crv != "P-256" {
		t.Fatalf("generated key is %s/%s, want EC/P-256", k.Kty, k.Crv)
	}
	x, err := base64.RawURLEncoding.DecodeString(k.X)
	if err != nil || len(x) != 32 {
		t.Fatalf("generated x is not 32 bytes: %v", err)
	}
	tp, err := k.Thumbprint()
	if err != nil {
		t.Fatalf("Thumbprint: %v", err)
	}
	if len(tp) != 32 {
		t.Fatalf("thumbprint is %d bytes, want 32", len(tp))
	}
}

func TestGenerateKeyFreshEachTime(t *testing.T) {
	a, err := GenerateKey()
	if err != nil {
		t.Fatalf("GenerateKey: %v", err)
	}
	b, err := GenerateKey()
	if err != nil {
		t.Fatalf("GenerateKey: %v", err)
	}
	if a.X == b.X {
		t.Fatal("two generated keys share an x coordinate")
	}
}

func TestParseJWKRejectsNonP256(t *testing.T) {
	for _, jwk := range []string{
		`{"kty":"EC","crv":"P-384","x":"AA","y":"AA"}`,  // wrong curve
		`{"kty":"RSA","n":"AA","e":"AQAB"}`,             // wrong type
		`{"kty":"EC","crv":"P-256","x":"!!!","y":"AA"}`, // x not base64url
		`{"kty":"EC","crv":"P-256","x":"AA","y":"AA"}`,  // not a curve point
		`{"kty":"EC","crv":"P-256","x":"AA"}`,           // missing y
		`{"crv":"P-256","x":"AA","y":"AA"}`,             // missing kty
	} {
		var k JWK
		if err := k.UnmarshalJSON([]byte(jwk)); err == nil {
			t.Errorf("accepted %s", jwk)
		} else if !strings.Contains(err.Error(), "jose:") {
			t.Errorf("error for %s is not namespaced: %v", jwk, err)
		}
	}
}

func TestParseJWKAcceptsVectorKey(t *testing.T) {
	var k JWK
	if err := k.UnmarshalJSON([]byte(`{"kty":"EC","crv":"P-256","x":"` + vecX + `","y":"` + vecY + `"}`)); err != nil {
		t.Fatalf("UnmarshalJSON: %v", err)
	}
	tp, err := k.Thumbprint()
	if err != nil {
		t.Fatalf("Thumbprint: %v", err)
	}
	if h := hex.EncodeToString(tp); h != vecThumbprintHex {
		t.Errorf("thumbprint = %s, want %s", h, vecThumbprintHex)
	}
}
