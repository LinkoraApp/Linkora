plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeHotReload) apply false
    id("com.mikepenz.aboutlibraries.plugin") version "13.1.0" apply false
    alias(libs.plugins.stability.analyzer) apply false
    id("androidx.room3") version "3.0.0-alpha01" apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    id("com.diffplug.spotless") version "8.8.0"
}

tasks.register("installGitHooks") {
    description = "Configures Git to use the .githooks directory and sets executable permissions"
    onlyIf {
        val currentPath =
            try {
                Runtime
                    .getRuntime()
                    .exec("git config core.hooksPath")
                    .inputStream
                    .bufferedReader()
                    .readText()
                    .trim()
            } catch (e: Exception) {
                ""
            }
        currentPath != ".githooks"
    }

    doLast {
        exec { commandLine("git", "config", "core.hooksPath", ".githooks") }
        file(".githooks").listFiles()?.forEach { it.setExecutable(true) }
        println("Git hooks installed successfully.")
    }
}

tasks.named("build") {
    dependsOn("installGitHooks")
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
    val files = project.findProperty("spotlessFiles") as? String
    val targetFiles = files?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }

    kotlin {
        if (targetFiles != null) {
            target(targetFiles)
        } else {
            target("**/src/**/*.kt")
        }

        targetExclude("**/build/**", "**/build-generated/**", "**/bin/**")

        ktlint("1.8.0").editorConfigOverride(
            mapOf(
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_filename" to "disabled",
                "ktlint_standard_package-name" to "disabled",
                "ktlint_standard_max-line-length" to "disabled",
                "ktlint_standard_no-consecutive-comments" to "disabled",
                "ktlint_standard_enum-entry-name-case" to "disabled",
                "ktlint_standard_backing-property-naming" to "disabled",
            ),
        )

        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(4)
    }

    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint("1.8.0")
    }
}

tasks.register<Exec>("cargoTest") {
    group = "verification"
    description = "Runs Rust tests in the web-capture module"
    workingDir = file("web-capture")
    commandLine("cargo", "test", "--", "--test-threads=1")
}

tasks.register("verifyAll") {
    group = "verification"
    description = "Runs all Rust and Kotlin Compose tests"

    dependsOn("cargoTest", ":composeApp:desktopTest")
}
