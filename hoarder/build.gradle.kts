import org.gradle.internal.os.OperatingSystem

plugins {
    id("java-library")
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(libs.jetbrains.kotlinx.coroutines.core)
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
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
            currentOs.isLinux -> "x86_64-unknown-linux-gnu" to "libhoarder.so"
            currentOs.isWindows -> "x86_64-pc-windows-msvc" to "hoarder.dll"
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