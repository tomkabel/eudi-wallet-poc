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
    // The fixture tests write into this repository's Go verifier testdata;
    // -Dzk.fixtureRoot and -Dstep0.rustProverDir stay available for other layouts.
    val fixtureRoot = System.getProperty("zk.fixtureRoot") ?: rootDir.path
    systemProperty("zk.fixtureRoot", fixtureRoot)
    systemProperty("step0.rustProverDir", System.getProperty("step0.rustProverDir")
        ?: "$fixtureRoot/verifier/go/zk/testdata/step0-rust-prover")
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
