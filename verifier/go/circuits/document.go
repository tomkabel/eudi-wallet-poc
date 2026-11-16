package circuits

import (
	"bytes"
	"crypto/ed25519"
	_ "embed"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"
	"unicode/utf16"
	"unicode/utf8"
)

// PinnedKey is public release material. Private release keys never enter the checkout.
//
//go:embed pinned-key.json
var PinnedKey []byte

type Signature struct {
	KeyID string `json:"key_id"`
	Sig   string `json:"sig"`
}
type PublicKey struct {
	KeyID string `json:"key_id"`
	Key   string `json:"public_key"`
}
type Document struct {
	Schema            string      `json:"schema"`
	ListVersion       string      `json:"list_version"`
	IssuedAt          string      `json:"issued_at"`
	ExpiresAt         string      `json:"expires_at"`
	MinLibraryVersion string      `json:"min_library_version"`
	Maintainer        string      `json:"maintainer"`
	Systems           []string    `json:"systems"`
	Circuits          []Circuit   `json:"circuits"`
	Signatures        []Signature `json:"signatures"`
}

// CanonicalPayload implements RFC 8785 for this document's deliberately integer-only
// schema. Floating point JSON is rejected rather than signed with a non-JCS formatter.
// Duplicate keys and unsafe integers are likewise rejected.
func CanonicalPayload(data []byte) ([]byte, error) {
	if err := validateUnicodeEscapes(data); err != nil {
		return nil, err
	}
	if !utf8.Valid(data) {
		return nil, fmt.Errorf("invalid Unicode")
	}
	dec := json.NewDecoder(bytes.NewReader(data))
	dec.UseNumber()
	value, err := readJSON(dec)
	if err != nil {
		return nil, err
	}
	if _, err := dec.Token(); err != io.EOF {
		return nil, fmt.Errorf("trailing JSON")
	}
	obj, ok := value.(map[string]any)
	if !ok {
		return nil, fmt.Errorf("document must be an object")
	}
	delete(obj, "signatures")
	var out bytes.Buffer
	if err := canonical(&out, obj); err != nil {
		return nil, err
	}
	return out.Bytes(), nil
}
func readJSON(dec *json.Decoder) (any, error) {
	tok, err := dec.Token()
	if err != nil {
		return nil, err
	}
	switch t := tok.(type) {
	case json.Delim:
		if t == '{' {
			m := map[string]any{}
			for dec.More() {
				k, e := dec.Token()
				if e != nil {
					return nil, e
				}
				key := k.(string)
				if _, exists := m[key]; exists {
					return nil, fmt.Errorf("duplicate JSON key %s", key)
				}
				v, e := readJSON(dec)
				if e != nil {
					return nil, e
				}
				m[key] = v
			}
			_, err = dec.Token()
			return m, err
		}
		if t == '[' {
			a := []any{}
			for dec.More() {
				v, e := readJSON(dec)
				if e != nil {
					return nil, e
				}
				a = append(a, v)
			}
			_, err = dec.Token()
			return a, err
		}
		return nil, fmt.Errorf("unexpected delimiter")
	default:
		return tok, nil
	}
}
func canonical(out *bytes.Buffer, value any) error {
	switch v := value.(type) {
	case map[string]any:
		keys := make([]string, 0, len(v))
		for k := range v {
			keys = append(keys, k)
		}
		sort.Slice(keys, func(i, j int) bool {
			a, b := utf16.Encode([]rune(keys[i])), utf16.Encode([]rune(keys[j]))
			for k := 0; k < len(a) && k < len(b); k++ {
				if a[k] != b[k] {
					return a[k] < b[k]
				}
			}
			return len(a) < len(b)
		})
		out.WriteByte('{')
		for i, k := range keys {
			if i > 0 {
				out.WriteByte(',')
			}
			writeString(out, k)
			out.WriteByte(':')
			if err := canonical(out, v[k]); err != nil {
				return err
			}
		}
		out.WriteByte('}')
	case []any:
		out.WriteByte('[')
		for i, e := range v {
			if i > 0 {
				out.WriteByte(',')
			}
			if err := canonical(out, e); err != nil {
				return err
			}
		}
		out.WriteByte(']')
	case string:
		writeString(out, v)
	case json.Number:
		n, err := strconv.ParseInt(string(v), 10, 64)
		if err != nil || n < -9007199254740991 || n > 9007199254740991 {
			return fmt.Errorf("only safe integer JSON numbers are supported")
		}
		out.WriteString(strconv.FormatInt(n, 10))
	case bool:
		out.WriteString(strconv.FormatBool(v))
	case nil:
		out.WriteString("null")
	default:
		return fmt.Errorf("unsupported JSON value")
	}
	return nil
}
func writeString(out *bytes.Buffer, s string) {
	out.WriteByte('"')
	for _, r := range s {
		switch r {
		case '"':
			out.WriteByte(92)
			out.WriteByte('"')
		case 92:
			out.WriteByte(92)
			out.WriteByte(92)
		case 8:
			out.WriteByte(92)
			out.WriteByte('b')
		case 9:
			out.WriteByte(92)
			out.WriteByte('t')
		case 10:
			out.WriteByte(92)
			out.WriteByte('n')
		case 12:
			out.WriteByte(92)
			out.WriteByte('f')
		case 13:
			out.WriteByte(92)
			out.WriteByte('r')
		default:
			if r < 32 {
				out.WriteByte(92)
				fmt.Fprintf(out, "u%04x", r)
			} else {
				out.WriteRune(r)
			}
		}
	}
	out.WriteByte('"')
}

