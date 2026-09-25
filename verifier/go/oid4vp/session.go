package oid4vp

import (
	"crypto/rand"
	"encoding/base64"
	"errors"
	"fmt"
	"net/url"
	"path"
	"sync"
	"time"
)

// Session is one presentation exchange, from request creation to result.
type Session struct {
	ID          string    `json:"id"`
	Nonce       string    `json:"nonce"`
	ClientID    string    `json:"client_id"`
	ResponseURI string    `json:"response_uri"`
	Query       DCQL      `json:"dcql_query"`
	Created     time.Time `json:"created"`
	ExpectedNow string    `json:"expected_now"`

	// Filled in once a response arrives.
	Answered bool   `json:"answered"`
	Valid    bool   `json:"valid"`
	Detail   string `json:"detail,omitempty"`

	// The ISO 18013-7 Annex C (dcapi) extension, nil on the redirect path.
	// Unexported: it holds the HPKE private key and must never reach the JSON
	// snapshots the API hands out. origin is the configured -dcapi-origin the
	// session's handover hashes; never a request header (plan §5.3).
	iso    *ISOExtension
	origin string
}

// Expired reports whether the session is past its time-to-live. A presentation
// request is short-lived: the nonce is the replay defence and it must not be
// reusable for long.
func (s *Session) Expired(ttl time.Duration) bool {
	return time.Since(s.Created) > ttl
}

// Store holds in-flight sessions. A real deployment would use something
// durable and shared; nothing here is persisted, deliberately — a verifier that
// keeps presentation records is a verifier that can be subpoenaed for them.
type Store struct {
	mu       sync.Mutex
	sessions map[string]*Session
	ttl      time.Duration
}

const maxSessions = 10_000

func NewStore(ttl time.Duration) *Store {
	return &Store{sessions: make(map[string]*Session), ttl: ttl}
}

func randomB64(n int) (string, error) {
	b := make([]byte, n)
	if _, err := rand.Read(b); err != nil {
		return "", fmt.Errorf("oid4vp: random: %w", err)
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}

// New creates a session with a fresh nonce.
func (st *Store) New(clientID, responseURIBase string, q DCQL) (*Session, error) {
	if _, err := q.Single(); err != nil {
		return nil, err
	}
	id, err := randomB64(12)
	if err != nil {
		return nil, err
	}
	nonce, err := randomB64(32)
	if err != nil {
		return nil, err
	}
	created := time.Now()
	s := &Session{
		ID:          id,
		Nonce:       nonce,
		ClientID:    clientID,
		ResponseURI: fmt.Sprintf("%s/%s", responseURIBase, id),
		Query:       q,
		Created:     created,
		ExpectedNow: created.UTC().Format("2006-01-02T15:04:05Z"),
	}
	return st.put(s)
}

// NewWith is New with the nonce and response URI chosen by the caller instead
// of generated. It exists for the OpenID4VP interop tests (the nonce is part of
// the B.2.6.1 handover, so it has to be the value the holder's proof was bound
// to); production sessions keep using New. The session ID is the response
// URI's last path segment, as New's is, so /present/response/<id> finds it.
func (st *Store) NewWith(clientID, responseURI, nonce string, q DCQL) (*Session, error) {
	if _, err := q.Single(); err != nil {
		return nil, err
	}
	if clientID == "" || nonce == "" || responseURI == "" {
		return nil, errors.New("oid4vp: clientID, nonce and responseURI are all required")
	}
	u, err := url.Parse(responseURI)
	if err != nil {
		return nil, fmt.Errorf("oid4vp: responseURI: %w", err)
	}
	id := path.Base(u.Path)
	if id == "." || id == "/" {
		return nil, errors.New("oid4vp: responseURI has no final path segment to use as the session id")
	}
	created := time.Now()
	s := &Session{
		ID:          id,
		Nonce:       nonce,
		ClientID:    clientID,
		ResponseURI: responseURI,
		Query:       q,
		Created:     created,
		ExpectedNow: created.UTC().Format("2006-01-02T15:04:05Z"),
	}
	return st.put(s)
}

// put installs a built session, applying the shared expiry/size discipline.
func (st *Store) put(s *Session) (*Session, error) {
	id := s.ID
	st.mu.Lock()
	defer st.mu.Unlock()
	for id, existing := range st.sessions {
		if existing.Expired(st.ttl) {
			delete(st.sessions, id)
		}
	}
	if len(st.sessions) >= maxSessions {
		return nil, ErrStoreFull
	}
	// New's 96-bit random IDs never collide; a caller-chosen NewWith ID can,
	// and must not silently replace a live session.
	if _, ok := st.sessions[id]; ok {
		return nil, ErrIDInUse
	}
	st.sessions[id] = s
	return snapshot(s), nil
}

var (
	ErrNoSession   = errors.New("oid4vp: no such session")
	ErrExpired     = errors.New("oid4vp: session expired")
	ErrAlreadyUsed = errors.New("oid4vp: session already answered")
	ErrStoreFull   = errors.New("oid4vp: too many active sessions")
	ErrIDInUse     = errors.New("oid4vp: session id already in use")
)

func snapshot(s *Session) *Session {
	copy := *s
	return &copy
}

// Get returns a snapshot of a live session.
func (st *Store) Get(id string) (*Session, error) {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return nil, ErrNoSession
	}
	if s.Expired(st.ttl) {
		delete(st.sessions, id)
		return nil, ErrExpired
	}
	return snapshot(s), nil
}

// Claim returns the session and marks it answered, so a captured vp_token
// cannot be replayed against the same nonce.
func (st *Store) Claim(id string) (*Session, error) {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return nil, ErrNoSession
	}
	if s.Expired(st.ttl) {
		delete(st.sessions, id)
		return nil, ErrExpired
	}
	if s.Answered {
		return nil, ErrAlreadyUsed
	}
	s.Answered = true
	return snapshot(s), nil
}

// Complete records the result and returns a snapshot of the completed session.
func (st *Store) Complete(id string, valid bool, detail string) (*Session, error) {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return nil, ErrNoSession
	}
	s.Valid = valid
	s.Detail = detail
	return snapshot(s), nil
}

// AttachISO installs the dcapi extension on the STORED session. NewISO calls
// this because New returns a snapshot: mutating the snapshot would silently
// drop the extension (the store copy keeps iso nil).
func (st *Store) AttachISO(id string, ext *ISOExtension, origin string) error {
	st.mu.Lock()
	defer st.mu.Unlock()
	s, ok := st.sessions[id]
	if !ok {
		return ErrNoSession
	}
	s.iso = ext
	s.origin = origin
	return nil
}

// ISOTranscript is the dcapi session transcript, computed from the session's
// own stored EncryptionInfo and origin — never from anything on the wire
// (plan §5.3).
func (s *Session) ISOTranscript() ([]byte, error) {
	if s.iso == nil {
		return nil, errors.New("oid4vp: session has no dcapi extension")
	}
	return ISOTranscript(s.iso.EncryptionInfoB64, s.origin)
}

// Transcript is the session transcript this session's holder must sign over.
func (s *Session) Transcript() ([]byte, error) {
	// nil thumbprint: this PoC uses direct_post, not direct_post.jwt, so the
	// response is not encrypted and the third handover element is null.
	return SessionTranscript(s.ClientID, s.Nonce, nil, s.ResponseURI)
}
