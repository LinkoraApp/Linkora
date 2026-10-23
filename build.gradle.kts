import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidKMPLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeHotReload) apply false
    id("com.mikepenz.aboutlibraries.plugin") version "13.1.0" apply false
    alias(libs.plugins.stability.analyzer) apply false
    id("androidx.room3") version "3.0.1" apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.spotless) apply false
    id("org.jetbrains.kotlinx.atomicfu") version "0.33.0"
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.android) apply false
}

tasks.register("installGitHooks") {
    description = "Configures Git to use the .githooks directory and sets executable permissions"
    onlyIf {
        val currentPath =
            try {
                providers
                    .exec {
                        commandLine("git", "config", "core.hooksPath")
                    }
                    .standardOutput
                    .asText
                    .get()
                    .trim()
            } catch (e: Exception) {
                ""
            }
        currentPath != ".githooks"
    }

    doLast {
        providers
            .exec {
                commandLine("git", "config", "core.hooksPath", ".githooks")
            }
            .result
            .get()
        file(".githooks").listFiles()?.forEach { it.setExecutable(true) }
        println("Git hooks installed successfully.")
    }
}

allprojects {
    apply { plugin(rootProject.libs.plugins.spotless.get().pluginId) }
    configure<SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**", "**/build-generated/**", "**/bin/**")
            ktfmt().kotlinlangStyle()
        }

        kotlinGradle {
            target("**/*.gradle.kts")
            targetExclude("**/build/**")
            ktfmt().kotlinlangStyle()
        }
    }
}

tasks.named("build") {
    dependsOn("installGitHooks")
}