func DecodeDocument(data []byte) (*Document, error) {
	if _, err := CanonicalPayload(data); err != nil {
		return nil, fmt.Errorf("invalid-list: %w", err)
	}
	if err := validateOptionalMetadata(data); err != nil {
		return nil, err
	}
	var doc Document
	dec := json.NewDecoder(bytes.NewReader(data))
	dec.DisallowUnknownFields()
	if err := dec.Decode(&doc); err != nil {
		return nil, fmt.Errorf("invalid-list: %w", err)
	}
	if doc.Schema != "ee-circuits/1" {
		return nil, fmt.Errorf("invalid-list-schema")
	}
	n, err := strconv.ParseUint(doc.ListVersion, 10, 64)
	if err != nil || n == 0 || strconv.FormatUint(n, 10) != doc.ListVersion {
		return nil, fmt.Errorf("invalid-list-version")
	}
	if doc.Maintainer == "" || len(doc.Circuits) == 0 {
		return nil, fmt.Errorf("missing-list-prerequisite")
	}
	if len(doc.Systems) != 2 || !contains(doc.Systems, "longfellow-libzk-v1") || !contains(doc.Systems, "org.iso.mdoc.zk") {
		return nil, fmt.Errorf("invalid-list-systems")
	}
	if _, err := NewRegistry(doc.Circuits); err != nil {
		return nil, err
	}
	for _, c := range doc.Circuits {
		if len(c.Hash) != 64 || strings.Trim(c.Hash, "0123456789abcdef") != "" || c.Version == 0 || c.NumAttributes == 0 || c.BlockEncHash == 0 || c.BlockEncSig == 0 {
			return nil, fmt.Errorf("invalid-circuit-parameters")
		}
	}
	return &doc, nil
}

// encoding/json accepts null string elements as empty strings. Reject that
// coercion for audits so Go and the wallet agree on every metadata type.
// Optional scalar nulls, like omitted/empty values, retain their zero value.
func validateOptionalMetadata(data []byte) error {
	var root struct {
		Circuits []map[string]json.RawMessage `json:"circuits"`
	}
	if err := json.Unmarshal(data, &root); err != nil {
		return fmt.Errorf("invalid-circuit-metadata: %w", err)
	}
	for _, entry := range root.Circuits {
		for _, field := range []string{"upstream_tag", "accepted_on", "deprecated_on"} {
			if raw, exists := entry[field]; exists && !bytes.Equal(bytes.TrimSpace(raw), []byte("null")) {
				var value string
				if err := json.Unmarshal(raw, &value); err != nil {
					return fmt.Errorf("invalid-circuit-metadata: %s", field)
				}
			}
		}
		if raw, exists := entry["audits"]; exists && !bytes.Equal(bytes.TrimSpace(raw), []byte("null")) {
			var values []json.RawMessage
			if err := json.Unmarshal(raw, &values); err != nil {
				return fmt.Errorf("invalid-circuit-metadata: audits")
			}
			for _, item := range values {
				var value string
				if bytes.Equal(bytes.TrimSpace(item), []byte("null")) || json.Unmarshal(item, &value) != nil {
					return fmt.Errorf("invalid-circuit-metadata: audits element")
				}
			}
		}
	}
	return nil
}
func contains(a []string, want string) bool {
	for _, s := range a {
		if s == want {
			return true
		}
	}
	return false
}

