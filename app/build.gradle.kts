import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.kotlin.serialization)
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

        /**
         * ✅ 重要：提供預設值，避免「沒有選到 flavor」或 IDE 索引時找不到欄位
         * 你的程式可以逐步從 BASE_URL 過渡到 API_BASE_URL
         */
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")
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

            // （可選）release 也能再覆蓋一次，但通常用 flavor 控就夠
            // buildConfigField("String", "API_BASE_URL", "\"https://api.yourdomain.com\"")
        }
        getByName("debug") {
            isMinifyEnabled = false
            isShrinkResources = false
            manifestPlaceholders["appLabel"] = "BiteCal (debug)"
        }
    }

    // ── 環境切分（dev / prod / devWifi / devUsb） ──────────────────────────────
    flavorDimensions += "env"
    productFlavors {

        create("dev") {
            dimension = "env"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            //buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")   //模擬器
            // ✅ 你原本用的（尾巴有 /）
            buildConfigField("String", "BASE_URL", "\"http://172.20.10.9:8080/\"") //同WIFI
            // ✅ 新增給你現在要用的（尾巴不要 /，方便你 concat path）
            buildConfigField("String", "API_BASE_URL", "\"http://172.20.10.9:8080\"")

            manifestPlaceholders["appLabel"] = "BiteCal (dev)"
        }

        create("devWifi") {
            dimension = "env"
            applicationIdSuffix = ".devwifi"
            versionNameSuffix = "-devwifi"

            buildConfigField("String", "BASE_URL", "\"http://172.20.10.9:8080/\"")
            buildConfigField("String", "API_BASE_URL", "\"http://172.20.10.9:8080\"")

            manifestPlaceholders["appLabel"] = "BiteCal (devWifi)"
        }

        create("devUsb") {
            dimension = "env"
            applicationIdSuffix = ".devusb"
            versionNameSuffix = "-devusb"

            // ⚠️ 提醒：真機用 127.0.0.1 會指向「手機自己」不是電腦
            // 如果你是 adb reverse 8080:8080 才能用這個
            buildConfigField("String", "BASE_URL", "\"http://127.0.0.1:8080/\"")
            buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8080\"")

            manifestPlaceholders["appLabel"] = "BiteCal (devUsb)"
        }

        create("prod") {
            dimension = "env"
            // TODO: 之後換正式域名

            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080\"")

            manifestPlaceholders["appLabel"] = "BiteCal"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions { jvmTarget = "21" }

    buildFeatures {
        compose = true
        buildConfig = true // ✅ 你已經有，保留
    }
}

dependencies {
    // ===== Compose（僅保留一份 BOM） =====
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // ===== 核心/生命週期 =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // ===== Navigation / Hilt =====
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    // ← Hilt-Work 的編譯器
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // ===== 協程 / DataStore =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ===== 網路層 =====
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("com.google.code.gson:gson:2.11.0")

    // ===== 其他（Health Connect / Coil / Paging / Room / Media） =====
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.paging:paging-compose:3.3.2")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // ===== WorkManager + Hilt =====
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")

    // ===== 測試 =====
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // Baseline Profile
    baselineProfile(project(":baselineprofile"))
}

kapt {
    correctErrorTypes = true
}

configurations.configureEach {
    resolutionStrategy.force("com.squareup:javapoet:1.13.0")
    resolutionStrategy.eachDependency {
        if (requested.group == "com.squareup" && requested.name == "javapoet") {
            useVersion("1.13.0")
        }
    }
}
