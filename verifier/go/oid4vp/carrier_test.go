package oid4vp

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/cborsub"
)

// The carrier table: every carrier × the four outcomes the profile v1.0 §7
// names (valid / tampered / wrong-query / wrong zk_system). Both carriers must
// answer the SAME multipaz proof identically — that is the property that makes
// a carrier swap safe forever.
//
// The golden proof is the step 8.7 fixture's (a real multipaz-generated
// Longfellow proof bound to the B.2.6.1 OpenID4VPHandover transcript); the
// interim carrier wraps it in this repository's JSON envelope, the CBOR
// carrier in a DeviceResponse.zkDocuments — the builder for that wrap is
// tools/build_cbor_fixture.py and the committed vector under
// tests/vectors/carrier-v1/ is asserted byte-for-byte below.

// carrierFixtureQuery is the query every case runs against: the step 8.7
// fixture's own doctype/namespace/element and the registry's v7/1 circuit.
func carrierFixtureQuery(t *testing.T) CredentialQuery {
	t.Helper()
	q, err := ZkAgeQuery("age_credential", "eu.europa.ec.av.1", "eu.europa.ec.av.1",
		"age_over_18", carrierFixtureCircuit).Single()
	if err != nil {
		t.Fatalf("query: %v", err)
	}
	return q
}

// --- the interim-JSON wrap of the golden proof ---

func interimWrap(t *testing.T, proof []byte) []byte {
	t.Helper()
	p := ZKPresentation{
		ZKSystem:      SystemMultipaz,
		Version:       7,
		NumAttributes: 1,
		DocType:       "eu.europa.ec.av.1",
		Namespace:     "eu.europa.ec.av.1",
		AttrID:        "age_over_18",
		AttrCBORHex:   "f5",
		ProofB64:      base64.StdEncoding.EncodeToString(proof),
	}
	raw, err := json.Marshal(p)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	return raw
}

// --- the CBOR wrap, built the way tools/build_cbor_fixture.py builds it ---

// goldenEntryBytes extracts the source fixture's verbatim zkDocument entry
// bytes — multipaz 0.99.0's own serialization — the same way the Python
// builder does (tools/build_cbor_fixture.py zk_document_entry_bytes). The
// wrap is then a fresh DeviceResponse head around those bytes. Re-serializing
// the decoded map would risk exactly the drift this vector pins (cbor2
// re-tags the timestamp when it round-trips a datetime), so both builders
// reuse the entry verbatim and can never disagree.
func goldenEntryBytes(t *testing.T, source []byte) []byte {
	t.Helper()
	const marker = "zkDocuments"
	i := bytes.Index(source, []byte(marker))
	if i < 0 {
		t.Fatal("source DeviceResponse has no zkDocuments key")
	}
	p := i + len(marker)
	if source[p] != 0x81 {
		t.Fatalf("zkDocuments is not a one-element array (head 0x%02x)", source[p])
	}
	entry := source[p+1:]
	// The entry is the RAW zkDocument map ({proof, documentData}); sanity-check
	// it by decoding it standalone — a zkDocument map, not a DeviceResponse.
	docs, err := parseSingleZkDocumentEntry(entry)
	if err != nil {
		t.Fatalf("the extracted entry is not one zkDocument: %v", err)
	}
	_ = docs
	return entry
}

// buildCBORDeviceResponse wraps the golden entry in the de-facto carrier's
// DeviceResponse: canonical CBOR, keys in canonical (shortest-first) order
// version < status < zkDocuments — byte-identical to the Python builder's
// WRAPPER_HEAD + entry.
func buildCBORDeviceResponse(entry []byte) []byte {
	out := []byte{0xa3}
	out = append(out, cborText("version")...)
	out = append(out, cborText("1.1")...)
	out = append(out, cborText("status")...)
	out = append(out, 0x00)
	out = append(out, cborText("zkDocuments")...)
	out = append(out, 0x81)
	return append(out, entry...)
}

// goldenDR returns the CBOR carrier's bytes: the verbatim golden entry in the
// de-facto DeviceResponse wrap.
func goldenDR(t *testing.T) []byte {
	t.Helper()
	return buildCBORDeviceResponse(goldenEntry(t))
}

// goldenEntry extracts the source fixture's verbatim zkDocument entry once per
// test.
func goldenEntry(t *testing.T) []byte {
	t.Helper()
	src, err := os.ReadFile(filepath.Join("..", "zk", "testdata", "step8-7-openid4vp-zk", "device_response.cbor"))
	if err != nil {
		t.Fatalf("read the golden fixture: %v", err)
	}
	return goldenEntryBytes(t, src)
}

