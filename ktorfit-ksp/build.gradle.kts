plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
    `maven-publish`
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.24-1.0.20")
    implementation("com.squareup:kotlinpoet:1.16.0")
    implementation("com.squareup:kotlinpoet-ksp:1.16.0")
    implementation(project(":ktorfit"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "ktorfit-ksp"
        }
    }
}
