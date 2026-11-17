# ANNEX A - Age Verification Profile
(normative)

## A.1. Introduction

This document defines a set of requirements for the existing specifications to
enable interoperability among Attestation Providers (APs), Age Verification App Instances (AVIs)
and Relying Parties (RPs) where a substantial level of security and privacy is required.
This document is an interoperability profile that can be used by implementations
in various contexts, be it a certain industry or a certain regulatory environment.

This profile is part of the EU Age Verification technical specifications. It builds
on existing specifications commonly used for credential issuance and verification
and defines a set of features to be implemented in the context of age verification

This document outlines a profile for the age verification target architecture,
serving as a conceptual framework rather than a comprehensive specification of all
features and requirements. At the current stage of development, not all functionalities
described herein have been implemented in the Age Verification Solution Toolbox.
Furthermore, certain features are also absent in the present development state of
the EUDI Wallet libraries. These functionalities will be developed and integrated
incrementally over the course of the project.

The profile uses OpenID for Verifiable Credential Issuance [OID4VCI] for issuance
of Proof of Age attestations. Furthermore, for the presentation of Proof of Age
attestations, the profile uses the W3C Digital Credentials API [W3C Digital Credentials API] as specified in
\[ISO/IEC 18013-7\], Annex C, as well as OpenID for Verifiable Presentations [OID4VP]
as a fallback mechanism. The attestation format used is ISO mDoc [ISO18013-5].

A full list of the open standards used in this profile can be found in Overview of
the Open Standards Requirements.

### Target audience/Usage

The intended audience for this document comprises Member States and designated
organizations that seek to implement an age verification solution. This profile,
the referenced technical standards and accompanying guidelines are designed to
support governmental authorities as well as commissioned organizations in the
development, integration, and deployment of compliant age verification systems
within their respective jurisdictions or operational domains. This approach ensures
that both public sector bodies and their authorized private sector partners are
equipped to realize and maintain robust, privacy-preserving age verification in
alignment with European regulatory requirements, age verification digital services
requirements, and digital identity initiatives.

## A.2. Terminology
This specification uses the terms
Attestation Providers (APs), Age Verification App Instances (AVIs), Relying Parties (RPs), Proof of Age attestation, and AVS Unit Attestation (AVSUA) as defined in [Operational, Security, Product, and Architecture Specifications](../../../docs/architecture-and-technical-specifications.md).


## A.3. Scope
The primary focus of this document is on online services, with the intention that
age verification should be conducted digitally in online environments. The solution
is specifically designed to enable users to prove their age when accessing online
content, products, or services that are subject to age restrictions. This approach
addresses the increasing need for robust online age verification mechanisms,
ensuring that age-restricted content is accessed only by individuals who meet
the required legal age, and supporting compliance with relevant regulatory
requirements for online platforms.

The following aspects are in scope of this interoperability profile:

- Profile of OpenID4VCI to issue ISO mDoc Proof of Age attestations
- Profile of OpenID4VP to present ISO mDoc Proof of Age attestation
- Crypto Suites
- Data model examples

The following assumptions are made:

- The APs and RPs cannot pre-discover Age Verification App’s capability
- The AP is talking to the AVI supporting the features defined in this profile (via AVI invocation mechanism)
- Support for mDoc only as the Proof of Age attestation format
- No personal data, especially no information from personal identification documents
such as national ID card, is stored within an AVI. Only the Proof of Age attestation,
specifically indicating "older than 18", is utilized for age verification purposes

### Out of Scope
The following items are out of scope for the current version of this document, but might be added in future versions:

- Support for Level of Assurance (LoA) "high".
- Proof of Age attestation re-issuance (using refresh tokens) and revocation.
- RP registration and trusted lists of RPs.
- Trusted lists of Age Verification App providers under the envisaged EU Age Verification Scheme (the interim publication of provider certificates by the Commission is described in the Operational, Security, Product, and Architecture Specifications, Section 4.2).
- Proof of Age attestation presentation using proximity.
- Profile of OpenID4VCI to issue ISO mDoc [ISO.18013-5] is defined in ISO 23220-3.


