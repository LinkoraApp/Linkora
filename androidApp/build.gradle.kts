import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    id("androidx.room3")
    alias(libs.plugins.kotlin.android)
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    sourceSets["main"].res.srcDirs(
        "src/main/res",
    )

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-parameters")
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.compose.uiTooling)
    implementation(libs.jetbrains.material3)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.compose.ui)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(project(":web-capture"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.sqlite.bundled)
    implementation(libs.androidx.documentfile)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.androidx.datastore.preferences.core)
    implementation(project(":shared"))
}

room3 {
    schemaDirectory("$projectDir/schemas")
}
