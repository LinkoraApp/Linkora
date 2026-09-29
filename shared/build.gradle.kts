import com.android.build.gradle.internal.tasks.factory.dependsOn
import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform") version "2.4.20"
    alias(libs.plugins.androidKMPLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.4.20"
    alias(libs.plugins.ksp)
    id("androidx.room3")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.mikepenz.aboutlibraries.plugin.android")
    alias(libs.plugins.stability.analyzer)
}

group = "com.sakethh.linkora.shared"

kotlin {

    android {
        namespace = "com.sakethh.linkora.shared"

        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()

        // https://youtrack.jetbrains.com/issue/CMP-9547
        androidResources.enable = true

        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    wasmJs {
        browser {
            testTask {
                useKarma {
                    useFirefox()
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        val desktopWebMain by creating {
            dependsOn(commonMain.get())
        }

        desktopMain.dependsOn(desktopWebMain)
        wasmJsMain.get().dependsOn(desktopWebMain)

        val androidDesktopMain by creating {
            dependsOn(commonMain.get())

            dependencies {
                implementation(libs.androidx.datastore.preferences.core)
            }
        }

        desktopMain.dependsOn(androidDesktopMain)
        androidMain.get().dependsOn(androidDesktopMain)

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.work.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.androidx.documentfile)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.datastore.preferences.core)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.jetbrains.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(compose.materialIconsExtended)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.async)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.fleeksoft.ksoup)
            implementation(libs.ksoup.network)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.ktor.client.logging)
            implementation(libs.kotlinx.datetime)
            implementation(libs.stately.concurrency)
            implementation(libs.stately.concurrent.collections)
            implementation(libs.adaptive.core)
            implementation(libs.adaptive.layout)
            implementation(libs.adaptive.navigation)
            implementation("com.composables:composeunstyled:1.49.6")
            implementation(project(":web-capture"))
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.assertk)
            implementation(libs.ktor.client.mock)
        }

        desktopTest.dependencies {
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
            implementation(libs.sqlite.bundled)
        }
    }
}
compose.resources {
    packageOfResClass = "com.sakethh.linkora.shared.generated.resources"
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspWasmJs", libs.androidx.room3.compiler)
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspDesktop", libs.androidx.room3.compiler)
    androidRuntimeClasspath(libs.compose.uiTooling)
}

allprojects {
    configurations.configureEach {
        if (name.contains("wasm", ignoreCase = true)) {
            resolutionStrategy {
                force(
                    "org.jetbrains.kotlin:kotlin-stdlib:2.4.20",
                    "org.jetbrains.kotlin:kotlin-stdlib-wasm-js:2.4.20",
                    "org.jetbrains.kotlin:kotlin-stdlib-js:2.4.20",
                    "org.jetbrains.kotlin:kotlin-stdlib-common:2.4.20",
                )
            }
        }
    }
}

val localizationDir = "generated/com/sakethh/linkora/localization"

data class LocalizationItem(
    val key: String,
    val defaultValue: String,
)

val localizationJsonFile =
    rootProject.layout.projectDirectory.file("locales/default.json")
val localizationItems =
    (JsonSlurper().parse(localizationJsonFile.asFile) as List<*>)
        .map {
            (it as Map<*, *>).run {
                LocalizationItem(
                    key = get("key").toString(),
                    defaultValue = get("defaultValue").toString(),
                )
            }
        }

val localizationGeneration =
    tasks.register("localizationGeneration") {
        doLast {
            val localizationBuildDir = layout.buildDirectory.dir(localizationDir)

            if (!localizationBuildDir.get().asFile.exists()) {
                localizationBuildDir.get().asFile.mkdirs()
            }

            val generatedKeysFile = File(localizationBuildDir.get().asFile, "LocalizationKey.kt")
            if (!generatedKeysFile.exists()) {
                generatedKeysFile.createNewFile()
            }
            val generatedStringsFile =
                File(localizationBuildDir.get().asFile, "LocalizedStrings.kt")
            if (!generatedStringsFile.exists()) {
                generatedStringsFile.createNewFile()
            }

            val enumBuilder = StringBuilder()
            val classBuilder = StringBuilder()

            enumBuilder.append("enum class LocalizationKey {")
            classBuilder.append(
                """
                class LocalizedStrings(values: Map<String, String>) {

                     companion object {
                         val Default = LocalizedStrings(mapOf())
                     }
                """.trimIndent(),
            )

            localizationItems.forEach { (enumName, defaultValue) ->
                enumBuilder.append("\n\t$enumName,")
                val escapedDefaultValue = defaultValue.replace("\n", "\\n").replace("\"", "\\\"")
                classBuilder.append(
                    "\n\n\tval $enumName = values[\"$enumName\"].takeUnless { it.isNullOrBlank() } ?: \"$escapedDefaultValue\"",
                )
            }

            enumBuilder.append("\n}")
            generatedKeysFile.writeText(enumBuilder.toString())

            classBuilder.append("\n}")
            generatedStringsFile.writeText(classBuilder.toString())
        }
    }

kotlin.sourceSets.named("commonMain") {
    kotlin.srcDir(layout.buildDirectory.dir(localizationDir))
}

val localizationVerification =
    tasks.register("localizationVerification") {
        doLast {
            val tempSet = mutableSetOf<String>()
            localizationItems.forEach { (enumKey, defaultValue) ->
                if (!tempSet.add(enumKey)) {
                    error("Duplicate localization key: \"$enumKey\". Localization keys must be unique.")
                }
                if (enumKey != defaultValue && !tempSet.add(defaultValue)) {
                    error(
                        "Duplicate localization default value: \"$defaultValue\" for key \"$enumKey\". " +
                            "Default values must be unique. Reuse the existing key that already contains this text.",
                    )
                }
            }
        }
    }

localizationGeneration.dependsOn(localizationVerification)

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(localizationVerification)
    dependsOn(localizationGeneration)
}

tasks.named("stabilityCheck") {
    dependsOn("compileProductionExecutableKotlinWasmJs")
    dependsOn("compileTestDevelopmentExecutableKotlinWasmJs")
    dependsOn("compileTestKotlinDesktop")
    dependsOn("compileTestKotlinWasmJs")
    dependsOn("compileAndroidMain")
}
