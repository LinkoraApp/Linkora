plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    id("com.mikepenz.aboutlibraries.plugin") version "13.1.0" apply false
    alias(libs.plugins.stability.analyzer) apply false
    id("androidx.room3") version "3.0.0-alpha01" apply false
}