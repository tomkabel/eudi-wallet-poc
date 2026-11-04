//go:build devflags

package main

import "flag"

// ignoreTrustFreshnessFlag registers -ignore-trust-freshness. Development
// builds only (`go build -tags devflags`): the flag disables the signed trust
// store's not_before/not_after enforcement (NEW-S1-3, critique C8).
func ignoreTrustFreshnessFlag() *bool {
	return flag.Bool("ignore-trust-freshness", false,
		"skip the signed trust store's not_before/not_after windows (development build only)")
}
