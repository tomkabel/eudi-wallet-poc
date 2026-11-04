package oid4vp

import (
	"bytes"
	"compress/zlib"
	"encoding/base64"
	"encoding/json"
	"errors"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
)

const listURI = "https://status.example.ee/lists/1"

func mintStatusList(t *testing.T, k jose.JWK, statuses []byte, bits int, claims map[string]any) []byte {
	t.Helper()
	packed := make([]byte, (len(statuses)*bits+7)/8)
	for i, s := range statuses {
		packed[i*bits/8] |= s << (i * bits % 8)
	}
	var z bytes.Buffer
	w := zlib.NewWriter(&z)
	w.Write(packed)
	w.Close()
	c := map[string]any{"sub": listURI, "iat": time.Now().Unix(), "exp": time.Now().Add(time.Hour).Unix(), "ttl": 300,
		"status_list": map[string]any{"bits": bits, "lst": base64.RawURLEncoding.EncodeToString(z.Bytes())}}
	for k, v := range claims {
		c[k] = v
	}
	payload, _ := json.Marshal(c)
	tok, err := jose.SignJWS(k, jose.JWSTypStatusList, string(payload))
	if err != nil {
		t.Fatal(err)
	}
	return []byte(tok)
}

func checker(t *testing.T, k jose.JWK, serve *[]byte, now *time.Time) *StatusChecker {
	pub := k
	pub.D = ""
	return &StatusChecker{
		Fetch:  func(string) ([]byte, error) { return *serve, nil },
		Keys:   map[string]jose.JWK{listURI: pub},
		Now:    func() time.Time { return *now },
		MaxTTL: time.Hour,
	}
}

// NEW-S3-2 judge: revoked index refused, tampered list refused, stale list
// refused; a valid index accepted.
func TestStatusListVerdicts(t *testing.T) {
	k := *signedKey(t)
	now := time.Now()
	statuses := []byte{0, 1, 0, 2, 0, 0, 0, 0, 1}
	list := mintStatusList(t, k, statuses, 2, nil)
	c := checker(t, k, &list, &now)

	if err := c.CheckValid(listURI, 0); err != nil {
		t.Fatalf("valid index refused: %v", err)
	}
	for _, idx := range []int{1, 3, 8} {
		if err := c.CheckValid(listURI, idx); err == nil {
			t.Fatalf("index %d (status %d) accepted", idx, statuses[idx])
		}
	}
	if _, err := c.Status(listURI, 1000); err == nil {
		t.Fatal("index past the list accepted")
	}

	t.Run("tampered", func(t *testing.T) {
		parts := strings.Split(string(mintStatusList(t, k, []byte{1}, 1, nil)), ".")
		forged := mintStatusList(t, k, []byte{0}, 1, nil)
		fparts := strings.Split(string(forged), ".")
		tampered := []byte(parts[0] + "." + fparts[1] + "." + parts[2])
		now := time.Now()
		if err := checker(t, k, &tampered, &now).CheckValid(listURI, 0); err == nil {
			t.Fatal("list with a swapped payload accepted")
		}
		other := *signedKey(t)
		foreign := mintStatusList(t, other, []byte{0}, 1, nil)
		if err := checker(t, k, &foreign, &now).CheckValid(listURI, 0); err == nil {
			t.Fatal("list signed by an untrusted key accepted")
		}
	})

	t.Run("stale", func(t *testing.T) {
		now := time.Now()
		expired := mintStatusList(t, k, []byte{0}, 1, map[string]any{"exp": now.Add(-time.Second).Unix()})
		if err := checker(t, k, &expired, &now).CheckValid(listURI, 0); !errors.Is(err, ErrStatusStale) {
			t.Fatalf("expired list: err = %v, want ErrStatusStale", err)
		}
		// A cached list past its ttl must be refetched; an unreachable
		// endpoint then fails closed rather than serving the old answer.
		fresh := mintStatusList(t, k, []byte{0}, 1, nil)
		c := checker(t, k, &fresh, &now)
		if err := c.CheckValid(listURI, 0); err != nil {
			t.Fatal(err)
		}
		c.Fetch = func(string) ([]byte, error) { return nil, errors.New("blackholed") }
		now = now.Add(301 * time.Second)
		if err := c.CheckValid(listURI, 0); !errors.Is(err, ErrStatusStale) {
			t.Fatalf("blackholed after ttl: err = %v, want ErrStatusStale", err)
		}
	})

	t.Run("wrong sub and unknown uri", func(t *testing.T) {
		now := time.Now()
		wrong := mintStatusList(t, k, []byte{0}, 1, map[string]any{"sub": "https://elsewhere.example/1"})
		if err := checker(t, k, &wrong, &now).CheckValid(listURI, 0); err == nil {
			t.Fatal("list for another URI accepted")
		}
		if err := checker(t, k, &wrong, &now).CheckValid("https://unknown.example/1", 0); err == nil {
			t.Fatal("URI with no trusted key accepted")
		}
	})
}

// The in-tree mock lists (statuslists/1 and 2) parse as profile tokens, so
// the checker reads the format the mock issuer ships.
func TestStatusListParsesInTreeMockShape(t *testing.T) {
	for _, f := range []string{"../../../statuslists/1", "../../../statuslists/2"} {
		raw, err := os.ReadFile(f)
		if err != nil {
			t.Fatal(err)
		}
		if _, err := jose.VerifyJWS(strings.TrimSpace(string(raw))); err != nil {
			t.Fatalf("%s: %v", f, err)
		}
	}
}
