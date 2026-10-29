plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

group = "com.gymmanager"
version = providers.gradleProperty("core.version").orElse("0.0.0-SNAPSHOT").get()

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets need a Mac host (Kotlin/Native). Windows/Linux: Android-only config.
    val hostIsMac = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
    if (hostIsMac) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64(),
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "GymManagerCore"
                isStatic = true
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Domain / sync deps land in later phases — keep scaffold empty of domain logic.
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

android {
    namespace = "com.gymmanager.core"
    compileSdk = 35
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
