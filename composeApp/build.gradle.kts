@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.android.build.gradle.internal.tasks.factory.dependsOn
import groovy.json.JsonSlurper
import org.gradle.internal.os.OperatingSystem
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform") version "2.3.10"
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.3.10"
    alias(libs.plugins.ksp)
    id("androidx.room3")
    id("com.mikepenz.aboutlibraries.plugin")
    id("com.mikepenz.aboutlibraries.plugin.android")
    alias(libs.plugins.stability.analyzer)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            outputModuleName = "composeApp"

            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }

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
            implementation(libs.ktor.client.android)
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

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqlite.bundled)
            implementation(libs.ktor.client.java)
            implementation(libs.androidx.datastore.preferences.core)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.serialization.json)
            api(libs.androidx.sqlite.web)
            implementation(npm("sqlite-wasm-worker", layout.projectDirectory.dir("worker").asFile))
            implementation(libs.kotlinx.browser)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.assertk)
            implementation(libs.ktor.client.mock)
        }

        desktopTest.dependencies {
            implementation(libs.mockk)
            implementation(libs.kotlinx.coroutines.test.v1110)
            implementation(libs.ktor.server.core)
            implementation(libs.ktor.server.cio)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.sakethh.linkora"

    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    defaultConfig {
        applicationId = "com.sakethh.linkora"

        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()

        versionCode = 55
        versionName = "0.21.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        register("preview") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
            matchingFallbacks += listOf("release", "debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets["main"].res.srcDirs(
        "src/commonMain/resources",
        "src/androidMain/resources",
    )

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    add("kspWasmJs", libs.androidx.room3.compiler)
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspDesktop", libs.androidx.room3.compiler)
}

compose.desktop {
    application {
        mainClass = "com.sakethh.linkora.MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.AppImage,
                TargetFormat.Rpm,
                TargetFormat.Pkg,
                TargetFormat.Exe,
            )

            packageName = "Linkora"
            this.vendor = "Saketh Pathike"
            this.packageVersion = "1.0.20"

            windows {
                this.iconFile.set(project.file("src/desktopMain/resources/logo.ico"))
            }

            linux {
                this.iconFile.set(project.file("src/desktopMain/resources/logo.png"))
            }

            modules("jdk.unsupported")
            modules("jdk.unsupported.desktop")

            jvmArgs +=
                if (OperatingSystem.current().isWindows) {
                    "-Djava.library.path=%APPDIR%;\$APPDIR/resources;\$APPDIR/resources/windows-x64;\$APPDIR/resources/windows-arm64"
                } else {
                    "-Djava.library.path=\$APPDIR/resources:\$APPDIR/resources/linux-x64"
                }
        }
    }
}

val addNetlifyHeadersToDist =
    task("addNetlifyHeadersToDist") {
        doLast {
            val prodDistDir =
                File(layout.projectDirectory.asFile, "/build/dist/wasmJs/productionExecutable")

            val headersFile = File(prodDistDir, "_headers")
            if (!headersFile.exists()) {
                headersFile.createNewFile()
            }

            headersFile.writeText(
                """
                /*
                Cross-Origin-Opener-Policy: same-origin
                Cross-Origin-Embedder-Policy: require-corp
                """.trimIndent(),
            )

            println("Wrote Netlify headers to: ${headersFile.absolutePath}")
        }
    }

tasks.named("wasmJsBrowserDistribution") {
    finalizedBy(addNetlifyHeadersToDist)
}

allprojects {
    configurations.configureEach {
        if (name.contains("wasm", ignoreCase = true)) {
            resolutionStrategy {
                force(
                    "org.jetbrains.kotlin:kotlin-stdlib:2.3.10",
                    "org.jetbrains.kotlin:kotlin-stdlib-wasm-js:2.3.10",
                    "org.jetbrains.kotlin:kotlin-stdlib-js:2.3.10",
                    "org.jetbrains.kotlin:kotlin-stdlib-common:2.3.10",
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
            val generatedStringsFile = File(localizationBuildDir.get().asFile, "LocalizedStrings.kt")
            if (!generatedStringsFile.exists()) {
                generatedStringsFile.createNewFile()
            }

            val enumBuilder = StringBuilder()
            val classBuilder = StringBuilder()

            enumBuilder.append("enum class LocalizationKey {")
            classBuilder.append(
                """
                import LocalizationKey

                class LocalizedStrings(private val values: Map<String, String>) {

                     private fun raw(key: LocalizationKey, defaultValue: String): String {
                         return values[key.name] ?: defaultValue
                     }

                     companion object {
                         val Default = LocalizedStrings(mapOf())
                     }
                """.trimIndent(),
            )

            localizationItems.forEach { (enumName, defaultValue) ->
                enumBuilder.append("\n\t$enumName,")
                val escapedDefaultValue = defaultValue.replace("\n", "\\n").replace("\"", "\\\"")
                classBuilder.append("\n\n\tval $enumName = raw(LocalizationKey.$enumName, \"$escapedDefaultValue\")")
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
