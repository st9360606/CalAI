package com.calai.app.data.activity.sync

object DataOriginPrefs {
    const val GOOGLE_FIT = "com.google.android.apps.fitness"
    const val SAMSUNG_HEALTH = "com.sec.android.app.shealth"
    const val ON_DEVICE_ANDROID = "android"

    val preferred = listOf(GOOGLE_FIT, SAMSUNG_HEALTH, ON_DEVICE_ANDROID)
}

suspend fun pickFirstExistingOrigin(
    preferred: List<String>,
    exists: suspend (String) -> Boolean
): String? {
    for (pkg in preferred) {
        if (exists(pkg)) return pkg
    }
    return null
}

