# External carrier attempt

Status: BLOCKED
Detail: External preparation unavailable: child exceeded 600.0s timeout
Date: 2026-11-11T21:05:15.584524&#43;00:00
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
  "observed_at": "2026-11-11T21:05:15.609285+00:00",
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
      "output": " M Makefile\n M _bmad-output/implementation-artifacts/spec-2-4-external-carrier-run-and-published-result.md\n M docs/README.md\n M docs/profiles/CARRIER-CONFORMANCE-RUNNER.md\n M tests/README.md\n M tests/run_carrier_conformance.py\n M tests/test_carrier_conformance.py\n?? docs/profiles/carrier/external-rerun-2026-11-11/README.md\n?? docs/profiles/carrier/external-rerun-2026-11-11/report.json\n",
      "detail": "",
      "cwd": "/home/notroot/Documents/eudi-wallet-poc"
    }
  ],
  "sha256": {
    "tests/run_external_carrier.py": "2fcf254874933c741093e13678db0fe25b87e7230cfe8ee1059f8ece7980989b",
    "tests/run_carrier_conformance.py": "6f60b34a7d0be864abd7884d537cf27ddb6d9ce0d48da83170dbd825ec11e44c",
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
Detail: child exceeded 600.0s timeout

~~~text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
Type-safe project accessors is an incubating feature.

> Configure project :multipaz
Skipping artifact signing for multipaz: No signing keys configured.
w: ⚠️ Invalid Source Set Dependency Across Trees
Kotlin Source Set 'androidInstrumentedTest' can't depend on 'commonTest' as they are from different Source Set Trees.
Solution: Please remove this dependency edge.


> Configure project :multipaz-android-legacy
Skipping artifact signing for multipaz-android-legacy: No signing keys configured.

> Configure project :multipaz-cbor-rpc
Skipping artifact signing for multipaz-cbor-rpc: No signing keys configured.

> Configure project :multipaz-compose

The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may break the build.
This might happen in subprojects that apply the Kotlin plugins with the Gradle 'plugins { ... }' DSL if they specify explicit versions, even if the versions are equal.
Please add the Kotlin plugin to the common parent project or the root project, then remove the versions in the subprojects.
If the parent project does not need the plugin, add 'apply false' to the plugin line.
See: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
The Kotlin plugin was loaded in the following projects: ':conventions', ':dokka-known-subclasses-plugin'

> Task :build-logic:conventions:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :build-logic:lokalize:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :build-logic:conventions:pluginDescriptors
> Task :build-logic:conventions:processResources
> Task :build-logic:lokalize:pluginDescriptors
> Task :build-logic:lokalize:processResources
> Task :build-logic:lokalize:compileKotlin
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build-logic/lokalize/src/main/kotlin/org/multipaz/lokalize/llm/strategy/GoogleStrategy.kt:22:18 'static enum entry GEMINI15FLASH: LLmModel' is deprecated. Use GEMINI2_0_FLASH or GEMINI2_5_FLASH instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build-logic/lokalize/src/main/kotlin/org/multipaz/lokalize/util/LLMConfig.kt:158:14 'static enum entry GEMINI15FLASH: LLmModel' is deprecated. Use GEMINI2_0_FLASH or GEMINI2_5_FLASH instead.

> Task :build-logic:lokalize:compileJava NO-SOURCE
> Task :build-logic:lokalize:classes
> Task :build-logic:lokalize:jar
> Task :build-logic:conventions:compileKotlin
> Task :build-logic:conventions:compileJava NO-SOURCE
> Task :build-logic:conventions:classes
> Task :build-logic:conventions:jar

> Configure project :multipaz-compose
Skipping artifact signing for multipaz-compose: No signing keys configured.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:3:30: 'ExperimentalComposeLibrary' is deprecated. Please specify dependency via version catalog
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:88:40: 'runtime: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:89:40: 'foundation: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:90:40: 'material3: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:91:40: 'ui: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:92:40: 'components: ComposePlugin.CommonComponentsDependencies' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:92:51: 'resources: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:93:40: 'materialIconsExtended: String' is deprecated. This artifact is pinned to version 1.7.3 and will not receive updates. Either use this version explicitly or migrate to Material Symbols (vector resources). See https://kotlinlang.org/docs/multiplatform/whats-new-compose-180.html
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:96:29: 'runtime: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:97:29: 'foundation: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:98:29: 'material3: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:99:29: 'ui: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:100:29: 'components: ComposePlugin.CommonComponentsDependencies' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:100:40: 'resources: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:101:29: 'materialIconsExtended: String' is deprecated. This artifact is pinned to version 1.7.3 and will not receive updates. Either use this version explicitly or migrate to Material Symbols (vector resources). See https://kotlinlang.org/docs/multiplatform/whats-new-compose-180.html
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:120:24: 'ExperimentalComposeLibrary' is deprecated. Please specify dependency via version catalog
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:121:40: 'uiTest: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build.gradle.kts:159:37: 'uiTooling: String' is deprecated. Use org.jetbrains.compose.ui:ui-tooling module instead
XML format detected for multipaz-compose - generateMultipazStrings will skip execution

> Configure project :multipaz-dcapi
Skipping artifact signing for multipaz-dcapi: No signing keys configured.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz-dcapi/build.gradle.kts:42:9: This declaration needs opt-in. Its usage should be marked with '@org.jetbrains.kotlin.gradle.ExperimentalWasmDsl' or '@OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)'
w: ⚠️ Invalid Source Set Dependency Across Trees
Kotlin Source Set 'androidInstrumentedTest' can't depend on 'commonTest' as they are from different Source Set Trees.
Solution: Please remove this dependency edge.


> Configure project :multipaz-doctypes
Skipping artifact signing for multipaz-doctypes: No signing keys configured.
JSON format configured for generateMultipazStrings task in multipaz-doctypes

> Configure project :multipaz-longfellow
Skipping artifact signing for multipaz-longfellow: No signing keys configured.

> Configure project :multipaz-server
Skipping artifact signing for multipaz-server: No signing keys configured.

> Configure project :multipaz-swiftui
Skipping artifact signing for multipaz-swiftui: No signing keys configured.

> Configure project :multipaz-utopia
Skipping artifact signing for multipaz-utopia: No signing keys configured.

> Configure project :multipaz:SwiftBridge
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/SwiftBridge/build.gradle.kts:30:7: 'create(String, noinline T.() -> Unit): T' is deprecated. Use register instead. See https://docs.gradle.org/current/userguide/task_configuration_avoidance.html for more information.

> Configure project :multipaz-dcapi:matcherTest
This version only understands SDK XML versions up to 3 but an SDK XML file of version 4 was encountered. This can happen if you use versions of Android Studio and the command-line tools that were released at different times.
Checking the license for package NDK (Side by side) 27.0.12077973 in /opt/android-sdk/licenses
License for package NDK (Side by side) 27.0.12077973 accepted.
Preparing "Install NDK (Side by side) 27.0.12077973 v.27.0.12077973".
"Install NDK (Side by side) 27.0.12077973 v.27.0.12077973" ready.
Installing NDK (Side by side) 27.0.12077973 in /opt/android-sdk/ndk/27.0.12077973
"Install NDK (Side by side) 27.0.12077973 v.27.0.12077973" complete.
"Install NDK (Side by side) 27.0.12077973 v.27.0.12077973" finished.

> Configure project :samples:testapp
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:130:40: 'preview: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:151:40: 'runtime: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:152:40: 'foundation: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:153:40: 'material3: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:154:40: 'ui: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:155:40: 'components: ComposePlugin.CommonComponentsDependencies' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:155:51: 'resources: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:156:40: 'components: ComposePlugin.CommonComponentsDependencies' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:156:51: 'uiToolingPreview: String' is deprecated. Specify dependency directly
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:157:40: 'materialIconsExtended: String' is deprecated. This artifact is pinned to version 1.7.3 and will not receive updates. Either use this version explicitly or migrate to Material Symbols (vector resources). See https://kotlinlang.org/docs/multiplatform/whats-new-compose-180.html
w: file:///tmp/eudi-carrier-multipaz-0.99.0/samples/testapp/build.gradle.kts:240:37: 'uiTooling: String' is deprecated. Use org.jetbrains.compose.ui:ui-tooling module instead
Checking the license for package Android SDK Platform 36 in /opt/android-sdk/licenses
License for package Android SDK Platform 36 accepted.
Preparing "Install Android SDK Platform 36 (revision 2)".
"Install Android SDK Platform 36 (revision 2)" ready.
Installing Android SDK Platform 36 in /opt/android-sdk/platforms/android-36
"Install Android SDK Platform 36 (revision 2)" complete.
"Install Android SDK Platform 36 (revision 2)" finished.
[=========                              ] 25%
Added dependency: compileDebugKotlinAndroid -> generateMultipazStrings for multipaz-compose
Configured generated strings source dir for multipaz-compose: /tmp/eudi-carrier-multipaz-0.99.0/multipaz-compose/build/generated/kmp/kotlin/org/multipaz/doctypes/generated
Added dependency: compileDebugKotlinAndroid -> generateMultipazStrings for multipaz-doctypes
Configured generated strings source dir for multipaz-doctypes: /tmp/eudi-carrier-multipaz-0.99.0/multipaz-doctypes/build/generated/kmp/kotlin/org/multipaz/doctypes/generated
Added dependency: compileWebMainKotlinMetadata -> generateMultipazStrings for multipaz-compose
Added dependency: compileCommonMainKotlinMetadata -> generateMultipazStrings for multipaz-compose
Added dependency: compileWebMainKotlinMetadata -> generateMultipazStrings for multipaz-doctypes
Added dependency: compileCommonMainKotlinMetadata -> generateMultipazStrings for multipaz-doctypes
Added dependency: compileAppleMainKotlinMetadata -> generateMultipazStrings for multipaz-doctypes
Added dependency: compileIosMainKotlinMetadata -> generateMultipazStrings for multipaz-doctypes
Added dependency: compileNativeMainKotlinMetadata -> generateMultipazStrings for multipaz-doctypes
Added dependency: compileAppleMainKotlinMetadata -> generateMultipazStrings for multipaz-compose
Added dependency: compileIosMainKotlinMetadata -> generateMultipazStrings for multipaz-compose
Added dependency: compileNativeMainKotlinMetadata -> generateMultipazStrings for multipaz-compose

> Task :samples:testapp:preBuild UP-TO-DATE
> Task :samples:testapp:preBlueDebugBuild UP-TO-DATE
> Task :samples:testapp:mergeBlueDebugNativeDebugMetadata NO-SOURCE
> Task :multipaz:kmpPartiallyResolvedDependenciesChecker
> Task :multipaz:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz:generateBuildConfigClasses UP-TO-DATE
> Task :multipaz:generateJavaSharedMainBuildConfigClasses SKIPPED
> Task :multipaz:generateJvmMainBuildConfigClasses SKIPPED
> Task :multipaz:transformCommonMainDependenciesMetadata UP-TO-DATE
> Task :multipaz-cbor-rpc:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz-cbor-rpc:compileKotlin UP-TO-DATE
> Task :multipaz-cbor-rpc:compileJava NO-SOURCE
> Task :multipaz-cbor-rpc:processResources UP-TO-DATE
> Task :multipaz-cbor-rpc:classes UP-TO-DATE
> Task :multipaz-cbor-rpc:jar UP-TO-DATE
> Task :multipaz:kspKotlinJvm SKIPPED
> Task :multipaz:jvmProcessResources NO-SOURCE
> Task :multipaz:processJvmMainResources SKIPPED
> Task :multipaz-swiftui:kmpPartiallyResolvedDependenciesChecker
> Task :multipaz-swiftui:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz-swiftui:jvmProcessResources NO-SOURCE
> Task :multipaz-swiftui:processJvmMainResources SKIPPED
> Task :multipaz:generateAndroidDebugBuildConfigClasses SKIPPED
> Task :multipaz:preBuild UP-TO-DATE
> Task :multipaz:preDebugBuild UP-TO-DATE
> Task :multipaz:generateDebugResValues
> Task :multipaz:generateDebugResources
> Task :multipaz:packageDebugResources
> Task :multipaz:processDebugNavigationResources
> Task :multipaz:javaPreCompileDebug
> Task :multipaz:parseDebugLocalResources
> Task :multipaz:kspCommonMainKotlinMetadata
> Task :multipaz-compose:kmpPartiallyResolvedDependenciesChecker

[runner terminated child: child exceeded 600.0s timeout]

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
