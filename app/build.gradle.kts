@file:Suppress("UnstableApiUsage")

import com.android.build.api.dsl.VariantDimension
import com.google.firebase.appdistribution.gradle.firebaseAppDistribution
import com.google.protobuf.gradle.GenerateProtoTask
import java.io.FileInputStream
import java.util.Properties

val localProperties = readLocalProperties()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.android.navigation.safeargs)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.distribution)
}

android {
    namespace = "ee.cyber.wallet"
    compileSdk = 35

    androidComponents {
        beforeVariants {
            // Disable release build
            if (it.buildType == "release") {
                it.enable = false
            }
        }
    }
    defaultConfig {
        applicationId = "ee.ria.wallet"
        minSdk = 31
        targetSdk = 35
        versionCode = project.properties["BUILD_NUMBER"]?.toString()?.toInt() ?: 9999
        versionName = "0.6.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        resourceConfigurations += arrayOf("en", "et")
        configureEnvironment(Environment.TEST)
        ndk {
            // multipaz-longfellow ships libzkp.so for these two ABIs only. Other dependencies pull
            // in armeabi-v7a and x86, and installing there would give a wallet that cannot generate
            // a ZK proof at all — so restrict to what the prover supports and keep "installable"
            // and "able to prove" the same thing.
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.properties["RELEASE_KEYSTORE_FILE"]!!)
            storePassword = project.properties["RELEASE_KEYSTORE_PASSWORD"]?.toString()
            keyAlias = project.properties["RELEASE_SIGN_KEY_ALIAS"]?.toString()
            keyPassword = project.properties["RELEASE_SIGN_KEY_PASSWORD"]?.toString()
        }
        getByName("debug") {
            storeFile = file(project.properties["DEBUG_KEYSTORE_FILE"] ?: "")
            storePassword = project.properties["DEBUG_KEYSTORE_PASSWORD"]?.toString()
            keyAlias = project.properties["DEBUG_SIGN_KEY_ALIAS"]?.toString()
            keyPassword = project.properties["DEBUG_SIGN_KEY_PASSWORD"]?.toString()
        }
    }

    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            configureEnvironment(Environment.PROD)
        }
        debug {
            isDefault = false
            isDebuggable = true
            enableUnitTestCoverage = true
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
            configureEnvironment(Environment.TEST)

            firebaseAppDistribution {
                artifactType = "APK"
                groups = "android-testers"
                releaseNotes = "No release notes."
            }
        }
        create("local") {
            initWith(buildTypes.getByName("debug"))
            configureEnvironment(Environment.LOCAL)
            boolBuildConfig("DEV", true)
        }
        create("local_mocks") {
            initWith(buildTypes.getByName("debug"))
            isDefault = true
            configureEnvironment(Environment.LOCAL)
            boolBuildConfig("DEV", true)
            boolBuildConfig("USE_MOCKS", true)
        }
    }
    kotlin {
        kotlinOptions {
            jvmToolchain(17)
            freeCompilerArgs += listOf(
                "-Xjsr305=strict",
//            "-XXLanguage:+ExplicitBackingFields",
//              "-Xdebug", "-Xnoopt",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                "-opt-in=kotlinx.serialization.ExperimentalSerializationApi",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=com.google.accompanist.permissions.ExperimentalPermissionsApi",
                "-opt-in=kotlin.time.ExperimentalTime"
            )
        }
        sourceSets.all {
            languageSettings {
                languageVersion = "2.1"
            }
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeCompiler {
        enableStrongSkippingMode = true
        includeSourceInformation = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES,versions/9/OSGI-INF/MANIFEST.MF}"
            // Exclude duplicate files from DSS and JAXB dependencies
            excludes += "/META-INF/sun-jaxb.episode"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/NOTICE.md"
        }
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("${layout.buildDirectory}/**/*.kt")
        ktlint(libs.versions.ktlint.get())
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        ktlint(libs.versions.ktlint.get())
    }
}

// Setup protobuf configuration, generating lite Java and Kotlin classes
protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    plugins {
        create("java") {
            artifact = libs.grpc.protoc.gen.grpc.java.get().toString()
        }
        create("grpc") {
            artifact = libs.grpc.protoc.gen.grpc.java.get().toString()
        }
        create("grpckt") {
            artifact = libs.grpc.protoc.gen.grpc.kotlin.get().toString() + ":jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("java") {
                    option("lite")
                }
                create("grpc") {
                    option("lite")
                }
                create("grpckt") {
                    option("lite")
                }
            }
            it.builtins {
                create("kotlin") {
                    option("lite")
                }
            }
        }
    }
}

