package oid4vp

import (
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/x509"
	"crypto/x509/pkix"
	"encoding/base64"
	"math/big"
	"strings"
	"sync"
	"testing"
	"time"
)

var testIACA = sync.OnceValue(func() string {
	k, err := ecdsa.GenerateKey(elliptic.P256(), rand.Reader)
	if err != nil {
		panic(err)
	}
	tpl := &x509.Certificate{
		SerialNumber: big.NewInt(1), Subject: pkix.Name{CommonName: "test IACA"},
		NotBefore: time.Now().Add(-time.Hour), NotAfter: time.Now().Add(time.Hour),
		BasicConstraintsValid: true, IsCA: true, MaxPathLenZero: true,
		KeyUsage: x509.KeyUsageCertSign | x509.KeyUsageCRLSign,
	}
	der, err := x509.CreateCertificate(rand.Reader, tpl, tpl, &k.PublicKey, k)
	if err != nil {
		panic(err)
	}
	return base64.StdEncoding.EncodeToString(der)
})

// NEW-S2-1: a signed (production) store must name each issuer's IACA, so the
// DS certificate is chain-validated and not only key-matched.
func TestSignedTrustStoreRequiresIACA(t *testing.T) {
	k := signedKey(t)
	issuers := batteryIssuers()
	issuers[0].IACA = ""
	_, err := LoadSignedTrustStore(writeSignedTrustStore(t, k, issuers), k, nil, time.Now, false)
	if err == nil || !strings.Contains(err.Error(), "has no iaca") {
		t.Fatalf("signed store without iaca: err = %v, want a refusal naming iaca", err)
	}
}

func TestTrustStoreRefusesNonCAIACA(t *testing.T) {
	is := batteryIssuers()
	is[0].IACA = base64.StdEncoding.EncodeToString([]byte("not a certificate"))
	if _, err := buildTrustStore(is); err == nil {
		t.Fatal("garbage iaca loaded")
	}
}
