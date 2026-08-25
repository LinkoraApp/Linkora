@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
    id("com.android.library")
    id("androidx.room3")
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    wasmJs {
        browser()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.kotlinx.coroutines.core)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.kapture)
            implementation(libs.atomicfu)
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test.v1110)
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.cio)
            }
        }
    }
}

android {
    namespace = "com.sakethh.linkora.web_capture"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspDesktop", libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
}
