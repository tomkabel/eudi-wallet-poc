# External carrier attempt

Status: BLOCKED
Detail: External preparation unavailable: child exceeded 120.0s timeout
Date: 2026-11-11T21:02:53.071736&#43;00:00
Target: Multipaz sample wallet 0.99.0
Source: https://github.com/openwallet-foundation/multipaz.git
Canonical commit: 7486ddbca490038c3580513d8273b9bbf18c6b70
Observed commit: 7486ddbca490038c3580513d8273b9bbf18c6b70
Selection: Existing carrier fixtures derive from Multipaz 0.99.0 ZK serialization. Its public sample wallet provides an attributable independent implementation target; fixture provenance establishes no external compatibility.
Host: &#123;&#34;hostname&#34;: &#34;cachy&#34;, &#34;platform&#34;: &#34;Linux-7.3.3-1-cachyos-x86&#95;64-with-glibc2.44&#34;, &#34;python&#34;: &#34;3.14.9&#34;&#125;
Evidence scope: Independent implementation attempt by repository agent; not independent operator evidence or certification.
Publication: PREPARED&#95;IN&#95;REPOSITORY; public publication and independent reproduction remain outstanding

## Observed suite provenance

~~~json
{
  "observed_at": "2026-11-11T21:02:53.083959+00:00",
  "revision": "62ccf9292cac0d1e2633a2ee2050b47d859d3337",
  "dirty": true,
  "executions": [
    {
      "name": "suite-revision",
      "status": "PASS",
      "command": [
        "git",
        "rev-parse",
        "HEAD"
      ],
      "returncode": 0,
      "output": "62ccf9292cac0d1e2633a2ee2050b47d859d3337\n",
      "detail": "",
      "cwd": "/home/notroot/Documents/eudi-wallet-poc"
    },
    {
      "name": "suite-cleanliness",
      "status": "PASS",
      "command": [
        "git",
        "status",
        "--porcelain",
        "--untracked-files=all"
      ],
      "returncode": 0,
      "output": " M Makefile\n M _bmad-output/implementation-artifacts/spec-2-4-external-carrier-run-and-published-result.md\n M tests/run_carrier_conformance.py\n M tests/test_carrier_conformance.py\n",
      "detail": "",
      "cwd": "/home/notroot/Documents/eudi-wallet-poc"
    }
  ],
  "sha256": {
    "tests/run_external_carrier.py": "2fcf254874933c741093e13678db0fe25b87e7230cfe8ee1059f8ece7980989b",
    "tests/run_carrier_conformance.py": "bee07c986c16dcd34bf6f7e3ce09f37bbf1cc2cd6777108a8d0db27ea2111e40",
    "tests/e2e.sh": "f51d6783c86f748c1ab24fa701d1625cbfc3bd2d5fc55b2041848afd923568aa",
    "docs/profiles/carrier/external-target.json": "f031f87726a3c408481a393cbaa5ddb698ad7fbef5f149f92e35a3b71f40b717",
    "tests/vectors/carrier-v1/device_response.cbor": "23978eb511a13d4a38a743260d5ef44a7fd9488d47806c33631fe092adc7c4aa",
    "tests/vectors/carrier-v1/device_response.cbor.b64": "507ebf7ad0a5c726a54ea42e63c774fb9ab093c480af1cf13712dca05cf523e3",
    "tests/vectors/carrier-v1/device_response.json.b64": "0137786051da14b4d37cffa4ed547c1762080893827ef966e4f0fb5927bbecb5",
    "tests/vectors/carrier-v1/device_response_transcript.bin": "544b2287e816c6bdd4cfb9c666672014c7774f18ec09880eabe8f46032a5aaa6",
    "tests/vectors/carrier-v1/manifest.json": "9405fc1bff0d9029049a07bf78fc647e4d61f36033a70ef5f230587cfa28eca4",
    "tests/vectors/carrier-v1/request.json": "4586ada2b2833fa5f3b20de5d4d2720dacd7ec3a5d1f6944cc5d96e4144b5e83"
  }
}
~~~

## Executed commands

### checkout-revision
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: git rev-parse HEAD
Exit: 0; disposition: PASS
Detail:

~~~text
7486ddbca490038c3580513d8273b9bbf18c6b70

