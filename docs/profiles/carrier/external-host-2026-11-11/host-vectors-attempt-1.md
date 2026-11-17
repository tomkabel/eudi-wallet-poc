# Multipaz host test attempt

Repository vector feed only; zero of 19 carrier flow cases exercised.

Generated: 2026-11-11T21:16:57.197504+00:00
Finished: 2026-11-11T21:17:41.661660+00:00
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
status: ERROR; returncode: 1

```text
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/8.14.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build

FAILURE: Build failed with an exception.

* Where:
Settings file '/tmp/eudi-carrier-multipaz-0.99.0/settings.gradle.kts' line: 63

* What went wrong:
Error resolving plugin [id: 'org.gradle.toolchains.foojay-resolver-convention', version: '0.9.0']
> Project with path ':multipaz-longfellow' could not be found in project ':build-logic'.

* Try:
> Run with --stacktrace option to get the stack trace.
> Run with --info or --debug option to get more log output.
> Run with --scan to get full insights.
> Get more help at https://help.gradle.org.

BUILD FAILED in 43s

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
