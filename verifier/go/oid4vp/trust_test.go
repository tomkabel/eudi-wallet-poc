package oid4vp

// Step 6 trust-store acceptance, exercised through the real TrustStore: the
// step 6 fixture's mock issuer key must be accepted for BOTH doctypes — the
// national ee.riik.poa.1 and the EU AV Profile eu.europa.ec.av.1 — because
// EE-POA-003 issues both from the same authoritative source in one issuance
// transaction (plan step 6). A key outside the store is refused for either,
// which is the rule the store exists to enforce.

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
)

// step6TrustStorePath is the fixture trust store; the file's provenance is in
// the sibling GENERATED-BY.
const step6TrustStorePath = "../zk/testdata/step6-device-pubkey/issuers.json"

// Any well-formed 32-byte coordinate pair works as "not in the store"; these
// are the step 0 fixture's issuer key, which this store must not carry.
const (
	foreignPKx = "0xa2a62ede3223470130fce5133e45cbb2bdf9bcd52439cdc7b70cf3758597a019"
	foreignPKy = "0x2a2ef0cfeb2e2ff2d4fd3698684c2cb70a9684bc98280f8f85fecc59716c2e88"
)

func TestStep6TrustStoreSelectsFixtureIssuerForBothDoctypes(t *testing.T) {
	ts, err := LoadTrustStore(step6TrustStorePath)
	if err != nil {
		t.Fatalf("load fixture trust store: %v", err)
	}
	meta := readStep6FixtureIssuer(t)
	for _, dt := range []string{"ee.riik.poa.1", "eu.europa.ec.av.1"} {
		is, err := ts.Select(dt, meta.PKx, meta.PKy)
		if err != nil {
			t.Errorf("fixture issuer key not accepted for %q: %v", dt, err)
			continue
		}
		if is.Namespace != dt {
			t.Errorf("doctype %q entry carries namespace %q", dt, is.Namespace)
		}
		if is.DocType != dt {
			t.Errorf("doctype %q entry carries doc_type %q", dt, is.DocType)
		}
	}
}

func TestStep6TrustStoreRefusesForeignIssuerKey(t *testing.T) {
	ts, err := LoadTrustStore(step6TrustStorePath)
	if err != nil {
		t.Fatalf("load fixture trust store: %v", err)
	}
	for _, dt := range []string{"ee.riik.poa.1", "eu.europa.ec.av.1"} {
		if _, err := ts.Select(dt, foreignPKx, foreignPKy); err == nil {
			t.Errorf("a foreign key was accepted for doctype %q", dt)
		}
	}
}

// readStep6FixtureIssuer returns the fixture's minting issuer key from the
// request.json ee_poa_demo wrote alongside the trust store.
func readStep6FixtureIssuer(t *testing.T) (meta struct {
	PKx string `json:"pkx"`
	PKy string `json:"pky"`
}) {
	t.Helper()
	raw, err := os.ReadFile("../zk/testdata/step6-device-pubkey/request.json")
	if err != nil {
		t.Fatalf("read fixture request.json: %v", err)
	}
	if err := json.Unmarshal(raw, &meta); err != nil {
		t.Fatalf("parse fixture request.json: %v", err)
	}
	return meta
}

// upper/lowerPairs mints a 64-hex-char coordinate pair in both cases, so the
// case-insensitivity test can use keys that pass the P-256 shape check.
func upperLowerPairs(t *testing.T, seed byte) (upper, lower [2]string) {
	t.Helper()
	x := make([]byte, 32)
	y := make([]byte, 32)
	x[0], y[0] = seed, seed+0x40
	ux := "0x" + strings.ToUpper(hex.EncodeToString(x))
	lx := "0x" + hex.EncodeToString(x)
	uy := "0x" + strings.ToUpper(hex.EncodeToString(y))
	ly := "0x" + hex.EncodeToString(y)
	return [2]string{ux, uy}, [2]string{lx, ly}
}

