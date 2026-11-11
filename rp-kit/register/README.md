# Shared synthetic registrations

`synthetic-v1.json` is version `synthetic-rp-register/1`. It contains exactly
three positive RPs: `x509_san_dns:verifier.example.ee`, `x509_san_dns:rp.example.ee`
and `x509_san_dns:localhost`, each for service `age-verification`. Their names,
identities and intended uses are invented local test data, with no authority
registration claim. The omitted `x509_san_dns:omitted.example` identity is the
negative control: no negative record may be added to represent absence.

Both supported fixture doctypes use their doctype as namespace. Each RP allows
age_over_16, age_over_18, age_over_21 and age_over_65 for those doctypes. The
privacy policy is explicitly absent (`null`), never blank.

Story 4.6's certificate generator should consume this same corpus, translating
its six-field registration without widening its uses. No certificate fixtures
or generator are claimed to exist by this story. A new profile/version must be
reviewed explicitly; the mock refuses unknown corpus versions.
