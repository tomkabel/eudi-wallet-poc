import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.android.navigation.safeargs) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.spotless) apply false
    // SBOM (CycloneDX): `./gradlew cyclonedxBom` writes build/reports/cyclonedx/bom.{json,xml}; CI uploads it.
    id("org.cyclonedx.bom") version "3.5.1"
}

// Commented out - causing configuration issues with single-module project
// subprojects {
//     tasks.withType<Test> {
//         useJUnit()
//         testLogging {
//             showStandardStreams = true
//             exceptionFormat = TestExceptionFormat.FULL
//             events("skipped", "failed", "passed")
//         }
//     }
// }


dependencyAnalysis {
    structure {
        ignoreKtx(ignore = true)
    }
    issues {
        all {
            onUnusedDependencies {
                severity("warn")
            }
            onUsedTransitiveDependencies {
                severity("warn")
            }
            onIncorrectConfiguration {
                severity("warn")
            }
            onUnusedAnnotationProcessors {
                severity("warn")
            }
            onRedundantPlugins {
                severity("warn")
            }
        }
    }
}