// Hex case is not identity: an uppercase store entry must still match the
// lowercase coordinates the presentation path derives, and vice versa.
func TestTrustStoreSelectIgnoresHexCase(t *testing.T) {
	path := t.TempDir() + "/issuers.json"
	body := `{"issuers":[{"name":"up","doc_type":"d","pkx":"0x` + strings.ToUpper("a2a62ede3223470130fce5133e45cbb2bdf9bcd52439cdc7b70cf3758597a019") + `","pky":"0x` + strings.ToUpper("2a2ef0cfeb2e2ff2d4fd3698684c2cb70a9684bc98280f8f85fecc59716c2e88") + `"}]}`
	if err := os.WriteFile(path, []byte(body), 0o600); err != nil {
		t.Fatal(err)
	}
	ts, err := LoadTrustStore(path)
	if err != nil {
		t.Fatal(err)
	}
	lower := [2]string{"0xa2a62ede3223470130fce5133e45cbb2bdf9bcd52439cdc7b70cf3758597a019", "0x2a2ef0cfeb2e2ff2d4fd3698684c2cb70a9684bc98280f8f85fecc59716c2e88"}
	upper := [2]string{strings.ToUpper(lower[0]), strings.ToUpper(lower[1])}
	for _, k := range [][2]string{lower, upper} {
		if _, err := ts.Select("d", k[0], k[1]); err != nil {
			t.Fatalf("Select(%s, %s): %v", k[0], k[1], err)
		}
	}
}

// W6 multi-credential: one session may ask for two doctypes, so the store
// must hold two doctypes from two DIFFERENT issuers and select the right one
// per doctype — trust.go's byDocType map already keys it, this test pins it.
func TestTrustStoreSelectsPerDoctypeFromTwoIssuers(t *testing.T) {
	path := t.TempDir() + "/issuers.json"
	body := `{"issuers":[
		{"name":"poa-issuer","doc_type":"ee.riik.poa.1","namespace":"ee.riik.poa.1","pkx":"0xa2a62ede3223470130fce5133e45cbb2bdf9bcd52439cdc7b70cf3758597a019","pky":"0x2a2ef0cfeb2e2ff2d4fd3698684c2cb70a9684bc98280f8f85fecc59716c2e88"},
		{"name":"av-issuer","doc_type":"eu.europa.ec.av.1","namespace":"eu.europa.ec.av.1","pkx":"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1","pky":"0xccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc2"}
	]}`
	if err := os.WriteFile(path, []byte(body), 0o600); err != nil {
		t.Fatal(err)
	}
	ts, err := LoadTrustStore(path)
	if err != nil {
		t.Fatal(err)
	}
	// Each doctype answers with its own issuer and refuses the other's key.
	poaK := [2]string{"0xa2a62ede3223470130fce5133e45cbb2bdf9bcd52439cdc7b70cf3758597a019", "0x2a2ef0cfeb2e2ff2d4fd3698684c2cb70a9684bc98280f8f85fecc59716c2e88"}
	poa, err := ts.Select("ee.riik.poa.1", poaK[0], poaK[1])
	if err != nil || poa.Name != "poa-issuer" {
		t.Fatalf("Select(poa) = %+v, %v; want poa-issuer", poa, err)
	}
	avK := [2]string{"0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1", "0xccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc2"}
	av, err := ts.Select("eu.europa.ec.av.1", avK[0], avK[1])
	if err != nil || av.Name != "av-issuer" {
		t.Fatalf("Select(av) = %+v, %v; want av-issuer", av, err)
	}
	// Cross-quotes are refused: the poa issuer's key does not answer av
	// queries and vice versa — the per-doctype scoping a two-credential
	// session leans on.
	if _, err := ts.Select("eu.europa.ec.av.1", poaK[0], poaK[1]); err == nil {
		t.Fatal("the poa issuer's key was accepted for the av doctype")
	}
	if _, err := ts.Select("ee.riik.poa.1", avK[0], avK[1]); err == nil {
		t.Fatal("the av issuer's key was accepted for the poa doctype")
	}
	// Both doctypes are listed, and All() sees both issuers.
	if got := len(ts.All()); got != 2 {
		t.Fatalf("All() = %d issuers, want 2", got)
	}
}

// writeIssuers writes a trust store body to a temp file and returns its path.
func writeIssuers(t *testing.T, body string) string {
	t.Helper()
	path := t.TempDir() + "/issuers.json"
	if err := os.WriteFile(path, []byte(body), 0o600); err != nil {
		t.Fatal(err)
	}
	return path
}

