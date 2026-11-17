# External carrier attempt

Status: **BLOCKED** — External preparation unavailable: child exceeded 120.0s timeout

Date: 2026-11-11T12:13:38.415502+00:00
Target: Multipaz sample wallet 0.99.0
Source: https://github.com/openwallet-foundation/multipaz.git
Canonical commit: 7486ddbca490038c3580513d8273b9bbf18c6b70
Observed commit: 7486ddbca490038c3580513d8273b9bbf18c6b70
Selection: Existing carrier fixtures derive from Multipaz 0.99.0 ZK serialization. Its public sample wallet provides an attributable independent implementation target; fixture provenance establishes no external compatibility.
Host: {"hostname": "cachy", "platform": "Linux-7.3.3-1-cachyos-x86_64-with-glibc2.44", "python": "3.14.9"}

Independent implementation attempt by repository agent; not independent operator evidence or certification.

PREPARED_IN_REPOSITORY; public publication and independent reproduction remain outstanding

## Executed commands

### checkout-revision
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: git rev-parse HEAD
Exit: 0; disposition: PASS;

~~~text
7486ddbca490038c3580513d8273b9bbf18c6b70

~~~

### checkout-origin
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: git remote get-url origin
Exit: 0; disposition: PASS;

~~~text
https://github.com/openwallet-foundation/multipaz.git

~~~

### checkout-cleanliness
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: git status --porcelain --untracked-files=all
Exit: 0; disposition: PASS;

~~~text

~~~

### preparation-1
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: adb devices -l
Exit: 0; disposition: PASS;

~~~text
List of devices attached


~~~

### preparation-2
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: ./gradlew --no-daemon --console=plain :samples:testapp:assembleDebug
Exit: 143; disposition: ERROR; child exceeded 120.0s timeout

~~~text
Downloading https://services.gradle.org/distributions/gradle-8.14.2-bin.zip
.............10%.............20%.............30%.............40%.............50%.............60%.............70%.............80%.............90%..............100%

Welcome to Gradle 8.14.2!

Here are the highlights of this release:
 - Java 24 support
 - GraalVM Native Image toolchain selection
 - Enhancements to test reporting
 - Build Authoring improvements

For more details see https://docs.gradle.org/8.14.2/release-notes.html

To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 

[runner terminated child: child exceeded 120.0s timeout]

~~~

## Case inventory

| Case | Carriers | Mode | Property | Result |
|---|---|---|---|---|
| 1 | interim-json | direct_post | not-applicable | NOT_EXECUTED |
| 2 | interim-json | direct_post | nonce | NOT_EXECUTED |
| 3 | interim-json | direct_post | single-use | NOT_EXECUTED |
| 4 | interim-json | direct_post | predicate-value | NOT_EXECUTED |
| 5 | none expected | direct_post | not-applicable | NOT_EXECUTED |
| 6 | none expected | direct_post.jwt | not-applicable | NOT_EXECUTED |
| 7 | interim-json | direct_post.jwt | not-applicable | NOT_EXECUTED |
| 8 | none expected | direct_post.jwt | not-applicable | NOT_EXECUTED |
| 9 | none expected | direct_post.jwt | not-applicable | NOT_EXECUTED |
| 10 | none expected | direct_post | not-applicable | NOT_EXECUTED |
| 11 | none expected | direct_post.jwt | not-applicable | NOT_EXECUTED |
| 12 | interim-json, interim-json | direct_post | not-applicable | NOT_EXECUTED |
| 13 | none expected | direct_post | not-applicable | NOT_EXECUTED |
| 14 | none expected | direct_post | not-applicable | NOT_EXECUTED |
| 15 | none expected | direct_post | not-applicable | NOT_EXECUTED |
| 16 | interim-json | direct_post.jwt | client_id | NOT_EXECUTED |
| 17 | interim-json | direct_post.jwt | response_uri | NOT_EXECUTED |
| 18 | interim-json | direct_post.jwt | jwk_thumbprint | NOT_EXECUTED |
| 19 | mso-mdoc-zk-cbor | direct_post | not-applicable | NOT_EXECUTED |

## Carrier vectors

- interim-json: NOT_EXECUTED; external adapter has not executed
- mso-mdoc-zk-cbor: NOT_EXECUTED; external adapter has not executed

## Limitations and remaining operator steps

- Local Go results, cached fixtures, upstream self-tests and builds are not external assertions.
- No device, independent operator, certification, ZKP policy or circuit security claim is established.
- adb devices -l reported no connected devices; no sample wallet device execution or external assertion adapter was available.
- Build the pinned sample wallet and connect a supported device/emulator.
- Implement an adapter executing all 19 assertions and both carrier vectors against that target.
- Rerun with --adapter-command; inspect evidence, arrange maintainer publication and independent operator reproduction.

Public source resolution and clone command are retained in [source-resolution.json](source-resolution.json).
