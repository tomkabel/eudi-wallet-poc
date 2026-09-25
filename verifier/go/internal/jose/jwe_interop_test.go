package jose

import (
	"bytes"
	"encoding/hex"
	"testing"
)

// The cross-library pin: this token was produced OUTSIDE this package, with
// the fixed ephemeral key of RFC 7520 §5.5 Figure 122 and a recorded CEK/IV,
// following RFC 7518 §4.6.2 (Concat KDF with the SuppPubInfo key length) and
// RFC 7516 §5.1/§5.2 — the construction jwcrypto (the Python wallet's
// library) and every conformant JOSE stack produce. If the KDF or the header
// handling drifts, this fails.
func TestJWEDecryptsForeignLibToken(t *testing.T) {
	var recipient JWK
	jwkJSON := `{"kty":"EC","crv":"P-256",` +
		`"x":"Ze2loSV3wrroKUN_4zhwGhCqo3Xhu1td4QjeQ5wIVR0",` +
		`"y":"HlLtdXARY_f55A3fnzQbPcm6hgr34Mp8p-nuzQCE0Zw",` +
		`"d":"r_kHyZ-a06rmxM3yESK84r1otSg-aQcVStkRhA-iCM8"}`
	if err := recipient.UnmarshalJSON([]byte(jwkJSON)); err != nil {
		t.Fatalf("unmarshal JWK: %v", err)
	}
	const token = "eyJhbGciOiJFQ0RILUVTK0EyNTZLVyIsImVuYyI6IkEyNTZHQ00iLCJlcGsiOnsiY3J2IjoiUC0yNTYiLCJrdHkiOiJFQyIsIngiOiJtUFVLVF9iQVdHSEloZzBUcGpqcVZzUDFyWFdRdV92d1ZPSEh0TmtkWW9BIiwieSI6IjhCUUFzSW1HZUFTNDZmeVd3NU1oWWZHVFQwSWpCcEZ3MlNTMzREdjRJcnMifX0.j13pmTXWdgVEXM4I2fj4b9lC4rEK-_9OSlSBd1fOvw12VsueRj2GtQ.AAECAwQFBgcICQoL.DnY0m1yW4nqtJfbl1owKAvalp1aFCDYSXRSWqT0vct1lf4LcyK579hOEEJj8p1FXmytA6TW50fQ.ZcM6iGRh7Y0J_TDvihpA-g"
	got, err := Decrypt(&recipient, token)
	if err != nil {
		t.Fatalf("Decrypt: %v", err)
	}
	want, _ := hex.DecodeString("4974e280997320612064616e6765726f757320627573696e6573732c2046726f646f2c20676f696e67206f757420796f757220646f6f722e")
	if !bytes.Equal(got, want) {
		t.Fatalf("plaintext = %q, want the recorded foreign-library plaintext", got)
	}
}
