plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

kotlin {
    androidTarget()
    jvm()
    iosArm64()
    iosSimulatorArm64()
    withSourcesJar()
}

android {
    namespace = "io.github.fpt.ktorfit"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "jvm") "ktorfit-jvm" else "ktorfit"
    }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    if (name != "generateMetadataFileForKotlinMultiplatformPublication") {
        enabled = false
    }
}