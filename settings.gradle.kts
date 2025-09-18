pluginManagement {
    repositories {
        google()            // ★ 必須：AGP 外掛從這裡抓
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()            // ★ 這裡也要
        mavenCentral()
    }
}

rootProject.name = "CalAI"
include(":app")
