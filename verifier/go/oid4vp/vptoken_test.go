package oid4vp

import (
	"encoding/base64"
	"encoding/json"
	"strings"
	"testing"
)

const (
	testDocType   = "ee.riik.poa.1"
	testNamespace = "ee.riik.poa.1"
	testElement   = "age_over_18"
)

// validPresentation is what a well-behaved wallet answers testVPQuery with.
func validPresentation() ZKPresentation {
	return ZKPresentation{
		ZKSystem:      "longfellow-libzk-v1",
		Version:       7,
		NumAttributes: 1,
		DocType:       testDocType,
		Namespace:     testNamespace,
		AttrID:        testElement,
		AttrCBORHex:   "f5",
		ProofB64:      base64.StdEncoding.EncodeToString([]byte("not a real proof")),
	}
}

func tokenFor(t *testing.T, id string, p ZKPresentation) VPToken {
	t.Helper()
	raw, err := json.Marshal(p)
	if err != nil {
		t.Fatalf("marshal presentation: %v", err)
	}
	return VPToken{id: []string{base64.RawURLEncoding.EncodeToString(raw)}}
}

// EE-ZKP-021(b): the proof must establish the requested attribute value is
// `true`. A presentation carrying CBOR false (0xf4) is a proof that the holder
// is *not* over 18, and must not answer an over-18 query.
func TestVPTokenParseBindsTheQueriedValue(t *testing.T) {
	for _, tc := range []struct {
		name    string
		mutate  func(*ZKPresentation)
		wantErr string
	}{
		{"cbor true answers a true query", func(*ZKPresentation) {}, ""},
		{"cbor false does not", func(p *ZKPresentation) { p.AttrCBORHex = "f4" }, "f4"},
		{"attr_cbor_hex is not hex", func(p *ZKPresentation) { p.AttrCBORHex = "zz" }, "not hex"},
		{"an integer is not a boolean", func(p *ZKPresentation) { p.AttrCBORHex = "18f5" }, "18f5"},
		{"an empty value is not a boolean", func(p *ZKPresentation) { p.AttrCBORHex = "" }, "only accepts"},
		{"wrong doctype", func(p *ZKPresentation) { p.DocType = "org.iso.18013.5.1.mDL" }, "doctype"},
		{"wrong namespace", func(p *ZKPresentation) { p.Namespace = "org.iso.18013.5.1" }, "namespace"},
		{"wrong attr_id", func(p *ZKPresentation) { p.AttrID = "age_over_21" }, "discloses"},
		{"unsupported zk_system", func(p *ZKPresentation) { p.ZKSystem = "groth16" }, "zk_system"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			q, err := AgeQuery("proof_of_age", testDocType, testNamespace, testElement).Single()
			if err != nil {
				t.Fatalf("Single(): %v", err)
			}
			p := validPresentation()
			tc.mutate(&p)

			got, proof, err := tokenFor(t, q.ID, p).Parse(q)
			if tc.wantErr == "" {
				if err != nil {
					t.Fatalf("Parse() = %v, want accepted", err)
				}
				if got.AttrCBORHex != "f5" || len(proof) == 0 {
					t.Fatalf("Parse() returned %+v / %d proof bytes", got, len(proof))
				}
				return
			}
			if err == nil {
				t.Fatalf("Parse() accepted %+v, want rejection mentioning %q", p, tc.wantErr)
			}
			if !strings.Contains(err.Error(), tc.wantErr) {
				t.Fatalf("Parse() error = %q, want it to mention %q", err, tc.wantErr)
			}
		})
	}
}

// A query with no value constraint accepts both true and false, which is the
// bug this binding exists to prevent — so absence is an error, not a default.
func TestDCQLSingleRequiresBooleanValues(t *testing.T) {
	q := AgeQuery("proof_of_age", testDocType, testNamespace, testElement)
	if got := q.Credentials[0].Claims[0].Values; len(got) != 1 || got[0] != true {
		t.Fatalf("AgeQuery values = %v, want [true]", got)
	}
	c, err := q.Single()
	if err != nil {
		t.Fatalf("Single(): %v", err)
	}
	if len(c.Values()) != 1 || c.Values()[0] != true {
		t.Fatalf("Values() = %v, want [true]", c.Values())
	}

	for _, tc := range []struct {
		name   string
		values []any
	}{
		{"no values at all", nil},
		{"empty values", []any{}},
		{"a non-boolean value", []any{"true"}},
		{"a boolean and a non-boolean", []any{true, 18}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			q := AgeQuery("proof_of_age", testDocType, testNamespace, testElement)
			q.Credentials[0].Claims[0].Values = tc.values
			if _, err := q.Single(); err == nil {
				t.Fatalf("Single() accepted values %v", tc.values)
			}
		})
	}
}