// --- the table ---

func TestCarrierTableBothCarriers(t *testing.T) {
	q := carrierFixtureQuery(t)
	proof := goldenCarrierProof(t)
	interim := InterimJSON{}.Parse
	cborParse := MsoMdocZkCBOR{}.Parse

	goodCBOR := goldenDR(t)
	tampered := append([]byte(nil), proof...)
	tampered[len(tampered)/2] ^= 0x01
	// The tampered CBOR case flips the proof bit inside the verbatim entry.
	entry := goldenEntry(t)
	tamperedEntry := bytes.Replace(entry, proof, tampered, 1)
	if bytes.Equal(tamperedEntry, entry) {
		t.Fatal("the tampered entry is byte-identical to the golden entry")
	}
	tamperedCBOR := buildCBORDeviceResponse(tamperedEntry)

	for _, tc := range []struct {
		name    string
		raw     []byte
		parse   func([]byte, CredentialQuery) (*CheckedPresentation, error)
		wantErr string
	}{
		{"valid: interim", interimWrap(t, proof), interim, ""},
		{"valid: cbor", goodCBOR, cborParse, ""},
		{"tampered: interim flips a proof bit", interimWrap(t, tampered), interim, ""},
		{"tampered: cbor flips a proof bit", tamperedCBOR, cborParse, ""},
		{"wrong query: interim discloses another element", interimWrap(t, proof), func(r []byte, q2 CredentialQuery) (*CheckedPresentation, error) {
			wq, err := ZkAgeQuery("age_credential", "eu.europa.ec.av.1", "eu.europa.ec.av.1",
				"age_over_21", carrierFixtureCircuit).Single()
			if err != nil {
				t.Fatalf("query: %v", err)
			}
			return interim(r, wq)
		}, "discloses"},
		{"wrong query: cbor carries another doctype", goodCBOR, func(r []byte, _ CredentialQuery) (*CheckedPresentation, error) {
			wq, err := ZkAgeQuery("age_credential", "eu.europa.ec.pid.1", "eu.europa.ec.av.1",
				"age_over_18", carrierFixtureCircuit).Single()
			if err != nil {
				t.Fatalf("query: %v", err)
			}
			return cborParse(r, wq)
		}, "doctype"},
		{"wrong zk_system: interim", interimWrapWrongSystem(t, proof), interim, "zk_system"},
		{"wrong zk_system: cbor spec id under an unknown label", buildCBORDeviceResponse(
			tamperEntrySpecID(t, entry, "bogus-system_7_1_4151_4096_"+strings.Repeat("ab", 32))),
			cborParse, "does not name a supported zk system"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			cp, err := tc.parse(tc.raw, q)
			// The tampered cases are parse-clean by design: the tampered
			// proof is still a well-formed envelope/zkDocument, so the
			// carrier layer must hand it through untouched and the refusal
			// belongs to zk.Verify. Assert the hand-through (bytes intact)
			// and that the bytes differ from the golden proof.
			if tc.wantErr == "" {
				if err != nil {
					t.Fatalf("Parse(): %v", err)
				}
				if len(cp.Proof) == 0 {
					t.Fatal("Parse() returned no proof bytes")
				}
				if strings.Contains(tc.name, "tampered") && !bytes.Equal(cp.Proof, tamperProof(t, proof)) {
					t.Fatal("the tampered proof did not survive the carrier layer byte for byte")
				}
				return
			}
			if err == nil {
				t.Fatalf("Parse() accepted, want refusal naming %q", tc.wantErr)
			}
			if !strings.Contains(err.Error(), tc.wantErr) {
				t.Fatalf("Parse() error = %q, want it to mention %q", err, tc.wantErr)
			}
		})
	}
}

func interimWrapWrongSystem(t *testing.T, proof []byte) []byte {
	t.Helper()
	p := ZKPresentation{
		ZKSystem:      "groth16",
		Version:       7,
		NumAttributes: 1,
		DocType:       "eu.europa.ec.av.1",
		Namespace:     "eu.europa.ec.av.1",
		AttrID:        "age_over_18",
		AttrCBORHex:   "f5",
		ProofB64:      base64.StdEncoding.EncodeToString(proof),
	}
	raw, err := json.Marshal(p)
	if err != nil {
		t.Fatalf("marshal: %v", err)
	}
	return raw
}

func tamperProof(t *testing.T, proof []byte) []byte {
	t.Helper()
	out := append([]byte(nil), proof...)
	out[len(out)/2] ^= 0x01
	return out
}

