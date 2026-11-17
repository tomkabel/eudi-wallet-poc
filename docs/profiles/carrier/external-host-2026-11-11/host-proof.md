# Multipaz host proof self-test attempt

Upstream JVM self-test only; neither the 19 external carrier assertions nor the two repository vectors are exercised.

Generated: 2026-11-11T21:10:14.132784+00:00
Finished: 2026-11-11T21:16:35.885304+00:00
Result: PASS

## revision-before

argv: `["git", "rev-parse", "HEAD"]`
status: PASS; returncode: 0

```text
7486ddbca490038c3580513d8273b9bbf18c6b70

```

## cleanliness-before

argv: `["git", "status", "--porcelain", "--untracked-files=all"]`
status: PASS; returncode: 0

```text

```

## java-version

argv: `["/usr/lib/jvm/java-17-openjdk/bin/java", "-version"]`
status: PASS; returncode: 0

```text
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment (build 17.0.20.1+1)
OpenJDK 64-Bit Server VM (build 17.0.20.1+1, mixed mode, sharing)

```

## upstream-system-test

argv: `["./gradlew", "--no-daemon", "--console=plain", "--max-workers=2", ":multipaz-longfellow:jvmTest", "--tests", "org.multipaz.mdoc.zkp.longfellow.SystemTest.testProofFullFlow_success"]`
status: PASS; returncode: 0

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
Type-safe project accessors is an incubating feature.

> Configure project :
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build.gradle.kts:25:17: 'exec(Action<in ExecSpec!>!): ExecResult!' is deprecated. Deprecated in Java
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build.gradle.kts:43:17: 'exec(Action<in ExecSpec!>!): ExecResult!' is deprecated. Deprecated in Java
w: file:///tmp/eudi-carrier-multipaz-0.99.0/build.gradle.kts:47:19: 'toString(): String' is deprecated. Deprecated in Java

> Configure project :multipaz
Skipping artifact signing for multipaz: No signing keys configured.
w: ⚠️ Invalid Source Set Dependency Across Trees
Kotlin Source Set 'androidInstrumentedTest' can't depend on 'commonTest' as they are from different Source Set Trees.
Solution: Please remove this dependency edge.


> Configure project :multipaz-android-legacy
Skipping artifact signing for multipaz-android-legacy: No signing keys configured.

> Configure project :multipaz-cbor-rpc
Skipping artifact signing for multipaz-cbor-rpc: No signing keys configured.

> Task :build-logic:conventions:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :build-logic:lokalize:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :build-logic:lokalize:compileKotlin UP-TO-DATE
> Task :build-logic:lokalize:compileJava NO-SOURCE
> Task :build-logic:conventions:compileKotlin UP-TO-DATE
> Task :build-logic:conventions:compileJava NO-SOURCE
> Task :build-logic:conventions:pluginDescriptors UP-TO-DATE
> Task :build-logic:conventions:processResources UP-TO-DATE
> Task :build-logic:conventions:classes UP-TO-DATE
> Task :build-logic:conventions:jar UP-TO-DATE
> Task :build-logic:lokalize:pluginDescriptors UP-TO-DATE
> Task :build-logic:lokalize:processResources UP-TO-DATE
> Task :build-logic:lokalize:classes UP-TO-DATE
> Task :build-logic:lokalize:jar UP-TO-DATE

> Configure project :multipaz-compose

The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may break the build.
This might happen in subprojects that apply the Kotlin plugins with the Gradle 'plugins { ... }' DSL if they specify explicit versions, even if the versions are equal.
Please add the Kotlin plugin to the common parent project or the root project, then remove the versions in the subprojects.
If the parent project does not need the plugin, add 'apply false' to the plugin line.
See: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
The Kotlin plugin was loaded in the following projects: ':conventions', ':dokka-known-subclasses-plugin'
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

