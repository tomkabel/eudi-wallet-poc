// Package rpregister defines the provisional, fail-closed RP registration seam.
package rpregister

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/url"
	"os"
	"reflect"
	"regexp"
	"strings"
	"sync"
	"time"
)

var (
	ErrNoImplementation    = errors.New("rp-register: missing implementation")
	ErrRegisterUnreachable = errors.New("rp-register: register unreachable")
	ErrNotFound            = errors.New("rp-register: not found")
	ErrInvalid             = errors.New("rp-register: invalid registration")
	ErrUnauthorized        = errors.New("rp-register: unauthorized intended use")
)

const DefaultTTL = 15 * time.Minute
const DefaultCapacity = 256
const MaxBodyBytes = 1 << 20
const CorpusVersion = "synthetic-rp-register/1"

type RegisterIdentity struct {
	UniqueIdentifier  string `json:"uniqueIdentifier"`
	ServiceIdentifier string `json:"serviceIdentifier"`
}
type DoctypePredicate struct {
	Doctype   string `json:"doctype"`
	Predicate string `json:"predicate"`
}
type RegisteredIntendedUse struct {
	TradeName         string             `json:"tradeName"`
	UniqueIdentifier  string             `json:"uniqueIdentifier"`
	ServiceIdentifier string             `json:"serviceIdentifier"`
	MemberState       string             `json:"memberState"`
	IntendedUses      []DoctypePredicate `json:"intendedUses"`
	PrivacyPolicyURL  *string            `json:"privacyPolicyUrl"`
}

// UnmarshalJSON requires one complete six-field record, including an explicit
// null privacy marker. No partially populated JSON is upgraded into a record.
func (r *RegisteredIntendedUse) UnmarshalJSON(data []byte) error {
	type plain RegisteredIntendedUse
	if err := strictJSON(data, reflect.TypeOf(plain{})); err != nil {
		return ErrInvalid
	}
	var value plain
	decoder := json.NewDecoder(strings.NewReader(string(data)))
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&value); err != nil {
		return ErrInvalid
	}
	*r = RegisteredIntendedUse(value)
	return nil
}

type RPRegister interface {
	IntendedUse(RegisterIdentity) (RegisteredIntendedUse, error)
}

// Unconfigured is the explicit default; nil consumers return the same error.
type Unconfigured struct{}

func (Unconfigured) IntendedUse(RegisterIdentity) (RegisteredIntendedUse, error) {
	return RegisteredIntendedUse{}, ErrNoImplementation
}

var predicate = regexp.MustCompile(`^age_over_[0-9]{2}$`)
var memberState = regexp.MustCompile(`^[A-Z]{2}$`)

func validText(s string) bool { return strings.TrimSpace(s) == s && s != "" && len(s) <= 512 }
func (id RegisterIdentity) Validate() error {
	if !validText(id.UniqueIdentifier) || !validText(id.ServiceIdentifier) {
		return ErrInvalid
	}
	return nil
}
func (r RegisteredIntendedUse) Validate(id RegisterIdentity) error {
	if id.Validate() != nil || r.UniqueIdentifier != id.UniqueIdentifier || r.ServiceIdentifier != id.ServiceIdentifier || !validText(r.TradeName) || !memberState.MatchString(r.MemberState) || len(r.IntendedUses) == 0 {
		return ErrInvalid
	}
	seen := map[DoctypePredicate]bool{}
	for _, use := range r.IntendedUses {
		if !validText(use.Doctype) || !predicate.MatchString(use.Predicate) || seen[use] {
			return ErrInvalid
		}
		seen[use] = true
	}
	if r.PrivacyPolicyURL != nil {
		u, err := url.Parse(*r.PrivacyPolicyURL)
		if err != nil || u.Scheme != "https" || u.Hostname() == "" || u.User != nil {
			return ErrInvalid
		}
	}
	return nil
}
func clone(r RegisteredIntendedUse) RegisteredIntendedUse {
	r.IntendedUses = append([]DoctypePredicate(nil), r.IntendedUses...)
	if r.PrivacyPolicyURL != nil {
		s := *r.PrivacyPolicyURL
		r.PrivacyPolicyURL = &s
	}
	return r
}

// FileMock loads a strict, synthetic version-labelled corpus once at startup.
type FileMock struct {
	entries map[RegisterIdentity]RegisteredIntendedUse
}