// parseSingleZkDocumentEntry sanity-checks a verbatim zkDocument entry: it
// must decode as a map carrying proof + documentData (the same shape
// ParseZkDocument reads), via the parser's own zkDocuments arm.
func parseSingleZkDocumentEntry(entry []byte) ([]*ZkDocument, error) {
	root, err := cborsub.Decode(entry, cborsub.DefaultLimits())
	if err != nil {
		return nil, err
	}
	d, err := ParseZkDocument(root)
	if err != nil {
		return nil, err
	}
	return []*ZkDocument{d}, nil
}

// tamperEntrySpecID rewrites the zkSystemId text inside a verbatim zkDocument
// entry, keeping every other byte (including lengths) intact. Used for the
// unknown-system refusal case.
func tamperEntrySpecID(t *testing.T, entry []byte, specID string) []byte {
	t.Helper()
	old, err := parseSingleZkDocumentEntry(entry)
	if err != nil || len(old) != 1 {
		t.Fatalf("entry does not parse: %v", err)
	}
	// The bogus spec id must be exactly the real one's length, so the
	// replacement keeps every CBOR length intact (the entry stays verbatim
	// apart from the id text itself).
	if len(specID) > len(old[0].ZkSystemSpecID) {
		specID = specID[:len(old[0].ZkSystemSpecID)]
	}
	for len(specID) < len(old[0].ZkSystemSpecID) {
		specID += "0"
	}
	out := bytes.Replace(entry, []byte(old[0].ZkSystemSpecID), []byte(specID), 1)
	if bytes.Equal(out, entry) {
		t.Fatal("the spec id was not replaced")
	}
	if len(out) != len(entry) {
		t.Fatal("the replacement changed the entry length; the test fixture assumption broke")
	}
	return out
}

// Same-table check through VPToken.ParseCarrier: one vp_token, both carriers,
// dispatch per format and per sniff — the entry point the presenter uses.
func TestVPTokenParseCarrierDispatch(t *testing.T) {
	q := carrierFixtureQuery(t)
	proof := goldenCarrierProof(t)

	for _, tc := range []struct {
		name     string
		zkFormat bool // true: the query names mso_mdoc_zk (format dispatch); false: sniff
		raw      []byte
		wantName string
	}{
		{"interim envelope, sniffs", false, interimWrap(t, proof), "interim-json"},
		{"cbor device response, sniffs", false, goldenDR(t), "mso-mdoc-zk-cbor"},
		{"cbor device response, format-named", true,
			goldenDR(t),
			"mso-mdoc-zk-cbor"},
	} {
		t.Run(tc.name, func(t *testing.T) {
			tq := q
			if tc.zkFormat {
				tq.Format = FormatMsoMdocZk
			} else {
				tq.Format = FormatMsoMdoc
			}
			token := VPToken{q.ID: []string{base64.RawURLEncoding.EncodeToString(tc.raw)}}
			cp, carrier, err := token.ParseCarrier(tq, nil)
			if err != nil {
				t.Fatalf("ParseCarrier(): %v", err)
			}
			if carrier.Name() != tc.wantName {
				t.Fatalf("dispatched %q, want %q", carrier.Name(), tc.wantName)
			}
			if !bytes.Equal(cp.Proof, proof) {
				t.Fatalf("proof bytes differ from the golden proof (%d vs %d bytes)",
					len(cp.Proof), len(proof))
			}
			if cp.TranscriptFlow != FlowRedirectB261 {
				t.Fatalf("TranscriptFlow = %v, want FlowRedirectB261", cp.TranscriptFlow)
			}
		})
	}
}

// The explicit -carrier override: a test that asks for one carrier and gets
// the other must fail loudly, not fall back.
func TestCarrierOverrideIsLoud(t *testing.T) {
	q := carrierFixtureQuery(t)
	proof := goldenCarrierProof(t)
	interim := interimWrap(t, proof)
	cborRaw := goldenDR(t)

	// Asking for the CBOR carrier over interim bytes must fail.
	cborCarrier := MsoMdocZkCBOR{}
	if _, err := cborCarrier.Parse(interim, q); err == nil {
		t.Fatal("the CBOR carrier accepted interim-JSON bytes")
	}
	// Asking for the interim carrier over CBOR bytes must fail.
	jsonCarrier := InterimJSON{}
	if _, err := jsonCarrier.Parse(cborRaw, q); err == nil {
		t.Fatal("the interim carrier accepted CBOR DeviceResponse bytes")
	}
	// ParseCarrierName rejects unknown names instead of falling back.
	if _, err := ParseCarrierName("mystery-carrier"); err == nil {
		t.Fatal("ParseCarrierName accepted an unknown carrier")
	}
	for _, name := range []string{"", "interim-json", "mso-mdoc-zk-cbor"} {
		if _, err := ParseCarrierName(name); err != nil {
			t.Fatalf("ParseCarrierName(%q): %v", name, err)
		}
	}
}

