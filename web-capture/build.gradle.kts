@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.internal.os.OperatingSystem
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
        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        val androidMain by getting {
            dependsOn(jvmAndAndroidMain)
        }
        val desktopMain by getting {
            dependsOn(jvmAndAndroidMain)
        }
        commonMain.dependencies {
            implementation(libs.jetbrains.kotlinx.coroutines.core)
            implementation(libs.androidx.room3.runtime)
        }
    }

}

android {
    namespace = "com.sakethh.linkora.web_capture"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    sourceSets["main"].jniLibs.srcDirs(
        project.layout.buildDirectory.dir("jniLibs")
    )
}

dependencies {
    ksp(libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
}

private val rustBasePath = layout.projectDirectory.asFile
private val jniLibsDir = layout.buildDirectory.dir("jniLibs")

tasks.register("cargoBuildAndroid") {
    group = "rust"
    doLast {
        val ndkBase = file("${System.getProperty("user.home")}/Android/Sdk/ndk")
        val ndkDir = ndkBase.listFiles()?.maxOrNull()
            ?: throw GradleException("NDK not found in $ndkBase. Please install it via Android Studio SDK Manager.")

        listOf(
            "aarch64-linux-android",
            "x86_64-linux-android",
            "armv7-linux-androideabi"
        ).forEach { target ->
            exec {
                workingDir = rustBasePath
                environment("ANDROID_NDK_HOME", ndkDir.absolutePath)
                commandLine(
                    "cargo",
                    "ndk",
                    "-t",
                    target,
                    "-o",
                    jniLibsDir.get().asFile.absolutePath,
                    "build",
                    "--release"
                )
            }
        }
    }
}

tasks.register("cargoBuildDesktop") {
    group = "rust"
    doLast {
        val currentOs = OperatingSystem.current()
        val (desktopTarget, binaryName) = when {
            currentOs.isLinux -> "x86_64-unknown-linux-gnu" to "libweb_capture.so"
            currentOs.isWindows -> "x86_64-pc-windows-msvc" to "web_capture.dll"
            else -> throw GradleException("Unsupported host operating system for building monolith-binding")
        }

        exec {
            workingDir = rustBasePath
            commandLine("cargo", "build", "--release", "--target", desktopTarget)
        }

        val libDir = layout.buildDirectory.dir("rustLibs/desktop").get().asFile
        libDir.mkdirs()

        val sourceFile = File("$rustBasePath/target/$desktopTarget/release/$binaryName")

        if (sourceFile.exists()) {
            println("Copying native lib from: ${sourceFile.absolutePath}")
            println("To: ${libDir.absolutePath}")
            copy {
                from(sourceFile)
                into(libDir)
            }
        } else {
            throw GradleException("Rust build failed or output file not found at: ${sourceFile.absolutePath}")
        }
    }
}

tasks.matching { it.name == "processJvmResources" || it.name == "jvmProcessResources" }
    .configureEach {
        dependsOn("cargoBuildDesktop")
    }