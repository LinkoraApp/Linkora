@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidKMPLibrary)
    id("androidx.room3")
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

kotlin {
    android {
        compileSdk {
            namespace = "com.sakethh.linkora.web_capture"
            version = release(37)
        }

        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }

    jvm("desktop") {
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
dependencies {
    add("kspDesktop", libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
    add("kspAndroid", libs.androidx.room3.compiler)
}
