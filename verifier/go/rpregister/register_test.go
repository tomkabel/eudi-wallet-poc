package rpregister

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"testing"
	"time"
)

type sourceFunc func(RegisterIdentity) (RegisteredIntendedUse, error)

func (f sourceFunc) IntendedUse(id RegisterIdentity) (RegisteredIntendedUse, error) { return f(id) }
func record(id RegisterIdentity) RegisteredIntendedUse {
	return RegisteredIntendedUse{TradeName: "Synthetic", UniqueIdentifier: id.UniqueIdentifier, ServiceIdentifier: id.ServiceIdentifier, MemberState: "EE", IntendedUses: []DoctypePredicate{{"ee.riik.poa.1", "age_over_18"}}}
}

var identity = RegisterIdentity{"synthetic", "age-verification"}

func TestCacheBoundariesIsolationCopies(t *testing.T) {
	now := time.Date(2026, 11, 12, 0, 0, 0, 0, time.UTC)
	calls := 0
	var failure error
	var logs bytes.Buffer
	c, err := NewCache(sourceFunc(func(id RegisterIdentity) (RegisteredIntendedUse, error) { calls++; return record(id), failure }), time.Minute, 2, func(f string, a ...any) { fmt.Fprintf(&logs, f, a...) })
	if err != nil {
		t.Fatal(err)
	}
	c.now = func() time.Time { return now }
	r, err := c.IntendedUse(identity)
	if err != nil {
		t.Fatal(err)
	}
	r.IntendedUses[0].Predicate = "age_over_99"
	now = now.Add(time.Minute - time.Nanosecond)
	failure = ErrRegisterUnreachable
	r, err = c.IntendedUse(identity)
	if err != nil || calls != 1 || r.IntendedUses[0].Predicate != "age_over_18" {
		t.Fatal("fresh/cache copy", r, err, calls)
	}
	other := RegisterIdentity{"other", "age-verification"}
	if _, err = c.IntendedUse(other); !errors.Is(err, ErrRegisterUnreachable) {
		t.Fatal(err)
	}
	now = now.Add(time.Nanosecond)
	if _, err = c.IntendedUse(identity); !errors.Is(err, ErrRegisterUnreachable) {
		t.Fatal("TTL boundary allowed", err)
	}
	if len(c.entries) != 0 {
		t.Fatal("stale retained")
	}
	failure = ErrNotFound
	if _, err = c.IntendedUse(identity); !errors.Is(err, ErrNotFound) {
		t.Fatal(err)
	}
	if len(c.entries) != 0 {
		t.Fatal("error cached")
	}
	failure = nil
	for _, id := range []RegisterIdentity{identity, other, {"third", "age-verification"}} {
		now = now.Add(time.Second)
		if _, err = c.IntendedUse(id); err != nil {
			t.Fatal(err)
		}
	}
	if len(c.entries) != 2 {
		t.Fatal(len(c.entries))
	}
	if _, ok := c.entries[identity]; ok {
		t.Fatal("oldest entry retained")
	}
	for _, word := range []string{"cache-hit-within-ttl", "network=not-contacted", "stale-discard-relookup", "capacity-eviction"} {
		if !strings.Contains(logs.String(), word) {
			t.Fatal(logs.String())
		}
	}
}
func TestConcurrentCache(t *testing.T) {
	c, _ := NewCache(sourceFunc(func(id RegisterIdentity) (RegisteredIntendedUse, error) { return record(id), nil }), time.Minute, 3, nil)
	var wg sync.WaitGroup
	for i := 0; i < 50; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			r, e := c.IntendedUse(RegisterIdentity{fmt.Sprint(i % 5), "service"})
			if errors.Is(e, ErrRegisterUnreachable) {
				return
			}
			if e != nil {
				t.Error(e)
				return
			}
			r.IntendedUses[0].Predicate = "caller-mutated"
		}(i)
	}
	wg.Wait()
	if len(c.entries) > 3 {
		t.Fatal("capacity exceeded")
	}
}
func TestStrictModelAndMock(t *testing.T) {
	m, e := LoadFile("../../../rp-kit/register/synthetic-v1.json")
	if e != nil {
		t.Fatal(e)
	}
	for id, expected := range m.entries {
		got, err := m.IntendedUse(id)
		if err != nil || got.Validate(id) != nil || len(got.IntendedUses) != len(expected.IntendedUses) {
			t.Fatalf("positive corpus entry: %v", err)
		}
	}
	if len(m.entries) != 3 {
		t.Fatal("corpus must have three positive RPs")
	}
	if _, e = m.IntendedUse(identity); !errors.Is(e, ErrNotFound) {
		t.Fatal(e)
	}
	id := RegisterIdentity{"x509_san_dns:verifier.example.ee", "age-verification"}
	r, e := m.IntendedUse(id)
	if e != nil {
		t.Fatal(e)
	}
	r.IntendedUses[0].Predicate = "changed"
	r, e = m.IntendedUse(id)
	if e != nil || r.Validate(id) != nil {
		t.Fatal("mock leaked mutable data")
	}
	for _, mutate := range []func(*RegisteredIntendedUse){func(r *RegisteredIntendedUse) { r.IntendedUses = nil }, func(r *RegisteredIntendedUse) { r.UniqueIdentifier = "wrong" }, func(r *RegisteredIntendedUse) { r.TradeName = " " }, func(r *RegisteredIntendedUse) { r.IntendedUses[0].Predicate = "given_name" }, func(r *RegisteredIntendedUse) { s := ""; r.PrivacyPolicyURL = &s }} {
		r := record(identity)
		mutate(&r)
		if r.Validate(identity) == nil {
			t.Fatal("malformed allowed", r)
		}
	}
}
func TestHTTPContractControls(t *testing.T) {
	for _, mode := range []string{"positive", "404", "5xx", "redirect", "oversize", "malformed", "mismatch"} {
		t.Run(mode, func(t *testing.T) {
			server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				if r.URL.Query().Get("uniqueIdentifier") != identity.UniqueIdentifier || r.URL.Query().Get("serviceIdentifier") != identity.ServiceIdentifier {
					t.Error("identity query missing")
				}
				switch mode {
				case "404":
					w.WriteHeader(404)
				case "5xx":
					w.WriteHeader(500)
				case "redirect":
					w.Header().Set("Location", "/other")
					w.WriteHeader(302)
				case "oversize":
					_, _ = fmt.Fprint(w, strings.Repeat(" ", MaxBodyBytes+1))
				case "malformed":
					_, _ = fmt.Fprint(w, "{}")
				case "mismatch":
					r := record(identity)
					r.ServiceIdentifier = "wrong"
					_ = json.NewEncoder(w).Encode(r)
				default:
					_ = json.NewEncoder(w).Encode(record(identity))
				}
			}))
			defer server.Close()
			h, e := NewHTTP(server.URL, time.Second)
			if e != nil {
				t.Fatal(e)
			}
			_, e = h.IntendedUse(identity)
			switch mode {
			case "positive":
				if e != nil {
					t.Fatal(e)
				}
			case "404":
				if !errors.Is(e, ErrNotFound) {
					t.Fatal(e)
				}
			case "5xx", "redirect":
				if !errors.Is(e, ErrRegisterUnreachable) {
					t.Fatal(e)
				}
			default:
				if !errors.Is(e, ErrInvalid) {
					t.Fatal(e)
				}
			}
		})
	}
	for _, url := range []string{"http://example.com", "http://localhost:123", "https://user:secret@example.com", "file:///tmp/data"} {
		if _, e := NewHTTP(url, time.Second); e == nil {
			t.Fatal("unsafe endpoint", url)
		}
	}
}

