package com.calai.bitecal.i18n

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")
private val KEY_LANG = stringPreferencesKey("app_lang")

class LanguageStore(private val context: Context) {
    val langFlow = context.dataStore.data.map { it[KEY_LANG] ?: "" }
    suspend fun save(tag: String) {
        context.dataStore.edit { it[KEY_LANG] = tag }
    }
}
