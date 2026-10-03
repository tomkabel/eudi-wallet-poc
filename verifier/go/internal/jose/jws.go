package jose

import (
	"crypto/ecdh"
	"crypto/ecdsa"
	"crypto/elliptic"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"math/big"
	"strings"
)

// This file is the JWS half of the minimal-jose doctrine (ADR-003): one
// algorithm, compact serialization, P-256 only — ES256 (JWS) mirroring the
// JWE profile's ECDH-ES+A256KW / A256GCM. It signs OpenID4VP request objects
// (JAR, RFC 9101) and the signed trust store (B3). Anything else is
// ErrUnsupportedAlgorithm — a refusal, never a fallback.

// ErrUnsupportedAlgorithm marks an alg/typ outside the ES256 profile.
var ErrUnsupportedAlgorithm = errorsNew("jose: unsupported outside the ES256 / P-256 JWS profile")

// ErrSig marks a JWS whose signature does not verify: malformed base64,
// missing members, or a bad signature all land here so a caller cannot
// branch on why.
var ErrSig = errorsNew("jose: signature verification failed")

// jwsAlg is the one signature algorithm this package speaks.
const jwsAlg = "ES256"

// jwsTypObject is the typ the verifier emits for signed request objects;
// jwsTypTrustStore is the typ on the signed trust store document. A header
// with a different typ is refused.
const (
	// JWSTypObject rides on signed OpenID4VP request objects (JAR).
	JWSTypObject = "JWT"
	// JWSTypTrustStore rides on the signed trust store document (B3).
	JWSTypTrustStore = "trust-store+json"

	jwsTypObject     = JWSTypObject
	jwsTypTrustStore = JWSTypTrustStore
)

// SignJWS produces a compact ES256 JWS over payload with the private key in
// k (a JWK carrying d, as GenerateKey and LoadPEMKey produce). The protected
// header is fixed: {"alg":"ES256","typ":typ,"kid":kid} — anything else the
// caller might want is out of profile by construction. kid, when non-empty,
// is ThumbprintB64(public(k)) by convention, matching the JWE/JWKS usage.
func SignJWS(k JWK, typ, payload string) (string, error) {
	if k.D == "" {
		return "", fmt.Errorf("%w: signing key carries no private member (d)", ErrUnsupportedAlgorithm)
	}
	if k.Kty != "EC" || k.Crv != "P-256" {
		return "", fmt.Errorf("%w: key is %s/%s, want EC/P-256", ErrUnsupportedAlgorithm, k.Kty, k.Crv)
	}
	if typ != jwsTypObject && typ != jwsTypTrustStore {
		return "", fmt.Errorf("%w: typ %q is not a profile typ", ErrUnsupportedAlgorithm, typ)
	}
	priv, err := k.PrivateECDHKey()
	if err != nil {
		return "", fmt.Errorf("%w: signing key unusable: %v", ErrUnsupportedAlgorithm, err)
	}
	// crypto/ecdh hides the scalar; recover it through the stdlib P-256
	// private key the scalar encodes (32-byte big-endian, zero-padded).
	privEC, err := ecdsaPrivFromECDH(priv)
	if err != nil {
		return "", err
	}

	header, err := json.Marshal(struct {
		Alg string `json:"alg"`
		Typ string `json:"typ"`
		Kid string `json:"kid,omitempty"`
	}{Alg: jwsAlg, Typ: typ, Kid: k.Kid})
	if err != nil {
		return "", fmt.Errorf("jose: header: %w", err)
	}
	signingInput := base64.RawURLEncoding.EncodeToString(header) + "." +
		base64.RawURLEncoding.EncodeToString([]byte(payload))
	digest := sha256.Sum256([]byte(signingInput))
	r, s, err := ecdsa.Sign(rand.Reader, privEC, digest[:])
	if err != nil {
		return "", fmt.Errorf("jose: sign: %w", err)
	}
	return signingInput + "." + base64.RawURLEncoding.EncodeToString(sigRS(r, s)), nil
}

// VerifyJWS checks a compact JWS and returns the payload bytes. The header
// must be exactly the profile: alg=ES256, typ one of the profile typs; a kid
// member is accepted only as an unverified hint (key selection is the
// caller's, the same rule the JWE half applies to its headers).
func VerifyJWS(token string) (payload []byte, err error) {
	payload, _, err = parseJWS(token)
	return payload, err
}

// parseJWS is VerifyJWS that also returns the header typ.
func parseJWS(token string) (payload []byte, typ string, err error) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return nil, "", fmt.Errorf("%w: compact JWS is %d parts, want 3", ErrParse, len(parts))
	}
	headerRaw, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		return nil, "", fmt.Errorf("%w: header is not base64url: %v", ErrParse, err)
	}
	var h struct {
		Alg string `json:"alg"`
		Typ string `json:"typ"`
		Kid string `json:"kid,omitempty"`
	}
	if err := json.Unmarshal(headerRaw, &h); err != nil {
		return nil, "", fmt.Errorf("%w: header is not valid JSON: %v", ErrParse, err)
	}
	if h.Alg != jwsAlg {
		return nil, "", fmt.Errorf("%w: alg %q, want ES256", ErrUnsupportedAlgorithm, h.Alg)
	}
	if h.Typ != jwsTypObject && h.Typ != jwsTypTrustStore {
		return nil, "", fmt.Errorf("%w: typ %q is not a profile typ", ErrUnsupportedAlgorithm, h.Typ)
	}
	sig, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil {
		return nil, "", fmt.Errorf("%w: signature is not base64url: %v", ErrSig, err)
	}
	if _, _, err := parseRS(sig); err != nil {
		return nil, "", fmt.Errorf("%w: %v", ErrSig, err)
	}
	payloadRaw, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return nil, "", fmt.Errorf("%w: payload is not base64url: %v", ErrParse, err)
	}
	// Structure-only: the signature bytes must decode, but cryptographic
	// verification needs the signer's key — that is VerifyJWSWithKey. A
	// caller that only needs the payload (e.g. to route on typ) gets it
	// here; nothing trusts the payload as verified until WithKey passes.
	return payloadRaw, h.Typ, nil
}

