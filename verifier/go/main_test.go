package main

import (
	"os"
	"strings"
	"testing"
)

// TestUnsafeDevAPILoopbackGuard pins the guard wiring: -unsafe-dev-api must
// refuse a non-loopback bind (the :8080 default included) and accept the
// loopback spellings, parsed the way net.SplitHostPort parses them.
func TestUnsafeDevAPILoopbackGuard(t *testing.T) {
	cases := []struct {
		addr    string
		wantErr bool
	}{
		{":8080", true},             // the default — all interfaces
		{"0.0.0.0:8080", true},      // explicit all-interfaces
		{"[::]:8080", true},         // IPv6 wildcard
		{"192.168.1.10:8080", true}, // LAN address
		{"example.ee:8080", true},   // hostname is not proof of loopback
		{"127.0.0.1:8080", false},   // IPv4 loopback
		{"[::1]:8080", false},       // IPv6 loopback
		{"localhost:8080", false},   // loopback name
		{"127.0.0.1", true},         // no port: unparseable → refuse
		{"", true},                  // empty → unparseable → refuse
	}
	for _, tc := range cases {
		err := requireLoopbackAddr(tc.addr)
		if (err != nil) != tc.wantErr {
			t.Errorf("requireLoopbackAddr(%q) = %v, wantErr %v", tc.addr, err, tc.wantErr)
		}
	}
}

func TestCheckLongfellowRev(t *testing.T) {
	dir := t.TempDir()
	revFile := dir + "/longfellow-rev.txt"

	if err := checkLongfellowRev("", revFile); err != nil {
		t.Errorf("empty linked rev must pass (dev/-version build): %v", err)
	}
	if err := checkLongfellowRev("abc123", dir+"/missing.txt"); err != nil {
		t.Errorf("missing revfile must pass: %v", err)
	}
	if err := os.WriteFile(revFile, []byte("  \n"), 0o600); err != nil {
		t.Fatal(err)
	}
	if err := checkLongfellowRev("abc123", revFile); err != nil {
		t.Errorf("blank revfile must pass: %v", err)
	}
	if err := os.WriteFile(revFile, []byte("deadbeef\n"), 0o600); err != nil {
		t.Fatal(err)
	}
	if err := checkLongfellowRev("deadbeef", revFile); err != nil {
		t.Errorf("matching rev must pass: %v", err)
	}
	err := checkLongfellowRev("cafebabe", revFile)
	if err == nil || !strings.Contains(err.Error(), "deadbeef") {
		t.Errorf("stale linked rev must fail naming both revs, got %v", err)
	}
}