func issuerJSON(name, docType, pkx, pky string) string {
	return `{"name":"` + name + `","doc_type":"` + docType + `","namespace":"` + docType + `","pkx":"` + pkx + `","pky":"` + pky + `"}`
}

// TestTrustStoreRejectsBadHex: malformed coordinates fail the load and the
// error names the offending issuer — a store that silently trusts nothing is
// worse than one that refuses to start.
func TestTrustStoreRejectsBadHex(t *testing.T) {
	cases := []struct {
		name, pkx, pky, want string
	}{
		{"wrong length", "0xabcd", "0x" + strings.Repeat("aa", 32), "64-hex-char"},
		{"not hex", "0x" + strings.Repeat("zz", 32), "0x" + strings.Repeat("aa", 32), "64-hex-char"},
		{"missing prefix", strings.Repeat("aa", 32), "0x" + strings.Repeat("aa", 32), "0x-prefixed"},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			_, err := LoadTrustStore(writeIssuers(t, `{"issuers":[`+issuerJSON("bad-issuer", "d", tc.pkx, tc.pky)+`]}`))
			if err == nil {
				t.Fatalf("LoadTrustStore accepted %s pkx %q", tc.name, tc.pkx)
			}
			if !strings.Contains(err.Error(), "bad-issuer") {
				t.Errorf("error %q does not name the issuer", err)
			}
		})
	}
}

// TestTrustStoreCapsIssuersPerDocType: a fifth key for one doctype is a load
// error — every key runs a ~2.5s ZK verify before Select rejects it, so an
// unbounded list stretches presentations toward the write timeout.
func TestTrustStoreCapsIssuersPerDocType(t *testing.T) {
	var entries []string
	for i := 0; i < maxIssuersPerDocType+1; i++ {
		x := "0x" + strings.Repeat(fmt.Sprintf("%02x", i+1), 32)
		y := "0x" + strings.Repeat(fmt.Sprintf("%02x", 0x80+i), 32)
		entries = append(entries, issuerJSON(fmt.Sprintf("iss-%d", i), "d", x, y))
	}
	_, err := LoadTrustStore(writeIssuers(t, `{"issuers":[`+strings.Join(entries, ",")+`]}`))
	if err == nil {
		t.Fatalf("LoadTrustStore accepted %d issuers for one doctype", maxIssuersPerDocType+1)
	}
	if !strings.Contains(err.Error(), "cap") {
		t.Errorf("error %q does not name the cap", err)
	}

	// At the cap it still loads.
	entries = entries[:maxIssuersPerDocType]
	if _, err := LoadTrustStore(writeIssuers(t, `{"issuers":[`+strings.Join(entries, ",")+`]}`)); err != nil {
		t.Fatalf("LoadTrustStore at the cap failed: %v", err)
	}
}

// --- B3: signed trust store battery -----------------------------------------
//
// The signed shape (LoadSignedTrustStore) must behave exactly like the
// development shape once verified — and refuse everything the signature,
// append-only rule, or freshness windows forbid.

// signedKey is the trust root for the battery: a fresh P-256 key whose public
// half plays the -trust-root role.
func signedKey(t *testing.T) *jose.JWK {
	t.Helper()
	k, err := jose.GenerateKey()
	if err != nil {
		t.Fatalf("jose.GenerateKey: %v", err)
	}
	return &k
}

// writeSignedTrustStore signs issuers with k and writes the on-disk document;
// it returns the path.
func writeSignedTrustStore(t *testing.T, k *jose.JWK, issuers []Issuer) string {
	t.Helper()
	payload, err := json.Marshal(issuers)
	if err != nil {
		t.Fatalf("marshal issuers: %v", err)
	}
	tok, err := jose.SignJWS(*k, jose.JWSTypTrustStore, string(payload))
	if err != nil {
		t.Fatalf("SignJWS: %v", err)
	}
	raw, err := json.Marshal(signedTrustDoc{Issuers: issuers, Sig: tok})
	if err != nil {
		t.Fatalf("marshal signed trust doc: %v", err)
	}
	path := t.TempDir() + "/issuers.signed.json"
	if err := os.WriteFile(path, raw, 0o600); err != nil {
		t.Fatal(err)
	}
	return path
}

