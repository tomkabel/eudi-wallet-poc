package main

import (
	"errors"
	"os"
	"os/exec"
	"path/filepath"
	"strings"
	"testing"
)

// NEW-S1-3 judge: a release build refuses -ignore-trust-freshness at startup
// (non-zero exit naming the flag); a `-tags devflags` build still accepts it.
// `-h` after the flag stops a build that accepts it before it serves anything.
func TestIgnoreTrustFreshnessIsDevBuildOnly(t *testing.T) {
	if testing.Short() {
		t.Skip("builds the verifier binary twice")
	}
	if _, err := os.Stat("../zkverify-ffi/target/release/libzkverify.a"); err != nil {
		t.Skip("Rust staticlib not built (make deps): ", err)
	}
	goBin, err := exec.LookPath("go")
	if err != nil {
		t.Skip("go toolchain not on PATH")
	}
	build := func(tags ...string) string {
		out := filepath.Join(t.TempDir(), "verifier")
		args := append([]string{"build", "-o", out}, tags...)
		if b, err := exec.Command(goBin, append(args, ".")...).CombinedOutput(); err != nil {
			t.Fatalf("go build %v: %v\n%s", tags, err, b)
		}
		return out
	}

	out, err := exec.Command(build(), "-ignore-trust-freshness", "-h").CombinedOutput()
	var exit *exec.ExitError
	if !errors.As(err, &exit) || exit.ExitCode() == 0 {
		t.Fatalf("release build accepted -ignore-trust-freshness (err %v):\n%s", err, out)
	}
	if !strings.Contains(string(out), "flag provided but not defined: -ignore-trust-freshness") {
		t.Fatalf("release build's refusal does not name the flag:\n%s", out)
	}

	out, err = exec.Command(build("-tags", "devflags"), "-ignore-trust-freshness", "-h").CombinedOutput()
	if err != nil || !strings.Contains(string(out), "-ignore-trust-freshness") {
		t.Fatalf("devflags build refused -ignore-trust-freshness (err %v):\n%s", err, out)
	}
}
