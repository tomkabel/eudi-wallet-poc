//go:build !devflags

package main

// ignoreTrustFreshnessFlag is the release stub: the flag is not registered,
// so passing -ignore-trust-freshness fails flag parsing and the binary exits
// non-zero, and trust-store freshness is always enforced.
func ignoreTrustFreshnessFlag() *bool { return new(bool) }