// batteryIssuers is a minimal valid store: one issuer, well-formed keys.
func batteryIssuers() []Issuer {
	return []Issuer{{Name: "battery-issuer", DocType: "d", Namespace: "d",
		PKx: "0x" + strings.Repeat("aa", 32), PKy: "0x" + strings.Repeat("bb", 32)}}
}

// TestSignedTrustStoreGoodSignatureLoads: the verified store behaves exactly
// like the unsigned one downstream.
func TestSignedTrustStoreGoodSignatureLoads(t *testing.T) {
	k := signedKey(t)
	issuers := batteryIssuers()
	ts, err := LoadSignedTrustStore(writeSignedTrustStore(t, k, issuers), k, nil, time.Now, false)
	if err != nil {
		t.Fatalf("LoadSignedTrustStore: %v", err)
	}
	if _, err := ts.Select("d", issuers[0].PKx, issuers[0].PKy); err != nil {
		t.Errorf("verified store does not Select its own issuer: %v", err)
	}
}

// TestSignedTrustStoreTamperedArrayFails: splicing the unsigned array after
// signing (here: a swapped coordinate) must not ride the signature.
func TestSignedTrustStoreTamperedArrayFails(t *testing.T) {
	k := signedKey(t)
	issuers := batteryIssuers()
	path := writeSignedTrustStore(t, k, issuers)
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	tampered := strings.Replace(string(raw), "aaaa", "aaab", 1)
	if tampered == string(raw) {
		t.Fatal("tamper no-op — fixture coordinate not found")
	}
	if err := os.WriteFile(path, []byte(tampered), 0o600); err != nil {
		t.Fatal(err)
	}
	if _, err := LoadSignedTrustStore(path, k, nil, time.Now, false); err == nil {
		t.Fatal("a spliced issuer array loaded")
	}
}

// TestSignedTrustStoreForeignSignatureFails: a store signed by a key that is
// not the trust root is refused — the trust anchor decides, not the file.
func TestSignedTrustStoreForeignSignatureFails(t *testing.T) {
	k := signedKey(t)
	other := signedKey(t)
	path := writeSignedTrustStore(t, other, batteryIssuers())
	if _, err := LoadSignedTrustStore(path, k, nil, time.Now, false); err == nil {
		t.Fatal("a store signed by a foreign key loaded")
	}
}

// TestSignedTrustStoreRemovedEntryFails: the append-only rule — an issuer in
// prior that vanishes from the new array is a load error; retire with
// not_after, never by deletion.
func TestSignedTrustStoreRemovedEntryFails(t *testing.T) {
	k := signedKey(t)
	full := batteryIssuers()
	full = append(full, Issuer{Name: "battery-issuer-2", DocType: "d2", Namespace: "d2",
		PKx: "0x" + strings.Repeat("cc", 32), PKy: "0x" + strings.Repeat("dd", 32)})
	prior, err := LoadSignedTrustStore(writeSignedTrustStore(t, k, full), k, nil, time.Now, false)
	if err != nil {
		t.Fatalf("prior load: %v", err)
	}
	path := writeSignedTrustStore(t, k, full[:len(full)-1])
	_, err = LoadSignedTrustStore(path, k, prior, time.Now, false)
	if err == nil {
		t.Fatal("a store that removed a prior issuer loaded")
	}
	if !strings.Contains(err.Error(), "battery-issuer") {
		t.Errorf("error %q does not name the removed issuer", err)
	}
	// The same store without a prior is fine: append-only is relative.
	if _, err := LoadSignedTrustStore(path, k, nil, time.Now, false); err != nil {
		t.Errorf("fresh (prior=nil) load of the shorter store failed: %v", err)
	}
}

