package oid4vp

import (
	"encoding/hex"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
)

// The direct_post.jwt half of the two-sided pin: the transcript derived from a
// REAL P-256 key's RFC 7638 thumbprint must be byte-identical to what the
// Python wallet derives from the same JWK. The key is fixed (RFC 7520 §5.5
// Figure 120's key), so the thumbprint — and with it the whole transcript —
// is a golden vector, not a property test.
//
// The Python side asserts the same bytes in wallet/test_transcript.py
// (EncryptedTranscriptGoldenVector): the same JWK literal, thumbprinted with
// the RFC 7638 member set, fed through the wallet's session_transcript.
// Both files must be updated together.
func TestSessionTranscriptGoldenVectorRealJWKThumbprint(t *testing.T) {
	// RFC 7520 §5.5 Figure 120 (public half only).
	const jwkJSON = `{"kty":"EC","crv":"P-256",` +
		`"x":"Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0",` +
		`"y":"HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw"}`
	var key jose.JWK
	if err := key.UnmarshalJSON([]byte(jwkJSON)); err != nil {
		t.Fatalf("unmarshal JWK: %v", err)
	}
	tp, err := key.Thumbprint()
	if err != nil {
		t.Fatalf("thumbprint: %v", err)
	}
	wantTP := "1ec4856a5c30df23fe74efa558662015cc95e47db6a1270815ce15d98e863ada"
	if got := hex.EncodeToString(tp); got != wantTP {
		t.Fatalf("thumbprint = %s, want the pinned RFC 7638 vector %s", got, wantTP)
	}

	got, err := SessionTranscript(vecClientID, vecNonce, tp, vecResponseURI)
	if err != nil {
		t.Fatalf("SessionTranscript: %v", err)
	}
	if h := hex.EncodeToString(got); h != vecEncryptedRealKey {
		t.Errorf("transcript does not match the Python wallet's:\n go     = %s\n python = %s",
			h, vecEncryptedRealKey)
	}
}

// The thumbprint bytes fed to SessionTranscript must be a real SHA-256 of the
// JWK member set, not an arbitrary 32 bytes: the wallet derives them from the
// published JWK, so any other derivation breaks every proof.
func TestSessionTranscriptThumbprintIsTheJWKHash(t *testing.T) {
	var key jose.JWK
	if err := key.UnmarshalJSON([]byte(
		`{"kty":"EC","crv":"P-256","x":"Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0",` +
			`"y":"HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw"}`)); err != nil {
		t.Fatalf("unmarshal JWK: %v", err)
	}
	tp, _ := key.Thumbprint()
	if tp[0] != 0x1e || tp[1] != 0xc4 {
		t.Fatalf("thumbprint prefix %x, want 1ec4 (the pinned vector's)", tp[:2])
	}
}

const vecEncryptedRealKey = "83f6f682714f70656e494434565048616e646f76657258202a" +
	"498fe27b5e6a0f68911225669f840203824880aa67678612cfb6064f759f03"
