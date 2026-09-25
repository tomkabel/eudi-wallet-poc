// Package jose implements exactly the JWE profile the OpenID4VP
// direct_post.jwt response needs, and refuses everything else (ADR-003):
//
//	alg=ECDH-ES+A256KW, enc=A256GCM, compact serialization, P-256 keys only.
//
// Zero third-party dependencies, like internal/cborsub: the accepted surface
// is closed by construction and pinned to published RFC vectors.
package jose

import (
	"crypto/ecdh"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/sha256"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"fmt"
)

// JWK is an EC P-256 public JWK. Anything else does not unmarshal.
type JWK struct {
	Kty string `json:"kty"`
	Crv string `json:"crv"`
	X   string `json:"x"`
	Y   string `json:"y"`
	D   string `json:"d,omitempty"` // private; accepted in memory, never published
	Kid string `json:"kid,omitempty"`
	Use string `json:"use,omitempty"`
}

// GenerateKey makes a fresh P-256 response key as a JWK.
func GenerateKey() (JWK, error) {
	priv, err := ecdh.P256().GenerateKey(rand.Reader)
	if err != nil {
		return JWK{}, fmt.Errorf("jose: keygen: %w", err)
	}
	pub := priv.PublicKey().Bytes() // uncompressed point: 0x04 || x || y
	return JWK{
		Kty: "EC", Crv: "P-256",
		X: base64.RawURLEncoding.EncodeToString(pub[1:33]),
		Y: base64.RawURLEncoding.EncodeToString(pub[33:65]),
		D: base64.RawURLEncoding.EncodeToString(priv.Bytes()),
	}, nil
}

// UnmarshalJSON parses a JWK and refuses every key this package does not
// support: not EC, not P-256, not a point on P-256, or not decodable
// base64url. The failure is the feature — an accepted-but-unusable key would
// surface as a decrypt error per presentation instead of at configuration.
func (j *JWK) UnmarshalJSON(b []byte) error {
	type alias JWK
	var a alias
	if err := json.Unmarshal(b, &a); err != nil {
		return fmt.Errorf("jose: jwk: %w", err)
	}
	if a.Kty != "EC" {
		return fmt.Errorf("jose: jwk kty %q is not EC", a.Kty)
	}
	if a.Crv != "P-256" {
		return fmt.Errorf("jose: jwk curve %q is not P-256", a.Crv)
	}
	if a.X == "" || a.Y == "" {
		return errorsNew("jose: jwk is missing x or y")
	}
	x, err := base64.RawURLEncoding.DecodeString(a.X)
	if err != nil {
		return fmt.Errorf("jose: jwk x is not base64url: %w", err)
	}
	y, err := base64.RawURLEncoding.DecodeString(a.Y)
	if err != nil {
		return fmt.Errorf("jose: jwk y is not base64url: %w", err)
	}
	if len(x) != 32 || len(y) != 32 {
		return fmt.Errorf("jose: jwk coordinates are %d/%d bytes, want 32", len(x), len(y))
	}
	pub := make([]byte, 65)
	pub[0] = 0x04
	copy(pub[1:], x)
	copy(pub[33:], y)
	if _, err := ecdh.P256().NewPublicKey(pub); err != nil {
		return fmt.Errorf("jose: jwk is not a P-256 point: %w", err)
	}
	*j = JWK(a)
	return nil
}

// MarshalJSON emits the public members only — a `d` must never reach the
// wire, so it is dropped here rather than trusted to every call site.
func (j JWK) MarshalJSON() ([]byte, error) {
	if j.D != "" {
		j.D = ""
	}
	type alias JWK
	return json.Marshal(alias(j))
}

// ECDHKey converts the JWK's public half to a crypto/ecdh key.
func (j JWK) ECDHKey() (*ecdh.PublicKey, error) {
	x, err := base64.RawURLEncoding.DecodeString(j.X)
	if err != nil {
		return nil, fmt.Errorf("jose: jwk x is not base64url: %w", err)
	}
	y, err := base64.RawURLEncoding.DecodeString(j.Y)
	if err != nil {
		return nil, fmt.Errorf("jose: jwk y is not base64url: %w", err)
	}
	if len(x) != 32 || len(y) != 32 {
		return nil, fmt.Errorf("jose: jwk coordinates are %d/%d bytes, want 32", len(x), len(y))
	}
	pub := make([]byte, 65)
	pub[0] = 0x04
	copy(pub[1:], x)
	copy(pub[33:], y)
	return ecdh.P256().NewPublicKey(pub)
}