// Git revisions have no lexical version order. Until a compatibility manifest
// exists, only the exact audited revision is supported; upgrades fail closed.
func ValidateDocument(data, keyData []byte, runtime string, now time.Time) (*Registry, error) {
	return ValidateDocumentWithClock(data, keyData, runtime, func() time.Time { return now }, time.Now)
}

// ValidateDocumentWithClock separates validation time from the live expiry clock.
// Services normally use ValidateDocument; deterministic consumers can inject both.
func ValidateDocumentWithClock(data, keyData []byte, runtime string, validationClock, liveClock func() time.Time) (*Registry, error) {
	now := validationClock()
	doc, err := DecodeDocument(data)
	if err != nil {
		return nil, err
	}
	var key PublicKey
	if err := json.Unmarshal(keyData, &key); err != nil || key.KeyID == "" {
		return nil, fmt.Errorf("invalid-pinned-key")
	}
	pub, err := base64.RawURLEncoding.DecodeString(key.Key)
	if err != nil || len(pub) != ed25519.PublicKeySize {
		return nil, fmt.Errorf("invalid-pinned-key")
	}
	payload, err := CanonicalPayload(data)
	if err != nil {
		return nil, err
	}
	verified := false
	for _, sig := range doc.Signatures {
		if sig.KeyID == key.KeyID {
			b, e := base64.RawURLEncoding.DecodeString(sig.Sig)
			if e == nil && ed25519.Verify(pub, payload, b) {
				verified = true
			}
		}
	}
	if !verified {
		return nil, fmt.Errorf("invalid-list-signature-or-unknown-key")
	}
	issued, err := time.Parse(time.RFC3339, doc.IssuedAt)
	if err != nil || issued.After(now) {
		return nil, fmt.Errorf("invalid-list-issued-at")
	}
	expiry, err := time.Parse(time.RFC3339, doc.ExpiresAt)
	if err != nil || !expiry.After(issued) {
		return nil, fmt.Errorf("invalid-list-expiry")
	}
	if !now.Before(expiry) {
		return nil, fmt.Errorf("stale-list")
	}
	if len(runtime) != 40 || doc.MinLibraryVersion != runtime {
		return nil, fmt.Errorf("incompatible-library-version: list %s runtime %s", doc.MinLibraryVersion, runtime)
	}
	r, err := NewRegistry(doc.Circuits)
	if err != nil {
		return nil, err
	}
	r.document = doc
	r.expiresAt = expiry
	r.now = liveClock
	return r, nil
}
func LoadSigned(path string, keyData []byte, runtime string, now time.Time) (*Registry, error) {
	b, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("missing-local-list: %w", err)
	}
	return ValidateDocument(b, keyData, runtime, now)
}
func (r *Registry) ListVersion() string {
	if r.document == nil {
		return ""
	}
	return r.document.ListVersion
}

// encoding/json replaces unpaired UTF-16 escapes; RFC 8785 requires rejection.
func validateUnicodeEscapes(data []byte) error {
	inString := false
	for i := 0; i < len(data); i++ {
		if data[i] == '"' {
			inString = !inString
			continue
		}
		if !inString || data[i] != 92 {
			continue
		}
		i++
		if i >= len(data) {
			return fmt.Errorf("invalid string escape")
		}
		if data[i] != 'u' {
			continue
		}
		if i+4 >= len(data) {
			return fmt.Errorf("invalid Unicode escape")
		}
		n, err := strconv.ParseUint(string(data[i+1:i+5]), 16, 16)
		if err != nil {
			return fmt.Errorf("invalid Unicode escape")
		}
		i += 4
		if n >= 0xdc00 && n <= 0xdfff {
			return fmt.Errorf("unpaired Unicode surrogate")
		}
		if n >= 0xd800 && n <= 0xdbff {
			if i+6 >= len(data) || data[i+1] != 92 || data[i+2] != 'u' {
				return fmt.Errorf("unpaired Unicode surrogate")
			}
			low, err := strconv.ParseUint(string(data[i+3:i+7]), 16, 16)
			if err != nil || low < 0xdc00 || low > 0xdfff {
				return fmt.Errorf("unpaired Unicode surrogate")
			}
			i += 6
		}
	}
	return nil
}
