package oid4vp

import (
	"crypto/sha256"
	"encoding/hex"
	"testing"
)

// Cross-language golden vectors for the OpenID4VP 1.0 Appendix B.2.6.1
// SessionTranscript.
//
// The verifier is in Go and the holder in Python, and they derive this
// independently. If they disagree by a single byte, every proof fails
// verification with an error that says nothing about why — the transcript is an
// input to the circuit, not something either side transmits. So the agreement
// has to be pinned by a test rather than assumed.
//
// The same vectors are asserted from the Python side in
// wallet/test_transcript.py. Both must be updated together, and neither is
// allowed to be regenerated from the implementation it is testing: to change a
// vector, compute it from the *other* implementation and paste it here.
//
//	cd wallet && python3 test_transcript.py --print
const (
	vecClientID    = "x509_san_dns:verifier.example.ee"
	vecNonce       = "s1U6zVsPQ0GQ0hZ4mQ0h0w"
	vecResponseURI = "https://verifier.example.ee/present/response/abc"

	// direct_post: the response is not encrypted, so jwkThumbprint is null.
	vecUnencrypted = "83f6f682714f70656e494434565048616e646f766572582034" +
		"08b9522985938a5b2501c88b62866ab854c42b62a7e8be4e257d5b0f674cef"

	// direct_post.jwt: jwkThumbprint is the RFC 7638 SHA-256 thumbprint of the
	// verifier's response-encryption key. Any 32 bytes exercise the encoding.
	vecEncrypted = "83f6f682714f70656e494434565048616e646f766572582047" +
		"afacda2c896b96bc0d1062e2b81345e504d3ec2a4c7d521458360953bf662e"
)

// vecThumbprint is a stand-in for a real JWK thumbprint. The Python side
// derives it the same way so the vector is reproducible from either language
// without shipping a key.
func vecThumbprint() []byte {
	sum := sha256.Sum256([]byte("EE-EUDIW transcript test vector"))
	return sum[:]
}

func TestSessionTranscriptGoldenVectorUnencrypted(t *testing.T) {
	got, err := SessionTranscript(vecClientID, vecNonce, nil, vecResponseURI)
	if err != nil {
		t.Fatalf("SessionTranscript: %v", err)
	}
	if h := hex.EncodeToString(got); h != vecUnencrypted {
		t.Errorf("transcript does not match the Python wallet's:\n go     = %s\n python = %s",
			h, vecUnencrypted)
	}
}

func TestSessionTranscriptGoldenVectorEncrypted(t *testing.T) {
	got, err := SessionTranscript(vecClientID, vecNonce, vecThumbprint(), vecResponseURI)
	if err != nil {
		t.Fatalf("SessionTranscript: %v", err)
	}
	if h := hex.EncodeToString(got); h != vecEncrypted {
		t.Errorf("transcript does not match the Python wallet's:\n go     = %s\n python = %s",
			h, vecEncrypted)
	}
}

// The two vectors must differ, or the encrypted case is not being exercised at
// all and both tests would pass against an implementation that ignored the
// thumbprint.
func TestSessionTranscriptVectorsDiffer(t *testing.T) {
	if vecUnencrypted == vecEncrypted {
		t.Fatal("the encrypted and unencrypted vectors are identical; " +
			"the jwkThumbprint is not reaching the transcript")
	}
}
