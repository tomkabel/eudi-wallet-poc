package oid4vp

import (
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"os"
	"testing"
	"time"
)

// trustVectors is tests/vectors/trust/cases.json, the differential suite the
// Android CertificateChainValidator runs too (PKI plan NEW-S2-4).
type trustVectors struct {
	At    time.Time `json:"at"`
	Cases []struct {
		Name    string   `json:"name"`
		Profile string   `json:"profile"`
		Chain   []string `json:"chain"`
		Anchors []string `json:"anchors"`
		Expect  string   `json:"expect"`
		Why     string   `json:"why"`
	} `json:"cases"`
}

func TestSharedTrustVectors(t *testing.T) {
	b, err := os.ReadFile("../../../tests/vectors/trust/cases.json")
	if err != nil {
		t.Fatal(err)
	}
	var v trustVectors
	if err := json.Unmarshal(b, &v); err != nil {
		t.Fatal(err)
	}
	if len(v.Cases) < 15 {
		t.Fatalf("only %d trust vectors; the suite has shrunk", len(v.Cases))
	}
	dec := func(t *testing.T, s string) []byte {
		d, err := base64.StdEncoding.DecodeString(s)
		if err != nil {
			t.Fatal(err)
		}
		return d
	}
	for _, c := range v.Cases {
		t.Run(c.Name, func(t *testing.T) {
			var chain [][]byte
			for _, s := range c.Chain {
				chain = append(chain, dec(t, s))
			}
			var anchors []*x509.Certificate
			for _, s := range c.Anchors {
				a, err := x509.ParseCertificate(dec(t, s))
				if err != nil {
					t.Fatal(err)
				}
				anchors = append(anchors, a)
			}
			_, err := VerifyChain(chain, anchors, v.At, ChainProfile(c.Profile))
			got := "accept"
			if err != nil {
				got = "refuse"
			}
			if got != c.Expect {
				t.Fatalf("%s: got %s, want %s (%s); err=%v", c.Name, got, c.Expect, c.Why, err)
			}
			if err != nil {
				t.Logf("refused: %v", err)
			}
		})
	}
}
