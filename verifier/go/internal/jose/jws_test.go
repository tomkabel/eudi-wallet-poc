package jose

import (
	"encoding/base64"
	"strings"
	"testing"
)

// base64urlEncode is RawURLEncoding under the test's own name.
func base64urlEncode(b []byte) string { return base64.RawURLEncoding.EncodeToString(b) }

// signKey is a JWK with a private half; pubKey drops it.
func signKey(t *testing.T) JWK {
	t.Helper()
	k, err := GenerateKey()
	if err != nil {
		t.Fatalf("GenerateKey: %v", err)
	}
	return k
}

func TestJWSSignVerifyRoundTrip(t *testing.T) {
	k := signKey(t)
	k.Kid = ThumbprintB64(&k)
	payload := `{"iss":"verifier.example.ee","aud":"wallet","dcql_query":{"credentials":[{"id":"c","format":"mso_mdoc"}]}}`
	tok, err := SignJWS(k, jwsTypObject, payload)
	if err != nil {
		t.Fatalf("SignJWS: %v", err)
	}
	if strings.Count(tok, ".") != 2 {
		t.Fatalf("token is not compact: %q", tok)
	}
	got, err := VerifyJWSWithKey(tok, k.Public())
	if err != nil {
		t.Fatalf("VerifyJWSWithKey: %v", err)
	}
	if string(got) != payload {
		t.Fatalf("payload round-trip mismatch:\n got %s\nwant %s", got, payload)
	}
}

func TestJWSTamperedPayloadFails(t *testing.T) {
	k := signKey(t)
	tok, err := SignJWS(k, jwsTypObject, `{"a":1}`)
	if err != nil {
		t.Fatalf("SignJWS: %v", err)
	}
	parts := strings.Split(tok, ".")
	// Flip payload to a different valid base64url body.
	mutated := parts[0] + "." + parts[1][:len(parts[1])-2] + "AA" + "." + parts[2]
	if _, err := VerifyJWSWithKey(mutated, k.Public()); err == nil {
		t.Fatal("a tampered payload verified")
	}
}

func TestJWSWrongAlgRefused(t *testing.T) {
	k := signKey(t)
	tok, err := SignJWS(k, jwsTypObject, `{"a":1}`)
	if err != nil {
		t.Fatalf("SignJWS: %v", err)
	}
	parts := strings.Split(tok, ".")
	forged := `{"alg":"ES384","typ":"JWT"}`
	header := base64urlEncode([]byte(forged))
	if _, err := VerifyJWSWithKey(header+"."+parts[1]+"."+parts[2], k.Public()); err == nil {
		t.Fatal("an ES384 header was accepted")
	}
}

func TestJWSUntrustedKeyFails(t *testing.T) {
	k := signKey(t)
	other := signKey(t)
	tok, err := SignJWS(k, jwsTypObject, `{"a":1}`)
	if err != nil {
		t.Fatalf("SignJWS: %v", err)
	}
	if _, err := VerifyJWSWithKey(tok, other.Public()); err == nil {
		t.Fatal("a signature from another key verified")
	} else if !strings.Contains(err.Error(), "signature") {
		t.Fatalf("unexpected error shape: %v", err)
	}
}

func TestJWSThumbprintBindingMatchesJWEConvention(t *testing.T) {
	k := signKey(t)
	k.Kid = ThumbprintB64(&k)
	tok, err := SignJWS(k, jwsTypObject, `{"a":1}`)
	if err != nil {
		t.Fatalf("SignJWS: %v", err)
	}
	if !strings.HasPrefix(tok, base64urlEncode([]byte(`{"alg":"ES256","typ":"JWT","kid":"`+k.Kid+`"}`))) {
		t.Fatalf("header is not the pinned form: %q", tok[:60])
	}
}

func TestJWSRefusesWrongTypAndMissingPrivateKey(t *testing.T) {
	k := signKey(t)
	if _, err := SignJWS(k, "weird-typ", `{}`); err == nil {
		t.Fatal("a non-profile typ was signed")
	}
	if _, err := SignJWS(k.Public(), jwsTypObject, `{}`); err == nil {
		t.Fatal("a public-only key was accepted for signing")
	}
}
