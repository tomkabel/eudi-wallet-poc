# Multipaz host test attempt

Repository vector feed only; zero of 19 carrier flow cases exercised.

Generated: 2026-11-11T21:20:11.490103+00:00
Finished: 2026-11-11T21:21:36.660286+00:00
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

## host-target-test

argv: `["./gradlew", "--offline", "--configure-on-demand", "--init-script", "/home/notroot/Documents/eudi-wallet-poc/docs/profiles/carrier/external-host-2026-11-11/vector-harness/init.gradle", "--no-daemon", "--console=plain", "--max-workers=2", ":multipaz-longfellow:jvmTest", "--tests", "org.eudi.externalcarrier.RepositoryCarrierVectorTest.feedBothVectors"]`
status: PASS; returncode: 0

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
Configuration on demand is an incubating feature.
Type-safe project accessors is an incubating feature.

> Configure project :multipaz-longfellow
Skipping artifact signing for multipaz-longfellow: No signing keys configured.

> Task :build-logic:conventions:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :build-logic:lokalize:checkKotlinGradlePluginConfigurationErrors SKIPPED

> Configure project :multipaz-compose

The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may break the build.
This might happen in subprojects that apply the Kotlin plugins with the Gradle 'plugins { ... }' DSL if they specify explicit versions, even if the versions are equal.
Please add the Kotlin plugin to the common parent project or the root project, then remove the versions in the subprojects.
If the parent project does not need the plugin, add 'apply false' to the plugin line.
See: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
The Kotlin plugin was loaded in the following projects: ':conventions', ':dokka-known-subclasses-plugin'

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
Skipping artifact signing for multipaz-compose: No signing keys configured.
XML format detected for multipaz-compose - generateMultipazStrings will skip execution

> Configure project :multipaz
Skipping artifact signing for multipaz: No signing keys configured.
w: ⚠️ Invalid Source Set Dependency Across Trees
Kotlin Source Set 'androidInstrumentedTest' can't depend on 'commonTest' as they are from different Source Set Trees.
Solution: Please remove this dependency edge.


> Configure project :multipaz-cbor-rpc
Skipping artifact signing for multipaz-cbor-rpc: No signing keys configured.

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
> Task :multipaz-longfellow:kmpPartiallyResolvedDependenciesChecker
> Task :multipaz-longfellow:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz-longfellow:generatePayloads
> Task :multipaz:kspCommonMainKotlinMetadata
> Task :multipaz:compileKotlinJvm UP-TO-DATE
> Task :multipaz:compileJvmMainJava NO-SOURCE
> Task :multipaz:jvmMainClasses UP-TO-DATE
> Task :multipaz:jvmJar UP-TO-DATE
> Task :multipaz-longfellow:kspKotlinJvm SKIPPED
> Task :multipaz-longfellow:compileKotlinJvm UP-TO-DATE
> Task :multipaz-longfellow:compileJvmMainJava NO-SOURCE
> Task :multipaz-longfellow:jvmProcessResources UP-TO-DATE
> Task :multipaz-longfellow:processJvmMainResources SKIPPED
> Task :multipaz-longfellow:jvmMainClasses UP-TO-DATE
> Task :multipaz-longfellow:jvmJar UP-TO-DATE
> Task :multipaz-longfellow:kspTestKotlinJvm SKIPPED
> Task :multipaz-longfellow:jvmTestProcessResources NO-SOURCE
> Task :multipaz-longfellow:processJvmTestResources SKIPPED
> Task :multipaz-longfellow:compileTestKotlinJvm
> Task :multipaz-longfellow:compileJvmTestJava NO-SOURCE
> Task :multipaz-longfellow:jvmTestClasses
[INFO][+   96.228 ms] bytes len: 98932952

> Task :multipaz-longfellow:jvmTest

RepositoryCarrierVectorTest[jvm] > feedBothVectors[jvm] STANDARD_OUT
    2026-11-11T23:21:34.467432431: INFO: LongfellowZkSystem: Verifying proof using ZkSystemSpec(id=longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121, system=longfellow-libzk-v1)

[INFO][+  495.556 ms] circuit created. h[in:85118], s[in:3739]
[INFO][+    0.401 ms] proof params: h[nl:17, ni:85118], s[nl:21, ni:3739] hc[b:461 r:266] sc[b:455 r:19]
[INFO][+    3.307 ms] proofs read
[INFO][+    0.058 ms] verifier: recv commit
[INFO][+    0.001 ms] verifier: recv commit
[INFO][+    2.113 ms] verifier: verify
[INFO][+  379.362 ms] verify done: ok
[INFO][+    0.012 ms] verifier: verify
[INFO][+  365.533 ms] verify done: ok

> Task :multipaz-longfellow:jvmTest
    2026-11-11T23:21:36.011716246: INFO: LongfellowZkSystem: Verification Code: 0

[Incubating] Problems report is available at: file:///tmp/eudi-carrier-multipaz-0.99.0/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 24s
24 actionable tasks: 6 executed, 18 up-to-date

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
