// Top-level build file. Plugins are declared here and applied in the :app module.

// AGP 9.0+ ships with built-in Kotlin support (no kotlin-android plugin). It depends on a
// baseline Kotlin Gradle plugin version; pin it UP to our chosen Kotlin so that the built-in
// Kotlin compiler and the Compose compiler plugin (org.jetbrains.kotlin.plugin.compose) match.
// Keep this version in sync with `kotlin` in gradle/libs.versions.toml.
// (The version catalog `libs` accessor is not available inside a buildscript {} block.)
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.oss.licenses) apply false
}
