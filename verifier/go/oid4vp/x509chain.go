package oid4vp

import (
	"crypto/x509"
	"encoding/asn1"
	"errors"
	"fmt"
	"slices"
	"time"

	"github.com/tomkabel/eudi-wallet-poc/verifier/go/internal/algpolicy"
)

// ChainProfile names the certificate profile a chain is validated against.
type ChainProfile string

const (
	// ProfileGeneric is RFC 5280 path validation plus the algorithm policy.
	ProfileGeneric ChainProfile = "generic"
	// ProfileMdocDS adds the ISO/IEC 18013-5 document signer rules: the
	// id-mdl-kp-mdlDS EKU, keyUsage digitalSignature, and not a CA.
	ProfileMdocDS ChainProfile = "mdoc_ds"
)

// oidMdlDS is id-mdl-kp-mdlDS (ISO/IEC 18013-5).
var oidMdlDS = asn1.ObjectIdentifier{1, 0, 18013, 5, 1, 2}

// VerifyChain validates chain (DER, leaf first) to one of anchors at the given
// instant and returns the leaf (PKI plan NEW-S2-1). Every non-anchor
// certificate must also pass the shared algorithm policy (NEW-S1-2). Nothing in
// the chain can become an anchor: anchors come from the trust store only.
func VerifyChain(chain [][]byte, anchors []*x509.Certificate, at time.Time, profile ChainProfile) (*x509.Certificate, error) {
	if len(chain) == 0 {
		return nil, errors.New("certificate chain is empty")
	}
	if len(anchors) == 0 {
		return nil, errors.New("no trust anchor configured for this chain")
	}
	certs := make([]*x509.Certificate, 0, len(chain))
	for i, der := range chain {
		c, err := x509.ParseCertificate(der)
		if err != nil {
			return nil, fmt.Errorf("chain certificate %d does not parse: %w", i, err)
		}
		certs = append(certs, c)
	}
	leaf := certs[0]
	for _, c := range certs {
		if err := algpolicy.Default.CheckCertificate(c); err != nil {
			return nil, err
		}
	}
	roots := x509.NewCertPool()
	for _, a := range anchors {
		roots.AddCert(a)
	}
	inter := x509.NewCertPool()
	for _, c := range certs[1:] {
		inter.AddCert(c)
	}
	if _, err := leaf.Verify(x509.VerifyOptions{
		Roots:         roots,
		Intermediates: inter,
		CurrentTime:   at,
		// EKU is checked per profile below; x509's own EKU nesting rule is
		// written for TLS and would reject the ISO mdlDS EKU outright.
		KeyUsages: []x509.ExtKeyUsage{x509.ExtKeyUsageAny},
	}); err != nil {
		return nil, fmt.Errorf("certificate %q does not chain to a trusted anchor: %w", leaf.Subject, err)
	}
	switch profile {
	case ProfileGeneric:
	case ProfileMdocDS:
		if err := checkMdocDS(leaf); err != nil {
			return nil, err
		}
	default:
		return nil, fmt.Errorf("unknown chain profile %q", profile)
	}
	return leaf, nil
}

func checkMdocDS(leaf *x509.Certificate) error {
	if leaf.IsCA {
		return fmt.Errorf("document signer %q is marked CA", leaf.Subject)
	}
	if leaf.KeyUsage&x509.KeyUsageDigitalSignature == 0 {
		return fmt.Errorf("document signer %q lacks keyUsage digitalSignature", leaf.Subject)
	}
	if !slices.ContainsFunc(leaf.UnknownExtKeyUsage, oidMdlDS.Equal) {
		return fmt.Errorf("document signer %q lacks the mdlDS extended key usage %s", leaf.Subject, oidMdlDS)
	}
	return nil
}