> Task :multipaz:kmpPartiallyResolvedDependenciesChecker
> Task :multipaz:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz:generateBuildConfigClasses
> Task :multipaz:generateJavaSharedMainBuildConfigClasses SKIPPED
> Task :multipaz:generateJvmMainBuildConfigClasses SKIPPED
> Task :multipaz:transformCommonMainDependenciesMetadata
> Task :multipaz-cbor-rpc:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz-cbor-rpc:processResources
> Task :multipaz:kspKotlinJvm SKIPPED
> Task :multipaz:jvmProcessResources NO-SOURCE
> Task :multipaz:processJvmMainResources SKIPPED
> Task :multipaz-longfellow:kmpPartiallyResolvedDependenciesChecker
> Task :multipaz-longfellow:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz-longfellow:generatePayloads
> Task :multipaz-cbor-rpc:compileKotlin
> Task :multipaz-cbor-rpc:compileJava NO-SOURCE
> Task :multipaz-cbor-rpc:classes
> Task :multipaz-cbor-rpc:jar
> Task :multipaz:kspCommonMainKotlinMetadata
> Task :multipaz:compileKotlinJvm
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/engagement/EngagementGenerator.kt:49:78 'class EngagementGenerator : Any' is deprecated. This class is deprecated, use DeviceEngagement instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/engagement/EngagementGenerator.kt:63:56 'class EngagementGenerator : Any' is deprecated. This class is deprecated, use DeviceEngagement instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/request/DeviceRequestGenerator.kt:80:8 'class DeviceRequestGenerator : Any' is deprecated. This class is deprecated, use DeviceRequest instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/request/DeviceRequestGenerator.kt:128:26 'suspend fun coseSign1Sign(secureArea: SecureArea, alias: String, message: ByteArray, includeMessageInPayload: Boolean, protectedHeaders: Map<CoseLabel, DataItem>, unprotectedHeaders: Map<CoseLabel, DataItem>, unlockReason: Reason = ...): CoseSign1' is deprecated. Use SigningKey-based method instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/request/DeviceRequestGenerator.kt:168:8 'class DeviceRequestGenerator : Any' is deprecated. This class is deprecated, use DeviceRequest instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/request/DeviceRequestGenerator.kt:239:26 'suspend fun coseSign1Sign(key: EcPrivateKey, dataToSign: ByteArray, includeDataInPayload: Boolean, signatureAlgorithm: Algorithm, protectedHeaders: Map<CoseLabel, DataItem>, unprotectedHeaders: Map<CoseLabel, DataItem>): CoseSign1' is deprecated. Use SigningKey-based method instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/response/DeviceResponseParser.kt:33:8 'class MobileSecurityObjectParser : Any' is deprecated. Deprecated, use MobileSecurityObject instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/response/DeviceResponseParser.kt:168:29 'constructor(encodedMobileSecurityObject: ByteArray): MobileSecurityObjectParser' is deprecated. Deprecated, use MobileSecurityObject instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/response/DocumentGenerator.kt:70:60 'class DocumentGenerator : Any' is deprecated. Deprecated, use DeviceResponse instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/response/DocumentGenerator.kt:124:22 'suspend fun coseSign1Sign(secureArea: SecureArea, alias: String, message: ByteArray, includeMessageInPayload: Boolean, protectedHeaders: Map<CoseLabel, DataItem>, unprotectedHeaders: Map<CoseLabel, DataItem>, unlockReason: Reason = ...): CoseSign1' is deprecated. Use SigningKey-based method instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/util/MdocUtil.kt:31:8 'class DocumentRequest : Any' is deprecated. Deprecated, use Request instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/util/MdocUtil.kt:32:8 'class DocumentRequest : Any' is deprecated. Deprecated, use Request instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/util/MdocUtil.kt:41:8 'class DeviceRequestParser : Any' is deprecated. This class is deprecated, use DeviceRequest instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/util/MdocUtil.kt:261:18 'class DocumentRequest : Any' is deprecated. Deprecated, use Request instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/util/MdocUtil.kt:371:8 'class DocumentRequest : Any' is deprecated. Deprecated, use Request instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/mdoc/util/MdocUtil.kt:380:16 'constructor(requestedDataElements: List<DocumentRequest.DataElement>): DocumentRequest' is deprecated. Deprecated, use Request instead.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/openid/OpenID4VP.kt:686:67 Unchecked cast of 'List<RequestedClaim>' to 'List<MdocRequestedClaim>'.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/openid/OpenID4VP.kt:719:58 Unchecked cast of 'List<RequestedClaim>' to 'List<JsonRequestedClaim>'.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/presentment/mdocPresentment.kt:145:62 Unchecked cast of 'List<RequestedClaim>' to 'List<MdocRequestedClaim>'.
w: file:///tmp/eudi-carrier-multipaz-0.99.0/multipaz/src/commonMain/kotlin/org/multipaz/provisioning/DocumentProvisioningHandler.kt:161:49 Unchecked cast of 'List<Credential>' to 'List<SecureAreaBoundCredential>'.