// The dual-carrier golden assertion: the SAME multipaz proof wrapped in each
// carrier normalizes to the same CheckedPresentation, byte for byte on the
// proof. This is the test that makes the carrier swap safe forever (plan W1
// task 2). It runs on the committed vector when the FFI staticlib is absent;
// the real zk.Verify run over both wraps lives in the zk package's
// TestCarrierVectorsVerifyIdentically (skipped with the same guard as the
// other FFI tests).
func TestGoldenProofNormalizesIdentically(t *testing.T) {
	q := carrierFixtureQuery(t)
	proof := goldenCarrierProof(t)
	goodCBOR := goldenDR(t)

	icp, err := InterimJSON{}.Parse(interimWrap(t, proof), q)
	if err != nil {
		t.Fatalf("interim: %v", err)
	}
	ccp, err := MsoMdocZkCBOR{}.Parse(goodCBOR, q)
	if err != nil {
		t.Fatalf("cbor: %v", err)
	}
	if !bytes.Equal(icp.Proof, ccp.Proof) {
		t.Fatal("the two carriers did not normalize to the same proof bytes")
	}
	if icp.DocType != ccp.DocType || icp.AttrID != ccp.AttrID || icp.Namespace != ccp.Namespace {
		t.Fatalf("identity fields differ: %+v vs %+v", icp, ccp)
	}
	if !icp.DisclosedTrue || !ccp.DisclosedTrue {
		t.Fatal("both carriers must report the disclosed predicate as true")
	}
	if icp.TranscriptFlow != ccp.TranscriptFlow {
		t.Fatalf("TranscriptFlow differs: %v vs %v", icp.TranscriptFlow, ccp.TranscriptFlow)
	}
}

// --- committed vector wiring ---

// carrierVectors is tests/vectors/carrier-v1/: the profile v1.0 references
// this directory, and every file here is asserted against the builder output
// byte for byte, so the vector cannot drift from what the verifier parses.
func TestCarrierVectorsMatchTheBuilder(t *testing.T) {
	proof := goldenCarrierProof(t)
	want := map[string][]byte{
		"device_response.json.b64":       []byte(base64.RawURLEncoding.EncodeToString(interimWrap(t, proof))),
		"device_response.cbor.b64":       []byte(base64.RawURLEncoding.EncodeToString(goldenDR(t))),
		"device_response_transcript.bin": transcriptForGolden(t),
	}
	for name, built := range want {
		got, err := os.ReadFile(filepath.Join("..", "..", "..", "tests", "vectors", "carrier-v1", name))
		if err != nil {
			t.Fatalf("read %s: %v", name, err)
		}
		// The .b64 vectors end in a newline so the files diff cleanly; the
		// transcript is binary. Trim trailing whitespace before comparing.
		got = bytes.TrimRight(got, "\n")
		builtOut := bytes.TrimRight(built, "\n")
		if !bytes.Equal(got, builtOut) {
			t.Fatalf("%s drifted from the builder: %d bytes on disk, %d built; first diff at %d",
				name, len(got), len(builtOut), firstDiff(got, builtOut))
		}
	}
}

// firstDiff reports the first byte position where a and b differ, for the
// drift message.
func firstDiff(a, b []byte) int {
	n := len(a)
	if len(b) < n {
		n = len(b)
	}
	for i := 0; i < n; i++ {
		if a[i] != b[i] {
			return i
		}
	}
	return n
}

// --- flow registry coverage (transcript.go TranscriptForFlow) ---

