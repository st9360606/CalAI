import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)        // ✅ 留這個就好
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    alias(libs.plugins.baselineprofile)
    // ✅ 明確指定 Kotlin Serialization 插件版本（請與你的 Kotlin 版本一致）
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
}

@Suppress("UnstableApiUsage")
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

        // 預設 app 顯示名稱（不覆蓋多語字串）
        manifestPlaceholders["appLabel"] = "BiteCal"
    }

    // ── 讀取 keystore.properties（兩個常見路徑；找不到就跳過簽章） ──
    val keystoreProps: Properties = Properties().apply {
        val candidates = listOf(
            rootProject.file("keystore/keystore.properties"),
            rootProject.file("android/keystore/keystore.properties")
        )
        val f = candidates.firstOrNull { it.exists() }
        if (f != null) {
            f.inputStream().use { load(it) }
        } else {
            // 可選：用環境變數跑 CI
            setProperty("storeFile", System.getenv("KEYSTORE_FILE") ?: "")
            setProperty("storePassword", System.getenv("KEYSTORE_STORE_PASSWORD") ?: "")
            setProperty("keyAlias", System.getenv("KEYSTORE_ALIAS") ?: "")
            setProperty("keyPassword", System.getenv("KEYSTORE_KEY_PASSWORD") ?: "")
        }
    }

    signingConfigs {
        val storeFilePath = keystoreProps.getProperty("storeFile")?.trim().orEmpty()
        if (storeFilePath.isNotEmpty()) {
            val sf = rootProject.file(storeFilePath)
            if (sf.exists()) {
                create("release") {
                    storeFile = sf
                    storePassword = keystoreProps.getProperty("storePassword")
                    keyAlias = keystoreProps.getProperty("keyAlias")
                    keyPassword = keystoreProps.getProperty("keyPassword")
                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                    enableV4Signing = true
                }
            } else {
                logger.warn("signingConfig: keystore not found at $storeFilePath")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-rules.pro")
            )
            manifestPlaceholders["appLabel"] = "BiteCal"
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            manifestPlaceholders["appLabel"] = "BiteCal (debug)"
        }
    }

    // ── 環境切分（dev / prod） ──────────────────────────────
    flavorDimensions += "env"
    productFlavors {
        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
            manifestPlaceholders["appLabel"] = "BiteCal (dev)"
        }
        create("devWifi") {
            dimension = "env"
            applicationIdSuffix = ".devwifi"
            versionNameSuffix = "-devwifi"
            buildConfigField("String","BASE_URL","\"http://172.20.10.9:8080/\"")
            manifestPlaceholders["appLabel"] = "BiteCal (devWifi)"
        }
        create("devUsb") {
            dimension = "env"
            applicationIdSuffix = ".devusb"
            versionNameSuffix = "-devusb"
            buildConfigField("String","BASE_URL","\"http://127.0.0.1:8080/\"")
            manifestPlaceholders["appLabel"] = "BiteCal (devUsb)"
        }
        create("prod") {
            dimension = "env"
            // TODO: 之後換正式域名
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
            manifestPlaceholders["appLabel"] = "BiteCal"
        }
    }

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
    // Compose / AndroidX（以 BOM 對齊）
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.startup.runtime)
    "baselineProfile"(project(":baselineprofile"))
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 測試
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
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // ✅ 移除重複

    // ViewModel（Compose）
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    kapt("com.squareup:javapoet:1.13.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // 測試輔助
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation(kotlin("test"))

    // Splash
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Material / AppCompat
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Material Icons（用 BOM）
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    // 既有網路層（如需保留 Moshi）
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("androidx.activity:activity-ktx:1.9.2")

    // Credential Manager + Google ID
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Kotlinx Serialization（Retrofit 轉換器）
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    implementation("com.google.code.gson:gson:2.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

}

kapt {
    correctErrorTypes = true
}

configurations.configureEach {
    // 固定 javapoet 版本（避免相依拉錯）
    resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    resolutionStrategy.eachDependency {
        if (requested.group == "com.squareup" && requested.name == "javapoet") {
            useVersion("1.13.0")
        }
    }
}
