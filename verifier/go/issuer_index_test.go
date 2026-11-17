package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/oid4vp"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/zk"
)

// NEW-S2-3 judge: with 50 trusted issuers for one doctype, a plain-path
// presentation naming its issuer by issuer_kid costs exactly one ZK
// verification, against that issuer's key; without the hint the verifier
// refuses before any FFI work instead of looping over 50 keys.
func TestPlainPathFiftyIssuersOneVerify(t *testing.T) {
	var entries []string
	for i := 0; i < 50; i++ {
		entries = append(entries, fmt.Sprintf(
			`{"name":"iss-%d","doc_type":"eu.europa.ec.av.1","namespace":"eu.europa.ec.av.1","pkx":"0x%s","pky":"0x%s"}`,
			i, strings.Repeat(fmt.Sprintf("%02x", i+1), 32), strings.Repeat(fmt.Sprintf("%02x", 0x80+i), 32)))
	}
	trust, err := oid4vp.LoadTrustStore(writeTemp(t, []byte(`{"issuers":[`+strings.Join(entries, ",")+`]}`)))
	if err != nil {
		t.Fatalf("trust store with 50 issuers: %v", err)
	}
	want, err := trust.SelectByKeyID("eu.europa.ec.av.1", mustKID(t, trust, "iss-37"))
	if err != nil {
		t.Fatal(err)
	}

	var calls []zk.Request
	orig := zkVerify
	zkVerify = func(r zk.Request) error { calls = append(calls, r); return nil }
	t.Cleanup(func() { zkVerify = orig })

	envelope := func(kid string) []byte {
		var env map[string]any
		if err := json.Unmarshal(plainEnvelope(t), &env); err != nil {
			t.Fatal(err)
		}
		if kid != "" {
			env["issuer_kid"] = kid
		}
		raw, _ := json.Marshal(env)
		return raw
	}

	// With the hint: one verification, against the named issuer's key.
	p, s := plainCarrierHarness(t, nil)
	p.trust = trust
	rec := postPlain(t, p, s, envelope(want.KeyID()))
	if rec.Code != http.StatusOK || !strings.Contains(rec.Body.String(), `"valid":true`) {
		t.Fatalf("hinted presentation: %d %s", rec.Code, rec.Body.String())
	}
	if len(calls) != 1 || calls[0].PKx != want.PKx || calls[0].PKy != want.PKy {
		t.Fatalf("zk verifications = %d (want 1, against %s): %+v", len(calls), want.Name, calls)
	}

	// A hint naming no trusted key is refused (403) with no FFI work.
	calls = nil
	p, s = plainCarrierHarness(t, nil)
	p.trust = trust
	if rec := postPlain(t, p, s, envelope(strings.Repeat("ab", 32))); rec.Code != http.StatusForbidden || len(calls) != 0 {
		t.Fatalf("unknown issuer_kid: %d %s, %d verifications", rec.Code, rec.Body.String(), len(calls))
	}

	// No hint over the scan bound: a 4xx naming issuer_kid, no FFI work.
	p, s = plainCarrierHarness(t, nil)
	p.trust = trust
	rec = postPlain(t, p, s, envelope(""))
	if rec.Code != http.StatusBadRequest || !strings.Contains(rec.Body.String(), "issuer_kid") || len(calls) != 0 {
		t.Fatalf("unhinted presentation over the bound: %d %s, %d verifications", rec.Code, rec.Body.String(), len(calls))
	}
}

func mustKID(t *testing.T, ts *oid4vp.TrustStore, name string) string {
	t.Helper()
	for _, is := range ts.All() {
		if is.Name == name {
			return is.KeyID()
		}
	}
	t.Fatalf("no issuer %q", name)
	return ""
}
