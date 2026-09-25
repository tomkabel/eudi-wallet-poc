package main

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

// A full read pool sheds a request before its body is touched: the reader
// below fails the test if anything reads from it.
func TestReadPoolShedsBeforeBody(t *testing.T) {
	reads := make(limiter, 1)
	reads.tryAcquire() // pool full
	p := &presenter{sem: make(limiter, 1), reads: reads}

	for _, tc := range []struct {
		path string
		h    http.HandlerFunc
	}{
		{"/present/response/x", p.handleResponse},
		{"/present/dcapi/response/x", p.handleDCAPIResponse},
		{"/zkverify", handleVerify(nil, make(limiter, 1), reads)},
	} {
		req := httptest.NewRequest(http.MethodPost, tc.path, failReader{t})
		rec := httptest.NewRecorder()
		tc.h(rec, req)
		if rec.Code != http.StatusServiceUnavailable || !strings.Contains(rec.Body.String(), "busy") {
			t.Fatalf("%s: got %d %s, want 503 busy", tc.path, rec.Code, rec.Body)
		}
	}
}

type failReader struct{ t *testing.T }

func (f failReader) Read([]byte) (int, error) {
	f.t.Fatal("body was read before admission")
	return 0, nil
}
