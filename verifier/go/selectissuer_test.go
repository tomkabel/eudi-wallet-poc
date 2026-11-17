package main

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/asn1"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"math/big"
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
)

// NEW-S2-1 at the presentation boundary: with an IACA in the trust store, a
// DS certificate carrying the pinned key is accepted only when it chains to
// that IACA under the ISO DS profile. Key pinning alone used to accept a
// self-signed certificate around a trusted key.
func TestSelectIssuerChainValidatesDS(t *testing.T) {
	iacaKey, _ := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	dsKey, _ := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	now := time.Now()
	iacaTpl := &x509.Certificate{SerialNumber: big.NewInt(1), Subject: pkix.Name{CommonName: "IACA"},
		NotBefore: now.Add(-time.Hour), NotAfter: now.Add(time.Hour), BasicConstraintsValid: true, IsCA: true,
		MaxPathLenZero: true, KeyUsage: x509.KeyUsageCertSign}
	iacaDER, err := x509.CreateCertificate(rand.Reader, iacaTpl, iacaTpl, &iacaKey.PublicKey, iacaKey)
	if err != nil {
		t.Fatal(err)
	}
	iaca, _ := x509.ParseCertificate(iacaDER)
	ds := func(eku []asn1.ObjectIdentifier, signer *ecdsa.PrivateKey, parent *x509.Certificate, notAfter time.Time) []byte {
		tpl := &x509.Certificate{SerialNumber: big.NewInt(2), Subject: pkix.Name{CommonName: "DS"},
			NotBefore: now.Add(-time.Hour), NotAfter: notAfter, KeyUsage: x509.KeyUsageDigitalSignature,
			UnknownExtKeyUsage: eku}
		if parent == nil {
			parent = tpl
		}
		der, err := x509.CreateCertificate(rand.Reader, tpl, parent, &dsKey.PublicKey, signer)
		if err != nil {
			t.Fatal(err)
		}
		return der
	}
	mdlDS := []asn1.ObjectIdentifier{{1, 0, 18013, 5, 1, 2}}
	pt, _ := dsKey.PublicKey.ECDH()
	b := pt.Bytes()
	issuer := map[string]string{"name": "iss", "doc_type": "org.iso.18013.5.1.mDL", "namespace": "org.iso.18013.5.1",
		"pkx": "0x" + hex.EncodeToString(b[1:33]), "pky": "0x" + hex.EncodeToString(b[33:65])}
	store := func(withIACA bool) *oid4vp.TrustStore {
		is := map[string]string{}
		for k, v := range issuer {
			is[k] = v
		}
		if withIACA {
			is["iaca"] = base64.StdEncoding.EncodeToString(iacaDER)
		}
		raw, _ := json.Marshal(map[string]any{"issuers": []any{is}})
		path := filepath.Join(t.TempDir(), "issuers.json")
		if err := os.WriteFile(path, raw, 0o600); err != nil {
			t.Fatal(err)
		}
		ts, err := oid4vp.LoadTrustStore(path)
		if err != nil {
			t.Fatal(err)
		}
		return ts
	}
	cases := []struct {
		name   string
		chain  []byte
		iaca   bool
		accept bool
		reason string
	}{
		{"ds under the IACA", ds(mdlDS, iacaKey, iaca, now.Add(time.Hour)), true, true, ""},
		{"self-signed DS around the pinned key", ds(mdlDS, dsKey, nil, now.Add(time.Hour)), true, false, "does not chain"},
		{"DS without the mdlDS EKU", ds(nil, iacaKey, iaca, now.Add(time.Hour)), true, false, "mdlDS"},
		{"expired DS", ds(mdlDS, iacaKey, iaca, now.Add(-time.Minute)), true, false, "expired"},
		{"dev store without IACA pins the key only", ds(mdlDS, dsKey, nil, now.Add(time.Hour)), false, true, ""},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			p := &presenter{trust: store(c.iaca)}
			_, err := p.selectIssuer([][]byte{c.chain}, "org.iso.18013.5.1.mDL")
			if c.accept != (err == nil) {
				t.Fatalf("accept=%v, err=%v", c.accept, err)
			}
			if err != nil && !strings.Contains(err.Error(), c.reason) {
				t.Fatalf("refused for the wrong reason: %v (want %q)", err, c.reason)
			}
		})
	}
}
