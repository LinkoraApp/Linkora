@file:OptIn(ExperimentalWasmDsl::class)

import org.gradle.internal.os.OperatingSystem
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.ksp)
    id("com.android.library")
    id("androidx.room3")
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

private val rustBasePath = layout.projectDirectory.asFile
private val jniLibsDir = layout.buildDirectory.dir("jniLibs")
private val desktopLibDir = layout.buildDirectory.dir("rustLibs/desktop")

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

val cargoBuildAndroid =
    tasks.register("cargoBuildAndroid") {
        group = "rust"
        outputs.dir(jniLibsDir)
        doLast {
            val sdkPath =
                System.getenv("ANDROID_HOME")
                    ?: System.getenv("ANDROID_SDK_ROOT")
                    ?: file("${System.getProperty("user.home")}/Android/Sdk").absolutePath

            val ndkBase = file("$sdkPath/ndk")
            val ndkDir =
                ndkBase.listFiles()?.maxOrNull()
                    ?: throw GradleException("NDK not found in $ndkBase. Please install it via Android Studio SDK Manager.")

            val androidTargets =
                listOf(
                    "aarch64-linux-android",
                    "x86_64-linux-android",
                    "armv7-linux-androideabi",
                )

            println("Ensuring Android Rust targets are installed...")
            exec {
                commandLine("rustup", "target", "add", *androidTargets.toTypedArray())
            }

            val hasCargoNdk =
                try {
                    val output = ByteArrayOutputStream()
                    exec {
                        commandLine("cargo", "--list")
                        standardOutput = output
                    }
                    output.toString().contains(" ndk")
                } catch (e: Exception) {
                    false
                }

            if (!hasCargoNdk) {
                println("cargo-ndk is missing. Installing it now (this will take a moment)...")
                exec {
                    commandLine("cargo", "install", "cargo-ndk")
                }
            }

            androidTargets.forEach { target ->
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
                        "--release",
                    )
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
            isMinifyEnabled = false
        }
        register("preview") {
            matchingFallbacks += listOf("release", "debug")
        }
    }
    sourceSets["main"].jniLibs.setSrcDirs(listOf(cargoBuildAndroid))
}

dependencies {
    ksp(libs.androidx.room3.compiler)
    add("kspWasmJs", libs.androidx.room3.compiler)
}

tasks.register("cargoBuildDesktop") {
    group = "rust"
    description = "Builds the Rust desktop library and copies it into build/rustLibs/desktop"
    outputs.dir(desktopLibDir)

    doLast {
        val currentOs = OperatingSystem.current()

        val (desktopTarget, binaryName) =
            when {
                currentOs.isLinux -> "x86_64-unknown-linux-gnu" to "libweb_capture.so"
                currentOs.isWindows -> "x86_64-pc-windows-msvc" to "web_capture.dll"
                else -> throw GradleException("Unsupported host operating system for building web-capture")
            }

        exec {
            workingDir = rustBasePath
            commandLine("cargo", "build", "--release", "--target", desktopTarget)
        }

        val libDir = desktopLibDir.get().asFile
        libDir.deleteRecursively()
        libDir.mkdirs()

        val sourceFile = File("$rustBasePath/target/$desktopTarget/release/$binaryName")
        if (!sourceFile.exists()) {
            throw GradleException("Rust build failed or output file not found at: ${sourceFile.absolutePath}")
        }

        copy {
            from(sourceFile)
            into(libDir)
        }
    }
}

tasks
    .matching { it.name == "processJvmResources" || it.name == "jvmProcessResources" }
    .configureEach {
        dependsOn("cargoBuildDesktop")
    }

tasks.named<Test>("desktopTest") {
    dependsOn("cargoBuildDesktop")

    val rustLibDir = desktopLibDir.get().asFile
    jvmArgs("-Djava.library.path=${rustLibDir.absolutePath}")
    environment("LD_LIBRARY_PATH", rustLibDir.absolutePath)
}

tasks
    .matching {
        it.name.startsWith("merge") && it.name.endsWith("JniLibFolders")
    }.configureEach {
        dependsOn(cargoBuildAndroid)
    }
