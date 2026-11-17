package oid4vp

// These are fixture/container and local allowlist assertions. The copied minted
// proof binds OpenID4VPHandover, and encrypted public examples are never opened.
// No FFI verification, scheme acceptance or AV transport conformance is claimed.
import (
	"bytes"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"os"
	"reflect"
	"regexp"
	"strings"
	"testing"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/cborsub"
)

const avProfileRevision = "014dc2fd0f9332eeabe1eef70d234ed2aef20fd7"
const avFixtureRoot = "../../../tests/vectors/av-profile/"
const avCircuitHash = "8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121"

func avRead(t *testing.T, path string) []byte {
	t.Helper()
	raw, err := os.ReadFile(path)
	if err != nil {
		t.Fatal(err)
	}
	return raw
}

func avJSON(t *testing.T, path string, target any) {
	t.Helper()
	if err := json.Unmarshal(avRead(t, avFixtureRoot+path), target); err != nil {
		t.Fatal(err)
	}
}

func avDecode(t *testing.T, raw []byte) *cborsub.Value {
	t.Helper()
	v, err := cborsub.Decode(raw, cborsub.DefaultLimits())
	if err != nil {
		t.Fatal(err)
	}
	return v
}

func avGet(t *testing.T, value *cborsub.Value, key string) *cborsub.Value {
	t.Helper()
	v, ok, err := value.MapGet(key)
	if err != nil || !ok {
		t.Fatalf("missing %s: %v", key, err)
	}
	return v
}

func avText(t *testing.T, value *cborsub.Value, want string) {
	t.Helper()
	if value.Kind != cborsub.KText || value.Text != want {
		t.Fatalf("want text %q, got %s %q", want, value.Kind, value.Text)
	}
}

func avItems(t *testing.T, path string) *cborsub.Value {
	t.Helper()
	req := avDecode(t, avRead(t, avFixtureRoot+path))
	avText(t, avGet(t, req, "version"), "1.0")
	docs := avGet(t, req, "docRequests")
	if docs.Kind != cborsub.KArray || len(docs.Array) != 1 {
		t.Fatal("request must carry one docRequest")
	}
	tag := avGet(t, &docs.Array[0], "itemsRequest")
	if tag.Kind != cborsub.KTag24 {
		t.Fatal("itemsRequest must be tag 24")
	}
	items := avDecode(t, tag.Bytes)
	avText(t, avGet(t, items, "docType"), "eu.europa.ec.av.1")
	predicate := avGet(t, avGet(t, avGet(t, items, "nameSpaces"), "eu.europa.ec.av.1"), "age_over_18")
	if predicate.Kind != cborsub.KBool || predicate.Bool {
		t.Fatal("request predicate must be age_over_18: false (intentToRetain)")
	}
	return items
}

func avCBORSpec(t *testing.T, spec *cborsub.Value) ZkSystemTypeSpec {
	t.Helper()
	avText(t, avGet(t, spec, "system"), "longfellow-libzk-v1")
	params := avGet(t, spec, "params")
	if params.Kind != cborsub.KMap || len(params.Map) != 5 {
		t.Fatal("ZkSystemSpec must carry exactly five named params")
	}
	number := func(key string) uint32 {
		t.Helper()
		v := avGet(t, params, key)
		if v.Kind != cborsub.KUint || v.Uint > 1<<32-1 {
			t.Fatalf("%s is not uint32", key)
		}
		return uint32(v.Uint)
	}
	hash := avGet(t, params, "circuit_hash")
	if hash.Kind != cborsub.KText || len(hash.Text) != 64 {
		t.Fatal("circuit_hash must be 64 hex characters")
	}
	if _, err := hex.DecodeString(hash.Text); err != nil {
		t.Fatal(err)
	}
	return ZkSystemTypeSpec{System: "longfellow-libzk-v1", ID: avGet(t, spec, "zkSystemId").Text,
		CircuitHash: hash.Text, Version: number("version"), NumAttributes: number("num_attributes"),
		BlockEncHash: number("block_enc_hash"), BlockEncSig: number("block_enc_sig")}
}

func avLocalSpec() ZkSystemTypeSpec {
	return ZkSystemTypeSpec{System: "longfellow-libzk-v1", ID: testEncryptionID, CircuitHash: avCircuitHash,
		Version: 7, NumAttributes: 1, BlockEncHash: 4151, BlockEncSig: 4096}
}