// TestTranscriptForFlowCoversBothFlows pins the registry: both flows derive
// the same bytes as their original entry points, an unknown flow is refused,
// and the two derivations are byte-different for the same session facts —
// the structural form of "the two flows are NEVER interchangeable" (F2).
func TestTranscriptForFlowCoversBothFlows(t *testing.T) {
	p := TranscriptParams{
		ClientID: clientID, Nonce: nonce, ResponseURI: respURI,
		JWKThumbprint: nil,
	}
	for _, tc := range []struct {
		name   string
		flow   Flow
		direct func() ([]byte, error)
	}{
		{"redirect B.2.6.1", FlowRedirectB261, func() ([]byte, error) {
			return SessionTranscript(clientID, nonce, nil, respURI)
		}},
		{"dcapi ISO", FlowDcapiISO, func() ([]byte, error) {
			return ISOTranscript("enc-b64", "https://example.org")
		}},
	} {
		t.Run(tc.name, func(t *testing.T) {
			params := p
			if tc.flow == FlowDcapiISO {
				params = TranscriptParams{EncryptionInfoB64: "enc-b64", Origin: "https://example.org"}
			}
			via, err := TranscriptForFlow(tc.flow, params)
			if err != nil {
				t.Fatalf("TranscriptForFlow: %v", err)
			}
			direct, err := tc.direct()
			if err != nil {
				t.Fatalf("direct derivation: %v", err)
			}
			if !bytes.Equal(via, direct) {
				t.Fatalf("TranscriptForFlow(%v) differs from the direct derivation", tc.flow)
			}
		})
	}

	// Unknown flow refused, not defaulted.
	if _, err := TranscriptForFlow(Flow(99), p); err == nil {
		t.Fatal("an unknown flow produced a transcript instead of an error")
	}

	// The two flows are never interchangeable: for the same verifier session
	// facts they hash different handover bytes, in both directions.
	b261, err := TranscriptForFlow(FlowRedirectB261, p)
	if err != nil {
		t.Fatal(err)
	}
	iso, err := TranscriptForFlow(FlowDcapiISO, TranscriptParams{
		EncryptionInfoB64: "enc-b64", Origin: "https://example.org"})
	if err != nil {
		t.Fatal(err)
	}
	if bytes.Equal(b261, iso) {
		t.Fatal("the B.2.6.1 and ISO dcapi transcripts are the same bytes")
	}
	// A proof bound to one flow must not verify over the other's transcript.
	// The byte-level guarantee is pinned by TestStep87OpenID4VPZkVerify
	// (both directions, real Longfellow proof); here the registry guarantees
	// the CALLER cannot even request the wrong pairing silently.
	if _, err := TranscriptForFlow(FlowDcapiISO, TranscriptParams{}); err == nil {
		t.Fatal("the dcapi flow accepted empty params instead of refusing")
	}
	if _, err := TranscriptForFlow(FlowRedirectB261, TranscriptParams{}); err == nil {
		t.Fatal("the redirect flow accepted empty params instead of refusing")
	}
}

// --- fixture plumbing shared by this file and the zk-side dual-carrier test ---

const (
	goldenSpecID    = "longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121"
	goldenTimestamp = "2026-09-24T14:19:58Z"
)

// carrierFixtureCircuit is the registry's v7/1 entry the golden spec id names.
var carrierFixtureCircuit = circuits.Circuit{
	Hash:    "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121",
	Version: 7, NumAttributes: 1, BlockEncHash: 4151, BlockEncSig: 4096,
}

func goldenCarrierProof(t *testing.T) []byte {
	t.Helper()
	return readGoldenProof(t)
}

func readGoldenProof(t *testing.T) []byte {
	t.Helper()
	b, err := os.ReadFile(filepath.Join("zk", "testdata", "step8-7-openid4vp-zk", "proof.bin"))
	if err == nil {
		return b
	}
	// The oid4vp package sits next to zk/ in verifier/go; the zkDocuments
	// fixture is one directory up from the oid4vp package dir.
	b, err = os.ReadFile(filepath.Join("..", "zk", "testdata", "step8-7-openid4vp-zk", "device_response.cbor"))
	if err != nil {
		t.Fatalf("read the golden proof fixture: %v", err)
	}
	docs, err := ParseZkDocumentsBytes(b)
	if err != nil || len(docs) != 1 {
		t.Fatalf("parse the golden fixture: %v (%d docs)", err, len(docs))
	}
	return docs[0].Proof
}

func transcriptForGolden(t *testing.T) []byte {
	t.Helper()
	meta := goldenRequestMeta(t)
	tr, err := SessionTranscript(meta.ClientID, meta.Nonce, nil, meta.ResponseURI)
	if err != nil {
		t.Fatalf("transcript: %v", err)
	}
	return tr
}

// goldenRequest mirrors the fixture's request.json.
type goldenRequest struct {
	ClientID, Nonce, ResponseURI string
}

func goldenRequestMeta(t *testing.T) goldenRequest {
	t.Helper()
	b, err := os.ReadFile(filepath.Join("..", "zk", "testdata", "step8-7-openid4vp-zk", "request.json"))
	if err != nil {
		t.Fatalf("read request.json: %v", err)
	}
	var m struct {
		ClientID    string `json:"client_id"`
		Nonce       string `json:"nonce"`
		ResponseURI string `json:"response_uri"`
	}
	if err := json.Unmarshal(b, &m); err != nil {
		t.Fatalf("parse request.json: %v", err)
	}
	return goldenRequest{ClientID: m.ClientID, Nonce: m.Nonce, ResponseURI: m.ResponseURI}
}
