package oid4vp

import (
	"bytes"
	"compress/zlib"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"sync"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/jose"
)

// Token Status List (IETF draft-ietf-oauth-status-list [UNVERIFIED current
// revision]) verification, PKI plan NEW-S3-2. Status values: 0 VALID,
// 1 INVALID, 2 SUSPENDED; anything else is refused too.
//
// Not wired into the ZK paths on purpose: a status index is a per-credential
// identifier, so revealing it would link every presentation of the credential
// and undo what the zero-knowledge proof hides. ZK credentials rely on short
// validity and single-use batches (EE-POA-010..012) instead. This checker is
// for presentations that disclose the index anyway: WUA and non-ZK documents
// (NEW-S3-3, waiting on stories 10.1 and 10.3).

// StatusValid is the only status value a checker accepts.
const StatusValid = 0

// ErrStatusStale marks a list the checker could not refresh after its ttl or
// exp passed; callers fail closed on it.
var ErrStatusStale = errors.New("status list is stale")

// StatusChecker fetches, verifies and caches status list tokens. Each list
// URI maps to the one key trusted to sign it; a URI with no key is refused.
type StatusChecker struct {
	Fetch func(uri string) ([]byte, error)
	Keys  map[string]jose.JWK
	Now   func() time.Time
	// MaxTTL bounds how long a list is cached whatever its ttl claims.
	MaxTTL time.Duration

	mu    sync.Mutex
	cache map[string]statusList
}

type statusList struct {
	bits    int
	lst     []byte
	expires time.Time // min(exp, fetched+ttl): after this the list is stale
}

// Status returns the status value at idx in the list at uri. It errors
// (fail closed) when the list cannot be fetched fresh, does not verify, or
// does not cover idx.
func (c *StatusChecker) Status(uri string, idx int) (int, error) {
	if idx < 0 {
		return 0, fmt.Errorf("status index %d is negative", idx)
	}
	l, err := c.list(uri)
	if err != nil {
		return 0, err
	}
	bit := idx * l.bits
	if bit/8 >= len(l.lst) {
		return 0, fmt.Errorf("status index %d is outside the %d-entry list at %s", idx, len(l.lst)*8/l.bits, uri)
	}
	return int(l.lst[bit/8]>>(bit%8)) & (1<<l.bits - 1), nil
}

// CheckValid is Status that refuses anything but VALID.
func (c *StatusChecker) CheckValid(uri string, idx int) error {
	s, err := c.Status(uri, idx)
	if err != nil {
		return err
	}
	if s != StatusValid {
		return fmt.Errorf("credential status at %s#%d is %d, not valid", uri, idx, s)
	}
	return nil
}

func (c *StatusChecker) list(uri string) (statusList, error) {
	now := c.Now()
	c.mu.Lock()
	cached, ok := c.cache[uri]
	c.mu.Unlock()
	if ok && now.Before(cached.expires) {
		return cached, nil
	}
	key, ok := c.Keys[uri]
	if !ok {
		return statusList{}, fmt.Errorf("no trusted signing key for status list %s", uri)
	}
	raw, err := c.Fetch(uri)
	if err != nil {
		return statusList{}, fmt.Errorf("%w: fetch %s: %v", ErrStatusStale, uri, err)
	}
	l, err := parseStatusList(bytes.TrimSpace(raw), key, uri, now, c.MaxTTL)
	if err != nil {
		return statusList{}, err
	}
	c.mu.Lock()
	if c.cache == nil {
		c.cache = map[string]statusList{}
	}
	c.cache[uri] = l
	c.mu.Unlock()
	return l, nil
}

func parseStatusList(token []byte, key jose.JWK, uri string, now time.Time, maxTTL time.Duration) (statusList, error) {
	payload, err := jose.VerifyJWSWithKey(string(token), key, jose.JWSTypStatusList)
	if err != nil {
		return statusList{}, fmt.Errorf("status list %s does not verify: %w", uri, err)
	}
	var claims struct {
		Sub        string `json:"sub"`
		Iat        int64  `json:"iat"`
		Exp        int64  `json:"exp"`
		TTL        int64  `json:"ttl"`
		StatusList struct {
			Bits int    `json:"bits"`
			Lst  string `json:"lst"`
		} `json:"status_list"`
	}
	if err := json.Unmarshal(payload, &claims); err != nil {
		return statusList{}, fmt.Errorf("status list %s: claims: %w", uri, err)
	}
	if claims.Sub != uri {
		return statusList{}, fmt.Errorf("status list fetched from %s names sub %q", uri, claims.Sub)
	}
	if claims.Iat == 0 || time.Unix(claims.Iat, 0).After(now.Add(time.Minute)) {
		return statusList{}, fmt.Errorf("status list %s: iat %d is missing or in the future", uri, claims.Iat)
	}
	expires := now.Add(maxTTL)
	if claims.TTL > 0 && now.Add(time.Duration(claims.TTL)*time.Second).Before(expires) {
		expires = now.Add(time.Duration(claims.TTL) * time.Second)
	}
	if claims.Exp != 0 {
		exp := time.Unix(claims.Exp, 0)
		if !now.Before(exp) {
			return statusList{}, fmt.Errorf("%w: %s expired at %s", ErrStatusStale, uri, exp.UTC().Format(time.RFC3339))
		}
		if exp.Before(expires) {
			expires = exp
		}
	}
	switch claims.StatusList.Bits {
	case 1, 2, 4, 8:
	default:
		return statusList{}, fmt.Errorf("status list %s: bits %d is not 1, 2, 4 or 8", uri, claims.StatusList.Bits)
	}
	compressed, err := base64.RawURLEncoding.DecodeString(claims.StatusList.Lst)
	if err != nil {
		return statusList{}, fmt.Errorf("status list %s: lst is not base64url: %w", uri, err)
	}
	zr, err := zlib.NewReader(bytes.NewReader(compressed))
	if err != nil {
		return statusList{}, fmt.Errorf("status list %s: lst is not zlib: %w", uri, err)
	}
	// 1 MiB of status bits is 8M one-bit entries; a list claiming more is refused.
	lst, err := io.ReadAll(io.LimitReader(zr, 1<<20+1))
	if err != nil {
		return statusList{}, fmt.Errorf("status list %s: inflate: %w", uri, err)
	}
	if len(lst) > 1<<20 {
		return statusList{}, fmt.Errorf("status list %s inflates past 1 MiB", uri)
	}
	return statusList{bits: claims.StatusList.Bits, lst: lst, expires: expires}, nil
}
