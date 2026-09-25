import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

pluginManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        maven("https://maven.waltid.dev/releases")
        maven("https://maven.waltid.dev/snapshots")
        google()
        mavenCentral()
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            mavenContent { snapshotsOnly() }
        }
        // Mirror of the signerry/aarmam fork artifacts (com.signerry.dss-android:1.02.08,
        // com.signerry:androidawt, com.signerry.santuario:xmlsec and the JAXB 3.1.6
        // rebuilds), published to gh-pages by .github/workflows/mirror.yml and pinned
        // by .github/maven-mirror/manifest.json. Static HTTPS, no credentials — fork
        // PRs build without secrets.
        maven {
            url = uri("https://tomkabel.github.io/eudi-wallet-poc/maven")
            content {
                includeGroup("com.signerry")
                includeGroup("com.signerry.dss-android")
                includeGroup("com.signerry.santuario")
                includeGroup("org.glassfish.jaxb")
                includeGroup("com.sun.xml.bind")
                includeGroup("com.sun.xml.bind.mvn")
            }
        }
    }
}
//enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "EE Wallet"
include(":app")

// JVM-only: multipaz-longfellow ships a desktop libzkp.so, so the ZK round trip runs in a plain
// unit test. It cannot live in :app — that module resolves the Android variant of multipaz and the
// two collide on the test classpath.
include(":zk-conformance")

// include eu-digital-identity-wallet libs sources into project if exist
val properties = Properties().apply {
    val localProperties = file("local.properties")
    if (localProperties.exists()) {
        localProperties.inputStream().use {
            load(it)
        }
    }
}

// TODO: remove this later
val euDigitalIdentityWalletPath = properties["eu-digital-identity-wallet-path"] as? String
if (euDigitalIdentityWalletPath != null && File(euDigitalIdentityWalletPath).exists()) {
    includeIfExists("$euDigitalIdentityWalletPath/eudi-lib-jvm-sdjwt-kt")
    includeIfExists("$euDigitalIdentityWalletPath/eudi-lib-jvm-siop-openid4vp-kt")
    includeIfExists("$euDigitalIdentityWalletPath/eudi-lib-jvm-openid4vci-kt")
    includeIfExists("$euDigitalIdentityWalletPath/eudi-lib-jvm-presentation-exchange-kt")
}

fun includeIfExists(path: String) {
    if (Files.exists(Path.of(path))) {
        println("including build $path")
        includeBuild(path)
    } else {
        println("skipping $path - not found")
    }
}
