@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class) wasmJs {
        browser {
            outputModuleName = "composeApp"
            commonWebpackConfig {
                outputFileName = "composeApp.js"
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
        }
        desktopTest.dependencies {
            implementation(libs.mockk)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

android {
    namespace = "com.sakethh.linkora"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sakethh.linkora"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 48
        versionName = "0.16.0"
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
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
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
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            applicationIdSuffix = ".preview"
            versionNameSuffix = "-preview"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets["main"].res.srcDirs(
        "src/commonMain/resources", "src/androidMain/resources"
    )

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    ksp(libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
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
                TargetFormat.Exe
            )
            packageName = "Linkora"
            this.vendor = "Saketh Pathike"
            this.packageVersion = "1.0.14"/*

            This logo (src/desktopMain/resources/logo.*) was painted by `mondstern`.
            The original post can be found here: https://pixelfed.social/p/mondstern/747494483548287527

             */

            windows {
                this.iconFile.set(project.file("src/desktopMain/resources/logo.ico"))
            }

            linux {
                this.iconFile.set(project.file("src/desktopMain/resources/logo.png"))
            }
            modules("jdk.unsupported")
            modules("jdk.unsupported.desktop")
        }
    }
}


val addNetlifyHeadersToDist = task("addNetlifyHeadersToDist") {
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
            """.trimIndent()
        )
        println("Wrote Netlify headers to: ${headersFile.absolutePath}")
    }
}

tasks.named("wasmJsBrowserDistribution") {
    finalizedBy(addNetlifyHeadersToDist)
}