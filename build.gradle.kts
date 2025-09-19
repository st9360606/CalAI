plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        // 提供 Hilt Gradle 插件給子模組使用（子模組就不用寫 version）
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.52")
        // 關鍵：也把 javapoet 放進「插件 classpath」
        classpath("com.squareup:javapoet:1.13.0")
    }
    configurations.classpath {
        resolutionStrategy.force("com.squareup:javapoet:1.13.0")
        resolutionStrategy.eachDependency {
            if (requested.group == "com.squareup" && requested.name == "javapoet") {
                useVersion("1.13.0")
            }
        }
    }
}