// TestSignedTrustStoreStaleNotAfter: an entry outside its window loads (so
// retiring an issuer with not_after cannot stop the verifier) but is not
// selectable — and the window is checked per lookup, so an issuer that
// expires while the verifier runs stops being trusted.
func TestSignedTrustStoreStaleNotAfter(t *testing.T) {
	k := signedKey(t)
	now := time.Now()
	clock := func() time.Time { return now }
	stale := batteryIssuers()
	stale[0].NotAfter = now.Add(-time.Hour).Format(time.RFC3339)
	path := writeSignedTrustStore(t, k, stale)
	ts, err := LoadSignedTrustStore(path, k, nil, clock, false)
	if err != nil {
		t.Fatalf("a retired (not_after past) entry must not fail the load: %v", err)
	}
	if _, err := ts.Select("d", stale[0].PKx, stale[0].PKy); err == nil {
		t.Fatal("a stale issuer was selectable with freshness enforced")
	}
	ts, err = LoadSignedTrustStore(path, k, nil, clock, true)
	if err != nil {
		t.Fatalf("ignore-trust-freshness load: %v", err)
	}
	if _, err := ts.Select("d", stale[0].PKx, stale[0].PKy); err != nil {
		t.Errorf("ignore-trust-freshness: issuer not selectable: %v", err)
	}
	// A not-yet-valid entry is the mirror case.
	future := batteryIssuers()
	future[0].NotBefore = now.Add(time.Hour).Format(time.RFC3339)
	ts, err = LoadSignedTrustStore(writeSignedTrustStore(t, k, future), k, nil, clock, false)
	if err != nil {
		t.Fatalf("not-yet-valid load: %v", err)
	}
	if _, err := ts.Select("d", future[0].PKx, future[0].PKy); err == nil {
		t.Fatal("a not-yet-valid issuer was selectable")
	}
	// Expiry while running: valid now, gone once the clock passes not_after.
	live := batteryIssuers()
	live[0].NotAfter = now.Add(time.Hour).Format(time.RFC3339)
	ts, err = LoadSignedTrustStore(writeSignedTrustStore(t, k, live), k, nil, clock, false)
	if err != nil {
		t.Fatalf("live load: %v", err)
	}
	if _, err := ts.Select("d", live[0].PKx, live[0].PKy); err != nil {
		t.Fatalf("a currently valid issuer was not selectable: %v", err)
	}
	now = now.Add(2 * time.Hour)
	if _, err := ts.Select("d", live[0].PKx, live[0].PKy); err == nil {
		t.Fatal("an issuer that expired after load is still trusted")
	}
	// An unparsable bound is still a load error.
	bad := batteryIssuers()
	bad[0].NotAfter = "tomorrow"
	if _, err := LoadSignedTrustStore(writeSignedTrustStore(t, k, bad), k, nil, clock, false); err == nil {
		t.Fatal("an unparsable not_after loaded")
	}
}

// TestSignedTrustStoreAppendOnlyCaseInsensitive: prior keys are stored
// lowercased; a new store written in uppercase hex still keeps them.
func TestSignedTrustStoreAppendOnlyCaseInsensitive(t *testing.T) {
	k := signedKey(t)
	upper := batteryIssuers()
	upper[0].PKx = "0x" + strings.ToUpper(strings.TrimPrefix(upper[0].PKx, "0x"))
	upper[0].PKy = "0x" + strings.ToUpper(strings.TrimPrefix(upper[0].PKy, "0x"))
	path := writeSignedTrustStore(t, k, upper)
	prior, err := LoadSignedTrustStore(path, k, nil, time.Now, false)
	if err != nil {
		t.Fatalf("prior load: %v", err)
	}
	if _, err := LoadSignedTrustStore(path, k, prior, time.Now, false); err != nil {
		t.Fatalf("reloading the same uppercase store reported a removal: %v", err)
	}
}

// TestSignedTrustStoreRefusesBareFile: a file without a sig must not be
// snuck through the signed loader — the unsigned loader exists for that.
func TestSignedTrustStoreRefusesBareFile(t *testing.T) {
	if _, err := LoadSignedTrustStore(writeIssuers(t, `{"issuers":[`+issuerJSON("n", "d", "0x"+strings.Repeat("aa", 32), "0x"+strings.Repeat("bb", 32))+`]}`),
		signedKey(t), nil, time.Now, false); err == nil {
		t.Fatal("an unsigned file loaded through the signed path")
	}
}
