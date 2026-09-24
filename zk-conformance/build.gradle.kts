plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

dependencies {
    // multipaz-longfellow does not expose multipaz core on the compile classpath, so declare both.
    testImplementation(libs.multipaz.longfellow)
    testImplementation(libs.multipaz.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    // Step 0 (ee-eudiw plan §4): ee-eudiw is checked out next to this repository;
    // -Dstep0.* overrides stay available for other layouts.
    val eeEudiw = System.getProperty("step0.eeEudiw") ?: rootDir.resolveSibling("ee-eudiw").path
    systemProperty("step0.eeEudiw", eeEudiw)
    systemProperty("step0.rustProverDir", System.getProperty("step0.rustProverDir")
        ?: "$eeEudiw/verifier/go/zk/testdata/step0-rust-prover")
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
