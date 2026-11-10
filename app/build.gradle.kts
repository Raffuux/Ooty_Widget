plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
val planBaseUrl = (project.findProperty("planBaseUrl") as String?) ?: ""

android {
    namespace = "fr.rafu.ooty"
    compileSdk = 34

    defaultConfig {
        applicationId = "fr.rafu.ooty"
        minSdk = 29
        targetSdk = 34
        versionCode = runNumber
        versionName = "1.$runNumber"
        buildConfigField("String", "PLAN_BASE_URL", "\"$planBaseUrl\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("signing/release.keystore")
            storePassword = "ootywidget"
            keyAlias = "ooty"
            keyPassword = "ootywidget"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