// VerifyJWSWithKey additionally pins the signing key and the typ: the
// signature must verify under pub and the header typ must be exactly typ, so
// a token signed for one purpose (a request object) can never pass as
// another (a trust store) even if the two ever share a key. Returns the
// payload on success.
func VerifyJWSWithKey(token string, pub JWK, typ string) ([]byte, error) {
	payload, gotTyp, err := parseJWS(token)
	if err != nil {
		return nil, err
	}
	if gotTyp != typ {
		return nil, fmt.Errorf("%w: typ %q, want %q", ErrUnsupportedAlgorithm, gotTyp, typ)
	}
	pk, err := pub.ECDHKey()
	if err != nil {
		return nil, fmt.Errorf("%w: verification key unusable: %v", ErrUnsupportedAlgorithm, err)
	}
	ecdsaPub, err := ecdsaPubFromECDH(pk)
	if err != nil {
		return nil, err
	}
	parts := strings.Split(token, ".")
	sig, err := base64.RawURLEncoding.DecodeString(parts[2])
	if err != nil {
		return nil, fmt.Errorf("%w: signature is not base64url: %v", ErrSig, err)
	}
	r, s, err := parseRS(sig)
	if err != nil {
		return nil, fmt.Errorf("%w: %v", ErrSig, err)
	}
	digest := sha256.Sum256([]byte(parts[0] + "." + parts[1]))
	if !ecdsa.Verify(ecdsaPub, digest[:], r, s) {
		return nil, ErrSig
	}
	return payload, nil
}

// sigRS encodes an ECDSA signature as the 64-byte JWS r||s form.
func sigRS(r, s *big.Int) []byte {
	out := make([]byte, 64)
	r.FillBytes(out[:32])
	s.FillBytes(out[32:])
	return out
}

// parseRS decodes the 64-byte r||s form; anything else is an error.
func parseRS(sig []byte) (r, s *big.Int, err error) {
	if len(sig) != 64 {
		return nil, nil, fmt.Errorf("signature is %d bytes, want 64", len(sig))
	}
	return new(big.Int).SetBytes(sig[:32]), new(big.Int).SetBytes(sig[32:]), nil
}

// ecdhToCurve is elliptic.P256 under the crypto/ecdh identity this package
// already pins: every conversion here is P-256 and refuses anything else.
func ecdhToCurve() elliptic.Curve { return elliptic.P256() }

// ecdsaPrivFromECDH rebuilds the stdlib signing key from the crypto/ecdh
// private key: ecdh stores the scalar exactly as a P-256 private key.
// Reconstruction goes through the non-deprecated SEC 1 raw forms (Bytes /
// ParseRawPrivateKey / ParseUncompressedPublicKey) — the big.Int fields are
// deprecated since Go 1.26 and the conversion round-trip keeps the scalar
// byte-identical.
func ecdsaPrivFromECDH(priv *ecdh.PrivateKey) (*ecdsa.PrivateKey, error) {
	b := priv.Bytes()
	if len(b) != 32 {
		return nil, fmt.Errorf("%w: ecdh private scalar is %d bytes", ErrUnsupportedAlgorithm, len(b))
	}
	privEC, err := ecdsa.ParseRawPrivateKey(ecdhToCurve(), b)
	if err != nil {
		return nil, fmt.Errorf("%w: ecdh private scalar unusable: %v", ErrUnsupportedAlgorithm, err)
	}
	// Recover the public point from the JWK form of the same key.
	pubB := priv.PublicKey().Bytes()
	if len(pubB) != 65 || pubB[0] != 0x04 {
		return nil, fmt.Errorf("%w: ecdh public point is malformed", ErrUnsupportedAlgorithm)
	}
	pub, err := ecdsa.ParseUncompressedPublicKey(ecdhToCurve(), pubB)
	if err != nil {
		return nil, fmt.Errorf("%w: ecdh public point is malformed: %v", ErrUnsupportedAlgorithm, err)
	}
	privEC.PublicKey = *pub
	return privEC, nil
}

// ecdsaPubFromECDH rebuilds a stdlib verifying key from a crypto/ecdh key
// (non-deprecated SEC 1 raw form; see ecdsaPrivFromECDH).
func ecdsaPubFromECDH(pub *ecdh.PublicKey) (*ecdsa.PublicKey, error) {
	b := pub.Bytes()
	if len(b) != 65 || b[0] != 0x04 {
		return nil, fmt.Errorf("%w: ecdh public point is malformed", ErrUnsupportedAlgorithm)
	}
	return ecdsa.ParseUncompressedPublicKey(ecdhToCurve(), b)
}
