plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.protobuf)

    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}