> Task :multipaz:compileJvmMainJava NO-SOURCE
> Task :multipaz:jvmMainClasses
> Task :multipaz:jvmJar
> Task :multipaz-longfellow:kspKotlinJvm SKIPPED
> Task :multipaz-longfellow:jvmProcessResources
> Task :multipaz-longfellow:processJvmMainResources SKIPPED
> Task :multipaz-longfellow:compileKotlinJvm
> Task :multipaz-longfellow:compileJvmMainJava NO-SOURCE
> Task :multipaz-longfellow:jvmMainClasses
> Task :multipaz-longfellow:jvmJar
> Task :multipaz-longfellow:kspTestKotlinJvm SKIPPED
> Task :multipaz-longfellow:jvmTestProcessResources NO-SOURCE
> Task :multipaz-longfellow:processJvmTestResources SKIPPED
> Task :multipaz-longfellow:compileTestKotlinJvm
> Task :multipaz-longfellow:compileJvmTestJava NO-SOURCE
> Task :multipaz-longfellow:jvmTestClasses

> Task :multipaz-longfellow:jvmTest

SystemTest[jvm] > testProofFullFlow_success[jvm] STANDARD_OUT
    2026-11-11T23:16:27.555116008: INFO: TAG: nameSpaces: 1 bytes of CBOR: a0
    In diagnostic notation:
    {}
    2026-11-11T23:16:27.693381855: INFO: LongfellowZkSystem: Generating proof using ZkSystemSpec(id=one_longfellow-libzk-v1, system=longfellow-libzk-v1)

[INFO][+  135.119 ms] bytes len: 87718862
[INFO][+  659.739 ms] circuit created. h[in:75098 q:15], s[in:3739 q:21]
[INFO][+   43.509 ms] ZK Commit start
[INFO][+  255.580 ms] ZK Commitment done
[INFO][+    0.022 ms] ZK Commit start
[INFO][+  251.241 ms] ZK Commitment done
[INFO][+    0.018 ms] commit created. h[nl:15, ni:75098], s[nl:21, ni:3739] hc[b:682 r:142] sc[b:491 r:17]
[INFO][+ 1359.938 ms] ZK sumcheck done
[INFO][+    9.471 ms] ZK constraints done
[INFO][+   21.101 ms] Prover Done: flag
[INFO][+    4.361 ms] ZK hash proof done
[INFO][+ 1237.837 ms] ZK sumcheck done
[INFO][+   28.609 ms] ZK constraints done
[INFO][+   88.033 ms] Prover Done: flag
[INFO][+    0.028 ms] ZK signature proof done
[INFO][+   19.063 ms] com:32, sc:16800, com_proof:117072 [1364 el, 18176 el, 463 d in 142 rows]: 133904b
[INFO][+    0.551 ms] com:32, sc:36032, com_proof:153644 [982 el, 2176 el, 407 d in 17 rows]: 189708b
[INFO][+    0.006 ms] proof_len: 323708

> Task :multipaz-longfellow:jvmTest
    2026-11-11T23:16:32.006564202: INFO: LongfellowZkSystem: Verifying proof using ZkSystemSpec(id=one_longfellow-libzk-v1, system=longfellow-libzk-v1)

[INFO][+  329.468 ms] bytes len: 87718862
[INFO][+  898.941 ms] circuit created. h[in:75098], s[in:3739]
[INFO][+    0.173 ms] proof params: h[nl:15, ni:75098], s[nl:21, ni:3739] hc[b:682 r:142] sc[b:491 r:17]
[INFO][+    2.915 ms] proofs read
[INFO][+    0.084 ms] verifier: recv commit
[INFO][+    0.003 ms] verifier: recv commit
[INFO][+    0.167 ms] verifier: verify
[INFO][+  655.166 ms] verify done: ok
[INFO][+    0.042 ms] verifier: verify
[INFO][+  488.001 ms] verify done: ok

> Task :multipaz-longfellow:jvmTest
    2026-11-11T23:16:34.321172121: INFO: LongfellowZkSystem: Verification Code: 0

[Incubating] Problems report is available at: file:///tmp/eudi-carrier-multipaz-0.99.0/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 6m 20s
24 actionable tasks: 16 executed, 8 up-to-date

```

## revision-after

argv: `["git", "rev-parse", "HEAD"]`
status: PASS; returncode: 0

```text
7486ddbca490038c3580513d8273b9bbf18c6b70

```

## cleanliness-after

argv: `["git", "status", "--porcelain", "--untracked-files=all"]`
status: PASS; returncode: 0

```text

```
