# Multipaz host test attempt

Repository vector feed only; zero of 19 carrier flow cases exercised.

Generated: 2026-11-11T21:17:55.810126+00:00
Finished: 2026-11-11T21:19:56.454769+00:00
Result: ERROR

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

argv: `["./gradlew", "--offline", "--init-script", "/home/notroot/Documents/eudi-wallet-poc/docs/profiles/carrier/external-host-2026-11-11/vector-harness/init.gradle", "--no-daemon", "--console=plain", "--max-workers=2", ":multipaz-longfellow:jvmTest", "--tests", "org.eudi.externalcarrier.RepositoryCarrierVectorTest.feedBothVectors"]`
status: ERROR; returncode: 143

```text
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

[runner terminated child: child exceeded 120s timeout]

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
