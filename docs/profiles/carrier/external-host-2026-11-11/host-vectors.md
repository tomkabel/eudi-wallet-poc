# Multipaz host test attempt

Repository vector compatibility only; zero of 19 flow cases executed

Result: FAIL
Process status: PASS
Compatibility result: FAIL

Full structured provenance, hashes and unexecuted inventory: `host-vectors.json`

~~~json
{
  "schema_version": 1,
  "scope": "Repository vector compatibility only; zero of 19 flow cases executed",
  "target_revision": "7486ddbca490038c3580513d8273b9bbf18c6b70",
  "carrier_vectors": [
    {
      "carrier": "interim-json",
      "result": "FAIL",
      "parsed": false,
      "proof_verified": false,
      "evidence": "java.lang.IllegalArgumentException: Out of bounds decoding data"
    },
    {
      "carrier": "mso-mdoc-zk-cbor",
      "result": "PASS",
      "parsed": true,
      "proof_verified": true,
      "evidence": "Exact repository vector parsed and native proof verified by Multipaz 0.99.0 public APIs"
    }
  ]
}
~~~

## revision-before

argv: &#91;&#34;git&#34;, &#34;rev-parse&#34;, &#34;HEAD&#34;&#93;
status: PASS; returncode: 0

~~~text
7486ddbca490038c3580513d8273b9bbf18c6b70

~~~

## origin-before

argv: &#91;&#34;git&#34;, &#34;remote&#34;, &#34;get-url&#34;, &#34;origin&#34;&#93;
status: PASS; returncode: 0

~~~text
https://github.com/openwallet-foundation/multipaz.git

~~~

## cleanliness-before

argv: &#91;&#34;git&#34;, &#34;status&#34;, &#34;--porcelain&#34;, &#34;--untracked-files&#61;all&#34;&#93;
status: PASS; returncode: 0

~~~text

~~~

## java-version

argv: &#91;&#34;/usr/lib/jvm/java-17-openjdk/bin/java&#34;, &#34;-version&#34;&#93;
status: PASS; returncode: 0

~~~text
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment (build 17.0.20.1+1)
OpenJDK 64-Bit Server VM (build 17.0.20.1+1, mixed mode, sharing)

~~~

## host-target-test

argv: &#91;&#34;./gradlew&#34;, &#34;--offline&#34;, &#34;--configure-on-demand&#34;, &#34;--init-script&#34;, &#34;/home/notroot/Documents/eudi-wallet-poc/docs/profiles/carrier/external-host-2026-11-11/vector-harness/init.gradle&#34;, &#34;--no-daemon&#34;, &#34;--console&#61;plain&#34;, &#34;--max-workers&#61;2&#34;, &#34;:multipaz-longfellow:jvmTest&#34;, &#34;--tests&#34;, &#34;org.eudi.externalcarrier.RepositoryCarrierVectorTest.feedBothVectors&#34;&#93;
status: PASS; returncode: 0

~~~text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build
Configuration on demand is an incubating feature.
Type-safe project accessors is an incubating feature.

> Configure project :multipaz-longfellow
Skipping artifact signing for multipaz-longfellow: No signing keys configured.

> Configure project :multipaz-compose

