plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.taskTree)
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    testImplementation(libs.bundles.kotlin.testing.common)
    testImplementation(libs.kotest.runner.junit5.jvm)
}

tasks.test {
    useJUnitPlatform()
}
