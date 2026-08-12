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
    description = "Builds the Rust desktop library for the current desktop platform(s) and stages architecture-specific outputs"
    outputs.dir(desktopLibDir)

    doLast {
        val currentOs = OperatingSystem.current()
        val libRoot = desktopLibDir.get().asFile

        libRoot.deleteRecursively()
        libRoot.mkdirs()

        val requestedTarget = providers.gradleProperty("desktopRustTarget").orNull
        val targetsAndOutputDirs =
            when {
                currentOs.isWindows ->
                    listOf(
                        "x86_64-pc-windows-msvc" to "windows-x64",
                        "aarch64-pc-windows-msvc" to "windows-arm64",
                    ).let { supported ->
                        if (requestedTarget == null) {
                            supported
                        } else {
                            supported.filter { it.first == requestedTarget }.ifEmpty {
                                throw GradleException("Unsupported desktopRustTarget on Windows: $requestedTarget")
                            }
                        }
                    }
                currentOs.isLinux ->
                    listOf("x86_64-unknown-linux-gnu" to "linux-x64").let { supported ->
                        if (requestedTarget == null || requestedTarget == supported.single().first) {
                            supported
                        } else {
                            throw GradleException("Unsupported desktopRustTarget on Linux: $requestedTarget")
                        }
                    }
                else ->
                    throw GradleException("Unsupported host operating system for building web-capture")
            }

        targetsAndOutputDirs.forEach { (desktopTarget, resourceDir) ->
            val binaryName =
                when {
                    currentOs.isWindows -> "web_capture.dll"
                    currentOs.isLinux -> "libweb_capture.so"
                    else -> error("Unsupported host operating system")
                }

            println("Building Rust desktop target: $desktopTarget")

            exec {
                workingDir = rustBasePath
                commandLine("cargo", "build", "--release", "--target", desktopTarget)
            }

            val sourceFile = File("$rustBasePath/target/$desktopTarget/release/$binaryName")
            if (!sourceFile.exists()) {
                throw GradleException(
                    "Rust build succeeded but output file was not found at: ${sourceFile.absolutePath}",
                )
            }

            val outputDir = File(libRoot, resourceDir)
            outputDir.mkdirs()

            println("Copying native lib from: ${sourceFile.absolutePath}")
            println("To: ${outputDir.absolutePath}")

            copy {
                from(sourceFile)
                into(outputDir)
            }
        }

        println("Staged desktop native libraries:")
        libRoot.walkTopDown()
            .filter { it.isFile }
            .forEach { println("  ${it.relativeTo(libRoot)}") }
    }
}

tasks.named<Test>("desktopTest") {
    dependsOn("cargoBuildDesktop")

    val rustLibDir = desktopLibDir.get().asFile
    jvmArgs("-Djava.library.path=${rustLibDir.absolutePath}")
    environment("LD_LIBRARY_PATH", rustLibDir.absolutePath)
}

tasks
    .matching { it.name == "processJvmResources" || it.name == "jvmProcessResources" }
    .configureEach {
        dependsOn("cargoBuildDesktop")
    }

tasks
    .matching {
        it.name.startsWith("merge") && it.name.endsWith("JniLibFolders")
    }.configureEach {
        dependsOn(cargoBuildAndroid)
    }