// PrivateECDHKey converts the JWK's private half to a crypto/ecdh key.
// The private scalar is 32 bytes, zero-padded on the left if shorter.
func (j JWK) PrivateECDHKey() (*ecdh.PrivateKey, error) {
	d, err := base64.RawURLEncoding.DecodeString(j.D)
	if err != nil {
		return nil, fmt.Errorf("jose: jwk d is not base64url: %w", err)
	}
	if len(d) == 0 || len(d) > 32 {
		return nil, fmt.Errorf("jose: jwk d is %d bytes, want 1..32", len(d))
	}
	if len(d) < 32 {
		padded := make([]byte, 32)
		copy(padded[32-len(d):], d)
		d = padded
	}
	return ecdh.P256().NewPrivateKey(d)
}

// Thumbprint is the RFC 7638 SHA-256 hash over the required members in their
// lexicographic order: crv, kty, x, y. Private members are ignored by
// construction (only the four are hashed).
func (j JWK) Thumbprint() ([]byte, error) {
	x, err := base64.RawURLEncoding.DecodeString(j.X)
	if err != nil {
		return nil, fmt.Errorf("jose: jwk x is not base64url: %w", err)
	}
	y, err := base64.RawURLEncoding.DecodeString(j.Y)
	if err != nil {
		return nil, fmt.Errorf("jose: jwk y is not base64url: %w", err)
	}
	// The hash input is the exact required-member set with no whitespace:
	// {"crv":"P-256","kty":"EC","x":"...","y":"..."}
	h := sha256.New()
	h.Write([]byte(`{"crv":"P-256","kty":"EC","x":"`))
	h.Write([]byte(base64.RawURLEncoding.EncodeToString(x)))
	h.Write([]byte(`","y":"`))
	h.Write([]byte(base64.RawURLEncoding.EncodeToString(y)))
	h.Write([]byte(`"}`))
	return h.Sum(nil), nil
}

// errorsNew keeps errors.New local to the two uses above without an import
// alias dance; fmt.Errorf with no verbs would trip go vet's printf check.
func errorsNew(s string) error { return fmt.Errorf("%s", s) }

// JWKSet is the JWKS document shape: a `keys` array of JWKs. It carries only
// public members — JWK.MarshalJSON drops any private material.
type JWKSet struct {
	Keys []JWK `json:"keys"`
}

// ThumbprintB64 returns the key's RFC 7638 thumbprint, base64url-encoded —
// the form logs and configs quote.
func ThumbprintB64(k *JWK) string {
	tp, err := k.Thumbprint()
	if err != nil {
		return ""
	}
	return base64.RawURLEncoding.EncodeToString(tp)
}

// LoadPEMKey parses a PEM-encoded EC P-256 private key, either SEC 1
// ("EC PRIVATE KEY", the traditional form) or PKIX ("PRIVATE KEY", PKCS#8).
func LoadPEMKey(pemBytes []byte) (*JWK, error) {
	block, _ := pem.Decode(pemBytes)
	if block == nil {
		return nil, errorsNew("no PEM block found")
	}
	var priv *ecdsa.PrivateKey
	if k, err := x509.ParseECPrivateKey(block.Bytes); err == nil {
		priv = k
	} else if k8, err := x509.ParsePKCS8PrivateKey(block.Bytes); err == nil {
		pk, ok := k8.(*ecdsa.PrivateKey)
		if !ok {
			return nil, errorsNew("PKCS#8 key is not an EC key")
		}
		priv = pk
	} else {
		return nil, errorsNew("not an EC private key (tried SEC 1 and PKCS#8)")
	}
	if priv.Curve != elliptic.P256() {
		return nil, errorsNew("EC key is not P-256")
	}
	// priv.D is deprecated (Go 1.26): the raw scalar comes from the crypto/ecdh
	// conversion instead — for P-256, Bytes() is the same 32-byte big-endian
	// scalar priv.D held, and the conversion rejects out-of-range keys.
	ecdhKey, err := priv.ECDH()
	if err != nil {
		return nil, errorsNew("EC key is not a usable ECDH private key")
	}
	return &JWK{
		Kty: "EC", Crv: "P-256",
		X: base64.RawURLEncoding.EncodeToString(priv.X.FillBytes(make([]byte, 32))),
		Y: base64.RawURLEncoding.EncodeToString(priv.Y.FillBytes(make([]byte, 32))),
		D: base64.RawURLEncoding.EncodeToString(ecdhKey.Bytes()),
	}, nil
}
