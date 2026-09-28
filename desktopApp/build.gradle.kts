import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    id("androidx.room3")
    alias(libs.plugins.ksp)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeHotReload)
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(libs.jetbrains.material3)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.sqlite.bundled)
    implementation(libs.ktor.client.cio)
    implementation(libs.androidx.datastore.preferences.core)

    implementation(project(":web-capture"))
    implementation(project(":shared"))
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
            this.packageVersion = "1.0.21"

            windows {
                this.iconFile.set(project.file("src/main/resources/logo.ico"))
            }

            linux {
                this.iconFile.set(project.file("src/main/resources/logo.png"))
            }

            modules("jdk.unsupported")
            modules("jdk.unsupported.desktop")

            jvmArgs +=
                if (org.gradle.internal.os.OperatingSystem
                        .current()
                        .isWindows
                ) {
                    "-Djava.library.path=%APPDIR%;\$APPDIR/resources;\$APPDIR/resources/windows-x64;\$APPDIR/resources/windows-arm64"
                } else {
                    "-Djava.library.path=\$APPDIR/resources:\$APPDIR/resources/linux-x64"
                }
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
