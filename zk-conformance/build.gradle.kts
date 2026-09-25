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
    // The fixture tests write into build/fixture-root by default, so a plain test run
    // leaves the committed fixtures alone. -Dzk.regenerate=true writes into this
    // repository's Go verifier testdata instead; -Dzk.fixtureRoot and
    // -Dstep0.rustProverDir stay available for other layouts.
    val regenerate = System.getProperty("zk.regenerate") == "true"
    val fixtureRoot = System.getProperty("zk.fixtureRoot")
        ?: if (regenerate) rootDir.path else layout.buildDirectory.dir("fixture-root").get().asFile.path
    systemProperty("zk.fixtureRoot", fixtureRoot)
    // An input, not an output: always the committed Rust-prover fixture.
    systemProperty("step0.rustProverDir", System.getProperty("step0.rustProverDir")
        ?: "${rootDir.path}/verifier/go/zk/testdata/step0-rust-prover")
    testLogging {
        showStandardStreams = true
        events("passed", "failed", "skipped")
    }
}
