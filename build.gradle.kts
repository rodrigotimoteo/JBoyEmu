import io.gitlab.arturbosch.detekt.Detekt

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.detekt)
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))

    source.setFrom(
        files(
            "app/src/main/java",
            "app/src/test/java",
            "kboyemucore/src/main/java",
            "kboyemucore/src/test/java",
        )
    )
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        xml.required.set(false)
        html.required.set(true)
        html.outputLocation.set(file("$rootDir/build/reports/detekt/detekt.html"))
    }
}
