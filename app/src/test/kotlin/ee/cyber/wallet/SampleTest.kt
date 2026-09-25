package ee.cyber.wallet

/**
 * Testing L2 (E1 rider): the dead commented-out integration test (and its unused
 * [rootCertificate] PEM, session-shape data class, HTTP helper and httpLogger) were pruned —
 * it required a locally running test-rp (the `@Ignore`d shape), exercised nothing in CI, and
 * only duplicated what e2e.sh covers against the live stack. The end-to-end walk (RP session
 * create → authorization request → credential presentation → verification) is the integration
 * battery's job, not a unit test's; the two `@Ignore`d integration tests in this module stay
 * the pointer until the test-rp make target exists (Deferred, testing L3).
 *
 * This file intentionally holds no code: it survives as the anchor for the deferred
 * integration-test work so the module's test inventory keeps its documented placeholder.
 */