func TestAVProfileLocalContainers(t *testing.T) {
	accepted := mustSpecs(t)
	request := avRead(t, avFixtureRoot+"local/device-request.cbor")
	runtime, err := DeviceRequest("eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", accepted)
	if err != nil || !bytes.Equal(request, runtime) {
		t.Fatalf("independently constructed request differs from runtime DeviceRequest: %v", err)
	}
	items := avItems(t, "local/device-request.cbor")
	zk := avGet(t, avGet(t, items, "requestInfo"), "zkRequest")
	if v := avGet(t, zk, "version"); v.Kind != cborsub.KUint || v.Uint != 1 {
		t.Fatal("local zkRequest version must be 1")
	}
	if v := avGet(t, zk, "zkRequired"); v.Kind != cborsub.KBool || !v.Bool {
		t.Fatal("local request requires ZK")
	}
	specs := avGet(t, zk, "systemSpecs")
	if specs.Kind != cborsub.KArray || len(specs.Array) != 1 {
		t.Fatal("local request must carry one system spec")
	}
	got := avCBORSpec(t, &specs.Array[0])
	if got != avLocalSpec() {
		t.Fatalf("local tuple changed: %+v", got)
	}
	avText(t, avGet(t, &specs.Array[0], "id"), got.ID)
	var tuple struct {
		System string           `json:"system"`
		ID     string           `json:"zkSystemId"`
		Params circuits.Circuit `json:"params"`
	}
	avJSON(t, "local/zk-system-spec.json", &tuple)
	if tuple.System != got.System || tuple.ID != got.ID || tuple.Params.SpecID() != got.ID || tuple.Params.Hash != got.CircuitHash {
		t.Fatal("JSON tuple disagrees with CBOR request")
	}
	var query DCQL
	avJSON(t, "local/dcql.json", &query)
	want := ZkAgeQuery("proof_of_age", "eu.europa.ec.av.1", "eu.europa.ec.av.1", "age_over_18", accepted[0])
	if !reflect.DeepEqual(query, want) {
		t.Fatal("local DCQL differs from runtime ZkAgeQuery")
	}
	if _, err := query.Validate(1); err != nil {
		t.Fatal(err)
	}
	proof := avRead(t, avFixtureRoot+"local/device-response.cbor")
	if !bytes.Equal(proof, avRead(t, "../zk/testdata/step8-7-openid4vp-zk/device_response.cbor")) {
		t.Fatal("minted DeviceResponse bytes were changed")
	}
	response := avDecode(t, proof)
	avText(t, avGet(t, response, "version"), "1.1")
	if v := avGet(t, response, "status"); v.Kind != cborsub.KUint || v.Uint != 0 {
		t.Fatal("minted response status must be 0")
	}
	docs := avGet(t, response, "zkDocuments")
	if docs.Kind != cborsub.KArray || len(docs.Array) != 1 {
		t.Fatal("minted response must carry one zkDocument")
	}
	doc, err := ParseZkDocument(&docs.Array[0])
	if err != nil || doc.DocType != "eu.europa.ec.av.1" || doc.ZkSystemSpecID != got.ID || len(doc.Proof) == 0 {
		t.Fatalf("minted AV document fields: %+v, %v", doc, err)
	}
	issuer := avDecode(t, doc.IssuerSigned)
	claims := avGet(t, issuer, "eu.europa.ec.av.1")
	if claims.Kind != cborsub.KArray || len(claims.Array) != 1 {
		t.Fatal("minted AV document must disclose one claim")
	}
	avText(t, avGet(t, &claims.Array[0], "elementIdentifier"), "age_over_18")
	if v := avGet(t, &claims.Array[0], "elementValue"); v.Kind != cborsub.KBool || !v.Bool {
		t.Fatal("minted age_over_18 claim must be true")
	}
	var handover struct {
		DocType string `json:"doc_type"`
		SpecID  string `json:"zk_system_spec_id"`
	}
	avJSON(t, "local/proof-request.json", &handover)
	if handover.DocType != doc.DocType || handover.SpecID != doc.ZkSystemSpecID ||
		!bytes.Contains(avRead(t, avFixtureRoot+"local/proof-provenance.txt"), []byte("OpenID4VPHandover")) {
		t.Fatal("copied proof must retain its OpenID4VP provenance")
	}
}