func TestNoImplementationAndInvalidProviderNeverCache(t *testing.T) {
	var mock *FileMock
	for _, source := range []RPRegister{nil, mock, sourceFunc(nil), Unconfigured{}} {
		c, _ := NewCache(source, time.Minute, 2, nil)
		if _, err := c.IntendedUse(identity); !errors.Is(err, ErrNoImplementation) {
			t.Fatalf("nil source: %v", err)
		}
	}
	for _, modify := range []func(*RegisteredIntendedUse){func(r *RegisteredIntendedUse) { r.ServiceIdentifier = "other" }, func(r *RegisteredIntendedUse) { r.IntendedUses = nil }} {
		c, _ := NewCache(sourceFunc(func(id RegisterIdentity) (RegisteredIntendedUse, error) { r := record(id); modify(&r); return r, nil }), time.Minute, 2, nil)
		if _, err := c.IntendedUse(identity); !errors.Is(err, ErrInvalid) {
			t.Fatal(err)
		}
		if len(c.entries) != 0 {
			t.Fatal("invalid provider cached")
		}
	}
}
func TestJSONRequiresCompleteSixFields(t *testing.T) {
	data, err := json.Marshal(record(identity))
	if err != nil {
		t.Fatal(err)
	}
	for _, field := range []string{"tradeName", "uniqueIdentifier", "serviceIdentifier", "memberState", "intendedUses", "privacyPolicyUrl"} {
		var object map[string]json.RawMessage
		_ = json.Unmarshal(data, &object)
		delete(object, field)
		raw, _ := json.Marshal(object)
		var r RegisteredIntendedUse
		if json.Unmarshal(raw, &r) == nil {
			t.Fatalf("missing field allowed: %s", field)
		}
	}
}

