plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    // ← 取消 version，改由 root buildscript 提供
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace = "com.calai.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.calai.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // ⚠️ 不要在這裡放 BASE_URL，交給 flavor 管
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // 網路 log 由 OkHttp logging-interceptor 處理
        }
    }

    // ── 環境切分（dev/prod） ──────────────────────────────
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            // 模擬器打本機用 10.0.2.2
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
        }
        create("prod") {
            dimension = "env"
            // 之後換正式網域（建議 https）
            buildConfigField("String", "BASE_URL", "\"https://api.yourdomain.com/\"")
        }
    }

    // ── 語言等級 ────────────────────────────────────────
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose / AndroidX（沿用你的 version catalog）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Retrofit / OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ViewModel（Compose）
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")

    // 保險：KAPT 還是明確帶 1.13.0
    kapt("com.squareup:javapoet:1.13.0")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    implementation("androidx.datastore:datastore-preferences:1.1.1")


}

kapt {
    correctErrorTypes = true
}

/**
 * 修正 Hilt 編譯時 JavaPoet 被拉成舊版，導致
 * ClassName.canonicalName() 找不到的問題
 */
// ★ 強制鎖定 JavaPoet 版本（包含 kapt/compile classpath）
configurations.configureEach {
    resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    resolutionStrategy.eachDependency {
        if (requested.group == "com.squareup" && requested.name == "javapoet") {
            useVersion("1.13.0")
        }
    }
}