### Standards Requirements
This specification enables interoperable implementations of the following flows:

- Issuance of ISO mDoc Proof of Age attestation using OpenID4VCI
- Presentation of ISO mDoc Proof of Age attestation using OpenID4VP
Implementations of this specification do not have to implement all of the flows listed above.
A parameter that is listed as optional to be implemented in a specification that is
being profiled (i.e., OpenID4VCI, OpenID4VP and ISO mDoc) remains optional unless it is stated otherwise in this specification.

## A.4 Proof of Age attestation Data Model
The data model for the Proof of Age attestation is defined as a profile of
the attribute schema specified in \[ISO/IEC 18013-5\]
and \[ISO/IEC 23220-2\]. A Proof of Age attestation SHALL comply with this data model.

#### A.4.1 Document type
The document type for Proof of Age attestation SHALL be `eu.europa.ec.av.1`.

#### A.4.2 Attribute set
The Proof of Age attestation attribute set SHALL be composed of attributes defined in the following
Table. All attributes belong to namespace `eu.europa.ec.av.1`

| Identifier | Meaning | Presence in issuance | Presence in verification | Encoding format |
| :---- | :---- | :---- | :---- | :---- |
| age\_over\_18 | This attribute is present in all Proof of Age attestations and indicates whether the user is above 18\. | Mandatory | Conditional mandatory (either age\_over\_18 or one of age\_over\_NN attributes SHALL be requested) | bool |
| age\_over\_NN | Confirming whether the Proof of Age attestation user is currently over NN years of age. Multiple entries MAY be provided as separate attributes. | Optional | Conditional mandatory (either age\_over\_18 or one of age\_over\_NN attributes SHALL be requested)  | bool |

A Proof of Age Attestation SHALL NOT include any other attribute.

**Note on Age Attribute Scope**

While the technical architecture and data model (based on \[ISO/IEC 18013-5\]
and \[ISO/IEC 23220-2\]) inherently support the age\_over\_NN attribute schema
for arbitrary age thresholds (e.g., age\_over\_14, age\_over\_65), this
specification explicitly focuses on age\_over\_18 to address the most common
regulatory requirement for age-restricted online services in the EU.

Extending the solution to additional age thresholds would require:

1. Validation of enrolment methods for the target age group.

2. Alignment with jurisdictional legal frameworks governing the specific age-based restrictions.

Section 7.2.5 \[ISO/IEC 18013-5\] of specification 'Age attestation: nearest “true”
attestation above request' may be considered in implementations making use of
the optional age_over_NN attribute.

