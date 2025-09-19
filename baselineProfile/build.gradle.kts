plugins {
    id("com.android.test")
    id("androidx.baselineprofile") // ← 不要帶 version，沿用 classpath 的 1.2.4
    kotlin("android")
}


android {
    namespace = "com.calai.app.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    // 與 app 對齊 flavor（只錄 prod 線）
    flavorDimensions += listOf("env")
    productFlavors {
        create("prod") { dimension = "env" }
    }

    // JVM 統一，避免前面碰到的 1.8 / 21 不一致
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // 升級這一條，修正你剛才遇到的 IllegalStateException
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")

    implementation("androidx.test.ext:junit:1.1.5")
    implementation("androidx.test:runner:1.5.2")
    implementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}

// ★ Kotlin Toolchain 統一 17
kotlin {
    jvmToolchain(17)
}
