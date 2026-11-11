# Later source and host diagnostics supplement

Observed: 2026-11-11T12:28:24.398753+00:00

Later observations only. This supplement does not recreate the original build observation or establish its historical suite/toolchain state. Original report.json, README.md and source-resolution.json remain unchanged.

Original build remains BLOCKED. These commands are diagnostics and a separate clone rerun; no carrier assertions ran.

## bounded-public-clone-rerun

Command: git clone --depth 1 --branch 0.99.0 https://github.com/openwallet-foundation/multipaz.git /tmp/story24-clone-supplement-p3b79ga3/multipaz
CWD: /tmp
Started: 2026-11-11T12:28:24.429422+00:00
Finished: 2026-11-11T12:28:42.541366+00:00
Elapsed seconds: 18.111955607999334
Exit: 0; PASS
Detail:

~~~text
Cloning into '/tmp/story24-clone-supplement-p3b79ga3/multipaz'...
warning: refs/tags/0.99.0 e8a55fb5f9083d433ed69ca45e4faa16edeccafb is not a commit!
Note: switching to '7486ddbca490038c3580513d8273b9bbf18c6b70'.

You are in 'detached HEAD' state. You can look around, make experimental
changes and commit them, and you can discard any commits you make in this
state without impacting any branches by switching back to a branch.

If you want to create a new branch to retain commits you create, you may
do so (now or later) by using -c with the switch command. Example:

  git switch -c <new-branch-name>

Or undo this operation with:

  git switch -

Turn off this advice by setting config variable advice.detachedHead to false
~~~
## supplement-clone-revision

Command: git rev-parse HEAD
CWD: /tmp/story24-clone-supplement-p3b79ga3/multipaz
Started: 2026-11-11T12:28:42.541419+00:00
Finished: 2026-11-11T12:28:42.544226+00:00
Elapsed seconds: 0.002813831000821665
Exit: 0; PASS
Detail:

~~~text
7486ddbca490038c3580513d8273b9bbf18c6b70
~~~
## host-java-version

Command: java -version
CWD: /tmp
Started: 2026-11-11T12:28:42.544247+00:00
Finished: 2026-11-11T12:28:42.606993+00:00
Elapsed seconds: 0.06276494800113142
Exit: 0; PASS
Detail:

~~~text
openjdk version "21.0.12.1" 2026-08-18
OpenJDK Runtime Environment (build 21.0.12.1+1)
OpenJDK 64-Bit Server VM (build 21.0.12.1+1, mixed mode, sharing)
~~~
## host-sdk-diagnostics

Command: /home/notroot/Documents/eudi-wallet-poc/.venv/bin/python -c &#39;import os,pathlib,shutil; print&#40;&#34;java&#61;&#34;&#43;str&#40;shutil.which&#40;&#34;java&#34;&#41;&#41;&#41;; print&#40;&#34;adb&#61;&#34;&#43;str&#40;shutil.which&#40;&#34;adb&#34;&#41;&#41;&#41;; print&#40;&#34;sdkmanager&#61;&#34;&#43;str&#40;shutil.which&#40;&#34;sdkmanager&#34;&#41;&#41;&#41;; print&#40;&#34;&#92;n&#34;.join&#40;k&#43;&#34;&#61;&#34;&#43;str&#40;os.environ.get&#40;k&#41;&#41; for k in &#40;&#34;JAVA&#95;HOME&#34;,&#34;ANDROID&#95;HOME&#34;,&#34;ANDROID&#95;SDK&#95;ROOT&#34;&#41;&#41;&#41;; roots&#61;&#123;os.environ.get&#40;&#34;ANDROID&#95;HOME&#34;&#41;,os.environ.get&#40;&#34;ANDROID&#95;SDK&#95;ROOT&#34;&#41;,&#34;/opt/android-sdk&#34;,&#34;/opt/android-sdk-linux&#34;&#125;; print&#40;&#34;&#92;n&#34;.join&#40;str&#40;p&#41;&#43;&#34;: &#34;&#43;str&#40;sorted&#40;x.name for x in p.iterdir&#40;&#41;&#41; if p.is&#95;dir&#40;&#41; else &#34;absent&#34;&#41; for p in &#91;pathlib.Path&#40;r&#41; for r in roots if r&#93;&#41;&#41;&#39;
CWD: /tmp
Started: 2026-11-11T12:28:42.607038+00:00
Finished: 2026-11-11T12:28:42.666343+00:00
Elapsed seconds: 0.059315358999810996
Exit: 0; PASS
Detail:

~~~text
java=/usr/bin/java
adb=/usr/bin/adb
sdkmanager=/opt/android-sdk/cmdline-tools/latest/bin/sdkmanager
JAVA_HOME=None
ANDROID_HOME=/opt/android-sdk
ANDROID_SDK_ROOT=/opt/android-sdk
/opt/android-sdk: ['.knownPackages', '.temp', 'build-tools', 'cmdline-tools', 'licenses', 'platform-tools', 'platforms']
/opt/android-sdk-linux: absent
~~~
## pinned-wrapper-gradle-version