## A.5. Proof of Age Attestation Issuance
Issuance of Proof of Age attestations SHOULD follow the issuance framework
defined for the EUDI Wallet in the [EUDI Wallet ARF](https://eu-digital-identity-wallet.github.io/eudi-doc-architecture-and-reference-framework/),
which is based on OpenID for Verifiable Credential Issuance. 

An Attestation Provider and an Age Verification App Provider MAY instead agree on an internal
issuance interface (for example, where both roles are performed by the same or
affiliated entities), provided that it achieves equivalent assurance, in
particular regarding the verification of the legitimacy of the requesting AVI
(see AVS Unit Attestation below).

#### Credential Offer
- As a way to invoke the AVI, at least a custom URL scheme av-vci:// MUST be supported.

### AVS Unit Attestation

When following the EUDI Wallet issuance framework, the AVI SHOULD present an AVS Unit
Attestation (AVSUA) to the AP during issuance. The AVSUA comprises an Age Verification App
Instance Attestation (AVIA), attesting to the integrity of the AVI, and a Key Attestation
(KA), attesting to the security of the keys the AVI uses in the issuance flow. Both are
issued and signed by the provider of the Age Verification solution, which performs for the
AVI the role that [TS3] assigns to the Wallet Provider.

The AVSUA is the Age Verification instantiation of the Wallet Unit Attestation mechanism
specified in [TS3]: the AVIA corresponds to the Wallet Instance Attestation (WIA) and the KA
to the Key Attestation of that specification. Implementations MAY reuse Wallet Unit
Attestation implementations for this purpose. The [ARF] provides the wider context.

Note (informative): on the trust anchor for AVSUA validation and its evolution under the
envisaged EU Age Verification Scheme, see Section 3.4.4 of the Operational, Security,
Product, and Architecture Specifications.

## A.6. Proof of Age attestation presentation
Presentation involves two choices: the presentation mechanism and the transport. The two are not independent, since the Zero-Knowledge Proof mechanism is available only over the W3C Digital Credentials API transport.

- **Presentation mechanism.** The preferred mechanism is a Zero-Knowledge Proof defined in Section A.8. A plain ISO mDoc presentation SHALL be used as the fallback where, and only where, either (a) the User's device does not support Zero-Knowledge Proof generation, or (b) the OpenID for Verifiable Presentations transport is used.

- **Transport.** The default transport is the W3C Digital Credentials API. OpenID for Verifiable Presentations is used as a transport fallback when the Digital Credentials API is not available.

### W3C Digital Credentials API
Presentation as a Zero-Knowledge Proof over the W3C Digital Credentials API SHALL follow Section A.8, which specifies the normative request and response format by reference; a non-normative example is provided in Section A.11 ("W3C Digital Credentials API with ZKP"). The plain ISO mDoc presentation described below is the fallback mechanism, used under the conditions set out at the beginning of this section.
The W3C Digital Credentials API is used as specified in \[ISO/IEC 18013-7\], Annex C.

#### Request
The Relying Party builds a request which consists of two parts: `encryptionInfo`
and `deviceRequest`.

**Encryption info**
`encryptionInfo` is the base64-url without padding encoding of the cbor
encoded `EncryptionInfo` object defined below

```cddl
EncryptionInfo = [
  "dcapi",
  EncryptionParameters
]

EncryptionParameters = {
  "nonce" : bstr,
  "recipientPublicKey" : COSE_Key
}
```

**Device Request**
`deviceRequest` is the base64-url without padding encoding of the cbor encoded
`DeviceRequest` defined in \[ISO/IEC 18013-5\] §8.3.2.1.2.1

#### Response
The response is the base64-url without padding encoding of the cbor
encoded `EncryptedResponse` object defined below:

```cddl
EncryptedResponse = [
  "dcapi",
  EncryptedResponseData
]
EncryptedResponseData = {
  "enc" : bstr,
  "cipherText" : bstr
}
```

`enc` is the public key used for the generation of the ciphertext, which is used
in the decryption process, and `cipherText` is the encrypted response, which is
a `DeviceResponse`, defined in Section 8.3.2.1.2.3 of ISO/IEC 18013-5, encrypted
using Hybrid Public Key Encryption (HPKE) defined in RFC 9180.

#### Reader Authentication
Reader authentication is not required and therefore is out of scope of this profile

### OpenID for Verifiable Presentations profile Requirements
OpenID for Verifiable Presentations is used as a transport fallback when W3C
Digital Credentials API is not available. Zero-Knowledge Proofs are not supported over OpenID for Verifiable Presentations in this version of the profile, since no standardized DCQL query for requesting Zero-Knowledge Proof is available. This transport is for ISO mDoc format attestations only. This transport will be revisited once the relevan standardisation work has concluded.

- As a way to invoke the Age Verification App, at least a custom URL scheme av:// MUST be supported.
- Response type MUST be `vp_token`
- `response_mode` MUST be `direct_post`
- RP MUST send the request as URL-encoded query parameters. The `request` and `request_uri` parameters MUST NOT be used (i.e., support for JAR is not required)
- The client identifier scheme MUST be `redirect_uri` followed by the `response_uri`
- A request MUST specify the nonce parameter
- The DCQL query and response as defined in Section 6 of [OID4VP] MUST be used
- The `state` parameter as defined in Section 4.1.1 of [OID4VP] is optional

#### Client Authentication
Client authentication is not required and is therefore out of scope of this profile.


## A.7. Crypto Suites
All entities MUST support P-256 (secp256r1) as a key type with ES256 JWT algorithm for signing and signature validation
whenever this profile requires to do so.
SHA256 MUST be supported by all the entities as the hash algorithm to generate and validate the digests in the MDOC VC.
Note: When using this profile with other cryptosuites, it is recommended to be explicit about which entity is required
to support which curve for signing and/or signature validation.

## A.8. Zero-Knowledge Proofs
Where the User's device provides the necessary platform support, the AVI SHALL support the generation of Zero-Knowledge Proofs using the solution detailed in: *"Matteo Frigo and abhi shelat, Anonymous credentials from ECDSA,
IACR Communications in Cryptology, vol. 3, no. 1, 2026, available at [https://doi.org/10.62056/a3qjmpgxq](https://doi.org/10.62056/a3qjmpgxq)"* [Fri2024].
An open-source implementation of this solution has been released [here](https://github.com/google/longfellow-zk). A Zero-Knowledge Proof is the preferred mechanism for presenting a Proof of Age attestation. It is requested and returned over the W3C Digital Credentials API.

For this version of the profile, the only Zero-Knowledge Proof system in scope is the system identified by `longfellow-libzk-v1`, as defined in [Fri2024] and specified in [draft-google-cfrg-libzk](https://datatracker.ietf.org/doc/draft-google-cfrg-libzk/), using the circuit and parameters identified by the ZkSystemSpec parameters (`circuit_hash`, `version`, `num_attributes`, `block_enc_hash`, `block_enc_sig`) as defined in ISO/IEC DIS 18013-5 (Second Edition), Section 10.2.7. An implementation SHALL use that system. Support for any other Zero-Knowledge Proof system does not constitute conformance with this profile. The system identifier is versioned, and future versions of this profile may add or replace the systems in scope. The set of circuits accepted for the purposes of this profile, identified by their circuit hashes, is published and maintained by the scheme owner separately from this document.

The request and response structure of a Zero-Knowledge Proof presentation SHALL follow the ZkSystemSpec mechanism defined in ISO/IEC 18013-5 (Second Edition), Section 10.2.7, transported over the W3C Digital Credentials API as specified in ISO/IEC 18013-7, Annex C. The proof format SHALL follow [draft-google-cfrg-libzk]; a non-normative example is provided in Section A.11 ("W3C Digital Credentials API with ZKP").

Such a proof SHALL demonstrate that:

- The Proof of Age attestation includes a signature that can be verified using the public key of the AP
- The Proof of Age attestation includes the requested attribute and its value is true
- The AVI can generate a signature of the nonce that is verifiable using the
public key included in the Proof of attestation
- The Proof of Age attestation is within its validity period

Verification requirements:
- An RP SHALL be able to verify these Zero-Knowledge Proofs.
- An RP SHALL verify that the Zero-Knowledge Proof was generated using an accepted circuit, by verifying the circuit hash against the set of circuits accepted for the purposes of this profile, before verifying the proof. An RP SHALL reject a presentation generated using a circuit that is not among the accepted circuits.
- Where the User's device does not support Zero-Knowledge Proof generation, the AVI SHALL fall back to the plain ISO mDoc presentation defined in Section A.6. The RP SHALL be able to verify both a Zero-Knowledge Proof presentation and the plain ISO mDoc fallback presentation.


## A.9. Security Considerations
The security considerations in [OID4VCI] and [OID4VP] apply when compatible with Level of Assurance substantial only.

Because Relying Parties cannot pre-discover the capabilities of an Age Verification App Instance, a Relying Party cannot distinguish a User device that lacks Zero-Knowledge Proof support from a presentation that has been downgraded to the fallback mechanism. A Relying Party SHALL NOT reject a presentation solely on the ground that it uses the plain ISO mDoc fallback mechanism. Note that the plain ISO mDoc presentation does not provide the unlinkability properties of the Zero-Knowledge Proof mechanism (see Annex B); the residual linkability risk is mitigated by the batch issuance and single-use requirements of the main specification.