func LoadFile(path string) (*FileMock, error) {
	f, err := os.Open(path)
	if err != nil {
		return nil, err
	}
	defer func() { _ = f.Close() }()
	var corpus struct {
		Version       string                  `json:"version"`
		Registrations []RegisteredIntendedUse `json:"registrations"`
	}
	if err := decode(f, &corpus); err != nil {
		return nil, fmt.Errorf("%w: corpus", ErrInvalid)
	}
	if corpus.Version != CorpusVersion || len(corpus.Registrations) == 0 {
		return nil, ErrInvalid
	}
	m := &FileMock{entries: map[RegisterIdentity]RegisteredIntendedUse{}}
	for _, r := range corpus.Registrations {
		id := RegisterIdentity{r.UniqueIdentifier, r.ServiceIdentifier}
		if r.Validate(id) != nil {
			return nil, ErrInvalid
		}
		if _, exists := m.entries[id]; exists {
			return nil, ErrInvalid
		}
		m.entries[id] = clone(r)
	}
	return m, nil
}
func (m *FileMock) IntendedUse(id RegisterIdentity) (RegisteredIntendedUse, error) {
	if m == nil {
		return RegisteredIntendedUse{}, ErrNoImplementation
	}
	r, ok := m.entries[id]
	if !ok {
		return RegisteredIntendedUse{}, ErrNotFound
	}
	return clone(r), nil
}

// strictJSON checks the existing struct shape before encoding/json's permissive
// case matching can apply. Every object rejects duplicate and unknown keys.
func strictJSON(data []byte, shape reflect.Type) error {
	d := json.NewDecoder(strings.NewReader(string(data)))
	var value func(reflect.Type) error
	value = func(t reflect.Type) error {
		for t.Kind() == reflect.Pointer {
			t = t.Elem()
		}
		token, err := d.Token()
		if err != nil {
			return ErrInvalid
		}
		if token == nil {
			return nil
		}
		switch token {
		case json.Delim('{'):
			if t.Kind() != reflect.Struct {
				return ErrInvalid
			}
			fields := map[string]reflect.Type{}
			for i := 0; i < t.NumField(); i++ {
				f := t.Field(i)
				fields[strings.Split(f.Tag.Get("json"), ",")[0]] = f.Type
			}
			seen := map[string]bool{}
			for d.More() {
				key, err := d.Token()
				if err != nil {
					return ErrInvalid
				}
				name, ok := key.(string)
				if !ok || seen[name] {
					return ErrInvalid
				}
				field, ok := fields[name]
				if !ok {
					return ErrInvalid
				}
				seen[name] = true
				if err := value(field); err != nil {
					return err
				}
			}
			if len(seen) != len(fields) {
				return ErrInvalid
			}
			end, err := d.Token()
			if err != nil || end != json.Delim('}') {
				return ErrInvalid
			}
		case json.Delim('['):
			if t.Kind() != reflect.Slice {
				return ErrInvalid
			}
			for d.More() {
				if err := value(t.Elem()); err != nil {
					return err
				}
			}
			end, err := d.Token()
			if err != nil || end != json.Delim(']') {
				return ErrInvalid
			}
		default:
			if _, ok := token.(json.Delim); ok {
				return ErrInvalid
			}
		}
		return nil
	}
	if err := value(shape); err != nil {
		return err
	}
	if _, err := d.Token(); err != io.EOF {
		return ErrInvalid
	}
	return nil
}

func decode(reader io.Reader, out any) error {
	data, err := io.ReadAll(io.LimitReader(reader, MaxBodyBytes+1))
	if err != nil {
		return ErrRegisterUnreachable
	}
	if len(data) > MaxBodyBytes {
		return ErrInvalid
	}
	if err := strictJSON(data, reflect.TypeOf(out).Elem()); err != nil {
		return err
	}
	d := json.NewDecoder(strings.NewReader(string(data)))
	d.DisallowUnknownFields()
	if err := d.Decode(out); err != nil {
		return err
	}
	if d.Decode(new(any)) != io.EOF {
		return ErrInvalid
	}
	return nil
}

// HTTP is a local proposal, not a claim that any authority exposes this API.
// Its absolute timeout includes headers and reading the bounded response body.
type HTTP struct {
	endpoint string
	client   *http.Client
}

