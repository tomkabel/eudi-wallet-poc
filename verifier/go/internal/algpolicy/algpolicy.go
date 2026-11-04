// Package algpolicy enforces the shared certificate algorithm policy (PKI plan
// NEW-S1-2). The policy file is the same bytes the Android wallet packages as a
// Java resource, so both engines refuse the same algorithms.
package algpolicy

import (
	"crypto/ecdsa"
	"crypto/rsa"
	"crypto/x509"
	"crypto/x509/pkix"
	_ "embed"
	"encoding/asn1"
	"encoding/json"
	"fmt"
	"slices"
)

//go:embed policy/algorithms.json
var policyJSON []byte

// Policy is the parsed policy file.
type Policy struct {
	Version       int      `json:"version"`
	X509SigAlgs   []string `json:"x509_signature_algorithms"`
	JWSAlgs       []string `json:"jws_algorithms"`
	ECCurves      []string `json:"ec_curves"`
	RSAMinBits    int      `json:"rsa_min_bits"`
	ReservedLater []string `json:"reserved_for_later"`
}

// Default is the embedded policy. A file that does not parse is a build defect,
// so this panics at init rather than starting a verifier with no policy.
var Default = mustParse(policyJSON)

func mustParse(b []byte) Policy {
	var p Policy
	if err := json.Unmarshal(b, &p); err != nil {
		panic("algpolicy: embedded policy does not parse: " + err.Error())
	}
	if len(p.X509SigAlgs) == 0 || len(p.ECCurves) == 0 || p.RSAMinBits <= 0 {
		panic("algpolicy: embedded policy is incomplete")
	}
	return p
}

var curveOIDs = map[string]string{
	"P-224": "1.3.132.0.33",
	"P-256": "1.2.840.10045.3.1.7",
	"P-384": "1.3.132.0.34",
	"P-521": "1.3.132.0.35",
}

// CheckCertificate refuses a certificate whose signature algorithm or public
// key the policy does not list.
func (p Policy) CheckCertificate(c *x509.Certificate) error {
	sigOID, err := signatureOID(c.Raw)
	if err != nil {
		return err
	}
	if !slices.Contains(p.X509SigAlgs, sigOID) {
		return fmt.Errorf("certificate %q: signature algorithm %s is not in the algorithm policy", c.Subject, sigOID)
	}
	switch k := c.PublicKey.(type) {
	case *ecdsa.PublicKey:
		oid := curveOIDs[k.Curve.Params().Name]
		if !slices.Contains(p.ECCurves, oid) {
			return fmt.Errorf("certificate %q: curve %s is not in the algorithm policy", c.Subject, k.Curve.Params().Name)
		}
	case *rsa.PublicKey:
		if k.N.BitLen() < p.RSAMinBits {
			return fmt.Errorf("certificate %q: RSA-%d is below the policy minimum of %d bits", c.Subject, k.N.BitLen(), p.RSAMinBits)
		}
	default:
		return fmt.Errorf("certificate %q: public key type %T is not in the algorithm policy", c.Subject, c.PublicKey)
	}
	return nil
}

// CheckJWSAlg refuses a JWS alg the policy does not list.
func (p Policy) CheckJWSAlg(alg string) error {
	if !slices.Contains(p.JWSAlgs, alg) {
		return fmt.Errorf("JWS alg %q is not in the algorithm policy", alg)
	}
	return nil
}

// signatureOID reads the outer signatureAlgorithm OID from the DER, because
// crypto/x509 maps unknown and legacy algorithms to one Unknown value.
func signatureOID(der []byte) (string, error) {
	var cert struct {
		TBS     asn1.RawValue
		SigAlg  pkix.AlgorithmIdentifier
		SigBits asn1.BitString
	}
	if _, err := asn1.Unmarshal(der, &cert); err != nil {
		return "", fmt.Errorf("certificate does not parse as DER: %w", err)
	}
	return cert.SigAlg.Algorithm.String(), nil
}