func TestAVProfileRequiredParameters(t *testing.T) {
	accepted := mustSpecs(t)
	raw := avRead(t, avFixtureRoot+"local/dcql.json")
	var positive DCQL
	if err := json.Unmarshal(raw, &positive); err != nil {
		t.Fatal(err)
	}
	meta := positive.Credentials[0].Meta
	if meta.DoctypeValue != "eu.europa.ec.av.1" || len(meta.ZkSystemType) != 1 || meta.ZkSystemType[0] != avLocalSpec() {
		t.Fatal("positive AV tuple changed")
	}
	if result, err := ZkSystemTypeAllowlist(meta, accepted); err != nil || len(result) != 1 || result[0].Hash != avCircuitHash {
		t.Fatalf("positive local tuple refused: %v", err)
	}
	t.Run("legacy-system-compatibility", func(t *testing.T) {
		legacy := meta.ZkSystemType[0]
		legacy.System = SystemName
		if _, err := ZkSystemTypeAllowlist(&Meta{ZkSystemType: []ZkSystemTypeSpec{legacy}}, accepted); err != nil {
			t.Fatalf("existing carrier compatibility alias refused: %v", err)
		}
	})
	unlisted := accepted[0]
	unlisted.Hash = strings.Repeat("0", 64)
	for _, registry := range []struct {
		name     string
		accepted []circuits.Circuit
	}{
		{"nil", nil},
		{"empty", []circuits.Circuit{}},
		{"different-circuit", []circuits.Circuit{unlisted}},
	} {
		t.Run("accepted-set/"+registry.name, func(t *testing.T) {
			if _, err := ZkSystemTypeAllowlist(meta, registry.accepted); err == nil {
				t.Fatal("valid fixture accepted without its circuit in the supplied set")
			}
		})
	}
	for _, offer := range []struct {
		name string
		meta *Meta
	}{
		{"nil-meta", nil},
		{"absent-list", &Meta{DoctypeValue: meta.DoctypeValue}},
		{"empty-list", &Meta{DoctypeValue: meta.DoctypeValue, ZkSystemType: []ZkSystemTypeSpec{}}},
	} {
		t.Run("missing-offer/"+offer.name, func(t *testing.T) {
			if _, err := ZkSystemTypeAllowlist(offer.meta, accepted); err == nil {
				t.Fatal("missing offer accepted")
			}
		})
	}
	for _, field := range []string{"circuit_hash", "version", "num_attributes", "block_enc_hash", "block_enc_sig"} {
		for _, value := range []string{"null", "true", "-1", "1.5", "4294967296"} {
			t.Run(field+"/malformed/"+value, func(t *testing.T) {
				var spec map[string]json.RawMessage
				original, err := json.Marshal(meta.ZkSystemType[0])
				if err != nil {
					t.Fatal(err)
				}
				if err := json.Unmarshal(original, &spec); err != nil {
					t.Fatal(err)
				}
				spec[field] = json.RawMessage(value)
				changed, err := json.Marshal(spec)
				if err != nil {
					t.Fatal(err)
				}
				var malformed ZkSystemTypeSpec
				if err := json.Unmarshal(changed, &malformed); err != nil {
					return // Malformed representations may be refused at JSON decoding.
				}
				if _, err := ZkSystemTypeAllowlist(&Meta{ZkSystemType: []ZkSystemTypeSpec{malformed}}, accepted); err == nil {
					t.Fatal("malformed representation accepted after decoding")
				}
			})
		}
	}
	for _, field := range []string{"circuit_hash", "version", "num_attributes", "block_enc_hash", "block_enc_sig", "system", "id"} {
		for _, mode := range []string{"missing", "tampered"} {
			t.Run(field+"/"+mode, func(t *testing.T) {
				var obj map[string]any
				if err := json.Unmarshal(raw, &obj); err != nil {
					t.Fatal(err)
				}
				credential := obj["credentials"].([]any)[0].(map[string]any)
				spec := credential["meta"].(map[string]any)["zk_system_type"].([]any)[0].(map[string]any)
				if mode == "missing" {
					delete(spec, field)
				} else if field == "circuit_hash" {
					spec[field] = strings.Repeat("0", 64)
				} else if field == "system" {
					spec[field] = "unlisted-system"
				} else if field == "id" {
					spec[field] = "unlisted-id"
				} else {
					spec[field] = spec[field].(float64) + 1
				}
				changed, err := json.Marshal(obj)
				if err != nil {
					t.Fatal(err)
				}
				var query DCQL
				if err := json.Unmarshal(changed, &query); err != nil {
					t.Fatalf("mutation must reach actual allowlist: %v", err)
				}
				bad := query.Credentials[0].Meta.ZkSystemType[0]
				good := meta.ZkSystemType[0]
				for _, layout := range []struct {
					name    string
					entries []ZkSystemTypeSpec
				}{
					{"alone", []ZkSystemTypeSpec{bad}},
					{"valid-first", []ZkSystemTypeSpec{good, bad}},
					{"valid-last", []ZkSystemTypeSpec{bad, good}},
				} {
					t.Run(layout.name, func(t *testing.T) {
						changedMeta := *query.Credentials[0].Meta
						changedMeta.ZkSystemType = layout.entries
						if _, err := ZkSystemTypeAllowlist(&changedMeta, accepted); err == nil {
							t.Fatalf("allowlist accepted %s %s in %s layout", mode, field, layout.name)
						}
					})
				}
			})
		}
	}
}