func NewHTTP(endpoint string, timeout time.Duration) (*HTTP, error) {
	u, err := url.Parse(endpoint)
	if err != nil || u.Hostname() == "" || u.User != nil || u.Fragment != "" || timeout <= 0 {
		return nil, ErrInvalid
	}
	ip := net.ParseIP(u.Hostname())
	local := ip != nil && ip.IsLoopback()
	if u.Scheme != "https" && (u.Scheme != "http" || !local) {
		return nil, ErrInvalid
	}
	transport := http.DefaultTransport.(*http.Transport).Clone()
	transport.Proxy = nil
	transport.MaxConnsPerHost = 8
	transport.MaxIdleConnsPerHost = 8
	transport.ResponseHeaderTimeout = timeout
	return &HTTP{endpoint: endpoint, client: &http.Client{Timeout: timeout, Transport: transport, CheckRedirect: func(*http.Request, []*http.Request) error { return http.ErrUseLastResponse }}}, nil
}
func (h *HTTP) IntendedUse(id RegisterIdentity) (RegisteredIntendedUse, error) {
	if id.Validate() != nil {
		return RegisteredIntendedUse{}, ErrInvalid
	}
	u, _ := url.Parse(h.endpoint)
	q := u.Query()
	q.Set("uniqueIdentifier", id.UniqueIdentifier)
	q.Set("serviceIdentifier", id.ServiceIdentifier)
	u.RawQuery = q.Encode()
	response, err := h.client.Get(u.String())
	if err != nil {
		return RegisteredIntendedUse{}, ErrRegisterUnreachable
	}
	defer func() { _ = response.Body.Close() }()
	if response.StatusCode == 404 {
		return RegisteredIntendedUse{}, ErrNotFound
	}
	if response.StatusCode != 200 {
		return RegisteredIntendedUse{}, ErrRegisterUnreachable
	}
	var r RegisteredIntendedUse
	if err := decode(response.Body, &r); err != nil {
		if errors.Is(err, ErrRegisterUnreachable) {
			return RegisteredIntendedUse{}, err
		}
		return r, ErrInvalid
	}
	if err := r.Validate(id); err != nil {
		return RegisteredIntendedUse{}, err
	}
	return r, nil
}

type entry struct {
	value RegisteredIntendedUse
	at    time.Time
}

// Cache admits one cold lookup without queuing; fresh hits remain accessible.
// Reads never change insertion time. Only validated positive records are stored.
type Cache struct {
	mu       sync.Mutex
	lookup   chan struct{}
	source   RPRegister
	ttl      time.Duration
	capacity int
	entries  map[RegisterIdentity]entry
	now      func() time.Time
	logf     func(string, ...any)
}

func NewCache(source RPRegister, ttl time.Duration, capacity int, logf func(string, ...any)) (*Cache, error) {
	if ttl <= 0 || capacity < 1 {
		return nil, ErrInvalid
	}
	return &Cache{lookup: make(chan struct{}, 1), source: source, ttl: ttl, capacity: capacity, entries: map[RegisterIdentity]entry{}, now: time.Now, logf: logf}, nil
}
func (c *Cache) log(format string, args ...any) {
	if c.logf != nil {
		c.logf(format, args...)
	}
}
func (c *Cache) IntendedUse(id RegisterIdentity) (RegisteredIntendedUse, error) {
	if absent(c.source) {
		return RegisteredIntendedUse{}, ErrNoImplementation
	}
	if id.Validate() != nil {
		return RegisteredIntendedUse{}, ErrInvalid
	}
	c.mu.Lock()
	now := c.now()
	if old, ok := c.entries[id]; ok {
		age := now.Sub(old.at)
		if age >= 0 && age < c.ttl {
			c.log("rp-register decision=cache-hit-within-ttl provenance=bounded-positive-cache age=%s ttl=%s network=not-contacted", age, c.ttl)
			c.mu.Unlock()
			return clone(old.value), nil
		}
		delete(c.entries, id)
		c.log("rp-register decision=stale-discard-relookup age=%s ttl=%s", age, c.ttl)
	}
	c.mu.Unlock()
	select {
	case c.lookup <- struct{}{}:
		defer func() { <-c.lookup }()
	default:
		return RegisteredIntendedUse{}, ErrRegisterUnreachable
	}
	r, err := c.source.IntendedUse(id)
	if err != nil {
		return RegisteredIntendedUse{}, err
	}
	if err := r.Validate(id); err != nil {
		return RegisteredIntendedUse{}, err
	}
	c.mu.Lock()
	defer c.mu.Unlock()
	if len(c.entries) >= c.capacity {
		var oldest RegisterIdentity
		var at time.Time
		for key, v := range c.entries {
			if at.IsZero() || v.at.Before(at) {
				oldest = key
				at = v.at
			}
		}
		delete(c.entries, oldest)
		c.log("rp-register decision=capacity-eviction")
	}
	c.entries[id] = entry{clone(r), c.now()}
	c.log("rp-register decision=lookup-positive age=0s ttl=%s", c.ttl)
	return clone(r), nil
}

func absent(source RPRegister) bool {
	if source == nil {
		return true
	}
	v := reflect.ValueOf(source)
	switch v.Kind() {
	case reflect.Chan, reflect.Func, reflect.Interface, reflect.Map, reflect.Pointer, reflect.Slice:
		return v.IsNil()
	}
	return false
}
