plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.taskTree)
    alias(libs.plugins.kover)
    alias(libs.plugins.klint)
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.coroutines)
    implementation(libs.slf4j.api)
    implementation(libs.logback.core)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging.jvm)
    testImplementation(libs.bundles.kotlin.testing.common)
    testImplementation(libs.kotest.runner.junit5.jvm)
}

tasks.test {
    useJUnitPlatform()
}
