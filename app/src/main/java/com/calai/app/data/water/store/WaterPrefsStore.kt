package com.calai.app.data.water.store

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.waterPrefsDataStore by preferencesDataStore(name = "water_prefs")

enum class WaterUnit { ML, OZ }

@Singleton
class WaterPrefsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val KEY_UNIT = stringPreferencesKey("unit") // "ML" or "OZ"

    /** 觀察目前顯示用單位（預設 ML） */
    val unitFlow: Flow<WaterUnit> = context.waterPrefsDataStore.data.map { prefs ->
        when (prefs[KEY_UNIT]) {
            "OZ" -> WaterUnit.OZ
            else -> WaterUnit.ML
        }
    }

    /** 切換單位並存檔 */
    suspend fun setUnit(unit: WaterUnit) {
        context.waterPrefsDataStore.edit { prefs ->
            prefs[KEY_UNIT] = unit.name
        }
    }
}