The Kotlin Gradle plugin was loaded multiple times in different subprojects, which is not supported and may break the build.
This might happen in subprojects that apply the Kotlin plugins with the Gradle 'plugins { ... }' DSL if they specify explicit versions, even if the versions are equal.
Please add the Kotlin plugin to the common parent project or the root project, then remove the versions in the subprojects.
If the parent project does not need the plugin, add 'apply false' to the plugin line.
See: https://docs.gradle.org/current/userguide/plugins.html#sec:subprojects_plugins_dsl
The Kotlin plugin was loaded in the following projects: ':conventions', ':dokka-known-subclasses-plugin'

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
> Task :multipaz:kspCommonMainKotlinMetadata UP-TO-DATE
> Task :multipaz:kspKotlinJvm SKIPPED
> Task :multipaz:compileKotlinJvm UP-TO-DATE
> Task :multipaz:compileJvmMainJava NO-SOURCE
> Task :multipaz:jvmProcessResources NO-SOURCE
> Task :multipaz:processJvmMainResources SKIPPED
> Task :multipaz:jvmMainClasses UP-TO-DATE
> Task :multipaz:jvmJar UP-TO-DATE
> Task :multipaz-longfellow:kmpPartiallyResolvedDependenciesChecker
> Task :multipaz-longfellow:checkKotlinGradlePluginConfigurationErrors SKIPPED
> Task :multipaz-longfellow:generatePayloads UP-TO-DATE
> Task :multipaz-longfellow:kspKotlinJvm SKIPPED
> Task :multipaz-longfellow:compileKotlinJvm UP-TO-DATE
> Task :multipaz-longfellow:compileJvmMainJava NO-SOURCE
> Task :multipaz-longfellow:jvmProcessResources UP-TO-DATE
> Task :multipaz-longfellow:processJvmMainResources SKIPPED
> Task :multipaz-longfellow:jvmMainClasses UP-TO-DATE
> Task :multipaz-longfellow:jvmJar UP-TO-DATE
> Task :multipaz-longfellow:kspTestKotlinJvm SKIPPED
> Task :multipaz-longfellow:compileTestKotlinJvm UP-TO-DATE
> Task :multipaz-longfellow:compileJvmTestJava NO-SOURCE
> Task :multipaz-longfellow:jvmTestProcessResources NO-SOURCE
> Task :multipaz-longfellow:processJvmTestResources SKIPPED
> Task :multipaz-longfellow:jvmTestClasses UP-TO-DATE

> Task :multipaz-longfellow:jvmTest

RepositoryCarrierVectorTest[jvm] > feedBothVectors[jvm] STANDARD_OUT
    2026-11-11T23:26:40.997577223: INFO: LongfellowZkSystem: Verifying proof using ZkSystemSpec(id=longfellow-libzk-v1_7_1_4151_4096_8d079211715200ff06c5109639245502bfe94aa869908d31176aae4016182121, system=longfellow-libzk-v1)

[INFO][+  232.293 ms] bytes len: 98932952
[INFO][+  987.373 ms] circuit created. h[in:85118], s[in:3739]
[INFO][+    1.568 ms] proof params: h[nl:17, ni:85118], s[nl:21, ni:3739] hc[b:461 r:266] sc[b:455 r:19]
[INFO][+    4.862 ms] proofs read
[INFO][+    0.101 ms] verifier: recv commit
[INFO][+    0.002 ms] verifier: recv commit
[INFO][+    6.519 ms] verifier: verify
[INFO][+  912.881 ms] verify done: ok
[INFO][+    0.019 ms] verifier: verify
[INFO][+  604.891 ms] verify done: ok

> Task :multipaz-longfellow:jvmTest
    2026-11-11T23:26:44.132670496: INFO: LongfellowZkSystem: Verification Code: 0

[Incubating] Problems report is available at: file:///tmp/eudi-carrier-multipaz-0.99.0/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 9.0.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 1m 13s
24 actionable tasks: 3 executed, 21 up-to-date

~~~

## revision-after

argv: &#91;&#34;git&#34;, &#34;rev-parse&#34;, &#34;HEAD&#34;&#93;
status: PASS; returncode: 0

~~~text
7486ddbca490038c3580513d8273b9bbf18c6b70

~~~

## origin-after

argv: &#91;&#34;git&#34;, &#34;remote&#34;, &#34;get-url&#34;, &#34;origin&#34;&#93;
status: PASS; returncode: 0

~~~text
https://github.com/openwallet-foundation/multipaz.git

~~~

## cleanliness-after

argv: &#91;&#34;git&#34;, &#34;status&#34;, &#34;--porcelain&#34;, &#34;--untracked-files&#61;all&#34;&#93;
status: PASS; returncode: 0

~~~text

~~~

Presentation trims trailing whitespace; raw JSON retains original output.
