import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

// Detect if we're running as a subproject or standalone
val isSubproject = rootProject.name == "MobileIDSdk"

kotlin {
    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_11)
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    // iOS targets - Configure for iOS app
    iosX64 {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    iosArm64 {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    iosSimulatorArm64 {
        binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Dual-mode dependency: use project reference or prebuilt artifact
                if (isSubproject) {
                    implementation(project(":sdk"))
                } else {
                    // Standalone mode: use Maven repository with full dependency resolution
                    // Read version from VERSION file (look in parent dir for standalone mode)
                    val versionFile = File(project.rootDir.parentFile, "VERSION")
                    val sdkVersion = if (versionFile.exists()) {
                        versionFile.readText().trim()
                    } else {
                        "0.00.01" // fallback version
                    }
                    // KMP publishes Android as a separate artifact with -android suffix
                    // The artifact ID is based on the project name "sdk" from settings.gradle.kts
                    implementation("de.baltech:sdk-android:$sdkVersion")
                    // For iOS, the framework is linked via Xcode project
                    // Dependencies are automatically resolved from the AAR's POM
                }

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.components.resources)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.androidx.datastore.preferences)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(compose.preview)
                implementation("androidx.activity:activity-compose:1.8.0")
                implementation("androidx.datastore:datastore-preferences:1.1.1")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "de.baltech.mobileid_appnote_kotlin"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.baltech.mobileid_appnote_kotlin"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}