func TestAVProfileExternalContainers(t *testing.T) {
	var manifest struct {
		Revision        string `json:"profile_revision"`
		LocalRevision   string `json:"local_source_revision"`
		UpstreamVersion int    `json:"upstream_example_circuit_version"`
		LocalVersion    int    `json:"local_circuit_version"`
		Artifacts       []struct {
			Path           string `json:"path"`
			Revision       string `json:"profile_revision"`
			Origin         string `json:"origin"`
			SourceRevision string `json:"source_revision"`
			SHA256         string `json:"sha256"`
			Bytes          int    `json:"bytes"`
			Source         struct {
				Fence int `json:"fence"`
				Start int `json:"byte_start"`
				End   int `json:"byte_end"`
			} `json:"source"`
		} `json:"artifacts"`
	}
	avJSON(t, "manifest.json", &manifest)
	if manifest.Revision != avProfileRevision || manifest.LocalRevision != "cc112f6a0a7705e98fad38b9eac83ba0bb55d96c" || manifest.UpstreamVersion != 6 || manifest.LocalVersion != 7 {
		t.Fatal("manifest version/origin labels changed")
	}
	source := avRead(t, "../../../docs/profiles/av/annex-a-examples.md")
	if sum := fmt.Sprintf("%x", sha256.Sum256(source)); sum != "c907940c18a291fce6b231bc2419fc25433218d89368553582771d695e28afe2" {
		t.Fatal("retained public A.11 bytes changed")
	}
	fences := regexp.MustCompile("(?ms)^```[^\\r\\n]*\\r?\\n(.*?)^```[ \\t]*\\r?$").FindAllSubmatchIndex(source, -1)
	if len(fences) != 32 || len(manifest.Artifacts) != 48 {
		t.Fatal("incomplete source/fixture inventory")
	}
	paths := make(map[string]bool)
	literals := make(map[int]bool)
	for _, artifact := range manifest.Artifacts {
		if paths[artifact.Path] {
			t.Fatalf("duplicate artifact path %s", artifact.Path)
		}
		paths[artifact.Path] = true
		raw := avRead(t, avFixtureRoot+artifact.Path)
		if artifact.Revision != avProfileRevision || len(raw) != artifact.Bytes || fmt.Sprintf("%x", sha256.Sum256(raw)) != artifact.SHA256 {
			t.Fatalf("artifact %s has stale version/digest", artifact.Path)
		}
		if strings.HasPrefix(artifact.Path, "external/") {
			if !strings.HasPrefix(artifact.Origin, "upstream-") || artifact.SourceRevision != avProfileRevision {
				t.Fatal("external source provenance lost")
			}
		} else if artifact.SourceRevision != manifest.LocalRevision || !strings.HasPrefix(artifact.Origin, "generated-local-") && artifact.Origin != "copied-minted-multipaz-fixture" {
			t.Fatal("local source provenance lost")
		}
		if artifact.Origin == "upstream-literal" {
			if artifact.Source.Fence < 1 || artifact.Source.Fence > len(fences) {
				t.Fatal("invalid source fence")
			}
			if literals[artifact.Source.Fence] {
				t.Fatalf("duplicate literal fence %d", artifact.Source.Fence)
			}
			literals[artifact.Source.Fence] = true
			fence := fences[artifact.Source.Fence-1]
			if artifact.Source.Start != fence[2] || artifact.Source.End != fence[3] || !bytes.Equal(raw, source[fence[2]:fence[3]]) {
				t.Fatalf("external literal %s was normalized or changed", artifact.Path)
			}
		}
	}
	for fence := 1; fence <= len(fences); fence++ {
		if !literals[fence] {
			t.Fatalf("missing external literal fence %d", fence)
		}
	}
	for _, prefix := range []string{"14-dcapi", "20-dcapi-zkp"} {
		var request struct {
			Requests []struct {
				Protocol string
				Data     map[string]string
			}
		}
		avJSON(t, "external/"+prefix+"-request.json", &request)
		if len(request.Requests) != 1 || request.Requests[0].Protocol != "org-iso-mdoc" {
			t.Fatal("external DC API request shape")
		}
		for key, suffix := range map[string]string{"deviceRequest": "device-request", "encryptionInfo": "encryption-info"} {
			decoded, err := base64.RawURLEncoding.DecodeString(request.Requests[0].Data[key])
			if err != nil || !bytes.Equal(decoded, avRead(t, avFixtureRoot+"external/"+prefix+"-"+suffix+".cbor")) {
				t.Fatalf("external %s bytes differ: %v", key, err)
			}
		}
		avItems(t, "external/"+prefix+"-device-request.cbor")
	}
	for _, prefix := range []string{"16-dcapi", "22-dcapi-zkp"} {
		var response struct{ Response string }
		avJSON(t, "external/"+prefix+"-response.json", &response)
		decoded, err := base64.RawURLEncoding.DecodeString(response.Response)
		if err != nil || !bytes.Equal(decoded, avRead(t, avFixtureRoot+"external/"+prefix+"-encrypted-response.cbor")) {
			t.Fatalf("encrypted external response bytes differ: %v", err)
		}
		outer := avDecode(t, decoded)
		if outer.Kind != cborsub.KArray || len(outer.Array) != 2 {
			t.Fatal("encrypted response must be [dcapi, map]")
		}
		avText(t, &outer.Array[0], "dcapi")
		if enc := avGet(t, &outer.Array[1], "enc"); enc.Kind != cborsub.KBytes || len(enc.Bytes) != 65 {
			t.Fatal("enc must retain a 65-byte public key")
		}
		if ct := avGet(t, &outer.Array[1], "cipherText"); ct.Kind != cborsub.KBytes || len(ct.Bytes) == 0 {
			t.Fatal("cipherText must remain opaque nonempty bytes")
		}
	}
	var token map[string][]string
	avJSON(t, "external/29-oid4vp-vp-token.json", &token)
	if len(token["proof_of_age"]) != 1 {
		t.Fatal("external VP token shape")
	}
	decoded, err := base64.RawURLEncoding.DecodeString(token["proof_of_age"][0])
	if err != nil || !bytes.Equal(decoded, avRead(t, avFixtureRoot+"external/29-oid4vp-device-response.cbor")) {
		t.Fatalf("external plain DeviceResponse bytes differ: %v", err)
	}
	plain := avDecode(t, decoded)
	avText(t, avGet(t, plain, "version"), "1.0")
	docs := avGet(t, plain, "documents")
	if docs.Kind != cborsub.KArray || len(docs.Array) != 1 {
		t.Fatal("external plain response must carry one document")
	}
	avText(t, avGet(t, &docs.Array[0], "docType"), "eu.europa.ec.av.1")
	for _, prefix := range []string{"18-dcapi", "24-dcapi-zkp", "31-oid4vp"} {
		hexRaw := avRead(t, avFixtureRoot+"external/"+prefix+"-transcript.hex")
		decoded, err := hex.DecodeString(strings.TrimSpace(string(hexRaw)))
		if err != nil || !bytes.Equal(decoded, avRead(t, avFixtureRoot+"external/"+prefix+"-transcript.bin")) {
			t.Fatalf("external transcript byte drift: %v", err)
		}
		st := avDecode(t, decoded)
		if st.Kind != cborsub.KArray || len(st.Array) != 3 || st.Array[0].Kind != cborsub.KNull || st.Array[1].Kind != cborsub.KNull {
			t.Fatal("external SessionTranscript shape")
		}
	}
	items := avItems(t, "external/20-dcapi-zkp-device-request.cbor")
	zk := avGet(t, avGet(t, items, "requestInfo"), "zkRequest")
	if v := avGet(t, zk, "zkRequired"); v.Kind != cborsub.KBool || v.Bool {
		t.Fatal("upstream example zkRequired must remain false")
	}
	specs := avGet(t, zk, "systemSpecs")
	if specs.Kind != cborsub.KArray || len(specs.Array) != 4 {
		t.Fatal("upstream ZKP example must retain all four version-6 tuples")
	}
	for i := range specs.Array {
		spec := avCBORSpec(t, &specs.Array[i])
		expectedID := fmt.Sprintf("%s_%d_%d_%d_%d_%s", spec.System, spec.Version, spec.NumAttributes,
			spec.BlockEncHash, spec.BlockEncSig, spec.CircuitHash)
		if spec.Version != 6 || spec.NumAttributes != uint32(i+1) || spec.ID != expectedID {
			t.Fatal("upstream version-6 tuple changed")
		}
		if _, err := ZkSystemTypeAllowlist(&Meta{ZkSystemType: []ZkSystemTypeSpec{spec}}, mustSpecs(t)); err == nil {
			t.Fatal("external v6 example must not be accepted as local v7")
		}
	}
}
