plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    id("androidx.room3")
    alias(libs.plugins.ksp)
}

kotlin {
    wasmJs {
        browser {
            outputModuleName = "webApp"

            commonWebpackConfig {
                outputFileName = "webApp.js"
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
        webMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":web-capture"))
            implementation(libs.compose.ui)
            implementation(libs.navigation.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.jetbrains.material3)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.kotlinx.browser)

            implementation(npm("sqlite-wasm-worker", layout.projectDirectory.dir("worker").asFile))
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.serialization.json)
            api(libs.androidx.sqlite.web)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
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