// https://github.com/google/ksp/issues/1590
androidComponents {
    onVariants(selector().all()) { variant ->
        afterEvaluate {
            val protoTask =
                project.tasks.getByName("generate" + variant.name.replaceFirstChar { it.uppercaseChar() } + "Proto") as GenerateProtoTask
            val kspTask = project.tasks.getByName("ksp" + variant.name.replaceFirstChar { it.uppercaseChar() } + "Kotlin")
            kspTask.dependsOn(protoTask)
            project.extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension>("kotlin") {
                sourceSets.getByName(variant.name) {
                    kotlin.srcDir(protoTask.outputBaseDir)
                }
            }
        }
    }
}

dependencies {
    // Bouncy Castle
    implementation(libs.bcprov.jdk18on)
    implementation(libs.bcpkix.jdk18on)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)

    // Material
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // ProtoBuf
    runtimeOnly(libs.grpc.okhttp)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.okhttp)
    implementation(libs.grpc.protobuf.lite)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.kotlin.lite)

    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.security.crypto)
    implementation(libs.mlkit.barcode)

    // Digital Credentials API
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play)
    implementation(libs.androidx.registry.digitalcredentials.mdoc)
    implementation(libs.androidx.registry.digitalcredentials.openid)
    implementation(libs.androidx.registry.provider)
    implementation(libs.play.services.identity.credentials)
    implementation(libs.androidx.registry.provider.play.services)

    implementation(libs.cose.java) {
        exclude(group = "org.bouncycastle", module = "bcpkix-lts8on")
        exclude(group = "org.bouncycastle", module = "bcprov-lts8on")
    }
    implementation(libs.waltid.mdoc.credentials) {
        exclude(group = "org.bouncycastle", module = "bcpkix-lts8on")
        exclude(group = "org.bouncycastle", module = "bcprov-lts8on")
    }

    // Ktor
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // AndroidX Room
    implementation(libs.bundles.androidx.room)
    ksp(libs.androidx.room.compiler)

    // KotlinX
    implementation(libs.kotlinx.datetime)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    // Navigation
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Logging
    implementation(libs.slf4j.android)
    implementation(libs.slf4j.simple)

    // EUDIW
    implementation(libs.eudi.openid4vp)
    implementation(libs.eudi.prex)
    implementation(libs.eudi.openid4vci)
    implementation(libs.eudi.sdjwt)
    implementation(libs.eudi.iso18013.data.transfer)
    implementation(libs.eudi.document.manager)

    // Longfellow ZK prover: bundles libzkp.so (arm64-v8a, x86_64) and the pre-built circuits
    implementation(libs.multipaz.longfellow)

    // multipaz-longfellow needs a newer multipaz than eudi-iso18013-data-transfer pins. Constrain
    // the multipaz artifacts it pulls in transitively so core and -android stay on one version.
    constraints {
        implementation(libs.multipaz.core)
        implementation(libs.multipaz.android)
        implementation(libs.multipaz.android.legacy)
    }

    implementation(libs.zxing.core)
    implementation(libs.jackson.databind)

    // Arrow for functional programming
    implementation(libs.arrow.core)
    implementation(libs.arrow.fx.coroutines)

    // DSS Android for LOTL certificate fetching (Android-compatible fork)
    // Exclude old Bouncy Castle jdk15on - we use jdk18on
    implementation(libs.dss.service) {
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.dss.tsl.validation) {
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.dss.utils.google.guava) {
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.jackson.module.kotlin)
    implementation(libs.sha2)

    // Local Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.slf4j.simple) {
        exclude(
            group = libs.slf4j.android.get().group,
            module = libs.slf4j.android.get().module.name
        )
    }

    // Dev tools
    debugImplementation(libs.compose.ui.tooling)
    debugRuntimeOnly(libs.compose.ui.test.manifest)
}

ksp {
    // The schemas directory contains a schema file for each version of the Room database.
    // This is required to enable Room auto migrations.
    // See https://developer.android.com/reference/kotlin/androidx/room/AutoMigration.
    arg(RoomSchemaArgProvider(File(projectDir, "schemas")))
}

/**
 * https://issuetracker.google.com/issues/132245929
 * [Export schemas](https://developer.android.com/training/data-storage/room/migrating-db-versions#export-schemas)
 */
class RoomSchemaArgProvider(
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaDir: File
) : CommandLineArgumentProvider {
    override fun asArguments() = listOf("room.schemaLocation=${schemaDir.path}")
}

