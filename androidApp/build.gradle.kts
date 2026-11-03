import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Widgets: Compose Color (card shades) and coroutines (refresh scope). Both catalog entries already exist.
    implementation(libs.compose.ui)
    implementation(libs.kotlinx.coroutines.core)

    // Cloud sync while the app is closed.
    implementation(libs.androidx.work.runtime)
    // Google Calendar import: Google Identity Authorization API (calendar.readonly), Android only.
    implementation(libs.play.services.auth)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.example.reclaimapp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.reclaimapp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        // Backend base URL. Override with -PnixuraApiBaseUrl=https://api.yourdomain.com (release must be https).
        buildConfigField("String", "NIXURA_API_BASE_URL", "\"${(project.findProperty("nixuraApiBaseUrl") as String?) ?: "https://api.nixura.example"}\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            // Emulator reaches the host machine at 10.0.2.2 (cleartext is allowed in debug builds only).
            buildConfigField("String", "NIXURA_API_BASE_URL", "\"${(project.findProperty("nixuraDebugApiBaseUrl") as String?) ?: "http://10.0.2.2:8080"}\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}