~~~
### checkout-origin
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: git remote get-url origin
Exit: 0; disposition: PASS
Detail:

~~~text
https://github.com/openwallet-foundation/multipaz.git

~~~
### checkout-cleanliness
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: git status --porcelain --untracked-files&#61;all
Exit: 0; disposition: PASS
Detail:

~~~text

~~~
### preparation-1
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: adb devices -l
Exit: 0; disposition: PASS
Detail:

~~~text
List of devices attached
c482d41b               device usb:1-4.4 product:CPH2609EEA model:CPH2609 device:OP5D35L1 transport_id:1


~~~
### preparation-2
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Command: ./gradlew --no-daemon --console&#61;plain :samples:testapp:assembleDebug
Exit: 143; disposition: ERROR
Detail: child exceeded 120.0s timeout

~~~text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
Type-safe project accessors is an incubating feature.

> Configure project :
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build.gradle.kts:25:17: 'exec(Action<in ExecSpec!>!): ExecResult!' is deprecated. Deprecated in Java
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build.gradle.kts:43:17: 'exec(Action<in ExecSpec!>!): ExecResult!' is deprecated. Deprecated in Java
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build.gradle.kts:47:19: 'toString(): String' is deprecated. Deprecated in Java

> Configure project :multipaz
Skipping artifact signing for multipaz: No signing keys configured.

[runner terminated child: child exceeded 120.0s timeout]

~~~

## Case inventory

| Case | Carriers | Mode | Property | Result |
|---|---|---|---|---|
| 1 | interim-json | direct&#95;post | not-applicable | NOT&#95;EXECUTED |
| 2 | interim-json | direct&#95;post | nonce | NOT&#95;EXECUTED |
| 3 | interim-json | direct&#95;post | single-use | NOT&#95;EXECUTED |
| 4 | interim-json | direct&#95;post | predicate-value | NOT&#95;EXECUTED |
| 5 | none expected | direct&#95;post | not-applicable | NOT&#95;EXECUTED |
| 6 | none expected | direct&#95;post.jwt | not-applicable | NOT&#95;EXECUTED |
| 7 | interim-json | direct&#95;post.jwt | not-applicable | NOT&#95;EXECUTED |
| 8 | none expected | direct&#95;post.jwt | not-applicable | NOT&#95;EXECUTED |
| 9 | none expected | direct&#95;post.jwt | not-applicable | NOT&#95;EXECUTED |
| 10 | none expected | direct&#95;post | not-applicable | NOT&#95;EXECUTED |
| 11 | none expected | direct&#95;post.jwt | not-applicable | NOT&#95;EXECUTED |
| 12 | interim-json, interim-json | direct&#95;post | not-applicable | NOT&#95;EXECUTED |
| 13 | none expected | direct&#95;post | not-applicable | NOT&#95;EXECUTED |
| 14 | none expected | direct&#95;post | not-applicable | NOT&#95;EXECUTED |
| 15 | none expected | direct&#95;post | not-applicable | NOT&#95;EXECUTED |
| 16 | interim-json | direct&#95;post.jwt | client&#95;id | NOT&#95;EXECUTED |
| 17 | interim-json | direct&#95;post.jwt | response&#95;uri | NOT&#95;EXECUTED |
| 18 | interim-json | direct&#95;post.jwt | jwk&#95;thumbprint | NOT&#95;EXECUTED |
| 19 | mso-mdoc-zk-cbor | direct&#95;post | not-applicable | NOT&#95;EXECUTED |

## Carrier vectors

- interim-json: NOT&#95;EXECUTED; external adapter has not executed
- mso-mdoc-zk-cbor: NOT&#95;EXECUTED; external adapter has not executed

## Limitations and remaining operator steps

- Local Go results, cached fixtures, upstream self-tests and builds are not external assertions.
- No device, independent operator, certification, ZKP policy or circuit security claim is established.
- Build the pinned sample wallet and connect a supported device/emulator.
- Implement an adapter executing all 19 assertions and both carrier vectors against that target.
- Rerun with --adapter-command; inspect evidence, arrange maintainer publication and independent operator reproduction.

Markdown presentation whitespace is normalized; report.json retains original command-output bytes.