fun VariantDimension.configureEnvironment(env: Environment) {
    stringBuildConfig("CLIENT_USER_AGENT", "ee.cyber.wallet")

    when (env) {
        Environment.LOCAL -> "http://localhost:6565"
        Environment.TEST -> "https://eudi-wallet-provider.example.com"
        Environment.PROD -> "https://eudi-wallet-provider.example.com"
    }.also { stringBuildConfig("WALLET_PROVIDER_RPC_URL", it) }

    when (env) {
        Environment.LOCAL -> "http://localhost:4200"
        Environment.TEST -> "https://eudi-issuer.example.com"
        Environment.PROD -> "https://eudi-issuer.example.com"
    }.also { stringBuildConfig("PID_PROVIDER_URL", it) }

    when (env) {
        Environment.LOCAL -> "http://localhost:6543"
        Environment.TEST -> "https://eudi-issuer.example.com"
        Environment.PROD -> "https://eudi-issuer.example.com"
    }.also { stringBuildConfig("PID_PROVIDER_RPC_URL", it) }

    when (env) {
        Environment.LOCAL -> "https://localhost:13443"
        Environment.TEST -> "https://eudi-issuer-01.example.com"
        Environment.PROD -> "https://eudi-issuer-01.example.com"
    }.also { stringBuildConfig("ISSUER_URL", it) }

    when (env) {
        Environment.LOCAL -> "eudi-wallet.localhost"
        Environment.TEST -> "eudi-wallet.localhost"
        Environment.PROD -> "eudi-wallet.localhost"
    }.also { stringBuildConfig("CLIENT_ID", it) }

    val schema = "haip://"
    stringBuildConfig("DEEP_LINK_SCHEMA", schema)
    stringBuildConfig("PRESENTATION_REDIRECT_URI", "${schema}present")
    stringBuildConfig("ISSUE_REDIRECT_URI", "${schema}issue")
    stringBuildConfig("PID_REDIRECT_URI", "${schema}pid")

    when (env) {
        Environment.PROD -> ClientLogLevels.NONE
        else -> ClientLogLevels.ALL
    }.also { clientLogLevelConfig(it) }

    intBuildConfig("CLIENT_CONNECT_TIMEOUT_SECONDS", 10)
    intBuildConfig("CLIENT_READ_TIMEOUT_SECONDS", 60)
    intBuildConfig("CLIENT_WRITE_TIMEOUT_SECONDS", 30)

    // LOTL (List of Trusted Lists) configuration
    boolBuildConfig("LOTL_ENABLED", true)
    when (env) {
        Environment.LOCAL -> "https://trustedlist.serviceproviders.eudiw.dev/LOTL/01.xml"
        Environment.TEST -> "https://trustedlist.serviceproviders.eudiw.dev/LOTL/01.xml"
        Environment.PROD -> "https://ec.europa.eu/tools/lotl/eu-lotl.xml"
    }.also { stringBuildConfig("LOTL_LOCATION", it) }
    stringBuildConfig("LOTL_SERVICE_TYPE_IDENTIFIER", "http://uri.etsi.org/Svc/Svctype/CA/RPaccess")
    boolBuildConfig("LOTL_ENABLED", true)
    boolBuildConfig("USE_MOCKS", false)
}

enum class ClientLogLevels {
    ALL, HEADERS, BODY, INFO, NONE
}

fun VariantDimension.clientLogLevelConfig(level: ClientLogLevels) {
    stringBuildConfig("CLIENT_LOG_LEVEL", level.name)
}

enum class Environment {
    LOCAL, TEST, PROD
}

object BuildConfigTypes {
    const val BOOLEAN = "boolean"
    const val STRING = "String"
    const val INT = "int"
}

object BuildConfigValues {
    const val TRUE = "true"
    const val FALSE = "false"
}

fun VariantDimension.boolBuildConfig(name: String, value: Boolean) {
    buildConfigField(BuildConfigTypes.BOOLEAN, name, if (value) BuildConfigValues.TRUE else BuildConfigValues.FALSE)
}

fun VariantDimension.intBuildConfig(name: String, value: Int) {
    buildConfigField(BuildConfigTypes.INT, name, value.toString())
}

fun VariantDimension.longBuildConfig(name: String, value: Long) {
    buildConfigField(BuildConfigTypes.INT, name, value.toString())
}

fun VariantDimension.stringBuildConfig(name: String, value: String) {
    buildConfigField(BuildConfigTypes.STRING, name, value.quoted())
}

fun String.quoted(): String = "\"$this\""

fun readLocalProperties(): Properties =
    Properties().apply {
        val file = file("../local.properties")
        if (file.exists()) {
            load(FileInputStream(file))
        }
    }
