package oid4vp

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"testing"
)

const (
	clientID = "x509_san_dns:verifier.example.ee"
	nonce    = "s1U6zVsPQ0GQ0hZ4mQ0h0w"
	respURI  = "https://verifier.example.ee/present/response/abc"
)

// The transcript must be [null, null, ["OpenID4VPHandover", h]] with h the
// SHA-256 of the CBOR-encoded OpenID4VPHandoverInfo.
func TestSessionTranscriptShape(t *testing.T) {
	st, err := SessionTranscript(clientID, nonce, nil, respURI)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.HasPrefix(st, []byte{0x83, 0xf6, 0xf6, 0x82, 0x71}) {
		t.Fatalf("unexpected prefix %s", hex.EncodeToString(st[:8]))
	}
	if !bytes.Contains(st, []byte("OpenID4VPHandover")) {
		t.Fatal("handover identifier missing")
	}
	// tail must be bstr(32) = 0x58 0x20 followed by the digest
	tail := st[len(st)-34:]
	if tail[0] != 0x58 || tail[1] != 0x20 {
		t.Fatalf("expected a 32-byte hash, got header %x %x", tail[0], tail[1])
	}
	info, err := handoverInfo(clientID, nonce, nil, respURI)
	if err != nil {
		t.Fatal(err)
	}
	want := sha256.Sum256(info)
	if !bytes.Equal(tail[2:], want[:]) {
		t.Fatal("hash is not sha256 of the handover info encoding")
	}
}

// Changing any bound parameter must change the transcript — this is the
// property that stops a proof made for one verifier being replayed at another.
func TestSessionTranscriptBinding(t *testing.T) {
	base, _ := SessionTranscript(clientID, nonce, nil, respURI)
	for _, tc := range []struct {
		name          string
		cid, non, uri string
	}{
		{"different client_id", "x509_san_dns:evil.example", nonce, respURI},
		{"different nonce", clientID, "AAAAAAAAAAAAAAAAAAAAAA", respURI},
		{"different response_uri", clientID, nonce, respURI + "/x"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			other, err := SessionTranscript(tc.cid, tc.non, nil, tc.uri)
			if err != nil {
				t.Fatal(err)
			}
			if bytes.Equal(base, other) {
				t.Fatal("transcript did not change")
			}
		})
	}
}

func TestEncryptedResponseUsesThumbprint(t *testing.T) {
	tp := make([]byte, 32)
	withTP, err := SessionTranscript(clientID, nonce, tp, respURI)
	if err != nil {
		t.Fatal(err)
	}
	withoutTP, _ := SessionTranscript(clientID, nonce, nil, respURI)
	if bytes.Equal(withTP, withoutTP) {
		t.Fatal("an encrypted response must bind the key thumbprint")
	}
	if _, err := SessionTranscript(clientID, nonce, []byte{1, 2, 3}, respURI); err == nil {
		t.Fatal("a short thumbprint must be rejected")
	}
}

func TestDCQLSingle(t *testing.T) {
	q := AgeQuery("age", "ee.riik.poa.1", "ee.riik.poa.1", "age_over_18")
	c, err := q.Single()
	if err != nil {
		t.Fatal(err)
	}
	if c.Namespace() != "ee.riik.poa.1" || c.Element() != "age_over_18" {
		t.Fatalf("bad destructure: %s / %s", c.Namespace(), c.Element())
	}
	if _, err := (DCQL{}).Single(); err == nil {
		t.Fatal("empty query must be rejected")
	}
}
