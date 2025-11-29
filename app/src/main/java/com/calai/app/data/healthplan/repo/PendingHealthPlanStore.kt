package com.calai.app.data.healthplan.repo

import androidx.datastore.preferences.core.Preferences
import com.calai.app.data.healthplan.api.SaveHealthPlanRequest

/**
 * 抽象化 pending store，讓 Repository 可以寫單元測試（不用 Android Context）。
 */
interface PendingHealthPlanStore {
    suspend fun getPending(): SaveHealthPlanRequest?
    suspend fun setPending(req: SaveHealthPlanRequest)
    suspend fun clearPending()
}