Command: ./gradlew --no-daemon --console&#61;plain --version
CWD: /tmp/eudi-carrier-multipaz-0.99.0
Started: 2026-11-11T12:28:42.666368+00:00
Finished: 2026-11-11T12:28:44.092391+00:00
Elapsed seconds: 1.4260434709995025
Exit: 0; PASS
Detail:

~~~text

------------------------------------------------------------
Gradle 8.14.2
------------------------------------------------------------

Build time:    2025-06-05 13:32:01 UTC
Revision:      30db2a3bdfffa9f8b40e798095675f9dab990a9a

Kotlin:        2.0.21
Groovy:        3.0.24
Ant:           Apache Ant(TM) version 1.10.15 compiled on August 25 2024
Launcher JVM:  21.0.12.1 (Arch Linux 21.0.12.1+1)
Daemon JVM:    /usr/lib/jvm/java-21-openjdk (no JDK specified, using current Java home)
OS:            Linux 7.3.3-1-cachyos amd64
~~~
## supplement-device-inventory

Command: adb devices -l
CWD: /tmp
Started: 2026-11-11T12:28:44.092422+00:00
Finished: 2026-11-11T12:28:44.142555+00:00
Elapsed seconds: 0.050152529000115464
Exit: 0; PASS
Detail:

~~~text
List of devices attached
~~~

## Suite provenance observed at supplementation

This source revision, dirty state and input hashes were observed later; they are not asserted as the original build-run provenance.

~~~json
{
  "dirty": true,
  "executions": [
    {
      "command": [
        "git",
        "rev-parse",
        "HEAD"
      ],
      "cwd": "/home/notroot/Documents/eudi-wallet-poc",
      "detail": "",
      "name": "suite-revision",
      "output": "a1a8c81c141145ca7e280b4591464c15e65f1377\n",
      "returncode": 0,
      "status": "PASS"
    },
    {
      "command": [
        "git",
        "status",
        "--porcelain",
        "--untracked-files=all"
      ],
      "cwd": "/home/notroot/Documents/eudi-wallet-poc",
      "detail": "",
      "name": "suite-cleanliness",
      "output": " M Makefile\n M _bmad-output/implementation-artifacts/sprint-status.yaml\n M docs/README.md\n M docs/profiles/CARRIER-CONFORMANCE-RUNNER.md\n M tests/README.md\n M tests/run_carrier_conformance.py\n M tests/test_carrier_conformance.py\n?? _bmad-output/implementation-artifacts/spec-2-4-external-carrier-run-and-published-result.md\n?? docs/profiles/carrier/external-run-2026-11-11/README.md\n?? docs/profiles/carrier/external-run-2026-11-11/report.json\n?? docs/profiles/carrier/external-run-2026-11-11/source-resolution.json\n?? docs/profiles/carrier/external-target.json\n?? tests/run_external_carrier.py\n?? tests/test_external_carrier.py\n",
      "returncode": 0,
      "status": "PASS"
    }
  ],
  "observed_at": "2026-11-11T12:28:24.420430+00:00",
  "revision": "a1a8c81c141145ca7e280b4591464c15e65f1377",
  "sha256": {
    "docs/profiles/carrier/external-target.json": "f031f87726a3c408481a393cbaa5ddb698ad7fbef5f149f92e35a3b71f40b717",
    "tests/e2e.sh": "f51d6783c86f748c1ab24fa701d1625cbfc3bd2d5fc55b2041848afd923568aa",
    "tests/run_carrier_conformance.py": "40995d431ebd3bf1577a68040794e14b3daa2f798794c6b2e7ee59c25aee99c1",
    "tests/run_external_carrier.py": "1b0cacd803f877c710c46eb72b11066a96c6b86da493c3b8c95593cd502acad4",
    "tests/vectors/carrier-v1/device_response.cbor": "23978eb511a13d4a38a743260d5ef44a7fd9488d47806c33631fe092adc7c4aa",
    "tests/vectors/carrier-v1/device_response.cbor.b64": "507ebf7ad0a5c726a54ea42e63c774fb9ab093c480af1cf13712dca05cf523e3",
    "tests/vectors/carrier-v1/device_response.json.b64": "0137786051da14b4d37cffa4ed547c1762080893827ef966e4f0fb5927bbecb5",
    "tests/vectors/carrier-v1/device_response_transcript.bin": "544b2287e816c6bdd4cfb9c666672014c7774f18ec09880eabe8f46032a5aaa6",
    "tests/vectors/carrier-v1/manifest.json": "9405fc1bff0d9029049a07bf78fc647e4d61f36033a70ef5f230587cfa28eca4",
    "tests/vectors/carrier-v1/request.json": "4586ada2b2833fa5f3b20de5d4d2720dacd7ec3a5d1f6944cc5d96e4144b5e83"
  }
}
~~~