func TestStrictJSONCorpus(t *testing.T) {
	raw, err := os.ReadFile("../../../rp-kit/register/synthetic-v1.json")
	if err != nil {
		t.Fatal(err)
	}
	var corpus map[string]json.RawMessage
	_ = json.Unmarshal(raw, &corpus)
	var records []json.RawMessage
	_ = json.Unmarshal(corpus["registrations"], &records)
	r := string(records[0])
	pair := `{"doctype":"ee.riik.poa.1","predicate":"age_over_18"}`
	for name, data := range map[string]string{
		"unknown-version":    strings.Replace(string(raw), CorpusVersion, "future/2", 1),
		"duplicate-identity": `{"version":"` + CorpusVersion + `","registrations":[` + r + `,` + r + `]}`,
		"duplicate-pair":     strings.Replace(r, `"intendedUses": [`, `"intendedUses": [`+pair+`,`+pair+`,`, 1),
		"duplicate-key":      strings.Replace(r, `"intendedUses":`, `"intendedUses":[],"intendedUses":`, 1),
		"case-key":           strings.Replace(r, `"intendedUses":`, `"IntendedUses":`, 1),
		"case-pair":          strings.Replace(r, `"doctype":`, `"Doctype":`, 1),
		"duplicate-nested":   strings.Replace(r, `"doctype":`, `"doctype":"wrong","doctype":`, 1),
		"extra-pair":         strings.Replace(r, `"doctype":`, `"extra":1,"doctype":`, 1),
		"extra-corpus":       strings.Replace(string(raw), `"version":`, `"extra":1,"version":`, 1),
		"case-corpus":        strings.Replace(string(raw), `"version":`, `"Version":`, 1),
		"duplicate-corpus":   strings.Replace(string(raw), `"version":`, `"version":"bad","version":`, 1),
		"trailing":           string(raw) + ` {}`,
		"oversize":           strings.Repeat(" ", MaxBodyBytes+1),
	} {
		t.Run(name, func(t *testing.T) {
			if strings.HasPrefix(strings.TrimSpace(data), `{"tradeName"`) || strings.Contains(name, "pair") || name == "duplicate-key" || name == "case-key" || name == "duplicate-nested" {
				data = `{"version":"` + CorpusVersion + `","registrations":[` + data + `]}`
			}
			path := filepath.Join(t.TempDir(), "corpus.json")
			if err := os.WriteFile(path, []byte(data), 0600); err != nil {
				t.Fatal(err)
			}
			if _, err := LoadFile(path); !errors.Is(err, ErrInvalid) {
				t.Fatalf("%s: %v", name, err)
			}
		})
	}
}
func TestHTTPBodyDeadline(t *testing.T) {
	release := make(chan struct{})
	completed := make(chan struct{})
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(200)
		w.(http.Flusher).Flush()
		<-release
		_ = json.NewEncoder(w).Encode(record(identity))
		close(completed)
	}))
	defer server.Close()
	defer close(release)
	h, _ := NewHTTP(server.URL, 50*time.Millisecond)
	start := time.Now()
	_, err := h.IntendedUse(identity)
	if !errors.Is(err, ErrRegisterUnreachable) || time.Since(start) > time.Second {
		t.Fatalf("deadline: %v %s", err, time.Since(start))
	}
	select {
	case <-completed:
		t.Fatal("body completed before timeout")
	default:
	}
}
func TestCacheBoundedColdAdmission(t *testing.T) {
	entered := make(chan struct{})
	release := make(chan struct{})
	c, _ := NewCache(sourceFunc(func(id RegisterIdentity) (RegisteredIntendedUse, error) {
		if id.UniqueIdentifier == "blocked" {
			close(entered)
			<-release
			return RegisteredIntendedUse{}, ErrRegisterUnreachable
		}
		return record(id), nil
	}), time.Minute, 3, nil)
	if _, err := c.IntendedUse(identity); err != nil {
		t.Fatal(err)
	}
	done := make(chan struct{})
	go func() { defer close(done); _, _ = c.IntendedUse(RegisterIdentity{"blocked", "service"}) }()
	<-entered
	defer func() { close(release); <-done }()
	var wg sync.WaitGroup
	for i := 0; i < 20; i++ {
		wg.Add(1)
		go func(i int) {
			defer wg.Done()
			_, err := c.IntendedUse(RegisterIdentity{fmt.Sprint(i), "cold"})
			if !errors.Is(err, ErrRegisterUnreachable) {
				t.Errorf("cold excess: %v", err)
			}
		}(i)
	}
	finished := make(chan struct{})
	go func() { wg.Wait(); close(finished) }()
	select {
	case <-finished:
	case <-time.After(time.Second):
		t.Fatal("cold calls queued")
	}
	hit := make(chan error, 1)
	go func() { _, err := c.IntendedUse(identity); hit <- err }()
	select {
	case err := <-hit:
		if err != nil {
			t.Fatal(err)
		}
	case <-time.After(time.Second):
		t.Fatal("fresh hit blocked")
	}
}
