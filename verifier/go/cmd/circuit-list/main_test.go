package main

import (
	"crypto/ed25519"
	"crypto/rand"
	"crypto/x509"
	"encoding/json"
	"encoding/pem"
	"os"
	"os/exec"
	"path/filepath"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/circuits"
)

func TestSigningCLINormalizesOptionalFieldsBeforeSigning(t *testing.T) {
	dir := t.TempDir()
	executable := filepath.Join(dir, "circuit-list")
	build := exec.Command("go", "build", "-o", executable, ".")
	if output, err := build.CombinedOutput(); err != nil {
		t.Fatalf("build: %v\n%s", err, output)
	}
	_, private, err := ed25519.GenerateKey(rand.Reader)
	if err != nil {
		t.Fatal(err)
	}
	encoded, err := x509.MarshalPKCS8PrivateKey(private)
	if err != nil {
		t.Fatal(err)
	}
	keyPath := filepath.Join(dir, "ephemeral.pem")
	if err := os.WriteFile(keyPath, pem.EncodeToMemory(&pem.Block{Type: "PRIVATE KEY", Bytes: encoded}), 0600); err != nil {
		t.Fatal(err)
	}
	original, err := os.ReadFile("../../../circuits.json")
	if err != nil {
		t.Fatal(err)
	}
	for _, tc := range []struct {
		name  string
		value any
	}{{"empty", ""}, {"null", nil}} {
		t.Run(tc.name, func(t *testing.T) {
			var doc map[string]any
			if err := json.Unmarshal(original, &doc); err != nil {
				t.Fatal(err)
			}
			circuit := doc["circuits"].([]any)[0].(map[string]any)
			for _, field := range []string{"deprecated_on", "upstream_tag", "accepted_on"} {
				circuit[field] = tc.value
			}
			circuit["audits"] = []any{}
			source := filepath.Join(dir, tc.name+"-source.json")
			bytes, err := json.Marshal(doc)
			if err != nil {
				t.Fatal(err)
			}
			if err := os.WriteFile(source, bytes, 0600); err != nil {
				t.Fatal(err)
			}
			outputPath, publicPath := filepath.Join(dir, tc.name+"-signed.json"), filepath.Join(dir, tc.name+"-public.json")
			cmd := exec.Command(executable, "-source", source, "-key", keyPath, "-key-id", "ephemeral-test", "-out", outputPath, "-public-out", publicPath)
			if output, err := cmd.CombinedOutput(); err != nil {
				t.Fatalf("sign: %v\n%s", err, output)
			}
			signed, err := os.ReadFile(outputPath)
			if err != nil {
				t.Fatal(err)
			}
			public, err := os.ReadFile(publicPath)
			if err != nil {
				t.Fatal(err)
			}
			parsed, err := circuits.DecodeDocument(signed)
			if err != nil {
				t.Fatal(err)
			}
			issued, err := time.Parse(time.RFC3339, parsed.IssuedAt)
			if err != nil {
				t.Fatal(err)
			}
			reg, err := circuits.ValidateDocument(signed, public, parsed.MinLibraryVersion, issued)
			if err != nil || reg.ListVersion() != parsed.ListVersion {
				t.Fatalf("emitted roundtrip: %v", err)
			}
			var normalized map[string]any
			if err := json.Unmarshal(signed, &normalized); err != nil {
				t.Fatal(err)
			}
			entry := normalized["circuits"].([]any)[0].(map[string]any)
			for _, field := range []string{"deprecated_on", "upstream_tag", "accepted_on", "audits"} {
				if _, exists := entry[field]; exists {
					t.Fatalf("empty optional %s was not normalized", field)
				}
			}
		})
	}
}
